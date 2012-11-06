package platform.server.logics;

import org.apache.log4j.Logger;
import platform.base.identity.DefaultIDGenerator;
import platform.base.identity.IDGenerator;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.KeyStrokes;
import platform.interop.PropertyEditType;
import platform.interop.action.MessageClientAction;
import platform.interop.action.UserChangedClientAction;
import platform.interop.action.UserReloginClientAction;
import platform.interop.form.layout.ContainerType;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.server.Settings;
import platform.server.caches.IdentityLazy;
import platform.server.classes.*;
import platform.server.data.SQLSession;
import platform.server.data.Time;
import platform.server.data.Union;
import platform.server.data.expr.query.PartitionType;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.*;
import platform.server.form.instance.FormInstance;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.ContainerView;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.form.window.AbstractWindow;
import platform.server.form.window.NavigatorWindow;
import platform.server.form.window.TreeNavigatorWindow;
import platform.server.logics.linear.LAP;
import platform.server.logics.linear.LCP;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.*;
import platform.server.logics.property.actions.flow.ApplyActionProperty;
import platform.server.logics.property.actions.flow.BreakActionProperty;
import platform.server.logics.property.actions.flow.CancelActionProperty;
import platform.server.logics.property.actions.flow.ReturnActionProperty;
import platform.server.logics.property.actions.form.*;
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
    public ConcreteCustomClass customUser;
    public ConcreteCustomClass computer;
    public ConcreteCustomClass policy;
    public ConcreteCustomClass session;
    public ConcreteCustomClass userRole;
    public ConcreteCustomClass multiLanguageNamed;
    public ConcreteCustomClass navigatorElement;
    public ConcreteCustomClass form;
    public ConcreteCustomClass navigatorAction;
    public ConcreteCustomClass propertyDraw;
    public ConcreteCustomClass groupObject;
    public StaticCustomClass propertyDrawShowStatus;
    public ConcreteCustomClass abstractGroup;
    public ConcreteCustomClass property;
    public ConcreteCustomClass notification;
    public ConcreteCustomClass scheduledTask;
    public ConcreteCustomClass scheduledTaskLog;
    public ConcreteCustomClass scheduledClientTaskLog;
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

    public ConcreteCustomClass currency;
    public ConcreteCustomClass typeExchange;
    public LCP currencyTypeExchange;
    public LCP nameCurrencyTypeExchange;
    public LCP rateExchange;
    private LCP lessCmpDate;
    public LCP nearestPredDate;
    public LCP nearestRateExchange;

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
    public LCP groeq2;
    public LCP lsoeq2;
    public LCP greater2, less2;
    public LCP greater22, less22;
    public LCP between;
    protected LCP betweenDate;
    public LCP betweenDates;
    public LCP object1, and1, andNot1;
    public LCP equals2, diff2;
    public LCP upper;
    public LCP sum;
    public LCP subtract;
    protected LCP delta;
    public LCP multiply;
    public LCP subtractInteger;
    public LCP subtractIntegerIncl;
    protected LCP sqr;
    protected LCP sqrt;
    public LCP divide;
    public LCP divideInteger;
    public LCP divideIntegerNeg;
    public LCP divideIntegerRnd;
    public LCP sumDate;
    public LCP sumDateTimeDay;
    public LCP subtractDate;
    public LCP toDateTime;
    public LCP timeDate;

    public LCP string2SP, istring2SP;
    public LCP string2, istring2;
    public LCP ustring2SP, ustring2, ustring3, ustring4, ustring5CM;

    protected LCP concat2;
    public LCP percent;
    public LCP percent2;
    public LCP share;
    public LCP weekInDate;
    public LCP numberDOWInDate;
    public LCP numberMonthInDate;
    public LCP yearInDate;
    public LCP dayInDate;
    public LCP dateInTime;
    public LCP timeInDateTime;
    public LCP jumpWorkdays;
    public LCP completeBarcode;

    public LCP numberMonth;
    public LCP numberToMonth;
    public LCP monthInDate;

    public LCP numberDOW;
    public LCP numberToDOW;
    public LCP DOWInDate;

    public LCP vtrue;
    public LCP vzero;
    public LCP vnull;
    public LCP charLength;
    public LCP positive, negative;

    public LCP round;

    public LCP minusInteger;
    public LCP minus;

    public LCP dumb1;
    public LCP dumb2;

    protected LCP castText;
    protected LCP castString;

    public LCP<?> name;
    public LCP<?> date;

    public LCP redColor;
    public LCP yellowColor;

    public LAP formPrint;
    public LAP formEdit;
    public LAP formXls;
    public LAP formNull;
    public LAP formRefresh;
    public LAP formApply;
    public LAP formCancel;
    public LAP formOk;
    public LAP formClose;

    public LCP daysInclBetweenDates;
    public LCP weeksInclBetweenDates;
    public LCP weeksNullInclBetweenDates;

    public LCP sumDateWeekFrom;
    public LCP sumDateWeekTo;

    protected LCP transactionLater;
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
    protected LCP isServerRestarting;
    public LCP<PropertyInterface> barcode;
    public LCP<PropertyInterface> barcodeToObject;
    public LCP barcodeObjectName;
    public LCP equalsObjectBarcode;
    public LAP seekBarcodeAction;
    public LAP barcodeNotFoundMessage;
    public LCP extSID, extSIDToObject;
    public LCP timeCreated, userCreated, nameUserCreated, computerCreated, hostnameComputerCreated;
    public LAP restartServerAction;
    public LAP runGarbageCollector;
    public LAP cancelRestartServerAction;
    public LCP reverseBarcode;

    public LCP userLogin;
    public LCP userPassword;
    public LCP userFirstName;
    public LCP userLastName;
    public LCP userPhone;
    public LCP userPostAddress;
    public LCP userBirthday;
    public LCP userMainRole;
    public LCP customUserMainRole;
    public LCP customUserSIDMainRole;
    public LCP nameUserMainRole;
    public LCP inUserMainRole;
    public LCP userRoleSID;
    public LCP userRoleDefaultForms;
    public LCP forbidAllUserRoleForms;
    public LCP forbidAllUserForm;
    public LCP allowAllUserRoleForms;
    public LCP allowAllUserForm;
    public LCP forbidViewAllUserRoleProperty;
    public LCP forbidViewAllUserForm;
    public LCP allowViewAllUserRoleProperty;
    public LCP allowViewAllUserForm;
    public LCP forbidChangeAllUserRoleProperty;
    public LCP forbidChangeAllUserForm;
    public LCP allowChangeAllUserRoleProperty;
    public LCP allowChangeAllUserForm;

    public LCP userDefaultForms;
    public LCP sidToRole;
    public LCP inUserRole;
    public LCP inLoginSID;
    public LCP currentUserName;
    public LCP<?> loginToUser;

    public LCP email;
    public LCP emailToObject;
    public LAP generateLoginPassword;

    public LAP emailUserPassUser;

    public LCP connectionUser;
    public LCP userNameConnection;
    public LCP connectionComputer;
    public LCP<PropertyInterface> connectionCurrentStatus;
    public LCP connectionFormCount;
    public LCP connectionConnectTime;
    public LCP connectionDisconnectTime;
    public LAP disconnectConnection;

    public LCP symbolCurrency;
    public LCP shortNameCurrency;
    public LCP currencyShortName;

    public LCP launchComputer;
    public LCP computerNameLaunch;
    public LCP launchTime;
    public LCP launchRevision;
    public LP documentNameCurrency;

    public LCP policyDescription;
    protected LCP<?> nameToPolicy;
    public LCP userRolePolicyOrder;
    public LCP userPolicyOrder;

    public LCP hostname;
    public LCP notZero;
    public LCP onlyNotZero;

    public LAP delete;
    public LAP deleteApply;
    public LAP dropString;

    public LAP<?> apply;
    public LCP<?> canceled;

    public LAP flowBreak;
    public LAP flowReturn;
    public LAP<?> cancel;

    public LCP objectClass;
    public LCP objectClassName;
    public LCP classSID;
    public LCP dataName;
    public LCP navigatorElementSID;
    public LCP numberNavigatorElement;
    public LCP navigatorElementCaption;

    public LCP propertyDrawSID;
    public LCP captionPropertyDraw;
    public LCP SIDToPropertyDraw;
    public LCP formPropertyDraw;
    public LCP groupObjectPropertyDraw;
    public LCP SIDNavigatorElementSIDPropertyDrawToPropertyDraw;
    public LCP showPropertyDraw;
    public LCP nameShowPropertyDraw;
    public LCP showPropertyDrawCustomUser;
    public LCP nameShowPropertyDrawCustomUser;
    public LCP showOverridePropertyDrawCustomUser;
    public LCP nameShowOverridePropertyDrawCustomUser;
    public LCP columnWidthPropertyDrawCustomUser;
    public LCP columnWidthPropertyDraw;
    public LCP columnWidthOverridePropertyDrawCustomUser;
    public LCP columnOrderPropertyDrawCustomUser;
    public LCP columnOrderPropertyDraw;
    public LCP columnOrderOverridePropertyDrawCustomUser;
    public LCP columnSortPropertyDrawCustomUser;
    public LCP columnSortPropertyDraw;
    public LCP columnSortOverridePropertyDrawCustomUser;
    public LCP columnAscendingSortPropertyDrawCustomUser;
    public LCP columnAscendingSortPropertyDraw;
    public LCP columnAscendingSortOverridePropertyDrawCustomUser;
    public LCP hasUserPreferencesGroupObject;
    public LCP hasUserPreferencesGroupObjectCustomUser;
    public LCP hasUserPreferencesOverrideGroupObjectCustomUser;

    public LCP groupObjectSID;
    public LCP navigatorElementGroupObject;
    public LCP sidNavigatorElementGroupObject;
    public LCP SIDNavigatorElementSIDGroupObjectToGroupObject;
    
    public LCP messageException;
    public LCP dateException;
    public LCP erTraceException;
    public LCP typeException;
    public LCP clientClientException;
    public LCP loginClientException;
    public LCP captionAbstractGroup;
    public LCP parentAbstractGroup;
    public LCP numberAbstractGroup;
    public LCP SIDAbstractGroup;
    public LCP SIDToAbstractGroup;
    public LCP parentProperty;
    public LCP numberProperty;
    public LCP SIDProperty;
    public LCP loggableProperty;
    public LCP userLoggableProperty;
    public LCP storedProperty;
    public LCP isSetNotNullProperty;
    public LCP signatureProperty;
    public LCP returnProperty;
    public LCP classProperty;
    public LCP captionProperty;
    public LCP SIDToProperty;
    public LCP isEventNotification;
    public LCP emailFromNotification;
    public LCP emailToNotification;
    public LCP emailToCCNotification;
    public LCP emailToBCNotification;
    public LCP textNotification;
    public LCP subjectNotification;
    public LCP inNotificationProperty;
    public LCP nameScheduledTask;
    public LCP runAtStartScheduledTask;
    public LCP startDateScheduledTask;
    public LCP periodScheduledTask;
    public LCP activeScheduledTask;
    public LCP inScheduledTaskProperty;
    public LCP activeScheduledTaskProperty;
    public LCP orderScheduledTaskProperty;
    public LCP propertyScheduledTaskLog;
    public LCP resultScheduledTaskLog;
    public LCP dateStartScheduledTaskLog;
    public LCP dateFinishScheduledTaskLog;
    public LCP scheduledTaskScheduledTaskLog;
    public LCP currentScheduledTaskLogScheduledTask;
    public LCP messageScheduledClientTaskLog;
    public LCP scheduledTaskLogScheduledClientTaskLog;
    public LCP permitViewUserRoleProperty;
    public LCP permitViewUserProperty;
    public LCP forbidViewUserRoleProperty;
    public LCP forbidViewUserProperty;
    public LCP permitChangeUserRoleProperty;
    public LCP permitChangeUserProperty;
    public LCP forbidChangeUserRoleProperty;
    public LCP forbidChangeUserProperty;
    public LCP permitViewProperty;
    public LCP forbidViewProperty;
    public LCP permitChangeProperty;
    public LCP forbidChangeProperty;
    public LCP notNullPermissionUserProperty;

    public LCP SIDToNavigatorElement;
    public LCP parentNavigatorElement;
    public LCP isNavigatorElement;
    public LCP isNavigatorAction;
    public LCP isForm;
    public LCP permitUserRoleForm;
    public LCP forbidUserRoleForm;
    public LCP permitUserForm;
    public LCP forbidUserForm;
    public LCP permitForm;
    public LCP forbidForm;
    public LCP userRoleFormDefaultNumber;
    public LCP userFormDefaultNumber;

    public LCP sidTable;
    public LCP sidTableKey;
    public LCP nameTableKey;
    public LCP sidTableColumn;
    public LCP propertyTableColumn;
    public LCP propertyNameTableColumn;
    public LCP sidToTable;
    public LCP sidToTableKey;
    public LCP sidToTableColumn;
    public LCP tableTableKey;
    public LCP classTableKey;
    public LCP tableTableColumn;
    public LCP rowsTable;
    public LCP sparseColumnsTable;
    public LCP quantityTableKey;
    public LCP quantityTableColumn;
    public LCP notNullQuantityTableColumn;
    public LCP perCentNotNullTableColumn;
    public LAP recalculateAggregationTableColumn;

    public LCP<?> sidDropColumn;
    public LCP sidToDropColumn;
    public LCP<?> sidTableDropColumn;
    public LCP timeDropColumn;
    public LCP revisionDropColumn;
    public LAP dropDropColumn;

    public LCP customID;
    public LCP stringID;
    public LCP integerID;
    public LCP dateID;

    public LCP objectByName;
    public LAP seekObjectName;

    public LCP webHost;

    public LCP encryptedConnectionType;
    public LCP nameEncryptedConnectionType;
    public LCP smtpHost;
    public LCP smtpPort;
    public LCP emailAccount;
    public LCP emailPassword;
    public LCP emailBlindCarbonCopy;
    public LCP fromAddress;
    public LCP disableEmail;

    public LCP defaultBackgroundColor;
    public LCP defaultOverrideBackgroundColor;
    public LCP defaultForegroundColor;
    public LCP defaultOverrideForegroundColor;

    protected LCP nameToObject;

    protected LCP termDictionary;
    protected LCP translationDictionary;
    public LCP insensitiveDictionary;
    public LCP insensitiveTermDictionary;
    protected LCP entryDictionary;
    protected LCP nameEntryDictionary;
    public LCP translationDictionaryTerm;
    public LCP insensitiveTranslationDictionaryTerm;

    private LCP selectRoleForms;
    private LAP selectUserRoles;

    private LAP checkAggregationsAction;
    private LAP recalculateAction;
    private LAP recalculateFollowsAction;
    private LAP analyzeDBAction;
    private LAP packAction;
    private LAP serviceDBAction;

    public SelectionPropertySet selection;
    protected CompositeNamePropertySet compositeName;
    public ObjectValuePropertySet objectValue;


    // navigators
    public NavigatorElement<T> baseElement;
    public NavigatorElement<T> objectElement;
    public NavigatorElement<T> adminElement;

    public NavigatorElement<T> applicationElement;
    public NavigatorElement<T> accessElement;
    public NavigatorElement<T> eventsElement;
    public NavigatorElement<T> configElement;
    public NavigatorElement<T> catalogElement;

    public FormEntity<T> objectForm;

    public NavigatorWindow navigatorWindow;
    public AbstractWindow relevantFormsWindow;
    public AbstractWindow relevantClassFormsWindow;
    public AbstractWindow logWindow;
    public AbstractWindow statusWindow;
    public AbstractWindow formsWindow;

    public TableFactory tableFactory;

    public final StringClass navigatorElementSIDClass = StringClass.get(50);
    public final StringClass navigatorElementCaptionClass = StringClass.get(250);

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
    public void initModule() {
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

        contact = addAbstractClass("contact", getString("logics.user.contact"), emailObject);
        user = addAbstractClass("user", getString("logics.user"), baseClass);
        customUser = addConcreteClass("customUser", getString("logics.user.ordinary.user"), user, contact, barcodeObject);
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

        multiLanguageNamed = addConcreteClass("multiLanguageNamed", "Мультиязычный объект", baseClass);

        navigatorElement = addConcreteClass("navigatorElement", getString("logics.navigator.element"), baseClass);
        form = addConcreteClass("form", getString("logics.forms.form"), navigatorElement);
        navigatorAction = addConcreteClass("navigatorAction", getString("logics.forms.action"), navigatorElement);
        propertyDraw = addConcreteClass("propertyDraw", getString("logics.property.draw"), baseClass);
        propertyDrawShowStatus = addStaticClass("propertyDrawShowStatus", getString("logics.forms.property.show"),
                new String[]{"Show", "Hide"},
                new String[]{getString("logics.property.draw.show"), getString("logics.property.draw.hide")});
        groupObject = addConcreteClass("groupObject", getString("logics.group.object"), baseClass);
        abstractGroup = addConcreteClass("abstractGroup", getString("logics.property.group"), baseClass);
        property = addConcreteClass("property", getString("logics.property"), baseClass);
        notification = addConcreteClass("notification", getString("logics.notification"), baseClass);
        scheduledTask = addConcreteClass("scheduledTask", getString("logics.scheduled.task"), baseClass);
        scheduledTaskLog = addConcreteClass("scheduledTaskLog", getString("logics.scheduled.task.log"), baseClass);
        scheduledClientTaskLog = addConcreteClass("scheduledClientTaskLog", getString("logics.scheduled.task.log.client"), baseClass);
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

        currency = addConcreteClass("currency", getString("logics.currency"), baseClass.named);
        typeExchange = addConcreteClass("typeExchange", "Тип обмена", baseClass.named);

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

        addTable("userTable", user);
        addTable("contact", contact);
        addTable("customUser", customUser);
        addTable("userRole", userRole);
        addTable("policy", policy);
        addTable("loginSID", StringClass.get(30), StringClass.get(30));
        addTable("objectObjectDate", baseClass, baseClass, DateClass.instance);
        addTable("navigatorElement", navigatorElement);
        addTable("abstractGroup", abstractGroup);
        addTable("property", property);
        addTable("propertyDraw", propertyDraw);
        addTable("groupObject", groupObject);
        addTable("groupObjectCustomUser", groupObject, customUser);
        addTable("exception", exception);
        addTable("notification", notification);
        addTable("scheduledTask", scheduledTask);
        addTable("scheduledTaskLog", scheduledTaskLog);
        addTable("scheduledClientTaskLog", scheduledClientTaskLog);
        addTable("launch", launch);
        addTable("transaction", transaction);
        addTable("named", baseClass.named);
        addTable("sidClass", baseClass.sidClass);
        addTable("barcodeObject", barcodeObject);
        addTable("emailObject", emailObject);
        addTable("externalObject", externalObject);
        addTable("historyObject", historyObject);
        addTable("dictionary", dictionary);
        addTable("dictionaryEntry", dictionaryEntry);

        addTable("session", session);
        addTable("connection", connection);
        addTable("computer", computer);

        addTable("sessionObject", session, baseClass);

        addTable("connectionNavigatorElement", connection, navigatorElement);
        addTable("userRoleNavigatorElement", userRole, navigatorElement);
        addTable("userRoleProperty", userRole, property);
        addTable("notificationProperty", notification, property);
        addTable("scheduledTaskProperty", scheduledTask, property);
        addTable("scheduledTaskScheduledTaskLog", scheduledTask, scheduledTaskLog);
        addTable("propertyDrawCustomUser", propertyDraw, customUser);
        addTable("formPropertyDraw", form, propertyDraw);

        addTable("tables", table);
        addTable("tableKey", tableKey);
        addTable("tableColumn", tableColumn);
        addTable("dropColumn", dropColumn);

        addTable("customUserRole", customUser, userRole);
        addTable("userRolePolicy", userRole, policy);

        addTable("month", month);
        addTable("dow", DOW);

        addTable("typeExchange", typeExchange);
        addTable("currency", currency);
        addTable("rateExchange", typeExchange, currency, DateClass.instance);
    }

    @Override
    public void initProperties() {

        canceled = addProperty(null, new LCP<ClassPropertyInterface>(new SessionDataProperty("canceled", "Canceled", LogicalClass.instance)));

        apply = addAProp(new ApplyActionProperty(BL, canceled.property));
        cancel = addAProp(new CancelActionProperty());

        flowBreak = addProperty(null, new LAP(new BreakActionProperty()));
        flowReturn = addProperty(null, new LAP(new ReturnActionProperty()));

        selection = new SelectionPropertySet();
        sessionGroup.add(selection);

        objectValue = new ObjectValuePropertySet();
        baseGroup.add(objectValue);

        compositeName = new CompositeNamePropertySet();
        privateGroup.add(compositeName);

        classSID = addDProp("classSID", getString("logics.statcode"), StringClass.get(250), baseClass.sidClass);
        dataName = addDProp("name", getString("logics.name"), InsensitiveStringClass.get(110), baseClass.named);
        ((CalcProperty)dataName.property).aggProp = true;

        symbolCurrency = addDProp(baseGroup, "symbolCurrency", getString("logics.currency.symbol.currency"), StringClass.get(5), currency);
        shortNameCurrency = addDProp(baseGroup, "shortNameCurrency", getString("logics.currency.short.name.currency"), StringClass.get(3), currency);
        currencyShortName = addAGProp(baseGroup, "currencyShortName", getString("logics.currency.short.name.currency"), shortNameCurrency);
        documentNameCurrency = addDProp(baseGroup, "documentNameCurrency", getString("logics.currency.document.name.currency"), StringClass.get(10), currency);

        // математические св-ва
        equals2 = addCFProp("equals2", Compare.EQUALS);
        object1 = addAFProp();
        and1 = addAFProp("and1", false);
        andNot1 = addAFProp(true);
        concat2 = addCCProp(2);
        groeq2 = addCFProp(Compare.GREATER_EQUALS);
        greater2 = addCFProp("greater2", Compare.GREATER);
        lsoeq2 = addCFProp(Compare.LESS_EQUALS);
        less2 = addCFProp(Compare.LESS);
        greater22 = addJProp(greater2, concat2, 1, 2, concat2, 3, 4);
        less22 = addJProp(less2, concat2, 1, 2, concat2, 3, 4);
        diff2 = addCFProp("diff2", Compare.NOT_EQUALS);
        between = addJProp("between", getString("logics.between"), and1, groeq2, 1, 2, groeq2, 3, 1);

        string2SP = addSProp("string2SP", 2);
        istring2SP = addInsensitiveSProp("istring2SP", 2);

        string2 = addSProp("string2", 2, "");
        istring2 = addInsensitiveSProp("istring2", 2, "");

        ustring2SP = addSFUProp("ustring2SP", " ", 2);
        ustring2 = addSFUProp("ustring2", "", 2);
        ustring3 = addSFUProp("ustring3", "", 3);
        ustring4 = addSFUProp("ustring4", "", 4);

        ustring5CM = addSFUProp("ustring5CM", ",", 5);

        upper = addSFProp("upper", "prm1", 1);

        sum = addSFProp("sum", "((prm1)+(prm2))", 2);
        sumDate = addSFProp("sumDate", "((prm1)+(prm2))", DateClass.instance, 2);

        sumDateTimeDay = addSFProp("sumDateTimeDay", "((prm1)+(prm2)*CAST('1 days' AS INTERVAL))", DateTimeClass.instance, 2);

        multiply = addMFProp("multiply", 2);

        subtract = addSFProp("subtract", "((prm1)-(prm2))", 2);
        delta = addSFProp("delta", "abs((prm1)-(prm2))", 2);
        subtractDate = addSFProp("subtractDate", "((prm1)-(prm2))", DateClass.instance, 2);
        subtractInteger = addSFProp("subtractInteger", "((prm1)-(prm2))", IntegerClass.instance, 2);
        subtractIntegerIncl = addSFProp("subtractIntegerIncl", "((prm1)-(prm2)+1)", IntegerClass.instance, 2);

        divide = addSFProp("divide", "((prm1)/(prm2))", 2);
        divideInteger = addSFProp("divideInteger", "CAST(CAST(trunc(prm1) AS integer)/CAST(trunc(prm2) as integer) as integer)", IntegerClass.instance, 2);
        divideIntegerNeg = addSFProp("divideIntegerNeg", "CASE WHEN CAST((prm1) AS integer)<0 THEN -CAST(((-CAST((prm1) as integer)-1)/CAST((prm2) as integer)) as integer) ELSE CAST(CAST((prm1) as integer)/CAST((prm2) as integer) as integer) END", IntegerClass.instance, 2);
        divideIntegerRnd = addSFProp("divideIntegerRnd", "CAST(round((prm1)/(prm2),0) as integer)", IntegerClass.instance, 2);

        sqr = addSFProp("sqr", "(prm1)*(prm1)", 1);
        sqrt = addSFProp("sqrt", "sqrt(prm1)", 1);
        percent = addSFProp("percent", "((prm1)*(prm2)/100)", 2);
        share = addSFProp("share", "((prm1)*100/(prm2))", 2);

        jumpWorkdays = addSFProp("jumpWorkdays", "jumpWorkdays(prm1, prm2, prm3)", DateClass.instance, 3); //1 - country, 2 - date, 3 - days to jump
        completeBarcode = addSFProp("completeBarcode", "completeBarcode(prm1)", StringClass.get(13), 1);

        vtrue = addCProp(getString("logics.true"), LogicalClass.instance, true);
        vzero = addCProp("0", DoubleClass.instance, 0);
        vnull = addProperty(privateGroup, new LCP<PropertyInterface>(NullValueProperty.instance));

        round = addSFProp("round", "round(CAST((prm1) as numeric),prm2)", 2);

        minus = addSFProp("minus", "(-(prm1))", 1);

        dumb1 = dumb(1);
        dumb2 = dumb(2);

        castText = addSFProp("CAST((prm1) as text)", TextClass.instance, 1);
        castString = addSFProp("CAST((prm1) as char(50))", StringClass.get(50), 1);

        charLength = addSFProp("char_length(prm1)", IntegerClass.instance, 1);

        positive = addJProp(greater2, 1, vzero);
        negative = addJProp(less2, 1, vzero);

        weekInDate = addSFProp("weekInDate", "(extract(week from (prm1)))", IntegerClass.instance, 1);
        numberDOWInDate = addSFProp("numberDOWInDate", "(extract(dow from (prm1)))", IntegerClass.instance, 1);
        numberMonthInDate = addSFProp("numberMonthInDate", "(extract(month from (prm1)))", IntegerClass.instance, 1);
        yearInDate = addSFProp("yearInDate", "(extract(year from (prm1)))", IntegerClass.instance, 1);
        dayInDate = addJProp("dayInDate", "День даты", baseLM.and1, addSFProp("(extract(day from (prm1)))", IntegerClass.instance, 1), 1, is(DateClass.instance), 1);

        dateInTime = addSFProp("dateInTime", "(CAST((prm1) as date))", DateClass.instance, 1);
        toDateTime = addSFProp("toDateTime", "to_timestamp(CAST(prm1 as char(10)) || CAST(prm2 as char(8)), \'YYYY-MM-DDHH24:MI:SS\')", DateTimeClass.instance, 2);
        timeDate = addSFProp("timeDate", "(CAST((prm1) as timestamp))", DateTimeClass.instance, 1);
        timeInDateTime = addSFProp("timeInDateTime", "(CAST((prm1) as time))", TimeClass.instance, 1);

        numberMonth = addOProp(baseGroup, "numberMonth", true, getString("logics.month.number"), addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), is(month), 1), PartitionType.SUM, true, true, 0, 1);
        numberToMonth = addAGProp("numberToMonth", getString("logics.month.id"), numberMonth);
        monthInDate = addJProp("monthInDate", getString("logics.month.id"), numberToMonth, numberMonthInDate, 1);

        numberDOW = addJProp(baseGroup, "numberDOW", true, getString("logics.week.day.number"), subtractInteger,
                addOProp("numberDOWP1", getString("logics.week.day.number.plus.one"), PartitionType.SUM, addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), is(DOW), 1), true, false, 0, 1), 1,
                addCProp(IntegerClass.instance, 1));
        numberToDOW = addAGProp("numberToDOW", getString("logics.week.day.id"), numberDOW);
        DOWInDate = addJProp("DOWInDate", getString("logics.week.day.id"), numberToDOW, numberDOWInDate, 1);

        delete = addAProp(baseClass.unknown.getChangeClassAction());

        deleteApply = addListAProp("deleteApply", delete.property.caption, delete, 1, apply);
        deleteApply.setImage("delete.png");
        deleteApply.setEditKey(KeyStrokes.getDeleteActionPropertyKeyStroke());
        deleteApply.setShowEditKey(false);
        deleteApply.setAskConfirm(true);
        deleteApply.setShouldBeLast(true);

        dropString = addAProp(new DropObjectActionProperty(StringClass.get(13)));

        date = addDProp(baseGroup, "date", getString("logics.date"), DateClass.instance, transaction);

        redColor = addCProp(ColorClass.instance, Color.RED);
        yellowColor = addCProp(ColorClass.instance, Color.YELLOW);

        formApply = addProperty(null, new LAP(new FormApplyActionProperty(BL)));
        formCancel = addProperty(null, new LAP(new FormCancelActionProperty()));
        formPrint = addProperty(null, new LAP(new PrintActionProperty()));
        formEdit = addProperty(null, new LAP(new EditActionProperty()));
        formXls = addProperty(null, new LAP(new XlsActionProperty()));
        formNull = addProperty(null, new LAP(new NullActionProperty()));
        formRefresh = addProperty(null, new LAP(new RefreshActionProperty()));
        formOk = addProperty(null, new LAP(new OkActionProperty()));
        formClose = addProperty(null, new LAP(new CloseActionProperty()));

        notZero = addJProp(diff2, 1, vzero);
        onlyNotZero = addJProp(andNot1, 1, addJProp(equals2, 1, vzero), 1);

        daysInclBetweenDates = addJProp("daysInclBetweenDates", getString("logics.date.quantity.days"), and(false, false), addJProp(subtractIntegerIncl, 2, 1), 1, 2, is(DateClass.instance), 1, is(DateClass.instance), 2);
        weeksInclBetweenDates = addJProp("weeksInclBetweenDates", getString("logics.date.quantity.weeks"), divideInteger, daysInclBetweenDates, 1, 2, addCProp(IntegerClass.instance, 7));
        weeksNullInclBetweenDates = addJProp("weeksNullInclBetweenDates", getString("logics.date.quantity.weeks"), onlyNotZero, weeksInclBetweenDates, 1, 2);

        sumDateWeekFrom = addJProp("sumDateWeekFrom", getString("logics.date.from"), and(false, false), addSFProp("((prm1)+(prm2)*7)", DateClass.instance, 2), 1, 2, is(DateClass.instance), 1, is(IntegerClass.instance), 2);
        sumDateWeekTo = addJProp("sumDateWeekTo", getString("logics.date.to"), and(false, false), addSFProp("((prm1)+((prm2)*7+6))", DateClass.instance, 2), 1, 2, is(DateClass.instance), 1, is(IntegerClass.instance), 2);

        betweenDates = addJProp(getString("logics.date.of.doc.between"), between, object(DateClass.instance), 1, object(DateClass.instance), 2, object(DateClass.instance), 3);
        betweenDate = addJProp(getString("logics.date.of.doc.between"), betweenDates, date, 1, 2, 3);

        transactionLater = addSUProp(getString("logics.transaction.later"), Union.OVERRIDE, addJProp(getString("logics.date.later"), greater2, date, 1, date, 2),
                                     addJProp("", and1, addJProp(getString("logics.date.equals.date"), equals2, date, 1, date, 2), 1, 2, addJProp(getString("logics.transaction.code.later"), greater2, 1, 2), 1, 2));

        hostname = addDProp(baseGroup, "hostname", getString("logics.host.name"), InsensitiveStringClass.get(100), computer);

        currentDate = addDProp(baseGroup, "currentDate", getString("logics.date.current.date"), DateClass.instance);
        currentMonth = addJProp(baseGroup, "currentMonth", getString("logics.date.current.month"), numberMonthInDate, currentDate);
        currentYear = addJProp(baseGroup, "currentYear", getString("logics.date.current.year"), yearInDate, currentDate);
        currentHour = addTProp("currentHour", getString("logics.date.current.hour"), Time.HOUR);
        currentMinute = addTProp("currentMinute", getString("logics.date.current.minute"), Time.MINUTE);
        currentEpoch = addTProp("currentEpoch", getString("logics.date.current.epoch"), Time.EPOCH);
        currentDateTime = addTProp("currentDateTime", getString("logics.date.current.datetime"), Time.DATETIME);
        currentTime = addJProp("currentTime", getString("logics.date.current.time"), timeInDateTime, currentDateTime);
        currentUser = addProperty(null, new LCP<PropertyInterface>(new CurrentUserFormulaProperty("currentUser", user)));
        currentSession = addProperty(null, new LCP<ClassPropertyInterface>(new CurrentSessionDataProperty("currentSession", session)));

        currentComputer = addProperty(null, new LCP<PropertyInterface>(new CurrentComputerFormulaProperty("currentComputer", computer)));
        hostnameCurrentComputer = addJProp("hostnameCurrentComputer", getString("logics.current.computer.hostname"), hostname, currentComputer);

        isServerRestarting = addProperty(null, new LCP<PropertyInterface>(new IsServerRestartingFormulaProperty("isServerRestarting")));
        changeUser = addProperty(null, new LAP(new ChangeUserActionProperty("changeUser", customUser)));

        userLogin = addDProp(baseGroup, "userLogin", getString("logics.user.login"), StringClass.get(30), customUser);
        loginToUser = addAGProp("loginToUser", getString("logics.user"), userLogin);
        userPassword = addDProp(publicGroup, "userPassword", getString("logics.user.password"), StringClass.get(30), customUser);
        userPassword.setEchoSymbols(true);
        userFirstName = addDProp(publicGroup, "userFirstName", getString("logics.user.firstname"), StringClass.get(30), contact);
        userFirstName.setMinimumCharWidth(10);

        userLastName = addDProp(publicGroup, "userLastName", getString("logics.user.lastname"), StringClass.get(30), contact);
        userLastName.setMinimumCharWidth(10);

        userPhone = addDProp(publicGroup, "userPhone", getString("logics.user.phone"), StringClass.get(30), contact);
        userPhone.setMinimumCharWidth(10);

        userPostAddress = addDProp(publicGroup, "userPostAddress", getString("logics.user.postAddress"), StringClass.get(100), contact);
        userPostAddress.setMinimumCharWidth(20);

        userBirthday = addDProp(publicGroup, "userBirthday", getString("logics.user.birthday"),  DateClass.instance, contact);

        userRoleSID = addDProp(baseGroup, "userRoleSID", getString("logics.user.identificator"), StringClass.get(30), userRole);
        sidToRole = addAGProp(idGroup, "sidToRole", getString("logics.user.role.id"), userRole, userRoleSID);
        inUserRole = addDProp(baseGroup, "inUserRole", getString("logics.user.role.in"), LogicalClass.instance, customUser, userRole);
        userRoleDefaultForms = addDProp(baseGroup, "userRoleDefaultForms", getString("logics.user.displaying.forms.by.default"), LogicalClass.instance, userRole);
        inLoginSID = addJProp("inLoginSID", true, getString("logics.login.has.a.role"), inUserRole, loginToUser, 1, sidToRole, 2);

        email = addDProp(baseGroup, "email", getString("logics.email"), StringClass.get(50), contact);
        email.setRegexp("^[-a-zA-Z0-9!#$%&'*+/=?^_`{|}~]+(?:\\.[-a-zA-Z0-9!#$%&'*+/=?^_`{|}~]+)*@(?:[a-zA-Z0-9]([-a-zA-Z0-9]{0,61}[a-zA-Z0-9])?\\.)*(?:aero|arpa|asia|biz|cat|com|coop|edu|gov|info|int|jobs|mil|mobi|museum|name|net|org|pro|tel|travel|[a-zA-Z][a-zA-Z])$");
        email.setRegexpMessage("<html>Неверный формат e-mail</html>");

        emailToObject = addAGProp("emailToObject", getString("logics.email.to.object"), email);

        generateLoginPassword = addAProp(actionGroup, new GenerateLoginPasswordActionProperty(email, userLogin, userPassword, customUser));

        name = addCUProp(recognizeGroup, "commonName", getString("logics.name"), dataName,
                addJProp(istring2SP, userFirstName, 1, userLastName, 1));
        ((CalcProperty)name.property).aggProp = true;

        connectionComputer = addDProp("connectionComputer", getString("logics.computer"), computer, connection);
        addJProp(baseGroup, getString("logics.computer"), hostname, connectionComputer, 1);
        connectionUser = addDProp("connectionUser", getString("logics.user"), customUser, connection);
        userNameConnection = addJProp(baseGroup, getString("logics.user"), userLogin, connectionUser, 1);
        connectionCurrentStatus = addDProp("connectionCurrentStatus", getString("logics.connection.status"), connectionStatus, connection);
        addJProp(baseGroup, getString("logics.connection.status"), name, connectionCurrentStatus, 1);

        connectionConnectTime = addDProp(baseGroup, "connectionConnectTime", getString("logics.connection.connect.time"), DateTimeClass.instance, connection);
        connectionDisconnectTime = addDProp(baseGroup, "connectionDisconnectTime", getString("logics.connection.disconnect.time"), DateTimeClass.instance, connection);
        connectionDisconnectTime.setEventChangePrevSet(currentDateTime,
                addJProp(equals2, connectionCurrentStatus, 1, addCProp(connectionStatus, "disconnectedConnection")), 1);
        disconnectConnection = addProperty(null, new LAP(new DisconnectActionProperty(BL, this, connection)));
        addIfAProp(baseGroup, getString("logics.connection.disconnect"), true, connectionDisconnectTime, 1, disconnectConnection, 1);

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

        nameToPolicy = addAGProp("nameToPolicy", getString("logics.policy"), policy, name);
        policyDescription = addDProp(baseGroup, "policyDescription", getString("logics.policy.description"), StringClass.get(100), policy);

        userRolePolicyOrder = addDProp(baseGroup, "userRolePolicyOrder", getString("logics.policy.order"), IntegerClass.instance, userRole, policy);
        userPolicyOrder = addJProp(baseGroup, "userPolicyOrder", getString("logics.policy.order"), userRolePolicyOrder, userMainRole, 1, 2);

        barcode = addDProp(recognizeGroup, "barcode", getString("logics.barcode"), StringClass.get(Settings.instance.getBarcodeLength()), barcodeObject);

        barcode.setFixedCharWidth(13);
        barcodeToObject = addAGProp("barcodeToObject", getString("logics.object"), barcode);
        barcodeObjectName = addJProp(baseGroup, "barcodeObjectName", getString("logics.object"), name, barcodeToObject, 1);

        equalsObjectBarcode = addJProp(equals2, barcode, 1, 2);

        seekBarcodeAction = addJoinAProp(getString("logics.barcode.search"), addSAProp(null), barcodeToObject, 1);
        barcodeNotFoundMessage = addIfAProp(addJProp(baseLM.andNot1, is(StringClass.get(13)), 1, barcodeToObject, 1), 1, addMAProp(getString("logics.barcode.not.found"), getString("logics.error")));

        extSID = addDProp(recognizeGroup, "extSID", getString("logics.extsid"), StringClass.get(100), externalObject);
        extSIDToObject = addAGProp("extSIDToObject", getString("logics.object"), extSID);
        
        timeCreated = addDProp(historyGroup, "timeCreated", getString("logics.timecreated"), DateTimeClass.instance, historyObject);
        userCreated = addDProp(idGroup, "userCreated", getString("logics.usercreated"), customUser, historyObject);
        nameUserCreated = addJProp(historyGroup, "nameUserCreated", getString("logics.usercreated"), name, userCreated, 1);
        nameUserCreated.setMinimumCharWidth(10); nameUserCreated.setPreferredCharWidth(20);
        computerCreated = addDProp(idGroup, "computerCreated", getString("logics.computercreated"), computer, historyObject);
        hostnameComputerCreated = addJProp(historyGroup, "hostnameComputerCreated", getString("logics.computercreated"), hostname, computerCreated, 1);
        hostnameComputerCreated.setMinimumCharWidth(10); hostnameComputerCreated.setPreferredCharWidth(20);
        
        timeCreated.setEventChangeNew(currentDateTime, is(historyObject), 1);
        userCreated.setEventChangeNew(currentUser, is(historyObject), 1);
        computerCreated.setEventChangeNew(currentComputer, is(historyObject), 1);

        restartServerAction = addIfAProp(getString("logics.server.stop"), true, isServerRestarting, addRestartActionProp());
        runGarbageCollector = addGarbageCollectorActionProp();
        cancelRestartServerAction = addIfAProp(getString("logics.server.cancel.stop"), isServerRestarting, addCancelRestartActionProp());

        checkAggregationsAction = addProperty(null, new LAP(new CheckAggregationsActionProperty("checkAggregationsAction", getString("logics.check.aggregations"))));
        recalculateAction = addProperty(null, new LAP(new RecalculateActionProperty("recalculateAction", getString("logics.recalculate.aggregations"))));
        recalculateFollowsAction = addProperty(null, new LAP(new RecalculateFollowsActionProperty("recalculateFollowsAction", getString("logics.recalculate.follows"))));
        analyzeDBAction = addProperty(null, new LAP(new AnalyzeDBActionProperty("analyzeDBAction", getString("logics.vacuum.analyze"))));
        packAction = addProperty(null, new LAP(new PackActionProperty("packAction", getString("logics.tables.pack"))));
        serviceDBAction = addProperty(null, new LAP(new ServiceDBActionProperty("serviceDBAction", getString("logics.service.db"))));

        currentUserName = addJProp("currentUserName", getString("logics.user.current.user.name"), name, currentUser);

        reverseBarcode = addSDProp("reverseBarcode", getString("logics.barcode.reverse"), LogicalClass.instance);

        objectClass = addProperty(null, new LCP<ClassPropertyInterface>(baseClass.getObjectClassProperty()));
        objectClassName = addJProp(baseGroup, "objectClassName", getString("logics.object.class"), name, objectClass, 1);
        objectClassName.makeLoggable(this, true);

        navigatorElementSID = addDProp(baseGroup, "navigatorElementSID", getString("logics.forms.code"), navigatorElementSIDClass, navigatorElement);
        numberNavigatorElement = addDProp(baseGroup, "numberNavigatorElement", getString("logics.number"), IntegerClass.instance, navigatorElement);
        navigatorElementCaption = addDProp(baseGroup, "navigatorElementCaption", getString("logics.forms.name"), navigatorElementCaptionClass, navigatorElement);
        SIDToNavigatorElement = addAGProp("SIDToNavigatorElement", getString("logics.forms.form"), navigatorElementSID);
        parentNavigatorElement = addDProp("parentNavigatorElement", getString("logics.forms.parent.form"), navigatorElement, navigatorElement);
        isNavigatorElement = addJProp("isNavigatorElement", and(true, true), is(navigatorElement), 1, is(form), 1, is(navigatorAction), 1);
        isForm = is(form);
        isNavigatorAction = is(navigatorAction);

        propertyDrawSID = addDProp(baseGroup, "propertyDrawSID", getString("logics.forms.property.draw.code"), propertySIDValueClass, propertyDraw);
        captionPropertyDraw = addDProp(baseGroup, "captionPropertyDraw", getString("logics.forms.property.draw.caption"), propertyCaptionValueClass, propertyDraw);
        formPropertyDraw = addDProp(baseGroup, "formPropertyDraw", getString("logics.forms.form"), form, propertyDraw);
        groupObjectPropertyDraw = addDProp(baseGroup, "groupObjectPropertyDraw", getString("logics.group.object"), groupObject, propertyDraw);
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
        columnOrderPropertyDrawCustomUser = addDProp(baseGroup, "columnOrderPropertyDrawCustomUser", getString("logics.forms.property.order.user"), IntegerClass.instance, propertyDraw, customUser);
        columnOrderPropertyDraw = addDProp(baseGroup, "columnOrderPropertyDraw", getString("logics.forms.property.order"), IntegerClass.instance, propertyDraw);
        columnOrderOverridePropertyDrawCustomUser = addSUProp(baseGroup, "columnOrderOverridePropertyDrawCustomUser", getString("logics.forms.property.order"), Union.OVERRIDE, addJProp(and1, columnOrderPropertyDraw, 1, is(customUser), 2), columnOrderPropertyDrawCustomUser);

        columnSortPropertyDrawCustomUser = addDProp(baseGroup, "columnSortPropertyDrawCustomUser", getString("logics.forms.property.sort.user"), IntegerClass.instance, propertyDraw, customUser);
        columnSortPropertyDraw = addDProp(baseGroup, "columnSortPropertyDraw", getString("logics.forms.property.sort"), IntegerClass.instance, propertyDraw);
        columnSortOverridePropertyDrawCustomUser = addSUProp(baseGroup, "columnSortOverridePropertyDrawCustomUser", getString("logics.forms.property.sort"), Union.OVERRIDE, addJProp(and1, columnSortPropertyDraw, 1, is(customUser), 2), columnSortPropertyDrawCustomUser);
        columnAscendingSortPropertyDrawCustomUser = addDProp(baseGroup, "columnAscendingSortPropertyDrawCustomUser", getString("logics.forms.property.ascending.sort.user"), LogicalClass.instance, propertyDraw, customUser);
        columnAscendingSortPropertyDraw = addDProp(baseGroup, "columnAscendingSortPropertyDraw", getString("logics.forms.property.ascending.sort"), LogicalClass.instance, propertyDraw);
        columnAscendingSortOverridePropertyDrawCustomUser = addSUProp(baseGroup, "columnAscendingSortOverridePropertyDrawCustomUser", getString("logics.forms.property.ascending.sort"), Union.OVERRIDE, addJProp(and1, columnAscendingSortPropertyDraw, 1, is(customUser), 2), columnAscendingSortPropertyDrawCustomUser);

        hasUserPreferencesGroupObjectCustomUser = addDProp(baseGroup, "hasUserPreferencesGroupObjectCustomUser", getString("logics.group.object.has.user.preferences.user"), LogicalClass.instance, groupObject, customUser);
        hasUserPreferencesGroupObject = addDProp(baseGroup, "hasUserPreferencesGroupObject", getString("logics.group.object.has.user.preferences"), LogicalClass.instance, groupObject);
        hasUserPreferencesOverrideGroupObjectCustomUser = addSUProp(baseGroup, "hasUserPreferencesOverrideGroupObjectCustomUser", getString("logics.group.object.has.user.preferences"), Union.OVERRIDE, addJProp(and1, hasUserPreferencesGroupObject, 1, is(customUser), 2), hasUserPreferencesGroupObjectCustomUser);

        groupObjectSID = addDProp(baseGroup, "groupObjectSID", getString("logics.group.object.sid"), propertySIDValueClass, groupObject);
        navigatorElementGroupObject = addDProp(baseGroup, "navigatorElementGroupObject", getString("logics.navigator.element"), navigatorElement, groupObject);
        sidNavigatorElementGroupObject = addJProp(baseGroup, "sidNavigatorElementGroupObject", navigatorElementSID, navigatorElementGroupObject, 1);
        SIDNavigatorElementSIDGroupObjectToGroupObject = addAGProp(baseGroup, "SIDToGroupObject", getString("logics.group.object"), groupObjectSID, sidNavigatorElementGroupObject);

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

        isEventNotification = addDProp(baseGroup, "isDerivedChangeNotification", getString("logics.notification.for.any.change"), LogicalClass.instance, notification);
        emailFromNotification = addDProp(baseGroup, "emailFromNotification", getString("logics.notification.sender.address"), StringClass.get(50), notification);
        emailToNotification = addDProp(baseGroup, "emailToNotification", getString("logics.notification.recipient.address"), StringClass.get(50), notification);
        emailToCCNotification = addDProp(baseGroup, "emailToCCNotification", getString("logics.notification.copy"), StringClass.get(50), notification);
        emailToBCNotification = addDProp(baseGroup, "emailToBCNotification", getString("logics.notification.blind.copy"), StringClass.get(50), notification);
        textNotification = addDProp(baseGroup, "textNotification", getString("logics.notification.text"), TextClass.instance, notification);
        subjectNotification = addDProp(baseGroup, "subjectNotification", getString("logics.notification.topic"), StringClass.get(100), notification);
        inNotificationProperty = addDProp(baseGroup, "inNotificationProperty", getString("logics.notification.enable"), LogicalClass.instance, notification, baseLM.property);

        nameScheduledTask = addDProp(baseGroup, "nameScheduledTask", getString("logics.scheduled.task.name"), StringClass.get(100), scheduledTask);
        runAtStartScheduledTask = addDProp(baseGroup, "runAtStartScheduledTask", getString("logics.scheduled.task.run.at.start"), LogicalClass.instance, scheduledTask);
        startDateScheduledTask = addDProp(baseGroup, "startDateScheduledTask", getString("logics.scheduled.task.start.date"), DateTimeClass.instance, scheduledTask);
        periodScheduledTask = addDProp(baseGroup, "periodScheduledTask", getString("logics.scheduled.task.period"), IntegerClass.instance, scheduledTask);
        activeScheduledTask = addDProp(baseGroup, "activeScheduledTask", getString("logics.scheduled.task.active"), LogicalClass.instance, scheduledTask);
        inScheduledTaskProperty = addDProp(baseGroup, "inScheduledTaskProperty", getString("logics.scheduled.task.enable"), LogicalClass.instance, scheduledTask, baseLM.property);
        activeScheduledTaskProperty = addDProp(baseGroup, "activeScheduledTaskProperty", getString("logics.scheduled.task.active"), LogicalClass.instance, scheduledTask, baseLM.property);
        orderScheduledTaskProperty = addDProp(baseGroup, "orderScheduledTaskProperty", getString("logics.scheduled.task.order"), IntegerClass.instance, scheduledTask, baseLM.property);

        resultScheduledTaskLog = addDProp(baseGroup, "resultScheduledTaskLog", getString("logics.scheduled.task.result"), StringClass.get(200), scheduledTaskLog);
        propertyScheduledTaskLog = addDProp(baseGroup, "propertyScheduledTaskLog", getString("logics.scheduled.task.property"), StringClass.get(200), scheduledTaskLog);
        dateStartScheduledTaskLog = addDProp(baseGroup, "dateStartScheduledTaskLog", getString("logics.scheduled.task.date.start"), DateTimeClass.instance, scheduledTaskLog);
        dateFinishScheduledTaskLog = addDProp(baseGroup, "dateFinishScheduledTaskLog", getString("logics.scheduled.task.date.finish"), DateTimeClass.instance, scheduledTaskLog);
        scheduledTaskScheduledTaskLog = addDProp(baseGroup, "scheduledTaskScheduledTaskLog", getString("logics.scheduled.task"), scheduledTask, scheduledTaskLog);
        currentScheduledTaskLogScheduledTask = addDProp(baseGroup, "currentScheduledTaskLogScheduledTask", getString("logics.scheduled.task.log.current"), IntegerClass.instance, scheduledTask);
        scheduledTaskLogScheduledClientTaskLog = addDProp(baseGroup, "scheduledTaskLogScheduledClientTaskLog", getString("logics.scheduled.task.log"), scheduledTaskLog, scheduledClientTaskLog);
        messageScheduledClientTaskLog = addDProp(baseGroup, "messageScheduledClientTaskLog", getString("logics.scheduled.task.log.message"), StringClass.get(200), scheduledClientTaskLog);
        
        permitViewUserRoleProperty = addDProp(baseGroup, "permitViewUserRoleProperty", getString("logics.policy.permit.property.view"), LogicalClass.instance, userRole, property);
        permitViewUserProperty = addJProp(baseGroup, "permitViewUserProperty", getString("logics.policy.permit.property.view"), permitViewUserRoleProperty, userMainRole, 1, 2);
        forbidViewUserRoleProperty = addDProp(baseGroup, "forbidViewUserRoleProperty", getString("logics.policy.forbid.property.view"), LogicalClass.instance, userRole, property);
        forbidViewUserProperty = addJProp(baseGroup, "forbidViewUserProperty", getString("logics.policy.forbid.property.view"), forbidViewUserRoleProperty, userMainRole, 1, 2);
        permitChangeUserRoleProperty = addDProp(baseGroup, "permitChangeUserRoleProperty", getString("logics.policy.permit.property.change"), LogicalClass.instance, userRole, property);
        permitChangeUserProperty = addJProp(baseGroup, "permitChangeUserProperty", getString("logics.policy.permit.property.change"), permitChangeUserRoleProperty, userMainRole, 1, 2);
        forbidChangeUserRoleProperty = addDProp(baseGroup, "forbidChangeUserRoleProperty", getString("logics.policy.forbid.property.change"), LogicalClass.instance, userRole, property);
        forbidChangeUserProperty = addJProp(baseGroup, "forbidChangeUserProperty", getString("logics.policy.forbid.property.change"), forbidChangeUserRoleProperty, userMainRole, 1, 2);
        notNullPermissionUserProperty = addSUProp("notNullPermissionUserProperty", Union.OVERRIDE, permitViewUserProperty, forbidViewUserProperty, permitChangeUserProperty, forbidChangeUserProperty);
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

        sidTable = addDProp(recognizeGroup, "sidTable", getString("logics.tables.name"), StringClass.get(100), table);
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
        sparseColumnsTable = addDProp(baseGroup, "sparseColumnsTable", getString("logics.tables.sparse.columns"), IntegerClass.instance, table);
        quantityTableKey = addDProp(baseGroup, "quantityTableKey", getString("logics.tables.key.variety.quantity"), IntegerClass.instance, tableKey);
        quantityTableColumn = addDProp(baseGroup, "quantityTableColumn", getString("logics.tables.column.variety.quantity"), IntegerClass.instance, tableColumn);
        notNullQuantityTableColumn = addDProp(baseGroup, "notNullQuantityTableColumn", getString("logics.tables.column.notnull.quantity"), IntegerClass.instance, tableColumn);
        perCentNotNullTableColumn = addDProp(baseGroup, "perCentNotNullTableColumn", getString("logics.tables.column.notnull.per.cent"), NumericClass.get(6, 2), tableColumn);
        recalculateAggregationTableColumn = addAProp(actionGroup, new RecalculateTableColumnActionProperty(getString("logics.recalculate.aggregations"), tableColumn));

        sidDropColumn = addDProp(baseGroup, "sidDropColumn", getString("logics.tables.column.name"), StringClass.get(100), dropColumn);
        sidToDropColumn = addAGProp("sidToDropColumn", getString("logics.tables.deleted.column"), sidDropColumn);
        sidTableDropColumn = addDProp(baseGroup, "sidTableDropColumn", getString("logics.tables.name"), StringClass.get(100), dropColumn);
        timeDropColumn = addDProp(baseGroup, "timeDropColumn", getString("logics.tables.deleted.column.time"), DateTimeClass.instance, dropColumn);
        revisionDropColumn = addDProp(baseGroup, "revisionDropColumn", getString("logics.launch.revision"), StringClass.get(10), dropColumn);

        dropDropColumn = addAProp(baseGroup, new DropColumnActionProperty("dropDropColumn", getString("logics.tables.deleted.column.drop"), dropColumn));
        dropDropColumn.setEventAction(this, IncrementType.DROP, false, is(dropColumn), 1); // event, который при удалении колонки из системы удаляет ее из базы

        // заполним сессии
        LCP sessionUser = addDProp("sessionUser", getString("logics.session.user"), user, session);
        sessionUser.setEventChangeNew(currentUser, is(session), 1);
        addJProp(baseGroup, getString("logics.session.user"), name, sessionUser, 1);
        LCP sessionDate = addDProp(baseGroup, "sessionDate", getString("logics.session.date"), DateTimeClass.instance, session);
        sessionDate.setEventChangeNew(currentDateTime, is(session), 1);

        objectByName = addMGProp(idGroup, "objectByName", getString("logics.object.name"), object(baseClass.named), name, 1);
        seekObjectName = addJoinAProp(getString("logics.object.search"), addSAProp(null), objectByName, 1);

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

        defaultBackgroundColor = addDProp("defaultBackgroundColor", getString("logics.default.background.color"), ColorClass.instance);
        defaultOverrideBackgroundColor = addSUProp("defaultOverrideBackgroundColor", true, getString("logics.default.background.color"), Union.OVERRIDE, yellowColor, defaultBackgroundColor);
        defaultForegroundColor = addDProp("defaultForegroundColor", getString("logics.default.foreground.color"), ColorClass.instance);
        defaultOverrideForegroundColor = addSUProp("defaultOverrideForegroundColor", true, getString("logics.default.foreground.color"), Union.OVERRIDE, redColor, defaultForegroundColor);

        dataName.setEventChange(addJProp(string2, addJProp(name.getOld(), objectClass, 1), 1, castString, 1), 1, is(baseClass.named), 1);
        date.setEventChange(currentDate, is(transaction), 1);

        insensitiveDictionary = addDProp(recognizeGroup, "insensitiveDictionary", getString("logics.dictionary.insensitive"), LogicalClass.instance, dictionary);
        entryDictionary = addDProp("entryDictionary", getString("logics.dictionary"), dictionary, dictionaryEntry);
        termDictionary = addDProp(recognizeGroup, "termDictionary", getString("logics.dictionary.termin"), StringClass.get(50), dictionaryEntry);
        insensitiveTermDictionary = addJProp(baseGroup, "insensitiveTermDictionary", upper, termDictionary, 1);
        translationDictionary = addDProp(baseGroup, "translationDictionary", getString("logics.dictionary.translation"), StringClass.get(50), dictionaryEntry);
        translationDictionaryTerm = addCGProp(null, "translationDictionaryTerm", getString("logics.dictionary.translation"), translationDictionary, termDictionary, entryDictionary, 1, termDictionary, 1);
        nameEntryDictionary = addJProp(baseGroup, "nameEntryDictionary", getString("logics.dictionary"), name, entryDictionary, 1);

        insensitiveTranslationDictionaryTerm = addMGProp((AbstractGroup)null, "insensitiveTranslationDictionaryTerm", getString("logics.dictionary.translation.insensitive"), translationDictionary, entryDictionary, 1, insensitiveTermDictionary, 1);
        
        currencyTypeExchange = addDProp(idGroup, "currencyTypeExchange", "Валюта типа обмена (ИД)", currency, typeExchange);
        nameCurrencyTypeExchange = addJProp(baseGroup, "nameCurrencyTypeExchange", "Валюта типа обмена (наим.)", name, currencyTypeExchange, 1);
        rateExchange = addDProp(baseGroup, "rateExchange", "Курс обмена", NumericClass.get(15, 8), typeExchange, baseLM.currency, DateClass.instance);

         //lessCmpDate = addJProp(and(false, true, false), object(DateClass.instance), 3, rateExchange, 1, 2, 3, greater2, 3, 4, is(DateClass.instance), 4);
        lessCmpDate = addJProp(and(false, true, false), object(DateClass.instance), 3, rateExchange, 1, 2, 3, addJProp(greater2, 3, date, 4), 1, 2, 3, 4, is(transaction), 4);
        nearestPredDate = addMGProp((AbstractGroup) null, "nearestPredDate", "Ближайшая меньшая дата", lessCmpDate, 1, 2, 4);
        nearestRateExchange = addJProp("nearestRateExchange", "Ближайший курс обмена", rateExchange, 1, 2, nearestPredDate, 1, 2, 3);

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

        applicationElement = addNavigatorElement(adminElement, "applicationElement", getString("logics.administration.application"));
        addFormEntity(new OptionsFormEntity(applicationElement, "options"));
        addFormEntity(new IntegrationDataFormEntity(applicationElement, "integrationData"));
        addFormEntity(new MigrationDataFormEntity(applicationElement, "migrationData"));

        catalogElement = addNavigatorElement(adminElement, "catalogElement", getString("logics.administration.catalogs"));

        accessElement = addNavigatorElement(adminElement, "accessElement", getString("logics.administration.access"));

        UserEditFormEntity userEditForm = addFormEntity(new UserEditFormEntity(null, "userEditForm"));
        customUser.setEditForm(userEditForm, userEditForm.objUser);

        addFormEntity(new UserPolicyFormEntity(accessElement, "userPolicyForm"));
        addFormEntity(new SecurityPolicyFormEntity(accessElement, "securityPolicyForm"));

        configElement = addNavigatorElement(adminElement, "configElement", getString("logics.administration.config"));
        addFormEntity(new AdminFormEntity(configElement, "adminForm"));
        addFormEntity(new PropertiesFormEntity(configElement, "propertiesForm"));
        addFormEntity(new PhysicalModelFormEntity(configElement, "physicalModelForm"));
        addFormEntity(new FormsFormEntity(configElement, "formsForm"));
        addFormEntity(new NotificationFormEntity(configElement, "notification"));
        addFormEntity(new ScheduledTaskFormEntity(configElement, "scheduledTask"));

        eventsElement = addNavigatorElement(adminElement, "eventsElement", getString("logics.administration.events"));
        addFormEntity(new LaunchesFormEntity(eventsElement, "launchesForm"));
        addFormEntity(new ConnectionsFormEntity(eventsElement, "connectionsForm"));
        addFormEntity(new ExceptionsFormEntity(eventsElement, "exceptionsForm"));

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

    private class CheckAggregationsActionProperty extends AdminActionProperty {
        private CheckAggregationsActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
        }

        @Override
        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            SQLSession sqlSession = context.getSession().sql;

            sqlSession.startTransaction();
            String message = BL.checkAggregations(sqlSession);
            sqlSession.commitTransaction();

            context.delayUserInterfaction(new MessageClientAction(getString("logics.check.aggregation.was.completed") + '\n' + '\n' + message, getString("logics.checking.aggregations"), true));
        }
    }

    private class RecalculateActionProperty extends AdminActionProperty {
        private RecalculateActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
        }

        @Override
        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            SQLSession sqlSession = context.getSession().sql;

            sqlSession.startTransaction();
            BL.recalculateAggregations(sqlSession, BL.getAggregateStoredProperties());
            sqlSession.commitTransaction();

            context.delayUserInterfaction(new MessageClientAction(getString("logics.recalculation.was.completed"), getString("logics.recalculation.aggregations")));
        }
    }

    private class RecalculateTableColumnActionProperty extends AdminActionProperty {

        private final ClassPropertyInterface tableColumnInterface;

        private RecalculateTableColumnActionProperty(String caption, ValueClass tableColumn) {
            super(genSID(), caption, new ValueClass[]{tableColumn});
            Iterator<ClassPropertyInterface> i = interfaces.iterator();
            tableColumnInterface = i.next();
        }

        @Override
        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            SQLSession sqlSession = context.getSession().sql;

            DataObject tableColumnObject = context.getKeyValue(tableColumnInterface);
            String propertySID = (String) sidTableColumn.read(context, tableColumnObject);

            sqlSession.startTransaction();
            BL.recalculateAggregationTableColumn(sqlSession, propertySID.trim());
            sqlSession.commitTransaction();

            context.delayUserInterfaction(new MessageClientAction(getString("logics.recalculation.was.completed"), getString("logics.recalculation.aggregations")));
        }
    }

    private class PackActionProperty extends AdminActionProperty {
        private PackActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
        }

        @Override
        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            SQLSession sqlSession = context.getSession().sql;

            sqlSession.startTransaction();
            BL.packTables(sqlSession, tableFactory.getImplementTables());
            sqlSession.commitTransaction();

            context.delayUserInterfaction(new MessageClientAction(getString("logics.tables.packing.completed"), getString("logics.tables.packing")));
        }
    }

    private class RecalculateFollowsActionProperty extends AdminActionProperty {
        private RecalculateFollowsActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
        }

        @Override
        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            DataSession session = BL.createSession();
            BL.recalculateFollows(session);
            session.apply(BL);
            session.close();

            context.delayUserInterfaction(new MessageClientAction(getString("logics.recalculation.was.completed"), getString("logics.recalculation.follows")));
        }
    }

    private class AnalyzeDBActionProperty extends AdminActionProperty {
        private AnalyzeDBActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
        }

        @Override
        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            DataSession session = BL.createSession();
            BL.analyzeDB(session.sql);
            session.apply(BL);
            session.close();

            context.delayUserInterfaction(new MessageClientAction(getString("logics.vacuum.analyze.was.completed"), getString("logics.vacuum.analyze")));
        }
    }

    private class ServiceDBActionProperty extends AdminActionProperty {
        private ServiceDBActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
        }

        @Override
        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            SQLSession sqlSession = context.getSession().sql;

            sqlSession.startTransaction();
            BL.recalculateAggregations(sqlSession, BL.getAggregateStoredProperties());
            sqlSession.commitTransaction();

            BL.recalculateFollows(context.getSession());

            sqlSession.startTransaction();
            BL.packTables(sqlSession, tableFactory.getImplementTables());
            sqlSession.commitTransaction();

            BL.analyzeDB(sqlSession);

            BL.recalculateStats(context.getSession());
            context.getSession().apply(BL);

            context.delayUserInterfaction(new MessageClientAction(getString("logics.service.db.completed"), getString("logics.service.db")));
        }
    }
    
    private class RecalculateStatsActionProperty extends AdminActionProperty {
        private RecalculateStatsActionProperty(String sID, String caption) {
            super(sID, caption, new ValueClass[]{});
        }

        @Override
        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            BL.recalculateStats(context.getSession());
        }
    }

    public class DropColumnActionProperty extends AdminActionProperty {
        private DropColumnActionProperty(String sID, String caption, ValueClass dropColumn) {
            super(sID, caption, new ValueClass[]{dropColumn});
        }

        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            DataObject dropColumnObject = context.getSingleKeyValue();
            String columnName = (String) sidDropColumn.getOld().read(context, dropColumnObject);
            String tableName = (String) sidTableDropColumn.getOld().read(context, dropColumnObject);
            BL.dropColumn(tableName, columnName);
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

    private class UserPolicyFormEntity extends FormEntity {
        protected UserPolicyFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.user.users"));

            ObjectEntity objUser = addSingleGroupObject(customUser, nameUserMainRole, name, userLogin, email, barcode);
            setEditType(objUser, PropertyEditType.READONLY);

            addFormActions(this, objUser);
        }
    }

    private class UserEditFormEntity extends FormEntity {

        private final ObjectEntity objUser;
        private final ObjectEntity objRole;

        protected UserEditFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.user.user"));

            objUser = addSingleGroupObject(customUser, userFirstName, userLastName, userLogin, userPassword, email, nameUserMainRole, barcode);
            objUser.groupTo.setSingleClassView(ClassViewType.PANEL);

            objRole = addSingleGroupObject(userRole, name, userRoleSID);
            setEditType(objRole, PropertyEditType.READONLY);
            
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

            setEditType(navigatorElementSID, PropertyEditType.READONLY);
            setEditType(navigatorElementCaption, PropertyEditType.READONLY);

            PropertyDrawEntity balanceDraw = getPropertyDraw(userRolePolicyOrder, objPolicy.groupTo);
            PropertyDrawEntity sidDraw = getPropertyDraw(userRoleSID, objUserRole.groupTo);
            balanceDraw.addColumnGroupObject(objUserRole.groupTo);
            balanceDraw.setPropertyCaption((CalcPropertyObjectEntity) sidDraw.propertyObject);

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
            container.type = ContainerType.TABBED_PANE;

            ContainerView defaultPolicyContainer = design.createContainer(getString("logics.policy.default"));
            ContainerView defaultFormsContainer = design.createContainer(getString("logics.forms"));
            defaultFormsContainer.type = ContainerType.TABBED_PANE;
            defaultFormsContainer.add(design.getTreeContainer(treeDefaultForm));
            defaultFormsContainer.add(design.getGroupObjectContainer(objDefaultForm.groupTo));
            ContainerView defaultPropertyContainer = design.createContainer(getString("logics.property.properties"));
            defaultPropertyContainer.type = ContainerType.TABBED_PANE;
            defaultPropertyContainer.add(design.getTreeContainer(treeDefaultProperty));
            defaultPropertyContainer.add(design.getGroupObjectContainer(objDefaultProperty.groupTo));
            defaultPolicyContainer.type = ContainerType.TABBED_PANE;
            defaultPolicyContainer.add(defaultFormsContainer);
            defaultPolicyContainer.add(defaultPropertyContainer);

            ContainerView rolesContainer = design.createContainer(getString("logics.policy.roles"));
            ContainerView rolePolicyContainer = design.createContainer();
            rolePolicyContainer.type = ContainerType.TABBED_PANE;
            ContainerView formsContainer = design.createContainer(getString("logics.forms"));
            formsContainer.type = ContainerType.TABBED_PANE;
            formsContainer.add(design.getTreeContainer(treeFormObject));
            formsContainer.add(design.getGroupObjectContainer(objForm.groupTo));
            rolePolicyContainer.add(formsContainer);
            ContainerView propertiesContainer = design.createContainer(getString("logics.property.properties"));
            propertiesContainer.type = ContainerType.TABBED_PANE;
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

//            setEditType(baseGroup, PropertyEditType.READONLY);

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
            setEditType(PropertyEditType.READONLY);
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
            setEditType(objDropColumn, PropertyEditType.READONLY);
            setEditType(dropDropColumn, PropertyEditType.EDITABLE);

            recalculateStats = addPropertyDraw(addAProp(new RecalculateStatsActionProperty("recalculateStats", getString("logics.tables.recalculate.stats"))));
            addPropertyDraw(recalculateAggregationTableColumn, objColumn);

            setEditType(propertyNameTableColumn, PropertyEditType.READONLY);

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
            container.type = ContainerType.TABBED_PANE;
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

            setEditType(PropertyEditType.READONLY);
            setEditType(userLoggableProperty, PropertyEditType.EDITABLE);
            setEditType(storedProperty, PropertyEditType.EDITABLE);
            setEditType(isSetNotNullProperty, PropertyEditType.EDITABLE);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            ContainerView container = design.createContainer();
            container.type = ContainerType.TABBED_PANE;

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

            addPropertyDraw(new LP[]{smtpHost, smtpPort, nameEncryptedConnectionType, fromAddress, emailAccount, emailPassword,
                    emailBlindCarbonCopy, disableEmail});

            objNotification = addSingleGroupObject(notification, getString("logics.notification"));
            objProperty = addSingleGroupObject(property, getString("logics.property.properties"));

            addPropertyDraw(inNotificationProperty, objNotification, objProperty);
            addPropertyDraw(objNotification, subjectNotification, textNotification, emailFromNotification, emailToNotification, emailToCCNotification, emailToBCNotification, isEventNotification);
            addObjectActions(this, objNotification);
            addPropertyDraw(objProperty, captionProperty, SIDProperty);
            setForceViewType(textNotification, ClassViewType.PANEL);
            setEditType(captionProperty, PropertyEditType.READONLY);
            setEditType(SIDProperty, PropertyEditType.READONLY);

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
            specContainer.type = ContainerType.TABBED_PANE;

            addDefaultOrder(getPropertyDraw(SIDProperty, objProperty), true);
            return design;
        }
    }

    public class ScheduledTaskFormEntity extends FormEntity {

        private ObjectEntity objScheduledTask;
        private ObjectEntity objProperty;
        private ObjectEntity objScheduledTaskLog;
        private ObjectEntity objScheduledClientTaskLog;

        public ScheduledTaskFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.scheduled.task.tasks"));

            objScheduledTask = addSingleGroupObject(scheduledTask, getString("logics.scheduled.task"));
            objProperty = addSingleGroupObject(property, getString("logics.property.properties"));
            objScheduledTaskLog = addSingleGroupObject(scheduledTaskLog, getString("logics.scheduled.task.log"));
            objScheduledClientTaskLog = addSingleGroupObject(scheduledClientTaskLog, getString("logics.scheduled.task.log.client"));

            addPropertyDraw(objScheduledTask, objProperty, inScheduledTaskProperty, activeScheduledTaskProperty, orderScheduledTaskProperty);
            addPropertyDraw(objScheduledTask, activeScheduledTask, nameScheduledTask, startDateScheduledTask, periodScheduledTask, runAtStartScheduledTask);
            addObjectActions(this, objScheduledTask);
            addPropertyDraw(objProperty, captionProperty, SIDProperty, classProperty, returnProperty);
            addPropertyDraw(objScheduledTaskLog, propertyScheduledTaskLog, resultScheduledTaskLog, dateStartScheduledTaskLog, dateFinishScheduledTaskLog);
            addPropertyDraw(objScheduledClientTaskLog, messageScheduledClientTaskLog);
            setEditType(captionProperty, PropertyEditType.READONLY);
            setEditType(SIDProperty, PropertyEditType.READONLY);
            setEditType(classProperty, PropertyEditType.READONLY);
            setEditType(returnProperty, PropertyEditType.READONLY);
            setEditType(objScheduledTaskLog, PropertyEditType.READONLY);
            setEditType(objScheduledClientTaskLog, PropertyEditType.READONLY);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(scheduledTaskScheduledTaskLog, objScheduledTaskLog), Compare.EQUALS, objScheduledTask));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(scheduledTaskLogScheduledClientTaskLog, objScheduledClientTaskLog), Compare.EQUALS, objScheduledTaskLog));
            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(
                    new RegularFilterEntity(genID(),
                            new NotNullFilterEntity(addPropertyObject(inScheduledTaskProperty, objScheduledTask, objProperty)),
                            getString("logics.only.checked"),
                            KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)
                    ), true);
            addRegularFilterGroup(filterGroup);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            ContainerView specContainer = design.createContainer();
            ContainerView bottomContainer = design.createContainer();
            bottomContainer.add(design.getGroupObjectContainer(objProperty.groupTo));

            ContainerView logContainer = design.createContainer("Лог");
            logContainer.add(design.getGroupObjectContainer(objScheduledTaskLog.groupTo));
            logContainer.add(design.getGroupObjectContainer(objScheduledClientTaskLog.groupTo));

            bottomContainer.add(logContainer);
            bottomContainer.type = ContainerType.TABBED_PANE;
            
            specContainer.add(design.getGroupObjectContainer(objScheduledTask.groupTo));
            specContainer.add(bottomContainer);
            specContainer.type = ContainerType.SPLIT_PANE_VERTICAL;

            design.getMainContainer().add(0, specContainer);
            return design;
        }
    }

    class FormsFormEntity extends FormEntity{

        ObjectEntity objTreeForm;
        TreeGroupEntity treeFormObject;
        ObjectEntity objUser;
        ObjectEntity objGroupObject;
        ObjectEntity objPropertyDraw;
        protected FormsFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.tables.forms"));

            objTreeForm = addSingleGroupObject(navigatorElement, true);
            objTreeForm.groupTo.setIsParents(addPropertyObject(parentNavigatorElement, objTreeForm));

            treeFormObject = addTreeGroupObject(objTreeForm.groupTo);
            addPropertyDraw(new LP[]{navigatorElementSID, navigatorElementCaption, parentNavigatorElement}, objTreeForm);
            objUser = addSingleGroupObject(customUser, getString("logics.user"), userFirstName, userLastName, userLogin);
            objGroupObject = addSingleGroupObject(groupObject, getString("logics.group.object"), groupObjectSID, hasUserPreferencesGroupObject);
            objPropertyDraw = addSingleGroupObject(propertyDraw, getString("logics.property.draw"), propertyDrawSID, captionPropertyDraw);

            addPropertyDraw(hasUserPreferencesGroupObjectCustomUser, objGroupObject, objUser);
            
            addPropertyDraw(nameShowPropertyDraw, objPropertyDraw);
            addPropertyDraw(nameShowPropertyDrawCustomUser, objPropertyDraw, objUser);
            addPropertyDraw(columnWidthPropertyDraw, objPropertyDraw);
            addPropertyDraw(columnWidthPropertyDrawCustomUser, objPropertyDraw, objUser);
            addPropertyDraw(columnOrderPropertyDraw, objPropertyDraw);
            addPropertyDraw(columnOrderPropertyDrawCustomUser, objPropertyDraw, objUser);
            addPropertyDraw(columnSortPropertyDraw, objPropertyDraw);
            addPropertyDraw(columnAscendingSortPropertyDraw, objPropertyDraw);
            addPropertyDraw(columnSortPropertyDrawCustomUser, objPropertyDraw, objUser);
            addPropertyDraw(columnAscendingSortPropertyDrawCustomUser, objPropertyDraw, objUser);

            objUser.groupTo.initClassView = ClassViewType.PANEL;

            addFixedFilter(new CompareFilterEntity(addPropertyObject(navigatorElementGroupObject, objGroupObject), Compare.EQUALS, objTreeForm));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(formPropertyDraw, objPropertyDraw), Compare.EQUALS, objTreeForm));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(currentUser), Compare.EQUALS, objUser));

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(groupObjectPropertyDraw, objPropertyDraw), Compare.EQUALS, objGroupObject),
                    getString("logics.group.object.only.current"),
                    KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
            addRegularFilterGroup(filterGroup);

            setEditType(PropertyEditType.READONLY);
            setEditType(nameShowPropertyDraw, PropertyEditType.EDITABLE);
            setEditType(nameShowPropertyDrawCustomUser, PropertyEditType.EDITABLE);
            setEditType(columnWidthPropertyDrawCustomUser, PropertyEditType.EDITABLE);
            setEditType(columnWidthPropertyDraw, PropertyEditType.EDITABLE);
            setEditType(columnOrderPropertyDrawCustomUser, PropertyEditType.EDITABLE);
            setEditType(columnOrderPropertyDraw, PropertyEditType.EDITABLE);
            setEditType(columnSortPropertyDraw, PropertyEditType.EDITABLE);
            setEditType(columnAscendingSortPropertyDraw, PropertyEditType.EDITABLE);
            setEditType(columnSortPropertyDrawCustomUser, PropertyEditType.EDITABLE);
            setEditType(columnAscendingSortPropertyDrawCustomUser, PropertyEditType.EDITABLE);
        }
    }


    class ExceptionsFormEntity extends FormEntity {
        ObjectEntity objExceptions;

        protected ExceptionsFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.tables.exceptions"));
            objExceptions = addSingleGroupObject(exception, getString("logics.tables.exceptions"), messageException, clientClientException, loginClientException, typeException, dateException);
            addPropertyDraw(erTraceException, objExceptions).forceViewType = ClassViewType.PANEL;
            setEditType(PropertyEditType.READONLY);
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

    private class AdminFormEntity extends FormEntity {
        private AdminFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.global.parameters"));

            addPropertyDraw(new LP[]{webHost, defaultBackgroundColor,
                    defaultForegroundColor, restartServerAction, cancelRestartServerAction, checkAggregationsAction, recalculateAction,
                    recalculateFollowsAction, packAction, analyzeDBAction, serviceDBAction, runGarbageCollector});
        }
    }

    private class RemindUserPassFormEntity extends FormEntity { // письмо пользователю о логине
        private ObjectEntity objUser;

        private RemindUserPassFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, getString("logics.user.password.remind"), true);

            objUser = addSingleGroupObject(1, "customUser", customUser, userLogin, userPassword, name);
            objUser.groupTo.initClassView = ClassViewType.PANEL;

            addInlineEAForm(emailUserPassUser, this, objUser, 1);

            setEditType(PropertyEditType.READONLY);
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
