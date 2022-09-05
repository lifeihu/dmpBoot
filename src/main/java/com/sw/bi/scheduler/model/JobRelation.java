package com.sw.bi.scheduler.model;

// Generated 2011-12-19 14:42:39 by Hibernate Tools 3.4.0.CR1

import java.util.Date;

/**
 * JobRelation generated by hbm2java
 */
public class JobRelation implements java.io.Serializable {

	private Long jobRelationId;
	private long jobId;
	private Long parentId;
	private Date createTime;
	private Date updateTime;

	public JobRelation() {}

	public JobRelation(long jobId, Date createTime) {
		this.jobId = jobId;
		this.createTime = createTime;
	}

	public JobRelation(long jobId, Long parentId, Date createTime, Date updateTime) {
		this.jobId = jobId;
		this.parentId = parentId;
		this.createTime = createTime;
		this.updateTime = updateTime;
	}

	public Long getJobRelationId() {
		return this.jobRelationId;
	}

	public void setJobRelationId(Long jobRelationId) {
		this.jobRelationId = jobRelationId;
	}

	public long getJobId() {
		return this.jobId;
	}

	public void setJobId(long jobId) {
		this.jobId = jobId;
	}

	public Long getParentId() {
		return this.parentId;
	}

	public void setParentId(Long parentId) {
		this.parentId = parentId;
	}

	public Date getCreateTime() {
		return this.createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Date getUpdateTime() {
		return this.updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

}