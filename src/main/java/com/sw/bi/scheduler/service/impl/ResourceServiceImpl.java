package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.Resource;
import com.sw.bi.scheduler.service.ResourceService;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@SuppressWarnings("unchecked")
public class ResourceServiceImpl extends GenericServiceHibernateSupport<Resource> implements ResourceService {

	@Override
	public void save(Resource resource) {
		if (resource == null)
			return;

		if (resource.getResourceId() == null) {
			resource.setResourceId(generateResourceId(resource.getParent() == null ? null : resource.getParent().getResourceId()));

			getHibernateTemplate().save(resource);

		} else {
			super.update(resource);

		}
	}

	@Override
	public long generateResourceId(Long parentId) {
		Criteria query = createCriteria();
		/*query.setMaxResults(1);
		query.setProjection(Projections.property("resourceId"));
		query.addOrder(Order.desc("resourceId"));*/
		query.setProjection(Projections.max("resourceId"));
		if (parentId == null || parentId == 1) {
			query.add(Restrictions.isNull("parent"));
			query.add(Restrictions.not(Restrictions.eq("resourceId", 99l)));
		} else
			query.add(Restrictions.eq("parent.id", parentId));
		Long maxResourceId = (Long) query.uniqueResult();

		if (maxResourceId != null) {
			return maxResourceId + 1;
		} else {
			if (parentId == null || parentId == 1)
				return 10l;
			else
				return Long.parseLong(parentId + "01");
		}
	}

	@Override
	public List<Resource> getChildrenResources(Long parentId, Boolean onceLoad) {
		Criteria query = createCriteria();
		if (parentId == 1)
			query.add(Restrictions.isNull("parent"));
		else
			query.add(Restrictions.eq("parent.id", parentId));
		query.addOrder(Order.asc("sortNo"));
		query.addOrder(Order.asc("id"));

		// 将查询结果直接放入DTO对象
		// query.setResultTransformer(Transformers.aliasToBean(TreeNodeDTO.class));

		List<Resource> resources = query.list();
		for (Resource resource : resources) {
			if (onceLoad == Boolean.FALSE) {
				resource.setLeaf(resource.getChildren().size() == 0);
				resource.setChildren(null);
			}
		}

		return resources;
	}
}
