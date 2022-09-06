package com.sw.bi.scheduler.controller;

import com.sw.bi.scheduler.model.User;
import com.sw.bi.scheduler.model.UserGroup;
import com.sw.bi.scheduler.model.UserGroupRelation;
import com.sw.bi.scheduler.service.UserGroupRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collection;

@Controller
@RequestMapping("/manage/userGroupRelation")
public class UserGroupRelationController extends BaseActionController<UserGroupRelation> {

	@Autowired
	private UserGroupRelationService userGroupRelationService;

	/**
	 * 给指定用户组分配指定用户
	 * 
	 * @param userGroupId
	 * @param userId
	 */
	@RequestMapping("/assignUsers")
	public void assignUsers(long userGroupId, String userId) {
		userGroupRelationService.assignUsers(userGroupId, userId);
	}

	@RequestMapping("/users")
	@ResponseBody
	public Collection<User> getUsersByUserGroup(long userGroupId, boolean userGroupCascade) {
		return userGroupRelationService.getUsersByUserGroup(userGroupId, userGroupCascade);
	}

	@RequestMapping("/userGroups")
	@ResponseBody
	public Collection<UserGroup> getUserGroupsByUser(long userId) {
		return userGroupRelationService.getUserGroupsByUser(userId);
	}

	@Override
	public UserGroupRelationService getDefaultService() {
		return userGroupRelationService;
	}
}
