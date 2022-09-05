package com.sw.bi.scheduler.background.taskexcuter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.springframework.util.StringUtils;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.model.AlertSystemConfig;
import com.sw.bi.scheduler.model.JobDatasyncConfig;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.service.AlertSystemConfigService;
import com.sw.bi.scheduler.service.JobDatasyncConfigService;
import com.sw.bi.scheduler.util.Configure;
import com.sw.bi.scheduler.util.Configure.JobCycle;
import com.sw.bi.scheduler.util.DateUtil;
import com.sw.bi.scheduler.util.ExecAgent.ExecResult;
import com.sw.bi.scheduler.util.MailUtil;
import com.sw.bi.scheduler.util.SshUtil;

/**
 * 将本地文件以textfile格式put到HDFS的执行器
 * 
 * @author shiming.hong
 */
public class PutHdfsExcuter extends AbExcuter {
	private File ftptmpPath = new File("/home/tools/temp/ftptemp");

	private String gateway;
	private int cycleType;
	private JobDatasyncConfig config;

	private String localPath;
	private File localWorkPath;
	private String hdfsPath;

	private String fileUniquePattern;
	private String[] dateTimePositions;
	private Date settingTime;
	private Integer fileNumber;
	private File[] localFiles;
	private int localFileCount = 0;

	private Integer year;
	private Integer month;
	private Integer date;
	private Integer hour;
	private Integer minute;

	private boolean appendDate = false;
	private String hiveTableName;
	private String tmpHiveTableName;
	private String hiveFields;
	private String createTableSql;

	public PutHdfsExcuter(Task task, String logFolder) {
		super(task, logFolder);
	}

	@Override
	public boolean excuteCommand() throws Exception {
		gateway = currentAction.getGateway();

		JobDatasyncConfigService jobDatasyncConfigService = BeanFactory.getService(JobDatasyncConfigService.class);

		Long jobId = currentTask.getJobId();
		cycleType = currentTask.getCycleType();
		config = jobDatasyncConfigService.getJobDatasyncConfigByJob(jobId);

		localPath = config.getLinuxTmpDir();
		hdfsPath = config.getHdfsPath();
		fileNumber = config.getFileNumber();

		hiveTableName = config.getHiveTableName();
		hiveFields = config.getHiveFields();
		createTableSql = config.getCreateTableSql();// 对这张表的格式是有要求的. 要求双分区hour=  min= 并且是textfile格式  实际文本数据中的分隔符与建表语句中的分隔符要一致

		if (!StringUtils.hasText(hdfsPath)) {
			appendDate = true;
			hdfsPath = Configure.property(Configure.HIVE_DATABASE_PATH); // "/group/user/tools/meta/hive-temp-table/tools.db";
		}

		// 替换动态参数
		localPath = this.replaceParams(localPath);
		hdfsPath = this.replaceParams(hdfsPath);

		// 初始化日期匹配模式

		this.initDateTimePositions(config.getDateTimePosition());

		// 检查FTP临时目录是否存在
		if (!ftptmpPath.exists()) {
			ftptmpPath.mkdirs();
		}

		// 检查本地目录是否存在
		localWorkPath = new File(localPath);
		if (!localWorkPath.exists()) {
			log("本地文件(" + localPath + ")不存在.");
			return false;
		}

		// 文件唯一性匹配规则
		fileUniquePattern = config.getFileUniquePattern();

		// 检查HDFS目录是否存在

		if (!hdfsPath.endsWith("/")) {
			hdfsPath += "/";
		}

		// 获得本地目录下所有文件

		this.getLocalFiles();

		// 校验本地文件数量
		// 本地文件数量不需要校验了，发现一种特殊情况指定了需要4个文件，但实际因某些原因只会有3个，但这3个又是需要的
		// 如果加了校验则作业始终报错。如不加校验虽然最终还是会失败，但这3个文件会被PUT到HDFS上了

		/*if (!this.validateLocalFileNumber(year, month, date, hour, minute)) {
			return false;
		}*/

		// 初始化追加字段

		if (!this.initAppendDate()) {
			return false;
		}

		// 开始上传文件到HDFS

		if (!this.put2hdfs()) {
			return false;
		}

		if (appendDate) {
			// 将临时表中的数据加上文件名中时间后导出到正式表
			boolean success = this.exportTempTable(tmpHiveTableName, hiveTableName, hdfsPath);
			log("在" + hiveTableName + "追加时间字段" + (success ? "成功" : "失败"));
		}

		return this.referenceHdfs2Table();
	}

	/**
	 * 获得本地目录下所有文件
	 */
	private void getLocalFiles() {
		localFiles = localWorkPath.listFiles(new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				if (pathname.isDirectory()) {
					return false;
				}

				// 如果没定义文件唯一性规则，则获得该文件
				if (!StringUtils.hasText(fileUniquePattern)) {
					return true;
				}

				return pathname.getName().matches(fileUniquePattern);
			}

		});
		localFileCount = localFiles == null ? 0 : localFiles.length;

		if (fileNumber == null) {
			fileNumber = localFileCount;
		}
	}

	/**
	 * 将本地文件上传至HDFS
	 * 
	 * @return
	 * @throws IOException
	 */
	private boolean put2hdfs() throws IOException {
		if (!this.validateHdfsPath()) {
			return false;
		}

		ExecResult execResult = null;

		for (File file : localFiles) {
			String fileName = file.getName();
			String localFileName = localPath + "/" + fileName;

			if (!FtpUtil.isSame(fileName, year, month, date, hour, minute, dateTimePositions)) {
				continue;
			}

			// 开始上传文件
			execResult = SshUtil.execHadoopCommand(gateway, "put " + localFileName + " " + hdfsPath);
			log("本地文件(" + localFileName + ")上传至HDFS目录(" + hdfsPath + ")" + (execResult.success() ? "成功" : "失败") + ".");

			if (execResult.failure()) {
				log("错误信息: " + execResult.getStderr());

				return false;
			}
		}

		// 校验HDFS目录下是否上传了计划文件数量

		return this.validateHdfsFileNumber(year, month, date, hour, minute);
	}

	/**
	 * 初始化日期匹配模式
	 * 
	 * @param dateTimePosition
	 */
	private void initDateTimePositions(String dateTimePosition) {
		dateTimePositions = null;
		if (StringUtils.hasText(dateTimePosition)) {
			// 数据库job_datasync_config的date_time_position字段中保存的形式是  0,4|5,7|8,10|11,13|14,16
			dateTimePositions = dateTimePosition.split("\\|");
		}

		settingTime = currentTask.getSettingTime();
		if (dateTimePositions != null && dateTimePositions.length > 0) {
			Calendar calendar = DateUtil.getCalendar(settingTime);

			// 除分钟作业必须取当前分钟点外其他周期的作业都统一取上一个时间点
			if (cycleType == JobCycle.HOUR.indexOf()) {
				calendar.add(Calendar.HOUR_OF_DAY, -1);
			} else if (cycleType == JobCycle.DAY.indexOf()) {
				calendar.add(Calendar.DATE, -1);
			} else if (cycleType == JobCycle.WEEK.indexOf()) {
				calendar.add(Calendar.DATE, -7);
			} else if (cycleType == JobCycle.MONTH.indexOf()) {
				calendar.add(Calendar.MONTH, -1);
			}

			for (int i = 0, len = dateTimePositions.length; i < len; i++) {
				String position = dateTimePositions[i];

				if (!StringUtils.hasText(position)) {
					continue;
				}

				if (i == 0) {
					year = calendar.get(Calendar.YEAR);
				} else if (i == 1) {
					month = calendar.get(Calendar.MONTH) + 1;
				} else if (i == 2) {
					date = calendar.get(Calendar.DATE);
				} else if (i == 3) {
					hour = calendar.get(Calendar.HOUR_OF_DAY);
				} else if (i == 4) {
					minute = calendar.get(Calendar.MINUTE);
				}
			}
		}
	}

	private boolean initAppendDate() {
		// 如果不需要追加日期字段则不需要进入下面的初始化动作
		if (!appendDate) {
			return true;
		}

		boolean success = true;
		ExecResult execResult = null;

		if (!createTableSql.endsWith(";")) {
			createTableSql += ";";
		}

		if (createTableSql.indexOf("if not exists") == -1) {
			int pos = createTableSql.indexOf("(");
			createTableSql = "create table if not exists " + hiveTableName + createTableSql.substring(pos);
		}

		String dateTime = DateUtil.format(settingTime, "yyyyMMddHHmm");
		tmpHiveTableName = hiveTableName + "_tmp1"; // 数据导入的临时表

		// 创建临时表用于将ftp文件导入其中
		success = this.createTempTable(tmpHiveTableName);
		if (success) {
			// 需要确保hdfs目录只被组织一次
			if (hdfsPath.indexOf(tmpHiveTableName) == -1) {
				int pos = dateTime.length() - 2;
				hdfsPath += "/" + tmpHiveTableName + "/hour=" + dateTime.substring(0, pos) + "/min=" + dateTime.substring(pos) + "/";

				// 判断临时表的HDFS路径是否已经存在,如果已经存在需要先删除才能成功put文件
				/*execResult = SshUtil.execHadoopCommand(gateway, "ls " + hdfsPath);
				if (execResult.success()) {
					execResult = SshUtil.execHadoopCommand(gateway, "rm -r " + hdfsPath);
					log("HDFS目录(" + hdfsPath + ")删除" + (execResult.success() ? "成功" : "失败") + ".");
					if (execResult.failure()) {
						return false;
					}
				}*/
			}
		}

		// 检查HDFS目录是否存在(这里hdfsPath可能已经被更新了,所以需要重新校验是否存在)
		/*execResult = SshUtil.execHadoopCommand(gateway, "ls " + hdfsPath);
		if (execResult.failure()) {
			execResult = SshUtil.execHadoopCommand(gateway, "mkdir " + hdfsPath);
			log("HDFS目录(" + hdfsPath + ")创建" + (execResult.success() ? "成功" : "失败") + ".");
			if (execResult.failure()) {
				return false;
			}
		}*/

		return success;
	}

	/**
	 * 需要追加时间字段
	 * 
	 * @return
	 * @throws IOException
	 */
	@Deprecated
	private boolean requireAppendDate(String fileName) throws IOException {
		boolean success = true;

		if (!createTableSql.endsWith(";")) {
			createTableSql += ";";
		}

		if (createTableSql.indexOf("if not exists") == -1) {
			int pos = createTableSql.indexOf("(");
			createTableSql = "create table if not exists " + hiveTableName + createTableSql.substring(pos);
		}

		String dateTime = DateUtil.format(settingTime, "yyyyMMddHHmm");
		String tmpHiveTableName = hiveTableName + "_tmp1"; // 数据导入的临时表

		// 创建临时表用于将ftp文件导入其中
		success = this.createTempTable(tmpHiveTableName);
		if (success) {
			// 需要确保hdfs目录只被组织一次
			if (hdfsPath.indexOf(tmpHiveTableName) == -1) {
				int pos = dateTime.length() - 2;
				hdfsPath += "/" + tmpHiveTableName + "/hour=" + dateTime.substring(0, pos) + "/min=" + dateTime.substring(pos);
			}

		} else {
			return success;
		}

		// success = FtpUtil.download2Hdfs(gateway, ftp, fileName, remotePath, remoteBakPath, localPath, localBakPath, localErrPath, hdfsPath, settingTime, logFileWriter);

		if (success) {
			if (this.validateHdfsFileNumber(year, month, date, hour, minute)) {
				// 将临时表中的数据加上文件名中时间后导出到正式表
				success = this.exportTempTable(tmpHiveTableName, hiveTableName, hdfsPath);
			} else {
				log("在" + hiveTableName + "追加时间字段失败.");
				return false;
			}
		}

		log("在" + hiveTableName + "追加时间字段" + (success ? "成功" : "失败"));

		return success;
	}

	/**
	 * 创建临时表,用于将FTP上的文件内容导入该表
	 * 
	 * @parama tmpHiveTableName
	 * @return
	 */
	private boolean createTempTable(String tmpHiveTableName) {
		StringBuffer tmpCreateTableSql = new StringBuffer();
		tmpCreateTableSql.append("use ").append(Configure.property(Configure.HIVE_DATABASE)).append(";\r");

		int pos = createTableSql.indexOf("(") + 1;
		tmpCreateTableSql.append(createTableSql.substring(0, pos).replaceFirst(hiveTableName, tmpHiveTableName));

		pos = createTableSql.indexOf(",") + 1;
		tmpCreateTableSql.append(createTableSql.substring(pos));

		log("建表语句: " + tmpCreateTableSql);

		String tmpHiveSqlFile = ftptmpPath.getPath() + "/create-temp-table-" + DateUtil.format(settingTime, "yyyyMMddHHmm") + ".sql";
		log("建表的临时SQL文件(" + tmpHiveSqlFile + ")");
		File hiveSql = new File(tmpHiveSqlFile);
		BufferedWriter writer = null;

		boolean success = true;

		try {
			if (!hiveSql.exists()) {
				hiveSql.createNewFile();
			}

			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(hiveSql), "utf-8"));
			writer.write(tmpCreateTableSql.toString());
			writer.flush();

			// 执行SQL
			ExecResult execResult = SshUtil.execCommand(gateway, Configure.property(Configure.HIVE_HOME) + "hiveclient -f " + tmpHiveSqlFile); // this.programeRun(tmpHiveSqlFile);
			success = execResult.success();
			log("执行HiveSQL文件(" + tmpHiveSqlFile + ")" + (success ? "成功" : "失败") + ".");
			if (!success) {
				log("执行HiveSQL文件(" + tmpHiveSqlFile + ")失败信息: " + execResult.getStderr());
			}

		} catch (Exception e) {
			e.printStackTrace();
			success = false;

		} finally {
			try {
				if (writer != null) {
					writer.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (hiveSql.exists()) {
				hiveSql.delete();
			}
		}

		return success;
	}

	/**
	 * 导出临时表数据
	 * 
	 * @param sourceHiveTableName
	 * @param targetHiveTableName
	 * @return
	 */
	private boolean exportTempTable(String sourceHiveTableName, String targetHiveTableName, String hdfsPath) {
		boolean success = true;

		String dateTime = DateUtil.format(settingTime, "yyyyMMddHHmmss");
		String hour = DateUtil.format(settingTime, "yyyyMMddHH");
		String minute = DateUtil.format(settingTime, "mm");
		String partition = "(hour='" + hour + "',min='" + minute + "')";

		StringBuffer exportSql = new StringBuffer();
		exportSql.append("use ").append(Configure.property(Configure.HIVE_DATABASE)).append(";\r");

		exportSql.append("alter table ").append(sourceHiveTableName);
		exportSql.append(" add if not exists partition ").append(partition);
		exportSql.append(" location '").append(hdfsPath).append("/'");
		exportSql.append(";").append("\r");

		exportSql.append(createTableSql).append("\r");

		exportSql.append("set hive.exec.compress.output=true;\r");
		exportSql.append("set mapred.output.compression.codec=org.apache.hadoop.io.compress.BZip2Codec;\r");

		exportSql.append("insert overwrite table ").append(targetHiveTableName);
		exportSql.append(" partition ").append(partition);
		exportSql.append(" select '").append(dateTime).append("' ").append(hiveFields);
		exportSql.append(" from ").append(sourceHiveTableName);
		exportSql.append(" where hour='").append(hour).append("'");
		exportSql.append(" and min='").append(minute).append("'");
		exportSql.append(";").append("\r");

		exportSql.append("alter table ").append(sourceHiveTableName);
		exportSql.append(" drop if exists partition (hour='").append(hour).append("',min='").append(minute).append("')");
		exportSql.append(";");

		log("导出SQL: " + exportSql);

		String tmpExportHiveSqlFile = ftptmpPath.getPath() + "/export-temp-table-" + hour + minute + ".sql";
		log("导出的临时SQL文件(" + tmpExportHiveSqlFile + ")");
		File exportHiveSql = new File(tmpExportHiveSqlFile);
		BufferedWriter writer = null;

		try {
			if (!exportHiveSql.exists()) {
				exportHiveSql.createNewFile();
			}

			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(exportHiveSql), "utf-8"));
			writer.write(exportSql.toString());
			writer.flush();

			// 执行SQL
			ExecResult execResult = SshUtil.execCommand(gateway, Configure.property(Configure.HIVE_HOME) + "hiveclient -f " + tmpExportHiveSqlFile);
			success = execResult.success();
			log("导出表(" + sourceHiveTableName + ")中的数据至表(" + targetHiveTableName + ")" + (success ? "成功" : "失败") + ".");
			if (!success) {
				log("导出表(" + sourceHiveTableName + ")中的数据至表(" + targetHiveTableName + ")失败信息: " + execResult.getStderr());
			}

		} catch (Exception e) {
			success = false;

		} finally {
			try {
				if (writer != null) {
					writer.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return success;
	}

	/**
	 * 校验HDFS目录是否已经存在,如果存在则先删除
	 * 
	 * @return
	 */
	private boolean validateHdfsPath() {
		ExecResult execResult = SshUtil.execHadoopCommand(gateway, "ls " + hdfsPath);

		// 命令执行成功则表示HDFS目录已经存在
		if (execResult.success()) {
			AlertSystemConfigService alertAlertSystemConfigService = BeanFactory.getService(AlertSystemConfigService.class);
			AlertSystemConfig config = alertAlertSystemConfigService.get(1l);
			String mailList = config.getAlertMaillist();
			String content = currentTask + "中HDFS目录(" + hdfsPath + ")已经存在";

			try {
				if (hdfsPath.indexOf("/pt=") > -1 || hdfsPath.indexOf("_tmp1/hour=") > -1) {
					execResult = SshUtil.execHadoopCommand(gateway, "rm -r " + hdfsPath);

					if (execResult.success()) {
						content += ",系统已自动删除该目录.";
						MailUtil.send(mailList, "HDFS目录已经存在系统已自动删除", content);
						log(content);

					} else {
						String stderr = execResult.getStderr();

						if (stderr.indexOf("No such file or directory") == -1) {
							content += ",系统自动删除失败,需要人工处理.";
							MailUtil.send(mailList, "HDFS目录已经存在需要人工处理", content);
							log("HDFS目录(" + hdfsPath + ")删除失败.");
							log("错误信息: " + execResult.getStderr());

							return false;
						}
					}

				} else {
					content += ",需要人工删除该目录.";
					MailUtil.send(mailList, "HDFS目录已经存在需要人工处理", content);
					log(content);

					return false;
				}
			} catch (Exception e) {
				log("发往 \"" + mailList + "\" 的邮件失败.");
				return false;
			}
		}

		execResult = SshUtil.execHadoopCommand(gateway, "mkdir " + hdfsPath);
		log("HDFS目录(" + hdfsPath + ")创建" + (execResult.success() ? "成功" : "失败") + ".");
		if (execResult.failure()) {
			log("错误信息: " + execResult.getStderr());
			return false;
		}

		return true;
	}

	/**
	 * 检验本地文件数据
	 * 
	 * @param year
	 * @param month
	 * @param date
	 * @param hour
	 * @param minute
	 * @return
	 * @throws IOException
	 */
	private boolean validateLocalFileNumber(Integer year, Integer month, Integer date, Integer hour, Integer minute) throws IOException {
		log("开始从本地目录(" + localPath + ")中检验文件个数.");
		log("计划文件数量: " + fileNumber + ".");
		log("日期匹配模式: " + year + ", " + month + ", " + date + ", " + hour + ", " + minute + ".");

		if (localFileCount == 0) {
			log("检验本地文件数量失败,本地目录(" + localPath + ")中没有找到文件.");
			return false;
		}

		log("从本地目录(" + localPath + ")中获得" + localFileCount + "个文件.");

		if (year == null && month == null && date == null && hour == null && minute == null) {
			// 如果未设置文件名中的日期定位，则不需要对每个文件遍历匹配，只需要匹配本地文件个数与计划文件数量是否一致即可
			boolean matched = localFileCount == fileNumber.intValue();
			log("校验本地文件数量" + (matched ? "成功" : "失败"));
			return matched;

		} else {
			int sameCount = 0;
			for (int i = 0, len = localFiles.length; i < len; i++) {
				String fileName = localFiles[i].getName();

				if (FtpUtil.isSame(fileName, year, month, date, hour, minute, dateTimePositions)) {
					sameCount += 1;
					log(i + ". 文件(" + fileName + ")匹配成功.");
				} else {
					log(i + ". 文件(" + fileName + ")匹配失败.");
				}

				if ((sameCount == fileNumber.intValue())) {
					log("校验本地文件数量成功.");
					return true;
				}
			}

			log("校验本地文件数量失败.");
		}

		return false;
	}

	/**
	 * 校验HDFS文件数量
	 * 
	 * @return
	 * @throws IOException
	 */
	private boolean validateHdfsFileNumber(Integer year, Integer month, Integer date, Integer hour, Integer minute) throws IOException {
		if (fileNumber != null && fileNumber == -1) {
			log("忽略HDFS目录下文件数量的校验,因为文件数据设置了-1");
			return true;
		}

		log("开始从HDFS目录(" + hdfsPath + ")中检验文件个数.");
		log("计划文件数量: " + fileNumber + ".");
		log("日期匹配模式: " + year + ", " + month + ", " + date + ", " + hour + ", " + minute + ".");

		// 执行到这里理论上hdfsPath肯定应该是存在的,如果不存在得得看看上面为什么没有建成功
		ExecResult execResult = SshUtil.execHadoopCommand(gateway, "ls " + hdfsPath); // SshUtil.execHadoopCommand(gateway, "test -d " + hdfsPath);
		if (execResult.failure()) {
			log("校验HDFS文件数量失败,HDFS目录(" + hdfsPath + ")不存在.");
			return false;
		}

		execResult = SshUtil.execHadoopCommand(gateway, "ls " + hdfsPath + " | awk -F \" \" '{print $8}'");

		if (execResult.failure()) {
			log("检验HDFS文件数量失败," + execResult.getStderr());
			return false;
		}

		String[] files = null;
		if (!execResult.isEmptyStdout()) {
			files = execResult.getStdoutAsArrays();
		}
		int hdfsFileCount = files == null ? 0 : files.length - 1;

		if (hdfsFileCount == 0) {
			log("检验HDFS文件数量失败,HDFS目录(" + hdfsPath + ")中没有找到文件.");
			return false;
		}

		log("从HDFS目录(" + hdfsPath + ")中获得" + hdfsFileCount + "个文件.");

		if (year == null && month == null && date == null && hour == null && minute == null) {
			// 如果未设置文件名中的日期定位，则不需要对每个文件遍历匹配，只需要匹配HDFS文件个数与计划文件数量是否一致即可
			boolean matched = (hdfsFileCount == fileNumber.intValue());
			log("校验HDFS文件数量" + (matched ? "成功" : "失败"));
			return matched;

		} else {
			int sameCount = 0;
			for (int i = 1; i <= hdfsFileCount; i++) {
				String fileName = files[i].substring(files[i].lastIndexOf("/") + 1);

				if (FtpUtil.isSame(fileName, year, month, date, hour, minute, dateTimePositions)) {
					sameCount += 1;
					log(i + ". 文件(" + fileName + ")匹配成功.");
				} else {
					log(i + ". 文件(" + fileName + ")匹配失败.");
				}

				if ((sameCount >= fileNumber.intValue())) {
					log("校验HDFS文件数量成功.");
					return true;
				}
			}

			log("校验HDFS文件数量失败.");
		}

		return false;
	}

	private boolean referenceHdfs2Table() {
		String referTableName = config.getReferTableName();
		String referPartName = config.getReferPartName();

		if (StringUtils.hasText(referTableName) && StringUtils.hasText(referPartName)) {
			StringBuffer sql = new StringBuffer();
			sql.append("use ").append(config.getReferDbName()).append(";");
			sql.append("alter table ").append(this.replaceParams(referTableName));
			sql.append(" add if not exists partition(").append(this.replaceParams(referPartName)).append(")");
			sql.append(" location '").append(this.replaceParams(hdfsPath)).append("';");

			log("引用HDFS目录到指定表.");
			log(sql.toString());

			try {
				String[] commands = new String[] { "/bin/bash", "-c", "hiveclient -e \"" + sql.toString() + "\"" };
				Process process = Runtime.getRuntime().exec(commands);
				process.waitFor();

				boolean result = process.exitValue() == 0;
				if (!result) {
					log("exit value: " + process.exitValue());

					log("error stream: " + IOUtils.toString(process.getErrorStream(), "utf-8"));
					log("input stream: " + IOUtils.toString(process.getInputStream(), "utf-8"));
				}

				return result;

			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		return true;
	}
}
