package com.sw.bi.scheduler.taskcreator;

import org.springframework.stereotype.Component;

/**
 * 创建天任务
 * 
 * @author shiming.hong
 */
@Component
public class DayTaskCreator extends AbstractTaskCreator {

	@Override
	protected void createTasks() {
		addTask(createTask());
	}

}
