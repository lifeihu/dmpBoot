package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.MethodExecTime;
import com.sw.bi.scheduler.service.MethodExecTimeService;
import com.sw.bi.scheduler.util.DateUtil;
import org.springframework.stereotype.Service;

@Service("methodExecTimeService")
public class MethodExecTimeServiceImpl extends GenericServiceHibernateSupport<MethodExecTime> implements MethodExecTimeService {

	private static MethodExecTime met;

	@Override
	public void begin(String method) {
		if (met != null) {
			return;
		}

		log.info("begin to " + method + " method...");

		met = new MethodExecTime();
		met.setMethodName(method);
		met.setDateDesc(DateUtil.getToday());
		met.setBeginTime(DateUtil.now());
		met.setCreateTime(DateUtil.now());

		save(met);
	}

	@Override
	public void finished(String method) {
		if (met == null) {
			return;
		}

		met.setEndTime(DateUtil.now());
		met.setRunTime((met.getEndTime().getTime() - met.getBeginTime().getTime()) / 1000);
		met.setUpdateTime(DateUtil.now());

		update(met);

		log.info(method + " method is finished(" + met.getRunTime() + "s).");

		met = null;

	}
}
