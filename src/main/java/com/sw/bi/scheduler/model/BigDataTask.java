package com.sw.bi.scheduler.model;

import java.util.Date;

/**
 * 大数据任务
 * 
 * @author shiming.hong
 */
public class BigDataTask {

	private long bigDataTaskId;
	private long taskId;
	private Date taskDate;
	private Date scanDate;
	private long jobId;
	private int jobType;
	private String jobName;
	private int cycleType;
	private long jobLevel;
	private Date settingTime;
	private Date createTime;
	private Date updateTime;

	public long getBigDataTaskId() {
		return bigDataTaskId;
	}

	public void setBigDataTaskId(long bigDataTaskId) {
		this.bigDataTaskId = bigDataTaskId;
	}

	public long getTaskId() {
		return taskId;
	}

	public void setTaskId(long taskId) {
		this.taskId = taskId;
	}

	public Date getTaskDate() {
		return taskDate;
	}

	public void setTaskDate(Date taskDate) {
		this.taskDate = taskDate;
	}

	public Date getScanDate() {
		return scanDate;
	}

	public void setScanDate(Date scanDate) {
		this.scanDate = scanDate;
	}

	public long getJobId() {
		return jobId;
	}

	public void setJobId(long jobId) {
		this.jobId = jobId;
	}

	public int getJobType() {
		return jobType;
	}

	public void setJobType(int jobType) {
		this.jobType = jobType;
	}

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public int getCycleType() {
		return cycleType;
	}

	public void setCycleType(int cycleType) {
		this.cycleType = cycleType;
	}

	public long getJobLevel() {
		return jobLevel;
	}

	public void setJobLevel(long jobLevel) {
		this.jobLevel = jobLevel;
	}

	public Date getSettingTime() {
		return settingTime;
	}

	public void setSettingTime(Date settingTime) {
		this.settingTime = settingTime;
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
