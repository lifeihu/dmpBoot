package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.background.util.DxDESCipher;
import com.sw.bi.scheduler.model.ServerUser;
import com.sw.bi.scheduler.service.ServerUserService;
import org.springframework.stereotype.Service;

@Service
public class ServerUserServiceImpl extends GenericServiceHibernateSupport<ServerUser> implements ServerUserService {

	@Override
	public void changePassword(long serverUserId, String password) {
		ServerUser serverUser = this.get(serverUserId);

		try {
			String encryptPassword = new DxDESCipher().encrypt(password);
			serverUser.setPassword(encryptPassword);

			super.update(serverUser);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void saveOrUpdate(ServerUser serverUser) {
		Long serverUserId = serverUser.getServerUserId();
		ServerUser oldServerUser = null;

		if (serverUserId == null) {
			try {
				// 创建帐号时需要加密密码
				String encryptPassword = new DxDESCipher().encrypt(serverUser.getPassword());
				serverUser.setPassword(encryptPassword);

			} catch (Exception e) {
				e.printStackTrace();
			}

		} else {
			// 保存用户时密码不变
			oldServerUser = this.get(serverUserId);
			this.getHibernateTemplate().evict(oldServerUser);

			serverUser.setPassword(oldServerUser.getPassword());
		}

		// 校验帐号是否已经存在
		/*Criteria criteria = this.createCriteria();
		criteria.setProjection(Projections.rowCount());
		criteria.add(Restrictions.eq("username", serverUser.getUsername()));
		if (serverUserId != null) {
			criteria.add(Restrictions.not(Restrictions.eq("serverUserId", serverUserId)));
		}
		Number count = (Number) criteria.uniqueResult();
		if (count.intValue() > 0) {
			throw new Warning("服务器帐号(" + serverUser.getUsername() + ")已经存在.");
		}*/

		super.saveOrUpdate(serverUser);
	}
}
