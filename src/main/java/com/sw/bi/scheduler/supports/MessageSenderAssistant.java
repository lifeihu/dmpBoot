package com.sw.bi.scheduler.supports;

import com.idogga.dc.sender.MailMessageSenderProxy;
import com.idogga.dc.sender.SmsMessageSenderProxy;
import framework.commons.sender.MessagePlatform;

/**
 * 短信发送辅助类，用来批量替换用的
 * Created by Administrator on 2015/11/27 0027.
 */
public class MessageSenderAssistant {

    public MessageSenderAssistant() {

    }

    public MessageSenderAssistant(String[] pros) {

    }

    public boolean send(MessagePlatform platform, String receiver, String message) {
        try {
            return SmsMessageSenderProxy.getInstance().send(receiver, message);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean sendMail(String email, String title, String message) {
        try {
            return MailMessageSenderProxy.getInstance().send(email, title, message);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
