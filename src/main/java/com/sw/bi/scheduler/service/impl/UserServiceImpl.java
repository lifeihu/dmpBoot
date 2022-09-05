package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.User;
import com.sw.bi.scheduler.service.RoleService;
import com.sw.bi.scheduler.service.UserGroupRelationService;
import com.sw.bi.scheduler.service.UserService;
import com.sw.bi.scheduler.util.EnumUtil.UserStatus;
import com.sw.bi.scheduler.util.MD5Util;
import com.sw.bi.scheduler.util.OperateAction;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.ResultTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate5.HibernateCallback;
import org.springframework.resolver.Warning;
import org.springframework.stereotype.Service;
import org.springframework.ui.PaginationSupport;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;

@Service("userService")
public class UserServiceImpl extends GenericServiceHibernateSupport<User> implements UserService {

	@Autowired
	private RoleService roleService;

	@Autowired
	private UserGroupRelationService userGroupRelationService;

	/*@Autowired
	private LoginLoggerService loginLoggerService;	

	@Autowired(required = false)
	private HttpServletRequest request;*/

	@Override
	public boolean isAdministrator(Long userId) {
		User user = get(userId);

		if (user.getIsAdmin())
			return true;

		// 如果用户本身不是系统管理员,则判断是否属于系统管理员角色
		Criteria query = roleService.createCriteria();
		query.createAlias("users", "user");
		query.add(Restrictions.eq("user.userId", userId));
		query.add(Restrictions.eq("isAdmin", true));
		query.setProjection(Projections.rowCount());
		Integer count = (Integer) query.uniqueResult();

		return count > 0;
	}

	@Override
	public User getAdministrator() {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("isAdmin", true));

		return (User) criteria.uniqueResult();
	}

	@Override
	public String changePassword(long userId, String password) {
		User user = get(userId);

		// 如果密码为空则随机产生6位的密码
		if (!StringUtils.hasText(password)) {
			password = RandomStringUtils.random(6, true, true);
			log.debug(user.getRealName() + "的新密码为: " + password);
		}
		
		user.setPasswd(MD5Util.getMD5Code(user.getUserName() + MD5Util.getMD5Code(password)));
		update(user, OperateAction.PASSWORD);

		return password;
	}

	@Override
	public void batchChangePassword() {
		Collection<User> users = this.queryAll();
		for (User user : users) {
			user.setPasswd(MD5Util.getMD5Code(user.getUserName() + MD5Util.getMD5Code(user.getPasswd())));
			getHibernateTemplate().update(user);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection<User> getNotAssignUsersByUserGroup() {
		return getHibernateTemplate().execute(new HibernateCallback<List<User>>() {

			@Override
			public List<User> doInHibernate(Session session) throws HibernateException {
				StringBuilder sql = new StringBuilder();
				sql.append("select * from user ");
				sql.append("where status = 0 ");
				sql.append("and user_id not in ");
				sql.append("(select distinct user_id from user_group_relation)");

				SQLQuery query = session.createSQLQuery(sql.toString());
				query.addEntity(User.class);

				return query.list();
			}

		});
	}

	/*@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {
		User user = getUserByName(username);

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

		AuthenticationUserDetails aud = new AuthenticationUserDetails();
		aud.setId(user.getUserId());
		aud.setUsername(user.getUserName());
		aud.setRealname(user.getRealName());
		aud.setPassword(user.getPasswd());
		aud.setAdministrator(isAdministrator(user.getUserId()));
		aud.setIp(request.getRemoteAddr());
		aud.setUserGroupId(userGroup == null ? null : userGroup.getUserGroupId());
		aud.setUserGroupName(userGroup == null ? null : userGroup.getName());
		aud.setUserGroupAdministrator(userGroup == null ? false : userGroup.isAdministrator());
		aud.setEtl(isEtl);
		aud.setAuthorities(authorities);

		loginLoggerService.log(aud);

		return aud;
	}*/

	@Override
	public void delete(String entityIds) {
		if (StringUtils.hasText(entityIds)) {
			Collection<User> users = query((Long[]) ConvertUtils.convert(entityIds.split(","), Long[].class));
			for (User user : users) {
				// 只是逻辑删除用户
				user.setStatus(UserStatus.REMOVE.ordinal());
				update(user, OperateAction.LOGIC_DELETE);
			}
		}
	}

	@Override
	public void saveOrUpdate(User user) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("status", UserStatus.NORMAL.ordinal()));
		criteria.add(Restrictions.eq("userName", user.getUserName()));
		criteria.add(Restrictions.not(Restrictions.eq("userId", user.getUserId())));
		User u = (User) criteria.uniqueResult();

		if (u != null) {
			throw new Warning("登录名(" + user.getUserName() + ")已经存在!");
		}

		if (user.getUserId() == null) {
			user.setPasswd(MD5Util.getMD5Code(user.getUserName() + MD5Util.getMD5Code(user.getPasswd())));

		} else {
			User oldUser = this.get(user.getUserId());
			getHibernateTemplate().evict(oldUser);

			user.setPasswd(oldUser.getPasswd());
		}

		super.saveOrUpdate(user);
	}

	@Override
	public PaginationSupport paging(Criteria criteria, int start, int limit, ResultTransformer resultTransformer) {
		if (criteria != null) {
			criteria.addOrder(Order.asc("status"));
		}
		return super.paging(criteria, start, limit, resultTransformer);
	}

	@Override
	public User intervene(User user) {
		user.setUserGroup(userGroupRelationService.getUserGroupByUser(user.getUserId()));

		return user;
	}

	@Override
	public User getUserByLoginName(String name) {
		if (!StringUtils.hasText(name))
			return null;

		Criteria criteria = this.createCriteria();
		criteria.add(Restrictions.eq("status", UserStatus.NORMAL.ordinal()));
		criteria.add(Restrictions.eq("userName", name));

		return (User) criteria.uniqueResult();
	}
}
