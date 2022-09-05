package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.background.exception.SchedulerException;
import com.sw.bi.scheduler.model.Job;
import com.sw.bi.scheduler.model.JobDatasyncConfig;
import com.sw.bi.scheduler.model.MailJobConfig;
import com.sw.bi.scheduler.model.ReportsQualityMonitor;
import org.springframework.resolver.Warning;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface JobService extends GenericService<Job> {

	/**
	 * 根据作业自动创建指定日期任务
	 * 
	 * @param taskDate
	 * @throws SchedulerException
	 */
	public void createTasks(Date taskDate) throws SchedulerException;

	/**
	 * 根据作业自动创建指定日期任务
	 * 
	 * @param jobIds
	 *            已经上线作业(调用该接口时必须保存传入的作业必须是已上线作业)
	 * @param taskDate
	 */
	public void createTasks(Long[] jobIds, Date taskDate);

	/**
	 * 根据作业自动创建指定日期任务
	 * 
	 * @param job
	 *            已经上线作业(调用该接口时必须保存传入的作业必须是已上线作业)
	 * @param taskDate
	 * @param taskFlag
	 */
	public void createTasks(Job job, Date taskDate, int taskFlag);

	/**
	 * 根据作业自动创建指定日期任务
	 * 
	 * @param jobs
	 *            已经上线作业(调用该接口时必须保存传入的作业必须是已上线作业)
	 * @param taskDate
	 */
	public void createTasks(Collection<Job> jobs, Date taskDate);

	/**
	 * 根据作业自动创建指定日期任务
	 * 
	 * @param jobs
	 *            已经上线作业(调用该接口时必须保存传入的作业必须是已上线作业)
	 * @param taskDate
	 * @param taskFlag
	 */
	public void createTasks(Collection<Job> jobs, Date taskDate, int taskFlag);

	/**
	 * 获得所有上线任务
	 */
	public List<Job> getOnlineJobs();

	/**
	 * 查询某一类运行周期的上线作业
	 * 
	 * @param cycleType
	 * @return
	 */
	public List<Job> getOnlineJobsByCycleType(long cycleType);

	/**
	 * 获得指定作业类型的上线作业
	 * 
	 * @param jobTypes
	 * @return
	 */
	public Collection<Job> getOnlineJobsByJobType(Long[] jobTypes);

	/**
	 * 在指定的作业ID集合中查出是上线的作业
	 * 
	 * @param jobIds
	 * @return
	 */
	public Collection<Job> getOnlineJobsByJobId(Long[] jobIds);

	/**
	 * 保存作业
	 * 
	 * @param job
	 * @param saveOnline
	 * @param onlineSuccess
	 * @param jobDatasyncConfig
	 * @param mailJobConfig
	 * @param reportsQualityMonitorConfig
	 * @param greenplumJobConfig
	 * @param parentJobIds
	 */
	public void save(Job job,
                     boolean saveOnline,
                     boolean onlineSuccess,
                     JobDatasyncConfig jobDatasyncConfig,
                     MailJobConfig mailJobConfig,
                     ReportsQualityMonitor reportsQualityMonitorConfig,
                     List<Long> parentJobIds);

	/**
	 * 上线指定作业
	 * 
	 * @param jobId
	 *            需要上线的作业ID
	 * @param allowCascadeOnline
	 *            是否允许级联上线
	 */
	public void online(long jobId, boolean allowCascadeOnline);

	/**
	 * 上线指定作业
	 * 
	 * @param jobId
	 *            需要上线的作业ID
	 * @param allowCascadeOnline
	 *            是否允许级联上线
	 * @param updateBy
	 *            操作人
	 */
	public void online(long jobId, boolean allowCascadeOnline, Long updateBy);

	/**
	 * 下线指定作业
	 * 
	 * @param jobId
	 *            需要下线的作业IDs
	 * @param downTime
	 *            下线时间
	 * @param downReason
	 *            下线原因
	 */
	public void offline(Long[] jobId, Long downMan, Date downTime, String downReason);

	/**
	 * 判断指定的作业ID和任务日期是否允许生成任务
	 * 
	 * @param taskDate
	 * @param jobIds
	 * @return
	 */
	public boolean isAlreadyCreateTasks(Date taskDate, Long[] jobIds);

	/**
	 * 校验指定作业类型是否允许设置指定的前置作业
	 * 
	 * @param cycleType
	 * @param prevJobs
	 * @return
	 */
	public boolean validateFrontJobs(int cycleType, Collection<Job> prevJobs) throws Warning;

	/**
	 * 获得上线作业中已配置的所有前置作业
	 * 
	 * @return
	 */
	public Collection<Long> getOnlineFrontJobIds();

	/**
	 * 获得指定作业的前置作业
	 * 
	 * @param jobId
	 * @return
	 */
	public Collection<Job> getFrontJobs(long jobId);

	/**
	 * 获得指定作业的后置作业(即将指定作业配置为前置的作业)
	 * 
	 * @param rearJobId
	 * @return
	 */
	public Collection<Job> getRearJobs(long rearJobId);

	/**
	 * 获得所有引用了指定数据源的作业
	 * 
	 * @param dataSourceId
	 * @return
	 */
	public Collection<Job> getJobsByDataSource(long dataSourceId);
	
	/**
	 * 在作业中删除指定的作业
	 * 
	 * @param entities
	 *
	 *            指定父作业,为空时表示所有父作业
	 */
	public void removeJobsFrom(Collection<Job> entities);

}
