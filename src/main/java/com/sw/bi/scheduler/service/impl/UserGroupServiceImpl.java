package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.UserGroup;
import com.sw.bi.scheduler.service.AlertSystemConfigService;
import com.sw.bi.scheduler.service.UserGroupRelationService;
import com.sw.bi.scheduler.service.UserGroupService;
import com.sw.bi.scheduler.util.OperateAction;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.resolver.Warning;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;

@Service
public class UserGroupServiceImpl extends GenericServiceHibernateSupport<UserGroup> implements UserGroupService {

	@Autowired
	private UserGroupRelationService userGroupRelationService;

	@Autowired
	private AlertSystemConfigService alertSystemConfigService;

	@Override
	public void logicDelete(UserGroup userGroup) {
		if (userGroup == null) {
			return;
		}

		Criteria criteria = createCriteria();
		criteria.add(Restrictions.sqlRestriction("{alias}.user_group_id like ?", userGroup.getUserGroupId() + "%", StandardBasicTypes.STRING));
		Collection<UserGroup> userGroups = criteria.list();
		for (UserGroup ug : userGroups) {
			ug.setActive(false);
			super.update(ug, OperateAction.LOGIC_DELETE);
		}
	}

	@Override
	public void recovery(UserGroup userGroup) {
		if (userGroup == null) {
			return;
		}

		Criteria criteria = createCriteria();
		criteria.add(Restrictions.sqlRestriction("{alias}.user_group_id like ?", userGroup.getUserGroupId() + "%", StandardBasicTypes.STRING));
		Collection<UserGroup> userGroups = criteria.list();
		for (UserGroup ug : userGroups) {
			ug.setActive(true);
			super.update(ug, OperateAction.RECOVERY);
		}
	}

	@Override
	public void saveOrUpdate(UserGroup userGroup) {
		boolean isCreate = userGroup.getUserGroupId() == null;

		if (isCreate) {
			Long parentId = userGroup.getParentId();

			userGroup.setUserGroupId(this.getNextLayerNo(parentId));
			userGroup.setSortNo(this.getNextSortNo(parentId));
		}

		String hdfsPath = userGroup.getHdfsPath();
		if (!String.valueOf(userGroup.getUserGroupId()).startsWith("10") && !StringUtils.hasText(hdfsPath)) {
			// 非总部用户组必须配置数据目录
			throw new Warning("用户组(" + userGroup.getName() + ")未设置数据目录");
		}

		super.saveOrUpdate(userGroup);

		if (isCreate) {
			// 如果是新建用户组则顺便将用户组的告警配置也同时创建了
			alertSystemConfigService.createDefault(userGroup.getUserGroupId());
		}
	}

	@Override
	public void delete(UserGroup userGroup) {
		if (userGroup == null) {
			return;
		}

		Criteria criteria = createCriteria();
		criteria.add(Restrictions.sqlRestriction("{alias}.user_group_id like ?", userGroup.getUserGroupId() + "%", StandardBasicTypes.STRING));
		criteria.addOrder(Order.desc("userGroupId"));
		Collection<UserGroup> userGroups = criteria.list();

		for (UserGroup ug : userGroups) {
			// 解除用户组与用户关系
			userGroupRelationService.unassignUsers(ug.getUserGroupId(), true);

			super.delete(ug);
		}

		alertSystemConfigService.delteByUserGroup(userGroup.getUserGroupId());
	}

	/**
	 * 获得指定指定上级用户组的下一个层次码
	 * 
	 * @param parentId
	 * @return
	 */
	private Long getNextLayerNo(Long parentId) {
		Criteria criteria = createCriteria();
		if (parentId == null) {
			criteria.add(Restrictions.isNull("parentId"));
		} else {
			criteria.add(Restrictions.eq("parentId", parentId));
		}
		criteria.setProjection(Projections.max("userGroupId"));
		Long maxLayerNo = (Long) criteria.uniqueResult();

		Long nextLayerNo = null;

		if (maxLayerNo == null) {
			if (parentId == null) {
				nextLayerNo = 10l;
			} else {
				nextLayerNo = Long.valueOf(parentId + "01");
			}
		} else {
			nextLayerNo = maxLayerNo.longValue() + 1;
		}

		return nextLayerNo;
	}

	/**
	 * 获得指定上级用户组下的下一个序号
	 * 
	 * @param parentId
	 * @return
	 */
	private int getNextSortNo(Long parentId) {
		Criteria criteria = createCriteria();
		if (parentId == null) {
			criteria.add(Restrictions.isNull("parentId"));
		} else {
			criteria.add(Restrictions.eq("parentId", parentId));
		}
		criteria.setProjection(Projections.max("sortNo"));
		Integer maxSortNo = (Integer) criteria.uniqueResult();

		maxSortNo = maxSortNo == null ? 0 : maxSortNo.intValue();

		return maxSortNo + 1;
	}

}
