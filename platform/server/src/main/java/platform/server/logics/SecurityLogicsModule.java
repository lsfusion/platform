package platform.server.logics;

import org.apache.log4j.Logger;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.PropertyEditType;
import platform.interop.action.MessageClientAction;
import platform.interop.form.layout.ContainerType;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.server.classes.*;
import platform.server.data.SQLSession;
import platform.server.data.Union;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.CompareFilterEntity;
import platform.server.form.entity.filter.NotNullFilterEntity;
import platform.server.form.entity.filter.RegularFilterEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.ContainerView;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.linear.LAP;
import platform.server.logics.linear.LCP;
import platform.server.logics.linear.LP;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.IncrementType;
import platform.server.logics.property.actions.AdminActionProperty;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.Iterator;

import static platform.server.logics.ServerResourceBundle.getString;


public class SecurityLogicsModule<T extends BusinessLogics<T>> extends LogicsModule {
    Logger logger;
    T BL;

    public T getBL(){
        return BL;
    }
    public ConcreteCustomClass userRole;
    public ConcreteCustomClass groupObject;
    public ConcreteCustomClass policy;
    
    protected LCP<?> nameToPolicy;
    public LCP policyDescription;
    public LCP userRolePolicyOrder;
    public LCP userPolicyOrder;

    public LCP permitViewProperty;
    public LCP forbidViewProperty;
    public LCP permitChangeProperty;
    public LCP forbidChangeProperty;

    public LCP permitViewAllUserRoleProperty;
    public LCP permitViewAllUserForm;
    public LCP forbidChangeAllUserRoleProperty;
    public LCP forbidChangeAllUserForm;
    public LCP forbidViewAllUserRoleProperty;
    public LCP forbidViewAllUserForm;
    public LCP permitChangeAllUserRoleProperty;
    public LCP permitChangeAllUserForm;
    public LCP permitViewUserRoleProperty;
    public LCP permitViewUserProperty;
    public LCP forbidViewUserRoleProperty;
    public LCP forbidViewUserProperty;
    public LCP permitChangeUserRoleProperty;
    public LCP permitChangeUserProperty;
    public LCP forbidChangeUserRoleProperty;
    public LCP forbidChangeUserProperty;

    public LCP notNullPermissionUserProperty;
    public LCP userRoleDefaultForms;
    public LCP userRoleFormDefaultNumber;
    public LCP userFormDefaultNumber;
    public LCP userDefaultForms;
    public LCP permitForm;
    public LCP forbidForm;

    public LCP forbidAllUserRoleForms;
    public LCP forbidAllUserForm;
    public LCP permitAllUserRoleForms;
    public LCP permitAllUserForm;
    public LCP permitUserRoleForm;
    public LCP forbidUserRoleForm;
    public LCP permitUserForm;
    public LCP forbidUserForm;

    public LCP userRoleSID;
    public LCP sidToRole;
    private LAP selectUserRoles;
    public LCP inUserRole;
    public LCP inLoginSID;
    public LCP inUserMainRole;

    public LCP userMainRole;
    public LCP customUserMainRole;
    public LCP customUserSIDMainRole;
    public LCP nameUserMainRole;    

    public SecurityLogicsModule(T BL, BaseLogicsModule baseLM, Logger logger) {
        super("Security", "Security");
        setBaseLogicsModule(baseLM);
        this.BL = BL;
        this.logger = logger;
    }
    @Override
    public void initModuleDependencies() {
    }

    @Override
    public void initModule() {
    }

    @Override
    public void initClasses() {
        userRole = addConcreteClass("userRole", getString("logics.role"), BL.LM.baseClass.named);
        groupObject = addConcreteClass("groupObject", getString("logics.group.object"), BL.LM.baseClass);
        policy = addConcreteClass("policy", getString("logics.policy.security.policy"), BL.LM.baseClass.named);
    }

    @Override
    public void initGroups() {
        idGroup = addAbstractGroup("idGroup", "Идентификаторы", publicGroup, false);
    }

    @Override
    public void initTables() {
        addTable("userRole", userRole);
        addTable("customUserRole", baseLM.customUser, userRole);
        addTable("userRolePolicy", userRole, policy);
        addTable("customUser", baseLM.customUser);
        addTable("groupObjectCustomUser", groupObject, baseLM.customUser);
        addTable("groupObject", groupObject);
        addTable("userRoleProperty", userRole, BL.reflectionLM.property);
        addTable("policy", policy);
    }

    @Override
    public void initProperties() {

        // ---- Роли
        // todo : переименовать в соответствии с namingPolicy
        userRoleSID = addDProp(BL.LM.baseGroup, "userRoleSID", getString("logics.user.identificator"), StringClass.get(30), userRole);
        sidToRole = addAGProp(idGroup, "sidToRole", getString("logics.user.role.id"), userRole, userRoleSID);

        // Главная роль
        userMainRole = addDProp(idGroup, "userMainRole", getString("logics.user.role.main.role.id"), userRole, BL.LM.user);
        customUserMainRole = addJProp(idGroup, "customUserMainRole", getString("logics.user.role.main.role.id"), BL.LM.and1, userMainRole, 1, is(baseLM.customUser), 1);
        customUserSIDMainRole = addJProp("customUserSIDMainRole", getString("logics.user.role.main.role.identificator"), userRoleSID, customUserMainRole, 1);
        nameUserMainRole = addJProp(BL.LM.baseGroup, "nameUserMainRole", getString("logics.user.role.main.role"), BL.LM.name, userMainRole, 1);

        // Список ролей для пользователей
        inUserRole = addDProp(BL.LM.baseGroup, "inUserRole", getString("logics.user.role.in"), LogicalClass.instance, baseLM.customUser, userRole);
        inLoginSID = addJProp("inLoginSID", true, getString("logics.login.has.a.role"), inUserRole, BL.LM.loginToUser, 1, sidToRole, 2);
        inUserMainRole = addSUProp("inUserMainRole", getString("logics.user.role.in"), Union.OVERRIDE,
                addJProp(BL.LM.equals2, customUserMainRole, 1, 2), inUserRole);
        selectUserRoles = addSelectFromListAction(null, getString("logics.user.role.edit.roles"), inUserRole, userRole, baseLM.customUser);

        // ------------------------ Политика безопасности ------------------ //
        nameToPolicy = addAGProp("nameToPolicy", getString("logics.policy"), policy, BL.LM.name);
        policyDescription = addDProp(BL.LM.baseGroup, "policyDescription", getString("logics.policy.description"), StringClass.get(100), policy);

        userRolePolicyOrder = addDProp(BL.LM.baseGroup, "userRolePolicyOrder", getString("logics.policy.order"), IntegerClass.instance, userRole, policy);
        userPolicyOrder = addJProp(BL.LM.baseGroup, "userPolicyOrder", getString("logics.policy.order"), userRolePolicyOrder, userMainRole, 1, 2);

        // ---- Политики для доменной логики

        // -- Глобальные разрешения для всех ролей
        permitViewProperty = addDProp(BL.LM.baseGroup, "permitViewProperty", getString("logics.policy.permit.property.view"), LogicalClass.instance, BL.reflectionLM.property);
        forbidViewProperty = addDProp(BL.LM.baseGroup, "forbidViewProperty", getString("logics.policy.forbid.property.view"), LogicalClass.instance, BL.reflectionLM.property);
        permitChangeProperty = addDProp(BL.LM.baseGroup, "permitChangeProperty", getString("logics.policy.permit.property.change"), LogicalClass.instance, BL.reflectionLM.property);
        forbidChangeProperty = addDProp(BL.LM.baseGroup, "forbidChangeProperty", getString("logics.policy.forbid.property.change"), LogicalClass.instance, BL.reflectionLM.property);

        // -- Разрешения для каждой роли

        // Разрешения для всех свойств
        permitViewAllUserRoleProperty = addDProp(BL.LM.baseGroup, "permitViewAllUserRoleProperty", getString("logics.user.allow.view.all.property"), LogicalClass.instance, userRole);
        permitViewAllUserForm = addJProp(publicGroup, "permitViewAllUserForm", getString("logics.user.allow.view.all.property"), permitViewAllUserRoleProperty, userMainRole, 1);
        forbidViewAllUserRoleProperty = addDProp(BL.LM.baseGroup, "forbidViewAllUserRoleProperty", getString("logics.user.forbid.view.all.property"), LogicalClass.instance, userRole);
        forbidViewAllUserForm = addJProp(publicGroup, "forbidViewAllUserForm", getString("logics.user.forbid.view.all.property"), forbidViewAllUserRoleProperty, userMainRole, 1);

        permitChangeAllUserRoleProperty = addDProp(BL.LM.baseGroup, "permitChangeAllUserRoleProperty", getString("logics.user.allow.change.all.property"), LogicalClass.instance, userRole);
        permitChangeAllUserForm = addJProp(publicGroup, "permitChangeAllUserForm", getString("logics.user.allow.change.all.property"), permitChangeAllUserRoleProperty, userMainRole, 1);
        forbidChangeAllUserRoleProperty = addDProp(BL.LM.baseGroup, "forbidChangeAllUserRoleProperty", getString("logics.user.forbid.change.all.property"), LogicalClass.instance, userRole);
        forbidChangeAllUserForm = addJProp(publicGroup, "forbidChangeAllUserForm", getString("logics.user.forbid.change.all.property"), forbidChangeAllUserRoleProperty, userMainRole, 1);

        // Разрешения для каждого свойства
        permitViewUserRoleProperty = addDProp(BL.LM.baseGroup, "permitViewUserRoleProperty", getString("logics.policy.permit.property.view"), LogicalClass.instance, userRole, BL.reflectionLM.property);
        permitViewUserProperty = addJProp(BL.LM.baseGroup, "permitViewUserProperty", getString("logics.policy.permit.property.view"), permitViewUserRoleProperty, userMainRole, 1, 2);
        forbidViewUserRoleProperty = addDProp(BL.LM.baseGroup, "forbidViewUserRoleProperty", getString("logics.policy.forbid.property.view"), LogicalClass.instance, userRole, BL.reflectionLM.property);
        forbidViewUserProperty = addJProp(BL.LM.baseGroup, "forbidViewUserProperty", getString("logics.policy.forbid.property.view"), forbidViewUserRoleProperty, userMainRole, 1, 2);

        permitChangeUserRoleProperty = addDProp(BL.LM.baseGroup, "permitChangeUserRoleProperty", getString("logics.policy.permit.property.change"), LogicalClass.instance, userRole, BL.reflectionLM.property);
        permitChangeUserProperty = addJProp(BL.LM.baseGroup, "permitChangeUserProperty", getString("logics.policy.permit.property.change"), permitChangeUserRoleProperty, userMainRole, 1, 2);
        forbidChangeUserRoleProperty = addDProp(BL.LM.baseGroup, "forbidChangeUserRoleProperty", getString("logics.policy.forbid.property.change"), LogicalClass.instance, userRole, BL.reflectionLM.property);
        forbidChangeUserProperty = addJProp(BL.LM.baseGroup, "forbidChangeUserProperty", getString("logics.policy.forbid.property.change"), forbidChangeUserRoleProperty, userMainRole, 1, 2);

        notNullPermissionUserProperty = addSUProp("notNullPermissionUserProperty", Union.OVERRIDE, permitViewUserProperty, forbidViewUserProperty, permitChangeUserProperty, forbidChangeUserProperty);

        // ---- Политики для логики представлений

        // Открытие форм по умолчанию
        userRoleDefaultForms = addDProp(BL.LM.baseGroup, "userRoleDefaultForms", getString("logics.user.displaying.forms.by.default"), LogicalClass.instance, userRole);

        userRoleFormDefaultNumber = addDProp(BL.LM.baseGroup, "userRoleFormDefaultNumber", getString("logics.forms.default.number"), IntegerClass.instance, userRole, BL.reflectionLM.navigatorElement);
        userFormDefaultNumber = addJProp(BL.LM.baseGroup, "userFormDefaultNumber", getString("logics.forms.default.number"), userRoleFormDefaultNumber, userMainRole, 1, 2);
        userDefaultForms = addJProp(publicGroup, "userDefaultForms", getString("logics.user.displaying.forms.by.default"), userRoleDefaultForms, userMainRole, 1);

        // -- Глобальные разрешения для всех ролей
        permitForm = addDProp(BL.LM.baseGroup, "permitForm", getString("logics.forms.permit.form"), LogicalClass.instance, BL.reflectionLM.navigatorElement);
        forbidForm = addDProp(BL.LM.baseGroup, "forbidForm", getString("logics.forms.prohibit.form"), LogicalClass.instance, BL.reflectionLM.navigatorElement);

        // -- Разрешения для каждой роли

        // Разрешения для всех элементов
        permitAllUserRoleForms = addDProp(BL.LM.baseGroup, "permitAllUserRoleForms", getString("logics.user.allow.all.user.form"), LogicalClass.instance, userRole);
        permitAllUserForm = addJProp(publicGroup, "permitAllUserForm", getString("logics.user.allow.all.user.form"), permitAllUserRoleForms, userMainRole, 1);
        forbidAllUserRoleForms = addDProp(BL.LM.baseGroup, "forbidAllUserRoleForms", getString("logics.user.forbid.all.user.form"), LogicalClass.instance, userRole);
        forbidAllUserForm = addJProp(publicGroup, "forbidAllUserForm", getString("logics.user.forbid.all.user.form"), forbidAllUserRoleForms, userMainRole, 1);

        // Разрешения для каждого элемента
        permitUserRoleForm = addDProp(BL.LM.baseGroup, "permitUserRoleForm", getString("logics.forms.permit.form"), LogicalClass.instance, userRole, BL.reflectionLM.navigatorElement);
        permitUserForm = addJProp(BL.LM.baseGroup, "permitUserForm", getString("logics.forms.permit.form"), permitUserRoleForm, userMainRole, 1, 2);
        forbidUserRoleForm = addDProp(BL.LM.baseGroup, "permissionUserRoleForm", getString("logics.forms.prohibit.form"), LogicalClass.instance, userRole, BL.reflectionLM.navigatorElement);
        forbidUserForm = addJProp(BL.LM.baseGroup, "permissionUserForm", getString("logics.forms.prohibit.form"), forbidUserRoleForm, userMainRole, 1, 2);

        initNavigators();
    }

    private void initNavigators() {
        addFormEntity(new SecurityPolicyFormEntity(BL.LM.security, "securityPolicyForm"));
        addFormEntity(new UserPolicyFormEntity(BL.LM.security, "userPolicyForm"));

        UserEditFormEntity userEditForm = addFormEntity(new UserEditFormEntity(null, "userEditForm"));
        BL.LM.customUser.setEditForm(userEditForm, userEditForm.objUser);
    }
    
    @Override
    public void initIndexes() {
    }

    @Override
    public String getNamePrefix() {
        return null;
    }

    private class UserEditFormEntity extends FormEntity {

        private final ObjectEntity objUser;
        private final ObjectEntity objRole;

        protected UserEditFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.user.user"));

            objUser = addSingleGroupObject(baseLM.customUser, BL.LM.userFirstName, BL.LM.userLastName, BL.LM.userLogin, BL.LM.userPassword, BL.emailLM.email, nameUserMainRole);
            objUser.groupTo.setSingleClassView(ClassViewType.PANEL);

            objRole = addSingleGroupObject(userRole, BL.LM.name, userRoleSID);
            setEditType(objRole, PropertyEditType.READONLY);

            addPropertyDraw(objUser, objRole, inUserMainRole);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.addIntersection(design.getGroupObjectContainer(objUser.groupTo),
                    design.getGroupObjectContainer(objRole.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            return design;
        }
    }

    private class SecurityPolicyFormEntity extends FormEntity {

        private ObjectEntity objUserRole;
        private ObjectEntity objPolicy;
        private ObjectEntity objForm;
        private ObjectEntity objTreeForm;
        private TreeGroupEntity treeFormObject;
        private ObjectEntity objProperty;
        private ObjectEntity objTreeProps;
        private ObjectEntity objProps;
        private TreeGroupEntity treePropertyObject;
        private ObjectEntity objDefaultForm;
        private ObjectEntity objTreeDefaultForm;
        private TreeGroupEntity treeDefaultForm;
        private ObjectEntity objDefaultProperty;
        private ObjectEntity objTreeDefaultProps;
        private ObjectEntity objDefaultProps;
        private TreeGroupEntity treeDefaultProperty;

        protected SecurityPolicyFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.policy.security.policy"));

            objUserRole = addSingleGroupObject(userRole, BL.LM.baseGroup, true);
            objPolicy = addSingleGroupObject(policy, getString("logics.policy.additional.policies"), BL.LM.baseGroup, true);
            objForm = addSingleGroupObject(BL.reflectionLM.navigatorElement, getString("logics.grid"), true);
            objTreeForm = addSingleGroupObject(BL.reflectionLM.navigatorElement, getString("logics.tree"), true);
            objProperty = addSingleGroupObject(BL.reflectionLM.property, getString("logics.grid"), true);
            objTreeProps = addSingleGroupObject(BL.reflectionLM.abstractGroup, getString("logics.tree"), true);
            objProps = addSingleGroupObject(BL.reflectionLM.property, getString("logics.tree"), true);
            objDefaultForm = addSingleGroupObject(BL.reflectionLM.navigatorElement, getString("logics.grid"), true);
            objTreeDefaultForm = addSingleGroupObject(BL.reflectionLM.navigatorElement, getString("logics.tree"), true);
            objDefaultProperty = addSingleGroupObject(BL.reflectionLM.property, getString("logics.grid"), true);
            objTreeDefaultProps = addSingleGroupObject(BL.reflectionLM.abstractGroup, getString("logics.tree"), true);
            objDefaultProps = addSingleGroupObject(BL.reflectionLM.property, getString("logics.grid"), true);

            objTreeForm.groupTo.setIsParents(addPropertyObject(BL.reflectionLM.parentNavigatorElement, objTreeForm));
            treeFormObject = addTreeGroupObject(objTreeForm.groupTo);
            objTreeProps.groupTo.setIsParents(addPropertyObject(BL.reflectionLM.parentAbstractGroup, objTreeProps));
            treePropertyObject = addTreeGroupObject(objTreeProps.groupTo, objProps.groupTo);

            objTreeDefaultForm.groupTo.setIsParents(addPropertyObject(BL.reflectionLM.parentNavigatorElement, objTreeDefaultForm));
            treeDefaultForm = addTreeGroupObject(objTreeDefaultForm.groupTo);
            objTreeDefaultProps.groupTo.setIsParents(addPropertyObject(BL.reflectionLM.parentAbstractGroup, objTreeDefaultProps));
            treeDefaultProperty = addTreeGroupObject(objTreeDefaultProps.groupTo, objDefaultProps.groupTo);

            addObjectActions(this, objUserRole);

            addPropertyDraw(new LP[]{BL.reflectionLM.navigatorElementCaption, BL.reflectionLM.navigatorElementSID, BL.reflectionLM.numberNavigatorElement}, objForm);
            addPropertyDraw(new LP[]{BL.reflectionLM.navigatorElementCaption, BL.reflectionLM.navigatorElementSID, BL.reflectionLM.numberNavigatorElement}, objTreeForm);
            addPropertyDraw(objUserRole, objPolicy, BL.LM.baseGroup, true);
            addPropertyDraw(objUserRole, objForm, permitUserRoleForm, forbidUserRoleForm);
            addPropertyDraw(objUserRole, objTreeForm, permitUserRoleForm, forbidUserRoleForm);
            addPropertyDraw(forbidUserRoleForm, objUserRole, objTreeForm).toDraw = objUserRole.groupTo;
            addPropertyDraw(objUserRole, objForm, userRoleFormDefaultNumber);
            addPropertyDraw(objUserRole, objTreeForm, userRoleFormDefaultNumber);
            addPropertyDraw(new LP[]{BL.reflectionLM.captionProperty, BL.reflectionLM.SIDProperty}, objProperty);
            addPropertyDraw(objUserRole, objProperty, permitViewUserRoleProperty, forbidViewUserRoleProperty, permitChangeUserRoleProperty, forbidChangeUserRoleProperty);
            addPropertyDraw(new LP[]{BL.reflectionLM.captionAbstractGroup, BL.reflectionLM.SIDAbstractGroup, BL.reflectionLM.numberAbstractGroup}, objTreeProps);
            addPropertyDraw(new LP[]{BL.reflectionLM.captionProperty, BL.reflectionLM.SIDProperty, BL.reflectionLM.numberProperty}, objProps);
            addPropertyDraw(objUserRole, objProps, permitViewUserRoleProperty, forbidViewUserRoleProperty, permitChangeUserRoleProperty, forbidChangeUserRoleProperty);

            addPropertyDraw(new LP[]{BL.reflectionLM.navigatorElementCaption, BL.reflectionLM.navigatorElementSID, BL.reflectionLM.numberNavigatorElement, permitForm, forbidForm}, objDefaultForm);
            addPropertyDraw(new LP[]{BL.reflectionLM.navigatorElementCaption, BL.reflectionLM.navigatorElementSID, BL.reflectionLM.numberNavigatorElement, permitForm, forbidForm}, objTreeDefaultForm);
            addPropertyDraw(new LP[]{BL.reflectionLM.captionProperty, BL.reflectionLM.SIDProperty}, objDefaultProperty);
            addPropertyDraw(objDefaultProperty, permitViewProperty, forbidViewProperty, permitChangeProperty, forbidChangeProperty);
            addPropertyDraw(new LP[]{BL.reflectionLM.captionAbstractGroup, BL.reflectionLM.SIDAbstractGroup, BL.reflectionLM.numberAbstractGroup}, objTreeDefaultProps);
            addPropertyDraw(new LP[]{BL.reflectionLM.captionProperty, BL.reflectionLM.SIDProperty, BL.reflectionLM.numberProperty}, objDefaultProps);
            addPropertyDraw(objDefaultProps, permitViewProperty, forbidViewProperty, permitChangeProperty, forbidChangeProperty);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(BL.reflectionLM.parentProperty, objProps), Compare.EQUALS, objTreeProps));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(BL.reflectionLM.parentProperty, objDefaultProps), Compare.EQUALS, objTreeDefaultProps));

            setEditType(BL.reflectionLM.navigatorElementSID, PropertyEditType.READONLY);
            setEditType(BL.reflectionLM.navigatorElementCaption, PropertyEditType.READONLY);

            PropertyDrawEntity balanceDraw = getPropertyDraw(userRolePolicyOrder, objPolicy.groupTo);
            PropertyDrawEntity sidDraw = getPropertyDraw(userRoleSID, objUserRole.groupTo);
            balanceDraw.addColumnGroupObject(objUserRole.groupTo);
            balanceDraw.setPropertyCaption((CalcPropertyObjectEntity) sidDraw.propertyObject);

            addDefaultOrder(getPropertyDraw(BL.reflectionLM.numberNavigatorElement, objTreeForm.groupTo), true);
            addDefaultOrder(getPropertyDraw(BL.reflectionLM.numberNavigatorElement, objTreeDefaultForm.groupTo), true);
            addDefaultOrder(getPropertyDraw(BL.reflectionLM.SIDProperty, objProperty.groupTo), true);
            addDefaultOrder(getPropertyDraw(BL.reflectionLM.SIDProperty, objDefaultProperty.groupTo), true);
            addDefaultOrder(getPropertyDraw(BL.reflectionLM.numberProperty, objProps.groupTo), true);
            addDefaultOrder(getPropertyDraw(BL.reflectionLM.numberAbstractGroup, objTreeProps.groupTo), true);
            addDefaultOrder(getPropertyDraw(BL.reflectionLM.numberProperty, objDefaultProps.groupTo), true);
            addDefaultOrder(getPropertyDraw(BL.reflectionLM.numberAbstractGroup, objTreeDefaultProps.groupTo), true);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            ContainerView container = design.createContainer();
            container.type = ContainerType.TABBED_PANE;

            ContainerView defaultPolicyContainer = design.createContainer(getString("logics.policy.default"));
            ContainerView defaultFormsContainer = design.createContainer(getString("logics.forms"));
            defaultFormsContainer.type = ContainerType.TABBED_PANE;
            defaultFormsContainer.add(design.getTreeContainer(treeDefaultForm));
            defaultFormsContainer.add(design.getGroupObjectContainer(objDefaultForm.groupTo));
            ContainerView defaultPropertyContainer = design.createContainer(getString("logics.property.properties"));
            defaultPropertyContainer.type = ContainerType.TABBED_PANE;
            defaultPropertyContainer.add(design.getTreeContainer(treeDefaultProperty));
            defaultPropertyContainer.add(design.getGroupObjectContainer(objDefaultProperty.groupTo));
            defaultPolicyContainer.type = ContainerType.TABBED_PANE;
            defaultPolicyContainer.add(defaultFormsContainer);
            defaultPolicyContainer.add(defaultPropertyContainer);

            ContainerView rolesContainer = design.createContainer(getString("logics.policy.roles"));
            ContainerView rolePolicyContainer = design.createContainer();
            rolePolicyContainer.type = ContainerType.TABBED_PANE;
            ContainerView formsContainer = design.createContainer(getString("logics.forms"));
            formsContainer.type = ContainerType.TABBED_PANE;
            formsContainer.add(design.getTreeContainer(treeFormObject));
            formsContainer.add(design.getGroupObjectContainer(objForm.groupTo));
            rolePolicyContainer.add(formsContainer);
            ContainerView propertiesContainer = design.createContainer(getString("logics.property.properties"));
            propertiesContainer.type = ContainerType.TABBED_PANE;
            propertiesContainer.add(design.getTreeContainer(treePropertyObject));
            propertiesContainer.add(design.getGroupObjectContainer(objProperty.groupTo));
            rolePolicyContainer.add(propertiesContainer);
            rolesContainer.add(design.getGroupObjectContainer(objUserRole.groupTo));
            rolesContainer.add(rolePolicyContainer);

            container.add(defaultPolicyContainer);
            container.add(rolesContainer);
            container.add(design.getGroupObjectContainer(objPolicy.groupTo));

            design.getMainContainer().add(0, container);

            return design;
        }
    }

    private class UserPolicyFormEntity extends FormEntity {
        protected UserPolicyFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.user.users"));

            ObjectEntity objUser = addSingleGroupObject(baseLM.customUser, nameUserMainRole, baseLM.name, baseLM.userLogin, BL.emailLM.email);
            setEditType(objUser, PropertyEditType.READONLY);

            addFormActions(this, objUser);
        }
    }

}
