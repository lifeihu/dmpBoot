package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.background.exception.GatewayNotFoundException;
import com.sw.bi.scheduler.model.Gateway;
import com.sw.bi.scheduler.service.GatewayService;
import com.sw.bi.scheduler.service.ScheduleSystemStatusService;
import com.sw.bi.scheduler.util.Configure.ReferJobLevel;
import com.sw.bi.scheduler.util.Configure.SchedulerSystemStatus;
import com.sw.bi.scheduler.util.Configure.SchedulerWay;
import org.apache.commons.beanutils.ConvertUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service("scheduleSystemStatus")
public class ScheduleSystemStatusServiceImpl /*extends GenericServiceHibernateSupport<ScheduleSystemStatus>*/implements ScheduleSystemStatusService {

	/*private ScheduleSystemStatus sss;
	private Map<String, ScheduleSystemStatus> ssss = new HashMap<String, ScheduleSystemStatus>();*/
	private Gateway gateway;
	private Map<String, Gateway> gateways = new HashMap<String, Gateway>();

	@Autowired
	private GatewayService gatewayService;

	/*@Override
	public void open(String gateway) throws GatewayNotFoundExecption {
		sss = get(gateway);

		if (sss.getStatus() == SchedulerSystemStatus.CLOSE.indexOf()) {
			sss.setStatus(SchedulerSystemStatus.OPEN.indexOf());
			gatewayService.save(sss);
		}
	}

	@Override
	public void close(String gateway) throws GatewayNotFoundExecption {
		sss = get(gateway);

		if (sss.getStatus() == SchedulerSystemStatus.OPEN.indexOf()) {
			sss.setStatus(SchedulerSystemStatus.CLOSE.indexOf());
			gatewayService.save(sss);
		}
	}

	@Override
	public void openReferJobLevel(String gateway) {
		sss = get(gateway);

		if (sss.getReferToJobLevel() == ReferJobLevel.NO.indexOf()) {
			sss.setReferToJobLevel(ReferJobLevel.YES.indexOf());
			update(sss);
		}
	}

	@Override
	public void closeReferJobLevel(String gateway) {
		sss = get(gateway);

		if (sss.getReferToJobLevel() == ReferJobLevel.YES.indexOf()) {
			sss.setReferToJobLevel(ReferJobLevel.NO.indexOf());
			update(sss);
		}
	}

	@Override
	public void openReferPointRandom(String gateway) {
		sss = get(gateway);

		if (sss.getReferPointRandom() == ReferPointRandom.NO.indexOf()) {
			sss.setReferPointRandom(ReferPointRandom.YES.indexOf());
			update(sss);
		}

	}

	@Override
	public void closeReferPointRandom(String gateway) {
		sss = get(gateway);

		if (sss.getReferPointRandom() == ReferPointRandom.YES.indexOf()) {
			sss.setReferPointRandom(ReferPointRandom.NO.indexOf());
			update(sss);
		}
	}

	@Override
	public void setTaskRunningPriority(String gateway, TaskRunningPriority taskRunningPriority) {
		sss = get(gateway);

		int ordinal = taskRunningPriority.indexOf();
		if (ordinal != sss.getTaskRunningPriority()) {
			sss.setTaskRunningPriority(ordinal);
			update(sss);
		}
	}

	@Override
	public void setTaskFailReturnTimes(String gateway, int taskFailReturnTimes) {
		sss = get(gateway);

		if (sss.getTaskFailReturnTimes() != taskFailReturnTimes) {
			sss.setTaskFailReturnTimes(taskFailReturnTimes);
			update(sss);
		}
	}*/

	@Override
	public boolean isMasterGateway(String gateway) throws GatewayNotFoundException {
		return get(gateway).isMaster();
	}

	@Override
	public boolean isOpened(String gateway) throws GatewayNotFoundException {
		return get(gateway).getStatus() == SchedulerSystemStatus.OPEN.indexOf();
	}

	@Override
	public boolean isReferJobLevel(String gateway) throws GatewayNotFoundException {
		return get(gateway).getReferToJobLevel() == ReferJobLevel.YES.indexOf();
	}

	@Override
	public int getReferPointRandom(String gateway) throws GatewayNotFoundException {
		// return get(gateway).getReferPointRandom() == ReferPointRandom.YES.indexOf();
		return get(gateway).getReferPointRandom();
	}

	@Override
	public int getRoundWay(String gateway) throws GatewayNotFoundException {
		return get(gateway).getRoundWay();
	}

	@Override
	public String getTailNumber(String gateway) throws GatewayNotFoundException {
		return get(gateway).getTailNumber();
	}

	@Override
	public int getTaskRunningPriority(String gateway) throws GatewayNotFoundException {
		return get(gateway).getTaskRunningPriority();
	}

	@Override
	public int getTaskFailReturnTimes(String gateway) throws GatewayNotFoundException {
		return get(gateway).getTaskFailReturnTimes();
	}

	@Override
	public int getTaskRunningMax(String gateway) throws GatewayNotFoundException {
		return get(gateway).getTaskRunningMax();
	}

	@Override
	public int getWaitUpdateStatusTaskCount(String gateway) throws GatewayNotFoundException {
		return get(gateway).getWaitUpdateStatusTaskCount();
	}

	@Override
	public Long[] getTaskCountExceptJobIds(String gateway) throws GatewayNotFoundException {
		String taskCountExceptJob = get(gateway).getTaskCountExceptJobs();
		return StringUtils.hasText(taskCountExceptJob) ? (Long[]) ConvertUtils.convert(taskCountExceptJob.split(","), Long.class) : null;
	}

	@Override
	public String getAllowExecuteJobTypes(String gateway) throws GatewayNotFoundException {
		return get(gateway).getJobType();
	}

	@Override
	public Long[] getAllowExecuteJobTypesAsArray(String gateway) throws GatewayNotFoundException {
		String jobType = get(gateway).getJobType();
		return (Long[]) ConvertUtils.convert(jobType.split(","), Long.class);
	}

	@Override
	public boolean isUseWhiteList(String gateway) throws GatewayNotFoundException {
		return get(gateway).isUseWhiteList();
	}

	@Override
	public String getWhiteListJobIds(String gateway) throws GatewayNotFoundException {
		return get(gateway).getJobWhiteList();
	}

	@Override
	public Long[] getWhiteListJobIdsAsArray(String gateway) throws GatewayNotFoundException {
		String jobWhiteList = get(gateway).getJobWhiteList();

		if (StringUtils.hasText(jobWhiteList)) {
			return (Long[]) ConvertUtils.convert(jobWhiteList.split(","), Long.class);
		}

		return null;
	}

	@Override
	public boolean isSerialScheduler(String gateway) throws GatewayNotFoundException {
		return get(gateway).getSchedulerWay() == SchedulerWay.SERIAL.indexOf();
	}

	@Override
	public Collection<Integer> getDisableSupplyHours(String gateway) throws GatewayNotFoundException {
		Gateway g = get(gateway);

		if (!StringUtils.hasText(g.getDisableSupplyHours())) {
			return new ArrayList<Integer>();
		}

		Collection<Integer> disableSupplyHours = new ArrayList<Integer>();
		for (String hour : g.getDisableSupplyHours().split(",")) {
			disableSupplyHours.add(Integer.parseInt(hour.trim()));
		}

		return disableSupplyHours;
	}

	/**
	 * 获得指定名称的网关机
	 * 
	 * @param gatewayName
	 * @return
	 * @throws GatewayNotFoundException
	 */
	private Gateway get(String gatewayName) throws GatewayNotFoundException {
		gateway = gateways.get(gatewayName);

		if (gateway != null)
			return gateway;

		gateway = gatewayService.getGateway(gatewayName);
		/*Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("gateway", gateway));
		sss = (ScheduleSystemStatus) criteria.uniqueResult();*/

		if (gateway == null) {
			// 当前指定网关机不存在时则直接抛出异常
			throw new GatewayNotFoundException(gatewayName);
			/*sss = new ScheduleSystemStatus(gatewayName, SchedulerSystemStatus.OPEN.indexOf(), ReferJobLevel.NO.indexOf(), SchedulerSystemBalance.CLOSE.indexOf(),
					TaskRunningPriority.PERCENT.indexOf(), ReferPointRandom.YES.indexOf(), DateUtil.getToday());
			sss.setTaskFailReturnTimes(1);
			sss.setTaskRunningMax(20);
			sss.setWaitUpdateStatusTaskCount(50);
			save(sss);*/
		}

		this.update(gateway);

		return gateway;
	}

	@Override
	public void update(Gateway gateway) {
		gateways.put(gateway.getName(), gateway);
	}

	@Override
	public void remove(Gateway gateway) {
		gateways.remove(gateway.getName());
	}

}
