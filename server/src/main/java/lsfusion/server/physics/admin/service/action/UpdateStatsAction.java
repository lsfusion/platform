package lsfusion.server.physics.admin.service.action;

import lsfusion.base.Result;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

public class UpdateStatsAction extends InternalAction {

    public UpdateStatsAction(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeInternal(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        Result<Integer> majorStatChangedCount = new Result(0);
        context.getDbManager().updateStats(context.getSession().sql, majorStatChangedCount);
        context.getBL().dropLRU(majorStatChangedCount);
    }
}