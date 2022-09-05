package com.sw.bi.scheduler.model;

import java.io.Serializable;
import java.util.Date;

public class OperateLogger implements Serializable {

	private Long operateLoggerId;

	/**
	 * 登录用户ID
	 */
	private long userId;

	/**
	 * 登录用户
	 */
	private String userName;

	/**
	 * 用户组ID
	 */
	private Long userGroupId;

	/**
	 * 用户组名称
	 */
	private String userGroupName;

	/**
	 * 操作内容
	 */
	private String operateContent;

	/**
	 * 操作动作
	 */
	private String operateAction;

	/**
	 * 操作IP
	 */
	private String operateIp;

	private Date createTime;
	private Date updateTime;

	public Long getOperateLoggerId() {
		return operateLoggerId;
	}

	public void setOperateLoggerId(Long operateLoggerId) {
		this.operateLoggerId = operateLoggerId;
	}

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public Long getUserGroupId() {
		return userGroupId;
	}

	public void setUserGroupId(Long userGroupId) {
		this.userGroupId = userGroupId;
	}

	public String getUserGroupName() {
		return userGroupName;
	}

	public void setUserGroupName(String userGroupName) {
		this.userGroupName = userGroupName;
	}

	public String getOperateContent() {
		return operateContent;
	}

	public void setOperateContent(String operateContent) {
		this.operateContent = operateContent;
	}

	public String getOperateAction() {
		return operateAction;
	}

	public void setOperateAction(String operateAction) {
		this.operateAction = operateAction;
	}

	public String getOperateIp() {
		return operateIp;
	}

	public void setOperateIp(String operateIp) {
		this.operateIp = operateIp;
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
