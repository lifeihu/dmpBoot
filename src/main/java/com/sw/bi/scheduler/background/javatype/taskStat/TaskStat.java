package com.sw.bi.scheduler.background.javatype.taskStat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.model.Action;
import com.sw.bi.scheduler.service.ActionService;
import com.sw.bi.scheduler.util.DateUtil;

/**
 * 2012-06-29  为了统计每个分钟时间点内的任务运行情况
 * @author feng.li
 *
 */
// extjs的折线图可以参考 http://lkj107.iteye.com/blog/591543
//com.shunwang.sms.core-0.0.1.jar,org.springframework.jms-3.0.5.RELEASE.jar,activemq-all-5.5.0.jar 加到classpath里
// select * from action  where task_date = '2012-03-30' order by start_time asc   scheduler12
@Component
public class TaskStat {
	
	@Autowired
	private ActionService actionService;
	
	private static int minite_space = 60;  //统计的分钟间隔   这个值可以由前台传入,看每5分钟的任务分布,每10分钟,每30分钟,每小时等等
	
	public static void main(String args[]){
		String date = "2012-03-30";
		Date taskDate = DateUtil.parseDate(date);
		List<Date> minites = getOneDayMinites(taskDate);
		List<Action> list = TaskStat.getTaskStat().getActions(taskDate);//查某天的action记录

		Map<Long,ArrayList<Long>> map = genMap(minites,list);
		
		for(Map.Entry<Long,ArrayList<Long>> entry:map.entrySet())   
		{   
		    System.out.println(TimeStamp2Date(entry.getKey()+"")+"--->"+entry.getValue().size());   
		}  

		
	}
	
	
	private static Map<Long,ArrayList<Long>> genMap(List<Date> minites,List<Action> list){
		Map<Long,ArrayList<Long>> map = new LinkedHashMap<Long,ArrayList<Long>>();
		for(Date minite:minites){
			long now_minite = minite.getTime();
			
			ArrayList<Long> arraylist = new ArrayList<Long>();
			map.put(now_minite, arraylist);
			
			for(Action action:list){
				long startTime = action.getStartTime().getTime();
				if(startTime>now_minite){
					long next_minite = now_minite+minite_space*60*1000;
					if(startTime>=next_minite){
						break;  //这里用break,而不是continue, 是因为action的集合本来就是根据startTime从小到大排列的,当出现一个startTime比now_minite大,那么后续的action的startTime都会比now_minite大
					}else{
						arraylist = map.get(now_minite);
						arraylist.add(action.getActionId());
						map.put(now_minite, arraylist);
					}
				}else{
					long end_time = action.getEndTime()==null?new Date().getTime():action.getEndTime().getTime();
					if(end_time<now_minite){
						continue;  //跳过. 应该向pre_minite归类,而不应该向now_minite归类
					}else{
						arraylist = map.get(now_minite);
						arraylist.add(action.getActionId());
						map.put(now_minite, arraylist);
					}
				}
			}
			
		}
		return map;
	}
	
	
	
	public static String TimeStamp2Date(String timestampString){     
		  Long timestamp = Long.parseLong(timestampString);     
		  String date = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp));
		  return date;     
	}    

	
	private static List<Date> getOneDayMinites(Date taskDate){
		
		List<Date> list = new ArrayList<Date>();

		Calendar calendar = DateUtil.cloneCalendar(DateUtil.getCalendar(taskDate));
		// 从指定日期的零点开始计算
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		list.add(calendar.getTime());
		boolean start = true;
		int taskDay = calendar.get(Calendar.DATE); //当天的日期
		while (start) {
			calendar.add(Calendar.MINUTE, minite_space);  // 分钟任务,每次循环增加dayN分钟

			if (calendar.get(Calendar.DATE) == taskDay){  //增加minite_space分钟后,如果日期还是与当天的日期一样,则创建对应的任务,否则跳出循环,结束分钟任务的创建
				list.add(calendar.getTime());
			}else{
				start = false;
			}
				
		}

		return list;
	}
	
	private static TaskStat getTaskStat() {
		return BeanFactory.getBean(TaskStat.class);
	}
	
	public List<Action> getActions(Date taskDate){
		List<Action> list = actionService.getActionsOrderByStartTimeAsc(taskDate);
		return list;
	}
	
	
}
