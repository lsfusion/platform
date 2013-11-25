package lsfusion.server.logics;

import lsfusion.base.BaseUtils;
import lsfusion.base.DefaultFormsType;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.add.MAddMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.interop.Compare;
import lsfusion.interop.exceptions.LoginException;
import lsfusion.interop.remote.UserInfo;
import lsfusion.server.ServerLoggers;
import lsfusion.server.auth.SecurityPolicy;
import lsfusion.server.auth.User;
import lsfusion.server.classes.StringClass;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.form.navigator.NavigatorElement;
import lsfusion.server.lifecycle.LifecycleAdapter;
import lsfusion.server.lifecycle.LifecycleEvent;
import lsfusion.server.logics.property.Property;
import lsfusion.server.session.DataSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class SecurityManager extends LifecycleAdapter implements InitializingBean {
    private static final Logger logger = ServerLoggers.systemLogger;

    public static SecurityPolicy serverSecurityPolicy = new SecurityPolicy();

    private final MAddMap<Integer, SecurityPolicy> policies = MapFact.mAddOverrideMap();
    private final MAddMap<Integer, List<SecurityPolicy>> userPolicies = MapFact.mAddOverrideMap();

    public SecurityPolicy defaultPolicy;
    public SecurityPolicy permitAllPolicy;
    public SecurityPolicy readOnlyPolicy;
    public SecurityPolicy allowConfiguratorPolicy;

    private BusinessLogics businessLogics;
    private DBManager dbManager;

    private String initialAdminPassword;

    private BaseLogicsModule LM;
    private AuthenticationLogicsModule authenticationLM;
    private SecurityLogicsModule securityLM;
    private ReflectionLogicsModule reflectionLM;

    public SecurityManager() {
        super(SECURITYMANAGER_ORDER);
    }

    public void setBusinessLogics(BusinessLogics businessLogics) {
        this.businessLogics = businessLogics;
    }

    public void setDbManager(DBManager dbManager) {
        this.dbManager = dbManager;
    }

    public void setInitialAdminPassword(String initialAdminPassword) {
        this.initialAdminPassword = initialAdminPassword;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(businessLogics, "businessLogics must be specified");
        Assert.notNull(dbManager, "dbManager must be specified");
        if (BaseUtils.isRedundantString(initialAdminPassword)) {
            initialAdminPassword = "fusion";
        }
    }

    @Override
    protected void onInit(LifecycleEvent event) {
        logger.info("Initializing Security Manager.");
        this.LM = businessLogics.LM;
        this.authenticationLM = businessLogics.authenticationLM;
        this.securityLM = businessLogics.securityLM;
        this.reflectionLM = businessLogics.reflectionLM;

        try {
            defaultPolicy = new SecurityPolicy();

            permitAllPolicy = addPolicy(getString("logics.policy.allow.all"), getString("logics.policy.allows.all.actions"));
            permitAllPolicy.setReplaceMode(true);

            readOnlyPolicy = addPolicy(getString("logics.policy.forbid.editing.all.properties"), getString("logics.policy.read.only.forbids.editing.of.all.properties.on.the.forms"));
            readOnlyPolicy.property.change.defaultPermission = false;
            readOnlyPolicy.cls.edit.add.defaultPermission = false;
            readOnlyPolicy.cls.edit.change.defaultPermission = false;
            readOnlyPolicy.cls.edit.remove.defaultPermission = false;

            allowConfiguratorPolicy = addPolicy(getString("logics.policy.allow.configurator"), getString("logics.policy.logics.allow.configurator"));
            allowConfiguratorPolicy.configurator = true;
        } catch (SQLException e) {
            throw new RuntimeException("Error initializing Security Manager: ", e);
        }
    }

    @Override
    protected void onStarted(LifecycleEvent event) {
        logger.info("Starting Security Manager.");
        try {
            businessLogics.initAuthentication(this);
        } catch (SQLException e) {
            throw new RuntimeException("Error starting Security Manager: ", e);
        }
    }

    public void putPolicy(Integer policyID, SecurityPolicy policy) {
        policies.add(policyID, policy);
    }

    public SecurityPolicy getPolicy(Integer policyID) {
        return policies.get(policyID);
    }

    public void setUserPolicies(int userId, SecurityPolicy... policies) {
        setUserPolicies(userId, Arrays.asList(policies));
    }

    public void setUserPolicies(int userId, List<SecurityPolicy> policies) {
        userPolicies.add(userId, policies);
    }

    public List<SecurityPolicy> getUserPolicies(int userId) {
        return userPolicies.get(userId);
    }

    public void setupDefaultAdminUser() throws SQLException {
        DataSession session = createSession();
        setUserPolicies(addUser("admin", initialAdminPassword, session).ID, permitAllPolicy, allowConfiguratorPolicy);
        session.apply(businessLogics);
    }

    private DataSession createSession() throws SQLException {
        return dbManager.createSession();
    }

    public SecurityPolicy addPolicy(String policyName, String description) throws SQLException {
        DataSession session = createSession();

        Integer policyID = readPolicy(policyName, session);
        if (policyID == null) {
            DataObject addObject = session.addObject(securityLM.policy);
            securityLM.namePolicy.change(policyName, session, addObject);
            securityLM.descriptionPolicy.change(description, session, addObject);
            policyID = (Integer) addObject.object;
            session.apply(businessLogics);
        }

        session.close();

        SecurityPolicy policyObject = new SecurityPolicy(policyID);
        putPolicy(policyID, policyObject);
        return policyObject;
    }

    private Integer readPolicy(String name, DataSession session) throws SQLException {
        return (Integer) securityLM.policyName.read(session, new DataObject(name, StringClass.get(50)));
    }

    public String addUser(String username, String email, String password, String firstName, String lastName, String localeLanguage) throws RemoteException {
        try {
            //todo: в будущем нужно поменять на проставление локали в Context
//            ServerResourceBundle.load(localeLanguage);
            DataSession session = createSession();
            Object userId = authenticationLM.customUserLogin.read(session, new DataObject(username, StringClass.get(30)));
            if (userId != null)
                return getString("logics.error.user.duplicate");

            Object emailId = businessLogics.contactLM.contactEmail.read(session, new DataObject(email, StringClass.get(50)));
            if (emailId != null) {
                return getString("logics.error.emailContact.duplicate");
            }

            DataObject userObject = session.addObject(authenticationLM.customUser);
            authenticationLM.loginCustomUser.change(username, session, userObject);
            businessLogics.contactLM.emailContact.change(email, session, userObject);
            authenticationLM.sha256PasswordCustomUser.change(BaseUtils.calculateBase64Hash("SHA-256", password, UserInfo.salt), session, userObject);
            businessLogics.contactLM.firstNameContact.change(firstName, session, userObject);
            businessLogics.contactLM.lastNameContact.change(lastName, session, userObject);
            session.apply(businessLogics);
        } catch (SQLException e) {
            return getString("logics.error.registration");
        }
        return null;
    }

    protected User addUser(String login, String defaultPassword, DataSession session) throws SQLException {

        User user = readUser(login, session);
        if (user == null) {
            DataObject userObject = session.addObject(authenticationLM.customUser);
            authenticationLM.loginCustomUser.change(login, session, userObject);
            authenticationLM.sha256PasswordCustomUser.change(BaseUtils.calculateBase64Hash("SHA-256", defaultPassword.trim(), UserInfo.salt), session, userObject);
            Integer userID = (Integer) userObject.object;
            user = new User(userID);
        }

        return user;
    }

    public User readUser(String login, DataSession session) throws SQLException {
        Integer userId = (Integer) authenticationLM.customUserLogin.read(session, new DataObject(login, StringClass.get(30)));
        if (userId == null) {
            return null;
        }
        User userObject = new User(userId);

        // политика по умолчанию из кода
        userObject.addSecurityPolicy(defaultPolicy);

        // политики для пользователя, заданные в коде
        List<SecurityPolicy> codeUserPolicy = getUserPolicies(userObject.ID);
        if (codeUserPolicy != null) {
            for (SecurityPolicy policy : codeUserPolicy)
                userObject.addSecurityPolicy(policy);
        }

        // политика по умолчанию из формы "Политика безопасности"
        applyDefaultFormDefinedPolicy(userObject);

        // политика для роли из формы "Политика безопасности"
        applyFormDefinedUserPolicy(userObject);

        // дополнительные политики из формы "Политика безопасности"
        List<Integer> userPoliciesIds = readUserPoliciesIds(userId);
        for (int policyId : userPoliciesIds) {
            SecurityPolicy policy = getPolicy(policyId);
            if (policy != null) {
                userObject.addSecurityPolicy(policy);
            }
        }

        return userObject;
    }

    private List<Integer> readUserPoliciesIds(Integer userId) {
        try {
            ArrayList<Integer> result = new ArrayList<Integer>();
            DataSession session = createSession();

            QueryBuilder<String, Object> q = new QueryBuilder<String, Object>(SetFact.toExclSet("userId", "policyId"));
            Expr orderExpr = securityLM.orderUserPolicy.getExpr(session.getModifier(), q.getMapExprs().get("userId"), q.getMapExprs().get("policyId"));

            q.addProperty("pOrder", orderExpr);
            q.and(orderExpr.getWhere());
            q.and(q.getMapExprs().get("userId").compare(new DataObject(userId, authenticationLM.customUser), Compare.EQUALS));

            ImOrderMap<Object, Boolean> orderBy = MapFact.<Object, Boolean>singletonOrder("pOrder", false);
            ImSet<ImMap<String, Object>> keys = q.execute(session, orderBy, 0).keys();
            if (keys.size() != 0) {
                for (ImMap<String, Object> keyMap : keys) {
                    result.add((Integer) keyMap.get("policyId"));
                }
            }

            session.close();

            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ImRevMap<String, Property> getSIDProperties() {
        return businessLogics.getProperties().mapRevKeys(new GetValue<String, Property>() {
            public String getMapValue(Property value) {
                return value.getSID();
            }
        });
    }

    private void applyDefaultFormDefinedPolicy(User user) {
        SecurityPolicy policy = new SecurityPolicy(-1);
        try {
            DataSession session = createSession();

            QueryBuilder<String, String> qf = new QueryBuilder<String, String>(SetFact.singleton("formId"));
            Expr expr = reflectionLM.sidNavigatorElement.getExpr(session.getModifier(), qf.getMapExprs().get("formId"));
            Expr permitFormExpr = securityLM.permitNavigatorElement.getExpr(session.getModifier(), qf.getMapExprs().get("formId"));
            Expr forbidFormExpr = securityLM.forbidNavigatorElement.getExpr(session.getModifier(), qf.getMapExprs().get("formId"));

            qf.and(expr.getWhere());
            qf.and(permitFormExpr.getWhere().or(forbidFormExpr.getWhere()));

            qf.addProperty("sid", expr);
            qf.addProperty("permit", permitFormExpr);
            qf.addProperty("forbid", forbidFormExpr);

            ImCol<ImMap<String, Object>> formValues = qf.execute(session.sql).values();
            for (ImMap<String, Object> valueMap : formValues) {
                NavigatorElement element = LM.root.getNavigatorElement(((String) valueMap.get("sid")).trim());
                if (valueMap.get("forbid") != null)
                    policy.navigator.deny(element);
                else if (valueMap.get("permit") != null)
                    policy.navigator.permit(element);
            }

            QueryBuilder<String, String> qp = new QueryBuilder<String, String>(SetFact.singleton("propertyId"));
            Expr expr2 = reflectionLM.SIDProperty.getExpr(session.getModifier(), qp.getMapExprs().get("propertyId"));
            qp.and(expr2.getWhere());
            qp.and(securityLM.notNullPermissionProperty.getExpr(session.getModifier(), qp.getMapExprs().get("propertyId")).getWhere());

            qp.addProperty("sid", expr2);
            qp.addProperty("permitView", securityLM.permitViewProperty.getExpr(session.getModifier(), qp.getMapExprs().get("propertyId")));
            qp.addProperty("forbidView", securityLM.forbidViewProperty.getExpr(session.getModifier(), qp.getMapExprs().get("propertyId")));
            qp.addProperty("permitChange", securityLM.permitChangeProperty.getExpr(session.getModifier(), qp.getMapExprs().get("propertyId")));
            qp.addProperty("forbidChange", securityLM.forbidChangeProperty.getExpr(session.getModifier(), qp.getMapExprs().get("propertyId")));

            ImCol<ImMap<String, Object>> propertyValues = qp.execute(session.sql).values();
            ImRevMap<String, Property> sidProperties = getSIDProperties();
            for (ImMap<String, Object> valueMap : propertyValues) {
//                Property prop = businessLogics.getProperty(((String) valueMap.get("sid")).trim());
                Property prop = sidProperties.get(((String) valueMap.get("sid")).trim());
                if (valueMap.get("forbidView") != null)
                    policy.property.view.deny(prop);
                else if (valueMap.get("permitView") != null)
                    policy.property.view.permit(prop);
                if (valueMap.get("forbidChange") != null)
                    policy.property.change.deny(prop);
                else if (valueMap.get("permitChange") != null)
                    policy.property.change.permit(prop);
            }

            user.addSecurityPolicy(policy);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void applyFormDefinedUserPolicy(User user) {
        SecurityPolicy policy = new SecurityPolicy(-1);
        try {
            DataSession session = createSession();

            DataObject userObject = new DataObject(user.ID, authenticationLM.customUser);

            QueryBuilder<String, String> qu = new QueryBuilder<String, String>(SetFact.toExclSet("userId"));
            Expr userExpr = qu.getMapExprs().get("userId");
            qu.and(userExpr.compare(userObject, Compare.EQUALS));
            qu.addProperty("permitAllForms", securityLM.permitAllFormsUser.getExpr(session.getModifier(), userExpr));
            qu.addProperty("forbidAllForms", securityLM.forbidAllFormsUser.getExpr(session.getModifier(), userExpr));
            qu.addProperty("permitViewAllProperties", securityLM.permitViewAllPropertyUser.getExpr(session.getModifier(), userExpr));
            qu.addProperty("forbidViewAllProperty", securityLM.forbidViewAllPropertyUser.getExpr(session.getModifier(), userExpr));
            qu.addProperty("permitChangeAllProperty", securityLM.permitChangeAllPropertyUser.getExpr(session.getModifier(), userExpr));
            qu.addProperty("forbidChangeAllProperty", securityLM.forbidChangeAllPropertyRole.getExpr(session.getModifier(), userExpr));

            ImCol<ImMap<String, Object>> userPermissionValues = qu.execute(session.sql).values();
            for (ImMap<String, Object> valueMap : userPermissionValues) {
                if (valueMap.get("forbidAllForms") != null)
                    policy.navigator.defaultPermission = false;
                else if (valueMap.get("permitAllForms") != null)
                    policy.navigator.defaultPermission = true;

                if (valueMap.get("forbidViewAllProperty") != null)
                    policy.property.view.defaultPermission = false;
                else if (valueMap.get("permitViewAllProperties") != null)
                    policy.property.view.defaultPermission = true;

                if (valueMap.get("forbidChangeAllProperty") != null)
                    policy.property.change.defaultPermission = false;
                else if (valueMap.get("permitChangeAllProperty") != null)
                    policy.property.change.defaultPermission = true;
            }


            QueryBuilder<String, String> qf = new QueryBuilder<String, String>(SetFact.toExclSet("userId", "formId"));
            Expr formExpr = reflectionLM.sidNavigatorElement.getExpr(session.getModifier(), qf.getMapExprs().get("formId"));
            Expr permitUserFormExpr = securityLM.permitUserNavigatorElement.getExpr(session.getModifier(), qf.getMapExprs().get("userId"), qf.getMapExprs().get("formId"));
            Expr forbidUserFormExpr = securityLM.forbidUserNavigatorElement.getExpr(session.getModifier(), qf.getMapExprs().get("userId"), qf.getMapExprs().get("formId"));

            qf.and(formExpr.getWhere());
            qf.and(qf.getMapExprs().get("userId").compare(userObject, Compare.EQUALS));
            qf.and(permitUserFormExpr.getWhere().or(forbidUserFormExpr.getWhere()));

            qf.addProperty("sid", formExpr);
            qf.addProperty("permit", permitUserFormExpr);
            qf.addProperty("forbid", forbidUserFormExpr);

            ImCol<ImMap<String, Object>> formValues = qf.execute(session.sql).values();
            for (ImMap<String, Object> valueMap : formValues) {
                NavigatorElement element = LM.root.getNavigatorElement(((String) valueMap.get("sid")).trim());
                if (valueMap.get("forbid") != null)
                    policy.navigator.deny(element);
                else if (valueMap.get("permit") != null)
                    policy.navigator.permit(element);
            }

            QueryBuilder<String, String> qp = new QueryBuilder<String, String>(SetFact.toExclSet("userId", "propertyId"));
            Expr propExpr = reflectionLM.SIDProperty.getExpr(session.getModifier(), qp.getMapExprs().get("propertyId"));
            qp.and(propExpr.getWhere());
            qp.and(qp.getMapExprs().get("userId").compare(userObject, Compare.EQUALS));
            qp.and(securityLM.notNullPermissionUserProperty.getExpr(session.getModifier(), qp.getMapExprs().get("userId"), qp.getMapExprs().get("propertyId")).getWhere());

            qp.addProperty("sid", propExpr);
            qp.addProperty("permitView", securityLM.permitViewUserProperty.getExpr(session.getModifier(), qp.getMapExprs().get("userId"), qp.getMapExprs().get("propertyId")));
            qp.addProperty("forbidView", securityLM.forbidViewUserProperty.getExpr(session.getModifier(), qp.getMapExprs().get("userId"), qp.getMapExprs().get("propertyId")));
            qp.addProperty("permitChange", securityLM.permitChangeUserProperty.getExpr(session.getModifier(), qp.getMapExprs().get("userId"), qp.getMapExprs().get("propertyId")));
            qp.addProperty("forbidChange", securityLM.forbidChangeUserProperty.getExpr(session.getModifier(), qp.getMapExprs().get("userId"), qp.getMapExprs().get("propertyId")));

            ImCol<ImMap<String, Object>> propValues = qp.execute(session.sql).values();
            ImRevMap<String, Property> sidProperties = getSIDProperties();
            for (ImMap<String, Object> valueMap : propValues) {
                Property prop = sidProperties.get(((String) valueMap.get("sid")).trim());
                if (valueMap.get("forbidView") != null)
                    policy.property.view.deny(prop);
                else if (valueMap.get("permitView") != null)
                    policy.property.view.permit(prop);
                if (valueMap.get("forbidChange") != null)
                    policy.property.change.deny(prop);
                else if (valueMap.get("permitChange") != null)
                    policy.property.change.permit(prop);
            }

            user.addSecurityPolicy(policy);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public DefaultFormsType showDefaultForms(DataObject user) {
        try {
            DataSession session = createSession();

            ObjectValue defaultForms = securityLM.defaultFormsUser.readClasses(session, user);
            if (defaultForms instanceof NullValue) return DefaultFormsType.NONE;
            else {
                String name = (String) LM.getLCPByName("staticName").read(session, defaultForms);
                if (name.contains("default"))
                    return DefaultFormsType.DEFAULT;
                else if (name.contains("restore"))
                    return DefaultFormsType.RESTORE;
                else return DefaultFormsType.NONE;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getDefaultForms(DataObject user) {
        try {
            DataSession session = createSession();

            QueryBuilder<String, String> q = new QueryBuilder<String, String>(SetFact.toExclSet("userId", "formId"));
            Expr expr = securityLM.defaultNumberUserNavigatorElement.getExpr(session.getModifier(), q.getMapExprs().get("userId"), q.getMapExprs().get("formId"));
            q.and(expr.getWhere());
            q.and(q.getMapExprs().get("userId").compare(user, Compare.EQUALS));

            q.addProperty("sid", reflectionLM.sidNavigatorElement.getExpr(session.getModifier(), q.getMapExprs().get("formId")));
            q.addProperty("number", securityLM.defaultNumberUserNavigatorElement.getExpr(session.getModifier(), q.getMapExprs().get("userId"), q.getMapExprs().get("formId")));


            ImCol<ImMap<String, Object>> values = q.execute(session.sql).values();
            ArrayList<String> result = new ArrayList<String>();
            Map<String, String> sortedValues = new TreeMap<String, String>();
            for (ImMap<String, Object> valueMap : values) {
                String sid = (String) valueMap.get("sid");
                Integer number = (Integer) valueMap.get("number");
                sortedValues.put(number.toString() + Character.MIN_VALUE, sid);
            }

            for (String sid : sortedValues.values()) {
                result.add(sid);
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public UserInfo getUserInfo(String username, List<String> extraUserRoleNames) {
        try {
            DataSession session = createSession();
            try {
                //User user = readUser(username, session);
                //if (user == null) {
                //    throw new LoginException();
                //}
                Integer userId = (Integer) authenticationLM.customUserLogin.read(session, new DataObject(username, StringClass.get(30)));
                if (userId == null) {
                    throw new LoginException();
                }
                DataObject userObject = new DataObject(userId, authenticationLM.customUser);
                String password = (String) authenticationLM.sha256PasswordCustomUser.read(session, userObject);
                Boolean isLocked = (Boolean) authenticationLM.isLockedCustomUser.read(session, userObject);
                if (password != null)
                    password = password.trim();

                return new UserInfo(isLocked, username, password, getUserRolesNames(username, extraUserRoleNames));
            } finally {
                session.close();
            }
        } catch (SQLException se) {
            throw new RuntimeException(getString("logics.info.error.reading.user.data"), se);
        }
    }

    private List<String> getUserRolesNames(String username, List<String> extraUserRoleNames) {
        try {
            DataSession session = createSession();
            try {
                ImRevMap<String, KeyExpr> keys = KeyExpr.getMapKeys(SetFact.toExclSet("user", "role"));
                Expr userExpr = keys.get("user");
                Expr roleExpr = keys.get("role");

                QueryBuilder<String, String> q = new QueryBuilder<String, String>(keys);
                q.and(securityLM.inMainRoleCustomUser.getExpr(session.getModifier(), userExpr, roleExpr).getWhere());
                q.and(authenticationLM.loginCustomUser.getExpr(session.getModifier(), userExpr).compare(new DataObject(username), Compare.EQUALS));

                q.addProperty("roleName", securityLM.sidUserRole.getExpr(session.getModifier(), roleExpr));

                ImOrderMap<ImMap<String, Object>, ImMap<String, Object>> values = q.execute(session.sql);

                List<String> roles = new ArrayList<String>();
                for (ImMap<String, Object> value : values.valueIt()) {
                    Object rn = value.get("roleName");
                    if (rn instanceof String) {
                        String roleName = ((String) rn).trim();
                        if (!roleName.isEmpty()) {
                            roles.add(roleName);
                        }
                    }
                }
                roles.addAll(extraUserRoleNames);

                return roles;
            } finally {
                session.close();
            }

        } catch (SQLException e) {
            throw new RuntimeException(getString("logics.info.error.reading.list.of.roles"), e);
        }
    }

    public void setMainRoleCustomUser(User user, String userRoleSID, DataSession session) {
        try {

            if (userRoleSID != null) {
                DataObject customUser = new DataObject(user.ID, authenticationLM.customUser);
                ObjectValue userRole = securityLM.userRoleSID.readClasses(session, new DataObject(userRoleSID));

                if (userRole instanceof NullValue) {
                    userRole = session.addObject(securityLM.userRole);
                    securityLM.sidUserRole.change(userRoleSID, session, (DataObject) userRole);
                    securityLM.nameUserRole.change(userRoleSID, session, (DataObject) userRole);
                }

                securityLM.mainRoleCustomUser.change(userRole.getValue(), session, customUser);
            }
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public boolean checkPropertyViewPermission(String userName, String propertySID) {
        boolean permitView = false;
        try {
            User user = readUser(userName, createSession());
            if (user != null) {
                SecurityPolicy policy = user.getSecurityPolicy();
                permitView = policy.property.view.checkPermission(businessLogics.getProperty(propertySID));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return permitView;
    }

    public boolean checkPropertyChangePermission(String userName, String propertySID) {
        boolean permitChange = false;
        try {
            User user = readUser(userName, createSession());
            if (user != null) {
                SecurityPolicy policy = user.getSecurityPolicy();
                permitChange = policy.property.change.checkPermission(businessLogics.getProperty(propertySID));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return permitChange;
    }

    public boolean checkDefaultViewPermission(String propertySid) {
        Property property = businessLogics.getProperty(propertySid);
        if (defaultPolicy.property.view.checkPermission(property)) {
            try {
                DataSession session = createSession();
                DataObject propertyObject = new DataObject(reflectionLM.propertySID.read(session, new DataObject(propertySid)), reflectionLM.property);
                return securityLM.permitViewProperty.read(session, propertyObject) != null;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    public boolean checkFormExportPermission(String formSid) {
        try {
            DataSession session = createSession();
            Object form = reflectionLM.navigatorElementSID.read(session, new DataObject(formSid));
            if (form == null) {
                throw new RuntimeException(getString("form.navigator.form.with.id.not.found"));
            }
            DataObject formObject = new DataObject(form, reflectionLM.navigatorElement);
            return securityLM.permitExportNavigatorElement.read(session, formObject) != null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

