package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.ServerShell;
import com.sw.bi.scheduler.service.ServerShellService;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.resolver.Warning;
import org.springframework.stereotype.Service;

@Service
public class ServerShellServiceImpl extends GenericServiceHibernateSupport<ServerShell> implements ServerShellService {

	@Override
	public void saveOrUpdate(ServerShell serverShell) {
		Criteria criteria = this.createCriteria();
		criteria.add(Restrictions.eq("path", serverShell.getPath()));
		if (serverShell.getServerShellId() != null) {
			criteria.add(Restrictions.not(Restrictions.eq("serverShellId", serverShell.getServerShellId())));
		}
		if (this.count(criteria) > 0) {
			throw new Warning("脚本(" + serverShell.getPath() + ")已经存在.");
		}

		super.saveOrUpdate(serverShell);
	}

}
