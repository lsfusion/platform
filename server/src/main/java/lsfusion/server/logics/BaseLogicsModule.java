package lsfusion.server.logics;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.identity.DefaultIDGenerator;
import lsfusion.base.identity.IDGenerator;
import lsfusion.interop.form.ModalityWindowFormType;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.caches.IdentityStrongLazy;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.NFLazy;
import lsfusion.server.base.version.Version;
import lsfusion.server.data.expr.formula.*;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.lambda.SQLCallable;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.language.property.oraction.LAP;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.LocalNestedType;
import lsfusion.server.logics.action.session.changed.IncrementType;
import lsfusion.server.logics.classes.StaticClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.AnyValuePropertyHolder;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.logics.classes.data.integral.DoubleClass;
import lsfusion.server.logics.classes.data.integral.IntegerClass;
import lsfusion.server.logics.classes.data.time.IntervalClass;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.classes.user.ObjectValueClassSet;
import lsfusion.server.logics.classes.user.set.OrObjectClassSet;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.classes.user.set.ResolveOrObjectClassSet;
import lsfusion.server.logics.constraint.PropertyFormEntity;
import lsfusion.server.logics.event.PrevScope;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapChange;
import lsfusion.server.logics.form.interactive.action.change.ActionObjectSelector;
import lsfusion.server.logics.form.interactive.action.change.FormAddObjectAction;
import lsfusion.server.logics.form.interactive.action.edit.FormSessionScope;
import lsfusion.server.logics.form.interactive.action.input.RequestResult;
import lsfusion.server.logics.form.interactive.property.GroupObjectProp;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.navigator.NavigatorFolder;
import lsfusion.server.logics.navigator.window.AbstractWindow;
import lsfusion.server.logics.navigator.window.NavigatorWindow;
import lsfusion.server.logics.property.JoinProperty;
import lsfusion.server.logics.property.LazyProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.cases.CaseUnionProperty;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.logics.property.classes.data.FormulaJoinProperty;
import lsfusion.server.logics.property.classes.data.NotFormulaProperty;
import lsfusion.server.logics.property.classes.infer.AlgType;
import lsfusion.server.logics.property.classes.user.ClassDataProperty;
import lsfusion.server.logics.property.data.SessionDataProperty;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.implement.PropertyRevImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.logics.property.value.NullValueProperty;
import lsfusion.server.physics.admin.drilldown.action.LazyDrillDownAction;
import lsfusion.server.physics.admin.drilldown.form.DrillDownFormEntity;
import lsfusion.server.physics.admin.log.form.LogFormEntity;
import lsfusion.server.physics.admin.monitor.SystemEventsLogicsModule;
import lsfusion.server.physics.dev.debug.ActionDebugger;
import lsfusion.server.physics.dev.debug.action.WatchAction;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.id.name.AbstractPropertyNameParser;
import lsfusion.server.physics.dev.id.name.DBNamingPolicy;
import lsfusion.server.physics.dev.id.name.PropertyCanonicalNameParser;
import lsfusion.server.physics.dev.id.name.PropertyCanonicalNameUtils;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;
import lsfusion.server.physics.exec.db.table.ImplementTable;
import lsfusion.server.physics.exec.db.table.TableFactory;
import org.antlr.runtime.RecognitionException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static com.google.common.collect.Iterables.size;
import static lsfusion.server.physics.dev.id.name.PropertyCanonicalNameUtils.intervalPrefix;
import static lsfusion.server.physics.dev.id.name.PropertyCanonicalNameUtils.objValuePrefix;

public class BaseLogicsModule extends ScriptingLogicsModule {
    // classes
    public BaseClass baseClass;

    // groups
    public Group drillDownGroup; // для того чтобы в reflection'е можно было для всех drillDown одну политику безопасности проставлять
    public Group propertyPolicyGroup; // для того чтобы в reflection'е можно было для всех propertyPolicy одну политику безопасности проставлять

    public Group objectsGroup; // for usage in the interactive JSONs

    public Group rootGroup;
    public Group publicGroup;
    public Group baseGroup;
    public Group recognizeGroup;
    
    // properties
    public LP groeq2;
    public LP lsoeq2;
    public LP greater2, less2;
    public LP object1, and1, andNot1;
    public LP object;
    public LP equals2, diff2;
    public LP like2;
    public LP match2;
    public LP sum;
    public LP subtract;
    public LP multiply;
    public LP divide;
    public LP round;
    public LP roundScale;

//    public LP string2SP, istring2SP, string3SP, istring3SP, string4SP, istring4SP, string5SP, istring5SP;
//    public LP string2, istring2, string3, istring3;
//    public LP string5CM;
//    public LP ustring2CM, ustring2SP, ustring3SP, ustring4SP, ustring5SP, ustring2, ustring3, ustring4, ustring3CM, ustring4CM, ustring5CM;
//    public LP ustring2CR;

    public LP vtrue;
    public LP vzero;
    public LP vnull;

    public LP static30;

    public LP minus;

    private LA watch;

    public LA sleep;
    public LA applyOnlyWithoutRecalc;
    public LA applyAll;

    public LA delete;

    public LA<?> apply;
    public LP<?> canceled;

    public LP responseTcp;
    public LP timeoutTcp;

    public LP statusHttp;
    public LP statusHttpTo;
    public LP timeoutHttp;
    public LP insecureSSL;

    public LP<?> headers;
    public LP<?> cookies;
    public LP<?> headersTo;
    public LP<?> cookiesTo;
    public LP<?> query;
    public LP<?> params;
    public LP<?> actionPathInfo;
    public LP<?> contentType;
    public LP<?> body;
    public LP<?> appHost;
    public LP<?> appPort;
    public LP<?> exportName;
    public LP<?> scheme;
    public LP<?> method;
    public LP<?> webHost;
    public LP<?> webPort;
    public LP<?> contextPath;
    public LP<?> servletPath;
    public LP<?> pathInfo;
    public LP<?> url;

    public LP messageCaughtException;
    public LP javaStackTraceCaughtException;
    public LP lsfStackTraceCaughtException;

    public LA<?> empty;

    public LA flowBreak;
    public LA flowReturn;
    public LA<?> cancel;
    
    public LP<?> sessionOwners;

    public LP objectClass;
    public LP random;
    public LP objectClassName;
    public LP staticName;
    public LP staticCaption;

    public LP staticImage;
    public LP statCustomObjectClass;

    private LP addedObject;
    private LP beforeCanceled;
    private LP requestCanceled;
    private LP isHTMLSupported;
    private LP requestPushed;
    private LP isActiveForm;
    public LP formPageCount;
    public LP exportFile;
    public LP importFile;
    public LP readFile;

    public LP showResult;
    public LP eventSource;

    public LP extension;

    public LP inputFileName;

    public LP<?> imported;
    public LP importedString;

    public LP isServer;

    public LA openFile;
    public LA openRawFile;
    public LA openLink;
    public LA openRawLink;

    public LP defaultBackgroundColor;
    public LP defaultOverrideBackgroundColor;
    public LP defaultForegroundColor;
    public LP defaultOverrideForegroundColor;

    public LP reportRowHeight, reportCharWidth, reportToStretch;

    public LP networkPath;

    public LP fillingIDs;

    public LP logicsCaption;
    public LP topModule;

    public ConcreteCustomClass listViewType;
    public LP count;
    public LP isPivot;

    public Group privateGroup;

    public TableFactory tableFactory;

    // счетчик идентификаторов
    private static final IDGenerator idGenerator = new DefaultIDGenerator();

    // не надо делать логику паблик, чтобы не было возможности тянуть её прямо из BaseLogicsModule,
    // т.к. она должна быть доступна в точке, в которой вызывается baseLM.BL
    private final BusinessLogics BL;

    public BaseLogicsModule(BusinessLogics BL) throws IOException {
        super(null, BL, "/system/System.lsf");
        setBaseLogicsModule(this);
        this.BL = BL;

        // all these collections are used in the lazy / cached elements creation mechanisms
        namedProperties = NFFact.concurrentMap(namedProperties);
        namedActions = NFFact.concurrentMap(namedActions);
        propClasses = NFFact.concurrentMap(propClasses);
        unnamedForms = SetFact.concurrentSet(unnamedForms);
    }

    // need to implement next methods this way, because they are used in super.initMainLogic, and can not be initialized before super.initMainLogic

    @IdentityLazy
    public LA getFormEditReport() {
        try {
            return findAction("formEditReport[]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            Throwables.propagate(e);
        }
        return null;
    }

    @IdentityLazy
    public LA getFormDrop() {
        try {
            return findAction("formDrop[]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            Throwables.propagate(e);
        }
        return null;
    }

    @IdentityLazy
    public LA<?> getFormRefresh() {
        try {
            return findAction("formRefresh[]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            Throwables.propagate(e);
        }
        return null;
    }

    @IdentityLazy
    public LA getFormApply() {
        try {
            return findAction("formApply[]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            Throwables.propagate(e);
        }
        return null;
    }

    @IdentityLazy
    public LA getFormCancel() {
        try {
            return findAction("formCancel[]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            Throwables.propagate(e);
        }
        return null;
    }

    @IdentityLazy
    public LA getFormOk() {
        try {
            return findAction("formOk[]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            Throwables.propagate(e);
        }
        return null;
    }

    @IdentityLazy
    public LA getFormClose() {
        try {
            return findAction("formClose[]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            Throwables.propagate(e);
        }
        return null;
    }

    @IdentityLazy
    public LA getFormApplied() {
        try {
            return findAction("formApplied[]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            Throwables.propagate(e);
        }
        return null;
    }

    @IdentityLazy
    public LA getFormShare() {
        try {
            return findAction("formShare[]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            Throwables.propagate(e);
        }
        return null;
    }

    @IdentityLazy
    public LA<?> getPolyEdit() {
        try {
            return findAction("edit[Object]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    @IdentityLazy
    public LA<?> getFormEdit() {
        try {
            return findAction("formEdit[Object]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    @IdentityInstanceLazy
    public LA<?> getFormEditObject() {
        try {
            return findAction("formEditObject[Object]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    @IdentityLazy
    public LA<?> getPolyDelete() {
        try {
            return findAction("delete[Object]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    @IdentityLazy
    public LA<?> getFormDelete() {
        try {
            return findAction("formDelete[Object]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    @IdentityLazy
    public LP<?> getCanceled() {
        try {
            return findProperty("canceled[]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    @IdentityLazy
    public LP<?> getApplyMessage() {
        try {
            return findProperty("applyMessage[]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    @IdentityLazy
    public LP<?> getLogMessage() {
        try {
            return findProperty("logMessage[]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    @IdentityLazy
    public LA<?> getEmpty() {
        try {
            return findAction("empty[]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    @IdentityLazy
    public LA<?> getEmptyObject() {
        try {
            return findAction("empty[Object]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    public AbstractPropertyNameParser.ClassFinder getClassFinder() {
        return new PropertyCanonicalNameParser.CanonicalNameClassFinder(BL);    
    }
    
    @Override
    public void initMetaAndClasses() throws RecognitionException {
        baseClass = addBaseClass(elementCanonicalName("Object"), LocalizedString.create("{logics.object}"), elementCanonicalName("StaticObject"), LocalizedString.create("{classes.static.object.class}"));
        super.initMetaAndClasses();
        initNativeGroups();
    }

    @Override
    public void initTables(DBNamingPolicy namingPolicy) throws RecognitionException {
        tableFactory = new TableFactory();
        baseClass.initFullTables(tableFactory);
        
        super.initTables(namingPolicy);
    }

    @Override
    public void initMainLogic() throws RecognitionException {
        initNativeProperties();

        // логические св-ва
        and1 = addAFProp(false);
        andNot1 = addAFProp(true);

        // Сравнения
        equals2 = addCFProp(Compare.EQUALS);
        groeq2 = addCFProp(Compare.GREATER_EQUALS);
        greater2 = addCFProp(Compare.GREATER);
        lsoeq2 = addCFProp(Compare.LESS_EQUALS);
        less2 = addCFProp(Compare.LESS);
        diff2 = addCFProp(Compare.NOT_EQUALS);
        like2 = addCFProp(Compare.CONTAINS);
        match2 = addCFProp(Compare.MATCH);

        // Математические операции
        sum = addSumProp();
        multiply = addMultProp();
        subtract = addSubtractProp();
        divide = addDivideProp();
        round = addRoundProp(false);
        roundScale = addRoundProp(true);

        minus = addSFProp("(-(prm1))", SetFact.toOrderExclSet("prm1"));

        object = addAFProp();
        object.property.caption = LocalizedString.create("{logics.id}");

        // Константы
        vtrue = addCProp(LogicalClass.instance, true);
        vzero = addCProp(DoubleClass.instance, 0.0);
        vnull = addProperty(null, new LP<>(NullValueProperty.instance));

        static30 = addCProp(IntegerClass.instance, 30);

        if(ActionDebugger.getInstance().isEnabled()) {
            watch = addAction(null, new LA<>(WatchAction.instance));
            makeActionPublic(watch, "watch");
        }

        // need it before initMainLogic because it is used in constraints
        cancel = addCancelAProp(null, LocalizedString.NONAME, SetFact.EMPTY());

        // navigator elements (before parsing because root is used there)
        root = new NavigatorFolder("System.root");
        root.caption = () -> LocalizedString.NONAME;
        addNavigatorElement(root);

        super.initMainLogic();
        initGroups();
        addClassDataPropsToGroup();

        // через JOIN (не операторы)

        apply = findAction("apply[]");
//        cancel = findAction("cancel[]");

        responseTcp = findProperty("responseTcp[]");
        timeoutTcp = findProperty("timeoutTcp[]");

        statusHttp = findProperty("statusHttp[]");
        statusHttpTo = findProperty("statusHttpTo[]");
        timeoutHttp = findProperty("timeoutHttp[]");
        insecureSSL = findProperty("insecureSSL[]");

        headers = findProperty("headers[TEXT]");
        cookies = findProperty("cookies[TEXT]");
        headersTo = findProperty("headersTo[TEXT]");
        cookiesTo = findProperty("cookiesTo[TEXT]");
        query = findProperty("query[]");
        actionPathInfo = findProperty("actionPathInfo[]");
        params = findProperty("params[TEXT, INTEGER]");
        contentType = findProperty("contentType[]");
        body = findProperty("body[]");
        appHost = findProperty("appHost[]");
        appPort = findProperty("appPort[]");
        exportName = findProperty("exportName[]");
        scheme = findProperty("scheme[]");
        method = findProperty("method[]");
        webHost = findProperty("webHost[]");
        webPort = findProperty("webPort[]");
        contextPath = findProperty("contextPath[]");
        servletPath = findProperty("servletPath[]");
        pathInfo = findProperty("pathInfo[]");
        url = findProperty("url[]");

        messageCaughtException = findProperty("messageCaughtException[]");
        javaStackTraceCaughtException = findProperty("javaStackTraceCaughtException[]");
        lsfStackTraceCaughtException = findProperty("lsfStackTraceCaughtException[]");

        addedObject = findProperty("addedObject[]");
        beforeCanceled = findProperty("beforeCanceled[]");
//        requestCanceled = findProperty("requestCanceled[]");
        requestPushed = findProperty("requestPushed[]");
        isActiveForm = findProperty("isActiveForm[]");
        formPageCount = findProperty("formPageCount[]");
        exportFile = findProperty("exportFile[]");
        importFile = findProperty("importFile[]");
        readFile = findProperty("readFile[]");

        eventSource = findProperty("eventSource[]");
        showResult = findProperty("showResult[]");

        imported = findProperty("imported[INTEGER]");
        importedString = findProperty("importedString[STRING[10]]");

        isServer = findProperty("isServer[]");

        openFile = findAction("open[FILE]");
        openRawFile = findAction("open[RAWFILE]");
        openLink = findAction("open[LINK]");
        openRawLink = findAction("open[RAWLINK]");

        sleep = findAction("sleep[LONG]");
        applyOnlyWithoutRecalc = findAction("applyOnlyWithoutRecalc[]");
        applyAll = findAction("applyAll[]");

        staticName = findProperty("staticName[StaticObject]");
        staticCaption = findProperty("staticCaption[StaticObject]");
        staticImage = findProperty("image[StaticObject]");

        sessionOwners = findProperty("sessionOwners[]");
        ((SessionDataProperty)sessionOwners.property).noNestingInNestedSession = true;

        objectClassName = findProperty("objectClassName[Object]");
        statCustomObjectClass = findProperty("stat[CustomObjectClass]");

        extension = findProperty("extension[FILE]");

        // Настройка отчетов
        reportRowHeight = findProperty("reportRowHeight[]");
        reportCharWidth = findProperty("reportCharWidth[]");
        reportToStretch = findProperty("reportToStretch[]");

        networkPath = findProperty("networkPath[]");

        logicsCaption = findProperty("logicsCaption[]");
        topModule = findProperty("topModule[]");

        fillingIDs = findProperty("fillingIDs[]");

        // Настройка форм
        defaultBackgroundColor = findProperty("defaultBackgroundColor[]");
        defaultOverrideBackgroundColor = findProperty("defaultOverrideBackgroundColor[]");
        defaultForegroundColor = findProperty("defaultForegroundColor[]");
        defaultOverrideForegroundColor = findProperty("defaultOverrideForegroundColor[]");

        listViewType = (ConcreteCustomClass) findClass("ListViewType");
        count = findProperty("count[]");
        isPivot = findProperty("isPivot[ListViewType]");

        initNavigators();
    }

    private void initNativeProperties() {
        objectClass = addProperty(null, new LP<>(baseClass.getObjectClassProperty()));
        makePropertyPublic(objectClass, "objectClass", Collections.nCopies(1, null));

        random = addRMProp(LocalizedString.create("Random"));
        makePropertyPublic(random, "random", Collections.emptyList());

        SessionDataProperty requestCanceledProp = new SessionDataProperty(LocalizedString.NONAME, LogicalClass.instance);
        requestCanceledProp.nestedType = LocalNestedType.ALL;
        requestCanceled = addProperty(null, new LP<>(requestCanceledProp));
        makePropertyPublic(requestCanceled, "requestCanceled", Collections.emptyList());

        isHTMLSupported = addAUProp(null, false, false, false, false, CaseUnionProperty.Type.MULTI, LocalizedString.NONAME, LogicalClass.instance);
        makePropertyPublic(isHTMLSupported, "isHTMLSupported", Collections.emptyList());

        makePropertyPublic(FormEntity.isDocked, "isDocked");
        makePropertyPublic(FormEntity.isEditing, "isEditing");
        makePropertyPublic(FormEntity.isAdd, "isAdd");
        makePropertyPublic(FormEntity.isManageSession, "isManageSession");
        makePropertyPublic(FormEntity.isExternal, "isExternal");
        makePropertyPublic(FormEntity.showOk, "showOk");
        makePropertyPublic(FormEntity.showDrop, "showDrop");
        makePropertyPublic(DataSession.isDataChanged, "isDataChanged");
    }

    private void initNativeGroups() {
        rootGroup = addAbstractGroup("root", LocalizedString.create("root"), null);
//        rootGroup.changeChildrenToSimple(version);
        rootGroup.system = true;

        publicGroup = addAbstractGroup("public", LocalizedString.create("public"), rootGroup);
        publicGroup.system = true;

        privateGroup = addAbstractGroup("private", LocalizedString.create("private"), rootGroup);
        privateGroup.changeChildrenToSimple(getVersion());
        privateGroup.system = true;

        baseGroup = addAbstractGroup("base", LocalizedString.create("base"), publicGroup);
        baseGroup.system = true;

        recognizeGroup = addAbstractGroup("id", LocalizedString.create("id"), baseGroup);
        recognizeGroup.system = true;
    }

    private void addClassDataPropsToGroup() {
        for (List<LP<?>> propList : namedProperties.values()) {
            for (LP<?> lcp : propList) {
                if (lcp.property instanceof ClassDataProperty) {
                    addProperty(null, lcp);
                }
            }
        }
    }

    private void initGroups() throws RecognitionException {
        Version version = getVersion();

        drillDownGroup = findGroup("drillDown");
        drillDownGroup.changeChildrenToSimple(version);
        drillDownGroup.system = true;

        propertyPolicyGroup = findGroup("propertyPolicy");
        propertyPolicyGroup.changeChildrenToSimple(version);
        propertyPolicyGroup.system = true;

        objectsGroup = findGroup("objects");
    }

    @Override
    public void initIndexes(DBManager dbManager) throws RecognitionException {
        super.initIndexes(dbManager);
        dbManager.addIndex(staticCaption);
    }

    public <P extends PropertyInterface> PropertyFormEntity getLogForm(Property<P> property, Property<?> messageProperty, ImList<PropertyMapImplement<?, P>> properties) { // messageProperty - nullable
        PropertyFormEntity form = new PropertyFormEntity(this, property, messageProperty, properties);
        addAutoFormEntity(form);
        return form;
    }

    public static int generateStaticNewID() {
        return idGenerator.idShift();
    }

    public <I extends PropertyInterface> IntegrationForm<I> addFinalIntegrationForm(ImOrderSet<I> innerInterfaces, ImList<ValueClass> innerClasses, ImOrderSet<I> mapInterfaces, ImList<PropertyInterfaceImplement<I>> properties, ImList<IntegrationPropUsage> propUsages, ImOrderMap<String, Boolean> orders, PropertyInterfaceImplement<I> where) {
        try {
            IntegrationForm<I> integrationForm = addIntegrationForm(innerInterfaces, innerClasses, mapInterfaces, properties, propUsages, orders, where);
            addAutoFormEntityFinalized(integrationForm.form);
            return integrationForm;
        } catch (FormEntity.AlreadyDefined e) {
            throw Throwables.propagate(e);
        }
    }

    public LP addFinalJSONFormProp(LocalizedString caption, IntegrationForm integrationForm) {
        LP jsonFormProp = addJSONFormProp(caption, integrationForm, false);
//        jsonFormProp.property.finalizeInit();
        ((LazyProperty)jsonFormProp.property).finalizeLazyInit();
        return jsonFormProp;
    }

    // Окна
    public class Windows {
        public NavigatorWindow logo;
        public NavigatorWindow root;
        public NavigatorWindow system;
        public NavigatorWindow toolbar;
        public AbstractWindow forms;
        public AbstractWindow log;
    }

    public Windows baseWindows;

    // Навигаторы
    public NavigatorElement root;

    public NavigatorElement administration;

    public NavigatorElement application;
    public NavigatorElement logs;
    public NavigatorElement system;

    private void initNavigators() throws ScriptingErrorLog.SemanticErrorException {

        // Окна
        baseWindows = new Windows();

        baseWindows.logo = (NavigatorWindow) findWindow("logo");
        baseWindows.logo.autoSize = true;

        baseWindows.root = (NavigatorWindow) findWindow("root");
        baseWindows.root.autoSize = true;
        root.window = baseWindows.root;

        baseWindows.system = (NavigatorWindow) findWindow("system");
        baseWindows.system.autoSize = true;

        baseWindows.toolbar = (NavigatorWindow) findWindow("toolbar");

        baseWindows.forms = findWindow("forms");
        baseWindows.log = findWindow("log");

        administration = findNavigatorElement("administration");

        application = findNavigatorElement("application");

        system = findNavigatorElement("system");

        logs = findNavigatorElement("logs");
    }


    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////
    /// Properties
    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////

    // --------------------------------- Identity Strong Lazy ----------------------------------- //

    @Override
    @IdentityStrongLazy
    public LP is(ValueClass valueClass) {
        return addProperty(null, new LP<>(valueClass.getProperty()));
    }
    @Override
    @IdentityStrongLazy
    public LP object(ValueClass valueClass) {
        LP lcp = addJProp(and1, 1, is(valueClass), 1);
        ((JoinProperty)lcp.property).caption = LocalizedString.concatList(valueClass.getCaption(), " (", LocalizedString.create("{logics.id}"), ")");
        ((JoinProperty)lcp.property).objectPropertyClass = valueClass;
        return lcp;
    }

    @Override
    @IdentityStrongLazy
    public LP not() {
        return addProperty(null, new LP<>(NotFormulaProperty.instance));
    }

    @Override
    @IdentityStrongLazy
    public <P extends PropertyInterface> LP<?> addCProp(StaticClass valueClass, Object value) {
        return addProperty(null, new LP<>(PropertyFact.createStatic(value, valueClass).property));
    }

    @Override
    @IdentityStrongLazy
    public LP addCastProp(DataClass castClass) {
        return addProperty(null, new LP<>(new FormulaJoinProperty(LocalizedString.create("castTo" + castClass.toString()), 1, new CastFormulaImpl(castClass))));
    }

    @Override
    @IdentityStrongLazy
    protected LP addSumProp() {
        return addProperty(null, new LP<>(new FormulaJoinProperty(LocalizedString.create("sum"), 2, SumFormulaImpl.instance)));
    }

    @Override
    @IdentityStrongLazy
    protected LP addMultProp() {
        return addProperty(null, new LP<>(new FormulaJoinProperty(LocalizedString.create("multiply"), 2, MultiplyFormulaImpl.instance)));
    }

    @Override
    @IdentityStrongLazy
    protected LP addSubtractProp() {
        return addProperty(null, new LP<>(new FormulaJoinProperty(LocalizedString.create("subtract"), 2, SubtractFormulaImpl.instance)));
    }

    @Override
    @IdentityStrongLazy
    protected LP addDivideProp() {
        return addProperty(null, new LP<>(new FormulaJoinProperty(LocalizedString.create("divide"), 2, DivideFormulaImpl.instance)));
    }

    @Override
    @IdentityStrongLazy
    protected LP addRoundProp(boolean hasScale) {
        return addProperty(null, new LP<>(new FormulaJoinProperty(LocalizedString.create("round"), hasScale ? 2 : 1, RoundFormulaImpl.instance)));
    }

    @Override
    public SessionDataProperty getAddedObjectProperty() {
        return (SessionDataProperty) addedObject.property;
    }

    @IdentityStrongLazy
    @NFLazy
    public AnyValuePropertyHolder getRequestedValueProperty() {
        return getAnyValuePropertyHolder("requested");
    }

    @IdentityStrongLazy
    @NFLazy
    public AnyValuePropertyHolder getExportValueProperty() {
        return getAnyValuePropertyHolder("export");
    }

    public AnyValuePropertyHolder getAnyValuePropertyHolder(String namePrefix) {
        return new AnyValuePropertyHolder(
                getLCPByUniqueName(namePrefix + "Object"),
                getLCPByUniqueName(namePrefix + "String"),
                getLCPByUniqueName(namePrefix + "BpString"),
                getLCPByUniqueName(namePrefix + "Text"),
                getLCPByUniqueName(namePrefix + "RichText"),
                getLCPByUniqueName(namePrefix + "HTMLText"),
                getLCPByUniqueName(namePrefix + "Integer"),
                getLCPByUniqueName(namePrefix + "Long"),
                getLCPByUniqueName(namePrefix + "Double"),
                getLCPByUniqueName(namePrefix + "Numeric"),
                getLCPByUniqueName(namePrefix + "Year"),
                getLCPByUniqueName(namePrefix + "DateTime"),
                getLCPByUniqueName(namePrefix + "ZDateTime"),
                getLCPByUniqueName(namePrefix + "IntervalDate"),
                getLCPByUniqueName(namePrefix + "IntervalDateTime"),
                getLCPByUniqueName(namePrefix + "IntervalTime"),
                getLCPByUniqueName(namePrefix + "IntervalZDateTime"),
                getLCPByUniqueName(namePrefix + "Boolean"),
                getLCPByUniqueName(namePrefix + "TBoolean"),
                getLCPByUniqueName(namePrefix + "Date"),
                getLCPByUniqueName(namePrefix + "Time"),
                getLCPByUniqueName(namePrefix + "Color"),
                getLCPByUniqueName(namePrefix + "JSON"),
                getLCPByUniqueName(namePrefix + "JSONText"),
                getLCPByUniqueName(namePrefix + "WordFile"),
                getLCPByUniqueName(namePrefix + "ImageFile"),
                getLCPByUniqueName(namePrefix + "PdfFile"),
                getLCPByUniqueName(namePrefix + "DbfFile"),
                getLCPByUniqueName(namePrefix + "RawFile"),
                getLCPByUniqueName(namePrefix + "File"),
                getLCPByUniqueName(namePrefix + "ExcelFile"),
                getLCPByUniqueName(namePrefix + "TextFile"),
                getLCPByUniqueName(namePrefix + "CsvFile"),
                getLCPByUniqueName(namePrefix + "HtmlFile"),
                getLCPByUniqueName(namePrefix + "JsonFile"),
                getLCPByUniqueName(namePrefix + "XmlFile"),
                getLCPByUniqueName(namePrefix + "TableFile"),
                getLCPByUniqueName(namePrefix + "NamedFile"),
                getLCPByUniqueName(namePrefix + "WordLink"),
                getLCPByUniqueName(namePrefix + "ImageLink"),
                getLCPByUniqueName(namePrefix + "PdfLink"),
                getLCPByUniqueName(namePrefix + "DbfLink"),
                getLCPByUniqueName(namePrefix + "RawLink"),
                getLCPByUniqueName(namePrefix + "Link"),
                getLCPByUniqueName(namePrefix + "ExcelLink"),
                getLCPByUniqueName(namePrefix + "TextLink"),
                getLCPByUniqueName(namePrefix + "CsvLink"),
                getLCPByUniqueName(namePrefix + "HtmlLink"),
                getLCPByUniqueName(namePrefix + "JsonLink"),
                getLCPByUniqueName(namePrefix + "XmlLink"),
                getLCPByUniqueName(namePrefix + "TableLink"),
                getLCPByUniqueName(namePrefix + "TSVectorLink")
                );
    }

    protected LP<?> getLCPByUniqueName(String name) {
        Iterable<LP<?>> result = getNamedProperties(name);
        assert size(result) == 1;
        return result.iterator().next();
    }

    public LP getBeforeCanceledProperty() {
        return beforeCanceled;
    }

    public LP getRequestCanceledProperty() {
        return requestCanceled;
    }

    public LP getIsHTMLSupported() {
        return isHTMLSupported;
    }

    private LP getRequestPushedProperty() {
        return requestPushed;
    }

    @Override
    public LP getIsActiveFormProperty() {
        return isActiveForm;
    }

    @Override
    @IdentityStrongLazy
    public <P extends PropertyInterface> LP<P> addOldProp(LP<P> lp, PrevScope scope) {
        return addProperty(null, new LP<>(lp.property.getOld(scope), lp.listInterfaces));
    }

    @Override
    @IdentityStrongLazy
    public <P extends PropertyInterface> LP<P> addCHProp(LP<P> lp, IncrementType type, PrevScope scope) {
        addOldProp(lp, scope); // регистрируем старое значение в списке свойств
        return addProperty(null, new LP<>(lp.property.getChanged(type, scope), lp.listInterfaces));
    }

    @Override
    @IdentityStrongLazy
    public <P extends PropertyInterface> LP addClassProp(LP<P> lp) {
        return mapLProp(null, false, lp.property.getClassProperty().cloneProp(), lp);
    }

    private LA addObjInputAProp(DataClass dataClass, LP targetProp, ObjectEntity objectEntity) {
        return addInputAProp(dataClass, targetProp, false, SetFact.EMPTYORDER(), null, null, null, ListFact.EMPTY(), null, false);
    }

    @Override
    @IdentityStrongLazy
    public Pair<LP, ActionObjectSelector> getObjValueProp(FormEntity formEntity, ObjectEntity obj) {
        LP value;
        if(!obj.noClasses()) {
            value = object(obj.baseClass); // we want this property to have classes (i.e. getType to return correct type)
            if (formEntity.getCanonicalName() != null) {
                value = wrapObjProperty(value); // wrapping because all other form operators create new actions / properties
                String name = objValuePrefix + getFormPrefix(formEntity) + getObjectPrefix(obj); // issue #47
                makePropertyPublic(value, name, obj.baseClass.getResolveSet());
            }
        } else
            value = object;

        ActionObjectSelector onChange = null;
        if (!obj.noClasses() && obj.baseClass instanceof DataClass && obj.groupTo.viewType.isPanel()) {
            DataClass dataClass = (DataClass) obj.baseClass;

            LP targetProp = getRequestedValueProperty(dataClass);

            LA<?> input = addObjInputAProp(dataClass, targetProp, obj);

            ActionMapImplement<?, ClassPropertyInterface> request = PropertyFact.createRequestAction(SetFact.<ClassPropertyInterface>EMPTY(), input.getImplement(), obj.getSeekPanelAction(this, targetProp), null);

            PropertyFact.setResetAsync(request.action, new AsyncMapChange<>(null, obj, null, null));

            onChange = request.mapObjects(MapFact.EMPTYREV());
        }
        return new Pair<>(value, onChange);
    }

    @Override
    @IdentityStrongLazy
    public Pair<LP, ActionObjectSelector> getObjIntervalProp(FormEntity formEntity, ObjectEntity objectFrom, ObjectEntity objectTo, LP intervalProperty, LP fromIntervalProperty, LP toIntervalProperty) {
        LP value = intervalProperty;

        if (formEntity.getCanonicalName() != null) {
            value = wrapObjProperty(value); // wrapping because all other form operators create new actions / properties
            String name = intervalPrefix + getFormPrefix(formEntity) + getObjectPrefix(objectFrom) + getObjectPrefix(objectTo); // issue #47
            makePropertyPublic(value, name, objectFrom.baseClass.getResolveSet(), objectTo.baseClass.getResolveSet());
        }

        ActionObjectSelector onChange = null;
        if(objectFrom.groupTo.viewType.isPanel() && objectTo.groupTo.viewType.isPanel()) {
            DataClass dataClass = (IntervalClass) intervalProperty.getActionOrProperty().getValueClass(AlgType.defaultType);

            LP targetProp = getRequestedValueProperty(dataClass);

            LA<?> input = addObjInputAProp(dataClass, targetProp, objectFrom);

            ActionMapImplement<?, ClassPropertyInterface> request = PropertyFact.createRequestAction(SetFact.<ClassPropertyInterface>EMPTY(), input.getImplement(),
                    PropertyFact.createListAction(SetFact.<ClassPropertyInterface>EMPTY(),
                            objectFrom.getSeekPanelAction(this, addJProp(fromIntervalProperty, targetProp)),
                            objectTo.getSeekPanelAction(this, addJProp(toIntervalProperty, targetProp))), null);

            PropertyFact.setResetAsync(request.action, new AsyncMapChange<>(null, objectFrom, null, null));

            onChange = request.mapObjects(MapFact.EMPTYREV());
        }

        return new Pair<>(value, onChange);
    }

    @Override
    @IdentityStrongLazy
    public LA getAddObjectAction(FormEntity formEntity, ObjectEntity obj, CustomClass explicitClass) {
        CustomClass cls = explicitClass;
        if(explicitClass == null)
            cls = (CustomClass)obj.baseClass;
        LA result = addAProp(new FormAddObjectAction(cls, BL.LM.getAddedObjectProperty(), obj));
        
        setAddActionOptions(result, obj);
        
        if (formEntity.getCanonicalName() != null) {
            String name = "_NEW" + getFormPrefix(formEntity) + getObjectPrefix(obj) + (explicitClass != null ? getClassPrefix(cls) : "");
            makeActionPublic(result, name, cls.getResolveSet());
        }

        return result;
    }

    @IdentityStrongLazy
    public LP addGroupObjectProp(GroupObjectEntity groupObject, GroupObjectProp prop) {
        PropertyRevImplement<ClassPropertyInterface, ObjectEntity> filterProperty = groupObject.getProperty(prop);
        if(prop.equals(GroupObjectProp.FILTER))
            groupObject.isFilterExplicitlyUsed = true;
        if(prop.equals(GroupObjectProp.ORDER))
            groupObject.isOrderExplicitlyUsed = true;
        return addProperty(null, new LP<>(filterProperty.property, groupObject.getOrderObjects().mapOrder(filterProperty.mapping.reverse())));
    }

    @IdentityStrongLazy
    public LP addValueObjectProp(ObjectEntity object) {
        Property<?> valueProperty = object.getNFValueProperty();
        return addProperty(null, new LP<>(valueProperty));
    }

    @IdentityStrongLazy
    public LA getFormNavigatorAction(FormEntity form) {
        LA<?> result = addIFAProp(null, LocalizedString.NONAME, form, SetFact.EMPTYORDER(), FormSessionScope.OLDSESSION, false, ModalityWindowFormType.DOCKED, true);

        if(form.getCanonicalName() != null) {
            String name = "_NAVIGATORFORM" + getFormPrefix(form);
            makeActionPublic(result, name, new ArrayList<>());
        }
        
        return result;
    }

    @IdentityStrongLazy
    public LA getAddFormAction(CustomClass cls, FormEntity formEntity, ObjectEntity contextObject) {
        LA<?> result = addNewEditAction(cls, contextObject);

        if(formEntity.getCanonicalName() != null) {
            String name = "_ADDFORMNEWSESSION" + getFormPrefix(formEntity) + getObjectPrefix(contextObject) + getClassPrefix(cls); // issue #47
            makeActionPublic(result, name, new ArrayList<>());
        }

        return result;
    }

    @IdentityStrongLazy
    public LA getEditFormAction(CustomClass cls) {
        LA<?> result = addEditFormAction(cls);

        String name = "_EDITFORMNEWSESSION" + getClassPrefix(cls); // issue #47
        makeActionPublic(result, name, cls.getResolveSet());

        return result;
    }

    @IdentityStrongLazy
    public LA getDeleteAction(CustomClass cls) {
        LA res = addDeleteAction(cls);

        String name = "_DELETE";
        makeActionPublic(res, name, cls.getResolveSet());
        return res;
    }

    private static String getClassPrefix(CustomClass cls) {
        return "_" + cls.getSID();
    }

    private static String getFormPrefix(FormEntity formEntity) {
        return "_" + formEntity.getCanonicalName().replace('.', '_');
    }

    private static String getObjectPrefix(ObjectEntity objectEntity) {
        return "_" + objectEntity.getSID();
    }

    // REQUEST / INPUT BLOCK
    
    public void dropRequestCanceled(ExecutionEnvironment env) throws SQLException, SQLHandledException {
        getRequestCanceledProperty().change((Object)null, env);
    }
    
    public void dropBeforeCanceled(ExecutionEnvironment env) throws SQLException, SQLHandledException {
        getBeforeCanceledProperty().change((Object)null, env);
    }
    
    public <R> R pushRequest(ExecutionEnvironment env, SQLCallable<R> callable) throws SQLException, SQLHandledException {
        return pushPopRequestValue(true, env, callable);
    }

    public <R> R popRequest(ExecutionEnvironment env, SQLCallable<R> callable) throws SQLException, SQLHandledException {
        return pushPopRequestValue(false, env, callable);
    }

    public <R> R pushPopRequestValue(boolean push, ExecutionEnvironment env, SQLCallable<R> callable) throws SQLException, SQLHandledException {
        LP requestProperty = getRequestPushedProperty();
        Object prevValue = requestProperty.read(env);
        requestProperty.change(push ? true : null, env);
        try {
            return callable.call();
        } finally {
            requestProperty.change(prevValue, env);
        }
    }

    public boolean isBeforeCanceled(ExecutionEnvironment env) throws SQLException, SQLHandledException {
        return getBeforeCanceledProperty().read(env) != null;
    }

    public boolean isRequestPushed(ExecutionEnvironment env) throws SQLException, SQLHandledException {
        return getRequestPushedProperty().read(env) != null;
    }

    public boolean isRequestCanceled(ExecutionEnvironment env) throws SQLException, SQLHandledException {
        return getRequestCanceledProperty().read(env) != null;
    }

    @Deprecated
    public ObjectValue getRequestedValue(Type type, ExecutionEnvironment env, SQLCallable<ObjectValue> request) throws SQLException, SQLHandledException {
        LP<?> targetProp = getRequestedValueProperty().getLP(type);
        if(isRequestPushed(env))
            return targetProp.readClasses(env);

        ObjectValue result = request.call();
        writeRequested(RequestResult.get(result, type, targetProp), env);
        return result;
    }

    // should correspond getRequestChangeProps
    public void writeRequested(ImList<RequestResult> requestResults, ExecutionEnvironment env) throws SQLException, SQLHandledException {
        LP<?> requestCanceledProperty = getRequestCanceledProperty();
        if (requestResults == null) {
            requestCanceledProperty.change(true, env);
        } else {
            requestCanceledProperty.change((Object)null, env);
            for(RequestResult requestResult : requestResults)
                requestResult.targetProp.change(requestResult.chosenValue, env);
        }
    }

    // should correspond writeRequested
    public ImSet<Property> getRequestChangeProps(int count, Function<Integer, Type> type, Function<Integer, LP> targetProp) {
        return SetFact.toOrderExclSet(count, i -> {
            LP prop = targetProp.apply(i);
            assert prop != null;
            return prop.property;
        }).getSet().addExcl(getRequestCanceledProperty().property);
    }

    // ------------------- DRILLDOWN ----------------- //

    public void setupDrillDownProperty(Property property, boolean isLightStart) {
        if (property.supportsDrillDown()) {
            LA<?> drillDownFormProperty = addAProp(baseLM.drillDownGroup, new LazyDrillDownAction(LocalizedString.create("{logics.property.drilldown.action}"), property));
            if (property.isNamed()) {
                List<ResolveClassSet> signature = new ArrayList<>();
                String name = nameForDrillDownAction(property, signature);
                makeActionPublic(drillDownFormProperty, name, signature);
            }

            Action formProperty = drillDownFormProperty.action;
            property.setContextMenuAction(formProperty.getSID(), formProperty.caption);
            property.setEventAction(formProperty.getSID(), formProperty.getImplement(property.getReflectionOrderInterfaces()));
        }
    }

    private String nameForDrillDownAction(Property property, List<ResolveClassSet> signature) {
        assert property.isNamed();
        PropertyCanonicalNameParser parser = new PropertyCanonicalNameParser(property.getCanonicalName(), baseLM.getClassFinder());
        String name = PropertyCanonicalNameUtils.drillDownPrefix + parser.getNamespace() + "_" + property.getName();
        signature.addAll(parser.getSignature());
        return name;
    }

    public LA<?> addDDAProp(Property property) {
        List<ResolveClassSet> signature = new ArrayList<>();
        DrillDownFormEntity drillDownFormEntity = property.getDrillDownForm(this);
        LA result = addMFAProp(baseLM.drillDownGroup, LocalizedString.create("{logics.property.drilldown.action}"), drillDownFormEntity, drillDownFormEntity.paramObjects, property.drillDownInNewSession());
        if (property.isNamed()) {
            String name = nameForDrillDownAction(property, signature);
            makeActionPublic(result, name, signature);
        }
        return result;
    }

    protected void markFull(ImplementTable table, ImList<ValueClass> listClasses, DBNamingPolicy namingPolicy) {
        // создаем IS
        PropertyRevImplement<?, Integer> mapProperty = IsClassProperty.getProperty(listClasses.toIndexedMap()); // тут конечно стремновато из кэша брать, так как остальные гарантируют создание
        LP<?> lcp = addJProp(mapProperty.createLP(ListFact.consecutiveList(listClasses.size(), 0)), ListFact.consecutiveList(listClasses.size()).toArray(new Integer[listClasses.size()]));
//        addProperty(null, lcp);

        // делаем public, persistent
        makePropertyPublic(lcp, PropertyCanonicalNameUtils.fullPropPrefix + table.getName(), listClasses.mapListValues(ValueClass::getResolveSet).toJavaList());
        lcp.property.markStored(table);
        lcp.property.initStored(baseLM.tableFactory, namingPolicy); // we need to initialize because we use calcClassValueWhere for init stored properties

        // marking full
        table.setFullField(lcp.property.field);
    }

    public void markClassData(ImplementTable table, ImSet<ConcreteCustomClass> set, DBNamingPolicy namingPolicy) {
        ObjectValueClassSet classSet = OrObjectClassSet.fromSetConcreteChildren(set);

        CustomClass tableClass = (CustomClass) table.getMapFields().singleValue();
        // помечаем full tables
        assert tableClass.getUpSet().containsAll(classSet, false); // должны быть все классы по определению, исходя из логики раскладывания классов по таблицам
        boolean isFull = classSet.containsAll(tableClass.getUpSet(), false);
        if(isFull) // важно чтобы getInterfaceClasses дал тот же tableClass
            classSet = tableClass.getUpSet();

        ClassDataProperty dataProperty = new ClassDataProperty(LocalizedString.create(classSet.toString(), false), classSet);
        LP<ClassPropertyInterface> lp = new LP<>(dataProperty);
        addProperty(null, new LP<>(dataProperty));
        makePropertyPublic(lp, PropertyCanonicalNameUtils.classDataPropPrefix + table.getName(), Collections.singletonList(ResolveOrObjectClassSet.fromSetConcreteChildren(set)));
        // именно такая реализация, а не implementTable, из-за того что getInterfaceClasses может попасть не в "класс таблицы", а мимо и тогда нарушится assertion что должен попасть в ту же таблицу, это в принципе проблема getInterfaceClasses
        dataProperty.markStored(table);
        dataProperty.initStored(tableFactory, namingPolicy); // we need to initialize because we use calcClassValueWhere for init stored properties

        // помечаем dataProperty
        for(ConcreteCustomClass customClass : set)
            customClass.dataProperty = dataProperty;
        if(isFull) // неважно implicit или нет
            table.setFullField(dataProperty);
    }

    public LA addLFAProp(LP lp, SystemEventsLogicsModule systemEventsLM, DBNamingPolicy namingPolicy) {
        LP<?> logValueProperty = addLProp(systemEventsLM, lp, namingPolicy);
        LP<?> logDropProperty = addLDropProp(systemEventsLM, lp, namingPolicy);
        LP<?> logWhereProperty = addLWhereProp(logValueProperty, logDropProperty);

        LogFormEntity logFormEntity = new LogFormEntity(LocalizedString.create("{logics.property.log.form}"), lp, logValueProperty, logWhereProperty, this, systemEventsLM.session);
        addAutoFormEntity(logFormEntity);
        return addMFAProp(LocalizedString.create("{logics.property.log.action}"), logFormEntity, logFormEntity.params, true);
    }

    @Override
    protected <T extends LAP<?, ?>> List<T> createLAPList() {
        return Collections.synchronizedList(super.createLAPList());
    }
}
