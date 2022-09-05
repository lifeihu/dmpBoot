package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.model.ActionSql;
import org.springframework.ui.ConditionModel;
import org.springframework.ui.PaginationSupport;

public interface ActionSqlService extends GenericService<ActionSql> {

	public long getLastSqlIndex(long actionId);

	public PaginationSupport pagingBySql(ConditionModel cm);
}
