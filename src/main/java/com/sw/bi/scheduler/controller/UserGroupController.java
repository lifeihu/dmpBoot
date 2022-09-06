package com.sw.bi.scheduler.controller;

import com.sw.bi.scheduler.model.UserGroup;
import com.sw.bi.scheduler.service.UserGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/manage/userGroup")
public class UserGroupController extends BaseActionController<UserGroup> {

	@Autowired
	private UserGroupService userGroupService;

	@RequestMapping("/logicRemove")
	@ResponseBody
	public void logicRemove(String id) {
		if (StringUtils.hasText(id)) {
			for (String userGroupId : id.split(",")) {
				userGroupService.logicDelete(userGroupService.get(Long.valueOf(userGroupId)));
			}
		}
	}

	@RequestMapping("/recovery")
	public void recovery(String id) {
		if (StringUtils.hasText(id)) {
			for (String userGroupId : id.split(",")) {
				userGroupService.recovery(userGroupService.get(Long.valueOf(userGroupId)));
			}
		}
	}

	@Override
	public UserGroupService getDefaultService() {
		return userGroupService;
	}
}
