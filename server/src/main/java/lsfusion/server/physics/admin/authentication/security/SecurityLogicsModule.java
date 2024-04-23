package lsfusion.server.physics.admin.authentication.security;

import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.form.struct.FormEntity;
import org.antlr.runtime.RecognitionException;

public class SecurityLogicsModule extends ScriptingLogicsModule {

    public ConcreteCustomClass userRole;

    public LP userRoleSID;
    public LP firstRoleUser;
    public LP userRolesUser;
    public LP inCustomUserUserRole;
    public LP hasUserRole;
    public LA<?> createSystemUserRoles;
    public LP disableRole;

    public LP<?> permissionUserRoleNavigatorElement;
    public LP<?> permissionViewUserRoleActionOrProperty;
    public LP<?> permissionChangeUserRoleActionOrProperty;
    public LP<?> permissionEditObjectsUserRoleActionOrProperty;
    public LP<?> permissionGroupChangeUserRoleActionOrProperty;

    public FormEntity propertyPolicyForm;
    public FormEntity actionPolicyForm;

    public SecurityLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) {
        super(baseLM, BL, "/system/Security.lsf");
    }

    @Override
    public void initMetaAndClasses() throws RecognitionException {
        super.initMetaAndClasses();
        userRole = (ConcreteCustomClass) findClass("UserRole");
    }

    @Override
    public void initMainLogic() throws RecognitionException {
        super.initMainLogic();

        //roles
        userRoleSID = findProperty("userRoleSID[STRING[30]]");
        firstRoleUser = findProperty("firstRole[User]");
        userRolesUser = findProperty("userRoles[User]");
        inCustomUserUserRole = findProperty("in[CustomUser,UserRole]");
        hasUserRole = findProperty("has[User,UserRole]");
        createSystemUserRoles = findAction("createSystemUserRoles[]");
        disableRole = findProperty("disableRole[UserRole]");

        //permissions
        permissionUserRoleNavigatorElement = findProperty("namePermission[UserRole,NavigatorElement]");
        permissionViewUserRoleActionOrProperty = findProperty("namePermissionView[UserRole,ActionOrProperty]");
        permissionChangeUserRoleActionOrProperty = findProperty("namePermissionChange[UserRole,ActionOrProperty]");
        permissionEditObjectsUserRoleActionOrProperty = findProperty("namePermissionEditObjects[UserRole,ActionOrProperty]");
        permissionGroupChangeUserRoleActionOrProperty = findProperty("namePermissionGroupChange[UserRole,ActionOrProperty]");

        //forms
        propertyPolicyForm = findForm("propertyPolicy");
        actionPolicyForm = findForm("actionPolicy");
    }
}
