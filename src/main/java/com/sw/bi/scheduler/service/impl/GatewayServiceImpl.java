package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.Gateway;
import com.sw.bi.scheduler.service.GatewayService;
import com.sw.bi.scheduler.service.ScheduleSystemStatusService;
import com.sw.bi.scheduler.util.Configure.SchedulerSystemStatus;
import com.sw.bi.scheduler.util.SshUtil;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.resolver.Warning;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;

@Service
@SuppressWarnings("unchecked")
public class GatewayServiceImpl extends GenericServiceHibernateSupport<Gateway> implements GatewayService {

	@Autowired
	private ScheduleSystemStatusService scheduleSystemStatusService;

	@Override
	public Gateway getGateway(String gateway) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("name", gateway));

		return (Gateway) criteria.uniqueResult();
	}

	@Override
	public Collection<Gateway> getActiveGateways() {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("status", 1));

		return criteria.list();
	}

	@Override
	public Gateway getMasterGateway() {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("master", true));
		criteria.add(Restrictions.eq("status", 1));
		criteria.setMaxResults(1);

		return (Gateway) criteria.uniqueResult();
	}

	@Override
	public Collection<Gateway> getGatewaysByJobType(int jobType) {
		Collection<Gateway> gateways = this.queryAll();
//		for (Iterator<Gateway> iter = gateways.iterator(); iter.hasNext();) {
//			Gateway gateway = iter.next();
//			Collection<Integer> jobTypes = Arrays.asList((Integer[]) ConvertUtils.convert(gateway.getJobType().split(","), Integer.class));
//			if (!jobTypes.contains(jobType)) {
//				iter.remove();
//			}
//		}

		return gateways;
	}

	/*@Override
	public boolean isMasterGateway(String gateway) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("name", gateway));
		criteria.add(Restrictions.eq("master", true));
		criteria.setProjection(Projections.rowCount());
		Integer count = (Integer) criteria.uniqueResult();

		return count == null ? false : count.intValue() > 0;
	}*/

	@Override
	public String getHiveJDBC() {
		// 获得最近修改过的HiveJDBC连接
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.isNotNull("hiveJdbc"));
		criteria.add(Restrictions.sqlRestriction("length(hive_jdbc) > 0"));
		criteria.setMaxResults(1);
		criteria.setProjection(Projections.property("hiveJdbc"));
		criteria.addOrder(Order.desc("updateTime"));

		return (String) criteria.uniqueResult();
	}

	@Override
	public void saveOrUpdate(Gateway gateway) {
		Criteria criteria = createCriteria();
		criteria.setProjection(Projections.rowCount());
		criteria.add(Restrictions.eq("ip", gateway.getIp()));
		if (gateway.getGatewayId() != null) {
			criteria.add(Restrictions.not(Restrictions.eq("gatewayId", gateway.getGatewayId())));
		}
		Integer count = (Integer) criteria.uniqueResult();
		if (count != null && count.intValue() > 0) {
			throw new Warning("网关机IP(" + gateway.getIp() + ")已经存在.");
		}

		criteria = createCriteria();
		criteria.setProjection(Projections.rowCount());
		criteria.add(Restrictions.eq("name", gateway.getName()));
		if (gateway.getGatewayId() != null) {
			criteria.add(Restrictions.not(Restrictions.eq("gatewayId", gateway.getGatewayId())));
		}
		count = (Integer) criteria.uniqueResult();
		if (count != null && count.intValue() > 0) {
			throw new Warning("网关机名称(" + gateway.getName() + ")已经存在.");
		}

		if (gateway.isMaster()) {
			criteria = createCriteria();
			criteria.setProjection(Projections.rowCount());
			criteria.add(Restrictions.eq("master", true));
			if (gateway.getGatewayId() != null) {
				criteria.add(Restrictions.not(Restrictions.eq("gatewayId", gateway.getGatewayId())));
			}
			count = (Integer) criteria.uniqueResult();
			if (count != null && count.intValue() > 0) {
				throw new Warning("已经存在主网关机.");
			}
		}

		if (gateway.getGatewayId() != null) {
			SshUtil.unregisterAgent(gateway.getName());
		}

		super.saveOrUpdate(gateway);
		super.flush();

		scheduleSystemStatusService.update(gateway);
		SshUtil.registerAgent(gateway.getName(), gateway.getIp(), gateway.getPort());
	}

	@Override
	public void delete(Gateway gateway) {
		super.delete(gateway);

		scheduleSystemStatusService.remove(gateway);
		SshUtil.unregisterAgent(gateway.getName());
	}

	@Override
	public Collection<String> getGatewayNames() {
		Criteria criteria = this.createCriteria();
		criteria.add(Restrictions.eq("status", SchedulerSystemStatus.OPEN.indexOf()));
		criteria.addOrder(Order.asc("gatewayId"));
		criteria.setProjection(Projections.property("name"));

		return criteria.list();
	}

	@Override
	public void removeJobFromWhiteList(String gatewayName, long jobId) {
		if (!StringUtils.hasText(gatewayName)) {
			return;
		}

		Gateway gateway = this.getGateway(gatewayName);

		if (gateway == null) {
			return;
		}

		if (!gateway.isUseWhiteList()) {
			return;
		}

		if (!StringUtils.hasText(gateway.getJobWhiteList())) {
			return;
		}

		StringBuilder newJobIds = new StringBuilder();
		String[] jobIds = gateway.getJobWhiteList().split(",");
		for (String id : jobIds) {
			if (Long.parseLong(id) == jobId) {
				continue;
			}

			if (newJobIds.length() > 0) {
				newJobIds.append(",");
			}
			newJobIds.append(id);
		}

		gateway.setJobWhiteList(newJobIds.toString());
		this.update(gateway);
	}

	@Override
	public void addJobToWhiteList(String gatewayName, long jobId) {
		if (!StringUtils.hasText(gatewayName)) {
			return;
		}

		Gateway gateway = this.getGateway(gatewayName);

		if (gateway == null) {
			return;
		}

		if (!gateway.isUseWhiteList()) {
			return;
		}

		StringBuilder newJobIds = new StringBuilder();

		if (!StringUtils.hasText(gateway.getJobWhiteList())) {
			newJobIds.append(jobId);

		} else {
			String[] jobIds = gateway.getJobWhiteList().split(",");
			for (String id : jobIds) {
				if (Long.parseLong(id) == jobId) {
					continue;
				}

				if (newJobIds.length() > 0) {
					newJobIds.append(",");
				}
				newJobIds.append(id);
			}

			if (newJobIds.length() > 0) {
				newJobIds.append(",");
			}
			newJobIds.append(jobId);
		}

		gateway.setJobWhiteList(newJobIds.toString());
		this.update(gateway);
	}

	@Override
	public void updateSchedulerWay(int schedulerWay) {
		Collection<Gateway> gateways = this.queryAll();
		for (Gateway gateway : gateways) {
			gateway.setSchedulerWay(schedulerWay);
			this.update(gateway);
		}
	}

	@Override
	public void updateRoundWay(int roundWay) {
		Collection<Gateway> gateways = this.queryAll();
		for (Gateway gateway : gateways) {
			gateway.setRoundWay(roundWay);
			this.update(gateway);
		}
	}



}
