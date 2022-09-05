package com.sw.bi.scheduler.taskcreator;

import org.springframework.stereotype.Component;

import java.util.Calendar;

/**
 * 创建月任务
 * 
 * @author shiming.hong
 */
@Component
public class MonthTaskCreator extends AbstractTaskCreator {

	@Override
	protected void createTasks() {
		long dayN = job.getDayN();
		int month = taskDate.get(Calendar.DAY_OF_MONTH);

		if (dayN == month) {
			addTask(createTask());
		}
	}
}
