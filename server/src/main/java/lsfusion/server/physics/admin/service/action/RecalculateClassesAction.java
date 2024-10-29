package lsfusion.server.physics.admin.service.action;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.sql.SQLException;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public class RecalculateClassesAction extends InternalAction {

    public RecalculateClassesAction(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeInternal(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ServiceDBAction.run(context, DBManager.RECALC_CLASSES_TIL, (session, isolatedTransaction) -> {
            BusinessLogics BL = context.getBL();
            String result = context.getDbManager().recalculateClasses(session, isolatedTransaction);
            if(result != null)
                context.message(result, localize("{logics.recalculating.data.classes}"));
            context.getDbManager().packTables(session, BL.LM.tableFactory.getImplementTables(), isolatedTransaction);
        });

        context.messageSuccess(localize(LocalizedString.createFormatted("{logics.recalculation.completed}", localize("{logics.recalculating.data.classes}"))), localize("{logics.recalculating.data.classes}"));
    }
}
