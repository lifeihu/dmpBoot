package com.sw.bi.scheduler.model;

import java.io.Serializable;
import java.util.Date;

public class UserGroup implements Serializable, LoggerEntity {

	private Long userGroupId;
	private Long parentId;
	private String name;

	/**
	 * 该用户组能访问的Hive数据库
	 */
	private String hiveDatabase;

	/**
	 * 该用户组能访问的HDFS目录
	 */
	private String hdfsPath;
	private String description;
	private int sortNo;
	private boolean administrator = false;
	private boolean active;
	private Date createTime;
	private Date updateTime;

	public Long getUserGroupId() {
		return userGroupId;
	}

	public void setUserGroupId(Long userGroupId) {
		this.userGroupId = userGroupId;
	}

	public Long getParentId() {
		return parentId;
	}

	public void setParentId(Long parentId) {
		this.parentId = parentId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getHiveDatabase() {
		return hiveDatabase;
	}

	public void setHiveDatabase(String hiveDatabase) {
		this.hiveDatabase = hiveDatabase;
	}

	public String getHdfsPath() {
		return hdfsPath;
	}

	public void setHdfsPath(String hdfsPath) {
		this.hdfsPath = hdfsPath;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getSortNo() {
		return sortNo;
	}

	public void setSortNo(int sortNo) {
		this.sortNo = sortNo;
	}

	public boolean isAdministrator() {
		return administrator;
	}

	public void setAdministrator(boolean administrator) {
		this.administrator = administrator;
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

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	@Override
	public String getEntityName() {
		return "用户组";
	}

	@Override
	public String getLoggerName() {
		return this.getName() + "(" + this.getUserGroupId() + ")";
	}

}
