package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.model.LoginLogger;
import org.springframework.security.core.userdetails.AuthenticationUserDetails;

public interface LoginLoggerService extends GenericService<LoginLogger> {

	/**
	 * 记录用户登录日志
	 * 
	 * @param aud
	 * @return
	 */
	public LoginLogger log(AuthenticationUserDetails aud);

}
