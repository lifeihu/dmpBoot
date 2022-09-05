package com.sw.bi.scheduler.background.javatype.check;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.background.util.DxDESCipher;
import com.sw.bi.scheduler.model.Datasource;
import com.sw.bi.scheduler.model.User;
import com.sw.bi.scheduler.service.DatasourceService;
import com.sw.bi.scheduler.service.UserService;

import framework.commons.sender.MessagePlatform;
import com.sw.bi.scheduler.supports.MessageSenderAssistant;

/**
 * 检查数据源连接
 * 
 * @author shiming.hong
 */
@Component
//    /home/tools/scheduler/scheduler jar   /home/tools/scheduler/scheduler.jar com.sw.bi.scheduler.background.javatype.check.CheckDataSourceConnection
public class CheckDataSourceConnection {
	private static final Logger log = Logger.getLogger(CheckDataSourceConnection.class);

	@Autowired
	private DatasourceService datasourceService;

	@Autowired
	private UserService userService;

	/*@Autowired(required = false)
	// @Qualifier("SWSmsSender")
	@Qualifier("WebSmsSender")
	private SmsSender smsSender;*/

	/*@Autowired
	private SmsService smsService;*/
	private MessageSenderAssistant messageSender = new MessageSenderAssistant();

	/**
	 * 连接失败的数据源信息
	 */
	private Map<Long, String> unconnection = new HashMap<Long, String>();

	/**
	 * 连接失败的数据源数量
	 */
	private int unconnectionNumber = 0;

	private void check() {
		Collection<Datasource> datasources = datasourceService.queryAll();

		for (Datasource datasource : datasources) {
			if (datasource.getName().indexOf("作废") > -1) {
				continue;
			}

			this.testDatasource(datasource);
		}

		if (unconnectionNumber > 0) {
			for (Entry<Long, String> entry : unconnection.entrySet()) {
				long userId = entry.getKey();
				User user = userService.get(userId);
				String mobile = user.getMobilePhone();

				// smsService.sendMsg(mobile, entry.getValue());
//				messageSender.sendSms(mobile, entry.getValue());
				messageSender.send(MessagePlatform.SMS_ADTIME,mobile, entry.getValue());
			}
		}

		System.exit(0);
	}

	public void testDatasource(Datasource datasource) {
		String driverClass = null;
		int type = datasource.getType().intValue();
		long userId = datasource.getCreateBy();
		String name = datasource.getName();

		FTPClient client = null;
		Connection connection = null;
		Statement stmt = null;
		ResultSet rs = null;
		// String message = "";
		String typeDesc = null;

		try {
			if (type == 3) {
				typeDesc = "FTP";

				// FTP数据源
				client = new FTPClient();
				client.connect(datasource.getIp(), Integer.parseInt(datasource.getPort()));
				client.login(datasource.getUsername(), DxDESCipher.DecryptDES(datasource.getPassword(), datasource.getUsername()));

				int reply = client.getReplyCode();
				if (!FTPReply.isPositiveCompletion(reply)) {
					client.disconnect();
					log.info("FTP(" + name + ") - 服务器拒绝连接.");

					unconnectionNumber += 1;

					String disconnection = unconnection.get(userId);
					if (disconnection == null) {
						disconnection = "";
					}
					disconnection += "FTP(" + name + ") - 服务器拒绝连接.\n";
					unconnection.put(userId, disconnection);
				}

				client.logout();

				// message = "FTP服务器(" + datasource.getIp() + ").";

			} else {
				// DB数据源

				String testSql = null;
				if (type == 0) {
					testSql = "select concat('MySQL ', version())";
					driverClass = "com.mysql.jdbc.Driver";
					typeDesc = "MySQL";

				} else if (type == 1) {
					testSql = "select @@version";
					driverClass = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
					typeDesc = "SQLServer";

				} else if (type == 7) {
					testSql = "select version()";
					driverClass = "org.postgresql.Driver";
					typeDesc = "Greenplum";
				}

				Class.forName(driverClass);
				connection = DriverManager.getConnection(datasource.getConnectionString(), datasource.getUsername(), DxDESCipher.DecryptDES(datasource.getPassword(), datasource.getUsername()));
				stmt = connection.createStatement();
				rs = stmt.executeQuery(testSql);

				/*if (rs.next()) {
					message = rs.getString(1);
				}*/

			}

			// message = "数据源连接成功 - " + message;
			log.info(typeDesc + "(" + name + ") - 连接成功.");

		} catch (Exception e) {
			log.info(typeDesc + "(" + name + ") - 连接失败.");
			e.printStackTrace();

			unconnectionNumber += 1;

			String disconnection = unconnection.get(userId);
			if (disconnection == null) {
				disconnection = "";
			}
			disconnection += typeDesc + "(" + name + ") - 连接失败.\n";
			unconnection.put(userId, disconnection);

		} finally {
			try {
				if (client != null) {
					client.disconnect();
				}

				if (rs != null) {
					rs.close();
				}

				if (stmt != null) {
					stmt.close();
				}

				if (connection != null) {
					connection.close();
				}
			} catch (Exception e) {}
		}
	}

	public static CheckDataSourceConnection getCheckDataSourceConnection() {
		return BeanFactory.getBean(CheckDataSourceConnection.class);
	}

	public static void main(String[] args) {
		getCheckDataSourceConnection().check();
	}

}
