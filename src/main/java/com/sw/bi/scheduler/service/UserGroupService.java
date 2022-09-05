package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.model.UserGroup;

public interface UserGroupService extends GenericService<UserGroup> {

	/**
	 * 逻辑删除用户组
	 * 
	 * @param userGroup
	 */
	public void logicDelete(UserGroup userGroup);

	/**
	 * 恢复用户组
	 * 
	 * @param userGroup
	 */
	public void recovery(UserGroup userGroup);

}
