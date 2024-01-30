package lsfusion.server.physics.dev.integration.external.to.net.websocket;

import lsfusion.server.base.controller.manager.LogicsManager;
import lsfusion.server.logics.BusinessLogics;
import org.java_websocket.WebSocket;
import org.springframework.beans.factory.InitializingBean;

import java.util.HashMap;
import java.util.Map;

public class WebSocketManager extends LogicsManager implements InitializingBean {

    private HashMap<WebSocket, String> connectionMap = new HashMap<>();

    public void putConnection(String id, WebSocket conn) {
        connectionMap.put(conn, id);
    }

    public String getConnectionId(WebSocket conn) {
        return connectionMap.get(conn);
    }

    public WebSocket getConnection(String id) {
        for(Map.Entry<WebSocket, String> entry : connectionMap.entrySet()) {
            if(entry.getValue().equals(id)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void removeConnection(WebSocket conn) {
        connectionMap.remove(conn);
    }

    public WebSocketManager() {
        super(DAEMON_ORDER);
    }

    @Override
    protected BusinessLogics getBusinessLogics() {
        return null;
    }

    @Override
    public void afterPropertiesSet() {
    }
}