package com.sw.bi.scheduler.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.sql.Timestamp;
import java.util.Date;

/**
 * ActionSql entity. @author MyEclipse Persistence Tools
 */
@JsonIgnoreProperties(value = { "hibernateLazyInitializer" })
public class ActionSql implements java.io.Serializable {

	// Fields

	private Long actionSqlId;
	private Action action;
	private Date taskDate;
	private String hiveSqlPath;
	private String sqlString;
	private Timestamp beginTime;
	private Timestamp endTime;
	private Long runTime;
	private Integer sqlIndex;
	private Timestamp createTime;
	private Timestamp updateTime;
	private String dutyMan;
	private int runResult;

	// Constructors

	/** default constructor */
	public ActionSql() {}

	/** minimal constructor */
	public ActionSql(Action action, Date taskDate, String hiveSqlPath, String sqlString, Timestamp beginTime, Integer sqlIndex, Timestamp createTime, int runResult) {
		this.action = action;
		this.taskDate = taskDate;
		this.hiveSqlPath = hiveSqlPath;
		this.sqlString = sqlString;
		this.beginTime = beginTime;
		this.sqlIndex = sqlIndex;
		this.createTime = createTime;
		this.runResult = runResult;
	}

	/** full constructor */
	public ActionSql(Action action, Date taskDate, String hiveSqlPath, String sqlString, Timestamp beginTime, Timestamp endTime, Long runTime, Integer sqlIndex, Timestamp createTime,
                     Timestamp updateTime, String dutyMan, int runResult) {
		this.action = action;
		this.taskDate = taskDate;
		this.hiveSqlPath = hiveSqlPath;
		this.sqlString = sqlString;
		this.beginTime = beginTime;
		this.endTime = endTime;
		this.runTime = runTime;
		this.sqlIndex = sqlIndex;
		this.createTime = createTime;
		this.updateTime = updateTime;
		this.dutyMan = dutyMan;
		this.runResult = runResult;
	}

	// Property accessors

	public Long getActionSqlId() {
		return this.actionSqlId;
	}

	public void setActionSqlId(Long actionSqlId) {
		this.actionSqlId = actionSqlId;
	}

	@JsonIgnore
	public Action getAction() {
		return this.action;
	}

	public void setAction(Action action) {
		this.action = action;
	}

	public Date getTaskDate() {
		return this.taskDate;
	}

	public void setTaskDate(Date taskDate) {
		this.taskDate = taskDate;
	}

	public String getHiveSqlPath() {
		return this.hiveSqlPath;
	}

	public void setHiveSqlPath(String hiveSqlPath) {
		this.hiveSqlPath = hiveSqlPath;
	}

	public String getSqlString() {
		return this.sqlString;
	}

	public void setSqlString(String sqlString) {
		this.sqlString = sqlString;
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

	public Integer getSqlIndex() {
		return this.sqlIndex;
	}

	public void setSqlIndex(Integer sqlIndex) {
		this.sqlIndex = sqlIndex;
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

	public void setDutyMan(String dutyMan) {
		this.dutyMan = dutyMan;
	}

	public String getDutyMan() {
		return dutyMan;
	}

	public void setRunResult(int runResult) {
		this.runResult = runResult;
	}

	public int getRunResult() {
		return runResult;
	}

}