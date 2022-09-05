package com.sw.bi.scheduler.background.javatype.clean;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sw.bi.scheduler.background.taskexcuter.Parameters;
import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.model.EtlCleanConfig;
import com.sw.bi.scheduler.service.EtlCleanConfigService;
import com.sw.bi.scheduler.util.Configure;
import com.sw.bi.scheduler.util.DateUtil;

@Component
// 数据生命周期管理
// 目前只支持删除单分区的. 双分区的表不多,暂时程序上不支持. 如果有需要再修改本程序.
// ALTER TABLE table_name DROP IF EXISTS PARTITION (pt='20080808',hour='09');
// ALTER TABLE table_name DROP IF EXISTS PARTITION (pt='20080808');
// 1. hive table的数据生命周期配置,在前台界面上.
// 2. 配置一个类型为MapReduce的任务. 用来定点清理数据.
// 程序路径    ${scheduler_path}/scheduler jar ${scheduler_path}/scheduler.jar com.sw.bi.scheduler.background.javatype.clean.HiveTableCleaner
//  注意: 如果执行失败,删除不了HDFS上的数据,检查一下HDFS文件的属主,看看权限是否设置正常
public class HiveTableCleaner {
	private static final Logger log = Logger.getLogger(HiveTableCleaner.class);

	// 执行失败后自动重新执行的次数
	private static final int REDO_NUMBER = 5;

	@Autowired
	private EtlCleanConfigService etlCleanConfigService;

	/*@Autowired(required = false)
	// @Qualifier("SWSmsSender")
	@Qualifier("WebSmsSender")
	private SmsSender smsSender;*/

	/*@Autowired
	private SmsService smsService;*/

	public FileWriter logFileWriter;
	private String tmp_clean_dir = Parameters.tempCleanPath;

	private static String mobile = Configure.property(Configure.SMS_MOBILE);
	private static String smsKey = Configure.property(Configure.SMS_KEY);

	String[] hours = { "00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23" };

	public static void main(String args[]) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String today = sdf.format(DateUtil.getToday());
		try {
			boolean clean_result = HiveTableCleaner.getHiveTableCleaner().clean();
			if (!clean_result) {
				//SmsAlert.sendSms(today + "数据清理失败!", mobile, smsKey);
				System.exit(1);
			}
		} catch (Exception e) {
			try {
				// SmsAlert.sendSms(today + "数据清理时发生异常!", mobile, smsKey);
				System.exit(1);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

	private static HiveTableCleaner getHiveTableCleaner() {
		return BeanFactory.getBean(HiveTableCleaner.class);
	}

	private boolean clean() throws Exception {
		File filedir = new File(tmp_clean_dir);
		if (filedir.exists()) {
			filedir.delete();
		}
		filedir.mkdirs();

		StringBuffer content = new StringBuffer();
		Collection<String> needExcuteFiles = new ArrayList<String>();
		List<EtlCleanConfig> list = etlCleanConfigService.queryAll();
		for (EtlCleanConfig etlCleanConfig : list) {
			String table_name = etlCleanConfig.getTableName(); //from db
			String pt_name = etlCleanConfig.getPartitionName(); //from db
			String pt_type = etlCleanConfig.getPartitionType(); //from db
			int keep_days = etlCleanConfig.getKeepDays(); //from db

			if (keep_days < 3) { // 至少保留最近3天数据
				try {
					// SmsAlert.sendSms(table_name + "的数据清理配置有误!至少保留最近3天的数据,当前设置为: 保留" + keep_days + "天的数据!", mobile, smsKey);
					// smsService.sendMsg(mobile, table_name + "的数据清理配置有误!至少保留最近3天的数据,当前设置为: 保留" + keep_days + "天的数据!");
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					continue;
				}
			}

			log.info("待清理表名: " + table_name + ",表的分区名: " + pt_name + ",表的分区类型: " + pt_type + ",系统设置保留该表" + keep_days + "天的数据. ");

			int keepDays = keep_days * -1;
			String today = DateUtil.format(new Date(), "yyyyMMdd");

			for (int i = -40; i < keepDays; i++) {
				String date = DateUtil.getDay(today, i);

				if ("yyyyMMddHH".equals(pt_type)) {
					// 清空内容
					content.setLength(0);

					for (int j = 0; j < hours.length; j++) {
						String hour = hours[j];
						String pt_hour = date + hour;
						String drop = "ALTER TABLE " + table_name + " DROP IF EXISTS PARTITION (" + pt_name + "='" + pt_hour + "');";
						content.append(drop + "\r\n");
					}

					String fragmentFile = tmp_clean_dir + table_name + "_" + date + ".txt";
					logFileWriter = new FileWriter(fragmentFile);
					logFileWriter.write(content.toString());
					logFileWriter.flush();
					logFileWriter.close();

					needExcuteFiles.add(fragmentFile);

				} else if ("yyyyMMdd".equals(pt_type)) {
					String drop = "ALTER TABLE " + table_name + " DROP IF EXISTS PARTITION (" + pt_name + "='" + date + "');";
					content.append(drop + "\r\n");
				}
			}

			if ("yyyyMMdd".equals(pt_type)) {
				String fragmentFile = tmp_clean_dir + table_name + ".txt";
				logFileWriter = new FileWriter(fragmentFile);
				logFileWriter.write(content.toString());
				logFileWriter.flush();
				logFileWriter.close();

				needExcuteFiles.add(fragmentFile);
				content.setLength(0);
			}
		}

		log.info(needExcuteFiles.size());
		for (String fragmentFile : needExcuteFiles) {
			log.info("开始执行 " + fragmentFile);

			if (!this.programeRun(fragmentFile)) {
				return false;
			}
		}

		return true;

	}

	private boolean programeRun(String tempSqlPath) throws IOException {
		Process process = null;
		String[] commands = new String[] { "/bin/bash", "-c", "hiveclient -f " + tempSqlPath };

		try {
			for (int i = 0; i < REDO_NUMBER; i++) {
				process = Runtime.getRuntime().exec(commands);
				process.waitFor();

				if (process.exitValue() != 0) {
					log.info(tempSqlPath + " 运行失败,将被重新执行.");
					Thread.sleep(20000); //  20s
					continue;

				} else {
					log.info(tempSqlPath + " 运行成功.");
					return true;
				}
			}

		} catch (Exception e) {
			log.error(tempSqlPath + " 运行异常.", e);
		}

		return false;
	}
}
