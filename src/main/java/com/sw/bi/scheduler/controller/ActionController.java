package com.sw.bi.scheduler.controller;

import com.sw.bi.scheduler.model.Action;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.service.ActionService;
import com.sw.bi.scheduler.service.GenericService;
import com.sw.bi.scheduler.service.TaskService;
import com.sw.bi.scheduler.util.OperateAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ConditionModel;
import org.springframework.ui.PaginationSupport;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;

@Controller
@RequestMapping("/manage/action")
public class ActionController extends BaseActionController<Action> {

	@Autowired
	private ActionService actionService;

	@Autowired
	private TaskService taskService;

	@RequestMapping("/log")
	@ResponseBody
	public String log(Long actionId) {
		return actionService.getActionLog(actionId);
	}

	@RequestMapping("/viewPID")
	@ResponseBody
	public String viewPID(Long actionId) {
		return actionService.getActionPID(actionId);
	}

	@RequestMapping("/killPID")
	public void killPID(Long actionId) {
		Task task = taskService.get(actionService.get(actionId).getTaskId());
		taskService.isAuthorizedUserGroup(task, OperateAction.KILL_PID);

		actionService.killActionPID(actionId);
	}

	/**
	 * 删除所有网关机上指定日期的所有正在运行的任务进程
	 * 
	 * @param gateway
	 */
	@RequestMapping("/killGatewayPID")
	public void killGatewayPID(String gateway) {
		actionService.killGatewayPID(gateway);
	}

	@RequestMapping("/lastAction")
	@ResponseBody
	public Long lastAction(Long taskId, Date taskDate) {
		return actionService.getLastActionIdByTask(taskId, taskDate);
	}

	@Override
	@RequestMapping("/paging")
	public PaginationSupport paging(@RequestParam("condition")
                                            ConditionModel cm, Integer start, Integer limit, @RequestParam(required = false)
	String sort, @RequestParam(value = "dir", required = false)
	String direction) {
		cm.setStart(start);
		cm.setLimit(limit);
		cm.addOrder(sort, direction);

		return actionService.pagingBySql(cm);
	}

	@Override
	public GenericService<Action> getDefaultService() {
		return actionService;
	}

}
