package platform.server.logics;

import org.apache.log4j.Logger;
import platform.base.identity.DefaultIDGenerator;
import platform.base.identity.IDGenerator;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.KeyStrokes;
import platform.interop.PropertyEditType;
import platform.interop.action.LogOutClientAction;
import platform.interop.action.UserChangedClientAction;
import platform.interop.action.UserReloginClientAction;
import platform.interop.form.layout.ContainerType;
import platform.server.caches.IdentityLazy;
import platform.server.classes.*;
import platform.server.data.Time;
import platform.server.data.Union;
import platform.server.data.expr.query.PartitionType;
import platform.server.form.entity.CalcPropertyObjectEntity;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.entity.filter.*;
import platform.server.form.instance.FormInstance;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.ContainerView;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.form.window.AbstractWindow;
import platform.server.form.window.NavigatorWindow;
import platform.server.form.window.ToolBarNavigatorWindow;
import platform.server.form.window.TreeNavigatorWindow;
import platform.server.logics.linear.LAP;
import platform.server.logics.linear.LCP;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.AdminActionProperty;
import platform.server.logics.property.actions.FormActionProperty;
import platform.server.logics.property.actions.UserActionProperty;
import platform.server.logics.property.actions.flow.ApplyActionProperty;
import platform.server.logics.property.actions.flow.BreakActionProperty;
import platform.server.logics.property.actions.flow.CancelActionProperty;
import platform.server.logics.property.actions.flow.ReturnActionProperty;
import platform.server.logics.property.actions.form.*;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.property.group.PropertySet;
import platform.server.logics.table.TableFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

import static platform.server.logics.PropertyUtils.mapCalcImplement;
import static platform.server.logics.PropertyUtils.readCalcImplements;
import static platform.server.logics.ServerResourceBundle.getString;

/**
 * User: DAle
 * Date: 16.05.11
 * Time: 17:52
 */

public class BaseLogicsModule<T extends BusinessLogics<T>> extends LogicsModule {
    // classes
    public BaseClass baseClass;
    Logger logger;

    public AbstractCustomClass contact;
    public AbstractCustomClass user;
    public ConcreteCustomClass systemUser;
    public ConcreteCustomClass session;
    public ConcreteCustomClass multiLanguageNamed;
    public ConcreteCustomClass customUser;
    public ConcreteCustomClass computer;
    public ConcreteCustomClass dictionary;
    public ConcreteCustomClass dictionaryEntry;

    public StaticCustomClass month;
    public StaticCustomClass DOW;

    public StaticCustomClass formResult;

    // groups
    public AbstractGroup rootGroup;
    public AbstractGroup publicGroup;
    public AbstractGroup privateGroup;
    public AbstractGroup baseGroup;
    public AbstractGroup actionGroup;
    public AbstractGroup sessionGroup;
    public AbstractGroup recognizeGroup;

    // properties
    public LCP groeq2;
    public LCP lsoeq2;
    public LCP greater2, less2;
    public LCP object1, and1, andNot1;
    public LCP equals2, diff2;
    public LCP upper;
    public LCP sum;
    public LCP subtract;
    public LCP multiply;
    public LCP subtractInteger;
    public LCP divide;
    public LCP sumDate;
    public LCP sumDateTimeDay;
    public LCP subtractDate;
    public LCP dateTimeToDateTime;
    public LCP toDateTime;

    public LCP string2SP, istring2SP;
    public LCP string2, istring2;
    public LCP ustring2CM, ustring2SP, ustring3SP, ustring2, ustring3, ustring4, ustring5CM;

    public LCP weekInDate;
    public LCP numberDOWInDate;
    public LCP numberMonthInDate;
    public LCP yearInDate;
    public LCP dayInDate;
    public LCP toDate;
    public LCP toTime;

    public LCP numberMonth;
    public LCP monthNumber;
    public LCP monthInDate;

    public LCP numberDOW;
    public LCP DOWNumber;
    public LCP DOWInDate;

    public LCP vtrue;
    public LCP vzero;
    public LCP vnull;

    public LCP minus;

    public LCP<?> name;

    public LAP formPrint;
    public LAP formEdit;
    public LAP formXls;
    public LAP formNull;
    public LAP formRefresh;
    public LAP formApply;
    public LAP formCancel;
    public LAP formOk;
    public LAP formClose;

    public LAP seek;

    public LCP currentDate;
    public LCP currentMonth;
    public LCP currentYear;
    public LCP currentHour;
    public LCP currentMinute;
    protected LCP currentEpoch;
    protected LCP currentDateTime;
    protected LCP currentTime;
    public LCP currentUser;
    public LCP currentSession;
    public LCP currentComputer, hostnameCurrentComputer;
    public LAP changeUser;
    public LAP logOut;
    protected LCP isServerRestarting;
    public LAP restartServerAction;
    public LAP runGarbageCollector;
    public LAP cancelRestartServerAction;

    public LCP userLogin;
    public LCP userPassword;
    public LCP userFirstName;
    public LCP userLastName;
    public LCP userPhone;
    public LCP userPostAddress;
    public LCP userBirthday;

    public LCP currentUserName;
    public LCP<?> loginToUser;

    public LCP hostname;

    public LAP delete;
    public LAP deleteApply;

    public LAP<?> apply;
    public LCP<?> canceled;

    public LAP flowBreak;
    public LAP flowReturn;
    public LAP<?> cancel;

    public LCP objectClass;
    public LCP objectClassName;
    public LCP classSID;
    public LCP dataName;

    public LCP defaultBackgroundColor;
    public LCP defaultOverrideBackgroundColor;
    public LCP defaultForegroundColor;
    public LCP defaultOverrideForegroundColor;

    protected LCP termDictionary;
    protected LCP translationDictionary;
    public LCP insensitiveDictionary;
    public LCP insensitiveTermDictionary;
    protected LCP entryDictionary;
    protected LCP nameEntryDictionary;
    public LCP translationDictionaryTerm;
    public LCP insensitiveTranslationDictionaryTerm;

    public LCP dumb1;

    public SelectionPropertySet selection;
    protected CompositeNamePropertySet compositeName;
    public ObjectValuePropertySet objectValue;

    public TableFactory tableFactory;

    public List<LP> lproperties = new ArrayList<LP>();

    // счетчик идентификаторов
    static private IDGenerator idGenerator = new DefaultIDGenerator();

    T BL;

    public T getBL(){
        return BL;
    }
    
    public BaseLogicsModule(T BL, Logger logger) {
        super("System", "System");
        setBaseLogicsModule(this);
        this.BL = BL;
        this.logger = logger;
    }

    public LP getLP(String sID) {
        objectValue.getProperty(sID);
        selection.getProperty(sID);
        compositeName.getProperty(sID);

        for (LP lp : lproperties) {
            if (lp.property.getSID().equals(sID)) {
                return lp;
            }
        }
        return null;
    }

    @Override
    public void initModuleDependencies() {
    }

    @Override
    public void initModule() {
    }

    @Override
    public void initClasses() {
        baseClass = addBaseClass("object", getString("logics.object"));

        contact = addAbstractClass("contact", getString("logics.user.contact"), baseClass);
        user = addAbstractClass("user", getString("logics.user"), baseClass);
        systemUser = addConcreteClass("systemUser", getString("logics.user.system.user"), user);

        session = addConcreteClass("session", getString("logics.session"), baseClass);

        customUser = addConcreteClass("customUser", getString("logics.user.ordinary.user"), BL.LM.user, BL.LM.contact/*, BL.LM.barcodeObject*/);
        computer = addConcreteClass("computer", getString("logics.workplace"), baseClass);

        dictionary = addConcreteClass("dictionary", getString("logics.dictionary"), baseClass.named);
        dictionaryEntry = addConcreteClass("dictionaryEntry", getString("logics.dictionary.entries"), baseClass);

        month = addStaticClass("month", getString("logics.month"),
                new String[]{"january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december"},
                new String[]{getString("logics.month.january"), getString("logics.month.february"), getString("logics.month.march"), getString("logics.month.april"), getString("logics.month.may"), getString("logics.month.june"), getString("logics.month.july"), getString("logics.month.august"), getString("logics.month.september"), getString("logics.month.october"), getString("logics.month.november"), getString("logics.month.december")});
        DOW = addStaticClass("DOW", getString("logics.week.day"),
                  new String[]{"sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday"},
                  new String[]{getString("logics.days.sunday"), getString("logics.days.monday"), getString("logics.days.tuesday"), getString("logics.days.wednesday"), getString("logics.days.thursday"), getString("logics.days.friday"), getString("logics.days.saturday")});
        formResult = addStaticClass("formResult", "Результат вызова формы",
                new String[]{"null", "ok", "close"},
                new String[]{"Неизвестно", "Принять", "Закрыть"});

        // todo : раскидать по модулям

        multiLanguageNamed = addConcreteClass("multiLanguageNamed", "Мультиязычный объект", baseClass);
    }

    @Override
    public void initGroups() {
        rootGroup = addAbstractGroup("rootGroup", getString("logics.groups.rootgroup"), null, false);
        sessionGroup = addAbstractGroup("sessionGroup", getString("logics.groups.sessiongroup"), rootGroup, false);
        publicGroup = addAbstractGroup("publicGroup", getString("logics.groups.publicgroup"), rootGroup, false);
        actionGroup = addAbstractGroup("actionGroup", getString("logics.groups.actiongroup"), rootGroup, false);
        privateGroup = addAbstractGroup("privateGroup", getString("logics.groups.privategroup"), rootGroup, false);
        baseGroup = addAbstractGroup("baseGroup", getString("logics.groups.basegroup"), publicGroup, false);
        recognizeGroup = addAbstractGroup("recognizeGroup", getString("logics.groups.recognizegroup"), baseGroup, false);

        initBaseGroupAliases();
    }

    @Override
    public void initTables() {
        tableFactory = new TableFactory();
        for (int i = 0; i < TableFactory.MAX_INTERFACE; i++) { // заполним базовые таблицы
            CustomClass[] baseClasses = new CustomClass[i];
            for (int j = 0; j < i; j++)
                baseClasses[j] = baseClass;
            addTable("base_" + i, baseClasses);
        }

        addTable("computer", computer);
        addTable("userTable", user);
        addTable("contact", contact);
        addTable("loginSID", StringClass.get(30), StringClass.get(30));
        addTable("objectObjectDate", baseClass, baseClass, DateClass.instance);
        addTable("named", baseClass.named);
        addTable("sidClass", baseClass.sidClass);
        addTable("dictionary", dictionary);
        addTable("dictionaryEntry", dictionaryEntry);

        addTable("session", session);

        addTable("sessionObject", session, baseClass);

        addTable("month", month);
        addTable("dow", DOW);
    }

    @Override
    public void initProperties() {

        dumb1 = dumb(1);

        canceled = addProperty(null, new LCP<ClassPropertyInterface>(new SessionDataProperty("canceled", "Canceled", LogicalClass.instance)));

        apply = addAProp(new ApplyActionProperty(BL, canceled.property));
        cancel = addAProp(new CancelActionProperty());

        flowBreak = addProperty(null, new LAP(new BreakActionProperty()));
        flowReturn = addProperty(null, new LAP(new ReturnActionProperty()));

        // Множества свойств
        selection = new SelectionPropertySet();
        sessionGroup.add(selection);

        objectValue = new ObjectValuePropertySet();
        baseGroup.add(objectValue);

        compositeName = new CompositeNamePropertySet();
        privateGroup.add(compositeName);

        // логические св-ва
        and1 = addAFProp("and1", false);
        andNot1 = addAFProp(true);

        // Сравнения
        equals2 = addCFProp("equals2", Compare.EQUALS);
        groeq2 = addCFProp(Compare.GREATER_EQUALS);
        greater2 = addCFProp("greater2", Compare.GREATER);
        lsoeq2 = addCFProp(Compare.LESS_EQUALS);
        less2 = addCFProp(Compare.LESS);
        diff2 = addCFProp("diff2", Compare.NOT_EQUALS);

        // "Склеивание" строк
        string2SP = addSProp("string2SP", 2);
        istring2SP = addInsensitiveSProp("istring2SP", 2);

        string2 = addSProp("string2", 2, "");
        istring2 = addInsensitiveSProp("istring2", 2, "");

        ustring2CM = addSFUProp("ustring2CM", " ", 2);
        ustring2SP = addSFUProp("ustring2SP", " ", 2);
        ustring3SP = addSFUProp("ustring3SP", " ", 3);
        ustring2 = addSFUProp("ustring2", "", 2);
        ustring3 = addSFUProp("ustring3", "", 3);
        ustring4 = addSFUProp("ustring4", "", 4);

        ustring5CM = addSFUProp("ustring5CM", ",", 5);

        // Обработка строк
        upper = addSFProp("upper", "upper(prm1)", 1);

        // Математические операции
        sum = addSFProp("sum", "((prm1)+(prm2))", 2);
        subtract = addSFProp("subtract", "((prm1)-(prm2))", 2);
        multiply = addMFProp("multiply", 2);
        divide = addSFProp("divide", "((prm1)/(prm2))", 2);

        minus = addSFProp("minus", "(-(prm1))", 1);

        // Оставляем пока в BaseLogicsModule, посколько скорее всего их придется использовать в DSL

        // Операции с целыми числами
        subtractInteger = addSFProp("subtractInteger", "((prm1)-(prm2))", IntegerClass.instance, 2);

        // Операции над датами
        sumDate = addSFProp("sumDate", "((prm1)+(prm2))", DateClass.instance, 2);
        subtractDate = addSFProp("subtractDate", "((prm1)-(prm2))", DateClass.instance, 2);

        sumDateTimeDay = addSFProp("sumDateTimeDay", "((prm1)+(prm2)*CAST('1 days' AS INTERVAL))", DateTimeClass.instance, 2);

        // Константы
        vtrue = addCProp(getString("logics.true"), LogicalClass.instance, true);
        vzero = addCProp("0", DoubleClass.instance, 0);
        vnull = addProperty(privateGroup, new LCP<PropertyInterface>(NullValueProperty.instance));

        // Обработка дат

        numberDOW = addJProp(baseGroup, "numberDOW", true, getString("logics.week.day.number"), subtractInteger,
                addOProp("numberDOWP1", getString("logics.week.day.number.plus.one"), PartitionType.SUM, addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), is(DOW), 1), true, false, 0, 1), 1,
                addCProp(IntegerClass.instance, 1));
        DOWNumber = addAGProp("DOWNumber", getString("logics.week.day.id"), numberDOW);

        numberMonth = addOProp(baseGroup, "numberMonth", true, getString("logics.month.number"), addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), is(month), 1), PartitionType.SUM, true, true, 0, 1);
        monthNumber = addAGProp("monthNumber", getString("logics.month.id"), numberMonth);

        // Преобразование типов

        dayInDate = addSFProp("dayInDate", "(extract(day from (prm1)))", IntegerClass.instance, 1);
        weekInDate = addSFProp("weekInDate", "(extract(week from (prm1)))", IntegerClass.instance, 1);

        numberDOWInDate = addSFProp("numberDOWInDate", "(extract(dow from (prm1)))", IntegerClass.instance, 1);
        DOWInDate = addJProp("DOWInDate", getString("logics.week.day.id"), DOWNumber, numberDOWInDate, 1);

        numberMonthInDate = addSFProp("numberMonthInDate", "(extract(month from (prm1)))", IntegerClass.instance, 1);
        monthInDate = addJProp("monthInDate", getString("logics.month.id"), monthNumber, numberMonthInDate, 1);

        yearInDate = addSFProp("yearInDate", "(extract(year from (prm1)))", IntegerClass.instance, 1);

        toDate = addSFProp("toDate", "(CAST((prm1) as date))", DateClass.instance, 1);
        toTime = addSFProp("toTime", "(CAST((prm1) as time))", TimeClass.instance, 1);
        toDateTime = addSFProp("toDateTime", "(CAST((prm1) as timestamp))", DateTimeClass.instance, 1);

        dateTimeToDateTime = addSFProp("dateTimeToDateTime", "to_timestamp(CAST(prm1 as char(10)) || CAST(prm2 as char(8)), \'YYYY-MM-DDHH24:MI:SS\')", DateTimeClass.instance, 2);

        // Действия

        delete = addAProp(baseClass.unknown.getChangeClassAction());

        deleteApply = addListAProp("deleteApply", delete.property.caption, delete, 1, apply);
        deleteApply.setImage("delete.png");
        deleteApply.setEditKey(KeyStrokes.getDeleteActionPropertyKeyStroke());
        deleteApply.setShowEditKey(false);
        deleteApply.setAskConfirm(true);
        deleteApply.setShouldBeLast(true);

        // Действия на форме
        formApply = addProperty(null, new LAP(new FormApplyActionProperty(BL)));
        formCancel = addProperty(null, new LAP(new FormCancelActionProperty()));
        formPrint = addProperty(null, new LAP(new PrintActionProperty()));
        formEdit = addProperty(null, new LAP(new EditActionProperty()));
        formXls = addProperty(null, new LAP(new XlsActionProperty()));
        formNull = addProperty(null, new LAP(new NullActionProperty()));
        formRefresh = addProperty(null, new LAP(new RefreshActionProperty()));
        formOk = addProperty(null, new LAP(new OkActionProperty()));
        formClose = addProperty(null, new LAP(new CloseActionProperty()));
        
        seek = addSAProp(null);

        // Текущие значения
        currentDate = addDProp(baseGroup, "currentDate", getString("logics.date.current.date"), DateClass.instance);
        currentMonth = addJProp(baseGroup, "currentMonth", getString("logics.date.current.month"), numberMonthInDate, currentDate);
        currentYear = addJProp(baseGroup, "currentYear", getString("logics.date.current.year"), yearInDate, currentDate);

        currentDateTime = addTProp("currentDateTime", getString("logics.date.current.datetime"), Time.DATETIME);
        currentTime = addJProp("currentTime", getString("logics.date.current.time"), toTime, currentDateTime);
        currentMinute = addTProp("currentMinute", getString("logics.date.current.minute"), Time.MINUTE);
        currentHour = addTProp("currentHour", getString("logics.date.current.hour"), Time.HOUR);
        currentEpoch = addTProp("currentEpoch", getString("logics.date.current.epoch"), Time.EPOCH);

        // Сессия
        currentSession = addProperty(null, new LCP<ClassPropertyInterface>(new CurrentSessionDataProperty("currentSession", session)));

        // Компьютер
        // todo : переименовать в соответствии с naming policy
        hostname = addDProp(baseGroup, "hostname", getString("logics.host.name"), InsensitiveStringClass.get(100), computer);
        currentComputer = addProperty(null, new LCP<PropertyInterface>(new CurrentComputerFormulaProperty("currentComputer", computer)));
        hostnameCurrentComputer = addJProp("hostnameCurrentComputer", getString("logics.current.computer.hostname"), hostname, currentComputer);

        // Контакты
        // todo : переименовать в соответствии с namingPolicy
        userFirstName = addDProp(publicGroup, "userFirstName", getString("logics.user.firstname"), StringClass.get(30), contact);
        userFirstName.setMinimumCharWidth(10);

        userLastName = addDProp(publicGroup, "userLastName", getString("logics.user.lastname"), StringClass.get(30), contact);
        userLastName.setMinimumCharWidth(10);

        userPhone = addDProp(publicGroup, "userPhone", getString("logics.user.phone"), StringClass.get(30), contact);
        userPhone.setMinimumCharWidth(10);

        userPostAddress = addDProp(publicGroup, "userPostAddress", getString("logics.user.postAddress"), StringClass.get(100), contact);
        userPostAddress.setMinimumCharWidth(20);

        userBirthday = addDProp(publicGroup, "userBirthday", getString("logics.user.birthday"),  DateClass.instance, contact);

        // todo : тут надо что-то придумать более логичное
        dataName = addDProp("name", getString("logics.name"), InsensitiveStringClass.get(110), baseClass.named);
        ((CalcProperty)dataName.property).aggProp = true;
        name = addCUProp(recognizeGroup, "commonName", getString("logics.name"), dataName,
                addJProp(istring2SP, userFirstName, 1, userLastName, 1));
        ((CalcProperty)name.property).aggProp = true;

        // todo : тут надо рефакторить как имена свойст, так и классов
        classSID = addDProp("classSID", getString("logics.statcode"), StringClass.get(250), baseClass.sidClass);
        objectClass = addProperty(null, new LCP<ClassPropertyInterface>(baseClass.getObjectClassProperty()));
        objectClassName = addJProp(baseGroup, "objectClassName", getString("logics.object.class"), name, objectClass, 1);
        objectClassName.makeLoggable(this, true);

        // записываем в имя имя класса + номер объекта
        dataName.setEventChange(addJProp(string2, addJProp(name.getOld(), objectClass, 1), 1,
                addSFProp("CAST((prm1) as char(50))", StringClass.get(50), 1), 1), 1,
                is(baseClass.named), 1);

        // ----- Пользователи
        // todo : переименовать в соответствии с namingPolicy
        // Авторизация
        userLogin = addDProp(baseGroup, "userLogin", getString("logics.user.login"), StringClass.get(30), customUser);
        loginToUser = addAGProp("loginToUser", getString("logics.user"), userLogin);

        userPassword = addDProp(publicGroup, "userPassword", getString("logics.user.password"), StringClass.get(30), customUser);
        userPassword.setEchoSymbols(true);

        // Текущий пользователь
        currentUser = addProperty(null, new LCP<PropertyInterface>(new CurrentUserFormulaProperty("currentUser", user)));
        currentUserName = addJProp("currentUserName", getString("logics.user.current.user.name"), name, currentUser);

        // Действия по авторизация
        changeUser = addProperty(null, new LAP(new ChangeUserActionProperty("changeUser", customUser)));
        logOut = addProperty(null, new LAP(new LogOutActionProperty("logOut")));

        // Управление сервером приложений
        isServerRestarting = addProperty(null, new LCP<PropertyInterface>(new IsServerRestartingFormulaProperty("isServerRestarting")));

        restartServerAction = addIfAProp(getString("logics.server.stop"), true, isServerRestarting, addRestartActionProp());
        runGarbageCollector = addGarbageCollectorActionProp();
        cancelRestartServerAction = addIfAProp(getString("logics.server.cancel.stop"), isServerRestarting, addCancelRestartActionProp());

        // Настройка форм
        defaultBackgroundColor = addDProp("defaultBackgroundColor", getString("logics.default.background.color"), ColorClass.instance);
        defaultOverrideBackgroundColor = addSUProp("defaultOverrideBackgroundColor", true, getString("logics.default.background.color"), Union.OVERRIDE, addCProp(ColorClass.instance, Color.YELLOW), defaultBackgroundColor);
        defaultForegroundColor = addDProp("defaultForegroundColor", getString("logics.default.foreground.color"), ColorClass.instance);
        defaultOverrideForegroundColor = addSUProp("defaultOverrideForegroundColor", true, getString("logics.default.foreground.color"), Union.OVERRIDE, addCProp(ColorClass.instance, Color.RED), defaultForegroundColor);

        // Словари
        insensitiveDictionary = addDProp(recognizeGroup, "insensitiveDictionary", getString("logics.dictionary.insensitive"), LogicalClass.instance, dictionary);
        entryDictionary = addDProp("entryDictionary", getString("logics.dictionary"), dictionary, dictionaryEntry);
        termDictionary = addDProp(recognizeGroup, "termDictionary", getString("logics.dictionary.termin"), StringClass.get(50), dictionaryEntry);
        insensitiveTermDictionary = addJProp(baseGroup, "insensitiveTermDictionary", upper, termDictionary, 1);
        translationDictionary = addDProp(baseGroup, "translationDictionary", getString("logics.dictionary.translation"), StringClass.get(50), dictionaryEntry);
        translationDictionaryTerm = addCGProp(null, "translationDictionaryTerm", getString("logics.dictionary.translation"), translationDictionary, termDictionary, entryDictionary, 1, termDictionary, 1);
        nameEntryDictionary = addJProp(baseGroup, "nameEntryDictionary", getString("logics.dictionary"), name, entryDictionary, 1);

        insensitiveTranslationDictionaryTerm = addMGProp(baseGroup, "insensitiveTranslationDictionaryTerm", getString("logics.dictionary.translation.insensitive"), translationDictionary, entryDictionary, 1, insensitiveTermDictionary, 1);

        //todo : инлайнить в свои модули

        initNavigators();
    }

    @Override
    public void initIndexes() {
        addIndex(dataName);
    }

    static int generateStaticNewID() {
        return idGenerator.idShift();
    }

    <T extends LP<?, ?>> void registerProperty(T lp) {
        lproperties.add(lp);     // todo [dale]: нужно?
        lp.property.ID = idGenerator.idShift();
    }

    public abstract class MapClassesPropertySet<K, V extends CalcProperty> extends PropertySet {
        protected LinkedHashMap<K, V> properties = new LinkedHashMap<K, V>();

        @Override
        public List<Property> getProperties() {
            return new ArrayList<Property>(properties.values());
        }

        @Override
        protected List<CalcPropertyClassImplement> getProperties(List<ValueClassWrapper> classes) {
            ValueClass[] valueClasses = getClasses(classes);
            V property = getProperty(valueClasses);

            List<?> interfaces = getPropertyInterfaces(property, valueClasses);
            return Collections.singletonList(new CalcPropertyClassImplement(property, classes, interfaces));
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

            SelectionProperty property = new SelectionProperty(sid, classArray, baseLM);
            LCP lp = new LCP<ClassPropertyInterface>(property);
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
        private LCP getStringConcatanationProperty(int intNum) {
            return new LCP<StringConcatenateProperty.Interface>(new StringConcatenateProperty(genSID(), getString("logics.join"), intNum, ", "));
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

            LCP stringConcat = getStringConcatanationProperty(intNum);

            List<JoinProperty.Interface> listInterfaces = JoinProperty.getInterfaces(intNum);
            JoinProperty<ClassPropertyInterface> joinProperty = new JoinProperty(sid, getString("logics.compound.name")+" (" + intNum + ")",
                    listInterfaces, false, mapCalcImplement(stringConcat, readCalcImplements(listInterfaces, joinParams)));
            LCP listJoinProperty = new LCP<JoinProperty.Interface>(joinProperty, listInterfaces);

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
            LCP prop = new LCP<ClassPropertyInterface>(property);
            registerProperty(prop);
            sidToLP.put(sid, prop);
            setParent(property);
            return property;
        }

        public LCP getLP(ValueClass cls) {
            String sid = prefix + cls.getBaseClass().getSID();
            if (!sidToLP.containsKey(sid)) {
                createProperty(new ValueClass[]{cls});
            }
            return (LCP) sidToLP.get(sid);
        }
    }

    // Окна
    public class Windows {
        public NavigatorWindow root;
        public NavigatorWindow toolbar;
        public NavigatorWindow tree;
        public AbstractWindow forms;
        public AbstractWindow log;
        public AbstractWindow status;
        public AbstractWindow relevantForms;
        public AbstractWindow relevantClassForms;
    }

    public Windows windows;

    // Навигаторы
    public NavigatorElement<T> root;
    public NavigatorElement<T> objects;
    public NavigatorElement<T> account;
    public NavigatorElement<T> administration;

    public NavigatorElement<T> application;
    public NavigatorElement<T> security;
    public NavigatorElement<T> systemEvents;
    public NavigatorElement<T> configuration;
    public NavigatorElement<T> catalogs;

    public FormEntity<T> objectForm;

    private void initNavigators() {

        // Окна
        windows = new Windows();
        windows.root = addWindow(new ToolBarNavigatorWindow(JToolBar.HORIZONTAL, "root", getString("logics.window.root"), 0, 0, 100, 6));
        windows.root.titleShown = false;
        windows.root.drawScrollBars = false;

        windows.toolbar = addWindow(new ToolBarNavigatorWindow(JToolBar.VERTICAL, "toolbar", getString("logics.window.toolbar"), 0, 6, 20, 64));
        windows.toolbar.titleShown = false;

        windows.tree = addWindow(new TreeNavigatorWindow("tree", getString("logics.window.tree"), 0, 6, 20, 64));
        windows.tree.titleShown = false;

        windows.forms = addWindow(new AbstractWindow("forms", getString("logics.window.forms"), 20, 20, 80, 79));

        windows.log = addWindow(new AbstractWindow("log", getString("logics.window.log"), 0, 70, 20, 29));

        windows.status = addWindow(new AbstractWindow("status", getString("logics.window.status"), 0, 99, 100, 1));
        windows.status.titleShown = false;

        // временно не показываем
        windows.relevantForms = addWindow(new AbstractWindow("relevantForms", getString("logics.forms.relevant.forms"), 0, 70, 20, 29));
        windows.relevantForms.visible = false;

        windows.relevantClassForms = addWindow(new AbstractWindow("relevantClassForms", getString("logics.forms.relevant.class.forms"), 0, 70, 20, 29));
        windows.relevantClassForms.visible = false;

        // todo : перенести во внутренний класс Navigator, как в Windows
        // Навигатор
        root = addNavigatorElement("root", getString("logics.forms"));
        root.window = windows.root;

        account = addNavigatorElement(root, "account", getString("logics.account"));
        addNavigatorAction(account, "logout", getString("logics.logout"), logOut);
        account.window = windows.toolbar;

        administration = addNavigatorElement(root, "administration", getString("logics.administration"));
        administration.window = windows.toolbar;

        application = addNavigatorElement(administration, "application", getString("logics.administration.application"));
        addFormEntity(new OptionsFormEntity(application, "options"));
        addFormEntity(new IntegrationDataFormEntity(application, "integrationData"));
        addFormEntity(new MigrationDataFormEntity(application, "migrationData"));

        catalogs = addNavigatorElement(administration, "catalogs", getString("logics.administration.catalogs"));

        objects = addNavigatorElement(catalogs, "objects", getString("logics.object"));
        objects.window = windows.tree;

        security = addNavigatorElement(administration, "security", getString("logics.administration.access"));

        configuration = addNavigatorElement(administration, "configuration", getString("logics.administration.config"));

        systemEvents = addNavigatorElement(administration, "systemEvents", getString("logics.administration.events"));
    }

    public void initClassForms() {
        objectForm = baseClass.getBaseClassForm(this);
        objects.add(objectForm);
    }

    @Override
    public String getNamePrefix() {
        return null;
    }

    protected Map<String, CustomClass> sidToClass = new HashMap<String, CustomClass>();

    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////
    /// Properties
    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////

    int idCounter = 0;
    Set<String> idSet = new HashSet<String>();

    boolean isGeneratedSID(String sid) {
        return idSet.contains(sid) || sid.startsWith(DerivedProperty.ID_PREFIX_GEN);
    }

    Collection<LCP[]> checkCUProps = new ArrayList<LCP[]>();

    // объединяет разные по классам св-ва

    Collection<LCP[]> checkSUProps = new ArrayList<LCP[]>();


    @Override
    @IdentityLazy
    public LCP is(ValueClass valueClass) {
        return addProperty(null, new LCP<ClassPropertyInterface>(valueClass.getProperty()));
    }
    @Override
    @IdentityLazy
    public LCP object(ValueClass valueClass) {
        return addJProp(valueClass.toString(), baseLM.and1, 1, is(valueClass), 1);
    }

    protected LAP addRestartActionProp() {
        return BL.addRestartActionProp();
//        return addProperty(null, new LP<ClassPropertyInterface>(new RestartActionProperty(genSID(), "")));
    }

    protected LAP addCancelRestartActionProp() {
        return BL.addCancelRestartActionProp();
//        return addProperty(null, new LP<ClassPropertyInterface>(new CancelRestartActionProperty(genSID(), "")));
    }

    protected LAP addGarbageCollectorActionProp() {
        return BL.addGarbageCollectorActionProp();
    }

    public static class IncrementActionProperty extends UserActionProperty {

        LCP dataProperty;
        LCP maxProperty;
        List<Integer> params;

        IncrementActionProperty(String sID, String caption, LCP dataProperty, LCP maxProperty, Integer[] params) {
            super(sID, caption, dataProperty.getInterfaceClasses());

            this.dataProperty = dataProperty;
            this.maxProperty = maxProperty;
            this.params = Arrays.asList(params);
        }

        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

            // здесь опять учитываем, что порядок тот же
            int i = 0;
            DataObject[] dataPropertyInput = new DataObject[context.getKeyCount()];
            List<DataObject> maxPropertyInput = new ArrayList<DataObject>();

            for (ClassPropertyInterface classInterface : interfaces) {
                dataPropertyInput[i] = context.getKeyValue(classInterface);
                if (params.contains(i + 1)) {
                    maxPropertyInput.add(dataPropertyInput[i]);
                }
                i++;
            }

            Integer maxValue = (Integer) maxProperty.read(context, maxPropertyInput.toArray(new DataObject[0]));
            if (maxValue == null)
                maxValue = 0;
            maxValue += 1;

            dataProperty.change(maxValue, context, dataPropertyInput);
        }
    }

    private static class ChangeUserActionProperty extends AdminActionProperty {

        private ChangeUserActionProperty(String sID, ConcreteValueClass userClass) {
            super(sID, getString("logics.user.change.user"), new ValueClass[]{userClass});
        }

        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            context.emitExceptionIfNotInFormSession();

            DataObject user = context.getSingleKeyValue();
            if (context.getFormInstance().BL.requiredPassword) {
                context.delayUserInterfaction(new UserReloginClientAction(context.getFormInstance().BL.getUserName(user).trim()));
            } else {
                context.getSession().user.changeCurrentUser(user);
                context.delayUserInterfaction(new UserChangedClientAction());
            }
        }
    }

    private class LogOutActionProperty extends AdminActionProperty {
        private LogOutActionProperty(String sID) {
            super(sID, getString("logics.logout"), new ValueClass[]{});
        }

        @Override
        protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            context.delayUserInteraction(new LogOutClientAction());
        }
    }


    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////
    /// Indices
    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////

    protected Map<List<? extends CalcProperty>, Boolean> indexes = new HashMap<List<? extends CalcProperty>, Boolean>();

    public void addIndex(LCP<?>... lps) {
        List<CalcProperty> index = new ArrayList<CalcProperty>();
        for (LCP<?> lp : lps)
            index.add((CalcProperty) lp.property);
        indexes.put(index, lps[0].property.getType() instanceof DataClass);
    }


    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////
    /// Forms
    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////


    class SelectFromListFormEntity extends FormEntity implements FormActionProperty.SelfInstancePostProcessor {
        ObjectEntity[] mainObjects;
        private ObjectEntity selectionObject;
        private ObjectEntity remapObject;
        private final FilterEntity[] remapFilters;

        SelectFromListFormEntity(ObjectEntity remapObject, FilterEntity[] remapFilters, LCP selectionProperty, boolean isSelectionClassFirstParam, ValueClass selectionClass, ValueClass... baseClasses) {
            super(null, null);

            this.remapObject = remapObject;
            this.remapFilters = remapFilters;

            mainObjects = new ObjectEntity[baseClasses.length];
            for (int i = 0; i < baseClasses.length; i++) {
                ValueClass baseClass = baseClasses[i];
                mainObjects[i] = addSingleGroupObject(baseClass, baseGroup);
                mainObjects[i].groupTo.setSingleClassView(ClassViewType.PANEL);
                PropertyDrawEntity objectValue = getPropertyDraw(BaseLogicsModule.this.objectValue, mainObjects[i]);
                if (objectValue != null) {
                    objectValue.setEditType(PropertyEditType.READONLY);
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

            CalcPropertyObjectEntity selectionPropertyObject = addPropertyObject(selectionProperty, selectionObjects);
            PropertyDrawEntity selectionPropertyDraw = addPropertyDraw(null, selectionPropertyObject);

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(
                    new RegularFilterEntity(genID(),
                            new NotFilterEntity(
                                    new CompareFilterEntity(selectionPropertyObject, Compare.EQUALS, true)),
                            getString("logics.object.not.selected.objects"),
                            KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)
                    ), true);
            filterGroup.addFilter(
                    new RegularFilterEntity(genID(),
                            new CompareFilterEntity(selectionPropertyObject, Compare.EQUALS, true),
                            getString("logics.object.selected.objects"),
                            KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)
                    ));
            addRegularFilterGroup(filterGroup);
        }

        public void postProcessSelfInstance(Map<ClassPropertyInterface, DataObject> keys, FormInstance executeForm, FormInstance selfFormInstance) {
            for (FilterEntity filterEntity : remapFilters) {
                selfFormInstance.addFixedFilter(
                        filterEntity.getRemappedFilter(remapObject, selectionObject, executeForm.instanceFactory)
                );
            }
        }
    }

    private class ApplicationFormEntity extends FormEntity {
        public ApplicationFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

            ContainerView pane = design.createContainer(null, null, "pane");
            pane.setType(ContainerType.TABBED_PANE);

            pane.constraints.fillVertical = 1.0;
            pane.constraints.fillHorizontal = 1.0;

            design.mainContainer.addBefore(pane, design.formButtonContainer);

            pane.add(design.createContainer(getString("logics.application.commons"), null, "commons"));

            return design;
        }
    }

    private class OptionsFormEntity extends ApplicationFormEntity {
        private OptionsFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.application.options"));
        }
    }

    private class IntegrationDataFormEntity extends ApplicationFormEntity {
        private IntegrationDataFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.application.integrationData"));
        }
    }

    private class MigrationDataFormEntity extends ApplicationFormEntity {
        private MigrationDataFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.application.migrationData"));
        }
    }
}
