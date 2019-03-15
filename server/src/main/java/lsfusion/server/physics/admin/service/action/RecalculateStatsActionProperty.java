package lsfusion.server.physics.admin.service.action;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

public class RecalculateStatsActionProperty extends ScriptingAction {
    public RecalculateStatsActionProperty(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        context.getDbManager().recalculateStats(context.getSession());
        context.apply();
    }
}