package com.sw.bi.scheduler.background.javatype.genTaskByJobid;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.model.Job;
import com.sw.bi.scheduler.service.JobService;
import com.sw.bi.scheduler.util.DateUtil;

/**
 * 生成指定job_id对应的task任务信息 生成了task信息,还要注意生成参考点信息 2012-07-03
 * 
 * @author feng.li
 * 
 */
//    /home/tools/scheduler/scheduler jar   /home/tools/scheduler/scheduler.jar com.sw.bi.scheduler.background.javatype.genTaskByJobid.GenTaskByJobid 11
@Component
public class GenTaskByJobid {
	private static final Logger log = Logger.getLogger(GenTaskByJobid.class);

	@Autowired
	private JobService jobService;

	public static void main(String args[]) {
		String jobId = args[0];
		GenTaskByJobid.GenTaskByJobid().genTaskByJobid(jobId);

	}

	private static GenTaskByJobid GenTaskByJobid() {
		return BeanFactory.getBean(GenTaskByJobid.class);
	}

	public void genTaskByJobid(String jobId) {
		Collection<Job> jobs = jobService.getOnlineJobsByJobId(new Long[] { Long.valueOf(jobId) });
		if (jobs.size() > 0) {
			jobService.createTasks(jobs, DateUtil.getToday());
		}

	}
}
