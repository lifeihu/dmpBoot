package com.sw.bi.scheduler.model;

/**
 * 需要记入日志的对象接口
 * 
 * @author Administrator
 * 
 */
public interface LoggerEntity {

	/**
	 * 记入日志的对象标识
	 * 
	 * @return
	 */
	public String getEntityName();

	/**
	 * 记入日志的记录标识
	 * 
	 * @return
	 */
	public String getLoggerName();

}
