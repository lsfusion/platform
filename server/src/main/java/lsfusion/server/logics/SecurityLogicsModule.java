package lsfusion.server.logics;

import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.antlr.runtime.RecognitionException;

import java.io.IOException;


public class SecurityLogicsModule extends ScriptingLogicsModule{
    
    public ConcreteCustomClass userRole;
    public ConcreteCustomClass policy;
    
    protected LCP<?> namePolicy;
    protected LCP<?> policyName;
    public LCP descriptionPolicy;
    public LCP orderUserPolicy;

    public LCP permitViewProperty;
    public LCP forbidViewProperty;
    public LCP permitChangeProperty;
    public LCP forbidChangeProperty;
    public LCP notNullPermissionProperty;

    public LCP forbidDuplicateFormsCurrentUser;
    public LCP permitViewAllPropertyUser;
    public LCP forbidChangeAllPropertyRole;
    public LCP forbidViewAllPropertyUser;
    public LCP permitChangeAllPropertyUser;
    public LCP fullForbidViewUserProperty;
    public LCP fullForbidChangeUserProperty;

    public LCP forbidViewAllSetupPolicies;
    public LCP forbidChangeAllSetupPolicies;

    public LCP permitNavigatorElement;
    public LCP forbidNavigatorElement;
    public LCP permitExportNavigatorElement;

    public LCP forbidAllFormsUser;
    public LCP permitAllFormsUser;
    public LCP overPermitUserNavigatorElement;
    public LCP overForbidUserNavigatorElement;

    public LCP cachePropertyPolicyUser;

    public LCP transactTimeoutUser;
    
    public LCP sidUserRole;
    public LCP userRoleSID;
    public LCP nameUserRole;
    public LCP mainRoleCustomUser;
    public LCP inMainRoleCustomUser;

    public LCP mainRoleUser;
    public LCP sidMainRoleCustomUser;
    public LCP nameMainRoleUser;
    public LCP currentUserMainRoleName;

    public LAP copyAccess;    

    public FormEntity propertyPolicyForm;
    public FormEntity actionPolicyForm;

    public SecurityLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(SecurityLogicsModule.class.getResourceAsStream("/system/Security.lsf"), "/system/Security.lsf", baseLM, BL);
    }

    @Override
    public void initMetaGroupsAndClasses() throws RecognitionException {
        super.initMetaGroupsAndClasses();
        userRole = (ConcreteCustomClass) findClass("UserRole");
        policy = (ConcreteCustomClass) findClass("Policy");
    }

    @Override
    public void initProperties() throws RecognitionException {
        super.initProperties();
        // ---- Роли
        sidUserRole = findProperty("sid[UserRole]");
        nameUserRole = findProperty("name[UserRole]");
        userRoleSID = findProperty("userRoleSID[VARSTRING[30]]");
        sidMainRoleCustomUser = findProperty("sidMainRole[CustomUser]");
        nameMainRoleUser = findProperty("nameMainRole[User]");
        currentUserMainRoleName = findProperty("currentUserMainRoleName[]");

        // Список ролей для пользователей
        mainRoleCustomUser = findProperty("mainRole[CustomUser]");
        inMainRoleCustomUser = findProperty("inMainRole[CustomUser,UserRole]");

        // ------------------------ Политика безопасности ------------------ //
        namePolicy = findProperty("name[Policy]");
        policyName = findProperty("policy[VARISTRING[100]]");
        descriptionPolicy = findProperty("description[Policy]");
        orderUserPolicy = findProperty("order[User,Policy]");

        // ---- Политики для доменной логики

        // -- Глобальные разрешения для всех ролей
        permitViewProperty = findProperty("permitView[ActionOrProperty]");
        forbidViewProperty = findProperty("forbidView[ActionOrProperty]");
        permitChangeProperty = findProperty("permitChange[ActionOrProperty]");
        forbidChangeProperty = findProperty("forbidChange[ActionOrProperty]");
        notNullPermissionProperty = findProperty("notNullPermission[ActionOrProperty]");

        // -- Разрешения для каждой роли

        // Разрешения для всех свойств
        forbidDuplicateFormsCurrentUser = findProperty("forbidDuplicateFormsCurrentUser[]");
        permitViewAllPropertyUser = findProperty("permitViewAllProperty[User]");
        forbidViewAllPropertyUser = findProperty("forbidViewAllProperty[User]");
        permitChangeAllPropertyUser = findProperty("permitChangeAllProperty[User]");
        forbidChangeAllPropertyRole = findProperty("forbidChangeAllProperty[User]");

        forbidViewAllSetupPolicies = findProperty("forbidViewAllSetupPolicies[User]");
        forbidChangeAllSetupPolicies = findProperty("forbidChangeAllSetupPolicies[User]");

        // Разрешения для каждого свойства
        fullForbidViewUserProperty = findProperty("fullForbidView[User,ActionOrProperty]");
        fullForbidChangeUserProperty = findProperty("fullForbidChange[User,ActionOrProperty]");

        // ---- Политики для логики представлений

        // -- Глобальные разрешения для всех ролей
        permitNavigatorElement = findProperty("permit[NavigatorElement]");
        forbidNavigatorElement = findProperty("forbid[NavigatorElement]");
        permitExportNavigatorElement = findProperty("permitExport[NavigatorElement]");
        
        // -- Разрешения для каждой роли

        // Разрешения для всех элементов
        permitAllFormsUser = findProperty("permitAllForms[User]");
        forbidAllFormsUser = findProperty("forbidAllForms[User]");
        
        // Разрешения для каждого элемента
        overPermitUserNavigatorElement = findProperty("overPermit[User,NavigatorElement]");
        overForbidUserNavigatorElement = findProperty("overForbid[User,?]");

        cachePropertyPolicyUser = findProperty("cachePropertyPolicy[User]");

        transactTimeoutUser = findProperty("transactTimeout[User]");

        propertyPolicyForm = findForm("propertyPolicy");
        actionPolicyForm = findForm("actionPolicy");

        copyAccess = findAction("copyAccess[]");
    }
}
