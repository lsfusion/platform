package lsfusion.server.physics.admin.service.action;

import lsfusion.base.Result;
import lsfusion.interop.action.MessageClientType;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.sql.SQLException;

import static lsfusion.base.BaseUtils.isEmpty;
import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public class CheckClassesAction extends InternalAction {
    public CheckClassesAction(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeInternal(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        final Result<String> message = new Result<>();
        ServiceDBAction.run(context, DBManager.CHECK_CLASSES_TIL, (session, isolatedTransaction) -> message.set(context.getDbManager().checkClasses(session, isolatedTransaction)));

        boolean noErrors = isEmpty(message.result);

        context.message(localize(LocalizedString.createFormatted(noErrors ? "{logics.check.completed}" : "{logics.check.failed}",
                localize("{logics.checking.data.classes}"))) + (noErrors ? "" : ("\n\n" + message.result)),
                localize("{logics.checking.data.classes}"), noErrors ? MessageClientType.SUCCESS : MessageClientType.ERROR);
    }
}