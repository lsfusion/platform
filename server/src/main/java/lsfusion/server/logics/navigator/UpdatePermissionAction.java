package lsfusion.server.logics.navigator;

import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.authentication.security.SecurityLogicsModule;
import lsfusion.server.physics.admin.authentication.security.controller.manager.SecurityManager;
import lsfusion.server.physics.admin.authentication.security.policy.RoleSecurityPolicy;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.util.Iterator;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.getLogicsInstance;

public class UpdatePermissionAction extends InternalAction {
    private final ClassPropertyInterface sidUserRoleInterface;
    private final ClassPropertyInterface canonicalNameNavigatorElementInterface;
    private final ClassPropertyInterface staticNamePermissionInterface;

    public UpdatePermissionAction(SecurityLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        sidUserRoleInterface = i.next();
        canonicalNameNavigatorElementInterface = i.next();
        staticNamePermissionInterface = i.next();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {

        String userRole = (String) context.getKeyValue(sidUserRoleInterface).getValue();
        String navigatorElement = (String) context.getKeyValue(canonicalNameNavigatorElementInterface).getValue();
        if(userRole != null && navigatorElement != null) {
            SecurityManager securityManager = getLogicsInstance().getSecurityManager();
            Boolean permission = securityManager.getPermissionValue(context.getKeyValue(staticNamePermissionInterface).getValue());
            RoleSecurityPolicy securityPolicy = securityManager.cachedSecurityPolicies.get(userRole);
            if (securityPolicy != null) {
                securityPolicy.navigator.setPermission(context.getBL().findNavigatorElement(navigatorElement), permission);
            }
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}