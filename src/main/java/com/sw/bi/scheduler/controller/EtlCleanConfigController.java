package com.sw.bi.scheduler.controller;

import com.sw.bi.scheduler.model.EtlCleanConfig;
import com.sw.bi.scheduler.service.EtlCleanConfigService;
import com.sw.bi.scheduler.service.GenericService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/manage/etlCleanConfig")
public class EtlCleanConfigController extends BaseActionController<EtlCleanConfig> {

	@Autowired
	private EtlCleanConfigService etlCleanConfigService;

	@Override
	public GenericService<EtlCleanConfig> getDefaultService() {
		return etlCleanConfigService;
	}

}
