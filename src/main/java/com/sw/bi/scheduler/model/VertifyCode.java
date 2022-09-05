package com.sw.bi.scheduler.model;

import com.sw.bi.scheduler.util.Configure;
import com.sw.bi.scheduler.util.DateUtil;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

public class VertifyCode implements Serializable {

	private Long vertifyCodeId;

	/**
	 * 手机
	 */
	private String mobile;

	/**
	 * 验证码
	 */
	private String code;

	/**
	 * 登录用户
	 */
	private String username;

	/**
	 * 验证码使用次数
	 */
	private int useTimes = 0;

	private Date createTime;
	private Date updateTime;

	public Long getVertifyCodeId() {
		return vertifyCodeId;
	}

	public void setVertifyCodeId(Long vertifyCodeId) {
		this.vertifyCodeId = vertifyCodeId;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public int getUseTimes() {
		return useTimes;
	}

	public void setUseTimes(int useTimes) {
		this.useTimes = useTimes;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	/**
	 * 验证码是否有效
	 * 
	 * @return
	 */
	public boolean isEffective() {
		int hour = Configure.property(Configure.VERTIFY_CODE_TIMEOUT, Integer.class);
		Calendar calendar = DateUtil.getCalendar(this.getCreateTime());
		calendar.add(Calendar.HOUR_OF_DAY, hour);

		return DateUtil.now().getTime() <= calendar.getTimeInMillis();
	}

}
