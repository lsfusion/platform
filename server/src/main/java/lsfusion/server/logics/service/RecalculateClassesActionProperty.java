package lsfusion.server.logics.service;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class RecalculateClassesActionProperty extends ScriptingActionProperty {

    public RecalculateClassesActionProperty(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ServiceDBActionProperty.run(context, new RunService() {
            public void run(SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
                BusinessLogics BL = context.getBL();
                String result = BL.recalculateClasses(session, isolatedTransaction);
                if(result != null)
                    context.delayUserInterfaction(new MessageClientAction(result, getString("logics.recalculating.data.classes")));
                context.getDbManager().packTables(session, BL.LM.tableFactory.getImplementTables(), isolatedTransaction);
            }});

        context.delayUserInterfaction(new MessageClientAction(getString("logics.recalculation.completed", getString("logics.recalculating.data.classes")), getString("logics.recalculating.data.classes"), true));
    }

    @Override
    protected boolean isVolatile() {
        return true;
    }
}
