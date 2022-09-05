package com.sw.bi.scheduler.util;

import java.io.IOException;
import java.util.Properties;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.log4j.Logger;

public class Configure {
	private static final Logger log = Logger.getLogger(Configure.class);

	private static final String CONFIGURE_PROPERTIES = "scheduler.properties";

	// 执行调度程序的服务器标识
	public static final String GATEWAY = "scheduler.gateway";

	// 按模拟方式调度时各时间段最大选取数量
	public static final String SIMULATE_SELECT_MAX_NUMBERS = "simulate.select.max.number";

	// 服务器选取可执行任务的ID选取范围
	public static final String TASK_RANGE = "scheduler.task.range";

	// 当前调试程序是否是有主服务器执行的
	public static final String MAIN_SCHEDULER = "main.scheduler";

	// 能同时运行的任务的最大数量
	// public static final String TASK_RUNNING_MAX = "task.running.max";

	// 在根据作业创建任务时每多少条作业提交一次
	public static final String JOB_COMMIT_COUNT = "job.commit.count";

	// 持续指定分钟后自动将调度程序未完成改成完成状态
	public static final String UPDATE_SCHEDULER_FINISHED_PERSIST_MINUTE = "update.scheduler.finished.persist.minute";

	// 一次选取需要被修改为未触发状态的任务数量
	// public static final String WAIT_UPDATE_STATUS_TASK_COUNT = "wait.update.status.task.count";

	// 选取今天需要被修改为未触发状态的任务的百分比
	public static final String TODAY_WAIT_UPDATE_STATUS_TASK_SELECT_PERCENT = "today.wait.update.status.task.select.percent";

	// 导出至MySQL作业的最大运行数
	// public static final String MYSQL_RUNNING_MAX_COUNT = "mysql.running.max.count";

	// 作业下线需要被通知的邮箱
	public static final String JOB_OFFLINE_MAILS = "job.offline.mails";

	public static final String JOB_NOTICE_EMAIL = "job.notice.email";
	
	public static final String SENDER_DEFAULT_SMS_PLATFORM = "sender.default.sms.platform";
	
	
	// 短信接口提供方(sw: 短信平台, web: 中国网建)
	public static final String SMS_PROVIDER = "sms.provider";

	// 短信配置
	public static final String SMS_KEY = "sms.key";
	public static final String SMS_MOBILE = "sms.mobile";

	// 邮件配置
	public static final String MAIL_SMTP_HOST = "mail.smtp.host";
	public static final String MAIL_TRANSPORT_PROTOCOL = "mail.transport.protocol";
	public static final String MAIL_SMTP_AUTH = "mail.smtp.auth";
	public static final String MAIL_AUTH_USERNAME = "mail.auth.username";
	public static final String MAIL_AUTH_PASSWORD = "mail.auth.password";
	public static final String MAIL_FROM = "mail.from";

	// HIVE数据源
	public static final String HIVE_DATABASE_CONNECTION_DRIVER_CLASS = "hive.database.connection.driver_class";
	public static final String HIVE_DATABASE_CONNECTION_URL = "hive.database.connection.url";
	public static final String HIVE_DATABASE_CONNECTION_USERNAME = "hive.database.connection.username";
	public static final String HIVE_DATABASE_CONNECTION_PASSWORD = "hive.database.connection.password";

	// Hive版本
	public static final String HIVE_VERSION = "hive.version";
	public static final String HIVE_0_7 = "hive.version.0.7";
	public static final String HIVE_0_9 = "hive.version.0.9";
	public static final String HIVE_1_1 = "hive.version.1.1";

	// 报表监控中需要的hive连接地址(该配置移入gateway表的hiveJdbc字段)
	// public static final String REPORT_QUALITY_HIVE_CONNECTION_URL = "report.quality.hive.connection.url";

	// Gateway服务器(使用gateway表替代)
	/*public static final String GATEWAY_SCHEDULER_HOST = "gateway.scheduler.host";
	public static final String GATEWAY_SCHEDULER_PORT = "gateway.scheduler.port";
	public static final String GATEWAY_SCHEDULER_USERNAME = "gateway.scheduler.username";
	public static final String GATEWAY_SCHEDULER_PASSWORD = "gateway.scheduler.password";

	public static final String GATEWAY_SCHEDULER2_HOST = "gateway.scheduler2.host";
	public static final String GATEWAY_SCHEDULER2_PORT = "gateway.scheduler2.port";
	public static final String GATEWAY_SCHEDULER2_USERNAME = "gateway.scheduler2.username";
	public static final String GATEWAY_SCHEDULER2_PASSWORD = "gateway.scheduler2.password";*/

	// hive数据库及地址(用于FtpExcuter)
	public static final String HIVE_DATABASE = "hive.database";
	public static final String HIVE_DATABASE_PATH = "hive.database.path";

	// Hudson同步配置
	public static final String HUDSON_SYNC_SOURCE_PATH = "hudson.sync.source.path";
	public static final String HUDSON_SYNC_SOURCE_PATH_USERNAME = "hudson.sync.source.path.username";
	public static final String HUDSON_SYNC_SOURCE_PATH_PASSWORD = "hudson.sync.source.path.password";

	public static final String HUDSON_SYNC_TARGET_PATH = "hudson.sync.target.path";
	public static final String HUDSON_SYNC_TEMP_PATH = "hudson.sync.temp.path";

	@Deprecated
	public static final String HUDSON_SYNC_TEMP_HADOOP_PATH = "hudson.sync.temp.hadoop.path";

	@Deprecated
	public static final String HUDSON_SYNC_GANGRONG_SOURCE_PATH = "hudson.sync.ganrong.source.path";
	@Deprecated
	public static final String HUDSON_SYNC_DAIJIN_SOURCE_PATH = "hudson.sync.daijin.source.path";
	@Deprecated
	public static final String HUDSON_SYNC_MR_SOURCE_PATH = "hudson.sync.mr.source.path";
	@Deprecated
	public static final String HUDSON_SYNC_UDF_SOURCE_PATH = "hudson.sync.udf.source.path";
	@Deprecated
	public static final String HUDSON_SYNC_PY_SOURCE_PATH = "hudson.sync.py.source.path";

	@Deprecated
	public static final String HUDSON_SYNC_GANGRONG_TARGET_PATH = "hudson.sync.ganrong.target.path";
	@Deprecated
	public static final String HUDSON_SYNC_DAIJIN_TARGET_PATH = "hudson.sync.daijin.target.path";
	@Deprecated
	public static final String HUDSON_SYNC_MR_TARGET_PATH = "hudson.sync.mr.target.path";
	@Deprecated
	public static final String HUDSON_SYNC_UDF_TARGET_PATH = "hudson.sync.udf.target.path";
	@Deprecated
	public static final String HUDSON_SYNC_PY_TARGET_PATH = "hudson.sync.py.target.path";

	// 财务预算表数据源
	public static final String BUDGET_DATABASE_CONNECTION_DRIVER_CLASS = "budget.database.connection.driver_class";
	public static final String BUDGET_DATABASE_CONNECTION_URL = "budget.database.connection.url";
	public static final String BUDGET_DATABASE_CONNECTION_USERNAME = "budget.database.connection.username";
	public static final String BUDGET_DATABASE_CONNECTION_PASSWORD = "budget.database.connection.password";

	// 调度后台执行日志所有目录
	public static final String SCHEDULER_BACKGROUND_LOG_PATH = "scheduler.background.log.path";

	// Hadoop安装目录
	public static final String HADOOP_HOME = "hadoop.home";

	// Hive安装目录
	public static final String HIVE_HOME = "hive.home";

	// hundson打包文件的发布网关机
	@Deprecated
	public static final String HUDSON_DISTRIBUTE_GATEWAY = "hudson.distribute.gateway";

	// 没有子任务作业ID清单
	public static final String UNCHILDREN_JOBS = "unchildren.jobs";

	// 妖神登录信息URL
	public static final String YS_LOGIN_INFO_URL = "ys.login.info.url";

	// 妖神登录信息临时存放目录
	public static final String YS_LOGIN_INFO_TEMP_PATH = "ys.login.info.temp.path";

	// GP数据源
	public static final String GREENPLUM_DATABASE_CONNECTION_DRIVER_CLASS = "greenplum.database.connection.driver_class";
	public static final String GREENPLUM_DATABASE_CONNECTION_URL = "greenplum.database.connection.url";
	public static final String GREENPLUM_DATABASE_CONNECTION_USERNAME = "greenplum.database.connection.username";
	public static final String GREENPLUM_DATABASE_CONNECTION_PASSWORD = "greenplum.database.connection.password";

	// 手工导出作业自动重跑的日志目录
	public static final String MANUAL_EXPORT_JOB_AUTO_REDO_LOGPATH = "manual.export.job.auto.redo.log.path";

	// 手机验证码有效时间(小时)
	public static final String VERTIFY_CODE_TIMEOUT = "vertify.code.timeout";

	private static Properties configure = new Properties();

	static {
		try {
			configure.load(Configure.class.getClassLoader().getResourceAsStream(CONFIGURE_PROPERTIES));

			// property(TASK_RUNNING_MAX, Integer.valueOf(property(TASK_RUNNING_MAX)));
			property(JOB_COMMIT_COUNT, Integer.valueOf(property(JOB_COMMIT_COUNT)));
			// property(WAIT_UPDATE_STATUS_TASK_COUNT, Integer.valueOf(property(WAIT_UPDATE_STATUS_TASK_COUNT)));
			property(TODAY_WAIT_UPDATE_STATUS_TASK_SELECT_PERCENT, Double.valueOf(property(TODAY_WAIT_UPDATE_STATUS_TASK_SELECT_PERCENT)));
			property(UPDATE_SCHEDULER_FINISHED_PERSIST_MINUTE, Integer.valueOf(property(UPDATE_SCHEDULER_FINISHED_PERSIST_MINUTE)));

			property(HIVE_0_7, HiveVersion.HIVE_0_7.value().equals(property(Configure.HIVE_VERSION)));
			property(HIVE_0_9, HiveVersion.HIVE_0_9.value().equals(property(Configure.HIVE_VERSION)));
			property(HIVE_1_1, HiveVersion.HIVE_1_1.value().equals(property(Configure.HIVE_VERSION)));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String property(String key) {
		return configure.getProperty(key);
	}

	@SuppressWarnings("unchecked")
	public static <T> T property(String key, Class<T> clazz) {
		Object value = configure.get(key);

		if (value == null) {
			return null;
		}

		Class<?> valueClass = value.getClass();

		if (valueClass.isAssignableFrom(clazz)) {
			return (T) value;
		}

		if (valueClass.isAssignableFrom(String.class)) {
			return (T) ConvertUtils.convert(value, clazz);
		}

		return null;
		// return (T) configure.get(key);
	}

	public static void property(String key, Object value) {
		configure.put(key, value);
	}

	public static void remove(String key) {
		configure.remove(key);
	}

	////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * 调度系统状态
	 */
	public enum SchedulerSystemStatus {
		CLOSE, // 关闭
		OPEN; // 开启

		public int indexOf() {
			return this.ordinal();
		}

		public static SchedulerSystemStatus valueOf(int index) {
			return Configure.valueOf(SchedulerSystemStatus.class, index);
		}
	}

	/**
	 * 是否参照作业优先级
	 */
	public enum ReferJobLevel {
		NO, // 不参照作业优先级
		YES; // 参照作业优先级

		public int indexOf() {
			return this.ordinal();
		}

		public static SchedulerSystemStatus valueOf(int index) {
			return Configure.valueOf(SchedulerSystemStatus.class, index);
		}
	}

	/**
	 * 是否开始负载平衡
	 */
	public enum SchedulerSystemBalance {
		CLOSE, // 开启负载平衡
		OPEN; // 开启负载平衡

		public int indexOf() {
			return this.ordinal();
		}

		public static SchedulerSystemStatus valueOf(int index) {
			return Configure.valueOf(SchedulerSystemStatus.class, index);
		}
	}

	/**
	 * 作业状态
	 * 
	 * @author shiming.hong
	 */
	public enum JobStatus {
		UN_LINE, // 未上线
		ON_LINE, // 已上线
		OFF_LINE; // 已下线

		public int indexOf() {
			return this.ordinal();
		}

		public static JobStatus valueOf(int index) {
			return Configure.valueOf(JobStatus.class, index);
		}
	}

	/**
	 * 作业运行周期
	 * 
	 * @author shiming.hong
	 * 
	 */
	public enum JobCycle {
		MONTH(1), // 月
		WEEK(2), // 周
		DAY(3), // 天
		HOUR(4), // 小时
		MINUTE(5), // 分钟
		NONE(6); // 无特定周期,该周期只应用于分支作业类型

		private final int index;

		private JobCycle(int index) {
			this.index = index;
		}

		public int indexOf() {
			return index;
		}

		public static JobCycle valueOf(int index) {
			JobCycle[] values = JobCycle.values();
			for (JobCycle value : values) {
				if (value.indexOf() == index) {
					return value;
				}
			}

			return null;
		}

		public static String toString(int index) {
			if (index == 1) {
				return "月";
			} else if (index == 2) {
				return "周";
			} else if (index == 3) {
				return "天";
			} else if (index == 4) {
				return "小时";
			} else if (index == 5) {
				return "分钟";
			} else if (index == 6) {
				return "待触发";
			}

			return "未知";
		}
	}

	/**
	 * 作业类型
	 * 
	 * @author shiming.hong
	 */
	public enum JobType {
		// 数据同步
		ORACLE_TO_HDFS(1), // Oracle同步到HDFS
		MYSQL_TO_HDFS(2), // MySQL同步到HDFS
		SQLSERVER_TO_HDFS(3), // SQLServer同步到HDFS
		LOCAL_FILE_TO_HDFS(4), // 本地文件同步到HDFS

		// 如果以后扩展FTP类型则需要改一下ToolboxServiceImpl.restoreBackupFile
		FTP_FILE_TO_HDFS(5), // FTP文件同步到HDFS(需要成功标记)
		FTP_FILE_TO_HDFS_FIVE_MINUTE(6), // FTP文件同步到HDFS(间隔n分钟)
		FTP_FILE_TO_HDFS_YESTERDAY(7), // FTP文件同步到HDFS(不需要成功标记)
		
		GREENPLUM_FUNCTION(8), // GP函数
		PUT_TO_HDFS(9), // HDFS上传作业
		FILE_NUMBER_CHECK(10), // 文件数量校验作业 
		
		// add by zhuzhongji 2015年9月11日09:16:58
		FTP_FILE_TO_MYSQL(131),
		FTP_FILE_TO_SQLSERVER(132),
		FTP_FILE_TO_ORACLE(130),
		FTP_FILE_TO_LOCAL_FILE(133),
		FTP_FILE_TO_CSV(134),
		FTP_FILE_TO_GP(135),
		FTP_FILE_TO_FTP_FILE(136),
		FTP_FILE_TO_HBASE(137),
		FTP_FILE_TO_FTP_HDFS(138),



		// 计算任务
		HIVE_SQL(20), // HiveSQL计算
		MAPREDUCE(21), // Mapreduce计算

		// 导出任务
		HDFS_TO_ORACLE(30), // HDFS导出到Oracle 
		HDFS_TO_MYSQL(31), // HDFS导出到MySQL
		HDFS_TO_SQLSERVER(32), // HDFS导出到SQLServer
		HDFS_TO_LOCAL_FILE(33), // HDFS导出到本地文件
		HDFS_TO_HDFS(34), // HDFS导出到HDFS
		HDFS_TO_CSV(35), // HDFS导出到CSV
		HDFS_TO_GP(36), // HDFS导出到GP
		
		// add by zhuzhongji 2015年9月11日09:17:21
		HDFS_TO_FTP(37),
		HDFS_TO_HBASE(38),

		// 其他 
		SHELL(40), // Shell脚本
		PERL(41), // Perl脚本
		STORE_PROCEDURE(42), // 存储过程

		LOCAL_FILE_TO_ORACLE(50), // 本地文件导出到Oracle
		LOCAL_FILE_TO_MYSQL(51), // 本地文件导出到MySQL
		LOCAL_FILE_TO_SQLSERVER(52), // 本地文件导出到SQLServer
		LOCAL_FILE_TO_LOCAL_FILE(53), // 本地文件导出到本地文件
		LOCAL_FILE_TO_CSV(54), // 本地文件导出到CSV
		LOCAL_FILE_TO_GP(55), // 本地文件导出至GP
		
		// add by zhuzhongji 2015年9月11日09:30:09
		LOCAL_FILE_TO_FTP(56),
		LOCAL_FILE_TO_HBASE(57),

		MYSQL_TO_ORACLE(60), // MySQL导出到Oracle
		MYSQL_TO_MYSQL(61), // MySQL导出到MySQL
		MYSQL_TO_SQLSERVER(62), // MySQL导出到SQLServer
		MYSQL_TO_LOCAL_FILE(63), // MySQL导出到本地文件
		MYSQL_TO_CSV(64), // MySQL导出到CSV
		MYSQL_TO_GP(65), // MySQL导出到GP
		
		// add by zhuzhongji 2015年9月11日09:30:48
		MYSQL_TO_FTP(66),
		MYSQL_TO_HBASE(67),

		SQLSERVER_TO_ORACLE(70), // SQLServer导出到Oracle
		SQLSERVER_TO_MYSQL(71), // SQLServer导出到MySQL
		SQLSERVER_TO_SQLSERVER(72), // SQLServer导出到SQLServer
		SQLSERVER_TO_LOCAL_FILE(73), // SQLServer导出到本地文件
		SQLSERVER_TO_CSV(74), // SQLServer导出到CSV
		SQLSERVER_TO_GP(75), // SQLServer导出到GP
		
		// add by zhuzhongji 2015年9月11日09:17:48
		SQLSERVER_TO_FTP(76),
		SQLSERVER_TO_HBASE(77),

		ORACLE_TO_ORACLE(80), // Oracle导出到Oracle
		ORACLE_TO_MYSQL(81), // Oracle导出到MySQL
		ORACLE_TO_SQLSERVER(82), // Oracle导出到SQLServer
		ORACLE_TO_LOCAL_FILE(83), // Oracle导出到本地文件
		ORACLE_TO_CSV(84), // Oracle导出到CSV
		ORACLE_TO_GP(85), // Oracle导出到GP
		
		// add by zhuzhongji 2015年9月11日09:18:08
		ORACLE_TO_FTP(86),
		ORACLE_TO_HBASE(87),

		GP_TO_ORACLE(110), // GP导出到Oracle
		GP_TO_MYSQL(111), // GP导出到MySQL
		GP_TO_SQLSERVER(112), // GP导出到SQLServer
		GP_TO_LOCAL_FILE(113), // GP导出到本地文件
		GP_TO_CSV(114), // GP导出到CSV
		GP_TO_GP(115), // GP导出到GP
		GP_TO_HDFS(116), // GP导出HDFS
		
		// add by zhuzhongji 2015年9月11日09:18:34
		GP_TO_FTP(117),
		GP_TO_HBASE(118),

		CSV_TO_ORACLE(120), // GP导出到Oracle
		CSV_TO_MYSQL(121), // GP导出到MySQL
		CSV_TO_SQLSERVER(122), // GP导出到SQLServer
		CSV_TO_LOCAL_FILE(123), // GP导出到本地文件
		CSV_TO_CSV(124), // CSV导出到CSV
		CSV_TO_GP(125), // CSV导出到GP
		CSV_TO_HDFS(126), // GP导出HDFS
		
		// add by zhuzhongji 2015年9月11日09:18:52
		CSV_TO_FTP(127),
		CSV_TO_HBASE(128),
		
		// add by zhuzhongji 2015年9月11日09:19:24
		HBASE_TO_MYSQL(141),
		HBASE_TO_SQLSERVER(142),
		HBASE_TO_ORACLE(140),
		HBASE_TO_HDFS(146),
		HBASE_TO_LOCAL_FILE(143),
		HBASE_TO_CSV(144),
		HBASE_TO_GP(145),
		HBASE_TO_FTP(147),
		HBASE_TO_HBASE(148),

		
		//add by zhoushasha   2016/05/10 17:26:51 19种 
		MONGODB_TO_MYSQL(151),
		MONGODB_TO_SQLSERVER(152),
		 MONGODB_TO_ORACLE(150),
		 MONGODB_TO_HDFS(156),
		 MONGODB_TO_LOCAL_FILE(153),
		 MONGODB_TO_CSV(154),
		 MONGODB_TO_GP(155),
		 MONGODB_TO_FTP(157),
		 MONGODB_TO_HBASE(158),
		 MONGODB_TO_MONGODB(159),
		 MYSQL_TO_MONGODB(68),
		 SQLSERVER_TO_MONGODB(78),
		 ORACLE_TO_MONGODB(88),
		 FTP_TO_MONGODB(139),
		 HDFS_TO_MONGODB(39),
		 LOCAL_FILE_TO_MONGODB(58),
		 CSV_TO_MONGODB(129),
		 GP_TO_MONGODB(119),
		 HBASE_TO_MONGODB(149),
		 
		MAIL(90), // 邮件发送作业 
		REPORT_QUALITY(91), // 报表质量监控作业

		CHECK_DAY_DEPENDENCY_HOUR(92), // 检验天依赖小时
		CHECK_MONTH_DEPENDENCY_DAY(93), // 检验月依赖天

		
		
		VIRTUAL(100), // 虚拟作业
		DATAX_CUSTOM_XML(101), // 使用自定义XML配置文件
		BRANCH(102),// 分支作业 
		/*
		 * modify by qyx 定义一个为特殊标志的datax的shell执行模式id 
		 * 
		 */
		NEW_DATAX_SHELL(103),//使用新的模式执行datax任务
		/*
		 * modify by qyx 2018-11-29 17:37:54 定义一个调用python生成json的方式执行的模式id
		 */
		NEW_DATAX_PY(104),
		
		
		//add by mashifeng   2018/12/10 17:26:51 100种  新datax的JobType
		NEW_MYSQL_TO_MYSQL(2030),//MySQL导出到MySQL
		NEW_MYSQL_TO_SQLSERVER(2031),//MySQL导出到SQLServer
		NEW_MYSQL_TO_ORACLE(2032),//MySQL导出到Oracle
		NEW_MYSQL_TO_FTP(2033),//MySQL导出到FTP
		NEW_MYSQL_TO_HDFS(2034),//MySQL导出到HDFS
		NEW_MYSQL_TO_LOCAL_FILE(2035),//MySQL导出到本地文件
		NEW_MYSQL_TO_CSV(2036),//MySQL导出到CSV
		NEW_MYSQL_TO_GREENPLUM(2037),//MySQL导出到Greenplum
		NEW_MYSQL_TO_HBASE(2038),//MySQL导出到HBase
		NEW_MYSQL_TO_MONGODB(2039),//MySQL导出到MongoDb
		NEW_MYSQL_TO_SUNDB(2040),//MySQL导出到SUNDB
		
		
		NEW_SQLSERVER_TO_MYSQL(2130),//SQLServer导出到MySQL
		NEW_SQLSERVER_TO_SQLSERVER(2131),//SQLServer导出到SQLServer
		NEW_SQLSERVER_TO_ORACLE(2132),//SQLServer导出到Oracle
		NEW_SQLSERVER_TO_FTP(2133),//SQLServer导出到FTP
		NEW_SQLSERVER_TO_HDFS(2134),//SQLServer导出到HDFS
		NEW_SQLSERVER_TO_LOCAL_FILE(2135),//SQLServer导出到本地文件
		NEW_SQLSERVER_TO_CSV(2136),//SQLServer导出到CSV
		NEW_SQLSERVER_TO_GREENPLUM(2137),//SQLServer导出到Greenplum
		NEW_SQLSERVER_TO_HBASE(2138),//SQLServer导出到HBase
		NEW_SQLSERVER_TO_MONGODB(2139),//SQLServer导出到MongoDb
		NEW_SQLSERVER_TO_SUNDB(2140),//SQLServer导出到SUNDB

		NEW_ORACLE_TO_MYSQL(2230),//Oracle导出到MySQL
		NEW_ORACLE_TO_SQLSERVER(2231),//Oracle导出到SQLServer
		NEW_ORACLE_TO_ORACLE(2232),//Oracle导出到Oracle
		NEW_ORACLE_TO_FTP(2233),//Oracle导出到FTP
		NEW_ORACLE_TO_HDFS(2234),//Oracle导出到HDFS
		NEW_ORACLE_TO_LOCAL_FILE(2235),//Oracle导出到本地文件
		NEW_ORACLE_TO_CSV(2236),//Oracle导出到CSV
		NEW_ORACLE_TO_GREENPLUM(2237),//Oracle导出到Greenplum
		NEW_ORACLE_TO_HBASE(2238),//Oracle导出到HBase
		NEW_ORACLE_TO_MONGODB(2239),//Oracle导出到MongoDb
		NEW_ORACLE_TO_SUNDB(2240),//Oracle导出到SUNDB

		NEW_FTP_TO_MYSQL(2330),//FTP导出到MySQL
		NEW_FTP_TO_SQLSERVER(2331),//FTP导出到SQLServer
		NEW_FTP_TO_ORACLE(2332),//FTP导出到Oracle
		NEW_FTP_TO_FTP(2333),//FTP导出到FTP
		NEW_FTP_TO_HDFS(2334),//FTP导出到HDFS
		NEW_FTP_TO_LOCAL_FILE(2335),//FTP导出到本地文件
		NEW_FTP_TO_CSV(2336),//FTP导出到CSV
		NEW_FTP_TO_GREENPLUM(2337),//FTP导出到Greenplum
		NEW_FTP_TO_HBASE(2338),//FTP导出到HBase
		NEW_FTP_TO_MONGODB(2339),//FTP导出到MongoDb
		NEW_FTP_TO_SUNDB(2340),//FTP导出到SUNDB

		NEW_HDFS_TO_MYSQL(2430),//HDFS导出到MySQL
		NEW_HDFS_TO_SQLSERVER(2431),//HDFS导出到SQLServer
		NEW_HDFS_TO_ORACLE(2432),//HDFS导出到Oracle
		NEW_HDFS_TO_FTP(2433),//HDFS导出到FTP
		NEW_HDFS_TO_HDFS(2434),//HDFS导出到HDFS
		NEW_HDFS_TO_LOCAL_FILE(2435),//HDFS导出到本地文件
		NEW_HDFS_TO_CSV(2436),//HDFS导出到CSV
		NEW_HDFS_TO_GREENPLUM(2437),//HDFS导出到Greenplum
		NEW_HDFS_TO_HBASE(2438),//HDFS导出到HBase
		NEW_HDFS_TO_MONGODB(2439),//HDFS导出到MongoDb
		NEW_HDFS_TO_SUNDB(2440),//HDFS导出到SUNDB

		NEW_LOCAL_FILE_TO_MYSQL(2530),//本地文件导出到MySQL
		NEW_LOCAL_FILE_TO_SQLSERVER(2531),//本地文件导出到SQLServer
		NEW_LOCAL_FILE_TO_ORACLE(2532),//本地文件导出到Oracle
		NEW_LOCAL_FILE_TO_FTP(2533),//本地文件导出到FTP
		NEW_LOCAL_FILE_TO_HDFS(2534),//本地文件导出到HDFS
		NEW_LOCAL_FILE_TO_LOCAL_FILE(2535),//本地文件导出到本地文件
		NEW_LOCAL_FILE_TO_CSV(2536),//本地文件导出到CSV
		NEW_LOCAL_FILE_TO_GREENPLUM(2537),//本地文件导出到Greenplum
		NEW_LOCAL_FILE_TO_HBASE(2538),//本地文件导出到HBase
		NEW_LOCAL_FILE_TO_MONGODB(2539),//本地文件导出到MongoDb
		NEW_LOCAL_FILE_TO_SUNDB(2540),//本地文件导出到SUNDB
		
		
		NEW_CSV_TO_MYSQL(2630),//CSV导出到MySQL
		NEW_CSV_TO_SQLSERVER(2631),//CSV导出到SQLServer
		NEW_CSV_TO_ORACLE(2632),//CSV导出到Oracle
		NEW_CSV_TO_FTP(2633),//CSV导出到FTP
		NEW_CSV_TO_HDFS(2634),//CSV导出到HDFS
		NEW_CSV_TO_LOCAL_FILE(2635),//CSV导出到本地文件
		NEW_CSV_TO_CSV(2636),//CSV导出到CSV
		NEW_CSV_TO_GREENPLUM(2637),//CSV导出到Greenplum
		NEW_CSV_TO_HBASE(2638),//CSV导出到HBase
		NEW_CSV_TO_MONGODB(2639),//CSV导出到MongoDb
		NEW_CSV_TO_SUNDB(2640),//CSV导出到SUNDB
		
		
		NEW_GP_TO_MYSQL(2730),//Greenplum导出到MySQL
		NEW_GP_TO_SQLSERVER(2731),//Greenplum导出到SQLServer
		NEW_GP_TO_ORACLE(2732),//Greenplum导出到Oracle
		NEW_GP_TO_FTP(2733),//Greenplum导出到FTP
		NEW_GP_TO_HDFS(2734),//Greenplum导出到HDFS
		NEW_GP_TO_LOCAL_FILE(2735),//Greenplum导出到本地文件
		NEW_GP_TO_CSV(2736),//Greenplum导出到CSV
		NEW_GP_TO_GREENPLUM(2737),//Greenplum导出到Greenplum
		NEW_GP_TO_HBASE(2738),//Greenplum导出到HBase
		NEW_GP_TO_MONGODB(2739),//Greenplum导出到MongoDb
		NEW_GP_TO_SUNDB(2740),//Greenplum导出到SUNDB
		

		NEW_HBASE_TO_MYSQL(2830),//HBase导出到MySQL
		NEW_HBASE_TO_SQLSERVER(2831),//HBase导出到SQLServer
		NEW_HBASE_TO_ORACLE(2832),//HBase导出到Oracle
		NEW_HBASE_TO_FTP(2833),//HBase导出到FTP
		NEW_HBASE_TO_HDFS(2834),//HBase导出到HDFS
		NEW_HBASE_TO_LOCAL_FILE(2835),//HBase导出到本地文件
		NEW_HBASE_TO_CSV(2836),//HBase导出到CSV
		NEW_HBASE_TO_GREENPLUM(2837),//HBase导出到Greenplum
		NEW_HBASE_TO_HBASE(2838),//HBase导出到HBase
		NEW_HBASE_TO_MONGODB(2839),//HBase导出到MongoDb
		NEW_HBASE_TO_SUNDB(2840),//HBase导出到SUNDB
		
		
		NEW_MONGODB_TO_MYSQL(2930),//MongoDb导出到MySQL
		NEW_MONGODB_TO_SQLSERVER(2931),//MongoDb导出到SQLServer
		NEW_MONGODB_TO_ORACLE(2932),//MongoDb导出到Oracle
		NEW_MONGODB_TO_FTP(2933),//MongoDb导出到FTP
		NEW_MONGODB_TO_HDFS(2934),//MongoDb导出到HDFS
		NEW_MONGODB_TO_LOCAL_FILE(2935),//MongoDb导出到本地文件
		NEW_MONGODB_TO_CSV(2936),//MongoDb导出到CSV
		NEW_MONGODB_TO_GREENPLUM(2937),//MongoDb导出到Greenplum
		NEW_MONGODB_TO_HBASE(2938),//MongoDb导出到HBase
		NEW_MONGODB_TO_MONGODB(2939),	//MongoDb导出到MongoDb
		NEW_MONGODB_TO_SUNDB(2940),//MongoDb导出到SUNDB
				
		NEW_SUNDB_TO_MYSQL(3030),//MongoDb导出到MySQL
		NEW_SUNDB_TO_SQLSERVER(3031),//MongoDb导出到SQLServer
		NEW_SUNDB_TO_ORACLE(3032),//MongoDb导出到Oracle
		NEW_SUNDB_TO_FTP(3033),//MongoDb导出到FTP
		NEW_SUNDB_TO_HDFS(3034),//MongoDb导出到HDFS
		NEW_SUNDB_TO_LOCAL_FILE(3035),//MongoDb导出到本地文件
		NEW_SUNDB_TO_CSV(3036),//MongoDb导出到CSV
		NEW_SUNDB_TO_GREENPLUM(3037),//MongoDb导出到Greenplum
		NEW_SUNDB_TO_HBASE(3038),//MongoDb导出到HBase
		NEW_SUNDB_TO_MONGODB(3039),//MongoDb导出到MongoDb
		NEW_SUNDB_TO_SUNDB(3040);	//SunDb导出到SunDb
		
		private final int index;

		private JobType(int index) {
			this.index = index;
		}

		public int indexOf() {
			return index;
		}

		public static JobType valueOf(int index) {
			JobType[] values = JobType.values();
			for (JobType value : values) {
				if (value.indexOf() == index) {
					return value;
				}
			}

			return null;
		}

	}
	/**
	 * 来源data类型
	 * 
	 * @author 
	 */
	//使用新的模式执行datax任务
	public enum SourceDataTypes {
		
		MYSQL_READER(20,"mysqlreader"),
		SQLSERVER_READER(21,"sqlserverreader"),
		ORACLE_READER(22,"oraclereader"),
		FTP_READER(23,"ftpreader"),
		HDFS_READER(24,"hdfsreader"),
		FILE_READER(25,"filereader"),
		CSV_READER(26,"csvreader"),
		POSTGRESQL_READER(27,"postgresqlreader"),
		MONGODB_READER(29,"mongodbreader"),
		SUNDB_READER(30,"sundbreader");

		private  long key;
		private  String value;
		
		private SourceDataTypes(int key, String value) {
			this.key = key;
			this.value = value;
		}
		
		public long getKey() {
			return key;
		}
		
		public String getValue() {
			return value;
		}
		
		public Long indexOf() {
			return key;
		}
		
		public static SourceDataTypes valueOf(long key) {
			SourceDataTypes[] values = SourceDataTypes.values();
			for (SourceDataTypes value : values) {
				if (value.indexOf() == key) {
					return value;
				}
			}

			return null;
		}
	}
	/**
	 * 目标源data类型
	 * 
	 * @author 
	 */
	public enum TargetDataTypes {
		
		MYSQL_WRITER(30,"mysqlwriter"),
		SQLSERVER_WRITER(31,"sqlserverwriter"),
		ORACLE_WRITER(32,"oraclewriter"),
		FTP_WRITER(33,"ftpwriter"),
		HDFS_WRITER(34,"hdfswriter"),
		FILE_WRITER(35,"filewriter"),
		CSV_WRITER(36,"csvwriter"),
		POSTGRESQL_WRITER(37,"postgresqlwriter"),
		MONGODB_WRITER(39,"mongodbwriter"),
		SUNDB_WRITER(40,"sundbwriter");

		private  long key;
		private  String value;
		
		private TargetDataTypes(int key, String value) {
			this.key = key;
			this.value = value;
		}
		
		public long getKey() {
			return key;
		}
		
		public String getValue() {
			return value;
		}
		
		public long indexOf() {
			return key;
		}
		
		public static TargetDataTypes valueOf(long key) {
			TargetDataTypes[] values = TargetDataTypes.values();
			for (TargetDataTypes value : values) {
				if (value.indexOf() == key) {
					return value;
				}
			}

			return null;
		}
	}

	/**
	 * 作业优先级
	 * 
	 * @author shiming.hong
	 */
	public enum JobLevel {
		LOWEST, // 预留0
		LOWER, // 预留1
		LOW, // 预留2

		TODAY, // 当天任务。只要当天能计算完成即可的任务，优先级较低
		BEFORE_NIGHT_18HOUR, // 晚18点任务。晚上18：00前必须完成的任务
		BEFORE_AFTERNOON_13HOUR, // 午13点任务。中午13点之前需要完成的任务
		BEFORE_MORNING_9HOUR, // 早9点任务。早上9点之前需要完成的任务
		HIGH_REPORT, // 高级报表。高层领导重点关注的业务数据报表
		FOREGROUND_INNER, // 前台。内部应用中所使用到的数据
		FOREGROUND_OUTER; // 前台。客户使用的产品中所使用到的数据

		public int indexOf() {
			return this.ordinal();
		}

		public static JobLevel valueOf(int index) {
			return Configure.valueOf(JobLevel.class, index);
		}

		public static String toString(int jobLevel) {
			if (TODAY.indexOf() == jobLevel) {
				return "lv3: 今天任务";
			} else if (BEFORE_NIGHT_18HOUR.indexOf() == jobLevel) {
				return "lv4: 晚18点任务";
			} else if (BEFORE_AFTERNOON_13HOUR.indexOf() == jobLevel) {
				return "lv5: 午13点任务";
			} else if (BEFORE_MORNING_9HOUR.indexOf() == jobLevel) {
				return "lv6: 早9点任务";
			} else if (HIGH_REPORT.indexOf() == jobLevel) {
				return "lv7: 高层报表";
			} else if (FOREGROUND_INNER.indexOf() == jobLevel) {
				return "lv8: 前台(对内)";
			} else if (FOREGROUND_OUTER.indexOf() == jobLevel) {
				return "lv8: 前台(对外)";
			}

			return "";
		}
	}

	/**
	 * 警告类型
	 * 
	 * @author shiming.hong
	 */
	public enum AlertType {
		ALERT_TO_SELF_DEPT, // 需要警告,只警告给本部门值班人员
		ALERT_TO_OTHER_DEPT, // 需要警告,只警告给其他部门 接口人
		NOT_ALERT, // 不需要警告
		PAUSE_ALERT; // 暂停警告

		public int indexOf() {
			return this.ordinal();
		}

		public static AlertType valueOf(int index) {
			return Configure.valueOf(AlertType.class, index);
		}
	}

	/**
	 * 任务标记
	 * 
	 * @author shiming.hong
	 */
	public enum TaskFlag {
		SUPPLY, // 补数据
		SYSTEM, // 系统自动生成的任务
		REDO, // 重跑任务
		ONLINE, // 新上线
		WEIGHT; // 加权

		public int indexOf() {
			return this.ordinal();
		}

		public static TaskFlag valueOf(int index) {
			return Configure.valueOf(TaskFlag.class, index);
		}
	}

	/**
	 * 任务状态
	 * 
	 * @author shiming.hong
	 */
	public enum TaskStatus {
		INITIALIZE, // 初始化状态 0
		WAIT_TRIGGER, // 未触发 1
		TRIGGERED, // 已触发 2
		RUNNING, // 运行中 3
		RUN_FAILURE, // 运行失败 4
		RUN_SUCCESS, // 运行成功 5
		RE_INITIALIZE, // 重做初始化 6
		RE_WAIT_TRIGGER, // 重做未触发 7
		RE_TRIGGERED, // 重做已触发 8
		RE_RUNNING, // 重做运行中 9
		RE_RUN_FAILURE, // 重做失败 10
		RE_RUN_SUCCESS; // 重做成功 11

		public int indexOf() {
			return this.ordinal();
		}

		public static TaskStatus valueOf(int index) {
			return Configure.valueOf(TaskStatus.class, index);
		}

		public static String toString(int taskStatus) {
			if (INITIALIZE.indexOf() == taskStatus) {
				return "初始化";
			} else if (WAIT_TRIGGER.indexOf() == taskStatus) {
				return "未触发";
			} else if (TRIGGERED.indexOf() == taskStatus) {
				return "已触发";
			} else if (RUNNING.indexOf() == taskStatus) {
				return "运行中";
			} else if (RUN_FAILURE.indexOf() == taskStatus) {
				return "运行失败";
			} else if (RUN_SUCCESS.indexOf() == taskStatus) {
				return "运行成功";
			} else if (RE_INITIALIZE.indexOf() == taskStatus) {
				return "重做初始化";
			} else if (RE_WAIT_TRIGGER.indexOf() == taskStatus) {
				return "重做未触发";
			} else if (RE_TRIGGERED.indexOf() == taskStatus) {
				return "重做已触发";
			} else if (RE_RUNNING.indexOf() == taskStatus) {
				return "重做运行中";
			} else if (RE_RUN_FAILURE.indexOf() == taskStatus) {
				return "重做失败";
			} else if (RE_RUN_SUCCESS.indexOf() == taskStatus) {
				return "重做成功";
			}

			return "";
		}
	}

	/**
	 * 任务前台显示状态
	 */
	public enum TaskForegroundStatus {
		NOT_RUNNING, // 未运行
		RUNNING, // 运行中
		RUN_FAILURE, // 运行失败
		RUN_SUCCESS, // 运行成功
		CANCEL_SUPPLY; // 取消补数据(该状态只应用于取消补数据的操作)

		public int indexOf() {
			return this.ordinal();
		}
	}

	/**
	 * 任务可执行的动作
	 */
	public enum TaskAction {
		REDO, // 重跑该任务
		REDO_CHILDREN, // 重跑该任务及其子任务
		REDO_BATCH, // 批量重跑

		PARALLEL_SUPPLY, // 并行补该任务数据
		PARALLEL_SUPPLY_CHILDREN, // 并行补该任务及其子任务数据
		PARALLEL_SUPPLY_BATCH, // 批量并行补数据

		SERIAL_SUPPLY, // 串行补该任务数据
		SERIAL_SUPPLY_CHILDREN, // 串行补该任务及其子任务数据
		SERIAL_SUPPLY_BATCH; // 批量串行补数据

		public int indexOf() {
			return this.ordinal();
		}
	}

	public enum ActionStatus {
		RUNNING, RUN_FAILURE, RUN_SUCCESS, RUN_EXCEPTION, LOG_EXCEPTION;
		public int indexOf() {
			return this.ordinal();
		}

		public static ActionStatus valueOf(int index) {
			return Configure.valueOf(ActionStatus.class, index);
		}
	}

	/**
	 * 调度程序执行是否已经完成
	 */
	public enum SchedulerMethod {
		CREATE_TASKS, // 根据作业每天自动创建相应任务
		UPDATE_RUN_FAILURE_TIMES, // 更新自动重跑运行失败的次数
		UPDATE_WAIT_TRIGGER, // 将符合条件的任务状态更改为未触发
		UPDATE_TRIGGERED, // 将符合条件的任务状态更改为已触发
		TASK_EXECUTE, // 任务执行
		GetChildrenTasks, TASK_UPDATE_WAIT_TRIGGER;
	}

	/**
	 * 文件准备完成的判断方式
	 * 
	 */
	public enum JudgeWay {
		FILE_NUMBER, // 以文件个数为依据
		HOUR; // 以小时点为依据

		public int indexOf() {
			return this.ordinal();
		}
	}

	/**
	 * 任务运行优先级
	 */
	public enum TaskRunningPriority {
		TODAY_FIRST, // 以当天任务优先运行
		YESTERDAY_FIRST, // 以昨天任务优先运行
		PERCENT; // 按当天与昨天的一个指定比率运行

		public int indexOf() {
			return this.ordinal();
		}
	}

	/**
	 * 是否随机选取需要被修改为未触发状态的任务
	 */
	public enum ReferPointRandom {
		NO, // 不随机选取需要被修改为未触发状态的任务
		YES, // 随机选取需要被修改为未触发状态的任务
		AUTO; // 自动切换参考点选取(9:30-22:30按固定顺序选取,其余时间随机选取)

		public int indexOf() {
			return this.ordinal();
		}
	}

	/**
	 * 是否需要断点重跑
	 */
	public enum NeedContinueRun {
		NO, // 不需要断点重跑,
		YES; // 需要断点重跑

		public int indexOf() {
			return this.ordinal();
		}
	}

	/**
	 * 任务操作类型(重跑、并行补数据、串行补数据)。用于生成操作批次号
	 */
	public enum TaskOperate {
		REDO("RD"), PARALLEL_SUPPLY("PS"), SERIAL_SUPPLY("SS");

		final String value;

		private TaskOperate(String value) {
			this.value = value;
		}

		public String value() {
			return this.value;
		}
	}

	/**
	 * 并发配置中作业类型分类
	 * 
	 * @author shiming.hong
	 * 
	 */
	public enum ConcurrentCategory {
		IMPORT_MYSQL, // 所有导入MySQL的作业类型
		IMPORT_GREENPLUM, // 所有导入GP的作业类型

		;

		public int indexOf() {
			return this.ordinal();
		}
	}

	/**
	 * 网关机调度方式
	 */
	public enum SchedulerWay {
		PARALLEL, // 并行
		SERIAL, // 串行

		;

		public int indexOf() {
			return this.ordinal();
		}
	}

	/**
	 * 网关机轮循方式
	 */
	public enum RoundWay {
		REFER, // 参考点
		SIMULATE, // 模拟

		;

		public int indexOf() {
			return this.ordinal();
		}
	}

	/**
	 * 服务器执行状态
	 */
	public enum ServerOperateStatus {
		UN_LINE, // 未上线
		ON_LINE, // 已上线
		OFF_LINE; // 已下线

		public int indexOf() {
			return this.ordinal();
		}
	}

	/**
	 * Hudson发布状态
	 * 
	 * @author shiming.hong
	 * 
	 */
	public enum HudsonPublishStatus {
		UN_PUBLISH, // 未发布
		PUBLISHING, // 正在发布
		PUBLISH_SUCCESS, // 发布成功
		PUBLISH_FAILURE // 发布失败
	}

	/**
	 * 短信提供方
	 */
	public enum SmsProvider {
		SW("sw"), // 短信平台
		WEB("web"), // 中国网建

		;

		final String value;

		private SmsProvider(String value) {
			this.value = value;
		}

		public String value() {
			return this.value;
		}
	}

	/**
	 * Hive版本
	 * 
	 * @author shiming.hong
	 */
	public enum HiveVersion {
		HIVE_0_7("0.7"), // Hive0.7
		HIVE_0_9("0.9"), // Hive0.9
		HIVE_1_1("1.1"), // Hive1.1

		;

		final String value;

		private HiveVersion(String value) {
			this.value = value;
		}

		public String value() {
			return this.value;
		}
	}

	// 前台未运行
	public static final Long[] TASK_FOREGROUND_NOT_RUNNING_STATUS = new Long[] { (long) TaskStatus.INITIALIZE.indexOf(), (long) TaskStatus.WAIT_TRIGGER.indexOf(),
			(long) TaskStatus.TRIGGERED.indexOf(), (long) TaskStatus.RE_INITIALIZE.indexOf(), (long) TaskStatus.RE_WAIT_TRIGGER.indexOf(), (long) TaskStatus.RE_TRIGGERED.indexOf() };

	// 前台运行状态
	public static final Long[] TASK_FOREGROUND_RUNNING_STATUS = new Long[] { (long) TaskStatus.RUNNING.indexOf(), (long) TaskStatus.RE_RUNNING.indexOf() };

	// 前台成功状态
	public static final Long[] TASK_FOREGROUND_SUCCESS_STATUS = new Long[] { (long) TaskStatus.RUN_SUCCESS.indexOf(), (long) TaskStatus.RE_RUN_SUCCESS.indexOf() };

	// 前台失败状态
	public static final Long[] TASK_FOREGROUND_FAILURE_STATUS = new Long[] { (long) TaskStatus.RUN_FAILURE.indexOf(), (long) TaskStatus.RE_RUN_FAILURE.indexOf() };

	@SuppressWarnings("unchecked")
	private static <T extends Enum<T>> T valueOf(Class<T> enumType, int index) {
		Object[] values = enumType.getEnumConstants();
		if (index < 0 || index >= values.length) {
			return null;
		}

		return (T) values[index];
	}
}