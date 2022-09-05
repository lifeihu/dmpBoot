package com.sw.bi.scheduler.service;

import java.util.Date;

public interface ToolboxService {

	/**
	 * 获得指定表的建表语句
	 * 
	 * @param dbName
	 * @param tableName
	 * @return
	 */
	public String viewCreateTable(String dbName, String tableName);

	/**
	 * 同步Hudson中指定项目
	 * 
	 * @param project
	 * @return
	 */
	public String syncHundson(String project);

	/**
	 * 查看调度系统的后台执行日志
	 * 
	 * @param gateway
	 * @param date
	 * @param tailNumber
	 * @return
	 */
	public String viewSchedulerLog(String gateway, Date date, Integer tailNumber);

	/**
	 * 恢复指定任务日期的备份文件
	 * 
	 * @param taskDate
	 * @param jobIds
	 * @throws Exception
	 */
	public String restoreBackupFile(Date taskDate, Long[] jobIds) throws Exception;

}
