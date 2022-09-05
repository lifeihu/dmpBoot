package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.model.SchedulerExecTimeHistory;

public interface SchedulerExecTimeHistoryService extends GenericService<SchedulerExecTimeHistory> {

	/**
	 * 开始本次调度过程明细
	 */
	public void begin();

	/**
	 * 结果本次调度过程明细
	 */
	public void finished();

}
