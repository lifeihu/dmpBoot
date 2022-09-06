package com.sw.bi.scheduler.controller;

import com.sw.bi.scheduler.model.AlertSystemLog;
import com.sw.bi.scheduler.service.AlertSystemLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ConditionModel;
import org.springframework.ui.PaginationSupport;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/manage/alertSystemLog")
public class AlertSystemLogController extends BaseActionController<AlertSystemLog> {

	@Autowired
	private AlertSystemLogService alertSystemLogService;

	@RequestMapping("/pagingGroup")
	@ResponseBody
	public PaginationSupport pagingGroup(@RequestParam("condition") ConditionModel cm, Integer start, Integer limit) {
		cm.setStart(start);
		cm.setLimit(limit);

		return alertSystemLogService.pagingGroup(cm);
	}

	@Override
	public AlertSystemLogService getDefaultService() {
		return alertSystemLogService;
	}

}
