package lsfusion.server.physics.admin.service.action;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

public class UpdateStatsAction extends InternalAction {

    public UpdateStatsAction(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeInternal(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        int majorStatChanged = context.getDbManager().updateStats(context.getSession().sql, true);
        if(majorStatChanged > Settings.get().getUpdateStatsDropLRUThreshold())
            context.getBL().serviceLM.dropLRU.execute(context);
    }
}