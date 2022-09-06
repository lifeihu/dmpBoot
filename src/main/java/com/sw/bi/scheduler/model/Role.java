package com.sw.bi.scheduler.model;

// Generated 2012-2-2 13:44:07 by Hibernate Tools 3.4.0.CR1


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Role generated by hbm2java
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class Role implements java.io.Serializable {

	private Long roleId;
	private String roleName;
	private boolean isAdmin;
	private Date createTime;
	private Date updateTime;
	@JsonIgnore
	private Set<User> users = new LinkedHashSet<User>();

	public Role() {}

	public Role(String roleName, boolean isAdmin, Date createTime) {
		this.roleName = roleName;
		this.isAdmin = isAdmin;
		this.createTime = createTime;
	}

	public Role(String roleName, boolean isAdmin, Date createTime, Date updateTime) {
		this.roleName = roleName;
		this.isAdmin = isAdmin;
		this.createTime = createTime;
		this.updateTime = updateTime;
	}

	public Long getRoleId() {
		return this.roleId;
	}

	public void setRoleId(Long roleId) {
		this.roleId = roleId;
	}

	public String getRoleName() {
		return this.roleName;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	public boolean getIsAdmin() {
		return isAdmin;
	}

	public void setIsAdmin(boolean isAdmin) {
		this.isAdmin = isAdmin;
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

	public Set<User> getUsers() {
		return users;
	}

	@JsonIgnore
	public void setUsers(Set<User> users) {
		this.users = users;
	}

}
