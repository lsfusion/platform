package lsfusion.server.logics;

import com.google.common.base.Throwables;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.identity.DefaultIDGenerator;
import lsfusion.base.identity.IDGenerator;
import lsfusion.interop.Compare;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.caches.IdentityStrongLazy;
import lsfusion.server.classes.*;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.data.expr.formula.CastFormulaImpl;
import lsfusion.server.form.entity.ClassFormEntity;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyFormEntity;
import lsfusion.server.form.instance.FormSessionScope;
import lsfusion.server.form.navigator.NavigatorElement;
import lsfusion.server.form.window.AbstractWindow;
import lsfusion.server.form.window.NavigatorWindow;
import lsfusion.server.form.window.ToolBarNavigatorWindow;
import lsfusion.server.logics.debug.ActionPropertyDebugger;
import lsfusion.server.logics.debug.WatchActionProperty;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.mutables.NFFact;
import lsfusion.server.logics.mutables.NFLazy;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.FormAddObjectActionProperty;
import lsfusion.server.logics.property.actions.flow.BreakActionProperty;
import lsfusion.server.logics.property.actions.flow.ReturnActionProperty;
import lsfusion.server.logics.property.derived.DerivedProperty;
import lsfusion.server.logics.property.group.AbstractGroup;
import lsfusion.server.logics.property.group.PropertySet;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.logics.table.TableFactory;
import org.antlr.runtime.RecognitionException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;

import static lsfusion.server.logics.ServerResourceBundle.getString;

/**
 * User: DAle
 * Date: 16.05.11
 * Time: 17:52
 */

public class BaseLogicsModule<T extends BusinessLogics<T>> extends ScriptingLogicsModule {
    // classes
    // classes
    public BaseClass baseClass;

    public ConcreteCustomClass formResult;

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
    public LCP upper;
    public LCP sum;
    public LCP subtract;
    public LCP multiply;
    public LCP subtractInteger;
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

    private LAP formPrint;
    private LAP formEdit;
    private LAP formXls;
    private LAP formDrop;
    private LAP formRefresh;
    private LAP formApply;
    private LAP formCancel;
    private LAP formOk;
    private LAP formClose;

    private LAP watch;

    public LAP seek;

    public LAP sleep;
    public LAP applyOnlyWithoutRecalc;
    public LAP applyAll;

    public LAP delete;

    public LAP<?> apply;
    public LCP<?> canceled;
    public LAP<?> onStarted;

    public LAP flowBreak;
    public LAP flowReturn;
    public LAP<?> cancel;

    public LCP objectClass;
    public LCP random;
    public LCP objectClassName;
    public LCP staticName;
    public LCP staticCaption;
    public LCP statCustomObjectClass; 

    private LCP addedObject;
    private LCP confirmed;
    private LCP requestCanceled;
    private LCP formResultProp;
    public LCP formPageCount;
    public LCP formExportFile;
    public LCP ignorePrintType;

    public LCP imported;
    public LCP importedString;

    public LCP defaultBackgroundColor;
    public LCP defaultOverrideBackgroundColor;
    public LCP defaultForegroundColor;
    public LCP defaultOverrideForegroundColor;

    public LCP selectedRowBackgroundColor;
    public LCP overrideSelectedRowBackgroundColor;
    public LCP selectedRowBorderColor;
    public LCP overrideSelectedRowBorderColor;
    public LCP selectedCellBackgroundColor;
    public LCP overrideSelectedCellBackgroundColor;
    public LCP focusedCellBackgroundColor;
    public LCP overrideFocusedCellBackgroundColor;
    public LCP focusedCellBorderColor;
    public LCP overrideFocusedCellBorderColor;

    public LCP reportRowHeight, reportCharWidth, reportToStretch;
    
    public ObjectValuePropertySet objectValue;

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
        namedModuleProperties = NFFact.simpleMap(namedModuleProperties);
    }

    @IdentityLazy
    public LAP getFormPrint() {
        try {
            return formPrint = findAction("formPrint[]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            Throwables.propagate(e);
        }
        return null;
    }

    @IdentityLazy
    public LAP getFormEdit() {
        try {
            return formEdit = findAction("formEdit[]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            Throwables.propagate(e);
        }
        return null;
    }

    @IdentityLazy
    public LAP getFormXls() {
        try {
            return formXls = findAction("formXls[]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            Throwables.propagate(e);
        }
        return null;
    }

    @IdentityLazy
    public LAP getFormDrop() {
        try {
            return formDrop = findAction("formDrop[]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            Throwables.propagate(e);
        }
        return null;
    }

    @IdentityLazy
    public LAP getFormRefresh() {
        try {
            return formRefresh = findAction("formRefresh[]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            Throwables.propagate(e);
        }
        return null;
    }

    @IdentityLazy
    public LAP getFormApply() {
        try {
            return formApply = findAction("formApply[]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            Throwables.propagate(e);
        }
        return null;
    }

    @IdentityLazy
    public LAP getFormCancel() {
        try {
            return formCancel = findAction("formCancel[]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            Throwables.propagate(e);
        }
        return null;
    }

    @IdentityLazy
    public LAP getFormOk() {
        try {
            return formOk = findAction("formOk[]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            Throwables.propagate(e);
        }
        return null;
    }

    @IdentityLazy
    public LAP getFormClose() {
        try {
            return formClose = findAction("formClose[]");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            Throwables.propagate(e);
        }
        return null;
    }

    public PropertyDBNamePolicy getDBNamePolicy() {
        return propertyDBNamePolicy;
    }
    
    public AbstractPropertyNameParser.ClassFinder getClassFinder() {
        return new PropertyCanonicalNameParser.CanonicalNameClassFinder(BL);    
    }
    
    @Override
    public void initClasses() throws RecognitionException {
        baseClass = addBaseClass(transformNameToSID("Object"), getString("logics.object"));
        
        super.initClasses();

        formResult = (ConcreteCustomClass) findClass("FormResult");
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
        Version version = getVersion();

        objectClass = addProperty(null, new LCP<ClassPropertyInterface>(baseClass.getObjectClassProperty()));
        makePropertyPublic(objectClass, "objectClass", Collections.<ResolveClassSet>nCopies(1, null));
        random = addRMProp("Random");
        makePropertyPublic(random, "random", Arrays.<ResolveClassSet>asList());

        // только через операторы 
        flowBreak = addProperty(null, new LAP(new BreakActionProperty()));
        flowReturn = addProperty(null, new LAP(new ReturnActionProperty()));

        // Множества свойств
        objectValue = new ObjectValuePropertySet();
        publicGroup.add(objectValue, version);

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

        // Математические операции
        sum = addSumProp();
        multiply = addMultProp();
        subtract = addSubtractProp();
        divide = addDivideProp();

        minus = addSFProp("(-(prm1))", 1);

        // Константы
        vtrue = addCProp(LogicalClass.instance, true);
        vzero = addCProp(DoubleClass.instance, 0);
        vnull = addProperty((AbstractGroup) null, new LCP<PropertyInterface>(NullValueProperty.instance));

        if(ActionPropertyDebugger.getInstance().isEnabled()) {
            watch = addProperty((AbstractGroup) null, new LAP<ClassPropertyInterface>(WatchActionProperty.instance));
            makePropertyPublic(watch, "watch");
        }

        super.initProperties();

        // через JOIN (не операторы)

        canceled = findProperty("canceled[]");

        apply = findAction("apply");
        cancel = findAction("cancel[]");

        onStarted = findAction("onStarted[]");


        // Обработка строк
        upper = findProperty("upper[?]");

        // Операции с целыми числами
        subtractInteger = findProperty("subtractInteger[DATE,DATE]");

        seek = findAction("seek[Object]");
        
        addedObject = findProperty("addedObject[]");
        confirmed = findProperty("confirmed[]");
        requestCanceled = findProperty("requestCanceled[]");
        formResultProp = findProperty("formResult[]");
        formPageCount = findProperty("formPageCount[]");
        formExportFile = findProperty("formExportFile[]");
        ignorePrintType = findProperty("ignorePrintType[]");

        imported = findProperty("imported[INTEGER]");
        importedString = findProperty("importedString[VARSTRING[10]]");

        sleep = findAction("sleep[LONG]");
        applyOnlyWithoutRecalc = findAction("applyOnlyWithoutRecalc[]");
        applyAll = findAction("applyAll[]");

        staticName = findProperty("staticName[Object]");
        staticCaption = findProperty("staticCaption[Object]");
        ((CalcProperty)staticCaption.property).aggProp = true;

        objectClassName = findProperty("objectClassName[Object]");
        statCustomObjectClass = findProperty("stat[CustomObjectClass]");
        
        // Настройка отчетов
        reportRowHeight = findProperty("reportRowHeight[]");
        reportCharWidth = findProperty("reportCharWidth[]");
        reportToStretch = findProperty("reportToStretch[]");
        
        // Настройка форм
        defaultBackgroundColor = findProperty("defaultBackgroundColor[]");
        defaultOverrideBackgroundColor = findProperty("defaultOverrideBackgroundColor[]");
        defaultForegroundColor = findProperty("defaultForegroundColor[]");
        defaultOverrideForegroundColor = findProperty("defaultOverrideForegroundColor[]");

        selectedRowBackgroundColor = findProperty("selectedRowBackgroundColor[]");
        overrideSelectedRowBackgroundColor = findProperty("overrideSelectedRowBackgroundColor[]");
        selectedRowBorderColor = findProperty("selectedRowBorderColor[]");
        overrideSelectedRowBorderColor = findProperty("overrideSelectedRowBorderColor[]");
        selectedCellBackgroundColor = findProperty("selectedCellBackgroundColor[]");
        overrideSelectedCellBackgroundColor = findProperty("overrideSelectedCellBackgroundColor[]");
        focusedCellBackgroundColor = findProperty("focusedCellBackgroundColor[]");
        overrideFocusedCellBackgroundColor = findProperty("overrideFocusedCellBackgroundColor[]");
        focusedCellBorderColor = findProperty("focusedCellBorderColor[]");
        overrideFocusedCellBorderColor = findProperty("overrideFocusedCellBorderColor[]");
        initNavigators();
    }

    @Override
    public void initIndexes() throws RecognitionException {
        
        super.initIndexes();
        
        addIndex(staticCaption);
    }

    @IdentityStrongLazy
    public <P extends PropertyInterface> PropertyFormEntity<T> getLogForm(CalcProperty<P> property) {
        PropertyFormEntity<T> form = new PropertyFormEntity<T>(this, property, recognizeGroup);
        addFormEntity(form);
        return form;
    }

    public static int generateStaticNewID() {
        return idGenerator.idShift();
    }

    public abstract class MapClassesPropertySet<K, V extends CalcProperty> extends PropertySet {
        protected final LinkedHashMap<K, V> properties = new LinkedHashMap<K, V>();

        @Override
        public ImOrderSet<Property> getProperties() {
            return SetFact.fromJavaOrderSet(new ArrayList<Property>(properties.values()));
        }

        @Override
        protected ImList<CalcPropertyClassImplement> getProperties(ImSet<ValueClassWrapper> classes, Version version) {
            ImOrderSet<ValueClassWrapper> orderClasses = classes.toOrderSet();
            ValueClass[] valueClasses = getClasses(orderClasses);
            V property = getProperty(valueClasses, version);

            ImOrderSet<?> interfaces = getPropertyInterfaces(property, valueClasses);
            return ListFact.singleton(new CalcPropertyClassImplement(property, orderClasses, interfaces));
        }

        private ValueClass[] getClasses(ImOrderSet<ValueClassWrapper> classes) {
            ValueClass[] valueClasses = new ValueClass[classes.size()];
            for (int i = 0; i < classes.size(); i++) {
                valueClasses[i] = classes.get(i).valueClass;
            }
            return valueClasses;
        }

        @NFLazy
        protected V getProperty(ValueClass[] classes, Version version) {
            K key = createKey(classes);
            if (!properties.containsKey(key)) {
                V property = createProperty(classes, version);
                properties.put(key, property);
                return property;
            } else {
                return properties.get(key);
            }
        }

        protected abstract ImOrderSet<?> getPropertyInterfaces(V property, ValueClass[] valueClasses);

        protected abstract V createProperty(ValueClass[] classes, Version version);

        protected abstract K createKey(ValueClass[] classes);
    }

    public class ObjectValuePropertySet extends MapClassesPropertySet<ValueClass, ObjectValueProperty> {
        @Override
        protected boolean isInInterface(ImSet<ValueClassWrapper> classes) {
            return classes.size() == 1;
        }

        protected Class<?> getPropertyClass() {
            return ObjectValueProperty.class;
        }

        @Override
        protected ImOrderSet<?> getPropertyInterfaces(ObjectValueProperty property, ValueClass[] valueClasses) {
            return SetFact.singletonOrder(property.getOrderInterfaces().get(0));
        }

        @Override
        protected ValueClass createKey(ValueClass[] classes) {
            assert classes.length == 1;
            return classes[0].getBaseClass();
        }

        @Override
        protected ObjectValueProperty createProperty(ValueClass[] classes, Version version) {
            assert classes.length == 1;

            ValueClass valueClass = classes[0].getBaseClass();
            ObjectValueProperty property = new ObjectValueProperty(valueClass);
            // Необходимо создавать свойства с разными каноническими именами. В случае с классами STRING и NUMERIC их размерность не влияет на сигнатуру,
            // поэтому для этих классов будем создавать другие имена. включающие в себя сигнатуру
            String name = PropertyCanonicalNameUtils.objValuePrefix;
            if (valueClass instanceof StringClass || valueClass instanceof NumericClass) {
                name = name + valueClass.getSID();
            }
            property.setCanonicalName(getNamespace(), name, Arrays.asList(valueClass.getResolveSet()), property.getOrderInterfaces(), getDBNamePolicy());
            setParent(property, version);
            return property;
        }
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

    public Windows windows;

    // Навигаторы
    public NavigatorElement<T> root;

    public NavigatorElement<T> administration;

    public NavigatorElement<T> objects;

    public NavigatorElement<T> application;
    public NavigatorElement<T> systemEvents;
    public NavigatorElement<T> configuration;

    public FormEntity<T> objectForm;

    private void initNavigators() throws ScriptingErrorLog.SemanticErrorException {

        // Окна
        windows = new Windows();
        windows.root = (ToolBarNavigatorWindow) findWindow("root");

        windows.toolbar = (NavigatorWindow) findWindow("toolbar");

        windows.tree = (NavigatorWindow) findWindow("tree");

        windows.forms = addWindow("forms", new AbstractWindow(null, getString("logics.window.forms"), 20, 20, 80, 79));

        windows.log = addWindow("log", new AbstractWindow(null, getString("logics.window.log"), 0, 70, 20, 29));

        windows.status = addWindow("status", new AbstractWindow(null, getString("logics.window.status"), 0, 99, 100, 1));
        windows.status.titleShown = false;

        // todo : перенести во внутренний класс Navigator, как в Windows
        // Навигатор
        root = findNavigatorElement("root");

        administration = findNavigatorElement("administration");

        application = findNavigatorElement("application");

        configuration = findNavigatorElement("configuration");

        systemEvents = findNavigatorElement("systemEvents");

        objects = findNavigatorElement("objects");
    }

    public void initClassForms() {
        objectForm = baseClass.getBaseClassForm(this);
        objects.add(objectForm, getVersion());
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
        return addProperty(null, new LCP<ClassPropertyInterface>(valueClass.getProperty()));
    }
    @Override
    @IdentityStrongLazy
    public LCP object(ValueClass valueClass) {
        LCP lcp = addJProp(false, valueClass.toString(), and1, 1, is(valueClass), 1);
        ((JoinProperty)lcp.property).objectPropertyClass = valueClass;
        return lcp;
    }

    @Override
    @IdentityStrongLazy
    public LCP not() {
        return addProperty(null, new LCP<PropertyInterface>(NotFormulaProperty.instance));
    }

    @Override
    @IdentityStrongLazy
    protected <T extends PropertyInterface> LCP addCProp(StaticClass valueClass, Object value) {
        CalcPropertyRevImplement<T, Integer> implement = (CalcPropertyRevImplement<T, Integer>) DerivedProperty.createCProp("sys", valueClass, value, MapFact.<Integer, ValueClass>EMPTY());
        return addProperty(null, false, new LCP<T>(implement.property, ListFact.fromIndexedMap(implement.mapping.reverse())));
    }

    @Override
    @IdentityStrongLazy
    protected <P extends PropertyInterface> LCP addCastProp(DataClass castClass) {
        return addProperty(null, new LCP<FormulaImplProperty.Interface>(new FormulaImplProperty("castTo" + castClass.toString(), 1, new CastFormulaImpl(castClass))));
    }

    @Override
    public SessionDataProperty getAddedObjectProperty() {
        return (SessionDataProperty) addedObject.property;
    }

    @Override
    public LCP getConfirmedProperty() {
        return confirmed;
    }

    @Override
    @IdentityStrongLazy
    public AnyValuePropertyHolder getChosenValueProperty() {
        return addAnyValuePropertyHolder("chosen", "Chosen", StringClass.get(100));
    }

    @Override
    @IdentityStrongLazy
    public AnyValuePropertyHolder getRequestedValueProperty() {
        return addAnyValuePropertyHolder("requested", "Requested");
    }

    @Override
    @IdentityStrongLazy
    public LCP getRequestCanceledProperty() {
        return requestCanceled;
    }

    @Override
    @IdentityStrongLazy
    public LCP getFormResultProperty() {
        return formResultProp;
    }

    @Override
    @IdentityStrongLazy
    public <T extends PropertyInterface> LCP<T> addOldProp(LCP<T> lp, PrevScope scope) {
        return addProperty(null, new LCP<T>(lp.property.getOld(scope), lp.listInterfaces));
    }

    @Override
    @IdentityStrongLazy
    public <T extends PropertyInterface> LCP<T> addCHProp(LCP<T> lp, IncrementType type, PrevScope scope) {
        addOldProp(lp, scope); // регистрируем старое значение в списке свойств
        return addProperty(null, new LCP<T>(lp.property.getChanged(type, scope), lp.listInterfaces));
    }

    @Override
    @IdentityStrongLazy
    public <T extends PropertyInterface> LCP addClassProp(LCP<T> lp) {
        return mapLProp(null, false, lp.property.getClassProperty().cloneProp(), lp);
    }

    @Override
    @IdentityStrongLazy
    public LAP getAddObjectAction(CustomClass cls, FormEntity formEntity, ObjectEntity obj) {
        LAP result = addAProp(new FormAddObjectActionProperty(cls, obj));
        if (formEntity.getCanonicalName() != null) {
            String name = "_ADDOBJ_" + formEntity.getCanonicalName().replace('.', '_') + "_" + obj.getSID();
            makePropertyPublic(result, name, cls.getResolveSet());
        }
        return result;
    }

    @Override
    @IdentityStrongLazy
    public LAP getDeleteAction(CustomClass cls, boolean oldSession) {
        String name = "_DELETE" + (oldSession ? "SESSION" : "");

        LAP res = addChangeClassAProp(baseClass.unknown, 1, 0, false, true, 1, is(cls), 1);
        if (!oldSession) {
            res = addNewSessionAProp(null, res.property.caption, res, true, false, false);
            res.setAskConfirm(true);
        }
        setDeleteActionOptions(res);
        makePropertyPublic(res, name, cls.getResolveSet());
        return res;
    }

    @IdentityStrongLazy
    public LAP getAddFormAction(CustomClass cls, FormSessionScope scope, ClassFormEntity form) {
        String name = "_ADDFORM" + scope + "_" + cls.getSID() + (form.form.isNamed() ? "_" + form.form.getCanonicalName().replace('.', '_') : "");
        LAP result = addDMFAProp(null, ServerResourceBundle.getString("logics.add"), //+ "(" + cls + ")",
                form.form, new ObjectEntity[]{},
                form.form.addPropertyObject(getAddObjectAction(cls, form.form, form.object)), scope, true);
        makePropertyPublic(result, name, new ArrayList<ResolveClassSet>());

        setAddFormActionProperties(result, form, scope);
        return result;
    }

    @IdentityStrongLazy
    public LAP getEditFormAction(CustomClass cls, FormSessionScope scope, ClassFormEntity form) {
        String name = "_EDITFORM" + scope + "_" + cls.getSID() + (form.form.isNamed() ? "_" + form.form.getCanonicalName().replace('.', '_') : "");
        LAP result = addDMFAProp(null, ServerResourceBundle.getString("logics.edit"), form.form, new ObjectEntity[]{form.object}, scope);
        makePropertyPublic(result, name, form.object.getResolveClassSet());
        return result;
    } 
}
