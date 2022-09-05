package org.springframework.ui;

import java.util.ArrayList;
import java.util.List;

public class PaginationSupport {

	/**
	 * 当前页码
	 */
	private int pageNumber = 1;

	/**
	 * 每页显示记录数
	 */
	private int pageSize = 15;

	/**
	 * 总记录数
	 */
	private int total = 0;

	/**
	 * 分页结果
	 */
	private List paginationResults = new ArrayList();

	public PaginationSupport() {}

	public PaginationSupport(int pageNumber, int pageSize) {
		this.pageNumber = pageNumber;
		this.pageSize = pageSize;
	}

	/**
	 * 获得总页数
	 * 
	 * @return
	 */
	public int getPages() {
		if (total == 0)
			return 0;

		// 计算总页数
		int pages = total / pageSize;
		if (total % pageSize != 0)
			pages++;

		return pages;
	}

	public int getPageNumber() {
		return pageNumber;
	}

	public int getPageSize() {
		return pageSize;
	}

	public int getTotal() {
		return total;
	}

	@SuppressWarnings("unchecked")
	public List getPaginationResults() {
		return paginationResults;
	}

	public void setPageNumber(int pageNumber) {
		this.pageNumber = pageNumber;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public void setPaginationResults(List paginationResults) {
		this.paginationResults = paginationResults;
	}

	public boolean getSuccess() {
		return true;
	}

}
