package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.background.exception.SchedulerException;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.model.WaitUpdateStatusTask;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface WaitUpdateStatusTaskService extends GenericService<WaitUpdateStatusTask> {

	/**
	 * 从参考点表中获取本次调度要用来参考的参考点的集合
	 * 
	 * @param scanDate
	 * @return
	 * @throws SchedulerException
	 */
	public List<WaitUpdateStatusTask> getWaitUpdateStatusTasks(Date date) throws SchedulerException;

	/**
	 * 获得指定任务ID的参考点
	 * 
	 * @param taskIds
	 * @return
	 */
	public Collection<WaitUpdateStatusTask> getWaitUpdateStatusTasks(Collection<Long> taskIds);

	/**
	 * 将指定任务日期的根任务放入参考点表
	 * 
	 * @param taskDate
	 * @throws SchedulerException
	 */
	public WaitUpdateStatusTask addRootTask(Date taskDate) throws SchedulerException;

	/**
	 * 将指定任务的所有父任务加入参考点表
	 * 
	 * @param task
	 * @param scanDate
	 */
	public void addParentTasks(Task task, Date scanDate);

	/**
	 * 将指定任务的所有父任务加入参考点表
	 * 
	 * @param task
	 * @param scanDate
	 */
	public void addParentTasks(List<Task> tasks, Date scanDate);

	/**
	 * 创建参考点
	 * 
	 * @param scanDate
	 *            参考点扫描日期
	 * @param task
	 * @return
	 */
	public WaitUpdateStatusTask create(Date scanDate, Task task);

	/**
	 * 创建参考点
	 * 
	 * @param scanDate
	 *            参考点扫描日期
	 * @param task
	 * @param isSerial
	 *            是否串行参考点
	 * @return
	 */
	public WaitUpdateStatusTask create(Date scanDate, Task task, boolean isSerial);

	/**
	 * 创建参考点
	 * 
	 * @param scanDate
	 * @param task
	 * @param flag
	 * @param flag2
	 * @return
	 */
	// public WaitUpdateStatusTask create(Date scanDate, Task task, long flag, Integer flag2);

	/**
	 * 获得指定范围的随机数
	 * 
	 * @param start
	 * @param end
	 * @return
	 */
	public Integer[] getRangeRandom(int total, int end);

	/**
	 * 获得指定日期的所有参考点
	 * 
	 * @param taskDate
	 * @return
	 */
	public List<WaitUpdateStatusTask> getWaitUpdateStatusTasksByDate(Date taskDate);

	/**
	 * 校验指定参考点是否允许被删除
	 * 
	 * @param waitUpdateStatusTaskId
	 * @return
	 */
	public boolean isAllowRemove(long waitUpdateStatusTaskId);

	/**
	 * 删除指定任务日期下的所有参考点
	 * 
	 * @param taskDate
	 */
	public void remove(Date taskDate);

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
	 * 获得所有有效的参考点
	 * 
	 * @return
	 */
	public Collection<WaitUpdateStatusTask> getActiveWaitUpdateStatusTasks();

}
