package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.LoginLogger;
import com.sw.bi.scheduler.service.LoginLoggerService;
import org.springframework.security.core.userdetails.AuthenticationUserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class LoginLoggerServiceImpl extends GenericServiceHibernateSupport<LoginLogger> implements LoginLoggerService {

	@Override
	public LoginLogger log(AuthenticationUserDetails aud) {
		if (aud == null) {
			return null;
		}

		LoginLogger logger = new LoginLogger();

		logger.setUserId(aud.getId());
		logger.setLoginName(aud.getUsername());
		logger.setUserName(aud.getRealname());
		logger.setUserGroupId(aud.getUserGroupId());
		logger.setUserGroupName(aud.getUserGroupName());
		logger.setLoginIp(aud.getIp());
		logger.setVertifyCode(aud.getVertifyCode());
		logger.setCreateTime(new Date());

		this.saveOrUpdate(logger);

		return logger;
	}

}
