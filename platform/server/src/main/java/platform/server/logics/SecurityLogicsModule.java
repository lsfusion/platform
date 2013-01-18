package platform.server.logics;

import org.antlr.runtime.RecognitionException;
import platform.server.classes.*;
import platform.server.logics.linear.LCP;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;

import static platform.server.logics.ServerResourceBundle.getString;


public class SecurityLogicsModule extends ScriptingLogicsModule{
    
    public ConcreteCustomClass userRole;
    public ConcreteCustomClass policy;
    
    protected LCP<?> policyName;
    public LCP descriptionPolicy;
    public LCP orderUserRolePolicy;
    public LCP orderUserPolicy;

    public LCP permitViewProperty;
    public LCP forbidViewProperty;
    public LCP permitChangeProperty;
    public LCP forbidChangeProperty;

    public LCP permitViewAllPropertyUserRole;
    public LCP permitViewAllPropertyUser;
    public LCP forbidChangeAllPropertyUserRole;
    public LCP forbidChangeAllPropertyRole;
    public LCP forbidViewAllPropertyUserRole;
    public LCP forbidViewAllPropertyUser;
    public LCP permitChangeAllPropertyUserRole;
    public LCP permitChangeAllPropertyUser;
    public LCP permitViewUserProperty;
    public LCP forbidViewUserProperty;
    public LCP permitChangeUserProperty;
    public LCP forbidChangeUserProperty;

    public LCP notNullPermissionUserProperty;
    public LCP defaultFormsUserRole;
    public LCP defaultNumberUserRoleNavigatorElement;
    public LCP defaultNumberUserNavigatorElement;
    public LCP defaultFormsUser;
    public LCP permitNavigatorElement;
    public LCP forbidNavigatorElement;

    public LCP forbidAllFormsUserRole;
    public LCP forbidAllFormsUser;
    public LCP permitAllFormsUserRole;
    public LCP permitAllFormsUser;
    public LCP permitUserNavigatorElement;
    public LCP forbidUserNavigatorElement;

    public LCP sidUserRole;
    public LCP userRoleSID;
    public LCP inMainRoleCustomUser;

    public LCP mainRoleUser;
    public LCP sidMainRoleCustomUser;
    public LCP nameMainRoleUser;

    public SecurityLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(SecurityLogicsModule.class.getResourceAsStream("/scripts/system/Security.lsf"), baseLM, BL);
        setBaseLogicsModule(baseLM);
    }

    @Override
    public void initClasses() throws RecognitionException {
        super.initClasses();
        userRole = (ConcreteCustomClass) getClassByName("userRole");
        policy = (ConcreteCustomClass) getClassByName("policy");
    }

    @Override
    public void initProperties() throws RecognitionException {
        super.initProperties();
        // ---- Роли
        sidUserRole = getLCPByName("sidUserRole");
        sidMainRoleCustomUser = getLCPByName("sidMainRoleCustomUser");
        nameMainRoleUser = getLCPByName("nameMainRoleUser");

        // Список ролей для пользователей
        inMainRoleCustomUser = getLCPByName("inMainRoleCustomUser");

        // ------------------------ Политика безопасности ------------------ //
        policyName = getLCPByName("policyName");
        descriptionPolicy = getLCPByName("descriptionPolicy");
        orderUserPolicy = getLCPByName("orderUserPolicy");

        // ---- Политики для доменной логики

        // -- Глобальные разрешения для всех ролей
        permitViewProperty = getLCPByName("permitViewProperty");
        forbidViewProperty = getLCPByName("forbidViewProperty");
        permitChangeProperty = getLCPByName("permitChangeProperty");
        forbidChangeProperty = getLCPByName("forbidChangeProperty");

        // -- Разрешения для каждой роли

        // Разрешения для всех свойств
        permitViewAllPropertyUser = getLCPByName("permitViewAllPropertyUser");
        forbidViewAllPropertyUser = getLCPByName("forbidViewAllPropertyUser");
        permitChangeAllPropertyUser = getLCPByName("permitChangeAllPropertyUser");
        forbidChangeAllPropertyRole = getLCPByName("forbidChangeAllPropertyRole");

        // Разрешения для каждого свойства
        permitViewUserProperty = getLCPByName("permitViewUserProperty");
        forbidViewUserProperty = getLCPByName("forbidViewUserProperty");
        permitChangeUserProperty = getLCPByName("permitChangeUserProperty");
        forbidChangeUserProperty = getLCPByName("forbidChangeUserProperty");

        notNullPermissionUserProperty = getLCPByName("notNullPermissionUserProperty");
        // ---- Политики для логики представлений

        // Открытие форм по умолчанию
        defaultNumberUserNavigatorElement = getLCPByName("defaultNumberUserNavigatorElement");
        defaultFormsUser = getLCPByName("defaultFormsUser");

        // -- Глобальные разрешения для всех ролей
        permitNavigatorElement = getLCPByName("permitNavigatorElement");
        forbidNavigatorElement = getLCPByName("forbidNavigatorElement");
        
        // -- Разрешения для каждой роли

        // Разрешения для всех элементов
        permitAllFormsUser = getLCPByName("permitAllFormsUser");
        forbidAllFormsUser = getLCPByName("forbidAllFormsUser");
        
        // Разрешения для каждого элемента
        permitUserNavigatorElement = getLCPByName("permitUserNavigatorElement");
        forbidUserNavigatorElement = getLCPByName("forbidUserNavigatorElement");
    }
}
