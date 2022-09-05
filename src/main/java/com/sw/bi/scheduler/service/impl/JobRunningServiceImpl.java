package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.JobRunning;
import com.sw.bi.scheduler.service.JobRunningService;
import org.hibernate.Query;
import org.springframework.stereotype.Service;

@Service("JobRunningService")
public class JobRunningServiceImpl extends GenericServiceHibernateSupport<JobRunning> implements JobRunningService {

	@Override
	public int getRunningCount() {
		// TODO Auto-generated method stub
	   return	this.get(1l).getRunningCount().intValue();

	}

	@Override
	public void updateRunningCount(int num) {
		// TODO Auto-generated method stub
		String sql = "update job_running SET running_count = running_count + " + num  
		+" where id = 1";
		Query query = this.getCurrentSession().createSQLQuery(sql);
		try
		{
			query.executeUpdate();
		}
		catch(Exception ex)
		{
			
		}
	}

	@Override
	public void setZero() {
		// TODO Auto-generated method stub
		String sql = "update job_running SET running_count = 0  where id = 1";
		Query query = this.getCurrentSession().createSQLQuery(sql);
		try
		{
			query.executeUpdate();
		}
		catch(Exception ex)
		{
			
		}
	}

	
}
