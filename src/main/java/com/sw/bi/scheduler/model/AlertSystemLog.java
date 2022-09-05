package com.sw.bi.scheduler.model;

import com.sw.bi.scheduler.util.Configure.JobCycle;
import com.sw.bi.scheduler.util.DateUtil;

import java.io.Serializable;
import java.util.Date;

public class AlertSystemLog implements Serializable {
	private long alertSystemLogId;
	private Date alertDate;
	private long jobId;
	private long taskId;
	private Date taskDate;
	private String jobName;
	private int jobStatus;
	private Long jobType;
	private int cycleType;
	private Date settingTime;
	private Long lastActionId;
	private String dutyOfficer;
	private Integer alertType;
	private Integer alertPolicy;
	private Integer alertWay;
	private Date alertTime;
	private Integer alertStatus;
	private String receiver;
	private Date createTime;
	private Date updateTime;

	private Integer alertCount;
	private UserGroup userGroup;

	public long getAlertSystemLogId() {
		return alertSystemLogId;
	}

	public void setAlertSystemLogId(long alertSystemLogId) {
		this.alertSystemLogId = alertSystemLogId;
	}

	public Date getAlertDate() {
		return alertDate;
	}

	public void setAlertDate(Date alertDate) {
		this.alertDate = alertDate;
	}

	public long getJobId() {
		return jobId;
	}

	public void setJobId(long jobId) {
		this.jobId = jobId;
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

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public int getJobStatus() {
		return jobStatus;
	}

	public void setJobStatus(int jobStatus) {
		this.jobStatus = jobStatus;
	}

	public Long getJobType() {
		return jobType;
	}

	public void setJobType(Long jobType) {
		this.jobType = jobType;
	}

	public int getCycleType() {
		return cycleType;
	}

	public void setCycleType(int cycleType) {
		this.cycleType = cycleType;
	}

	public Date getSettingTime() {
		return settingTime;
	}

	public void setSettingTime(Date settingTime) {
		this.settingTime = settingTime;
	}

	public Long getLastActionId() {
		return lastActionId;
	}

	public void setLastActionId(Long lastActionId) {
		this.lastActionId = lastActionId;
	}

	public String getDutyOfficer() {
		return dutyOfficer;
	}

	public void setDutyOfficer(String dutyOfficer) {
		this.dutyOfficer = dutyOfficer;
	}

	public Integer getAlertType() {
		return alertType;
	}

	public void setAlertType(Integer alertType) {
		this.alertType = alertType;
	}

	public Integer getAlertPolicy() {
		return alertPolicy;
	}

	public void setAlertPolicy(Integer alertPolicy) {
		this.alertPolicy = alertPolicy;
	}

	public Integer getAlertWay() {
		return alertWay;
	}

	public void setAlertWay(Integer alertWay) {
		this.alertWay = alertWay;
	}

	public Date getAlertTime() {
		return alertTime;
	}

	public void setAlertTime(Date alertTime) {
		this.alertTime = alertTime;
	}

	public Integer getAlertStatus() {
		return alertStatus;
	}

	public void setAlertStatus(Integer alertStatus) {
		this.alertStatus = alertStatus;
	}

	public String getReceiver() {
		return receiver;
	}

	public void setReceiver(String receiver) {
		this.receiver = receiver;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Integer getAlertCount() {
		return alertCount;
	}

	public void setAlertCount(Integer alertCount) {
		this.alertCount = alertCount;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public UserGroup getUserGroup() {
		return userGroup;
	}

	public void setUserGroup(UserGroup userGroup) {
		this.userGroup = userGroup;
	}

	public String getName() {
		int cycleType = this.getCycleType();
		String jobName = this.getJobName();

		if (JobCycle.HOUR.indexOf() == cycleType || JobCycle.MINUTE.indexOf() == cycleType) {
			Date settingTime = this.getSettingTime();
			return jobName + "(" + DateUtil.format(settingTime, "HH:mm") + ")";
		}

		return jobName;
	}
}
