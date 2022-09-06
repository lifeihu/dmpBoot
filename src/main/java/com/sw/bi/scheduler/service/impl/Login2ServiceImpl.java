package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.Role;
import com.sw.bi.scheduler.model.User;
import com.sw.bi.scheduler.model.UserGroup;
import com.sw.bi.scheduler.model.VertifyCode;
import com.sw.bi.scheduler.service.LoginLoggerService;
import com.sw.bi.scheduler.service.UserGroupRelationService;
import com.sw.bi.scheduler.service.UserService;
import com.sw.bi.scheduler.service.VertifyCodeService;
import com.sw.bi.scheduler.supports.MessageSenderAssistant;
import com.sw.bi.scheduler.util.Configure;
import com.sw.bi.scheduler.util.MD5Util;
import com.sw.bi.scheduler.util.PropertiesUtil;
import framework.commons.sender.MessagePlatform;
import framework.exception.Warning;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.AuthenticationUserDetails;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class Login2ServiceImpl{
	private static final Logger log = Logger.getLogger(Login2ServiceImpl.class);

	@Autowired
	private UserService userService;

	@Autowired
	private UserGroupRelationService userGroupRelationService;

	@Autowired
	private LoginLoggerService loginLoggerService;

	@Autowired
	private VertifyCodeService vertifyCodeService;

	@Autowired(required = false)
	private HttpServletRequest request;

	private MessageSenderAssistant messageSender = new MessageSenderAssistant(new String[] { "scheduler.properties" });
	//private MessageSenderAssistant messageSender = new MessageSenderAssistant("通知");

	/**
	 * 获得指定用户的手机号
	 * 
	 * @param username
	 * @return
	 */
	public String getUserMobiles(String username) {
		User user = userService.getUserByLoginName(username);

		String userMobile = user.getMobilePhone();
		if (!StringUtils.hasText(userMobile)) {
			throw new Warning("指定登录用户未设置手机号,不允许登录系统.");
		}

		return userMobile.substring(0, 3) + "****" + userMobile.substring(userMobile.length() - 4);
	}

	/**
	 * 生成指定用户的验证码并发送至手机
	 * 
	 * @param username
	 * @param password
	 */
	public void sendMobileCode(String username, String password) {
		Assert.hasText(username, "登录用户不允许为空.");

		User user = userService.getUserByLoginName(username);
		Assert.notNull(user, "登录用户(" + username + ")不存在或已被禁用.");

		String mobile = user.getMobilePhone();
		Assert.hasText(mobile, "手机不允许为空.");

		String vertifyCode = vertifyCodeService.generate(username, mobile);
		if (this.validate(username, mobile, password, vertifyCode)) {
			StringBuffer content = new StringBuffer();
			content.append("您(").append(username).append(")登录调度系统的验证码: ").append(vertifyCode);
			content.append(", 有效期").append(Configure.property(Configure.VERTIFY_CODE_TIMEOUT)).append("小时");

			log.info(content);

			/////messageSender.sendSms(mobile, content.toString(),"通知");

			// by whl 海信没有短信接口，验证码不用发送，固定为123456
			messageSender.send(MessagePlatform.SMS_ADTIME, mobile, content.toString()+ PropertiesUtil.getProperty("sender.sms.signature"));

			
			
			
			// 如果用户配置的邮箱则再往邮箱发送一个验证码,以防万一
//			if (StringUtils.hasText(user.getEmail())) {
//				messageSender.sendMail(user.getEmail(), "调度系统登录验证码", content.toString());
//			}
		}
	}

	/**
	 * 校验登录用户
	 * 
	 * @param username
	 * @param password
	 * @param vertifyCode
	 */
	public boolean validate(String username, String mobile, String password, String vertifyCode) {
		Assert.hasText(username, "未指定登录用户.");

		User user = userService.getUserByLoginName(username);
		Assert.notNull(user, "登录用户(" + username + ")不存在或已被禁用.");

		if (StringUtils.hasText(mobile)) {
			mobile = user.getMobilePhone();
		}
		Assert.hasText(mobile, "手机不允许为空.");

		Assert.hasText(vertifyCode, "未输入手机验证码.");

		String passwordEncode = MD5Util.getMD5Code(username + MD5Util.getMD5Code(password));
		if (!user.getPasswd().equals(passwordEncode)) {
			throw new Warning("输入的密码错误,请重新输入.");
		}

		VertifyCode vc = vertifyCodeService.getVertifyCode(username, mobile);
		if (vc == null || !vc.isEffective()) {
			throw new Warning("手机验证码已失效或未生成,请重新生成.");
		}

//		if (!vc.getCode().equals(vertifyCode)) {
//			throw new Warning("输入的手机验证码错误,请重新输入.");
//		}

		return true;
	}

	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {
		User user = userService.getUserByLoginName(username);

		if (user == null) {
			return null;
		}

		boolean isEtl = false;
		Set<Role> roles = user.getRoles();
		List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>(roles.size());
		for (Role role : roles) {
			final String roleName = role.getRoleName();

			// 这里ETL角色名称写死了
			if ("ETL开发工程师".equals(roleName)) {
				isEtl = true;
			}

			authorities.add(new GrantedAuthority() {

				private static final long serialVersionUID = 1L;

				@Override
				public String getAuthority() {
					return roleName;
				}

			});
		}

		UserGroup userGroup = userGroupRelationService.getUserGroupByUser(user.getUserId());
		VertifyCode vertifyCode = vertifyCodeService.getVertifyCode(username, user.getMobilePhone());

		AuthenticationUserDetails aud = new AuthenticationUserDetails();
		aud.setId(user.getUserId());
		aud.setUsername(user.getUserName());
		aud.setRealname(user.getRealName());
		aud.setPassword(user.getPasswd());
		aud.setAdministrator(userService.isAdministrator(user.getUserId()));
		aud.setIp(request.getRemoteAddr());
		aud.setUserGroupId(userGroup == null ? null : userGroup.getUserGroupId());
		aud.setUserGroupName(userGroup == null ? null : userGroup.getName());
		aud.setUserGroupAdministrator(userGroup == null ? false : userGroup.isAdministrator());
		aud.setEtl(isEtl);
		aud.setVertifyCode(vertifyCode.getCode());
		aud.setAuthorities(authorities);

		loginLoggerService.log(aud);

		return aud;
	}

	public static void main(String[] args) {
		String passwordEncode = MD5Util.getMD5Code("admin" + MD5Util.getMD5Code("123456"));
		System.out.println(passwordEncode);
	}

}
