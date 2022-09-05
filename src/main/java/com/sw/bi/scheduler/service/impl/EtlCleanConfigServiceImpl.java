package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.EtlCleanConfig;
import com.sw.bi.scheduler.service.EtlCleanConfigService;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.resolver.Warning;
import org.springframework.stereotype.Service;

@Service
public class EtlCleanConfigServiceImpl extends GenericServiceHibernateSupport<EtlCleanConfig> implements EtlCleanConfigService {

	@Override
	public void saveOrUpdate(EtlCleanConfig cleanConfig) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("tableName", cleanConfig.getTableName()));
		if (cleanConfig.getEtlCleanConfigId() != null) {
			criteria.add(Restrictions.not(Restrictions.eq("etlCleanConfigId", cleanConfig.getEtlCleanConfigId())));
		}
		EtlCleanConfig cc = (EtlCleanConfig) criteria.uniqueResult();

		if (cc != null) {
			throw new Warning("表名(" + cleanConfig.getTableName() + ")已经存在!");
		}

		super.saveOrUpdate(cleanConfig);
	}

}
