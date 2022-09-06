package com.sw.bi.scheduler.controller;

import com.sw.bi.scheduler.model.Role;
import com.sw.bi.scheduler.model.User;
import com.sw.bi.scheduler.service.GenericService;
import com.sw.bi.scheduler.service.RoleService;
import com.sw.bi.scheduler.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collection;

@Controller
@RequestMapping("/manage/role")
public class RoleController extends BaseActionController<Role> {

	@Autowired
	public RoleService roleService;

	@Autowired
	public UserService userService;

	/**
	 * 获得指定用户未被分配的角色
	 * 
	 * @param userId
	 * @return
	 */
	@RequestMapping("/notAssignRoles")
	@ResponseBody
	public Collection<Role> getNotAssignRolesByUser(Long userId) {
		return roleService.getNotAssignRolesByUser(userId);
	}

	/**
	 * 获得指定用户的所有角色
	 * 
	 * @return
	 */
	@RequestMapping("/assignRoles")
	@ResponseBody
	public Collection<Role> getRolesByUser(Long userId) {
		User user = userService.get(userId);
		return user.getRoles();
	}

	public GenericService<Role> getDefaultService() {
		return roleService;
	}

}
