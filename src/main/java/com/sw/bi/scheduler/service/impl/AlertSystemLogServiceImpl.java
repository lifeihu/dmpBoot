package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.AlertSystemLog;
import com.sw.bi.scheduler.model.User;
import com.sw.bi.scheduler.model.UserGroup;
import com.sw.bi.scheduler.service.AlertSystemLogService;
import com.sw.bi.scheduler.service.UserGroupRelationService;
import com.sw.bi.scheduler.service.UserGroupService;
import com.sw.bi.scheduler.service.UserService;
import org.hibernate.Criteria;
import org.hibernate.criterion.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.ConditionModel;
import org.springframework.ui.PaginationSupport;

import java.util.*;

@Service
@SuppressWarnings("unchecked")
public class AlertSystemLogServiceImpl extends GenericServiceHibernateSupport<AlertSystemLog> implements AlertSystemLogService {

	@Autowired
	private UserService userService;

	@Autowired
	private UserGroupService userGroupService;

	@Autowired
	private UserGroupRelationService userGroupRelationService;

	@Override
	public PaginationSupport pagingGroup(ConditionModel cm) {
		/*Integer jobStatus = cm.getValue("jobStatus", Integer.class);
		cm.removeCondition("jobStatus");*/

		PaginationSupport paging = new PaginationSupport(cm.getStart(), cm.getLimit());

		Long userGroupId = cm.getValue("userGroupId", Long.class);
		cm.removeCondition("userGroupId");

		if (userGroupId == null) {
			return paging;
		}

		// 告警日志需要只查出与指定用户组及子用户组相关的记录
		// 所以这里需要得到指定用户组及子用户组下所有用户的登录名(因为日志表中责任人存的是登录名)
		Collection<String> loginNames = new ArrayList<String>();
		Map<String, Long> loginNameAndUserIdMapping = new HashMap<String, Long>();

		UserGroup userGroup = userGroupService.get(userGroupId);

		// 获得指定用户组及子用户组下所有用户
		if (!userGroup.isAdministrator()) {
			Collection<User> users = userGroupRelationService.getUsersByUserGroup(userGroupId, true);
			for (User user : users) {
				loginNames.add(user.getUserName());
				loginNameAndUserIdMapping.put(user.getUserName(), user.getUserId());
			}
		}

		Criteria criteria = createCriteria(cm);
		criteria.addOrder(Order.desc("alertTime"));

		if (loginNames.size() > 0) {
			criteria.add(Restrictions.in("dutyOfficer", loginNames));
		}

		ProjectionList pl = Projections.projectionList();
		pl.add(Property.forName("alertDate").group());
		pl.add(Property.forName("jobId").group());
		pl.add(Property.forName("taskDate").group());
		criteria.setProjection(pl);
		paging.setTotal(criteria.list().size());

		pl.add(Property.forName("alertSystemLogId"));
		pl.add(Property.forName("taskId"));
		pl.add(Property.forName("jobName"));
		pl.add(Property.forName("jobStatus"));
		pl.add(Property.forName("dutyOfficer"));
		pl.add(Property.forName("alertType"));
		pl.add(Property.forName("alertWay"));
		pl.add(Property.forName("alertTime").max());
		// pl.add(Projections.max("alertTime"));
		pl.add(Property.forName("alertStatus"));
		pl.add(Property.forName("receiver"));
		pl.add(Property.forName("createTime"));
		pl.add(Property.forName("updateTime"));
		// pl.add(Property.forName("taskDate"));
		pl.add(Property.forName("jobType"));
		pl.add(Property.forName("cycleType"));
		pl.add(Property.forName("settingTime"));
		pl.add(Projections.rowCount());
		criteria.setProjection(pl);
		criteria.setFirstResult(cm.getStart());
		criteria.setMaxResults(cm.getLimit());

		Collection<Object[]> results = criteria.list();
		for (Object[] result : results) {
			Long userId = null;
			if (userGroup.isAdministrator()) {
				User user = userService.getUserByLoginName((String) result[7]);
				userId = user == null ? null : user.getUserId();

			} else {
				loginNameAndUserIdMapping.get((String) result[7]);
			}

			AlertSystemLog alertSystemLog = new AlertSystemLog();
			alertSystemLog.setAlertDate((Date) result[0]);
			alertSystemLog.setJobId((Long) result[1]);
			alertSystemLog.setTaskDate((Date) result[2]);
			alertSystemLog.setAlertSystemLogId((Long) result[3]);
			alertSystemLog.setTaskId((Long) result[4]);
			alertSystemLog.setJobName((String) result[5]);
			alertSystemLog.setJobStatus((Integer) result[6]);
			alertSystemLog.setDutyOfficer((String) result[7]);
			alertSystemLog.setUserGroup(userId == null ? null : userGroupRelationService.getUserGroupByUser(userId));
			alertSystemLog.setAlertType((Integer) result[8]);
			alertSystemLog.setAlertWay((Integer) result[9]);
			alertSystemLog.setAlertTime((Date) result[10]);
			alertSystemLog.setAlertStatus((Integer) result[11]);
			alertSystemLog.setReceiver((String) result[12]);
			alertSystemLog.setCreateTime((Date) result[13]);
			alertSystemLog.setUpdateTime((Date) result[14]);
			// alertSystemLog.setTaskDate((Date) result[14]);
			alertSystemLog.setJobType((Long) result[15]);
			alertSystemLog.setCycleType((Integer) result[16]);
			alertSystemLog.setSettingTime((Date) result[17]);
			alertSystemLog.setAlertCount((Integer) result[18]);

			paging.getPaginationResults().add(alertSystemLog);
		}

		return paging;
	}

	@Override
	public AlertSystemLog intervene(AlertSystemLog alertSystemLog) {
		User user = userService.getUserByLoginName(alertSystemLog.getDutyOfficer());
		UserGroup userGroup = userGroupRelationService.getUserGroupByUser(user.getUserId());
		alertSystemLog.setUserGroup(userGroup);

		return alertSystemLog;
	}

}
