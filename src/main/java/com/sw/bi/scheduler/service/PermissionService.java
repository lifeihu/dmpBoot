package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.model.Permission;
import com.sw.bi.scheduler.model.Resource;
import org.springframework.security.web.access.intercept.DatabaseSecurityMetadataMapping;
import org.springframework.ui.PaginationSupport;

import java.util.List;

public interface PermissionService extends GenericService<Permission>, DatabaseSecurityMetadataMapping {

	/**
	 * 获得指定用户所拥有的所有菜单
	 * 
	 * @param userId
	 * @return
	 */
	public List<Resource> getMenuTreeByUser(Long userId);

	/**
	 * 获得指定用户所拥有的所有资源
	 * 
	 * @param userId
	 * @return
	 */
	public List<Resource> getResourceTreeByUser(Long userId);

	/**
	 * 获得指定用户指定父资源的所有下层资源
	 * 
	 * @param userId
	 * @param parentId
	 * @return
	 */
	public List<Resource> getResourceTreeByUser(Long userId, Long parentResourceId);

	/**
	 * 获得指定资源的所有下层资源
	 * 
	 * @param userId
	 * @param resourceId
	 * @return
	 */
	public PaginationSupport getResourcesListByUser(Long userId, Long resourceId, Integer start, Integer limit);

	/**
	 * 获得指定角色拥有的所有资源
	 * 
	 * @param roleId
	 * @return
	 */
	public List<Resource> getPermissionResourcesByRole(long roleId);

	/**
	 * 获得指定角色所拥有的所有资源
	 * 
	 * @param roleIds
	 * @return
	 */
	public List<Resource> getResourcesByRole(List<Long> roleIds);

	/**
	 * 给角色授权菜单
	 * 
	 * @param roleId
	 * @param resourceIds
	 */
	public void grant(long roleId, List<Long> resourceIds);
	
	/**
	 * 删除指定角色的所有权限
	 * @param roleId
	 */
	public void deleteByRole(long roleId);
}
