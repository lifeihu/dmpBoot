package com.sw.bi.scheduler.model;

import java.util.Collection;
import java.util.HashSet;

/**
 * 运行数量计数器
 * 
 * @author shiming.hong
 */
public class RunningNumberCounterDto {

	private String scanDate;
	private String time;
	private int counter = 0;
	private Collection<Long> jobIds = new HashSet<Long>();
	private Collection<Long> actionIds = new HashSet<Long>();

	public String getScanDate() {
		return scanDate;
	}

	public void setScanDate(String scanDate) {
		this.scanDate = scanDate;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public int getCounter() {
		return counter;
	}

	public void setCounter(int counter) {
		this.counter = counter;
	}

	public void addJobId(long jobId) {
		this.jobIds.add(jobId);
	}

	public String getJobIds() {
		StringBuilder ids = new StringBuilder();
		for (Long jobId : jobIds) {
			if (ids.length() > 0) {
				ids.append(",");
			}
			ids.append(jobId);
		}

		return ids.toString();
	}

	public void setJobIds(Collection<Long> jobIds) {
		this.jobIds = jobIds;
	}

	public void addActionId(long actionId) {
		this.actionIds.add(actionId);
	}

	public String getActionIds() {
		StringBuilder ids = new StringBuilder();
		for (Long actionId : actionIds) {
			if (ids.length() > 0) {
				ids.append(",");
			}
			ids.append(actionId);
		}

		return ids.toString();
	}

	public void setActionIds(Collection<Long> actionIds) {
		this.actionIds = actionIds;
	}

	public void addCounter() {
		this.counter += 1;
	}

}
