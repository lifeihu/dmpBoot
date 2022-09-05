package com.sw.bi.scheduler.background.taskexcuter;

import com.sw.bi.scheduler.model.Task;

/**
 * 虚拟作业执行器
 * 
 * @author shiming.hong
 * 
 */
public class VirtualExcuter extends AbExcuter {

	public VirtualExcuter(Task task, String logFolder) {
		super(task, logFolder);
	}

	@Override
	public boolean excuteCommand() throws Exception {
		log("虚拟作业执行成功");

		return true;
	}

}
