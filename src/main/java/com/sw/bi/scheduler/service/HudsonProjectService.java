package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.model.HudsonProject;

public interface HudsonProjectService extends GenericService<HudsonProject> {

	/**
	 * 发布指定Hudson项目
	 * 
	 * @param hudsonProjectId
	 */
	public void publish(long hudsonProjectId);

	/**
	 * 获得指定发布日志
	 * 
	 * @param logFile
	 * @return
	 */
	public String getPublishLog(String logFile);

}
