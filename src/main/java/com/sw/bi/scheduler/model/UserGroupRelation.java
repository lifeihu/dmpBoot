package com.sw.bi.scheduler.model;

import java.io.Serializable;

public class UserGroupRelation implements Serializable {

	private Long userGroupRelationId;
	private long userId;
	private long userGroupId;

	public Long getUserGroupRelationId() {
		return userGroupRelationId;
	}

	public void setUserGroupRelationId(Long userGroupRelationId) {
		this.userGroupRelationId = userGroupRelationId;
	}

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public long getUserGroupId() {
		return userGroupId;
	}

	public void setUserGroupId(long userGroupId) {
		this.userGroupId = userGroupId;
	}

}
