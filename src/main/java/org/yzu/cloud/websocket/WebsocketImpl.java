package org.yzu.cloud.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@ServerEndpoint("/ws")
public class WebsocketImpl {

    public static final Set<Session> CLIENTS = new HashSet<>();

    @OnOpen
    public void onOpen(Session session) {
        log.info("已连接");
        CLIENTS.add(session);
        try {
            session.getBasicRemote().sendText("test");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @OnClose
    public void onClose(Session session) {
        log.info("已移除");
        CLIENTS.remove(session);
    }
}
