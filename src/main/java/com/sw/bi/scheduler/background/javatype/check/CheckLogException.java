package com.sw.bi.scheduler.background.javatype.check;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.model.Action;
import com.sw.bi.scheduler.model.Gateway;
import com.sw.bi.scheduler.model.Job;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.service.ActionService;
import com.sw.bi.scheduler.service.GatewayService;
import com.sw.bi.scheduler.service.JobService;
import com.sw.bi.scheduler.service.TaskService;
import com.sw.bi.scheduler.service.UserService;
import com.sw.bi.scheduler.util.Configure.ActionStatus;
import com.sw.bi.scheduler.util.Configure.JobType;
import com.sw.bi.scheduler.util.Configure.TaskStatus;
import com.sw.bi.scheduler.util.DateUtil;
import com.sw.bi.scheduler.util.ExecAgent.ExecResult;
import com.sw.bi.scheduler.util.SshUtil;

/**
 * 检测那些进程还存在,但是程序已经不在往日志写入的异常情况,并自动进行一定的修复 2012-07-20
 * 
 * @author feng.li
 * 
 */
//     /home/tools/scheduler/scheduler jar   /home/tools/scheduler/scheduler.jar com.sw.bi.scheduler.background.javatype.check.CheckLogException
//     /home/tools/scheduler/scheduler_test jar   /home/tools/scheduler/scheduler_test.jar com.sw.bi.scheduler.background.javatype.check.CheckLogException
//     ★普通的java main方法执行的任务,里面使用System.out.println朝对应的action日志输出日志
@Component
public class CheckLogException {

	@Autowired
	private ActionService actionService;

	@Autowired
	private JobService jobService;

	@Autowired
	private TaskService taskService;

	@Autowired
	private GatewayService gatewayService;

	@Autowired
	private UserService userService;

	/*@Autowired(required = false)
	// @Qualifier("SWSmsSender")
	@Qualifier("WebSmsSender")
	private SmsSender smsSender;*/

	/*@Autowired
	private SmsService smsService;*/

	// 白名单作业
	private static Collection<Long> whiteJobs = new HashSet<Long>();

	// public static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	private static Collection<Gateway> gateways = null;

	public static void main(String args[]) {
		// 添加白名单作业
		whiteJobs.add(1686l);

		String today = DateUtil.formatDate(new Date());
		CheckLogException.CheckTaskException().check(today);

		String yesterday = DateUtil.formatDate(DateUtil.getYesterday());
		CheckLogException.CheckTaskException().check(yesterday);

	}

	/**
	 * 校验所有网关中进程在但已经不写日志的任务Action
	 * 
	 * @param checkDate
	 */
	public void check(String checkDate) {
		this.getActiveGateways();

		for (Gateway gateway : gateways) {
			String gatewayName = gateway.getName();
			ExecResult execResult = SshUtil.execCommand(gatewayName, "/usr/sbin/lsof /data1/tools/logs/etl_log/" + checkDate + "/*|awk -F \" \" '{print $9}'");

			if (execResult.isEmptyStdout()) {
				continue;
			}

			String[] logs = execResult.getStdoutAsArrays();
			for (int i = 1, len = logs.length; i < len; i++) {
				String log = logs[i];
				//System.out.println("log: "+log);

				// 获得日志文件最近一次修改时间
				// 注意,下面的ls -l不能直接写成ll   没有ll这个命令的,ll只是一个别名
				execResult = SshUtil.execCommand(gatewayName, "ls -l --full-time anaconda-ks.cfg " + log + " | awk -F \" \" '{print $6 \" \" $7}'");
				String updateTime = execResult.getStdout();
				Date logUpdateTime = DateUtil.parse(updateTime.substring(0, updateTime.lastIndexOf(":")), "yyyy-MM-dd HH:mm");
				Date now = DateUtil.now();

				int diffMinites = (int) (now.getTime() - logUpdateTime.getTime()) / 1000 / 60;

				// 已经有30分钟不朝日志文件写入日志了,进程还存在. 这样的情况就判定为日志异常
				// 就要把对应的action记录状态修改为:日志异常; 同时将task记录状态修改为运行失败
				// 有些程序莫名其妙的卡住10分钟,但是最终还是正常运行完毕的。 所以暂时把这个异常时间临界点调整为30分钟
				if (diffMinites > 30) {
					System.out.println(DateUtil.formatDateTime(new Date()) + ": " + "当前日志文件时间: " + updateTime + ",距离当前系统时间已经相差[" + diffMinites + "]分钟没有写入日志了");
					System.out.println(DateUtil.formatDateTime(new Date()) + ": " + "系统判定该任务出现了日志异常,现在开始进行自动修复");

					//    /home/tools/logs/etl_log/2012-07-20/52560.log
					int last = log.lastIndexOf("/");
					int dian = log.indexOf(".");
					long actionId = Long.valueOf(log.substring(last + 1, dian));
					System.out.println(actionId);

					Action action = actionService.get(actionId);

					// 忽略白名单中的作业
					if (whiteJobs.contains(action.getJobId())) {
						continue;
					}

					// TODO 暂时需要屏蔽所有导入MySQL的作业、存储过程作业
					int jobType = action.getJobType().intValue();
					if (jobType == JobType.HDFS_TO_MYSQL.indexOf() || jobType == JobType.LOCAL_FILE_TO_MYSQL.indexOf() || jobType == JobType.SQLSERVER_TO_MYSQL.indexOf() ||
							jobType == JobType.ORACLE_TO_MYSQL.indexOf() || jobType == JobType.MYSQL_TO_MYSQL.indexOf() || jobType == JobType.CSV_TO_MYSQL.indexOf() ||
							jobType == JobType.GP_TO_MYSQL.indexOf() || jobType == JobType.STORE_PROCEDURE.indexOf()) {
						continue;
					}

					// 屏蔽所有导入Greenplum的作业
					if (jobType == JobType.HDFS_TO_GP.indexOf() || jobType == JobType.LOCAL_FILE_TO_GP.indexOf() || jobType == JobType.SQLSERVER_TO_GP.indexOf() ||
							jobType == JobType.ORACLE_TO_GP.indexOf() || jobType == JobType.MYSQL_TO_GP.indexOf() || jobType == JobType.CSV_TO_GP.indexOf() || jobType == JobType.GP_TO_GP.indexOf() ||
							jobType == JobType.GREENPLUM_FUNCTION.indexOf()) {
						continue;
					}

					// 屏蔽MapReduce类型中的同步Cube作业
					if (jobType == JobType.MAPREDUCE.indexOf()) {
						Job job = jobService.get(action.getJobId());
						String programPath = job.getProgramPath();
						if (StringUtils.hasText(programPath) && programPath.indexOf("com.sw.bi.scheduler.background.javatype.sa.SyncCube") > -1) {
							continue;
						}
					}

					if (action.getActionStatus() == ActionStatus.RUNNING.indexOf()) {
						action.setActionStatus(ActionStatus.LOG_EXCEPTION.indexOf());
						action.setEndTime(new Date());
						actionService.saveOrUpdate(action);

						long taskId = action.getTaskId();
						Task task = taskService.get(taskId);
						if (!task.isRunSuccess()) {
							task.setTaskStatus(TaskStatus.RUN_FAILURE.indexOf());
							taskService.saveOrUpdate(task);
							System.out.println(DateUtil.formatDateTime(new Date()) + ": " + "task_id: " + taskId + "状态被置为运行失败");
						}

						System.out.println(DateUtil.formatDateTime(new Date()) + ": " + "action_id: " + actionId + "状态被置为日志异常");

						// smsService.sendMsg(userService.get(task.getDutyOfficer()).getMobilePhone(), task + "运行失败, 状态被置为日志异常.");
					}
				} else {
					System.out.println(DateUtil.formatDateTime(new Date()) + ": " + "日志文件: " + log + "正常!");
				}
			}
		}

		/*String logs = programeRun("/usr/bin/lsof /home/tools/logs/etl_log/" + checkDate + "/*|awk -F \" \" '{print $9}'");
		///System.out.println(logs);
		String[] strList = logs.split("\n");
		for (String log : strList) {
			if (log.startsWith("/home/tools/logs/etl_log/")) {
				///System.out.println(log);
				//注意,下面的ls -l不能直接写成ll   没有ll这个命令的,ll只是一个别名
				String now_log_time = programeRun("/bin/ls -l " + log + " | awk -F \" \" '{print $6 \" \" $7}'");
				///System.out.println(now_log_time);
				Date now_logtime = DateUtil.parseDateTime(now_log_time);
				Date nowtime = DateUtil.now();
				int diff_minites = (int) (nowtime.getTime() - now_logtime.getTime()) / 1000 / 60;
				// 已经有10分钟不朝日志文件写入日志了,进程还存在. 这样的情况就判定为日志异常
				// 就要把对应的action记录状态修改为:日志异常; 同时将task记录状态修改为运行失败
				if (diff_minites > 10) {
					System.out.println(DateUtil.formatDateTime(new Date()) + ": " + "当前日志文件时间: " + now_log_time + ",距离当前系统时间已经相差[" + diff_minites + "]分钟没有写入日志了");
					System.out.println(DateUtil.formatDateTime(new Date()) + ": " + "系统判定该任务出现了日志异常,现在开始进行自动修复");

					//    /home/tools/logs/etl_log/2012-07-20/52560.log
					int last = log.lastIndexOf("/");
					int dian = log.indexOf(".");
					long action_id = Long.valueOf(log.substring(last + 1, dian));
					System.out.println(action_id);
					Action action = actionService.get(action_id);
					if (action.getActionStatus() == ActionStatus.RUNNING.indexOf()) {
						action.setActionStatus(ActionStatus.LOG_EXCEPTION.indexOf());
						action.setEndTime(new Date());
						actionService.saveOrUpdate(action);

						long task_id = action.getTaskId();
						Task task = taskService.get(task_id);
						task.setTaskStatus(TaskStatus.RUN_FAILURE.indexOf());
						taskService.saveOrUpdate(task);

						System.out.println(DateUtil.formatDateTime(new Date()) + ": " + "action_id: " + action_id + "状态被置为日志异常");
						System.out.println(DateUtil.formatDateTime(new Date()) + ": " + "task_id: " + task_id + "状态被置为运行失败");
					}
				} else {
					System.out.println(DateUtil.formatDateTime(new Date()) + ": " + "日志文件: " + log + "正常!");
				}
			}
		}*/

	}

	/**
	 * 获得所有启用状态的网关机
	 * 
	 * @return
	 */
	private Collection<Gateway> getActiveGateways() {
		if (gateways == null) {
			gateways = gatewayService.getActiveGateways();
		}

		return gateways;
	}

	private static CheckLogException CheckTaskException() {
		return BeanFactory.getBean(CheckLogException.class);
	}

	/**
	 * 传入一个linux下的shell命令行, 调用该方法,得到该shell命令的返回字符串
	 * 
	 * @param command
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	/*private String programeRun(String command) throws IOException, InterruptedException {

		String[] commands = new String[] { "/bin/bash", "-c", command };
		Process child = Runtime.getRuntime().exec(commands);
		child.waitFor();
		BufferedInputStream in = new BufferedInputStream(child.getInputStream());
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String s = "";
		String return_str = "";
		while ((s = br.readLine()) != null) {
			return_str += s + "\n";
		}
		return return_str;
	}*/

}
