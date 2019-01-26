package lsfusion.server.logics.service;

import com.google.common.base.Throwables;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DBManager;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;

import java.sql.SQLException;

public class OverCalculateStatsActionProperty extends ScriptingActionProperty {
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