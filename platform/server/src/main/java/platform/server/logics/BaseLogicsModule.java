package platform.server.logics;

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
import platform.server.data.expr.query.PartitionType;
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

    public ConcreteCustomClass month;
    public ConcreteCustomClass DOW;

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
    public LCP sumDate;
    public LCP sumDateTimeDay;
    public LCP subtractDateTimeSeconds;
    public LCP subtractDate;
    public LCP dateTimeToDateTime;
    public LCP toDateTime;

    public LCP string2SP, istring2SP, string3SP, istring3SP;
    public LCP string2, istring2, string3, istring3;
    public LCP string5CM;
    public LCP ustring2CM, ustring2SP, ustring3SP, ustring4SP, ustring2, ustring3, ustring4, ustring3CM, ustring4CM, ustring5CM;
    public LCP ustring2CR;

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

    public LCP currentDate;
    public LCP currentMonth;
    public LCP currentYear;
    public LCP currentHour;
    public LCP currentMinute;
    protected LCP currentEpoch;
    protected LCP currentDateTime;
    protected LCP currentTime;

    public LAP delete;
    public LAP deleteApply;

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

    public LCP dumb1;

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

        month = addConcreteClass("Month", getString("logics.month"),
                new String[]{"january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december"},
                new String[]{getString("logics.month.january"), getString("logics.month.february"), getString("logics.month.march"), getString("logics.month.april"), getString("logics.month.may"), getString("logics.month.june"), getString("logics.month.july"), getString("logics.month.august"), getString("logics.month.september"), getString("logics.month.october"), getString("logics.month.november"), getString("logics.month.december")},
                baseClass);
        DOW = addConcreteClass("DOW", getString("logics.week.day"),
                new String[]{"sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday"},
                new String[]{getString("logics.days.sunday"), getString("logics.days.monday"), getString("logics.days.tuesday"), getString("logics.days.wednesday"), getString("logics.days.thursday"), getString("logics.days.friday"), getString("logics.days.saturday")},
                baseClass);
        formResult = addConcreteClass("FormResult", "Результат вызова формы",
                new String[]{"drop", "ok", "close"},
                new String[]{"Сбросить", "Принять", "Закрыть"},
                baseClass);

        // todo : раскидать по модулям
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

        addTable("month", month);
        addTable("dow", DOW);
    }

    @Override
    public void initProperties() throws ScriptingErrorLog.SemanticErrorException {

        dumb1 = dumb(1);

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

        string3 = addSProp("string3", 3, "");
        istring3 = addInsensitiveSProp("istring3", 3, "");

        ustring2CM = addSFUProp("ustring2CM", ",", 2);
        ustring2SP = addSFUProp("ustring2SP", " ", 2);
        ustring3SP = addSFUProp("ustring3SP", " ", 3);
        ustring4SP = addSFUProp("ustring4SP", " ", 4);
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

        subtractDateTimeSeconds = addSFProp("subtractDateTimeSeconds", "((prm1)-(prm2)*CAST('1 seconds' AS INTERVAL))", DateTimeClass.instance, 2);

        // Константы
        vtrue = addCProp(getString("logics.true"), LogicalClass.instance, true);
        vzero = addCProp("0", DoubleClass.instance, 0);
        vnull = addProperty(privateGroup, new LCP<PropertyInterface>(NullValueProperty.instance));

        // Обработка дат

        numberDOW = addJProp(baseGroup, "numberDOW", true, getString("logics.week.day.number"), subtractInteger,
                addOProp("numberDOWP1", getString("logics.week.day.number.plus.one"), PartitionType.SUM, addJProp(and1, addCProp(IntegerClass.instance, 1), is(DOW), 1), true, false, 0, 1), 1,
                addCProp(IntegerClass.instance, 1));
        DOWNumber = addAGProp("DOWNumber", getString("logics.week.day.id"), numberDOW);

        numberMonth = addOProp(baseGroup, "numberMonth", true, getString("logics.month.number"), addJProp(and1, addCProp(IntegerClass.instance, 1), is(month), 1), PartitionType.SUM, true, true, 0, 1);
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
        setDeleteActionOptions(delete);

        deleteApply = addListAProp("deleteApply", delete.property.caption, delete, 1, apply);
        setDeleteActionOptions(deleteApply);
        deleteApply.setAskConfirm(true);

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

        staticName = addDProp("staticName", getString("logics.statcode"), StringClass.get(250), baseClass);
        staticCaption = addDProp(recognizeGroup, "staticCaption", "Статическое имя", InsensitiveStringClass.get(100), baseClass);
        ((CalcProperty)staticCaption.property).aggProp = true;

        // todo : поменять возможно названия
        objectClass = addProperty(null, new LCP<ClassPropertyInterface>(baseClass.getObjectClassProperty()));
        objectClassName = addJProp(baseGroup, "objectClassName", getString("logics.object.class"), staticCaption, objectClass, 1);

/*        name.setEventChange(addJProp(string2SP, addJProp(name.getOld(), objectClass, 1), 1,
                addSFProp("CAST((prm1) as char(50))", StringClass.get(50), 1), 1), 1,
                is(named), 1);*/

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


    @Override
    @IdentityStrongLazy // для ID
    public LCP is(ValueClass valueClass) {
        return addProperty(null, new LCP<ClassPropertyInterface>(valueClass.getProperty()));
    }
    @Override
    @IdentityStrongLazy // для ID
    public LCP object(ValueClass valueClass) {
        return addJProp(valueClass.toString(), and1, 1, is(valueClass), 1);
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
