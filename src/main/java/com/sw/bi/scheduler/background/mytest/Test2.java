package com.sw.bi.scheduler.background.mytest;

import java.util.Calendar;
import java.util.Date;

import com.sw.bi.scheduler.model.Job;
import com.sw.bi.scheduler.util.Configure.JobCycle;
import com.sw.bi.scheduler.util.DateUtil;

public class Test2 {
	

	
	
	   public static void main(String args[]){
		    Job job = new Job();
		    job.setCycleType(1l); //month
		    job.setDayN(4l);
		    
			Date today = DateUtil.getToday();
			Calendar todayC = DateUtil.getCalendar(today);
			if(JobCycle.MONTH.indexOf()==job.getCycleType()){
				
				
             
				
				todayC.set(Calendar.MONTH, todayC.get(Calendar.MONTH)-1);  //月份减1
				todayC.set(Calendar.DAY_OF_MONTH, job.getDayN().intValue()); //月几跟数据库中的一样
			}else if(JobCycle.WEEK.indexOf()==job.getCycleType()){
				todayC.set(Calendar.DAY_OF_WEEK, job.getDayN().intValue()); //周几跟数据库中的一样
				todayC.add(Calendar.DATE, -7);  //上周,向前推7天
			}
			today = todayC.getTime();
		    System.out.println(DateUtil.formatDate(today));
		   
		   
	   }
	

	   
	   
	   
	   
	   
	   
	   
	   
	   
	   
	

}
