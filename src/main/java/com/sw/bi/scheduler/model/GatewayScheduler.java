package com.sw.bi.scheduler.model;

import java.util.Date;

public class GatewayScheduler {

	private long gatewaySchedulerId;

	/**
	 * 处理状态(1:处理完毕 0:正在处理)
	 */
	private boolean finished;

	/**
	 * 正在执行的网关机
	 */
	private String gateway;

	/**
	 * 最大处理时间(单位分钟)
	 */
	private int maxDealingTime;

	/**
	 * 告警时间
	 */
	private Date alertTime;

	private Date createTime;
	private Date updateTime;

	public long getGatewaySchedulerId() {
		return gatewaySchedulerId;
	}

	public void setGatewaySchedulerId(long gatewaySchedulerId) {
		this.gatewaySchedulerId = gatewaySchedulerId;
	}

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}

	public String getGateway() {
		return gateway;
	}

	public void setGateway(String gateway) {
		this.gateway = gateway;
	}

	public int getMaxDealingTime() {
		return maxDealingTime;
	}

	public void setMaxDealingTime(int maxDealingTime) {
		this.maxDealingTime = maxDealingTime;
	}

	public Date getAlertTime() {
		return alertTime;
	}

	public void setAlertTime(Date alertTime) {
		this.alertTime = alertTime;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

}
