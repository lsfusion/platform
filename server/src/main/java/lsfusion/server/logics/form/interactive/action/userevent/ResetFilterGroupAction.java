package lsfusion.server.logics.form.interactive.action.userevent;

import lsfusion.interop.action.ResetFilterGroupClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.monitor.SystemEventsLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.Iterator;

public class ResetFilterGroupAction extends InternalAction {
    private final ClassPropertyInterface sidInterface;

    public ResetFilterGroupAction(SystemEventsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        sidInterface = i.next();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String sid = (String) context.getKeyValue(sidInterface).getValue();
        context.requestUserInteraction(new ResetFilterGroupClientAction(sid != null ? ("FILTERGROUP(" + sid + ")") : null));
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}