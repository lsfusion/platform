package lsfusion.server.physics.admin.authentication.action;

import lsfusion.interop.action.LogOutClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.authentication.AuthenticationLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.Iterator;

public class LogOutAction extends InternalAction {
    private final ClassPropertyInterface restartInterface;
    private final ClassPropertyInterface reconnectInterface;

    public LogOutAction(AuthenticationLogicsModule LM, ValueClass... valueClasses) {
        super(LM, valueClasses);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        restartInterface = i.next();
        reconnectInterface = i.next();
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        boolean restart = context.getKeyValue(restartInterface).getValue() != null;
        boolean reconnect = context.getKeyValue(reconnectInterface).getValue() != null;
        context.delayUserInteraction(new LogOutClientAction(restart, reconnect));
    }
}
