package lsfusion.server.physics.admin.service.action;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public class VacuumDBAction extends InternalAction {
    public VacuumDBAction(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        context.getDbManager().vacuumDB(context.getSession().sql);
        context.delayUserInterfaction(new MessageClientAction(localize("{logics.vacuum.db.was.completed}"), localize("{logics.vacuum.db}")));
    }
}