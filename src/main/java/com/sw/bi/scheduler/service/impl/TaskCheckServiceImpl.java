package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.model.WaitUpdateStatusTask;
import com.sw.bi.scheduler.service.ActionService;
import com.sw.bi.scheduler.service.TaskCheckService;
import com.sw.bi.scheduler.service.TaskService;
import com.sw.bi.scheduler.service.WaitUpdateStatusTaskService;
import com.sw.bi.scheduler.util.Configure.ActionStatus;
import com.sw.bi.scheduler.util.Configure.JobCycle;
import com.sw.bi.scheduler.util.Configure.TaskStatus;
import com.sw.bi.scheduler.util.DateUtil;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Date;

/**
 * 仅用于告警作业相关查询(javatype.check包)
 * 
 * @author shiming.hong
 */
@Service
public class TaskCheckServiceImpl implements TaskCheckService {
	private static final Logger log = Logger.getLogger(TaskCheckService.class);

	@Autowired
	private TaskService taskService;

	@Autowired
	private ActionService actionService;

	@Autowired
	private WaitUpdateStatusTaskService waitUpdateStatusTaskService;

	@Override
	public Collection<Task> getNotFoundActionTasks(int interval) {
		Criteria criteria = taskService.createCriteria();
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

		Criteria criteria = taskService.createCriteria();
		criteria.add(Restrictions.eq("taskDate", DateUtil.getToday()));
		criteria.add(Restrictions.ge("jobLevel", jobLevel));
		criteria.add(Restrictions.in("cycleType", jobCycle));
		criteria.add(Restrictions.not(Restrictions.in("taskStatus", taskStatus)));

		if (interval != null && interval.intValue() > 0) {
			criteria.add(Restrictions.sqlRestriction("adddate(setting_time, interval " + interval.intValue() + " minute) <= now()"));
		}

		return taskService.count(criteria);
	}

	@Override
	public int countNotRunSuccessTasksByYesterday() {
		Long[] taskStatus = new Long[] { (long) TaskStatus.RUN_SUCCESS.indexOf(), (long) TaskStatus.RE_RUN_SUCCESS.indexOf() };

		Criteria criteria = taskService.createCriteria();
		criteria.add(Restrictions.eq("taskDate", DateUtil.getYesterday()));
		criteria.add(Restrictions.not(Restrictions.in("taskStatus", taskStatus)));

		return taskService.count(criteria);
	}

	@Override
	public void unactiveWaitUpdateStatusTask() throws Exception {
		Date today = DateUtil.getToday();

		Criteria criteria = waitUpdateStatusTaskService.createCriteria();
		criteria.add(Restrictions.eq("scanDate", today));
		criteria.add(Restrictions.lt("taskDate", today));
		Collection<WaitUpdateStatusTask> wusts = criteria.list();

		for (WaitUpdateStatusTask wust : wusts) {
			wust.setActive(false);
			waitUpdateStatusTaskService.update(wust);

			log.info("禁用 - " + "ID: " + wust.getWaitUpdateStatusTaskId() + ", taskId: " + wust.getTaskId() + ", taskDate: " + DateUtil.formatDate(wust.getTaskDate()));
		}
	}

	@Override
	public void activeWaitUpdateStatusTask() throws Exception {
		Criteria criteria = waitUpdateStatusTaskService.createCriteria();
		criteria.add(Restrictions.eq("active", false));
		Collection<WaitUpdateStatusTask> wusts = criteria.list();

		for (WaitUpdateStatusTask wust : wusts) {
			wust.setActive(true);
			waitUpdateStatusTaskService.update(wust);

			log.info("启用 - " + "ID: " + wust.getWaitUpdateStatusTaskId() + ", taskId: " + wust.getTaskId() + ", taskDate: " + DateUtil.formatDate(wust.getTaskDate()));
		}
	}

	@Override
	public int countTodayRunningActions(int beginTimeInterval, Long[] excludeJobIds) {
		Criteria criteria = actionService.createCriteria();
		criteria.add(Restrictions.eq("scanDate", DateUtil.getToday()));
		criteria.add(Restrictions.eq("actionStatus", ActionStatus.RUNNING.indexOf()));

		if (excludeJobIds != null && excludeJobIds.length > 0) {
			criteria.add(Restrictions.not(Restrictions.in("jobId", excludeJobIds)));
		}

		if (beginTimeInterval > 0) {
			criteria.add(Restrictions.sqlRestriction("adddate(start_time, interval " + beginTimeInterval + " minute) <= now()"));
		}

		return actionService.count(criteria);
	}

	@Override
	public Collection<Task> getStuckRunningTasks(Date taskDate, int interval) {
		Criteria criteria = taskService.createCriteria();
		criteria.add(Restrictions.eq("taskDate", taskDate));
		criteria.add(Restrictions.in("taskStatus", new Long[] { (long) TaskStatus.RUNNING.indexOf(), (long) TaskStatus.RE_RUNNING.indexOf() }));
		criteria.add(Restrictions.sqlRestriction("adddate(task_begin_time, interval " + interval + " minute) < now()"));

		return criteria.list();
	}

}
