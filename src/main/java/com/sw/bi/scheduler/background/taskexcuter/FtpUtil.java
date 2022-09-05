package com.sw.bi.scheduler.background.taskexcuter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Date;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.util.StringUtils;

import com.sw.bi.scheduler.util.DateUtil;
import com.sw.bi.scheduler.util.ExecAgent.ExecResult;
import com.sw.bi.scheduler.util.SshUtil;

//利用org.apache.commons.net.ftp包实现上传下载文件资源
//hadoop jar ftp.jar com.shunwang.ftp.FtpUtil
public class FtpUtil {

	public synchronized static FTPClient getFTPClient(String url, int port, String username, String password, String remotePath, Writer logFileWriter) throws IOException {
		int reply;
		FTPClient ftp = new FTPClient();

		try {
			logFileWriter.write(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + " - FTP服务器(" + url + ":" + port + ").\n");
			ftp.connect(url, port);
		} catch (Exception e) {
			logFileWriter.write(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + " - FTP服务器连接异常.\r\n");
			return null;
		}
		logFileWriter.flush();

		ftp.login(username, password);
		logFileWriter.write(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + " - 正在登录FTP服务器..." + "\r\n");

		reply = ftp.getReplyCode();
		logFileWriter.write(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + " - FTP校验码：" + reply + ".\r\n");
		if (!FTPReply.isPositiveCompletion(reply)) {
			ftp.disconnect();
			//检查FTP的IP,端口,用户名,密码是否正确,IP是否可以ping通
			logFileWriter.write(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + " - FTP服务登录失败." + "\r\n");
			return null;
		}

		logFileWriter.write(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + " - FTP服务器登录成功." + "\r\n");
		logFileWriter.flush();

		// 切换FTP服务器的当前工作目录
		if (StringUtils.hasText(remotePath)) {
			if (!ftp.changeWorkingDirectory(remotePath)) {
				ftp.disconnect();
				ftp = null;
				logFileWriter.write(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + " - 更改FTP工作目录(" + remotePath + ")失败.\r\n");
			} else {
				logFileWriter.write(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + " - FTP工作目录(" + remotePath + ").\n");
			}
		}
		logFileWriter.flush();

		return ftp;
	}

	/**
	 * 在FTP上创建指定目录
	 * 
	 * @param ftp
	 * @param directory
	 */
	public static void createDirectory(FTPClient ftp, String directory) {
		if (!StringUtils.hasText(directory))
			return;

		try {
			ftp.changeWorkingDirectory("/");

			if (directory.startsWith("/")) {
				directory = directory.substring(1);
			}

			if (directory.indexOf("/") > -1) {
				int pos = 0;
				while ((pos = directory.indexOf("/")) > -1) {
					String name = directory.substring(0, pos);

					if (!ftp.changeWorkingDirectory(name)) {
						ftp.makeDirectory(name);
					}

					ftp.changeWorkingDirectory(name);

					directory = directory.substring(pos + 1);
				}

				if (StringUtils.hasText(directory)) {
					if (!ftp.changeWorkingDirectory(directory)) {
						ftp.makeDirectory(directory);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 将文件从指定FTP中上传至Hadoop中
	 * 
	 * @param ftp
	 * @param fileName
	 * @param remotePath
	 * @param remoteBakPath
	 * @param localPath
	 * @param localBakPath
	 * @param localErrPath
	 * @param categoryHdfsPath
	 * @param settingTime
	 * @param logFileWriter
	 * @return
	 */
	public static boolean download2Hdfs(//
	String gateway, // gateway
			FTPClient ftp,
			String fileName, // ftp
			String remotePath,
			String remoteBakPath, // remote
			String localPath,
			String localBakPath,
			String localErrPath, // local
			String categoryHdfsPath, // hdfs
			Date settingTime,
			Writer logFileWriter) {

		String result = null;
		boolean success = true;
		FileWriter writer = null;

		try {

			File localF = new File(localPath);
			if (!localF.exists()) {
				localF.mkdirs();
			}

			// TODO 跟踪调度用,正式运行时可能删除了
			// writer = new FileWriter(localPath + "/ftp-total.log", true);

			// logFileWriter.write(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + " - file name is " + fileName + "\r\n");

			/////////////////////////////// 从远程目录下载文件至本地临时目录

			File localFile = new File(localPath + "/" + fileName);
			OutputStream is = new FileOutputStream(localFile);
			ftp.retrieveFile(fileName, is); // 下载FTP远程目录下的文件到linux指定目录
			logFileWriter.write(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + " - 下载FTP文件(" + fileName + ")至本地目录(" + localPath + ").\n");
			is.close();

			logFileWriter.flush();

			/////////////////////////////// 将FTP远程目录下的文件移动到FTP远程备份目录

			if (StringUtils.hasText(remoteBakPath)) {
				createDirectory(ftp, remoteBakPath);
				ftp.changeWorkingDirectory(remotePath);

				String bakFileName = remoteBakPath + "/" + fileName;
				ftp.rename(fileName, bakFileName);
				ftp.deleteFile(fileName);
				logFileWriter.write(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + " - FTP文件(" + localPath + "/" + fileName + ")被移至FTP备份目录(" + remoteBakPath + ").\n");
			}

			logFileWriter.flush();

			// 如果HDFS路径不存在,则先创建
			ExecResult execResult = SshUtil.execHadoopCommand(gateway, "test -e " + categoryHdfsPath);
			if (execResult.failure()) {
				execResult = SshUtil.execHadoopCommand(gateway, "mkdir " + categoryHdfsPath);
				logFileWriter.write(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + " - HDFS目录(" + categoryHdfsPath + ")创建" + (execResult.success() ? "成功" : "失败") + ".\n");
			}

			// 将linux本地临时文件存放目录下的文件上传到HDFS目录下
			String localFileName = localPath + "/" + fileName;
			// logFileWriter.write(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + " - the file \"" + fileName + "\" upload to HDFS directory \"" + categoryHdfsPath + "\".\r\n");
			// success = copyFile2Hdfs(gateway, fileName, localFileName, categoryHdfsPath, logFileWriter);
			// success = !StringUtils.hasText(SshUtil.execHadoopCommand(gateway, "put " + localFileName + " " + categoryHdfsPath));
			execResult = SshUtil.execHadoopCommand(gateway, "put " + localFileName + " " + categoryHdfsPath);
			success = execResult.success();
			logFileWriter.write(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + " - 本地文件(" + localFileName + ")上传至HDFS目录(" + categoryHdfsPath + ")" + (success ? "成功" : "失败") + ".\n");

			// 如果上传HDFS失败,则将此文件拷贝到 linux本地错误文件存放目录
			if (!success) {
				logFileWriter.write(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + " - " + execResult.getStderr());

				if (StringUtils.hasText(localErrPath)) {
					File localErrPathF = new File(localErrPath);
					if (!localErrPathF.exists()) {
						localErrPathF.mkdirs();
						logFileWriter.write(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + " - 创建本地错误目录(" + localErrPath + ").\r\n");
					}

					String localErrName = localErrPath + "/" + fileName;
					File localErrFileName = new File(localErrName);
					if (!localErrFileName.exists()) {
						logFileWriter.write(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + " - 本地文件(" + localFileName + ")被移至本地错误目录(" + localErrPath + ").\n");
						CopyFile.copyFile(localFileName, localErrName);
					}

				}

			} else {
				if (StringUtils.hasText(localBakPath)) {
					File localBakPathF = new File(localBakPath);
					if (!localBakPathF.exists()) {
						localBakPathF.mkdirs();
						logFileWriter.write(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + " - 创建本地备份目录(" + localBakPath + ").\r\n");
					}

					String localBakName = localBakPath + "/" + fileName;
					File localBakFileName = new File(localBakName);
					if (!localBakFileName.exists()) {
						logFileWriter.write(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + " - 本地文件(" + localFileName + ")被移至本地备份目录(" + localBakPath + ").\n");
						CopyFile.copyFile(localFileName, localBakName);
					}
				}
			}

			// 最后删除 linux本地临时文件存放目录下的文件
			logFileWriter.write(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + " - 删除本地文件(" + localFileName + ").\n");
			CopyFile.delFile(localFileName);

			logFileWriter.flush();

		} catch (IOException e) {
			e.printStackTrace();
			try {
				logFileWriter.write(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + " - 本地文件(" + fileName + ")上传至HDFS目录(" + categoryHdfsPath + ")异常.\n");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			success = false;

		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return success;
	}

	/**
	 * 指定的文件与指定的日期是否一致
	 * 
	 * @param fileName
	 * @param year
	 * @param month
	 * @param date
	 * @param hour
	 * @param minute
	 * @param dateTimePositions
	 * @return
	 */
	public static boolean isSame(String fileName, Integer year, Integer month, Integer date, Integer hour, Integer minute, String[] dateTimePositions) {
    
		try {
			
			
			System.out.println( fileName + "---"+ year+ "---" +  month+ "---" +  date+ "---" +  hour+ "---" +  minute + "---"+ dateTimePositions.toString());
			
			
			// 文件名中的时间与settingTime的时间是否一致
			boolean same = true;
			// 0,4|5,7|8,10|11,13|14,16 用|分割后的数组
			for (int i = 0; i < dateTimePositions.length; i++) {
				String position = dateTimePositions[i];

				if (!StringUtils.hasText(position)) {
					continue;
				}

				Integer[] token = (Integer[]) ConvertUtils.convert(position.split(","), Integer.class);
				Integer value = Integer.valueOf(fileName.substring(token[0], token[1]));

				// 分别从fileName中,在循环dateTimePositions的时候,把年,月,日,时,分的值取出来,保存在value中。去和setting_time中的年月日时分比较
				// 如果都一样,则same=true,否则为false,跳出循环,返回这个same的值
				if (i == 0) {
					same = year == null ? true : value == year.intValue();
				} else if (i == 1) {
					same = month == null ? true : value == month.intValue();
				} else if (i == 2) {
					same = date == null ? true : value == date.intValue();
				} else if (i == 3) {
					same = hour == null ? true : value == hour.intValue();
				} else if (i == 4) {
					same = minute == null ? true : value == minute.intValue();
				}

				if (!same) {
					break;
				}
			}

			return same;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 上传本地文件至FTP服务器
	 * 
	 * @param ftp
	 * @param file
	 * @param fileName
	 * @return
	 */
	public static boolean uploadFile(FTPClient ftp, File file, String fileName) {
		if (!file.exists()) {
			return true;
		}

		boolean result = true;
		InputStream is = null;
		try {
			is = new FileInputStream(file);
			ftp.storeFile(fileName, is);

		} catch (Exception e) {
			result = false;
			e.printStackTrace();

		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return result;
	}

	/**
	 * 
	 * @param local_file_name
	 * @param local_path
	 * @param hdfs_path
	 * @return
	 */
	/*public static boolean copyFile2Hdfs(String gateway, String local_file_name, String local_path, String hdfs_path, Writer logFileWriter) {

		try {
			Configuration config = new Configuration();
			FileSystem hdfs = FileSystem.get(config);

			String hdfs_file_name = hdfs_path + "/" + local_file_name;
			boolean isFileExists = hdfs.exists(new Path(hdfs_file_name));
			if (isFileExists) {
				logFileWriter.write(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + " - copy to \"" + hdfs_path + "\" failed. because \"" + local_file_name +
						"\" file already exists, please manually delete.\r\n");
				logFileWriter.flush();
				return false;
			}

			Path lPath = new Path(local_path);
			Path hadoopPath = new Path(hdfs_path);
			logFileWriter.write("准备将本地文件(" + local_path + ")上传至HDFS(" + hdfs_path + ").\n");
			hdfs.copyFromLocalFile(lPath, hadoopPath); //

		} catch (Exception e) {
			StringWriter writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
			try {
				logFileWriter.write(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + " - " + writer.getBuffer().toString() + "\n");
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			return false;
		}
		return true;
	}*/
	// 把这个项目打成jar包. 如果同步的类型是: 远程文件同步,则调用这个jar的FtpUtil.java
	// 再根据具体情况决定是调downFile还是downloadFile
	// 其他配置参数可以由界面保存到数据库里
	// 如果不是远程文件同步这种类型,是其他类型的同步,则调用datax里的Launcher.java
	// 没必要非要把这个类集成到datax.jar里去.
	public static void main(String args[]) {

		// FTP的IP地址
		// FTP端口
		// FTP用户名
		// FTP密码
		// FTP远程目录
		// FTP远程备份目录
		// FTP远程错误文件存放目录
		// linux本地临时文件存放目录
		// linux本地错误文件存放目录
		// linux目标HDFS目录

		// 连接上远程FTP地址,将FTP远程目录下的所有文件同步到linux本地临时文件.并且上传到HDFS上去.
		// 然后再把文件备份一份,放在FTP远程备份目录下.
		// 在FTP远程目录下文件备份过程中，出现错误,则将错误文件存放在FTP远程错误文件存放目录;
		// 在put到HDFS出现错误时,把错误文件存放到linux本地错误文件存放目录.
		/*		boolean flag = FtpUtil.downloadFileFiveMin("172.16.15.162", 21, "feng.li",
						"abc#123", "d:/test1", "d:/test2", "d:/test3", "/root/test1",
						"/root/test3", "hdfs://namenode:50001/mytmp/path");
				System.out.println(flag);*/

		// FTP的IP地址
		// FTP端口
		// FTP用户名
		// FTP密码
		// 成功标记,表示远程目录下的文件已经准备好了
		// 检查间隔 单位秒
		// FTP远程目录
		// linux本地临时文件存放目录
		// linux目标HDFS目录

		// 先连接上远程FTP地址,检查FTP远程目录下,是否有指定的success.txt标记,如果有,则开始同步;如果没有,则等待60秒后,继续判断.
		// 将FTP远程目录下的文件(success.txt标记除外),同步到linux本地临时文件存放目录.并且上传到HDFS上去. 然后删除linux临时目录下的文件.

		/*boolean successflag;
		try {
			successflag = FtpUtil.downloadFileDaily("172.16.15.162", 21, "feng.li", "abc#123", "success.txt", 60, "d:/test1", "/root/test1", "hdfs://namenode:50001/mytmp/path", null);
			System.out.println(successflag);
		} catch (IOException e) {
			e.printStackTrace();
		}*/

	}

}
