package com.sw.bi.scheduler.model;

import com.sw.bi.scheduler.util.Configure.HudsonPublishStatus;

import java.util.Date;

/**
 * Hudson项目
 */
public class HudsonProject implements java.io.Serializable, AuthenticationUserGroup {

	private Long hudsonProjectId;

	/**
	 * Hudson项目名称
	 */
	private String name;

	/**
	 * SVN目录
	 */
	private String svnPath;

	/**
	 * 发布目录
	 */
	private String localPath;

	/**
	 * 最近一次发布状态
	 */
	private int publishStatus = HudsonPublishStatus.UN_PUBLISH.ordinal();

	/**
	 * 最近一次发布开始时间
	 */
	private Date publishStartTime;

	/**
	 * 最近一次发布结束时间
	 */
	private Date publishEndTime;

	/**
	 * 最近一次发布日志
	 */
	private String publishLogFile;

	private long createBy;
	private Date createTime;
	private Date updateTime;

	public Long getHudsonProjectId() {
		return hudsonProjectId;
	}

	public void setHudsonProjectId(Long hudsonProjectId) {
		this.hudsonProjectId = hudsonProjectId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSvnPath() {
		return svnPath;
	}

	public void setSvnPath(String svnPath) {
		this.svnPath = svnPath;
	}

	public String getLocalPath() {
		return localPath;
	}

	public int getPublishStatus() {
		return publishStatus;
	}

	public void setPublishStatus(int publishStatus) {
		this.publishStatus = publishStatus;
	}

	public Date getPublishStartTime() {
		return publishStartTime;
	}

	public void setPublishStartTime(Date publishStartTime) {
		this.publishStartTime = publishStartTime;
	}

	public Date getPublishEndTime() {
		return publishEndTime;
	}

	public void setPublishEndTime(Date publishEndTime) {
		this.publishEndTime = publishEndTime;
	}

	public String getPublishLogFile() {
		return publishLogFile;
	}

	public void setPublishLogFile(String publishLogFile) {
		this.publishLogFile = publishLogFile;
	}

	public void setLocalPath(String localPath) {
		this.localPath = localPath;
	}

	public long getCreateBy() {
		return createBy;
	}

	public void setCreateBy(long createBy) {
		this.createBy = createBy;
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

	@Override
	public String getEntityName() {
		return "Hudson项目";
	}

	@Override
	public String getLoggerName() {
		return this.getName();
	}

	@Override
	public Long getUserId() {
		return this.getCreateBy();
	}

}
