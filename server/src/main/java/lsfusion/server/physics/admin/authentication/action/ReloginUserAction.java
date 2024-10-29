package lsfusion.server.physics.admin.authentication.action;

import lsfusion.base.ExceptionUtils;
import lsfusion.interop.action.UserChangedClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.authentication.AuthenticationLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public class ReloginUserAction extends InternalAction {

    public ReloginUserAction(AuthenticationLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject user = context.getSingleDataKeyValue();
        if (context.getSession().user.changeCurrentUser(user, context.stack)) {
            context.delayUserInterfaction(new UserChangedClientAction());
            try {
                context.getBL().systemEventsLM.findAction("userChanged[CustomUser]").execute(context, user);
            } catch (ScriptingErrorLog.SemanticErrorException e) {
                throw ExceptionUtils.propagate(e, SQLException.class);
            }
        } else {
            context.messageError(localize("{logics.error.changing.current.user.different.roles}"));
        }
    }
}