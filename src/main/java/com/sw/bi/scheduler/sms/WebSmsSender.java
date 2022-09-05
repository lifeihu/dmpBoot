package com.sw.bi.scheduler.sms;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;

import com.sw.bi.scheduler.util.Configure;

/**
 * 中国网建的短信接口
 * 
 * @author shiming.hong
 */
// @Component("WebSmsSender")
@Deprecated
public class WebSmsSender implements SmsSender {
	private static final Logger log = Logger.getLogger(WebSmsSender.class);

	@Override
	public boolean sendMsg(String mobile, String content) {
		HttpClient client = new HttpClient();

		PostMethod post = new PostMethod("http://gbk.sms.webchinese.cn/");
		post.addRequestHeader("Content-Type", "application/x-www-form-urlencoded;charset=gbk");
		NameValuePair[] data = new NameValuePair[4];
		data[0] = new NameValuePair("Uid", "kele8boy");
		data[1] = new NameValuePair("Key", Configure.property(Configure.SMS_KEY));
		data[2] = new NameValuePair("smsMob", mobile);
		data[3] = new NameValuePair("smsText", content + "【调度系统】");
		post.setRequestBody(data);

		boolean success = true;
		try {
			client.executeMethod(post);
			Header[] headers = post.getResponseHeaders();
			int statusCode = post.getStatusCode();

			if (log.isDebugEnabled()) {
				log.debug("send status code: " + statusCode);
				for (Header header : headers) {
					log.debug("header: " + header.toString());
				}
			}

			String result = new String(post.getResponseBodyAsString().getBytes("gbk"));
			success = Integer.parseInt(result) >= 0;

		} catch (Exception e) {
			success = false;
			log.error("send sms failed.", e);

		} finally {
			post.releaseConnection();
		}

		if (success) {
			log.info("sms sent successfully to " + mobile);
		}

		return success;
	}

	public static void main(String args[]) {
		WebSmsSender t = new WebSmsSender();
		t.sendMsg("18057190969", "test中文");

	}

}
