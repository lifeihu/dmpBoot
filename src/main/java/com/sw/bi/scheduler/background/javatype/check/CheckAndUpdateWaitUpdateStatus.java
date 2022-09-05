package com.sw.bi.scheduler.background.javatype.check;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.service.WaitUpdateStatusTaskService;

import framework.commons.sender.MessagePlatform;
import com.sw.bi.scheduler.supports.MessageSenderAssistant;

/**
 * 定时启用/禁用参考点作业
 * 
 * @author shiming.hong
 * 
 */

//     /home/tools/scheduler/scheduler jar   /home/tools/scheduler/scheduler.jar com.sw.bi.scheduler.background.javatype.check.CheckAndUpdateWaitUpdateStatus

@Component
public class CheckAndUpdateWaitUpdateStatus {
	private static final Logger log = Logger.getLogger(CheckAndUpdateWaitUpdateStatus.class);

	@Autowired
	private WaitUpdateStatusTaskService waitUpdateStatusTaskService;

	/*@Autowired(required = false)
	// @Qualifier("SWSmsSender")
	@Qualifier("WebSmsSender")
	private SmsSender smsSender;*/

	/*@Autowired
	private SmsService smsService;*/
	private MessageSenderAssistant messageSender = new MessageSenderAssistant();

	/**
	 * @param active
	 *            启用/禁用
	 * @param exceptionTryTimes
	 *            状态修改异常时重试次数
	 * @param mobiles
	 *            重试指定次数后仍未成功能需要告警的手机
	 */
	public void check(final boolean active, final int exceptionTryTimes, final String[] mobiles) {
		int tryTimes = 0;

		while (tryTimes < exceptionTryTimes) {
			log.info("try times: " + tryTimes);

			try {
				if (active) {
					waitUpdateStatusTaskService.activeWaitUpdateStatusTask();
				} else {
					waitUpdateStatusTaskService.unactiveWaitUpdateStatusTask();
				}

				return;
			} catch (Exception e) {
				try {
					log.error("try again.", e);
					Thread.sleep(1000 * 30); // 间隔半分钟后再尝试

				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}

				tryTimes += 1;
				continue;
			}

		}

		// 如果异常重试次数超过指定次数则需要告警
		if (mobiles != null && mobiles.length > 0) {
			String message = (active ? "启用" : "禁用") + "参考点作业失败,已经尝试修改 " + tryTimes + " 次.";
			for (String mobile : mobiles) {
				// smsService.sendMsg(mobile, message);
//				messageSender.sendSms(mobile, message);
				messageSender.send(MessagePlatform.SMS_ADTIME,mobile, message);
				log.warn("短信告警(" + mobile + ") - " + message);
			}
		}
	}

	public static CheckAndUpdateWaitUpdateStatus getCheckAndUpdateWaitUpdateStatus() {
		return BeanFactory.getBean(CheckAndUpdateWaitUpdateStatus.class);
	}

	public static void main(String[] args) {

		//  args[0]   0 禁用补数据的参考点    1启用补数据的参考点

		if (args.length == 0) {
			throw new IllegalArgumentException("未指定参数.");
		}

		int exceptionTryTimes = 3;
		if (args.length >= 2 && StringUtils.hasText(args[1])) {
			exceptionTryTimes = Integer.parseInt(args[1]); //  当更新参考点表时，碰到表锁，自动尝试的次数。当超过这个尝试次数，依然失败时，会有短信告警
		}

		String[] mobiles = null;
		if (args.length >= 3 && StringUtils.hasText(args[2])) {
			mobiles = args[2].split(","); // 短信告警的接收手机
		}

		getCheckAndUpdateWaitUpdateStatus().check("1".equals(args[0]), exceptionTryTimes, mobiles);
	}

}
