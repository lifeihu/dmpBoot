package com.sw.bi.scheduler.controller;

import com.sw.bi.scheduler.model.ServerUser;
import com.sw.bi.scheduler.service.ServerUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/manage/serverUser")
public class ServerUserController extends BaseActionController<ServerUser> {

	@Autowired
	private ServerUserService serverUserService;

	@RequestMapping("/password")
	public void changePassword(long serverUserId, String password) {
		serverUserService.changePassword(serverUserId, password);
	}

	@Override
	public ServerUserService getDefaultService() {
		return serverUserService;
	}

}
