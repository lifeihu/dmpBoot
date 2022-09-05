package com.sw.bi.scheduler.model;

import java.util.Date;

/**
 * 服务器Shell
 * 
 * @author shiming.hong
 */
public class ServerShell {

	private Long serverShellId;
	private String name;
	private String path;
	private String command;
	private String description;
	private String createdBy;
	private String updatedBy;
	private Date createTime;
	private Date updateTime;
	private boolean active = true;

	public Long getServerShellId() {
		return serverShellId;
	}

	public void setServerShellId(Long serverShellId) {
		this.serverShellId = serverShellId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
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

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

}
