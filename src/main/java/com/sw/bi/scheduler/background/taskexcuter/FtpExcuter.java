package com.sw.bi.scheduler.background.taskexcuter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.util.StringUtils;

import com.sw.bi.scheduler.background.taskexcuter.download.DownloadInputStreamSource;
import com.sw.bi.scheduler.background.taskexcuter.download.FTPDownloadInputStreamSource;
import com.sw.bi.scheduler.background.taskexcuter.download.FileDownloadManager;
import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.background.util.DxDESCipher;
import com.sw.bi.scheduler.model.Datasource;
import com.sw.bi.scheduler.model.Job;
import com.sw.bi.scheduler.model.JobDatasyncConfig;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.service.DatasourceService;
import com.sw.bi.scheduler.service.JobDatasyncConfigService;
import com.sw.bi.scheduler.service.JobService;
import com.sw.bi.scheduler.util.Configure.JobCycle;
import com.sw.bi.scheduler.util.Configure.JobType;
import com.sw.bi.scheduler.util.DateUtil;

public class FtpExcuter extends AbExcuter {
	private File ftptmpPath = new File("/home/tools/temp/ftptemp");

	private String gateway;
	private boolean result;
	private int jobType;

	private Integer threadNumber;

	private String url;
	private int port;
	private String userName;
	private String passWord;

	private String successFlag;
	private long checkMillis;
	private long timeoutMillis;

	private String remotePath;
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

	private FTPClient ftp;
	private FTPFile[] ftpFiles;

	@Deprecated
	private String hdfsPath;
	@Deprecated
	private String remoteBakPath;
	@Deprecated
	private String localBakPath;
	@Deprecated
	private String localErrPath;

	@Deprecated
	private String hiveTableName;
	@Deprecated
	private String hiveFields;
	@Deprecated
	private String createTableSql;

	@Deprecated
	private boolean appendDate = false;

	public FtpExcuter(Task task, String logFolder) {
		super(task, logFolder);
	}

	@Override
	public boolean excuteCommand() throws Exception {
		try {
			gateway = currentAction.getGateway();

			JobService jobService = BeanFactory.getService(JobService.class);
			JobDatasyncConfigService jobDatasyncConfigService = BeanFactory.getService(JobDatasyncConfigService.class);
			DatasourceService datasourceService = BeanFactory.getService(DatasourceService.class);

			Long jobId = currentTask.getJobId();
			Job job = jobService.get(jobId);
			jobType = (int) job.getJobType();

			JobDatasyncConfig config = jobDatasyncConfigService.getJobDatasyncConfigByJob(jobId);
			Datasource ftpDatasource = datasourceService.get(config.getDatasourceByFtpDatasourceId().getDatasourceId());

			url = ftpDatasource.getIp();
			port = Integer.parseInt(ftpDatasource.getPort());
			userName = ftpDatasource.getUsername();
			passWord = DxDESCipher.DecryptDES(ftpDatasource.getPassword(), ftpDatasource.getUsername());

			threadNumber = config.getThreadNumber() == null ? 1 : config.getThreadNumber();

			checkMillis = Long.parseLong(config.getCheckSeconds()) * 1000;
			timeoutMillis = config.getTimeoutMinutes() * 60 * 1000;

			remotePath = config.getFtpDir();
			localPath = config.getLinuxTmpDir();
			/*hdfsPath = config.getHdfsPath();
			remoteBakPath = config.getFtpBakDir();
			localBakPath = config.getLinuxBakDir();
			localErrPath = config.getLinuxErrDir();*/
			fileNumber = config.getFileNumber();
			/*hiveTableName = config.getHiveTableName();
			hiveFields = config.getHiveFields();
			createTableSql = config.getCreateTableSql();*///对这张表的格式是有要求的. 要求双分区hour=  min= 并且是textfile格式  实际文本数据中的分隔符与建表语句中的分隔符要一致

			/*if (!StringUtils.hasText(hdfsPath)) {
				appendDate = true;
				hdfsPath = Configure.property(Configure.HIVE_DATABASE_PATH); // "/group/user/tools/meta/hive-temp-table/tools.db";
			}*/

			// 替换动态参数
			remotePath = this.replaceParams(remotePath);
			localPath = this.replaceParams(localPath);
			/*hdfsPath = this.replaceParams(hdfsPath);
			remoteBakPath = this.replaceParams(remoteBakPath);
			localBakPath = this.replaceParams(localBakPath);
			localErrPath = this.replaceParams(localErrPath);*/

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
			// Date yesterday = DateUtil.getYesterday(settingTime);

			// 除分钟作业必须取当前分钟点外其他周期的作业都统一取上一个时间点
			if (cycleType == JobCycle.HOUR.indexOf()) {
				//calendar.add(Calendar.HOUR_OF_DAY, -1); // 前个小时
				
				if(jobId==117l||jobId==120l||jobId==121l){
					calendar.add(Calendar.HOUR_OF_DAY, -10); 
					log("小时点减10");
				}else{
					calendar.add(Calendar.HOUR_OF_DAY, -1);
					log("小时点减1");
				}
				
				
				
			} else if (cycleType == JobCycle.DAY.indexOf()) {
				calendar.add(Calendar.DATE, -1); // 前一天
			} else if (cycleType == JobCycle.WEEK.indexOf()) {
				calendar.add(Calendar.DATE, -7); // 上周
			} else if (cycleType == JobCycle.MONTH.indexOf()) {
				calendar.add(Calendar.MONTH, -1); // 上月
			}

			if (jobType == JobType.FTP_FILE_TO_HDFS_FIVE_MINUTE.indexOf()) {
				// Calendar calendar = DateUtil.getCalendar(settingTime);
				year = calendar.get(Calendar.YEAR);
				month = calendar.get(Calendar.MONTH) + 1;
				date = calendar.get(Calendar.DATE);
				hour = calendar.get(Calendar.HOUR_OF_DAY);
				minute = calendar.get(Calendar.MINUTE);
			} else {
				// Calendar calendar = DateUtil.getCalendar(yesterday);
				year = calendar.get(Calendar.YEAR);
				month = calendar.get(Calendar.MONTH) + 1;
				date = calendar.get(Calendar.DATE);
			}

			// 创建本地工作目录
			localWorkPath = new File(localPath);
			if (!localWorkPath.exists()) {
				localWorkPath.mkdirs();
			}

			// 检查HDFS目录是否存在

			/*ExecResult execResult = SshUtil.execHadoopCommand(gateway, "test -d " + hdfsPath);
			if (execResult.failure()) {
				SshUtil.execHadoopCommand(gateway, "mkdir " + hdfsPath);
				log("成功创建HDFS目录(" + hdfsPath + ").");
			}*/

			// 初始化FTP

			ftp = FtpUtil.getFTPClient(url, port, userName, passWord, remotePath, logFileWriter);
			if (ftp == null) {
				return false;
			}

			long waitMillis = 0;
			int times = 1;
			do {
				log("第" + times + "次FTP任务轮循开始...");

				// 根据文件唯一性规则获取相应文件
				ftpFiles = this.getFTPFilesByFileUniquePattern();

				log("FTP工作目录(" + ftp.printWorkingDirectory() + ")下获得" + ftpFiles.length + "个文件.");

				if (jobType == JobType.FTP_FILE_TO_HDFS.indexOf()) {
					log("当前作业类型 '需要成功标记'.");

					successFlag = config.getSuccessFlag();
					result = this.downloadYesterdayFilesNeedSuccessFlag();

				} else if (jobType == JobType.FTP_FILE_TO_HDFS_FIVE_MINUTE.indexOf()) {
					log("当前作业类型 '每间隔n分钟'.");

					result = this.downloadTodayFilesPerNMinutes();

				} else if (jobType == JobType.FTP_FILE_TO_HDFS_YESTERDAY.indexOf()) {
					log("当前作业类型 '不需要成功标记'.");

					result = this.downloadYesterdayFilesNotNeedSuccessFlag();
				}

				if (result) {
					log("FTP任务执行成功.");

					break;

				} else {
					if (waitMillis > timeoutMillis) {
						log("等待超时,FTP任务执行终止.");

						break;

					} else {
						log("文件未准备完毕,请耐心等待...");

						Thread.sleep(checkMillis);
						waitMillis += checkMillis;
					}
				}

				times += 1;
				// log("第" + (++times) + "次轮循结束.\n");

			}
			while (true);

		} catch (SocketException e) {
			if ("Broken pipe".equals(e.getMessage())) {
				result = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			if (ftp != null /*&& ftp.isConnected()*/) {
				try {
					ftp.logout();
					ftp.disconnect();
				} catch (Exception ioe) {
					// throw ioe;
				}
			}
		}

		return result;
	}

	/**
	 * 根据唯一文件匹配规则去获得FTP上的文件
	 * 
	 * @return
	 * @throws Exception
	 */
	private FTPFile[] getFTPFilesByFileUniquePattern() throws Exception {
		ftp.enterLocalPassiveMode();

		FTPFile[] ftpFiles = ftp.listFiles(); // 这句api有问题,偶尔程序会卡在这里(现在好像已经很久没再出现了)
		// fileUniquePattern = "^love_.*\\d+.*";

		// 如果没定义文件唯一性规则，则获得所有文件
		if (!StringUtils.hasText(fileUniquePattern)) {
			return ftpFiles;
		}

		Collection<FTPFile> matchedFiles = new ArrayList<FTPFile>();
		for (FTPFile ftpFile : ftpFiles) {
			if (ftpFile.isDirectory()) {
				continue;
			}

			String fileName = ftpFile.getName();
			// System.out.println(fileName);

			if (fileName.matches(fileUniquePattern)) {
				matchedFiles.add(ftpFile);
			}
		}
		// System.out.println("matched files: " + matchedFiles.size());

		return matchedFiles.toArray(new FTPFile[matchedFiles.size()]);
	}

	/**
	 * 根据成功标记下载昨天的文件
	 * 
	 * @return
	 * @throws Exception
	 */
	private boolean downloadYesterdayFilesNeedSuccessFlag() throws Exception {
		boolean notReady = true;

		log("开始检查成功标记(" + successFlag + ").");
		for (FTPFile file : ftpFiles) {
			if (file.isDirectory()) {
				continue;
			}

			if (file.getName().equals(successFlag)) {
				notReady = false;
				break;
			}
		}

		if (notReady) {
			return false;
		}

		log("成功标记(" + successFlag + ")已经准备完毕, 准备下载文件.");

		/////////////////////////////////////////////////////////////////////////////////////

		boolean success = true;
		for (FTPFile file : ftpFiles) {
			if (file.isDirectory()) {
				continue;
			}

			String fileName = file.getName();
			if (fileName.equals(successFlag)) {
				continue;
			}

			success = this.multiDownload(file);
			// success = FtpUtil.download2Hdfs(gateway, ftp, fileName, remotePath, null, localPath, null, null, hdfsPath, settingTime, logFileWriter);

			if (!success) {
				break;
			}
		}

		return success;
	}

	/**
	 * 不需要根据成功标记下载昨天的文件
	 * 
	 * @return
	 * @throws Exception
	 */
	private boolean downloadYesterdayFilesNotNeedSuccessFlag() throws Exception {
		boolean success = false;

		for (FTPFile file : ftpFiles) {
			if (file.isDirectory()) {
				continue;
			}

			String fileName = file.getName();

			// 没有对应到预设时间点的文件则跳过
			if (!FtpUtil.isSame(fileName, year, month, date, null, null, dateTimePositions)) {
				log(fileName+"-----"+year+"------"+month+"-----"+date+"-----"+dateTimePositions);
				log("没有对应到预设时间点的文件则跳过");
				continue;
			}

			log("准备下载FTP文件(" + fileName + ")...");
			success = this.multiDownload(file);

			// 每上传成功一个文件后就需要检查文件个数,因为文件不一定是同一时间到达的
			// 如：需要二个文件才算成功。当前有一个文件上传成功了，但另一个可能会在十分钟后到达
			if (success) {
				// 当计划文件数量为-1时则忽略文件数量的校验
				if (fileNumber.intValue() == -1) {
					success = true;

				} else {
					if (this.vaildateFileNumber(year, month, date, null, null)) {
						// 如果文件数量校验成功则代表已经满足指定的文件数了，则可以直接退出循环
						break;
					} else {
						success = false;
					}
				}
			}
		}

		return success;
	}

	/**
	 * 每间隔N分钟下载一次文件(用于分钟任务)
	 * 
	 * @return
	 * @throws Exception
	 */
	private boolean downloadTodayFilesPerNMinutes() throws Exception {
		boolean success = false;
		log("ftpFiles.size: "+ftpFiles.length);
		for (FTPFile file : ftpFiles) {
			if (file.isDirectory()) {
				continue;
			}

			String fileName = file.getName();

			// 没有对应到预设时间点的文件则跳过
			// 从任务的setting_time中解析出该任务的year, month, date, hour, minute
			// 然后遍历FTP目录下的文件,分别与任务的year, month, date, hour, minute去一个一个匹配,如果全部匹配上,则表示该任务就是要处理该文件,如果匹配不上,则跳到下一个文件继续匹配
			// 要注意的是,如果文件名不符合预先设定的规则,在匹配过程中就可能导致程序失败,所以这里要做异常处理
			if (!FtpUtil.isSame(fileName, year, month, date, hour, minute, dateTimePositions)) {
				log(fileName+"-----"+year+"------"+month+"-----"+date+"-----"+hour+"-----"+minute+"-----"+dateTimePositions);
				log("没有对应到预设时间点的文件则跳过");
				continue;
			}

			// 将远程文件下载至本地Linux目录
			success = this.multiDownload(file);

			// 每上传成功一个文件后就需要检查文件个数,因为文件不一定是同一时间到达的
			// 如：需要二个文件才算成功。当前有一个文件上传成功了，但另一个可能会在十分钟后到达
			if (success) {
				if (this.vaildateFileNumber(year, month, date, hour, minute)) {
					// 如果文件数量校验成功则代表已经满足指定的文件数了，则可以直接退出循环
					break;
				} else {
					success = false;
				}
			}
		}

		return success;
	}

	private boolean multiDownload(FTPFile ftpFile) throws Exception {
		if (threadNumber == 1) {
			// 单线程下载
			return this.singleDownload(ftpFile);
		}

		long fileSize = ftpFile.getSize();
		String downloadFileName = ftpFile.getName();

		// 下载是否成功
		boolean success = false;

		File localFile = new File(localWorkPath, downloadFileName);
		if (localFile.exists()) {
			success = localFile.delete();
			log("删除本地已存在文件(" + localFile.getAbsolutePath() + ")" + (success ? "成功" : "失败"));
		}

		////////////////////////////////////////////////////////////////////////

		try {
			DownloadInputStreamSource source = new FTPDownloadInputStreamSource(url, port, userName, passWord, remotePath, downloadFileName, logFileWriter);
			FileDownloadManager fileDownloadManager = FileDownloadManager.createFileDownloadManager(localFile.getAbsolutePath(), fileSize, threadNumber, logFileWriter, source);

			if (fileDownloadManager == null) {
				return false;
			}

			success = fileDownloadManager.download();

		} catch (Exception e) {
			success = false;
			e.printStackTrace();
		}

		if (!success) {
			if (localFile.exists()) {
				localFile.delete();
			}
		}

		return success;
	}

	private boolean singleDownload(FTPFile ftpFile) throws Exception {
		String fileName = ftpFile.getName();

		// 设置被动模式(可以解决在Linux上执行retrieveFile或listFiles命令时出现假死状态)
		// 这个方法的意思就是每次数据连接之前,ftp client告诉ftp server开通一个端口来传输数据
		ftp.enterLocalPassiveMode();

		// 以二进制方法下载
		ftp.setFileType(FTPClient.BINARY_FILE_TYPE);

		// 下载是否成功
		boolean success = false;

		// 获得远程文件大小
		long remoteFileSize = ftpFile.getSize();

		long localFileSize = 0;
		BufferedOutputStream bos = null;
		// BufferedInputStream bis = null;
		File localFile = new File(localWorkPath, fileName);

		if (localFile.exists()) {
			localFileSize = localFile.length();

			// 本地已经存在的文件大小与远程文件不一致时删除重载
			if (localFileSize != remoteFileSize) {
				success = localFile.delete();
				log("远程文件大小: " + remoteFileSize + ", 本地文件大小: " + localFileSize);
				log("删除本地已存在文件(" + localFile.getAbsolutePath() + ")" + (success ? "成功" : "失败"));
				if (!success) {
					return false;
				}
			} else {
				return true;
			}
		}

		try {
			bos = new BufferedOutputStream(new FileOutputStream(localFile));

			// 开始从远程下载文件
			log("开始下载FTP文件(" + fileName + ")...");
			success = ftp.retrieveFile(fileName, bos);
			log("下载FTP文件(" + fileName + ")" + (success ? "成功" : "失败"));

		} catch (Exception e) {
			success = false;
			e.printStackTrace();
			// throw e;

		} finally {
			// IOUtils.closeQuietly(bis);
			IOUtils.closeQuietly(bos);
		}

		if (!success) {
			if (localFile.exists()) {
				localFile.delete();
			}
		}

		// 如果文件已经存在则进行断点续载
		/*if (localFile.exists()) {
			// 获得本地已存在文件的大小
			localFileSize = localFile.length();

			if (localFileSize < remoteFileSize) {
				int percent = (int) (((localFileSize * 1.0) / remoteFileSize) * 100);
				log("本地文件(" + localFile.getAbsolutePath() + ")已经下载 " + percent + "%,进入断点下载模式");

				try {
					bos = new BufferedOutputStream(new FileOutputStream(localFile, true));

					// 设置断点下载的偏移值
					log("断点偏移值: " + localFileSize);
					ftp.setRestartOffset(localFileSize);

					// 开始从远程下载文件
					success = ftp.retrieveFile(fileName, bos);
					log("下载FTP文件(" + fileName + ")" + (success ? "成功" : "失败"));
					bis = new BufferedInputStream(ftp.retrieveFileStream(fileName));
					this.read(localFile.getAbsolutePath(), bis, bos, remoteFileSize, localFileSize);

				} catch (Exception e) {
					e.printStackTrace();
					throw e;

				} finally {
					// IOUtils.closeQuietly(bis);
					IOUtils.closeQuietly(bos);
				}

				success = ftp.completePendingCommand();

			} else if (localFileSize == remoteFileSize) {
				success = true;

			} else {
				log("本地文件(" + localFile.getAbsolutePath() + ")大小大于远程文件");
			}

		} else {
			try {
				bos = new BufferedOutputStream(new FileOutputStream(localFile));

				// 开始从远程下载文件
				success = ftp.retrieveFile(fileName, bos);
				log("下载FTP文件(" + fileName + ")" + (success ? "成功" : "失败"));
				bis = new BufferedInputStream(ftp.retrieveFileStream(fileName));
				this.read(localFile.getAbsolutePath(), bis, bos, remoteFileSize, localFileSize);

			} catch (Exception e) {
				e.printStackTrace();
				throw e;

			} finally {
				// IOUtils.closeQuietly(bis);
				IOUtils.closeQuietly(bos);
			}

			// 执行该命令前必须把上面的二个流关闭才行
			// success = ftp.completePendingCommand();
		}*/

		// TODO 文件成功后是否需要对文件内容进行校验

		return success;
	}

	/**
	 * 处理文件下载进度
	 * 
	 * @param fileName
	 * @param bis
	 * @param bos
	 * @param remoteFileSize
	 *            远程文件总大小
	 * @param localFileSize
	 *            当前已下载的大小
	 * @throws Exception
	 */
	private void read(String fileName, BufferedInputStream bis, BufferedOutputStream bos, long remoteFileSize, long localFileSize) throws Exception {
		byte[] bytes = new byte[2048];
		int len = 0;

		int lastPercent = 0;
		while ((len = bis.read(bytes)) != -1) {
			// 写入本地文件
			bos.write(bytes, 0, len);

			// 已下载大小
			localFileSize += len;

			int percent = (int) (((localFileSize * 1.0) / remoteFileSize) * 100);

			// System.out.println("download size: " + localFileSize + ", remote size: " + remoteFileSize + ", process: " + percent);

			if (percent % 5 == 0 && percent != lastPercent) {
				lastPercent = percent;
				log("本地文件(" + fileName + ")已下载 - " + percent + "%");
			}
		}
	}

	/**
	 * 检验文件数据
	 * 
	 * @param year
	 * @param month
	 * @param date
	 * @param hour
	 * @param minute
	 * @return
	 * @throws Exception
	 */
	private boolean vaildateFileNumber(Integer year, Integer month, Integer date, Integer hour, Integer minute) throws Exception {
		log("开始从本地目录(" + localPath + ")中检验文件个数.");
		log("计划文件数量: " + fileNumber + ".");
		log("日期匹配模式: " + year + ", " + month + ", " + date + ", " + hour + ", " + minute + ".");

		File[] files = localWorkPath.listFiles(new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				return pathname.isFile();
			}

		});

		if (files == null || files.length == 0) {
			log("检验文件数量失败,本地目录(" + localPath + ")中没有找到文件.");
			return false;
		}

		log("从本地目录(" + localPath + ")中获得" + files.length + "个文件.");

		int sameCount = 0;
		for (int i = 0, len = files.length; i < len; i++) {
			String fileName = files[i].getName();

			if (FtpUtil.isSame(fileName, year, month, date, hour, minute, dateTimePositions)) {
				sameCount += 1;
				log(i + ". 文件(" + fileName + ")匹配成功.");
			} else {
				log(i + ". 文件(" + fileName + ")匹配失败.");
			}

			if ((sameCount >= fileNumber.intValue())) {
				log("校验文件数量成功.");
				return true;
			}
		}

		log("校验文件数量失败.");

		return false;
	}
	 
	
	
	
	
	
	public static void main(String[] args){
		
		String fileName = "url_20150517_2.txt";
		String year = "2015";
		String month = "5";
		String date = "17";
				
	    System.out.println(fileName.substring(8,10));
		Integer value = Integer.valueOf(fileName.substring(8,10));
		System.out.println(value);
	 
		
		
		
		
		
	}
	
	
	
	
	
	
	
	
	
	
	
	
}
