package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.model.Concurrent;

import java.util.Collection;

public interface ConcurrentService extends GenericService<Concurrent> {

	/**
	 * 获得所有作业分类的任务运行时最大并发数
	 * 
	 * @param category
	 * @return
	 */
	public int getMaxConcurrentNumberByCategory(int category);

	/**
	 * 获得所有作业分类的大任务运行时最大并发数
	 * 
	 * @param category
	 * @return
	 */
	public int getBigDataMaxConcurrentNumberByCategory(int category);

	/**
	 * 获得分类名称
	 * 
	 * @param category
	 * @return
	 */
	public String getCategoryName(int category);

	/**
	 * 获得所有分类
	 * 
	 * @return
	 */
	public Collection<Integer> getConcurrentCategories();

	/**
	 * 获得指定作业类型的分类
	 * 
	 * @param jobType
	 * @return
	 */
	public Integer getConcurrentCategory(long jobType);

	/**
	 * 获得并发配置中的所有作业类型
	 * 
	 * @return
	 */
	public Collection<Long> getConcurrentJobTypes();

	/**
	 * 获得指定分类的作业类型
	 * 
	 * @param category
	 * @return
	 */
	public Collection<Long> getConcurrentJobTypes(int category);

}
