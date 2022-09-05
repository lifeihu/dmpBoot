package com.sw.bi.scheduler.sms;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.sw.bi.scheduler.util.Configure;
import com.sw.bi.scheduler.util.Configure.SmsProvider;

// @Service
@Deprecated
public class SmsService {

	@Autowired(required = false)
	@Qualifier("SWSmsSender")
	private SmsSender swSmsSender;

	@Autowired(required = false)
	@Qualifier("WebSmsSender")
	private SmsSender webSmsSender;

	private String provider = Configure.property(Configure.SMS_PROVIDER);

	/**
	 * 即时发送短信
	 * 
	 * @param mobile
	 *            手机号
	 * @param content
	 *            短信内容
	 */
	public boolean sendMsg(final String mobile, final String content) {
		SmsSender sender = this.getSmsSender();

		if (sender == null) {
			return false;
		}

		return sender.sendMsg(mobile, content);
	}

	private SmsSender getSmsSender() {
		if (SmsProvider.SW.value().equals(provider)) {
			return swSmsSender;
		} else if (SmsProvider.WEB.value().equals(provider)) {
			return webSmsSender;
		}

		return null;
	}

}
