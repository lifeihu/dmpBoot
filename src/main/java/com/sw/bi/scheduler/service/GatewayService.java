package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.model.Gateway;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@Transactional
public interface GatewayService extends GenericService<Gateway> {

	/**
	 * 获得指定网关机
	 * 
	 * @param gateway
	 * @return
	 */
	public Gateway getGateway(String gateway);

	/**
	 * 获得所有启用状态的网关机
	 * 
	 * @return
	 */
	public Collection<Gateway> getActiveGateways();

	/**
	 * 获得主网关机
	 * 
	 * @return
	 */
	public Gateway getMasterGateway();

	// public boolean isMasterGateway(String gateway);

	/**
	 * 获得启用的HiveJDBC连接
	 * 
	 * @return
	 */
	public String getHiveJDBC();

	/**
	 * 获得可执行指定作业类型的网关机
	 * 
	 * @param jobType
	 * @return
	 */
	public Collection<Gateway> getGatewaysByJobType(int jobType);

	/**
	 * 获得所有网关机名称
	 * 
	 * @return
	 */
	public Collection<String> getGatewayNames();

	/**
	 * 从指定网关机的白名单中删除指定作业ID
	 * 
	 * @param gateway
	 * @param jobId
	 */
	public void removeJobFromWhiteList(String gateway, long jobId);

	/**
	 * 将指定作业ID加入指定网关机的白名单中
	 * 
	 * @param gateway
	 * @param jobId
	 */
	public void addJobToWhiteList(String gateway, long jobId);

	/**
	 * 修改网关机调度方式
	 * 
	 * @param schedulerWay
	 */
	public void updateSchedulerWay(int schedulerWay);

	/**
	 * 修改网关机轮循方式
	 * 
	 * @param roundWay
	 */
	public void updateRoundWay(int roundWay);

}
