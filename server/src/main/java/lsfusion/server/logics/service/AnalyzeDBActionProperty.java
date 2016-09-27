package lsfusion.server.logics.service;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

public class AnalyzeDBActionProperty extends ScriptingActionProperty {
    public AnalyzeDBActionProperty(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try (DataSession session = context.createSession()) {
            context.getDbManager().analyzeDB(session.sql);
            session.apply(context);
        }
        context.delayUserInterfaction(new MessageClientAction(ThreadLocalContext.localize("{logics.vacuum.analyze.was.completed}"), ThreadLocalContext.localize("{logics.vacuum.analyze}")));
    }

    @Override
    protected boolean isVolatile() {
        return true;
    }
}