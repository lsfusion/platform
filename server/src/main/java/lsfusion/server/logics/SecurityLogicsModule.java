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
    public LCP orderUserRolePolicy;
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
    public LCP permitViewUserProperty;
    public LCP forbidViewUserProperty;
    public LCP permitChangeUserProperty;
    public LCP forbidChangeUserProperty;

    public LCP notNullPermissionUserProperty;
    public LCP defaultNumberUserRoleNavigatorElement;
    public LCP defaultNumberUserNavigatorElement;
    public LCP defaultFormsUser;
    public LCP permitNavigatorElement;
    public LCP forbidNavigatorElement;
    public LCP permitExportNavigatorElement;

    public LCP forbidAllFormsUserRole;
    public LCP forbidAllFormsUser;
    public LCP permitAllFormsUserRole;
    public LCP permitAllFormsUser;
    public LCP permitUserNavigatorElement;
    public LCP forbidUserNavigatorElement;

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
        userRole = (ConcreteCustomClass) getClassByName("UserRole");
        policy = (ConcreteCustomClass) getClassByName("Policy");
    }

    @Override
    public void initProperties() throws RecognitionException {
        super.initProperties();
        // ---- Роли
        sidUserRole = getLCPByOldName("sidUserRole");
        nameUserRole = getLCPByOldName("nameUserRole");
        userRoleSID = getLCPByOldName("userRoleSID");
        sidMainRoleCustomUser = getLCPByOldName("sidMainRoleCustomUser");
        nameMainRoleUser = getLCPByOldName("nameMainRoleUser");

        // Список ролей для пользователей
        mainRoleCustomUser = getLCPByOldName("mainRoleCustomUser");
        inMainRoleCustomUser = getLCPByOldName("inMainRoleCustomUser");

        // ------------------------ Политика безопасности ------------------ //
        namePolicy = getLCPByOldName("namePolicy");
        policyName = getLCPByOldName("policyName");
        descriptionPolicy = getLCPByOldName("descriptionPolicy");
        orderUserPolicy = getLCPByOldName("orderUserPolicy");

        // ---- Политики для доменной логики

        // -- Глобальные разрешения для всех ролей
        permitViewProperty = getLCPByOldName("permitViewProperty");
        forbidViewProperty = getLCPByOldName("forbidViewProperty");
        permitChangeProperty = getLCPByOldName("permitChangeProperty");
        forbidChangeProperty = getLCPByOldName("forbidChangeProperty");
        notNullPermissionProperty = getLCPByOldName("notNullPermissionProperty");

        // -- Разрешения для каждой роли

        // Разрешения для всех свойств
        permitViewAllPropertyUser = getLCPByOldName("permitViewAllPropertyUser");
        forbidViewAllPropertyUser = getLCPByOldName("forbidViewAllPropertyUser");
        permitChangeAllPropertyUser = getLCPByOldName("permitChangeAllPropertyUser");
        forbidChangeAllPropertyRole = getLCPByOldName("forbidChangeAllPropertyRole");

        // Разрешения для каждого свойства
        permitViewUserProperty = getLCPByOldName("permitViewUserProperty");
        forbidViewUserProperty = getLCPByOldName("forbidViewUserProperty");
        permitChangeUserProperty = getLCPByOldName("permitChangeUserProperty");
        forbidChangeUserProperty = getLCPByOldName("forbidChangeUserProperty");

        notNullPermissionUserProperty = getLCPByOldName("notNullPermissionUserProperty");
        // ---- Политики для логики представлений

        // Открытие форм по умолчанию
        defaultNumberUserNavigatorElement = getLCPByOldName("defaultNumberUserNavigatorElement");
        defaultFormsUser = getLCPByOldName("defaultFormsUser");

        // -- Глобальные разрешения для всех ролей
        permitNavigatorElement = getLCPByOldName("permitNavigatorElement");
        forbidNavigatorElement = getLCPByOldName("forbidNavigatorElement");
        permitExportNavigatorElement = getLCPByOldName("permitExportNavigatorElement");
        
        // -- Разрешения для каждой роли

        // Разрешения для всех элементов
        permitAllFormsUser = getLCPByOldName("permitAllFormsUser");
        forbidAllFormsUser = getLCPByOldName("forbidAllFormsUser");
        
        // Разрешения для каждого элемента
        permitUserNavigatorElement = getLCPByOldName("permitUserNavigatorElement");
        forbidUserNavigatorElement = getLCPByOldName("forbidUserNavigatorElement");

        propertyPolicyForm = (FormEntity) getNavigatorElementByName("propertyPolicy");
    }
}
