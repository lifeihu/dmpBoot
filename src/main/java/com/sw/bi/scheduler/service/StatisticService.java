package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.model.RunningNumberCounterDto;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

public interface StatisticService {

	/**
	 * 统计指定任务日期内各状态的任务数量
	 * 
	 * @param taskDate
	 * @return
	 */
	public Collection<Map<String, Integer>> statisticCycleTypeTaskNumbers(Date taskDate);

	/**
	 * 获得指定任务日期范围的任务执行情况
	 * 
	 * @param startTaskDate
	 * @param endTaskDate
	 * @return
	 */
	public Collection<Map<String, Object>> statisticTaskCompletions(Date startTaskDate, Date endTaskDate);

	/**
	 * 根据指定运行时长的规则和任务日期获得相应的任务执行的时长情况
	 * 
	 * @param runTimeRanges
	 * @param taskDate
	 * @return
	 */
	public Collection<Map<String, Object>> statisticTaskRunTimes(Collection<Integer[]> runTimeRanges, Date taskDate);

	/**
	 * 获得作业每个周期内各类型的作业数
	 * 
	 * @return
	 */
	public Collection<Map<String, Integer>> statisticJobCycleType();

	/**
	 * 统计指定日期范围内作业运行数量(图形)
	 * 
	 * @param jobType
	 * @param interval
	 *            分钟间隔
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	public Collection<RunningNumberCounterDto> statisticTaskRunningNumber4Chart(String jobType, int interval, Date startTime, Date endTime);

	/**
	 * 统计指定日期范围内指定作业的运行时长(图形)
	 * 
	 * @param jobId
	 * @param startDate
	 * @param endDate
	 * @param time
	 * @return
	 */
	public Collection<Map<String, Object>> statisticTaskRunTime4Chart(Long jobId, Date startDate, Date endDate, String time);

}
