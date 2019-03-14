package lsfusion.server.physics.admin.authentication;

import lsfusion.base.ExceptionUtils;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.interop.action.UserChangedClientAction;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.DataObject;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.language.ScriptingActionProperty;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;

import java.sql.SQLException;

import static lsfusion.server.base.context.ThreadLocalContext.localize;

public class ReloginUserActionProperty extends ScriptingActionProperty {

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