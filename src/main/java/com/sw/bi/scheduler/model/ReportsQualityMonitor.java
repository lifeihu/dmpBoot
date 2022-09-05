package com.sw.bi.scheduler.model;

import java.sql.Timestamp;

/**
 * ReportsQualityMonitor entity. @author MyEclipse Persistence Tools
 */

public class ReportsQualityMonitor implements java.io.Serializable {

	// Fields

	private Long jobId;
	private String tablename;
	private String ptName;
	private String ptType;
	private Boolean monitorTotalNumber;
	private Double upAndDown;
	private Boolean monitorQuata;
	private String weidu;
	private Integer alertWay;
	private String quata;
	private String email;
	private String mobilePhone;
	private Timestamp createTime;
	private Timestamp updateTime;

	// Constructors

	/** default constructor */
	public ReportsQualityMonitor() {
	}

	/** minimal constructor */
	public ReportsQualityMonitor(String tablename, String ptName,
			Boolean monitorTotalNumber, Double upAndDown, Boolean monitorQuata,
			String weidu, Integer alertWay, Timestamp createTime) {
		this.tablename = tablename;
		this.ptName = ptName;
		this.monitorTotalNumber = monitorTotalNumber;
		this.upAndDown = upAndDown;
		this.monitorQuata = monitorQuata;
		this.weidu = weidu;
		this.alertWay = alertWay;
		this.createTime = createTime;
	}

	/** full constructor */
	public ReportsQualityMonitor(String tablename, String ptName,
			String ptType, Boolean monitorTotalNumber, Double upAndDown,
			Boolean monitorQuata, String weidu, Integer alertWay, String quata,
			String email, String mobilePhone, Timestamp createTime,
			Timestamp updateTime) {
		this.tablename = tablename;
		this.ptName = ptName;
		this.ptType = ptType;
		this.monitorTotalNumber = monitorTotalNumber;
		this.upAndDown = upAndDown;
		this.monitorQuata = monitorQuata;
		this.weidu = weidu;
		this.alertWay = alertWay;
		this.quata = quata;
		this.email = email;
		this.mobilePhone = mobilePhone;
		this.createTime = createTime;
		this.updateTime = updateTime;
	}

	// Property accessors

	public Long getJobId() {
		return this.jobId;
	}

	public void setJobId(Long jobId) {
		this.jobId = jobId;
	}

	public String getTablename() {
		return this.tablename;
	}

	public void setTablename(String tablename) {
		this.tablename = tablename;
	}

	public String getPtName() {
		return this.ptName;
	}

	public void setPtName(String ptName) {
		this.ptName = ptName;
	}

	public String getPtType() {
		return this.ptType;
	}

	public void setPtType(String ptType) {
		this.ptType = ptType;
	}

	public Boolean getMonitorTotalNumber() {
		return this.monitorTotalNumber;
	}

	public void setMonitorTotalNumber(Boolean monitorTotalNumber) {
		this.monitorTotalNumber = monitorTotalNumber;
	}

	public Double getUpAndDown() {
		return this.upAndDown;
	}

	public void setUpAndDown(Double upAndDown) {
		this.upAndDown = upAndDown;
	}

	public Boolean getMonitorQuata() {
		return this.monitorQuata;
	}

	public void setMonitorQuata(Boolean monitorQuata) {
		this.monitorQuata = monitorQuata;
	}

	public String getWeidu() {
		return this.weidu;
	}

	public void setWeidu(String weidu) {
		this.weidu = weidu;
	}

	public Integer getAlertWay() {
		return this.alertWay;
	}

	public void setAlertWay(Integer alertWay) {
		this.alertWay = alertWay;
	}

	public String getQuata() {
		return this.quata;
	}

	public void setQuata(String quata) {
		this.quata = quata;
	}

	public String getEmail() {
		return this.email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getMobilePhone() {
		return this.mobilePhone;
	}

	public void setMobilePhone(String mobilePhone) {
		this.mobilePhone = mobilePhone;
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