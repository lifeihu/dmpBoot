package com.sw.bi.scheduler.background.javatype.precompute;


import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.model.Job;
import com.sw.bi.scheduler.model.JobDelayStandard;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.service.JobDelayStandardService;
import com.sw.bi.scheduler.service.JobService;
import com.sw.bi.scheduler.service.TaskService;
import com.sw.bi.scheduler.util.Configure;
import com.sw.bi.scheduler.util.DateUtil;

/**
 * 2012-07-20
 * @author feng.li
 *
 */

//   /home/tools/scheduler/scheduler_test jar   /home/tools/scheduler/scheduler_test.jar com.sw.bi.scheduler.background.javatype.precompute.PreComputeMonth
@Component
public class PreComputeMonth {
	
	@Autowired
	private JobService jobService;
	
	@Autowired
	private  TaskService taskService;
	
	@Autowired
	private  JobDelayStandardService jobDelayStandardService;
	
	public static void main(String args[]){
		
		PreComputeMonth.PreComputeMonth().computeMonth();
		
	}
	
	
	private static PreComputeMonth PreComputeMonth() {
		return BeanFactory.getBean(PreComputeMonth.class);
	}
	
	
	
	// 根据昨天的小时任务的运行时长,预算出今天的小时任务的标准运行时间,以便告警系统以此作为判断依据,来判断小时任务是否延迟了.
	public void computeMonth(){
		//本任务定点在每天的0:05分执行
		//查询前一天的任务表中,状态是成功或者重做成功的,并且运行周期是小时的所有任务记录
		//这些task记录根据job_id进行group by,去掉最大的5条后再取平均值以后加300秒.
		//最终得到 job_id  job_delay_standard_time
		//如                11      500
		//        19      700
		//然后插入到当天的job_delay_standard表中....
		//因为任务可能会重跑，所以插入时,要先把对应的job_id的记录删除后再插入
		//针对每一个小时作业,只要在job_delay_standard表中加一条记录即可.
		//只要预算记录加进去,监控主程序就有了判断延迟的依据,对月,周,天,小时,分钟类型的延迟都会告警
		
		List<Job> jobs = jobService.getOnlineJobsByCycleType(Configure.JobCycle.MONTH.indexOf());
		for(Job job:jobs){
			
			List<Task> tasks = taskService.getSuccessTasksOrderByRunTimeDesc(job.getJobId(), null);
			
			if(tasks.size()==0){
				continue;
			}
			
			int i=0;
			//  过滤掉运行时长最大的5条记录后取平均值
			if(tasks.size()>5){
				i=5;
			}
			long total_runtime = 0;
			for(int j=i;j<tasks.size();j++){
				Task task = tasks.get(j);
				total_runtime+=task.getRunTime();
			}
			
			long job_avg_runtime = tasks.size()>5?total_runtime/(tasks.size()-5):total_runtime/tasks.size();

			long job_id = job.getJobId();
			int yesterday_time = (int)job_avg_runtime/1000;                //单位秒   //昨天的平均运行时间
			
			int cankao = (int)job_avg_runtime/1000;
			int job_delay_standard_time = 0;
		
			// 计算今天的标准时长
			if(cankao>240*60){
				job_delay_standard_time = (int)(cankao*1.2);
			}else if(cankao>180*60&&cankao<=240*60){
				job_delay_standard_time = (int)(cankao*1.3);
			}else if(cankao>120*60&&cankao<=180*60){
				job_delay_standard_time = (int)(cankao*1.4);
			}else if(cankao>60*60&&cankao<=120*60){
				job_delay_standard_time = (int)(cankao*1.5);
			}else if(cankao>45*60&&cankao<=60*60){
				job_delay_standard_time = (int)(cankao*1.6);
			}else if(cankao>30*60&&cankao<=45*60){
				job_delay_standard_time = (int)(cankao*1.7);
			}else if(cankao>15*60&&cankao<=30*60){
				job_delay_standard_time = (int)(cankao*2);
			}else if(cankao<=15*60){
				job_delay_standard_time = cankao+900;
			}
			
			 
			//在这里向job_delay_standard表insert一条记录即可.  输出一些打印信息
			
			JobDelayStandard jobDelayStandard = jobDelayStandardService.getJobDelayStandardByJobid(job_id, DateUtil.getToday());
			if(jobDelayStandard==null){
				jobDelayStandard = new JobDelayStandard();
				jobDelayStandard.setCreateTime(new Timestamp(new Date().getTime()));
			}
			jobDelayStandard.setTaskDate(DateUtil.getToday());
			jobDelayStandard.setJobId(job_id);
			jobDelayStandard.setYesterdayRunTime(Long.valueOf(yesterday_time+""));
			jobDelayStandard.setStandardRunTime(Long.valueOf(job_delay_standard_time+""));
			jobDelayStandard.setUpdateTime(new Timestamp(new Date().getTime()));
			jobDelayStandardService.saveOrUpdate(jobDelayStandard);
			
			
		}
		
		
		
		
		
	}
	

		
	

	
	
	

}
