package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.model.JobDatasyncConfig;

public interface JobDatasyncConfigService extends GenericService<JobDatasyncConfig> {

	/**
	 * 根据作业获得数据同步配置信息
	 * 
	 * @param jobId
	 * @return
	 */
	public JobDatasyncConfig getJobDatasyncConfigByJob(long jobId);

}
