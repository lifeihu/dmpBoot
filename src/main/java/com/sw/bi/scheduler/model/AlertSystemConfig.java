package com.sw.bi.scheduler.model;

import java.util.Date;

/**
 * AlertSystemConfig entity. @author MyEclipse Persistence Tools
 */

public class AlertSystemConfig implements java.io.Serializable {

	// Fields

	private long alertSystemConfigId;
	private String beginWorkTime;
	private String endWorkTime;
	private Long dutyMan;
	private String alertJobRange;
	private Integer alertWay;
	private Integer alertRate;
	private Integer alertTarget;
	private Integer alertPolicy;
	private Integer smsContent;
	private String observeMan;
	private String alertMaillist;
	private Date precomputeForWhichdate;
	private Integer monitorBeginTime;
	private Integer monitorEndTime;
	private String scanCycle;

	private long userGroupId;

	public long getUserGroupId() {
		return userGroupId;
	}

	public void setUserGroupId(long userGroupId) {
		this.userGroupId = userGroupId;
	}

	public long getAlertSystemConfigId() {
		return alertSystemConfigId;
	}

	public void setAlertSystemConfigId(long alertSystemConfigId) {
		this.alertSystemConfigId = alertSystemConfigId;
	}

	public String getBeginWorkTime() {
		return beginWorkTime;
	}

	public void setBeginWorkTime(String beginWorkTime) {
		this.beginWorkTime = beginWorkTime;
	}

	public String getEndWorkTime() {
		return endWorkTime;
	}

	public void setEndWorkTime(String endWorkTime) {
		this.endWorkTime = endWorkTime;
	}

	public Long getDutyMan() {
		return dutyMan;
	}

	public void setDutyMan(Long dutyMan) {
		this.dutyMan = dutyMan;
	}

	public String getAlertJobRange() {
		return alertJobRange;
	}

	public void setAlertJobRange(String alertJobRange) {
		this.alertJobRange = alertJobRange;
	}

	public Integer getAlertWay() {
		return alertWay;
	}

	public void setAlertWay(Integer alertWay) {
		this.alertWay = alertWay;
	}

	public Integer getAlertRate() {
		return alertRate;
	}

	public void setAlertRate(Integer alertRate) {
		this.alertRate = alertRate;
	}

	public Integer getAlertTarget() {
		return alertTarget;
	}

	public void setAlertTarget(Integer alertTarget) {
		this.alertTarget = alertTarget;
	}

	public Integer getAlertPolicy() {
		return alertPolicy;
	}

	public void setAlertPolicy(Integer alertPolicy) {
		this.alertPolicy = alertPolicy;
	}

	public Integer getSmsContent() {
		return smsContent;
	}

	public void setSmsContent(Integer smsContent) {
		this.smsContent = smsContent;
	}

	public String getObserveMan() {
		return observeMan;
	}

	public void setObserveMan(String observeMan) {
		this.observeMan = observeMan;
	}

	public String getAlertMaillist() {
		return alertMaillist;
	}

	public void setAlertMaillist(String alertMaillist) {
		this.alertMaillist = alertMaillist;
	}

	public Date getPrecomputeForWhichdate() {
		return precomputeForWhichdate;
	}

	public void setPrecomputeForWhichdate(Date precomputeForWhichdate) {
		this.precomputeForWhichdate = precomputeForWhichdate;
	}

	public Integer getMonitorBeginTime() {
		return monitorBeginTime;
	}

	public void setMonitorBeginTime(Integer monitorBeginTime) {
		this.monitorBeginTime = monitorBeginTime;
	}

	public Integer getMonitorEndTime() {
		return monitorEndTime;
	}

	public void setMonitorEndTime(Integer monitorEndTime) {
		this.monitorEndTime = monitorEndTime;
	}

	public String getScanCycle() {
		return scanCycle;
	}

	public void setScanCycle(String scanCycle) {
		this.scanCycle = scanCycle;
	}

}