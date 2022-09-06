package com.sw.bi.scheduler.controller;

import com.sw.bi.scheduler.service.WeightService;
import org.apache.commons.beanutils.ConvertUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/manage/weight")
public class WeightController {

	@Autowired
	private WeightService weightService;

	/**
	 * 获得参考点数据或所有已触发任务
	 * 
	 * @param jobIds
	 * @return
	 */
	@RequestMapping("/getReferenceOrTriggeredTasks")
	@ResponseBody
	public Model getReferenceOrTriggeredTasks(String jobId, Model model) {
		Long[] jobIds = null;
		if (StringUtils.hasText(jobId)) {
			jobIds = (Long[]) ConvertUtils.convert(jobId.split(","), Long.class);
		}
		model.addAllAttributes(weightService.getReferenceOrTriggeredTasks(jobIds));

		return model;
	}

	/**
	 * 给指定任务加权,使其能获得更高的执行优先级
	 * 
	 * @param taskId
	 */
	@RequestMapping("/weighting")
	public void weighting(Long taskId) {
		weightService.weighting(taskId);
	}

	/**
	 * 对参考点表中的任务进行加权操作
	 * 
	 * @param waitUpdateStatusTaskId
	 * @param jobId
	 */
	@RequestMapping("/weightingReference")
	public void weightingReference(long waitUpdateStatusTaskId, Long jobId) {
		weightService.weightingReference(waitUpdateStatusTaskId, jobId);
	}

	/**
	 * 对指定任务的Flag2进行加权操作
	 * 
	 * @param taskId
	 */
	@RequestMapping("/weightingTaskFlag2")
	public void weightingTaskFlag2(long taskId) {
		weightService.weightingTaskFlag2(taskId);
	}

	/**
	 * 对指定任务的readyTime进行加权操作
	 * 
	 * @param taskId
	 */
	@RequestMapping("/weightingTaskReadyTime")
	public void weightingTaskReadyTime(long taskId) {
		weightService.weightingTaskReadyTime(taskId);
	}
}
