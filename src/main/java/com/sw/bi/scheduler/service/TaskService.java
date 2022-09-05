package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.background.exception.GatewayNotFoundException;
import com.sw.bi.scheduler.background.exception.SchedulerException;
import com.sw.bi.scheduler.model.Action;
import com.sw.bi.scheduler.model.Job;
import com.sw.bi.scheduler.model.Task;
import org.springframework.ui.ConditionModel;
import org.springframework.ui.PaginationSupport;

import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface TaskService extends GenericService<Task> {

	/**
	 * 获得指定任务时间内的所有初始化任务
	 * 
	 * @param taskDate
	 * @return
	 */
	public List<Task> getInitializeTasksByTaskDate(Date taskDate);

	/**
	 * 获得指定日期范围内所有与指定任务相同的任务
	 * 
	 * @param taskId
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public List<Task> getTasks(long master_taskId, long child_taskId, Date startDate, Date endDate, boolean useMaster);

	/**
	 * 得到指定作业在指定日期内的所有任务记录(小时和分钟任务存在多条任务记录的现象)
	 * 
	 * @param jobId
	 * @param taskDate
	 * @return
	 */
	public List<Task> getTasksByJob(long jobId, Date taskDate);

	/**
	 * 得到指定作业在指定日期内的所有任务记录(小时和分钟任务存在多条任务记录的现象)
	 * 
	 * @param jobIds
	 * @param taskDate
	 * @return
	 */
	public Collection<Task> getTasksByJobs(Collection<Long> jobIds, Date taskDate);

	/**
	 * 得到指定作业在指定日期内的所有任务记录(小时和分钟任务存在多条任务记录的现象)
	 * 
	 * @param jobIds
	 * @param startTaskDate
	 * @param endTaskDate
	 * @return
	 */
	public Collection<Task> getTasksByJobs(Collection<Long> jobIds, Date startTaskDate, Date endTaskDate);

	/**
	 * 获得指定作业在指定任务日期里的所有任务
	 * 
	 * @param jobId
	 * @param startTaskDate
	 * @param endTaskDate
	 * @return
	 */
	public Collection<Task> getTasksByJob(long jobId, Date startTaskDate, Date endTaskDate);

	/**
	 * 获得指定任务日期及父作业下的所有任务
	 * 
	 * @param startTaskDate
	 * @param endTaskDate
	 * @param parentJobIds
	 * @return
	 */
	public List<Task> getTasksByParentJob(Date taskDate, Collection<Long> parentJobIds);

	/**
	 * 获得指定任务日期及父作业下的所有任务
	 * 
	 * @param startTaskDate
	 * @param endTaskDate
	 * @param parentJobIds
	 * @return
	 */
	public List<Task> getTasksByParentJob(Date startTaskDate, Date endTaskDate, Collection<Long> parentJobIds);

	/**
	 * 获得指定状态、任务日期下的所有任务
	 * 
	 * @param status
	 * @param startTaskDate
	 * @param endTaskDate
	 * @return
	 */
	public Collection<Task> getTasksByStatus(Long[] status, Date startTaskDate, Date endTaskDate);

	/**
	 * 获得指定任务日期内所有任务
	 * 
	 * @param taskDate
	 * @return
	 */
	public Collection<Task> getTasksByTaskDate(Date taskDate);

	/**
	 * 获得指定任务日期内所有任务
	 * 
	 * @param startTaskDate
	 * @param endTaskDate
	 * @return
	 */
	public Collection<Task> getTasksByTaskDate(Date startTaskDate, Date endTaskDate);

	/**
	 * 获得指定预设时间内所有任务
	 * 
	 * @param jobIds
	 * @param startSettingTime
	 * @param endSettingTime
	 * @return
	 */
	public Collection<Task> getTasksBySettingTime(Collection<Long> jobIds, Date startSettingTime, Date endSettingTime);

	/**
	 * 获得所有根节点上的任务
	 * 
	 * @param taskDate
	 * @return
	 * @throws SchedulerException
	 */
	public Task getRootTask(Date taskDate) throws SchedulerException;

	/**
	 * 获得指定任务的所有上层父任务
	 * 
	 * @param task
	 * @return
	 */
	public Collection<Task> getParentTasks(Task task);

	/**
	 * 获得指定任务的所有下层子任务
	 * 
	 * @param task
	 * @return
	 */
	public Collection<Task> getChildrenTasks(Task task);

	/**
	 * 获得指定任务的所有下层子任务
	 * 
	 * <pre>
	 * 	这个方法取子任务的逻辑
	 * 	1.通过作业关联表得到指定任务的所有子作业
	 * 	2.所有子作业中与配置信息中指定的作业相符时只需要查这些未完成的作业并limit 20
	 * 	3.与配置信息中不相符的还是于以前的逻辑一致
	 * </pre>
	 * 
	 * @param task
	 * @return
	 */
	@Deprecated
	public Collection<Task> getChildrenTasksByUnsuccessLimit(Task task);

	/**
	 * 判断指定任务的上层所有父任务是否运行成功
	 * 
	 * @param task
	 * @return
	 */
	public boolean isParentTaskRunSuccess(Task task);

	/**
	 * 判断指定任务是否存在子任务
	 * 
	 * @param task
	 * @return
	 */
	@Deprecated
	public boolean hasChildrenTasks(Task task);

	/**
	 * 计算任务的运行时间
	 * 
	 * @param task
	 * @param initializeDate
	 * @return
	 */
	public Date calculateSettingTime(Task task, Date initializeDate);

	/**
	 * 计算任务指定时间范围内的运行时间
	 * 
	 * @param task
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public Collection<Date> calculateSettingTime(Task masterTask, Task childTask, Date startDate, Date endDate, boolean useMaster);

	/**
	 * 计算任务的准备时间(需要考虑到优先级)
	 * 
	 * <pre>
	 * 计算任务的已触发事件(ready_time)
	 *  task表的flag字段            2： 表示重跑的任务
	 *                      1： 系统自动生成的任务
	 *                      0： 表示补数据的任务
	 *  task表的flag2字段         4:  表示加权重的任务
	 * 					3： 表示新上线的任务
	 * 					2： 表示重跑的任务
	 * 					1： 系统自动生成的任务
	 * 					0： 表示补数据的任务
	 * 					flag2字段数值如果是5及以上,只是表示一个较大的权重
	 *  TaskFlag.SYSTEM(1： 系统自动生成的任务)表示一种普通的权重,凡是flag = Math.max(flag, flag2)大于普通权重时,表示希望尽快执行该任务,这个时候ready_time就给予最小值(0:05)
	 *  flag = Math.max(flag, flag2)小于等于普通权重的时候,就以当前系统时间为基准设置ready_time,同时参考任务的优先级
	 * </pre>
	 * 
	 * @param task
	 * @return
	 */
	public Date calculateReadyTime(Task task);

	/**
	 * 重跑操作可能会对任务状态的变更产生影响，所以需要进行一个反查操作
	 * 
	 * @param task
	 */
	public boolean recheck(Task task);

	/**
	 * 设置所有未完成日期的任务运行完成
	 * 
	 * @param today
	 */
	public boolean runCompleteExcludeToday(Date today);

	/**
	 * 设置指定日期内的所有任务都已经运行完成
	 * 
	 * @param today
	 */
	public boolean runComplete(Date today);

	/**
	 * 重跑指定任务(一般只针对月、周、天、小时任务进行该操作)
	 * 
	 * @param taskId
	 * @param breakpoint
	 * @param operateBy
	 */
	public boolean redo(long taskId, boolean breakpoint, Long operateBy);

	/**
	 * 重跑指定任务及指定子任务
	 * 
	 * @param masterTaskId
	 * @param childTaskIds
	 * @param breakpoint
	 * @param operateBy
	 */
	public boolean redo(long masterTaskId, Long[] childTaskIds, boolean breakpoint, Long operateBy);

	/**
	 * 批量对指定任务及其子任务进行重跑操作
	 * 
	 * @param masterAndChildrenTask
	 * @param operateBy
	 */
	public void batchRedo(Map<String, Collection<Integer>> masterAndChildrenTask, Long operateBy);

	/**
	 * 对指定任务进行补数据(一般只针对天、小时任务进行该操作)
	 * 
	 * @param taskId
	 * @param startDate
	 * @param endDate
	 * @param isSerialSupply
	 * @param operateBy
	 * @return
	 */
	public boolean supply(long taskId, Date startDate, Date endDate, boolean isSerialSupply, boolean isCascadeValidateParentTask, Long operateBy);

	/**
	 * 对指定任务及其指定定子任务进行补数据操作
	 * 
	 * @param masterTaskId
	 * @param childTaskIds
	 * @param startDate
	 * @param endDate
	 * @param isSerialSupply
	 * @param isCascadeValidateParentTask
	 * @param operateBy
	 * @return
	 */
	public boolean supply(long masterTaskId, Long[] childTaskIds, Date startDate, Date endDate, boolean isSerialSupply, boolean isCascadeValidateParentTask, Long operateBy);

	/**
	 * 批量对指定任务及其子任务进行补数据操作
	 * 
	 * @param masterAndChildrenTask
	 * @param startDate
	 * @param endDate
	 * @param isSerialSupply
	 * @param isCascadeValidateParentTask
	 * @param operateBy
	 */
	public void batchSupply(Map<String, Collection<Integer>> masterAndChildrenTask, Date startDate, Date endDate, boolean isSerialSupply, boolean isCascadeValidateParentTask, Long operateBy);

	/**
	 * 取消补数据操作
	 * 
	 * @param operateNo
	 *            补数据操作批号
	 * @param taskId
	 *            需要被取消补数据的任务日期
	 */
	public void cancelSupply(String operateNo, Date taskDate);

	/**
	 * 使用SQL查询并分页
	 * 
	 * @param cm
	 * @param start
	 * @param limit
	 * @return
	 */
	public PaginationSupport pagingBySql(ConditionModel cm);

	/**
	 * 获得指定日期内,非成功状态的任务
	 * 
	 * @param date
	 * @return
	 */
	public List<Task> getUnSuccessTasks(Date date);

	/**
	 * 准备开始运行指定任务
	 * 
	 * @param task
	 * @return
	 */
	public boolean runBegin(Task task);

	/**
	 * 指定任务已运行结果
	 * 
	 * @param task
	 * @param action
	 * @return
	 */
	public boolean runFinished(Task task, Action action);

	/**
	 * 对指定作业的任务下线
	 * 
	 * @param jobIds
	 * @param taskDate
	 */
	public void offline(Long[] jobIds, Date taskDate);

	/**
	 * 对指定任务日期的所有任务下线
	 * 
	 * @param taskDate
	 */
	public void offline(Date taskDate);

	/**
	 * 修改指定作业对应的当天任务的作业冗余字段
	 * 
	 * @param job
	 */
	public void updateByJob(Job job);

	/**
	 * 获得指定作业的指定层级的子任务
	 * 
	 * @param parent
	 * @param depth
	 * @param merge
	 * @param allowFetchParent
	 * @return
	 */
	public Map<Long, Collection<Task>> getDepthChildrenTasks(Task parent, Integer depth, boolean merge, boolean allowFetchParent);

	/**
	 * 获得指定作业的指定层级的父任务
	 * 
	 * @param child
	 * @param depth
	 * @param merge
	 *            是否需要合并小时/分钟任务
	 * @return
	 */
	public Map<Long, Collection<Task>> getDepthParentTasks(Task child, Integer depth, boolean merge);

	/**
	 * 获得指定任务中程序代码(只允许获得HiveSQL和Shell类型的程序代码)
	 * 
	 * @param taskId
	 * @return
	 */
	public String getProgramCode(Long taskId);

	/**
	 * 调整任务优先级或状态
	 * 
	 * @param taskId
	 * @param jobLevel
	 * @param taskStatus
	 * @param preTasksFromOperate
	 */
	public void updateLevelOrStatus(Long taskId, Long jobLevel, Long taskStatus, String preTasksFromOperate);

	public List<Task> getSuccessTasksOrderByRunTimeDesc(long jobId, Date taskDate);

	/**
	 * 查询指定ID,指定日期内失败的任务
	 * 
	 * @param jobId
	 * @param taskDate
	 */
	public List<Task> getFailedTasks(long jobId, Date taskDate);

	/**
	 * 查询指定scanDate下所有未运行的任务(该方法暂时没用到)
	 * 
	 * @param scanDate
	 * @return
	 */
	@Deprecated
	public List<Task> getWaitRunningTasks(Date scanDate);

	/**
	 * 指定的所有任务是否都已经执行成功
	 * 
	 * @param taskIds
	 */
	public boolean isRunSuccess(Long[] taskIds);

	/**
	 * 暂停作业告警
	 * 
	 * @param taskDate
	 * @param jobId
	 */
	public void pauseAlert(Date taskDate, long jobId);

	/**
	 * 暂停作业告警
	 * 
	 * @param taskId
	 */
	public void pauseAlert(long taskId);

	/**
	 * 恢复作业告警
	 * 
	 * @param taskDate
	 * @param jobId
	 */
	public void resetAlert(Date taskDate, long jobId);

	/**
	 * 恢复作业告警
	 * 
	 * @param taskId
	 */
	public void resetAlert(long taskId);

	/**
	 * 如果指定集合中小时/分钟任务,则将其合并为一个小时/分钟任务
	 * 
	 * @param tasks
	 * @return
	 */
	public Collection<Task> mergeTasks(Collection<Task> tasks);

	/**
	 * 模拟后台调度执行指定的任务
	 * 
	 * @param taskIds
	 * @param gateway
	 * @throws GatewayNotFoundException
	 */
	public String simulateSchedule(Long[] taskIds, String gateway) throws GatewayNotFoundException;

	/**
	 * <pre>
	 * 	校验用于模拟调度方式选取到的任务
	 * 	该接口的具体逻辑与上面simulateSchedule基本一致,最初
	 * 	这些逻辑是写在SchedulerService.simulateSchedule接口
	 * 	中的，但是由于逻辑中判断父任务是通过异常方式去实现的
	 * 	所以最终会导致出现“UnexpectedRollbackException: Transaction rolled back because it has been marked as rollback-only”
	 * 	的异常,该异常导致的原因大概是因为service事务进行了嵌套
	 * 	操作,在子service中抛了异常父service捕获了最终事务提交
	 * 	就有问题,但在同一个类中是可以的,所以才有了该接口的存在
	 * </pre>
	 * 
	 * @param tasks
	 * @see http://antlove.iteye.com/blog/1748739
	 * @return
	 */
	public Collection<Task> validateSimulateScheduleTasks(Collection<Task> tasks);

	/**
	 * 获得指定任务的作业前置任务
	 * 
	 * @param task
	 * @return
	 */
	public Collection<Task> getFrontTasks(Task task);

	/**
	 * 获得指定任务的操作前置任务
	 * 
	 * @param task
	 * @return
	 */
	public Collection<Task> getFrontTasksFromOperate(Task task);

	/**
	 * 根据指定的前置作业生成指定任务的前置任务
	 * 
	 * @param task
	 * @param prevJobIds
	 */
	public Collection<Task> getFrontTasks(Task task, Long[] prevJobIds);

	//////////////////////// 用于监控 ///////////////////////

	/**
	 * 获得指定间隔时长内任务被运行却Action始终未创建的任务
	 * 
	 * @param interval
	 * @return
	 */
	public Collection<Task> getNotFoundActionTasks(int interval);

	/**
	 * 统计指定作业等级中未运行成功的任务数量
	 * 
	 * @param jobLevel
	 * @param interval
	 * @return
	 */
	public int countNotRunSuccessTasksByJobLevel(long jobLevel, Integer interval);

	/**
	 * 统计昨天未运行成功的任务数量
	 * 
	 * @return
	 */
	public int countNotRunSuccessTasksByYesterday();

	/**
	 * 获得正在运行超过指定间隔的任务
	 * 
	 * @param taskDate
	 * @param interval
	 * @param excludeJobIds
	 * @param excludeRedoOrSupplyJob
	 * @return
	 */
	public Collection<Task> getRunningTasks(Date taskDate, int interval, Long[] excludeJobIds, boolean excludeRedoOrSupplyJob, boolean excludeCheckFileNumber);

	/**
	 * 统计所有正在运行的任务数量
	 *
	 * @param beginTimeInterval
	 * @param excludeJobIds
	 * @param excludeRedoOrSupplyJob
	 * @return
	 */
	public int countTodayRunningTasks(int beginTimeInterval, Long[] excludeJobIds, boolean excludeRedoOrSupplyJob, boolean excludeCheckFileNumber);

	/**
	 * 查询mysql和GP大任务
	 * 
	 * @param mysql_time
	 * @param gp_time
	 * @param mysql_type
	 * @param gp_type
	 * @return
	 * @throws ParseException
	 */
	public List<Long> getBigTasks(String mysql_time, String gp_time, String mysql_type, String gp_type, String task_date) throws ParseException;

	/**
	 * 按当天扫描日期修复参考点
	 */
	public void repairWaitUpdateStatusTasksByScanDate();

	/**
	 * 按指定任务日期范围修复参考点
	 * 
	 * @param startTaskDate
	 * @param endTaskDate
	 */
	public void repairWaitUpdateStatusTasksByTaskDate(Date startTaskDate, Date endTaskDate);

	/**
	 * 分析指定任务未运行的原因
	 * 
	 * @param unrunningTasks
	 */
	public Collection<Task> analyseUnrunningTasks(Collection<Task> unrunningTasks);

	/**
	 * 分析指定参考点的子任务未运行
	 * 
	 * @param referTaskIds
	 * @return
	 */
	public Collection<Task> analyseUnrunningTasksByReferPoint(Long[] referTaskIds);

	/**
	 * 分析未运行的GP大任务
	 * 
	 * @return
	 */
	public Collection<Task> analyseUnrunningBigGreenplumTasks();

	/**
	 * 根据指定作业修改所有历史任务的预设时间
	 * 
	 * @param job
	 */
	public void updateSettingTime(Job job);

	/**
	 * 将指定日期内所有任务的扫描日期改成当天
	 * 
	 * @param yesterday
	 */
	public void updateYesterdayScanDate(Date yesterday);
	/**
	 * @author zhoushasha 2016/5/5
	 * 根据用户组活动task
	 */
	public Collection<Task> getTasksByUserGroup(long userGroupId);
}
