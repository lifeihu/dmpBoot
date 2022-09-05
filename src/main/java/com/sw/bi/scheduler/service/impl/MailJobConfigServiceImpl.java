package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.MailJobConfig;
import com.sw.bi.scheduler.service.MailJobConfigService;
import org.springframework.stereotype.Service;

@Service("mailJobConfigService")
public class MailJobConfigServiceImpl extends GenericServiceHibernateSupport<MailJobConfig> implements MailJobConfigService {

	@Override
	public MailJobConfig getByJobId(Long jobId) {
		// TODO Auto-generated method stub

		return getHibernateTemplate().get(entityClass, jobId);
	}

}
