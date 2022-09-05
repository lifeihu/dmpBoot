package com.sw.bi.scheduler.background.taskexcuter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.model.Job;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.service.JobRelationService;
import com.sw.bi.scheduler.util.Configure.JobType;
import com.sw.bi.scheduler.util.DateUtil;

@SuppressWarnings("unchecked")
public class CheckDependencyExcuter extends AbExcuter {
	private static final long TIMEOUT = 12 * 60 * 60 * 1000; // 12小时
	private static final long INTERVAL = 3 * 60 * 1000; // 3分钟
	private static long waiteTime = 0;
	public static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	private JobRelationService jobRelationService = BeanFactory.getService(JobRelationService.class);

	/**
	 * 父作业ID
	 */
	private Collection<Long> parentJobIds;

	/**
	 * 父作业的任务日期
	 */
	private Date parentTaskDate;

	public CheckDependencyExcuter(Task task, String logFolder) {
		super(task, logFolder);
	}

	@Override
	public boolean excuteCommand() throws Exception {
		int jobType = (int) currentJob.getJobType();

		Collection<Job> parentJobs = jobRelationService.getOnlineParentJobs(currentJob.getJobId());

		if (parentJobs.size() == 0) {
			logFileWriter.write(DATE_TIME_FORMAT.format(new Date())+": "+"该作业没有父作业,可以直接运行了.\n");
			logFileWriter.flush();
			return true;
		}

		parentJobIds = new ArrayList<Long>();
		for (Job parentJob : parentJobs) {
			parentJobIds.add(parentJob.getJobId());
		}
        
		Collection<Task> parentTasks = null;
		if (JobType.CHECK_DAY_DEPENDENCY_HOUR.indexOf() == jobType) {
			parentTasks = this.getDayDependencyHourParentTasks();
			logFileWriter.write(DATE_TIME_FORMAT.format(new Date())+": "+" 共获得周期为小时的父任务共: " + parentTasks.size() + " 个.\n");
			logFileWriter.flush();

		} else if (JobType.CHECK_MONTH_DEPENDENCY_DAY.indexOf() == jobType) {
			parentTasks = this.getMonthDependencyDayParentTasks();
			logFileWriter.write(DATE_TIME_FORMAT.format(new Date())+": "+" 共获得周期为天的父任务共: " + parentTasks.size() + " 个.\n");
			logFileWriter.flush();
		}

		// 校验所有父任务是否全部执行成功
		boolean success = true;
		for (Task parentTask : parentTasks) {
			if (!parentTask.isRunSuccess()) {
				logFileWriter.write(DATE_TIME_FORMAT.format(new Date())+": "+parentTask + " 执行失败或未执行,等待"+INTERVAL/60/1000+"分钟后再次检查...\n");
				logFileWriter.flush();
				success = false;
				break;
			}
		}

		if (!success) {
			Thread.sleep(INTERVAL);
			waiteTime += INTERVAL;
			if (waiteTime > TIMEOUT) {
				logFileWriter.write(DATE_TIME_FORMAT.format(new Date())+": "+"检查超时.\n");
				logFileWriter.write(DATE_TIME_FORMAT.format(new Date())+": "+"程序运行失败,退出.\n");
				logFileWriter.flush();
				return false;
			} else {
				return this.excuteCommand();
			}
		} else {
			logFileWriter.write(DATE_TIME_FORMAT.format(new Date())+": "+"该作业的所有父作业已经运行成功,可以直接运行了.\n");
			logFileWriter.write(DATE_TIME_FORMAT.format(new Date())+": "+"运行成功.\n");
			logFileWriter.flush();
			return true;
		}
	}

	/**
	 * 获得天依赖小时的所有小时父任务
	 * 
	 * @return
	 */
	private Collection<Task> getDayDependencyHourParentTasks() {
		// 天依赖的小时必须是上一天的小时
		Date taskDate = DateUtil.clearTime(DateUtil.getYesterday(currentTask.getSettingTime())); //根据当前task的setting_time算出上一天的日期
		return taskService.getTasksByParentJob(taskDate, parentJobIds);
		/*Criteria criteria = taskService.createCriteria();
		criteria.add(Restrictions.in("jobId", parentJobIds));
		criteria.add(Restrictions.eq("taskDate", taskDate));

		return criteria.list();*/
	}

	/**
	 * 获得月依赖天的所有天父任务
	 * 
	 * @return
	 */
	private Collection<Task> getMonthDependencyDayParentTasks() {
		Calendar calendar = DateUtil.getCalendar(DateUtil.clearTime(currentTask.getSettingTime()));  //根据task的setting_time来计算
		calendar.add(Calendar.MONTH, -1);
		calendar.set(Calendar.DATE, 1);
		Date startTaskDate = calendar.getTime();   //上个月第一天

		calendar.add(Calendar.MONDAY, 1);
		calendar.add(Calendar.DATE, -1);
		Date endTaskDate = calendar.getTime();     //上个月最后一天

		return taskService.getTasksByParentJob(startTaskDate, endTaskDate, parentJobIds);
		/*Criteria criteria = taskService.createCriteria();
		criteria.add(Restrictions.in("jobId", parentJobIds));
		criteria.add(Restrictions.and(Restrictions.ge("taskDate", startTaskDate), Restrictions.le("taskDate", endTaskDate)));

		return criteria.list();*/
	}

}
