package com.sw.bi.scheduler.controller;

import com.sw.bi.scheduler.model.RunningNumberCounterDto;
import com.sw.bi.scheduler.service.StatisticService;
import org.apache.commons.beanutils.ConvertUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Map;

@Controller
@RequestMapping("/manage/statistic")
public class StatisticController {

	@Autowired
	private StatisticService statisticService;

	/**
	 * 统计指定任务日期内各周期的任务数量
	 * 
	 * @param taskDate
	 * @return
	 */
	@RequestMapping("/statisticCycleTypeTaskNumbers")
	@ResponseBody
	public Collection<Map<String, Integer>> statisticCycleTypeTaskNumbers(Date taskDate) {
		return statisticService.statisticCycleTypeTaskNumbers(taskDate);
	}

	/**
	 * 获得指定任务日期范围的任务执行情况
	 * 
	 * @param startTaskDate
	 * @param endTaskDate
	 * @return
	 */
	@RequestMapping("/statisticTaskCompletions")
	@ResponseBody
	public Collection<Map<String, Object>> statisticTaskCompletions(Date startTaskDate, Date endTaskDate) {
		return statisticService.statisticTaskCompletions(startTaskDate, endTaskDate);
	}

	/**
	 * 根据指定运行时长的规则和任务日期获得相应的任务执行的时长情况
	 * 
	 * @param runTimeRange
	 * @param taskDate
	 * @return
	 */
	@RequestMapping("/statisticTaskRunTimes")
	@ResponseBody
	public Collection<Map<String, Object>> statisticTaskRunTimes(String runTimeRange, Date taskDate) {
		String[] ranges = runTimeRange.split(";");
		Collection<Integer[]> runTimeRanges = new LinkedHashSet<Integer[]>();
		for (String token : ranges) {
			Integer[] range = (Integer[]) ConvertUtils.convert(token.split(","), Integer.class);
			runTimeRanges.add(range);
		}

		return statisticService.statisticTaskRunTimes(runTimeRanges, taskDate);
	}

	/**
	 * 获得作业每个周期内各类型的作业数
	 * 
	 * @return
	 */
	@RequestMapping("/statisticJobCycleType")
	@ResponseBody
	public Collection<Map<String, Integer>> statisticJobCycleType() {
		return statisticService.statisticJobCycleType();
	}

	/**
	 * 统计指定日期范围内作业运行数量(图形)
	 * 
	 * @param jobType
	 * @param interval
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	@RequestMapping("/statisticTaskRunningNumber4Chart")
	@ResponseBody
	public Collection<RunningNumberCounterDto> statisticTaskRunningNumber4Chart(String jobType, int interval, Date startTime, Date endTime) {
		return statisticService.statisticTaskRunningNumber4Chart(jobType, interval, startTime, endTime);
	}

	/**
	 * 统计指定日期范围内指定作业的运行时长(图形)
	 * 
	 * @param jobId
	 * @param startDate
	 * @param endDate
	 * @param time
	 * @return
	 */
	@RequestMapping("/statisticTaskRunTime4Chart")
	@ResponseBody
	public Collection<Map<String, Object>> statisticTaskRunTime4Chart(Long jobId, Date startDate, Date endDate, String time) {
		return statisticService.statisticTaskRunTime4Chart(jobId, startDate, endDate, time);
	}
}
