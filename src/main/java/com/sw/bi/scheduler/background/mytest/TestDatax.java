package com.sw.bi.scheduler.background.mytest;

import org.springframework.stereotype.Component;

import com.sw.bi.scheduler.background.util.BeanFactory;

import framework.commons.sender.MessagePlatform;
import com.sw.bi.scheduler.supports.MessageSenderAssistant;

@Component
public class TestDatax {

	/*@Autowired(required = false)
	// @Qualifier("SWSmsSender")
	@Qualifier("WebSmsSender")
	private SmsSender smsSender;*/

	/*@Autowired
	private SmsService smsService;*/
	private MessageSenderAssistant messageSender = new MessageSenderAssistant();

	public static void main(String args[]) throws Exception {
		/*		String sql = "  select a,b,c from tables;";
				sql = sql.trim();
				if(sql.startsWith("select")){
					sql = "select top 500 " + sql.substring(7, sql.length());
				}
				System.out.println(sql);*/

		TestDatax.getTempQuery().query();
	}

	public static TestDatax getTempQuery() {
		return BeanFactory.getBean(TestDatax.class);
	}

	private void query() throws Exception {
		// smsService.sendMsg("13184206500", "333中文");
//		messageSender.sendSms("13184206500", "333中文");
		messageSender.send(MessagePlatform.SMS_ADTIME,"13184206500", "333中文");
		System.out.println("1111111111111111");

	}

}
