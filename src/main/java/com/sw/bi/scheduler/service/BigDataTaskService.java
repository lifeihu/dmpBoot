package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.model.BigDataTask;

public interface BigDataTaskService extends GenericService<BigDataTask> {

	/**
	 * 指定任务是否是大数据任务
	 * 
	 * @param jobId
	 * @return
	 */
	public boolean isBigDataTask(long jobId);

}
