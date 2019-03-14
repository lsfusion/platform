package lsfusion.server.logics;

import com.google.common.collect.Iterables;
import lsfusion.interop.form.stat.report.FormPrintType;
import lsfusion.base.BaseUtils;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.interop.form.property.ClassViewType;
import lsfusion.interop.form.property.Compare;
import lsfusion.interop.form.event.KeyStrokes;
import lsfusion.interop.form.WindowFormType;
import lsfusion.server.base.caches.IdentityStrongLazy;
import lsfusion.server.classes.*;
import lsfusion.server.logics.action.change.AddObjectActionProperty;
import lsfusion.server.logics.action.change.ChangeClassActionProperty;
import lsfusion.server.logics.action.change.SetActionProperty;
import lsfusion.server.logics.action.interactive.ConfirmActionProperty;
import lsfusion.server.logics.action.interactive.MessageActionProperty;
import lsfusion.server.logics.action.session.ApplyActionProperty;
import lsfusion.server.logics.action.session.CancelActionProperty;
import lsfusion.server.logics.action.session.NewSessionActionProperty;
import lsfusion.server.logics.action.session.changed.IncrementType;
import lsfusion.server.logics.classes.*;
import lsfusion.server.logics.classes.sets.ResolveClassSet;
import lsfusion.server.base.context.ThreadLocalContext;
import lsfusion.server.data.Union;
import lsfusion.server.data.expr.StringAggUnionProperty;
import lsfusion.server.data.expr.formula.*;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.query.PartitionType;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.event.Event;
import lsfusion.server.logics.event.PrevScope;
import lsfusion.server.logics.form.interactive.action.focus.FocusActionProperty;
import lsfusion.server.logics.form.interactive.action.input.InputActionProperty;
import lsfusion.server.logics.form.interactive.action.input.RequestActionProperty;
import lsfusion.server.logics.form.interactive.action.seek.SeekGroupObjectActionProperty;
import lsfusion.server.logics.form.interactive.action.seek.SeekObjectActionProperty;
import lsfusion.server.logics.form.open.interactive.FormInteractiveActionProperty;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.constraint.OutFormSelector;
import lsfusion.server.logics.form.interactive.GroupObjectProp;
import lsfusion.server.logics.form.interactive.ManageSessionType;
import lsfusion.server.logics.form.open.stat.PrintActionProperty;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.property.cases.CaseUnionProperty;
import lsfusion.server.logics.property.classes.*;
import lsfusion.server.logics.property.classes.data.*;
import lsfusion.server.logics.property.data.SessionDataProperty;
import lsfusion.server.logics.property.data.StoredDataProperty;
import lsfusion.server.logics.property.implement.*;
import lsfusion.server.logics.property.infer.ClassType;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.logics.property.oraction.PropertyInterfaceImplement;
import lsfusion.server.logics.property.set.*;
import lsfusion.server.logics.property.value.ValueProperty;
import lsfusion.server.physics.admin.drilldown.DrillDownFormEntity;
import lsfusion.server.logics.form.struct.filter.FilterEntity;
import lsfusion.server.logics.form.struct.filter.RegularFilterGroupEntity;
import lsfusion.server.logics.form.interactive.action.edit.FormSessionScope;
import lsfusion.server.logics.navigator.DefaultIcon;
import lsfusion.server.logics.navigator.NavigatorAction;
import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.navigator.NavigatorFolder;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.navigator.window.AbstractWindow;
import lsfusion.server.language.EvalActionProperty;
import lsfusion.server.language.LazyActionProperty;
import lsfusion.server.language.MetaCodeFragment;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.flow.*;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.interactive.UpdateType;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.physics.admin.monitor.SystemEventsLogicsModule;
import lsfusion.server.physics.dev.debug.ActionDelegationType;
import lsfusion.server.physics.dev.debug.ActionPropertyDebugger;
import lsfusion.server.physics.dev.debug.DebugInfo;
import lsfusion.server.physics.dev.debug.PropertyFollowsDebug;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.language.linear.LAP;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.language.linear.LP;
import lsfusion.server.base.version.GlobalVersion;
import lsfusion.server.base.version.LastVersion;
import lsfusion.server.base.version.NFLazy;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.form.stat.integration.FormIntegrationType;
import lsfusion.server.logics.form.stat.integration.IntegrationFormEntity;
import lsfusion.server.logics.form.open.stat.ExportActionProperty;
import lsfusion.server.logics.form.stat.integration.exporting.hierarchy.json.ExportJSONActionProperty;
import lsfusion.server.logics.form.stat.integration.exporting.hierarchy.xml.ExportXMLActionProperty;
import lsfusion.server.logics.form.stat.integration.exporting.plain.csv.ExportCSVActionProperty;
import lsfusion.server.logics.form.stat.integration.exporting.plain.dbf.ExportDBFActionProperty;
import lsfusion.server.logics.form.stat.integration.exporting.plain.table.ExportTableActionProperty;
import lsfusion.server.logics.form.stat.integration.exporting.plain.xls.ExportXLSActionProperty;
import lsfusion.server.logics.form.open.stat.ImportActionProperty;
import lsfusion.server.logics.form.stat.integration.importing.hierarchy.json.ImportJSONActionProperty;
import lsfusion.server.logics.form.stat.integration.importing.hierarchy.xml.ImportXMLActionProperty;
import lsfusion.server.logics.form.stat.integration.importing.plain.csv.ImportCSVActionProperty;
import lsfusion.server.logics.form.stat.integration.importing.plain.dbf.ImportDBFActionProperty;
import lsfusion.server.logics.form.stat.integration.importing.plain.table.ImportTableActionProperty;
import lsfusion.server.logics.form.stat.integration.importing.plain.xls.ImportXLSActionProperty;
import lsfusion.server.logics.action.flow.*;
import lsfusion.server.logics.property.cases.ActionCase;
import lsfusion.server.logics.property.cases.CalcCase;
import lsfusion.server.logics.property.derived.*;
import lsfusion.server.logics.form.struct.group.AbstractGroup;
import lsfusion.server.physics.dev.id.name.CanonicalNameUtils;
import lsfusion.server.physics.dev.id.name.ClassCanonicalNameUtils;
import lsfusion.server.physics.dev.id.name.PropertyCanonicalNameParser;
import lsfusion.server.physics.dev.id.name.PropertyCanonicalNameUtils;
import lsfusion.server.physics.dev.id.resolve.ResolveManager;
import lsfusion.server.physics.dev.id.resolve.ResolvingErrors;
import lsfusion.server.physics.dev.integration.internal.to.StringFormulaProperty;
import lsfusion.server.physics.exec.table.ImplementTable;
import lsfusion.server.logics.action.session.LocalNestedType;
import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

import static lsfusion.base.BaseUtils.add;
import static lsfusion.server.logics.property.PropertyUtils.*;
import static lsfusion.server.logics.property.derived.DerivedProperty.createAnd;
import static lsfusion.server.logics.property.derived.DerivedProperty.createStatic;

// modules logics in theory should be in dev.module.package but in this class it's more about logics, than about modularity
public abstract class LogicsModule {
    protected static final Logger logger = Logger.getLogger(LogicsModule.class);

    protected static final ActionPropertyDebugger debugger = ActionPropertyDebugger.getInstance();

    // после этого шага должны быть установлены name, namespace, requiredModules
    public abstract void initModuleDependencies() throws RecognitionException;

    public abstract void initMetaAndClasses() throws RecognitionException;

    public abstract void initTables() throws RecognitionException;

    public abstract void initMainLogic() throws FileNotFoundException, RecognitionException;

    public abstract void initIndexes() throws RecognitionException;

    public String getErrorsDescription() { return "";}

    public BaseLogicsModule baseLM;

    protected Map<String, List<LCP<?>>> namedProperties = new HashMap<>();
    protected Map<String, List<LAP<?>>> namedActions = new HashMap<>();
    
    protected final Map<String, AbstractGroup> groups = new HashMap<>();
    protected final Map<String, CustomClass> classes = new HashMap<>();
    protected final Map<String, AbstractWindow> windows = new HashMap<>();
    protected final Map<String, NavigatorElement> navigatorElements = new HashMap<>();
    protected final Map<String, FormEntity> namedForms = new HashMap<>();
    protected final Map<String, ImplementTable> tables = new HashMap<>();
    protected final Map<Pair<String, Integer>, MetaCodeFragment> metaCodeFragments = new HashMap<>();

    private final Set<FormEntity> unnamedForms = new HashSet<>();
    private final Map<LCP<?>, LocalPropertyData> locals = new HashMap<>();


    protected final Map<LP<?, ?>, List<ResolveClassSet>> propClasses = new HashMap<>();
    

    protected Map<String, List<LogicsModule>> namespaceToModules = new LinkedHashMap<>();
    
    protected ResolveManager resolveManager;
    
    protected LogicsModule() {
        resolveManager = new ResolveManager(this);        
    }

    public LogicsModule(String name, String namespace, LinkedHashSet<String> requiredNames) {
        this();
        this.name = name;
        this.namespace = namespace;
        this.requiredNames = requiredNames;
    }

    private String name;
    private String namespace;
    private LinkedHashSet<String> requiredNames;
    private List<String> namespacePriority;
    private boolean defaultNamespace;

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    protected String elementCanonicalName(String name) {
        return CanonicalNameUtils.createCanonicalName(getNamespace(), name);
    }
    
    public String getLogName(int moduleCount, int orderNum) {
        String result = "#" + orderNum + " of " + moduleCount + " " + name;
        if (order != null)
            result += " (actual: " + (order + 1) + ")";
        return result;
    }

    public Iterable<LCP<?>> getNamedProperties() {
        return Iterables.concat(namedProperties.values());
    }

    public Iterable<LAP<?>> getNamedActions() {
        return Iterables.concat(namedActions.values());
    }

    public Iterable<LCP<?>> getNamedProperties(String name) {
        return createEmptyIfNull(namedProperties.get(name));
    }

    public Iterable<LAP<?>> getNamedActions(String name) {
        return createEmptyIfNull(namedActions.get(name));
    }

    public Collection<CustomClass> getClasses() {
        return classes.values();
    }
    
    private <T extends LP<?, ?>> Iterable<T> createEmptyIfNull(Collection<T> col) {
        if (col == null) {
            return Collections.emptyList();
        } else {
            return col;
        }
    }
    
    protected void addModuleLP(LP<?, ?> lp) {
        String name = null;
        assert getNamespace().equals(lp.property.getNamespace());
        if (lp instanceof LAP) {
            name = ((LAP<?>)lp).property.getName();
            putLPToMap(namedActions, (LAP) lp, name);
        } else if (lp instanceof LCP) {
            name = ((LCP<?>)lp).property.getName();
            putLPToMap(namedProperties, (LCP)lp, name);
        }
        assert name != null;
    }

    private <T extends LP<?, ?>> void putLPToMap(Map<String, List<T>> moduleMap, T lp, String name) {
        if (!moduleMap.containsKey(name)) {
            moduleMap.put(name, new ArrayList<T>());
        }
        moduleMap.get(name).add(lp);
    }

    @NFLazy
    protected <P extends PropertyInterface, T extends LP<P, ?>> void makeActionOrPropertyPublic(T lp, String name, List<ResolveClassSet> signature) {
        lp.property.setCanonicalName(getNamespace(), name, signature, lp.listInterfaces, baseLM.getDBNamingPolicy());
        propClasses.put(lp, signature);
        addModuleLP(lp);
    }

    protected void makePropertyPublic(LCP<?> lp, String name, ResolveClassSet... signature) {
        makePropertyPublic(lp, name, Arrays.asList(signature));
    }
    
    protected void makeActionPublic(LAP<?> lp, String name, ResolveClassSet... signature) {
        makeActionPublic(lp, name, Arrays.asList(signature));
    }

    protected <P extends PropertyInterface> void makePropertyPublic(LCP<P> lp, String name, List<ResolveClassSet> signature) {
        makeActionOrPropertyPublic(lp, name, signature);
    }
    
    protected <P extends PropertyInterface> void makeActionPublic(LAP<P> lp, String name, List<ResolveClassSet> signature) {
        makeActionOrPropertyPublic(lp, name, signature);
    }

    public AbstractGroup getGroup(String name) {
        return groups.get(name);
    }

    protected void addGroup(AbstractGroup group) {
        assert !groups.containsKey(group.getName());
        groups.put(group.getName(), group);
    }

    public CustomClass getClass(String name) {
        return classes.get(name);
    }

    protected void addModuleClass(CustomClass valueClass) {
        assert !classes.containsKey(valueClass.getName());
        classes.put(valueClass.getName(), valueClass);
    }

    public ImplementTable getTable(String name) {
        return tables.get(name);
    }

    protected void addModuleTable(ImplementTable table) {
        // В классе Table есть метод getName(), который используется для других целей, в частности
        // в качестве имени таблицы в базе данных, поэтому пока приходится использовать отличный от
        // остальных элементов системы способ получения простого имени 
        String name = CanonicalNameUtils.getName(table.getCanonicalName());
        assert !tables.containsKey(name);
        tables.put(name, table);
    }

    protected <T extends AbstractWindow> T addWindow(T window) {
        assert !windows.containsKey(window.getName());
        windows.put(window.getName(), window);
        return window;
    }

    public AbstractWindow getWindow(String name) {
        return windows.get(name);
    }

    public MetaCodeFragment getMetaCodeFragment(String name, int paramCnt) {
        return metaCodeFragments.get(new Pair<>(name, paramCnt));
    }

    protected void addMetaCodeFragment(MetaCodeFragment fragment) {
        assert !metaCodeFragments.containsKey(new Pair<>(fragment.getName(), fragment.parameters.size()));
        metaCodeFragments.put(new Pair<>(fragment.getName(), fragment.parameters.size()), fragment);
    }

    protected void setBaseLogicsModule(BaseLogicsModule baseLM) {
        this.baseLM = baseLM;
    }

    public FunctionSet<Version> visible;
    public Integer order;
    public boolean temporary;
    private final Version version = new Version() {
        public boolean canSee(Version version) {
            assert !(version instanceof LastVersion);
            return version instanceof GlobalVersion || visible.contains(version);
        }

        public Integer getOrder() {
            return order;
        }

        public int compareTo(Version o) {
            return getOrder().compareTo(o.getOrder());
        }

        public boolean isTemporary() {
            return temporary;
        }
    };
    public Version getVersion() {
        return version;
    }
    
    protected AbstractGroup addAbstractGroup(String name, LocalizedString caption, AbstractGroup parent) {
        return addAbstractGroup(name, caption, parent, true);
    }

    protected AbstractGroup addAbstractGroup(String name, LocalizedString caption, AbstractGroup parent, boolean toCreateContainer) {
        AbstractGroup group = new AbstractGroup(elementCanonicalName(name), caption);
        Version version = getVersion();
        if (parent != null) {
            parent.add(group, version);
        } else {
            if (baseLM.privateGroup != null && !temporary)
                baseLM.privateGroup.add(group, version);
        }
        group.system = !toCreateContainer;
        addGroup(group);
        return group;
    }

    protected void storeCustomClass(CustomClass customClass) {
        addModuleClass(customClass);
    }

    protected BaseClass addBaseClass(String canonicalName, LocalizedString caption, String staticCanonicalName, LocalizedString staticCanonicalCaption) {
        BaseClass baseClass = new BaseClass(canonicalName, caption, staticCanonicalName, staticCanonicalCaption, getVersion());
        storeCustomClass(baseClass);
        storeCustomClass(baseClass.staticObjectClass);
        return baseClass;
    }

    protected static void printStaticObjectsChanges(String path, String staticName, List<String> sids) {
        try {
            PrintWriter w = new PrintWriter(new FileWriter(path, true));
            for (String sid : sids) {
                w.print("OBJECT " + sid + " -> " + staticName + "." + sid + "\n");
            }
            w.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected ConcreteCustomClass addConcreteClass(String name, LocalizedString caption, List<String> objNames, List<LocalizedString> objCaptions, ImList<CustomClass> parents) {
        if(!objNames.isEmpty())
            parents = parents.addList(getBaseClass().staticObjectClass);
        parents = checkEmptyParents(parents);
        ConcreteCustomClass customClass = new ConcreteCustomClass(elementCanonicalName(name), caption, getVersion(), parents);
        customClass.addStaticObjects(objNames, objCaptions, getVersion());
        storeCustomClass(customClass);
        return customClass;
    }

    private ImList<CustomClass> checkEmptyParents(ImList<CustomClass> parents) {
        if(parents.isEmpty())
            parents = ListFact.<CustomClass>singleton(getBaseClass());
        return parents;
    }

    protected AbstractCustomClass addAbstractClass(String name, LocalizedString caption, ImList<CustomClass> parents) {
        parents = checkEmptyParents(parents);
        AbstractCustomClass customClass = new AbstractCustomClass(elementCanonicalName(name), caption, getVersion(), parents);
        storeCustomClass(customClass);
        return customClass;
    }

    protected ImplementTable addTable(String name, boolean isFull, boolean isExplicit, ValueClass... classes) {
        String canonicalName = elementCanonicalName(name);
        ImplementTable table = baseLM.tableFactory.include(CanonicalNameUtils.toSID(canonicalName), getVersion(), classes);
        table.setCanonicalName(canonicalName);
        addModuleTable(table);
        
        if(isFull) {
            if(classes.length == 1)
                table.markedFull = true;
            else
                markFull(table, classes);
        } else
            table.markedExplicit = isExplicit;
        return table;
    }

    protected void markFull(ImplementTable table, ValueClass... classes) {
        // создаем IS
        ImList<ValueClass> listClasses = ListFact.toList(classes);
        CalcPropertyRevImplement<?, Integer> mapProperty = IsClassProperty.getProperty(listClasses.toIndexedMap()); // тут конечно стремновато из кэша брать, так как остальные гарантируют создание
        LCP<?> lcp = addJProp(mapProperty.createLP(ListFact.consecutiveList(listClasses.size(), 0)), ListFact.consecutiveList(listClasses.size()).toArray(new Integer[listClasses.size()]));
//        addProperty(null, lcp);

        // делаем public, persistent
        makePropertyPublic(lcp, PropertyCanonicalNameUtils.fullPropPrefix + table.getName(), ClassCanonicalNameUtils.getResolveList(classes));
        addPersistent(lcp, table);

        // помечаем fullField из помеченного свойства
        table.setFullField(lcp.property.field);
    }

    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////
    /// Properties
    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////

    // ------------------- DATA ----------------- //

    protected LCP addDProp(LocalizedString caption, ValueClass value, ValueClass... params) {
        StoredDataProperty dataProperty = new StoredDataProperty(caption, params, value);
        LCP lp = addProperty(null, new LCP<>(dataProperty));
        return lp;
    }

    // ------------------- Loggable ----------------- //

    protected <D extends PropertyInterface> LCP addDCProp(LocalizedString caption, int whereNum, LCP<D> derivedProp, Object... params) {
        Pair<ValueClass[], ValueClass> signature = getSignature(derivedProp, whereNum, params);

        // выполняем само создание свойства
        StoredDataProperty dataProperty = new StoredDataProperty(caption, signature.first, signature.second);
        LCP derDataProp = addProperty(null, new LCP<>(dataProperty));

        derDataProp.setEventChange(derivedProp, whereNum, params);
        return derDataProp;
    }

    protected <D extends PropertyInterface> LCP addLogProp(LocalizedString caption, int whereNum, LCP<D> derivedProp, Object... params) {
        Pair<ValueClass[], ValueClass> signature = getSignature(derivedProp, whereNum, params);

        // выполняем само создание свойства
        StoredDataProperty dataProperty = new StoredDataProperty(caption, signature.first, LogicalClass.instance);
        return addProperty(null, new LCP<>(dataProperty));
    }

    private <D extends PropertyInterface> Pair<ValueClass[], ValueClass> getSignature(LCP<D> derivedProp, int whereNum, Object[] params) {
        // придется создавать Join свойство чтобы считать его класс
        int dersize = getIntNum(params);
        ImOrderSet<JoinProperty.Interface> listInterfaces = JoinProperty.getInterfaces(dersize);

        final ImList<CalcPropertyInterfaceImplement<JoinProperty.Interface>> list = readCalcImplements(listInterfaces, params);

        assert whereNum == list.size() - 1; // один ON CHANGED, то есть union делать не надо (выполняется, так как только в addLProp работает)

        AndFormulaProperty andProperty = new AndFormulaProperty(list.size());
        ImMap<AndFormulaProperty.Interface, CalcPropertyInterfaceImplement<JoinProperty.Interface>> mapImplement =
                MapFact.<AndFormulaProperty.Interface, CalcPropertyInterfaceImplement<JoinProperty.Interface>>addExcl(
                        andProperty.andInterfaces.mapValues(new GetIndex<CalcPropertyInterfaceImplement<JoinProperty.Interface>>() {
                            public CalcPropertyInterfaceImplement<JoinProperty.Interface> getMapValue(int i) {
                                return list.get(i);
                            }
                        }), andProperty.objectInterface, mapCalcListImplement(derivedProp, listInterfaces));

        JoinProperty<AndFormulaProperty.Interface> joinProperty = new JoinProperty<>(LocalizedString.NONAME, listInterfaces,
                new CalcPropertyImplement<>(andProperty, mapImplement));
        LCP<JoinProperty.Interface> listProperty = new LCP<>(joinProperty, listInterfaces);

        // получаем классы
        ValueClass[] commonClasses = listProperty.getInterfaceClasses(ClassType.logPolicy); // есть и другие obsolete использования
        ValueClass valueClass = listProperty.property.getValueClass(ClassType.logPolicy);
        return new Pair<>(commonClasses, valueClass);
    }

    // ------------------- Scripted DATA ----------------- //

    protected LCP addSDProp(LocalizedString caption, boolean isLocalScope, ValueClass value, LocalNestedType nestedType, ValueClass... params) {
        return addSDProp(null, false, caption, isLocalScope, value, nestedType, params);
    }

    protected LCP addSDProp(AbstractGroup group, boolean persistent, LocalizedString caption, boolean isLocalScope, ValueClass value, LocalNestedType nestedType, ValueClass... params) {
        SessionDataProperty prop = new SessionDataProperty(caption, params, value);
        if (isLocalScope) {
            prop.setLocal(true);
        }
        prop.nestedType = nestedType;
        return addProperty(group, new LCP<>(prop));
    }

    // ------------------- Form actions ----------------- //


    protected LAP addFormAProp(LocalizedString caption, CustomClass cls, LAP action) {
        return addIfAProp(caption, is(cls), 1, action, 1); // по идее можно просто exec сделать, но на всякий случай
    }

    protected LAP addEditAProp(LocalizedString caption, CustomClass cls) {
        cls.markUsed(true);
        return addFormAProp(caption, cls, baseLM.getFormEdit());
    }

    protected LAP addDeleteAProp(LocalizedString caption, CustomClass cls) {
        return addFormAProp(caption, cls, baseLM.getFormDelete());
    }

    // loggable, security, drilldown
    public LAP addMFAProp(LocalizedString caption, FormEntity form, ImOrderSet<ObjectEntity> objectsToSet, boolean newSession) {
        return addMFAProp(null, caption, form, objectsToSet, newSession);
    }
    public LAP addMFAProp(AbstractGroup group, LocalizedString caption, FormEntity form, ImOrderSet<ObjectEntity> objectsToSet, boolean newSession) {
        LAP result = addIFAProp(caption, form, objectsToSet, true, WindowFormType.FLOAT, false);
        return addSessionScopeAProp(group, newSession ? FormSessionScope.NEWSESSION : FormSessionScope.OLDSESSION, result);
    }

    protected <O extends ObjectSelector> LAP addIFAProp(LocalizedString caption, FormSelector<O> form, ImOrderSet<O> objectsToSet, boolean syncType, WindowFormType windowType, boolean forbidDuplicate) {
        return addIFAProp(null, caption, form, objectsToSet, ListFact.toList(false, objectsToSet.size()), ManageSessionType.AUTO, FormEntity.DEFAULT_NOCANCEL, syncType, windowType, forbidDuplicate, false, false);
    }
    protected <O extends ObjectSelector> LAP addIFAProp(AbstractGroup group, LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls, ManageSessionType manageSession, Boolean noCancel, boolean syncType, WindowFormType windowType, boolean forbidDuplicate, boolean checkOnOk, boolean readonly) {
        return addIFAProp(group, caption, form, objectsToSet, nulls, ListFact.<O>EMPTY(), ListFact.<LCP>EMPTY(), ListFact.<Boolean>EMPTY(), manageSession, noCancel, ListFact.<O>EMPTY(), ListFact.<CalcProperty>EMPTY(), syncType, windowType, forbidDuplicate, checkOnOk, readonly);
    }
    protected <O extends ObjectSelector> LAP addIFAProp(AbstractGroup group, LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls, ImList<O> inputObjects, ImList<LCP> inputProps, ImList<Boolean> inputNulls, ManageSessionType manageSession, Boolean noCancel, ImList<O> contextObjects, ImList<CalcProperty> contextProperties, boolean syncType, WindowFormType windowType, boolean forbidDuplicate, boolean checkOnOk, boolean readonly) {
        return addProperty(group, new LAP<>(new FormInteractiveActionProperty<>(caption, form, objectsToSet, nulls, inputObjects, inputProps, inputNulls, contextObjects, contextProperties, manageSession, noCancel, syncType, windowType, forbidDuplicate, checkOnOk, readonly)));
    }
    protected <O extends ObjectSelector> LAP<?> addPFAProp(AbstractGroup group, LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls, CalcProperty printerProperty, LCP sheetNameProperty, FormPrintType staticType, boolean syncType, Integer selectTop, CalcProperty passwordProperty, LCP targetProp, boolean removeNullsAndDuplicates) {
        return addProperty(group, new LAP<>(new PrintActionProperty<>(caption, form, objectsToSet, nulls, staticType, syncType, selectTop, passwordProperty, sheetNameProperty, targetProp, printerProperty, baseLM.formPageCount, removeNullsAndDuplicates)));
    }
    protected <O extends ObjectSelector> LAP addEFAProp(AbstractGroup group, LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls, FormIntegrationType staticType, boolean noHeader, String separator, boolean noEscape, String charset, CalcProperty root, CalcProperty tag, LCP singleExportFile, ImMap<GroupObjectEntity, LCP> exportFiles) {
        ExportActionProperty<O> exportAction;
        switch(staticType) {
            case XML:
                exportAction = new ExportXMLActionProperty<O>(caption, form, objectsToSet, nulls, staticType, singleExportFile, charset, root, tag);
                break;
            case JSON:
                exportAction = new ExportJSONActionProperty<O>(caption, form, objectsToSet, nulls, staticType, singleExportFile, charset);
                break;
            case CSV:
                exportAction = new ExportCSVActionProperty<O>(caption, form, objectsToSet, nulls, staticType, exportFiles, charset, noHeader, separator, noEscape);
                break;
            case XLS:
                exportAction = new ExportXLSActionProperty<O>(caption, form, objectsToSet, nulls, staticType, exportFiles, charset, false, noHeader);
                break;
            case XLSX:
                exportAction = new ExportXLSActionProperty<O>(caption, form, objectsToSet, nulls, staticType, exportFiles, charset, true, noHeader);
                break;
            case DBF:
                exportAction = new ExportDBFActionProperty<O>(caption, form, objectsToSet, nulls, staticType, exportFiles, charset);
                break;
            case TABLE:
                exportAction = new ExportTableActionProperty<>(caption, form, objectsToSet, nulls, staticType, exportFiles, charset);
                break;
            default:
                throw new UnsupportedOperationException();                
        }
        return addProperty(group, new LAP<>(exportAction));
    }

    protected <O extends ObjectSelector> LAP addAutoImportFAProp(FormEntity formEntity, int paramsCount, ImOrderSet<GroupObjectEntity> groupFiles, boolean sheetAll, String separator, boolean noHeader, boolean noEscape, String charset, boolean hasWhere) {
        // getExtension(FILE(prm1))
        // FOR x = getExtension(prm1) DO {
        //    CASE EXCLUSIVE
        //          WHEN x = type.getExtension
        //              IMPORT type form...
        // }
        
        Object[] cases = new Object[0];
        boolean isPlain = !groupFiles.isEmpty();
        for(FormIntegrationType importType : FormIntegrationType.values()) 
            if(importType.isPlain() == isPlain) {
                cases = add(cases, add(new Object[] {addJProp(baseLM.equals2, 1, addCProp(StringClass.text, LocalizedString.create(importType.getExtension(), false))), paramsCount + 1 }, // WHEN x = type.getExtension()
                    directLI(addImportFAProp(importType, formEntity, paramsCount, groupFiles, sheetAll, separator, noHeader, noEscape, charset, hasWhere)))); // IMPORT type form...
            }        
        
        return addForAProp(LocalizedString.create("{logics.add}"), false, false, false, false, paramsCount, null, false, true, 0, false,
                add(add(getUParams(paramsCount), 
                        new Object[] {addJProp(baseLM.equals2, 1, baseLM.getExtension, 2), paramsCount + 1, 1}), // FOR x = getExtension(FILE(prm1))  
                        directLI(addCaseAProp(true, cases))));  // CASE EXCLUSIVE
    }
    
    protected <O extends ObjectSelector> LAP addImportFAProp(FormIntegrationType format, FormEntity formEntity, int paramsCount, ImOrderSet<GroupObjectEntity> groupFiles, boolean sheetAll, String separator, boolean noHeader, boolean noEscape, String charset, boolean hasWhere) {
        ImportActionProperty importAction;

        if(format == null)
            return addAutoImportFAProp(formEntity, paramsCount, groupFiles, sheetAll, separator, noHeader, noEscape, charset, hasWhere);

        switch (format) {
            // hierarchical
            case XML:
                importAction = new ImportXMLActionProperty(paramsCount, formEntity, charset);
                break;
            case JSON:
                importAction = new ImportJSONActionProperty(paramsCount, formEntity, charset);
                break;
            // plain
            case CSV:
                importAction = new ImportCSVActionProperty(paramsCount, groupFiles, formEntity, charset, noHeader, noEscape, separator);
                break;
            case DBF:
                importAction = new ImportDBFActionProperty(paramsCount, groupFiles, formEntity, charset, hasWhere);
                break;
            case XLS:
            case XLSX:
                importAction = new ImportXLSActionProperty(paramsCount, groupFiles, formEntity, charset, noHeader, sheetAll);
                break;
            case TABLE:
                importAction = new ImportTableActionProperty(paramsCount, groupFiles, formEntity, charset);
                break;
            default:
                throw new UnsupportedOperationException();
        }
        return addProperty(null, new LAP<>(importAction));
    }

    // ------------------- Change Class action ----------------- //

    protected LAP addChangeClassAProp(ConcreteObjectClass cls, int resInterfaces, int changeIndex, boolean extendedContext, boolean conditional, Object... params) {
        int innerIntCnt = resInterfaces + (extendedContext ? 1 : 0);
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(innerIntCnt);
        ImOrderSet<PropertyInterface> mappedInterfaces = extendedContext ? innerInterfaces.removeOrderIncl(innerInterfaces.get(changeIndex)) : innerInterfaces;
        ImList<CalcPropertyInterfaceImplement<PropertyInterface>> readImplements = readCalcImplements(innerInterfaces, params);
        CalcPropertyMapImplement<PropertyInterface, PropertyInterface> conditionalPart = (CalcPropertyMapImplement<PropertyInterface, PropertyInterface>)
                (conditional ? readImplements.get(resInterfaces) : null);

        return addAProp(new ChangeClassActionProperty<>(cls, false, innerInterfaces.getSet(),
                mappedInterfaces, innerInterfaces.get(changeIndex), conditionalPart, getBaseClass()));
    }

    // ------------------- Export property action ----------------- //
    protected LAP addExportPropertyAProp(LocalizedString caption, FormIntegrationType type, int resInterfaces, List<String> aliases, List<Boolean> literals, ImOrderMap<String, Boolean> orders,
                                         LCP singleExportFile, boolean conditional, CalcProperty root, CalcProperty tag, String separator,
                                         boolean noHeader, boolean noEscape, String charset, boolean attr, Object... params) throws FormEntity.AlreadyDefined {
        int extraParamsCount = (root != null ? 1 : 0) + (tag != null ? 1 : 0);
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(getIntNum(params));
        ImList<CalcPropertyInterfaceImplement<PropertyInterface>> readImplements = readCalcImplements(innerInterfaces, params);
        final ImList<CalcPropertyInterfaceImplement<PropertyInterface>> exprs = readImplements.subList(resInterfaces, readImplements.size() - (conditional ? 1 : 0) - extraParamsCount);
        ImOrderSet<PropertyInterface> mapInterfaces = BaseUtils.immutableCast(readImplements.subList(0, resInterfaces).toOrderExclSet());
        
        // determining where
        CalcPropertyInterfaceImplement<PropertyInterface> where = conditional ? readImplements.get(readImplements.size() - 1 - extraParamsCount) : null;
        where = DerivedProperty.getFullWhereProperty(innerInterfaces.getSet(), mapInterfaces.getSet(), where, exprs.getCol());

        // creating form
        IntegrationFormEntity<PropertyInterface> form = new IntegrationFormEntity<>(baseLM, innerInterfaces, null, mapInterfaces, aliases, literals, exprs, where, orders, attr, version);
        ImOrderSet<ObjectEntity> objectsToSet = mapInterfaces.mapOrder(form.mapObjects);
        ImList<Boolean> nulls = ListFact.toList(true, mapInterfaces.size());
        
        ImMap<GroupObjectEntity, LCP> exportFiles = MapFact.EMPTY();
        if(type.isPlain()) {
            exportFiles = MapFact.singleton(form.groupObject == null ? GroupObjectEntity.NULL : form.groupObject, singleExportFile);
            singleExportFile = null;
        }            
                
        // creating action
        return addEFAProp(null, caption, form, objectsToSet, nulls, type, noHeader, separator, noEscape, charset, root, tag, singleExportFile, exportFiles);
    }

    protected LAP addImportPropertyAProp(FormIntegrationType type, int paramsCount, List<String> aliases, List<Boolean> literals, ImList<ValueClass> paramClasses, LCP<?> whereLCP, String separator, boolean noHeader, boolean noEscape, String charset, boolean sheetAll, boolean attr, boolean hasWhere, Object... params) throws FormEntity.AlreadyDefined {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(getIntNum(params));
        ImList<CalcPropertyInterfaceImplement<PropertyInterface>> exprs = readCalcImplements(innerInterfaces, params);

        // determining where
        CalcPropertyInterfaceImplement<PropertyInterface> where = innerInterfaces.size() == 1 && whereLCP != null ? whereLCP.getImplement(innerInterfaces.single()) : null;

        // creating form
        IntegrationFormEntity<PropertyInterface> form = new IntegrationFormEntity<>(baseLM, innerInterfaces, paramClasses, SetFact.<PropertyInterface>EMPTYORDER(), aliases, literals, exprs, where, MapFact.<String, Boolean>EMPTYORDER(), attr, version);
        
        // create action
        return addImportFAProp(type, form, paramsCount, SetFact.singletonOrder(form.groupObject), sheetAll, separator, noHeader, noEscape, charset, hasWhere);
    }

    // ------------------- Set property action ----------------- //

    protected <C extends PropertyInterface, W extends PropertyInterface> LAP addSetPropertyAProp(AbstractGroup group, LocalizedString caption, int resInterfaces,
                                                                                                 boolean conditional, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(getIntNum(params));
        ImList<CalcPropertyInterfaceImplement<PropertyInterface>> readImplements = readCalcImplements(innerInterfaces, params);
        CalcPropertyMapImplement<W, PropertyInterface> conditionalPart = (CalcPropertyMapImplement<W, PropertyInterface>)
                (conditional ? readImplements.get(resInterfaces + 2) : DerivedProperty.createTrue());
        return addProperty(group, new LAP<>(new SetActionProperty<C, W, PropertyInterface>(caption,
                innerInterfaces.getSet(), (ImOrderSet) readImplements.subList(0, resInterfaces).toOrderExclSet(), conditionalPart,
                (CalcPropertyMapImplement<C, PropertyInterface>) readImplements.get(resInterfaces), readImplements.get(resInterfaces + 1))));
    }

    // ------------------- List action ----------------- //

    protected LAP addListAProp(Object... params) {
        return addListAProp(SetFact.<SessionDataProperty>EMPTY(), params);
    }
    protected LAP addListAProp(ImSet<SessionDataProperty> localsInScope, Object... params) {
        return addListAProp(null, 0, LocalizedString.NONAME, localsInScope, params);
    }
    protected LAP addListAProp(int removeLast, Object... params) {
        return addListAProp(null, removeLast, LocalizedString.NONAME, SetFact.<SessionDataProperty>EMPTY(), params);
    }
    protected LAP addListAProp(LocalizedString caption, Object... params) {
        return addListAProp(null, 0, caption, SetFact.<SessionDataProperty>EMPTY(), params);        
    }
    protected LAP addListAProp(AbstractGroup group, int removeLast, LocalizedString caption, ImSet<SessionDataProperty> localsInScope, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        return addProperty(group, new LAP<>(new ListActionProperty(caption, listInterfaces,
                readActionImplements(listInterfaces, removeLast > 0 ? Arrays.copyOf(params, params.length - removeLast) : params), localsInScope)));
    }

    protected LAP addAbstractListAProp(boolean isChecked, boolean isLast, ValueClass[] params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(params.length);
        return addProperty(null, new LAP<>(new ListActionProperty(LocalizedString.NONAME, isChecked, isLast, listInterfaces, listInterfaces.mapList(ListFact.toList(params)))));
    }

    // ------------------- Try action ----------------- //

    protected LAP addTryAProp(AbstractGroup group, LocalizedString caption, boolean hasCatch, boolean hasFinally, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<PropertyInterfaceImplement<PropertyInterface>> readImplements = readImplements(listInterfaces, params);
        assert readImplements.size() >= 1 && readImplements.size() <= 3;

        ActionPropertyMapImplement<?, PropertyInterface> tryAction = (ActionPropertyMapImplement<?, PropertyInterface>) readImplements.get(0);
        ActionPropertyMapImplement<?, PropertyInterface> catchAction = (ActionPropertyMapImplement<?, PropertyInterface>) (hasCatch ? readImplements.get(1) : null);
        ActionPropertyMapImplement<?, PropertyInterface> finallyAction = (ActionPropertyMapImplement<?, PropertyInterface>) (hasFinally ? (readImplements.get(hasCatch ? 2 : 1)) : null);
        return addProperty(group, new LAP<>(new TryActionProperty(caption, listInterfaces, tryAction, catchAction, finallyAction)));
    }
    
    // ------------------- If action ----------------- //

    protected LAP addIfAProp(Object... params) {
        return addIfAProp(null, LocalizedString.NONAME, false, params);
    }

    protected LAP addIfAProp(LocalizedString caption, Object... params) {
        return addIfAProp(null, caption, false, params);
    }

    protected LAP addIfAProp(AbstractGroup group, LocalizedString caption, boolean not, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<PropertyInterfaceImplement<PropertyInterface>> readImplements = readImplements(listInterfaces, params);
        assert readImplements.size() >= 2 && readImplements.size() <= 3;

        return addProperty(group, new LAP(CaseActionProperty.createIf(caption, not, listInterfaces, (CalcPropertyInterfaceImplement<PropertyInterface>) readImplements.get(0),
                (ActionPropertyMapImplement<?, PropertyInterface>) readImplements.get(1), readImplements.size() == 3 ? (ActionPropertyMapImplement<?, PropertyInterface>) readImplements.get(2) : null)));
    }

    // ------------------- Case action ----------------- //

    protected LAP addCaseAProp(boolean isExclusive, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<PropertyInterfaceImplement<PropertyInterface>> readImplements = readImplements(listInterfaces, params);

        MList<ActionCase<PropertyInterface>> mCases = ListFact.mList();
        for (int i = 0; i*2+1 < readImplements.size(); i++) {
            mCases.add(new ActionCase<>((CalcPropertyMapImplement<?, PropertyInterface>) readImplements.get(i*2), (ActionPropertyMapImplement<?, PropertyInterface>) readImplements.get(i*2+1)));
        }
        if(readImplements.size() % 2 != 0) {
            mCases.add(new ActionCase<>(DerivedProperty.createTrue(), (ActionPropertyMapImplement<?, PropertyInterface>) readImplements.get(readImplements.size() - 1)));
        }
        return addProperty(null, new LAP<>(new CaseActionProperty(LocalizedString.NONAME, isExclusive, listInterfaces, mCases.immutableList())));
    }

    protected LAP addMultiAProp(boolean isExclusive, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<PropertyInterfaceImplement<PropertyInterface>> readImplements = readImplements(listInterfaces, params);

        MList<ActionPropertyMapImplement> mCases = ListFact.mList();
        for (int i = 0; i < readImplements.size(); i++) {
            mCases.add((ActionPropertyMapImplement) readImplements.get(i));
        }
        return addProperty(null, new LAP<>(new CaseActionProperty(LocalizedString.NONAME, isExclusive, mCases.immutableList(), listInterfaces)));
    }

    protected LAP addAbstractCaseAProp(ListCaseActionProperty.AbstractType type, boolean isExclusive, boolean isChecked, boolean isLast, ValueClass[] params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(params.length);
        return addProperty(null, new LAP<>(new CaseActionProperty(LocalizedString.NONAME, isExclusive, isChecked, isLast, type, listInterfaces, listInterfaces.mapList(ListFact.toList(params)))));
    }

    // ------------------- For action ----------------- //

    protected LAP addForAProp(LocalizedString caption, boolean ascending, boolean ordersNotNull, boolean recursive, boolean hasElse, int resInterfaces, CustomClass addClass, boolean autoSet, boolean hasCondition, int noInline, boolean forceInline, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(getIntNum(params));
        ImList<PropertyInterfaceImplement<PropertyInterface>> readImplements = readImplements(innerInterfaces, params);

        int implCnt = readImplements.size();

        ImOrderSet<PropertyInterface> mapInterfaces = BaseUtils.immutableCast(readImplements.subList(0, resInterfaces).toOrderExclSet());

        CalcPropertyMapImplement<?, PropertyInterface> ifProp = hasCondition? (CalcPropertyMapImplement<?, PropertyInterface>) readImplements.get(resInterfaces) : null;

        ImOrderMap<CalcPropertyInterfaceImplement<PropertyInterface>, Boolean> orders =
                BaseUtils.<ImList<CalcPropertyInterfaceImplement<PropertyInterface>>>immutableCast(readImplements.subList(resInterfaces + (hasCondition ? 1 : 0), implCnt - (hasElse ? 2 : 1) - (addClass != null ? 1: 0) - noInline)).toOrderExclSet().toOrderMap(!ascending);

        PropertyInterface addedInterface = addClass!=null ? (PropertyInterface) readImplements.get(implCnt - (hasElse ? 3 : 2) - noInline) : null;

        ActionPropertyMapImplement<?, PropertyInterface> elseAction =
                !hasElse ? null : (ActionPropertyMapImplement<?, PropertyInterface>) readImplements.get(implCnt - 2 - noInline);

        ActionPropertyMapImplement<?, PropertyInterface> action =
                (ActionPropertyMapImplement<?, PropertyInterface>) readImplements.get(implCnt - 1 - noInline);

        ImSet<PropertyInterface> noInlineInterfaces = BaseUtils.<ImList<PropertyInterface>>immutableCast(readImplements.subList(implCnt - noInline, implCnt)).toOrderExclSet().getSet();

        return addProperty(null, new LAP<>(
                new ForActionProperty<>(caption, innerInterfaces.getSet(), mapInterfaces, ifProp, orders, ordersNotNull, action, elseAction, addedInterface, addClass, autoSet, recursive, noInlineInterfaces, forceInline))
        );
    }

    // ------------------- JOIN ----------------- //

    public LAP addJoinAProp(LAP action, Object... params) {
        return addJoinAProp(LocalizedString.NONAME, action, params);
    }

    protected LAP addJoinAProp(LocalizedString caption, LAP action, Object... params) {
        return addJoinAProp(null, caption, action, params);
    }

    protected LAP addJoinAProp(AbstractGroup group, LocalizedString caption, LAP action, Object... params) {
        return addJoinAProp(group, caption, null, action, params);
    }

    protected LAP addJoinAProp(AbstractGroup group, LocalizedString caption, ValueClass[] classes, LAP action, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<CalcPropertyInterfaceImplement<PropertyInterface>> readImplements = readCalcImplements(listInterfaces, params);
        return addProperty(group, new LAP(new JoinActionProperty(caption, listInterfaces, mapActionImplement(action, readImplements))));
    }

    // ------------------------ APPLY / CANCEL ----------------- //

    protected LAP addApplyAProp(AbstractGroup group, LocalizedString caption, LAP action, boolean singleApply,
                                FunctionSet<SessionDataProperty> keepSessionProps, boolean serializable) {
        
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(action.listInterfaces.size());
        ActionPropertyMapImplement<?, PropertyInterface> actionImplement = mapActionListImplement(action, listInterfaces);

        ApplyActionProperty applyAction = new ApplyActionProperty(baseLM, actionImplement, caption, listInterfaces, keepSessionProps, serializable);
        actionImplement.property.singleApply = singleApply;
        return addProperty(group, new LAP<>(applyAction));
    }

    protected LAP addCancelAProp(AbstractGroup group, LocalizedString caption, FunctionSet<SessionDataProperty> keepSessionProps) {

        CancelActionProperty applyAction = new CancelActionProperty(caption, keepSessionProps);
        return addProperty(group, new LAP<>(applyAction));
    }

    // ------------------- SESSION SCOPE ----------------- //

    protected LAP addSessionScopeAProp(FormSessionScope sessionScope, LAP action) {
        return addSessionScopeAProp(null, sessionScope, action);
    }
    protected LAP addSessionScopeAProp(AbstractGroup group, FormSessionScope sessionScope, LAP action) {
        return addSessionScopeAProp(group, sessionScope, action, SetFact.<LCP>EMPTY());
    }
    protected LAP addSessionScopeAProp(FormSessionScope sessionScope, LAP action, ImCol<LCP> nestedProps) {
        return addSessionScopeAProp(null, sessionScope, action, nestedProps);
    }
    protected LAP addSessionScopeAProp(AbstractGroup group, FormSessionScope sessionScope, LAP action, ImCol<LCP> nestedProps) {
        if(sessionScope.isNewSession()) {
            action = addNewSessionAProp(null, action, sessionScope.isNestedSession(), false, false, nestedProps.mapMergeSetValues(new GetValue<SessionDataProperty, LCP>() {
                public SessionDataProperty getMapValue(LCP value) {
                    return (SessionDataProperty) value.property;
                }
            }));
        }
        return action;
    }

    // ------------------- NEWSESSION ----------------- //

    protected LAP addNewSessionAProp(AbstractGroup group,
                                     LAP action, boolean isNested, boolean singleApply, boolean newSQL,
                                     FunctionSet<SessionDataProperty> migrateSessionProps) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(action.listInterfaces.size());
        ActionPropertyMapImplement<?, PropertyInterface> actionImplement = mapActionListImplement(action, listInterfaces);

        NewSessionActionProperty actionProperty = new NewSessionActionProperty(
                LocalizedString.NONAME, listInterfaces, actionImplement, singleApply, newSQL, migrateSessionProps, isNested);
        
        actionProperty.drawOptions.inheritDrawOptions(action.property.drawOptions);
        actionProperty.inheritCaption(action.property);
        
        return addProperty(group, new LAP<>(actionProperty));
    }

    protected LAP addNewThreadAProp(AbstractGroup group, LocalizedString caption, boolean withConnection, boolean hasPeriod, boolean hasDelay, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<PropertyInterfaceImplement<PropertyInterface>> readImplements = readImplements(listInterfaces, params);
        CalcPropertyInterfaceImplement connection = withConnection ? (CalcPropertyInterfaceImplement) readImplements.get(1) : null;
        CalcPropertyInterfaceImplement period = hasPeriod ? (CalcPropertyInterfaceImplement) readImplements.get(1) : null;
        CalcPropertyInterfaceImplement delay = hasDelay ? (CalcPropertyInterfaceImplement) readImplements.get(hasPeriod ? 2 : 1) : null;
        return addProperty(group, new LAP(new NewThreadActionProperty(caption, listInterfaces, (ActionPropertyMapImplement) readImplements.get(0), period, delay, connection)));
    }

    protected LAP addNewExecutorAProp(AbstractGroup group, LocalizedString caption, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<PropertyInterfaceImplement<PropertyInterface>> readImplements = readImplements(listInterfaces, params);
        return addProperty(group, new LAP(new NewExecutorActionProperty(caption, listInterfaces,
                (ActionPropertyMapImplement) readImplements.get(0), (CalcPropertyInterfaceImplement) readImplements.get(1))));
    }

    // ------------------- Request action ----------------- //

    protected LAP addRequestAProp(AbstractGroup group, LocalizedString caption, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<PropertyInterfaceImplement<PropertyInterface>> readImplements = readImplements(listInterfaces, params);
        assert readImplements.size() >= 2;

        ActionPropertyMapImplement<?, PropertyInterface> elseAction =  readImplements.size() == 3 ? (ActionPropertyMapImplement<?, PropertyInterface>) readImplements.get(2) : null;
        return addProperty(group, new LAP(new RequestActionProperty(caption, listInterfaces, 
                (ActionPropertyMapImplement<?, PropertyInterface>) readImplements.get(0), (ActionPropertyMapImplement<?, PropertyInterface>) readImplements.get(1),
                elseAction))
        );
    }

    // ------------------- Input ----------------- //

    protected LP addInputAProp(AbstractGroup group, LocalizedString caption, DataClass dataClass, LCP<?> targetProp, Object... params) {
        return addJoinAProp(group, caption, addInputAProp(dataClass, targetProp != null ? targetProp.property : null), params);
    }
    @IdentityStrongLazy
    protected LAP addInputAProp(DataClass dataClass, CalcProperty targetProp) { // так как у LCP нет 
        return addProperty(null, new LAP(new InputActionProperty(LocalizedString.create("Input"), dataClass, targetProp != null ? new LCP(targetProp) : null)));
    }

    // ------------------- Constant ----------------- //

    protected <T extends PropertyInterface> LCP addUnsafeCProp(DataClass valueClass, Object value) {
        ValueProperty.checkLocalizedString(value, valueClass);
        return baseLM.addCProp(valueClass, valueClass instanceof StringClass ? value : valueClass.read(value));
    }

    protected <T extends PropertyInterface> LCP addCProp(StaticClass valueClass, Object value) {
        return baseLM.addCProp(valueClass, value);
    }

    // ------------------- Random ----------------- //

    protected LCP addRMProp(LocalizedString caption) {
        return addProperty(null, new LCP<>(new RandomFormulaProperty(caption)));
    }

    // ------------------- FORMULA ----------------- //

    protected LCP addSFProp(String formula, int paramCount) {
        return addSFProp(formula, null, paramCount);
    }

    protected LCP addSFProp(CustomFormulaSyntax formula, int paramCount, boolean hasNotNull) {
        return addSFProp(formula, null, paramCount, hasNotNull);
    }

    protected LCP addSFProp(String formula, DataClass value, int paramCount) {
        return addSFProp(new CustomFormulaSyntax(formula), value, paramCount, false);
    }
    
    protected LCP addSFProp(CustomFormulaSyntax formula, DataClass value, int paramCount, boolean hasNotNull) {
        return addProperty(null, new LCP<>(new StringFormulaProperty(value, formula, paramCount, hasNotNull)));
    }

    // ------------------- Операции сравнения ----------------- //

    protected LCP addCFProp(Compare compare) {
        return addProperty(null, new LCP<>(new CompareFormulaProperty(compare)));
    }

    // ------------------- Алгебраические операции ----------------- //

    protected LCP addSumProp() {
        return addProperty(null, new LCP<>(new FormulaImplProperty(LocalizedString.create("sum"), 2, new SumFormulaImpl())));
    }

    protected LCP addMultProp() {
        return addProperty(null, new LCP<>(new FormulaImplProperty(LocalizedString.create("multiply"), 2, new MultiplyFormulaImpl())));
    }

    protected LCP addSubtractProp() {
        return addProperty(null, new LCP<>(new FormulaImplProperty(LocalizedString.create("subtract"), 2, new SubtractFormulaImpl())));
    }

    protected LCP addDivideProp() {
        return addProperty(null, new LCP<>(new FormulaImplProperty(LocalizedString.create("divide"), 2, new DivideFormulaImpl())));
    }

    // ------------------- cast ----------------- //

    protected <P extends PropertyInterface> LCP addCastProp(DataClass castClass) {
        return baseLM.addCastProp(castClass);
    }

    // ------------------- Операции со строками ----------------- //

    protected <P extends PropertyInterface> LCP addSProp(int intNum) {
        return addSProp(intNum, " ");
    }

    protected <P extends PropertyInterface> LCP addSProp(int intNum, String separator) {
        return addProperty(null, new LCP<>(new StringConcatenateProperty(LocalizedString.create("{logics.join}"), intNum, separator)));
    }

    protected <P extends PropertyInterface> LCP addInsensitiveSProp(int intNum) {
        return addInsensitiveSProp(intNum, " ");
    }

    protected <P extends PropertyInterface> LCP addInsensitiveSProp(int intNum, String separator) {
        return addProperty(null, new LCP<>(new StringConcatenateProperty(LocalizedString.create("{logics.join}"), intNum, separator, true)));
    }

    // ------------------- AND ----------------- //

    protected LCP addAFProp(boolean... nots) {
        return addAFProp(null, nots);
    }

    protected LCP addAFProp(AbstractGroup group, boolean... nots) {
        ImOrderSet<PropertyInterface> interfaces = genInterfaces(nots.length + 1);
        MList<Boolean> mList = ListFact.mList(nots.length);
        boolean wasNot = false;
        for(boolean not : nots) {
            mList.add(not);
            wasNot = wasNot || not;
        }
        if(wasNot)
            return mapLProp(group, false, DerivedProperty.createAnd(interfaces, mList.immutableList()), interfaces);
        else
            return addProperty(group, new LCP<>(new AndFormulaProperty(nots.length)));
    }

    // ------------------- concat ----------------- //

    protected LCP addCCProp(int paramCount) {
        return addProperty(null, new LCP<>(new ConcatenateProperty(paramCount)));
    }

    protected LCP addDCCProp(int paramIndex) {
        return addProperty(null, new LCP<>(new DeconcatenateProperty(paramIndex, baseLM.baseClass)));
    }

    // ------------------- JOIN (продолжение) ----------------- //

    public LCP addJProp(LCP mainProp, Object... params) {
        return addJProp( false, mainProp, params);
    }

    protected LCP addJProp(boolean user, LCP mainProp, Object... params) {
        return addJProp(user, 0, mainProp, params);
    }
    protected LCP addJProp(boolean user, int removeLast, LCP<?> mainProp, Object... params) {

        ImOrderSet<JoinProperty.Interface> listInterfaces = JoinProperty.getInterfaces(getIntNum(params));
        ImList<CalcPropertyInterfaceImplement<JoinProperty.Interface>> listImplements = readCalcImplements(listInterfaces, removeLast > 0 ? Arrays.copyOf(params, params.length - removeLast) : params);
        JoinProperty<?> property = new JoinProperty(LocalizedString.NONAME, listInterfaces, user,
                mapCalcImplement(mainProp, listImplements));

        for(CalcProperty andProp : mainProp.property.getAndProperties())
            property.drawOptions.inheritDrawOptions(andProp.drawOptions);

        return addProperty(null, new LCP<>(property, listInterfaces));
    }

    // ------------------- mapLProp ----------------- //

    private <P extends PropertyInterface, L extends PropertyInterface> LCP mapLProp(AbstractGroup group, boolean persistent, CalcPropertyMapImplement<L, P> implement, ImOrderSet<P> listInterfaces) {
        return addProperty(group, new LCP<>(implement.property, listInterfaces.mapOrder(implement.mapping.reverse())));
    }

    protected <P extends PropertyInterface, L extends PropertyInterface> LCP mapLProp(AbstractGroup group, boolean persistent, CalcPropertyMapImplement<L, P> implement, LCP<P> property) {
        return mapLProp(group, persistent, implement, property.listInterfaces);
    }

    private <P extends PropertyInterface, L extends PropertyInterface> LCP mapLGProp(AbstractGroup group, CalcPropertyImplement<L, CalcPropertyInterfaceImplement<P>> implement, ImList<CalcPropertyInterfaceImplement<P>> listImplements) {
        return mapLGProp(group, false, implement, listImplements);
    }

    private <P extends PropertyInterface, L extends PropertyInterface> LCP mapLGProp(AbstractGroup group, boolean persistent, CalcPropertyImplement<L, CalcPropertyInterfaceImplement<P>> implement, ImList<CalcPropertyInterfaceImplement<P>> listImplements) {
        return addProperty(group, new LCP<>(implement.property, listImplements.toOrderExclSet().mapOrder(implement.mapping.toRevExclMap().reverse())));
    }

    private <P extends PropertyInterface> LCP mapLGProp(AbstractGroup group, boolean persistent, GroupProperty property, ImList<CalcPropertyInterfaceImplement<P>> listImplements) {
        return mapLGProp(group, persistent, new CalcPropertyImplement<GroupProperty.Interface<P>, CalcPropertyInterfaceImplement<P>>(property, property.getMapInterfaces()), listImplements);
    }

    // ------------------- Order property ----------------- //

    protected <P extends PropertyInterface> LCP addOProp(AbstractGroup group, boolean persistent, LocalizedString caption, PartitionType partitionType, boolean ascending, boolean ordersNotNull, boolean includeLast, int partNum, Object... params) {
        ImOrderSet<PropertyInterface> interfaces = genInterfaces(getIntNum(params));
        ImList<CalcPropertyInterfaceImplement<PropertyInterface>> listImplements = readCalcImplements(interfaces, params);

        ImSet<CalcPropertyInterfaceImplement<PropertyInterface>> partitions = listImplements.subList(0, partNum).toOrderSet().getSet();
        ImList<CalcPropertyInterfaceImplement<PropertyInterface>> mainProp = listImplements.subList(partNum, partNum + 1);
        ImOrderMap<CalcPropertyInterfaceImplement<PropertyInterface>, Boolean> orders = listImplements.subList(partNum + 1, listImplements.size()).toOrderSet().toOrderMap(!ascending);

        return mapLProp(group, persistent, DerivedProperty.createOProp(caption, partitionType, interfaces.getSet(), mainProp, partitions, orders, ordersNotNull, includeLast), interfaces);
    }

    protected <P extends PropertyInterface> LCP addRProp(AbstractGroup group, boolean persistent, LocalizedString caption, Cycle cycle, ImList<Integer> resInterfaces, ImRevMap<Integer, Integer> mapPrev, Object... params) {
        int innerCount = getIntNum(params);
        final ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(innerCount);
        ImList<CalcPropertyInterfaceImplement<PropertyInterface>> listImplement = readCalcImplements(innerInterfaces, params);

        GetValue<PropertyInterface, Integer> getInnerInterface = new GetValue<PropertyInterface, Integer>() {
            public PropertyInterface getMapValue(Integer value) {
                return innerInterfaces.get(value);
            }
        };

        final ImOrderSet<RecursiveProperty.Interface> interfaces = RecursiveProperty.getInterfaces(resInterfaces.size());
        ImRevMap<RecursiveProperty.Interface, PropertyInterface> mapInterfaces = resInterfaces.mapListRevKeyValues(new GetIndex<RecursiveProperty.Interface>() {
            public RecursiveProperty.Interface getMapValue(int i) {
                return interfaces.get(i);
            }}, getInnerInterface);
        ImRevMap<PropertyInterface, PropertyInterface> mapIterate = mapPrev.mapRevKeyValues(getInnerInterface, getInnerInterface); // старые на новые

        CalcPropertyMapImplement<?, PropertyInterface> initial = (CalcPropertyMapImplement<?, PropertyInterface>) listImplement.get(0);
        CalcPropertyMapImplement<?, PropertyInterface> step = (CalcPropertyMapImplement<?, PropertyInterface>) listImplement.get(1);

        assert initial.property.getType() instanceof IntegralClass == (step.property.getType() instanceof IntegralClass);
        if(!(initial.property.getType() instanceof IntegralClass) && (cycle == Cycle.NO || (cycle==Cycle.IMPOSSIBLE && persistent))) {
            CalcPropertyMapImplement<?, PropertyInterface> one = createStatic(1L, LongClass.instance);
            initial = createAnd(innerInterfaces.getSet(), one, initial);
            step = createAnd(innerInterfaces.getSet(), one, step);
        }

        RecursiveProperty<PropertyInterface> property = new RecursiveProperty<>(caption, interfaces, cycle,
                mapInterfaces, mapIterate, initial, step);
        if(cycle==Cycle.NO)
            addConstraint(property.getConstrainedProperty(), false);

        LCP result = new LCP<>(property, interfaces);
//        if (convertToLogical)
//            return addJProp(group, name, false, caption, baseLM.notZero, directLI(addProperty(null, persistent, result)));
//        else
            return addProperty(group, result);
    }

    // ------------------- Ungroup property ----------------- //

    protected <L extends PropertyInterface> LCP addUGProp(AbstractGroup group, boolean persistent, boolean over, LocalizedString caption, int intCount, boolean ascending, boolean ordersNotNull, LCP<L> ungroup, Object... params) {
        int partNum = ungroup.listInterfaces.size();
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(intCount);
        final ImList<CalcPropertyInterfaceImplement<PropertyInterface>> listImplements = readCalcImplements(innerInterfaces, params);
        ImMap<L, CalcPropertyInterfaceImplement<PropertyInterface>> groupImplement = ungroup.listInterfaces.mapOrderValues(new GetIndex<CalcPropertyInterfaceImplement<PropertyInterface>>() {
            public CalcPropertyInterfaceImplement<PropertyInterface> getMapValue(int i) {
                return listImplements.get(i);
            }});
        CalcPropertyInterfaceImplement<PropertyInterface> restriction = listImplements.get(partNum);
        ImOrderMap<CalcPropertyInterfaceImplement<PropertyInterface>, Boolean> orders = listImplements.subList(partNum + 1, listImplements.size()).toOrderSet().toOrderMap(!ascending);

        return mapLProp(group, persistent, DerivedProperty.createUGProp(caption, innerInterfaces.getSet(),
                new CalcPropertyImplement<>(ungroup.property, groupImplement), orders, ordersNotNull, restriction, over), innerInterfaces);
    }

    protected <L extends PropertyInterface> LCP addPGProp(AbstractGroup group, boolean persistent, int roundlen, boolean roundfirst, LocalizedString caption, int intCount, List<ResolveClassSet> explicitInnerClasses, boolean ascending, boolean ordersNotNull, LCP<L> ungroup, Object... params) {
        int partNum = ungroup.listInterfaces.size();
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(intCount);
        final ImList<CalcPropertyInterfaceImplement<PropertyInterface>> listImplements = readCalcImplements(innerInterfaces, params);
        ImMap<L, CalcPropertyInterfaceImplement<PropertyInterface>> groupImplement = ungroup.listInterfaces.mapOrderValues(new GetIndex<CalcPropertyInterfaceImplement<PropertyInterface>>() {
            public CalcPropertyInterfaceImplement<PropertyInterface> getMapValue(int i) {
                return listImplements.get(i);
            }});
        CalcPropertyInterfaceImplement<PropertyInterface> proportion = listImplements.get(partNum);
        ImOrderMap<CalcPropertyInterfaceImplement<PropertyInterface>, Boolean> orders =
                listImplements.subList(partNum + 1, listImplements.size()).toOrderSet().toOrderMap(!ascending);

        return mapLProp(group, persistent, DerivedProperty.createPGProp(caption, roundlen, roundfirst, baseLM.baseClass, innerInterfaces, explicitInnerClasses,
                new CalcPropertyImplement<>(ungroup.property, groupImplement), proportion, orders, ordersNotNull), innerInterfaces);
    }

    /*
      // свойство обратное группируещему - для этого задается ограничивающее свойство, результирующее св-во с группировочными, порядковое св-во
      protected LF addUGProp(AbstractGroup group, String title, LF maxGroupProp, LF unGroupProp, Object... params) {
          List<LI> lParams = readLI(params);
          List<LI> lUnGroupParams = lParams.subList(0,unGroupProp.listInterfaces.size());
          List<LI> orderParams = lParams.subList(unGroupProp.listInterfaces.size(),lParams.size());

          int intNum = maxGroupProp.listInterfaces.size();

          // "двоим" интерфейсы, для результ. св-ва
          // ставим equals'ы на группировочные свойства (раздвоенные)
          List<Object[]> groupParams = new ArrayList<Object[]>();
          groupParams.add(directLI(maxGroupProp));
          for(LI li : lUnGroupParams)
              groupParams.add(li.compare(equals2, this, intNum));

          boolean[] andParams = new boolean[groupParams.size()-1];
          for(int i=0;i<andParams.length;i++)
              andParams[i] = false;
          LF groupPropSet = addJProp(addAFProp(andParams),BaseUtils.add(groupParams));

          for(int i=0;i<intNum;i++) { // докинем не достающие порядки
              boolean found = false;
              for(LI order : orderParams)
                  if(order instanceof LII && ((LII)order).intNum==i+1) {
                      found = true;
                      break;
                  }
              if(!found)
                  orderParams.add(new LII(i+1));
          }

          // ставим на предшествие сначала order'а, потом всех интерфейсов
          LF[] orderProps = new LF[orderParams.size()];
          for(int i=0;i<orderParams.size();i++) {
              orderProps[i] = (addJProp(and1, BaseUtils.add(directLI(groupPropSet),orderParams.get(i).compare(greater2, this, intNum))));
              groupPropSet = addJProp(and1, BaseUtils.add(directLI(groupPropSet),orderParams.get(i).compare(equals2, this, intNum)));
          }
          LF groupPropPrev = addSUProp(Union.OVERRIDE, orderProps);

          // группируем суммируя по "задвоенным" св-вам maxGroup
          Object[] remainParams = new Object[intNum];
          for(int i=1;i<=intNum;i++)
              remainParams[i-1] = i+intNum;
          LF remainPrev = addSGProp(groupPropPrev, remainParams);

          // создадим группировочное св-во с маппом на общий интерфейс, нужно поубирать "дырки"


          // возвращаем MIN2(unGroup-MU(prevGroup,0(maxGroup)),maxGroup) и не unGroup<=prevGroup
          LF zeroQuantity = addJProp(and1, BaseUtils.add(new Object[]{vzero},directLI(maxGroupProp)));
          LF zeroRemainPrev = addSUProp(Union.OVERRIDE , zeroQuantity, remainPrev);
          LF calc = addSFProp("prm3+prm1-prm2-GREATEST(prm3,prm1-prm2)",DoubleClass.instance,3);
          LF maxRestRemain = addJProp(calc, BaseUtils.add(BaseUtils.add(unGroupProp.write(),directLI(zeroRemainPrev)),directLI(maxGroupProp)));
          LF exceed = addJProp(groeq2, BaseUtils.add(directLI(remainPrev),unGroupProp.write()));
          return addJProp(group, title, andNot1, BaseUtils.add(directLI(maxRestRemain),directLI(exceed)));
      }
    */

    protected ImOrderSet<PropertyInterface> genInterfaces(int interfaces) {
        return SetFact.toOrderExclSet(interfaces, Property.genInterface);
    }

    // ------------------- GROUP SUM ----------------- //

    protected LCP addSGProp(AbstractGroup group, boolean persistent, boolean notZero, LocalizedString caption, int interfaces, List<ResolveClassSet> explicitInnerClasses, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(interfaces);
        return addSGProp(group, persistent, notZero, caption, innerInterfaces, explicitInnerClasses, readCalcImplements(innerInterfaces, params));
    }

    protected <T extends PropertyInterface> LCP addSGProp(AbstractGroup group, boolean persistent, boolean notZero, LocalizedString caption, ImOrderSet<T> innerInterfaces, List<ResolveClassSet> explicitInnerClasses, ImList<CalcPropertyInterfaceImplement<T>> implement) {
        ImList<CalcPropertyInterfaceImplement<T>> listImplements = implement.subList(1, implement.size());
        SumGroupProperty<T> property = new SumGroupProperty<>(caption, innerInterfaces.getSet(), listImplements, implement.get(0));
        property.setExplicitInnerClasses(innerInterfaces, explicitInnerClasses);

        return mapLGProp(group, persistent, property, listImplements);
    }

    // ------------------- Override property ----------------- //

    public <T extends PropertyInterface> LCP addOGProp(AbstractGroup group, boolean persist, LocalizedString caption, GroupType type, int numOrders, boolean ordersNotNull, boolean descending, int interfaces, List<ResolveClassSet> explicitInnerClasses, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(interfaces);
        return addOGProp(group, persist, caption, type, numOrders, ordersNotNull, descending, innerInterfaces, explicitInnerClasses, readCalcImplements(innerInterfaces, params));
    }
    public <T extends PropertyInterface> LCP addOGProp(AbstractGroup group, boolean persist, LocalizedString caption, GroupType type, int numOrders, boolean ordersNotNull, boolean descending, ImOrderSet<T> innerInterfaces, List<ResolveClassSet> explicitInnerClasses, ImList<CalcPropertyInterfaceImplement<T>> listImplements) {
        int numExprs = type.numExprs();
        ImList<CalcPropertyInterfaceImplement<T>> props = listImplements.subList(0, numExprs);
        ImOrderMap<CalcPropertyInterfaceImplement<T>, Boolean> orders = listImplements.subList(numExprs, numExprs + numOrders).toOrderSet().toOrderMap(descending);
        ImList<CalcPropertyInterfaceImplement<T>> groups = listImplements.subList(numExprs + numOrders, listImplements.size());
        OrderGroupProperty<T> property = new OrderGroupProperty<>(caption, innerInterfaces.getSet(), groups.getCol(), props, type, orders, ordersNotNull);
        property.setExplicitInnerClasses(innerInterfaces, explicitInnerClasses);

        return mapLGProp(group, persist, property, groups);
    }

    // ------------------- GROUP MAX ----------------- //

    protected LCP addMGProp(AbstractGroup group, boolean persist, LocalizedString caption, boolean min, int interfaces, List<ResolveClassSet> explicitInnerClasses, Object... params) {
        return addMGProp(group, persist, new LocalizedString[]{caption}, 1, min, interfaces, explicitInnerClasses, params)[0];
    }

    protected LCP[] addMGProp(AbstractGroup group, boolean persist, LocalizedString[] captions, int exprs, boolean min, int interfaces, List<ResolveClassSet> explicitInnerClasses, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(interfaces);
        return addMGProp(group, persist, captions, exprs, min, innerInterfaces, explicitInnerClasses, readCalcImplements(innerInterfaces, params));
    }

    protected <T extends PropertyInterface> LCP[] addMGProp(AbstractGroup group, boolean persist, LocalizedString[] captions, int exprs, boolean min, ImOrderSet<T> listInterfaces, List<ResolveClassSet> explicitInnerClasses, ImList<CalcPropertyInterfaceImplement<T>> listImplements) {
        LCP[] result = new LCP[exprs];

        MSet<CalcProperty> mOverridePersist = SetFact.mSet();

        ImList<CalcPropertyInterfaceImplement<T>> groupImplements = listImplements.subList(exprs, listImplements.size());
        ImList<CalcPropertyImplement<?, CalcPropertyInterfaceImplement<T>>> mgProps = DerivedProperty.createMGProp(captions, listInterfaces, explicitInnerClasses, baseLM.baseClass,
                listImplements.subList(0, exprs), groupImplements.getCol(), mOverridePersist, min);

        ImSet<CalcProperty> overridePersist = mOverridePersist.immutable();

        for (int i = 0; i < mgProps.size(); i++)
            result[i] = mapLGProp(group, mgProps.get(i), groupImplements);

        if (persist) {
            if (overridePersist.size() > 0) {
                for (CalcProperty property : overridePersist)
                    addProperty(null, new LCP(property));
            } else
                for (LCP lcp : result) addPersistent(lcp);
        }

        return result;
    }

    // ------------------- CGProperty ----------------- //

    protected <T extends PropertyInterface, P extends PropertyInterface> LCP addCGProp(AbstractGroup group, boolean checkChange, boolean persistent, LocalizedString caption, LCP<PropertyInterface> dataProp, int interfaces, List<ResolveClassSet> explicitInnerClasses, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(interfaces);
        return addCGProp(group, checkChange, persistent, caption, dataProp, innerInterfaces, explicitInnerClasses, readCalcImplements(innerInterfaces, params));
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LCP addCGProp(AbstractGroup group, boolean checkChange, boolean persistent, LocalizedString caption, LCP<P> dataProp, ImOrderSet<T> innerInterfaces, List<ResolveClassSet> explicitInnerClasses, ImList<CalcPropertyInterfaceImplement<T>> listImplements) {
        CycleGroupProperty<T, P> property = new CycleGroupProperty<>(caption, innerInterfaces.getSet(), listImplements.subList(1, listImplements.size()).getCol(), listImplements.get(0), dataProp == null ? null : dataProp.property);
        property.setExplicitInnerClasses(innerInterfaces, explicitInnerClasses);

        // нужно добавить ограничение на уникальность
        addConstraint(property.getConstrainedProperty(), checkChange);

        return mapLGProp(group, persistent, property, listImplements.subList(1, listImplements.size()));
    }

//    protected static <T extends PropertyInterface<T>> AggregateGroupProperty create(String sID, LocalizedString caption, CalcProperty<T> property, T aggrInterface, Collection<CalcPropertyMapImplement<?, T>> groupProps) {

    // ------------------- GROUP AGGR ----------------- //

    protected LCP addAGProp(AbstractGroup group, boolean checkChange, boolean persistent, LocalizedString caption, boolean noConstraint, int interfaces, List<ResolveClassSet> explicitInnerClasses, Object... props) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(interfaces);
        return addAGProp(group, checkChange, persistent, caption, noConstraint, innerInterfaces, explicitInnerClasses, readCalcImplements(innerInterfaces, props));
    }

    protected <T extends PropertyInterface<T>, I extends PropertyInterface> LCP addAGProp(AbstractGroup group, boolean checkChange, boolean persistent, LocalizedString caption, boolean noConstraint, ImOrderSet<T> innerInterfaces, List<ResolveClassSet> explicitInnerClasses, ImList<CalcPropertyInterfaceImplement<T>> listImplements) {
        T aggrInterface = (T) listImplements.get(0);
        CalcPropertyInterfaceImplement<T> whereProp = listImplements.get(1);
        ImList<CalcPropertyInterfaceImplement<T>> groupImplements = listImplements.subList(2, listImplements.size());

        AggregateGroupProperty<T> aggProp = AggregateGroupProperty.create(caption, innerInterfaces.getSet(), whereProp, aggrInterface, groupImplements.toOrderExclSet().getSet());
        aggProp.setExplicitInnerClasses(innerInterfaces, explicitInnerClasses);
        return addAGProp(group, checkChange, persistent, noConstraint, aggProp, groupImplements);
    }

    // чисто для generics
    private <T extends PropertyInterface<T>> LCP addAGProp(AbstractGroup group, boolean checkChange, boolean persistent, boolean noConstraint, AggregateGroupProperty<T> property, ImList<CalcPropertyInterfaceImplement<T>> listImplements) {
        // нужно добавить ограничение на уникальность
        if(!noConstraint)
            addConstraint(property.getConstrainedProperty(), checkChange);

        return mapLGProp(group, persistent, property, listImplements);
    }

    // ------------------- UNION ----------------- //

    protected LCP addUProp(AbstractGroup group, LocalizedString caption, Union unionType, String separator, int[] coeffs, Object... params) {
        return addUProp(group, false, caption, unionType, null, coeffs, params);
    }

    protected LCP addUProp(AbstractGroup group, boolean persistent, LocalizedString caption, Union unionType, String separator, int[] coeffs, Object... params) {

        assert (unionType==Union.SUM)==(coeffs!=null);
        assert (unionType==Union.STRING_AGG)==(separator !=null);

        int intNum = getIntNum(params);
        ImOrderSet<UnionProperty.Interface> listInterfaces = UnionProperty.getInterfaces(intNum);
        ImList<CalcPropertyInterfaceImplement<UnionProperty.Interface>> listOperands = readCalcImplements(listInterfaces, params);

        UnionProperty property = null;
        switch (unionType) {
            case MAX:
            case MIN:
                property = new MaxUnionProperty(unionType == Union.MIN, caption, listInterfaces, listOperands.getCol());
                break;
            case SUM:
                MMap<CalcPropertyInterfaceImplement<UnionProperty.Interface>, Integer> mMapOperands = MapFact.mMap(MapFact.<CalcPropertyInterfaceImplement<UnionProperty.Interface>>addLinear());
                for(int i=0;i<listOperands.size();i++)
                    mMapOperands.add(listOperands.get(i), coeffs[i]);
                property = new SumUnionProperty(caption, listInterfaces, mMapOperands.immutable());
                break;
            case OVERRIDE:
                property = new CaseUnionProperty(caption, listInterfaces, listOperands, false, false, false);
                break;
            case XOR:
                property = new XorUnionProperty(caption, listInterfaces, listOperands);
                break;
            case EXCLUSIVE:
                property = new CaseUnionProperty(caption, listInterfaces, listOperands.getCol(), false);
                break;
            case CLASS:
                property = new CaseUnionProperty(caption, listInterfaces, listOperands.getCol(), true);
                break;
            case CLASSOVERRIDE:
                property = new CaseUnionProperty(caption, listInterfaces, listOperands, true, false, false);
                break;
            case STRING_AGG:
                property = new StringAggUnionProperty(caption, listInterfaces, listOperands, separator);
                break;
        }

        return addProperty(group, new LCP<>(property, listInterfaces));
    }

    protected LCP addAUProp(AbstractGroup group, boolean persistent, boolean isExclusive, boolean isChecked, boolean isLast, CaseUnionProperty.Type type, LocalizedString caption, ValueClass valueClass, ValueClass... interfaces) {
        ImOrderSet<UnionProperty.Interface> listInterfaces = UnionProperty.getInterfaces(interfaces.length);
        return addProperty(group, new LCP<>(
                new CaseUnionProperty(isExclusive, isChecked, isLast, type, caption, listInterfaces, valueClass, listInterfaces.mapList(ListFact.toList(interfaces))), listInterfaces));
    }

    protected LCP addCaseUProp(AbstractGroup group, boolean persistent, LocalizedString caption, boolean isExclusive, Object... params) {
        ImOrderSet<UnionProperty.Interface> listInterfaces = UnionProperty.getInterfaces(getIntNum(params));
        MList<CalcCase<UnionProperty.Interface>> mListCases = ListFact.mList();
        ImList<CalcPropertyMapImplement<?,UnionProperty.Interface>> mapImplements = (ImList<CalcPropertyMapImplement<?, UnionProperty.Interface>>) (ImList<?>) readCalcImplements(listInterfaces, params);
        for (int i = 0; i < mapImplements.size() / 2; i++)
            mListCases.add(new CalcCase<>(mapImplements.get(2 * i), mapImplements.get(2 * i + 1)));
        if (mapImplements.size() % 2 != 0)
            mListCases.add(new CalcCase<>(new CalcPropertyMapImplement<PropertyInterface, UnionProperty.Interface>((CalcProperty<PropertyInterface>) baseLM.vtrue.property), mapImplements.get(mapImplements.size() - 1)));

        return addProperty(group, new LCP<>(new CaseUnionProperty(caption, listInterfaces, isExclusive, mListCases.immutableList()), listInterfaces));
    }

    public static List<ResolveClassSet> getSignatureForLogProperty(List<ResolveClassSet> basePropSignature, SystemEventsLogicsModule systemEventsLM) {
        List<ResolveClassSet> signature = new ArrayList<>(basePropSignature);
        signature.add(systemEventsLM.currentSession.property.getValueClass(ClassType.aroundPolicy).getResolveSet());
        return signature;
    }
    
    public static List<ResolveClassSet> getSignatureForLogProperty(LCP lp, SystemEventsLogicsModule systemEventsLM) {
        List<ResolveClassSet> signature = new ArrayList<>();
        for (ValueClass cls : lp.getInterfaceClasses(ClassType.logPolicy)) {
            signature.add(cls.getResolveSet());
        }
        return getSignatureForLogProperty(signature, systemEventsLM);    
    } 

    public static String getLogPropertyCN(LCP<?> lp, String logNamespace, SystemEventsLogicsModule systemEventsLM) {
        String namespace = PropertyCanonicalNameParser.getNamespace(lp.property.getCanonicalName());
        String name = getLogPropertyName(namespace, lp.property.getName());

        List<ResolveClassSet> signature = getSignatureForLogProperty(lp, systemEventsLM);
        return PropertyCanonicalNameUtils.createName(logNamespace, name, signature);
    }

    private static String getLogPropertyName(LCP<?> lp, boolean drop) {
        String namespace = PropertyCanonicalNameParser.getNamespace(lp.property.getCanonicalName());
        return getLogPropertyName(namespace, lp.property.getName(), drop);
    }

    private static String getLogPropertyName(String namespace, String name) {
        return getLogPropertyName(namespace, name, false);
    }


    private static String getLogPropertyName(String namespace, String name, boolean drop) {
        return (drop ? PropertyCanonicalNameUtils.logDropPropPrefix : PropertyCanonicalNameUtils.logPropPrefix) + namespace + "_" + name;
    }
    
    public static String getLogPropertyCN(String logNamespace, String namespace, String name, List<ResolveClassSet> signature) {
        return PropertyCanonicalNameUtils.createName(logNamespace, getLogPropertyName(namespace, name), signature);            
    } 
    
    // ------------------- Loggable ----------------- //
    // todo [dale]: тут конечно страх, во-первых, сигнатура берется из интерфейсов свойства (issue #48),
    // во-вторых руками markStored вызывается, чтобы обойти проблему с созданием propertyField из addDProp 
    public LCP addLProp(SystemEventsLogicsModule systemEventsLM, LCP lp) {
        assert lp.property.isNamed();
        String name = getLogPropertyName(lp, false);
        
        List<ResolveClassSet> signature = getSignatureForLogProperty(lp, systemEventsLM);
        
        LCP result = addDCProp(LocalizedString.create("{logics.log}" + " " + lp.property), 1, lp, add(new Object[]{addJProp(baseLM.equals2, 1, systemEventsLM.currentSession), lp.listInterfaces.size() + 1}, directLI(lp)));

        makePropertyPublic(result, name, signature);
        ((StoredDataProperty)result.property).markStored(baseLM.tableFactory);
        return result;
    }

    public LCP addLDropProp(SystemEventsLogicsModule systemEventsLM, LCP lp) {
        String name = getLogPropertyName(lp, true);

        List<ResolveClassSet> signature = getSignatureForLogProperty(lp, systemEventsLM);

        LCP equalsProperty = addJProp(baseLM.equals2, 1, systemEventsLM.currentSession);
        LCP logDropProperty = addLogProp(LocalizedString.create("{logics.log}" + " " + lp.property + " {drop}"), 1, lp, add(new Object[]{equalsProperty, lp.listInterfaces.size() + 1}, directLI(lp)));

        LCP changedProperty = addCHProp(lp, IncrementType.DROP, PrevScope.EVENT);
        LCP whereProperty = addJProp(baseLM.and1, add(directLI(changedProperty), new Object[] {equalsProperty, changedProperty.listInterfaces.size() + 1}));

        Object[] params = directLI(baseLM.vtrue);
        if (whereProperty != null) {
            params = BaseUtils.add(params, directLI(whereProperty));
        }
        logDropProperty.setEventChange(systemEventsLM, true, params);

        makePropertyPublic(logDropProperty, name, signature);
        ((StoredDataProperty)logDropProperty.property).markStored(baseLM.tableFactory);

        return logDropProperty;
    }

    private LCP toLogical(LCP property) {
        return addJProp(baseLM.and1, add(baseLM.vtrue, directLI(property)));
    }

    private LCP convertToLogical(LCP property) {
        if (!isLogical(property)) {
            property = toLogical(property);
        }
        return property;
    }

    protected boolean isLogical(LCP<?> property) {
        if(property == null)
            return false;

        Type type = property.property.getType();
        return type != null && type.equals(LogicalClass.instance);
    }

    public LCP addLWhereProp(LCP logValueProperty, LCP logDropProperty) {
        return addUProp(null, LocalizedString.NONAME, Union.OVERRIDE, null, null, add(directLI(convertToLogical(logValueProperty)), directLI(logDropProperty)));
                
    }

    // ------------------- CONCAT ----------------- //

    protected LCP addSFUProp(int intNum, String separator) {
        return addSFUProp(separator, intNum);
    }

    protected LCP addSFUProp(String separator, int intNum) {
        return addUProp(null, false, LocalizedString.create("{logics.join}"), Union.STRING_AGG, separator, null, getUParams(intNum));
    }

    // ------------------- ACTION ----------------- //

    public LAP addAProp(ActionProperty property) {
        return addAProp(null, property);
    }

    public LAP addAProp(AbstractGroup group, ActionProperty property) {
        return addProperty(group, new LAP(property));
    }

    // ------------------- MESSAGE ----------------- //

    protected LAP addMAProp(String title, boolean noWait, Object... params) {
        return addMAProp(null, LocalizedString.NONAME, title, noWait, params);
    }

    protected LAP addMAProp(AbstractGroup group, LocalizedString caption, String title, boolean noWait, Object... params) {
        return addJoinAProp(group, caption, addMAProp(title, noWait), params);
    }

    @IdentityStrongLazy
    protected LAP addMAProp(String title, boolean noWait) {
        return addProperty(null, new LAP(new MessageActionProperty(LocalizedString.create("Message"), title, noWait)));
    }

    public LAP addFocusActionProp(PropertyDrawEntity propertyDrawEntity) {
        return addProperty(null, new LAP(new FocusActionProperty(propertyDrawEntity)));
    }

    // ------------------- CONFIRM ----------------- //


    protected LAP addConfirmAProp(String title, boolean yesNo, LCP targetProp, Object... params) {
        return addConfirmAProp(null, LocalizedString.NONAME, title, yesNo, targetProp, params);
    }

    protected LAP addConfirmAProp(AbstractGroup group, LocalizedString caption, String title, boolean yesNo, LCP<?> targetProp, Object... params) {
        return addJoinAProp(group, caption, addConfirmAProp(title, yesNo, targetProp != null ? targetProp.property : null), params);
    }

    @IdentityStrongLazy
    protected LAP addConfirmAProp(String title, boolean yesNo, CalcProperty property) {
        return addProperty(null, new LAP(new ConfirmActionProperty(LocalizedString.create("Confirm"), title, yesNo, property != null ? new LCP(property) : null)));
    }

    // ------------------- Async Update Action ----------------- //

    protected LAP addAsyncUpdateAProp(Object... params) {
        return addAsyncUpdateAProp(LocalizedString.NONAME, params);
    }

    protected LAP addAsyncUpdateAProp(LocalizedString caption, Object... params) {
        return addAsyncUpdateAProp(null, caption, params);
    }

    protected LAP addAsyncUpdateAProp(AbstractGroup group, LocalizedString caption, Object... params) {
        return addJoinAProp(group, caption, addAsyncUpdateAProp(), params);
    }

    @IdentityStrongLazy
    protected LAP addAsyncUpdateAProp() {
        return addProperty(null, new LAP(new AsyncUpdateEditValueActionProperty(LocalizedString.create("Async Update"))));
    }

    // ------------------- EVAL ----------------- //

    public LAP addEvalAProp(LCP<?> scriptSource, List<LCP<?>> params, boolean action) {
        return addAProp(null, new EvalActionProperty(LocalizedString.NONAME, scriptSource, params, action));
    }

    // ------------------- DRILLDOWN ----------------- //

    public void setupDrillDownProperty(Property property, boolean isLightStart) {
        if (property instanceof CalcProperty && ((CalcProperty) property).supportsDrillDown()) {
            LAP<?> drillDownFormProperty = isLightStart ? addLazyAProp((CalcProperty) property) : addDDAProp((CalcProperty) property);
            ActionProperty formProperty = drillDownFormProperty.property;
            property.setContextMenuAction(formProperty.getSID(), formProperty.caption);
            property.setEditAction(formProperty.getSID(), formProperty.getImplement(property.getReflectionOrderInterfaces()));
        }
    }
    
    public LAP addDrillDownAProp(LCP<?> property) {
        return addDDAProp(property);
    }

    public LAP<?> addDDAProp(LCP property) {
        assert property.property.getReflectionOrderInterfaces().equals(property.listInterfaces);
        if (property.property instanceof CalcProperty && ((CalcProperty) property.property).supportsDrillDown())
            return addDDAProp((CalcProperty) property.property);
        else 
            throw new UnsupportedOperationException();
    }

    private String nameForDrillDownAction(CalcProperty property, List<ResolveClassSet> signature) {
        assert property.isNamed();
        PropertyCanonicalNameParser parser = new PropertyCanonicalNameParser(property.getCanonicalName(), baseLM.getClassFinder());
        String name = PropertyCanonicalNameUtils.drillDownPrefix + parser.getNamespace() + "_" + property.getName();
        signature.addAll(parser.getSignature());
        return name;
    }

    public LAP<?> addDDAProp(CalcProperty property) {
        List<ResolveClassSet> signature = new ArrayList<>();
        DrillDownFormEntity drillDownFormEntity = property.getDrillDownForm(this, null);
        LAP result = addMFAProp(baseLM.drillDownGroup, LocalizedString.create("{logics.property.drilldown.action}"), drillDownFormEntity, drillDownFormEntity.paramObjects, property.drillDownInNewSession());
        if (property.isNamed()) {
            String name = nameForDrillDownAction(property, signature);
            makeActionPublic(result, name, signature);
        }
        return result;
    }

    public LAP<?> addLazyAProp(CalcProperty property) {
        LAP result = addAProp(null, new LazyActionProperty(LocalizedString.create("{logics.property.drilldown.action}"), property));
        if (property.isNamed()) {
            List<ResolveClassSet> signature = new ArrayList<>();
            String name = nameForDrillDownAction(property, signature);
            makeActionPublic(result, name, signature);
        }
        return result;
    }

    public SessionDataProperty getAddedObjectProperty() {
        return baseLM.getAddedObjectProperty();
    }

    public LCP getIsActiveFormProperty() {
        return baseLM.getIsActiveFormProperty();
    }

    // ---------------------- VALUE ---------------------- //

    public LCP getObjValueProp(FormEntity formEntity, ObjectEntity obj) {
        return baseLM.getObjValueProp(formEntity, obj);
    }

    // ---------------------- Add Object ---------------------- //

    public <T extends PropertyInterface, I extends PropertyInterface> LAP addAddObjAProp(CustomClass cls, boolean autoSet, int resInterfaces, boolean conditional, boolean resultExists, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(getIntNum(params));
        ImList<CalcPropertyInterfaceImplement<PropertyInterface>> readImplements = readCalcImplements(innerInterfaces, params);
        CalcPropertyMapImplement<T, PropertyInterface> resultPart = (CalcPropertyMapImplement<T, PropertyInterface>)
                (resultExists ? readImplements.get(resInterfaces) : null);
        CalcPropertyMapImplement<T, PropertyInterface> conditionalPart = (CalcPropertyMapImplement<T, PropertyInterface>)
                (conditional ? readImplements.get(resInterfaces + (resultExists ? 1 : 0)) : null);

        return addAProp(null, new AddObjectActionProperty(cls, innerInterfaces.getSet(), readImplements.subList(0, resInterfaces).toOrderExclSet(), conditionalPart, resultPart, MapFact.<CalcPropertyInterfaceImplement<I>, Boolean>EMPTYORDER(), false, autoSet));
    }

    public LAP getAddObjectAction(FormEntity formEntity, ObjectEntity obj, CustomClass explicitClass) {
        return baseLM.getAddObjectAction(formEntity, obj, explicitClass);
    }

    // ---------------------- Delete Object ---------------------- //

    public LAP addDeleteAction(CustomClass cls, FormSessionScope scope) {
//        LAP delete = addChangeClassAProp(baseClass.unknown, 1, 0, false, true, 1, is(cls), 1);
//
//        LAP<?> result = addIfAProp(LocalizedString.create("{logics.delete}"), baseLM.sessionOwners, // IF sessionOwners() THEN 
//                delete, 1, // DELETE
//                addListAProp( // ELSE
//                        addConfirmAProp("lsFusion", addCProp(StringClass.text, LocalizedString.create("{form.instance.do.you.really.want.to.take.action} '{logics.delete}'"))), // CONFIRM
//                        addIfAProp(baseLM.confirmed, // IF confirmed() THEN
//                                addListAProp(
//                                        delete, 1, // DELETE
//                                        baseLM.apply), 1), 1 // apply()
//                ), 1);
        
        LAP<?> result = addDeleteAProp(LocalizedString.create("{logics.delete}"), cls);

        result.property.setSimpleDelete(true);
        setDeleteActionOptions(result);

        return addSessionScopeAProp(scope, result);
    }

    protected void setDeleteActionOptions(LAP property) {
        setFormActions(property);

        property.setImage("delete.png");
        property.setChangeKey(KeyStrokes.getDeleteActionPropertyKeyStroke());
        property.setShowChangeKey(false);
    }

    // ---------------------- Add Form ---------------------- //

    protected LAP addAddFormAction(CustomClass cls, ObjectEntity contextObject, FormSessionScope scope) {
        LocalizedString caption = LocalizedString.NONAME;

        // NEW AUTOSET x=X DO {
        //      REQUEST
        //          edit(x);
        //      DO
        //          SEEK co=x;
        //      ELSE
        //          IF sessionOwners THEN
        //              DELETE x;
        // }

        LAP result = addForAProp(LocalizedString.create("{logics.add}"), false, false, false, false, 0, cls, true, false, 0, false,
                1, //NEW x=X
                addRequestAProp(null, caption, // REQUEST
                        baseLM.getPolyEdit(), 1, // edit(x);
                        (contextObject != null ? addOSAProp(contextObject, UpdateType.LAST, 1) : baseLM.getEmptyObject()), 1, // DO SEEK co = x
                        addIfAProp(baseLM.sessionOwners, baseLM.getPolyDelete(), 1), 1 // ELSE IF seekOwners THEN delete(x)
                ), 1
        );
//        LAP result = addListAProp(
//                            addAddObjAProp(cls, true, 0, false, true, addedProperty), // NEW (FORM with AUTOSET), addAddObjAProp(cls, false, true, 0, false, true, addedProperty),
//                            addJoinAProp(addListAProp( // так хитро делается чтобы заnest'ить addedProperty (иначе apply его сбрасывает)
//                                    addDMFAProp(caption, cls, ManageSessionType.AUTO, true), 1, // FORM EDIT class OBJECT prm
//                                    addSetPropertyAProp(1, false, 1, addedProperty, 1), 1), // addedProperty <- prm
//                            addedProperty)); // FORM EDIT class OBJECT prm
//
//        LCP formResultProperty = baseLM.getFormResultProperty();
//        result = addListAProp(LocalizedString.create("{logics.add}"), result,
//                addIfAProp(addJProp(baseLM.equals2, formResultProperty, addCProp(baseLM.formResult, "ok")), // IF formResult == ok
//                        (contextObject != null ? addJoinAProp(addOSAProp(contextObject, true, 1), addedProperty) : baseLM.getEmpty()), // THEN (contextObject != null) SEEK exf.o prm
//                        (addIfAProp(baseLM.sessionOwners, addJoinAProp(getDeleteAction(cls, contextObject, FormSessionScope.OLDSESSION), addedProperty)))) // ELSE IF sessionOwners DELETE prm, // предполагается что если нет
//                         );

        setAddActionOptions(result, contextObject);
        
        return addSessionScopeAProp(scope, result);
    }

    protected void setAddActionOptions(LAP property, final ObjectEntity objectEntity) {

        setFormActions(property);

        property.setImage("add.png");
        property.setChangeKey(KeyStrokes.getAddActionPropertyKeyStroke());
        property.setShowChangeKey(false);

        if(objectEntity != null) {
            property.addProcessor(new Property.DefaultProcessor() {
                public void proceedDefaultDraw(PropertyDrawEntity entity, FormEntity form) {
                    if(entity.toDraw == null)
                        entity.toDraw = objectEntity.groupTo;
                }
                public void proceedDefaultDesign(PropertyDrawView propertyView) {
                }
            });
        }
    }

    // ---------------------- Edit Form ---------------------- //

    protected LAP addEditFormAction(FormSessionScope scope, CustomClass customClass) {
        LAP result = addEditAProp(LocalizedString.create("{logics.edit}"), customClass);

        setEditActionOptions(result);

        return addSessionScopeAProp(scope, result);
    }
    
    private void setFormActions(LAP result) {
        result.setShouldBeLast(true);
        result.setForceViewType(ClassViewType.TOOLBAR);
    }

    private void setEditActionOptions(LAP result) {
        setFormActions(result);
        
        result.setImage("edit.png");
        result.setChangeKey(KeyStrokes.getEditActionPropertyKeyStroke());
        result.setShowChangeKey(false);
    }

    public LAP addProp(ActionProperty prop) {
        return addProp(null, prop);
    }

    public LAP addProp(AbstractGroup group, ActionProperty prop) {
        return addProperty(group, new LAP(prop));
    }

    public LCP addProp(CalcProperty<? extends PropertyInterface> prop) {
        return addProp(null, prop);
    }

    public LCP addProp(AbstractGroup group, CalcProperty<? extends PropertyInterface> prop) {
        return addProperty(group, new LCP(prop));
    }

    protected void addPropertyToGroup(Property<?> property, AbstractGroup group) {
        Version version = getVersion();
        if (group != null) {
            group.add(property, version);
        } else if (!property.isLocal() && !temporary) {
            baseLM.privateGroup.add(property, version);
        }
    }

    protected <T extends LP<?, ?>> T addProperty(AbstractGroup group, T lp) {
        addPropertyToGroup(lp.property, group);
        return lp;
    }

    public void addIndex(LCP lp) {
        ImOrderSet<String> keyNames = SetFact.toOrderExclSet(lp.listInterfaces.size(), new GetIndex<String>() {
            public String getMapValue(int i) {
                return "key"+i;
            }});
        addIndex(keyNames, directLI(lp));
    }

    public void addIndex(CalcProperty property) {
        addIndex(new LCP(property));
    }

    public void addIndex(ImOrderSet<String> keyNames, Object... params) {
        ImList<CalcPropertyObjectInterfaceImplement<String>> index = PropertyUtils.readObjectImplements(keyNames, params);
        ThreadLocalContext.getDbManager().addIndex(index);
    }

    protected void addPersistent(LCP lp) {
        addPersistent((AggregateProperty) lp.property, null);
    }

    protected void addPersistent(LCP lp, ImplementTable table) {
        addPersistent((AggregateProperty) lp.property, table);
    }

    private void addPersistent(AggregateProperty property, ImplementTable table) {
        assert property.isNamed();

        logger.debug("Initializing stored property " + property + "...");
        property.markStored(baseLM.tableFactory, table);
    }

    // нужен так как иначе начинает sID расширять

    public <T extends PropertyInterface> LCP<T> addOldProp(LCP<T> lp, PrevScope scope) {
        return baseLM.addOldProp(lp, scope);
    }

    public <T extends PropertyInterface> LCP<T> addCHProp(LCP<T> lp, IncrementType type, PrevScope scope) {
        return baseLM.addCHProp(lp, type, scope);
    }

    public <T extends PropertyInterface> LCP addClassProp(LCP<T> lp) {
        return baseLM.addClassProp(lp);
    }

    @IdentityStrongLazy // для ID
    public LCP addGroupObjectProp(GroupObjectEntity groupObject, GroupObjectProp prop) {
        CalcPropertyRevImplement<ClassPropertyInterface, ObjectEntity> filterProperty = groupObject.getProperty(prop);
        return addProperty(null, new LCP<>(filterProperty.property, groupObject.getOrderObjects().mapOrder(filterProperty.mapping.reverse())));
    }
    
    protected LAP addOSAProp(ObjectEntity object, UpdateType type, Object... params) {
        return addOSAProp(null, LocalizedString.NONAME, object, type, params);
    }

    protected LAP addOSAProp(AbstractGroup group, LocalizedString caption, ObjectEntity object, UpdateType type, Object... params) {
        return addJoinAProp(group, caption, addOSAProp(object, type), params);
    }

    @IdentityStrongLazy // для ID
    public LAP addOSAProp(ObjectEntity object, UpdateType type) {
        SeekObjectActionProperty seekProperty = new SeekObjectActionProperty(object, type);
        return addProperty(null, new LAP<>(seekProperty));
    }

    protected LAP addGOSAProp(GroupObjectEntity object, List<ObjectEntity> objects, UpdateType type, Object... params) {
        return addGOSAProp(null, LocalizedString.NONAME, object, objects, type, params);
    }

    protected LAP addGOSAProp(AbstractGroup group, LocalizedString caption, GroupObjectEntity object, List<ObjectEntity> objects, UpdateType type, Object... params) {
        return addJoinAProp(group, caption, addGOSAProp(object, objects, type), params);
    }

    @IdentityStrongLazy // для ID
    public LAP addGOSAProp(GroupObjectEntity object, List<ObjectEntity> objects, UpdateType type) {
        List<ValueClass> objectClasses = new ArrayList<>();
        for (ObjectEntity obj : objects) {
            objectClasses.add(obj.baseClass);
        }
        SeekGroupObjectActionProperty seekProperty = new SeekGroupObjectActionProperty(object, objects, type, objectClasses.toArray(new ValueClass[objectClasses.size()]));
        return addProperty(null, new LAP<>(seekProperty));
    }

    public void addConstraint(CalcProperty property, boolean checkChange) {
        addConstraint(property, null, checkChange);
    }

    public void addConstraint(CalcProperty property, CalcProperty messageProperty, boolean checkChange) {
        addConstraint(property, messageProperty, checkChange, null);
    }

    public void addConstraint(CalcProperty property, boolean checkChange, DebugInfo.DebugPoint debugPoint) {
        addConstraint(addProp(property), null, checkChange, debugPoint);
    }

    public void addConstraint(CalcProperty property, CalcProperty messageProperty, boolean checkChange, DebugInfo.DebugPoint debugPoint) {
        addConstraint(addProp(property), messageProperty == null ? null : addProp(messageProperty), checkChange, debugPoint);
    }

    public void addConstraint(LCP<?> lp, LCP<?> messageLP, boolean checkChange, DebugInfo.DebugPoint debugPoint) {
        addConstraint(lp, messageLP, (checkChange ? CalcProperty.CheckType.CHECK_ALL : CalcProperty.CheckType.CHECK_NO), null, Event.APPLY, this, debugPoint);
    }

    protected void addConstraint(LCP<?> lp, LCP<?> messageLP, CalcProperty.CheckType type, ImSet<CalcProperty<?>> checkProps, Event event, LogicsModule lm, DebugInfo.DebugPoint debugPoint) {
        if(!(lp.property).noDB())
            lp = addCHProp(lp, IncrementType.SET, event.getScope());
        // assert что lp уже в списке properties
        setConstraint(lp.property, messageLP == null ? null : messageLP.property, type, event, checkProps, debugPoint);
    }

    public <T extends PropertyInterface> void setConstraint(CalcProperty property, CalcProperty messageProperty, CalcProperty.CheckType type, Event event, ImSet<CalcProperty<?>> checkProperties, DebugInfo.DebugPoint debugPoint) {
        assert type != CalcProperty.CheckType.CHECK_SOME || checkProperties != null;
        assert property.noDB();

        property.checkChange = type;
        property.checkProperties = checkProperties;

        ActionPropertyMapImplement<ClassPropertyInterface, ClassPropertyInterface> logAction;
//            logAction = new LogPropertyActionProperty<T>(property, messageProperty).getImplement();
        //  PRINT OUT property MESSAGE NOWAIT;
        logAction = (ActionPropertyMapImplement<ClassPropertyInterface, ClassPropertyInterface>) addPFAProp(null, LocalizedString.concat("Constraint - ",property.caption), new OutFormSelector<T>(property, messageProperty), ListFact.<ObjectSelector>EMPTY(), ListFact.<Boolean>EMPTY(), null, null, FormPrintType.MESSAGE, false, 30, null, null, true).property.getImplement();
        ActionPropertyMapImplement<?, ClassPropertyInterface> constraintAction =
                DerivedProperty.createListAction(
                        SetFact.<ClassPropertyInterface>EMPTY(),
                        ListFact.toList(logAction,
                                baseLM.cancel.property.getImplement(SetFact.<ClassPropertyInterface>EMPTYORDER())
                        )
                );
        constraintAction.mapEventAction(this, DerivedProperty.createAnyGProp(property).getImplement(), event, true, debugPoint);
        addProp(constraintAction.property);
    }

    public <T extends PropertyInterface> void addEventAction(Event event, boolean descending, boolean ordersNotNull, int noInline, boolean forceInline, DebugInfo.DebugPoint debugPoint, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(getIntNum(params));

        ImList<PropertyInterfaceImplement<PropertyInterface>> listImplements = readImplements(innerInterfaces, params);
        int implCnt = listImplements.size();

        ImOrderMap<CalcPropertyInterfaceImplement<PropertyInterface>, Boolean> orders = BaseUtils.immutableCast(listImplements.subList(2, implCnt - noInline).toOrderSet().toOrderMap(descending));

        ImSet<PropertyInterface> noInlineInterfaces = BaseUtils.<ImList<PropertyInterface>>immutableCast(listImplements.subList(implCnt - noInline, implCnt)).toOrderExclSet().getSet();

        addEventAction(innerInterfaces.getSet(), (ActionPropertyMapImplement<?, PropertyInterface>) listImplements.get(0), (CalcPropertyMapImplement<?, PropertyInterface>) listImplements.get(1), orders, ordersNotNull, event, noInlineInterfaces, forceInline, false, debugPoint);
    }

    public <P extends PropertyInterface, D extends PropertyInterface> void addEventAction(ActionProperty<P> actionProperty, CalcPropertyMapImplement<?, P> whereImplement, ImOrderMap<CalcPropertyInterfaceImplement<P>, Boolean> orders, boolean ordersNotNull, Event event, boolean resolve, DebugInfo.DebugPoint debugPoint) {
        addEventAction(actionProperty.interfaces, actionProperty.getImplement(), whereImplement, orders, ordersNotNull, event, SetFact.<P>EMPTY(), false, resolve, debugPoint);
    }

    public <P extends PropertyInterface, D extends PropertyInterface> void addEventAction(ImSet<P> innerInterfaces, ActionPropertyMapImplement<?, P> actionProperty, CalcPropertyMapImplement<?, P> whereImplement, ImOrderMap<CalcPropertyInterfaceImplement<P>, Boolean> orders, boolean ordersNotNull, Event event, ImSet<P> noInline, boolean forceInline, boolean resolve, DebugInfo.DebugPoint debugPoint) {
        if(!(whereImplement.property).noDB())
            whereImplement = whereImplement.mapChanged(IncrementType.SET, event.getScope());

        ActionProperty<? extends PropertyInterface> action =
                innerInterfaces.isEmpty() ?
                    DerivedProperty.createIfAction(innerInterfaces, whereImplement, actionProperty, null).property :
                    DerivedProperty.createForAction(innerInterfaces, SetFact.<P>EMPTY(), whereImplement, orders, ordersNotNull, actionProperty, null, false, noInline, forceInline).property;

        if(debugPoint != null) { // создано getEventDebugPoint
            if(debugger.isEnabled()) // topContextActionPropertyDefinitionBodyCreated
                debugger.setNewDebugStack(action);

            assert action.getDelegationType(true) == ActionDelegationType.AFTER_DELEGATE;
            ScriptingLogicsModule.setDebugInfo(true, debugPoint, action); // actionPropertyDefinitionBodyCreated
        }

//        action.setStrongUsed(whereImplement.property); // добавить сильную связь, уже не надо поддерживается более общий механизм - смотреть на Session Calc
//        action.caption = "WHEN " + whereImplement.property + " " + actionProperty;
        addProp(action);

        addBaseEvent(action, event, resolve, false);
    }

    public <P extends PropertyInterface> void addBaseEvent(ActionProperty<P> action, Event event, boolean resolve, boolean single) {
        action.addEvent(event.base, event.session);
        if(event.after != null)
            action.addStrongUsed(event.after);
        action.singleApply = single;
        action.resolve = resolve;
    }

    public <P extends PropertyInterface> void addAspectEvent(int interfaces, ActionPropertyImplement<P, Integer> action, String mask, boolean before) {
        // todo: непонятно что пока с полными каноническими именами и порядками параметров делать
    }

    public <P extends PropertyInterface, T extends PropertyInterface> void addAspectEvent(ActionProperty<P> action, ActionPropertyMapImplement<T, P> aspect, boolean before) {
        if(before)
            action.addBeforeAspect(aspect);
        else
            action.addAfterAspect(aspect);
    }

    protected <L extends PropertyInterface, T extends PropertyInterface> void follows(LCP<T> first, LCP<L> second, Integer... mapping) {
        follows(first, null, ListFact.toList(new PropertyFollowsDebug(true, null), new PropertyFollowsDebug(false, null)), Event.APPLY, second, mapping);
    }

    protected <L extends PropertyInterface, T extends PropertyInterface> void follows(final LCP<T> first, DebugInfo.DebugPoint debugPoint, ImList<PropertyFollowsDebug> options, Event event, LCP<L> second, final Integer... mapping) {
        addFollows(first.property, new CalcPropertyMapImplement<>(second.property, second.getRevMap(first.listInterfaces, mapping)), debugPoint, options, event);
    }

    public <T extends PropertyInterface, L extends PropertyInterface> void setNotNull(CalcProperty<T> property, DebugInfo.DebugPoint debugPoint, ImList<PropertyFollowsDebug> options, Event event) {
        CalcPropertyMapImplement<L, T> mapClasses = (CalcPropertyMapImplement<L, T>) IsClassProperty.getMapProperty(property.getInterfaceClasses(ClassType.logPolicy));
        property.setNotNull = true;
        addFollows(mapClasses.property, new CalcPropertyMapImplement<>(property, mapClasses.mapping.reverse()),
                LocalizedString.concatList(LocalizedString.create("{logics.property} "), property.caption, " [" + property.getSID(), LocalizedString.create("] {logics.property.not.defined}")),
                debugPoint, options, event);
    }

    public <T extends PropertyInterface, L extends PropertyInterface> void addFollows(CalcProperty<T> property, CalcPropertyMapImplement<L, T> implement, DebugInfo.DebugPoint debugPoint, ImList<PropertyFollowsDebug> options, Event event) {
        addFollows(property, implement, LocalizedString.create("{logics.property.violated.consequence.from}" + "(" + this + ") => (" + implement.property + ")"), debugPoint, options, event);
    }

    public <T extends PropertyInterface, L extends PropertyInterface> void addFollows(CalcProperty<T> property, CalcPropertyMapImplement<L, T> implement, LocalizedString caption, DebugInfo.DebugPoint debugPoint, ImList<PropertyFollowsDebug> options, Event event) {
//        PropertyFollows<T, L> propertyFollows = new PropertyFollows<T, L>(this, implement, options);

        for(PropertyFollowsDebug option : options) {
            assert !option.isTrue || property.interfaces.size() == implement.mapping.size(); // assert что количество
            ActionPropertyMapImplement<?, T> setAction = option.isTrue ? implement.getSetNotNullAction(true) : property.getSetNotNullAction(false);
            if(setAction!=null) {
//                setAction.property.caption = "RESOLVE " + option.isTrue + " : " + property + " => " + implement.property;
                CalcPropertyMapImplement<?, T> condition;
                if(option.isFull)
                    condition = DerivedProperty.createAndNot(property, implement).mapChanged(IncrementType.SET, event.getScope());
                else {
                    if (option.isTrue)
                        condition = DerivedProperty.createAndNot(property.getChanged(IncrementType.SET, event.getScope()), implement);
                    else
                        condition = DerivedProperty.createAnd(property, implement.mapChanged(IncrementType.DROP, event.getScope()));
                }
                setAction.mapEventAction(this, condition, event, true, option.debugPoint);
            }
        }

        CalcProperty constraint = DerivedProperty.createAndNot(property, implement).property;
        constraint.caption = caption;
        addConstraint(constraint, false, debugPoint);
    }

    protected <P extends PropertyInterface, C extends PropertyInterface> void setNotNull(LCP<P> lp, ImList<PropertyFollowsDebug> resolve) {
        setNotNull(lp, null, Event.APPLY, resolve);
    }

    protected <P extends PropertyInterface, C extends PropertyInterface> void setNotNull(LCP<P> lp, DebugInfo.DebugPoint debugPoint, Event event, ImList<PropertyFollowsDebug> resolve) {
        setNotNull(lp.property, debugPoint, resolve, event);
    }

    public static <P extends PropertyInterface, T extends PropertyInterface> ActionPropertyMapImplement<P, T> mapActionListImplement(LAP<P> property, ImOrderSet<T> mapList) {
        return new ActionPropertyMapImplement<>(property.property, getMapping(property, mapList));
    }
    public static <P extends PropertyInterface, T extends PropertyInterface> CalcPropertyMapImplement<P, T> mapCalcListImplement(LCP<P> property, ImOrderSet<T> mapList) {
        return new CalcPropertyMapImplement<>(property.property, getMapping(property, mapList));
    }

    private static <P extends PropertyInterface, T extends PropertyInterface> ImRevMap<P, T> getMapping(LP<P, ?> property, final ImOrderSet<T> mapList) {
        return property.getRevMap(mapList);
    }

    protected void makeUserLoggable(SystemEventsLogicsModule systemEventsLM, LCP... lps) {
        for (LCP lp : lps)
            lp.makeUserLoggable(this, systemEventsLM);
    }

    public LCP not() {
        return baseLM.not();
    }

    // получает свойство is
    public LCP<?> is(ValueClass valueClass) {
        return baseLM.is(valueClass);
    }

    public LCP object(ValueClass valueClass) {
        return baseLM.object(valueClass);
    }

    protected LCP and(boolean... nots) {
        return addAFProp(nots);
    }

    protected NavigatorElement addNavigatorFolder(String canonicalName, LocalizedString caption) {
        NavigatorElement elem = new NavigatorFolder(canonicalName, caption);
        addNavigatorElement(elem);
        return elem;
    }

    protected NavigatorAction addNavigatorAction(LAP<?> property, String canonicalName, LocalizedString caption) {
        NavigatorAction navigatorAction = new NavigatorAction(property.property, canonicalName, caption, null, "/images/action.png", DefaultIcon.ACTION);
        addNavigatorElement(navigatorAction);
        return navigatorAction;
    }

    protected LAP<?> getNavigatorAction(FormEntity form) {
        return baseLM.getFormNavigatorAction(form);
    }

    protected NavigatorElement addNavigatorForm(FormEntity form, String canonicalName, LocalizedString caption) {
        NavigatorAction navigatorForm = new NavigatorAction(getNavigatorAction(form).property, canonicalName, caption, form, "/images/form.png", DefaultIcon.FORM);

        addNavigatorElement(navigatorForm);
        return navigatorForm;
    }
    
    public Collection<NavigatorElement> getNavigatorElements() {
        return navigatorElements.values();
    }

    public Collection<FormEntity> getNamedForms() {
        return namedForms.values();
    } 
    
    public Collection<ImplementTable> getTables() {
        return tables.values();    
    }
    
    // в том числе и приватные 
    public Collection<FormEntity> getAllModuleForms() {
        List<FormEntity> elements = new ArrayList<>();
        elements.addAll(unnamedForms);
        elements.addAll(namedForms.values());
        return elements;
    }
    
    public NavigatorElement getNavigatorElement(String name) {
        return navigatorElements.get(name);
    }

    public FormEntity getForm(String name) {
        return namedForms.get(name);
    }
    
    public <T extends FormEntity> T addFormEntity(T form) {
        if (form.isNamed()) {
            addNamedForm(form);
        } else {
            addPrivateForm(form);
        }
        return form;
    }
    
    @NFLazy
    private void addNavigatorElement(NavigatorElement element) {
        assert !navigatorElements.containsKey(element.getName());
        navigatorElements.put(element.getName(), element);
    }

    @NFLazy
    private void addNamedForm(FormEntity form) {
        assert !namedForms.containsKey(form.getName());
        namedForms.put(form.getName(), form);
    }
    
    @NFLazy
    private void addPrivateForm(FormEntity form) {
        unnamedForms.add(form);
    }

    public void addFormActions(FormEntity form, ObjectEntity object, FormSessionScope scope) {
        Version version = getVersion();
        form.addPropertyDraw(getAddFormAction(form, object, null, scope, version), version);
        form.addPropertyDraw(getEditFormAction(object, null, scope, version), version, object);
        form.addPropertyDraw(getDeleteAction(object, scope), version, object);
    }

    public LAP getAddFormAction(FormEntity contextForm, ObjectEntity contextObject, CustomClass explicitClass, FormSessionScope scope, Version version) {
        CustomClass cls = explicitClass;
        if(cls == null)
            cls = (CustomClass)contextObject.baseClass;
        return baseLM.getAddFormAction(cls, contextForm, contextObject, scope);
    }

    public LAP getEditFormAction(ObjectEntity object, CustomClass explicitClass, FormSessionScope scope, Version version) {
        CustomClass cls = explicitClass;
        if(cls == null)
            cls = (CustomClass) object.baseClass;
        return baseLM.getEditFormAction(cls, scope);
    }

    public LAP getDeleteAction(ObjectEntity object, FormSessionScope scope) {
        CustomClass cls = (CustomClass) object.baseClass;
        return getDeleteAction(cls, object, scope);
    }
    public LAP getDeleteAction(CustomClass cls, ObjectEntity object, FormSessionScope scope) {
        return baseLM.getDeleteAction(cls, scope);
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public boolean isDefaultNamespace() {
        return defaultNamespace;
    }

    public void setDefaultNamespace(boolean defaultNamespace) {
        this.defaultNamespace = defaultNamespace;
    }

    public Set<String> getRequiredNames() {
        return requiredNames;
    }

    public void setRequiredNames(LinkedHashSet<String> requiredNames) {
        this.requiredNames = requiredNames;
    }

    public List<String> getNamespacePriority() {
        return namespacePriority;
    }

    public void setNamespacePriority(List<String> namespacePriority) {
        this.namespacePriority = namespacePriority;
    }

    public List<ResolveClassSet> getParamClasses(LP<?, ?> lp) {
        List<ResolveClassSet> paramClasses;
        if (lp instanceof LCP && locals.containsKey(lp)) {
            paramClasses = locals.get(lp).signature;
        } else {
            paramClasses = propClasses.get(lp);
        }
        return paramClasses == null ? Collections.<ResolveClassSet>nCopies(lp.listInterfaces.size(), null) : paramClasses;                   
    }

    // для обратной совместимости
    public void addFormFixedFilter(FormEntity form, FilterEntity filter) {
        form.addFixedFilter(filter, getVersion());
    }

    public RegularFilterGroupEntity newRegularFilterGroupEntity(int id) {
        return new RegularFilterGroupEntity(id, getVersion());
    }

    public void addFormHintsIncrementTable(FormEntity form, LCP... lps) {
        form.addHintsIncrementTable(getVersion(), lps);
    }

    public int getModuleComplexity() {
        return 1;
    }
    
    public List<LogicsModule> getRequiredModules(String namespace) {
        return namespaceToModules.get(namespace);
    }

    public Set<String> getRequiredNamespaces() {
        return namespaceToModules.keySet();
    }
    
    public Map<String, List<LogicsModule>> getNamespaceToModules() {
        return namespaceToModules;
    }

    public BaseClass getBaseClass() {
        return baseLM.baseClass;
    }

    public AbstractGroup getRootGroup() {
        return baseLM.rootGroup;
    }

    public AbstractGroup getPublicGroup() {
        return baseLM.publicGroup;
    }

    public AbstractGroup getBaseGroup() {
        return baseLM.baseGroup;
    }

    public AbstractGroup getRecognizeGroup() {
        return baseLM.recognizeGroup;
    }

    public static class LocalPropertyData {
        public String name;
        public List<ResolveClassSet> signature;

        public LocalPropertyData(String name, List<ResolveClassSet> signature) {
            this.name = name;
            this.signature = signature;
        }
    }

    protected <P extends PropertyInterface> void addLocal(LCP<P> lcp, LocalPropertyData data) {
        locals.put(lcp, data);
        lcp.property.setCanonicalName(getNamespace(), data.name, data.signature, lcp.listInterfaces, baseLM.getDBNamingPolicy());
    }

    protected void removeLocal(LCP<?> lcp) {
        assert locals.containsKey(lcp);
        locals.remove(lcp);
    }

    public List<ResolveClassSet> getLocalSignature(LCP<?> lcp) {
        assert locals.containsKey(lcp);
        return locals.get(lcp).signature;
    }

    public Map<LCP<?>, LocalPropertyData> getLocals() {
        return locals;
    }

    public LCP<?> resolveProperty(String compoundName, List<ResolveClassSet> params) throws ResolvingErrors.ResolvingError {
        return resolveManager.findProperty(compoundName, params);
    }

    public LCP<?> resolveAbstractProperty(String compoundName, List<ResolveClassSet> params, boolean prioritizeNotEquals) throws ResolvingErrors.ResolvingError {
        return resolveManager.findAbstractProperty(compoundName, params, prioritizeNotEquals);
    }

    public LAP<?> resolveAction(String compoundName, List<ResolveClassSet> params) throws ResolvingErrors.ResolvingError {
        return resolveManager.findAction(compoundName, params);
    }

    public LAP<?> resolveAbstractAction(String compoundName, List<ResolveClassSet> params, boolean prioritizeNotEquals) throws ResolvingErrors.ResolvingError {
        return resolveManager.findAbstractAction(compoundName, params, prioritizeNotEquals);
    }

    public ValueClass resolveClass(String compoundName) throws ResolvingErrors.ResolvingError {
        return resolveManager.findClass(compoundName);
    }

    public MetaCodeFragment resolveMetaCodeFragment(String compoundName, int paramCnt) throws ResolvingErrors.ResolvingError {
        return resolveManager.findMetaCodeFragment(compoundName, paramCnt);
    }
    
    public AbstractGroup resolveGroup(String compoundName) throws ResolvingErrors.ResolvingError {
        return resolveManager.findGroup(compoundName);
    }
    
    public AbstractWindow resolveWindow(String compoundName) throws ResolvingErrors.ResolvingError {
        return resolveManager.findWindow(compoundName);    
    }
    
    public FormEntity resolveForm(String compoundName) throws ResolvingErrors.ResolvingError {
        return resolveManager.findForm(compoundName);
    }
    
    public NavigatorElement resolveNavigatorElement(String compoundName) throws ResolvingErrors.ResolvingError {
        return resolveManager.findNavigatorElement(compoundName);
    } 
    
    public ImplementTable resolveTable(String compoundName) throws ResolvingErrors.ResolvingError {
        return resolveManager.findTable(compoundName);
    }
}
