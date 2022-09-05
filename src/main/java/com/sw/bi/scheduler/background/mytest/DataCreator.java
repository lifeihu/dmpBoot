package com.sw.bi.scheduler.background.mytest;

import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import com.sw.bi.scheduler.background.taskexcuter.CopyFile;
import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.model.Action;
import com.sw.bi.scheduler.model.Job;
import com.sw.bi.scheduler.model.JobRelation;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.service.ActionService;
import com.sw.bi.scheduler.service.GenericService;
import com.sw.bi.scheduler.service.JobRelationService;
import com.sw.bi.scheduler.service.JobService;
import com.sw.bi.scheduler.service.TaskService;
import com.sw.bi.scheduler.util.Configure.ActionStatus;
import com.sw.bi.scheduler.util.Configure.JobCycle;
import com.sw.bi.scheduler.util.Configure.TaskFlag;
import com.sw.bi.scheduler.util.Configure.TaskStatus;
import com.sw.bi.scheduler.util.DateUtil;

// 生成大批量测试数据，用来测试调度后台程序的性能如何   从而考虑对数据库表加索引和加分区 
//select program_path,concat('/root/sqls/',SUBSTRING(program_path,9)) from job
//update job set program_path = concat('/root/sqls/',SUBSTRING(program_path,9));
public class DataCreator {
	//job数
	private final int job_count = 50;
	//运行天数，天任务的task数为job数*天数
	private final int day_count = 20;

	//任务的子节点数
	private final int minChildCount = 1;
	private final int maxChildCount = 3;

	//private String programPath = "/home/p_shunwang_data/scheduler-test/";
	private String programPath = "c:/sqls/";

	//考虑内存消耗，每次只生成一定条数记录执行
	private int maxSaveOnce = 10000;
	ArrayDeque queue = new ArrayDeque();

	public void create() {
		Date date = new Date();
		this.createJobs();
		this.buildJobRelations();
		this.createTasks();
		this.createActions();
		System.out.println((new Date().getTime() - date.getTime()) / 1000 + " seconds");
	}

	private void createActions() {
		System.out.println(" actions creating");
		TaskService ts = BeanFactory.getService(TaskService.class);
		for (int i = 0; i <= this.job_count; i++) {
			List<Task> list = ts.getTasksByJob(i + 1, null);
			for (Task task : list) {
				this.queue.add(this.createAction(task));
				//as.save(this.createAction(task));
				if (this.queue.size() >= this.maxSaveOnce) {
					this.saveOnce(ActionService.class);
				}
			}
		}
		if (this.queue.size() > 0) {
			this.saveOnce(ActionService.class);
		}

		System.out.println(job_count * this.day_count + " actions created");
	}

	private void createSqlFile(int job, int root) {
		job++;
		root++;
		String sql = "CREATE TABLE if not exists " + ("scheduler_test" + "job" + job) + "(" + "\n";
		sql += "        a               string," + "\n";
		sql += "        b               string," + "\n";
		sql += "        c               string)" + "\n";
		sql += "PARTITIONED BY (pt STRING)" + "\n";
		sql += "row format delimited" + "\n";
		sql += "fields terminated by '\\\"'" + "\n";
		sql += "lines terminated by '\\n'" + "\n";
		sql += "STORED AS TEXTFILE;" + "\n";
		sql += "insert overwrite table " + ("scheduler_test" + "job" + job) + " partition(pt='${date_desc}')" + "\n";
		sql += "  select a,b,c from scheduler_test" + "job" + root + "  where pt = '${date_desc}';";

		CopyFile.newFile(this.programPath + "job" + job + ".sql", sql);

	}

	private void createJobs() {
		queue.push(this.getBaseJob());

		for (int _job = 1; _job <= this.job_count; _job++) {
			queue.push(this.createJob(_job));
			if (queue.size() == this.maxSaveOnce) {
				this.saveOnce(JobService.class);
			}
		}
		if (queue.size() > 0) {
			this.saveOnce(JobService.class);

		}

		System.out.println(job_count + " jobs created");
	}

	private Job getBaseJob() {
		Job job = new Job();
		job.setJobId(1);
		job.setJobName("rootjob");
		job.setAlert(1l);
		job.setCreateTime(new Date());
		//job.setDayN(1l);
		job.setCycleType(JobCycle.DAY.indexOf());
		job.setDutyOfficer(1l);
		job.setJobBusinessGroup("");
		job.setJobLevel(1l);
		job.setJobDesc("rootjob");
		job.setJobType(100l);
		job.setJobStatus(1l);
		job.setJobTime("00:00");
		job.setProgramPath(this.programPath + "a.sh");
		//////job.setOutput("a.sh");
		return job;
	}

	private void buildJobRelations() {

		this.createBaseRelation();
		this.createBaseFiles();
		int maxChild = 2;
		int[] roots = new int[] { 2 };
		while (this.job_count >= maxChild) {
			Random r = new Random();

			int child = 0;
			for (int i = 0; i < roots.length; i++) {
				int childCount = r.nextInt(this.maxChildCount - this.minChildCount + 1) + this.minChildCount;
				for (int j = 1; j <= childCount; j++) {
					JobRelation jr = this.createJobRelation(roots[i], j + child + maxChild);
					if (jr == null) {
						if (queue.size() > 0) {
							this.saveOnce(JobRelationService.class);
						}

						System.out.println("jobRelations created");
						//this.createBaseFiles();
						return;
					} else {

						queue.push(jr);
						if (queue.size() >= this.maxSaveOnce) {
							this.saveOnce(JobRelationService.class);
						}
					}
				}
				child += childCount;
			}

			roots = new int[child];
			for (int i = 0; i < roots.length; i++) {
				roots[i] = maxChild++;
			}

		}

	}

	private void createBaseFiles() {
		String sql = "CREATE TABLE if not exists " + ("scheduler_testjob2") + "(" + "\n";
		sql += "        a               string," + "\n";
		sql += "        b               string," + "\n";
		sql += "        c               string)" + "\n";
		sql += "PARTITIONED BY (pt STRING)" + "\n";
		sql += "row format delimited" + "\n";
		sql += "fields terminated by '\\\"'" + "\n";
		sql += "lines terminated by '\\n'" + "\n";
		sql += "STORED AS TEXTFILE;" + "\n";
		sql += "insert overwrite table scheduler_testjob2 partition(pt='${date_desc}')" + "\n";
		sql += "select 'a','b','c' from dual;";

		CopyFile.newFile(this.programPath + "job2.sql", sql);

		String sh = "#!/bin/bash" + "\n";

		sh += "	echo \"ok\"" + "\n";
		CopyFile.newFile(this.programPath + "a.sh", sh);

	}

	private void createBaseRelation() {
		queue.add(this.createJobRelation(1, 2));
		queue.add(this.createJobRelation(0, 1));
	}

	private void createTasks() {
		System.out.println(" tasks creating");
		for (int _job = 0; _job <= this.job_count; _job++) {
			for (int _day = 0; _day < this.day_count; _day++) {
				Task task = this.createTask(_job + 1, _day);
				//ts.save(task);
				this.queue.add(task);
				if (queue.size() >= this.maxSaveOnce) {
					this.saveOnce(TaskService.class);
				}
			}
		}
		if (this.queue.size() > 0) {
			this.saveOnce(TaskService.class);
		}

		System.out.println(job_count * this.day_count + " tasks created");
	}

	private <T extends GenericService> void saveOnce(Class<T> kind) {
		GenericService service = BeanFactory.getService(kind);
		service.saveOrUpdateAll(this.queue);

		int size = this.queue.size();
		this.queue.clear();

		if (size >= 10000) {
			System.gc();
		}
		System.out.println(kind.getSimpleName() + ": " + size + " in");
	}

	private JobRelation createJobRelation(int root, int child) {
		if (child > this.job_count + 1) {
			return null;
		}
		JobRelation jr = new JobRelation();
		jr.setCreateTime(new Date());

		jr.setJobId(child);
		if (root != 0) {
			jr.setParentId((long) root);
		}
		if (child > 1) {
			this.createSqlFile(child, root);
		}

		return jr;
	}

	private Job createJob(int num) {
		num++;
		Job job = new Job();
		job.setJobId(num);
		job.setJobName("job" + num);
		job.setAlert(1l);
		job.setCreateTime(new Date());
		//job.setDayN(1l);
		job.setCycleType(JobCycle.DAY.indexOf());
		job.setDutyOfficer(1l);
		job.setJobBusinessGroup("");
		job.setJobLevel(1l);
		job.setJobDesc("job");
		job.setJobType(20l);
		job.setJobStatus(1l);
		job.setJobTime("00:00");
		job.setProgramPath(this.programPath + job.getJobName() + ".sql");
		//////job.setOutput(job.getJobName() + ".sql");
		return job;
	}

	private Job cacheJob;

	private Task createTask(int job_id, int day_before) {
		Calendar calendar = DateUtil.getCalendar(new Date());
		calendar.add(Calendar.DATE, 0 - day_before - 1);
		Date d = calendar.getTime();
		if (cacheJob == null || cacheJob.getJobId() != job_id) {
			cacheJob = BeanFactory.getService(JobService.class).get((long) job_id);
		}
		Task task = new Task();
		task.setCreateTime(new Date());
		task.setAlert(1l);
		task.setDutyOfficer(1l);
		// task.setJob(cacheJob);
		task.setJobId(cacheJob.getJobId());
		task.setFlag(TaskFlag.SYSTEM.indexOf());
		task.setJobBusinessGroup(cacheJob.getJobBusinessGroup());
		task.setJobDesc(cacheJob.getJobDesc());
		task.setJobLevel(cacheJob.getJobLevel());
		task.setJobName(cacheJob.getJobName());
		task.setJobType(cacheJob.getJobType());
		task.setScanDate(d);
		task.setSettingTime(d);
		task.setTaskDate(d);
		task.setTaskStatus(TaskStatus.RUN_SUCCESS.indexOf());
		task.setRunTimes(1);
		task.setCycleType((int) cacheJob.getCycleType());
		task.setTaskBeginTime(calendar.getTime());
		calendar.add(Calendar.MINUTE, 10);
		task.setTaskEndTime(calendar.getTime());

		return task;
	}

	private Action createAction(Task task) {
		Action a = new Action();
		a.setActionLog("");
		a.setActionStatus(ActionStatus.RUN_SUCCESS.indexOf());
		a.setCreateTime(new Date());
		a.setFlag((int) task.getFlag());
		// a.setJobId(task.getJob().getJobId());
		a.setJobId(task.getJobId());
		a.setJobName(task.getJobName());
		a.setStartTime(new Date());
		a.setSettingTime(task.getSettingTime());
		a.setTaskDate(task.getTaskDate());
		a.setTaskId(task.getTaskId());
		a.setOperator("system");
		a.setGateway("scheduler");
		return a;
	}

	public static void main(String args[]) {
		new DataCreator().create();
	}

}
