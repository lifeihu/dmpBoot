package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.background.exception.GatewayNotFoundException;
import com.sw.bi.scheduler.model.Action;
import com.sw.bi.scheduler.model.Job;
import com.sw.bi.scheduler.model.Task;
import org.springframework.ui.ConditionModel;
import org.springframework.ui.PaginationSupport;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface ActionService extends GenericService<Action> {

	public boolean excuteProcedure(String procName, Object[] params);

	/**
	 * 根据指定的任务创建相应的Action
	 * 
	 * @param task
	 * @return
	 */
	public Action create(Task task);

	/**
	 * 使用SQL进行分页查询
	 * 
	 * @param cm
	 * @return
	 */
	public PaginationSupport pagingBySql(ConditionModel cm);

	/**
	 * 修改指定作业对应的当天所有任务执行明细的作业冗余字段
	 * 
	 * @param job
	 */
	public void updateByJob(Job job);

	/**
	 * 统计指定网关机当天正在运行的任务数量
	 * 
	 * @param gateway
	 * @return
	 * @throws GatewayNotFoundException
	 */
	public int countTodayRunningTasks(String gateway) throws GatewayNotFoundException;

	/**
	 * 根据并发配置统计已配置的作业类型当天正在运行的任务数量
	 * 
	 * @param gateway
	 * @return
	 */
	public Map<Integer, Collection<Action>> countTodayRunningTasksByConcurrentJobType(String gateway) throws GatewayNotFoundException;

	/**
	 * 统计当天正在导入MySQL的任务数量
	 * 
	 * @param gateway
	 * @return
	 * @throws GatewayNotFoundException
	 */
	@Deprecated
	public Integer countTodayToMySQLTasks(String gateway) throws GatewayNotFoundException;

	/**
	 * 获得指定Action的日志信息
	 * 
	 * @param actionId
	 * @return
	 */
	public String getActionLog(Long actionId);

	/**
	 * 获得指定任务的最近一次Action
	 * 
	 * @param taskId
	 * @param taskDate
	 */
	public Long getLastActionIdByTask(Long taskId, Date taskDate);

	/**
	 * 得到某一天的所有action执行记录,并按照start_time从小到大排列
	 * 
	 * @param taskDate
	 * @return
	 */
	public List<Action> getActionsOrderByStartTimeAsc(Date taskDate);

	/**
	 * 获得指定任务所有正在运行的Action
	 * 
	 * @param taskId
	 * @return
	 */
	public List<Action> getRunningActions(long taskId);

	/**
	 * 得到某个日期内正在运行的action实例
	 * 
	 * @param taskDate
	 * @return
	 */
	public List<Action> getRunningActions(Date taskDate);

	/**
	 * 得到所有正在运行的action实例
	 * 
	 * @return
	 */
	public List<Action> getAllRunningActions();

	/**
	 * 获得指定Action的进程信息
	 * 
	 * @param actionId
	 * @return
	 */
	public String getActionPID(Long actionId);

	/**
	 * 删除指定Action的进程(无论是否有删除进和都会将Task和Action状态置失败)
	 * 
	 * @param actionId
	 */
	public void killActionPID(Long actionId);

	/**
	 * <pre>
	 * 删除所有网关机中指定日期的所有正在运行的进程(用于前台“调度维护”功能)
	 * 使用killGatewayPID(String gateway)接口替代
	 * </pre>
	 * 
	 * @param taskDate
	 */
	@Deprecated
	public void killGatewayPID(Date taskDate);

	/**
	 * 删除指定网关机最近二天所有正在运行的进程(用于前台“调度维护”功能)
	 * 
	 * @param gateway
	 */
	public void killGatewayPID(String gateway);

	/**
	 * 根据指定条件获得相应的Action(该方法只是由异常检测程序用来临时解决产生二条相同Action的BUG，
	 * 最好还是需要查出产生二条相同Action的原因)
	 * 
	 * @param taskId
	 * @param taskDate
	 * @param startTime
	 * @param createTime
	 * @return
	 */
	@Deprecated
	public Action getAction(Long taskId, Date taskDate, Date startTime, Date createTime);

	/**
	 * 统计所有网关机上正在运行的作业数量
	 * 
	 * @param beginTimeInterval
	 * @param excludeJobIds
	 * @param excludeRedoOrSupplyJob
	 * @return
	 */
	public int countTodayRunningActions(int beginTimeInterval, Long[] excludeJobIds, boolean excludeRedoOrSupplyJob, boolean excludeCheckFileNumber);

	/**
	 * 获得指定任务指定扫描日期内正在运行的Action数量
	 * 
	 * @param taskId
	 * @param scanDate
	 * @return
	 */
	public boolean hasRunningActionByTaskAndScanDate(long taskId, Date scanDate);

}
