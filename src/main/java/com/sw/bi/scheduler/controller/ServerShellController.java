package com.sw.bi.scheduler.controller;

import com.sw.bi.scheduler.model.ServerShell;
import com.sw.bi.scheduler.service.ServerShellService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/manage/serverShell")
public class ServerShellController extends BaseActionController<ServerShell> {

	@Autowired
	private ServerShellService serverShellService;

	@Override
	public ServerShellService getDefaultService() {
		return serverShellService;
	}

}
