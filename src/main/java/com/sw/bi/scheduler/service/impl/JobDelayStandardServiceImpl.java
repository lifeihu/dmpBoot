package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.JobDelayStandard;
import com.sw.bi.scheduler.service.JobDelayStandardService;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service("jobDelayStandardService")
@SuppressWarnings("unchecked")
public class JobDelayStandardServiceImpl extends GenericServiceHibernateSupport<JobDelayStandard> implements JobDelayStandardService {

	@Override
	public JobDelayStandard getJobDelayStandardByJobid(long job_id, Date taskDate) {
		
		
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("jobId", job_id));
		if (taskDate != null) {
			criteria.add(Restrictions.eq("taskDate", taskDate));
		}
		 
		 
		return (JobDelayStandard)criteria.uniqueResult();
	}

 


	
	
	
}
