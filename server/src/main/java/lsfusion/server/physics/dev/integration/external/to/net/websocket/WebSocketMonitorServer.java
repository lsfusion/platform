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
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class WebSocketMonitorServer extends MonitorServer {

    private LogicsInstance logicsInstance;

    private ScriptingLogicsModule LM;

    public DataObject getConnectionObject(WebSocket conn) {
        return new DataObject(logicsInstance.getWebSocketManager().getConnectionId(conn));
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

            server = new Server(new InetSocketAddress(getLogicsInstance().getRmiManager().getWebSocketPort()), conn -> {
                ThreadLocalContext.aspectBeforeMonitorHTTP(WebSocketMonitorServer.this);
                try (DataSession session = createSession()) {
                    LM.findAction("onOpen[STRING]").execute(session, getStack(),
                            new DataObject(conn.getRemoteSocketAddress().getHostName()));
                    String connectionId = (String) LM.findProperty("connectionId[]").read(session);
                    session.applyException(logicsInstance.getBusinessLogics(), getStack());
                    return connectionId;
                } catch (SQLException | SQLHandledException | ScriptingErrorLog.SemanticErrorException e) {
                    throw Throwables.propagate(e);
                } finally {
                    ThreadLocalContext.aspectAfterMonitorHTTP(WebSocketMonitorServer.this);
                }
            }, (conn, message) -> {
                ThreadLocalContext.aspectBeforeMonitorHTTP(WebSocketMonitorServer.this);
                try (DataSession session = createSession()) {
                    LM.findAction("onStringMessage[STRING,STRING]").execute(session, getStack(),
                            getConnectionObject(conn), new DataObject(message));
                    session.applyException(logicsInstance.getBusinessLogics(), getStack());
                } catch (SQLException | SQLHandledException | ScriptingErrorLog.SemanticErrorException e) {
                    throw Throwables.propagate(e);
                } finally {
                    ThreadLocalContext.aspectAfterMonitorHTTP(WebSocketMonitorServer.this);
                }
            }, (conn, message) -> {
                ThreadLocalContext.aspectBeforeMonitorHTTP(WebSocketMonitorServer.this);
                try (DataSession session = createSession()) {
                    LM.findAction("onBinaryMessage[STRING,RAWFILE]").execute(session, getStack(),
                            getConnectionObject(conn), new DataObject(message, CustomStaticFormatFileClass.get()));
                    session.applyException(logicsInstance.getBusinessLogics(), getStack());
                } catch (SQLException | SQLHandledException | ScriptingErrorLog.SemanticErrorException e) {
                    throw Throwables.propagate(e);
                } finally {
                    ThreadLocalContext.aspectAfterMonitorHTTP(WebSocketMonitorServer.this);
                }
            }, conn -> {
                ThreadLocalContext.aspectBeforeMonitorHTTP(WebSocketMonitorServer.this);
                try (DataSession session = createSession()) {
                    LM.findAction("onClose[STRING]").execute(session, getStack(), getConnectionObject(conn));
                    session.applyException(logicsInstance.getBusinessLogics(), getStack());
                } catch (SQLException | SQLHandledException | ScriptingErrorLog.SemanticErrorException e) {
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

    public class Server extends WebSocketServer {

        final Function<WebSocket, String> onOpen;
        final BiConsumer<WebSocket, String> onStringMessage;
        final BiConsumer<WebSocket, RawFileData> onBinaryMessage;
        final Consumer<WebSocket> onClose;

        public Server(InetSocketAddress address, Function<WebSocket, String> onOpen, BiConsumer<WebSocket, String> onStringMessage,
                      BiConsumer<WebSocket, RawFileData> onBinaryMessage, Consumer<WebSocket> onClose) {
            super(address);
            this.onOpen = onOpen;
            this.onStringMessage = onStringMessage;
            this.onBinaryMessage = onBinaryMessage;
            this.onClose = onClose;
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            //conn.send("Welcome to the server!"); //this method sends a message to the new client
            String connectionId = onOpen.apply(conn);
            logicsInstance.getWebSocketManager().putConnection(connectionId, conn);

        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            //accept string messages
            onStringMessage.accept(conn, message);
        }

        @Override
        public void onMessage(WebSocket conn, ByteBuffer message) {
            //accept binary messages
            onBinaryMessage.accept(conn, new RawFileData(message.array()));
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            onClose.accept(conn);
            logicsInstance.getWebSocketManager().removeConnection(conn);
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