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

import static lsfusion.base.BaseUtils.trim;

public class UpdatePermissionAction extends InternalAction {
    private final ClassPropertyInterface userRoleInterface;
    private final ClassPropertyInterface canonicalNameActionOrPropertyInterface;

    private final ClassPropertyInterface isPropertyInterface;
    private final ClassPropertyInterface staticNamePermissionInterface;
    private final ClassPropertyInterface typeInterface;

    public UpdatePermissionAction(SecurityLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        userRoleInterface = i.next();
        canonicalNameActionOrPropertyInterface = i.next();
        isPropertyInterface = i.next();
        staticNamePermissionInterface = i.next();
        typeInterface = i.next();

    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {

        Long userRole = (Long) context.getKeyValue(userRoleInterface).getValue();
        String actionOrPropertyCN = trim((String) context.getKeyValue(canonicalNameActionOrPropertyInterface).getValue());
        boolean isProperty = context.getKeyValue(isPropertyInterface).getValue() != null;
        if(userRole != null && actionOrPropertyCN != null) {
            String permission = (String) context.getKeyValue(staticNamePermissionInterface).getValue();
            String type = (String) context.getKeyValue(typeInterface).getValue();
            SecurityManager securityManager = context.getSecurityManager();
            RoleSecurityPolicy sp = securityManager.cachedSecurityPolicies.get(userRole);
            if (sp != null) {
                ElementSecurityPolicy esp;
                switch (type) {
                    case "view": esp = sp.propertyView; break;
                    case "change": esp = sp.propertyChange; break;
                    case "editObjects": esp = sp.propertyEditObjects; break;
                    case "groupChange": esp = sp.propertyGroupChange; break;
                    default: esp = null;
                }
                if (esp != null) {
                    LAP<?,?> property = isProperty ?
                                            context.getBL().findProperty(actionOrPropertyCN) :
                                            context.getBL().findAction(actionOrPropertyCN);
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