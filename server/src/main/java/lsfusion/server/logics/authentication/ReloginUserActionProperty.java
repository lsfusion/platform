package lsfusion.server.logics.authentication;

import lsfusion.base.ExceptionUtils;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.interop.action.UserChangedClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.AuthenticationLogicsModule;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;
import java.util.ResourceBundle;

public class ReloginUserActionProperty extends ScriptingActionProperty {

    public ReloginUserActionProperty(AuthenticationLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, "reloginUser", new ValueClass[]{LM.findClass("CustomUser")});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject user = context.getSingleDataKeyValue();
        if (context.getSession().user.changeCurrentUser(user)) {
            context.delayUserInterfaction(new UserChangedClientAction());
            ScriptingLogicsModule authenticationLM = context.getBL().getModule("Authentication");
            try {
                authenticationLM.findProperty("userChanged").change(true, context);
            } catch (ScriptingErrorLog.SemanticErrorException e) {
                throw ExceptionUtils.propagate(e, SQLException.class);
            }
        } else {
            ResourceBundle resourceBundle = ResourceBundle.getBundle("ServerResourceBundle");
            context.requestUserInteraction(new MessageClientAction(resourceBundle.getString("logics.error.changing.current.user.different.roles"), resourceBundle.getString("logics.error")));
        }
    }
}