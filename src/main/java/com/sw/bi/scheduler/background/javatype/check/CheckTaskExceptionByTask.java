package com.sw.bi.scheduler.background.javatype.check;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;

import org.apache.commons.beanutils.ConvertUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.model.Action;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.service.ActionService;
import com.sw.bi.scheduler.service.TaskService;
import com.sw.bi.scheduler.util.Configure.ActionStatus;
import com.sw.bi.scheduler.util.Configure.TaskStatus;
import com.sw.bi.scheduler.util.DateUtil;

/**
 * 异常记录检测任务,可以设置为分钟级别的任务,定时检测异常记录,并自动修复异常记录. 2012-07-03
 * 这个脚本也有监控的盲点，它是先查询action表，然后找是否有对应的进程。 但是有下面这种情况无法自动修复：
 * 只有task记录，没有action,没有进程。。。
 * 
 * @author feng.li
 * 
 */
//     /home/tools/scheduler/scheduler jar   /home/tools/scheduler/scheduler.jar com.sw.bi.scheduler.background.javatype.check.CheckTaskExceptionByTask interval excludeJobIds
@Component
public class CheckTaskExceptionByTask {

	@Autowired
	private ActionService actionService;

	@Autowired
	private TaskService taskService;

	public static void main(String args[]) throws IOException, InterruptedException {
		int interval = 3;
		if (args.length >= 1 && StringUtils.hasText(args[0])) {
			interval = Integer.parseInt(args[0]);
		}

		Long[] excludeJobIds = null;
		if (args.length >= 2 && StringUtils.hasText(args[1])) {
			excludeJobIds = (Long[]) ConvertUtils.convert(args[1].split(","), Long.class); //不纳入统计的作业的ID  逗号分隔
		}

		CheckTaskExceptionByTask.getCheckTaskExceptionByTask().check(interval, excludeJobIds);

	}

	public void check(int interval, Long[] excludeJobIds) {
		Collection<Task> runningTasks = taskService.getRunningTasks(DateUtil.getToday(), interval, excludeJobIds, false,false);

		for (Task task : runningTasks) {
			Long lastActionId = task.getLastActionId();

			if (lastActionId == null) {
				task.setTaskStatus(TaskStatus.RE_RUN_FAILURE.indexOf());
				task.setTaskEndTime(DateUtil.now());
				task.setUpdateTime(DateUtil.now());
				taskService.update(task);

				System.out.println(task + ", 没有对应的执行实例.");

				continue;
			}

			Action action = actionService.get(lastActionId);
			if (!StringUtils.hasText(actionService.getActionPID(lastActionId))) {
				action.setActionStatus(ActionStatus.RUN_EXCEPTION.indexOf());
				action.setEndTime(new Date()); //把异常的时间也记录一下,方便查找问题
				actionService.saveOrUpdate(action);

				task.setTaskStatus(TaskStatus.RE_RUN_FAILURE.indexOf());
				task.setTaskEndTime(DateUtil.now());
				task.setUpdateTime(DateUtil.now());
				taskService.update(task);

				System.out.println(task + ", 执行实例(" + lastActionId + ")没有对应的进程.");
				continue;
			}
		}
	}

	private static CheckTaskExceptionByTask getCheckTaskExceptionByTask() {
		return BeanFactory.getBean(CheckTaskExceptionByTask.class);
	}
}
