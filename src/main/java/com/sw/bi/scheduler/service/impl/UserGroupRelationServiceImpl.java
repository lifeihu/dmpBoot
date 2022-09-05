package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.User;
import com.sw.bi.scheduler.model.UserGroup;
import com.sw.bi.scheduler.model.UserGroupRelation;
import com.sw.bi.scheduler.service.UserGroupRelationService;
import com.sw.bi.scheduler.service.UserGroupService;
import com.sw.bi.scheduler.service.UserService;
import com.sw.bi.scheduler.util.OperateAction;
import org.apache.commons.beanutils.ConvertUtils;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.internal.SessionImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate5.HibernateCallback;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class UserGroupRelationServiceImpl extends GenericServiceHibernateSupport<UserGroupRelation> implements UserGroupRelationService {

	@Autowired
	private UserService userService;

	@Autowired
	private UserGroupService userGroupService;

	@Override
	public void assignUsers(long userGroupId, String userId) {
		boolean hasUsers = StringUtils.hasText(userId);
		Collection<User> users = null;

		if (hasUsers) {
			users = userService.query((Long[]) ConvertUtils.convert(userId.split(","), Long.class));
			hasUsers = users.size() > 0;
		}

		// 分配用户前需要先解除已有用户
		this.unassignUsers(userGroupId, !hasUsers);

		if (!hasUsers) {
			return;
		}

		UserGroup userGroup = userGroupService.get(userGroupId);
		StringBuilder loggerContent = new StringBuilder();
		loggerContent.append("[用户组与用户关系].[").append(userGroup.getName()).append("(").append(userGroup.getUserGroupId()).append(") -> ");

		for (User user : users) {
			UserGroupRelation ugr = new UserGroupRelation();
			ugr.setUserGroupId(userGroupId);
			ugr.setUserId(user.getUserId());

			super.save(ugr);

			loggerContent.append(user.getRealName()).append("(").append(user.getUserName()).append("),");
		}

		getOperateLoggerService().log(OperateAction.ASSIGN, loggerContent.toString().replaceFirst(",$", "]"));
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection<User> getUsersByUserGroup(final long userGroupId, final boolean userGroupCascade) {
		//modify by zhoushasha 2016/5/5
		List<Long> groupIdList=new ArrayList<Long>();
		getGroupIds(userGroupId,groupIdList);
		StringBuffer groupIds=new StringBuffer();
		for(Long groupId:groupIdList){
			groupIds.append(groupId).append(",");
		}
		final String ids=groupIds.toString();
		return getHibernateTemplate().execute(new HibernateCallback<List<User>>() {

			@Override
			public List<User> doInHibernate(Session session) throws HibernateException {
				StringBuilder sql = new StringBuilder();

				sql.append("select u.* from user1 u ");
				sql.append("where u.status = 0 ");
				sql.append("and u.user_id in ");
				sql.append("(select ugr.user_id from user_group_relation ugr ");

				if (userGroupCascade) {
					sql.append("where ugr.user_group_id like '").append(userGroupId).append("%'");
				} else {
					//modify by zhoushasha 2016/5/5
					//sql.append("where ugr.user_group_id = ").append(userGroupId);
					sql.append("where ugr.user_group_id in ( ")
					.append(ids.substring(0, ids.length()-1)).append(" )");
				}
				sql.append(")");

				SQLQuery query = session.createSQLQuery(sql.toString());
				query.addEntity("u", User.class);

				return query.list();
			}

		});
	}
	//add by zhoushasha 2016/5/5
	public void getGroupIds(long userGroupId,List<Long> groupIdList) {
		groupIdList.add(userGroupId);
		Session session=getHibernateTemplate().getSessionFactory().getCurrentSession();
		String sql="select user_group_id  from user_group where parent_id=? ";
		PreparedStatement ptm=null;
		ResultSet rs=null;
		try {
			ptm=((SessionImpl)session).connection().prepareStatement(sql);
			ptm.setLong(1, userGroupId);
			rs=ptm.executeQuery();
			while(rs.next()){
				long childrenId=rs.getLong(1);
				getGroupIds(childrenId,groupIdList);
			}
			
		} catch (HibernateException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			try{
				if(rs!=null){
					rs.close();
				}
				if(ptm!=null){
					ptm.close();
				}
				
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	@Override
	public void unassignUsers(final long userGroupId, boolean allowLogger) {
		Collection<User> users = this.getUsersByUserGroup(userGroupId, false);

		if (users.size() == 0) {
			return;
		}

		StringBuilder loggerContent = new StringBuilder();

		if (allowLogger) {
			UserGroup userGroup = userGroupService.get(userGroupId);
			loggerContent.append("[用户组与用户关系].[").append(userGroup.getName()).append("(").append(userGroup.getUserGroupId()).append(") -> ");
		}

		for (final User user : users) {
			int result = getHibernateTemplate().execute(new HibernateCallback<Integer>() {

				@Override
				public Integer doInHibernate(Session session) throws HibernateException {
					StringBuilder sql = new StringBuilder();

					sql.append("delete from user_group_relation ");
					sql.append("where user_id = :userId ");
					sql.append("and user_group_id = :userGroupId");

					SQLQuery query = session.createSQLQuery(sql.toString());
					query.setLong("userId", user.getUserId());
					query.setLong("userGroupId", userGroupId);

					return query.executeUpdate();
				}

			});

			if (result == 0) {
				continue;
			}

			if (allowLogger) {
				loggerContent.append(user.getRealName()).append("(").append(user.getUserName()).append("),");
			}
		}

		getOperateLoggerService().log(OperateAction.UNASSIGN, loggerContent.toString().replaceFirst(",$", "]"));
	}

	@Override
	public UserGroup getUserGroupByUser(final long userId) {
		return getHibernateTemplate().execute(new HibernateCallback<UserGroup>() {

			@Override
			public UserGroup doInHibernate(Session session) throws HibernateException {
				StringBuilder sql = new StringBuilder();
				sql.append("select ug.* from user_group ug ");
				sql.append("where ug.active = true ");
				sql.append("and ug.user_group_id in ");
				sql.append("(select distinct ugr.user_group_id from user_group_relation ugr where ugr.user_id = :userId)");

				SQLQuery query = session.createSQLQuery(sql.toString());
				query.addEntity("ug", UserGroup.class);
				query.setLong("userId", userId);
				query.setMaxResults(1);

				return (UserGroup) query.uniqueResult();
			}

		});
	}

	@Override
	public Collection<UserGroup> getUserGroupsByUser(long userId) {
		final UserGroup userGroup = this.getUserGroupByUser(userId);
		if (userGroup == null) {
			return new ArrayList<UserGroup>();
		}

		return getHibernateTemplate().execute(new HibernateCallback<Collection<UserGroup>>() {

			@Override
			public Collection<UserGroup> doInHibernate(Session session) throws HibernateException {
				StringBuilder sql = new StringBuilder();
				sql.append("select * from user_group ug");
				sql.append(" where ug.active = true");
				sql.append(" and ug.user_group_id like '").append(userGroup.getUserGroupId()).append("%'");
				sql.append(" order by ug.user_group_id");

				SQLQuery query = session.createSQLQuery(sql.toString());
				query.addEntity("ug", UserGroup.class);

				return query.list();
			}

		});
	}
}
