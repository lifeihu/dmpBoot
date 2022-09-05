package com.sw.bi.scheduler.model;

import com.sw.bi.scheduler.util.Configure.ServerOperateStatus;
import org.apache.commons.beanutils.ConvertUtils;

import java.util.Date;

/**
 * 服务器Shell执行
 * 
 * @author shiming.hong
 */
public class ServerOperate {

	private Long serverOperateId;
	private String name;
	private String businessGroup;
	private long serverUserId;
	private long serverShellId;
	private String serverId;
	private String serverName;
	private boolean showResult = true;
	private int status = ServerOperateStatus.UN_LINE.indexOf();
	private String description;
	private String createdBy;
	private String updatedBy;
	private Date createTime;
	private Date updateTime;
	private boolean active = true;

	public Long getServerOperateId() {
		return serverOperateId;
	}

	public void setServerOperateId(Long serverOperateId) {
		this.serverOperateId = serverOperateId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBusinessGroup() {
		return businessGroup;
	}

	public void setBusinessGroup(String businessGroup) {
		this.businessGroup = businessGroup;
	}

	public long getServerUserId() {
		return serverUserId;
	}

	public void setServerUserId(long serverUserId) {
		this.serverUserId = serverUserId;
	}

	public long getServerShellId() {
		return serverShellId;
	}

	public void setServerShellId(long serverShellId) {
		this.serverShellId = serverShellId;
	}

	public String getServerId() {
		return serverId;
	}

	public Long[] getServerIds() {
		return (Long[]) ConvertUtils.convert(serverId.split(","), Long.class);
	}

	public void setServerId(String serverId) {
		this.serverId = serverId;
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public boolean isShowResult() {
		return showResult;
	}

	public void setShowResult(boolean showResult) {
		this.showResult = showResult;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
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
