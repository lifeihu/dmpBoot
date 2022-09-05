package com.sw.bi.scheduler.model;

import java.util.Date;

public class EtlCleanConfig {

	private Long etlCleanConfigId;
	private String tableName;
	private String partitionName;
	private String partitionType;
	private long applyMan;
	private int keepDays;
	private Date createTime;
	private Date updateTime;

	public Long getEtlCleanConfigId() {
		return etlCleanConfigId;
	}

	public void setEtlCleanConfigId(Long etlCleanConfigId) {
		this.etlCleanConfigId = etlCleanConfigId;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getPartitionName() {
		return partitionName;
	}

	public void setPartitionName(String partitionName) {
		this.partitionName = partitionName;
	}

	public String getPartitionType() {
		return partitionType;
	}

	public void setPartitionType(String partitionType) {
		this.partitionType = partitionType;
	}

	public long getApplyMan() {
		return applyMan;
	}

	public void setApplyMan(long applyMan) {
		this.applyMan = applyMan;
	}

	public int getKeepDays() {
		return keepDays;
	}

	public void setKeepDays(int keepDays) {
		this.keepDays = keepDays;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

}
