package com.sw.bi.scheduler.controller;

import com.sw.bi.scheduler.model.Gateway;
import com.sw.bi.scheduler.service.GatewayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Collection;

@Controller
@RequestMapping("/manage/gateway")
public class GatewayController extends BaseActionController<Gateway> {

	@Autowired
	private GatewayService gatewayService;

	/**
	 * 获得可执行指定作业类型的网关机
	 * 
	 * @param jobType
	 * @return
	 */
	@RequestMapping("/getGatewaysByJobType")
	@ResponseBody
	
	public Collection<Gateway> getGatewaysByJobType(String jobType) {
		System.out.println("jobType:"+jobType);
		Collection<Gateway> gateways = null;

		
		try {
			gateways = gatewayService.getGatewaysByJobType(Integer.parseInt(jobType));
		} catch (NumberFormatException e) {
			gateways = new ArrayList<Gateway>();
		}

		return gateways;
	}

	/**
	 * 修改调度方式
	 * 
	 * @param schedulerWay
	 */
	@RequestMapping("/updateSchedulerWay")
	public void updateSchedulerWay(int schedulerWay) {
		gatewayService.updateSchedulerWay(schedulerWay);
	}

	/**
	 * 修改调度轮循方式
	 * 
	 * @param roundWay
	 */
	@RequestMapping("/updateRoundWay")
	public void updateRoundWay(int roundWay) {
		gatewayService.updateRoundWay(roundWay);
	}

	@Override
	public GatewayService getDefaultService() {
		return gatewayService;
	}

}
