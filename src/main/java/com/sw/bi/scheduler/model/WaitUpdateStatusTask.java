package com.sw.bi.scheduler.model;

// Generated 2012-1-9 10:47:01 by Hibernate Tools 3.4.0.CR1

import com.sw.bi.scheduler.util.DateUtil;

import java.util.Collection;
import java.util.Date;

/**
 * WaitUpdateStatusTask generated by hbm2java
 */
public class WaitUpdateStatusTask implements java.io.Serializable {

	private Long waitUpdateStatusTaskId;
	private Date scanDate;
	private Date taskDate;
	private long taskId;
	private int flag;
	private Long jobId;
	private String taskName;
	private Date settingTime;
	private boolean active = true;
	private Date createTime;
	private Date scanTime;

	/**
	 * 参考点被扫描过的次数
	 */
	private int scanTimes;

	/**
	 * 是否串行参考点
	 */
	private boolean serial = false;

	private Collection<Job> childrenJobs;

	public WaitUpdateStatusTask() {}

	public WaitUpdateStatusTask(Date scanDate, Date taskDate, long taskId) {
		this.scanDate = scanDate;
		this.taskDate = taskDate;
		this.taskId = taskId;
	}

	public Long getWaitUpdateStatusTaskId() {
		return this.waitUpdateStatusTaskId;
	}

	public void setWaitUpdateStatusTaskId(Long waitUpdateStatusTaskId) {
		this.waitUpdateStatusTaskId = waitUpdateStatusTaskId;
	}

	public Date getScanDate() {
		return this.scanDate;
	}

	public void setScanDate(Date scanDate) {
		this.scanDate = scanDate;
	}

	public Date getTaskDate() {
		return taskDate;
	}

	public void setTaskDate(Date taskDate) {
		this.taskDate = taskDate;
	}

	public long getTaskId() {
		return this.taskId;
	}

	public void setTaskId(long taskId) {
		this.taskId = taskId;
	}

	public int getFlag() {
		return flag;
	}

	public void setFlag(int flag) {
		this.flag = flag;
	}

	public Long getJobId() {
		return jobId;
	}

	public void setJobId(Long jobId) {
		this.jobId = jobId;
	}

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public Date getSettingTime() {
		return settingTime;
	}

	public void setSettingTime(Date settingTime) {
		this.settingTime = settingTime;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Date getScanTime() {
		return scanTime;
	}

	public void setScanTime(Date scanTime) {
		this.scanTime = scanTime;
	}

	public int getScanTimes() {
		return scanTimes;
	}

	public void setScanTimes(int scanTimes) {
		this.scanTimes = scanTimes;
	}

	public boolean isSerial() {
		return serial;
	}

	public void setSerial(boolean serial) {
		this.serial = serial;
	}

	public Collection<Job> getChildrenJobs() {
		return childrenJobs;
	}

	public void setChildrenJobs(Collection<Job> childrenJobs) {
		this.childrenJobs = childrenJobs;
	}

	@Override
	public String toString() {
		StringBuilder content = new StringBuilder("参考点[");
		content.append(this.getJobId()).append(" - ");
		content.append(this.getTaskName()).append(" ");
		content.append("[");
		content.append("任务日期:" + DateUtil.formatDate(this.getTaskDate()));
		content.append(",串行:").append(this.isSerial() ? "是" : "否");
		content.append("]]");

		return content.toString();
	}

}
