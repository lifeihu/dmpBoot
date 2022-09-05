package com.sw.bi.scheduler.background.javatype.reply;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.service.TaskService;
import com.sw.bi.scheduler.util.DateUtil;
import com.sw.bi.scheduler.util.ExecAgent.ExecResult;
import com.sw.bi.scheduler.util.SshUtil;

/**
 * 任务补数据
 * 
 * @author shiming.hong
 */

//  /home/tools/scheduler/scheduler jar   /home/tools/scheduler/scheduler.jar com.sw.bi.scheduler.background.javatype.reply.TaskSupply 223,224_22,23 /group/user/tools/temp/aa/*

@Component
public class TaskSupply {
	private static final Logger log = Logger.getLogger(TaskSupply.class);

	@Autowired
	private TaskService taskService;

	/**
	 * 是否串行补数据
	 */
	private boolean isSerialSupply;

	/**
	 * 网关
	 */
	private String gateway;

	private static TaskSupply getTaskSupply() {
		return BeanFactory.getBean(TaskSupply.class);
	}

	public void setSerialSupply(boolean isSerialSupply) {
		this.isSerialSupply = isSerialSupply;
	}

	public void setGateway(String gateway) {
		this.gateway = gateway;
	}

	/**
	 * 补数据
	 * 
	 * @param supplyTasks
	 * @param supplyDates
	 */
	public void taskSupply(Map<Long, Long[]> supplyTasks, Collection<Date> supplyDates) {
		Iterator<Date> iter = supplyDates.iterator();

		if (isSerialSupply) {
			Date startTaskDate = iter.next();
			Date endTaskDate = iter.next();

			this.taskSupply(supplyTasks, startTaskDate, endTaskDate);

		} else {
			for (; iter.hasNext();) {
				Date taskDate = iter.next();
				this.taskSupply(supplyTasks, taskDate, taskDate);
			}
		}

		log.info("成功对指定作业进行补数据操作.");
	}

	/**
	 * 补数据
	 * 
	 * @param supplyTasks
	 * @param startTaskDate
	 * @param endTaskDate
	 */
	private void taskSupply(Map<Long, Long[]> supplyTasks, Date startTaskDate, Date endTaskDate) {
		for (Entry<Long, Long[]> entry : supplyTasks.entrySet()) {
			long masterTaskId = entry.getKey();
			Long[] childrenTaskIds = entry.getValue();

			taskService.supply(masterTaskId, childrenTaskIds, startTaskDate, endTaskDate, isSerialSupply, true, 1l);
		}
	}

	/**
	 * 根据指定的作业ID获得需要补数据的相应任务
	 * 
	 * @param supplyJobIds
	 *            各批作业以“_”分隔，每批作业ID以“,”分隔，并且第一个代表主作业
	 * @param supplyDates
	 * @return
	 */
	private Map<Long, Long[]> getSupplyTasks(String supplyJobIds, Collection<Date> supplyDates) throws Exception {
		Map<Long, Long[]> results = new LinkedHashMap<Long, Long[]>();

		String[] tokens1 = supplyJobIds.split("_");
		for (String token1 : tokens1) {
			String[] jobIds = token1.split(",");
			long masterJobId = 0;
			Collection<Long> childrenJobIds = new ArrayList<Long>(jobIds.length - 1);
			for (int i = 0, len = jobIds.length; i < len; i++) {
				long jobId = Long.parseLong(jobIds[i].trim());

				if (i == 0) {
					masterJobId = jobId;
				} else {
					childrenJobIds.add(jobId);
				}
			}
			log.info("master: " + masterJobId + ", children: " + childrenJobIds);

			Date taskDate = DateUtil.getToday();
			Collection<Task> masterTasks = taskService.getTasksByJob(masterJobId, taskDate);

			if (masterTasks.size() == 0) {
				throw new Exception("主作业(作业ID: " + masterJobId + ", 任务日期: " + DateUtil.formatDate(taskDate) + ")没有对应的任务,请核查指定的作业ID");
			}

			for (Task masterTask : masterTasks) {
				Collection<Long> childrenTaskIds = null;
				if (childrenJobIds.size() > 0) {
					Collection<Task> childrenTasks = taskService.getTasksByJobs(childrenJobIds, taskDate);
					if (childrenTasks.size() > 0) {
						childrenTaskIds = new ArrayList<Long>();
						for (Task childrenTask : childrenTasks) {
							childrenTaskIds.add(childrenTask.getTaskId());
						}
					}
				}
				results.put(masterTask.getTaskId(), childrenTaskIds != null && childrenTaskIds.size() > 0 ? childrenTaskIds.toArray(new Long[] {}) : null);
			}
		}

		return results;
	}

	/**
	 * 从指定的hive表中获得需要补数据的日期 yyyy-MM-dd
	 * 
	 * @param tableName
	 * @return
	 */
	private Collection<Date> getSupplyDates(String tableName) {
		Collection<Date> results = new ArrayList<Date>();

		ExecResult execResult = SshUtil.execHadoopCommand(gateway, "cat " + tableName);

		if (execResult.failure()) {
			log.warn(execResult.getStderr());
			return results;
		}
		// log.info("hive table content: " + content);

		String[] supplyDates = execResult.getStdoutAsArrays();
		if (supplyDates == null) {
			return results;
		}

		for (int i = 0, len = supplyDates.length; i < len; i++) {
			// 如果是串行补则只需要则开始和结束日期(所以需要在数据组织时就要规定第一个是开始时间，最后一个是结束时间)
			if (isSerialSupply && i > 0 && i < len - 1) {
				continue;
			}

			try {
				log.info("解析需要补数据日期: " + supplyDates[i]);
				if (StringUtils.hasText(supplyDates[i])) {
					results.add(DateUtil.parseDate(supplyDates[i]));
				}
			} catch (Exception e) {
				continue;
			}
		}

		return results;
	}

	/**
	 * @param args
	 *            (jobId, tableName, depth, isSerialSupply, gateway)
	 */
	public static void main(String[] args) throws Exception {
		TaskSupply taskSupply = TaskSupply.getTaskSupply();

		// 需要补数据的作业ID
		String supplyJobIds = args[0];
		if (!StringUtils.hasText(supplyJobIds)) {
			throw new IllegalArgumentException("必须指定需要补数据操作的作业ID(如果有多批作业时以;分隔,每批作业中如有父子关系则以,分隔,且第一个代表主作业,后面都表示子作业,如：10,11,12;13;14,15)");
		}

		// Hive表
		String tableName = args[1];
		if (!StringUtils.hasText(tableName)) {
			throw new IllegalArgumentException("必须指定用于存放补数据日期的Hive表(需要HDFS的全路径)");
		}

		// 是否串行补数据
		boolean isSerialSupply = false;
		if (args.length >= 3) {
			isSerialSupply = "true".equals(args[2]);
		}
		taskSupply.setSerialSupply(isSerialSupply);

		// 指定hive表所在的服务器
		String gateway = SshUtil.DEFAULT_GATEWAY;
		if (args.length >= 4) {
			gateway = args[3];
		}
		taskSupply.setGateway(gateway);

		log.info("需要补数据的作业ID: " + supplyJobIds);
		log.info("Hive表: " + tableName);
		log.info("是否串行补数据: " + isSerialSupply);
		log.info("网关: " + gateway);

		Collection<Date> supplyDates = taskSupply.getSupplyDates(tableName);
		Map<Long, Long[]> supplyTasks = taskSupply.getSupplyTasks(supplyJobIds, supplyDates);
		taskSupply.taskSupply(supplyTasks, supplyDates);
	}
}
