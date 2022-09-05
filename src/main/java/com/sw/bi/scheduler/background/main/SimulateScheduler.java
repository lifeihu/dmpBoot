package com.sw.bi.scheduler.background.main;

import java.util.Date;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sw.bi.scheduler.background.exception.SchedulerException;
import com.sw.bi.scheduler.background.exception.TransactionLockTimeoutException;
import com.sw.bi.scheduler.background.exception.UnknownSchedulerException;
import com.sw.bi.scheduler.background.service.SchedulerService;
import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.service.ScheduleSystemStatusService;
import com.sw.bi.scheduler.service.SchedulerExecTimeService;
import com.sw.bi.scheduler.service.TaskCreateLogService;
import com.sw.bi.scheduler.service.TaskService;
import com.sw.bi.scheduler.util.Configure;
import com.sw.bi.scheduler.util.DateUtil;

import framework.commons.sender.MessagePlatform;
import com.sw.bi.scheduler.supports.MessageSenderAssistant;

/**
 * 模拟方式调度
 * 
 * @author shiming.hong
 * @date 2014-05-28
 */
@Component
public class SimulateScheduler {
	private static final Logger log = Logger.getLogger(SimulateScheduler.class);

	private static final MessageSenderAssistant messageSender = new MessageSenderAssistant();

	@Autowired
	private SchedulerExecTimeService schedulerExecTimeService;

	@Autowired
	private SchedulerService schedulerService;

	@Autowired
	private ScheduleSystemStatusService scheduleSystemStatusService;

	@Autowired
	private TaskCreateLogService taskCreateLogService;

	@Autowired
	private TaskService taskService;

	public void schedule() {
		Date today = DateUtil.getToday();

		String gateway = Configure.property(Configure.GATEWAY);
		String mobile = Configure.property(Configure.SMS_MOBILE);

		// 当前网关机的前提执行条件是否已经准备完成
		boolean isExecuteConditionPrepared = true;

		try {
			boolean isMainScheduler = scheduleSystemStatusService.isMasterGateway(gateway);
			Configure.property(Configure.MAIN_SCHEDULER, isMainScheduler);

			// 需要等前一次调度过程结束后方可继续
			if (!schedulerExecTimeService.isFinished()) {

				// 根据时间重置调度程序的完成状态
				if (schedulerExecTimeService.refinished(false)) {
					String msg = "由于上次模拟调度主程序(" + gateway + ")执行失败后已超过" + Configure.property(Configure.UPDATE_SCHEDULER_FINISHED_PERSIST_MINUTE, Integer.class) + "分钟，系统已自动重置为完成状态.";
					log.info(msg);

//					messageSender.sendSms(mobile, msg);
					messageSender.send(MessagePlatform.SMS_ADTIME,mobile, msg);

				} else {
					isExecuteConditionPrepared = false;
					log.info("上一轮模拟调度未执行完毕...");
					return;
				}
			}

			if (!isMainScheduler && !taskCreateLogService.isTaskCreated(today)) {
				isExecuteConditionPrepared = false;
				log.info("在任务未被创建时辅助调度程序停止调度，直至主调度程序将任务创建完毕。 ");
				return;
			}

			// 开始本次调度过程
			schedulerExecTimeService.begin();

			// 开始自动创建任务    只有主服务器创建当天的任务信息. 其他服务器不负责创建任务
			if (isMainScheduler && !taskCreateLogService.isTaskCreated(today)) {
				//当跨天之后，将所有scan_date是昨天的任务记录，scan_date更新为今天，以便后续的扫描程序可以用scan_date=今天来扫描到这些任务
				taskService.updateYesterdayScanDate(DateUtil.getYesterday(today));
				schedulerService.createTasks(today);
			}

			// 模拟方式调度
			schedulerService.simulateSchedule();

		} catch (Exception e) {
			this.processException(gateway, mobile, e);

		} finally {
			// 如果本轮调度是在执行的前提条件准备完成的情况下执行成功或失败，则最终都需要将状态改成“完成”
			// 如果本轮调度是由于执行的前提条件未准备完成的情况下退出，则不需要修改状态
			if (isExecuteConditionPrepared) {
				// 本次调度过程完毕
				schedulerExecTimeService.finished();
			}
		}
	}

	/**
	 * 处理异常
	 * 
	 * @param gateway
	 * @param mobile
	 * @param e
	 */
	private void processException(String gateway, String mobile, Exception e) {
		e.printStackTrace();

		// 重置调度程序的完成状态
		schedulerExecTimeService.refinished(true);

		//////////////////////////////////////////////////////////////////////////

		StringBuffer message = new StringBuffer();
		message.append("网关机(").append(gateway).append(")模拟调度主程序异常");

		// 捕获事务锁超时异常
		if (StringUtils.hasText(e.getMessage()) && e.getMessage().indexOf("Lock wait timeout exceeded; try restarting transaction") > -1) {
			e = new TransactionLockTimeoutException(e);
		}

		SchedulerException se = null;
		if (e instanceof SchedulerException) {
			se = (SchedulerException) e;

			// 目前只对已知的调度异常作出原因告警
			if (!(e instanceof UnknownSchedulerException)) {
				message.append("[原因(#").append(se.getErrorCode()).append(")： ").append(e.getMessage()).append("]");
			}
		}

		log.error(message.toString());
		// messageSender.sendSms(mobile, message.toString());
	}

	public static SimulateScheduler getSimulateScheduler() {
		return BeanFactory.getBean(SimulateScheduler.class);
	}

	/**
	 * @param gateway
	 * @param selectMaxNumber
	 *            任务最大选取数量(格式: [0-9点选取数量,其他时间段选取数量], 例: 500,300)
	 */
	public static void main(String[] args) {
		String gateway = null;

		/**
		 * <pre>
		 * 按时间段划分任务选取的最大数量
		 * 0:0 - 9:0 	最大选取500个
		 * 其他时间段	最大选取300个
		 * </pre>
		 */
		/**
		 * 
		 * 模拟后台轮询方式就是选取若干个状态是非成功，非运行中的任务，然后判断其父任务，以及触发时间是否满足，如果满足，则提交执行
		 * 因为判断其父任务和触发时间是否满足，会消耗一定的性能，目前调度的任务运行情况是： 0-9点任务比较多，而9点以后任务比较少，
		 * 所以这里设置了一个规则： 默认在0-9点，选取500个未成功，未运行的任务进行判断，9点以后，将数量减少到300个。
		 * 这个也可以由shell脚本传递过来
		 * 
		 * 
		 * 
		 */
		Integer[] simulateSelectMaxNumber = new Integer[] { 500, 300 };

		if (args.length > 0) {
			gateway = args[0];

			if (args.length == 2) {
				String[] token = args[1].split(",");
				int len = token.length;
				if (len > 0) {
					for (int i = 0; i < len; i++) {
						simulateSelectMaxNumber[i] = Integer.valueOf(token[i].trim());
					}
				}
			}
		}

		if (!StringUtils.hasText(gateway)) {
			log.error("gateway is requires.");
			return;
		}

		Configure.property(Configure.GATEWAY, gateway);
		Configure.property(Configure.SIMULATE_SELECT_MAX_NUMBERS, simulateSelectMaxNumber);

		SimulateScheduler.getSimulateScheduler().schedule();
	}

}
