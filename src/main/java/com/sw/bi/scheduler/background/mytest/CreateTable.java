package com.sw.bi.scheduler.background.mytest;


public class CreateTable {

	
	  private static String t = "CREATE TABLE if not exists scheduler_testjob_AAAAA(\n"
      +"        a               string,\n"
      +"        b               string,\n"
      +"        c               string)\n"
      +"PARTITIONED BY (pt STRING)\n"
      +"row format delimited\n"
      +"fields terminated by '\\\"'\n"
      +"lines terminated by '\\n'\n"
      +"STORED AS TEXTFILE;\n";
	  
	  
	  
	  
	  
	  
	  public static void main(String args[]){
		  
		  String aa = "";
          for(int i=1;i<=5000;i++){
        	  aa+=replaceBy(t,String.valueOf(i));
          }
          System.out.println(aa);
/*          try {
			//DxFileUtils.string2File(aa, "c:/a.txt", "utf-8");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	  }
	
	
	  
	  public static String replaceBy(String t,String i){
		   String return_value = t.replace("_AAAAA", i);
		  
		   return return_value;
	  }
	
	
	
	
	
	
}
