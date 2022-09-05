package com.sw.bi.scheduler.background.javatype.createjob_for_gp;

public class Test {
	
	
	public static void main(String args[]){
		
		 String a = "insert into rp_hi_flow_client_nvidia_m (day_id,";
		 int index1 = a.indexOf("(");
		 int index2 = a.indexOf("insert into ");
		 String b = a.substring(index2+12, index1).trim();
		 System.out.println(b);
		
		
		String aa = "delete from rpd_vip_mon_lev_detail_d where day_id=cast('${date_desc}' as date)";
		if(aa.indexOf(" rpd_")>=0){
			System.out.println("==");
			aa=aa.replaceFirst(" rpd_", " dmn.rpd_");
		}
		System.out.println(aa);
		
	}
	
	
	
	
	
	
	
	
	
	

}
