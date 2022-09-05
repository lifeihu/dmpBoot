package com.sw.bi.scheduler.taskcreator;

import com.sw.bi.scheduler.model.Job;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.service.JobRelationService;
import com.sw.bi.scheduler.service.TaskService;
import com.sw.bi.scheduler.util.Configure;
import com.sw.bi.scheduler.util.Configure.JobCycle;
import com.sw.bi.scheduler.util.Configure.TaskFlag;
import com.sw.bi.scheduler.util.DateUtil;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.internal.SessionImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

@Component
@SuppressWarnings("unchecked")
public class TaskCreatorRunner {
	protected static Logger log = Logger.getLogger(TaskCreatorRunner.class);

	@Autowired
	private MonthTaskCreator monthTaskCreator;

	@Autowired
	private WeekTaskCreator weekTaskCreator;

	@Autowired
	private DayTaskCreator dayTaskCreator;

	@Autowired
	private HourTaskCreator hourTaskCreator;

	@Autowired
	private MinuteTaskCreator minuteTaskCreator;

	@Autowired
	private NonePeriodTaskCreator nonePeriodTaskCreator;

	@Autowired
	private JobRelationService jobRelationService;

	@Autowired
	private TaskService taskService;

	private Connection connection;
	private static PreparedStatement pstmt;

	public void create(Collection<Job> jobs, Date taskDate) {
		this.create(jobs, taskDate, TaskFlag.SYSTEM.indexOf());
	}

	/**
	 * 
	 * @param jobs
	 *            作业的集合
	 * @param taskDate
	 *            任务日期
	 * @param taskFlag
	 *            用来设置task表的flag2字段 4: 表示加权重的任务 3： 表示新上线的任务 2： 表示重跑的任务 1：
	 *            系统自动生成的任务 0： 表示补数据的任务 用来生成指定作业在指定日期内的任务信息(供外部调用)
	 */
	public void create(Collection<Job> jobs, Date taskDate, Integer taskFlag) {
		this.initPreparedStatement(); // 初始化一个PreparedStatement对象,保存在pstmt静态变量中

		try {
			// 已经生成任务的作业数量
			int createCompleteJobs = 0;

			int jobCommitCount = Configure.property(Configure.JOB_COMMIT_COUNT, Integer.class);

			// 前置作业中设置了依赖自己的作业
			Collection<Job> dependenceSelfJobs = new ArrayList<Job>();

			for (Job job : jobs) {
				create(job, taskDate, taskFlag);

				// 过滤出前置任务中设置了自己的作业
				if (StringUtils.hasText(job.getPrevJobs()) && job.isPrevJobDependenceSelf()) {
					dependenceSelfJobs.add(job);
				}

				createCompleteJobs += 1;

				if (createCompleteJobs == jobs.size() || createCompleteJobs % jobCommitCount == 0) {
					try {
						int[] result = pstmt.executeBatch(); //执行
						connection.commit();

						log.info(createCompleteJobs + " jobs commit(create tasks: " + result.length + ").");
					} catch (SQLException e) {
						e.printStackTrace();
					}

				}
			}

			// 生成依赖自己的前置任务
			if (dependenceSelfJobs.size() > 0) {
				for (Job job : dependenceSelfJobs) {
					Collection<Task> tasks = taskService.getTasksByJob(job.getJobId(), taskDate);
					for (Task task : tasks) {
						Collection<Task> prevTasks = taskService.getFrontTasks(task, job.getPrevJobIds());
						task.setPreTasksByCollection(prevTasks);

						taskService.update(task);
					}
				}
			}

		} finally {
			try {
				if (connection != null) {
					connection.close();
				}

				if (pstmt != null) {
					pstmt.close();
				}
			} catch (Exception e) {}
		}
	}

	private void create(Job job, Date taskDate, Integer taskFlag) {
		Calendar calendar = DateUtil.getCalendar(taskDate);

		switch (JobCycle.valueOf((int) job.getCycleType())) {
			case MONTH:
				monthTaskCreator.create(job, calendar, taskFlag);
				break;
			case WEEK:
				weekTaskCreator.create(job, calendar, taskFlag);
				break;
			case DAY:
				dayTaskCreator.create(job, calendar, taskFlag);
				break;
			case HOUR:
				hourTaskCreator.create(job, calendar, taskFlag);
				break;
			case MINUTE:
				minuteTaskCreator.create(job, calendar, taskFlag);
				break;
			case NONE:
				/**
				 * <pre>
				 * 后台自动生成的任务的Flag都是System,但是后台自动生成时不希望把待触发作业给生成
				 * 待触发作业只能被分支作业创建并执行,所以在分支作业中创建时Flag会被传入ONLINE
				 * </pre>
				 */
				if (taskFlag == TaskFlag.ONLINE.indexOf()) {
					nonePeriodTaskCreator.create(job, calendar, taskFlag);
				}
				break;
		}
	}

	@SuppressWarnings("deprecation")
	// 这里要特别注意,当task表增加了字段的时候,这个地方也要做相应的调整
	public void initPreparedStatement() {
		connection = ((SessionImpl)taskService.getCurrentSession()).connection();

		StringBuffer sql = new StringBuffer("insert into task");
		sql.append("(job_id, job_name, job_desc, job_business_group, duty_officer, job_level, setting_time, task_status, ");
		sql.append("task_date, scan_date, task_begin_time, task_end_time, flag, flag2, ready_time, alert, cycle_type, run_times, create_time, job_type, pre_tasks, gateway, failure_rerun_times, failure_rerun_interval, refer_run_time) values ");
		sql.append("(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

		try {
			pstmt = connection.prepareStatement(sql.toString());
		} catch (DataAccessResourceFailureException e) {
			e.printStackTrace();
		} catch (HibernateException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static PreparedStatement getPreparedStatement() {
		return pstmt;
	}
}
