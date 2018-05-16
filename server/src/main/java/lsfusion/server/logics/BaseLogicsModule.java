package lsfusion.server.logics;

import com.google.common.base.Throwables;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.identity.DefaultIDGenerator;
import lsfusion.base.identity.IDGenerator;
import lsfusion.interop.Compare;
import lsfusion.interop.WindowFormType;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.caches.IdentityStrongLazy;
import lsfusion.server.classes.*;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.data.SQLCallable;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.formula.CastFormulaImpl;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.entity.*;
import lsfusion.server.form.instance.FormSessionScope;
import lsfusion.server.form.navigator.NavigatorElement;
import lsfusion.server.form.window.AbstractWindow;
import lsfusion.server.form.window.NavigatorWindow;
import lsfusion.server.form.window.ToolBarNavigatorWindow;
import lsfusion.server.logics.debug.ActionPropertyDebugger;
import lsfusion.server.logics.debug.WatchActionProperty;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.mutables.NFFact;
import lsfusion.server.logics.mutables.NFLazy;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.FormAddObjectActionProperty;
import lsfusion.server.logics.property.derived.DerivedProperty;
import lsfusion.server.logics.property.group.AbstractGroup;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.logics.table.TableFactory;
import lsfusion.server.session.ExecutionEnvironment;
import org.antlr.runtime.RecognitionException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;

import static com.google.common.collect.Iterables.size;
import static lsfusion.server.logics.PropertyCanonicalNameUtils.objValuePrefix;

public class BaseLogicsModule<T extends BusinessLogics<T>> extends ScriptingLogicsModule {
    // classes
    // classes
    public BaseClass baseClass;

    // groups
    public AbstractGroup actionGroup;
    public AbstractGroup drillDownGroup;
    public AbstractGroup propertyPolicyGroup;

    // properties
    public LCP groeq2;
    public LCP lsoeq2;
    public LCP greater2, less2;
    public LCP object1, and1, andNot1;
    public LCP equals2, diff2;
    public LCP like2;
    public LCP sum;
    public LCP subtract;
    public LCP multiply;
    public LCP divide;

//    public LCP string2SP, istring2SP, string3SP, istring3SP, string4SP, istring4SP, string5SP, istring5SP;
//    public LCP string2, istring2, string3, istring3;
//    public LCP string5CM;
//    public LCP ustring2CM, ustring2SP, ustring3SP, ustring4SP, ustring5SP, ustring2, ustring3, ustring4, ustring3CM, ustring4CM, ustring5CM;
//    public LCP ustring2CR;

    public LCP vtrue;
    public LCP vzero;
    public LCP vnull;

    public LCP minus;

    private LAP watch;

    public LAP sleep;
    public LAP applyOnlyWithoutRecalc;
    public LAP applyAll;

    public LAP delete;

    public LAP<?> apply;
    public LCP<?> canceled;
    public LAP<?> onStarted;

    public LAP<?> empty;

    public LAP flowBreak;
    public LAP flowReturn;
    public LAP<?> cancel;
    
    public LCP<?> sessionOwners;

    public LCP objectClass;
    public LCP random;
    public LCP objectClassName;
    public LCP staticName;
    public LCP staticCaption;
    public LCP statCustomObjectClass; 

    private LCP addedObject;
    private LCP requestCanceled;
    private LCP requestPushed;
    private LCP isActiveForm;
    public LCP formPageCount;
    public LCP exportFile;
    public LCP exportFiles;
    public LCP ignorePrintType;

    public LCP imported;
    public LCP importedString;

    public LCP defaultBackgroundColor;
    public LCP defaultOverrideBackgroundColor;
    public LCP defaultForegroundColor;
    public LCP defaultOverrideForegroundColor;

    public LCP reportRowHeight, reportCharWidth, reportToStretch;

    public LCP networkPath;
    
    public AbstractGroup privateGroup;

    public TableFactory tableFactory;

    // счетчик идентификаторов
    private static final IDGenerator idGenerator = new DefaultIDGenerator();

    private PropertyDBNamePolicy propertyDBNamePolicy;
    
    // не надо делать логику паблик, чтобы не было возможности тянуть её прямо из BaseLogicsModule,
    // т.к. она должна быть доступна в точке, в которой вызывается baseLM.BL
    private final T BL;

    public BaseLogicsModule(T BL, PropertyDBNamePolicy propertyDBNamePolicy) throws IOException {
        super(BaseLogicsModule.class.getResourceAsStream("/lsfusion/system/System.lsf"), "/lsfusion/system/System.lsf", null, BL);
        setBaseLogicsModule(this);
        this.BL = BL;
        this.propertyDBNamePolicy = propertyDBNamePolicy;
        namedProperties = NFFact.simpleMap(namedProperties);
        namedActions = NFFact.simpleMap(namedActions);
    }

    @IdentityLazy
    public LAP getFormEditReport() {
        try {
            return findAction("formEditReport[]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            Throwables.propagate(e);
        }
        return null;
    }

    @IdentityLazy
    public LAP getFormDrop() {
        try {
            return findAction("formDrop[]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            Throwables.propagate(e);
        }
        return null;
    }

    @IdentityLazy
    public LAP getFormRefresh() {
        try {
            return findAction("formRefresh[]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            Throwables.propagate(e);
        }
        return null;
    }

    @IdentityLazy
    public LAP getFormApply() {
        try {
            return findAction("formApply[]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            Throwables.propagate(e);
        }
        return null;
    }

    @IdentityLazy
    public LAP getFormCancel() {
        try {
            return findAction("formCancel[]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            Throwables.propagate(e);
        }
        return null;
    }

    @IdentityLazy
    public LAP getFormOk() {
        try {
            return findAction("formOk[]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            Throwables.propagate(e);
        }
        return null;
    }

    @IdentityLazy
    public LAP getFormClose() {
        try {
            return findAction("formClose[]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            Throwables.propagate(e);
        }
        return null;
    }

    @IdentityLazy
    public LAP<?> getPolyEdit() {
        try {
            return findAction("edit[Object]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    @IdentityLazy
    public LAP<?> getFormEdit() {
        try {
            return findAction("formEdit[Object]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    @IdentityLazy
    public LAP<?> getPolyDelete() {
        try {
            return findAction("delete[Object]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    @IdentityLazy
    public LAP<?> getFormDelete() {
        try {
            return findAction("formDelete[Object]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    @IdentityLazy
    public LCP<?> getCanceled() {
        try {
            return findProperty("canceled[]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    @IdentityLazy
    public LAP<?> getEmpty() {
        try {
            return findAction("empty[]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    @IdentityLazy
    public LAP<?> getEmptyObject() {
        try {
            return findAction("empty[Object]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }


    public PropertyDBNamePolicy getDBNamePolicy() {
        return propertyDBNamePolicy;
    }
    
    public AbstractPropertyNameParser.ClassFinder getClassFinder() {
        return new PropertyCanonicalNameParser.CanonicalNameClassFinder(BL);    
    }
    
    @Override
    public void initClasses() throws RecognitionException {
        baseClass = addBaseClass(elementCanonicalName("Object"), LocalizedString.create("{logics.object}"));
        
        super.initClasses();
    }

    @Override
    public void initGroups() throws RecognitionException {
        super.initGroups();

        Version version = getVersion();

        rootGroup = findGroup("root");
        rootGroup.changeChildrenToSimple(version);
        rootGroup.createContainer = false;

        publicGroup = findGroup("public");
        publicGroup.createContainer = false;

        privateGroup = findGroup("private");
        privateGroup.changeChildrenToSimple(version); 
        privateGroup.createContainer = false;

        baseGroup = findGroup("base");
        baseGroup.createContainer = false;

        recognizeGroup = findGroup("recognize");
        recognizeGroup.createContainer = false;

        drillDownGroup = findGroup("drillDown");
        drillDownGroup.changeChildrenToSimple(version);
        drillDownGroup.createContainer = false;

        propertyPolicyGroup = findGroup("propertyPolicy");
        propertyPolicyGroup.changeChildrenToSimple(version);
        propertyPolicyGroup.createContainer = false;
    }

    @Override
    public void initTables() throws RecognitionException {
        tableFactory = new TableFactory(baseClass);
        baseClass.initFullTables(tableFactory);
        
        super.initTables();
    }

    @Override
    public void initProperties() throws RecognitionException {

        objectClass = addProperty(null, new LCP<>(baseClass.getObjectClassProperty()));
        makePropertyPublic(objectClass, "objectClass", Collections.<ResolveClassSet>nCopies(1, null));
        random = addRMProp(LocalizedString.create("Random"));
        makePropertyPublic(random, "random", Collections.<ResolveClassSet>emptyList());

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
        like2 = addCFProp(Compare.LIKE);

        // Математические операции
        sum = addSumProp();
        multiply = addMultProp();
        subtract = addSubtractProp();
        divide = addDivideProp();

        minus = addSFProp("(-(prm1))", 1);

        // Константы
        vtrue = addCProp(LogicalClass.instance, true);
        vzero = addCProp(DoubleClass.instance, 0.0);
        vnull = addProperty(null, new LCP<>(NullValueProperty.instance));

        if(ActionPropertyDebugger.getInstance().isEnabled()) {
            watch = addProperty(null, new LAP<>(WatchActionProperty.instance));
            makeActionPublic(watch, "watch");
        }

        super.initProperties();

        // через JOIN (не операторы)

        apply = findAction("apply[]");
        cancel = findAction("cancel[]");

        onStarted = findAction("onStarted[]");
        
        addedObject = findProperty("addedObject[]");
        requestCanceled = findProperty("requestCanceled[]");
        requestPushed = findProperty("requestPushed[]");
        isActiveForm = findProperty("isActiveForm[]");
        formPageCount = findProperty("formPageCount[]");
        exportFile = findProperty("exportFile[]");
        exportFiles = findProperty("exportFiles[VARSTRING[100]]");
        ignorePrintType = findProperty("ignorePrintType[]");

        imported = findProperty("imported[INTEGER]");
        importedString = findProperty("importedString[VARSTRING[10]]");

        sleep = findAction("sleep[LONG]");
        applyOnlyWithoutRecalc = findAction("applyOnlyWithoutRecalc[]");
        applyAll = findAction("applyAll[]");

        staticName = findProperty("staticName[Object]");
        staticCaption = findProperty("staticCaption[Object]");
        
        sessionOwners = findProperty("sessionOwners[]");
        ((SessionDataProperty)sessionOwners.property).noNestingInNestedSession = true;

        objectClassName = findProperty("objectClassName[Object]");
        statCustomObjectClass = findProperty("stat[CustomObjectClass]");
        
        // Настройка отчетов
        reportRowHeight = findProperty("reportRowHeight[]");
        reportCharWidth = findProperty("reportCharWidth[]");
        reportToStretch = findProperty("reportToStretch[]");

        networkPath = findProperty("networkPath[]");
        
        // Настройка форм
        defaultBackgroundColor = findProperty("defaultBackgroundColor[]");
        defaultOverrideBackgroundColor = findProperty("defaultOverrideBackgroundColor[]");
        defaultForegroundColor = findProperty("defaultForegroundColor[]");
        defaultOverrideForegroundColor = findProperty("defaultOverrideForegroundColor[]");

        initNavigators();
    }

    @Override
    public void initIndexes() throws RecognitionException {
        
        super.initIndexes();
        
        addIndex(staticCaption);
    }

    @IdentityStrongLazy
    public <P extends PropertyInterface> PropertyFormEntity getLogForm(CalcProperty<P> property, CalcProperty messageProperty) { // messageProperty - nullable
        PropertyFormEntity form = new PropertyFormEntity(this, property, messageProperty, recognizeGroup);
        addFormEntity(form);
        return form;
    }

    public static int generateStaticNewID() {
        return idGenerator.idShift();
    }

    // Окна
    public class Windows {
        public ToolBarNavigatorWindow root;
        public NavigatorWindow toolbar;
        public NavigatorWindow tree;
        public AbstractWindow forms;
        public AbstractWindow log;
        public AbstractWindow status;
    }

    public Windows baseWindows;

    // Навигаторы
    public NavigatorElement root;

    public NavigatorElement administration;

    public NavigatorElement application;
    public NavigatorElement systemEvents;
    public NavigatorElement configuration;

    private void initNavigators() throws ScriptingErrorLog.SemanticErrorException {

        // Окна
        baseWindows = new Windows();
        baseWindows.root = (ToolBarNavigatorWindow) findWindow("root");

        baseWindows.toolbar = (NavigatorWindow) findWindow("toolbar");

        baseWindows.tree = (NavigatorWindow) findWindow("tree");

        baseWindows.forms = addWindow(new AbstractWindow(elementCanonicalName("forms"), LocalizedString.create("{logics.window.forms}"), 20, 20, 80, 79));

        baseWindows.log = addWindow(new AbstractWindow(elementCanonicalName("log"), LocalizedString.create("{logics.window.log}"), 0, 70, 20, 29));

        baseWindows.status = addWindow(new AbstractWindow(elementCanonicalName("status"), LocalizedString.create("{logics.window.status}"), 0, 99, 100, 1));
        baseWindows.status.titleShown = false;

        // todo : перенести во внутренний класс Navigator, как в Windows
        // Навигатор
        root = findNavigatorElement("root");

        administration = findNavigatorElement("administration");

        application = findNavigatorElement("application");

        configuration = findNavigatorElement("configuration");

        systemEvents = findNavigatorElement("systemEvents");
    }


    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////
    /// Properties
    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////

    // --------------------------------- Identity Strong Lazy ----------------------------------- //

    @Override
    @IdentityStrongLazy
    public LCP is(ValueClass valueClass) {
        return addProperty(null, new LCP<>(valueClass.getProperty()));
    }
    @Override
    @IdentityStrongLazy
    public LCP object(ValueClass valueClass) {
        LCP lcp = addJProp(false, LocalizedString.create(valueClass.toString()), and1, 1, is(valueClass), 1);
        ((JoinProperty)lcp.property).objectPropertyClass = valueClass;
        return lcp;
    }

    @Override
    @IdentityStrongLazy
    public LCP not() {
        return addProperty(null, new LCP<>(NotFormulaProperty.instance));
    }

    @Override
    @IdentityStrongLazy
    protected <P extends PropertyInterface> LCP addCProp(StaticClass valueClass, Object value) {
        CalcPropertyRevImplement<P, Integer> implement = (CalcPropertyRevImplement<P, Integer>) DerivedProperty.createCProp(LocalizedString.NONAME, valueClass, value, MapFact.<Integer, ValueClass>EMPTY());
        return addProperty(null, new LCP<>(implement.property, ListFact.fromIndexedMap(implement.mapping.reverse())));
    }

    @Override
    @IdentityStrongLazy
    protected LCP addCastProp(DataClass castClass) {
        return addProperty(null, new LCP<>(new FormulaImplProperty(LocalizedString.create("castTo" + castClass.toString()), 1, new CastFormulaImpl(castClass))));
    }

    @Override
    public SessionDataProperty getAddedObjectProperty() {
        return (SessionDataProperty) addedObject.property;
    }

    @IdentityStrongLazy
    @NFLazy
    public AnyValuePropertyHolder getRequestedValueProperty() {
        return addAnyValuePropertyHolder("requested");
    }

    public AnyValuePropertyHolder addAnyValuePropertyHolder(String namePrefix) {
        return new AnyValuePropertyHolder(
                getLCPByUniqueName(namePrefix + "Object"),
                getLCPByUniqueName(namePrefix + "String"),
                getLCPByUniqueName(namePrefix + "Text"),
                getLCPByUniqueName(namePrefix + "Integer"),
                getLCPByUniqueName(namePrefix + "Long"),
                getLCPByUniqueName(namePrefix + "Double"),
                getLCPByUniqueName(namePrefix + "Numeric"),
                getLCPByUniqueName(namePrefix + "Year"),
                getLCPByUniqueName(namePrefix + "DateTime"),
                getLCPByUniqueName(namePrefix + "Logical"),
                getLCPByUniqueName(namePrefix + "Date"),
                getLCPByUniqueName(namePrefix + "Time"),
                getLCPByUniqueName(namePrefix + "Color"),
                getLCPByUniqueName(namePrefix + "WordFile"),
                getLCPByUniqueName(namePrefix + "ImageFile"),
                getLCPByUniqueName(namePrefix + "PdfFile"),
                getLCPByUniqueName(namePrefix + "CustomFile"),
                getLCPByUniqueName(namePrefix + "ExcelFile"),
                getLCPByUniqueName(namePrefix + "WordLink"),
                getLCPByUniqueName(namePrefix + "ImageLink"),
                getLCPByUniqueName(namePrefix + "PdfLink"),
                getLCPByUniqueName(namePrefix + "CustomLink"),
                getLCPByUniqueName(namePrefix + "ExcelLink")
        );
    }

    protected LCP<?> getLCPByUniqueName(String name) {
        Iterable<LCP<?>> result = getNamedProperties(name);
        assert size(result) == 1;
        return result.iterator().next();
    }

    public LCP getRequestCanceledProperty() {
        return requestCanceled;
    }

    private LCP getRequestPushedProperty() {
        return requestPushed;
    }

    @Override
    public LCP getIsActiveFormProperty() {
        return isActiveForm;
    }

    @Override
    @IdentityStrongLazy
    public <P extends PropertyInterface> LCP<P> addOldProp(LCP<P> lp, PrevScope scope) {
        return addProperty(null, new LCP<>(lp.property.getOld(scope), lp.listInterfaces));
    }

    @Override
    @IdentityStrongLazy
    public <P extends PropertyInterface> LCP<P> addCHProp(LCP<P> lp, IncrementType type, PrevScope scope) {
        addOldProp(lp, scope); // регистрируем старое значение в списке свойств
        return addProperty(null, new LCP<>(lp.property.getChanged(type, scope), lp.listInterfaces));
    }

    @Override
    @IdentityStrongLazy
    public <P extends PropertyInterface> LCP addClassProp(LCP<P> lp) {
        return mapLProp(null, false, lp.property.getClassProperty().cloneProp(), lp);
    }

    @Override
    @IdentityStrongLazy
    public LCP getObjValueProp(FormEntity formEntity, ObjectEntity obj) {
        ValueClass cls = obj.baseClass;
        LCP result = addProp(new ObjectValueProperty(cls, obj));
        if (formEntity.getCanonicalName() != null) {
            // issue #1725 Потенциальное совпадение канонических имен различных свойств
            String name = objValuePrefix + formEntity.getCanonicalName().replace('.', '_') + "_" + obj.getSID();
            makePropertyPublic(result, name, cls.getResolveSet());
        }
        return result;
    }

    @Override
    @IdentityStrongLazy
    public LAP getAddObjectAction(FormEntity formEntity, ObjectEntity obj, CustomClass explicitClass) {
        CustomClass cls = explicitClass;
        if(explicitClass == null)
            cls = (CustomClass)obj.baseClass;
        LAP result = addAProp(new FormAddObjectActionProperty(cls, obj));
        
        setAddActionOptions(result, obj);
        
        if (formEntity.getCanonicalName() != null) {
            String name = "_NEW_" + formEntity.getCanonicalName().replace('.', '_') + "_" + obj.getSID() + (explicitClass != null ? getClassPrefix(cls) : "");
            makeActionPublic(result, name, cls.getResolveSet());
        }
        return result;
    }

    @IdentityStrongLazy
    public LAP getFormNavigatorAction(FormEntity form) {
        return addIFAProp(LocalizedString.NONAME, form, new ArrayList<ObjectEntity>(), false, WindowFormType.DOCKED, true);
    }

    @IdentityStrongLazy
    public LAP getAddFormAction(CustomClass cls, FormEntity contextForm, ObjectEntity contextObject, FormSessionScope scope, ClassFormEntity form) {
        LAP<?> result = addAddFormAction(cls, contextObject, scope);
        // issue #1725 Потенциальное совпадение канонических имен различных свойств
        String contextPrefix = getFormPrefix(contextForm) + getObjectPrefix(contextObject);
        String name = "_ADDFORM" + scope + contextPrefix + getClassPrefix(cls);

        makeActionPublic(result, name, new ArrayList<ResolveClassSet>());

        return result;
    }

    @IdentityStrongLazy
    public LAP getEditFormAction(CustomClass cls, FormSessionScope scope, ClassFormEntity form) {
        LAP<?> result = addEditFormAction(scope, cls);
        // issue #1725 Потенциальное совпадение канонических имен различных свойств
        String name = "_EDITFORM" + scope + getClassPrefix(cls);
        makeActionPublic(result, name, form.object.getResolveClassSet());

        return result;
    }

    @IdentityStrongLazy
    public LAP getDeleteAction(CustomClass cls, FormSessionScope scope) {
        LAP res = addDeleteAction(cls, scope);

        String name = "_DELETE" + (scope == FormSessionScope.OLDSESSION ? "SESSION" : (scope == FormSessionScope.NESTEDSESSION ? scope : ""));
        makeActionPublic(res, name, cls.getResolveSet());
        return res;
    }

    private static String getClassPrefix(CustomClass cls) {
        return "_" + cls.getSID();
    }

    private static String getFormPrefix(FormEntity formEntity) {
        return formEntity.isNamed() ? "_" + formEntity.getCanonicalName().replace('.', '_') : "";
    }

    private static String getObjectPrefix(ObjectEntity objectEntity) {
        return "_" + objectEntity.getSID();
    }

    // REQUEST / INPUT BLOCK
    
    public void dropRequestCanceled(ExecutionEnvironment env) throws SQLException, SQLHandledException {
        getRequestCanceledProperty().change((Object)null, env);
    }
    
    public <R> R pushRequest(ExecutionEnvironment env, SQLCallable<R> callable) throws SQLException, SQLHandledException {
        dropRequestCanceled(env);
        return pushPopRequestValue(true, env, callable);
    }

    public <R> R pushPopRequestValue(boolean push, ExecutionEnvironment env, SQLCallable<R> callable) throws SQLException, SQLHandledException {
        LCP requestPushed = getRequestPushedProperty();
        Object prevValue = requestPushed.read(env);
        requestPushed.change(push ? true : null, env);
        try {
            return callable.call();
        } finally {
            requestPushed.change(prevValue, env);
        }
    }

    public boolean isRequestPushed(ExecutionEnvironment env) throws SQLException, SQLHandledException {
        return getRequestPushedProperty().read(env) != null;
    }

    public boolean isRequestCanceled(ExecutionEnvironment env) throws SQLException, SQLHandledException {
        return getRequestCanceledProperty().read(env) != null;
    }

    public <R> R pushRequestedValue(ObjectValue value, Type type, ExecutionEnvironment env, SQLCallable<R> callable) throws SQLException, SQLHandledException {
        if(value != null) {
            getRequestedValueProperty().write(type, value, env);
            return pushRequest(env, callable);
        } else
            return callable.call();
    }

    // defaultchange'и + обратная совместимость
    public ObjectValue getRequestedValue(Type type, ExecutionEnvironment env, SQLCallable<ObjectValue> request) throws SQLException, SQLHandledException {
        if(isRequestPushed(env))
            return getRequestedValueProperty().read(type, env);

        ObjectValue result = request.call();
        writeRequested(RequestResult.get(result, type, null), env);
        return result;
    }
    
    public void writeRequested(ImList<RequestResult> requestResults, ExecutionEnvironment env) throws SQLException, SQLHandledException {
        LCP<?> requestCanceledProperty = getRequestCanceledProperty();
        if (requestResults == null) {
            requestCanceledProperty.change(true, env);
        } else {
            requestCanceledProperty.change((Object)null, env);
            for(RequestResult requestResult : requestResults) {
                if (requestResult.targetProp == null)
                    getRequestedValueProperty().write(requestResult.type, requestResult.chosenValue, env);
                else
                    requestResult.targetProp.change(requestResult.chosenValue, env);
            }
        }
    }
}
