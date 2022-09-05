package com.sw.bi.scheduler.background.main;

import java.io.IOException;
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
import com.sw.bi.scheduler.service.GatewaySchedulerService;
import com.sw.bi.scheduler.service.GatewayService;
import com.sw.bi.scheduler.service.ScheduleSystemStatusService;
import com.sw.bi.scheduler.service.SchedulerExecTimeService;
import com.sw.bi.scheduler.service.TaskCreateLogService;
import com.sw.bi.scheduler.util.Configure;
import com.sw.bi.scheduler.util.DateUtil;

import framework.commons.sender.MessagePlatform;
import com.sw.bi.scheduler.supports.MessageSenderAssistant;

/**
 * 调度入口类
 * 
 * @author shiming.hong
 */
// Transaction rolled back because it has been marked as rollback-only 
// 现象：重跑作业的时候提示以上错误信息
// 可能的原因： 检查一下参考点表，看看有没有重复task_id的记录，如果有删除掉一个重复参考点即可
@Component
public class Scheduler {
	private static final Logger log = Logger.getLogger(Scheduler.class);

	@Autowired
	private SchedulerService schedulerService;

	@Autowired
	private TaskCreateLogService taskCreateLogService;

	@Autowired
	private SchedulerExecTimeService schedulerExecTimeService;

	@Autowired
	private GatewayService gatewayService;

	@Autowired
	private GatewaySchedulerService gatewaySchedulerService;

	@Autowired
	private ScheduleSystemStatusService scheduleSystemStatusService;

	/*@Autowired(required = false)
	// @Qualifier("SWSmsSender")
	@Qualifier("WebSmsSender")
	private SmsSender smsSender;*/

	/*@Autowired
	private SmsService smsService;*/
	private MessageSenderAssistant messageSender = new MessageSenderAssistant();

	public static Scheduler getScheduler() {
		return BeanFactory.getBean(Scheduler.class);
	}

	private Scheduler() {}

	public void schedule() {
		Date today = DateUtil.getToday();

		String gateway = Configure.property(Configure.GATEWAY);
		String mobile = Configure.property(Configure.SMS_MOBILE);

		// 当前网关机的前提执行条件是否已经准备完成
		boolean isExecuteConditionPrepared = true;

		log.info("-----------------------------------------------------------------------------------------------");
		log.info(gateway + " gateway is executing scheduler.");

		try {
			boolean isMainScheduler = scheduleSystemStatusService.isMasterGateway(gateway);
			Configure.property(Configure.MAIN_SCHEDULER, isMainScheduler);

			// 需要等前一次调度过程结束后方可继续
			if (!schedulerExecTimeService.isFinished()) {

				// 根据时间重置调度程序的完成状态
				if (schedulerExecTimeService.refinished(false)) {
					String msg = "由于上次调度主程序(" + gateway + ")执行失败后已超过" + Configure.property(Configure.UPDATE_SCHEDULER_FINISHED_PERSIST_MINUTE, Integer.class) + "分钟，系统已自动重置为完成状态.";
					log.info(msg);
					// SmsAlert.sendSms(msg, mobile, smsKey);
					// smsService.sendMsg(mobile, msg);
//					messageSender.sendSms(mobile, msg);
					messageSender.send(MessagePlatform.SMS_ADTIME,mobile, msg);

				} else {
					isExecuteConditionPrepared = false;
					log.info("previous scheduler has not finished...");
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
				schedulerService.createTasks(today);
			}

			schedulerService.schedule(today);

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
		message.append("网关机(").append(gateway).append(")调度主程序异常");

		// 捕获事务锁超时异常
		if (e.getMessage().indexOf("Lock wait timeout exceeded; try restarting transaction") > -1) {
			e = new TransactionLockTimeoutException(e);
		}

		SchedulerException se = null;
		if (e instanceof SchedulerException) {
			se = (SchedulerException) e;

			// 目前只对已知的调度异常作出原因告警
			if (!(e instanceof UnknownSchedulerException)) {
				message.append("[原因(#").append(se.getErrorCode()).append(")： ").append(e.getMessage()).append("]");
			}
		}/* else {
			se = new UnknownSchedulerException(e);
			}*/

		log.error(message.toString());
		// smsService.sendMsg(mobile, message.toString());
//		messageSender.sendSms(mobile, message.toString());
		messageSender.send(MessagePlatform.SMS_ADTIME,mobile, message.toString());
	}

	public static void main(String[] args) {
		String gateway = null;

		
		// 本地测试
//					String[] cmd = { "cmd ", "/c", "datax.py /home/tools/temp/newdataxtemp/20220829/15.json > /home/tools/logs/etl_log/2022-08-29/15.log 2>&1" };
//					// 把每个元素按次序拼接转成字符串
//					try {
//						Process process = Runtime.getRuntime().exec(cmd);
//						try {
//							process.waitFor();
//							System.out.println(process.exitValue()); 
//						} catch (InterruptedException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
					
					
		if (args.length > 0) {
			gateway = args[0];

			// 命令行参数屏蔽了第二个尾号参数，直接由网关机表中获取
			/*if (args.length == 2) {
				Configure.property(Configure.TASK_RANGE, args[1]);
			}*/
		}

		
		if (!StringUtils.hasText(gateway)) {
			log.error("gateway is requires.");
			gateway = "web1";
//			return;
		}

		Configure.property(Configure.GATEWAY, gateway);
		// Configure.property(Configure.MAIN_SCHEDULER, "scheduler".equals(gateway));

		Scheduler.getScheduler().schedule();
	}
}

//     多服务器版
//     ./scheduler jar scheduler.jar com.sw.bi.scheduler.background.main.Scheduler scheduler 
//     ./scheduler jar scheduler.jar com.sw.bi.scheduler.background.main.Scheduler scheduler2 
//     第一个参数表示gateway的标记;
//     表schedule_system_status中的gateway字段用来表明是哪个服务器的配置信息
//     表scheduler_exec_time和scheduler_exec_time_history中的flag,记录的是服务器的标记。用来控制每个服务器最多同时运行1个调度主程序。
//     表action中的gateway用来表明该action实例是在哪个服务器上执行的,日志文件是分散在各服务器上的。在哪个服务器上执行程序，日志就保存在哪个服务器上。所以要记录在action表中
//  调度多服务器版:
//  hive:每个网关部署一个
//  程序脚本:每个网关部署一份
//  调度程序:每个网关部署一个
//  datax工具也要每个网关部署一个,这样才能在其他网关执行datax任务.
//  日志分散在各网关机上
