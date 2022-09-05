package com.sw.bi.scheduler.model;

import java.sql.Timestamp;

/**
 * MailJobConfig entity. @author MyEclipse Persistence Tools
 */

public class MailJobConfig implements java.io.Serializable {

	// Fields

	private Long jobId;
	private String mailReceivers;
	private String mailContent;
	private Long datasourceId;
	private Timestamp createTime;
	private Timestamp updateTime;
	private String mailTitle;

	// Constructors

	/** default constructor */
	public MailJobConfig() {
	}

	/** minimal constructor */
	public MailJobConfig(Long jobId, String mailReceivers,
			Timestamp createTime, String mailTitle) {
		this.jobId = jobId;
		this.mailReceivers = mailReceivers;
		this.createTime = createTime;
		this.mailTitle = mailTitle;
	}

	/** full constructor */
	public MailJobConfig(Long jobId, String mailReceivers, String mailContent,
			Long datasourceId, Timestamp createTime, Timestamp updateTime,
			String mailTitle) {
		this.jobId = jobId;
		this.mailReceivers = mailReceivers;
		this.mailContent = mailContent;
		this.datasourceId = datasourceId;
		this.createTime = createTime;
		this.updateTime = updateTime;
		this.mailTitle = mailTitle;
	}

	// Property accessors

	public Long getJobId() {
		return this.jobId;
	}

	public void setJobId(Long jobId) {
		this.jobId = jobId;
	}

	public String getMailReceivers() {
		return this.mailReceivers;
	}

	public void setMailReceivers(String mailReceivers) {
		this.mailReceivers = mailReceivers;
	}

	public String getMailContent() {
		return this.mailContent;
	}

	public void setMailContent(String mailContent) {
		this.mailContent = mailContent;
	}

	public Long getDatasourceId() {
		return this.datasourceId;
	}

	public void setDatasourceId(Long datasourceId) {
		this.datasourceId = datasourceId;
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

	public String getMailTitle() {
		return this.mailTitle;
	}

	public void setMailTitle(String mailTitle) {
		this.mailTitle = mailTitle;
	}

}