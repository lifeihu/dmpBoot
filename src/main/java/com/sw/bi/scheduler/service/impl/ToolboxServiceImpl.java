package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.background.taskexcuter.FtpUtil;
import com.sw.bi.scheduler.background.util.DxDESCipher;
import com.sw.bi.scheduler.model.Datasource;
import com.sw.bi.scheduler.model.Gateway;
import com.sw.bi.scheduler.model.JobDatasyncConfig;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.service.*;
import com.sw.bi.scheduler.util.Configure;
import com.sw.bi.scheduler.util.Configure.JobType;
import com.sw.bi.scheduler.util.Configure.TaskStatus;
import com.sw.bi.scheduler.util.DateUtil;
import com.sw.bi.scheduler.util.ExecAgent.ExecResult;
import com.sw.bi.scheduler.util.SshUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@SuppressWarnings("unchecked")
public class ToolboxServiceImpl implements ToolboxService {
	private static final Logger log = Logger.getLogger(ToolboxServiceImpl.class);

	private FileWriter logFile;

	@Autowired
	private GatewayService gatewayService;

	@Autowired
	private TaskService taskService;

	@Autowired
	private JobDatasyncConfigService jobDatasyncConfigService;

	@Autowired
	private DatasourceService datasourceService;

	@Override
	public String viewCreateTable(String dbName, String tableName) {
		if (!StringUtils.hasText(tableName)) {
			return "";
		}

		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;

		try {
			Class.forName(Configure.property(Configure.HIVE_DATABASE_CONNECTION_DRIVER_CLASS));

			conn = DriverManager.getConnection(Configure.property(Configure.HIVE_DATABASE_CONNECTION_URL), Configure.property(Configure.HIVE_DATABASE_CONNECTION_USERNAME), Configure
					.property(Configure.HIVE_DATABASE_CONNECTION_PASSWORD));
			stmt = conn.createStatement();

			// ???????????????ID
			String dbId = null;
			String sql = "select DB_ID from DBS where NAME = '" + dbName + "'";
			rs = stmt.executeQuery(sql);
			if (rs.next()) {
				dbId = rs.getString(1);
			}
			if (dbId == null) {
				// throw new Warning("?????????(" + dbName + ")?????????.");
				return "";
			}

			sql = "select TBL_ID,SD_ID from TBLS where TBL_NAME='" + tableName + "' and DB_ID=" + dbId;
			rs = stmt.executeQuery(sql);

			String tblId = "";
			String sdId = "";
			if (rs.next()) {
				tblId = rs.getString(1);
				sdId = rs.getString(2);
			} else {
				// throw new Warning("???(" + tableName + ")?????????.");
				return "";
			}

			sql = "select count(*) from TABLE_PARAMS where TBL_ID=" + tblId + " and PARAM_KEY='EXTERNAL' and PARAM_VALUE='TRUE';";
			boolean external_table = false;
			rs = stmt.executeQuery(sql);
			if (rs.next()) {
				int external = rs.getInt(1);
				if (external == 1) {
					external_table = true;
				}
			}
			String external_flag = "";
			if (external_table == true) {
				external_flag = "external";
			}

			sql = "select PKEY_NAME,PKEY_TYPE from PARTITION_KEYS where TBL_ID=" + tblId + " order by INTEGER_IDX asc;";
			rs = stmt.executeQuery(sql);
			//partitioned by (pt string,hashcodemod string)
			String partitioned = "partitioned by (";
			while (rs.next()) {
				String PKEY_NAME = rs.getString(1);
				String PKEY_TYPE = rs.getString(2);
				partitioned += PKEY_NAME + " " + PKEY_TYPE + ",";
			}
			partitioned = partitioned.substring(0, partitioned.length() - 1) + ")";

			//org.apache.hadoop.hive.ql.io.RCFileInputFormat   
			//org.apache.hadoop.mapred.TextInputFormat         
			//org.apache.hadoop.mapred.SequenceFileInputFormat 
			String INPUT_FORMAT = "stored as textfile";
			String LOCATION = "";
			String SERDE_ID = "";
			sql = "select INPUT_FORMAT,LOCATION,SERDE_ID from SDS where SD_ID=" + sdId + ";";
			rs = stmt.executeQuery(sql);
			if (rs.next()) {
				INPUT_FORMAT = rs.getString(1);
				LOCATION = rs.getString(2);
				SERDE_ID = rs.getString(3);
				if ("org.apache.hadoop.hive.ql.io.RCFileInputFormat".equals(INPUT_FORMAT)) {
					INPUT_FORMAT = "stored as rcfile";
				} else if ("org.apache.hadoop.mapred.SequenceFileInputFormat".equals(INPUT_FORMAT)) {
					INPUT_FORMAT = "stored as sequencefile";
				} else if ("org.apache.hadoop.mapred.TextInputFormat".equals(INPUT_FORMAT)) {
					INPUT_FORMAT = "stored as textfile";
				}
			}

			if (Configure.property(Configure.HIVE_0_7, Boolean.class)) {
				sql = "select COLUMN_NAME,TYPE_NAME from COLUMNS where SD_ID=" + sdId + " order by INTEGER_IDX asc;";

			} else if (Configure.property(Configure.HIVE_0_9, Boolean.class)
					|| Configure.property(Configure.HIVE_1_1, Boolean.class)) {
				sql = "select COLUMN_NAME,TYPE_NAME from COLUMNS_V2";
				sql += " left join SDS on SDS.CD_ID = COLUMNS_V2.CD_ID";
				sql += " where SDS.SD_ID = " + sdId;
				sql += " order by INTEGER_IDX asc";
			}
			rs = stmt.executeQuery(sql);
			String ziduan_desc = "";
			while (rs.next()) {
				String COLUMN_NAME = rs.getString(1);
				String TYPE_NAME = rs.getString(2);
				ziduan_desc += "   " + COLUMN_NAME + "   " + TYPE_NAME + "," + "\n";
			}
			ziduan_desc = ziduan_desc.substring(0, ziduan_desc.length() - 2);

			String split = null;
			sql = "select PARAM_VALUE from SERDE_PARAMS where SERDE_ID=" + SERDE_ID + " and PARAM_KEY='field.delim';";
			rs = stmt.executeQuery(sql);
			if (rs.next()) {
				split = rs.getString(1);
				if ("\"".equals(split)) {
					split = "\\\"";
				} else if ("\001".equals(split)) {
					split = "\\001";
				} else if ("\t".equals(split)) {
					split = "\\t";
				}
			}

			String createTable = "CREATE " + external_flag + " TABLE if not exists " + tableName + "(" + "\n";
			createTable += ziduan_desc + ")" + "\n";
			if (!"partitioned by )".equals(partitioned)) {
				createTable += partitioned + "\n";
			}
			if (split != null) {
				createTable += "row format delimited\n";
				createTable += "fields terminated by '" + split + "'\n";
				createTable += "lines terminated by '\\n'\n";
			}
			createTable += INPUT_FORMAT;
			if (external_table) {
				createTable += "\n" + "location '" + LOCATION + "'";
			}
			createTable += ";";

			StringBuffer result = new StringBuffer(createTable);
			result.append("\n").append("\n");
			result.append("data location: " + LOCATION).append("\n");

			return result.toString();

		} catch (ClassNotFoundException e) {
			e.printStackTrace();

		} catch (SQLException e) {
			e.printStackTrace();

		} finally {
			try {
				if (rs != null) {
					rs.close();
				}

				if (stmt != null) {
					stmt.close();
				}

				if (conn != null) {
					conn.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}

		}

		return "";
	}

	@Override
	public String viewSchedulerLog(String gateway, Date date, Integer tailNumber) {
		String schedulerLog = Configure.property(Configure.SCHEDULER_BACKGROUND_LOG_PATH) + DateUtil.format(date, "yyyyMMdd/HH") + ".txt";
		ExecResult execResult = null;

		if (tailNumber == null) {
			execResult = SshUtil.execCommand(gateway, "cat " + schedulerLog);
		} else {
			execResult = SshUtil.execCommand(gateway, "tail -" + tailNumber + " " + schedulerLog);
		}

		if (execResult.success()) {
			return execResult.isEmptyStdout() ? "????????????(" + schedulerLog + ")??????????????????." : execResult.getStdout();
		} else {
			return execResult.getStderr();
		}
	}

	@Override
	public String syncHundson(String project) {

		// ??????????????????????????????????????????????????????URL
		Authenticator.setDefault(new Authenticator() {

			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(Configure.property(Configure.HUDSON_SYNC_SOURCE_PATH_USERNAME), Configure.property(Configure.HUDSON_SYNC_SOURCE_PATH_PASSWORD).toCharArray());
			}

		});

		File tempDirectory = new File(Configure.property(Configure.HUDSON_SYNC_TEMP_PATH));
		if (!tempDirectory.exists()) {
			tempDirectory.mkdirs();
		}

		File latestVersion = null;
		boolean success = false;

		try {
			File file = new File(tempDirectory, project + "-" + DateUtil.format(new Date(), "yyyyMMddHHmm") + ".log");
			if (!file.exists()) {
				file.createNewFile();
				log.info(file + " created.");
			}
			logFile = new FileWriter(file, true);
			log.info("create log file writer.");

			// ???HUDSON???svn???????????????????????????????????????
			latestVersion = this.donwloadLatestVersion(project);
			if (latestVersion == null) {
				logFile.write("not found latest version."); //??????????????????hundson??????????????????IP?????????
				return null;
			}

			if (latestVersion.getName().endsWith(".zip")) {
				// ?????????????????????
				File unzipDirectory = this.unzip(latestVersion);
				if (unzipDirectory == null) {
					logFile.write("unzip failed.");
					return null;
				}

				// ??????????????????????????????Hadoop??????,???????????????????????????
				// success = this.copy2Target(project, unzipDirectory);
				if (this.publishHadoop(project, unzipDirectory)) {
					// ?????????????????????
					success = this.distributeGateways(project);

				} else {
					return null;
				}

				// ?????????????????????,??????????????????
				/*if (unzipDirectory.exists()) {
					unzipDirectory.delete();
				}*/

			} else if (latestVersion.getName().endsWith(".jar")) {
				success = this.publishHadoop(project, latestVersion); // this.copy2Target(project, latestVersion);

			}

			if (latestVersion.exists())
				latestVersion.delete();

		} catch (FileNotFoundException e) {
			success = false;
			log.error("create log file exception.", e);

		} catch (IOException e) {
			success = false;
			log.error("write log file exception.", e);

		} finally {
			try {
				if (logFile != null) {
					logFile.flush();
					logFile.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return success ? latestVersion.getName() : null;
	}

	@Override
	public String restoreBackupFile(Date taskDate, Long[] jobIds) throws Exception {
		StringBuffer result = new StringBuffer();

		// ?????????????????????????????????????????????FTP??????
		Criteria criteria = taskService.createCriteria();
		criteria.add(Restrictions.in("jobId", jobIds));
		criteria.add(Restrictions.eq("taskDate", taskDate));
		criteria.add(Restrictions.in("jobType", new Long[] { (long) JobType.FTP_FILE_TO_HDFS_FIVE_MINUTE.indexOf(), (long) JobType.FTP_FILE_TO_HDFS_YESTERDAY.indexOf() }));
		criteria.add(Restrictions.not(Restrictions.in("taskStatus", new Long[] { (long) TaskStatus.RUN_SUCCESS.indexOf(), (long) TaskStatus.RE_RUN_SUCCESS.indexOf() })));
		Collection<Task> tasks = criteria.list();

		if (tasks.size() == 0) {
			return DateUtil.formatDate(taskDate) + "?????????????????????????????????.";
		}

		Integer year = null;
		Integer month = null;
		Integer date = null;
		Integer hour = null;
		Integer minute = null;

		for (Task task : tasks) {
			// ??????FTP????????????
			JobDatasyncConfig config = jobDatasyncConfigService.getJobDatasyncConfigByJob(task.getJobId());
			// ??????????????????
			String remotePath = config.getFtpDir();
			// ??????????????????
			String remoteBakPath = config.getFtpBakDir();
			// ????????????
			Date settingTime = task.getSettingTime();
			// ????????????????????????
			Date yesterday = DateUtil.getYesterday(settingTime);

			// FTP?????????
			Datasource ftpDatasource = datasourceService.get(config.getDatasourceByFtpDatasourceId().getDatasourceId());
			String url = ftpDatasource.getIp();
			int port = Integer.parseInt(ftpDatasource.getPort());
			String username = ftpDatasource.getUsername();
			String password = DxDESCipher.DecryptDES(ftpDatasource.getPassword(), ftpDatasource.getUsername());

			// ??????FTP?????????
			StringWriter writer = new StringWriter();
			FTPClient ftp = null;
			try {
				ftp = FtpUtil.getFTPClient(url, port, username, password, remoteBakPath, writer);

			} catch (IOException e) {
				log.error("??????FTP???????????????.", e);

			} finally {
				log.info(writer.getBuffer().toString());
			}

			if (ftp == null) {
				log.error("??????FTP???????????????.");
				continue;
			}

			////////////////////////////////////////////////////////////

			FTPFile[] backupFiles = null;
			try {
				backupFiles = ftp.listFiles();
			} catch (IOException e) {
				log.error("??????FTP????????????.", e);
			}

			if (backupFiles == null || backupFiles.length == 0) {
				log.warn("????????????(" + config.getLinuxBakDir() + ")??????????????????.");
				continue;
			}

			////////////////////////////////////////////////////////////

			String[] dateTimePositions = null;
			if (StringUtils.hasText(config.getDateTimePosition())) {
				//?????????job_datasync_config???date_time_position???????????????????????????  0,4|5,7|8,10|11,13|14,16
				dateTimePositions = config.getDateTimePosition().split("\\|");
			}

			if (task.getJobType().intValue() == JobType.FTP_FILE_TO_HDFS_FIVE_MINUTE.indexOf()) {
				// ??????n???????????????????????????????????????
				Calendar calendar = DateUtil.getCalendar(settingTime);
				year = calendar.get(Calendar.YEAR);
				month = calendar.get(Calendar.MONTH) + 1;
				date = calendar.get(Calendar.DATE);
				hour = calendar.get(Calendar.HOUR_OF_DAY);
				minute = calendar.get(Calendar.MINUTE);

			} else {
				// ???????????????????????????????????????
				Calendar calendar = DateUtil.getCalendar(yesterday);
				year = calendar.get(Calendar.YEAR);
				month = calendar.get(Calendar.MONTH) + 1;
				date = calendar.get(Calendar.DATE);
			}

			for (FTPFile backupFile : backupFiles) {
				if (backupFile.isDirectory()) {
					continue;
				}

				String fileName = backupFile.getName();

				if (!FtpUtil.isSame(fileName, year, month, date, hour, minute, dateTimePositions)) {
					continue;
				}

				try {
					ftp.rename(fileName, remotePath + "/" + fileName);
					result.append("????????????(" + fileName + ")??????FTP??????(" + remotePath + ")??????.<br>");

				} catch (IOException e) {
					result.append("????????????(" + fileName + ")??????FTP??????(" + remotePath + ")??????.<br>");
				}
			}

			////////////////////////////////////////////////////////////

			try {
				// ??????FTP??????
				ftp.logout();

			} catch (IOException e) {
				log.error("??????FTP??????.", e);

			} finally {
				if (ftp.isConnected()) {
					try {
						ftp.disconnect();
					} catch (IOException e) {
						log.error("??????FTP????????????.", e);
					}
				}
			}
		}

		return result.toString();
	}

	/**
	 * ?????????????????????svn??????????????????
	 * 
	 * @param project
	 * @return
	 * @throws MalformedURLException
	 */
	private File donwloadLatestVersion(String project) {
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;

		try {
			String sourceUrl = null;
			if ("daijin".equals(project)) {
				sourceUrl = Configure.property(Configure.HUDSON_SYNC_DAIJIN_SOURCE_PATH);
			} else if ("ganrong".equals(project)) {
				sourceUrl = Configure.property(Configure.HUDSON_SYNC_GANGRONG_SOURCE_PATH);
			} else if ("mr".equals(project)) {
				sourceUrl = Configure.property(Configure.HUDSON_SYNC_MR_SOURCE_PATH);
			} else if ("py".equals(project)) {
				sourceUrl = Configure.property(Configure.HUDSON_SYNC_PY_SOURCE_PATH);
			} else if ("sw_udf".equals(project)) {
				sourceUrl = Configure.property(Configure.HUDSON_SYNC_UDF_SOURCE_PATH);
			} else {
				return null;
			}

			URL latestVersionUrl = getLatestVersionURL(sourceUrl);
			if (latestVersionUrl == null) {
				log.error("??????????????????????????????.");
				return null;
			}

			bis = new BufferedInputStream(latestVersionUrl.openStream());

			File tempDirectory = new File(Configure.property(Configure.HUDSON_SYNC_TEMP_PATH));
			if (!tempDirectory.exists()) {
				tempDirectory.mkdirs();
			}

			String fileName = latestVersionUrl.getFile();
			File file = new File(tempDirectory, fileName.substring(fileName.lastIndexOf("/") + 1));
			if (file.exists()) {
				file.delete();
			}
			file.createNewFile();

			bos = new BufferedOutputStream(new FileOutputStream(file));
			IOUtils.copy(bis, bos);
			bos.close();

			log.info("download " + file + " success.");

			return file;

		} catch (Exception e) {
			log.error("hudson sync execption.", e);

		} finally {

			try {
				if (bis != null) {
					bis.close();
				}

				if (bos != null) {
					bos.close();
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	/**
	 * ???????????????????????????URL?????? <dir name="20120524" href="20120524/" /> <dir
	 * name="20120525" href="20120525/" />
	 * 
	 * <file name="package-checksums.md5" href="package-checksums.md5" /> <file
	 * name="sw_udf-2345.jar" href="sw_udf-2345.jar" />
	 * 
	 * @param url
	 */
	private URL getLatestVersionURL(String url) {
		InputStreamReader isr = null;
		BufferedReader reader = null;
		try {
			isr = new InputStreamReader(new URL(url).openStream());
			reader = new BufferedReader(isr);
			String line = null;
			long maxPath = 0l;
			while ((line = reader.readLine()) != null) {
				if (line.indexOf("<dir ") > -1) {
					int pos = line.indexOf("\"") + 1;
					char[] chars = line.toCharArray();
					String path = "";
					for (int i = pos; i < chars.length; i++) {
						if (chars[i] == '"') {
							break;
						} else {
							path += chars[i];
						}
					}
					//?????????20120524  ?????????maxPath  ??????????????????20120525  ???????????????????????????maxPath
					maxPath = Math.max(maxPath, Long.parseLong(path));

				} else if (line.indexOf("<file ") > -1 && (line.indexOf(".zip") > -1 || line.indexOf(".jar") > -1)) {
					int pos = line.indexOf("\"") + 1;
					char[] chars = line.toCharArray();
					String path = "";
					for (int i = pos; i < chars.length; i++) {
						if (chars[i] == '"') {
							break;
						} else {
							path += chars[i];
						}
					}
					//???????????????path= sw_udf-2345.jar   ????????????new URL(url + "/" + path)
					//???????????????jar????????????url??????
					return new URL(url + "/" + path);
				}
			}

			return getLatestVersionURL(url + "/" + maxPath); //????????????

		} catch (IOException e) {
			e.printStackTrace();

		} finally {
			try {
				if (reader != null) {
					reader.close();
				}

				if (isr != null) {
					isr.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	/**
	 * ?????????????????????
	 * 
	 * @param file
	 */
	private File unzip(File file) {
		if (file == null) {
			return null;
		}
		// http://192.168.17.243/svn/bi/daijin/etl-daijin/20120525/20120525142129/daijin-2346-sql.zip
		// daijin-2346-sql
		String zipFileName = file.getName().substring(file.getName().lastIndexOf("/") + 1, file.getName().lastIndexOf("."));
		File targetDirectory = new File(file.getParentFile(), zipFileName);
		if (targetDirectory.exists()) {
			targetDirectory.delete();
		}
		targetDirectory.mkdirs(); //??????daijin-2346-sql??????

		FileInputStream fis = null;
		ZipInputStream zis = null;

		try {
			fis = new FileInputStream(file);
			zis = new ZipInputStream(fis);
			ZipEntry entry = null;

			while ((entry = zis.getNextEntry()) != null) {
				String entryName = entry.getName();

				//???????????????,????????????????????????
				if ("target/".equals(entryName) || "hudsonBuild.properties".equals(entryName)) {
					continue;
				}

				if (entry.isDirectory()) {
					File entryDirectory = new File(targetDirectory, entry.getName());
					if (!entryDirectory.exists()) {
						entryDirectory.mkdirs();
					}

					continue;
				}

				File entryFile = new File(targetDirectory, entry.getName());
				if (!entryFile.exists()) {
					entryFile.delete();
				}
				entryFile.createNewFile();

				FileOutputStream fos = new FileOutputStream(entryFile);

				IOUtils.copy(zis, fos);
				fos.close();
			}

			log.info("unzip to " + targetDirectory);

			return targetDirectory;

		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			try {
				if (zis != null) {
					zis.close();
				}

				if (fis != null) {
					fis.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	/**
	 * ??????????????????(??????????????????)?????????(jar??????)?????????Hadoop???????????????
	 * 
	 * @param project
	 * @param sourceFile
	 * @throws IOException
	 */
	private boolean publishHadoop(String project, File sourceFile) throws IOException {
		String gateway = SshUtil.DEFAULT_GATEWAY;
		boolean success = true;
		ExecResult execResult = null;

		if (sourceFile.isDirectory()) {
			String hadoopTempPath = Configure.property(Configure.HUDSON_SYNC_TEMP_HADOOP_PATH);
			execResult = SshUtil.execHadoopCommand(gateway, "test -d " + hadoopTempPath);
			if (execResult.failure()) {
				SshUtil.execHadoopCommand(gateway, "mkdir " + hadoopTempPath);
				logFile.write("path " + hadoopTempPath + " is not exists, will be maked.\n");
			}

			String hadoopTargetPath = hadoopTempPath + project;
			execResult = SshUtil.execHadoopCommand(gateway, "test -d " + hadoopTargetPath);
			if (execResult.success()) {
				logFile.write(hadoopTargetPath + " already exists, will be removed.\n");
				SshUtil.execHadoopCommand(gateway, "rm -r " + hadoopTargetPath);
			}

			execResult = SshUtil.execHadoopCommand(gateway, "put " + sourceFile.getPath() + " " + hadoopTargetPath);
			if (execResult.success()) {
				logFile.write("path " + sourceFile.getPath() + " put to " + hadoopTargetPath + " success.\n");
			} else {
				success = false;
				logFile.write("path " + sourceFile.getPath() + " put to " + hadoopTargetPath + " failure.\n");
				logFile.write("failure message: " + execResult.getStderr());
			}

		} else {
			// ???????????????????????????????????????????????????Hadoop?????????
			String fileName = sourceFile.getName();
			fileName = fileName.substring(0, fileName.lastIndexOf("-")) + ".jar";

			// ??????Hadoop???????????????????????????,???????????????
			String targetFile = Configure.property(Configure.HUDSON_SYNC_UDF_TARGET_PATH) + fileName;
			logFile.write("init udf hdfs target path: " + targetFile + ".\n");
			execResult = SshUtil.execHadoopCommand(gateway, "test -z " + targetFile);
			if (execResult.success()) {
				SshUtil.execHadoopCommand(gateway, "rm -r " + targetFile);
				logFile.write(targetFile + " already exists, will be removed.\n");
			}

			// ????????????????????????Hadoop??????
			execResult = SshUtil.execHadoopCommand(gateway, "put " + sourceFile.getPath() + " " + targetFile);
			if (execResult.success()) {
				logFile.write("file " + sourceFile.getPath() + " put to " + targetFile + " success.\n");
			} else {
				success = false;
				logFile.write("file " + sourceFile.getPath() + " put to " + targetFile + " failure.\n");
			}
		}

		return success;
	}

	/**
	 * ?????????????????????
	 * 
	 * @param project
	 * @return
	 */
	private boolean distributeGateways(String project) throws IOException {
		ExecResult execResult = null;
		String hadoopSourcePath = Configure.property(Configure.HUDSON_SYNC_TEMP_HADOOP_PATH) + project;

		// ??????Hadoop?????????????????????
		execResult = SshUtil.execHadoopCommand(SshUtil.DEFAULT_GATEWAY, "test -d " + hadoopSourcePath);
		if (execResult.failure()) {
			logFile.write(hadoopSourcePath + " not found.\n");
			return false;
		}

		// ???????????????????????????
		String targetPath = null;
		if ("daijin".equals(project)) {
			targetPath = Configure.property(Configure.HUDSON_SYNC_DAIJIN_TARGET_PATH);
		} else if ("ganrong".equals(project)) {
			targetPath = Configure.property(Configure.HUDSON_SYNC_GANGRONG_TARGET_PATH);
		} else if ("mr".equals(project)) {
			targetPath = Configure.property(Configure.HUDSON_SYNC_MR_TARGET_PATH);
		} else if ("py".equals(project)) {
			targetPath = Configure.property(Configure.HUDSON_SYNC_PY_TARGET_PATH);
		} else if ("sw_udf".equals(project)) {
			targetPath = Configure.property(Configure.HUDSON_SYNC_UDF_TARGET_PATH);
		} else {
			logFile.write("not found target directory from configure file.\n");
			return false;
		}

		// ????????????
		String tempPath = Configure.property(Configure.HUDSON_SYNC_TEMP_PATH) + project;

		// ?????????????????????(????????????????????????????????????)
		Collection<Gateway> gateways = gatewayService.queryAll();

		String distributeGateway = Configure.property(Configure.HUDSON_DISTRIBUTE_GATEWAY);
		if (StringUtils.hasText(distributeGateway)) {
			String[] distributeGateways = distributeGateway.split(",");
			for (String ip : distributeGateways) {
				Gateway gateway = new Gateway();
				gateway.setName(ip);
				gateway.setIp(ip);
				gateway.setPort(22);
				gateway.setUserName("tools");
				gateway.setPassword("tools");
				SshUtil.registerAgent(ip, ip, 22);
				gateways.add(gateway);
			}
		}

		for (Gateway gateway : gateways) {
			String gatewayName = gateway.getName();

			// ????????????????????????????????????
			execResult = SshUtil.execCommand(gatewayName, "rm -rf " + tempPath);
			if (execResult.success()) {
				logFile.write("in the " + gatewayName + " gateway " + tempPath + " already exists will be removed.\n");
			}

			// ???Hadoop??????????????????????????????????????????????????????
			execResult = SshUtil.execHadoopCommand(gatewayName, "get " + hadoopSourcePath + " " + tempPath);
			if (execResult.success()) {
				logFile.write("get " + hadoopSourcePath + " to " + tempPath + " success, from " + gatewayName + " gateway.\n");
			} else {
				logFile.write("get " + hadoopSourcePath + " to " + tempPath + " failure, from " + gatewayName + " gateway.\n");
				logFile.write("failur message: " + execResult.getStderr());
				return false;
			}

			// ????????????????????????????????????
			execResult = SshUtil.execCommand(gatewayName, "cp -r " + tempPath + "/* " + targetPath);
			if (execResult.success()) {
				logFile.write("from " + tempPath + " copy to " + targetPath + " success, from " + gatewayName + " gateway.\n");
			} else {
				logFile.write("from " + tempPath + " copy to " + targetPath + " failure, from " + gatewayName + " gateway.\n");
				logFile.write("failur message: " + execResult.getStderr());
				return false;
			}

		}

		// ???????????????????????????????????????Hadoop??????????????????
		SshUtil.execHadoopCommand(SshUtil.DEFAULT_GATEWAY, "rm -r " + hadoopSourcePath);
		logFile.write("remove " + hadoopSourcePath + " from hadoop.\n");

		return true;
	}

	/**
	 * ??????????????????????????????????????????
	 * 
	 * @param project
	 * @param sourceFile
	 */
	@Deprecated
	private boolean copy2Target(String project, File sourceFile) {

		try {
			String targetPath = null;
			if ("daijin".equals(project)) {
				targetPath = Configure.property(Configure.HUDSON_SYNC_DAIJIN_TARGET_PATH);
			} else if ("ganrong".equals(project)) {
				targetPath = Configure.property(Configure.HUDSON_SYNC_GANGRONG_TARGET_PATH);
			} else if ("mr".equals(project)) {
				targetPath = Configure.property(Configure.HUDSON_SYNC_MR_TARGET_PATH);
			} else if ("py".equals(project)) {
				targetPath = Configure.property(Configure.HUDSON_SYNC_PY_TARGET_PATH);
			} else if ("sw_udf".equals(project)) {
				targetPath = Configure.property(Configure.HUDSON_SYNC_UDF_TARGET_PATH);
			} else {
				logFile.write("not found target directory from configure.\n");
				return false;
			}

			if (sourceFile.isDirectory()) {
				File targetDirectory = new File(targetPath);
				if (!targetDirectory.exists()) {
					targetDirectory.mkdirs();
				}

				FileSystemUtils.copyRecursively(sourceFile, targetDirectory);
				logFile.write("copy " + sourceFile + " to " + targetDirectory + ".\n");

			} else {
				// ????????????HDFS???????????????(???????????????)
				String fileName = sourceFile.getName();
				fileName = fileName.substring(0, fileName.lastIndexOf("-")) + ".jar";
				File renameFile = new File(sourceFile.getParentFile(), fileName);
				if (!renameFile.exists()) {
					renameFile.createNewFile();
				}
				sourceFile.renameTo(renameFile);
				logFile.write(sourceFile + " rename to " + renameFile + ".\n");
				sourceFile = renameFile;

				Path localFile = new Path(renameFile.getPath());

				Path hdfsPath = new Path(targetPath);

				String cmd = "hadoop fs -test -e " + hdfsPath;
				Process process = programeRun(cmd);
				process.waitFor();
				if (process.exitValue() == 0) {
					this.logFile.write(hdfsPath + " exists" + "\r\n");
				} else {
					this.logFile.write(hdfsPath + " not exists" + "\r\n");
				}
				if (process.exitValue() != 0) {
					cmd = "hadoop fs -mkdir " + hdfsPath;
					process = programeRun(cmd);
					process.waitFor();
					if (process.exitValue() == 0) {
						logFile.write("make hdfs directory " + hdfsPath + "  success.\n");
					} else {
						logFile.write("make hdfs directory " + hdfsPath + "  failed.\n");
						return false;
					}
				}

				Path hdfsFile = new Path(targetPath + "/" + renameFile.getName());

				cmd = "hadoop fs -test -e " + hdfsFile;
				process = programeRun(cmd);
				process.waitFor();
				if (process.exitValue() == 0) {
					this.logFile.write(hdfsFile + " exists" + "\r\n");
				} else {
					this.logFile.write(hdfsFile + " not exists" + "\r\n");
				}

				if (process.exitValue() == 0) {
					logFile.write(hdfsFile + " already exists, will be removed.\n");
					cmd = "hadoop fs -rm -r " + hdfsFile;
					process = programeRun(cmd);
					process.waitFor();
					if (process.exitValue() == 0) {
						logFile.write("delete  udf " + hdfsFile + "  success.\n");
					} else {
						logFile.write("delete  udf " + hdfsFile + "  failed.\n");
						return false;
					}
				}

				String hundson_temp_path = Configure.property(Configure.HUDSON_SYNC_TEMP_PATH);
				cmd = "hadoop fs -put " + hundson_temp_path + "/" + fileName + " " + targetPath;
				process = programeRun(cmd);
				process.waitFor();
				if (process.exitValue() == 0) {
					logFile.write("copy " + localFile + " to " + hdfsPath + "  success.\n");
				} else {
					logFile.write("copy " + localFile + " to " + hdfsPath + "  failed.\n");
				}

			}
		} catch (IOException e) {

			try {
				logFile.write("copy " + sourceFile + " to target direcotry exception.\n");
				log.info("copy " + sourceFile + " to target direcotry exception.");
				logFile.write(e.getMessage() + "\r\n");
				for (StackTraceElement stack : e.getStackTrace()) {
					logFile.write(stack.toString() + "\r\n");
				}
			} catch (IOException ie) {
				ie.printStackTrace();
			}

			return false;
		} catch (InterruptedException e) {
			try {
				logFile.write(e.getMessage() + "\r\n");
				for (StackTraceElement stack : e.getStackTrace()) {
					logFile.write(stack.toString() + "\r\n");
				}
			} catch (IOException ie) {
				ie.printStackTrace();
			}
			return false;
		} finally {
			try {
				logFile.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return true;
	}

	@Deprecated
	private Process programeRun(String path) throws IOException {

		String[] commands = new String[] { "/bin/bash", "-c", path };
		Process process = Runtime.getRuntime().exec(commands);

		return process;
	}

	public static void main(String[] args) {
		String dir = "<dir name=\"20120521112728\" href=\"20120521112728/\" />";
		int pos = dir.indexOf("\"") + 1;
		char[] chars = dir.toCharArray();
		String name = "";
		for (int i = pos; i < chars.length; i++) {
			if (chars[i] == '"') {
				break;
			} else {
				name += chars[i];
			}
		}
		System.out.println(name);
	}
}
