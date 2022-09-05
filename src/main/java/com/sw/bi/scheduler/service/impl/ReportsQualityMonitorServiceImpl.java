package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.ReportsQualityMonitor;
import com.sw.bi.scheduler.service.ReportsQualityMonitorService;
import org.springframework.stereotype.Service;

@Service("reportsQualityMonitorService")
public class ReportsQualityMonitorServiceImpl extends GenericServiceHibernateSupport<ReportsQualityMonitor> implements ReportsQualityMonitorService {

	@Override
	public ReportsQualityMonitor getByJobId(Long jobId) {
		// TODO Auto-generated method stub
		return getHibernateTemplate().get(entityClass, jobId);
	}
	
}
