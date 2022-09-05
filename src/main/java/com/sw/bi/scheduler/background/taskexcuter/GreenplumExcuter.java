package com.sw.bi.scheduler.background.taskexcuter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.util.StringUtils;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.background.util.DxDESCipher;
import com.sw.bi.scheduler.model.Datasource;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.service.DatasourceService;

/**
 * GP函数执行器(出于性能考虑大数据都建议通过gpfdist服务导入GP)
 * 
 * @author shiming.hong
 */
public class GreenplumExcuter extends AbExcuter {

	public GreenplumExcuter() {}

	public GreenplumExcuter(Task task, String logFolder) {
		super(task, logFolder);
	}

	@Override
	public boolean excuteCommand() throws Exception {
		DatasourceService datasourceService = BeanFactory.getService(DatasourceService.class);

		String programPath = currentJob.getProgramPath();
		int pos = programPath.indexOf(";");

		Datasource datasource = datasourceService.get(Long.parseLong(programPath.substring(0, pos)));
		log("GP数据源: " + datasource.getName());

		String funcName = programPath.substring(pos + 1);
		String jobParams = currentJob.getParameters();
		if (StringUtils.hasText(jobParams)) {
			String funcParams = this.replaceParams(jobParams);
			funcName += "(" + funcParams + ")";

			log("函数(" + funcName + ")参数: " + jobParams + " -> " + funcParams);
		} else {
			funcName += "()";

			log("函数(" + funcName + ")没有参数.");
		}

		// 函数调用SQL
		String sql = "select " + funcName;
		log("函数调用SQL： " + sql);

		Class.forName("org.postgresql.Driver");
		String password = DxDESCipher.DecryptDES(datasource.getPassword(), datasource.getUsername());
		Connection connection = DriverManager.getConnection(datasource.getConnectionString(), datasource.getUsername(), password);

		Statement stmt = null;
		try {
			stmt = connection.createStatement();

			log("开始执行GP函数...");

			stmt.execute(sql);

			log("GP函数执行完毕.");

			return true;

		} catch (SQLException e) {
			e.printStackTrace();
			log("GP函数执行器发生异常");
			throw e;

		} finally {
			if (stmt != null) {
				stmt.close();
			}

			if (connection != null && !connection.isClosed()) {
				connection.close();
			}
		}
	}

	public static void main(String[] args) throws Exception {
		GreenplumExcuter excuter = new GreenplumExcuter();
		excuter.excuteCommand();
	}

}
