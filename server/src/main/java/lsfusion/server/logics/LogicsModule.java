package lsfusion.server.logics;

import lsfusion.base.BaseUtils;
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
import lsfusion.interop.*;
import lsfusion.server.caches.IdentityInstanceLazy;
import lsfusion.server.caches.IdentityStrongLazy;
import lsfusion.server.classes.*;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.Time;
import lsfusion.server.data.Union;
import lsfusion.server.data.expr.StringAggUnionProperty;
import lsfusion.server.data.expr.formula.DivideFormulaImpl;
import lsfusion.server.data.expr.formula.MultiplyFormulaImpl;
import lsfusion.server.data.expr.formula.SubtractFormulaImpl;
import lsfusion.server.data.expr.formula.SumFormulaImpl;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.query.PartitionType;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.entity.*;
import lsfusion.server.form.instance.FormSessionScope;
import lsfusion.server.form.navigator.NavigatorAction;
import lsfusion.server.form.navigator.NavigatorElement;
import lsfusion.server.form.window.AbstractWindow;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.*;
import lsfusion.server.logics.property.actions.flow.*;
import lsfusion.server.logics.property.derived.*;
import lsfusion.server.logics.property.group.AbstractGroup;
import lsfusion.server.logics.scripted.EvalActionProperty;
import lsfusion.server.logics.scripted.MetaCodeFragment;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.logics.table.ImplementTable;
import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

import static java.util.Arrays.copyOfRange;
import static lsfusion.base.BaseUtils.add;
import static lsfusion.server.logics.PropertyUtils.*;
import static lsfusion.server.logics.property.derived.DerivedProperty.createAnd;
import static lsfusion.server.logics.property.derived.DerivedProperty.createStatic;

/**
 * User: DAle
 * Date: 16.05.11
 * Time: 17:37
 */

public abstract class LogicsModule {
    protected static final Logger logger = Logger.getLogger(LogicsModule.class);

    // после этого шага должны быть установлены name, namespace, requiredModules
    public abstract void initModuleDependencies() throws RecognitionException;

    public abstract void initModule() throws RecognitionException;

    public abstract void initClasses() throws RecognitionException;

    public abstract void initTables() throws RecognitionException;

    public abstract void initGroups() throws RecognitionException;

    public abstract void initProperties() throws FileNotFoundException, RecognitionException;

    public abstract void initIndexes() throws RecognitionException;

    public String getErrorsDescription() { return "";}

    /// Добавляется к SID объектов модуля: классам, группам, свойствам...
    public abstract String getNamePrefix();

    public String transformSIDToName(String sid) {
        String modulePrefix = getNamePrefix();
        if (modulePrefix == null) {
            return sid;
        }

        if (sid == null || !sid.startsWith(modulePrefix)) {
            throw new IllegalArgumentException("SID must not be null and begin with name prefix");
        }

        return sid.substring(modulePrefix.length() + 1);
    }

    // Используется для всех элементов системы кроме свойств и действий
    public String transformNameToSID(String name) {
        return transformNameToSID(getNamePrefix(), name);
    }

    public static String transformNameToSID(String modulePrefix, String name) {
        if (modulePrefix == null) {
            return name;
        } else {
            return modulePrefix + "_" + name;
        }
    }

    public BaseLogicsModule<?> baseLM;

    private final Map<String, List<LP<?,?>>> namedModuleProperties = new HashMap<String, List<LP<?, ?>>>();
    private final Map<String, AbstractGroup> moduleGroups = new HashMap<String, AbstractGroup>();
    private final Map<String, ValueClass> moduleClasses = new HashMap<String, ValueClass>();
    private final Map<String, AbstractWindow> windows = new HashMap<String, AbstractWindow>();
    public final Map<String, NavigatorElement<?>> moduleNavigators = new HashMap<String, NavigatorElement<?>>();
    private final Map<String, ImplementTable> moduleTables = new HashMap<String, ImplementTable>();

    public final Map<LP<?, ?>, List<AndClassSet>> propClasses = new HashMap<LP<?, ?>, List<AndClassSet>>();
    
    private final Map<String, List<String>> propNamedParams = new HashMap<String, List<String>>();
    private final Map<Pair<String, Integer>, MetaCodeFragment> metaCodeFragments = new HashMap<Pair<String, Integer>, MetaCodeFragment>();

    protected LogicsModule() {}

    public LogicsModule(String name) {
        this(name, name);
    }

    public LogicsModule(String name, String namespace) {
        this(name, namespace, new HashSet<String>());
    }

    public LogicsModule(String name, String namespace, Set<String> requiredModules) {
        this.name = name;
        this.namespace = namespace;
        this.requiredModules = requiredModules;
    }

    private String name;
    private String namespace;
    private Set<String> requiredModules;

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public LP<?, ?> getLPByOldName(String name) {
        List<LP<?, ?>> result = new ArrayList<LP<?, ?>>();
        for (List<LP<?, ?>> namedLPs : namedModuleProperties.values()) {
            for (LP<?, ?> property : namedLPs) {
                String actualName = property.property.getOldName() == null ? property.property.getName() : property.property.getOldName(); 
                if (name.equals(actualName)) {
                    result.add(property);                        
                }
            }
        }
        assert result.size() == 1;
        return result.get(0);
    }

    private List<LP<?, ?>> getAllLPByName(String name) {
        List<LP<?, ?>> allLP = namedModuleProperties.get(name); 
        return allLP != null ? allLP : new ArrayList<LP<?, ?>>(); 
    }
    
    public LCP<?> getLCPByOldName(String name) {
        return (LCP<?>) getLPByOldName(name);
    }

    public LAP<?> getLAPByOldName(String name) {
        return (LAP<?>) getLPByOldName(name);
    }

    public Map<String, List<LP<?,?>>> getNamedModuleProperties() {
        return namedModuleProperties;        
    }
    
    protected void addModuleLP(LP<?, ?> lp) {
        String name = lp.property.getName();
        if (name != null) {
            if (!namedModuleProperties.containsKey(name)) {
                namedModuleProperties.put(name, new ArrayList<LP<?, ?>>());
            } 
            namedModuleProperties.get(name).add(lp);
        }
    }

    protected void removeModuleLP(LP<?, ?> lp) {
        String name = lp.property.getName();
        if (name != null) {
            if (namedModuleProperties.containsKey(name)) {
                namedModuleProperties.get(name).remove(lp);
                if (namedModuleProperties.get(name).isEmpty()) {
                    namedModuleProperties.remove(name);
                }
            }
        }
    }

    public AbstractGroup getGroupBySID(String sid) {
        return moduleGroups.get(sid);
    }

    public AbstractGroup getGroupByName(String name) {
        return getGroupBySID(transformNameToSID(name));
    }

    protected void addModuleGroup(AbstractGroup group) {
        assert !moduleGroups.containsKey(group.getSID());
        moduleGroups.put(group.getSID(), group);
    }

    public ValueClass getClassBySID(String sid) {
        return moduleClasses.get(sid);
    }

    public ValueClass getClassByName(String name) {
        return getClassBySID(transformNameToSID(name));
    }

    protected void addModuleClass(ValueClass valueClass) {
        assert !moduleClasses.containsKey(valueClass.getSID());
        moduleClasses.put(valueClass.getSID(), valueClass);
    }

    public ImplementTable getTableBySID(String sid) {
        return moduleTables.get(sid);
    }

    public ImplementTable getTableByName(String name) {
        return getTableBySID(transformNameToSID(name));
    }

    protected void addModuleTable(ImplementTable table) {
        assert !moduleTables.containsKey(table.name);
        moduleTables.put(table.name, table);
    }

    protected <T extends AbstractWindow> T addWindow(T window) {
        assert !windows.containsKey(window.getSID());
        windows.put(window.getSID(), window);
        return window;
    }

    protected <T extends AbstractWindow> T addWindow(String name, T window) {
        window.setSID(transformNameToSID(name));
        return addWindow(window);
    }

    public AbstractWindow getWindowByName(String name) {
        return getWindowBySID(transformNameToSID(name));
    }

    public AbstractWindow getWindowBySID(String sid) {
        return windows.get(sid);
    }

    public List<String> getNamedParams(String sID) {
        return propNamedParams.get(sID);
    }

    protected void addNamedParams(String sID, List<String> namedParams) {
        assert !propNamedParams.containsKey(sID);
        propNamedParams.put(sID, namedParams);
    }

    public MetaCodeFragment getMetaCodeFragmentByName(String name, int paramCnt) {
        return getMetaCodeFragmentBySID(transformNameToSID(name), paramCnt);
    }

    protected MetaCodeFragment getMetaCodeFragmentBySID(String sid, int paramCnt) {
        return metaCodeFragments.get(new Pair<String, Integer>(sid, paramCnt));
    }

    protected void addMetaCodeFragment(String name, MetaCodeFragment fragment) {
        assert !metaCodeFragments.containsKey(new Pair<String, Integer>(transformNameToSID(name), fragment.parameters.size()));
        metaCodeFragments.put(new Pair<String, Integer>(transformNameToSID(name), fragment.parameters.size()), fragment);
    }

    // aliases для использования внутри иерархии логических модулей
    protected BaseClass baseClass;

    public AbstractGroup rootGroup;
    public AbstractGroup publicGroup;
    public AbstractGroup privateGroup;
    public AbstractGroup baseGroup;
    public AbstractGroup recognizeGroup;

    protected void setBaseLogicsModule(BaseLogicsModule<?> baseLM) {
        this.baseLM = baseLM;
    }

    protected void initBaseGroupAliases() {
        this.rootGroup = baseLM.rootGroup;
        this.publicGroup = baseLM.publicGroup;
        this.privateGroup = baseLM.privateGroup;
        this.baseGroup = baseLM.baseGroup;
        this.recognizeGroup = baseLM.recognizeGroup;
    }

    protected void initBaseClassAliases() {
        this.baseClass = baseLM.baseClass;
    }

    protected AbstractGroup addAbstractGroup(String name, String caption) {
        return addAbstractGroup(name, caption, null);
    }

    protected AbstractGroup addAbstractGroup(String name, String caption, AbstractGroup parent) {
        return addAbstractGroup(name, caption, parent, true);
    }

    protected AbstractGroup addAbstractGroup(String name, String caption, AbstractGroup parent, boolean toCreateContainer) {
        AbstractGroup group = new AbstractGroup(transformNameToSID(name), caption);
        if (parent != null) {
            parent.add(group);
        } else {
            if (privateGroup != null)
                privateGroup.add(group);
        }
        group.createContainer = toCreateContainer;
        addModuleGroup(group);
        return group;
    }

    protected void storeCustomClass(CustomClass customClass) {
        addModuleClass(customClass);
        assert !baseLM.sidToClass.containsKey(customClass.getSID());
        baseLM.sidToClass.put(customClass.getSID(), customClass);
    }

    protected BaseClass addBaseClass(String sID, String caption) {
        BaseClass baseClass = new BaseClass(sID, caption);
        storeCustomClass(baseClass);
        return baseClass;
    }

    protected CustomClass findCustomClass(String sid) {
        return baseLM.sidToClass.get(sid);
    }

    protected ValueClass findValueClass(String sid) {
        ValueClass valueClass = findCustomClass(sid);
        if (valueClass == null) {
            valueClass = DataClass.findDataClass(sid);
        }
        return valueClass;
    }

    protected ConcreteCustomClass addConcreteClass(String name, String caption, CustomClass... parents) {
        return addConcreteClass(name, caption, new ArrayList<String>(), new ArrayList<String>(), parents);
    }

    protected ConcreteCustomClass addConcreteClass(String name, String caption, String[] sids, String[] names, CustomClass... parents) {
        return addConcreteClass(name, caption, BaseUtils.toList(sids), BaseUtils.toList(names), parents);
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

    protected ConcreteCustomClass addConcreteClass(String name, String caption, List<String> sids, List<String> names, CustomClass... parents) {
        assert parents.length > 0;
        ConcreteCustomClass customClass = new ConcreteCustomClass(transformNameToSID(name), caption, sids, names, parents);
        storeCustomClass(customClass);
        return customClass;
    }

    protected AbstractCustomClass addAbstractClass(String name, String caption, CustomClass... parents) {
        AbstractCustomClass customClass = new AbstractCustomClass(transformNameToSID(name), caption, parents);
        storeCustomClass(customClass);
        return customClass;
    }

    protected ImplementTable addTable(String name, ValueClass... classes) {
        ImplementTable table = baseLM.tableFactory.include(transformNameToSID(name), classes);
        addModuleTable(table);
        return table;
    }

    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////
    /// Properties
    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////

    public String genSID() {
        String id = "property" + baseLM.idCounter++;
        baseLM.idSet.add(id);
        return id;
    }

    // ------------------- DATA ----------------- //

    protected LCP addDProp(String name, String caption, ValueClass value, ValueClass... params) {
        return addDProp(null, name, false, caption, value, params);
    }

    protected LCP addDProp(AbstractGroup group, String name, String caption, ValueClass value, ValueClass... params) {
        return addDProp(group, name, false, caption, value, params);
    }

    protected LCP addDProp(AbstractGroup group, String name, boolean persistent, String caption, ValueClass value, ValueClass... params) {
        StoredDataProperty dataProperty = new StoredDataProperty(name, caption, params, value);
        LCP lp = addProperty(group, persistent, new LCP<ClassPropertyInterface>(dataProperty));
        dataProperty.markStored(baseLM.tableFactory);
        return lp;
    }

    // ------------------- Loggable ----------------- //

    protected <D extends PropertyInterface> LCP addDCProp(String name, String caption, int whereNum, LCP<D> derivedProp, Object... params) {
        return addDCProp(null, name, caption, whereNum, derivedProp, params);
    }

    protected <D extends PropertyInterface> LCP addDCProp(AbstractGroup group, String name, String caption, int whereNum,  LCP<D> derivedProp, Object... params) {
        return addDCProp(group, name, false, caption, false, whereNum, derivedProp, params);
    }

    protected <D extends PropertyInterface> LCP addDCProp(AbstractGroup group, String name, boolean persistent, String caption, boolean forced, int whereNum, LCP<D> derivedProp, Object... params) {

        // считываем override'ы с конца
        List<ValueClass> backClasses = new ArrayList<ValueClass>();
        int i = params.length - 1;
        while (i > 0 && (params[i] == null || params[i] instanceof ValueClass))
            backClasses.add((ValueClass) params[i--]);
        params = copyOfRange(params, 0, i + 1);
        ValueClass[] overrideClasses = BaseUtils.reverseThis(backClasses).toArray(new ValueClass[0]);

        boolean defaultChanged = false;
        if (params[0] instanceof Boolean) {
            defaultChanged = (Boolean) params[0];
            params = copyOfRange(params, 1, params.length);
        }

        // придется создавать Join свойство чтобы считать его класс

        int propsize = derivedProp.listInterfaces.size();
        int dersize = getIntNum(params);
        ImOrderSet<JoinProperty.Interface> listInterfaces = JoinProperty.getInterfaces(dersize);

        ImList<CalcPropertyInterfaceImplement<JoinProperty.Interface>> list = readCalcImplements(listInterfaces, params);
        final ImList<CalcPropertyInterfaceImplement<JoinProperty.Interface>> subList = list.subList(propsize, list.size());

        AndFormulaProperty andProperty = new AndFormulaProperty(genSID(), list.size() - propsize);
        ImMap<AndFormulaProperty.Interface, CalcPropertyInterfaceImplement<JoinProperty.Interface>> mapImplement =
                    MapFact.<AndFormulaProperty.Interface, CalcPropertyInterfaceImplement<JoinProperty.Interface>>addExcl(
                            andProperty.andInterfaces.mapValues(new GetIndex<CalcPropertyInterfaceImplement<JoinProperty.Interface>>() {
                                public CalcPropertyInterfaceImplement<JoinProperty.Interface> getMapValue(int i) {
                                    return subList.get(i);
                                }
                            }), andProperty.objectInterface, DerivedProperty.createJoin(mapCalcImplement(derivedProp, list.subList(0, propsize))));

        JoinProperty<AndFormulaProperty.Interface> joinProperty = new JoinProperty<AndFormulaProperty.Interface>(name, caption, listInterfaces, false,
                new CalcPropertyImplement<AndFormulaProperty.Interface, CalcPropertyInterfaceImplement<JoinProperty.Interface>>(andProperty, mapImplement));
        LCP<JoinProperty.Interface> listProperty = new LCP<JoinProperty.Interface>(joinProperty, listInterfaces);

        // получаем классы
        ValueClass[] commonClasses = listProperty.getInterfaceClasses();

        // override'им классы
        ValueClass valueClass = listProperty.property.getValueClass();
        if (overrideClasses.length > dersize) {
            valueClass = overrideClasses[dersize];
            assert !overrideClasses[dersize].isCompatibleParent(valueClass);
            overrideClasses = copyOfRange(params, 0, dersize, ValueClass[].class);
        }

        // выполняем само создание свойства
        LCP derDataProp = addDProp(group, name, persistent, caption, valueClass, overrideClasses(commonClasses, overrideClasses));
        if (forced)
            derDataProp.setEventChangeSet(defaultChanged, whereNum, derivedProp, params);
        else
            derDataProp.setEventChange(defaultChanged, whereNum, derivedProp, params);
        return derDataProp;
    }

    // ------------------- Scripted DATA ----------------- //

    protected LCP addSDProp(String name, String caption, ValueClass value, ValueClass... params) {
        return addSDProp(null, name, caption, value, params);
    }

    protected LCP addSDProp(AbstractGroup group, String name, String caption, ValueClass value, ValueClass... params) {
        return addSDProp(group, name, false, caption, value, params);
    }

    protected LCP addSDProp(AbstractGroup group, String name, boolean persistent, String caption, ValueClass value, ValueClass... params) {
        return addProperty(group, persistent, new LCP<ClassPropertyInterface>(new SessionDataProperty(name, caption, params, value)));
    }

    // ------------------- Multi File action ----------------- //

    public LAP addMFAProp(String caption, FormEntity form, ObjectEntity... params) {
        return addMFAProp(null, caption, form, params, false);
    }

    protected LAP addMFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, boolean newSession) {
        return addMFAProp(group, genSID(), caption, form, objectsToSet, newSession);
    }

    protected LAP addMFAProp(AbstractGroup group, String sID, String caption, FormEntity form, ObjectEntity[] objectsToSet, boolean newSession) {
        return addMFAProp(group, sID, caption, form, objectsToSet, null, newSession);
    }

    protected LAP addMFAProp(AbstractGroup group, String sID, String caption, FormEntity form, ObjectEntity[] objectsToSet, ActionPropertyObjectEntity startAction, boolean newSession) {
        return addFAProp(group, sID, caption, form, objectsToSet, startAction, newSession, true, false);
    }

    // ------------------- Data Multi File action ----------------- //

    protected LAP addDMFAProp(AbstractGroup group, String sID, String caption, FormEntity form, ObjectEntity[] objectsToSet, boolean newSession) {
        return addDMFAProp(group, sID, caption, form, objectsToSet, null, newSession);
    }

    protected LAP addDMFAProp(AbstractGroup group, String sID, String caption, FormEntity form, ObjectEntity[] objectsToSet, ActionPropertyObjectEntity startAction, boolean newSession) {
        return addFAProp(group, sID, caption, form, objectsToSet, startAction,
                newSession ? FormSessionScope.NEWSESSION : FormSessionScope.OLDSESSION,
                ModalityType.DOCKED_MODAL, false);
    }

    // ------------------- File action ----------------- //

    protected LAP addFAProp(AbstractGroup group, String sID, String caption, FormEntity form, ObjectEntity[] objectsToSet, ActionPropertyObjectEntity startAction, boolean newSession, boolean isModal, boolean checkOnOk) {
        return addFAProp(group, sID, caption, form, objectsToSet, startAction, newSession, isModal, checkOnOk, null);
    }

    protected LAP addFAProp(AbstractGroup group, String sID, String caption, FormEntity form, ObjectEntity[] objectsToSet, ActionPropertyObjectEntity startAction, boolean newSession, boolean isModal, boolean checkOnOk, FormPrintType printType) {
        return addFAProp(group, sID, caption, form, objectsToSet, startAction,
                newSession ? FormSessionScope.NEWSESSION : FormSessionScope.OLDSESSION,
                isModal ? ModalityType.MODAL : ModalityType.DOCKED, checkOnOk, printType);
    }

    protected LAP addFAProp(AbstractGroup group, String sID, String caption, FormEntity form, ObjectEntity[] objectsToSet, ActionPropertyObjectEntity startAction, FormSessionScope sessionScope, ModalityType modalityType, boolean checkOnOk) {
        return addFAProp(group, sID, caption, form, objectsToSet, startAction, null, null, sessionScope, modalityType, checkOnOk, false);
    }

    protected LAP addFAProp(AbstractGroup group, String sID, String caption, FormEntity form, ObjectEntity[] objectsToSet, ActionPropertyObjectEntity startAction, FormSessionScope sessionScope, ModalityType modalityType, boolean checkOnOk, FormPrintType printType) {
        return addFAProp(group, sID, caption, form, objectsToSet, startAction, null, null, null, sessionScope, modalityType, checkOnOk, false, printType);
    }

    protected LAP addFAProp(AbstractGroup group, String sID, String caption, FormEntity form, ObjectEntity[] objectsToSet, ActionPropertyObjectEntity startAction, ObjectEntity contextObject, CalcProperty contextProperty, FormSessionScope sessionScope, ModalityType modalityType, boolean checkOnOk, boolean showDrop) {
        return addFAProp(group, sID, caption, form, objectsToSet, startAction, null, null, null, sessionScope, modalityType, checkOnOk, false, null);
    }

    protected LAP addFAProp(AbstractGroup group, String sID, String caption, FormEntity form, ObjectEntity[] objectsToSet, ActionPropertyObjectEntity startAction, ObjectEntity contextObject, CalcProperty contextProperty, PropertyDrawEntity initFilterProperty, FormSessionScope sessionScope, ModalityType modalityType, boolean checkOnOk, boolean showDrop, FormPrintType printType) {
        return addProperty(group, new LAP(new FormActionProperty(sID, caption, form, objectsToSet, startAction, sessionScope, modalityType, checkOnOk, showDrop, printType, baseLM.formResult, baseLM.getFormResultProperty(), baseLM.getChosenValueProperty(), contextObject, contextProperty, initFilterProperty)));
    }

    // ------------------- Change Class action ----------------- //

    protected LAP addChangeClassAProp(String sID, ConcreteObjectClass cls, int resInterfaces, int changeIndex, boolean extendedContext, boolean conditional, Object... params) {
        int innerIntCnt = resInterfaces + (extendedContext ? 1 : 0);
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(innerIntCnt);
        ImOrderSet<PropertyInterface> mappedInterfaces = extendedContext ? innerInterfaces.removeOrderIncl(innerInterfaces.get(changeIndex)) : innerInterfaces;
        ImList<CalcPropertyInterfaceImplement<PropertyInterface>> readImplements = readCalcImplements(innerInterfaces, params);
        CalcPropertyMapImplement<PropertyInterface, PropertyInterface> conditionalPart = (CalcPropertyMapImplement<PropertyInterface, PropertyInterface>)
                (conditional ? readImplements.get(resInterfaces) : null);

        return addAProp(new ChangeClassActionProperty<PropertyInterface, PropertyInterface>(sID, cls, false, innerInterfaces.getSet(),
                mappedInterfaces, innerInterfaces.get(changeIndex), conditionalPart, baseClass));
    }

    // ------------------- Set property action ----------------- //

    protected <C extends PropertyInterface, W extends PropertyInterface> LAP addSetPropertyAProp(AbstractGroup group, String name, String caption, int resInterfaces,
                                                                                                 boolean conditional, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(getIntNum(params));
        ImList<CalcPropertyInterfaceImplement<PropertyInterface>> readImplements = readCalcImplements(innerInterfaces, params);
        CalcPropertyMapImplement<W, PropertyInterface> conditionalPart = (CalcPropertyMapImplement<W, PropertyInterface>)
                (conditional ? readImplements.get(resInterfaces + 2) : DerivedProperty.createTrue());
        return addProperty(group, new LAP(new SetActionProperty<C, W, PropertyInterface>(name, caption,
                innerInterfaces.getSet(), (ImOrderSet) readImplements.subList(0, resInterfaces).toOrderExclSet(), conditionalPart,
                (CalcPropertyMapImplement<C, PropertyInterface>) readImplements.get(resInterfaces), readImplements.get(resInterfaces + 1))));
    }

    // ------------------- List action ----------------- //

    protected LAP addListAProp(Object... params) {
        return addListAProp(genSID(), "sys", params);
    }
    protected LAP addListAProp(String name, String caption, Object... params) {
        return addListAProp(null, name, caption, params);
    }
    protected LAP addListAProp(AbstractGroup group, String name, String caption, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        return addProperty(group, new LAP(new ListActionProperty(name, caption, listInterfaces,
                readActionImplements(listInterfaces, params))));
    }

    protected LAP addAbstractListAProp(boolean isChecked, ValueClass[] params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(params.length);
        return addProperty(null, new LAP(new ListActionProperty(genSID(), "sys", isChecked, listInterfaces, listInterfaces.mapList(ListFact.toList(params)))));
    }

    // ------------------- If action ----------------- //

    protected LAP addIfAProp(AbstractGroup group, String caption, boolean not, Object... params) {
        return addIfAProp(group, genSID(), caption, not, params);
    }
    protected LAP addIfAProp(AbstractGroup group, String name, String caption, boolean not, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<PropertyInterfaceImplement<PropertyInterface>> readImplements = readImplements(listInterfaces, params);
        assert readImplements.size() >= 2 && readImplements.size() <= 3;

        return addProperty(group, new LAP(CaseActionProperty.createIf(name, caption, not, listInterfaces, (CalcPropertyMapImplement<?, PropertyInterface>) readImplements.get(0),
                (ActionPropertyMapImplement<?, PropertyInterface>) readImplements.get(1), readImplements.size() == 3 ? (ActionPropertyMapImplement<?, PropertyInterface>) readImplements.get(2) : null)));
    }

    // ------------------- Case action ----------------- //

    protected LAP addCaseAProp(boolean isExclusive, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<PropertyInterfaceImplement<PropertyInterface>> readImplements = readImplements(listInterfaces, params);

        MList<CaseActionProperty.Case<PropertyInterface>> mCases = ListFact.mList();
        for (int i = 0; i*2+1 < readImplements.size(); i++) {
            mCases.add(new CaseActionProperty.Case((CalcPropertyMapImplement<?, PropertyInterface>) readImplements.get(i*2), (ActionPropertyMapImplement<?, PropertyInterface>) readImplements.get(i*2+1)));
        }
        if(readImplements.size() % 2 != 0) {
            mCases.add(new CaseActionProperty.Case(DerivedProperty.createTrue(), (ActionPropertyMapImplement<?, PropertyInterface>) readImplements.get(readImplements.size() - 1)));
        }
        return addProperty(null, new LAP(new CaseActionProperty(genSID(), "", isExclusive, listInterfaces, mCases.immutableList())));
    }

    protected LAP addMultiAProp(boolean isExclusive, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<PropertyInterfaceImplement<PropertyInterface>> readImplements = readImplements(listInterfaces, params);

        MList<ActionPropertyMapImplement> mCases = ListFact.mList();
        for (int i = 0; i < readImplements.size(); i++) {
            mCases.add((ActionPropertyMapImplement) readImplements.get(i));
        }
        return addProperty(null, new LAP(new CaseActionProperty(genSID(), "", isExclusive, mCases.immutableList(), listInterfaces)));
    }

    protected LAP addAbstractCaseAProp(ListCaseActionProperty.AbstractType type, boolean isExclusive, boolean isChecked, ValueClass[] params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(params.length);
        return addProperty(null, new LAP(new CaseActionProperty(genSID(), "sys", isExclusive, isChecked, type, listInterfaces, listInterfaces.mapList(ListFact.toList(params)))));
    }

    // ------------------- For action ----------------- //

    protected LAP addForAProp(AbstractGroup group, String name, String caption, boolean ascending, boolean ordersNotNull, boolean recursive, boolean hasElse, int resInterfaces, CustomClass addClass, boolean hasCondition, int noInline, boolean forceInline, Object... params) {
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

        return addProperty(group, new LAP<PropertyInterface>(
                new ForActionProperty<PropertyInterface>(name, caption, innerInterfaces.getSet(), mapInterfaces, ifProp, orders, ordersNotNull, action, elseAction, addedInterface, addClass, false, recursive, noInlineInterfaces, forceInline))
        );
    }

    // ------------------- JOIN ----------------- //

    protected LAP addJoinAProp(LAP action, Object... params) {
        return addJoinAProp("sys", action, params);
    }

    protected LAP addJoinAProp(String caption, LAP action, Object... params) {
        return addJoinAProp(genSID(), caption, action, params);
    }

    protected LAP addJoinAProp(String name, String caption, LAP action, Object... params) {
        return addJoinAProp(null, name, caption, action, params);
    }

    protected LAP addJoinAProp(AbstractGroup group, String name, String caption, LAP action, Object... params) {
        return addJoinAProp(group, name, caption, null, action, params);
    }

    protected LAP addJoinAProp(AbstractGroup group, String name, String caption, ValueClass[] classes, LAP action, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<CalcPropertyInterfaceImplement<PropertyInterface>> readImplements = readCalcImplements(listInterfaces, params);
        return addProperty(group, new LAP(new JoinActionProperty(name, caption, listInterfaces, mapActionImplement(action, readImplements))));
    }

    // ------------------- NEWSESSION ----------------- //

    protected LAP addNewSessionAProp(AbstractGroup group, String name, String caption, LAP action, boolean doApply, boolean singleApply, ImSet<SessionDataProperty> local, ImSet<SessionDataProperty> sessionUsed) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(action.listInterfaces.size());
        ActionPropertyMapImplement<?, PropertyInterface> actionImplement = mapActionListImplement(action, listInterfaces);

        return addProperty(group, new LAP(new NewSessionActionProperty(name, caption, listInterfaces, actionImplement, doApply,
                singleApply, sessionUsed, local)));
    }

    protected LAP addNewThreadAProp(AbstractGroup group, String name, String caption, LAP action, long delay, Long period) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(action.listInterfaces.size());
        ActionPropertyMapImplement<?, PropertyInterface> actionImplement = mapActionListImplement(action, listInterfaces);

        return addProperty(group, new LAP(new NewThreadActionProperty(name, caption, listInterfaces, actionImplement, period, delay)));
    }

    // ------------------- Request action ----------------- //

    protected LP addRequestUserInputAProp(AbstractGroup group, String name, String caption, LAP action, Type requestValueType, String chosenKey) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(action.listInterfaces.size());
        ActionPropertyMapImplement<?, PropertyInterface> actionImplement = mapActionListImplement(action, listInterfaces);

        return addProperty(group, new LAP(
                new RequestUserInputActionProperty(name, caption, listInterfaces, actionImplement,
                        requestValueType, chosenKey,
                        baseLM.getRequestCanceledProperty(), baseLM.getRequestedValueProperty(),
                        baseLM.getChosenValueProperty(), baseLM.formResult, baseLM.getFormResultProperty()))
        );
    }

    protected LAP addRequestUserDataAProp(AbstractGroup group, String name, String caption, DataClass dataClass) {
        return addAProp(group, new RequestUserDataActionProperty(name, caption, dataClass, baseLM.getRequestCanceledProperty(), baseLM.getRequestedValueProperty()));
    }

    // ------------------- Constant ----------------- //

    protected <T extends PropertyInterface> LCP addCProp(StaticClass valueClass, Object value) {
        return baseLM.addCProp(valueClass, value);
    }

    // ------------------- TIME ----------------- //

    protected LCP addTProp(String sID, String caption, Time time) {
        return addProperty(null, new LCP<PropertyInterface>(new TimeFormulaProperty(sID, caption, time)));
    }

    // ------------------- FORMULA ----------------- //

    protected LCP addSFProp(String name, String formula, int paramCount) {
        return addSFProp(name, formula, null, paramCount);
    }

    protected LCP addSFProp(String formula, int paramCount) {
        return addSFProp(formula, (DataClass) null, paramCount);
    }

    protected LCP addSFProp(String formula, DataClass value, int paramCount) {
        return addSFProp(genSID(), formula, value, paramCount);
    }

    protected LCP addSFProp(String name, String formula, DataClass value, int paramCount) {
        return addProperty(null, new LCP<StringFormulaProperty.Interface>(new StringFormulaProperty(name, value, formula, paramCount)));
    }

    // ------------------- Операции сравнения ----------------- //

    protected LCP addCFProp(Compare compare) {
        return addCFProp(genSID(), compare);
    }

    protected LCP addCFProp(String name, Compare compare) {
        return addProperty(null, new LCP<CompareFormulaProperty.Interface>(new CompareFormulaProperty(name, compare)));
    }

    // ------------------- Алгебраические операции ----------------- //

    protected LCP addSumProp(String name) {
        return addProperty(null, new LCP<FormulaImplProperty.Interface>(new FormulaImplProperty(name, "sum", 2, new SumFormulaImpl())));
    }

    protected LCP addMultProp(String name) {
        return addProperty(null, new LCP<FormulaImplProperty.Interface>(new FormulaImplProperty(name, "multiply", 2, new MultiplyFormulaImpl())));
    }

    protected LCP addSubtractProp(String name) {
        return addProperty(null, new LCP<FormulaImplProperty.Interface>(new FormulaImplProperty(name, "subtract", 2, new SubtractFormulaImpl())));
    }

    protected LCP addDivideProp(String name) {
        return addProperty(null, new LCP<FormulaImplProperty.Interface>(new FormulaImplProperty(name, "divide", 2, new DivideFormulaImpl())));
    }

    // ------------------- cast ----------------- //

    protected <P extends PropertyInterface> LCP addCastProp(DataClass castClass) {
        return baseLM.addCastProp(castClass);
    }

    // ------------------- Операции со строками ----------------- //

    protected <P extends PropertyInterface> LCP addSProp(String name, int intNum) {
        return addSProp(name, intNum, " ");
    }

    protected <P extends PropertyInterface> LCP addSProp(String name, int intNum, String separator) {
        return addProperty(null, new LCP<StringConcatenateProperty.Interface>(new StringConcatenateProperty(name, ServerResourceBundle.getString("logics.join"), intNum, separator)));
    }

    protected <P extends PropertyInterface> LCP addInsensitiveSProp(String name, int intNum) {
        return addInsensitiveSProp(name, intNum, " ");
    }

    protected <P extends PropertyInterface> LCP addInsensitiveSProp(String name, int intNum, String separator) {
        return addProperty(null, new LCP<StringConcatenateProperty.Interface>(new StringConcatenateProperty(name, ServerResourceBundle.getString("logics.join"), intNum, separator, true)));
    }

    // ------------------- AND ----------------- //

    protected LCP addAFProp(boolean... nots) {
        return addAFProp((AbstractGroup) null, nots);
    }

    protected LCP addAFProp(String name, boolean... nots) {
        return addAFProp(null, name, nots);
    }

    protected LCP addAFProp(AbstractGroup group, boolean... nots) {
        return addAFProp(group, genSID(), nots);
    }

    protected LCP addAFProp(AbstractGroup group, String name, boolean... nots) {
        ImOrderSet<PropertyInterface> interfaces = genInterfaces(nots.length + 1);
        MList<Boolean> mList = ListFact.mList(nots.length);
        boolean wasNot = false;
        for(boolean not : nots) {
            mList.add(not);
            wasNot = wasNot || not;
        }
        if(wasNot)
            return mapLProp(group, false, DerivedProperty.createAnd(name, interfaces, mList.immutableList()), interfaces);
        else
            return addProperty(group, new LCP<AndFormulaProperty.Interface>(new AndFormulaProperty(name, nots.length)));
    }

    // ------------------- concat ----------------- //

    protected LCP addCCProp(int paramCount) {
        return addProperty(null, new LCP<ConcatenateProperty.Interface>(new ConcatenateProperty(genSID(), paramCount)));
    }

    protected LCP addDCCProp(int paramIndex) {
        return addProperty(null, new LCP<DeconcatenateProperty.Interface>(new DeconcatenateProperty(genSID(), paramIndex, baseLM.baseClass)));
    }

    // ------------------- JOIN (продолжение) ----------------- //

    public LCP addJProp(LCP mainProp, Object... params) {
        return addJProp(baseLM.privateGroup, "sys", mainProp, params);
    }

    protected LCP addJProp(String caption, LCP mainProp, Object... params) {
        return addJProp((AbstractGroup) null, caption, mainProp, params);
    }

    protected LCP addJProp(String name, String caption, LCP mainProp, Object... params) {
        return addJProp(name, false, caption, mainProp, params);
    }

    protected LCP addJProp(String name, boolean persistent, String caption, LCP mainProp, Object... params) {
        return addJProp(null, name, persistent, caption, mainProp, params);
    }

    protected LCP addJProp(AbstractGroup group, String caption, LCP mainProp, Object... params) {
        return addJProp(group, genSID(), caption, mainProp, params);
    }

    protected LCP addJProp(AbstractGroup group, String name, String caption, LCP mainProp, Object... params) {
        return addJProp(group, false, name, caption, mainProp, params);
    }

    protected LCP addJProp(AbstractGroup group, String name, boolean persistent, String caption, LCP mainProp, Object... params) {
        return addJProp(group, false, name, persistent, caption, mainProp, params);
    }

    protected LCP addJProp(AbstractGroup group, boolean implementChange, String name, String caption, LCP mainProp, Object... params) {
        return addJProp(group, implementChange, name, false, caption, mainProp, params);
    }

    protected LCP addJProp(AbstractGroup group, boolean implementChange, String name, boolean persistent, String caption, LCP mainProp, Object... params) {

        ImOrderSet<JoinProperty.Interface> listInterfaces = JoinProperty.getInterfaces(getIntNum(params));
        JoinProperty<?> property = new JoinProperty(name, caption, listInterfaces, implementChange,
                mapCalcImplement(mainProp, readCalcImplements(listInterfaces, params)));
        property.inheritFixedCharWidth(mainProp.property);
        property.inheritImage(mainProp.property);

        return addProperty(group, persistent, new LCP<JoinProperty.Interface>(property, listInterfaces));
    }

    // ------------------- mapLProp ----------------- //

    private <P extends PropertyInterface, L extends PropertyInterface> LCP mapLProp(AbstractGroup group, boolean persistent, CalcPropertyMapImplement<L, P> implement, ImOrderSet<P> listInterfaces) {
        return addProperty(group, persistent, new LCP<L>(implement.property, listInterfaces.mapOrder(implement.mapping.reverse())));
    }

    protected <P extends PropertyInterface, L extends PropertyInterface> LCP mapLProp(AbstractGroup group, boolean persistent, CalcPropertyMapImplement<L, P> implement, LCP<P> property) {
        return mapLProp(group, persistent, implement, property.listInterfaces);
    }

    private <P extends PropertyInterface, L extends PropertyInterface> LCP mapLGProp(AbstractGroup group, CalcPropertyImplement<L, CalcPropertyInterfaceImplement<P>> implement, ImList<CalcPropertyInterfaceImplement<P>> listImplements) {
        return mapLGProp(group, false, implement, listImplements);
    }

    private <P extends PropertyInterface, L extends PropertyInterface> LCP mapLGProp(AbstractGroup group, boolean persistent, CalcPropertyImplement<L, CalcPropertyInterfaceImplement<P>> implement, ImList<CalcPropertyInterfaceImplement<P>> listImplements) {
        return addProperty(group, persistent, new LCP<L>(implement.property, listImplements.toOrderExclSet().mapOrder(implement.mapping.toRevExclMap().reverse())));
    }

    private <P extends PropertyInterface> LCP mapLGProp(AbstractGroup group, boolean persistent, GroupProperty property, ImList<CalcPropertyInterfaceImplement<P>> listImplements) {
        return mapLGProp(group, persistent, new CalcPropertyImplement<GroupProperty.Interface<P>, CalcPropertyInterfaceImplement<P>>(property, property.getMapInterfaces()), listImplements);
    }

    // ------------------- Order property ----------------- //

    protected <P extends PropertyInterface> LCP addOProp(AbstractGroup group, String name, boolean persistent, String caption, PartitionType partitionType, boolean ascending, boolean ordersNotNull, boolean includeLast, int partNum, Object... params) {
        ImOrderSet<PropertyInterface> interfaces = genInterfaces(getIntNum(params));
        ImList<CalcPropertyInterfaceImplement<PropertyInterface>> listImplements = readCalcImplements(interfaces, params);

        ImList<CalcPropertyInterfaceImplement<PropertyInterface>> mainProp = listImplements.subList(0, 1);
        ImSet<CalcPropertyInterfaceImplement<PropertyInterface>> partitions = listImplements.subList(1, partNum + 1).toOrderSet().getSet();
        ImOrderMap<CalcPropertyInterfaceImplement<PropertyInterface>, Boolean> orders = listImplements.subList(partNum + 1, listImplements.size()).toOrderSet().toOrderMap(!ascending);

        return mapLProp(group, persistent, DerivedProperty.createOProp(name, caption, partitionType, interfaces.getSet(), mainProp, partitions, orders, ordersNotNull, includeLast), interfaces);
    }

    protected <P extends PropertyInterface> LCP addRProp(AbstractGroup group, String name, boolean persistent, String caption, Cycle cycle, ImList<Integer> resInterfaces, ImRevMap<Integer, Integer> mapPrev, Object... params) {
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

        boolean convertToLogical = false;
        assert initial.property.getType() instanceof IntegralClass == (step.property.getType() instanceof IntegralClass);
        if(!(initial.property.getType() instanceof IntegralClass) && (cycle == Cycle.NO || (cycle==Cycle.IMPOSSIBLE && persistent))) {
            CalcPropertyMapImplement<?, PropertyInterface> one = createStatic(1, LongClass.instance);
            initial = createAnd(innerInterfaces.getSet(), one, initial);
            step = createAnd(innerInterfaces.getSet(), one, step);
            convertToLogical = true;
        }

        RecursiveProperty<PropertyInterface> property = new RecursiveProperty<PropertyInterface>(name, caption, interfaces, cycle,
                mapInterfaces, mapIterate, initial, step);
        if(cycle==Cycle.NO)
            addConstraint(property.getConstrainedProperty(), false);

        LCP result = new LCP<RecursiveProperty.Interface>(property, interfaces);
//        if (convertToLogical)
//            return addJProp(group, name, false, caption, baseLM.notZero, directLI(addProperty(null, persistent, result)));
//        else
            return addProperty(group, persistent, result);
    }

    // ------------------- Ungroup property ----------------- //

    protected <L extends PropertyInterface> LCP addUGProp(AbstractGroup group, String name, boolean persistent, boolean over, String caption, int intCount, boolean ascending, boolean ordersNotNull, LCP<L> ungroup, Object... params) {
        int partNum = ungroup.listInterfaces.size();
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(intCount);
        final ImList<CalcPropertyInterfaceImplement<PropertyInterface>> listImplements = readCalcImplements(innerInterfaces, params);
        CalcPropertyInterfaceImplement<PropertyInterface> restriction = listImplements.get(0);
        ImMap<L, CalcPropertyInterfaceImplement<PropertyInterface>> groupImplement = ungroup.listInterfaces.mapOrderValues(new GetIndex<CalcPropertyInterfaceImplement<PropertyInterface>>() {
            public CalcPropertyInterfaceImplement<PropertyInterface> getMapValue(int i) {
                return listImplements.get(i+1);
            }});
        ImOrderMap<CalcPropertyInterfaceImplement<PropertyInterface>, Boolean> orders = listImplements.subList(partNum + 1, listImplements.size()).toOrderSet().toOrderMap(!ascending);

        return mapLProp(group, persistent, DerivedProperty.createUGProp(name, caption, innerInterfaces.getSet(),
                new CalcPropertyImplement<L, CalcPropertyInterfaceImplement<PropertyInterface>>((CalcProperty<L>) ungroup.property, groupImplement), orders, ordersNotNull, restriction, over), innerInterfaces);
    }

    protected <L extends PropertyInterface> LCP addPGProp(AbstractGroup group, String name, boolean persistent, int roundlen, boolean roundfirst, String caption, int intCount, boolean ascending, boolean ordersNotNull, LCP<L> ungroup, Object... params) {
        int partNum = ungroup.listInterfaces.size();
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(intCount);
        final ImList<CalcPropertyInterfaceImplement<PropertyInterface>> listImplements = readCalcImplements(innerInterfaces, params);
        CalcPropertyInterfaceImplement<PropertyInterface> proportion = listImplements.get(0);
        ImMap<L, CalcPropertyInterfaceImplement<PropertyInterface>> groupImplement = ungroup.listInterfaces.mapOrderValues(new GetIndex<CalcPropertyInterfaceImplement<PropertyInterface>>() {
            public CalcPropertyInterfaceImplement<PropertyInterface> getMapValue(int i) {
                return listImplements.get(i+1);
            }});
        ImOrderMap<CalcPropertyInterfaceImplement<PropertyInterface>, Boolean> orders =
                listImplements.subList(partNum + 1, listImplements.size()).toOrderSet().toOrderMap(!ascending);

        return mapLProp(group, persistent, DerivedProperty.createPGProp(name, caption, roundlen, roundfirst, baseLM.baseClass, innerInterfaces.getSet(),
                new CalcPropertyImplement<L, CalcPropertyInterfaceImplement<PropertyInterface>>((CalcProperty<L>) ungroup.property, groupImplement), proportion, orders, ordersNotNull), innerInterfaces);
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

    protected LCP addSGProp(AbstractGroup group, String name, boolean persistent, boolean notZero, String caption, int interfaces, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(interfaces);
        return addSGProp(group, name, persistent, notZero, caption, innerInterfaces, readCalcImplements(innerInterfaces, params));
    }

    protected <T extends PropertyInterface> LCP addSGProp(AbstractGroup group, String name, boolean persistent, boolean notZero, String caption, ImOrderSet<T> innerInterfaces, ImList<CalcPropertyInterfaceImplement<T>> implement) {
        ImList<CalcPropertyInterfaceImplement<T>> listImplements = implement.subList(1, implement.size());
        SumGroupProperty<T> property = new SumGroupProperty<T>(name, caption, innerInterfaces.getSet(), listImplements.getCol(), implement.get(0));

        LCP lp = mapLGProp(group, persistent, property, listImplements);
        lp.listGroupInterfaces = innerInterfaces;
        return lp;
    }

    // ------------------- Override property ----------------- //

    public <T extends PropertyInterface> LCP addOGProp(AbstractGroup group, String name, boolean persist, String caption, GroupType type, int numOrders, boolean ordersNotNull, boolean descending, int interfaces, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(interfaces);
        return addOGProp(group, name, persist, caption, type, numOrders, ordersNotNull, descending, innerInterfaces, readCalcImplements(innerInterfaces, params));
    }
    public <T extends PropertyInterface> LCP addOGProp(AbstractGroup group, String name, boolean persist, String caption, GroupType type, int numOrders, boolean ordersNotNull, boolean descending, ImOrderSet<T> innerInterfaces, ImList<CalcPropertyInterfaceImplement<T>> listImplements) {
        int numExprs = type.numExprs();
        ImList<CalcPropertyInterfaceImplement<T>> props = listImplements.subList(0, numExprs);
        ImOrderMap<CalcPropertyInterfaceImplement<T>, Boolean> orders = listImplements.subList(numExprs, numExprs + numOrders).toOrderSet().toOrderMap(descending);
        ImList<CalcPropertyInterfaceImplement<T>> groups = listImplements.subList(numExprs + numOrders, listImplements.size());
        OrderGroupProperty<T> property = new OrderGroupProperty<T>(name, caption, innerInterfaces.getSet(), groups.getCol(), props, type, orders, ordersNotNull);

        return mapLGProp(group, persist, property, groups);
    }

    // ------------------- GROUP MAX ----------------- //

    protected LCP addMGProp(AbstractGroup group, String name, boolean persist, String caption, boolean min, int interfaces, Object... params) {
        return addMGProp(group, persist, new String[]{name}, new String[]{caption}, 1, min, interfaces, params)[0];
    }

    protected LCP[] addMGProp(AbstractGroup group, boolean persist, String[] names, String[] captions, int exprs, boolean min, int interfaces, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(interfaces);
        return addMGProp(group, persist, names, captions, exprs, min, innerInterfaces, readCalcImplements(innerInterfaces, params));
    }

    protected <T extends PropertyInterface> LCP[] addMGProp(AbstractGroup group, boolean persist, String[] names, String[] captions, int exprs, boolean min, ImOrderSet<T> listInterfaces, ImList<CalcPropertyInterfaceImplement<T>> listImplements) {
        LCP[] result = new LCP[exprs];

        MSet<CalcProperty> mOverridePersist = SetFact.mSet();

        ImList<CalcPropertyInterfaceImplement<T>> groupImplements = listImplements.subList(exprs, listImplements.size());
        ImList<CalcPropertyImplement<?, CalcPropertyInterfaceImplement<T>>> mgProps = DerivedProperty.createMGProp(names, captions, listInterfaces.getSet(), baseLM.baseClass,
                listImplements.subList(0, exprs), groupImplements.getCol(), mOverridePersist, min);

        ImSet<CalcProperty> overridePersist = mOverridePersist.immutable();

        for (int i = 0; i < mgProps.size(); i++)
            result[i] = mapLGProp(group, mgProps.get(i), groupImplements);

        if (persist) {
            if (overridePersist.size() > 0) {
                for (CalcProperty property : overridePersist)
                    addProperty(null, true, new LCP(property));
            } else
                for (int i = 0; i < result.length; i++)
                    addPersistent(result[i]);
        }

        return result;
    }

    // ------------------- CGProperty ----------------- //

    protected <T extends PropertyInterface, P extends PropertyInterface> LCP addCGProp(AbstractGroup group, boolean checkChange, String name, boolean persistent, String caption, LCP<P> dataProp, int interfaces, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(interfaces);
        return addCGProp(group, checkChange, name, persistent, caption, dataProp, innerInterfaces, readCalcImplements(innerInterfaces, params));
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LCP addCGProp(AbstractGroup group, boolean checkChange, String name, boolean persistent, String caption, LCP<P> dataProp, ImOrderSet<T> innerInterfaces, ImList<CalcPropertyInterfaceImplement<T>> listImplements) {
        CycleGroupProperty<T, P> property = new CycleGroupProperty<T, P>(name, caption, innerInterfaces.getSet(), listImplements.subList(1, listImplements.size()).getCol(), listImplements.get(0), dataProp == null ? null : (CalcProperty<P>)dataProp.property);

        // нужно добавить ограничение на уникальность
        addConstraint(property.getConstrainedProperty(), checkChange);

        return mapLGProp(group, persistent, property, listImplements.subList(1, listImplements.size()));
    }

//    protected static <T extends PropertyInterface<T>> AggregateGroupProperty create(String sID, String caption, CalcProperty<T> property, T aggrInterface, Collection<CalcPropertyMapImplement<?, T>> groupProps) {

    // ------------------- GROUP AGGR ----------------- //

    protected LCP addAGProp(AbstractGroup group, boolean checkChange, String name, boolean persistent, String caption, boolean noConstraint, int interfaces, Object... props) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(interfaces);
        return addAGProp(group, checkChange, name, persistent, caption, noConstraint, innerInterfaces, readCalcImplements(innerInterfaces, props));
    }

    protected <T extends PropertyInterface<T>, I extends PropertyInterface> LCP addAGProp(AbstractGroup group, boolean checkChange, String name, boolean persistent, String caption, boolean noConstraint, ImOrderSet<T> innerInterfaces, ImList<CalcPropertyInterfaceImplement<T>> listImplements) {
        T aggrInterface = (T) listImplements.get(0);
        CalcPropertyInterfaceImplement<T> whereProp = listImplements.get(1);
        ImList<CalcPropertyInterfaceImplement<T>> groupImplements = listImplements.subList(2, listImplements.size());

        AggregateGroupProperty<T> aggProp = AggregateGroupProperty.create(name, caption, innerInterfaces.getSet(), whereProp, aggrInterface, groupImplements.toOrderExclSet().getSet());
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

    protected LCP addUProp(AbstractGroup group, String caption, Union unionType, String separator, int[] coeffs, Object... params) {
        return addUProp(group, genSID(), false, caption, unionType, null, coeffs, params);
    }

    protected LCP addUProp(AbstractGroup group, String name, boolean persistent, String caption, Union unionType, String separator, int[] coeffs, Object... params) {

        assert (unionType==Union.SUM)==(coeffs!=null);
        assert (unionType==Union.STRING_AGG)==(separator !=null);

        int intNum = getIntNum(params);
        ImOrderSet<UnionProperty.Interface> listInterfaces = UnionProperty.getInterfaces(intNum);
        ImList<CalcPropertyInterfaceImplement<UnionProperty.Interface>> listOperands = readCalcImplements(listInterfaces, params);

        UnionProperty property = null;
        switch (unionType) {
            case MAX:
            case MIN:
                property = new MaxUnionProperty(unionType == Union.MIN, name, caption, listInterfaces, listOperands.getCol());
                break;
            case SUM:
                MMap<CalcPropertyInterfaceImplement<UnionProperty.Interface>, Integer> mMapOperands = MapFact.mMap(MapFact.<CalcPropertyInterfaceImplement<UnionProperty.Interface>>addLinear());
                for(int i=0;i<listOperands.size();i++)
                    mMapOperands.add(listOperands.get(i), coeffs[i]);
                property = new SumUnionProperty(name, caption, listInterfaces, mMapOperands.immutable());
                break;
            case OVERRIDE:
                property = new CaseUnionProperty(name, caption, listInterfaces, listOperands, false, true);
                break;
            case XOR:
                property = new XorUnionProperty(name, caption, listInterfaces, listOperands);
                break;
            case EXCLUSIVE:
                property = new CaseUnionProperty(name, caption, listInterfaces, listOperands.getCol(), false);
                break;
            case CLASS:
                property = new CaseUnionProperty(name, caption, listInterfaces, listOperands.getCol(), true);
                break;
            case CLASSOVERRIDE:
                property = new CaseUnionProperty(name, caption, listInterfaces, listOperands, true, false);
                break;
            case STRING_AGG:
                property = new StringAggUnionProperty(name, caption, listInterfaces, listOperands, separator);
                break;
        }

        return addProperty(group, persistent, new LCP<UnionProperty.Interface>(property, listInterfaces));
    }

    protected LCP addAUProp(AbstractGroup group, String name, boolean persistent, boolean isExclusive, boolean isChecked, CaseUnionProperty.Type type, String caption, ValueClass valueClass, ValueClass... interfaces) {
        ImOrderSet<UnionProperty.Interface> listInterfaces = UnionProperty.getInterfaces(interfaces.length);
        return addProperty(group, persistent, new LCP<UnionProperty.Interface>(
                new CaseUnionProperty(name, isExclusive, isChecked, type, caption, listInterfaces, valueClass, listInterfaces.mapList(ListFact.toList(interfaces))), listInterfaces));
    }

    protected LCP addCaseUProp(AbstractGroup group, String name, boolean persistent, String caption, Object... params) {
        return addCaseUProp(group, name, persistent, caption, false, params);
    }

    protected LCP addCaseUProp(AbstractGroup group, String name, boolean persistent, String caption, boolean isExclusive, Object... params) {
        ImOrderSet<UnionProperty.Interface> listInterfaces = UnionProperty.getInterfaces(getIntNum(params));
        MList<CaseUnionProperty.Case> mListCases = ListFact.mList();
        ImList<CalcPropertyMapImplement<?,UnionProperty.Interface>> mapImplements = (ImList<CalcPropertyMapImplement<?, UnionProperty.Interface>>) (ImList<?>) readCalcImplements(listInterfaces, params);
        for (int i = 0; i < mapImplements.size() / 2; i++)
            mListCases.add(new CaseUnionProperty.Case(mapImplements.get(2 * i), mapImplements.get(2 * i + 1)));
        if (mapImplements.size() % 2 != 0)
            mListCases.add(new CaseUnionProperty.Case(new CalcPropertyMapImplement<PropertyInterface, UnionProperty.Interface>((CalcProperty<PropertyInterface>) baseLM.vtrue.property), mapImplements.get(mapImplements.size() - 1)));

        return addProperty(group, persistent, new LCP<UnionProperty.Interface>(new CaseUnionProperty(name, caption, listInterfaces, isExclusive, mListCases.immutableList()), listInterfaces));
    }

    // ------------------- Loggable ----------------- //

    public LCP addLProp(SystemEventsLogicsModule systemEventsLM, LCP lp, ValueClass... classes) {
        return addDCProp("LG_" + lp.property.getSID(), ServerResourceBundle.getString("logics.log") + " " + lp.property, 1, lp,
                add(new Object[]{true}, add(getParams(lp), add(new Object[]{addJProp(baseLM.equals2, 1, systemEventsLM.currentSession), lp.listInterfaces.size() + 1}, add(directLI(lp), classes)))));
    }

    // ------------------- UNION SUM ----------------- //

    protected LCP addSUProp(String name, boolean persistent, String caption, Union unionType, LCP... props) {
        return addSUProp(null, name, persistent, caption, unionType, props);
    }

    protected LCP addSUProp(AbstractGroup group, String name, boolean persistent, String caption, Union unionType, LCP... props) {
        return addUProp(group, name, persistent, caption, unionType, null, (unionType == Union.SUM ? BaseUtils.genArray(1, props.length) : null), getUParams(props));
    }

    // ------------------- CONCAT ----------------- //

    protected LCP addSFUProp(int intNum, String separator) {
        return addSFUProp(genSID(), separator, intNum);
    }

    protected LCP addSFUProp(String name, String separator, int intNum) {
        return addUProp(null, name, false, ServerResourceBundle.getString("logics.join"), Union.STRING_AGG, separator, null, getUParams(intNum));
    }

    // ------------------- ACTION ----------------- //

    public LAP addAProp(ActionProperty property) {
        return addAProp(baseLM.privateGroup, property);
    }

    public LAP addAProp(AbstractGroup group, ActionProperty property) {
        return addProperty(group, new LAP(property));
    }

    // ------------------- MESSAGE ----------------- //

    protected LAP addMAProp(String title, Object... params) {
        return addMAProp(null, "", title, params);
    }

    protected LAP addMAProp(AbstractGroup group, String caption, String title, Object... params) {
        return addJoinAProp(group, genSID(), caption, addMAProp(title), params);
    }

    @IdentityInstanceLazy
    protected LAP addMAProp(String title) {
        return addProperty(null, new LAP(new MessageActionProperty(genSID(), "Message", title)));
    }

    // ------------------- CONFIRM ----------------- //

    protected LAP addConfirmAProp(String title, Object... params) {
        return addConfirmAProp(null, "", title, params);
    }

    protected LAP addConfirmAProp(AbstractGroup group, String caption, String title, Object... params) {
        return addJoinAProp(group, genSID(), caption, addConfirmAProp(title), params);
    }

    @IdentityInstanceLazy
    protected LAP addConfirmAProp(String title) {
        return addProperty(null, new LAP(new ConfirmActionProperty(genSID(), "Confirm", title, getConfirmedProperty())));
    }

    // ------------------- Async Update Action ----------------- //

    protected LAP addAsyncUpdateAProp(Object... params) {
        return addAsyncUpdateAProp("", params);
    }

    protected LAP addAsyncUpdateAProp(String caption, Object... params) {
        return addAsyncUpdateAProp(null, caption, params);
    }

    protected LAP addAsyncUpdateAProp(AbstractGroup group, String caption, Object... params) {
        return addJoinAProp(group, genSID(), caption, addAsyncUpdateAProp(), params);
    }

    @IdentityInstanceLazy
    protected LAP addAsyncUpdateAProp() {
        return addProperty(null, new LAP(new AsyncUpdateEditValueActionProperty(genSID(), "Async Update")));
    }

    // ------------------- LOAD FILE ----------------- //

    protected LAP addLFAProp(LCP lp) {
        return addLFAProp(null, "lfa", lp);
    }

    protected LAP addLFAProp(AbstractGroup group, String caption, LCP lp) {
        return addProperty(group, new LAP(new LoadActionProperty(genSID(), caption, lp)));
    }

    // ------------------- OPEN FILE ----------------- //

    protected LAP addOFAProp(LCP lp) {
        return addOFAProp(null, "ofa", lp);
    }

    protected LAP addOFAProp(AbstractGroup group, String caption, LCP lp) { // обернем сразу в and
        return addProperty(group, new LAP(new OpenActionProperty(genSID(), caption, lp)));
    }

    // ------------------- EVAL ----------------- //

    public LAP addEvalAProp(LCP<?> scriptSource) {
        return addAProp(null, new EvalActionProperty(genSID(), "", scriptSource));
    }

    public SessionDataProperty getAddedObjectProperty() {
        return baseLM.getAddedObjectProperty();
    }

    public LCP getConfirmedProperty() {
        return baseLM.getConfirmedProperty();
    }

    public AnyValuePropertyHolder getChosenValueProperty() {
        return baseLM.getChosenValueProperty();
    }

    public AnyValuePropertyHolder getRequestedValueProperty() {
        return baseLM.getRequestedValueProperty();
    }

    public LCP getRequestCanceledProperty() {
        return baseLM.getRequestCanceledProperty();
    }

    public LCP getFormResultProperty() {
        return baseLM.getFormResultProperty();
    }

    public AnyValuePropertyHolder addAnyValuePropertyHolder(String sidPrefix, String captionPrefix, ValueClass... classes) {
        return new AnyValuePropertyHolder(
                getLCPByOldName(sidPrefix + "Object"),
                getLCPByOldName(sidPrefix + "String"),
                getLCPByOldName(sidPrefix + "Integer"),
                getLCPByOldName(sidPrefix + "Long"),
                getLCPByOldName(sidPrefix + "Double"),
                getLCPByOldName(sidPrefix + "Numeric"),
                getLCPByOldName(sidPrefix + "Year"),
                getLCPByOldName(sidPrefix + "DateTime"),
                getLCPByOldName(sidPrefix + "Logical"),
                getLCPByOldName(sidPrefix + "Date"),           
                getLCPByOldName(sidPrefix + "Time"),
                getLCPByOldName(sidPrefix + "Color"),
                getLCPByOldName(sidPrefix + "WordFile"),
                getLCPByOldName(sidPrefix + "ImageFile"),
                getLCPByOldName(sidPrefix + "PdfFile"),
                getLCPByOldName(sidPrefix + "CustomFile"),
                getLCPByOldName(sidPrefix + "ExcelFile")
        );
    }

    // ---------------------- Add Object ---------------------- //

    public <T extends PropertyInterface, I extends PropertyInterface> LAP getScriptAddObjectAction(CustomClass cls, boolean forceDialog, int resInterfaces, boolean conditional, boolean resultExists, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(getIntNum(params));
        ImList<CalcPropertyInterfaceImplement<PropertyInterface>> readImplements = readCalcImplements(innerInterfaces, params);
        CalcPropertyMapImplement<T, PropertyInterface> resultPart = (CalcPropertyMapImplement<T, PropertyInterface>)
                (resultExists ? readImplements.get(resInterfaces) : null);
        CalcPropertyMapImplement<T, PropertyInterface> conditionalPart = (CalcPropertyMapImplement<T, PropertyInterface>)
                (conditional ? readImplements.get(resInterfaces + (resultExists ? 1 : 0)) : DerivedProperty.createTrue());

        return addAProp(null, new AddObjectActionProperty(genSID(), cls, forceDialog, innerInterfaces.getSet(), (ImOrderSet) readImplements.subList(0, resInterfaces).toOrderExclSet(), conditionalPart, resultPart, MapFact.<CalcPropertyInterfaceImplement<I>, Boolean>EMPTYORDER(), false));
    }

    public LAP getAddObjectAction(FormEntity formEntity, ObjectEntity obj) {
        return getAddObjectAction((CustomClass) obj.baseClass, formEntity, obj);
    }

    public LAP getAddObjectAction(CustomClass cls, FormEntity formEntity, ObjectEntity obj) {
        return baseLM.getAddObjectAction(cls, formEntity, obj);
    }

    // ---------------------- Delete Object ---------------------- //

    public LAP getDeleteAction(CustomClass cls, boolean oldSession) {
        return baseLM.getDeleteAction(cls, oldSession);
    }

    protected void setDeleteActionOptions(LAP property) {
        property.setImage("delete.png");
        property.setShouldBeLast(true);
        property.setEditKey(KeyStrokes.getDeleteActionPropertyKeyStroke());
        property.setShowEditKey(false);
    }

    // ---------------------- Add Form ---------------------- //

    public LAP getScriptAddFormAction(CustomClass cls, boolean session) {
        ClassFormEntity form = cls.getEditForm(baseLM);

        LAP property = addDMFAProp(null, genSID(), ServerResourceBundle.getString("logics.add"),
                form.form, new ObjectEntity[] {},
                form.form.addPropertyObject(getAddObjectAction(cls, form.form, form.object)), !session);
        setAddFormActionProperties(property, form, session);
        return property;
    }

    public LAP getAddFormAction(CustomClass cls, boolean oldSession) {
        return baseLM.getAddFormAction(cls, oldSession);
    }

    protected void setAddFormActionProperties(LAP property, ClassFormEntity form, boolean oldSession) {
        property.setImage("add.png");
        property.setShouldBeLast(true);
        property.setEditKey(KeyStrokes.getAddActionPropertyKeyStroke());
        property.setShowEditKey(false);
        property.setDrawToToolbar(true);
        property.setForceViewType(ClassViewType.PANEL);

        // todo : так не очень правильно делать - получается, что мы добавляем к Immutable объекту FormActionProperty ссылки на ObjectEntity
        FormActionProperty formAction = (FormActionProperty)property.property;
        formAction.seekOnOk.add(form.object);
        if (oldSession) {
            formAction.closeAction = form.form.addPropertyObject(getDeleteAction((CustomClass)form.object.baseClass, true), form.object);
        }
    }

    // ---------------------- Edit Form ---------------------- //

    public LAP getScriptEditFormAction(CustomClass cls, boolean oldSession) {
        ClassFormEntity form = cls.getEditForm(baseLM);
        LAP property = addDMFAProp(null, genSID(), ServerResourceBundle.getString("logics.edit"), form.form, new ObjectEntity[]{form.object}, !oldSession);
        setEditFormActionProperties(property);
        return property;
    }

    public LAP getEditFormAction(CustomClass cls, boolean oldSession) {
        return baseLM.getEditFormAction(cls, oldSession);
    }

    protected void setEditFormActionProperties(LAP property) {
        property.setImage("edit.png");
        property.setShouldBeLast(true);
        property.setEditKey(KeyStrokes.getEditActionPropertyKeyStroke());
        property.setShowEditKey(false);
        property.setDrawToToolbar(true);
        property.setForceViewType(ClassViewType.PANEL);
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

    protected <T extends LP<?, ?>> T addProperty(AbstractGroup group, T lp) {
        return addProperty(group, false, lp);
    }

    protected void addPropertyToGroup(Property<?> property, AbstractGroup group) {
        if (group != null) {
            group.add(property);
        } else {
            baseLM.privateGroup.add(property);
        }
    }

    protected <T extends LP<?, ?>> T addProperty(AbstractGroup group, boolean persistent, T lp) {
        setPropertySID(lp, lp.property.getSID(), lp.property.getOldName(), true);
        if (group != null && group != baseLM.privateGroup || persistent) {
            lp.property.freezeSID();
        }
        addModuleLP(lp);
        baseLM.registerProperty(lp);
        addPropertyToGroup(lp.property, group);

        if (persistent) {
            addPersistent((LCP)lp);
        }
        return lp;
    }

    protected <T extends LP<?, ?>> void setPropertySID(T lp, String name, String oldName, boolean generated) {
        String oldSID = lp.property.getSID();
        lp.property.setName(name, generated);
        String newSID = baseLM.getSIDPolicy().createSID(getNamePrefix(), name, null, oldName);
        if (baseLM.idSet.contains(oldSID)) {
            baseLM.idSet.remove(oldSID);
            if (generated)
                baseLM.idSet.add(newSID);
        }
        lp.property.setSID(newSID);
    }

    public void addIndex(LCP<?>... lps) {
        ThreadLocalContext.getDbManager().addIndex(lps);
    }

    protected void addPersistent(LCP lp) {
        addPersistent((AggregateProperty) lp.property, null);
    }

    protected void addPersistent(LCP lp, ImplementTable table) {
        addPersistent((AggregateProperty) lp.property, table);
    }

    private void addPersistent(AggregateProperty property, ImplementTable table) {
        assert !baseLM.isGeneratedSID(property.getSID());

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
        return addProperty(null, new LCP<ClassPropertyInterface>(filterProperty.property, groupObject.getOrderObjects().mapOrder(filterProperty.mapping.reverse())));
    }

    protected LAP addOSAProp(FormEntity form, ObjectEntity object, Object... params) {
        return addOSAProp(null, "", form, object, params);
    }

    protected LAP addOSAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity object, Object... params) {
        return addJoinAProp(group, genSID(), caption, addOSAProp(form, object), params);
    }

    @IdentityStrongLazy // для ID
    public LAP addOSAProp(FormEntity form, ObjectEntity object) {
        SeekActionProperty seekProperty = new SeekActionProperty((ScriptingLogicsModule)this, form, object);
        return addProperty(null, new LAP<ClassPropertyInterface>(seekProperty));
    }

    public void addConstraint(CalcProperty property, boolean checkChange) {
        addConstraint(addProp(property), checkChange);
    }

    public void addConstraint(LCP<?> lp, boolean checkChange) {
        addConstraint(lp, (checkChange ? CalcProperty.CheckType.CHECK_ALL : CalcProperty.CheckType.CHECK_NO), null, Event.APPLY, this);
    }

    protected void addConstraint(LCP<?> lp, CalcProperty.CheckType type, ImSet<CalcProperty<?>> checkProps, Event event, LogicsModule lm) {
        if(!((CalcProperty)lp.property).noDB())
            lp = addCHProp(lp, IncrementType.SET, event.getScope());
        // assert что lp уже в списке properties
        setConstraint((CalcProperty) lp.property, type, event, checkProps);
    }

    public <T extends PropertyInterface> void setConstraint(CalcProperty property, CalcProperty.CheckType type, Event event, ImSet<CalcProperty<?>> checkProperties) {
        assert type != CalcProperty.CheckType.CHECK_SOME || checkProperties != null;
        assert property.noDB();

        property.checkChange = type;
        property.checkProperties = checkProperties;

        ActionPropertyMapImplement<?, ClassPropertyInterface> constraintAction =
                DerivedProperty.createListAction(
                        SetFact.<ClassPropertyInterface>EMPTY(),
                        ListFact.<ActionPropertyMapImplement<?, ClassPropertyInterface>>toList(
                                new LogPropertyActionProperty<T>(property, recognizeGroup).getImplement(),
                                baseLM.cancel.property.getImplement(SetFact.<ClassPropertyInterface>EMPTYORDER())
                        )
                );
        constraintAction.mapEventAction(this, DerivedProperty.createAnyGProp(property).getImplement(), event, true);
        addProp(constraintAction.property);
    }

    public <T extends PropertyInterface> void addEventAction(Event event, boolean descending, boolean ordersNotNull, int noInline, boolean forceInline, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(getIntNum(params));

        ImList<PropertyInterfaceImplement<PropertyInterface>> listImplements = readImplements(innerInterfaces, params);
        int implCnt = listImplements.size();

        ImOrderMap<CalcPropertyInterfaceImplement<PropertyInterface>, Boolean> orders = BaseUtils.immutableCast(listImplements.subList(2, implCnt - noInline).toOrderSet().toOrderMap(descending));

        ImSet<PropertyInterface> noInlineInterfaces = BaseUtils.<ImList<PropertyInterface>>immutableCast(listImplements.subList(implCnt - noInline, implCnt)).toOrderExclSet().getSet();

        addEventAction(innerInterfaces.getSet(), (ActionPropertyMapImplement<?, PropertyInterface>) listImplements.get(0), (CalcPropertyMapImplement<?, PropertyInterface>) listImplements.get(1), orders, ordersNotNull, event, noInlineInterfaces, forceInline, false);
    }

    public <P extends PropertyInterface, D extends PropertyInterface> void addEventAction(ActionProperty<P> actionProperty, CalcPropertyMapImplement<?, P> whereImplement, ImOrderMap<CalcPropertyInterfaceImplement<P>, Boolean> orders, boolean ordersNotNull, Event event, boolean resolve) {
        addEventAction(actionProperty.interfaces, actionProperty.getImplement(), whereImplement, orders, ordersNotNull, event, SetFact.<P>EMPTY(), false, resolve);
    }

    public <P extends PropertyInterface, D extends PropertyInterface> void addEventAction(ImSet<P> innerInterfaces, ActionPropertyMapImplement<?, P> actionProperty, CalcPropertyMapImplement<?, P> whereImplement, ImOrderMap<CalcPropertyInterfaceImplement<P>, Boolean> orders, boolean ordersNotNull, Event event, ImSet<P> noInline, boolean forceInline, boolean resolve) {
        if(!((CalcProperty)whereImplement.property).noDB())
            whereImplement = whereImplement.mapChanged(IncrementType.SET, event.getScope());

        ActionProperty<? extends PropertyInterface> action =
                innerInterfaces.isEmpty() ?
                    DerivedProperty.createIfAction(innerInterfaces, whereImplement, actionProperty, null).property :
                    DerivedProperty.createForAction(innerInterfaces, SetFact.<P>EMPTY(), whereImplement, orders, ordersNotNull, actionProperty, null, false, noInline, forceInline).property;

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
        follows(first, PropertyFollows.RESOLVE_ALL, Event.APPLY, second, mapping);
    }

    protected <L extends PropertyInterface, T extends PropertyInterface> void follows(final LCP<T> first, int options, Event event, LCP<L> second, final Integer... mapping) {
        addFollows(first.property, new CalcPropertyMapImplement<L, T>(second.property, second.getRevMap(first.listInterfaces, mapping)), options, event);
    }

    public <T extends PropertyInterface, L extends PropertyInterface> void setNotNull(CalcProperty<T> property, int options, Event event) {
        CalcPropertyMapImplement<L, T> mapClasses = (CalcPropertyMapImplement<L, T>) IsClassProperty.getMapProperty(property.getInterfaceClasses(ClassType.ASSERTFULL));
        addFollows(mapClasses.property, new CalcPropertyMapImplement<T, L>(property, mapClasses.mapping.reverse()),
                ServerResourceBundle.getString("logics.property") + " " + property.caption + " [" + property.getSID() + "] " + ServerResourceBundle.getString("logics.property.not.defined"),
                options, event);
    }

    public <T extends PropertyInterface, L extends PropertyInterface> void addFollows(CalcProperty<T> property, CalcPropertyMapImplement<L, T> implement, int options, Event event) {
        addFollows(property, implement, ServerResourceBundle.getString("logics.property.violated.consequence.from") + "(" + this + ") => (" + implement.property + ")", options, event);
    }

    public <T extends PropertyInterface, L extends PropertyInterface> void addFollows(CalcProperty<T> property, CalcPropertyMapImplement<L, T> implement, String caption, int options, Event event) {
//        PropertyFollows<T, L> propertyFollows = new PropertyFollows<T, L>(this, implement, options);

        if((options & PropertyFollows.RESOLVE_TRUE)!=0) { // оптимизационная проверка
            assert property.interfaces.size() == implement.mapping.size(); // assert что количество
            ActionPropertyMapImplement<?, T> setAction = implement.getSetNotNullAction(true);
            if(setAction!=null) {
//                setAction.property.caption = "RESOLVE TRUE : " + property + " => " + implement.property;
                setAction.mapEventAction(this, DerivedProperty.createAndNot(property.getChanged(IncrementType.SET, event.getScope()), implement), event, true);
            }
        }
        if((options & PropertyFollows.RESOLVE_FALSE)!=0) {
            ActionPropertyMapImplement<?, T> setAction = property.getSetNotNullAction(false);
            if(setAction!=null) {
//                setAction.property.caption = "RESOLVE FALSE : " + property + " => " + implement.property;
                setAction.mapEventAction(this, DerivedProperty.createAnd(property, implement.mapChanged(IncrementType.DROP, event.getScope())), event, true);
            }
        }

        CalcProperty constraint = DerivedProperty.createAndNot(property, implement).property;
        constraint.caption = caption;
        addConstraint(constraint, false);
    }

    protected void setNotNull(LCP property) {
        setNotNull(property, PropertyFollows.RESOLVE_TRUE);
    }

    protected <P extends PropertyInterface, C extends PropertyInterface> void setNotNull(LCP<P> lp, int resolve) {
        setNotNull(lp, Event.APPLY, resolve);
    }

    protected <P extends PropertyInterface, C extends PropertyInterface> void setNotNull(LCP<P> lp, Event event, int resolve) {
        setNotNull(lp.property, resolve, event);
    }

    public static <P extends PropertyInterface, T extends PropertyInterface> ActionPropertyMapImplement<P, T> mapActionListImplement(LAP<P> property, ImOrderSet<T> mapList) {
        return new ActionPropertyMapImplement<P, T>(property.property, getMapping(property, mapList));
    }
    public static <P extends PropertyInterface, T extends PropertyInterface> CalcPropertyMapImplement<P, T> mapCalcListImplement(LCP<P> property, ImOrderSet<T> mapList) {
        return new CalcPropertyMapImplement<P, T>(property.property, getMapping(property, mapList));
    }

    private static <P extends PropertyInterface, T extends PropertyInterface> ImRevMap<P, T> getMapping(LP<P, ?> property, final ImOrderSet<T> mapList) {
        return property.getRevMap(mapList);
    }

    protected void makeUserLoggable(SystemEventsLogicsModule systemEventsLM, LCP... lps) {
        for (LCP lp : lps)
            lp.makeUserLoggable(systemEventsLM);
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

    protected NavigatorElement addNavigatorElement(String name, String caption, String icon) {
        return addNavigatorElement(null, name, caption, icon);
    }

    protected NavigatorElement addNavigatorElement(NavigatorElement parent, String name, String caption) {
        return addNavigatorElement(parent, name, caption, null);
    }

    protected NavigatorElement addNavigatorElement(NavigatorElement parent, String name, String caption, String icon) {
        NavigatorElement elem = new NavigatorElement(parent, transformNameToSID(name), caption, icon);
        addModuleNavigator(elem);
        return elem;
    }

    protected NavigatorAction addNavigatorAction(String name, String caption, LAP property, String icon) {
        return addNavigatorAction(null, name, caption, property, icon);
    }

    protected NavigatorAction addNavigatorAction(NavigatorElement parent, String name, String caption, LAP property, String icon) {
        return addNavigatorAction(parent, name, caption, (ActionProperty) property.property, icon);
    }

    protected NavigatorAction addNavigatorAction(NavigatorElement parent, String name, String caption, ActionProperty property, String icon) {
        NavigatorAction navigatorAction = new NavigatorAction(parent, transformNameToSID(name), caption, icon);
        navigatorAction.setProperty(property);
        addModuleNavigator(navigatorAction);
        return navigatorAction;
    }

    public NavigatorElement getNavigatorElementBySID(String sid) {
        return moduleNavigators.get(sid);
    }

    public NavigatorElement getNavigatorElementByName(String name) {
        return getNavigatorElementBySID(transformNameToSID(name));
    }

    public <T extends FormEntity> T addFormEntity(T form) {
        form.setSID(transformNameToSID(form.getSID()));
        addModuleNavigator(form);
        return form;
    }

    protected void addModuleNavigator(NavigatorElement<?> element) {
        assert !moduleNavigators.containsKey(element.getSID());
        moduleNavigators.put(element.getSID(), element);
    }

    public void addObjectActions(FormEntity form, ObjectEntity object) {
        addObjectActions(form, object, true);
    }

    protected void addObjectActions(FormEntity form, ObjectEntity object, boolean shouldBeLast) {
        PropertyDrawEntity actionAddPropertyDraw;
        actionAddPropertyDraw = form.addPropertyDraw(getAddObjectAction(form, object));
        actionAddPropertyDraw.shouldBeLast = shouldBeLast;
        actionAddPropertyDraw.toDraw = object.groupTo;

        form.addPropertyDraw(getDeleteAction((CustomClass)object.baseClass, true), object).shouldBeLast = shouldBeLast;
    }

    public void addFormActions(FormEntity form, ObjectEntity object) {
        addFormActions(form, object, false);
    }

    public void addFormActions(FormEntity form, ObjectEntity object, boolean session) {
        addAddFormAction(form, object, session);
        addEditFormAction(form, object, session);
        form.addPropertyDraw(getDeleteAction((CustomClass) object.baseClass, false), object);
    }

    public PropertyDrawEntity addAddFormAction(FormEntity form, ObjectEntity object, boolean session) {
        LAP addForm = getAddFormAction((CustomClass)object.baseClass, session);
        PropertyDrawEntity actionAddPropertyDraw = form.addPropertyDraw(addForm);
        actionAddPropertyDraw.toDraw = object.groupTo;

        return actionAddPropertyDraw;
    }

    public PropertyDrawEntity addEditFormAction(FormEntity form, ObjectEntity object, boolean session) {
        return form.addPropertyDraw(getEditFormAction((CustomClass)object.baseClass, session), object);
    }

    public PropertyDrawEntity addFormDeleteAction(FormEntity form, ObjectEntity object, boolean oldSession) {
        return form.addPropertyDraw(getDeleteAction((CustomClass) object.baseClass, oldSession), object);
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Set<String> getRequiredModules() {
        return requiredModules;
    }

    public void setRequiredModules(Set<String> requiredModules) {
        this.requiredModules = requiredModules;
    }

    public LPEqualNameModuleFinder getEqualLPModuleFinder() {
        return new LPEqualNameModuleFinder();        
    }
    
    public interface ModuleFinder<T, P> {
        public List<T> resolveInModule(LogicsModule module, String simpleName, P param);
    }

    public List<AndClassSet> getParamClasses(LP<?, ?> lp) {
        List<AndClassSet> paramClasses = propClasses.get(lp);
        return paramClasses == null ? Collections.<AndClassSet>nCopies(lp.listInterfaces.size(), null) : paramClasses;                   
    }

    public static class OldLPNameModuleFinder implements ModuleFinder<LP<?, ?>, List<AndClassSet>> {
        @Override
        public List<LP<?, ?>> resolveInModule(LogicsModule module, String simpleName, List<AndClassSet> classes)  {
            List<LP<?, ?>> result = new ArrayList<LP<?, ?>>();
            for (List<LP<?, ?>> lps : module.getNamedModuleProperties().values()) {
                for (LP<?, ?> lp : lps) {
                    String actualName = BaseUtils.nvl(lp.property.getOldName(), lp.property.getName());
                    if (simpleName.equals(actualName)) {
                        result.add(lp);
                    }
                }
            }
            return result;
        }
    }

    public static class SoftLPNameModuleFinder implements ModuleFinder<LP<?, ?>, List<AndClassSet>> {
        @Override
        public List<LP<?, ?>> resolveInModule(LogicsModule module, String simpleName, List<AndClassSet> classes)  {
            List<LP<?, ?>> result = new ArrayList<LP<?, ?>>();
            for (LP<?, ?> lp : module.getAllLPByName(simpleName)) {
                if (softMatch(module.getParamClasses(lp), classes)) {
                    result.add(lp);
                }
            }
            return result;
        }
    }                           
    
    public static class LPNameModuleFinder implements ModuleFinder<LP<?, ?>, List<AndClassSet>> {
        @Override
        public List<LP<?, ?>> resolveInModule(LogicsModule module, String simpleName, List<AndClassSet> classes)  {
            List<LP<?, ?>> result = new ArrayList<LP<?, ?>>();
            for (LP<?, ?> lp : module.getAllLPByName(simpleName)) {
                if (match(module.getParamClasses(lp), classes, false)) {
                    result.add(lp);
                }
            }
            return result;
        }
    }

    public static class LPEqualNameModuleFinder implements ModuleFinder<LP<?, ?>, List<AndClassSet>> {
        @Override
        public List<LP<?, ?>> resolveInModule(LogicsModule module, String simpleName, List<AndClassSet> classes)  {
            List<LP<?, ?>> result = new ArrayList<LP<?, ?>>();
            for (LP<?, ?> lp : module.getAllLPByName(simpleName)) {
                if (match(module.getParamClasses(lp), classes, false) && match(classes, module.getParamClasses(lp), false)) {
                    result.add(lp);
                }
            }
            return result;
        }
    }
    
    public static boolean match(List<AndClassSet> interfaceClasses, List<AndClassSet> paramClasses, boolean strict) {
        assert interfaceClasses != null;
        if (paramClasses == null) {
            return true;
        }
        if (interfaceClasses.size() != paramClasses.size()) {
            return false;
        }
        
        for (int i = 0; i < interfaceClasses.size(); i++) {
            if (interfaceClasses.get(i) != null && paramClasses.get(i) != null && !interfaceClasses.get(i).containsAll(paramClasses.get(i), !strict)) {
                return false;
            }
        }
        return true;
    }

    public static boolean softMatch(List<AndClassSet> interfaceClasses, List<AndClassSet> paramClasses) {
        assert interfaceClasses != null;
        if (paramClasses == null) {
            return true;
        }
        if (interfaceClasses.size() != paramClasses.size()) {
            return false;
        }

        for (int i = 0; i < interfaceClasses.size(); i++) {
            if (interfaceClasses.get(i) != null && paramClasses.get(i) != null && (interfaceClasses.get(i).and(paramClasses.get(i))).isEmpty()) {
                return false;
            }
        }
        return true;
    }
    
    public static class GroupNameModuleFinder implements ModuleFinder<AbstractGroup, Object> {
        @Override
        public List<AbstractGroup> resolveInModule(LogicsModule module, String simpleName, Object param) {
            AbstractGroup group = module.getGroupByName(simpleName); 
            return group == null ? new ArrayList<AbstractGroup>() : Collections.singletonList(group);
        }
    }

    public static class NavigatorElementNameModuleFinder implements ModuleFinder<NavigatorElement, Object> {
        @Override
        public List<NavigatorElement> resolveInModule(LogicsModule module, String simpleName, Object param) {
            NavigatorElement ne = module.getNavigatorElementByName(simpleName); 
            return ne == null ? new ArrayList<NavigatorElement>() : Collections.singletonList(ne);
        }
    }                                           

    public static class WindowNameModuleFinder implements ModuleFinder<AbstractWindow, Object> {
        @Override
        public List<AbstractWindow> resolveInModule(LogicsModule module, String simpleName, Object param) {
            AbstractWindow wnd = module.getWindowByName(simpleName); 
            return wnd == null ? new ArrayList<AbstractWindow>() : Collections.singletonList(wnd);
        }
    }

    public static class MetaCodeNameModuleFinder implements ModuleFinder<MetaCodeFragment, Integer> {
        @Override
        public List<MetaCodeFragment> resolveInModule(LogicsModule module, String simpleName, Integer paramCnt) {
            MetaCodeFragment code = module.getMetaCodeFragmentByName(simpleName, paramCnt); 
            return code == null ? new ArrayList<MetaCodeFragment>() : Collections.singletonList(code);
        }
    }

    public static class TableNameModuleFinder implements ModuleFinder<ImplementTable, Object> {
        @Override
        public List<ImplementTable> resolveInModule(LogicsModule module, String simpleName, Object param) {
            ImplementTable table = module.getTableByName(simpleName); 
            return table == null ? new ArrayList<ImplementTable>() : Collections.singletonList(table);
        }
    }

    public static class ClassNameModuleFinder implements ModuleFinder<ValueClass, Object> {
        @Override
        public List<ValueClass> resolveInModule(LogicsModule module, String simpleName, Object param) {
            ValueClass cls = module.getClassByName(simpleName);             
            return cls == null ? new ArrayList<ValueClass>() : Collections.singletonList(cls);
        }
    }
}
