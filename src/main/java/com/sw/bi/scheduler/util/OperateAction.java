package com.sw.bi.scheduler.util;

/**
 * 操作日志动作
 * 
 * @author shiming.hong
 */
public enum OperateAction {

	CREATE("添加"), UPDATE("修改"), DELETE("删除"), LOGIC_DELETE("禁用"), RECOVERY("启用"),

	PASSWORD("修改密码"),

	ASSIGN("分配"), UNASSIGN("解除"), UNAUTHORIZED("越权"),

	ONLINE("上线"), OFFLINE("下线"),

	REDO("重跑"), SUPPLY("补数据"), CANCEL_SUPPLY("取消补数据"), KILL_PID("杀进程"), TASK_UPDATE("手工修改"),

	PAUSE_ALERT("暂停告警"), RESET_ALERT("恢复告警"),

	SIMULATE("模拟调度"),

	PUBLISH("发布"),

	NONE("无"), // 无动作(理论该动作只是一个默认值,实际记录日志的记录都是会有一个动作的)

	;

	final String value;

	private OperateAction(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}
}
