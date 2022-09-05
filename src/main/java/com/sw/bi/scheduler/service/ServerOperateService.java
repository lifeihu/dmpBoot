package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.model.ServerOperate;

import java.util.Map;

public interface ServerOperateService extends GenericService<ServerOperate> {

	/**
	 * 服务器操作上线
	 * 
	 * @param serverOperateId
	 */
	public void online(long serverOperateId);

	/**
	 * 服务器操作下线
	 * 
	 * @param serverOperateId
	 */
	public void offline(long serverOperateId);

	/**
	 * 执行服务器操作
	 * 
	 * @param serverOperateId
	 */
	public Map<String, String> execute(long serverOperateId);

}
