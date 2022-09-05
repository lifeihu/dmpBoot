package com.sw.bi.scheduler.model;

import java.io.Serializable;
import java.util.Date;

public class LoginLogger implements Serializable {

	private Long loginLoggerId;

	/**
	 * 登录用户ID
	 */
	private long userId;

	/**
	 * 登录帐号
	 */
	private String loginName;

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
	 * 登录IP
	 */
	private String loginIp;

	/**
	 * 登录验证码
	 */
	private String vertifyCode;

	private Date createTime;
	private Date updateTime;

	public Long getLoginLoggerId() {
		return loginLoggerId;
	}

	public void setLoginLoggerId(Long loginLoggerId) {
		this.loginLoggerId = loginLoggerId;
	}

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public String getLoginName() {
		return loginName;
	}

	public void setLoginName(String loginName) {
		this.loginName = loginName;
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

	public String getLoginIp() {
		return loginIp;
	}

	public void setLoginIp(String loginIp) {
		this.loginIp = loginIp;
	}

	public String getVertifyCode() {
		return vertifyCode;
	}

	public void setVertifyCode(String vertifyCode) {
		this.vertifyCode = vertifyCode;
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
