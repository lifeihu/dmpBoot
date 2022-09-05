package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.Action;
import com.sw.bi.scheduler.model.RunningNumberCounterDto;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.model.TaskCreateLog;
import com.sw.bi.scheduler.service.*;
import com.sw.bi.scheduler.util.Configure;
import com.sw.bi.scheduler.util.Configure.JobCycle;
import com.sw.bi.scheduler.util.Configure.JobStatus;
import com.sw.bi.scheduler.util.Configure.JobType;
import com.sw.bi.scheduler.util.Configure.TaskStatus;
import com.sw.bi.scheduler.util.DateUtil;
import org.apache.commons.beanutils.ConvertUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.Map.Entry;

@Service
@SuppressWarnings("unchecked")
public class StatisticServiceImpl implements StatisticService {

	@Autowired
	private JobService jobService;

	@Autowired
	private TaskService taskService;

	@Autowired
	private ActionService actionService;

	@Autowired
	private TaskCreateLogService taskCreateLogService;

	@Override
	public Collection<Map<String, Integer>> statisticCycleTypeTaskNumbers(Date taskDate) {
		Criteria criteria = taskService.createCriteria();
		criteria.add(Restrictions.eq("taskDate", taskDate));
		ProjectionList projections = Projections.projectionList();
		projections.add(Projections.groupProperty("cycleType"));
		projections.add(Projections.groupProperty("taskStatus"));
		projections.add(Projections.rowCount());
		criteria.setProjection(projections);
		Collection<Object[]> results = criteria.list();

		Map<Integer, Map<String, Integer>> stats = new LinkedHashMap<Integer, Map<String, Integer>>();

		// 天任务统计
		Map<String, Integer> stat = new HashMap<String, Integer>();
		stat.put("cycleType", JobCycle.DAY.indexOf());
		stat.put("notRunning", 0);
		stat.put("running", 0);
		stat.put("runSuccess", 0);
		stat.put("runFailure", 0);
		stats.put(JobCycle.DAY.indexOf(), stat);

		// 小时任务统计
		stat = new HashMap<String, Integer>();
		stat.put("cycleType", JobCycle.HOUR.indexOf());
		stat.put("notRunning", 0);
		stat.put("running", 0);
		stat.put("runSuccess", 0);
		stat.put("runFailure", 0);
		stats.put(JobCycle.HOUR.indexOf(), stat);

		// 分钟任务统计
		stat = new HashMap<String, Integer>();
		stat.put("cycleType", JobCycle.MINUTE.indexOf());
		stat.put("notRunning", 0);
		stat.put("running", 0);
		stat.put("runSuccess", 0);
		stat.put("runFailure", 0);
		stats.put(JobCycle.MINUTE.indexOf(), stat);

		// 月任务统计
		stat = new HashMap<String, Integer>();
		stat.put("cycleType", JobCycle.MONTH.indexOf());
		stat.put("notRunning", 0);
		stat.put("running", 0);
		stat.put("runSuccess", 0);
		stat.put("runFailure", 0);
		stats.put(JobCycle.MONTH.indexOf(), stat);

		// 周任务统计
		stat = new HashMap<String, Integer>();
		stat.put("cycleType", JobCycle.WEEK.indexOf());
		stat.put("notRunning", 0);
		stat.put("running", 0);
		stat.put("runSuccess", 0);
		stat.put("runFailure", 0);
		stats.put(JobCycle.WEEK.indexOf(), stat);

		// 待触发任务统计
		stat = new HashMap<String, Integer>();
		stat.put("cycleType", JobCycle.NONE.indexOf());
		stat.put("notRunning", 0);
		stat.put("running", 0);
		stat.put("runSuccess", 0);
		stat.put("runFailure", 0);
		stats.put(JobCycle.NONE.indexOf(), stat);

		for (Object[] result : results) {
			int cycleType = (Integer) result[0];
			long taskStatus = (Long) result[1];
			int count = (Integer) result[2];

			stat = stats.get(cycleType);

			switch (TaskStatus.valueOf((int) taskStatus)) {
				case RUNNING:
				case RE_RUNNING:
					stat.put("running", stat.get("running") + count);
					break;
				case RUN_SUCCESS:
				case RE_RUN_SUCCESS:
					stat.put("runSuccess", stat.get("runSuccess") + count);
					break;
				case RUN_FAILURE:
				case RE_RUN_FAILURE:
					stat.put("runFailure", stat.get("runFailure") + count);
					break;
				default:
					stat.put("notRunning", stat.get("notRunning") + count);
					break;
			}

			stats.put(cycleType, stat);
		}

		int notRunning = 0, running = 0, runSuccess = 0, runFailure = 0, total = 0;
		for (Map<String, Integer> s : stats.values()) {
			int notRunningCount = s.get("notRunning");
			int runningCount = s.get("running");
			int runSuccesCount = s.get("runSuccess");
			int runFailureCount = s.get("runFailure");
			int totalCount = notRunningCount + runningCount + runSuccesCount + runFailureCount;
			s.put("total", totalCount);

			notRunning += notRunningCount;
			running += runningCount;
			runSuccess += runSuccesCount;
			runFailure += runFailureCount;
			total += totalCount;
		}

		stat = new HashMap<String, Integer>();
		stat.put("notRunning", notRunning);
		stat.put("running", running);
		stat.put("runSuccess", runSuccess);
		stat.put("runFailure", runFailure);
		stat.put("total", total);
		stats.put(7, stat);

		return new ArrayList<Map<String, Integer>>(stats.values());
	}

	@Override
	public Collection<Map<String, Object>> statisticTaskCompletions(Date startTaskDate, Date endTaskDate) {
		Collection<TaskCreateLog> taskCreateLogs = taskCreateLogService.getTaskCreateLogs(startTaskDate, endTaskDate);
		Collection<Map<String, Object>> results = new ArrayList<Map<String, Object>>();

		Map<String, Object> result = null;
		for (TaskCreateLog taskCreateLog : taskCreateLogs) {
			Date taskDate = taskCreateLog.getTaskDate();
			Date lastCompleteTime = null;
			int uncomplete = 0;

			if (!taskCreateLog.getRunSuccess()) {
				Criteria criteria = taskService.createCriteria();
				criteria.add(Restrictions.eq("taskDate", taskDate));
				criteria.add(Restrictions.not(Restrictions.in("taskStatus", new Long[] { (long) TaskStatus.RUN_SUCCESS.indexOf(), (long) TaskStatus.RE_RUN_SUCCESS.indexOf() })));
				criteria.setProjection(Projections.rowCount());
				uncomplete = (Integer) criteria.uniqueResult();
			} else {
				lastCompleteTime = taskCreateLog.getUpdateTime();
			}

			result = new HashMap<String, Object>();
			result.put("taskDate", taskDate);
			result.put("uncomplete", uncomplete);
			result.put("lastCompleteTime", lastCompleteTime);
			results.add(result);
		}

		return results;
	}

	/**
	 * 运行时长统计
	 */
	@Override
	public Collection<Map<String, Object>> statisticTaskRunTimes(Collection<Integer[]> runTimeRanges, Date taskDate) {
		Criteria criteria = taskService.createCriteria();
		criteria.add(Restrictions.eq("taskDate", taskDate));
		criteria.add(Restrictions.in("taskStatus", Configure.TASK_FOREGROUND_SUCCESS_STATUS));
		criteria.add(Restrictions.isNotNull("runTime"));
		Collection<Task> tasks = criteria.list();

		Map<String, Long[]> runTimeMapping = new LinkedHashMap<String, Long[]>();
		for (Integer[] runTimeRange : runTimeRanges) {
			runTimeMapping.put(runTimeRange[0] + "," + runTimeRange[1], new Long[] { 0l, 0l });
		}

		for (Task task : tasks) {
			long runTime = task.getRunTime() == null ? 0 : task.getRunTime();

			for (Integer[] runTimeRange : runTimeRanges) {
				int startRange = runTimeRange[0];
				int endRange = runTimeRange[1];
				boolean isInRange = false;

				if (startRange != 0 && endRange != 0) {
					isInRange = runTime >= startRange && runTime < endRange;
				} else if (startRange > 0) {
					isInRange = runTime >= startRange;
				} else if (endRange > 0) {
					isInRange = runTime < endRange;
				}

				if (isInRange) {
					String key = startRange + "," + endRange;
					Long[] result = runTimeMapping.get(key);

					result[0] = result[0] + 1;
					result[1] = result[1] + ((int) (runTime / 1000 / 60));
					runTimeMapping.put(key, result);
				}
			}
		}

		Collection<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		for (Entry<String, Long[]> entry : runTimeMapping.entrySet()) {
			String runTimeRange = entry.getKey();
			Long[] value = entry.getValue();

			Map<String, Object> result = new HashMap<String, Object>();
			result.put("runTimeRange", runTimeRange);
			result.put("taskCount", value[0]);
			result.put("sumRunTime", value[1]);

			results.add(result);
		}

		return results;
	}

	/**
	 * 作业统计
	 */
	@Override
	public Collection<Map<String, Integer>> statisticJobCycleType() {
		// 初始化各个作业周期内的各作业类型的数量
		Map<Long, Map<String, Integer>> cycleTypeMapping = new HashMap<Long, Map<String, Integer>>();
		for (JobCycle jobCycle : JobCycle.values()) {
			Map<String, Integer> jobTypeMapping = new HashMap<String, Integer>();
			jobTypeMapping.put("dataxCount", 0);
			jobTypeMapping.put("hiveCount", 0);
			jobTypeMapping.put("mapreduceCount", 0);
			jobTypeMapping.put("shellCount", 0);
			jobTypeMapping.put("ftpCount", 0);
			jobTypeMapping.put("procedureCount", 0);
			jobTypeMapping.put("virtualCount", 0);
			jobTypeMapping.put("mailCount", 0);
			jobTypeMapping.put("reportCount", 0);
			jobTypeMapping.put("dependencyCount", 0);

			cycleTypeMapping.put((long) jobCycle.indexOf(), jobTypeMapping);
		}

		Criteria criteria = jobService.createCriteria();
		criteria.add(Restrictions.eq("jobStatus", (long) JobStatus.ON_LINE.indexOf()));
		ProjectionList pl = Projections.projectionList();
		pl.add(Projections.groupProperty("cycleType"));
		pl.add(Projections.groupProperty("jobType"));
		pl.add(Projections.rowCount());
		criteria.setProjection(pl);
		criteria.addOrder(Order.asc("cycleType"));
		Collection<Object[]> jobs = criteria.list();

		for (Object[] job : jobs) {
			Long cycleType = (Long) job[0];
			Long jobType = (Long) job[1];
			Integer count = (Integer) job[2];
			Map<String, Integer> jobTypeMapping = cycleTypeMapping.get(cycleType);

			int dataxCount = jobTypeMapping.get("dataxCount");
			int hiveCount = jobTypeMapping.get("hiveCount");
			int mapreduceCount = jobTypeMapping.get("mapreduceCount");
			int shellCount = jobTypeMapping.get("shellCount");
			int ftpCount = jobTypeMapping.get("ftpCount");
			int procedureCount = jobTypeMapping.get("procedureCount");
			int virtualCount = jobTypeMapping.get("virtualCount");
			int mailCount = jobTypeMapping.get("mailCount");
			int reportCount = jobTypeMapping.get("reportCount");
			int dependencyCount = jobTypeMapping.get("dependencyCount");

			switch (JobType.valueOf(jobType.intValue())) {
				case HIVE_SQL:
					hiveCount += count;
					break;
				case MAPREDUCE:
					mapreduceCount += count;
					break;
				case SHELL:
					shellCount += count;
					break;
				case FTP_FILE_TO_HDFS:
				case FTP_FILE_TO_HDFS_YESTERDAY:
				case FTP_FILE_TO_HDFS_FIVE_MINUTE:
					ftpCount += count;
					break;
				case STORE_PROCEDURE:
					procedureCount += count;
					break;
				case VIRTUAL:
					virtualCount += count;
					break;
				case MAIL:
					mailCount += count;
					break;
				case REPORT_QUALITY:
					reportCount += count;
					break;
				case CHECK_DAY_DEPENDENCY_HOUR:
				case CHECK_MONTH_DEPENDENCY_DAY:
					dependencyCount += count;
					break;
				case HDFS_TO_HDFS:
				case HDFS_TO_MYSQL:
				case HDFS_TO_LOCAL_FILE:
				case HDFS_TO_SQLSERVER:
				case HDFS_TO_ORACLE:
				case MYSQL_TO_HDFS:
				case MYSQL_TO_MYSQL:
				case MYSQL_TO_LOCAL_FILE:
				case MYSQL_TO_SQLSERVER:
				case MYSQL_TO_ORACLE:
				case LOCAL_FILE_TO_HDFS:
				case LOCAL_FILE_TO_MYSQL:
				case LOCAL_FILE_TO_LOCAL_FILE:
				case LOCAL_FILE_TO_SQLSERVER:
				case LOCAL_FILE_TO_ORACLE:
				case SQLSERVER_TO_HDFS:
				case SQLSERVER_TO_MYSQL:
				case SQLSERVER_TO_LOCAL_FILE:
				case SQLSERVER_TO_SQLSERVER:
				case SQLSERVER_TO_ORACLE:
				case ORACLE_TO_HDFS:
				case ORACLE_TO_MYSQL:
				case ORACLE_TO_LOCAL_FILE:
				case ORACLE_TO_SQLSERVER:
				case ORACLE_TO_ORACLE:
				case DATAX_CUSTOM_XML:
					dataxCount += count;
					break;

			}

			jobTypeMapping.put("dataxCount", dataxCount);
			jobTypeMapping.put("hiveCount", hiveCount);
			jobTypeMapping.put("mapreduceCount", mapreduceCount);
			jobTypeMapping.put("shellCount", shellCount);
			jobTypeMapping.put("ftpCount", ftpCount);
			jobTypeMapping.put("procedureCount", procedureCount);
			jobTypeMapping.put("virtualCount", virtualCount);
			jobTypeMapping.put("mailCount", mailCount);
			jobTypeMapping.put("reportCount", reportCount);
			jobTypeMapping.put("dependencyCount", dependencyCount);
			cycleTypeMapping.put(cycleType, jobTypeMapping);
		}

		int datax = 0, hive = 0, mapreduce = 0, shell = 0, ftp = 0, procedure = 0, virtual = 0, mail = 0, report = 0, dependency = 0, total = 0;
		Collection<Map<String, Integer>> results = new ArrayList<Map<String, Integer>>();
		for (Entry<Long, Map<String, Integer>> entry : cycleTypeMapping.entrySet()) {
			Long cycleType = entry.getKey();
			Map<String, Integer> result = entry.getValue();

			int dataxCount = result.get("dataxCount");
			int hiveCount = result.get("hiveCount");
			int mapreduceCount = result.get("mapreduceCount");
			int shellCount = result.get("shellCount");
			int ftpCount = result.get("ftpCount");
			int procedureCount = result.get("procedureCount");
			int virtualCount = result.get("virtualCount");
			int mailCount = result.get("mailCount");
			int reportCount = result.get("reportCount");
			int dependencyCount = result.get("dependencyCount");
			int totalCount = dataxCount + hiveCount + mapreduceCount + shellCount + ftpCount + procedureCount + virtualCount + mailCount + reportCount + dependencyCount;

			result.put("cycleType", cycleType.intValue());
			result.put("total", totalCount);
			results.add(result);

			datax += dataxCount;
			hive += hiveCount;
			mapreduce += mapreduceCount;
			shell += shellCount;
			ftp += ftpCount;
			procedure += procedureCount;
			virtual += virtualCount;
			mail += mailCount;
			report += reportCount;
			dependency += dependencyCount;
			total += totalCount;
		}

		Map<String, Integer> result = new HashMap<String, Integer>();
		result.put("dataxCount", datax);
		result.put("hiveCount", hive);
		result.put("mapreduceCount", mapreduce);
		result.put("shellCount", shell);
		result.put("ftpCount", ftp);
		result.put("procedureCount", procedure);
		result.put("virtualCount", virtual);
		result.put("mailCount", mail);
		result.put("reportCount", report);
		result.put("dependencyCount", dependency);
		result.put("total", total);
		results.add(result);

		return results;
	}

	
	/**
	 *  功能点： 每日任务分布统计
	 *  用来验证2013-09-12 04:25:00 间隔5分钟的情况
		select * from action where scan_date  = '2013-09-12' 
		and start_time < '2013-09-12 04:30:00'
		and end_time >= '2013-09-12 04:25:00';
		
		验证2013-09-12 04:25:00这个时间点，正在运行的任务数
		1。 首先要查询action表，scan_date是2013-09-12
		2。 开始时间要小于下一个时间点
		3。 结束时间要大于等于当前时间点
		
		如果要知道精确一些，如就是某个分钟点上，正在运行的任务数，则把时间间隔缩小为1分钟，用如下SQL查询
		select * from action where scan_date  = '2013-09-12' 
		and start_time < '2013-09-12 04:26:00'
		and end_time >= '2013-09-12 04:25:00';
	 */
	@Override
	public Collection<RunningNumberCounterDto> statisticTaskRunningNumber4Chart(String jobType, int interval, Date startTime, Date endTime) {
		Date scanDate = DateUtil.clearTime(startTime);  // 指定日期的 00:00:00

		Criteria criteria = actionService.createCriteria();
		criteria.addOrder(Order.asc("settingTime"));
		criteria.addOrder(Order.asc("startTime"));
		criteria.add(Restrictions.eq("scanDate", scanDate));  // 要查询指定日期，各个时刻的正在运行的任务数量，考虑到可能存在重跑或者补历史数据的情况，所以查询时，应该查询action表，且用scan_date等于指定日期来查询
		criteria.add(Restrictions.ge("endTime", startTime));  // 如果页面上，还限制了时间范围段，则应该满足 startTime<=结束时间  and endTime >=开始时间
		criteria.add(Restrictions.le("startTime", endTime));
		if (StringUtils.hasText(jobType)) {
			criteria.add(Restrictions.in("jobType", (Long[]) ConvertUtils.convert(jobType.split(","), Long.class)));
		}
		Collection<Action> actions = criteria.list();

		long startMillis = scanDate.getTime();
		long endMillis = endTime.getTime();

		// 用于后面X轴起始时间点的计算
		int minStartPhase = (int) ((startTime.getTime() - startMillis) / 1000 / 60 / interval);

		// 每个周期的统计值
		Map<Integer, RunningNumberCounterDto> phaseCounter = new HashMap<Integer, RunningNumberCounterDto>();

		for (Action action : actions) {
			long startTimeMillis = action.getStartTime().getTime();
			long endTimeMillis = action.getEndTime() == null ? System.currentTimeMillis() : action.getEndTime().getTime();

			int startPhase = Math.max((int) ((startTimeMillis - startMillis) / 1000 / 60 / interval), minStartPhase);
			int endPhase = (int) ((endTimeMillis - startMillis) / 1000 / 60 / interval);

			for (int i = startPhase; i <= endPhase; i++) {
				RunningNumberCounterDto counter = phaseCounter.get(i);
				if (counter == null) {
					counter = new RunningNumberCounterDto();
				}

				counter.addCounter();
				counter.addJobId(action.getJobId());
				counter.addActionId(action.getActionId());

				phaseCounter.put(i, counter);
			}
		}

		Collection<RunningNumberCounterDto> results = new ArrayList<RunningNumberCounterDto>();

		// 计算X轴开始的时间点
		Calendar calendar = DateUtil.getCalendar(scanDate);
		calendar.add(Calendar.MINUTE, minStartPhase * interval);

		int currentPhase = minStartPhase;
		String scanDateStr = DateUtil.formatDate(scanDate);
		while (calendar.getTimeInMillis() < endMillis) {
			Date date = calendar.getTime();
			if (date.getTime() >= startTime.getTime()) {
				RunningNumberCounterDto counter = phaseCounter.get(currentPhase);
				if (counter == null) {
					counter = new RunningNumberCounterDto();
				}

				counter.setScanDate(scanDateStr);
				counter.setTime(DateUtil.format(date, "HH:mm"));

				results.add(counter);
			}

			calendar.add(Calendar.MINUTE, interval);
			currentPhase += 1;
		}

		return results;
	}

	@Override
	public Collection<Map<String, Object>> statisticTaskRunTime4Chart(Long jobId, Date startDate, Date endDate, String time) {
		Criteria criteria = taskService.createCriteria();
		criteria.add(Restrictions.eq("jobId", jobId));
		criteria.add(Restrictions.ge("taskDate", startDate));
		criteria.add(Restrictions.le("taskDate", endDate));
		criteria.addOrder(Order.asc("taskDate"));
		Collection<Task> tasks = criteria.list();

		Collection<Map<String, Object>> results = new ArrayList<Map<String, Object>>();

		for (Task task : tasks) {
			if (StringUtils.hasText(time)) {
				if (!DateUtil.format(task.getSettingTime(), "HH:mm").equals(time)) {
					continue;
				}
			}

			Map<String, Object> record = new HashMap<String, Object>();

			record.put("taskId", task.getTaskId());
			record.put("name", task.getName());
			record.put("taskDate", DateUtil.formatDate(task.getTaskDate()));
			record.put("runTime", task.getRunTime() == null ? 0 : task.getRunTime() / 1000 / 60);

			results.add(record);
		}

		return results;
	}
}
