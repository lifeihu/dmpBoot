package com.sw.bi.scheduler.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/manage")
public class IndexController {

	@RequestMapping("/")
	public String execute() {
		return "home";
	}

	@RequestMapping("/login")
	public String login() {
		return "manage-login";
	}

	@RequestMapping("/restricted")
	public String restricted() {
		return "restricted";
	}
}
