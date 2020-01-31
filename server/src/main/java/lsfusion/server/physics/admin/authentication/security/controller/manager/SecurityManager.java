package lsfusion.server.physics.admin.authentication.security.controller.manager;

import com.google.common.base.Throwables;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.add.MAddMap;
import lsfusion.base.col.lru.LRUSVSMap;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.interop.base.exception.AuthenticationException;
import lsfusion.interop.base.exception.LockedException;
import lsfusion.interop.base.exception.LoginException;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.base.controller.lifecycle.LifecycleEvent;
import lsfusion.server.base.controller.manager.LogicsManager;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.language.property.oraction.LAP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.authentication.AuthenticationLogicsModule;
import lsfusion.server.physics.admin.authentication.LDAPAuthenticationService;
import lsfusion.server.physics.admin.authentication.LDAPParameters;
import lsfusion.server.physics.admin.authentication.UserInfo;
import lsfusion.server.physics.admin.authentication.security.SecurityLogicsModule;
import lsfusion.server.physics.admin.authentication.security.policy.SecurityPolicy;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.admin.reflection.ReflectionLogicsModule;
import lsfusion.server.physics.dev.id.name.CanonicalNameUtils;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import javax.naming.CommunicationException;
import java.sql.SQLException;
import java.util.*;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public class SecurityManager extends LogicsManager implements InitializingBean {
    private static final Logger startLogger = ServerLoggers.startLogger;
    private static final Logger systemLogger = ServerLoggers.systemLogger;

    @Deprecated
    public static SecurityPolicy serverSecurityPolicy = new SecurityPolicy();

    private final MAddMap<Long, SecurityPolicy> policies = MapFact.mAddOverrideMap();

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
    public void afterPropertiesSet() {
        Assert.notNull(businessLogics, "businessLogics must be specified");
        Assert.notNull(dbManager, "dbManager must be specified");
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

            permitAllPolicy = addPolicy("allowAll", localize("{logics.policy.allow.all}"), localize("{logics.policy.allows.all.actions}"));
            permitAllPolicy.setReplaceMode(true);

            readOnlyPolicy = addPolicy("readonly", localize("{logics.policy.forbid.editing.all.properties}"), localize("{logics.policy.read.only.forbids.editing.of.all.properties.on.the.forms}"));
            readOnlyPolicy.property.change.defaultPermission = false;
            readOnlyPolicy.cls.edit.add.defaultPermission = false;
            readOnlyPolicy.cls.edit.change.defaultPermission = false;
            readOnlyPolicy.cls.edit.remove.defaultPermission = false;

            for (FormEntity formEntity : businessLogics.getAllForms())
                formEntity.proceedAllEventActions((eventAction, drawAction) -> {
                    if (eventAction.property.ignoreReadOnlyPolicy()) {
                        readOnlyPolicy.property.change.permit(eventAction.property); // permits eventAction if it doesn't change anything
                    } else {
                        if (drawAction != null) { // hiding actions that cannot be executed 
                            drawAction.deny(readOnlyPolicy.property.view);
                        }
                    }                    
                });

            allowConfiguratorPolicy = addPolicy("allowConfiguration", localize("{logics.policy.allow.configurator}"), localize("{logics.policy.logics.allow.configurator}"));
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

    private DataObject adminUser = null;
    private DataObject anonymousUser = null;
    public void initUsers() throws SQLException, SQLHandledException {
        try(DataSession session = createSession()) {
            DataObject adminUser = readUser("admin", session);
            if (adminUser == null) {
                adminUser = addUser("admin", initialAdminPassword, session);
                apply(session);
            }
            this.adminUser = new DataObject((Long) adminUser.object, authenticationLM.customUser); // to update classes after apply

            DataObject anonymousUser = readUser("anonymous", session);
            if (anonymousUser == null) {
                anonymousUser = addUser("anonymous", initialAdminPassword, session);
                apply(session);
            }
            this.anonymousUser = new DataObject((Long) anonymousUser.object, authenticationLM.customUser); // to update classes after apply
        }
    }

    public DataObject getAdminUser() {
        return adminUser;
    }

    public DataObject getAnonymousUser() {
        return anonymousUser;
    }

    private DataSession createSession() throws SQLException {
        return dbManager.createSession();
    }

    public SecurityPolicy addPolicy(String id, String name, String description) throws SQLException, SQLHandledException {

        Long policyID;
        try (DataSession session = createSession()) {
            policyID = readPolicy(id, session);
            if (policyID == null) {
                DataObject addObject = session.addObject(securityLM.policy);
                securityLM.idPolicy.change(id, session, addObject);
                securityLM.namePolicy.change(name, session, addObject);
                securityLM.descriptionPolicy.change(description, session, addObject);
                policyID = (Long) addObject.object;
                apply(session);
            }
        }

        SecurityPolicy policyObject = new SecurityPolicy(policyID);
        putPolicy(policyID, policyObject);
        return policyObject;
    }

    private Long readPolicy(String id, DataSession session) throws SQLException, SQLHandledException {
        return (Long) securityLM.policyId.read(session, new DataObject(id, StringClass.get(100)));
    }

    protected DataObject addUser(String login, String defaultPassword, DataSession session) throws SQLException, SQLHandledException {
        DataObject userObject = session.addObject(authenticationLM.customUser);
        authenticationLM.loginCustomUser.change(login, session, userObject);
        authenticationLM.sha256PasswordCustomUser.change(BaseUtils.calculateBase64Hash("SHA-256", defaultPassword.trim(), UserInfo.salt), session, userObject);
        return userObject;
    }

    public DataObject readUser(String login, DataSession session) throws SQLException, SQLHandledException {
        ObjectValue userObject = authenticationLM.useLDAP.read(session) != null ? authenticationLM.customUserUpcaseLogin.readClasses(session, new DataObject(login.toUpperCase(), StringClass.get(100))) :
                authenticationLM.customUserLogin.readClasses(session, new DataObject(login, StringClass.get(100)));
        return userObject.isNull() ? null : (DataObject) userObject;
    }

    private String secret = null;
    public void initSecret() throws SQLException, SQLHandledException {
        try(DataSession session = createSession()) {
            LP secretLCP = authenticationLM.secret;
            String secretKey = (String) secretLCP.read(session);
            if(secretKey == null) {
                secretKey = BaseUtils.generatePassword(32, false, false);
                secretLCP.change(secretKey, session);
                apply(session);
            }
            this.secret = secretKey;
        }
    }

    public String parseToken(AuthenticationToken token) {
        if(token.isAnonymous())
            return null;

        Claims body;
        try {
            body = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token.string)
                    .getBody();
        } catch (Exception e) {
            throw new AuthenticationException(e.getMessage());
        }

        return body.getSubject();
//        u.setId(Long.parseLong((String) body.get("userId")));
    }

    public AuthenticationToken generateToken(String userLogin) {
        Claims claims = Jwts.claims().setSubject(userLogin);
        claims.setExpiration(new Date(System.currentTimeMillis() + Settings.get().getAuthTokenExpiration() * 1000 * 60)); // expiration * 1000*60*60
//        claims.put("userId", u.getId() + "");

        return new AuthenticationToken(Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact());
    }
    
    public AuthenticationToken authenticateUser(String userName, String password, ExecutionStack stack) {
        try(DataSession session = createSession()) {
            DataObject userObject = readUser(userName, session);
            authenticateUser(session, userObject, userName, password, stack);
            return generateToken(userName);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public SecurityPolicy readSecurityPolicy(DataSession session, DataObject userObject) {
        // политика по умолчанию из кода
        List<SecurityPolicy> securityPolicies = new ArrayList<>();
        
        securityPolicies.add(defaultPolicy);

        // политика по умолчанию из формы "Политика безопасности"
        applyDefaultNavigatorElementDefinedPolicy(securityPolicies, session);

        // политика для роли из формы "Политика безопасности"
        applyNavigatorElementDefinedUserPolicy(securityPolicies, userObject, session);

        // дополнительные политики из формы "Политика безопасности"
        List<Long> userPoliciesIds = readUserPoliciesIds(userObject, session);
        for (long policyId : userPoliciesIds) {
            SecurityPolicy policy = getPolicy(policyId);
            if (policy != null) {
                securityPolicies.add(policy);
            }
        }

        if(userObject.equals(adminUser)) {
            securityPolicies.add(permitAllPolicy);
            securityPolicies.add(allowConfiguratorPolicy);
        }

        SecurityPolicy resultPolicy = new SecurityPolicy(-1);
        for (SecurityPolicy policy : securityPolicies) {
            resultPolicy.override(policy);
        }
        return resultPolicy;
    }

    private List<Long> readUserPoliciesIds(DataObject userObject, DataSession session) {
        try {
            ArrayList<Long> result = new ArrayList<>();

            QueryBuilder<String, Object> q = new QueryBuilder<>(SetFact.toExclSet("userId", "policyId"));
            Expr orderExpr = securityLM.orderUserPolicy.getExpr(session.getModifier(), q.getMapExprs().get("userId"), q.getMapExprs().get("policyId"));

            q.addProperty("pOrder", orderExpr);
            q.and(orderExpr.getWhere());
            q.and(q.getMapExprs().get("userId").compare(userObject, Compare.EQUALS));

            ImOrderMap<Object, Boolean> orderBy = MapFact.singletonOrder("pOrder", false);
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

    public void authenticateUser(DataSession session, DataObject userObject, String login, String password, ExecutionStack stack) throws SQLException, SQLHandledException {
        if (authenticationLM.useLDAP.read(session) != null) {
            String server = (String) authenticationLM.serverLDAP.read(session);
            Integer port = (Integer) authenticationLM.portLDAP.read(session);
            String baseDN = (String) authenticationLM.baseDNLDAP.read(session);
            String userDNSuffix = (String) authenticationLM.userDNSuffixLDAP.read(session);

            try {
                LDAPParameters ldapParameters = new LDAPAuthenticationService(server, port, baseDN, userDNSuffix).authenticate(login, password);
                if (ldapParameters.isConnected()) {
                    if (userObject == null) {
                        userObject = addUser(login, password, session);
                    }
                    setUserParameters(userObject, ldapParameters.getFirstName(), ldapParameters.getLastName(), ldapParameters.getEmail(), ldapParameters.getGroupNames(), session);
                    apply(session);
                    return;
                } else {
                    throw new LoginException();
                }
            } catch (CommunicationException e) {
                systemLogger.error("LDAP authentication failed", e);
            }
        }

        if (userObject == null) {
            throw new LoginException();
        }

        if (authenticationLM.isLockedCustomUser.read(session, userObject) != null) {
            throw new LockedException();
        }

        if (!authenticationLM.checkPassword(session, userObject, password, stack))
            throw new LoginException();
    }

    private void applyDefaultNavigatorElementDefinedPolicy(List<SecurityPolicy> securityPolicies, DataSession session) {
        SecurityPolicy policy = new SecurityPolicy(-1);
        try {
            QueryBuilder<String, String> qne = new QueryBuilder<>(SetFact.singleton("neId"));
            Expr nameExpr = reflectionLM.canonicalNameNavigatorElement.getExpr(session.getModifier(), qne.getMapExprs().get("neId"));
            Expr permissionNEExpr = securityLM.permissionNavigatorElement.getExpr(session.getModifier(), qne.getMapExprs().get("neId"));

            qne.and(nameExpr.getWhere());
            qne.and(permissionNEExpr.getWhere());

            qne.addProperty("canonicalName", nameExpr);
            qne.addProperty("permission", permissionNEExpr);

            applyNavigatorElementPolicy(qne.execute(session).values(), policy);
            securityPolicies.add(policy);
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
                Boolean permission = getPermissionValue(valueMap.get("permission"));
                if(permission != null) {
                    if(permission)
                        policy.navigator.permit(element);
                    else
                        policy.navigator.deny(element);
                }
            }
        }
    }
    
    private void applyNavigatorElementDefinedUserPolicy(List<SecurityPolicy> securityPolicies, DataObject userObject, DataSession session) {
        SecurityPolicy policy = new SecurityPolicy(-1);
        try {
            QueryBuilder<String, String> qu = new QueryBuilder<>(SetFact.toExclSet("userId"));
            Expr userExpr = qu.getMapExprs().get("userId");
            qu.and(userExpr.compare(userObject, Compare.EQUALS));
            qu.addProperty("permissionAllForms", securityLM.permissionAllFormsUser.getExpr(session.getModifier(), userExpr));
            qu.addProperty("permissionViewAllProperty", securityLM.permissionViewAllPropertyUser.getExpr(session.getModifier(), userExpr));
            qu.addProperty("permissionChangeAllProperty", securityLM.permissionChangeAllPropertyUser.getExpr(session.getModifier(), userExpr));

            qu.addProperty("forbidViewAllSetupPolicies", securityLM.forbidViewAllSetupPolicies.getExpr(session.getModifier(), userExpr));
            qu.addProperty("forbidChangeAllSetupPolicies", securityLM.forbidChangeAllSetupPolicies.getExpr(session.getModifier(), userExpr));
            qu.addProperty("forbidEditObjects", securityLM.forbidEditObjects.getExpr(session.getModifier(), userExpr));

            qu.addProperty("cachePropertyPolicy", securityLM.cachePropertyPolicyUser.getExpr(session.getModifier(), userExpr));

            boolean cachePropertyPolicy = false;
            boolean forbidViewAllSetupPolicies = false;
            boolean forbidChangeAllSetupPolicies = false;
            boolean forbidEditObjects = false;

            ImCol<ImMap<String, Object>> userPermissionValues = qu.execute(session).values();
            for (ImMap<String, Object> valueMap : userPermissionValues) {
                Boolean permission = getPermissionValue(valueMap.get("permissionAllForms"));
                if(permission != null) {
                    policy.navigator.defaultPermission = permission;
                }

                Boolean permissionViewAllProperty = getPermissionValue(valueMap.get("permissionViewAllProperty"));
                if(permissionViewAllProperty != null) {
                    policy.property.view.defaultPermission = permissionViewAllProperty;
                }

                Boolean permissionChangeAllProperty = getPermissionValue(valueMap.get("permissionChangeAllProperty"));
                if(permissionChangeAllProperty != null) {
                    policy.property.change.defaultPermission = permissionChangeAllProperty;
                }
                
                cachePropertyPolicy = valueMap.get("cachePropertyPolicy") != null;
                forbidViewAllSetupPolicies = valueMap.get("forbidViewAllSetupPolicies") != null;
                forbidChangeAllSetupPolicies = valueMap.get("forbidChangeAllSetupPolicies") != null;
                forbidEditObjects = valueMap.get("forbidEditObjects") != null;
            }

            QueryBuilder<String, String> qne = new QueryBuilder<>(SetFact.toExclSet("userId", "neId"));
            Expr nameExpr = reflectionLM.canonicalNameNavigatorElement.getExpr(session.getModifier(), qne.getMapExprs().get("neId"));
            Expr permissionUserNeExpr = securityLM.permissionUserNavigatorElement.getExpr(session.getModifier(), qne.getMapExprs().get("userId"), qne.getMapExprs().get("neId"));

            qne.and(nameExpr.getWhere());
            qne.and(qne.getMapExprs().get("userId").compare(userObject, Compare.EQUALS));
            qne.and(permissionUserNeExpr.getWhere());

            qne.addProperty("canonicalName", nameExpr);
            qne.addProperty("permission", permissionUserNeExpr);

            applyNavigatorElementPolicy(qne.execute(session).values(), policy);

            ImCol<ImMap<String, Object>> propValues = readPropertyPolicy(null, session, userObject, cachePropertyPolicy, false);
            for (ImMap<String, Object> valueMap : propValues) {
                String cn = ((String) valueMap.get("cn")).trim();
                try {
                    LAP<?, ?> prop = businessLogics.findPropertyElseAction(cn);
                    if (prop != null) {
                        if (valueMap.get("forbidView") != null)
                            policy.property.view.deny(prop);
                        if (valueMap.get("forbidChange") != null)
                            policy.property.change.deny(prop);
                    } else {
                       startLogger.debug(String.format("Property '%s' is not found when applying security policy", cn));
                    }
                } catch (CanonicalNameUtils.ParseException e) {
                    startLogger.debug(String.format("Canonical name parsing error: '%s' when applying security policy", e.getMessage()));
                }
            }

            policy.editObjects = !forbidEditObjects;
            if(forbidViewAllSetupPolicies || forbidChangeAllSetupPolicies) {
                for (ActionOrProperty prop : LM.propertyPolicyGroup.getIndexedPropChildren().keyIt()) {
                    if(forbidViewAllSetupPolicies)
                        policy.property.view.deny(prop);
                    if(forbidChangeAllSetupPolicies)
                        policy.property.change.deny(prop);
                }
            }

            securityPolicies.add(policy);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    Boolean getPermissionValue(Object permission) {
        return permission != null ? permission.equals("Security_Permission.permit") : null;
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
        qp.and(securityLM.forbidViewUserProperty.getExpr(modifier, userExpr, actionOrPropertyExpr).getWhere().or(
                securityLM.forbidChangeUserProperty.getExpr(modifier, userExpr, actionOrPropertyExpr).getWhere()));

        qp.addProperty("cn", propExpr);
        qp.addProperty("forbidView", securityLM.forbidViewUserProperty.getExpr(modifier, userExpr, actionOrPropertyExpr));
        qp.addProperty("forbidChange", securityLM.forbidChangeUserProperty.getExpr(modifier, userExpr, actionOrPropertyExpr));

        ImOrderMap<ImMap<String, Object>, ImMap<String, Object>> queryResult = context != null ? qp.execute(context) : qp.execute(session);
        result = queryResult.values();
        if(cache)
            propertyPolicyCache.put((long)userObject.object, result);
        return result;
    }
    
    public void updatePropertyPolicyCaches(ExecutionContext context, DataObject userObject) throws SQLException, SQLHandledException {
        readPropertyPolicy(context, null, userObject, true, true);
    }

    public void setUserParameters(DataObject customUser, String firstName, String lastName, String email, List<String> userRoleSIDs, DataSession session) {
        try {
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

