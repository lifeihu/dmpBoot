package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.model.Task;

import java.util.Collection;
import java.util.Date;

public interface TaskCheckService {

	/**
	 * 获得指定间隔时长内任务被运行却Action始终未创建的任务
	 * 
	 * @param interval
	 * @return
	 */
	public Collection<Task> getNotFoundActionTasks(int interval);

	/**
	 * 统计指定作业等级中未运行成功的任务数量
	 * 
	 * @param jobLevel
	 * @param interval
	 * @return
	 */
	public int countNotRunSuccessTasksByJobLevel(long jobLevel, Integer interval);

	/**
	 * 统计昨天未运行成功的任务数量
	 * 
	 * @return
	 */
	public int countNotRunSuccessTasksByYesterday();

	/**
	 * 在参考点表中启用补数据任务
	 * 
	 * @throws Exception
	 */
	public void activeWaitUpdateStatusTask() throws Exception;

	/**
	 * 在参考点表中禁用补数据任务
	 * 
	 * @throws Exception
	 */
	public void unactiveWaitUpdateStatusTask() throws Exception;

	/**
	 * 统计所有网关机上正在运行的作业数量
	 * 
	 * @param beginTimeInterval
	 * @param excludeJobIds
	 * @return
	 */
	public int countTodayRunningActions(int beginTimeInterval, Long[] excludeJobIds);

	/**
	 * 获得正在运行超过指定间隔的任务
	 * 
	 * @param taskDate
	 * @param interval
	 * @return
	 */
	public Collection<Task> getStuckRunningTasks(Date taskDate, int interval);

}
