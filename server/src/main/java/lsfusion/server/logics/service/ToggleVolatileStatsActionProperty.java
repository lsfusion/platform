package lsfusion.server.logics.service;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

public class ToggleVolatileStatsActionProperty extends ScriptingActionProperty {

    public ToggleVolatileStatsActionProperty(ServiceLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataSession session = context.getSession();
        session.sql.toggleVolatileStats(session.getOwner());
    }
}
