package lsfusion.server.physics.admin.service;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.language.ScriptingAction;

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