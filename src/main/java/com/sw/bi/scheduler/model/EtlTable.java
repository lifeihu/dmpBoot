package com.sw.bi.scheduler.model;

import java.util.Date;

public class EtlTable implements java.io.Serializable {

	private Long etlTableId; //必须是表名+ID的形式.  为了使用GenericServiceHibernateSupport.delete方法.  里面的getEntityId()方法中有约定.
	private Date taskDate;
	private String tableName;
	private String programFullName;
	private Long jobId;
	private String createTable;

	public EtlTable() {}

	public Long getEtlTableId() {
		return etlTableId;
	}

	public void setEtlTableId(Long etlTableId) {
		this.etlTableId = etlTableId;
	}

	public Date getTaskDate() {
		return taskDate;
	}

	public void setTaskDate(Date taskDate) {
		this.taskDate = taskDate;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getProgramFullName() {
		return programFullName;
	}

	public void setProgramFullName(String programFullName) {
		this.programFullName = programFullName;
	}

	public Long getJobId() {
		return jobId;
	}

	public void setJobId(Long jobId) {
		this.jobId = jobId;
	}

	public String getCreateTable() {
		return createTable;
	}

	public void setCreateTable(String createTable) {
		this.createTable = createTable;
	}

}
