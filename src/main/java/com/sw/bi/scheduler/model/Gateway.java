package com.sw.bi.scheduler.model;

import java.util.Date;

public class Gateway implements java.io.Serializable {

	private Long gatewayId;

	/**
	 * 网关机名称
	 */
	private String name;

	/**
	 * 网关机IP
	 */
	private String ip;

	/**
	 * 用户名
	 */
	private String userName;

	/**
	 * 密码
	 */
	private String password;

	/**
	 * 网关机端口
	 */
	private int port;

	/**
	 * 是否主网关机
	 */
	private boolean master;

	/**
	 * 备注
	 */
	private String description;

	/**
	 * 状态
	 */
	private int status;

	/**
	 * HiveJDBC连接
	 */
	private String hiveJdbc;

	//////////////////////////// 调度配置参数 ///////////////////////////

	/**
	 * 网关机调度方式(0:并行 1:串行)
	 */
	private int schedulerWay;

	/**
	 * 网关机轮循方式(0:参考点 1:模拟)
	 */
	private int roundWay;

	/**
	 * 执行任务尾号
	 */
	private String tailNumber;

	/**
	 * 选取任务优先级
	 */
	private int referToJobLevel;

	/**
	 * 参考点选取
	 */
	private int taskRunningPriority;

	/**
	 * 随机选取参考点
	 */
	private int referPointRandom;

	/**
	 * 任务出错重跑次数
	 */
	private int taskFailReturnTimes;

	/**
	 * 调度系统同时运行的最大任务数
	 */
	private int taskRunningMax = 20;

	/**
	 * 调度系统选取的最大参考点数
	 */
	private int waitUpdateStatusTaskCount = 50;

	/**
	 * 统计执行任务数需要排除的作业ID
	 */
	private String taskCountExceptJobs;

	/**
	 * 允许执行的作业类型
	 */
	private String jobType;

	/**
	 * 作业白名单
	 */
	private String jobWhiteList;

	/**
	 * 该网关机是否启用白名单
	 */
	private boolean useWhiteList;

	/**
	 * 禁止补数据时间点
	 */
	private String disableSupplyHours;

	private Date createTime;
	private Date updateTime;

	public Long getGatewayId() {
		return gatewayId;
	}

	public void setGatewayId(Long gatewayId) {
		this.gatewayId = gatewayId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public boolean isMaster() {
		return master;
	}

	public void setMaster(boolean master) {
		this.master = master;
	}

	public int getSchedulerWay() {
		return schedulerWay;
	}

	public void setSchedulerWay(int schedulerWay) {
		this.schedulerWay = schedulerWay;
	}

	public int getRoundWay() {
		return roundWay;
	}

	public void setRoundWay(int roundWay) {
		this.roundWay = roundWay;
	}

	public String getTailNumber() {
		return tailNumber;
	}

	public void setTailNumber(String tailNumber) {
		this.tailNumber = tailNumber;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
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

	public String getHiveJdbc() {
		return hiveJdbc;
	}

	public void setHiveJdbc(String hiveJdbc) {
		this.hiveJdbc = hiveJdbc;
	}

	public int getReferToJobLevel() {
		return referToJobLevel;
	}

	public void setReferToJobLevel(int referToJobLevel) {
		this.referToJobLevel = referToJobLevel;
	}

	public int getTaskRunningPriority() {
		return taskRunningPriority;
	}

	public void setTaskRunningPriority(int taskRunningPriority) {
		this.taskRunningPriority = taskRunningPriority;
	}

	public int getReferPointRandom() {
		return referPointRandom;
	}

	public void setReferPointRandom(int referPointRandom) {
		this.referPointRandom = referPointRandom;
	}

	public int getTaskFailReturnTimes() {
		return taskFailReturnTimes;
	}

	public void setTaskFailReturnTimes(int taskFailReturnTimes) {
		this.taskFailReturnTimes = taskFailReturnTimes;
	}

	public int getTaskRunningMax() {
		return taskRunningMax;
	}

	public void setTaskRunningMax(int taskRunningMax) {
		this.taskRunningMax = taskRunningMax;
	}

	public int getWaitUpdateStatusTaskCount() {
		return waitUpdateStatusTaskCount;
	}

	public void setWaitUpdateStatusTaskCount(int waitUpdateStatusTaskCount) {
		this.waitUpdateStatusTaskCount = waitUpdateStatusTaskCount;
	}

	public String getTaskCountExceptJobs() {
		return taskCountExceptJobs;
	}

	public void setTaskCountExceptJobs(String taskCountExceptJobs) {
		this.taskCountExceptJobs = taskCountExceptJobs;
	}

	public String getJobType() {
		return jobType;
	}

	public void setJobType(String jobType) {
		this.jobType = jobType;
	}

	public String getJobWhiteList() {
		return jobWhiteList;
	}

	public void setJobWhiteList(String jobWhiteList) {
		this.jobWhiteList = jobWhiteList;
	}

	public boolean isUseWhiteList() {
		return useWhiteList;
	}

	public void setUseWhiteList(boolean useWhiteList) {
		this.useWhiteList = useWhiteList;
	}

	public String getDisableSupplyHours() {
		return disableSupplyHours;
	}

	public void setDisableSupplyHours(String disableSupplyHours) {
		this.disableSupplyHours = disableSupplyHours;
	}

}
