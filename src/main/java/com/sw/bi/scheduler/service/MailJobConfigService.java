package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.model.MailJobConfig;

public interface MailJobConfigService extends GenericService<MailJobConfig> {

	/**
	 * 获得指定作业对应的邮件发送配置
	 * 
	 * @param jobId
	 * @return
	 */
	public MailJobConfig getByJobId(Long jobId);

}
