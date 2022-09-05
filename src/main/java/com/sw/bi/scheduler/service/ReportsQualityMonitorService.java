package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.model.ReportsQualityMonitor;

public interface ReportsQualityMonitorService extends GenericService<ReportsQualityMonitor> {

	/**
	 * 获得指定作业对应的报表质量监控配置
	 * 
	 * @param jobId
	 * @return
	 */
	public ReportsQualityMonitor getByJobId(Long jobId);

}
