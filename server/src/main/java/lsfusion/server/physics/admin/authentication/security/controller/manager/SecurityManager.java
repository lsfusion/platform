package lsfusion.server.physics.admin.authentication.security.controller.manager;

import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.interop.base.exception.AuthenticationException;
import lsfusion.interop.base.exception.LockedException;
import lsfusion.interop.base.exception.LoginException;
import lsfusion.interop.connection.authentication.*;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.server.base.controller.lifecycle.LifecycleEvent;
import lsfusion.server.base.controller.manager.LogicsManager;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.language.property.oraction.LAP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.SystemProperties;
import lsfusion.server.physics.admin.authentication.AuthenticationLogicsModule;
import lsfusion.server.physics.admin.authentication.LDAPAuthenticationService;
import lsfusion.server.physics.admin.authentication.LDAPParameters;
import lsfusion.server.physics.admin.authentication.UserInfo;
import lsfusion.server.physics.admin.authentication.security.SecurityLogicsModule;
import lsfusion.server.physics.admin.authentication.security.policy.RoleSecurityPolicy;
import lsfusion.server.physics.admin.authentication.security.policy.SecurityPolicy;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.admin.reflection.ReflectionLogicsModule;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.naming.CommunicationException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static lsfusion.base.ApiResourceBundle.getString;
import static lsfusion.base.BaseUtils.trim;
import static lsfusion.server.physics.admin.log.ServerLoggers.*;

public class SecurityManager extends LogicsManager implements InitializingBean {
    private static final Logger systemLogger = ServerLoggers.systemLogger;

    @Deprecated
    public static SecurityPolicy baseServerSecurityPolicy = new SecurityPolicy();

    private final HashMap<Long, Object> cachedSecuritySemaphores = new HashMap<>();
    private synchronized Object getCachedSecuritySemaphore(Long user) {
        return cachedSecuritySemaphores.computeIfAbsent(user, k -> new Object());
    }
    public ConcurrentHashMap<Long, RoleSecurityPolicy> cachedSecurityPolicies = new ConcurrentHashMap<>();

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
        startLog("Initializing security manager");
        this.LM = businessLogics.LM;
        this.authenticationLM = businessLogics.authenticationLM;
        this.securityLM = businessLogics.securityLM;
        this.reflectionLM = businessLogics.reflectionLM;

//        not sure what it was for
//        for (FormEntity formEntity : businessLogics.getAllForms())
//            formEntity.proceedAllEventActions((eventAction, drawAction) -> {
//            });
    }

    @Override
    protected void onStarted(LifecycleEvent event) {
        startLog("Starting security manager");
        try {
            businessLogics.initAuthentication(this);
        } catch (SQLException | SQLHandledException e) {
            throw new RuntimeException("Error starting security manager: ", e);
        }
    }

    private DataObject adminUserRole = null;
    private DataObject readOnlyUserRole = null;
    private DataObject adminUser = null;
    private DataObject anonymousUser = null;
    public void initUsers() throws SQLException, SQLHandledException {
        try(DataSession session = createSession()) {
            securityLM.createSystemUserRoles.execute(session, getStack());
            apply(session);
        }

        try(DataSession session = createSession()) {

            //created in createSystemUserRoles
            String admin = "admin";
            this.adminUserRole = (DataObject) securityLM.userRoleSID.readClasses(session, new DataObject(admin));
            this.readOnlyUserRole = (DataObject) securityLM.userRoleSID.readClasses(session, new DataObject("readonly"));

            this.adminUser = initUser(admin, session);

            this.anonymousUser = initUser("anonymous", session);
        }
    }

    private DataObject initUser(String admin, DataSession session) throws SQLException, SQLHandledException {
        DataObject adminUser = readUser(admin, session);

        if (adminUser == null) {
            adminUser = addUser(admin, initialAdminPassword, session);
            apply(session);

            // to update classes after apply
            return new DataObject((Long) adminUser.object, authenticationLM.customUser);
        }

        return adminUser;
    }

    private DataObject initAndUpdateUser(DataSession session, ExecutionStack stack, String userName, Supplier<String> password, String firstName, String lastName, String email, List<String> groupNames, Map<String, String> attributes) throws SQLException, SQLHandledException {
        DataObject userObject = readUser(userName, session);

        if (userObject == null)
            userObject = addUser(userName, password.get(), session);

        setUserParameters(userObject, firstName, lastName, email, groupNames, attributes, session);
        apply(session, stack);

        return userObject;
    }

    public DataObject getAdminUser() {
        return adminUser;
    }

    public DataObject getDefaultLoginUser() {
        return SystemProperties.inDevMode ? adminUser : anonymousUser;
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
        ObjectValue userObject = authenticationLM.customUserNormalized.readClasses(session, new DataObject(login));
        return userObject.isNull() ? null : (DataObject) userObject;
    }

    public DataObject getUser(String login, DataSession session) throws SQLException, SQLHandledException {
        if(login != null) {
            DataObject user = readUser(login, session);
            if(user == null) {
                throw new AuthenticationException(String.format("User with login %s not found", login));
            }
            return user;
        }
        return getDefaultLoginUser();
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
        return generateToken(userLogin, null);
    }

    public AuthenticationToken generateToken(String userLogin, Integer tokenExpiration) { //tokenExpiration in minutes
        Claims claims = Jwts.claims().setSubject(userLogin);

        claims.setExpiration(new Date(System.currentTimeMillis() +
                (tokenExpiration != null ? tokenExpiration : Settings.get().getAuthTokenExpiration()) * 1000 * 60));

        return new AuthenticationToken(Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact());
    }

    public String signData(String message) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(BaseUtils.getHashBytes(secret), "HmacSHA256");
            sha256_HMAC.init(secretKeySpec);
            byte[] hmacBytes = sha256_HMAC.doFinal(BaseUtils.getHashBytes(message));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw Throwables.propagate(e);
        }
    }

    public boolean verifyData(String message, String signature) {
        return signData(message).equals(signature);
    }

    private String getWebClientSecret() {
        try (DataSession session = createSession()) {
            return (String) authenticationLM.webClientSecret.read(session);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public AuthenticationToken authenticateUser(Authentication authentication, ExecutionStack stack) {
        try (DataSession session = createSession()) {
            DataObject userObject = null;
            if (authentication instanceof PasswordAuthentication) {
                if (authenticationLM.useLDAP.read(session) != null) {
                    String server = (String) authenticationLM.serverLDAP.read(session);
                    Integer port = (Integer) authenticationLM.portLDAP.read(session);
                    String baseDN = (String) authenticationLM.baseDNLDAP.read(session);
                    String userDNSuffix = (String) authenticationLM.userDNSuffixLDAP.read(session);
                    try {
                        String userName = authentication.getUserName();
                        String password = ((PasswordAuthentication) authentication).getPassword();
                        LDAPParameters ldapParameters = new LDAPAuthenticationService(server, port, baseDN, userDNSuffix).authenticate(userName, password);

                        if (ldapParameters.isConnected()) {
                            userObject = initAndUpdateUser(session, stack, userName, () -> password, ldapParameters.getFirstName(), ldapParameters.getLastName(), ldapParameters.getEmail(), ldapParameters.getGroupNames(), ldapParameters.getAttributes());
                        } else {
                            throw new LoginException();
                        }
                    } catch (CommunicationException e) {
                        systemLogger.error("LDAP authentication failed", e);
                    }
                }

                if(userObject == null) {
                    userObject = readUser(authentication.getUserName(), session);

                    if (userObject == null || !authenticationLM.checkPassword(session, userObject, ((PasswordAuthentication) authentication).getPassword()))
                        throw new LoginException();
                }
            } else {
                OAuth2Authentication oauth2 = (OAuth2Authentication) authentication;
                String webClientAuthSecret = oauth2.getAuthSecret();
                if ((webClientAuthSecret == null || !webClientAuthSecret.equals(getWebClientSecret())))
                    throw new AuthenticationException(getString("exceptions.incorrect.web.client.auth.token"));

                // Because user data can change on the oauth2 provider side - we will update userParameters on each authentication.
                userObject = initAndUpdateUser(session, stack, oauth2.getUserName(), () -> BaseUtils.generatePassword(20, false, true), oauth2.getFirstName(), oauth2.getLastName(), oauth2.getEmail(), userObject == null ?  Collections.singletonList("selfRegister") : null, oauth2.getAttributes());
            }
            if (authenticationLM.isLockedCustomUser.read(session, userObject) != null) {
                throw new LockedException();
            }
            return generateToken(authentication.getUserName());
        } catch (SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    public SecurityPolicy getSecurityPolicy(DataSession session, DataObject userObject) {
        List<RoleSecurityPolicy> policies = new ArrayList<>();
        try {
            Set<DataObject> userRoleSet = readUserRoleSet(session, userObject);
            if(!SystemProperties.lightStart || !userRoleSet.contains(adminUserRole))
                for (DataObject userRole : userRoleSet)
                    policies.add(getRoleSecurityPolicy(session, userRole));
            return new SecurityPolicy(policies);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public RoleSecurityPolicy getReadOnlySecurityPolicy(DataSession session) throws SQLException, SQLHandledException {
        return getRoleSecurityPolicy(session, readOnlyUserRole);
    }

    private RoleSecurityPolicy getRoleSecurityPolicy(DataSession session, DataObject userRole) throws SQLException, SQLHandledException {
        Long userRoleId = (Long) userRole.getValue();
        RoleSecurityPolicy policy = cachedSecurityPolicies.get(userRoleId);
        if (policy == null) {
            Object cachedSecuritySemaphore = getCachedSecuritySemaphore(userRoleId);
            synchronized (cachedSecuritySemaphore) { // we use semaphore to prevent simultaneous reading security policy, since this process consumes a lot of time and memory, can be run really a lot of times concurrently (for example when reconnecting users after server restart)
                policy = cachedSecurityPolicies.get(userRoleId);
                if (policy == null) { // double check
                    policy = readSecurityPolicy(userRole, session);
                    cachedSecurityPolicies.put((Long) userRole.getValue(), policy);
                }
            }
        }
        return policy;
    }

    public void prereadSecurityPolicies() {
        try (DataSession session = createSession()) {
            Set<DataObject> userRoleSet = readUserRoleSet(session, null);
            for (DataObject userRole : userRoleSet) {
                cachedSecurityPolicies.put((Long) userRole.getValue(), readSecurityPolicy(userRole, session));
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private RoleSecurityPolicy readSecurityPolicy(DataObject userRoleObject, DataSession session) throws SQLException, SQLHandledException {
        session.sql.pushNoQueryLimit();
        try {
            RoleSecurityPolicy policy = new RoleSecurityPolicy(userRoleObject.equals(readOnlyUserRole));

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
                if (element != null) {
                    policy.navigator.setPermission(element, getPermissionValue(entry.get("permission")));
                } else {
                    startLogDebug(String.format("NavigatorElement '%s' is not found when applying security policy", canonicalName));
                }
            }

            KeyExpr actionOrPropertyExpr = new KeyExpr("actionOrProperty");
            query = new QueryBuilder<>(MapFact.singletonRev("actionOrProperty", actionOrPropertyExpr));
            query.addProperty("canonicalName", reflectionLM.canonicalNameActionOrProperty.getExpr(session.getModifier(), actionOrPropertyExpr));
            query.addProperty("isProperty", reflectionLM.isProperty.getExpr(session.getModifier(), actionOrPropertyExpr));
            query.addProperty("permissionView", securityLM.permissionViewUserRoleActionOrProperty.getExpr(session.getModifier(), userRoleObject.getExpr(), actionOrPropertyExpr));
            query.addProperty("permissionChange", securityLM.permissionChangeUserRoleActionOrProperty.getExpr(session.getModifier(), userRoleObject.getExpr(), actionOrPropertyExpr));
            query.addProperty("permissionEditObjects", securityLM.permissionEditObjectsUserRoleActionOrProperty.getExpr(session.getModifier(), userRoleObject.getExpr(), actionOrPropertyExpr));
            query.addProperty("permissionGroupChange", securityLM.permissionGroupChangeUserRoleActionOrProperty.getExpr(session.getModifier(), userRoleObject.getExpr(), actionOrPropertyExpr));

            query.and(reflectionLM.canonicalNameActionOrProperty.getExpr(session.getModifier(), actionOrPropertyExpr).getWhere());
            query.and(securityLM.permissionViewUserRoleActionOrProperty.getExpr(session.getModifier(), userRoleObject.getExpr(), actionOrPropertyExpr).getWhere().or(
                    securityLM.permissionChangeUserRoleActionOrProperty.getExpr(session.getModifier(), userRoleObject.getExpr(), actionOrPropertyExpr).getWhere()).or(
                    securityLM.permissionEditObjectsUserRoleActionOrProperty.getExpr(session.getModifier(), userRoleObject.getExpr(), actionOrPropertyExpr).getWhere()).or(
                    securityLM.permissionGroupChangeUserRoleActionOrProperty.getExpr(session.getModifier(), userRoleObject.getExpr(), actionOrPropertyExpr).getWhere()
            ));

            queryResult = query.execute(session);
            for (ImMap<Object, Object> entry : queryResult.values()) {

                String canonicalName = trim((String) entry.get("canonicalName"));
                boolean isProperty = entry.get("isProperty") != null;
                try {
                    LAP<?, ?> property = isProperty ? businessLogics.findProperty(canonicalName) : businessLogics.findAction(canonicalName);

                    if (property != null) {
                        ActionOrProperty<?> actionOrProperty = property.getActionOrProperty();
                        policy.propertyView.setPermission(actionOrProperty, getPermissionValue(entry.get("permissionView")));
                        policy.propertyChange.setPermission(actionOrProperty, getPermissionValue(entry.get("permissionChange")));
                        policy.propertyEditObjects.setPermission(actionOrProperty, getPermissionValue(entry.get("permissionEditObjects")));
                        policy.propertyGroupChange.setPermission(actionOrProperty, getPermissionValue(entry.get("permissionGroupChange")));
                    } else {
                        startLogDebug(String.format("Property '%s' is not found when applying security policy", canonicalName));
                    }
                } catch (Throwable ignored) {
                }

            }

            return policy;
        } finally {
            session.sql.popNoQueryLimit();
        }
    }

    private Set<DataObject> readUserRoleSet(DataSession session, DataObject userObject) throws SQLException, SQLHandledException {
        Set<DataObject> userRoleSet = new HashSet<>();

        KeyExpr userRoleExpr = new KeyExpr("userRole");
        QueryBuilder<Object, Object> userRoleQuery = new QueryBuilder<>(MapFact.singletonRev("userRole", userRoleExpr));
        userRoleQuery.and(LM.is(securityLM.userRole).getExpr(userRoleExpr).getWhere());
        if(userObject != null) {
            userRoleQuery.and(securityLM.hasUserRole.getExpr(session.getModifier(), userObject.getExpr(), userRoleExpr).getWhere());
        }
        userRoleQuery.and(securityLM.disableRole.getExpr(session.getModifier(), userRoleExpr).getWhere().not());
        ImOrderMap<ImMap<Object, DataObject>, ImMap<Object, ObjectValue>> queryResult = userRoleQuery.executeClasses(session);
        for (int i = 0, size = queryResult.size(); i < size; i++) {
            userRoleSet.add(queryResult.getKey(i).get("userRole"));
        }
        return userRoleSet;
    }

    public Boolean getPermissionValue(Object permission) {
        if (permission != null) {
            switch ((String) permission) {
                case "Security_Permission.permit":
                    return true;
                case "Security_Permission.forbid":
                    return false;
                default: //Security_Permission.default
                    return null;
            }
        } else return null;
    }

    public void setUserParameters(DataObject customUser, String firstName, String lastName, String email, List<String> userRoleSIDs, Map<String, String> attributes, DataSession session) {
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

                    if (userRole instanceof DataObject) {
                        securityLM.inCustomUserUserRole.change(true, session, customUser, (DataObject) userRole);
                        break;
                    }
                }
            }

            if (attributes != null) {
                for (String key : attributes.keySet()) {
                    String value = attributes.get(key);
                    if (value != null)
                        authenticationLM.attributes.change(value, session, customUser, new DataObject(key));
                }
            }
        } catch (SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }    
}

