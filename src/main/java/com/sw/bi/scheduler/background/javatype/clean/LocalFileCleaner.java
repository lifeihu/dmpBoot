package com.sw.bi.scheduler.background.javatype.clean;

import java.io.File;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.model.AlertSystemConfig;
import com.sw.bi.scheduler.model.Job;
import com.sw.bi.scheduler.model.JobDatasyncConfig;
import com.sw.bi.scheduler.service.AlertSystemConfigService;
import com.sw.bi.scheduler.service.JobDatasyncConfigService;
import com.sw.bi.scheduler.service.JobService;
import com.sw.bi.scheduler.util.Configure.JobType;
import com.sw.bi.scheduler.util.DateUtil;
import com.sw.bi.scheduler.util.MailUtil;

/**
 * 清理FTP作业、文件数量校验作业中用到的超期文件
 * 
 * @author shiming.hong
 */
@Component
public class LocalFileCleaner {
	private static final Logger log = Logger.getLogger(LocalFileCleaner.class);

	@Autowired
	private JobService jobService;

	@Autowired
	private JobDatasyncConfigService jobDatasyncConfigService;

	@Autowired
	private AlertSystemConfigService alertSystemConfigService;

	@SuppressWarnings("unchecked")
	public void cleanup(Date taskDate, Date defaultRemoveDate, Map<String, Date> pathRemoveDateMapping) {
		// 计算需要删除文件的临界时间点
		/*Calendar calendar = DateUtil.getCalendar(taskDate);
		calendar.add(Calendar.DATE, days * -1);
		Date removeDate = DateUtil.clearTime(calendar.getTime());*/

		StringBuilder message = new StringBuilder();
		int totalSuccessCount = 0;
		int totalFailureCount = 0;

		Long[] jobTypes = new Long[] { (long) JobType.FTP_FILE_TO_HDFS.indexOf(), (long) JobType.FTP_FILE_TO_HDFS_FIVE_MINUTE.indexOf(), (long) JobType.FTP_FILE_TO_HDFS_YESTERDAY.indexOf(),
				(long) JobType.FILE_NUMBER_CHECK.indexOf() };
		Collection<Job> jobs = jobService.getOnlineJobsByJobType(jobTypes);
		for (Job job : jobs) {
			JobDatasyncConfig config = jobDatasyncConfigService.getJobDatasyncConfigByJob(job.getJobId());
			final String jobDesc = job.toString();
			String linuxPath = config.getLinuxTmpDir();

			int pos = linuxPath.indexOf("${date_");
			if (pos > -1) {
				linuxPath = linuxPath.substring(0, pos);
			}

			pos = linuxPath.indexOf("${hour_");
			if (pos > -1) {
				linuxPath = linuxPath.substring(0, pos);
			}
			// linuxPath = "F:/Repositories/Temp/scheduler/filecleaner";

			File parent = new File(linuxPath);
			if (parent.exists()) {
				Date removeDate = defaultRemoveDate;
				if (pathRemoveDateMapping.containsKey(linuxPath)) {
					removeDate = pathRemoveDateMapping.get(linuxPath);
				}

				// 遍历指定目录及子目录中所有文件修改时间早于删除临界时间的所有文件
				Collection<File> files = FileUtils.listFiles(parent, new AgeFileFilter(removeDate, true), new DirectoryFileFilter() {

					@Override
					public boolean accept(File file) {
						return file.isDirectory();
					}

				});

				message.append("<b>").append(linuxPath).append("(共找到 ").append(files.size()).append(" 个早于 \"").append(DateUtil.formatDate(removeDate)).append("\" 的文件)</b><br>");
				log.info("在Linux目录(" + linuxPath + ")中共找到 " + files.size() + " 个早于 \"" + DateUtil.formatDate(removeDate) + "\" 的文件");

				// 遍历删除文件

				int successCount = 0;
				int failureCount = 0;
				for (File file : files) {
					String filePath = file.getAbsolutePath();

					if (FileUtils.deleteQuietly(file)) {
						successCount += 1;
						message.append("　　").append(filePath).append(" - OK.<br>");

					} else {
						failureCount += 1;
						message.append("　　").append(filePath).append(" - <span style=\"color:red;\">Fail</span>.<br>");
						log.info("文件(" + file.getAbsolutePath() + ")删除失败.");
					}
				}
				message.append("删除文件, 成功 <span style=\"color:blue;\">").append(successCount).append("</span> 个, 失败 <span style=\"color:red;\">").append(failureCount).append("</span> 个.<br>");
				log.info("从Linux目录(" + linuxPath + ")中删除文件: 成功 " + successCount + " 个, 失败 " + failureCount + " 个.");

				totalSuccessCount += successCount;
				totalFailureCount += failureCount;

			} else {
				log.warn(jobDesc + "中Linux目录(" + linuxPath + ")不存在,该Linux目录被忽略.");
			}

		}

		if (totalSuccessCount > 0 || totalFailureCount > 0) {
			AlertSystemConfig config = alertSystemConfigService.get(1l);
			try {
				log.info("---------------------------------------------------------------------------------");
				MailUtil.send(config.getAlertMaillist(), "文件清理清单", message.toString());
			} catch (Exception e) {
				log.error("邮件发送失败.");
			}
		}

	}

	public static LocalFileCleaner getLocalFileCleaner() {
		return BeanFactory.getBean(LocalFileCleaner.class);
	}

	public static void main(String[] args) {
		if (args.length < 2) {
			throw new IllegalArgumentException("参数指定不正确.");
		}

		// 任务日期
		Date taskDate = DateUtil.parse(args[0], "yyyyMMdd");

		// 根据设置的超期天数计算出默认的删除临界日期
		int days = Integer.parseInt(args[1]);
		Calendar calendar = DateUtil.getCalendar(taskDate);
		calendar.add(Calendar.DATE, days * -1);
		Date defaultRemoveDate = calendar.getTime();

		// 解析指定目录的删除临界日期
		Map<String, Date> pathRemoveDateMapping = new HashMap<String, Date>();
		if (args.length >= 3) {

			for (int i = 2, len = args.length; i < len; i++) {
				String[] values = args[i].split(",");

				String path = values[0];
				if (path.endsWith("/")) {
					path = path.substring(0, path.length() - 1);
				}

				int pathDays = Integer.parseInt(values[1]);
				calendar = DateUtil.getCalendar(taskDate);
				calendar.add(Calendar.DATE, pathDays * -1);

				pathRemoveDateMapping.put(path, calendar.getTime());
			}
		}

		getLocalFileCleaner().cleanup(taskDate, defaultRemoveDate, pathRemoveDateMapping);
	}
}
