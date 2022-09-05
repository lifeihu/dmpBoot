package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.model.OperateLogger;
import com.sw.bi.scheduler.util.OperateAction;

public interface OperateLoggerService extends GenericService<OperateLogger> {

	/**
	 * 操作日志
	 * 
	 * @param operateAction
	 * @param loggerEntity
	 */
	public void log(OperateAction operateAction, Object loggerEntity);

	/**
	 * 操作日志
	 * 
	 * @param operateAction
	 * @param loggerContent
	 */
	public void log(OperateAction operateAction, String loggerContent);

	/**
	 * 操作日志
	 * 
	 * @param operateAction
	 * @param loggerContent
	 */
	public void log(String operateAction, String loggerContent);

	/**
	 * 添加操作的日志
	 * 
	 * @param loggerEntity
	 */
	public void logCreate(Object loggerEntity);

	/**
	 * 修改操作的日志
	 * 
	 * @param loggerEntity
	 */
	public void logUpdate(Object loggerEntity);

	/**
	 * 删除操作的日志
	 * 
	 * @param loggerEntity
	 */
	public void logDelete(Object loggerEntity);

	/**
	 * 越权操作的日志
	 * 
	 * @param loggerContent
	 */
	public void logUnauthorized(String loggerContent);

}
