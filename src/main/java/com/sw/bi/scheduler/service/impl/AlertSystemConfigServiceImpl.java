package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.AlertSystemConfig;
import com.sw.bi.scheduler.model.Job;
import com.sw.bi.scheduler.model.User;
import com.sw.bi.scheduler.service.AlertSystemConfigService;
import com.sw.bi.scheduler.service.JobService;
import com.sw.bi.scheduler.service.UserService;
import com.sw.bi.scheduler.supports.MessageSenderAssistant;
import com.sw.bi.scheduler.util.Configure;
import com.sw.bi.scheduler.util.DateUtil;
import com.sw.bi.scheduler.util.MailUtil;
import framework.commons.sender.MessagePlatform;
import org.apache.commons.beanutils.ConvertUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
public class AlertSystemConfigServiceImpl extends GenericServiceHibernateSupport<AlertSystemConfig> implements AlertSystemConfigService {

	@Autowired
	private UserService userService;

	@Autowired
	private JobService jobService;

	/*@Autowired(required = false)
	// @Qualifier("SWSmsSender")
	@Qualifier("WebSmsSender")
	private SmsSender smsSender;*/

	/*@Autowired
	private SmsService smsService;*/
	private MessageSenderAssistant messageSender = new MessageSenderAssistant();

	@Override
	public void alert(long jobId, String title, String message) {
		alert(jobId, title, message, message);
	}

	@Override
	public void alert(long jobId, String title, String emailMessage, String smsMessage) {
		AlertSystemConfig config = this.get(1l);

		int alertWay = config.getAlertWay();
		int alertTarget = config.getAlertTarget();

		// 获得发送方式(邮件或短信)
		if (alertWay == 0) {
			alertWay = isWorkingTime(config.getBeginWorkTime(), config.getEndWorkTime()) ? 1 : 2;
		}

		if (alertWay == 1) {
			// 邮件
			try {
				MailUtil.send(config.getAlertMaillist(), title, emailMessage);
			} catch (Exception e) {
				log.error("发往[" + config.getAlertMaillist() + "]的邮件失败.");
			}

		} else if (alertWay == 2) {
			// 短信

			// 获得发送对象
			Collection<String> mobiles = new HashSet<String>();

			// 观察人
			String observeMan = config.getObserveMan();
			if (StringUtils.hasText(observeMan)) {
				Long[] observeManIds = (Long[]) ConvertUtils.convert(observeMan.split(","), Long.class);
				Collection<User> observeMans = userService.query(observeManIds);
				for (User user : observeMans) {
					mobiles.add(user.getMobilePhone());
				}
			}

			// 责任人
			if (alertTarget == 0 || alertTarget == 2) {
				Job job = jobService.get(jobId);
				mobiles.add(userService.get(job.getDutyOfficer()).getMobilePhone());
			}

			// 值周人
			if (alertTarget == 1 || alertTarget == 2) {
				mobiles.add(userService.get(config.getDutyMan()).getMobilePhone());
			}

			if (mobiles.size() == 0) {
				mobiles.add(Configure.property(Configure.SMS_MOBILE));
			}

			for (String mobile : mobiles) {
				// smsService.sendMsg(mobile, smsMessage);
//				messageSender.sendSms(mobile, smsMessage);
				messageSender.send(MessagePlatform.SMS_ADTIME,mobile, smsMessage);
				log.warn("短信告警(" + mobile + ") - " + smsMessage);
			}
		}
	}

	@Override
	public AlertSystemConfig createDefault(long userGroupId) {
		AlertSystemConfig config = new AlertSystemConfig();

		config.setUserGroupId(userGroupId);
		config.setBeginWorkTime("09:00");
		config.setEndWorkTime("17:30");
		config.setScanCycle("0-9,15;9-18,15;18-24,15");
		config.setAlertJobRange("0-24,0");
		config.setAlertWay(0);
		config.setAlertTarget(0);
		config.setSmsContent(3);
		config.setPrecomputeForWhichdate(null);
		config.setAlertMaillist(null);
		config.setPrecomputeForWhichdate(new Date());
		config.setAlertMaillist("feng.li@shunwang.com");

		super.saveOrUpdate(config);

		return config;
	}

	@Override
	public void delteByUserGroup(long userGroupId) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("userGroupId", userGroupId));
		criteria.setMaxResults(1);
		AlertSystemConfig config = (AlertSystemConfig) criteria.uniqueResult();

		super.delete(config);
	}

	/**
	 * 校验当前时间是否是工作时间
	 * 
	 * @param workingStart
	 * @param workingEnd
	 * @return
	 */
	private boolean isWorkingTime(String workingStart, String workingEnd) {
		int beginTimeInteger = Integer.parseInt(workingStart.replaceAll(":", ""));
		int endTimeInteger = Integer.parseInt(workingEnd.replaceAll(":", ""));

		Calendar calendar = new GregorianCalendar();
		calendar.setFirstDayOfWeek(Calendar.SUNDAY);
		calendar.setTime(DateUtil.now());

		if (calendar.get(Calendar.DAY_OF_WEEK) == 1 || calendar.get(Calendar.DAY_OF_WEEK) == 7) {
			return false;
		}

		int nowTimeInteger = calendar.get(Calendar.HOUR_OF_DAY) * 100 + calendar.get(Calendar.MINUTE);
		return (nowTimeInteger >= beginTimeInteger && nowTimeInteger <= endTimeInteger);
	}

}
