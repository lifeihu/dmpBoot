package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.TaskCreateLog;
import com.sw.bi.scheduler.service.TaskCreateLogService;
import com.sw.bi.scheduler.util.Configure;
import com.sw.bi.scheduler.util.DateUtil;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Date;

@Service("taskCreateLogService")
@SuppressWarnings("unchecked")
public class TaskCreateLogServiceImpl extends GenericServiceHibernateSupport<TaskCreateLog> implements TaskCreateLogService {

	@Override
	public void createComplete(Date taskDate) {
		if (taskDate == null)
			return;

		TaskCreateLog tcl = getTaskCreateLog(taskDate);
		if (tcl == null) {
			return;
		}
		tcl.setCreateSuccess(true);
		tcl.setUpdateTime(DateUtil.now());
		save(tcl);
	}

	@Override
	public void runComplete(Date taskDate) {
		if (taskDate == null)
			return;

		TaskCreateLog tcl = getTaskCreateLog(taskDate);
		if (tcl == null) {
			return;
		}
		tcl.setRunSuccess(true);
		tcl.setUpdateTime(DateUtil.now());
		save(tcl);
	}

	@Override
	public boolean isTaskCreated(Date taskDate) {
		TaskCreateLog tcl = getTaskCreateLog(taskDate);
		if (tcl == null) {
			return false;
		}
		return tcl.getCreateSuccess();
	}

	@Override
	public boolean isTaskRuned(Date taskDate) {
		TaskCreateLog tcl = getTaskCreateLog(taskDate);
		if (tcl == null) {
			return false;
		}
		return tcl.getRunSuccess();
	}

	@Override
	public Collection<Date> getNotRunCompleteDatesExcludeToday(Date today) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("runSuccess", false));
		criteria.add(Restrictions.not(Restrictions.eq("taskDate", today)));
		criteria.setProjection(Projections.property("taskDate"));
		criteria.addOrder(Order.asc("taskDate"));

		return criteria.list();
	}

	@Override
	public Collection<TaskCreateLog> getTaskCreateLogs(Date startTaskDate, Date endTaskDate) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.ge("taskDate", startTaskDate));
		criteria.add(Restrictions.le("taskDate", endTaskDate));
		criteria.addOrder(Order.desc("taskDate"));

		return criteria.list();
	}

	@Override
	public Date getLatestCreateCompleteTaskDate() {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("createSuccess", true));
		criteria.setProjection(Projections.max("taskDate"));

		return (Date) criteria.uniqueResult();
	}

	/**
	 * 获得指定任务日期的任务日志(如果没有日志则会生成一个新并返回)
	 * 
	 * @param taskDate
	 * @return
	 */
	private TaskCreateLog getTaskCreateLog(Date taskDate) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("taskDate", taskDate));
		TaskCreateLog tcl = (TaskCreateLog) criteria.uniqueResult();
		if (tcl == null) {
			boolean isMainScheduler = Configure.property(Configure.MAIN_SCHEDULER, Boolean.class);
			if (isMainScheduler) {
				tcl = new TaskCreateLog(taskDate, false, false, DateUtil.now(), null);
				save(tcl);
			}
		}
		return tcl;
	}

}
