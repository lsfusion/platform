package lsfusion.server.logics.security;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.SecurityLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;

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
