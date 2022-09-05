package com.sw.bi.scheduler.background.taskexcuter.download;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.math.MathContext;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

/**
 * 文件下载分片
 * 
 * @author shiming.hong
 * @date 2014-08-01
 */
public class FileDownloadFragment {
	private static final Logger log = Logger.getLogger(FileDownloadFragment.class);

	private static final int BUFFER_SIZE = 1024;

	private FileDownloadManager fileDownloadManager;

	private int fragmentIndex;

	/**
	 * 本地文件
	 */
	private String localFileName;

	/**
	 * 分片开始位置
	 */
	private long start;

	/**
	 * 分片结束位置
	 */
	private long end;

	/**
	 * 分片当前位置(预留用于断点续载)
	 */
	private long current;

	/**
	 * 下载字节数
	 */
	private long downloaded = 0l;

	/**
	 * 下载开始时间
	 */
	private long startTime = 0;

	/**
	 * 用于下载的文件流
	 */
	private InputStream inputStream;

	public FileDownloadFragment(int fragmentIndex, String localFileName, long start, long end, InputStream downloadInputStream) {
		this.fragmentIndex = fragmentIndex;
		this.localFileName = localFileName;
		this.start = start;
		this.end = end;
		this.current = start;
		this.inputStream = downloadInputStream;
	}

	public void start() throws IOException {
		RandomAccessFile localFile = null;
		BufferedInputStream bis = null;

		byte[] buff = new byte[BUFFER_SIZE];

		startTime = System.currentTimeMillis();

		try {
			localFile = new RandomAccessFile(localFileName, "rw");
			localFile.seek(start);

			if (inputStream instanceof BufferedInputStream) {
				bis = (BufferedInputStream) inputStream;
			} else {
				bis = new BufferedInputStream(inputStream);
			}

			while (current < end) {
				int len = bis.read(buff);

				if (len == -1) {
					break;
				}

				localFile.write(buff, 0, len);

				current += len;

				// 计算已下载字节数
				downloaded += current > end ? len - (current - end) : len;
			}

			fileDownloadManager.log(this.toString());

		} finally {
			IOUtils.closeQuietly(bis);

			if (localFile != null) {
				try {
					localFile.close();
				} catch (Exception e) {}
			}
		}
	}

	public FileDownloadManager getFileDownloadManager() {
		return fileDownloadManager;
	}

	public void setFileDownloadManager(FileDownloadManager fileDownloadManager) {
		this.fileDownloadManager = fileDownloadManager;
	}

	public long getStart() {
		return start;
	}

	public void setStart(long start) {
		this.start = start;
	}

	public long getEnd() {
		return end;
	}

	public void setEnd(long end) {
		this.end = end;
	}

	public long getCurrent() {
		return current;
	}

	@Override
	public String toString() {
		StringBuilder content = new StringBuilder("下载分片");
		content.append("<#").append(fragmentIndex).append("> ");
		/*content.append("[").append(new BigDecimal(downloaded).divide(new BigDecimal(1024 * 1024), 4, BigDecimal.ROUND_HALF_DOWN)).append(" / ");
		content.append(new BigDecimal(end - start).divide(new BigDecimal(1024 * 1024), 4, BigDecimal.ROUND_HALF_DOWN)).append(" MB");*/
		content.append("[").append(downloaded).append(" / ");
		content.append(end - start).append(" byte");
		// content.append(", Current: ").append(current);
		content.append(", Range: ").append(start).append(" - ").append(end);

		if (startTime > 0) {
			content.append(", Cost: ").append(new BigDecimal((System.currentTimeMillis() - startTime) / 1000.0 / 60).round(new MathContext(4))).append(" min");
		}

		content.append("]");

		return content.toString();
	}

}
