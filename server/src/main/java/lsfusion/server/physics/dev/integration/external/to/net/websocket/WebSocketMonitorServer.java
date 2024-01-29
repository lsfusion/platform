package lsfusion.server.physics.dev.integration.external.to.net.websocket;

import com.google.common.base.Throwables;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.session.ExecInterface;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.interop.session.SessionInfo;
import lsfusion.server.base.controller.lifecycle.LifecycleEvent;
import lsfusion.server.base.controller.manager.MonitorServer;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.controller.remote.RemoteLogics;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.function.Consumer;

public class WebSocketMonitorServer extends MonitorServer {

    private LogicsInstance logicsInstance;
    private RemoteLogics remoteLogics;

    private static HashMap<String, WebSocket> connectionMap = new HashMap<>();

    public static WebSocket getConnection(String id) {
        return connectionMap.get(id);
    }

    private static String getConnectionId(WebSocket conn) {
        return String.valueOf(conn.hashCode());
    }

    @Override
    public String getEventName() {
        return "websocket-server";
    }

    @Override
    public LogicsInstance getLogicsInstance() {
        return logicsInstance;
    }

    @Override
    protected void onInit(LifecycleEvent event) {
        //do nothing
    }

    @Override
    protected void onStarted(LifecycleEvent event) {
        ServerLoggers.systemLogger.info("Binding WebSocketServer");
        Server server;
        try {

            server = new Server(new InetSocketAddress(getLogicsInstance().getRmiManager().getWebSocketPort()), conn -> {
                ThreadLocalContext.aspectBeforeMonitorHTTP(WebSocketMonitorServer.this);
                try {
                    ExecInterface remoteExec = ExternalUtils.getExecInterface(AuthenticationToken.ANONYMOUS,
                            new SessionInfo("remote", null, null, null, null, null, null, null),
                            remoteLogics);
                    remoteExec.exec("onOpen[STRING,STRING]", new ExternalRequest(
                            new Object[]{getConnectionId(conn), conn.getRemoteSocketAddress().getHostName()}));
                } catch (RemoteException e) {
                    throw Throwables.propagate(e);
                } finally {
                    ThreadLocalContext.aspectAfterMonitorHTTP(WebSocketMonitorServer.this);
                }
            }, conn -> {
                ThreadLocalContext.aspectBeforeMonitorHTTP(WebSocketMonitorServer.this);
                try {
                    ExecInterface remoteExec = ExternalUtils.getExecInterface(AuthenticationToken.ANONYMOUS,
                            new SessionInfo("remote", null, null, null, null, null, null, null),
                            remoteLogics);
                    remoteExec.exec("onClose[STRING]", new ExternalRequest(new Object[]{getConnectionId(conn)}));
                } catch (RemoteException e) {
                    throw Throwables.propagate(e);
                } finally {
                    ThreadLocalContext.aspectAfterMonitorHTTP(WebSocketMonitorServer.this);
                }
            });
            server.start();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public WebSocketMonitorServer() {
        super(DAEMON_ORDER);
    }

    public void setLogicsInstance(LogicsInstance logicsInstance) {
        this.logicsInstance = logicsInstance;
    }

    public void setRemoteLogics(RemoteLogics remoteLogics) {
        this.remoteLogics = remoteLogics;
    }

    public static class Server extends WebSocketServer {

        final Consumer<WebSocket> onOpen;
        final Consumer<WebSocket> onClose;

        public Server(InetSocketAddress address, Consumer<WebSocket> onOpen, Consumer<WebSocket> onClose) {
            super(address);
            this.onOpen = onOpen;
            this.onClose = onClose;
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            //conn.send("Welcome to the server!"); //this method sends a message to the new client
            connectionMap.put(getConnectionId(conn), conn);
            onOpen.accept(conn);
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            //do nothing
        }

        @Override
        public void onMessage(WebSocket conn, ByteBuffer message) {
            //do nothing
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            connectionMap.remove(getConnectionId(conn));
            onClose.accept(conn);
        }

        @Override
        public void onError(WebSocket conn, Exception e) {
            throw Throwables.propagate(e);
        }

        @Override
        public void onStart() {
            //do nothing
        }
    }
}