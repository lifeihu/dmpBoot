package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.SchedulerExecTimeHistory;
import com.sw.bi.scheduler.service.SchedulerExecTimeHistoryService;
import com.sw.bi.scheduler.service.SchedulerExecTimeService;
import com.sw.bi.scheduler.util.DateUtil;
import org.springframework.stereotype.Service;

@Service("schedulerExecTimeHistoryService")
public class SchedulerExecTimeHistoryServiceImpl extends GenericServiceHibernateSupport<SchedulerExecTimeHistory> implements SchedulerExecTimeHistoryService {

	private static SchedulerExecTimeHistory seth;

	@Override
	public void begin() {
		if (seth != null) {
			return;
		}

		seth = new SchedulerExecTimeHistory();
		seth.setDateDesc(DateUtil.getToday());
		seth.setBeginTime(DateUtil.now());
		seth.setEndTime(null);
		seth.setFlag(SchedulerExecTimeService.SCHEDULER_FLAG);
		seth.setCreateTime(DateUtil.now());
		seth.setFinished(false);

		save(seth);
	}

	@Override
	public void finished() {
		if (seth == null) {
			return;
		}

		seth.setEndTime(DateUtil.now());
		seth.setRunTime((seth.getEndTime().getTime() - seth.getBeginTime().getTime()) / 1000);
		seth.setUpdateTime(DateUtil.now());
		seth.setFinished(true);

		update(seth);

		seth = null;
	}

}
