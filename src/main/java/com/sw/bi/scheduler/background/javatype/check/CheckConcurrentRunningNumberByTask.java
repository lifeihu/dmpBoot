package com.sw.bi.scheduler.background.javatype.check;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.service.TaskService;

import framework.commons.sender.MessagePlatform;
import com.sw.bi.scheduler.supports.MessageSenderAssistant;

/**
 * 检查作业并发运行数量(按Task统计)
 * 
 * @author shiming.hong
 */

//    /home/tools/scheduler/scheduler jar   /home/tools/scheduler/scheduler.jar com.sw.bi.scheduler.background.javatype.check.CheckConcurrentRunningNumberByTask
@Component
public class CheckConcurrentRunningNumberByTask {
	private static final Logger log = Logger.getLogger(CheckConcurrentRunningNumberByTask.class);

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
	 * @param runningNumberThreshold
	 *            运行作业数量阈值
	 * @param beginTimeInterval
	 *            运行时间超过该值的作业才有效
	 * @param tryTimes
	 *            尝试次数
	 * @param tryInterval
	 *            每次尝试的间隔
	 * @param mobiles
	 *            告警手机号
	 * @param excludeJobIds
	 *            排除作业数
	 */
	public void check(final int runningNumberThreshold, final int beginTimeInterval, final int tryTimes, final int tryInterval, final String[] mobiles, final Long[] excludeJobIds) {

		/**
		 * 当前尝试的次数
		 */
		int currentTryTimes = 0;

		/**
		 * 是否连续超出阈值
		 */
		boolean isContinuous = true;

		/**
		 * 最近一次超出阈值的运行数量
		 */
		int runningNumber = 0;

		while (true) {
			try {
				int count = taskService.countTodayRunningTasks(beginTimeInterval, excludeJobIds, false,false);

				String message = "运行第 " + (currentTryTimes + 1) + " 次,查到运行时间超过" + beginTimeInterval + "分钟的正在运行作业 " + count + " 条";

				// 实际运行数量小于阈值
				if (count < runningNumberThreshold) {
					isContinuous = false;
					message += " - 未超出 " + runningNumberThreshold;

				} else {
					runningNumber = count;
					message += " - 超出 " + runningNumberThreshold;
				}

				log.info(message);

				currentTryTimes += 1;
				if (currentTryTimes >= tryTimes) {
					log.info("检查完毕并退出");
					break;
				}

				try {
					log.info("start sleep " + tryInterval + ".");
					Thread.sleep(tryInterval);
				} catch (Exception e) {
					e.printStackTrace();
				}

			} catch (Exception e) {
				log.error("try again.", e);

				isContinuous = false;
				currentTryTimes += 1;

				try {
					Thread.sleep(tryInterval);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}

		// 对连续超出阈值的则需要告警
		if (isContinuous) {
			String message = "发现开始运行时间超过 " + beginTimeInterval + " 分钟的作业共 " + runningNumber + " 条";
			if (mobiles != null && mobiles.length > 0) {
				for (String mobile : mobiles) {
					// smsService.sendMsg(mobile, message);
//					messageSender.sendSms(mobile, message);
					messageSender.send(MessagePlatform.SMS_ADTIME,mobile, message);
					log.warn("短信告警(" + mobile + ") - " + message);
				}
			}
		}

	}

	public static CheckConcurrentRunningNumberByTask getCheckConcurrentRunningNumber() {
		return BeanFactory.getBean(CheckConcurrentRunningNumberByTask.class);
	}

	public static void main(String[] args) {
		if (args.length < 4) {
			throw new IllegalArgumentException("指定的参数不正确.");
		}

		int runningNumberThreshold = Integer.parseInt(args[0]); //并行正在运行的最大任务数量
		int beginTimeInterval = Integer.parseInt(args[1]); //任务开始时间超过args[1]分钟的任务，才被统计进当前正在运行的任务数中，单位分钟
		int tryTimes = Integer.parseInt(args[2]); //检测的次数
		int tryInterval = Integer.parseInt(args[3]); //检测的间隔   单位毫秒

		String[] mobiles = null;
		if (args.length >= 5 && StringUtils.hasText(args[4])) {
			mobiles = args[4].split(","); // 接收的手机号码，必填
		}

		Long[] excludeJobIds = null;
		if (args.length >= 6 && StringUtils.hasText(args[5])) {
			excludeJobIds = (Long[]) ConvertUtils.convert(args[5].split(","), Long.class); //不纳入统计的作业的ID  逗号分隔
		}

		getCheckConcurrentRunningNumber().check(runningNumberThreshold, beginTimeInterval, tryTimes, tryInterval, mobiles, excludeJobIds);
	}

}
