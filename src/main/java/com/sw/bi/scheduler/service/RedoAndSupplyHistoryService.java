package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.model.RedoAndSupplyHistory;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.util.Configure.TaskAction;
import org.springframework.ui.ConditionModel;
import org.springframework.ui.PaginationSupport;

import java.util.Date;

public interface RedoAndSupplyHistoryService extends GenericService<RedoAndSupplyHistory> {

	/**
	 * 生成操作批号(规则：年月日时分秒+5位随机数[-批量操作编号]_动作编号)
	 * 
	 * @param operateDate
	 * @param taskAction
	 * @param batchOperateNo
	 *            批量操作编号
	 * @return
	 */
	public String createOperateNo(Date operateDate, TaskAction taskAction/*, String batchOperateNo*/);

	/**
	 * 创建重跑或补数据操作的历史记录
	 * 
	 * @param task
	 * @param operateNo
	 * @param operateBy
	 * @param operateDate
	 * @param taskAction
	 */
	public RedoAndSupplyHistory create(Task task, boolean master, String operateNo, Long operateBy, Date operateDate, TaskAction taskAction);

	/**
	 * 指定的任务已经可以开始运行
	 * 
	 * @param task
	 * @return
	 */
	public boolean taskRunBegin(Task task);

	/**
	 * 指定的任务已经运行完毕
	 * 
	 * @param task
	 * @return
	 */
	public boolean taskRunFinished(Task task, boolean runSuccess);

	/**
	 * 根据指定批号和ID获得相应的明细记录
	 * 
	 * @param operateNo
	 * @param taskId
	 * @return
	 */
	public RedoAndSupplyHistory getRedoAndSupplyHistoryByTask(String operateNo, Long taskId);

	/**
	 * 分页获取重跑历史中的主任务
	 * 
	 * @param cm
	 * @return
	 */
	public PaginationSupport pagingRedoMasters(ConditionModel cm);

	/**
	 * 分页获取补数据历史中的主任务
	 * 
	 * @param cm
	 * @return
	 */
	public PaginationSupport pagingSupplyMasters(ConditionModel cm);
}
