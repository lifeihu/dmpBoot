package com.sw.bi.scheduler.background.javatype.check;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.model.Action;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.service.ActionService;
import com.sw.bi.scheduler.service.TaskService;
import com.sw.bi.scheduler.service.UserService;
import com.sw.bi.scheduler.util.Configure.ActionStatus;
import com.sw.bi.scheduler.util.Configure.TaskStatus;
import com.sw.bi.scheduler.util.DateUtil;
import com.sw.bi.scheduler.util.ExecAgent.ExecResult;
import com.sw.bi.scheduler.util.SshUtil;

/**
 * 异常记录检测任务,可以设置为分钟级别的任务,定时检测异常记录,并自动修复异常记录. 2012-07-03
 * 这个脚本也有监控的盲点，它是先查询action表，然后找是否有对应的进程。 但是有下面这种情况无法自动修复：
 * 只有task记录，没有action,没有进程。。。
 * 
 * @author feng.li
 * 
 */
//     /home/tools/scheduler/scheduler jar   /home/tools/scheduler/scheduler.jar com.sw.bi.scheduler.background.javatype.check.CheckTaskException today
@Component
public class CheckTaskException {

	@Autowired
	private ActionService actionService;

	@Autowired
	private TaskService taskService;

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

	public static void main(String args[]) throws IOException, InterruptedException {
		// 添加白名单作业

		String type = args[0]; // all   or   today
		Date taskDate = DateUtil.getToday();
		List<Action> list = CheckTaskException.getCheckTaskException().getActions(taskDate, type);

		CheckTaskException.getCheckTaskException().check(list);

	}

	public void check(List<Action> list) throws IOException, InterruptedException {
		for (Action action : list) {
			String logpath = action.getActionLog();
			if (!logpath.endsWith(".log")) {
				logpath += action.getActionId() + ".log";
			}
			System.out.println("日志文件: " + logpath);

			ExecResult execResult = SshUtil.execCommand(action.getGateway(), "/usr/sbin/lsof " + logpath + " | wc -l");

			System.out.println("begin to check actionid: " + action.getActionId());
			if (execResult.success() && execResult.getStdout().startsWith("0")) {
				long actionId = action.getActionId();
				action = actionService.get(actionId);

				// 忽略白名单中的作业
				if (whiteJobs.contains(action.getJobId())) {
					continue;
				}

				//从数据库里重新加载action对象,获取最新的action_status,如果是运行中,而又没对应的进程,那么肯定是异常记录.
				if (action.getActionStatus() == ActionStatus.RUNNING.indexOf()) {
					//这里要修复这些异常数据,action表记录action_status置为异常.  task表记录task_status置为失败
					action.setActionStatus(ActionStatus.RUN_EXCEPTION.indexOf());
					action.setEndTime(new Date()); //把异常的时间也记录一下,方便查找问题
					actionService.saveOrUpdate(action);
					System.out.println("job_id: "+action.getJobId()+"job_name: "+action.getJobName()+"actionid: " + action.getActionId() + " running exception,repair successfully.");

					long taskId = action.getTaskId();
					Task task = taskService.get(taskId);

					// 查一下action表中是否存在与该异常action记录    相同task_id,相同task_date,相同start_time,相同create_time,且运行状态是成功的记录
					// 如果已经存在这样的action记录,则不要再把对应的task记录的状态修改为运行失败了.
					// 加这个逻辑条件的原因是: 发现action表中偶尔会出现2条相同的记录,而产生这2条相同记录的原因又不明,暂时的临时解决方法
					// 把多余出来的那条action记录修改为异常. 但是如果已经存在成功的action记录,那么就不要去修改task记录了,这样后续的程序才能继续运行下去,不至于卡住.
					Action successAction = actionService.getAction(taskId, action.getTaskDate(), action.getStartTime(), action.getCreateTime());
					if (successAction == null) {
						// Task task = taskService.get(taskId);
						//  有可能对作业进行了下线又上线的操作,导致生成的task记录被物理删除了,所以可能会存在action记录对应的task记录已经不存在的现象
						// 目前发现一个现象：同一个进程，对应两条action记录。当一条action记录执行成功后，回填task状态是运行成功。 但是这个时候由于进程消失，另外一个action记录最终被检测为异常，并且回填task状态为运行失败
						// 所以这里增加一个判断条件： 如果检测异常时，要回填的那个task状态已经是运行成功了，就不要回填了。
						if (task != null && task.getTaskStatus() != (long) TaskStatus.RUN_SUCCESS.indexOf() && task.getTaskStatus() != (long) TaskStatus.RE_RUN_SUCCESS.indexOf()) {
							task.setTaskStatus(TaskStatus.RUN_FAILURE.indexOf());
							taskService.saveOrUpdate(task);
							System.out.println("job_id: "+action.getJobId()+"job_name: "+action.getJobName()+"taskid: " + action.getTaskId() + " running exception,repair successfully.");
						}
					}

					// smsService.sendMsg(userService.get(task.getDutyOfficer()).getMobilePhone(), task + "运行失败, 状态被置为进程异常.");
				}
			} else {
				System.out.println("actionid: " + action.getActionId() + " is normal.");
			}
		}
	}

	private static CheckTaskException getCheckTaskException() {
		return BeanFactory.getBean(CheckTaskException.class);
	}

	// 注意: 这里是查的action实例中的异常记录,action表只有一个task_date字段,所以查出来的异常记录不是100%准
	// 现实中可能存在的情况是task表中的异常记录的task_date与scan_date不是同一天....   所以结合手工 select * from task where scan_date = '2012-07-09' and (task_status = 3 or task_status = 9)
	public List<Action> getActions(Date taskDate, String type) {
		List<Action> list = null;
		if ("all".equals(type)) {
			list = actionService.getAllRunningActions();
		} else if ("today".equals(type)) {
			list = actionService.getRunningActions(taskDate);
		}
		return list;
	}

	/**
	 * 根据action记录的日志文件,查找当前正在访问这个日志文件的进程,如果找到进程,说明这个action实例是正常的.
	 * 如果找不到进程了,说明这条action记录是异常记录了
	 * 注意:如果找不到进程,也有一种可能是这个进程正好运行结束了,所以还要反查一下这条action记录
	 * ,看他的最新状态是什么,如果最新状态依然是运行中,则必定是异常记录, 如果运行状态已经变为成功或者失败了,那么这条记录是正常的记录,不是异常记录.
	 * 
	 * @param logpath
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	/*private String programeRun(String logpath) throws IOException, InterruptedException {

		String[] commands = new String[] { "/bin/bash", "-c", "/usr/bin/lsof " + logpath + " | wc -l" };
		Process child = Runtime.getRuntime().exec(commands);
		child.waitFor();
		BufferedInputStream in = new BufferedInputStream(child.getInputStream());
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String s;
		while ((s = br.readLine()) != null) {
			if (Integer.parseInt(s) > 0) {
				return "1";
			} else {
				return "0"; //表示进程已经消失
			}
		}
		return "0";
	}*/
}
