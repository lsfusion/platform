package lsfusion.server.logics.authentication;

import lsfusion.interop.action.UserChangedClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.AuthenticationLogicsModule;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;

import java.sql.SQLException;

public class ReloginUserActionProperty extends ScriptingActionProperty {

    public ReloginUserActionProperty(AuthenticationLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, "reloginUser", new ValueClass[]{LM.findClass("CustomUser")});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject user = context.getSingleDataKeyValue();
        context.getSession().user.changeCurrentUser(user);
        context.delayUserInterfaction(new UserChangedClientAction());
    }
}