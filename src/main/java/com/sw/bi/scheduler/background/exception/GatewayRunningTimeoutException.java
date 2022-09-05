package com.sw.bi.scheduler.background.exception;

public class GatewayRunningTimeoutException extends SchedulerException {

	public GatewayRunningTimeoutException(String gateway) {
		super("网关机调度检测到网关机(" + gateway + ")长时间运行仍未结束,可能导致其他网关机无法被执行");
	}

	@Override
	public int getErrorCode() {
		return ErrorCode.GATEWAY_RUNNING_TIMEOUT.errorCode();
	}

}
