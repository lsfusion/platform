package lsfusion.server.physics.dev.integration.external.to.net.websocket;

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

public class SendStringMessageAction extends InternalAction {
    private final ClassPropertyInterface socketInterface;
    private final ClassPropertyInterface messageInterface;

    public SendStringMessageAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        socketInterface = i.next();
        messageInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject socketObject = context.getDataKeyValue(socketInterface);
        DataObject messageObject = context.getDataKeyValue(messageInterface);
        WebSocket connection = context.getLogicsInstance().getWebSocketServer().getSocket(socketObject);
        connection.send((String) messageObject.getValue());
    }
}
