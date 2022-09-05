package com.sw.bi.scheduler.taskcreator;

import com.sw.bi.scheduler.util.DateUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Calendar;

/**
 * 创建分钟任务
 * 
 * @author shiming.hong
 */
@Component
public class MinuteTaskCreator extends AbstractTaskCreator {

	@Override
	protected void createTasks() {
		long dayN = job.getDayN();

		Long endTime = null;
		if (StringUtils.hasText(job.getEndTime())) {
			Calendar calendarEndTime = DateUtil.cloneCalendar(taskDate);
			String[] times = job.getEndTime().split(":");
			calendarEndTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(times[0]));
			calendarEndTime.set(Calendar.MINUTE, Integer.parseInt(times[1]));
			calendarEndTime.set(Calendar.SECOND, 0);
			endTime = calendarEndTime.getTimeInMillis();
		}

		Calendar calendar = DateUtil.cloneCalendar(taskDate);

		// 从指定日期的零点开始计算
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		addTask(createTask(calendar));

		boolean start = true;
		int taskDay = calendar.get(Calendar.DATE); //当天的日期

		while (start) {
			calendar.add(Calendar.MINUTE, (int) dayN); // 分钟任务,每次循环增加dayN分钟

			// 作业设置了结束时间则累加的时间不能超过该结束时间
			if (endTime != null && calendar.getTimeInMillis() > endTime.longValue()) {
				start = false;
				continue;
			}

			if (calendar.get(Calendar.DATE) == taskDay) //增加dayN分钟后,如果日期还是与当天的日期一样,则创建对应的任务,否则跳出循环,结束分钟任务的创建
				addTask(createTask(calendar));
			else
				start = false;
		}
	}

}
