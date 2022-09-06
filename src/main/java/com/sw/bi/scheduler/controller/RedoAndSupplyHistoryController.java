package com.sw.bi.scheduler.controller;

import com.sw.bi.scheduler.model.RedoAndSupplyHistory;
import com.sw.bi.scheduler.service.GenericService;
import com.sw.bi.scheduler.service.RedoAndSupplyHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ConditionModel;
import org.springframework.ui.PaginationSupport;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/manage/redoAndSupplyHistory")
public class RedoAndSupplyHistoryController extends BaseActionController<RedoAndSupplyHistory> {

	@Autowired
	private RedoAndSupplyHistoryService redoAndSupplyHistoryService;

	/**
	 * 获得重跑的主任务
	 * 
	 * @return
	 */
	@RequestMapping("/redoMasters")
	@ResponseBody
	public PaginationSupport redoMasters(@RequestParam("condition") ConditionModel cm, Integer start, Integer limit) {
		cm.setStart(start);
		cm.setLimit(limit);

		return redoAndSupplyHistoryService.pagingRedoMasters(cm);
	}

	/**
	 * 获得补的主任务
	 * 
	 * @return
	 */
	@RequestMapping("/supplyMasters")
	@ResponseBody
	public PaginationSupport supplyMaster(@RequestParam("condition") ConditionModel cm, Integer start, Integer limit) {
		cm.setStart(start);
		cm.setLimit(limit);

		return redoAndSupplyHistoryService.pagingSupplyMasters(cm);
	}

	@Override
	public GenericService<RedoAndSupplyHistory> getDefaultService() {
		return redoAndSupplyHistoryService;
	}

}
