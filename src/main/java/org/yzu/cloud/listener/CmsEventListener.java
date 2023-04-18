package org.yzu.cloud.listener;

import org.springframework.context.ApplicationListener;
import org.yzu.cloud.StreamService.StreamDemo;
import org.yzu.cloud.event.CmsEvent;

public class CmsEventListener implements ApplicationListener<CmsEvent> {

    private final StreamDemo streamDemo;

    public CmsEventListener(StreamDemo streamDemo) {
        this.streamDemo = streamDemo;
    }

    @Override
    public void onApplicationEvent(CmsEvent event) {
        // 触发了注册事件，开始拉流
        streamDemo.startRealPlayListen();
        streamDemo.RealPlay(1);
    }
}
