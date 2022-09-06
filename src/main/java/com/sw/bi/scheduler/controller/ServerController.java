package com.sw.bi.scheduler.controller;

import com.sw.bi.scheduler.model.Server;
import com.sw.bi.scheduler.service.ServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/manage/server")
public class ServerController extends BaseActionController<Server> {

	@Autowired
	private ServerService serverService;

	@Override
	public ServerService getDefaultService() {
		return serverService;
	}

}
