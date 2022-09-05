package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.model.Resource;

import java.util.List;

public interface ResourceService extends GenericService<Resource> {

	/**
	 * 获得指定资源的下级资源
	 * 
	 * @param parentId
	 * @param onceLoad
	 *            是否一次加载所有菜单
	 * @return
	 */
	public List<Resource> getChildrenResources(Long parentId, Boolean onceLoad);

	/**
	 * 根据父节点生成子节点ID
	 * 
	 * @param parentId
	 * @return
	 */
	public long generateResourceId(Long parentId);
}
