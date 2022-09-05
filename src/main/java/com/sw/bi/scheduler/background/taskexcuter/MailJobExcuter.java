package com.sw.bi.scheduler.background.taskexcuter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.springframework.util.StringUtils;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.background.util.DxDESCipher;
import com.sw.bi.scheduler.model.Datasource;
import com.sw.bi.scheduler.model.MailJobConfig;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.service.DatasourceService;
import com.sw.bi.scheduler.service.MailJobConfigService;
import com.sw.bi.scheduler.util.EnumUtil.DatasourceType;
import com.sw.bi.scheduler.util.MailUtil;

/**
 * 邮件发送任务. 邮件内容可以是固定的内容,也可以是SQL查询语句.
 * 程序会自动连接到指定的数据库,并执行SQL查询语句,将查询结果返回(最多返回500条记录)
 * 
 */
public class MailJobExcuter extends AbExcuter {

	public MailJobExcuter(Task task, String logFolder) {
		super(task, logFolder);
	}

	@Override
	public boolean excuteCommand() throws Exception {

		MailJobConfigService mailJobConfigService = BeanFactory.getService(MailJobConfigService.class);
		MailJobConfig config = mailJobConfigService.getByJobId(this.currentAction.getJobId());
		String mailTitle = this.replaceParams(config.getMailTitle());
		String mailContent = this.getMailContent(config);

		// 有邮件内容时才发送邮件
		if (StringUtils.hasText(mailContent)) {
			// String[] mailAddresses = config.getMailReceivers().split(",");

			MailUtil.send(config.getMailReceivers(), mailTitle, mailContent);
			/*try {
				for (String address : mailAddresses) {
					MailUtil.send(address, mailTitle, mailContent);
				}
			} catch (AddressException e) {
				throw e;
			} catch (Exception e) {
				throw e;
			}*/
		} else {
			log("邮件内容为空忽略邮件发送");
		}

		return true;
	}

	private String getMailContent(MailJobConfig config) throws Exception {
		if (config.getDatasourceId() == null) {
			return config.getMailContent();
		} else {
			String sql = config.getMailContent();
			sql = this.replaceParams(sql.replaceAll("\n", " ").trim());

			DatasourceService datasourceService = BeanFactory.getService(DatasourceService.class);
			Datasource ds = datasourceService.get(config.getDatasourceId());

			Connection conn = this.getConnection(ds);
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			String[] sqls = sql.split(";");

			try {
				for (int i = 0, len = sqls.length; i < len; i++) {
					String s = sqls[i].trim();
					log("SQL: " + s);
					pstmt = conn.prepareStatement(s);

					if (i < len - 1) {
						// 不是最后一条SQL时不需要返回结果集
						pstmt.execute();
					} else {
						if (s.startsWith("select ")) {
							// 最后一条SQL需要限制limit，并且有返回结果集
							this.checkLimit(s, ds);

							// 最后一条SQL以select开头则需要出结果集
							rs = pstmt.executeQuery();
						} else {
							pstmt.execute();
						}
					}
				}

				if (rs == null) {
					return null;
				}

				return this.toHTMLTable(rs);

			} finally {
				if (rs != null) {
					rs.close();
				}

				if (pstmt != null) {
					pstmt.close();
				}

				if (conn != null && !conn.isClosed()) {
					conn.close();
				}
			}
		}
	}

	private String toHTMLTable(ResultSet rs) throws SQLException {
		StringBuffer html = new StringBuffer();

		ResultSetMetaData rsm = rs.getMetaData();
		html.append("<table border=1 style=\"" + Parameters.MailTableStyle + "\">");
		//添加列名行
		html.append("<tr style=\"" + Parameters.MailTableHeadRowStyle + "\">");
		for (int i = 1; i <= rsm.getColumnCount(); i++) {
			html.append("<td>");
			html.append(rsm.getColumnLabel(i));
			html.append("</td>");
		}
		html.append("</tr>");

		boolean hasRecord = false;
		int count = 0;

		//添加数据行
		while (rs.next()) {
			hasRecord = true;

			html.append("<tr>");
			for (int i = 1; i <= rsm.getColumnCount(); i++) {
				html.append("<td>");
				//不考虑格式，任何数据类型都返回toString()的结果
				Object value = rs.getObject(i);
				html.append(value == null ? "&nbsp;" : value.toString());
				html.append("</td>");
			}
			html.append("</tr>");

			count++;
		}
		log("获得 " + count + " 条数据");

		if (!hasRecord) {
			return null;
		}

		html.append("</table>");
		return html.toString();
	}

	/**
	 * 根据数据源和sql语句查询，得到ResultSet结果集
	 * 
	 * @param ds
	 *            数据源
	 * @return
	 * @throws Exception
	 */
	private Connection getConnection(Datasource ds) throws Exception {

		if (ds.getType() == DatasourceType.MYSQL.indexOf()) {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		} else if (ds.getType() == DatasourceType.SQLSERVER.indexOf()) {
			Class.forName("com.microsoft.jdbc.sqlserver.SQLServerDriver").newInstance();
		} else if (ds.getType() == DatasourceType.GREENPLUM.indexOf()) {
			Class.forName("org.postgresql.Driver").newInstance();
		}

		String url = ds.getConnectionString();
		String username = ds.getUsername();
		String password = DxDESCipher.DecryptDES(ds.getPassword(), ds.getUsername());
		Connection con = DriverManager.getConnection(url, username, password);

		return con;
	}

	/**
	 * 检查简单的sql语句是否有控制返回条数，没有则添加limit 500 复杂的sql语句可能导致异常
	 * 
	 * @param sql
	 * @return
	 */
	private String checkLimit(String sql, Datasource ds) {
		String sqlLow = sql.toLowerCase();
		if (sqlLow.indexOf(" top ") > -1 || sqlLow.indexOf(" limit ") > -1) {
			return sql;
		} else {

			if (ds.getType() == DatasourceType.MYSQL.indexOf()) {
				if (!sql.trim().endsWith(";")) {
					sql += " limit 0,500 ";
				} else {
					sql = sql.trim().substring(0, sql.length() - 1) + " limit 0,500 ;";
				}
			} else if (ds.getType() == DatasourceType.SQLSERVER.indexOf()) {
				//sqlserver要写成select top 500 a,b,c,d from tablename ...
				sql = sql.trim();
				if (sql.startsWith("select")) {
					sql = "select top 500 " + sql.substring(7, sql.length());
				}
			} else if (ds.getType() == DatasourceType.GREENPLUM.indexOf()) {
				if (!sql.trim().endsWith(";")) {
					sql += " limit 500 ";
				} else {
					sql = sql.trim().substring(0, sql.length() - 1) + " limit 500 ;";
				}
			}

			return sql;
		}
	}

	private MailJobExcuter() {

	}

	public static void main(String[] args) throws Exception {
		MailJobConfig config = new MailJobConfig();
		config.setMailContent("select * from user");
		config.setDatasourceId(10l);
		MailJobExcuter e = new MailJobExcuter();
		try {
			System.out.print(e.getMailContent(config));
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}
