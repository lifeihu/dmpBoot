package com.sw.bi.scheduler.sms;

@Deprecated
public interface SmsSender {

	/**
	 * 即时发送短信
	 * 
	 * @param mobile
	 *            手机号
	 * @param content
	 *            短信内容
	 */
	public boolean sendMsg(final String mobile, final String content);

}
