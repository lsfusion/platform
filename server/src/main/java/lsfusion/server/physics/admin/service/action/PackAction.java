package lsfusion.server.physics.admin.service.action;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.sql.SQLException;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public class PackAction extends InternalAction {
    public PackAction(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeInternal(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        
        ServiceDBAction.run(context, DBManager.PACK_TIL, (session, isolatedTransaction) -> context.getDbManager().packTables(session, context.getBL().LM.tableFactory.getImplementTables(), isolatedTransaction));
        context.messageSuccess(localize("{logics.tables.packing.completed}"), localize("{logics.tables.packing}"));
    }
}