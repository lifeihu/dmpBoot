package com.sw.bi.scheduler.background.javatype.check;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.model.AlertSystemConfig;
import com.sw.bi.scheduler.model.User;
import com.sw.bi.scheduler.service.AlertSystemConfigService;
import com.sw.bi.scheduler.service.TaskService;
import com.sw.bi.scheduler.service.UserService;
import com.sw.bi.scheduler.util.Configure;

import framework.commons.sender.MessagePlatform;
import com.sw.bi.scheduler.supports.MessageSenderAssistant;

/**
 * 检查某些优先级的天、周、月任务在该任务配置时间点时是否有未运行成功
 * 
 * @author shiming.hong
 * 
 */
//  /home/tools/scheduler/scheduler jar   /home/tools/scheduler/scheduler.jar com.sw.bi.scheduler.background.javatype.check.CheckTaskNotRunSuccessByJobLevel 3
@Component
public class CheckTaskNotRunSuccessByJobLevel {
	private static final Logger log = Logger.getLogger(CheckTaskNotRunSuccessByJobLevel.class);

	@Autowired
	private TaskService taskService;

	@Autowired
	private UserService userService;

	@Autowired
	private AlertSystemConfigService alertSystemConfigService;

	/*@Autowired(required = false)
	// @Qualifier("SWSmsSender")
	@Qualifier("WebSmsSender")
	private SmsSender smsSender;*/

	/*@Autowired
	private SmsService smsService;*/
	private MessageSenderAssistant messageSender = new MessageSenderAssistant();

	public static CheckTaskNotRunSuccessByJobLevel getCheckTaskNotRunSuccessByJobLevel() {
		return BeanFactory.getBean(CheckTaskNotRunSuccessByJobLevel.class);
	}

	public void check(long jobLevel, Collection<String> mobiles, Integer interval) {
		if (mobiles == null) {
			mobiles = new HashSet<String>();
		}

		/*Integer[] jobCycle = new Integer[] { JobCycle.DAY.indexOf(), JobCycle.WEEK.indexOf(), JobCycle.MONTH.indexOf() };
		Long[] taskStatus = new Long[] { (long) TaskStatus.RUN_SUCCESS.indexOf(), (long) TaskStatus.RE_RUN_SUCCESS.indexOf() };

		ConditionModel cm = new ConditionModel();
		cm.addCondition("taskDate", ConditionExpression.EQ, DateUtil.getToday());
		cm.addCondition("jobLevel", ConditionExpression.GE, jobLevel);
		cm.addCondition("cycleType", ConditionExpression.IN, jobCycle);
		cm.addCondition("taskStatus", ConditionExpression.NIN, taskStatus);

		Criteria criteria = taskService.createCriteria(cm);
		if (delay != null && delay.intValue() > 0) {
			criteria.add(Restrictions.sqlRestriction("adddate(setting_time, interval " + delay.intValue() + " minute) <= now()"));
		}*/

		int count = taskService.countNotRunSuccessTasksByJobLevel(jobLevel, interval);

		if (count > 0) {
			AlertSystemConfig config = alertSystemConfigService.get(1l);

			Long dutyMan = config.getDutyMan();
			if (dutyMan != null) {
				mobiles.add(userService.get(dutyMan).getMobilePhone());
			}

			// 观察人
			String observeMan = config.getObserveMan();
			if (StringUtils.hasText(observeMan)) {
				Long[] observeManIds = (Long[]) ConvertUtils.convert(observeMan.split(","), Long.class);
				Collection<User> observeMans = userService.query(observeManIds);
				for (User user : observeMans) {
					mobiles.add(user.getMobilePhone());
				}
			}

			if (mobiles.size() == 0) {
				mobiles.add(Configure.property(Configure.SMS_MOBILE));
			}

			String message = "当前还有 " + count + " 个重要任务未完成,请及时处理.";
			for (String mobile : mobiles) {
				// smsService.sendMsg(mobile, message);
//				messageSender.sendSms(mobile, message);
				messageSender.send(MessagePlatform.SMS_ADTIME,mobile, message);
				log.warn("短信告警(" + mobile + ") - " + message);
			}

			System.exit(0);

		} else {
			log.info("当前未完成任务 " + count + " 个.");
		}
	}

	public static void main(String[] args) {
		long jobLevel = Long.parseLong(args[0]);
		Collection<String> mobiles = new HashSet<String>();
		Integer interval = null;

		if (args.length > 1) {
			if (StringUtils.hasText(args[1])) {
				for (String mobile : args[1].split(",")) {
					mobiles.add(mobile.trim());
				}
			}

			if (args.length == 3) {
				if (StringUtils.hasText(args[2])) {
					interval = Integer.valueOf(args[2]);
				}
			}
		}

		getCheckTaskNotRunSuccessByJobLevel().check(jobLevel, mobiles, interval);
	}

}
