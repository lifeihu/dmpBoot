package com.sw.bi.scheduler.controller;

import com.sw.bi.scheduler.model.Resource;
import com.sw.bi.scheduler.service.GenericService;
import com.sw.bi.scheduler.service.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/manage/resource")
public class ResourceController extends BaseActionController<Resource> {

	@Autowired
	private ResourceService resourceService;

	@RequestMapping("/tree")
	@ResponseBody
	public List<Resource> getChildrenResources(@RequestParam("node") Long parentId, Boolean onceLoad) {
		return resourceService.getChildrenResources(parentId, onceLoad);
	}

	@RequestMapping("/generate")
	@ResponseBody
	public long generateResourceId(Integer parentId) {
		return resourceService.generateResourceId(Long.valueOf(parentId));
	}

	@Override
	public GenericService<Resource> getDefaultService() {
		return resourceService;
	}
}
