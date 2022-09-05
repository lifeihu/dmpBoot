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
 * 根据昨天的分钟任务的运行时长,预算出今天的分钟任务的标准运行时间,以便告警系统以此作为判断依据,来判断分钟任务是否延迟了.
 * 这里以任务插件的形式增加告警逻辑
 * 之前的通用告警程序就不去修改了,如果有特殊的告警点，以任务插件形式动态增加
 * 2012-07-11
 * @author feng.li
 *
 */

//   /home/tools/scheduler/scheduler_test jar   /home/tools/scheduler/scheduler_test.jar com.sw.bi.scheduler.background.javatype.precompute.PreComputeMin
@Component
public class PreComputeMin {
	
	@Autowired
	private JobService jobService;
	
	@Autowired
	private  TaskService taskService;
	
	@Autowired
	private  JobDelayStandardService jobDelayStandardService;
	
	public static void main(String args[]){
		
		PreComputeMin.PreComputeMin().computeMin();
		
	}
	
	
	private static PreComputeMin PreComputeMin() {
		return BeanFactory.getBean(PreComputeMin.class);
	}
	
	
	
	// 根据昨天的小时任务的运行时长,预算出今天的小时任务的标准运行时间,以便告警系统以此作为判断依据,来判断小时任务是否延迟了.
	public void computeMin(){
		
		List<Job> jobs = jobService.getOnlineJobsByCycleType(Configure.JobCycle.MINUTE.indexOf());
		for(Job job:jobs){
			
			List<Task> tasks = taskService.getSuccessTasksOrderByRunTimeDesc(job.getJobId(), DateUtil.getYesterday());
			
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
			int yesterday_time = (int)job_avg_runtime/1000;                //单位秒   //昨天的平均时间
			
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
