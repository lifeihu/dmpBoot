package com.sw.bi.scheduler.background.javatype.check;

import java.util.Collection;
import java.util.Date;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.service.ActionService;
import com.sw.bi.scheduler.service.TaskService;
import com.sw.bi.scheduler.util.Configure;
import com.sw.bi.scheduler.util.DateUtil;

import framework.commons.sender.MessagePlatform;
import com.sw.bi.scheduler.supports.MessageSenderAssistant;

/**
 * 检查正在运行的超时作业并修改为运行失败 一般用来检查昨天的任务，状态是正在运行的，并且运行时长超过了一定的阀值
 * 
 * @author shiming.hong
 */

//    /home/tools/scheduler/scheduler jar   /home/tools/scheduler/scheduler.jar com.sw.bi.scheduler.background.javatype.check.CheckAndFixedRunningTask
@Component
public class CheckAndFixedRunningTask {
	private static final Logger log = Logger.getLogger(CheckAndFixedRunningTask.class);

	@Autowired
	private ActionService actionService;

	@Autowired
	private TaskService taskService;

	/*@Autowired(required = false)
	// @Qualifier("SWSmsSender")
	@Qualifier("WebSmsSender")
	private SmsSender smsSender;*/

	/*@Autowired
	private SmsService smsService;*/
	private MessageSenderAssistant messageSender = new MessageSenderAssistant();

	/**
	 * 检查并修改异常作业
	 * 
	 * @param taskDate
	 *            任务日期
	 * @param interval
	 *            进程卡住的时长
	 * @param isAutoFixed
	 *            是否自动修复
	 * @param mobiles
	 *            告警手机
	 */
	public void checkAndFixed(Date taskDate, int interval, boolean isAutoFixed, String[] mobiles) {
		Collection<Task> tasks = taskService.getRunningTasks(taskDate, interval, null, false,false);

		int runningCount = tasks.size();
		int fixedCount = 0;

		for (Task task : tasks) {
			Long lastActionId = task.getLastActionId();

			// 作业没有ActionID则认为是异常需要被修改的作业
			boolean isException = (lastActionId == null);

			if (!isException) {
				// 根据ActionID没有查到实际的进程号则也认为是异常作业
				isException = !StringUtils.hasText(actionService.getActionPID(lastActionId));
			}

			String message = task.toString();

			if (isException && isAutoFixed) {
				if (task.getTaskStatus() == Configure.TaskStatus.RE_RUNNING.indexOf()) {
					task.setTaskStatus(Configure.TaskStatus.RE_RUN_FAILURE.indexOf());
				} else {
					task.setTaskStatus(Configure.TaskStatus.RUN_FAILURE.indexOf());
				}

				task.setTaskEndTime(DateUtil.now());
				task.setUpdateTime(DateUtil.now());

				Date beginTime = task.getTaskBeginTime();
				Date endTime = task.getTaskEndTime();
				if (beginTime != null && endTime != null) {
					task.setRunTime(endTime.getTime() - beginTime.getTime());
				}

				taskService.update(task);
				fixedCount += 1;

				message += " - 已修复";
			}

			log.info(message);
		}

		if (runningCount > 0) {
			String message = "共发现开始运行时间超过 " + interval + " 分钟的正在运行作业 " + tasks.size() + " 条";
			if (isAutoFixed) {
				message += ", 自动修复 " + fixedCount + " 条";
			}

			if (mobiles != null && mobiles.length > 0) {
				for (String mobile : mobiles) {
					// smsService.sendMsg(mobile, message);
//					messageSender.sendSms(mobile, message);
					messageSender.send(MessagePlatform.SMS_ADTIME,mobile, message);
					log.warn("短信告警(" + mobile + ") - " + message);
				}
			}
		} else {
			log.info("本次未发现运行时长异常的running job");
		}
	}

	public static CheckAndFixedRunningTask getCheckAndFixedRunningTask() {
		return BeanFactory.getBean(CheckAndFixedRunningTask.class);
	}

	public static void main(String[] args) {
		if (args.length < 2) {
			throw new IllegalArgumentException("参数指定不正确");
		}

		Date taskDate = DateUtil.parseDate(args[0]); // yyyy-MM-dd  查询task_date等于传入日期的记录
		int interval = Integer.parseInt(args[1]); // 正在运行的任务，已经运行的时长，单位分钟
		boolean isAutoFixed = "1".equals(args[2]); // 是否恢复。    0，只检查，不恢复。 1是检查后恢复。

		String[] mobiles = null;
		if (args.length >= 4 && StringUtils.hasText(args[3])) {
			mobiles = args[3].split(","); // 短信告警接收的手机，支持逗号分隔
		}

		getCheckAndFixedRunningTask().checkAndFixed(taskDate, interval, isAutoFixed, mobiles);
	}
}
