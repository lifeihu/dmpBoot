package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.model.AlertSystemLog;
import org.springframework.ui.ConditionModel;
import org.springframework.ui.PaginationSupport;

public interface AlertSystemLogService extends GenericService<AlertSystemLog> {

	/**
	 * 根据告警日期和作业ID分组告警信息
	 * 
	 * @param cm
	 */
	public PaginationSupport pagingGroup(ConditionModel cm);

}
