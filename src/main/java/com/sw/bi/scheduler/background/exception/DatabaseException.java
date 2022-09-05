package com.sw.bi.scheduler.background.exception;

public class DatabaseException extends SchedulerException {

	public DatabaseException(String message, Throwable cause) {
		super(message, cause);
	}

	@Override
	public int getErrorCode() {
		return ErrorCode.DATABASE.errorCode();
	}

}
