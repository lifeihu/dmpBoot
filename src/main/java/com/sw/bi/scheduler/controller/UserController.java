package com.sw.bi.scheduler.controller;

import com.sw.bi.scheduler.model.User;
import com.sw.bi.scheduler.service.GenericService;
import com.sw.bi.scheduler.service.UserGroupRelationService;
import com.sw.bi.scheduler.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collection;

@Controller
@RequestMapping("/manage/user")
public class UserController extends BaseActionController<User> {

	@Autowired
	private UserService userService;

	@Autowired
	private UserGroupRelationService userGroupRelationService;

	@RequestMapping("/password")
	@ResponseBody
	public String changePassword(long userId, String password) {
		return userService.changePassword(userId, password);
	}

	@RequestMapping("/batchChangePassword")
	public void batchChangePassword() {
		userService.batchChangePassword();
	}

	/**
	 * <pre>
	 * 	获得未被分配的用户
	 * 	这里获得的未分配用户是指未分配给任意用户组的用户
	 * </pre>
	 * 
	 * @return
	 */
	@RequestMapping("/notAssignUsers")
	@ResponseBody
	public Collection<User> getNotAssignUsersByUserGroup() {
		return userService.getNotAssignUsersByUserGroup();
	}

	/**
	 * 获得指定用户的所有用户
	 * 
	 * @param userGroupId
	 * @return
	 */
	@RequestMapping("/assignUsers")
	@ResponseBody
	public Collection<User> getUsersByUserGroup(Long userGroupId) {
		return userGroupRelationService.getUsersByUserGroup(userGroupId, false);
	}

	@Override
	public GenericService<User> getDefaultService() {
		return userService;
	}
}
