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
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class WebSocketServer extends MonitorServer {

    private LogicsInstance logicsInstance;

    private ScriptingLogicsModule LM;

    private HashMap<WebSocket, DataObject> connectionMap = new HashMap<>();

    public void putConnection(DataObject connectionObject, WebSocket conn) {
        connectionMap.put(conn, connectionObject);
    }

    public DataObject getConnectionObject(WebSocket conn) {
        return connectionMap.get(conn);
    }

    public WebSocket getConnection(DataObject connectionObject) {
        for (Map.Entry<WebSocket, DataObject> entry : connectionMap.entrySet()) {
            if (entry.getValue().equals(connectionObject)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void removeConnection(WebSocket conn) {
        connectionMap.remove(conn);
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
        Server server;
        try {

            server = new Server(new InetSocketAddress(getLogicsInstance().getRmiManager().getWebSocketPort()), (session, conn) -> {
                execute(session, "onOpen[STRING]", Collections.singletonList(new DataObject(conn.getRemoteSocketAddress().getHostName())));
                try {
                    return (DataObject) LM.findProperty("connectionCreated[]").readClasses(session);
                } catch (SQLException | SQLHandledException | ScriptingErrorLog.SemanticErrorException e) {
                    throw Throwables.propagate(e);
                }
            }, (session, conn, message) -> execute(session, "onStringMessage[WebSocketClient,STRING]", Arrays.asList(getConnectionObject(conn), new DataObject(message))),
                    (session, conn, message) -> execute(session, "onBinaryMessage[WebSocketClient,RAWFILE]", Arrays.asList(getConnectionObject(conn), new DataObject(message, CustomStaticFormatFileClass.get()))),
                    (session, conn) -> execute(session, "onClose[WebSocketClient]", Collections.singletonList(getConnectionObject(conn))));
            server.start();
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

        final BiFunction<DataSession, WebSocket, DataObject> onOpen;
        final TriConsumer<DataSession, WebSocket, String> onStringMessage;
        final TriConsumer<DataSession, WebSocket, RawFileData> onBinaryMessage;
        final BiConsumer<DataSession, WebSocket> onClose;

        public Server(InetSocketAddress address, BiFunction<DataSession, WebSocket, DataObject> onOpen, TriConsumer<DataSession, WebSocket, String> onStringMessage,
                      TriConsumer<DataSession, WebSocket, RawFileData> onBinaryMessage, BiConsumer<DataSession, WebSocket> onClose) {
            super(address);
            this.onOpen = onOpen;
            this.onStringMessage = onStringMessage;
            this.onBinaryMessage = onBinaryMessage;
            this.onClose = onClose;
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            aspectAccept((session) -> {
                //conn.send("Welcome to the server!"); //this method sends a message to the new client
                DataObject connectionObject = onOpen.apply(session, conn);
                logicsInstance.getWebSocketServer().putConnection(connectionObject, conn);
            });
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            //accept string messages
            aspectAccept((session) -> onStringMessage.accept(session, conn, message));
        }

        @Override
        public void onMessage(WebSocket conn, ByteBuffer message) {
            //accept binary messages
            aspectAccept((session) -> onBinaryMessage.accept(session, conn, new RawFileData(message.array())));
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            aspectAccept((session) -> {
                onClose.accept(session, conn);
                logicsInstance.getWebSocketServer().removeConnection(conn);
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

    private void execute(DataSession session, String action, List<ObjectValue> objectValues) {
        try {
            LM.findAction(action).execute(session, getStack(), objectValues.toArray(new ObjectValue[0]));
            session.applyException(logicsInstance.getBusinessLogics(), getStack());
        } catch (SQLException | SQLHandledException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    @FunctionalInterface
    public interface TriConsumer<T, U, V> {
        void accept(T k, U v, V s);
    }
}