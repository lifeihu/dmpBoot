package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.model.MethodExecTime;

public interface MethodExecTimeService extends GenericService<MethodExecTime> {

	/**
	 * 指定方法开始执行
	 * 
	 * @param method
	 */
	public void begin(String method);

	/**
	 * 指定方法执行完毕
	 * 
	 * @param method
	 */
	public void finished(String method);

}
