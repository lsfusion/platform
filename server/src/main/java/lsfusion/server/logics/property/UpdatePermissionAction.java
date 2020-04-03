package lsfusion.server.logics.property;

import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.authentication.security.SecurityLogicsModule;
import lsfusion.server.physics.admin.authentication.security.controller.manager.SecurityManager;
import lsfusion.server.physics.admin.authentication.security.policy.AddSecurityPolicy;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.util.Iterator;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.getLogicsInstance;

public class UpdatePermissionAction extends InternalAction {
    private final ClassPropertyInterface sidUserRoleInterface;
    private final ClassPropertyInterface canonicalNameActionOrPropertyInterface;
    private final ClassPropertyInterface staticNamePermissionInterface;
    private final ClassPropertyInterface changeInterface;

    public UpdatePermissionAction(SecurityLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        sidUserRoleInterface = i.next();
        canonicalNameActionOrPropertyInterface = i.next();
        staticNamePermissionInterface = i.next();
        changeInterface = i.next();

    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {

        String userRole = (String) context.getKeyValue(sidUserRoleInterface).getValue();
        String actionOrProperty = (String) context.getKeyValue(canonicalNameActionOrPropertyInterface).getValue();
        String permission = (String) context.getKeyValue(staticNamePermissionInterface).getValue();
        boolean change = context.getKeyValue(changeInterface).getValue() != null;
        if(userRole != null && actionOrProperty != null) {
            SecurityManager securityManager = getLogicsInstance().getSecurityManager();
            AddSecurityPolicy securityPolicy = securityManager.cachedSecurityPolicies.get(userRole);
            if (securityPolicy != null) {
                if(change) {
                    securityPolicy.propertyChange.setPermission(context.getBL().findPropertyElseAction(actionOrProperty), securityManager.getPermissionValue(permission));
                } else {
                    securityPolicy.propertyView.setPermission(context.getBL().findPropertyElseAction(actionOrProperty), securityManager.getPermissionValue(permission));
                }
            }
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}