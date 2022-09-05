package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.background.exception.DatabaseException;
import com.sw.bi.scheduler.background.exception.GatewayRunningTimeoutException;
import com.sw.bi.scheduler.model.GatewayScheduler;

public interface GatewaySchedulerService extends GenericService<GatewayScheduler> {

	/**
	 * 指定网关机是否允许被执行
	 * 
	 * @param gateway
	 * @return
	 * @throws GatewayRunningTimeoutException
	 */
	public boolean isAllowExecution(String gateway) throws GatewayRunningTimeoutException;

	/**
	 * 指定网关机开始执行
	 * 
	 * @param gateway
	 */
	public void execute(String gateway);

	/**
	 * 指定网关机执行完毕
	 * 
	 * @param gateway
	 */
	public void finished(String gateway);

	/**
	 * 校验当前数据库连接是否已经超过阈值
	 * 
	 * @return
	 * @throws DatabaseException
	 */
	public boolean isExceedDatabaseMaxConnection() throws DatabaseException;

}
