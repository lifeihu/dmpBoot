package com.sw.bi.scheduler.background.exception;

/**
 * 网关机未找到
 * 
 * @author shiming.hong
 */
public class GatewayNotFoundException extends SchedulerException {

	public GatewayNotFoundException(String gateway) {
		super("调度系统中未配置指定网关机(" + gateway + ")");
	}

	@Override
	public int getErrorCode() {
		return ErrorCode.GATEWAY_NOT_FOUND.errorCode();
	}

}
