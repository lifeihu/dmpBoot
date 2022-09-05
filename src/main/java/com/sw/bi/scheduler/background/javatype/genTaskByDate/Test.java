package com.sw.bi.scheduler.background.javatype.genTaskByDate;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sw.bi.scheduler.background.exception.SchedulerException;
import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.service.JobService;
import com.sw.bi.scheduler.service.TaskCreateLogService;
import com.sw.bi.scheduler.util.Configure;
import com.sw.bi.scheduler.util.DateUtil;

/**
 * 生成指定日期对应的task任务信息 生成了task信息,还要注意生成参考点信息 2012-07-09
 * 
 * @author feng.li
 * 
 */
//    /home/tools/scheduler/scheduler jar   /home/tools/scheduler/scheduler.jar com.sw.bi.scheduler.background.javatype.genTaskByDate.GenTaskByDate 2012-07-07
@Component
@Deprecated
public class Test {

	@Autowired
	private JobService jobService;

	@Autowired
	private TaskCreateLogService taskCreateLogService;

	public static void main(String args[]) throws SchedulerException {
		Configure.property(Configure.MAIN_SCHEDULER, true); //need
		String riqi = args[0];
		Date date = DateUtil.parseDate(riqi);
		Test.GenTaskByDate().genTaskByDate(date);

	}

	private static Test GenTaskByDate() {
		return BeanFactory.getBean(Test.class);
	}

	public void genTaskByDate(Date date) throws SchedulerException {
		if (!taskCreateLogService.isTaskCreated(date)) {
			jobService.createTasks(date);
		}
	}
}
