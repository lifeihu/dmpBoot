package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.model.Role;

import java.util.Collection;
import java.util.List;

public interface RoleService extends GenericService<Role> {
	/**
	 * 获得指定用户所有拥有的所有角色ID
	 * 
	 * @param userId
	 * @return
	 */
	public List<Long> getRoleIdsByUser(long userId);

	/**
	 * 获得指定用户未分配的角色
	 * 
	 * @param userId
	 * @return
	 */
	public Collection<Role> getNotAssignRolesByUser(Long userId);
}
