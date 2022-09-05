package com.sw.bi.scheduler.util;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import com.sw.bi.scheduler.supports.MessageSenderAssistant;

public class MailUtil {
	private static final MessageSenderAssistant messageSender = new MessageSenderAssistant(new String[] { "scheduler.properties" });

	public static void send(String to, String title, String content) {
		messageSender.sendMail(to, title, content);
	}

	/*private static Boolean SESSION_DEBUG = false;

	private static Properties props;
	private static Authenticator authenticator;

	static {
		props = System.getProperties();
		props.put(Configure.MAIL_SMTP_HOST, Configure.property(Configure.MAIL_SMTP_HOST));
		props.put(Configure.MAIL_TRANSPORT_PROTOCOL, Configure.property(Configure.MAIL_TRANSPORT_PROTOCOL));
		props.put(Configure.MAIL_SMTP_AUTH, Configure.property(Configure.MAIL_SMTP_AUTH));

		authenticator = new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(Configure.property(Configure.MAIL_AUTH_USERNAME), Configure.property(Configure.MAIL_AUTH_PASSWORD));
			}
		};

	}*/

	/**
	 * 发送邮件
	 * 
	 * @param to
	 *            多个邮箱以逗号分隔
	 * @param title
	 * @param content
	 * @throws MessagingException
	 * @throws AddressException
	 */
	/*public static void send(String to, String title, String content) throws AddressException, MessagingException {
	if (!StringUtils.hasText(to)) {
		return;
	}

	Session session = Session.getDefaultInstance(props, authenticator);
	session.setDebug(SESSION_DEBUG);

	Message message = new MimeMessage(session);

	message.setFrom(new InternetAddress(Configure.property(Configure.MAIL_FROM)));

	InternetAddress[] toAddress = InternetAddress.parse(to, false);
	message.setRecipients(RecipientType.TO, toAddress);

	message.setSubject(title);
	message.setDataHandler(new DataHandler("testHtml", "text/html"));
	message.setContent(content, "text/html;charset=gb2312");

	Transport.send(message);
	}*/

	public static void main(String args[]) throws AddressException, MessagingException {

		MailUtil.send("shiming.hong@shunwang.com", "调度系统登录验证码", "content.toString()");

	}

}
