package com.sw.bi.scheduler;

import com.sw.bi.scheduler.background.main.Scheduler;
import com.sw.bi.scheduler.model.Gateway;
import com.sw.bi.scheduler.service.GatewayService;
import com.sw.bi.scheduler.util.Configure;
import com.sw.bi.scheduler.util.SshUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Collection;
import java.util.Date;

@Slf4j
@EnableAspectJAutoProxy(proxyTargetClass = true)
@SpringBootApplication(exclude = MongoAutoConfiguration.class)
@EnableConfigurationProperties
@EnableTransactionManagement
public class ScheduleBackApplication implements ApplicationRunner {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private GatewayService gatewayService;

    static ConfigurableApplicationContext context;
    public static void main(String[] args) {
        context = new SpringApplicationBuilder(ScheduleBackApplication.class)
                .web(WebApplicationType.NONE) // .REACTIVE, .SERVLET
                .bannerMode(Banner.Mode.OFF)
                .run(args);

//        SpringApplication.run(ScheduleBackApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//
//            }
//        }).start();

        // 判断是不是网关机器
        if (true) {
            return;
        }
        Collection<Gateway> gateways = gatewayService.queryAll();
        for (Gateway gateway : gateways) {
            SshUtil.registerAgent(gateway.getName(), gateway.getIp(), gateway.getPort());
        }
        Configure.property(Configure.GATEWAY, "web1");
        scheduler.schedule();
        log.info("调度结束");
        System.exit(0);
//        SpringApplication.exit(ScheduleBackApplication.context);
    }
}
