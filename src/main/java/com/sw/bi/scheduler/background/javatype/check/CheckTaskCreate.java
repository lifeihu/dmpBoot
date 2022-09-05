package com.sw.bi.scheduler.background.javatype.check;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.ConditionExpression;
import org.springframework.ui.ConditionModel;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.service.TaskService;
import com.sw.bi.scheduler.util.Configure;
import com.sw.bi.scheduler.util.DateUtil;

import framework.commons.sender.MessagePlatform;
import com.sw.bi.scheduler.supports.MessageSenderAssistant;

//   /home/tools/scheduler/scheduler jar   /home/tools/scheduler/scheduler.jar com.sw.bi.scheduler.background.javatype.check.CheckTaskCreate
@Component
public class CheckTaskCreate {
	private static final Logger log = Logger.getLogger(CheckTaskCreate.class);

	@Autowired
	private TaskService taskService;

	/*@Autowired(required = false)
	// @Qualifier("SWSmsSender")
	@Qualifier("WebSmsSender")
	private SmsSender smsSender;*/

	/*@Autowired
	private SmsService smsService;*/
	private MessageSenderAssistant messageSender = new MessageSenderAssistant();

	public static CheckTaskCreate getCheckTaskCreate() {
		return BeanFactory.getBean(CheckTaskCreate.class);
	}

	public void check() {
		ConditionModel cm = new ConditionModel();
		cm.addCondition("taskDate", ConditionExpression.EQ, DateUtil.getToday());
		int count = taskService.count(cm);

		// 当天任务没有生成则需要告警
		if (count == 0) {
			String mobile = Configure.property(Configure.SMS_MOBILE);
			String message = DateUtil.formatDate(DateUtil.getToday()) + "的任务没有生成,请及时.";
			// smsService.sendMsg(mobile, message);
//			messageSender.sendSms(mobile, message);
			messageSender.send(MessagePlatform.SMS_ADTIME,mobile, message);
			log.warn("短信告警(" + mobile + ") - " + message);

			System.exit(0);
		} else {
			log.info(DateUtil.formatDate(DateUtil.getToday()) + "共成功任务 " + count + "条.");
		}

	}

	public static void main(String[] args) {
		getCheckTaskCreate().check();
	}

}
