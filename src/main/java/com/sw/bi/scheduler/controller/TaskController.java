package com.sw.bi.scheduler.controller;

import com.sw.bi.scheduler.background.exception.GatewayNotFoundException;
import com.sw.bi.scheduler.model.Job;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.service.GenericService;
import com.sw.bi.scheduler.service.JobRelationService;
import com.sw.bi.scheduler.service.JobService;
import com.sw.bi.scheduler.service.TaskService;
import com.sw.bi.scheduler.util.DateUtil;
import com.sw.bi.scheduler.util.OperateAction;
import org.apache.commons.beanutils.ConvertUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ConditionModel;
import org.springframework.ui.Model;
import org.springframework.ui.PaginationSupport;
import org.springframework.util.JsonUtil;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
@RequestMapping("/manage/task")
public class TaskController extends BaseActionController<Task> {

	@Autowired
	private JobService jobService;

	@Autowired
	private TaskService taskService;

	@Autowired
	private JobRelationService jobRelationService;

	@RequestMapping("/")
	@ResponseBody
	public Model execute(Long id, Model model) {
		Task task = null;
		Job job = null;

		if (id != null) {
			task = getDefaultService().get(id);
			job = jobService.get(task.getJobId());

			// 获得父作业
			StringBuffer parentJobIds = new StringBuffer();
			Collection<Job> parentJobs = jobRelationService.getOnlineParentJobs(job.getJobId());
			for (Job parentJob : parentJobs) {
				if (parentJobIds.length() > 0) {
					parentJobIds.append("\n");
				}
				parentJobIds.append(parentJob.getJobId()).append(" - ").append(parentJob.getJobName());
			}

			// 获得前置任务(由前置作业生成)
			StringBuffer prevTasks = new StringBuffer();
			if (StringUtils.hasText(task.getPreTasks())) {
				Collection<Task> tasks = taskService.query((Long[]) ConvertUtils.convert(task.getPreTasks().split(","), Long.class));
				for (Task t : tasks) {
					if (prevTasks.length() > 0) {
						prevTasks.append("\n");
					}
					prevTasks.append(t.getJobId() + " - " + t.getName());
				}
			}

			// 获得前置任务(由补数据操作生成)
			StringBuffer prevTasksFromOperate = new StringBuffer();
			if (StringUtils.hasText(task.getPreTasksFromOperate())) {
				Collection<Task> tasks = taskService.query((Long[]) ConvertUtils.convert(task.getPreTasksFromOperate().split(","), Long.class));
				for (Task t : tasks) {
					if (prevTasksFromOperate.length() > 0) {
						prevTasksFromOperate.append("\n");
					}
					prevTasksFromOperate.append(t.getJobId() + " - " + t.getName() + " [" + DateUtil.formatDate(t.getTaskDate()) + "]");
				}
			}

			model.addAttribute("task.job", job);
			model.addAttribute("parentJobs", parentJobIds);
			model.addAttribute("preTasks", prevTasks);
			model.addAttribute("preTasksFromOperate", prevTasksFromOperate);
		}

		return model.addAttribute("task", taskService.intervene(task));
	}

	/**
	 * 获得指定任务下指定层级的子任务
	 * 
	 * @param taskId
	 * @param depth
	 * @param allowFetchParent
	 * @param merge
	 *            是否合并小时/分钟任务(默认合并)
	 * @return
	 */
	@RequestMapping("/children")
	@ResponseBody
	public Map<Long, Collection<Task>> getDepthChildren(long taskId, Integer depth, boolean allowFetchParent, Boolean merge) {
		if (merge == null) {
			merge = Boolean.TRUE;
		}

		return taskService.getDepthChildrenTasks(taskService.get(taskId), depth, merge, allowFetchParent);
	}

	/**
	 * 获得指定任务下指定层级的父任务
	 * 
	 * @param taskId
	 * @param depth
	 * @return
	 */
	@RequestMapping("/parents")
	@ResponseBody
	public Map<Long, Collection<Task>> getDepthParents(long taskId, Integer depth) {
		return taskService.getDepthParentTasks(taskService.get(taskId), depth, true);
	}

	/**
	 * 重跑指定作业及其子作业
	 * 
	 * @param taskId
	 * @param childTaskIds
	 * @param breakpoint
	 *            是否允许断点重跑
	 * @param operateBy
	 * @return
	 */
	@RequestMapping("/redo")
	public void redo(long taskId, String childrenTasks, Boolean breakpoint, Long operateBy) {
		Task masterTask = taskService.get(taskId);

		// 重跑前需要先校验主任务的用户组权限
		taskService.isAuthorizedUserGroup(masterTask, OperateAction.REDO);

		Long[] childrenTaskIds = null;
		if (StringUtils.hasText(childrenTasks)) {
			childrenTaskIds = (Long[]) ConvertUtils.convert(childrenTasks.split(","), Long.class);
		}

		taskService.redo(taskId, childrenTaskIds, breakpoint, operateBy);
	}

	/**
	 * 批量重跑
	 * 
	 * <pre>
	 * 	由于实际重跑的逻辑中主作业与子作业在处理逻辑上并没有关联，而且现在
	 * 	主作业与子作业的状态全部统一改成初始化了，因此在批量重跑时可以采取
	 * 	一种取巧的方式进行。即，将所有需要重跑的子作业全部归于某一个主作业
	 * 	下，然后对该主作业重跑及其子作业，其余的只需要重跑主作业就可以了
	 * </pre>
	 * 
	 * @param masterAndChildrenTaskId
	 * @param operateBy
	 */
	@RequestMapping("/batchRedo")
	public void batchRedo(String masterAndChildrenTaskId, Long operateBy) {
		Map<String, Collection<Integer>> masterAndChildrenTask = JsonUtil.decode(masterAndChildrenTaskId, HashMap.class, String.class, ArrayList.class);

		// 批量重跑时需要校验所有主任务的用户组权限
		Collection<String> masterIds = masterAndChildrenTask.keySet();
		for (String masterId : masterIds) {
			Task masterTask = taskService.get(Long.valueOf(masterId));
			taskService.isAuthorizedUserGroup(masterTask, OperateAction.REDO);
		}

		taskService.batchRedo(masterAndChildrenTask, operateBy);
	}

	/**
	 * 补指定作业在指定范围内的数据
	 * 
	 * @param taskId
	 * @param childTaskIds
	 * @param startDate
	 * @param endDate
	 * @param isSerialSupply
	 *            是否串行补数据
	 * @param isCascadeValidateParentTask
	 *            是否级联校验父任务
	 * @param operateBy
	 */
	@RequestMapping("/supply")
	public void supply(long taskId, String childrenTasks, Date startDate, Date endDate, Boolean isSerialSupply, Boolean isCascadeValidateParentTask, Long operateBy) {
		Task masterTask = taskService.get(taskId);

		taskService.isAuthorizedUserGroup(masterTask, OperateAction.SUPPLY);

		Long[] childrenTaskIds = null;
		if (StringUtils.hasText(childrenTasks)) {
			childrenTaskIds = (Long[]) ConvertUtils.convert(childrenTasks.split(","), Long.class);
		}

		taskService.supply(taskId, childrenTaskIds, startDate, endDate, isSerialSupply, isCascadeValidateParentTask, operateBy);
	}

	/**
	 * 批量补数据(主作业与子作业的处理方式与批量重跑一样)
	 * 
	 * @param masterTaskId
	 * @param childrenTaskId
	 * @param startDate
	 * @param endDate
	 * @param isSerialSupply
	 * @param isCascadeValidateParentTask
	 * @param operateBy
	 */
	@RequestMapping("/batchSupply")
	public void batchSupply(String masterAndChildrenTaskId, Date startDate, Date endDate, Boolean isSerialSupply, Boolean isCascadeValidateParentTask, Long operateBy) {
		Map<String, Collection<Integer>> masterAndChildrenTask = JsonUtil.decode(masterAndChildrenTaskId, HashMap.class, String.class, ArrayList.class);

		Collection<String> masterIds = masterAndChildrenTask.keySet();
		for (String masterId : masterIds) {
			taskService.isAuthorizedUserGroup(taskService.get(Long.valueOf(masterId)), OperateAction.SUPPLY);
		}

		taskService.batchSupply(masterAndChildrenTask, startDate, endDate, isSerialSupply, isCascadeValidateParentTask, operateBy);
	}

	/**
	 * 取消补数据
	 * 
	 * @param operateNo
	 *            操作批号
	 * @param taskDate
	 *            需要被取消的任务日期
	 */
	@RequestMapping("/cancelSupply")
	public void cancelSupply(String operateNo, Date taskDate) {
		taskService.cancelSupply(operateNo, taskDate);
	}

	/**
	 * 获得指定任务中的程序代码
	 * 
	 * @param taskId
	 * @return
	 */
	@RequestMapping("/programCode")
	@ResponseBody
	public String programCode(Long taskId) {
		return taskService.getProgramCode(taskId);
	}

	/**
	 * 调整任务的优先级和状态
	 * 
	 * @param taskId
	 * @param jobLevel
	 * @param taskStatus
	 * @param preTasksFromOperate
	 */
	@RequestMapping("/update")
	@ResponseBody
	public void save(Long taskId, Long jobLevel, Long taskStatus, String preTasksFromOperate) {
		Task task = taskService.get(taskId);
		if (task == null) {
			return;
		}

		taskService.isAuthorizedUserGroup(task, OperateAction.TASK_UPDATE);
		taskService.updateLevelOrStatus(taskId, jobLevel, taskStatus, preTasksFromOperate);
	}

	/**
	 * 暂停作业告警
	 * 
	 * @param taskDate
	 * @param jobId
	 * @param taskId
	 */
	@RequestMapping("/pauseAlert")
	@ResponseBody
	public void pauseAlert(Date taskDate, Long jobId, Long taskId) {
		if (taskId == null) {
			taskService.pauseAlert(taskDate, jobId);
		} else {
			taskService.pauseAlert(taskId);
		}
	}

	/**
	 * 恢复作业告警
	 * 
	 * @param taskDate
	 * @param jobId
	 * @param taskId
	 */
	@RequestMapping("/resetAlert")
	@ResponseBody
	public void resetAlert(Date taskDate, Long jobId, Long taskId) {
		if (taskId == null) {
			taskService.resetAlert(taskDate, jobId);
		} else {
			taskService.resetAlert(taskId);
		}
	}

	@Override
	@RequestMapping("/paging")
	public PaginationSupport paging(@RequestParam("condition")
                                            ConditionModel cm, Integer start, Integer limit, @RequestParam(required = false)
	String sort, @RequestParam(value = "dir", required = false)
	String direction) {
		cm.setStart(start);
		cm.setLimit(limit);
		cm.addOrder(sort, direction);

		return taskService.pagingBySql(cm);
	}

	/**
	 * 对分钟任务进行分组
	 * 
	 * @param jobId
	 * @param taskDate
	 * @return
	 */
	@RequestMapping("/groupMinuteTasks")
	@ResponseBody
	public Collection<Task> groupMinuteTasks(long jobId, Date taskDate) {
		Collection<Task> minuteTasks = taskService.getTasksByJob(jobId, taskDate);

		int currentHour = -1;
		Collection<Task> hourTasks = new ArrayList<Task>();
		Collection<Task> tmpTasks = new ArrayList<Task>();

		for (Task minuteTask : minuteTasks) {
			Date settingTime = minuteTask.getSettingTime();
			int hour = settingTime.getHours();

			if (currentHour != hour) {
				if (tmpTasks.size() > 0) {
					hourTasks.addAll(taskService.mergeTasks(tmpTasks));
				}

				tmpTasks.clear();
				currentHour = hour;
			}

			tmpTasks.add(minuteTask);
		}

		if (tmpTasks.size() > 0) {
			hourTasks.addAll(taskService.mergeTasks(tmpTasks));
		}

		return hourTasks;
	}

	/**
	 * 模拟后台调度执行指定的任务
	 * 
	 * @param gateway
	 * @param id
	 * @throws GatewayNotFoundException
	 */
	@RequestMapping("/simulateSchedule")
	@ResponseBody
	public String simulateSchedule(String gateway, String id) throws GatewayNotFoundException {
		if (StringUtils.hasText(id)) {
			return taskService.simulateSchedule((Long[]) ConvertUtils.convert(id.split(","), Long.class), gateway);
		}

		return null;
	}

	/**
	 * 根据扫描日期添加参考点
	 */
	@RequestMapping("/addReferPointsByScanDate")
	public void addReferPointsByScanDate() {
		taskService.repairWaitUpdateStatusTasksByScanDate();
	}

	/**
	 * 根据指定任务添加参考点
	 * 
	 * @param unsuccessTaskId
	 */
	@RequestMapping("/addReferPointsByTaskDate")
	public void addReferPointsByTaskDate(Date startDate, Date endDate) {
		taskService.repairWaitUpdateStatusTasksByTaskDate(startDate, endDate);
	}

	/**
	 * 分析指定任务未运行原因
	 * 
	 * @param taskId
	 * @return
	 */
	@RequestMapping("/analyseUnrunningTasks")
	@ResponseBody
	public Collection<Task> analyseUnrunningTasks(String taskId) {
		if (!StringUtils.hasText(taskId)) {
			return new ArrayList<Task>();
		}

		Long[] taskIds = (Long[]) ConvertUtils.convert(taskId.split(","), Long.class);
		Collection<Task> unrunningTasks = taskService.query(taskIds);

		return taskService.analyseUnrunningTasks(unrunningTasks);
	}

	/**
	 * 分析指定参考点的子任务未运行
	 * 
	 * @param referTaskId
	 * @return
	 */
	@RequestMapping("/analyseUnrunningTasksByReferPoint")
	@ResponseBody
	public Collection<Task> analyseUnrunningTasksByReferPoint(String referTaskId) {
		if (!StringUtils.hasText(referTaskId)) {
			return new ArrayList<Task>();
		}

		Long[] referTaskIds = (Long[]) ConvertUtils.convert(referTaskId.split(","), Long.class);
		return taskService.analyseUnrunningTasksByReferPoint(referTaskIds);
	}

	/**
	 * 分析未运行的GP大任务
	 * 
	 * @return
	 */
	@RequestMapping("/analyseUnrunningBigGreenplumTasks")
	@ResponseBody
	public Collection<Task> analyseUnrunningBigGreenplumTasks() {
		return taskService.analyseUnrunningBigGreenplumTasks();
	}

	@Override
	public GenericService<Task> getDefaultService() {
		return taskService;
	}

}
