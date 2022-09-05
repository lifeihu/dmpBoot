package com.sw.bi.scheduler.model;

import java.util.Date;

public class EtlTableRelation implements java.io.Serializable{
	
	private long etlTableRelationId;
	private Date taskDate;
	private long tableId;
	private long parentTableId;

	
	public EtlTableRelation(){}
	
	public long getEtlTableRelationId() {
		return etlTableRelationId;
	}
	public void setEtlTableRelationId(long etlTableRelationId) {
		this.etlTableRelationId = etlTableRelationId;
	}
	public Date getTaskDate() {
		return taskDate;
	}
	public void setTaskDate(Date taskDate) {
		this.taskDate = taskDate;
	}
	public long getTableId() {
		return tableId;
	}
	public void setTableId(long tableId) {
		this.tableId = tableId;
	}
	public long getParentTableId() {
		return parentTableId;
	}

	public void setParentTableId(long parentTableId) {
		this.parentTableId = parentTableId;
	}

	
}
