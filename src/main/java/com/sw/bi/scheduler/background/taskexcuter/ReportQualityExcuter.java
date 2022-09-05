package com.sw.bi.scheduler.background.taskexcuter;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.model.ReportsQualityMonitor;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.service.GatewayService;
import com.sw.bi.scheduler.service.ReportsQualityMonitorService;
import com.sw.bi.scheduler.util.MailUtil;

import framework.commons.sender.MessagePlatform;
import com.sw.bi.scheduler.supports.MessageSenderAssistant;

/**
 * 报表质量监控任务. 可以设置检测表的总记录数,比较今天与昨天的总记录数,看是否波动异常 也可以设置检测指定维度下的,某个主要指标值的波动情况.
 * 注意定期清理: t_report_monitor_开头的临时表. 
 * nohup /opt/app/hive-0.7.0-rc1/bin/hive --service hiveserver 50031 & 
 * 最好用root账号去启动这个服务(错)，如果用root，则权限控制不住
 * nohup /opt/app/hive-0.9.0/bin/hive --service hiveserver 50031 & 
 * 
 * netstat -na -p |grep 50031
 * 
 * 
 * 
 * 
 * 在ODPS上面HIVE执行不下去，一直刷就是因为这个HIVE连接断了
 * 
 * 
 * d盘的hive-lib下的jar都要放到scheduler-lib下面 用HiveClient.java做测试
 * 
 * String con_url = "jdbc:hive://"+ip+":50031/"+database; 不仅仅支持default库，其他库也支持
 * Connection con = DriverManager.getConnection(con_url, "",""); 不需要用户名和密码
 * netstat -na -p |grep 50031 找端口的进程 netstat -na -p |grep 50031 |wc -l
 * 如果是1,表示进程还在. 如果是0,表示进程已经不在了. 做监控 如果执行SQL报错可能是因为hive-site中屏蔽了密码引起的.
 * 把hive-site.xml中关于mysql连接部分的注释打开即可
 */
public class ReportQualityExcuter extends AbExcuter {

	private static String driverName = "org.apache.hadoop.hive.jdbc.HiveDriver";

	// private SmsSender smsSender = BeanFactory.getSmsSender();
	// private SmsService smsService = BeanFactory.getBean(SmsService.class);
	private MessageSenderAssistant messageSender = new MessageSenderAssistant();
	private GatewayService gatewayService = BeanFactory.getService(GatewayService.class);

	public ReportQualityExcuter(Task task, String logFolder) {
		super(task, logFolder);
	}

	@Override
	public boolean excuteCommand() throws Exception {

		this.logFileWriter.write("针对作为ID为：" + this.currentAction.getJobId() + "的报表质量检测程序开始运行..." + "\r\n");
		this.logFileWriter.flush();
		ReportsQualityMonitorService reportsQualityMonitorService = BeanFactory.getService(ReportsQualityMonitorService.class);
		ReportsQualityMonitor reportsQualityMonitor = reportsQualityMonitorService.getByJobId(this.currentAction.getJobId());
		if (reportsQualityMonitor != null) {
			if (!reportsQualityMonitor.getMonitorTotalNumber() && !reportsQualityMonitor.getMonitorQuata()) {
				//不需要任何监控，直接返回，task执行成功
				this.logFileWriter.write("作业ID：" + this.currentAction.getJobId() + "的系统配置为：不需要检测总记录数,也不需要检测指标,检测程序退出..." + "\r\n");
				this.logFileWriter.flush();
				return true;
			} else {
				//加载hive jdbc驱动
				try {
					Class.forName(driverName);
					this.logFileWriter.write("加载hive驱动完成..." + "\r\n");
					this.logFileWriter.flush();
				} catch (ClassNotFoundException e) {
					throw e;
				}

				Connection connow = null;
				Connection conlast = null;
				Statement statenow = null;
				Statement statelast = null;
				ResultSet result_set_today = null;
				ResultSet result_set_yesterday = null;

				try {
					String hiveConnString = gatewayService.getHiveJDBC(); // Configure.property(Configure.REPORT_QUALITY_HIVE_CONNECTION_URL);
					List<QualityResultStruct> structList = new ArrayList<QualityResultStruct>();
					if (reportsQualityMonitor.getMonitorTotalNumber()) {
						connow = DriverManager.getConnection(hiveConnString, "", "");
						conlast = DriverManager.getConnection(hiveConnString, "", "");
						statenow = connow.createStatement();
						statelast = conlast.createStatement();

						this.logFileWriter.write("开始检测总记录数波动情况..." + "\r\n");
						this.logFileWriter.flush();
						String sqlnow = this.totalQuerySql(reportsQualityMonitor, 1);
						this.logFileWriter.write("当天总记录数查询语句：" + sqlnow + "\r\n");
						this.logFileWriter.flush();
						String sqllast = this.totalQuerySql(reportsQualityMonitor, 2);
						this.logFileWriter.write("昨天总记录数查询语句：" + sqllast + "\r\n");
						this.logFileWriter.flush();

						this.logFileWriter.write("开始执行hive查询,计算总记录数" + "\r\n");
						this.logFileWriter.flush();
						result_set_today = statenow.executeQuery(sqlnow);
						result_set_yesterday = statelast.executeQuery(sqllast);
						this.logFileWriter.write("hive查询执行完毕,计算总记录数" + "\r\n");
						this.logFileWriter.flush();

						QualityResultStruct result = this.checkTotal(reportsQualityMonitor.getUpAndDown().floatValue(), result_set_today, result_set_yesterday);
						if (result != null) {
							this.logFileWriter.write("记录总数波动异常" + "\r\n");
							this.logFileWriter.flush();
							structList.add(result);
						} else {
							this.logFileWriter.write("记录总数正常！！！" + "\r\n");
							this.logFileWriter.flush();
						}
						/*result_set_today.close();
						result_set_yesterday.close();
						statenow.close();
						statelast.close();
						connow.close();
						conlast.close();*/
					}
					if (reportsQualityMonitor.getMonitorQuata()) {
						connow = DriverManager.getConnection(hiveConnString, "", "");
						conlast = DriverManager.getConnection(hiveConnString, "", "");
						statenow = connow.createStatement();
						statelast = conlast.createStatement();

						this.logFileWriter.write("开始检测指标波动情况..." + "\r\n");
						this.logFileWriter.write("开始执行hive查询,计算指标波动" + "\r\n");
						this.logFileWriter.flush();
						String sqlnow = this.buildTempQuerySql(reportsQualityMonitor, 1, statenow);
						String sqllast = this.buildTempQuerySql(reportsQualityMonitor, 2, statelast);
						this.logFileWriter.write("hive查询执行完毕,计算指标波动" + "\r\n");
						this.logFileWriter.flush();
						result_set_today = statenow.executeQuery(sqlnow);
						result_set_yesterday = statelast.executeQuery(sqllast);
						List<QualityResultStruct> result = this.checkPart(this.quotaNum, result_set_today, result_set_yesterday);
						if (result != null && result.size() > 0) {
							this.logFileWriter.write("指标波动异常" + "\r\n");
							this.logFileWriter.flush();
							structList.addAll(result);
						} else {
							this.logFileWriter.write("指标正常！！！" + "\r\n");
							this.logFileWriter.flush();
						}
						/*result_set_today.close();
						result_set_yesterday.close();
						statenow.close();
						statelast.close();
						connow.close();
						conlast.close();*/
					}
					if (structList.size() > 0) {
						//如果有超过浮动率的数据，就进入告警
						this.logFileWriter.write("有" + structList.size() + "条记录浮动异常,进入告警" + "\r\n");
						this.logFileWriter.flush();
						this.logFileWriter.write("开始告警" + "\r\n");
						this.logFileWriter.flush();
						this.alert(reportsQualityMonitor, structList);
						this.logFileWriter.write("结束告警" + "\r\n");
						this.logFileWriter.flush();
					}
				} finally {
					if (result_set_today != null) {
						result_set_today.close();
					}

					if (result_set_yesterday != null) {
						result_set_yesterday.close();
					}

					if (statenow != null) {
						statenow.close();
					}

					if (statelast != null) {
						statelast.close();
					}

					if (connow != null && !connow.isClosed()) {
						connow.close();
					}

					if (conlast != null && !conlast.isClosed()) {
						conlast.close();
					}
				}
			}
		} else {
			this.logFileWriter.write("作业ID：" + this.currentAction.getJobId() + "对应的检测配置信息ReportsQualityMonitor为空..." + "\r\n");
			this.logFileWriter.flush();
		}
		return true;
	}

	/**
	 * 发出告警函数
	 * 
	 * @param reportsQualityMonitor
	 * @param structList
	 * @throws Exception
	 */
	private void alert(ReportsQualityMonitor reportsQualityMonitor, List<QualityResultStruct> structList) throws Exception {
		switch (reportsQualityMonitor.getAlertWay()) {
			case 0:
				this.sendMail(reportsQualityMonitor, structList);
				break;
			case 1:
				this.sendSMS(reportsQualityMonitor, structList);
				break;
			case 2:
				this.sendMail(reportsQualityMonitor, structList);
				this.sendSMS(reportsQualityMonitor, structList);
				break;
			default:
				return;
		}
	}

	/**
	 * 发送短信方法，目前只要有一条短信发送成功不会导致task失败
	 * 
	 * @param reportsQualityMonitor
	 * @param structList
	 * @throws Exception
	 */
	private void sendSMS(ReportsQualityMonitor reportsQualityMonitor, List<QualityResultStruct> structList) throws Exception {
		String SMSContent = this.getSmsString(reportsQualityMonitor);
		String[] phoneNums = reportsQualityMonitor.getMobilePhone().split(",");
		if (phoneNums.length > 0) {
			boolean result = false;
			for (String phoneNum : phoneNums) {
//				result |= messageSender.sendSms(phoneNum, SMSContent); // smsService.sendMsg(phoneNum, SMSContent); // com.sw.bi.scheduler.util.SmsAlert.sendSms(SMSContent, phoneNum, smsKey) >= 0;
				result |= messageSender.send(MessagePlatform.SMS_ADTIME,phoneNum, SMSContent);
				this.logFileWriter.write("发送短信:" + SMSContent + "至[" + phoneNum + "]" + (result ? "成功" : "失败") + "\r\n");
				this.logFileWriter.flush();
			}
			if (!result) {
				throw new Exception("SMS Send  Error");
			}
		}
	}

	/**
	 * 发送邮件
	 * 
	 * @param reportsQualityMonitor
	 * @param structList
	 * @throws MessagingException
	 * @throws IOException
	 */
	private void sendMail(ReportsQualityMonitor reportsQualityMonitor, List<QualityResultStruct> structList) throws MessagingException, IOException {
		String mailContent = this.getMailString(structList);
		String mailTitle = "报表质量监控_" + new SimpleDateFormat("yyyy年MM月dd日").format(this.getDateBefore(0)) + " 表名[";
		mailTitle += reportsQualityMonitor.getTablename() + "]" + "数据波动异常告警";

		MailUtil.send(reportsQualityMonitor.getEmail(), mailTitle, mailContent);
		this.logFileWriter.write("发送邮件:" + mailTitle + "至[" + reportsQualityMonitor.getEmail() + "]" + "\r\n");
		this.logFileWriter.write("邮件内容:" + mailContent + "\r\n");
		this.logFileWriter.flush();

		/*String[] mailAddresses = reportsQualityMonitor.getEmail().split(",");
		try {
			for (String address : mailAddresses) {
				MailUtil.send(address, mailTitle, mailContent);
				this.logFileWriter.write("发送邮件:" + mailTitle + "至[" + address + "]" + "\r\n");
				this.logFileWriter.write("邮件内容:" + mailContent + "\r\n");
				this.logFileWriter.flush();
			}
		} catch (AddressException e) {
			throw e;
		} catch (MessagingException e) {
			throw e;
		}*/
	}

	private String getSmsString(ReportsQualityMonitor reportsQualityMonitor) {
		return new SimpleDateFormat("yyyy年MM月dd日").format(this.getDateBefore(0)) + ",table[" + reportsQualityMonitor.getTablename() + "]数据波动异常,请检查!";
	}

	/**
	 * 邮件内容字符串获取方法
	 * 
	 * @param structList
	 * @return
	 */
	private String getMailString(List<QualityResultStruct> structList) {
		StringBuffer totalTable = new StringBuffer();
		StringBuffer othersTable = new StringBuffer();
		for (QualityResultStruct struct : structList) {
			if (struct.type == 0) {
				if (totalTable.length() == 0) {
					totalTable.append("<table border=1 style=\"" + Parameters.MailTableStyle + "\">");
					totalTable = this.initHeadRow(totalTable, struct);

					totalTable = this.initDataRow(totalTable, struct);

					totalTable.append("</table>");
					totalTable.append("</BR>");
				}

			} else if (struct.type == 1) {
				if (othersTable.length() == 0) {
					othersTable.append("<table border=1 style=\"" + Parameters.MailTableStyle + "\">");
					othersTable = this.initHeadRow(othersTable, struct);
				}
				othersTable = this.initDataRow(othersTable, struct);
			}
		}
		if (othersTable.length() > 0) {
			othersTable.append("</table>");
		}
		return totalTable.toString() + othersTable.toString();
	}

	/**
	 * 构造html表格头行(列名行)
	 * 
	 * @param table
	 * @param struct
	 * @return
	 */
	private StringBuffer initHeadRow(StringBuffer table, QualityResultStruct struct) {
		table.append("<tr style=\"" + Parameters.MailTableHeadRowStyle + "\">");
		table.append("<td>" + "昨日值" + "</td>");
		table.append("<td>" + "今日值" + "</td>");
		table.append("<td>" + "当前波动率" + "</td>");
		table.append("<td>" + "最大波动率" + "</td>");
		for (String key : struct.map.keySet()) {
			table.append("<td>" + key + "</td>");
		}
		table.append("</tr>");
		return table;
	}

	/**
	 * 构造html表格数据行
	 * 
	 * @param table
	 * @param struct
	 * @return
	 */
	private StringBuffer initDataRow(StringBuffer table, QualityResultStruct struct) {
		table.append("<tr>");
		table.append("<td>" + struct.lastValue + "</td>");
		table.append("<td>" + struct.nowValue + "</td>");
		table.append("<td>" + struct.changePersent + "</td>");
		table.append("<td>" + struct.quota + "</td>");
		for (String key : struct.map.keySet()) {
			table.append("<td>" + struct.map.get(key) + "</td>");
		}
		table.append("</tr>");
		return table;
	}

	/**
	 * 浮点数转百分比字符串
	 * 
	 * @param num数字
	 * @param deep保留小数位数
	 * @return
	 */
	private String getPersentString(Float num, int deep) {
		num = num * 100;
		String s = num.toString();
		int index = s.indexOf(".");
		if (index > 0 && s.length() > index + deep + 1) {
			s = s.substring(0, index + deep + 1);
		}
		s += "%";
		return s;

	}

	/**
	 * 总记录数浮动检查
	 * 
	 * @return
	 * @throws SQLException
	 * @throws IOException
	 */
	private QualityResultStruct checkTotal(Float quota, ResultSet rsn, ResultSet rsl) throws SQLException, IOException {
		ResultSetMetaData rsmn = rsn.getMetaData();
		Long nowCount = 0l;
		Long lastCount = 0l;
		while (rsn.next()) {
			nowCount = rsn.getLong("quota_n");
		}
		while (rsl.next()) {
			lastCount = rsl.getLong("quota_n");
		}
		this.logFileWriter.write("lastCount: " + lastCount + "   nowCount: " + nowCount + "\r\n");
		boolean needAlert = false;
		Float quotaNum = 0f;
		if (lastCount == 0 || nowCount == 0) {
			if (lastCount != nowCount) {
				needAlert = true;
				this.logFileWriter.write("比较对象中其中有一天数值为0\r\n");
			}
		} else {
			quotaNum = Math.abs(1 - nowCount.floatValue() / lastCount.floatValue());
			needAlert = quotaNum > quota;
			this.logFileWriter.write("总数波动率为:" + quotaNum + ",正常波动范围为:" + quota + " 结果：" + (quotaNum > quota ? "异常" : "正常") + "\r\n");
		}

		if (needAlert) {
			QualityResultStruct struct = new QualityResultStruct();
			struct.type = 0;
			struct.changePersent = this.getPersentString(quotaNum, 2);
			struct.lastValue = lastCount.toString();
			struct.nowValue = nowCount.toString();
			struct.quota = this.getPersentString(quota, 2);
			for (int i = 1; i < rsmn.getColumnCount(); i++) {
				String label;
				if (!(label = rsmn.getColumnLabel(i)).equals("quota_n")) {
					Object valuen = rsn.getObject(i);
					Object valuel = rsn.getObject(i);
					if (valuen != null && valuel != null) {
						String value;
						if ((value = valuen.toString()).equals(valuel.toString())) {
							struct.map.put(label, value);
						}
					}
				}
			}
			return struct;
		}
		return null;
	}

	/**
	 * 对数值比较大的指标浮动率进行检查
	 * 
	 * @param quota
	 *            浮动指标
	 * @param rsn
	 *            当前数据结果
	 * @param rsl
	 *            上一次的数据结果
	 * @return 浮动数值超过指标的每条数据转化为QualityResultStruct对象，返回结果集
	 * @throws SQLException
	 * @throws IOException
	 */
	private List<QualityResultStruct> checkPart(Float quota, ResultSet rsn, ResultSet rsl) throws SQLException, IOException {
		List<QualityResultStruct> list = new ArrayList<QualityResultStruct>();
		ResultSetMetaData rsmn = rsn.getMetaData();
		Map<String, Long> map = new HashMap<String, Long>();

		while (rsn.next()) {
			Long nowCount = rsn.getLong("quota_n");
			String key = "";
			for (int i = 1; i < rsmn.getColumnCount(); i++) {
				if (!(rsmn.getColumnLabel(i)).equals("quota_n")) {
					Object value = rsn.getObject(i);
					if (value != null) {
						key += value.toString() + " ";
					}
				}
			}
			map.put(key, nowCount);
		}
		while (rsl.next()) {
			String key = "";
			QualityResultStruct struct = new QualityResultStruct();
			for (int i = 1; i < rsmn.getColumnCount(); i++) {
				String label;
				if (!(label = rsmn.getColumnLabel(i)).equals("quota_n")) {
					Object value = rsl.getObject(i);
					if (value != null) {
						struct.map.put(label, value.toString());
						key += value.toString() + " ";
					}
				}
			}
			Long nowCount;
			if ((nowCount = map.get(key)) != null) {
				Long lastCount = rsl.getLong("quota_n");
				boolean needAlert = false;
				if (lastCount == 0 || nowCount == 0) {
					if (lastCount != nowCount) {
						needAlert = true;
						this.logFileWriter.write("属性:" + key + "的比较值中有一个值为0" + "\r\n");
					}
				} else {
					Float quotaNum = Math.abs(1 - nowCount.floatValue() / lastCount.floatValue());
					needAlert = quotaNum > quota;
				}

				if (needAlert) {
					Float quotaNum = Math.abs(1 - nowCount.floatValue() / lastCount.floatValue());
					this.logFileWriter.write("属性:" + key + "的波动率为:" + quotaNum + ",正常波动范围为:" + quota + " 结果：" + (quotaNum > quota ? "异常" : "正常") + "\r\n");
					struct.type = 1;
					struct.changePersent = this.getPersentString(quotaNum, 2);
					struct.lastValue = lastCount.toString();
					struct.nowValue = nowCount.toString();
					struct.quota = this.getPersentString(quota, 2);
					list.add(struct);
				}
			} else {
				this.logFileWriter.write("属性:" + key + "不在比较结果集的前100条数据内" + "\r\n");
			}
		}
		return list;
	}

	/**
	 * 分区过滤条件生成方法
	 * 
	 * @param ptName
	 *            分区名
	 * @param ptType
	 *            分区命名类型，仅支持"yyyyMMdd"、"yyyyMMddHH"
	 * @param dayBefor
	 *            要查询的为dayBefore天的数据
	 * @return
	 * @throws Exception
	 */
	private String initPtFilter(String ptName, String ptType, int dayBefore) throws Exception {
		String DateString = new SimpleDateFormat("yyyyMMdd").format(this.getDateBefore(dayBefore));

		String ptFilter = "";
		if (ptType.equals("yyyyMMdd")) {
			ptFilter = ptName + "= '" + DateString + "'";
		} else if (ptType.equals("yyyyMMddHH")) {
			ptFilter = ptName + ">= '" + DateString + "00" + "' and ";
			ptFilter += ptName + "<= '" + DateString + "23" + "' ";
		} else {
			throw new Exception("Unknow ptType");
		}
		return ptFilter;
	}

	/**
	 * 查询总数的sql语句生成
	 * 
	 * @param monitor
	 * @param dayBefore
	 * @return
	 * @throws Exception
	 */
	private String totalQuerySql(ReportsQualityMonitor monitor, int dayBefore) throws Exception {

		String ptFilter = this.initPtFilter(monitor.getPtName(), monitor.getPtType(), dayBefore);
		String sql = "select count(1) as quota_n";
		sql += " from " + monitor.getTablename();
		sql += " where " + ptFilter;

		return sql;
	}

	private Float quotaNum;

	/**
	 * 构建sql,并执行生成临时表后，返回查询该临时表的sql语句
	 * 
	 * @param monitor
	 * @param dayBefore
	 * @return
	 * @throws Exception
	 */
	private String buildTempQuerySql(ReportsQualityMonitor monitor, int dayBefore, Statement state) throws Exception {
		String ptFilter = this.initPtFilter(monitor.getPtName(), monitor.getPtType(), dayBefore);
		String DateString = new SimpleDateFormat("yyyyMMdd").format(this.getDateBefore(dayBefore));
		// 注： 这个临时表名前没有加库名,可能会把表建立到default库,目前tools账号是有权限在default下建表的。
		// 暂时先这样，反正这些临时表只被用一次，放tools库比较碍眼,放到目前没有使用的default库也好
		String tempName = "t_report_monitor_" + this.currentAction.getActionId() + "_" + DateString;
		String[] args = monitor.getQuata().split("=");
		if (args.length != 2) {
			throw new Exception("Error quota String");
		}
		String quota = args[0];
		quotaNum = new Float(args[1]);
		String innerSql = "CREATE  TABLE if not exists " + tempName + " as select ";
		String[] attributes = monitor.getWeidu().split(",");

		for (String attr : attributes) {
			innerSql += attr + ",";
		}

		innerSql += " sum(" + quota + ") as quota_n ";
		innerSql += " from " + monitor.getTablename();
		innerSql += " where " + ptFilter;
		innerSql += " group by ";
		for (String attr : attributes) {
			innerSql += attr + ",";
		}
		innerSql = innerSql.substring(0, innerSql.length() - 1);
		innerSql += " order by quota_n desc limit 100";
		this.logFileWriter.write(DateString + "临时表创建语句: " + innerSql + "\r\n");

		state.execute(innerSql);
		return "select * from " + tempName;
	}

	private Date getDateBefore(int day) {
		Date date = this.currentTask.getTaskDate();
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		calendar.add(Calendar.DATE, 0 - day);
		return calendar.getTime();
	}

	/**
	 * 容错率错误的记录临时转化该数据格式，根据告警方式再根据该格式转化为告警内容。
	 * 
	 * @author dly
	 * 
	 */
	class QualityResultStruct {

		//0:总数比较结果 1：抽样比较结果
		public int type;

		public String quota;
		//上次（前天）取值
		public String lastValue;
		//当前值
		public String nowValue;
		//浮动率 abs(1 - asb(nowvalue/lastValue)) * 100%
		public String changePersent;

		//维度对应的属性和属性值
		public Map<String, String> map = new HashMap<String, String>();
	}

}
