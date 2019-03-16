package lsfusion.server.physics.admin.service.action;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.service.RunService;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public class RecalculateActionProperty extends InternalAction {
    public RecalculateActionProperty(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        ServiceDBActionProperty.run(context, new RunService() {
            public void run(SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
                String result = context.getDbManager().recalculateAggregations(context.stack, session, isolatedTransaction);
                if(result != null)
                    context.delayUserInterfaction(new MessageClientAction(result, localize("{logics.recalculation.aggregations}")));
            }});

        context.delayUserInterfaction(new MessageClientAction(localize(LocalizedString.createFormatted("{logics.recalculation.completed}", localize("{logics.recalculation.aggregations}"))), localize("{logics.recalculation.aggregations}")));
    }
}