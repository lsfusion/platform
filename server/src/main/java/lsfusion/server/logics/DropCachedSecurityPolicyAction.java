package lsfusion.server.logics;

import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.authentication.security.SecurityLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.util.Iterator;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.getLogicsInstance;

public class DropCachedSecurityPolicyAction extends InternalAction {
    private final ClassPropertyInterface userRoleInterface;

    public DropCachedSecurityPolicyAction(SecurityLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        userRoleInterface = i.next();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        Long userRole = (Long) context.getDataKeyValue(userRoleInterface).getValue();
        getLogicsInstance().getSecurityManager().cachedSecurityPolicies.remove(userRole);
    }
}