package lsfusion.server.physics.admin.service;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.language.ScriptingAction;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

import static lsfusion.server.base.context.ThreadLocalContext.localize;

public class RecalculateClassesActionProperty extends ScriptingAction {

    public RecalculateClassesActionProperty(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ServiceDBActionProperty.run(context, new RunService() {
            public void run(SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
                BusinessLogics BL = context.getBL();
                String result = context.getDbManager().recalculateClasses(session, isolatedTransaction);
                if(result != null)
                    context.delayUserInterfaction(new MessageClientAction(result, localize("{logics.recalculating.data.classes}")));
                context.getDbManager().packTables(session, BL.LM.tableFactory.getImplementTables(), isolatedTransaction);
            }});

        context.delayUserInterfaction(new MessageClientAction(localize(LocalizedString.createFormatted("{logics.recalculation.completed}", localize("{logics.recalculating.data.classes}"))), localize("{logics.recalculating.data.classes}"), true));
    }
}
