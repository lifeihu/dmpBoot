package com.sw.bi.scheduler.controller;

import com.sw.bi.scheduler.model.HudsonProject;
import com.sw.bi.scheduler.service.HudsonProjectService;
import com.sw.bi.scheduler.util.OperateAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/manage/hudsonProject")
public class HudsonProjectController extends BaseActionController<HudsonProject> {

	@Autowired
	private HudsonProjectService hudsonProjectService;

	/**
	 * 发布指定项目
	 * 
	 * @param hudsonProjectId
	 */
	@RequestMapping("/publish")
	public void publish(long hudsonProjectId) {
		HudsonProject hudsonProject = hudsonProjectService.get(hudsonProjectId);

		hudsonProjectService.isAuthorizedUserGroup(hudsonProject, OperateAction.PUBLISH);

		hudsonProjectService.publish(hudsonProjectId);
	}

	/**
	 * 获得指定发布日志
	 * 
	 * @param logFile
	 * @return
	 */
	@RequestMapping("/log")
	@ResponseBody
	public String getPublishLog(String logFile) {
		return hudsonProjectService.getPublishLog(logFile);
	}

	@Override
	public HudsonProjectService getDefaultService() {
		return hudsonProjectService;
	}

}
