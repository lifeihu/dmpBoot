package com.sw.bi.scheduler.service;
import com.sw.bi.scheduler.model.JobRunning;

public interface JobRunningService extends GenericService<JobRunning> {

	public int getRunningCount();
	
	public void updateRunningCount(int num);
	
	public void setZero();
}
