package lsfusion.server.physics.admin.authentication.security;

import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.form.struct.FormEntity;
import org.antlr.runtime.RecognitionException;

import java.io.IOException;


public class SecurityLogicsModule extends ScriptingLogicsModule{

    public ConcreteCustomClass userRole;
    public ConcreteCustomClass policy;

    public LP forbidDuplicateFormsCustomUser;
    public LP showDetailedInfoCustomUser;

    public LP<?> permissionUserRoleNavigatorElement;
    public LP<?> permissionViewUserRoleActionOrProperty;
    public LP<?> permissionChangeUserRoleActionOrProperty;
    public LP<?> permissionEditObjectsUserRoleActionOrProperty;

    public LP userRoleSID;
    public LP firstRoleUser;
    public LP userRolesUser;
    public LP inCustomUserUserRole;
    public LP hasUserRole;
    public LA<?> createSystemUserRoles;
    public LP disableRole;

    public FormEntity propertyPolicyForm;
    public FormEntity actionPolicyForm;

    public SecurityLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(SecurityLogicsModule.class.getResourceAsStream("/system/Security.lsf"), "/system/Security.lsf", baseLM, BL);
    }

    @Override
    public void initMetaAndClasses() throws RecognitionException {
        super.initMetaAndClasses();
        userRole = (ConcreteCustomClass) findClass("UserRole");
        policy = (ConcreteCustomClass) findClass("Policy");
    }

    @Override
    public void initMainLogic() throws RecognitionException {
        super.initMainLogic();
        // ---- Роли
        userRoleSID = findProperty("userRoleSID[STRING[30]]");

        // Список ролей для пользователей
        firstRoleUser = findProperty("firstRole[User]");
        userRolesUser = findProperty("userRoles[User]");
        inCustomUserUserRole = findProperty("in[CustomUser,UserRole]");
        hasUserRole = findProperty("has[User,UserRole]");

        createSystemUserRoles = findAction("createSystemUserRoles[]");

        disableRole = findProperty("disableRole[UserRole]");

        // ---- Политики для доменной логики

        // Разрешения для всех свойств
        forbidDuplicateFormsCustomUser = findProperty("forbidDuplicateForms[CustomUser]");
        showDetailedInfoCustomUser = findProperty("showDetailedInfo[CustomUser]");

        // ---- Политики для логики представлений

        // Разрешения для каждого элемента
        permissionUserRoleNavigatorElement = findProperty("namePermission[UserRole,NavigatorElement]");

        permissionViewUserRoleActionOrProperty = findProperty("namePermissionView[UserRole,ActionOrProperty]");
        permissionChangeUserRoleActionOrProperty = findProperty("namePermissionChange[UserRole,ActionOrProperty]");
        permissionEditObjectsUserRoleActionOrProperty = findProperty("namePermissionEditObjects[UserRole,ActionOrProperty]");

        propertyPolicyForm = findForm("propertyPolicy");
        actionPolicyForm = findForm("actionPolicy");
    }
}
