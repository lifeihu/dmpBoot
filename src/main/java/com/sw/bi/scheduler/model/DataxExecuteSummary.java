package com.sw.bi.scheduler.model;

import java.sql.Timestamp;

/**
 * DataxExecuteSummary entity. @author MyEclipse Persistence Tools
 */

public class DataxExecuteSummary implements java.io.Serializable {

	// Fields

	private Long dataxExecuteSummaryId;
	private Long actionid;
	private String xmlFile;
	private Timestamp beginTime;
	private Timestamp endTime;
	private Long runTime;
	private Integer totalReadSuccessLines;
	private Integer totalReadFailedLines;
	private Integer totalWriteSuccessLines;
	private Integer totalWriteFailedLines;
	private Integer totalLines;
	private Integer totalFailedLines;
	private Timestamp createTime;
	private Timestamp updateTime;
	// add by mashifeng 2019年1月9日10:23:26
	private Integer readSpeed;
	private Double taskFlow;
	
	// Constructors

	/** default constructor */
	public DataxExecuteSummary() {
	}

	/** minimal constructor */
	public DataxExecuteSummary(Long actionid, Timestamp beginTime,
			Timestamp endTime, Long runTime, Integer totalReadSuccessLines,
			Integer totalReadFailedLines, Integer totalWriteSuccessLines,
			Integer totalWriteFailedLines, Integer totalLines,
			Integer totalFailedLines, Timestamp createTime) {
		this.actionid = actionid;
		this.beginTime = beginTime;
		this.endTime = endTime;
		this.runTime = runTime;
		this.totalReadSuccessLines = totalReadSuccessLines;
		this.totalReadFailedLines = totalReadFailedLines;
		this.totalWriteSuccessLines = totalWriteSuccessLines;
		this.totalWriteFailedLines = totalWriteFailedLines;
		this.totalLines = totalLines;
		this.totalFailedLines = totalFailedLines;
		this.createTime = createTime;
	}

	/** full constructor */
	public DataxExecuteSummary(Long actionid, String xmlFile,
			Timestamp beginTime, Timestamp endTime, Long runTime,
			Integer totalReadSuccessLines, Integer totalReadFailedLines,
			Integer totalWriteSuccessLines, Integer totalWriteFailedLines,
			Integer totalLines, Integer totalFailedLines, Timestamp createTime,
			Timestamp updateTime, Integer readSpeed, Double taskFlow) {
		this.actionid = actionid;
		this.xmlFile = xmlFile;
		this.beginTime = beginTime;
		this.endTime = endTime;
		this.runTime = runTime;
		this.totalReadSuccessLines = totalReadSuccessLines;
		this.totalReadFailedLines = totalReadFailedLines;
		this.totalWriteSuccessLines = totalWriteSuccessLines;
		this.totalWriteFailedLines = totalWriteFailedLines;
		this.totalLines = totalLines;
		this.totalFailedLines = totalFailedLines;
		this.createTime = createTime;
		this.updateTime = updateTime;
		// add by mashifeng 2019年1月9日10:23:26
		this.readSpeed = readSpeed;
		this.taskFlow = taskFlow;
	}

	// Property accessors

	public Long getDataxExecuteSummaryId() {
		return this.dataxExecuteSummaryId;
	}

	public void setDataxExecuteSummaryId(Long dataxExecuteSummaryId) {
		this.dataxExecuteSummaryId = dataxExecuteSummaryId;
	}

	public Long getActionid() {
		return this.actionid;
	}

	public void setActionid(Long actionid) {
		this.actionid = actionid;
	}

	public String getXmlFile() {
		return this.xmlFile;
	}

	public void setXmlFile(String xmlFile) {
		this.xmlFile = xmlFile;
	}

	public Timestamp getBeginTime() {
		return this.beginTime;
	}

	public void setBeginTime(Timestamp beginTime) {
		this.beginTime = beginTime;
	}

	public Timestamp getEndTime() {
		return this.endTime;
	}

	public void setEndTime(Timestamp endTime) {
		this.endTime = endTime;
	}

	public Long getRunTime() {
		return this.runTime;
	}

	public void setRunTime(Long runTime) {
		this.runTime = runTime;
	}

	public Integer getTotalReadSuccessLines() {
		return this.totalReadSuccessLines;
	}

	public void setTotalReadSuccessLines(Integer totalReadSuccessLines) {
		this.totalReadSuccessLines = totalReadSuccessLines;
	}

	public Integer getTotalReadFailedLines() {
		return this.totalReadFailedLines;
	}

	public void setTotalReadFailedLines(Integer totalReadFailedLines) {
		this.totalReadFailedLines = totalReadFailedLines;
	}

	public Integer getTotalWriteSuccessLines() {
		return this.totalWriteSuccessLines;
	}

	public void setTotalWriteSuccessLines(Integer totalWriteSuccessLines) {
		this.totalWriteSuccessLines = totalWriteSuccessLines;
	}

	public Integer getTotalWriteFailedLines() {
		return this.totalWriteFailedLines;
	}

	public void setTotalWriteFailedLines(Integer totalWriteFailedLines) {
		this.totalWriteFailedLines = totalWriteFailedLines;
	}

	public Integer getTotalLines() {
		return this.totalLines;
	}

	public void setTotalLines(Integer totalLines) {
		this.totalLines = totalLines;
	}

	public Integer getTotalFailedLines() {
		return this.totalFailedLines;
	}

	public void setTotalFailedLines(Integer totalFailedLines) {
		this.totalFailedLines = totalFailedLines;
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
	
	// add by mashifeng 2019年1月9日10:23:26
	public Integer getReadSpeed() {
		return readSpeed;
	}

	public void setReadSpeed(Integer readSpeed) {
		this.readSpeed = readSpeed;
	}

	public Double getTaskFlow() {
		return taskFlow;
	}

	public void setTaskFlow(Double taskFlow) {
		this.taskFlow = taskFlow;
	}

}