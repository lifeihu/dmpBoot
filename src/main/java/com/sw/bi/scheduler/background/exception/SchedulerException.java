package com.sw.bi.scheduler.background.exception;

public abstract class SchedulerException extends Exception {

	public SchedulerException() {}

	public SchedulerException(Throwable cause) {
		super(cause);
	}

	public SchedulerException(String message) {
		super(message);
	}

	public SchedulerException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * 获得错误代码
	 * 
	 * @return
	 */
	public abstract int getErrorCode();

}
