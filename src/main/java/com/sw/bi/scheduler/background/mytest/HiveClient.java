package com.sw.bi.scheduler.background.mytest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
 
//  
 //  nohup /opt/app/hive-0.7.0-rc1/bin/hive --service hiveserver 50031 & 
 

//   netstat -na -p |grep 50031


//  ��scheduler_test����ű� ������scheduler����ű��� ���ߵ������,scheduler����ű����scheduler.jar�������ص�������
//    ./scheduler_test jar   /home/tools/scheduler/scheduler_test.jar  com.sw.bi.scheduler.background.mytest.HiveClient 172.16.15.225 tools dual 
//./scheduler_test jar   /home/tools/scheduler/scheduler_test.jar  com.sw.bi.scheduler.background.mytest.HiveClient 192.168.181.210 lifeng dual 
public class HiveClient {
	private static String driverName = "org.apache.hadoop.hive.jdbc.HiveDriver";

	public static void main(String[] args) throws SQLException {
		//String ip = args[0];
		//String database = args[1];
		//String table = args[2];
		String ip = "192.168.181.210";
		String database = "default";
	  
		try {
			Class.forName(driverName);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
        String con_url = "jdbc:hive://"+ip+":50031/"+database;
        System.out.println(con_url);
		Connection con = DriverManager.getConnection(con_url, "","");
		Statement stmt = con.createStatement();
		//String sql = "select * from "+database+"."+table+" limit 10";
		//String sql = "select * from "+table+" limit 10";
		//String sql = "create table "+database+".ghgh as select s from dual";
		String sql = "select * from test_chi";
		System.out.println(sql); 
		stmt.execute(sql);
/*		ResultSet res = stmt.executeQuery(sql);
		while (res.next()) {
			//System.out.println(res.getString(1));
		}*/
		
		stmt.close();
		con.close();
	}
}
