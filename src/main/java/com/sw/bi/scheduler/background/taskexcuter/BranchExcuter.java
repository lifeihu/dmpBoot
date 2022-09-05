package com.sw.bi.scheduler.background.taskexcuter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.beanutils.ConvertUtils;
import org.springframework.util.StringUtils;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.model.Job;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.service.JobService;
import com.sw.bi.scheduler.service.TaskService;
import com.sw.bi.scheduler.util.Configure.JobStatus;
import com.sw.bi.scheduler.util.Configure.TaskFlag;
import com.sw.bi.scheduler.util.Configure.TaskStatus;
import com.sw.bi.scheduler.util.DateUtil;
import com.sw.bi.scheduler.util.ExecAgent.ExecResult;
import com.sw.bi.scheduler.util.SshUtil;

/**
 * 分支作业执行器
 * 
 * @author shiming.hong
 * 
 */
public class BranchExcuter extends AbExcuter {
	public static final String MASTER_RETURN_VALUE_PREFIX = "master_branch_return_value=";

	private JobService jobService = BeanFactory.getService(JobService.class);
	private TaskService taskService = BeanFactory.getService(TaskService.class);

	private Job masterJob;

	public BranchExcuter(Task task, String logFolder) {
		super(task, logFolder);
	}

	@Override
	public boolean excuteCommand() throws InterruptedException, IOException {
		Long[] jobIds = (Long[]) ConvertUtils.convert(currentJob.getProgramPath().split(","), Long.class);

		if (jobIds.length == 0) {
			log("未找到指定的主从分支作业(" + currentJob.getProgramPath() + ").");
			return false;
		}

		List<Job> jobs = new ArrayList<Job>(jobIds.length); // jobService.query((Long[]) ConvertUtils.convert(currentJob.getProgramPath().split(","), Long.class));

		// 如有未上线作业则认为执行失败
		for (Long jobId : jobIds) {
			Job job = jobService.get(jobId);
			jobs.add(job);

			if (job.getJobStatus() != JobStatus.ON_LINE.indexOf()) {
				log(job.toString() + " 未上线.");
				return false;
			}
		}

		// 执行主作业
		masterJob = jobs.get(0);
		Integer[] results = this.executeMasterJob(masterJob);

		// 没有返回值则表示执行失败
		if (results == null) {
			return false;
		}

		// 执行成功,但如果是返回0则表示不需要选择分支作业,直接成功
		if (results.length == 1 && results[0] == 0) {
			log(currentJob + "执行成功, 不需要执行分支作业.");
			return true;
		}

		log("主" + masterJob + "执行成功, 选取的分支作业: " + Arrays.asList(results) + ".");

		// 开始执行分支任务
		for (int i = 0, len = results.length; i < len; i++) {
			int idx = results[i];

			if (idx < 1 || idx >= jobs.size()) {
				// TODO 这种情况是否直接失败还是允许执行下一个分支
				log("指定了一个不存在的分支作业序号(" + idx + ").");
				return false;
			}

			// 只要有一个分支执行失败就直接失败退出
			if (!this.executeBranchJob(jobs.get(results[i]))) {
				return false;
			}
		}

		return true;
	}

	/**
	 * 执行指定的主作业
	 * 
	 * @param job
	 * @return 返回需要执行的分支作业序号(从1开始,如果返回0表示不需要执行从作业)
	 */
	private Integer[] executeMasterJob(Job job) {
		Date taskDate = currentTask.getTaskDate();

		Collection<Task> masterTasks = taskService.getTasksByJob(job.getJobId(), taskDate);

		// 没有找到主作业时需要自动生成
		if (masterTasks.size() == 0) {
			jobService.createTasks(job, taskDate, TaskFlag.ONLINE.indexOf());
			masterTasks = taskService.getTasksByJob(job.getJobId(), taskDate);
		}

		for (Task masterTask : masterTasks) {
			masterTask.setTaskStatus(masterTask.getTaskStatus() == TaskStatus.RE_TRIGGERED.indexOf() ? TaskStatus.RE_RUNNING.indexOf() : TaskStatus.RUNNING.indexOf());
			masterTask.setTaskBeginTime(DateUtil.now());
			taskService.update(masterTask);

			AbExcuter excuter = ExcuterFactory.getExcuterByJobType(masterTask, this.m_logFolder);

			log("开始执行主" + masterTask);

			boolean result = excuter.excute();

			// 在此处将分支作业的日志文件输出是为了后期在查看日志时可以根据此路径查到分支作业的具体日志信息
			log(excuter.getLogPathName());

			log("主" + masterTask + "执行" + (result ? "成功" : "失败") + ".");

			if (result) {
				Integer[] results = this.getReturnValue(excuter.getLogPathName());
				return results;
			}
		}

		return null;
	}

	@Deprecated
	private Integer[] executeMasterJob1(Job job) {
		/*int jobType = (int) job.getJobType();
		String command = job.getProgramPath();

		Integer[] results = null;
		Process process = null;

		if (jobType == JobType.SHELL.indexOf()) {
			results = this.runShell(command);

		} else if (jobType == JobType.MAPREDUCE.indexOf()) {
			results = this.runMapReduce(command);

		} else {
			log("主作业必须是 \"Shell作业\" 或 \"MapReduce作业\" .");
			return null;
		}*/

		/*BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		try {
			String line = null;
			String returnValue = null;
			while ((line = reader.readLine()) != null) {
				log(line);

				if (StringUtils.hasText(line) && line.indexOf(MASTER_RETURN_VALUE_PREFIX) > -1) {
					String[] token = line.split(MASTER_RETURN_VALUE_PREFIX);
					returnValue = token[1];

					if (StringUtils.hasText(returnValue)) {
						results = (Integer[]) ConvertUtils.convert(returnValue.split(","), Integer.class);

					} else {
						log("主" + job + "中未指定返回值.");
						return null;
					}
				}
			}

			process.waitFor();

			if (process.exitValue() == 0) {
				log("主" + job + " 执行成功, 选取的分支作业: " + returnValue + ".");

			} else {
				// 主进程执行失败就算有返回值也算失败
				log("主" + job + "进程执行失败.");
				return null;
			}

		} catch (IOException e) {
			log("读取主" + job + "日志信息失败.");
			return null;

		} catch (InterruptedException e) {
			log(e.getMessage());

		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}*/

		// return results;
		return null;
	}

	/**
	 * 执行指定分支任务
	 * 
	 * @param job
	 * @return
	 */
	private boolean executeBranchJob(Job job) {
		Date taskDate = currentTask.getTaskDate();

		Collection<Task> branchTasks = taskService.getTasksByJob(job.getJobId(), taskDate);

		if (branchTasks.size() == 0) {
			jobService.createTasks(job, taskDate, TaskFlag.ONLINE.indexOf());
			branchTasks = taskService.getTasksByJob(job.getJobId(), taskDate);
		}

		log("根据分支" + job + "得到" + branchTasks.size() + "个分支任务.");

		for (Task branchTask : branchTasks) {
			branchTask.setTaskStatus(branchTask.getTaskStatus() == TaskStatus.RE_TRIGGERED.indexOf() ? TaskStatus.RE_RUNNING.indexOf() : TaskStatus.RUNNING.indexOf());
			branchTask.setTaskBeginTime(DateUtil.now());
			taskService.update(branchTask);

			AbExcuter excuter = ExcuterFactory.getExcuterByJobType(branchTask, this.m_logFolder);

			log("开始执行分支" + branchTask);

			boolean result = excuter.excute();

			// 在此处将分支作业的日志文件输出是为了后期在查看日志时可以根据此路径查到分支作业的具体日志信息
			log(excuter.getLogPathName());

			log("分支" + branchTask + "执行" + (result ? "成功" : "失败") + ".");

			if (!result) {
				return false;
			}
		}

		return true;
	}

	/**
	 * 从指定的Action日志文件中获得返回值
	 * 
	 * @param actionLogFile
	 * @return
	 */
	private Integer[] getReturnValue(String actionLogFile) {
		String gateway = this.currentAction.getGateway();

		ExecResult execResult = SshUtil.execCommand(gateway, "cat " + actionLogFile);
		if (execResult.failure()) {
			log("获得主" + masterJob + "返回值失败," + execResult.getStderr());
			return null;
		}

		Integer[] results = null;
		String[] lines = execResult.getStdoutAsArrays();
		for (String line : lines) {
			if (StringUtils.hasText(line) && line.indexOf(MASTER_RETURN_VALUE_PREFIX) > -1) {
				String[] token = line.split(MASTER_RETURN_VALUE_PREFIX);
				String returnValue = token[1];

				if (StringUtils.hasText(returnValue)) {
					results = (Integer[]) ConvertUtils.convert(returnValue.split(","), Integer.class);

				} else {
					log("主" + masterJob + "执行失败,未指定返回值.");
					return null;
				}
			}
		}

		return results;
	}

	/**
	 * 运行Shell脚本(默认给shell脚本2个参数 $1 $2 $1是天. $2是小时)
	 * 
	 * @param commnd
	 * @return
	 * @throws IOException
	 */
	/*private Integer[] runShell(String commnd) {
		Map<String, String> params = Parameters.getRunTimeParamter(currentTask);

		Process process = null;
		Integer[] results = null;

		try {
			// java调用shell脚本并且传入了两个参数. $1表示yyyyMMdd  $2表示yyyyMMddHH
			String[] commands = new String[] { "/bin/bash", commnd, params.get("${date_desc}"), params.get("${hour_desc}") };
			process = Runtime.getRuntime().exec(commands);

			// 从InputStream中获得主作业返回值
			results = this.getReturnValue(process.getInputStream());

			process.waitFor();

			if (process.exitValue() == 0) {
				log("主" + masterJob + "执行成功, 选取的分支作业: " + Arrays.asList(results) + ".");

			} else {
				// 主进程执行失败就算有返回值也算失败
				log("主" + masterJob + "执行失败, 进程返回值1.");
				return null;
			}

		} catch (IOException e) {
			log("Shell进程执行失败.");
			return null;

		} catch (InterruptedException e) {
			log(e.getMessage());
			return null;
		}

		return results;
	}*/

	/**
	 * 运行MapReduce程序
	 * 
	 * @param command
	 * @return
	 */
	/*private Integer[] runMapReduce(String command) {
		// 替换参数
		for (Entry<String, String> entry : Parameters.getRunTimeParamter(currentTask).entrySet()) {
			command = command.replaceAll(entry.getKey(), entry.getValue());
		}

		Integer[] results = null;

		try {
			String[] commands = new String[] { "/bin/bash", "-c", command + " > " + this.getLogPathName() + " 2>&1" };
			Process process = Runtime.getRuntime().exec(commands);
			process.waitFor();

			if (process.exitValue() == 0) {
				results = this.getReturnValue(this.getLogPathName());

			} else {
				// 主进程执行失败就算有返回值也算失败
				log("主" + masterJob + "执行失败, 进程返回值1.");
				return null;
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return results;
	}*/

	/**
	 * 从InputStream中获得作业返回值
	 * 
	 * @param is
	 * @return
	 */
	/*private Integer[] getReturnValue(InputStream is) {
		Integer[] results = null;
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		try {
			String line = null;
			String returnValue = null;
			while ((line = reader.readLine()) != null) {
				log(line);

				if (StringUtils.hasText(line) && line.indexOf(MASTER_RETURN_VALUE_PREFIX) > -1) {
					String[] token = line.split(MASTER_RETURN_VALUE_PREFIX);
					returnValue = token[1];

					if (StringUtils.hasText(returnValue)) {
						results = (Integer[]) ConvertUtils.convert(returnValue.split(","), Integer.class);

					} else {
						log("主" + masterJob + "执行失败,未指定返回值.");
						return null;
					}
				}
			}

		} catch (IOException e) {
			log("读取主" + masterJob + "日志信息失败.");
			return null;

		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return results;
	}*/

}
