package lsfusion.server.physics.admin.service.action;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

public class OverCalculateStatsActionProperty extends InternalAction {
    public OverCalculateStatsActionProperty(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            Integer maxQuantityOverCalculate = (Integer) findProperty("maxQuantityOverCalculate[]").read(context);
            context.getDbManager().overCalculateStats(context.getSession(), maxQuantityOverCalculate);
            context.apply();
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}