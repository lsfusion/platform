package platform.server.logics;

import platform.base.BaseUtils;
import platform.base.col.ListFact;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MMap;
import platform.base.col.interfaces.mutable.add.MAddExclMap;
import platform.base.identity.DefaultIDGenerator;
import platform.base.identity.IDGenerator;
import platform.interop.Compare;
import platform.interop.form.layout.ContainerType;
import platform.server.caches.IdentityStrongLazy;
import platform.server.classes.*;
import platform.server.data.Time;
import platform.server.data.Union;
import platform.server.data.expr.formula.CastFormulaImpl;
import platform.server.data.expr.query.PartitionType;
import platform.server.form.entity.ClassFormEntity;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
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
import platform.server.logics.property.actions.FormAddObjectActionProperty;
import platform.server.logics.property.actions.flow.ApplyActionProperty;
import platform.server.logics.property.actions.flow.BreakActionProperty;
import platform.server.logics.property.actions.flow.CancelActionProperty;
import platform.server.logics.property.actions.flow.ReturnActionProperty;
import platform.server.logics.property.actions.form.*;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.property.group.PropertySet;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.table.TableFactory;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import static platform.server.logics.ServerResourceBundle.getString;

/**
 * User: DAle
 * Date: 16.05.11
 * Time: 17:52
 */

public class BaseLogicsModule<T extends BusinessLogics<T>> extends LogicsModule {
    // classes
    public BaseClass baseClass;

    public ConcreteCustomClass formResult;

    // groups
    public AbstractGroup rootGroup;
    public AbstractGroup publicGroup;
    public AbstractGroup privateGroup;
    public AbstractGroup baseGroup;
    public AbstractGroup actionGroup;
    public AbstractGroup recognizeGroup;
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

    public LCP string2SP, istring2SP, string3SP, istring3SP, string4SP, istring4SP, string5SP, istring5SP;
    public LCP string2, istring2, string3, istring3;
    public LCP string5CM;
    public LCP ustring2CM, ustring2SP, ustring3SP, ustring4SP, ustring5SP, ustring2, ustring3, ustring4, ustring3CM, ustring4CM, ustring5CM;
    public LCP ustring2CR;

    public LCP vtrue;
    public LCP vzero;
    public LCP vnull;

    public LCP minus;

    public LAP formPrint;
    public LAP formEdit;
    public LAP formXls;
    public LAP formDrop;
    public LAP formRefresh;
    public LAP formApply;
    public LAP formCancel;
    public LAP formOk;
    public LAP formClose;

    public LAP seek;

    public LAP delete;

    public LAP<?> apply;
    public LCP<?> canceled;

    public LAP flowBreak;
    public LAP flowReturn;
    public LAP<?> cancel;

    public LCP objectClass;
    public LCP objectClassName;
    public LCP staticName;
    public LCP staticCaption;

    public LCP defaultBackgroundColor;
    public LCP defaultOverrideBackgroundColor;
    public LCP defaultForegroundColor;
    public LCP defaultOverrideForegroundColor;

    public SelectionPropertySet selection;
    public ObjectValuePropertySet objectValue;

    public TableFactory tableFactory;

    public List<LP> lproperties = new ArrayList<LP>();

    // счетчик идентификаторов
    static private IDGenerator idGenerator = new DefaultIDGenerator();

    // не надо делать логику паблик, чтобы не было возможности тянуть её прямо из BaseLogicsModule,
    // т.к. она должна быть доступна в точке, в которой вызывается baseLM.BL
    private final T BL;

    public BaseLogicsModule(T BL) {
        super("System", "System");
        setBaseLogicsModule(this);
        this.BL = BL;
    }

    public LP getLP(String sID) {
        objectValue.getProperty(sID);
        selection.getProperty(sID);

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
        baseClass = addBaseClass("Object", getString("logics.object"));

        formResult = addConcreteClass("FormResult", "Результат вызова формы",
                new String[]{"drop", "ok", "close"},
                new String[]{"Сбросить", "Принять", "Закрыть"},
                baseClass);

        initBaseClassAliases();
    }

    @Override
    public void initGroups() {
        rootGroup = addAbstractGroup("root", getString("logics.groups.root"), null, false);
        publicGroup = addAbstractGroup("public", getString("logics.groups.public"), rootGroup, false);
        privateGroup = addAbstractGroup("private", getString("logics.groups.private"), rootGroup, false);
        baseGroup = addAbstractGroup("base", getString("logics.groups.base"), publicGroup, false);
        recognizeGroup = addAbstractGroup("recognize", getString("logics.groups.recognize"), baseGroup, false);
        drillDownGroup = addAbstractGroup("drilldown", getString("logics.groups.drilldown"), rootGroup, false);
        propertyPolicyGroup = addAbstractGroup("propertyPolicy", getString("logics.groups.policy"), rootGroup, false);

        initBaseGroupAliases();
    }

    @Override
    public void initTables() {
        tableFactory = new TableFactory(baseClass);
    }

    @Override
    public void initProperties() throws ScriptingErrorLog.SemanticErrorException {

        canceled = addProperty(null, new LCP<ClassPropertyInterface>(new SessionDataProperty("canceled", "Canceled", LogicalClass.instance)));

        apply = addAProp(new ApplyActionProperty(canceled.property));
        cancel = addAProp(new CancelActionProperty());

        flowBreak = addProperty(null, new LAP(new BreakActionProperty()));
        flowReturn = addProperty(null, new LAP(new ReturnActionProperty()));

        // Множества свойств
        selection = new SelectionPropertySet();
        publicGroup.add(selection);

        objectValue = new ObjectValuePropertySet();
        publicGroup.add(objectValue);

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

        string3SP = addSProp("string3SP", 3);
        istring3SP = addInsensitiveSProp("istring3SP", 3);

        string4SP = addSProp("string4SP", 4);
        istring4SP = addInsensitiveSProp("istring4SP", 4);

        string5SP = addSProp("string5SP", 5);
        istring5SP = addInsensitiveSProp("istring5SP", 5);

        string3 = addSProp("string3", 3, "");
        istring3 = addInsensitiveSProp("istring3", 3, "");

        ustring2CM = addSFUProp("ustring2CM", ",", 2);
        ustring2SP = addSFUProp("ustring2SP", " ", 2);
        ustring3SP = addSFUProp("ustring3SP", " ", 3);
        ustring4SP = addSFUProp("ustring4SP", " ", 4);
        ustring5SP = addSFUProp("ustring5SP", " ", 5);
        ustring2 = addSFUProp("ustring2", "", 2);
        ustring3 = addSFUProp("ustring3", "", 3);
        ustring4 = addSFUProp("ustring4", "", 4);

        string5CM = addSFUProp("string5CM", " ", 5);
        ustring3CM = addSFUProp("ustring3CM", ",", 3);
        ustring4CM = addSFUProp("ustring4CM", ",", 4);
        ustring5CM = addSFUProp("ustring5CM", ",", 5);

        ustring2CR = addSFUProp("ustring2CR", "\n", 2);

        // Обработка строк
        upper = addSFProp("upper", "upper(prm1)", 1);

        // Математические операции
        sum = addSumProp("sum");
        multiply = addMultProp("multiply");
        subtract = addSubtractProp("subtract");
        divide = addDivideProp("divide");

        minus = addSFProp("minus", "(-(prm1))", 1);

        // Оставляем пока в BaseLogicsModule, посколько скорее всего их придется использовать в DSL

        // Операции с целыми числами
        subtractInteger = addSFProp("subtractInteger", "((prm1)-(prm2))", IntegerClass.instance, 2);

        // Константы
        vtrue = addCProp(LogicalClass.instance, true);
        vzero = addCProp(DoubleClass.instance, 0);
        vnull = addProperty(privateGroup, new LCP<PropertyInterface>(NullValueProperty.instance));

        // Действия на форме
        formApply = addProperty(null, new LAP(new FormApplyActionProperty()));
        formCancel = addProperty(null, new LAP(new FormCancelActionProperty()));
        formPrint = addProperty(null, new LAP(new PrintActionProperty()));
        formEdit = addProperty(null, new LAP(new EditActionProperty()));
        formXls = addProperty(null, new LAP(new XlsActionProperty()));
        formDrop = addProperty(null, new LAP(new DropActionProperty()));
        formRefresh = addProperty(null, new LAP(new RefreshActionProperty()));
        formOk = addProperty(null, new LAP(new OkActionProperty()));
        formClose = addProperty(null, new LAP(new CloseActionProperty()));

        seek = addSAProp();

        staticName = addDProp(publicGroup, "staticName", getString("logics.static.name"), StringClass.get(250), baseClass);
        staticCaption = addDProp(publicGroup, "staticCaption", getString("logics.static.caption"), StringClass.geti(100), baseClass);
        ((CalcProperty)staticCaption.property).aggProp = true;

        // todo : поменять возможно названия
        objectClass = addProperty(null, new LCP<ClassPropertyInterface>(baseClass.getObjectClassProperty()));
        objectClassName = addJProp(baseGroup, "objectClassName", getString("logics.object.class"), staticCaption, objectClass, 1);

        // Настройка форм
        defaultBackgroundColor = addDProp("defaultBackgroundColor", getString("logics.default.background.color"), ColorClass.instance);
        defaultOverrideBackgroundColor = addSUProp("defaultOverrideBackgroundColor", true, getString("logics.default.background.color"), Union.OVERRIDE, addCProp(ColorClass.instance, Color.YELLOW), defaultBackgroundColor);
        defaultForegroundColor = addDProp("defaultForegroundColor", getString("logics.default.foreground.color"), ColorClass.instance);
        defaultOverrideForegroundColor = addSUProp("defaultOverrideForegroundColor", true, getString("logics.default.foreground.color"), Union.OVERRIDE, addCProp(ColorClass.instance, Color.RED), defaultForegroundColor);

        initNavigators();
    }

    @Override
    public void initIndexes() {
        addIndex(staticCaption);
    }

    public static int generateStaticNewID() {
        return idGenerator.idShift();
    }

    <T extends LP<?, ?>> void registerProperty(T lp) {
        lproperties.add(lp);     // todo [dale]: нужно?
        lp.property.ID = idGenerator.idShift();
    }

    public abstract class MapClassesPropertySet<K, V extends CalcProperty> extends PropertySet {
        protected LinkedHashMap<K, V> properties = new LinkedHashMap<K, V>();

        @Override
        public ImOrderSet<Property> getProperties() {
            return SetFact.fromJavaOrderSet(new ArrayList<Property>(properties.values()));
        }

        @Override
        protected ImList<CalcPropertyClassImplement> getProperties(ImSet<ValueClassWrapper> classes) {
            ImOrderSet<ValueClassWrapper> orderClasses = classes.toOrderSet();
            ValueClass[] valueClasses = getClasses(orderClasses);
            V property = getProperty(valueClasses);

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

        protected abstract ImOrderSet<?> getPropertyInterfaces(V property, ValueClass[] valueClasses);

        protected abstract V createProperty(ValueClass[] classes);

        protected abstract K createKey(ValueClass[] classes);
    }

    public class SelectionPropertySet extends MapClassesPropertySet<ImMap<ValueClass, Integer>, SelectionProperty> {
        static private final String prefix = "SelectionProperty_";
        private MAddExclMap<String, LP> selectionLP = MapFact.mBigStrongMap();

        protected Class<?> getPropertyClass() {
            return SelectionProperty.class;
        }

        @Override
        protected boolean isInInterface(ImSet<ValueClassWrapper> classes) {
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
        protected ImOrderSet<?> getPropertyInterfaces(SelectionProperty property, ValueClass[] classes) {
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
            return SetFact.toOrderExclSet(interfaces);
        }

        protected ImMap<ValueClass, Integer> createKey(ValueClass[] classes) {
            MMap<ValueClass, Integer> key = MapFact.mMap(MapFact.<ValueClass>addLinear());
            for (ValueClass valueClass : classes)
                key.add(valueClass, 1);
            return key.immutable();
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
            selectionLP.exclAdd(sid, lp);
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

    public class ObjectValuePropertySet extends MapClassesPropertySet<ValueClass, ObjectValueProperty> {
        private Map<String, LP> sidToLP = new HashMap<String, LP>();
        private static final String prefix = "objectValueProperty_";

        @Override
        protected boolean isInInterface(ImSet<ValueClassWrapper> classes) {
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
        protected ImOrderSet<?> getPropertyInterfaces(ObjectValueProperty property, ValueClass[] valueClasses) {
            return SetFact.singletonOrder(property.getOrderInterfaces().get(0));
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
        public ToolBarNavigatorWindow root;
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

    public NavigatorElement<T> administration;

    public NavigatorElement<T> objects;

    public NavigatorElement<T> application;
    public NavigatorElement<T> systemEvents;
    public NavigatorElement<T> configuration;

    public FormEntity<T> objectForm;

    private void initNavigators() {

        // Окна
        windows = new Windows();
        windows.root = addWindow(new ToolBarNavigatorWindow(JToolBar.HORIZONTAL, "root", getString("logics.window.root"), 0, 0, 100, 6));
        windows.root.alignmentY = JToolBar.CENTER_ALIGNMENT;
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
        root = addNavigatorElement("root", getString("logics.forms"), null);
        root.window = windows.root;

        administration = addNavigatorElement(root, "administration", getString("logics.administration"));
        administration.window = windows.toolbar;
        administration.setImage("/images/tools.png");

        application = addNavigatorElement(administration, "application", getString("logics.administration.application"));
        addFormEntity(new OptionsFormEntity(application, "options"));
        addFormEntity(new IntegrationDataFormEntity(application, "integrationData"));
        addFormEntity(new MigrationDataFormEntity(application, "migrationData"));

        configuration = addNavigatorElement(administration, "configuration", getString("logics.administration.config"));

        systemEvents = addNavigatorElement(administration, "systemEvents", getString("logics.administration.events"));

        objects = addNavigatorElement(administration, "objects", getString("logics.object"));
        objects.window = windows.tree;
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

    // --------------------------------- Identity Strong Lazy ----------------------------------- //

    @Override
    @IdentityStrongLazy
    public LCP is(ValueClass valueClass) {
        return addProperty(null, new LCP<ClassPropertyInterface>(valueClass.getProperty()));
    }
    @Override
    @IdentityStrongLazy
    public LCP object(ValueClass valueClass) {
        return addJProp(valueClass.toString(), and1, 1, is(valueClass), 1);
    }

    @Override
    @IdentityStrongLazy
    public LCP not() {
        return addProperty(null, new LCP<PropertyInterface>(NotFormulaProperty.instance));
    }

    @Override
    @IdentityStrongLazy
    protected <T extends PropertyInterface> LCP addCProp(StaticClass valueClass, Object value) {
        CalcPropertyRevImplement<T, Integer> implement = (CalcPropertyRevImplement<T, Integer>) DerivedProperty.createCProp(genSID(), "sys", valueClass, value, MapFact.<Integer, ValueClass>EMPTY());
        return addProperty(null, false, new LCP<T>(implement.property, ListFact.fromIndexedMap(implement.mapping.reverse())));
    }

    @Override
    @IdentityStrongLazy
    protected <P extends PropertyInterface> LCP addCastProp(DataClass castClass) {
        return addProperty(null, new LCP<FormulaImplProperty.Interface>(new FormulaImplProperty(genSID(), "castTo" + castClass.toString(), 1, new CastFormulaImpl(castClass))));
    }

    @Override
    @IdentityStrongLazy
    public SessionDataProperty getAddedObjectProperty() {
        SessionDataProperty addedObject = new SessionDataProperty("addedObject", "Added Object", baseLM.baseClass);
        addProperty(null, new LCP<ClassPropertyInterface>(addedObject));
        return addedObject;
    }

    @Override
    @IdentityStrongLazy
    public LCP getConfirmedProperty() {
        return addProperty(null, new LCP<ClassPropertyInterface>(new SessionDataProperty("confirmed", "Confirmed", LogicalClass.instance)));
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
        return addProperty(null, new LCP<ClassPropertyInterface>(new SessionDataProperty("requestCanceled", "Request Input Canceled", LogicalClass.instance)));
    }

    @Override
    @IdentityStrongLazy
    public LCP getFormResultProperty() {
        return addProperty(null, new LCP<ClassPropertyInterface>(new SessionDataProperty("formResult", "Form Result", baseLM.formResult)));
    }

    @Override
    @IdentityStrongLazy
    protected LAP<?> addSAProp() {
        return addProperty(null, new LAP(new SeekActionProperty(baseLM.baseClass)));
    }

    @Override
    @IdentityStrongLazy
    public <T extends PropertyInterface> LCP<T> addOldProp(LCP<T> lp) {
        return addProperty(null, new LCP<T>(lp.property.getOld(), lp.listInterfaces));
    }

    @Override
    @IdentityStrongLazy
    public <T extends PropertyInterface> LCP<T> addCHProp(LCP<T> lp, IncrementType type) {
        addOldProp(lp); // регистрируем старое значение в списке свойств
        return addProperty(null, new LCP<T>(lp.property.getChanged(type), lp.listInterfaces));
    }

    @Override
    @IdentityStrongLazy
    public <T extends PropertyInterface> LCP addClassProp(LCP<T> lp) {
        return mapLProp(null, false, lp.property.getClassProperty().cloneProp(), lp);
    }

    @Override
    @IdentityStrongLazy
    public LAP getAddObjectAction(CustomClass cls, FormEntity formEntity, ObjectEntity obj) {
        String sid = "addObject" + "_" + BaseUtils.capitalize(cls.getSID()) +
                                   "_" + BaseUtils.capitalize(formEntity.getSID()) +
                                   "_" + BaseUtils.capitalize(obj.getSID());
        return addAProp(new FormAddObjectActionProperty(sid, cls, obj));
    }

    @IdentityStrongLazy
    public LAP getDeleteAction(CustomClass cls, boolean oldSession) {
        String sid = "delete" + (oldSession ? "Session" : "") + "_" + BaseUtils.capitalize(cls.getSID());

        LAP res = addChangeClassAProp(oldSession ? sid : genSID(), baseClass.unknown, 1, 0, false, true, 1, is(cls), 1);
        if (!oldSession) {
            res = (LAP) addNewSessionAProp(null, sid, res.property.caption, res, true, false, SetFact.<SessionDataProperty>EMPTY(), SetFact.<SessionDataProperty>EMPTY());
            res.setAskConfirm(true);
        }
        setDeleteActionOptions(res);
        return res;
    }

    @IdentityStrongLazy
    public LAP getAddFormAction(CustomClass cls, boolean oldSession) {
        ClassFormEntity form = cls.getEditForm(baseLM);

        String sid = "addForm" + (oldSession ? "Session" : "") + "_" + BaseUtils.capitalize(cls.getSID());

        LAP property = addDMFAProp(publicGroup, sid, ServerResourceBundle.getString("logics.add"), //+ "(" + cls + ")",
                form.form, new ObjectEntity[]{},
                form.form.addPropertyObject(getAddObjectAction(cls, form.form, form.object)), !oldSession);
        setAddFormActionProperties(property, form, oldSession);
        return property;
    }

    @IdentityStrongLazy
    public LAP getEditFormAction(CustomClass cls, boolean oldSession) {
        ClassFormEntity form = cls.getEditForm(baseLM);

        String sid = "editForm" + (oldSession ? "Session" : "") + "_" + BaseUtils.capitalize(cls.getSID());

        LAP property = addDMFAProp(publicGroup, sid, ServerResourceBundle.getString("logics.edit"),
                form.form, new ObjectEntity[]{form.object}, !oldSession);
        setEditFormActionProperties(property);
        return property;
    }

    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////
    /// Forms
    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////

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
