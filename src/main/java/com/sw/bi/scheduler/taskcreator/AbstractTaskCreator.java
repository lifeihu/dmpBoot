package com.sw.bi.scheduler.taskcreator;

import com.sw.bi.scheduler.model.Job;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.service.JobService;
import com.sw.bi.scheduler.service.TaskService;
import com.sw.bi.scheduler.util.Configure.JobType;
import com.sw.bi.scheduler.util.Configure.TaskFlag;
import com.sw.bi.scheduler.util.Configure.TaskStatus;
import com.sw.bi.scheduler.util.DateUtil;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

public abstract class AbstractTaskCreator implements TaskCreator {
	protected static final Logger log = Logger.getLogger(AbstractTaskCreator.class);

	/**
	 * 前一天各作业的运行时长
	 */
	private static Map<String, Long> yesterdayRunTimes;

	@Autowired
	private TaskService taskService;

	@Autowired
	private JobService jobService;

	protected Job job;
	protected Calendar taskDate;
	private Integer taskFlag;

	@Override
	public void create(Job job, Calendar taskDate, Integer taskFlag) {
		this.job = job;
		this.taskDate = taskDate;
		this.taskFlag = taskFlag;

		// 计算前一天各作业的运行时长
		if (yesterdayRunTimes == null || yesterdayRunTimes.size() == 0) {
			this.calculateReferRunTime();
		}

		this.createTasks();
	}

	/**
	 * 创建任务，这个方法还只是在内存中new出来了一个Task对象,并设置相关的属性,还不涉及到数据库层面的操作
	 * 
	 * @return
	 */
	protected Task createTask() {
		return createTask(DateUtil.cloneCalendar(taskDate));
	}

	/**
	 * 创建任务: 根据作业信息创建任务记录
	 * 
	 * @param initializeDate
	 *            任务初始执行时间,需要根据Job中的time计算出实际执行时间
	 * @return
	 */
	protected Task createTask(Calendar initializeDate) {
		Task task = new Task();
		Long jobId = job.getJobId();

		boolean isRoot = jobId == 1l;

		task.setJobId(jobId);
		task.setJobName(job.getJobName());
		task.setJobDesc(job.getJobDesc());
		task.setJobBusinessGroup(job.getJobBusinessGroup());
		task.setDutyOfficer(job.getDutyOfficer());
		task.setJobType(job.getJobType());
		task.setJobLevel(job.getJobLevel());
		task.setGateway(job.getGateway());
		task.setFailureRerunTimes(job.getFailureRerunTimes());
		task.setFailureRerunInterval(job.getFailureRerunInterval());
		task.setFlag(TaskFlag.SYSTEM.indexOf());
		task.setFlag2(taskFlag); // 对于新建的任务该字段只会是新上线作业产生的任务才会有值且只会是"新上线"
		task.setTaskDate(taskDate.getTime());
		//task.setScanDate(taskDate.getTime());
		task.setScanDate(new Date()); //因为现在传入时,可能创建的日期是前几天的,所以这里直接写死scanDate是今天就可以了

		if (job.getJobType() == JobType.CHECK_DAY_DEPENDENCY_HOUR.indexOf() || job.getJobType() == JobType.CHECK_MONTH_DEPENDENCY_DAY.indexOf()) {
			// 检查类型的作业直接将状态改为已触发。 因为检查类型的作业，他的依赖是前一天的任务，如果创建任务的时候，状态设置为初始化状态，那么就无法扫描到这些点。
			// 只有在创建任务的时候，把状态设置为已触发状态，这样调度才能选取到这些点。  然后在依赖检查执行器中，去判断前一天的任务是否已经完成了。
			task.setTaskStatus(TaskStatus.TRIGGERED.indexOf());
			task.setReadyTime(DateUtil.now());
		} else {
			task.setTaskStatus(TaskStatus.INITIALIZE.indexOf());
		}

		task.setAlert(job.getAlert());
		task.setCycleType((int) job.getCycleType());
		task.setRunTimes(0);
		task.setCreateTime(DateUtil.getToday());
		task.setSettingTime(taskService.calculateSettingTime(task, initializeDate.getTime()));
		task.setReferRunTime(yesterdayRunTimes.get(jobId + "_" + DateUtil.format(task.getSettingTime(), "HHmm")));

		// 根作业创建时,状态直接置成运行成功。 虚拟作业创建时,状态是初始化,因为现在开发了一个针对虚拟作业的虚拟作业执行器
		if (isRoot/* || job.getJobType() == JobType.VIRTUAL.indexOf()*/) {
			task.setTaskStatus(TaskStatus.RUN_SUCCESS.indexOf());
			task.setReadyTime(DateUtil.now());
			task.setTaskBeginTime(task.getReadyTime());
			task.setTaskEndTime(task.getReadyTime());
			task.setRunTime(0l);

		} else {
			Long[] prevJobIds = job.getPrevJobIds();
			if (prevJobIds != null && prevJobIds.length > 0) {
				// 对于前置依赖自己的作业在这里先禁止生成前置任务
				// 因为这个时候这个作业的所有任务没有全部生成，依赖自己的前置任务也是生成不出来的
				// 所以对于依赖自己的前置任务会在TaskCreatorRunner.create接口中commit后生成
				if (!job.isPrevJobDependenceSelf()) {
					Collection<Task> prevTasks = taskService.getFrontTasks(task, prevJobIds);

					// 设置任务的前置任务。 前置任务的任务id以逗号分隔,保存在task的this.preTasks字段中
					task.setPreTasksByCollection(prevTasks);
				}
			}
			/*String prevJobs = job.getPrevJobs(); // 录入作业时填写的前置作业ids
			if (StringUtils.hasText(prevJobs)) {
				Long[] prevJobIds = (Long[]) ConvertUtils.convert(prevJobs.split(","), Long.class);
				Collection<Task> prevTasks = taskService.getPredecessorTasks(task, prevJobIds);
				task.setPreTasksByCollection(prevTasks); //设置任务的前置任务。 前置任务的任务id以逗号分隔,保存在task的this.preTasks字段中
			}*/
		}

		return task;
	}

	/**
	 * 创建任务, 并pstmt.addBatch()。 还没到执行这一步
	 * 
	 * @param task
	 */
	protected void addTask(Task task) {
		PreparedStatement pstmt = TaskCreatorRunner.getPreparedStatement();

		try {
			Job job = jobService.get(task.getJobId());

			pstmt.setLong(1, job.getJobId());
			pstmt.setString(2, job.getJobName());
			pstmt.setString(3, job.getJobDesc());
			pstmt.setString(4, job.getJobBusinessGroup());
			pstmt.setLong(5, job.getDutyOfficer());
			pstmt.setLong(6, job.getJobLevel());
			pstmt.setTimestamp(7, new Timestamp(task.getSettingTime().getTime()));
			pstmt.setLong(8, task.getTaskStatus());
			pstmt.setDate(9, new java.sql.Date(task.getTaskDate().getTime()));
			pstmt.setDate(10, new java.sql.Date(task.getScanDate().getTime()));
			pstmt.setTimestamp(11, task.getTaskBeginTime() == null ? null : new Timestamp(task.getTaskBeginTime().getTime()));
			pstmt.setTimestamp(12, task.getTaskEndTime() == null ? null : new Timestamp(task.getTaskEndTime().getTime()));
			pstmt.setLong(13, task.getFlag());
			pstmt.setInt(14, task.getFlag2());
			pstmt.setTimestamp(15, task.getReadyTime() == null ? null : new Timestamp(task.getReadyTime().getTime()));
			pstmt.setLong(16, task.getAlert());
			pstmt.setInt(17, task.getCycleType());
			pstmt.setInt(18, task.getRunTimes());
			pstmt.setTimestamp(19, new Timestamp(System.currentTimeMillis()));
			pstmt.setLong(20, task.getJobType());
			pstmt.setString(21, task.getPreTasks());
			pstmt.setString(22, task.getGateway());
			pstmt.setObject(23, task.getFailureRerunTimes());
			pstmt.setObject(24, task.getFailureRerunInterval());
			pstmt.setObject(25, task.getReferRunTime());

			pstmt.addBatch(); //  addBatch()把若干sql语句装载到一起，然后一次送到数据库执行，执行需要很短的时间。 还没到执行这一步
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 计算作业的参考运行时行(该时长取了上一天的运行时长，不过只是一个大概值，不是很精确的)
	 * 
	 * @return
	 */
	private void calculateReferRunTime() {
		yesterdayRunTimes = new HashMap<String, Long>();

		Collection<Task> tasks = taskService.getTasksByTaskDate(DateUtil.getYesterday(taskDate.getTime()));
		for (Task task : tasks) {
			Long runTime = task.getRunTime();

			if (runTime == null) {
				continue;
			}

			String key = task.getJobId() + "_" + DateUtil.format(task.getSettingTime(), "HHmm");
			yesterdayRunTimes.put(key, runTime);
		}
	}

	/**
	 * 根据作业周期创建任务
	 * 
	 * @return
	 */
	protected abstract void createTasks();
}