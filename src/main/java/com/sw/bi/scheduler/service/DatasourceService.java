package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.model.Datasource;

public interface DatasourceService extends GenericService<Datasource> {

	/**
	 * 测试指定数据源
	 * 
	 * @param datasource
	 * @return
	 */
	public String testDatasource(Datasource datasource);

	/**
	 * 修改指定数据源密码
	 * 
	 * @param datasourceId
	 * @param password
	 * @return
	 */
	public String changePassword(long datasourceId, String password);

	/**
	 * 统一加密码数据源密码
	 */
	public void batchChangePassword();

	/**
	 * 禁用数据源
	 * 
	 * @param datasource
	 */
	public void logicDelete(Datasource datasource);

	/**
	 * 启用数据源
	 * 
	 * @param datasource
	 */
	public void recovery(Datasource datasource);
	
	/**
	 * 查詢数据源
	 * 
	 * @param datasource
	 */
	
	public Datasource queryById(Long datasourceId);

}
