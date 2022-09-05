package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.background.exception.GatewayNotFoundException;
import com.sw.bi.scheduler.background.exception.SchedulerException;
import com.sw.bi.scheduler.model.Gateway;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.model.WaitUpdateStatusTask;
import com.sw.bi.scheduler.service.*;
import com.sw.bi.scheduler.util.Configure;
import com.sw.bi.scheduler.util.Configure.ReferPointRandom;
import com.sw.bi.scheduler.util.Configure.RoundWay;
import com.sw.bi.scheduler.util.Configure.TaskRunningPriority;
import com.sw.bi.scheduler.util.DateUtil;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate5.HibernateCallback;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service("waitUpdateStatusTaskserService")
@SuppressWarnings("unchecked")
public class WaitUpdateStatusTaskServiceImpl extends GenericServiceHibernateSupport<WaitUpdateStatusTask> implements WaitUpdateStatusTaskService {

	@Autowired
	private TaskCreateLogService taskCreateLogService;

	@Autowired
	private ScheduleSystemStatusService scheduleSystemStatusService;

	@Autowired
	private TaskService taskService;

	@Autowired
	private GatewayService gatewayService;

	@Override
	public List<WaitUpdateStatusTask> getWaitUpdateStatusTasks(Date date) throws SchedulerException {
		// 对指定日期进行初始化操作
		this.initialize(date);

		// 将昨天遗留下来的任务把扫描日期改成指定日期
		this.updateYesterdayScanDate(date);

		List<WaitUpdateStatusTask> list = this.getWaitUpdateStatusTasksWithCalculate(date);

		// 修改参考点最近一次扫描时间和处理次数,这二个值主要协助后期排查
		for (WaitUpdateStatusTask wust : list) {
			wust.setScanTime(new Date());
			wust.setScanTimes(wust.getScanTimes() + 1);

			super.update(wust);
		}

		return list;
	}

	@Override
	public Collection<WaitUpdateStatusTask> getWaitUpdateStatusTasks(Collection<Long> taskIds) {
		if (taskIds == null || taskIds.size() == 0) {
			return new ArrayList<WaitUpdateStatusTask>();
		}

		Criteria criteria = createCriteria();
		criteria.add(Restrictions.in("taskId", taskIds));

		return criteria.list();
	}

	@Override
	public WaitUpdateStatusTask addRootTask(final Date taskDate) throws SchedulerException {
		// 校验主网关机当前轮循方式
		Gateway gateway = gatewayService.getMasterGateway();
		if (gateway.getRoundWay() == RoundWay.SIMULATE.indexOf()) {
			return null;
		}

		Task root = taskService.getRootTask(taskDate);
		if (root == null) {
			return null;
		}

		/*WaitUpdateStatusTask wust = new WaitUpdateStatusTask(DateUtil.getToday(), taskDate, root.getTaskId());
		this.updateWeight(wust, root);*/
		return this.create(DateUtil.getToday(), root);
	}

	/**
	 * 将指定任务Task task的所有父任务加入到参考点表中 判断逻辑：
	 * 判断指定任务的父任务的状态是否是成功状态,如果是成功状态,并且如果参考点表中没有该父任务
	 * ,则加入到参考点表中;如果参考点表中已经有了,则更新其flag字段。
	 */
	@Override
	public void addParentTasks(Task task, Date scanDate) {
		// 校验主网关机当前轮循方式
		Gateway gateway = gatewayService.getMasterGateway();
		if (gateway.getRoundWay() == RoundWay.SIMULATE.indexOf()) {
			return;
		}

		boolean isSerial = StringUtils.hasText(task.getPreTasks()) || StringUtils.hasText(task.getPreTasksFromOperate());

		// 如果指定任务已经是成功状态，则不需要对其父任务进行操作了
		// 主要是针对加权操作，因为新上线和重跑操作时指定任务的状态肯定是已经被更新为非成功状态的
		if (task.isRunSuccess() && !task.isRoot()) {
			return;
		}

		Criteria criteria = createCriteria();
		//criteria.add(Restrictions.eq("scanDate", scanDate));  防止参考点出现重复. 避免出现scan_date不同,task_id相同的重复参考点
		criteria.setProjection(Projections.property("taskId"));
		List<Long> taskIds = criteria.list(); //........................当前参考点的taskid集合

		Collection<Task> parentTasks = new HashSet<Task>();
		if (task.isRoot()) {
			// 如果是根任务因为没有父任务了，所以需要把自身放入表中
			parentTasks.add(task);
		} else {
			parentTasks = taskService.getParentTasks(task);
		}
		//................................................................指定任务的父任务集合

		for (Task parentTask : parentTasks) {
			long parentTaskId = parentTask.getTaskId();
			// 需要被加入参数点表的父任务状态必须为“运行成功”或“重做成功”
			if (parentTask.isRunSuccess()) {
				if (!taskIds.contains(parentTaskId)) {
					// 如果该父任务在参考点表不存在则新增
					// wust = new WaitUpdateStatusTask(scanDate, parentTask.getTaskDate(), parentTaskId); //创建一个参考点对象

					long flag = task.getFlag();
					Integer flag2 = task.getFlag2();

					// 如果当前任务是执行了串行补数据操作则直接将其的权重降至最低
					// 因为并行补数据和串行补数据的flag都是同一个值,所以使用preTasksFromOperate字段来判断是否是串行
					if (StringUtils.hasText(task.getPreTasksFromOperate())) {
						flag = 0;
						flag2 = 0;
					}

					this.create(scanDate, parentTask, flag, flag2, isSerial);
					taskIds.add(parentTaskId);

					// 不需要放入所有父任务，只需要确保参数点表中有该任务的任意一个父任务即可
					break;

				} else {
					// 如果已经有参考点表中了则需要将其获得以更改flag字段
					WaitUpdateStatusTask wust = this.getWaitUpdateStatusTaskByTask(parentTaskId);

					// 根据指定的任务标志来设置被放入参考点表的父任务的标志
					this.updateWeight(wust, task.getFlag(), task.getFlag2());
				}
			}
		}
	}

	/**
	 * 与public void addParentTasks(Task task, Date scanDate)这个方法比,就是多了一步 for
	 * (Task task : tasks) { 将指定任务List<Task> tasks的所有父任务加入到参考点表中 判断逻辑：
	 * 判断指定任务的父任务的状态是否是成功状态
	 * ,如果是成功状态,并且如果参考点表中没有该父任务,则加入到参考点表中;如果参考点表中已经有了,则更新其flag字段。
	 */
	@Override
	public void addParentTasks(List<Task> tasks, Date scanDate) {
		if (tasks.size() == 0) {
			return;
		}

		// 校验主网关机当前轮循方式
		Gateway gateway = gatewayService.getMasterGateway();
		if (gateway.getRoundWay() == RoundWay.SIMULATE.indexOf()) {
			return;
		}

		Criteria criteria = createCriteria();
		//criteria.add(Restrictions.eq("scanDate", scanDate));  防止参考点出现重复
		criteria.setProjection(Projections.property("taskId"));
		List<Long> taskIds = criteria.list();

		for (Task task : tasks) {
			// 2014-05-14感觉这里应该和上面一样把成功状态的任务排除
			if (task.isRunSuccess()) {
				continue;
			}

			boolean isSerial = StringUtils.hasText(task.getPreTasks()) || StringUtils.hasText(task.getPreTasksFromOperate());

			Collection<Task> parentTasks = new HashSet<Task>();
			if (task.isRoot()) {
				parentTasks.add(task);
			} else {
				parentTasks = taskService.getParentTasks(task);
			}

			for (Task parentTask : parentTasks) {
				long parentTaskId = parentTask.getTaskId();

				// 需要被加入参数点表的父任务状态必须为“运行成功”或“重做成功”
				if (parentTask.isRunSuccess()) {
					if (!taskIds.contains(parentTaskId)) {
						// 如果该父任务在参考点表不存在则新增

						long flag = task.getFlag();
						Integer flag2 = task.getFlag2();

						// 如果当前任务是执行了串行补数据操作则直接将其的权重降至最低
						// 因为并行补数据和串行补数据的flag都是同一个值,所以使用preTasksFromOperate字段来判断是否是串行
						if (StringUtils.hasText(task.getPreTasksFromOperate())) {
							flag = 0;
							flag2 = 0;
						}

						this.create(scanDate, parentTask, flag, flag2, isSerial);
						taskIds.add(parentTaskId);

						// 不需要放入所有父任务，只需要确保参数点表中有该任务的任意一个父任务即可
						break;

					} else {
						// 如果已经有参考点表中了则需要将其获得以更改flag字段
						WaitUpdateStatusTask wust = this.getWaitUpdateStatusTaskByTask(parentTaskId);
						this.updateWeight(wust, task.getFlag(), task.getFlag2());
					}
				}
			}
		}
	}

	/**
	 * 作用：创建参考点 参数Date date是参考点的扫描日期 参数Task task是被放入参考点表的那个任务
	 */
	@Override
	public WaitUpdateStatusTask create(Date scanDate, Task task) {
		return create(scanDate, task, false);
	}

	@Override
	public WaitUpdateStatusTask create(Date scanDate, Task task, boolean isSerial) {
		if (task == null) {
			return null;
		}

		return this.create(scanDate, task, task.getFlag(), task.getFlag2(), isSerial);
	}

	// @Override
	private WaitUpdateStatusTask create(Date scanDate, Task task, long flag, Integer flag2, boolean isSerial) {
		// 校验主网关机当前轮循方式
		Gateway gateway = gatewayService.getMasterGateway();
		if (gateway.getRoundWay() == RoundWay.SIMULATE.indexOf()) {
			return null;
		}

		int cycleType = task.getCycleType();
		long jobId = task.getJobId();
		Date settingTime = task.getSettingTime();

		/**
		 * <pre>
		 *  生产现象: 一个小时点内，分钟任务只取1个作为参考点，之前这么做，是为了避免参考点表中有大量的分钟参考点
		 *  但是现在线上发现：凌晨的时候，假如后续的分钟任务跑错了，则会因为对应的分钟参考点没有被加入到参考点表中，使得这些任务无法再次被扫描到，就需要人工上去处理了
		 *  所以： 这个限制暂时先放开，看看实际的运行情况再说
		 *  FTP5(天任务)---1483（小时任务）---1153（小时任务）
		 *                                当轮询FTP5的时候，取到其子任务1483， 发现1483的23:50和23:55都存在未完成的子任务， 所以将1483的23:50加入到了参考点，1483的23:55因为之前为了避免过多分钟参考点，被屏蔽掉了，没有加入。 而后面的1153两个孙任务，在本轮扫描中，被修改为了已触发。
		 *                                但是当孙任务1153的23:55一旦运行失败， 因为它对应的参考点没有加入，所以无法再次被轮询启动
		 *                              （因为FTP5后面的两个子任务1483的23:50,23:55都运行成功了，所以参考点FTP5就移除了。 1483的23:50的子任务1153的23:50也运行成功后，参考点1483的23:50也移除了，这样1153的23:55就成了一个孤立的点，没有对应的参考点了）
		 * </pre>
		 */
		/*if (cycleType == JobCycle.MINUTE.indexOf()) {
			// 如果是分钟任务则需要校验该时间点内只存在一个参考点
			Calendar calendar = DateUtil.getCalendar(settingTime);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			Date settingTimeStart = calendar.getTime();

			calendar.set(Calendar.MINUTE, 59);
			calendar.set(Calendar.SECOND, 59);
			Date settingTimeEnd = calendar.getTime();

			// 如果相同作业ID在同一个小时中有参考点存在则不需要再加入新的参考点，只需要将已存在的参考的权重更新
			Collection<WaitUpdateStatusTask> wusts = this.getWaitUpdateStatusTasksBySettingTime(jobId, settingTimeStart, settingTimeEnd);
			if (wusts.size() > 0) {
				// 更新已有参考点的权重
				for (WaitUpdateStatusTask wust : wusts) {
					this.updateWeight(wust, flag, flag2);
				}

				return null;
			}
		}*/

		WaitUpdateStatusTask wust = new WaitUpdateStatusTask(scanDate, task.getTaskDate(), task.getTaskId());
		wust.setJobId(jobId);
		wust.setSettingTime(settingTime);
		wust.setTaskName(task.getName());
		wust.setCreateTime(new Date());
		wust.setSerial(isSerial);

		this.updateWeight(wust, flag, flag2);

		return wust;
	}

	@Override
	public Collection<WaitUpdateStatusTask> getActiveWaitUpdateStatusTasks() {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("active", true));
		criteria.addOrder(Order.desc("flag"));
		criteria.addOrder(Order.asc("taskDate"));
		criteria.addOrder(Order.asc("jobId"));
		criteria.addOrder(Order.asc("waitUpdateStatusTaskId"));

		return criteria.list();
	}

	/**
	 * 根据TaskID获得参考点,但是在补数据操作时会出现多个相同TaskID(原因未查到)，所以只返回第一个
	 * 
	 * @param taskId
	 * @return
	 */
	private WaitUpdateStatusTask getWaitUpdateStatusTaskByTask(Long taskId) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("taskId", taskId));
		criteria.setMaxResults(1);

		return (WaitUpdateStatusTask) criteria.uniqueResult();
	}

	/**
	 * 对指定日期进行初始化操作(将根节点任务放入到参考点表中)
	 * 
	 * @param date
	 * @throws SchedulerException
	 */
	private void initialize(Date date) throws SchedulerException {
		// 如果指定日期的所有任务都已经完成则不需要再进行初始化操作
		if (taskCreateLogService.isTaskRuned(date))
			return;

		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("scanDate", date));
		int count = count(criteria);

		// 如果指定日期已有数据则也不需要进行初始化
		if (count > 0)
			return;

		// 将根节点任务放入到参考点表中(因为现在在创建任务时就会把根任务放入参考点,所以以下代码应该不会再被执行)
		// this.create(date, taskService.getRootTask(date));
		this.addRootTask(date);

	}

	/**
	 * 更改历史任务的扫描日期
	 * 
	 *            date 是今天的日期
	 */
	// 将参考点表中,scanDate小于等于昨天日期的参考点的scanDate修改为今天
	private void updateYesterdayScanDate(Date date) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.le("scanDate", DateUtil.getYesterday(date))); //历史任务： scanDate小于等于昨天
		List<WaitUpdateStatusTask> wusts = criteria.list();

		for (WaitUpdateStatusTask wust : wusts) {
			wust.setScanDate(date);
			update(wust);

			Task task = taskService.get(wust.getTaskId());
			task.setScanDate(date);
			taskService.update(task);

		}
	}

	/**
	 * 修改参考点权重
	 * 
	 * @param wust
	 * @param flag
	 * @param flag2
	 */
	private void updateWeight(WaitUpdateStatusTask wust, long flag, Integer flag2) {
		/*if (flag <= TaskFlag.SYSTEM.indexOf()) {
			flag = TaskFlag.SYSTEM.indexOf();
		}*/

		// Integer flag2 = task.getFlag2();
		if (flag2 != null) {
			flag = Math.max(flag, flag2);
		}

		flag = Math.max(flag, wust.getFlag());

		wust.setFlag((int) flag);
		saveOrUpdate(wust);
	}

	/**
	 * 从参考点表中获取本次调度要用来参考的参考点的集合
	 * 
	 * @param date
	 * @return
	 * @throws GatewayNotFoundException
	 */
	private List<WaitUpdateStatusTask> getWaitUpdateStatusTasksWithCalculate(Date date) throws GatewayNotFoundException {
		String gateway = Configure.property(Configure.GATEWAY);

		int waitUpdateStatusTaskCount = scheduleSystemStatusService.getWaitUpdateStatusTaskCount(gateway); //服务器单次调度选取的最大参考点数目

		/**
		 * <pre>
		 * 	2014-05-04
		 * 	固定选取参考点会导致前一天晚上剩余的任务会很难执行起来
		 *  随机选取参考点又会导致串补数据时增加很多无谓的任务被选取
		 *  权衡后决定按时间自动切换二种选取策略
		 * </pre>
		 */
		int referPointRandom = scheduleSystemStatusService.getReferPointRandom(gateway); //参考点随机选取还是按照固定次序选取
		boolean isReferPointRandom = referPointRandom == ReferPointRandom.YES.indexOf();

		// 参考点选取需要自动切换
		if (referPointRandom == ReferPointRandom.AUTO.indexOf()) {
			Calendar calendar = DateUtil.getTodayCalendar();
			calendar.set(Calendar.HOUR_OF_DAY, 9);
			calendar.set(Calendar.MINUTE, 30);
			Date switchStartDate = calendar.getTime();

			calendar.set(Calendar.HOUR_OF_DAY, 22);
			Date switchEndDate = calendar.getTime();

			long now = System.currentTimeMillis();

			isReferPointRandom = (now < switchStartDate.getTime() || switchEndDate.getTime() < now);
		}

		log.info("参考点选取方式: " + (isReferPointRandom ? "随机" : "固定") + "方式选取");

		int taskRunningPriority = scheduleSystemStatusService.getTaskRunningPriority(gateway); //任务运行的优先级：今日任务优先,历史任务优先,按照比例选取今日任务和历史任务运行
		boolean isTodayFirst = TaskRunningPriority.TODAY_FIRST.indexOf() == taskRunningPriority;
		boolean isYesterdayFirst = TaskRunningPriority.YESTERDAY_FIRST.indexOf() == taskRunningPriority;
		boolean isPercent = TaskRunningPriority.PERCENT.indexOf() == taskRunningPriority;

		List<WaitUpdateStatusTask> todayTasks = new ArrayList<WaitUpdateStatusTask>();
		List<WaitUpdateStatusTask> yesterdayTasks = new ArrayList<WaitUpdateStatusTask>();

		Criteria criteria = null;
		int ignoreSameJobCount = 0; // 忽略相同作业ID的数量
		if (isTodayFirst || isPercent) {
			criteria = createCriteria();
			criteria.add(Restrictions.eq("active", true));
			criteria.add(Restrictions.eq("scanDate", date));
			criteria.add(Restrictions.eq("taskDate", date));//taskDate等于今天
			criteria.addOrder(Order.desc("flag"));
			criteria.addOrder(Order.asc("waitUpdateStatusTaskId"));
			todayTasks = criteria.list();
		}

		if (isYesterdayFirst || isPercent) {
			criteria = createCriteria();
			criteria.add(Restrictions.eq("active", true));
			criteria.add(Restrictions.eq("scanDate", date));
			criteria.add(Restrictions.lt("taskDate", date)); //taskDate小于今天
			criteria.addOrder(Order.desc("flag"));
			criteria.addOrder(Order.asc("taskDate"));
			criteria.addOrder(Order.asc("jobId"));
			criteria.addOrder(Order.asc("settingTime"));
			criteria.addOrder(Order.asc("waitUpdateStatusTaskId"));
			yesterdayTasks = criteria.list();

			// 获得所有串行参考点的最小任务日期
			Map<Long, Date> serialReferMinTaskDate = new HashMap<Long, Date>();
			for (WaitUpdateStatusTask wust : yesterdayTasks) {
				if (!wust.isSerial()) {
					continue;
				}

				Date minTaskDate = serialReferMinTaskDate.get(wust.getJobId());
				if (minTaskDate == null) {
					minTaskDate = wust.getTaskDate();
				} else {
					if (wust.getTaskDate().getTime() < minTaskDate.getTime()) {
						minTaskDate = wust.getTaskDate();
					}
				}
				serialReferMinTaskDate.put(wust.getJobId(), minTaskDate);
			}

			// 去除相同作业ID的参考点
			// 2014.3.17,最近发现补数据操作引发了一系列的表死锁现象，原因是从一个子，孙节点比较多的节点开始补数据，该节点被大量放入参考点表
			// 导致后台轮询选取历史参考点的时候，选出了该作业的大量历史日期的任务节点，而这样的子，孙节点比较多的节点，后台处理较慢，导致一次轮询时间过长
			// Collection<Long> jobIds = new HashSet<Long>();
			log.info("被忽略的相同作业ID的参考点清单:");
			for (Iterator<WaitUpdateStatusTask> iter = yesterdayTasks.iterator(); iter.hasNext();) {
				WaitUpdateStatusTask yesterdayTask = iter.next();
				Long jobId = yesterdayTask.getJobId();

				if (yesterdayTask.isSerial()) {
					Date minTaskDate = serialReferMinTaskDate.get(jobId);

					if (minTaskDate.getTime() != yesterdayTask.getTaskDate().getTime()) {
						ignoreSameJobCount += 1;
						log.info("[忽略相同参考点] " + yesterdayTask);

						iter.remove();

					} else {
						log.info("[串行选中参考点] " + yesterdayTask);
					}
				}

				/*if (yesterdayTask.isSerial() && jobIds.contains(jobId)) {
					// 串行参考点时相同作业ID只取一个
					ignoreSameJobCount += 1;
					log.info("[忽略相同参考点] " + yesterdayTask);

					iter.remove();
				} else {
					log.info("[串行选中参考点] " + yesterdayTask);
					jobIds.add(jobId);
				}*/
			}
		}

		int yesterdayFactCount = yesterdayTasks.size(); //数据库查询出来的,参考点表中的历史参考点的个数
		int todayFactCount = todayTasks.size(); //数据库查询出来的,参考点表中的今日参考点的个数

		// todaySelectCount：           理论上应该被选取出来的今日参考点的个数
		// yesterdaySelectCount: 理论上应该被选取出来的历史参考点的个数
		int todaySelectCount = 0, yesterdaySelectCount = 0;
		if (isTodayFirst) {
			todaySelectCount = waitUpdateStatusTaskCount;
			log.info("参考点选取规则: 仅选取今日参考点.");

		} else if (isYesterdayFirst) {
			yesterdaySelectCount = waitUpdateStatusTaskCount;
			log.info("参考点选取规则: 仅选取历史参考点.");

		} else if (isPercent) {
			double percent = Configure.property(Configure.TODAY_WAIT_UPDATE_STATUS_TASK_SELECT_PERCENT, Double.class);

			todaySelectCount = Math.min(todayFactCount, (int) Math.round(waitUpdateStatusTaskCount * percent));
			yesterdaySelectCount = Math.min(yesterdayFactCount, waitUpdateStatusTaskCount - todaySelectCount);

			int factWaitUpdateStatusTaskCount = todaySelectCount + yesterdaySelectCount;
			if (factWaitUpdateStatusTaskCount < waitUpdateStatusTaskCount) {
				todaySelectCount += (waitUpdateStatusTaskCount - factWaitUpdateStatusTaskCount);
			}

			log.info("参考点选取规则: 按比例选取 - " + percent + ".");
		}

		//根据todaySelectCount和todayFactCount及yesterdaySelectCount和yesterdayFactCount将参考点放入List<WaitUpdateStatusTask> wusts
		List<WaitUpdateStatusTask> wusts = new ArrayList<WaitUpdateStatusTask>();
		int todayFactSelectCount = 0; // 今天实际选取的参考点数
		if (todayFactCount <= todaySelectCount) {
			todayFactSelectCount = todayTasks.size();
			wusts.addAll(todayTasks);
		} else {
			if (isReferPointRandom) { //参考点随机的情况下,随机选择参考点
				Integer[] randoms = this.getRangeRandom(todaySelectCount, todayFactCount);
				todayFactSelectCount = randoms.length;
				for (Integer random : randoms) {
					wusts.add(todayTasks.get(random));
				}
			} else {
				Collection<WaitUpdateStatusTask> todayWusts = todayTasks.subList(0, todaySelectCount);
				todayFactSelectCount = todayWusts.size();
				wusts.addAll(todayWusts);
			}
		}
		log.info("今日参考点有 " + todayFactCount + " 个, 实际选取 " + todayFactSelectCount + " 个.");

		int yesterdayFactSelectCount = 0;
		if (yesterdayFactCount <= yesterdaySelectCount) {
			yesterdayFactSelectCount = yesterdayTasks.size();
			wusts.addAll(yesterdayTasks);
		} else {
			if (isReferPointRandom) { //参考点随机的情况下,随机选择参考点
				Integer[] randoms = this.getRangeRandom(yesterdaySelectCount, yesterdayFactCount);
				yesterdayFactSelectCount = randoms.length;
				for (Integer random : randoms) {
					wusts.add(yesterdayTasks.get(random));
				}
			} else {
				Collection<WaitUpdateStatusTask> yesterdayWusts = yesterdayTasks.subList(0, yesterdaySelectCount);
				yesterdayFactSelectCount = yesterdayWusts.size();
				wusts.addAll(yesterdayWusts);
			}
		}
		log.info("历史参考点有 " + yesterdayFactCount + " 个" + (ignoreSameJobCount == 0 ? "" : "(忽略相同作业 " + ignoreSameJobCount + " 个)") + ", 实际选取 " + yesterdayFactSelectCount + " 个.");
		log.info("参考点总计选取 " + wusts.size() + " 个.");

		return wusts;
	}

	/**
	 * 根据指定作业ID和预设时间获得参考点
	 * 
	 * @param jobId
	 * @param settingTimeStart
	 * @param settingTimeEnd
	 * @return
	 */
	private Collection<WaitUpdateStatusTask> getWaitUpdateStatusTasksBySettingTime(long jobId, Date settingTimeStart, Date settingTimeEnd) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("jobId", jobId));

		if (settingTimeStart != null) {
			criteria.add(Restrictions.ge("settingTime", settingTimeStart));
		}

		if (settingTimeEnd != null) {
			criteria.add(Restrictions.le("settingTime", settingTimeEnd));
		}

		return criteria.list();
	}

	@Override
	public Integer[] getRangeRandom(int total, int end) {
		if (total > end)
			total = end;

		Collection<Integer> randoms = new HashSet<Integer>();
		Random random = new Random();
		while (randoms.size() < total) {
			randoms.add(random.nextInt(end));
		}

		return randoms.toArray(new Integer[total]);
	}

	@Override
	@Deprecated
	public List<WaitUpdateStatusTask> getWaitUpdateStatusTasksByDate(Date taskDate) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("taskDate", taskDate));
		List<WaitUpdateStatusTask> wusts = criteria.list();
		return wusts;
	}

	@Override
	public boolean isAllowRemove(long waitUpdateStatusTaskId) {
		WaitUpdateStatusTask wust = this.get(waitUpdateStatusTaskId);
		Collection<Task> childrenTasks = taskService.getChildrenTasks(taskService.get(wust.getTaskId()));

		for (Task task : childrenTasks) {
			if (!task.isRunSuccess()) {
				return false;
			}
		}

		return true;
	}

	@Override
	public void remove(final Date taskDate) {

		getHibernateTemplate().execute(new HibernateCallback<Integer>() {

			@Override
			public Integer doInHibernate(Session session) throws HibernateException {
				String hql = "delete from WaitUpdateStatusTask where taskDate = :taskDate";
				Query query = session.createQuery(hql);
				query.setDate("taskDate", taskDate);

				return query.executeUpdate();
			}

		});

	}

	/////////////////////////////////// 用于监控 ///////////////////////////////////

	@Override
	public void unactiveWaitUpdateStatusTask() throws Exception {
		Date today = DateUtil.getToday();

		Criteria criteria = this.createCriteria();
		criteria.add(Restrictions.eq("scanDate", today));
		criteria.add(Restrictions.lt("taskDate", today));
		criteria.add(Restrictions.eq("active", true));
		Collection<WaitUpdateStatusTask> wusts = criteria.list();

		for (WaitUpdateStatusTask wust : wusts) {
			wust.setActive(false);
			this.update(wust);

			log.info("禁用 - " + "ID: " + wust.getWaitUpdateStatusTaskId() + ", taskId: " + wust.getTaskId() + ", taskDate: " + DateUtil.formatDate(wust.getTaskDate()));
		}
	}

	@Override
	public void activeWaitUpdateStatusTask() throws Exception {
		Criteria criteria = this.createCriteria();
		criteria.add(Restrictions.eq("active", false));
		Collection<WaitUpdateStatusTask> wusts = criteria.list();

		for (WaitUpdateStatusTask wust : wusts) {
			wust.setActive(true);
			this.update(wust);

			log.info("启用 - " + "ID: " + wust.getWaitUpdateStatusTaskId() + ", taskId: " + wust.getTaskId() + ", taskDate: " + DateUtil.formatDate(wust.getTaskDate()));
		}
	}
}
