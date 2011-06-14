package platform.server.logics;

import org.apache.log4j.Logger;
import platform.base.BaseUtils;
import platform.base.identity.DefaultIDGenerator;
import platform.base.identity.IDGenerator;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.action.*;
import platform.server.caches.IdentityLazy;
import platform.server.classes.*;
import platform.server.data.SQLSession;
import platform.server.data.Time;
import platform.server.data.Union;
import platform.server.data.expr.query.OrderType;
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
import platform.server.form.window.TreeNavigatorWindow;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.DeleteObjectActionProperty;
import platform.server.logics.property.actions.FormActionProperty;
import platform.server.logics.property.actions.GenerateLoginPasswordActionProperty;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.property.group.PropertySet;
import platform.server.logics.table.TableFactory;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

import static platform.server.logics.PropertyUtils.mapImplement;
import static platform.server.logics.PropertyUtils.readImplements;

/**
 * User: DAle
 * Date: 16.05.11
 * Time: 17:52
 */

public class BaseLogicsModule<T extends BusinessLogics<T>> extends LogicsModule {
    // classes
    public BaseClass baseClass;
    Logger logger;

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
    public LP divideDouble;
    public LP divideDouble2;
    public LP addDate2;
    public LP string2;
    public LP insensitiveString2;
    protected LP concat2;
    public LP percent;
    public LP percent2;
    public LP share2;
    public LP yearInDate;

    public LP vtrue, actionTrue, vzero;
    public LP positive, negative;

    public LP round0;

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
    public LP parentNavigatorElement;
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
    public LP sidToCountry;
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
    static private IDGenerator idGenerator = new DefaultIDGenerator();

    T BL;

    public BaseLogicsModule(T BL, Logger logger) {
        super("BaseLogicsModule");
        setBaseLogicsModule(this);
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
        rootGroup = addAbstractGroup("rootGroup", "Корневая группа", null, false);
        sessionGroup = addAbstractGroup("sessionGroup", "Сессионные свойства", rootGroup, false);
        publicGroup = addAbstractGroup("publicGroup", "Пользовательские свойства", rootGroup, false);
        actionGroup = addAbstractGroup("actionGroup", "Действия", rootGroup, false);
        privateGroup = addAbstractGroup("privateGroup", "Внутренние свойства", rootGroup, false);
        baseGroup = addAbstractGroup("baseGroup", "Атрибуты", publicGroup, false);
        idGroup = addAbstractGroup("idGroup", "Идентификаторы", publicGroup, false);
        recognizeGroup = addAbstractGroup("recognizeGroup", "Идентифицирующие свойства", baseGroup, false);
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
        tableFactory.include("country", country, DateClass.instance);

        tableFactory.include("session", session);
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
        share2 = addSFProp("round(CAST(((prm1)/(prm2)*100) as numeric), 2)", DoubleClass.instance, 2);
        between = addJProp("Между", and1, groeq2, 1, 2, groeq2, 3, 1);
        vtrue = addCProp("Истина", LogicalClass.instance, true);
        vzero = addCProp("0", DoubleClass.instance, 0);

        round0 = addSFProp("round(CAST(prm1 as numeric), 0)", DoubleClass.instance, 1);

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
        parentNavigatorElement = addDProp("parentNavigatorElement", "Родит. форма", navigatorElement, navigatorElement);

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

    static int generateStaticNewID() {
        return idGenerator.idShift();
    }

    <T extends LP<?>> void registerProperty(T lp) {
        lproperties.add(lp);     // todo [dale]: нужно?
        lp.property.ID = idGenerator.idShift();
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
        baseClass.named.setClassForm(new NamedObjectClassForm(this, baseClass.named));

        baseElement = new NavigatorElement("baseElement", "Формы");
        baseWindow = new TreeNavigatorWindow(baseElement.getSID() + "Window", "Навигатор", 0, 0, 20, 70);
        baseElement.window = baseWindow;

        adminElement = new NavigatorElement(baseElement, "adminElement", "Администрирование");

        objectElement = baseClass.getBaseClassForm(this);
        adminElement.add(objectElement);

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

    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////
    /// Properties
    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////

    Set<String> idSet = new HashSet<String>();


    Collection<LP[]> checkCUProps = new ArrayList<LP[]>();

    // объединяет разные по классам св-ва

    Collection<LP[]> checkSUProps = new ArrayList<LP[]>();


    Map<ValueClass, LP> is = new HashMap<ValueClass, LP>();

    Map<ValueClass, LP> object = new HashMap<ValueClass, LP>();

    protected LP addRestartActionProp() {
        return BL.addRestartActionProp();
//        return addProperty(null, new LP<ClassPropertyInterface>(new RestartActionProperty(genSID(), "")));
    }

    protected LP addCancelRestartActionProp() {
        return BL.addCancelRestartActionProp();
//        return addProperty(null, new LP<ClassPropertyInterface>(new CancelRestartActionProperty(genSID(), "")));
    }

    public static class SeekActionProperty extends ActionProperty {

        Property property;

        SeekActionProperty(String sID, String caption, ValueClass[] classes, Property property) {
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

    public static class LoadActionProperty extends ActionProperty {

        LP fileProperty;

        LoadActionProperty(String sID, String caption, LP fileProperty) {
            super(sID, caption, fileProperty.getMapClasses());

            this.fileProperty = fileProperty;
        }

        @Override
        public DataClass getValueClass() {
            FileClass fileClass = (FileClass) fileProperty.property.getType();
            return FileActionClass.getDefinedInstance(false, fileClass.toString(), fileClass.getExtensions());
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
            FormInstance<?> form = executeForm.form;
            DataObject[] objects = new DataObject[keys.size()];
            int i = 0; // здесь опять учитываем, что порядок тот же
            for (ClassPropertyInterface classInterface : interfaces)
                objects[i++] = keys.get(classInterface);
            fileProperty.execute(value.getValue(), session, modifier, objects);
        }

        @Override
        public void proceedDefaultDesign(DefaultFormView view, PropertyDrawEntity<ClassPropertyInterface> entity) {
            super.proceedDefaultDesign(view, entity);
            view.get(entity).design.setImage(new ImageIcon(LoadActionProperty.class.getResource("/images/load.png")));
        }
    }

    public static class OpenActionProperty extends ActionProperty {

        LP fileProperty;

        OpenActionProperty(String sID, String caption, LP fileProperty) {
            super(sID, caption, fileProperty.getMapClasses());

            this.fileProperty = fileProperty;
        }

        private FileClass getFileClass() {
            return (FileClass) fileProperty.property.getType();
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
            FormInstance<?> form = executeForm.form;
            DataObject[] objects = new DataObject[keys.size()];
            int i = 0; // здесь опять учитываем, что порядок тот же
            for (ClassPropertyInterface classInterface : interfaces)
                objects[i++] = keys.get(classInterface);
            actions.add(new OpenFileClientAction((byte[]) fileProperty.read(session, modifier, objects), BaseUtils.firstWord(getFileClass().getExtensions(), ",")));
        }

        @Override
        public void proceedDefaultDesign(DefaultFormView view, PropertyDrawEntity<ClassPropertyInterface> entity) {
            super.proceedDefaultDesign(view, entity);
            view.get(entity).design.setImage(new ImageIcon(OpenActionProperty.class.getResource("/images/open.png")));
        }
    }

    public static class IncrementActionProperty extends ActionProperty {

        LP dataProperty;
        LP maxProperty;
        List<Integer> params;

        IncrementActionProperty(String sID, String caption, LP dataProperty, LP maxProperty, Integer[] params) {
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

    private class RecalculateActionProperty extends ActionProperty {
        private RecalculateActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapExecuteObjects) {
            throw new RuntimeException("should not be");
        }

        @Override
        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
            SQLSession sqlSession = session.sql;

            sqlSession.startTransaction();
            BL.recalculateAggregations(session.sql, BL.getAggregateStoredProperties());
            sqlSession.commitTransaction();

            actions.add(new MessageClientAction("Перерасчет был успешно завершен", "Перерасчет агрегаций"));
        }
    }

    private class PackActionProperty extends ActionProperty {
        private PackActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
        }

        @Override
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

        MessageActionProperty(String message, String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
            this.message = message;
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
            actions.add(new MessageClientAction(message, caption));
        }
    }

    class AddBarcodeActionProperty extends ActionProperty {

        ConcreteCustomClass customClass;
        Property<?> addProperty;

        AddBarcodeActionProperty(ConcreteCustomClass customClass, Property addProperty, String sID) {
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
        private ObjectEntity objTreeForm;
        private TreeGroupEntity treeFormObject;

        protected RolePolicyFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Роли");

            objUserRole = addSingleGroupObject(userRole, baseGroup, true);
            objPolicy = addSingleGroupObject(policy, "Политики безопасности", baseGroup, true);
            objForm = addSingleGroupObject(navigatorElement, "Таблица", true);
            objTreeForm = addSingleGroupObject(navigatorElement, "Дерево", true);

            objTreeForm.groupTo.setIsParents(addPropertyObject(parentNavigatorElement, objTreeForm));
            treeFormObject = addTreeGroupObject(objTreeForm.groupTo);

            addObjectActions(this, objUserRole);

            addPropertyDraw(new LP[]{navigatorElementCaption, navigatorElementSID}, objForm);
            addPropertyDraw(new LP[]{navigatorElementCaption, navigatorElementSID}, objTreeForm);
            addPropertyDraw(objUserRole, objPolicy, baseGroup, true);
            addPropertyDraw(objUserRole, objForm, permissionUserRoleForm);
            addPropertyDraw(objUserRole, objTreeForm, permissionUserRoleForm);
            addPropertyDraw(objUserRole, objForm, userRoleFormDefaultNumber);
            addPropertyDraw(objUserRole, objTreeForm, userRoleFormDefaultNumber);

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

            ContainerView formsContainer = design.createContainer("Формы");
            formsContainer.tabbedPane = true;
            formsContainer.add(design.getTreeContainer(treeFormObject));
            formsContainer.add(design.getGroupObjectContainer(objForm.groupTo));

            design.getMainContainer().addAfter(container, design.getGroupObjectContainer(objUserRole.groupTo));
            container.add(design.getGroupObjectContainer(objPolicy.groupTo));
            container.add(formsContainer);

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

    class SelectFromListFormEntity extends FormEntity implements FormActionProperty.SelfInstancePostProcessor {
        ObjectEntity[] mainObjects;
        private ObjectEntity selectionObject;
        private ObjectEntity remapObject;
        private final FilterEntity[] remapFilters;

        SelectFromListFormEntity(ObjectEntity remapObject, FilterEntity[] remapFilters, LP selectionProperty, boolean isSelectionClassFirstParam, ValueClass selectionClass, ValueClass... baseClasses) {
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

    private class DictionariesFormEntity extends FormEntity {

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

    private class NamedObjectClassForm extends ClassFormEntity {
        public ObjectEntity objObjectName;


        public NamedObjectClassForm(BaseLogicsModule LM, CustomClass cls, String sID, String caption) {
            super(LM, cls, sID, caption);

            objObjectName = addSingleGroupObject(StringClass.get(50), "Поиск по началу имени", objectValue);
            objObjectName.groupTo.setSingleClassView(ClassViewType.PANEL);

            //двигаем в начало
            groups.remove(objObjectName.groupTo);
            groups.add(0, objObjectName.groupTo);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(name, object), Compare.START_WITH, objObjectName));
        }

        public NamedObjectClassForm(BaseLogicsModule LM, CustomClass cls) {
            this(LM, cls, "namedObjectForm" + cls.getSID(), cls.caption);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();
            design.get(getPropertyDraw(objectValue, objObjectName)).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0);
            return design;
        }

        @Override
        public AbstractClassFormEntity copy() {
            return new NamedObjectClassForm(LM, cls, getSID() + "_copy" + copies++, caption);
        }
    }


}
