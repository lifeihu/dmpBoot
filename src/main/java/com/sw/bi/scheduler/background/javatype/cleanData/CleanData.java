package com.sw.bi.scheduler.background.javatype.cleanData;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.sw.bi.scheduler.util.DateUtil;
import com.sw.bi.scheduler.util.ExecAgent.ExecResult;
import com.sw.bi.scheduler.util.SshUtil;


//   /home/tools/scheduler/scheduler_test jar   /home/tools/scheduler/scheduler_test.jar com.sw.bi.scheduler.background.javatype.cleanData.CleanData 20130820 7 /home/tools/test/test1/,/home/tools/test/test2/,/home/tools/test/test3/ 
/**
 * 删除指定目录下面7天以前的数据目录
 * @author feng.li
 * 2013-08-20
 */
public class CleanData {

	  public static void main(String args[]){
		  //第一个参数是${date_desc}
		  //第二个参数是保留的数据目录的天数，如7
		  //第三个参数是要进行处理的数据目录，用逗号分隔，如果下次有新增加，请到对应的任务，修改参数
		  String date_desc = args[0];
		  String days = args[1];
		  String dirs = args[2];
		  Map day_map = new HashMap();
		  ////String date_desc = "20130820";
		  ////String days = "7";
		  ////String dirs = "/home/tools/test/test1/,/home/tools/test/test2/,/home/tools/test/test3/";
		  
		  
		  System.out.println("date_desc: "+date_desc);
		  System.out.println("days: "+days);
		  System.out.println("dirs: "+dirs);
		  
		  for(int i=0;i<=Integer.parseInt(days);i++){
			  System.out.println(DateUtil.getDay(date_desc, -i)+"保留");
			  day_map.put(DateUtil.getDay(date_desc, -i), "保留");
		  }
		  
          String[] dirList = dirs.split(",");
		  for(String dir:dirList){
			  if(!dir.endsWith("/")){
				  dir = dir + "/";
			  }
			  System.out.println("正在处理目录："+dir+"......");
			  ExecResult execResult = SshUtil.execCommand(SshUtil.DEFAULT_GATEWAY,"cd "+dir+"&&"+"ls -l | awk -F \" \" '{print $8}'");
			  if(execResult.success()){
				  String[] lsdirs = execResult.getStdoutAsArrays();
				  for(String lsdir:lsdirs){
					  if(lsdir!=null&&lsdir.startsWith("20")){
						  if(day_map.get(lsdir)==null){
							  System.out.println("正在删除目录："+dir+lsdir+"/"+" ......");
							  SshUtil.execCommand(SshUtil.DEFAULT_GATEWAY,"rm -rf "+dir+lsdir+"/");
						  }
					  }
				  }
			  }
		  } 
		  System.out.println(DateUtil.formatDateTime(new Date())+"......OK");
	  }
}
