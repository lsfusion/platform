package lsfusion.server.physics.dev.integration.external.to.net.websocket;

import lsfusion.base.file.RawFileData;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import org.java_websocket.WebSocket;

import java.sql.SQLException;
import java.util.Iterator;

public class SendBinaryMessageAction extends InternalAction {
    private final ClassPropertyInterface webSocketClientInterface;
    private final ClassPropertyInterface messageInterface;

    public SendBinaryMessageAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        webSocketClientInterface = i.next();
        messageInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject clientObject = context.getDataKeyValue(webSocketClientInterface);
        DataObject messageObject = context.getDataKeyValue(messageInterface);
        WebSocket connection = context.getLogicsInstance().getWebSocketServer().getConnection(clientObject);
        connection.send(((RawFileData) messageObject.getValue()).getBytes());
    }
}
