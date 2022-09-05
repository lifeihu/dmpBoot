package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.Role;
import com.sw.bi.scheduler.model.User;
import com.sw.bi.scheduler.service.PermissionService;
import com.sw.bi.scheduler.service.RoleService;
import com.sw.bi.scheduler.service.UserService;
import org.apache.commons.beanutils.ConvertUtils;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate5.HibernateCallback;
import org.springframework.resolver.Warning;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@SuppressWarnings("unchecked")
public class RoleServiceImpl extends GenericServiceHibernateSupport<Role> implements RoleService {

	@Autowired
	private UserService userService;

	@Autowired
	private PermissionService permissionService;

	@Override
	public List<Long> getRoleIdsByUser(long userId) {
		Criteria query = createCriteria();
		query.createAlias("users", "user");
		query.add(Restrictions.eq("user.id", userId));
		query.setProjection(Projections.property("id"));
		return query.list();
	}

	@Override
	public Collection<Role> getNotAssignRolesByUser(Long userId) {
		User user = null;
		Collection<Role> userRoles = null;

		if (userId != null) {
			user = userService.get(userId);
			userRoles = user.getRoles();
		}

		if (user == null || userRoles == null || userRoles.size() == 0) {
			return this.queryAll();

		} else {

			Collection<Long> roleIds = new ArrayList<Long>();
			for (Role role : userRoles) {
				roleIds.add(role.getRoleId());
			}

			Criteria criteria = createCriteria();
			criteria.add(Restrictions.not(Restrictions.in("roleId", roleIds)));

			return criteria.list();
		}
	}

	@Override
	public void saveOrUpdate(Role role) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("roleName", role.getRoleName()));
		if (role.getRoleId() != null) {
			criteria.add(Restrictions.not(Restrictions.eq("roleId", role.getRoleId())));
		}
		Role r = (Role) criteria.uniqueResult();

		if (r != null) {
			throw new Warning("角色(" + role.getRoleName() + ")已经存在!");
		}

		super.saveOrUpdate(role);
	}

	@Override
	public void delete(String entityIds) {
		if (StringUtils.hasText(entityIds)) {
			final Long[] ids = (Long[]) ConvertUtils.convert(entityIds.split(","), Long[].class);

			getHibernateTemplate().execute(new HibernateCallback<Object>() {

				@Override
				public Object doInHibernate(Session session) throws HibernateException {
					for (Long id : ids) {
						// 删除用户与角色关系
						String sql = "delete from user_role where role_id = :roleId";
						SQLQuery query = session.createSQLQuery(sql);
						query.setParameter("roleId", id);
						query.executeUpdate();

						// 删除权限
						permissionService.deleteByRole(id);

						delete(get(id));
					}

					return null;
				}

			});

		}
	}

}
