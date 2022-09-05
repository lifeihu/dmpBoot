package com.sw.bi.scheduler.background.taskexcuter;

/**
 * 并发分类,用于统计各分类的信息
 * 
 * @author shiming.hong
 */
public class ConcurrentCategory {

	private int category;

	private String categoryName;

	/**
	 * 当前分类的任务最大并发数
	 */
	private int maxConcurrentNumber = 0;

	/**
	 * 当前分类的大数据任务的最大并发数
	 */
	private int bigDataMaxConcurrentNumber;

	/**
	 * 当前分类选取的任务数量
	 */
	private int selectNumber = 0;

	/**
	 * 当前分类选取的大数据任务数量
	 */
	private int bigDataSelectNumber = 0;

	/**
	 * 当前分类正在运行的任务数量
	 */
	private int runningNumber = 0;

	/**
	 * 当前分类正在运行的大数据任务数量
	 */
	private int bigDataRunningNumber = 0;

	/**
	 * 当前分类还能提交的任务配额(该值不参于运算只用于日志显示)
	 */
	private int submitQuota = 0;

	/**
	 * 当前分类还能提交的任务数量(该后面还要参于运算)
	 */
	private int submitNumber = 0;

	/**
	 * 当前分类还能提交的大数据任务配额(该值不参于运算只用于日志显示)
	 */
	private int bigDataSubmitQuota = 0;

	/**
	 * 当前分类还能提交的大数据任务数量(该值后面还要参于运算)
	 */
	private int bigDataSubmitNumber = 0;

	/**
	 * 当前分类实际提交的任务数量
	 */
	private int actualSubmitNumber = 0;

	/**
	 * 当前分类实际提交的大数据任务数量
	 */
	private int bigDataActualSubmitNumber = 0;

	/**
	 * 当前分类实际排除的任务数量
	 */
	private int removeNumber = 0;

	/**
	 * 当前分类实际排除的大数据任务数量
	 */
	private int bigDataRemoveNumber = 0;

	/**
	 * 当前分类实际排除的非本网关机执行的任务数量
	 */
	private int removeNotGatewayNumber = 0;

	/**
	 * 当前分类实际排除的非本网关机执行的大数据任务数量
	 */
	private int bigDataRemoveNotGatewayNumber = 0;

	public ConcurrentCategory(int category) {
		this.category = category;
	}

	public int getCategory() {
		return category;
	}

	public void setCategory(int category) {
		this.category = category;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	public int getMaxConcurrentNumber() {
		return maxConcurrentNumber;
	}

	public void setMaxConcurrentNumber(int maxConcurrentNumber) {
		this.maxConcurrentNumber = maxConcurrentNumber;
	}

	public int getBigDataMaxConcurrentNumber() {
		return bigDataMaxConcurrentNumber;
	}

	public void setBigDataMaxConcurrentNumber(int bigDataMaxConcurrentNumber) {
		this.bigDataMaxConcurrentNumber = bigDataMaxConcurrentNumber;
	}

	public int getSelectNumber() {
		return selectNumber;
	}

	public void setSelectNumber(int selectNumber) {
		this.selectNumber = selectNumber;
	}

	public void addSelectNumber() {
		this.selectNumber += 1;
	}

	public int getBigDataSelectNumber() {
		return bigDataSelectNumber;
	}

	public void setBigDataSelectNumber(int bigDataSelectNumber) {
		this.bigDataSelectNumber = bigDataSelectNumber;
	}

	public void addBigDataSelectNumber() {
		this.bigDataSelectNumber += 1;
	}

	public int getRunningNumber() {
		return runningNumber;
	}

	public void setRunningNumber(int runningNumber) {
		this.runningNumber = runningNumber;
	}

	public int getBigDataRunningNumber() {
		return bigDataRunningNumber;
	}

	public void setBigDataRunningNumber(int bigDataRunningNumber) {
		this.bigDataRunningNumber = bigDataRunningNumber;
	}

	public int getSubmitQuota() {
		return submitQuota;
	}

	public void setSubmitQuota(int submitQuota) {
		this.submitQuota = submitQuota;
		this.submitNumber = submitQuota;
	}

	public void minusSubmitNumber() {
		this.submitNumber -= 1;
	}

	public boolean hasSubmitNumber() {
		return this.submitNumber > 0;
	}

	public int getBigDataSubmitQuota() {
		return bigDataSubmitQuota;
	}

	public void setBigDataSubmitQuota(int bigDataSubmitQuota) {
		this.bigDataSubmitQuota = bigDataSubmitQuota;
		this.bigDataSubmitNumber = bigDataSubmitQuota;
	}

	public void minusBigDataSubmitNumber() {
		this.bigDataSubmitNumber -= 1;
	}

	public boolean hasBigDataSubmitNumber() {
		return this.bigDataSubmitNumber > 0;
	}

	public int getActualSubmitNumber() {
		return actualSubmitNumber;
	}

	public void setActualSubmitNumber(int actualSubmitNumber) {
		this.actualSubmitNumber = actualSubmitNumber;
	}

	public void addActualSubmitNumber() {
		this.actualSubmitNumber += 1;
	}

	public int getBigDataActualSubmitNumber() {
		return bigDataActualSubmitNumber;
	}

	public void setBigDataActualSubmitNumber(int bigDataActualSubmitNumber) {
		this.bigDataActualSubmitNumber = bigDataActualSubmitNumber;
	}

	public void addBigDataActualSubmitNumber() {
		this.bigDataActualSubmitNumber += 1;
	}

	public int getRemoveNumber() {
		return removeNumber;
	}

	public void setRemoveNumber(int removeNumber) {
		this.removeNumber = removeNumber;
	}

	public void addRemoveNumber() {
		this.removeNumber += 1;
	}

	public void minusRemoveNumber() {
		this.removeNumber -= 1;
	}

	public int getBigDataRemoveNumber() {
		return bigDataRemoveNumber;
	}

	public void setBigDataRemoveNumber(int bigDataRemoveNumber) {
		this.bigDataRemoveNumber = bigDataRemoveNumber;
	}

	public void addBigDataRemoveNumber() {
		this.bigDataRemoveNumber += 1;
	}

	public int getRemoveNotGatewayNumber() {
		return removeNotGatewayNumber;
	}

	public void setRemoveNotGatewayNumber(int removeNotGatewayNumber) {
		this.removeNotGatewayNumber = removeNotGatewayNumber;
	}

	public void addRemoveNotGatewayNumber() {
		this.removeNotGatewayNumber += 1;
	}

	public int getBigDataRemoveNotGatewayNumber() {
		return bigDataRemoveNotGatewayNumber;
	}

	public void setBigDataRemoveNotGatewayNumber(int bigDataRemoveNotGatewayNumber) {
		this.bigDataRemoveNotGatewayNumber = bigDataRemoveNotGatewayNumber;
	}

	public void addBigDataRemoveNotGatewayNumber() {
		this.bigDataRemoveNotGatewayNumber += 1;
	}
}
