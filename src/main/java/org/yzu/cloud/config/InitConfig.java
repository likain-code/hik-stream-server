package org.yzu.cloud.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.yzu.cloud.CmsService.CmsDemo;
import org.yzu.cloud.StreamService.StreamDemo;
import org.yzu.cloud.listener.CheckLogin;
import org.yzu.cloud.properties.ServerProperties;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(ServerProperties.class)
public class InitConfig {

    @Resource
    private ApplicationEventPublisher publisher;

    @Bean
    public CmsDemo cmsDemo() {
        return new CmsDemo();
    }

    @Bean
    public StreamDemo streamDemo() {
        return new StreamDemo();
    }

    @PostConstruct
    public void init() throws IOException {
        // 初始化流媒体服务
        streamDemo().eStream_Init();

        // 初始化注册服务
        cmsDemo().cMS_Init();
        cmsDemo().startCmsListen();

        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(new CheckLogin(publisher), 3, 5, TimeUnit.SECONDS);
    }
}
