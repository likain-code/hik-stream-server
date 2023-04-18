package org.yzu.cloud.listener;

import org.springframework.context.ApplicationEventPublisher;
import org.yzu.cloud.CmsService.CmsDemo;
import org.yzu.cloud.event.CmsEvent;

public class CheckLogin implements Runnable {

    private boolean flag = false;
    private final ApplicationEventPublisher applicationEventPublisher;

    public CheckLogin(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void run() {
        if (CmsDemo.lLoginID != -1) {
            if (!flag) {
                applicationEventPublisher.publishEvent(new CmsEvent(this));
                flag = true;
            }
        }
    }
}
