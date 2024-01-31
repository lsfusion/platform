package lsfusion.server.physics.dev.integration.external.to.net.websocket;

import com.google.common.base.Throwables;
import lsfusion.base.file.RawFileData;
import lsfusion.server.base.controller.lifecycle.LifecycleEvent;
import lsfusion.server.base.controller.manager.MonitorServer;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.data.file.CustomStaticFormatFileClass;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;

public class WebSocketServer extends MonitorServer {

    private LogicsInstance logicsInstance;

    private ScriptingLogicsModule LM;

    private HashMap<WebSocket, DataObject> socketMap = new HashMap<>();

    public void putSocket(DataObject socketObject, WebSocket conn) {
        socketMap.put(conn, socketObject);
    }

    public DataObject getSocketObject(WebSocket conn) {
        return socketMap.get(conn);
    }

    public WebSocket getSocket(DataObject socketObject) {
        for (Map.Entry<WebSocket, DataObject> entry : socketMap.entrySet()) {
            if (entry.getValue().equals(socketObject)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void removeSocket(WebSocket conn) {
        socketMap.remove(conn);
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
        LM = logicsInstance.getBusinessLogics().getModule("WebSocket");
    }

    @Override
    protected void onStarted(LifecycleEvent event) {
        ServerLoggers.systemLogger.info("Binding WebSocketServer");
        try {
            new Server(new InetSocketAddress(getLogicsInstance().getRmiManager().getWebSocketPort())).start();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public WebSocketServer() {
        super(DAEMON_ORDER);
    }

    public void setLogicsInstance(LogicsInstance logicsInstance) {
        this.logicsInstance = logicsInstance;
    }

    public class Server extends org.java_websocket.server.WebSocketServer {
        public Server(InetSocketAddress address) {
            super(address);
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            aspectAccept((session) -> {
                execute(session, "onOpen[STRING]", new DataObject(conn.getRemoteSocketAddress().getHostName()));
                try {
                    DataObject socketObject = (DataObject) LM.findProperty("socketCreated[]").readClasses(session);
                    logicsInstance.getWebSocketServer().putSocket(socketObject, conn);
                } catch (SQLException | SQLHandledException | ScriptingErrorLog.SemanticErrorException e) {
                    throw Throwables.propagate(e);
                }
            });
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            //accept string messages
            aspectAccept((session) -> execute(session, "onStringMessage[Socket,STRING]", getSocketObject(conn), new DataObject(message)));
        }

        @Override
        public void onMessage(WebSocket conn, ByteBuffer message) {
            //accept binary messages
            aspectAccept((session) -> execute(session, "onBinaryMessage[Socket,RAWFILE]", getSocketObject(conn), new DataObject(new RawFileData(message.array()), CustomStaticFormatFileClass.get())));
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            aspectAccept((session) -> {
                execute(session, "onClose[Socket]", getSocketObject(conn));
                logicsInstance.getWebSocketServer().removeSocket(conn);
            });
        }

        @Override
        public void onError(WebSocket conn, Exception e) {
            throw Throwables.propagate(e);
        }

        @Override
        public void onStart() {
            //do nothing
        }

        private void aspectAccept(Consumer<DataSession> consumer) {
            ThreadLocalContext.aspectBeforeMonitorHTTP(WebSocketServer.this);
            try (DataSession session = createSession()) {
                consumer.accept(session);
            } catch (SQLException e) {
                throw Throwables.propagate(e);
            } finally {
                ThreadLocalContext.aspectAfterMonitorHTTP(WebSocketServer.this);
            }
        }
    }

    private void execute(DataSession session, String action, ObjectValue... objectValues) {
        try {
            LM.findAction(action).execute(session, getStack(), objectValues);
            session.applyException(logicsInstance.getBusinessLogics(), getStack());
        } catch (SQLException | SQLHandledException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}