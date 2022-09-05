package com.sw.bi.scheduler.background.javatype.reportmonitor;

import org.springframework.stereotype.Component;

import com.sw.bi.scheduler.background.util.BeanFactory;

//报表质量监控
@Component
public class ReportQualityMonitor {
	
	
	    public static void main(String args[]){
	    	ReportQualityMonitor.getReportQualityMonitor().reportMonitorAndAlert();
	    }
	
	
	
	
	 	private static ReportQualityMonitor getReportQualityMonitor() {
			return BeanFactory.getBean(ReportQualityMonitor.class);
		}
	
	
	 	private void reportMonitorAndAlert(){
	 		
	 		
	 		
	 		
	 		
	 		
	 		
	 		
	 		
	 		
	 		
	 		
	 		
	 	}
	 	
	 	
	 	
	 	
	 	private void monitorTotalNumber(){
	 		
	 		
	 		
	 		
	 		
	 		
	 	}
	 	
	 	
	
	
	
	

}
