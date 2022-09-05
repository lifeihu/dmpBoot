package com.sw.bi.scheduler.util;

import org.springframework.dao.DataAccessException;

public class MyPasswordEncode  {

	public boolean isPasswordValid(String savePass, String submitPass,
			Object salt) {
		// savePass为数据库中加密保存的密码，submitPass为用户登录时提交的明文密码
		return savePass.equalsIgnoreCase(encodePassword(submitPass, salt));
	}

	public String encodePassword(String submitPass, Object salt)
			throws DataAccessException {
		return MD5Util.getMD5Code(salt + MD5Util.getMD5Code(submitPass));
	}

}
