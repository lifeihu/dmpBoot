package com.sw.bi.scheduler.model;

/**
 * <pre>
 * 实际该接口的实体类在操作时需要先认证用户组权限
 * </pre>
 * 
 * @author shiming.hong
 * @date 2014-07-21
 */
public interface AuthenticationUserGroup extends LoggerEntity {

	/**
	 * 获得实体创建者ID
	 * 
	 * @return
	 */
	public Long getUserId();

}
