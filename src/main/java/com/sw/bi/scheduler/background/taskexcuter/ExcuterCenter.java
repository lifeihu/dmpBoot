package com.sw.bi.scheduler.background.taskexcuter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;

import com.sw.bi.scheduler.background.exception.GatewayNotFoundException;
import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.model.Action;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.service.ActionService;
import com.sw.bi.scheduler.service.RedoAndSupplyHistoryService;
import com.sw.bi.scheduler.service.ScheduleSystemStatusService;
import com.sw.bi.scheduler.service.TaskService;
import com.sw.bi.scheduler.util.Configure;
import com.sw.bi.scheduler.util.Configure.ActionStatus;
import com.sw.bi.scheduler.util.DateUtil;

public class ExcuterCenter {
	private static final Logger log = Logger.getLogger(ExcuterCenter.class);
	private static ExcuterCenter m_instance;

	private TaskService taskService = BeanFactory.getService(TaskService.class);
	private ActionService actionService = BeanFactory.getService(ActionService.class);
	private RedoAndSupplyHistoryService redoAndSupplyHistoryService = BeanFactory.getService(RedoAndSupplyHistoryService.class);
	// private ConcurrentService concurrentService = BeanFactory.getService(ConcurrentService.class);
	// private BigDataTaskService bigDataTaskService = BeanFactory.getService(BigDataTaskService.class);

	/**
	 *  网关机上最大并发任务数
	 */
	private int maxThreadCount = 0;
	private String nowLogFolder;

	public static ExcuterCenter getInstance() throws GatewayNotFoundException {
		if (m_instance == null) {
			m_instance = new ExcuterCenter();
		}
		return m_instance;
	}

	private ExcuterCenter() throws GatewayNotFoundException {
		maxThreadCount = BeanFactory.getBean(ScheduleSystemStatusService.class).getTaskRunningMax(Configure.property(Configure.GATEWAY));
	}

	/**
	 * <pre>
	 * 添加需要执行的任务集合
	 * 说明：前面已经选取出来了两个任务集合：Collection<Task> normalTasks, Collection<Task> specialTasks
	 *       现在就是要根据每个网关机上面设定的最大任务并发数，优先选取特殊任务，以普通任务补满后，得到一个真正用于提交给执行中心的任务集合Collection<Task> executeTasks
	 * 
	 * 	一、特殊任务为空仍然走以前的逻辑
	 *  二、特殊任务不为空，则优先考虑特殊任务，如提交数量不足则以正常任务补充
	 *  	1、按并发配置中的分类计算出各分类的最大并发数、正在运行数和实际可提交数
	 *  	2、按上面计算所得的各数值筛选出各分类本次理论可提交的所有任务
	 *  	3、计算出当前网关机实际可提交数
	 *  	4、如果可提交的特殊任务大于“网关机实际提交数”时则按作业readyTime排除靠后的任务
	 *  		如果不足“网关机实际提交数”时将用正常任务进行补充
	 * </pre>
	 * 
	 * @param normalTasks
	 *            正常任务(正常轮循到的任务)
	 * @param specialTasks
	 *            特殊任务(根据作业类型并发配置表获得的任务)
	 * @return
	 * @throws GatewayNotFoundException
	 */
	public int addTasks(Collection<Task> normalTasks, Collection<Task> specialTasks) throws GatewayNotFoundException {
		if (specialTasks.size() == 0) {
			return this.addTasks(normalTasks, false);
		}

		// 获得当前的日志路径
		String logFolderPath = getNowLogFolder();

		/**
		 * 以特殊任务优先,生成本次轮循实际应该提交执行的任务
		 */
		// 优化： getExecuteTasks这个方法最后要废弃掉,里面计算不同类型任务的配额部分的代码,逻辑太复杂,当增加一个新的类型时,代码改动量太大了
		// 比如接下来要增加一个功能: 配置每个网关机上面某种jobtype任务的并发数
		// 优化逻辑: 先根据各自的逻辑,选取出符合条件的任务的集合,然后将这3个任务集合按照优先等级排序后,传给this.execute方法
		// 在this.execute方法中,依次判断任务是否允许提交,并遍历一个一个的提交任务,同时将每种类型的任务的已提交数记录下来
		// 当循环达到本次轮询可提交的数后,就中止掉循环,退出。 这样就去掉了多种任务集合之间配额的计算逻辑.后续如果增加新的集合类型,这部分的代码就不需要改动了
		Collection<Task> executeTasks = this.getExecuteTasks(specialTasks, normalTasks);

		// 开始执行任务
		this.execute(executeTasks, logFolderPath);

		return executeTasks.size();
	}

	/**
	 * 添加需要执行的task集合，如果集合过多超过最大线程，将只会运行前面一部分任务
	 * 
	 * @param normalTasks
	 *            集合按重要度排好序
	 * @param ignoreGatewayQuota
	 *            本次提交的任务是否忽略当前网关机的执行配额(true: 提交多少执行多少, false:
	 *            需要根据执行配额算出实际能提交的任务)
	 * @return 成功开始执行的task数
	 * @throws GatewayNotFoundException
	 */
	public int addTasks(Collection<Task> normalTasks, boolean ignoreGatewayQuota) throws GatewayNotFoundException {
		if (normalTasks.size() == 0) {
			return 0;
		}

		// 获得当前的日志路径
		String logFolderPath = getNowLogFolder();

		if (ignoreGatewayQuota) {
			// 不需要按网关机执行配额提交任务(一般用于前台模拟调度)
			log.info("忽略网关机(" + Configure.property(Configure.GATEWAY) + ")上的执行配额,直接提交 " + normalTasks.size() + " 条任务");
			return this.execute(normalTasks, logFolderPath);
		} else {
			// 需要按网关机执行配额提交任务
			return this.execute(this.getExecuteTasks(null, normalTasks), logFolderPath);
		}
	}

	/**
	 * 获取当前的日志路径 Parameters.logPath： /home/tools/logs/etl_log/ 日志路径：
	 * /home/tools/logs/etl_log/2012-09-04/
	 * 
	 * @return
	 */
	public String getNowLogFolder() {
		if (nowLogFolder == null) {
			String log_path = Parameters.logPath + DateUtil.format(new Date(), "yyyy-MM-dd");
			File log_p = new File(log_path);
			if (!log_p.exists()) {
				log_p.mkdirs();
			}
			this.nowLogFolder = log_path + "/";
		}
		return nowLogFolder;
	}

	/**
	 * <pre>
	 * 以特殊任务优先，生成本次轮循实际应该提交执行的任务
	 * 
	 * 1. 计算出该网关机上正在运行的action实例数
	 * 2. 根据当前正在运行的action实例数和该网关机配置的最大并发数，计算出还允许提交的任务数gatewayQuota
	 * 3. 如果当前正在运行的任务数已经超过最大并发数的75%，则本次暂不提交任务
	 * 4. 然后分多种可能的情况，优先选取特殊任务，如果特殊任务不足，则普通任务补上，选取出gatewayQuota个任务，准备提交执行。
	 *    特别说明： 在这个过程中，今日任务和历史任务的比例，可能就开始不准确了。 比如优先选取特殊任务后，用普通任务补全的时候，可能补全上去的都是历史的普通任务，导致本次提交的整个任务集合的今日任务与历史任务的比例已经变化过了
	 * <pre>
	 * 
	 * @param specialTasks
	 *            特殊任务(已经过滤过作业指定网关机)： 在下面代码中，凡是普通任务，在代码中判断了一下this.isNotGatewayExecute(gateway, task)
	 *                                              而特殊任务都没有做判断，那是因为之前判断过了
	 * @param normalTasks
	 * @return
	 * @throws GatewayNotFoundException
	 */
	private Collection<Task> getExecuteTasks(Collection<Task> specialTasks, Collection<Task> normalTasks) throws GatewayNotFoundException {
		String gateway = Configure.property(Configure.GATEWAY);
		Collection<Task> executeTasks = new ArrayList<Task>();

		// 当前网关机所有正在运行的任务数量
		int runningCount = actionService.countTodayRunningTasks(gateway);

		// 当前网关机实际还允许提交的任务数(每次提交都不得大于该数值)
		int gatewayQuota = Math.max(this.maxThreadCount - runningCount, 0);

		if (runningCount > maxThreadCount * 0.75 || gatewayQuota == 0) {
			log.warn("在网关机(" + gateway + ")上,上次任务还有" + runningCount + "条正在运行.当前调度比较繁忙,本次暂不提交任务!");
			return executeTasks;
		}

		String logger = "在网关机(" + gateway + ")上：最大并发 " + this.maxThreadCount + " 条, 正在运行 " + runningCount + " 条, 还能提交 " + gatewayQuota + " 条";

		////////////////////////////////////////////////////////////////////////////////

		int normalCount = normalTasks == null ? 0 : normalTasks.size();
		int specialCount = specialTasks == null ? 0 : specialTasks.size();

		if (specialCount == 0) {
			int i = Math.min(gatewayQuota, normalCount);
			int removeNotGateway = 0;
			logger += ", 实际提交 " + i + " 条(正常任务 " + normalCount + " 条";

			Iterator<Task> iter = normalTasks.iterator();
			for (; iter.hasNext() && i > 0; i--) {
				Task task = iter.next();
				// String executeGateway = task.getGateway();

				// 某个任务，它的作业类型和任务尾号都符合网关机A的要求， 但是被指定到了B网关机上面执行，这个时候，根据上面的查询SQL逻辑，网关机A和网关机B都能选取到这个任务
				// 所以要在执行中心，提交执行之前，进一步根据任务的gateway字段来校验和判断
				if (this.isNotGatewayExecute(gateway, task)) {
					// 当前任务不应该在当前网关机上执行
					removeNotGateway += 1;
					continue;
				}

				executeTasks.add(task);
			}

			// log.info(logger + ", 排除正常任务 " + (Math.max(normalCount - gatewayQuota, 0) + " 条, 非本网关机的正常任务排除 " + removeNotGateway + " 条)"));
			log.info(logger + ", 排除正常任务 " + Math.max(normalCount - executeTasks.size(), 0) + " 条)"); // (含 " + removeNotGateway + " 条非本网关的正常任务)");
			return executeTasks;

		} else {
			if (specialCount == gatewayQuota) {
				log.info(logger + ", 实际提交(特殊任务 " + specialCount + " 条)");
				return specialTasks;
			}

			if (specialCount < gatewayQuota) {
				int removeNotGateway = 0;

				// 特殊任务未达到网关机本次实际允许提交的数量则需要用正常任务补充
				executeTasks.addAll(specialTasks);

				// 相差的数量从正常任务中补充过来
				normalCount = Math.min(gatewayQuota - specialCount, normalCount);
				if (normalCount > 0) {
					Iterator<Task> iter = normalTasks.iterator();
					for (int i = normalCount; iter.hasNext() && i > 0; i -= 1) {
						Task task = iter.next();
						// String executeGateway = task.getGateway();

						// if (StringUtils.hasText(executeGateway) && !executeGateway.equals(gateway)) {
						if (this.isNotGatewayExecute(gateway, task)) {
							// 当前任务不应该在当前网关机上执行
							removeNotGateway += 1;
							continue;
						}

						executeTasks.add(task);
					}
				}

				log.info(logger + ", 实际提交 " + executeTasks.size() + " 条(特殊任务 " + specialCount + " 条， 正常任务 " + normalCount + " 条)"); //, 非本网关机的正常任务排除 " + removeNotGateway + " 条");

			} else if (specialCount > gatewayQuota) {
				// 特殊任务超过网关机本次实际允许提交的数量则需要将readyTime时间靠后的任务删除(这时正常任务将被全部忽略)
				Iterator<Task> iter = specialTasks.iterator();
				for (int i = 0; iter.hasNext() && i < gatewayQuota; i++) {
					executeTasks.add(iter.next());
				}

				log.info(logger + ", 实际提交 " + executeTasks.size() + " 条(特殊任务 " + specialCount + " 条, 排除特殊任务 " + (specialCount - gatewayQuota) + " 条)");
			}
		}

		return executeTasks;
	}

	/**
	 * 校验指定任务是否不是在指定网关机上执行
	 * 
	 * @param gateway
	 * @param task
	 * @return
	 */
	private boolean isNotGatewayExecute(String gateway, Task task) {
		String executeGateway = task.getGateway();
		return StringUtils.hasText(executeGateway) && !executeGateway.equals(gateway);
	}

	/**
	 * <pre>
	 * 	在实际执行过程中偶尔会出现同一个任务在二次调度轮循中都被执行了，导致二个都失败
	 * 	
	 *  原因可能是第一个进程启动后由于服务器原因可能被挂起这类的，导致任务的状态可能还没能被改成“正在运行”，
	 * 	仍处理“已触发”状态，所以该任务又被第二次调度轮循给获取并执行
	 * 	
	 *  处理：将进程中修改状态的操作拿出来，先做修改，使在该任务已处于运行状态后再扔给执行器执行
	 * 
	 *  2014-04-23
	 *  
	 *  以上情况是以前发生的现象，但未真正查出根源，只是临时的观察的解决方法
	 *  
	 *  生产现象：但今年随着任务量的急剧上涨，问题暴露出越来越多的问题，最典型就是模拟后台的操作始终不能百分之百模拟起来
	 *  总是会出选取的任务只模拟起了几个，其余都变成“已触发”状态，但是任务是“已触发”状态，Action却是运行中，
	 *  这就导致Action执行完成后任务状态无法被回填，下次后台轮循时这些“已触发”任务有可能再次被执行到，于是Action
	 *  中又多了一条，如果任务执行时间比较长的可能会出现一个任务有二个进程一起在跑
	 *  
	 *  调试现象1：经过跟踪调度后发现一个现象，就是所有被模拟起来的任务从代码逻辑层面看完成没问题，但实际效果就是明明在
	 *  	Session中状态都改成“运行中”了，但一直未被持久化到数据库，另一现象也很奇怪，任务状态一直不能被持久化到数据库
	 *  	可Action却能成功创建并置为了“运行中”。
	 *  原因判定：初步判断上面的现象可能是由Spring的多线程事务导致的。Spring事务是线程安全的，但是任务执行的地方却将
	 *  	每一个任务执行器丢给一个线程去处理，这时主进程的事务与每个子进程中的事务已经是独立的了。
	 *  
	 *  调试现象2. 带着上面疑问再继续跟踪到Spring事务相关的源码中，发现主进程(即TaskService.simulateSchedule)执行过程
	 *  	中，当子进程被创建执行时是没有等待的，所以所有子进程创建完后主进程马上就结束了，这时主进程事务也随之关闭。但
	 *  	每个子进程仍然在运行，事务也分别开启着
	 *  原因判定：结合调试现象1、调试现象2再去分析为什么任务状态一直不能被持久化到数据库而Action可以，在代码中发现下面一行
	 *  	final Task executeTask = taskService.get(iter.next().getTaskId());
	 *  	感觉这是在主进程中从数据库获取了最新的任务信息，然后分别扔给了各自的子进程，给我的感觉是这个任务应该是用的是
	 *  	主进程的Connection，问题就出在这里，主进程结束这些任务应该就不能被操作，所以导致任务状态不能被修改，Action是
	 *  	在子进程中创建起来的所以没什么影响仍能继续保存，最终就产生了任务状态保存失败，Action成功保存的现象（但这只是我
	 *  	个人如此认为，事实可能也并非完全如此，但以下的处理方式还是基于了这种原因去修改的）
	 *  
	 *  解决方法：
	 *  	1. final Task executeTask = iter.next(); // 这里不拿任务数据，直接扔到子进程
	 *  
	 *  	2. public AbExcuter(Task task, String logFolder) {
	 * 			this.executeTask = taskService.get(task.getTaskId()); // 由子进程的构造函数中通过Service从数据库拿最新信息
	 * 			...
	 * 		}
	 * 
	 * 		3. 以前的临时解决方法中将“运行中”状态的修改移到了任务执行前，但感觉这个没必要，所以这次又移回到了TaskService.runBegin方法中
	 * 
	 * 		4. 以上三步改完后每次模拟13个任务，一共模拟几十次每次都成功。但发现其他一个小问题，就是当提示模拟成功后再看列表
	 * 		会发现有一些任务状态仍是“未运行”，这时实际数据库已经更新，只是页面刷新的太快，数据库中还来不及都变更过来，所以
	 * 		TaskModule.simulateSchedule方法中重刷任务的方法延迟了800毫秒，暂时没再出现问题， 如果以后生产上发现还是有问题可以手工刷新一下
	 * 
	 * 	2014-04-24
	 * 
	 * 	生产测试：
	 * 		通过上面的解决方法在实际生产环境中测试发现该方法只是大幅提升了模拟后台的成功率，并没有根本性的解决问题
	 * 
	 * 	现象分析
	 * 		退一步分析在TaskService.simulateSchedule中修改状态是百分百成功的，所以可以将原先执行器的预处理方法AbExcuter.prepare迁移到主进程中
	 * 
	 * 	解决方法：
	 * 		以前执行的逻辑是遍历每个执行任务，并且分别为每个任务创建一个子进程执行，每个子进程中各自去修改任务“运行中”状态和创建执行Action
	 * 		新的逻辑是对执行任务先批量创建好执行器，在这个过程已经对任务状态和执行Action有判断了，如果某个任务在创建时失败则会忽略（不会影响
	 * 		其他任务的继续创建）。等所有执行器创建完成后再分别为这些执行器创建子进程进行执行
	 * 
	 * 	生产测试
	 * 		采用这种解决方法虽然未运行的情况解决了，但出现对于执行很快的作业（如虚拟作业、Shell作业等）回填成功率非常低
	 * </pre>
	 * 
	 * @param executeTasks
	 * @param logFolderPath
	 * @return
	 */
	@Deprecated
	private int execute1(Collection<Task> executeTasks, final String logFolderPath) {
		if (executeTasks.size() == 0) {
			return 0;
		}

		// 为需要执行的任务创建执行器
		Collection<AbExcuter> excuters = this.createTaskExcuters(executeTasks, logFolderPath);
		int excuterCount = excuters.size();

		if (excuterCount == 0) {
			return 0;
		}

		for (final AbExcuter excuter : excuters) {
			Thread t = new Thread() {
				public void run() {
					try {
						excuter.excute();

					} catch (Exception ex) {
						ex.printStackTrace();
						log.error(ex.getMessage());
					} finally {

					}
				}
			};

			synchronized (t) {
				t.start();
			}
		}

		nowLogFolder = null;

		return excuterCount;
	}

	
	/**
	 * 真正提交执行任务的地方
	 * @param executeTasks
	 * @param logFolderPath
	 * @return
	 */
	private int execute(Collection<Task> executeTasks, final String logFolderPath) {
		if (executeTasks.size() == 0) {
			return 0;
		}

		// 下面的日志信息仅为调试
		log.info("以下是被放置至执行中心待执行的任务清单:");
		for (Task executeTask : executeTasks) {
			log.info("[等待执行]" + executeTask);
		}

		Iterator<Task> iter = executeTasks.iterator();
		for (; iter.hasNext();) {
			final Task executeTask = iter.next();
			int jobType = executeTask.getJobType().intValue();

			/**
			 * <pre>
			 * 	2014-07-08
			 * 	依赖检查类型的作业的特殊性，在创建时就已经是“已触发”状态
			 * 	被执行后执行器中会轮循校验父任务的完成情况，直至所有父任务都成功为止
			 * 	但现在发现由于初始就是“已触发”状态所以被执行起来到所有父任务
			 * 	都校验成功后的这段时间会非常长，经常会出现告警，其实这个告警
			 * 如果始终是参考点方式调度的话不会告警，因为虽然时间长但一直是长的
			 * 但模拟方式由于在执行前还有一步校验会将该类型作业排除在执行
			 * 队列外所以它实际执行的时间就比较短，这样如果先用模拟方式再
			 * 切换至参考点方式就会导致超时告警
			 * 这其实对数据没有任务影响只是ETL经常收到告警比较烦（其实七天后也不会告了）
			 * </pre>
			 */
/*			if (jobType == JobType.CHECK_DAY_DEPENDENCY_HOUR.indexOf() || jobType == JobType.CHECK_MONTH_DEPENDENCY_DAY.indexOf()) {
				try {
					taskService.isParentTaskRunSuccess(executeTask);
				} catch (Warning e) {
					log.info(e.getMessage());
					continue;
				}
			}*/

			if (actionService.hasRunningActionByTaskAndScanDate(executeTask.getTaskId(), DateUtil.getToday())) {
				log.info(executeTask + "已经有正在运行的Action了,忽略本次执行");
				iter.remove();
				continue;
			}

			// taskService.evict(executeTask);

			new Thread() {
				public void run() {
					try {
						AbExcuter excuter = ExcuterFactory.getExcuterByJobType(executeTask, logFolderPath);
						excuter.excute();

					} catch (Exception ex) {
						ex.printStackTrace();
						log.error(ex.getMessage());
					} finally {

					}
				}
			}.start();
		}

		/**
		 * <pre>
		 * 	TODO 需要考虑一下是不是需要再加一步执行任务的反查
		 * 	现在的解决方案并未从根据上解决任务未运行Action已经运行
		 * 	的情况，而且还有偶发性的无法回填任务的问题（机率非常小）
		 * 	如果忽略回填的问题可以加执行任务的反查，但如果要将回填
		 * 	问题考虑进去就不能反查了，因为反查动作是将Action已经启动
		 * 	任务是已触发的改成运行中，所以如果没有回填，任务又被置
		 * 	成运行中，则会导致该任务一直不能被调起来
		 * </pre>
		 */

		nowLogFolder = null;

		return executeTasks.size();
	}

	/**
	 * 创建任务执行器
	 * 
	 * @param executeTasks
	 * @return
	 */
	private Collection<AbExcuter> createTaskExcuters(Collection<Task> executeTasks, final String logFolderPath) {
		Collection<AbExcuter> excuters = new ArrayList<AbExcuter>();

		for (Task executeTask : executeTasks) {
			try {
				// 将任务状态先设置为运行状态,然后进行反查,如果反查成功,则表示该任务可以真正被执行了
				if (!taskService.runBegin(executeTask)) {
					log.warn(executeTask + ",在修改“运行中”状态过程未通过反查而被忽略执行");
					continue;
				}

				// 为任务创建一条执行明细
				Action executeAction = actionService.create(executeTask);
				executeAction.setActionLog(logFolderPath);
				executeAction.setStartTime(DateUtil.now());
				executeAction.setActionStatus(ActionStatus.RUNNING.indexOf());
				executeAction.setGateway(Configure.property(Configure.GATEWAY));
				executeAction.setUpdateTime(DateUtil.now());
				actionService.saveOrUpdate(executeAction);
				actionService.flush();

				// 需要将新建的ActionID回填到Task表
				executeTask.setLastActionId(executeAction.getActionId());
				taskService.update(executeTask);
				taskService.flush();

				// 回填重跑/补数据操作历史中的数据
				redoAndSupplyHistoryService.taskRunBegin(executeTask);

				taskService.evict(executeTask);
				actionService.evict(executeAction);

				AbExcuter excuter = ExcuterFactory.getExcuterByJobType(executeTask, logFolderPath);
				excuter.setExecuteAction(executeAction);
				excuters.add(excuter);

				log.info("提交到执行队列: " + executeTask);

			} catch (Exception e) {
				e.printStackTrace();

				log.error("忽略" + executeTask + "的执行,因为在创建执行器失败(" + e.getMessage() + ")");
				continue;
			}
		}

		return excuters;
	}
}
