package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.BigDataTask;
import com.sw.bi.scheduler.service.BigDataTaskService;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Service;

@Service
// select distinct job_id from task where task_date = '2013-07-23' and job_type in (select GROUP_CONCAT(job_type) from concurrent where category=1) and run_time > 600000
public class BigDataTaskServiceImpl extends GenericServiceHibernateSupport<BigDataTask> implements BigDataTaskService {

	@Override
	public boolean isBigDataTask(long jobId) {
		Criteria criteria = this.createCriteria();
		criteria.add(Restrictions.eq("jobId", jobId));

		return this.count(criteria) > 0; // jobId % 3 == 0;
	}

}
