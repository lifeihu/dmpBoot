package com.sw.bi.scheduler.background.exception;

public class TransactionLockTimeoutException extends SchedulerException {

	public TransactionLockTimeoutException(Throwable cause) {
		super("数据库事务锁超时", cause);
	}

	@Override
	public int getErrorCode() {
		return ErrorCode.TRANSACTION_LOCK_TIMEOUT.errorCode();
	}

}
