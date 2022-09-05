package com.sw.bi.scheduler.background.taskexcuter.download;

import java.io.InputStream;

/**
 * 下载流来源
 * 
 * @author shiming.hong
 * @date 2014-08-01
 */
public interface DownloadInputStreamSource {

	/**
	 * 获得下载文件输入流
	 * 
	 * @param start
	 * @param end
	 * @param fileSize
	 * @param fragmentIndex
	 * @param fragmentNumber
	 * @return
	 */
	public InputStream getDownloadInputStream(long start, long end, long fileSize, int fragmentIndex, int fragmentNumber) throws Exception;

	/**
	 * 获得下载文件名
	 * 
	 * @return
	 */
	public String getDownloadFileName();

	/**
	 * 
	 */
	public void destory();

}
