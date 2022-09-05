package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.SchedulerExecTime;
import com.sw.bi.scheduler.service.SchedulerExecTimeHistoryService;
import com.sw.bi.scheduler.service.SchedulerExecTimeService;
import com.sw.bi.scheduler.util.Configure;
import com.sw.bi.scheduler.util.DateUtil;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

@Service("schedulerExecTimeService")
public class SchedulerExecTimeServiceImpl extends GenericServiceHibernateSupport<SchedulerExecTime> implements SchedulerExecTimeService {

	/**
	 * 各网关机执行状态日志目录
	 */
	public static File SCHEDULER_EXECUTE_LOG_PATH;

	@Autowired
	private SchedulerExecTimeHistoryService schedulerExecTimeHistoryService;

	private static SchedulerExecTime set;

	static {
		SCHEDULER_EXECUTE_LOG_PATH = new File("/home/tools/logs/scheduler/");
		if (!SCHEDULER_EXECUTE_LOG_PATH.exists()) {
			SCHEDULER_EXECUTE_LOG_PATH.mkdirs();
		}
	}

	@Override
	public boolean isFinished() {
		/**
		 * <pre>
		 * 2014-05-08
		 * 判断上一轮调度是否还在执行不从数据库判断
		 * 改成判断执行文件是否存在
		 * </pre>
		 */
		get();

		synchronized (set.getFlag()) {
			File executeFile = new File(SCHEDULER_EXECUTE_LOG_PATH, set.getFlag());
			return !executeFile.exists();
		}
		// return get().isFinished();
	}

	@Override
	public void begin() {
		log.info("begin to scheduler...");

		/**
		 * <pre>
		 * 	2014-05-07
		 * 	最近经常有发现一个任务有二个Action在同一时间点上同时被执行
		 * 	从日志中分析到的原因是因为第一次调度起来后修改调度完成状态
		 * 	为false时没有真正持久化到数据库，但又由于查MySQL连接数卡住
		 * 	了，导致这一轮调度一直卡了一分钟以，这时第二轮调度又被调起来
		 * 	了，这时第一轮调度又解锁了，这时二轮调度被并在了同一时间点
		 * 	执行，所以任务被选取到了二次，被执行了二次，Action也就有了
		 * 	同时执行的二条
		 * 	解决方法：将正在运行的状态保存到文件
		 * </pre>
		 */
		synchronized (set.getFlag()) {
			File executeFile = new File(SCHEDULER_EXECUTE_LOG_PATH, set.getFlag());
			if (!executeFile.exists()) {
				try {
					executeFile.createNewFile();

					log.info("网关机(" + set.getFlag() + ")进入独占执行状态,本轮未结束,下轮不能启动");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		get();

		set.setBeginTime(DateUtil.now());
		set.setEndTime(null);
		set.setUpdateTime(DateUtil.now());
		set.setFinished(false);

		update(set);
		flush();

		schedulerExecTimeHistoryService.begin();
	}

	@Override
	@Deprecated
	public void helpBegin() {
		log.info("begin to scheduler help...");

		get();

		set.setBeginTime(DateUtil.now());
		set.setEndTime(null);
		set.setUpdateTime(DateUtil.now());
		set.setFinished(false);

		update(set);
	}

	@Override
	public void finished() {
		get();

		set.setEndTime(DateUtil.now());
		set.setRunTime((set.getEndTime().getTime() - set.getBeginTime().getTime()) / 1000);
		set.setUpdateTime(DateUtil.now());
		set.setFinished(true);

		update(set);

		// 调度执行完后将执行文件删除
		synchronized (set.getFlag()) {
			File executeFile = new File(SCHEDULER_EXECUTE_LOG_PATH, set.getFlag());
			if (executeFile.exists()) {
				executeFile.delete();
			}
		}

		log.info("scheduler for this times is finished(" + set.getRunTime() + "s).");

		schedulerExecTimeHistoryService.finished();
	}

	@Override
	@Deprecated
	public void helpFinished() {
		get();

		set.setEndTime(DateUtil.now());
		set.setRunTime((set.getEndTime().getTime() - set.getBeginTime().getTime()) / 1000);
		set.setUpdateTime(DateUtil.now());
		set.setFinished(true);

		update(set);

		log.info("scheduler help for this times is finished(" + set.getRunTime() + "s).");
	}

	@Override
	public boolean refinished(boolean immediately) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("flag", SCHEDULER_FLAG)); //网关机的标识
		criteria.add(Restrictions.eq("finished", false));

		if (!immediately) {
			Calendar calendar = DateUtil.getCalendar(DateUtil.now());
			calendar.add(Calendar.MINUTE, Configure.property(Configure.UPDATE_SCHEDULER_FINISHED_PERSIST_MINUTE, Integer.class) * -1);
			//updateTime比当前时间早15分钟及以上
			criteria.add(Restrictions.le("updateTime", calendar.getTime()));
		}

		SchedulerExecTime schedulerExecTime = (SchedulerExecTime) criteria.uniqueResult();
		if (schedulerExecTime == null)
			return false;

		// 调度执行完后将执行文件删除
		synchronized (schedulerExecTime.getFlag()) {
			File executeFile = new File(SCHEDULER_EXECUTE_LOG_PATH, schedulerExecTime.getFlag());
			if (executeFile.exists()) {
				executeFile.delete();
			}
		}

		//由程序将该条记录设置为： setFinished(true)  并进行短信告警处理
		schedulerExecTime.setEndTime(DateUtil.now());
		schedulerExecTime.setRunTime((schedulerExecTime.getEndTime().getTime() - schedulerExecTime.getBeginTime().getTime()) / 1000);
		schedulerExecTime.setUpdateTime(DateUtil.now());
		schedulerExecTime.setFinished(true);
		update(schedulerExecTime);

		set = schedulerExecTime;

		return true;
	}

	private SchedulerExecTime get() {
		if (set != null)
			return set;

		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("flag", SCHEDULER_FLAG));
		set = (SchedulerExecTime) criteria.uniqueResult();

		if (set == null) {
			set = new SchedulerExecTime();
			set.setFlag(SCHEDULER_FLAG);
			set.setDateDesc(DateUtil.getToday());
			set.setBeginTime(DateUtil.now());
			set.setCreateTime(DateUtil.now());
			set.setFinished(true);

			save(set);
		}

		return set;
	}

}
