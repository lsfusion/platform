package lsfusion.server.physics.admin.service;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.language.ScriptingAction;

import java.sql.SQLException;

import static lsfusion.server.base.context.ThreadLocalContext.localize;

public class VacuumDBActionProperty extends ScriptingAction {
    public VacuumDBActionProperty(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        context.getDbManager().vacuumDB(context.getSession().sql);
        context.delayUserInterfaction(new MessageClientAction(localize("{logics.vacuum.db.was.completed}"), localize("{logics.vacuum.db}")));
    }
}