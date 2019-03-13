package lsfusion.server.physics.admin.authentication.policy;

import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.SecurityLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.language.ScriptingActionProperty;

import java.sql.SQLException;

public class UpdatePropertyPolicyCachesProperty extends ScriptingActionProperty {

    public UpdatePropertyPolicyCachesProperty(SecurityLogicsModule LM, ValueClass... valueClasses) {
        super(LM, valueClasses);
    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        context.getSecurityManager().updatePropertyPolicyCaches(context, context.getSingleDataKeyValue());
    }
}
