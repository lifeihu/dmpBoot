package com.sw.bi.scheduler.background.taskexcuter.download;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.sw.bi.scheduler.util.DateUtil;

/**
 * 文件下载状态
 * 
 * @author shiming.hong
 * @date 2014-08-02
 */
public class FileDownloadListener extends Thread {

	/**
	 * 
	 */
	private FileDownloadManager fileDownloadManager;

	public FileDownloadListener(FileDownloadManager fileDownloadManager) {
		this.fileDownloadManager = fileDownloadManager;
	}

	@Override
	public void run() {
		Collection<FileDownloadFragment> fileDownloadFragments = fileDownloadManager.getFileDownloadFragments();

		/*long fileSize = fileDownloadManager.getFileSize();
		StringBuilder message = new StringBuilder();*/
		long downloaded = 0l;

		while (!fileDownloadManager.isCompleted()) {
			/*message.setLength(0);

			message.append("下载状态 [Comleted: ");*/

			fileDownloadManager.log(">>>> 下载状态(" + DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + ") <<<<");

			for (FileDownloadFragment fileDownloadFragment : fileDownloadFragments) {
				downloaded += fileDownloadFragment.getCurrent() - fileDownloadFragment.getStart();
				fileDownloadManager.log(fileDownloadFragment.toString());
			}

			fileDownloadManager.log("");
			/*message.append(new BigDecimal(downloaded / fileSize * 100.0).round(new MathContext(2))).append("% (");
			message.append(new BigDecimal(downloaded)).append(" / ");
			message.append(new BigDecimal(fileSize)).append(" byte), Cost: ");
			message.append(new BigDecimal((System.currentTimeMillis() - startTime) / 1000.0 / 60).round(new MathContext(4))).append(" min]");
			fileDownloadManager.log(message.toString());*/

			try {
				TimeUnit.SECONDS.sleep(15);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// fileDownloadManager.log("成功下载 -> " + fileDownloadManager.getLocalFileName() + "(" + new BigDecimal(downloaded / 1024 / 1024).round(new MathContext(4)) + " MB)");

	}
}
