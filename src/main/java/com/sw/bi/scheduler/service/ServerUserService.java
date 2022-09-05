package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.model.ServerUser;

public interface ServerUserService extends GenericService<ServerUser> {

	/**
	 * 设置指定用户的密码
	 * 
	 * @param serverUserId
	 * @param password
	 * @return
	 */
	public void changePassword(long serverUserId, String password);

}
