package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.model.VertifyCode;

public interface VertifyCodeService extends GenericService<VertifyCode> {

	/**
	 * 获得指定用户已经生成的验证码
	 * 
	 * @param username
	 * @param mobile
	 * @return
	 */
	public VertifyCode getVertifyCode(String username, String mobile);

	/**
	 * 获得指定用户验证码
	 * 
	 * @param username
	 * @param mobile
	 * @param vertifyCode
	 * @return
	 */
	public VertifyCode getVertifyCode(String username, String mobile, String vertifyCode);

	/**
	 * 生成验证码
	 * 
	 * @param username
	 * @param mobile
	 * @return
	 */
	public String generate(String username, String mobile);

	/**
	 * 验证码被正确使用
	 * 
	 * @param username
	 * @param mobile
	 * @param vertifyCode
	 */
	public void use(String username, String mobile, String vertifyCode);

}
