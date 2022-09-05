package com.sw.bi.scheduler.background.taskexcuter;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.util.Configure.JobType;
import com.sw.bi.scheduler.util.DateUtil;

public class Parameters {

	public static String scheduler_user = "tools";
	//public static String scheduler_user = "p_shunwang_test";

	/**
	 * .sql临时文件存放目录
	 */
	public static String tempSqlPath = "/home/" + scheduler_user + "/temp/sqltemp/";

	/**
	 * .xml临时文件存放目录
	 */
	public static String tempXmlPath = "/home/" + scheduler_user + "/temp/xmltemp/";
	
	/**
	 * modify by qyx NewDatax临时文件存储路径
	 */
	public static String tempNewDataxPath = "/home/" + scheduler_user + "/temp/newdataxtemp/";

	/**
	 * 用于进行数据清理的命令脚本临时存放目录
	 */
	public static String tempCleanPath = "/home/" + scheduler_user + "/temp/cleantemp/";

	/**
	 * 用于预算同步的日志存放目录
	 */
	public static String tempBudgetPath = "/home/" + scheduler_user + "/temp/budgettemp/";

	/**
	 * datax工具的安装目录 并且要设置/home/tools/datax/tmpdir目录对调度所使用的用户可写.  ((这个地址不能修改，不然找不到模板了))
	 */
	public static String dataxToolPath = "/home/" + scheduler_user + "/datax/";

	/**
	 * datax模板文件存放地址.  ((这个地址不能修改，不然找不到模板了))
	 */
	public static String dataxFileTemplateDir = "/home/" + scheduler_user + "/datax/datax_file_template/";

	/**
	 * 新的datax工具的安装目录 根据上面说要设置/home/tools/datax/tmpdir目录对调度所使用的用户可写. 
	 * 
	 * ps:datax的模板地址应该是不用变的
	 */
	public static String newDataxToolPath = "/home/"+scheduler_user+"/newdatax/";
	
	/**
	 * 程序日志所在路径目录
	 */
	public static String logPath = "/home/" + scheduler_user + "/logs/etl_log/";

	/**
	 * hive jdbc连接地址 在报表质量监控中需要使用到.(ReportQualityExcuter)
	 */
	// public static String hiveConnString = "jdbc:hive://datanode2:50031/default";

	/**
	 * 调度程序安装主目录 提供给调度前台界面使用的
	 */
	public static String schedulerPath = "/home/" + scheduler_user + "/scheduler/";

	/**
	 * hivesql脚本主目录 提供给调度前台界面使用的
	 */
	public static String hivesqlPath = "/home/"+scheduler_user+"/etl/";

	/**
	 * 短信发送key
	 */
	// public static String smsKey = "3jqsd82122dslz882kxs8a9no4";

	/**
	 * 邮件表格头样式
	 */
	public static String MailTableHeadRowStyle = "font-weight:bold";

	/**
	 * 邮件表格样式
	 */
	public static String MailTableStyle = "border:1px 1px 1px 1px; width:95%";
	
	
	
	
	public static void main(String[] args){
		System.out.println(DateUtil.clearTime(new Date()).getTime());
	}
	

	public enum HiveSqlRunMode {
		TOPTOEND, BREAKCONTINE;
		public int indexOf() {
			return this.ordinal();
		}
	}
	//@modify by qyx ps:对运行结果的标识 成功1 失败0
	public enum BooleanResult {
		FAILED(false, 0), SUCEESS(true, 1);

		private boolean result;
		private int index;

		BooleanResult(boolean reuslt, int index) {
			this.result = result;
			this.index = index;
		}

		public int indexOf() {
			return this.ordinal();
		}

		public static BooleanResult valueOf(boolean result) {
			return BooleanResult.values()[result ? 1 : 0];
		}
	}

    
	//这里必须按照数字从小到大排序  ->DataxState 任务不同的操作码
	public static long[] DataxState = new long[] {

			JobType.ORACLE_TO_HDFS.indexOf(),//1
			JobType.MYSQL_TO_HDFS.indexOf(),//2
			JobType.SQLSERVER_TO_HDFS.indexOf(),//3
			JobType.LOCAL_FILE_TO_HDFS.indexOf(),//4

			JobType.HDFS_TO_ORACLE.indexOf(),//30
			JobType.HDFS_TO_MYSQL.indexOf(),//31
			JobType.HDFS_TO_SQLSERVER.indexOf(),//32
			JobType.HDFS_TO_LOCAL_FILE.indexOf(),//33
			JobType.HDFS_TO_HDFS.indexOf(), //34
			JobType.HDFS_TO_CSV.indexOf(), // 35
			JobType.HDFS_TO_GP.indexOf(), // 36
			JobType.HDFS_TO_FTP.indexOf(), // 37
			JobType.HDFS_TO_HBASE.indexOf(), // 38
			JobType.HDFS_TO_MONGODB.indexOf(),//39
			
			JobType.LOCAL_FILE_TO_ORACLE.indexOf(),//50
			JobType.LOCAL_FILE_TO_MYSQL.indexOf(),//51
			JobType.LOCAL_FILE_TO_SQLSERVER.indexOf(),//52
			JobType.LOCAL_FILE_TO_LOCAL_FILE.indexOf(),//53
			JobType.LOCAL_FILE_TO_CSV.indexOf(), // 54
			JobType.LOCAL_FILE_TO_GP.indexOf(), // 55
			JobType.LOCAL_FILE_TO_FTP.indexOf(),	//56
			JobType.LOCAL_FILE_TO_HBASE.indexOf(),	//57
			JobType.LOCAL_FILE_TO_MONGODB.indexOf(),//58
			
			JobType.MYSQL_TO_ORACLE.indexOf(),//60
			JobType.MYSQL_TO_MYSQL.indexOf(),//61
			JobType.MYSQL_TO_SQLSERVER.indexOf(),//62
			JobType.MYSQL_TO_LOCAL_FILE.indexOf(),//63
			JobType.MYSQL_TO_CSV.indexOf(),//
			JobType.MYSQL_TO_GP.indexOf(),//65
			JobType.MYSQL_TO_FTP.indexOf(),	//66
			JobType.MYSQL_TO_HBASE.indexOf(),	//67
			JobType.MYSQL_TO_MONGODB.indexOf(),//68
			
			JobType.SQLSERVER_TO_ORACLE.indexOf(),//70
			JobType.SQLSERVER_TO_MYSQL.indexOf(),//71
			JobType.SQLSERVER_TO_SQLSERVER.indexOf(),//72
			JobType.SQLSERVER_TO_LOCAL_FILE.indexOf(),//73
			JobType.SQLSERVER_TO_CSV.indexOf(), //
			JobType.SQLSERVER_TO_GP.indexOf(),//75
			JobType.SQLSERVER_TO_FTP.indexOf(),	//76
			JobType.SQLSERVER_TO_HBASE.indexOf(),	//77
			JobType.SQLSERVER_TO_MONGODB.indexOf(),//78
			
			JobType.ORACLE_TO_ORACLE.indexOf(),//80
			JobType.ORACLE_TO_MYSQL.indexOf(),//81
			JobType.ORACLE_TO_SQLSERVER.indexOf(),//82
			JobType.ORACLE_TO_LOCAL_FILE.indexOf(),//83
			JobType.ORACLE_TO_CSV.indexOf(), //
			JobType.ORACLE_TO_GP.indexOf(),//85
			JobType.ORACLE_TO_FTP.indexOf(),	//86
			JobType.ORACLE_TO_HBASE.indexOf(),	//87
			JobType.ORACLE_TO_MONGODB.indexOf(),//88
			
			JobType.DATAX_CUSTOM_XML.indexOf(), //101

			JobType.GP_TO_ORACLE.indexOf(), // 110
			JobType.GP_TO_MYSQL.indexOf(), // 111
			JobType.GP_TO_SQLSERVER.indexOf(), // 112
			JobType.GP_TO_LOCAL_FILE.indexOf(), // 113
			JobType.GP_TO_CSV.indexOf(), // 114
			JobType.GP_TO_GP.indexOf(), // 115
			JobType.GP_TO_HDFS.indexOf(), // 116
			JobType.GP_TO_FTP.indexOf(),	//117
			JobType.GP_TO_HBASE.indexOf(),	//118
			JobType.GP_TO_MONGODB.indexOf(),//119
			
			JobType.CSV_TO_ORACLE.indexOf(), // 120
			JobType.CSV_TO_MYSQL.indexOf(), // 121
			JobType.CSV_TO_SQLSERVER.indexOf(), // 122
			JobType.CSV_TO_LOCAL_FILE.indexOf(), // 123
			JobType.CSV_TO_CSV.indexOf(), // 124
			JobType.CSV_TO_GP.indexOf(), // 125
			JobType.CSV_TO_HDFS.indexOf(), // 126
			JobType.CSV_TO_FTP.indexOf(),	// 127
			JobType.CSV_TO_HBASE.indexOf(),	// 128
			JobType.CSV_TO_MONGODB.indexOf(),//129
			
			JobType.FTP_FILE_TO_ORACLE.indexOf(),	//130
			JobType.FTP_FILE_TO_MYSQL.indexOf(),	//131
			JobType.FTP_FILE_TO_SQLSERVER.indexOf(),	//132
			JobType.FTP_FILE_TO_LOCAL_FILE.indexOf(),	//133
			JobType.FTP_FILE_TO_CSV.indexOf(),	//134
			JobType.FTP_FILE_TO_GP.indexOf(),	//135
			JobType.FTP_FILE_TO_FTP_FILE.indexOf(),	//136
			JobType.FTP_FILE_TO_HBASE.indexOf(),	//137
			JobType.FTP_FILE_TO_FTP_HDFS.indexOf(),	//138
			JobType.FTP_TO_MONGODB.indexOf(),//139
			
			JobType.HBASE_TO_ORACLE.indexOf(),	//140
			JobType.HBASE_TO_MYSQL.indexOf(),	//141
			JobType.HBASE_TO_SQLSERVER.indexOf(),	//142
			JobType.HBASE_TO_LOCAL_FILE.indexOf(),	//143
			JobType.HBASE_TO_CSV.indexOf(),	//144
			JobType.HBASE_TO_GP.indexOf(),	//145
			JobType.HBASE_TO_HDFS.indexOf(),	//146
			JobType.HBASE_TO_FTP.indexOf(),	//147
			JobType.HBASE_TO_HBASE.indexOf(),	//148
			JobType.HBASE_TO_MONGODB.indexOf(),//149
			
			//add by zhoushasha
			JobType.MONGODB_TO_ORACLE.indexOf(),//150
			JobType.MONGODB_TO_MYSQL.indexOf(),//151
			JobType.MONGODB_TO_SQLSERVER.indexOf(),//152
			JobType.MONGODB_TO_LOCAL_FILE.indexOf(),//153
			JobType.MONGODB_TO_CSV.indexOf(),//154
			JobType.MONGODB_TO_GP.indexOf(),//155
			JobType.MONGODB_TO_HDFS.indexOf(),//156
			JobType.MONGODB_TO_FTP.indexOf(),//157
			JobType.MONGODB_TO_HBASE.indexOf(),//158
			JobType.MONGODB_TO_MONGODB.indexOf()//159

	};
	//add by mashifeng   2018/12/25 16:26:51 
    //新datax任务不同的操作码
	//这里必须按照数字从小到大排序  ->NewDataxState 任务不同的操作码
	public static long[] NewDataxState = new long[]{
	
		JobType.NEW_DATAX_SHELL.indexOf(), //103 
		JobType.NEW_DATAX_PY.indexOf(),//104 
		JobType.NEW_MYSQL_TO_MYSQL.indexOf(),//2030
		JobType.NEW_MYSQL_TO_SQLSERVER.indexOf(),//2031
		JobType.NEW_MYSQL_TO_ORACLE.indexOf(),//2032
		JobType.NEW_MYSQL_TO_FTP.indexOf(),//2033
		JobType.NEW_MYSQL_TO_HDFS.indexOf(),//2034
		JobType.NEW_MYSQL_TO_LOCAL_FILE.indexOf(),//2035
		JobType.NEW_MYSQL_TO_CSV.indexOf(),//2036
		JobType.NEW_MYSQL_TO_GREENPLUM.indexOf(),//2037
		JobType.NEW_MYSQL_TO_HBASE.indexOf(),//2038
		JobType.NEW_MYSQL_TO_MONGODB.indexOf(),//2039
		JobType.NEW_MYSQL_TO_SUNDB.indexOf(),//2040
		
		JobType.NEW_SQLSERVER_TO_MYSQL.indexOf(),//2130
		JobType.NEW_SQLSERVER_TO_SQLSERVER.indexOf(),//2131
		JobType.NEW_SQLSERVER_TO_ORACLE.indexOf(),//2132
		JobType.NEW_SQLSERVER_TO_FTP.indexOf(),//2133
		JobType.NEW_SQLSERVER_TO_HDFS.indexOf(),//2134
		JobType.NEW_SQLSERVER_TO_LOCAL_FILE.indexOf(),//2135
		JobType.NEW_SQLSERVER_TO_CSV.indexOf(),//2136
		JobType.NEW_SQLSERVER_TO_GREENPLUM.indexOf(),//2137
		JobType.NEW_SQLSERVER_TO_HBASE.indexOf(),//2138
		JobType.NEW_SQLSERVER_TO_MONGODB.indexOf(),//2139
		JobType.NEW_SQLSERVER_TO_SUNDB.indexOf(),//2040

		JobType.NEW_ORACLE_TO_MYSQL.indexOf(),//2230
		JobType.NEW_ORACLE_TO_SQLSERVER.indexOf(),//2231
		JobType.NEW_ORACLE_TO_ORACLE.indexOf(),//2232
		JobType.NEW_ORACLE_TO_FTP.indexOf(),//2233
		JobType.NEW_ORACLE_TO_HDFS.indexOf(),//2234
		JobType.NEW_ORACLE_TO_LOCAL_FILE.indexOf(),//2235
		JobType.NEW_ORACLE_TO_CSV.indexOf(),//2236
		JobType.NEW_ORACLE_TO_GREENPLUM.indexOf(),//2237
		JobType.NEW_ORACLE_TO_HBASE.indexOf(),//2238
		JobType.NEW_ORACLE_TO_MONGODB.indexOf(),//2239
		JobType.NEW_ORACLE_TO_SUNDB.indexOf(),//2040

		JobType.NEW_FTP_TO_MYSQL.indexOf(),//2330
		JobType.NEW_FTP_TO_SQLSERVER.indexOf(),//2331
		JobType.NEW_FTP_TO_ORACLE.indexOf(),//2332
		JobType.NEW_FTP_TO_FTP.indexOf(),//2333
		JobType.NEW_FTP_TO_HDFS.indexOf(),//2334
		JobType.NEW_FTP_TO_LOCAL_FILE.indexOf(),//2335
		JobType.NEW_FTP_TO_CSV.indexOf(),//2336
		JobType.NEW_FTP_TO_GREENPLUM.indexOf(),//2337
		JobType.NEW_FTP_TO_HBASE.indexOf(),//2338
		JobType.NEW_FTP_TO_MONGODB.indexOf(),//2339
		JobType.NEW_FTP_TO_SUNDB.indexOf(),//2040

		JobType.NEW_HDFS_TO_MYSQL.indexOf(),//2430
		JobType.NEW_HDFS_TO_SQLSERVER.indexOf(),//2431
		JobType.NEW_HDFS_TO_ORACLE.indexOf(),//2432
		JobType.NEW_HDFS_TO_FTP.indexOf(),//2433
		JobType.NEW_HDFS_TO_HDFS.indexOf(),//2434
		JobType.NEW_HDFS_TO_LOCAL_FILE.indexOf(),//2435
		JobType.NEW_HDFS_TO_CSV.indexOf(),//2436
		JobType.NEW_HDFS_TO_GREENPLUM.indexOf(),//2437
		JobType.NEW_HDFS_TO_HBASE.indexOf(),//2438
		JobType.NEW_HDFS_TO_MONGODB.indexOf(),//2439
		JobType.NEW_HDFS_TO_SUNDB.indexOf(),//2040

		JobType.NEW_LOCAL_FILE_TO_MYSQL.indexOf(),//2530
		JobType.NEW_LOCAL_FILE_TO_SQLSERVER.indexOf(),//2531
		JobType.NEW_LOCAL_FILE_TO_ORACLE.indexOf(),//2532
		JobType.NEW_LOCAL_FILE_TO_FTP.indexOf(),//2533
		JobType.NEW_LOCAL_FILE_TO_HDFS.indexOf(),//2534
		JobType.NEW_LOCAL_FILE_TO_LOCAL_FILE.indexOf(),//2535
		JobType.NEW_LOCAL_FILE_TO_CSV.indexOf(),//2536
		JobType.NEW_LOCAL_FILE_TO_GREENPLUM.indexOf(),//2537
		JobType.NEW_LOCAL_FILE_TO_HBASE.indexOf(),//2538
		JobType.NEW_LOCAL_FILE_TO_MONGODB.indexOf(),//2539
		JobType.NEW_LOCAL_FILE_TO_SUNDB.indexOf(),//2040

		JobType.NEW_CSV_TO_MYSQL.indexOf(),//2630
		JobType.NEW_CSV_TO_SQLSERVER.indexOf(),//2631
		JobType.NEW_CSV_TO_ORACLE.indexOf(),//2632
		JobType.NEW_CSV_TO_FTP.indexOf(),//2633
		JobType.NEW_CSV_TO_HDFS.indexOf(),//2634
		JobType.NEW_CSV_TO_LOCAL_FILE.indexOf(),//2635
		JobType.NEW_CSV_TO_CSV.indexOf(),//2636
		JobType.NEW_CSV_TO_GREENPLUM.indexOf(),//2637
		JobType.NEW_CSV_TO_HBASE.indexOf(),//2638
		JobType.NEW_CSV_TO_MONGODB.indexOf(),//2639
		JobType.NEW_CSV_TO_SUNDB.indexOf(),//2040
		
		
		JobType.NEW_GP_TO_MYSQL.indexOf(),//2730
		JobType.NEW_GP_TO_SQLSERVER.indexOf(),//2731
		JobType.NEW_GP_TO_ORACLE.indexOf(),//2732
		JobType.NEW_GP_TO_FTP.indexOf(),//2733
		JobType.NEW_GP_TO_HDFS.indexOf(),//2734
		JobType.NEW_GP_TO_LOCAL_FILE.indexOf(),//2735
		JobType.NEW_GP_TO_CSV.indexOf(),//2736
		JobType.NEW_GP_TO_GREENPLUM.indexOf(),//2737
		JobType.NEW_GP_TO_HBASE.indexOf(),//2738
		JobType.NEW_GP_TO_MONGODB.indexOf(),//2739
		JobType.NEW_GP_TO_SUNDB.indexOf(),//2040

		JobType.NEW_HBASE_TO_MYSQL.indexOf(),//2830
		JobType.NEW_HBASE_TO_SQLSERVER.indexOf(),//2831
		JobType.NEW_HBASE_TO_ORACLE.indexOf(),//2832
		JobType.NEW_HBASE_TO_FTP.indexOf(),//2833
		JobType.NEW_HBASE_TO_HDFS.indexOf(),//2834
		JobType.NEW_HBASE_TO_LOCAL_FILE.indexOf(),//2835
		JobType.NEW_HBASE_TO_CSV.indexOf(),//2836
		JobType.NEW_HBASE_TO_GREENPLUM.indexOf(),//2837
		JobType.NEW_HBASE_TO_HBASE.indexOf(),//2838
		JobType.NEW_HBASE_TO_MONGODB.indexOf(),//2839
		JobType.NEW_HBASE_TO_SUNDB.indexOf(),//2040

		JobType.NEW_MONGODB_TO_MYSQL.indexOf(),//2930
		JobType.NEW_MONGODB_TO_SQLSERVER.indexOf(),//2931
		JobType.NEW_MONGODB_TO_ORACLE.indexOf(),//2932
		JobType.NEW_MONGODB_TO_FTP.indexOf(),//2933
		JobType.NEW_MONGODB_TO_HDFS.indexOf(),//2934
		JobType.NEW_MONGODB_TO_LOCAL_FILE.indexOf(),//2935
		JobType.NEW_MONGODB_TO_CSV.indexOf(),//2936
		JobType.NEW_MONGODB_TO_GREENPLUM.indexOf(),//2937
		JobType.NEW_MONGODB_TO_HBASE.indexOf(),//2938
		JobType.NEW_MONGODB_TO_MONGODB.indexOf(),//2939
		JobType.NEW_MONGODB_TO_SUNDB.indexOf(),//2040
		
		
		JobType.NEW_SUNDB_TO_MYSQL.indexOf(),//3030
		JobType.NEW_SUNDB_TO_SQLSERVER.indexOf(),//3031
		JobType.NEW_SUNDB_TO_ORACLE.indexOf(),//3032
		JobType.NEW_SUNDB_TO_FTP.indexOf(),//3033
		JobType.NEW_SUNDB_TO_HDFS.indexOf(),//3034
		JobType.NEW_SUNDB_TO_LOCAL_FILE.indexOf(),//3035
		JobType.NEW_SUNDB_TO_CSV.indexOf(),//3036
		JobType.NEW_SUNDB_TO_GREENPLUM.indexOf(),//3037
		JobType.NEW_SUNDB_TO_HBASE.indexOf(),//3038
		JobType.NEW_SUNDB_TO_MONGODB.indexOf(),//3039
		JobType.NEW_SUNDB_TO_SUNDB.indexOf(),//3040
		
		

	};
	public static long[] FtpState = new long[] {

	JobType.FTP_FILE_TO_HDFS.indexOf(), JobType.FTP_FILE_TO_HDFS_FIVE_MINUTE.indexOf(), JobType.FTP_FILE_TO_HDFS_YESTERDAY.indexOf() };

	public static long[] HiveSqlState = new long[] { JobType.HIVE_SQL.indexOf() };

	public static long[] MapReduceState = new long[] { JobType.MAPREDUCE.indexOf() };

	public static long[] ProcState = new long[] { JobType.STORE_PROCEDURE.indexOf() };

	public static long[] ShellState = new long[] { JobType.SHELL.indexOf() };

	public static long[] MailJob = new long[] { JobType.MAIL.indexOf() };

	public static long[] ReportQuality = new long[] { JobType.REPORT_QUALITY.indexOf() };

	public static long[] VirtualState = new long[] { JobType.VIRTUAL.indexOf() };

	public static long[] CheckDependency = new long[] { JobType.CHECK_DAY_DEPENDENCY_HOUR.indexOf(), JobType.CHECK_MONTH_DEPENDENCY_DAY.indexOf() };

	public static long[] Branch = new long[] { JobType.BRANCH.indexOf() };

	public static long[] Greenplum = new long[] { JobType.GREENPLUM_FUNCTION.indexOf() };

	public static long[] Put2Hdfs = new long[] { JobType.PUT_TO_HDFS.indexOf() };

	public static long[] FileNumberCheck = new long[] { JobType.FILE_NUMBER_CHECK.indexOf() };

	public static Map<String, String> getRunTimeParamter(Task task) {
		Map<String, String> runTimeParams = new HashMap<String, String>();
		Calendar calendar = DateUtil.cloneCalendar();

		calendar.setTime(task != null ? task.getSettingTime() : new Date());
		runTimeParams.put("$date_now", DateUtil.format(calendar.getTime(), "yyyyMMdd"));
		runTimeParams.put("${date_now}", DateUtil.format(calendar.getTime(), "yyyyMMdd"));
		runTimeParams.put("$%7Bdate_now}", DateUtil.format(calendar.getTime(), "yyyyMMdd"));

		runTimeParams.put("$hour_now", DateUtil.format(calendar.getTime(), "yyyyMMddHH"));
		runTimeParams.put("${hour_now}", DateUtil.format(calendar.getTime(), "yyyyMMddHH"));
		runTimeParams.put("$%7Bhour_now}", DateUtil.format(calendar.getTime(), "yyyyMMddHH"));
		
		runTimeParams.put("$hour_now_short", DateUtil.format(calendar.getTime(), "HH"));
		runTimeParams.put("${hour_now_short}", DateUtil.format(calendar.getTime(), "HH"));
		runTimeParams.put("$%7Bhour_now_short}", DateUtil.format(calendar.getTime(), "HH"));
		
		
		

		runTimeParams.put("$month_now", DateUtil.format(calendar.getTime(), "yyyyMM"));
		runTimeParams.put("${month_now}", DateUtil.format(calendar.getTime(), "yyyyMM"));
		runTimeParams.put("$%7Bmonth_now}", DateUtil.format(calendar.getTime(), "yyyyMM"));

		/////////////////////////////////////////////////////////////////////////

		calendar.add(Calendar.DATE, -1);
		runTimeParams.put("$date_desc", DateUtil.format(calendar.getTime(), "yyyyMMdd"));
		runTimeParams.put("${date_desc}", DateUtil.format(calendar.getTime(), "yyyyMMdd"));
		runTimeParams.put("$%7Bdate_desc}", DateUtil.format(calendar.getTime(), "yyyyMMdd"));

		calendar.setTime(task != null ? task.getSettingTime() : new Date());
		calendar.add(Calendar.HOUR, -1);
		runTimeParams.put("$hour_desc", DateUtil.format(calendar.getTime(), "yyyyMMddHH"));
		runTimeParams.put("${hour_desc}", DateUtil.format(calendar.getTime(), "yyyyMMddHH"));
		runTimeParams.put("$%7Bhour_desc}", DateUtil.format(calendar.getTime(), "yyyyMMddHH"));
		
		runTimeParams.put("$hour_desc_short", DateUtil.format(calendar.getTime(), "HH"));
		runTimeParams.put("${hour_desc_short}", DateUtil.format(calendar.getTime(), "HH"));
		runTimeParams.put("$%7Bhour_desc_short}", DateUtil.format(calendar.getTime(), "HH"));
		

		calendar.setTime(task != null ? task.getSettingTime() : new Date());
		calendar.add(Calendar.MONTH, -1);
		runTimeParams.put("$month_desc", DateUtil.format(calendar.getTime(), "yyyyMM"));
		runTimeParams.put("${month_desc}", DateUtil.format(calendar.getTime(), "yyyyMM"));
		runTimeParams.put("$%7Bmonth_desc}", DateUtil.format(calendar.getTime(), "yyyyMM"));

		//对参数再进行一些扩展. 如果配置job的时候直接把路径写死. 那么下次如果程序路径发生了变更,那么还要去批量修改数据库中的记录.
		//如果job录入的时候,hivesql脚本所在的根目录路径可以用变量的形式,那么路径变更时,只要修改一处即可.
		runTimeParams.put("$hivesql_path", hivesqlPath);
		runTimeParams.put("${hivesql_path}", hivesqlPath);
		runTimeParams.put("$%7Bhivesql_path}", hivesqlPath);

		runTimeParams.put("$scheduler_path", schedulerPath);
		runTimeParams.put("${scheduler_path}", schedulerPath);
		runTimeParams.put("$%7Bscheduler_path}", schedulerPath);

		return runTimeParams;
	}
	
	


	/**
	 * hadoop集群性能状态解析页面 该变量目前没有使用
	 */
	///public static String hadoopClusterPage = "http://172.16.15.120/ganglia/?p=2&c=shunwang-hadoop-cluster&h=&hc=4&p=1";

	//modify by qyx ps:这是是对数据库的类型进行定义的枚举,通过不同的枚举拿到数据库对应的驱动类
	public enum DBType {
		//modify by zhoushasha
		File(3, ""), Mysql(0, "com.mysql.jdbc.Driver"), Sqlserver(1, "com.microsoft.sqlserver.jdbc.SQLServerDriver"), Oracle(2, "oracle.jdbc.driver.OracleDriver"), Ftp(4, ""), HDFS(5, ""), CSV(6, ""), Greenplum(
				7, "org.postgresql.Driver"), HBase(8, ""),MongoDb(9, ""),SunDb(10, "");
		private final int index;

		private DBType(int index, String driver) {
			this.index = index;
			this.driver = driver;
		}

		private final String driver;

		public int indexOf() {
			return index;
		}

		public String getDriver() {
			return this.driver;
		}

		public static DBType valueOf(int index) {
			DBType[] values = DBType.values();
			for (DBType value : values) {
				if (value.indexOf() == index) {
					return value;
				}
			}
			return null;
		
		}
		
		

	}


}
