package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.background.exception.GatewayNotFoundException;
import com.sw.bi.scheduler.background.exception.SchedulerException;
import com.sw.bi.scheduler.background.exception.TaskDuplicateCreateSchedulerExecption;
import com.sw.bi.scheduler.background.exception.UnknownSchedulerException;
import com.sw.bi.scheduler.background.service.SchedulerService;
import com.sw.bi.scheduler.model.*;
import com.sw.bi.scheduler.service.*;
import com.sw.bi.scheduler.supports.MessageSenderAssistant;
import com.sw.bi.scheduler.util.Configure;
import com.sw.bi.scheduler.util.Configure.*;
import com.sw.bi.scheduler.util.DateUtil;
import com.sw.bi.scheduler.util.ExecAgent.ExecResult;
import com.sw.bi.scheduler.util.OperateAction;
import com.sw.bi.scheduler.util.SshUtil;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConvertUtils;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate5.HibernateCallback;
import org.springframework.resolver.Warning;
import org.springframework.stereotype.Service;
import org.springframework.ui.ConditionModel;
import org.springframework.ui.PaginationSupport;
import org.springframework.util.StringUtils;

import javax.persistence.NonUniqueResultException;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

@Service("taskService")
@SuppressWarnings("unchecked")
public class TaskServiceImpl extends GenericServiceHibernateSupport<Task> implements TaskService {

	@Autowired
	private JobService jobService;

	@Autowired
	private JobRelationService jobRelationService;

	@Autowired
	private TaskCreateLogService taskCreateLogService;

	@Autowired
	private WaitUpdateStatusTaskService waitUpdateStatusTaskService;

	@Autowired
	private RedoAndSupplyHistoryService redoAndSupplyHistoryService;

	@Autowired
	private ActionService actionService;

	@Autowired
	private SchedulerService schedulerService;

	@Autowired
	private GatewayService gatewayService;

	@Autowired
	private BigDataTaskService bigDataTaskService;

	@Autowired
	private ScheduleSystemStatusService scheduleSystemStatusService;

	@Autowired
	private UserGroupService userGroupService;

	@Autowired
	private UserGroupRelationService userGroupRelationService;

	@Autowired
	private UserService userService;

	/**
	 * 是否为重跑或补数据的主任务
	 */
	private boolean isMasterTask = false;

	/**
	 * 当前重跑或补数据的操作批号
	 */
	private String currentOperateNo;

	/**
	 * 任务操作类型(重跑该作业、重跑该作业及其子作业、补该作业数据和补该作业及其子作业数据)
	 */
	private TaskAction taskAction;

	/**
	 * 任务重跑或补数据操作的日期
	 */
	private Date operateDate;

	/**
	 * 任务重跑或补数据的操作人
	 */
	private Long operateBy;

	/**
	 * 当前是否是批量重跑/补数据
	 */
	private boolean isBatchOperate;

	/**
	 * 批量补数据时需要将所有被补的主任务都集中加入该集合,最终一起执行添加参考点操作
	 */
	private List<Task> needSupplyMasterTasks;

	/**
	 * 参考点序号
	 */
	private Map<Long, Integer> referPointIndex;

	@Override
	public List<Task> getInitializeTasksByTaskDate(Date taskDate) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("taskDate", taskDate));
		criteria.add(Restrictions.or(Restrictions.eq("taskStatus", (long) TaskStatus.INITIALIZE.indexOf()), Restrictions.eq("taskStatus", (long) TaskStatus.RE_INITIALIZE.indexOf())));

		return criteria.list();
	}

	@Override
	public Task getRootTask(Date taskDate) throws SchedulerException {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("taskDate", taskDate));
		criteria.add(Restrictions.eq("jobId", 1l));

		Task rootTask = null;
		try {
			rootTask = (Task) criteria.uniqueResult();

		} catch (Exception e) {
			if (e instanceof NonUniqueResultException) {
				e = new TaskDuplicateCreateSchedulerExecption(e);
			} else {
				e = new UnknownSchedulerException(e);
			}

			throw (SchedulerException) e;
		}

		return rootTask;
	}

	/**
	 * 获得指定任务的父任务
	 */
	@Override
	public Collection<Task> getParentTasks(Task task) {
		Collection<Task> parentTasks = new ArrayList<Task>();

		List<Job> parentJobs = jobRelationService.getOnlineParentJobs(task.getJobId());
		if (parentJobs.size() == 0)
			return parentTasks;

		// 对小时作业依赖分钟作业进行特殊处理
		parentTasks.addAll(getParentMinuteTasksByHourTask(task, parentJobs));

		// 对小时作业依赖小时作业进行特殊处理
		parentTasks.addAll(getParentHourTasksByHourTask(task, parentJobs));

		// 对天作业依赖小时作业进行特殊处理
		parentTasks.addAll(getParentHourTasksByDayTask(task, parentJobs));

		// 对月作业依赖天作业进行特殊处理
		parentTasks.addAll(getParentDayTasksByMonthTask(task, parentJobs));

		// 对分钟作业依赖分钟作业进行特殊处理
		parentTasks.addAll(getParentMinuteTasksByMinuteTask(task, parentJobs));

		if (parentJobs.size() > 0) {
			Collection<Long> jobIds = new ArrayList<Long>();
			for (Job parentJob : parentJobs) {
				jobIds.add(parentJob.getJobId());
			}
			//......jobIds存放的是指定任务的父作业的jobid集合(分钟父任务的作业ID和小时父任务的作业ID除外)

			Criteria criteria = createCriteria();
			criteria.add(Restrictions.in("jobId", jobIds));
			criteria.add(Restrictions.eq("taskDate", task.getTaskDate()));
			criteria.addOrder(Order.asc("jobId"));
			criteria.addOrder(Order.asc("settingTime"));
			parentTasks.addAll(this.intervene(criteria.list()));
		}

		return parentTasks;
	}

	@Override
	public Collection<Task> getChildrenTasks(Task task) {
		Collection<Task> childrenTasks = new ArrayList<Task>();

		List<Job> childrenJobs = jobRelationService.getOnlineChildrenJobs(task.getJobId());
		if (childrenJobs.size() == 0)
			return childrenTasks;

		// 对小时作业依赖分钟作业进行特殊处理
		childrenTasks.addAll(getChildrenHourTasksByMinuteTask(task, childrenJobs));

		// 对小时作业依赖小时作业进行特殊处理
		childrenTasks.addAll(getChildrenHourTasksByHourTask(task, childrenJobs));

		// 对分钟作业依赖分钟作业进行特殊处理
		childrenTasks.addAll(getChildrenMinuteTasksByMinuteTask(task, childrenJobs));

		// (天依赖小时的特殊处理)父任务是小时作业,子任务非小时作业,进行特殊处理。这个时候子任务的任务日期是父任务小时任务的下一天日期
		childrenTasks.addAll(getChildrenDayTasksByHourTask(task, childrenJobs));

		// 对月作业依赖天作业进行特殊处理
		childrenTasks.addAll(getChildrenMonthTasksByDayTask(task, childrenJobs));

		if (childrenJobs.size() > 0) {
			Collection<Long> jobIds = new ArrayList<Long>();
			for (Job childJob : childrenJobs) {
				jobIds.add(childJob.getJobId());
			}

			Criteria criteria = createCriteria();
			criteria.add(Restrictions.in("jobId", jobIds));
			criteria.add(Restrictions.eq("taskDate", task.getTaskDate()));
			criteria.addOrder(Order.asc("jobId"));
			criteria.addOrder(Order.asc("settingTime"));
			childrenTasks.addAll(this.intervene(criteria.list()));

		}

		return childrenTasks;
	}

	@Override
	@Deprecated
	public Collection<Task> getChildrenTasksByUnsuccessLimit(Task task) {
		Collection<Task> childrenTasks = new ArrayList<Task>();
		StringBuffer message = new StringBuffer();
		message.append("参考点作业ID ").append(task.getJobId());

		// 配置信息中指定了没有子任务的作业ID清单
		String jobs = Configure.property(Configure.UNCHILDREN_JOBS);
		Collection<Long> unchildrenJobs = new ArrayList<Long>();
		if (StringUtils.hasText(jobs)) {
			for (String jobId : jobs.split(",")) {
				unchildrenJobs.add(Long.valueOf(jobId));
			}
		}

		List<Job> childrenJobs = jobRelationService.getOnlineChildrenJobs(task.getJobId());
		if (childrenJobs.size() == 0)
			return childrenTasks;

		message.append(": 总子作业数 ").append(childrenJobs.size());

		// 还没有区分两批jobids之前,先进行特殊处理
		// 对小时作业依赖分钟作业进行特殊处理
		childrenTasks.addAll(getChildrenHourTasksByMinuteTask(task, childrenJobs));

		// 对小时作业依赖小时作业进行特殊处理
		childrenTasks.addAll(getChildrenHourTasksByHourTask(task, childrenJobs));

		// 对分钟作业依赖分钟作业进行特殊处理
		childrenTasks.addAll(getChildrenMinuteTasksByMinuteTask(task, childrenJobs));

		message.append(", 特殊处理后子作业数 ").append(childrenJobs.size());

		Collection<Long> unchildrenJobIds = new ArrayList<Long>();
		for (Iterator<Job> iter = childrenJobs.iterator(); iter.hasNext();) {
			Job childJob = iter.next();
			if (unchildrenJobs.contains(childJob.getJobId())) {
				unchildrenJobIds.add(childJob.getJobId());
				iter.remove();
			}
		}

		message.append(", 优化处理后@1子作业数 ").append(childrenJobs.size()).append(", @2没有孙子任务的子作业数 ").append(unchildrenJobIds.size());
		// log.info("before: " + message);

		if (childrenJobs.size() > 0) {
			Collection<Long> jobIds = new ArrayList<Long>();
			for (Job childJob : childrenJobs) {
				jobIds.add(childJob.getJobId());
			}

			Criteria criteria = createCriteria();
			criteria.add(Restrictions.in("jobId", jobIds));
			criteria.add(Restrictions.eq("taskDate", task.getTaskDate()));
			criteria.addOrder(Order.asc("jobId"));
			criteria.addOrder(Order.asc("settingTime"));
			childrenTasks.addAll(criteria.list());

			message.append(", @1对应任务数 ").append(childrenTasks.size());
		}

		// log.info("middle: " + message);

		if (unchildrenJobIds.size() > 0) {
			Criteria criteria = createCriteria();
			criteria.setFirstResult(0);
			criteria.setMaxResults(30);
			criteria.add(Restrictions.in("jobId", unchildrenJobIds));
			criteria.add(Restrictions.eq("taskDate", task.getTaskDate()));
			criteria.add(Restrictions.not(Restrictions.in("taskStatus", new Long[] { (long) TaskStatus.RUN_SUCCESS.indexOf(), (long) TaskStatus.RE_RUN_SUCCESS.indexOf() })));
			//不能加这个排序条件。加了这个条件后，其实首先是以jobid排序，然后才是setting_time排序。
			//这样会选出某个 jobid的大量任务，其中许多任务的setting_time启动时间都还没到.
			//选取了这些任务，最终任务状态最多到未触发状态，无法到已触发状态，影响任务的执行
			//criteria.addOrder(Order.asc("jobId")); 
			criteria.addOrder(Order.asc("settingTime"));
			childrenTasks.addAll(criteria.list());

			message.append(", @2对应任务数 ").append(childrenTasks.size());
		}

		// System.out.println(message);

		return childrenTasks;
	}

	@Override
	public Map<Long, Collection<Task>> getDepthChildrenTasks(Task parent, Integer depth, boolean merge, boolean allowFetchParent) {
		Map<Long, Collection<Task>> children = new HashMap<Long, Collection<Task>>();

		if (depth == null) {
			depth = Integer.MAX_VALUE;
		}

		if (depth > 0) {
			this.getDepthChildrenTasks(parent, this.getChildrenTasks(parent), depth - 1, merge, children, allowFetchParent);
		}

		return children;
	}

	//Integer depth  null全部取, boolean merge 用false
	//Map<Long, Collection<Task>>
	@Override
	public Map<Long, Collection<Task>> getDepthParentTasks(Task child, Integer depth, boolean merge) {
		Map<Long, Collection<Task>> parents = new HashMap<Long, Collection<Task>>();

		if (depth == null) {
			depth = Integer.MAX_VALUE;
		}

		if (depth > 0) {
			this.getDepthParentTasks(child, this.getParentTasks(child), depth - 1, merge, parents);
		}

		return parents;
	}

	/**
	 * 获得指定作业的指定层级的子任务
	 * 
	 * @param parent
	 * @param childrenTasks
	 * @param depth
	 * @param children
	 * @param allowFetchParent
	 *            是否需要获取父任务信息
	 */
	private void getDepthChildrenTasks(Task parent, Collection<Task> childrenTasks, int depth, boolean merge, Map<Long, Collection<Task>> children, boolean allowFetchParent) {
		if (childrenTasks.size() == 0 || depth < 0) {
			return;
		}

		Long parentId = parent.getTaskId();

		if (merge) {
			childrenTasks = this.mergeTasks(childrenTasks);
		}

		for (Task childTask : childrenTasks) {
			Collection<Task> tasks = children.get(parentId);
			if (tasks == null) {
				tasks = new LinkedHashSet<Task>();
			}

			if (tasks.contains(childTask)) {
				continue;
			}

			tasks.add(childTask);
			children.put(parentId, tasks);

			if (allowFetchParent) {
				childTask.setParents(this.getParentTasks(childTask));
			}

			this.getDepthChildrenTasks(childTask, this.getChildrenTasks(childTask), depth - 1, merge, children, allowFetchParent);
		}
	}

	/**
	 * 获得指定作业的指定层级的父任务
	 * 
	 * @param child
	 * @param parentTasks
	 * @param depth
	 * @param merge
	 *            是否需要合并小时/分钟任务
	 * @param parents
	 */
	private void getDepthParentTasks(Task child, Collection<Task> parentTasks, int depth, boolean merge, Map<Long, Collection<Task>> parents) {
		if (parentTasks.size() == 0 || depth < 0) {
			return;
		}

		Long childId = child.getTaskId();

		// 是否合并小时/分钟任务
		if (merge) {
			parentTasks = this.mergeTasks(parentTasks);
		}

		for (Task parentTask : parentTasks) {
			Collection<Task> tasks = parents.get(childId);
			if (tasks == null) {
				tasks = new LinkedHashSet<Task>();
			}

			tasks.add(parentTask);
			parents.put(childId, tasks);

			this.getDepthParentTasks(parentTask, this.getParentTasks(parentTask), depth - 1, merge, parents);
		}
	}

	@Override
	@Deprecated
	public boolean hasChildrenTasks(Task task) {
		List<Job> childrenJobs = jobRelationService.getOnlineChildrenJobs(task.getJobId());
		if (childrenJobs.size() == 0)
			return false;

		// 对小时作业依赖分钟作业进行特殊处理
		if (getChildrenHourTasksByMinuteTask(task, childrenJobs).size() > 0)
			return true;

		// 对小时作业依赖小时作业进行特殊处理
		if (getChildrenHourTasksByHourTask(task, childrenJobs).size() > 0)
			return true;

		// 对分钟作业依赖分钟作业进行特殊处理
		// TODO

		if (childrenJobs.size() > 0) {
			Collection<Long> jobIds = new ArrayList<Long>();
			for (Job childJob : childrenJobs) {
				jobIds.add(childJob.getJobId());
			}

			Criteria criteria = createCriteria();
			criteria.add(Restrictions.in("jobId", jobIds));
			criteria.add(Restrictions.eq("taskDate", task.getTaskDate()));
			return count(criteria) > 0;
		}

		return false;
	}

	@Override
	/**
	 *  注意： 调用这个方法的时候，如果传入的useMaster是false，则master_taskId不能为空
	 */
	public List<Task> getTasks(long master_taskId, long child_taskId, Date startDate, Date endDate, boolean useMaster) {
		long taskId = 0;
		Task masterTask = null;
		Task childTask = null;
		Task task = null;
		if (useMaster) {
			taskId = master_taskId;
			task = get(taskId);
			masterTask = task;
		} else {
			taskId = child_taskId;
			task = get(taskId);
			childTask = task;
			masterTask = get(master_taskId);
		}

		List<Date> settingTimes = calculateSettingTime(masterTask, childTask, startDate, endDate, useMaster);

		if (settingTimes.size() == 0) {
			return new ArrayList<Task>();
		}

		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("jobId", task.getJobId()));
		criteria.add(Restrictions.in("settingTime", settingTimes));

		return criteria.list();
	}

	@Override
	public List<Task> getTasksByJob(long jobId, Date taskDate) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("jobId", jobId));
		if (taskDate != null) {
			criteria.add(Restrictions.eq("taskDate", taskDate));
		}

		return criteria.list();
	}

	@Override
	public Collection<Task> getTasksByJobs(Collection<Long> jobIds, Date taskDate) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.in("jobId", jobIds));
		criteria.add(Restrictions.eq("taskDate", taskDate));

		return criteria.list();
	}

	@Override
	public Collection<Task> getTasksByJobs(Collection<Long> jobIds, Date startTaskDate, Date endTaskDate) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.in("jobId", jobIds));
		criteria.add(Restrictions.and(Restrictions.ge("taskDate", startTaskDate), Restrictions.le("taskDate", endTaskDate)));

		return criteria.list();
	}

	@Override
	public Collection<Task> getTasksByJob(long jobId, Date startTaskDate, Date endTaskDate) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("jobId", jobId));
		criteria.add(Restrictions.and(Restrictions.ge("taskDate", startTaskDate), Restrictions.le("taskDate", endTaskDate)));

		return criteria.list();
	}

	@Override
	public List<Task> getTasksByParentJob(Date taskDate, Collection<Long> parentJobIds) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.in("jobId", parentJobIds));
		criteria.add(Restrictions.eq("taskDate", taskDate));

		return criteria.list();
	}

	@Override
	public List<Task> getTasksByParentJob(Date startTaskDate, Date endTaskDate, Collection<Long> parentJobIds) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.in("jobId", parentJobIds));
		criteria.add(Restrictions.and(Restrictions.ge("taskDate", startTaskDate), Restrictions.le("taskDate", endTaskDate)));

		return criteria.list();
	}

	@Override
	public Collection<Task> getTasksByStatus(Long[] status, Date startTaskDate, Date endTaskDate) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.in("taskStatus", status));
		criteria.add(Restrictions.and(Restrictions.ge("taskDate", startTaskDate), Restrictions.le("taskDate", endTaskDate)));

		return criteria.list();
	}

	@Override
	public Collection<Task> getTasksByTaskDate(Date taskDate) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("taskDate", taskDate));

		return criteria.list();
	}

	@Override
	public Collection<Task> getTasksByTaskDate(Date startTaskDate, Date endTaskDate) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.ge("taskDate", startTaskDate));
		criteria.add(Restrictions.le("taskDate", endTaskDate));

		return criteria.list();
	}

	@Override
	public Collection<Task> getTasksBySettingTime(Collection<Long> jobIds, Date startSettingTime, Date endSettingTime) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.in("jobId", jobIds));
		criteria.add(Restrictions.and(Restrictions.ge("settingTime", startSettingTime), Restrictions.le("settingTime", endSettingTime)));

		return criteria.list();
	}

	@Override
	public boolean isParentTaskRunSuccess(Task task) throws Warning {
		Collection<Task> parentTasks = getParentTasks(task);

		if (parentTasks.size() > 0) {
			for (Task parentTask : parentTasks) {
				if (!parentTask.isRunSuccess()) {
					throw new Warning(task.toString() + "的父" + parentTask.toString() + " 未运行成功!");
				}
			}
		} else {
			if (!task.isRoot()) {
				// 不是根节点则不允许没有父任务

				// 控制在每小时的01或02分发告警
				int minute = DateUtil.getCalendar(new Date()).get(Calendar.MINUTE);
				if (minute == 1 || minute == 2) {
					User user = userService.get(task.getDutyOfficer());

					MessageSenderAssistant messageSender = new MessageSenderAssistant();
					
					
					//messageSender.sendSms(user.getMobilePhone(), "作业(" + task.getJobId() + " - " + task.getJobName() + ")未设置父作业");
					
//	                   messageSender.send(MessagePlatform.SMS_ADTIME, user.getMobilePhone(), "作业(" + task.getJobId() + " - " + task.getJobName() + ")未设置父作业"+"【泰一指尚】");

					
				}

				throw new Warning(task.toString() + " 没有父作业.");
			}
		}

		// 校验前置任务是否都已经完成

		Collection<Long> prevTaskIds = new HashSet<Long>();
		if (StringUtils.hasText(task.getPreTasks())) {
			prevTaskIds.addAll(Arrays.asList((Long[]) ConvertUtils.convert(task.getPreTasks().split(","), Long.class)));
		}
		if (StringUtils.hasText(task.getPreTasksFromOperate())) {
			prevTaskIds.addAll(Arrays.asList((Long[]) ConvertUtils.convert(task.getPreTasksFromOperate().split(","), Long.class)));
		}
		for (Long prevTaskId : prevTaskIds) {
			Task prevTask = this.get(prevTaskId);
			if (!prevTask.isRunSuccess()) {
				throw new Warning(task.toString() + "的前置" + prevTask.toString() + "未运行成功.");
			}
		}

		return true;
	}

	/**
	 * 根据任务和某个日期,计算出一个setting_time
	 */
	@Override
	public Date calculateSettingTime(Task task, Date initializeDate) {
		Calendar calendar = DateUtil.getCalendar(initializeDate == null ? task.getTaskDate() : initializeDate);
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		int second = calendar.get(Calendar.SECOND);

		Job job = jobService.get(task.getJobId());
		int cycleType = (int) job.getCycleType();
		String time = job.getJobTime();
		if (StringUtils.hasText(time)) {
			if (time.indexOf(":") != -1) {
				int[] hm = (int[]) ConvertUtils.convert(time.split(":"), int[].class);
				hour = hm[0];
				minute = hm[1];
			} else {
				minute = Integer.parseInt(time);
			}
		} else if (JobCycle.DAY.indexOf() == cycleType) {
			// 当jobTime字段未设置且是天任务时默认00:05
			hour = second = 0;
			minute = 5;
		}
		// 小时任务时， 如果 minute == 0  则 分钟默认设置为05分钟
		if (JobCycle.MINUTE.indexOf() != cycleType && minute == 0)
			minute = 5;
		//......................................................................................

		calendar.set(Calendar.HOUR_OF_DAY, hour);
		calendar.set(Calendar.MINUTE, minute);
		calendar.set(Calendar.SECOND, second);

		return calendar.getTime();
	}

	/**
	 * 根据任务的setting_time,把一段日期范围内要补数据的任务的setting_time都计算出来 2013.8.29 逻辑变更:
	 * 支持对小时任务补数据,同时也支持把接在小时任务后面的 天依赖小时任务,天任务一块补数据 注意：
	 * 接在后面的天依赖小时任务和天任务的任务日期是下一天的
	 * 如果useMaster=true,就是计算主任务在一段日期范围内的setting_times,
	 * 如计算8.20-8.27,则startDate=8.20,endDate=8.27
	 * 如果useMaster=false,(传入的masterTask一定不能为空
	 * )就是计算子任务在一段日期范围内的setting_times,如计算8.20
	 * -8.27,则如果主任务是非小时任务,则startDate=8.20,endDate
	 * =8.27;如果主任务是小时任务,子任务也是小时任务,计算周期是8.20-8.27,子任务是非小时任务,计算周期8.21-8.28
	 */
	@Override
	public List<Date> calculateSettingTime(Task masterTask, Task childTask, Date startDate, Date endDate, boolean useMaster) {
		Calendar startCalendar = DateUtil.getCalendar(startDate, true);
		Calendar endCalendar = DateUtil.getCalendar(endDate, true);

		Task task = null;
		if (useMaster) {
			task = masterTask;
		} else {
			task = childTask;
			//主任务的周期是小时,子任务是天,月,周时,要做特殊处理: startDate,endDate都顺延一天
			if (masterTask != null && masterTask.getCycleType() == JobCycle.HOUR.indexOf() && childTask.getCycleType() != JobCycle.HOUR.indexOf()) {
				startCalendar.add(Calendar.DATE, 1);
				endCalendar.add(Calendar.DATE, 1);
			}
		}

		Calendar settingTime = DateUtil.getCalendar(task.getSettingTime());

		int cycleType = task.getCycleType();
		int date = settingTime.get(Calendar.DATE);
		int hour = settingTime.get(Calendar.HOUR_OF_DAY);
		int minute = settingTime.get(Calendar.MINUTE);
		int second = settingTime.get(Calendar.SECOND);
		int week = settingTime.get(Calendar.DAY_OF_WEEK);

		startCalendar.set(Calendar.HOUR_OF_DAY, hour);
		startCalendar.set(Calendar.MINUTE, minute);
		startCalendar.set(Calendar.SECOND, second);

		endCalendar.set(Calendar.HOUR_OF_DAY, hour);
		endCalendar.set(Calendar.MINUTE, minute);
		endCalendar.set(Calendar.SECOND, second);

		//  如果是小时作业且小时0点,则预设时间应该是下一天的0点
		// log.info("cycle type: " + task.getCycleType() + ", hour: " + hour);
		if (task.getCycleType() == JobCycle.HOUR.indexOf() && hour == 0) {
			startCalendar.add(Calendar.DATE, 1);
			endCalendar.add(Calendar.DATE, 1);
			log.info("start setting time: " + DateUtil.formatDateTime(startCalendar.getTime()) + ", end setting time: " + DateUtil.formatDateTime(endCalendar.getTime()));
		}

		long startMillisecond = startCalendar.getTimeInMillis();
		long endMillisecond = endCalendar.getTimeInMillis();

		List<Date> settingTimes = new ArrayList<Date>();
		while (startMillisecond <= endMillisecond) {
			Date newSettingTime = calculateSettingTime(task, startCalendar.getTime()); //传入的startCalendar已经是clearTime的,也就是时分秒都是0
			Calendar newSettingTimeCal = DateUtil.getCalendar(newSettingTime);
			boolean isAdd = true;

			if (JobCycle.WEEK.indexOf() == cycleType) {
				if (week != newSettingTimeCal.get(Calendar.DAY_OF_WEEK)) {
					isAdd = false;
				}
			} else if (JobCycle.MONTH.indexOf() == cycleType) {
				if (date != newSettingTimeCal.get(Calendar.DATE)) {
					isAdd = false;
				}
			} else if (JobCycle.HOUR.indexOf() == cycleType) {
				newSettingTimeCal.set(Calendar.HOUR_OF_DAY, 0);
				for (int i = 0; i <= 23; i++) {
					newSettingTimeCal.add(Calendar.HOUR_OF_DAY, 1);//每循环一次,小时点加1
					settingTimes.add(newSettingTimeCal.getTime());
				}
				isAdd = false;
			} else if (JobCycle.MINUTE.indexOf() == cycleType) {
				// 分钟任务一般没有补数据的需求,可以暂时不用考虑
			}

			if (isAdd) {
				settingTimes.add(newSettingTime);
			}

			startCalendar.add(Calendar.DATE, 1);
			startMillisecond = startCalendar.getTimeInMillis();
		}

		return settingTimes;
	}

	@Override
	/**
	 *   计算任务的readytime,结合任务的flag,flag2,joblevel
	 *   public Date calculateReadyTime(Task task)
	 *   1. 当前任务为重跑、新上线和加权时优先级较高，所以准备时间直接定义为当天的00:05(minReadyTime)
	 *   2. 结合任务的优先级,如果任务的优先级大于JobLevel.TODAY.indexOf()级别,则readyTime修正为实际的readyTime减去与优先级等同的分钟数(如果减去后,比minReadyTime还要小,则取minReadyTime)
	 * 
	 */
	public Date calculateReadyTime(Task task) {
		Calendar minReadyTime = DateUtil.getTodayCalendar();
		minReadyTime.set(Calendar.MINUTE, 5);

		Calendar readyTime = null;

		int flag = (int) task.getFlag();
		Integer flag2 = task.getFlag2();
		if (flag2 != null) {
			flag = Math.max(flag, flag2);
		}

		// 当前任务为重跑、新上线和加权时优先级较高，所以准备时间直接定义为当天的00:05
		if (flag > TaskFlag.SYSTEM.indexOf()) {
			readyTime = minReadyTime;
		} else {
			readyTime = Calendar.getInstance();
			readyTime.set(Calendar.SECOND, 0);

			// 需要结合任务的优先级进行计算
			// 优先级为3则为系统时间
			// 优先级大于3时则减相应的分钟数
			if (task.getJobLevel() > JobLevel.TODAY.indexOf()) {
				readyTime.add(Calendar.MINUTE, (int) task.getJobLevel() * -1);

				if (readyTime.getTimeInMillis() < minReadyTime.getTimeInMillis()) {
					readyTime = minReadyTime;
				}
			}
		}

		return readyTime.getTime();
	}

	@Override
	public boolean recheck(Task task) {
		if (task == null)
			return false;

		Job job = jobService.get(task.getJobId());

		// 如果作业类型是检查作业，则直接让其反查成功
		if (job.getJobType() == JobType.CHECK_DAY_DEPENDENCY_HOUR.indexOf() || job.getJobType() == JobType.CHECK_MONTH_DEPENDENCY_DAY.indexOf()) {
			return true;
		}

		try {
			return this.isParentTaskRunSuccess(task);

		} catch (Exception e) {
			// int taskStatus = (int) task.getTaskStatus();

			// 如果task是未触发,则变回INITIALIZE   如果是其他状态,都变回RE_INITIALIZE
			task.setTaskStatus(TaskStatus.WAIT_TRIGGER.indexOf() == task.getTaskStatus() ? TaskStatus.INITIALIZE.indexOf() : TaskStatus.RE_INITIALIZE.indexOf());
			task.setTaskBeginTime(null);
			task.setTaskEndTime(null);
			task.setRunTime(null);
			task.setReadyTime(null);
			task.setUpdateTime(DateUtil.now());
			update(task);

			// log.info("★★★★★★★★★★★" + task + "反查失败, 状态由 " + TaskStatus.valueOf(taskStatus).toString() + " 改回 " + TaskStatus.valueOf((int) task.getTaskStatus()).toString());

			return false;
		}
	}

	@Override
	public boolean runCompleteExcludeToday(Date today) {
		Collection<Date> notCompleteDates = taskCreateLogService.getNotRunCompleteDatesExcludeToday(today);
		boolean complete = true;

		for (Date notCompleteDate : notCompleteDates) {
			if (!this.runComplete(notCompleteDate)) {
				complete = false;
			}
		}

		return complete;
	}

	@Override
	public boolean runComplete(Date date) {
		boolean complete = taskCreateLogService.isTaskRuned(date);

		if (!complete) {
			Criteria criteria = createCriteria();
			criteria.add(Restrictions.eq("taskDate", date));
			int count = count(criteria);
			criteria.add(Restrictions.or(Restrictions.eq("taskStatus", (long) TaskStatus.RUN_SUCCESS.indexOf()), Restrictions.eq("taskStatus", (long) TaskStatus.RE_RUN_SUCCESS.indexOf())));
			int successCount = count(criteria);

			if (count > 0 && successCount > 0 && count == successCount) {
				complete = true;
			}

			if (complete) {
				taskCreateLogService.runComplete(date);
			}
		}

		return complete;
	}

	/**
	 * 重跑单个任务
	 */
	@Override
	public boolean redo(long taskId, boolean breakpoint, Long operateBy) {
		this.isMasterTask = true;
		this.taskAction = TaskAction.REDO;
		this.operateBy = operateBy;
		this.operateDate = DateUtil.now();
		createOperateNo(operateDate);

		Task masterTask = get(taskId);

		try {
			if (allowUpdateReInitializeStatus(masterTask, false)) {
				masterTask.setNeedContinueRun(breakpoint ? NeedContinueRun.YES.indexOf() : NeedContinueRun.NO.indexOf());
				// updateTriggered(task, TaskFlag.REDO.indexOf()); //变更为重做已触发
				updateReInitializeStatus4Master(masterTask, TaskFlag.REDO.indexOf()); // 主任务更改为初始化

				if (!isBatchOperate) {
					waitUpdateStatusTaskService.addParentTasks(masterTask, DateUtil.getToday()); //更新参考点表
				}

				StringBuffer loggerContent = new StringBuffer();
				loggerContent.append("[").append(masterTask.getEntityName()).append("].[");
				loggerContent.append("重跑批号: ").append(currentOperateNo).append(", ");
				loggerContent.append(masterTask.getLoggerName()).append("]");
				getOperateLoggerService().log(OperateAction.REDO, loggerContent.toString());

				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();

			if (e instanceof Warning) {
				throw (Warning) e;
			}

			return false;

		} finally {
			if (!isBatchOperate) {
				currentOperateNo = null;
			}
		}

		return false;
	}

	/**
	 * 重跑该任务及其子任务
	 */
	@Override
	public boolean redo(long masterTaskId, Long[] childTaskIds, boolean breakpoint, Long operateBy) {
		// 子任务为空时直接重跑主任务
		if (childTaskIds == null || childTaskIds.length == 0) {
			return redo(masterTaskId, breakpoint, operateBy);
		}

		this.isMasterTask = true;
		this.taskAction = TaskAction.REDO_CHILDREN;
		this.operateBy = operateBy;
		this.operateDate = DateUtil.now();
		createOperateNo(operateDate);

		Task masterTask = get(masterTaskId);

		try {
			if (allowUpdateReInitializeStatus(masterTask, false)) {
				List<Task> childTasks = query(childTaskIds);

				Iterator<Task> iter = childTasks.iterator();
				while (iter.hasNext()) {
					Task childTask = iter.next();

					// 判断子任务是否都允许更改为重做初始化,如果不允许会直接抛出异常所以不需要获得方法返回值
					// 在批量重跑时有种情况会不是通过异常方式抛出，而是直接返回false，这时可以排除该任务的重跑
					if (!allowUpdateReInitializeStatus(childTask, true)) {
						log.info("[忽略] " + childTask);
						iter.remove();
					}
				}

				// 如果主任务及所有子任务都允许则正式修改状态

				masterTask.setNeedContinueRun(breakpoint ? NeedContinueRun.YES.indexOf() : NeedContinueRun.NO.indexOf());
				// updateTriggered(masterTask, TaskFlag.REDO.indexOf()); //主节点: 变更为重做已触发
				updateReInitializeStatus4Master(masterTask, TaskFlag.REDO.indexOf()); // 主任务也改成初始化

				if (!isBatchOperate) {
					/**
					 * <pre>
					 * 	2014-05-08
					 * 	非批量重跑时主任务状态改完就需要添加参考点，但是
					 * 	当批量重跑时就不能在这个时候添加参考点了，需要将
					 * 	所有主任务及子任务都改完状态然后再对所有主任务进行
					 * 	添加参考点操作
					 * 	原因：
					 * 		父作业			子作业
					 * 		1924			1925
					 * 		427				1920
					 * 
					 * 		1925			1921,1920
					 * 		1921
					 * 		1920			1921
					 * 
					 * 	批量重跑上面三个作业1925,1921,1920，按这个顺序
					 * 	执行1925时会将1924加入参考点，这个没错
					 * 	执行1921时1920的状态还未修改所以是成功，因为会将1920加入参考点
					 * 	执行1920时会将427加入参考点，这个也没错，但这时1920已经改成初始化了
					 * 		那参考点的1920其实就不对了，但已经加入
					 * 	这个批量重跑的操作实际加入参考点的作业应该是427,1924	
					 * 
					 * 	所以批量操作需要将所有任务都改完初始化后再统一加参考点
					 * 
					 * </pre>
					 */
					waitUpdateStatusTaskService.addParentTasks(masterTask, DateUtil.getToday()); //更新参考点表
				}

				this.isMasterTask = false;

				if (childTasks != null) {
					for (Task childTask : childTasks) {
						updateReInitializeStatus(childTask, TaskFlag.REDO.indexOf()); //子节点: 变更为重做初始化
					}
				}

				StringBuffer loggerContent = new StringBuffer();
				loggerContent.append("[").append(masterTask.getEntityName()).append("].[");
				loggerContent.append("重跑批号: ").append(currentOperateNo).append(", ");
				loggerContent.append(masterTask.getLoggerName()).append("]");
				getOperateLoggerService().log(OperateAction.REDO, loggerContent.toString());

				return true;
			}

		} catch (Exception e) {
			e.printStackTrace();

			if (e instanceof Warning) {
				throw (Warning) e;
			}

		} finally {
			if (!isBatchOperate) {
				currentOperateNo = null;
			}
		}

		return false;
	}

	@Override
	public void batchRedo(Map<String, Collection<Integer>> masterAndChildrenTask, Long operateBy) {
		isBatchOperate = true;
		taskAction = TaskAction.REDO_BATCH;

		// batchOperateNo = RandomStringUtils.random(3, false, true);
		this.createOperateNo(DateUtil.now());

		try {
			for (Entry<String, Collection<Integer>> entry : masterAndChildrenTask.entrySet()) {
				long masterTaskId = Long.parseLong(entry.getKey().trim());
				Collection<Integer> childrenTaskId = entry.getValue();
				Long[] childrenTaskIds = null;

				if (childrenTaskId.size() > 0) {
					childrenTaskIds = new Long[childrenTaskId.size()];
					Iterator<Integer> iter = childrenTaskId.iterator();
					for (int i = 0; iter.hasNext(); i++) {
						childrenTaskIds[i] = (long) iter.next();
					}
				}

				this.redo(masterTaskId, childrenTaskIds, false, operateBy);
			}

			// 统一添加主任务参考点
			Date today = DateUtil.getToday();
			for (String masterTaskId : masterAndChildrenTask.keySet()) {
				Task masterTask = this.get(Long.valueOf(masterTaskId));
				waitUpdateStatusTaskService.addParentTasks(masterTask, today);
			}

		} finally {
			isBatchOperate = false;
			currentOperateNo = null;
		}
	}

	/**
	 * <pre>
	 * 对该任务补历史数据. 支持对分钟和小时类型的任务的补数据操作 2013.8.27修改了以下逻辑： 
	 * 1. calculateSettingTime方法,对于指定日期范围内,startDate---endDate,如果任务的类型是周任务或者月任务,
	 * 则只会生成相同周几或者相同月几的任务,修正了之前每天都会生成一条周任务/月任务的BUG 
	 * 2. throw new Warning("作业[作业ID: " + task.getJobId()当补周任务或者月任务时,所选择的日期范围内,
	 * 不会有周任务记录/月任务记录,则弹出"在指定日期范围内没有任务信息"这样的错误提示信息 
	 * 3. 先调用task = updateTriggered(task, TaskFlag.SUPPLY.indexOf());持久化之后，
	 * 又在this.addPredecessorTasks方法中,更新了任务的前置作业对应的前置任务信息。(主任务和子任务的前置任务信息都更新了)
	 * </pre>
	 */
	@Override
	public boolean supply(long taskId, Date startDate, Date endDate, boolean isSerialSupply, boolean isCascadeValidateParentTask, Long operateBy) {
		this.isMasterTask = true;
		this.taskAction = isSerialSupply ? TaskAction.SERIAL_SUPPLY : TaskAction.PARALLEL_SUPPLY;
		this.operateBy = operateBy;
		this.operateDate = DateUtil.now();
		createOperateNo(operateDate);

		Task masterTask = get(taskId);

		// 校验指定日期范围内主任务的所有父任务是否都已经创建
		allowParentSupply(getParentTasks(masterTask), startDate, endDate, isCascadeValidateParentTask);

		// 计算出来的,需要补数据操作的,任务的settingTime集合
		List<Date> needSupplyDates = calculateSettingTime(masterTask, null, startDate, endDate, true);

		if (needSupplyDates.size() == 0) {
			Task task = this.get(taskId);
			throw new Warning("作业[作业ID: " + task.getJobId() + ", 作业名称: " + task.getName() + ", 作业周期: " + JobCycle.toString(task.getCycleType()) + "]在指定日期范围内没有任务信息.");
		}

		List<Task> needSupplyTasks = getTasks(taskId, 0, startDate, endDate, true);//数据库中已有的记录

		try {
			if (allowSupply(needSupplyTasks, false)) {
				Collection<Date> differenceDates = removeSameSettingTime(needSupplyDates, needSupplyTasks);//前面2个集合去掉重复的,就是需要新增加的补数据记录.
				if (differenceDates.size() > 0) {
					List<Task> newSupplyTasks = createSupplyTasks(get(taskId), differenceDates);//要新补的任务. 在内存中,还没持久化到数据中
					if (allowSupply(newSupplyTasks, false))
						needSupplyTasks.addAll(newSupplyTasks);
				}

				// 如果日期范围内所有的任务都允许更改为重做已触发则正式更改状态
				// needSupplyTasks 为本次操作要变为重做已触发状态的任务的集合.
				for (int i = 0; i < needSupplyTasks.size(); i++) {
					Task task = needSupplyTasks.get(i);
					// task = updateTriggered(task, TaskFlag.SUPPLY.indexOf()); // 数据库中已有的task记录+需要新增加进去的task记录.
					task = updateReInitializeStatus4Master(task, TaskFlag.SUPPLY.indexOf());

					needSupplyTasks.set(i, task);
				}

				// 如果需要串行补数据则需要添加前置任务
				if (isSerialSupply) {
					this.addPredecessorTasks(needSupplyTasks, needSupplyDates, null);
				}

				if (!isBatchOperate) {
					waitUpdateStatusTaskService.addParentTasks(needSupplyTasks, DateUtil.getToday());
				} else {
					needSupplyMasterTasks.addAll(needSupplyTasks);
				}

				StringBuffer loggerContent = new StringBuffer();
				loggerContent.append("[").append(masterTask.getEntityName()).append("].[");
				loggerContent.append("补数据批号: ").append(currentOperateNo).append(", ");
				loggerContent.append(masterTask.getLoggerName()).append("]");
				getOperateLoggerService().log(OperateAction.SUPPLY, loggerContent.toString());

				return true;
			}

		} catch (Exception e) {
			e.printStackTrace();

			if (e instanceof Warning) {
				throw (Warning) e;
			}

		} finally {
			if (!isBatchOperate) {
				currentOperateNo = null;
			}
		}

		return false;
	}

	/**
	 * 对该任务及其子任务补历史数据
	 */
	@Override
	public boolean supply(long masterTaskId, Long[] childTaskIds, Date startDate, Date endDate, boolean isSerialSupply, boolean isCascadeValidateParentTask, Long operateBy) {
		if (childTaskIds == null || childTaskIds.length == 0) {
			return supply(masterTaskId, startDate, endDate, isSerialSupply, isCascadeValidateParentTask, operateBy);
		}

		this.isMasterTask = true;
		this.taskAction = isSerialSupply ? TaskAction.SERIAL_SUPPLY_CHILDREN : TaskAction.PARALLEL_SUPPLY_CHILDREN;
		this.operateBy = operateBy;
		this.operateDate = DateUtil.now();
		createOperateNo(operateDate);

		Task masterTask = get(masterTaskId);

		// 校验指定日期范围内主任务的所有父任务是否都已经创建
		allowParentSupply(getParentTasks(masterTask), startDate, endDate, isCascadeValidateParentTask);

		List<Date> needSupplyDates = calculateSettingTime(masterTask, null, startDate, endDate, true);

		if (needSupplyDates.size() == 0) {
			throw new Warning("作业[作业ID: " + masterTask.getJobId() + ", 作业名称: " + masterTask.getName() + ", 作业周期: " + JobCycle.toString(masterTask.getCycleType()) + "]在指定日期范围内没有作业.");
		}

		List<Task> needSupplyTasks = getTasks(masterTaskId, 0, startDate, endDate, true);

		try {
			if (allowSupply(needSupplyTasks, false)) {
				Collection<Date> differenceDates = removeSameSettingTime(needSupplyDates, needSupplyTasks);
				List<Task> newSupplyTasks = createSupplyTasks(masterTask, differenceDates);
				if (newSupplyTasks.size() > 0) {
					if (allowSupply(newSupplyTasks, false))
						needSupplyTasks.addAll(newSupplyTasks);
				}

				List<Task> childrenSupplyTasks = query(childTaskIds);
				List<Task> needSupplyChildTasks = new ArrayList<Task>();
				for (Task childSupplyTask : childrenSupplyTasks) {
					List<Date> needSupplyChildDates = calculateSettingTime(masterTask, childSupplyTask, startDate, endDate, false);
					List<Task> supplyChildTasks = getTasks(masterTaskId, childSupplyTask.getTaskId(), startDate, endDate, false);

					if (allowSupply(supplyChildTasks, true)) {
						needSupplyChildTasks.addAll(supplyChildTasks);
						differenceDates = removeSameSettingTime(needSupplyChildDates, supplyChildTasks);
						needSupplyChildTasks.addAll(createSupplyTasks(childSupplyTask, differenceDates));
					}
				}

				// 主任务及其子任务都允许更改状态则正式开始更改状态

				for (Task needSupplyTask : needSupplyTasks) {
					// updateTriggered(needSupplyTask, TaskFlag.SUPPLY.indexOf());
					updateReInitializeStatus(needSupplyTask, TaskFlag.SUPPLY.indexOf());
				}

				isMasterTask = false;

				if (needSupplyChildTasks != null) {
					for (Task needSupplyTask : needSupplyChildTasks) {
						updateReInitializeStatus(needSupplyTask, TaskFlag.SUPPLY.indexOf());
					}
				}

				// 如果需要串行补数据则需要添加前置任务
				if (isSerialSupply) {
					// 注： 叶子任务在不同日期,是在变化的。比如小时--小时--天依赖小时--天--周,一开始周任务是叶子任务,但是到了第二天,并不存在周任务了,这个时候叶子任务就变成了周任务上面的那个天任务了
					// 所以下面这行代码的调用,参数由childrenSupplyTasks变更为needSupplyChildTasks
					// this.addPredecessorTasks(needSupplyTasks, needSupplyDates, childrenSupplyTasks);
					this.addPredecessorTasks(needSupplyTasks, needSupplyDates, needSupplyChildTasks);

					// 生成子任务的作业前置任务(必须要在此重新遍历后保存，在上面的循环中有些任务还没被入库，会导致任务无法取到)
					for (Task needSupplyTask : needSupplyChildTasks) {
						Job job = jobService.get(needSupplyTask.getJobId());
						needSupplyTask.setPreTasksByCollection(this.getFrontTasks(needSupplyTask, job.getPrevJobIds()));
						super.saveOrUpdate(needSupplyTask);
					}
				}

				if (!isBatchOperate) {
					waitUpdateStatusTaskService.addParentTasks(needSupplyTasks, DateUtil.getToday());
				} else {
					needSupplyMasterTasks.addAll(needSupplyTasks);
				}

				StringBuffer loggerContent = new StringBuffer();
				loggerContent.append("[").append(masterTask.getEntityName()).append("].[");
				loggerContent.append("补数据批号: ").append(currentOperateNo).append(", ");
				loggerContent.append(masterTask.getLoggerName()).append("]");
				getOperateLoggerService().log(OperateAction.SUPPLY, loggerContent.toString());

				return true;
			}

		} catch (Exception e) {
			e.printStackTrace();

			if (e instanceof Warning) {
				throw (Warning) e;
			}

		} finally {
			if (!isBatchOperate) {
				currentOperateNo = null;
			}
		}

		return false;
	}

	@Override
	public void batchSupply(Map<String, Collection<Integer>> masterAndChildrenTask, Date startDate, Date endDate, boolean isSerialSupply, boolean isCascadeValidateParentTask, Long operateBy) {
		isBatchOperate = true;
		needSupplyMasterTasks = new ArrayList<Task>();
		taskAction = isSerialSupply ? TaskAction.SERIAL_SUPPLY_BATCH : TaskAction.PARALLEL_SUPPLY_BATCH;

		// batchOperateNo = RandomStringUtils.random(3, false, true);
		this.createOperateNo(DateUtil.now());

		try {
			for (Entry<String, Collection<Integer>> entry : masterAndChildrenTask.entrySet()) {
				long masterTaskId = Long.parseLong(entry.getKey().trim());
				Collection<Integer> childrenTaskId = entry.getValue();
				Long[] childrenTaskIds = null;

				if (childrenTaskId.size() > 0) {
					childrenTaskIds = new Long[childrenTaskId.size()];
					Iterator<Integer> iter = childrenTaskId.iterator();
					for (int i = 0; iter.hasNext(); i++) {
						childrenTaskIds[i] = (long) iter.next();
					}
				}

				this.supply(masterTaskId, childrenTaskIds, startDate, endDate, isSerialSupply, isCascadeValidateParentTask, operateBy);
			}

			// 统一添加主任务参考点
			waitUpdateStatusTaskService.addParentTasks(needSupplyMasterTasks, DateUtil.getToday());

		} finally {
			isBatchOperate = false;
			needSupplyMasterTasks.clear();
			currentOperateNo = null;
		}
	}

	@Override
	public void cancelSupply(String operateNo, Date taskDate) {
		// 根据操作批号判断之前是并行还是串行补数据
		boolean isSerialSupply = operateNo.indexOf("_SS") > -1; //串行补数据的编号以_SS标识
		Collection<RedoAndSupplyHistory> supplyHistories = new ArrayList<RedoAndSupplyHistory>();

		// 获得指定批号和任务日期的所有未取消的补数据任务
		Criteria criteria = redoAndSupplyHistoryService.createCriteria();
		criteria.add(Restrictions.eq("operateNo", operateNo));
		criteria.add(Restrictions.not(Restrictions.eq("taskStatus", TaskForegroundStatus.CANCEL_SUPPLY.indexOf())));
		if (isSerialSupply) {
			criteria.add(Restrictions.ge("taskDate", taskDate)); //串行补数据,因为前后日期之间存在依赖关系,所以取消某个日期的补数据操作以后,该日期之后的补数据操作也同时被取消
		} else {
			criteria.add(Restrictions.eq("taskDate", taskDate));
		}
		supplyHistories = criteria.list();

		if (supplyHistories.size() == 0) {
			return;
		}

		for (final RedoAndSupplyHistory supplyHistory : supplyHistories) {
			final Task supplyTask = this.get(supplyHistory.getTaskId());

			super.isAuthorizedUserGroup(new AuthenticationUserGroup() {

				@Override
				public String getLoggerName() {
					return supplyTask.getLoggerName();
				}

				@Override
				public String getEntityName() {
					return supplyTask.getEntityName();
				}

				@Override
				public Long getUserId() {
					return supplyHistory.getOperateMan();
				}

			}, OperateAction.CANCEL_SUPPLY);

			// 更改补数据历史记录的状态为“取消补数据”
			supplyHistory.setTaskStatus(TaskForegroundStatus.CANCEL_SUPPLY.indexOf());
			redoAndSupplyHistoryService.update(supplyHistory);

			// 更改任务状态为补数据前的状态
			if (supplyTask.getBeforeSupplyStatus() != null) {
				supplyTask.setTaskStatus(supplyTask.getBeforeSupplyStatus()); //取消补数据后,任务的状态改回补数据操作之前时的状态
				supplyTask.setFlag(TaskFlag.SYSTEM.indexOf());
				supplyTask.setPreTasksFromOperate(null);
				supplyTask.setOperateNo(null);
				supplyTask.setBeforeSupplyStatus(null);
				this.update(supplyTask);
			}
		}

		StringBuilder loggerContent = new StringBuilder();
		loggerContent.append("[任务].");
		loggerContent.append("[取消批号: ").append(operateNo);
		loggerContent.append(", 任务日期: ").append(DateUtil.formatDate(taskDate)).append("]");
		getOperateLoggerService().log(OperateAction.CANCEL_SUPPLY, loggerContent.toString());
	}

	@Override
	public void offline(Long[] jobIds, Date taskDate) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("taskDate", taskDate));
		criteria.add(Restrictions.in("jobId", jobIds));
		List<Task> tasks = criteria.list();

		this.offline(tasks);
	}

	@Override
	public void offline(Date taskDate) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("taskDate", taskDate));
		List<Task> tasks = criteria.list();

		this.offline(tasks);
	}

	@Override
	public boolean runBegin(Task task) {
		task.setTaskStatus(task.getTaskStatus() == TaskStatus.RE_TRIGGERED.indexOf() ? TaskStatus.RE_RUNNING.indexOf() : TaskStatus.RUNNING.indexOf());
		task.setUpdateTime(DateUtil.now());
		// 任务开始时间移到ExcuterCenter中，状态改完就认为它已经被开始了
		task.setTaskBeginTime(DateUtil.now()/*task.getUpdateTime()*/);
		task.setTaskEndTime(null);
		task.setRunTime(null);
		this.update(task);
		this.flush();

		return this.recheck(task);
	}

	@Override
	public boolean runFinished(Task task, Action action) {
		// 为了获得最新的任务状态(此状态可能会在别的操作被修改),所以在此重新从数据库中获取
		task = this.get(task.getTaskId());

		if (task == null) {
			return true;
		}

		boolean runSuccess = action.getActionStatus() == ActionStatus.RUN_SUCCESS.indexOf();
		long taskStatus;

		if (task.getTaskStatus() == TaskStatus.RE_RUNNING.indexOf()) {
			taskStatus = runSuccess ? TaskStatus.RE_RUN_SUCCESS.indexOf() : TaskStatus.RE_RUN_FAILURE.indexOf();

		} else if (TaskStatus.valueOf((int) task.getTaskStatus()) == TaskStatus.RUNNING) {
			taskStatus = runSuccess ? TaskStatus.RUN_SUCCESS.indexOf() : TaskStatus.RUN_FAILURE.indexOf();

		} else if (task.getTaskStatus() == TaskStatus.RE_TRIGGERED.indexOf()) {
			task.setTaskBeginTime(action.getStartTime());
			taskStatus = runSuccess ? TaskStatus.RE_RUN_SUCCESS.indexOf() : TaskStatus.RE_RUN_FAILURE.indexOf();

		} else if (TaskStatus.valueOf((int) task.getTaskStatus()) == TaskStatus.TRIGGERED) {
			task.setTaskBeginTime(action.getStartTime());
			taskStatus = runSuccess ? TaskStatus.RUN_SUCCESS.indexOf() : TaskStatus.RUN_FAILURE.indexOf();

		} else {
			return false;
		}

		task.setTaskStatus(taskStatus);//task的状态回填
		task.setTaskEndTime(DateUtil.now());
		task.setLastActionId(action.getActionId()); //给前台查看日志使用
		// 之所以要加一个LastActionIdForBreakpoint字段是因为： 当执行断点重跑的时候,首先会去执行AbExcuter的prepare方法,在这个方法里,会将对应的task任务的LastActionId变更为本次的actionid,这样前台才可以查看正在运行的任务的最新的日志
		// 这样,当执行HiveSqlExcuter的excuteCommand()时,就不能找到之前的那个action了(找到的是本次的action,而不是需要断点重跑的那一次action)
		task.setLastActionIdForBreakpoint(action.getActionId()); //给断点续跑使用
		task.setRunTimes(task.getRunTimes() + 1); //增加task runtimes更新
		task.setUpdateTime(DateUtil.now());

		Date beginTime = task.getTaskBeginTime();
		Date endTime = task.getTaskEndTime();
		if (beginTime != null && endTime != null) {
			task.setRunTime(endTime.getTime() - beginTime.getTime());
		}

		// 更新重跑或补数据历史的状态。。。在更新task之前，先把重跑/补数据记录更新掉。 因为task更新后，操作批号就会被置空
		boolean result = redoAndSupplyHistoryService.taskRunFinished(task, runSuccess);

		// 因为检查类型的作业的特殊性，在创建时就已经是“已触发”状态，跳过了“未触发”状态，所以需要在其执行成功后将其加入参考点表中
		if (runSuccess) {
			Job job = jobService.get(task.getJobId());
			if (job.getJobType() == JobType.CHECK_DAY_DEPENDENCY_HOUR.indexOf() || job.getJobType() == JobType.CHECK_MONTH_DEPENDENCY_DAY.indexOf()) {
				waitUpdateStatusTaskService.create(task.getScanDate(), task, StringUtils.hasText(task.getPreTasks()) || StringUtils.hasText(task.getPreTasksFromOperate()));
			}

			// 运行成功后重置由串行补数据操作设置的前置任务
			// 这个操作必须放在更新重跑或补数据历史状态之后，否则在改历史时就查不到批号了
			task.setPreTasksFromOperate(null);
			task.setOperateNo(null);
		}

		/*this.evict(task);
		this.flush();

		Connection connection = BeanFactory.connection();
		PreparedStatement pstmt = null;

		try {
			connection.setAutoCommit(false);

			StringBuilder sql = new StringBuilder();
			sql.append("update task set");
			if (runSuccess) {
				sql.append(" pre_tasks_from_operate= null,");
				sql.append(" operate_no = null,");
			}
			sql.append(" task_status = ?,");
			sql.append(" task_end_time = ?,");
			sql.append(" last_action_id = ?,");
			sql.append(" last_action_id_for_breakpoint = ?,");
			sql.append(" run_times = ?,");
			sql.append(" update_time = sysdate(),");
			sql.append(" run_time = ?");
			sql.append(" where task_id = ?");

			pstmt = connection.prepareStatement(sql.toString());
			pstmt.setObject(1, task.getTaskStatus());
			pstmt.setObject(2, task.getTaskEndTime());
			pstmt.setObject(3, task.getLastActionId());
			pstmt.setObject(4, task.getLastActionIdForBreakpoint());
			pstmt.setObject(5, task.getRunTimes());
			pstmt.setObject(6, task.getRunTime());
			pstmt.setObject(7, task.getTaskId());

			pstmt.execute();
			connection.commit();

		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			try {
				if (pstmt != null) {
					pstmt.close();
				}

				if (connection != null) {
					connection.close();
				}

			} catch (SQLException e) {
				e.printStackTrace();
			}

		}*/
		update(task);

		return result;
	}

	@Override
	public void updateLevelOrStatus(Long taskId, Long jobLevel, Long taskStatus, String preTasksFromOperate) {
		if (taskId == null)
			return;

		if (jobLevel == null && taskStatus == null) {
			return;
		}

		Task task = get(taskId);
		if (task == null)
			return;

		StringBuilder loggerContent = new StringBuilder();
		/*loggerContent.append("[").append(task.getEntityName()).append("].");
		loggerContent.append("[").append(task.getLoggerName()).append("].");*/

		if (jobLevel != null) {
			if (task.getJobLevel() != jobLevel.longValue()) {
				loggerContent.append("作业优先级: ").append(JobLevel.toString((int) task.getJobLevel())).append(" -> ").append(JobLevel.toString(jobLevel.intValue()));
			}

			task.setJobLevel(jobLevel);
		}

		if (taskStatus != null) {
			if (task.getTaskStatus() != taskStatus.longValue()) {
				if (loggerContent.length() > 0) {
					loggerContent.append(", ");
				}
				loggerContent.append("作业状态: ").append(TaskStatus.toString((int) task.getTaskStatus())).append(" -> ").append(TaskStatus.toString(taskStatus.intValue()));
			}

			task.setTaskStatus(taskStatus);

			/**
			 * <pre>
			 * 2014-09-10 当前把任务状态修改为成功或失败时需要将
			 * 所有正在运行的Action也改成相应状态
			 * </pre>
			 */
			if (task.isRunFailure() || task.isRunSuccess()) {
				List<Action> runningActions = actionService.getRunningActions(task.getTaskId());
				for (Action runningAction : runningActions) {
					runningAction.setActionStatus(task.isRunFailure() ? ActionStatus.RUN_FAILURE.indexOf() : ActionStatus.RUN_SUCCESS.indexOf());
					actionService.save(runningAction);
				}
			}
		}

		task.setPreTasksFromOperate(preTasksFromOperate);

		update(task);

		if (loggerContent.length() > 0) {
			loggerContent.insert(0, "[" + task.getEntityName() + "].[" + task.getLoggerName() + "].[");
			loggerContent.append("]");

			getOperateLoggerService().log(OperateAction.TASK_UPDATE, loggerContent.toString());
		}
	}

	@Override
	public String getProgramCode(Long taskId) {
		if (taskId == null)
			return null;

		Task task = get(taskId);
		if (task == null)
			return null;

		Job job = jobService.get(task.getJobId());
		if (job.getJobType() == JobType.HIVE_SQL.indexOf() || job.getJobType() == JobType.SHELL.indexOf()) {
			ExecResult execResult = SshUtil.execCommand(SshUtil.DEFAULT_GATEWAY, "cat " + job.getProgramPath());
			return execResult.success() ? execResult.getStdout() : execResult.getStderr();

		} else {
			return "该作业类型的程序代码不允许被查看.";
		}
	}

	@Override
	@SuppressWarnings("rawtypes")
	public PaginationSupport pagingBySql(final ConditionModel cm) {

		return (PaginationSupport) getHibernateTemplate().execute(new HibernateCallback<PaginationSupport>() {

			@Override
			public PaginationSupport doInHibernate(Session session) throws HibernateException {

				String taskId = cm.getValue("taskId", String.class);
				String jobId = cm.getValue("jobId", String.class);
				String jobName = cm.getValue("jobName", String.class);
				Date settingTimeStart = cm.getValue("settingTimeStart", Date.class);
				Date settingTimeEnd = cm.getValue("settingTimeEnd", Date.class);
				String jobBusinessGroup = cm.getValue("jobBusinessGroup", String.class);
				Long dutyOfficer = cm.getValue("dutyOfficer", Long.class);
				String taskStatus = cm.getValue("taskStatus", String.class);
				Date startDate = cm.getValue("taskDateStart", Date.class);
				Date endDate = cm.getValue("taskDateEnd", Date.class);
				boolean useScanDate = "on".equals(cm.getValue("useScanDate", String.class));
				/*Date startScanDate = cm.getValue("scanDateStart", Date.class);
				Date endScanDate = cm.getValue("scanDateEnd", Date.class);*/
				String jobCycle = cm.getValue("cycleType", String.class);
				String jobType = cm.getValue("jobType", String.class);
				String jobLevel = cm.getValue("jobLevel", String.class);
				Long startRunTime = cm.getValue("runTimeStart", Long.class);
				Long endRunTime = cm.getValue("runTimeEnd", Long.class);
				Date beginTime = cm.getValue("taskBeginTime", Date.class); // 运行开始时间
				Date endTime = cm.getValue("taskEndTime", Date.class); // 运行结束时间
				Long userGroupId = cm.getValue("userGroupId", Long.class);

				// 责任人条件未选,选择了用户组条件
				// 需要根据选取用户组下所有用户来过滤任务
				Collection<Long> userIds = null;
				if (userGroupId != null) {
					Collection<User> users = userGroupRelationService.getUsersByUserGroup(userGroupId, false);

					// 如果用户组下没有指定用户则就没必须继续查询了
					if (users.size() == 0) {
						return new PaginationSupport(cm.getStart(), cm.getLimit());
					}

					userIds = new ArrayList<Long>();
					for (User user : users) {
						userIds.add(user.getUserId());
					}
				}

				Collection<Long> jobTypes = new ArrayList<Long>();
				if (StringUtils.hasText(jobType)) {
					for (String jt : jobType.split(",")) {
						jobTypes.add(Long.parseLong(jt));
					}
				}

				List<String> clauses = new ArrayList<String>();
				Map<String, Object> params = new HashMap<String, Object>();

				if (StringUtils.hasText(taskId)) {
					clauses.add("task_id in (:taskId)");
					params.put("taskId", Arrays.asList(taskId.split(",")));
				}

				if (StringUtils.hasText(jobId)) {
					if (jobId.indexOf(",") == -1) {
						clauses.add("job_id = :jobId");
						params.put("jobId", Long.valueOf(jobId));
					} else {
						clauses.add("job_id in (:jobId)");
						params.put("jobId", Arrays.asList(jobId.split(",")));
					}
				}

				if (StringUtils.hasText(jobName)) {
					clauses.add("job_name like :jobName");
					params.put("jobName", "%" + jobName + "%");
				}

				if (StringUtils.hasText(jobBusinessGroup)) {
					clauses.add("job_business_group like :jobBusinessGroup");
					params.put("jobBusinessGroup", jobBusinessGroup + "%");
				}

				if (dutyOfficer != null && dutyOfficer > 0) {
					clauses.add("duty_officer = :dutyOfficer");
					params.put("dutyOfficer", dutyOfficer);

				} else if (userIds != null && userIds.size() > 0) {
					clauses.add("duty_officer in (:dutyOfficer)");
					params.put("dutyOfficer", userIds);
				}

				if (StringUtils.hasText(taskStatus)) {
					clauses.add("task_status in (:taskStatus)");

					Collection<Long> status = new HashSet<Long>();
					String[] tokens = taskStatus.split(",");
					for (String token : tokens) {
						if ("0".equals(token)) {
							status.addAll(Arrays.asList(Configure.TASK_FOREGROUND_NOT_RUNNING_STATUS));
						} else if ("1".equals(token)) {
							status.addAll(Arrays.asList(Configure.TASK_FOREGROUND_RUNNING_STATUS));
						} else if ("2".equals(token)) {
							status.addAll(Arrays.asList(Configure.TASK_FOREGROUND_FAILURE_STATUS));
						} else if ("3".equals(token)) {
							status.addAll(Arrays.asList(Configure.TASK_FOREGROUND_SUCCESS_STATUS));
						} else if ("4".equals(token)) {
							status.add((long) TaskStatus.TRIGGERED.indexOf());
							status.add((long) TaskStatus.RE_TRIGGERED.indexOf());
						}
					}

					params.put("taskStatus", status);
				}

				if (useScanDate) {
					clauses.add("scan_date = :scanDate");
					params.put("scanDate", DateUtil.getToday());

				}/* else {
					clauses.add("scan_date >= :scanDateStart and scan_date <= :scanDateEnd");
					params.put("scanDateStart", startScanDate);
					params.put("scanDateEnd", endScanDate);
					}*/

				if (startDate != null && endDate != null) {
					if (startDate.equals(endDate)) {
						clauses.add("task_date = :taskDate");
						params.put("taskDate", startDate);
					} else {
						clauses.add("task_date >= :taskDateStart and task_date <= :taskDateEnd");
						params.put("taskDateEnd", endDate);
						params.put("taskDateStart", startDate);
					}
				} else if (startDate != null) {
					clauses.add("task_date >= :taskDateStart");
					params.put("taskDateStart", startDate);
				} else if (endDate != null) {
					clauses.add("task_date <= :taskDateEnd");
					params.put("taskDateEnd", endDate);
				}

				if (beginTime != null) {
					clauses.add("task_begin_time >= :taskBeginTime");
					params.put("taskBeginTime", beginTime);
				}
				if (endTime != null) {
					clauses.add("task_end_time <= :taskEndTime");
					params.put("taskEndTime", endTime);
				}

				if (settingTimeStart != null && settingTimeEnd != null) {
					clauses.add("setting_time >= :settingTimeStart and setting_time <= :settingTimeEnd");
					params.put("settingTimeStart", settingTimeStart);
					params.put("settingTimeEnd", settingTimeEnd);
				} else if (settingTimeStart != null) {
					clauses.add("setting_time = :settingTimeStart");
					params.put("settingTimeStart", settingTimeStart);
				} else if (settingTimeEnd != null) {
					clauses.add("setting_time = :settingTimeEnd");
					params.put("settingTimeEnd", settingTimeEnd);
				}
				/*if (settingTime != null) {
					settingTime.setSeconds(0);
					clauses.add("setting_time = :settingTime");
					params.put("settingTime", settingTime);
				}*/

				if (StringUtils.hasText(jobCycle)) {
					clauses.add("cycle_type in (:cycleType)");
					params.put("cycleType", Arrays.asList((Integer[]) ConvertUtils.convert(jobCycle.split(","), Integer.class)));
				}

				if (jobTypes.size() > 0) {
					clauses.add("job_type in (:jobType)");
					params.put("jobType", jobTypes);
				}

				if (startRunTime != null && endRunTime != null) {
					clauses.add("run_time >= :startRunTime and run_time < :endRunTime");
					params.put("startRunTime", startRunTime);
					params.put("endRunTime", endRunTime);
				} else if (startRunTime != null) {
					clauses.add("run_time >= :startRunTime");
					params.put("startRunTime", startRunTime);
				} else if (endRunTime != null) {
					clauses.add("run_time < :endRunTime");
					params.put("endRunTime", endRunTime);
				}

				if (StringUtils.hasText(jobLevel)) {
					clauses.add("job_level in (:jobLevel)");
					params.put("jobLevel", Arrays.asList((Long[]) ConvertUtils.convert(jobLevel.split(","), Long.class)));
				}

				String sql = "from task";
				if (clauses.size() > 0) {
					sql += " where";
				}
				for (int i = 0; i < clauses.size(); i++) {
					sql += (i == 0 ? " " : " and ") + clauses.get(i);
				}

				//////////////////////////////////////////////////////////////////////////////////

				StringBuffer resultSql = new StringBuffer("select ");
				resultSql.append("task_id, ");
				resultSql.append("job_id, ");
				resultSql.append("job_name, ");
				resultSql.append("job_business_group, ");
				resultSql.append("duty_officer, ");
				resultSql.append("task_status, ");
				resultSql.append("task_begin_time, ");
				resultSql.append("task_end_time, ");
				resultSql.append("run_time, ");
				resultSql.append("task_date, ");
				resultSql.append("cycle_type, ");
				resultSql.append("setting_time, ");
				resultSql.append("last_action_id, ");
				resultSql.append("job_type, ");
				resultSql.append("ready_time, ");
				resultSql.append("flag2, ");
				resultSql.append("job_level, ");
				resultSql.append("refer_run_time ");
				resultSql.append(sql);
				resultSql.append(" order by ");

				if (cm.getOrderByCount() > 0) {
					resultSql.append(cm.toOrderBySqlString()).append(", ");

					if (cm.getOrderBy("jobName") != null) {
						resultSql.append("setting_time, ");
					}
				}

				resultSql.append("task_begin_time desc, task_id");

				/*String resultSql = "select task_id, job_id, job_name, job_business_group, duty_officer, task_status, task_begin_time, task_end_time, run_times, task_date, cycle_type, setting_time, last_action_id " +
						sql + " order by task_begin_time desc, task_id";*/
				//log.info("task query: " + resultSql);

				SQLQuery query = session.createSQLQuery(resultSql.toString());
				query.setFirstResult(cm.getStart());
				query.setMaxResults(cm.getLimit());

				for (Entry<String, Object> entry : params.entrySet()) {
					String key = entry.getKey();
					Object value = entry.getValue();

					if (value instanceof Collection) {
						query.setParameterList(key, (Collection) value);
					} else {
						query.setParameter(key, value);
					}

					/*if (key.startsWith("taskDate")) {
						log.info("param is " + key + ": " + DateUtil.formatDate((Date) value));
					} else {
						log.info("param is " + key + ": " + value);
					}*/
				}

				PaginationSupport ps = new PaginationSupport(cm.getStart(), cm.getLimit());

				Collection<Object[]> results = query.list();
				for (Object[] result : results) {
					Map<String, Object> task = new HashMap<String, Object>();
					String name = (String) result[2];
					long status = ((BigInteger) result[5]).longValue();
					Date taskBeginTime = (Date) result[6];
					Date taskEndTime = (Date) result[7];
					int cycleType = (Integer) result[10];
					Date settingTime = (Date) result[11];

					// 小时作业或分钟作业时把具体的时间加上
					if (JobCycle.HOUR.indexOf() == cycleType || JobCycle.MINUTE.indexOf() == cycleType) {
						name += "(" + DateUtil.format(settingTime, "HH:mm") + ")";
					}

					task.put("taskId", result[0]);
					task.put("jobId", result[1]);
					task.put("jobName", name);
					task.put("jobBusinessGroup", result[3]);
					task.put("dutyOfficer", result[4]);
					task.put("taskStatus", status);
					task.put("taskDate", result[9]);
					task.put("lastActionId", result[12]);
					task.put("cycleType", cycleType);
					task.put("settingTime", settingTime);
					task.put("settingTimeStart", settingTimeStart);
					task.put("settingTimeEnd", settingTimeEnd);
					task.put("jobType", result[13]);
					task.put("readyTime", result[14]);
					task.put("flag2", result[15]);
					task.put("jobLevel", result[16]);
					task.put("referRunTime", result[17]);
					task.put("userGroup", userGroupRelationService.getUserGroupByUser(((Number) result[4]).longValue()));

					if (Arrays.binarySearch(Configure.TASK_FOREGROUND_RUNNING_STATUS, status) >= 0) {
						task.put("taskBeginTime", taskBeginTime);
					}

					if (Arrays.binarySearch(Configure.TASK_FOREGROUND_SUCCESS_STATUS, status) >= 0 || Arrays.binarySearch(Configure.TASK_FOREGROUND_FAILURE_STATUS, status) > -0) {
						task.put("taskBeginTime", taskBeginTime);
						task.put("taskEndTime", taskEndTime);
						task.put("runTime", result[8]);

						/*long runTimes = 0;
						if (taskBeginTime != null && taskEndTime != null) {
							runTimes = (taskEndTime.getTime() - taskBeginTime.getTime()) / 1000 / 60;
						}
						task.put("runTimes", runTimes);*/
					}

					ps.getPaginationResults().add(task);
				}

				String countSql = "select count(*) count " + sql;
				query = session.createSQLQuery(countSql);
				query.addScalar("count", StandardBasicTypes.INTEGER);

				for (Entry<String, Object> entry : params.entrySet()) {
					String key = entry.getKey();
					Object value = entry.getValue();

					if (value instanceof Collection) {
						query.setParameterList(key, (Collection) value);
					} else {
						query.setParameter(key, value);
					}
				}

				ps.setTotal((Integer) query.uniqueResult());

				return ps;
			}

		});
	}

	@Override
	public void updateByJob(Job job) {
		List<Task> tasks = this.getTasksByJob(job.getJobId(), DateUtil.getToday());

		for (Task task : tasks) {
			// task.setJob(job);
			task.setJobId(job.getJobId());
			task.setJobName(job.getJobName());
			task.setJobBusinessGroup(job.getJobBusinessGroup());
			task.setJobDesc(job.getJobDesc());
			task.setJobLevel(job.getJobLevel());
			task.setDutyOfficer(job.getDutyOfficer());
			task.setJobType(job.getJobType());
			task.setAlert(job.getAlert());
			task.setSettingTime(this.calculateSettingTime(task, task.getSettingTime()));
			task.setGateway(job.getGateway());
			task.setFailureRerunInterval(job.getFailureRerunInterval());
			task.setFailureRerunTimes(job.getFailureRerunTimes());
			task.setUpdateTime(DateUtil.now());
			task.setUpdateBy(job.getUpdateBy());

			// 重新设置前置任务
			task.setPreTasksByCollection(this.getFrontTasks(task, job.getPrevJobIds()));

			this.update(task);
		}
	}

	@Override
	public void saveOrUpdate(Task task) {
		boolean isSystemAction = true;

		if (task.getTaskId() != null) {
			task.setUpdateTime(DateUtil.now());
		}

		if (StringUtils.hasText(currentOperateNo) && taskAction != null) {
			isSystemAction = false;
			task.setOperateNo(currentOperateNo);
			task.setUpdateBy(operateBy);
		}

		getHibernateTemplate().saveOrUpdate(task);

		if (!isSystemAction) {
			redoAndSupplyHistoryService.create(task, isMasterTask, currentOperateNo, operateBy, operateDate, taskAction);
		}
	}

	@Override
	public void update(Task task) {
		// 重载该方法，因为不需要记录日志
		getHibernateTemplate().update(task);
	}

	@Override
	public List<Task> getUnSuccessTasks(Date date) {
		Criteria criteria = createCriteria();
		// criteria.setFetchMode("job", FetchMode.JOIN);
		criteria.add(Restrictions.eq("taskDate", date));
		criteria.add(Restrictions.ne("taskStatus", (long) TaskStatus.RUN_SUCCESS.indexOf()));
		criteria.add(Restrictions.ne("taskStatus", (long) TaskStatus.RE_RUN_SUCCESS.indexOf()));

		List<Task> tasks = criteria.list();
		return tasks;
	}

	@Override
	public List<Task> getSuccessTasksOrderByRunTimeDesc(long jobId, Date taskDate) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("jobId", jobId));
		if (taskDate != null) {
			criteria.add(Restrictions.eq("taskDate", taskDate));
		}
		if (taskDate == null) {
			criteria.setMaxResults(20); //如果是统计月任务或者周任务,取最近的20次运行情况
		}
		criteria.add(Restrictions.in("taskStatus", new Long[] { (long) TaskStatus.RUN_SUCCESS.indexOf(), (long) TaskStatus.RE_RUN_SUCCESS.indexOf() }));
		criteria.add(Restrictions.isNotNull("runTime")); //加这个条件是为了排除那些被人工直接置为运行成功的任务记录
		criteria.add(Restrictions.isNotNull("taskBeginTime")); //加这个条件是为了排除那些被人工直接置为运行成功的任务记录
		criteria.add(Restrictions.isNotNull("taskEndTime")); //加这个条件是为了排除那些被人工直接置为运行成功的任务记录
		criteria.addOrder(Order.desc("runTime"));
		return criteria.list();
	}

	@Override
	public List<Task> getFailedTasks(long jobId, Date taskDate) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("jobId", jobId));
		if (taskDate != null) {
			criteria.add(Restrictions.eq("taskDate", taskDate));
		}
		criteria.add(Restrictions.in("taskStatus", new Long[] { (long) TaskStatus.RUN_FAILURE.indexOf(), (long) TaskStatus.RE_RUN_FAILURE.indexOf() }));

		return criteria.list();
	}

	@Override
	@Deprecated
	public List<Task> getWaitRunningTasks(Date scanDate) {
		Criteria criteria = createCriteria();
		if (scanDate != null) {
			criteria.add(Restrictions.eq("scanDate", scanDate));
		}
		criteria.add(Restrictions.in("taskStatus", new Long[] { (long) TaskStatus.RUN_FAILURE.indexOf(), (long) TaskStatus.RE_RUN_FAILURE.indexOf(), (long) TaskStatus.RE_RUN_FAILURE.indexOf(),
				(long) TaskStatus.RE_RUN_FAILURE.indexOf() }));
		return criteria.list();
	}

	@Override
	public boolean isRunSuccess(Long[] taskIds) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.in("taskId", taskIds));
		criteria.add(Restrictions.in("taskStatus", Configure.TASK_FOREGROUND_SUCCESS_STATUS));
		criteria.setProjection(Projections.rowCount());
		Integer count = (Integer) criteria.uniqueResult();

		return count != null && count.intValue() == taskIds.length;
	}

	@Override
	public void pauseAlert(Date taskDate, long jobId) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("taskDate", taskDate));
		criteria.add(Restrictions.eq("jobId", jobId));
		criteria.add(Restrictions.eq("alert", (long) AlertType.ALERT_TO_SELF_DEPT.indexOf()));
		Collection<Task> tasks = criteria.list();

		for (Task task : tasks) {
			task.setAlert((long) AlertType.NOT_ALERT.indexOf());
			this.update(task);
		}

		if (tasks.size() > 0) {
			Job job = jobService.get(jobId);
			getOperateLoggerService().log(OperateAction.PAUSE_ALERT, "[作业].[" + job.getLoggerName() + ", 扫描日期: " + DateUtil.formatDate(taskDate) + "]");
		}
	}

	@Override
	public void pauseAlert(long taskId) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("taskId", taskId));
		criteria.add(Restrictions.eq("alert", (long) AlertType.ALERT_TO_SELF_DEPT.indexOf()));
		Task task = (Task) criteria.uniqueResult();

		if (task != null) {
			task.setAlert((long) AlertType.NOT_ALERT.indexOf());
			this.update(task, OperateAction.PAUSE_ALERT);
		}
	}

	@Override
	public void resetAlert(Date taskDate, long jobId) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("taskDate", taskDate));
		criteria.add(Restrictions.eq("jobId", jobId));
		criteria.add(Restrictions.eq("alert", (long) AlertType.NOT_ALERT.indexOf()));
		Collection<Task> tasks = criteria.list();

		for (Task task : tasks) {
			task.setAlert((long) AlertType.ALERT_TO_SELF_DEPT.indexOf());
			this.update(task);
		}

		if (tasks.size() > 0) {
			Job job = jobService.get(jobId);
			getOperateLoggerService().log(OperateAction.RESET_ALERT, "[作业].[" + job.getLoggerName() + ", 扫描日期: " + DateUtil.formatDate(taskDate) + "]");
		}
	}

	@Override
	public void resetAlert(long taskId) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("taskId", taskId));
		criteria.add(Restrictions.eq("alert", (long) AlertType.NOT_ALERT.indexOf()));
		Task task = (Task) criteria.uniqueResult();

		if (task != null) {
			task.setAlert((long) AlertType.ALERT_TO_SELF_DEPT.indexOf());
			this.update(task, OperateAction.RESET_ALERT);
		}
	}

	/**
	 * 如果指定集合中小时/分钟任务,则将其合并为一个小时/分钟任务
	 * 
	 * @param tasks
	 * @return
	 */
	public Collection<Task> mergeTasks(Collection<Task> tasks) {
		Map<Long, Map<String, Object>> results = new HashMap<Long, Map<String, Object>>();
		for (Iterator<Task> iter = tasks.iterator(); iter.hasNext();) {
			Task task = iter.next();
			Long jobId = task.getJobId();
			int cycleType = task.getCycleType();

			getHibernateTemplate().evict(task);

			Map<String, Object> result = results.get(jobId);
			if (result == null) {
				result = new HashMap<String, Object>();
				result.put("notRunning", 0);
				result.put("running", 0);
				result.put("success", 0);
				result.put("failure", 0);
				result.put("total", 0);
				result.put("taskBeginTime", null);
				result.put("taskEndTime", null);
				result.put("masterTask", null);
			}

			if (cycleType == JobCycle.HOUR.indexOf() || cycleType == JobCycle.MINUTE.indexOf()) {
				int notRunning = (Integer) result.get("notRunning");
				int running = (Integer) result.get("running");
				int success = (Integer) result.get("success");
				int failure = (Integer) result.get("failure");
				int total = (Integer) result.get("total");
				Date taskBeginTime = (Date) result.get("taskBeginTime");
				Date taskEndTime = (Date) result.get("taskEndTime");
				Task masterTask = (Task) result.get("masterTask");

				if (task.isNotRunning()) {
					notRunning += 1;
				} else if (task.isRunning()) {
					running += 1;
				} else if (task.isRunSuccess()) {
					success += 1;
				} else if (task.isRunFailure()) {
					failure += 1;
				}

				// 计算最小开始时间和最大结束时间

				if (taskBeginTime == null) {
					taskBeginTime = task.getTaskBeginTime();
				} else {
					if (task.getTaskBeginTime() != null && taskBeginTime.getTime() > task.getTaskBeginTime().getTime()) {
						taskBeginTime = task.getTaskBeginTime();
					}
				}

				if (task.isRunSuccess() || task.isRunFailure()) {
					if (taskEndTime == null) {
						taskEndTime = task.getTaskEndTime();
					} else {
						if (task.getTaskEndTime() != null && taskEndTime.getTime() < task.getTaskEndTime().getTime()) {
							taskEndTime = task.getTaskEndTime();
						}
					}
				}

				result.put("notRunning", notRunning);
				result.put("running", running);
				result.put("success", success);
				result.put("failure", failure);
				result.put("total", total + 1);
				result.put("taskBeginTime", taskBeginTime);
				result.put("taskEndTime", taskEndTime);
				if (masterTask == null) {
					result.put("masterTask", task);
				}

			} else {
				result.put("masterTask", task);
			}

			results.put(jobId, result);
		}

		Collection<Task> mergeTasks = new ArrayList<Task>();
		for (Map<String, Object> result : results.values()) {
			int notRunning = (Integer) result.get("notRunning");
			// int running = (Integer) result.get("running");
			int success = (Integer) result.get("success");
			int failure = (Integer) result.get("failure");
			int total = (Integer) result.get("total");
			Date taskBeginTime = (Date) result.get("taskBeginTime");
			Date taskEndTime = (Date) result.get("taskEndTime");
			Task masterTask = (Task) result.get("masterTask");

			if (total > 0) {
				masterTask.setMerge(true);

				TaskForegroundStatus taskStatus = null;
				if (notRunning == total) {
					taskBeginTime = null;
					taskEndTime = null;
					taskStatus = TaskForegroundStatus.NOT_RUNNING;
				} else if (success == total) {
					taskStatus = TaskForegroundStatus.RUN_SUCCESS;
				} else if (failure > 0) {
					taskStatus = TaskForegroundStatus.RUN_FAILURE;
				} else {
					taskEndTime = null;
					taskStatus = TaskForegroundStatus.RUNNING;
				}
				masterTask.setTaskStatus((long) taskStatus.indexOf());
				masterTask.setTaskBeginTime(taskBeginTime);
				masterTask.setTaskEndTime(taskEndTime);
			}

			mergeTasks.add(masterTask);
		}

		return mergeTasks;
	}

	@Override
	public String simulateSchedule(Long[] taskIds, String gateway) throws GatewayNotFoundException {
		if (scheduleSystemStatusService.getRoundWay(gateway) == RoundWay.SIMULATE.indexOf()) {
			throw new Warning("网关机(" + gateway + ")后台正以模拟方式执行任务,前台不允许再模拟任务");
		}

		if (taskIds == null || taskIds.length == 0) {
			return null;
		}

		Collection<Task> tasks = this.query(taskIds);

		// 校验选取的任务的父任务是否都已经运行成功，只要有一个未成功则此次操作就被中止

		StringBuffer message = new StringBuffer();

		int failureCount = 0;
		int ignoreCount = 0;
		int successCount = 0;
		Collection<Long> successJobIds = new HashSet<Long>();
		Iterator<Task> iter = tasks.iterator();
		for (; iter.hasNext();) {
			Task task = iter.next();

			// "待触发"周期的作业不允许进行模拟后台操作
			if (task.getCycleType() == JobCycle.NONE.indexOf()) {
				message.append("<<span style='color:orange;'>忽略:“待触发”作业不允许模拟后台</span>>" + task).append("<br>");
				iter.remove();
				ignoreCount += 1;
				continue;
			}

			try {
				// 该方法如果校验失败会直接抛出异常
				this.isParentTaskRunSuccess(task);
			} catch (Warning e) {
				message.append("<<span style='color:red;'>失败:有未运行成功的父任务</span>>" + task).append("<br>");
				iter.remove();
				failureCount += 1;
				continue;
			}

			if (task.isRunning()) {
				// 如果任务正在运行则先删除进程
				// TODO 暂时屏蔽杀进程功能
				// actionService.killActionPID(task.getLastActionId());

				message.append("<<span style='color:red;'>失败:任务正在运行</span>>" + task).append("<br>");
				iter.remove();
				failureCount += 1;
				continue;
			}

			// 如果当前时间未到任务的预设时间则需要将任务状态改为“未触发”
			if (System.currentTimeMillis() < task.getSettingTime().getTime()) {
				schedulerService.updateWaitTrigger(task);

				message.append("<<span style='color:red;'>失败:未到任务预设时间</span>>" + task).append("<br>");
				iter.remove();
				failureCount += 1;
				continue;
			}

			message.append("<<span style='color:green;'>成功</span>>" + task).append("<br>");

			//这里要进行判断,如果当前时间已经超过task的setting_time了,那么就直接修改为已触发状态。
			//如果时间条件还不满足,那么就只能修改为未触发状态,并且要将这个task从执行的任务集合中remove掉
			//相应的提示文字也要做修正
			schedulerService.updateTriggered(task);

			// 将当前任务的父任务加入参考点表，以起到补全参考点的作用
			// 加入参考点必须放在schedulerService.updateTriggered(task);之后。 因为对于运行成功的任务，不会将其父任务加入到参考点表中。所以首先要把状态修改掉
			waitUpdateStatusTaskService.addParentTasks(task, DateUtil.getToday());

			successCount += 1;
			successJobIds.add(task.getJobId());
		}

		message.insert(0, "<b>模拟结果: 成功 " + successCount + " 条, 失败 " + failureCount + " 条" + (ignoreCount > 0 ? ", 忽略 " + ignoreCount + " 条" : "") + "</b><br>");

		// 执行后台调度程序
		Configure.property(Configure.GATEWAY, gateway);
		schedulerService.execute(tasks, true);

		StringBuilder logger = new StringBuilder();
		logger.append("[模拟结果: ");
		logger.append("成功 ").append(successJobIds).append(" 条, ");
		logger.append("失败 ").append(failureCount).append(" 条");
		if (ignoreCount > 0) {
			logger.append(", 忽略 ").append(ignoreCount).append(" 条, ");
		}
		logger.append("]");
		getOperateLoggerService().log(OperateAction.SIMULATE, logger.toString());

		return message.toString();
	}

	@Override
	public Collection<Task> validateSimulateScheduleTasks(Collection<Task> tasks) {
		int ignoreCount = 0;
		int successCount = 0;
		Date today = DateUtil.getToday();
		Iterator<Task> iter = tasks.iterator();
		for (; iter.hasNext();) {
			Task task = iter.next();

			// 更新扫描日期
			if (!today.equals(task.getScanDate())) {
				task.setScanDate(today);
				this.update(task);
			}

			try {
				// 该方法如果校验失败会直接抛出异常
				this.isParentTaskRunSuccess(task);
			} catch (Warning e) {
				// log.warn("[忽略] 有未运行成功的父任务" + task);
				iter.remove();
				ignoreCount += 1;
				continue;
			}

			if (task.isRunning()) {
				// log.warn("[忽略] 任务正在运行 " + task);
				iter.remove();
				ignoreCount += 1;
				continue;
			}

			// 如果当前时间未到任务的预设时间则需要将任务状态改为“未触发”
			if (System.currentTimeMillis() < task.getSettingTime().getTime()) {
				// log.warn("[忽略] 未到任务预设时间 " + task);
				iter.remove();
				ignoreCount += 1;
				continue;
			}

			StringBuilder message = new StringBuilder(task.toString());
			if (bigDataTaskService.isBigDataTask(task.getJobId())) {
				message.append("<大>");
			}

			String executeGateway = task.getGateway();
			if (StringUtils.hasText(executeGateway)) {
				message.append("[设定网关机:" + executeGateway + "]");
			}
			log.info(message);

			//这里要进行判断,如果当前时间已经超过task的setting_time了,那么就直接修改为已触发状态。
			//如果时间条件还不满足,那么就只能修改为未触发状态,并且要将这个task从执行的任务集合中remove掉
			//相应的提示文字也要做修正
			schedulerService.updateTriggered(task);

			successCount += 1;
		}

		log.info("[模拟结果] 成功 " + successCount + " 条" + (ignoreCount > 0 ? ", 忽略 " + ignoreCount + " 条" : ""));

		return tasks;
	}

	@Override
	public Collection<Task> getFrontTasks(Task task) {
		// 作业前置任务已经生成则从生成的任务ID清单中获得前置任务
		if (StringUtils.hasText(task.getPreTasks())) {
			Long[] frontTaskIds = (Long[]) ConvertUtils.convert(task.getPreTasks().split(","), Long.class);
			return this.query(frontTaskIds);
		}

		Job job = jobService.get(task.getJobId());
		return getFrontTasks(task, job.getPrevJobIds());
	}

	@Override
	public Collection<Task> getFrontTasksFromOperate(Task task) {
		if (StringUtils.hasText(task.getPreTasksFromOperate())) {
			Long[] frontTaskIds = (Long[]) ConvertUtils.convert(task.getPreTasksFromOperate().split(","), Long.class);
			return this.query(frontTaskIds);
		}

		return new ArrayList<Task>();
	}

	@Override
	/**
	 * 
	 *   获取某个task的前置任务集合
	 *   之所以要Task task作为参数,是因为 1. 要取任务的周期,当然这个也可以从job中取到
	 *                                  2. 关键是要取前置任务的集合,必须以当前任务task的任务日期作为基准task.getTaskDate()
	 * 
	 * 
	 */
	public Collection<Task> getFrontTasks(Task task, Long[] prevJobIds) {
		if (prevJobIds == null || prevJobIds.length == 0) {
			return null;
		}

		int cycleType = task.getCycleType(); //先取到任务的周期类型,因为不同的任务周期,其前置任务的时间范围是不同的
		Calendar calendar = DateUtil.getCalendar(task.getTaskDate());
		boolean byTaskDate = true;

		try {
			Collection<Job> frontJobs = jobService.query(prevJobIds); // 前置作业的集合
			if (jobService.validateFrontJobs(cycleType, frontJobs)) { //对作业的前置作业的设置有效性进行验证
				Date startTaskDate = null;
				Date endTaskDate = null;

				Date startSettingTime = null;
				Date endSettingTime = null;

				if (JobCycle.DAY.indexOf() == cycleType) { // 周期是天,则前置任务的startTaskDate和endTaskDate都取前一天的日期
					calendar.add(Calendar.DATE, -1);

					startTaskDate = calendar.getTime();
					endTaskDate = startTaskDate;

				} else if (JobCycle.WEEK.indexOf() == cycleType) { // 周期是周,则前置任务的startTaskDate取上周的周一，endTaskDate则取startTaskDate+6
					calendar.add(Calendar.DATE, -7);
					calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
					startTaskDate = calendar.getTime();

					calendar.add(Calendar.DATE, 6);
					endTaskDate = calendar.getTime();

				} else if (JobCycle.MONTH.indexOf() == cycleType) { // 周期是月,则前置任务的startTaskDate取上个月的第一天，endTaskDate取本月第一天减1天(也就是上月的月末)
					calendar.add(Calendar.MONTH, -1);
					calendar.set(Calendar.DATE, 1);
					startTaskDate = calendar.getTime();

					calendar.add(Calendar.MONTH, 1);
					calendar.add(Calendar.DATE, -1);
					endTaskDate = calendar.getTime();

				} else if (JobCycle.HOUR.indexOf() == cycleType) {
					// 周期是小时,则前置任务的startSettingTime取上一小时间点
					calendar = DateUtil.getCalendar(task.getSettingTime());
					calendar.add(Calendar.HOUR_OF_DAY, -1);

					startSettingTime = calendar.getTime();
					endSettingTime = startSettingTime;
					byTaskDate = false;
				}

				if (byTaskDate) {
					// 根据prevJobIds,startTaskDate,endTaskDate查询出前置任务的集合
					return this.getTasksByJobs(Arrays.asList(prevJobIds), startTaskDate, endTaskDate);
				} else {
					// 根据预设时间获得作业
					return this.getTasksBySettingTime(Arrays.asList(prevJobIds), startSettingTime, endSettingTime);
				}
			}

		} catch (Warning e) {
			log.error(e);
		}

		return null;
	}

	@Override
	public void repairWaitUpdateStatusTasksByScanDate() {
		Date today = DateUtil.getToday();

		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("scanDate", today));
		criteria.add(Restrictions.not(Restrictions.in("taskStatus", new Long[] { (long) TaskStatus.RUN_SUCCESS.indexOf(), (long) TaskStatus.RE_RUN_SUCCESS.indexOf() })));

		waitUpdateStatusTaskService.addParentTasks(criteria.list(), today);
	}

	@Override
	public void repairWaitUpdateStatusTasksByTaskDate(Date startDate, Date endDate) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.ge("taskDate", startDate));
		criteria.add(Restrictions.le("taskDate", endDate));
		criteria.add(Restrictions.not(Restrictions.in("taskStatus", new Long[] { (long) TaskStatus.RUN_SUCCESS.indexOf(), (long) TaskStatus.RE_RUN_SUCCESS.indexOf() })));

		waitUpdateStatusTaskService.addParentTasks(criteria.list(), DateUtil.getToday());
	}

	@Override
	public Collection<Task> analyseUnrunningTasks(Collection<Task> unrunningTasks) {
		referPointIndex = new HashMap<Long, Integer>();

		// 分析原因集合
		Collection<Task> causeTasks = new ArrayList<Task>();

		// 获得参考点被轮循到的大概顺序
		Collection<WaitUpdateStatusTask> referPoints = waitUpdateStatusTaskService.getActiveWaitUpdateStatusTasks();
		Iterator<WaitUpdateStatusTask> iter = referPoints.iterator();
		for (int i = 0; iter.hasNext(); i++) {
			WaitUpdateStatusTask wust = iter.next();

			referPointIndex.put(wust.getTaskId(), i + 1);
		}

		try {
			for (Task unrunningTask : unrunningTasks) {
				// 任务已经运行则忽略
				if (!unrunningTask.isNotRunning()) {
					continue;
				}

				causeTasks.addAll(this.analyseUnrunningTask(unrunningTask));
			}
		} finally {
			referPointIndex = null;
		}

		return causeTasks;

	}

	@Override
	public Collection<Task> analyseUnrunningTasksByReferPoint(Long[] referTaskIds) {
		Collection<WaitUpdateStatusTask> wusts = waitUpdateStatusTaskService.getWaitUpdateStatusTasks(Arrays.asList(referTaskIds));
		Collection<Task> childrenTasks = new ArrayList<Task>();

		for (WaitUpdateStatusTask wust : wusts) {
			childrenTasks.addAll(this.getChildrenTasks(this.get(wust.getTaskId())));
		}

		return analyseUnrunningTasks(childrenTasks);
	}

	@Override
	public Collection<Task> analyseUnrunningBigGreenplumTasks() {
		// 获得所有已触发的GP作业
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("jobType", (long) JobType.GREENPLUM_FUNCTION.indexOf()));
		criteria.add(Restrictions.in("taskStatus", new Long[] { (long) TaskStatus.TRIGGERED.indexOf(), (long) TaskStatus.RE_TRIGGERED.indexOf() }));
		criteria.addOrder(Order.asc("readyTime"));
		criteria.addOrder(Order.desc("flag"));
		criteria.addOrder(Order.desc("jobLevel"));
		Collection<Task> triggerGreenplumTasks = criteria.list();

		if (triggerGreenplumTasks.size() == 0) {
			return new ArrayList<Task>();
		}

		Map<Integer, String> tailNumberAndGatewayMapping = new HashMap<Integer, String>();
		Collection<Gateway> gateways = gatewayService.queryAll();
		for (Gateway gateway : gateways) {
			// 忽略启用白名单的网关机(实际就是忽略掉jump54)
			if (gateway.isUseWhiteList()) {
				continue;
			}

			String[] tailNumbers = gateway.getTailNumber().split(",");
			for (String tailNumber : tailNumbers) {
				tailNumberAndGatewayMapping.put(Integer.valueOf(tailNumber), gateway.getName());
			}
		}

		for (Task triggerGreenplumTask : triggerGreenplumTasks) {
			this.evict(triggerGreenplumTask);

			if (!StringUtils.hasText(triggerGreenplumTask.getGateway())) {
				// 如果任务未指定网关机则根据尾号自动匹配
				triggerGreenplumTask.setGateway(tailNumberAndGatewayMapping.get(triggerGreenplumTask.getTailNumber()));
			}
		}

		return triggerGreenplumTasks;
	}

	@Override
	public void updateSettingTime(Job job) {
		// 暂不支持对分钟/待触发作业的修改
		if (job.getCycleType() == JobCycle.MINUTE.indexOf() || job.getCycleType() == JobCycle.NONE.indexOf()) {
			return;
		}

		Long dayN = job.getDayN();
		Integer hourN = null;
		Integer minuteN = null;
		String jobTime = job.getJobTime();
		if (StringUtils.hasText(jobTime)) {
			int pos = jobTime.indexOf(":");
			if (pos == -1) {
				minuteN = Integer.valueOf(jobTime);
			} else {
				hourN = Integer.valueOf(jobTime.substring(0, pos));
				minuteN = Integer.valueOf(jobTime.substring(pos + 1));
			}
		}

		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("jobId", job.getJobId()));
		Collection<Task> tasks = criteria.list();
		for (Task task : tasks) {
			Calendar calendar = DateUtil.getCalendar(task.getSettingTime());

			/**
			 * <pre>
			 * 2014-08-20
			 * 这里的日期不能被修改，现在发现的问题是1801作业原来01分执行，现在
			 * 改成了40分执行，但由于这里同时把日期也改了，最终所有任务变成了
			 * 1号的任务，最终导致历史任务又被跑起来了
			 * </pre>
			 */
			/*if (dayN != null) {
				calendar.set(Calendar.DATE, dayN.intValue());
			}*/

			if (hourN != null) {
				calendar.set(Calendar.HOUR_OF_DAY, hourN.intValue());
			}

			if (minuteN != null) {
				calendar.set(Calendar.MINUTE, minuteN.intValue());
			}

			task.setSettingTime(calendar.getTime());
			this.update(task);
		}
	}

	@Override
	public void updateYesterdayScanDate(Date yesterday) {
		Date today = DateUtil.getToday();

		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("scanDate", yesterday));
		Collection<Task> tasks = criteria.list();

		for (Task task : tasks) {
			if (task.isRunSuccess()) {
				continue;
			}

			task.setScanDate(today);
			super.update(task);
		}
	}

	@Override
	public Task intervene(Task task) {
		task.setUserGroup(userGroupRelationService.getUserGroupByUser(task.getDutyOfficer()));

		return task;
	}

	//////////////////////////////////////////////////////////////////

	/**
	 * 获得分钟父任务
	 * 
	 * @param parentJobs
	 * @return
	 */
	private List<Task> getParentMinuteTasksByHourTask(Task task, List<Job> parentJobs) {
		List<Task> tasks = new ArrayList<Task>();
		Job job = jobService.get(task.getJobId());

		if (job.getCycleType() != JobCycle.HOUR.indexOf())
			return tasks;

		// 获得所有分钟作业的ID
		List<Long> minuteJobIds = new ArrayList<Long>();
		for (Iterator<Job> iter = parentJobs.iterator(); iter.hasNext();) {
			Job parentJob = iter.next();
			if (parentJob.getCycleType() == JobCycle.MINUTE.indexOf()) {
				minuteJobIds.add(parentJob.getJobId());
				iter.remove();
			}
		}

		if (minuteJobIds.size() == 0)
			return tasks;

		Calendar calendar = DateUtil.getCalendar(task.getSettingTime());
		calendar.add(Calendar.HOUR_OF_DAY, -1);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		Date startTime = calendar.getTime();

		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		Date endTime = calendar.getTime();

		Criteria criteria = createCriteria();
		criteria.add(Restrictions.in("jobId", minuteJobIds));
		criteria.add(Restrictions.eq("taskDate", task.getTaskDate()));
		criteria.add(Restrictions.ge("settingTime", startTime));
		criteria.add(Restrictions.le("settingTime", endTime));

		return criteria.list();
	}

	/**
	 * 获得小时父任务
	 * 
	 * @param task
	 * @param parentJobs
	 * @return
	 */
	private List<Task> getParentHourTasksByHourTask(Task task, List<Job> parentJobs) {
		List<Task> tasks = new ArrayList<Task>();
		Job job = jobService.get(task.getJobId());

		if (job.getCycleType() != JobCycle.HOUR.indexOf())
			return tasks;

		// 获得所有小时作业的ID
		List<Long> hourJobIds = new ArrayList<Long>();
		for (Iterator<Job> iter = parentJobs.iterator(); iter.hasNext();) {
			Job parentJob = iter.next();
			if (parentJob.getCycleType() == JobCycle.HOUR.indexOf()) {
				hourJobIds.add(parentJob.getJobId());
				iter.remove();
			}
		}

		if (hourJobIds.size() == 0)
			return tasks;

		Calendar calendar = DateUtil.getCalendar(task.getSettingTime());
		int hour = calendar.get(Calendar.HOUR_OF_DAY);

		Criteria criteria = createCriteria();
		criteria.add(Restrictions.in("jobId", hourJobIds));
		criteria.add(Restrictions.eq("taskDate", task.getTaskDate()));
		criteria.add(Restrictions.sqlRestriction("HOUR({alias}.setting_time) = ?", hour, StandardBasicTypes.INTEGER));

		return criteria.list();
	}

	/**
	 * 获得天依赖小时的所有小时父任务
	 * 
	 * @param task
	 * @param parentJobs
	 * @return
	 */
	private List<Task> getParentHourTasksByDayTask(Task task, List<Job> parentJobs) {
		List<Task> tasks = new ArrayList<Task>();
		Job job = jobService.get(task.getJobId());

		if (job.getJobType() != JobType.CHECK_DAY_DEPENDENCY_HOUR.indexOf()) {
			return tasks;
		}

		// 获得所有小时作业的ID
		List<Long> hourJobIds = new ArrayList<Long>();
		for (Iterator<Job> iter = parentJobs.iterator(); iter.hasNext();) {
			Job parentJob = iter.next();
			if (parentJob.getCycleType() == JobCycle.HOUR.indexOf()) {
				hourJobIds.add(parentJob.getJobId());
				iter.remove();
			}
		}

		if (hourJobIds.size() == 0)
			return tasks;

		// 天依赖的小时必须是上一天的小时
		Date taskDate = DateUtil.clearTime(DateUtil.getYesterday(task.getSettingTime())); // 根据当前task的setting_time算出上一天的日期
		return this.getTasksByParentJob(taskDate, hourJobIds);
	}

	/**
	 * 获得月依赖天的所有天父任务
	 * 
	 * @param task
	 * @param parentJobs
	 * @return
	 */
	private List<Task> getParentDayTasksByMonthTask(Task task, List<Job> parentJobs) {
		List<Task> tasks = new ArrayList<Task>();
		Job job = jobService.get(task.getJobId());

		if (job.getJobType() != JobType.CHECK_MONTH_DEPENDENCY_DAY.indexOf()) {
			return tasks;
		}

		// 获得所有天作业的ID
		List<Long> dayJobIds = new ArrayList<Long>();
		for (Iterator<Job> iter = parentJobs.iterator(); iter.hasNext();) {
			Job parentJob = iter.next();
			if (parentJob.getCycleType() == JobCycle.DAY.indexOf()) {
				dayJobIds.add(parentJob.getJobId());
				iter.remove();
			}
		}

		if (dayJobIds.size() == 0)
			return tasks;

		Calendar calendar = DateUtil.getCalendar(DateUtil.clearTime(task.getSettingTime())); // 根据task的setting_time来计算
		calendar.add(Calendar.MONTH, -1);
		calendar.set(Calendar.DATE, 1);
		Date startTaskDate = calendar.getTime(); //上个月第一天

		calendar.add(Calendar.MONDAY, 1);
		calendar.add(Calendar.DATE, -1);
		Date endTaskDate = calendar.getTime(); //上个月最后一天

		return this.getTasksByParentJob(startTaskDate, endTaskDate, dayJobIds);
	}

	/**
	 * 获得分钟依赖分钟的所有分钟父任务
	 * 
	 * @param task
	 * @param parentJobs
	 * @return
	 */
	private List<Task> getParentMinuteTasksByMinuteTask(Task task, List<Job> parentJobs) {
		List<Task> tasks = new ArrayList<Task>();
		Job job = jobService.get(task.getJobId());

		if (job.getCycleType() != JobCycle.MINUTE.indexOf())
			return tasks;

		// 获得所有分钟作业的ID
		List<Long> minuteJobIds = new ArrayList<Long>();
		for (Iterator<Job> iter = parentJobs.iterator(); iter.hasNext();) {
			Job parentJob = iter.next();
			if (parentJob.getCycleType() == JobCycle.MINUTE.indexOf()) {
				minuteJobIds.add(parentJob.getJobId());
				iter.remove();
			}
		}

		if (minuteJobIds.size() == 0)
			return tasks;

		Calendar calendar = DateUtil.getCalendar(task.getSettingTime());
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);

		Criteria criteria = createCriteria();
		criteria.add(Restrictions.in("jobId", minuteJobIds));
		criteria.add(Restrictions.eq("taskDate", task.getTaskDate()));
		criteria.add(Restrictions.sqlRestriction("HOUR({alias}.setting_time) = ?", hour, StandardBasicTypes.INTEGER));
		criteria.add(Restrictions.sqlRestriction("MINUTE({alias}.setting_time) = ?", minute, StandardBasicTypes.INTEGER));

		return criteria.list();
	}

	/**
	 * 获得分钟任务的小时子任务
	 * 
	 * @param task
	 * @param childrenJobs
	 * @return
	 */
	private List<Task> getChildrenHourTasksByMinuteTask(Task task, List<Job> childrenJobs) {
		List<Task> tasks = new ArrayList<Task>();
		Job job = jobService.get(task.getJobId());

		if (job.getCycleType() != JobCycle.MINUTE.indexOf())
			return tasks;

		List<Long> hourJobIds = new ArrayList<Long>();
		for (Iterator<Job> iter = childrenJobs.iterator(); iter.hasNext();) {
			Job childJob = iter.next();
			if (childJob.getCycleType() == JobCycle.HOUR.indexOf()) {
				hourJobIds.add(childJob.getJobId());
				iter.remove();
			}
		}

		if (hourJobIds.size() == 0)
			return tasks;

		Calendar calendar = DateUtil.getCalendar(task.getSettingTime());
		calendar.add(Calendar.HOUR_OF_DAY, 1);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		Date startTime = calendar.getTime();

		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		Date endTime = calendar.getTime();

		Criteria criteria = createCriteria();
		// criteria.add(Restrictions.in("job.jobId", hourJobIds));
		criteria.add(Restrictions.in("jobId", hourJobIds));
		criteria.add(Restrictions.eq("taskDate", task.getTaskDate()));
		criteria.add(Restrictions.ge("settingTime", startTime));
		criteria.add(Restrictions.le("settingTime", endTime));
		criteria.addOrder(Order.asc("jobId"));
		criteria.addOrder(Order.asc("settingTime"));

		return criteria.list();
	}

	/**
	 * 获得小时任务的小时子任务
	 * 
	 * @param task
	 * @param childrenJobs
	 * @return
	 */
	private List<Task> getChildrenHourTasksByHourTask(Task task, List<Job> childrenJobs) {
		List<Task> tasks = new ArrayList<Task>();
		Job job = jobService.get(task.getJobId());

		if (job.getCycleType() != JobCycle.HOUR.indexOf())
			return tasks;

		List<Long> hourJobIds = new ArrayList<Long>();
		for (Iterator<Job> iter = childrenJobs.iterator(); iter.hasNext();) {
			Job childJob = iter.next();
			if (childJob.getCycleType() == JobCycle.HOUR.indexOf()) {
				hourJobIds.add(childJob.getJobId());
				iter.remove();
			}
		}

		if (hourJobIds.size() == 0)
			return tasks;

		Calendar calendar = DateUtil.getCalendar(task.getSettingTime());
		int hour = calendar.get(Calendar.HOUR_OF_DAY);

		Criteria criteria = createCriteria();
		criteria.add(Restrictions.in("jobId", hourJobIds));
		criteria.add(Restrictions.eq("taskDate", task.getTaskDate()));
		criteria.add(Restrictions.sqlRestriction("HOUR({alias}.setting_time) = ?", hour, StandardBasicTypes.INTEGER));
		criteria.addOrder(Order.asc("jobId"));
		criteria.addOrder(Order.asc("settingTime"));

		return criteria.list();
	}

	/**
	 * 获得分钟依赖分钟的所有分钟子任务
	 * 
	 * @param task
	 * @param childrenJobs
	 * @return
	 */
	private List<Task> getChildrenMinuteTasksByMinuteTask(Task task, List<Job> childrenJobs) {
		List<Task> tasks = new ArrayList<Task>();
		Job job = jobService.get(task.getJobId());

		if (job.getCycleType() != JobCycle.MINUTE.indexOf())
			return tasks;

		List<Long> minuteJobIds = new ArrayList<Long>();
		for (Iterator<Job> iter = childrenJobs.iterator(); iter.hasNext();) {
			Job childJob = iter.next();
			if (childJob.getCycleType() == JobCycle.MINUTE.indexOf()) {
				minuteJobIds.add(childJob.getJobId());
				iter.remove();
			}
		}

		if (minuteJobIds.size() == 0)
			return tasks;

		Calendar calendar = DateUtil.getCalendar(task.getSettingTime());
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);

		Criteria criteria = createCriteria();
		criteria.add(Restrictions.in("jobId", minuteJobIds));
		criteria.add(Restrictions.eq("taskDate", task.getTaskDate()));
		criteria.add(Restrictions.sqlRestriction("HOUR({alias}.setting_time) = ?", hour, StandardBasicTypes.INTEGER));
		criteria.add(Restrictions.sqlRestriction("MINUTE({alias}.setting_time) = ?", minute, StandardBasicTypes.INTEGER));
		criteria.addOrder(Order.asc("jobId"));
		criteria.addOrder(Order.asc("settingTime"));

		return criteria.list();
	}

	/**
	 * 2013.8.30 lifeng 天依赖小时的特殊处理
	 * 父任务是小时作业,子任务非小时作业,进行特殊处理。这个时候子任务的任务日期是父任务小时任务的下一天日期
	 * 
	 * @param task
	 * @param childrenJobs
	 * @return
	 */
	private List<Task> getChildrenDayTasksByHourTask(Task task, List<Job> childrenJobs) {
		List<Task> tasks = new ArrayList<Task>();
		Job job = jobService.get(task.getJobId());
		if (job.getCycleType() != JobCycle.HOUR.indexOf())
			return tasks;

		List<Long> childrenJobIds = new ArrayList<Long>();
		for (Iterator<Job> iter = childrenJobs.iterator(); iter.hasNext();) {
			Job childJob = iter.next();
			// if (childJob.getCycleType() != JobCycle.HOUR.indexOf()) {
			if (childJob.getCycleType() == JobCycle.DAY.indexOf()) {
				childrenJobIds.add(childJob.getJobId());
				iter.remove();
			}
		}

		if (childrenJobIds.size() == 0)
			return tasks;

		List<Task> childrenTasksByHourTask = new ArrayList<Task>();
		Date taskDate = task.getTaskDate();

		//因为小时依赖小时前面已经特殊处理过了,所以不会有下面注释的这段。
		//这里的特殊处理其实就是天依赖小时的情况
		/*		Criteria criteria = createCriteria();
				criteria.add(Restrictions.in("jobId", childrenJobIds));
				criteria.add(Restrictions.eq("taskDate",taskDate));
				criteria.add(Restrictions.eq("cycleType",JobCycle.HOUR.indexOf()));
				criteria.addOrder(Order.asc("jobId"));
				criteria.addOrder(Order.asc("settingTime"));
				childrenTasksByHourTask.addAll(criteria.list());*/

		Calendar calendar = DateUtil.getCalendar(taskDate);
		calendar.add(Calendar.DATE, 1);
		Date nextDate = calendar.getTime();
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.in("jobId", childrenJobIds));
		criteria.add(Restrictions.eq("taskDate", nextDate));
		criteria.add(Restrictions.ne("cycleType", JobCycle.HOUR.indexOf()));
		criteria.addOrder(Order.asc("jobId"));
		criteria.addOrder(Order.asc("settingTime"));
		childrenTasksByHourTask.addAll(criteria.list());

		return childrenTasksByHourTask;
	}

	/**
	 * <pre>
	 * 	获得月依赖天的所有月子任务
	 * 	该接口只针对子任务作业周期是“月”，且作业类型是“月依赖天”的依赖作业
	 * </pre>
	 * 
	 * @param task
	 * @param childrenJobs
	 * @return
	 */
	private List<Task> getChildrenMonthTasksByDayTask(Task task, List<Job> childrenJobs) {
		List<Task> tasks = new ArrayList<Task>();
		Job job = jobService.get(task.getJobId());
		if (job.getCycleType() != JobCycle.DAY.indexOf())
			return tasks;

		List<Long> childrenJobIds = new ArrayList<Long>();
		for (Iterator<Job> iter = childrenJobs.iterator(); iter.hasNext();) {
			Job childJob = iter.next();
			if (childJob.getJobType() == JobType.CHECK_MONTH_DEPENDENCY_DAY.indexOf()) {
				childrenJobIds.add(childJob.getJobId());
				iter.remove();
			}
		}

		if (childrenJobIds.size() == 0)
			return tasks;

		Calendar calendar = DateUtil.getCalendar(DateUtil.clearTime(task.getSettingTime()));
		calendar.add(Calendar.MONTH, 1);
		calendar.set(Calendar.DATE, 1);
		Date startDate = calendar.getTime();

		calendar.add(Calendar.MONTH, 1);
		calendar.add(Calendar.DATE, -1);
		Date endDate = calendar.getTime();

		Criteria criteria = createCriteria();
		criteria.add(Restrictions.in("jobId", childrenJobIds));
		criteria.add(Restrictions.ge("taskDate", startDate));
		criteria.add(Restrictions.le("taskDate", endDate));
		criteria.addOrder(Order.asc("jobId"));
		criteria.addOrder(Order.asc("settingTime"));

		return criteria.list();
	}

	/**
	 * 去除相同的任务运行时间
	 * 
	 * @param dates
	 * @param tasks
	 * @return
	 */
	private Collection<Date> removeSameSettingTime(List<Date> dates, List<Task> tasks) {
		Collection<Date> results = new TreeSet<Date>();

		if (dates.size() == tasks.size())
			return results;

		Map<Long, Boolean> map = new HashMap<Long, Boolean>();
		for (Date date : dates) {
			map.put(date.getTime(), true);
		}

		for (Task task : tasks) {
			Date settingTime = task.getSettingTime();
			map.remove(settingTime.getTime());
		}

		for (Long m : map.keySet()) {
			results.add(new Date(m));
		}

		return results;
	}

	/**
	 * 
	 * 1. 作业当前未处于重跑或者补数据的状态 2. 作业本身的状态是运行成功或者运行失败 3. 父任务的状态都是运行成功
	 * 
	 * 只有这样的作业才可以被重跑. 判断是否允许将任务更改为重做已触发状态
	 * 
	 * @param task
	 * @return
	 */
	@Deprecated
	private boolean allowUpdateReTriggeredStatus(Task task) throws Warning {
		this.isRedoOrSupplyOperate(task);

		// 只允许状态为成功或失败的任务(对于新建的任务不作状态的检验)
		if (task.getTaskId() != null && !task.isRunSuccess() && !task.isRunFailure()) {
			throw new Warning(task.toString() + " 状态不是\"运行成功\"或\"运行失败\",请检查该作业的运行情况.");
		}

		try {

			// 检验父任务是否都已经运行成功,如果父任务有未成功的则会抛出Warning,不抛异常肯定为true
			return isParentTaskRunSuccess(task);

		} catch (Exception e) {
			e.printStackTrace();

			if (e instanceof Warning) {
				throw (Warning) e;
			}

			return false;
		}
	}

	/**
	 * 判断是否允许将任务更改为重做初始化状态
	 * 
	 * 补数据的要求： 0. 如果task之前已经执行了重跑或者补数据操作的话,当前task的状态必须是运行成功或者运行失败,才可以执行补数据 1.
	 * 主节点任务的状态必须是运行成功或者运行失败 2. 子节点任务的状态不能是正在运行
	 * 
	 * @param task
	 * @param isChildTask
	 * @return
	 */
	private boolean allowUpdateReInitializeStatus(Task task, boolean isChildTask) throws Warning {
		if (!this.isRedoOrSupplyOperate(task)) {
			return false;
		}

		// 只要求任务不是正在运行中即可，不需要判断所有父任务都执行成功
		if (isChildTask) {
			if (task.isRunning()) {
				throw new Warning("子" + task.toString() + " 状态不允许是\"运行中\".");
			}

		} else {
			// 只允许状态为成功或失败的任务(对于新建的任务不作状态的检验)
			if (task.getTaskId() != null && !task.isRunSuccess() && !task.isRunFailure()) {
				throw new Warning(task.toString() + " 状态不是\"运行成功\"或\"运行失败\",请检查该作业的运行情况.");
			}
		}

		return true;
	}

	/**
	 * 校验指定的父任务是否在指定补数据日期范围内都已经创建
	 * 
	 * @param parentTasks
	 * @param startDate
	 * @param endDate
	 * @param isCascadeValidateParentTask
	 * @return
	 */
	private boolean allowParentSupply(Collection<Task> parentTasks, Date startDate, Date endDate, boolean isCascadeValidateParentTask) {
		/**
		 * <pre>
		 * 	该方法的校验是一层层父任务往上校验的，如主任务上面的三个父任务
		 *  则先把这三个校验了，如果没问题再校验这三个任务的所有父任务，依
		 *  次类推
		 * </pre>
		 */
		if (parentTasks.size() == 0) {
			return true;
		}

		for (Task parentTask : parentTasks) {
			List<Date> needSupplyDates = this.calculateSettingTime(parentTask, null, startDate, endDate, true);
			List<Task> needSupplyTasks = getTasks(parentTask.getTaskId(), 0, startDate, endDate, true);
			Collection<Date> differenceDates = removeSameSettingTime(needSupplyDates, needSupplyTasks);

			// 等于0时表示当前父任务在指定日期范围内都已经创建
			if (differenceDates.size() == 0) {
				continue;
			}

			StringBuilder message = new StringBuilder("父作业[");
			message.append(parentTask.getJobId()).append(" - ");
			message.append(parentTask.getJobName());
			message.append("]在日期范围(");

			int i = 0;
			int len = differenceDates.size();
			for (Date differenceDate : differenceDates) {
				if (i > 0 && i < len) {
					message.append(",");
				}
				message.append(DateUtil.formatDate(differenceDate));

				i += 1;
			}

			message.append(")内未创建任务,补数据操作被终止");

			throw new Warning(message.toString());
		}

		// 如果不需要级联校验父任务状态则只校验第一层父任务后就可以返回了
		if (!isCascadeValidateParentTask) {
			return true;
		}

		// 获得指定父任务的所有父任务
		Collection<Task> allParentTasks = new LinkedHashSet<Task>();
		for (Task parentTask : parentTasks) {
			allParentTasks.addAll(this.getParentTasks(parentTask));
		}

		return this.allowParentSupply(allParentTasks, startDate, endDate, isCascadeValidateParentTask);
	}

	/**
	 * 指定任务是否允许补数据操作
	 * 
	 * @param needSupplyTasks
	 *            需要执行补数据操作的任务的集合
	 * @param isChildTask
	 *            该任务是否是子任务. 补数据操作时,被操作的那个节点,是主任务,是需要被修改为重做已触发的;
	 *            其子任务是需要被修改为重做初始化的.
	 * @return
	 */
	private boolean allowSupply(List<Task> needSupplyTasks, boolean isChildTask) throws Warning {
		boolean allow = true;

		try {
			Iterator<Task> iter = needSupplyTasks.iterator();
			while (iter.hasNext()) {
				Task needSupplyTask = iter.next();

				if (!allowUpdateReInitializeStatus(needSupplyTask, isChildTask)) {
					/**
					 * <pre>
					 * 	2014-05-09
					 * 	能进该判断分支的应该是在执行批量补数据且是同一批量操作编号
					 * 	中的补数据。而且如果进了这个分支那表示该任务肯定是在补数据且仍未完成
					 * </pre>
					 */
					// iter.remove();
					// log.info("-> " + needSupplyTask);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();

			if (e instanceof Warning) {
				throw (Warning) e;
			}

			return false;
		}

		return allow;
	}

	/**
	 * 指定任务是否已经执行了重跑或补数据操作，且操作仍未完成(状态未成功或未失败)
	 * 
	 * @param task
	 * @return
	 */
	private boolean isRedoOrSupplyOperate(Task task) {
		if (task.getTaskId() == null)
			return true;

		// 该Task已经执行了重跑或补数据操作在没有成功或失败之前不允许再次操作
		if (TaskFlag.REDO.indexOf() == task.getFlag() || TaskFlag.SUPPLY.indexOf() == task.getFlag()) {
			if (!task.isRunSuccess() && !task.isRunFailure()) {
				String operateNo = task.getOperateNo();
				log.info("当前批号: " + currentOperateNo + ", " + task + ",批号: " + operateNo);

				boolean isSameBatchOperate = false;

				// 只有当是批量操作时才需要校验当前任务与当前批量操作编号是否一致
				if (isBatchOperate && StringUtils.hasText(operateNo)) {
					isSameBatchOperate = currentOperateNo.equals(operateNo);
					/*int pos = operateNo.indexOf("-");
					if (pos > -1) {
						// 获得当前任务的批量操作编号
						String batchOperateNo = operateNo.substring(pos + 1, operateNo.lastIndexOf("_"));
						isSameBatchOperate = this.batchOperateNo.equals(batchOperateNo);
					}*/
				}

				if (isSameBatchOperate) {
					/**
					 * <pre>
					 * 	2014-05-08
					 *  如果是同一批量操作编号中重跑/补数据时遇到任务已是重跑/
					 *  补数据且未运行完成时是不需要抛异常出来的，因为有可能是
					 *  某个子任务同时有多个主任务，在某次操作时已经将该子任务
					 *  置成重跑/补数据和初始化，另一次操作再处理到该子任务时
					 *  就可能出现这种情况了
					 * </pre>
					 */
					return false;
				} else {
					throw new Warning(task.toString() + " 已经执行了重跑或补数据操作，且操作仍未完成!");
				}
			}
		}

		return true;
	}

	/**
	 * 将指定的任务修改为"已触发"或者"重做已触发"状态. (该方法只允许被重跑或补数据操作调用)
	 * 
	 * @param task
	 * @param taskFlag
	 */
	@Deprecated
	private Task updateTriggered(Task task, long taskFlag) {
		Long taskId = task.getTaskId();

		Long runTime = null;
		Date beginTime = null, endTime = null;

		// 重跑和补数据操作都是父任务都成功，且自身也已经准备好的，
		// 所以不需要将它置为未触发，而且可以直接置已触发让其立即就能被调度执行起来
		if (task.isRoot()) {
			task.setTaskStatus(TaskStatus.RUN_SUCCESS.indexOf());
			beginTime = endTime = DateUtil.now();
			runTime = 0l;

		} else {

			// 改成已触发状态前先保存下之前的状态,用于补数据取消
			if (taskId == null) {
				task.setTaskStatus(TaskStatus.TRIGGERED.indexOf());
				task.setBeforeSupplyStatus(task.getTaskStatus());
			} else {
				task.setBeforeSupplyStatus(task.getTaskStatus());
				task.setTaskStatus(TaskStatus.RE_TRIGGERED.indexOf());
			}

			// 重跑操作不需要保存之前的任务状态
			if (TaskFlag.REDO.indexOf() == taskFlag) {
				task.setBeforeSupplyStatus(null);
			}
		}
		task.setFlag(taskFlag);
		task.setTaskBeginTime(beginTime);
		task.setTaskEndTime(endTime);
		task.setRunTime(runTime);
		task.setUpdateTime(DateUtil.now());
		task.setScanDate(DateUtil.getToday());

		//根据任务的flag,flag2及JobLevel设置任务的ReadyTime
		task.setReadyTime(this.calculateReadyTime(task));

		//更新task表.  如果是重跑或者补数据操作,还会朝重跑补数据历史表插入一条新记录
		saveOrUpdate(task);

		return task;
	}

	/**
	 * <pre>
	 * 	2014-05-08
	 * 	经过讨论后决定重跑/补数据操作时主任务也将其改为初始化状态
	 * 	不再让主任务走捷径，让其由调度后台自动调起执行。这样的缺点
	 * 	就是这些主任务及其子任务的完成时间会比以前有所延迟，但可以
	 * 	放开前台的批量重跑或补数据，以前禁止批量执行是因为前台选中
	 *  执行的任务可能会有父子任务穿插，先改了子任务状态再改父任务
	 *  可能会导致子任务跑了也失败，现在统一将父子任务都改成初始化
	 *  后就可调度后台去控制它们各自的父子关系了
	 * </pre>
	 * 
	 * @param task
	 * @param taskFlag
	 * @return
	 */
	private Task updateReInitializeStatus4Master(Task task, long taskFlag) {
		Long taskId = task.getTaskId();

		if (task.isRoot()) {
			/**
			 * <pre>
			 * 	2014-05-08
			 *  如果需要修改的是根任务则直接将其置为成功，因为在添加参考点
			 *  的逻辑中遇到根任务是肯定会将根任务添加进参考点的，所以这里
			 *  肯定需要将根节点设置为成功了
			 * </pre>
			 */
			task.setTaskStatus(TaskStatus.RUN_SUCCESS.indexOf());

			task.setTaskBeginTime(DateUtil.now());
			task.setTaskEndTime(DateUtil.now());
			task.setRunTime(0l);
			task.setReadyTime(DateUtil.now());

		} else {
			// 改成已触发状态前先保存下之前的状态,用于补数据取消
			if (taskId == null) {
				task.setTaskStatus(TaskStatus.INITIALIZE.indexOf());
				task.setBeforeSupplyStatus(task.getTaskStatus());

			} else {
				/**
				 * <pre>
				 *  2014-05-13
				 * 	加这个判断是为了防止在批量补数据时重复修改该状态，
				 * 	如果被重复修改则状态最终会被改成初始化，这样当
				 * 	取消补数据时状态会回填成初始化，这样如果再进行
				 *  补数据操作时会提示这些批任务是未完成状态不让补
				 * </pre>
				 */
				if (task.getBeforeSupplyStatus() == null) {
					task.setBeforeSupplyStatus(task.getTaskStatus());
				}
				task.setTaskStatus(TaskStatus.RE_INITIALIZE.indexOf());
			}

			// 重跑操作不需要保存之前的任务状态
			if (TaskFlag.REDO.indexOf() == taskFlag) {
				task.setBeforeSupplyStatus(null);
			}

			task.setTaskBeginTime(null);
			task.setTaskEndTime(null);
			task.setRunTime(null);
			task.setReadyTime(null);
		}

		task.setFlag(taskFlag);
		task.setUpdateTime(DateUtil.now());
		task.setScanDate(DateUtil.getToday());

		//更新task表.  如果是重跑或者补数据操作,还会朝重跑补数据历史表插入一条新记录
		saveOrUpdate(task);

		return task;
	}

	/**
	 * 将指定任务更改为重做初始化状态
	 * 
	 * @param task
	 * @param taskFlag
	 */
	private void updateReInitializeStatus(Task task, long taskFlag) {
		/**
		 * <pre>
		 * 2014-05-08
		 * 如果指定任务已经是初始化状态则不需要再修改状态了
		 * 这种情况一般会出现在批量重跑/补数据操作中，如某
		 * 个子任务分别有二个主任务，在第一个主任务执行时
		 * 已经将子任务设置成初始化状态，当第二个主任务执行
		 * 时就不需要再其状态了
		 * </pre>
		 */
		if (task.getTaskId() != null && task.isInitialize()) {
			return;
		}

		if (task.getTaskId() == null) {
			task.setTaskStatus(TaskStatus.INITIALIZE.indexOf());
			task.setBeforeSupplyStatus(task.getTaskStatus());
		} else {
			task.setBeforeSupplyStatus(task.getTaskStatus());
			task.setTaskStatus(TaskStatus.RE_INITIALIZE.indexOf());
		}
		task.setFlag(taskFlag);
		task.setTaskBeginTime(null);
		task.setTaskEndTime(null);
		task.setRunTime(null);
		task.setReadyTime(null);
		task.setUpdateTime(DateUtil.now());
		task.setScanDate(DateUtil.getToday());
		saveOrUpdate(task);
	}

	/**
	 * 创建指定任务在指定的各时间点的任务
	 * 
	 * @param task
	 * @param settingTimes
	 * @return
	 */
	private List<Task> createSupplyTasks(Task task, Collection<Date> settingTimes) {
		Task newTask = null;

		List<Task> tasks = new ArrayList<Task>();
		for (Date settingTime : settingTimes) {
			try {
				newTask = (Task) BeanUtils.cloneBean(task);
				Job job = jobService.get(newTask.getJobId());

				newTask.setTaskId(null);
				newTask.setFlag(TaskFlag.SUPPLY.indexOf());
				newTask.setCreateTime(DateUtil.now());
				newTask.setDutyOfficer(task.getDutyOfficer());
				newTask.setSettingTime(settingTime);
				newTask.setScanDate(DateUtil.getToday());
				newTask.setPreTasksFromOperateByCollection(this.getFrontTasks(newTask, job.getPrevJobIds()));

				Calendar calendar = DateUtil.getCalendar(settingTime);
				// 如果是小时候作业且小时又是0点，则需要减一天
				if (task.getCycleType() == JobCycle.HOUR.indexOf() && calendar.get(Calendar.HOUR_OF_DAY) == 0) {
					calendar.add(Calendar.DATE, -1);
					newTask.setTaskDate(DateUtil.clearTime(calendar.getTime()));
				} else {
					newTask.setTaskDate(DateUtil.clearTime(settingTime));
				}

				if (task.isRoot()) {
					newTask.setTaskBeginTime(DateUtil.now());
					newTask.setTaskEndTime(newTask.getTaskBeginTime());
					newTask.setRunTime(0l);
					newTask.setTaskStatus((long) TaskStatus.RUN_SUCCESS.indexOf());
				} else {
					newTask.setTaskBeginTime(null);
					newTask.setTaskEndTime(null);
					newTask.setRunTime(null);
					newTask.setTaskStatus((long) TaskStatus.INITIALIZE.indexOf());
				}

				tasks.add(newTask);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return tasks;
	}

	/**
	 * 串行补数据时,给每一个日期的主任务添加前置任务
	 * 
	 * @param needSupplyTasks
	 *            补数据操作的主任务
	 * @param needSupplyDates
	 *            补数据的日期范围 当支持小时任务以后，Collection<Date>
	 *            needSupplyDates含有同一天的24个小时setting_time
	 * @param childrenSupplyTasks
	 *            补数据操作的子任务
	 * 
	 */
	private void addPredecessorTasks(Collection<Task> needSupplyTasks, Collection<Date> needSupplyDates, Collection<Task> childrenSupplyTasks) {
		Task masterTask = needSupplyTasks.iterator().next();
		Map<String, Collection<Task>> leafChildrenTasks = this.getLeafChildrenTasks(masterTask, childrenSupplyTasks); //现在改成了传入要补的所有子任务,然后把这些子任务中的叶子任务求出来
		Map<Date, Task> supplyTaskSettingTimeMapping = new HashMap<Date, Task>();
		Map<String, Collection<Task>> supplyTaskMapping = new HashMap<String, Collection<Task>>();
		for (Task needSupplyTask : needSupplyTasks) {
			String taskDate = DateUtil.formatDate(needSupplyTask.getTaskDate());

			// 生成作业前置
			Job job = jobService.get(needSupplyTask.getJobId());
			needSupplyTask.setPreTasksByCollection(this.getFrontTasks(needSupplyTask, job.getPrevJobIds()));
			super.saveOrUpdate(needSupplyTask);

			supplyTaskSettingTimeMapping.put(needSupplyTask.getSettingTime(), needSupplyTask);
			// 如果补的主作业是小时作业的话,那么到时候get出来的就只是一个小时点的任务,所以这里需要修改,key用taskDate,  Map<String,Collection<Task>>
			Collection<Task> supplyTasks = null;
			if (supplyTaskMapping.get(taskDate) == null) {
				supplyTasks = new LinkedHashSet<Task>();
				supplyTasks.add(needSupplyTask);
			} else {
				supplyTasks = supplyTaskMapping.get(taskDate);
				supplyTasks.add(needSupplyTask);
			}
			supplyTaskMapping.put(taskDate, supplyTasks);
		}

		Iterator<Date> iter = needSupplyDates.iterator();
		Date prevNeedSupplyDate = iter.next(); // 主节点是小时作业时,needSupplyDates:  2013-08-28 01:05 2013-08-28 02:05 ... 2013-08-29 00:05 || 2013-08-29 01:05
		while (iter.hasNext()) {
			//edit2: Task prevSupplyTask = supplyTaskMapping.get(prevNeedSupplyDate);
			Task prevSupplyTask = supplyTaskSettingTimeMapping.get(prevNeedSupplyDate);
			String prevSupplyTaskDate = DateUtil.formatDate(prevSupplyTask.getTaskDate());
			StringBuffer prevTasks = new StringBuffer();
			if (leafChildrenTasks.size() > 0) {
				Collection<Task> childrenTasks = leafChildrenTasks.get(prevSupplyTaskDate);
				if (childrenTasks != null) {
					for (Task childrenTask : childrenTasks) {
						if (prevTasks.length() > 0) {
							prevTasks.append(",");
						}
						prevTasks.append(childrenTask.getTaskId());
					}
				}
			} else {
				// 如果没有叶子任务则将主任务作为前置任务
				// bug: 这里有一个bug,当串行补单个小时作业时,本来应该是前一天的所有小时任务的ID串,作为下一天的所有小时任务的前置任务,目前的现象是前一天最后一个小时点任务ID,作为下一天所有小时任务的前置任务
				//edit3 prevTasks.append(prevSupplyTask.getTaskId());  
				Collection<Task> supplyTasks = supplyTaskMapping.get(prevSupplyTaskDate);
				if (supplyTasks != null) {
					for (Task supplyTask : supplyTasks) {
						if (prevTasks.length() > 0) {
							prevTasks.append(",");
						}
						prevTasks.append(supplyTask.getTaskId());
					}
				}
			}

			//当补数据支持小时任务以后,传入的要补的setting_time中,有同一天的24个小时点的setting_time，所以光用Date needSupplyDate = iter.next();无法切换到下一天,只是切换到了同一天的下一个小时点
			//所以要修改为下面这段代码的逻辑
			while (iter.hasNext()) {
				Date needSupplyDate = iter.next();
				Task supplyTask = supplyTaskSettingTimeMapping.get(needSupplyDate);
				String supplyTaskDate = DateUtil.formatDate(supplyTask.getTaskDate());

				if (!supplyTaskDate.equals(prevSupplyTaskDate)) {
					supplyTask.setPreTasksFromOperate(prevTasks.toString()); //设置前置任务
					this.update(supplyTask);
					prevNeedSupplyDate = needSupplyDate; //prevNeedSupplyDate这个日期只有break后,才起到作用

					if (supplyTask.getCycleType() != JobCycle.HOUR.indexOf()) {
						break;
					} else {
						//处理小时任务时,0点的那个任务，如2013-08-29 00:05:00  其任务日期是2013-08-28. 只有当处理完毕最后一个小时点之后,才可以break
						if (!DateUtil.formatDate(needSupplyDate).equals(supplyTaskDate)) {
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * 获得指定的任务集合中的所有叶子任务(在指定的任务集合中是叶子任务,在所有任务,一个大的任务树中,未必是叶子任务)
	 * 求出每一个任务的子任务,如果所有的子任务都不在任务集合中,则说明该任务就是叶子任务
	 * 
	 * @param childrenSupplyTasks
	 * @return
	 */
	private Map<String, Collection<Task>> getLeafChildrenTasks(Task masterTask, Collection<Task> childrenSupplyTasks) {
		Map<String, Collection<Task>> leaf_childrentask_map = new HashMap<String, Collection<Task>>();

		if (childrenSupplyTasks == null || childrenSupplyTasks.size() == 0) {
			return leaf_childrentask_map;
		}

		Map<Long, Task> childrenSupplyTaskMapping = new HashMap<Long, Task>();
		for (Task childrenSupplyTask : childrenSupplyTasks) {
			childrenSupplyTaskMapping.put(childrenSupplyTask.getTaskId(), childrenSupplyTask);
		}

		for (Task childrenSupplyTask : childrenSupplyTasks) {
			// 获得指定子任务的所有子任务(即孙子任务)
			Collection<Task> grandsonTasks = this.getChildrenTasks(childrenSupplyTask); //modify getChildrenTasksForSupply

			// 如果这些孙子任务都不在需要补数据的子任务清单中则认为当前子任务是叶子任务
			boolean isLeafTask = true;
			for (Task grandsonTask : grandsonTasks) {
				if (childrenSupplyTaskMapping.get(grandsonTask.getTaskId()) != null) {
					isLeafTask = false;
					break;
				}
			}

			if (isLeafTask) {
				Collection<Task> leafChildrenTasks = null;
				// 如果父任务是小时,子任务不是小时,则子任务的taskDate是下一天的. 所以在这种情况下,要处理成日期-1天
				String taskDate = DateUtil.formatDate(childrenSupplyTask.getTaskDate());
				if (masterTask.getCycleType() == JobCycle.HOUR.indexOf() && childrenSupplyTask.getCycleType() != JobCycle.HOUR.indexOf()) {
					Calendar c = DateUtil.getCalendar(childrenSupplyTask.getTaskDate());
					c.add(Calendar.DATE, -1);
					taskDate = DateUtil.formatDate(c.getTime());
				}

				if (leaf_childrentask_map.get(taskDate) == null) {
					leafChildrenTasks = new LinkedHashSet<Task>();
					leafChildrenTasks.add(childrenSupplyTask);
				} else {
					leafChildrenTasks = leaf_childrentask_map.get(taskDate);
					leafChildrenTasks.add(childrenSupplyTask);
				}

				leaf_childrentask_map.put(taskDate, leafChildrenTasks);
			}
		}

		return leaf_childrentask_map;
	}

	/**
	 * 创建重跑/补数据的批号
	 * 
	 * @param operateDate
	 * @return
	 */
	private String createOperateNo(Date operateDate) {
		if (!StringUtils.hasText(currentOperateNo)) {
			currentOperateNo = redoAndSupplyHistoryService.createOperateNo(operateDate, taskAction/*, batchOperateNo*/);
		}

		return currentOperateNo;
	}

	/**
	 * 对指定任务下线
	 * 
	 * @param tasks
	 */
	private void offline(Collection<Task> tasks) {
		if (tasks == null || tasks.size() == 0) {
			return;
		}

		for (Task task : tasks) {
			// 如果任务正在运行则需要删除执行进程
			// TODO 目前只按正常逻辑判断，如果有异常则另行处理(如：任务非运行状态，但进程却还在)
			// TODO 暂时屏蔽杀进程功能
			/*if (task.isRunning()) {
				actionService.killActionPID(task.getLastActionId());
			}*/

			getHibernateTemplate().delete(task);
		}
	}

	/**
	 * 分析指定任务未运行的原因
	 * 
	 * @param unrunningTask
	 * @return
	 */
	private Collection<Task> analyseUnrunningTask(Task unrunningTask) {
		Collection<Task> causeTasks = new LinkedHashSet<Task>();

		// 如果任务不是未运行则忽略分析
		if (!unrunningTask.isNotRunning()) {
			return causeTasks;
		}

		// 分组名称
		String groupName = "[" + unrunningTask.getTaskId() + "] " + unrunningTask.getJobId() + " - " + unrunningTask.getName();
		unrunningTask.setAnalyseType("分析任务");
		unrunningTask.setAnalyseGroupName(groupName);
		unrunningTask.setAnalyseReferPointIndex(referPointIndex.get(unrunningTask.getTaskId()));
		causeTasks.add(unrunningTask);

		// 分析父任务运行情况
		Collection<Task> parentTasks = this.getParentTasks(unrunningTask);
		for (Task parentTask : parentTasks) {
			parentTask.setAnalyseType("父任务");
			parentTask.setAnalyseGroupName(groupName);
			parentTask.setAnalyseReferPointIndex(referPointIndex.get(parentTask.getTaskId()));

			this.evict(parentTask);
			causeTasks.add(parentTask);
		}

		// 分析前置任务运行情况
		Collection<Task> frontTasks = this.getFrontTasks(unrunningTask);
		if (frontTasks != null && frontTasks.size() > 0) {
			for (Task frontTask : frontTasks) {
				frontTask.setAnalyseType("前置任务");
				frontTask.setAnalyseGroupName(groupName);
				frontTask.setAnalyseReferPointIndex(referPointIndex.get(frontTask.getTaskId()));

				this.evict(frontTask);
				causeTasks.add(frontTask);
			}
		}

		// 分析操作前置任务运行情况
		frontTasks = this.getFrontTasksFromOperate(unrunningTask);
		if (frontTasks != null && frontTasks.size() > 0) {
			for (Task frontTask : frontTasks) {
				frontTask.setAnalyseType("前置操作任务");
				frontTask.setAnalyseGroupName(groupName);
				frontTask.setAnalyseReferPointIndex(referPointIndex.get(frontTask.getTaskId()));

				this.evict(frontTask);
				causeTasks.add(frontTask);
			}
		}

		this.evict(unrunningTask);
		return causeTasks;
	}

	////////////////////////// 用于作业监控 ////////////////////////////////////

	@Override
	public Collection<Task> getNotFoundActionTasks(int interval) {
		Criteria criteria = this.createCriteria();
		criteria.add(Restrictions.eq("scanDate", DateUtil.getToday()));
		criteria.add(Restrictions.in("taskStatus", new Long[] { (long) TaskStatus.RUNNING.indexOf(), (long) TaskStatus.RE_RUNNING.indexOf() }));
		criteria.add(Restrictions.isNull("lastActionId"));
		criteria.add(Restrictions.sqlRestriction("adddate(task_begin_time, interval " + interval + " minute) < now()"));

		return criteria.list();
	}

	@Override
	public int countNotRunSuccessTasksByJobLevel(long jobLevel, Integer interval) {
		Integer[] jobCycle = new Integer[] { JobCycle.DAY.indexOf(), JobCycle.WEEK.indexOf(), JobCycle.MONTH.indexOf() };
		Long[] taskStatus = new Long[] { (long) TaskStatus.RUN_SUCCESS.indexOf(), (long) TaskStatus.RE_RUN_SUCCESS.indexOf() };

		Criteria criteria = this.createCriteria();
		criteria.add(Restrictions.eq("taskDate", DateUtil.getToday()));
		criteria.add(Restrictions.ge("jobLevel", jobLevel));
		criteria.add(Restrictions.in("cycleType", jobCycle));
		criteria.add(Restrictions.not(Restrictions.in("taskStatus", taskStatus)));

		if (interval != null && interval.intValue() > 0) {
			criteria.add(Restrictions.sqlRestriction("adddate(setting_time, interval " + interval.intValue() + " minute) <= now()"));
		}

		return this.count(criteria);
	}

	@Override
	/**
	 * 2014.5.22 逻辑修改为： 只查询昨天的分钟，小时任务，没有完成的，进行告警。 对于天，月，周的，暂时不查询了。(因为也有一些特殊的场合，前一天的天，月，周任务没有人去及时的处理)
	 * 因为前一天的分钟，小时任务，肯定会影响当天的天，月，周任务，所以必须处理正确。
	 * 而前一天的天，月，周任务，大部分是独立的。 即使有一部分被当天任务依赖，那也是之前就有告警的，不需要用这个告警来通知
	 * 这个告警主要是通知给管理员的，因为某种原因，前一天的分钟，小时任务没有全部完成，用这个告警，做最后一层通知。
	 */
	public int countNotRunSuccessTasksByYesterday() {
		Long[] taskStatus = new Long[] { (long) TaskStatus.RUN_SUCCESS.indexOf(), (long) TaskStatus.RE_RUN_SUCCESS.indexOf() };
		Integer[] jobCycle = new Integer[] { JobCycle.MINUTE.indexOf(), JobCycle.HOUR.indexOf() };
		Criteria criteria = this.createCriteria();
		criteria.add(Restrictions.in("cycleType", jobCycle));
		criteria.add(Restrictions.eq("taskDate", DateUtil.getYesterday()));
		criteria.add(Restrictions.not(Restrictions.in("taskStatus", taskStatus)));

		return this.count(criteria);
	}

	@Override
	public Collection<Task> getRunningTasks(Date scanDate, int interval, Long[] excludeJobIds, boolean excludeRedoOrSupplyJob, boolean excludeCheckFileNumber) {
		Criteria criteria = this.createCriteria();
		criteria.add(Restrictions.eq("scanDate", scanDate));
		criteria.add(Restrictions.in("taskStatus", new Long[] { (long) TaskStatus.RUNNING.indexOf(), (long) TaskStatus.RE_RUNNING.indexOf() }));

		if (excludeJobIds != null && excludeJobIds.length > 0) {
			criteria.add(Restrictions.not(Restrictions.in("jobId", excludeJobIds)));
		}

		if (excludeRedoOrSupplyJob) {
			criteria.add(Restrictions.not(Restrictions.in("flag", new Long[] { (long) TaskFlag.REDO.indexOf(), (long) TaskFlag.SUPPLY.indexOf() })));
		}

		if (interval > 0) {
			criteria.add(Restrictions.sqlRestriction("adddate(task_begin_time, interval " + interval + " minute) <= now()"));
		}

		if (excludeCheckFileNumber) {
			criteria.add(Restrictions.not(Restrictions.eq("jobType", 10l)));
		}

		return criteria.list();
	}

	@Override
	public int countTodayRunningTasks(int beginTimeInterval, Long[] excludeJobIds, boolean excludeRedoOrSupplyJob, boolean excludeCheckFileNumber) {
		Collection<Task> runningTasks = this.getRunningTasks(DateUtil.getToday(), beginTimeInterval, excludeJobIds, excludeRedoOrSupplyJob, excludeCheckFileNumber);

		return runningTasks.size();
	}

	@Override
	public List<Long> getBigTasks(String mysql_time, String gp_time, String mysql_type, String gp_type, String task_date) throws ParseException {
		List<Long> jobids = new ArrayList<Long>();
		Collection<Long> mysqljobtypes = new ArrayList<Long>();
		String[] mysql_jobtypes = mysql_type.split(",");
		for (String mysqljobtype : mysql_jobtypes) {
			mysqljobtypes.add(Long.valueOf(mysqljobtype));
		}

		Collection<Long> gpjobtypes = new ArrayList<Long>();
		String[] gp_jobtypes = gp_type.split(",");
		for (String gpjobtype : gp_jobtypes) {
			gpjobtypes.add(Long.valueOf(gpjobtype));
		}

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		Date date = sdf.parse(task_date);

		Criteria criteria = this.createCriteria();
		criteria.add(Restrictions.eq("taskDate", date));
		criteria.add(Restrictions.in("taskStatus", new Long[] { (long) TaskStatus.RUN_SUCCESS.indexOf(), (long) TaskStatus.RE_RUN_SUCCESS.indexOf() }));
		criteria.add(Restrictions.in("jobType", mysqljobtypes));
		criteria.add(Restrictions.gt("runTime", Long.valueOf(mysql_time))); //private Long runTime; 毫秒
		criteria.setProjection(Projections.distinct(Property.forName("jobId")));
		List<Long> mysql_results = criteria.list();
		jobids.addAll(mysql_results);

		criteria = this.createCriteria();
		criteria.add(Restrictions.eq("taskDate", date));
		criteria.add(Restrictions.in("taskStatus", new Long[] { (long) TaskStatus.RUN_SUCCESS.indexOf(), (long) TaskStatus.RE_RUN_SUCCESS.indexOf() }));
		criteria.add(Restrictions.in("jobType", gpjobtypes));
		criteria.add(Restrictions.gt("runTime", Long.valueOf(gp_time))); //private Long runTime; 毫秒
		criteria.setProjection(Projections.distinct(Property.forName("jobId")));
		List<Long> gp_results = criteria.list();
		jobids.addAll(gp_results);

		return jobids;
	}
	/**
	 * add by zhoushasha 2016/5/5
	 */
	@Override
	public Collection<Task> getTasksByUserGroup(final long userGroupId) {
		
		Collection<User> users = userGroupRelationService.getUsersByUserGroup(userGroupId, false);
		StringBuffer userIds=new StringBuffer();
		for(User user:users){
			userIds.append(user.getUserId()).append(",");
		}
		final String ids=userIds.toString();
		return getHibernateTemplate().execute(new HibernateCallback<List<Task>>() {
			
			@Override
			public List<Task> doInHibernate(Session session) throws HibernateException {
				StringBuilder sql = new StringBuilder();

				sql.append("select t.* from task t ");
				sql.append("where  t.duty_officer in ( ");
				sql.append(ids.substring(0, ids.length()-1)).append(" )");


				SQLQuery query = session.createSQLQuery(sql.toString());
				query.addEntity("t", Task.class);
				
				return query.list();
			}
			

		});
		
	}

}
