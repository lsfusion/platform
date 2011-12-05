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
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.ContainerView;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.form.window.AbstractWindow;
import platform.server.form.window.NavigatorWindow;
import platform.server.form.window.TreeNavigatorWindow;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.*;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.property.group.PropertySet;
import platform.server.logics.table.TableFactory;
import platform.server.session.DataSession;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.*;

import static platform.server.logics.PropertyUtils.getUParams;
import static platform.server.logics.PropertyUtils.mapImplement;
import static platform.server.logics.PropertyUtils.readImplements;
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
    public ConcreteCustomClass property;
    public ConcreteCustomClass connection;
    public StaticCustomClass connectionStatus;
    public ConcreteCustomClass dictionary;
    public ConcreteCustomClass dictionaryEntry;
    public ConcreteCustomClass table;
    public ConcreteCustomClass tableKey;
    public ConcreteCustomClass tableColumn;

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
    public LP lsoeq2;
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
    public LP sumInteger2;
    public LP sumInteger3;
    public LP subtractInteger2;
    public LP subtractInclInteger2;
    public LP multiplyIntegerBy2;
    protected LP squareInteger;
    protected LP squareDouble;
    protected LP sqrtDouble2;
    public LP divideDouble;
    public LP divideDouble2;
    public LP divideDouble3;
    public LP divideInteger;
    public LP divideNegativeInteger;
    public LP divideInteger0;
    public LP addDate2;
    public LP subtractDate2;
    public LP string2;
    public LP insensitiveString2;
    protected LP concat2;
    public LP percent;
    public LP percent2;
    public LP share2;
    public LP weekInDate;
    public LP monthInDate;
    public LP yearInDate;
    public LP dayInDate;
    public LP dateInTime;
    public LP jumpWorkdays;

    public LP vtrue, actionTrue, vzero;
    public LP vnull;
    public LP positive, negative;

    public LP round0;

    public LP minusInteger;
    public LP minusDouble;

    public LP dumb1;
    public LP dumb2;

    protected LP castText;
    protected LP addText2;

    public LP<?> name;
    public LP<?> date;

    public LP daysInclBetweenDates;
    public LP weeksInclBetweenDates;
    public LP weeksNullInclBetweenDates;

    public LP sumDateWeekFrom;
    public LP sumDateWeekTo;

    protected LP transactionLater;
    public LP currentDate;
    public LP currentMonth;
    public LP currentYear;
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
    public LP equalsObjectBarcode;
    public LP barcodePrefix;
    public LP seekBarcodeAction;
    public LP barcodeNotFoundMessage;
    public LP restartServerAction;
    public LP runGarbageCollector;
    public LP cancelRestartServerAction;
    public LP reverseBarcode;

    public LP userLogin;
    public LP userPassword;
    public LP userFirstName;
    public LP userLastName;
    public LP userMainRole;
    public LP customUserMainRole;
    public LP customUserSIDMainRole;
    public LP nameUserMainRole;
    public LP inUserMainRole;
    public LP userRoleSID;
    public LP userRoleDefaultForms;
    public LP forbidAllUserRoleForms;
    public LP forbidAllUserForm;
    public LP allowAllUserRoleForms;
    public LP allowAllUserForm;
    public LP forbidViewAllUserRoleProperty;
    public LP forbidViewAllUserForm;
    public LP allowViewAllUserRoleProperty;
    public LP allowViewAllUserForm;
    public LP forbidChangeAllUserRoleProperty;
    public LP forbidChangeAllUserForm;
    public LP allowChangeAllUserRoleProperty;
    public LP allowChangeAllUserForm;

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
    public LP userNameConnection;
    public LP connectionComputer;
    public LP connectionCurrentStatus;
    public LP connectionFormCount;
    public LP connectionConnectTime;
    public LP connectionDisconnectTime;
    public LP disconnectConnection;

    public LP policyDescription;
    protected LP<?> nameToPolicy;
    public LP userRolePolicyOrder;
    public LP userPolicyOrder;

    public LP hostname;
    public LP onlyNotZero;

    public LP delete;

    public LP apply;
    public LP cancel;

    public LP objectClass;
    public LP objectClassName;
    public LP classSID;
    public LP dataName;
    public LP navigatorElementSID;
    public LP numberNavigatorElement;
    public LP navigatorElementCaption;

    public LP SIDProperty;
    public LP captionProperty;
    public LP SIDToProperty;
    public LP permitViewUserRoleProperty;
    public LP permitViewUserProperty;
    public LP forbidViewUserRoleProperty;
    public LP forbidViewUserProperty;
    public LP permitChangeUserRoleProperty;
    public LP permitChangeUserProperty;
    public LP forbidChangeUserRoleProperty;
    public LP forbidChangeUserProperty;
    public LP permitViewProperty;
    public LP forbidViewProperty;
    public LP permitChangeProperty;
    public LP forbidChangeProperty;

    public LP SIDToNavigatorElement;
    public LP parentNavigatorElement;
    public LP permitUserRoleForm;
    public LP forbidUserRoleForm;
    public LP permitUserForm;
    public LP forbidUserForm;
    public LP permitForm;
    public LP forbidForm;
    public LP userRoleFormDefaultNumber;
    public LP userFormDefaultNumber;

    public LP sidTable;
    public LP sidTableKey;
    public LP nameTableKey;
    public LP sidTableColumn;
    public LP propertyTableColumn;
    public LP propertyNameTableColumn;
    public LP sidToTable;
    public LP sidToTableKey;
    public LP sidToTableColumn;
    public LP tableTableKey;
    public LP classTableKey;
    public LP tableTableColumn;
    public LP rowsTable;
    public LP quantityTableKey;
    public LP quantityTableColumn;

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
    public LP defaultCountry;

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
    protected LP nameEntryDictionary;
    public LP translationDictionaryTerm;

    private LP selectRoleForms;
    private LP selectUserRoles;

    private LP<ClassPropertyInterface> checkAggregationsAction;
    private LP<ClassPropertyInterface> recalculateAction;
    private LP<ClassPropertyInterface> recalculateFollowsAction;
    private LP<ClassPropertyInterface> packAction;

    public SelectionPropertySet selection;
    protected CompositeNamePropertySet compositeName;
    public ObjectValuePropertySet objectValue;


    // navigators
    public NavigatorElement<T> baseElement;
    public NavigatorElement<T> objectElement;
    public NavigatorElement<T> adminElement;

    public FormEntity<T> dictionaryForm;

    public NavigatorWindow navigatorWindow;
    public AbstractWindow relevantFormsWindow;
    public AbstractWindow relevantClassFormsWindow;
    public AbstractWindow logWindow;
    public AbstractWindow statusWindow;
    public AbstractWindow formsWindow;

    public TableFactory tableFactory;

    public final StringClass formSIDValueClass = StringClass.get(50);
    public final StringClass formCaptionValueClass = StringClass.get(250);

    public final StringClass propertySIDValueClass = StringClass.get(100);
    public final StringClass propertyCaptionValueClass = StringClass.get(250);

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
    public void initClasses() {
        baseClass = addBaseClass("object", getString("logics.object"));

        transaction = addAbstractClass("transaction", getString("logics.transaction"), baseClass);
        barcodeObject = addAbstractClass("barcodeObject", getString("logics.object.barcoded.object"), baseClass);

        emailObject = addAbstractClass("emailObject", getString("logics.object.with.email"), baseClass);

        user = addAbstractClass("user", getString("logics.user"), baseClass);
        customUser = addConcreteClass("customUser", getString("logics.user.ordinary.user"), user, barcodeObject, emailObject);
        systemUser = addConcreteClass("systemUser", getString("logics.user.system.user"), user);
        computer = addConcreteClass("computer", getString("logics.workplace"), baseClass);
        userRole = addConcreteClass("userRole", getString("logics.role"), baseClass.named);

        policy = addConcreteClass("policy", getString("logics.policy.security.policy"), baseClass.named);
        session = addConcreteClass("session", getString("logics.transaction"), baseClass);

        connection = addConcreteClass("connection", getString("logics.connection"), baseClass);
        connectionStatus = addStaticClass("connectionStatus", getString("logics.connection.status"),
                new String[]{"connectedConnection", "disconnectedConnection"},
                new String[]{getString("logics.connection.connected"), getString("logics.connection.disconnected")});

        country = addConcreteClass("country", getString("logics.country"), baseClass.named);

        navigatorElement = addConcreteClass("navigatorElement", getString("logics.navigator.element"), baseClass);
        form = addConcreteClass("form", getString("logics.forms.form"), navigatorElement);
        property = addConcreteClass("property", getString("logics.property"), baseClass);
        dictionary = addConcreteClass("dictionary", getString("logics.dictionary"), baseClass.named);
        dictionaryEntry = addConcreteClass("dictionaryEntry", getString("logics.dictionary.entries"), baseClass);

        table = addConcreteClass("table", getString("logics.tables.table"), baseClass);
        tableKey = addConcreteClass("tableKey", getString("lodics.tables.key"), baseClass);
        tableColumn = addConcreteClass("tableColumn", getString("logics.tables.column"), baseClass);
    }

    @Override
    public void initGroups() {
        rootGroup = addAbstractGroup("rootGroup", getString("logics.groups.rootgroup"), null, false);
        sessionGroup = addAbstractGroup("sessionGroup", getString("logics.groups.sessiongroup"), rootGroup, false);
        publicGroup = addAbstractGroup("publicGroup", getString("logics.groups.publicgroup"), rootGroup, false);
        actionGroup = addAbstractGroup("actionGroup", getString("logics.groups.actiongroup"), rootGroup, false);
        privateGroup = addAbstractGroup("privateGroup", getString("logics.groups.privategroup"), rootGroup, false);
        baseGroup = addAbstractGroup("baseGroup", getString("logics.groups.basegroup"), publicGroup, false);
        idGroup = addAbstractGroup("idGroup", getString("logics.groups.idgroup"), publicGroup, false);
        recognizeGroup = addAbstractGroup("recognizeGroup", getString("logics.groups.recognizegroup"), baseGroup, false);
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

        tableFactory.include("userTable", user);
        tableFactory.include("customUser", customUser);
        tableFactory.include("loginSID", StringClass.get(30), StringClass.get(30));
        tableFactory.include("countryDate", country, DateClass.instance);
        tableFactory.include("objectObjectDate", baseClass, baseClass, DateClass.instance);
        tableFactory.include("country", country, DateClass.instance);
        tableFactory.include("navigatorElement", navigatorElement);
        tableFactory.include("transaction", transaction);
        tableFactory.include("named", baseClass.named);
        tableFactory.include("barcodeObject", barcodeObject);
        tableFactory.include("emailObject", emailObject);
        tableFactory.include("dictionary", dictionary);
        tableFactory.include("dictionaryEntry", dictionaryEntry);

        tableFactory.include("session", session);
        tableFactory.include("connection", connection);
        tableFactory.include("computer", computer);

        tableFactory.include("connectionNavigatorElement", connection, navigatorElement);
        tableFactory.include("userRoleNavigatorElement", userRole, navigatorElement);

        tableFactory.include("tables", table);
        tableFactory.include("tableKey", tableKey);
        tableFactory.include("tableColumn", tableColumn);

        tableFactory.include("customUserRole", customUser, userRole);
        tableFactory.include("userRolePolicy", userRole, policy);
        tableFactory.include("userRoleProperty", userRole, property);
    }

    @Override
    public void initProperties() {
        selection = new SelectionPropertySet();
        sessionGroup.add(selection);

        objectValue = new ObjectValuePropertySet();
        baseGroup.add(objectValue);

        compositeName = new CompositeNamePropertySet();
        privateGroup.add(compositeName);

        classSID = addDProp("classSID", getString("logics.statcode"), StringClass.get(250), baseClass.sidClass);
        dataName = addDProp("name", getString("logics.name"), InsensitiveStringClass.get(110), baseClass.named);

        // математические св-ва
        equals2 = addCFProp("equals2", Compare.EQUALS);
        object1 = addAFProp();
        and1 = addAFProp("and1", false);
        andNot1 = addAFProp(true);
        string2 = addSProp(2);
        insensitiveString2 = addInsensitiveSProp(2);
        concat2 = addCCProp(2);
        groeq2 = addCFProp(Compare.GREATER_EQUALS);
        greater2 = addCFProp("greater2", Compare.GREATER);
        lsoeq2 = addCFProp(Compare.LESS_EQUALS);
        less2 = addCFProp(Compare.LESS);
        greater22 = addJProp(greater2, concat2, 1, 2, concat2, 3, 4);
        less22 = addJProp(less2, concat2, 1, 2, concat2, 3, 4);
        diff2 = addCFProp(Compare.NOT_EQUALS);
        sumDouble2 = addSFProp("sumDouble2", "((prm1)+(prm2))", DoubleClass.instance, 2);
        subtractDouble2 = addSFProp("subtractDouble2", "((prm1)-(prm2))", DoubleClass.instance, 2);
        deltaDouble2 = addSFProp("abs((prm1)-(prm2))", DoubleClass.instance, 2);
        multiplyDouble2 = addMFProp("multiplyDouble2", DoubleClass.instance, 2);
        sumInteger2 = addSFProp("sumInteger2", "((prm1)+(prm2))", IntegerClass.instance, 2);
        sumInteger3 = addSFProp("sumInteger2", "((prm1)+(prm2)+(prm3))", IntegerClass.instance, 3);
        subtractInteger2 = addSFProp("subtractInteger2", "((prm1)-(prm2))", IntegerClass.instance, 2);
        subtractInclInteger2 = addSFProp("subtractInclInteger2", "((prm1)-(prm2)+1)", IntegerClass.instance, 2);
        multiplyIntegerBy2 = addSFProp("((prm1)*2)", IntegerClass.instance, 1);
        squareInteger = addSFProp("(prm1)*(prm1)", IntegerClass.instance, 1);
        squareDouble = addSFProp("(prm1)*(prm1)", DoubleClass.instance, 1);
        sqrtDouble2 = addSFProp("round(sqrt(prm1),2)", DoubleClass.instance, 1);
        divideDouble = addSFProp("((prm1)/(prm2))", DoubleClass.instance, 2);
        divideDouble2 = addSFProp("divideDouble2", "round(CAST((CAST((prm1) as numeric)/(prm2)) as numeric),2)", DoubleClass.instance, 2);
        divideDouble3 = addSFProp("divideDouble3", "round(CAST((CAST((prm1) as numeric)/(prm2)) as numeric),3)", DoubleClass.instance, 2);
        divideInteger = addSFProp("CAST(((prm1)/(prm2)) as integer)", IntegerClass.instance, 2);
        divideNegativeInteger = addSFProp("CASE WHEN (prm1)<0 THEN -CAST(((-(prm1)-1)/(prm2)) as integer) ELSE CAST(((prm1)/(prm2)) as integer) END", IntegerClass.instance, 2);
        divideInteger0 = addSFProp("CAST(round((prm1)/(prm2),0) as integer)", IntegerClass.instance, 2);
        addDate2 = addSFProp("((prm1)+(prm2))", DateClass.instance, 2);
        subtractDate2 = addSFProp("((prm1)-(prm2))", DateClass.instance, 2);
        percent = addSFProp("((prm1)*(prm2)/100)", DoubleClass.instance, 2);
        percent2 = addSFProp("round(CAST(((prm1)*(prm2)/100) as numeric), 2)", DoubleClass.instance, 2);
        share2 = addSFProp("round(CAST(((prm1)/(prm2)*100) as numeric), 2)", DoubleClass.instance, 2);
        jumpWorkdays = addSFProp("jumpWorkdays(prm1, prm2, prm3)", DateClass.instance, 3); //1 - country, 2 - date, 3 - days to jump
        between = addJProp("between", getString("logics.between"), and1, groeq2, 1, 2, groeq2, 3, 1);
        vtrue = addCProp(getString("logics.true"), LogicalClass.instance, true);
        vzero = addCProp("0", DoubleClass.instance, 0);
        vnull = addProperty(privateGroup, new LP<PropertyInterface>(new NullValueProperty()));

        round0 = addSFProp("round(CAST(prm1 as numeric), 0)", DoubleClass.instance, 1);

        minusInteger = addSFProp("(-(prm1))", IntegerClass.instance, 1);
        minusDouble = addSFProp("(-(prm1))", DoubleClass.instance, 1);

        actionTrue = addCProp("ActionTrue", ActionClass.instance, true);

        dumb1 = dumb(1);
        dumb2 = dumb(2);

        castText = addSFProp("CAST((prm1) as text)", TextClass.instance, 1);
        addText2 = addSFProp("((prm1)+(prm2))", TextClass.instance, 2);

        positive = addJProp(greater2, 1, vzero);
        negative = addJProp(less2, 1, vzero);

        weekInDate = addSFProp("(extract(week from (prm1)))", IntegerClass.instance, 1);
        monthInDate = addSFProp("(extract(month from (prm1)))", IntegerClass.instance, 1);
        yearInDate = addSFProp("(extract(year from (prm1)))", IntegerClass.instance, 1);
        dayInDate = addSFProp("(extract(day from (prm1)))", IntegerClass.instance, 1);
        dateInTime = addSFProp("(CAST((prm1) as date))", DateClass.instance, 1);

        delete = addAProp(new DeleteObjectActionProperty(baseClass));

        apply = addAProp(new ApplyActionProperty(BL));
        cancel = addAProp(new CancelActionProperty());

        date = addDProp(baseGroup, "date", getString("logics.date"), DateClass.instance, transaction);

        onlyNotZero = addJProp(andNot1, 1, addJProp(equals2, 1, vzero), 1);
        onlyNotZero.property.isOnlyNotZero = true;

        daysInclBetweenDates = addJProp("daysInclBetweenDates", "Кол-во дней", and(false, false), addJProp(subtractInclInteger2, 2, 1), 1, 2, is(DateClass.instance), 1, is(DateClass.instance), 2);
        weeksInclBetweenDates = addJProp("weeksInclBetweenDates", "Кол-во недель", divideInteger, daysInclBetweenDates, 1, 2, addCProp(IntegerClass.instance, 7));
        weeksNullInclBetweenDates = addJProp("weeksNullInclBetweenDates", "Кол-во недель", onlyNotZero, weeksInclBetweenDates, 1, 2);

        sumDateWeekFrom = addJProp("sumDateWeekFrom", "Дата (с)", and(false, false), addSFProp("((prm1)+(prm2)*7)", DateClass.instance, 2), 1, 2, is(DateClass.instance), 1, is(IntegerClass.instance), 2);
        sumDateWeekTo = addJProp("sumDateWeekTo", "Дата (по)", and(false, false), addSFProp("((prm1)+((prm2)*7+6))", DateClass.instance, 2), 1, 2, is(DateClass.instance), 1, is(IntegerClass.instance), 2);

        betweenDates = addJProp(getString("logics.date.of.doc.between"), between, object(DateClass.instance), 1, object(DateClass.instance), 2, object(DateClass.instance), 3);
        betweenDate = addJProp(getString("logics.date.of.doc.between"), betweenDates, date, 1, 2, 3);

        sidCountry = addDProp(baseGroup, "sidCountry", getString("logics.country.key"), IntegerClass.instance, country);
        generateDatesCountry = addDProp(privateGroup, "generateDatesCountry", getString("logics.day.generate.days.off"), LogicalClass.instance, country);
        sidToCountry = addAGProp("sidToCountry", getString("logics.country"), sidCountry);
        
        isDayOffCountryDate = addDProp(baseGroup, "isDayOffCD", getString("logics.day.off"), LogicalClass.instance, country, DateClass.instance);

        
        workingDay = addJProp(baseGroup, "workingDay", getString("logics.day.working"), andNot1, addCProp(IntegerClass.instance, 1, country, DateClass.instance), 1, 2, isDayOffCountryDate, 1, 2);
        isWorkingDay = addJProp(baseGroup, "isWorkingDay", getString("logics.day.working"), and(false, false), workingDay, 1, 3, groeq2, 3, 2, is(DateClass.instance), 3);
        workingDaysQuantity = addOProp(baseGroup, "workingDaysQuantity", getString("logics.day.working.days"), OrderType.SUM, isWorkingDay, true, true, 1, 2, 1, 3);
        equalsWorkingDaysQuantity = addJProp(baseGroup, "equalsWorkingDaysQuantity", getString("logics.day.equals.quantity.of.working.days"), equals2, object(IntegerClass.instance), 1, workingDaysQuantity, 2, 3, 4);

        transactionLater = addSUProp(getString("logics.transaction.later"), Union.OVERRIDE, addJProp(getString("logics.date.later"), greater2, date, 1, date, 2),
                                     addJProp("", and1, addJProp(getString("logics.date.equals.date"), equals2, date, 1, date, 2), 1, 2, addJProp(getString("logics.transaction.code.later"), greater2, 1, 2), 1, 2));

        hostname = addDProp(baseGroup, "hostname", getString("logics.host.name"), InsensitiveStringClass.get(100), computer);

        currentDate = addDProp(baseGroup, "currentDate", getString("logics.date.current.date"), DateClass.instance);
        currentMonth = addJProp(baseGroup, "currentMonth", getString("logics.date.current.month"), monthInDate, currentDate);
        currentYear = addJProp(baseGroup, "currentYear", getString("logics.date.current.year"), yearInDate, currentDate);
        currentHour = addTProp(Time.HOUR);
        currentMinute = addTProp(Time.MINUTE);
        currentEpoch = addTProp(Time.EPOCH);
        currentDateTime = addTProp(Time.DATETIME);
        currentUser = addProperty(null, new LP<PropertyInterface>(new CurrentUserFormulaProperty(genSID(), user)));
        currentSession = addProperty(null, new LP<PropertyInterface>(new CurrentSessionFormulaProperty(genSID(), session)));
        currentComputer = addProperty(null, new LP<PropertyInterface>(new CurrentComputerFormulaProperty(genSID(), computer)));
        isServerRestarting = addProperty(null, new LP<PropertyInterface>(new IsServerRestartingFormulaProperty(genSID())));
        changeUser = addProperty(null, new LP<ClassPropertyInterface>(new ChangeUserActionProperty(genSID(), customUser)));

        userLogin = addDProp(baseGroup, "userLogin", getString("logics.user.login"), StringClass.get(30), customUser);
        loginToUser = addAGProp("loginToUser", getString("logics.user"), userLogin);
        userPassword = addDProp(baseGroup, "userPassword", getString("logics.user.password"), StringClass.get(30), customUser);

        userFirstName = addDProp(baseGroup, "userFirstName", getString("logics.user.firstname"), StringClass.get(30), customUser);
        userFirstName.setMinimumCharWidth(10);

        userLastName = addDProp(baseGroup, "userLastName", getString("logics.user.lastname"), StringClass.get(30), customUser);
        userLastName.setMinimumCharWidth(10);

        userRoleSID = addDProp(baseGroup, "userRoleSID", getString("logics.user.identificator"), StringClass.get(30), userRole);
        sidToRole = addAGProp(idGroup, "sidToRole", getString("logics.user.role.id"), userRole, userRoleSID);
        inUserRole = addDProp(baseGroup, "inUserRole", getString("logics.user.role.in"), LogicalClass.instance, customUser, userRole);
        userRoleDefaultForms = addDProp(baseGroup, "userRoleDefaultForms", getString("logics.user.displaying.forms.by.default"), LogicalClass.instance, userRole);
        inLoginSID = addJProp("inLoginSID", true, getString("logics.login.has.a.role"), inUserRole, loginToUser, 1, sidToRole, 2);

        email = addDProp(baseGroup, "email", getString("logics.email"), StringClass.get(50), emailObject);
        emailToObject = addAGProp("emailToObject", getString("logics.email.to.object"), email);

        emailUserPassUser = addEAProp(getString("logics.user.password.reminder"), customUser);
        addEARecepient(emailUserPassUser, email, 1);

        generateLoginPassword = addAProp(actionGroup, new GenerateLoginPasswordActionProperty(email, userLogin, userPassword, customUser));

        name = addCUProp(recognizeGroup, "commonName", getString("logics.name"), dataName,
                addJProp(insensitiveString2, userFirstName, 1, userLastName, 1));

        connectionComputer = addDProp("connectionComputer", getString("logics.computer"), computer, connection);
        addJProp(baseGroup, getString("logics.computer"), hostname, connectionComputer, 1);
        connectionUser = addDProp("connectionUser", getString("logics.user"), customUser, connection);
        userNameConnection = addJProp(baseGroup, getString("logics.user"), userLogin, connectionUser, 1);
        connectionCurrentStatus = addDProp("connectionCurrentStatus", getString("logics.connection.status"), connectionStatus, connection);
        addJProp(baseGroup, getString("logics.connection.status"), name, connectionCurrentStatus, 1);

        connectionConnectTime = addDProp(baseGroup, "connectionConnectTime", getString("logics.connection.connect.time"), DateTimeClass.instance, connection);
        connectionDisconnectTime = addDProp(baseGroup, "connectionDisconnectTime", getString("logics.connection.disconnect.time"), DateTimeClass.instance, connection);
        connectionDisconnectTime.setDerivedForcedChange(currentDateTime,
                addJProp(equals2, connectionCurrentStatus, 1, addCProp(connectionStatus, "disconnectedConnection")), 1);
        disconnectConnection = addProperty(null, new LP<ClassPropertyInterface>(new DisconnectActionProperty(BL, this, connection)));
        addJProp(baseGroup, getString("logics.connection.disconnect"), andNot1, getUParams(new LP[]{disconnectConnection, connectionDisconnectTime}, 0));

        connectionFormCount = addDProp(baseGroup, "connectionFormCount", getString("logics.forms.number.of.opened.forms"), IntegerClass.instance, connection, navigatorElement);

        userMainRole = addDProp(idGroup, "userMainRole", getString("logics.user.role.main.role.id"), userRole, user);
        customUserMainRole = addJProp(idGroup, "customUserMainRole", getString("logics.user.role.main.role.id"), and1, userMainRole, 1, is(customUser), 1);
        customUserSIDMainRole = addJProp("customUserSIDMainRole", getString("logics.user.role.main.role.identificator"), userRoleSID, customUserMainRole, 1);
        nameUserMainRole = addJProp(baseGroup, "nameUserMainRole", getString("logics.user.role.main.role"), name, userMainRole, 1);

        inUserMainRole = addSUProp("inUserMainRole", getString("logics.user.role.in"), Union.OVERRIDE,
                         addJProp(equals2, customUserMainRole, 1, 2), inUserRole);

        nameToCountry = addAGProp("nameToCountry", getString("logics.country"), country, name);

        nameToPolicy = addAGProp("nameToPolicy", getString("logics.policy"), policy, name);
        policyDescription = addDProp(baseGroup, "description", getString("logics.policy.description"), StringClass.get(100), policy);

        userRolePolicyOrder = addDProp(baseGroup, "userRolePolicyOrder", getString("logics.policy.order"), IntegerClass.instance, userRole, policy);
        userPolicyOrder = addJProp(baseGroup, "userPolicyOrder", getString("logics.policy.order"), userRolePolicyOrder, userMainRole, 1, 2);

        barcode = addDProp(recognizeGroup, "barcode", getString("logics.barcode"), StringClass.get(13), barcodeObject);

        barcode.setFixedCharWidth(13);
        barcodeToObject = addAGProp("barcodeToObject", getString("logics.object"), barcode);
        barcodeObjectName = addJProp(baseGroup, "barcodeObjectName", getString("logics.object"), name, barcodeToObject, 1);

        equalsObjectBarcode = addJProp(equals2, barcode, 1, 2);

        barcodePrefix = addDProp(baseGroup, "barcodePrefix", getString("logics.barcode.prefix"), StringClass.get(13));

        seekBarcodeAction = addJProp(true, getString("logics.barcode.search"), addSAProp(null), barcodeToObject, 1);
        barcodeNotFoundMessage = addJProp(true, "", and(false, true), addMAProp(getString("logics.barcode.not.found"), getString("logics.error")), is(StringClass.get(13)), 1, barcodeToObject, 1);

        restartServerAction = addJProp(getString("logics.server.stop"), andNot1, addRestartActionProp(), isServerRestarting);
        runGarbageCollector = addGarbageCollectorActionProp();
        cancelRestartServerAction = addJProp(getString("logics.server.cancel.stop"), and1, addCancelRestartActionProp(), isServerRestarting);

        checkAggregationsAction = addProperty(null, new LP<ClassPropertyInterface>(new CheckAggregationsActionProperty(genSID(), getString("logics.check.aggregations"))));
        recalculateAction = addProperty(null, new LP<ClassPropertyInterface>(new RecalculateActionProperty(genSID(), getString("logics.recalculate.aggregations"))));
        recalculateFollowsAction = addProperty(null, new LP<ClassPropertyInterface>(new RecalculateFollowsActionProperty(genSID(), getString("logics.recalculate.follows"))));
        packAction = addProperty(null, new LP<ClassPropertyInterface>(new PackActionProperty(genSID(), getString("logics.tables.pack"))));

        currentUserName = addJProp(getString("logics.user.current.user.name"), name, currentUser);

        reverseBarcode = addSDProp("reverseBarcode", getString("logics.barcode.reverse"), LogicalClass.instance);

        objectClass = addProperty(null, new LP<ClassPropertyInterface>(new ObjectClassProperty(genSID(), baseClass)));
        objectClassName = addJProp(baseGroup, "objectClassName", getString("logics.object.class"), name, objectClass, 1);

        navigatorElementSID = addDProp(baseGroup, "navigatorElementSID", getString("logics.forms.code"), formSIDValueClass, navigatorElement);
        numberNavigatorElement = addDProp(baseGroup, "numberNavigatorElement", getString("logics.number"), IntegerClass.instance, navigatorElement);
        navigatorElementCaption = addDProp(baseGroup, "navigatorElementCaption", getString("logics.forms.name"), formCaptionValueClass, navigatorElement);
        SIDToNavigatorElement = addAGProp("SIDToNavigatorElement", getString("logics.forms.form"), navigatorElementSID);
        parentNavigatorElement = addDProp("parentNavigatorElement", getString("logics.forms.parent.form"), navigatorElement, navigatorElement);

        SIDProperty = addDProp(baseGroup, "SIDProperty", getString("logics.property.sid"), propertySIDValueClass, property);
        captionProperty = addDProp(baseGroup, "captionProperty", getString("logics.property.caption"), propertyCaptionValueClass, property);
        SIDToProperty = addAGProp("SIDToProperty", getString("logics.property"), SIDProperty);
        permitViewUserRoleProperty = addDProp(baseGroup, "permitViewUserRoleProperty", getString("logics.policy.permit.property.view"), LogicalClass.instance, userRole, property);
        permitViewUserProperty = addJProp(baseGroup, "permitViewUserProperty", getString("logics.policy.permit.property.view"), permitViewUserRoleProperty, userMainRole, 1, 2);
        forbidViewUserRoleProperty = addDProp(baseGroup, "forbidViewUserRoleProperty", getString("logics.policy.forbid.property.view"), LogicalClass.instance, userRole, property);
        forbidViewUserProperty = addJProp(baseGroup, "forbidViewUserProperty", getString("logics.policy.forbid.property.view"), forbidViewUserRoleProperty, userMainRole, 1, 2);
        permitChangeUserRoleProperty = addDProp(baseGroup, "permitChangeUserRoleProperty", getString("logics.policy.permit.property.change"), LogicalClass.instance, userRole, property);
        permitChangeUserProperty = addJProp(baseGroup, "permitChangeUserProperty", getString("logics.policy.permit.property.change"), permitChangeUserRoleProperty, userMainRole, 1, 2);
        forbidChangeUserRoleProperty = addDProp(baseGroup, "forbidChangeUserRoleProperty", getString("logics.policy.forbid.property.change"), LogicalClass.instance, userRole, property);
        forbidChangeUserProperty = addJProp(baseGroup, "forbidChangeUserProperty", getString("logics.policy.forbid.property.change"), forbidChangeUserRoleProperty, userMainRole, 1, 2);
        permitViewProperty = addDProp(baseGroup, "permitViewProperty", getString("logics.policy.permit.property.view"), LogicalClass.instance, property);
        forbidViewProperty = addDProp(baseGroup, "forbidViewProperty", getString("logics.policy.forbid.property.view"), LogicalClass.instance, property);
        permitChangeProperty = addDProp(baseGroup, "permitChangeProperty", getString("logics.policy.permit.property.change"), LogicalClass.instance, property);
        forbidChangeProperty = addDProp(baseGroup, "forbidChangeProperty", getString("logics.policy.forbid.property.change"), LogicalClass.instance, property);
        permitUserRoleForm = addDProp(baseGroup, "permitUserRoleForm", getString("logics.forms.permit.form"), LogicalClass.instance, userRole, navigatorElement);
        permitUserForm = addJProp(baseGroup, "permitUserForm", getString("logics.forms.permit.form"), permitUserRoleForm, userMainRole, 1, 2);
        forbidUserRoleForm = addDProp(baseGroup, "permissionUserRoleForm", getString("logics.forms.prohibit.form"), LogicalClass.instance, userRole, navigatorElement);
        forbidUserForm = addJProp(baseGroup, "permissionUserForm", getString("logics.forms.prohibit.form"), forbidUserRoleForm, userMainRole, 1, 2);
        permitForm = addDProp(baseGroup, "permitForm", getString("logics.forms.permit.form"), LogicalClass.instance, navigatorElement);
        forbidForm = addDProp(baseGroup, "forbidForm", getString("logics.forms.prohibit.form"), LogicalClass.instance, navigatorElement);

        allowAllUserRoleForms = addDProp(baseGroup, "allowAllUserRoleForms", getString("logics.user.allow.all.user.form"), LogicalClass.instance, userRole);
        allowAllUserForm = addJProp(baseGroup, "allowAllUserForm", getString("logics.user.allow.all.user.form"), allowAllUserRoleForms, userMainRole, 1);
        forbidAllUserRoleForms = addDProp(baseGroup, "forbidAllUserRoleForms", getString("logics.user.forbid.all.user.form"), LogicalClass.instance, userRole);
        forbidAllUserForm = addJProp(baseGroup, "forbidAllUserForm", getString("logics.user.forbid.all.user.form"), forbidAllUserRoleForms, userMainRole, 1);

        allowViewAllUserRoleProperty = addDProp(baseGroup, "allowViewAllUserRoleProperty", getString("logics.user.allow.view.all.property"), LogicalClass.instance, userRole);
        allowViewAllUserForm = addJProp(baseGroup, "allowViewAllUserForm", getString("logics.user.allow.view.all.property"), allowViewAllUserRoleProperty, userMainRole, 1);
        forbidViewAllUserRoleProperty = addDProp(baseGroup, "forbidViewAllUserRoleProperty", getString("logics.user.forbid.view.all.property"), LogicalClass.instance, userRole);
        forbidViewAllUserForm = addJProp(baseGroup, "forbidViewAllUserForm", getString("logics.user.forbid.view.all.property"), forbidViewAllUserRoleProperty, userMainRole, 1);

        allowChangeAllUserRoleProperty = addDProp(baseGroup, "allowChangeAllUserRoleProperty", getString("logics.user.allow.change.all.property"), LogicalClass.instance, userRole);
        allowChangeAllUserForm = addJProp(baseGroup, "allowChangeAllUserForm", getString("logics.user.allow.change.all.property"), allowChangeAllUserRoleProperty, userMainRole, 1);
        forbidChangeAllUserRoleProperty = addDProp(baseGroup, "forbidChangeAllUserRoleProperty", getString("logics.user.forbid.change.all.property"), LogicalClass.instance, userRole);
        forbidChangeAllUserForm = addJProp(baseGroup, "forbidChangeAllUserForm", getString("logics.user.forbid.change.all.property"), forbidChangeAllUserRoleProperty, userMainRole, 1);

        userRoleFormDefaultNumber = addDProp(baseGroup, "userRoleFormDefaultNumber", getString("logics.forms.default.number"), IntegerClass.instance, userRole, navigatorElement);
        userFormDefaultNumber = addJProp(baseGroup, "userFormDefaultNumber", getString("logics.forms.default.number"), userRoleFormDefaultNumber, userMainRole, 1, 2);
        userDefaultForms = addJProp(baseGroup, "userDefaultForms", getString("logics.user.displaying.forms.by.default"), userRoleDefaultForms, userMainRole, 1);
//        permissionUserForm = addDProp(baseGroup, "permissionUserForm", "Запретить форму", LogicalClass.instance, user, navigatorElement);

        selectUserRoles = addSelectFromListAction(null, getString("logics.user.role.edit.roles"), inUserRole, userRole, customUser);
        //selectRoleForms = addSelectFromListAction(null, "Редактировать формы", permissionUserRoleForm, navigatorElement, userRole);

        sidTable = addDProp(baseGroup, "sidTable", getString("logics.tables.name"), StringClass.get(100), table);
        sidTableKey = addDProp("sidTableKey", getString("logics.tables.key.sid"), StringClass.get(100), tableKey);
        nameTableKey = addDProp(baseGroup, "nameTableKey", getString("logics.tables.key.name"), StringClass.get(20), tableKey);
        sidTableColumn = addDProp(baseGroup, "sidTableColumn", getString("logics.tables.column.name"), StringClass.get(100), tableColumn);
        propertyTableColumn = addJProp("propertyTableColumn", getString("logics.property"), SIDToProperty, sidTableColumn, 1);
        propertyNameTableColumn = addJProp(baseGroup, "propertyNameTableColumn", getString("logics.tables.property.name"), captionProperty, propertyTableColumn, 1);
        sidToTable = addAGProp("sidToTable", getString("logics.tables.table"), sidTable);
        sidToTableKey = addAGProp("sidToTableKey", getString("lodics.tables.key"), sidTableKey);
        sidToTableColumn = addAGProp("sidToTableColumn", getString("logics.tables.column"), sidTableColumn);
        tableTableKey = addDProp("tableTableKey", getString("logics.tables.table"), table, tableKey);
        classTableKey = addDProp(baseGroup, "classTableKey", getString("logics.tables.key.class"), StringClass.get(40), tableKey);
        tableTableColumn = addDProp("tableTableColumn", getString("logics.tables.table"), table, tableColumn);
        rowsTable = addDProp(baseGroup, "rowsTable", getString("logics.tables.rows"), IntegerClass.instance, table);
        quantityTableKey = addDProp(baseGroup, "quantityTableKey", getString("logics.tables.key.distinct.quantity"), IntegerClass.instance, tableKey);
        quantityTableColumn = addDProp(baseGroup, "quantityTableColumn", getString("logics.tables.column.values.quantity"), IntegerClass.instance, tableColumn);

        // заполним сессии
        LP sessionUser = addDProp("sessionUser", getString("logics.session.user"), user, session);
        sessionUser.setDerivedChange(currentUser, true, is(session), 1);
        addJProp(baseGroup, getString("logics.session.user"), name, sessionUser, 1);
        LP sessionDate = addDProp(baseGroup, "sessionDate", getString("logics.session.date"), DateTimeClass.instance, session);
        sessionDate.setDerivedChange(currentDateTime, true, is(session), 1);

        objectByName = addMGProp(idGroup, "objectByName", getString("logics.object.name"), object(baseClass.named), name, 1);
        seekObjectName = addJProp(true, getString("logics.object.search"), addSAProp(null), objectByName, 1);

        webHost = addDProp("webHost", getString("logics.host.webhost"), StringClass.get(50));

        smtpHost = addDProp("smtpHost", getString("logics.host.smtphost"), StringClass.get(50));
        smtpPort = addDProp("smtpPort", getString("logics.host.smtpport"), StringClass.get(10));
        emailAccount = addDProp("emailAccount", getString("logics.email.accountname"), StringClass.get(50));
        emailPassword = addDProp("emailPassword", getString("logics.email.password"), StringClass.get(50));
        emailBlindCarbonCopy = addDProp("emailBlindCarbonCopy", getString("logics.email.copy.bcc"), StringClass.get(50));
        fromAddress = addDProp("fromAddress", getString("logics.email.sender"), StringClass.get(50));

        disableEmail = addDProp("disableEmail", getString("logics.email.disable.email.sending"), LogicalClass.instance);

        defaultCountry = addDProp("defaultCountry", getString("logics.country.default.country"), country);

        entryDictionary = addDProp("entryDictionary", getString("logics.dictionary"), dictionary, dictionaryEntry);
        termDictionary = addDProp(recognizeGroup, "termDictionary", getString("logics.dictionary.termin"), StringClass.get(50), dictionaryEntry);
        translationDictionary = addDProp(baseGroup, "translationDictionary", getString("logics.dictionary.translation"), StringClass.get(50), dictionaryEntry);
        translationDictionaryTerm = addCGProp(null, "translationDictionayTerm", getString("logics.dictionary.translation"), translationDictionary, termDictionary, entryDictionary, 1, termDictionary, 1);
        nameEntryDictionary = addJProp(baseGroup, "nameEntryDictionary", getString("logics.dictionary"), name, entryDictionary, 1);
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
            return new LP<StringConcatenateProperty.Interface>(new StringConcatenateProperty(genSID(), getString("logics.join"), intNum, ", "));
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

            JoinProperty<ClassPropertyInterface> joinProperty = new JoinProperty(sid, getString("logics.compound.name")+" (" + intNum + ")", intNum, false);
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
        baseClass.named.setListForm(new NamedListFormEntity(this, baseClass.named));

        navigatorWindow = new TreeNavigatorWindow("navigator", getString("logics.navigator"), 0, 0, 20, 70);
        relevantFormsWindow = new AbstractWindow("relevantForms", getString("logics.forms.relevant.forms"), 0, 70, 20, 29);
        relevantClassFormsWindow = new AbstractWindow("relevantClassForms", getString("logics.forms.relevant.class.forms"), 0, 70, 20, 29);
        logWindow = new AbstractWindow("log", getString("logics.log"), 0, 70, 20, 29);
        statusWindow = new AbstractWindow("status", getString("logics.status"), 0, 99, 100, 1);
        statusWindow.titleShown = false;
        formsWindow = new AbstractWindow("forms", getString("logics.forms"), 20, 20, 80, 79);

        baseElement = new NavigatorElement("baseElement", getString("logics.forms"));
        baseElement.window = navigatorWindow;
        adminElement = new NavigatorElement(baseElement, "adminElement",  getString("logics.administration"));

        objectElement = baseClass.getBaseClassForm(this);
        adminElement.add(objectElement);

        addFormEntity(new UserPolicyFormEntity(adminElement, "userPolicyForm"));
        addFormEntity(new SecurityPolicyFormEntity(adminElement, "securityPolicyForm"));
        addFormEntity(new ConnectionsFormEntity(adminElement, "connectionsForm"));
        addFormEntity(new PhysicalModelFormEntity(adminElement, "physicalModelForm"));
        addFormEntity(new AdminFormEntity(adminElement, "adminForm"));
        addFormEntity(new DaysOffFormEntity(adminElement, "daysOffForm"));

        dictionaryForm = addFormEntity(new DictionariesFormEntity(adminElement, "dictionariesForm"));

        addFormEntity(new RemindUserPassFormEntity(adminElement, "remindPasswordLetter"));
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

    protected LP addGarbageCollectorActionProp() {
        return BL.addGarbageCollectorActionProp();
    }

    public static class SeekActionProperty extends ActionProperty {

        Property property;

        SeekActionProperty(String sID, String caption, ValueClass[] classes, Property property) {
            super(sID, caption, classes);
            this.property = property;
        }

        public void execute(ExecutionContext context) throws SQLException {
            FormInstance<?> form = context.getFormInstance();
            Collection<ObjectInstance> objects;
            if (property != null)
                objects = form.instanceFactory.getInstance(form.entity.getPropertyObject(property)).mapping.values();
            else
                objects = form.getObjects();
            for (Map.Entry<ClassPropertyInterface, DataObject> key : context.getKeys().entrySet()) {
                if (context.getObjectInstance(key.getKey()) == null) {
                    for (ObjectInstance object : objects) {
                        ConcreteClass keyClass = context.getSession().getCurrentClass(key.getValue());
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
            return FileActionClass.getInstance(false, fileClass.isCustom(), fileClass.toString(), fileClass.getExtensions());
        }

        public void execute(ExecutionContext context) throws SQLException {
            DataObject[] objects = new DataObject[context.getKeyCount()];
            int i = 0; // здесь опять учитываем, что порядок тот же
            for (ClassPropertyInterface classInterface : interfaces)
                objects[i++] = context.getKeyValue(classInterface);
            fileProperty.execute(context.getValueObject(), context, objects);
        }

        @Override
        public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
            super.proceedDefaultDesign(propertyView, view);
            propertyView.design.setIconPath("load.png");
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

        public void execute(ExecutionContext context) throws SQLException {
            DataObject[] objects = new DataObject[context.getKeyCount()];
            int i = 0; // здесь опять учитываем, что порядок тот же
            for (ClassPropertyInterface classInterface : interfaces)
                objects[i++] = context.getKeyValue(classInterface);
            byte[] fullData = (byte[]) fileProperty.read(context, objects);
            if (getFileClass().isCustom()) {
                context.addAction(new OpenFileClientAction(BaseUtils.getFile(fullData), BaseUtils.getExtension(fullData)));
            } else
                context.addAction(new OpenFileClientAction(fullData, BaseUtils.firstWord(getFileClass().getExtensions(), ",")));
        }

        @Override
        public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
            super.proceedDefaultDesign(propertyView, view);
            propertyView.design.setIconPath("open.png");
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

        public void execute(ExecutionContext context) throws SQLException {

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

            dataProperty.execute(maxValue, context, dataPropertyInput);
        }
    }

    private static class ChangeUserActionProperty extends ActionProperty {

        private ChangeUserActionProperty(String sID, ConcreteValueClass userClass) {
            super(sID, getString("logics.user.change.user"), new ValueClass[]{userClass});
        }

        @Override
        public DataClass getValueClass() {
            return LogicalClass.instance;
        }

        public void execute(ExecutionContext context) throws SQLException {
            DataObject user = context.getSingleKeyValue();
            if (context.getFormInstance().BL.requiredPassword) {
                context.addAction(new UserReloginClientAction(context.getFormInstance().BL.getUserName(user).trim()));
            } else {
                context.getSession().user.changeCurrentUser(user);
                context.addAction(new UserChangedClientAction());
            }
        }
    }

    private class CheckAggregationsActionProperty extends ActionProperty {
        private CheckAggregationsActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
        }

        @Override
        public void execute(ExecutionContext context) throws SQLException {
            SQLSession sqlSession = context.getSession().sql;

            sqlSession.startTransaction();
            String message = BL.checkAggregations(sqlSession);
            sqlSession.commitTransaction();

            context.addAction(new MessageClientAction(getString("logics.check.aggregation.was.completed")+'\n'+'\n'+message, getString("logics.checking.aggregations"), true));
        }
    }

    private class RecalculateActionProperty extends ActionProperty {
        private RecalculateActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
        }

        @Override
        public void execute(ExecutionContext context) throws SQLException {
            SQLSession sqlSession = context.getSession().sql;

            sqlSession.startTransaction();
            BL.recalculateAggregations(sqlSession, BL.getAggregateStoredProperties());
            sqlSession.commitTransaction();

            context.addAction(new MessageClientAction(getString("logics.recalculation.was.completed"), getString("logics.recalculation.aggregations")));
        }
    }

    private class PackActionProperty extends ActionProperty {
        private PackActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
        }

        @Override
        public void execute(ExecutionContext context) throws SQLException {
            SQLSession sqlSession = context.getSession().sql;

            sqlSession.startTransaction();
            BL.packTables(sqlSession, tableFactory.getImplementTables().values());
            sqlSession.commitTransaction();

            context.addAction(new MessageClientAction(getString("logics.tables.packing.completed"), getString("logics.tables.packing")));
        }
    }

    private class RecalculateFollowsActionProperty extends ActionProperty {
        private RecalculateFollowsActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
        }

        @Override
        public void execute(ExecutionContext context) throws SQLException {
            DataSession session = BL.createSession();
            session.resolveFollows(BL, true);
            session.apply(BL);
            session.close();

            context.addAction(new MessageClientAction(getString("logics.recalculation.was.completed"), getString("logics.recalculation.follows")));
        }
    }

    private class RecalculateStatsActionProperty extends ActionProperty {
        private RecalculateStatsActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
        }

        @Override
        public void execute(ExecutionContext context) throws SQLException {
            BL.recalculateStats();
        }
    }

    public static class MessageActionProperty extends ActionProperty {
        private String message;

        MessageActionProperty(String message, String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
            this.message = message;
        }

        public void execute(ExecutionContext context) throws SQLException {
            context.addAction(new MessageClientAction(message, caption));
        }
    }

    class AddBarcodeActionProperty extends ActionProperty {

        ConcreteCustomClass customClass;
        Property<?> addProperty;

        AddBarcodeActionProperty(ConcreteCustomClass customClass, Property addProperty, String sID) {
            super(sID, getString("logics.add")+" [" + customClass + "] " + getString("logics.add.by.barcode"), new ValueClass[]{StringClass.get(13)});

            this.customClass = customClass;
            this.addProperty = addProperty;
        }

        public void execute(ExecutionContext context) throws SQLException {
            if (addProperty.read(context) != null) {
                String barString = (String) context.getSingleKeyObject();
                if (barString.trim().length() != 0) {
                    addProperty.execute(context, null);
                    barcode.execute(barString, context, context.addObject(customClass));
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
            super(parent, sID, getString("logics.user.users"));

            ObjectEntity objUser = addSingleGroupObject(customUser, selection, baseGroup, true);
            ObjectEntity objRole = addSingleGroupObject(userRole, baseGroup, true);
            getPropertyDraw(userRoleDefaultForms).shouldBeLast = true;

            addObjectActions(this, objUser);

            addPropertyDraw(objUser, objRole, baseGroup, true);

            addPropertyDraw(selectUserRoles, objRole.groupTo, objUser).forceViewType = ClassViewType.PANEL;

            addFixedFilter(new CompareFilterEntity(addPropertyObject(inUserRole, objUser, objRole), Compare.EQUALS, true));
        }
    }

    private class SecurityPolicyFormEntity extends FormEntity {

        private ObjectEntity objUserRole;
        private ObjectEntity objPolicy;
        private ObjectEntity objForm;
        private ObjectEntity objTreeForm;
        private TreeGroupEntity treeFormObject;
        private ObjectEntity objProperty;
        private ObjectEntity objDefaultForm;
        private ObjectEntity objTreeDefaultForm;
        private TreeGroupEntity treeDefaultForm;
        private ObjectEntity objDefaultProperty;

        protected SecurityPolicyFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.policy.security.policy"));

            objUserRole = addSingleGroupObject(userRole, baseGroup, true);
            objPolicy = addSingleGroupObject(policy, getString("logics.policy.additional.policies"), baseGroup, true);
            objForm = addSingleGroupObject(navigatorElement, getString("logics.grid"), true);
            objTreeForm = addSingleGroupObject(navigatorElement, getString("logics.tree"), true);
            objProperty = addSingleGroupObject(property, getString("logics.property.properties"), true);
            objDefaultForm = addSingleGroupObject(navigatorElement, getString("logics.grid"), true);
            objTreeDefaultForm = addSingleGroupObject(navigatorElement, getString("logics.tree"), true);
            objDefaultProperty = addSingleGroupObject(property, getString("logics.property.properties"), true);

            objTreeForm.groupTo.setIsParents(addPropertyObject(parentNavigatorElement, objTreeForm));
            treeFormObject = addTreeGroupObject(objTreeForm.groupTo);

            objTreeDefaultForm.groupTo.setIsParents(addPropertyObject(parentNavigatorElement, objTreeDefaultForm));
            treeDefaultForm = addTreeGroupObject(objTreeDefaultForm.groupTo);

            addObjectActions(this, objUserRole);

            addPropertyDraw(new LP[]{navigatorElementCaption, navigatorElementSID, numberNavigatorElement}, objForm);
            addPropertyDraw(new LP[]{navigatorElementCaption, navigatorElementSID, numberNavigatorElement}, objTreeForm);
            addPropertyDraw(objUserRole, objPolicy, baseGroup, true);
            addPropertyDraw(objUserRole, objForm, permitUserRoleForm, forbidUserRoleForm);
            addPropertyDraw(objUserRole, objTreeForm, permitUserRoleForm, forbidUserRoleForm);
            addPropertyDraw(forbidUserRoleForm, objUserRole, objTreeForm).toDraw = objUserRole.groupTo;
            addPropertyDraw(objUserRole, objForm, userRoleFormDefaultNumber);
            addPropertyDraw(objUserRole, objTreeForm, userRoleFormDefaultNumber);
            addPropertyDraw(new LP[]{captionProperty, SIDProperty}, objProperty);
            addPropertyDraw(objUserRole, objProperty, permitViewUserRoleProperty, forbidViewUserRoleProperty, permitChangeUserRoleProperty, forbidChangeUserRoleProperty);

            addPropertyDraw(new LP[]{navigatorElementCaption, navigatorElementSID, numberNavigatorElement, permitForm, forbidForm}, objDefaultForm);
            addPropertyDraw(new LP[]{navigatorElementCaption, navigatorElementSID, numberNavigatorElement, permitForm, forbidForm}, objTreeDefaultForm);
            addPropertyDraw(new LP[]{captionProperty, SIDProperty}, objDefaultProperty);
            addPropertyDraw(objDefaultProperty, permitViewProperty, forbidViewProperty, permitChangeProperty, forbidChangeProperty);

            setReadOnly(navigatorElementSID, true);
            setReadOnly(navigatorElementCaption, true);

            PropertyDrawEntity balanceDraw = getPropertyDraw(userRolePolicyOrder, objPolicy.groupTo);
            PropertyDrawEntity sidDraw = getPropertyDraw(userRoleSID, objUserRole.groupTo);
            balanceDraw.addColumnGroupObject(objUserRole.groupTo);
            balanceDraw.setPropertyCaption(sidDraw.propertyObject);

            addDefaultOrder(getPropertyDraw(numberNavigatorElement, objTreeForm.groupTo), true);
            addDefaultOrder(getPropertyDraw(numberNavigatorElement, objTreeDefaultForm.groupTo), true);
            addDefaultOrder(getPropertyDraw(SIDProperty, objProperty.groupTo), true);
            addDefaultOrder(getPropertyDraw(SIDProperty, objDefaultProperty.groupTo), true);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            ContainerView container = design.createContainer();
            container.tabbedPane = true;

            ContainerView defaultPolicyContainer = design.createContainer(getString("logics.policy.default"));
            ContainerView defaultFormsContainer = design.createContainer(getString("logics.forms"));
            defaultFormsContainer.tabbedPane = true;
            defaultFormsContainer.add(design.getTreeContainer(treeDefaultForm));
            defaultFormsContainer.add(design.getGroupObjectContainer(objDefaultForm.groupTo));
            defaultPolicyContainer.tabbedPane = true;
            defaultPolicyContainer.add(defaultFormsContainer);
            defaultPolicyContainer.add(design.getGroupObjectContainer(objDefaultProperty.groupTo));

            ContainerView rolesContainer = design.createContainer(getString("logics.policy.roles"));
            ContainerView rolePolicyContainer = design.createContainer();
            rolePolicyContainer.tabbedPane = true;
            ContainerView formsContainer = design.createContainer(getString("logics.forms"));
            formsContainer.tabbedPane = true;
            formsContainer.add(design.getTreeContainer(treeFormObject));
            formsContainer.add(design.getGroupObjectContainer(objForm.groupTo));
            rolePolicyContainer.add(formsContainer);
            rolePolicyContainer.add(design.getGroupObjectContainer(objProperty.groupTo));
            rolesContainer.add(design.getGroupObjectContainer(objUserRole.groupTo));
            rolesContainer.add(rolePolicyContainer);

            container.add(defaultPolicyContainer);
            container.add(rolesContainer);
            container.add(design.getGroupObjectContainer(objPolicy.groupTo));

            design.getMainContainer().add(0, container);

            return design;
        }
    }

    private class ConnectionsFormEntity extends FormEntity {
        protected ConnectionsFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.connection.server.connections"));

            ObjectEntity objConnection = addSingleGroupObject(connection, baseGroup, true);
            ObjectEntity objForm = addSingleGroupObject(navigatorElement, getString("logics.forms.opened.forms"), baseGroup, true);

//            setReadOnly(baseGroup, true);

            addPropertyDraw(objConnection, objForm, baseGroup, true);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(connectionFormCount, objConnection, objForm), Compare.GREATER, 0));

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(connectionCurrentStatus, objConnection), Compare.EQUALS, connectionStatus.getDataObject("connectedConnection")),
                    getString("logics.connection.active.connections"),
                    KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
            addRegularFilterGroup(filterGroup);
        }
    }

    class PhysicalModelFormEntity extends FormEntity{
        protected PhysicalModelFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.tables.physical.model"));

            ObjectEntity objTable = addSingleGroupObject(table, getString("logics.tables.tables"), baseGroup);
            ObjectEntity objKey = addSingleGroupObject(tableKey, getString("lodics.tables.keys"), baseGroup);
            ObjectEntity objColumn = addSingleGroupObject(tableColumn, getString("logics.tables.columns"), baseGroup);

            addPropertyDraw(addAProp(new RecalculateStatsActionProperty("recalculateStats", getString("logics.tables.recalculate.stats"))));

            setReadOnly(propertyNameTableColumn, true);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(tableTableKey, objKey), Compare.EQUALS, objTable));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(tableTableColumn, objColumn), Compare.EQUALS, objTable));
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
            super(parent, sID, getString("logics.global.parameters"));

            addPropertyDraw(new LP[]{smtpHost, smtpPort, fromAddress, emailAccount, emailPassword, emailBlindCarbonCopy, disableEmail, webHost, defaultCountry, barcodePrefix, restartServerAction, cancelRestartServerAction, checkAggregationsAction, recalculateAction, recalculateFollowsAction, packAction, runGarbageCollector});
        }
    }

    private class DaysOffFormEntity extends FormEntity {
        ObjectEntity objDays;

        public DaysOffFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.days.off"));

            ObjectEntity objCountry = addSingleGroupObject(country, getString("logics.country"));
            objCountry.groupTo.initClassView = ClassViewType.PANEL;

            objDays = addSingleGroupObject(DateClass.instance, getString("logics.day"));

            ObjectEntity objNewDate = addSingleGroupObject(DateClass.instance, getString("logics.date"));
            objNewDate.groupTo.setSingleClassView(ClassViewType.PANEL);

            addPropertyDraw(objCountry, baseGroup);
            addPropertyDraw(objDays, baseLM.objectValue);
            addPropertyDraw(isDayOffCountryDate, objCountry, objDays);
            addPropertyDraw(objNewDate, baseLM.objectValue);
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
            super(parent, sID, getString("logics.dictionary.dictionaries"));

            ObjectEntity objDict = addSingleGroupObject(dictionary, getString("logics.dictionary"));
            objDict.groupTo.initClassView = ClassViewType.PANEL;
            ObjectEntity objDictEntry = addSingleGroupObject(dictionaryEntry, getString("logics.dictionary.entries"));

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
            super(parent, sID, getString("logics.user.password.remind"), true);

            objUser = addSingleGroupObject(1, "customUser", customUser, userLogin, userPassword, name);
            objUser.groupTo.initClassView = ClassViewType.PANEL;

            addInlineEAForm(emailUserPassUser, this, objUser, 1);

            setReadOnly(true);
        }
    }

    private class NamedListFormEntity extends ListFormEntity {
        public ObjectEntity objObjectName;


        public NamedListFormEntity(BaseLogicsModule LM, CustomClass cls, String sID, String caption) {
            super(LM, cls, sID, caption);

            objObjectName = addSingleGroupObject(StringClass.get(50), getString("logics.search.by.name.beginning"), objectValue);
            objObjectName.groupTo.setSingleClassView(ClassViewType.PANEL);

            //двигаем в начало
            groups.remove(objObjectName.groupTo);
            groups.add(0, objObjectName.groupTo);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(name, object), Compare.START_WITH, objObjectName));
        }

        public NamedListFormEntity(BaseLogicsModule LM, CustomClass cls) {
            this(LM, cls, "namedListForm_" + cls.getSID(), cls.caption);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();
            design.get(getPropertyDraw(objectValue, objObjectName)).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0);
            return design;
        }

    }


}
