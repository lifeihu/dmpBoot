/**
  * 文件名：TestDate.java
  * 版本信息：Version 1.0
  * 日期：2016-6-14
  * Copyright www.adtime.com Corporation 2016 
  * 版权所有
  */
package com.sw.bi.scheduler.util;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


	/**
 * 类描述：
 * @version: 1.0
 * @author: chenpanpan
 * @version: 2016-6-14 上午11:34:47 
 */
public class TestDate
{
    public static void main(String[] args)
    {

/*       String str = "select * from test where pt=${date_now,50} and pt=${date_now,5} and pt=${date_now,1}";
        String reg = "\\$\\{date_now[^\\$]*\\}";    
        Pattern p = Pattern.compile(reg);
        Matcher m = p.matcher(str);
        while(m.find()){
            Calendar calendar = DateUtil.cloneCalendar();
            calendar.add(Calendar.DATE, 0-Integer.parseInt((m.group().substring(m.group().indexOf(",")+1,m.group().length()-1))));
            str= str.replace(m.group(), DateUtil.format(calendar.getTime(), "yyyyMMdd"));
        }
        System.out.println(str);*/
        
/*        
        String strHour = "select * from test where pt=${hour_now,50} and pt=${hour_now,5} and pt=${hour_now,1}";
        
        System.out.println(strHour.contains("date_now"));

        
        System.out.println(strHour.contains("hour_now"));
        
        String regHour = "\\$\\{hour_now[^\\$]*\\}";    
        Pattern p = Pattern.compile(regHour);
        Matcher m = p.matcher(strHour);
        while(m.find()){
            Calendar calendar = DateUtil.cloneCalendar();
            calendar.add(Calendar.HOUR, 0-Integer.parseInt((m.group().substring(m.group().indexOf(",")+1,m.group().length()-1))));
            strHour= strHour.replace(m.group(), DateUtil.format(calendar.getTime(), "yyyyMMddHH"));
        }
        System.out.println(strHour);
        
        
        */
        
        
       String strHour = "select * from test where pt=${hour_now,50} and pt=${hour_now,5} and pt=${hour_now,1}";
        
        System.out.println(strHour.contains("date_now"));

        
        System.out.println(strHour.contains("hour_now"));
        
        String regHour = "\\$\\{hour_now[^\\$]*\\}";    
        Pattern p = Pattern.compile(regHour);
        Matcher m = p.matcher(strHour);
        while(m.find()){
            Calendar calendar = DateUtil.cloneCalendar();
            calendar.add(Calendar.MONTH, 0-Integer.parseInt((m.group().substring(m.group().indexOf(",")+1,m.group().length()-1))));
            strHour= strHour.replace(m.group(), DateUtil.format(calendar.getTime(), "yyyyMM"));
        }
        System.out.println(strHour);
        
        
        
        
        
        
        
    }
}








