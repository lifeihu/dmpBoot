package com.sw.bi.scheduler.background.javatype.check;

import java.io.IOException;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.service.TaskCheckService;
import com.sw.bi.scheduler.service.TaskService;
import com.sw.bi.scheduler.util.Configure.TaskStatus;
import com.sw.bi.scheduler.util.DateUtil;

/**
 * 检测任务已被运行,但Action一直没被创建的异常
 * 
 * @author shiming.hong
 * 
 */
//     /home/tools/scheduler/scheduler_test jar   /home/tools/scheduler/scheduler_test.jar com.sw.bi.scheduler.background.javatype.check.CheckTaskNotFoundAction 30
@Component
@SuppressWarnings("unchecked")
public class CheckTaskNotFoundAction {

	@Autowired
	private TaskCheckService taskCheckService;

	@Autowired
	private TaskService taskService;

	public void check(int interval) throws IOException, InterruptedException {
		Collection<Task> tasks = taskCheckService.getNotFoundActionTasks(interval);
		System.out.println("共发现 " + tasks.size() + " 条异常作业.");

		for (Task task : tasks) {
			long taskStatus = TaskStatus.RUN_FAILURE.indexOf();

			if (task.getTaskStatus() == TaskStatus.RE_RUNNING.indexOf()) {
				taskStatus = TaskStatus.RE_RUN_FAILURE.indexOf();
			} else if (task.getTaskStatus() == TaskStatus.RUNNING.indexOf()) {
				taskStatus = TaskStatus.RUN_FAILURE.indexOf();
			}

			task.setTaskStatus(taskStatus);//task的状态回填            
			task.setTaskEndTime(DateUtil.now());
			task.setLastActionId(null);
			task.setLastActionIdForBreakpoint(null); //给断点续跑使用
			task.setRunTime(0l);
			task.setUpdateTime(DateUtil.now());

			taskService.update(task);
			System.out.println("已修复: " + task);
		}
	}

	//传入一个参数  单位是分钟
	public static void main(String args[]) throws IOException, InterruptedException {
		int interval = 30;
		if (StringUtils.hasText(args[0])) {
			interval = Integer.parseInt(args[0]);
		}

		CheckTaskNotFoundAction.getCheckTaskNotFoundAction().check(interval);

	}

	private static CheckTaskNotFoundAction getCheckTaskNotFoundAction() {
		return BeanFactory.getBean(CheckTaskNotFoundAction.class);
	}
}
