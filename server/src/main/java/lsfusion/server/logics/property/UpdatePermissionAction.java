package lsfusion.server.logics.property;

import lsfusion.server.language.property.oraction.LAP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.authentication.security.SecurityLogicsModule;
import lsfusion.server.physics.admin.authentication.security.controller.manager.SecurityManager;
import lsfusion.server.physics.admin.authentication.security.policy.ElementSecurityPolicy;
import lsfusion.server.physics.admin.authentication.security.policy.RoleSecurityPolicy;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.util.Iterator;

public class UpdatePermissionAction extends InternalAction {
    private final ClassPropertyInterface userRoleInterface;
    private final ClassPropertyInterface canonicalNameActionOrPropertyInterface;
    private final ClassPropertyInterface staticNamePermissionInterface;
    private final ClassPropertyInterface typeInterface;

    public UpdatePermissionAction(SecurityLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        userRoleInterface = i.next();
        canonicalNameActionOrPropertyInterface = i.next();
        staticNamePermissionInterface = i.next();
        typeInterface = i.next();

    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {

        Long userRole = (Long) context.getKeyValue(userRoleInterface).getValue();
        String actionOrProperty = (String) context.getKeyValue(canonicalNameActionOrPropertyInterface).getValue();
        if(userRole != null && actionOrProperty != null) {
            String permission = (String) context.getKeyValue(staticNamePermissionInterface).getValue();
            String type = (String) context.getKeyValue(typeInterface).getValue();
            SecurityManager securityManager = context.getLogicsInstance().getSecurityManager();
            RoleSecurityPolicy sp = securityManager.cachedSecurityPolicies.get(userRole);
            if (sp != null) {
                ElementSecurityPolicy esp = type.equals("view") ? sp.propertyView : type.equals("change") ? sp.propertyChange : type.equals("editObjects") ? sp.propertyEditObjects : null;
                if (esp != null) {
                    LAP property = context.getBL().findPropertyElseAction(actionOrProperty);
                    if (property != null) {
                        esp.setPermission(property.getActionOrProperty(), securityManager.getPermissionValue(permission));
                    }
                }
            }
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}