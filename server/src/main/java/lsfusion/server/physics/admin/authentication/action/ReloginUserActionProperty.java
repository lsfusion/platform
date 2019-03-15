package lsfusion.server.physics.admin.authentication.action;

import lsfusion.base.ExceptionUtils;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.interop.action.UserChangedClientAction;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.physics.admin.authentication.AuthenticationLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public class ReloginUserActionProperty extends ScriptingAction {

    public ReloginUserActionProperty(AuthenticationLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject user = context.getSingleDataKeyValue();
        if (context.getSession().user.changeCurrentUser(user, context.stack)) {
            context.delayUserInterfaction(new UserChangedClientAction());
            ScriptingLogicsModule authenticationLM = context.getBL().getModule("Authentication");
            try {
                authenticationLM.findProperty("userChanged[]").change(true, context);
            } catch (ScriptingErrorLog.SemanticErrorException e) {
                throw ExceptionUtils.propagate(e, SQLException.class);
            }
        } else {
            context.requestUserInteraction(new MessageClientAction(localize("{logics.error.changing.current.user.different.roles}"), localize("{logics.error}")));
        }
    }
}