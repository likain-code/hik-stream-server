package org.yzu.cloud.event;

import org.springframework.context.ApplicationEvent;

public class CmsEvent extends ApplicationEvent {

    public CmsEvent(Object source) {
        super(source);
    }
}
