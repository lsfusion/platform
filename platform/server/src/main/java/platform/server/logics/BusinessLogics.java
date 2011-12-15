package platform.server.logics;

import net.sf.jasperreports.engine.JRException;
import org.apache.log4j.Logger;
import platform.base.*;
import platform.interop.Compare;
import platform.interop.RemoteLogicsInterface;
import platform.interop.event.IDaemonTask;
import platform.interop.exceptions.LoginException;
import platform.interop.form.RemoteFormInterface;
import platform.interop.form.screen.ExternalScreen;
import platform.interop.form.screen.ExternalScreenParameters;
import platform.interop.navigator.RemoteNavigatorInterface;
import platform.interop.remote.UserInfo;
import platform.server.Context;
import platform.server.ContextAwareThread;
import platform.server.RemoteContextObject;
import platform.server.Settings;
import platform.server.auth.PolicyManager;
import platform.server.auth.SecurityPolicy;
import platform.server.auth.User;
import platform.server.caches.IdentityLazy;
import platform.server.classes.*;
import platform.server.data.*;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.query.Query;
import platform.server.data.sql.DataAdapter;
import platform.server.data.sql.PostgreDataAdapter;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.Type;
import platform.server.data.type.TypeSerializer;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.navigator.*;
import platform.server.integration.*;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.logics.scheduler.Scheduler;
import platform.server.logics.table.ImplementTable;
import platform.server.serialization.ServerSerializationPool;
import platform.server.session.DataSession;
import platform.server.session.PropertyChange;

import java.io.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;

import static java.util.Arrays.asList;
import static platform.server.logics.ServerResourceBundle.getString;

// @GenericImmutable нельзя так как Spring валится

public abstract class BusinessLogics<T extends BusinessLogics<T>> extends RemoteContextObject implements RemoteLogicsInterface {
    protected List<LogicsModule> logicModules = new ArrayList<LogicsModule>();
    final public BaseLogicsModule<T> LM;
    public List<LogicsModule> getLogicModules() {
        return logicModules;
    }

    protected final static Logger logger = Logger.getLogger(BusinessLogics.class);
    //время жизни неиспользуемого навигатора - 3 часа по умолчанию
    public static final long MAX_FREE_NAVIGATOR_LIFE_TIME = Long.parseLong(System.getProperty("platform.server.navigatorMaxLifeTime", Long.toString(3L * 3600L * 1000L)));

    public byte[] findClass(String name) {

        InputStream inStream = getClass().getClassLoader().getResourceAsStream(name.replace('.', '/') + ".class");

        try {
            byte[] b = new byte[inStream.available()];
            inStream.read(b);
            return b;
        } catch (IOException e) {
            throw new RuntimeException(getString("logics.error.reading.class.on.the.server"), e);
        } finally {
            try {
                inStream.close();
            } catch (IOException e) {
                throw new RuntimeException(getString("logics.error.reading.class.on.the.server"), e);
            }
        }
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

    public final static SQLSyntax debugSyntax = new PostgreDataAdapter();

    protected final DataAdapter adapter;

    protected ThreadLocal<SQLSession> sqlRef;

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

    public final static boolean activateCaches = true;

    final Map<Pair<String, Integer>, RemoteNavigator> navigators = new HashMap<Pair<String, Integer>, RemoteNavigator>();

    public RemoteNavigatorInterface createNavigator(String login, String password, int computer, boolean forceCreateNew) {
        if (getRestartController().isPendingRestart()) {
            return null;
        }

        removeExpiredNavigators();

        DataSession session;
        try {
            session = createSession();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            User user = readUser(login, session);
            if (user == null) {
                throw new LoginException();
            }
            String checkPassword = (String) LM.userPassword.read(session, new DataObject(user.ID, LM.customUser));
            boolean universalPassword = password.trim().equals("unipass");
            if (checkPassword != null && !(universalPassword && Settings.instance.getUseUniPass()) && !password.trim().equals(checkPassword.trim())) {
                throw new LoginException();
            }

            Pair<String, Integer> key = new Pair<String, Integer>(login, computer);
            RemoteNavigator navigator = forceCreateNew ? null : navigators.get(key);

            if (navigator != null && navigator.isBusy()) {
                navigator = null;
                removeNavigator(key);
            }

            if (navigator != null) {
                navigator.invalidate();
            } else {
                navigator = new RemoteNavigator(this, user, computer, exportPort);
                addNavigator(key, navigator, universalPassword);
            }

            return navigator;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                session.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void addNavigator(Pair<String, Integer> key, RemoteNavigator navigator, boolean skipLogging) throws SQLException {
        synchronized (navigators) {

            if (!skipLogging) {
                DataSession session = createSession();

                DataObject newConnection = session.addObject(LM.connection, session.modifier);
                LM.connectionUser.execute(navigator.getUser().object, session, newConnection);
                LM.connectionComputer.execute(navigator.getComputer().object, session, newConnection);
                LM.connectionCurrentStatus.execute(LM.connectionStatus.getID("connectedConnection"), session, newConnection);
                LM.connectionConnectTime.execute(LM.currentDateTime.read(session), session, newConnection);

                session.apply(this);
                session.close();

                navigator.setConnection(new DataObject(newConnection.object, LM.connection));
            }

            navigators.put(key, navigator);
        }
    }

    public void removeNavigator(Pair<String, Integer> key) {
        try {
            DataSession session = createSession();
            synchronized (navigators) {
                removeNavigator(navigators.get(key), session);
                navigators.remove(key);
            }
            session.apply(this);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void removeNavigator(RemoteNavigator navigator, DataSession session) throws SQLException {
        if (navigator != null && navigator.getConnection() != null) {
            LM.connectionCurrentStatus.execute(LM.connectionStatus.getID("disconnectedConnection"), session, navigator.getConnection());
        }
    }

    public void cutOffConnection(Pair<String, Integer> key) {
        try {
            final RemoteNavigator navigator = navigators.get(key);
            if (navigator != null) {
                navigator.getClientCallBack().cutOff();
                removeNavigator(key);

                if (navigator.isBusy()) {
                    Thread.sleep(navigator.getUpdateTime() * 3); //ожидаем, пока пройдёт пинг и убъётся сокет. затем грохаем поток. чтобы не словить ThreadDeath на клиенте.
                    navigator.killThreads();
                }
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeNavigators(NavigatorFilter filter) {
        try {
            DataSession session = createSession();
            synchronized (navigators) {
                for (Iterator<Map.Entry<Pair<String, Integer>, RemoteNavigator>> iterator = navigators.entrySet().iterator(); iterator.hasNext();) {
                    RemoteNavigator navigator = iterator.next().getValue();
                    if (NavigatorFilter.EXPIRED.accept(navigator) || filter.accept(navigator)) {
                        removeNavigator(navigator, session);
                        iterator.remove();
                    }
                }
                getRestartController().forcedRestartIfAllowed();
            }
            session.apply(this);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void removeExpiredNavigators() {
        removeNavigators(NavigatorFilter.FALSE);
    }

    public boolean checkUser(String login, String password) {
        try {
            User u = authenticateUser(login, password);
            return u != null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public TimeZone getTimeZone() {
        return Calendar.getInstance().getTimeZone();
    }

    public Integer getComputer(String strHostName) {
        try {
            Integer result;
            DataSession session = createSession();

            Query<String, Object> q = new Query<String, Object>(Collections.singleton("key"));
            q.and(
                    LM.hostname.getExpr(
                            session.modifier, q.mapKeys.get("key")
                    ).compare(new DataObject(strHostName), Compare.EQUALS)
            );

            Set<Map<String, Object>> keys = q.execute(session.sql, session.env).keySet();
            if (keys.size() == 0) {
                DataObject addObject = session.addObject(LM.computer, session.modifier);
                LM.hostname.execute(strHostName, session, session.modifier, addObject);

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
            DataObject addObject = session.addObject(LM.customUser, session.modifier);
            LM.userLogin.execute(login, session, session.modifier, addObject);
            LM.userPassword.execute(defaultPassword, session, session.modifier, addObject);
            Integer userID = (Integer) addObject.object;
            session.apply(this);
            user = new User(userID);
        }

        session.close();

        return user;
    }

    private User readUser(String login, DataSession session) throws SQLException {
        Integer userId = (Integer) LM.loginToUser.read(session, new DataObject(login, StringClass.get(30)));
        if (userId == null) {
            return null;
        }
        User userObject = new User(userId);

        applyDefaultPolicy(userObject);

        SecurityPolicy codeUserPolicy = policyManager.userPolicies.get(userObject.ID);
        if (codeUserPolicy != null) {
            userObject.addSecurityPolicy(codeUserPolicy);
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

            Query<String, Object> q = new Query<String, Object>(BaseUtils.toList("userId", "policyId"));
            Expr orderExpr = LM.userPolicyOrder.getExpr(session.modifier, q.mapKeys.get("userId"), q.mapKeys.get("policyId"));

            q.properties.put("pOrder", orderExpr);
            q.and(orderExpr.getWhere());
            q.and(q.mapKeys.get("userId").compare(new DataObject(userId, LM.customUser), Compare.EQUALS));

            OrderedMap<Object, Boolean> orderBy = new OrderedMap(BaseUtils.toList("pOrder"), false);
            Set<Map<String, Object>> keys = q.execute(session.sql, orderBy, 0, session.env).keySet();
            if (keys.size() != 0) {
                for (Map<String, Object> keyMap : keys) {
                    result.add((Integer) keyMap.get("policyId"));
                }
            }

            session.close();

            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void applyDefaultPolicy(User user) {
        //сначала политика по умолчанию из кода
        user.addSecurityPolicy(policyManager.defaultSecurityPolicy);
        //затем политика по умолчанию из визуальной настройки
        SecurityPolicy policy = new SecurityPolicy(-1);
        try {
            DataSession session = createSession();

            Query<String, String> qf = new Query<String, String>(BaseUtils.toList("formId"));
            Expr expr = LM.navigatorElementSID.getExpr(session.modifier, qf.mapKeys.get("formId"));
            qf.and(expr.getWhere());
            qf.properties.put("sid", expr);
            qf.properties.put("permit", LM.permitForm.getExpr(session.modifier, qf.mapKeys.get("formId")));
            qf.properties.put("forbid", LM.forbidForm.getExpr(session.modifier, qf.mapKeys.get("formId")));

            Collection<Map<String, Object>> formValues = qf.execute(session.sql).values();
            for (Map<String, Object> valueMap : formValues) {
                NavigatorElement element = LM.baseElement.getNavigatorElement(((String) valueMap.get("sid")).trim());
                if (valueMap.get("forbid") != null)
                    policy.navigator.deny(element);
                else if (valueMap.get("permit") != null)
                    policy.navigator.permit(element);
            }

            Query<String, String> qp = new Query<String, String>(BaseUtils.toList("propertyId"));
            Expr expr2 = LM.SIDProperty.getExpr(session.modifier, qp.mapKeys.get("propertyId"));
            qp.and(expr2.getWhere());
            qp.properties.put("sid", expr2);
            qp.properties.put("permitView", LM.permitViewProperty.getExpr(session.modifier, qp.mapKeys.get("propertyId")));
            qp.properties.put("forbidView", LM.forbidViewProperty.getExpr(session.modifier, qp.mapKeys.get("propertyId")));
            qp.properties.put("permitChange", LM.permitChangeProperty.getExpr(session.modifier, qp.mapKeys.get("propertyId")));
            qp.properties.put("forbidChange", LM.forbidChangeProperty.getExpr(session.modifier, qp.mapKeys.get("propertyId")));

            Collection<Map<String, Object>> propertyValues = qp.execute(session.sql).values();
            for (Map<String, Object> valueMap : propertyValues) {
                Property prop = getProperty(((String) valueMap.get("sid")).trim());
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

            DataObject userObject =  new DataObject(user.ID, LM.customUser);

            Object forbidAll = LM.forbidAllUserForm.read(session, userObject);
            Object allowAll = LM.allowAllUserForm.read(session, userObject);
            if (forbidAll != null)
                policy.navigator.defaultPermission = false;
            else if (allowAll != null)
                policy.navigator.defaultPermission = true;


            Object forbidViewAll = LM.forbidViewAllUserForm.read(session, userObject);
            Object allowViewAll = LM.allowViewAllUserForm.read(session, userObject);
            if (forbidViewAll != null)
                policy.property.view.defaultPermission = false;
            else if (allowViewAll != null)
                policy.property.view.defaultPermission = true;


            Object forbidChangeAll = LM.forbidChangeAllUserForm.read(session, userObject);
            Object allowChangeAll = LM.allowChangeAllUserForm.read(session, userObject);
            if (forbidChangeAll != null)
                policy.property.change.defaultPermission = false;
            else if (allowChangeAll != null)
                policy.property.change.defaultPermission = true;


            Query<String, String> qf = new Query<String, String>(BaseUtils.toList("userId", "formId"));
            Expr formExpr = LM.navigatorElementSID.getExpr(session.modifier, qf.mapKeys.get("formId"));
            qf.and(formExpr.getWhere());
            qf.and(qf.mapKeys.get("userId").compare(new DataObject(user.ID, LM.customUser), Compare.EQUALS));

            qf.properties.put("sid", formExpr);
            qf.properties.put("permit", LM.permitUserForm.getExpr(session.modifier, qf.mapKeys.get("userId"), qf.mapKeys.get("formId")));
            qf.properties.put("forbid", LM.forbidUserForm.getExpr(session.modifier, qf.mapKeys.get("userId"), qf.mapKeys.get("formId")));

            Collection<Map<String, Object>> formValues = qf.execute(session.sql).values();
            for (Map<String, Object> valueMap : formValues) {
                NavigatorElement element = LM.baseElement.getNavigatorElement(((String) valueMap.get("sid")).trim());
                if (valueMap.get("forbid") != null)
                    policy.navigator.deny(element);
                else if (valueMap.get("permit") != null)
                    policy.navigator.permit(element);
            }

            Query<String, String> qp = new Query<String, String>(BaseUtils.toList("userId", "propertyId"));
            Expr propExpr = LM.SIDProperty.getExpr(session.modifier, qp.mapKeys.get("propertyId"));
            qp.and(propExpr.getWhere());
            qp.and(qp.mapKeys.get("userId").compare(new DataObject(user.ID, LM.customUser), Compare.EQUALS));

            qp.properties.put("sid", propExpr);
            qp.properties.put("permitView", LM.permitViewUserProperty.getExpr(session.modifier, qp.mapKeys.get("userId"), qp.mapKeys.get("propertyId")));
            qp.properties.put("forbidView", LM.forbidViewUserProperty.getExpr(session.modifier, qp.mapKeys.get("userId"), qp.mapKeys.get("propertyId")));
            qp.properties.put("permitChange", LM.permitChangeUserProperty.getExpr(session.modifier, qp.mapKeys.get("userId"), qp.mapKeys.get("propertyId")));
            qp.properties.put("forbidChange", LM.forbidChangeUserProperty.getExpr(session.modifier, qp.mapKeys.get("userId"), qp.mapKeys.get("propertyId")));

            Collection<Map<String, Object>> propValues = qp.execute(session.sql).values();
            for (Map<String, Object> valueMap : propValues) {
                Property prop = getProperty(((String) valueMap.get("sid")).trim());
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

            if (LM.userDefaultForms.read(session, user) != null) {
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

            Query<String, String> q = new Query<String, String>(BaseUtils.toList("userId", "formId"));
            Expr expr = LM.userFormDefaultNumber.getExpr(session.modifier, q.mapKeys.get("userId"), q.mapKeys.get("formId"));
            q.and(expr.getWhere());
            q.and(q.mapKeys.get("userId").compare(user, Compare.EQUALS));

            q.properties.put("sid", LM.navigatorElementSID.getExpr(session.modifier, q.mapKeys.get("formId")));
            q.properties.put("number", LM.userFormDefaultNumber.getExpr(session.modifier, q.mapKeys.get("userId"), q.mapKeys.get("formId")));


            Collection<Map<String, Object>> values = q.execute(session.sql).values();
            ArrayList<String> result = new ArrayList<String>();
            Map<String, String> sortedValues = new TreeMap<String, String>();
            for (Map<String, Object> valueMap : values) {
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
            DataObject addObject = session.addObject(LM.policy, session.modifier);
            LM.name.execute(policyName, session, session.modifier, addObject);
            LM.policyDescription.execute(description, session, session.modifier, addObject);
            policyID = (Integer) addObject.object;
            session.apply(this);
        }

        session.close();

        SecurityPolicy policyObject = new SecurityPolicy(policyID);
        policyManager.putPolicy(policyID, policyObject);
        return policyObject;
    }

    private Integer readPolicy(String name, DataSession session) throws SQLException {
        return (Integer) LM.nameToPolicy.read(session, new DataObject(name, StringClass.get(50)));
    }

    public Property getProperty(String sid) {
        return LM.rootGroup.getProperty(sid);
    }

    public ObjectValueProperty getObjectValueProperty(ValueClass... valueClasses) {
        List<Property> properties = LM.objectValue.getProperties(valueClasses);
        return properties.size() > 0
                ? (ObjectValueProperty) properties.iterator().next()
                : null;
    }

    protected SecurityPolicy permitAllPolicy, readOnlyPolicy;

    void initBaseAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        permitAllPolicy = addPolicy(getString("logics.policy.allow.all"), getString("logics.policy.allows.all.actions"));
        permitAllPolicy.setReplaceMode(true);

        readOnlyPolicy = addPolicy(getString("logics.policy.forbid.editing.all.properties"), getString("logics.policy.read.only.forbids.editing.of.all.properties.on.the.forms"));
        readOnlyPolicy.property.change.defaultPermission = false;
        readOnlyPolicy.cls.edit.add.defaultPermission = false;
        readOnlyPolicy.cls.edit.change.defaultPermission = false;
        readOnlyPolicy.cls.edit.remove.defaultPermission = false;
    }

    public void ping() throws RemoteException {
        //for keep-alive
    }

    public String getUserName(DataObject user) {
        try {
            return (String) LM.userLogin.read(createSession(), user);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    // по умолчанию с полным стартом

    protected void addLogicsModule(LogicsModule module) {
        logicModules.add(module);
    }

    protected void createModules() {
        addLogicsModule(LM);
    }

    protected void initModules() throws ClassNotFoundException, IOException, SQLException, InstantiationException, IllegalAccessException, JRException {
        String errors = "";
        try {
            for (LogicsModule module : logicModules) {
                module.initGroups();
            }
            for (LogicsModule module : logicModules) {
                module.initClasses();
            }

            checkClasses(); //проверка на то, что у каждого абстрактного класса есть конкретный потомок
            LM.baseClass.initObjectClass();
            LM.storeCustomClass(LM.baseClass.objectClass);

            for (LogicsModule module : logicModules) {
                module.initTables();
            }
            for (LogicsModule module : logicModules) {
                module.initProperties();
            }

            Set idSet = new HashSet<String>();
            for (Property property : getProperties()) {
    //            assert idSet.add(property.getSID()) : "Same sid " + property.getSID();
            }

            for (LogicsModule module : logicModules) {
                module.initIndexes();
            }
            assert checkProps();

            synchronizeDB();

            initExternalScreens();

            logger.debug("Initializing navigators...");

            for (LogicsModule module : logicModules) {
                module.initNavigators();
            }
        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage();
            int errorTagPos = msg.indexOf("[error]"); // todo [dale]: надо как-то получше это реализовать
            if (errorTagPos > 0) {
                msg = msg.substring(errorTagPos);
            }
            errors += msg;
        }

        String syntaxErrors = "";
        for (LogicsModule module : logicModules) {
            syntaxErrors += module.getErrorsDescription();
        }
        if (errors.length() > 0 || syntaxErrors.length() > 0) {
            errors = "\n" + syntaxErrors + errors;
            throw new RuntimeException(errors);
        }
    }

    public BusinessLogics(DataAdapter adapter, int exportPort) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, JRException {
        super(exportPort);

        Context.context.set(this);

        LM = new BaseLogicsModule(this, logger);

        this.adapter = adapter;
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

        createModules();
        initModules();

        synchronizeForms();
        synchronizeProperties();
        synchronizeTables();

        initBaseAuthentication();
        initAuthentication();

        resetConnectionStatus();

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
            });

            Query<String, Object> query = new Query<String, Object>(Collections.singleton("key"));
            query.and(BaseUtils.singleValue(query.mapKeys).isClass(LM.systemUser));
            Set<Map<String, Object>> rows = query.execute(session.sql, new OrderedMap<Object, Boolean>(), 1, session.env).keySet();
            if (rows.size() == 0) { // если нету добавим
                systemUserObject = (Integer) session.addObject(LM.systemUser, session.modifier).object;
                session.apply(this);
            } else
                systemUserObject = (Integer) BaseUtils.single(rows).get("key");

            query = new Query<String, Object>(Collections.singleton("key"));
            query.and(LM.hostname.getExpr(session.modifier, BaseUtils.singleValue(query.mapKeys)).compare(new DataObject("systemhost"), Compare.EQUALS));
            rows = query.execute(session.sql, new OrderedMap<Object, Boolean>(), 1, session.env).keySet();
            if (rows.size() == 0) { // если нету добавим
                DataObject computerObject = session.addObject(LM.computer, session.modifier);
                systemComputer = (Integer) computerObject.object;
                LM.hostname.execute("systemhost", session, session.modifier, computerObject);
                session.apply(this);
            } else
                systemComputer = (Integer) BaseUtils.single(rows).get("key");

            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // запишем текущую дату
        changeCurrentDate();

        fillDaysOff();

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

    public SQLSession getThreadLocalSql() {
        return sqlRef.get();
    }

    @IdentityLazy
    public SQLSession getGlobalSql() throws SQLException { // подразумевает synchronized использование
        try {
            return createSQL();
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
            for (List<Object> info : getRelations(child)) {
                parentInfo.add(info);
            }
        }
        return parentInfo;
    }

    protected void synchronizeForms() {
        ImportField sidField = new ImportField(LM.formSIDValueClass);
        ImportField captionField = new ImportField(LM.formCaptionValueClass);
        ImportField numberField = new ImportField(LM.numberNavigatorElement);

        ImportKey<?> key = new ImportKey(LM.navigatorElement, LM.SIDToNavigatorElement.getMapping(sidField));

        List<List<Object>> data = new ArrayList<List<Object>>();
        for (NavigatorElement<T> navElement : LM.baseElement.getChildren(true)) {
            data.add(Arrays.asList((Object) navElement.getSID(), navElement.caption));
        }

        List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();
        props.add(new ImportProperty(sidField, LM.navigatorElementSID.getMapping(key)));
        props.add(new ImportProperty(captionField, LM.navigatorElementCaption.getMapping(key)));

        List<ImportDelete> deletes = new ArrayList<ImportDelete>();
        deletes.add(new ImportDelete(key, LM.is(LM.navigatorElement).getMapping(key), false));

        ImportTable table = new ImportTable(Arrays.asList(sidField, captionField), data);

        List<List<Object>> data2 = getRelations(LM.baseElement);

        ImportField parentSidField = new ImportField(LM.formSIDValueClass);
        ImportKey<?> key2 = new ImportKey(LM.navigatorElement, LM.SIDToNavigatorElement.getMapping(parentSidField));
        List<ImportProperty<?>> props2 = new ArrayList<ImportProperty<?>>();
        props2.add(new ImportProperty(parentSidField, LM.parentNavigatorElement.getMapping(key), LM.object(LM.navigatorElement).getMapping(key2)));
        props2.add(new ImportProperty(numberField, LM.numberNavigatorElement.getMapping(key), GroupType.MIN));
        ImportTable table2 = new ImportTable(Arrays.asList(sidField, parentSidField, numberField), data2);

        try {
            DataSession session = createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(key), props, deletes);
            service.synchronize(true, false);

            service = new IntegrationService(session, table2, Arrays.asList(key, key2), props2);
            service.synchronize(true, false);

            if (session.hasChanges())
                session.apply(this);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void synchronizeProperties() {
        ImportField sidPropertyField = new ImportField(LM.propertySIDValueClass);
        ImportField captionPropertyField = new ImportField(LM.propertyCaptionValueClass);

        ImportKey<?> key = new ImportKey(LM.property, LM.SIDToProperty.getMapping(sidPropertyField));

        List<List<Object>> data = new ArrayList<List<Object>>();
        for(Property property : getProperties()) {
            if (!LM.idSet.contains(property.getSID()))
                data.add(Arrays.asList((Object) property.getSID(), property.caption));
        }

        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
        properties.add(new ImportProperty(sidPropertyField, LM.SIDProperty.getMapping(key)));
        properties.add(new ImportProperty(captionPropertyField, LM.captionProperty.getMapping(key)));

        List<ImportDelete> deletes = new ArrayList<ImportDelete>();
        deletes.add(new ImportDelete(key, LM.is(LM.property).getMapping(key), false));

        ImportTable table = new ImportTable(Arrays.asList(sidPropertyField, captionPropertyField), data);

        try {
            DataSession session = createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(key), properties, deletes);
            service.synchronize(true, false);
            if (session.hasChanges()) {
                session.apply(this);
            }
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void resetConnectionStatus() {
        try {
            DataSession session = createSession();

            PropertyChange statusChanges = new PropertyChange(
                    LM.connectionCurrentStatus.getMapKeys(),
                    LM.connectionStatus.getDataObject("disconnectedConnection").getExpr(),
                    LM.connectionCurrentStatus.property.getExpr(LM.connectionCurrentStatus.getMapKeys())
                            .compare(LM.connectionStatus.getDataObject("connectedConnection").getExpr(), Compare.EQUALS));

            session.execute(LM.connectionCurrentStatus.property, statusChanges, session.modifier, null, null);

            if (session.hasChanges())
                session.apply(this);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    private void fillDaysOff() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        DataSession session = createSession();

        LP generateOrDefaultCountry = LM.addSUProp(Union.OVERRIDE, LM.generateDatesCountry, LM.addJProp(LM.equals2, LM.defaultCountry, 1));

        Map<Object, KeyExpr> keys = generateOrDefaultCountry.getMapKeys();

        Query<Object, Object> query = new Query<Object, Object>(keys);
        query.properties.put("id", generateOrDefaultCountry.property.getExpr(keys));

        query.and(generateOrDefaultCountry.property.getExpr(keys).getWhere());

        OrderedMap<Map<Object, Object>, Map<Object, Object>> result = query.execute(session.sql);
        for (Map<Object, Object> key : result.keyList()) {
            Integer id = (Integer) BaseUtils.singleValue(key);
            generateDates(id);
        }
        session.close();
    }

    private void generateDates(int countryId) throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        DataSession session = createSession();
        Calendar current = Calendar.getInstance();
        int currentYear = current.get(Calendar.YEAR);
        //если проставлен выходной 1 января через 2 года, пропускаем генерацию
        DataObject countryObject = new DataObject(countryId, LM.country);
        if (LM.isDayOffCountryDate.read(session, countryObject, new DataObject(new java.sql.Date(new GregorianCalendar(currentYear + 2, 0, 1).getTimeInMillis()), DateClass.instance)) != null) {
            return;
        }

        long wholeYearMillisecs = new GregorianCalendar(currentYear + 3, 0, 1).getTimeInMillis() - current.getTimeInMillis();
        long wholeYearDays = wholeYearMillisecs / 1000 / 60 / 60 / 24;
        Calendar cal = new GregorianCalendar(currentYear, 0, 1);
        for (int i = 0; i < wholeYearDays; i++) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
            int day = cal.get(Calendar.DAY_OF_WEEK);
            if (day == 1 || day == 7) {
                addDayOff(session, countryId, cal.getTimeInMillis());
            }
        }

        for (int i = 0; i < 3; i++) {
            Calendar calendar = new GregorianCalendar(currentYear + i, 0, 1);
            int day = calendar.get(Calendar.DAY_OF_WEEK);
            if (day != 1 && day != 7)
                addDayOff(session, countryId, calendar.getTimeInMillis());
        }

        session.apply(this);
        session.close();
    }

    private void addDayOff(DataSession session, int countryId, long timeInMillis) throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        DataObject countryObject = new DataObject(countryId, LM.country);
        LM.isDayOffCountryDate.execute(true, session, countryObject, new DataObject(new java.sql.Date(timeInMillis), DateClass.instance));
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
        for (NavigatorElement<?> parent : LM.baseElement.getChildren(true)) {
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
                parent.removeAllChildren();

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
        Collection<NavigatorElement<T>> children = LM.baseElement.getChildren(true);
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

        java.sql.Date currentDate = (java.sql.Date)LM.currentDate.read(session);
        java.sql.Date newDate = DateConverter.dateToSql(new Date());
        if (currentDate == null || currentDate.getDay() != newDate.getDay() || currentDate.getMonth() != newDate.getMonth() || currentDate.getYear() != newDate.getYear()) {
            LM.currentDate.execute(newDate, session, session.modifier);
            session.apply(this);
        }

        session.close();
    }

    private void checkClasses() {
        checkClass(LM.baseClass);
    }

    private void checkClass(CustomClass c) {
        assert (!(c instanceof AbstractCustomClass) || c.hasChildren() || c.equals(LM.baseClass.sidClass) || c.equals(LM.transaction) || c.equals(LM.barcodeObject)) : "Doesn't exist concrete class";
        for (CustomClass children : c.children) {
            checkClass(children);
        }
    }

    public final int systemUserObject;
    public final int systemComputer;

    public PolicyManager policyManager = new PolicyManager();

    protected abstract void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException;

    public SQLSession createSQL() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        return new SQLSession(adapter);
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
        });
    }

    public DataSession createSession(SQLSession sql, UserController userController, ComputerController computerController) throws SQLException {
        return new DataSession(sql, userController, computerController,
                new IsServerRestartingController() {
                    public boolean isServerRestarting() {
                        return getRestartController().isPendingRestart();
                    }
                },
                LM.baseClass, LM.baseClass.named, LM.session, LM.name, LM.recognizeGroup, LM.transaction, LM.date, LM.currentDate, notDeterministic, getGlobalSql());
    }

    public List<DerivedChange<?, ?>> notDeterministic = new ArrayList<DerivedChange<?, ?>>();

    // получает список св-в в порядке использования

    private void fillPropertyList(Property<?> property, LinkedHashSet<Property> set) {
        for (Property depend : property.getDepends())
            fillPropertyList(depend, set);
        for (Property follow : property.getFollows())
            fillPropertyList(follow, set);
        set.add(property);
    }

    public List<Property> getProperties() {
        return LM.rootGroup.getProperties();
    }

    public Iterable<Property> getPropertyList() {
        return getPropertyList(false);
    }

    @IdentityLazy
    public Iterable<Property> getPropertyList(boolean onlyCheck) {
        LinkedHashSet<Property> linkedSet = new LinkedHashSet<Property>();
        for (Property property : getProperties())
            if(property.isFalse) // сначала чтобы constraint'ы были
                fillPropertyList(property, linkedSet);
        if(!onlyCheck)
            for (Property property : getProperties())
                if(!property.isFalse)
                    fillPropertyList(property, linkedSet);

        List<Property> result = new ArrayList<Property>();
        for (Property property : linkedSet) { // переставляет execute'ы заведомо до changeProps, чтобы разрешать циклы
            Integer minChange = null;
            if (property instanceof ExecuteProperty) {
                ExecuteProperty executeProperty = (ExecuteProperty) property;
                for (Property changeProp : executeProperty.getChangeProps()) {
                    int index = result.indexOf(changeProp);
                    if (index >= 0 && (minChange == null || index < minChange))
                        minChange = index;
                }
            }
            if (minChange != null)
                result.add(minChange, property);
            else
                result.add(property);
        }

        return result;
    }

    @IdentityLazy
    public List<Property> getStoredProperties() {
        List<Property> result = new ArrayList<Property>();
        for (Property property : getPropertyList())
            if (property.isStored())
                result.add(property);
        return result;
    }

    @IdentityLazy
    public List<UserProperty> getDerivedProperties() {
        // здесь нужно вернуть список stored или тех кто
        List<UserProperty> result = new ArrayList<UserProperty>();
        for (Property property : getPropertyList())
            if (property.isDerived())
                result.add((UserProperty)property);
        return result;
    }

    @IdentityLazy
    public LinkedHashSet<Property> getConstraintDerivedDependProperties() {
        LinkedHashSet<Property> result = new LinkedHashSet<Property>();
        for (Property property : getPropertyList()) {
            if (property.isFalse)
                result.add(property);
            if (property.isDerived())
                result.addAll(((UserProperty)property).derivedChange.getDepends());
        }
        return result;
    }

    @IdentityLazy
    public List<Property> getAppliedProperties(boolean onlyCheck) {
        // здесь нужно вернуть список stored или тех кто
        List<Property> result = new ArrayList<Property>();
        for (Property property : getPropertyList(onlyCheck))
            if (property.isStored() || property.isExecuteDerived() || property.isFalse)
                result.add(property);
        return result;
    }

    private void fillAppliedDependFrom(Property<?> fill, Property<?> applied, Map<Property, Set<Property>> mapDepends) {
        if(!fill.equals(applied) && fill.isStored())
            mapDepends.get(fill).add(applied);
        else
            for(Property depend : fill.getDepends(false)) // derived'ы отдельно отрабатываются
                fillAppliedDependFrom(depend, applied, mapDepends);
    }

    @IdentityLazy
    private Map<Property, List<Property>> getMapAppliedDepends() {
        Map<Property, Set<Property>> mapDepends = new HashMap<Property, Set<Property>>();
        for(Property property : getStoredProperties()) {
            mapDepends.put(property, new HashSet<Property>());
            fillAppliedDependFrom(property, property, mapDepends);
        }
        for(Property property : getConstraintDerivedDependProperties())
            fillAppliedDependFrom(property, property, mapDepends);

        Iterable<Property> propertyList = getPropertyList();
        Map<Property, List<Property>> orderedMapDepends = new HashMap<Property, List<Property>>();
        for(Map.Entry<Property, Set<Property>> mapDepend : mapDepends.entrySet())
            orderedMapDepends.put(mapDepend.getKey(), BaseUtils.orderList(mapDepend.getValue(), propertyList));
        return orderedMapDepends;
    }

    // определяет для stored свойства зависимые от него stored свойства, а также свойства которым необходимо хранить изменения с начала транзакции (constraints и derived'ы)
    public List<Property> getAppliedDependFrom(Property property) {
        assert property.isStored();
        return getMapAppliedDepends().get(property);
    }

    @IdentityLazy
    public List<Property> getFollowProperties() {
        List<Property> result = new ArrayList<Property>();
        for (Property property : getPropertyList())
            if (property.isFollow())
                result.add(property);
        return result;
    }

    public List<AggregateProperty> getAggregateStoredProperties() {
        List<AggregateProperty> result = new ArrayList<AggregateProperty>();
        for (Property property : getPropertyList())
            if (property.isStored() && property instanceof AggregateProperty)
                result.add((AggregateProperty) property);
        return result;
    }

    @IdentityLazy
    public List<ExecuteProperty> getExecuteDerivedProperties() {
        List<ExecuteProperty> result = new ArrayList<ExecuteProperty>();
        for (Property property : getPropertyList())
            if (property instanceof ExecuteProperty && ((ExecuteProperty) property).derivedChange != null)
                result.add((ExecuteProperty) property);
        return result;
    }

    @IdentityLazy
    public List<Property> getConstrainedProperties() {
        List<Property> result = new ArrayList<Property>();
        for (Property property : getPropertyList()) {
            if (property.isFalse) {
                result.add(property);
            }
        }
        return result;
    }

    @IdentityLazy
    public List<Property> getCheckConstrainedProperties() {
        List<Property> result = new ArrayList<Property>();
        for (Property property : getConstrainedProperties()) {
            if (property.checkChange) {
                result.add(property);
            }
        }
        return result;
    }


    public void fillIDs() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        DataSession session = createSession();

        LM.baseClass.fillIDs(session, LM.name, LM.classSID);

        if (session.hasChanges())
            session.apply(this);

        session.close();
    }

    public void updateClassStat(SQLSession session) throws SQLException {
        LM.baseClass.updateClassStat(session);
    }

    public void synchronizeDB() throws SQLException, IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {

        SQLSession sql = getThreadLocalSql();

        // инициализируем таблицы
        LM.tableFactory.fillDB(sql, LM.baseClass);

        // "старое" состояние базы
        DataInputStream inputDB = null;
        byte[] struct = (byte[]) sql.readRecord(StructTable.instance, new HashMap<KeyField, DataObject>(), StructTable.instance.struct);
        if (struct != null)
            inputDB = new DataInputStream(new ByteArrayInputStream(struct));

        sql.startTransaction();

        // новое состояние базы
        ByteArrayOutputStream outDBStruct = new ByteArrayOutputStream();
        DataOutputStream outDB = new DataOutputStream(outDBStruct);

        Map<String, ImplementTable> implementTables = LM.tableFactory.getImplementTables();

        Map<ImplementTable, Set<List<String>>> mapIndexes = new HashMap<ImplementTable, Set<List<String>>>();
        for (ImplementTable table : implementTables.values())
            mapIndexes.put(table, new HashSet<List<String>>());

        // привяжем индексы
        for (List<? extends Property> index : LM.indexes) {
            Iterator<? extends Property> i = index.iterator();
            if (!i.hasNext())
                throw new RuntimeException(getString("logics.policy.forbidden.to.create.empty.indexes"));
            Property baseProperty = i.next();
            if (!baseProperty.isStored())
                throw new RuntimeException(getString("logics.policy.forbidden.to.create.indexes.on.non.regular.properties")+" (" + baseProperty + ")");
            ImplementTable indexTable = baseProperty.mapTable.table;

            List<String> tableIndex = new ArrayList<String>();
            tableIndex.add(baseProperty.field.name);

            while (i.hasNext()) {
                Property property = i.next();
                if (!property.isStored())
                    throw new RuntimeException(getString("logics.policy.forbidden.to.create.indexes.on.non.regular.properties") + " (" + baseProperty + ")");
                if (indexTable.findProperty(property.field.name) == null)
                    throw new RuntimeException(getString("logics.policy.forbidden.to.create.indexes.on.properties.in.different.tables", baseProperty, property));
                tableIndex.add(property.field.name);
            }
            mapIndexes.get(indexTable).add(tableIndex);
        }

        // запишем новое состояние таблиц (чтобы потом изменять можно было бы)
        outDB.writeInt(mapIndexes.size());
        for (Map.Entry<ImplementTable, Set<List<String>>> mapIndex : mapIndexes.entrySet()) {
            mapIndex.getKey().serialize(outDB);
            outDB.writeInt(mapIndex.getValue().size());
            for (List<String> index : mapIndex.getValue()) {
                outDB.writeInt(index.size());
                for (String indexField : index)
                    outDB.writeUTF(indexField);
            }
        }

        Collection<Property> storedProperties = new ArrayList<Property>(getStoredProperties());
        // запишем новое состояние таблиц (чтобы потом изменять можно было бы)
        outDB.writeInt(storedProperties.size());
        for (Property<?> property : storedProperties) {
            outDB.writeUTF(property.getSID());
            outDB.writeUTF(property.mapTable.table.name);
            for (Map.Entry<? extends PropertyInterface, KeyField> mapKey : property.mapTable.mapKeys.entrySet()) {
                outDB.writeInt(mapKey.getKey().ID);
                outDB.writeUTF(mapKey.getValue().name);
            }
        }

        // если не совпали sID или идентификаторы из базы удаляем сразу
        Map<String, SerializedTable> prevTables = new HashMap<String, SerializedTable>();
        for (int i = inputDB == null ? 0 : inputDB.readInt(); i > 0; i--) {
            SerializedTable prevTable = new SerializedTable(inputDB, LM.baseClass);
            prevTables.put(prevTable.name, prevTable);

            for (int j = inputDB.readInt(); j > 0; j--) {
                List<String> index = new ArrayList<String>();
                for (int k = inputDB.readInt(); k > 0; k--)
                    index.add(inputDB.readUTF());
                ImplementTable implementTable = implementTables.get(prevTable.name);
                if (implementTable == null || !mapIndexes.get(implementTable).remove(index))
                    sql.dropIndex(prevTable.name, index);
            }
        }

        // добавим таблицы которых не было
        for (ImplementTable table : implementTables.values()) {
            if (!prevTables.containsKey(table.name))
                sql.createTable(table.name, table.keys);
        }

        Set<ImplementTable> packTables = new HashSet<ImplementTable>();

        // бежим по свойствам
        int prevStoredNum = inputDB == null ? 0 : inputDB.readInt();
        for (int i = 0; i < prevStoredNum; i++) {
            String sID = inputDB.readUTF();
            SerializedTable prevTable = prevTables.get(inputDB.readUTF());
            Map<Integer, KeyField> mapKeys = new HashMap<Integer, KeyField>();
            for (int j = 0; j < prevTable.keys.size(); j++)
                mapKeys.put(inputDB.readInt(), prevTable.findKey(inputDB.readUTF()));

            boolean keep = false;
            for (Iterator<Property> is = storedProperties.iterator(); is.hasNext();) {
                Property<?> property = is.next();
                if (property.getSID().equals(sID)) {
                    Map<KeyField, PropertyInterface> foundInterfaces = new HashMap<KeyField, PropertyInterface>();
                    for (PropertyInterface propertyInterface : property.interfaces) {
                        KeyField mapKeyField = mapKeys.get(propertyInterface.ID);
                        if (mapKeyField != null) foundInterfaces.put(mapKeyField, propertyInterface);
                    }
                    if (foundInterfaces.size() == mapKeys.size()) { // если все нашли
                        if (!(keep = property.mapTable.table.name.equals(prevTable.name))) { // если в другой таблице
                            sql.addColumn(property.mapTable.table.name, property.field);
                            // делаем запрос на перенос

                            logger.info(getString("logics.info.property.is.transferred.from.table.to.table", property.field, property.caption, prevTable.name, property.mapTable.table.name));
                            property.mapTable.table.moveColumn(sql, property.field, prevTable,
                                    BaseUtils.join(foundInterfaces, (Map<PropertyInterface, KeyField>) property.mapTable.mapKeys), prevTable.findProperty(sID));
                            logger.info("Done");
                        } else // надо проверить что тип не изменился
                            if (!prevTable.findProperty(sID).type.equals(property.field.type))
                                sql.modifyColumn(property.mapTable.table.name, property.field, prevTable.findProperty(sID).type);
                        is.remove();
                    }
                    break;
                }
            }
            if (!keep) {
                sql.dropColumn(prevTable.name, sID);
                ImplementTable table = implementTables.get(prevTable.name); // надо упаковать таблицу если удалили колонку
                if (table != null) packTables.add(table);
            }
        }

        Collection<AggregateProperty> recalculateProperties = new ArrayList<AggregateProperty>();
        for (Property property : storedProperties) { // добавляем оставшиеся
            sql.addColumn(property.mapTable.table.name, property.field);
            if (struct != null && property instanceof AggregateProperty) // если все свойства "новые" то ничего перерасчитывать не надо
                recalculateProperties.add((AggregateProperty) property);
        }

        // удаляем таблицы старые
        for (String table : prevTables.keySet())
            if (!implementTables.containsKey(table))
                sql.dropTable(table);

        packTables(sql, packTables); // упакуем таблицы

        updateStats();  // пересчитаем статистику

        // создадим индексы в базе
        for (Map.Entry<ImplementTable, Set<List<String>>> mapIndex : mapIndexes.entrySet())
            for (List<String> index : mapIndex.getValue())
                sql.addIndex(mapIndex.getKey().name, index);

        try {
            sql.insertRecord(StructTable.instance, new HashMap<KeyField, DataObject>(), Collections.singletonMap(StructTable.instance.struct, (ObjectValue) new DataObject((Object) outDBStruct.toByteArray(), ByteArrayClass.instance)), true);
        } catch (Exception e) {
            Map<PropertyField, ObjectValue> propFields = Collections.singletonMap(StructTable.instance.struct, (ObjectValue) new DataObject((Object) new byte[0], ByteArrayClass.instance));
            sql.insertRecord(StructTable.instance, new HashMap<KeyField, DataObject>(), propFields, true);
        }

        fillIDs();

        updateClassStat(sql);

        recalculateAggregations(sql, recalculateProperties); // перерасчитаем агрегации
//        recalculateAggregations(sql, getAggregateStoredProperties());

        sql.commitTransaction();
    }

    public void synchronizeTables() {
        ImportField tableSidField = new ImportField(LM.sidTable);
        ImportField tableKeySidField = new ImportField(LM.sidTableKey);
        ImportField tableKeyNameField = new ImportField(LM.nameTableKey);
        ImportField tableKeyClassField = new ImportField(LM.classTableKey);
        ImportField tableColumnSidField = new ImportField(LM.sidTableColumn);

        ImportKey<?> tableKey = new ImportKey(LM.table, LM.sidToTable.getMapping(tableSidField));
        ImportKey<?> tableKeyKey = new ImportKey(LM.tableKey, LM.sidToTableKey.getMapping(tableKeySidField));
        ImportKey<?> tableColumnKey = new ImportKey(LM.tableColumn, LM.sidToTableColumn.getMapping(tableColumnSidField));

        List<List<Object>> data = new ArrayList<List<Object>>();
        for(ImplementTable implementTable : LM.tableFactory.getImplementTables().values()) {
            Object tableName = implementTable.name;
            Map classes = implementTable.getClasses().getCommonParent(implementTable.keys);
            for (KeyField key : implementTable.keys) {
                data.add(Arrays.asList(tableName, key.name, tableName + "." + key.name, ((ValueClass) classes.get(key)).getCaption()));
            }
        }

        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
        properties.add(new ImportProperty(tableSidField, LM.sidTable.getMapping(tableKey)));
        properties.add(new ImportProperty(tableKeySidField, LM.sidTableKey.getMapping(tableKeyKey)));
        properties.add(new ImportProperty(tableKeyNameField, LM.nameTableKey.getMapping(tableKeyKey)));
        properties.add(new ImportProperty(tableSidField, LM.tableTableKey.getMapping(tableKeyKey), LM.object(LM.table).getMapping(tableKey)));
        properties.add(new ImportProperty(tableKeyClassField, LM.classTableKey.getMapping(tableKeyKey)));

        List<ImportDelete> deletes = new ArrayList<ImportDelete>();
        deletes.add(new ImportDelete(tableKey, LM.is(LM.table).getMapping(tableKey), false));
        deletes.add(new ImportDelete(tableKeyKey, LM.is(LM.tableKey).getMapping(tableKeyKey), false));

        ImportTable table = new ImportTable(Arrays.asList(tableSidField, tableKeyNameField, tableKeySidField, tableKeyClassField), data);

        List<List<Object>> data2 = new ArrayList<List<Object>>();
        for(ImplementTable implementTable : LM.tableFactory.getImplementTables().values()) {
            Object tableName = implementTable.name;
            for (PropertyField property : implementTable.properties) {
                data2.add(Arrays.asList(tableName, property.name));
            }
        }

        List<ImportProperty<?>> properties2 = new ArrayList<ImportProperty<?>>();
        properties2.add(new ImportProperty(tableSidField, LM.sidTable.getMapping(tableKey)));
        properties2.add(new ImportProperty(tableColumnSidField, LM.sidTableColumn.getMapping(tableColumnKey)));
        properties2.add(new ImportProperty(tableSidField, LM.tableTableColumn.getMapping(tableColumnKey), LM.object(LM.table).getMapping(tableKey)));

        List<ImportDelete> deletes2 = new ArrayList<ImportDelete>();
        deletes2.add(new ImportDelete(tableColumnKey, LM.is(LM.tableColumn).getMapping(tableColumnKey), false));

        ImportTable table2 = new ImportTable(Arrays.asList(tableSidField, tableColumnSidField), data2);

        try {
            DataSession session = createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(tableKey, tableKeyKey), properties, deletes);
            service.synchronize(true, false);

            service = new IntegrationService(session, table2, Arrays.asList(tableKey, tableColumnKey), properties2, deletes2);
            service.synchronize(true, false);

            if (session.hasChanges()) {
                session.apply(this);
            }

            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void recalculateStats() throws SQLException {
        DataSession session = createSession();
        for (ImplementTable implementTable : LM.tableFactory.getImplementTables().values()) {
            implementTable.calculateStat(LM, session);
        }
        if (session.hasChanges()) {
            session.apply(this);
        }
    }

    public void updateStats() throws SQLException {
        updateStats(true); // чтобы сами таблицы статистики получили статистику
        if(!"true".equals(System.getProperty("platform.server.logics.donotcalculatestats")))
            updateStats(false);
    }

    public void updateStats(boolean statDefault) throws SQLException {
        DataSession session = createSession();
        for (ImplementTable implementTable : LM.tableFactory.getImplementTables().values()) {
            implementTable.updateStat(LM, session, statDefault);
        }
    }

    protected LP getLP(String sID) {
        return LM.getLP(sID);
    }

    private boolean intersect(LP[] props) {
        for (int i = 0; i < props.length; i++)
            for (int j = i + 1; j < props.length; j++)
                if (((LP<?>) props[i]).intersect((LP<?>) props[j]))
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
            for (Property prop : getProperties()) {
                logger.debug("Checking property : " + prop + "...");
                assert prop.check();
            }
        for (LP[] props : LM.checkCUProps) {
            logger.debug("Checking class properties : " + props + "...");
            assert !intersect(props);
        }
        for (LP[] props : LM.checkSUProps) {
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

    public void recalculateAggregations(SQLSession session, Collection<AggregateProperty> recalculateProperties) throws SQLException {

        for (Property property : getPropertyList())
            if (property instanceof AggregateProperty) {
                AggregateProperty dependProperty = (AggregateProperty) property;
                if (recalculateProperties.contains(dependProperty))
                    dependProperty.recalculateAggregation(session);
            }
    }

    public void recalculateAggregationTableColumn(SQLSession session, String propertySID) throws SQLException {
        for (Property property : getPropertyList())
            if (property.isStored() && property instanceof AggregateProperty && property.getSID().equals(propertySID)) {
                AggregateProperty aggregateProperty = (AggregateProperty) property;
                aggregateProperty.recalculateAggregation(session);
            }
    }

    void packTables(SQLSession session, Collection<ImplementTable> tables) throws SQLException {
        for (Table table : tables) {
            logger.debug(getString("logics.info.packing.table")+" (" + table + ")... ");
            session.packTable(table);
            logger.debug("Done");
        }
    }

    protected Scheduler scheduler;

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
        this.scheduler.start();
    }

    protected LP addRestartActionProp() {
        return LM.addProperty(null, new LP<ClassPropertyInterface>(new RestartActionProperty(LM.genSID(), "")));
    }

    protected LP addCancelRestartActionProp() {
        return LM.addProperty(null, new LP<ClassPropertyInterface>(new CancelRestartActionProperty(LM.genSID(), "")));
    }

    protected LP addGarbageCollectorActionProp() {
        return LM.addProperty(null, new LP<ClassPropertyInterface>(new GarbageCollectorActionProperty(LM.genSID(), getString("logics.garbage.collector"))));
    }

    public void updateEnvironmentProperty(Property property, ObjectValue value) {
        synchronized (navigators) {
            for (RemoteNavigator remoteNavigator : navigators.values()) {
                remoteNavigator.updateEnvironmentProperty(property, value);
            }
        }
    }

    private void updateRestartProperty() {
        Boolean isRestarting = getRestartController().isPendingRestart() ? Boolean.TRUE : null;
        updateEnvironmentProperty(LM.isServerRestarting.property, ObjectValue.getValue(isRestarting, LogicalClass.instance));
    }

    public class RestartActionProperty extends ActionProperty {
        private RestartActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
        }

        public void execute(ExecutionContext context) throws SQLException {
            getRestartController().initRestart();
            updateRestartProperty();
        }
    }

    public class CancelRestartActionProperty extends ActionProperty {
        private CancelRestartActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
        }

        public void execute(ExecutionContext context) throws SQLException {
            getRestartController().cancelRestart();
            updateRestartProperty();
        }
    }

    public class GarbageCollectorActionProperty extends ActionProperty {
        private GarbageCollectorActionProperty(String sid, String caption) {
            super(sid, caption, new ValueClass[]{});
        }

        public void execute(ExecutionContext context) {
            System.runFinalization();
            System.gc();
        }
    }

    @IdentityLazy
    private synchronized RestartController getRestartController() {
        return new RestartController(this);
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
        for (Map.Entry<ImplementTable, Collection<Property>> groupTable : BaseUtils.group(new BaseUtils.Group<ImplementTable, Property>() {
            public ImplementTable group(Property key) {
                return key.mapTable.table;
            }
        }, getStoredProperties()).entrySet()) {
            result += groupTable.getKey().outputKeys() + '\n';
            for (Property property : groupTable.getValue())
                result += '\t' + property.outputStored(false) + '\n';
        }
        result += '\n' + getString("logics.info.by.properties") + '\n' + '\n';
        for (Property property : getStoredProperties())
            result += property.outputStored(true) + '\n';
        System.out.println(result);
    }

    public static int generateStaticNewID() {
        return BaseLogicsModule.generateStaticNewID();
    }

    public int generateNewID() throws RemoteException {
        return generateStaticNewID();
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
            Map<ValueClassWrapper, Integer> classes = new HashMap<ValueClassWrapper, Integer>();
            int size = inStream.readInt();
            for (int i = 0; i < size; i++) {
                Integer ID = inStream.readInt();
                ValueClass valueClass = TypeSerializer.deserializeValueClass(this, inStream);
                classes.put(new ValueClassWrapper(valueClass), ID);

                int groupId = inStream.readInt();
                if (groupId >= 0) {
                    groupMap.put(ID, groupId);
                }
            }

            ArrayList<Property> result = new ArrayList<Property>();
            ArrayList<ArrayList<Integer>> idResult = new ArrayList<ArrayList<Integer>>();

            addProperties(classes, groupMap, result, idResult, isCompulsory, isAny);

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

    private void addProperties(Map<ValueClassWrapper, Integer> classes, Map<Integer, Integer> groupMap, ArrayList<Property> result, ArrayList<ArrayList<Integer>> idResult, boolean isCompulsory, boolean isAny) {
        Set<Integer> allGroups = new HashSet<Integer>(groupMap.values());
        List<List<ValueClassWrapper>> classLists = new ArrayList<List<ValueClassWrapper>>();

        for (Set<ValueClassWrapper> classSet : new Subsets<ValueClassWrapper>(classes.keySet())) {
            List<ValueClassWrapper> classList = new ArrayList<ValueClassWrapper>(classSet);
            Set<Integer> classesGroups = new HashSet<Integer>();
            for (ValueClassWrapper wrapper : classList) {
                int id = classes.get(wrapper);
                if (groupMap.containsKey(id)) {
                    classesGroups.add(groupMap.get(id));
                }
            }
            if ((isCompulsory && classesGroups.size() == allGroups.size()) ||
                    (!isCompulsory && classesGroups.size() > 0 || groupMap.isEmpty()) || classList.isEmpty()) {
                classLists.add(classList);
            }
        }

        for (PropertyClassImplement implement : LM.rootGroup.getProperties(classLists, isAny)) {
            result.add(implement.property);
            ArrayList<Integer> ids = new ArrayList<Integer>();
            for (Object iface : implement.property.interfaces) {
                ids.add(classes.get(implement.mapping.get(iface)));
            }
            idResult.add(ids);
        }
    }

    // создает форму не интерактивную (не для чтения)
    public RemoteFormInterface createForm(DataSession session, FormEntity formEntity, Map<ObjectEntity, DataObject> mapObjects) {
        try {
            FormInstance<T> formInstance = new FormInstance<T>(formEntity, (T) this, session, PolicyManager.serverSecurityPolicy, null, null, new DataObject(getServerComputer(), LM.computer), mapObjects, false);
            if(!formInstance.areObjectsFounded())
                return null;
            return new RemoteForm<T, FormInstance<T>>(formInstance, formEntity.getRichDesign(), exportPort, null);
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
                User user = readUser(username, session);
                if (user == null) {
                    throw new LoginException();
                }

                String password = (String) LM.userPassword.read(session, new DataObject(user.ID, LM.customUser));
                if (password != null) {
                    password = password.trim();
                }

                return new UserInfo(username, password, getUserRolesNames(username));
            } finally {
                session.close();
            }
        } catch (SQLException se) {
            throw new RuntimeException(getString("logics.info.error.reading.user.data"), se);
        }
    }

    public boolean getUseUniPass() throws RemoteException {
        return Settings.instance.getUseUniPass();
    }

    private List<String> getUserRolesNames(String username) {
        try {
            DataSession session = createSession();
            try {
                Map<String, KeyExpr> keys = KeyExpr.getMapKeys(asList("user", "role"));
                Expr userExpr = keys.get("user");
                Expr roleExpr = keys.get("role");

                Query<String, String> q = new Query<String, String>(keys);
                q.and(LM.inUserMainRole.getExpr(session.modifier, userExpr, roleExpr).getWhere());
                q.and(LM.userLogin.getExpr(session.modifier, userExpr).compare(new DataObject(username), Compare.EQUALS));

                q.properties.put("roleName", LM.userRoleSID.getExpr(session.modifier, roleExpr));

                OrderedMap<Map<String, Object>, Map<String, Object>> values = q.execute(session.sql);

                List<String> roles = new ArrayList<String>();
                for (Map<String, Object> value : values.values()) {
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

    private User authenticateUser(String login, String password) throws LoginException, SQLException {
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
    }

    public ArrayList<IDaemonTask> getDaemonTasks(int compId) {
        return new ArrayList<IDaemonTask>();
    }

    @Override
    public void remindPassword(String email) throws RemoteException {
        throw new UnsupportedOperationException("Напоминание пароля не поддерживается...");
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
            DataObject propertyObject = new DataObject(LM.SIDToProperty.read(session, new DataObject(propertySid)), LM.property);
            default2 = (Boolean) LM.forbidViewProperty.read(session, propertyObject);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return default1 && default2 == null;
    }

    @Override
    public byte[] readFile(String sid, String... params) throws RemoteException {
        LP property = getLP(sid);
        List<ClassPropertyInterface> interfaces = new ArrayList<ClassPropertyInterface>(property.property.interfaces);
        DataObject[] objects = new DataObject[interfaces.size()];
        byte[] fileBytes;
        try {
            DataSession session = createSession();
            for (int i = 0; i < interfaces.size(); i++) {
                objects[i] = session.getDataObject(Integer.decode(params[i]), interfaces.get(i).interfaceClass.getType());
            }
            fileBytes = (byte[]) property.read(session, objects);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return fileBytes;
    }
}
