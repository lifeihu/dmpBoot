package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.Permission;
import com.sw.bi.scheduler.model.Resource;
import com.sw.bi.scheduler.service.PermissionService;
import com.sw.bi.scheduler.service.ResourceService;
import com.sw.bi.scheduler.service.RoleService;
import com.sw.bi.scheduler.service.UserService;
import com.sw.bi.scheduler.util.EnumUtil.ResourceType;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.PaginationSupport;
import org.springframework.util.StringUtils;

import java.util.*;

@Service("permissionService")
@SuppressWarnings("unchecked")
public class PermissionServiceImpl extends GenericServiceHibernateSupport<Permission> implements PermissionService {

	@Autowired
	private UserService userService;

	@Autowired
	private RoleService roleService;

	@Autowired
	private ResourceService resourceService;

	private boolean isMenu = false;

	@Override
	public List<Resource> getMenuTreeByUser(Long userId) {
		isMenu = true;
		List<Resource> resources = getResourceTreeByUser(userId);
		isMenu = false;

		return resources;
	}

	@Override
	public List<Resource> getResourceTreeByUser(Long userId) {
		return getResourcesByUser(userId, null, true);
	}

	@Override
	public List<Resource> getResourceTreeByUser(Long userId, Long parentResourceId) {
		return getResourcesByUser(userId, parentResourceId, false);
	}

	@Override
	public PaginationSupport getResourcesListByUser(Long userId, Long resourceId, Integer start, Integer limit) {
		Criteria query = null;
		PaginationSupport ps = null;

		if (userService.isAdministrator(userId)) {
			query = resourceService.createCriteria();

			if (isMenu)
				query.add(Restrictions.eq("menu", true));

			if (resourceId == null || resourceId == 1l)
				query.add(Restrictions.isNull("parent"));
			else {
				query.add(Restrictions.or(Restrictions.eq("id", resourceId), Restrictions.eq("parent.id", resourceId)));
				// query.add(Restrictions.or(Restrictions.eq("id", resourceId), Restrictions.sqlRestriction("{alias}.parent_id like ?", resourceId + "%", Hibernate.STRING)));
			}

			query.addOrder(Order.asc("parent.id"));
			query.addOrder(Order.asc("sortNo"));
			query.addOrder(Order.asc("id"));

			ps = resourceService.paging(query, start, limit);

		} else {
			query = createCriteria();
			query.createAlias("resource", "resource");

			if (isMenu)
				query.add(Restrictions.eq("resource.menu", true));

			if (!(resourceId == null || resourceId == 1l)) {
				query.add(Restrictions.or(Restrictions.eq("resource.resourceId", resourceId), Restrictions.eq("resource.parent.id", resourceId)));
				// query.add(Restrictions.or(Restrictions.eq("resource.resourceId", resourceId), Restrictions.sqlRestriction("resource1_.parent_id like ?", resourceId + "%", Hibernate.STRING)));
			}

			query.add(Restrictions.in("role.roleId", roleService.getRoleIdsByUser(userId)));
			query.setProjection(Projections.property("resource"));
			query.addOrder(Order.asc("resource.sortNo"));
			query.addOrder(Order.asc("resource.resourceId"));

			ps = paging(query, start, limit);
		}

		List<Object> results = ps.getPaginationResults();
		for (int i = 0; i < results.size(); i++) {
			Object resource = results.get(i);

			if (resource instanceof Permission) {
				resource = ((Permission) resource).getResource();
				ps.getPaginationResults().set(i, resource);
			}

			((Resource) resource).setChildren(null);
		}

		return ps;
	}

	@Override
	public List<Resource> getResourcesByRole(List<Long> roleIds) {
		if (roleIds.size() == 0) {
			return null;
		}

		Criteria query = createCriteria();
		query.createAlias("resource", "resource");

		if (isMenu)
			query.add(Restrictions.eq("resource.menu", true));

		query.add(Restrictions.in("role.roleId", roleIds));
		query.setProjection(Projections.property("resource"));
		query.addOrder(Order.asc("resource.parent"));
		query.addOrder(Order.asc("resource.sortNo"));
		query.addOrder(Order.asc("resource.resourceId"));

		// 组织资源树
		return createResourceTree(query.list());
	}

	@Override
	public List<Resource> getPermissionResourcesByRole(long roleId) {
		List<Long> roleIds = new ArrayList<Long>();
		roleIds.add(roleId);

		Criteria query = createCriteria();
		query.add(Restrictions.in("role.roleId", roleIds));
		query.setProjection(Projections.property("resource"));
		query.addOrder(Order.asc("resource.resourceId"));

		return query.list();
	}

	@Override
	public void grant(long roleId, List<Long> resourceIds) {
		Query query = getCurrentSession().createQuery("delete Permission where role.roleId = :roleId");
		query.setParameter("roleId", roleId);
		query.executeUpdate();

		if (resourceIds.size() > 0) {
			for (Long resourceId : resourceIds) {
				if (resourceId == 1)
					continue;

				save(new Permission(roleId, resourceId));
			}
		}
	}

	private List<Resource> getResourcesByRole(List<Long> roleIds, Long parentResourceId) {
		Criteria query = createCriteria();
		query.createAlias("resource", "resource");

		if (isMenu)
			query.add(Restrictions.eq("resource.menu", true));

		if (parentResourceId == null || parentResourceId == 1)
			query.add(Restrictions.isNull("resource.parent"));
		else
			query.add(Restrictions.eq("resource.parent.id", parentResourceId));

		query.add(Restrictions.in("role.roleId", roleIds));
		query.setProjection(Projections.property("resource"));
		query.addOrder(Order.asc("resource.sortNo"));
		query.addOrder(Order.asc("resource.resourceId"));

		return query.list();
	}

	/**
	 * 将给定的资源列表组织成树结构
	 * 
	 * @param resources
	 * @return
	 */
	private List<Resource> createResourceTree(List<Resource> resources) {
		Resource root = new Resource();
		Map<Long, Resource> mapping = new HashMap<Long, Resource>();
		// 不同数据库对null的排序不同，做个兼容
		for (Resource resource : resources) {
			// 此处必须先将children置空,否则在下面调用getChildren方法时会把所有子节点加载出来
			resource.setChildren(new LinkedHashSet<Resource>());

			Resource parent = resource.getParent();
			if (parent == null) {
				parent = root;
				parent.getChildren().add(resource);
				mapping.put(resource.getResourceId(), resource);
			}
		}


		for (Resource resource : resources) {
			// 此处必须先将children置空,否则在下面调用getChildren方法时会把所有子节点加载出来
			resource.setChildren(new LinkedHashSet<Resource>());

			Resource parent = resource.getParent();
			if (parent == null)
				continue;
			else
				parent = mapping.get(parent.getResourceId());
			parent.getChildren().add(resource);
			mapping.put(resource.getResourceId(), resource);
		}

		return new ArrayList<Resource>(root.getChildren());
	}

	/**
	 * 获得指定用户的资源
	 * 
	 * @param userId
	 * @param parentResourceId
	 * @param cascade
	 *            true: 一次获得所有资源, false: 只取以parentResourceId为父资源的所有资源
	 * @return
	 */
	private List<Resource> getResourcesByUser(Long userId, Long parentResourceId, boolean cascade) {
		Criteria query = null;
		List<Resource> resources = null;

		if (userService.isAdministrator(userId)) {
			query = resourceService.createCriteria();

			if (isMenu)
				query.add(Restrictions.eq("menu", true));

			if (!cascade) {
				if (parentResourceId == null || parentResourceId == 1l)
					query.add(Restrictions.isNull("parent"));
				else
					query.add(Restrictions.eq("parent.id", parentResourceId));
			}

			query.addOrder(Order.asc("parent.id"));
			query.addOrder(Order.asc("sortNo"));
			query.addOrder(Order.asc("id"));
			resources = query.list();

			if (cascade)
				resources = createResourceTree(resources);

		} else {
			if (!cascade)
				resources = getResourcesByRole(roleService.getRoleIdsByUser(userId), parentResourceId);
			else
				resources = getResourcesByRole(roleService.getRoleIdsByUser(userId));

		}

		if (!cascade) {
			for (Resource resource : resources) {
				resource.setLeaf(resource.getChildren().size() == 0);
				resource.setChildren(null);
			}
		}

		return resources;
	}

	@Override
	public void deleteByRole(long roleId) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("role.id", roleId));

		this.delete(criteria.list());
	}

	@Override
	public Map<String, String> loadDatabaseSecurityMetadataMapping() {
		Map<String, String> resourceMap = new HashMap<String, String>();

		Criteria criteria = createCriteria();
		criteria.createAlias("resource", "resource");
		criteria.add(Restrictions.eq("resource.type", ResourceType.URL.ordinal()));
		criteria.add(Restrictions.isNotNull("resource.url"));
		Collection<Permission> permissions = criteria.list();

		for (Permission permission : permissions) {
			String resource = "manage/" + permission.getResource().getUrl();
			String role = permission.getRole().getRoleName().trim();

			String roles = resourceMap.get(resource);
			if (StringUtils.hasText(roles))
				roles += "," + role;
			else
				roles = role;

			resourceMap.put(resource, roles);
		}

		System.out.println("have " + permissions.size() + " permissions.");

		return resourceMap;
	}
}
