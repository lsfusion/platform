package platform.server.logics.authentication;

import platform.interop.action.UserChangedClientAction;
import platform.server.classes.ValueClass;
import platform.server.logics.AuthenticationLogicsModule;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;

import java.sql.SQLException;

public class ReloginUserActionProperty extends ScriptingActionProperty {

    public ReloginUserActionProperty(AuthenticationLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, "reloginUser", new ValueClass[]{LM.findClassByCompoundName("CustomUser")});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        DataObject user = context.getSingleDataKeyValue();
        context.getSession().user.changeCurrentUser(user);
        context.delayUserInterfaction(new UserChangedClientAction());
    }
}