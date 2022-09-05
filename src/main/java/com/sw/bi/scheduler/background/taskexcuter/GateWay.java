package com.sw.bi.scheduler.background.taskexcuter;

public class GateWay {	
	 private  static String defaultHost = "127.0.0.1";
     public static String getLowestHost()
     {      	   
	   String load_lowest_gateway = null;
	   String load_lowest = null;
	   try
	   {
    	   String s = GetPageStr.getWebContentGetMethod("http://172.16.15.120/ganglia/?p=2&c=shunwang-hadoop-cluster&h=&hc=4&p=1", "gbk");
   		   String[] sss=StringProcessor.midString(s, "<table cellspacing=5 border=0>", "<table border=0>");
		   String info = sss[0];
		   String[] sa = StringProcessor.splitString(info, "<table width=\"100%\" cellpadding=1 cellspacing=0 border=0>");
		   for (int i = 1; i < sa.length;i++) {
               s = sa[i];
			   sss=StringProcessor.midString(s, "h=", "\">");
			   String hostname = sss[0];
			   sss=StringProcessor.midString(sss[1], "<small>", "</small>");
			   String load = sss[0];
			   
			   //排除
			   if(hostname.equals("172.16.15.233")){
				   continue;
			   }
			   
			   if(load_lowest==null){
				   load_lowest_gateway = hostname;
				   load_lowest = load;
			   }
			   
			   if(load_lowest!=null&&Double.parseDouble(load)<Double.parseDouble(load_lowest)){
				   load_lowest_gateway = hostname;
				   load_lowest = load;
			   }
		   }
	   }
	   catch(Exception e)
	   {
		   
	   }
	   if(load_lowest_gateway == null)
	   {
		   return defaultHost;
	   }
	   return load_lowest_gateway;
     }
}
