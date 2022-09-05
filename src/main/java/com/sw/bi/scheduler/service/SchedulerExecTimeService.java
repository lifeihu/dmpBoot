package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.model.SchedulerExecTime;
import com.sw.bi.scheduler.util.Configure;

public interface SchedulerExecTimeService extends GenericService<SchedulerExecTime> {

	public static final String SCHEDULER_FLAG = Configure.property(Configure.GATEWAY);

	/**
	 * 上一次的调度过程是否已经执行完成
	 * 
	 * @return
	 */
	public boolean isFinished();

	/**
	 * 开始本次调度过程
	 */
	public void begin();

	/**
	 * 调度辅助程序开始调度过程
	 */
	@Deprecated
	public void helpBegin();

	/**
	 * 本次调度过程已经执行完毕
	 */
	public void finished();

	/**
	 * 调度辅助程序已经执行完毕
	 */
	@Deprecated
	public void helpFinished();

	/**
	 * 当出错或上次调度持续未完成时才进行此重置操作
	 * 
	 * @param immediately
	 * @return
	 */
	public boolean refinished(boolean immediately);
}
