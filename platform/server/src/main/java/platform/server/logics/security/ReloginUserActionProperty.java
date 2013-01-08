package platform.server.logics.security;

import platform.interop.action.UserChangedClientAction;
import platform.interop.action.UserReloginClientAction;
import platform.server.classes.ConcreteValueClass;
import platform.server.classes.ValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.SecurityLogicsModule;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.actions.AdminActionProperty;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;

import java.sql.SQLException;

import static platform.server.logics.ServerResourceBundle.getString;

public class ReloginUserActionProperty extends ScriptingActionProperty {

    public ReloginUserActionProperty(SecurityLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, "reloginUser", new ValueClass[]{LM.findClassByCompoundName("customUser")});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        DataObject user = context.getSingleKeyValue();
        context.getSession().user.changeCurrentUser(user);
        context.delayUserInterfaction(new UserChangedClientAction());
    }
}