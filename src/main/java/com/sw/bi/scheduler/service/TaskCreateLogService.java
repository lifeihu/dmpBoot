package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.model.TaskCreateLog;

import java.util.Collection;
import java.util.Date;

public interface TaskCreateLogService extends GenericService<TaskCreateLog> {

	/**
	 * 指定任务日期内的所有任务已经创建完毕
	 * 
	 * @param taskDate
	 */
	public void createComplete(Date taskDate);

	/**
	 * 指定任务日期内的所有任务已经运行完毕
	 * 
	 * @param taskDate
	 */
	public void runComplete(Date taskDate);

	/**
	 * 校验指定日期的任务是否已经创建
	 * 
	 * @return
	 */
	public boolean isTaskCreated(Date taskDate);

	/**
	 * 判断指定日期的任务是否已经全部运行完毕
	 * 
	 * @param taskDate
	 * @return
	 */
	public boolean isTaskRuned(Date taskDate);

	/**
	 * 获得除今天外所有任务未运行完成的日期
	 * 
	 * @return
	 */
	public Collection<Date> getNotRunCompleteDatesExcludeToday(Date today);

	/**
	 * 获得指定任务日期范围内所有运行日志
	 * 
	 * @param startTaskDate
	 * @param endTaskDate
	 * @return
	 */
	public Collection<TaskCreateLog> getTaskCreateLogs(Date startTaskDate, Date endTaskDate);

	/**
	 * 获得最后一次任务创建完毕的任务日期
	 * 
	 * @return
	 */
	public Date getLatestCreateCompleteTaskDate();
}
