package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.background.exception.GatewayNotFoundException;
import com.sw.bi.scheduler.background.taskexcuter.Parameters;
import com.sw.bi.scheduler.model.*;
import com.sw.bi.scheduler.service.*;
import com.sw.bi.scheduler.util.Configure.*;
import com.sw.bi.scheduler.util.DateUtil;
import com.sw.bi.scheduler.util.ExecAgent.ExecResult;
import com.sw.bi.scheduler.util.OperateAction;
import com.sw.bi.scheduler.util.SshUtil;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate5.HibernateCallback;
import org.springframework.resolver.Warning;
import org.springframework.stereotype.Service;
import org.springframework.ui.ConditionModel;
import org.springframework.ui.PaginationSupport;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.Map.Entry;

@Service("actionService")
@SuppressWarnings("unchecked")
public class ActionServiceImpl extends GenericServiceHibernateSupport<Action> implements ActionService {

	@Autowired
	private TaskService taskService;

	@Autowired
	private UserService userService;

	@Autowired
	private GatewayService gatewayService;

	@Autowired
	private RedoAndSupplyHistoryService redoAndSupplyHistoryService;

	@Autowired
	private ScheduleSystemStatusService scheduleSystemStatusService;

	@Autowired
	private ConcurrentService concurrentService;

	public boolean excuteProcedure(String procName, Object[] params) {
		// TODO Auto-generated method stub

		String sql = "call " + procName + "(";
		for (Object param : params) {
			sql += "?,";
		}
		if (params.length > 0) {
			sql = sql.substring(0, sql.length() - 1);
		}
		sql += ")";
		SQLQuery sqlQuery = this.getCurrentSession().createSQLQuery("{" + sql + "}");

		for (int i = 1; i <= params.length; i++) {
			sqlQuery.setParameter(i, params[i]);
		}
		try {
			return sqlQuery.executeUpdate() >= 0;
		} catch (Exception ex) {
			return false;
		}
	}

	@Override
	public Action create(Task task) {
		Action action = new Action();

		action.setTaskId(task.getTaskId());
		action.setFlag((int) task.getFlag());
		action.setJobId(task.getJobId());
		action.setJobType(task.getJobType());
		action.setJobName(task.getJobName());
		action.setSettingTime(task.getSettingTime());
		action.setCycleType(task.getCycleType());
		action.setTaskDate(task.getTaskDate());
		action.setScanDate(task.getScanDate());
		action.setCreateTime(new Date());

		if (action.getFlag() == TaskFlag.SYSTEM.indexOf()) {
			action.setOperator("系统");
		} else if (task.getUpdateBy() != null) {
			User user = userService.get(task.getUpdateBy());
			action.setOperator(user.getRealName());
		} else {
			action.setOperator("系统");
		}

		return action;
	}

	@Override
	public PaginationSupport pagingBySql(final ConditionModel cm) {
		//modify by zhoushasha 用户只能查看当前组及下属组的action ，先获取当前组的taskId，再通过taskId查找action
		Long groupId=cm.getValue("userGroupId", Long.class);
		Collection<Task> tasks=taskService.getTasksByUserGroup(groupId);
		final List<Long> taskIds=new ArrayList<Long>();
		for(Task task:tasks){
			taskIds.add(task.getTaskId());
		}
		if(taskIds.size()==0){
			return null;
		}
		return getHibernateTemplate().execute(new HibernateCallback<PaginationSupport>() {

			@SuppressWarnings("rawtypes")
			@Override
			public PaginationSupport doInHibernate(Session session) throws HibernateException {
				Long taskId = cm.getValue("taskId", Long.class);
				String jobId = cm.getValue("jobId", String.class);
				String jobName = cm.getValue("jobName", String.class);
				Date settingTime = cm.getValue("settingTime", Date.class);
				Long actionStatus = cm.getValue("actionStatus", Long.class);
				Integer flag = cm.getValue("flag", Integer.class);
				String operator = cm.getValue("operator", String.class);
				Date startDate = cm.getValue("taskDateStart", Date.class);
				Date endDate = cm.getValue("taskDateEnd", Date.class);
				boolean useScanDate = "on".equals(cm.getValue("useScanDate", String.class));
				String gateway = cm.getValue("gateway", String.class);
				String jobType = cm.getValue("jobType", String.class);
				Date scanDate = cm.getValue("scanDate", Date.class);
				Date startTime = cm.getValue("startTime", Date.class);
				Date endTime = cm.getValue("endTime", Date.class);
				String actionId = cm.getValue("actionId", String.class);

				List<String> clauses = new ArrayList<String>();
				Map<String, Object> params = new HashMap<String, Object>();

				if (taskId != null) {
					clauses.add("task_id = :taskId");
					params.put("taskId", taskId);
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

				if (StringUtils.hasText(actionId)) {
					if (actionId.indexOf(",") == -1) {
						clauses.add("action_id = :actionId");
						params.put("actionId", Long.valueOf(actionId));
					} else {
						clauses.add("action_id in (:actionId)");
						params.put("actionId", Arrays.asList(actionId.split(",")));
					}
				}

				if (StringUtils.hasText(jobName)) {
					clauses.add("job_name like :jobName");
					params.put("jobName", "%" + jobName + "%");
				}

				if (settingTime != null) {
					settingTime.setSeconds(0);
					clauses.add("setting_time = :settingTime");
					params.put("settingTime", settingTime);
				}

				if (actionStatus != null) {
					clauses.add("action_status = :actionStatus");
					params.put("actionStatus", actionStatus);
				}

				if (flag != null) {
					clauses.add("flag = :flag");
					params.put("flag", flag);
				}

				if (StringUtils.hasText(operator)) {
					clauses.add("operator = :operator");
					params.put("operator", operator);
				}

				if (useScanDate) {
					if (scanDate != null) {
						clauses.add("scan_date = :scanDate");
						params.put("scanDate", scanDate);
					} else {
						clauses.add("scan_date = :todayScanDate");
						params.put("todayScanDate", DateUtil.getToday());
					}
				}

				if (startDate != null && endDate != null) {
					if (startDate.equals(endDate)) {
						clauses.add("task_date = :taskDate");
						params.put("taskDate", startDate);
					} else {
						clauses.add("task_date >= :taskDateStart and task_date <= :taskDateEnd");
						params.put("taskDateEnd", endDate);
						params.put("taskDateStart", startDate);
					}
				}

				if (startTime != null) {
					clauses.add("start_time >= :startTime");
					params.put("startTime", startTime);
				}

				if (endTime != null) {
					clauses.add("end_time <= :endTime");
					params.put("endTime", endTime);
				}

				if (StringUtils.hasText(gateway)) {
					clauses.add("gateway = :gateway");
					params.put("gateway", gateway);
				}

				if (StringUtils.hasText(jobType)) {
					clauses.add("job_type in (:jobType)");
					params.put("jobType", Arrays.asList(jobType.split(",")));
				}
				//add by zhoushasha 2016/5/5 
				if(taskIds.size()!=0){
					clauses.add("task_id in (:task_id)");
					params.put("task_id", taskIds);
				}
				String sql = "from action";
				if (clauses.size() > 0) {
					sql += " where";
				}
				for (int i = 0; i < clauses.size(); i++) {
					sql += (i == 0 ? " " : " and ") + clauses.get(i);
				}

				//////////////////////////////////////////////////////////////////////////////////

				String resultSql = "select action_id, task_id, job_id, job_name, action_status, start_time, end_time, task_date, flag, operator, cycle_type, setting_time, gateway, job_type " + sql +
						" order by ";

				if (cm.getOrderByCount() > 0) {
					String order = cm.toOrderBySqlString();
					// order = order.replace("run_time", "period_diff(end_time, start_time)");
					resultSql += order;

					if (cm.getOrderBy("jobName") != null) {
						resultSql += ", setting_time";
					}
				} else {
					resultSql += " start_time desc";
				}
				log.info("action query: " + resultSql);

				SQLQuery query = session.createSQLQuery(resultSql);
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

					if (key.startsWith("taskDate")) {
						log.info("param is " + key + ": " + DateUtil.formatDate((Date) value));
					} else {
						log.info("param is " + key + ": " + value);
					}
				}

				PaginationSupport ps = new PaginationSupport(cm.getStart(), cm.getLimit());

				Collection<Object[]> results = query.list();
				for (Object[] result : results) {
					Map<String, Object> action = new HashMap<String, Object>();

					startTime = (Date) result[5];
					endTime = null;
					Long runTime = null;
					long status = ((Integer) result[4]).longValue();

					if (ActionStatus.RUNNING.indexOf() != status) {
						endTime = (Date) result[6];
						runTime = 0l;
						if (startTime != null && endTime != null) {
							runTime = (endTime.getTime() - startTime.getTime()) / 1000 / 60;
						}
					}

					String name = (String) result[3];
					int cycleType = (Integer) result[10];
					settingTime = (Date) result[11];

					// 小时作业或分钟作业时把具体的时间加上
					if (JobCycle.HOUR.indexOf() == cycleType || JobCycle.MINUTE.indexOf() == cycleType) {
						name += "(" + DateUtil.format(settingTime, "HH:mm") + ")";
					}

					action.put("actionId", result[0]);
					action.put("taskId", result[1]);
					action.put("jobId", result[2]);
					action.put("jobName", name);
					action.put("actionStatus", result[4]);
					action.put("startTime", startTime);
					action.put("endTime", endTime);
					action.put("runTime", runTime);
					action.put("taskDate", result[7]);
					action.put("flag", result[8]);
					action.put("operator", result[9]);
					action.put("gateway", result[12]);
					action.put("jobType", result[13]);

					ps.getPaginationResults().add(action);
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
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("jobId", job.getJobId()));
		criteria.add(Restrictions.eq("taskDate", DateUtil.getToday()));
		List<Action> actions = criteria.list();

		for (Action action : actions) {
			action.setJobName(job.getJobName());

			this.update(action);
		}
	}

	@Override
	public int countTodayRunningTasks(String gateway) throws GatewayNotFoundException {
		Long[] taskCountExceptJobs = scheduleSystemStatusService.getTaskCountExceptJobIds(gateway);

		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("gateway", gateway));
		criteria.add(Restrictions.eq("scanDate", DateUtil.getToday()));
		criteria.add(Restrictions.eq("actionStatus", ActionStatus.RUNNING.indexOf()));

		if (taskCountExceptJobs != null && taskCountExceptJobs.length > 0) {
			criteria.add(Restrictions.not(Restrictions.in("jobId", taskCountExceptJobs)));
		}

		criteria.setProjection(Projections.rowCount());
		Integer count = (Integer) criteria.uniqueResult();

		return count == null ? 0 : count;
	}

	@Override
	public Map<Integer, Collection<Action>> countTodayRunningTasksByConcurrentJobType(String gateway) throws GatewayNotFoundException {
		Collection<Integer> categories = concurrentService.getConcurrentCategories();
		Long[] taskCountExceptJobs = scheduleSystemStatusService.getTaskCountExceptJobIds(gateway);

		Map<Integer, Collection<Action>> countMapping = new HashMap<Integer, Collection<Action>>();
		for (Integer category : categories) {
			Collection<Long> jobTypes = concurrentService.getConcurrentJobTypes(category);

			// 统计指定分类下的作业类型当前正在运行数量
			Criteria criteria = createCriteria();
			criteria.add(Restrictions.eq("scanDate", DateUtil.getToday()));
			criteria.add(Restrictions.eq("actionStatus", ActionStatus.RUNNING.indexOf()));
			criteria.add(Restrictions.in("jobType", jobTypes));

			if (taskCountExceptJobs != null && taskCountExceptJobs.length > 0) {
				criteria.add(Restrictions.not(Restrictions.in("jobId", taskCountExceptJobs)));
			}

			/*criteria.setProjection(Projections.rowCount());
			Number count = (Number) criteria.uniqueResult();
			if (count == null) {
				count = 0;
			}*/

			countMapping.put(category, criteria.list());
		}

		return countMapping;
	}

	@Override
	@Deprecated
	public Integer countTodayToMySQLTasks(String gateway) throws GatewayNotFoundException {
		Long[] jobTypes = new Long[] { (long) JobType.HDFS_TO_MYSQL.indexOf(), (long) JobType.LOCAL_FILE_TO_MYSQL.indexOf(), (long) JobType.MYSQL_TO_MYSQL.indexOf(),
				(long) JobType.SQLSERVER_TO_MYSQL.indexOf(), (long) JobType.ORACLE_TO_MYSQL.indexOf() };
		Long[] taskCountExceptJobs = scheduleSystemStatusService.getTaskCountExceptJobIds(gateway);

		Criteria criteria = createCriteria();
		// criteria.add(Restrictions.eq("gateway", gateway)); //注意,加了网关机这个条件以后,效果是每台网关机上正在运行的导入mysql类型的任务数,而不是整个集群
		criteria.add(Restrictions.eq("scanDate", DateUtil.getToday()));
		criteria.add(Restrictions.eq("actionStatus", ActionStatus.RUNNING.indexOf()));
		criteria.add(Restrictions.in("jobType", jobTypes)); //限定为 导入到mysql的任务类型

		if (taskCountExceptJobs != null && taskCountExceptJobs.length > 0) {
			criteria.add(Restrictions.not(Restrictions.in("jobId", taskCountExceptJobs)));
		}

		criteria.setProjection(Projections.rowCount());
		Integer count = (Integer) criteria.uniqueResult();

		return count == null ? 0 : count;
	}

	@Override
	public String getActionLog(Long actionId) {
		if (actionId == null) {
			return null;
		}

		Action action = get(actionId);

		String actionLog = action.getActionLog();
		if (!actionLog.endsWith(".log")) {
			actionLog += actionId + ".log";
		}

		long jobType = action.getJobType();
		if (Arrays.binarySearch(Parameters.DataxState, jobType) > -1) {
			// 对于DataX作业日志信息需要到tmp目录中查找
			int pos = actionLog.lastIndexOf("/") + 1;
			actionLog = actionLog.substring(0, pos) + "tmp/" + actionLog.substring(pos);
		}

		String std = null;
		ExecResult execResult = SshUtil.execCommand(action.getGateway(), "cat " + actionLog);
		if (execResult.failure()) {
			std = execResult.getStderr();

		} else {
			// 如果是分支作业则需要展开具体执行分支的日志
			if (action.getJobType() == JobType.BRANCH.indexOf()) {
				StringBuffer result = new StringBuffer();
				String[] contents = execResult.getStdoutAsArrays(); // content.split("\n");
				for (String line : contents) {
					if (line.endsWith(".log")) {
						String[] token = line.split(" - ");
						actionLog = token[1];

						// 从日志文件路径中解析ActionID
						String branchActionId = actionLog.substring(actionLog.lastIndexOf("/") + 1, actionLog.lastIndexOf("."));
						log.info("branch action id: " + branchActionId);
						if (StringUtils.hasText(branchActionId)) {
							Action branchAction = this.get(Long.valueOf(branchActionId));
							log.info("branch action: " + branchAction);
							if (branchAction != null) {
								log.info("gateway: " + branchAction.getGateway() + ", log: " + actionLog);
								execResult = SshUtil.execCommand(branchAction.getGateway(), "cat " + actionLog);
								if (execResult.success()) {
									result.append(execResult.getStdout());
									continue;
								}
							}
						}

					}

					result.append(line).append("\n");
				}

				std = result.toString();

			} else {
				std = execResult.getStdout();
			}
		}

		if (StringUtils.hasText(std)) {
			std = std.replaceAll("<", "&lt;");
		}

		return std;
	}

	@Override
	public String getActionPID(Long actionId) {
		if (actionId == null) {
			return null;
		}

		Action action = get(actionId);
		String actionLog = action.getActionLog();
		if (!actionLog.endsWith(".log")) {
			actionLog += actionId + ".log";
		}

		String command = null;
		long jobType = action.getJobType();
		if (Arrays.binarySearch(Parameters.DataxState, jobType) > -1 || jobType == JobType.MAPREDUCE.indexOf() || jobType == JobType.HIVE_SQL.indexOf()) {
			// DataX、MapReduce和HiveSQL作业能精确到具体的进程ID
			command = "ps -ef | grep " + actionLog + "|grep -v \"grep\" | awk -F \" \" '{print $1, $2, $3, $5}'";
		} else {
			// 该命令只能查到整调度的进程ID
			command = "/usr/sbin/lsof " + actionLog;
		}

		ExecResult execResult = SshUtil.execCommand(action.getGateway(), command);
		return execResult.success() ? execResult.getStdout() : null;
	}

	@Override
	/**
	 * 1.前台右键杀进程(修改前台JS界面)
	 * 2.作业下线杀进程(修改TaskServiceImpl.offline)
	 * 3.模拟后台杀进程(修改TaskServiceImpl.simulateSchedule)
	 */
	public void killActionPID(Long actionId) {
		if (actionId == null) {
			return;
		}

		Action action = get(actionId);
		long jobType = action.getJobType();
		boolean killSuccess = true;
		String pid = null;

		// 获得进程信息,暂时只支持删除DataX、MapReduce和HiveSQL类型的作业PID
		if (Arrays.binarySearch(Parameters.DataxState, jobType) > -1 || jobType == JobType.MAPREDUCE.indexOf() || jobType == JobType.HIVE_SQL.indexOf()) {
			String pInfo = this.getActionPID(actionId);
			if (StringUtils.hasText(pInfo)) {
				pid = pInfo.split(" ")[1];
				ExecResult execResult = SshUtil.execCommand(action.getGateway(), "kill -9 " + pid);
				killSuccess = execResult.success();
			}
		}

		/*Action action = get(actionId);
		String gateway = action.getGateway();
		String actionLog = action.getActionLog();
		if (!actionLog.endsWith(".log")) {
			actionLog += actionId + ".log";
		}

		// 如果存在进程则删除
		ExecResult execResult = SshUtil.execCommand(gateway, "lsof " + actionLog + "|awk -F \" \" '{print $2}' | tail -1");
		if (execResult.success() && StringUtils.hasText(execResult.getStdout())) {
			SshUtil.execCommand(gateway, "kill -9 " + execResult.getStdout());
			throw new Warning("未找到进程!");
			if (StringUtils.hasText(result)) {
				throw new Warning("进程(" + pid + ")已经结束!");
			}
		}*/

		// 无论进程是否删除都将Action和Task的状态改为失败

		action.setActionStatus(ActionStatus.RUN_FAILURE.indexOf());
		super.update(action);

		Task task = taskService.get(action.getTaskId());
		task.setRunTimes(Math.max(0, task.getRunTimes() - 1));
		task.setTaskStatus(TaskStatus.RUN_FAILURE.indexOf());
		taskService.update(task);

		if (StringUtils.hasText(task.getOperateNo())) {
			RedoAndSupplyHistory rash = redoAndSupplyHistoryService.getRedoAndSupplyHistoryByTask(task.getOperateNo(), task.getTaskId());
			if (rash != null) {
				rash.setTaskStatus(TaskForegroundStatus.RUN_FAILURE.indexOf());
				rash.setEndTime(task.getTaskEndTime());
				rash.setUpdateTime(DateUtil.now());
				redoAndSupplyHistoryService.update(rash);
			}
		}

		if (!killSuccess && StringUtils.hasText(pid)) {
			throw new Warning("作业状态修改成功,但进程(#" + pid + ")删除失败.");
		}

		StringBuilder logger = new StringBuilder();
		logger.append("[").append(task.getEntityName()).append("]");
		logger.append(".[").append(StringUtils.hasText(pid) ? "进程号: " + pid + ", " : "").append(task.getLoggerName()).append("]");
		getOperateLoggerService().log(OperateAction.KILL_PID, logger.toString());
	}

	@Override
	@Deprecated
	public void killGatewayPID(Date taskDate) {
		// TODO 暂时屏蔽杀进程功能
		/*String logPath = Parameters.logPath + DateUtil.formatDate(taskDate);
		for (Gateway gateway : gateways) {
			String result = SshUtil.execCommand(gateway.getName(), "lsof " + logPath + "/* | awk -F \" \" '{print $2}'");

			if (StringUtils.hasText(result)) {
				continue;
			}

			String[] pids = result.split("\n");
			for (int i = 1, len = pids.length; i < len; i++) {
				String pid = pids[i];
				if (StringUtils.hasText(pid)) {
					SshUtil.execCommand(gateway.getName(), "kill -9 " + pid);
				}
			}
		}*/

		// 修改所有正在运行的Action
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("scanDate", taskDate));
		criteria.add(Restrictions.eq("actionStatus", ActionStatus.RUNNING.indexOf()));
		Collection<Action> actions = criteria.list();
		for (Action action : actions) {
			action.setActionStatus(ActionStatus.RUN_FAILURE.indexOf());
			super.update(action);
		}

		// 修改所有正在运行的Task
		criteria = taskService.createCriteria();
		criteria.add(Restrictions.eq("scanDate", taskDate));
		criteria.add(Restrictions.in("taskStatus", new Long[] { (long) TaskStatus.RUNNING.indexOf(), (long) TaskStatus.RE_RUNNING.indexOf() }));
		Collection<Task> tasks = criteria.list();
		for (Task task : tasks) {
			task.setRunTimes(Math.max(0, task.getRunTimes() - 1));
			task.setTaskStatus(TaskStatus.RUN_FAILURE.indexOf());
			taskService.update(task);

			if (StringUtils.hasText(task.getOperateNo())) {
				RedoAndSupplyHistory rash = redoAndSupplyHistoryService.getRedoAndSupplyHistoryByTask(task.getOperateNo(), task.getTaskId());
				if (rash != null) {
					rash.setTaskStatus(TaskForegroundStatus.RUN_FAILURE.indexOf());
					rash.setEndTime(task.getTaskEndTime());
					rash.setUpdateTime(DateUtil.now());
					redoAndSupplyHistoryService.update(rash);
				}
			}
		}
	}

	@Override
	public void killGatewayPID(String gateway) {
		Date[] scanDates = new Date[] { DateUtil.getToday(), DateUtil.getYesterday() };

		// 修改最近二天所有正在运行的Action
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.in("scanDate", scanDates));
		criteria.add(Restrictions.eq("actionStatus", ActionStatus.RUNNING.indexOf()));
		if (StringUtils.hasText(gateway)) {
			criteria.add(Restrictions.eq("gateway", gateway));
		}
		Collection<Action> actions = criteria.list();
		for (Action action : actions) {
			action.setActionStatus(ActionStatus.RUN_FAILURE.indexOf());
			super.update(action);
		}

		// 修改所有正在运行的Task
		criteria = taskService.createCriteria();
		criteria.add(Restrictions.in("scanDate", scanDates));
		criteria.add(Restrictions.in("taskStatus", new Long[] { (long) TaskStatus.RUNNING.indexOf(), (long) TaskStatus.RE_RUNNING.indexOf() }));
		if (StringUtils.hasText(gateway)) {
			criteria.add(Restrictions.eq("gateway", gateway));
		}
		Collection<Task> tasks = criteria.list();
		for (Task task : tasks) {
			task.setRunTimes(Math.max(0, task.getRunTimes() - 1));
			task.setTaskStatus(TaskStatus.RUN_FAILURE.indexOf());
			taskService.update(task);

			if (StringUtils.hasText(task.getOperateNo())) {
				RedoAndSupplyHistory rash = redoAndSupplyHistoryService.getRedoAndSupplyHistoryByTask(task.getOperateNo(), task.getTaskId());
				if (rash != null) {
					rash.setTaskStatus(TaskForegroundStatus.RUN_FAILURE.indexOf());
					rash.setEndTime(task.getTaskEndTime());
					rash.setUpdateTime(DateUtil.now());
					redoAndSupplyHistoryService.update(rash);
				}
			}
		}
	}

	@Override
	public Long getLastActionIdByTask(Long taskId, Date taskDate) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("taskDate", taskDate));
		criteria.add(Restrictions.eq("taskId", taskId));
		ProjectionList projections = Projections.projectionList();
		projections.add(Projections.groupProperty("taskId"));
		projections.add(Projections.max("actionId"));
		criteria.setProjection(projections);

		Object[] result = (Object[]) criteria.uniqueResult();

		return result == null ? null : (Long) result[1];
	}

	@Override
	public List<Action> getActionsOrderByStartTimeAsc(Date taskDate) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("taskDate", taskDate));
		criteria.addOrder(Order.asc("startTime"));

		return criteria.list();
	}

	@Override
	public List<Action> getRunningActions(long taskId) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("taskId", taskId));
		criteria.add(Restrictions.eq("actionStatus", ActionStatus.RUNNING.indexOf()));

		return criteria.list();
	}

	@Override
	public List<Action> getRunningActions(Date taskDate) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("scanDate", taskDate));
		criteria.add(Restrictions.eq("actionStatus", ActionStatus.RUNNING.indexOf()));

		return criteria.list();
	}

	@Override
	public List<Action> getAllRunningActions() {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("actionStatus", ActionStatus.RUNNING.indexOf()));

		return criteria.list();
	}

	/**
	 * 判断任务是否有正在运行中的实例
	 * 如果有正在运行中的实例，则本次该任务不再提交执行，防止一个任务同时出现两条正在运行中的实例，引起一些数据准确性方面的问题
	 */
	@Override
	public boolean hasRunningActionByTaskAndScanDate(long taskId, Date scanDate) {
		Criteria criteria = createCriteria();
		criteria.setCacheable(false);
		criteria.add(Restrictions.eq("taskId", taskId));
		criteria.add(Restrictions.eq("scanDate", scanDate));
		criteria.add(Restrictions.eq("actionStatus", ActionStatus.RUNNING.indexOf()));
		int count = this.count(criteria);

		return count > 0;

		// 通过Hibernate方法获取有发现记录不同步，所以改用JDBC方式
		/*Connection connection = BeanFactory.connection();
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			StringBuilder sql = new StringBuilder("select count(*)");
			sql.append(" from `action`");
			sql.append(" where task_id = ?");
			sql.append(" and scan_date = ?");
			sql.append(" and action_status = ?");

			pstmt = connection.prepareStatement(sql.toString());

			pstmt.setObject(1, taskId);
			pstmt.setObject(2, scanDate);
			pstmt.setObject(3, ActionStatus.RUNNING.indexOf());

			rs = pstmt.executeQuery();
			if (rs.next()) {
				int count = rs.getInt(1);
				return count > 0;
			}

		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			try {
				if (rs != null) {
					rs.close();
				}

				if (pstmt != null) {
					pstmt.close();
				}

				if (connection != null) {
					connection.close();
				}
			} catch (Exception e) {

			}
		}

		return false;*/
	}

	//////////////////////////用于作业监控 ////////////////////////////////////

	@Override
	@Deprecated
	public Action getAction(Long taskId, Date taskDate, Date startTime, Date createTime) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("taskId", taskId));
		criteria.add(Restrictions.eq("taskDate", taskDate));
		criteria.add(Restrictions.eq("startTime", startTime));
		criteria.add(Restrictions.eq("createTime", createTime));
		criteria.add(Restrictions.eq("actionStatus", ActionStatus.RUN_SUCCESS.indexOf()));

		return (Action) criteria.uniqueResult();
	}

	@Override
	public int countTodayRunningActions(int beginTimeInterval, Long[] excludeJobIds, boolean excludeRedoOrSupplyJob,boolean excludeCheckFileNumber) {
		Criteria criteria = this.createCriteria();
		criteria.add(Restrictions.eq("scanDate", DateUtil.getToday()));
		criteria.add(Restrictions.eq("actionStatus", ActionStatus.RUNNING.indexOf()));

		if (excludeJobIds != null && excludeJobIds.length > 0) {
			criteria.add(Restrictions.not(Restrictions.in("jobId", excludeJobIds)));
		}

		if (excludeRedoOrSupplyJob) {
			criteria.add(Restrictions.not(Restrictions.in("flag", new Integer[] { TaskFlag.REDO.indexOf(), TaskFlag.SUPPLY.indexOf() })));
		}

		if (beginTimeInterval > 0) {
			criteria.add(Restrictions.sqlRestriction("adddate(start_time, interval " + beginTimeInterval + " minute) <= now()"));
		}
		
		if (excludeCheckFileNumber) {
			criteria.add(Restrictions.not(Restrictions.eq("jobType", 10l)));
		}

		return this.count(criteria);
	}
}
