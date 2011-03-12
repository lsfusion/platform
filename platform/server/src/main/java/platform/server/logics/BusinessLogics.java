package platform.server.logics;

import net.sf.jasperreports.engine.JRException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.FileSystemResource;
import platform.base.*;
import platform.base.identity.DefaultIDGenerator;
import platform.base.identity.IDGenerator;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.RemoteLogicsInterface;
import platform.interop.action.*;
import platform.interop.exceptions.LoginException;
import platform.interop.form.RemoteFormInterface;
import platform.interop.form.screen.ExternalScreen;
import platform.interop.form.screen.ExternalScreenParameters;
import platform.interop.navigator.RemoteNavigatorInterface;
import platform.interop.remote.RemoteObject;
import platform.interop.remote.ServerSocketFactory;
import platform.server.Settings;
import platform.server.auth.PolicyManager;
import platform.server.auth.SecurityPolicy;
import platform.server.auth.User;
import platform.server.caches.IdentityLazy;
import platform.server.classes.*;
import platform.server.data.*;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.OrderType;
import platform.server.data.query.Query;
import platform.server.data.sql.DataAdapter;
import platform.server.data.sql.PostgreDataAdapter;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.Type;
import platform.server.data.type.TypeSerializer;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.*;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.navigator.*;
import platform.server.form.view.ContainerView;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.integration.*;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.*;
import platform.server.logics.property.derived.*;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.property.group.PropertySet;
import platform.server.logics.scheduler.Scheduler;
import platform.server.logics.table.ImplementTable;
import platform.server.logics.table.TableFactory;
import platform.server.net.ServerInstanceLocator;
import platform.server.net.ServerInstanceLocatorSettings;
import platform.server.serialization.ServerSerializationPool;
import platform.server.session.DataSession;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIFailureHandler;
import java.rmi.server.RMISocketFactory;
import java.sql.SQLException;
import java.util.*;

// @GenericImmutable нельзя так как Spring валится

public abstract class BusinessLogics<T extends BusinessLogics<T>> extends RemoteObject implements RemoteLogicsInterface {
    protected final static Logger logger = Logger.getLogger(BusinessLogics.class);

    //время жизни неиспользуемого навигатора - 3 часа по умолчанию
    private static final long MAX_FREE_NAVIGATOR_LIFE_TIME = Long.parseLong(System.getProperty("platform.server.navigatorMaxLifeTime", Long.toString(3L * 3600L * 1000L)));

    public byte[] findClass(String name) {

        InputStream inStream = getClass().getClassLoader().getResourceAsStream(name.replace('.', '/') + ".class");

        try {
            byte[] b = new byte[inStream.available()];
            inStream.read(b);
            return b;
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при считывании класса на сервере", e);
        } finally {
            try {
                inStream.close();
            } catch (IOException e) {
                throw new RuntimeException("Ошибка при считывании класса на сервере", e);
            }
        }
    }

    private static BusinessLogics BL;
    private static Registry registry;

    private static Boolean stopped = false;
    private static final Object serviceMonitor = new Object();

    public static void start(String[] args) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException, JRException {

        // делаем, чтобы сборщик мусора срабатывал каждую минуту - для удаления ненужных connection'ов

        if (System.getProperty("sun.rmi.dgc.server.gcInterval") == null) {
            System.setProperty("sun.rmi.dgc.server.gcInterval", "600000");
        }

        System.setProperty("mail.mime.encodefilename", "true");

        initRMISocketFactory();

        stopped = false;

        logger.info("Server is starting...");

        XmlBeanFactory factory = new XmlBeanFactory(new FileSystemResource("conf/settings.xml"));

        if (factory.containsBean("settings")) {
            Settings.instance = (Settings) factory.getBean("settings");
        } else {
            Settings.instance = new Settings();
        }

        BL = (BusinessLogics) factory.getBean("businessLogics");
        registry = LocateRegistry.createRegistry(BL.getExportPort());

//        registry.rebind("BusinessLogics", BL);
        registry.rebind("BusinessLogicsLoader", new BusinessLogicsLoader(BL));

        logger.info("Server has successfully started");

        if (factory.containsBean("serverInstanceLocatorSettings")) {
            ServerInstanceLocatorSettings settings = (ServerInstanceLocatorSettings) factory.getBean("serverInstanceLocatorSettings");
            new ServerInstanceLocator().start(settings, BL.getExportPort());

            logger.info("Server instance locator successfully started");
        }

        synchronized (serviceMonitor) {
            while (!stopped) {
                try {
                    serviceMonitor.wait();
                } catch (InterruptedException e) {
                }
            }
        }

        logger.info("Server has successfully stopped");
    }

    private static void initRMISocketFactory() throws IOException {
        RMISocketFactory socketFactory = RMISocketFactory.getSocketFactory();
        if (socketFactory == null) {
            socketFactory = RMISocketFactory.getDefaultSocketFactory();
        }

        socketFactory = new ServerSocketFactory();

        RMISocketFactory.setFailureHandler(new RMIFailureHandler() {

            public boolean failure(Exception ex) {
                return true;
            }
        });

        RMISocketFactory.setSocketFactory(socketFactory);
    }

    public static void stop(String[] args) throws RemoteException, NotBoundException {

        stopped = true;

        logger.info("Server is stopping...");

        registry.unbind("BusinessLogicsLoader");

        registry = null;
        BL = null;

        synchronized (serviceMonitor) {
            serviceMonitor.notify();
        }
    }

    public static void main(String[] args) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException, JRException {
        start(args);
    }

    public final static SQLSyntax debugSyntax = new PostgreDataAdapter();

    protected DataAdapter adapter;
    protected SQLSession sql;

    public SQLSyntax getAdapter() {
        return adapter;
    }

    public final static boolean activateCaches = true;

    final Map<Pair<String, Integer>, RemoteNavigator> navigators = new HashMap<Pair<String, Integer>, RemoteNavigator>();

    public RemoteNavigatorInterface createNavigator(String login, String password, int computer) {
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
            String checkPassword = (String) userPassword.read(session, new DataObject(user.ID, customUser));
            if (checkPassword != null && !password.trim().equals(checkPassword.trim())) {
                throw new LoginException();
            }

            Pair<String, Integer> key = new Pair<String, Integer>(login, computer);
            RemoteNavigator navigator = navigators.get(key);

            if (navigator != null) {
                navigator.invalidate();
            } else {
                navigator = new RemoteNavigator(this, user, computer, exportPort);
                synchronized (navigators) {
                    navigators.put(key, navigator);
                }
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

    public void removeNavigator(RemoteNavigator remoteNavigator) {
        removeExpiredNavigators();
        synchronized (navigators) {
            for (Iterator<Map.Entry<Pair<String, Integer>, RemoteNavigator>> iterator = navigators.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry<Pair<String, Integer>, RemoteNavigator> entry = iterator.next();
                if (entry.getValue() == remoteNavigator) {
                    iterator.remove();
                    break;
                }
            }
            getRestartController().forcedRestartIfAllowed();
        }
    }

    private void removeExpiredNavigators() {
        synchronized (navigators) {
            for (Iterator<Map.Entry<Pair<String, Integer>, RemoteNavigator>> iterator = navigators.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry<Pair<String, Integer>, RemoteNavigator> entry = iterator.next();
                long suspendTime = System.currentTimeMillis() - entry.getValue().getLastUsedTime();
                if (suspendTime > MAX_FREE_NAVIGATOR_LIFE_TIME) {
                    iterator.remove();
                }
            }
            getRestartController().forcedRestartIfAllowed();
        }
    }

    public boolean checkUser(String login, String password) {
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
            String checkPassword = (String) userPassword.read(session, new DataObject(user.ID, customUser));
            if (checkPassword != null && !password.trim().equals(checkPassword.trim())) {
                throw new LoginException();
            }

            return true;
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

    public TimeZone getTimeZone() {
        return Calendar.getInstance().getTimeZone();
    }

    // счетчик идентификаторов
    static private IDGenerator idGenerator = new DefaultIDGenerator();

    int idShift(int offs) {
        return idGenerator.idShift(offs);
    }

    protected AbstractCustomClass transaction, barcodeObject;
    protected AbstractCustomClass namedUniqueObject;

    public AbstractCustomClass user;
    public ConcreteCustomClass systemUser;
    public ConcreteCustomClass customUser;
    public ConcreteCustomClass computer;
    public ConcreteCustomClass policy;
    public ConcreteCustomClass session;
    public ConcreteCustomClass userRole;
    public ConcreteCustomClass country;
    public ConcreteCustomClass navigatorElement;
    public ConcreteCustomClass form;
    public ConcreteCustomClass dictionary;
    public ConcreteCustomClass dictionaryEntry;

    public Integer getComputer(String strHostName) {
        try {
            Integer result;
            DataSession session = createSession();

            Query<String, Object> q = new Query<String, Object>(Collections.singleton("key"));
            q.and(
                    hostname.getExpr(
                            session.modifier, q.mapKeys.get("key")
                    ).compare(new DataObject(strHostName), Compare.EQUALS)
            );

            Set<Map<String, Object>> keys = q.execute(session.sql, session.env).keySet();
            if (keys.size() == 0) {
                DataObject addObject = session.addObject(computer, session.modifier);
                hostname.execute(strHostName, session, session.modifier, addObject);

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
            ConcreteClass result = type.getDataClass(object, session.sql, baseClass);
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
            DataObject addObject = session.addObject(customUser, session.modifier);
            userLogin.execute(login, session, session.modifier, addObject);
            userPassword.execute(defaultPassword, session, session.modifier, addObject);
            Integer userID = (Integer) addObject.object;
            session.apply(this);
            user = new User(userID);
            policyManager.putUser(userID, user);
        }

        session.close();

        return user;
    }

    private User readUser(String login, DataSession session) throws SQLException {
        Integer userId = (Integer) loginToUser.read(session, new DataObject(login, StringClass.get(30)));
        if (userId == null) {
            return null;
        }

        User userObject = policyManager.getUser(userId);
        if (userObject == null) {
            userObject = new User(userId);
            policyManager.putUser(userId, userObject);
        }

        List<Integer> userPoliciesIds = readUserPoliciesIds(userId);
        for (int policyId : userPoliciesIds) {
            SecurityPolicy policy = policyManager.getPolicy(policyId);
            if (policy != null) {
                userObject.addSecurityPolicy(policy);
            }
        }

        setAvailableForms(userObject);
        return userObject;
    }

    private List<Integer> readUserPoliciesIds(Integer userId) {
        try {
            ArrayList<Integer> result = new ArrayList<Integer>();
            DataSession session = createSession();

            Query<String, Object> q = new Query<String, Object>(BaseUtils.toList("userId", "policyId"));
            Expr orderExpr = userPolicyOrder.getExpr(session.modifier, q.mapKeys.get("userId"), q.mapKeys.get("policyId"));

            q.properties.put("pOrder", orderExpr);
            q.and(orderExpr.getWhere());
            q.and(q.mapKeys.get("userId").compare(new DataObject(userId, customUser), Compare.EQUALS));

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

    private void setAvailableForms(User user) {
        SecurityPolicy policy = new SecurityPolicy(-1);
        policy.navigator.replaceMode = true;
        try {
            DataSession session = createSession();

            Query<String, String> q = new Query<String, String>(BaseUtils.toList("userId", "formId"));
            Expr expr = permissionUserForm.getExpr(session.modifier, q.mapKeys.get("userId"), q.mapKeys.get("formId"));
            q.and(expr.getWhere());
            q.and(q.mapKeys.get("userId").compare(new DataObject(user.ID, customUser), Compare.EQUALS));

            q.properties.put("sid", navigatorElementSID.getExpr(session.modifier, q.mapKeys.get("formId")));

            Collection<Map<String, Object>> values = q.execute(session.sql).values();
            for (Map<String, Object> valueMap : values) {
                String sid = (String) valueMap.get("sid");
                policy.navigator.deny(baseElement.getNavigatorElement(sid.trim()));
            }
            user.addSecurityPolicy(policy);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected SecurityPolicy addPolicy(String policyName, String description) throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        DataSession session = createSession();

        Integer policyID = readPolicy(policyName, session);
        if (policyID == null) {
            DataObject addObject = session.addObject(policy, session.modifier);
            name.execute(policyName, session, session.modifier, addObject);
            policyDescription.execute(description, session, session.modifier, addObject);
            policyID = (Integer) addObject.object;
            session.apply(this);
        }

        session.close();

        SecurityPolicy policyObject = new SecurityPolicy(policyID);
        policyManager.putPolicy(policyID, policyObject);
        return policyObject;
    }

    private Integer readPolicy(String name, DataSession session) throws SQLException {
        return (Integer) nameToPolicy.read(session, new DataObject(name, StringClass.get(50)));
    }

    protected LP groeq2;
    protected LP greater2, less2;
    protected LP greater22, less22;
    protected LP between;
    protected LP object1, and1, andNot1;
    protected LP equals2, diff2;
    protected LP sumDouble2;
    protected LP subtractDouble2;
    protected LP deltaDouble2;
    protected LP multiplyDouble2;
    protected LP multiplyIntegerBy2;
    protected LP squareInteger;
    protected LP squareDouble;
    protected LP sqrtDouble2;
    protected LP divideDouble;
    protected LP divideDouble2;
    protected LP addDate2;
    protected LP string2;
    protected LP insensitiveString2;
    protected LP concat2;
    protected LP percent;
    protected LP percent2;
    protected LP yearInDate;

    protected LP vtrue, vzero;
    protected LP positive, negative;

    protected LP castText;

    public LP<?> name;
    public LP<?> date;

    protected LP transactionLater;
    public LP currentDate;
    protected LP currentHour;
    protected LP currentEpoch;
    protected LP currentDateTime;
    public LP currentUser;
    public LP currentSession;
    public LP currentComputer;
    protected LP changeUser;
    protected LP isServerRestarting;
    public LP<PropertyInterface> barcode;
    public LP barcodeToObject;
    protected LP barcodeObjectName;
    public LP barcodePrefix;
    public LP seekBarcodeAction;
    public LP barcodeNotFoundMessage;
    public LP restartServerAction;
    public LP cancelRestartServerAction;
    public LP reverseBarcode;

    public LP userLogin;
    public LP userPassword;
    public LP userFirstName;
    public LP userLastName;
    public LP userMainRole;
    public LP nameUserMainRole;
    public LP userRoleSID;
    public LP sidToRole;
    public LP inUserRole;
    public LP inLoginSID;
    public LP currentUserName;
    public LP<?> loginToUser;

    public LP policyDescription;
    protected LP<?> nameToPolicy;
    public LP userRolePolicyOrder;
    public LP userPolicyOrder;

    public LP hostname;
    public LP onlyNotZero;

    public LP delete;

    public LP objectClass;
    public LP objectClassName;
    public LP classSID;
    public LP navigatorElementSID;
    public LP navigatorElementCaption;

    public LP SIDToNavigatorElement;
    public LP permissionUserRoleForm;
    public LP permissionUserForm;

    public LP customID;
    public LP stringID;
    public LP integerID;
    public LP dateID;

    public LP objectByName;
    public LP seekObjectName;

    protected LP webHost;

    protected LP smtpHost;
    protected LP smtpPort;
    protected LP emailAccount;
    protected LP emailPassword;
    protected LP fromAddress;
    protected LP defaultCountry;

    protected LP sidCountry;
    protected LP generateDatesCountry;
    protected LP sidToCountry;
    protected LP nameToCountry;
    protected LP nameToObject;
    protected LP isDayOffCountryDate;
    LP workingDay, isWorkingDay, workingDaysQuantity, equalsWorkingDaysQuantity;

    protected LP termDictionary;
    protected LP translationDictionary;
    protected LP entryDictionary;
    protected LP translationDictionaryTerm;

    private LP selectRoleForms;
    private LP selectUserRoles;

    private final ConcreteValueClass classSIDValueClass = StringClass.get(250);
    private final StringClass formSIDValueClass = StringClass.get(50);
    private final StringClass formCaptionValueClass = StringClass.get(250);

    public static int genSystemClassID(int id) {
        return 9999976 - id;
    }

    public Property getProperty(String sid) {
        return rootGroup.getProperty(sid);
    }

    public ObjectValueProperty getObjectValueProperty(ValueClass... valueClasses) {
        List<Property> properties = objectValue.getProperties(valueClasses);
        return properties.size() > 0
                ? (ObjectValueProperty) properties.iterator().next()
                : null;
    }

    public abstract class MapClassesPropertySet<K, V extends Property> extends PropertySet {
        protected LinkedHashMap<K, V> properties = new LinkedHashMap<K, V>();

        @Override
        public List<Property> getProperties() {
            return new ArrayList<Property>(properties.values());
        }

        @Override
        protected List<PropertyClassImplement> getProperties(List<ValueClassWrapper> classes) {
            ValueClass[] valueClasses = getClasses(classes);
            V property = getProperty(valueClasses);

            List<?> interfaces = getPropertyInterfaces(property, valueClasses);
            return Collections.singletonList(new PropertyClassImplement(property, classes, interfaces));
        }

        private ValueClass[] getClasses(List<ValueClassWrapper> classes) {
            ValueClass[] valueClasses = new ValueClass[classes.size()];
            for (int i = 0; i < classes.size(); i++) {
                valueClasses[i] = classes.get(i).valueClass;
            }
            return valueClasses;
        }

        protected V getProperty(ValueClass[] classes) {
            K key = createKey(classes);
            if (!properties.containsKey(key)) {
                V property = createProperty(classes);
                properties.put(key, property);
                return property;
            } else {
                return properties.get(key);
            }
        }

        protected abstract List<?> getPropertyInterfaces(V property, ValueClass[] valueClasses);

        protected abstract V createProperty(ValueClass[] classes);

        protected abstract K createKey(ValueClass[] classes);
    }

    public class SelectionPropertySet extends MapClassesPropertySet<Map<ValueClass, Integer>, SelectionProperty> {
        static private final String prefix = "SelectionProperty_";
        private Map<String, LP> selectionLP = new HashMap<String, LP>();

        protected Class<?> getPropertyClass() {
            return SelectionProperty.class;
        }

        @Override
        protected boolean isInInterface(List<ValueClassWrapper> classes) {
            return classes.size() >= 1;
        }

        @Override
        public Property getProperty(String sid) {
            if (sid.startsWith(prefix)) {
                String[] sids = sid.substring(prefix.length()).split("\\|");
                ValueClass[] valueClasses = new ValueClass[sids.length];
                for (int i = 0; i < sids.length; i++) {
                    valueClasses[i] = BusinessLogics.this.findValueClass(sids[i]);
                    assert valueClasses[i] != null;
                }
                return getProperty(valueClasses);
            }
            return null;
        }

        @Override
        protected List<? extends PropertyInterface> getPropertyInterfaces(SelectionProperty property, ValueClass[] classes) {
            int intNum = classes.length;
            PropertyInterface[] interfaces = new PropertyInterface[intNum];
            boolean[] was = new boolean[intNum];
            for (ClassPropertyInterface iface : property.interfaces) {
                for (int i = 0; i < intNum; i++) {
                    if (!was[i] && iface.interfaceClass == classes[i]) {
                        interfaces[i] = iface;
                        was[i] = true;
                        break;
                    }
                }
            }
            return Arrays.asList(interfaces);
        }

        protected Map<ValueClass, Integer> createKey(ValueClass[] classes) {
            Map<ValueClass, Integer> key = new HashMap<ValueClass, Integer>();
            for (ValueClass valueClass : classes) {
                if (key.containsKey(valueClass)) {
                    key.put(valueClass, key.get(valueClass) + 1);
                } else {
                    key.put(valueClass, 1);
                }
            }
            return key;
        }

        private String getSID(ValueClass[] classes) {
            String sid = prefix;
            for (int i = 0; i < classes.length; i++) {
                sid += classes[i].getSID();
                if (i + 1 < classes.length) {
                    sid += '|';
                }
            }
            return sid;
        }

        protected SelectionProperty createProperty(ValueClass[] classes) {
            ValueClass[] classArray = new ValueClass[classes.length];
            String sid = getSID(classes);
            for (int i = 0; i < classes.length; i++) {
                classArray[i] = classes[i];
            }

            SelectionProperty property = new SelectionProperty(sid, classArray);
            LP lp = new LP<ClassPropertyInterface>(property);
            registerProperty(lp);
            selectionLP.put(sid, lp);
            setParent(property);
            return property;
        }

        public LP getLP(ValueClass[] classes) {
            String sid = getSID(classes);
            if (!selectionLP.containsKey(sid)) {
                createProperty(classes);
            }

            return selectionLP.get(sid);
        }

        public LP getLP(ObjectEntity object) {
            return getLP(new ValueClass[]{object.baseClass});
        }
    }

    public SelectionPropertySet selection;

    public class CompositeNamePropertySet extends MapClassesPropertySet<Integer, JoinProperty> {
        private static final String prefix = "CompositeNameProperty_";

        protected Class<?> getPropertyClass() {
            return JoinProperty.class;
        }

        @Override
        protected boolean isInInterface(List<ValueClassWrapper> classes) {
            return classes.size() >= 1;
        }

        @Override
        public Property getProperty(String sid) {
            if (sid.startsWith(prefix)) {
                int cnt = Integer.parseInt(sid.substring(prefix.length()));
                if (!properties.containsKey(cnt)) {
                    createProperty(cnt);
                }
                return properties.get(cnt);
            }
            return null;
        }

        @Override
        protected List<?> getPropertyInterfaces(JoinProperty property, ValueClass[] valueClasses) {
            return new ArrayList(property.interfaces);
        }

        @Override
        protected Integer createKey(ValueClass[] classes) {
            return classes.length;
        }

        @IdentityLazy
        private LP getStringConcatanationProperty(int intNum) {
            return new LP<StringConcatenateProperty.Interface>(new StringConcatenateProperty(genSID(), "Объед.", intNum, ", "));
        }

        @Override
        protected JoinProperty<ClassPropertyInterface> createProperty(ValueClass[] classes) {
            return createProperty(classes.length);
        }

        private JoinProperty<ClassPropertyInterface> createProperty(int intNum) {
            String sid = prefix + intNum;

            Object joinParams[] = new Object[2 * intNum];
            for (int i = 0; i < intNum; i++) {
                joinParams[2 * i] = name;
                joinParams[2 * i + 1] = i + 1;
            }

            LP stringConcat = getStringConcatanationProperty(intNum);

            JoinProperty<ClassPropertyInterface> joinProperty = new JoinProperty(sid, "Составное имя (" + intNum + ")", intNum, false);
            LP listJoinProperty = new LP<JoinProperty.Interface>(joinProperty);
            joinProperty.implement = mapImplement(stringConcat, readImplements(listJoinProperty.listInterfaces, joinParams));

            registerProperty(listJoinProperty);
            setParent(joinProperty);
            return joinProperty;
        }
    }

    protected CompositeNamePropertySet compositeName;

    public class ObjectValuePropertySet extends MapClassesPropertySet<ValueClass, ObjectValueProperty> {
        private Map<String, LP> sidToLP = new HashMap<String, LP>();
        private static final String prefix = "objectValueProperty_";

        @Override
        protected boolean isInInterface(List<ValueClassWrapper> classes) {
            return classes.size() == 1;
        }

        protected Class<?> getPropertyClass() {
            return ObjectValueProperty.class;
        }

        @Override
        public Property getProperty(String sid) {
            if (sid.startsWith(prefix)) {
                ValueClass valueClass = BusinessLogics.this.findValueClass(sid.substring(prefix.length()));
                assert valueClass != null;
                return getProperty(new ValueClass[]{valueClass});
            }
            return null;
        }

        @Override
        protected List<?> getPropertyInterfaces(ObjectValueProperty property, ValueClass[] valueClasses) {
            return Arrays.asList(property.interfaces.iterator().next());
        }

        @Override
        protected ValueClass createKey(ValueClass[] classes) {
            assert classes.length == 1;
            return classes[0].getBaseClass();
        }

        @Override
        protected ObjectValueProperty createProperty(ValueClass[] classes) {
            assert classes.length == 1;

            ValueClass valueClass = classes[0].getBaseClass();

            String sid = prefix + valueClass.getSID();
            ObjectValueProperty property = new ObjectValueProperty(sid, valueClass);
            LP prop = new LP<ClassPropertyInterface>(property);
            registerProperty(prop);
            sidToLP.put(sid, prop);
            setParent(property);
            return property;
        }

        public LP getLP(ValueClass cls) {
            String sid = prefix + cls.getBaseClass().getSID();
            if (!sidToLP.containsKey(sid)) {
                createProperty(new ValueClass[]{cls});
            }
            return sidToLP.get(sid);
        }
    }

    public ObjectValuePropertySet objectValue;

    void initBase() {

        rootGroup = new AbstractGroup("Корневая группа");
        rootGroup.createContainer = false;

        sessionGroup = new AbstractGroup("Сессионные свойства");
        sessionGroup.createContainer = false;
        rootGroup.add(sessionGroup);

        publicGroup = new AbstractGroup("Пользовательские свойства");
        publicGroup.createContainer = false;
        rootGroup.add(publicGroup);

        actionGroup = new AbstractGroup("Действия");
        actionGroup.createContainer = false;
        rootGroup.add(actionGroup);

        privateGroup = new AbstractGroup("Внутренние свойства");
        privateGroup.createContainer = false;
        rootGroup.add(privateGroup);

        baseGroup = new AbstractGroup("Атрибуты");
        baseGroup.createContainer = false;
        publicGroup.add(baseGroup);

        idGroup = new AbstractGroup("Идентификаторы");
        idGroup.createContainer = false;
        publicGroup.add(idGroup);

        selection = new SelectionPropertySet();
        sessionGroup.add(selection);

        objectValue = new ObjectValuePropertySet();
        baseGroup.add(objectValue);

        compositeName = new CompositeNamePropertySet();
        privateGroup.add(compositeName);

        baseClass = addBaseClass("object", "Объект");

        transaction = addAbstractClass("transaction", "Транзакция", baseClass);
        barcodeObject = addAbstractClass("barcodeObject", "Штрих-кодированный объект", baseClass);

        user = addAbstractClass("user", "Пользователь", baseClass);
        customUser = addConcreteClass("customUser", "Обычный пользователь", user, barcodeObject);
        systemUser = addConcreteClass("systemUser", "Системный пользователь", user);
        computer = addConcreteClass("computer", "Рабочее место", baseClass);
        userRole = addConcreteClass("userRole", "Роль", baseClass.named);

        policy = addConcreteClass("policy", "Политика безопасности", baseClass.named);
        session = addConcreteClass("session", "Транзакция", baseClass);

        country = addConcreteClass("country", "Страна", baseClass.named);

        navigatorElement = addConcreteClass("navigatorElement", "Элемент навигатора", baseClass);
        form = addConcreteClass("form", "Форма", navigatorElement);
        dictionary = addConcreteClass("dictionary", "Словарь", baseClass.named);
        dictionaryEntry = addConcreteClass("dictionaryEntry", "Слова", baseClass);

        tableFactory = new TableFactory();
        for (int i = 0; i < TableFactory.MAX_INTERFACE; i++) { // заполним базовые таблицы
            CustomClass[] baseClasses = new CustomClass[i];
            for (int j = 0; j < i; j++)
                baseClasses[j] = baseClass;
            tableFactory.include("base_" + i, baseClasses);
        }

        baseElement = new NavigatorElement<T>("baseElement", "Base Group");
    }

    void initBaseProperties() {
        // математические св-ва
        equals2 = addCFProp(Compare.EQUALS);
        object1 = addAFProp();
        and1 = addAFProp(false);
        andNot1 = addAFProp(true);
        string2 = addSProp(2);
        insensitiveString2 = addInsensitiveSProp(2);
        concat2 = addCCProp(2);
        groeq2 = addCFProp(Compare.GREATER_EQUALS);
        greater2 = addCFProp(Compare.GREATER);
        less2 = addCFProp(Compare.LESS);
        greater22 = addJProp(greater2, concat2, 1, 2, concat2, 3, 4);
        less22 = addJProp(less2, concat2, 1, 2, concat2, 3, 4);
        diff2 = addCFProp(Compare.NOT_EQUALS);
        sumDouble2 = addSFProp("((prm1)+(prm2))", DoubleClass.instance, 2);
        subtractDouble2 = addSFProp("((prm1)-(prm2))", DoubleClass.instance, 2);
        deltaDouble2 = addSFProp("abs((prm1)-(prm2))", DoubleClass.instance, 2);
        multiplyDouble2 = addMFProp(DoubleClass.instance, 2);
        multiplyIntegerBy2 = addSFProp("((prm1)*2)", IntegerClass.instance, 1);
        squareInteger = addSFProp("(prm1)*(prm1)", IntegerClass.instance, 1);
        squareDouble = addSFProp("(prm1)*(prm1)", DoubleClass.instance, 1);
        sqrtDouble2 = addSFProp("round(sqrt(prm1),2)", DoubleClass.instance, 1);
        divideDouble = addSFProp("((prm1)/(prm2))", DoubleClass.instance, 2);
        divideDouble2 = addSFProp("round(CAST((CAST((prm1) as numeric)/(prm2)) as numeric),2)", DoubleClass.instance, 2);
        addDate2 = addSFProp("((prm1)+(prm2))", DateClass.instance, 2);
        percent = addSFProp("((prm1)*(prm2)/100)", DoubleClass.instance, 2);
        percent2 = addSFProp("round(CAST(((prm1)*(prm2)/100) as numeric), 2)", DoubleClass.instance, 2);
        between = addJProp("Между", and1, groeq2, 1, 2, groeq2, 3, 1);
        vtrue = addCProp("Истина", LogicalClass.instance, true);
        vzero = addCProp("0", DoubleClass.instance, 0);

        castText = addSFProp("CAST((prm1) as text)", TextClass.instance, 1);

        positive = addJProp(greater2, 1, vzero);
        negative = addJProp(less2, 1, vzero);

        yearInDate = addSFProp("(extract(year from prm1))", IntegerClass.instance, 1);

        delete = addAProp(new DeleteObjectActionProperty(genSID(), baseClass));

        date = addDProp(baseGroup, "date", "Дата", DateClass.instance, transaction);

        sidCountry = addDProp(baseGroup, "sidCountry", "Код страны", IntegerClass.instance, country);
        generateDatesCountry = addDProp(privateGroup, "generateDatesCountry", "Генерировать выходные", LogicalClass.instance, country);
        sidToCountry = addCGProp(null, "sidToCountry", "Страна", object(country), sidCountry, sidCountry, 1);
        isDayOffCountryDate = addDProp(baseGroup, "isDayOffCD", "Выходной", LogicalClass.instance, country, DateClass.instance);

        workingDay = addJProp(baseGroup, "workingDay", "Рабочий", andNot1, addCProp(IntegerClass.instance, 1, country, DateClass.instance), 1, 2, isDayOffCountryDate, 1, 2);
        isWorkingDay = addJProp(baseGroup, "isWorkingDay", "Рабочий", and(false, false), workingDay, 1, 3, groeq2, 3, 2, is(DateClass.instance), 3);
        workingDaysQuantity = addOProp(baseGroup, "workingDaysQuantity", "Рабочих дней", OrderType.SUM, isWorkingDay, true, true, 1, 2, 1, 3);
        equalsWorkingDaysQuantity = addJProp(baseGroup, "equalsWorkingDaysQuantity", "Совпадает количество раб. дней", equals2, object(IntegerClass.instance), 1, workingDaysQuantity, 2, 3, 4);

        transactionLater = addSUProp("Транзакция позже", Union.OVERRIDE, addJProp("Дата позже", greater2, date, 1, date, 2),
                addJProp("", and1, addJProp("Дата=дата", equals2, date, 1, date, 2), 1, 2, addJProp("Код транзакции после", greater2, 1, 2), 1, 2));

        hostname = addDProp(baseGroup, "hostname", "Имя хоста", InsensitiveStringClass.get(100), computer);

        currentDate = addDProp(baseGroup, "currentDate", "Тек. дата", DateClass.instance);
        currentHour = addTProp(Time.HOUR);
        currentEpoch = addTProp(Time.EPOCH);
        currentDateTime = addTProp(Time.DATETIME);
        currentUser = addProperty(null, new LP<PropertyInterface>(new CurrentUserFormulaProperty(genSID(), user)));
        currentSession = addProperty(null, new LP<PropertyInterface>(new CurrentSessionFormulaProperty(genSID(), session)));
        currentComputer = addProperty(null, new LP<PropertyInterface>(new CurrentComputerFormulaProperty(genSID(), computer)));
        isServerRestarting = addProperty(null, new LP<PropertyInterface>(new IsServerRestartingFormulaProperty(genSID())));
        changeUser = addProperty(null, new LP<ClassPropertyInterface>(new ChangeUserActionProperty(genSID(), customUser)));

        userLogin = addDProp(baseGroup, "userLogin", "Логин", StringClass.get(30), customUser);
        loginToUser = addCGProp(null, "loginToUser", "Пользователь", object(customUser), userLogin, userLogin, 1);
        userPassword = addDProp(baseGroup, "userPassword", "Пароль", StringClass.get(30), customUser);
        userFirstName = addDProp(baseGroup, "userFirstName", "Имя", StringClass.get(30), customUser);
        userLastName = addDProp(baseGroup, "userLastName", "Фамилия", StringClass.get(30), customUser);

        userRoleSID = addDProp(baseGroup, "userRoleSID", "Идентификатор", StringClass.get(30), userRole);
        sidToRole = addCGProp(idGroup, "sidToRole", "Роль (ИД)", object(userRole), userRoleSID, userRoleSID, 1);
        inUserRole = addDProp(baseGroup, "inUserRole", "Вкл.", LogicalClass.instance, customUser, userRole);
        inLoginSID = addJProp("inLoginSID", true, "Логину назначена роль", inUserRole, loginToUser, 1, sidToRole, 2);

        name = addCUProp(baseGroup, "commonName", "Имя", addDProp("name", "Имя", InsensitiveStringClass.get(60), baseClass.named),
                addJProp(insensitiveString2, userFirstName, 1, userLastName, 1));

        userMainRole = addDProp(idGroup, "userMainRole", "Главная роль (ИД)", userRole, user);
        nameUserMainRole = addJProp(baseGroup, "nameUserMainRole", "Главная роль", name, userMainRole, 1);

        nameToCountry = addCGProp(null, "nameToCountry", "Страна", object(country), name, name, 1);

        nameToPolicy = addCGProp(null, "nameToPolicy", "Политика", object(policy), name, name, 1);
        policyDescription = addDProp(baseGroup, "description", "Описание", StringClass.get(100), policy);

        userRolePolicyOrder = addDProp(baseGroup, "userRolePolicyOrder", "Порядок политики", IntegerClass.instance, userRole, policy);
        userPolicyOrder = addJProp(baseGroup, "userPolicyOrder", "Порядок политики", userRolePolicyOrder, userMainRole, 1, 2);

        barcode = addDProp(baseGroup, "barcode", "Штрих-код", StringClass.get(13), barcodeObject);
        barcode.setFixedCharWidth(13);
        barcodeToObject = addCGProp(null, "barcodeToObject", "Объект", object(barcodeObject), barcode, barcode, 1);
        barcodeObjectName = addJProp(baseGroup, "Объект", name, barcodeToObject, 1);

        barcodePrefix = addDProp(baseGroup, "barcodePrefix", "Префикс штрих-кодов", StringClass.get(13));

        seekBarcodeAction = addJProp(true, "Поиск штрих-кода", addSAProp(null), barcodeToObject, 1);
        barcodeNotFoundMessage = addJProp(true, "", and(false, true), addMAProp("Штрих-код не найден!", "Ошибка"), is(StringClass.get(13)), 1, barcodeToObject, 1);

        restartServerAction = addJProp("Остановить сервер", andNot1, addRestartActionProp(), isServerRestarting);
        cancelRestartServerAction = addJProp("Отменить остановку сервера", and1, addCancelRestartActionProp(), isServerRestarting);

        currentUserName = addJProp("Имя тек. польз.", name, currentUser);

        reverseBarcode = addSDProp("Реверс", LogicalClass.instance);

        classSID = addDProp("classSID", "Стат. код", classSIDValueClass, baseClass.sidClass);
        objectClass = addProperty(null, new LP<ClassPropertyInterface>(new ObjectClassProperty(genSID(), baseClass)));
        objectClassName = addJProp(baseGroup, "objectClassName", "Класс объекта", name, objectClass, 1);

        navigatorElementSID = addDProp(baseGroup, "navigatorElementSID", "Код формы", formSIDValueClass, navigatorElement);
        navigatorElementCaption = addDProp(baseGroup, "navigatorElementCaption", "Название формы", formCaptionValueClass, navigatorElement);
        SIDToNavigatorElement = addCGProp(null, "SIDToNavigatorElement", "Форма", object(navigatorElement), navigatorElementSID, navigatorElementSID, 1);

        permissionUserRoleForm = addDProp(baseGroup, "permissionUserRoleForm", "Запретить форму", LogicalClass.instance, userRole, navigatorElement);
        permissionUserForm = addJProp(baseGroup, "permissionUserForm", "Запретить форму", permissionUserRoleForm, userMainRole, 1, 2);
//        permissionUserForm = addDProp(baseGroup, "permissionUserForm", "Запретить форму", LogicalClass.instance, user, navigatorElement);

        selectUserRoles = addSelectFromListAction(null, "Редактировать роли", inUserRole, userRole, customUser);
        selectRoleForms = addSelectFromListAction(null, "Редактировать формы", permissionUserRoleForm, navigatorElement, userRole);

        // заполним сессии
        LP sessionUser = addDProp("sessionUser", "Пользователь сессии", user, session);
        sessionUser.setDerivedChange(currentUser, true, is(session), 1);
        addJProp(baseGroup, "Пользователь сессии", name, sessionUser, 1);
        LP sessionDate = addDProp(baseGroup, "sessionDate", "Дата сессии", DateClass.instance, session);
        sessionDate.setDerivedChange(currentDate, true, is(session), 1);
        onlyNotZero = addJProp(andNot1, 1, addJProp(equals2, 1, vzero), 1);
        onlyNotZero.property.isOnlyNotZero = true;

        objectByName = addMGProp(idGroup, "objectByName", "Объект (Имя)", object(baseClass.named), name, 1);
        seekObjectName = addJProp(true, "Поиск объекта", addSAProp(null), objectByName, 1);

        webHost = addDProp("webHost", "Web хост", StringClass.get(50));

        smtpHost = addDProp("smtpHost", "SMTP хост", StringClass.get(50));
        smtpPort = addDProp("smtpPort", "SMTP порт", StringClass.get(10));
        emailAccount = addDProp("emailAccount", "Имя аккаунта", StringClass.get(50));
        emailPassword = addDProp("emailPassword", "Пароль", StringClass.get(50));
        fromAddress = addDProp("fromAddress", "Адрес отправителя", StringClass.get(50));
        defaultCountry = addDProp("defaultCountry", "Страна по умолчанию", country);

        entryDictionary = addDProp("entryDictionary", "Словарь", dictionary, dictionaryEntry);
        termDictionary = addDProp(baseGroup, "termDictionary", "Термин", StringClass.get(50), dictionaryEntry);
        translationDictionary = addDProp(baseGroup, "translationDictionary", "Перевод", StringClass.get(50), dictionaryEntry);
        translationDictionaryTerm = addCGProp(null, "translationDictionayTerm", "Перевод", translationDictionary, termDictionary, entryDictionary , 1, termDictionary, 1);
    }

    private void initBaseTables() {
        tableFactory.include("customUser", customUser);
        tableFactory.include("loginSID", StringClass.get(30), StringClass.get(30));
        tableFactory.include("countryDate", country, DateClass.instance);
    }

    private void initBaseIndexes() {
        addIndex(barcode);
    }

    /**
     * Нужно для скрытия свойств при соблюдении какого-то критерия
     * <p/>
     * <pre>
     * Пример использования:
     *       Скроем свойство policyDescription, если у текущего user'а логин - "Admin"
     * <p/>
     *       Вводим свойство критерия:
     * <p/>
     *         LP hideUserPolicyDescription = addJProp(diff2, userLogin, 1, addCProp(StringClass.get(30), "Admin"));
     * <p/>
     *       Вводим свойство которое будет использовано в качестве propertyCaption для policyDescription:
     * <p/>
     *         policyDescriptorCaption = addHideCaptionProp(null, "Policy caption", policyDescription, hideUserPolicyDescription);
     * <p/>
     *       Далее в форме указываем соответсвующий propertyCaption:
     * <p/>
     *         PropertyDrawEntity descriptionDraw = getPropertyDraw(policyDescription, objPolicy.groupTo);
     *         PropertyDrawEntity descriptorCaptionDraw = addPropertyDraw(policyDescriptorCaption, objUser);
     *         descriptionDraw.setPropertyCaption(descriptorCaptionDraw.propertyObject);
     * </pre>
     *
     * @param group        ...
     * @param caption      ...
     * @param original     свойство, к которому будет применятся критерий сокрытия
     * @param hideProperty критерий
     * @return свойство, которое должно использоваться в качестве propertyCaption для скрываемого свойства
     */
    protected LP addHideCaptionProp(AbstractGroup group, String caption, LP original, LP hideProperty) {
        LP originalCaption = addCProp(StringClass.get(100), original.property.caption);
        LP result = addJProp(group, caption, and1, BaseUtils.add(new Object[]{originalCaption}, directLI(hideProperty)));
        return result;
    }

    void initBaseNavigators() {
        adminElement = new NavigatorElement(baseElement, "adminElement", "Администрирование");
        NavigatorElement policyElement = new NavigatorElement(adminElement, "policyElement", "Политика безопасности");
            addFormEntity(new UserPolicyFormEntity(policyElement, "userPolicyForm"));
            addFormEntity(new RolePolicyFormEntity(policyElement, "rolePolicyForm"));
        addFormEntity(new AdminFormEntity(adminElement, "adminForm"));
        addFormEntity(new DaysOffFormEntity(adminElement, "daysOffForm"));
        addFormEntity(new DictionariesFormEntity(adminElement, "dictionariesForm"));
    }

    protected SecurityPolicy permitAllPolicy, readOnlyPolicy;

    void initBaseAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        permitAllPolicy = addPolicy("Разрешить всё", "Политика разрешает все действия.");
        permitAllPolicy.setReplaceMode(true);

        readOnlyPolicy = addPolicy("Запретить редактирование всех свойств", "Режим \"только чтение\". Запрещает редактирование всех свойств на формах.");
        readOnlyPolicy.property.change.defaultPermission = false;
        readOnlyPolicy.cls.edit.add.defaultPermission = false;
        readOnlyPolicy.cls.edit.change.defaultPermission = false;
        readOnlyPolicy.cls.edit.remove.defaultPermission = false;
    }

    public void ping() throws RemoteException {
        return;
    }

    protected void initBaseClassForms() {
        baseClass.named.setClassForm(new NamedObjectClassForm(this, baseClass.named));
    }

    private class UserPolicyFormEntity extends FormEntity {
        protected UserPolicyFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Пользователи");

            ObjectEntity objUser = addSingleGroupObject(customUser, selection, baseGroup, true);
            ObjectEntity objRole = addSingleGroupObject(userRole, baseGroup, true);

            addObjectActions(this, objUser);

            addPropertyDraw(objUser, objRole, baseGroup, true);

            addPropertyDraw(selectUserRoles, objRole.groupTo, objUser).forceViewType = ClassViewType.PANEL;

            addFixedFilter(new CompareFilterEntity(addPropertyObject(inUserRole, objUser, objRole), Compare.EQUALS, true));
        }
    }

    private class RolePolicyFormEntity extends FormEntity {

        private ObjectEntity objUserRole;
        private ObjectEntity objPolicy;
        private ObjectEntity objForm;

        protected RolePolicyFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Роли");

            objUserRole = addSingleGroupObject(userRole, baseGroup, true);
            objPolicy = addSingleGroupObject(policy, "Политики безопасности", baseGroup, true);
            objForm = addSingleGroupObject(navigatorElement, "Формы", baseGroup, true);

            addObjectActions(this, objUserRole);

            addPropertyDraw(objUserRole, objPolicy, baseGroup, true);
            addPropertyDraw(objUserRole, objForm, permissionUserRoleForm);

            addPropertyDraw(selectRoleForms, objForm.groupTo, objUserRole).forceViewType = ClassViewType.PANEL;

            setReadOnly(navigatorElementSID, true);
            setReadOnly(navigatorElementCaption, true);

            PropertyDrawEntity balanceDraw = getPropertyDraw(userRolePolicyOrder, objPolicy.groupTo);
            PropertyDrawEntity sidDraw = getPropertyDraw(userRoleSID, objUserRole.groupTo);
            balanceDraw.addColumnGroupObject(objUserRole.groupTo);
            balanceDraw.setPropertyCaption(sidDraw.propertyObject);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(permissionUserRoleForm, objUserRole, objForm), Compare.EQUALS, true));
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            ContainerView container = design.createContainer();
            container.tabbedPane = true;

            design.getMainContainer().addAfter(container, design.getGroupObjectContainer(objUserRole.groupTo));
            container.add(design.getGroupObjectContainer(objPolicy.groupTo));
            container.add(design.getGroupObjectContainer(objForm.groupTo));

            return design;
        }
    }

    private class SelectFromListFormEntity extends FormEntity implements FormActionProperty.SelfInstancePostProcessor {
        private ObjectEntity[] mainObjects;
        private ObjectEntity selectionObject;
        private ObjectEntity remapObject;
        private final FilterEntity[] remapFilters;

        private SelectFromListFormEntity(ObjectEntity remapObject, FilterEntity[] remapFilters, LP selectionProperty, boolean isSelectionClassFirstParam, ValueClass selectionClass, ValueClass... baseClasses) {
            this.remapObject = remapObject;
            this.remapFilters = remapFilters;

            mainObjects = new ObjectEntity[baseClasses.length];
            for (int i = 0; i < baseClasses.length; i++) {
                ValueClass baseClass = baseClasses[i];
                mainObjects[i] = addSingleGroupObject(baseClass, baseGroup);
                mainObjects[i].groupTo.setSingleClassView(ClassViewType.PANEL);
                PropertyDrawEntity objectValue = getPropertyDraw(BusinessLogics.this.objectValue, mainObjects[i]);
                if (objectValue != null) {
                    objectValue.readOnly = true;
                }
            }

            selectionObject = addSingleGroupObject(selectionClass, baseGroup);
            selectionObject.groupTo.setSingleClassView(ClassViewType.GRID);

            ObjectEntity[] selectionObjects = new ObjectEntity[mainObjects.length + 1];
            if (isSelectionClassFirstParam) {
                System.arraycopy(mainObjects, 0, selectionObjects, 1, mainObjects.length);
                selectionObjects[0] = selectionObject;
            } else {
                System.arraycopy(mainObjects, 0, selectionObjects, 0, mainObjects.length);
                selectionObjects[mainObjects.length] = selectionObject;
            }

            PropertyDrawEntity selectionPropertyDraw = addPropertyDraw(selectionProperty, selectionObjects);
            PropertyObjectEntity selectionPropertyObject = selectionPropertyDraw.propertyObject;

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(
                    new RegularFilterEntity(genID(),
                                            new NotFilterEntity(
                                                    new CompareFilterEntity(selectionPropertyObject, Compare.EQUALS, true)),
                                            "Невыбранные объекты",
                                            KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)
                    ), true);
            filterGroup.addFilter(
                    new RegularFilterEntity(genID(),
                                            new CompareFilterEntity(selectionPropertyObject, Compare.EQUALS, true),
                                            "Выбранные объекты",
                                            KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)
                    ));
            addRegularFilterGroup(filterGroup);
        }

        public void postProcessSelfInstance(Map<ClassPropertyInterface, DataObject> keys, RemoteForm executeForm, FormInstance selfFormInstance) {
            for (FilterEntity filterEntity : remapFilters) {
                selfFormInstance.addFixedFilter(
                        filterEntity.getRemappedFilter(remapObject, selectionObject, executeForm.form.instanceFactory)
                );
            }
        }
    }

    private class AdminFormEntity extends FormEntity {
        private AdminFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Глобальные параметры");

            addPropertyDraw(new LP[]{smtpHost, smtpPort, fromAddress, emailAccount, emailPassword, webHost, defaultCountry, barcodePrefix, restartServerAction, cancelRestartServerAction});
        }
    }

    private class DaysOffFormEntity extends FormEntity {
        ObjectEntity objDays;

        public DaysOffFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Выходные дни");

            ObjectEntity objCountry = addSingleGroupObject(country, "Страна");
            objCountry.groupTo.initClassView = ClassViewType.PANEL;

            objDays = addSingleGroupObject(DateClass.instance, "День");

            ObjectEntity objNewDate = addSingleGroupObject(DateClass.instance, "Дата");
            objNewDate.groupTo.setSingleClassView(ClassViewType.PANEL);

            addPropertyDraw(objCountry, baseGroup);
            addPropertyDraw(objDays, baseGroup);
            addPropertyDraw(isDayOffCountryDate, objCountry, objDays);
            addPropertyDraw(objNewDate, baseGroup);
            addPropertyDraw(isDayOffCountryDate, objCountry, objNewDate);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(isDayOffCountryDate, objCountry, objDays)));
        }

        public FormView createDefaultRichDesign() {
            FormView design = super.createDefaultRichDesign();
            design.getGroupObject(objDays.groupTo).grid.constraints.fillVertical = 3;
            return design;
        }
    }

    public class DictionariesFormEntity extends FormEntity {

        public DictionariesFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Словари");

            ObjectEntity objDict = addSingleGroupObject(dictionary, "Словарь");
            objDict.groupTo.initClassView = ClassViewType.PANEL;
            ObjectEntity objDictEntry = addSingleGroupObject(dictionaryEntry, "Слова");

            addPropertyDraw(objDict, baseGroup);
            addPropertyDraw(objDictEntry, baseGroup);

            addObjectActions(this, objDict);
            addObjectActions(this, objDictEntry, objDict);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(entryDictionary, objDictEntry), Compare.EQUALS, objDict));
        }
    }

    public class NamedObjectClassForm extends ClassFormEntity {
        public ObjectEntity objObjectName;


        public NamedObjectClassForm(BusinessLogics BL, CustomClass cls, String sID, String caption) {
            super(BL, cls, sID, caption);

            objObjectName = addSingleGroupObject(StringClass.get(50), "Поиск по началу имени", objectValue);
            objObjectName.groupTo.setSingleClassView(ClassViewType.PANEL);

            //двигаем в начало
            groups.remove(objObjectName.groupTo);
            groups.add(0, objObjectName.groupTo);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(name, object), Compare.START_WITH, objObjectName));
        }

        public NamedObjectClassForm(BusinessLogics BL, CustomClass cls) {
            this(BL, cls, "namedObjectForm" + cls.getSID(), cls.caption);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();
            design.get(getPropertyDraw(objectValue, objObjectName)).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0);
            return design;
        }

        @Override
        public AbstractClassFormEntity copy() {
            return new NamedObjectClassForm(BL, cls, getSID() + "_copy" + copies++, caption);
        }
    }

    @IdentityLazy
    protected LP getAddObjectAction(ValueClass cls) {
        return addAProp(new AddObjectActionProperty(genSID(), (CustomClass) cls));
    }

    @IdentityLazy
    protected LP getAddObjectActionWithClassCheck(ValueClass baseClass, ValueClass checkClass) {
        LP addObjectAction = getAddObjectAction(baseClass);
        return addJProp(addObjectAction.property.caption, and1, addObjectAction, is(checkClass), 1);
    }

    @IdentityLazy
    protected LP getImportObjectAction(ValueClass cls) {
        return addAProp(new ImportFromExcelActionProperty(genSID(), (CustomClass) cls));
    }

    private static class ChangeUserActionProperty extends ActionProperty {

        private ChangeUserActionProperty(String sID, ConcreteValueClass userClass) {
            super(sID, "Сменить пользователя", new ValueClass[]{userClass});
        }

        @Override
        protected DataClass getValueClass() {
            return LogicalClass.instance;
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            DataObject user = BaseUtils.singleValue(keys);
            if (executeForm.form.BL.requiredPassword) {
                actions.add(new UserReloginClientAction(executeForm.form.BL.getUserName(user).trim()));
            } else {
                executeForm.form.session.user.changeCurrentUser(user);
                actions.add(new UserChangedClientAction());
            }
        }
    }

    public String getUserName(DataObject user) {
        try {
            return (String) userLogin.read(createSession(), user);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<ValueClass, LP> is = new HashMap<ValueClass, LP>();

    // получает свойство is
    // для множества классов есть CProp
    protected LP is(ValueClass valueClass) {
        LP isProp = is.get(valueClass);
        if (isProp == null) {
            isProp = addCProp(valueClass.toString() + "(пр.)", LogicalClass.instance, true, valueClass);
            is.put(valueClass, isProp);
        }
        return isProp;
    }

    private Map<ValueClass, LP> object = new HashMap<ValueClass, LP>();

    public LP object(ValueClass valueClass) {
        LP objectProp = object.get(valueClass);
        if (objectProp == null) {
            objectProp = addJProp(valueClass.toString(), and1, 1, is(valueClass), 1);
            object.put(valueClass, objectProp);
        }
        return objectProp;
    }

    protected LP and(boolean... nots) {
        return addAFProp(nots);
    }

    private Map<ValueClass, LP> split = new HashMap<ValueClass, LP>();

    public LP split(ValueClass valueClass) { // избыточно так как не может сама класс определить
        LP splitProp = split.get(valueClass);
        if (splitProp == null) {
            splitProp = addJProp("split " + valueClass.toString(), equals2, object(valueClass), 1, object(valueClass), 2);
            split.put(valueClass, splitProp);
        }
        return splitProp;
    }

    // по умолчанию с полным стартом

    public BusinessLogics(DataAdapter adapter, int exportPort) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, FileNotFoundException, JRException {
        super(exportPort);

        this.adapter = adapter;
        sql = createSQL();

        initBase();

        initGroups();

        initClasses();
        checkClasses(); //проверка на то, что у каждого абстрактного класса есть конкретный потомок
        baseClass.initObjectClass();
        storeCustomClass(baseClass.objectClass);

        // после classes и до properties, чтобы можно было бы создать таблицы и использовать persistent таблицы в частности для определения классов
        initBaseTables();

        initTables();

        initBaseProperties();

        initProperties();

        Set idSet = new HashSet<String>();
        for (Property property : getProperties()) {
            assert idSet.add(property.sID) : "Same sid " + property.sID;
        }

        initBaseIndexes();

        initIndexes();

        assert checkProps();

        synchronizeDB();

        initExternalScreens();

        logger.debug("Initializing navigators...");

        initBaseClassForms();

        objectElement = baseClass.getBaseClassForm(this);
        baseElement.add(objectElement);

        initBaseNavigators();
        initNavigators();

        initBaseAuthentication();
        initAuthentication();

        synchronizeForms();

        // считаем системного пользователя
        try {
            DataSession session = createSession(sql, new UserController() {
                public void changeCurrentUser(DataObject user) {
                    throw new RuntimeException("not supported");
                }

                public DataObject getCurrentUser() {
                    return new DataObject(0, systemUser);
                }
            }, new ComputerController() {
                public DataObject getCurrentComputer() {
                    return new DataObject(0, computer);
                }
            });

            Query<String, Object> query = new Query<String, Object>(Collections.singleton("key"));
            query.and(BaseUtils.singleValue(query.mapKeys).isClass(systemUser));
            Set<Map<String, Object>> rows = query.execute(session.sql, new OrderedMap<Object, Boolean>(), 1, session.env).keySet();
            if (rows.size() == 0) { // если нету добавим
                systemUserObject = (Integer) session.addObject(systemUser, session.modifier).object;
                session.apply(this);
            } else
                systemUserObject = (Integer) BaseUtils.single(rows).get("key");

            query = new Query<String, Object>(Collections.singleton("key"));
            query.and(hostname.getExpr(session.modifier, BaseUtils.singleValue(query.mapKeys)).compare(new DataObject("systemhost"), Compare.EQUALS));
            rows = query.execute(session.sql, new OrderedMap<Object, Boolean>(), 1, session.env).keySet();
            if (rows.size() == 0) { // если нету добавим
                DataObject computerObject = session.addObject(computer, session.modifier);
                systemComputer = (Integer) computerObject.object;
                hostname.execute("systemhost", session, session.modifier, computerObject);
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

        Thread thread = new Thread(new Runnable() {
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
                    } catch (Exception e) {
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();

        reloadNavigatorTree();
    }

    protected void synchronizeForms() {
        ImportField sidField = new ImportField(formSIDValueClass);
        ImportField captionField = new ImportField(formCaptionValueClass);

        ImportKey<?> key = new ImportKey(navigatorElement, SIDToNavigatorElement.getMapping(sidField));

        List<List<Object>> data = new ArrayList<List<Object>>();
        for (NavigatorElement<T> navElement : baseElement.getChildren(true)) {
            data.add(Arrays.asList((Object)navElement.getSID(), navElement.caption));
        }

        List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();
        props.add(new ImportProperty(sidField, navigatorElementSID.getMapping(key)));
        props.add(new ImportProperty(captionField, navigatorElementCaption.getMapping(key)));

        ImportTable table = new ImportTable(Arrays.asList(sidField, captionField), data);

        try {
            DataSession session = createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(key), props);
            service.synchronize(true, true, true);
            session.apply(this);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final String navigatorTreeFilePath = "conf/" + getName() + "/navigatorTree.data";

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

        LP generateOrDefaultCountry = addSUProp(Union.OVERRIDE, generateDatesCountry, addJProp(equals2, defaultCountry, 1));

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
        Date date = new Date();
        int currentYear = date.getYear();
        //если проставлен выходной 1 января через 2 года, пропускаем генерацию
        DataObject countryObject = new DataObject(countryId, this.country);
        if (isDayOffCountryDate.read(session, countryObject, new DataObject(new java.sql.Date(new Date(currentYear + 2, 0, 1).getTime()), DateClass.instance)) != null) {
            return;
        }

        long wholeYearMillisecs = new Date(currentYear + 3, 0, 1).getTime() - new Date(currentYear, 0, 1).getTime();
        long wholeYearDays = wholeYearMillisecs / 1000 / 60 / 60 / 24;
        for (int i = 0; i < wholeYearDays; i++) {
            Date newDate = new Date(currentYear, 0, 1 + i);
            int day = newDate.getDay();
            if (day == 5 || day == 6) {
                addDayOff(session, countryId, newDate);
            }
        }

        for (int i = 0; i < 3; i++) {
            Date newYear = new Date(currentYear + i + "/01/01");
            if(newYear.getDay() != 5 && newYear.getDay() != 6)
                addDayOff(session, countryId, newYear);
        }

        session.apply(this);
        session.close();
    }

    private void addDayOff(DataSession session, int countryId, Date dayOff) throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        DataObject countryObject = new DataObject(countryId, country);
        isDayOffCountryDate.execute(true, session, countryObject, new DataObject(new java.sql.Date(dayOff.getTime()), DateClass.instance));
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
        Map<String, NavigatorElement<T>> elementsMap = new HashMap<String, NavigatorElement<T>>();
        for (NavigatorElement<T> parent : baseElement.getChildren(true)) {
            String parentSID = parent.getSID();
            elementsMap.put(parentSID, parent);

            if (!treeStructure.containsKey(parentSID)) {
                List<String> children = new ArrayList<String>();
                for (NavigatorElement<T> child : parent.getChildren(false)) {
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
                    NavigatorElement<T> element = elementsMap.get(childSID);

                    if (element != null) {
                        parent.add(element);
                    }
                }
            }
        }
    }

    private void overrideElement(Map<String, NavigatorElement<T>> elementsMap, String elementSID) {
        NavigatorElement<T> element = getOverridenElement(elementSID);
        if (element == null) {
            element = getOverridenForm(elementSID);
        }

        if (element != null) {
            elementsMap.put(elementSID, element);
        }
    }

    public String getFormSerializationPath(String formSID) {
        try {
            return "conf/" + getName() + "/forms/" + formSID;
        } catch (RemoteException re) {
            return "conf/forms/" + formSID;
        }
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
        try {
            return "conf/" + getName() + "/elements/" + elementSID;
        } catch (RemoteException re) {
            return "conf/elements/" + elementSID;
        }
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
        Collection<NavigatorElement<T>> children = baseElement.getChildren(true);
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
        currentDate.execute(DateConverter.dateToSql(new Date()), session, session.modifier);
        session.apply(this);
        session.close();
    }

    private void checkClasses() {
        checkClass(baseClass);
    }

    private void checkClass(CustomClass c) {
        assert (!(c instanceof AbstractCustomClass) || c.hasChildren() || c.equals(baseClass.sidClass) || c.equals(transaction) || c.equals(barcodeObject)) : "Doesn't exist concrete class";
        for (CustomClass children : c.children) {
            checkClass(children);
        }
    }

    public final int systemUserObject;
    public final int systemComputer;

    /*
      final static Set<Integer> wereSuspicious = new HashSet<Integer>();

      // тестирующий конструктор
      public BusinessLogics(DataAdapter iAdapter,int testType,Integer seed,int iterations) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException {
          super(1099);

          adapter = iAdapter;

          initBase();

          if(testType>=1) {
              initLogics();
              if(testType>=2)
                  initDatabaseMapping();
          }

          if(seed==null) {
              List<Integer> proceedSeeds = new ArrayList<Integer>();
              int[] suspicious = {888,1252,8773,9115,8700,9640,2940,4611,8038};
              if(testType>=0 || wereSuspicious.size()>=suspicious.length)
                  seed = (new Random()).nextInt(10000);
              else {
                  while(true) {
                      seed = suspicious[(new Random()).nextInt(suspicious.length)];
                      if(!wereSuspicious.contains(seed)) {
                          wereSuspicious.add(seed);
                          break;
                      }
                  }
              }
          }

          System.out.println("Random seed - "+ seed);

          Random randomizer = new Random(seed);

          if(testType<1) {
  //            randomClasses(randomizer);
  //            randomProperties(randomizer);
          }

          if(testType<2) {
              randomImplement(randomizer);
              randomPersistent(randomizer);
          }

          // запустить ChangeDBTest
          try {
              changeDBTest(iterations,randomizer);
          } catch(Exception e) {
              throw new RuntimeException(e);
          }
      }
    */
    public AbstractGroup rootGroup;
    public AbstractGroup publicGroup;
    public AbstractGroup privateGroup;
    public AbstractGroup baseGroup;
    public AbstractGroup idGroup;
    public AbstractGroup actionGroup;
    public AbstractGroup sessionGroup;

    protected abstract void initGroups();

    protected abstract void initClasses();

    protected abstract void initProperties();

    protected abstract void initTables();

    protected abstract void initIndexes();

    public NavigatorElement<T> baseElement;
    public NavigatorElement<T> objectElement;
    public NavigatorElement<T> adminElement;

    protected abstract void initNavigators() throws JRException, FileNotFoundException;

    public PolicyManager policyManager = new PolicyManager();

    protected abstract void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException;

    Set<String> idSet = new HashSet<String>();

    public String genSID() {
        String id = "property" + idSet.size();
        idSet.add(id);
        return id;
    }

    private void addPersistent(AggregateProperty property) {
        assert !idSet.contains(property.sID);
        property.stored = true;

        logger.debug("Initializing stored property...");
        property.markStored(tableFactory);
    }

    public void addPersistent(LP lp) {
        addPersistent((AggregateProperty) lp.property);
    }

    protected void addIndex(LP<?>... lps) {
        List<Property> index = new ArrayList<Property>();
        for (LP<?> lp : lps)
            index.add(lp.property);
        indexes.add(index);
    }

    public SQLSession createSQL() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        return new SQLSession(adapter);
    }

    public DataSession createSession() throws SQLException {
        return createSession(sql, new UserController() {
            public void changeCurrentUser(DataObject user) {
                throw new RuntimeException("not supported");
            }

            public DataObject getCurrentUser() {
                return new DataObject(systemUserObject, systemUser);
            }
        }, new ComputerController() {

            public DataObject getCurrentComputer() {
                return new DataObject(systemComputer, computer);
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
                baseClass, baseClass.named, session, name, transaction, date, currentDate, notDeterministic);
    }

    public List<DerivedChange<?, ?>> notDeterministic = new ArrayList<DerivedChange<?, ?>>();

    public BaseClass baseClass;

    public TableFactory tableFactory;
    public List<LP> lproperties = new ArrayList<LP>();
    protected Set<AggregateProperty> persistents = new HashSet<AggregateProperty>();
    protected Set<List<? extends Property>> indexes = new HashSet<List<? extends Property>>();

    // получает список св-в в порядке использования

    private void fillPropertyList(Property<?> property, LinkedHashSet<Property> set) {
        for (Property depend : property.getDepends())
            fillPropertyList(depend, set);
        set.add(property);
    }

    public List<Property> getProperties() {
        return rootGroup.getProperties();
    }

    @IdentityLazy
    public Iterable<Property> getPropertyList() {
        LinkedHashSet<Property> linkedSet = new LinkedHashSet<Property>();
        for (Property property : getProperties())
            fillPropertyList(property, linkedSet);
        return linkedSet;
    }

    private boolean depends(Property<?> property, Property check) { // пока только для getChangeConstrainedProperties
        if (property.equals(check))
            return true;
        for (Property depend : property.getDepends())
            if (depends(depend, check))
                return true;
        return false;
    }

    @IdentityLazy
    public List<Property> getStoredProperties() {
        List<Property> result = new ArrayList<Property>();
        for (Property property : getPropertyList())
            if (property.isStored())
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

    private List<Property> getDerivedPropertyList() { // переставляет execute'ы заведомо до changeProps, чтобы разрешать циклы
        List<Property> result = new ArrayList<Property>();
        for (Property property : getPropertyList()) {
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
    public List<ExecuteProperty> getExecuteDerivedProperties() {
        List<ExecuteProperty> result = new ArrayList<ExecuteProperty>();
        for (Property property : getDerivedPropertyList())
            if (property instanceof ExecuteProperty && ((ExecuteProperty) property).derivedChange != null)
                result.add((ExecuteProperty) property);
        return result;
    }

    public <P extends PropertyInterface> Collection<MaxChangeProperty<?, P>> getChangeConstrainedProperties(Property<P> change) {
        Collection<MaxChangeProperty<?, P>> result = new ArrayList<MaxChangeProperty<?, P>>();
        for (Property<?> property : getPropertyList())
            if (property.isFalse && property.checkChange && depends(property, change))
                result.add(property.getMaxChangeProperty(change));
        return result;
    }

    public List<Property> getAppliedProperties() {
        List<Property> result = new ArrayList<Property>();
        for (Property property : getPropertyList())
            if (property.isStored() || property.isFalse)
                result.add(property);
        return result;
    }

    public void fillIDs() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        DataSession session = createSession();

        baseClass.fillIDs(session, name, classSID);

        session.apply(this);
        session.close();
    }

    public void synchronizeDB() throws SQLException, IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {

        // инициализируем таблицы
        tableFactory.fillDB(sql, baseClass);

        // "старое" состояние базы
        DataInputStream inputDB = null;
        byte[] struct = (byte[]) sql.readRecord(StructTable.instance, new HashMap<KeyField, DataObject>(), StructTable.instance.struct);
        if (struct != null) {
            inputDB = new DataInputStream(new ByteArrayInputStream(struct));

            if(struct.length==0) { //чисто для бага JTDS
                sql.rollbackTransaction();
                return;
            }

            fillIDs();
        }

        sql.startTransaction();

/*        try {
            FileInputStream inputDBFile = new FileInputStream("prevstruct.str");
            byte[] readInput = new byte[inputDBFile.read()*255+inputDBFile.read()];
            inputDBFile.read(readInput);
            inputDB = new DataInputStream(new ByteArrayInputStream(readInput));
        } catch (FileNotFoundException e) {
        }*/

        // новое состояние базы
        ByteArrayOutputStream outDBStruct = new ByteArrayOutputStream();
        DataOutputStream outDB = new DataOutputStream(outDBStruct);

        Map<String, ImplementTable> implementTables = tableFactory.getImplementTables();

        Map<ImplementTable, Set<List<String>>> mapIndexes = new HashMap<ImplementTable, Set<List<String>>>();
        for (ImplementTable table : implementTables.values())
            mapIndexes.put(table, new HashSet<List<String>>());

        // привяжем индексы
        for (List<? extends Property> index : indexes) {
            Iterator<? extends Property> i = index.iterator();
            if (!i.hasNext())
                throw new RuntimeException("Запрещено создавать пустые индексы");
            Property baseProperty = i.next();
            if (!baseProperty.isStored())
                throw new RuntimeException("Запрещено создавать индексы по не постоянным св-вам (" + baseProperty + ")");
            ImplementTable indexTable = baseProperty.mapTable.table;

            List<String> tableIndex = new ArrayList<String>();
            tableIndex.add(baseProperty.field.name);

            while (i.hasNext()) {
                Property property = i.next();
                if (!property.isStored())
                    throw new RuntimeException("Запрещено создавать индексы по не постоянным св-вам (" + baseProperty + ")");
                if (indexTable.findProperty(property.field.name) == null)
                    throw new RuntimeException("Запрещено создавать индексы по св-вам (" + baseProperty + "," + property + ") в разных таблицах");
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
            outDB.writeUTF(property.sID);
            outDB.writeUTF(property.mapTable.table.name);
            for (Map.Entry<? extends PropertyInterface, KeyField> mapKey : property.mapTable.mapKeys.entrySet()) {
                outDB.writeInt(mapKey.getKey().ID);
                outDB.writeUTF(mapKey.getValue().name);
            }
        }

        // если не совпали sID или идентификаторы из базы удаляем сразу
        Map<String, Table> prevTables = new HashMap<String, Table>();
        for (int i = inputDB == null ? 0 : inputDB.readInt(); i > 0; i--) {
            Table prevTable = new Table(inputDB, baseClass);
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
            Table prevTable = prevTables.get(inputDB.readUTF());
            Map<Integer, KeyField> mapKeys = new HashMap<Integer, KeyField>();
            for (int j = 0; j < prevTable.keys.size(); j++)
                mapKeys.put(inputDB.readInt(), prevTable.findKey(inputDB.readUTF()));

            boolean keep = false;
            for (Iterator<Property> is = storedProperties.iterator(); is.hasNext();) {
                Property<?> property = is.next();
                if (property.sID.equals(sID)) {
                    Map<KeyField, PropertyInterface> foundInterfaces = new HashMap<KeyField, PropertyInterface>();
                    for (PropertyInterface propertyInterface : property.interfaces) {
                        KeyField mapKeyField = mapKeys.get(propertyInterface.ID);
                        if (mapKeyField != null) foundInterfaces.put(mapKeyField, propertyInterface);
                    }
                    if (foundInterfaces.size() == mapKeys.size()) { // если все нашли
                        if (!(keep = property.mapTable.table.name.equals(prevTable.name))) { // если в другой таблице
                            sql.addColumn(property.mapTable.table.name, property.field);
                            // делаем запрос на перенос
                            logger.info("Идет перенос колонки " + property.field + " (" + property.caption + ")" + " из таблицы " + prevTable.name + " в таблицу " + property.mapTable.table.name + "... ");
                            Query<KeyField, PropertyField> moveColumn = new Query<KeyField, PropertyField>(property.mapTable.table);
                            Expr moveExpr = prevTable.joinAnd(BaseUtils.join(BaseUtils.join(foundInterfaces, ((Property<PropertyInterface>) property).mapTable.mapKeys), moveColumn.mapKeys)).getExpr(prevTable.findProperty(sID));
                            moveColumn.properties.put(property.field, moveExpr);
                            moveColumn.and(moveExpr.getWhere());
                            sql.modifyRecords(new ModifyQuery(property.mapTable.table, moveColumn));
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
            if (property instanceof AggregateProperty)
                recalculateProperties.add((AggregateProperty) property);
        }

        // удаляем таблицы старые
        for (String table : prevTables.keySet())
            if (!implementTables.containsKey(table))
                sql.dropTable(table);

        // упакуем таблицы
        for (ImplementTable table : packTables)
            sql.packTable(table);

        recalculateAggregations(sql, recalculateProperties);
//        recalculateAggregations(sql, getAggregateStoredProperties());


        // создадим индексы в базе
        for (Map.Entry<ImplementTable, Set<List<String>>> mapIndex : mapIndexes.entrySet())
            for (List<String> index : mapIndex.getValue())
                sql.addIndex(mapIndex.getKey().name, index);

        try {
            sql.updateInsertRecord(StructTable.instance, new HashMap<KeyField, DataObject>(), Collections.singletonMap(StructTable.instance.struct, (ObjectValue) new DataObject((Object) outDBStruct.toByteArray(), ByteArrayClass.instance)));
        } catch (Exception e) {
            Map<PropertyField, ObjectValue> propFields = Collections.singletonMap(StructTable.instance.struct, (ObjectValue) new DataObject((Object) new byte[0], ByteArrayClass.instance));
            sql.updateInsertRecord(StructTable.instance, new HashMap<KeyField, DataObject>(), propFields);
        }

        sql.commitTransaction();

        if(struct==null)
            fillIDs();
    }

    boolean checkPersistent(SQLSession session) throws SQLException {
//        System.out.println("checking persistent...");
        for (Property property : getStoredProperties()) {
//            System.out.println(Property.title);
            if (property instanceof AggregateProperty && !((AggregateProperty) property).checkAggregation(session, property.caption)) // Property.title.equals("Расх. со скл.")
                return false;
//            Property.Out(Adapter);
        }

        return true;
    }


    protected Map<String, CustomClass> sidToClass = new HashMap<String, CustomClass>();

    protected void storeCustomClass(CustomClass customClass) {
        assert !sidToClass.containsKey(customClass.getSID());
        sidToClass.put(customClass.getSID(), customClass);
    }

    protected ConcreteCustomClass addConcreteClass(String sID, String caption, CustomClass... parents) {
        return addConcreteClass(baseGroup, sID, caption, parents);
    }

    protected ConcreteCustomClass addConcreteClass(AbstractGroup group, String sID, String caption, CustomClass... parents) {
        ConcreteCustomClass customClass = new ConcreteCustomClass(sID, caption, parents);
        group.add(customClass);
        storeCustomClass(customClass);
        return customClass;
    }

    protected StaticCustomClass addStaticClass(String sID, String caption, String[] sids, String[] names) {
        StaticCustomClass customClass = new StaticCustomClass(sID, caption, baseClass.sidClass, sids, names);
        storeCustomClass(customClass);
        customClass.dialogReadOnly = true;
        return customClass;
    }

    protected BaseClass addBaseClass(String sID, String caption) {
        BaseClass baseClass = new BaseClass(sID, caption);
        storeCustomClass(baseClass);
        storeCustomClass(baseClass.named);
        return baseClass;
    }

    protected AbstractCustomClass addAbstractClass(String sID, String caption, CustomClass... parents) {
        return addAbstractClass(baseGroup, sID, caption, parents);
    }

    protected AbstractCustomClass addAbstractClass(AbstractGroup group, String sID, String caption, CustomClass... parents) {
        AbstractCustomClass customClass = new AbstractCustomClass(sID, caption, parents);
        group.add(customClass);
        storeCustomClass(customClass);
        return customClass;
    }

    public CustomClass findCustomClass(String sid) {
        return sidToClass.get(sid);
    }

    public ValueClass findValueClass(String sid) {
        ValueClass valueClass = findCustomClass(sid);
        if (valueClass == null) {
            valueClass = DataClass.findDataClass(sid);
        }
        return valueClass;
    }

    protected LP addDProp(String sID, String caption, ValueClass value, ValueClass... params) {
        return addDProp(sID, false, caption, value, params);
    }

    protected LP addDProp(String sID, boolean persistent, String caption, ValueClass value, ValueClass... params) {
        return addDProp(null, sID, persistent, caption, value, params);
    }

    protected LP addDProp(AbstractGroup group, String sID, String caption, ValueClass value, ValueClass... params) {
        return addDProp(group, sID, false, caption, value, params);
    }

    protected LP[] addDProp(AbstractGroup group, String paramID, String[] sIDs, String[] captions, ValueClass[] values, ValueClass... params) {
        LP[] result = new LP[sIDs.length];
        for(int i=0;i<sIDs.length;i++)
            result[i] = addDProp(group, sIDs[i]+paramID, captions[i], values[i], params);
        return result;
    }

    protected LP addDProp(AbstractGroup group, String sID, boolean persistent, String caption, ValueClass value, ValueClass... params) {
        StoredDataProperty dataProperty = new StoredDataProperty(sID, caption, params, value);
        LP lp = addProperty(group, persistent, new LP<ClassPropertyInterface>(dataProperty));
        dataProperty.markStored(tableFactory);
        return lp;
    }

    protected <D extends PropertyInterface> LP addDCProp(String sID, String caption, LP<D> derivedProp, Object... params) {
        return addDCProp(null, sID, caption, derivedProp, params);
    }

    protected <D extends PropertyInterface> LP addDCProp(String sID, boolean persistent, String caption, LP<D> derivedProp, Object... params) {
        return addDCProp(null, sID, persistent, caption, false, derivedProp, params);
    }

    protected <D extends PropertyInterface> LP addDCProp(AbstractGroup group, String sID, String caption, LP<D> derivedProp, Object... params) {
        return addDCProp(group, sID, false, caption, false, derivedProp, params);
    }

    protected <D extends PropertyInterface> LP addDCProp(String sID, String caption, boolean forced, LP<D> derivedProp, Object... params) {
        return addDCProp(null, sID, caption, forced, derivedProp, params);
    }

    protected <D extends PropertyInterface> LP addDCProp(AbstractGroup group, String sID, String caption, boolean forced, LP<D> derivedProp, Object... params) {
        return addDCProp(group, sID, false, caption, forced, derivedProp, params);
    }

    protected <D extends PropertyInterface> LP addDCProp(AbstractGroup group, String sID, boolean persistent, String caption, boolean forced, LP<D> derivedProp, Object... params) {

        // считываем override'ы с конца
        List<ValueClass> backClasses = new ArrayList<ValueClass>();
        int i = params.length - 1;
        while (i > 0 && (params[i] == null || params[i] instanceof ValueClass))
            backClasses.add((ValueClass) params[i--]);
        params = Arrays.copyOfRange(params, 0, i + 1);
        ValueClass[] overrideClasses = BaseUtils.reverse(backClasses).toArray(new ValueClass[1]);

        boolean defaultChanged = false;
        if (params[0] instanceof Boolean) {
            defaultChanged = (Boolean) params[0];
            params = Arrays.copyOfRange(params, 1, params.length);
        }

        // придется создавать Join свойство чтобы считать его класс
        List<LI> list = readLI(params);

        int propsize = derivedProp.listInterfaces.size();
        int dersize = getIntNum(params);
        JoinProperty<AndFormulaProperty.Interface> joinProperty = new JoinProperty<AndFormulaProperty.Interface>(sID, caption, dersize, false);
        LP<JoinProperty.Interface> listProperty = new LP<JoinProperty.Interface>(joinProperty);

        AndFormulaProperty andProperty = new AndFormulaProperty(genSID(), new boolean[list.size() - propsize]);
        Map<AndFormulaProperty.Interface, PropertyInterfaceImplement<JoinProperty.Interface>> mapImplement = new HashMap<AndFormulaProperty.Interface, PropertyInterfaceImplement<JoinProperty.Interface>>();
        mapImplement.put(andProperty.objectInterface, DerivedProperty.createJoin(mapImplement(derivedProp, mapLI(list.subList(0, propsize), listProperty.listInterfaces))));
        Iterator<AndFormulaProperty.AndInterface> itAnd = andProperty.andInterfaces.iterator();
        for (PropertyInterfaceImplement<JoinProperty.Interface> partProperty : mapLI(list.subList(propsize, list.size()), listProperty.listInterfaces))
            mapImplement.put(itAnd.next(), partProperty);

        joinProperty.implement = new PropertyImplement<PropertyInterfaceImplement<JoinProperty.Interface>, AndFormulaProperty.Interface>(andProperty, mapImplement);

        // получаем классы
        Result<ValueClass> value = new Result<ValueClass>();
        ValueClass[] commonClasses = listProperty.getCommonClasses(value);

        // override'им классы
        ValueClass valueClass;
        if (overrideClasses.length > dersize) {
            valueClass = overrideClasses[dersize];
            assert !overrideClasses[dersize].isCompatibleParent(value.result);
            overrideClasses = Arrays.copyOfRange(params, 0, dersize, ValueClass[].class);
        } else
            valueClass = value.result;

        // выполняем само создание свойства
        LP derDataProp = addDProp(group, sID, persistent, caption, valueClass, overrideClasses(commonClasses, overrideClasses));
        if (forced)
            derDataProp.setDerivedForcedChange(defaultChanged, derivedProp, params);
        else
            derDataProp.setDerivedChange(defaultChanged, derivedProp, params);
        return derDataProp;
    }

    private static ValueClass[] overrideClasses(ValueClass[] commonClasses, ValueClass[] overrideClasses) {
        ValueClass[] classes = new ValueClass[commonClasses.length];
        int ic = 0;
        for (ValueClass common : commonClasses) {
            ValueClass overrideClass;
            if (ic < overrideClasses.length && ((overrideClass = overrideClasses[ic]) != null)) {
                classes[ic++] = overrideClass;
                assert !overrideClass.isCompatibleParent(common);
            } else
                classes[ic++] = common;
        }
        return classes;
    }

    // сессионные

    protected LP addSDProp(String caption, ValueClass value, ValueClass... params) {
        return addSDProp((AbstractGroup) null, caption, value, params);
    }

    protected LP addSDProp(AbstractGroup group, String caption, ValueClass value, ValueClass... params) {
        return addSDProp(group, genSID(), caption, value, params);
    }

    protected LP addSDProp(String sID, String caption, ValueClass value, ValueClass... params) {
        return addSDProp(null, sID, caption, value, params);
    }

    protected LP addSDProp(String sID, boolean persistent, String caption, ValueClass value, ValueClass... params) {
        return addSDProp(null, sID, persistent, caption, value, params);
    }

    protected LP addSDProp(AbstractGroup group, String sID, String caption, ValueClass value, ValueClass... params) {
        return addSDProp(group, sID, false, caption, value, params);
    }

    protected LP addSDProp(AbstractGroup group, String sID, boolean persistent, String caption, ValueClass value, ValueClass... params) {
        return addProperty(group, persistent, new LP<ClassPropertyInterface>(new SessionDataProperty(sID, caption, params, value)));
    }

    protected LP addFAProp(String caption, FormEntity form, ObjectEntity... params) {
        return addFAProp(null, caption, form, params, new PropertyObjectEntity[0], new PropertyObjectEntity[0], false, false);
    }

    protected LP addFAProp(AbstractGroup group, FormEntity form, ObjectEntity... params) {
        return addFAProp(group, form.caption, form, params);
    }

    protected LP addFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity... params) {
        return addFAProp(group, caption, form, params, new PropertyObjectEntity[0], new PropertyObjectEntity[0], false, false);
    }

    protected LP addMFAProp(String caption, FormEntity form, ObjectEntity... params) {
        return addMFAProp(null, caption, form, params, new PropertyObjectEntity[0], new PropertyObjectEntity[0]);
    }

    protected LP addMFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, PropertyObjectEntity... setProperties) {
        return addMFAProp(group, caption, form, objectsToSet, true, setProperties);
    }

    protected LP addMFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, boolean newSession, PropertyObjectEntity... setProperties) {
        // во все setProperties просто будут записаны null'ы
        return addMFAProp(group, caption, form, objectsToSet, setProperties, new PropertyObjectEntity[setProperties.length], newSession);
    }

    protected LP addMFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, PropertyObjectEntity[] setProperties, PropertyObjectEntity[] getProperties) {
        return addMFAProp(group, caption, form, objectsToSet, setProperties, getProperties, true);
    }

    protected LP addMFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, PropertyObjectEntity[] setProperties, PropertyObjectEntity[] getProperties, boolean newSession) {
        return addFAProp(group, caption, form, objectsToSet, setProperties, getProperties, newSession, true);
    }

    protected LP addFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, PropertyObjectEntity[] setProperties, PropertyObjectEntity[] getProperties, boolean newSession, boolean isModal) {
        return addProperty(group, new LP<ClassPropertyInterface>(new FormActionProperty(genSID(), caption, form, objectsToSet, setProperties, getProperties, newSession, isModal)));
    }

    public LP addSelectFromListAction(AbstractGroup group, String caption, LP selectionProperty, ValueClass selectionClass, ValueClass... baseClasses) {
        return addSelectFromListAction(group, caption, null, new FilterEntity[0], selectionProperty, selectionClass, baseClasses);
    }

    public LP addSelectFromListAction(AbstractGroup group, String caption, ObjectEntity remapObject, FilterEntity[] remapFilters, LP selectionProperty, ValueClass selectionClass, ValueClass... baseClasses) {
        return addSelectFromListAction(group, caption, remapObject, remapFilters, selectionProperty, false, selectionClass, baseClasses);
    }

    public LP addSelectFromListAction(AbstractGroup group, String caption, ObjectEntity remapObject, FilterEntity[] remapFilters, LP selectionProperty, boolean isSelectionClassFirstParam, ValueClass selectionClass, ValueClass... baseClasses) {
        SelectFromListFormEntity selectFromListForm = new SelectFromListFormEntity(remapObject, remapFilters, selectionProperty, isSelectionClassFirstParam, selectionClass, baseClasses);
        return addMFAProp(group, caption, selectFromListForm, selectFromListForm.mainObjects, false);
    }
    
    protected LP addStopActionProp(String caption, String header) {
        return addAProp(new StopActionProperty(genSID(), caption, header));
    }

    protected LP addEAProp(ValueClass... params) {
        return addEAProp(null, genSID(), "email", null, params);
    }

    protected LP addEAProp(AbstractGroup group, String sID, String caption, String subject, ValueClass... params) {
        return addProperty(group, new LP<ClassPropertyInterface>(new EmailActionProperty(sID, caption, subject, this, params)));
    }

    protected <X extends PropertyInterface> void addEARecepient(LP<ClassPropertyInterface> eaProp, LP<X> emailProp, Integer... params) {
        Map<X, ClassPropertyInterface> mapInterfaces = new HashMap<X, ClassPropertyInterface>();
        for (int i = 0; i < emailProp.listInterfaces.size(); i++)
            mapInterfaces.put(emailProp.listInterfaces.get(i), eaProp.listInterfaces.get(params[i] - 1));
        ((EmailActionProperty) eaProp.property).addRecepient(new PropertyMapImplement<X, ClassPropertyInterface>(emailProp.property, mapInterfaces));
    }

    protected void addInlineEAForm(LP<ClassPropertyInterface> eaProp, FormEntity form, Object... params) {
        Map<ObjectEntity, ClassPropertyInterface> mapObjects = new HashMap<ObjectEntity, ClassPropertyInterface>();
        for (int i = 0; i < params.length / 2; i++)
            mapObjects.put((ObjectEntity) params[2 * i], eaProp.listInterfaces.get((Integer) params[2 * i + 1] - 1));
        ((EmailActionProperty) eaProp.property).addInlineForm(form, mapObjects);
    }

    protected void addAttachEAForm(LP<ClassPropertyInterface> eaProp, FormEntity form, EmailActionProperty.Format format, Object... params) {
        Map<ObjectEntity, ClassPropertyInterface> mapObjects = new HashMap<ObjectEntity, ClassPropertyInterface>();
        for (int i = 0; i < params.length / 2; i++)
            mapObjects.put((ObjectEntity) params[2 * i], eaProp.listInterfaces.get((Integer) params[2 * i + 1] - 1));
        ((EmailActionProperty) eaProp.property).addAttachmentForm(form, format, mapObjects);
    }

    protected LP addTAProp(LP sourceProperty, LP targetProperty) {
        return addProperty(null, new LP<ClassPropertyInterface>(new TranslateActionProperty(genSID(), "translate", this, sourceProperty, targetProperty, dictionary)));
    }

    protected <P extends PropertyInterface> LP addSCProp(LP<P> lp) {
        return addSCProp(privateGroup, "sys", lp);
    }

    protected <P extends PropertyInterface> LP addSCProp(AbstractGroup group, String caption, LP<P> lp) {
        return addProperty(group, new LP<ShiftChangeProperty.Interface<P>>(new ShiftChangeProperty<P, PropertyInterface>(genSID(), caption, lp.property, new PropertyMapImplement<PropertyInterface, P>(reverseBarcode.property))));
    }

    // добавляет свойство с бесконечным значением
    protected LP addICProp(DataClass valueClass, ValueClass... params) {
        return addProperty(privateGroup, false, new LP<ClassPropertyInterface>(new InfiniteClassProperty(genSID(), "Беск.", params, valueClass)));
    }

    protected LP addCProp(StaticClass valueClass, Object value, ValueClass... params) {
        return addCProp("sys", valueClass, value, params);
    }

    protected LP addCProp(String caption, StaticClass valueClass, Object value, ValueClass... params) {
        return addCProp(null, false, caption, valueClass, value, params);
    }

    protected LP addCProp(AbstractGroup group, boolean persistent, String caption, StaticClass valueClass, Object value, ValueClass... params) {
        return addCProp(group, persistent, caption, valueClass, value, Arrays.asList(params));
    }

    // только для того, чтобы обернуть все в IdentityLazy, так как только для List нормально сделан equals
    @IdentityLazy
    protected LP addCProp(AbstractGroup group, boolean persistent, String caption, StaticClass valueClass, Object value, List<ValueClass> params) {
        return addCProp(group, genSID(), persistent, caption, valueClass, value, params.toArray(new ValueClass[]{}));
    }

    protected LP addCProp(AbstractGroup group, String sID, boolean persistent, String caption, StaticClass valueClass, Object value, ValueClass... params) {
        return addProperty(group, persistent, new LP<ClassPropertyInterface>(new ValueClassProperty(sID, caption, params, valueClass, value)));
    }

    protected LP addTProp(Time time) {
        return addProperty(null, new LP<PropertyInterface>(new TimeFormulaProperty(genSID(), time)));
    }

    protected <P extends PropertyInterface> LP addTCProp(Time time, String sID, String caption, LP<P> changeProp, ValueClass... classes) {
        return addTCProp(null, time, sID, caption, changeProp, classes);
    }

    protected <P extends PropertyInterface> LP addTCProp(Time time, String sID, boolean persistent, String caption, LP<P> changeProp, ValueClass... classes) {
        return addTCProp(null, time, sID, persistent, caption, changeProp, classes);
    }

    protected <P extends PropertyInterface> LP addTCProp(AbstractGroup group, Time time, String sID, String caption, LP<P> changeProp, ValueClass... classes) {
        return addTCProp(group, time, sID, false, caption, changeProp, classes);
    }

    protected <P extends PropertyInterface> LP addTCProp(AbstractGroup group, Time time, String sID, boolean persistent, String caption, LP<P> changeProp, ValueClass... classes) {
        TimeChangeDataProperty<P> timeProperty = new TimeChangeDataProperty<P>(time, sID, caption, overrideClasses(changeProp.getMapClasses(), classes), changeProp.listInterfaces);
        changeProp.property.timeChanges.put(time, timeProperty);
        return addProperty(group, persistent, new LP<ClassPropertyInterface>(timeProperty));
    }

    protected LP addSFProp(String formula, ConcreteValueClass value, int paramCount) {
        return addProperty(null, new LP<StringFormulaProperty.Interface>(new StringFormulaProperty(genSID(), value, formula, paramCount)));
    }

    protected LP addCFProp(Compare compare) {
        return addProperty(null, new LP<CompareFormulaProperty.Interface>(new CompareFormulaProperty(genSID(), compare)));
    }

    protected <P extends PropertyInterface> LP addSProp(int intNum) {
        return addProperty(null, new LP<StringConcatenateProperty.Interface>(new StringConcatenateProperty(genSID(), "Объед.", intNum, " ")));
    }

    protected <P extends PropertyInterface> LP addSProp(int intNum, String separator) {
        return addProperty(null, new LP<StringConcatenateProperty.Interface>(new StringConcatenateProperty(genSID(), "Объед.", intNum, separator)));
    }

    protected <P extends PropertyInterface> LP addInsensitiveSProp(int intNum) {
        return addProperty(null, new LP<StringConcatenateProperty.Interface>(new StringConcatenateProperty(genSID(), "Объед.", intNum, " ", false)));
    }

    protected <P extends PropertyInterface> LP addInsensitiveSProp(int intNum, String separator) {
        return addProperty(null, new LP<StringConcatenateProperty.Interface>(new StringConcatenateProperty(genSID(), "Объед.", intNum, separator, false)));
    }

    protected LP addMFProp(ConcreteValueClass value, int paramCount) {
        return addProperty(null, new LP<StringFormulaProperty.Interface>(new MultiplyFormulaProperty(genSID(), value, paramCount)));
    }

    protected LP addAFProp(boolean... nots) {
        return addAFProp((AbstractGroup) null, nots);
    }

    protected LP addAFProp(String sID, boolean... nots) {
        return addAFProp(null, sID, nots);
    }

    protected LP addAFProp(AbstractGroup group, boolean... nots) {
        return addAFProp(group, genSID(), nots);
    }

    protected LP addAFProp(AbstractGroup group, String sID, boolean... nots) {
        return addProperty(group, new LP<AndFormulaProperty.Interface>(new AndFormulaProperty(sID, nots)));
    }

    protected LP addCCProp(int paramCount) {
        return addProperty(null, new LP<ConcatenateProperty.Interface>(new ConcatenateProperty(paramCount)));
    }

    protected LP addProp(Property<? extends PropertyInterface> prop) {
        return addProp(null, prop);
    }

    public LP addProp(AbstractGroup group, Property<? extends PropertyInterface> prop) {
        return addProperty(group, new LP(prop));
    }

    // Linear Implement

    private static abstract class LI {
        abstract <T extends PropertyInterface<T>> PropertyInterfaceImplement<T> map(List<T> interfaces);

        abstract Object[] write();

        abstract Object[] compare(LP compare, BusinessLogics BL, int intOff);
    }

    private static class LII extends LI {
        int intNum;

        private LII(int intNum) {
            this.intNum = intNum;
        }

        <T extends PropertyInterface<T>> PropertyInterfaceImplement<T> map(List<T> interfaces) {
            return interfaces.get(intNum - 1);
        }

        Object[] write() {
            return new Object[]{intNum};
        }

        Object[] compare(LP compare, BusinessLogics BL, int intOff) {
            return new Object[]{compare, intNum, intNum + intOff};
        }
    }

    private static class LMI<P extends PropertyInterface> extends LI {
        LP<P> lp;
        int[] mapInt;

        private LMI(LP<P> lp) {
            this.lp = lp;
            this.mapInt = new int[lp.listInterfaces.size()];
        }

        <T extends PropertyInterface<T>> PropertyInterfaceImplement<T> map(List<T> interfaces) {
            PropertyMapImplement<P, T> mapRead = new PropertyMapImplement<P, T>(lp.property);
            for (int i = 0; i < lp.listInterfaces.size(); i++)
                mapRead.mapping.put(lp.listInterfaces.get(i), interfaces.get(mapInt[i] - 1));
            return mapRead;
        }

        Object[] write() {
            Object[] result = new Object[mapInt.length + 1];
            result[0] = lp;
            for (int i = 0; i < mapInt.length; i++)
                result[i + 1] = mapInt[i];
            return result;
        }

        Object[] compare(LP compare, BusinessLogics BL, int intOff) {
            int lmiLen = mapInt.length;
            Object[] common = new Object[lmiLen * 2 + 1];
            Object[] shift = new Object[lmiLen + 1];
            shift[0] = lp;
            for (int j = 1; j <= lmiLen; j++) {
                shift[j] = j + lmiLen;
                common[j] = mapInt[j - 1];
                common[j + lmiLen] = mapInt[j - 1] + intOff;
            }
            common[0] = BL.addJProp(compare, BaseUtils.add(BL.directLI(lp), shift));
            return common;
        }
    }

    public static Object[] getUParams(LP[] props, int exoff) {
        int intNum = props[0].listInterfaces.size();
        Object[] params = new Object[props.length * (1 + intNum + exoff)];
        for (int i = 0; i < props.length; i++) {
            if (exoff > 0)
                params[i * (1 + intNum + exoff)] = 1;
            params[i * (1 + intNum + exoff) + exoff] = props[i];
            for (int j = 1; j <= intNum; j++)
                params[i * (1 + intNum + exoff) + exoff + j] = j;
        }
        return params;
    }

    protected Object[] directLI(LP prop) {
        return getUParams(new LP[]{prop}, 0);
    }

    // считывает "линейные" имплементации

    static List<LI> readLI(Object[] params) {
        List<LI> result = new ArrayList<LI>();
        for (int i = 0; i < params.length; i++)
            if (params[i] instanceof Integer)
                result.add(new LII((Integer) params[i]));
            else {
                LMI impl = new LMI((LP) params[i]);
                for (int j = 0; j < impl.mapInt.length; j++)
                    impl.mapInt[j] = (Integer) params[i + j + 1];
                i += impl.mapInt.length;
                result.add(impl);
            }
        return result;
    }

    static Object[] writeLI(List<LI> linearImpl) {
        Object[][] objectLI = new Object[linearImpl.size()][];
        for (int i = 0; i < linearImpl.size(); i++)
            objectLI[i] = linearImpl.get(i).write();
        int size = 0;
        for (Object[] li : objectLI)
            size += li.length;
        Object[] result = new Object[size];
        int i = 0;
        for (Object[] li : objectLI)
            for (Object param : li)
                result[i++] = param;
        return result;
    }

    static <T extends PropertyInterface> List<PropertyInterfaceImplement<T>> mapLI(List<LI> linearImpl, List<T> interfaces) {
        List<PropertyInterfaceImplement<T>> result = new ArrayList<PropertyInterfaceImplement<T>>();
        for (LI impl : linearImpl)
            result.add(impl.map(interfaces));
        return result;
    }

    public static <T extends PropertyInterface> List<PropertyInterfaceImplement<T>> readImplements(List<T> listInterfaces, Object... params) {
        return mapLI(readLI(params), listInterfaces);
    }

    protected <T extends LP<?>> T addProperty(AbstractGroup group, T lp) {
        return addProperty(group, false, lp);
    }

    private <T extends LP<?>> T addProperty(AbstractGroup group, boolean persistent, T lp) {
        registerProperty(lp);
        if (group != null) {
            group.add(lp.property);
        } else {
            privateGroup.add(lp.property);
        }
        if (persistent) {
            addPersistent(lp);
        }
        return lp;
    }

    private <T extends LP<?>> void registerProperty(T lp) {
        lproperties.add(lp);
        lp.property.ID = idGenerator.idShift();
    }

    protected LP getLP(String sID) {
        objectValue.getProperty(sID);
        selection.getProperty(sID);
        for (LP lp : lproperties) {
            if (lp.property.sID.equals(sID)) {
                return lp;
            }
        }
        return null;
    }

    protected LP addJProp(LP mainProp, Object... params) {
        return addJProp(privateGroup, "sys", mainProp, params);
    }

    protected LP addJProp(String caption, LP mainProp, Object... params) {
        return addJProp((AbstractGroup) null, caption, mainProp, params);
    }

    protected LP addJProp(String sID, String caption, LP mainProp, Object... params) {
        return addJProp(sID, false, caption, mainProp, params);
    }

    protected LP addJProp(String sID, boolean persistent, String caption, LP mainProp, Object... params) {
        return addJProp(null, sID, persistent, caption, mainProp, params);
    }

    protected LP addJProp(AbstractGroup group, String caption, LP mainProp, Object... params) {
        return addJProp(group, genSID(), caption, mainProp, params);
    }

    protected LP addJProp(boolean implementChange, String caption, LP mainProp, Object... params) {
        return addJProp((AbstractGroup) null, implementChange, caption, mainProp, params);
    }

    protected LP addJProp(boolean implementChange, String sID, String caption, LP mainProp, Object... params) {
        return addJProp((AbstractGroup) null, implementChange, sID, caption, mainProp, params);
    }

    protected LP addJProp(AbstractGroup group, boolean implementChange, String caption, LP mainProp, Object... params) {
        return addJProp(group, implementChange, genSID(), caption, mainProp, params);
    }

    protected int getIntNum(Object[] params) {
        int intNum = 0;
        for (Object param : params)
            if (param instanceof Integer)
                intNum = BaseUtils.max(intNum, (Integer) param);
        return intNum;
    }

    public static <T extends PropertyInterface, P extends PropertyInterface> PropertyImplement<PropertyInterfaceImplement<P>, T> mapImplement(LP<T> property, List<PropertyInterfaceImplement<P>> propImpl) {
        int mainInt = 0;
        Map<T, PropertyInterfaceImplement<P>> mapping = new HashMap<T, PropertyInterfaceImplement<P>>();
        for (PropertyInterfaceImplement<P> implement : propImpl) {
            mapping.put(property.listInterfaces.get(mainInt), implement);
            mainInt++;
        }
        return new PropertyImplement<PropertyInterfaceImplement<P>, T>(property.property, mapping);
    }

    protected LP addJProp(AbstractGroup group, String sID, String caption, LP mainProp, Object... params) {
        return addJProp(group, false, sID, caption, mainProp, params);
    }

    protected LP addJProp(AbstractGroup group, String sID, boolean persistent, String caption, LP mainProp, Object... params) {
        return addJProp(group, false, sID, persistent, caption, mainProp, params);
    }

    protected LP addJProp(boolean implementChange, LP mainProp, Object... params) {
        return addJProp(privateGroup, implementChange, genSID(), "sys", mainProp, params);
    }

    protected LP addJProp(AbstractGroup group, boolean implementChange, String sID, String caption, LP mainProp, Object... params) {
        return addJProp(group, implementChange, sID, false, caption, mainProp, params);
    }

    protected LP addJProp(AbstractGroup group, boolean implementChange, String sID, boolean persistent, String caption, LP mainProp, Object... params) {

        JoinProperty<?> property = new JoinProperty(sID, caption, getIntNum(params), implementChange);
        property.inheritFixedCharWidth(mainProp.property);

        LP listProperty = new LP<JoinProperty.Interface>(property);
        property.implement = mapImplement(mainProp, readImplements(listProperty.listInterfaces, params));

        return addProperty(group, persistent, listProperty);
    }

    protected LP[] addJProp(AbstractGroup group, boolean implementChange, String paramID, LP[] props, String caption, Object... params) {
        LP[] result = new LP[props.length];
        for(int i=0;i<props.length;i++)
            result[i] = addJProp(group, implementChange, paramID + props[i].property.sID, props[i].property.caption + (caption.length()==0?"":(" " + caption)), props[i], params);
        return result;
    }

    protected LP[] addJProp(AbstractGroup group, boolean implementChange, String paramID, LP[] props, Object... params) {
        return addJProp(group, implementChange, paramID, props, "", params);
    }

    private <T extends PropertyInterface> LP addGProp(AbstractGroup group, String sID, boolean persistent, String caption, LP<T> groupProp, List<PropertyInterfaceImplement<T>> listImplements) {

        GroupProperty<T> property = new SumGroupProperty<T>(sID, caption, listImplements, groupProp.property);
        return mapLGProp(group, persistent, property, listImplements);
    }

    private <P extends PropertyInterface, L extends PropertyInterface> LP mapLProp(AbstractGroup group, boolean persistent, PropertyMapImplement<L, P> implement, LP<P> property) {
        return addProperty(group, persistent, new LP<L>(implement.property, BaseUtils.mapList(property.listInterfaces, BaseUtils.reverse(implement.mapping))));
    }

    private <P extends PropertyInterface, L extends PropertyInterface> LP mapLGProp(AbstractGroup group, PropertyImplement<PropertyInterfaceImplement<P>, L> implement, List<PropertyInterfaceImplement<P>> listImplements) {
        return mapLGProp(group, false, implement, listImplements);
    }

    private <P extends PropertyInterface, L extends PropertyInterface> LP mapLGProp(AbstractGroup group, boolean persistent, PropertyImplement<PropertyInterfaceImplement<P>, L> implement, List<PropertyInterfaceImplement<P>> listImplements) {
        return addProperty(group, persistent, new LP<L>(implement.property, BaseUtils.mapList(listImplements, BaseUtils.reverse(implement.mapping))));
    }

    private <P extends PropertyInterface> LP mapLGProp(AbstractGroup group, boolean persistent, GroupProperty<P> property, List<PropertyInterfaceImplement<P>> listImplements) {
        return mapLGProp(group, persistent, new PropertyImplement<PropertyInterfaceImplement<P>, GroupProperty.Interface<P>>(property, property.getMapInterfaces()), listImplements);
    }

    protected <P extends PropertyInterface> LP addOProp(String caption, OrderType orderType, LP<P> sum, boolean ascending, boolean includeLast, int partNum, Object... params) {
        return addOProp(genSID(), caption, orderType, sum, ascending, includeLast, partNum, params);
    }

    protected <P extends PropertyInterface> LP addOProp(String sID, String caption, OrderType orderType, LP<P> sum, boolean ascending, boolean includeLast, int partNum, Object... params) {
        return addOProp((AbstractGroup) null, sID, caption, orderType, sum, ascending, includeLast, partNum, params);
    }

    protected <P extends PropertyInterface> LP addOProp(AbstractGroup group, String caption, OrderType orderType, LP<P> sum, boolean ascending, boolean includeLast, int partNum, Object... params) {
        return addOProp(group, genSID(), caption, orderType, sum, ascending, includeLast, partNum, params);
    }

    protected <P extends PropertyInterface> LP addOProp(AbstractGroup group, String sID, String caption, OrderType orderType, LP<P> sum, boolean ascending, boolean includeLast, int partNum, Object... params) {
        return addOProp(group, sID, false, caption, sum, false, orderType, ascending, includeLast, partNum, params);
    }

    // проценты
    protected <P extends PropertyInterface> LP addPOProp(AbstractGroup group, String caption, LP<P> sum, boolean ascending, boolean includeLast, int partNum, Object... params) {
        return addPOProp(group, genSID(), false, caption, sum, ascending, includeLast, partNum, params);
    }

    protected <P extends PropertyInterface> LP addPOProp(AbstractGroup group, String sID, boolean persistent, String caption, LP<P> sum, boolean ascending, boolean includeLast, int partNum, Object... params) {
        return addOProp(group, sID, persistent, caption, sum, true, null, ascending, includeLast, partNum, params);
    }

    private <P extends PropertyInterface> LP addOProp(AbstractGroup group, String sID, boolean persistent, String caption, LP<P> sum, boolean percent, OrderType orderType, boolean ascending, boolean includeLast, int partNum, Object... params) {
        List<LI> li = readLI(params);

        Collection<PropertyInterfaceImplement<P>> partitions = mapLI(li.subList(0, partNum), sum.listInterfaces);
        OrderedMap<PropertyInterfaceImplement<P>, Boolean> orders = new OrderedMap<PropertyInterfaceImplement<P>, Boolean>(mapLI(li.subList(partNum, li.size()), sum.listInterfaces), !ascending);

        PropertyMapImplement<?, P> orderProperty;
        if (percent)
            orderProperty = DerivedProperty.createPOProp(sID, caption, sum.property, partitions, orders, includeLast);
        else
            orderProperty = DerivedProperty.createOProp(sID, caption, orderType, sum.property, partitions, orders, includeLast);

        return mapLProp(group, persistent, orderProperty, sum);
    }

    protected <R extends PropertyInterface, L extends PropertyInterface> LP addUGProp(AbstractGroup group, String caption, boolean ascending, LP<R> restriction, LP<L> ungroup, Object... params) {
        return addUGProp(group, genSID(), caption, ascending, restriction, ungroup, params);
    }

    protected <R extends PropertyInterface, L extends PropertyInterface> LP addUGProp(AbstractGroup group, String sID, String caption, boolean ascending, LP<R> restriction, LP<L> ungroup, Object... params) {
        return addUGProp(group, sID, false, caption, ascending, restriction, ungroup, params);
    }

    protected <R extends PropertyInterface, L extends PropertyInterface> LP addUGProp(AbstractGroup group, String sID, boolean persistent, String caption, boolean ascending, LP<R> restriction, LP<L> ungroup, Object... params) {
        List<LI> li = readLI(params);

        Map<L, PropertyInterfaceImplement<R>> groupImplement = new HashMap<L, PropertyInterfaceImplement<R>>();
        for (int i = 0; i < ungroup.listInterfaces.size(); i++)
            groupImplement.put(ungroup.listInterfaces.get(i), li.get(i).map(restriction.listInterfaces));
        OrderedMap<PropertyInterfaceImplement<R>, Boolean> orders = new OrderedMap<PropertyInterfaceImplement<R>, Boolean>(mapLI(li.subList(ungroup.listInterfaces.size(), li.size()), restriction.listInterfaces), ascending);
        return mapLProp(group, persistent, DerivedProperty.createUGProp(sID, caption, new PropertyImplement<PropertyInterfaceImplement<R>, L>(ungroup.property, groupImplement), orders, restriction.property), restriction);
    }

    protected <R extends PropertyInterface, L extends PropertyInterface> LP addPGProp(AbstractGroup group, String sID, boolean persistent, int roundlen, boolean roundfirst, String caption, LP<R> proportion, LP<L> ungroup, Object... params) {
        List<LI> li = readLI(params);

        Map<L, PropertyInterfaceImplement<R>> groupImplement = new HashMap<L, PropertyInterfaceImplement<R>>();
        for (int i = 0; i < ungroup.listInterfaces.size(); i++)
            groupImplement.put(ungroup.listInterfaces.get(i), li.get(i).map(proportion.listInterfaces));
        return mapLProp(group, persistent, DerivedProperty.createPGProp(sID, caption, roundlen, roundfirst, baseClass, new PropertyImplement<PropertyInterfaceImplement<R>, L>(ungroup.property, groupImplement), proportion.property), proportion);
    }

    /*
      // свойство обратное группируещему - для этого задается ограничивающее свойство, результирующее св-во с группировочными, порядковое св-во
      protected LF addUGProp(AbstractGroup group, String title, LF maxGroupProp, LF unGroupProp, Object... params) {
          List<LI> lParams = readLI(params);
          List<LI> lUnGroupParams = lParams.subList(0,unGroupProp.listInterfaces.size());
          List<LI> orderParams = lParams.subList(unGroupProp.listInterfaces.size(),lParams.size());

          int intNum = maxGroupProp.listInterfaces.size();

          // "двоим" интерфейсы, для результ. св-ва
          // ставим equals'ы на группировочные свойства (раздвоенные)
          List<Object[]> groupParams = new ArrayList<Object[]>();
          groupParams.add(directLI(maxGroupProp));
          for(LI li : lUnGroupParams)
              groupParams.add(li.compare(equals2, this, intNum));

          boolean[] andParams = new boolean[groupParams.size()-1];
          for(int i=0;i<andParams.length;i++)
              andParams[i] = false;
          LF groupPropSet = addJProp(addAFProp(andParams),BaseUtils.add(groupParams));

          for(int i=0;i<intNum;i++) { // докинем не достающие порядки
              boolean found = false;
              for(LI order : orderParams)
                  if(order instanceof LII && ((LII)order).intNum==i+1) {
                      found = true;
                      break;
                  }
              if(!found)
                  orderParams.add(new LII(i+1));
          }

          // ставим на предшествие сначала order'а, потом всех интерфейсов
          LF[] orderProps = new LF[orderParams.size()];
          for(int i=0;i<orderParams.size();i++) {
              orderProps[i] = (addJProp(and1, BaseUtils.add(directLI(groupPropSet),orderParams.get(i).compare(greater2, this, intNum))));
              groupPropSet = addJProp(and1, BaseUtils.add(directLI(groupPropSet),orderParams.get(i).compare(equals2, this, intNum)));
          }
          LF groupPropPrev = addSUProp(Union.OVERRIDE, orderProps);

          // группируем суммируя по "задвоенным" св-вам maxGroup
          Object[] remainParams = new Object[intNum];
          for(int i=1;i<=intNum;i++)
              remainParams[i-1] = i+intNum;
          LF remainPrev = addSGProp(groupPropPrev, remainParams);

          // создадим группировочное св-во с маппом на общий интерфейс, нужно поубирать "дырки"


          // возвращаем MIN2(unGroup-MU(prevGroup,0(maxGroup)),maxGroup) и не unGroup<=prevGroup
          LF zeroQuantity = addJProp(and1, BaseUtils.add(new Object[]{vzero},directLI(maxGroupProp)));
          LF zeroRemainPrev = addSUProp(Union.OVERRIDE , zeroQuantity, remainPrev);
          LF calc = addSFProp("prm3+prm1-prm2-GREATEST(prm3,prm1-prm2)",DoubleClass.instance,3);
          LF maxRestRemain = addJProp(calc, BaseUtils.add(BaseUtils.add(unGroupProp.write(),directLI(zeroRemainPrev)),directLI(maxGroupProp)));
          LF exceed = addJProp(groeq2, BaseUtils.add(directLI(remainPrev),unGroupProp.write()));
          return addJProp(group, title, andNot1, BaseUtils.add(directLI(maxRestRemain),directLI(exceed)));
      }
    */

    protected LP addSGProp(LP groupProp, Object... params) {
        return addSGProp(privateGroup, "sys", groupProp, params);
    }

    protected LP addSGProp(String caption, LP groupProp, Object... params) {
        return addSGProp((AbstractGroup) null, caption, groupProp, params);
    }

    protected LP addSGProp(AbstractGroup group, String caption, LP groupProp, Object... params) {
        return addSGProp(group, genSID(), caption, groupProp, params);
    }

    protected LP addSGProp(String sID, String caption, LP groupProp, Object... params) {
        return addSGProp(sID, false, caption, groupProp, params);
    }

    protected LP addSGProp(String sID, boolean persistent, String caption, LP groupProp, Object... params) {
        return addSGProp(null, sID, persistent, caption, groupProp, params);
    }

    protected LP addSGProp(AbstractGroup group, String sID, String caption, LP groupProp, Object... params) {
        return addSGProp(group, sID, false, caption, groupProp, params);
    }

    protected LP addSGProp(AbstractGroup group, String sID, boolean persistent, String caption, LP groupProp, Object... params) {
        return addSGProp(group, sID, persistent, false, caption, groupProp, params);
    }

    protected <T extends PropertyInterface> LP addSGProp(AbstractGroup group, String sID, boolean persistent, boolean notZero, String caption, LP<T> groupProp, Object... params) {
        return addSGProp(group, sID, persistent, notZero, caption, groupProp, readImplements(groupProp.listInterfaces, params));
    }

    private <T extends PropertyInterface> LP addSGProp(AbstractGroup group, String sID, boolean persistent, boolean notZero, String caption, LP<T> groupProp, List<PropertyInterfaceImplement<T>> listImplements) {
        boolean wrapNotZero = persistent && (notZero || !Settings.instance.isDisableSumGroupNotZero());
        SumGroupProperty<T> property = new SumGroupProperty<T>(wrapNotZero?genSID():sID, caption, listImplements, groupProp.property);

        LP result;
        if (wrapNotZero)
            result = addJProp(group, sID, persistent, caption, onlyNotZero, directLI(mapLGProp(null, false, property, listImplements)));
        else
            result = mapLGProp(group, persistent, property, listImplements);

        result.sumGroup = property; // так как может wrap'ся, использование - setDG
        result.groupProperty = groupProp; // для порядка параметров, использование - setDG

        return result;
    }

    protected LP addMGProp(LP groupProp, Object... params) {
        return addMGProp(privateGroup, genSID(), "sys", groupProp, params);
    }

    protected LP addMGProp(AbstractGroup group, String sID, String caption, LP groupProp, Object... params) {
        return addMGProp(group, sID, false, caption, groupProp, params);
    }

    protected LP addMGProp(AbstractGroup group, String sID, boolean persist, String caption, LP groupProp, Object... params) {
        return addMGProp(group, persist, new String[]{sID}, new String[]{caption}, 0, groupProp, params)[0];
    }

    protected <T extends PropertyInterface> LP[] addMGProp(AbstractGroup group, String[] ids, String[] captions, int extra, LP<T> groupProp, Object... params) {
        return addMGProp(group, false, ids, captions, extra, groupProp, params);
    }

    protected <T extends PropertyInterface> LP[] addMGProp(AbstractGroup group, boolean persist, String[] ids, String[] captions, int extra, LP<T> groupProp, Object... params) {
        LP[] result = new LP[extra + 1];

        Collection<Property> suggestPersist = new ArrayList<Property>();

        List<PropertyInterfaceImplement<T>> listImplements = readImplements(groupProp.listInterfaces, params);
        List<PropertyInterfaceImplement<T>> groupImplements = listImplements.subList(extra, listImplements.size());
        List<PropertyImplement<PropertyInterfaceImplement<T>, ?>> mgProps = DerivedProperty.createMGProp(ids, captions, groupProp.property, baseClass,
                listImplements.subList(0, extra), new HashSet<PropertyInterfaceImplement<T>>(groupImplements), suggestPersist);

        if (persist)
            for (Property property : suggestPersist)
                addPersistent(addProperty(null, new LP(property)));

        for (int i = 0; i < mgProps.size(); i++)
            result[i] = mapLGProp(group, mgProps.get(i), groupImplements);
        return result;

        /*
        List<LI> li = readLI(params);
        Object[] interfaces = writeLI(li.subList(extra,li.size())); // "вырежем" группировочные интерфейсы

        LF[] result = new LF[extra+1];
        int i = 0;
        do {
            result[i] = addGProp(group,ids[i],captions[i],groupProp,false,interfaces);
            if(i<extra) // если не последняя
                groupProp = addJProp(and1, BaseUtils.add(li.get(i).write(),directLI( // само свойство
                        addJProp(equals2, BaseUtils.add(directLI(groupProp),directLI( // только те кто дает предыдущий максимум
                        addJProp(result[i], interfaces))))))); // предыдущий максимум
        } while (i++<extra);
        return result;*/
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LP addCGProp(AbstractGroup group, String sID, String caption, LP<T> groupProp, LP<P> dataProp, Object... params) {
        return addCGProp(group, true, sID, caption, groupProp, dataProp, params);
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LP addCGProp(AbstractGroup group, String sID, boolean persistent, String caption, LP<T> groupProp, LP<P> dataProp, Object... params) {
        return addCGProp(group, true, sID, persistent, caption, groupProp, dataProp, params);
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LP addCGProp(AbstractGroup group, boolean checkChange, String sID, String caption, LP<T> groupProp, LP<P> dataProp, Object... params) {
        return addCGProp(group, checkChange, sID, false, caption, groupProp, dataProp, params);
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LP addCGProp(AbstractGroup group, boolean checkChange, String sID, boolean persistent, String caption, LP<T> groupProp, LP<P> dataProp, Object... params) {
        List<PropertyInterfaceImplement<T>> listImplements = readImplements(groupProp.listInterfaces, params);
        CycleGroupProperty<T, P> property = new CycleGroupProperty<T, P>(sID, caption, listImplements, groupProp.property, dataProp.property);

        // нужно добавить ограничение на уникальность
        addProperty(null, new LP(property.getConstrainedProperty(checkChange)));

        return mapLGProp(group, persistent, property, listImplements);
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LP addDGProp(int orders, boolean ascending, LP<T> groupProp, Object... params) {
        return addDGProp(privateGroup, "sys", orders, ascending, groupProp, params);
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LP addDGProp(AbstractGroup group, String caption, int orders, boolean ascending, LP<T> groupProp, Object... params) {
        return addDGProp(group, genSID(), caption, orders, ascending, groupProp, params);
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LP addDGProp(AbstractGroup group, String sID, String caption, int orders, boolean ascending, LP<T> groupProp, Object... params) {
        return addDGProp(group, sID, false, caption, orders, ascending, groupProp, params);
    }

    protected <T extends PropertyInterface> LP addDGProp(AbstractGroup group, String sID, boolean persistent, String caption, int orders, boolean ascending, LP<T> groupProp, Object... params) {
        List<PropertyInterfaceImplement<T>> listImplements = readImplements(groupProp.listInterfaces, params);  int intNum = listImplements.size();
        LP result = addSGProp(group, sID, persistent, false, caption, groupProp, listImplements.subList(0, intNum - orders - 1));
        result.setDG(ascending, listImplements.subList(intNum - orders - 1, intNum));
        return result;
    }

    protected LP addUProp(AbstractGroup group, String sID, String caption, Union unionType, Object... params) {
        return addUProp(group, sID, false, caption, unionType, params);
    }

    protected LP addUProp(AbstractGroup group, String sID, boolean persistent, String caption, Union unionType, Object... params) {

        int intNum = ((LP) params[unionType == Union.SUM ? 1 : 0]).listInterfaces.size();

        UnionProperty property = null;
        int extra = 0;
        switch (unionType) {
            case MAX:
                property = new MaxUnionProperty(sID, caption, intNum);
                break;
            case SUM:
                property = new SumUnionProperty(sID, caption, intNum);
                extra = 1;
                break;
            case OVERRIDE:
                property = new OverrideUnionProperty(sID, caption, intNum);
                break;
            case EXCLUSIVE:
                property = new ExclusiveUnionProperty(sID, caption, intNum);
                break;
        }

        LP listProperty = new LP<UnionProperty.Interface>(property);

        for (int i = 0; i < params.length / (intNum + 1 + extra); i++) {
            Integer offs = i * (intNum + 1 + extra);
            LP<?> opImplement = (LP) params[offs + extra];
            PropertyMapImplement operand = new PropertyMapImplement(opImplement.property);
            for (int j = 0; j < intNum; j++)
                operand.mapping.put(opImplement.listInterfaces.get(((Integer) params[offs + 1 + extra + j]) - 1), listProperty.listInterfaces.get(j));

            switch (unionType) {
                case MAX:
                    ((MaxUnionProperty) property).operands.add(operand);
                    break;
                case SUM:
                    ((SumUnionProperty) property).operands.put(operand, (Integer) params[offs]);
                    break;
                case OVERRIDE:
                    ((OverrideUnionProperty) property).operands.add(operand);
                    break;
                case EXCLUSIVE:
                    ((ExclusiveUnionProperty) property).operands.add(operand);
                    break;
            }
        }

        return addProperty(group, persistent, listProperty);
    }

    // объединение классовое (непересекающихся) свойств

    protected LP addCUProp(LP... props) {
        return addCUProp(privateGroup, "sys", props);
    }

    protected LP addCUProp(String caption, LP... props) {
        return addCUProp((AbstractGroup) null, caption, props);
    }

    protected LP addCUProp(AbstractGroup group, String caption, LP... props) {
        return addCUProp(group, genSID(), caption, props);
    }

    protected LP addCUProp(String sID, String caption, LP... props) {
        return addCUProp(sID, false, caption, props);
    }

    protected LP addCUProp(String sID, boolean persistent, String caption, LP... props) {
        return addCUProp(null, sID, persistent, caption, props);
    }

    Collection<LP[]> checkCUProps = new ArrayList<LP[]>();

    // объединяет разные по классам св-ва

    protected LP addCUProp(AbstractGroup group, String sID, String caption, LP... props) {
        return addCUProp(group, sID, false, caption, props);
    }

    protected LP addCUProp(AbstractGroup group, String sID, boolean persistent, String caption, LP... props) {
        assert checkCUProps.add(props);
        return addXSUProp(group, sID, persistent, caption, props);
    }

    // разница

    protected LP addDUProp(LP prop1, LP prop2) {
        return addDUProp(privateGroup, "sys", prop1, prop2);
    }

    protected LP addDUProp(String caption, LP prop1, LP prop2) {
        return addDUProp((AbstractGroup) null, caption, prop1, prop2);
    }

    protected LP addDUProp(AbstractGroup group, String caption, LP prop1, LP prop2) {
        return addDUProp(group, genSID(), caption, prop1, prop2);
    }

    protected LP addDUProp(String sID, String caption, LP prop1, LP prop2) {
        return addDUProp(null, sID, caption, prop1, prop2);
    }

    protected LP addDUProp(String sID, boolean persistent, String caption, LP prop1, LP prop2) {
        return addDUProp(null, sID, persistent, caption, prop1, prop2);
    }

    protected LP addDUProp(AbstractGroup group, String sID, String caption, LP prop1, LP prop2) {
        return addDUProp(group, sID, false, caption, prop1, prop2);
    }

    protected LP addDUProp(AbstractGroup group, String sID, boolean persistent, String caption, LP prop1, LP prop2) {
        int intNum = prop1.listInterfaces.size();
        Object[] params = new Object[2 * (2 + intNum)];
        params[0] = 1;
        params[1] = prop1;
        for (int i = 0; i < intNum; i++)
            params[2 + i] = i + 1;
        params[2 + intNum] = -1;
        params[3 + intNum] = prop2;
        for (int i = 0; i < intNum; i++)
            params[4 + intNum + i] = i + 1;
        return addUProp(group, sID, persistent, caption, Union.SUM, params);
    }

    protected LP addNUProp(LP prop) {
        return addNUProp(privateGroup, genSID(), "sys", prop);
    }

    protected LP addNUProp(AbstractGroup group, String sID, String caption, LP prop) {
        return addNUProp(group, sID, false, caption, prop);
    }

    protected LP addNUProp(AbstractGroup group, String sID, boolean persistent, String caption, LP prop) {
        int intNum = prop.listInterfaces.size();
        Object[] params = new Object[2 + intNum];
        params[0] = -1;
        params[1] = prop;
        for (int i = 0; i < intNum; i++)
            params[2 + i] = i + 1;
        return addUProp(group, sID, persistent, caption, Union.SUM, params);
    }

    protected LP addLProp(LP lp, ValueClass... classes) {
        return addDCProp("LG_" + lp.property.sID, "Лог " + lp.property, object1, BaseUtils.add(BaseUtils.add(directLI(lp), new Object[]{addJProp(equals2, 1, currentSession), lp.listInterfaces.size() + 1}), classes));
    }

    // XOR

    protected LP addXorUProp(LP prop1, LP prop2) {
        return addXorUProp(privateGroup, genSID(), "sys", prop1, prop2);
    }

    protected LP addXorUProp(AbstractGroup group, String sID, String caption, LP prop1, LP prop2) {
        return addXorUProp(group, sID, false, caption, prop1, prop2);
    }

    protected LP addXorUProp(AbstractGroup group, String sID, boolean persistent, String caption, LP prop1, LP prop2) {
        int intNum = prop1.listInterfaces.size();
        Object[] params = new Object[2 * (1 + intNum)];
        params[0] = prop1;
        for (int i = 0; i < intNum; i++)
            params[1 + i] = i + 1;
        params[1 + intNum] = prop2;
        for (int i = 0; i < intNum; i++)
            params[2 + intNum + i] = i + 1;
        return addXSUProp(group, sID, persistent, caption, addJProp(andNot1, getUParams(new LP[]{prop1, prop2}, 0)), addJProp(andNot1, getUParams(new LP[]{prop2, prop1}, 0)));
    }

    // IF и IF ELSE

    protected LP addIfProp(LP prop, boolean not, LP ifProp, Object... params) {
        return addIfProp(privateGroup, genSID(), "sys", prop, not, ifProp, params);
    }

    protected LP addIfProp(AbstractGroup group, String sID, String caption, LP prop, boolean not, LP ifProp, Object... params) {
        return addIfProp(group, sID, false, caption, prop, not, ifProp, params);
    }

    protected LP addIfProp(AbstractGroup group, String sID, boolean persistent, String caption, LP prop, boolean not, LP ifProp, Object... params) {
        return addJProp(group, sID, persistent, caption, and(not), BaseUtils.add(getUParams(new LP[]{prop}, 0), BaseUtils.add(new LP[]{ifProp}, params)));
    }

    protected LP addIfElseUProp(LP prop1, LP prop2, LP ifProp, Object... params) {
        return addIfElseUProp(privateGroup, "sys", prop1, prop2, ifProp, params);
    }

    protected LP addIfElseUProp(AbstractGroup group, String caption, LP prop1, LP prop2, LP ifProp, Object... params) {
        return addIfElseUProp(group, genSID(), caption, prop1, prop2, ifProp, params);
    }

    protected LP addIfElseUProp(AbstractGroup group, String sID, String caption, LP prop1, LP prop2, LP ifProp, Object... params) {
        return addIfElseUProp(group, sID, false, caption, prop1, prop2, ifProp, params);
    }

    protected LP addIfElseUProp(AbstractGroup group, String sID, boolean persistent, String caption, LP prop1, LP prop2, LP ifProp, Object... params) {
        return addXSUProp(group, sID, persistent, caption, addIfProp(prop1, false, ifProp, params), addIfProp(prop2, true, ifProp, params));
    }

    // объединение пересекающихся свойств

    protected LP addSUProp(Union unionType, LP... props) {
        return addSUProp(privateGroup, "sys", unionType, props);
    }

    protected LP addSUProp(String caption, Union unionType, LP... props) {
        return addSUProp((AbstractGroup) null, caption, unionType, props);
    }

    protected LP addSUProp(AbstractGroup group, String caption, Union unionType, LP... props) {
        return addSUProp(group, genSID(), caption, unionType, props);
    }

    protected LP addSUProp(String sID, String caption, Union unionType, LP... props) {
        return addSUProp(sID, false, caption, unionType, props);
    }

    protected LP addSUProp(String sID, boolean persistent, String caption, Union unionType, LP... props) {
        return addSUProp(null, sID, persistent, caption, unionType, props);
    }

    Collection<LP[]> checkSUProps = new ArrayList<LP[]>();

    // объединяет разные по классам св-ва

    protected LP addSUProp(AbstractGroup group, String sID, String caption, Union unionType, LP... props) {
        return addSUProp(group, sID, false, caption, unionType, props);
    }

    protected LP addSUProp(AbstractGroup group, String sID, boolean persistent, String caption, Union unionType, LP... props) {
        assert checkSUProps.add(props);
        return addUProp(group, sID, persistent, caption, unionType, getUParams(props, (unionType == Union.SUM ? 1 : 0)));
    }

    protected LP addXSUProp(AbstractGroup group, String caption, LP... props) {
        return addXSUProp(group, genSID(), caption, props);
    }

    // объединяет заведомо непересекающиеся но не классовые свойства

    protected LP addXSUProp(AbstractGroup group, String sID, String caption, LP... props) {
        return addXSUProp(group, sID, false, caption, props);
    }

    protected LP addXSUProp(AbstractGroup group, String sID, boolean persistent, String caption, LP... props) {
        return addUProp(group, sID, persistent, caption, Union.EXCLUSIVE, getUParams(props, 0));
    }

    protected LP[] addMUProp(AbstractGroup group, String[] ids, String[] captions, int extra, LP... props) {
        int propNum = props.length / (1 + extra);
        LP[] maxProps = Arrays.copyOfRange(props, 0, propNum);

        LP[] result = new LP[extra + 1];
        int i = 0;
        do {
            result[i] = addUProp(group, ids[i], captions[i], Union.MAX, getUParams(maxProps, 0));
            if (i < extra) { // если не последняя
                for (int j = 0; j < propNum; j++)
                    maxProps[j] = addJProp(and1, BaseUtils.add(directLI(props[(i + 1) * propNum + j]), directLI( // само свойство
                            addJProp(equals2, BaseUtils.add(directLI(maxProps[j]), directLI(result[i])))))); // только те кто дает предыдущий максимум
            }
        } while (i++ < extra);
        return result;
    }

    protected void addConstraint(LP<?> lp, boolean checkChange) {
        lp.property.setConstraint(checkChange);
    }

    protected void setNotNull(LP property) {

        ValueClass[] values = property.getMapClasses();

        int size = values.length;
        boolean boolArray[] = new boolean[size];
        for (int i = 0; i < size - 1; i++) {
            boolArray[i] = false;
        }
        boolArray[size - 1] = true;
        Object params[] = new Object[3 * size + 1];
        for (int i = 0; i < size; i++) {
            params[2 * i] = is(values[i]);
            params[2 * i + 1] = i + 1;
        }

        params[2 * size] = property;
        for (int i = 1; i < size + 1; i++) {
            params[i + 2 * size] = i;
        }
        LP checkProp = addJProp("Не задано свойство \"" + property.property.caption + "\"", and(boolArray), params);
        addConstraint(checkProp, false);
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
        for (LP[] props : checkCUProps) {
            logger.debug("Checking class properties : " + props + "...");
            assert !intersect(props);
        }
        for (LP[] props : checkSUProps) {
            logger.debug("Checking union properties : " + props + "...");
            assert intersect(props);
        }
        return true;
    }

    public void fillData() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
    }

    /*    // случайным образом генерирует классы
     void randomClasses(Random randomizer) {
         int customClasses = randomizer.nextInt(20);//
         List<CustomClass> objClasses = new ArrayList<CustomClass>();
         objClasses.add(customClass);
         for(int i=0;i<customClasses;i++) {
             CustomClass customClass = new CustomClass(i+10000, "Случайный класс"+i);
             int parents = randomizer.nextInt(2) + 1;
             for(int j=0;j<parents;j++)
                 customClass.addParent(objClasses.get(randomizer.nextInt(objClasses.size())));
             objClasses.add(customClass);
         }
     }

     // случайным образом генерирует св-ва
     void randomProperties(Random randomizer) {

         List<CustomClass> classes = new ArrayList<CustomClass>();
         customClass.fillChilds(classes);

         List<Property> randProps = new ArrayList<Property>();
         List<Property> randObjProps = new ArrayList<Property>();
         List<Property> randIntegralProps = new ArrayList<Property>();

         CompareFormulaProperty dirihle = new CompareFormulaProperty(genSID(), tableFactory, Compare.LESS);
         randProps.add(dirihle);

         MultiplyFormulaProperty multiply = new MultiplyFormulaProperty(genSID(), tableFactory, RemoteClass.integer,2);
         randProps.add(multiply);

         int dataPropCount = randomizer.nextInt(15)+1;
         for(int i=0;i<dataPropCount;i++) {
             int intCount = randomizer.nextInt(tableFactory.MAX_INTERFACE)+1;
             CustomClass[] interfaceClasses = new CustomClass[intCount];
             for(int j=0;j<intCount;j++)
                 interfaceClasses[j] = classes.get(randomizer.nextInt(classes.size()));

             // DataProperty
             DataProperty dataProp = new DataProperty("prop"+i,interfaceClasses,tableFactory,(i%4==0? RemoteClass.integer :classes.get(randomizer.nextInt(classes.size()))));
             dataProp.title = "Data Property " + i;
             // генерируем классы

             randProps.add(dataProp);
             randObjProps.add(dataProp);
             if(dataProp.getBaseClass().getCommonClass() instanceof IntegralClass)
                 randIntegralProps.add(dataProp);
         }

         System.out.print("Создание аггрег. св-в ");

         int propCount = randomizer.nextInt(1000)+1; //
         for(int i=0;i<propCount;i++) {
 //            int RandClass = Randomizer.nextInt(10);
 //            int PropClass = (RandClass>7?0:(RandClass==8?1:2));
             int propClass = randomizer.nextInt(6);
 //            int PropClass = 5;
             Property genProp = null;
             String resType = "";
             if(propClass ==0) {
                 // генерируем случайно кол-во интерфейсов

                 int intCount = randomizer.nextInt(tableFactory.MAX_INTERFACE)+1;
                 // JoinProperty
                 JoinProperty relProp = new JoinProperty(genSID(),intCount,tableFactory,randProps.get(randomizer.nextInt(randProps.size())));
                 List<PropertyInterface> relPropInt = new ArrayList<PropertyInterface>(relProp.interfaces);

                 // чтобы 2 раза на одну и ту же ветку не натыкаться
                 List<PropertyInterface> availRelInt = new ArrayList(relPropInt);
                 boolean correct = true;

                 for(PropertyInterface propertyInterface : (Collection<PropertyInterface>)relProp.implement.property.interfaces) {
                     // генерируем случайно map'ы на эти интерфейсы
                     if(!(relProp.implement.property instanceof FormulaProperty) && randomizer.nextBoolean()) {
                         if(availRelInt.size()==0) {
                             correct = false;
                             break;
                         }
                         PropertyInterface mapInterface = availRelInt.get(randomizer.nextInt(availRelInt.size()));
                         relProp.implement.mapping.put(propertyInterface,mapInterface);
                         availRelInt.remove(mapInterface);
                     } else {
                         // другое property пока сгенерим на 1
                         PropertyMapImplement impProp = new PropertyMapImplement(randObjProps.get(randomizer.nextInt(randObjProps.size())));
                         if(impProp.property.interfaces.size()>relPropInt.size()) {
                             correct = false;
                             break;
                         }

                         List<PropertyInterface> mapRelInt = new ArrayList(relPropInt);
                         for(PropertyInterface impInterface : (Collection<PropertyInterface>)impProp.property.interfaces) {
                             PropertyInterface mapInterface = mapRelInt.get(randomizer.nextInt(mapRelInt.size()));
                             impProp.mapping.put(impInterface,mapInterface);
                             mapRelInt.remove(mapInterface);
                         }
                         relProp.implement.mapping.put(propertyInterface,impProp);
                     }
                 }

                 if(correct) {
                     genProp = relProp;
                     resType = "R";
                 }
             }

             if(propClass ==1 || propClass ==2) {
                 // группировочное
                 Property groupProp;
                 if(propClass ==1)
                     groupProp = randIntegralProps.get(randomizer.nextInt(randIntegralProps.size()));
                 else
                     groupProp = randObjProps.get(randomizer.nextInt(randObjProps.size()));

                 Collection<Function> interfaces = new ArrayList<Function>();

                 boolean correct = true;
                 List<PropertyInterface> groupInt = new ArrayList(groupProp.interfaces);
                 int groupCount = randomizer.nextInt(tableFactory.MAX_INTERFACE)+1;
                 for(int j=0;j<groupCount;j++) {
                     PropertyInterfaceImplement implement;
                     // генерируем случайно map'ы на эти интерфейсы
                     if(randomizer.nextBoolean())
                         implement = groupInt.get(randomizer.nextInt(groupInt.size()));
                     else {
                         // другое property пока сгенерим на 1
                         PropertyMapImplement impProp = new PropertyMapImplement(randObjProps.get(randomizer.nextInt(randObjProps.size())));
                         if(impProp.property.interfaces.size()>groupInt.size()) {
                             correct = false;
                             break;
                         }

                         List<PropertyInterface> mapRelInt = new ArrayList(groupInt);
                         for(PropertyInterface impInterface : (Collection<PropertyInterface>)impProp.property.interfaces) {
                             PropertyInterface mapInterface = mapRelInt.get(randomizer.nextInt(mapRelInt.size()));
                             impProp.mapping.put(impInterface,mapInterface);
                             mapRelInt.remove(mapInterface);
                         }
                         implement = impProp;
                     }

                     interfaces.add(new Function(j,implement));
                 }

                 if(correct) {
                     if(propClass ==1) {
                         genProp = new SumGroupProperty(genSID(),interfaces,tableFactory,groupProp);
                         resType = "SG";
                     } else {
                         genProp = new MaxGroupProperty(genSID(),interfaces,tableFactory,groupProp);
                         resType = "MG";
                     }
                 }
             }

             if(propClass ==3 || propClass ==4 || propClass ==5) {
                 UnionProperty property = null;
                 List<Property> randValProps = randObjProps;
                 int opIntCount = randomizer.nextInt(tableFactory.MAX_INTERFACE)+1;
                 if(propClass ==3) {
                     randValProps = randIntegralProps;
                     property = new SumUnionProperty(genSID(),opIntCount,tableFactory);
                     resType = "SL";
                 } else {
                 if(propClass ==4) {
                     property = new MaxUnionProperty(genSID(),opIntCount,tableFactory);
                     resType = "ML";
                 } else {
                     property = new OverrideUnionProperty(genSID(),opIntCount,tableFactory);
                     resType = "OL";
                 }
                 }

                 boolean correct = true;
                 List<PropertyInterface> opInt = new ArrayList(property.interfaces);
                 int opCount = randomizer.nextInt(4)+1;
                 for(int j=0;j<opCount;j++) {
                     PropertyMapImplement operand = new PropertyMapImplement(randValProps.get(randomizer.nextInt(randValProps.size())));
                     if(operand.property.interfaces.size()!=opInt.size()) {
                         correct = false;
                         break;
                     }

                     List<PropertyInterface> mapRelInt = new ArrayList(opInt);
                     for(PropertyInterface impInterface : (Collection<PropertyInterface>)operand.property.interfaces) {
                         PropertyInterface mapInterface = mapRelInt.get(randomizer.nextInt(mapRelInt.size()));
                         operand.mapping.put(impInterface,mapInterface);
                         mapRelInt.remove(mapInterface);
                     }
                     property.operands.put(operand,1);
                 }

                 if(correct)
                     genProp = property;
             }


             if(genProp!=null && genProp.isInInterface(new HashMap()) && genProp.hasAllKeys()) {
                 genProp.title = resType + " " + i;
                 // проверим что есть в интерфейсе и покрыты все ключи
                 System.out.print(resType+"-");
                 randProps.add(genProp);
                 randObjProps.add(genProp);
                 if(genProp.getBaseClass().getCommonClass() instanceof IntegralClass)
                     randIntegralProps.add(genProp);
             }
         }

         property.addAll(randProps);

         System.out.println();
     }
    */
    /*
    // случайным образом генерирует имплементацию
    void randomImplement(Random randomizer) {
        List<CustomClass> classes = new ArrayList<CustomClass>(baseClass.getChilds());

        // заполнение физ модели
        int implementCount = randomizer.nextInt(8);
        for(int i=0;i<implementCount;i++) {
            int objCount = randomizer.nextInt(3)+1;
            CustomClass[] randomClasses = new CustomClass[objCount];
            for(int ioc=0;ioc<objCount;ioc++)
                randomClasses[ioc] = classes.get(randomizer.nextInt(classes.size()));
            tableFactory.include(randomClasses);
        }
    }

    // случайным образом генерирует постоянные аггрегации
    void randomPersistent(Random Randomizer) {

        persistents.clear();

        // сначала список получим
        List<AggregateProperty> aggrProperties = new ArrayList<AggregateProperty>();
        for(Property property : properties) {
            if(property instanceof AggregateProperty && property.isObject())
                aggrProperties.add((AggregateProperty)property);
        }

        int persistentNum = Randomizer.nextInt(aggrProperties.size())+1;
        for(int i=0;i<persistentNum;i++)
            persistents.add(aggrProperties.get(Randomizer.nextInt(aggrProperties.size())));

//        for(AggregateProperty Property : AggrProperties)
//            if(Property.title.equals("R 1"))
//            Persistents.add(Property);
     }

    static int changeDBIteration = 0;
    void changeDBTest(Integer maxIterations,Random randomizer) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {

        // сначала список получим
        List<StoredDataProperty> dataProperties = new ArrayList<StoredDataProperty>();
        for(Property property : properties)
            if(property instanceof StoredDataProperty)
                dataProperties.add((StoredDataProperty)property);

        DataSession session = createSession(adapter);

        List<ConcreteCustomClass> addClasses = new ArrayList<ConcreteCustomClass>();
        baseClass.fillConcreteChilds(addClasses);
        for(ConcreteCustomClass addClass : addClasses) {
            int objectAdd = randomizer.nextInt(10)+1;
            for(int ia=0;ia<objectAdd;ia++)
                session.addObject(addClass,null);
        }

        session.apply(this);

        long prevTime = System.currentTimeMillis();

//        Randomizer.setSeed(1);
        int iterations = 1;
        while(iterations<maxIterations) {

            long currentTime = System.currentTimeMillis();
            if(currentTime-prevTime>=40000)
                break;

            prevTime = currentTime;

            changeDBIteration = iterations;
            System.out.println("Iteration" + iterations++);

            // будем также рандомно создавать объекты
            addClasses = new ArrayList<ConcreteCustomClass>();
            baseClass.fillConcreteChilds(addClasses);
            int objectAdd = randomizer.nextInt(5);
            for(int ia=0;ia<objectAdd;ia++)
                session.addObject(addClasses.get(randomizer.nextInt(addClasses.size())),null);

            int propertiesChanged = randomizer.nextInt(8)+1;
            for(int ip=0;ip<propertiesChanged;ip++) {
                // берем случайные n св-в
                StoredDataProperty changeProp = dataProperties.get(randomizer.nextInt(dataProperties.size()));
                int numChanges = randomizer.nextInt(3)+1;
                for(int in=0;in<numChanges;in++) {

                    // генерим рандомные объекты этих классов
                    Map<ClassPropertyInterface, DataObject> keys = new HashMap<ClassPropertyInterface, DataObject>();
                    for(ClassPropertyInterface propertyInterface : changeProp.interfaces)
                        keys.put(propertyInterface,propertyInterface.interfaceClass.getRandomObject(session, randomizer));

                    ObjectValue valueObject;
                    if(randomizer.nextInt(10)<8)
                        valueObject = changeProp.value.getRandomObject(session, randomizer);
                    else
                        valueObject = NullValue.instance;

                    session.changeProperty(changeProp, keys, valueObject);
                }
            }

            session.apply(this);
            checkPersistent(session);
        }

        session.close();
    }

    // полностью очищает базу
    protected void clean(SQLSession session) throws SQLException {
        // удаляем все объекты
        session.deleteKeyRecords(baseClass.table, new HashMap<KeyField, Integer>());

        // удаляем все св-ва
        for(ImplementTable table : tableFactory.getImplementTables().values())
            session.deleteKeyRecords(table, new HashMap<KeyField, Object>());
    }

    public static boolean autoFillDB = false;
    public static int autoIDCounter = 0;
    static int autoSeed = 1400;
    public void autoFillDB(Map<ConcreteCustomClass, Integer> classQuantity, Map<StoredDataProperty, Integer> propQuantity, Map<StoredDataProperty, Set<ClassPropertyInterface>> propNotNull) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {

        System.out.print("Идет заполнение базы данных...");

        autoFillDB = true;
        DataSession session = createSession(adapter);

        clean(session);

        // сначала вырубим все аггрегации в конце пересчитаем
        Map<AggregateProperty, PropertyField> savePersistents = new HashMap<AggregateProperty, PropertyField>();
        for(AggregateProperty property : persistents) {
            savePersistents.put(property,property.field);
            property.field = null;
        }
        persistents.clear();

        // берем все конкретные классы
        Map<DataObject,String> objectNames = new HashMap<DataObject, String>();
        List<ConcreteCustomClass> concreteClasses = new ArrayList<ConcreteCustomClass>();
        baseClass.fillConcreteChilds(concreteClasses);

        // генерируем списки ИД объектов по классам
        List<CustomClass> classes = new ArrayList<CustomClass>(baseClass.getChilds());
        Map<CustomClass,List<DataObject>> objects = new HashMap<CustomClass, List<DataObject>>();
        for(CustomClass fillClass : classes)
            objects.put(fillClass,new ArrayList<DataObject>());

        for(ConcreteCustomClass fillClass : concreteClasses)
            if(fillClass.children.size()==0) {
                System.out.println("Класс : "+fillClass.title);

                Integer quantity = classQuantity.get(fillClass);
                if(quantity==null) quantity = 1;

                List<DataObject> listObjects = new ArrayList<DataObject>();
                for(int i=0;i<quantity;i++) {
                    DataObject idObject = session.addObject(fillClass,null);
                    listObjects.add(idObject);
                    objectNames.put(idObject,fillClass.title+" "+(i+1));
                }

                Set<CustomClass> parents = new HashSet<CustomClass>();
                fillClass.fillParents(parents);

                for(CustomClass customClass : parents)
                    objects.get(customClass).addAll(listObjects);
            }

        Random randomizer = new Random(autoSeed);

        // бежим по св-вам
        for(Property abstractProperty : properties)
            if(abstractProperty instanceof StoredDataProperty) {
                StoredDataProperty property = (StoredDataProperty)abstractProperty;

                System.out.println("Свойство : "+property.title);

                Set<ClassPropertyInterface> interfaceNotNull = propNotNull.get(property);
                if(interfaceNotNull==null) interfaceNotNull = new HashSet<ClassPropertyInterface>();
                Integer quantity = propQuantity.get(property);
                if(quantity==null) {
                    quantity = 1;
                    for(ClassPropertyInterface propertyInterface : property.interfaces)
                        if(!interfaceNotNull.contains(propertyInterface))
                            quantity = quantity * propertyInterface.interfaceClass.getRandomList(objects).size();

                    if(quantity > 1)
                        quantity = (int)(quantity * 0.5);
                }

                Map<ClassPropertyInterface,List<DataObject>> mapInterfaces = new HashMap<ClassPropertyInterface, List<DataObject>>();
                if(propNotNull.containsKey(property))
                    for(ClassPropertyInterface propertyInterface : interfaceNotNull)
                        mapInterfaces.put(propertyInterface,propertyInterface.interfaceClass.getRandomList(objects));

                // сначала для всех PropNotNull генерируем все возможные Map<ы>
                for(Map<ClassPropertyInterface,DataObject> notNulls : new Combinations<ClassPropertyInterface,DataObject>(mapInterfaces)) { //
                    int randomInterfaces = 0;
                    while(randomInterfaces<quantity) {
                        Map<ClassPropertyInterface,DataObject> randomIteration = new HashMap<ClassPropertyInterface,DataObject>(notNulls);
                        for(ClassPropertyInterface propertyInterface : property.interfaces)
                            if(!notNulls.containsKey(propertyInterface))
                                randomIteration.put(propertyInterface,BaseUtils.getRandom(propertyInterface.interfaceClass.getRandomList(objects),randomizer));

                        DataObject valueObject = null;
                        if(property.value instanceof StringClass) {
                            String objectName = "";
                            for(ClassPropertyInterface propertyInterface : property.interfaces)
                                objectName += objectNames.get(randomIteration.get(propertyInterface)) + " ";
                            valueObject = new DataObject(objectName,StringClass.get(50));
                        } else
                            valueObject = BaseUtils.getRandom(property.value.getRandomList(objects),randomizer);
                        session.changeProperty(property,randomIteration,valueObject);
                        randomInterfaces++;
                    }
                }
            }

        System.out.println("Применение изменений...");
        session.apply(this);

        session.startTransaction();

        for(Map.Entry<AggregateProperty,PropertyField> save : savePersistents.entrySet()) {
            save.getKey().field = save.getValue();
            persistents.add(save.getKey());
        }

        recalculateAggregations(session,savePersistents.keySet());

        session.commitTransaction();

        IDTable.instance.reserveID(session, IDTable.OBJECT, autoIDCounter);

        session.close();

        System.out.println("База данных была успешно заполнена");

        autoFillDB = false;
    }
*/

    private void recalculateAggregations(SQLSession session, Collection<AggregateProperty> recalculateProperties) throws SQLException {

        for (Property property : getPropertyList())
            if (property instanceof AggregateProperty) {
                AggregateProperty dependProperty = (AggregateProperty) property;
                if (recalculateProperties.contains(dependProperty)) {
                    logger.debug("Идет перерасчет аггрегированного св-ва (" + dependProperty + ")... ");
                    dependProperty.recalculateAggregation(session);
                    logger.debug("Done");
                }
            }
    }

    public <T extends FormEntity> T addFormEntity(T form) {
        form.richDesign = form.createDefaultRichDesign();
        return form;
    }

    public void addObjectActions(FormEntity form, ObjectEntity object) {
        addObjectActions(form, object, false);
    }

    public void addObjectActions(FormEntity form, ObjectEntity object, ObjectEntity checkObject) {
        addObjectActions(form, object, checkObject, null);
    }

    public void addObjectActions(FormEntity form, ObjectEntity object, ObjectEntity checkObject, ValueClass checkObjectClass) {
        addObjectActions(form, object, false, checkObject, checkObjectClass);
    }

    public void addObjectActions(FormEntity form, ObjectEntity object, boolean actionImport) {
        addObjectActions(form, object, actionImport, null, null);
    }

    public void addObjectActions(FormEntity form, ObjectEntity object, boolean actionImport, ObjectEntity checkObject, ValueClass checkObjectClass) {
        form.addPropertyDraw(delete, object);
        if (actionImport)
            form.forceDefaultDraw.put(form.addPropertyDraw(getImportObjectAction(object.baseClass)), object.groupTo);

        PropertyDrawEntity actionAddPropertyDraw;
        if (checkObject == null) {
            actionAddPropertyDraw = form.addPropertyDraw(getAddObjectAction(object.baseClass));
        } else {
            actionAddPropertyDraw = form.addPropertyDraw(
                    getAddObjectActionWithClassCheck(object.baseClass, checkObjectClass != null ? checkObjectClass : checkObject.baseClass),
                    checkObject);

            actionAddPropertyDraw.shouldBeLast = true;
            actionAddPropertyDraw.forceViewType = ClassViewType.PANEL;
        }
        form.forceDefaultDraw.put(actionAddPropertyDraw, object.groupTo);
    }

    protected Scheduler scheduler;

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
        this.scheduler.start();
    }

    protected LP addSAProp(LP lp) {
        return addSAProp(privateGroup, "sys", lp);
    }

    protected LP addSAProp(AbstractGroup group, String caption, LP lp) {
        return addProperty(group, new LP<ClassPropertyInterface>(new SeekActionProperty(genSID(), caption, new ValueClass[]{baseClass}, lp == null ? null : lp.property)));
    }

    protected LP addEPAProp(LP lp) {
        return addAProp(new ExecutePropertyActionProperty(genSID(), "sys", lp));
    }

    protected LP addLFAProp(AbstractGroup group, String caption, LP lp) {
        return addProperty(group, new LP<ClassPropertyInterface>(new LoadActionProperty(genSID(), caption, lp)));
    }

    protected LP addOFAProp(AbstractGroup group, String caption, LP lp) { // обернем сразу в and
        LP<ClassPropertyInterface> openAction = new LP<ClassPropertyInterface>(new OpenActionProperty(genSID(), caption, lp));
        return addJProp(group, caption, and1, getUParams(new LP[]{openAction, lp}, 0));
    }

    public static class LoadActionProperty extends ActionProperty {

        LP fileProperty;

        private LoadActionProperty(String sID, String caption, LP fileProperty) {
            super(sID, caption, fileProperty.getMapClasses());

            this.fileProperty = fileProperty;
        }

        @Override
        protected DataClass getValueClass() {
            FileClass fileClass = (FileClass) fileProperty.property.getType();
            return FileActionClass.getInstance(fileClass.toString(), fileClass.getExtensions());
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            FormInstance<?> form = executeForm.form;
            DataObject[] objects = new DataObject[keys.size()];
            int i = 0; // здесь опять учитываем, что порядок тот же
            for (ClassPropertyInterface classInterface : interfaces)
                objects[i++] = keys.get(classInterface);
            fileProperty.execute(value.getValue(), form.session, form, objects);
        }

        @Override
        public void proceedDefaultDraw(PropertyDrawEntity<ClassPropertyInterface> entity, FormEntity form) {
            super.proceedDefaultDraw(entity, form);
            entity.shouldBeLast = true;
            entity.forceViewType = ClassViewType.PANEL;
        }
    }

    public static class OpenActionProperty extends ActionProperty {

        LP fileProperty;

        private OpenActionProperty(String sID, String caption, LP fileProperty) {
            super(sID, caption, fileProperty.getMapClasses());

            this.fileProperty = fileProperty;
        }

        private FileClass getFileClass() {
            return (FileClass) fileProperty.property.getType();
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            FormInstance<?> form = executeForm.form;
            DataObject[] objects = new DataObject[keys.size()];
            int i = 0; // здесь опять учитываем, что порядок тот же
            for (ClassPropertyInterface classInterface : interfaces)
                objects[i++] = keys.get(classInterface);
            actions.add(new OpenFileClientAction((byte[]) fileProperty.read(form.session.sql, form, form.session.env, objects), BaseUtils.firstWord(getFileClass().getExtensions(), ",")));
        }
    }

    // params - по каким входам группировать
    protected LP addIAProp(LP dataProperty, Integer... params) {
        return addAProp(new IncrementActionProperty(genSID(), "sys", dataProperty,
                addMGProp(dataProperty, params),
                params));
    }

    public static class IncrementActionProperty extends ActionProperty {

        LP dataProperty;
        LP maxProperty;
        List<Integer> params;

        private IncrementActionProperty(String sID, String caption, LP dataProperty, LP maxProperty, Integer[] params) {
            super(sID, caption, dataProperty.getMapClasses());

            this.dataProperty = dataProperty;
            this.maxProperty = maxProperty;
            this.params = Arrays.asList(params);
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            FormInstance<?> form = executeForm.form;
            DataSession session = form.session;

            // здесь опять учитываем, что порядок тот же
            int i = 0;
            DataObject[] dataPropertyInput = new DataObject[keys.size()];
            List<DataObject> maxPropertyInput = new ArrayList<DataObject>();

            for (ClassPropertyInterface classInterface : interfaces) {
                dataPropertyInput[i] = keys.get(classInterface);
                if (params.contains(i + 1)) {
                    maxPropertyInput.add(dataPropertyInput[i]);
                }
                i++;
            }

            Integer maxValue = (Integer) maxProperty.read(session, maxPropertyInput.toArray(new DataObject[0]));
            ;
            if (maxValue == null)
                maxValue = 0;
            maxValue += 1;

            dataProperty.execute(maxValue, session, form, dataPropertyInput);
        }
    }

    protected LP addMAProp(String message, String caption) {
        return addMAProp(message, null, caption);
    }

    protected LP addMAProp(String message, AbstractGroup group, String caption) {
        return addProperty(group, new LP<ClassPropertyInterface>(new MessageActionProperty(message, genSID(), caption)));
    }

    protected LP addRestartActionProp() {
        return addProperty(null, new LP<ClassPropertyInterface>(new RestartActionProperty(genSID(), "")));
    }

    protected LP addCancelRestartActionProp() {
        return addProperty(null, new LP<ClassPropertyInterface>(new CancelRestartActionProperty(genSID(), "")));
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
        updateEnvironmentProperty(isServerRestarting.property, ObjectValue.getValue(isRestarting, LogicalClass.instance));
    }

    public static class SeekActionProperty extends ActionProperty {

        Property property;

        private SeekActionProperty(String sID, String caption, ValueClass[] classes, Property property) {
            super(sID, caption, classes);
            this.property = property;
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            FormInstance<?> form = executeForm.form;
            Collection<ObjectInstance> objects;
            if (property != null)
                objects = form.instanceFactory.getInstance(form.entity.getPropertyObject(property)).mapping.values();
            else
                objects = form.getObjects();
            for (Map.Entry<ClassPropertyInterface, DataObject> key : keys.entrySet()) {
                if (mapObjects.get(key.getKey()) == null) {
                    for (ObjectInstance object : objects) {
                        ConcreteClass keyClass = form.session.getCurrentClass(key.getValue());
                        if (keyClass instanceof ConcreteValueClass && object.getBaseClass().isCompatibleParent((ValueClass) keyClass)) {
                            form.seekObject(object, key.getValue());
                        }
                    }
                }
            }
        }
    }

    public static class MessageActionProperty extends ActionProperty {
        private String message;

        private MessageActionProperty(String message, String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
            this.message = message;
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            actions.add(new MessageClientAction(message, caption));
        }
    }

    public class RestartActionProperty extends ActionProperty {
        private RestartActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            getRestartController().initRestart();
            updateRestartProperty();
        }
    }

    public class CancelRestartActionProperty extends ActionProperty {
        private CancelRestartActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            getRestartController().cancelRestart();
            updateRestartProperty();
        }
    }

    @IdentityLazy
    private synchronized RestartController getRestartController() {
        return new RestartController(this);
    }

    protected LP addAProp(ActionProperty property) {
        return addAProp(actionGroup, property);
    }

    protected LP addAProp(AbstractGroup group, ActionProperty property) {
        return addProperty(group, new LP<ClassPropertyInterface>(property));
    }

    protected LP addBAProp(ConcreteCustomClass customClass, LP add) {
        return addAProp(new AddBarcodeActionProperty(customClass, add.property, genSID()));
    }

    private class AddBarcodeActionProperty extends ActionProperty {

        ConcreteCustomClass customClass;
        Property<?> addProperty;

        private AddBarcodeActionProperty(ConcreteCustomClass customClass, Property addProperty, String sID) {
            super(sID, "Добавить [" + customClass + "] по бар-коду", new ValueClass[]{StringClass.get(13)});

            this.customClass = customClass;
            this.addProperty = addProperty;
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            FormInstance<?> remoteForm = executeForm.form;
            DataSession session = remoteForm.session;
            if (addProperty.read(session.sql, new HashMap(), remoteForm, session.env) != null) {
                String barString = (String) BaseUtils.singleValue(keys).object;
                if (barString.trim().length() != 0) {
                    addProperty.execute(new HashMap(), session, null, remoteForm);
                    barcode.execute(barString, session, remoteForm, session.addObject(customClass, remoteForm));
                }
            }
        }
    }

    protected LP addAAProp(CustomClass customClass, LP... properties) {
        return addAAProp(customClass, null, null, false, properties);
    }

    protected LP addAAProp(CustomClass customClass, LP barcode, LP barcodePrefix, boolean quantity, LP... properties) {
        return addAProp(new AddObjectActionProperty(genSID(),
                (barcode != null) ? barcode.property : null, (barcodePrefix != null) ? barcodePrefix.property : null,
                quantity, customClass, LP.toPropertyArray(properties)));
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

    public byte[] getLogo() throws RemoteException {
        return null;
    }

    public void outputPropertyClasses() {
        for (LP lp : lproperties) {
            logger.debug(lp.property.sID + " : " + lp.property.caption + " - " + lp.getClassWhere());
        }
    }

    public void outputPersistent() {
        String result = "";

        result += '\n' + "ПО ТАБЛИЦАМ :" + '\n' + '\n';
        for (Map.Entry<ImplementTable, Collection<Property>> groupTable : BaseUtils.group(new BaseUtils.Group<ImplementTable, Property>() {
            public ImplementTable group(Property key) {
                return key.mapTable.table;
            }
        }, getStoredProperties()).entrySet()) {
            result += groupTable.getKey().outputKeys() + '\n';
            for (Property property : groupTable.getValue())
                result += '\t' + property.outputStored(false) + '\n';
        }
        result += '\n' + "ПО СВОЙСТВАМ :" + '\n' + '\n';
        for (Property property : getStoredProperties())
            result += property.outputStored(true) + '\n';
        System.out.println(result);
    }

    public static int generateStaticNewID() {
        return idGenerator.idShift();
    }

    public int generateNewID() throws RemoteException {
        return generateStaticNewID();
    }

    public byte[] getBaseClassByteArray() throws RemoteException {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(outStream);
            baseClass.serialize(dataStream);
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

        for (PropertyClassImplement implement : rootGroup.getProperties(classLists, isAny)) {
            result.add(implement.property);
            ArrayList<Integer> ids = new ArrayList<Integer>();
            for (Object iface : implement.property.interfaces) {
                ids.add(classes.get(implement.mapping.get(iface)));
            }
            idResult.add(ids);
        }
    }
    /*
    private Map<String, String> getPropertiesNames(String[] properties) {

        Map<String, String> result = new HashMap<String, String>();
        for (String prop : properties) {
            result.put(prop, prop);
        }

        Class cls = this.getClass();
        for (java.lang.reflect.Field field :  cls.getDeclaredFields()) {



        }


    }
    */

    public RemoteFormInterface createForm(DataSession session, FormEntity formEntity, Map<ObjectEntity, DataObject> mapObjects) {
        try {
            FormInstance<T> formInstance = new FormInstance<T>(formEntity, (T) this, session, PolicyManager.serverSecurityPolicy, null, null, new DataObject(getServerComputer(), computer), mapObjects);
            return new RemoteForm<T, FormInstance<T>>(formInstance, formEntity.getRichDesign(), exportPort, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void close() throws SQLException {
        sql.close();
    }
}
