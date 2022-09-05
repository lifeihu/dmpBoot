package com.sw.bi.scheduler.background.taskexcuter;

import java.io.File;
import java.io.FileFilter;
import java.util.Calendar;
import java.util.Date;

import org.springframework.util.StringUtils;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.model.Job;
import com.sw.bi.scheduler.model.JobDatasyncConfig;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.service.DatasourceService;
import com.sw.bi.scheduler.service.JobDatasyncConfigService;
import com.sw.bi.scheduler.service.JobService;
import com.sw.bi.scheduler.util.Configure.JobCycle;
import com.sw.bi.scheduler.util.DateUtil;

/**
 * 文件数量校验作业执行器
 * 
 * @author shiming.hong
 */
public class FileNumberCheckExcuter extends AbExcuter {

	private boolean result;
	private int jobType;

	private String successFlag;
	private boolean hasSuccessFlag;
	private boolean successFlagReady = false;

	private long checkMillis;
	private long timeoutMillis;

	private String localPath;
	private File localWorkPath;

	private String fileUniquePattern;
	private String[] dateTimePositions;
	private Date settingTime;
	private Integer fileNumber;

	private Integer year;
	private Integer month;
	private Integer date;
	private Integer hour;
	private Integer minute;

	private File[] localFiles;

	public FileNumberCheckExcuter() {}

	public FileNumberCheckExcuter(Task task, String logFolder) {
		super(task, logFolder);
	}

	@Override
	public boolean excuteCommand() throws Exception {
		try {
			JobService jobService = BeanFactory.getService(JobService.class);
			JobDatasyncConfigService jobDatasyncConfigService = BeanFactory.getService(JobDatasyncConfigService.class);
			DatasourceService datasourceService = BeanFactory.getService(DatasourceService.class);

			Long jobId = currentTask.getJobId();
			Job job = jobService.get(jobId);
			jobType = (int) job.getJobType();

			JobDatasyncConfig config = jobDatasyncConfigService.getJobDatasyncConfigByJob(jobId);

			successFlag = config.getSuccessFlag();
			hasSuccessFlag = StringUtils.hasText(successFlag);

			checkMillis = Long.parseLong(config.getCheckSeconds()) * 1000;
			timeoutMillis = config.getTimeoutMinutes() * 60 * 1000;

			fileNumber = config.getFileNumber();

			// 替换动态参数
			localPath = config.getLinuxTmpDir();
			localPath = this.replaceParams(localPath);

			// 创建本地工作目录
			localWorkPath = new File(localPath);
			if (!localWorkPath.exists()) {
				localWorkPath.mkdirs();
			}

			// 文件唯一性匹配规则
			fileUniquePattern = config.getFileUniquePattern();

			dateTimePositions = null;
			if (StringUtils.hasText(config.getDateTimePosition())) {
				// 数据库job_datasync_config的date_time_position字段中保存的形式是  0,4|5,7|8,10|11,13|14,16
				dateTimePositions = config.getDateTimePosition().split("\\|");
			}

			settingTime = currentTask.getSettingTime();
			int cycleType = currentTask.getCycleType();
			Calendar calendar = DateUtil.getCalendar(settingTime);

			// 除分钟作业必须取当前分钟点外其他周期的作业都统一取上一个时间点
			if (cycleType == JobCycle.HOUR.indexOf()) {
				calendar.add(Calendar.HOUR_OF_DAY, -1); // 前个小时
			} else if (cycleType == JobCycle.DAY.indexOf()) {
				calendar.add(Calendar.DATE, -1); // 前一天
			} else if (cycleType == JobCycle.WEEK.indexOf()) {
				calendar.add(Calendar.DATE, -7); // 上周
			} else if (cycleType == JobCycle.MONTH.indexOf()) {
				calendar.add(Calendar.MONTH, -1); // 上月
			}

			year = calendar.get(Calendar.YEAR);
			month = calendar.get(Calendar.MONTH) + 1;
			date = calendar.get(Calendar.DATE);
			hour = calendar.get(Calendar.HOUR_OF_DAY);
			minute = calendar.get(Calendar.MINUTE);

			long waitMillis = 0;
			int times = 1;
			do {
				log("第" + times + "次文件数量校验任务轮循开始...");

				// 根据文件唯一性规则获取相应文件
				localFiles = this.getLocalFilesByFileUniquePattern();

				log("工作目录(" + localPath + ")下获得" + localFiles.length + "个文件.");

				if (hasSuccessFlag) {
					result = successFlagReady;

					if (successFlagReady) {
						log("成功标记(" + successFlag + ")已经准备完毕.");
					} else {
						log("成功标记(" + successFlag + ")没有找到.");
					}

				} else {
					// 匹配到的文件个数
					int sameFileNumber = 0;

					for (File file : localFiles) {
						String fileName = file.getName();

						if (FtpUtil.isSame(fileName, year, month, date, hour, minute, dateTimePositions)) {
							sameFileNumber += 1;
							// log("文件(" + fileName + ")匹配成功.");
						} else {
							// log("文件(" + fileName + ")匹配失败.");
						}
					}

					log("实际匹配成功的文件数量: " + sameFileNumber + ", 预设成功数量: " + fileNumber);

					// 实际匹配数量与计划数量是否一致
					result = (sameFileNumber >= fileNumber);
				}

				if (result) {
					log("文件数量校验任务执行成功.");

					break;

				} else {
					if (waitMillis > timeoutMillis) {
						log("等待超时,文件数量校验任务执行终止.");

						break;

					} else {
						log("文件未准备完毕,请耐心等待...");

						Thread.sleep(checkMillis);
						waitMillis += checkMillis;
					}
				}

				times += 1;
			}
			while (true);

		} catch (Exception e) {
			e.printStackTrace();
			result = false;
		}

		return result;
	}

	/**
	 * 根据唯一文件匹配规则去获得本地文件
	 * 
	 * @return
	 * @throws Exception
	 */
	private File[] getLocalFilesByFileUniquePattern() throws Exception {
		File[] localFiles = localWorkPath.listFiles(new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				if (pathname.isDirectory()) {
					return false;
				}

				String fileName = pathname.getName();

				if (hasSuccessFlag && !successFlagReady && successFlag.equals(fileName)) {
					successFlagReady = true;
					return false;
				}

				// 如果没定义文件唯一性规则，则获得该文件
				if (!StringUtils.hasText(fileUniquePattern)) {
					return true;
				}

				return fileName.matches(fileUniquePattern);
			}

		});

		return localFiles;
	}

}
