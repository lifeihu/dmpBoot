package com.sw.bi.scheduler.background.exception;

/**
 * 未知调度异常
 * 
 * @author shiming.hong
 */
public class UnknownSchedulerException extends SchedulerException {

	public UnknownSchedulerException() {}

	public UnknownSchedulerException(Throwable e) {
		super(e);
	}

	@Override
	public int getErrorCode() {
		return ErrorCode.UNKNOWN.errorCode();
	}

}
