package com.sw.bi.scheduler.background.exception;

public enum ErrorCode {

	UNKNOWN(-1), // 未知

	// 100-199 调试系统异常
	GATEWAY_NOT_FOUND(100), // 网关机未找到
	TRANSACTION_LOCK_TIMEOUT(101), // 数据库事务锁超时
	GATEWAY_RUNNING_TIMEOUT(102), // 网关机运行超时
	DATABASE(103), // 数据库操作异常

	// 200-299作业异常
	TASK_DUPLICATE_CREATE(200), // 任务重复创建

	;

	private int errorCode = -1;

	private ErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public int errorCode() {
		return this.errorCode;
	}

}
