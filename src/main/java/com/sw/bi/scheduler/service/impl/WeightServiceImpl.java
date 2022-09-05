package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.Job;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.model.WaitUpdateStatusTask;
import com.sw.bi.scheduler.service.*;
import com.sw.bi.scheduler.util.Configure.TaskFlag;
import com.sw.bi.scheduler.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@SuppressWarnings("unchecked")
public class WeightServiceImpl implements WeightService {

	@Autowired
	private JobService jobService;

	@Autowired
	private TaskService taskService;

	@Autowired
	private JobRelationService jobRelationService;

	@Autowired
	private WaitUpdateStatusTaskService waitUpdateStatusTaskService;

	@Override
	public Map<String, Object> getReferenceOrTriggeredTasks(Long[] jobIds) {
		Date today = DateUtil.getToday();
		Date yesterday = DateUtil.getYesterday();

		// 所有参考点列表
		Map<Long, Boolean> referenceMapping = new LinkedHashMap<Long, Boolean>();
		Map<String, Collection<Job>> childrenJobMapping = new HashMap<String, Collection<Job>>();
		/*Criteria criteria = waitUpdateStatusTaskService.createCriteria();
		criteria.add(Restrictions.eq("active", true));
		criteria.addOrder(Order.desc("flag"));
		criteria.addOrder(Order.asc("taskDate"));
		criteria.addOrder(Order.asc("jobId"));
		criteria.addOrder(Order.asc("waitUpdateStatusTaskId"));
		Collection<WaitUpdateStatusTask> waitUpdateStatusTasks = criteria.list();*/
		Collection<WaitUpdateStatusTask> waitUpdateStatusTasks = waitUpdateStatusTaskService.getActiveWaitUpdateStatusTasks();
		for (WaitUpdateStatusTask wust : waitUpdateStatusTasks) {
			Long referenceJobId = wust.getJobId();

			// 作业ID则从Task表中取得相应信息，不过这个判断只是为了兼容以前的参考点数据，以后该字段肯定不会为空
			// FIXME 以下判断在运行一段时间以后是可以删除的
			if (referenceJobId == null) {
				Task referenceTask = taskService.get(wust.getTaskId());

				if (referenceTask == null) {
					continue;
				}

				referenceJobId = referenceTask.getJobId();
				wust.setJobId(referenceJobId);
				wust.setTaskName(referenceTask.getName());
				wust.setSettingTime(referenceTask.getSettingTime());
				// wust.setTask(referenceTask);
			}

			// Long referenceJobId = referenceTask.getJobId();
			String key = referenceJobId.toString() + DateUtil.formatDate(wust.getTaskDate());
			Collection<Job> childrenJobs = childrenJobMapping.get(key);
			if (childrenJobs == null) {
				childrenJobs = jobRelationService.getOnlineChildrenJobs(referenceJobId);
				childrenJobMapping.put(key, childrenJobs);
			}
			wust.setChildrenJobs(childrenJobs);

			referenceMapping.put(wust.getTaskId(), true);
		}

		// 提示任务列表
		Collection<Task> otherStatusTasks = new ArrayList<Task>();

		Map<Long, Long> initializeTaskJobMapping = new HashMap<Long, Long>();
		Collection<Long> triggeredTaskIds = new HashSet<Long>();

		if (jobIds != null) {
			Collection<Job> jobs = jobService.query(jobIds);
			for (Job job : jobs) {
				Long jobId = job.getJobId();
				Collection<Task> tasks = taskService.getTasksByJob(jobId, yesterday, today);

				for (Task task : tasks) {
					Long taskId = task.getTaskId();

					if (task.isInitialize()) {
						// 判断任务的父任务是否存在于参考点表中
						boolean presentReferencePoints = false; // 是否存在于参考点中
						Collection<Task> parentTasks = taskService.getParentTasks(task);
						for (Task parentTask : parentTasks) {
							Long parentTaskId = parentTask.getTaskId();
							if (referenceMapping.get(parentTaskId) != null) {
								presentReferencePoints = true;
								initializeTaskJobMapping.put(parentTaskId, jobId);
							}
						}

						// 如果任务的所有父任务都不在参考点表则将该任务加入提示任务列表中
						if (!presentReferencePoints) {
							otherStatusTasks.add(task);
						}

					} else if (task.isTriggered()) {
						triggeredTaskIds.add(taskId);

					} else {
						otherStatusTasks.add(task);
					}
				}
			}
		}

		Map<String, Object> result = new HashMap<String, Object>();
		result.put("initializeTaskIds", initializeTaskJobMapping);
		result.put("triggeredTaskIds", triggeredTaskIds);
		result.put("referenceTasks", waitUpdateStatusTasks /*new LinkedHashSet<WaitUpdateStatusTask>(referenceMapping.values())*/);
		result.put("otherStatusTasks", otherStatusTasks);

		return result;
	}

	/**
	 * 应用场所：前台任务列表那里的加权动作 1. 将任务的flag2字段设置为TaskFlag.WEIGHT 2.
	 * 同时将对应的参考点表中的点的flag字段也加权 waitUpdateStatusTaskService.addParentTasks(task,
	 * DateUtil.getToday());
	 */
	@Override
	public void weighting(long taskId) {
		Task task = taskService.get(taskId);
		task.setFlag2(TaskFlag.WEIGHT.indexOf());

		// 如果该任务是“已触发”或“重做已触发”状态则直接将准备时间改为00:05
		if (task.isTriggered()) {
			task.setReadyTime(taskService.calculateReadyTime(task));
		}

		taskService.update(task);

		waitUpdateStatusTaskService.addParentTasks(task, DateUtil.getToday());
	}

	/**
	 * 任务权重管理左下角,参考点任务列表那里的加权动作 1. 参考点权重小于等于4,则加权后变为5;大于4,则加权操作后累加1 2.
	 * 参考点对应的“被查询作业”所对应的任务记录的flag变5
	 */
	@Override
	public void weightingReference(long waitUpdateStatusTaskId, Long jobId) {
		WaitUpdateStatusTask wust = waitUpdateStatusTaskService.get(waitUpdateStatusTaskId);

		// 修改参考点表的Flag(原先Flag小于4则改成5,否则加1)
		int flag = wust.getFlag();
		if (flag <= TaskFlag.WEIGHT.indexOf()) {
			flag = 5;
		} else {
			flag += 1;
		}
		wust.setFlag(flag);
		waitUpdateStatusTaskService.update(wust);

		if (jobId != null) {
			Collection<Task> tasks = taskService.getTasksByJob(jobId, wust.getTaskDate());
			for (Task task : tasks) {
				task.setFlag2(5);
				taskService.update(task);
			}
		}
	}

	/**
	 * 权重管理,上面的作业提示信息那里,加权就是直接把对应任务的flag设置为5
	 */
	@Override
	public void weightingTaskFlag2(long taskId) {
		Task task = taskService.get(taskId);
		task.setFlag2(5);
		taskService.update(task);
	}

	/**
	 * 权重管理--右下角已触发任务那里,加权就是直接把对应任务的ready_time设置为最小值。(日期与scan_date同)
	 */
	@Override
	public void weightingTaskReadyTime(long taskId) {
		Task task = taskService.get(taskId);

		Calendar readyTime = DateUtil.getCalendar(task.getScanDate());
		readyTime.set(Calendar.HOUR_OF_DAY, 0);
		readyTime.set(Calendar.MINUTE, 5);
		readyTime.set(Calendar.SECOND, 0);
		readyTime.set(Calendar.MILLISECOND, 0);
		task.setReadyTime(readyTime.getTime());

		taskService.update(task);
	}

}
