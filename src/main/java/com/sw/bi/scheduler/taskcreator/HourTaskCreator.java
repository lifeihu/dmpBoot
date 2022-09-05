package com.sw.bi.scheduler.taskcreator;

import com.sw.bi.scheduler.util.DateUtil;
import org.springframework.stereotype.Component;

import java.util.Calendar;

/**
 * 创建小时任务
 * 
 * @author shiming.hong
 */
@Component
public class HourTaskCreator extends AbstractTaskCreator {

	@Override
	@SuppressWarnings("static-access")
	protected void createTasks() {
		Calendar calendar = DateUtil.cloneCalendar(taskDate);

		calendar.set(calendar.HOUR_OF_DAY, 0);

		for (int i = 0; i <= 23; i++) {
			calendar.add(Calendar.HOUR_OF_DAY, 1);//每循环一次,小时点加1
			addTask(createTask(calendar));
		}
	}
}
