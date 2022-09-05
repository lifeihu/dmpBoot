package com.sw.bi.scheduler.background.exception;

/**
 * 任务重复创建异常
 * 
 * @author shiming.hong
 */
public class TaskDuplicateCreateSchedulerExecption extends SchedulerException {

	public TaskDuplicateCreateSchedulerExecption(Throwable cause) {
		super("任务被重复创建", cause);
	}

	@Override
	public int getErrorCode() {
		return ErrorCode.TASK_DUPLICATE_CREATE.errorCode();
	}

}
