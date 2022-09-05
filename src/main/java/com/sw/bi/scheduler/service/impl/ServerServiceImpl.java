package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.Server;
import com.sw.bi.scheduler.service.ServerService;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.resolver.Warning;
import org.springframework.stereotype.Service;

@Service
public class ServerServiceImpl extends GenericServiceHibernateSupport<Server> implements ServerService {

	@Override
	public void saveOrUpdate(Server server) {
		Criteria criteria = this.createCriteria();
		criteria.add(Restrictions.eq("ip", server.getIp()));
		if (server.getServerId() != null) {
			criteria.add(Restrictions.not(Restrictions.eq("serverId", server.getServerId())));
		}
		if (this.count(criteria) > 0) {
			throw new Warning("服务器(" + server.getIp() + ")已经存在.");
		}

		super.saveOrUpdate(server);
	}

}
