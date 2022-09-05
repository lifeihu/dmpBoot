package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.model.Job;
import com.sw.bi.scheduler.model.JobRelation;

import java.util.Collection;
import java.util.List;

public interface JobRelationService extends GenericService<JobRelation> {

	/**
	 * 创建作业关系
	 * 
	 * @param jobId
	 * @param parentIds
	 */
	public void createRelation(long jobId, Long[] parentIds);

	/**
	 * 获得根节点作业
	 * 
	 * @return
	 */
	public Job getRootJob();

	/**
	 * 获得指定作业的在线父作业的集合
	 * 
	 * @param jobId
	 * @return
	 */
	public List<Job> getOnlineParentJobs(long jobId);

	/**
	 * 获得指定任务的所有状态的上层作业
	 * 
	 * @param jobId
	 * @return
	 */
	public List<Job> getAllParentJobs(long jobId);

	/**
	 * 获得指定作业的已上线的指定层级的上层作业
	 * 
	 * @param jobId
	 * @param depth
	 *            指定取几层下层作业，如果为空则取所有上层作业
	 * @return 列出所有父作业的集合
	 */
	public Collection<Job> getOnlineDepthParentJobsToCollection(long jobId, Integer depth);

	/**
	 * 查询一个job的未完成上层任务数（本日）
	 * 
	 * @param jobId
	 * @return
	 */
	public int getUnsuceesParentSize(long jobId);

	/**
	 * 获得指定作业的已上线的下层作业
	 * 
	 * @param parentJobId
	 * @return
	 */
	public List<Job> getOnlineChildrenJobs(long parentJobId);

	/**
	 * 获得指定作业的已上线的指定层级的下层作业
	 * 
	 * @param parentJobId
	 * @param depth
	 *            指定取几层下层作业，如果为空则取所有下层作业
	 * @return 列出所有子作业的集体
	 */
	public Collection<Job> getOnlineDepthChildrenJobsToCollection(long parentJobId, Integer depth);

	/**
	 * 在作业关系中删除指定作业的指定或所有父作业
	 * 
	 * @param jobId
	 * @param parentIds
	 *            指定父作业,为空时表示所有父作业
	 */
	public void removeParentJobsFromJobRelations(long jobId, Collection<Long> parentIds);
	
	/**
	 * 根据作业ID查询出JobRelation对象
	 * 注意:这个方法是为了批量给GP生成一批作业写的。这里约定传入的jobid在job_relation表中只有一条对应的记录，也就是只有1个父任务
	 * @param jobId
	 * @return
	 */
	public JobRelation getJobRelationByJob(long jobId);

}
