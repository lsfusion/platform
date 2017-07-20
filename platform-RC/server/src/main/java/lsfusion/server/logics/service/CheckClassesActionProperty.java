package lsfusion.server.logics.service;

import lsfusion.base.Result;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.i18n.FormatLocalizedString;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;

import static lsfusion.server.context.ThreadLocalContext.localize;

public class CheckClassesActionProperty extends ScriptingActionProperty {
    public CheckClassesActionProperty(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        final Result<String> message = new Result<>();
        ServiceDBActionProperty.run(context, new RunService() {
            @Override
            public void run(SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
                message.set(context.getBL().checkClasses(session));
            }
        });
        context.delayUserInterfaction(new MessageClientAction(localize(new FormatLocalizedString("{logics.check.completed}", localize("{logics.checking.data.classes}"))) + '\n' + '\n' + message.result, localize("{logics.checking.data.classes}"), true));
    }

    @Override
    protected boolean isVolatile() {
        return true;
    }
}