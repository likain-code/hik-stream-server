package org.yzu.cloud.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.yzu.cloud.StreamService.StreamDemo;
import org.yzu.cloud.listener.CmsEventListener;

@Configuration
public class ListenerConfig {

    @Bean
    public CmsEventListener cmsEventListener(StreamDemo streamDemo) {
        return new CmsEventListener(streamDemo);
    }
}
