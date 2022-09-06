package com.sw.bi.scheduler.controller;

import com.sw.bi.scheduler.model.WaitUpdateStatusTask;
import com.sw.bi.scheduler.service.TaskService;
import com.sw.bi.scheduler.service.WaitUpdateStatusTaskService;
import com.sw.bi.scheduler.util.DateUtil;
import org.apache.commons.beanutils.ConvertUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/manage/waitUpdateStatusTask")
public class WaitUpdateStatusTaskController extends BaseActionController<WaitUpdateStatusTask> {

	@Autowired
	private WaitUpdateStatusTaskService waitUpdateStatusTaskService;

	@Autowired
	private TaskService taskService;

	/**
	 * 校验指定的参考点是否允许删除
	 * 
	 * @param waitUpdateStatusTaskId
	 * @return
	 */
	@RequestMapping("/isAllowRemove")
	@ResponseBody
	public boolean isAllowRemove(long waitUpdateStatusTaskId) {
		return waitUpdateStatusTaskService.isAllowRemove(waitUpdateStatusTaskId);
	}

	/**
	 * 添加参考点
	 * 
	 * @param taskId
	 */
	/*@RequestMapping("/addParentTasks")
	public void addParentTasks(long taskId) {
		Task task = taskService.get(taskId);
		if (task != null) {
			waitUpdateStatusTaskService.addParentTasks(task, DateUtil.getToday());
		}
	}*/

	/**
	 * 添加参考点
	 * 
	 * @param taskId
	 */
	@RequestMapping("/addParentTasks")
	public void addParentTasks(String taskId) {
		if (!StringUtils.hasText(taskId)) {
			return;
		}

		Long[] unsuccessTaskIds = (Long[]) ConvertUtils.convert(taskId.split(","), Long.class);
		waitUpdateStatusTaskService.addParentTasks(taskService.query(unsuccessTaskIds), DateUtil.getToday());
	}

	@Override
	public WaitUpdateStatusTaskService getDefaultService() {
		return waitUpdateStatusTaskService;
	}

}
