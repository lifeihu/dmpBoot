package com.sw.bi.scheduler.controller;

import com.sw.bi.scheduler.model.AlertSystemConfig;
import com.sw.bi.scheduler.service.AlertSystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/manage/alertSystemConfig")
public class AlertSystemConfigController extends BaseActionController<AlertSystemConfig> {

	@Autowired
	private AlertSystemConfigService alertSystemConfigService;

	@Override
	public AlertSystemConfigService getDefaultService() {
		return alertSystemConfigService;
	}

}
