package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.background.exception.DatabaseException;
import com.sw.bi.scheduler.background.exception.GatewayRunningTimeoutException;
import com.sw.bi.scheduler.model.GatewayScheduler;
import com.sw.bi.scheduler.service.GatewaySchedulerService;
import com.sw.bi.scheduler.service.GatewayService;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate5.SessionFactoryUtils;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.List;

@Service
public class GatewaySchedulerServiceImpl extends GenericServiceHibernateSupport<GatewayScheduler> implements GatewaySchedulerService {

	@Autowired
	private GatewayService gatewayService;

	/**
	 * 所有网关机
	 */
	private List<String> gateways;

	@Override
	public boolean isAllowExecution(String gateway) throws GatewayRunningTimeoutException {
		GatewayScheduler gatewayScheduler = this.get();

		// 获得所有网关的轮循顺序
		gateways = (List<String>) gatewayService.getGatewayNames();
		log.info(">>>>>网关机执行顺序清单: " + gateways + "<<<<<<");

		// 当前正在执行的网关机
		String runningGateway = gatewayScheduler.getGateway();

		// 获得下个允许执行的网关机
		String nextGateway = this.getNextGateway(runningGateway);

		log.info("当前正在执行的网关机: " + runningGateway + ", 下一个允许执行的网关机: " + nextGateway);

		if (gatewayScheduler.isFinished()) {
			if (gateway.equals(nextGateway)) {
				log.info("网关机(" + runningGateway + ")执行完毕,当前网关机(" + gateway + ")被执行");
				this.execute(gateway);
				return true;
			} else {
				log.info("网关机(" + runningGateway + ")执行完毕,下一个允许执行的网关机(" + nextGateway + ")不是当前网关机(" + gateway + ")");
				return false;
			}
		}

		// 如果当前有网关机正在运行，则需要校验运行时间是否超过了指定的值

		boolean allowAlert = false;

		// 已经运行的时长
		long runningTime = System.currentTimeMillis() - gatewayScheduler.getUpdateTime().getTime();
		if (runningTime > gatewayScheduler.getMaxDealingTime() * 60 * 1000) {
			log.warn("网关机(" + runningGateway + ")已经运行了 " + runningTime / 1000 / 60 + "m.");

			// 超过指定运行时长时需要告警(告警时长设置为15分钟，防止重复告警)
			Date alertTime = gatewayScheduler.getAlertTime();

			// 如果之前没有告警则直接允许告警
			allowAlert = alertTime == null;

			if (!allowAlert) {
				// 如果之前已经有过告警，则需要判断间隔时长
				if (System.currentTimeMillis() - alertTime.getTime() > 15 * 60 * 1000) {
					allowAlert = true;
				}
			}
		}

		log.info("网关机(" + runningGateway + ")正在执行,导致当前网关机(" + gateway + ")本轮执行被终止.");

		if (allowAlert) {
			throw new GatewayRunningTimeoutException(runningGateway);
		}

		return false;
	}

	@Override
	public void execute(String gateway) {
		GatewayScheduler gatewayScheduler = get();

		gatewayScheduler.setFinished(false);
		gatewayScheduler.setGateway(gateway);
		gatewayScheduler.setUpdateTime(new Date());

		this.update(gatewayScheduler);
	}

	@Override
	public void finished(String gateway) {
		GatewayScheduler gatewayScheduler = get();

		gatewayScheduler.setFinished(true);
		gatewayScheduler.setUpdateTime(new Date());

		this.update(gatewayScheduler);
	}

	@Override
	public boolean isExceedDatabaseMaxConnection() throws DatabaseException {
		Connection connection = null;
		Statement stmt = null;
		ResultSet rs = null;

		try {
			connection = SessionFactoryUtils.getDataSource(getSessionFactory()).getConnection();
			stmt = connection.createStatement();
			rs = stmt.executeQuery("show status like '%Threads_connected%'");

			rs.next();
			int connected = rs.getInt(2);
			boolean exceed = connected > 800;

			String message = "已连接数: " + connected;
			if (exceed) {
				message += ", 连接数超过阈值(800)操作被终止.";
			}
			log.info(message);

			return exceed;

		} catch (SQLException e) {
			e.printStackTrace();
			throw new DatabaseException("获得数据库最大连接数失败.", e);

		} finally {
			try {
				if (rs != null) {
					rs.close();
				}

				if (stmt != null) {
					stmt.close();
				}

				if (connection != null && !connection.isClosed()) {
					connection.close();
				}
			} catch (SQLException e) {}
		}
	}

	private String getNextGateway(String gateway) {
		int pos = gateways.indexOf(gateway);

		if (pos < 0) {
			return gateways.get(0);
		}

		int nextPos = pos + 1;
		if (nextPos >= gateways.size()) {
			nextPos = 0;
		}

		return gateways.get(nextPos);
	}

	private GatewayScheduler get() {
		Criteria criteria = this.createCriteria();
		criteria.add(Restrictions.eq("gatewaySchedulerId", 1l));

		return (GatewayScheduler) criteria.uniqueResult();
	}

}
