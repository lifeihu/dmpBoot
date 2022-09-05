package com.sw.bi.scheduler.background.javatype.manualexport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.model.WaitUpdateStatusTask;
import com.sw.bi.scheduler.service.TaskService;
import com.sw.bi.scheduler.service.WaitUpdateStatusTaskService;
import com.sw.bi.scheduler.util.Configure;
import com.sw.bi.scheduler.util.Configure.TaskFlag;
import com.sw.bi.scheduler.util.Configure.TaskStatus;
import com.sw.bi.scheduler.util.DateUtil;

/**
 * 手工导入作业的自动重跑
 * 
 * <pre>
 * 	干熔那边的需求，有些作业的数据来源是从SDS上导入的，干熔会将这些作业的作业ID和任务日期记录在GP的一张表中
 * 	处理逻辑：
 * 		1. 从GP表中得到需要重跑的作业
 * 		2. 得到需要重跑作业及其所有子作业集合，并删除在这个集合中的所有参考点
 * 		3. 重跑该作业及其子作业
 * 		4. 在GP表中删除已经重跑的作业
 * </pre>
 * 
 * @author shiming.hong
 */
@Component
public class ManualExportJobAutoRedo {
	private static final Logger log = Logger.getLogger(ManualExportJobAutoRedo.class);

	@Autowired
	private TaskService taskService;

	@Autowired
	private WaitUpdateStatusTaskService waitUpdateStatusTaskService;

	private Connection connection;

	private Writer writer;

	private static ManualExportJobAutoRedo getManualExportJobAutoRedo() {
		return BeanFactory.getBean(ManualExportJobAutoRedo.class);
	}

	private void execute(long taskId, long interval) throws Exception {
		File parent = new File(Configure.property(Configure.MANUAL_EXPORT_JOB_AUTO_REDO_LOGPATH));
		if (!parent.exists()) {
			parent.mkdirs();
		}

		File logFile = new File(parent, DateUtil.formatDate(DateUtil.getToday()) + ".log");
		if (!logFile.exists()) {
			logFile.createNewFile();
		}

		writer = new OutputStreamWriter(new FileOutputStream(logFile, true), "utf-8");

		/**
		 * 该作业比较特殊,为了防止某些原因导致该作业同一时刻会起来多少时只要求执行最近一个的任务
		 * 所以在该作业执行前需要有一个预处理，当前任务的预设时间小于当前时间-指定间隔后的时间点时直接置成成功
		 */
		Task selfTask = taskService.get(taskId);
		long settingTime = selfTask.getSettingTime().getTime();
		if (settingTime <= System.currentTimeMillis() - interval) {
			// 当前任务预设时间小于等于指定间隔则直接返回
			log.info("[忽略] " + selfTask.getName() + "(" + DateUtil.formatDateTime(selfTask.getSettingTime()) + ").");
			this.log("[忽略] " + selfTask.getName() + "(" + DateUtil.formatDateTime(selfTask.getSettingTime()) + ").");
			return;
		}

		try {
			this.log("[启动] " + selfTask.getName() + ".");

			Collection<Map<String, Object>> needRedoJobs = this.getNeedRedoJobs();

			if (needRedoJobs.size() == 0) {
				log.info("没有需要自动重跑的作业.");
				this.log("[终止] 没有需要自动重跑的作业.");
				return;
			}

			log.info("获得 " + needRedoJobs.size() + " 个需要自动重跑的作业.");
			this.log("获得 " + needRedoJobs.size() + " 个需要自动重跑的作业.");

			for (Map<String, Object> needRedoJob : needRedoJobs) {
				this.redo(needRedoJob);
			}

			this.log("[完成] " + selfTask.getName() + ".");

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

	/**
	 * 重跑指定作业及其子作业
	 * 
	 * @param needRedoJob
	 */
	private void redo(Map<String, Object> needRedoJob) throws Exception {
		long id = (Long) needRedoJob.get("id");
		long jobId = (Long) needRedoJob.get("jobId");
		Date taskDate = (Date) needRedoJob.get("taskDate");

		Collection<Task> needRedoTasks = taskService.getTasksByJob(jobId, taskDate);
		for (Task needRedoTask : needRedoTasks) {
			Map<Long, Collection<Task>> needRedoChildTaskMapping = taskService.getDepthChildrenTasks(needRedoTask, null, true, false);

			// 需要重跑作业及其子作业会涉及到的参考点ID清单
			Collection<Long> referTaskIds = new HashSet<Long>();

			// 将需要重跑作业的父作业加入参考点清单
			Collection<Task> parentTasks = taskService.getParentTasks(needRedoTask);
			for (Task parentTask : parentTasks) {
				referTaskIds.add(parentTask.getTaskId());
			}

			// 将需要重跑作业本身加入参考点清单
			referTaskIds.add(needRedoTask.getTaskId());

			// 需要重跑作业的所有子作业ID
			Collection<Long> needRedoChildTaskIds = new HashSet<Long>();
			for (Collection<Task> needRedoChildTasks : needRedoChildTaskMapping.values()) {
				for (Task needRedoChildTask : needRedoChildTasks) {
					needRedoChildTaskIds.add(needRedoChildTask.getTaskId());
					referTaskIds.add(needRedoChildTask.getTaskId());

					needRedoChildTask.setFlag(TaskFlag.SYSTEM.indexOf());
					if (!needRedoChildTask.isNotRunning()) {
						needRedoChildTask.setTaskStatus(TaskStatus.RE_RUN_FAILURE.indexOf());
					}
					taskService.update(needRedoChildTask);
				}
			}
			log.info(needRedoTask + " 获得 " + needRedoChildTaskIds.size() + " 个子任务.");

			// 在重跑作业之前需要先清理参考点表
			Collection<WaitUpdateStatusTask> waitUpdateStatusTasks = waitUpdateStatusTaskService.getWaitUpdateStatusTasks(referTaskIds);
			if (waitUpdateStatusTasks.size() > 0) {
				for (WaitUpdateStatusTask waitUpdateStatusTask : waitUpdateStatusTasks) {
					// 一条条删除是为了防止表锁
					waitUpdateStatusTaskService.delete(waitUpdateStatusTask);
					log.info("删除参考点: " + waitUpdateStatusTask.getTaskName() + "(" + waitUpdateStatusTask.getJobId() + ").");
					this.log("[删除] 参考点: " + waitUpdateStatusTask.getTaskName() + "(" + waitUpdateStatusTask.getJobId() + ").");
				}
			}

			needRedoTask.setFlag(TaskFlag.SYSTEM.indexOf());
			// 只有成功或失败的作业才允许重跑
			needRedoTask.setTaskStatus(TaskStatus.RE_RUN_FAILURE.indexOf());
			taskService.update(needRedoTask);

			// 重跑该作业及其子作业
			boolean success = taskService.redo(needRedoTask.getTaskId(), needRedoChildTaskIds.toArray(new Long[needRedoChildTaskIds.size()]), false, 1l/*系统管理员*/);

			// 成功开始重跑后则需要删除GP上相对应的记录
			if (success) {
				this.log("[重跑] " + needRedoTask + "重跑逻辑已更新,等待调度执行.");
				PreparedStatement pstmt = null;

				try {
					this.connection();

					StringBuilder sql = new StringBuilder();
					sql.append("delete from report.src_job_date_control");
					sql.append(" where job_id = ?");
					sql.append(" and job_task_date = ?");
					sql.append(" and serial_id <= ").append(id);

					pstmt = connection.prepareStatement(sql.toString());
					pstmt.setLong(1, jobId);
					pstmt.setDate(2, new java.sql.Date(needRedoTask.getTaskDate().getTime()));
					int result = pstmt.executeUpdate();
					// int result = pstmt.executeUpdate("delete from report.src_job_date_control where serial_id = " + id);

					log.info("[删除] GP表(report.src_job_date_control)记录(作业ID: " + needRedoTask.getJobId() + ", 任务日期: " + DateUtil.formatDate(needRedoTask.getTaskDate()) + ")删除" +
							(result > 0 ? "成功" : "失败"));
					this.log("[删除] GP表(report.src_job_date_control)记录(作业ID: " + needRedoTask.getJobId() + ", 任务日期: " + DateUtil.formatDate(needRedoTask.getTaskDate()) + ")删除" +
							(result > 0 ? "成功" : "失败"));

				} catch (Exception e) {
					log.info("[删除] GP表(report.src_job_date_control)记录(作业ID: " + needRedoTask.getJobId() + ", 任务日期: " + DateUtil.formatDate(needRedoTask.getTaskDate()) + ")删除异常");
					this.log("[删除] GP表(report.src_job_date_control)记录(作业ID: " + needRedoTask.getJobId() + ", 任务日期: " + DateUtil.formatDate(needRedoTask.getTaskDate()) + ")删除异常");
					e.printStackTrace();
					throw e;

				} finally {
					try {
						if (pstmt != null) {
							pstmt.close();
						}

						this.release();
					} catch (Exception e) {}
				}

			} else {
				this.log("[重跑] " + needRedoTask + "重跑逻辑处理失败.");
			}
		}

	}

	/**
	 * 从GP中获得需要重跑的所有作业
	 * 
	 * @return
	 * @throws Exception
	 */
	private Collection<Map<String, Object>> getNeedRedoJobs() throws Exception {
		Statement stmt = null;
		ResultSet rs = null;

		// 需要自动重跑的作业
		Collection<Map<String, Object>> needRedoJobs = new LinkedHashSet<Map<String, Object>>();

		try {
			this.connection();

			stmt = connection.createStatement();
			rs = stmt.executeQuery("select job_id,job_task_date,max(serial_id) serial_id from report.src_job_date_control group by job_id,job_task_date");

			while (rs.next()) {
				Map<String, Object> job = new HashMap<String, Object>();
				job.put("id", rs.getLong("serial_id"));
				job.put("jobId", rs.getLong("job_id"));
				job.put("taskDate", new Date(rs.getDate("job_task_date").getTime()));

				needRedoJobs.add(job);
			}

		} catch (Exception e) {
			log.error("从Greenplum中获得src_job_date_control表数据失败.");
			throw e;

		} finally {
			try {
				if (rs != null) {
					rs.close();
				}

				if (stmt != null) {
					stmt.close();
				}

				this.release();
			} catch (Exception e) {}
		}

		return needRedoJobs;
	}

	/**
	 * 连接GP数据源
	 * 
	 * @return
	 */
	private void connection() {
		try {
			Class.forName(Configure.property(Configure.GREENPLUM_DATABASE_CONNECTION_DRIVER_CLASS)).newInstance();

			connection = DriverManager.getConnection(Configure.property(Configure.GREENPLUM_DATABASE_CONNECTION_URL), Configure.property(Configure.GREENPLUM_DATABASE_CONNECTION_USERNAME), Configure
					.property(Configure.GREENPLUM_DATABASE_CONNECTION_PASSWORD));

		} catch (ClassNotFoundException e) {
			log.error("not found greenplum connection driver.", e);
		} catch (Exception e) {
			log.error("create greenplum connection exception.", e);
		}

		log.debug("create greenplum connection success.");
	}

	private void release() {
		try {
			if (connection != null && !connection.isClosed()) {
				connection.close();
			}
		} catch (SQLException e) {
			log.error("release greenplum connection exception.", e);
		}

		log.debug("release greenplum connection success.");
	}

	private void log(String content) throws IOException {
		writer.write(DateUtil.format(DateUtil.now(), "yyyyMMdd HH:mm:ss") + " - " + content + "\n");
		writer.flush();
	}

	public static void main(String[] args) throws Exception {
		ManualExportJobAutoRedo manualExportJobAutoRedo = ManualExportJobAutoRedo.getManualExportJobAutoRedo();

		long interval = Integer.parseInt(args[0]) * 60 * 1000; // 间隔(毫秒)
		long taskId = Long.parseLong(args[1]); // 任务ID

		manualExportJobAutoRedo.execute(taskId, interval);
	}
}
