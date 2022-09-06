package com.sw.bi.scheduler.controller;

import com.sw.bi.scheduler.model.ServerOperate;
import com.sw.bi.scheduler.service.ServerOperateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequestMapping("/manage/serverOperate")
public class ServerOperateController extends BaseActionController<ServerOperate> {

	@Autowired
	private ServerOperateService serverOperateService;

	/**
	 * 服务器操作上线
	 * 
	 * @param serverOperateId
	 */
	@RequestMapping("/online")
	@ResponseBody
	public void online(long serverOperateId) {
		serverOperateService.online(serverOperateId);
	}

	/**
	 * 服务器操作下线
	 * 
	 * @param serverOperateId
	 */
	@RequestMapping("/offline")
	@ResponseBody
	public void offline(long serverOperateId) {
		serverOperateService.offline(serverOperateId);
	}

	/**
	 * 执行服务器操作
	 * 
	 * @param serverOperateId
	 */
	@RequestMapping("/execute")
	@ResponseBody
	public Map<String, String> execute(long serverOperateId) {
		return serverOperateService.execute(serverOperateId);
	}

	@Override
	public ServerOperateService getDefaultService() {
		return serverOperateService;
	}

}
