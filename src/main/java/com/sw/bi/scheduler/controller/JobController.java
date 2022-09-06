package com.sw.bi.scheduler.controller;

import com.sw.bi.scheduler.background.exception.SchedulerException;
import com.sw.bi.scheduler.model.*;
import com.sw.bi.scheduler.service.*;
import com.sw.bi.scheduler.supports.MessageSenderAssistant;
import com.sw.bi.scheduler.util.Configure;
import com.sw.bi.scheduler.util.Configure.JobType;
import com.sw.bi.scheduler.util.OperateAction;
import org.apache.commons.beanutils.ConvertUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ConditionExpression;
import org.springframework.ui.ConditionModel;
import org.springframework.ui.Model;
import org.springframework.ui.PaginationSupport;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
public class JobController extends BaseActionController<Job> {

	@Autowired
	private JobService jobService;

	@Autowired
	private TaskService taskService;

	@Autowired
	private JobRelationService jobRelationService;

	@Autowired
	private JobDatasyncConfigService jobDatasyncConfigService;

	@Autowired
	private MailJobConfigService mailJobConfigService;

	@Autowired
	private ReportsQualityMonitorService reportsQualityMonitorService;
		
	
	private MessageSenderAssistant messageSender = new MessageSenderAssistant(new String[] { "scheduler.properties" });

	@PostMapping(value = "/manage/job")
	@ResponseBody
	public Map execute(Long id) {
		Map model = new HashMap();
		Job job = null;
		Collection<Job> parentJobs = new ArrayList<Job>();

		if (id != null) {
			job = jobService.get(id);
			parentJobs = jobRelationService.getAllParentJobs(id);
		}

		model.put("job", job);
		model.put("parentJobs", parentJobs);

		// 同步作业配置信息
		JobDatasyncConfig config = job == null ? null : jobDatasyncConfigService.getJobDatasyncConfigByJob(job.getJobId());
		model.put("jobDatasyncConfig", config);

		// 邮件发送配置
		MailJobConfig mailJobConfig = job == null ? null : mailJobConfigService.getByJobId(job.getJobId());
		model.put("mailJobConfig", mailJobConfig);

		// 报表质量监控配置
		ReportsQualityMonitor reportsQualityMonitorConfig = job == null ? null : reportsQualityMonitorService.getByJobId(job.getJobId());
		model.put("reportsQualityMonitorConfig", reportsQualityMonitorConfig);

		return model;
	}

	@Override
	@RequestMapping("/manage/job/save")
	public Job save(String[] childrenDataRoot) {
		Job job = decode("job", Job.class);

		// 保存后是否同时上线
		boolean saveOnline = Boolean.valueOf(request.getParameter("saveOnline"));
		System.out.println("saveOnline:"+saveOnline);

		// 上线的作业是否直接设置成成功
		boolean onlineSuccess = Boolean.valueOf(request.getParameter("onlineSuccess"));
		System.out.println("onlineSuccess:"+onlineSuccess);
		

		String parentJobs = request.getParameter("parentJobs");
		System.out.println("parentJobs:"+parentJobs);
		List<Long> parentJobIds = null;
		if (StringUtils.hasText(parentJobs) == true) {
			parentJobIds = new ArrayList<Long>();
			String[] tokens = parentJobs.split(",");
			for (String parentJobId : tokens) {
				parentJobIds.add(Long.valueOf(parentJobId));
			}
		}

		JobDatasyncConfig jobDatasyncConfig = null;
		MailJobConfig mailJobConfig = null;
		ReportsQualityMonitor reportsQualityMonitorConfig = null;
		switch (JobType.valueOf((int) job.getJobType())) {
			case HIVE_SQL:
			case SHELL:
			case MAPREDUCE:
			case STORE_PROCEDURE:
				break;
			case MAIL:
				mailJobConfig = decode("job", MailJobConfig.class);
				break;
			case REPORT_QUALITY:
				reportsQualityMonitorConfig = decode("job", ReportsQualityMonitor.class);
				break;
			default:
				jobDatasyncConfig = decode("job", JobDatasyncConfig.class);
		}

		jobService.isAuthorizedUserGroup(job, jobService.getEntityIdValue(job) == null ? OperateAction.CREATE : OperateAction.UPDATE);

		jobService.save(job, saveOnline, onlineSuccess, jobDatasyncConfig, mailJobConfig, reportsQualityMonitorConfig, parentJobIds);

		return job;
	}

	/**
	 * 作业上线
	 * 
	 * @param jobId
	 * @param updateBy
	 * @param allowCascadeOnline
	 *            是否允许级联上线
	 */
	@RequestMapping("/manage/job/online")
	public void online(long jobId, Long updateBy, @RequestParam(required = false)
	Boolean allowCascadeOnline) {
		jobService.isAuthorizedUserGroup(jobService.get(jobId), OperateAction.ONLINE);
		jobService.online(jobId, Boolean.TRUE.equals(allowCascadeOnline), updateBy);
		
		String job_notice_email = Configure.property(Configure.JOB_NOTICE_EMAIL);
		messageSender.sendMail(job_notice_email, "有新作业上线,请关注!", "作业ID: "+jobId);
	}

	/**
	 * 作业批量上线
	 * 
	 * @param jobId
	 */
	@RequestMapping("/manage/job/batchOnline")
	public void batchOnline(String jobId) {
		Long[] jobIds = (Long[]) ConvertUtils.convert(jobId.split(","), Long.class);

		// 校验登录用户对作业的操作权限
		for (Long id : jobIds) {
			jobService.isAuthorizedUserGroup(jobService.get(id), OperateAction.ONLINE);
		}

		for (Long id : jobIds) {
			this.online(id, 1l, false);
		}
	}

	/**
	 * 作业下线
	 * 
	 * @param jobIds
	 *            需要下线的作业IDs
	 * @param downMan
	 *            下线操作人
	 * @param downTime
	 *            下线时间
	 * @param downReason
	 *            下线原因
	 */
	@RequestMapping("/manage/job/offline")
	public void offline(String jobIds, Long downMan, Date downTime, String downReason) {
		Long[] jobId = (Long[]) ConvertUtils.convert(jobIds.split(","), Long.class);

		// 校验登录用户对作业的操作权限
		// 下线时不需要校验子任务的权限只要用户对主任务有权限即可
		jobService.isAuthorizedUserGroup(jobService.get(jobId[0]), OperateAction.OFFLINE);
		/*for (Long id : jobId) {
			jobService.isAuthorizedUserGroup(jobService.get(id), OperateAction.OFFLINE);
		}*/

		jobService.offline(jobId, downMan, downTime, downReason);
	}

	/**
	 * 获得指定作业所有子作业
	 * 
	 * @param jobId
	 * @param depth
	 * @return
	 */
	@RequestMapping("/manage/job/children")
	@ResponseBody
	public Collection<Job> getOnlineDepthChildrenJobsToCollection(long jobId, Integer depth) {
		return jobRelationService.getOnlineDepthChildrenJobsToCollection(jobId, depth);
	}

	/**
	 * 获得指定前置作业的后置作业
	 * 
	 * @param jobId
	 * @return
	 */
	@RequestMapping("/manage/job/getFrontJobs")
	@ResponseBody
	public Collection<Job> getOnlineFrontJobs(long rearJobId) {
		return jobService.getFrontJobs(rearJobId);
	}

	/**
	 * 获得指定前置作业的后置作业
	 * 
	 * @param rearJobId
	 * @return
	 */
	@RequestMapping("/manage/job/getRearJobs")
	@ResponseBody
	public Collection<Job> getOnlineRearJobs(long rearJobId) {
		return jobService.getRearJobs(rearJobId);
	}

	/**
	 * 判断指定的作业ID和任务日期是否允许生成任务
	 * 
	 * @param taskDate
	 * @param jobId
	 * @return
	 */
	@RequestMapping("/manage/job/allowCreateTasks")
	@ResponseBody
	public boolean allowCreateTasks(Date taskDate, String jobId) {
		Long[] jobIds = null;

		if (StringUtils.hasText(jobId)) {
			jobIds = (Long[]) ConvertUtils.convert(jobId.split(","), Long.class);
		}

		return jobService.isAlreadyCreateTasks(taskDate, jobIds);
	}

	/**
	 * 根据指定的任务和作业ID生成相应的任务
	 * 
	 * @param taskDate
	 * @param jobId
	 * @throws SchedulerException
	 */
	@RequestMapping("/manage/job/createTasks")
	@ResponseBody
	public PaginationSupport createTasks(Date taskDate, @RequestParam(required = false)
	String jobId) throws SchedulerException {
		// 模拟主网关机去创建任务
		Configure.property(Configure.MAIN_SCHEDULER, true);

		ConditionModel model = new ConditionModel(0, Integer.MAX_VALUE);
		model.addCondition("taskDateStart", ConditionExpression.EQ, taskDate);
		model.addCondition("taskDateEnd", ConditionExpression.EQ, taskDate);

		if (StringUtils.hasText(jobId)) {
			Long[] jobIds = (Long[]) ConvertUtils.convert(jobId.split(","), Long.class);
			Collection<Job> onlineJobs = jobService.getOnlineJobsByJobId(jobIds);

			if (onlineJobs.size() > 0) {
				Long[] onlineJobIds = new Long[onlineJobs.size()];
				StringBuffer ids = new StringBuffer();
				for (Job onlineJob : onlineJobs) {
					if (ids.length() > 0) {
						ids.append(",");
					}
					ids.append(onlineJob.getJobId());

					onlineJobIds[onlineJobIds.length - 1] = onlineJob.getJobId();
				}

				taskService.offline(onlineJobIds, taskDate);
				jobService.createTasks(onlineJobs, taskDate);

				model.addCondition("jobId", ConditionExpression.IN, ids.toString());
			}

		} else {
			// 因为按任务日期生成的任务比较多，前台只需要得到一个总数，所以分页中只取了一条
			model.setLimit(1);

			taskService.offline(taskDate);
			jobService.createTasks(taskDate);
		}

		return taskService.pagingBySql(model);
	}

	/**
	 * 获得上线作业中已配置的所有前置作业
	 * 
	 * @return
	 */
	@RequestMapping("/manage/job/getOnlinePrevJobIds")
	@ResponseBody
	public Collection<Long> getOnlinePrevJobIds() {
		return jobService.getOnlineFrontJobIds();
	}

	/**
	 * 获得所有引用了指定数据源的作业
	 * 
	 * @param dataSourceId
	 * @return
	 */
	@RequestMapping("/manage/job/getJobsByDataSource")
	@ResponseBody
	public Collection<Job> getJobsByDataSource(long dataSourceId) {
		return jobService.getJobsByDataSource(dataSourceId);
	}

	@Override
	public GenericService<Job> getDefaultService() {
		return jobService;
	}
	/**
	 * 获取JobId删除指定的作业
	 * 
	 * @param dataSourceId
	 * @return
	 */
	@RequestMapping("/manage/job/remove")
	@ResponseBody
	public void remove(String id) {
		  if (!StringUtils.hasText(id)) {
		    return;
		  }
		      Collection<Job> entities = new ArrayList<Job>();
		    for (String JobId : id.split(",")) {
		       Job entity = jobService.get(Long.valueOf(JobId));

		      if (entity instanceof AuthenticationUserGroup) {
		        getDefaultService().isAuthorizedUserGroup((AuthenticationUserGroup) entity, OperateAction.DELETE);
		      }

		      entities.add(entity);
		      //userGroupService.logicDelete(userGroupService.get(Long.valueOf(userGroupId)));
		      jobService.removeJobsFrom(entities);
		       System.out.println(id);
		      
		    }

		  }
}
