package com.sw.bi.scheduler.background.service.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate5.HibernateCallback;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sw.bi.scheduler.background.exception.GatewayNotFoundException;
import com.sw.bi.scheduler.background.exception.SchedulerException;
import com.sw.bi.scheduler.background.service.SchedulerService;
import com.sw.bi.scheduler.background.taskexcuter.ConcurrentCategory;
import com.sw.bi.scheduler.background.taskexcuter.ExcuterCenter;
import com.sw.bi.scheduler.model.Action;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.model.WaitUpdateStatusTask;
import com.sw.bi.scheduler.service.ActionService;
import com.sw.bi.scheduler.service.BigDataTaskService;
import com.sw.bi.scheduler.service.ConcurrentService;
import com.sw.bi.scheduler.service.GatewaySchedulerService;
import com.sw.bi.scheduler.service.GatewayService;
import com.sw.bi.scheduler.service.JobService;
import com.sw.bi.scheduler.service.MethodExecTimeService;
import com.sw.bi.scheduler.service.ScheduleSystemStatusService;
import com.sw.bi.scheduler.service.TaskCreateLogService;
import com.sw.bi.scheduler.service.TaskService;
import com.sw.bi.scheduler.service.WaitUpdateStatusTaskService;
import com.sw.bi.scheduler.util.Configure;
import com.sw.bi.scheduler.util.Configure.JobCycle;
import com.sw.bi.scheduler.util.Configure.SchedulerMethod;
import com.sw.bi.scheduler.util.Configure.TaskRunningPriority;
import com.sw.bi.scheduler.util.Configure.TaskStatus;
import com.sw.bi.scheduler.util.DateUtil;

@Service("schedulerService")
@SuppressWarnings("unchecked")
public class SchedulerServiceImpl implements SchedulerService {
	private static final Logger log = Logger.getLogger(SchedulerServiceImpl.class);

	/**
	 * <pre>
	 * 参考点处理时长阀值(默认：50秒)
	 * </pre>
	 */
	private static final long REFER_PROCESS_THRESHOLD = 90 * 1000;

	@Autowired
	private JobService jobService;

	@Autowired
	private TaskService taskService;

	@Autowired
	private ActionService actionService;

	@Autowired
	private TaskCreateLogService taskCreateLogService;

	@Autowired
	private ScheduleSystemStatusService scheduleSystemStatusService;

	@Autowired
	private WaitUpdateStatusTaskService waitUpdateStatusTaskService;

	@Autowired
	private MethodExecTimeService methodExecTimeService;

	@Autowired
	private ConcurrentService concurrentService;

	@Autowired
	private BigDataTaskService bigDataTaskService;

	@Autowired
	private GatewayService gatewayService;

	@Autowired
	private GatewaySchedulerService gatewaySchedulerService;

	/**
	 * 被修改为未触发的所有任务
	 */
	private Collection<Task> updateWaitTriggerTasks = new HashSet<Task>();

	/**
	 * 被修改为已触发的所有任务
	 */
	private Collection<Task> updateTriggeredTasks = new HashSet<Task>();

	@Override
	public void schedule(Date today) throws SchedulerException {
		String gateway = Configure.property(Configure.GATEWAY);

		// 以后可以通过该变量来判断调试程序是否是有主程序调起
		// 如果是以辅助程序调起则可以相应的减少一些步骤
		boolean isMainScheduler = Configure.property(Configure.MAIN_SCHEDULER, Boolean.class);

		//这个today变量必须从Scheduler.java中传入,不然是一个BUG. 
		//执行Scheduler.java的时候时的today 与schedulerService.schedule()时的today已经不是同一天了. 可能有跨天现象,导致后续异常(当天记录还没生成的时候,task_create_log表的run_success字段被置为1,导致后续调度不再进行下去).
		//////Date today = DateUtil.getToday();  

		// 调度系统已经被关闭
		if (!scheduleSystemStatusService.isOpened(gateway)) {
			// log.info("网关机(" + Configure.property(Configure.GATEWAY) + ")上前一次调度仍在继续, 当前调度被关闭.");
			log.info("网关机(" + gateway + ")已经被禁用.");
			return;
		}

		// 判断当前数据库连接数是否已经超过阈值(800)
		if (gatewaySchedulerService.isExceedDatabaseMaxConnection()) {
			return;
		}

		if (isMainScheduler) {
			// 更新自动重跑运行失败的次数
			this.updateRunFailureTimes(today);

			// 对满足条件的任务进行未触发状态的调整
			this.updateWaitTrigger(today);

			// 将未触发状态的任务更改为已触发
			this.updateTriggered(today);
		}

		// 由网关机调度检测当前网关机能否被执行
		// 为了避免多台网关机同时执行任务时,一些查询上的数字不准,从而起不到精确控制大任务并发的数量,
		// 现在原则上让多台网关机的轮询一台一台的串行执行
		// 为了不影响效率,主关机的失败重跑，修改为未触发，修改为已触发这部分的逻辑还是每次进来都执行的
		// 只是任务执行这部分,每次只允许同时一台网关机轮询。（从而起到规避一些并发查询的问题）
		// 而多网关机，各网关机按照任务尾号分配任务，同时也起到了任务分发的作用
		if (scheduleSystemStatusService.isSerialScheduler(gateway) && !gatewaySchedulerService.isAllowExecution(gateway)) {
			return;
		}

		// 执行指定日期内所有满足条件的任务
		// 辅助服务器只提交执行任务,且提交执行的任务不能与主服务器提交执行的任务重复.
		// 辅助服务器不要做其他操作,如不要做变更为:未触发的操作,否则参考点表可能会出现相同的参考点.  也不要做runComplete,... 等操作,只提交执行任务即可.

		try {
			this.execute(today);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// 本轮网关机调度完毕
			gatewaySchedulerService.finished(gateway);
		}
	}

	@Override
	public void simulateSchedule() throws SchedulerException {
		Date today = DateUtil.getToday(); // DateUtil.parseDate("2014-01-07");
		String gateway = Configure.property(Configure.GATEWAY);

		// 以后可以通过该变量来判断调试程序是否是有主程序调起
		// 如果是以辅助程序调起则可以相应的减少一些步骤
		boolean isMainScheduler = Configure.property(Configure.MAIN_SCHEDULER, Boolean.class);

		// 调度系统已经被关闭
		if (!scheduleSystemStatusService.isOpened(gateway)) {
			log.info("网关机(" + gateway + ")已经被禁用.");
			return;
		}

		// 判断当前数据库连接数是否已经超过阈值(800)
		if (gatewaySchedulerService.isExceedDatabaseMaxConnection()) {
			return;
		}

		if (isMainScheduler) {
			// 更新自动重跑运行失败的次数
			// 模拟调度时不能只限制扫描日期是今天的任务所以这里扫描日期就传入null
			this.updateRunFailureTimes(null);
		}

		try {
			this.execute4Simulate(gateway, today);

		} finally {
			// 本轮网关机调度完毕
			gatewaySchedulerService.finished(gateway);
		}
	}

	@Override
	@Deprecated
	public void scheduleUpdateWaitTrigger() throws SchedulerException {
		Date today = DateUtil.getToday();

		if (!scheduleSystemStatusService.isOpened(Configure.property(Configure.GATEWAY))) {
			log.info("schedule is closed...");
			return;
		}

		// 对满足条件的任务进行未触发状态的调整
		this.updateWaitTrigger(today);
	}

	@Override
	public void createTasks(Date date) throws SchedulerException {
		if (!taskCreateLogService.isTaskCreated(date)) {
			String method = SchedulerMethod.CREATE_TASKS.toString();
			methodExecTimeService.begin(method);

			jobService.createTasks(date);

			methodExecTimeService.finished(method);
		}
	}

	/**
	 * 更新自动重跑运行失败的次数
	 * 
	 * @param scanDate
	 * @throws GatewayNotFoundException
	 */
	public void updateRunFailureTimes(Date scanDate) throws GatewayNotFoundException {
		String method = SchedulerMethod.UPDATE_RUN_FAILURE_TIMES.toString();
		methodExecTimeService.begin(method);

		Criteria criteria = taskService.createCriteria();
		if (scanDate != null) {
			criteria.add(Restrictions.eq("scanDate", scanDate));
		}
		//  runTimes 小于等于  配置的失败重跑次数
		// criteria.add(Restrictions.le("runTimes", scheduleSystemStatusService.getTaskFailReturnTimes(Configure.property(Configure.GATEWAY))));
		criteria.add(Restrictions.or(Restrictions.eq("taskStatus", (long) TaskStatus.RUN_FAILURE.indexOf()), Restrictions.eq("taskStatus", (long) TaskStatus.RE_RUN_FAILURE.indexOf())));
		Collection<Task> tasks = criteria.list();

		log.info("共有 " + tasks.size() + " 个任务运行失败, 并准备自动重跑, 以下是这些任务的清单:");

		int taskFailReturnTimes = scheduleSystemStatusService.getTaskFailReturnTimes(Configure.property(Configure.GATEWAY));
		for (Task task : tasks) {
			int runtimes = task.getRunTimes();

			// 获得当前任务失败后重跑的次数(如果当前作业没有配置次数则以当前网关机配置的参数为准)
			boolean isTaskFailureRerunTimesEmpty = task.getFailureRerunTimes() == null;
			int rerunTimes = isTaskFailureRerunTimesEmpty ? taskFailReturnTimes : task.getFailureRerunTimes().intValue();

			// 作业当前运行次数如果超过设置的重跑次数则不允许再被自动重跑
			if (runtimes > rerunTimes) {
				log.info(task + ": 已重跑 " + runtimes + " 次, 预设重跑 " + rerunTimes + " 次(" + (isTaskFailureRerunTimesEmpty ? "以网关机配置为准" : "以作业配置为准") + ")");
				continue;
			}

			// 允许重跑时还需要校验作业预设的间隔时长
			Integer rerunInterval = task.getFailureRerunInterval();
			if (rerunInterval != null) {
				// 获得当前作业的失败时间(优先考虑updateTime字段，如果为空则使用taskEndTime字段)
				Date endTime = task.getUpdateTime();
				if (endTime == null) {
					endTime = task.getTaskEndTime();
				}

				// 如果以上二个字段的时间都为空则该不作业不允许被重跑
				if (endTime == null) {
					continue;
				}

				// 计算该作业自失败后到现在的间隔时间,如果间隔小于设置的间隔时长则不允许被重跑(只有当间隔大于等于设置的间隔时长时才允许被重跑)
				Date now = DateUtil.now();
				long interval = (now.getTime() - endTime.getTime()) / 1000 / 60;
				if (interval < rerunInterval) {
					log.info(task + ": 失败时间 \"" + DateUtil.format(endTime, "yyyy-MM-dd HH:mm:ss") + "\", 当前时间 \"" + DateUtil.format(now, "yyyy-MM-dd HH:mm:ss") + " \", 实际间隔 " + interval +
							" 分钟, 作业预设间隔 " + rerunInterval + " 分钟");
					continue;
				}
			}

			task.setTaskStatus((long) TaskStatus.RE_INITIALIZE.indexOf());
			task.setTaskBeginTime(null);
			task.setTaskEndTime(null);
			task.setRunTime(null);
			task.setReadyTime(null);
			taskService.saveOrUpdate(task);

			log.info("被自动重跑的" + task);
		}

		// log.info("update run failure tasks: " + tasks.size());

		methodExecTimeService.finished(method);
	}

	/**
	 * 以参考点为依据,向下遍历任务树。将节点的状态变更为:未触发
	 * 
	 * @throws SchedulerException
	 */
	@Override
	public void updateWaitTrigger(Date date) throws SchedulerException {
		String method = SchedulerMethod.UPDATE_WAIT_TRIGGER.toString();
		methodExecTimeService.begin(method);

		List<WaitUpdateStatusTask> waitUpdateStatusTasks = waitUpdateStatusTaskService.getWaitUpdateStatusTasks(date); //本次取到的参考点

		Criteria criteria = waitUpdateStatusTaskService.createCriteria();
		criteria.add(Restrictions.eq("active", true));
		Collection<WaitUpdateStatusTask> wusts = waitUpdateStatusTaskService.paging(criteria, 0, 5000).getPaginationResults();
		Map<Long, Task> tasks = new HashMap<Long, Task>();
		for (WaitUpdateStatusTask wust : wusts) {
			tasks.put(wust.getTaskId(), taskService.get(wust.getTaskId()));
		}

		long start = System.currentTimeMillis();
		for (WaitUpdateStatusTask wust : waitUpdateStatusTasks) {
			StringBuilder message = new StringBuilder();
			message.append(wust.getJobId()).append(" - ");
			message.append(wust.getTaskName()).append(" [");
			message.append("任务日期: " + DateUtil.formatDate(wust.getTaskDate()));
			message.append(", 扫描日期: " + DateUtil.formatDate(wust.getScanDate()));
			message.append("]");

			// 如果参考点处理了超过45秒仍未完成，则忽略后面的参考点处理 
			// 2014.5.13, 当串行补数据时，如果被加入到参考点的节点，下面有大量子节点，后台轮询处理大量这种历史参考点时，每个节点处理时间较长，如果一次轮询中，要处理很多个这样的节点
			// 会导致后台一次轮询时间被加长，导致整个调度性能下降。 所以在这里：通过在时间上限制一次扫描参考点的花费。 当扫描处理参考点的总时长超过45秒的时候，就放弃剩余参考点的处理
			if (System.currentTimeMillis() - start > REFER_PROCESS_THRESHOLD) {
				log.info("处理耗时(90秒)忽略的参考点: " + message);
				continue;
			}

			// log.info("....now dealing with: " + wust.getJobId() + " ..." + DateUtil.formatDate(wust.getTaskDate()) + " ..." + DateUtil.formatDate(wust.getScanDate()) + " ..." + wust.getTaskName());
			log.info("....now dealing with: " + message);
			int successCount = 0;

			Task task = taskService.get(wust.getTaskId());
			if (task == null) {
				waitUpdateStatusTaskService.delete(wust);
				continue;
			}

			// TODO 后台偶尔会抛“Could not synchronize database state with session”的异常，一时没有好的解决方法，暂时以忽略的方式进行处理
			Collection<Task> childTasks = null;
			try {
				childTasks = taskService.getChildrenTasks(task); // taskService.getChildrenTasksByUnsuccessLimit(task); //参考点的子节点
			} catch (Exception e) {
				continue;
			}

			if (childTasks.size() > 0) {
				for (Task childTask : childTasks) {
					// 排除"待触发"周期的作业,以保证该周期的作业不会被后台调启
					if (childTask.getCycleType() == JobCycle.NONE.indexOf()) {
						successCount += 1;
						continue;
					}

					// 参考点的子节点,如果是成功的,则不需要考虑将其状态变未触发(因为已经运行成功了).  只要考虑是否要放入参考点表中
					// 该参考点的子节点是否要放入参考点表中:  只要再取其子节点,如果孙节点任务状态全部成功,则不放入;如果有一个还没成功,则放入
					if (childTask.isRunSuccess()) {
						boolean allSuccess = true;
						boolean isSerial = false;
						Collection<Task> secondChildren = taskService.getChildrenTasks(childTask); // taskService.getChildrenTasksByUnsuccessLimit(childTask); //参考点的孙节点

						for (Task secondChild : secondChildren) {
							// 排除"待触发"周期的作业,以保证该周期的作业不会被后台调启
							if (secondChild.getCycleType() == JobCycle.NONE.indexOf()) {
								continue;
							}

							if (!secondChild.isRunSuccess()) {
								allSuccess = false;
								updateWaitTrigger(secondChild); //顺便在本次扫描中,将这些孙节点的状态变 未触发
							} else {
								if (!isSerial) {
									isSerial = StringUtils.hasText(secondChild.getPreTasks()) || StringUtils.hasText(secondChild.getPreTasksFromOperate());
								}
							}
						}

						// 防止重复存入参考点表
						if (!allSuccess && !tasks.containsKey(childTask.getTaskId())) {
							waitUpdateStatusTaskService.create(childTask.getScanDate(), childTask, false);
							tasks.put(childTask.getTaskId(), childTask); //如果有新增加进去的节点,也要更新内存中的tasks,避免重复存入数据库
						}

						successCount += 1;
					} else {
						// 参考点的子节点，任务状态不是运行成功的,那么肯定就不用考虑是否要将这个子节点要放入参考点表了,只需要考虑是否要将这个子节点的状态变成 未触发
						updateWaitTrigger(childTask);
					}
				}
			}

			if (childTasks.size() == successCount) {
				waitUpdateStatusTaskService.delete(wust); //参考点的子节点全部成功,则数据库中删除参考点
			}
		}

		log.info("共有 " + updateWaitTriggerTasks.size() + " 个任务被修改为\"未触发\"状态, 以下是被修改任务的清单:");
		for (Task task : updateWaitTriggerTasks) {
			log.info(task);
		}

		methodExecTimeService.finished(method);
	}

	@Override
	public void updateTriggered(Date date) {
		String method = SchedulerMethod.UPDATE_TRIGGERED.toString();
		methodExecTimeService.begin(method);

		Criteria criteria = taskService.createCriteria();
		criteria.add(Restrictions.eq("scanDate", date));
		criteria.add(Restrictions.le("settingTime", DateUtil.now()));
		criteria.add(Restrictions.not(Restrictions.eq("cycleType", JobCycle.NONE.indexOf()))); // 确保"待触发"周期的作业不会被更改为已触发状态
		criteria.add(Restrictions.or(Restrictions.eq("taskStatus", (long) TaskStatus.WAIT_TRIGGER.indexOf()), Restrictions.eq("taskStatus", (long) TaskStatus.RE_WAIT_TRIGGER.indexOf())));
		List<Task> tasks = criteria.list();

		for (Task task : tasks) {
			this.updateTriggered(task);
		}

		log.info("共有 " + updateTriggeredTasks.size() + " 个任务被修改为\"已触发\"状态, 以下是被修改任务的清单:");
		for (Task task : updateTriggeredTasks) {
			log.info(task);
		}

		methodExecTimeService.finished(method);
	}

	@Override
	public int execute(Date date) throws GatewayNotFoundException {
		String method = SchedulerMethod.TASK_EXECUTE.toString();
		methodExecTimeService.begin(method);

		int result = 0;
		String gateway = Configure.property(Configure.GATEWAY);
		Collection<Task> normalTasks = this.selectTasksByGateway(gateway, date);
		Collection<Task> specialTasks = this.selectTasksByCluster(gateway, date);

		if (normalTasks.size() == 0 && specialTasks.size() == 0) {
			return 0;
		}

		log.info("在网关机(" + Configure.property(Configure.GATEWAY) + ")上本次轮循选取任务 " + (normalTasks.size() + specialTasks.size()) + "条(正常任务 " + normalTasks.size() + "条, 特殊任务 " + specialTasks.size() +
				" 条)");

		// 按并发配置表处理特殊任务
		this.processSpecialTasksByConcurrent(specialTasks);

		result = ExcuterCenter.getInstance().addTasks(normalTasks, specialTasks);

		methodExecTimeService.finished(method);

		return result;
	}

	public int execute4Simulate(String gateway, Date today) throws GatewayNotFoundException {
		String method = SchedulerMethod.TASK_EXECUTE.toString();
		methodExecTimeService.begin(method);

		// 选取到非成功或失败的任务
		Collection<Task> normalTasks = this.selectTasks4SimulateByGateway(gateway, today);
		Collection<Task> specialTasks = this.selectTasks4SimulateByCluster(gateway, today);

		if (normalTasks.size() == 0 && specialTasks.size() == 0) {
			return 0;
		}

		// 按并发配置表处理特殊任务
		this.processSpecialTasksByConcurrent(specialTasks);

		log.info("按并发配置表过滤后的特殊任务清单:");
		for (Task specialTask : specialTasks) {
			StringBuilder message = new StringBuilder(specialTask.toString());
			if (bigDataTaskService.isBigDataTask(specialTask.getJobId())) {
				message.append("<大>");
			}

			String executeGateway = specialTask.getGateway();
			if (StringUtils.hasText(executeGateway)) {
				message.append("[设定网关机:" + executeGateway + "]");
			}

			log.info(message);
		}

		log.info("在网关机(" + Configure.property(Configure.GATEWAY) + ")上本次轮循选取任务 " + (normalTasks.size() + specialTasks.size()) + "条(正常任务 " + normalTasks.size() + "条, 特殊任务 " + specialTasks.size() +
				" 条)");

		// 经过上面校验过后的任务列表应该都是已触发的任务
		// 这里需要对这批已触发任务按readyTime升序排序
		Comparator<Task> readyTimeComparator = new Comparator<Task>() {
			@Override
			public int compare(Task t1, Task t2) {
				long readyTime1 = t1.getReadyTime() == null ? 0 : t1.getReadyTime().getTime();
				long readyTime2 = t2.getReadyTime() == null ? 0 : t2.getReadyTime().getTime();

				if (readyTime1 < readyTime2) {
					return -1;
				} else if (readyTime1 > readyTime2) {
					return 1;
				}

				return 0;
			}
		};

		Collections.sort((List<Task>) normalTasks, readyTimeComparator);
		Collections.sort((List<Task>) specialTasks, readyTimeComparator);

		// 从经过升序排序后的已触发任务中选取网关机最大运行任务数量的任务
		// int runningMax = scheduleSystemStatusService.getTaskRunningMax(gateway); // 网关机最大运行任务数
		// int result = this.execute(((List<Task>) normalTasks).subList(0, Math.min(normalTasks.size(), runningMax)), false);
		int result = ExcuterCenter.getInstance().addTasks(normalTasks, specialTasks);

		methodExecTimeService.finished(method);

		return result;
	}

	@Override
	public int execute(Collection<Task> triggeredTasks, boolean ignoreGatewayQuota) throws GatewayNotFoundException {
		String method = SchedulerMethod.TASK_EXECUTE.toString();
		methodExecTimeService.begin(method);

		int result = 0;
		if (triggeredTasks.size() > 0) {
			result = ExcuterCenter.getInstance().addTasks(triggeredTasks, ignoreGatewayQuota);
		}

		methodExecTimeService.finished(method);

		return result;
	}

	@Override
	public void updateTriggered(Task task) {
		long taskStatus = task.getTaskStatus();

		// 如果需要修改的任务已经是“已触发”状态则忽略
		if (taskStatus != TaskStatus.TRIGGERED.indexOf() && taskStatus != TaskStatus.RE_TRIGGERED.indexOf()) {
			task.setTaskStatus(taskStatus == TaskStatus.WAIT_TRIGGER.indexOf() || taskStatus == TaskStatus.INITIALIZE.indexOf() ? TaskStatus.TRIGGERED.indexOf() : TaskStatus.RE_TRIGGERED.indexOf());
			task.setReadyTime(taskService.calculateReadyTime(task));
			// 修改scan_date字段是为了解决昨天遗留下来的任务，能使这些遗留下来的任务在今天能被运行
			task.setScanDate(DateUtil.getToday());
			task.setTaskBeginTime(null);
			task.setTaskEndTime(null);
			task.setRunTime(null);
			taskService.update(task);

		}

		if (taskService.recheck(task)) {
			updateTriggeredTasks.add(task);
		}
	}

	@Override
	public void updateWaitTrigger(Task task) {
		if (task.getTaskStatus() != TaskStatus.INITIALIZE.indexOf() && task.getTaskStatus() != TaskStatus.RE_INITIALIZE.indexOf()) {
			// 如果扫描到的参考点是历史遗留下来的(task_date是以前的日期),那么在修改为 未触发这一步时,会将该参考点下面的子节点的scan_date也修改为今天的日期
			Date today = DateUtil.getToday();
			if (!today.equals(task.getScanDate())) {
				task.setScanDate(today);
				taskService.update(task);
			}

			return;
		}

		int taskStatus = (int) task.getTaskStatus();
		if (task.getSettingTime().getTime() <= System.currentTimeMillis()) {
			// 如果当前时间已经大于了任务的setting_time则直接改为已触发状态
			this.updateTriggered(task);

		} else {
			task.setTaskStatus(taskStatus == TaskStatus.INITIALIZE.indexOf() ? TaskStatus.WAIT_TRIGGER.indexOf() : TaskStatus.RE_WAIT_TRIGGER.indexOf());
			task.setScanDate(DateUtil.getToday());
			task.setTaskBeginTime(null);
			task.setTaskEndTime(null);
			task.setRunTime(null);
			taskService.update(task);

			if (taskService.recheck(task)) {
				updateWaitTriggerTasks.add(task);
			}
		}
	}

	/**
	 * 选取指定日期内的状态是已触发/重做已触发的任务
	 * 
	 * @return
	 * @throws GatewayNotFoundException
	 */
	@Deprecated
	private Collection<Task> selectTasks(Date today) throws GatewayNotFoundException {
		String gateway = Configure.property(Configure.GATEWAY);

		Collection<Task> tasks = new ArrayList<Task>();
		tasks.addAll(this.selectTasksByGateway(gateway, today));
		tasks.addAll(this.selectTasksByCluster(gateway, today));

		return tasks;
	}

	/**
	 * 按指定网关机的配置选取已触发任务
	 * 
	 * @param gateway
	 * @return
	 * @throws GatewayNotFoundException
	 */
	private Collection<Task> selectTasksByGateway(String gateway, Date today) throws GatewayNotFoundException {
		// boolean isMainScheduler = Configure.property(Configure.MAIN_SCHEDULER, Boolean.class);

		// int taskRunningCount = scheduleSystemStatusService.getTaskRunningMax(gateway); //最大运行任务数
		int taskRunningPriority = scheduleSystemStatusService.getTaskRunningPriority(gateway); //作业运行的优先级(今日任务优先/昨日任务优先/按比例选取今日和昨日任务)
		boolean isTodayFirst = TaskRunningPriority.TODAY_FIRST.indexOf() == taskRunningPriority; //今日任务优先
		boolean isYesterdayFirst = TaskRunningPriority.YESTERDAY_FIRST.indexOf() == taskRunningPriority; //昨日任务优先
		boolean isPercent = TaskRunningPriority.PERCENT.indexOf() == taskRunningPriority; //按比例选取今日和昨日任务

		List<Task> todayTasks = new ArrayList<Task>();
		List<Task> yesterdayTasks = new ArrayList<Task>();

		// 如果当前网关机启用了白名单，但未添加白名单作业，则直接返回空集合
		if (scheduleSystemStatusService.isUseWhiteList(gateway) && !StringUtils.hasText(scheduleSystemStatusService.getWhiteListJobIds(gateway))) {
			return new ArrayList<Task>();
		}

		////////////////////////////////////////////////////////////////

		Criteria criteria = null;

		if (isTodayFirst || isPercent) {
			criteria = this.createSelectTaskCriteria();
			criteria.add(Restrictions.eq("taskDate", today));
			criteria.add(Restrictions.eq("scanDate", today));
			todayTasks = criteria.list();
			// log.info("直接从数据库中查询出来的今日任务个数: " + todayTasks.size());
		}

		if (isYesterdayFirst || isPercent) {
			criteria = this.createSelectTaskCriteria();
			criteria.add(Restrictions.or(Restrictions.eq("scanDate", today), Restrictions.eq("scanDate", DateUtil.getYesterday(today))));
			criteria.add(Restrictions.lt("taskDate", today));
			yesterdayTasks = criteria.list();
			// log.info("直接从数据库中查询出来的历史任务个数: " + yesterdayTasks.size());
		}

		Collection<Task> tasks = this.mergeTasksByPercent(todayTasks, yesterdayTasks, false, null);

		////////////////////////////////////////////////////////////////

		/*int yesterdayFactCount = yesterdayTasks.size(); // 本次查询出来的昨日任务的实际数量
		int todayFactCount = todayTasks.size(); // 本次查询出来的今日任务的实际数量
		int todaySelectCount = 0, yesterdaySelectCount = 0;
		if (isTodayFirst) {
			todaySelectCount = taskRunningCount; // 目标选取的今日任务数量
			log.info("任务选取规则: 仅选取今日任务.");

		} else if (isYesterdayFirst) {
			yesterdaySelectCount = taskRunningCount; //目标选取的昨日任务数量
			log.info("任务选取规则: 仅选取历史任务.");

		} else if (isPercent) {
			double percent = Configure.property(Configure.TODAY_WAIT_UPDATE_STATUS_TASK_SELECT_PERCENT, Double.class); // 今日和昨日任务的比例
			todaySelectCount = Math.min(todayFactCount, (int) Math.round(taskRunningCount * percent)); // 目标选取的今日任务数量
			yesterdaySelectCount = Math.min(yesterdayFactCount, taskRunningCount - todaySelectCount); // 目标选取的昨日任务数量

			int factTaskRunningCount = todaySelectCount + yesterdaySelectCount;
			if (factTaskRunningCount < taskRunningCount) {
				todaySelectCount += (taskRunningCount - factTaskRunningCount);
			}

			log.info("任务选取规则: 按比例选取 - " + percent + ".");
		}

		// 今天的任务已经执行完毕
		if (todayFactCount == 0 && isMainScheduler) {
			taskService.runComplete(today);
		}

		// 昨天(今天以前)的任务已经执行完毕
		if (yesterdayFactCount == 0 && isMainScheduler) {
			taskService.runCompleteExcludeToday(today);
		}

		Collection<Task> tasks = new ArrayList<Task>();

		int todayFactSelectCount = 0;
		if (todayFactCount <= todaySelectCount) {
			todayFactSelectCount = todayTasks.size();
			tasks.addAll(todayTasks);
		} else {
			Collection<Task> todaySelectTasks = todayTasks.subList(0, todaySelectCount);
			todayFactSelectCount = todaySelectTasks.size();
			tasks.addAll(todaySelectTasks);
		}
		log.info("在网关机(" + gateway + ")上, 今日已触发任务有 " + todayFactCount + " 个, 实际选取 " + todayFactSelectCount + " 个.");

		int yesterdayFactSelectCount = 0;
		if (yesterdayFactCount <= yesterdaySelectCount) {
			yesterdayFactSelectCount = yesterdayTasks.size();
			tasks.addAll(yesterdayTasks);
		} else {
			Collection<Task> yesterdaySelectTasks = yesterdayTasks.subList(0, yesterdaySelectCount);
			yesterdayFactSelectCount = yesterdaySelectTasks.size();
			tasks.addAll(yesterdaySelectTasks);
		}
		log.info("在网关机(" + gateway + ")上, 历史已触发任务有 " + yesterdayFactCount + " 个, 实际选取 " + yesterdayFactSelectCount + " 个.");
		log.info("在网关机(" + gateway + ")上, 已触发任务总计选取 " + tasks.size() + " 个, 以下是被选取到的任务清单:");*/

		for (Task t : tasks) {
			String executeGateway = t.getGateway();
			String message = t.toString();
			Date readyTime = t.getReadyTime();

			if (bigDataTaskService.isBigDataTask(t.getJobId())) {
				message += "<大>";
			}

			if (StringUtils.hasText(executeGateway)) {
				message += "[设定网关机:" + executeGateway + "]";
			}

			// 当前用户在前台人工修改为已触发状态时可能导致该时间为空,其他情况应该是不为空的
			if (readyTime != null) {
				message += ",readyTime:" + DateUtil.formatDateTime(readyTime);
			}

			message += ",scanDate:" + DateUtil.formatDate(t.getScanDate());

			log.info(message);
		}

		return tasks;
	}

    /**
     * <pre>
     * 模拟后台轮询时，根据网关机选取任务(普通任务)
     * 1. 考虑了禁止补数据时，不要选取历史任务
     * 2. 选取出来的任务，进行了校验，其父任务已经执行完成，前置已经执行完成，也就是说，选出来的，都是可以准备执行的任务
     * 3. 选取出来，准备提交执行的任务，按照今日与历史任务的比例进行选取
     * </pre>
     * @param gateway
     * @param today
     * @return
     * @throws GatewayNotFoundException
     */
	private Collection<Task> selectTasks4SimulateByGateway(String gateway, Date today) throws GatewayNotFoundException {
		int taskRunningPriority = scheduleSystemStatusService.getTaskRunningPriority(gateway); //作业运行的优先级(今日任务优先/昨日任务优先/按比例选取今日和昨日任务)
		boolean isTodayFirst = TaskRunningPriority.TODAY_FIRST.indexOf() == taskRunningPriority; //今日任务优先
		boolean isYesterdayFirst = TaskRunningPriority.YESTERDAY_FIRST.indexOf() == taskRunningPriority; //昨日任务优先
		boolean isPercent = TaskRunningPriority.PERCENT.indexOf() == taskRunningPriority; //按比例选取今日和昨日任务

		// 当前时间是否禁止补数据
		boolean isDisableSupply = false;
		// 取到网关机上面禁止补数据的时间点集合
		Collection<Integer> disableSupplyHours = scheduleSystemStatusService.getDisableSupplyHours(gateway);
		// 拿当前时间的Calendar.HOUR_OF_DAY与disableSupplyHours判断，当前是否禁止补数据：isDisableSupply
		if (disableSupplyHours.size() > 0) {
			Calendar calendar = DateUtil.getCalendar(new Date());
			isDisableSupply = disableSupplyHours.contains(calendar.get(Calendar.HOUR_OF_DAY));
		}

		List<Task> todayTasks = new ArrayList<Task>();
		List<Task> yesterdayTasks = new ArrayList<Task>();

		// 如果当前网关机启用了白名单，但未添加白名单作业，则直接返回空集合
		if (scheduleSystemStatusService.isUseWhiteList(gateway) && !StringUtils.hasText(scheduleSystemStatusService.getWhiteListJobIds(gateway))) {
			return new ArrayList<Task>();
		}

		////////////////////////////////////////////////////////////////

		Criteria criteria = null;

		if (isTodayFirst || isPercent) {
			criteria = this.createSelectTask4SimulateCriteria();
			criteria.add(Restrictions.eq("taskDate", today));
			todayTasks = criteria.list();
		}

		if (isYesterdayFirst || isPercent) {
			criteria = this.createSelectTask4SimulateCriteria();

			/**
			 * <pre>
			 * 	考虑到对补数据任务的控制,在选取历史任务时加了以下逻辑
			 * 	22:10-09:10		这段范围内是控制补数据的所以在选取时只取昨天一天的任务
			 * 	其他时间段		选取今天以前的所有历史任务
			 * </pre>
			 */
			/**
			 * 当选取普通任务的时候，如果当前时间点禁止补数据，则选取历史任务的时候，只选取taskDate是昨天的任务
			 * (在选取普通任务的历史任务时，这里其实略放开了范围。 真正的严格的逻辑应该是：
			 * 禁止补数据，且当前时间段是18:00-23:59，不选取任何taskDate不等于今日的任务;
			 * 禁止补数据，且当前时间段是0:00--9:00，则选取历史任务时，可以选取taskDate=昨天的任务)
			 * 如果当前时间点没有禁止补数据，则选取历史任务的时候，选取taskDate小于今天的任务
			 */
			if (isDisableSupply) {
				criteria.add(Restrictions.eq("taskDate", DateUtil.getYesterday()));
			} else {
				criteria.add(Restrictions.lt("taskDate", today));
			}

			yesterdayTasks = criteria.list();
		}

		Integer[] simulateSelectMaxNumber = Configure.property(Configure.SIMULATE_SELECT_MAX_NUMBERS, Integer[].class);
		int taskRunningCount = DateUtil.isTimeRange("00:00", "09:00") ? simulateSelectMaxNumber[0] : simulateSelectMaxNumber[1]; //最大运行任务数

		log.info("校验成功的今日正常任务清单:");
		taskService.validateSimulateScheduleTasks(todayTasks);

		log.info("校验成功的历史正常任务清单:");
		taskService.validateSimulateScheduleTasks(yesterdayTasks);

		List<Task> normalTasks = this.mergeTasksByPercent(todayTasks, yesterdayTasks, isDisableSupply, taskRunningCount);

		return normalTasks;
	}

	/**
	 * 按针对集群的作业类型运行数量配置信息选取已触发任务
	 * 
	 * @param gateway
	 * @param today
	 * @return
	 * @throws GatewayNotFoundException
	 */
	// select count(*) from task t right join concurrent c on t.job_type = c.job_type  and t.task_date = '2013-07-24'
	// select count(*) from task t  where  t.task_date = '2013-07-24' and t.job_type in (select job_type from concurrent)
	private Collection<Task> selectTasksByCluster(String gateway, Date today) throws GatewayNotFoundException {
		Collection<Task> tasks = new ArrayList<Task>();

		StringBuffer tempSql = new StringBuffer("select t.* from task t right join concurrent c on t.job_type = c.job_type where ");

		// 1.确保"待触发"周期的作业不被选取到
		String cycleTypeSql = "t.cycle_type != " + JobCycle.NONE.indexOf();

		// 2.按集群选取时不用考虑今天与昨天的比例，默认按扫描日期过滤后按readyTime排序就行了
		String scanDateSql = "t.scan_date = '" + DateUtil.formatDate(DateUtil.getToday()) + "'";

		// 3.选取“已触发”或“重做已触发”状态的任务
		String taskStatusSql = "(t.task_status = " + TaskStatus.TRIGGERED.indexOf() + " or t.task_status = " + TaskStatus.RE_TRIGGERED.indexOf() + ")";

		// 4.选取网关机配置中指定的作业类型的任务
		String gatewayJobTypeSql = "t.job_type in (" + scheduleSystemStatusService.getAllowExecuteJobTypes(gateway) + ")";

		// 5.选取符合执行尾号的作业
		String tailNumberSql = "substring(t.task_id, length(t.task_id), 1) in (" + scheduleSystemStatusService.getTailNumber(gateway) + ")";

		// 6.选取作业中配置的gateway跟传入的gateway一样的任务
		String gatewaySql = "t.gateway = '" + gateway + "'";

		// 7.网关机白名单
		String whiteListJobIds = scheduleSystemStatusService.getWhiteListJobIds(gateway);

		// 以上条件最终组合的效果应该是
		// 未启用白名单: 1 and 2 and 3 and ((4 and 5) or 6)
		// 启用白名单: 1 and 2 and 3 and 7
		// 注：如果“任务1”指定在“A网关机”运行，那通过这个SQL在“B网关机”上也是会取到“任务1”，这时需要在执行中心中进一步对“执行网关机”校验
		// 因为这里的条件用了and ((4 and 5) or 6)   所以符合该网关机配置的任务类型的，并且也符合该网关机配置的尾号规则的任务，会被选择出来，
		// 但是这些选择出来的任务，也许会被指定在其他网关机上运行。 所以提交这些任务执行之前，还要根据某个任务的具体的gateway，去判断一下，是否是这个网关机上执行的
		// 所以在执行中心，还需要进一步的校验。

		tempSql.append(cycleTypeSql);
		tempSql.append(" and ").append(scanDateSql);
		tempSql.append(" and ").append(taskStatusSql);

		if (scheduleSystemStatusService.isUseWhiteList(gateway)) {
			if (StringUtils.hasText(whiteListJobIds)) {
				tempSql.append(" and t.job_id in (").append(whiteListJobIds).append(")");
			} else {
				// 如果启用了白名单却又未指定白名单作业ID，则直接返回一个空集合
				return tasks;
			}
		} else {
			tempSql.append(" and ((").append(gatewayJobTypeSql).append(" and ").append(tailNumberSql).append(") or ").append(gatewaySql).append(")");
		}

		int i = 0;
		final StringBuffer sql = new StringBuffer();
		Collection<Integer> categories = concurrentService.getConcurrentCategories();
		for (Integer category : categories) {
			if (i > 0) {
				sql.append(" union ");
			}

			sql.append("(").append(tempSql.toString());
			sql.append(" and c.category = ").append(category);

			if (scheduleSystemStatusService.isReferJobLevel(gateway)) {
				sql.append(" order by t.job_level desc, t.ready_time, t.flag desc");
			} else {
				sql.append(" order by t.ready_time, t.flag desc, t.job_level desc");
			}

			// 设置指定作业类型分类中的所有作业类型最大任务运行任务数量
			sql.append(" limit ").append(concurrentService.getMaxConcurrentNumberByCategory(category.intValue()));
			sql.append(")");

			i += 1;
		}

		log.info("特殊任务查询SQL: " + sql);

		tasks = taskService.executeFind(new HibernateCallback<Collection<Task>>() {

			@Override
			public Collection<Task> doInHibernate(Session session) throws HibernateException {
				SQLQuery query = session.createSQLQuery(sql.toString());
				query.addEntity(Task.class);

				return query.list();
			}

		});

		if (tasks.size() > 0) {
			log.info("在网关机(" + gateway + ")上,特殊任务选取 " + tasks.size() + " 条, 以下是特殊任务清单:");
			for (Task t : tasks) {
				String executeGateway = t.getGateway();
				String message = t.toString();

				if (bigDataTaskService.isBigDataTask(t.getJobId())) {
					message += "<大>";
				}

				if (StringUtils.hasText(executeGateway)) {
					message += "[设定网关机:" + executeGateway + "]";
				}

				message += ",jobType:" + t.getJobType();

				log.info(message);
			}
		} else {
			log.info("在网关机(" + gateway + ")上,未选取到特殊任务.");
		}

		return tasks;
	}

	
	/**
	 * 
	 * 在网关机上，选取特殊类型的任务(如GP任务的选取)
	 * 
	 * @param gateway
	 * @param today
	 * @return
	 * @throws GatewayNotFoundException
	 */
	private Collection<Task> selectTasks4SimulateByCluster(String gateway, Date today) throws GatewayNotFoundException {
		// 当前时间是否禁止补数据
		boolean isDisableSupply = false;
		Calendar calendar = DateUtil.getCalendar(new Date());
		int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
		Collection<Integer> disableSupplyHours = scheduleSystemStatusService.getDisableSupplyHours(gateway);
		if (disableSupplyHours.size() > 0) {
			isDisableSupply = disableSupplyHours.contains(currentHour);
		}

		List<Task> todayTasks = new ArrayList<Task>();
		List<Task> yesterdayTasks = new ArrayList<Task>();

		final String todaySpecialSql = this.createSelectTasks4SimulateByClusterSql(true, isDisableSupply);
		final String yesterdaySpecialSql = this.createSelectTasks4SimulateByClusterSql(false, isDisableSupply);

		if (todaySpecialSql != null) {
			log.info("今日特殊任务查询SQL: " + todaySpecialSql);
			todayTasks = taskService.executeFind(new HibernateCallback<Collection<Task>>() {

				@Override
				public Collection<Task> doInHibernate(Session session) throws HibernateException {
					SQLQuery query = session.createSQLQuery(todaySpecialSql);
					query.addEntity(Task.class);

					return query.list();
				}

			});
		}

		if (yesterdaySpecialSql != null) {
			log.info("历史特殊任务查询SQL: " + yesterdaySpecialSql);
			yesterdayTasks = taskService.executeFind(new HibernateCallback<Collection<Task>>() {

				@Override
				public Collection<Task> doInHibernate(Session session) throws HibernateException {
					SQLQuery query = session.createSQLQuery(yesterdaySpecialSql);
					query.addEntity(Task.class);

					return query.list();
				}

			});
		}

		//这里一定要先进行校验。 再按照一定的比例选取通过校验的今日特殊(GP)任务和历史特殊(GP)任务.
		//不然的话，因为特殊任务本来名额就比较少，最后进行校验的话，很可能又被过滤掉，这样就会出现特殊任务运行延迟了。
		//之前经常出现0-9点GP任务启动延迟，因为这个时间段，大量GP任务的setting_time没有满足，所以在验证阶段被过滤掉了，所以启动不起来
		//而在白天重跑的那些GP任务，能正常运行，是因为setting_time都满足，所以被过滤几率小，所以白天的时候，很难复现
		log.info("校验成功的今日特殊任务清单:");
		taskService.validateSimulateScheduleTasks(todayTasks);

		log.info("校验成功的历史特殊任务清单:");
		taskService.validateSimulateScheduleTasks(yesterdayTasks);

		// 按设置的比例合并今日与历史任务
		List<Task> tasks = this.mergeTasksByPercent(todayTasks, yesterdayTasks, isDisableSupply, null);
		
		//到这里，按照比例选取出来了符合运行条件的今日特殊任务和历史特殊任务
		
		
        //下面的代码，对上面符合运行条件的任务集合进行循环，然后判断在某个特殊任务分类下，是否已经达到该分类设定的最大并发数量，如果已经达到，则过滤掉接下来的任务
		//这里其实是存在一定缺陷的，在根据分类最大并发量判断的时候，有可能把本次选取到的特殊任务全部过滤掉了，导致本次虽然选取到了特殊任务，但是特殊任务提交执行不起来。但是这个问题在现实中并不算大，因为终将会有机会被提交执行起来的
		if (tasks.size() > 0) {
			Map<Integer, Collection<Long>> concurrentCategoryAndJobMapping = new HashMap<Integer, Collection<Long>>();
			Collection<Task> newTasks = new ArrayList<Task>();

			for (Task t : tasks) {
				long jobId = t.getJobId();
				long jobType = t.getJobType();
				boolean isIgnore = false;
				StringBuilder message = new StringBuilder();

				Integer category = concurrentService.getConcurrentCategory(jobType);
				String categoryName = concurrentService.getCategoryName(category);
				int concurrentMaxNumber = concurrentService.getMaxConcurrentNumberByCategory(category);
				Collection<Long> allowRunningJobs = concurrentCategoryAndJobMapping.get(category);
				if (allowRunningJobs == null) {
					allowRunningJobs = new HashSet<Long>();
				}

				// 当前分类下的任务已经超过并发最大数
				if (allowRunningJobs.size() >= concurrentMaxNumber) {
					isIgnore = true;
					message.append("[并发数忽略]已达到分类(").append(categoryName).append(")最大并发数量(").append(concurrentMaxNumber).append(") - ");
					// continue;
				}

				if (StringUtils.hasText(t.getPreTasks()) || StringUtils.hasText(t.getPreTasksFromOperate())) {
					// 如果有前置作业则只允许加入第一个任务
					if (allowRunningJobs.contains(jobId)) {
						isIgnore = true;
						message.append("[前置忽略]相同作业ID的前置任务已经加入执行队列 - ");
						// continue;
					}
				}

				// 已加入的作业ID
				if (!isIgnore) {
					newTasks.add(t);
					allowRunningJobs.add(jobId);
					concurrentCategoryAndJobMapping.put(category, allowRunningJobs);
				}

				message.append(t.toString());
				message.append("[分类: ").append(categoryName).append("],");

				if (bigDataTaskService.isBigDataTask(t.getJobId())) {
					message.append("<大>");
				}

				String executeGateway = t.getGateway();
				if (StringUtils.hasText(executeGateway)) {
					message.append("[设定网关机:" + executeGateway + "]");
				}

				message.append(",jobType:" + t.getJobType());

				log.info(message);
			}

			log.info("在网关机(" + gateway + ")上,特殊任务共选取 " + tasks.size() + " 条, 按分类并发数过滤后保留 " + newTasks.size() + " 条");

			return newTasks;

		} else {
			log.info("在网关机(" + gateway + ")上,未选取到特殊任务.");
		}

		return tasks;
	}

	private Criteria createSelectTaskCriteria() throws GatewayNotFoundException {
		String gateway = Configure.property(Configure.GATEWAY);

		Criteria criteria = taskService.createCriteria();

		// 1.确保"待触发"周期的作业不被选取到
		Criterion cycleTypeCriterion = Restrictions.not(Restrictions.eq("cycleType", JobCycle.NONE.indexOf()));

		// 2.选取“已触发”或“重做已触发”状态的任务
		Criterion taskStatusCriterion = Restrictions.or(Restrictions.eq("taskStatus", (long) TaskStatus.TRIGGERED.indexOf()), Restrictions.eq("taskStatus", (long) TaskStatus.RE_TRIGGERED.indexOf()));

		// 3.排除并发配置中设置的作业类型(配置中的作业类型会在针对集群选取时单独查询)
		Criterion concurrentJobTypeCriterion = Restrictions.not(Restrictions.in("jobType", concurrentService.getConcurrentJobTypes()));

		// 4.选取网关机配置中指定的作业类型的任务
		Criterion gatewayJobTypeCriterion = Restrictions.in("jobType", scheduleSystemStatusService.getAllowExecuteJobTypesAsArray(gateway));

		// 5.选取符合执行尾号的作业
		Criterion tailNumberCriterion = Restrictions.sqlRestriction("substring(task_id, length(task_id), 1) in (" + scheduleSystemStatusService.getTailNumber(gateway) + ")");

		// 6.选取作业中配置的gateway与当前传入的gateway相同的任务
		Criterion gatewayCriterion = Restrictions.eq("gateway", gateway);

		// 7.网关机白名单
		Long[] whiteListJobIds = scheduleSystemStatusService.getWhiteListJobIdsAsArray(gateway);

		// 以上条件最终组合的效果应该是
		// 未启用白名单: 1 and 2 and 3 and ((4 and 5) or 6)
		// 启用白名单: 1 and 2 and 3 and 7
		// 注：如果“任务1”指定在“A网关机”运行，那通过这个SQL在“B网关机”上也是会取到“任务1”，这时需要在执行中心中进一步对“执行网关机”校验
		criteria.add(cycleTypeCriterion);
		criteria.add(taskStatusCriterion);
		criteria.add(concurrentJobTypeCriterion);

		if (scheduleSystemStatusService.isUseWhiteList(gateway)) {
			if (whiteListJobIds != null && whiteListJobIds.length > 0) {
				criteria.add(Restrictions.in("jobId", whiteListJobIds));
			}
		} else {
			criteria.add(Restrictions.or(Restrictions.and(gatewayJobTypeCriterion, tailNumberCriterion), gatewayCriterion));
		}

		if (scheduleSystemStatusService.isReferJobLevel(gateway)) {
			criteria.addOrder(Order.desc("jobLevel")); //此种排序主要参照jobLevel,只要准备好了,谁的作业优先级高,谁就先运行,能尽快保证作业优先级高的任务优先被执行.
			criteria.addOrder(Order.asc("readyTime"));
			criteria.addOrder(Order.desc("flag"));
		} else {
			criteria.addOrder(Order.asc("readyTime")); //此种排序主要参照readyTime,谁先准备好,谁先运行.
			criteria.addOrder(Order.desc("flag"));
			criteria.addOrder(Order.desc("jobLevel"));
		}

		// 设置网关机最大运行任务数
		criteria.setMaxResults(scheduleSystemStatusService.getTaskRunningMax(gateway));

		return criteria;
	}

	/**
	 * 选取任务 模拟后台轮询方式，选取任务的方法
	 * 这里选取出来的任务，只是那些可以在这台网关机上面运行的任务，至于此次轮询时是否已经满足了运行的前提条件（父任务，前置任务等），尚未进行验证 1.
	 * 选取任务的时候，排除掉那些“待触发”类型的任务，这些任务不是被后台轮询的 2.
	 * 选取任务的时候，选取任务状态是：“初始化”、“未触发”、“已触发”、“重做初始化”、“重做未触发”、“重做已触发”状态的任务 3.
	 * 并且考虑任务的类型不在特殊任务并发配置表中涉及的
	 * (目前任务分两大类：在特殊任务并发配置表中涉及到的任务类型，都算特殊任务，单独选取；没有涉及到的，算普通任务)
	 * 
	 * 4. 选取任务的时候，还要考虑网关机配置表中，配置的该网关机允许执行的任务类型 5.
	 * 选取任务的时候，还要考虑网关机配置表中，配置的该网关机允许执行的任务尾号集合 6. 选取任务的时候，还要考虑作业配置时，有没有指定特定网关机执行
	 * 7. 选取任务的时候，还要考虑网关机有没有配置作业白名单
	 * 
	 * @return
	 * @throws GatewayNotFoundException
	 */
	private Criteria createSelectTask4SimulateCriteria() throws GatewayNotFoundException {
		String gateway = Configure.property(Configure.GATEWAY);

		Criteria criteria = taskService.createCriteria();

		// 1.确保"待触发"周期的作业不被选取到
		Criterion cycleTypeCriterion = Restrictions.not(Restrictions.eq("cycleType", JobCycle.NONE.indexOf()));

		// 2.选取“初始化”、“未触发”、“已触发”、“重做初始化”、“重做未触发”、“重做已触发”状态的任务
		Criterion taskStatusCriterion = Restrictions.in("taskStatus", new Long[] { (long) TaskStatus.INITIALIZE.indexOf(), (long) TaskStatus.WAIT_TRIGGER.indexOf(),
				(long) TaskStatus.TRIGGERED.indexOf(), (long) TaskStatus.RE_INITIALIZE.indexOf(), (long) TaskStatus.RE_WAIT_TRIGGER.indexOf(), (long) TaskStatus.RE_TRIGGERED.indexOf() });

		// 3.选取失败次数小于2次的作业
		/*Criterion failureTimesCriterion = Restrictions
				.and(Restrictions.in("taskStatus", new Long[] { (long) TaskStatus.RUN_FAILURE.indexOf(), (long) TaskStatus.RE_RUN_FAILURE.indexOf() }), Restrictions.or(Restrictions
						.isNull("failureRerunTimes"), Restrictions.lt("failureRerunTimes", 2)));*/
		// 3.排除并发配置中设置的作业类型(配置中的作业类型会在针对集群选取时单独查询)
		Criterion concurrentJobTypeCriterion = Restrictions.not(Restrictions.in("jobType", concurrentService.getConcurrentJobTypes()));

		// 4.选取网关机配置中指定的作业类型的任务
		Criterion gatewayJobTypeCriterion = Restrictions.in("jobType", scheduleSystemStatusService.getAllowExecuteJobTypesAsArray(gateway));

		// 5.选取符合执行尾号的作业
		Criterion tailNumberCriterion = Restrictions.sqlRestriction("substring(task_id, length(task_id), 1) in (" + scheduleSystemStatusService.getTailNumber(gateway) + ")");

		// 6.选取作业中配置的gateway与当前传入的gateway相同的任务
		Criterion gatewayCriterion = Restrictions.eq("gateway", gateway);

		// 7.网关机白名单(注：配置白名单作业，表名该网关机只运行白名单中配置的作业，其他作业一律不运行；此时需要做双向绑定：在网关机配置表中，配置这些白名单作业ID，在作业中，指定到这台网关机上)
		Long[] whiteListJobIds = scheduleSystemStatusService.getWhiteListJobIdsAsArray(gateway);

		// 以上条件最终组合的效果应该是
		// 未启用白名单: 1 and 2 and 3 and ((4 and 5) or 6)
		// 启用白名单: 1 and 2 and 3 and 7
		// 注：如果“任务1”指定在“A网关机”运行，那通过这个SQL在“B网关机”上也是会取到“任务1”，这时需要在执行中心中进一步对“执行网关机”校验
		// 某个任务，它的作业类型和任务尾号都符合网关机A的要求， 但是被指定到了B网关机上面执行，这个时候，根据上面的查询SQL逻辑，网关机A和网关机B都能选取到这个任务
		// 所以要在执行中心，提交执行之前，进一步根据任务的gateway字段来校验和判断
		criteria.add(cycleTypeCriterion);
		criteria.add(taskStatusCriterion);
		criteria.add(concurrentJobTypeCriterion);
		// criteria.add(Restrictions.or(taskStatusCriterion, failureTimesCriterion));

		if (scheduleSystemStatusService.isUseWhiteList(gateway)) {
			if (whiteListJobIds != null && whiteListJobIds.length > 0) {
				criteria.add(Restrictions.in("jobId", whiteListJobIds));
			}
		} else {
			criteria.add(Restrictions.or(Restrictions.and(gatewayJobTypeCriterion, tailNumberCriterion), gatewayCriterion));
		}

		// 当前时间是否0-9点
		boolean isZero2Nine = DateUtil.isTimeRange("00:00", "09:00");

		// 按比例控制TaskID的排序方向
		// 0-9点		80%升序, 20%降序
		// 其他时间	60%升序, 40%降序
		/**
		 * 模拟后台轮询方式，在选取任务的时候，任务的选取顺序做了一个特殊的处理：0-9点 TaskID 80%升序, 20%降序；其他时间
		 * TaskID 60%升序, 40%降序 这样做的目的是：
		 * 因为在0-9点，主要是系统后台自动轮询，所以一般taskid比较小的任务，在任务层次中处于较高层次，一般较先运行是符合常理的。
		 * 但是也不排除后来上线的某个任务，处于任务层次中较高的层次，所以还要安排20%的反序查询。
		 * 在0-9点时间段以外，很有可能有许多人为的重跑操作
		 * ，被重跑的任务，可能taskid较大，所以这个时候，得安排40%降序查询，以便那些taskid较大的任务能及时得到轮询。
		 */
		boolean[] directions = isZero2Nine ? new boolean[] { true, true, true, true, true, true, true, true, false, false } : new boolean[] { true, true, true, true, true, true, false, false, false,
				false };
		boolean direction = directions[new Random().nextInt(directions.length)];
		criteria.addOrder(direction ? Order.asc("taskId") : Order.desc("taskId"));

		// 设置网关机最大运行任务数(0-9点取第一个,其他时段取第二个)
		Integer[] simulateSelectMaxNumbers = Configure.property(Configure.SIMULATE_SELECT_MAX_NUMBERS, Integer[].class);
		criteria.setMaxResults(isZero2Nine ? simulateSelectMaxNumbers[0] : simulateSelectMaxNumbers[1]);

		return criteria;
	}

	
	/**
	 * 根据并发配置表，选取特殊类型的任务
	 * 这里选取出来的任务，只是那些可以在这台网关机上面运行的任务，至于此次轮询时是否已经满足了运行的前提条件（父任务，前置任务等），尚未进行验证
	 * 从任务集合中，选取出那些job_type跟特殊任务并发配置表中的job_type匹配的任务，再继续根据其他条件进行筛选
	 * 1. 选取任务的时候，排除掉那些“待触发”类型的任务，这些任务不是被后台轮询的
	 * 2. 选取扫描日期是今天的， 并且根据task_date再做进一步的过滤
	 * 3. 选取任务的时候，选取任务状态是：“初始化”、“未触发”、“已触发”、“重做初始化”、“重做未触发”、“重做已触发”状态的任务
	 * 4. 选取任务的时候，还要考虑网关机配置表中，配置的该网关机允许执行的任务类型
	 * 5. 选取任务的时候，还要考虑网关机配置表中，配置的该网关机允许执行的任务尾号集合
	 * 6. 选取任务的时候，还要考虑作业配置时，有没有指定特定网关机执行
	 * 7. 选取任务的时候，还要考虑网关机有没有配置作业白名单
	 * @param isSelectToday
	 * @param isDisableSupply
	 * @return
	 * @throws GatewayNotFoundException
	 */
	private String createSelectTasks4SimulateByClusterSql(boolean isSelectToday, boolean isDisableSupply) throws GatewayNotFoundException {
		String gateway = Configure.property(Configure.GATEWAY);

		// 如果当前已禁补时间点了则判断当前时间点是否18-23间
		// 如果在这范围内则不再允许取前一任务日期的作业了
		Calendar calendar = DateUtil.getCalendar(new Date());
		int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
		if (isDisableSupply && (currentHour >= 18 && currentHour <= 23) && !isSelectToday) {
			return null;
		}

		// 从任务集合中，选取出那些job_type跟特殊任务并发配置表中的job_type匹配的任务，再继续根据其他条件进行筛选
		StringBuffer tempSql = new StringBuffer("select t.* from task t right join concurrent c on t.job_type = c.job_type where ");

		// 1.确保"待触发"周期的作业不被选取到
		String cycleTypeSql = "t.cycle_type != " + JobCycle.NONE.indexOf();

		// 2.按集群选取时不用考虑今天与昨天的比例，默认按扫描日期过滤后按readyTime排序就行了
		String today = DateUtil.formatDate(DateUtil.getToday());
		String yesterday = DateUtil.formatDate(DateUtil.getYesterday());
		String scanDateSql = "(t.scan_date = '" + today + "'";  //选择扫描日期是今天的
		if (isSelectToday) {
			scanDateSql += " and t.task_date = '" + today + "')";
		} else {
			if (isDisableSupply) {
				scanDateSql += " and t.task_date = '" + yesterday + "')";
			} else {
				scanDateSql += " and t.task_date < '" + today + "')";
			}
		}

		// 3.选取“初始化”、“未触发”、“已触发”、“重做初始化”、“重做未触发”、“重做已触发”状态的任务
		String taskStatusSql = "(t.task_status = " + TaskStatus.INITIALIZE.indexOf() + " or t.task_status = " + TaskStatus.RE_INITIALIZE.indexOf() + " or t.task_status = " +
				TaskStatus.WAIT_TRIGGER.indexOf() + " or t.task_status = " + TaskStatus.RE_WAIT_TRIGGER.indexOf() + " or t.task_status = " + TaskStatus.TRIGGERED.indexOf() + " or t.task_status = " +
				TaskStatus.RE_TRIGGERED.indexOf() + ")";

		// 4.选取网关机配置中指定的作业类型的任务
		String gatewayJobTypeSql = "t.job_type in (" + scheduleSystemStatusService.getAllowExecuteJobTypes(gateway) + ")";

		// 5.选取符合执行尾号的作业
		String tailNumberSql = "substring(t.task_id, length(t.task_id), 1) in (" + scheduleSystemStatusService.getTailNumber(gateway) + ")";

		// 6.选取作业中配置的gateway跟传入的gateway一样的任务
		String gatewaySql = "t.gateway = '" + gateway + "'";

		// 7.网关机白名单
		String whiteListJobIds = scheduleSystemStatusService.getWhiteListJobIds(gateway);

		// 以上条件最终组合的效果应该是
		// 未启用白名单: 1 and 2 and 3 and ((4 and 5) or 6)
		// 启用白名单: 1 and 2 and 3 and 7
		// 注：如果“任务1”指定在“A网关机”运行，那通过这个SQL在“B网关机”上也是会取到“任务1”，这时需要在执行中心中进一步对“执行网关机”校验
		// 因为这里的条件用了and ((4 and 5) or 6)   所以符合该网关机配置的任务类型的，并且也符合该网关机配置的尾号规则的任务，会被选择出来，
		// 但是这些选择出来的任务，也许会被指定在其他网关机上运行。 所以提交这些任务执行之前，还要根据某个任务的具体的gateway，去判断一下，是否是这个网关机上执行的
		// 所以在执行中心，还需要进一步的校验。

		tempSql.append(cycleTypeSql);
		tempSql.append(" and ").append(scanDateSql);
		tempSql.append(" and ").append(taskStatusSql);

		if (scheduleSystemStatusService.isUseWhiteList(gateway)) {
			if (StringUtils.hasText(whiteListJobIds)) {
				tempSql.append(" and t.job_id in (").append(whiteListJobIds).append(")");
			} else {
				// 如果启用了白名单却又未指定白名单作业ID，则直接返回一个空集合
				return null;
			}
		} else {
			tempSql.append(" and ((").append(gatewayJobTypeSql).append(" and ").append(tailNumberSql).append(") or ").append(gatewaySql).append(")");
		}

		int i = 0;
		StringBuffer sql = new StringBuffer();
		Collection<Integer> categories = concurrentService.getConcurrentCategories();
		for (Integer category : categories) {
			if (i > 0) {
				sql.append(" union ");
			}

			sql.append("(").append(tempSql.toString());
			sql.append(" and c.category = ").append(category);
			sql.append(" order by task_date");

			// 设置指定作业类型分类中的所有作业类型最大任务运行任务数量
			/**
			 * <pre>
			 * 	设置指定作业类型分类中的所有作业类型最大任务运行任务数量
			 * 	
			 * 	2014-07-04
			 * 	由于这里限制了记录数会导致选取到多个需要串行的作业从而降低了并行作业的选取效率
			 * 	因为这里不作限制会在后续处理每种分类的最大数据
			 * </pre>
			 */
			sql.append(")");

			i += 1;
		}

		return sql.toString();
	}

	/**
	 * 处理特殊任务
	 * 
	 * <pre>
	 * 	1、按并发配置中的分类计算出各分类的最大并发数、正在运行数和实际可提交数
	 *  2、按上面计算所得的各数值筛选出各分类本次理论可提交的所有任务
	 * </pre>
	 * 
	 * @param tasks
	 * @throws GatewayNotFoundException
	 */
	private void processSpecialTasksByConcurrent(Collection<Task> tasks) throws GatewayNotFoundException {
		String gateway = Configure.property(Configure.GATEWAY);

		// 每个作业类型分类下正在运行的任务数量
		Map<Integer, Collection<Action>> countCategoryMapping = actionService.countTodayRunningTasksByConcurrentJobType(gateway);

		// 每个分类的数量信息
		Map<Integer, ConcurrentCategory> concurrentCategoryMapping = new HashMap<Integer, ConcurrentCategory>();

		for (Entry<Integer, Collection<Action>> entry : countCategoryMapping.entrySet()) {
			int category = entry.getKey();
			Collection<Action> runningActions = entry.getValue();

			ConcurrentCategory concurrentCategory = new ConcurrentCategory(category);
			concurrentCategory.setCategoryName(concurrentService.getCategoryName(category));

			////////////////////////////////////////////////////////////////

			// 该分类下大数据任务当前正在运行的数量
			int categoryBigDataRunningCount = 0;
			log.info("属于\"" + concurrentCategory.getCategoryName() + "\"分类的有 " + runningActions.size() + " 条任务正在运行, 以下是运行任务的清单:");
			for (Action runningAction : runningActions) {
				String message = "正在运行作业[作业ID: " + runningAction.getJobId() + ",名称:" + runningAction.getJobName() + "]";

				if (bigDataTaskService.isBigDataTask(runningAction.getJobId())) {
					categoryBigDataRunningCount += 1;
					message += "<大>";
				}

				log.info(message);
			}

			// 该分类下大数据任务最大并发运行数量
			concurrentCategory.setBigDataMaxConcurrentNumber(concurrentService.getBigDataMaxConcurrentNumberByCategory(category));
			int categoryBigDataMaxConcurrentNumber = concurrentService.getBigDataMaxConcurrentNumberByCategory(category);

			// 该分类下还能运行的大任务数量
			int categoryBigDataQuota = Math.max(categoryBigDataMaxConcurrentNumber - categoryBigDataRunningCount, 0);

			concurrentCategory.setBigDataRunningNumber(categoryBigDataRunningCount);
			concurrentCategory.setBigDataSubmitQuota(categoryBigDataQuota);

			////////////////////////////////////////////////////////////////

			// 该分类当前正在运行的数量
			int categoryRunningCount = runningActions.size();

			// 该分类最大并发运行数量
			int categoryMaxConcurrentNumber = concurrentService.getMaxConcurrentNumberByCategory(category);

			// 该分类还能运行的数量
			// 因为最大并发数是包含了大数据最大并发数的，所以这里还需要减去大数据任务的配额数量，如：
			// 最大并发设置了10条，大数据并发设置了2条，那意味着如果大数据允许执行2条，那非大数据就只能执行8条
			int categoryQuota = Math.max(Math.max(categoryMaxConcurrentNumber - categoryRunningCount, 0) - categoryBigDataQuota, 0);

			concurrentCategory.setRunningNumber(categoryRunningCount);
			concurrentCategory.setMaxConcurrentNumber(categoryMaxConcurrentNumber);
			concurrentCategory.setSubmitQuota(categoryQuota);

			concurrentCategoryMapping.put(category, concurrentCategory);
		}

		///////////////////////////////////////////////////////////////////////////////////////////////////////////////

		// 保留被删除的非大数据任务,当大数据任务的配额未用完,则用这些非大数据任务填补
		Map<Integer, Collection<Task>> categoryRemoveTaskMapping = new HashMap<Integer, Collection<Task>>();

		for (Iterator<Task> iter = tasks.iterator(); iter.hasNext();) {
			Task task = iter.next();

			String executeGateway = task.getGateway();
			long jobId = task.getJobId();
			int jobType = task.getJobType() == null ? 0 : task.getJobType().intValue();
			Integer category = concurrentService.getConcurrentCategory(jobType);

			// 当前任务是否不在当前网关机执行
			boolean isNotExecuteAtGateway = StringUtils.hasText(executeGateway) && !gateway.equals(executeGateway);

			// 如果指定作业类型在并发表中未配置分类则保留该任务
			if (category == null) {
				continue;
			}

			// 如果该分类未找到相应的数量统计信息则保留该任务
			ConcurrentCategory concurrentCategory = concurrentCategoryMapping.get(category);
			if (concurrentCategory == null) {
				continue;
			}

			if (bigDataTaskService.isBigDataTask(jobId)) {
				concurrentCategory.addBigDataSelectNumber();

				if (isNotExecuteAtGateway) {
					// 排除掉不在当前网关机执行的任务
					iter.remove();
					concurrentCategory.addBigDataRemoveNotGatewayNumber();
					continue;
				}

				if (concurrentCategory.hasBigDataSubmitNumber()) {
					concurrentCategory.minusBigDataSubmitNumber();
					concurrentCategory.addBigDataActualSubmitNumber();
				} else {
					// 如果该分类的大数据配额数已用完则删除该任务
					iter.remove();
					concurrentCategory.addBigDataRemoveNumber();
				}

			} else {
				concurrentCategory.addSelectNumber();

				if (isNotExecuteAtGateway) {
					// 排除掉不在当前网关机执行的任务
					iter.remove();
					concurrentCategory.addRemoveNotGatewayNumber();
					continue;
				}

				if (concurrentCategory.hasSubmitNumber()) {
					concurrentCategory.minusSubmitNumber();
					concurrentCategory.addActualSubmitNumber();
				} else {
					// 保留该分类中被删除的非大数据任务
					Collection<Task> removeTasks = categoryRemoveTaskMapping.get(category);
					if (removeTasks == null) {
						removeTasks = new ArrayList<Task>();
					}
					removeTasks.add(task);
					categoryRemoveTaskMapping.put(category, removeTasks);

					// 如果该分类的大数据配额数已用完则删除该任务
					iter.remove();
					concurrentCategory.addRemoveNumber();
				}

			}

		}

		// 判断大数据的配额是否未被用完,如果还有配额则可以将非大数据的任务进行填补

		for (Entry<Integer, ConcurrentCategory> entry : concurrentCategoryMapping.entrySet()) {
			ConcurrentCategory cc = entry.getValue();

			// 计算大数据实际提交与配额间的差额
			int bigDataDifference = cc.getBigDataSubmitQuota() - cc.getBigDataActualSubmitNumber();

			// 计算非大数据任务实际提交与配额间的差额
			int difference = cc.getSubmitQuota() - cc.getActualSubmitNumber();

			// 计算非大数据任务中还允许填补的数量
			int fillNumber = Math.min(bigDataDifference, difference);

			cc.setSubmitQuota(cc.getSubmitQuota() + fillNumber);

			if (fillNumber > 0) {
				// 将该分类下已经删除的非大数据任务按计算后的填补值进行填补
				Collection<Task> removeTasks = categoryRemoveTaskMapping.get(entry.getKey());
				if (removeTasks != null && removeTasks.size() > 0) {
					Iterator<Task> iter = tasks.iterator();
					for (; fillNumber > 0; fillNumber--) {
						tasks.add(iter.next());

						// 因为是回填所以需要将实际已排除的值减回去,实际提交的值加上去
						cc.minusRemoveNumber();
						cc.addActualSubmitNumber();
					}
				}
			}

			/////////////////////// 以下代码仅用于日志的输出，与逻辑无任何关系 ////////////////////////////

			String tempLog = "属于\"%1$s\"分类的%2$s作业: 最大并发 %3$d 条, 正在运行 %4$d 条, 还能提交 %5$d 条, 本次选取 %6$d 条, 实际提交 %7$d 条, 实际排除 %8$d 条."; // , 非本网关机排除 %8$d 条

			if (cc.getSelectNumber() > 0 || cc.getBigDataSelectNumber() > 0) {
				String categoryName = cc.getCategoryName(); // concurrentService.getCategoryName(cc.getCategory());
				log.info(String.format(tempLog, new Object[] { categoryName, "大数据", cc.getBigDataMaxConcurrentNumber(), cc.getBigDataRunningNumber(), cc.getBigDataSubmitQuota(),
						cc.getBigDataSelectNumber(), cc.getBigDataActualSubmitNumber(), cc.getBigDataRemoveNumber() }));
				log.info(String.format(tempLog, new Object[] { categoryName, "所有", cc.getMaxConcurrentNumber(), cc.getRunningNumber(), cc.getSubmitQuota(), cc.getSelectNumber(),
						cc.getActualSubmitNumber(), cc.getRemoveNumber() }));
			}
		}
	}

	/**
	 * 按今日/历史的百分比合并任务
	 * 
	 * @param todayTasks
	 * @param yesterdayTasks
	 * @param isDisableSupply
	 * @param gatewayMaxRunningCount
	 *            当前网关机最大运行数量
	 * @return
	 * @throws GatewayNotFoundException
	 */
	private List<Task> mergeTasksByPercent(List<Task> todayTasks, List<Task> yesterdayTasks, boolean isDisableSupply, Integer gatewayMaxRunningCount) throws GatewayNotFoundException {
		String gateway = Configure.property(Configure.GATEWAY, String.class);
		boolean isMainScheduler = Configure.property(Configure.MAIN_SCHEDULER, Boolean.class);

		int taskRunningPriority = scheduleSystemStatusService.getTaskRunningPriority(gateway); //作业运行的优先级(今日任务优先/昨日任务优先/按比例选取今日和昨日任务)
		boolean isTodayFirst = TaskRunningPriority.TODAY_FIRST.indexOf() == taskRunningPriority; //今日任务优先
		boolean isYesterdayFirst = TaskRunningPriority.YESTERDAY_FIRST.indexOf() == taskRunningPriority; //昨日任务优先
		boolean isPercent = TaskRunningPriority.PERCENT.indexOf() == taskRunningPriority; //按比例选取今日和昨日任务
		if (gatewayMaxRunningCount == null) {
			gatewayMaxRunningCount = scheduleSystemStatusService.getTaskRunningMax(gateway); //最大运行任务数
		}

		Date today = DateUtil.getToday();

		int yesterdayFactCount = yesterdayTasks.size(); // 本次查询出来的昨日任务的实际数量
		int todayFactCount = todayTasks.size(); // 本次查询出来的今日任务的实际数量
		int todaySelectCount = 0, yesterdaySelectCount = 0;
		if (isTodayFirst) {
			todaySelectCount = gatewayMaxRunningCount; // 目标选取的今日任务数量
			log.info("任务选取规则: 仅选取今日任务.");

		} else if (isYesterdayFirst) {
			yesterdaySelectCount = gatewayMaxRunningCount; // 目标选取的昨日任务数量
			log.info("任务选取规则: 仅选取历史任务.");

		} else if (isPercent) {
			double percent = Configure.property(Configure.TODAY_WAIT_UPDATE_STATUS_TASK_SELECT_PERCENT, Double.class); // 今日和昨日任务的比例
			todaySelectCount = Math.min(todayFactCount, (int) Math.round(gatewayMaxRunningCount * percent)); // 目标选取的今日任务数量
			yesterdaySelectCount = Math.min(yesterdayFactCount, gatewayMaxRunningCount - todaySelectCount); // 目标选取的昨日任务数量

			int factTaskRunningCount = todaySelectCount + yesterdaySelectCount;
			if (factTaskRunningCount < gatewayMaxRunningCount) {
				todaySelectCount += (gatewayMaxRunningCount - factTaskRunningCount);
			}

			log.info("任务选取规则: 按比例选取 - " + percent + ".");
		}

		// 今天的任务已经执行完毕
		if (todayFactCount == 0 && isMainScheduler) {
			taskService.runComplete(today);
		}

		// 昨天(今天以前)的任务已经执行完毕
		if (yesterdayFactCount == 0 && isMainScheduler) {
			if (isDisableSupply) {
				taskService.runComplete(DateUtil.getYesterday());
			} else {
				taskService.runCompleteExcludeToday(today);
			}
		}

		List<Task> tasks = new ArrayList<Task>();

		int todayFactSelectCount = 0;
		if (todayFactCount <= todaySelectCount) {
			todayFactSelectCount = todayTasks.size();
			tasks.addAll(todayTasks);
		} else {
			Collection<Task> todaySelectTasks = todayTasks.subList(0, todaySelectCount);
			todayFactSelectCount = todaySelectTasks.size();
			tasks.addAll(todaySelectTasks);
		}
		log.info("在网关机(" + gateway + ")上, 今日未成功任务有 " + todayFactCount + " 个, 实际选取 " + todayFactSelectCount + " 个.");

		int yesterdayFactSelectCount = 0;
		if (yesterdayFactCount <= yesterdaySelectCount) {
			yesterdayFactSelectCount = yesterdayTasks.size();
			tasks.addAll(yesterdayTasks);
		} else {
			Collection<Task> yesterdaySelectTasks = yesterdayTasks.subList(0, yesterdaySelectCount);
			yesterdayFactSelectCount = yesterdaySelectTasks.size();
			tasks.addAll(yesterdaySelectTasks);
		}
		log.info("在网关机(" + gateway + ")上, 历史未成功任务有 " + yesterdayFactCount + " 个, 实际选取 " + yesterdayFactSelectCount + " 个.");
		log.info("在网关机(" + gateway + ")上, 未成功任务总计选取 " + tasks.size() + " 个, 以下是被选取到的任务清单:");

		return tasks;
	}

}
