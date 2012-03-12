package platform.server.logics;

import org.apache.log4j.Logger;
import platform.base.BaseUtils;
import platform.base.identity.DefaultIDGenerator;
import platform.base.identity.IDGenerator;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.action.*;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.server.caches.IdentityLazy;
import platform.server.classes.*;
import platform.server.data.SQLSession;
import platform.server.data.Time;
import platform.server.data.Union;
import platform.server.data.expr.query.PartitionType;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.*;
import platform.server.form.instance.CustomObjectInstance;
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
import platform.server.logics.property.actions.flow.BreakActionProperty;
import platform.server.logics.property.actions.flow.ReturnActionProperty;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.property.group.PropertySet;
import platform.server.logics.table.TableFactory;
import platform.server.session.DataSession;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

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
    public ConcreteCustomClass propertyDraw;
    public StaticCustomClass propertyDrawShowStatus;
    public ConcreteCustomClass abstractGroup;
    public ConcreteCustomClass property;
    public ConcreteCustomClass notification;
    public AbstractCustomClass exception;
    public ConcreteCustomClass clientException;
    public ConcreteCustomClass serverException;
    public ConcreteCustomClass connection;
    public ConcreteCustomClass launch;
    public StaticCustomClass connectionStatus;
    public ConcreteCustomClass dictionary;
    public ConcreteCustomClass dictionaryEntry;
    public ConcreteCustomClass table;
    public ConcreteCustomClass tableKey;
    public ConcreteCustomClass tableColumn;
    public ConcreteCustomClass dropColumn;

    public AbstractCustomClass transaction, transactionTime, barcodeObject, externalObject, historyObject;

    public AbstractCustomClass emailObject;

    public StaticCustomClass encryptedConnectionTypeStatus;

    public StaticCustomClass month;
    public StaticCustomClass DOW;

    public StaticCustomClass formResult;

    // groups
    public AbstractGroup rootGroup;
    public AbstractGroup publicGroup;
    public AbstractGroup privateGroup;
    public AbstractGroup baseGroup;
    public AbstractGroup idGroup;
    public AbstractGroup actionGroup;
    public AbstractGroup sessionGroup;
    public AbstractGroup recognizeGroup;
    public AbstractGroup emailGroup;
    public AbstractGroup historyGroup;

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
    public LP subtractDouble2;
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
    public LP toDateTime;
    public LP string2;
    public LP insensitiveString2;
    protected LP concat2;
    public LP percent;
    public LP percent2;
    public LP share2;
    public LP weekInDate;
    public LP numberDOWInDate;
    public LP numberMonthInDate;
    public LP yearInDate;
    public LP dayInDate;
    public LP dateInTime;
    public LP timeInDateTime;
    public LP jumpWorkdays;
    public LP toEAN13;

    public LP numberMonth;
    public LP numberToMonth;
    public LP monthInDate;

    public LP numberDOW;
    public LP numberToDOW;
    public LP DOWInDate;

    public LP vtrue, actionTrue, vzero;
    public LP vnull;
    public LP charLength;
    public LP positive, negative;

    public LP round0;
    public LP roundMinus1;

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
    protected LP currentTime;
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
    public LP extSID, extSIDToObject;
    public LP timeCreated, userCreated, nameUserCreated, computerCreated, hostnameComputerCreated;
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

    public LP launchComputer;
    public LP computerNameLaunch;
    public LP launchTime;
    public LP launchRevision;

    public LP policyDescription;
    protected LP<?> nameToPolicy;
    public LP userRolePolicyOrder;
    public LP userPolicyOrder;

    public LP hostname;
    public LP notZero;
    public LP onlyNotZero;

    public LP delete;

    public LP apply;
    public LP cancel;

    public LP flowApply;
    public LP flowBreak;
    public LP flowReturn;

    public LP objectClass;
    public LP objectClassName;
    public LP classSID;
    public LP dataName;
    public LP navigatorElementSID;
    public LP numberNavigatorElement;
    public LP navigatorElementCaption;

    public LP propertyDrawSID;
    public LP captionPropertyDraw;
    public LP SIDToPropertyDraw;
    public LP formPropertyDraw;
    public LP SIDNavigatorElementSIDPropertyDrawToPropertyDraw;
    public LP showPropertyDraw;
    public LP nameShowPropertyDraw;
    public LP showPropertyDrawCustomUser;
    public LP nameShowPropertyDrawCustomUser;
    public LP showOverridePropertyDrawCustomUser;
    public LP nameShowOverridePropertyDrawCustomUser;
    public LP columnWidthPropertyDrawCustomUser;
    public LP columnWidthPropertyDraw;
    public LP columnWidthOverridePropertyDrawCustomUser;

    public LP messageException;
    public LP dateException;
    public LP erTraceException;
    public LP typeException;
    public LP clientClientException;
    public LP loginClientException;
    public LP captionAbstractGroup;
    public LP parentAbstractGroup;
    public LP numberAbstractGroup;
    public LP SIDAbstractGroup;
    public LP SIDToAbstractGroup;
    public LP parentProperty;
    public LP numberProperty;
    public LP SIDProperty;
    public LP loggableProperty;
    public LP userLoggableProperty;
    public LP storedProperty;
    public LP isSetNotNullProperty;
    public LP signatureProperty;
    public LP returnProperty;
    public LP classProperty;
    public LP captionProperty;
    public LP SIDToProperty;
    public LP isDerivedChangeNotification;
    public LP emailFromNotification;
    public LP emailToNotification;
    public LP emailToCCNotification;
    public LP emailToBCNotification;
    public LP textNotification;
    public LP subjectNotification;
    public LP inNotificationProperty;
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
    public LP isNavigatorElementNotForm;
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
    public LP recalculateAggregationTableColumn;

    public LP sidDropColumn;
    public LP sidToDropColumn;
    public LP sidTableDropColumn;
    public LP timeDropColumn;
    public LP revisionDropColumn;
    public LP dropDropColumn;

    public LP customID;
    public LP stringID;
    public LP integerID;
    public LP dateID;

    public LP objectByName;
    public LP seekObjectName;

    public LP webHost;

    public LP encryptedConnectionType;
    public LP nameEncryptedConnectionType;
    public LP smtpHost;
    public LP smtpPort;
    public LP emailAccount;
    public LP emailPassword;
    public LP emailBlindCarbonCopy;
    public LP fromAddress;
    public LP disableEmail;
    public LP defaultCountry;
    public LP nameDefaultCountry;

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

    public NavigatorElement<T> accessElement;
    public NavigatorElement<T> eventsElement;
    public NavigatorElement<T> configElement;
    public NavigatorElement<T> catalogElement;

    public FormEntity<T> dictionaryForm;
    public FormEntity<T> objectForm;

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
    public final StringClass propertySignatureValueClass = StringClass.get(100);
    public final LogicalClass propertyLoggableValueClass = LogicalClass.instance;
    public final LogicalClass propertyStoredValueClass = LogicalClass.instance;
    public final LogicalClass propertyIsSetNotNullValueClass = LogicalClass.instance;
    public final StringClass loginValueClass = StringClass.get(100);
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

        externalObject = addAbstractClass("externalObject", getString("logics.object.external.object"), baseClass);

        historyObject = addAbstractClass("historyObject", getString("logics.object.history.object"), baseClass);

        emailObject = addAbstractClass("emailObject", getString("logics.object.with.email"), baseClass);

        encryptedConnectionTypeStatus = addStaticClass("encryptedConnectionTypeStatus", getString("logics.connection.type.status"),
                new String[]{"SSL", "TLS"},
                new String[]{"SSL", "TLS"});

        user = addAbstractClass("user", getString("logics.user"), baseClass);
        customUser = addConcreteClass("customUser", getString("logics.user.ordinary.user"), user, barcodeObject, emailObject);
        systemUser = addConcreteClass("systemUser", getString("logics.user.system.user"), user);
        computer = addConcreteClass("computer", getString("logics.workplace"), baseClass);
        userRole = addConcreteClass("userRole", getString("logics.role"), baseClass.named);

        policy = addConcreteClass("policy", getString("logics.policy.security.policy"), baseClass.named);
        session = addConcreteClass("session", getString("logics.session"), baseClass);

        connection = addConcreteClass("connection", getString("logics.connection"), baseClass);
        connectionStatus = addStaticClass("connectionStatus", getString("logics.connection.status"),
                new String[]{"connectedConnection", "disconnectedConnection"},
                new String[]{getString("logics.connection.connected"), getString("logics.connection.disconnected")});
        launch = addConcreteClass("launch", getString("logics.launch"), baseClass);
        country = addConcreteClass("country", getString("logics.country"), baseClass.named);

        navigatorElement = addConcreteClass("navigatorElement", getString("logics.navigator.element"), baseClass);
        form = addConcreteClass("form", getString("logics.forms.form"), navigatorElement);
        propertyDraw = addConcreteClass("propertyDraw", getString("logics.property.draw"), baseClass);
        propertyDrawShowStatus = addStaticClass("propertyDrawShowStatus", getString("logics.forms.property.show"),
                new String[]{"Show", "Hide"},
                new String[]{getString("logics.property.draw.show"), getString("logics.property.draw.hide")});
        abstractGroup = addConcreteClass("abstractGroup", getString("logics.property.group"), baseClass);
        property = addConcreteClass("property", getString("logics.property"), baseClass);
        notification = addConcreteClass("notification", getString("logics.notification"), baseClass);
        exception = addAbstractClass("exception", getString("logics.exception"), baseClass);
        clientException = addConcreteClass("clientException", getString("logics.exception.client"), exception);
        serverException = addConcreteClass("serverException", getString("logics.exception.server"), exception);
        dictionary = addConcreteClass("dictionary", getString("logics.dictionary"), baseClass.named);
        dictionaryEntry = addConcreteClass("dictionaryEntry", getString("logics.dictionary.entries"), baseClass);

        table = addConcreteClass("table", getString("logics.tables.table"), baseClass);
        tableKey = addConcreteClass("tableKey", getString("logics.tables.key"), baseClass);
        tableColumn = addConcreteClass("tableColumn", getString("logics.tables.column"), baseClass);
        dropColumn = addConcreteClass("dropColumn", getString("logics.tables.deleted.column"), baseClass);

        month = addStaticClass("month", getString("logics.month"),
                new String[]{"january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december"},
                new String[]{getString("logics.month.january"), getString("logics.month.february"), getString("logics.month.march"), getString("logics.month.april"), getString("logics.month.may"), getString("logics.month.june"), getString("logics.month.july"), getString("logics.month.august"), getString("logics.month.september"), getString("logics.month.october"), getString("logics.month.november"), getString("logics.month.december")});
        DOW = addStaticClass("DOW", getString("logics.week.day"),
                  new String[]{"sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday"},
                  new String[]{getString("logics.days.sunday"), getString("logics.days.monday"), getString("logics.days.tuesday"), getString("logics.days.wednesday"), getString("logics.days.thursday"), getString("logics.days.friday"), getString("logics.days.saturday")});
        formResult = addStaticClass("formResult", "Результат вызова формы",
                new String[]{"null", "ok", "close"},
                new String[]{"Неизвестно", "Принять", "Закрыть"});
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
        emailGroup = addAbstractGroup("emailGroup", getString("logics.groups.emailgroup"), rootGroup, true);
        historyGroup = addAbstractGroup("historyGroup", getString("logics.groups.historygroup"), rootGroup, true);
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
        tableFactory.include("abstractGroup", abstractGroup);
        tableFactory.include("property", property);
        tableFactory.include("propertyDraw", propertyDraw);
        tableFactory.include("exception", exception);
        tableFactory.include("notification", notification);
        tableFactory.include("launch", launch);
        tableFactory.include("transaction", transaction);
        tableFactory.include("named", baseClass.named);
        tableFactory.include("barcodeObject", barcodeObject);
        tableFactory.include("emailObject", emailObject);
        tableFactory.include("externalObject", externalObject);
        tableFactory.include("historyObject", historyObject);
        tableFactory.include("dictionary", dictionary);
        tableFactory.include("dictionaryEntry", dictionaryEntry);

        tableFactory.include("session", session);
        tableFactory.include("exception", exception);
        tableFactory.include("connection", connection);
        tableFactory.include("computer", computer);

        tableFactory.include("connectionNavigatorElement", connection, navigatorElement);
        tableFactory.include("userRoleNavigatorElement", userRole, navigatorElement);
        tableFactory.include("userRoleProperty", userRole, property);
        tableFactory.include("notificationProperty", notification, property);
        tableFactory.include("propertyDrawCustomUser", propertyDraw, customUser);
        tableFactory.include("formPropertyDraw", form, propertyDraw);

        tableFactory.include("tables", table);
        tableFactory.include("tableKey", tableKey);
        tableFactory.include("tableColumn", tableColumn);
        tableFactory.include("dropColumn", dropColumn);

        tableFactory.include("customUserRole", customUser, userRole);
        tableFactory.include("userRolePolicy", userRole, policy);
        tableFactory.include("userRoleProperty", userRole, property);

        tableFactory.include("month", month);
        tableFactory.include("dow", DOW);
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
        dataName.property.aggProp = true;

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
        diff2 = addCFProp("diff2", Compare.NOT_EQUALS);
        sumDouble2 = addSFProp("sumDouble2", "((prm1)+(prm2))", DoubleClass.instance, 2);
        subtractDouble2 = addSFProp("subtractDouble2", "((prm1)-(prm2))", DoubleClass.instance, 2);
        deltaDouble2 = addSFProp("deltaDouble2", "abs((prm1)-(prm2))", DoubleClass.instance, 2);
        multiplyDouble2 = addMFProp("multiplyDouble2", DoubleClass.instance, 2);
        sumInteger2 = addSFProp("sumInteger2", "((prm1)+(prm2))", IntegerClass.instance, 2);
        sumInteger3 = addSFProp("sumInteger3", "((prm1)+(prm2)+(prm3))", IntegerClass.instance, 3);
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
        addDate2 = addSFProp("addDate2", "((prm1)+(prm2))", DateClass.instance, 2);
        subtractDate2 = addSFProp("subtractDate2", "((prm1)-(prm2))", DateClass.instance, 2);
        toDateTime = addSFProp("toDateTime", "to_timestamp(CAST(prm1 as char(10)) || CAST(prm2 as char(8)), \'YYYY-MM-DDHH24:MI:SS\')", DateTimeClass.instance, 2);
        percent = addSFProp("((prm1)*(prm2)/100)", DoubleClass.instance, 2);
        percent2 = addSFProp("round(CAST(((prm1)*(prm2)/100) as numeric), 2)", DoubleClass.instance, 2);
        share2 = addSFProp("round(CAST(((prm1)*100/(prm2)) as numeric), 2)", DoubleClass.instance, 2);
        jumpWorkdays = addSFProp("jumpWorkdays(prm1, prm2, prm3)", DateClass.instance, 3); //1 - country, 2 - date, 3 - days to jump
        toEAN13 = addSFProp("toEAN13(prm1)", StringClass.get(13), 1);
        between = addJProp("between", getString("logics.between"), and1, groeq2, 1, 2, groeq2, 3, 1);
        vtrue = addCProp(getString("logics.true"), LogicalClass.instance, true);
        vzero = addCProp("0", DoubleClass.instance, 0);
        vnull = addProperty(privateGroup, new LP<PropertyInterface>(new NullValueProperty()));

        round0 = addSFProp("round0", "round(CAST(prm1 as numeric), 0)", DoubleClass.instance, 1);
        roundMinus1 = addSFProp("roundMinus1", "round(CAST(prm1 as numeric), -1)", DoubleClass.instance, 1);

        minusInteger = addSFProp("(-(prm1))", IntegerClass.instance, 1);
        minusDouble = addSFProp("(-(prm1))", DoubleClass.instance, 1);

        actionTrue = addCProp("ActionTrue", ActionClass.instance, true);

        dumb1 = dumb(1);
        dumb2 = dumb(2);

        castText = addSFProp("CAST((prm1) as text)", TextClass.instance, 1);
        addText2 = addSFProp("((prm1)+(prm2))", TextClass.instance, 2);

        charLength = addSFProp("char_length(prm1)", IntegerClass.instance, 1);

        positive = addJProp(greater2, 1, vzero);
        negative = addJProp(less2, 1, vzero);

        weekInDate = addSFProp("weekInDate", "(extract(week from (prm1)))", IntegerClass.instance, 1);
        numberDOWInDate = addSFProp("numberDOWInDate", "(extract(dow from (prm1)))", IntegerClass.instance, 1);
        numberMonthInDate = addSFProp("numberMonthInDate", "(extract(month from (prm1)))", IntegerClass.instance, 1);
        yearInDate = addSFProp("yearInDate", "(extract(year from (prm1)))", IntegerClass.instance, 1);
        dayInDate = addSFProp("dayInDate", "(extract(day from (prm1)))", IntegerClass.instance, 1);
        dateInTime = addSFProp("dateInTime", "(CAST((prm1) as date))", DateClass.instance, 1);

        timeInDateTime = addSFProp("timeInDateTime", "(CAST((prm1) as time))", TimeClass.instance, 1);

        numberMonth = addOProp(baseGroup, "numberMonth", true, getString("logics.month.number"), addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), is(month), 1), PartitionType.SUM, true, true, 0, 1);
        numberToMonth = addAGProp("numberToMonth", getString("logics.month.id"), numberMonth);
        monthInDate = addJProp("monthInDate", getString("logics.month.id"), numberToMonth, numberMonthInDate, 1);

        numberDOW = addJProp(baseGroup, "numberDOW", true, getString("logics.week.day.number"), subtractInteger2,
                addOProp("numberDOWP1", getString("logics.week.day.number.plus.one"), PartitionType.SUM, addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), is(DOW), 1), true, false, 0, 1), 1,
                addCProp(IntegerClass.instance, 1));
        numberToDOW = addAGProp("numberToDOW", getString("logics.week.day.id"), numberDOW);
        DOWInDate = addJProp("DOWInDate", getString("logics.week.day.id"), numberToDOW, numberDOWInDate, 1);

        delete = addAProp(new DeleteObjectActionProperty(baseClass));

        apply = addAProp(new ApplyActionProperty(BL));
        cancel = addAProp(new CancelActionProperty());

        flowApply = addJoinAProp(null, "", "", 0, apply);
        flowBreak = addProperty(null, new LP<ClassPropertyInterface>(new BreakActionProperty()));
        flowReturn = addProperty(null, new LP<ClassPropertyInterface>(new ReturnActionProperty()));

        date = addDProp(baseGroup, "date", getString("logics.date"), DateClass.instance, transaction);

        notZero = addJProp(diff2, 1, vzero);
        onlyNotZero = addJProp(andNot1, 1, addJProp(equals2, 1, vzero), 1);
        onlyNotZero.property.isOnlyNotZero = true;

        daysInclBetweenDates = addJProp("daysInclBetweenDates", getString("logics.date.quantity.days"), and(false, false), addJProp(subtractInclInteger2, 2, 1), 1, 2, is(DateClass.instance), 1, is(DateClass.instance), 2);
        weeksInclBetweenDates = addJProp("weeksInclBetweenDates", getString("logics.date.quantity.weeks"), divideInteger, daysInclBetweenDates, 1, 2, addCProp(IntegerClass.instance, 7));
        weeksNullInclBetweenDates = addJProp("weeksNullInclBetweenDates", getString("logics.date.quantity.weeks"), onlyNotZero, weeksInclBetweenDates, 1, 2);

        sumDateWeekFrom = addJProp("sumDateWeekFrom", getString("logics.date.from"), and(false, false), addSFProp("((prm1)+(prm2)*7)", DateClass.instance, 2), 1, 2, is(DateClass.instance), 1, is(IntegerClass.instance), 2);
        sumDateWeekTo = addJProp("sumDateWeekTo", getString("logics.date.to"), and(false, false), addSFProp("((prm1)+((prm2)*7+6))", DateClass.instance, 2), 1, 2, is(DateClass.instance), 1, is(IntegerClass.instance), 2);

        betweenDates = addJProp(getString("logics.date.of.doc.between"), between, object(DateClass.instance), 1, object(DateClass.instance), 2, object(DateClass.instance), 3);
        betweenDate = addJProp(getString("logics.date.of.doc.between"), betweenDates, date, 1, 2, 3);

        sidCountry = addDProp(baseGroup, "sidCountry", getString("logics.country.key"), IntegerClass.instance, country);
        generateDatesCountry = addDProp(privateGroup, "generateDatesCountry", getString("logics.day.generate.days.off"), LogicalClass.instance, country);
        sidToCountry = addAGProp("sidToCountry", getString("logics.country"), sidCountry);

        isDayOffCountryDate = addDProp(baseGroup, "isDayOffCD", getString("logics.day.off"), LogicalClass.instance, country, DateClass.instance);


        workingDay = addJProp(baseGroup, "workingDay", getString("logics.day.working"), andNot1, addCProp(IntegerClass.instance, 1, country, DateClass.instance), 1, 2, isDayOffCountryDate, 1, 2);
        isWorkingDay = addJProp(baseGroup, "isWorkingDay", getString("logics.day.working"), and(false, false), workingDay, 1, 3, groeq2, 3, 2, is(DateClass.instance), 3);
        workingDaysQuantity = addOProp(baseGroup, "workingDaysQuantity", getString("logics.day.working.days"), PartitionType.SUM, isWorkingDay, true, true, 1, 2, 1, 3);
        equalsWorkingDaysQuantity = addJProp(baseGroup, "equalsWorkingDaysQuantity", getString("logics.day.equals.quantity.of.working.days"), equals2, object(IntegerClass.instance), 1, workingDaysQuantity, 2, 3, 4);

        transactionLater = addSUProp(getString("logics.transaction.later"), Union.OVERRIDE, addJProp(getString("logics.date.later"), greater2, date, 1, date, 2),
                                     addJProp("", and1, addJProp(getString("logics.date.equals.date"), equals2, date, 1, date, 2), 1, 2, addJProp(getString("logics.transaction.code.later"), greater2, 1, 2), 1, 2));

        hostname = addDProp(baseGroup, "hostname", getString("logics.host.name"), InsensitiveStringClass.get(100), computer);

        currentDate = addDProp(baseGroup, "currentDate", getString("logics.date.current.date"), DateClass.instance);
        currentMonth = addJProp(baseGroup, "currentMonth", getString("logics.date.current.month"), numberMonthInDate, currentDate);
        currentYear = addJProp(baseGroup, "currentYear", getString("logics.date.current.year"), yearInDate, currentDate);
        currentHour = addTProp("currentHour", Time.HOUR);
        currentMinute = addTProp("currentMinute", Time.MINUTE);
        currentEpoch = addTProp("currentEpoch", Time.EPOCH);
        currentDateTime = addTProp("currentDateTime", Time.DATETIME);
        currentTime = addJProp("currentTime", getString("logics.date.current.time"), timeInDateTime, currentDateTime);
        currentUser = addProperty(null, new LP<PropertyInterface>(new CurrentUserFormulaProperty("currentUser", user)));
        currentSession = addProperty(null, new LP<PropertyInterface>(new CurrentSessionFormulaProperty("currentSession", session)));
        currentComputer = addProperty(null, new LP<PropertyInterface>(new CurrentComputerFormulaProperty("currentComputer", computer)));
        isServerRestarting = addProperty(null, new LP<PropertyInterface>(new IsServerRestartingFormulaProperty("isServerRestarting")));
        changeUser = addProperty(null, new LP<ClassPropertyInterface>(new ChangeUserActionProperty("changeUser", customUser)));

        userLogin = addDProp(baseGroup, "userLogin", getString("logics.user.login"), StringClass.get(30), customUser);
        loginToUser = addAGProp("loginToUser", getString("logics.user"), userLogin);
        userPassword = addDProp(publicGroup, "userPassword", getString("logics.user.password"), StringClass.get(30), customUser);
        userPassword.setEchoSymbols(true);
        userFirstName = addDProp(publicGroup, "userFirstName", getString("logics.user.firstname"), StringClass.get(30), customUser);
        userFirstName.setMinimumCharWidth(10);

        userLastName = addDProp(publicGroup, "userLastName", getString("logics.user.lastname"), StringClass.get(30), customUser);
        userLastName.setMinimumCharWidth(10);

        userRoleSID = addDProp(baseGroup, "userRoleSID", getString("logics.user.identificator"), StringClass.get(30), userRole);
        sidToRole = addAGProp(idGroup, "sidToRole", getString("logics.user.role.id"), userRole, userRoleSID);
        inUserRole = addDProp(baseGroup, "inUserRole", getString("logics.user.role.in"), LogicalClass.instance, customUser, userRole);
        userRoleDefaultForms = addDProp(baseGroup, "userRoleDefaultForms", getString("logics.user.displaying.forms.by.default"), LogicalClass.instance, userRole);
        inLoginSID = addJProp("inLoginSID", true, getString("logics.login.has.a.role"), inUserRole, loginToUser, 1, sidToRole, 2);

        email = addDProp(baseGroup, "email", getString("logics.email"), StringClass.get(50), emailObject);
        email.setRegexp("^[-a-zA-Z0-9!#$%&'*+/=?^_`{|}~]+(?:\\.[-a-zA-Z0-9!#$%&'*+/=?^_`{|}~]+)*@(?:[a-zA-Z0-9]([-a-zA-Z0-9]{0,61}[a-zA-Z0-9])?\\.)*(?:aero|arpa|asia|biz|cat|com|coop|edu|gov|info|int|jobs|mil|mobi|museum|name|net|org|pro|tel|travel|[a-zA-Z][a-zA-Z])$");
        email.setRegexpMessage("<html>Неверный формат e-mail</html>");

        emailToObject = addAGProp("emailToObject", getString("logics.email.to.object"), email);

        generateLoginPassword = addAProp(actionGroup, new GenerateLoginPasswordActionProperty(email, userLogin, userPassword, customUser));

        name = addCUProp(recognizeGroup, "commonName", getString("logics.name"), dataName,
                addJProp(insensitiveString2, userFirstName, 1, userLastName, 1));
        name.property.aggProp = true;

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

        launchComputer = addDProp("launchComputer", getString("logics.computer"), computer, launch);
        computerNameLaunch = addJProp(baseGroup, getString("logics.computer"), hostname, launchComputer, 1);
        launchTime = addDProp(baseGroup, "launchConnectTime", getString("logics.launch.time"), DateTimeClass.instance, launch);
        launchRevision = addDProp(baseGroup, "launchRevision", getString("logics.launch.revision"), StringClass.get(10), launch);

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

        extSID = addDProp(baseGroup, "extSID", getString("logics.extsid"), StringClass.get(100), externalObject);
        extSIDToObject = addAGProp("extSIDToObject", getString("logics.object"), extSID);
        
        timeCreated = addDProp(historyGroup, "timeCreated", getString("logics.timecreated"), DateTimeClass.instance, historyObject);
        userCreated = addDProp(idGroup, "userCreated", getString("logics.usercreated"), customUser, historyObject);
        nameUserCreated = addJProp(historyGroup, "nameUserCreated", getString("logics.usercreated"), name, userCreated, 1);
        computerCreated = addDProp(idGroup, "computerCreated", getString("logics.computercreated"), computer, historyObject);
        hostnameComputerCreated = addJProp(historyGroup, "hostnameComputerCreated", getString("logics.computercreated"), hostname, computerCreated, 1);
        
        timeCreated.setDerivedChange(true, currentDateTime, is(historyObject), 1);
        userCreated.setDerivedChange(true, currentUser, is(historyObject), 1);
        computerCreated.setDerivedChange(true, currentComputer, is(historyObject), 1);

        restartServerAction = addJProp(getString("logics.server.stop"), andNot1, addRestartActionProp(), isServerRestarting);
        runGarbageCollector = addGarbageCollectorActionProp();
        cancelRestartServerAction = addJProp(getString("logics.server.cancel.stop"), and1, addCancelRestartActionProp(), isServerRestarting);

        checkAggregationsAction = addProperty(null, new LP<ClassPropertyInterface>(new CheckAggregationsActionProperty(genSID(), getString("logics.check.aggregations"))));
        recalculateAction = addProperty(null, new LP<ClassPropertyInterface>(new RecalculateActionProperty(genSID(), getString("logics.recalculate.aggregations"))));
        recalculateFollowsAction = addProperty(null, new LP<ClassPropertyInterface>(new RecalculateFollowsActionProperty(genSID(), getString("logics.recalculate.follows"))));
        packAction = addProperty(null, new LP<ClassPropertyInterface>(new PackActionProperty(genSID(), getString("logics.tables.pack"))));

        currentUserName = addJProp(getString("logics.user.current.user.name"), name, currentUser);

        reverseBarcode = addSDProp("reverseBarcode", getString("logics.barcode.reverse"), LogicalClass.instance);

        objectClass = addProperty(null, new LP<ClassPropertyInterface>(baseClass.getObjectClassProperty()));
        objectClassName = addJProp(baseGroup, "objectClassName", getString("logics.object.class"), name, objectClass, 1);
        objectClassName.makeLoggable(this, true);

        navigatorElementSID = addDProp(baseGroup, "navigatorElementSID", getString("logics.forms.code"), formSIDValueClass, navigatorElement);
        numberNavigatorElement = addDProp(baseGroup, "numberNavigatorElement", getString("logics.number"), IntegerClass.instance, navigatorElement);
        navigatorElementCaption = addDProp(baseGroup, "navigatorElementCaption", getString("logics.forms.name"), formCaptionValueClass, navigatorElement);
        SIDToNavigatorElement = addAGProp("SIDToNavigatorElement", getString("logics.forms.form"), navigatorElementSID);
        parentNavigatorElement = addDProp("parentNavigatorElement", getString("logics.forms.parent.form"), navigatorElement, navigatorElement);
        isNavigatorElementNotForm = addJProp("isNavigatorElementNotForm", and(true), is(navigatorElement), 1, is(form), 1);

        propertyDrawSID = addDProp(baseGroup, "propertyDrawSID", getString("logics.forms.property.draw.code"), propertySIDValueClass, propertyDraw);
        captionPropertyDraw = addDProp(baseGroup, "captionPropertyDraw", getString("logics.forms.property.draw.caption"), propertyCaptionValueClass, propertyDraw);
        formPropertyDraw = addDProp(baseGroup, "formPropertyDraw", getString("logics.forms.form"), form, propertyDraw);
        SIDToPropertyDraw = addAGProp(baseGroup, "SIDToPropertyDraw", getString("logics.property.draw"), formPropertyDraw, propertyDrawSID);
        SIDNavigatorElementSIDPropertyDrawToPropertyDraw = addJProp(baseGroup, "SIDNavigatorElementSIDPropertyDrawToPropertyDraw", getString("logics.forms.code"), SIDToPropertyDraw, SIDToNavigatorElement, 1, 2);
        showPropertyDraw = addDProp(baseGroup, "showPropertyDraw", getString("logics.forms.property.show"), propertyDrawShowStatus, propertyDraw);
        nameShowPropertyDraw = addJProp(baseGroup, "nameShowPropertyDraw", getString("logics.forms.property.show"), name, showPropertyDraw, 1);
        nameShowPropertyDraw.setPreferredWidth(50);
        showPropertyDrawCustomUser = addDProp(baseGroup, "showPropertyDrawCustomUser", getString("logics.forms.property.show.user"), propertyDrawShowStatus, propertyDraw, customUser);
        nameShowPropertyDrawCustomUser = addJProp(baseGroup, "nameShowPropertyDrawCustomUser", getString("logics.forms.property.show.user"), name, showPropertyDrawCustomUser, 1, 2);
        nameShowPropertyDrawCustomUser.setPreferredWidth(50);
        showOverridePropertyDrawCustomUser = addSUProp(baseGroup, "showOverridePropertyDrawCustomUser", getString("logics.forms.property.show"), Union.OVERRIDE, addJProp(and1, showPropertyDraw, 1, is(customUser), 2), showPropertyDrawCustomUser);
        nameShowOverridePropertyDrawCustomUser = addJProp(baseGroup, "nameShowOverridePropertyDrawCustomUser", getString("logics.forms.property.show"), name, showOverridePropertyDrawCustomUser, 1, 2);
        columnWidthPropertyDrawCustomUser = addDProp(baseGroup, "columnWidthPropertyDrawCustomUser", getString("logics.forms.property.width.user"), IntegerClass.instance, propertyDraw, customUser);
        columnWidthPropertyDraw = addDProp(baseGroup, "columnWidthPropertyDraw", getString("logics.forms.property.width"), IntegerClass.instance, propertyDraw);
        columnWidthOverridePropertyDrawCustomUser = addSUProp(baseGroup, "columnWidthOverridePropertyDrawCustomUser", getString("logics.forms.property.width"), Union.OVERRIDE, addJProp(and1, columnWidthPropertyDraw, 1, is(customUser), 2), columnWidthPropertyDrawCustomUser);

        messageException = addDProp(baseGroup, "messageException", getString("logics.exception.message"), propertyCaptionValueClass, exception);
        dateException = addDProp(baseGroup, "dateException", getString("logics.exception.date"), DateTimeClass.instance, exception);
        erTraceException = addDProp(baseGroup, "erTraceException", getString("logics.exception.ertrace"), TextClass.instance, exception);
        erTraceException.setPreferredWidth(500);
        typeException =  addDProp(baseGroup, "typeException", getString("logics.exception.type"), propertyCaptionValueClass, exception);
        clientClientException = addDProp(baseGroup, "clientClientException", getString("logics.exception.client.client"), loginValueClass, clientException);
        loginClientException = addDProp(baseGroup, "loginClientException", getString("logics.exception.client.login"), loginValueClass, clientException);

        captionAbstractGroup = addDProp(baseGroup, "captionAbstractGroup", getString("logics.name"), propertyCaptionValueClass, abstractGroup);
        parentAbstractGroup = addDProp(baseGroup, "parentAbstractGroup", getString("logics.property.group"), abstractGroup, abstractGroup);
        numberAbstractGroup = addDProp(baseGroup, "numberAbstractGroup", getString("logics.property.number"), IntegerClass.instance, abstractGroup);
        SIDAbstractGroup = addDProp(baseGroup, "SIDAbstractGroup", getString("logics.property.sid"), propertySIDValueClass, abstractGroup);
        SIDToAbstractGroup = addAGProp("SIDToAbstractGroup", getString("logics.property"), SIDAbstractGroup);

        parentProperty = addDProp(baseGroup, "parentProperty", getString("logics.property.group"), abstractGroup, property);
        numberProperty = addDProp(baseGroup, "numberProperty", getString("logics.property.number"), IntegerClass.instance, property);
        SIDProperty = addDProp(baseGroup, "SIDProperty", getString("logics.property.sid"), propertySIDValueClass, property);
        loggableProperty = addDProp(baseGroup, "loggableProperty", getString("logics.property.loggable"), LogicalClass.instance, property);
        userLoggableProperty = addDProp(baseGroup, "userLoggableProperty", getString("logics.property.user.loggable"), LogicalClass.instance, property);
        storedProperty = addDProp(baseGroup, "storedProperty", getString("logics.property.stored"), LogicalClass.instance, property);
        isSetNotNullProperty = addDProp(baseGroup, "isSetNotNullProperty", getString("logics.property.set.not.null"), LogicalClass.instance, property);
        signatureProperty = addDProp(baseGroup, "signatureProperty", getString("logics.property.signature"), propertySignatureValueClass, property);
        returnProperty = addDProp(baseGroup, "returnProperty", getString("logics.property.return"), propertySignatureValueClass, property);
        classProperty = addDProp(baseGroup, "classProperty", getString("logics.property.class"), propertySignatureValueClass, property);
        captionProperty = addDProp(baseGroup, "captionProperty", getString("logics.property.caption"), propertyCaptionValueClass, property);
        SIDToProperty = addAGProp("SIDToProperty", getString("logics.property"), SIDProperty);

        isDerivedChangeNotification = addDProp(baseGroup, "isDerivedChangeNotification", getString("logics.notification.for.any.change"), LogicalClass.instance, notification);
        emailFromNotification = addDProp(baseGroup, "emailFromNotification", getString("logics.notification.sender.address"), StringClass.get(50), notification);
        emailToNotification = addDProp(baseGroup, "emailToNotification", getString("logics.notification.recipient.address"), StringClass.get(50), notification);
        emailToCCNotification = addDProp(baseGroup, "emailToCCNotification", getString("logics.notification.copy"), StringClass.get(50), notification);
        emailToBCNotification = addDProp(baseGroup, "emailToBCNotification", getString("logics.notification.blind.copy"), StringClass.get(50), notification);
        textNotification = addDProp(baseGroup, "textNotification", getString("logics.notification.text"), TextClass.instance, notification);
        subjectNotification = addDProp(baseGroup, "subjectNotification", getString("logics.notification.topic"), StringClass.get(100), notification);
        inNotificationProperty = addDProp(baseGroup, "inNotificationProperty", getString("logics.notification.enable"), LogicalClass.instance, notification, baseLM.property);

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
        allowAllUserForm = addJProp(publicGroup, "allowAllUserForm", getString("logics.user.allow.all.user.form"), allowAllUserRoleForms, userMainRole, 1);
        forbidAllUserRoleForms = addDProp(baseGroup, "forbidAllUserRoleForms", getString("logics.user.forbid.all.user.form"), LogicalClass.instance, userRole);
        forbidAllUserForm = addJProp(publicGroup, "forbidAllUserForm", getString("logics.user.forbid.all.user.form"), forbidAllUserRoleForms, userMainRole, 1);

        allowViewAllUserRoleProperty = addDProp(baseGroup, "allowViewAllUserRoleProperty", getString("logics.user.allow.view.all.property"), LogicalClass.instance, userRole);
        allowViewAllUserForm = addJProp(publicGroup, "allowViewAllUserForm", getString("logics.user.allow.view.all.property"), allowViewAllUserRoleProperty, userMainRole, 1);
        forbidViewAllUserRoleProperty = addDProp(baseGroup, "forbidViewAllUserRoleProperty", getString("logics.user.forbid.view.all.property"), LogicalClass.instance, userRole);
        forbidViewAllUserForm = addJProp(publicGroup, "forbidViewAllUserForm", getString("logics.user.forbid.view.all.property"), forbidViewAllUserRoleProperty, userMainRole, 1);

        allowChangeAllUserRoleProperty = addDProp(baseGroup, "allowChangeAllUserRoleProperty", getString("logics.user.allow.change.all.property"), LogicalClass.instance, userRole);
        allowChangeAllUserForm = addJProp(publicGroup, "allowChangeAllUserForm", getString("logics.user.allow.change.all.property"), allowChangeAllUserRoleProperty, userMainRole, 1);
        forbidChangeAllUserRoleProperty = addDProp(baseGroup, "forbidChangeAllUserRoleProperty", getString("logics.user.forbid.change.all.property"), LogicalClass.instance, userRole);
        forbidChangeAllUserForm = addJProp(publicGroup, "forbidChangeAllUserForm", getString("logics.user.forbid.change.all.property"), forbidChangeAllUserRoleProperty, userMainRole, 1);

        userRoleFormDefaultNumber = addDProp(baseGroup, "userRoleFormDefaultNumber", getString("logics.forms.default.number"), IntegerClass.instance, userRole, navigatorElement);
        userFormDefaultNumber = addJProp(baseGroup, "userFormDefaultNumber", getString("logics.forms.default.number"), userRoleFormDefaultNumber, userMainRole, 1, 2);
        userDefaultForms = addJProp(publicGroup, "userDefaultForms", getString("logics.user.displaying.forms.by.default"), userRoleDefaultForms, userMainRole, 1);
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
        sidToTableKey = addAGProp("sidToTableKey", getString("logics.tables.key"), sidTableKey);
        sidToTableColumn = addAGProp("sidToTableColumn", getString("logics.tables.column"), sidTableColumn);
        tableTableKey = addDProp("tableTableKey", getString("logics.tables.table"), table, tableKey);
        classTableKey = addDProp(baseGroup, "classTableKey", getString("logics.tables.key.class"), StringClass.get(40), tableKey);
        tableTableColumn = addDProp("tableTableColumn", getString("logics.tables.table"), table, tableColumn);
        rowsTable = addDProp(baseGroup, "rowsTable", getString("logics.tables.rows"), IntegerClass.instance, table);
        quantityTableKey = addDProp(baseGroup, "quantityTableKey", getString("logics.tables.key.distinct.quantity"), IntegerClass.instance, tableKey);
        quantityTableColumn = addDProp(baseGroup, "quantityTableColumn", getString("logics.tables.column.values.quantity"), IntegerClass.instance, tableColumn);
        recalculateAggregationTableColumn = addAProp(actionGroup, new RecalculateTableColumnActionProperty(getString("logics.recalculate.aggregations"), tableColumn));

        sidDropColumn = addDProp(baseGroup, "sidDropColumn", getString("logics.tables.column.name"), StringClass.get(100), dropColumn);
        sidToDropColumn = addAGProp("sidToDropColumn", getString("logics.tables.deleted.column"), sidDropColumn);
        sidTableDropColumn = addDProp(baseGroup, "sidTableDropColumn", getString("logics.tables.name"), StringClass.get(100), dropColumn);
        timeDropColumn = addDProp(baseGroup, "timeDropColumn", getString("logics.tables.deleted.column.time"), DateTimeClass.instance, dropColumn);
        revisionDropColumn = addDProp(baseGroup, "revisionDropColumn", getString("logics.launch.revision"), StringClass.get(10), dropColumn);
        dropDropColumn = addAProp(baseGroup, new DropColumnActionProperty("dropDropColumn", getString("logics.tables.deleted.column.drop"), dropColumn));
        dropDropColumn.setAskConfirm(true);

        // заполним сессии
        LP sessionUser = addDProp("sessionUser", getString("logics.session.user"), user, session);
        sessionUser.setDerivedChange(currentUser, true, is(session), 1);
        addJProp(baseGroup, getString("logics.session.user"), name, sessionUser, 1);
        LP sessionDate = addDProp(baseGroup, "sessionDate", getString("logics.session.date"), DateTimeClass.instance, session);
        sessionDate.setDerivedChange(currentDateTime, true, is(session), 1);

        objectByName = addMGProp(idGroup, "objectByName", getString("logics.object.name"), object(baseClass.named), name, 1);
        seekObjectName = addJProp(true, getString("logics.object.search"), addSAProp(null), objectByName, 1);

        webHost = addDProp("webHost", getString("logics.host.webhost"), StringClass.get(50));

        encryptedConnectionType = addDProp(emailGroup, "encryptedConnectionType", getString("logics.connection.type.status"), encryptedConnectionTypeStatus);
        nameEncryptedConnectionType = addJProp(emailGroup, "nameEncryptedConnectionType", getString("logics.connection.type.status"), baseLM.name, encryptedConnectionType);
        nameEncryptedConnectionType.setPreferredCharWidth(3);
        smtpHost = addDProp(emailGroup, "smtpHost", getString("logics.host.smtphost"), StringClass.get(50));
        smtpPort = addDProp(emailGroup, "smtpPort", getString("logics.host.smtpport"), StringClass.get(10));
        emailAccount = addDProp(emailGroup, "emailAccount", getString("logics.email.accountname"), StringClass.get(50));
        emailPassword = addDProp(emailGroup, "emailPassword", getString("logics.email.password"), StringClass.get(50));
        emailBlindCarbonCopy = addDProp(emailGroup, "emailBlindCarbonCopy", getString("logics.email.copy.bcc"), StringClass.get(50));
        fromAddress = addDProp(emailGroup, "fromAddress", getString("logics.email.sender"), StringClass.get(50));

        emailUserPassUser = addEAProp(getString("logics.user.password.reminder"), customUser);
        addEARecipients(emailUserPassUser, email, 1);

        disableEmail = addDProp(emailGroup, "disableEmail", getString("logics.email.disable.email.sending"), LogicalClass.instance);

        defaultCountry = addDProp("defaultCountry", getString("logics.country.default.country"), country);
        nameDefaultCountry = addJProp("nameDefaultCountry", getString("logics.country.default.country"), baseLM.name, defaultCountry);
        nameDefaultCountry.setPreferredCharWidth(30);

        entryDictionary = addDProp("entryDictionary", getString("logics.dictionary"), dictionary, dictionaryEntry);
        termDictionary = addDProp(recognizeGroup, "termDictionary", getString("logics.dictionary.termin"), StringClass.get(50), dictionaryEntry);
        translationDictionary = addDProp(baseGroup, "translationDictionary", getString("logics.dictionary.translation"), StringClass.get(50), dictionaryEntry);
        translationDictionaryTerm = addCGProp(null, "translationDictionayTerm", getString("logics.dictionary.translation"), translationDictionary, termDictionary, entryDictionary, 1, termDictionary, 1);
        nameEntryDictionary = addJProp(baseGroup, "nameEntryDictionary", getString("logics.dictionary"), name, entryDictionary, 1);

        initNavigators();
    }

    @Override
    public void initIndexes() {
        addIndex(barcode);
        addIndex(dataName);
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

            List<JoinProperty.Interface> listInterfaces = JoinProperty.getInterfaces(intNum);
            JoinProperty<ClassPropertyInterface> joinProperty = new JoinProperty(sid, getString("logics.compound.name")+" (" + intNum + ")",
                    listInterfaces, false, mapImplement(stringConcat, readImplements(listInterfaces, joinParams)));
            LP listJoinProperty = new LP<JoinProperty.Interface>(joinProperty, listInterfaces);

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

    private void initNavigators() {
        NamedListFormEntity namedListFormEntity = new NamedListFormEntity(this, baseClass.named);
        baseClass.named.setListForm(namedListFormEntity, namedListFormEntity.object);

        navigatorWindow = addWindow(new TreeNavigatorWindow("navigator", getString("logics.navigator"), 0, 0, 20, 70));
        relevantFormsWindow = addWindow(new AbstractWindow("relevantForms", getString("logics.forms.relevant.forms"), 0, 70, 20, 29));
        relevantClassFormsWindow = addWindow(new AbstractWindow("relevantClassForms", getString("logics.forms.relevant.class.forms"), 0, 70, 20, 29));
        logWindow = addWindow(new AbstractWindow("log", getString("logics.log"), 0, 70, 20, 29));
        statusWindow = addWindow(new AbstractWindow("status", getString("logics.status"), 0, 99, 100, 1));
        statusWindow.titleShown = false;
        formsWindow = addWindow(new AbstractWindow("forms", getString("logics.forms"), 20, 20, 80, 79));

        baseElement = addNavigatorElement("baseElement", getString("logics.forms"));
        baseElement.window = navigatorWindow;
        adminElement = addNavigatorElement(baseElement, "adminElement", getString("logics.administration"));

        objectElement = addNavigatorElement(adminElement, "objectElement", getString("logics.object"));
        adminElement.add(objectElement);

        accessElement = addNavigatorElement(adminElement, "accessElement", getString("logics.administration.access"));

        UserEditFormEntity userEditForm = addFormEntity(new UserEditFormEntity(null, "userEditForm"));
        customUser.setEditForm(userEditForm, userEditForm.objUser);

        addFormEntity(new UserPolicyFormEntity(accessElement, "userPolicyForm"));
        addFormEntity(new SecurityPolicyFormEntity(accessElement, "securityPolicyForm"));

        eventsElement = addNavigatorElement(adminElement, "eventsElement", getString("logics.administration.events"));
        addFormEntity(new LaunchesFormEntity(eventsElement, "launchesForm"));
        addFormEntity(new ConnectionsFormEntity(eventsElement, "connectionsForm"));
        addFormEntity(new ExceptionsFormEntity(eventsElement, "exceptionsForm"));

        configElement = addNavigatorElement(adminElement, "configElement", getString("logics.administration.config"));
        addFormEntity(new PropertiesFormEntity(configElement, "propertiesForm"));
        addFormEntity(new PhysicalModelFormEntity(configElement, "physicalModelForm"));
        addFormEntity(new FormsFormEntity(configElement, "formsForm"));
        addFormEntity(new NotificationFormEntity(configElement, "notification"));

        catalogElement = addNavigatorElement(adminElement, "catalogElement", getString("logics.administration.catalogs"));
        addFormEntity(new DaysOffFormEntity(catalogElement, "daysOffForm"));
        dictionaryForm = addFormEntity(new DictionariesFormEntity(catalogElement, "dictionariesForm"));
        catalogElement.add(country.getListForm(this).form);

        addFormEntity(new AdminFormEntity(adminElement, "adminForm"));

        addFormEntity(new RemindUserPassFormEntity(null, "remindPasswordLetter"));
    }

    public void initClassForms() {
        objectForm = baseClass.getBaseClassForm(this);
        objectElement.add(objectForm);
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

    Collection<LP[]> checkCUProps = new ArrayList<LP[]>();

    // объединяет разные по классам св-ва

    Collection<LP[]> checkSUProps = new ArrayList<LP[]>();


    @Override
    @IdentityLazy
    public LP is(ValueClass valueClass) {
        return addProperty(null, new LP<ClassPropertyInterface>(valueClass.getProperty()));
    }
    @Override
    @IdentityLazy
    public LP object(ValueClass valueClass) {
        return addJProp(valueClass.toString(), baseLM.and1, 1, is(valueClass), 1);
    }
    @Override
    @IdentityLazy
    public LP vdefault(ConcreteValueClass valueClass) {
        return addProperty(null, new LP<PropertyInterface>(new DefaultValueProperty("default" + valueClass.getSID(), valueClass)));
    }

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

    public static class SeekActionProperty extends CustomActionProperty {

        Property property;

        SeekActionProperty(String sID, String caption, ValueClass[] classes, Property property) {
            super(sID, caption, classes);
            this.property = property;
        }

        public void execute(ExecutionContext context) throws SQLException {
            context.emitExceptionIfNotInFormSession();

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

    public static class LoadActionProperty extends CustomActionProperty {

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

    public static class OpenActionProperty extends CustomActionProperty {

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

    public static class IncrementActionProperty extends CustomActionProperty {

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

    private static class ChangeUserActionProperty extends CustomActionProperty {

        private ChangeUserActionProperty(String sID, ConcreteValueClass userClass) {
            super(sID, getString("logics.user.change.user"), new ValueClass[]{userClass});
        }

        @Override
        public DataClass getValueClass() {
            return LogicalClass.instance;
        }

        public void execute(ExecutionContext context) throws SQLException {
            context.emitExceptionIfNotInFormSession();

            DataObject user = context.getSingleKeyValue();
            if (context.getFormInstance().BL.requiredPassword) {
                context.addAction(new UserReloginClientAction(context.getFormInstance().BL.getUserName(user).trim()));
            } else {
                context.getSession().user.changeCurrentUser(user);
                context.addAction(new UserChangedClientAction());
            }
        }
    }

    private class CheckAggregationsActionProperty extends CustomActionProperty {
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

    private class RecalculateActionProperty extends CustomActionProperty {
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

    private class RecalculateTableColumnActionProperty extends CustomActionProperty {

        private final ClassPropertyInterface tableColumnInterface;

        private RecalculateTableColumnActionProperty(String caption, ValueClass tableColumn) {
            super(genSID(), caption, new ValueClass[]{tableColumn});
            Iterator<ClassPropertyInterface> i = interfaces.iterator();
            tableColumnInterface = i.next();
        }

        @Override
        public void execute(ExecutionContext context) throws SQLException {
            SQLSession sqlSession = context.getSession().sql;

            DataObject tableColumnObject = context.getKeyValue(tableColumnInterface);
            String propertySID = (String) sidTableColumn.read(context, tableColumnObject);

            sqlSession.startTransaction();
            BL.recalculateAggregationTableColumn(sqlSession, propertySID.trim());
            sqlSession.commitTransaction();

            context.addAction(new MessageClientAction(getString("logics.recalculation.was.completed"), getString("logics.recalculation.aggregations")));
        }
    }

    private class PackActionProperty extends CustomActionProperty {
        private PackActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
        }

        @Override
        public void execute(ExecutionContext context) throws SQLException {
            SQLSession sqlSession = context.getSession().sql;

            sqlSession.startTransaction();
            BL.packTables(sqlSession, tableFactory.getImplementTables());
            sqlSession.commitTransaction();

            context.addAction(new MessageClientAction(getString("logics.tables.packing.completed"), getString("logics.tables.packing")));
        }
    }

    private class RecalculateFollowsActionProperty extends CustomActionProperty {
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

    private class RecalculateStatsActionProperty extends CustomActionProperty {
        private RecalculateStatsActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
        }

        @Override
        public void execute(ExecutionContext context) throws SQLException {
            BL.recalculateStats(context.getSession());
        }
    }

    public class DropColumnActionProperty extends CustomActionProperty {
        private DropColumnActionProperty(String sID, String caption, ValueClass dropColumn) {
            super(sID, caption, new ValueClass[] {dropColumn});
        }

        public void execute(ExecutionContext context) throws SQLException {
            DataObject dropColumnObject = context.getSingleKeyValue();
            String columnName = (String) sidDropColumn.read(context, dropColumnObject);
            String tableName = (String) sidTableDropColumn.read(context, dropColumnObject);
            BL.dropColumn(tableName, columnName);

            context.getFormInstance().changeClass((CustomObjectInstance) context.getSingleObjectInstance(), context.getSingleKeyValue(), -1);
            context.applyChanges(BL);
        }

        public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
            super.proceedDefaultDesign(propertyView, view);
            propertyView.design.setIconPath("delete.png");
        }
    }

    class AddBarcodeActionProperty extends CustomActionProperty {

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

    protected Map<List<? extends Property>, Boolean> indexes = new HashMap<List<? extends Property>, Boolean>();

    public void addIndex(LP<?>... lps) {
        List<Property> index = new ArrayList<Property>();
        for (LP<?> lp : lps)
            index.add(lp.property);
        indexes.put(index, lps[0].property.getType() instanceof DataClass);
    }


    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////
    /// Forms
    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////

    private class UserPolicyFormEntity extends FormEntity {
        protected UserPolicyFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.user.users"));

            ObjectEntity objUser = addSingleGroupObject(customUser, nameUserMainRole, name, userLogin, email, barcode);
            setReadOnly(objUser, true);

            addFormActions(this, objUser);
        }
    }

    private class UserEditFormEntity extends FormEntity {

        private final ObjectEntity objUser;
        private final ObjectEntity objRole;

        protected UserEditFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.user.user"));

            objUser = addSingleGroupObject(customUser, userFirstName, userLastName, userLogin, userPassword, email, nameUserMainRole);
            objUser.groupTo.setSingleClassView(ClassViewType.PANEL);

            objRole = addSingleGroupObject(userRole, name, userRoleSID);
            setReadOnly(objRole, true);
            
            addPropertyDraw(objUser, objRole, inUserMainRole);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.addIntersection(design.getGroupObjectContainer(objUser.groupTo),
                                   design.getGroupObjectContainer(objRole.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            
            return design;
        }
    }

    private class SecurityPolicyFormEntity extends FormEntity {

        private ObjectEntity objUserRole;
        private ObjectEntity objPolicy;
        private ObjectEntity objForm;
        private ObjectEntity objTreeForm;
        private TreeGroupEntity treeFormObject;
        private ObjectEntity objProperty;
        private ObjectEntity objTreeProps;
        private ObjectEntity objProps;
        private TreeGroupEntity treePropertyObject;
        private ObjectEntity objDefaultForm;
        private ObjectEntity objTreeDefaultForm;
        private TreeGroupEntity treeDefaultForm;
        private ObjectEntity objDefaultProperty;
        private ObjectEntity objTreeDefaultProps;
        private ObjectEntity objDefaultProps;
        private TreeGroupEntity treeDefaultProperty;

        protected SecurityPolicyFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.policy.security.policy"));

            objUserRole = addSingleGroupObject(userRole, baseGroup, true);
            objPolicy = addSingleGroupObject(policy, getString("logics.policy.additional.policies"), baseGroup, true);
            objForm = addSingleGroupObject(navigatorElement, getString("logics.grid"), true);
            objTreeForm = addSingleGroupObject(navigatorElement, getString("logics.tree"), true);
            objProperty = addSingleGroupObject(property, getString("logics.grid"), true);
            objTreeProps = addSingleGroupObject(abstractGroup, getString("logics.tree"), true);
            objProps = addSingleGroupObject(property, getString("logics.tree"), true);
            objDefaultForm = addSingleGroupObject(navigatorElement, getString("logics.grid"), true);
            objTreeDefaultForm = addSingleGroupObject(navigatorElement, getString("logics.tree"), true);
            objDefaultProperty = addSingleGroupObject(property, getString("logics.grid"), true);
            objTreeDefaultProps = addSingleGroupObject(abstractGroup, getString("logics.tree"), true);
            objDefaultProps = addSingleGroupObject(property, getString("logics.grid"), true);

            objTreeForm.groupTo.setIsParents(addPropertyObject(parentNavigatorElement, objTreeForm));
            treeFormObject = addTreeGroupObject(objTreeForm.groupTo);
            objTreeProps.groupTo.setIsParents(addPropertyObject(parentAbstractGroup, objTreeProps));
            treePropertyObject = addTreeGroupObject(objTreeProps.groupTo, objProps.groupTo);

            objTreeDefaultForm.groupTo.setIsParents(addPropertyObject(parentNavigatorElement, objTreeDefaultForm));
            treeDefaultForm = addTreeGroupObject(objTreeDefaultForm.groupTo);
            objTreeDefaultProps.groupTo.setIsParents(addPropertyObject(parentAbstractGroup, objTreeDefaultProps));
            treeDefaultProperty = addTreeGroupObject(objTreeDefaultProps.groupTo, objDefaultProps.groupTo);

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
            addPropertyDraw(new LP[]{captionAbstractGroup, SIDAbstractGroup, numberAbstractGroup}, objTreeProps);
            addPropertyDraw(new LP[]{captionProperty, SIDProperty, numberProperty}, objProps);
            addPropertyDraw(objUserRole, objProps, permitViewUserRoleProperty, forbidViewUserRoleProperty, permitChangeUserRoleProperty, forbidChangeUserRoleProperty);

            addPropertyDraw(new LP[]{navigatorElementCaption, navigatorElementSID, numberNavigatorElement, permitForm, forbidForm}, objDefaultForm);
            addPropertyDraw(new LP[]{navigatorElementCaption, navigatorElementSID, numberNavigatorElement, permitForm, forbidForm}, objTreeDefaultForm);
            addPropertyDraw(new LP[]{captionProperty, SIDProperty}, objDefaultProperty);
            addPropertyDraw(objDefaultProperty, permitViewProperty, forbidViewProperty, permitChangeProperty, forbidChangeProperty);
            addPropertyDraw(new LP[]{captionAbstractGroup, SIDAbstractGroup, numberAbstractGroup}, objTreeDefaultProps);
            addPropertyDraw(new LP[]{captionProperty, SIDProperty, numberProperty}, objDefaultProps);
            addPropertyDraw(objDefaultProps, permitViewProperty, forbidViewProperty, permitChangeProperty, forbidChangeProperty);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(parentProperty, objProps), Compare.EQUALS, objTreeProps));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(parentProperty, objDefaultProps), Compare.EQUALS, objTreeDefaultProps));

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
            addDefaultOrder(getPropertyDraw(numberProperty, objProps.groupTo), true);
            addDefaultOrder(getPropertyDraw(numberAbstractGroup, objTreeProps.groupTo), true);
            addDefaultOrder(getPropertyDraw(numberProperty, objDefaultProps.groupTo), true);
            addDefaultOrder(getPropertyDraw(numberAbstractGroup, objTreeDefaultProps.groupTo), true);
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
            ContainerView defaultPropertyContainer = design.createContainer(getString("logics.property.properties"));
            defaultPropertyContainer.tabbedPane = true;
            defaultPropertyContainer.add(design.getTreeContainer(treeDefaultProperty));
            defaultPropertyContainer.add(design.getGroupObjectContainer(objDefaultProperty.groupTo));
            defaultPolicyContainer.tabbedPane = true;
            defaultPolicyContainer.add(defaultFormsContainer);
            defaultPolicyContainer.add(defaultPropertyContainer);

            ContainerView rolesContainer = design.createContainer(getString("logics.policy.roles"));
            ContainerView rolePolicyContainer = design.createContainer();
            rolePolicyContainer.tabbedPane = true;
            ContainerView formsContainer = design.createContainer(getString("logics.forms"));
            formsContainer.tabbedPane = true;
            formsContainer.add(design.getTreeContainer(treeFormObject));
            formsContainer.add(design.getGroupObjectContainer(objForm.groupTo));
            rolePolicyContainer.add(formsContainer);
            ContainerView propertiesContainer = design.createContainer(getString("logics.property.properties"));
            propertiesContainer.tabbedPane = true;
            propertiesContainer.add(design.getTreeContainer(treePropertyObject));
            propertiesContainer.add(design.getGroupObjectContainer(objProperty.groupTo));
            rolePolicyContainer.add(propertiesContainer);
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
                    new CompareFilterEntity(addPropertyObject(connectionCurrentStatus, objConnection), Compare.EQUALS, addPropertyObject(addCProp(connectionStatus, "connectedConnection"))),
                    getString("logics.connection.active.connections"),
                    KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
            addRegularFilterGroup(filterGroup);
        }
    }

    private class LaunchesFormEntity extends FormEntity {
        protected LaunchesFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.launch.log"));

            ObjectEntity objLaunch = addSingleGroupObject(launch, baseGroup, true);
            setReadOnly(true);
        }
    }

    class PhysicalModelFormEntity extends FormEntity{
        PropertyDrawEntity recalculateStats;
        ObjectEntity objTable;
        ObjectEntity objKey;
        ObjectEntity objColumn;
        ObjectEntity objDropColumn;

        protected PhysicalModelFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.tables.physical.model"));

            objTable = addSingleGroupObject(table, getString("logics.tables.tables"), baseGroup);
            objKey = addSingleGroupObject(tableKey, getString("logics.tables.keys"), baseGroup);
            objColumn = addSingleGroupObject(tableColumn, getString("logics.tables.columns"), baseGroup);
            objDropColumn = addSingleGroupObject(dropColumn, getString("logics.tables.deleted.column"), baseGroup);
            setReadOnly(objDropColumn, true);
            setReadOnly(dropDropColumn, false);

            recalculateStats = addPropertyDraw(addAProp(new RecalculateStatsActionProperty("recalculateStats", getString("logics.tables.recalculate.stats"))));
            addPropertyDraw(recalculateAggregationTableColumn, objColumn);

            setReadOnly(propertyNameTableColumn, true);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(tableTableKey, objKey), Compare.EQUALS, objTable));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(tableTableColumn, objColumn), Compare.EQUALS, objTable));
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            ContainerView modelContainer = design.createContainer(getString("logics.tables.physical.model"));
            modelContainer.add(design.getGroupObjectContainer(objTable.groupTo));
            modelContainer.add(design.getGroupObjectContainer(objKey.groupTo));
            modelContainer.add(design.getGroupObjectContainer(objColumn.groupTo));
            modelContainer.add(design.get(recalculateStats));

            ContainerView dropColumnsContainer = design.createContainer(getString("logics.tables.deleted.columns"));
            dropColumnsContainer.add(design.getGroupObjectContainer(objDropColumn.groupTo));

            ContainerView container = design.createContainer();
            container.tabbedPane = true;
            container.add(modelContainer);
            container.add(dropColumnsContainer);

            design.getMainContainer().add(0, container);

            return design;
        }
    }

    class PropertiesFormEntity extends FormEntity {
        ObjectEntity objProperties;
        ObjectEntity objProps;
        ObjectEntity objTreeProps;
        TreeGroupEntity treePropertiesObject;
        protected PropertiesFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.tables.properties"));

            objProperties = addSingleGroupObject(property, true);

            objTreeProps = addSingleGroupObject(abstractGroup, true);
            objProps = addSingleGroupObject(property, true);

            objTreeProps.groupTo.setIsParents(addPropertyObject(parentAbstractGroup, objTreeProps));
            treePropertiesObject = addTreeGroupObject(objTreeProps.groupTo, objProps.groupTo);

            addPropertyDraw(new LP[]{captionProperty, SIDProperty, signatureProperty, returnProperty, classProperty, parentProperty, numberProperty, userLoggableProperty, loggableProperty, storedProperty, isSetNotNullProperty}, objProperties);
            addPropertyDraw(new LP[]{captionAbstractGroup, SIDAbstractGroup, baseLM.dumb1, baseLM.dumb1, baseLM.dumb1, parentAbstractGroup, numberAbstractGroup, baseLM.dumb1, baseLM.dumb1, baseLM.dumb1, baseLM.dumb1}, objTreeProps);
            addPropertyDraw(new LP[]{captionProperty, SIDProperty, signatureProperty, returnProperty, classProperty, parentProperty, numberProperty, userLoggableProperty, loggableProperty, storedProperty, isSetNotNullProperty}, objProps);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(parentProperty, objProps), Compare.EQUALS, objTreeProps));

            setReadOnly(true);
            setReadOnly(userLoggableProperty, false);
            setReadOnly(storedProperty, false);
            setReadOnly(isSetNotNullProperty, false);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            ContainerView container = design.createContainer();
            container.tabbedPane = true;

            ContainerView treeContainer = design.createContainer(getString("logics.tree"));
            ContainerView tableContainer = design.createContainer(getString("logics.tables.table"));

            treeContainer.add(design.getTreeContainer(treePropertiesObject));
            treeContainer.add(design.getGroupObjectContainer(objProperties.groupTo));

            tableContainer.add(design.getGroupObjectContainer(objProperties.groupTo));
            
            container.add(treeContainer);
            container.add(tableContainer);

            design.getMainContainer().add(0, container);

            addDefaultOrder(getPropertyDraw(numberProperty, objProps.groupTo), true);
            addDefaultOrder(getPropertyDraw(numberAbstractGroup, objTreeProps.groupTo), true);

            return design;
        }
    }

    public class NotificationFormEntity extends FormEntity {

        private ObjectEntity objNotification;
        private ObjectEntity objProperty;

        public NotificationFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.notification.notifications"));

            objNotification = addSingleGroupObject(notification, getString("logics.notification"));
            objProperty = addSingleGroupObject(property, getString("logics.property.properties"));

            addPropertyDraw(inNotificationProperty, objNotification, objProperty);
            addPropertyDraw(objNotification, subjectNotification, textNotification, emailFromNotification, emailToNotification, emailToCCNotification, emailToBCNotification, isDerivedChangeNotification);
            addObjectActions(this, objNotification);
            addPropertyDraw(objProperty, captionProperty, SIDProperty);
            setForceViewType(textNotification, ClassViewType.PANEL);
            setReadOnly(captionProperty, true);
            setReadOnly(SIDProperty, true);

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(
                    new RegularFilterEntity(genID(),
                            new NotNullFilterEntity(addPropertyObject(inNotificationProperty, objNotification, objProperty)),
                            getString("logics.only.checked"),
                            KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)
                    ), true);
            addRegularFilterGroup(filterGroup);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            ContainerView textContainer = design.createContainer(getString("logics.notification.text"));
            textContainer.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            textContainer.add(design.get(getPropertyDraw(textNotification, objNotification)));
            textContainer.constraints.fillHorizontal = 1.0;
            textContainer.constraints.fillVertical = 1.0;

            PropertyDrawView textView = design.get(getPropertyDraw(textNotification, objNotification));
            textView.constraints.fillHorizontal = 1.0;
            textView.preferredSize = new Dimension(-1, 300);
            textView.panelLabelAbove = true;

            ContainerView specContainer = design.createContainer();
            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objNotification.groupTo));
            specContainer.add(design.getGroupObjectContainer(objProperty.groupTo));
            specContainer.add(textContainer);
            specContainer.tabbedPane = true;

            addDefaultOrder(getPropertyDraw(SIDProperty, objProperty), true);
            return design;
        }
    }

    class FormsFormEntity extends FormEntity{

        ObjectEntity objTreeForm;
        TreeGroupEntity treeFormObject;
        ObjectEntity objUser;
        ObjectEntity objPropertyDraw;
        protected FormsFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.tables.forms"));

            objTreeForm = addSingleGroupObject(navigatorElement, true);
            objTreeForm.groupTo.setIsParents(addPropertyObject(parentNavigatorElement, objTreeForm));

            treeFormObject = addTreeGroupObject(objTreeForm.groupTo);
            addPropertyDraw(new LP[]{navigatorElementSID, navigatorElementCaption, parentNavigatorElement}, objTreeForm);
            objUser = addSingleGroupObject(customUser, getString("logics.user"), userFirstName, userLastName, userLogin);
            objPropertyDraw = addSingleGroupObject(propertyDraw, getString("logics.property.draw"), propertyDrawSID, captionPropertyDraw);

            addPropertyDraw(nameShowPropertyDraw, objPropertyDraw);
            addPropertyDraw(nameShowPropertyDrawCustomUser, objPropertyDraw, objUser);
            addPropertyDraw(columnWidthPropertyDraw, objPropertyDraw);
            addPropertyDraw(columnWidthPropertyDrawCustomUser, objPropertyDraw, objUser);

            objUser.groupTo.initClassView = ClassViewType.PANEL;

            addFixedFilter(new CompareFilterEntity(addPropertyObject(formPropertyDraw, objPropertyDraw), Compare.EQUALS, objTreeForm));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(currentUser), Compare.EQUALS, objUser));

            setReadOnly(true);
            setReadOnly(nameShowPropertyDraw, false);
            setReadOnly(nameShowPropertyDrawCustomUser, false);
            setReadOnly(columnWidthPropertyDrawCustomUser, false);
            setReadOnly(columnWidthPropertyDraw, false);
        }
    }


    class ExceptionsFormEntity extends FormEntity {
        ObjectEntity objExceptions;

        protected ExceptionsFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.tables.exceptions"));
            objExceptions = addSingleGroupObject(exception, getString("logics.tables.exceptions"), messageException, clientClientException, loginClientException, typeException, dateException);
            addPropertyDraw(erTraceException, objExceptions).forceViewType = ClassViewType.PANEL;
            setReadOnly(true);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            ContainerView textContainer = design.createContainer();
            textContainer.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            textContainer.add(design.get(getPropertyDraw(erTraceException, objExceptions)));
            textContainer.constraints.fillHorizontal = 1.0;
            textContainer.constraints.fillVertical = 1.0;

            PropertyDrawView textView = design.get(getPropertyDraw(erTraceException, objExceptions));
            textView.constraints.fillHorizontal = 1.0;
            textView.constraints.fillVertical = 0.5;
            textView.preferredSize = new Dimension(-1, 200);
            textView.panelLabelAbove = true;

            ContainerView specContainer = design.createContainer();
            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objExceptions.groupTo));
            specContainer.add(design.getGroupObjectContainer(objExceptions.groupTo));
            specContainer.add(textContainer);

            return design;
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

            addPropertyDraw(new LP[]{smtpHost, smtpPort, nameEncryptedConnectionType, fromAddress, emailAccount, emailPassword, emailBlindCarbonCopy, disableEmail, webHost, nameDefaultCountry, barcodePrefix, restartServerAction, cancelRestartServerAction, checkAggregationsAction, recalculateAction, recalculateFollowsAction, packAction, runGarbageCollector});
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

            ObjectEntity objDict = addSingleGroupObject("dict", dictionary, getString("logics.dictionary"));
            objDict.groupTo.initClassView = ClassViewType.PANEL;
            ObjectEntity objDictEntry = addSingleGroupObject("dictEntry", dictionaryEntry, getString("logics.dictionary.entries"));

            addPropertyDraw(objDict, baseGroup);
            addPropertyDraw(objDictEntry, baseGroup);

            addObjectActions(this, objDict);
            addObjectActions(this, objDictEntry, objDict);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(entryDictionary, objDictEntry), Compare.EQUALS, objDict));
        }
    }

    private class RemindUserPassFormEntity extends FormEntity { // письмо пользователю о логине
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
