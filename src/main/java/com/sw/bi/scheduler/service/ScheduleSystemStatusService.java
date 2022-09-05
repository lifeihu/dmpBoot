package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.background.exception.GatewayNotFoundException;
import com.sw.bi.scheduler.model.Gateway;

import java.util.Collection;

public interface ScheduleSystemStatusService /*extends GenericService<ScheduleSystemStatus>*/{

	/**
	 * 开启调度系统
	 * 
	 * @throws GatewayNotFoundExecption
	 */
	// public void open(String gateway) throws GatewayNotFoundExecption;

	/**
	 * 关闭调度系统
	 * 
	 * @throws GatewayNotFoundExecption
	 */
	// public void close(String gateway) throws GatewayNotFoundExecption;

	/**
	 * 开启参照作业优先级
	 */
	// public void openReferJobLevel(String gateway);

	/**
	 * 关闭参照作业优先级
	 */
	// public void closeReferJobLevel(String gateway);

	/**
	 * 开启随机选取被修改为未触发状态的任务
	 */
	// public void openReferPointRandom(String gateway);

	/**
	 * 关闭随机选取被修改为未触发状态的任务
	 */
	// public void closeReferPointRandom(String gateway);

	/**
	 * 设置任务运行优先级
	 * 
	 * @param taskRunningPriority
	 */
	// public void setTaskRunningPriority(String gateway, TaskRunningPriority taskRunningPriority);

	/**
	 * 设置系统自动重跑失败任务的次数
	 * 
	 * @param taskFailReturnTimes
	 */
	// public void setTaskFailReturnTimes(String gateway, int taskFailReturnTimes);

	/**
	 * 更新网关机缓存
	 * 
	 * @param gateway
	 */
	public void update(Gateway gateway);

	/**
	 * 删除网关机缓存
	 * 
	 * @param gateway
	 */
	public void remove(Gateway gateway);

	/**
	 * 获得主网关机
	 * 
	 * @param gateway
	 * @return
	 * @throws GatewayNotFoundException
	 */
	public boolean isMasterGateway(String gateway) throws GatewayNotFoundException;

	/**
	 * 是否已开启调度系统
	 * 
	 * @return
	 * @throws GatewayNotFoundException
	 */
	public boolean isOpened(String gateway) throws GatewayNotFoundException;

	/**
	 * 是否参照作业优先级选取任务
	 * 
	 * @return
	 * @throws GatewayNotFoundException
	 */
	public boolean isReferJobLevel(String gateway) throws GatewayNotFoundException;

	/**
	 * 参考点是否随机选取
	 * 
	 * @return
	 * @throws GatewayNotFoundException
	 */
	public int getReferPointRandom(String gateway) throws GatewayNotFoundException;

	/**
	 * 获得网关机轮循方式
	 * 
	 * @param gateway
	 * @return
	 * @throws GatewayNotFoundException
	 */
	public int getRoundWay(String gateway) throws GatewayNotFoundException;

	/**
	 * 获得网关机执行尾号
	 * 
	 * @param gateway
	 * @return
	 * @throws GatewayNotFoundException
	 */
	public String getTailNumber(String gateway) throws GatewayNotFoundException;

	/**
	 * 获得任务运行优先级设置
	 * 
	 * @return
	 * @throws GatewayNotFoundException
	 */
	public int getTaskRunningPriority(String gateway) throws GatewayNotFoundException;

	/**
	 * 获得系统自动重跑失败任务的次数
	 * 
	 * @return
	 * @throws GatewayNotFoundException
	 */
	public int getTaskFailReturnTimes(String gateway) throws GatewayNotFoundException;

	/**
	 * 获得同时运行的任务的最大数量
	 * 
	 * @param gateway
	 * @return
	 * @throws GatewayNotFoundException
	 */
	public int getTaskRunningMax(String gateway) throws GatewayNotFoundException;

	/**
	 * 服务器单次调度选取的最大参考点数目
	 * 
	 * @param gateway
	 * @return
	 * @throws GatewayNotFoundException
	 */
	public int getWaitUpdateStatusTaskCount(String gateway) throws GatewayNotFoundException;

	/**
	 * 获得统计执行任务数需要排除的作业ID
	 * 
	 * @param gateway
	 * @return
	 * @throws GatewayNotFoundException
	 */
	public Long[] getTaskCountExceptJobIds(String gateway) throws GatewayNotFoundException;

	/**
	 * 获得指定网关机上允许执行所有作业类型
	 * 
	 * @param gateway
	 * @return
	 */
	public String getAllowExecuteJobTypes(String gateway) throws GatewayNotFoundException;

	/**
	 * 获得指定网关机上允许执行所有作业类型
	 * 
	 * @param gateway
	 * @return
	 * @throws GatewayNotFoundException
	 */
	public Long[] getAllowExecuteJobTypesAsArray(String gateway) throws GatewayNotFoundException;

	/**
	 * 指定网关机是否开始白名单
	 * 
	 * @param gateway
	 * @return
	 * @throws GatewayNotFoundException
	 */
	public boolean isUseWhiteList(String gateway) throws GatewayNotFoundException;

	/**
	 * 获得指定网关机上的白名单列表
	 * 
	 * @param gateway
	 * @return
	 * @throws GatewayNotFoundException
	 */
	public String getWhiteListJobIds(String gateway) throws GatewayNotFoundException;

	/**
	 * 获得指定网关机上的白名单列表
	 * 
	 * @param gateway
	 * @return
	 * @throws GatewayNotFoundException
	 */
	public Long[] getWhiteListJobIdsAsArray(String gateway) throws GatewayNotFoundException;

	/**
	 * 指定网关机是否以串行方式轮循
	 * 
	 * @param gateway
	 * @return
	 * @throws GatewayNotFoundException
	 */
	public boolean isSerialScheduler(String gateway) throws GatewayNotFoundException;

	/**
	 * 获得指定网关机禁止补数据的时间点
	 * 
	 * @param gateway
	 * @return
	 * @throws GatewayNotFoundException
	 */
	public Collection<Integer> getDisableSupplyHours(String gateway) throws GatewayNotFoundException;
}
