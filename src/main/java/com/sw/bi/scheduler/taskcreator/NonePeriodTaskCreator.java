package com.sw.bi.scheduler.taskcreator;

import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.util.Configure.AlertType;
import com.sw.bi.scheduler.util.Configure.TaskStatus;
import com.sw.bi.scheduler.util.DateUtil;
import org.springframework.stereotype.Component;

/**
 * 创建无特定周期(待触发)任务
 * 
 * @author shiming.hong
 */
@Component
public class NonePeriodTaskCreator extends AbstractTaskCreator {

	@Override
	protected void createTasks() {
		Task task = createTask();

		task.setAlert((long) AlertType.NOT_ALERT.indexOf());
		task.setTaskStatus((long) TaskStatus.RUN_SUCCESS.indexOf());
		task.setReadyTime(DateUtil.now());
		task.setTaskBeginTime(task.getReadyTime());
		task.setTaskEndTime(task.getReadyTime());
		task.setRunTime(0l);

		addTask(task);
	}

}
