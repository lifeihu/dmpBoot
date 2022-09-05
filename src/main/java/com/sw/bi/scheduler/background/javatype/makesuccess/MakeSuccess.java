package com.sw.bi.scheduler.background.javatype.makesuccess;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.service.TaskService;
import com.sw.bi.scheduler.util.Configure.TaskStatus;
import com.sw.bi.scheduler.util.DateUtil;
import com.sw.bi.scheduler.util.MailUtil;

/**
 * 每天晚上4点运行,将前一天作业ID是11的所有失败的任务状态修改为成功,以便让后续天任务可以顺利运行下去. 同时需要记录修改日志并发送邮件通知
 * 
 * 2012-07-24
 * 
 * @author feng.li
 * 
 */
//    /home/tools/scheduler/scheduler jar   /home/tools/scheduler/scheduler.jar com.sw.bi.scheduler.background.javatype.makesuccess.MakeSuccess ${date_desc}
@Component
public class MakeSuccess {

	@Autowired
	private TaskService taskService;

	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

	public static void main(String args[]) throws AddressException, MessagingException, ParseException {
		String riqi = args[0]; //格式是yyyyMMdd
		Date date = DATE_FORMAT.parse(riqi);
		MakeSuccess.makesuccess().makesuccess(date);

	}

	private static MakeSuccess makesuccess() {
		return BeanFactory.getBean(MakeSuccess.class);
	}

	public void makesuccess(Date date) throws AddressException, MessagingException {

		StringBuffer sb = new StringBuffer();
		sb.append("<table border=1 width=90%>");
		sb.append("<tr style=\"font-weight:bold\"><td>针对5分钟任务的自动修复结果如下:</td></tr>");
		sb.append("<tr style=\"font-weight:bold\"><td>以下任务ID的任务运行失败,并已自动修复为运行成功</td></tr>");

		List<Task> tasks = taskService.getFailedTasks(11, date);
		for (Task task : tasks) {
			task.setTaskStatus(TaskStatus.RE_RUN_SUCCESS.indexOf());
			taskService.saveOrUpdate(task);

			String message = "检测到任务ID: " + task.getTaskId() + "(" + DateUtil.formatDateTime(task.getSettingTime()) + ")运行失败,修复成功.";
			System.out.println(message);
			sb.append("<tr style=\"font-weight:bold\"><td>" + task.getTaskId() + "(" + DateUtil.formatDateTime(task.getSettingTime()) + ")</td></tr>");

		}
		if (tasks.size() > 0) {
			sb.append("</table>");
			System.out.println("开始发送通知邮件");
			MailUtil.send("jin.dai@shunwang.com,feng.li@shunwang.com,r.gan@shunwang.com", "5分钟任务自动修复为运行成功!修复清单见邮件!", sb.toString());
			System.out.println("通知邮件发送完毕");
		} else {
			System.out.println("昨天作业ID是11的所有分钟任务都运行成功,无需修改!");
		}
	}
}
