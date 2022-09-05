package com.sw.bi.scheduler.background.taskexcuter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.resolver.Warning;

import com.sw.bi.scheduler.background.taskexcuter.Parameters.BooleanResult;
import com.sw.bi.scheduler.background.taskexcuter.Parameters.HiveSqlRunMode;
import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.model.ActionSql;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.model.User;
import com.sw.bi.scheduler.model.UserGroup;
import com.sw.bi.scheduler.service.ActionSqlService;
import com.sw.bi.scheduler.service.OperateLoggerService;
import com.sw.bi.scheduler.service.UserGroupRelationService;
import com.sw.bi.scheduler.service.UserService;
import com.sw.bi.scheduler.util.Configure;

/**
 * 
 * 说明: hivesql脚本书写有一个约定的规范: add jar ...; CREATE TEMPORARY FUNCTION...; add
 * file...等必须放在脚本头部,并且最后用两个分号作为结尾标识符;;
 * 
 */
public class HiveSqlExcuter extends AbExcuter {
	private String tmp_sqlPath;

	private static final Pattern DROP_DATABASE_PATTERN = Pattern.compile("drop database", Pattern.CASE_INSENSITIVE);

	private static final Pattern USE_DATABASE_PATTERN = Pattern.compile("use\\s+(\\w+)", Pattern.CASE_INSENSITIVE);

	// HiveSQL中解析数据库的正则表达式
	private static final Pattern DATABASE_PATTERN = Pattern.compile("\\s+(into|table|from|join)\\s+((`?\\w+`?)\\.){1,}", Pattern.CASE_INSENSITIVE);

	private User user;

	/**
	 * 作业责任人所属用户组
	 */
	private UserGroup userGroup;

	@Override
	public boolean excuteCommand() throws InterruptedException, IOException {
		UserGroupRelationService userGroupRelationService = BeanFactory.getService(UserGroupRelationService.class);

		userGroup = userGroupRelationService.getUserGroupByUser(currentJob.getDutyOfficer());

		if (userGroup == null) {
			throw new Warning("无法执行 " + currentTask + ", 未能识别责任人的用户组");
		}

		ActionSqlService actionSqlService = BeanFactory.getService(ActionSqlService.class);
		UserService userService = BeanFactory.getService(UserService.class);

		/**
		 * 责任人
		 */
		user = userService.get(currentJob.getDutyOfficer());

		tmp_sqlPath = Parameters.tempSqlPath + this.currentAction.getActionId() + "/";
		String path = currentJob.getProgramPath(); //完整的hivesql脚本全路径
		try {
			boolean actionResult = true;
			Queue<SqlLogStruct> logs = this.createTmpHiveSql(path);
			if (logs.size() == 0)
				return false;
			do {
				SqlLogStruct log = logs.poll(); //队列是先进先出的
				Timestamp nowStamp = new Timestamp((new Date().getTime()));
				log.getActionSql().setBeginTime(nowStamp); //小段SQL的开始执行时间
				log.getActionSql().setCreateTime(nowStamp);//小段SQL的创建时间

				//执行小段SQL代码
				Process process = this.programeRun(log.getTempFilePath()); // log.getTempFilePath()就是小段SQL的临时脚本全路径
				process.waitFor();
				actionResult &= (process.exitValue() == 0);

				nowStamp = new Timestamp((new Date().getTime()));
				log.getActionSql().setEndTime(nowStamp); //小段SQL的结束执行时间
				long usedTime = (nowStamp.getTime() - log.getActionSql().getBeginTime().getTime()) / 1000; //小段SQL的运行时长
				log.getActionSql().setRunTime(usedTime);
				log.getActionSql().setRunResult(BooleanResult.valueOf(actionResult).indexOf()); // 小段SQL的运行结果状态

				if (user != null) {
					log.getActionSql().setDutyMan(user.getRealName()); //责任人
				}

				//保存actionSql对象。朝actionsql表插入一条记录,记录该小段SQL的执行情况
				actionSqlService.saveOrUpdate(log.getActionSql());
			}
			while (actionResult && logs.size() > 0);

			File tmphivesql = new File(tmp_sqlPath);
			if (tmphivesql.exists()) {
				tmphivesql.delete();
			}

			return actionResult;

		} catch (IOException e) {
			throw e;
		} catch (InterruptedException e) {
			throw e;
		}
	}

	public HiveSqlExcuter(Task task, String logFolder) {
		super(task, logFolder);
	}

	//判断是否为断点继续执行任务
	private boolean isBreakContine() {
		if (HiveSqlRunMode.BREAKCONTINE.indexOf() == this.currentTask.getNeedContinueRun()) {
			return true;
		}
		return false;
	}

	private Process programeRun(String tempSqlPath) throws IOException {
		String[] commands = new String[] { "/bin/bash", "-c", Configure.property(Configure.HIVE_HOME) + "hiveclient -f " + tempSqlPath + " >> " + this.getLogPathName() + " 2>&1" };
		Process process = Runtime.getRuntime().exec(commands);
		return process;
	}

	//hivesql 头部，引用jar包、临时表建立等操作，作用于所有的sqlPart中
	private String headPart = "";

	/**
	 * 作用：对hivesql的脚本进行解析。 要执行的每段SQL存放在一个list中。 以便后续可以在循环中一段一段的调用hiveclient -f
	 * 
	 * @param sqlPath
	 *            传入进去的参数是hivesql的脚本全路径
	 * @return 经过该方法的处理, 脚本中符号;;前面的部分被保存在String headPart中。
	 *         其余的sql代码以;作为分隔符,被保存在List<String> sqlPartList中。
	 * @throws IOException
	 */
	private List<String> getSqlPart(String sqlPath) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(sqlPath), "utf-8"));
		List<String> sqlPartList = new ArrayList<String>();
		String line = "";
		StringBuffer contentBuffer = new StringBuffer();
		while ((line = reader.readLine()) != null) {
			if (!line.trim().startsWith("--")) {
				contentBuffer.append(line + "\n");
				if (line.trim().endsWith(";")) {
					sqlPartList.add(contentBuffer.toString());
					contentBuffer.delete(0, contentBuffer.length());
				}
				//";;"分隔符用于分隔头部和sql部分
				if (line.trim().endsWith(";;") && headPart.length() == 0) {
					for (String part : sqlPartList) {
						headPart += part;
					}
					headPart = headPart.substring(0, headPart.lastIndexOf(";")); //  ;;前面的公共部分的头代码
					sqlPartList.clear();
				}
			}
		}
		reader.close();
		return sqlPartList; //除了头代码以外的SQL代码
	}

	/**
	 * 对sqlPartList进行循环,然后对每一段SQL代码段中的动态参数进行替换
	 * 
	 * @param sqlPartList
	 * @return
	 */
	private List<String> sqlFilter(List<String> sqlPartList) {
		// java.util.Map<String, String> map = Parameters.getRunTimeParamter(currentTask);
		List<String> list = new ArrayList<String>();
		for (String sqlPart : sqlPartList) {
			list.add(this.replaceParams(sqlPart));
			/*for (String s : map.keySet()) {
				sqlPart = sqlPart.replace(s, map.get(s));
			}
			list.add(sqlPart);*/
		}
		return list;
	}

	/**
	 * 创建临时的去掉注释并替换参数的hivesql
	 * 
	 * @param sqlPath
	 *            源文件路径
	 * @return 临时文件路径
	 * @throws IOException
	 */
	private Queue<SqlLogStruct> createTmpHiveSql(String sqlPath) throws IOException {
		File tmphivesql = new File(tmp_sqlPath);
		if (tmphivesql.exists()) {
			tmphivesql.delete();
		}
		tmphivesql.mkdirs();

		List<String> sqlPartList = this.getSqlPart(sqlPath);

		// 校验公共头部分的SQL
		this.validateUserGroupDatabase(this.headPart);

		sqlPartList = this.sqlFilter(sqlPartList); //动态参数替换
		Queue<SqlLogStruct> pathQueue = new java.util.ArrayDeque<SqlLogStruct>();
		int beginIndex = 1;

		//System.out.println("this.isBreakContine(): " + this.isBreakContine()); // 断点 1
		//System.out.println("1:  " + beginIndex);

		if (this.isBreakContine()) {
			ActionSqlService service = BeanFactory.getService(ActionSqlService.class);
			beginIndex = (int) service.getLastSqlIndex(this.currentTask.getLastActionIdForBreakpoint()); //getLastActionIdForBreakpoint
		}
		//System.out.println("2:   " + beginIndex);

		//循环,一段一段生成SQL代码段的临时脚本文件
		for (int i = beginIndex; i <= sqlPartList.size(); i++) {
			String partPath = tmp_sqlPath + i + ".sql";
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(partPath), "utf-8"));
			String sqlText = sqlPartList.get(i - 1);

			// 校验每段SQL中是否含有无权操作的Hive数据库
			this.validateUserGroupDatabase(sqlText);

			//如果有head部分，写入文件时添加head(每一段SQL代码前面都加上公共部分的代码)
			if (this.headPart.length() > 0) {
				writer.write(this.headPart + " \n" + sqlText);
			} else {
				writer.write(sqlText);
			}

			writer.close();

			ActionSql as = new ActionSql();
			as.setAction(this.currentAction);
			as.setSqlIndex(i);
			as.setHiveSqlPath(currentJob.getProgramPath());
			as.setSqlString(sqlText);
			as.setTaskDate(this.currentTask.getTaskDate());

			pathQueue.add(new SqlLogStruct(partPath, as));
		}
		return pathQueue;
	}

	/**
	 * 校验指定SQL中是否包含责任人所在用户组无权操作的Hive数据库
	 * 
	 * @param sql
	 * @return
	 */
	private boolean validateUserGroupDatabase(String sql) {
		// 超级用户组无需校验Hive数据库
		if (userGroup.isAdministrator()) {
			return true;
		}

		String userGroupDbname = userGroup.getHiveDatabase();
		String message = currentTask + " 脚本中含有责任人(" + user.getRealName() + ")无权操作的数据库(%1$s)";

		try {
			Matcher matcher = DROP_DATABASE_PATTERN.matcher(sql);
			if (matcher.find()) {
				throw new Warning(currentTask + " 脚本中含有被禁止的DROP DATABASE语句");
			}

			matcher = USE_DATABASE_PATTERN.matcher(sql);
			while (matcher.find()) {
				String dbname = matcher.group(1);
				if (!dbname.equals(userGroupDbname)) {
					throw new Warning(String.format(message, dbname));
				}
			}

			matcher = DATABASE_PATTERN.matcher(sql);
			while (matcher.find()) {
				String dbname = matcher.group(3);
				if (!dbname.equals(userGroupDbname)) {
					throw new Warning(String.format(message, dbname));
				}
			}
		} catch (Warning e) {
			log("越权SQL: " + sql);
			BeanFactory.getService(OperateLoggerService.class).logUnauthorized(e.getMessage());

			throw e;
		}

		return true;
	}

	class SqlLogStruct {

		private String tempFilePath;

		private ActionSql actionSql;

		private boolean status;

		/**
		 * 
		 * @param tempFilePath
		 *            单段SQL的临时hivesql脚本全路径
		 * @param as
		 *            该单端SQL执行时产生的日志保存为actionSql对象
		 */
		public SqlLogStruct(String tempFilePath, ActionSql as) {
			this.tempFilePath = tempFilePath;
			this.actionSql = as;
			this.status = false;
		}

		public void setTempFilePath(String tempFilePath) {
			this.tempFilePath = tempFilePath;
		}

		public String getTempFilePath() {
			return tempFilePath;
		}

		public void setActionSql(ActionSql actionSql) {
			this.actionSql = actionSql;
		}

		public ActionSql getActionSql() {
			return actionSql;
		}

		public void setStatus(boolean status) {
			this.status = status;
		}

		public boolean isStatus() {
			return status;
		}

	}
}
