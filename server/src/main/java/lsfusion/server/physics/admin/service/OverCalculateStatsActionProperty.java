package lsfusion.server.physics.admin.service;

import com.google.common.base.Throwables;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.language.ScriptingAction;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

public class OverCalculateStatsActionProperty extends ScriptingAction {
    public OverCalculateStatsActionProperty(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            Integer maxQuantityOverCalculate = (Integer) findProperty("maxQuantityOverCalculate[]").read(context);
            context.getDbManager().overCalculateStats(context.getSession(), maxQuantityOverCalculate);
            context.apply();
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}