package com.sw.bi.scheduler.background.javatype.check;

import com.sw.bi.scheduler.util.PropertiesUtil;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.service.TaskService;
import com.sw.bi.scheduler.util.DateUtil;

import framework.commons.sender.MessagePlatform;
import com.sw.bi.scheduler.supports.MessageSenderAssistant;

/**
 * 校验昨天的任务是否都已经完成
 * 
 * @author shiming.hong
 */

//    /home/tools/scheduler/scheduler jar   /home/tools/scheduler/scheduler.jar com.sw.bi.scheduler.background.javatype.check.CheckTaskNotRunSuccessByYesterday

@Component
public class CheckTaskNotRunSuccessByYesterday {
	private static final Logger log = Logger.getLogger(CheckTaskNotRunSuccessByJobLevel.class);

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
	 * @param mobiles
	 */
	public void check(String[] mobiles) {
		int count = taskService.countNotRunSuccessTasksByYesterday();

		if (count > 0) {
			String message = DateUtil.formatDate(DateUtil.getYesterday()) + "仍有 " + count + " 个作业未运行成功.";
			for (String mobile : mobiles) {
				// smsService.sendMsg(mobile, message);
				//messageSender.sendSms(mobile, message);
				messageSender.send(MessagePlatform.SMS_ADTIME, mobile, message+ PropertiesUtil.getProperty("sender.sms.signature"));
				log.warn("短信告警(" + mobile + ") - " + message);
			}
		}

		log.info("查询昨天未运行成功的作业 " + count + " 条");
	}

	public static CheckTaskNotRunSuccessByYesterday getCheckTaskNotRunSuccessByYesterday() {
		return BeanFactory.getBean(CheckTaskNotRunSuccessByYesterday.class);
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			throw new IllegalArgumentException("未指定告警手机号");
		}
		//  args[0] 是告警接收的手机号码
		getCheckTaskNotRunSuccessByYesterday().check(args[0].split(","));
	}
}
