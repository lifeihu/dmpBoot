package com.sw.bi.scheduler.background.service;

import java.util.Collection;
import java.util.Date;

import com.sw.bi.scheduler.background.exception.GatewayNotFoundException;
import com.sw.bi.scheduler.background.exception.SchedulerException;
import com.sw.bi.scheduler.model.Task;

public interface SchedulerService {

	/**
	 * 参考点方式调度
	 * 
	 * @throws SchedulerException
	 */
	public void schedule(Date date) throws SchedulerException;

	/**
	 * 模拟方式调度
	 * 
	 * @throws SchedulerException
	 */
	public void simulateSchedule() throws SchedulerException;

	/**
	 * 调度流程之未触发状态更新
	 * 
	 * @throws SchedulerException
	 */
	@Deprecated
	public void scheduleUpdateWaitTrigger() throws SchedulerException;

	/**
	 * 自动创建指定日期内的任务
	 * 
	 * @param date
	 * @throws SchedulerException
	 */
	public void createTasks(Date date) throws SchedulerException;

	/**
	 * 对指定日期内所有满足条件任务更改为“未触发”状态
	 * 
	 * @param taskDate
	 * @throws SchedulerException
	 */
	public void updateWaitTrigger(Date taskDate) throws SchedulerException;

	/**
	 * 修改指定任务的状态为未触发
	 * 
	 * @param task
	 */
	public void updateWaitTrigger(Task task);

	/**
	 * 对指定日期内所有满足条件任务更改为“已触发”状态
	 * 
	 * @param today
	 */
	public void updateTriggered(Date today);

	/**
	 * 修改指定任务的状态更改为“已触发”状态
	 * 
	 * @param task
	 */
	public void updateTriggered(Task task);

	/**
	 * 执行指定日期内满足条件的所有任务
	 * 
	 * @param date
	 * @return
	 * @throws GatewayNotFoundException
	 */
	public int execute(Date date) throws GatewayNotFoundException;

	/**
	 * 执行指定的所有已触发任务
	 * 
	 * @param triggeredTasks
	 * @param ignoreGatewayQuota
	 * @return
	 * @throws GatewayNotFoundException
	 */
	public int execute(Collection<Task> triggeredTasks, boolean ignoreGatewayQuota) throws GatewayNotFoundException;

	/**
	 * FIXME 因为测试才公开该接口，该接口应该是private
	 * 
	 * @param gateway
	 * @param today
	 * @return
	 * @throws GatewayNotFoundException
	 */
	// public Collection<Task> selectTasksByCluster(String gateway, Date today) throws GatewayNotFoundException;

}
