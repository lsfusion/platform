package lsfusion.server.logics;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.add.MAddMap;
import lsfusion.base.col.lru.LRUSVSMap;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.interop.Compare;
import lsfusion.interop.exceptions.LockedException;
import lsfusion.interop.exceptions.LoginException;
import lsfusion.interop.form.ServerResponse;
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
import lsfusion.server.form.entity.ActionPropertyObjectEntity;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.navigator.NavigatorElement;
import lsfusion.server.lifecycle.LifecycleEvent;
import lsfusion.server.lifecycle.LogicsManager;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.Property;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.Modifier;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import javax.naming.CommunicationException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;

import static lsfusion.server.context.ThreadLocalContext.localize;

public class SecurityManager extends LogicsManager implements InitializingBean {
    private static final Logger startLogger = ServerLoggers.startLogger;
    private static final Logger systemLogger = ServerLoggers.systemLogger;

    public static SecurityPolicy serverSecurityPolicy = new SecurityPolicy();

    private final MAddMap<Long, SecurityPolicy> policies = MapFact.mAddOverrideMap();
    private final MAddMap<Long, List<SecurityPolicy>> userPolicies = MapFact.mAddOverrideMap();

    public SecurityPolicy defaultPolicy;
    public SecurityPolicy permitAllPolicy;
    public SecurityPolicy readOnlyPolicy;
    public SecurityPolicy allowConfiguratorPolicy;

    private BusinessLogics businessLogics;
    private DBManager dbManager;

    @Override
    protected BusinessLogics getBusinessLogics() {
        return businessLogics;
    }

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

        try {
            defaultPolicy = new SecurityPolicy();

            permitAllPolicy = addPolicy(localize("{logics.policy.allow.all}"), localize("{logics.policy.allows.all.actions}"));
            permitAllPolicy.setReplaceMode(true);

            readOnlyPolicy = addPolicy(localize("{logics.policy.forbid.editing.all.properties}"), localize("{logics.policy.read.only.forbids.editing.of.all.properties.on.the.forms}"));
            readOnlyPolicy.property.change.defaultPermission = false;
            readOnlyPolicy.cls.edit.add.defaultPermission = false;
            readOnlyPolicy.cls.edit.change.defaultPermission = false;
            readOnlyPolicy.cls.edit.remove.defaultPermission = false;

            String[] changeEvents = {ServerResponse.CHANGE, ServerResponse.CHANGE_WYS, ServerResponse.GROUP_CHANGE, ServerResponse.EDIT_OBJECT};
            for (FormEntity formEntity : businessLogics.getAllForms()) {
                for(PropertyDrawEntity propertyDraw : formEntity.getPropertyDrawsIt()) {
                    for(String changeEvent : changeEvents) {
                        ActionPropertyObjectEntity<?> editAction = propertyDraw.getEditAction(changeEvent);
                        if (editAction != null && editAction.property.ignoreReadOnlyPolicy()) {
                            readOnlyPolicy.property.change.permit(editAction.property); // permits editAction if it doesn't change anything
                        } else {
                            if (changeEvent.equals(ServerResponse.CHANGE) && !propertyDraw.isCalcProperty()) { // hiding actions that cannot be executed 
                                propertyDraw.deny(readOnlyPolicy.property.view);
                            }
                        }
                    }
                }
                for(ImList<ActionPropertyObjectEntity<?>> eventActions : formEntity.getEventActions().valueIt()) {
                    for(ActionPropertyObjectEntity<?> eventAction : eventActions) {
                        if (eventAction != null && eventAction.property.ignoreReadOnlyPolicy()) {
                            readOnlyPolicy.property.change.permit(eventAction.property); // permits eventAction if it doesn't change anything
                        }
                    }
                }
            }

            allowConfiguratorPolicy = addPolicy(localize("{logics.policy.allow.configurator}"), localize("{logics.policy.logics.allow.configurator}"));
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

    public void putPolicy(Long policyID, SecurityPolicy policy) {
        policies.add(policyID, policy);
    }

    public SecurityPolicy getPolicy(Long policyID) {
        return policies.get(policyID);
    }

    public void setUserPolicies(long userId, SecurityPolicy... policies) {
        setUserPolicies(userId, Arrays.asList(policies));
    }

    public void setUserPolicies(long userId, List<SecurityPolicy> policies) {
        userPolicies.add(userId, policies);
    }

    public List<SecurityPolicy> getUserPolicies(long userId) {
        return userPolicies.get(userId);
    }

    public void setupDefaultAdminUser() throws SQLException, SQLHandledException {
        try(DataSession session = createSession()) {
            User user = addUser("admin", initialAdminPassword, session);
            applySecurityPolicy(user, session);
            setUserPolicies(user.ID, permitAllPolicy, allowConfiguratorPolicy);
            apply(session);
        }
    }

    private DataSession createSession() throws SQLException {
        return dbManager.createSession();
    }

    public SecurityPolicy addPolicy(String policyName, String description) throws SQLException, SQLHandledException {

        Long policyID;
        try (DataSession session = createSession()) {
            policyID = readPolicy(policyName, session);
            if (policyID == null) {
                DataObject addObject = session.addObject(securityLM.policy);
                securityLM.namePolicy.change(policyName, session, addObject);
                securityLM.descriptionPolicy.change(description, session, addObject);
                policyID = (Long) addObject.object;
                apply(session);
            }
        }

        SecurityPolicy policyObject = new SecurityPolicy(policyID);
        putPolicy(policyID, policyObject);
        return policyObject;
    }

    private Long readPolicy(String name, DataSession session) throws SQLException, SQLHandledException {
        return (Long) securityLM.policyName.read(session, new DataObject(name, StringClass.get(50)));
    }

    public String addUser(String username, String email, String password, String firstName, String lastName, String localeLanguage, ExecutionStack stack) throws RemoteException {
        try {
            //todo: в будущем нужно поменять на проставление локали в Context
//            ServerResourceBundle.load(localeLanguage);
            try(DataSession session = createSession()) {
                Object userId = authenticationLM.customUserLogin.read(session, new DataObject(username, StringClass.get(30)));
                if (userId != null) return localize("{logics.error.user.duplicate}");

                Object emailId = businessLogics.authenticationLM.contactEmail.read(session, new DataObject(email, StringClass.get(50)));
                if (emailId != null) {
                    return localize("{logics.error.emailContact.duplicate}");
                }

                DataObject userObject = session.addObject(authenticationLM.customUser);
                authenticationLM.loginCustomUser.change(username, session, userObject);
                businessLogics.authenticationLM.emailContact.change(email, session, userObject);
                authenticationLM.sha256PasswordCustomUser.change(BaseUtils.calculateBase64Hash("SHA-256", password, UserInfo.salt), session, userObject);
                businessLogics.authenticationLM.firstNameContact.change(firstName, session, userObject);
                businessLogics.authenticationLM.lastNameContact.change(lastName, session, userObject);
                apply(session);
            }
        } catch (SQLException e) {
            return localize("{logics.error.registration}");
        } catch (SQLHandledException e) {
            return localize("{logics.error.registration}");
        }
        return null;
    }

    protected User addUser(String login, String defaultPassword, DataSession session) throws SQLException, SQLHandledException {

        User user = readUser(login, session);
        if (user == null) {
            DataObject userObject = session.addObject(authenticationLM.customUser);
            authenticationLM.loginCustomUser.change(login, session, userObject);
            authenticationLM.sha256PasswordCustomUser.change(BaseUtils.calculateBase64Hash("SHA-256", defaultPassword.trim(), UserInfo.salt), session, userObject);
            Long userID = (Long) userObject.object;
            user = new User(userID);
        }

        return user;
    }

    public User readUser(String login, DataSession session) throws SQLException, SQLHandledException {
        return readUser(login, false, session);
    }

    public User readUser(String login, boolean insensitive, DataSession session) throws SQLException, SQLHandledException {
        Long userId = insensitive ? (Long) authenticationLM.customUserUpcaseLogin.read(session, new DataObject(login.toUpperCase(), StringClass.get(30))) :
                (Long) authenticationLM.customUserLogin.read(session, new DataObject(login, StringClass.get(30)));
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
        applyDefaultNavigatorElementDefinedPolicy(userObject, session);

        // политика для роли из формы "Политика безопасности"
        applyNavigatorElementDefinedUserPolicy(userObject, session);

        // дополнительные политики из формы "Политика безопасности"
        List<Long> userPoliciesIds = readUserPoliciesIds(userObject, session);
        for (long policyId : userPoliciesIds) {
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

    private List<Long> readUserPoliciesIds(User user, DataSession session) {
        try {
            ArrayList<Long> result = new ArrayList<>();

            QueryBuilder<String, Object> q = new QueryBuilder<>(SetFact.toExclSet("userId", "policyId"));
            Expr orderExpr = securityLM.orderUserPolicy.getExpr(session.getModifier(), q.getMapExprs().get("userId"), q.getMapExprs().get("policyId"));

            q.addProperty("pOrder", orderExpr);
            q.and(orderExpr.getWhere());
            q.and(q.getMapExprs().get("userId").compare(user.getDataObject(authenticationLM.customUser, session), Compare.EQUALS));

            ImOrderMap<Object, Boolean> orderBy = MapFact.<Object, Boolean>singletonOrder("pOrder", false);
            ImSet<ImMap<String, Object>> keys = q.execute(session, orderBy, 0).keys();
            if (keys.size() != 0) {
                for (ImMap<String, Object> keyMap : keys) {
                    result.add((Long) keyMap.get("policyId"));
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

    private void applyDefaultNavigatorElementDefinedPolicy(User user, DataSession session) {
        SecurityPolicy policy = new SecurityPolicy(-1);
        try {
            QueryBuilder<String, String> qne = new QueryBuilder<>(SetFact.singleton("neId"));
            Expr nameExpr = reflectionLM.canonicalNameNavigatorElement.getExpr(session.getModifier(), qne.getMapExprs().get("neId"));
            Expr permitNEExpr = securityLM.permitNavigatorElement.getExpr(session.getModifier(), qne.getMapExprs().get("neId"));
            Expr forbidNEExpr = securityLM.forbidNavigatorElement.getExpr(session.getModifier(), qne.getMapExprs().get("neId"));

            qne.and(nameExpr.getWhere());
            qne.and(permitNEExpr.getWhere().or(forbidNEExpr.getWhere()));

            qne.addProperty("canonicalName", nameExpr);
            qne.addProperty("permit", permitNEExpr);
            qne.addProperty("forbid", forbidNEExpr);

            applyNavigatorElementPolicy(qne.execute(session).values(), policy);
            user.addSecurityPolicy(policy);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void applyNavigatorElementPolicy(ImCol<ImMap<String, Object>> neQueryValues, SecurityPolicy policy) {
        Map<String, ImMap<String, Object>> neMap = new HashMap<>();
        for (ImMap<String, Object> valueMap : neQueryValues) {
            neMap.put(((String) valueMap.get("canonicalName")).trim(), valueMap);
        }

        for (NavigatorElement ne : businessLogics.LM.root.getOrderedChildrenList()) {
            String canonicalName = ne.getCanonicalName();
            if (neMap.containsKey(canonicalName)) {
                ImMap<String, Object> valueMap = neMap.get(canonicalName);
                NavigatorElement element = businessLogics.findNavigatorElement(canonicalName);
                if (valueMap.get("forbid") != null) {
                    policy.navigator.deny(element);
                } else if (valueMap.get("permit") != null) {
                    policy.navigator.permit(element);
                }
            }
        }
    }
    
    private void applyNavigatorElementDefinedUserPolicy(User user, DataSession session) {
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

            qu.addProperty("forbidViewAllSetupPolicies", securityLM.forbidViewAllSetupPolicies.getExpr(session.getModifier(), userExpr));
            qu.addProperty("forbidChangeAllSetupPolicies", securityLM.forbidChangeAllSetupPolicies.getExpr(session.getModifier(), userExpr));
            
            qu.addProperty("cachePropertyPolicy", securityLM.cachePropertyPolicyUser.getExpr(session.getModifier(), userExpr));

            boolean cachePropertyPolicy = false;
            boolean forbidViewAllSetupPolicies = false;
            boolean forbidChangeAllSetupPolicies = false;
            
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
                
                cachePropertyPolicy = valueMap.get("cachePropertyPolicy") != null;
                forbidViewAllSetupPolicies = valueMap.get("forbidViewAllSetupPolicies") != null;
                forbidChangeAllSetupPolicies = valueMap.get("forbidChangeAllSetupPolicies") != null;
            }

            QueryBuilder<String, String> qne = new QueryBuilder<>(SetFact.toExclSet("userId", "neId"));
            Expr nameExpr = reflectionLM.canonicalNameNavigatorElement.getExpr(session.getModifier(), qne.getMapExprs().get("neId"));
            Expr permitUserNeExpr = securityLM.overPermitUserNavigatorElement.getExpr(session.getModifier(), qne.getMapExprs().get("userId"), qne.getMapExprs().get("neId"));
            Expr forbidUserNeExpr = securityLM.overForbidUserNavigatorElement.getExpr(session.getModifier(), qne.getMapExprs().get("userId"), qne.getMapExprs().get("neId"));

            qne.and(nameExpr.getWhere());
            qne.and(qne.getMapExprs().get("userId").compare(userObject, Compare.EQUALS));
            qne.and(permitUserNeExpr.getWhere().or(forbidUserNeExpr.getWhere()));

            qne.addProperty("canonicalName", nameExpr);
            qne.addProperty("permit", permitUserNeExpr);
            qne.addProperty("forbid", forbidUserNeExpr);

            applyNavigatorElementPolicy(qne.execute(session).values(), policy);

            ImCol<ImMap<String, Object>> propValues = readPropertyPolicy(null, session, userObject, cachePropertyPolicy, false);
            for (ImMap<String, Object> valueMap : propValues) {
                String cn = ((String) valueMap.get("cn")).trim();
                try {
                    LP<?, ?> prop = businessLogics.findPropertyElseAction(cn);
                    if (prop != null) {
                        if (valueMap.get("fullForbidView") != null)
                            policy.property.view.deny(prop);
                        if (valueMap.get("fullForbidChange") != null)
                            policy.property.change.deny(prop);
                    } else {
                       startLogger.debug(String.format("Property '%s' is not found when applying security policy", cn));
                    }
                } catch (CanonicalNameUtils.ParseException e) {
                    startLogger.debug(String.format("Canonical name parsing error: '%s' when applying security policy", e.getMessage()));
                }
            }

            if(forbidViewAllSetupPolicies || forbidChangeAllSetupPolicies) {
                for (Property prop : LM.propertyPolicyGroup.getIndexedPropChildren().keyIt()) {
                    if(forbidViewAllSetupPolicies)
                        policy.property.view.deny(prop);
                    if(forbidChangeAllSetupPolicies)
                        policy.property.change.deny(prop);
                }
            }

            user.addSecurityPolicy(policy);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private LRUSVSMap<Long, ImCol<ImMap<String, Object>>> propertyPolicyCache = new LRUSVSMap<>(LRUUtil.G2);

    private ImCol<ImMap<String, Object>> readPropertyPolicy(ExecutionContext context, DataSession session, DataObject userObject, boolean cache, boolean reupdateCache) throws SQLException, SQLHandledException {

        ImCol<ImMap<String, Object>> result = null;
        if(cache && !reupdateCache) {
            result = propertyPolicyCache.get((long) userObject.object);
            if (result != null)
                return result;
        }

        Modifier modifier = context != null ? context.getModifier() : session.getModifier();
        
        Expr userExpr;QueryBuilder<String, String> qp = new QueryBuilder<>(SetFact.toExclSet("userId", "propertyCN"));
        Expr actionOrPropertyExpr = qp.getMapExprs().get("propertyCN");
        userExpr = qp.getMapExprs().get("userId");
        Expr propExpr = reflectionLM.canonicalNameActionOrProperty.getExpr(modifier, actionOrPropertyExpr);
        qp.and(propExpr.getWhere());
        qp.and(userExpr.compare(userObject, Compare.EQUALS));
        qp.and(securityLM.fullForbidViewUserProperty.getExpr(modifier, userExpr, actionOrPropertyExpr).getWhere().or(
                securityLM.fullForbidChangeUserProperty.getExpr(modifier, userExpr, actionOrPropertyExpr).getWhere()));

        qp.addProperty("cn", propExpr);
        qp.addProperty("fullForbidView", securityLM.fullForbidViewUserProperty.getExpr(modifier, userExpr, actionOrPropertyExpr));
        qp.addProperty("fullForbidChange", securityLM.fullForbidChangeUserProperty.getExpr(modifier, userExpr, actionOrPropertyExpr));

        ImOrderMap<ImMap<String, Object>, ImMap<String, Object>> queryResult = context != null ? qp.execute(context) : qp.execute(session);
        result = queryResult.values();
        if(cache)
            propertyPolicyCache.put((long)userObject.object, result);
        return result;
    }
    
    public void updatePropertyPolicyCaches(ExecutionContext context, DataObject userObject) throws SQLException, SQLHandledException {
        readPropertyPolicy(context, null, userObject, true, true);
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
            throw new RuntimeException(localize("{logics.info.error.reading.list.of.roles}"), e);
        } catch (SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    public void setUserParameters(User user, String firstName, String lastName, String email, List<String> userRoleSIDs, DataSession session) {
        try {

            DataObject customUser = user.getDataObject(authenticationLM.customUser, session);

            if (firstName != null)
                authenticationLM.firstNameContact.change(firstName, session, (DataObject) customUser);

            if (lastName != null)
                authenticationLM.lastNameContact.change(lastName, session, (DataObject) customUser);
            
            if (email != null)
                authenticationLM.emailContact.change(email, session, (DataObject) customUser);

            if (userRoleSIDs != null) {
                for (String userRoleName : userRoleSIDs) {
                    ObjectValue userRole = securityLM.userRoleSID.readClasses(session, new DataObject(userRoleName));

                    if (! (userRole instanceof NullValue)) {
                        securityLM.mainRoleCustomUser.change(userRole, session, customUser);
                        break;
                    }
                }
            }
        } catch (SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }    
}

