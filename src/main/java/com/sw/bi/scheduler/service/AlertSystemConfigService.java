package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.model.AlertSystemConfig;

public interface AlertSystemConfigService extends GenericService<AlertSystemConfig> {

	/**
	 * 告警
	 * 
	 * @param title
	 * @param message
	 */
	public void alert(long jobId, String title, String message);

	/**
	 * 告警
	 * 
	 * @param jobId
	 * @param title
	 * @param emailMessage
	 * @param smsMessage
	 */
	public void alert(long jobId, String title, String emailMessage, String smsMessage);

	/**
	 * 给指定用户组创建一个告警配置
	 * 
	 * @param userGroupId
	 * @return
	 */
	public AlertSystemConfig createDefault(long userGroupId);

	/**
	 * 删除指定用户组的告警配置
	 * 
	 * @param userGroupId
	 */
	public void delteByUserGroup(long userGroupId);

}
