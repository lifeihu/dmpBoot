package com.sw.bi.scheduler.service;

import java.util.Map;

public interface WeightService {

	/**
	 * 获得指定作业的参考点或已触发任务
	 * 
	 * @param jobIds
	 * @return
	 */
	public Map<String, Object> getReferenceOrTriggeredTasks(Long[] jobIds);

	/**
	 * 给指定任务加权,使其能获得更高的执行优先级(直接对Flag2标记为4)
	 * 
	 * @param taskId
	 */
	public void weighting(long taskId);

	/**
	 * 对参考点表中的任务进行加权操作
	 * 
	 * @param waitUpdateStatusTaskId
	 * @param jobId
	 */
	public void weightingReference(long waitUpdateStatusTaskId, Long jobId);

	/**
	 * 对指定任务的flag2进行加权操作
	 * 
	 * @param taskId
	 */
	public void weightingTaskFlag2(long taskId);

	/**
	 * 对指定任务的readyTime进行加权操作
	 * 
	 * @param taskId
	 */
	public void weightingTaskReadyTime(long taskId);
}
