package com.sw.bi.scheduler.model;

import java.sql.Timestamp;
import java.util.Date;

/**
 * JobDelayStandard entity. @author MyEclipse Persistence Tools
 */

public class JobDelayStandard implements java.io.Serializable {

	// Fields

	private Long id;
	private Date taskDate;
	private Long jobId;
	private Long yesterdayRunTime;
	private Long recentSevenRunTime;
	private Long recentSevenRunTimeMovemax;
	private Long standardRunTime;
	private Timestamp createTime;
	private Timestamp updateTime;

	// Constructors

	/** default constructor */
	public JobDelayStandard() {
	}

	/** minimal constructor */
	public JobDelayStandard(Date taskDate, Long jobId, Long yesterdayRunTime,
			Long recentSevenRunTime, Long recentSevenRunTimeMovemax,
			Long standardRunTime, Timestamp createTime) {
		this.taskDate = taskDate;
		this.jobId = jobId;
		this.yesterdayRunTime = yesterdayRunTime;
		this.recentSevenRunTime = recentSevenRunTime;
		this.recentSevenRunTimeMovemax = recentSevenRunTimeMovemax;
		this.standardRunTime = standardRunTime;
		this.createTime = createTime;
	}

	/** full constructor */
	public JobDelayStandard(Date taskDate, Long jobId, Long yesterdayRunTime,
			Long recentSevenRunTime, Long recentSevenRunTimeMovemax,
			Long standardRunTime, Timestamp createTime, Timestamp updateTime) {
		this.taskDate = taskDate;
		this.jobId = jobId;
		this.yesterdayRunTime = yesterdayRunTime;
		this.recentSevenRunTime = recentSevenRunTime;
		this.recentSevenRunTimeMovemax = recentSevenRunTimeMovemax;
		this.standardRunTime = standardRunTime;
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

	public Date getTaskDate() {
		return this.taskDate;
	}

	public void setTaskDate(Date taskDate) {
		this.taskDate = taskDate;
	}

	public Long getJobId() {
		return this.jobId;
	}

	public void setJobId(Long jobId) {
		this.jobId = jobId;
	}

	public Long getYesterdayRunTime() {
		return this.yesterdayRunTime;
	}

	public void setYesterdayRunTime(Long yesterdayRunTime) {
		this.yesterdayRunTime = yesterdayRunTime;
	}

	public Long getRecentSevenRunTime() {
		return this.recentSevenRunTime;
	}

	public void setRecentSevenRunTime(Long recentSevenRunTime) {
		this.recentSevenRunTime = recentSevenRunTime;
	}

	public Long getRecentSevenRunTimeMovemax() {
		return this.recentSevenRunTimeMovemax;
	}

	public void setRecentSevenRunTimeMovemax(Long recentSevenRunTimeMovemax) {
		this.recentSevenRunTimeMovemax = recentSevenRunTimeMovemax;
	}

	public Long getStandardRunTime() {
		return this.standardRunTime;
	}

	public void setStandardRunTime(Long standardRunTime) {
		this.standardRunTime = standardRunTime;
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