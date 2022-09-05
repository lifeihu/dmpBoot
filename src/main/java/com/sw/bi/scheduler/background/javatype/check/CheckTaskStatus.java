package com.sw.bi.scheduler.background.javatype.check;

import java.util.Arrays;
import java.util.Collection;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.service.AlertSystemConfigService;
import com.sw.bi.scheduler.service.TaskService;
import com.sw.bi.scheduler.util.Configure.JobCycle;
import com.sw.bi.scheduler.util.DateUtil;

//   /home/tools/scheduler/scheduler jar   /home/tools/scheduler/scheduler.jar com.sw.bi.scheduler.background.javatype.check.CheckTaskStatus 222
@Component
public class CheckTaskStatus {
	private static final Logger log = Logger.getLogger(CheckTaskStatus.class);

	@Autowired
	private TaskService taskService;

	@Autowired
	private AlertSystemConfigService alertSystemConfigService;

	public static CheckTaskStatus getCheckTaskStatus() {
		return BeanFactory.getBean(CheckTaskStatus.class);
	}

	/**
	 * 检查指定作业当前天完成情况
	 * 
	 * @param jobId
	 * @throws MessagingException
	 * @throws AddressException
	 */
	public void check(Collection<Long> jobIds) throws AddressException, MessagingException {
		Collection<Task> tasks = taskService.getTasksByJobs(jobIds, DateUtil.getToday());

		if (tasks.size() == 0) {
			return;
		}

		for (Task task : tasks) {
			System.out.println(task + " - 周期: " + JobCycle.valueOf(task.getCycleType()).toString());

			// 只允许天作业
			if (task.getCycleType() != JobCycle.DAY.indexOf()) {
				continue;
			}

			// 任务运行成功则不需要处理
			if (task.isRunSuccess()) {
				continue;
			}

			// 任务运行失败则需要告警

			long jobId = task.getJobId();
			tasks = taskService.getTasksByJob(jobId, DateUtil.getYesterday());
			Task yesterdayTask = null;

			if (tasks.size() > 0) {
				yesterdayTask = tasks.iterator().next();
			}

			String message = task + "未完成";
			if (yesterdayTask != null) {
				message += "(昨天该作业的运行时长: " + yesterdayTask.getRunTime() / 1000 / 60 + " 分钟)";
			}
			message += ",请及时处理.";

			alertSystemConfigService.alert(jobId, "未完成作业告警", message);
		}

		System.exit(0);
	}

	public static void main(String[] args) throws AddressException, MessagingException {
		String jobId = args[0];
		if (StringUtils.hasText(jobId)) {
			Long[] jobIds = (Long[]) ConvertUtils.convert(jobId.split(","), Long.class);
			getCheckTaskStatus().check(Arrays.asList(jobIds));
		}
	}

}
