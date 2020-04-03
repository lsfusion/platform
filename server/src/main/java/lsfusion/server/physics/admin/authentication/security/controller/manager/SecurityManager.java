package lsfusion.server.physics.admin.authentication.security.controller.manager;

import com.google.common.base.Throwables;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
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
import lsfusion.server.data.expr.key.KeyExpr;
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
import lsfusion.server.physics.admin.authentication.security.policy.AddSecurityPolicy;
import lsfusion.server.physics.admin.authentication.security.policy.BaseSecurityPolicy;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.admin.reflection.ReflectionLogicsModule;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import javax.naming.CommunicationException;
import java.sql.SQLException;
import java.util.*;

public class SecurityManager extends LogicsManager implements InitializingBean {
    private static final Logger startLogger = ServerLoggers.startLogger;
    private static final Logger systemLogger = ServerLoggers.systemLogger;

    @Deprecated
    public static BaseSecurityPolicy baseServerSecurityPolicy = new BaseSecurityPolicy();

    public Map<String, AddSecurityPolicy> cachedSecurityPolicies = new HashMap<>();

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

        for (FormEntity formEntity : businessLogics.getAllForms())
            formEntity.proceedAllEventActions((eventAction, drawAction) -> {
            });
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

    private DataObject adminUser = null;
    private DataObject anonymousUser = null;
    public void initUsers() throws SQLException, SQLHandledException {
        try(DataSession session = createSession()) {
            securityLM.createSystemUserRoles.execute(session, getStack());
            apply(session);
        }

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

    public BaseSecurityPolicy getSecurityPolicy(DataSession session, DataObject userObject) {
        List<AddSecurityPolicy> policies = new ArrayList<>();
        try {
            Map<String, DataObject> userRolesMap = readUserRolesMap(session, userObject);
            for (Map.Entry<String, DataObject> userRoleEntry : userRolesMap.entrySet()) {
                AddSecurityPolicy policy = cachedSecurityPolicies.get(userRoleEntry.getKey());
                if (policy == null) {
                    policy = readSecurityPolicy(userRoleEntry.getValue(), session);
                    cachedSecurityPolicies.put(userRoleEntry.getKey(), policy);
                }
                policies.add(policy);
            }
            return new BaseSecurityPolicy(policies);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private AddSecurityPolicy readSecurityPolicy(DataObject userRoleObject, DataSession session) throws SQLException, SQLHandledException {
        AddSecurityPolicy policy = new AddSecurityPolicy();

        KeyExpr navigatorElementExpr = new KeyExpr("navigatorElement");
        QueryBuilder<Object, Object> query = new QueryBuilder<>(MapFact.singletonRev("navigatorElement", navigatorElementExpr));
        query.addProperty("canonicalName", reflectionLM.canonicalNameNavigatorElement.getExpr(session.getModifier(), navigatorElementExpr));
        query.addProperty("permission", securityLM.permissionUserRoleNavigatorElement.getExpr(session.getModifier(), userRoleObject.getExpr(), navigatorElementExpr));
        query.and(reflectionLM.canonicalNameNavigatorElement.getExpr(session.getModifier(), navigatorElementExpr).getWhere());
        query.and(securityLM.permissionUserRoleNavigatorElement.getExpr(session.getModifier(), userRoleObject.getExpr(), navigatorElementExpr).getWhere());

        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> queryResult = query.execute(session);
        for (ImMap<Object, Object> entry : queryResult.values()) {
            String canonicalName = (String) entry.get("canonicalName");
            NavigatorElement element = businessLogics.findNavigatorElement(canonicalName);
            if(element != null) {
                policy.navigator.setPermission(element, getPermissionValue(entry.get("permission")));
            } else {
                startLogger.debug(String.format("NavigatorElement '%s' is not found when applying security policy", canonicalName));
            }
        }

        policy.forbidEditObjects = securityLM.forbidEditObjects.read(session, userRoleObject) != null;

        boolean forbidViewAllSetupPolicies = securityLM.forbidViewAllSetupPolicies.read(session, userRoleObject) != null;
        boolean forbidChangeAllSetupPolicies = securityLM.forbidChangeAllSetupPolicies.read(session, userRoleObject) != null;
        if(forbidViewAllSetupPolicies || forbidChangeAllSetupPolicies) {
            for (ActionOrProperty prop : LM.propertyPolicyGroup.getIndexedPropChildren().keyIt()) {
                if(forbidViewAllSetupPolicies)
                    policy.propertyView.setPermission(prop, false);
                if(forbidChangeAllSetupPolicies)
                    policy.propertyChange.setPermission(prop, false);
            }
        }

        KeyExpr actionOrPropertyExpr = new KeyExpr("actionOrProperty");
        query = new QueryBuilder<>(MapFact.singletonRev("actionOrProperty", actionOrPropertyExpr));
        query.addProperty("canonicalName", reflectionLM.canonicalNameActionOrProperty.getExpr(session.getModifier(), actionOrPropertyExpr));
        query.addProperty("permissionView", securityLM.permissionViewUserRoleActionOrProperty.getExpr(session.getModifier(), userRoleObject.getExpr(), actionOrPropertyExpr));
        query.addProperty("permissionChange", securityLM.permissionChangeUserRoleActionOrProperty.getExpr(session.getModifier(), userRoleObject.getExpr(), actionOrPropertyExpr));

        query.and(reflectionLM.canonicalNameActionOrProperty.getExpr(session.getModifier(), actionOrPropertyExpr).getWhere());
        query.and(securityLM.permissionViewUserRoleActionOrProperty.getExpr(session.getModifier(), userRoleObject.getExpr(), actionOrPropertyExpr).getWhere().or(
                securityLM.permissionChangeUserRoleActionOrProperty.getExpr(session.getModifier(), userRoleObject.getExpr(), actionOrPropertyExpr).getWhere()
        ));

        queryResult = query.execute(session);
        for (ImMap<Object, Object> entry : queryResult.values()) {

            String canonicalName = (String) entry.get("canonicalName");
            try {
                LAP<?, ?> property = businessLogics.findPropertyElseAction(canonicalName);

                if (property != null) {
                    ActionOrProperty<?> actionOrProperty = property.getActionOrProperty();
                    policy.propertyView.setPermission(actionOrProperty, getPermissionValue(entry.get("permissionView")));
                    policy.propertyChange.setPermission(actionOrProperty, getPermissionValue(entry.get("permissionChange")));
                } else {
                    startLogger.debug(String.format("Property '%s' is not found when applying security policy", canonicalName));
                }
            } catch (Exception e) {}

        }

        return policy;
    }

    private Map<String, DataObject> readUserRolesMap(DataSession session, DataObject userObject) throws SQLException, SQLHandledException {
        Map<String, DataObject> userRolesMap = new HashMap<>();

        KeyExpr userRoleExpr = new KeyExpr("userRole");
        QueryBuilder<Object, Object> userRoleQuery = new QueryBuilder<>(MapFact.singletonRev("userRole", userRoleExpr));
        userRoleQuery.addProperty("sidUserRole", securityLM.sidUserRole.getExpr(session.getModifier(), userRoleExpr));
        userRoleQuery.and(securityLM.hasUserRole.getExpr(session.getModifier(), userObject.getExpr(), userRoleExpr).getWhere());

        ImOrderMap<ImMap<Object, DataObject>, ImMap<Object, ObjectValue>> queryResult = userRoleQuery.executeClasses(session);

        for (int i = 0, size = queryResult.size(); i < size; i++) {
            ImMap<Object, DataObject> resultKeys = queryResult.getKey(i);
            ImMap<Object, ObjectValue> resultValues = queryResult.getValue(i);

            userRolesMap.put((String) resultValues.get("sidUserRole").getValue(), resultKeys.get("userRole"));
        }
        return userRolesMap;
    }

    public Boolean getPermissionValue(Object permission) {
        return permission != null ? permission.equals("Security_Permission.permit") : null;
    }

    private LRUSVSMap<Long, ImCol<ImMap<String, Object>>> propertyPolicyCache = new LRUSVSMap<>(LRUUtil.G2);

    private ImCol<ImMap<String, Object>> readPropertyPolicy(ExecutionContext context, DataSession session, DataObject userObject, boolean cache, boolean reupdateCache) throws SQLException, SQLHandledException {

        ImCol<ImMap<String, Object>> result;
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
                authenticationLM.firstNameContact.change(firstName, session, customUser);

            if (lastName != null)
                authenticationLM.lastNameContact.change(lastName, session, customUser);
            
            if (email != null)
                authenticationLM.emailContact.change(email, session, customUser);

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

