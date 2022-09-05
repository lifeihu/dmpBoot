package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.VertifyCode;
import com.sw.bi.scheduler.service.VertifyCodeService;
import org.apache.commons.lang.RandomStringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class VertifyCodeServiceImpl extends GenericServiceHibernateSupport<VertifyCode> implements VertifyCodeService {

	@Override
	public VertifyCode getVertifyCode(String username, String mobile) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("username", username));
		criteria.add(Restrictions.eq("mobile", mobile));
		criteria.addOrder(Order.desc("createTime"));
		criteria.setMaxResults(1);

		return (VertifyCode) criteria.uniqueResult();
	}

	@Override
	public VertifyCode getVertifyCode(String username, String mobile, String vertifyCode) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("username", username));
		criteria.add(Restrictions.eq("mobile", mobile));
		criteria.add(Restrictions.eq("code", vertifyCode));

		return (VertifyCode) criteria.uniqueResult();
	}

	@Override
	public String generate(String username, String mobile) {
		VertifyCode vertifyCode = this.getVertifyCode(username, mobile);
		if (vertifyCode != null && vertifyCode.isEffective()) {
			return vertifyCode.getCode();
		}
// 		by whl 海信暂时没有验证码，生成一个固定的验证码123456
		String code = RandomStringUtils.random(6, false, true);
//		String code = "123456";
		vertifyCode = new VertifyCode();
		vertifyCode.setMobile(mobile);
		vertifyCode.setCode(code);
		vertifyCode.setUsername(username);
		vertifyCode.setCreateTime(new Date());
		super.save(vertifyCode);

		return code;
	}

	@Override
	public void use(String username, String mobile, String vertifyCode) {
		VertifyCode vc = this.getVertifyCode(username, mobile, vertifyCode);
		if (vc != null) {
			vc.setUseTimes(vc.getUseTimes() + 1);
			super.saveOrUpdate(vc);
		}
	}

}
