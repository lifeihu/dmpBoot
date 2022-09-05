package com.sw.bi.scheduler.background.taskexcuter;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.background.util.DxDESCipher;
import com.sw.bi.scheduler.model.Datasource;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.service.DatasourceService;

/* scheduler9中创建了下面这个存储过程做测试.
 * 
 * 
create procedure myProcTest()
  begin
		  DROP TABLE IF EXISTS `myproc`;
		
		CREATE TABLE `myproc` (
		  `task_create_log_id` bigint(20) NOT NULL AUTO_INCREMENT,
		  `task_date` date NOT NULL,
		  `create_success` tinyint(1) DEFAULT '0',
		  `run_success` tinyint(1) DEFAULT '0',
		  `create_time` datetime NOT NULL,
		  `update_time` datetime DEFAULT NULL,
		  PRIMARY KEY (`task_create_log_id`),
		  KEY `idx_task_date` (`task_date`)
		) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
  
  end;

create procedure ProcTest(in riqi varchar(20))
  begin
        
        
		DROP TABLE IF EXISTS procTest;
		
		CREATE TABLE procTest (
		  `task_create_log_id` bigint(20) NOT NULL AUTO_INCREMENT,
		  `task_date` varchar(20) NOT NULL,
		  `create_success` tinyint(1) DEFAULT '0',
		  PRIMARY KEY (`task_create_log_id`),
		  KEY `idx_task_date` (`task_date`)
		) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
		
		insert into procTest(task_date,create_success) values(riqi,0);
  
  end;
  
  call ProcTest("2011-06-01")
  
*/

public class ProcedureExcuter extends AbExcuter {

	public ProcedureExcuter(Task task, String logFolder) {
		super(task, logFolder);
	}

	@Override
	public boolean excuteCommand() throws Exception {
		Connection conn = null;
		CallableStatement cstmt = null;
		

		

		try {
			//  ${date_desc};${hour_desc};90
			//  ${date_desc}
			//  @total;${date_desc};90
			//  job表的parameters字段存放存储过程的参数.  下面是实现动态参数的替换
			String[] params = null;
			Object[] procParams = null;
			String jobParams = currentJob.getParameters();
			if (jobParams != null && jobParams.length() > 0) {
				this.logFileWriter.write("存储过程的参数: " + jobParams + "\r\n");
				params = jobParams.split(";");
				procParams = new Object[params.length];
				for (int i = 0; i < params.length; i++) {
					// Parameters.getRunTimeParamter(this.currentTask).get(params[i]);
					String value = this.runtimeParamters.get(params[i]);
					if (value != null) {
						procParams[i] = value;
					} else {
						procParams[i] = params[i];
					}
				}
			} else {
				this.logFileWriter.write("存储过程没有参数" + "\r\n");
			}

			// [url=jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=utf8];[name=root];[password=root];[procname=aa]
			//  因为存储过程类型的作业数量不多,而且今后也都不再新上存储过程的作业了. 所以这里的数据库配置信息就暂时不录到datasource表了.
			//  为了简化起见,就直接保存到job记录中的program_path字段之中
			String[] programPaths = currentJob.getProgramPath().split(";");

			//  getProgramPath()------------5;aa
			//  datasourceid=5  procedureName=aa
			//  根据datasourceid去查一下datasource表,最后拼出如下字符串
			//  [url=jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=utf8];[name=root];[password=root];[procname=aa]
			DatasourceService datasourceService = BeanFactory.getService(DatasourceService.class);
			Datasource datasource = datasourceService.get(Long.valueOf(programPaths[0]));

			String url = datasource.getConnectionString();
			String username = datasource.getUsername();
			String password = DxDESCipher.DecryptDES(datasource.getPassword(), datasource.getUsername());
			String procname = programPaths[1];

			
            //2015.5.18 这里暂时写死了。 如果需要支持多种存储过程，只需要在这里做一个判断，根据数据源的类型加载不同的驱动程序就可以了
			Class.forName("oracle.jdbc.driver.OracleDriver");
			
			// Connection con = DriverManager.getConnection(url, username, password);
			conn = DriverManager.getConnection(url, username, password);

			String sql = "call " + procname + "(";
			if (procParams != null) {
				for (Object param : procParams) {
					sql += param + ",";
				}
				if (procParams.length > 0) {
					sql = sql.substring(0, sql.length() - 1);
				}
			}
			sql += ")";

			sql = "{ " + sql + " }";
			this.logFileWriter.write("调用的语句:  " + sql + "\r\n");

			// CallableStatement c = con.prepareCall(sql);
			cstmt = conn.prepareCall(sql);

			this.logFileWriter.write("begin: 开始执行存储过程" + "\r\n");
			boolean exec_result = cstmt.execute(); // c.execute();
			this.logFileWriter.write("end: 存储过程执行完毕" + "\r\n");
			//CallableStatement.execute() 返回： 
			//如果第一个结果是 ResultSet对象，则返回 true；如果第一个结果是更新计数或者没有结果，则返回 false 
			//所以在这里,返回false并不代表执行失败
			//抛出： 
			//SQLException - 如果发生数据库访问错误
			return true;
		} catch (Exception e) {
			this.logFileWriter.write("存储过程执行器发生异常!请检查!\r\n");
			throw e;

		} finally {
			if (cstmt != null) {
				cstmt.close();
			}

			if (conn != null && !conn.isClosed()) {
				conn.close();
			}
		}
	}
	
	
	
	// 用java调用oracle存储过程总结
	// 调用方式参考  http://www.cnblogs.com/rootq/articles/1100086.html
	// 页面参考            MySQLProcedureMaintainModule.js

	public static void main(String args[]) throws SQLException {

		String url = "jdbc:mysql://192.168.181.96:3306/scheduler9?yearIsDateType=false&amp;useUnicode=true&amp;characterEncoding=utf-8";
		String username = "hive_user";
		String password = "abc#123";
		Connection con = DriverManager.getConnection(url, username, password);

		String sql = "{call myProcTest()}";
		CallableStatement c = con.prepareCall(sql);

		System.out.println(c.execute());
	}

}
