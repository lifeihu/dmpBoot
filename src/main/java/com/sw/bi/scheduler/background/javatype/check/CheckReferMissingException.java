package com.sw.bi.scheduler.background.javatype.check;


import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;

import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.service.TaskService;



/**
 * 监测参考点丢失异常--------------------以分钟任务的形势，每10分钟检测一次
 * 加强对未运行状态的任务的监控与告警
 * 通过对未运行状态的任务(scan_date是当天,状态是前台状态中的未运行)的查询,并监测其父任务(状态是成功) 是否存放于参考点表中
 * 但是某个节点运行成功后，并不是第一时间就能被放入参考点表中去的，运行成功的时间跟放入参考点表中的时间是有一定时间差的（暂定半个小时）
 * 2012-07-30
 * @author feng.li
 * 
 * 扫描当前所有状态是未运行的任务，并查询其所有向上一层的父任务，如果它向上一层的父任务状态是运行成功，且当前系统时间已经超过update_time 30分钟了，如果这个父任务节点还没有能在参考点表中查询到，那么就定义为参考点异常
 * 同时，前台，对于运行成功的任务，应该提供一个操作： 放入参考点（不能有重复）
 * 性能问题?   未运行的任务的数量一般都比较庞大，遍历每一个点，再向上遍历其父任务，这个方式不太可取。
 *
 */
public class CheckReferMissingException {



	@Autowired
	private  TaskService taskService;
	
    public static void main(String args[]) throws IOException, InterruptedException{
    	CheckReferMissingException.CheckReferMissingException().check();
    }

    public void check() throws IOException, InterruptedException{
    
    	
    	
    	
    }
    
    
    

	private static CheckReferMissingException CheckReferMissingException() {
		return BeanFactory.getBean(CheckReferMissingException.class);
	}

}
