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
        userRole = (ConcreteCustomClass) getClassByName("UserRole");
        policy = (ConcreteCustomClass) getClassByName("Policy");
    }

    @Override
    public void initProperties() throws RecognitionException {
        super.initProperties();
        // ---- Роли
        sidUserRole = findLCPByCompoundOldName("sidUserRole");
        nameUserRole = findLCPByCompoundOldName("nameUserRole");
        userRoleSID = findLCPByCompoundOldName("userRoleSID");
        sidMainRoleCustomUser = findLCPByCompoundOldName("sidMainRoleCustomUser");
        nameMainRoleUser = findLCPByCompoundOldName("nameMainRoleUser");

        // Список ролей для пользователей
        mainRoleCustomUser = findLCPByCompoundOldName("mainRoleCustomUser");
        inMainRoleCustomUser = findLCPByCompoundOldName("inMainRoleCustomUser");

        // ------------------------ Политика безопасности ------------------ //
        namePolicy = findLCPByCompoundOldName("namePolicy");
        policyName = findLCPByCompoundOldName("policyName");
        descriptionPolicy = findLCPByCompoundOldName("descriptionPolicy");
        orderUserPolicy = findLCPByCompoundOldName("orderUserPolicy");

        // ---- Политики для доменной логики

        // -- Глобальные разрешения для всех ролей
        permitViewProperty = findLCPByCompoundOldName("permitViewProperty");
        forbidViewProperty = findLCPByCompoundOldName("forbidViewProperty");
        permitChangeProperty = findLCPByCompoundOldName("permitChangeProperty");
        forbidChangeProperty = findLCPByCompoundOldName("forbidChangeProperty");
        notNullPermissionProperty = findLCPByCompoundOldName("notNullPermissionProperty");

        // -- Разрешения для каждой роли

        // Разрешения для всех свойств
        permitViewAllPropertyUser = findLCPByCompoundOldName("permitViewAllPropertyUser");
        forbidViewAllPropertyUser = findLCPByCompoundOldName("forbidViewAllPropertyUser");
        permitChangeAllPropertyUser = findLCPByCompoundOldName("permitChangeAllPropertyUser");
        forbidChangeAllPropertyRole = findLCPByCompoundOldName("forbidChangeAllPropertyRole");

        // Разрешения для каждого свойства
        permitViewUserProperty = findLCPByCompoundOldName("permitViewUserProperty");
        forbidViewUserProperty = findLCPByCompoundOldName("forbidViewUserProperty");
        permitChangeUserProperty = findLCPByCompoundOldName("permitChangeUserProperty");
        forbidChangeUserProperty = findLCPByCompoundOldName("forbidChangeUserProperty");

        notNullPermissionUserProperty = findLCPByCompoundOldName("notNullPermissionUserProperty");
        // ---- Политики для логики представлений

        // Открытие форм по умолчанию
        defaultNumberUserNavigatorElement = findLCPByCompoundOldName("defaultNumberUserNavigatorElement");
        defaultFormsUser = findLCPByCompoundOldName("defaultFormsUser");

        // -- Глобальные разрешения для всех ролей
        permitNavigatorElement = findLCPByCompoundOldName("permitNavigatorElement");
        forbidNavigatorElement = findLCPByCompoundOldName("forbidNavigatorElement");
        permitExportNavigatorElement = findLCPByCompoundOldName("permitExportNavigatorElement");
        
        // -- Разрешения для каждой роли

        // Разрешения для всех элементов
        permitAllFormsUser = findLCPByCompoundOldName("permitAllFormsUser");
        forbidAllFormsUser = findLCPByCompoundOldName("forbidAllFormsUser");
        
        // Разрешения для каждого элемента
        permitUserNavigatorElement = findLCPByCompoundOldName("permitUserNavigatorElement");
        forbidUserNavigatorElement = findLCPByCompoundOldName("forbidUserNavigatorElement");

        transactTimeoutUser = findLCPByCompoundOldName("transactTimeoutUser");

        propertyPolicyForm = (FormEntity) getNavigatorElement("propertyPolicy");
    }
}
