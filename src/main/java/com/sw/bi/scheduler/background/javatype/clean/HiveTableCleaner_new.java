package com.sw.bi.scheduler.background.javatype.clean;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.model.EtlCleanConfig;
import com.sw.bi.scheduler.service.EtlCleanConfigService;
import com.sw.bi.scheduler.util.Configure;
import com.sw.bi.scheduler.util.DateUtil;

import framework.commons.sender.MessagePlatform;
import com.sw.bi.scheduler.supports.MessageSenderAssistant;

@Component
// 慎用此类.
// 数据生命周期管理
// 目前只支持删除单分区的. 双分区的表不多,暂时程序上不支持. 如果有需要再修改本程序.
// ALTER TABLE table_name DROP IF EXISTS PARTITION (pt='20080808',hour='09');
// ALTER TABLE table_name DROP IF EXISTS PARTITION (pt='20080808');
// 1. hive table的数据生命周期配置,在前台界面上.
// 2. 配置一个类型为MapReduce的任务. 用来定点清理数据.
// 程序路径    ${scheduler_path}/scheduler jar ${scheduler_path}/scheduler.jar com.sw.bi.scheduler.background.javatype.clean.HiveTableCleaner
//  
public class HiveTableCleaner_new {
	private static final Logger log = Logger.getLogger(HiveTableCleaner_new.class);

	// 执行失败后自动重新执行的次数
	// private static final int REDO_NUMBER = 5;

	@Autowired
	private EtlCleanConfigService etlCleanConfigService;

	/*@Autowired(required = false)
	// @Qualifier("SWSmsSender")
	@Qualifier("WebSmsSender")
	private SmsSender smsSender;*/

	/*@Autowired
	private SmsService smsService;*/
	private MessageSenderAssistant messageSender = new MessageSenderAssistant();

	// public FileWriter logFileWriter;
	// private String tmp_clean_dir = Parameters.tempCleanPath;

	private static String mobile = Configure.property(Configure.SMS_MOBILE);
	private static String smsKey = Configure.property(Configure.SMS_KEY);

	private Connection connection;
	private Statement stmt;
	private PreparedStatement pstmt;
	private ResultSet rs;
	private FileSystem fileSystem;

	String[] hours = { "00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23" };

	public static void main(String args[]) {
		String today = DateUtil.format(DateUtil.getToday(), "yyyyMMdd");

		try {
			boolean result = HiveTableCleaner_new.getHiveTableCleaner().clean();
			if (!result) {
				//SmsAlert.sendSms(today + "数据清理失败!", mobile, smsKey);
				System.exit(1);
			}

		} catch (Exception e) {
			try {
				// SmsAlert.sendSms(today + "数据清理时发生异常!", mobile, smsKey);
				log.error(today + "数据清理时发生异常. ", e);
				System.exit(1);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

	private static HiveTableCleaner_new getHiveTableCleaner() {
		return BeanFactory.getBean(HiveTableCleaner_new.class);
	}

	private boolean clean() {
		Map<String, Collection<String>> mapping = getKeepPartitions();

		if (mapping.size() == 0) {
			log.info("没有需要被清理的表.");
			return true;
		}

		try {
			connection();

			for (String tableName : mapping.keySet()) {
				// 获得指定表应该被保留的分区
				Collection<String> partitions = mapping.get(tableName);

				// 获得指定表的相关信息

				Collection<Map<String, Object>> metadatas = this.getMetadata(tableName, partitions);

				if (metadatas.size() == 0) {
					continue;
				}

				// 删除分区

				connection.setAutoCommit(false);

				for (Map<String, Object> metadata : metadatas) {
					if (!this.deletePartition(metadata)) {
						return false;
					} else {
						log.info("表" + tableName + "的分区数据及数据库中的相关信息删除成功.");
					}
				}

				connection.commit();
			}

		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				log.error("rollback failed.", e1);
			}
		} finally {
			release(true);
		}

		return true;
	}

	/**
	 * 根据配置信息计算每个表需要被保留的分区
	 * 
	 * @return
	 */
	private Map<String, Collection<String>> getKeepPartitions() {
		Map<String, Collection<String>> mapping = new HashMap<String, Collection<String>>();

		Collection<EtlCleanConfig> configs = etlCleanConfigService.queryAll();
		Calendar yesterday = null;
		for (EtlCleanConfig config : configs) {
			String tableName = config.getTableName();
			String ptName = config.getPartitionName();
			String ptType = config.getPartitionType();
			int keepDays = config.getKeepDays();

			if (keepDays < 3) {
				try {
					// SmsAlert.sendSms(tableName + "的数据清理配置有误!至少保留最近3天的数据,当前设置为: 保留" + keepDays + "天的数据!", mobile, smsKey);
					// smsService.sendMsg(mobile, tableName + "的数据清理配置有误!至少保留最近3天的数据,当前设置为: 保留" + keepDays + "天的数据!");
//					messageSender.sendSms(mobile, tableName + "的数据清理配置有误!至少保留最近3天的数据,当前设置为: 保留" + keepDays + "天的数据!");
					messageSender.send(MessagePlatform.SMS_ADTIME,mobile, tableName + "的数据清理配置有误!至少保留最近3天的数据,当前设置为: 保留" + keepDays + "天的数据!");
				} catch (Exception e) {
					log.error(e);
				}

				continue;
			}

			yesterday = DateUtil.getYesterdayCalendar();
			Collection<String> partitions = new ArrayList<String>();
			for (int i = 1; i <= keepDays; i++) {
				String date = DateUtil.format(yesterday.getTime(), "yyyyMMdd");

				if ("yyyyMMdd".equals(ptType)) {
					partitions.add(ptName + "=" + date);

				} else if ("yyyyMMddHH".equals(ptType)) {
					for (String hour : hours) {
						partitions.add(ptName + "=" + date + hour);
					}
				}

				yesterday.add(Calendar.DATE, -1);
			}
			log.info(tableName + "中需要保留的分区: " + partitions);
			mapping.put(tableName, partitions);

		}

		return mapping;
	}

	/**
	 * 获得指定表的元数据信息
	 * 
	 * @param tableName
	 * @param partitions
	 *            需要保留的分区
	 * @return
	 */
	private Collection<Map<String, Object>> getMetadata(String tableName, Collection<String> partitions) {
		Collection<Map<String, Object>> metadatas = new ArrayList<Map<String, Object>>();

		StringBuffer sql = new StringBuffer();
		sql.append("select t.TBL_NAME, t.TBL_ID, p.SD_ID, p.PART_ID, p.PART_NAME, s.location ");
		sql.append("from TBLS t ");
		sql.append("left join PARTITIONS p on t.TBL_ID = p.TBL_ID ");
		sql.append("left join SDS s on p.SD_ID = s.SD_ID ");
		sql.append("where t.TBL_NAME = ? ");

		sql.append("and p.PART_NAME not in (");
		for (int i = 0, len = partitions.size(); i < len; i++) {
			sql.append(i == 0 ? "?" : ",?");
		}
		sql.append(")");
		log.info(sql);

		try {
			pstmt = connection.prepareStatement(sql.toString());
			pstmt.setString(1, tableName);
			int i = 2;
			for (String partition : partitions) {
				pstmt.setObject(i, partition);
				i += 1;
			}

			rs = pstmt.executeQuery();

			Map<String, Object> metadata = null;
			while (rs.next()) {
				metadata = new HashMap<String, Object>();

				metadata.put("tblName", rs.getString(1));
				metadata.put("tblId", rs.getLong(2));
				metadata.put("sdId", rs.getLong(3));
				metadata.put("partId", rs.getLong(4));
				metadata.put("partName", rs.getString(5));
				metadata.put("location", rs.getString(6));

				metadatas.add(metadata);
			}
		} catch (SQLException e) {
			log.error("get " + tableName + " metadata failed.", e);

		} finally {
			release(false);
		}

		return metadatas;
	}

	/**
	 * 删除分区信息
	 * 
	 * @param metadata
	 */
	private boolean deletePartition(Map<String, Object> metadata) {
		String tableName = (String) metadata.get("tblName");
		String location = (String) metadata.get("location");

		StringBuffer row = new StringBuffer("需要被清理表的信息: ");
		row.append("表名: ").append(tableName).append(", ");
		row.append("分区名: ").append(metadata.get("partName")).append(", ");
		row.append("Hadoop地址: ").append(location);
		log.info(row);

		// 先从Hadoop中删除分区数据

		Path patitionDirectory = new Path(location);
		try {
			log.info("正在删除 " + location + "...");
			fileSystem.delete(patitionDirectory, true);

		} catch (IOException e) {
			log.error("删除Hadoop中分区数据失败.", e);
			return false;
		}

		// 删除数据库相关表的信息

		Long sdId = (Long) metadata.get("sdId");
		Long partId = (Long) metadata.get("partId");

		try {
			stmt = connection.createStatement();
			stmt.addBatch("delete from PARTITION_KEY_VALS where PART_ID = " + partId);
			stmt.addBatch("delete from PARTITION_PARAMS where PART_ID = " + partId);
			stmt.addBatch("delete from PARTITIONS where PART_ID = " + partId);
			if (Configure.property(Configure.HIVE_0_7, Boolean.class)) {
				stmt.addBatch("delete from COLUMNS WHERE SD_ID = " + sdId);
			} else if (Configure.property(Configure.HIVE_0_9, Boolean.class)
					||Configure.property(Configure.HIVE_1_1, Boolean.class)) {
				stmt.addBatch("delete c from COLUMNS_V2 c left join SDS s on s.CD_ID = c.CD_ID where s.SD_ID = " + sdId);
			}
			stmt.addBatch("delete from SDS WHERE SD_ID = " + sdId);
			stmt.executeBatch();

			log.info("正在删除表" + tableName + "在数据库中的相关数据...");

		} catch (SQLException e) {
			log.error("删除数据库与分区相关表失败.", e);

			return false;
		} finally {
			release(false);
		}

		return true;
	}

	private void connection() {
		try {
			Class.forName(Configure.property(Configure.HIVE_DATABASE_CONNECTION_DRIVER_CLASS));

			connection = DriverManager.getConnection(Configure.property(Configure.HIVE_DATABASE_CONNECTION_URL), Configure.property(Configure.HIVE_DATABASE_CONNECTION_USERNAME), Configure
					.property(Configure.HIVE_DATABASE_CONNECTION_PASSWORD));

			log.info("Hive数据库连接成功");

		} catch (ClassNotFoundException e) {
			log.error("hive dirver not found.", e);
		} catch (SQLException e) {
			log.error("hive connected failed.", e);
		}

		try {
			Configuration configuration = new Configuration();
			fileSystem = FileSystem.get(configuration);
		} catch (IOException e) {
			log.error("get hadoop filesytem failed.", e);
		}
	}

	/**
	 * 释放连接
	 * 
	 * @param fullRelease
	 */
	private void release(boolean fullRelease) {
		try {
			if (rs != null) {
				rs.close();
				rs = null;
			}

			if (stmt != null) {
				stmt.close();
				stmt = null;
			}

			if (pstmt != null) {
				pstmt.close();
				pstmt = null;
			}

			if (fullRelease && connection != null) {
				connection.close();
				connection = null;

				log.info("Hive数据库连接释放.");
			}

		} catch (SQLException e) {
			log.error("release hive failed.", e);
		}

		if (fullRelease) {
			try {
				fileSystem.close();
			} catch (IOException e) {
				log.error("release hadoop filesystem failed.", e);
			}
		}
	}

}
