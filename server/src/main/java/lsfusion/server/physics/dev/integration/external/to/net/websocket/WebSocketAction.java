package lsfusion.server.physics.dev.integration.external.to.net.websocket;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class WebSocketAction extends InternalAction {

    private static Map<String, ChatServer> webSocketServers = new HashMap<>();

    private static HashMap<String, WebSocket> connectionMap = new HashMap<>();

    public WebSocketAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    public void startWebSocketServer(ExecutionContext context, DataObject serverObject, String host, Integer port, String path) throws IOException {
        ChatServer server = new ChatServer(new InetSocketAddress(host, port), new Consumer<String>() {
            @Override
            public void accept(String clientHost) {
                try (ExecutionContext.NewSession<ClassPropertyInterface> session = context.newSession()) {
                    findAction("newWebSocketClient[WebSocketServer,STRING]").execute(context, serverObject, new DataObject(clientHost));
                    session.apply();
                } catch (ScriptingErrorLog.SemanticErrorException | SQLException | SQLHandledException e) {
                    throw Throwables.propagate(e);
                }
            }
        }, new Consumer<String>() {
            @Override
            public void accept(String clientHost) {
                try (ExecutionContext.NewSession<ClassPropertyInterface> session = context.newSession()) {
                    findAction("newWebSocketClient[WebSocketServer,STRING]").execute(context, serverObject, new DataObject(clientHost));
                    session.apply();
                } catch (ScriptingErrorLog.SemanticErrorException | SQLException | SQLHandledException e) {
                    throw Throwables.propagate(e);
                }
            }
        });
        server.start();
        System.out.println("ChatServer started on port: " + port);

        webSocketServers.put(getWebSocketServerKey(host, port, path), server);
    }

    public void stopWebSocketServer(String host, Integer port, String path) throws InterruptedException {
        System.out.println("ChatServer stopped");
        ChatServer server = webSocketServers.remove(getWebSocketServerKey(host, port, path));
        server.stop();
    }

    public WebSocket getConnection(String clientHost) {
        return connectionMap.get(clientHost);
    }


    private String getWebSocketServerKey(String host, Integer port, String path) {
        return host + ":" + port + "/" + path;
    }

    public class ChatServer extends WebSocketServer {

        final Consumer<String> onOpen;
        final Consumer<String> onClose;


        public ChatServer(InetSocketAddress address, Consumer<String> onOpen, Consumer<String> onClose) {
            super(address);
            this.onOpen = onOpen;
            this.onClose = onClose;
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {

            //this method sends a message to the new client
            conn.send("Welcome to the server!");

            String clientHost = conn.getRemoteSocketAddress().getAddress().getHostAddress();

            System.out.println(clientHost + " entered the room!");

            connectionMap.put(clientHost, conn);

            onOpen.accept(clientHost);
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            System.out.println(conn + ": " + message);
        }

        @Override
        public void onMessage(WebSocket conn, ByteBuffer message) {
            System.out.println(conn + ": " + message);
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            String clientHost = conn.getRemoteSocketAddress().getAddress().getHostAddress();

            System.out.println(clientHost + " left the room!");

            connectionMap.remove(clientHost);

            onClose.accept(clientHost);
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
//            ex.printStackTrace();
//            if (conn != null) {
//                // some errors like port binding failed may not be assignable to a specific websocket
//            }
        }

        @Override
        public void onStart() {
//            System.out.println("Server started!");
//            setConnectionLostTimeout(0);
//            setConnectionLostTimeout(100);
        }
    }
}
