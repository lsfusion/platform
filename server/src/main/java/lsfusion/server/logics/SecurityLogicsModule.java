package lsfusion.server.logics;

import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.form.entity.FormEntity;
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

    public LCP permitViewAllPropertyUser;
    public LCP forbidChangeAllPropertyRole;
    public LCP forbidViewAllPropertyUser;
    public LCP permitChangeAllPropertyUser;
    public LCP overPermitViewUserProperty;
    public LCP overForbidViewUserProperty;
    public LCP overPermitChangeUserProperty;
    public LCP overForbidChangeUserProperty;

    public LCP notNullPermissionUserProperty;
    public LCP defaultNumberUserNavigatorElement;
    public LCP defaultFormsUser;
    public LCP permitNavigatorElement;
    public LCP forbidNavigatorElement;
    public LCP permitExportNavigatorElement;

    public LCP forbidAllFormsUser;
    public LCP permitAllFormsUser;
    public LCP overPermitUserNavigatorElement;
    public LCP overForbidUserNavigatorElement;

    public LCP transactTimeoutUser;
    
    public LCP sidUserRole;
    public LCP userRoleSID;
    public LCP nameUserRole;
    public LCP mainRoleCustomUser;
    public LCP inMainRoleCustomUser;

    public LCP mainRoleUser;
    public LCP sidMainRoleCustomUser;
    public LCP nameMainRoleUser;

    public FormEntity propertyPolicyForm;

    public SecurityLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(SecurityLogicsModule.class.getResourceAsStream("/lsfusion/system/Security.lsf"), "/lsfusion/system/Security.lsf", baseLM, BL);
        setBaseLogicsModule(baseLM);
    }

    @Override
    public void initClasses() throws RecognitionException {
        super.initClasses();
        userRole = (ConcreteCustomClass) findClass("UserRole");
        policy = (ConcreteCustomClass) findClass("Policy");
    }

    @Override
    public void initProperties() throws RecognitionException {
        super.initProperties();
        // ---- Роли
        sidUserRole = findProperty("sidUserRole");
        nameUserRole = findProperty("nameUserRole");
        userRoleSID = findProperty("userRoleSID");
        sidMainRoleCustomUser = findProperty("sidMainRoleCustomUser");
        nameMainRoleUser = findProperty("nameMainRoleUser");

        // Список ролей для пользователей
        mainRoleCustomUser = findProperty("mainRoleCustomUser");
        inMainRoleCustomUser = findProperty("inMainRoleCustomUser");

        // ------------------------ Политика безопасности ------------------ //
        namePolicy = findProperty("namePolicy");
        policyName = findProperty("policyName");
        descriptionPolicy = findProperty("descriptionPolicy");
        orderUserPolicy = findProperty("orderUserPolicy");

        // ---- Политики для доменной логики

        // -- Глобальные разрешения для всех ролей
        permitViewProperty = findProperty("permitViewProperty");
        forbidViewProperty = findProperty("forbidViewProperty");
        permitChangeProperty = findProperty("permitChangeProperty");
        forbidChangeProperty = findProperty("forbidChangeProperty");
        notNullPermissionProperty = findProperty("notNullPermissionProperty");

        // -- Разрешения для каждой роли

        // Разрешения для всех свойств
        permitViewAllPropertyUser = findProperty("permitViewAllPropertyUser");
        forbidViewAllPropertyUser = findProperty("forbidViewAllPropertyUser");
        permitChangeAllPropertyUser = findProperty("permitChangeAllPropertyUser");
        forbidChangeAllPropertyRole = findProperty("forbidChangeAllPropertyRole");

        // Разрешения для каждого свойства
        overPermitViewUserProperty = findProperty("overPermitViewUserProperty");
        overForbidViewUserProperty = findProperty("overForbidViewUserProperty");
        overPermitChangeUserProperty = findProperty("overPermitChangeUserProperty");
        overForbidChangeUserProperty = findProperty("overForbidChangeUserProperty");

        notNullPermissionUserProperty = findProperty("notNullPermissionUserProperty");
        // ---- Политики для логики представлений

        // Открытие форм по умолчанию
        defaultNumberUserNavigatorElement = findProperty("defaultNumberUserNavigatorElement");
        defaultFormsUser = findProperty("defaultFormsUser");

        // -- Глобальные разрешения для всех ролей
        permitNavigatorElement = findProperty("permitNavigatorElement");
        forbidNavigatorElement = findProperty("forbidNavigatorElement");
        permitExportNavigatorElement = findProperty("permitExportNavigatorElement");
        
        // -- Разрешения для каждой роли

        // Разрешения для всех элементов
        permitAllFormsUser = findProperty("permitAllFormsUser");
        forbidAllFormsUser = findProperty("forbidAllFormsUser");
        
        // Разрешения для каждого элемента
        overPermitUserNavigatorElement = findProperty("overPermitUserNavigatorElement");
        overForbidUserNavigatorElement = findProperty("overForbidUserNavigatorElement");

        transactTimeoutUser = findProperty("transactTimeoutUser");

        propertyPolicyForm = (FormEntity) findNavigatorElement("propertyPolicy");
    }
}
