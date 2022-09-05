package com.sw.bi.scheduler.background.taskexcuter.download;

import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.net.ftp.FTPClient;

import com.sw.bi.scheduler.background.taskexcuter.FtpUtil;

/**
 * FTP下载流
 * 
 * @author shiming.hong
 * @date 2014-08-01
 */
public class FTPDownloadInputStreamSource implements DownloadInputStreamSource {

	private String url;
	private int port;
	private String username;
	private String password;
	private String workPath;
	private String downloadFileName;

	private Writer loggerWriter;

	private Collection<FTPClient> ftpClients;

	public FTPDownloadInputStreamSource(String url, String username, String password, String workPath, String downloadFileName, Writer loggerWriter) {
		this(url, 21, username, password, workPath, downloadFileName, loggerWriter);
	}

	public FTPDownloadInputStreamSource(String url, int port, String username, String password, String workPath, String downloadFileName, Writer loggerWriter) {
		this.url = url;
		this.port = port;
		this.username = username;
		this.password = password;
		this.workPath = workPath.endsWith("/") ? workPath : workPath + "/";
		this.downloadFileName = downloadFileName;
		this.loggerWriter = loggerWriter;

		this.ftpClients = new ArrayList<FTPClient>();
	}

	@Override
	public InputStream getDownloadInputStream(long start, long end, long fileSize, int fragmentIndex, int fragmentNumber) throws Exception {
		FTPClient ftpClient = FtpUtil.getFTPClient(url, port, username, password, workPath, loggerWriter);

		if (ftpClient == null) {
			return null;
		}

		ftpClients.add(ftpClient);
		/*ftp.setDataTimeout(60000 * 5);
		ftp.setConnectTimeout(60000 * 5);*/

		ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
		ftpClient.enterLocalPassiveMode();

		ftpClient.rest(String.valueOf(start));

		return ftpClient.retrieveFileStream(workPath + downloadFileName);
	}

	@Override
	public String getDownloadFileName() {
		return workPath + downloadFileName;
	}

	@Override
	public void destory() {
		for (FTPClient ftpClient : ftpClients) {
			try {
				ftpClient.logout();

				if (ftpClient.isConnected()) {
					ftpClient.disconnect();
					System.out.println("Disconnected " + url + ":" + port);
				}

				ftpClient = null;
			} catch (Exception e) {}
		}
	}

}
