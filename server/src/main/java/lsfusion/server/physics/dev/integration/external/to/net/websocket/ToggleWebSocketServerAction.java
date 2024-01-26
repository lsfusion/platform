package lsfusion.server.physics.dev.integration.external.to.net.websocket;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;

import static lsfusion.base.BaseUtils.nvl;

public class ToggleWebSocketServerAction extends WebSocketAction {
    private final ClassPropertyInterface webSocketServerInterface;

    public ToggleWebSocketServerAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        webSocketServerInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        DataObject serverObject = context.getDataKeyValue(webSocketServerInterface);

        try {
            String host = nvl((String) findProperty("host[WebSocketServer]").read(context, serverObject), "localhost");
            Integer port = nvl((Integer) findProperty("port[WebSocketServer]").read(context, serverObject), 8887);
            String path = nvl((String) findProperty("path[WebSocketServer]").read(context, serverObject), "");

            boolean started = findProperty("started[WebSocketServer]").read(context, serverObject) != null;
            if(started) {
                stopWebSocketServer(host, port, path);
            } else {
                startWebSocketServer(context, serverObject, host, port, path);
            }

            try(ExecutionContext.NewSession<ClassPropertyInterface> session = context.newSession()) {
                findProperty("started[WebSocketServer]").change(started ? null : true, session, serverObject);
                session.apply();
            }

        } catch (ScriptingErrorLog.SemanticErrorException | IOException | InterruptedException e) {
            throw Throwables.propagate(e);
        }
    }
}
