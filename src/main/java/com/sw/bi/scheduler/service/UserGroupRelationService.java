package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.model.User;
import com.sw.bi.scheduler.model.UserGroup;
import com.sw.bi.scheduler.model.UserGroupRelation;

import java.util.Collection;

public interface UserGroupRelationService extends GenericService<UserGroupRelation> {

	/**
	 * 获得指定用户组下所有用户
	 * 
	 * @param userGroupId
	 * @param cascade
	 *            是否获得指定用户组的子用户组用户
	 * @return
	 */
	public Collection<User> getUsersByUserGroup(long userGroupId, boolean cascade);

	/**
	 * 给指定用户组分配指定用户
	 * 
	 * @param userGroupId
	 * @param userId
	 */
	public void assignUsers(long userGroupId, String userId);

	/**
	 * 解除用户组已分配用户
	 * 
	 * @param userGroupId
	 * @param allowLogger
	 */
	public void unassignUsers(long userGroupId, boolean allowLogger);

	/**
	 * 获得指定用户所属的用户组
	 * 
	 * @param userId
	 * @return
	 */
	public UserGroup getUserGroupByUser(long userId);

	/**
	 * 获得指定用户所属的用户组(含子用户组)
	 * 
	 * @param userId
	 * @return
	 */
	public Collection<UserGroup> getUserGroupsByUser(long userId);

}
