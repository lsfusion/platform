package platform.server.logics;

import com.google.common.base.Throwables;
import net.sf.jasperreports.engine.JRException;
import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.InitializingBean;
import platform.base.*;
import platform.base.col.ListFact;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.implementations.HSet;
import platform.base.col.interfaces.immutable.*;
import platform.base.col.interfaces.mutable.*;
import platform.base.col.interfaces.mutable.add.MAddMap;
import platform.base.col.interfaces.mutable.mapvalue.GetIndex;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.interop.Compare;
import platform.interop.RemoteLogicsInterface;
import platform.interop.event.IDaemonTask;
import platform.interop.exceptions.LoginException;
import platform.interop.exceptions.RemoteMessageException;
import platform.interop.form.screen.ExternalScreen;
import platform.interop.form.screen.ExternalScreenParameters;
import platform.interop.navigator.RemoteNavigatorInterface;
import platform.interop.remote.UserInfo;
import platform.server.*;
import platform.server.auth.PolicyManager;
import platform.server.auth.SecurityPolicy;
import platform.server.auth.User;
import platform.server.caches.IdentityLazy;
import platform.server.caches.IdentityStrongLazy;
import platform.server.classes.*;
import platform.server.data.*;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.query.QueryBuilder;
import platform.server.data.sql.DataAdapter;
import platform.server.data.sql.PostgreDataAdapter;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.Type;
import platform.server.data.type.TypeSerializer;
import platform.server.form.entity.*;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.FormSessionScope;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.navigator.*;
import platform.server.integration.*;
import platform.server.logics.linear.LAP;
import platform.server.logics.linear.LCP;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.FormActionProperty;
import platform.server.logics.property.actions.SystemEvent;
import platform.server.logics.property.actions.flow.ChangeFlowType;
import platform.server.logics.property.actions.flow.ListActionProperty;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.property.group.AbstractNode;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.logics.table.DataTable;
import platform.server.logics.table.IDTable;
import platform.server.logics.table.ImplementTable;
import platform.server.mail.NotificationActionProperty;
import platform.server.serialization.ServerSerializationPool;
import platform.server.session.DataSession;

import java.awt.*;
import java.io.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Timestamp;
import java.util.*;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static platform.server.logics.ServerResourceBundle.getString;

// @GenericImmutable нельзя так как Spring валится

public abstract class BusinessLogics<T extends BusinessLogics<T>> extends RemoteContextObject implements RemoteLogicsInterface, InitializingBean {
    protected final static Logger logger = Logger.getLogger(BusinessLogics.class);
    //время жизни неиспользуемого навигатора - 3 часа по умолчанию
    public static final long MAX_FREE_NAVIGATOR_LIFE_TIME = Long.parseLong(System.getProperty("platform.server.navigatorMaxLifeTime", Long.toString(3L * 3600L * 1000L)));

    private List<LogicsModule> logicModules = new ArrayList<LogicsModule>();
    private Map<String, LogicsModule> nameToModule = new HashMap<String, LogicsModule>();

    public final BaseLogicsModule<T> LM;
    public final ServiceLogicsModule serviceLM;
    public final ReflectionLogicsModule reflectionLM;
    public final SecurityLogicsModule securityLM;
    public final SystemEventsLogicsModule systemEventsLM;
    public final EmailLogicsModule emailLM;
    public final SchedulerLogicsModule schedulerLM;
    public final UtilsLogicsModule utilsLM;
    private String dbName;

    public final NavigatorsController navigatorsController;
    public final RestartController restartController;

    public final static SQLSyntax debugSyntax = new PostgreDataAdapter();

    protected final DataAdapter adapter;

    protected final ThreadLocal<SQLSession> sqlRef;

    public LogicsModule getModule(String name) {
        return nameToModule.get(name);
    }

    public SQLSyntax getAdapter() {
        return adapter;
    }

    private Boolean dialogUndecorated = true;

    public Boolean isDialogUndecorated() {
        return dialogUndecorated;
    }

    public void setDialogUndecorated(Boolean dialogUndecorated) {
        this.dialogUndecorated = dialogUndecorated;
    }

    public RemoteNavigatorInterface createNavigator(boolean isFullClient, String login, String password, int computer, String remoteAddress, boolean forceCreateNew) {
        if (restartController.isPendingRestart()) {
            return null;
        }

        return navigatorsController.createNavigator(isFullClient, login, password, computer, remoteAddress, forceCreateNew);
    }

    public TimeZone getTimeZone() {
        return Calendar.getInstance().getTimeZone();
    }

    public Integer getComputer(String strHostName) {
        try {
            Integer result;
            DataSession session = createSession();

            QueryBuilder<String, Object> q = new QueryBuilder<String, Object>(SetFact.singleton("key"));
            q.and(
                    LM.hostname.getExpr(
                            session.getModifier(), q.getMapExprs().get("key")
                    ).compare(new DataObject(strHostName), Compare.EQUALS)
            );

            ImSet<ImMap<String, Object>> keys = q.execute(session).keys();
            if (keys.size() == 0) {
                DataObject addObject = session.addObject(LM.computer);
                LM.hostname.change(strHostName, session, addObject);

                result = (Integer) addObject.object;
                session.apply(this);
            } else {
                result = (Integer) keys.iterator().next().get("key");
            }

            session.close();
            logger.debug("Begin user session " + strHostName + " " + result);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ConcreteClass getDataClass(Object object, Type type) {
        try {
            DataSession session = createSession();
            ConcreteClass result = type.getDataClass(object, session.sql, LM.baseClass);
            session.close();

            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void endSession(String clientInfo) {
        logger.debug("End user session " + clientInfo);
    }

    public Integer getServerComputer() {
        return getComputer(OSUtils.getLocalHostName());
    }

    protected void initExternalScreens() {
    }

    private List<ExternalScreen> externalScreens = new ArrayList<ExternalScreen>();

    protected void addExternalScreen(ExternalScreen screen) {
        externalScreens.add(screen);
    }

    public ExternalScreen getExternalScreen(int screenID) {
        for (ExternalScreen screen : externalScreens) {
            if (screen.getID() == screenID) {
                return screen;
            }
        }
        return null;
    }


    public ExternalScreenParameters getExternalScreenParameters(int screenID, int computerId) throws RemoteException {
        //NP
        return null;
    }


    protected User addUser(String login, String defaultPassword) throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        DataSession session = createSession();

        User user = readUser(login, session);
        if (user == null) {
            DataObject addObject = session.addObject(LM.customUser);
            LM.userLogin.change(login, session, addObject);
            LM.userPassword.change(defaultPassword, session, addObject);
            LM.sha256PasswordCustomUser.change(BaseUtils.calculateBase64Hash("SHA-256", defaultPassword.trim(), UserInfo.salt), session, addObject);
            Integer userID = (Integer) addObject.object;
            session.apply(this);
            user = new User(userID);
        }

        session.close();

        return user;
    }

    public User readUser(String login, DataSession session) throws SQLException {
        Integer userId = (Integer) LM.loginToUser.read(session, new DataObject(login, StringClass.get(30)));
        if (userId == null) {
            return null;
        }
        User userObject = new User(userId);

        applyDefaultPolicy(userObject);

        List<SecurityPolicy> codeUserPolicy = policyManager.userPolicies.get(userObject.ID);
        if (codeUserPolicy != null) {
            for (SecurityPolicy policy : codeUserPolicy)
                userObject.addSecurityPolicy(policy);
        }

        applyFormDefinedUserPolicy(userObject);

        List<Integer> userPoliciesIds = readUserPoliciesIds(userId);
        for (int policyId : userPoliciesIds) {
            SecurityPolicy policy = policyManager.getPolicy(policyId);
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
            q.and(q.getMapExprs().get("userId").compare(new DataObject(userId, LM.customUser), Compare.EQUALS));

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
        return getProperties().mapRevKeys(new GetValue<String, Property>() {
            public String getMapValue(Property value) {
                return value.getSID();
            }});
    }

    private void applyDefaultPolicy(User user) {
        ImRevMap<String, Property> sidProperties = getSIDProperties();

        //сначала политика по умолчанию из кода
        user.addSecurityPolicy(policyManager.defaultSecurityPolicy);
        //затем политика по умолчанию из визуальной настройки
        SecurityPolicy policy = new SecurityPolicy(-1);
        try {
            DataSession session = createSession();

            QueryBuilder<String, String> qf = new QueryBuilder<String, String>(SetFact.singleton("formId"));
            Expr expr = reflectionLM.sidNavigatorElement.getExpr(session.getModifier(), qf.getMapExprs().get("formId"));
            qf.and(expr.getWhere());
            qf.addProperty("sid", expr);
            qf.addProperty("permit", securityLM.permitNavigatorElement.getExpr(session.getModifier(), qf.getMapExprs().get("formId")));
            qf.addProperty("forbid", securityLM.forbidNavigatorElement.getExpr(session.getModifier(), qf.getMapExprs().get("formId")));

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
            qp.addProperty("sid", expr2);
            qp.addProperty("permitView", securityLM.permitViewProperty.getExpr(session.getModifier(), qp.getMapExprs().get("propertyId")));
            qp.addProperty("forbidView", securityLM.forbidViewProperty.getExpr(session.getModifier(), qp.getMapExprs().get("propertyId")));
            qp.addProperty("permitChange", securityLM.permitChangeProperty.getExpr(session.getModifier(), qp.getMapExprs().get("propertyId")));
            qp.addProperty("forbidChange", securityLM.forbidChangeProperty.getExpr(session.getModifier(), qp.getMapExprs().get("propertyId")));

            ImCol<ImMap<String, Object>> propertyValues = qp.execute(session.sql).values();
            for (ImMap<String, Object> valueMap : propertyValues) {
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
        ImRevMap<String, Property> sidProperties = getSIDProperties();

        SecurityPolicy policy = new SecurityPolicy(-1);
        try {
            DataSession session = createSession();

            DataObject userObject = new DataObject(user.ID, LM.customUser);

            Object forbidAll = securityLM.forbidAllFormsUser.read(session, userObject);
            Object allowAll = securityLM.permitAllFormsUser.read(session, userObject);
            if (forbidAll != null)
                policy.navigator.defaultPermission = false;
            else if (allowAll != null)
                policy.navigator.defaultPermission = true;


            Object forbidViewAll = securityLM.forbidViewAllPropertyUser.read(session, userObject);
            Object allowViewAll = securityLM.permitViewAllPropertyUser.read(session, userObject);
            if (forbidViewAll != null)
                policy.property.view.defaultPermission = false;
            else if (allowViewAll != null)
                policy.property.view.defaultPermission = true;


            Object forbidChangeAll = securityLM.forbidChangeAllPropertyRole.read(session, userObject);
            Object allowChangeAll = securityLM.permitChangeAllPropertyUser.read(session, userObject);
            if (forbidChangeAll != null)
                policy.property.change.defaultPermission = false;
            else if (allowChangeAll != null)
                policy.property.change.defaultPermission = true;


            QueryBuilder<String, String> qf = new QueryBuilder<String, String>(SetFact.toExclSet("userId", "formId"));
            Expr formExpr = reflectionLM.sidNavigatorElement.getExpr(session.getModifier(), qf.getMapExprs().get("formId"));
            qf.and(formExpr.getWhere());
            qf.and(qf.getMapExprs().get("userId").compare(new DataObject(user.ID, LM.customUser), Compare.EQUALS));

            qf.addProperty("sid", formExpr);
            qf.addProperty("permit", securityLM.permitUserNavigatorElement.getExpr(session.getModifier(), qf.getMapExprs().get("userId"), qf.getMapExprs().get("formId")));
            qf.addProperty("forbid", securityLM.forbidUserNavigatorElement.getExpr(session.getModifier(), qf.getMapExprs().get("userId"), qf.getMapExprs().get("formId")));

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
            qp.and(qp.getMapExprs().get("userId").compare(new DataObject(user.ID, LM.customUser), Compare.EQUALS));
            qp.and(securityLM.notNullPermissionUserProperty.getExpr(session.getModifier(), qp.getMapExprs().get("userId"), qp.getMapExprs().get("propertyId")).getWhere());

            qp.addProperty("sid", propExpr);
            qp.addProperty("permitView", securityLM.permitViewUserProperty.getExpr(session.getModifier(), qp.getMapExprs().get("userId"), qp.getMapExprs().get("propertyId")));
            qp.addProperty("forbidView", securityLM.forbidViewUserProperty.getExpr(session.getModifier(), qp.getMapExprs().get("userId"), qp.getMapExprs().get("propertyId")));
            qp.addProperty("permitChange", securityLM.permitChangeUserProperty.getExpr(session.getModifier(), qp.getMapExprs().get("userId"), qp.getMapExprs().get("propertyId")));
            qp.addProperty("forbidChange", securityLM.forbidChangeUserProperty.getExpr(session.getModifier(), qp.getMapExprs().get("userId"), qp.getMapExprs().get("propertyId")));

            ImCol<ImMap<String, Object>> propValues = qp.execute(session.sql).values();
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

    public boolean showDefaultForms(DataObject user) {
        try {
            DataSession session = createSession();

            if (securityLM.defaultFormsUser.read(session, user) != null) {
                return true;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    public ArrayList<String> getDefaultForms(DataObject user) {
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

    protected SecurityPolicy addPolicy(String policyName, String description) throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        DataSession session = createSession();

        Integer policyID = readPolicy(policyName, session);
        if (policyID == null) {
            DataObject addObject = session.addObject(securityLM.policy);
            LM.name.change(policyName, session, addObject);
            securityLM.descriptionPolicy.change(description, session, addObject);
            policyID = (Integer) addObject.object;
            session.apply(this);
        }

        session.close();

        SecurityPolicy policyObject = new SecurityPolicy(policyID);
        policyManager.putPolicy(policyID, policyObject);
        return policyObject;
    }

    private Integer readPolicy(String name, DataSession session) throws SQLException {
        return (Integer) securityLM.policyName.read(session, new DataObject(name, StringClass.get(50)));
    }

    public Property getProperty(String sid) {
        return LM.rootGroup.getProperty(sid);
    }

    public ObjectValueProperty getObjectValueProperty(ValueClass... valueClasses) {
        ImList<Property> properties = LM.objectValue.getProperties(valueClasses);
        return properties.size() > 0
                ? (ObjectValueProperty) properties.get(0)
                : null;
    }

    protected SecurityPolicy permitAllPolicy, readOnlyPolicy, allowConfiguratorPolicy;

    void initBaseAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        permitAllPolicy = addPolicy(getString("logics.policy.allow.all"), getString("logics.policy.allows.all.actions"));
        permitAllPolicy.setReplaceMode(true);

        readOnlyPolicy = addPolicy(getString("logics.policy.forbid.editing.all.properties"), getString("logics.policy.read.only.forbids.editing.of.all.properties.on.the.forms"));
        readOnlyPolicy.property.change.defaultPermission = false;
        readOnlyPolicy.cls.edit.add.defaultPermission = false;
        readOnlyPolicy.cls.edit.change.defaultPermission = false;
        readOnlyPolicy.cls.edit.remove.defaultPermission = false;

        allowConfiguratorPolicy = addPolicy(getString("logics.policy.allow.configurator"), getString("logics.policy.logics.allow.configurator"));
        allowConfiguratorPolicy.configurator = true;
    }

    public void ping() throws RemoteException {
        //for filterIncl-alive
    }

    public String getUserName(DataObject user) {
        try {
            return (String) LM.userLogin.read(createSession(), user);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    // по умолчанию с полным стартом

    protected <T extends LogicsModule> T addModule(T module) {
        logicModules.add(module);
        return module;
    }

    protected void createModules() throws IOException {
        addModule(LM);
        addModule(serviceLM);
        addModule(reflectionLM);
        addModule(securityLM);
        addModule(systemEventsLM);
        addModule(emailLM);
        addModule(schedulerLM);
        addModule(utilsLM);
    }

    protected void addModulesFromResource(List<String> paths, List<String> excludedPaths) throws IOException {

        List<String> excludedLSF = new ArrayList<String>();

        if (excludedPaths != null) {
            for (String filePath : excludedPaths) {
                if (filePath.endsWith(".lsf")) {
                    excludedLSF.add(filePath);
                } else {
                    Pattern pattern = Pattern.compile(filePath + ".*\\.lsf");
                    Collection<String> list = ResourceList.getResources(pattern);
                    for (String name : list) {
                        excludedLSF.add(name);
                    }
                }
            }
        }

        for (String filePath : paths) {
            if (filePath.endsWith(".lsf")) {
                if (!excludedLSF.contains(filePath))
                    addModule(new ScriptingLogicsModule(getClass().getResourceAsStream(filePath), LM, this));
            } else {
                Pattern pattern = Pattern.compile(filePath + ".*\\.lsf");
                Collection<String> list = ResourceList.getResources(pattern);
                for (String name : list) {
                    if (!excludedLSF.contains(name))
                        addModule(new ScriptingLogicsModule(getClass().getResourceAsStream("/" + name), LM, this));
                }
            }
        }
    }

    protected void addModulesFromResource(String... paths) throws IOException {
        for (String path : paths) {
            addModuleFromResource(path);
        }
    }

    protected ScriptingLogicsModule addModuleFromResource(String path) throws IOException {
        InputStream is = getClass().getResourceAsStream("/" + path);
        if (is == null)
            throw new RuntimeException(String.format("[error]:\tmodule '%s' cannot be found", path));
        return addModule(new ScriptingLogicsModule(is, LM, this));
    }

    private void fillNameToModules() {
        for (LogicsModule module : logicModules) {
            if (nameToModule.containsKey(module.getName())) {
                throw new RuntimeException(String.format("[error]:\tmodule '%s' has already been added", module.getName()));
            }
            nameToModule.put(module.getName(), module);
        }
    }

    private Map<String, List<String>> buildModuleGraph() {
        Map<String, List<String>> graph = new HashMap<String, List<String>>();
        for (LogicsModule module : logicModules) {
            graph.put(module.getName(), new ArrayList<String>());
        }

        for (LogicsModule module : logicModules) {
            for (String reqModule : module.getRequiredModules()) {
                if (graph.get(reqModule) == null) {
                    throw new RuntimeException(String.format("[error]:\t%s:\trequired module '%s' was not found", module.getName(), reqModule));
                }
                graph.get(reqModule).add(module.getName());
            }
        }
        return graph;
    }

    private void checkCycles(String cur, LinkedList<String> way, Set<String> used, Map<String, List<String>> graph) {
        way.add(cur);
        used.add(cur);
        for (String next : graph.get(cur)) {
            if (!used.contains(next)) {
                checkCycles(next, way, used, graph);
            } else if (way.contains(next)) {
                String errMsg = next;
                do {
                    errMsg = errMsg + " <- " + way.peekLast();
                } while (!way.pollLast().equals(next));
                throw new RuntimeException("[error]:\tthere is a circular dependency: " + errMsg);
            }
        }
        way.removeLast();
    }

    private void checkCycles(Map<String, List<String>> graph) {
        Set<String> used = new HashSet<String>();
        for (Map.Entry<String, List<String>> vertex : graph.entrySet()) {
            if (!used.contains(vertex.getKey())) {
                checkCycles(vertex.getKey(), new LinkedList<String>(), used, graph);
            }
        }
    }

    // Формирует .dot файл для построения графа иерархии модулей с помощью graphviz
    private void outDotFile() {
        try {
            FileWriter fstream = new FileWriter("D:/lsf/modules.dot");
            BufferedWriter out = new BufferedWriter(fstream);
            out.write("digraph Modules {\n");
            out.write("\tsize=\"6,4\"; ratio = fill;\n");
            out.write("\tnode [shape=box, fontsize=60, style=filled];\n");
            out.write("\tedge [arrowsize=2];\n");
            for (LogicsModule module : logicModules) {
                for (String name : module.getRequiredModules()) {
                    out.write("\t" + name + " -> " + module.getName() + ";\n");
                }
            }
            out.write("}\n");
            out.close();
        } catch (Exception e) {
        }
    }

    private List<LogicsModule> orderModules() {
        fillNameToModules();
        Map<String, List<String>> graph = buildModuleGraph();
//        outDotFile();
        checkCycles(graph);

        Map<String, Integer> degree = new HashMap<String, Integer>();
        for (LogicsModule module : logicModules) {
            degree.put(module.getName(), module.getRequiredModules().size());
        }

        Set<LogicsModule> usedModules = new LinkedHashSet<LogicsModule>();
        for (int i = 0; i < logicModules.size(); ++i) {
            for (LogicsModule module : logicModules) {
                if (degree.get(module.getName()) == 0 && !usedModules.contains(module)) {
                    for (String nextModule : graph.get(module.getName())) {
                        degree.put(nextModule, degree.get(nextModule) - 1);
                    }
                    usedModules.add(module);
                    break;
                }
            }
        }
        return new ArrayList<LogicsModule>(usedModules);
    }

    private void resetModuleList(String startModulesList) {

        Set<LogicsModule> was = new HashSet<LogicsModule>();
        Queue<LogicsModule> queue = new LinkedList<LogicsModule>();

        fillNameToModules();

        for (String moduleName : startModulesList.split(",\\s*")) {
            LogicsModule startModule = nameToModule.get(moduleName);
            assert startModule != null;
            queue.add(startModule);
            was.add(startModule);
        }

        while (!queue.isEmpty()) {
            LogicsModule current = queue.poll();

            for (String nextModuleName : current.getRequiredModules()) {
                LogicsModule nextModule = nameToModule.get(nextModuleName);
                if (!was.contains(nextModule)) {
                    was.add(nextModule);
                    queue.add(nextModule);
                }
            }
        }

        logicModules = new ArrayList<LogicsModule>(was);
        nameToModule.clear();
    }

    static class ScriptErrorsException extends RuntimeException {
        public ScriptErrorsException(String message) {
            super(message);
        }
    }

    protected void initModules() throws ClassNotFoundException, IOException, SQLException, InstantiationException, IllegalAccessException, JRException {
        String errors = "";
        Exception ex = null;
        try {
            for (LogicsModule module : logicModules) {
                module.initModuleDependencies();
            }

            if (System.getProperty("platform.server.logics.startmodules") != null) {
                resetModuleList(System.getProperty("platform.server.logics.startmodules"));
            }

            List<LogicsModule> orderedModules = orderModules();

            for (LogicsModule module : orderedModules) {
                module.initModule();
            }

            for (LogicsModule module : orderedModules) {
                module.initGroups();
            }

            for (LogicsModule module : orderedModules) {
                module.initClasses();
            }

            LM.baseClass.initObjectClass();
            LM.storeCustomClass(LM.baseClass.objectClass);

            for (LogicsModule module : orderedModules) {
                module.initTables();
            }

            initExternalScreens();

            for (LogicsModule module : orderedModules) {
                module.initProperties();
            }

            finishAbstract();

            finishActions();

            finishLogInit();

            showDependencies();

            getPropertyList(true);
            for(Property property : getPropertyList())
                property.finalizeAroundInit();

            LM.initClassForms();

            Set idSet = new HashSet<String>();
            for (Property property : getOrderProperties()) {
                //            assert idSet.add(property.getSID()) : "Same sid " + property.getSID();
            }

            for (LogicsModule module : orderedModules) {
                module.initIndexes();
            }
            assert checkProps();

            synchronizeDB();

            if (!BusinessLogicsBootstrap.isDebug()) {
                prereadCaches();
            }

            //setUserLoggableProperties();
            setPropertyNotifications();
            setNotNullProperties();
        } catch (RecognitionException e) {
            errors += e.getMessage();
        } catch (Exception e) {
            ex = e;
        }

        String syntaxErrors = "";
        for (LogicsModule module : logicModules) {
            syntaxErrors += module.getErrorsDescription();
        }

        if (errors.length() > 0 || syntaxErrors.length() > 0) {
            errors = "\n" + syntaxErrors + errors;
            if (ex != null) {
                errors += ex.getMessage();
            }
            throw new ScriptErrorsException(errors);
        } else if (ex != null) {
            Throwables.propagate(ex);
        }
    }

    protected void finishAbstract() {
        List<CaseUnionProperty> abstractUnions = new ArrayList<CaseUnionProperty>();
        for (Property property : getOrderProperties())
            if (property instanceof CaseUnionProperty && ((CaseUnionProperty) property).isAbstract()) {
                property.finalizeInit();
                abstractUnions.add((CaseUnionProperty) property);
            }

        for (CaseUnionProperty abstractUnion : abstractUnions)
            abstractUnion.checkExclusive();
    }

    protected void finishActions() { // потому как могут использовать abstract
        for (Property property : getOrderProperties())
            if(property instanceof ActionProperty) {
                if(property instanceof ListActionProperty && ((ListActionProperty)property).isAbstract())
                    property.finalizeInit();

                ImMap<CalcProperty, Boolean> change = ((ActionProperty<?>) property).getChangeExtProps();
                for(int i=0,size=change.size();i<size;i++) // вообще говоря DataProperty и IsClassProperty
                    change.getKey(i).addActionChangeProp(new Pair<Property<?>, LinkType>(property, change.getValue(i) ? LinkType.RECCHANGE : LinkType.DEPEND));
            }
    }

    private void finishLogInit() {
        // с одной стороны нужно отрисовать на форме логирования все свойства из recognizeGroup, с другой - LogFormEntity с Action'ом должен уже существовать
        // поэтому makeLoggable делаем сразу, а LogFormEntity при желании заполняем здесь
        for (Property property : getOrderProperties()) {
            if (property.loggable && property.logFormProperty.property instanceof FormActionProperty &&
                    ((FormActionProperty) property.logFormProperty.property).form instanceof LogFormEntity) {
                LogFormEntity logForm = (LogFormEntity) ((FormActionProperty) property.logFormProperty.property).form;
                if (logForm.lazyInit)
                    logForm.initProperties();
            }
        }
    }

    private void prereadCaches() {
        getAppliedProperties(true);
        getAppliedProperties(false);
        getMapAppliedDepends();
        for (Property property : getPropertyList()) // сделалем чтобы
            property.prereadCaches();
    }

    public BusinessLogics(DataAdapter adapter, int exportPort) throws IOException {
        super(exportPort);

        Context.context.set(this);

        this.adapter = adapter;

        LM = new BaseLogicsModule(this, logger);

        serviceLM = new ServiceLogicsModule(this, this.LM);

        reflectionLM = new ReflectionLogicsModule(this, this.LM);

        securityLM = new SecurityLogicsModule(this, this.LM);

        systemEventsLM = new SystemEventsLogicsModule(this, this.LM);

        emailLM = new EmailLogicsModule(this, this.LM);

        schedulerLM = new SchedulerLogicsModule(this, this.LM);

        utilsLM = new UtilsLogicsModule(this, this.LM);

        navigatorsController = new NavigatorsController(this);

        restartController = new RestartController(this);

        sqlRef = new ThreadLocal<SQLSession>() {
            @Override
            public SQLSession initialValue() {
                try {
                    return createSQL();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        createModules();
        initModules();

        if (!BusinessLogicsBootstrap.isDebug()) {
            synchronizeForms();
            synchronizeGroupProperties();
            synchronizeProperties();
        }

        synchronizeTables();
        new Scheduler(this).runScheduler();

        initBaseAuthentication();
        initAuthentication();

        systemEventsLM.resetConnectionStatus();
        logLaunch();

        // считаем системного пользователя
        try {
            DataSession session = createSession(getThreadLocalSql(), new UserController() {
                        public void changeCurrentUser(DataObject user) {
                            throw new RuntimeException("not supported");
                        }

                        public DataObject getCurrentUser() {
                            return new DataObject(0, LM.systemUser);
                        }
                    }, new ComputerController() {
                public DataObject getCurrentComputer() {
                    return new DataObject(0, LM.computer);
                }

                public boolean isFullClient() {
                    return false;
                }
            }
            );

            QueryBuilder<String, Object> query = new QueryBuilder<String, Object>(SetFact.singleton("key"));
            query.and(query.getMapExprs().singleValue().isClass(LM.systemUser));
            ImOrderSet<ImMap<String, Object>> rows = query.execute(session, MapFact.<Object, Boolean>EMPTYORDER(), 1).keyOrderSet();
            if (rows.size() == 0) { // если нету добавим
                systemUserObject = (Integer) session.addObject(LM.systemUser).object;
                session.apply(this);
            } else
                systemUserObject = (Integer) rows.single().get("key");

            query = new QueryBuilder<String, Object>(SetFact.singleton("key"));
            query.and(LM.hostname.getExpr(session.getModifier(), query.getMapExprs().singleValue()).compare(new DataObject("systemhost"), Compare.EQUALS));
            rows = query.execute(session, MapFact.<Object, Boolean>EMPTYORDER(), 1).keyOrderSet();
            if (rows.size() == 0) { // если нету добавим
                DataObject computerObject = session.addObject(LM.computer);
                systemComputer = (Integer) computerObject.object;
                LM.hostname.change("systemhost", session, computerObject);
                session.apply(this);
            } else
                systemComputer = (Integer) rows.single().get("key");

            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // запишем текущую дату
        changeCurrentDate();

        Thread thread = new ContextAwareThread(this, new Runnable() {
            long time = 1000;
            boolean first = true;

            public void run() {
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR);
                if (calendar.get(Calendar.AM_PM) == Calendar.PM) {
                    hour += 12;
                }
                time = (23 - hour) * 500 * 60 * 60;
                while (true) {
                    try {
                        calendar = Calendar.getInstance();
                        hour = calendar.get(Calendar.HOUR);
                        if (calendar.get(Calendar.AM_PM) == Calendar.PM) {
                            hour += 12;
                        }
                        if (hour == 0 && first) {
                            changeCurrentDate();
                            time = 12 * 60 * 60 * 1000;
                            first = false;
                        }
                        if (hour == 23) {
                            first = true;
                        }
                        Thread.sleep(time);
                        time = time / 2;
                        if (time < 1000) {
                            time = 1000;
                        }
                    } catch (Exception ignore) {
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();

        reloadNavigatorTree();
    }

    private String getRevision() {
        String revision = null;
        InputStream manifestStream = getClass().getResourceAsStream("/platform/server/../../META-INF/MANIFEST.MF");
        try {
            if (manifestStream != null) {
                Manifest manifest = new Manifest(manifestStream);
                Attributes attributes = manifest.getMainAttributes();
                revision = attributes.getValue("SCM-Revision");
            }
        } catch (IOException ex) {
        }
        return revision;
    }

    private void logLaunch() throws SQLException {

        DataSession session = createSession();

        DataObject newLaunch = session.addObject(systemEventsLM.launch);
        systemEventsLM.computerLaunch.change(getComputer(OSUtils.getLocalHostName()), session, newLaunch);
        systemEventsLM.timeLaunch.change(LM.currentDateTime.read(session), session, newLaunch);
        systemEventsLM.revisionLaunch.change(getRevision(), session, newLaunch);

        session.apply(this);
        session.close();
    }


    public SQLSession getThreadLocalSql() {
        return sqlRef.get();
    }

    @IdentityStrongLazy // ресурсы потребляет
    private SQLSession getIDSql() throws SQLException { // подразумевает synchronized использование
        try {
            return createSQL(Connection.TRANSACTION_REPEATABLE_READ);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected List<List<Object>> getRelations(NavigatorElement<T> element) {
        List<List<Object>> parentInfo = new ArrayList<List<Object>>();
        List<NavigatorElement<T>> children = (List<NavigatorElement<T>>) element.getChildren(false);
        int counter = 1;
        for (NavigatorElement<T> child : children) {
            parentInfo.add(BaseUtils.toList((Object) child.getSID(), element.getSID(), counter++));
            parentInfo.addAll(getRelations(child));
        }
        return parentInfo;
    }

    protected void synchronizeForms() {
        synchronizeNavigatorElements(reflectionLM.form, FormEntity.class, false, reflectionLM.isForm);
        synchronizeNavigatorElements(reflectionLM.navigatorAction, NavigatorAction.class, true, reflectionLM.isNavigatorAction);
        synchronizeNavigatorElements(reflectionLM.navigatorElement, NavigatorElement.class, true, reflectionLM.isNavigatorElement);
        synchronizeParents();
        synchronizeGroupObjects();
        synchronizePropertyDraws();
    }

    private void synchronizeNavigatorElements(ConcreteCustomClass elementCustomClass, Class<? extends NavigatorElement> filterJavaClass, boolean exactJavaClass, LCP deleteLP) {
        ImportField sidField = new ImportField(reflectionLM.navigatorElementSIDClass);
        ImportField captionField = new ImportField(reflectionLM.navigatorElementCaptionClass);

        ImportKey<?> keyNavigatorElement = new ImportKey(elementCustomClass, reflectionLM.navigatorElementSID.getMapping(sidField));

        List<List<Object>> elementsData = new ArrayList<List<Object>>();
        for (NavigatorElement<T> element : getNavigatorElements()) {
            if (exactJavaClass ? filterJavaClass == element.getClass() : filterJavaClass.isInstance(element)) {
                elementsData.add(asList((Object) element.getSID(), element.caption));
            }
        }

        List<ImportProperty<?>> propsNavigatorElement = new ArrayList<ImportProperty<?>>();
        propsNavigatorElement.add(new ImportProperty(sidField, reflectionLM.sidNavigatorElement.getMapping(keyNavigatorElement)));
        propsNavigatorElement.add(new ImportProperty(captionField, reflectionLM.captionNavigatorElement.getMapping(keyNavigatorElement)));

        List<ImportDelete> deletes = asList(
                new ImportDelete(keyNavigatorElement, deleteLP.getMapping(keyNavigatorElement), false)
        );
        ImportTable table = new ImportTable(asList(sidField, captionField), elementsData);

        try {
            DataSession session = createSession();
            session.pushVolatileStats();

            IntegrationService service = new IntegrationService(session, table, asList(keyNavigatorElement), propsNavigatorElement, deletes);
            service.synchronize(true, false);

            session.popVolatileStats();
            session.apply(this);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void synchronizeParents() {
        ImportField sidField = new ImportField(reflectionLM.navigatorElementSIDClass);
        ImportField parentSidField = new ImportField(reflectionLM.navigatorElementSIDClass);
        ImportField numberField = new ImportField(reflectionLM.numberNavigatorElement);

        List<List<Object>> dataParents = getRelations(LM.root);

        ImportKey<?> keyElement = new ImportKey(reflectionLM.navigatorElement, reflectionLM.navigatorElementSID.getMapping(sidField));
        ImportKey<?> keyParent = new ImportKey(reflectionLM.navigatorElement, reflectionLM.navigatorElementSID.getMapping(parentSidField));
        List<ImportProperty<?>> propsParent = new ArrayList<ImportProperty<?>>();
        propsParent.add(new ImportProperty(parentSidField, reflectionLM.parentNavigatorElement.getMapping(keyElement), LM.object(reflectionLM.navigatorElement).getMapping(keyParent)));
        propsParent.add(new ImportProperty(numberField, reflectionLM.numberNavigatorElement.getMapping(keyElement), GroupType.MIN));
        ImportTable table = new ImportTable(asList(sidField, parentSidField, numberField), dataParents);
        try {
            DataSession session = createSession();
            session.pushVolatileStats();

            IntegrationService service = new IntegrationService(session, table, asList(keyElement, keyParent), propsParent);
            service.synchronize(true, false);

            session.popVolatileStats();
            session.apply(this);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void synchronizePropertyDraws() {

        List<List<Object>> dataPropertyDraws = new ArrayList<List<Object>>();
        for (FormEntity<T> formElement : getFormEntities()) {
                List<PropertyDrawEntity> propertyDraws = formElement.propertyDraws;
                for (PropertyDrawEntity drawEntity : propertyDraws) {
                    GroupObjectEntity groupObjectEntity = drawEntity.getToDraw(formElement);
                    dataPropertyDraws.add(asList(drawEntity.propertyObject.toString(), drawEntity.getSID(), (Object) formElement.getSID(), groupObjectEntity == null ? null : groupObjectEntity.getSID()));
                }
        }

        ImportField captionPropertyDrawField = new ImportField(reflectionLM.propertyCaptionValueClass);
        ImportField sidPropertyDrawField = new ImportField(reflectionLM.propertySIDValueClass);
        ImportField sidNavigatorElementField = new ImportField(reflectionLM.navigatorElementSIDClass);
        ImportField sidGroupObjectField = new ImportField(reflectionLM.propertySIDValueClass);

        ImportKey<?> keyForm = new ImportKey(reflectionLM.form, reflectionLM.navigatorElementSID.getMapping(sidNavigatorElementField));
        ImportKey<?> keyPropertyDraw = new ImportKey(reflectionLM.propertyDraw, reflectionLM.propertyDrawSIDNavigatorElementSIDPropertyDraw.getMapping(sidNavigatorElementField, sidPropertyDrawField));
        ImportKey<?> keyGroupObject = new ImportKey(reflectionLM.groupObject, reflectionLM.groupObjectSIDGroupObjectSIDNavigatorElementGroupObject.getMapping(sidGroupObjectField, sidNavigatorElementField));

        List<ImportProperty<?>> propsPropertyDraw = new ArrayList<ImportProperty<?>>();
        propsPropertyDraw.add(new ImportProperty(captionPropertyDrawField, reflectionLM.captionPropertyDraw.getMapping(keyPropertyDraw)));
        propsPropertyDraw.add(new ImportProperty(sidPropertyDrawField, reflectionLM.sidPropertyDraw.getMapping(keyPropertyDraw)));
        propsPropertyDraw.add(new ImportProperty(sidNavigatorElementField, reflectionLM.formPropertyDraw.getMapping(keyPropertyDraw), LM.object(reflectionLM.navigatorElement).getMapping(keyForm)));
        propsPropertyDraw.add(new ImportProperty(sidGroupObjectField, reflectionLM.groupObjectPropertyDraw.getMapping(keyPropertyDraw), LM.object(reflectionLM.groupObject).getMapping(keyGroupObject)));


        List<ImportDelete> deletes = new ArrayList<ImportDelete>();
        deletes.add(new ImportDelete(keyPropertyDraw, LM.is(reflectionLM.propertyDraw).getMapping(keyPropertyDraw), false));

        ImportTable table = new ImportTable(asList(captionPropertyDrawField, sidPropertyDrawField, sidNavigatorElementField, sidGroupObjectField), dataPropertyDraws);

        try {
            DataSession session = createSession();
            session.pushVolatileStats();

            IntegrationService service = new IntegrationService(session, table, asList(keyForm, keyPropertyDraw, keyGroupObject), propsPropertyDraw, deletes);
            service.synchronize(true, false);

            session.popVolatileStats();
            session.apply(this);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void synchronizeGroupObjects() {

        List<List<Object>> dataGroupObjectList = new ArrayList<List<Object>>();
        for (FormEntity<T> formElement : getFormEntities()) { //formSID - sidGroupObject
                for (PropertyDrawEntity property : formElement.propertyDraws) {
                    GroupObjectEntity groupObjectEntity = property.getToDraw(formElement);
                    if (groupObjectEntity != null)
                        dataGroupObjectList.add(Arrays.asList((Object) formElement.getSID(),
                                groupObjectEntity.getSID()));
                }
        }

        ImportField sidNavigatorElementField = new ImportField(reflectionLM.navigatorElementSIDClass);
        ImportField sidGroupObjectField = new ImportField(reflectionLM.propertySIDValueClass);

        ImportKey<?> keyForm = new ImportKey(reflectionLM.form, reflectionLM.navigatorElementSID.getMapping(sidNavigatorElementField));
        ImportKey<?> keyGroupObject = new ImportKey(reflectionLM.groupObject, reflectionLM.groupObjectSIDGroupObjectSIDNavigatorElementGroupObject.getMapping(sidGroupObjectField, sidNavigatorElementField));

        List<ImportProperty<?>> propsGroupObject = new ArrayList<ImportProperty<?>>();
        propsGroupObject.add(new ImportProperty(sidGroupObjectField, reflectionLM.sidGroupObject.getMapping(keyGroupObject)));
        propsGroupObject.add(new ImportProperty(sidNavigatorElementField, reflectionLM.navigatorElementGroupObject.getMapping(keyGroupObject), LM.object(reflectionLM.navigatorElement).getMapping(keyForm)));

        List<ImportDelete> deletes = new ArrayList<ImportDelete>();
        deletes.add(new ImportDelete(keyGroupObject, LM.is(reflectionLM.groupObject).getMapping(keyGroupObject), false));

        ImportTable table = new ImportTable(asList(sidNavigatorElementField, sidGroupObjectField), dataGroupObjectList);

        try {
            DataSession session = createSession();
            session.pushVolatileStats();

            IntegrationService service = new IntegrationService(session, table, asList(keyForm, keyGroupObject), propsGroupObject, deletes);
            service.synchronize(true, false);

            session.popVolatileStats();
            session.apply(this);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean needsToBeSynchronized(Property property) {
        return !LM.isGeneratedSID(property.getSID()) && (property instanceof ActionProperty || property.isFull());
    }

    private void synchronizeProperties() {
        synchronizePropertyEntities();
        synchronizePropertyParents();
    }

    private void synchronizePropertyEntities() {
        ImportField sidPropertyField = new ImportField(reflectionLM.propertySIDValueClass);
        ImportField captionPropertyField = new ImportField(reflectionLM.propertyCaptionValueClass);
        ImportField loggablePropertyField = new ImportField(reflectionLM.propertyLoggableValueClass);
        ImportField storedPropertyField = new ImportField(reflectionLM.propertyStoredValueClass);
        ImportField isSetNotNullPropertyField = new ImportField(reflectionLM.propertyIsSetNotNullValueClass);
        ImportField signaturePropertyField = new ImportField(reflectionLM.propertySignatureValueClass);
        ImportField returnPropertyField = new ImportField(reflectionLM.propertySignatureValueClass);
        ImportField classPropertyField = new ImportField(reflectionLM.propertySignatureValueClass);

        ImportKey<?> keyProperty = new ImportKey(reflectionLM.property, reflectionLM.propertySID.getMapping(sidPropertyField));

        List<List<Object>> dataProperty = new ArrayList<List<Object>>();
        for (Property property : getOrderProperties()) {
            if (needsToBeSynchronized(property)) {
                String commonClasses = "";
                String returnClass = "";
                String classProperty = "";
                try {
                    classProperty = property.getClass().getSimpleName();
                    returnClass = property.getValueClass().getSID();
                    for (Object cc : property.getInterfaceClasses().valueIt()) {
                        if (cc instanceof CustomClass)
                            commonClasses += ((CustomClass) cc).getSID() + ", ";
                        else if (cc instanceof DataClass)
                            commonClasses += ((DataClass) cc).getSID() + ", ";
                    }
                    if (!"".equals(commonClasses))
                        commonClasses = commonClasses.substring(0, commonClasses.length() - 2);
                } catch (NullPointerException e) {
                    commonClasses = "";
                } catch (ArrayIndexOutOfBoundsException e) {
                    commonClasses = "";
                }
                dataProperty.add(asList((Object) property.getSID(), property.caption, property.loggable ? true : null,
                        property instanceof CalcProperty && ((CalcProperty) property).isStored() ? true : null, property instanceof CalcProperty && ((CalcProperty) property).setNotNull ? true : null, commonClasses, returnClass, classProperty));
            }
        }

        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
        properties.add(new ImportProperty(sidPropertyField, reflectionLM.SIDProperty.getMapping(keyProperty)));
        properties.add(new ImportProperty(captionPropertyField, reflectionLM.captionProperty.getMapping(keyProperty)));
        properties.add(new ImportProperty(loggablePropertyField, reflectionLM.loggableProperty.getMapping(keyProperty)));
        properties.add(new ImportProperty(storedPropertyField, reflectionLM.storedProperty.getMapping(keyProperty)));
        properties.add(new ImportProperty(isSetNotNullPropertyField, reflectionLM.isSetNotNullProperty.getMapping(keyProperty)));
        properties.add(new ImportProperty(signaturePropertyField, reflectionLM.signatureProperty.getMapping(keyProperty)));
        properties.add(new ImportProperty(returnPropertyField, reflectionLM.returnProperty.getMapping(keyProperty)));
        properties.add(new ImportProperty(classPropertyField, reflectionLM.classProperty.getMapping(keyProperty)));

        List<ImportDelete> deletes = new ArrayList<ImportDelete>();
        deletes.add(new ImportDelete(keyProperty, LM.is(reflectionLM.property).getMapping(keyProperty), false));

        ImportTable table = new ImportTable(asList(sidPropertyField, captionPropertyField, loggablePropertyField, storedPropertyField, isSetNotNullPropertyField, signaturePropertyField, returnPropertyField, classPropertyField), dataProperty);

        try {
            DataSession session = createSession();
            session.pushVolatileStats();

            IntegrationService service = new IntegrationService(session, table, asList(keyProperty), properties, deletes);
            service.synchronize(true, false);

            session.popVolatileStats();
            session.apply(this);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void synchronizePropertyParents() {
        ImportField sidPropertyField = new ImportField(reflectionLM.propertySIDValueClass);
        ImportField numberPropertyField = new ImportField(reflectionLM.numberProperty);
        ImportField parentSidField = new ImportField(reflectionLM.navigatorElementSIDClass);

        List<List<Object>> dataParent = new ArrayList<List<Object>>();
        for (Property property : getOrderProperties()) {
            if (needsToBeSynchronized(property))
                dataParent.add(asList(property.getSID(), (Object) property.getParent().getSID(), getNumberInListOfChildren(property)));
        }

        ImportKey<?> keyProperty = new ImportKey(reflectionLM.property, reflectionLM.propertySID.getMapping(sidPropertyField));
        ImportKey<?> keyParent = new ImportKey(reflectionLM.abstractGroup, reflectionLM.abstractGroupSID.getMapping(parentSidField));
        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();

        properties.add(new ImportProperty(parentSidField, reflectionLM.parentProperty.getMapping(keyProperty), LM.object(reflectionLM.abstractGroup).getMapping(keyParent)));
        properties.add(new ImportProperty(numberPropertyField, reflectionLM.numberProperty.getMapping(keyProperty)));
        ImportTable table = new ImportTable(asList(sidPropertyField, parentSidField, numberPropertyField), dataParent);

        try {
            DataSession session = createSession();
            session.pushVolatileStats();

            IntegrationService service = new IntegrationService(session, table, asList(keyProperty, keyParent), properties);
            service.synchronize(true, false);

            session.popVolatileStats();
            session.apply(this);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void synchronizeGroupProperties() {
        ImportField sidField = new ImportField(reflectionLM.navigatorElementSIDClass);
        ImportField captionField = new ImportField(reflectionLM.navigatorElementCaptionClass);
        ImportField numberField = new ImportField(reflectionLM.numberAbstractGroup);

        ImportKey<?> key = new ImportKey(reflectionLM.abstractGroup, reflectionLM.abstractGroupSID.getMapping(sidField));

        List<List<Object>> data = new ArrayList<List<Object>>();

        for (AbstractGroup group : getParentGroups()) {
            data.add(asList(group.getSID(), (Object) group.caption));
        }

        List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();
        props.add(new ImportProperty(sidField, reflectionLM.SIDAbstractGroup.getMapping(key)));
        props.add(new ImportProperty(captionField, reflectionLM.captionAbstractGroup.getMapping(key)));

        List<ImportDelete> deletes = new ArrayList<ImportDelete>();
        deletes.add(new ImportDelete(key, LM.is(reflectionLM.abstractGroup).getMapping(key), false));

        ImportTable table = new ImportTable(asList(sidField, captionField), data);

        List<List<Object>> data2 = new ArrayList<List<Object>>();

        for (AbstractGroup group : getParentGroups()) {
            if (group.getParent() != null) {
                data2.add(asList(group.getSID(), (Object) group.getParent().getSID(), getNumberInListOfChildren(group)));
            }
        }

        ImportField parentSidField = new ImportField(reflectionLM.navigatorElementSIDClass);
        ImportKey<?> key2 = new ImportKey(reflectionLM.abstractGroup, reflectionLM.abstractGroupSID.getMapping(parentSidField));
        List<ImportProperty<?>> props2 = new ArrayList<ImportProperty<?>>();
        props2.add(new ImportProperty(parentSidField, reflectionLM.parentAbstractGroup.getMapping(key), LM.object(reflectionLM.abstractGroup).getMapping(key2)));
        props2.add(new ImportProperty(numberField, reflectionLM.numberAbstractGroup.getMapping(key)));
        ImportTable table2 = new ImportTable(asList(sidField, parentSidField, numberField), data2);

        try {
            DataSession session = createSession();
            session.pushVolatileStats();

            IntegrationService service = new IntegrationService(session, table, asList(key), props, deletes);
            service.synchronize(true, false);

            service = new IntegrationService(session, table2, asList(key, key2), props2);
            service.synchronize(true, false);

            session.popVolatileStats();
            session.apply(this);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    Integer getNumberInListOfChildren(AbstractNode abstractNode) {
        Set<AbstractNode> siblings = abstractNode.getParent().children;
        if(abstractNode instanceof Property && siblings.size() > 20) // оптимизация
            return abstractNode.getParent().getIndexedPropChildren().get(((Property) abstractNode).getSID());

        int counter = 0;
        for (AbstractNode node : siblings) {
            counter++;
            if (abstractNode instanceof Property) {
                if (node instanceof Property)
                    if (((Property) node).getSID().equals(((Property) abstractNode).getSID())) {
                        return counter;
                    }
            } else {
                if (node instanceof AbstractGroup)
                    if (((AbstractGroup) node).getSID().equals(((AbstractGroup) abstractNode).getSID())) {
                        return counter;
                    }
            }
        }
        return 0;
    }

    private final String navigatorTreeFilePath = "conf/navigatorTree.data";

    private void reloadNavigatorTree() throws IOException {
        if (new File(navigatorTreeFilePath).exists()) {
            FileInputStream inStream = new FileInputStream(navigatorTreeFilePath);
            try {
                mergeNavigatorTree(new DataInputStream(inStream));
            } finally {
                inStream.close();
            }
        }
    }

    public void mergeNavigatorTree(DataInputStream inStream) throws IOException {
        //читаем новую структуру навигатора, в процессе подчитывая сохранённые элементы
        Map<String, List<String>> treeStructure = new HashMap<String, List<String>>();
        int mapSize = inStream.readInt();
        for (int i = 0; i < mapSize; ++i) {
            String parentSID = inStream.readUTF();
            int childrenCnt = inStream.readInt();
            List<String> childrenSIDs = new ArrayList<String>();
            for (int j = 0; j < childrenCnt; ++j) {
                String childSID = inStream.readUTF();
                childrenSIDs.add(childSID);
            }
            treeStructure.put(parentSID, childrenSIDs);
        }

        //формируем полное дерево, сохраняя мэппинг элементов
        Map<String, NavigatorElement<?>> elementsMap = new HashMap<String, NavigatorElement<?>>();
        for (NavigatorElement<?> parent : LM.root.getChildren(true)) {
            String parentSID = parent.getSID();
            elementsMap.put(parentSID, parent);

            if (!treeStructure.containsKey(parentSID)) {
                List<String> children = new ArrayList<String>();
                for (NavigatorElement<?> child : parent.getChildren(false)) {
                    children.add(child.getSID());
                }

                treeStructure.put(parentSID, children);
            }
        }

        //override элементов
        for (Map.Entry<String, List<String>> entry : treeStructure.entrySet()) {
            overrideElement(elementsMap, entry.getKey());
            for (String childSID : entry.getValue()) {
                overrideElement(elementsMap, childSID);
            }
        }

        //перестраиваем
        for (Map.Entry<String, List<String>> entry : treeStructure.entrySet()) {
            String parentSID = entry.getKey();
            NavigatorElement parent = elementsMap.get(parentSID);
            if (parent != null) {
                parent.clear();

                for (String childSID : entry.getValue()) {
                    NavigatorElement<?> element = elementsMap.get(childSID);

                    if (element != null) {
                        parent.add(element);
                    }
                }
            }
        }
    }

    private void overrideElement(Map<String, NavigatorElement<?>> elementsMap, String elementSID) {
        NavigatorElement<T> element = getOverridenElement(elementSID);
        if (element == null) {
            element = getOverridenForm(elementSID);
        }

        if (element != null) {
            elementsMap.put(elementSID, element);
        }
    }

    public String getFormSerializationPath(String formSID) {
        return "conf/forms/" + formSID;
    }

    private FormEntity<T> getOverridenForm(String formSID) {
        try {
            byte[] formState = IOUtils.getFileBytes(new File(getFormSerializationPath(formSID)));
            return (FormEntity<T>) FormEntity.deserialize(this, formState);
        } catch (Exception e) {
            return null;
        }
    }

    public String getElementSerializationPath(String elementSID) {
        return "conf/elements/" + elementSID;
    }

    private NavigatorElement<T> getOverridenElement(String elementSID) {
        try {
            byte[] elementState = IOUtils.getFileBytes(new File(getElementSerializationPath(elementSID)));
            return (NavigatorElement<T>) NavigatorElement.deserialize(elementState);
        } catch (Exception e) {
            return null;
        }
    }

    public void saveNavigatorTree() throws IOException {
        Collection<NavigatorElement<T>> children = LM.root.getChildren(true);
        File treeFile = new File(navigatorTreeFilePath);
        if (!treeFile.getParentFile().exists()) {
            treeFile.getParentFile().mkdirs();
        }

        FileOutputStream fileOutStream = new FileOutputStream(treeFile);
        try {
            DataOutputStream outStream = new DataOutputStream(fileOutStream);
            outStream.writeInt(children.size());
            for (NavigatorElement child : children) {
                outStream.writeUTF(child.getSID());

                Collection<NavigatorElement<T>> thisChildren = child.getChildren(false);
                outStream.writeInt(thisChildren.size());
                for (NavigatorElement<T> thisChild : thisChildren) {
                    outStream.writeUTF(thisChild.getSID());
                }
            }
        } finally {
            fileOutStream.close();
        }
    }

    private void changeCurrentDate() throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        DataSession session = createSession();

        java.sql.Date currentDate = (java.sql.Date) LM.currentDate.read(session);
        java.sql.Date newDate = DateConverter.getCurrentDate();
        if (currentDate == null || currentDate.getDay() != newDate.getDay() || currentDate.getMonth() != newDate.getMonth() || currentDate.getYear() != newDate.getYear()) {
            LM.currentDate.change(newDate, session);
            session.apply(this);
        }

        session.close();
    }

    public int systemUserObject;
    public int systemComputer;

    public PolicyManager policyManager = new PolicyManager();

    protected abstract void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException;

    public SQLSession createSQL() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        return createSQL(-1);
    }

    public SQLSession createSQL(int isolationLevel) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        return new SQLSession(adapter, isolationLevel);
    }

    public DataSession createSession() throws SQLException {
        return createSession(getThreadLocalSql(), new UserController() {
                    public void changeCurrentUser(DataObject user) {
                        throw new RuntimeException("not supported");
                    }

                    public DataObject getCurrentUser() {
                        return new DataObject(systemUserObject, LM.systemUser);
                    }
                }, new ComputerController() {
            public DataObject getCurrentComputer() {
                return new DataObject(systemComputer, LM.computer);
            }

            public boolean isFullClient() {
                return false;
            }
        }
        );
    }

    public DataSession createSession(SQLSession sql, UserController userController, ComputerController computerController) throws SQLException {
        return new DataSession(sql, userController, computerController,
                new IsServerRestartingController() {
                    public boolean isServerRestarting() {
                        return restartController.isPendingRestart();
                    }
                },
                LM.baseClass, systemEventsLM.session, systemEventsLM.currentSession, getIDSql(), getSessionEvents());
    }

    public ImOrderSet<Property> getOrderProperties() {
        return LM.rootGroup.getProperties();
    }
    public ImSet<Property> getProperties() {
        return getOrderProperties().getSet();
    }

    public List<AbstractGroup> getParentGroups() {
        return LM.rootGroup.getParentGroups();
    }

    public ImOrderSet<Property> getPropertyList() {
        return getPropertyList(false);
    }

    // находит свойство входящее в "верхнюю" сильносвязную компоненту
    private static HSet<Link> goDown(Property<?> property, MAddMap<Property, HSet<Link>> linksMap, List<Property> order, HSet<Link> removedLinks, boolean include, HSet<Property> component) {
        HSet<Link> linksIn = linksMap.get(property);
        if (linksIn == null) { // уже были, linksMap - одновременно используется и как пометки, и как список, и как обратный обход
            linksIn = new HSet<Link>();
            linksMap.add(property, linksIn);

            ImSet<Link> links = property.getLinks();
            for (int i = 0,size = links.size(); i < size; i++) {
                Link link = links.get(i);
                if (!removedLinks.contains(link) && component.contains(link.to) == include)
                    goDown(link.to, linksMap, order, removedLinks, include, component).add(link);
            }
            order.add(property);
        }
        return linksIn;
    }

    // бежим вниз отсекая выбирая ребро с минимальным приоритетом из этой компоненты
    private static void goUp(Property<?> property, MAddMap<Property, HSet<Link>> linksMap, HSet<Property> proceeded, Result<Link> minLink, HSet<Property> component) {
        if (component.add(property))
            return;

        HSet<Link> linksIn = linksMap.get(property);
        for (int i = 0; i < linksIn.size; i++) {
            Link link = linksIn.get(i);
            if (!proceeded.contains(link.from)) { // если не в верхней компоненте
                goUp(link.from, linksMap, proceeded, minLink, component);
                if (minLink.result == null || link.type.getNum() > minLink.result.type.getNum()) // сразу же ищем минимум из ребер
                    minLink.set(link);
            }
        }
    }

    // upComponent нужен так как изначально неизвестны все элементы
    private static HSet<Property> buildList(HSet<Property> props, HSet<Property> exclude, HSet<Link> removedLinks, MOrderExclSet<Property> mResult) {
        HSet<Property> proceeded;

        List<Property> order = new ArrayList<Property>();
        MAddMap<Property, HSet<Link>> linksMap = MapFact.mAddOverrideMap();
        for (int i = 0; i < props.size; i++) {
            Property property = props.get(i);
            if (linksMap.get(property) == null) // проверка что не было
                goDown(property, linksMap, order, removedLinks, exclude == null, exclude != null ? exclude : props);
        }

        Result<Link> minLink = new Result<Link>();
        proceeded = new HSet<Property>();
        for (int i = 0; i < order.size(); i++) { // тут нужн
            Property orderProperty = order.get(order.size() - 1 - i);
            if (!proceeded.contains(orderProperty)) {
                minLink.set(null);
                HSet<Property> innerComponent = new HSet<Property>();
                goUp(orderProperty, linksMap, proceeded, minLink, innerComponent);
                assert innerComponent.size > 0;
                if (innerComponent.size == 1) // если цикла нет все ОК
                    mResult.exclAdd(innerComponent.single());
                else { // нашли цикл
                    removedLinks.exclAdd(minLink.result);

                    if (minLink.result.type.equals(LinkType.DEPEND)) { // нашли сильный цикл
                        MOrderExclSet<Property> mCycle = SetFact.mOrderExclSet();
                        buildList(innerComponent, null, removedLinks, mCycle);
                        ImOrderSet<Property> cycle = mCycle.immutableOrder();

                        String print = "";
                        for (Property property : cycle)
                            print = (print.length() == 0 ? "" : print + " -> ") + property.toString();
                        throw new RuntimeException(ServerResourceBundle.getString("message.cycle.detected") + " : " + print + " -> " + minLink.result.to);
                    }
                    buildList(innerComponent, null, removedLinks, mResult);
                }
                proceeded.exclAddAll(innerComponent);
            }
        }

        return proceeded;
    }

    private static boolean findDependency(Property<?> property, Property<?> with, HSet<Property> proceeded, Stack<Link> path) {
        if (property.equals(with))
            return true;

        if (proceeded.add(property))
            return false;

        for (Link link : property.getLinks()) {
            path.push(link);
            if (findDependency(link.to, with, proceeded, path))
                return true;
            path.pop();
        }

        return false;
    }

    private static String outDependency(String direction, Property property, Stack<Link> path) {
        String result = direction + " : " + property;
        for (Link link : path)
            result += " " + link.type + " " + link.to;
        return result;
    }

    private static String findDependency(Property<?> property1, Property<?> property2) {
        String result = "";

        Stack<Link> forward = new Stack<Link>();
        if (findDependency(property1, property2, new HSet<Property>(), forward))
            result += outDependency("FORWARD", property1, forward) + '\n';

        Stack<Link> backward = new Stack<Link>();
        if (findDependency(property2, property1, new HSet<Property>(), backward))
            result += outDependency("BACKWARD", property2, backward) + '\n';

        if (result.isEmpty())
            result += "NO DEPENDENCY " + property1 + " " + property2 + '\n';

        return result;
    }

    public boolean backupDB(String binPath, String dumpDir) throws IOException, InterruptedException {
        return adapter.backupDB(binPath, dumpDir);
    }

    public void analyzeDB(SQLSession session) throws SQLException {
        session.executeDDL(adapter.getAnalyze());
    }

    private void showDependencies() {
        String show = "";
        for (Property property : getOrderProperties())
            if (property instanceof ActionProperty && ((ActionProperty) property).showDep != null)
                show += findDependency(property, ((ActionProperty) property).showDep);
        System.out.println(show);
    }

    @IdentityStrongLazy // глобальное очень сложное вычисление
    public ImOrderSet<Property> getPropertyList(boolean onlyCheck) {
        // жестковато тут конечно написано, но пока не сильно времени жрет

        // сначала бежим по Action'ам с cancel'ами
        HSet<Property> cancelActions = new HSet<Property>();
        HSet<Property> rest = new HSet<Property>();
        for (Property property : getOrderProperties())
            if (property instanceof ActionProperty && ((ActionProperty) property).hasFlow(ChangeFlowType.CANCEL))
                cancelActions.add(property);
            else
                rest.add(property);

        MOrderExclSet<Property> mCancelResult = SetFact.mOrderExclSet();
        HSet<Property> proceeded = buildList(cancelActions, new HSet<Property>(), new HSet<Link>(), mCancelResult);
        ImOrderSet<Property> cancelResult = mCancelResult.immutableOrder();
        if (onlyCheck)
            return cancelResult.reverseOrder();

        // потом бежим по всем остальным, за исключением proceeded
        MOrderExclSet<Property> mRestResult = SetFact.mOrderExclSet();
        HSet<Property> removed = new HSet<Property>();
        removed.addAll(rest.remove(proceeded));
        buildList(removed, proceeded, new HSet<Link>(), mRestResult); // потом этот cast уберем
        ImOrderSet<Property> restResult = mRestResult.immutableOrder();

        // затем по всем кроме proceeded на прошлом шаге
        assert cancelResult.getSet().disjoint(restResult.getSet());
        return cancelResult.reverseOrder().addOrderExcl(restResult.reverseOrder());
    }

    @IdentityLazy
    public ImOrderSet<CalcProperty> getStoredProperties() {
        return BaseUtils.immutableCast(getPropertyList().filterOrder(new SFunctionSet<Property>() {
            public boolean contains(Property property) {
                return property instanceof CalcProperty && ((CalcProperty) property).isStored();
            }}));
    }

    @IdentityLazy
    public ImOrderSet<OldProperty> getApplyEventDependProps() {
        MOrderSet<OldProperty> result = SetFact.mOrderSet();
        for (Property<?> property : getPropertyList()) {
            if (property instanceof ActionProperty && ((ActionProperty) property).onEvent(SystemEvent.APPLY))
                result.addAll(property.getOldDepends().toOrderSet());
            if (property instanceof DataProperty && ((DataProperty) property).event != null)
                result.addAll(((DataProperty) property).event.getOldDepends().toOrderSet());
        }
        return result.immutableOrder();
    }

    @IdentityLazy
    public List<Object> getAppliedProperties(boolean onlyCheck) {
        // здесь нужно вернуть список stored или тех кто
        List<Object> result = new ArrayList<Object>();
        for (Property property : getPropertyList(onlyCheck)) {
            if (property instanceof CalcProperty && ((CalcProperty) property).isStored())
                result.add(property);
            if (property instanceof ActionProperty && ((ActionProperty) property).onEvent(SystemEvent.APPLY))
                result.add(new ActionPropertyValueImplement((ActionProperty) property));
        }
        return result;
    }

    @IdentityLazy
    public List<CalcProperty> getDataChangeEvents() {
        List<CalcProperty> result = new ArrayList<CalcProperty>();
        for (Property property : getPropertyList())
            if (property instanceof DataProperty && ((DataProperty) property).event != null)
                result.add((((DataProperty) property).event).getWhere());
        return result;
    }

    private void fillAppliedDependFrom(CalcProperty<?> fill, CalcProperty<?> applied, Map<CalcProperty, MSet<CalcProperty>> mapDepends) {
        if (!fill.equals(applied) && fill.isStored())
            mapDepends.get(fill).add(applied);
        else
            for (CalcProperty depend : fill.getDepends(false)) // derived'ы отдельно отрабатываются
                fillAppliedDependFrom(depend, applied, mapDepends);
    }

    @IdentityLazy
    public List<ActionProperty> getSessionEvents() {
        List<ActionProperty> result = new ArrayList<ActionProperty>();
        for (Property property : getPropertyList())
            if (property instanceof ActionProperty && ((ActionProperty) property).onEvent(SystemEvent.SESSION))
                result.add((ActionProperty) property);
        return result;
    }

    // assert что key property is stored, а value property is stored или instanceof OldProperty
    @IdentityStrongLazy // глобальное очень сложное вычисление
    private ImMap<CalcProperty, ImOrderSet<CalcProperty>> getMapAppliedDepends() {
        Map<CalcProperty, MSet<CalcProperty>> mapDepends = new HashMap<CalcProperty, MSet<CalcProperty>>();
        for (CalcProperty property : getStoredProperties()) {
            mapDepends.put(property, SetFact.<CalcProperty>mSet());
            fillAppliedDependFrom(property, property, mapDepends);
        }
        for (OldProperty old : getApplyEventDependProps())
            fillAppliedDependFrom(old.property, old, mapDepends);

        ImRevMap<Property, Integer> indexMap = getPropertyList().mapOrderRevValues(new GetIndex<Integer>() {
            public Integer getMapValue(int i) {
                return i;
            }
        });
        MExclMap<CalcProperty, ImOrderSet<CalcProperty>> mOrderedMapDepends = MapFact.mExclMap(mapDepends.size());
        for (Map.Entry<CalcProperty, MSet<CalcProperty>> mapDepend : mapDepends.entrySet()) {
            ImSet<CalcProperty> depends = mapDepend.getValue().immutable();
            ImOrderSet<CalcProperty> dependList = indexMap.filterInclRev(depends).reverse().sort().valuesList().toOrderExclSet();
            assert dependList.size() == depends.size();
            mOrderedMapDepends.exclAdd(mapDepend.getKey(), dependList);
        }
        return mOrderedMapDepends.immutable();
    }

    // определяет для stored свойства зависимые от него stored свойства, а также свойства которым необходимо хранить изменения с начала транзакции (constraints и derived'ы)
    public ImOrderSet<CalcProperty> getAppliedDependFrom(CalcProperty property) {
        assert property.isStored();
        return getMapAppliedDepends().get(property);
    }

    public List<AggregateProperty> getAggregateStoredProperties() {
        List<AggregateProperty> result = new ArrayList<AggregateProperty>();
        for (Property property : getStoredProperties())
            if (property instanceof AggregateProperty)
                result.add((AggregateProperty) property);
        return result;
    }

    @IdentityLazy
    public List<CalcProperty> getCheckConstrainedProperties() {
        List<CalcProperty> result = new ArrayList<CalcProperty>();
        for (Property property : getPropertyList()) {
            if (property instanceof CalcProperty && ((CalcProperty) property).checkChange != CalcProperty.CheckType.CHECK_NO) {
                result.add((CalcProperty) property);
            }
        }
        return result;
    }

    public List<CalcProperty> getCheckConstrainedProperties(CalcProperty<?> changingProp) {
        List<CalcProperty> result = new ArrayList<CalcProperty>();
        for (CalcProperty property : getCheckConstrainedProperties()) {
            if (property.checkChange == CalcProperty.CheckType.CHECK_ALL ||
                    property.checkChange == CalcProperty.CheckType.CHECK_SOME && property.checkProperties.contains(changingProp)) {
                result.add(property);
            }
        }
        return result;
    }

    public void fillIDs(Map<String, String> sIDChanges) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        DataSession session = createSession();

        LM.baseClass.fillIDs(session, LM.name, LM.classSID, sIDChanges);

        session.apply(this);

        session.close();
    }

    public void updateClassStat(SQLSession session) throws SQLException {
        LM.baseClass.updateClassStat(session);
    }

    public List<LogicsModule> getLogicModules() {
        return logicModules;
    }

    public static class SIDChange {
        public String oldSID;
        public String newSID;

        public SIDChange(String oldSID, String newSID) {
            this.oldSID = oldSID;
            this.newSID = newSID;
        }
    }

    public static class DBVersion {
        private List<Integer> version;

        public DBVersion(String version) {
            this.version = versionToList(version);
        }

        public static List<Integer> versionToList(String version) {
            String[] splittedArr = version.split("\\.");
            List<Integer> intVersion = new ArrayList<Integer>();
            for (String part : splittedArr) {
                intVersion.add(Integer.parseInt(part));
            }
            return intVersion;
        }

        public int compare(DBVersion rhs) {
            return compareVersions(version, rhs.version);
        }

        public static int compareVersions(List<Integer> lhs, List<Integer> rhs) {
            for (int i = 0; i < Math.max(lhs.size(), rhs.size()); i++) {
                int left = (i < lhs.size() ? lhs.get(i) : 0);
                int right = (i < rhs.size() ? rhs.get(i) : 0);
                if (left < right) return -1;
                if (left > right) return 1;
            }
            return 0;
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < version.size(); i++) {
                if (i > 0) {
                    buf.append(".");
                }
                buf.append(version.get(i));
            }
            return buf.toString();
        }
    }

    class DBVersionComparator implements Comparator<DBVersion> {
        @Override
        public int compare(DBVersion lhs, DBVersion rhs) {
            return lhs.compare(rhs);
        }

        @Override
        public boolean equals(Object o) {
            return true;
        }
    }

    private TreeMap<DBVersion, List<SIDChange>> propertySIDChanges = new TreeMap<DBVersion, List<SIDChange>>(new DBVersionComparator());
    private TreeMap<DBVersion, List<SIDChange>> classSIDChanges = new TreeMap<DBVersion, List<SIDChange>>(new DBVersionComparator());
    private TreeMap<DBVersion, List<SIDChange>> tableSIDChanges = new TreeMap<DBVersion, List<SIDChange>>(new DBVersionComparator());

    private void addSIDChange(TreeMap<DBVersion, List<SIDChange>> sidChanges, String version, String oldSID, String newSID) {
        DBVersion dbVersion = new DBVersion(version);
        if (!sidChanges.containsKey(dbVersion)) {
            sidChanges.put(dbVersion, new ArrayList<SIDChange>());
        }
        sidChanges.get(dbVersion).add(new SIDChange(oldSID, newSID));
    }

    public void addPropertySIDChange(String version, String oldSID, String newSID) {
        addSIDChange(propertySIDChanges, version, oldSID, newSID);
    }

    public void addClassSIDChange(String version, String oldSID, String newSID) {
        addSIDChange(classSIDChanges, version, oldSID, newSID);
    }

    public void addTableSIDChange(String version, String oldSID, String newSID) {
        addSIDChange(tableSIDChanges, version, oldSID, newSID);
    }

    public Map<String, String> getChangesAfter(DBVersion versionAfter, TreeMap<DBVersion, List<SIDChange>> allChanges) {
        Map<String, String> resultChanges = new HashMap<String, String>();

        for (Map.Entry<DBVersion, List<SIDChange>> changesEntry : allChanges.entrySet()) {
            if (changesEntry.getKey().compare(versionAfter) > 0) {
                List<SIDChange> versionChanges = changesEntry.getValue();
                Map<String, String> versionChangesMap = new HashMap<String, String>();

                for (SIDChange change : versionChanges) {
                    if (versionChangesMap.containsKey(change.oldSID)) {
                        throw new RuntimeException(String.format("Renaming '%s' twice in version %s.", change.oldSID, changesEntry.getKey()));
                    } else if (resultChanges.containsKey(change.oldSID)) {
                        throw new RuntimeException(String.format("Renaming '%s' twice.", change.oldSID)); // todo [dale]: временно
                    }
                    versionChangesMap.put(change.oldSID, change.newSID);
                }

                for (Map.Entry<String, String> currentChanges : resultChanges.entrySet()) {
                    String renameTo = currentChanges.getValue();
                    if (versionChangesMap.containsKey(renameTo)) {
                        currentChanges.setValue(versionChangesMap.get(renameTo));
                        versionChangesMap.remove(renameTo);
                    }
                }

                resultChanges.putAll(versionChangesMap);

                Set<String> renameToSIDs = new HashSet<String>();
                for (String renameTo : resultChanges.values()) {
                    if (renameToSIDs.contains(renameTo)) {
                        throw new RuntimeException(String.format("Renaming to '%s' twice.", renameTo));
                    }
                    renameToSIDs.add(renameTo);
                }
            }
        }
        return resultChanges;
    }

    private void runAlterationScript() {
        try {
            InputStream scriptStream = getClass().getResourceAsStream("/migration.script");
            if (scriptStream != null) {
                ANTLRInputStream stream = new ANTLRInputStream(scriptStream);
                AlterationScriptLexer lexer = new AlterationScriptLexer(stream);
                AlterationScriptParser parser = new AlterationScriptParser(new CommonTokenStream(lexer));

                parser.self = this;

                parser.script();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void alterateDBStructure(DBStructure data, SQLSession sql) throws SQLException {
        Map<String, String> propertyChanges = getChangesAfter(data.dbVersion, propertySIDChanges);
        Map<String, String> tableChanges = getChangesAfter(data.dbVersion, tableSIDChanges);

        for (DBStoredProperty oldProperty : data.storedProperties) {
            if (propertyChanges.containsKey(oldProperty.sID)) {
                String newSID = propertyChanges.get(oldProperty.sID);
                sql.renameColumn(oldProperty.tableName, oldProperty.sID, newSID);
                oldProperty.sID = newSID;
            }

            if (tableChanges.containsKey(oldProperty.tableName)) {
                oldProperty.tableName = tableChanges.get(oldProperty.tableName);
            }
        }

        for (Table table : data.tables.keySet()) {
            for (PropertyField field : table.properties) {
                if (propertyChanges.containsKey(field.name)) {
                    field.name = propertyChanges.get(field.name);
                }
            }

            if (tableChanges.containsKey(table.name)) {
                String newSID = tableChanges.get(table.name);
                sql.renameTable(table.name, newSID);
                table.name = newSID;
            }
        }
    }

    private class DBStoredProperty {
        public String sID;
        public Boolean isDataProperty;
        public String tableName;
        public ImMap<Integer, KeyField> mapKeys;
        public CalcProperty<?> property = null;

        public DBStoredProperty(CalcProperty<?> property) {
            this.sID = property.getSID();
            this.isDataProperty = property instanceof DataProperty;
            this.tableName = property.mapTable.table.name;
            mapKeys = ((CalcProperty<PropertyInterface>)property).mapTable.mapKeys.mapKeys(new GetValue<Integer, PropertyInterface>() {
                public Integer getMapValue(PropertyInterface value) {
                    return value.ID;
                }});
            this.property = property;
        }

        public DBStoredProperty(String sID, Boolean isDataProperty, String tableName, ImMap<Integer, KeyField> mapKeys) {
            this.sID = sID;
            this.isDataProperty = isDataProperty;
            this.tableName = tableName;
            this.mapKeys = mapKeys;
        }
    }

    private class DBStructure {
        public int version;
        public DBVersion dbVersion;
        public Map<Table, Map<List<String>, Boolean>> tables = new HashMap<Table, Map<List<String>, Boolean>>();
        public List<DBStoredProperty> storedProperties = new ArrayList<DBStoredProperty>();

        public DBStructure(DBVersion dbVersion) {
            version = 3;
            this.dbVersion = dbVersion;

            for (Table table : LM.tableFactory.getImplementTablesMap().valueIt()) {
                tables.put(table, new HashMap<List<String>, Boolean>());
            }

            for (Map.Entry<List<? extends CalcProperty>, Boolean> index : LM.indexes.entrySet()) {
                Iterator<? extends CalcProperty> i = index.getKey().iterator();
                if (!i.hasNext())
                    throw new RuntimeException(getString("logics.policy.forbidden.to.create.empty.indexes"));
                CalcProperty baseProperty = i.next();
                if (!baseProperty.isStored())
                    throw new RuntimeException(getString("logics.policy.forbidden.to.create.indexes.on.non.regular.properties") + " (" + baseProperty + ")");

                ImplementTable indexTable = baseProperty.mapTable.table;

                List<String> tableIndex = new ArrayList<String>();
                tableIndex.add(baseProperty.field.name);

                while (i.hasNext()) {
                    CalcProperty property = i.next();
                    if (!property.isStored())
                        throw new RuntimeException(getString("logics.policy.forbidden.to.create.indexes.on.non.regular.properties") + " (" + baseProperty + ")");
                    if (indexTable.findProperty(property.field.name) == null)
                        throw new RuntimeException(getString("logics.policy.forbidden.to.create.indexes.on.properties.in.different.tables", baseProperty, property));
                    tableIndex.add(property.field.name);
                }
                tables.get(indexTable).put(tableIndex, index.getValue());
            }

            for (CalcProperty<?> property : getStoredProperties()) {
                storedProperties.add(new DBStoredProperty(property));
            }
        }

        public DBStructure(DataInputStream inputDB) throws IOException {
            if (inputDB == null) {
                version = -2;
                dbVersion = new DBVersion("0.0");
            } else {
                version = inputDB.read() - 'v';
                if (version < 0) {
                    inputDB.reset();
                }

                if (version > 2) {
                    dbVersion = new DBVersion(inputDB.readUTF());
                } else {
                    dbVersion = new DBVersion("0.0");
                }

                for (int i = inputDB.readInt(); i > 0; i--) {
                    SerializedTable prevTable = new SerializedTable(inputDB, LM.baseClass, version);
                    Map<List<String>, Boolean> indices = new HashMap<List<String>, Boolean>();
                    for (int j = inputDB.readInt(); j > 0; j--) {
                        List<String> index = new ArrayList<String>();
                        for (int k = inputDB.readInt(); k > 0; k--) {
                            index.add(inputDB.readUTF());
                        }
                        boolean prevOrdered = version >= 1 && inputDB.readBoolean();
                        indices.put(index, prevOrdered);
                    }
                    tables.put(prevTable, indices);
                }

                int prevStoredNum = inputDB.readInt();
                for (int i = 0; i < prevStoredNum; i++) {
                    String sID = inputDB.readUTF();
                    boolean isDataProperty = true;
                    if (version >= 0) {
                        isDataProperty = inputDB.readBoolean();
                    }
                    String tableName = inputDB.readUTF();
                    Table prevTable = getTable(tableName);
                    MExclMap<Integer, KeyField> mMapKeys = MapFact.mExclMap(prevTable.getTableKeys().size());
                    for (int j = 0; j < prevTable.getTableKeys().size(); j++) {
                        mMapKeys.exclAdd(inputDB.readInt(), prevTable.findKey(inputDB.readUTF()));
                    }
                    storedProperties.add(new DBStoredProperty(sID, isDataProperty, tableName, mMapKeys.immutable()));
                }
            }
        }

        public Table getTable(String name) {
            for (Table table : tables.keySet()) {
                if (table.name.equals(name)) {
                    return table;
                }
            }
            return null;
        }

        public void write(DataOutputStream outDB) throws IOException {
            outDB.write('v' + version);  //для поддержки обратной совместимости
            if (version > 2) {
                outDB.writeUTF(dbVersion.toString());
            }

            outDB.writeInt(tables.size());
            for (Map.Entry<Table, Map<List<String>, Boolean>> tableIndices : tables.entrySet()) {
                tableIndices.getKey().serialize(outDB);
                outDB.writeInt(tableIndices.getValue().size());
                for (Map.Entry<List<String>, Boolean> index : tableIndices.getValue().entrySet()) {
                    outDB.writeInt(index.getKey().size());
                    for (String indexField : index.getKey()) {
                        outDB.writeUTF(indexField);
                    }
                    outDB.writeBoolean(index.getValue());
                }
            }

            outDB.writeInt(storedProperties.size());
            for (DBStoredProperty property : storedProperties) {
                outDB.writeUTF(property.sID);
                outDB.writeBoolean(property.isDataProperty);
                outDB.writeUTF(property.tableName);
                for (int i=0,size=property.mapKeys.size();i<size;i++) {
                    outDB.writeInt(property.mapKeys.getKey(i));
                    outDB.writeUTF(property.mapKeys.getValue(i).name);
                }
            }
        }
    }

    private DBVersion getCurrentDBVersion(DBVersion oldVersion) {
        DBVersion curVersion = oldVersion;
        if (!propertySIDChanges.isEmpty() && curVersion.compare(propertySIDChanges.lastKey()) < 0) {
            curVersion = propertySIDChanges.lastKey();
        }
        if (!classSIDChanges.isEmpty() && curVersion.compare(classSIDChanges.lastKey()) < 0) {
            curVersion = classSIDChanges.lastKey();
        }
        if (!tableSIDChanges.isEmpty() && curVersion.compare(tableSIDChanges.lastKey()) < 0) {
            curVersion = tableSIDChanges.lastKey();
        }
        return curVersion;
    }

    public void synchronizeDB() throws SQLException, IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {

        SQLSession sql = getThreadLocalSql();

        // инициализируем таблицы
        LM.tableFactory.fillDB(sql, LM.baseClass);

        // "старое" состояние базы
        DataInputStream inputDB = null;
        byte[] struct = (byte[]) sql.readRecord(StructTable.instance, MapFact.<KeyField, DataObject>EMPTY(), StructTable.instance.struct);
        if (struct != null)
            inputDB = new DataInputStream(new ByteArrayInputStream(struct));

        DBStructure oldDBStructure = new DBStructure(inputDB);

        runAlterationScript();

        sql.startTransaction();

        // новое состояние базы
        ByteArrayOutputStream outDBStruct = new ByteArrayOutputStream();
        DataOutputStream outDB = new DataOutputStream(outDBStruct);

        DBVersion newDBVersion = getCurrentDBVersion(oldDBStructure.dbVersion);
        DBStructure newDBStructure = new DBStructure(newDBVersion);
        // запишем новое состояние таблиц (чтобы потом изменять можно было бы)
        newDBStructure.write(outDB);

        for (Map.Entry<Table, Map<List<String>, Boolean>> oldTableIndices : oldDBStructure.tables.entrySet()) {
            Table oldTable = oldTableIndices.getKey();
            Table newTable = newDBStructure.getTable(oldTableIndices.getKey().name);
            Map<List<String>, Boolean> newTableIndices = newDBStructure.tables.get(newTable);

            for (Map.Entry<List<String>, Boolean> oldIndex : oldTableIndices.getValue().entrySet()) {
                List<String> oldIndexKeys = oldIndex.getKey();
                boolean oldOrder = oldIndex.getValue();
                boolean drop = (newTable == null); // ушла таблица
                if (!drop) {
                    Boolean newOrder = newTableIndices.get(oldIndexKeys);
                    if (newOrder != null && newOrder.equals(oldOrder)) {
                        newTableIndices.remove(oldIndexKeys); // не трогаем индекс
                    } else {
                        drop = true;
                    }
                }
                if (drop) {
//                    sql.dropIndex(oldTable.name, oldTable.keys, SetFact.fromJavaOrderSet(oldIndexKeys), oldOrder);
                }
            }
        }

        alterateDBStructure(oldDBStructure, sql);

        // добавим таблицы которых не было
        for (Table table : newDBStructure.tables.keySet()) {
            if (oldDBStructure.getTable(table.name) == null)
                sql.createTable(table.name, table.keys);
        }

        MSet<ImplementTable> mPackTables = SetFact.mSet();

        // бежим по свойствам
        Map<String, String> columnsToDrop = new HashMap<String, String>();
        for (DBStoredProperty oldProperty : oldDBStructure.storedProperties) {
            Table oldTable = oldDBStructure.getTable(oldProperty.tableName);

            boolean keep = false, moved = false;
            for (Iterator<DBStoredProperty> is = newDBStructure.storedProperties.iterator(); is.hasNext(); ) {
                DBStoredProperty newProperty = is.next();

                if (newProperty.sID.equals(oldProperty.sID)) {
                    MExclMap<KeyField, PropertyInterface> mFoundInterfaces = MapFact.mExclMapMax(newProperty.property.interfaces.size());
                    for (PropertyInterface propertyInterface : newProperty.property.interfaces) {
                        KeyField mapKeyField = oldProperty.mapKeys.get(propertyInterface.ID);
                        if (mapKeyField != null) 
                            mFoundInterfaces.exclAdd(mapKeyField, propertyInterface);
                    }
                    ImMap<KeyField, PropertyInterface> foundInterfaces = mFoundInterfaces.immutable();

                    if (foundInterfaces.size() == oldProperty.mapKeys.size()) { // если все нашли
                        if (!(keep = newProperty.tableName.equals(oldProperty.tableName))) { // если в другой таблице
                            sql.addColumn(newProperty.tableName, newProperty.property.field);
                            // делаем запрос на перенос

                            logger.info(getString("logics.info.property.is.transferred.from.table.to.table", newProperty.property.field, newProperty.property.caption, oldProperty.tableName, newProperty.tableName));
                            newProperty.property.mapTable.table.moveColumn(sql, newProperty.property.field, oldTable,
                                    foundInterfaces.join((ImMap<PropertyInterface, KeyField>) newProperty.property.mapTable.mapKeys), oldTable.findProperty(oldProperty.sID));
                            logger.info("Done");
                            moved = true;
                        } else { // надо проверить что тип не изменился
                            if (!oldTable.findProperty(oldProperty.sID).type.equals(newProperty.property.field.type))
                                sql.modifyColumn(newProperty.property.mapTable.table.name, newProperty.property.field, oldTable.findProperty(oldProperty.sID).type);
                        }
                        is.remove();
                    }
                    break;
                }
            }
            if (!keep) {
                if (oldProperty.isDataProperty && !moved) {
                    String newName = oldProperty.sID + "_deleted";
                    Savepoint savepoint = sql.getConnection().setSavepoint();
                    try {
                        savepoint = sql.getConnection().setSavepoint();
                        sql.renameColumn(oldProperty.tableName, oldProperty.sID, newName);
                        columnsToDrop.put(newName, oldProperty.tableName);
                    } catch (PSQLException e) { // колонка с новым именем (с '_deleted') уже существует
                        sql.getConnection().rollback(savepoint);
                        sql.dropColumn(oldTable.name, oldProperty.sID);
                        ImplementTable table = (ImplementTable) newDBStructure.getTable(oldTable.name);
                        if (table != null) mPackTables.add(table);
                    }
                } else {
                    sql.dropColumn(oldTable.name, oldProperty.sID);
                    ImplementTable table = (ImplementTable) newDBStructure.getTable(oldTable.name); // надо упаковать таблицу если удалили колонку
                    if (table != null) mPackTables.add(table);
                }
            }
        }

        List<AggregateProperty> recalculateProperties = new ArrayList<AggregateProperty>();
        for (DBStoredProperty property : newDBStructure.storedProperties) { // добавляем оставшиеся
            sql.addColumn(property.tableName, property.property.field);
            if (struct != null && property.property instanceof AggregateProperty) // если все свойства "новые" то ничего перерасчитывать не надо
                recalculateProperties.add((AggregateProperty) property.property);
        }

        // удаляем таблицы старые
        for (Table table : oldDBStructure.tables.keySet())
            if (newDBStructure.getTable(table.name) == null) {
                sql.dropTable(table.name);
            }

        packTables(sql, mPackTables.immutable()); // упакуем таблицы

        updateStats();  // пересчитаем статистику

        // создадим индексы в базе
        for (Map.Entry<Table, Map<List<String>, Boolean>> mapIndex : newDBStructure.tables.entrySet())
            for (Map.Entry<List<String>, Boolean> index : mapIndex.getValue().entrySet())
                sql.addIndex(mapIndex.getKey().name, mapIndex.getKey().keys, SetFact.fromJavaOrderSet(index.getKey()), index.getValue());

        try {
            sql.insertRecord(StructTable.instance, MapFact.<KeyField, DataObject>EMPTY(), MapFact.singleton(StructTable.instance.struct, (ObjectValue) new DataObject((Object) outDBStruct.toByteArray(), ByteArrayClass.instance)), true);
        } catch (Exception e) {
            ImMap<PropertyField, ObjectValue> propFields = MapFact.singleton(StructTable.instance.struct, (ObjectValue) new DataObject((Object) new byte[0], ByteArrayClass.instance));
            sql.insertRecord(StructTable.instance, MapFact.<KeyField, DataObject>EMPTY(), propFields, true);
        }

        fillIDs(getChangesAfter(oldDBStructure.dbVersion, classSIDChanges));

        updateClassStat(sql);

        recalculateAggregations(sql, recalculateProperties); // перерасчитаем агрегации
//        recalculateAggregations(sql, getAggregateStoredProperties());

        sql.commitTransaction();

        DataSession session = createSession();
        for (String sid : columnsToDrop.keySet()) {
            DataObject object = session.addObject(reflectionLM.dropColumn);
            reflectionLM.sidDropColumn.change(sid, session, object);
            reflectionLM.sidTableDropColumn.change(columnsToDrop.get(sid), session, object);
            reflectionLM.timeDropColumn.change(new Timestamp(Calendar.getInstance().getTimeInMillis()), session, object);
            reflectionLM.revisionDropColumn.change(getRevision(), session, object);
        }
        session.apply(this);
        session.close();
    }

    public void synchronizeTables() {
        ImportField tableSidField = new ImportField(reflectionLM.sidTable);
        ImportField tableKeySidField = new ImportField(reflectionLM.sidTableKey);
        ImportField tableKeyNameField = new ImportField(reflectionLM.nameTableKey);
        ImportField tableKeyClassField = new ImportField(reflectionLM.classTableKey);
        ImportField tableColumnSidField = new ImportField(reflectionLM.sidTableColumn);

        ImportKey<?> tableKey = new ImportKey(reflectionLM.table, reflectionLM.tableSID.getMapping(tableSidField));
        ImportKey<?> tableKeyKey = new ImportKey(reflectionLM.tableKey, reflectionLM.tableKeySID.getMapping(tableKeySidField));
        ImportKey<?> tableColumnKey = new ImportKey(reflectionLM.tableColumn, reflectionLM.tableColumnSID.getMapping(tableColumnSidField));

        List<List<Object>> data = new ArrayList<List<Object>>();
        for (DataTable dataTable : LM.tableFactory.getDataTables(LM.baseClass)) {
            Object tableName = dataTable.name;
            ImMap<KeyField, ValueClass> classes = dataTable.getClasses().getCommonParent(dataTable.getTableKeys());
            for (KeyField key : dataTable.keys) {
                data.add(asList(tableName, key.name, tableName + "." + key.name, classes.get(key).getCaption()));
            }
        }

        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
        properties.add(new ImportProperty(tableSidField, reflectionLM.sidTable.getMapping(tableKey)));
        properties.add(new ImportProperty(tableKeySidField, reflectionLM.sidTableKey.getMapping(tableKeyKey)));
        properties.add(new ImportProperty(tableKeyNameField, reflectionLM.nameTableKey.getMapping(tableKeyKey)));
        properties.add(new ImportProperty(tableSidField, reflectionLM.tableTableKey.getMapping(tableKeyKey), LM.object(reflectionLM.table).getMapping(tableKey)));
        properties.add(new ImportProperty(tableKeyClassField, reflectionLM.classTableKey.getMapping(tableKeyKey)));

        List<ImportDelete> deletes = new ArrayList<ImportDelete>();
        deletes.add(new ImportDelete(tableKey, LM.is(reflectionLM.table).getMapping(tableKey), false));
        deletes.add(new ImportDelete(tableKeyKey, LM.is(reflectionLM.tableKey).getMapping(tableKeyKey), false));

        ImportTable table = new ImportTable(asList(tableSidField, tableKeyNameField, tableKeySidField, tableKeyClassField), data);

        List<List<Object>> data2 = new ArrayList<List<Object>>();
        for (DataTable dataTable : LM.tableFactory.getDataTables(LM.baseClass)) {
            Object tableName = dataTable.name;
            for (PropertyField property : dataTable.properties) {
                data2.add(asList(tableName, property.name));
            }
        }

        List<ImportProperty<?>> properties2 = new ArrayList<ImportProperty<?>>();
        properties2.add(new ImportProperty(tableSidField, reflectionLM.sidTable.getMapping(tableKey)));
        properties2.add(new ImportProperty(tableColumnSidField, reflectionLM.sidTableColumn.getMapping(tableColumnKey)));
        properties2.add(new ImportProperty(tableSidField, reflectionLM.tableTableColumn.getMapping(tableColumnKey), LM.object(reflectionLM.table).getMapping(tableKey)));

        List<ImportDelete> deletes2 = new ArrayList<ImportDelete>();
        deletes2.add(new ImportDelete(tableColumnKey, LM.is(reflectionLM.tableColumn).getMapping(tableColumnKey), false));

        ImportTable table2 = new ImportTable(asList(tableSidField, tableColumnSidField), data2);

        try {
            DataSession session = createSession();
            session.pushVolatileStats();

            IntegrationService service = new IntegrationService(session, table, asList(tableKey, tableKeyKey), properties, deletes);
            service.synchronize(true, false);

            service = new IntegrationService(session, table2, asList(tableKey, tableColumnKey), properties2, deletes2);
            service.synchronize(true, false);

            session.popVolatileStats();
            session.apply(this);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void recalculateStats(DataSession session) throws SQLException {
        for (DataTable dataTable : LM.tableFactory.getDataTables(LM.baseClass)) {
            dataTable.calculateStat(this.reflectionLM, session);
        }
    }

    public void recalculateFollows(DataSession session) throws SQLException {
        for (Property property : getPropertyList())
            if (property instanceof ActionProperty) {
                ActionProperty<?> action = (ActionProperty) property;
                if (action.onEvent(SystemEvent.APPLY) && action.resolve)
                    session.resolve(action);
            }
    }

    public void updateStats() throws SQLException {
        updateStats(true); // чтобы сами таблицы статистики получили статистику
        if (!"true".equals(System.getProperty("platform.server.logics.donotcalculatestats")))
            updateStats(false);
    }

    private ImMap<String, Integer> readStatsFromDB(DataSession session, LCP sIDProp, LCP statsProp) throws SQLException {
        QueryBuilder<String, String> query = new QueryBuilder<String, String>(SetFact.toSet("key"));
        Expr sidToObject = sIDProp.getExpr(session.getModifier(), query.getMapExprs().singleValue());
        query.and(sidToObject.getWhere());
        query.addProperty("property", statsProp.getExpr(session.getModifier(), sidToObject));
        return query.execute(session).getMap().mapKeyValues(new GetValue<String, ImMap<String, Object>>() {
                    public String getMapValue(ImMap<String, Object> key) {
                        return ((String) key.singleValue()).trim();
                    }}, new GetValue<Integer, ImMap<String, Object>>() {
                    public Integer getMapValue(ImMap<String, Object> value) {
                        return (Integer)value.singleValue();
                    }});
    }
    public void updateStats(boolean statDefault) throws SQLException {
        DataSession session = createSession();

        ImMap<String, Integer> tableStats;
        ImMap<String, Integer> keyStats;
        ImMap<String, Integer> propStats;
        if(statDefault) {
            tableStats = MapFact.EMPTY();
            keyStats = MapFact.EMPTY();
            propStats = MapFact.EMPTY();
        } else {
            tableStats = readStatsFromDB(session, reflectionLM.tableSID, reflectionLM.rowsTable);
            keyStats = readStatsFromDB(session, reflectionLM.tableKeySID, reflectionLM.quantityTableKey);
            propStats = readStatsFromDB(session, reflectionLM.tableColumnSID, reflectionLM.quantityTableColumn);
        }

        for (DataTable dataTable : LM.tableFactory.getDataTables(LM.baseClass)) {
            dataTable.updateStat(tableStats, keyStats, propStats, statDefault);
        }
    }

    public void dropColumn(String tableName, String columnName) throws SQLException {
        SQLSession sql = getThreadLocalSql();
        sql.startTransaction();
        try {
            sql.dropColumn(tableName, columnName);
            ImplementTable table = LM.tableFactory.getImplementTablesMap().get(tableName); // надо упаковать таблицу, если удалили колонку
            if (table != null)
                sql.packTable(table);
            sql.commitTransaction();
        } catch(SQLException e) {
            sql.rollbackTransaction();
            throw e;
        }
    }

    protected LP getLP(String sID) {
        return LM.getLP(sID);
    }

    public LCP getLCP(String sID) {
        return (LCP) LM.getLP(sID);
    }

    protected LAP getLAP(String sID) {
        return (LAP) LM.getLP(sID);
    }

    private boolean intersect(LCP[] props) {
        for (int i = 0; i < props.length; i++)
            for (int j = i + 1; j < props.length; j++)
                if (((LCP<?>) props[i]).intersect((LCP<?>) props[j]))
                    return true;
        return false;
    }

    public final static boolean checkClasses = false;

    public boolean isRequiredPassword() {
        return requiredPassword;
    }

    public void setRequiredPassword(boolean requiredPassword) {
        this.requiredPassword = requiredPassword;
    }

    public boolean requiredPassword = true;


    private boolean checkProps() {
        if (checkClasses)
            for (Property prop : getOrderProperties()) {
                logger.debug("Checking property : " + prop + "...");
                assert prop.check();
            }
        for (LCP[] props : LM.checkCUProps) {
            logger.debug("Checking class properties : " + props + "...");
            assert !intersect(props);
        }
        for (LCP[] props : LM.checkSUProps) {
            logger.debug("Checking union properties : " + props + "...");
//            assert intersect(props);
        }
        return true;
    }

    public void fillData() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
    }

    public String checkAggregations(SQLSession session) throws SQLException {
        String message = "";
        for (AggregateProperty property : getAggregateStoredProperties())
            message += property.checkAggregation(session);
        return message;
    }

    public void recalculateAggregations(SQLSession session, List<AggregateProperty> recalculateProperties) throws SQLException {
        for (AggregateProperty property : recalculateProperties)
            property.recalculateAggregation(session);
    }

    public void recalculateAggregationTableColumn(SQLSession session, String propertySID) throws SQLException {
        for (CalcProperty property : getAggregateStoredProperties())
            if (property.getSID().equals(propertySID)) {
                AggregateProperty aggregateProperty = (AggregateProperty) property;
                aggregateProperty.recalculateAggregation(session);
            }
    }

    public void packTables(SQLSession session, ImCol<ImplementTable> tables) throws SQLException {
        for (Table table : tables) {
            logger.debug(getString("logics.info.packing.table") + " (" + table + ")... ");
            session.packTable(table);
            logger.debug("Done");
        }
    }

    public String getVMInfo() throws SQLException {
        Long freeMemory = Runtime.getRuntime().freeMemory() / 1024 / 1024;
        Long maxMemory = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        Long totalMemory = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        Integer processors = Runtime.getRuntime().availableProcessors();
        return "Processors: " + processors + "\n" +
               "Free Memory: " + freeMemory + " MB\n" +
               "Total Memory: " + totalMemory + " MB\n" +
               "Max Memory: " + maxMemory + " MB";
    }

    public void updateRestartProperty() throws SQLException {
        Boolean isRestarting = restartController.isPendingRestart() ? Boolean.TRUE : null;
        navigatorsController.updateEnvironmentProperty((CalcProperty) serviceLM.isServerRestarting.property, ObjectValue.getValue(isRestarting, LogicalClass.instance));
    }

    private Map<String, String> formSets;

    public void setFormSets(Map<String, String> formSets) {
        this.formSets = formSets;
    }

    public String getForms(String formSet) {
        if (formSets != null)
            return formSets.get(formSet);
        else
            return null;
    }

    public String getName() throws RemoteException {
        return getClass().getSimpleName();
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getDisplayName() throws RemoteException {
        return null;
    }

    public byte[] getMainIcon() throws RemoteException {
        return null;
    }

    public byte[] getLogo() throws RemoteException {
        return null;
    }

    public void outputPropertyClasses() {
        for (LP lp : LM.lproperties) {
            logger.debug(lp.property.getSID() + " : " + lp.property.caption + " - " + lp.getClassWhere());
        }
    }

    public void outputPersistent() {
        String result = "";

        result += '\n' + getString("logics.info.by.tables") + '\n' + '\n';
        for (Map.Entry<ImplementTable, Collection<CalcProperty>> groupTable : BaseUtils.group(new BaseUtils.Group<ImplementTable, CalcProperty>() {
            public ImplementTable group(CalcProperty key) {
                return key.mapTable.table;
            }
        }, getStoredProperties()).entrySet()) {
            result += groupTable.getKey().outputKeys() + '\n';
            for (CalcProperty property : groupTable.getValue())
                result += '\t' + property.outputStored(false) + '\n';
        }
        result += '\n' + getString("logics.info.by.properties") + '\n' + '\n';
        for (CalcProperty property : getStoredProperties())
            result += property.outputStored(true) + '\n';
        System.out.println(result);
    }

    public static int generateStaticNewID() {
        return BaseLogicsModule.generateStaticNewID();
    }

    public int generateNewID() throws RemoteException {
        return generateStaticNewID();
    }

    public void setUserLoggableProperties() throws SQLException {

        DataSession session = createSession();

        LCP<PropertyInterface> isProperty = LM.is(reflectionLM.property);
        ImRevMap<PropertyInterface, KeyExpr> keys = isProperty.getMapKeys();
        KeyExpr key = keys.singleValue();
        QueryBuilder<PropertyInterface, Object> query = new QueryBuilder<PropertyInterface, Object>(keys);
        query.addProperty("SIDProperty", reflectionLM.SIDProperty.getExpr(session.getModifier(), key));
        query.addProperty("userLoggableProperty", reflectionLM.userLoggableProperty.getExpr(session.getModifier(), key));
        query.and(isProperty.getExpr(key).getWhere());
        ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> result = query.execute(session.sql);

        for (ImMap<Object, Object> values : result.valueIt()) {
            Object userLoggable = values.get("userLoggableProperty");
            if (userLoggable != null) {
                LM.makeUserLoggable((LCP) getLP(values.get("SIDProperty").toString().trim()));
            }
        }

    }

    public void setPropertyNotifications() throws SQLException {

        DataSession session = createSession();

        LCP isNotification = LM.is(emailLM.notification);
        ImRevMap<Object, KeyExpr> keys = isNotification.getMapKeys();
        KeyExpr key = keys.singleValue();
        QueryBuilder<Object, Object> query = new QueryBuilder<Object, Object>(keys);
        query.addProperty("isDerivedChange", emailLM.isEventNotification.getExpr(session.getModifier(), key));
        query.addProperty("subject", emailLM.subjectNotification.getExpr(session.getModifier(), key));
        query.addProperty("text", emailLM.textNotification.getExpr(session.getModifier(), key));
        query.addProperty("emailFrom", emailLM.emailFromNotification.getExpr(session.getModifier(), key));
        query.addProperty("emailTo", emailLM.emailToNotification.getExpr(session.getModifier(), key));
        query.addProperty("emailToCC", emailLM.emailToCCNotification.getExpr(session.getModifier(), key));
        query.addProperty("emailToBC", emailLM.emailToBCNotification.getExpr(session.getModifier(), key));
        query.and(isNotification.getExpr(key).getWhere());
        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> result = query.execute(session.sql);

        for (int i=0,size=result.size();i<size;i++) {
            DataObject notificationObject = new DataObject(result.getKey(i).getValue(0), emailLM.notification);
            KeyExpr propertyExpr2 = new KeyExpr("property");
            KeyExpr notificationExpr2 = new KeyExpr("notification");
            ImRevMap<String, KeyExpr> newKeys2 = MapFact.toRevMap("property", propertyExpr2, "notification", notificationExpr2);

            QueryBuilder<String, String> query2 = new QueryBuilder<String, String>(newKeys2);
            query2.addProperty("SIDProperty", reflectionLM.SIDProperty.getExpr(session.getModifier(), propertyExpr2));
            query2.and(emailLM.inNotificationProperty.getExpr(session.getModifier(), notificationExpr2, propertyExpr2).getWhere());
            query2.and(notificationExpr2.compare(notificationObject, Compare.EQUALS));
            ImOrderMap<ImMap<String, Object>, ImMap<String, Object>> result2 = query2.execute(session.sql);
            List<LCP> listInNotificationProperty = new ArrayList();
            for (int j=0,size2=result2.size();j<size2;j++) {
                listInNotificationProperty.add((LCP) getLP(result2.getValue(i).get("SIDProperty").toString().trim()));
            }
            ImMap<Object, Object> rowValue = result.getValue(i);

            for (LCP prop : listInNotificationProperty) {
                boolean isDerivedChange = rowValue.get("isDerivedChange") == null ? false : true;
                String subject = rowValue.get("subject") == null ? "" : rowValue.get("subject").toString().trim();
                String text = rowValue.get("text") == null ? "" : rowValue.get("text").toString().trim();
                String emailFrom = rowValue.get("emailFrom") == null ? "" : rowValue.get("emailFrom").toString().trim();
                String emailTo = rowValue.get("emailTo") == null ? "" : rowValue.get("emailTo").toString().trim();
                String emailToCC = rowValue.get("emailToCC") == null ? "" : rowValue.get("emailToCC").toString().trim();
                String emailToBC = rowValue.get("emailToBC") == null ? "" : rowValue.get("emailToBC").toString().trim();
                LAP emailNotificationProperty = LM.addProperty(LM.actionGroup, new LAP(new NotificationActionProperty(prop.property.getSID() + "emailNotificationProperty", "emailNotificationProperty", prop, subject, text, emailFrom, emailTo, emailToCC, emailToBC, LM.BL)));

                Integer[] params = new Integer[prop.listInterfaces.size()];
                for (int j = 0; j < prop.listInterfaces.size(); j++)
                    params[j] = j + 1;
                if (isDerivedChange)
                    emailNotificationProperty.setEventAction(LM, prop, params);
                else
                    emailNotificationProperty.setEventSetAction(LM, prop, params);
            }
        }
    }

    public void setNotNullProperties() throws SQLException {

        DataSession session = createSession();

        LCP isProperty = LM.is(reflectionLM.property);
        ImRevMap<Object, KeyExpr> keys = isProperty.getMapKeys();
        KeyExpr key = keys.singleValue();
        QueryBuilder<Object, Object> query = new QueryBuilder<Object, Object>(keys);
        query.addProperty("SIDProperty", reflectionLM.SIDProperty.getExpr(session.getModifier(), key));
        query.addProperty("isSetNotNullProperty", reflectionLM.isSetNotNullProperty.getExpr(session.getModifier(), key));
        query.and(isProperty.getExpr(key).getWhere());
        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> result = query.execute(session.sql);

        for (ImMap<Object, Object> values : result.valueIt()) {
            Object isSetNotNull = values.get("isSetNotNullProperty");
            if (isSetNotNull != null) {
                LCP<?> prop = (LCP) getLP(values.get("SIDProperty").toString().trim());
                prop.property.setNotNull = true;
                LM.setNotNull(prop);
            }
        }
    }

    public byte[] getBaseClassByteArray() throws RemoteException {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(outStream);
            LM.baseClass.serialize(dataStream);
            return outStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getPropertyObjectsByteArray(byte[] byteClasses, boolean isCompulsory, boolean isAny) {
        try {
            DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(byteClasses));

            Map<Integer, Integer> groupMap = new HashMap<Integer, Integer>();
            MExclMap<ValueClassWrapper, Integer> mClasses = MapFact.mExclMap();
            int size = inStream.readInt();
            for (int i = 0; i < size; i++) {
                Integer ID = inStream.readInt();
                ValueClass valueClass = TypeSerializer.deserializeValueClass(this, inStream);
                mClasses.exclAdd(new ValueClassWrapper(valueClass), ID);

                int groupId = inStream.readInt();
                if (groupId >= 0) {
                    groupMap.put(ID, groupId);
                }
            }

            ArrayList<Property> result = new ArrayList<Property>();
            ArrayList<ArrayList<Integer>> idResult = new ArrayList<ArrayList<Integer>>();

            addProperties(mClasses.immutable(), groupMap, result, idResult, isCompulsory, isAny);

            List<Property> newResult = BaseUtils.filterList(result, getProperties());

            ByteArrayOutputStream outStream = new ByteArrayOutputStream();

            DataOutputStream dataStream = new DataOutputStream(outStream);

            ServerSerializationPool pool = new ServerSerializationPool();

            dataStream.writeInt(result.size());
            int num = 0;
            for (Property<?> property : newResult) {
                pool.serializeObject(dataStream, property);
                Iterator<Integer> it = idResult.get(num++).iterator();
                for (PropertyInterface propertyInterface : property.interfaces) {
                    pool.serializeObject(dataStream, propertyInterface);
                    dataStream.writeInt(it.next());
                }
            }

            return outStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private <T extends PropertyInterface> void addProperties(ImMap<ValueClassWrapper, Integer> classes, Map<Integer, Integer> groupMap, ArrayList<Property> result, ArrayList<ArrayList<Integer>> idResult, boolean isCompulsory, boolean isAny) {
        Set<Integer> allGroups = new HashSet<Integer>(groupMap.values());
        MCol<ImSet<ValueClassWrapper>> mClassSets = ListFact.mCol();

        for (ImSet<ValueClassWrapper> classSet : new Subsets<ValueClassWrapper>(classes.keys())) {
            Set<Integer> classesGroups = new HashSet<Integer>();
            for (ValueClassWrapper wrapper : classSet) {
                int id = classes.get(wrapper);
                if (groupMap.containsKey(id)) {
                    classesGroups.add(groupMap.get(id));
                }
            }
            if ((isCompulsory && classesGroups.size() == allGroups.size()) ||
                    (!isCompulsory && classesGroups.size() > 0 || groupMap.isEmpty()) || classSet.isEmpty()) {
                mClassSets.add(classSet);
            }
        }

        for (PropertyClassImplement<T, ?> implement : LM.rootGroup.getProperties(mClassSets.immutableCol(), isAny)) {
            result.add(implement.property);
            ArrayList<Integer> ids = new ArrayList<Integer>();
            for (T iface : implement.property.interfaces) {
                ids.add(classes.get(implement.mapping.get(iface)));
            }
            idResult.add(ids);
        }
    }

    @Override
    public FormInstance createFormInstance(FormEntity formEntity, ImMap<ObjectEntity, DataObject> mapObjects, DataSession session, boolean isModal, FormSessionScope sessionScope, boolean checkOnOk, boolean showDrop, boolean interactive) throws SQLException {
        return new FormInstance(formEntity, this, session, PolicyManager.serverSecurityPolicy, null, null, new DataObject(getServerComputer(), LM.computer), null, mapObjects, isModal, sessionScope.isManageSession(), checkOnOk, showDrop, interactive, null);
    }

    @Override
    public RemoteForm createRemoteForm(FormInstance formInstance) {
        try {
            return new RemoteForm<T, FormInstance<T>>(formInstance, exportPort, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void close() throws SQLException {
        getThreadLocalSql().close();
    }

    public UserInfo getUserInfo(String username) throws RemoteException {
        try {
            DataSession session = createSession();
            try {
                //User user = readUser(username, session);
                //if (user == null) {
                //    throw new LoginException();
                //}
                Integer userId = (Integer) LM.loginToUser.read(session, new DataObject(username, StringClass.get(30)));
                if (userId == null) {
                    throw new LoginException();
                }

                String password = (String) LM.sha256PasswordCustomUser.read(session, new DataObject(userId, LM.customUser));
                if (password == null) {
                    String plainPassword = ((String) LM.userPassword.read(session, new DataObject(userId, LM.customUser))).trim();
                    password = BaseUtils.calculateBase64Hash("SHA-256", plainPassword, UserInfo.salt);
                }
                if (password != null)
                    password = password.trim();

                return new UserInfo(username, password, getUserRolesNames(username));
            } finally {
                session.close();
            }
        } catch (SQLException se) {
            throw new RuntimeException(getString("logics.info.error.reading.user.data"), se);
        }
    }

    @Override
    public int generateID() throws RemoteException {
        try {
            return IDTable.instance.generateID(getIDSql(), IDTable.OBJECT);
        } catch (SQLException e) {
            throw new RuntimeException(getString("logics.info.error.reading.user.data"), e);
        }
    }

    private List<String> getUserRolesNames(String username) {
        try {
            DataSession session = createSession();
            try {
                ImRevMap<String, KeyExpr> keys = KeyExpr.getMapKeys(SetFact.toExclSet("user", "role"));
                Expr userExpr = keys.get("user");
                Expr roleExpr = keys.get("role");

                QueryBuilder<String, String> q = new QueryBuilder<String, String>(keys);
                q.and(securityLM.inMainRoleCustomUser.getExpr(session.getModifier(), userExpr, roleExpr).getWhere());
                q.and(LM.userLogin.getExpr(session.getModifier(), userExpr).compare(new DataObject(username), Compare.EQUALS));

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
                roles.addAll(getExtraUserRoleNames(new DataObject(username)));

                return roles;
            } finally {
                session.close();
            }

        } catch (SQLException e) {
            throw new RuntimeException(getString("logics.info.error.reading.list.of.roles"), e);
        }
    }

    protected List<String> getExtraUserRoleNames(DataObject user) {
        return new ArrayList<String>();
    }

/*    private User authenticateUser(String login, String password) throws LoginException, SQLException {
        DataSession session = createSession();
        try {
            User user = readUser(login, session);
            if (user == null) {
                throw new LoginException();
            }
            String checkPassword = (String) LM.userPassword.read(session, new DataObject(user.ID, LM.customUser));
            if (checkPassword != null && !password.trim().equals(checkPassword.trim())) {
                throw new LoginException();
            }

            return user;
        } finally {
            session.close();
        }
    }*/

    public void logException(String message, String errorType, String erTrace, DataObject user, String clientName, boolean client) throws SQLException {
        DataSession session = createSession();
        DataObject exceptionObject;
        if (client) {
            exceptionObject = session.addObject(systemEventsLM.clientException);
            systemEventsLM.clientClientException.change(clientName, session, exceptionObject);
            String userLogin = (String) LM.userLogin.read(session, user);
            systemEventsLM.loginClientException.change(userLogin, session, exceptionObject);
        } else {
            exceptionObject = session.addObject(systemEventsLM.serverException);
        }
        systemEventsLM.messageException.change(message, session, exceptionObject);
        systemEventsLM.typeException.change(errorType, session, exceptionObject);
        systemEventsLM.erTraceException.change(erTrace, session, exceptionObject);
        systemEventsLM.dateException.change(DateConverter.dateToStamp(Calendar.getInstance().getTime()), session, exceptionObject);

        session.apply(this);
        session.close();
    }

    public ArrayList<IDaemonTask> getDaemonTasks(int compId) {
        return new ArrayList<IDaemonTask>();
    }

    protected Integer getUserByEmail(DataSession session, String email) throws SQLException {
        return (Integer) emailLM.contactEmail.read(session, new DataObject(email));
    }

    @Override
    public void remindPassword(String email, String localeLanguage) throws RemoteException {
        assert email != null;
        //todo: в будущем нужно поменять на проставление локали в Context
//            ServerResourceBundle.load(localeLanguage);
        try {
            DataSession session = createSession();
            try {
                Integer userId = getUserByEmail(session, email);
                if (userId == null) {
                    throw new RuntimeException(ServerResourceBundle.getString("mail.user.not.found") + ": " + email);
                }

                emailLM.emailUserPassUser.execute(session, new DataObject(userId, LM.customUser));
            } finally {
                session.close();
            }
        } catch (Exception e) {
            logger.error("Error reminding password: ", e);
            throw new RemoteMessageException(ServerResourceBundle.getString("mail.error.sending.password.remind"), e);
        }
    }

    @Override
    public boolean checkPropertyViewPermission(String userName, String propertySID) {
        boolean forbidView;
        try {
            User user = readUser(userName, createSession());
            SecurityPolicy policy = user.getSecurityPolicy();
            forbidView = policy.property.view.checkPermission(getProperty(propertySID));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return forbidView;
    }

    @Override
    public boolean checkDefaultViewPermission(String propertySid) throws RemoteException {
        Property property = getProperty(propertySid);
        boolean default1 = policyManager.defaultSecurityPolicy.property.view.checkPermission(property);
        Boolean default2;
        try {
            DataSession session = createSession();
            DataObject propertyObject = new DataObject(reflectionLM.propertySID.read(session, new DataObject(propertySid)), reflectionLM.property);
            default2 = (Boolean) securityLM.permitViewProperty.read(session, propertyObject);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return default1 && default2 != null;
    }

    @Override
    public byte[] readFile(String sid, String... params) throws RemoteException {
        LCP property = (LCP) getLP(sid);
        ImOrderSet<PropertyInterface> interfaces = property.listInterfaces;
        DataObject[] objects = new DataObject[interfaces.size()];
        byte[] fileBytes;
        try {
            DataSession session = createSession();
            for (int i = 0; i < interfaces.size(); i++) {
                objects[i] = session.getDataObject(Integer.decode(params[i]), property.property.getInterfaceType(interfaces.get(i)));
            }
            fileBytes = (byte[]) property.read(session, objects);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return fileBytes;
    }

    @Override
    public String addUser(String username, String email, String password, String firstName, String lastName, String localeLanguage) throws RemoteException {
        try {
            //todo: в будущем нужно поменять на проставление локали в Context
//            ServerResourceBundle.load(localeLanguage);
            DataSession session = createSession();
            Object userId = LM.loginToUser.read(session, new DataObject(username, StringClass.get(30)));
            if (userId != null)
                return ServerResourceBundle.getString("logics.error.user.duplicate");

            Object emailId = emailLM.contactEmail.read(session, new DataObject(email, StringClass.get(50)));
            if (emailId != null) {
                return ServerResourceBundle.getString("logics.error.emailContact.duplicate");
            }

            DataObject userObject = session.addObject(LM.customUser);
            LM.userLogin.change(username, session, userObject);
            emailLM.emailContact.change(email, session, userObject);
            LM.userPassword.change(password, session, userObject);
            LM.sha256PasswordCustomUser.change(BaseUtils.calculateBase64Hash("SHA-256", password, UserInfo.salt), session, userObject);
            LM.userFirstName.change(firstName, session, userObject);
            LM.userLastName.change(lastName, session, userObject);
            session.apply(this);
        } catch (SQLException e) {
            return ServerResourceBundle.getString("logics.error.registration");
        }
        return null;
    }

    public Collection<FormEntity> getFormEntities(){
        Collection<FormEntity> result = new ArrayList<FormEntity>();
        for(LogicsModule logicsModule : logicModules) {
           for(NavigatorElement entry : logicsModule.moduleNavigators.values())
               if(entry instanceof FormEntity)
                    result.add((FormEntity) entry);
        }
        return result;
    }

    public Collection<NavigatorElement> getNavigatorElements(){
        Collection<NavigatorElement> result = new ArrayList<NavigatorElement>();
        for(LogicsModule logicsModule : logicModules) {
                result.addAll(logicsModule.moduleNavigators.values());
        }
        return result;
    }

    public FormEntity getFormEntity(String formSID){
        for(LogicsModule logicsModule : logicModules) {
            for(NavigatorElement entry : logicsModule.moduleNavigators.values())
            if((formSID.equals(entry.getSID()))&& (entry instanceof FormEntity))
                return (FormEntity) entry;
        }
        return null;
    }

    @Override
    public BusinessLogics getBL() {
        return this;
    }

    public FormInstance getFormInstance() {
        return null;
    }

    // для обратной совместимости
    // нужно использовать класс BusinessLogicsBootstrap
    public static void start(String[] args) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException, JRException {
        BusinessLogicsBootstrap.start();
    }

    public static void stop(String[] args) throws RemoteException, NotBoundException {
        BusinessLogicsBootstrap.stop();
    }

    // интерфейс для обычного старта
    public static void main(String[] args) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException, JRException {
        BusinessLogicsBootstrap.start();
    }
}
