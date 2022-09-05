package com.sw.bi.scheduler.background.javatype.check;

import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.ConditionExpression;
import org.springframework.ui.ConditionModel;
import org.springframework.util.StringUtils;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.service.TaskService;
import com.sw.bi.scheduler.util.Configure;
import com.sw.bi.scheduler.util.Configure.TaskStatus;
import com.sw.bi.scheduler.util.DateUtil;

import framework.commons.sender.MessagePlatform;
import com.sw.bi.scheduler.supports.MessageSenderAssistant;

/**
 * 检验上一小时内所有运行成功的作业数量是否大于指定数量,如果小于则需要告警
 * 
 * @author shiming.hong
 * 
 */

//    /home/tools/scheduler/scheduler jar   /home/tools/scheduler/scheduler.jar com.sw.bi.scheduler.background.javatype.check.CheckHourTaskRunSuccessNumber 5
@Component
public class CheckHourTaskRunSuccessNumber {
	private static final Logger log = Logger.getLogger(CheckHourTaskRunSuccessNumber.class);

	@Autowired
	private TaskService taskService;

	/*@Autowired(required = false)
	// @Qualifier("SWSmsSender")
	@Qualifier("WebSmsSender")
	private SmsSender smsSender;*/

	/*@Autowired
	private SmsService smsService;*/
	private MessageSenderAssistant messageSender = new MessageSenderAssistant();

	public static CheckHourTaskRunSuccessNumber getCheckHourTaskRunSuccessCount() {
		return BeanFactory.getBean(CheckHourTaskRunSuccessNumber.class);
	}

	public void check(int planRunSuccessNumber) {
		Calendar calendar = DateUtil.getCalendar(DateUtil.now());

		calendar.add(Calendar.HOUR_OF_DAY, -1);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		Date startTaskDate = calendar.getTime();

		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		Date endTaskDate = calendar.getTime();

		ConditionModel cm = new ConditionModel();
		cm.addCondition("taskEndTime", ConditionExpression.GE, startTaskDate);
		cm.addCondition("taskEndTime", ConditionExpression.LE, endTaskDate);
		cm.addCondition("taskStatus", ConditionExpression.IN, new Long[] { (long) TaskStatus.RUN_SUCCESS.indexOf(), (long) TaskStatus.RE_RUN_SUCCESS.indexOf() });
		int count = taskService.count(cm);

		if (count < planRunSuccessNumber) {
			String mobile = Configure.property(Configure.SMS_MOBILE);
			String message = calendar.get(Calendar.HOUR_OF_DAY) + "点钟的任务只完成 了 " + count + " 个,预设值: " + planRunSuccessNumber + ",请及时处理.";
			// smsService.sendMsg(mobile, message);
//			messageSender.sendSms(mobile, message);
			messageSender.send(MessagePlatform.SMS_ADTIME,mobile, message);
			log.warn("短信告警(" + mobile + ") - " + message);

			System.exit(0);
		} else {
			log.info(calendar.get(Calendar.HOUR_OF_DAY) + "点钟的任务完成情况满足预设值: " + planRunSuccessNumber + ".");
		}
	}

	public static void main(String[] args) {
		int planRunSuccessNumber = StringUtils.hasText(args[0]) ? Integer.parseInt(args[0]) : 5;

		getCheckHourTaskRunSuccessCount().check(planRunSuccessNumber);
	}

}
