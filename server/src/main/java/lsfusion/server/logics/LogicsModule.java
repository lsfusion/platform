package lsfusion.server.logics;

import com.google.common.collect.Iterables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.interop.form.WindowFormType;
import lsfusion.interop.form.event.BindingMode;
import lsfusion.interop.form.event.KeyStrokes;
import lsfusion.interop.form.event.MouseInputEvent;
import lsfusion.interop.form.print.FormPrintType;
import lsfusion.interop.form.property.ClassViewType;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.base.caches.IdentityStrongLazy;
import lsfusion.server.base.version.GlobalVersion;
import lsfusion.server.base.version.LastVersion;
import lsfusion.server.base.version.NFLazy;
import lsfusion.server.base.version.Version;
import lsfusion.server.data.expr.formula.CustomFormulaSyntax;
import lsfusion.server.data.expr.formula.StringConcatenateFormulaImpl;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.query.PartitionType;
import lsfusion.server.data.expr.value.StaticParamNullableExpr;
import lsfusion.server.data.type.Type;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.metacode.MetaCodeFragment;
import lsfusion.server.language.property.LP;
import lsfusion.server.language.property.oraction.LAP;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.change.AddObjectAction;
import lsfusion.server.logics.action.change.ChangeClassAction;
import lsfusion.server.logics.action.change.SetAction;
import lsfusion.server.logics.action.flow.*;
import lsfusion.server.logics.action.implement.ActionImplement;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.interactive.ConfirmAction;
import lsfusion.server.logics.action.interactive.MessageAction;
import lsfusion.server.logics.action.session.LocalNestedType;
import lsfusion.server.logics.action.session.action.ApplyAction;
import lsfusion.server.logics.action.session.action.CancelAction;
import lsfusion.server.logics.action.session.action.NewSessionAction;
import lsfusion.server.logics.action.session.changed.IncrementType;
import lsfusion.server.logics.classes.StaticClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.integral.IntegralClass;
import lsfusion.server.logics.classes.data.integral.LongClass;
import lsfusion.server.logics.classes.user.*;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.constraint.OutFormSelector;
import lsfusion.server.logics.event.Event;
import lsfusion.server.logics.event.PrevScope;
import lsfusion.server.logics.form.interactive.ManageSessionType;
import lsfusion.server.logics.form.interactive.UpdateType;
import lsfusion.server.logics.form.interactive.action.async.QuickAccess;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapRemove;
import lsfusion.server.logics.form.interactive.action.change.ActionObjectSelector;
import lsfusion.server.logics.form.interactive.action.edit.FormSessionScope;
import lsfusion.server.logics.form.interactive.action.expand.ExpandCollapseGroupObjectAction;
import lsfusion.server.logics.form.interactive.action.expand.ExpandCollapseType;
import lsfusion.server.logics.form.interactive.action.focus.FocusAction;
import lsfusion.server.logics.form.interactive.action.input.*;
import lsfusion.server.logics.form.interactive.action.seek.SeekGroupObjectAction;
import lsfusion.server.logics.form.interactive.action.seek.SeekObjectAction;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.interactive.dialogedit.ClassFormEntity;
import lsfusion.server.logics.form.interactive.property.GroupObjectProp;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.open.interactive.FormInteractiveAction;
import lsfusion.server.logics.form.open.stat.ExportAction;
import lsfusion.server.logics.form.open.stat.ImportAction;
import lsfusion.server.logics.form.open.stat.PrintAction;
import lsfusion.server.logics.form.stat.struct.FormIntegrationType;
import lsfusion.server.logics.form.stat.struct.IntegrationFormEntity;
import lsfusion.server.logics.form.stat.struct.export.hierarchy.json.ExportJSONAction;
import lsfusion.server.logics.form.stat.struct.export.hierarchy.json.JSONProperty;
import lsfusion.server.logics.form.stat.struct.export.hierarchy.xml.ExportXMLAction;
import lsfusion.server.logics.form.stat.struct.export.plain.csv.ExportCSVAction;
import lsfusion.server.logics.form.stat.struct.export.plain.dbf.ExportDBFAction;
import lsfusion.server.logics.form.stat.struct.export.plain.table.ExportTableAction;
import lsfusion.server.logics.form.stat.struct.export.plain.xls.ExportXLSAction;
import lsfusion.server.logics.form.stat.struct.imports.hierarchy.json.ImportJSONAction;
import lsfusion.server.logics.form.stat.struct.imports.hierarchy.xml.ImportXMLAction;
import lsfusion.server.logics.form.stat.struct.imports.plain.csv.ImportCSVAction;
import lsfusion.server.logics.form.stat.struct.imports.plain.dbf.ImportDBFAction;
import lsfusion.server.logics.form.stat.struct.imports.plain.table.ImportTableAction;
import lsfusion.server.logics.form.stat.struct.imports.plain.xls.ImportXLSAction;
import lsfusion.server.logics.form.struct.AutoFormEntity;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilterEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilterSelector;
import lsfusion.server.logics.form.struct.filter.FilterEntity;
import lsfusion.server.logics.form.struct.filter.RegularFilterGroupEntity;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.navigator.NavigatorAction;
import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.navigator.NavigatorFolder;
import lsfusion.server.logics.navigator.window.AbstractWindow;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.cases.ActionCase;
import lsfusion.server.logics.property.cases.CalcCase;
import lsfusion.server.logics.property.cases.CaseUnionProperty;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.logics.property.classes.data.*;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.data.SessionDataProperty;
import lsfusion.server.logics.property.data.StoredDataProperty;
import lsfusion.server.logics.property.implement.PropertyImplement;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.implement.PropertyRevImplement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.ActionOrPropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.logics.property.set.*;
import lsfusion.server.logics.property.value.ValueProperty;
import lsfusion.server.physics.admin.drilldown.action.LazyAction;
import lsfusion.server.physics.admin.drilldown.form.DrillDownFormEntity;
import lsfusion.server.physics.admin.monitor.SystemEventsLogicsModule;
import lsfusion.server.physics.dev.debug.ActionDebugger;
import lsfusion.server.physics.dev.debug.ActionDelegationType;
import lsfusion.server.physics.dev.debug.DebugInfo;
import lsfusion.server.physics.dev.debug.PropertyFollowsDebug;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.id.name.CanonicalNameUtils;
import lsfusion.server.physics.dev.id.name.DBNamingPolicy;
import lsfusion.server.physics.dev.id.name.PropertyCanonicalNameParser;
import lsfusion.server.physics.dev.id.name.PropertyCanonicalNameUtils;
import lsfusion.server.physics.dev.id.resolve.ResolveManager;
import lsfusion.server.physics.dev.id.resolve.ResolvingErrors;
import lsfusion.server.physics.dev.integration.internal.to.StringFormulaProperty;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;
import lsfusion.server.physics.exec.db.table.ImplementTable;
import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;

import static lsfusion.base.BaseUtils.add;
import static lsfusion.server.logics.property.PropertyFact.createAnd;
import static lsfusion.server.logics.property.PropertyFact.createStatic;
import static lsfusion.server.logics.property.oraction.ActionOrPropertyUtils.*;

// modules logics in theory should be in dev.module.package but in this class it's more about logics, than about modularity
public abstract class LogicsModule {
    protected static final Logger logger = Logger.getLogger(LogicsModule.class);

    protected static final ActionDebugger debugger = ActionDebugger.getInstance();

    // после этого шага должны быть установлены name, namespace, requiredModules
    public abstract void initModuleDependencies() throws RecognitionException;

    public abstract void initMetaAndClasses() throws RecognitionException;

    public abstract void initTables(DBNamingPolicy namingPolicy) throws RecognitionException;

    public abstract void initMainLogic() throws RecognitionException;

    public abstract void initIndexes(DBManager dbManager) throws RecognitionException;

    public BaseLogicsModule baseLM;

    protected Map<String, List<LP<?>>> namedProperties = new HashMap<>();
    protected Map<String, List<LA<?>>> namedActions = new HashMap<>();
    
    protected final Map<String, Group> groups = new HashMap<>();
    protected final Map<String, CustomClass> classes = new HashMap<>();
    protected final Map<String, AbstractWindow> windows = new HashMap<>();
    protected final Map<String, NavigatorElement> navigatorElements = new HashMap<>();
    protected final Map<String, FormEntity> namedForms = new HashMap<>();
    protected final Map<String, ImplementTable> tables = new HashMap<>();
    protected final Map<Pair<String, Integer>, MetaCodeFragment> metaCodeFragments = new HashMap<>();

    private final Set<FormEntity> unnamedForms = new HashSet<>();
    private final Map<LP<?>, LocalPropertyData> locals = new HashMap<>();


    protected final Map<LAP<?, ?>, List<ResolveClassSet>> propClasses = new HashMap<>();
    

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

    public Iterable<LP<?>> getNamedProperties() {
        return Iterables.concat(namedProperties.values());
    }

    public Iterable<LA<?>> getNamedActions() {
        return Iterables.concat(namedActions.values());
    }

    public Iterable<LP<?>> getNamedProperties(String name) {
        return createEmptyIfNull(namedProperties.get(name));
    }

    public Iterable<LA<?>> getNamedActions(String name) {
        return createEmptyIfNull(namedActions.get(name));
    }

    public Collection<CustomClass> getClasses() {
        return classes.values();
    }
    
    private <T extends LAP<?, ?>> Iterable<T> createEmptyIfNull(Collection<T> col) {
        if (col == null) {
            return Collections.emptyList();
        } else {
            return col;
        }
    }
    
    protected void addModuleLAP(LAP<?, ?> lap) {
        String name = null;
        assert getNamespace().equals(lap.getActionOrProperty().getNamespace());
        if (lap instanceof LA) {
            name = ((LA<?>)lap).action.getName();
            putLAPToMap(namedActions, (LA) lap, name);
        } else if (lap instanceof LP) {
            name = ((LP<?>)lap).property.getName();
            putLAPToMap(namedProperties, (LP)lap, name);
        }
        assert name != null;
    }

    private <T extends LAP<?, ?>> void putLAPToMap(Map<String, List<T>> moduleMap, T lap, String name) {
        if (!moduleMap.containsKey(name)) {
            moduleMap.put(name, new ArrayList<>());
        }
        moduleMap.get(name).add(lap);
    }

    @NFLazy
    protected <P extends PropertyInterface, T extends LAP<P, ?>> void makeActionOrPropertyPublic(T lp, String name, List<ResolveClassSet> signature) {
        lp.getActionOrProperty().setCanonicalName(getNamespace(), name, signature, lp.listInterfaces);
        propClasses.put(lp, signature);
        addModuleLAP(lp);
    }

    protected void makePropertyPublic(LP<?> lp, String name, ResolveClassSet... signature) {
        makePropertyPublic(lp, name, Arrays.asList(signature));
    }
    
    protected void makeActionPublic(LA<?> la, String name, ResolveClassSet... signature) {
        makeActionPublic(la, name, Arrays.asList(signature));
    }

    protected <P extends PropertyInterface> void makePropertyPublic(LP<P> lp, String name, List<ResolveClassSet> signature) {
        makeActionOrPropertyPublic(lp, name, signature);
    }
    
    protected <P extends PropertyInterface> void makeActionPublic(LA<P> la, String name, List<ResolveClassSet> signature) {
        makeActionOrPropertyPublic(la, name, signature);
    }

    public Group getGroup(String name) {
        return groups.get(name);
    }

    protected void addGroup(Group group) {
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
    };
    public Version getVersion() {
        return version;
    }
    
    protected Group addAbstractGroup(String name, LocalizedString caption, Group parent) {
        return addAbstractGroup(name, caption, parent, true);
    }

    protected Group addAbstractGroup(String name, LocalizedString caption, Group parent, boolean toCreateContainer) {
        Group group = new Group(elementCanonicalName(name), caption);
        Version version = getVersion();
        if (parent != null) {
            parent.add(group, version);
        } else if (baseLM.privateGroup != null) {
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
            parents = ListFact.singleton(getBaseClass());
        return parents;
    }

    protected AbstractCustomClass addAbstractClass(String name, LocalizedString caption, ImList<CustomClass> parents) {
        parents = checkEmptyParents(parents);
        AbstractCustomClass customClass = new AbstractCustomClass(elementCanonicalName(name), caption, getVersion(), parents);
        storeCustomClass(customClass);
        return customClass;
    }

    protected ImplementTable addTable(String name, boolean isFull, boolean isExplicit, DBNamingPolicy namingPolicy, ValueClass... classes) {
        String canonicalName = elementCanonicalName(name);
        String dbName = namingPolicy.transformTableCNToDBName(canonicalName);
        ImplementTable table = baseLM.tableFactory.include(dbName, getVersion(), classes);
        table.setCanonicalName(canonicalName);
        addModuleTable(table);
        
        if(isFull)
            table.markedFull = true;
        else
            table.markedExplicit = isExplicit;
        return table;
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

    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////
    /// Properties
    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////

    // ------------------- DATA ----------------- //

    protected LP addDProp(LocalizedString caption, ValueClass value, ValueClass... params) {
        StoredDataProperty dataProperty = new StoredDataProperty(caption, params, value);
        LP lp = addProperty(null, new LP<>(dataProperty));
        return lp;
    }

    // ------------------- Loggable ----------------- //

    private <D extends PropertyInterface> Pair<ValueClass[], ValueClass> getSignature(LP<D> derivedProp, int whereNum, Object[] params) {
        // придется создавать Join свойство чтобы считать его класс
        int dersize = getIntNum(params);
        ImOrderSet<JoinProperty.Interface> listInterfaces = JoinProperty.getInterfaces(dersize);

        final ImList<PropertyInterfaceImplement<JoinProperty.Interface>> list = readCalcImplements(listInterfaces, params);

        assert whereNum == list.size() - 1; // один ON CHANGED, то есть union делать не надо (выполняется, так как только в addLProp работает)

        AndFormulaProperty andProperty = new AndFormulaProperty(list.size());
        ImMap<AndFormulaProperty.Interface, PropertyInterfaceImplement<JoinProperty.Interface>> mapImplement =
                MapFact.addExcl(
                        andProperty.andInterfaces.mapValues(new IntFunction<PropertyInterfaceImplement<JoinProperty.Interface>>() {
                            public PropertyInterfaceImplement<JoinProperty.Interface> apply(int i) {
                                return list.get(i);
                            }
                        }), andProperty.objectInterface, mapCalcListImplement(derivedProp, listInterfaces));

        JoinProperty<AndFormulaProperty.Interface> joinProperty = new JoinProperty<>(LocalizedString.NONAME, listInterfaces,
                new PropertyImplement<>(andProperty, mapImplement));
        LP<JoinProperty.Interface> listProperty = new LP<>(joinProperty, listInterfaces);

        // получаем классы
        ValueClass[] commonClasses = listProperty.getInterfaceClasses(ClassType.logPolicy); // есть и другие obsolete использования
        ValueClass valueClass = listProperty.property.getValueClass(ClassType.logPolicy);
        return new Pair<>(commonClasses, valueClass);
    }

    // ------------------- Scripted DATA ----------------- //

    protected LP addSDProp(LocalizedString caption, boolean isLocalScope, ValueClass value, LocalNestedType nestedType, ValueClass... params) {
        return addSDProp(null, false, caption, isLocalScope, value, nestedType, params);
    }

    protected LP addSDProp(Group group, boolean persistent, LocalizedString caption, boolean isLocalScope, ValueClass value, LocalNestedType nestedType, ValueClass... params) {
        SessionDataProperty prop = new SessionDataProperty(caption, params, value);
        if (isLocalScope) {
            prop.setLocal(true);
        }
        prop.nestedType = nestedType;
        return addProperty(group, new LP<>(prop));
    }

    // ------------------- Form actions ----------------- //


    protected LA addFormAProp(LocalizedString caption, CustomClass cls, LA action) {
        return addIfAProp(caption, is(cls), 1, action, 1); // по идее можно просто exec сделать, но на всякий случай
    }

    protected LA addEditAProp(LocalizedString caption, CustomClass cls) {
        cls.markUsed(true);
        return addFormAProp(caption, cls, baseLM.getFormEdit());
    }

    protected LA addDeleteAProp(LocalizedString caption, CustomClass cls) {
        return addFormAProp(caption, cls, baseLM.getFormDelete());
    }

    // loggable, security, drilldown
    public LA addMFAProp(LocalizedString caption, FormEntity form, ImOrderSet<ObjectEntity> objectsToSet, boolean newSession) {
        return addMFAProp(null, caption, form, objectsToSet, newSession);
    }
    public LA addMFAProp(Group group, LocalizedString caption, FormEntity form, ImOrderSet<ObjectEntity> objectsToSet, boolean newSession) {
        return addIFAProp(group, caption, form, objectsToSet, newSession ? FormSessionScope.NEWSESSION : FormSessionScope.OLDSESSION, true, WindowFormType.FLOAT, false);
    }

    protected <O extends ObjectSelector> LA addIFAProp(Group group, LocalizedString caption, FormSelector<O> form, ImOrderSet<O> objectsToSet, FormSessionScope scope, boolean syncType, WindowFormType windowType, boolean forbidDuplicate) {
        return addIFAProp(group, caption, form, objectsToSet, ListFact.toList(false, objectsToSet.size()), scope, ManageSessionType.AUTO, FormEntity.DEFAULT_NOCANCEL, SetFact.EMPTYORDER(), SetFact.EMPTY(), syncType, windowType, null, forbidDuplicate, false, false);
    }

    public <P extends PropertyInterface, O extends ObjectSelector, X extends PropertyInterface> LA<ClassPropertyInterface> addIFAProp(Group group, LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls, FormSessionScope scope, ManageSessionType manageSession, Boolean noCancel, ImOrderSet<P> orderInterfaces, ImSet<ContextFilterSelector<P, O>> contextProperties, Boolean syncType, WindowFormType windowType, Integer inContainerId, boolean forbidDuplicate, boolean checkOnOk, boolean readonly) {
        FormInteractiveAction<O> formAction = new FormInteractiveAction<>(caption, form, objectsToSet, nulls, ListFact.EMPTY(), ListFact.EMPTY(), ListFact.EMPTY(), orderInterfaces, contextProperties, null, manageSession, noCancel, syncType, windowType, inContainerId, forbidDuplicate, checkOnOk, readonly);

        ImOrderSet<ClassPropertyInterface> listInterfaces = formAction.getFriendlyOrderInterfaces();

        ActionMapImplement<X, ClassPropertyInterface> formImplement = (ActionMapImplement<X, ClassPropertyInterface>) PropertyFact.createSessionScopeAction(scope, formAction.interfaces, formAction.getImplement(), caption, SetFact.EMPTY());

        return addAction(group, new LA<>(formImplement.action, listInterfaces.mapOrder(formImplement.mapping.reverse())));
    }
    protected <O extends ObjectSelector> LA<?> addPFAProp(Group group, LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls,
                                                          ImOrderSet<PropertyInterface> orderContextInterfaces, ImSet<ContextFilterSelector<PropertyInterface, O>> contextFilters,
                                                          FormPrintType staticType, boolean server, boolean autoPrint, boolean syncType, Integer selectTop, LP targetProp, boolean removeNullsAndDuplicates,
                                                          ValueClass printer, ValueClass sheetName, ValueClass password) {
        return addAction(group, new LA<>(new PrintAction<>(caption, form, objectsToSet, nulls, orderContextInterfaces, contextFilters,
                staticType, server, syncType, autoPrint, selectTop, targetProp, baseLM.formPageCount, removeNullsAndDuplicates, printer, sheetName, password)));
    }
    protected <O extends ObjectSelector> LP addJSONFormProp(Group group, LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls,
                                                       ImOrderSet<PropertyInterface> orderContextInterfaces, ImSet<ContextFilterSelector<PropertyInterface, O>> contextFilters) {
        JSONProperty<O> property = new JSONProperty<O>(caption, form, objectsToSet, nulls, orderContextInterfaces, contextFilters);

        return addProperty(group, new LP<>(property));
    }
    protected <O extends ObjectSelector> LA addEFAProp(Group group, LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls,
                                                       ImOrderSet<PropertyInterface> orderContextInterfaces, ImSet<ContextFilterSelector<PropertyInterface, O>> contextFilters,
                                                       FormIntegrationType staticType, boolean noHeader, String separator, boolean noEscape, Integer selectTop, String charset,
                                                       LP singleExportFile, ImMap<GroupObjectEntity, LP> exportFiles, ValueClass root, ValueClass tag) {
        ExportAction<O> exportAction;
        switch(staticType) {
            case XML:
                exportAction = new ExportXMLAction<>(caption, form, objectsToSet, nulls, orderContextInterfaces, contextFilters, staticType, singleExportFile, selectTop, charset, root, tag);
                break;
            case JSON:
                exportAction = new ExportJSONAction<>(caption, form, objectsToSet, nulls, orderContextInterfaces, contextFilters, staticType, singleExportFile, selectTop, charset);
                break;
            case CSV:
                exportAction = new ExportCSVAction<>(caption, form, objectsToSet, nulls, orderContextInterfaces, contextFilters, staticType, exportFiles, selectTop, charset, noHeader, separator, noEscape);
                break;
            case XLS:
                exportAction = new ExportXLSAction<>(caption, form, objectsToSet, nulls, orderContextInterfaces, contextFilters, staticType, exportFiles, selectTop, charset, false, noHeader);
                break;
            case XLSX:
                exportAction = new ExportXLSAction<>(caption, form, objectsToSet, nulls, orderContextInterfaces, contextFilters, staticType, exportFiles, selectTop, charset, true, noHeader);
                break;
            case DBF:
                exportAction = new ExportDBFAction<>(caption, form, objectsToSet, nulls, orderContextInterfaces, contextFilters, staticType, exportFiles, selectTop, charset);
                break;
            case TABLE:
                exportAction = new ExportTableAction<>(caption, form, objectsToSet, nulls, orderContextInterfaces, contextFilters, staticType, exportFiles, selectTop, charset);
                break;
            default:
                throw new UnsupportedOperationException();                
        }
        return addAction(group, new LA<>(exportAction));
    }

    protected <O extends ObjectSelector> LA addAutoImportFAProp(FormEntity formEntity, int paramsCount, ImOrderSet<GroupObjectEntity> groupFiles, boolean sheetAll, String separator,
                                                                boolean noHeader, boolean noEscape, String charset, boolean hasRoot, boolean hasWhere, Boolean isPlain) {
        // getExtension(FILE(prm1))
        // FOR x = getExtension(prm1) DO {
        //    CASE EXCLUSIVE
        //          WHEN x = type.getExtension
        //              IMPORT type form...
        // }
        
        Object[] cases = new Object[0];
        for(FormIntegrationType importType : FormIntegrationType.values()) 
            if(isPlain == null || (isPlain.equals(importType.isPlain()))) {
                cases = add(cases, add(new Object[] {addJProp(baseLM.equals2, 1, addCProp(StringClass.text, LocalizedString.create(importType.getExtension(), false))), paramsCount + 1 }, // WHEN x = type.getExtension()
                    directLI(addImportFAProp(importType, formEntity, paramsCount, groupFiles, sheetAll, separator, noHeader, noEscape, charset, hasRoot, hasWhere, isPlain)))); // IMPORT type form...
            }        
        
        return addForAProp(LocalizedString.create("{logics.add}"), false, false, false, false, paramsCount, null, false, true, 0, false,
                add(add(getUParams(paramsCount), 
                        new Object[] {addJProp(baseLM.equals2, 1, baseLM.getExtension, 2), paramsCount + 1, 1}), // FOR x = getExtension(FILE(prm1))  
                        directLI(addCaseAProp(true, cases))));  // CASE EXCLUSIVE
    }
    
    protected <O extends ObjectSelector> LA addImportFAProp(FormIntegrationType format, FormEntity formEntity, int paramsCount, ImOrderSet<GroupObjectEntity> groupFiles, boolean sheetAll,
                                                            String separator, boolean noHeader, boolean noEscape, String charset, boolean hasRoot, boolean hasWhere, Boolean isPlain) {
        ImportAction importAction;

        if(format == null)
            return addAutoImportFAProp(formEntity, paramsCount, groupFiles, sheetAll, separator, noHeader, noEscape, charset, hasRoot, hasWhere, isPlain);

        switch (format) {
            // hierarchical
            case XML:
                importAction = new ImportXMLAction(paramsCount, formEntity, charset, hasRoot, hasWhere);
                break;
            case JSON:
                importAction = new ImportJSONAction(paramsCount, formEntity, charset, hasRoot, hasWhere);
                break;
            // plain
            case CSV:
                importAction = new ImportCSVAction(paramsCount, groupFiles, formEntity, charset, hasWhere, noHeader, noEscape, separator);
                break;
            case DBF:
                importAction = new ImportDBFAction(paramsCount, groupFiles, formEntity, charset, hasWhere);
                break;
            case XLS:
            case XLSX:
                importAction = new ImportXLSAction(paramsCount, groupFiles, formEntity, charset, hasWhere, noHeader, sheetAll);
                break;
            case TABLE:
                importAction = new ImportTableAction(paramsCount, groupFiles, formEntity, charset, hasWhere);
                break;
            default:
                throw new UnsupportedOperationException();
        }
        return addAction(null, new LA<>(importAction));
    }

    // ------------------- Change Class action ----------------- //

    protected LA addChangeClassAProp(ConcreteObjectClass cls, int resInterfaces, int changeIndex, boolean extendedContext, boolean conditional, Object... params) {
        int innerIntCnt = resInterfaces + (extendedContext ? 1 : 0);
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(innerIntCnt);
        ImOrderSet<PropertyInterface> mappedInterfaces = extendedContext ? innerInterfaces.removeOrderIncl(innerInterfaces.get(changeIndex)) : innerInterfaces;
        ImList<PropertyInterfaceImplement<PropertyInterface>> readImplements = readCalcImplements(innerInterfaces, params);
        PropertyMapImplement<PropertyInterface, PropertyInterface> conditionalPart = (PropertyMapImplement<PropertyInterface, PropertyInterface>)
                (conditional ? readImplements.get(resInterfaces) : null);

        return addAProp(new ChangeClassAction<>(cls, false, innerInterfaces.getSet(),
                mappedInterfaces, innerInterfaces.get(changeIndex), conditionalPart, getBaseClass()));
    }

    private static class IntegrationForm {
        public final IntegrationFormEntity<PropertyInterface> form;
        public final ImOrderSet<ObjectEntity> objectsToSet;
        public final ImList<Boolean> nulls;

        public IntegrationForm(IntegrationFormEntity<PropertyInterface> form, ImOrderSet<ObjectEntity> objectsToSet, ImList<Boolean> nulls) {
            this.form = form;
            this.objectsToSet = objectsToSet;
            this.nulls = nulls;
        }
    }

    private IntegrationForm addIntegrationForm(int resInterfaces, List<String> aliases, List<Boolean> literals, ImOrderMap<String, Boolean> orders, boolean hasWhere, Object[] params) throws FormEntity.AlreadyDefined {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(getIntNum(params));
        ImList<PropertyInterfaceImplement<PropertyInterface>> readImplements = readCalcImplements(innerInterfaces, params);
        final ImList<PropertyInterfaceImplement<PropertyInterface>> exprs = readImplements.subList(resInterfaces, readImplements.size() - (hasWhere ? 1 : 0));
        ImOrderSet<PropertyInterface> mapInterfaces = BaseUtils.immutableCast(readImplements.subList(0, resInterfaces).toOrderExclSet());

        // determining where
        PropertyInterfaceImplement<PropertyInterface> where = hasWhere ? readImplements.get(readImplements.size() - 1) : null;
        where = PropertyFact.getFullWhereProperty(innerInterfaces.getSet(), mapInterfaces.getSet(), where, exprs.getCol());

        // creating form
        IntegrationFormEntity<PropertyInterface> form = new IntegrationFormEntity<>(baseLM, innerInterfaces, null, mapInterfaces, aliases, literals, exprs, where, orders, false, version);
        addAutoFormEntity(form);

        ImOrderSet<ObjectEntity> objectsToSet = mapInterfaces.mapOrder(form.mapObjects);
        ImList<Boolean> nulls = ListFact.toList(true, mapInterfaces.size());

        return new IntegrationForm(form, objectsToSet, nulls);
    }

    protected LP addJSONProp(LocalizedString caption, int resInterfaces, List<String> aliases, List<Boolean> literals, ImOrderMap<String, Boolean> orders,
                                        boolean hasWhere, Object... params) throws FormEntity.AlreadyDefined {
        IntegrationForm integrationForm = addIntegrationForm(resInterfaces, aliases, literals, orders, hasWhere, params);

        // creating action
        return addJSONFormProp(null, caption, integrationForm.form, integrationForm.objectsToSet, integrationForm.nulls, SetFact.EMPTYORDER(), SetFact.EMPTY());
    }

    // ------------------- Export property action ----------------- //
    protected LA addExportPropertyAProp(LocalizedString caption, FormIntegrationType type, int resInterfaces, List<String> aliases, List<Boolean> literals, ImOrderMap<String, Boolean> orders,
                                        LP singleExportFile, boolean hasWhere, ValueClass root, ValueClass tag, String separator,
                                        boolean noHeader, boolean noEscape, Integer selectTop, String charset, boolean attr, Object... params) throws FormEntity.AlreadyDefined {
        IntegrationForm integrationForm = addIntegrationForm(resInterfaces, aliases, literals, orders, hasWhere, params);
        IntegrationFormEntity<PropertyInterface> form = integrationForm.form;

        ImMap<GroupObjectEntity, LP> exportFiles = MapFact.EMPTY();
        if(type.isPlain()) {
            exportFiles = MapFact.singleton(form.groupObject == null ? GroupObjectEntity.NULL : form.groupObject, singleExportFile);
            singleExportFile = null;
        }            
                
        // creating action
        return addEFAProp(null, caption, form, integrationForm.objectsToSet, integrationForm.nulls, SetFact.EMPTYORDER(), SetFact.EMPTY(), type, noHeader, separator, noEscape, selectTop, charset, singleExportFile, exportFiles, root, tag);
    }

    protected LA addImportPropertyAProp(FormIntegrationType type, int paramsCount, List<String> aliases, List<Boolean> literals, ImList<ValueClass> paramClasses, LP<?> whereLCP,
                                        String separator, boolean noHeader, boolean noEscape, String charset, boolean sheetAll, boolean attr, boolean hasRoot, boolean hasWhere, Object... params) throws FormEntity.AlreadyDefined {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(getIntNum(params));
        ImList<PropertyInterfaceImplement<PropertyInterface>> exprs = readCalcImplements(innerInterfaces, params);

        // determining where
        PropertyInterfaceImplement<PropertyInterface> where = innerInterfaces.size() == 1 && whereLCP != null ? whereLCP.getImplement(innerInterfaces.single()) : null;

        // creating form
        IntegrationFormEntity<PropertyInterface> form = new IntegrationFormEntity<>(baseLM, innerInterfaces, paramClasses, SetFact.EMPTYORDER(), aliases, literals, exprs, where, MapFact.EMPTYORDER(), attr, version);
        addAutoFormEntity(form);
        
        // create action
        return addImportFAProp(type, form, paramsCount, SetFact.singletonOrder(form.groupObject == null ? GroupObjectEntity.NULL : form.groupObject), sheetAll, separator, noHeader, noEscape, charset, hasRoot, hasWhere, null);
    }

    // ------------------- Set property action ----------------- //

    protected <C extends PropertyInterface, W extends PropertyInterface> LA addSetPropertyAProp(Group group, LocalizedString caption, int resInterfaces,
                                                                                                boolean conditional, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(getIntNum(params));
        ImList<PropertyInterfaceImplement<PropertyInterface>> readImplements = readCalcImplements(innerInterfaces, params);
        PropertyMapImplement<W, PropertyInterface> conditionalPart = (PropertyMapImplement<W, PropertyInterface>)
                (conditional ? readImplements.get(resInterfaces + 2) : PropertyFact.createTrue());
        return addAction(group, new LA<>(new SetAction<C, W, PropertyInterface>(caption,
                innerInterfaces.getSet(), (ImOrderSet) readImplements.subList(0, resInterfaces).toOrderExclSet(), conditionalPart,
                (PropertyMapImplement<C, PropertyInterface>) readImplements.get(resInterfaces), readImplements.get(resInterfaces + 1))));
    }

    protected <C extends PropertyInterface, W extends PropertyInterface> LA addRecalculatePropertyAProp(int resInterfaces, boolean hasWhere, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(getIntNum(params));
        ImList<PropertyInterfaceImplement<PropertyInterface>> readImplements = readCalcImplements(innerInterfaces, params);
        PropertyMapImplement<W, PropertyInterface> where = hasWhere ? (PropertyMapImplement<W, PropertyInterface>) (readImplements.get(resInterfaces + 1)) : null;
        return addAction(null, new LA<>(new RecalculatePropertyAction<C, PropertyInterface>(LocalizedString.NONAME, innerInterfaces.getSet(),
                (ImOrderSet) readImplements.subList(0, resInterfaces).toOrderExclSet(), (PropertyMapImplement<C, PropertyInterface>) readImplements.get(resInterfaces), where)));
    }

    // ------------------- List action ----------------- //

    protected LA addListAProp(Object... params) {
        return addListAProp(SetFact.EMPTY(), params);
    }
    protected LA addListAProp(ImSet<SessionDataProperty> localsInScope, Object... params) {
        return addListAProp(null, 0, LocalizedString.NONAME, localsInScope, params);
    }
    protected LA addListAProp(int removeLast, Object... params) {
        return addListAProp(null, removeLast, LocalizedString.NONAME, SetFact.EMPTY(), params);
    }
    protected LA addListAProp(LocalizedString caption, Object... params) {
        return addListAProp(null, 0, caption, SetFact.EMPTY(), params);        
    }
    protected LA addListAProp(Group group, int removeLast, LocalizedString caption, ImSet<SessionDataProperty> localsInScope, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        return addAction(group, new LA<>(new ListAction(caption, listInterfaces,
                readActionImplements(listInterfaces, removeLast > 0 ? Arrays.copyOf(params, params.length - removeLast) : params), localsInScope)));
    }

    protected LA addAbstractListAProp(boolean isChecked, boolean isLast, ValueClass[] params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(params.length);
        return addAction(null, new LA<>(new ListAction(LocalizedString.NONAME, isChecked, isLast, listInterfaces, listInterfaces.mapList(ListFact.toList(params)))));
    }

    // ------------------- Try action ----------------- //

    protected LA addTryAProp(Group group, LocalizedString caption, boolean hasCatch, boolean hasFinally, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<ActionOrPropertyInterfaceImplement> readImplements = readImplements(listInterfaces, params);
        assert readImplements.size() >= 1 && readImplements.size() <= 3;

        ActionMapImplement<?, PropertyInterface> tryAction = (ActionMapImplement<?, PropertyInterface>) readImplements.get(0);
        ActionMapImplement<?, PropertyInterface> catchAction = (ActionMapImplement<?, PropertyInterface>) (hasCatch ? readImplements.get(1) : null);
        ActionMapImplement<?, PropertyInterface> finallyAction = (ActionMapImplement<?, PropertyInterface>) (hasFinally ? (readImplements.get(hasCatch ? 2 : 1)) : null);
        return addAction(group, new LA<>(new TryAction(caption, listInterfaces, tryAction, catchAction, finallyAction)));
    }
    
    // ------------------- If action ----------------- //

    protected LA addIfAProp(Object... params) {
        return addIfAProp(null, LocalizedString.NONAME, false, params);
    }

    protected LA addIfAProp(LocalizedString caption, Object... params) {
        return addIfAProp(null, caption, false, params);
    }

    protected LA addIfAProp(Group group, LocalizedString caption, boolean not, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<ActionOrPropertyInterfaceImplement> readImplements = readImplements(listInterfaces, params);
        assert readImplements.size() >= 2 && readImplements.size() <= 3;

        return addAction(group, new LA(CaseAction.createIf(caption, not, listInterfaces, (PropertyInterfaceImplement<PropertyInterface>) readImplements.get(0),
                (ActionMapImplement<?, PropertyInterface>) readImplements.get(1), readImplements.size() == 3 ? (ActionMapImplement<?, PropertyInterface>) readImplements.get(2) : null)));
    }

    // ------------------- Case action ----------------- //

    protected LA addCaseAProp(boolean isExclusive, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<ActionOrPropertyInterfaceImplement> readImplements = readImplements(listInterfaces, params);

        MList<ActionCase<PropertyInterface>> mCases = ListFact.mList();
        for (int i = 0; i*2+1 < readImplements.size(); i++) {
            mCases.add(new ActionCase<>((PropertyMapImplement<?, PropertyInterface>) readImplements.get(i*2), (ActionMapImplement<?, PropertyInterface>) readImplements.get(i*2+1)));
        }
        if(readImplements.size() % 2 != 0) {
            mCases.add(new ActionCase<>(PropertyFact.createTrue(), (ActionMapImplement<?, PropertyInterface>) readImplements.get(readImplements.size() - 1)));
        }
        return addAction(null, new LA<>(new CaseAction(LocalizedString.NONAME, isExclusive, listInterfaces, mCases.immutableList())));
    }

    protected LA addMultiAProp(boolean isExclusive, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<ActionOrPropertyInterfaceImplement> readImplements = readImplements(listInterfaces, params);

        MList<ActionMapImplement> mCases = ListFact.mList();
        for (int i = 0; i < readImplements.size(); i++) {
            mCases.add((ActionMapImplement) readImplements.get(i));
        }
        return addAction(null, new LA<>(new CaseAction(LocalizedString.NONAME, isExclusive, mCases.immutableList(), listInterfaces)));
    }

    protected LA addAbstractCaseAProp(ListCaseAction.AbstractType type, boolean isExclusive, boolean isChecked, boolean isLast, ValueClass[] params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(params.length);
        return addAction(null, new LA<>(new CaseAction(LocalizedString.NONAME, isExclusive, isChecked, isLast, type, listInterfaces, listInterfaces.mapList(ListFact.toList(params)))));
    }

    // ------------------- For action ----------------- //

    protected LA addForAProp(LocalizedString caption, boolean ascending, boolean ordersNotNull, boolean recursive, boolean hasElse, int resInterfaces, CustomClass addClass, boolean autoSet, boolean hasCondition, int noInline, boolean forceInline, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(getIntNum(params));
        ImList<ActionOrPropertyInterfaceImplement> readImplements = readImplements(innerInterfaces, params);

        int implCnt = readImplements.size();

        ImOrderSet<PropertyInterface> mapInterfaces = BaseUtils.immutableCast(readImplements.subList(0, resInterfaces).toOrderExclSet());

        PropertyMapImplement<?, PropertyInterface> ifProp = hasCondition? (PropertyMapImplement<?, PropertyInterface>) readImplements.get(resInterfaces) : null;

        ImOrderMap<PropertyInterfaceImplement<PropertyInterface>, Boolean> orders =
                BaseUtils.<ImList<PropertyInterfaceImplement<PropertyInterface>>>immutableCast(readImplements.subList(resInterfaces + (hasCondition ? 1 : 0), implCnt - (hasElse ? 2 : 1) - (addClass != null ? 1: 0) - noInline)).toOrderExclSet().toOrderMap(!ascending);

        PropertyInterface addedInterface = addClass!=null ? (PropertyInterface) readImplements.get(implCnt - (hasElse ? 3 : 2) - noInline) : null;

        ActionMapImplement<?, PropertyInterface> elseAction =
                !hasElse ? null : (ActionMapImplement<?, PropertyInterface>) readImplements.get(implCnt - 2 - noInline);

        ActionMapImplement<?, PropertyInterface> action =
                (ActionMapImplement<?, PropertyInterface>) readImplements.get(implCnt - 1 - noInline);

        ImSet<PropertyInterface> noInlineInterfaces = BaseUtils.<ImList<PropertyInterface>>immutableCast(readImplements.subList(implCnt - noInline, implCnt)).toOrderExclSet().getSet();

        return addAction(null, new LA<>(
                new ForAction<>(caption, innerInterfaces.getSet(), mapInterfaces, ifProp, orders, ordersNotNull, action, elseAction, addedInterface, addClass, autoSet, recursive, noInlineInterfaces, forceInline))
        );
    }

    // ------------------- JOIN ----------------- //

    public LA addJoinAProp(LA action, Object... params) {
        return addJoinAProp(LocalizedString.NONAME, action, params);
    }

    protected LA addJoinAProp(LocalizedString caption, LA action, Object... params) {
        return addJoinAProp(null, caption, action, params);
    }

    protected LA addJoinAProp(Group group, LocalizedString caption, LA action, Object... params) {
        return addJoinAProp(group, caption, null, action, params);
    }

    protected LA addJoinAProp(Group group, LocalizedString caption, ValueClass[] classes, LA action, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<PropertyInterfaceImplement<PropertyInterface>> readImplements = readCalcImplements(listInterfaces, params);
        return addAction(group, new LA(new JoinAction(caption, listInterfaces, mapActionImplement(action, readImplements))));
    }

    // ------------------------ APPLY / CANCEL ----------------- //

    protected LA addApplyAProp(Group group, LocalizedString caption, LA action, boolean singleApply,
                               FunctionSet<SessionDataProperty> keepSessionProps, boolean serializable) {
        
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(action.listInterfaces.size());
        ActionMapImplement<?, PropertyInterface> actionImplement = mapActionListImplement(action, listInterfaces);

        ApplyAction applyAction = new ApplyAction(caption, listInterfaces, actionImplement, keepSessionProps, serializable, baseLM.getCanceled().property, baseLM.getApplyMessage().property);
        actionImplement.action.singleApply = singleApply;
        return addAction(group, new LA<>(applyAction));
    }

    protected LA addCancelAProp(Group group, LocalizedString caption, FunctionSet<SessionDataProperty> keepSessionProps) {

        CancelAction applyAction = new CancelAction(caption, keepSessionProps);
        return addAction(group, new LA<>(applyAction));
    }

    // ------------------- NEWSESSION ----------------- //

    public LA addNewSessionAProp(Group group,
                                    LA la, boolean isNested, boolean singleApply, boolean newSQL,
                                    FunctionSet<SessionDataProperty> migrateSessionProps) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(la.listInterfaces.size());
        ActionMapImplement<?, PropertyInterface> actionImplement = mapActionListImplement(la, listInterfaces);

        NewSessionAction action = new NewSessionAction(
                LocalizedString.NONAME, listInterfaces, actionImplement, singleApply, newSQL, migrateSessionProps, isNested);
        
        return addAction(group, new LA<>(action));
    }

    protected LA addNewThreadAProp(Group group, LocalizedString caption, boolean withConnection, boolean hasPeriod, boolean hasDelay, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<ActionOrPropertyInterfaceImplement> readImplements = readImplements(listInterfaces, params);
        PropertyInterfaceImplement connection = withConnection ? (PropertyInterfaceImplement) readImplements.get(1) : null;
        PropertyInterfaceImplement period = hasPeriod ? (PropertyInterfaceImplement) readImplements.get(1) : null;
        PropertyInterfaceImplement delay = hasDelay ? (PropertyInterfaceImplement) readImplements.get(hasPeriod ? 2 : 1) : null;
        return addAction(group, new LA(new NewThreadAction(caption, listInterfaces, (ActionMapImplement) readImplements.get(0), period, delay, connection)));
    }

    protected LA addNewExecutorAProp(Group group, LocalizedString caption, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<ActionOrPropertyInterfaceImplement> readImplements = readImplements(listInterfaces, params);
        return addAction(group, new LA(new NewExecutorAction(caption, listInterfaces,
                (ActionMapImplement) readImplements.get(0), (PropertyInterfaceImplement) readImplements.get(1))));
    }

    // ------------------- Request action ----------------- //

    protected LA addRequestAProp(Group group, boolean hasDo, LocalizedString caption, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<ActionOrPropertyInterfaceImplement> readImplements = readImplements(listInterfaces, params);
        
        ActionMapImplement<?, PropertyInterface> doAction = hasDo ? (ActionMapImplement<?, PropertyInterface>) readImplements.get(1) : null;
        ActionMapImplement<?, PropertyInterface> elseAction =  readImplements.size() == (2 + (hasDo ? 1 : 0)) ? (ActionMapImplement<?, PropertyInterface>) readImplements.get(hasDo ? 2 : 1) : null;
        return addAction(group, new LA(new RequestAction(caption, listInterfaces,
                (ActionMapImplement<?, PropertyInterface>) readImplements.get(0), doAction, elseAction))
        );
    }

    // ------------------- Input ----------------- //

    public <T extends PropertyInterface> LA<?> addInputAProp(ValueClass valueClass, LP targetProp, boolean hasOldValue,
                                                             ImOrderSet<T> orderInterfaces, InputListEntity<?, T> contextList,
                                                             FormSessionScope contextScope, InputFilterSelector<T> filterList,
                                                             ImList<InputContextAction<?, T>> contextActions, String customEditorFunction, boolean notNull) {
        if(contextList != null) {

            if (contextScope == FormSessionScope.NEWSESSION) {
                contextList = contextList.newSession();
            }

            // adding reset action
            if (!notNull) {
                contextActions = ListFact.add(contextActions, InputListEntity.getResetAction(baseLM, targetProp));
            }

            if (valueClass instanceof ConcreteCustomClass) {
                // adding newedit action
                contextActions = ListFact.add(contextList.getNewEditAction(baseLM, (ConcreteCustomClass) valueClass, targetProp, contextScope), contextActions);
            }
        }
        
        return addAction(null, new LA(new InputAction(LocalizedString.create("Input"), valueClass, targetProp, hasOldValue, orderInterfaces, contextList, filterList, contextActions, customEditorFunction)));
    }

    public <T extends PropertyInterface> LA addDialogInputAProp(CustomClass customClass, LP targetProp, FormSessionScope scope, ImOrderSet<T> orderInterfaces, InputListEntity<?, T> list, ImRevMap<T, StaticParamNullableExpr> listMapParamExprs, Function<ObjectEntity, ImSet<ContextFilterEntity<?, T, ObjectEntity>>> filters, String customChangeFunction, boolean notNull) {
//        if (viewProperties.isEmpty() || viewProperties.get(0).getValueClass(ClassType.tryEditPolicy) instanceof CustomClass)
//            viewProperties = ListFact.add(((LP<?>) getBaseLM().addCastProp(ObjectType.idClass)).property, viewProperties); // casting object class to long to provide WYS

        // drop list in auto event actions (inside INPUT operator) if it has not an unique value
        if(list != null && !(list.isDefaultWYSInput() && list.isValueUnique(listMapParamExprs)))
            list = null;

        ClassFormEntity dialogForm = customClass.getDialogForm(baseLM);
        return addDialogInputAProp(dialogForm.form, targetProp, dialogForm.object, true, orderInterfaces, scope, list, BaseUtils.immutableCast(filters.apply(dialogForm.object)), customChangeFunction, notNull);
    }
    
    public <T extends PropertyInterface, O extends ObjectSelector> LA addDialogInputAProp(FormSelector<O> formSelector, LP targetProp, O object, boolean hasOldValue, ImOrderSet<T> orderInterfaces, FormSessionScope scope, InputListEntity<?, T> list, ImSet<ContextFilterSelector<T, O>> filters, String customChangeFunction, boolean notNull) {
        return addDialogInputAProp(formSelector,
                hasOldValue ? ListFact.singleton(object) : ListFact.EMPTY(), hasOldValue ? ListFact.singleton(true) : ListFact.EMPTY(),
                ListFact.singleton(object), ListFact.singleton(targetProp), ListFact.singleton(true), scope, list,
                ManageSessionType.AUTO, FormEntity.DEFAULT_NOCANCEL, orderInterfaces, filters, true, WindowFormType.FLOAT, null, false, false, customChangeFunction, notNull);
    }

    public <P extends PropertyInterface, X extends PropertyInterface, O extends ObjectSelector> LA addDialogInputAProp(FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls, ImList<O> inputObjects, ImList<LP> inputProps, ImList<Boolean> inputNulls, FormSessionScope scope, InputListEntity<?, P> list, ManageSessionType manageSession, Boolean noCancel, ImOrderSet<P> orderInterfaces, ImSet<ContextFilterSelector<P, O>> contextFilters, boolean syncType, WindowFormType windowType, Integer inContainerId, boolean checkOnOk, boolean readonly, String customChangeFunction, boolean notNull) {
        // objects + contextInterfaces
        Result<InputListEntity<?, ClassPropertyInterface>> mappedList = list != null ? new Result<>() : null;
        FormInteractiveAction<O> formAction = new FormInteractiveAction<>(LocalizedString.NONAME, form, objectsToSet, nulls, inputObjects, inputProps, inputNulls, orderInterfaces, contextFilters,
                map -> { if(mappedList != null) mappedList.set(list.map(map)); }, manageSession, noCancel, syncType ? true : null, windowType, inContainerId, false, checkOnOk, readonly);

        ImOrderSet<ClassPropertyInterface> listInterfaces = formAction.getFriendlyOrderInterfaces();

        ActionMapImplement<X, ClassPropertyInterface> formImplement = (ActionMapImplement<X, ClassPropertyInterface>) formAction.getImplement();

        // adding scope
        formImplement = (ActionMapImplement<X, ClassPropertyInterface>) PropertyFact.createSessionScopeAction(scope, formAction.interfaces, formImplement, getMigrateInputProps(inputProps));

        LA<?> resultAction;
        O inputObject;
        // wrapping dialog into input operator
        if(inputObjects.size() == 1 && list != null && form.getBaseClass(inputObject = inputObjects.single()) instanceof CustomClass
                && form.isSingleGroup(inputObject)) { // just like in InputListEntity.mapInner we will ignore the cases when there are not all objects
            LP<?> inputProp = inputProps.single();

            // adding asyncUpdate with list value
            formImplement = (ActionMapImplement<X, ClassPropertyInterface>)
                    PropertyFact.createListAction(formAction.interfaces,
                            formImplement, mappedList.result.getAsyncUpdateAction(baseLM, inputProp.getImplement()));

            InputFilterSelector<ClassPropertyInterface> inputFilter = new FormInputFilterSelector<>(form, formAction.getContextFilterSelectors(), inputObject, formAction.mapObjects);

            // the order will / have to be the same as in formAction itself
            return addInputAProp(form.getBaseClass(inputObject), inputProp, false, listInterfaces,
                    // getting inputList entity with all filters
                    mappedList.result, scope, inputFilter, ListFact.toList(new InputContextAction<>("dialog", "F8", null, null, QuickAccess.DEFAULT, formImplement.action, formImplement.mapping)), customChangeFunction, notNull); // // adding dialog action (no string parameter, but extra parameters)
        }

        resultAction = new LA<>(formImplement.action, listInterfaces.mapOrder(formImplement.mapping.reverse()));

        return addAction(null, resultAction);
    }

    public ImSet<SessionDataProperty> getMigrateInputProps(ImList<LP> inputProps) {
        return inputProps.addList(baseLM.getRequestCanceledProperty()).getCol().mapMergeSetValues(lp -> (SessionDataProperty) lp.property);
    }

    public LP getRequestedValueProperty(ValueClass valueClass) {
        return baseLM.getRequestedValueProperty().getLP(valueClass.getType());
    }

    // ------------------- Constant ----------------- //

    protected <T extends PropertyInterface> LP addUnsafeCProp(DataClass valueClass, Object value) {
        ValueProperty.checkLocalizedString(value, valueClass);
        return baseLM.addCProp(valueClass, valueClass instanceof StringClass ? value : valueClass.read(value));
    }

    protected <T extends PropertyInterface> LP addCProp(StaticClass valueClass, Object value) {
        return baseLM.addCProp(valueClass, value);
    }

    // ------------------- Random ----------------- //

    protected LP addRMProp(LocalizedString caption) {
        return addProperty(null, new LP<>(new RandomFormulaProperty(caption)));
    }

    // ------------------- FORMULA ----------------- //

    protected LP addSFProp(String formula, int paramCount) {
        return addSFProp(formula, null, paramCount);
    }

    protected LP addSFProp(CustomFormulaSyntax formula, int paramCount, boolean hasNotNull) {
        return addSFProp(formula, null, paramCount, hasNotNull);
    }

    protected LP addSFProp(String formula, DataClass value, int paramCount) {
        return addSFProp(new CustomFormulaSyntax(formula), value, paramCount, false);
    }
    
    protected LP addSFProp(CustomFormulaSyntax formula, DataClass value, int paramCount, boolean hasNotNull) {
        return addProperty(null, new LP<>(new StringFormulaProperty(value, formula, paramCount, hasNotNull)));
    }

    // ------------------- Операции сравнения ----------------- //

    protected LP addCFProp(Compare compare) {
        return addProperty(null, new LP<>(new CompareFormulaProperty(compare)));
    }

    // ------------------- Алгебраические операции ----------------- //

    protected LP addSumProp() {
        return baseLM.addSumProp();
    }

    protected LP addMultProp() {
        return baseLM.addMultProp();
    }

    protected LP addSubtractProp() {
        return baseLM.addSubtractProp();
    }

    protected LP addDivideProp() {
        return baseLM.addDivideProp();
    }

    protected LP addRoundProp(boolean hasScale) {
        return baseLM.addRoundProp(hasScale);
    }

    // ------------------- cast ----------------- //

    protected <P extends PropertyInterface> LP addCastProp(DataClass castClass) {
        return baseLM.addCastProp(castClass);
    }

    // ------------------- AND ----------------- //

    protected LP addAFProp(boolean... nots) {
        ImOrderSet<PropertyInterface> interfaces = genInterfaces(nots.length + 1);
        MList<Boolean> mList = ListFact.mList(nots.length);
        boolean wasNot = false;
        for(boolean not : nots) {
            mList.add(not);
            wasNot = wasNot || not;
        }
        if(wasNot)
            return mapLProp(null, false, PropertyFact.createAnd(interfaces, mList.immutableList()), interfaces);
        else
            return addProperty(null, new LP<>(new AndFormulaProperty(nots.length)));
    }

    // ------------------- concat ----------------- //

    protected LP addCCProp(int paramCount) {
        return addProperty(null, new LP<>(new ConcatenateProperty(paramCount)));
    }

    protected LP addDCCProp(int paramIndex) {
        return addProperty(null, new LP<>(new DeconcatenateProperty(paramIndex, baseLM.baseClass)));
    }

    // ------------------- JOIN (продолжение) ----------------- //

    public LP addJProp(LP mainProp, Object... params) {
        return addJProp( false, mainProp, params);
    }

    protected LP addJProp(boolean user, LP mainProp, Object... params) {
        return addJProp(user, 0, mainProp, params);
    }
    protected LP addJProp(boolean user, int removeLast, LP<?> mainProp, Object... params) {

        ImOrderSet<JoinProperty.Interface> listInterfaces = JoinProperty.getInterfaces(getIntNum(params));
        ImList<PropertyInterfaceImplement<JoinProperty.Interface>> listImplements = readCalcImplements(listInterfaces, removeLast > 0 ? Arrays.copyOf(params, params.length - removeLast) : params);
        JoinProperty<?> property = new JoinProperty(LocalizedString.NONAME, listInterfaces, user,
                mapCalcImplement(mainProp, listImplements));

        for(Property andProp : mainProp.property.getAndProperties())
            property.drawOptions.inheritDrawOptions(andProp.drawOptions);

        return addProperty(null, new LP<>(property, listInterfaces));
    }

    // ------------------- mapLProp ----------------- //

    private <P extends PropertyInterface> LP mapLProp(Group group, boolean persistent, PropertyMapImplement<?, P> implement, ImOrderSet<P> listInterfaces) {
        return addProperty(group, new LP(implement.property, listInterfaces.mapOrder(implement.mapping.reverse())));
    }

    protected <P extends PropertyInterface> LP mapLProp(Group group, boolean persistent, PropertyMapImplement<?, P> implement, LP<P> property) {
        return mapLProp(group, persistent, implement, property.listInterfaces);
    }

    private <P extends PropertyInterface> LP mapLGProp(Group group, PropertyImplement<?, PropertyInterfaceImplement<P>> implement, ImList<PropertyInterfaceImplement<P>> listImplements) {
        return mapLGProp(group, false, implement, listImplements);
    }

    private <P extends PropertyInterface> LP mapLGProp(Group group, boolean persistent, PropertyImplement<?, PropertyInterfaceImplement<P>> implement, ImList<PropertyInterfaceImplement<P>> listImplements) {
        return addProperty(group, new LP(implement.property, listImplements.toOrderExclSet().mapOrder(implement.mapping.toRevExclMap().reverse())));
    }

    private <P extends PropertyInterface> LP mapLGProp(Group group, boolean persistent, GroupProperty property, ImList<PropertyInterfaceImplement<P>> listImplements) {
        return mapLGProp(group, persistent, new PropertyImplement<GroupProperty.Interface<P>, PropertyInterfaceImplement<P>>(property, property.getMapInterfaces()), listImplements);
    }

    // ------------------- Order property ----------------- //

    protected <P extends PropertyInterface> LP addOProp(Group group, boolean persistent, LocalizedString caption, PartitionType partitionType, boolean ascending, boolean ordersNotNull, boolean includeLast, int partNum, Object... params) {
        ImOrderSet<PropertyInterface> interfaces = genInterfaces(getIntNum(params));
        ImList<PropertyInterfaceImplement<PropertyInterface>> listImplements = readCalcImplements(interfaces, params);

        ImSet<PropertyInterfaceImplement<PropertyInterface>> partitions = listImplements.subList(0, partNum).toOrderSet().getSet();
        ImList<PropertyInterfaceImplement<PropertyInterface>> mainProp = listImplements.subList(partNum, partNum + 1);
        ImOrderMap<PropertyInterfaceImplement<PropertyInterface>, Boolean> orders = listImplements.subList(partNum + 1, listImplements.size()).toOrderSet().toOrderMap(!ascending);

        return mapLProp(group, persistent, PropertyFact.createOProp(caption, partitionType, interfaces.getSet(), mainProp, partitions, orders, ordersNotNull, includeLast), interfaces);
    }

    protected <P extends PropertyInterface> LP addRProp(Group group, boolean persistent, LocalizedString caption, Cycle cycle, ImList<Integer> resInterfaces, ImRevMap<Integer, Integer> mapPrev, Object... params) {
        int innerCount = getIntNum(params);
        final ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(innerCount);
        ImList<PropertyInterfaceImplement<PropertyInterface>> listImplement = readCalcImplements(innerInterfaces, params);

        Function<Integer, PropertyInterface> getInnerInterface = innerInterfaces::get;

        final ImOrderSet<RecursiveProperty.Interface> interfaces = RecursiveProperty.getInterfaces(resInterfaces.size());
        ImRevMap<RecursiveProperty.Interface, PropertyInterface> mapInterfaces = resInterfaces.mapListRevKeyValues(interfaces::get, getInnerInterface);
        ImRevMap<PropertyInterface, PropertyInterface> mapIterate = mapPrev.mapRevKeyValues(getInnerInterface, getInnerInterface); // старые на новые

        PropertyMapImplement<?, PropertyInterface> initial = (PropertyMapImplement<?, PropertyInterface>) listImplement.get(0);
        PropertyMapImplement<?, PropertyInterface> step = (PropertyMapImplement<?, PropertyInterface>) listImplement.get(1);

        assert initial.property.getType() instanceof IntegralClass == (step.property.getType() instanceof IntegralClass);
        if(!(initial.property.getType() instanceof IntegralClass) && (cycle == Cycle.NO || (cycle==Cycle.IMPOSSIBLE && persistent))) {
            PropertyMapImplement<?, PropertyInterface> one = createStatic(1L, LongClass.instance);
            initial = createAnd(innerInterfaces.getSet(), one, initial);
            step = createAnd(innerInterfaces.getSet(), one, step);
        }

        RecursiveProperty<PropertyInterface> property = new RecursiveProperty<>(caption, interfaces, cycle,
                mapInterfaces, mapIterate, initial, step);
        if(cycle==Cycle.NO)
            addConstraint(property.getConstrainedProperty(), property.getConstrainedMessage(), false);

        LP result = new LP<>(property, interfaces);
//        if (convertToLogical)
//            return addJProp(group, name, false, caption, baseLM.notZero, directLI(addProperty(null, persistent, result)));
//        else
            return addProperty(group, result);
    }

    // ------------------- Ungroup property ----------------- //

    protected <L extends PropertyInterface> LP addUGProp(Group group, boolean persistent, boolean over, LocalizedString caption, int intCount, boolean ascending, boolean ordersNotNull, LP<L> ungroup, Object... params) {
        int partNum = ungroup.listInterfaces.size();
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(intCount);
        final ImList<PropertyInterfaceImplement<PropertyInterface>> listImplements = readCalcImplements(innerInterfaces, params);
        ImMap<L, PropertyInterfaceImplement<PropertyInterface>> groupImplement = ungroup.listInterfaces.mapOrderValues(listImplements::get);
        PropertyInterfaceImplement<PropertyInterface> restriction = listImplements.get(partNum);
        ImOrderMap<PropertyInterfaceImplement<PropertyInterface>, Boolean> orders = listImplements.subList(partNum + 1, listImplements.size()).toOrderSet().toOrderMap(!ascending);

        return mapLProp(group, persistent, PropertyFact.createUGProp(caption, innerInterfaces.getSet(),
                new PropertyImplement<>(ungroup.property, groupImplement), orders, ordersNotNull, restriction, over), innerInterfaces);
    }

    protected <L extends PropertyInterface> LP addPGProp(Group group, boolean persistent, int roundlen, boolean roundfirst, LocalizedString caption, int intCount, List<ResolveClassSet> explicitInnerClasses, boolean ascending, boolean ordersNotNull, LP<L> ungroup, Object... params) {
        int partNum = ungroup.listInterfaces.size();
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(intCount);
        final ImList<PropertyInterfaceImplement<PropertyInterface>> listImplements = readCalcImplements(innerInterfaces, params);
        ImMap<L, PropertyInterfaceImplement<PropertyInterface>> groupImplement = ungroup.listInterfaces.mapOrderValues(listImplements::get);
        PropertyInterfaceImplement<PropertyInterface> proportion = listImplements.get(partNum);
        ImOrderMap<PropertyInterfaceImplement<PropertyInterface>, Boolean> orders =
                listImplements.subList(partNum + 1, listImplements.size()).toOrderSet().toOrderMap(!ascending);

        return mapLProp(group, persistent, PropertyFact.createPGProp(caption, roundlen, roundfirst, baseLM.baseClass, innerInterfaces, explicitInnerClasses,
                new PropertyImplement<>(ungroup.property, groupImplement), proportion, orders, ordersNotNull), innerInterfaces);
    }

    /*
      // свойство обратное группируещему - для этого задается ограничивающее свойство, результирующее св-во с группировочными, порядковое св-во
      protected LF addUGProp(Group group, String title, LF maxGroupProp, LF unGroupProp, Object... params) {
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
        return SetFact.toOrderExclSet(interfaces, ActionOrProperty.genInterface);
    }

    // ------------------- GROUP SUM ----------------- //

    protected LP addSGProp(Group group, boolean persistent, boolean notZero, LocalizedString caption, int interfaces, List<ResolveClassSet> explicitInnerClasses, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(interfaces);
        return addSGProp(group, persistent, notZero, caption, innerInterfaces, explicitInnerClasses, readCalcImplements(innerInterfaces, params));
    }

    protected <T extends PropertyInterface> LP addSGProp(Group group, boolean persistent, boolean notZero, LocalizedString caption, ImOrderSet<T> innerInterfaces, List<ResolveClassSet> explicitInnerClasses, ImList<PropertyInterfaceImplement<T>> implement) {
        ImList<PropertyInterfaceImplement<T>> listImplements = implement.subList(1, implement.size());
        SumGroupProperty<T> property = new SumGroupProperty<>(caption, innerInterfaces.getSet(), listImplements, implement.get(0));
        property.setExplicitInnerClasses(innerInterfaces, explicitInnerClasses);

        return mapLGProp(group, persistent, property, listImplements);
    }

    // ------------------- Override property ----------------- //

    public <T extends PropertyInterface> LP addOGProp(Group group, boolean persist, LocalizedString caption, GroupType type, int numOrders, boolean ordersNotNull, boolean descending, int interfaces, List<ResolveClassSet> explicitInnerClasses, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(interfaces);
        return addOGProp(group, persist, caption, type, numOrders, ordersNotNull, descending, innerInterfaces, explicitInnerClasses, readCalcImplements(innerInterfaces, params));
    }
    public <T extends PropertyInterface> LP addOGProp(Group group, boolean persist, LocalizedString caption, GroupType type, int numOrders, boolean ordersNotNull, boolean descending, ImOrderSet<T> innerInterfaces, List<ResolveClassSet> explicitInnerClasses, ImList<PropertyInterfaceImplement<T>> listImplements) {
        int numExprs = type.numExprs();
        ImList<PropertyInterfaceImplement<T>> props = listImplements.subList(0, numExprs);
        ImOrderMap<PropertyInterfaceImplement<T>, Boolean> orders = listImplements.subList(numExprs, numExprs + numOrders).toOrderSet().toOrderMap(descending);
        ImList<PropertyInterfaceImplement<T>> groups = listImplements.subList(numExprs + numOrders, listImplements.size());
        OrderGroupProperty<T> property = new OrderGroupProperty<>(caption, innerInterfaces.getSet(), groups.getCol(), props, type, orders, ordersNotNull);
        property.setExplicitInnerClasses(innerInterfaces, explicitInnerClasses);

        return mapLGProp(group, persist, property, groups);
    }

    // ------------------- GROUP MAX ----------------- //

    protected LP addMGProp(Group group, LocalizedString caption, boolean min, int interfaces, List<ResolveClassSet> explicitInnerClasses, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(interfaces);
        ImList<PropertyInterfaceImplement<PropertyInterface>> listImplements = readCalcImplements(innerInterfaces, params);
        LP[] result = new LP[1];

        MSet<Property> mOverridePersist = SetFact.mSet();

        ImList<PropertyInterfaceImplement<PropertyInterface>> groupImplements = listImplements.subList(1, listImplements.size());
        ImList<PropertyImplement<?, PropertyInterfaceImplement<PropertyInterface>>> mgProps = PropertyFact.createMGProp(new LocalizedString[]{caption}, innerInterfaces, explicitInnerClasses, baseLM.baseClass,
                listImplements.subList(0, 1), groupImplements.getCol(), mOverridePersist, min);

        for (int i = 0; i < mgProps.size(); i++)
            result[i] = mapLGProp(group, mgProps.get(i), groupImplements);
        return result[0];
    }

    // ------------------- CGProperty ----------------- //

    protected <T extends PropertyInterface, P extends PropertyInterface> LP addCGProp(Group group, boolean checkChange, boolean persistent, LocalizedString caption, LP<PropertyInterface> dataProp, int interfaces, List<ResolveClassSet> explicitInnerClasses, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(interfaces);
        return addCGProp(group, checkChange, persistent, caption, dataProp, innerInterfaces, explicitInnerClasses, readCalcImplements(innerInterfaces, params));
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LP addCGProp(Group group, boolean checkChange, boolean persistent, LocalizedString caption, LP<P> dataProp, ImOrderSet<T> innerInterfaces, List<ResolveClassSet> explicitInnerClasses, ImList<PropertyInterfaceImplement<T>> listImplements) {
        CycleGroupProperty<T, P> property = new CycleGroupProperty<>(caption, innerInterfaces.getSet(), listImplements.subList(1, listImplements.size()).getCol(), listImplements.get(0), dataProp == null ? null : dataProp.property);
        property.setExplicitInnerClasses(innerInterfaces, explicitInnerClasses);

        // нужно добавить ограничение на уникальность
        addConstraint(property.getConstrainedProperty(), property.getConstrainedMessage(), checkChange);

        return mapLGProp(group, persistent, property, listImplements.subList(1, listImplements.size()));
    }

//    protected static <T extends PropertyInterface<T>> AggregateGroupProperty create(String sID, LocalizedString caption, Property<T> property, T aggrInterface, Collection<PropertyMapImplement<?, T>> groupProps) {

    // ------------------- GROUP AGGR ----------------- //

    protected LP addAGProp(Group group, boolean checkChange, boolean persistent, LocalizedString caption, boolean noConstraint, int interfaces, List<ResolveClassSet> explicitInnerClasses, Object... props) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(interfaces);
        return addAGProp(group, checkChange, persistent, caption, noConstraint, innerInterfaces, explicitInnerClasses, readCalcImplements(innerInterfaces, props));
    }

    protected <T extends PropertyInterface<T>, I extends PropertyInterface> LP addAGProp(Group group, boolean checkChange, boolean persistent, LocalizedString caption, boolean noConstraint, ImOrderSet<T> innerInterfaces, List<ResolveClassSet> explicitInnerClasses, ImList<PropertyInterfaceImplement<T>> listImplements) {
        T aggrInterface = (T) listImplements.get(0);
        PropertyInterfaceImplement<T> whereProp = listImplements.get(1);
        ImList<PropertyInterfaceImplement<T>> groupImplements = listImplements.subList(2, listImplements.size());

        AggregateGroupProperty<T> aggProp = AggregateGroupProperty.create(caption, innerInterfaces.getSet(), whereProp, aggrInterface, groupImplements.toOrderExclSet().getSet());
        aggProp.setExplicitInnerClasses(innerInterfaces, explicitInnerClasses);
        return addAGProp(group, checkChange, persistent, noConstraint, aggProp, groupImplements);
    }

    // чисто для generics
    private <T extends PropertyInterface<T>> LP addAGProp(Group group, boolean checkChange, boolean persistent, boolean noConstraint, AggregateGroupProperty<T> property, ImList<PropertyInterfaceImplement<T>> listImplements) {
        // нужно добавить ограничение на уникальность
        if(!noConstraint)
            addConstraint(property.getConstrainedProperty(), property.getConstrainedMessage(), checkChange);

        return mapLGProp(group, persistent, property, listImplements);
    }

    // ------------------- UNION ----------------- //

    protected LP addUProp(Group group, LocalizedString caption, Union unionType, int[] coeffs, Object... params) {
        return addUProp(group, caption, unionType, null, coeffs, params);
    }

    protected LP addUProp(Group group, LocalizedString caption, Union unionType, String separator, int[] coeffs, Object... params) {

        assert (unionType==Union.SUM)==(coeffs!=null);

        int intNum = getIntNum(params);
        ImOrderSet<UnionProperty.Interface> listInterfaces = UnionProperty.getInterfaces(intNum);
        ImList<PropertyInterfaceImplement<UnionProperty.Interface>> listOperands = readCalcImplements(listInterfaces, params);

        UnionProperty property = null;
        switch (unionType) {
            case MAX:
            case MIN:
                property = new MaxUnionProperty(unionType == Union.MIN, caption, listInterfaces, listOperands.getCol());
                break;
            case SUM:
                MMap<PropertyInterfaceImplement<UnionProperty.Interface>, Integer> mMapOperands = MapFact.mMap(MapFact.addLinear());
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
            case CONCAT:
                property = new FormulaUnionProperty(caption, listInterfaces, listOperands, new StringConcatenateFormulaImpl(separator));
                break;
        }

        return addProperty(group, new LP<>(property, listInterfaces));
    }

    protected LP addAUProp(Group group, boolean persistent, boolean isExclusive, boolean isChecked, boolean isLast, CaseUnionProperty.Type type, LocalizedString caption, ValueClass valueClass, ValueClass... interfaces) {
        ImOrderSet<UnionProperty.Interface> listInterfaces = UnionProperty.getInterfaces(interfaces.length);
        return addProperty(group, new LP<>(
                new CaseUnionProperty(isExclusive, isChecked, isLast, type, caption, listInterfaces, valueClass, listInterfaces.mapList(ListFact.toList(interfaces))), listInterfaces));
    }

    protected LP addCaseUProp(Group group, boolean persistent, LocalizedString caption, boolean isExclusive, Object... params) {
        ImOrderSet<UnionProperty.Interface> listInterfaces = UnionProperty.getInterfaces(getIntNum(params));
        MList<CalcCase<UnionProperty.Interface>> mListCases = ListFact.mList();
        ImList<PropertyMapImplement<?,UnionProperty.Interface>> mapImplements = (ImList<PropertyMapImplement<?, UnionProperty.Interface>>) (ImList<?>) readCalcImplements(listInterfaces, params);
        for (int i = 0; i < mapImplements.size() / 2; i++)
            mListCases.add(new CalcCase<>(mapImplements.get(2 * i), mapImplements.get(2 * i + 1)));
        if (mapImplements.size() % 2 != 0)
            mListCases.add(new CalcCase<>(new PropertyMapImplement<>((Property<PropertyInterface>) baseLM.vtrue.property), mapImplements.get(mapImplements.size() - 1)));

        return addProperty(group, new LP<>(new CaseUnionProperty(caption, listInterfaces, isExclusive, mListCases.immutableList()), listInterfaces));
    }

    public static List<ResolveClassSet> getSignatureForLogProperty(List<ResolveClassSet> basePropSignature, SystemEventsLogicsModule systemEventsLM) {
        List<ResolveClassSet> signature = new ArrayList<>(basePropSignature);
        signature.add(systemEventsLM.currentSession.property.getValueClass(ClassType.aroundPolicy).getResolveSet());
        return signature;
    }
    
    public static List<ResolveClassSet> getSignatureForLogProperty(LP lp, SystemEventsLogicsModule systemEventsLM) {
        List<ResolveClassSet> signature = new ArrayList<>();
        for (ValueClass cls : lp.getInterfaceClasses(ClassType.logPolicy)) {
            signature.add(cls.getResolveSet());
        }
        return getSignatureForLogProperty(signature, systemEventsLM);    
    } 

    public static String getLogPropertyCN(LP<?> lp, String logNamespace, SystemEventsLogicsModule systemEventsLM) {
        String namespace = PropertyCanonicalNameParser.getNamespace(lp.property.getCanonicalName());
        String name = getLogPropertyName(namespace, lp.property.getName());

        List<ResolveClassSet> signature = getSignatureForLogProperty(lp, systemEventsLM);
        return PropertyCanonicalNameUtils.createName(logNamespace, name, signature);
    }

    private static String getLogPropertyName(LP<?> lp, boolean drop) {
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
    public LP addLProp(SystemEventsLogicsModule systemEventsLM, LP lp, DBNamingPolicy namingPolicy) {
        return addLProp(systemEventsLM, lp, false, namingPolicy);
    }

    public LP addLProp(SystemEventsLogicsModule systemEventsLM, LP lp, boolean drop, DBNamingPolicy namingPolicy) {
        assert lp.property.isNamed();
        String name = getLogPropertyName(lp, drop);
        
        List<ResolveClassSet> signature = getSignatureForLogProperty(lp, systemEventsLM);
        
        LP equalsProperty = systemEventsLM.isCurrentSession;

        LP value = drop ? baseLM.vtrue : lp;

        LP changed = addCHProp(lp, drop ? IncrementType.DROP : IncrementType.SETCHANGED, PrevScope.DB);
        LP where = addJProp(baseLM.and1, add(directLI(changed), new Object[]{equalsProperty, changed.listInterfaces.size() + 1}));

        StoredDataProperty data = new StoredDataProperty(LocalizedString.create("{logics.log}" + " " + lp.property), where.getInterfaceClasses(ClassType.logPolicy), value.property.getValueClass(ClassType.logPolicy));
        LP log = addProperty(null, new LP<>(data));

        log.setEventChange(systemEventsLM, Event.APPLY, BaseUtils.add(directLI(value), directLI(where)));

        makePropertyPublic(log, name, signature);
        markLoggableStored(log, namingPolicy);
        return log;
    }

    public LP addLDropProp(SystemEventsLogicsModule systemEventsLM, LP lp, DBNamingPolicy namingPolicy) {
        return addLProp(systemEventsLM, lp, true, namingPolicy);
    }

    private void markLoggableStored(LP lp, DBNamingPolicy namingPolicy) {
        lp.property.markStored(null);
        if(namingPolicy != null)
            lp.property.initStored(baseLM.tableFactory, namingPolicy); // we need to initialize it because reflection events initialized after init stored
    }

    private LP toLogical(LP property) {
        return addJProp(baseLM.and1, add(baseLM.vtrue, directLI(property)));
    }

    private LP convertToLogical(LP property) {
        if (!isLogical(property)) {
            property = toLogical(property);
        }
        return property;
    }

    protected boolean isLogical(LP<?> property) {
        if(property == null)
            return false;

        Type type = property.property.getType();
        return type != null && type.equals(LogicalClass.instance);
    }

    public LP addLWhereProp(LP logValueProperty, LP logDropProperty) {
        return addUProp(null, LocalizedString.NONAME, Union.OVERRIDE, null, add(directLI(convertToLogical(logValueProperty)), directLI(logDropProperty)));
                
    }

    // ------------------- CONCAT ----------------- //

    protected LP addSFUProp(String separator, int intNum) {
        return addUProp(null, LocalizedString.create("{logics.join}"), Union.CONCAT, separator, null, getUParams(intNum));
    }

    // ------------------- ACTION ----------------- //

    public LA addAProp(Action property) {
        return addAProp(null, property);
    }

    public LA addAProp(Group group, Action property) {
        return addAction(group, new LA(property));
    }

    // ------------------- MESSAGE ----------------- //

    protected LA addMAProp(String title, boolean noWait, boolean log, Object... params) {
        return addJoinAProp(null, LocalizedString.NONAME, addMAProp(title, noWait, log), params);
    }

    @IdentityStrongLazy
    protected LA addMAProp(String title, boolean noWait, boolean log) {
        return addAction(null, new LA(new MessageAction(LocalizedString.create("Message"), title, noWait, log)));
    }

    public LA addFocusAction(PropertyDrawEntity propertyDrawEntity) {
        return addAction(null, new LA(new FocusAction(propertyDrawEntity)));
    }

    // ------------------- CONFIRM ----------------- //


    protected LA addConfirmAProp(String title, boolean yesNo, LP targetProp, Object... params) {
        return addConfirmAProp(null, LocalizedString.NONAME, title, yesNo, targetProp, params);
    }

    protected LA addConfirmAProp(Group group, LocalizedString caption, String title, boolean yesNo, LP<?> targetProp, Object... params) {
        return addJoinAProp(group, caption, addConfirmAProp(title, yesNo, targetProp != null ? targetProp.property : null), params);
    }

    @IdentityStrongLazy
    protected LA addConfirmAProp(String title, boolean yesNo, Property property) {
        return addAction(null, new LA(new ConfirmAction(LocalizedString.create("Confirm"), title, yesNo, property != null ? new LP(property) : null)));
    }

    // ------------------- Async Update Action ----------------- //

    protected LA addAsyncUpdateAProp(Object... params) {
        return addAsyncUpdateAProp(LocalizedString.NONAME, params);
    }

    protected LA addAsyncUpdateAProp(LocalizedString caption, Object... params) {
        return addAsyncUpdateAProp(null, caption, params);
    }

    protected LA addAsyncUpdateAProp(Group group, LocalizedString caption, Object... params) {
        return addJoinAProp(group, caption, addAsyncUpdateAProp(), params);
    }

    @IdentityStrongLazy
    public LA<?> addAsyncUpdateAProp() {
        return addAction(null, new LA(new AsyncUpdateEditValueAction(LocalizedString.create("Async Update"))));
    }

    // ------------------- DRILLDOWN ----------------- //

    public void setupDrillDownProperty(Property property, boolean isLightStart) {
        if (property.supportsDrillDown()) {
            LA<?> drillDownFormProperty = addLazyAProp(property); //isLightStart ? : addDDAProp(property);
            Action formProperty = drillDownFormProperty.action;
            property.setContextMenuAction(formProperty.getSID(), formProperty.caption);
            property.setEventAction(formProperty.getSID(), formProperty.getImplement(property.getReflectionOrderInterfaces()));
        }
    }
    
    public LA addDrillDownAProp(LP<?> property) {
        return addDDAProp(property);
    }

    public LA<?> addDDAProp(LP property) {
        assert property.property.getReflectionOrderInterfaces().equals(property.listInterfaces);
        if (property.property instanceof Property && property.property.supportsDrillDown())
            return addDDAProp(property.property);
        else 
            throw new UnsupportedOperationException();
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

    public LA<?> addLazyAProp(Property property) {
        LA result = addAProp(baseLM.drillDownGroup, new LazyAction(LocalizedString.create("{logics.property.drilldown.action}"), property));
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

    public LP getIsActiveFormProperty() {
        return baseLM.getIsActiveFormProperty();
    }

    // ------------------- RESET ----------------- //

    public void setupResetProperty(Property property) {
        if (property.supportsReset()) {
            LP lp = new LP(property);
            LA<?> resetFormProperty = addSetupResetAProp(lp);
            Action formProperty = resetFormProperty.action;
            property.setContextMenuAction(formProperty.getSID(), formProperty.caption);
            property.setEventAction(formProperty.getSID(), resetFormProperty.getImplement(lp.listInterfaces));
        }
    }

    public LA<?> addSetupResetAProp(LP property) {
        LA result = addResetProperty(property);
        if (property.property.isNamed()) {
            List<ResolveClassSet> signature = new ArrayList<>();
            String name = nameForResetAction(property.property, signature);
            makeActionPublic(result, name, signature);
        }
        return result;
    }

    public LA<?> addResetAProp(LP property) {
        return addListAProp(addResetProperty(baseLM.getRequestCanceledProperty()), addResetProperty(property));
    }

    public LA<?> addResetProperty(LP property) {
        return addSetPropertyAProp(null, LocalizedString.create("{logics.property.reset}"), property.listInterfaces.size(), false,
                add(getUParams(property.listInterfaces.size()), add(directLI(property), baseLM.vnull)));
    }

    private String nameForResetAction(Property property, List<ResolveClassSet> signature) {
        assert property.isNamed();
        PropertyCanonicalNameParser parser = new PropertyCanonicalNameParser(property.getCanonicalName(), baseLM.getClassFinder());
        String name = PropertyCanonicalNameUtils.resetPrefix + parser.getNamespace() + "_" + property.getName();
        signature.addAll(parser.getSignature());
        return name;
    }

    // ---------------------- VALUE ---------------------- //

    public Pair<LP, ActionObjectSelector> getObjValueProp(FormEntity formEntity, ObjectEntity obj) {
        return baseLM.getObjValueProp(formEntity, obj);
    }

    public Pair<LP, ActionObjectSelector> getObjIntervalProp(FormEntity form, ObjectEntity objectFrom, ObjectEntity objectTo, LP intervalProperty, LP fromIntervalProperty, LP toIntervalProperty) {
        return baseLM.getObjIntervalProp(form, objectFrom, objectTo, intervalProperty, fromIntervalProperty, toIntervalProperty);
    }

    // ---------------------- Add Object ---------------------- //

    public <T extends PropertyInterface, I extends PropertyInterface> LA addAddObjAProp(CustomClass cls, boolean autoSet, int resInterfaces, boolean conditional, boolean resultExists, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(getIntNum(params));
        ImList<PropertyInterfaceImplement<PropertyInterface>> readImplements = readCalcImplements(innerInterfaces, params);
        PropertyMapImplement<T, PropertyInterface> resultPart = (PropertyMapImplement<T, PropertyInterface>)
                (resultExists ? readImplements.get(resInterfaces) : null);
        PropertyMapImplement<T, PropertyInterface> conditionalPart = (PropertyMapImplement<T, PropertyInterface>)
                (conditional ? readImplements.get(resInterfaces + (resultExists ? 1 : 0)) : null);

        return addAProp(null, new AddObjectAction(cls, innerInterfaces.getSet(), readImplements.subList(0, resInterfaces).toOrderExclSet(), conditionalPart, resultPart, MapFact.<PropertyInterfaceImplement<I>, Boolean>EMPTYORDER(), false, autoSet));
    }

    public LA getAddObjectAction(FormEntity formEntity, ObjectEntity obj, CustomClass explicitClass) {
        return baseLM.getAddObjectAction(formEntity, obj, explicitClass);
    }

    // ---------------------- Delete Object ---------------------- //

    public <X extends PropertyInterface> LA addDeleteAction(CustomClass cls) {
//        LA delete = addChangeClassAProp(baseClass.unknown, 1, 0, false, true, 1, is(cls), 1);
//
//        LA<?> result = addIfAProp(LocalizedString.create("{logics.delete}"), baseLM.sessionOwners, // IF sessionOwners() THEN 
//                delete, 1, // DELETE
//                addListAProp( // ELSE
//                        addConfirmAProp("lsFusion", addCProp(StringClass.text, LocalizedString.create("{form.instance.do.you.really.want.to.take.action} '{logics.delete}'"))), // CONFIRM
//                        addIfAProp(baseLM.confirmed, // IF confirmed() THEN
//                                addListAProp(
//                                        delete, 1, // DELETE
//                                        baseLM.apply), 1), 1 // apply()
//                ), 1);
        
        LA<X> result = addDeleteAProp(LocalizedString.create("{logics.delete}"), cls);

        result.action.setForceAsyncEventExec(calcAsync -> new AsyncMapRemove<>(result.action.interfaces.single()));
        setDeleteActionOptions(result);

        return result;
    }

    protected void setDeleteActionOptions(LA property) {
        setFormActions(property);

        property.setImage("delete.png");
        property.setChangeKey(KeyStrokes.getDeleteActionKeyStroke());
        property.setShowChangeKey(false);
    }

    // ---------------------- Add Form ---------------------- //

    // assumes 1 parameter - new, others - context
    protected LA addNewEditAction2(CustomClass cls, LA setAction, LA doAction, int contextParams) {
        int newIndex = contextParams + 1;
        Object[] setEditWithParams = new Object[]{baseLM.getPolyEdit(), newIndex};
        if(setAction != null) // adding setAction if any
            setEditWithParams = directLI(addListAProp(BaseUtils.add(directLI(setAction), setEditWithParams)));

        LA edit = addRequestAProp(null, true, LocalizedString.NONAME, // REQUEST
                BaseUtils.add(BaseUtils.add(
                                setEditWithParams, // edit(x);
                                directLI(doAction)), // DO <<doAction>>
                        new Object[]{addIfAProp(baseLM.sessionOwners, baseLM.getPolyDelete(), 1), newIndex}) // ELSE IF seekOwners THEN delete(x)
        );

        return addForAProp(LocalizedString.create("{logics.add}"), false, false, false, false, contextParams, cls, true, false, 0, false,
                BaseUtils.add(getUParams(contextParams + 1), directLI(edit))); // context + addedInterface + action
    }

    public LA<?> addNewEditAction2(CustomClass cls, LP targetProp, int contextParams, FormSessionScope scope, Object... setProperty) {
        Object[] externalParams = getUParams(contextParams + 1); // + new object
        LA action = addNewEditAction2(cls,
                addSetPropertyAProp(null, LocalizedString.NONAME, contextParams + 1, false, BaseUtils.add(externalParams, setProperty)),
                addSetPropertyAProp(null, LocalizedString.NONAME, contextParams + 1, false, BaseUtils.add(externalParams, new Object[] {targetProp, contextParams + 1})), // targetProp() <- new object
                contextParams);

        if(scope.isNewSession())
            action = addNewSessionAProp(null, action, scope.isNestedSession(), false, false, getMigrateInputProps(ListFact.singleton(targetProp)));

        return action;
    }



    // assumes 1 parameter - new, others - context
    protected LA addNewEditAction(CustomClass cls, LA setAction, LA doAction, int contextParams) {
        // NEW AUTOSET x=X DO {
        //      REQUEST
        //          <<setAction>>
        //          edit(x);
        //      DO
        //          <<doAction>>
        //      ELSE
        //          IF sessionOwners THEN
        //              DELETE x;
        // }

        int newIndex = contextParams + 1;
        Object[] setEditWithParams = new Object[]{baseLM.getPolyEdit(), newIndex};
        if(setAction != null) // adding setAction if any
            setEditWithParams = directLI(addListAProp(BaseUtils.add(directLI(setAction), setEditWithParams)));

        LA edit = addRequestAProp(null, true, LocalizedString.NONAME, // REQUEST
                BaseUtils.add(BaseUtils.add(
                setEditWithParams, // edit(x);
                directLI(doAction)), // DO <<doAction>>
                new Object[]{addIfAProp(baseLM.sessionOwners, baseLM.getPolyDelete(), 1), newIndex}) // ELSE IF seekOwners THEN delete(x)
        );

        return addForAProp(LocalizedString.create("{logics.add}"), false, false, false, false, contextParams, cls, true, false, 0, false,
                BaseUtils.add(getUParams(contextParams + 1), directLI(edit))); // context + addedInterface + action
    }

    public LA<?> addNewEditAction(CustomClass cls, LP targetProp, int contextParams, FormSessionScope scope, Object... setProperty) {
        Object[] externalParams = getUParams(contextParams + 1); // + new object
        LA action = addNewEditAction(cls,
                addSetPropertyAProp(null, LocalizedString.NONAME, contextParams + 1, false, BaseUtils.add(externalParams, setProperty)),
                addSetPropertyAProp(null, LocalizedString.NONAME, contextParams + 1, false, BaseUtils.add(externalParams, new Object[] {targetProp, contextParams + 1})), // targetProp() <- new object
                contextParams);

        if(scope.isNewSession())
            action = addNewSessionAProp(null, action, scope.isNestedSession(), false, false, getMigrateInputProps(ListFact.singleton(targetProp)));

        return action;
    }

    protected LA addNewEditAction(CustomClass cls, ObjectEntity contextObject) {
        assert contextObject != null;

        LA result = addNewEditAction(cls,
                null, // no action
                addOSAProp(contextObject, UpdateType.LAST, 1), // SEEK co=x;
                0);
        setAddActionOptions(result, contextObject);
        
        return result;
    }

    protected void setAddActionOptions(LA property, final ObjectEntity objectEntity) {

        setFormActions(property);

        property.setImage("add.png");
        property.setChangeKey(KeyStrokes.getAddActionKeyStroke());
        property.setShowChangeKey(false);

        if(objectEntity != null) {
            property.addProcessor(new ActionOrProperty.DefaultProcessor() {
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

    protected LA addEditFormAction(CustomClass customClass) {
        LA result = addEditAProp(LocalizedString.create("{logics.edit}"), customClass);

        setEditActionOptions(result);

        return result;
    }
    
    private void setFormActions(LA result) {
        result.setShouldBeLast(true);
        result.setViewType(ClassViewType.TOOLBAR);
    }

    private void setEditActionOptions(LA result) {
        setFormActions(result);
        
        result.setImage("edit.png");
        Map<String, BindingMode> bindingModes = new HashMap<>();
        bindingModes.put("preview", BindingMode.ONLY);
        bindingModes.put("group", BindingMode.ONLY);
        bindingModes.put("editing", BindingMode.NO);
        result.setChangeKey(KeyStrokes.getEditActionKeyStroke(), bindingModes);
        result.setShowChangeKey(false);
        result.setChangeMouse(MouseInputEvent.DBLCLK, bindingModes);
    }

    public LA addProp(Action prop) {
        return addProp(null, prop);
    }

    public LA addProp(Group group, Action prop) {
        return addAction(group, new LA(prop));
    }

    public LP addProp(Property<? extends PropertyInterface> prop) {
        return addProp(null, prop);
    }

    public LP addProp(Group group, Property<? extends PropertyInterface> prop) {
        return addProperty(group, new LP(prop));
    }

    protected void addPropertyToGroup(ActionOrProperty<?> property, Group group) {
        Version version = getVersion();
        if (group != null) {
            group.add(property, version);
        } else if (!property.isLocal()) {
            baseLM.privateGroup.add(property, version);
        }

        if(propsFinalized)
            property.finalizeAroundInit();
    }

    protected LP addProperty(Group group, LP lp) {
        return addActionOrProperty(group, lp);
    }

    protected LA addAction(Group group, LA la) {
        return addActionOrProperty(group, la);
    }

    private <T extends LAP<?, ?>> T addActionOrProperty(Group group, T lp) {
        addPropertyToGroup(lp.getActionOrProperty(), group);
        return lp;
    }

    // нужен так как иначе начинает sID расширять

    public <T extends PropertyInterface> LP<T> addOldProp(LP<T> lp, PrevScope scope) {
        return baseLM.addOldProp(lp, scope);
    }

    public <T extends PropertyInterface> LP<T> addCHProp(LP<T> lp, IncrementType type, PrevScope scope) {
        return baseLM.addCHProp(lp, type, scope);
    }

    public <T extends PropertyInterface> LP addClassProp(LP<T> lp) {
        return baseLM.addClassProp(lp);
    }

    public LP addGroupObjectProp(GroupObjectEntity groupObject, GroupObjectProp prop) {
        return baseLM.addGroupObjectProp(groupObject, prop);
    }
    
    protected LA addOSAProp(ObjectEntity object, UpdateType type, Object... params) {
        return addJoinAProp(null, LocalizedString.NONAME, addOSAProp(object, type), params);
    }

    @IdentityStrongLazy // для ID
    public LA<?> addOSAProp(ObjectEntity object, UpdateType type) {
        SeekObjectAction seekProperty = new SeekObjectAction(object, type);
        return addAction(null, new LA<>(seekProperty));
    }

    protected LA addGOSAProp(Group group, LocalizedString caption, GroupObjectEntity object, ImOrderSet<ObjectEntity> objects, UpdateType type, Object... params) {
        return addJoinAProp(group, caption, addGOSAProp(object, objects, type), params);
    }

    @IdentityStrongLazy // для ID
    public LA addGOSAProp(GroupObjectEntity object, ImOrderSet<ObjectEntity> objects, UpdateType type) {
        List<ValueClass> objectClasses = new ArrayList<>();
        for (ObjectEntity obj : objects) {
            objectClasses.add(obj.baseClass);
        }
        SeekGroupObjectAction seekProperty = new SeekGroupObjectAction(object, objects, type, objectClasses.toArray(new ValueClass[objectClasses.size()]));
        return addAction(null, new LA<>(seekProperty));
    }

    protected LA addExpandCollapseAProp(GroupObjectEntity object, List<ObjectEntity> objects, ExpandCollapseType type, boolean expand, Object... params) {
        return addExpandCollapseAProp(null, LocalizedString.NONAME, object, objects, type, expand, params);
    }

    protected LA addExpandCollapseAProp(Group group, LocalizedString caption, GroupObjectEntity object, List<ObjectEntity> objects, ExpandCollapseType type, boolean expand, Object... params) {
        return addJoinAProp(group, caption, addExpandCollapseAProp(object, objects, type, expand), params);
    }

    @IdentityStrongLazy // для ID
    public LA addExpandCollapseAProp(GroupObjectEntity object, List<ObjectEntity> objects, ExpandCollapseType type, boolean expand) {
        List<ValueClass> objectClasses = new ArrayList<>();
        for (ObjectEntity obj : objects) {
            objectClasses.add(obj.baseClass);
        }
        ExpandCollapseGroupObjectAction expandProperty = new ExpandCollapseGroupObjectAction(object, objects, type, expand, objectClasses.toArray(new ValueClass[objectClasses.size()]));
        return addAction(null, new LA<>(expandProperty));
    }

    public void addConstraint(Property<?> property, LocalizedString message, boolean checkChange) {
        addConstraint(property, message, checkChange, null);
    }

    public void addConstraint(Property<?> property, LocalizedString message, boolean checkChange, DebugInfo.DebugPoint debugPoint) {
        addConstraint(addProp(property), baseLM.addCProp(StringClass.text, message), ListFact.EMPTY(), (checkChange ? Property.CheckType.CHECK_ALL : Property.CheckType.CHECK_NO), null, Event.APPLY, this, debugPoint);
    }

    protected <P extends PropertyInterface> void addConstraint(LP<?> lp, LP<?> messageLP, ImList<PropertyMapImplement<?, P>> properties, Property.CheckType type, ImSet<Property<?>> checkProps, Event event, LogicsModule lm, DebugInfo.DebugPoint debugPoint) {
//      will not check for constraint prev value (i.e. do not let the user set any value if constraint was already broken)
        lp.property.checkChange = type;
        lp.property.checkProperties = checkProps;

        // not sure if it's needed, but lp is used instead of property to ensure that SET property is added to property list
        if(!lp.property.noDB()) // wrapping in SET
            lp = addCHProp(lp, IncrementType.SET, event.getScope());

        assert type != Property.CheckType.CHECK_SOME || checkProps != null;

//      will check for constraint prev value (i.e. let the user set any value if constraint was already broken)
//        lp.property.checkChange = type;
//        lp.property.checkProperties = checkProps;

        ActionMapImplement<ClassPropertyInterface, ClassPropertyInterface> logAction;
//            logAction = new LogPropertyActionProperty<T>(property, messageProperty).getImplement();
        //  PRINT OUT property MESSAGE NOWAIT;
        logAction = (ActionMapImplement<ClassPropertyInterface, ClassPropertyInterface>) addPFAProp(null, LocalizedString.concat("Constraint - ", lp.property.caption), new OutFormSelector<P>((Property) lp.property, messageLP.property, properties), ListFact.EMPTY(), ListFact.EMPTY(), SetFact.EMPTYORDER(), SetFact.EMPTY(), FormPrintType.MESSAGE, false, false, false, 30, null, true, null, null, null).action.getImplement();
        ActionMapImplement<?, ClassPropertyInterface> constraintAction =
                PropertyFact.createListAction(
                        SetFact.EMPTY(),
                        ListFact.toList(logAction,
                                baseLM.cancel.action.getImplement(SetFact.EMPTYORDER())
                        )
                );
        constraintAction.mapEventAction(this, PropertyFact.createAnyGProp(lp.property).getImplement(), event, true, debugPoint);
        addProp(constraintAction.action);
    }

    public <T extends PropertyInterface> void addEventAction(Event event, boolean descending, boolean ordersNotNull, int noInline, boolean forceInline, DebugInfo.DebugPoint debugPoint, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(getIntNum(params));

        ImList<ActionOrPropertyInterfaceImplement> listImplements = readImplements(innerInterfaces, params);
        int implCnt = listImplements.size();

        ImOrderMap<PropertyInterfaceImplement<PropertyInterface>, Boolean> orders = BaseUtils.immutableCast(listImplements.subList(2, implCnt - noInline).toOrderSet().toOrderMap(descending));

        ImSet<PropertyInterface> noInlineInterfaces = BaseUtils.<ImList<PropertyInterface>>immutableCast(listImplements.subList(implCnt - noInline, implCnt)).toOrderExclSet().getSet();

        addEventAction(innerInterfaces.getSet(), (ActionMapImplement<?, PropertyInterface>) listImplements.get(0), (PropertyMapImplement<?, PropertyInterface>) listImplements.get(1), orders, ordersNotNull, event, noInlineInterfaces, forceInline, false, debugPoint);
    }

    public <P extends PropertyInterface, D extends PropertyInterface> void addEventAction(Action<P> action, PropertyMapImplement<?, P> whereImplement, ImOrderMap<PropertyInterfaceImplement<P>, Boolean> orders, boolean ordersNotNull, Event event, boolean resolve, DebugInfo.DebugPoint debugPoint) {
        addEventAction(action.interfaces, action.getImplement(), whereImplement, orders, ordersNotNull, event, SetFact.EMPTY(), false, resolve, debugPoint);
    }

    public <P extends PropertyInterface, D extends PropertyInterface> void addEventAction(ImSet<P> innerInterfaces, ActionMapImplement<?, P> action, PropertyMapImplement<?, P> whereImplement, ImOrderMap<PropertyInterfaceImplement<P>, Boolean> orders, boolean ordersNotNull, Event event, ImSet<P> noInline, boolean forceInline, boolean resolve, DebugInfo.DebugPoint debugPoint) {
        if(!(whereImplement.property).noDB())
            whereImplement = whereImplement.mapChanged(IncrementType.SET, event.getScope());

        Action<? extends PropertyInterface> eventAction =
                innerInterfaces.isEmpty() ?
                    PropertyFact.createIfAction(innerInterfaces, whereImplement, action, null).action :
                    PropertyFact.createForAction(innerInterfaces, SetFact.EMPTY(), whereImplement, orders, ordersNotNull, action, null, false, noInline, forceInline).action;

        if(debugPoint != null) { // создано getEventDebugPoint
            if(debugger.isEnabled()) // topContextActionDefinitionBodyCreated
                debugger.setNewDebugStack(eventAction);

            assert eventAction.getDelegationType(true) == ActionDelegationType.AFTER_DELEGATE;
            ScriptingLogicsModule.setDebugInfo(true, debugPoint, eventAction); // actionDefinitionBodyCreated
        }

//        action.setStrongUsed(whereImplement.property); // добавить сильную связь, уже не надо поддерживается более общий механизм - смотреть на Session Calc
//        action.caption = "WHEN " + whereImplement.property + " " + actionProperty;
        addProp(eventAction);

        addBaseEvent(eventAction, event, resolve, false);
    }

    public <P extends PropertyInterface> void addBaseEvent(Action<P> action, Event event, boolean resolve, boolean single) {
        action.addEvent(event.base, event.session);
        if(event.after != null)
            action.addStrongUsed(event.after);
        action.singleApply = single;
        action.resolve = resolve;
    }

    public <P extends PropertyInterface> void addAspectEvent(int interfaces, ActionImplement<P, Integer> action, String mask, boolean before) {
        // todo: непонятно что пока с полными каноническими именами и порядками параметров делать
    }

    public <P extends PropertyInterface, T extends PropertyInterface> void addAspectEvent(Action<P> action, ActionMapImplement<T, P> aspect, boolean before) {
        if(before)
            action.addBeforeAspect(aspect);
        else
            action.addAfterAspect(aspect);
    }

    protected <L extends PropertyInterface, T extends PropertyInterface> void follows(LP<T> first, LP<L> second, Integer... mapping) {
        follows(first, null, ListFact.toList(new PropertyFollowsDebug(true, null), new PropertyFollowsDebug(false, null)), Event.APPLY, second, mapping);
    }

    protected <L extends PropertyInterface, T extends PropertyInterface> void follows(final LP<T> first, DebugInfo.DebugPoint debugPoint, ImList<PropertyFollowsDebug> options, Event event, LP<L> second, final Integer... mapping) {
        addFollows(first.property, new PropertyMapImplement<>(second.property, second.getRevMap(first.listInterfaces, mapping)), debugPoint, options, event);
    }

    public <T extends PropertyInterface, L extends PropertyInterface> void setNotNull(Property<T> property, DebugInfo.DebugPoint debugPoint, ImList<PropertyFollowsDebug> options, Event event) {
        PropertyMapImplement<L, T> mapClasses = (PropertyMapImplement<L, T>) IsClassProperty.getMapProperty(property.getInterfaceClasses(ClassType.logPolicy));
        property.notNull = true;
        addFollows(mapClasses.property, new PropertyMapImplement<>(property, mapClasses.mapping.reverse()),
                LocalizedString.concatList(LocalizedString.create("{logics.property} "), property.caption, " [" + property.getSID(), LocalizedString.create("] {logics.property.not.defined}")),
                debugPoint, options, event);
    }

    public <P extends PropertyInterface> void disableInputList(LP<P> lp) {
        lp.property.disableInputList = true;
    }

    public <T extends PropertyInterface, L extends PropertyInterface> void addFollows(Property<T> property, PropertyMapImplement<L, T> implement, DebugInfo.DebugPoint debugPoint, ImList<PropertyFollowsDebug> options, Event event) {
        addFollows(property, implement, LocalizedString.create("{logics.property.violated.consequence.from}" + "(" + this + ") => (" + implement.property + ")"), debugPoint, options, event);
    }

    public <T extends PropertyInterface, L extends PropertyInterface> void addFollows(Property<T> property, PropertyMapImplement<L, T> implement, LocalizedString caption, DebugInfo.DebugPoint debugPoint, ImList<PropertyFollowsDebug> options, Event event) {
//        PropertyFollows<T, L> propertyFollows = new PropertyFollows<T, L>(this, implement, options);

        for(PropertyFollowsDebug option : options) {
            assert !option.isTrue || property.interfaces.size() == implement.mapping.size(); // assert что количество
            ActionMapImplement<?, T> setAction = option.isTrue ? implement.getSetNotNullAction(true) : property.getSetNotNullAction(false);
            if(setAction!=null) {
//                setAction.property.caption = "RESOLVE " + option.isTrue + " : " + property + " => " + implement.property;
                PropertyMapImplement<?, T> condition;
                if(option.isFull)
                    condition = PropertyFact.createAndNot(property, implement).mapChanged(IncrementType.SET, event.getScope());
                else {
                    if (option.isTrue)
                        condition = PropertyFact.createAndNot(property.getChanged(IncrementType.SET, event.getScope()), implement);
                    else
                        condition = PropertyFact.createAnd(property, implement.mapChanged(IncrementType.DROP, event.getScope()));
                }
                setAction.mapEventAction(this, condition, event, true, option.debugPoint);
            }
        }

        Property constraint = PropertyFact.createAndNot(property, implement).property;
        addConstraint(constraint, caption, false, debugPoint);
    }

    public <P extends PropertyInterface, C extends PropertyInterface> void setNotNull(LP<P> lp, ImList<PropertyFollowsDebug> resolve) {
        setNotNull(lp, null, Event.APPLY, resolve);
    }

    public <P extends PropertyInterface, C extends PropertyInterface> void setNotNull(LP<P> lp, DebugInfo.DebugPoint debugPoint, Event event, ImList<PropertyFollowsDebug> resolve) {
        setNotNull(lp.property, debugPoint, resolve, event);
    }

    public static <P extends PropertyInterface, T extends PropertyInterface> ActionMapImplement<P, T> mapActionListImplement(LA<P> property, ImOrderSet<T> mapList) {
        return new ActionMapImplement<>(property.action, getMapping(property, mapList));
    }
    public static <P extends PropertyInterface, T extends PropertyInterface> PropertyMapImplement<P, T> mapCalcListImplement(LP<P> property, ImOrderSet<T> mapList) {
        return new PropertyMapImplement<>(property.property, getMapping(property, mapList));
    }

    private static <P extends PropertyInterface, T extends PropertyInterface> ImRevMap<P, T> getMapping(LAP<P, ?> property, final ImOrderSet<T> mapList) {
        return property.getRevMap(mapList);
    }

    public LP not() {
        return baseLM.not();
    }

    // получает свойство is
    public LP<?> is(ValueClass valueClass) {
        return baseLM.is(valueClass);
    }

    public LP object(ValueClass valueClass) {
        return baseLM.object(valueClass);
    }

    protected LP and(boolean... nots) {
        return addAFProp(nots);
    }

    protected NavigatorElement addNavigatorFolder(String canonicalName, LocalizedString caption) {
        NavigatorElement elem = new NavigatorFolder(canonicalName, caption);
        addNavigatorElement(elem);
        return elem;
    }

    protected NavigatorAction addNavigatorAction(LA<?> property, String canonicalName, LocalizedString caption) {
        NavigatorAction navigatorAction = new NavigatorAction(property.action, canonicalName, caption, null);
        addNavigatorElement(navigatorAction);
        return navigatorAction;
    }

    protected LA<?> getNavigatorAction(FormEntity form) {
        return baseLM.getFormNavigatorAction(form);
    }

    protected NavigatorElement addNavigatorForm(FormEntity form, String canonicalName, LocalizedString caption) {
        NavigatorAction navigatorForm = new NavigatorAction(getNavigatorAction(form).action, canonicalName, caption, form);

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
    
    public Collection<FormEntity> getAllModuleForms() {
        List<FormEntity> elements = new ArrayList<>();
        elements.addAll(unnamedForms);
        elements.addAll(namedForms.values());
        return elements;
    }

    // need this mark because unnamed forms can be added during / after finalization
    private boolean formsFinalized;
    public void markFormsForFinalization() {
        formsFinalized = true;
    }
    // need this mark because unnamed properties can be added during / after finalization
    private boolean propsFinalized;
    public void markPropsForFinalization() {
        propsFinalized = true;
    }

    public NavigatorElement getNavigatorElement(String name) {
        return navigatorElements.get(name);
    }

    public FormEntity getForm(String name) {
        return namedForms.get(name);
    }
    
    public void addFormEntity(FormEntity form) {
        assert form.isNamed();
        assert !(form instanceof AutoFormEntity);
        assert !formsFinalized;

        assert !namedForms.containsKey(form.getName());
        namedForms.put(form.getName(), form);
    }
    @NFLazy
    public boolean addAutoFormEntityNotFinalized(AutoFormEntity form) {
        assert !form.isNamed();
        return unnamedForms.add(form);
    }
    public void addAutoFormEntity(AutoFormEntity form) {
        boolean added = addAutoFormEntityNotFinalized(form);
        if(formsFinalized && added) // last check is recursion guard
            form.finalizeAroundInit();
    }
    
    @NFLazy
    private void addNavigatorElement(NavigatorElement element) {
        assert !navigatorElements.containsKey(element.getName());
        navigatorElements.put(element.getName(), element);
    }

    public void addFormActions(FormEntity form, ObjectEntity object, FormSessionScope scope) {
        Version version = getVersion();
        addFormAction(form, getAddFormAction(form, object, null), scope, version);
        addFormAction(form, getEditFormAction(object, null), scope, version, object);
        addFormAction(form, getDeleteAction(object), scope, version, object);
    }

    public void addFormAction(FormEntity form, LA formAction, FormSessionScope scope, Version version, ObjectEntity... objects) {
        PropertyDrawEntity propertyDraw = form.addPropertyDraw(formAction, version, SetFact.toOrderExclSet(objects));
        propertyDraw.defaultChangeEventScope = scope;
    }

    public LA getAddFormAction(FormEntity contextForm, ObjectEntity contextObject, CustomClass explicitClass) {
        CustomClass cls = explicitClass;
        if(cls == null)
            cls = (CustomClass)contextObject.baseClass;
        return baseLM.getAddFormAction(cls, contextForm, contextObject);
    }

    public LA getEditFormAction(ObjectEntity object, CustomClass explicitClass) {
        CustomClass cls = explicitClass;
        if(cls == null)
            cls = (CustomClass) object.baseClass;
        return baseLM.getEditFormAction(cls);
    }

    public LA getDeleteAction(ObjectEntity object) {
        CustomClass cls = (CustomClass) object.baseClass;
        return baseLM.getDeleteAction(cls);
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

    public List<ResolveClassSet> getParamClasses(LAP<?, ?> lp) {
        List<ResolveClassSet> paramClasses;
        if (lp instanceof LP && locals.containsKey(lp)) {
            paramClasses = locals.get(lp).signature;
        } else {
            paramClasses = propClasses.get(lp);
        }
        return paramClasses == null ? Collections.nCopies(lp.listInterfaces.size(), null) : paramClasses;                   
    }

    // для обратной совместимости
    public void addFormFixedFilter(FormEntity form, FilterEntity filter) {
        form.addFixedFilter(filter, getVersion());
    }

    public RegularFilterGroupEntity newRegularFilterGroupEntity(int id) {
        return new RegularFilterGroupEntity(id, getVersion());
    }

    public void addFormHintsIncrementTable(FormEntity form, LP... lps) {
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

    public Group getRootGroup() {
        return baseLM.rootGroup;
    }

    public Group getPublicGroup() {
        return baseLM.publicGroup;
    }

    public Group getBaseGroup() {
        return baseLM.baseGroup;
    }

    public Group getRecognizeGroup() {
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

    protected <P extends PropertyInterface> void addLocal(LP<P> lcp, LocalPropertyData data) {
        locals.put(lcp, data);
        lcp.property.setCanonicalName(getNamespace(), data.name, data.signature, lcp.listInterfaces);
    }

    protected void removeLocal(LP<?> lcp) {
        assert locals.containsKey(lcp);
        locals.remove(lcp);
    }

    public List<ResolveClassSet> getLocalSignature(LP<?> lcp) {
        assert locals.containsKey(lcp);
        return locals.get(lcp).signature;
    }

    public Map<LP<?>, LocalPropertyData> getLocals() {
        return locals;
    }

    public LP<?> resolveProperty(String compoundName, List<ResolveClassSet> params) throws ResolvingErrors.ResolvingError {
        return resolveManager.findProperty(compoundName, params);
    }

    public LP<?> resolveAbstractProperty(String compoundName, List<ResolveClassSet> params, boolean prioritizeNotEquals) throws ResolvingErrors.ResolvingError {
        return resolveManager.findAbstractProperty(compoundName, params, prioritizeNotEquals);
    }

    public LA<?> resolveAction(String compoundName, List<ResolveClassSet> params) throws ResolvingErrors.ResolvingError {
        return resolveManager.findAction(compoundName, params);
    }

    public LA<?> resolveAbstractAction(String compoundName, List<ResolveClassSet> params, boolean prioritizeNotEquals) throws ResolvingErrors.ResolvingError {
        return resolveManager.findAbstractAction(compoundName, params, prioritizeNotEquals);
    }

    public ValueClass resolveClass(String compoundName) throws ResolvingErrors.ResolvingError {
        return resolveManager.findClass(compoundName);
    }

    public MetaCodeFragment resolveMetaCodeFragment(String compoundName, int paramCnt) throws ResolvingErrors.ResolvingError {
        return resolveManager.findMetaCodeFragment(compoundName, paramCnt);
    }
    
    public Group resolveGroup(String compoundName) throws ResolvingErrors.ResolvingError {
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

    public enum InsertType {IN, BEFORE, AFTER, FIRST}
}

