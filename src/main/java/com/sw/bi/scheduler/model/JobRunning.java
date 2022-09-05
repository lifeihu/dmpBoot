package com.sw.bi.scheduler.model;

import java.sql.Timestamp;

/**
 * JobRunning entity. @author MyEclipse Persistence Tools
 */

public class JobRunning implements java.io.Serializable {

	// Fields

	private Long id;
	private Long runningCount;
	private Timestamp createTime;
	private Timestamp updateTime;

	// Constructors

	/** default constructor */
	public JobRunning() {
	}

	/** minimal constructor */
	public JobRunning(Long runningCount, Timestamp createTime) {
		this.runningCount = runningCount;
		this.createTime = createTime;
	}

	/** full constructor */
	public JobRunning(Long runningCount, Timestamp createTime,
			Timestamp updateTime) {
		this.runningCount = runningCount;
		this.createTime = createTime;
		this.updateTime = updateTime;
	}

	// Property accessors

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getRunningCount() {
		return this.runningCount;
	}

	public void setRunningCount(Long runningCount) {
		this.runningCount = runningCount;
	}

	public Timestamp getCreateTime() {
		return this.createTime;
	}

	public void setCreateTime(Timestamp createTime) {
		this.createTime = createTime;
	}

	public Timestamp getUpdateTime() {
		return this.updateTime;
	}

	public void setUpdateTime(Timestamp updateTime) {
		this.updateTime = updateTime;
	}

}