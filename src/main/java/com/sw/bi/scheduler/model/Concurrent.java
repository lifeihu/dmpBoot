package com.sw.bi.scheduler.model;

import java.util.Date;

/**
 * 集群中各作业类型的并发数设置
 * 
 * @author shiming.hong
 */
public class Concurrent {

	private long concurrentId;

	/**
	 * 分类
	 */
	private int category;

	/**
	 * 名称
	 */
	private String name;

	/**
	 * 作业类型
	 */
	private long jobType;

	/**
	 * 指定作业类型的任务运行时最大并发数
	 */
	private int runningMaxConcurrentNumber;

	/**
	 * 指定作业类型的大数据任务运行时最大并发数
	 */
	private int runningBigDataMaxConcurrentNumber;

	/**
	 * 指定作业类型的大数据任务的运行时长阀值
	 */
	private int bigDataRunTimeThreshold;

	private Date createTime;
	private Date updateTime;

	public int getCategory() {
		return category;
	}

	public void setCategory(int category) {
		this.category = category;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getConcurrentId() {
		return concurrentId;
	}

	public void setConcurrentId(long concurrentId) {
		this.concurrentId = concurrentId;
	}

	public long getJobType() {
		return jobType;
	}

	public void setJobType(long jobType) {
		this.jobType = jobType;
	}

	public int getRunningMaxConcurrentNumber() {
		return runningMaxConcurrentNumber;
	}

	public void setRunningMaxConcurrentNumber(int runningMaxConcurrentNumber) {
		this.runningMaxConcurrentNumber = runningMaxConcurrentNumber;
	}

	public int getRunningBigDataMaxConcurrentNumber() {
		return runningBigDataMaxConcurrentNumber;
	}

	public void setRunningBigDataMaxConcurrentNumber(int runningBigDataMaxConcurrentNumber) {
		this.runningBigDataMaxConcurrentNumber = runningBigDataMaxConcurrentNumber;
	}

	public int getBigDataRunTimeThreshold() {
		return bigDataRunTimeThreshold;
	}

	public void setBigDataRunTimeThreshold(int bigDataRunTimeThreshold) {
		this.bigDataRunTimeThreshold = bigDataRunTimeThreshold;
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
