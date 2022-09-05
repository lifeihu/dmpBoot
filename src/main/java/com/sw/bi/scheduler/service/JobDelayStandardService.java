package com.sw.bi.scheduler.service;


import com.sw.bi.scheduler.model.JobDelayStandard;

import java.util.Date;


public interface JobDelayStandardService extends GenericService<JobDelayStandard> {

         public JobDelayStandard getJobDelayStandardByJobid(long job_id, Date taskDate);
	    
	
	
	
	
}
