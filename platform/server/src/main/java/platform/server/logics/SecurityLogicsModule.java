package platform.server.logics;

import org.apache.log4j.Logger;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.PropertyEditType;
import platform.interop.form.layout.ContainerType;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.server.classes.*;
import platform.server.data.Union;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.CompareFilterEntity;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.ContainerView;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.logics.linear.LAP;
import platform.server.logics.linear.LCP;
import platform.server.logics.linear.LP;

import java.util.Arrays;

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
    public LCP permitViewUserRoleProperty;
    public LCP permitViewUserProperty;
    public LCP forbidViewUserRoleProperty;
    public LCP forbidViewUserProperty;
    public LCP permitChangeUserRoleProperty;
    public LCP permitChangeUserProperty;
    public LCP forbidChangeUserRoleProperty;
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
    public LCP permitUserRoleNavigatorElement;
    public LCP forbidUserRoleNavigatorElement;
    public LCP permitUserNavigatorElement;
    public LCP forbidUserNavigarorElement;

    public LCP sidUserRole;
    public LCP userRoleSID;
    private LAP selectUserRoles;
    public LCP inUserRole;
    public LCP inLoginSID;
    public LCP inMainRoleCustomUser;

    public LCP mainRoleUser;
    public LCP mainRoleCustomUser;
    public LCP sidMainRoleCustomUser;
    public LCP nameMainRoleUser;

    public SecurityLogicsModule(T BL, BaseLogicsModule baseLM, Logger logger) {
        super("Security", "Security");
        setBaseLogicsModule(baseLM);
        this.BL = BL;
        this.logger = logger;
    }
    @Override
    public void initModuleDependencies() {
        setRequiredModules(Arrays.asList("System", "Reflection", "Email"));
    }

    @Override
    public void initModule() {
    }

    @Override
    public void initClasses() {
        initBaseClassAliases();
        userRole = addConcreteClass("userRole", getString("logics.role"), BL.LM.baseClass.named);
        groupObject = addConcreteClass("groupObject", getString("logics.group.object"), BL.LM.baseClass);
        policy = addConcreteClass("policy", getString("logics.policy.security.policy"), BL.LM.baseClass.named);
    }

    @Override
    public void initGroups() {
        initBaseGroupAliases();
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
        sidUserRole = addDProp(BL.LM.baseGroup, "sidUserRole", getString("logics.user.identificator"), StringClass.get(30), userRole);
        userRoleSID = addAGProp(idGroup, "userRoleSID", getString("logics.user.role.id"), userRole, sidUserRole);

        // Главная роль
        mainRoleUser = addDProp(idGroup, "mainRoleUser", getString("logics.user.role.main.role.id"), userRole, BL.LM.user);
        mainRoleCustomUser = addJProp(idGroup, "mainRoleCustomUser", getString("logics.user.role.main.role.id"), BL.LM.and1, mainRoleUser, 1, is(baseLM.customUser), 1);
        sidMainRoleCustomUser = addJProp("sidMainRoleCustomUser", getString("logics.user.role.main.role.identificator"), sidUserRole, mainRoleCustomUser, 1);
        nameMainRoleUser = addJProp(BL.LM.baseGroup, "nameMainRoleUser", getString("logics.user.role.main.role"), BL.LM.name, mainRoleUser, 1);

        // Список ролей для пользователей
        inUserRole = addDProp(BL.LM.baseGroup, "inUserRole", getString("logics.user.role.in"), LogicalClass.instance, baseLM.customUser, userRole);
        inLoginSID = addJProp("inLoginSID", true, getString("logics.login.has.a.role"), inUserRole, BL.LM.loginToUser, 1, userRoleSID, 2);
        inMainRoleCustomUser = addSUProp("inMainRoleCustomUser", getString("logics.user.role.in"), Union.OVERRIDE,
                addJProp(BL.LM.equals2, mainRoleCustomUser, 1, 2), inUserRole);
        selectUserRoles = addSelectFromListAction(null, getString("logics.user.role.edit.roles"), inUserRole, userRole, baseLM.customUser);

        // ------------------------ Политика безопасности ------------------ //
        policyName = addAGProp("policyName", getString("logics.policy"), policy, BL.LM.name);
        descriptionPolicy = addDProp(BL.LM.baseGroup, "descriptionPolicy", getString("logics.policy.description"), StringClass.get(100), policy);

        orderUserRolePolicy = addDProp(BL.LM.baseGroup, "orderUserRolePolicy", getString("logics.policy.order"), IntegerClass.instance, userRole, policy);
        orderUserPolicy = addJProp(BL.LM.baseGroup, "orderUserPolicy", getString("logics.policy.order"), orderUserRolePolicy, mainRoleUser, 1, 2);

        // ---- Политики для доменной логики

        // -- Глобальные разрешения для всех ролей
        permitViewProperty = addDProp(BL.LM.baseGroup, "permitViewProperty", getString("logics.policy.permit.property.view"), LogicalClass.instance, BL.reflectionLM.property);
        forbidViewProperty = addDProp(BL.LM.baseGroup, "forbidViewProperty", getString("logics.policy.forbid.property.view"), LogicalClass.instance, BL.reflectionLM.property);
        permitChangeProperty = addDProp(BL.LM.baseGroup, "permitChangeProperty", getString("logics.policy.permit.property.change"), LogicalClass.instance, BL.reflectionLM.property);
        forbidChangeProperty = addDProp(BL.LM.baseGroup, "forbidChangeProperty", getString("logics.policy.forbid.property.change"), LogicalClass.instance, BL.reflectionLM.property);

        // -- Разрешения для каждой роли

        // Разрешения для всех свойств
        permitViewAllPropertyUserRole = addDProp(BL.LM.baseGroup, "permitViewAllPropertyUserRole", getString("logics.user.allow.view.all.property"), LogicalClass.instance, userRole);
        permitViewAllPropertyUser = addJProp(publicGroup, "permitViewAllPropertyUser", getString("logics.user.allow.view.all.property"), permitViewAllPropertyUserRole, mainRoleUser, 1);
        forbidViewAllPropertyUserRole = addDProp(BL.LM.baseGroup, "forbidViewAllPropertyUserRole", getString("logics.user.forbid.view.all.property"), LogicalClass.instance, userRole);
        forbidViewAllPropertyUser = addJProp(publicGroup, "forbidViewAllPropertyUser", getString("logics.user.forbid.view.all.property"), forbidViewAllPropertyUserRole, mainRoleUser, 1);

        permitChangeAllPropertyUserRole = addDProp(BL.LM.baseGroup, "permitChangeAllPropertyUserRole", getString("logics.user.allow.change.all.property"), LogicalClass.instance, userRole);
        permitChangeAllPropertyUser = addJProp(publicGroup, "permitChangeAllPropertyUser", getString("logics.user.allow.change.all.property"), permitChangeAllPropertyUserRole, mainRoleUser, 1);
        forbidChangeAllPropertyUserRole = addDProp(BL.LM.baseGroup, "forbidChangeAllPropertyUserRole", getString("logics.user.forbid.change.all.property"), LogicalClass.instance, userRole);
        forbidChangeAllPropertyRole = addJProp(publicGroup, "forbidChangeAllPropertyRole", getString("logics.user.forbid.change.all.property"), forbidChangeAllPropertyUserRole, mainRoleUser, 1);

        // Разрешения для каждого свойства
        permitViewUserRoleProperty = addDProp(BL.LM.baseGroup, "permitViewUserRoleProperty", getString("logics.policy.permit.property.view"), LogicalClass.instance, userRole, BL.reflectionLM.property);
        permitViewUserProperty = addJProp(BL.LM.baseGroup, "permitViewUserProperty", getString("logics.policy.permit.property.view"), permitViewUserRoleProperty, mainRoleUser, 1, 2);
        forbidViewUserRoleProperty = addDProp(BL.LM.baseGroup, "forbidViewUserRoleProperty", getString("logics.policy.forbid.property.view"), LogicalClass.instance, userRole, BL.reflectionLM.property);
        forbidViewUserProperty = addJProp(BL.LM.baseGroup, "forbidViewUserProperty", getString("logics.policy.forbid.property.view"), forbidViewUserRoleProperty, mainRoleUser, 1, 2);

        permitChangeUserRoleProperty = addDProp(BL.LM.baseGroup, "permitChangeUserRoleProperty", getString("logics.policy.permit.property.change"), LogicalClass.instance, userRole, BL.reflectionLM.property);
        permitChangeUserProperty = addJProp(BL.LM.baseGroup, "permitChangeUserProperty", getString("logics.policy.permit.property.change"), permitChangeUserRoleProperty, mainRoleUser, 1, 2);
        forbidChangeUserRoleProperty = addDProp(BL.LM.baseGroup, "forbidChangeUserRoleProperty", getString("logics.policy.forbid.property.change"), LogicalClass.instance, userRole, BL.reflectionLM.property);
        forbidChangeUserProperty = addJProp(BL.LM.baseGroup, "forbidChangeUserProperty", getString("logics.policy.forbid.property.change"), forbidChangeUserRoleProperty, mainRoleUser, 1, 2);

        notNullPermissionUserProperty = addSUProp("notNullPermissionUserProperty", Union.OVERRIDE, permitViewUserProperty, forbidViewUserProperty, permitChangeUserProperty, forbidChangeUserProperty);

        // ---- Политики для логики представлений

        // Открытие форм по умолчанию
        defaultFormsUserRole = addDProp(BL.LM.baseGroup, "defaultFormsUserRole", getString("logics.user.displaying.forms.by.default"), LogicalClass.instance, userRole);

        defaultNumberUserRoleNavigatorElement = addDProp(BL.LM.baseGroup, "defaultNumberUserRoleNavigatorElement", getString("logics.forms.default.number"), IntegerClass.instance, userRole, BL.reflectionLM.navigatorElement);
        defaultNumberUserNavigatorElement = addJProp(BL.LM.baseGroup, "defaultNumberUserNavigatorElement", getString("logics.forms.default.number"), defaultNumberUserRoleNavigatorElement, mainRoleUser, 1, 2);
        defaultFormsUser = addJProp(publicGroup, "defaultFormsUser", getString("logics.user.displaying.forms.by.default"), defaultFormsUserRole, mainRoleUser, 1);

        // -- Глобальные разрешения для всех ролей
        permitNavigatorElement = addDProp(BL.LM.baseGroup, "permitNavigatorElement", getString("logics.forms.permit.form"), LogicalClass.instance, BL.reflectionLM.navigatorElement);
        forbidNavigatorElement = addDProp(BL.LM.baseGroup, "forbidNavigatorElement", getString("logics.forms.prohibit.form"), LogicalClass.instance, BL.reflectionLM.navigatorElement);

        // -- Разрешения для каждой роли

        // Разрешения для всех элементов
        permitAllFormsUserRole = addDProp(BL.LM.baseGroup, "permitAllFormsUserRole", getString("logics.user.allow.all.user.form"), LogicalClass.instance, userRole);
        permitAllFormsUser = addJProp(publicGroup, "permitAllFormsUser", getString("logics.user.allow.all.user.form"), permitAllFormsUserRole, mainRoleUser, 1);
        forbidAllFormsUserRole = addDProp(BL.LM.baseGroup, "forbidAllFormsUserRole", getString("logics.user.forbid.all.user.form"), LogicalClass.instance, userRole);
        forbidAllFormsUser = addJProp(publicGroup, "forbidAllFormsUser", getString("logics.user.forbid.all.user.form"), forbidAllFormsUserRole, mainRoleUser, 1);

        // Разрешения для каждого элемента
        permitUserRoleNavigatorElement = addDProp(BL.LM.baseGroup, "permitUserRoleNavigatorElement", getString("logics.forms.permit.form"), LogicalClass.instance, userRole, BL.reflectionLM.navigatorElement);
        permitUserNavigatorElement = addJProp(BL.LM.baseGroup, "permitUserNavigatorElement", getString("logics.forms.permit.form"), permitUserRoleNavigatorElement, mainRoleUser, 1, 2);
        forbidUserRoleNavigatorElement = addDProp(BL.LM.baseGroup, "permissionUserRoleForm", getString("logics.forms.prohibit.form"), LogicalClass.instance, userRole, BL.reflectionLM.navigatorElement);
        forbidUserNavigarorElement = addJProp(BL.LM.baseGroup, "permissionUserForm", getString("logics.forms.prohibit.form"), forbidUserRoleNavigatorElement, mainRoleUser, 1, 2);

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

            objUser = addSingleGroupObject(baseLM.customUser, BL.LM.userFirstName, BL.LM.userLastName, BL.LM.userLogin, BL.LM.userPassword, BL.emailLM.emailContact, nameMainRoleUser);
            objUser.groupTo.setSingleClassView(ClassViewType.PANEL);

            objRole = addSingleGroupObject(userRole, BL.LM.name, sidUserRole);
            setEditType(objRole, PropertyEditType.READONLY);

            addPropertyDraw(objUser, objRole, inMainRoleCustomUser);
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

            addPropertyDraw(new LP[]{BL.reflectionLM.captionNavigatorElement, BL.reflectionLM.sidNavigatorElement, BL.reflectionLM.numberNavigatorElement}, objForm);
            addPropertyDraw(new LP[]{BL.reflectionLM.captionNavigatorElement, BL.reflectionLM.sidNavigatorElement, BL.reflectionLM.numberNavigatorElement}, objTreeForm);
            addPropertyDraw(objUserRole, objPolicy, BL.LM.baseGroup, true);
            addPropertyDraw(objUserRole, objForm, permitUserRoleNavigatorElement, forbidUserRoleNavigatorElement);
            addPropertyDraw(objUserRole, objTreeForm, permitUserRoleNavigatorElement, forbidUserRoleNavigatorElement);
            addPropertyDraw(forbidUserRoleNavigatorElement, objUserRole, objTreeForm).toDraw = objUserRole.groupTo;
            addPropertyDraw(objUserRole, objForm, defaultNumberUserRoleNavigatorElement);
            addPropertyDraw(objUserRole, objTreeForm, defaultNumberUserRoleNavigatorElement);
            addPropertyDraw(new LP[]{BL.reflectionLM.captionProperty, BL.reflectionLM.SIDProperty}, objProperty);
            addPropertyDraw(objUserRole, objProperty, permitViewUserRoleProperty, forbidViewUserRoleProperty, permitChangeUserRoleProperty, forbidChangeUserRoleProperty);
            addPropertyDraw(new LP[]{BL.reflectionLM.captionAbstractGroup, BL.reflectionLM.SIDAbstractGroup, BL.reflectionLM.numberAbstractGroup}, objTreeProps);
            addPropertyDraw(new LP[]{BL.reflectionLM.captionProperty, BL.reflectionLM.SIDProperty, BL.reflectionLM.numberProperty}, objProps);
            addPropertyDraw(objUserRole, objProps, permitViewUserRoleProperty, forbidViewUserRoleProperty, permitChangeUserRoleProperty, forbidChangeUserRoleProperty);

            addPropertyDraw(new LP[]{BL.reflectionLM.captionNavigatorElement, BL.reflectionLM.sidNavigatorElement, BL.reflectionLM.numberNavigatorElement, permitNavigatorElement, forbidNavigatorElement}, objDefaultForm);
            addPropertyDraw(new LP[]{BL.reflectionLM.captionNavigatorElement, BL.reflectionLM.sidNavigatorElement, BL.reflectionLM.numberNavigatorElement, permitNavigatorElement, forbidNavigatorElement}, objTreeDefaultForm);
            addPropertyDraw(new LP[]{BL.reflectionLM.captionProperty, BL.reflectionLM.SIDProperty}, objDefaultProperty);
            addPropertyDraw(objDefaultProperty, permitViewProperty, forbidViewProperty, permitChangeProperty, forbidChangeProperty);
            addPropertyDraw(new LP[]{BL.reflectionLM.captionAbstractGroup, BL.reflectionLM.SIDAbstractGroup, BL.reflectionLM.numberAbstractGroup}, objTreeDefaultProps);
            addPropertyDraw(new LP[]{BL.reflectionLM.captionProperty, BL.reflectionLM.SIDProperty, BL.reflectionLM.numberProperty}, objDefaultProps);
            addPropertyDraw(objDefaultProps, permitViewProperty, forbidViewProperty, permitChangeProperty, forbidChangeProperty);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(BL.reflectionLM.parentProperty, objProps), Compare.EQUALS, objTreeProps));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(BL.reflectionLM.parentProperty, objDefaultProps), Compare.EQUALS, objTreeDefaultProps));

            setEditType(BL.reflectionLM.sidNavigatorElement, PropertyEditType.READONLY);
            setEditType(BL.reflectionLM.captionNavigatorElement, PropertyEditType.READONLY);

            PropertyDrawEntity balanceDraw = getPropertyDraw(orderUserRolePolicy, objPolicy.groupTo);
            PropertyDrawEntity sidDraw = getPropertyDraw(sidUserRole, objUserRole.groupTo);
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

            ObjectEntity objUser = addSingleGroupObject(baseLM.customUser, nameMainRoleUser, baseLM.name, baseLM.userLogin, BL.emailLM.emailContact);
            setEditType(objUser, PropertyEditType.READONLY);

            addFormActions(this, objUser);
        }
    }

}
