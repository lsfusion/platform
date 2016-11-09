package lsfusion.server.logics;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.DefaultFormsType;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.add.MAddMap;
import lsfusion.interop.Compare;
import lsfusion.interop.exceptions.LockedException;
import lsfusion.interop.exceptions.LoginException;
import lsfusion.interop.remote.UserInfo;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.auth.SecurityPolicy;
import lsfusion.server.auth.User;
import lsfusion.server.classes.StringClass;
import lsfusion.server.context.ExecutionStack;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.form.navigator.NavigatorElement;
import lsfusion.server.lifecycle.LifecycleEvent;
import lsfusion.server.lifecycle.LogicsManager;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.property.ActionProperty;
import lsfusion.server.logics.property.ActionPropertyMapImplement;
import lsfusion.server.logics.property.Property;
import lsfusion.server.session.DataSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import javax.naming.CommunicationException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class SecurityManager extends LogicsManager implements InitializingBean {
    private static final Logger startLogger = ServerLoggers.startLogger;
    private static final Logger systemLogger = ServerLoggers.systemLogger;

    public static SecurityPolicy serverSecurityPolicy = new SecurityPolicy();

    private final MAddMap<Integer, SecurityPolicy> policies = MapFact.mAddOverrideMap();
    private final MAddMap<Integer, List<SecurityPolicy>> userPolicies = MapFact.mAddOverrideMap();

    public SecurityPolicy defaultPolicy;
    public SecurityPolicy permitAllPolicy;
    public SecurityPolicy readOnlyPolicy;
    public SecurityPolicy allowConfiguratorPolicy;

    private BusinessLogics<?> businessLogics;
    private DBManager dbManager;

    private String initialAdminPassword;

    private BaseLogicsModule LM;
    private AuthenticationLogicsModule authenticationLM;
    private SecurityLogicsModule securityLM;
    private ReflectionLogicsModule reflectionLM;
    private ContactLogicsModule contactLM;

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
        if (initialAdminPassword == null) {
            initialAdminPassword = "fusion";
        }
    }

    @Override
    protected void onInit(LifecycleEvent event) {
        startLogger.info("Initializing Security Manager.");
        this.LM = businessLogics.LM;
        this.authenticationLM = businessLogics.authenticationLM;
        this.securityLM = businessLogics.securityLM;
        this.reflectionLM = businessLogics.reflectionLM;
        this.contactLM = businessLogics.contactLM;

        try {
            defaultPolicy = new SecurityPolicy();

            permitAllPolicy = addPolicy(getString("logics.policy.allow.all"), getString("logics.policy.allows.all.actions"));
            permitAllPolicy.setReplaceMode(true);

            readOnlyPolicy = addPolicy(getString("logics.policy.forbid.editing.all.properties"), getString("logics.policy.read.only.forbids.editing.of.all.properties.on.the.forms"));
            readOnlyPolicy.property.change.defaultPermission = false;
            readOnlyPolicy.cls.edit.add.defaultPermission = false;
            readOnlyPolicy.cls.edit.change.defaultPermission = false;
            readOnlyPolicy.cls.edit.remove.defaultPermission = false;
            
            for (Property property : businessLogics.getProperties()) {
                if (property.ignoreReadOnlyPolicy()) {
                    readOnlyPolicy.property.change.permit(property);
                    ActionPropertyMapImplement changeAction = property.getEditAction("change");
                    if (changeAction != null) {
                        readOnlyPolicy.property.change.permit(changeAction.property);
                    }
                } else {
                    readOnlyPolicy.property.change.deny(property);
                    if (property instanceof ActionProperty) {
                        readOnlyPolicy.property.view.deny(property);
                    }
                }
            }

            allowConfiguratorPolicy = addPolicy(getString("logics.policy.allow.configurator"), getString("logics.policy.logics.allow.configurator"));
            allowConfiguratorPolicy.configurator = true;
        } catch (SQLException | SQLHandledException e) {
            throw new RuntimeException("Error initializing Security Manager: ", e);
        }
    }

    @Override
    protected void onStarted(LifecycleEvent event) {
        startLogger.info("Starting Security Manager.");
        try {
            businessLogics.initAuthentication(this);
        } catch (SQLException | SQLHandledException e) {
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

    public void setupDefaultAdminUser() throws SQLException, SQLHandledException {
        DataSession session = createSession();
        User user = addUser("admin", initialAdminPassword, session);
        applySecurityPolicy(user, session);
        setUserPolicies(user.ID, permitAllPolicy, allowConfiguratorPolicy);
        session.apply(businessLogics, getStack());
    }

    private DataSession createSession() throws SQLException {
        return dbManager.createSession();
    }

    public SecurityPolicy addPolicy(String policyName, String description) throws SQLException, SQLHandledException {

        Integer policyID;
        try (DataSession session = createSession()) {
            policyID = readPolicy(policyName, session);
            if (policyID == null) {
                DataObject addObject = session.addObject(securityLM.policy);
                securityLM.namePolicy.change(policyName, session, addObject);
                securityLM.descriptionPolicy.change(description, session, addObject);
                policyID = (Integer) addObject.object;
                session.apply(businessLogics, getStack());
            }
        }

        SecurityPolicy policyObject = new SecurityPolicy(policyID);
        putPolicy(policyID, policyObject);
        return policyObject;
    }

    private Integer readPolicy(String name, DataSession session) throws SQLException, SQLHandledException {
        return (Integer) securityLM.policyName.read(session, new DataObject(name, StringClass.get(50)));
    }

    public String addUser(String username, String email, String password, String firstName, String lastName, String localeLanguage, ExecutionStack stack) throws RemoteException {
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
            session.apply(businessLogics, stack);
        } catch (SQLException e) {
            return getString("logics.error.registration");
        } catch (SQLHandledException e) {
            return getString("logics.error.registration");
        }
        return null;
    }

    protected User addUser(String login, String defaultPassword, DataSession session) throws SQLException, SQLHandledException {

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

    public User readUser(String login, DataSession session) throws SQLException, SQLHandledException {
        return readUser(login, false, session);
    }

    public User readUser(String login, boolean insensitive, DataSession session) throws SQLException, SQLHandledException {
        Integer userId = insensitive ? (Integer) authenticationLM.customUserUpcaseLogin.read(session, new DataObject(login.toUpperCase(), StringClass.get(30))) :
                (Integer) authenticationLM.customUserLogin.read(session, new DataObject(login, StringClass.get(30)));
        if (userId == null) {
            return null;
        }
        User userObject = new User(userId);
        applyTimeout(userObject);
        return userObject;
    }
    
    public User readUserWithSecurityPolicy(String login, DataSession session) throws SQLException, SQLHandledException {
        User user = readUser(login, session);
        applySecurityPolicy(user, session);
        return user;
    }
    
    public void applySecurityPolicy(User userObject, DataSession session) throws SQLException, SQLHandledException {
        // политика по умолчанию из кода
        userObject.addSecurityPolicy(defaultPolicy);

        // политики для пользователя, заданные в коде
        List<SecurityPolicy> codeUserPolicy = getUserPolicies(userObject.ID);
        if (codeUserPolicy != null) {
            for (SecurityPolicy policy : codeUserPolicy)
                userObject.addSecurityPolicy(policy);
        }

        // политика по умолчанию из формы "Политика безопасности"
        applyDefaultFormDefinedPolicy(userObject, session);

        // политика для роли из формы "Политика безопасности"
        applyFormDefinedUserPolicy(userObject, session);

        // дополнительные политики из формы "Политика безопасности"
        List<Integer> userPoliciesIds = readUserPoliciesIds(userObject, session);
        for (int policyId : userPoliciesIds) {
            SecurityPolicy policy = getPolicy(policyId);
            if (policy != null) {
                userObject.addSecurityPolicy(policy);
            }
        }
    }

    private void applyTimeout(User user) throws SQLException, SQLHandledException {
        try (DataSession session = createSession()) {

            DataObject userObject = user.getDataObject(authenticationLM.customUser, session);

            QueryBuilder<String, String> qu = new QueryBuilder<>(SetFact.toExclSet("userId"));
            Expr userExpr = qu.getMapExprs().get("userId");
            qu.and(userExpr.compare(userObject, Compare.EQUALS));
            qu.addProperty("transactTimeoutUser", securityLM.transactTimeoutUser.getExpr(session.getModifier(), userExpr));

            ImCol<ImMap<String, Object>> timeoutValues = qu.execute(session).values();
            for (ImMap<String, Object> valueMap : timeoutValues) {
                Integer timeout = (Integer) valueMap.get("transactTimeoutUser");
                if (timeout != null) {
                    user.setTimeout(timeout);
                }
            }
        }
    }

    private List<Integer> readUserPoliciesIds(User user, DataSession session) {
        try {
            ArrayList<Integer> result = new ArrayList<>();

            QueryBuilder<String, Object> q = new QueryBuilder<>(SetFact.toExclSet("userId", "policyId"));
            Expr orderExpr = securityLM.orderUserPolicy.getExpr(session.getModifier(), q.getMapExprs().get("userId"), q.getMapExprs().get("policyId"));

            q.addProperty("pOrder", orderExpr);
            q.and(orderExpr.getWhere());
            q.and(q.getMapExprs().get("userId").compare(user.getDataObject(authenticationLM.customUser, session), Compare.EQUALS));

            ImOrderMap<Object, Boolean> orderBy = MapFact.<Object, Boolean>singletonOrder("pOrder", false);
            ImSet<ImMap<String, Object>> keys = q.execute(session, orderBy, 0).keys();
            if (keys.size() != 0) {
                for (ImMap<String, Object> keyMap : keys) {
                    result.add((Integer) keyMap.get("policyId"));
                }
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public User authenticateUser(DataSession session, String login, String password, ExecutionStack stack) throws SQLException, SQLHandledException {
        boolean needAuthentication = true;
        boolean useLDAP = authenticationLM.useLDAP.read(session) != null;
        User user = readUser(login, useLDAP, session);
        if (useLDAP) {
            String server = (String) authenticationLM.serverLDAP.read(session);
            Integer port = (Integer) authenticationLM.portLDAP.read(session);
            String baseDN = (String) authenticationLM.baseDNLDAP.read(session);
            String userDNSuffix = (String) authenticationLM.userDNSuffixLDAP.read(session);

            try {
                LDAPParameters ldapParameters = new LDAPAuthenticationService(server, port, baseDN, userDNSuffix).authenticate(login, password);
                if (ldapParameters.isConnected()) {
                    needAuthentication = false;
                    if (user == null) {
                        user = addUser(login, password, session);
                    }
                    setUserParameters(user, ldapParameters.getFirstName(), ldapParameters.getLastName(), ldapParameters.getEmail(), ldapParameters.getGroupNames(), session);
                } else {
                    throw new LoginException();
                }
            } catch (CommunicationException e) {
                systemLogger.error("LDAP authentication failed", e);
            }
        }

        if (needAuthentication) {
            if (user == null) {
                throw new LoginException();
            }

            DataObject userObject = user.getDataObject(authenticationLM.customUser, session);

            if (authenticationLM.isLockedCustomUser.read(session, userObject) != null) {
                throw new LockedException();
            }

            if (!isUniversalPassword(password) && !authenticationLM.checkPassword(userObject, password, stack))
                throw new LoginException();
        }
        
        if (user != null) {
            applySecurityPolicy(user, session);
        }

        return user;
    }

    public boolean isUniversalPassword(String password) {
        return "unipass".equals(password.trim()) && Settings.get().getUseUniPass();
    }

    private Map<String, Property> getCanonicalNamesMap() {
        Map<String, Property> result = new HashMap<>();
        for (LP<?, ?> lp : businessLogics.getNamedProperties()) {
            result.put(lp.property.getCanonicalName(), lp.property);
        }
        return result;
    }

    private void applyDefaultFormDefinedPolicy(User user, DataSession session) {
        SecurityPolicy policy = new SecurityPolicy(-1);
        try {
            QueryBuilder<String, String> qf = new QueryBuilder<>(SetFact.singleton("formId"));
            Expr nameExpr = reflectionLM.canonicalNameNavigatorElement.getExpr(session.getModifier(), qf.getMapExprs().get("formId"));
            Expr permitFormExpr = securityLM.permitNavigatorElement.getExpr(session.getModifier(), qf.getMapExprs().get("formId"));
            Expr forbidFormExpr = securityLM.forbidNavigatorElement.getExpr(session.getModifier(), qf.getMapExprs().get("formId"));

            qf.and(nameExpr.getWhere());
            qf.and(permitFormExpr.getWhere().or(forbidFormExpr.getWhere()));

            qf.addProperty("canonicalName", nameExpr);
            qf.addProperty("permit", permitFormExpr);
            qf.addProperty("forbid", forbidFormExpr);

            ImCol<ImMap<String, Object>> formValues = qf.execute(session).values();
            for (ImMap<String, Object> valueMap : formValues) {
                NavigatorElement element = LM.root.getNavigatorElementByCanonicalName(((String) valueMap.get("canonicalName")).trim());
                if (valueMap.get("forbid") != null) {
                    policy.navigator.deny(element);
                } else if (valueMap.get("permit") != null) {
                    policy.navigator.permit(element);
                }
            }

            user.addSecurityPolicy(policy);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void applyFormDefinedUserPolicy(User user, DataSession session) {
        SecurityPolicy policy = new SecurityPolicy(-1);
        try {
            DataObject userObject = user.getDataObject(authenticationLM.customUser, session);

            QueryBuilder<String, String> qu = new QueryBuilder<>(SetFact.toExclSet("userId"));
            Expr userExpr = qu.getMapExprs().get("userId");
            qu.and(userExpr.compare(userObject, Compare.EQUALS));
            qu.addProperty("permitAllForms", securityLM.permitAllFormsUser.getExpr(session.getModifier(), userExpr));
            qu.addProperty("forbidAllForms", securityLM.forbidAllFormsUser.getExpr(session.getModifier(), userExpr));
            qu.addProperty("permitViewAllProperties", securityLM.permitViewAllPropertyUser.getExpr(session.getModifier(), userExpr));
            qu.addProperty("forbidViewAllProperty", securityLM.forbidViewAllPropertyUser.getExpr(session.getModifier(), userExpr));
            qu.addProperty("permitChangeAllProperty", securityLM.permitChangeAllPropertyUser.getExpr(session.getModifier(), userExpr));
            qu.addProperty("forbidChangeAllProperty", securityLM.forbidChangeAllPropertyRole.getExpr(session.getModifier(), userExpr));

            ImCol<ImMap<String, Object>> userPermissionValues = qu.execute(session).values();
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


            QueryBuilder<String, String> qf = new QueryBuilder<>(SetFact.toExclSet("userId", "formId"));
            Expr nameExpr = reflectionLM.canonicalNameNavigatorElement.getExpr(session.getModifier(), qf.getMapExprs().get("formId"));
            Expr permitUserFormExpr = securityLM.overPermitUserNavigatorElement.getExpr(session.getModifier(), qf.getMapExprs().get("userId"), qf.getMapExprs().get("formId"));
            Expr forbidUserFormExpr = securityLM.overForbidUserNavigatorElement.getExpr(session.getModifier(), qf.getMapExprs().get("userId"), qf.getMapExprs().get("formId"));

            qf.and(nameExpr.getWhere());
            qf.and(qf.getMapExprs().get("userId").compare(userObject, Compare.EQUALS));
            qf.and(permitUserFormExpr.getWhere().or(forbidUserFormExpr.getWhere()));

            qf.addProperty("canonicalName", nameExpr);
            qf.addProperty("permit", permitUserFormExpr);
            qf.addProperty("forbid", forbidUserFormExpr);

            ImCol<ImMap<String, Object>> formValues = qf.execute(session).values();
            for (ImMap<String, Object> valueMap : formValues) {
                NavigatorElement element = LM.root.getNavigatorElementByCanonicalName(((String) valueMap.get("canonicalName")).trim());
                if (valueMap.get("forbid") != null)
                    policy.navigator.deny(element);
                else if (valueMap.get("permit") != null)
                    policy.navigator.permit(element);
            }

            QueryBuilder<String, String> qp = new QueryBuilder<>(SetFact.toExclSet("userId", "propertyCN"));
            Expr propExpr = reflectionLM.canonicalNameProperty.getExpr(session.getModifier(), qp.getMapExprs().get("propertyCN"));
            qp.and(propExpr.getWhere());
            qp.and(qp.getMapExprs().get("userId").compare(userObject, Compare.EQUALS));
            qp.and(securityLM.fullForbidViewUserProperty.getExpr(session.getModifier(), qp.getMapExprs().get("userId"), qp.getMapExprs().get("propertyCN")).getWhere().or(
                    securityLM.fullForbidChangeUserProperty.getExpr(session.getModifier(), qp.getMapExprs().get("userId"), qp.getMapExprs().get("propertyCN")).getWhere()));

            qp.addProperty("cn", propExpr);
            qp.addProperty("fullForbidView", securityLM.fullForbidViewUserProperty.getExpr(session.getModifier(), qp.getMapExprs().get("userId"), qp.getMapExprs().get("propertyCN")));
            qp.addProperty("fullForbidChange", securityLM.fullForbidChangeUserProperty.getExpr(session.getModifier(), qp.getMapExprs().get("userId"), qp.getMapExprs().get("propertyCN")));

            ImCol<ImMap<String, Object>> propValues = qp.execute(session).values();
            Map<String, Property> propertyCanonicalNames = getCanonicalNamesMap();
            for (ImMap<String, Object> valueMap : propValues) {
                Property prop = propertyCanonicalNames.get(((String) valueMap.get("cn")).trim());
                if(valueMap.get("fullForbidView") != null)
                    policy.property.view.deny(prop);
                if(valueMap.get("fullForbidChange") != null)
                    policy.property.change.deny(prop);
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
                String name = (String) LM.findProperty("staticName[Object]").read(session, defaultForms);
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
        try (DataSession session = createSession()) {
            KeyExpr navigatorElementExpr = new KeyExpr("navigatorElement");
            ImRevMap<Object, KeyExpr> keys = MapFact.singletonRev((Object) "navigatorElement", navigatorElementExpr);
            QueryBuilder<Object, Object> query = new QueryBuilder<>(keys);
            query.addProperty("canonicalNameNavigatorElement", securityLM.findProperty("canonicalName[NavigatorElement]").getExpr(navigatorElementExpr));
            query.addProperty("overDefaultNumberUserNavigatorElement", securityLM.findProperty("overDefaultNumber[User,NavigatorElement]").getExpr(user.getExpr(), navigatorElementExpr));
            query.and(securityLM.findProperty("overDefaultNumber[User,NavigatorElement]").getExpr(user.getExpr(), navigatorElementExpr).getWhere());
            query.and(securityLM.findProperty("canonicalName[NavigatorElement]").getExpr(navigatorElementExpr).getWhere());

            Map<String, Integer> defaultFormsMap = new HashMap<>();
            ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> result = query.execute(session);
            for (ImMap<Object, Object> entry : result.values()) {
                String canonicalName = (String) entry.get("canonicalNameNavigatorElement");
                Integer number = (Integer) entry.get("overDefaultNumberUserNavigatorElement");
                Integer oldNumber = defaultFormsMap.get(canonicalName);
                Integer newNumber = oldNumber == null ? number : Math.min(oldNumber, number);
                defaultFormsMap.put(canonicalName, newNumber);
            }

            LinkedList<Map.Entry<String, Integer>> defaultFormsList = new LinkedList<>(defaultFormsMap.entrySet());
            Collections.sort(defaultFormsList, new Comparator<Map.Entry<String, Integer>>() {
                @Override
                public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                    return (Integer) (o1.getValue() == null ? o2 : o2.getValue() == null ? 0 : o1.getValue().compareTo(o2.getValue()));
                }
            });
            List<String> defaultForms = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : defaultFormsList) {
                defaultForms.add(entry.getKey());
            }
            return defaultForms;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Object getUserMainRole(DataObject user) throws SQLException, SQLHandledException {
        return securityLM.mainRoleCustomUser.read(createSession(), user);
    }

    public List<String> getUserRolesNames(String username, List<String> extraUserRoleNames) {
        try {
            try (DataSession session = createSession()) {
                ImRevMap<String, KeyExpr> keys = KeyExpr.getMapKeys(SetFact.toExclSet("user", "role"));
                Expr userExpr = keys.get("user");
                Expr roleExpr = keys.get("role");

                QueryBuilder<String, String> q = new QueryBuilder<>(keys);
                q.and(securityLM.inMainRoleCustomUser.getExpr(session.getModifier(), userExpr, roleExpr).getWhere());
                q.and(authenticationLM.loginCustomUser.getExpr(session.getModifier(), userExpr).compare(new DataObject(username), Compare.EQUALS));

                q.addProperty("roleName", securityLM.sidUserRole.getExpr(session.getModifier(), roleExpr));

                ImOrderMap<ImMap<String, Object>, ImMap<String, Object>> values = q.execute(session);

                List<String> roles = new ArrayList<>();
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
            }

        } catch (SQLException e) {
            throw new RuntimeException(getString("logics.info.error.reading.list.of.roles"), e);
        } catch (SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    public void setUserParameters(User user, String firstName, String lastName, String email, List<String> userRoleSIDs, DataSession session) {
        try {

            DataObject customUser = user.getDataObject(authenticationLM.customUser, session);

            if (firstName != null)
                contactLM.firstNameContact.change(firstName, session, (DataObject) customUser);

            if (lastName != null)
                contactLM.lastNameContact.change(lastName, session, (DataObject) customUser);
            
            if (email != null)
                contactLM.emailContact.change(email, session, (DataObject) customUser);

            if (userRoleSIDs != null) {
                for (String userRoleName : userRoleSIDs) {
                    ObjectValue userRole = securityLM.userRoleSID.readClasses(session, new DataObject(userRoleName));

                    if (! (userRole instanceof NullValue)) {
                        securityLM.mainRoleCustomUser.change(userRole.getValue(), session, customUser);
                        break;
                    }
                }
            }
        } catch (SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    public boolean checkPropertyViewPermission(String userName, String propertySID) {
        boolean permitView = false;
        try {
            User user = readUserWithSecurityPolicy(userName, createSession());
            if (user != null) {
                SecurityPolicy policy = user.getSecurityPolicy();
                permitView = policy.property.view.checkPermission(businessLogics.findProperty(propertySID).property);
            }
        } catch (SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
        return permitView;
    }

    public boolean checkPropertyChangePermission(String userName, String propertySID) {
        boolean permitChange = false;
        try {
            User user = readUserWithSecurityPolicy(userName, createSession());
            if (user != null) {
                SecurityPolicy policy = user.getSecurityPolicy();
                permitChange = policy.property.change.checkPermission(businessLogics.findProperty(propertySID).property);
            }
        } catch (SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
        return permitChange;
    }

    public boolean checkDefaultViewPermission(String propertySid) {
        Property property = businessLogics.findProperty(propertySid).property;
        if (defaultPolicy.property.view.checkPermission(property)) {
            try {
                DataSession session = createSession();
                DataObject propertyObject = new DataObject(reflectionLM.propertyCanonicalName.read(session, new DataObject(propertySid)), reflectionLM.property);
                return securityLM.permitViewProperty.read(session, propertyObject) != null;
            } catch (SQLException | SQLHandledException e) {
                throw Throwables.propagate(e);
            }
        }
        return false;
    }

    public boolean checkFormExportPermission(String canonicalName) {
        try {
            DataSession session = createSession();
            Object form = reflectionLM.navigatorElementCanonicalName.read(session, new DataObject(canonicalName));
            if (form == null) {
                throw new RuntimeException(getString("form.navigator.form.with.id.not.found"));
            }
            DataObject formObject = new DataObject(form, reflectionLM.navigatorElement);
            return securityLM.permitExportNavigatorElement.read(session, formObject) != null;
        } catch (SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }
}

