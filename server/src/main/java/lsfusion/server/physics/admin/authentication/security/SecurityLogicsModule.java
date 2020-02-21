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
    
    public LP<?> idPolicy;
    public LP<?> policyId;
    public LP<?> namePolicy;
    public LP descriptionPolicy;
    public LP orderUserPolicy;

    public LP forbidDuplicateFormsCustomUser;
    public LP showDetailedInfoCustomUser;

    public LP forbidViewUserProperty;
    public LP forbidChangeUserProperty;

    public LP forbidViewAllSetupPolicies;
    public LP forbidChangeAllSetupPolicies;
    public LP forbidEditObjects;

    public LP<?> permissionUserNavigatorElement;

    public LP cachePropertyPolicyUser;

    public LP sidUserRole;
    public LP userRoleSID;
    public LP nameUserRole;
    public LP mainRoleCustomUser;
    public LP hasUserRole;
    public LA<?> createSystemUserRoles;

    public LP currentUserMainRoleName;
    public LP nameMainRoleUser;
    public LP currentUserTransactTimeout;
    public LP transactTimeoutUser;

    public LA copyAccess;    

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
        sidUserRole = findProperty("sid[UserRole]");
        nameUserRole = findProperty("name[UserRole]");
        userRoleSID = findProperty("userRoleSID[STRING[30]]");
        currentUserMainRoleName = findProperty("currentUserMainRoleName[]");
        nameMainRoleUser = findProperty("nameMainRole[User]");
        currentUserTransactTimeout = findProperty("currentUserTransactTimeout[]");
        transactTimeoutUser = findProperty("transactTimeout[User]");

        // Список ролей для пользователей
        mainRoleCustomUser = findProperty("mainRole[CustomUser]");
        hasUserRole = findProperty("has[User,UserRole]");

        createSystemUserRoles = findAction("createSystemUserRoles[]");

        // ------------------------ Политика безопасности ------------------ //
        idPolicy = findProperty("id[Policy]");
        policyId = findProperty("policy[STRING[100]]");
        namePolicy = findProperty("name[Policy]");
        descriptionPolicy = findProperty("description[Policy]");
        orderUserPolicy = findProperty("order[User,Policy]");

        // ---- Политики для доменной логики

        // Разрешения для всех свойств
        forbidDuplicateFormsCustomUser = findProperty("forbidDuplicateForms[CustomUser]");
        showDetailedInfoCustomUser = findProperty("showDetailedInfo[CustomUser]");

        forbidViewAllSetupPolicies = findProperty("forbidViewAllSetupPolicies[User]");
        forbidChangeAllSetupPolicies = findProperty("forbidChangeAllSetupPolicies[User]");
        forbidEditObjects = findProperty("forbidEditObjects[User]");

        // Разрешения для каждого свойства
        forbidViewUserProperty = findProperty("forbidView[User,ActionOrProperty]");
        forbidChangeUserProperty = findProperty("forbidChange[User,ActionOrProperty]");

        // ---- Политики для логики представлений

        // Разрешения для каждого элемента
        permissionUserNavigatorElement = findProperty("namePermission[User,NavigatorElement]");

        cachePropertyPolicyUser = findProperty("cachePropertyPolicy[User]");

        propertyPolicyForm = findForm("propertyPolicy");
        actionPolicyForm = findForm("actionPolicy");
    }
}
