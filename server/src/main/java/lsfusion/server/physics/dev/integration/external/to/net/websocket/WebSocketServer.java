package lsfusion.server.physics.dev.integration.external.to.net.websocket;

import com.google.common.base.Throwables;
import lsfusion.base.file.RawFileData;
import lsfusion.server.base.controller.lifecycle.LifecycleEvent;
import lsfusion.server.base.controller.manager.MonitorServer;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.data.file.CustomStaticFormatFileClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class WebSocketServer extends MonitorServer {

    private LogicsInstance logicsInstance;

    private ScriptingLogicsModule LM;

    private HashMap<WebSocket, Long> connectionMap = new HashMap<>();

    public void putConnection(Long id, WebSocket conn) {
        connectionMap.put(conn, id);
    }

    public Long getConnectionId(WebSocket conn) {
        return connectionMap.get(conn);
    }

    public WebSocket getConnection(Long id) {
        for (Map.Entry<WebSocket, Long> entry : connectionMap.entrySet()) {
            if (entry.getValue().equals(id)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void removeConnection(WebSocket conn) {
        connectionMap.remove(conn);
    }


    public DataObject getConnectionObject(WebSocket conn) throws ScriptingErrorLog.SemanticErrorException {
        return new DataObject(logicsInstance.getWebSocketServer().getConnectionId(conn), (ConcreteCustomClass) LM.findClass("WebSocketClient"));
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
                try {
                    LM.findAction("onOpen[STRING]").execute(session, getStack(),
                            new DataObject(conn.getRemoteSocketAddress().getHostName()));
                    Long connectionId = (Long) LM.findProperty("connectionId[]").read(session);
                    session.applyException(logicsInstance.getBusinessLogics(), getStack());
                    return connectionId;
                } catch (SQLException | SQLHandledException | ScriptingErrorLog.SemanticErrorException e) {
                    throw Throwables.propagate(e);
                }
            }, (session, conn, message) -> {
                try {
                    LM.findAction("onStringMessage[WebSocketClient,STRING]").execute(session, getStack(),
                            getConnectionObject(conn), new DataObject(message));
                    session.applyException(logicsInstance.getBusinessLogics(), getStack());
                } catch (SQLException | SQLHandledException | ScriptingErrorLog.SemanticErrorException e) {
                    throw Throwables.propagate(e);
                }
            }, (session, conn, message) -> {
                try {
                    LM.findAction("onBinaryMessage[WebSocketClient,RAWFILE]").execute(session, getStack(),
                            getConnectionObject(conn), new DataObject(message, CustomStaticFormatFileClass.get()));
                    session.applyException(logicsInstance.getBusinessLogics(), getStack());
                } catch (SQLException | SQLHandledException | ScriptingErrorLog.SemanticErrorException e) {
                    throw Throwables.propagate(e);
                }
            }, (session, conn) -> {
                try {
                    LM.findAction("onClose[WebSocketClient]").execute(session, getStack(), getConnectionObject(conn));
                    session.applyException(logicsInstance.getBusinessLogics(), getStack());
                } catch (SQLException | SQLHandledException | ScriptingErrorLog.SemanticErrorException e) {
                    throw Throwables.propagate(e);
                }
            });
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

        final BiFunction<DataSession, WebSocket, Long> onOpen;
        final TriConsumer<DataSession, WebSocket, String> onStringMessage;
        final TriConsumer<DataSession, WebSocket, RawFileData> onBinaryMessage;
        final BiConsumer<DataSession, WebSocket> onClose;

        public Server(InetSocketAddress address, BiFunction<DataSession, WebSocket, Long> onOpen, TriConsumer<DataSession, WebSocket, String> onStringMessage,
                      TriConsumer<DataSession, WebSocket, RawFileData> onBinaryMessage, BiConsumer<DataSession, WebSocket> onClose) {
            super(address);
            this.onOpen = onOpen;
            this.onStringMessage = onStringMessage;
            this.onBinaryMessage = onBinaryMessage;
            this.onClose = onClose;
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            consume((session) -> {
                //conn.send("Welcome to the server!"); //this method sends a message to the new client
                Long connectionId = onOpen.apply(session, conn);
                logicsInstance.getWebSocketServer().putConnection(connectionId, conn);
            });
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            //accept string messages
            consume((session) -> onStringMessage.accept(session, conn, message));
        }

        @Override
        public void onMessage(WebSocket conn, ByteBuffer message) {
            //accept binary messages
            consume((session) -> onBinaryMessage.accept(session, conn, new RawFileData(message.array())));
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            consume((session) -> {
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

        private void consume(Consumer<DataSession> consumer) {
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

    @FunctionalInterface
    public interface TriConsumer<T, U, V> {
        void accept(T k, U v, V s);
    }
}