package com.sw.bi.scheduler.model;

// Generated 2012-1-4 12:55:27 by Hibernate Tools 3.4.0.CR1

import java.util.Date;

/**
 * 该表的信息被合并至Gateway表
 */
@Deprecated
public class ScheduleSystemStatus implements java.io.Serializable {

	private Long scheduleSystemStatusId;
	private int status;
	private int referToJobLevel;
	private int needBalance;
	private int taskRunningPriority;
	private int referPointRandom;
	private int taskFailReturnTimes;
	private String gateway;
	private int taskRunningMax = 20;
	private int waitUpdateStatusTaskCount = 50;
	private String taskCountExceptJobs;
	private Date createTime;
	private Date updateTime;

	public ScheduleSystemStatus() {}

	public ScheduleSystemStatus(String gateway, int status, int referToJobLevel, int needBalance, int taskRunningPriority, int referPointRandom, Date createTime) {
		this.gateway = gateway;
		this.status = status;
		this.referToJobLevel = referToJobLevel;
		this.needBalance = needBalance;
		this.taskRunningPriority = taskRunningPriority;
		this.referPointRandom = referPointRandom;
		this.createTime = createTime;
	}

	public ScheduleSystemStatus(String gateway, int status, int referToJobLevel, int needBalance, int taskRunningPriority, int referPointRandom, Date createTime, Date updateTime) {
		this.gateway = gateway;
		this.status = status;
		this.referToJobLevel = referToJobLevel;
		this.needBalance = needBalance;
		this.taskRunningPriority = taskRunningPriority;
		this.referPointRandom = referPointRandom;
		this.createTime = createTime;
		this.updateTime = updateTime;
	}

	public Long getScheduleSystemStatusId() {
		return this.scheduleSystemStatusId;
	}

	public void setScheduleSystemStatusId(Long scheduleSystemStatusId) {
		this.scheduleSystemStatusId = scheduleSystemStatusId;
	}

	public int getStatus() {
		return this.status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public int getReferToJobLevel() {
		return this.referToJobLevel;
	}

	public void setReferToJobLevel(int referToJobLevel) {
		this.referToJobLevel = referToJobLevel;
	}

	public int getNeedBalance() {
		return this.needBalance;
	}

	public void setNeedBalance(int needBalance) {
		this.needBalance = needBalance;
	}

	public int getTaskRunningPriority() {
		return taskRunningPriority;
	}

	public void setTaskRunningPriority(int taskRunningPriority) {
		this.taskRunningPriority = taskRunningPriority;
	}

	public int getReferPointRandom() {
		return referPointRandom;
	}

	public void setReferPointRandom(int referPointRandom) {
		this.referPointRandom = referPointRandom;
	}

	public int getTaskFailReturnTimes() {
		return taskFailReturnTimes;
	}

	public void setTaskFailReturnTimes(int taskFailReturnTimes) {
		this.taskFailReturnTimes = taskFailReturnTimes;
	}

	public String getGateway() {
		return gateway;
	}

	public void setGateway(String gateway) {
		this.gateway = gateway;
	}

	public int getTaskRunningMax() {
		return taskRunningMax;
	}

	public void setTaskRunningMax(int taskRunningMax) {
		this.taskRunningMax = taskRunningMax;
	}

	public int getWaitUpdateStatusTaskCount() {
		return waitUpdateStatusTaskCount;
	}

	public void setWaitUpdateStatusTaskCount(int waitUpdateStatusTaskCount) {
		this.waitUpdateStatusTaskCount = waitUpdateStatusTaskCount;
	}

	public String getTaskCountExceptJobs() {
		return taskCountExceptJobs;
	}

	public void setTaskCountExceptJobs(String taskCountExceptJobs) {
		this.taskCountExceptJobs = taskCountExceptJobs;
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
