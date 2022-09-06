package com.sw.bi.scheduler.task;

import com.sw.bi.scheduler.model.Job;
import com.sw.bi.scheduler.model.User;
import com.sw.bi.scheduler.service.GatewayService;
import com.sw.bi.scheduler.service.JobService;
import com.sw.bi.scheduler.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@EnableScheduling
public class TestTask {

    @Autowired
    private GatewayService gatewayService;


    @Autowired
    private JobService jobService;

    @Autowired
    private UserService userService;
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void alarmSend() {
//       Job job = jobService.get(1L);
//        User user = userService.getUserByLoginName("admin");
//        System.out.println(gatewayService.getGateway("web1"));
    }
}
