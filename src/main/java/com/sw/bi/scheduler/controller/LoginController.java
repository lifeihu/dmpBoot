package com.sw.bi.scheduler.controller;

import javax.annotation.Resource;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;

import com.sw.bi.scheduler.service.impl.Login2ServiceImpl;
import com.sw.bi.scheduler.service.impl.LoginServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sw.bi.scheduler.model.User;
import com.sw.bi.scheduler.service.UserService;

import framework.exception.Warning;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manage/login")
public class LoginController {



	@Autowired
	private UserService userService;

	@Resource
	private Login2ServiceImpl loginService;


	@ModelAttribute
	public void prepared(HttpServletRequest request) {
		String username = request.getParameter("username");
		if (StringUtils.hasText(username)) {
			User user = userService.getUserByLoginName(username);

			if (user == null) {
				throw new Warning("指定的登录用户(" + username + ")不存在.");
			}

			if (user.getStatus() == 1) {
				throw new Warning("指定的登录用户(" + username + ")已经被禁用.");
			}
		}
	}

	/**
	 * 获得指定用户的手机
	 * 
	 * @param username
	 * @return
	 * @throws LoginException
	 */
	@RequestMapping("/mobile")
	@ResponseBody
	public String getUserMobiles(String username) {
		User user = userService.getUserByLoginName("admin");
		User user1 = new User();
		user1.setUserName("test");
		userService.save(user1);
		return "1335555555";
	}

	/**
	 * 生成手机验证码
	 * 
	 * @param username
	 * @param password
	 * @return
	 */
	@RequestMapping("/mobileCode")
	public void mobileCode(String username, String password) {
	}

	/**
	 * 校验用户登录信息
	 * 
	 * @param username
	 * @param password
	 * @param vertifyCode
	 */
	@RequestMapping("/validate")
	public void validate(String username, String mobile, String password, String vertifyCode) {
		loginService.validate(username, mobile, password, vertifyCode);
	}


}
