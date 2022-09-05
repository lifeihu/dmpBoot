package com.sw.bi.scheduler.taskcreator;

import org.springframework.stereotype.Component;

import java.util.Calendar;

/**
 * 创建周任务
 * 
 * @author shiming.hong
 */
@Component
public class WeekTaskCreator extends AbstractTaskCreator {

	@Override
	protected void createTasks() {
		//  dayN定义时需要注意星期天是1,星期一是2,以此类推
		long dayN = job.getDayN();
		int week = taskDate.get(Calendar.DAY_OF_WEEK);

		if (dayN == week) {
			addTask(createTask());
		}
	}

}
