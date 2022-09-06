package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.background.exception.SchedulerException;
import com.sw.bi.scheduler.model.*;
import com.sw.bi.scheduler.service.*;
import com.sw.bi.scheduler.taskcreator.TaskCreatorRunner;
import com.sw.bi.scheduler.util.Configure;
import com.sw.bi.scheduler.util.Configure.*;
import com.sw.bi.scheduler.util.DateUtil;
import com.sw.bi.scheduler.util.OperateAction;
import com.sw.bi.scheduler.util.WriteUtil;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.ResultTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate5.HibernateCallback;
import org.springframework.resolver.Warning;
import org.springframework.security.core.userdetails.AuthenticationUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.ui.ConditionModel;
import org.springframework.ui.PaginationSupport;
import org.springframework.util.StringUtils;

import java.util.*;

@Service("jobService")
@SuppressWarnings("unchecked")
public class JobServiceImpl extends GenericServiceHibernateSupport<Job> implements JobService {
	
	protected static final Logger log = Logger.getLogger(JobServiceImpl.class);

	@Autowired
	private JobRelationService jobRelationService;

	@Autowired
	private JobDatasyncConfigService jobDatasyncConfigService;

	@Autowired
	private MailJobConfigService mailJobConfigService;

	@Autowired
	private ReportsQualityMonitorService reportsQualityMonitorService;

	@Autowired
	private TaskCreateLogService taskCreateLogService;

	@Autowired
	private TaskCreatorRunner taskCreatorRunner;

	@Autowired
	private WaitUpdateStatusTaskService waitUpdateStatusTaskService;

	@Autowired
	private TaskService taskService;

	@Autowired
	private ActionService actionService;

	@Autowired
	private GatewayService gatewayService;

	@Autowired
	private UserGroupRelationService userGroupRelationService;

	@Autowired
	private UserGroupService userGroupService;
		
	@Autowired
	private DatasourceService datasourceService;

	@Override
	/**
	 *   创建任务
	 *   规则： 比如taskCreateLog表中,当前已经创建的任务记录,日期最大是3号。  而当前日期是10号
	 *         那么就会从4号开始创建任务,一直创建到10号的任务为止。
	 * 
	 */
	public void createTasks(Date taskDate) throws SchedulerException {
		// 获得最近一次任务创建完毕的任务
		Date latestTaskDate = taskCreateLogService.getLatestCreateCompleteTaskDate();

		Date startTaskDate = null;
		Calendar calendar = null;

		if (latestTaskDate != null) {
			calendar = DateUtil.getCalendar(latestTaskDate, true);
			calendar.add(Calendar.DATE, 1); //以数据库中已经创建了任务的最大日期+1天作为开始日期,开始创建任务
			startTaskDate = calendar.getTime();

		} else {
			startTaskDate = taskDate; //如果从来没有创建过任务,一般是调度系统运营第一天,则从当天日期taskDate开始创建任务
			calendar = DateUtil.getCalendar(startTaskDate);
		}
		
		/*// add by zhuzhongji 2015年9月15日17:18:53  测试用（要测试的时候打开，好测试用）
		startTaskDate = taskDate;
		calendar = DateUtil.getCalendar(startTaskDate);*/
		
		

		long end = taskDate.getTime(); //创建任务一直创建到当前日期为止。 taskDate就是传入进来的当天的日期

		while (startTaskDate.getTime() <= end) {
			taskCreatorRunner.create(getOnlineJobs(), startTaskDate);

			// 添加根任务前删除所有相同任务日期的参考点
			waitUpdateStatusTaskService.remove(startTaskDate);

			// 将根任务加入参考点
			waitUpdateStatusTaskService.addRootTask(startTaskDate);

			// 标注指定任务日期内的所有任务已经创建完成
			taskCreateLogService.createComplete(startTaskDate);

			calendar.add(Calendar.DATE, 1);
			startTaskDate = calendar.getTime();
		}
	}

	@Override
	public void createTasks(Long[] jobIds, Date taskDate) {
		if (jobIds == null || jobIds.length == 0) {
			return;
		}

		this.createTasks(this.query(jobIds), taskDate);
	}

	@Override
	public void createTasks(Job job, Date taskDate, int taskFlag) {
		Collection<Job> jobs = new ArrayList<Job>();
		jobs.add(job);

		this.createTasks(jobs, taskDate, taskFlag);
	}

	@Override
	public void createTasks(Collection<Job> jobs, Date taskDate) {
		this.createTasks(jobs, taskDate, TaskFlag.SYSTEM.indexOf());
	}

	@Override
	public void createTasks(Collection<Job> jobs, Date taskDate, int taskFlag) {
		// 对作业清单中已经上线的作业进行任务生成操作
		taskCreatorRunner.create(jobs, taskDate, taskFlag);

		// 将指定作业生成的任务的父任务加入参考点表
		for (Job job : jobs) {
			waitUpdateStatusTaskService.addParentTasks(taskService.getTasksByJob(job.getJobId(), taskDate), DateUtil.getToday());
		}
	}

	@Override
	public List<Job> getOnlineJobs() {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("jobStatus", (long) JobStatus.ON_LINE.ordinal()));

		return criteria.list();
	}

	@Override
	public List<Job> getOnlineJobsByCycleType(long cycleType) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("jobStatus", (long) JobStatus.ON_LINE.ordinal()));
		criteria.add(Restrictions.eq("cycleType", cycleType));

		return criteria.list();

	}

	@Override
	public Collection<Job> getOnlineJobsByJobType(Long[] jobTypes) {
		if (jobTypes == null || jobTypes.length == 0) {
			return new ArrayList<Job>();
		}

		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("jobStatus", (long) JobStatus.ON_LINE.ordinal()));
		criteria.add(Restrictions.in("jobType", jobTypes));

		return criteria.list();
	}

	@Override
	public Collection<Job> getOnlineJobsByJobId(Long[] jobIds) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("jobStatus", (long) JobStatus.ON_LINE.ordinal()));
		criteria.add(Restrictions.in("jobId", jobIds));

		return criteria.list();
	}

	@Override
	public void save(Job job,
                     boolean saveOnline,
                     boolean onlineSuccess,
                     JobDatasyncConfig jobDatasyncConfig,
                     MailJobConfig mailJobConfig,
                     ReportsQualityMonitor reportsQualityMonitorConfig,
                     List<Long> parentJobIds) {
		if (job == null)
			return;

		// 校验前置作业
		String prevJobs = job.getPrevJobs();
		boolean isDependenceSelf = false;
		if (StringUtils.hasText(prevJobs)) {
			int cycleType = (int) job.getCycleType();
			String newPrevJobs = "";
			String[] prevJobIds = prevJobs.split(",");
			Collection<Job> predecessorJobs = new ArrayList<Job>();
			for (String prevJobId : prevJobIds) {
				if ("${self_job}".equals(prevJobId)) {
					if (cycleType != JobCycle.HOUR.indexOf()) {
						throw new Warning("非小时类型的作业不允许设置自己作为前置作业");
					}

					isDependenceSelf = true;
					continue;
				}

				if (newPrevJobs.length() > 0) {
					newPrevJobs += ",";
				}
				newPrevJobs += prevJobId;

				predecessorJobs.add(this.get(Long.valueOf(prevJobId)));
			}

			// 校验前置作业
			this.validateFrontJobs(cycleType, predecessorJobs);

			// 将排除了${self_job}变量后的前置作业ID再重新回填
			// ${self_job}变量在该作业实际保存后会再次回填进去
			job.setPrevJobs(newPrevJobs);
		}

		// 校验待触发作业的主从作业
		if (job.getJobType() == JobType.BRANCH.indexOf()) {
			this.validateBranchJobs((Long[]) ConvertUtils.convert(job.getProgramPath().split(","), Long.class));
		}

		// 校验指定作业中配置的HDFS目录是否是该用户所拥有权限的目录
		this.validateUserGroupHdfsPath(jobDatasyncConfig);

		// 未修改前的作业
		Job oldJob = get(job.getJobId());

		if (oldJob != null) {
			getHibernateTemplate().evict(oldJob);

			// 从白名单中删除指定作业ID
			gatewayService.removeJobFromWhiteList(oldJob.getGateway(), oldJob.getJobId());
		}

		this.saveOrUpdate(job);

		// 如果有依赖自己则把自己的ID再加封回去
		if (isDependenceSelf) {
			prevJobs = job.getPrevJobs();
			if (prevJobs.length() > 0) {
				prevJobs += ",";
			}
			prevJobs += job.getJobId();
			job.setPrevJobs(prevJobs);
		}

		// 修改前的dayN为当天或当天的星期则需要删除当天的任务
		boolean removeTasks = false;
		// 修改后的dayN为当天或当天的星期则需要判断当天的任务是否已经生成，没有则生成新的任务
		boolean createTasks = false;
		if (job.getJobStatus() == JobStatus.ON_LINE.indexOf()) {
			int cycleType = (int) job.getCycleType();
			Long dayN = job.getDayN();
			Long oldDayN = oldJob.getDayN();
			Calendar today = DateUtil.getTodayCalendar();

			if (oldDayN != null && dayN != null && !oldDayN.equals(dayN)) {
				if (cycleType == JobCycle.MONTH.indexOf()) {
					int date = today.get(Calendar.DATE);
					if (oldJob.getDayN() == date) {
						removeTasks = true;
					}

					if (dayN == date) {
						createTasks = true;
					}

				} else if (cycleType == JobCycle.WEEK.indexOf()) {
					int week = today.get(Calendar.DAY_OF_WEEK);
					if (oldJob.getDayN() == week) {
						removeTasks = true;
					}

					if (dayN == week) {
						createTasks = true;
					}

				} else if (cycleType == JobCycle.MINUTE.indexOf()) {
					removeTasks = true;
					createTasks = true;
				}
			}
		}

		if (!removeTasks && !createTasks) {
			// 根据修改的作业更改相应任务的冗余字段
			taskService.updateByJob(job);

		} else {
			Date today = DateUtil.getToday();
			if (removeTasks) {
				taskService.offline(new Long[] { job.getJobId() }, today);
			}

			if (createTasks) {
				this.createTasks(job, today, TaskFlag.ONLINE.indexOf());
			}
		}

		// 将作业添加到指定网关机(如果该网关机已经开启了白名单)的白名单列表中
		gatewayService.addJobToWhiteList(job.getGateway(), job.getJobId());

		// 根据修改的作业更改相应任务执行情况(Action)的冗余字段
		actionService.updateByJob(job);

		this.processJobRelations(job, parentJobIds);

		if (jobDatasyncConfig != null) {
			jobDatasyncConfig.setJobId(job.getJobId());
			jobDatasyncConfig.setJobType(job.getJobType());
			
			// add by zhuzhongji 2015年9月15日16:06:48  考虑HBase全表扫描，简化前端填写的情况
			String sourceDatapath = jobDatasyncConfig.getSourceDatapath();
			if(sourceDatapath != null && "full_table_scan".equals(sourceDatapath)){
				sourceDatapath = "a;b;c;d;e;f;g;h;i;j;k;l;m;n;o;p;q;r;s;t;u;v;w;x;y;z;A;B;C;D;E;F;G;H;I;J;K;L;M;N;O;P;Q;R;S;T;U;V;W;X;Y;Z;0;1;2;3;4;5;6;7;8;9";
				jobDatasyncConfig.setSourceDatapath(sourceDatapath);
			}
			
			//add by zhuzhongji 2015年9月17日16:35:58
			Long jobType = jobDatasyncConfig.getJobType();
			//if("38,57,67,77,87,118,128,137,148".contains(jobType+"")){	//暂时默认写死
			if(Configure.JobType.FTP_FILE_TO_HBASE.indexOf() == jobType || Configure.JobType.HDFS_TO_HBASE.indexOf() == jobType ||
					Configure.JobType.LOCAL_FILE_TO_HBASE.indexOf() == jobType || Configure.JobType.MYSQL_TO_HBASE.indexOf() == jobType ||
					Configure.JobType.SQLSERVER_TO_HBASE.indexOf() == jobType || Configure.JobType.ORACLE_TO_HBASE.indexOf() == jobType ||
					Configure.JobType.GP_TO_HBASE.indexOf() == jobType || Configure.JobType.CSV_TO_HBASE.indexOf() == jobType ||
					Configure.JobType.HBASE_TO_HBASE.indexOf() == jobType){	//暂时默认写死

				jobDatasyncConfig.setTargetDatapath("hbase_default_datapath");
			}

			//add by mashifneg 2018年12月1日16:35:58
			if(Configure.JobType.GP_TO_HDFS.indexOf() == jobType){
				Datasource data = jobDatasyncConfig.getDatasourceBySourceDatasourceId();
				data.getDatasourceId();
				System.out.println("datasourceid:"+data.getDatasourceId());
			    Datasource newData = datasourceService.queryById(data.getDatasourceId());
			    System.out.println("databaseName:"+newData.getDatabaseName());
			    System.out.println("databaseName:"+newData.getConnectionString());
			    System.out.println("databaseName:"+newData.getName());
			    System.out.println("databaseName:"+newData.getPassword());
			    System.out.println("databaseName:"+newData.getUsername());
			    System.out.println("databaseName:"+newData.getIp());
			    System.out.println("databaseName:"+newData.getPort());
				WriteUtil.DataxWriteLinux(jobDatasyncConfig,job,newData);
			}
			
			jobDatasyncConfigService.saveOrUpdate(jobDatasyncConfig);
		}

		if (mailJobConfig != null) {
			mailJobConfig.setJobId(job.getJobId());
			mailJobConfigService.saveOrUpdate(mailJobConfig);
		}

		if (reportsQualityMonitorConfig != null) {
			reportsQualityMonitorConfig.setJobId(job.getJobId());
			reportsQualityMonitorService.saveOrUpdate(reportsQualityMonitorConfig);
		}

		if (saveOnline && job.getJobStatus() != JobStatus.ON_LINE.indexOf()) {
			this.online(job.getJobId(), true);
		}

		/**
		 * <pre>
		 * 如果选择了上线并成功则需要将上线作业生成的所有任务
		 * 中预设时间小于当前时间的任务默认设置成功。这个需求
		 * 主要用于小时/分钟作业刚上线时会将已经过的了时间点
		 * 都跑起来的问题，如果使用了该功能小时/分钟作业上线
		 * 后已经过了的时间点就默认设置为成功
		 * </pre>
		 */
		if (saveOnline && onlineSuccess) {
			Collection<Task> tasks = taskService.getTasksByJob(job.getJobId(), DateUtil.getToday());
			long now = System.currentTimeMillis();
			for (Task task : tasks) {
				if (now > task.getSettingTime().getTime()) {
					task.setTaskStatus(TaskStatus.RUN_SUCCESS.indexOf());
					task.setTaskBeginTime(new Date());
					task.setTaskEndTime(new Date());
					task.setRunTime(0l);
					taskService.update(task);
				}
			}
		}

		// 当月、周、天和小时作业被修改了启动时间后需要对历史任务的SettionTime作相应修改
		if (oldJob != null && !oldJob.getJobTime().equals(job.getJobTime())) {
			taskService.updateSettingTime(job);
		}
	}
	
	@Override
	public void saveOrUpdate(Job job) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("jobName", job.getJobName()));
		criteria.add(Restrictions.not(Restrictions.eq("jobId", job.getJobId())));
		Job j = (Job) criteria.uniqueResult();

		if (j != null) {
			throw new Warning("作业(" + job.getJobName() + ")已经存在!");
		}

		job.setUpdateTime(DateUtil.now());
		super.saveOrUpdate(job);
	}

	@Override
	public void online(long jobId, boolean allowCascadeOnline) {
		this.online(jobId, allowCascadeOnline, null);
	}

	@Override
	public void online(long jobId, boolean allowCascadeOnline, Long updateBy) {
		Collection<Job> parentJobs = jobRelationService.getAllParentJobs(jobId);
		if (allowCascadeOnline) {
			for (Job parent : parentJobs) {
				if (parent.getJobStatus() != JobStatus.ON_LINE.indexOf()) {
					// 如果有未上线的父作业则自动将其上线
					this.online(parent.getJobId(), allowCascadeOnline, updateBy);
				}
			}

		} else {
			// 校验父作业是否存在未上线的
			Collection<String> notOnlineJobs = new ArrayList<String>();
			for (Job parent : parentJobs) {
				if (parent.getJobStatus() != JobStatus.ON_LINE.indexOf()) {
					notOnlineJobs.add(parent.getJobName() + "-" + parent.getJobId());
				}
			}
			if (notOnlineJobs.size() > 0) {
				String message = "指定的作业有 " + notOnlineJobs.size() + " 个未上线的父作业" + notOnlineJobs.toString().replace("[", "(").replace("]", ").");
				throw new Warning(message);
			}
		}

		//add begin by lifeng, 2013-09-02 月作业和周作业上线的时候,默认生成上个月的任务记录和上周的任务记录,以便后续补数据
		Job job = get(jobId);
		Date today = DateUtil.getToday();
		Calendar todayC = DateUtil.getCalendar(today);
		if (JobCycle.MONTH.indexOf() == job.getCycleType()) {
			if (todayC.get(Calendar.DAY_OF_MONTH) != job.getDayN().intValue()) {
				todayC.set(Calendar.MONTH, todayC.get(Calendar.MONTH) - 1); //月份减1
				todayC.set(Calendar.DAY_OF_MONTH, job.getDayN().intValue()); //月几跟数据库中的一样
			}
		} else if (JobCycle.WEEK.indexOf() == job.getCycleType()) {
			if (todayC.get(Calendar.DAY_OF_WEEK) != job.getDayN().intValue()) {
				todayC.set(Calendar.DAY_OF_WEEK, job.getDayN().intValue()); //周几跟数据库中的一样
				todayC.add(Calendar.DATE, -7); //上周,向前推7天
			}
		}
		today = todayC.getTime();
		taskService.delete(taskService.getTasksByJob(jobId, today));
		//add end

		this.createTasks(job, today, TaskFlag.ONLINE.indexOf());

		// 修改需要上线作业的相关信息
		job.setDownMan(null);
		job.setDownReason(null);
		job.setDownTime(null);
		job.setJobStatus(JobStatus.ON_LINE.indexOf());
		job.setUpdateTime(DateUtil.now());
		if (updateBy != null) {
			job.setUpdateBy(updateBy);
		}
		update(job, OperateAction.ONLINE);
	}

	/**
	 * 作业下线
	 */
	@Override
	public void offline(Long[] jobIds, Long downMan, Date downTime, String downReason) {
		if (jobIds.length == 0) {
			return;
		}

		taskService.offline(jobIds, DateUtil.getToday()); //作业下线要删除对应的当天任务记录

		for (int i = 0; i < jobIds.length; i++) {
			Job job = get(jobIds[i]);

			job.setDownMan(downMan);
			job.setDownTime(downTime);
			job.setDownReason(downReason);
			job.setUpdateBy(downMan);
			job.setUpdateTime(downTime);

			job.setJobStatus(JobStatus.OFF_LINE.indexOf());
			update(job, OperateAction.OFFLINE);
		}
	}

	@Override
	public PaginationSupport paging(ConditionModel model, ResultTransformer resultTransformer) {
		Long jobId = model.getValue("jobId", Long.class);

		Boolean isChooseParentSearch = model.getValue("chooseParent", Boolean.class);
		model.removeCondition("chooseParent");
		if (isChooseParentSearch == Boolean.TRUE) {
			model.removeCondition("jobId");
		}

		// 获得指定用户组下被分配的所有用户ID
		Long userGroupId = model.getValue("userGroupId", Long.class);
		model.removeCondition("userGroupId");
		Collection<Long> userIds = null;
		if (userGroupId != null) {
			Collection<User> users = userGroupRelationService.getUsersByUserGroup(userGroupId, false);

			// 如果用户组下没有指定用户则就没必须继续查询了
			if (users.size() == 0) {
				return new PaginationSupport(model.getStart(), model.getStart());
			}

			userIds = new ArrayList<Long>();
			for (User user : users) {
				userIds.add(user.getUserId());
			}
		}

		Criteria criteria = createCriteria(model);
//		criteria.addOrder(Order.desc("jobId"));

		if (userIds != null) {
			// 当选择了用户组时需要按用户组下的用户过滤作业
			criteria.add(Restrictions.in("dutyOfficer", userIds));
		}

		if (isChooseParentSearch == Boolean.TRUE && jobId != null) {
			Collection<Long> excludeIds = new ArrayList<Long>();

			// 排除自身
			excludeIds.add(jobId);

			// 排除子作业
			Collection<Job> children = jobRelationService.getOnlineDepthChildrenJobsToCollection(jobId, null);
			for (Job child : children) {
				excludeIds.add(child.getJobId());
			}

			// 搜索时排除自身及其所有子作业
			criteria.add(Restrictions.not(Restrictions.in("jobId", excludeIds)));
		}

		return super.paging(criteria, model.getStart(), model.getLimit(), resultTransformer);
	}

	@Override
	public boolean isAlreadyCreateTasks(Date taskDate, Long[] jobIds) {
		if (jobIds != null && jobIds.length > 0) {
			Collection<Job> onlineJobs = this.getOnlineJobsByJobId(jobIds);

			// 如果有未上线任务则直接抛出异常
			if (jobIds.length != onlineJobs.size()) {
				Map<Long, Boolean> onlineMapping = new HashMap<Long, Boolean>();
				for (Job onlineJob : onlineJobs) {
					onlineMapping.put(onlineJob.getJobId(), true);
				}

				StringBuffer unlineJobIds = new StringBuffer();
				for (Long id : jobIds) {
					if (onlineMapping.get(id) == null) {
						if (unlineJobIds.length() > 0) {
							unlineJobIds.append(",");
						}
						unlineJobIds.append(id);
					}
				}

				throw new Warning("作业(" + unlineJobIds.toString() + ")未上线或作业不存在,不能创建任务.");
			}
		}

		Criteria criteria = taskService.createCriteria();
		criteria.add(Restrictions.eq("taskDate", taskDate));
		if (jobIds != null && jobIds.length > 0) {
			criteria.add(Restrictions.in("jobId", jobIds));
		}
		criteria.setProjection(Projections.rowCount());
		Integer count = (Integer) criteria.uniqueResult();

		return count != null && count.intValue() > 0;
	}

	@Override
	/**
	 *   对任务的前置作业进行验证。
	 *   cycleType是当前作业的周期
	 *   prevJobs是当前作业的前置作业
	 *   规则：
	 *      作业的周期是天，则它的前置作业的周期也必须是天
	 *      作业的周期是周，则它的前置作业的周期也必须是周
	 *      作业的周期是月，则它的前置作业的周期也必须是月
	 *      作业的周期是小时，则它的前置作业的周期也必须是小时且也只能是该作业自己
	 *      目前只支持周期类型是天，周，月，小时的作业，可以设置前置作业
	 * 
	 */
	public boolean validateFrontJobs(int cycleType, Collection<Job> prevJobs) throws Warning {
		if (JobCycle.DAY.indexOf() == cycleType) {
			for (Job prevJob : prevJobs) {
				if (cycleType != prevJob.getCycleType()) {
					throw new Warning("天类型的作业只允许设置天类型的前置作业");
				}
			}

		} else if (JobCycle.WEEK.indexOf() == cycleType) {
			for (Job prevJob : prevJobs) {
				if (cycleType != prevJob.getCycleType()) {
					throw new Warning("周类型的作业只允许设置周类型的前置作业");
				}
			}

		} else if (JobCycle.MONTH.indexOf() == cycleType) {
			for (Job prevJob : prevJobs) {
				if (cycleType != prevJob.getCycleType()) {
					throw new Warning("月类型的作业只允许设置月类型的前置作业");
				}
			}

		} else if (JobCycle.HOUR.indexOf() == cycleType) {
			for (Job prevJob : prevJobs) {
				if (cycleType != prevJob.getCycleType()) {
					throw new Warning("小时类型的作业只允许设置小时类型的前置作业");
				}
			}

		} else {
			throw new Warning("只允许给天、周、月、小时类型的作业设置前置作业.");
		}

		return true;
	}

	@Override
	public Collection<Long> getOnlineFrontJobIds() {
		Criteria criteria = this.createCriteria();
		criteria.add(Restrictions.isNotNull("prevJobs"));
		criteria.add(Restrictions.sqlRestriction("length(prev_jobs) > 0"));
		criteria.add(Restrictions.eq("jobStatus", (long) JobStatus.ON_LINE.indexOf()));
		Collection<Job> jobs = criteria.list();

		Collection<Long> frontJobIds = new ArrayList<Long>();
		for (Job job : jobs) {
			String[] frontJobs = job.getPrevJobs().split(",");
			for (String prevJobId : frontJobs) {
				frontJobIds.add(Long.parseLong(prevJobId.trim()));
			}
		}

		return frontJobIds;
	}

	@Override
	public Collection<Job> getFrontJobs(long jobId) {
		Job job = this.get(jobId);

		Long[] frontJobIds = job.getPrevJobIds();
		if (frontJobIds == null || frontJobIds.length == 0) {
			return new ArrayList<Job>();
		}

		return this.query(frontJobIds);
	}

	@Override
	public Collection<Job> getRearJobs(long frontJobId) {
		Criteria criteria = this.createCriteria();
		criteria.add(Restrictions.isNotNull("prevJobs"));
		criteria.add(Restrictions.sqlRestriction("length(prev_jobs) > 0"));
		Collection<Job> jobs = criteria.list();

		Collection<Job> rearJobs = new ArrayList<Job>();
		for (Job job : jobs) {
			Collection<Long> frontJobIds = Arrays.asList(job.getPrevJobIds());
			if (frontJobIds.contains(frontJobId)) {
				rearJobs.add(this.intervene(job));
			}
		}

		return rearJobs;
	}

	@Override
	public Collection<Job> getJobsByDataSource(final long dataSourceId) {
		return getHibernateTemplate().execute(new HibernateCallback<Collection<Job>>() {

			@Override
			public Collection<Job> doInHibernate(Session session) throws HibernateException {
				StringBuilder hql = new StringBuilder("select jc, j from Job j, JobDatasyncConfig jc");
				hql.append(" where j.jobId = jc.jobId");
				hql.append(" and ");
				hql.append("(jc.datasourceBySourceDatasourceId.datasourceId = ").append(dataSourceId);
				hql.append(" or jc.datasourceByTargetDatasourceId.datasourceId = ").append(dataSourceId);
				hql.append(" or jc.datasourceByInitDatasourceId.datasourceId = ").append(dataSourceId);
				hql.append(" or jc.datasourceByFinalyDatasourceId.datasourceId = ").append(dataSourceId);
				hql.append(" or jc.datasourceByFtpDatasourceId.datasourceId = ").append(dataSourceId);
				hql.append(")");

				Query query = session.createQuery(hql.toString());
				Collection<Object[]> results = query.list();
				Collection<Job> jobs = new LinkedHashSet<Job>();
				for (Object[] result : results) {
					JobDatasyncConfig config = (JobDatasyncConfig) result[0];
					Job job = (Job) result[1];

					StringBuilder dataSourceUse = new StringBuilder();

					if (config.getDatasourceByInitDatasourceId() != null) {
						dataSourceUse.append("初始");
					}

					if (config.getDatasourceBySourceDatasourceId() != null) {
						if (dataSourceUse.length() > 0) {
							dataSourceUse.append(",");
						}
						dataSourceUse.append("来源");
					}

					if (config.getDatasourceByTargetDatasourceId() != null) {
						if (dataSourceUse.length() > 0) {
							dataSourceUse.append(",");
						}
						dataSourceUse.append("目标");
					}

					if (config.getDatasourceByFinalyDatasourceId() != null) {
						if (dataSourceUse.length() > 0) {
							dataSourceUse.append(",");
						}
						dataSourceUse.append("清理");
					}

					if (config.getDatasourceByFtpDatasourceId() != null) {
						if (dataSourceUse.length() > 0) {
							dataSourceUse.append(",");
						}
						dataSourceUse.append("FTP");
					}

					job.setDataSourceUse(dataSourceUse.toString());
					jobs.add(job);
				}

				return jobs;
			}

		});
	}

	@Override
	public Job intervene(Job job) {
		job.setUserGroup(userGroupRelationService.getUserGroupByUser(job.getDutyOfficer()));

		return job;
	}

	/**
	 * 处理作业的父子关系
	 * 
	 * @param job
	 * @param parentJobIds
	 */
	private void processJobRelations(Job job, List<Long> parentJobIds) {
		if (parentJobIds == null)
			return;

		// 需要从作业关系中被删除的父作业
		Collection<Long> removeParentJobs = new ArrayList<Long>();

		// 需要添加至作业关系中的父作业
		Collection<Long> addParentJobs = new ArrayList<Long>();

		List<Job> parentJobsFromDb = jobRelationService.getAllParentJobs(job.getJobId());
		for (Iterator<Job> iter = parentJobsFromDb.iterator(); iter.hasNext();) {
			Job parent = iter.next();
			Long jobId = parent.getJobId();

			if (!parentJobIds.contains(jobId)) {
				// 数据库中的作业在新的父作业列表中不存在则需要被删除
				removeParentJobs.add(jobId);
			} else {
				// 数据库中的作业在新的父作业列表中存在则表示该关系未变更则无需要后续的处理
				parentJobIds.remove(jobId);
			}

			iter.remove();
		}

		// 通过上述操作后parentJobIds中存放的已是本次修改中新增的父作业了
		addParentJobs.addAll(parentJobIds);

		if (removeParentJobs.size() > 0) {
			jobRelationService.removeParentJobsFromJobRelations(job.getJobId(), removeParentJobs);
		}

		if (addParentJobs.size() > 0) {
			jobRelationService.createRelation(job.getJobId(), addParentJobs.toArray(new Long[addParentJobs.size()]));

			if (job.getJobStatus() == JobStatus.ON_LINE.indexOf()) {
				Date today = DateUtil.getToday();
				waitUpdateStatusTaskService.addParentTasks(taskService.getTasksByJob(job.getJobId(), today), today);
			}
		}
	}

	/**
	 * 校验指定的分支作业
	 * 
	 * @param jobIds
	 */
	private boolean validateBranchJobs(Long[] jobIds) {
		if (jobIds == null || jobIds.length <= 1) {
			throw new Warning("必需指定二个或二个以上的 \"主从作业\".");
		}

		// List<Job> jobs = this.query(jobIds);

		// for (int i = 0, len = jobs.size(); i < len; i++) {
		for (int i = 0, len = jobIds.length; i < len; i++) {
			Job job = this.get(jobIds[i]); // jobs.get(i);

			if (i == 0) {
				if (job.getJobType() != JobType.SHELL.indexOf() && job.getJobType() != JobType.MAPREDUCE.indexOf()) {
					throw new Warning("主作业必须是\"Shell作业\"或\"MapReduce作业\"");
				}
			}

			if (job.getJobStatus() != JobStatus.ON_LINE.indexOf()) {
				throw new Warning(job.toString() + " 未上线.");
			}

			if (job.getCycleType() != JobCycle.NONE.indexOf()) {
				throw new Warning(job.toString() + " 不是\"待触发\"周期,分支作业中的所有主从作业必须是\"待触发\"周期.");
			}
		}

		return true;
	}

	/**
	 * 校验指定作业中配置的HDFS目录是否是当前用户所拥有权限的目录
	 * 
	 * @param config
	 * @return
	 */
	public boolean validateUserGroupHdfsPath(JobDatasyncConfig config) {
		// 作业配置不需要用户作业配置表的作业默认就不需要校验HDFS目录了
		if (config == null) {
			return true;
		}

		AuthenticationUserDetails aud = this.getPrincipal();

		if (aud == null) {
			throw new Warning("用户可能未登录或登录超时");
		}

		long userGroupId = aud.getUserGroupId();
		UserGroup userGroup = userGroupService.get(userGroupId);

		// 总部用户组不需要对数据目录进行校验，它拥有对所有子公司数据目录的权限
		if (userGroup.isAdministrator()) {
			return true;
		}

		String userGroupHdfsPath = userGroup.getHdfsPath();

		if (!StringUtils.hasText(userGroupHdfsPath)) {
			throw new Warning("用户(" + aud.getRealname() + ")数据目录未知,请联系管理员");
		}

		if (StringUtils.hasText(config.getHdfsPath()) && !config.getHdfsPath().startsWith(userGroupHdfsPath)) {
			throw new Warning("用户(" + aud.getRealname() + ")无权操作HDFS目录(" + config.getHdfsPath() + ")");

		} else if (StringUtils.hasText(config.getSourceDatapath())) {
			int jobType = config.getJobType().intValue();
			if (JobType.HDFS_TO_CSV.indexOf() == jobType || JobType.HDFS_TO_GP.indexOf() == jobType || JobType.HDFS_TO_HDFS.indexOf() == jobType || JobType.HDFS_TO_LOCAL_FILE.indexOf() == jobType ||
					JobType.HDFS_TO_MYSQL.indexOf() == jobType || JobType.HDFS_TO_SQLSERVER.indexOf() == jobType) {

				// 来源为HDFS的DataX作业需要校验HDFS目录
				if (!config.getSourceDatapath().startsWith(userGroupHdfsPath)) {
					throw new Warning("用户(" + aud.getRealname() + ")无权操作来源HDFS文件目录(" + config.getSourceDatapath() + ")");
				}
			}

		} else if (StringUtils.hasText(config.getTargetDatapath())) {
			int jobType = config.getJobType().intValue();
			if (JobType.CSV_TO_HDFS.indexOf() == jobType || JobType.GP_TO_HDFS.indexOf() == jobType || JobType.HDFS_TO_HDFS.indexOf() == jobType || JobType.LOCAL_FILE_TO_HDFS.indexOf() == jobType ||
					JobType.MYSQL_TO_HDFS.indexOf() == jobType || JobType.SQLSERVER_TO_HDFS.indexOf() == jobType) {

				// 目标为HDFS的DataX作业需要校验HDFS目录
				if (!config.getTargetDatapath().startsWith(userGroupHdfsPath)) {
					throw new Warning("用户(" + aud.getRealname() + ")无权操作目标HDFS文件目录(" + config.getTargetDatapath() + ")");
				}
			}

		}

		// DataX目标选择了HDFS并填写了关联数据库则需要根据用户组校验权限
		if (StringUtils.hasText(config.getReferDbName()) && !config.getReferDbName().trim().equalsIgnoreCase(userGroup.getHiveDatabase())) {
			throw new Warning("用户(" + aud.getRealname() + ")无权操作关联数据库(" + config.getReferDbName() + ")");
		}

		return true;
	}
	@Override
	  public void removeJobsFrom(Collection<Job> entities) {
	    //效验数据库jobStatus状态值是否为0，2(jobStatus:0: 未上线; 1: 已上线; 2: 已下线;)
	    for (Job entity : entities) {
	      Criteria criteria = createCriteria();
	      criteria.add(Restrictions.eq("jobId", entity.getJobId()));
	      Job j = (Job) criteria.uniqueResult();
	      long jobstatus = j.getJobStatus();
	      System.out.println("jobstatus："+jobstatus);
	      if(j.getJobStatus()==1){
	        throw new Warning(j.toString() + " 未下线.");
	      }else{
	        this.delete(entity);  
	      }  
	    }
	  }
}
