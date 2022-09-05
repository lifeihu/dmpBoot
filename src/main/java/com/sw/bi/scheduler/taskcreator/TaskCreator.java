package com.sw.bi.scheduler.taskcreator;

import com.sw.bi.scheduler.model.Job;

import java.util.Calendar;

public interface TaskCreator {

	/**
	 * 根据指定的作业生成相应的任务
	 * 
	 * @param job
	 * @param taskDate
	 * @param taskFlag
	 */
	public void create(Job job, Calendar taskDate, Integer taskFlag);
}
