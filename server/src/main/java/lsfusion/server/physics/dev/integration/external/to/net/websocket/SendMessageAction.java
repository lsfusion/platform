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

import java.sql.SQLException;
import java.util.Iterator;

public class SendMessageAction extends InternalAction {
    private final ClassPropertyInterface webSocketClientInterface;

    public SendMessageAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        webSocketClientInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject clientObject = context.getDataKeyValue(webSocketClientInterface);
        try {
            String message = (String) findProperty("message[WebSocketClient]").read(context, clientObject);
            WebSocket connection = context.getLogicsInstance().getWebSocketServer().getConnection((Long) clientObject.getValue());
            connection.send(message);
            context.apply();
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
