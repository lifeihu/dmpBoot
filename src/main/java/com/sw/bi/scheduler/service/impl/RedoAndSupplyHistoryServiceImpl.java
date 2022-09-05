package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.RedoAndSupplyHistory;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.service.RedoAndSupplyHistoryService;
import com.sw.bi.scheduler.service.UserGroupRelationService;
import com.sw.bi.scheduler.util.Configure.TaskAction;
import com.sw.bi.scheduler.util.Configure.TaskFlag;
import com.sw.bi.scheduler.util.Configure.TaskForegroundStatus;
import com.sw.bi.scheduler.util.DateUtil;
import org.apache.commons.lang.RandomStringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.ConditionModel;
import org.springframework.ui.PaginationSupport;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Service
@SuppressWarnings("unchecked")
public class RedoAndSupplyHistoryServiceImpl extends GenericServiceHibernateSupport<RedoAndSupplyHistory> implements RedoAndSupplyHistoryService {

	@Autowired
	private UserGroupRelationService userGroupRelationService;

	@Override
	public String createOperateNo(Date operateDate, TaskAction taskAction/*, String batchOperateNo*/) {
		String prefix = DateUtil.format(operateDate, "yyyyMMddHHmmss");
		String operateNo = prefix + RandomStringUtils.random(5, false, true);

		// 批量操作编号
		/*if (StringUtils.hasText(batchOperateNo)) {
			operateNo += "-" + batchOperateNo;
		}*/

		switch (taskAction) {
			case REDO:
				operateNo += "_RD";
				break;
			case REDO_CHILDREN:
				operateNo += "_RDC";
				break;
			case REDO_BATCH:
				operateNo += "_RDB";
				break;
			case PARALLEL_SUPPLY:
				operateNo += "_PS";
				break;
			case PARALLEL_SUPPLY_CHILDREN:
				operateNo += "_PSC";
				break;
			case PARALLEL_SUPPLY_BATCH:
				operateNo += "_PSB";
				break;
			case SERIAL_SUPPLY:
				operateNo += "_SS";
				break;
			case SERIAL_SUPPLY_CHILDREN:
				operateNo += "_SSC";
				break;
			case SERIAL_SUPPLY_BATCH:
				operateNo += "_SSB";
				break;
		}

		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("operateDate", operateDate));
		criteria.add(Restrictions.eq("operateNo", operateNo));
		if (count(criteria) > 0) {
			// 校验生成的批次号是否已经使用过,如果已经使用过则重新生成
			return createOperateNo(operateDate, taskAction/*, batchOperateNo*/);
		}

		return operateNo;
	}

	@Override
	public RedoAndSupplyHistory create(Task task, boolean master, String operateNo, Long operateBy, Date operateDate, TaskAction taskAction) {
		RedoAndSupplyHistory rash = this.getRedoAndSupplyHistoryByTask(operateNo, task.getTaskId());
		if (rash != null) {
			return rash;
		}

		rash = new RedoAndSupplyHistory();
		rash.setOperateNo(operateNo);
		rash.setOperateMan(operateBy);
		rash.setOperateDate(operateDate);
		rash.setFlag(taskAction.indexOf());
		rash.setIsMaster(master);
		rash.setJobId(task.getJobId());
		rash.setJobName(task.getJobName());
		rash.setCycleType(task.getCycleType());
		rash.setTaskDate(task.getTaskDate());
		rash.setTaskId(task.getTaskId());
		rash.setDutyMan(task.getDutyOfficer());
		rash.setSettingTime(task.getSettingTime());
		rash.setCreateTime(DateUtil.now());
		if (task.isRoot()) {
			rash.setTaskStatus(TaskForegroundStatus.RUN_SUCCESS.indexOf());
			rash.setBeginTime(DateUtil.now());
			rash.setEndTime(rash.getBeginTime());
		} else {
			rash.setTaskStatus(TaskForegroundStatus.NOT_RUNNING.indexOf());
		}

		saveOrUpdate(rash);

		return rash;
	}

	@Override
	public boolean taskRunBegin(Task task) {
		if (task.getFlag() == TaskFlag.SYSTEM.indexOf()) {
			return true;
		}

		// 当重跑过后再进行模拟后台操作，此时flag已经不是“系统”而是“重跑”，但批号已经不存在了，所以需要加判断，否则会抛异常
		// 如果为重跑或补数据操作则需要对以下字段进行回填
		/*		重跑过的任务，运行成功后，重跑的状态回填以后，task的operate_no会被置空，而flag依然是2（重跑）
				这个时候，再去模拟后台执行这个任务，代码中就：
				跳过了if (task.getFlag() == TaskFlag.SYSTEM.indexOf()) {这个判断
				而RedoAndSupplyHistory rash = getRedoAndSupplyHistoryByTask(task.getOperateNo(), task.getTaskId());得到一个空对象
				所以就有BUG了*/
		if (StringUtils.hasText(task.getOperateNo())) {
			RedoAndSupplyHistory rash = getRedoAndSupplyHistoryByTask(task.getOperateNo(), task.getTaskId());
			rash.setTaskStatus(TaskForegroundStatus.RUNNING.indexOf());
			rash.setBeginTime(task.getTaskBeginTime());
			rash.setEndTime(null);
			rash.setActionId(task.getLastActionId());
			rash.setUpdateTime(DateUtil.now());
			update(rash);
		}

		return true;
	}

	@Override
	public boolean taskRunFinished(Task task, boolean runSuccess) {
		if (task.getFlag() == TaskFlag.SYSTEM.indexOf()) {
			return true;
		}

		RedoAndSupplyHistory rash = getRedoAndSupplyHistoryByTask(task.getOperateNo(), task.getTaskId());
		if (rash != null) {
			rash.setTaskStatus(runSuccess ? TaskForegroundStatus.RUN_SUCCESS.indexOf() : TaskForegroundStatus.RUN_FAILURE.indexOf());
			rash.setEndTime(task.getTaskEndTime());
			rash.setUpdateTime(DateUtil.now());
			update(rash);
		}

		return true;
	}

	@Override
	public RedoAndSupplyHistory getRedoAndSupplyHistoryByTask(String operateNo, Long taskId) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("operateNo", operateNo));
		criteria.add(Restrictions.eq("taskId", taskId));

		// 因为补数据时偶尔会出现同批号有相同TaskID,所以这里强制结果集只有一条数据
		criteria.setMaxResults(1);

		return (RedoAndSupplyHistory) criteria.uniqueResult();
	}

	@Override
	public PaginationSupport pagingRedoMasters(ConditionModel cm) {
		Integer taskStatus = cm.getValue("taskStatus", Integer.class);
		cm.removeCondition("taskStatus");

		Criteria criteria = createCriteria(cm);
		criteria.addOrder(Order.desc("redoAndSupplyHistoryId"));
		PaginationSupport paging = super.paging(criteria, cm.getStart(), cm.getLimit());

		List<Object> newResults = new ArrayList<Object>();
		List<Object> results = paging.getPaginationResults();
		for (int i = 0; i < results.size(); i++) {
			RedoAndSupplyHistory rash = (RedoAndSupplyHistory) results.get(i);
			rash.setUserGroup(userGroupRelationService.getUserGroupByUser(rash.getDutyMan()));
			rash.setOperateUserGroup(userGroupRelationService.getUserGroupByUser(rash.getOperateMan()));
			getHibernateTemplate().evict(rash);

			Collection<RedoAndSupplyHistory> children = this.getRedoHistoriesByMaster(rash.getOperateNo()/*, rash.getRedoAndSupplyHistoryId()*/);
			if (children.size() > 0) {
				int childrenCount = children.size();
				int notRunningCount = 0, runningCount = 0, successCount = 0, failureCount = 0;
				Date minBeginTime = null, minEndTime = null, maxEndTime = null, failureTime = null;

				// 遍历所有子任务，并计算出最早开始时间、最早结束时间和最晚结束时间
				for (RedoAndSupplyHistory child : children) {
					if (minBeginTime == null || child.getBeginTime() == null || child.getBeginTime().getTime() < minBeginTime.getTime()) {
						minBeginTime = child.getBeginTime();
					}

					if (minEndTime == null || child.getEndTime() == null || child.getEndTime().getTime() < minEndTime.getTime()) {
						minEndTime = child.getEndTime();
					}

					if (maxEndTime == null || child.getEndTime() == null || child.getEndTime().getTime() > maxEndTime.getTime()) {
						maxEndTime = child.getEndTime();
					}

					int childTaskStatus = child.getTaskStatus();
					if (TaskForegroundStatus.NOT_RUNNING.indexOf() == childTaskStatus) {
						notRunningCount += 1;
					} else if (TaskForegroundStatus.RUNNING.indexOf() == childTaskStatus) {
						runningCount += 1;
					} else if (TaskForegroundStatus.RUN_SUCCESS.indexOf() == childTaskStatus) {
						successCount += 1;
					} else if (TaskForegroundStatus.RUN_FAILURE.indexOf() == childTaskStatus) {
						failureCount += 1;
						failureTime = child.getEndTime();
					}
				}

				// 主任务的开始时间为所有子任务中最早的开始时间(未运行时为空)
				// 主任务的结束时间：
				//   如果所有子任务都是未运行则为空
				//   如果所有子任务都运行成功则为最晚结束时间
				//   如果有任何一条子任务运行失败则为最早结束时间
				//   剩下最后一种情况则为运行中，那结束时间为空
				rash.setBeginTime(minBeginTime);
				if (notRunningCount == childrenCount) {
					rash.setTaskStatus(TaskForegroundStatus.NOT_RUNNING.indexOf());
					rash.setEndTime(null);
				} else if (successCount == childrenCount) {
					rash.setTaskStatus(TaskForegroundStatus.RUN_SUCCESS.indexOf());
					rash.setEndTime(maxEndTime);
				} else if (failureCount > 0) {
					rash.setTaskStatus(TaskForegroundStatus.RUN_FAILURE.indexOf());
					rash.setEndTime(failureTime);
				} else {
					rash.setTaskStatus(TaskForegroundStatus.RUNNING.indexOf());
					rash.setEndTime(null);
				}

				paging.getPaginationResults().set(i, rash);
			}

			if (taskStatus != null) {
				if (!taskStatus.equals(rash.getTaskStatus())) {
					continue;
				}
			}

			newResults.add(rash);
		}

		paging.setPaginationResults(newResults);
		return paging;
	}

	@Override
	public PaginationSupport pagingSupplyMasters(ConditionModel cm) {
		Integer taskStatus = cm.getValue("taskStatus", Integer.class);
		cm.removeCondition("taskStatus");

		String operateNo = cm.getValue("operateNo", String.class);
		boolean needGroup = !StringUtils.hasText(operateNo);
		PaginationSupport paging = null;

		Criteria criteria = createCriteria(cm);

		if (needGroup) {
			criteria.addOrder(Order.desc("redoAndSupplyHistoryId"));

			paging = new PaginationSupport();

			ProjectionList projections = Projections.projectionList();
			projections.add(Property.forName("operateNo").group());
			criteria.setProjection(projections);
			paging.setTotal(criteria.list().size());

			projections.add(Property.forName("jobId"));
			projections.add(Property.forName("jobName"));
			projections.add(Property.forName("dutyMan"));
			projections.add(Property.forName("taskStatus"));
			projections.add(Property.forName("taskDate"));
			projections.add(Property.forName("operateDate"));
			projections.add(Property.forName("operateMan"));
			projections.add(Property.forName("createTime"));
			projections.add(Property.forName("redoAndSupplyHistoryId"));
			projections.add(Property.forName("isMaster"));
			projections.add(Property.forName("cycleType"));
			projections.add(Property.forName("settingTime"));
			projections.add(Projections.min("taskDate"));
			projections.add(Projections.max("taskDate"));
			criteria.setProjection(projections);

			// 统计分页结果
			criteria.setFirstResult(cm.getStart());
			criteria.setMaxResults(cm.getLimit());
			List<Object[]> results = criteria.list();
			for (int i = 0; i < results.size(); i++) {
				Object[] result = results.get(i);
				RedoAndSupplyHistory rash = new RedoAndSupplyHistory();
				rash.setOperateNo((String) result[0]);
				rash.setJobId((Long) result[1]);
				rash.setJobName((String) result[2]);
				rash.setDutyMan((Long) result[3]);
				rash.setTaskStatus((Integer) result[4]);
				rash.setTaskDate((Date) result[5]);
				rash.setOperateDate((Date) result[6]);
				rash.setOperateMan((Long) result[7]);
				rash.setCreateTime((Date) result[8]);
				rash.setRedoAndSupplyHistoryId((Long) result[9]);
				rash.setIsMaster((Boolean) result[10]);
				rash.setCycleType((Integer) result[11]);
				rash.setSettingTime((Date) result[12]);
				rash.setMinTaskDate((Date) result[13]);
				rash.setMaxTaskDate((Date) result[14]);

				paging.getPaginationResults().add(rash);
			}

		} else {
			criteria.addOrder(Order.asc("taskDate"));
			paging = super.paging(criteria, cm.getStart(), cm.getLimit());

		}

		List<Object> newResults = new ArrayList<Object>();
		List<Object> results = paging.getPaginationResults();
		for (int i = 0; i < results.size(); i++) {
			RedoAndSupplyHistory rash = (RedoAndSupplyHistory) results.get(i);
			rash.setUserGroup(userGroupRelationService.getUserGroupByUser(rash.getDutyMan()));
			rash.setOperateUserGroup(userGroupRelationService.getUserGroupByUser(rash.getOperateMan()));
			getHibernateTemplate().evict(rash);

			Collection<RedoAndSupplyHistory> children = null;
			if (needGroup) {
				children = this.getSupplyMasterHistoriesByOperateNo(rash.getOperateNo());
			} else {
				children = this.getSupplyHistoriesByMaster(rash.getOperateNo(), rash.getTaskDate());
			}
			if (children.size() > 0) {
				int childrenCount = children.size();
				int notRunningCount = 0, runningCount = 0, successCount = 0, failureCount = 0, cancelCount = 0;
				Date minBeginTime = null, minEndTime = null, maxEndTime = null, failureTime = null;

				for (RedoAndSupplyHistory child : children) {
					if (minBeginTime == null || child.getBeginTime() == null || child.getBeginTime().getTime() < minBeginTime.getTime()) {
						minBeginTime = child.getBeginTime();
					}

					if (minEndTime == null || child.getEndTime() == null || child.getEndTime().getTime() < minEndTime.getTime()) {
						minEndTime = child.getEndTime();
					}

					if (maxEndTime == null || child.getEndTime() == null || child.getEndTime().getTime() > maxEndTime.getTime()) {
						maxEndTime = child.getEndTime();
					}

					int childTaskStatus = child.getTaskStatus();
					if (TaskForegroundStatus.NOT_RUNNING.indexOf() == childTaskStatus) {
						notRunningCount += 1;
					} else if (TaskForegroundStatus.RUNNING.indexOf() == childTaskStatus) {
						runningCount += 1;
					} else if (TaskForegroundStatus.RUN_SUCCESS.indexOf() == childTaskStatus) {
						successCount += 1;
					} else if (TaskForegroundStatus.CANCEL_SUPPLY.indexOf() == childTaskStatus) {
						cancelCount += 1;
						notRunningCount += 1; // 取消也属于未运行状态
					} else if (TaskForegroundStatus.RUN_FAILURE.indexOf() == childTaskStatus) {
						failureCount += 1;
						if (failureTime == null) {
							failureTime = child.getEndTime();
						}
					}
				}

				rash.setBeginTime(minBeginTime);
				if (cancelCount == childrenCount) {
					rash.setTaskStatus(TaskForegroundStatus.CANCEL_SUPPLY.indexOf());
					rash.setEndTime(null);
				} else if (notRunningCount == childrenCount) {
					rash.setTaskStatus(TaskForegroundStatus.NOT_RUNNING.indexOf());
					rash.setEndTime(null);
				} else if (successCount == childrenCount) {
					rash.setTaskStatus(TaskForegroundStatus.RUN_SUCCESS.indexOf());
					rash.setEndTime(maxEndTime);
				} else if (failureCount > 0) {
					rash.setTaskStatus(TaskForegroundStatus.RUN_FAILURE.indexOf());
					rash.setEndTime(failureTime);
				} else {
					rash.setTaskStatus(TaskForegroundStatus.RUNNING.indexOf());
					rash.setEndTime(null);
				}

				paging.getPaginationResults().set(i, rash);
			}

			if (taskStatus != null) {
				if (!taskStatus.equals(rash.getTaskStatus())) {
					continue;
				}
			}

			newResults.add(rash);
		}

		paging.setPaginationResults(newResults);
		return paging;
	}

	@Override
	public RedoAndSupplyHistory intervene(RedoAndSupplyHistory rash) {
		rash.setUserGroup(userGroupRelationService.getUserGroupByUser(rash.getDutyMan()));
		rash.setOperateUserGroup(userGroupRelationService.getUserGroupByUser(rash.getOperateMan()));

		return rash;
	}

	/**
	 * 获得指定主任务及批号下的所有重跑子任务
	 * 
	 * @param redoAndSupplyHistoryId
	 * @return
	 */
	private Collection<RedoAndSupplyHistory> getRedoHistoriesByMaster(String operateNo/*, long redoAndSupplyHistoryId*/) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("operateNo", operateNo));
		// criteria.add(Restrictions.not(Restrictions.eq("redoAndSupplyHistoryId", redoAndSupplyHistoryId)));
		criteria.addOrder(Order.asc("isMaster"));

		return criteria.list();
	}

	/**
	 * 获得指定批号和任务日期下所有补数据子任务
	 * 
	 * @param operateNo
	 * @param taskDate
	 * @return
	 */
	private Collection<RedoAndSupplyHistory> getSupplyHistoriesByMaster(String operateNo, Date taskDate) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("operateNo", operateNo));
		criteria.add(Restrictions.eq("taskDate", taskDate));
		// criteria.add(Restrictions.eq("isMaster", false));
		criteria.addOrder(Order.asc("isMaster"));

		return criteria.list();
	}

	/**
	 * 获得指定批号下补数据的所有主任务
	 * 
	 * @param operateNo
	 * @return
	 */
	private Collection<RedoAndSupplyHistory> getSupplyMasterHistoriesByOperateNo(String operateNo) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("operateNo", operateNo));
		// criteria.add(Restrictions.eq("isMaster", true));
		criteria.addOrder(Order.asc("isMaster"));

		return criteria.list();
	}

}
