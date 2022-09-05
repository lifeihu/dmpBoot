package com.sw.bi.scheduler.background.mytest;

import java.io.IOException;

import com.sw.bi.scheduler.background.taskexcuter.xml.DxFileUtils;


public class DropTable {

	
	  private static String t = "drop TABLE   scheduler_testjob_AAAAA;\n";
	  
	  
	  
	  
	  
	  
	  public static void main(String args[]){
		  
		  String aa = "";
          for(int i=1;i<=5000;i++){
        	  aa+=replaceBy(t,String.valueOf(i));
          }
          System.out.println(aa);
          try {
			DxFileUtils.string2File(aa, "c:/a.txt", "utf-8");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  }
	
	
	  
	  public static String replaceBy(String t,String i){
		   String return_value = t.replace("_AAAAA", i);
		  
		   return return_value;
	  }
	
	
	
	
	
	
}
