package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.LoggerEntity;
import com.sw.bi.scheduler.model.OperateLogger;
import com.sw.bi.scheduler.service.OperateLoggerService;
import com.sw.bi.scheduler.util.OperateAction;
import org.hibernate.Session;
import org.springframework.security.core.userdetails.AuthenticationUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;

@Service
public class OperateLoggerServiceImpl extends GenericServiceHibernateSupport<OperateLogger> implements OperateLoggerService {

	@Override
	public void logCreate(Object loggerEntity) {
		log(OperateAction.CREATE, loggerEntity);
	}

	@Override
	public void logUpdate(Object loggerEntity) {
		log(OperateAction.UPDATE, loggerEntity);
	}

	@Override
	public void logDelete(Object loggerEntity) {
		log(OperateAction.DELETE, loggerEntity);
	}

	@Override
	public void log(OperateAction operateAction, Object loggerEntity) {
		if (!(loggerEntity instanceof LoggerEntity)) {
			return;
		}

		LoggerEntity le = (LoggerEntity) loggerEntity;
		String entityName = le.getEntityName();
		String loggerName = le.getLoggerName();

		log(operateAction, "[" + entityName + "].[" + loggerName + "]");
	}

	@Override
	public void log(OperateAction operateAction, String loggerContent) {
		log(operateAction.value(), loggerContent);
	}

	@Override
	public void log(String operateAction, String loggerContent) {
		if (!StringUtils.hasText(loggerContent)) {
			return;
		}

		OperateLogger logger = new OperateLogger();

		AuthenticationUserDetails aud = super.getPrincipal();
		if (aud == null) {
			return;
		}

		logger.setUserId(aud.getId());
		logger.setUserName(aud.getRealname());
		logger.setUserGroupId(aud.getUserGroupId());
		logger.setUserGroupName(aud.getUserGroupName());
		logger.setOperateIp(aud.getIp());
		logger.setOperateAction(operateAction);
		logger.setOperateContent(loggerContent);
		logger.setCreateTime(new Date());

		log.info("[" + operateAction + "] -> " + loggerContent);

		super.saveOrUpdate(logger);
	}

	@Override
	public void logUnauthorized(String loggerContent) {
		if (!StringUtils.hasText(loggerContent)) {
			return;
		}

		AuthenticationUserDetails aud = super.getPrincipal();
		if (aud == null) {
			return;
		}

		// 一般越权操作都会抛出异常所以这里需要新建一个数据源
		Session session = getHibernateTemplate().getSessionFactory().openSession();

		OperateLogger logger = new OperateLogger();

		logger.setUserId(aud.getId());
		logger.setUserName(aud.getRealname());
		logger.setOperateIp(aud.getIp());
		logger.setOperateAction(OperateAction.UNAUTHORIZED.value());
		logger.setOperateContent(loggerContent);
		logger.setCreateTime(new Date());

		log.info("[" + logger.getOperateAction() + "] -> " + loggerContent);

		try {
			session.saveOrUpdate(logger);
			session.flush();
		} finally {
			session.close();
		}
	}
}
