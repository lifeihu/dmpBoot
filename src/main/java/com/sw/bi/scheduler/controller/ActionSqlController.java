package com.sw.bi.scheduler.controller;

import com.sw.bi.scheduler.model.ActionSql;
import com.sw.bi.scheduler.service.ActionSqlService;
import com.sw.bi.scheduler.service.GenericService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ConditionModel;
import org.springframework.ui.Model;
import org.springframework.ui.PaginationSupport;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/manage/actionSql")
public class ActionSqlController extends BaseActionController<ActionSql> {

	@Autowired
	private ActionSqlService actionSqlService;

	@Override
	@RequestMapping("/")
	@ResponseBody
	public Map execute(Long id) {
		Map map = new HashMap();
		ActionSql actionSql = null;

		if (id != null) {
			actionSql = actionSqlService.get(id);

			if (actionSql.getRunTime() > 0) {
				actionSql.setRunTime(actionSql.getRunTime() / 60);
			}
		}
		map.put("actionSql", actionSql);
		return map;
	}

	@Override
	@RequestMapping("/paging")
	public PaginationSupport paging(@RequestParam("condition") ConditionModel cm, Integer start, Integer limit, @RequestParam(required = false) String sort,
                                    @RequestParam(value = "dir", required = false) String direction) {
		cm.setStart(start);
		cm.setLimit(limit);
		cm.addOrder(sort, direction);

		return actionSqlService.pagingBySql(cm);
	}

	@Override
	public GenericService<ActionSql> getDefaultService() {
		return actionSqlService;
	}

}
