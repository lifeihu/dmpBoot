package com.sw.bi.scheduler.background.mytest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

//     Hive JDBC
//     先申请kerberos证书,再执行下面这行代码
//     nohup  /opt/app/hive-0.7.0-rc1/bin/hive --service hiveserver 50031 >/dev/null 2>/dev/null &
//     datanode2为启动hiveserver的服务器名,50031为端口, default为数据库,目前只支持default数据库.  不需要用户名和密码
//     netstat -na -p |grep 50031 找端口的进程
//     netstat -na -p |grep 50031 |wc -l  如果是1,表示进程还在. 如果是0,表示进程已经不在了. 做监控
//     如果执行SQL报错可能是因为hive-site中屏蔽了密码引起的. 把hive-site.xml中关于mysql连接部分的注释打开即可


//   ./scheduler jar scheduler.jar com.sw.bi.scheduler.background.mytest.HiveJdbc


public class HiveJdbc {
	private static String driverName = "org.apache.hadoop.hive.jdbc.HiveDriver";

	public static void main(String[] args) throws SQLException {
		try {
			Class.forName(driverName);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
		Connection con = DriverManager.getConnection("jdbc:hive://192.168.181.209:50031/default", "", "");
		Statement stmt = con.createStatement();
		 
	
		try{
			String sql = "select 'a' from dual";
			ResultSet res = stmt.executeQuery(sql);
/*			System.out.println("客户端输出....");
			while (res.next()) {
			   System.out.println(res.getString(1));
			}
			
			res.close();*/
		}catch(Exception e){
			System.out.println(e.getMessage());
			for (StackTraceElement stack : e.getStackTrace()) {
				System.out.println(stack.toString());
			}
		}
	}
}
