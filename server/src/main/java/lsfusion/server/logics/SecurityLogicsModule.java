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

    public LCP forbidDuplicateFormsCustomUser;
    public LCP permitViewAllPropertyUser;
    public LCP forbidChangeAllPropertyRole;
    public LCP forbidViewAllPropertyUser;
    public LCP permitChangeAllPropertyUser;
    public LCP forbidViewUserProperty;
    public LCP forbidChangeUserProperty;

    public LCP forbidViewAllSetupPolicies;
    public LCP forbidChangeAllSetupPolicies;

    public LCP permitNavigatorElement;
    public LCP forbidNavigatorElement;

    public LCP forbidAllFormsUser;
    public LCP permitAllFormsUser;
    public LCP permitUserNavigatorElement;
    public LCP forbidUserNavigatorElement;

    public LCP cachePropertyPolicyUser;

    public LCP sidUserRole;
    public LCP userRoleSID;
    public LCP nameUserRole;
    public LCP mainRoleCustomUser;
    public LCP hasUserRole;

    public LCP currentUserMainRoleName;
    public LCP currentUserTransactTimeout;

    public LAP copyAccess;    

    public FormEntity propertyPolicyForm;
    public FormEntity actionPolicyForm;

    public LCP platformVersion;
    public LCP apiVersion;

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
        userRoleSID = findProperty("userRoleSID[VARSTRING[30]]");
        currentUserMainRoleName = findProperty("currentUserMainRoleName[]");
        currentUserTransactTimeout = findProperty("currentUserTransactTimeout[]");

        // Список ролей для пользователей
        mainRoleCustomUser = findProperty("mainRole[CustomUser]");
        hasUserRole = findProperty("has[User,UserRole]");

        // ------------------------ Политика безопасности ------------------ //
        namePolicy = findProperty("name[Policy]");
        policyName = findProperty("policy[VARISTRING[100]]");
        descriptionPolicy = findProperty("description[Policy]");
        orderUserPolicy = findProperty("order[User,Policy]");

        // ---- Политики для доменной логики

        // Разрешения для всех свойств
        forbidDuplicateFormsCustomUser = findProperty("forbidDuplicateForms[CustomUser]");
        permitViewAllPropertyUser = findProperty("permitViewAllProperty[User]");
        forbidViewAllPropertyUser = findProperty("forbidViewAllProperty[User]");
        permitChangeAllPropertyUser = findProperty("permitChangeAllProperty[User]");
        forbidChangeAllPropertyRole = findProperty("forbidChangeAllProperty[User]");

        forbidViewAllSetupPolicies = findProperty("forbidViewAllSetupPolicies[User]");
        forbidChangeAllSetupPolicies = findProperty("forbidChangeAllSetupPolicies[User]");

        // Разрешения для каждого свойства
        forbidViewUserProperty = findProperty("forbidView[User,ActionOrProperty]");
        forbidChangeUserProperty = findProperty("forbidChange[User,ActionOrProperty]");

        // ---- Политики для логики представлений

        // -- Глобальные разрешения для всех ролей
        permitNavigatorElement = findProperty("permit[NavigatorElement]");
        forbidNavigatorElement = findProperty("forbid[NavigatorElement]");
        
        // -- Разрешения для каждой роли

        // Разрешения для всех элементов
        permitAllFormsUser = findProperty("permitAllForms[User]");
        forbidAllFormsUser = findProperty("forbidAllForms[User]");
        
        // Разрешения для каждого элемента
        permitUserNavigatorElement = findProperty("permit[User,NavigatorElement]");
        forbidUserNavigatorElement = findProperty("forbid[User,NavigatorElement]");

        cachePropertyPolicyUser = findProperty("cachePropertyPolicy[User]");

        propertyPolicyForm = findForm("propertyPolicy");
        actionPolicyForm = findForm("actionPolicy");

        platformVersion = findProperty("platformVersion[]");
        apiVersion = findProperty("apiVersion[]");
    }
}
