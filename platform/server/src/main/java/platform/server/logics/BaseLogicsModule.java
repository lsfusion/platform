package platform.server.logics;

import org.apache.log4j.Logger;
import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.Result;
import platform.base.identity.DefaultIDGenerator;
import platform.base.identity.IDGenerator;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.action.*;
import platform.server.Settings;
import platform.server.caches.IdentityLazy;
import platform.server.classes.*;
import platform.server.data.SQLSession;
import platform.server.data.Time;
import platform.server.data.Union;
import platform.server.data.expr.query.OrderType;
import platform.server.data.where.classes.ClassWhere;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.*;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.ContainerView;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.form.window.NavigatorWindow;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.*;
import platform.server.logics.property.derived.AggregateGroupProperty;
import platform.server.logics.property.derived.ConcatenateProperty;
import platform.server.logics.property.derived.CycleGroupProperty;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.property.group.PropertySet;
import platform.server.logics.table.TableFactory;
import platform.server.mail.EmailActionProperty;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.consecutiveInts;
import static platform.base.BaseUtils.toPrimitive;
import static platform.server.logics.PropertyUtils.*;

/**
 * User: DAle
 * Date: 16.05.11
 * Time: 17:52
 */

// todo [dale]: отрефакторить нормально по смыслу и использованию все модификаторы доступа

public class BaseLogicsModule<T extends BusinessLogics<T>> extends LogicsModule {
    // classes
    public BaseClass baseClass;
    public Logger logger;

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
    public ConcreteCustomClass connection;
    public StaticCustomClass connectionStatus;
    public ConcreteCustomClass dictionary;
    public ConcreteCustomClass dictionaryEntry;

    public AbstractCustomClass transaction, barcodeObject;
    protected AbstractCustomClass namedUniqueObject;

    public AbstractCustomClass emailObject;


    // groups
    public AbstractGroup rootGroup;
    public AbstractGroup publicGroup;
    public AbstractGroup privateGroup;
    public AbstractGroup baseGroup;
    public AbstractGroup idGroup;
    public AbstractGroup actionGroup;
    public AbstractGroup sessionGroup;
    public AbstractGroup recognizeGroup;


    // properties
    public LP groeq2;
    protected LP lsoeq2;
    public LP greater2, less2;
    public LP greater22, less22;
    public LP between;
    protected LP betweenDate;
    public LP betweenDates;
    public LP object1, and1, andNot1;
    public LP equals2, diff2;
    public LP sumDouble2;
    protected LP subtractDouble2;
    protected LP deltaDouble2;
    public LP multiplyDouble2;
    public LP multiplyIntegerBy2;
    protected LP squareInteger;
    protected LP squareDouble;
    protected LP sqrtDouble2;
    protected LP divideDouble;
    public LP divideDouble2;
    public LP addDate2;
    public LP string2;
    public LP insensitiveString2;
    protected LP concat2;
    public LP percent;
    public LP percent2;
    public LP yearInDate;

    public LP vtrue, actionTrue, vzero;
    public LP positive, negative;

    public LP dumb1;
    public LP dumb2;

    protected LP castText;
    protected LP addText2;

    public LP<?> name;
    public LP<?> date;

    protected LP transactionLater;
    public LP currentDate;
    public LP currentHour;
    public LP currentMinute;
    protected LP currentEpoch;
    protected LP currentDateTime;
    public LP currentUser;
    public LP currentSession;
    public LP currentComputer;
    public LP changeUser;
    protected LP isServerRestarting;
    public LP<PropertyInterface> barcode;
    public LP barcodeToObject;
    public LP barcodeObjectName;
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
    public LP userRoleDefaultForms;
    public LP userDefaultForms;
    public LP sidToRole;
    public LP inUserRole;
    public LP inLoginSID;
    public LP currentUserName;
    public LP<?> loginToUser;

    public LP email;
    public LP emailToObject;
    public LP generateLoginPassword;

    public LP emailUserPassUser;

    public LP connectionUser;
    public LP connectionComputer;
    public LP connectionCurrentStatus;
    public LP connectionFormCount;
    public LP connectionConnectTime;
    public LP connectionDisconnectTime;

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
    public LP dataName;
    public LP navigatorElementSID;
    public LP navigatorElementCaption;

    public LP SIDToNavigatorElement;
    public LP permissionUserRoleForm;
    public LP permissionUserForm;
    public LP userRoleFormDefaultNumber;
    public LP userFormDefaultNumber;

    public LP customID;
    public LP stringID;
    public LP integerID;
    public LP dateID;

    public LP objectByName;
    public LP seekObjectName;

    public LP webHost;

    public LP smtpHost;
    public LP smtpPort;
    public LP emailAccount;
    public LP emailPassword;
    public LP emailBlindCarbonCopy;
    public LP fromAddress;
    public LP disableEmail;
    protected LP defaultCountry;

    public LP sidCountry;
    protected LP generateDatesCountry;
    protected LP sidToCountry;
    public LP nameToCountry;
    protected LP nameToObject;
    protected LP isDayOffCountryDate;
    LP workingDay, isWorkingDay, workingDaysQuantity, equalsWorkingDaysQuantity;

    protected LP termDictionary;
    protected LP translationDictionary;
    protected LP entryDictionary;
    public LP translationDictionaryTerm;

    private LP selectRoleForms;
    private LP selectUserRoles;

    private LP<ClassPropertyInterface> recalculateAction;
    private LP<ClassPropertyInterface> packAction;

    public SelectionPropertySet selection;
    protected CompositeNamePropertySet compositeName;
    public ObjectValuePropertySet objectValue;


    // navigators
    public NavigatorElement<T> baseElement;
    public NavigatorElement<T> objectElement;
    public NavigatorElement<T> adminElement;

    public NavigatorWindow baseWindow;

    public TableFactory tableFactory;

    public final StringClass formSIDValueClass = StringClass.get(50);
    public final StringClass formCaptionValueClass = StringClass.get(250);

    public List<LP> lproperties = new ArrayList<LP>();

    // счетчик идентификаторов
    static IDGenerator idGenerator = new DefaultIDGenerator(); // todo [dale]: static? package?

    private T BL;

    public BaseLogicsModule(T BL, Logger logger) {
        this.BL = BL;
        this.logger = logger;
    }

    @Override
    public void initClasses() {
        baseClass = addBaseClass("object", "Объект");

        transaction = addAbstractClass("transaction", "Транзакция", baseClass);
        barcodeObject = addAbstractClass("barcodeObject", "Штрих-кодированный объект", baseClass);

        emailObject = addAbstractClass("emailObject", "Объект с почтовым ящиком", baseClass);

        user = addAbstractClass("user", "Пользователь", baseClass);
        customUser = addConcreteClass("customUser", "Обычный пользователь", user, barcodeObject, emailObject);
        systemUser = addConcreteClass("systemUser", "Системный пользователь", user);
        computer = addConcreteClass("computer", "Рабочее место", baseClass);
        userRole = addConcreteClass("userRole", "Роль", baseClass.named);

        policy = addConcreteClass("policy", "Политика безопасности", baseClass.named);
        session = addConcreteClass("session", "Транзакция", baseClass);

        connection = addConcreteClass("connection", "Подключение", baseClass);
        connectionStatus = addStaticClass("connectionStatus", "Статус подключения",
                new String[]{"connectedConnection", "disconnectedConnection"},
                new String[]{"Подключён", "Отключён"});

        country = addConcreteClass("country", "Страна", baseClass.named);

        navigatorElement = addConcreteClass("navigatorElement", "Элемент навигатора", baseClass);
        form = addConcreteClass("form", "Форма", navigatorElement);
        dictionary = addConcreteClass("dictionary", "Словарь", baseClass.named);
        dictionaryEntry = addConcreteClass("dictionaryEntry", "Слова", baseClass);
    }

    @Override
    public void initGroups() {
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

        recognizeGroup = new AbstractGroup("Идентифицирующие свойства");
        recognizeGroup.createContainer = false;
        baseGroup.add(recognizeGroup);
    }

    @Override
    public void initTables() {
        tableFactory = new TableFactory();
        for (int i = 0; i < TableFactory.MAX_INTERFACE; i++) { // заполним базовые таблицы
            CustomClass[] baseClasses = new CustomClass[i];
            for (int j = 0; j < i; j++)
                baseClasses[j] = baseClass;
            tableFactory.include("base_" + i, baseClasses);
        }

        tableFactory.include("customUser", customUser);
        tableFactory.include("loginSID", StringClass.get(30), StringClass.get(30));
        tableFactory.include("countryDate", country, DateClass.instance);
    }

    @Override
    public void initProperties() {
        selection = new SelectionPropertySet();
        sessionGroup.add(selection);

        objectValue = new ObjectValuePropertySet();
        baseGroup.add(objectValue);

        compositeName = new CompositeNamePropertySet();
        privateGroup.add(compositeName);

        classSID = addDProp("classSID", "Стат. код", StringClass.get(250), baseClass.sidClass);
        dataName = addDProp("name", "Имя", InsensitiveStringClass.get(110), baseClass.named);

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
        lsoeq2 = addCFProp(Compare.LESS_EQUALS);
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

        actionTrue = addCProp("ActionTrue", ActionClass.instance, true);

        dumb1 = dumb(1);
        dumb2 = dumb(2);

        castText = addSFProp("CAST((prm1) as text)", TextClass.instance, 1);
        addText2 = addSFProp("((prm1)+(prm2))", TextClass.instance, 2);

        positive = addJProp(greater2, 1, vzero);
        negative = addJProp(less2, 1, vzero);

        yearInDate = addSFProp("(extract(year from prm1))", IntegerClass.instance, 1);

        delete = addAProp(new DeleteObjectActionProperty(genSID(), baseClass));

        date = addDProp(baseGroup, "date", "Дата", DateClass.instance, transaction);

        betweenDates = addJProp("Дата док. между", between, object(DateClass.instance), 1, object(DateClass.instance), 2, object(DateClass.instance), 3);
        betweenDate = addJProp("Дата док. между", betweenDates, date, 1, 2, 3);

        sidCountry = addDProp(baseGroup, "sidCountry", "Код страны", IntegerClass.instance, country);
        generateDatesCountry = addDProp(privateGroup, "generateDatesCountry", "Генерировать выходные", LogicalClass.instance, country);
        sidToCountry = addAGProp("sidToCountry", "Страна", sidCountry);
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
        currentMinute = addTProp(Time.MINUTE);
        currentEpoch = addTProp(Time.EPOCH);
        currentDateTime = addTProp(Time.DATETIME);
        currentUser = addProperty(null, new LP<PropertyInterface>(new CurrentUserFormulaProperty(genSID(), user)));
        currentSession = addProperty(null, new LP<PropertyInterface>(new CurrentSessionFormulaProperty(genSID(), session)));
        currentComputer = addProperty(null, new LP<PropertyInterface>(new CurrentComputerFormulaProperty(genSID(), computer)));
        isServerRestarting = addProperty(null, new LP<PropertyInterface>(new IsServerRestartingFormulaProperty(genSID())));
        changeUser = addProperty(null, new LP<ClassPropertyInterface>(new ChangeUserActionProperty(genSID(), customUser)));

        userLogin = addDProp(baseGroup, "userLogin", "Логин", StringClass.get(30), customUser);
        loginToUser = addAGProp("loginToUser", "Пользователь", userLogin);
        userPassword = addDProp(baseGroup, "userPassword", "Пароль", StringClass.get(30), customUser);
        userFirstName = addDProp(baseGroup, "userFirstName", "Имя", StringClass.get(30), customUser);
        userLastName = addDProp(baseGroup, "userLastName", "Фамилия", StringClass.get(30), customUser);

        userRoleSID = addDProp(baseGroup, "userRoleSID", "Идентификатор", StringClass.get(30), userRole);
        sidToRole = addAGProp(idGroup, "sidToRole", "Роль (ИД)", userRole, userRoleSID);
        inUserRole = addDProp(baseGroup, "inUserRole", "Вкл.", LogicalClass.instance, customUser, userRole);
        userRoleDefaultForms = addDProp(baseGroup, "userRoleDefaultForms", "Отображение форм по умолчанию", LogicalClass.instance, userRole);
        inLoginSID = addJProp("inLoginSID", true, "Логину назначена роль", inUserRole, loginToUser, 1, sidToRole, 2);

        email = addDProp(baseGroup, "email", "E-mail", StringClass.get(50), emailObject);
        emailToObject = addAGProp("emailToObject", "Объект по e-mail", email);

        emailUserPassUser = addEAProp("Напоминание пароля (Password reminder)", customUser);
        addEARecepient(emailUserPassUser, email, 1);

        generateLoginPassword = addAProp(actionGroup, new GenerateLoginPasswordActionProperty(email, userLogin, userPassword, customUser));

        name = addCUProp(recognizeGroup, "commonName", "Имя", dataName,
                addJProp(insensitiveString2, userFirstName, 1, userLastName, 1));

        connectionComputer = addDProp("connectionComputer", "Компьютер", computer, connection);
        addJProp(baseGroup, "Компьютер", hostname, connectionComputer, 1);
        connectionUser = addDProp("connectionUser", "Пользователь", customUser, connection);
        addJProp(baseGroup, "Пользователь", userLogin, connectionUser, 1);
        connectionCurrentStatus = addDProp("connectionCurrentStatus", "Статус подключения", connectionStatus, connection);
        addJProp(baseGroup, "Статус подключения", name, connectionCurrentStatus, 1);

        connectionConnectTime = addDProp(baseGroup, "connectionConnectTime", "Время подключения", DateTimeClass.instance, connection);
        connectionDisconnectTime = addDProp(baseGroup, "connectionDisconnectTime", "Время отключения", DateTimeClass.instance, connection);
        connectionDisconnectTime.setDerivedForcedChange(currentDateTime,
                addJProp(equals2, connectionCurrentStatus, 1, addCProp(connectionStatus, "disconnectedConnection")), 1);

        connectionFormCount = addDProp(baseGroup, "connectionFormCount", "Количество открытых форм", IntegerClass.instance, connection, navigatorElement);

        userMainRole = addDProp(idGroup, "userMainRole", "Главная роль (ИД)", userRole, user);
        nameUserMainRole = addJProp(baseGroup, "nameUserMainRole", "Главная роль", name, userMainRole, 1);

        nameToCountry = addAGProp("nameToCountry", "Страна", country, name);

        nameToPolicy = addAGProp("nameToPolicy", "Политика", policy, name);
        policyDescription = addDProp(baseGroup, "description", "Описание", StringClass.get(100), policy);

        userRolePolicyOrder = addDProp(baseGroup, "userRolePolicyOrder", "Порядок политики", IntegerClass.instance, userRole, policy);
        userPolicyOrder = addJProp(baseGroup, "userPolicyOrder", "Порядок политики", userRolePolicyOrder, userMainRole, 1, 2);

        barcode = addDProp(recognizeGroup, "barcode", "Штрих-код", StringClass.get(13), barcodeObject);

        barcode.setFixedCharWidth(13);
        barcodeToObject = addAGProp("barcodeToObject", "Объект", barcode);
        barcodeObjectName = addJProp(baseGroup, "barcodeObjectName", "Объект", name, barcodeToObject, 1);

        barcodePrefix = addDProp(baseGroup, "barcodePrefix", "Префикс штрих-кодов", StringClass.get(13));

        seekBarcodeAction = addJProp(true, "Поиск штрих-кода", addSAProp(null), barcodeToObject, 1);
        barcodeNotFoundMessage = addJProp(true, "", and(false, true), addMAProp("Штрих-код не найден!", "Ошибка"), is(StringClass.get(13)), 1, barcodeToObject, 1);

        restartServerAction = addJProp("Остановить сервер", andNot1, addRestartActionProp(), isServerRestarting);
        cancelRestartServerAction = addJProp("Отменить остановку сервера", and1, addCancelRestartActionProp(), isServerRestarting);

        recalculateAction = addProperty(null, new LP<ClassPropertyInterface>(new RecalculateActionProperty(genSID(), "Перерасчитать агрегации")));
        packAction = addProperty(null, new LP<ClassPropertyInterface>(new PackActionProperty(genSID(), "Упаковать таблицы")));

        currentUserName = addJProp("Имя тек. польз.", name, currentUser);

        reverseBarcode = addSDProp("reverseBarcode", "Реверс", LogicalClass.instance);

        objectClass = addProperty(null, new LP<ClassPropertyInterface>(new ObjectClassProperty(genSID(), baseClass)));
        objectClassName = addJProp(baseGroup, "objectClassName", "Класс объекта", name, objectClass, 1);

        navigatorElementSID = addDProp(baseGroup, "navigatorElementSID", "Код формы", formSIDValueClass, navigatorElement);
        navigatorElementCaption = addDProp(baseGroup, "navigatorElementCaption", "Название формы", formCaptionValueClass, navigatorElement);
        SIDToNavigatorElement = addAGProp("SIDToNavigatorElement", "Форма", navigatorElementSID);

        permissionUserRoleForm = addDProp(baseGroup, "permissionUserRoleForm", "Запретить форму", LogicalClass.instance, userRole, navigatorElement);
        permissionUserForm = addJProp(baseGroup, "permissionUserForm", "Запретить форму", permissionUserRoleForm, userMainRole, 1, 2);
        userRoleFormDefaultNumber = addDProp(baseGroup, "userRoleFormDefaultNumber", "Номер по умолчанию", IntegerClass.instance, userRole, navigatorElement);
        userFormDefaultNumber = addJProp(baseGroup, "userFormDefaultNumber", "Номер по умолчанию", userRoleFormDefaultNumber, userMainRole, 1, 2);
        userDefaultForms = addJProp(baseGroup, "userDefaultForms", "Отображение форм по умолчанию", userRoleDefaultForms, userMainRole, 1);
//        permissionUserForm = addDProp(baseGroup, "permissionUserForm", "Запретить форму", LogicalClass.instance, user, navigatorElement);

        selectUserRoles = addSelectFromListAction(null, "Редактировать роли", inUserRole, userRole, customUser);
        //selectRoleForms = addSelectFromListAction(null, "Редактировать формы", permissionUserRoleForm, navigatorElement, userRole);

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
        emailBlindCarbonCopy = addDProp("emailBlindCarbonCopy", "Копия (BCC)", StringClass.get(50));
        fromAddress = addDProp("fromAddress", "Адрес отправителя", StringClass.get(50));

        disableEmail = addDProp("disableEmail", "Отключить отсылку почты", LogicalClass.instance);

        defaultCountry = addDProp("defaultCountry", "Страна по умолчанию", country);

        entryDictionary = addDProp("entryDictionary", "Словарь", dictionary, dictionaryEntry);
        termDictionary = addDProp(baseGroup, "termDictionary", "Термин", StringClass.get(50), dictionaryEntry);
        translationDictionary = addDProp(baseGroup, "translationDictionary", "Перевод", StringClass.get(50), dictionaryEntry);
        translationDictionaryTerm = addCGProp(null, "translationDictionayTerm", "Перевод", translationDictionary, termDictionary, entryDictionary, 1, termDictionary, 1);
    }

    @Override
    public void initIndexes() {
        addIndex(barcode);
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
                    valueClasses[i] = findValueClass(sids[i]);
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
                ValueClass valueClass = findValueClass(sid.substring(prefix.length()));
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

    @Override
    public void initNavigators() {
        baseClass.named.setClassForm(new NamedObjectClassForm(BL, baseClass.named));

        baseElement = new NavigatorElement("baseElement", "Формы");
        baseWindow = new NavigatorWindow(baseElement.getSID() + "Window", "Навигатор", 0, 0, 20, 70);
        baseElement.window = baseWindow;

        objectElement = baseClass.getBaseClassForm(BL);
        baseElement.add(objectElement);

        adminElement = new NavigatorElement(baseElement, "adminElement", "Администрирование");
        NavigatorElement policyElement = new NavigatorElement(adminElement, "policyElement", "Политика безопасности");
        addFormEntity(new UserPolicyFormEntity(policyElement, "userPolicyForm"));
        addFormEntity(new RolePolicyFormEntity(policyElement, "rolePolicyForm"));
        addFormEntity(new ConnectionsFormEntity(adminElement, "connectionsForm"));
        addFormEntity(new AdminFormEntity(adminElement, "adminForm"));
        addFormEntity(new DaysOffFormEntity(adminElement, "daysOffForm"));
        addFormEntity(new DictionariesFormEntity(adminElement, "dictionariesForm"));

        addFormEntity(new RemindUserPassFormEntity(adminElement, "remindPasswordLetter"));
    }



    protected Map<String, CustomClass> sidToClass = new HashMap<String, CustomClass>();

    protected void storeCustomClass(CustomClass customClass) {
        assert !sidToClass.containsKey(customClass.getSID());
        sidToClass.put(customClass.getSID(), customClass);
    }

    protected ConcreteCustomClass addConcreteClass(AbstractGroup group, String sID, String caption, CustomClass... parents) {
        ConcreteCustomClass customClass = new ConcreteCustomClass(sID, caption, parents);
        group.add(customClass);
        storeCustomClass(customClass);
        return customClass;
    }

    protected BaseClass addBaseClass(String sID, String caption) {
        BaseClass baseClass = new BaseClass(sID, caption);
        storeCustomClass(baseClass);
        storeCustomClass(baseClass.named);
        return baseClass;
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

    public ConcreteCustomClass addConcreteClass(String sID, String caption, CustomClass... parents) {
        return addConcreteClass(baseGroup, sID, caption, parents);
    }

    public AbstractCustomClass addAbstractClass(String sID, String caption, CustomClass... parents) {
        return addAbstractClass(baseGroup, sID, caption, parents);
    }

    public StaticCustomClass addStaticClass(String sID, String caption, String[] sids, String[] names) {
        StaticCustomClass customClass = new StaticCustomClass(sID, caption, baseClass.sidClass, sids, names);
        storeCustomClass(customClass);
        customClass.dialogReadOnly = true;
        return customClass;
    }



    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////
    /// Properties
    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////

    private Set<String> idSet = new HashSet<String>();

    public String genSID() {
        String id = "property" + idSet.size();
        idSet.add(id);
        return id;
    }

    public LP addDProp(String sID, String caption, ValueClass value, ValueClass... params) {
        return addDProp(null, sID, false, caption, value, params);
    }

    public LP addDProp(AbstractGroup group, String sID, String caption, ValueClass value, ValueClass... params) {
        return addDProp(group, sID, false, caption, value, params);
    }

    public LP[] addDProp(AbstractGroup group, String paramID, String[] sIDs, String[] captions, ValueClass[] values, ValueClass... params) {
        LP[] result = new LP[sIDs.length];
        for (int i = 0; i < sIDs.length; i++)
            result[i] = addDProp(group, sIDs[i] + paramID, captions[i], values[i], params);
        return result;
    }

    public LP addDProp(AbstractGroup group, String sID, boolean persistent, String caption, ValueClass value, ValueClass... params) {
        StoredDataProperty dataProperty = new StoredDataProperty(sID, caption, params, value);
        LP lp = addProperty(group, persistent, new LP<ClassPropertyInterface>(dataProperty));
        dataProperty.markStored(tableFactory);
        return lp;
    }

    public LP addGDProp(AbstractGroup group, String paramID, String sID, String caption, ValueClass[] values, CustomClass[]... params) {
        CustomClass[][] listParams = new CustomClass[params[0].length][]; //
        for (int i = 0; i < listParams.length; i++) {
            listParams[i] = new CustomClass[params.length];
            for (int j = 0; j < params.length; j++)
                listParams[i][j] = params[j][i];
        }
        params = listParams;

        LP[] genProps = new LP[params.length];
        for (int i = 0; i < params.length; i++) {
            String genID = "";
            String genCaption = "";
            for (int j = 0; j < params[i].length; j++) {
                genID += params[i][j].getSID();
                genCaption = (genCaption.length() == 0 ? "" : genCaption) + params[i][j].caption;
            }
            genProps[i] = addDProp(sID + genID, caption + " (" + genCaption + ")", values[i], params[i]);
        }

        return addCUProp(group, sID + paramID, caption, genProps);
    }

    protected LP[] addGDProp(AbstractGroup group, String paramID, String[] sIDs, String[] captions, ValueClass[][] values, CustomClass[]... params) {
        LP[] result = new LP[values.length];
        for (int i = 0; i < values.length; i++)
            result[i] = addGDProp(group, paramID, sIDs[i], captions[i], values[i], params);
        return result;
    }

    public <D extends PropertyInterface> LP addDCProp(String sID, String caption, LP<D> derivedProp, Object... params) {
        return addDCProp(null, sID, caption, derivedProp, params);
    }

    protected <D extends PropertyInterface> LP addDCProp(String sID, boolean persistent, String caption, LP<D> derivedProp, Object... params) {
        return addDCProp(null, sID, persistent, caption, false, derivedProp, params);
    }

    public <D extends PropertyInterface> LP addDCProp(AbstractGroup group, String sID, String caption, LP<D> derivedProp, Object... params) {
        return addDCProp(group, sID, false, caption, false, derivedProp, params);
    }

    public <D extends PropertyInterface> LP addDCProp(String sID, String caption, boolean forced, LP<D> derivedProp, Object... params) {
        return addDCProp(null, sID, caption, forced, derivedProp, params);
    }

    public <D extends PropertyInterface> LP addDCProp(AbstractGroup group, String sID, String caption, boolean forced, LP<D> derivedProp, Object... params) {
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
        List<PropertyUtils.LI> list = readLI(params);

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

        joinProperty.implement = new PropertyImplement<AndFormulaProperty.Interface, PropertyInterfaceImplement<JoinProperty.Interface>>(andProperty, mapImplement);

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

    public LP addSDProp(String caption, ValueClass value, ValueClass... params) {
        return addSDProp((AbstractGroup) null, caption, value, params);
    }

    protected LP addSDProp(AbstractGroup group, String caption, ValueClass value, ValueClass... params) {
        return addSDProp(group, genSID(), caption, value, params);
    }

    public LP addSDProp(String sID, String caption, ValueClass value, ValueClass... params) {
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

    public LP addFAProp(String caption, FormEntity form, ObjectEntity... params) {
        return addFAProp(null, caption, form, params, new PropertyObjectEntity[0], new PropertyObjectEntity[0], false, false);
    }

    protected LP addFAProp(AbstractGroup group, FormEntity form, ObjectEntity... params) {
        return addFAProp(group, form.caption, form, params);
    }

    public LP addFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity... params) {
        return addFAProp(group, caption, form, params, new PropertyObjectEntity[0], new PropertyObjectEntity[0], false, false);
    }

    public LP addMFAProp(String caption, FormEntity form, ObjectEntity... params) {
        return addMFAProp(null, caption, form, params, new PropertyObjectEntity[0], new PropertyObjectEntity[0]);
    }

    public LP addMFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, PropertyObjectEntity... setProperties) {
        return addMFAProp(group, caption, form, objectsToSet, false, setProperties);
    }

    public LP addMFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, boolean newSession, PropertyObjectEntity... setProperties) {
        // во все setProperties просто будут записаны null'ы
        return addMFAProp(group, caption, form, objectsToSet, setProperties, new PropertyObjectEntity[setProperties.length], newSession);
    }

    protected LP addMFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, PropertyObjectEntity[] setProperties, PropertyObjectEntity[] getProperties) {
        return addMFAProp(group, caption, form, objectsToSet, setProperties, getProperties, true);
    }

    public LP addMFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, PropertyObjectEntity[] setProperties, PropertyObjectEntity[] getProperties, boolean newSession) {
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

    public LP addStopActionProp(String caption, String header) {
        return addAProp(new StopActionProperty(genSID(), caption, header));
    }

    public LP addEAProp(ValueClass... params) {
        return addEAProp(null, params);
    }

    protected LP addEAProp(String subject, ValueClass... params) {
        return addEAProp(null, genSID(), "email", subject, params);
    }

    protected LP addEAProp(AbstractGroup group, String sID, String caption, String subject, ValueClass... params) {
        return addProperty(group, new LP<ClassPropertyInterface>(new EmailActionProperty(sID, caption, subject, BL, params)));
    }

    public <X extends PropertyInterface> void addEARecepient(LP<ClassPropertyInterface> eaProp, LP<X> emailProp, Integer... params) {
        Map<X, ClassPropertyInterface> mapInterfaces = new HashMap<X, ClassPropertyInterface>();
        for (int i = 0; i < emailProp.listInterfaces.size(); i++)
            mapInterfaces.put(emailProp.listInterfaces.get(i), eaProp.listInterfaces.get(params[i] - 1));
        ((EmailActionProperty) eaProp.property).addRecepient(new PropertyMapImplement<X, ClassPropertyInterface>(emailProp.property, mapInterfaces));
    }

    public void addInlineEAForm(LP<ClassPropertyInterface> eaProp, FormEntity form, Object... params) {
        Map<ObjectEntity, ClassPropertyInterface> mapObjects = new HashMap<ObjectEntity, ClassPropertyInterface>();
        for (int i = 0; i < params.length / 2; i++)
            mapObjects.put((ObjectEntity) params[2 * i], eaProp.listInterfaces.get((Integer) params[2 * i + 1] - 1));
        ((EmailActionProperty) eaProp.property).addInlineForm(form, mapObjects);
    }

    public void addAttachEAForm(LP<ClassPropertyInterface> eaProp, FormEntity form, EmailActionProperty.Format format, Object... params) {
        LP attachmentName = null;
        if (params.length > 0 && params[0] instanceof LP) {
            attachmentName = (LP) params[0];
            params = Arrays.copyOfRange(params, 1, params.length);
        }
        Map<ObjectEntity, ClassPropertyInterface> mapObjects = new HashMap<ObjectEntity, ClassPropertyInterface>();
        for (int i = 0; i < params.length / 2; i++)
            mapObjects.put((ObjectEntity) params[2 * i], eaProp.listInterfaces.get((Integer) params[2 * i + 1] - 1));
        ((EmailActionProperty) eaProp.property).addAttachmentForm(form, format, mapObjects, attachmentName);
    }

    public LP addTAProp(LP sourceProperty, LP targetProperty) {
        return addProperty(null, new LP<ClassPropertyInterface>(new TranslateActionProperty(genSID(), "translate", translationDictionaryTerm, sourceProperty, targetProperty, dictionary)));
    }

    public <P extends PropertyInterface> LP addSCProp(LP<P> lp) {
        return addSCProp(privateGroup, "sys", lp);
    }

    protected <P extends PropertyInterface> LP addSCProp(AbstractGroup group, String caption, LP<P> lp) {
        return addProperty(group, new LP<ShiftChangeProperty.Interface<P>>(new ShiftChangeProperty<P, PropertyInterface>(genSID(), caption, lp.property, new PropertyMapImplement<PropertyInterface, P>(reverseBarcode.property))));
    }

    // добавляет свойство с бесконечным значением
    public LP addICProp(DataClass valueClass, ValueClass... params) {
        return addProperty(privateGroup, false, new LP<ClassPropertyInterface>(new InfiniteClassProperty(genSID(), "Беск.", params, valueClass)));
    }

    public LP addCProp(StaticClass valueClass, Object value, ValueClass... params) {
        return addCProp("sys", valueClass, value, params);
    }

    public LP addCProp(String caption, StaticClass valueClass, Object value, ValueClass... params) {
        return addCProp(null, false, caption, valueClass, value, params);
    }

    public LP addCProp(AbstractGroup group, boolean persistent, String caption, StaticClass valueClass, Object value, ValueClass... params) {
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

    public <P extends PropertyInterface> LP addTCProp(Time time, String sID, String caption, LP<P> changeProp, ValueClass... classes) {
        return addTCProp(null, time, sID, caption, changeProp, classes);
    }

    public <P extends PropertyInterface> LP addTCProp(Time time, String sID, boolean isStored, String caption, LP<P> changeProp, ValueClass... classes) {
        return addTCProp(null, time, sID, isStored, caption, changeProp, classes);
    }

    protected <P extends PropertyInterface> LP addTCProp(AbstractGroup group, Time time, String sID, String caption, LP<P> changeProp, ValueClass... classes) {
        return addTCProp(group, time, sID, false, caption, changeProp, classes);
    }

    protected <P extends PropertyInterface> LP addTCProp(AbstractGroup group, Time time, String sID, boolean isStored, String caption, LP<P> changeProp, ValueClass... classes) {
        TimePropertyChange<P> timeProperty = new TimePropertyChange<P>(isStored, time, sID, caption, overrideClasses(changeProp.getMapClasses(), classes), changeProp.listInterfaces);

        changeProp.property.timeChanges.put(time, timeProperty);

        if (isStored) {
            timeProperty.property.markStored(tableFactory);
        }

        return addProperty(group, false, new LP<ClassPropertyInterface>(timeProperty.property));
    }

    public LP addSFProp(String formula, ConcreteValueClass value, int paramCount) {
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

    public LP addMFProp(ConcreteValueClass value, int paramCount) {
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

    public LP addJProp(LP mainProp, Object... params) {
        return addJProp(privateGroup, "sys", mainProp, params);
    }

    public LP addJProp(String caption, LP mainProp, Object... params) {
        return addJProp((AbstractGroup) null, caption, mainProp, params);
    }

    public LP addJProp(String sID, String caption, LP mainProp, Object... params) {
        return addJProp(sID, false, caption, mainProp, params);
    }

    public LP addJProp(String sID, boolean persistent, String caption, LP mainProp, Object... params) {
        return addJProp(null, sID, persistent, caption, mainProp, params);
    }

    public LP addJProp(AbstractGroup group, String caption, LP mainProp, Object... params) {
        return addJProp(group, genSID(), caption, mainProp, params);
    }

    public LP addJProp(boolean implementChange, String caption, LP mainProp, Object... params) {
        return addJProp((AbstractGroup) null, implementChange, caption, mainProp, params);
    }

    public LP addJProp(boolean implementChange, String sID, String caption, LP mainProp, Object... params) {
        return addJProp((AbstractGroup) null, implementChange, sID, caption, mainProp, params);
    }

    public LP addJProp(AbstractGroup group, boolean implementChange, String caption, LP mainProp, Object... params) {
        return addJProp(group, implementChange, genSID(), caption, mainProp, params);
    }

    public LP addJProp(AbstractGroup group, String sID, String caption, LP mainProp, Object... params) {
        return addJProp(group, mainProp.property instanceof ActionProperty, sID, caption, mainProp, params);
    }

    public LP addJProp(AbstractGroup group, String sID, boolean persistent, String caption, LP mainProp, Object... params) {
        return addJProp(group, mainProp.property instanceof ActionProperty, sID, persistent, caption, mainProp, params);
    }

    public LP addJProp(boolean implementChange, LP mainProp, Object... params) {
        return addJProp(privateGroup, implementChange, genSID(), "sys", mainProp, params);
    }

    public LP addJProp(AbstractGroup group, boolean implementChange, String sID, String caption, LP mainProp, Object... params) {
        return addJProp(group, implementChange, sID, false, caption, mainProp, params);
    }

    public LP addJProp(AbstractGroup group, boolean implementChange, String sID, boolean persistent, String caption, LP mainProp, Object... params) {

        JoinProperty<?> property = new JoinProperty(sID, caption, getIntNum(params), implementChange);
        property.inheritFixedCharWidth(mainProp.property);

        LP listProperty = new LP<JoinProperty.Interface>(property);
        property.implement = mapImplement(mainProp, readImplements(listProperty.listInterfaces, params));

        return addProperty(group, persistent, listProperty);
    }

    public LP[] addJProp(AbstractGroup group, boolean implementChange, String paramID, LP[] props, String caption, Object... params) {
        LP[] result = new LP[props.length];
        for (int i = 0; i < props.length; i++)
            result[i] = addJProp(group, implementChange, props[i].property.sID + paramID, props[i].property.caption + (caption.length() == 0 ? "" : (" " + caption)), props[i], params);
        return result;
    }

    public LP[] addJProp(AbstractGroup group, boolean implementChange, String paramID, LP[] props, Object... params) {
        return addJProp(group, implementChange, paramID, props, "", params);
    }

    /**
     * Создаёт свойство для группового изменения, при этом итерация идёт по всем интерфейсам, и мэппинг интерфейсов происходит по порядку
     */
    public LP addGCAProp(AbstractGroup group, String sID, String caption, GroupObjectEntity groupObject, LP mainProperty, LP getterProperty) {
        int groupInts[] = consecutiveInts(mainProperty.listInterfaces.size());
        int getterInts[] = consecutiveInts(getterProperty.listInterfaces.size());

        return addGCAProp(group, sID, caption, groupObject, mainProperty, groupInts, getterProperty, getterInts);
    }

    /**
     * Создаёт свойство для группового изменения
     * Пример:
     * <pre>
     *   LP ценаПоставкиТовара = Свойство(Товар)
     *   LP ценаПродажиТовара = Свойство(Магазин, Товар)
     *
     *   Тогда, чтобы установить цену для всех товаров в магазине, равной цене поставки товара, создаём свойство
     *
     *   addGCAProp(..., ценаПродажиТовара, 2, ценаПоставкиТовара, 1)
     * </pre>
     *
     * @param groupObject используется для получения фильтров на набор, для которого будут происходить изменения
     * @param params сначала идут номера интерфейсов для группировки, затем getterProperty, затем мэппинг интерфейсов getterProperty
     */
    protected LP addGCAProp(AbstractGroup group, String sID, String caption, GroupObjectEntity groupObject, LP mainProperty, Object... params) {
        assert params.length > 0;

        List<Integer> groupInts = new ArrayList<Integer>();
        int i = 0;
        while (!(params[i] instanceof LP)) {
            groupInts.add((Integer) params[i++] - 1);
        }

        LP getterProperty = (LP) params[i++];

        List<Integer> getterInts = new ArrayList<Integer>();
        while (i < params.length) {
            getterInts.add((Integer) params[i++] - 1);
        }

        return addGCAProp(group, sID, caption, groupObject, mainProperty, toPrimitive(groupInts), getterProperty, toPrimitive(getterInts));
    }

    private LP addGCAProp(AbstractGroup group, String sID, String caption, GroupObjectEntity groupObject, LP mainProperty, int[] groupInts, LP getterProperty, int[] getterInts) {
        return addProperty(group, new LP<ClassPropertyInterface>(
                new GroupChangeActionProperty(sID, caption, groupObject,
                        mainProperty, groupInts,
                        getterProperty, getterInts)));
    }

    private <T extends PropertyInterface> LP addGProp(AbstractGroup group, String sID, boolean persistent, String caption, LP<T> groupProp, List<PropertyInterfaceImplement<T>> listImplements) {
        GroupProperty<T> property = new SumGroupProperty<T>(sID, caption, listImplements, groupProp.property);
        return mapLGProp(group, persistent, property, listImplements);
    }

    private <P extends PropertyInterface, L extends PropertyInterface> LP mapLProp(AbstractGroup group, boolean persistent, PropertyMapImplement<L, P> implement, LP<P> property) {
        return addProperty(group, persistent, new LP<L>(implement.property, BaseUtils.mapList(property.listInterfaces, BaseUtils.reverse(implement.mapping))));
    }

    private <P extends PropertyInterface, L extends PropertyInterface> LP mapLGProp(AbstractGroup group, PropertyImplement<L, PropertyInterfaceImplement<P>> implement, List<PropertyInterfaceImplement<P>> listImplements) {
        return mapLGProp(group, false, implement, listImplements);
    }

    private <P extends PropertyInterface, L extends PropertyInterface> LP mapLGProp(AbstractGroup group, boolean persistent, PropertyImplement<L, PropertyInterfaceImplement<P>> implement, List<PropertyInterfaceImplement<P>> listImplements) {
        return addProperty(group, persistent, new LP<L>(implement.property, BaseUtils.mapList(listImplements, BaseUtils.reverse(implement.mapping))));
    }

    private <P extends PropertyInterface> LP mapLGProp(AbstractGroup group, boolean persistent, GroupProperty<P> property, List<PropertyInterfaceImplement<P>> listImplements) {
        return mapLGProp(group, persistent, new PropertyImplement<GroupProperty.Interface<P>, PropertyInterfaceImplement<P>>(property, property.getMapInterfaces()), listImplements);
    }

    protected <P extends PropertyInterface> LP addOProp(String caption, OrderType orderType, LP<P> sum, boolean ascending, boolean includeLast, int partNum, Object... params) {
        return addOProp(genSID(), caption, orderType, sum, ascending, includeLast, partNum, params);
    }

    public <P extends PropertyInterface> LP addOProp(String sID, String caption, OrderType orderType, LP<P> sum, boolean ascending, boolean includeLast, int partNum, Object... params) {
        return addOProp((AbstractGroup) null, sID, caption, orderType, sum, ascending, includeLast, partNum, params);
    }

    public <P extends PropertyInterface> LP addOProp(AbstractGroup group, String caption, OrderType orderType, LP<P> sum, boolean ascending, boolean includeLast, int partNum, Object... params) {
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
        return mapLProp(group, persistent, DerivedProperty.createUGProp(sID, caption, new PropertyImplement<L, PropertyInterfaceImplement<R>>(ungroup.property, groupImplement), orders, restriction.property), restriction);
    }

    public <R extends PropertyInterface, L extends PropertyInterface> LP addPGProp(AbstractGroup group, String sID, boolean persistent, int roundlen, boolean roundfirst, String caption, LP<R> proportion, LP<L> ungroup, Object... params) {
        List<LI> li = readLI(params);

        Map<L, PropertyInterfaceImplement<R>> groupImplement = new HashMap<L, PropertyInterfaceImplement<R>>();
        for (int i = 0; i < ungroup.listInterfaces.size(); i++)
            groupImplement.put(ungroup.listInterfaces.get(i), li.get(i).map(proportion.listInterfaces));
        return mapLProp(group, persistent, DerivedProperty.createPGProp(sID, caption, roundlen, roundfirst, baseClass, new PropertyImplement<L, PropertyInterfaceImplement<R>>(ungroup.property, groupImplement), proportion.property), proportion);
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

    public LP addSGProp(LP groupProp, Object... params) {
        return addSGProp(privateGroup, "sys", groupProp, params);
    }

    public LP addSGProp(String caption, LP groupProp, Object... params) {
        return addSGProp((AbstractGroup) null, caption, groupProp, params);
    }

    public LP addSGProp(AbstractGroup group, String caption, LP groupProp, Object... params) {
        return addSGProp(group, genSID(), caption, groupProp, params);
    }

    public LP addSGProp(String sID, String caption, LP groupProp, Object... params) {
        return addSGProp(sID, false, caption, groupProp, params);
    }

    public LP addSGProp(String sID, boolean persistent, String caption, LP groupProp, Object... params) {
        return addSGProp(null, sID, persistent, caption, groupProp, params);
    }

    public LP addSGProp(AbstractGroup group, String sID, String caption, LP groupProp, Object... params) {
        return addSGProp(group, sID, false, caption, groupProp, params);
    }

    public LP addSGProp(AbstractGroup group, String sID, boolean persistent, String caption, LP groupProp, Object... params) {
        return addSGProp(group, sID, persistent, false, caption, groupProp, params);
    }

    public <T extends PropertyInterface> LP addSGProp(AbstractGroup group, String sID, boolean persistent, boolean notZero, String caption, LP<T> groupProp, Object... params) {
        return addSGProp(group, sID, persistent, notZero, caption, groupProp, readImplements(groupProp.listInterfaces, params));
    }

    private <T extends PropertyInterface> LP addSGProp(AbstractGroup group, String sID, boolean persistent, boolean notZero, String caption, LP<T> groupProp, List<PropertyInterfaceImplement<T>> listImplements) {
        boolean wrapNotZero = persistent && (notZero || !Settings.instance.isDisableSumGroupNotZero());
        SumGroupProperty<T> property = new SumGroupProperty<T>(wrapNotZero ? genSID() : sID, caption, listImplements, groupProp.property);

        LP result;
        if (wrapNotZero)
            result = addJProp(group, sID, persistent, caption, onlyNotZero, directLI(mapLGProp(null, false, property, listImplements)));
        else
            result = mapLGProp(group, persistent, property, listImplements);

        result.sumGroup = property; // так как может wrap'ся, использование - setDG
        result.groupProperty = groupProp; // для порядка параметров, использование - setDG

        return result;
    }

    public LP addMGProp(LP groupProp, Object... params) {
        return addMGProp(privateGroup, genSID(), "sys", groupProp, params);
    }

    public LP addMGProp(String sID, String caption, LP groupProp, Object... params) {
        return addMGProp(null, sID, caption, groupProp, params);
    }

    public LP addMGProp(AbstractGroup group, String sID, String caption, LP groupProp, Object... params) {
        return addMGProp(group, sID, false, caption, groupProp, params);
    }

    public LP addMGProp(AbstractGroup group, String sID, boolean persist, String caption, LP groupProp, Object... params) {
        return addMGProp(group, persist, new String[]{sID}, new String[]{caption}, 0, groupProp, params)[0];
    }

    protected <T extends PropertyInterface> LP[] addMGProp(AbstractGroup group, String[] ids, String[] captions, int extra, LP<T> groupProp, Object... params) {
        return addMGProp(group, false, ids, captions, extra, groupProp, params);
    }

    public <T extends PropertyInterface> LP[] addMGProp(AbstractGroup group, boolean persist, String[] ids, String[] captions, int extra, LP<T> groupProp, Object... params) {
        LP[] result = new LP[extra + 1];

        Collection<Property> suggestPersist = new ArrayList<Property>();

        List<PropertyInterfaceImplement<T>> listImplements = readImplements(groupProp.listInterfaces, params);
        List<PropertyInterfaceImplement<T>> groupImplements = listImplements.subList(extra, listImplements.size());
        List<PropertyImplement<?, PropertyInterfaceImplement<T>>> mgProps = DerivedProperty.createMGProp(ids, captions, groupProp.property, baseClass,
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

//    public static <T extends PropertyInterface<T>> AggregateGroupProperty create(String sID, String caption, Property<T> property, T aggrInterface, Collection<PropertyMapImplement<?, T>> groupProps) {

    public LP addAGProp(String sID, String caption, LP... props) {
        return addAGProp(null, sID, caption, props);
    }

    public LP addAGProp(AbstractGroup group, String sID, String caption, LP... props) {
        ClassWhere<Integer> classWhere = ClassWhere.<Integer>STATIC(true);
        for (LP<?> prop : props)
            classWhere = classWhere.and(prop.getClassWhere());
        return addAGProp(group, sID, caption, (CustomClass) BaseUtils.singleValue(classWhere.getCommonParent(Collections.singleton(1))), props);
    }

    public LP addAGProp(String sID, String caption, CustomClass customClass, LP... props) {
        return addAGProp(null, sID, caption, customClass, props);
    }

    protected LP addAGProp(AbstractGroup group, String sID, String caption, CustomClass customClass, LP... props) {
        return addAGProp(group, false, sID, false, caption, customClass, props);
    }

    protected LP addAGProp(AbstractGroup group, boolean checkChange, String sID, boolean persistent, String caption, CustomClass customClass, LP... props) {
        return addAGProp(group, checkChange, sID, persistent, caption, is(customClass), 1, getUParams(props, 0));
    }

    public <T extends PropertyInterface<T>> LP addAGProp(String sID, String caption, LP<T> lp, int aggrInterface, Object... props) {
        return addAGProp(sID, false, caption, lp, aggrInterface, props);
    }

    public <T extends PropertyInterface> LP addAGProp(AbstractGroup group, String sID, String caption, LP<T> lp, int aggrInterface, Object... props) {
        return addAGProp(group, false, sID, false, caption, lp, aggrInterface, props);
    }

    public <T extends PropertyInterface<T>> LP addAGProp(String sID, boolean persistent, String caption, LP<T> lp, int aggrInterface, Object... props) {
        return addAGProp(null, false, sID, persistent, caption, lp, aggrInterface, props);
    }

    public <T extends PropertyInterface<T>> LP addAGProp(AbstractGroup group, String sID, boolean persistent, String caption, LP<T> lp, int aggrInterface, Object... props) {
        return addAGProp(group, false, sID, persistent, caption, lp, aggrInterface, props);
    }

    protected <T extends PropertyInterface<T>> LP addAGProp(AbstractGroup group, boolean checkChange, String sID, boolean persistent, String caption, LP<T> lp, int aggrInterface, Object... props) {
        List<PropertyInterfaceImplement<T>> listImplements = readImplements(lp.listInterfaces, props);

        return addAGProp(group, checkChange, persistent, AggregateGroupProperty.create(sID, caption, lp.property, lp.listInterfaces.get(aggrInterface - 1), (List<PropertyMapImplement<?, T>>) (List<?>) listImplements), BaseUtils.mergeList(listImplements, BaseUtils.removeList(lp.listInterfaces, aggrInterface - 1)));
    }

    // чисто для generics
    private <T extends PropertyInterface<T>, J extends PropertyInterface> LP addAGProp(AbstractGroup group, boolean checkChange, boolean persistent, AggregateGroupProperty<T, J> property, List<PropertyInterfaceImplement<T>> listImplements) {
        // нужно добавить ограничение на уникальность
        addProperty(null, new LP(property.getConstrainedProperty(checkChange)));

        return mapLGProp(group, persistent, property, DerivedProperty.mapImplements(listImplements, property.getMapping()));
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LP addDGProp(int orders, boolean ascending, LP<T> groupProp, Object... params) {
        return addDGProp(privateGroup, "sys", orders, ascending, groupProp, params);
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LP addDGProp(AbstractGroup group, String caption, int orders, boolean ascending, LP<T> groupProp, Object... params) {
        return addDGProp(group, genSID(), caption, orders, ascending, groupProp, params);
    }

    public <T extends PropertyInterface, P extends PropertyInterface> LP addDGProp(AbstractGroup group, String sID, String caption, int orders, boolean ascending, LP<T> groupProp, Object... params) {
        return addDGProp(group, sID, false, caption, orders, ascending, groupProp, params);
    }

    public <T extends PropertyInterface> LP addDGProp(AbstractGroup group, String sID, boolean persistent, String caption, int orders, boolean ascending, LP<T> groupProp, Object... params) {
        List<PropertyInterfaceImplement<T>> listImplements = readImplements(groupProp.listInterfaces, params);
        int intNum = listImplements.size();
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
            case XOR:
                property = new XorUnionProperty(sID, caption, intNum);
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
                case XOR:
                    ((XorUnionProperty) property).operands.add(operand);
                    break;
                case EXCLUSIVE:
                    ((ExclusiveUnionProperty) property).operands.add(operand);
                    break;
            }
        }

        return addProperty(group, persistent, listProperty);
    }

    public LP addCaseUProp(AbstractGroup group, String sID, boolean persistent, String caption, Object... params) {
        List<LI> list = readLI(params);
        int intNum = ((LMI)list.get(1)).lp.listInterfaces.size(); // берем количество интерфейсов у первого case'а

        CaseUnionProperty caseProp = new CaseUnionProperty(sID, caption, intNum);
        LP<UnionProperty.Interface> listProperty = new LP<UnionProperty.Interface>(caseProp);
        List<PropertyMapImplement<?, UnionProperty.Interface>> mapImplements = (List<PropertyMapImplement<?, UnionProperty.Interface>>)(List<?>)mapLI(list, listProperty.listInterfaces);
        for(int i=0;i<mapImplements.size()/2;i++)
            caseProp.addCase(mapImplements.get(2*i), mapImplements.get(2*i+1));
        if(mapImplements.size()%2!=0)
            caseProp.addCase(new PropertyMapImplement<PropertyInterface, UnionProperty.Interface>(vtrue.property), mapImplements.get(mapImplements.size()-1));
        return addProperty(group, persistent, listProperty);
    }

    // объединение классовое (непересекающихся) свойств

    public LP addCUProp(LP... props) {
        return addCUProp(privateGroup, "sys", props);
    }

    public LP addCUProp(String caption, LP... props) {
        return addCUProp((AbstractGroup) null, caption, props);
    }

    public LP addCUProp(AbstractGroup group, String caption, LP... props) {
        return addCUProp(group, genSID(), caption, props);
    }

    public LP addCUProp(String sID, String caption, LP... props) {
        return addCUProp(sID, false, caption, props);
    }

    public LP addCUProp(String sID, boolean persistent, String caption, LP... props) {
        return addCUProp(null, sID, persistent, caption, props);
    }

    Collection<LP[]> checkCUProps = new ArrayList<LP[]>();

    // объединяет разные по классам св-ва

    public LP addCUProp(AbstractGroup group, String sID, String caption, LP... props) {
        return addCUProp(group, sID, false, caption, props);
    }

    public LP addCUProp(AbstractGroup group, String sID, boolean persistent, String caption, LP... props) {
        assert checkCUProps.add(props);
        return addXSUProp(group, sID, persistent, caption, props);
    }

    // разница

    public LP addDUProp(LP prop1, LP prop2) {
        return addDUProp(privateGroup, "sys", prop1, prop2);
    }

    public LP addDUProp(String caption, LP prop1, LP prop2) {
        return addDUProp((AbstractGroup) null, caption, prop1, prop2);
    }

    public LP addDUProp(AbstractGroup group, String caption, LP prop1, LP prop2) {
        return addDUProp(group, genSID(), caption, prop1, prop2);
    }

    public LP addDUProp(String sID, String caption, LP prop1, LP prop2) {
        return addDUProp(null, sID, caption, prop1, prop2);
    }

    public LP addDUProp(String sID, boolean persistent, String caption, LP prop1, LP prop2) {
        return addDUProp(null, sID, persistent, caption, prop1, prop2);
    }

    public LP addDUProp(AbstractGroup group, String sID, String caption, LP prop1, LP prop2) {
        return addDUProp(group, sID, false, caption, prop1, prop2);
    }

    public LP addDUProp(AbstractGroup group, String sID, boolean persistent, String caption, LP prop1, LP prop2) {
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

    public LP addXorUProp(AbstractGroup group, String sID, boolean persistent, String caption, LP... props) {
        return addUProp(group, sID, persistent, caption, Union.XOR, getUParams(props, 0));
//        int intNum = prop1.listInterfaces.size();
//        Object[] params = new Object[2 * (1 + intNum)];
//        params[0] = prop1;
//        for (int i = 0; i < intNum; i++)
//            params[1 + i] = i + 1;
//        params[1 + intNum] = prop2;
//        for (int i = 0; i < intNum; i++)
//            params[2 + intNum + i] = i + 1;
//        return addXSUProp(group, sID, persistent, caption, addJProp(andNot1, getUParams(new LP[]{prop1, prop2}, 0)), addJProp(andNot1, getUParams(new LP[]{prop2, prop1}, 0)));
    }

    // IF и IF ELSE

    public LP addIfProp(LP prop, boolean not, LP ifProp, Object... params) {
        return addIfProp(privateGroup, genSID(), "sys", prop, not, ifProp, params);
    }

    protected LP addIfProp(AbstractGroup group, String sID, String caption, LP prop, boolean not, LP ifProp, Object... params) {
        return addIfProp(group, sID, false, caption, prop, not, ifProp, params);
    }

    protected LP addIfProp(AbstractGroup group, String sID, boolean persistent, String caption, LP prop, boolean not, LP ifProp, Object... params) {
        return addJProp(group, sID, persistent, caption, and(not), BaseUtils.add(getUParams(new LP[]{prop}, 0), BaseUtils.add(new LP[]{ifProp}, params)));
    }

    public LP addIfElseUProp(LP prop1, LP prop2, LP ifProp, Object... params) {
        return addIfElseUProp(privateGroup, "sys", prop1, prop2, ifProp, params);
    }

    protected LP addIfElseUProp(AbstractGroup group, String caption, LP prop1, LP prop2, LP ifProp, Object... params) {
        return addIfElseUProp(group, genSID(), caption, prop1, prop2, ifProp, params);
    }

    public LP addIfElseUProp(AbstractGroup group, String sID, String caption, LP prop1, LP prop2, LP ifProp, Object... params) {
        return addIfElseUProp(group, sID, false, caption, prop1, prop2, ifProp, params);
    }

    protected LP addIfElseUProp(AbstractGroup group, String sID, boolean persistent, String caption, LP prop1, LP prop2, LP ifProp, Object... params) {
        return addXSUProp(group, sID, persistent, caption, addIfProp(prop1, false, ifProp, params), addIfProp(prop2, true, ifProp, params));
    }

    // объединение пересекающихся свойств

    public LP addSUProp(Union unionType, LP... props) {
        return addSUProp(privateGroup, "sys", unionType, props);
    }

    public LP addSUProp(String caption, Union unionType, LP... props) {
        return addSUProp((AbstractGroup) null, caption, unionType, props);
    }

    public LP addSUProp(AbstractGroup group, String caption, Union unionType, LP... props) {
        return addSUProp(group, genSID(), caption, unionType, props);
    }

    public LP addSUProp(String sID, String caption, Union unionType, LP... props) {
        return addSUProp(sID, false, caption, unionType, props);
    }

    public LP addSUProp(String sID, boolean persistent, String caption, Union unionType, LP... props) {
        return addSUProp(null, sID, persistent, caption, unionType, props);
    }

    Collection<LP[]> checkSUProps = new ArrayList<LP[]>();

    // объединяет разные по классам св-ва

    public LP addSUProp(AbstractGroup group, String sID, String caption, Union unionType, LP... props) {
        return addSUProp(group, sID, false, caption, unionType, props);
    }

    public LP addSUProp(AbstractGroup group, String sID, boolean persistent, String caption, Union unionType, LP... props) {
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

    public LP[] addMUProp(AbstractGroup group, String[] ids, String[] captions, int extra, LP... props) {
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

    public LP addAProp(ActionProperty property) {
        return addAProp(actionGroup, property);
    }

    public LP addAProp(AbstractGroup group, ActionProperty property) {
        return addProperty(group, new LP<ClassPropertyInterface>(property));
    }

    public LP addBAProp(ConcreteCustomClass customClass, LP add) {
        return addAProp(new AddBarcodeActionProperty(customClass, add.property, genSID()));
    }

    public LP addSAProp(LP lp) {
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

    protected LP addRestartActionProp() {
        return BL.addRestartActionProp();
//        return addProperty(null, new LP<ClassPropertyInterface>(new RestartActionProperty(genSID(), "")));
    }

    protected LP addCancelRestartActionProp() {
        return BL.addCancelRestartActionProp();
//        return addProperty(null, new LP<ClassPropertyInterface>(new CancelRestartActionProperty(genSID(), "")));
    }

    /**
     * Добавляет action для запуска свойств с мэппингом по порядку, т.е. на входы и выход каждого свойства мэппятся интерфейсы результирующего по порядку
     */
    public LP addEPAProp(LP... lps) {
        return addEPAProp(false, lps);
    }

    /**
     * Добавляет action для запуска свойств с мэппингом по порядку, т.е. на входы и выход каждого свойства мэппятся интерфейсы результирующего по порядку
     * @param writeDefaultValues Если == true, то мэппятся только входы, без выхода
     */
    protected LP addEPAProp(boolean writeDefaultValues, LP... lps) {
        int[][] mapInterfaces = new int[lps.length][];
        for (int i = 0; i < lps.length; ++i) {
            LP lp = lps[i];
            mapInterfaces[i] = consecutiveInts(lp.listInterfaces.size() + (writeDefaultValues ? 0 : 1));
        }

        return addEPAProp(writeDefaultValues, lps, mapInterfaces);
    }

    /**
     * Добавляет action для запуска других свойств.
     *
     * Мэппиг задаётся перечислением свойств с указанием после каждого номеров интерфейсов результирующего свойства,
     * которые пойдут на входы и выход данных свойств
     * Пример 1: addEPAProp(true, userLogin, 1, inUserRole, 1, 2)
     * Пример 2: addEPAProp(false, userLogin, 1, 3, inUserRole, 1, 2, 4)
     *
     * @param writeDefaultValues использовать ли значения по умолчанию для записи в свойства.
     * Если значение этого параметра false, то мэпиться должны не только выходы, но и вход, номер интерфейса, который пойдёт на вход, должен быть указан последним
     */
    protected LP addEPAProp(boolean writeDefaultValues, Object... params) {
        List<LP> lps = new ArrayList<LP>();
        List<int[]> mapInterfaces = new ArrayList<int[]>();

        int pi = 0;
        while (pi < params.length) {
            assert params[pi] instanceof LP;

            LP lp = (LP) params[pi++];

            int[] propMapInterfaces = new int[lp.listInterfaces.size() + (writeDefaultValues ? 0 : 1)];
            for (int j = 0; j < propMapInterfaces.length; ++j) {
                propMapInterfaces[j] = (Integer) params[pi++] - 1;
            }

            lps.add(lp);
            mapInterfaces.add(propMapInterfaces);
        }
        return addEPAProp(writeDefaultValues, lps.toArray(new LP[lps.size()]), mapInterfaces.toArray(new int[mapInterfaces.size()][]));
    }

    private LP addEPAProp(boolean writeDefaultValues, LP[] lps, int[][] mapInterfaces) {
        return addAProp(new ExecutePropertiesActionProperty(genSID(), "sys", writeDefaultValues, lps, mapInterfaces));
    }

    public LP addLFAProp(AbstractGroup group, String caption, LP lp) {
        return addProperty(group, new LP<ClassPropertyInterface>(new LoadActionProperty(genSID(), caption, lp)));
    }

    public LP addOFAProp(AbstractGroup group, String caption, LP lp) { // обернем сразу в and
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
        public DataClass getValueClass() {
            FileClass fileClass = (FileClass) fileProperty.property.getType();
            return FileActionClass.getDefinedInstance(false, fileClass.toString(), fileClass.getExtensions());
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
            DataObject[] objects = new DataObject[keys.size()];
            int i = 0; // здесь опять учитываем, что порядок тот же
            for (ClassPropertyInterface classInterface : interfaces)
                objects[i++] = keys.get(classInterface);
            fileProperty.execute(value.getValue(), session, modifier, objects);
        }

        @Override
        public void proceedDefaultDesign(DefaultFormView view, PropertyDrawEntity<ClassPropertyInterface> entity) {
            super.proceedDefaultDesign(view, entity);
            view.get(entity).design.image = new ImageIcon(LoadActionProperty.class.getResource("/images/load.png"));
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

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
            DataObject[] objects = new DataObject[keys.size()];
            int i = 0; // здесь опять учитываем, что порядок тот же
            for (ClassPropertyInterface classInterface : interfaces)
                objects[i++] = keys.get(classInterface);
            actions.add(new OpenFileClientAction((byte[]) fileProperty.read(session, modifier, objects), BaseUtils.firstWord(getFileClass().getExtensions(), ",")));
        }

        @Override
        public void proceedDefaultDesign(DefaultFormView view, PropertyDrawEntity<ClassPropertyInterface> entity) {
            super.proceedDefaultDesign(view, entity);
            view.get(entity).design.image = new ImageIcon(OpenActionProperty.class.getResource("/images/open.png"));
        }
    }

    // params - по каким входам группировать
    public LP addIAProp(LP dataProperty, Integer... params) {
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

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {

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

            Integer maxValue = (Integer) maxProperty.read(session, modifier, maxPropertyInput.toArray(new DataObject[0]));
            if (maxValue == null)
                maxValue = 0;
            maxValue += 1;

            dataProperty.execute(maxValue, session, modifier, dataPropertyInput);
        }
    }

    private static class ChangeUserActionProperty extends ActionProperty {

        private ChangeUserActionProperty(String sID, ConcreteValueClass userClass) {
            super(sID, "Сменить пользователя", new ValueClass[]{userClass});
        }

        @Override
        public DataClass getValueClass() {
            return LogicalClass.instance;
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
            DataObject user = BaseUtils.singleValue(keys);
            if (executeForm.form.BL.requiredPassword) {
                actions.add(new UserReloginClientAction(executeForm.form.BL.getUserName(user).trim()));
            } else {
                session.user.changeCurrentUser(user);
                actions.add(new UserChangedClientAction());
            }
        }
    }

    public class RecalculateActionProperty extends ActionProperty {
        private RecalculateActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
            SQLSession sqlSession = session.sql;

            sqlSession.startTransaction();
            BL.recalculateAggregations(session.sql, BL.getAggregateStoredProperties());
            sqlSession.commitTransaction();

            actions.add(new MessageClientAction("Перерасчет был успешно завершен", "Перерасчет агрегаций"));
        }
    }

    public class PackActionProperty extends ActionProperty {
        private PackActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
            SQLSession sqlSession = session.sql;

            sqlSession.startTransaction();
            BL.packTables(sqlSession, tableFactory.getImplementTables().values());
            sqlSession.commitTransaction();

            actions.add(new MessageClientAction("Упаковка таблиц была успешно завершена", "Упаковка таблиц"));
        }
    }

    public static class MessageActionProperty extends ActionProperty {
        private String message;

        private MessageActionProperty(String message, String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
            this.message = message;
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
            actions.add(new MessageClientAction(message, caption));
        }
    }

    private class AddBarcodeActionProperty extends ActionProperty {

        ConcreteCustomClass customClass;
        Property<?> addProperty;

        private AddBarcodeActionProperty(ConcreteCustomClass customClass, Property addProperty, String sID) {
            super(sID, "Добавить [" + customClass + "] по бар-коду", new ValueClass[]{StringClass.get(13)});

            this.customClass = customClass;
            this.addProperty = addProperty;
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
            if (addProperty.read(session, new HashMap(), modifier) != null) {
                String barString = (String) BaseUtils.singleValue(keys).object;
                if (barString.trim().length() != 0) {
                    addProperty.execute(new HashMap(), session, null, modifier);
                    barcode.execute(barString, session, modifier, session.addObject(customClass, modifier));
                }
            }
        }
    }

    public LP addAAProp(CustomClass customClass, LP... properties) {
        return addAAProp(customClass, null, null, false, properties);
    }

    /** Пример использования:
    fileActPricat = addAAProp(pricat, filePricat.property, FileActionClass.getCustomInstance(true));
    pricat - добавляемый класс
    filePricat.property - свойство, которое изменяется
    FileActionClass.getCustomInstance(true) - класс
     неявный assertion, что класс свойства должен быть совместим с классом Action
     */
    protected LP addAAProp(ValueClass cls, Property propertyValue, DataClass dataClass) {
        return addAProp(new AddObjectActionProperty(genSID(), (CustomClass) cls, propertyValue, dataClass));
    }

    public LP addAAProp(CustomClass customClass, LP barcode, LP barcodePrefix, boolean quantity, LP... properties) {
        return addAProp(new AddObjectActionProperty(genSID(),
                (barcode != null) ? barcode.property : null, (barcodePrefix != null) ? barcodePrefix.property : null,
                quantity, customClass, LP.toPropertyArray(properties), null, null));
    }

    @IdentityLazy
    public LP getAddObjectAction(ValueClass cls) {
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

    public static class SeekActionProperty extends ActionProperty {

        Property property;

        private SeekActionProperty(String sID, String caption, ValueClass[] classes, Property property) {
            super(sID, caption, classes);
            this.property = property;
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
            FormInstance<?> form = executeForm.form;
            Collection<ObjectInstance> objects;
            if (property != null)
                objects = form.instanceFactory.getInstance(form.entity.getPropertyObject(property)).mapping.values();
            else
                objects = form.getObjects();
            for (Map.Entry<ClassPropertyInterface, DataObject> key : keys.entrySet()) {
                if (mapObjects.get(key.getKey()) == null) {
                    for (ObjectInstance object : objects) {
                        ConcreteClass keyClass = session.getCurrentClass(key.getValue());
                        if (keyClass instanceof ConcreteValueClass && object.getBaseClass().isCompatibleParent((ValueClass) keyClass)) {
                            form.seekObject(object, key.getValue());
                        }
                    }
                }
            }
        }
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
    public LP addHideCaptionProp(AbstractGroup group, String caption, LP original, LP hideProperty) {
        LP originalCaption = addCProp(StringClass.get(100), original.property.caption);
        LP result = addJProp(group, caption, and1, BaseUtils.add(new Object[]{originalCaption}, directLI(hideProperty)));
        return result;
    }

    protected LP addProp(Property<? extends PropertyInterface> prop) {
        return addProp(null, prop);
    }

    public LP addProp(AbstractGroup group, Property<? extends PropertyInterface> prop) {
        return addProperty(group, new LP(prop));
    }

    public <T extends LP<?>> T addProperty(AbstractGroup group, T lp) {
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

    private void addPersistent(AggregateProperty property) {
        assert !idSet.contains(property.sID);
        property.stored = true;

        logger.debug("Initializing stored property...");
        property.markStored(tableFactory);
    }

    public void addPersistent(LP lp) {
        addPersistent((AggregateProperty) lp.property);
    }

    private <T extends LP<?>> void registerProperty(T lp) {
        lproperties.add(lp);     // todo [dale]: нужно?
        lp.property.ID = idGenerator.idShift();
    }

    public void addConstraint(LP<?> lp, boolean checkChange) {
        lp.property.setConstraint(checkChange);
    }

    protected <L extends PropertyInterface, T extends PropertyInterface> void follows(LP<T> first, LP<L> second, int... mapping) {
        Map<L, T> mapInterfaces = new HashMap<L, T>();
        for (int i = 0; i < second.listInterfaces.size(); i++) {
            mapInterfaces.put(second.listInterfaces.get(i), first.listInterfaces.get(mapping[i] - 1));
        }
        addProp(first.property.addFollows(new PropertyMapImplement<L, T>(second.property, mapInterfaces)));
    }

    public void followed(LP first, LP... lps) {
        for (LP lp : lps) {
            int[] mapping = new int[lp.listInterfaces.size()];
            for (int i = 0; i < mapping.length; i++) {
                mapping[i] = i + 1;
            }
            follows(lp, first, mapping);
        }
    }

    public void setNotNull(LP property) {

        ValueClass[] values = property.getMapClasses();

        LP checkProp = addCProp(LogicalClass.instance, true, values);

        Map mapInterfaces = new HashMap();
        for (int i = 0; i < property.listInterfaces.size(); i++) {
            mapInterfaces.put(property.listInterfaces.get(i), checkProp.listInterfaces.get(i));
        }
        addProp(checkProp.property.addFollows(new PropertyMapImplement(property.property, mapInterfaces), "Свойство " + property.property.sID + " не задано", PropertyFollows.RESOLVE_FALSE));
    }


    private Map<ValueClass, LP> is = new HashMap<ValueClass, LP>();

    // получает свойство is
    // для множества классов есть CProp
    public LP is(ValueClass valueClass) {
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

    public LP and(boolean... nots) {
        return addAFProp(nots);
    }

    @IdentityLazy
    protected LP dumb(int interfaces) {
        ValueClass params[] = new ValueClass[interfaces];
        for (int i = 0; i < interfaces; ++i) {
            params[i] = baseClass;
        }
        return addCProp("dumbProperty" + interfaces, StringClass.get(0), "", params);
    }


    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////
    /// Indices
    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////

    protected Set<List<? extends Property>> indexes = new HashSet<List<? extends Property>>();

    public void addIndex(LP<?>... lps) {
        List<Property> index = new ArrayList<Property>();
        for (LP<?> lp : lps)
            index.add(lp.property);
        indexes.add(index);
    }


    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////
    /// Forms
    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////

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

    private class UserPolicyFormEntity extends FormEntity {
        protected UserPolicyFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Пользователи");

            ObjectEntity objUser = addSingleGroupObject(customUser, selection, baseGroup, true);
            ObjectEntity objRole = addSingleGroupObject(userRole, baseGroup, true);
            getPropertyDraw(userRoleDefaultForms).shouldBeLast = true;

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
            addPropertyDraw(objUserRole, objForm, userRoleFormDefaultNumber);


            setReadOnly(navigatorElementSID, true);
            setReadOnly(navigatorElementCaption, true);

            PropertyDrawEntity balanceDraw = getPropertyDraw(userRolePolicyOrder, objPolicy.groupTo);
            PropertyDrawEntity sidDraw = getPropertyDraw(userRoleSID, objUserRole.groupTo);
            balanceDraw.addColumnGroupObject(objUserRole.groupTo);
            balanceDraw.setPropertyCaption(sidDraw.propertyObject);
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

    private class ConnectionsFormEntity extends FormEntity {
        protected ConnectionsFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Подключения к серверу");

            ObjectEntity objConnection = addSingleGroupObject(connection, baseGroup, true);
            ObjectEntity objForm = addSingleGroupObject(navigatorElement, "Открытые формы", baseGroup, true);

//            setReadOnly(baseGroup, true);

            addPropertyDraw(objConnection, objForm, baseGroup, true);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(connectionFormCount, objConnection, objForm), Compare.GREATER, 0));

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(connectionCurrentStatus, objConnection), Compare.EQUALS, connectionStatus.getDataObject("connectedConnection")),
                    "Активные подключения",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
            addRegularFilterGroup(filterGroup);
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
                PropertyDrawEntity objectValue = getPropertyDraw(BaseLogicsModule.this.objectValue, mainObjects[i]);
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

            addPropertyDraw(new LP[]{smtpHost, smtpPort, fromAddress, emailAccount, emailPassword, emailBlindCarbonCopy, disableEmail, webHost, defaultCountry, barcodePrefix, restartServerAction, cancelRestartServerAction, recalculateAction, packAction});
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

    private class RemindUserPassFormEntity extends FormEntity { // письмо эксперту о логине
        private ObjectEntity objUser;

        private RemindUserPassFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Напоминание пароля", true);

            objUser = addSingleGroupObject(1, "customUser", customUser, userLogin, userPassword, name);
            objUser.groupTo.initClassView = ClassViewType.PANEL;

            addInlineEAForm(emailUserPassUser, this, objUser, 1);
        }

        @Override
        public boolean isReadOnly() {
            return true;
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


}
