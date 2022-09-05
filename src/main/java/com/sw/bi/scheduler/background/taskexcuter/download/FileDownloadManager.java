package com.sw.bi.scheduler.background.taskexcuter.download;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;

import com.sw.bi.scheduler.util.DateUtil;

/**
 * <pre>
 * 	文件下载管理
 * 	1. 允许对指定文件实现多线程下载
 * 	2. 将来可以扩展该类，将它序列化到本地，方便存储下载的相关信息以便下次断点续传
 * </pre>
 * 
 * @author shiming.hong
 * @date 2014-08-01
 */
public class FileDownloadManager implements Serializable {
	private static final long serialVersionUID = -5360149351026728848L;

	private static final Logger log = Logger.getLogger(FileDownloadManager.class);

	/**
	 * 文件下载默认线程数
	 */
	public static int DEFAULT_FRAMENT_NUMBER = 5;

	/**
	 * 下载是否成功
	 */
	private volatile boolean success = false;

	/**
	 * 下载是否完成
	 */
	private volatile boolean completed = false;

	/**
	 * 本地文件名
	 */
	private String localFileName;

	/**
	 * 下载文件大小
	 */
	private long fileSize;

	/**
	 * 下载文件来源
	 */
	private DownloadInputStreamSource source;

	/**
	 * 文件下载分片
	 */
	private List<FileDownloadFragment> fileDownloadFragments;

	/**
	 * 日志文件
	 */
	private Writer loggerWriter;

	private FileDownloadManager(String localFileName, long fileSize, int fragmentNumber, DownloadInputStreamSource source, Writer loggerWriter) {
		this.fileSize = fileSize;
		this.localFileName = localFileName;
		this.loggerWriter = loggerWriter;
		this.source = source;
		fileDownloadFragments = new ArrayList<FileDownloadFragment>(fragmentNumber);
	}

	/**
	 * 开始下载
	 * 
	 * @throws Exception
	 */
	public boolean download() throws Exception {
		ExecutorService executorService = Executors.newFixedThreadPool(fileDownloadFragments.size());
		// final CountDownLatch countDownLatch = new CountDownLatch(fileDownloadFragments.size());

		long start = System.currentTimeMillis();

		success = false;

		// 启动下载状态监听
		new FileDownloadListener(this).start();

		try {
			// 创建本地文件
			RandomAccessFile localFile = new RandomAccessFile(localFileName, "rw");
			localFile.setLength(fileSize);
			localFile.close();

			Collection<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();

			// 开始多线程下载
			for (final FileDownloadFragment fileDownloadFragment : fileDownloadFragments) {
				Future<Boolean> future = executorService.submit(new Callable<Boolean>() {

					@Override
					public Boolean call() throws Exception {
						success = false;

						try {
							fileDownloadFragment.start();
							success = true;

						} finally {
							// countDownLatch.countDown();
						}

						return success;
					}

				});

				futures.add(future);
			}

			boolean isCancel = false;
			for (Future<Boolean> future : futures) {
				if (!future.get()) {
					isCancel = true;
					break;
				}
			}

			// countDownLatch.await();

			success = isCancel ? false : true;

			log("Downloaded -> " + localFileName + ", 耗时: " + new BigDecimal((System.currentTimeMillis() - start) / 1000.0 / 60).round(new MathContext(2)) + " min.");

		} catch (Exception e) {
			success = false;

		} finally {
			if (executorService != null && !executorService.isShutdown()) {
				executorService.shutdownNow();
			}

			if (source != null) {
				source.destory();
			}

			completed = true;
		}

		return success;
	}

	/**
	 * 下载日志记录
	 * 
	 * @param loggerContent
	 */
	protected void log(String loggerContent) {
		if (loggerWriter == null) {
			log.info(loggerContent);

		} else {
			try {
				loggerWriter.write(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + " - " + loggerContent + "\n");
				loggerWriter.flush();

				log.info(loggerContent);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public String getLocalFileName() {
		return localFileName;
	}

	public long getFileSize() {
		return fileSize;
	}

	public List<FileDownloadFragment> getFileDownloadFragments() {
		return fileDownloadFragments;
	}

	public void addFileDownloadFragments(Collection<FileDownloadFragment> fileDownloadFragments) {
		for (FileDownloadFragment fileDownloadFragment : fileDownloadFragments) {
			fileDownloadFragment.setFileDownloadManager(this);
			this.fileDownloadFragments.add(fileDownloadFragment);
		}
	}

	/**
	 * 下载是否成功
	 * 
	 * @return
	 */
	public boolean isCompleted() {
		return completed;
	}

	///////////////////////////////////////////////////////////////////////////////////////////

	public static FileDownloadManager createFileDownloadManager(String localFileName, long fileSize, DownloadInputStreamSource source) throws Exception {
		return createFileDownloadManager(localFileName, fileSize, DEFAULT_FRAMENT_NUMBER, source);
	}

	public static FileDownloadManager createFileDownloadManager(String localFileName, long fileSize, String loggerFileName, DownloadInputStreamSource source) throws Exception {
		return createFileDownloadManager(localFileName, fileSize, DEFAULT_FRAMENT_NUMBER, loggerFileName, source);
	}

	public static FileDownloadManager createFileDownloadManager(String localFileName, long fileSize, Writer loggerWriter, DownloadInputStreamSource source) throws Exception {
		return createFileDownloadManager(localFileName, fileSize, DEFAULT_FRAMENT_NUMBER, loggerWriter, source);
	}

	public static FileDownloadManager createFileDownloadManager(String localFileName, long fileSize, int fragmentNumber, DownloadInputStreamSource source) throws Exception {
		return createFileDownloadManager(localFileName, fileSize, fragmentNumber, (Writer) null, source);
	}

	public static FileDownloadManager createFileDownloadManager(String localFileName, long fileSize, int fragmentNumber, String loggerFileName, DownloadInputStreamSource source) throws Exception {
		Writer loggerWriter = null;
		if (StringUtils.hasText(loggerFileName)) {
			File loggerFile = new File(loggerFileName);
			if (!loggerFile.exists()) {
				loggerFile.createNewFile();
			}

			loggerWriter = new FileWriter(loggerFile, true);
		}

		return createFileDownloadManager(localFileName, fileSize, fragmentNumber, loggerWriter, source);
	}

	public static FileDownloadManager createFileDownloadManager(String localFileName, long fileSize, int fragmentNumber, Writer loggerWriter, DownloadInputStreamSource source) throws Exception {
		List<FileDownloadFragment> fileDownloadFragments = new ArrayList<FileDownloadFragment>();

		long fragmentSize = fileSize / fragmentNumber;
		long lastFragmentSize = fileSize % fragmentNumber;

		/**
		 * <pre>
		 * 	下载分片的开始值取值
		 * 	1. 下一个分片的开始是上一个分片的结束
		 *  2. 上一个分片通过getDownloadInputStream接口得到空值时下一个分片的开始值仍是上一个分片的开始值
		 * </pre>
		 */
		long start = 0;

		// 上一个下载分片
		FileDownloadFragment lastFileDownloadFragment = null;

		for (int i = 0; i < fragmentNumber; i++) {
			long end = i * fragmentSize + fragmentSize; // 分片结束位置

			if (fragmentNumber == (i + 1)) {
				end += lastFragmentSize;
				fragmentSize += lastFragmentSize;
			}

			InputStream downloadInputStream = source.getDownloadInputStream(start, end, fileSize, i, fragmentNumber);
			if (downloadInputStream == null) {
				if (lastFileDownloadFragment != null) {
					lastFileDownloadFragment.setEnd(end);
					start = end;
				}

			} else {
				lastFileDownloadFragment = new FileDownloadFragment(i, localFileName, start, end, downloadInputStream);
				fileDownloadFragments.add(lastFileDownloadFragment);

				StringBuilder message = new StringBuilder("创建下载分片");
				message.append("<#").append(Thread.currentThread().getId()).append("> ");
				message.append("-> ").append(source.getDownloadFileName()).append(" [");
				message.append(new BigDecimal(end - start).divide(new BigDecimal(1024 * 1024), 4, BigDecimal.ROUND_HALF_DOWN)).append(" MB, ");
				message.append(start).append(" - ").append(end).append("]");

				if (loggerWriter != null) {
					try {
						loggerWriter.write(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + " - " + message + "\n");
					} catch (Exception e) {}
				}
				log.info(message);

				start = end;
			}
		}

		if (fileDownloadFragments.size() == 0) {
			return null;
		}

		FileDownloadManager fileDownloadManager = new FileDownloadManager(localFileName, fileSize, fragmentNumber, source, loggerWriter);
		fileDownloadManager.addFileDownloadFragments(fileDownloadFragments);

		StringBuilder message = new StringBuilder();
		message.append("准备下载的文件 ");
		message.append(source.getDownloadFileName());
		message.append(" (").append(new BigDecimal(fileSize).divide(new BigDecimal(1024 * 1024), 2, BigDecimal.ROUND_HALF_DOWN)).append(" MB)");
		fileDownloadManager.log(message.toString());

		return fileDownloadManager;
	}
}
