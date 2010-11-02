package platform.server.logics;

import net.sf.jasperreports.engine.JRException;
import org.apache.commons.collections.MultiHashMap;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.FileSystemResource;
import platform.base.*;
import platform.interop.Compare;
import platform.interop.RemoteLogicsInterface;
import platform.interop.action.ClientAction;
import platform.interop.action.MessageClientAction;
import platform.interop.action.UserChangedClientAction;
import platform.interop.action.UserReloginClientAction;
import platform.interop.exceptions.LoginException;
import platform.interop.form.screen.ExternalScreen;
import platform.interop.form.screen.ExternalScreenParameters;
import platform.interop.navigator.RemoteNavigatorInterface;
import platform.interop.remote.RemoteObject;
import platform.server.auth.PolicyManager;
import platform.server.auth.SecurityPolicy;
import platform.server.auth.User;
import platform.server.caches.IdentityLazy;
import platform.server.classes.*;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.*;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.sql.DataAdapter;
import platform.server.data.sql.PostgreDataAdapter;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.Type;
import platform.server.data.type.TypeSerializer;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.OrderInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.navigator.RemoteNavigator;
import platform.server.form.navigator.UserController;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.AddObjectActionProperty;
import platform.server.logics.property.actions.DeleteObjectActionProperty;
import platform.server.logics.property.actions.ImportFromExcelActionProperty;
import platform.server.logics.property.derived.*;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.property.group.PropertySet;
import platform.server.logics.scheduler.Scheduler;
import platform.server.logics.table.ImplementTable;
import platform.server.logics.table.TableFactory;
import platform.server.net.ServerInstanceLocator;
import platform.server.net.ServerInstanceLocatorSettings;
import platform.server.net.ServerSocketFactory;
import platform.server.serialization.ServerSerializationPool;
import platform.server.session.DataSession;

import java.io.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIFailureHandler;
import java.rmi.server.RMISocketFactory;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.*;

// @GenericImmutable нельзя так как Spring валится
public abstract class BusinessLogics<T extends BusinessLogics<T>> extends RemoteObject implements RemoteLogicsInterface {
    private final static Logger logger = Logger.getLogger(BusinessLogics.class.getName());

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
        System.setProperty("sun.rmi.dgc.server.gcInterval", "60000");

        String logLevel = System.getProperty("platform.server.loglevel");
        if (logLevel != null) {
            LogManager.getLogManager().getLogger("").setLevel(Level.parse(logLevel));
        } else {
            LogManager.getLogManager().getLogger("").setLevel(Level.SEVERE);
        }

        initRMISocketFactory();

        Handler consoleHandler = new StreamHandler(System.out, new SimpleFormatter()) {
            @Override
            public synchronized void publish(LogRecord record) {
                try {
                    String message = getFormatter().format(record);
                    System.out.write(message.getBytes());
                } catch (Exception exception) {
                    reportError(null, exception, ErrorManager.FORMAT_FAILURE);
                    return;
                }
            }
        };

        LogManager.getLogManager().getLogger("").getHandlers()[0].setLevel(Level.OFF);
        LogManager.getLogManager().getLogger("").addHandler(consoleHandler);

        stopped = false;

        logger.severe("Server is starting...");

        XmlBeanFactory factory = new XmlBeanFactory(new FileSystemResource("conf/settings.xml"));

        BL = (BusinessLogics) factory.getBean("businessLogics");
        registry = LocateRegistry.createRegistry(BL.getExportPort());

        registry.rebind("BusinessLogics", BL);

        logger.severe("Server has successfully started");

        if (factory.containsBean("serverInstanceLocatorSettings")) {
            ServerInstanceLocatorSettings settings = (ServerInstanceLocatorSettings) factory.getBean("serverInstanceLocatorSettings");
            new ServerInstanceLocator().start(settings, BL.getExportPort());

            logger.severe("Server instance locator successfully started");
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

        registry.unbind("BusinessLogics");

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

    public SQLSyntax getAdapter() {
        return adapter;
    }

    public final static boolean activateCaches = true;

    public RemoteNavigatorInterface createNavigator(String login, String password, int computer) {

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

            return new RemoteNavigator(this, user, computer, exportPort);
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

    // счетчик идентификаторов
    IDGenerator idGenerator = new DefaultIDGenerator();

    int idShift(int offs) {
        return idGenerator.idShift(offs);
    }

    protected AbstractCustomClass transaction, barcodeObject;

    public AbstractCustomClass user;
    public ConcreteCustomClass systemUser;
    public ConcreteCustomClass customUser;
    public ConcreteCustomClass computer;
    public ConcreteCustomClass policy;
    public ConcreteCustomClass session;

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

            Set<Map<String, Object>> keys = q.execute(session).keySet();
            if (keys.size() == 0) {
                DataObject addObject = session.addObject(computer, session.modifier);
                hostname.execute(strHostName, session, session.modifier, addObject);

                result = (Integer) addObject.object;
                session.apply(this);
            } else {
                result = (Integer) keys.iterator().next().get("key");
            }

            session.close();
            logger.warning("Begin user session " + strHostName + " " + result);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ConcreteClass getDataClass(Object object, Type type) {
        try {
            DataSession session = createSession();
            ConcreteClass result = type.getDataClass(object, session, baseClass);
            session.close();

            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void endSession(String clientInfo) {
        logger.warning("End user session " + clientInfo);
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

        User userObject = new User(userId);
        policyManager.putUser(userId, userObject);

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
            Expr orderExpr = userPolicyOrder.getExpr(session.modifier, q.mapKeys.get("userId"), q.mapKeys.get("policyId"));

            q.properties.put("pOrder", orderExpr);
            q.and(orderExpr.getWhere());
            q.and(q.mapKeys.get("userId").compare(new DataObject(userId, customUser), Compare.EQUALS));

            OrderedMap<Object, Boolean> orderBy = new OrderedMap(BaseUtils.toList("pOrder"), false);
            Set<Map<String, Object>> keys = q.execute(session, orderBy, 0).keySet();
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
    protected LP divideDouble;
    protected LP string2;
    protected LP concat2;

    protected LP vtrue, vzero;

    public LP<?> name;
    public LP<?> date;

    protected LP transactionLater;
    protected LP currentDate;
    protected LP currentHour;
    protected LP currentEpoch;
    public LP currentUser;
    public LP currentSession;
    protected LP changeUser;
    public LP<PropertyInterface> barcode;
    public LP barcodeToObject;
    protected LP barcodeObjectName;
    public LP reverseBarcode;

    public LP userLogin;
    public LP userPassword;
    public LP userFirstName;
    public LP userLastName;
    public LP currentUserName;
    public LP<?> loginToUser;

    public LP policyDescription;
    protected LP<?> nameToPolicy;
    public LP userPolicyOrder;

    public LP hostname;
    public LP onlyNotZero;

    public LP delete;

    public LP objectClass;
    public LP objectClassName;
    public LP classSID;

    public LP customID;
    public LP stringID;
    public LP integerID;
    public LP dateID;

    public static int genSystemClassID(int id) {
        return 9999976 - id;
    }

    @IdentityLazy
    public SelectionProperty getSelectionProperty(ValueClass[] typeClasses) {
        SelectionProperty property = new SelectionProperty(genSID(), typeClasses);
        addProperty(null, new LP<ClassPropertyInterface>(property));
        return property;
    }

    public Property getProperty(int id) {
        for (Property property : properties) {
            if (property.getID() == id) {
                return property;
            }
        }
        return null;
    }

    public class SelectionPropertySet extends PropertySet {

        protected Class<?> getPropertyClass() {
            return SelectionProperty.class;
        }

        public boolean hasChild(Property prop) {
            return prop instanceof SelectionProperty;
        }

        protected Property getProperty(ValueClass[] classes) {
            return getSelectionProperty(classes);
        }
    }

    protected SelectionPropertySet selection;

    @IdentityLazy
    public ObjectValueProperty getObjectValueProperty(ValueClass typeClass) {
        ObjectValueProperty property = new ObjectValueProperty(genSID(), typeClass);
        addProperty(null, new LP<ClassPropertyInterface>(property));
        return property;
    }

    public class ObjectValuePropertySet extends PropertySet {

        protected Class<?> getPropertyClass() {
            return ObjectValueProperty.class;
        }

        @Override
        protected boolean isInInterface(ValueClass[] classes) {
            return classes.length == 1;
        }

        protected Property getProperty(ValueClass[] classes) {
            return getObjectValueProperty(classes[0].getBaseClass());
        }
    }

    protected ObjectValuePropertySet objectValue;

    void initBase() {
        rootGroup= new AbstractGroup("Корневая группа");
        rootGroup.createContainer = false;

        publicGroup = new AbstractGroup("Пользовательские свойства");
        publicGroup.createContainer = false;
        rootGroup.add(publicGroup);

        baseGroup = new AbstractGroup("Атрибуты");
        baseGroup.createContainer = false;        
        privateGroup = new AbstractGroup("Внутренние свойства");
        privateGroup.createContainer = false;
        publicGroup.add(baseGroup);
        rootGroup.add(privateGroup);
        
        objectValue = new ObjectValuePropertySet();
        baseGroup.add(objectValue);

        selection = new SelectionPropertySet();

        aggrGroup = new AbstractGroup("Аггрегированные атрибуты");
        aggrGroup.createContainer = false;

        baseClass = new BaseClass("Объект");

        transaction = addAbstractClass("Транзакция", baseClass);
        barcodeObject = addAbstractClass("Штрих-кодированный объект", baseClass);

        user = addAbstractClass("Пользователь", baseClass);
        customUser = addConcreteClass(genSystemClassID(0), "Обычный пользователь", user, barcodeObject);
        systemUser = addConcreteClass(genSystemClassID(2), "Системный пользователь", user);
        computer = addConcreteClass(genSystemClassID(1), "Рабочее место", baseClass);

        policy = addConcreteClass(genSystemClassID(3), "Политика безопасности", baseClass.named);
        session = addConcreteClass(genSystemClassID(4), "Транзакция", baseClass);

        tableFactory = new TableFactory();
        for (int i = 0; i < TableFactory.MAX_INTERFACE; i++) { // заполним базовые таблицы
            CustomClass[] baseClasses = new CustomClass[i];
            for (int j = 0; j < i; j++)
                baseClasses[j] = baseClass;
            tableFactory.include("base_" + i, baseClasses);
        }

        baseElement = new NavigatorElement<T>(0, "Base Group");
    }

    void initBaseProperties() {
        // математические св-ва
        equals2 = addCFProp(Compare.EQUALS);
        object1 = addAFProp();
        and1 = addAFProp(false);
        andNot1 = addAFProp(true);
        string2 = addSProp(2);
        concat2 = addCCProp(2);
        groeq2 = addCFProp(Compare.GREATER_EQUALS);
        greater2 = addCFProp(Compare.GREATER);
        less2 = addCFProp(Compare.LESS);
        greater22 = addJProp(greater2, concat2, 1, 2, concat2, 3, 4);
        less22 = addJProp(less2, concat2, 1, 2, concat2, 3, 4);
        diff2 = addCFProp(Compare.NOT_EQUALS);
        divideDouble = addSFProp("((prm1)/(prm2))", DoubleClass.instance, 2);
        between = addJProp("Между", and1, groeq2, 1, 2, groeq2, 3, 1);
        vtrue = addCProp("Истина", LogicalClass.instance, true);
        vzero = addCProp("0", DoubleClass.instance, 0);

        delete = addProperty(null, new LP<ClassPropertyInterface>(new DeleteObjectActionProperty(genSID(), baseClass)));

        date = addDProp(baseGroup, "date", "Дата", DateClass.instance, transaction);

        transactionLater = addSUProp("Транзакция позже", Union.OVERRIDE, addJProp("Дата позже", greater2, date, 1, date, 2),
                addJProp("", and1, addJProp("Дата=дата", equals2, date, 1, date, 2), 1, 2, addJProp("Код транзакции после", greater2, 1, 2), 1, 2));

        hostname = addDProp(baseGroup, "hostname", "Имя хоста", StringClass.get(100), computer);

        currentDate = addDProp(baseGroup, "currentDate", "Тек. дата", DateClass.instance);
        currentHour = addTProp(Time.HOUR);
        currentEpoch = addTProp(Time.EPOCH);
        currentUser = addProperty(null, new LP<PropertyInterface>(new CurrentUserFormulaProperty(genSID(), user)));
        currentSession = addProperty(null, new LP<PropertyInterface>(new CurrentSessionFormulaProperty(genSID(), session)));
        changeUser = addProperty(null, new LP<ClassPropertyInterface>(new ChangeUserActionProperty(genSID(), customUser)));

        userLogin = addDProp(baseGroup, "userLogin", "Логин", StringClass.get(30), customUser);
        loginToUser = addCGProp(null, "loginToUser", "Пользователь", object(customUser), userLogin, userLogin, 1);
        userPassword = addDProp(baseGroup, "userPassword", "Пароль", StringClass.get(30), customUser);
        userFirstName = addDProp(baseGroup, "userFirstName", "Имя", StringClass.get(30), customUser);
        userLastName = addDProp(baseGroup, "userLastName", "Фамилия", StringClass.get(30), customUser);

        name = addCUProp(baseGroup, "Имя", addDProp("name", "Имя", StringClass.get(60), baseClass.named),
                addJProp(string2, userFirstName, 1, userLastName, 1));

        nameToPolicy = addCGProp(null, "nameToPolicy", "Политика", object(policy), name, name, 1);
        policyDescription = addDProp(baseGroup, "description", "Описание", StringClass.get(100), policy);

        userPolicyOrder = addDProp(baseGroup, "userPolicyOrder", "Порядок политики", IntegerClass.instance, customUser, policy);

        barcode = addDProp(baseGroup, "barcode", "Штрих-код", StringClass.get(13), barcodeObject);
        barcodeToObject = addCGProp(null, "barcodeToObject", "Объект", object(barcodeObject), barcode, barcode, 1);
        barcodeObjectName = addJProp(baseGroup, "Объект", name, barcodeToObject, 1);

        currentUserName = addJProp("Имя тек. польз.", name, currentUser);

        reverseBarcode = addSDProp("Реверс", LogicalClass.instance);

        classSID = addDProp(baseGroup, "classSID", "Стат. код", IntegerClass.instance, baseClass.objectClass);
        objectClass = addProperty(null, new LP<ClassPropertyInterface>(new ObjectClassProperty(genSID(), baseClass)));
        objectClassName = addJProp(baseGroup, "Класс объекта", name, objectClass, 1);

        // заполним сессии
        LP sessionUser = addDProp("sessionUser", "Пользователь сессии", user, session);
        sessionUser.setDerivedChange(currentUser, true, is(session), 1);
        addJProp(baseGroup, "Пользователь сессии", name, sessionUser, 1);
        LP sessionDate = addDProp(baseGroup, "sessionDate", "Дата сессии", DateClass.instance, session);
        sessionDate.setDerivedChange(currentDate, true, is(session), 1);
        onlyNotZero = addJProp(andNot1, 1, addJProp(equals2, 1, vzero), 1);
    }

    void initBaseNavigators() {
        NavigatorElement policy = new NavigatorElement(baseElement, 50000, "Политики безопасности");
        addFormEntity(new UserPolicyFormEntity(policy, 50100));
    }

    protected SecurityPolicy permitAllPolicy, readOnlyPolicy;

    void initBaseAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        permitAllPolicy = addPolicy("Разрешить всё", "Политика разрешает все действия.");

        readOnlyPolicy = addPolicy("Запретить редактирование всех свойств", "Режим \"только чтение\". Запрещает редактирование всех свойств на формах.");
        readOnlyPolicy.property.change.defaultPermission = false;
        readOnlyPolicy.cls.edit.add.defaultPermission = false;
        readOnlyPolicy.cls.edit.change.defaultPermission = false;
        readOnlyPolicy.cls.edit.remove.defaultPermission = false;
    }

    public void ping() throws RemoteException {
        return;
    }

    private class UserPolicyFormEntity extends FormEntity {
        protected UserPolicyFormEntity(NavigatorElement parent, int ID) {
            super(parent, ID, "Политики пользователей");

            ObjectEntity objUser = addSingleGroupObject(customUser, selection, baseGroup, true);
            ObjectEntity objPolicy = addSingleGroupObject(policy, baseGroup, true);

            addObjectActions(this, objUser);

            addPropertyDraw(objUser, objPolicy, baseGroup, true);

            PropertyDrawEntity balanceDraw = getPropertyDraw(userPolicyOrder, objPolicy.groupTo);
            PropertyDrawEntity loginDraw = getPropertyDraw(userLogin, objUser.groupTo);
            balanceDraw.addColumnGroupObject(objUser.groupTo);
            balanceDraw.setPropertyCaption(loginDraw.propertyObject);
        }
    }

    @IdentityLazy
    private ActionProperty getAddObjectAction(ValueClass cls) {
        AddObjectActionProperty property = new AddObjectActionProperty(genSID(), (CustomClass) cls);
        addProperty(null, new LP<ClassPropertyInterface>(property));
        return property;
    }

    @IdentityLazy
    private ActionProperty getImportObjectAction(ValueClass cls) {
        ImportFromExcelActionProperty property = new ImportFromExcelActionProperty(genSID(), (CustomClass) cls);
        addProperty(null, new LP<ClassPropertyInterface>(property));
        return property;
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

        initBase();

        initGroups();

        initClasses();
        checkClasses(); //проверка на то, что у каждого абстрактного класса есть конкретный потомок

        // после classes и до properties, чтобы можно было бы создать таблицы и использовать persistent таблицы в частности для определения классов
        initTables();

        initBaseProperties();

        initProperties();

        Set idSet = new HashSet<String>();
        for (Property property : properties) {
            assert idSet.add(property.sID) : "Same sid " + property.sID;
        }

        initIndexes();

        assert checkProps();

        synchronizeDB();

        fillIDs();

        initExternalScreens();

        logger.info("Initializing navigators...");

        baseElement.add(baseClass.getBaseClassForm(this));

        synchronizeDB();

        initBaseAuthentication();
        initAuthentication();

        initBaseNavigators();
        initNavigators();

        // считаем системного пользователя
        try {
            DataSession session = createSession(new UserController() {
                public void changeCurrentUser(DataObject user) {
                    throw new RuntimeException("not supported");
                }

                public DataObject getCurrentUser() {
                    return new DataObject(0, systemUser);
                }
            });

            Query<String, Object> query = new Query<String, Object>(Collections.singleton("key"));
            query.and(BaseUtils.singleValue(query.mapKeys).isClass(systemUser));
            Set<Map<String, Object>> rows = query.execute(session, new OrderedMap<Object, Boolean>(), 1).keySet();
            if (rows.size() == 0) { // если нету добавим
                systemUserObject = (Integer) session.addObject(systemUser, session.modifier).object;
                session.apply(this);
            } else
                systemUserObject = (Integer) BaseUtils.single(rows).get("key");

            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // запишем текущую дату
        changeCurrentDate();

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
        assert (!(c instanceof AbstractCustomClass) || (c.hasChildren())) : "Doesn't exist concrete class";
        for (CustomClass children : c.children) {
            checkClass(children);
        }
    }

    public final int systemUserObject;
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
    public AbstractGroup baseGroup;
    public AbstractGroup publicGroup;
    public AbstractGroup aggrGroup;
    public AbstractGroup privateGroup;

    protected abstract void initGroups();

    protected abstract void initClasses();

    protected abstract void initProperties();

    protected abstract void initTables();

    protected abstract void initIndexes();

    public NavigatorElement<T> baseElement;

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

        logger.info("Initializing stored property...");
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

    public DataSession createSession() throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        return createSession(new UserController() {
            public void changeCurrentUser(DataObject user) {
                throw new RuntimeException("not supported");
            }

            public DataObject getCurrentUser() {
                return new DataObject(systemUserObject, systemUser);
            }
        });
    }

    public DataSession createSession(UserController userController) throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        return new DataSession(adapter, userController, baseClass, baseClass.named, session, name, transaction, date, notDeterministic);
    }

    public List<DerivedChange<?, ?>> notDeterministic = new ArrayList<DerivedChange<?, ?>>();

    public BaseClass baseClass;

    public TableFactory tableFactory;
    public List<LP> lproperties = new ArrayList<LP>();
    public Set<Property> properties = new HashSet<Property>();
    protected Set<AggregateProperty> persistents = new HashSet<AggregateProperty>();
    protected Set<List<? extends Property>> indexes = new HashSet<List<? extends Property>>();

    // получает список св-в в порядке использования
    private void fillPropertyList(Property<?> property, LinkedHashSet<Property> set) {
        for (Property depend : property.getDepends())
            fillPropertyList(depend, set);
        set.add(property);
    }

    @IdentityLazy
    public Iterable<Property> getPropertyList() {
        LinkedHashSet<Property> linkedSet = new LinkedHashSet<Property>();
        for (Property property : properties)
            fillPropertyList(property, linkedSet);
        return linkedSet;
    }

    private boolean depends(Property<?> property, Property check) {
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

        Map<Integer, CustomClass> usedSIds = new HashMap<Integer, CustomClass>();
        Set<Integer> usedIds = new HashSet<Integer>();
        Set<CustomClass> allClasses = new HashSet<CustomClass>();
        baseClass.fillChilds(allClasses);

        // baseClass'у и baseClass.objectClass'у нужны ID сразу потому как учавствуют в addObject
        baseClass.ID = 0;
        baseClass.named.ID = 1;
        baseClass.objectClass.ID = baseClass.objectClass.sID;

        session.startTransaction();

        CustomClass usedClass;
        for (CustomClass customClass : allClasses)
            if (customClass instanceof ConcreteCustomClass) {
                ConcreteCustomClass concreteClass = (ConcreteCustomClass) customClass;
                if ((usedClass = usedSIds.put(concreteClass.sID, customClass)) != null)
                    throw new RuntimeException("Одинаковые идентификаторы у классов " + customClass.caption + " и " + usedClass.caption);

                // ищем класс с таким sID, если не находим создаем
                Query<String, Object> findClass = new Query<String, Object>(Collections.singleton("key"));
                findClass.and(classSID.getExpr(session.modifier, BaseUtils.singleValue(findClass.mapKeys)).compare(new ValueExpr(concreteClass.sID, IntegerClass.instance), Compare.EQUALS));
                OrderedMap<Map<String, Object>, Map<Object, Object>> result = findClass.execute(session);
                if (result.size() == 0) { // не найдено добавляем новый объект и заменяем ему classID и caption
                    DataObject classObject;
                    if (concreteClass.equals(baseClass.objectClass)) { // добавим с явным ID объект
                        classObject = new DataObject(baseClass.objectClass.sID, baseClass.unknown);
                        session.changeClass(classObject, baseClass.objectClass);
                    } else {
                        classObject = session.addObject(baseClass.objectClass, session.modifier);
                        concreteClass.ID = (Integer) classObject.object;

                        // также для обратной совместимости в objects меняем старые ID на новые, только если с таким sID нету ID
//                        if(!session.isRecord(baseClass.table, Collections.singletonMap(baseClass.table.key, new DataObject(concreteClass.sID, SystemClass.instance)))) {
//                        Query<KeyField, PropertyField> update = new Query<KeyField, PropertyField>(baseClass.table);
//                        Join<PropertyField> baseJoin = baseClass.table.join(update.mapKeys);
//                        update.and(baseJoin.getExpr(baseClass.table.objectClass).compare(new ValueExpr(concreteClass.sID, SystemClass.instance), Compare.EQUALS));
//                        update.properties.put(baseClass.table.objectClass, new ValueExpr(classObject.object, SystemClass.instance));
//                        session.modifyRecords(new ModifyQuery(baseClass.table, update));
//                        }
                    }

                    name.execute(concreteClass.caption, session, session.modifier, classObject);
                    classSID.execute(concreteClass.sID, session, session.modifier, classObject);
                } else // assert'ся что класс 1
                    concreteClass.ID = (Integer) BaseUtils.singleKey(result).get("key");

                usedIds.add(concreteClass.ID);
            }

        session.apply(this);

        int free = 0;
        for (CustomClass customClass : allClasses)
            if (customClass instanceof AbstractCustomClass) {
                while (usedIds.contains(free))
                    free++;
                customClass.ID = free++;
            }

        /*session.startTransaction();
        for(CustomClass customClass : allClasses)
            if(customClass instanceof ConcreteCustomClass)
                name.execute(customClass.caption.substring(0,BaseUtils.min(customClass.caption.length(),50)), session, session.modifier, new DataObject(customClass.ID, baseClass.objectClass));
        session.apply(this);*/
    }

    public void synchronizeDB() throws SQLException, IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {

        DataSession session = createSession();

        session.startTransaction();

        // инициализируем таблицы
        tableFactory.fillDB(session);

        // "старое" состояние базы
        DataInputStream inputDB = null;
        byte[] struct = (byte[]) session.readRecord(GlobalTable.instance, new HashMap<KeyField, DataObject>(), GlobalTable.instance.struct);
        if (struct != null) inputDB = new DataInputStream(new ByteArrayInputStream(struct));

        if (struct != null && struct.length == 0) { //чисто для бага JTDS
            session.rollbackTransaction();
            session.close();
            return;
        }
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
            Table prevTable = new Table(inputDB);
            prevTables.put(prevTable.name, prevTable);

            for (int j = inputDB.readInt(); j > 0; j--) {
                List<String> index = new ArrayList<String>();
                for (int k = inputDB.readInt(); k > 0; k--)
                    index.add(inputDB.readUTF());
                ImplementTable implementTable = implementTables.get(prevTable.name);
                if (implementTable == null || !mapIndexes.get(implementTable).remove(index))
                    session.dropIndex(prevTable.name, index);
            }
        }

        // добавим таблицы которых не было
        for (ImplementTable table : implementTables.values())
            if (!prevTables.containsKey(table.name))
                session.createTable(table.name, table.keys);

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
                            session.addColumn(property.mapTable.table.name, property.field);
                            // делаем запрос на перенос
                            System.out.print("Идет перенос колонки " + property.field + " (" + property.caption + ")" + " из таблицы " + prevTable.name + " в таблицу " + property.mapTable.table.name + "... ");
                            Query<KeyField, PropertyField> moveColumn = new Query<KeyField, PropertyField>(property.mapTable.table);
                            Expr moveExpr = prevTable.joinAnd(BaseUtils.join(BaseUtils.join(foundInterfaces, ((Property<PropertyInterface>) property).mapTable.mapKeys), moveColumn.mapKeys)).getExpr(prevTable.findProperty(sID));
                            moveColumn.properties.put(property.field, moveExpr);
                            moveColumn.and(moveExpr.getWhere());
                            session.modifyRecords(new ModifyQuery(property.mapTable.table, moveColumn));
                            logger.info("Done");
                        } else // надо проверить что тип не изменился
                            if (!prevTable.findProperty(sID).type.equals(property.field.type))
                                session.modifyColumn(property.mapTable.table.name, property.field, prevTable.findProperty(sID).type);
                        is.remove();
                    }
                    break;
                }
            }
            if (!keep) {
                session.dropColumn(prevTable.name, sID);
                ImplementTable table = implementTables.get(prevTable.name); // надо упаковать таблицу если удалили колонку
                if (table != null) packTables.add(table);
            }
        }

        Collection<AggregateProperty> recalculateProperties = new ArrayList<AggregateProperty>();
        for (Property property : storedProperties) { // добавляем оставшиеся
            session.addColumn(property.mapTable.table.name, property.field);
            if (property instanceof AggregateProperty)
                recalculateProperties.add((AggregateProperty) property);
        }

        // удаляем таблицы старые
        for (String table : prevTables.keySet())
            if (!implementTables.containsKey(table))
                session.dropTable(table);

        // упакуем таблицы
        for (ImplementTable table : packTables)
            session.packTable(table);

        recalculateAggregations(session, recalculateProperties);

        // создадим индексы в базе
        for (Map.Entry<ImplementTable, Set<List<String>>> mapIndex : mapIndexes.entrySet())
            for (List<String> index : mapIndex.getValue())
                session.addIndex(mapIndex.getKey().name, index);

        try {
            session.updateInsertRecord(GlobalTable.instance, new HashMap<KeyField, DataObject>(),
                    Collections.singletonMap(GlobalTable.instance.struct, (ObjectValue) new DataObject((Object) outDBStruct.toByteArray(), ByteArrayClass.instance)));
        }
        catch (Exception e) {
            session.updateInsertRecord(GlobalTable.instance, new HashMap<KeyField, DataObject>(),
                    Collections.singletonMap(GlobalTable.instance.struct, (ObjectValue) new DataObject((Object) new byte[0], ByteArrayClass.instance)));
        }

        session.commitTransaction();

/*        byte[] outBytes = outDBStruct.toByteArray();
        FileOutputStream outFileStruct = new FileOutputStream("prevstruct.str");
        outFileStruct.write(outBytes.length/255);
        outFileStruct.write(outBytes.length%255);
        outFileStruct.write(outBytes);*/

        session.close();
    }

    boolean checkPersistent(SQLSession session) throws SQLException {
//        System.out.println("checking persistent...");
        for (Property property : getStoredProperties()) {
//            System.out.println(Property.caption);
            if (property instanceof AggregateProperty && !((AggregateProperty) property).checkAggregation(session, property.caption)) // Property.caption.equals("Расх. со скл.")
                return false;
//            Property.Out(Adapter);
        }

        return true;
    }

    // функционал по заполнению св-в по номерам, нужен для BL

    int genIDClasses = 0;

    protected ConcreteCustomClass addConcreteClass(String caption, CustomClass... parents) {
        return addConcreteClass(baseGroup, genIDClasses++, caption, parents);
    }

    protected ConcreteCustomClass addConcreteClass(Integer ID, String caption, CustomClass... parents) {
        return addConcreteClass(baseGroup, ID, caption, parents);
    }

    protected ConcreteCustomClass addConcreteClass(AbstractGroup group, int sID, String caption, CustomClass... parents) {
        ConcreteCustomClass customClass = new ConcreteCustomClass(sID, caption, parents);
        group.add(customClass);
        return customClass;
    }

    protected AbstractCustomClass addAbstractClass(String caption, CustomClass... parents) {
        return addAbstractClass(baseGroup, caption, parents);
    }

    protected AbstractCustomClass addAbstractClass(AbstractGroup group, String caption, CustomClass... parents) {
        AbstractCustomClass customClass = new AbstractCustomClass(caption, parents);
        group.add(customClass);
        return customClass;
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
        return addDCProp(null, sID, persistent, caption, derivedProp, params);
    }

    protected <D extends PropertyInterface> LP addDCProp(AbstractGroup group, String sID, String caption, LP<D> derivedProp, Object... params) {
        return addDCProp(group, sID, false, caption, derivedProp, params);
    }

    protected <D extends PropertyInterface> LP addDCProp(AbstractGroup group, String sID, boolean persistent, String caption, LP<D> derivedProp, Object... params) {

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

    protected LP addFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity... params) {
        return addProperty(group, new LP<ClassPropertyInterface>(new FormActionProperty(genSID(), caption, form, params)));
    }

    protected <P extends PropertyInterface> LP addSCProp(LP<P> lp) {
        return addSCProp(privateGroup, "sys", lp);
    }

    protected <P extends PropertyInterface> LP addSCProp(AbstractGroup group, String caption, LP<P> lp) {
        return addProperty(group, new LP<ShiftChangeProperty.Interface<P>>(new ShiftChangeProperty<P, PropertyInterface>(genSID(), caption, lp.property, new PropertyMapImplement<PropertyInterface, P>(reverseBarcode.property))));
    }

    protected LP addCProp(ConcreteValueClass valueClass, Object value, ValueClass... params) {
        return addCProp(privateGroup, genSID(), "sys", valueClass, value, params);
    }

    protected LP addCProp(String caption, ConcreteValueClass valueClass, Object value, ValueClass... params) {
        return addCProp(genSID(), caption, valueClass, value, params);
    }

    protected LP addCProp(String sID, String caption, ConcreteValueClass valueClass, Object value, ValueClass... params) {
        return addCProp(null, sID, caption, valueClass, value, params);
    }

    protected LP addCProp(String sID, boolean persistent, String caption, ConcreteValueClass valueClass, Object value, ValueClass... params) {
        return addCProp(null, sID, persistent, caption, valueClass, value, params);
    }

    protected LP addCProp(AbstractGroup group, String sID, String caption, ConcreteValueClass valueClass, Object value, ValueClass... params) {
        return addCProp(group, sID, false, caption, valueClass, value, params);
    }

    protected LP addCProp(AbstractGroup group, String sID, boolean persistent, String caption, ConcreteValueClass valueClass, Object value, ValueClass... params) {
        return addProperty(group, persistent, new LP<ClassPropertyInterface>(new ClassProperty(sID, caption, params, valueClass, value)));
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
        TimeChangeDataProperty<P> timeProperty = new TimeChangeDataProperty<P>(sID, caption, overrideClasses(changeProp.getMapClasses(), classes), changeProp.listInterfaces);
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
        return addProperty(null, new LP<StringConcatenateProperty.Interface>(new StringConcatenateProperty(genSID(), "Объед.", intNum)));
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

    private <T extends LP<?>> T addProperty(AbstractGroup group, T lp) {
        return addProperty(group, false, lp);
    }

    private <T extends LP<?>> T addProperty(AbstractGroup group, boolean persistent, T lp) {
        lproperties.add(lp);
        properties.add(lp.property);
        lp.property.ID = idGenerator.idShift();
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

        LP listProperty = new LP<JoinProperty.Interface>(property);
        property.implement = mapImplement(mainProp, readImplements(listProperty.listInterfaces, params));

        return addProperty(group, persistent, listProperty);
    }

    private <T extends PropertyInterface> LP addGProp(AbstractGroup group, String sID, String caption, LP<T> groupProp, boolean sum, Object... params) {
        return addGProp(group, sID, false, caption, groupProp, sum, params);
    }

    private <T extends PropertyInterface> LP addGProp(AbstractGroup group, String sID, boolean persistent, String caption, LP<T> groupProp, boolean sum, Object... params) {

        GroupProperty<T> property;
        List<PropertyInterfaceImplement<T>> listImplements = readImplements(groupProp.listInterfaces, params);
        if (sum)
            property = new SumGroupProperty<T>(sID, caption, listImplements, groupProp.property);
        else
            property = new MaxGroupProperty<T>(sID, caption, listImplements, groupProp.property);

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

    private <P extends PropertyInterface> LP mapLGProp(AbstractGroup group, GroupProperty<P> property, List<PropertyInterfaceImplement<P>> listImplements) {
        return mapLGProp(group, false, property, listImplements);
    }

    private <P extends PropertyInterface> LP mapLGProp(AbstractGroup group, boolean persistent, GroupProperty<P> property, List<PropertyInterfaceImplement<P>> listImplements) {
        return mapLGProp(group, persistent, new PropertyImplement<PropertyInterfaceImplement<P>, GroupProperty.Interface<P>>(property, property.getMapInterfaces()), listImplements);
    }

    protected <P extends PropertyInterface> LP addOProp(String caption, LP<P> sum, boolean percent, boolean ascending, boolean includeLast, int partNum, Object... params) {
        return addOProp((AbstractGroup) null, genSID(), caption, sum, percent, ascending, includeLast, partNum, params);
    }

    protected <P extends PropertyInterface> LP addOProp(AbstractGroup group, String caption, LP<P> sum, boolean percent, boolean ascending, boolean includeLast, int partNum, Object... params) {
        return addOProp(group, genSID(), caption, sum, percent, ascending, includeLast, partNum, params);
    }

    protected <P extends PropertyInterface> LP addOProp(AbstractGroup group, String sID, String caption, LP<P> sum, boolean percent, boolean ascending, boolean includeLast, int partNum, Object... params) {
        return addOProp(group, sID, false, caption, sum, percent, ascending, includeLast, partNum, params);
    }

    protected <P extends PropertyInterface> LP addOProp(AbstractGroup group, String sID, boolean persistent, String caption, LP<P> sum, boolean percent, boolean ascending, boolean includeLast, int partNum, Object... params) {
        List<LI> li = readLI(params);

        Collection<PropertyInterfaceImplement<P>> partitions = mapLI(li.subList(0, partNum), sum.listInterfaces);
        OrderedMap<PropertyInterfaceImplement<P>, Boolean> orders = new OrderedMap<PropertyInterfaceImplement<P>, Boolean>(mapLI(li.subList(partNum, li.size()), sum.listInterfaces), !ascending);

        PropertyMapImplement<?, P> orderProperty;
        if (percent)
            orderProperty = DerivedProperty.createPOProp(sID, caption, sum.property, partitions, orders, includeLast);
        else
            orderProperty = DerivedProperty.createOProp(sID, caption, sum.property, partitions, orders, includeLast);

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

    /*
      // свойство обратное группируещему - для этого задается ограничивающее свойство, результирующее св-во с группировочными, порядковое св-во
      protected LF addUGProp(AbstractGroup group, String caption, LF maxGroupProp, LF unGroupProp, Object... params) {
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


          // возвращаем MIN(unGroup-MU(prevGroup,0(maxGroup)),maxGroup) и не unGroup<=prevGroup
          LF zeroQuantity = addJProp(and1, BaseUtils.add(new Object[]{vzero},directLI(maxGroupProp)));
          LF zeroRemainPrev = addSUProp(Union.OVERRIDE , zeroQuantity, remainPrev);
          LF calc = addSFProp("prm3+prm1-prm2-GREATEST(prm3,prm1-prm2)",DoubleClass.instance,3);
          LF maxRestRemain = addJProp(calc, BaseUtils.add(BaseUtils.add(unGroupProp.write(),directLI(zeroRemainPrev)),directLI(maxGroupProp)));
          LF exceed = addJProp(groeq2, BaseUtils.add(directLI(remainPrev),unGroupProp.write()));
          return addJProp(group, caption, andNot1, BaseUtils.add(directLI(maxRestRemain),directLI(exceed)));
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
        if (persistent) {
            LP property = addGProp(group, genSID(), false, caption, groupProp, true, params);
            return addJProp(sID, persistent, caption, onlyNotZero, directLI(property));
        } else {
            return addGProp(group, sID, false, caption, groupProp, true, params);
        }
    }

    protected LP addMGProp(LP groupProp, Object... params) {
        return addMGProp(privateGroup, genSID(), "sys", groupProp, params);
    }

    protected LP addMGProp(AbstractGroup group, String sID, String caption, LP groupProp, Object... params) {
        return addMGProp(group, new String[]{sID}, new String[]{caption}, 0, groupProp, params)[0];
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

    protected <T extends PropertyInterface, P extends PropertyInterface> LP addDGProp(AbstractGroup group, String sID, boolean persistent, String caption, int orders, boolean ascending, LP<T> groupProp, Object... params) {
        List<PropertyInterfaceImplement<T>> listImplements = readImplements(groupProp.listInterfaces, params);
        int intNum = listImplements.size();

        // читаем groupProp, implements его для группировки, restriction с map'ом и orders с map'ом на restriction
        List<PropertyInterfaceImplement<T>> groupImplements = listImplements.subList(0, intNum - orders - 1);
        DistrGroupProperty<T, P> property = new DistrGroupProperty<T, P>(sID, caption, groupImplements, groupProp.property,
                new OrderedMap<PropertyInterfaceImplement<T>, Boolean>(listImplements.subList(intNum - orders, intNum), ascending),
                (PropertyMapImplement<P, T>) listImplements.get(intNum - orders - 1));

        // нужно добавить ограничение на уникальность
        return mapLGProp(group, persistent, property, groupImplements);
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
            for (Property prop : properties) {
                logger.info("Checking property : " + prop + "...");
                assert prop.check();
            }
        for (LP[] props : checkCUProps) {
            logger.info("Checking class properties : " + props + "...");
            assert !intersect(props);
        }
        for (LP[] props : checkSUProps) {
            logger.info("Checking union properties : " + props + "...");
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
             dataProp.caption = "Data Property " + i;
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
                 genProp.caption = resType + " " + i;
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
//            if(Property.caption.equals("R 1"))
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
                System.out.println("Класс : "+fillClass.caption);

                Integer quantity = classQuantity.get(fillClass);
                if(quantity==null) quantity = 1;

                List<DataObject> listObjects = new ArrayList<DataObject>();
                for(int i=0;i<quantity;i++) {
                    DataObject idObject = session.addObject(fillClass,null);
                    listObjects.add(idObject);
                    objectNames.put(idObject,fillClass.caption+" "+(i+1));
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

                System.out.println("Свойство : "+property.caption);

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
                    System.out.print("Идет перерасчет аггрегированного св-ва (" + dependProperty + ")... ");
                    dependProperty.recalculateAggregation(session);
                    logger.info("Done");
                }
            }
    }

    public <T extends FormEntity> T addFormEntity(T form) {
        // форма "перегружена"
        try {
            byte[] formState = IOUtils.getFileBytes(new File("conf/forms/form" + form.getID()));
            FormEntity overridenForm = FormEntity.deserialize(this, formState);
            form.getParent().replaceChild(form, overridenForm);
            //TODO : здесь возвращать надо стопроцентов overridenForm, но будут проблемы с программным взаимодействием
            return form;
        } catch (IOException e) {
            form.richDesign = form.createDefaultRichDesign();
        }
        return form;
    }

    public void addObjectActions(FormEntity form, ObjectEntity object) {
        form.forceDefaultDraw.put(form.addPropertyDraw(new LP<ClassPropertyInterface>(getImportObjectAction(object.baseClass))), object.groupTo);
        form.forceDefaultDraw.put(form.addPropertyDraw(new LP<ClassPropertyInterface>(getAddObjectAction(object.baseClass))), object.groupTo);
        form.addPropertyDraw(delete, object);
    }

    private Scheduler scheduler;

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

    protected LP addMAProp(String message, String caption) {
        return addMAProp(message, null, caption);
    }

    protected LP addMAProp(String message, AbstractGroup group, String caption) {
        return addProperty(group, new LP<ClassPropertyInterface>(new MessageActionProperty(message, genSID(), caption)));
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
                            Map<OrderInstance, Object> userSeeks = form.userGroupSeeks.get(object.groupTo);
                            if (userSeeks == null) {
                                userSeeks = new HashMap<OrderInstance, Object>();
                                form.userGroupSeeks.put(object.groupTo, userSeeks);
                            }
                            userSeeks.put(object, key.getValue().object);
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

    protected LP addAProp(AbstractGroup group, ActionProperty property) {
        return addProperty(null, new LP<ClassPropertyInterface>(property));
    }

    protected LP addBAProp(ConcreteCustomClass customClass, LP add) {
        return addAProp(null, new AddBarcodeActionProperty(customClass, add.property, genSID()));
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
            if (addProperty.read(session, new HashMap(), remoteForm) != null) {
                addProperty.execute(new HashMap(), session, null, remoteForm);
                barcode.execute(BaseUtils.singleValue(keys).object, session, remoteForm, session.addObject(customClass, remoteForm));
            }
        }
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

    public void outputPropertyClasses() {
        for (LP lp : lproperties) {
            logger.info(lp.property.sID + " : " + lp.property.caption + " - " + lp.getClassWhere());
        }
    }

    public int generateNewID() throws RemoteException {
        return idGenerator.idShift();
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
            Map<Integer, ValueClass> classes = new HashMap<Integer, ValueClass>();
            int size = inStream.readInt();
            for (int i = 0; i < size; i++) {
                Integer ID = inStream.readInt();
                classes.put(ID, TypeSerializer.deserializeValueClass(this, inStream));
                int groupId = inStream.readInt();
                if (groupId >= 0) {
                    groupMap.put(ID, groupId);
                }
            }

            MultiHashMap propertiesMap = new MultiHashMap();
            for (Property<?> property : properties) {
                propertiesMap.put(property.interfaces.size(), property);
            }

            ByteArrayOutputStream outStream = new ByteArrayOutputStream();

            DataOutputStream dataStream = new DataOutputStream(outStream);

            ServerSerializationPool pool = new ServerSerializationPool();

            ArrayList<Property> result = new ArrayList<Property>();
            ArrayList<ArrayList<Integer>> idResult = new ArrayList<ArrayList<Integer>>();
            for (Object key : propertiesMap.keySet()) {
                List list = (List) propertiesMap.get(key);
                addPropertiesFixedSize(classes, groupMap, list, result, idResult, isCompulsory, isAny);
            }

            dataStream.writeInt(result.size());
            int num = 0;
            for (Property<?> property : result) {
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

    private void addPropertiesFixedSize(Map<Integer, ValueClass> classes, Map<Integer, Integer> atLeastOne, List list, ArrayList<Property> result, ArrayList<ArrayList<Integer>> idResult, boolean isCompulsory, boolean isAny) {
        int size = classes.size();
        int interfaceSize = ((Property) list.get(0)).interfaces.size();
        int perm[] = new int[size];
        checkPerm(size, interfaceSize, 1, perm, classes, atLeastOne, list, result, idResult, isCompulsory, isAny);
    }

    //n - сколько всего
    //n - сколько надо поставить
    //k - какой по счету сейчас ставим
    private void checkPerm(int n, int m, int k, int[] perm, Map<Integer, ValueClass> classes, Map<Integer, Integer> groupMap, List list, ArrayList<Property> result, ArrayList<ArrayList<Integer>> idResult, boolean isCompulsory, boolean isAny) {
        if (k == m + 1) {
            Integer id[] = (Integer[]) classes.keySet().toArray(new Integer[classes.keySet().size()]);
            ArrayList<ValueClass> values = new ArrayList<ValueClass>(m);
            ArrayList<Integer> ids = new ArrayList<Integer>(m);
            for (int i = 0; i < m; i++) {
                values.add(null);
                ids.add(0);
            }

            for (int i = 0; i < n; i++) {
                if (perm[i] != 0) {
                    values.set(perm[i] - 1, classes.get(id[i]));
                    ids.set(perm[i] - 1, id[i]);
                }
            }

            Map<Integer, Boolean> isUsed = new HashMap<Integer, Boolean>();
            for (Map.Entry<Integer, Integer> entry : groupMap.entrySet()) {
                if (isUsed.get(entry.getValue()) == null) {
                    isUsed.put(entry.getValue(), false);
                }
                if (ids.contains(entry.getKey())) {
                    isUsed.put(entry.getValue(), true);
                }
            }
            boolean and = true, or = false;
            for (Map.Entry<Integer, Boolean> entry : isUsed.entrySet()) {
                and = and && entry.getValue();
                or = or || entry.getValue();
            }

            if ((isCompulsory && !and) || (!isCompulsory && !or && !groupMap.isEmpty())) {
                return;
            }

            for (Object prop : list) {
                Property p = (Property) prop;
                Map<PropertyInterface, AndClassSet> propertyInterface = new HashMap<PropertyInterface, AndClassSet>();
                int interfaceCount = 0;
                for (Object iface : p.interfaces) {
                    ValueClass propertyClass = values.get(interfaceCount++);
                    propertyInterface.put((PropertyInterface) iface, propertyClass.getUpSet());
                }
                if (!(p instanceof StringFormulaProperty) && p.isFull() && ((isAny && p.anyInInterface(propertyInterface)) || (!isAny && p.allInInterface(propertyInterface)))) {
                    result.add(p);
                    idResult.add(ids);
                }
            }

        } else {
            for (int i = 0; i < n; i++) {
                if (perm[i] == 0) {
                    perm[i] = k;
                    checkPerm(n, m, k + 1, perm, classes, groupMap, list, result, idResult, isCompulsory, isAny);
                    perm[i] = 0;
                }
            }
        }
    }
}
