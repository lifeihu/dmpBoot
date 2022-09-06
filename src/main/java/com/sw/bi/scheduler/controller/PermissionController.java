package com.sw.bi.scheduler.controller;

import com.sw.bi.scheduler.model.Permission;
import com.sw.bi.scheduler.model.Resource;
import com.sw.bi.scheduler.service.GenericService;
import com.sw.bi.scheduler.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ConditionModel;
import org.springframework.ui.PaginationSupport;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/manage/permission")
public class PermissionController extends BaseActionController<Permission> {

	@Autowired
	private PermissionService permissionService;

	@RequestMapping("/resourceTree")
	@ResponseBody
	public List<Resource> getResourceTree(Long userId, @RequestParam("node") Long parentId, boolean onceLoad) {
		if (onceLoad)
			return permissionService.getResourceTreeByUser(userId);
		else
			return permissionService.getResourceTreeByUser(userId, parentId);
	}

	@RequestMapping("/resourceList")
	@ResponseBody
	public PaginationSupport getResourceList(@RequestParam("condition") ConditionModel cm, Integer start, Integer limit) {
		return permissionService.getResourcesListByUser(cm.getValue("userId", Long.class), cm.getValue("resourceId", Long.class), start, limit);
	}

	@RequestMapping("/menuTree")
	@ResponseBody
	public List<Resource> getMenusByUser(Long userId) {
		return permissionService.getMenuTreeByUser(userId);
	}

	@RequestMapping("/resources")
	@ResponseBody
	public List<Long> list(long roleId) {
		List<Resource> resources = permissionService.getPermissionResourcesByRole(roleId);
		List<Long> resourceIds = new ArrayList<Long>();
		if (resources.size() > 0) {
			for (Resource resource : resources)
				resourceIds.add(resource.getResourceId());
		}

		return resourceIds;
	}

	@RequestMapping("/grant")
	public void grant(long roleId) {
		List<Long> resourceIds = decodeCollection("resourceIds", Long.class);
		permissionService.grant(roleId, resourceIds);
		/*List<Permission> permissions = decodeCollection("permissions", Permission.class);
		for (Permission permission : permissions)
			permissionService.save(permission);*/
	}

	@Override
	public GenericService<Permission> getDefaultService() {
		return permissionService;
	}
}
