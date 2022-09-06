package com.sw.bi.scheduler.controller;

import com.sw.bi.scheduler.model.OperateLogger;
import com.sw.bi.scheduler.service.OperateLoggerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/manage/operateLogger")
public class OperateLoggerController extends BaseActionController<OperateLogger> {

	@Autowired
	private OperateLoggerService operateLoggerService;

	@Override
	public OperateLoggerService getDefaultService() {
		return operateLoggerService;
	}

}
