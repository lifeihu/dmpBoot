package com.sw.bi.scheduler.background.mytest;

public class TestInsert {
	
	
	public static void main(String args[]){
		String v = "";
		for(int i=4500;i<5000;i++){
			String a = "insert into etl_clean_config(table_name,partition_name,partition_type,apply_man,keep_days,create_time,update_time) values('scheduler_testjob"+i+"','pt','yyyyMMdd',1,3,'2012-02-21 12:33:21','2012-02-21 12:33:21');\n";
		    v+=a;
		}
		
		System.out.println(v);
		
		
		
	}
	
	
	
	
	
	
	

}
