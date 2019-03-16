package lsfusion.server.physics.admin.authentication.security.policy;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.authentication.security.SecurityLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

public class UpdatePropertyPolicyCachesProperty extends InternalAction {

    public UpdatePropertyPolicyCachesProperty(SecurityLogicsModule LM, ValueClass... valueClasses) {
        super(LM, valueClasses);
    }

    @Override
    public void executeInternal(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        context.getSecurityManager().updatePropertyPolicyCaches(context, context.getSingleDataKeyValue());
    }
}
