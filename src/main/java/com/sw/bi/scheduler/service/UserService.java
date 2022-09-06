package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.model.User;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

public interface UserService extends GenericService<User>/*, UserDetailsService*/{

	/**
	 * 指定用户是否属于系统管理员
	 * 
	 * @param userId
	 * @return
	 */
	public boolean isAdministrator(Long userId);

	/**
	 * 获得系统管理员
	 * 
	 * @return
	 */
	public User getAdministrator();

	/**
	 * 设置指定用户的密码
	 * 
	 * @param userId
	 * @param password
	 *            密码为空时系统会初始化一个密码
	 * @return
	 */
	public String changePassword(long userId, String password);

	/**
	 * 统一加密用户登录密码
	 */
	public void batchChangePassword();

	/**
	 * 获得指定用户未被分配的用户
	 * 
	 * @return
	 */
	public Collection<User> getNotAssignUsersByUserGroup();

	/**
	 * 根据登录名获得用户
	 * 
	 * @param loginName
	 * @return
	 */
	@Transactional
	public User getUserByLoginName(String loginName);

}
