package platform.server.logics;

import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;
import platform.base.BaseUtils;
import platform.base.Pair;
import platform.base.col.ListFact;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.*;
import platform.base.col.interfaces.mutable.*;
import platform.base.col.interfaces.mutable.mapvalue.GetIndex;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.KeyStrokes;
import platform.interop.ModalityType;
import platform.interop.form.GlobalConstants;
import platform.server.Settings;
import platform.server.caches.IdentityStrongLazy;
import platform.server.classes.*;
import platform.server.context.ThreadLocalContext;
import platform.server.data.Time;
import platform.server.data.Union;
import platform.server.data.expr.StringAggUnionProperty;
import platform.server.data.expr.query.GroupType;
import platform.server.data.expr.query.PartitionType;
import platform.server.data.type.Type;
import platform.server.data.where.classes.ClassWhere;
import platform.server.form.entity.*;
import platform.server.form.instance.FormSessionScope;
import platform.server.form.navigator.NavigatorAction;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.window.AbstractWindow;
import platform.server.logics.linear.LAP;
import platform.server.logics.linear.LCP;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.*;
import platform.server.logics.property.actions.flow.*;
import platform.server.logics.property.derived.*;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.scripted.EvalActionProperty;
import platform.server.logics.scripted.MetaCodeFragment;
import platform.server.logics.table.ImplementTable;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

import static java.util.Arrays.copyOfRange;
import static platform.base.BaseUtils.add;
import static platform.base.BaseUtils.consecutiveInts;
import static platform.server.logics.PropertyUtils.*;
import static platform.server.logics.property.derived.DerivedProperty.*;

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

    private final Map<String, LP<?,?>> moduleProperties = new HashMap<String, LP<?, ?>>();
    private final Map<String, AbstractGroup> moduleGroups = new HashMap<String, AbstractGroup>();
    private final Map<String, ValueClass> moduleClasses = new HashMap<String, ValueClass>();
    private final Map<String, AbstractWindow> windows = new HashMap<String, AbstractWindow>();
    public final Map<String, NavigatorElement<?>> moduleNavigators = new HashMap<String, NavigatorElement<?>>();
    private final Map<String, ImplementTable> moduleTables = new HashMap<String, ImplementTable>();

    private final Map<String, List<String>> propNamedParams = new HashMap<String, List<String>>();
    private final Map<Pair<String, Integer>, MetaCodeFragment> metaCodeFragments = new HashMap<Pair<String, Integer>, MetaCodeFragment>();

    protected LogicsModule() {}

    public LogicsModule(String name) {
        this(name, name);
    }

    public LogicsModule(String name, String namespace) {
        this(name, namespace, new ArrayList<String>());
    }

    public LogicsModule(String name, String namespace, List<String> requiredModules) {
        this.name = name;
        this.namespace = namespace;
        this.requiredModules = requiredModules;
    }

    private String name;
    private String namespace;
    private List<String> requiredModules;

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public LP<?, ?> getLPBySID(String sID) {
        return moduleProperties.get(sID);
    }

    public LP<?, ?> getLPByName(String name) {
        return getLPBySID(transformNameToSID(name));
    }

    public LCP<?> getLCPByName(String name) {
        return (LCP<?>) getLPByName(name);
    }

    public LAP<?> getLAPByName(String name) {
        return (LAP<?>) getLPByName(name);
    }

    protected void addModuleLP(LP<?, ?> lp) {
        assert !moduleProperties.containsKey(lp.property.getSID());
        moduleProperties.put(lp.property.getSID(), lp);
    }

    protected void removeModuleLP(LP<?, ?> lp) {
        moduleProperties.remove(lp.property.getSID());
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

    protected AbstractGroup rootGroup;
    protected AbstractGroup publicGroup;
    protected AbstractGroup privateGroup;
    protected AbstractGroup baseGroup;
    protected AbstractGroup recognizeGroup;

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
        assert name.charAt(0) >= 'A' && name.charAt(0) <= 'Z';
        ConcreteCustomClass customClass = new ConcreteCustomClass(transformNameToSID(name), caption, sids, names, parents);
        storeCustomClass(customClass);
        return customClass;
    }

    protected AbstractCustomClass addAbstractClass(String name, String caption, CustomClass... parents) {
        assert name.charAt(0) >= 'A' && name.charAt(0) <= 'Z';
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

    protected LCP addDProp(AbstractGroup group, String name, String caption, ValueClass value, boolean stored, ValueClass... params) {
        if(stored)
            return addDProp(group, name, caption, value, params);
        else
            return addSDProp(group, name, caption, value, params);
    }

    protected LCP addDProp(String name, String caption, ValueClass value, ValueClass... params) {
        return addDProp(null, name, false, caption, value, params);
    }

    protected LCP addDProp(AbstractGroup group, String name, String caption, ValueClass value, ValueClass... params) {
        return addDProp(group, name, false, caption, value, params);
    }

    protected LCP[] addDProp(AbstractGroup group, String paramID, String[] names, String[] captions, ValueClass[] values, ValueClass... params) {
        LCP[] result = new LCP[names.length];
        for (int i = 0; i < names.length; i++)
            result[i] = addDProp(group, names[i] + paramID, captions[i], values[i], params);
        return result;
    }

    protected LCP addDProp(AbstractGroup group, String name, boolean persistent, String caption, ValueClass value, ValueClass... params) {
        StoredDataProperty dataProperty = new StoredDataProperty(name, caption, params, value);
        LCP lp = addProperty(group, persistent, new LCP<ClassPropertyInterface>(dataProperty));
        dataProperty.markStored(baseLM.tableFactory);
        return lp;
    }

    protected LCP addGDProp(AbstractGroup group, String paramID, String name, String caption, ValueClass[] values, CustomClass[]... params) {
        CustomClass[][] listParams = new CustomClass[params[0].length][]; //
        for (int i = 0; i < listParams.length; i++) {
            listParams[i] = new CustomClass[params.length];
            for (int j = 0; j < params.length; j++)
                listParams[i][j] = params[j][i];
        }
        params = listParams;

        LCP[] genProps = new LCP[params.length];
        for (int i = 0; i < params.length; i++) {
            String genID = "";
            String genCaption = "";
            for (int j = 0; j < params[i].length; j++) {
                genID += params[i][j].getSID();
                genCaption = (genCaption.length() == 0 ? "" : genCaption) + params[i][j].caption;
            }
            genProps[i] = addDProp(name + genID, caption + " (" + genCaption + ")", values[i], params[i]);
        }

        return addCUProp(group, name + paramID, caption, genProps);
    }

    protected LCP[] addGDProp(AbstractGroup group, String paramID, String[] names, String[] captions, ValueClass[][] values, CustomClass[]... params) {
        LCP[] result = new LCP[values.length];
        for (int i = 0; i < values.length; i++)
            result[i] = addGDProp(group, paramID, names[i], captions[i], values[i], params);
        return result;
    }

    protected <D extends PropertyInterface> LCP addDCProp(String name, String caption, LCP<D> derivedProp, Object... params) {
        return addDCProp(null, name, caption, derivedProp, params);
    }

    protected <D extends PropertyInterface> LCP addDCProp(String name, String caption, int whereNum, LCP<D> derivedProp, Object... params) {
        return addDCProp(null, name, caption, whereNum, derivedProp, params);
    }

    protected <D extends PropertyInterface> LCP addDCProp(String name, boolean persistent, String caption, LCP<D> derivedProp, Object... params) {
        return addDCProp(null, name, persistent, caption, false, derivedProp, params);
    }

    protected <D extends PropertyInterface> LCP addDCProp(AbstractGroup group, String name, String caption, LCP<D> derivedProp, Object... params) {
        return addDCProp(group, name, caption, 0, derivedProp, params);
    }

    protected <D extends PropertyInterface> LCP addDCProp(AbstractGroup group, String name, String caption, int whereNum,  LCP<D> derivedProp, Object... params) {
        return addDCProp(group, name, false, caption, false, whereNum, derivedProp, params);
    }

    protected <D extends PropertyInterface> LCP addDCProp(String name, String caption, boolean forced, LCP<D> derivedProp, Object... params) {
        return addDCProp(null, name, caption, forced, derivedProp, params);
    }

    protected <D extends PropertyInterface> LCP addDCProp(AbstractGroup group, String name, String caption, boolean forced, LCP<D> derivedProp, Object... params) {
        return addDCProp(group, name, false, caption, forced, derivedProp, params);
    }

    protected <D extends PropertyInterface> LCP addDCProp(AbstractGroup group, String name, boolean persistent, String caption, boolean forced, LCP<D> derivedProp, Object... params) {
        return addDCProp(group, name, persistent, caption, forced, 0, derivedProp, params);
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

    protected LCP addSDProp(String caption, ValueClass value, ValueClass... params) {
        return addSDProp((AbstractGroup) null, caption, value, params);
    }

    protected LCP addSDProp(AbstractGroup group, String caption, ValueClass value, ValueClass... params) {
        return addSDProp(group, genSID(), caption, value, params);
    }

    protected LCP addSDProp(String name, String caption, ValueClass value, ValueClass... params) {
        return addSDProp(null, name, caption, value, params);
    }

    protected LCP addSDProp(String name, boolean persistent, String caption, ValueClass value, ValueClass... params) {
        return addSDProp(null, name, persistent, caption, value, params);
    }

    protected LCP addSDProp(AbstractGroup group, String name, String caption, ValueClass value, ValueClass... params) {
        return addSDProp(group, name, false, caption, value, params);
    }

    protected LCP addSDProp(AbstractGroup group, String name, boolean persistent, String caption, ValueClass value, ValueClass... params) {
        return addProperty(group, persistent, new LCP<ClassPropertyInterface>(new SessionDataProperty(name, caption, params, value)));
    }

    protected LAP addFAProp(String caption, FormEntity form, ObjectEntity... params) {
        return addFAProp(null, caption, form, params, null, false, false);
    }

    protected LAP addFAProp(AbstractGroup group, FormEntity form, ObjectEntity... params) {
        return addFAProp(group, form.caption, form, params);
    }

    protected LAP addFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity... params) {
        return addFAProp(group, caption, form, params, null, false, false);
    }

    public LAP addMFAProp(String caption, FormEntity form, ObjectEntity... params) {
        return addMFAProp(null, caption, form, params, false);
    }

    protected LAP addMFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, boolean newSession) {
        return addMFAProp(group, genSID(), caption, form, objectsToSet, newSession);
    }

    protected LAP addMFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, ActionPropertyObjectEntity startAction, boolean newSession) {
        return addMFAProp(group, genSID(), caption, form, objectsToSet, startAction, newSession);
    }

    protected LAP addMFAProp(AbstractGroup group, String sID, String caption, FormEntity form, ObjectEntity[] objectsToSet, boolean newSession) {
        return addMFAProp(group, sID, caption, form, objectsToSet, null, newSession);
    }

    protected LAP addMFAProp(AbstractGroup group, String sID, String caption, FormEntity form, ObjectEntity[] objectsToSet, ActionPropertyObjectEntity startAction, boolean newSession) {
        return addFAProp(group, sID, caption, form, objectsToSet, startAction, newSession, true, false);
    }

    public LAP addDMFAProp(String caption, FormEntity form, ObjectEntity... params) {
        return addDMFAProp(null, caption, form, params, false);
    }

    protected LAP addDMFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, boolean newSession) {
        return addDMFAProp(group, genSID(), caption, form, objectsToSet, newSession);
    }

    protected LAP addDMFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, boolean newSession, ActionPropertyObjectEntity startAction) {
        return addDMFAProp(group, genSID(), caption, form, objectsToSet, startAction, newSession);
    }

    protected LAP addDMFAProp(AbstractGroup group, String sID, String caption, FormEntity form, ObjectEntity[] objectsToSet, boolean newSession) {
        return addDMFAProp(group, sID, caption, form, objectsToSet, null, newSession);
    }

    protected LAP addDMFAProp(AbstractGroup group, String sID, String caption, FormEntity form, ObjectEntity[] objectsToSet, ActionPropertyObjectEntity startAction, boolean newSession) {
        return addFAProp(group, sID, caption, form, objectsToSet, startAction,
                         newSession ? FormSessionScope.NEWSESSION : FormSessionScope.OLDSESSION,
                         ModalityType.DOCKED_MODAL, false);
    }

    protected LAP addFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, ActionPropertyObjectEntity startAction, boolean newSession, boolean isModal) {
        return addFAProp(group, genSID(), caption, form, objectsToSet, startAction, newSession, isModal, false);
    }

    protected LAP addFAProp(AbstractGroup group, String sID, String caption, FormEntity form, ObjectEntity[] objectsToSet, ActionPropertyObjectEntity startAction, boolean newSession, boolean isModal, boolean checkOnOk) {
        return addFAProp(group, sID, caption, form, objectsToSet, startAction,
                         newSession ? FormSessionScope.NEWSESSION : FormSessionScope.OLDSESSION,
                         isModal ? ModalityType.MODAL : ModalityType.DOCKED, checkOnOk);
    }

    protected LAP addFAProp(AbstractGroup group, String sID, String caption, FormEntity form, ObjectEntity[] objectsToSet, ActionPropertyObjectEntity startAction, FormSessionScope sessionScope, ModalityType modalityType, boolean checkOnOk) {
        return addFAProp(group, sID, caption, form, objectsToSet, startAction, sessionScope, modalityType, checkOnOk, false);
    }

    protected LAP addFAProp(AbstractGroup group, String sID, String caption, FormEntity form, ObjectEntity[] objectsToSet, ActionPropertyObjectEntity startAction, FormSessionScope sessionScope, ModalityType modalityType, boolean checkOnOk, boolean showDrop) {
        return addProperty(group, new LAP(new FormActionProperty(sID, caption, form, objectsToSet, startAction, sessionScope, modalityType, checkOnOk, showDrop, baseLM.formResult, baseLM.getFormResultProperty(), baseLM.getChosenValueProperty())));
    }

    protected LAP addChangeClassAProp() {
        return addAProp(baseClass.getChangeClassValueAction());
    }

    protected LAP addChangeClassAProp(ConcreteObjectClass cls, int resInterfaces, int changeIndex, boolean extendedContext, boolean conditional, Object... params) {
        int innerIntCnt = resInterfaces + (extendedContext ? 1 : 0);
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(innerIntCnt);
        ImOrderSet<PropertyInterface> mappedInterfaces = extendedContext ? innerInterfaces.removeOrderIncl(innerInterfaces.get(changeIndex)) : innerInterfaces;
        ImList<CalcPropertyInterfaceImplement<PropertyInterface>> readImplements = readCalcImplements(innerInterfaces, params);
        CalcPropertyMapImplement<PropertyInterface, PropertyInterface> conditionalPart = (CalcPropertyMapImplement<PropertyInterface, PropertyInterface>)
                (conditional ? readImplements.get(resInterfaces) : null);

        return addAProp(new ChangeClassActionProperty<PropertyInterface, PropertyInterface>(genSID(), cls, false, innerInterfaces.getSet(),
                mappedInterfaces, innerInterfaces.get(changeIndex), conditionalPart, baseClass));
    }

    protected <C extends PropertyInterface, W extends PropertyInterface> LAP addSetPropertyAProp(Object... params) {
        return addSetPropertyAProp("sys", params);
    }

    protected <C extends PropertyInterface, W extends PropertyInterface> LAP addSetPropertyAProp(String caption, Object... params) {
        return addSetPropertyAProp(genSID(), caption, params);
    }

    protected <C extends PropertyInterface, W extends PropertyInterface> LAP addSetPropertyAProp(AbstractGroup group, String caption, Object... params) {
        return addSetPropertyAProp(group, genSID(), caption, params);
    }

    protected <C extends PropertyInterface, W extends PropertyInterface> LAP addSetPropertyAProp(String name, String caption, Object... params) {
        return addSetPropertyAProp(null, name, caption, params);
    }

    protected <C extends PropertyInterface, W extends PropertyInterface> LAP addSetPropertyAProp(AbstractGroup group, String name, String caption, Object... params) {
        int resInterfaces = getIntNum(params);
        return addSetPropertyAProp(group, name, caption, resInterfaces, false, BaseUtils.add(ListFact.consecutiveList(resInterfaces).toArray(new Integer[resInterfaces]), params));
    }

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

    protected LAP addAbstractListAProp(int paramCnt) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(paramCnt);
        return addProperty(null, new LAP(new ListActionProperty(genSID(), "sys", listInterfaces)));
    }

    protected LAP addIfAProp(Object... params) {
        return addIfAProp(false, params);    }

    protected LAP addIfAProp(boolean not, Object... params) {
        return addIfAProp("if", not, params);
    }
    protected LAP addIfAProp(String caption, Object... params) {
        return addIfAProp(caption, false, params);
    }
    protected LAP addIfAProp(String caption, boolean not, Object... params) {
        return addIfAProp(null, genSID(), caption, not, params);
    }
    protected LAP addIfAProp(AbstractGroup group, String caption, Object... params) {
        return addIfAProp(group, caption, false, params);
    }
    protected LAP addIfAProp(AbstractGroup group, String name, String caption, Object... params) {
        return addIfAProp(group, name, caption, false, params);
    }
    protected LAP addIfAProp(AbstractGroup group, String caption, boolean not, Object... params) {
        return addIfAProp(group, genSID(), caption, not, params);
    }
    protected LAP addIfAProp(AbstractGroup group, String name, String caption, boolean not, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<PropertyInterfaceImplement<PropertyInterface>> readImplements = readImplements(listInterfaces, params);
        assert readImplements.size() >= 2 && readImplements.size() <= 3;

        return addProperty(group, new LAP(new IfActionProperty(name, caption, not, listInterfaces, (CalcPropertyMapImplement<?, PropertyInterface>) readImplements.get(0),
                (ActionPropertyMapImplement<?, PropertyInterface>) readImplements.get(1), readImplements.size() == 3 ? (ActionPropertyMapImplement<?, PropertyInterface>) readImplements.get(2) : null)));
    }

    protected LAP addPushAProp(Object... params) {
        return addPushAProp(null, genSID(), "sys", params);
    }
    protected LAP addPushAProp(AbstractGroup group, String name, String caption, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<PropertyInterfaceImplement<PropertyInterface>> readImplements = readImplements(listInterfaces, params);

        return addProperty(group, new LAP(new PushUserInputActionProperty(name, caption, listInterfaces, (CalcPropertyInterfaceImplement<PropertyInterface>) readImplements.get(0),
                (ActionPropertyMapImplement<?, PropertyInterface>) readImplements.get(1))));
    }

    protected LAP addForAProp(boolean hasElse, int resInterfaces, Object... params) {
        return addForAProp(false, false, false, hasElse, resInterfaces, params);
    }

    protected LAP addForAProp(boolean ascending, boolean ordersNotNull, boolean recursive, boolean hasElse, int resInterfaces, Object... params) {
        return addForAProp(null, genSID(), "sys", ascending, ordersNotNull, recursive, hasElse, resInterfaces, null, true, 0, false, params);
    }

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

    protected LAP addJoinAProp(LAP action, Object... params) {
        return addJoinAProp("sys", action, params);
    }

    protected LAP addJoinAProp(ValueClass[] classes, LAP action, Object... params) {
        return addJoinAProp(null, genSID(), "sys", classes, action, params);
    }

    protected LAP addJoinAProp(String caption, LAP action, Object... params) {
        return addJoinAProp(genSID(), caption, action, params);
    }

    protected LAP addJoinAProp(String name, String caption, LAP action, Object... params) {
        return addJoinAProp(null, name, caption, action, params);
    }

    protected LAP addJoinAProp(AbstractGroup group, String caption, LAP action, Object... params) {
        return addJoinAProp(group, genSID(), caption, null, action, params);
    }

    protected LAP addJoinAProp(AbstractGroup group, String name, String caption, LAP action, Object... params) {
        return addJoinAProp(group, name, caption, null, action, params);
    }

    protected LAP addJoinAProp(AbstractGroup group, String name, String caption, ValueClass[] classes, LAP action, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<CalcPropertyInterfaceImplement<PropertyInterface>> readImplements = readCalcImplements(listInterfaces, params);
        return addProperty(group, new LAP(new JoinActionProperty(name, caption, listInterfaces, mapActionImplement(action, readImplements))));
    }


    protected LP addNewSessionAProp(AbstractGroup group, String name, String caption, LAP action, boolean doApply, boolean singleApply, ImSet<SessionDataProperty> local, ImSet<SessionDataProperty> sessionUsed) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(action.listInterfaces.size());
        ActionPropertyMapImplement<?, PropertyInterface> actionImplement = mapActionListImplement(action, listInterfaces);

        return addProperty(group, new LAP(new NewSessionActionProperty(name, caption, listInterfaces, actionImplement, doApply,
                singleApply, sessionUsed, local)));
    }

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

    protected <P extends PropertyInterface> LCP addSCProp(LCP<P> lp) {
        return addSCProp(baseLM.privateGroup, "sys", lp);
    }

    protected <P extends PropertyInterface> LCP addSCProp(AbstractGroup group, String caption, LCP<P> lp) {
        return addProperty(group, new LCP<ShiftChangeProperty.Interface<P>>(new ShiftChangeProperty<P, PropertyInterface>(genSID(), caption,
                ((CalcProperty)lp.property), new CalcPropertyMapImplement<PropertyInterface, P>((CalcProperty<PropertyInterface>) getLCPByName("reverseBarcode").property))));
    }

    public LCP addCProp(StaticClass valueClass, Object value, ValueClass... params) {
        return addCProp("sys", valueClass, value, params);
    }

    protected LCP addCProp(String caption, StaticClass valueClass, Object value, ValueClass... params) {
        return addCProp(null, false, caption, valueClass, value, params);
    }

    protected LCP addCProp(AbstractGroup group, boolean persistent, String caption, StaticClass valueClass, Object value, ValueClass... params) {
        return addCProp(group, persistent, caption, valueClass, value, Arrays.asList(params));
    }

    // только для того, чтобы обернуть все в IdentityLazy, так как только для List нормально сделан equals
    @IdentityStrongLazy // для ID
    protected LCP addCProp(AbstractGroup group, boolean persistent, String caption, StaticClass valueClass, Object value, List<ValueClass> params) {
        return addCProp(group, genSID(), persistent, caption, valueClass, value, params.toArray(new ValueClass[]{}));
    }

    protected <T extends PropertyInterface> LCP addCProp(AbstractGroup group, String name, boolean persistent, String caption, StaticClass valueClass, Object value, ValueClass... params) {
        CalcPropertyRevImplement<T, Integer> implement = (CalcPropertyRevImplement<T, Integer>) DerivedProperty.createCProp(name, caption, valueClass, value, ListFact.toList(params).toIndexedMap());
        return addProperty(group, persistent, new LCP<T>(implement.property, ListFact.fromIndexedMap(implement.mapping.reverse())));
    }

    // добавляет свойство с бесконечным значением
    protected <T extends PropertyInterface> LCP addICProp(DataClass valueClass, ValueClass... params) {
        CalcPropertyRevImplement<T, Integer> implement = (CalcPropertyRevImplement<T, Integer>) DerivedProperty.createCProp(genSID(), ServerResourceBundle.getString("logics.infinity"), valueClass, ListFact.toList(params).toIndexedMap());
        return addProperty(baseLM.privateGroup, false, new LCP<T>(implement.property, ListFact.fromIndexedMap(implement.mapping.reverse())));
    }


    protected LCP addTProp(Time time) {
        return addTProp(genSID(), time.toString(), time);
    }

    protected LCP addTProp(String sID, String caption, Time time) {
        return addProperty(null, new LCP<PropertyInterface>(new TimeFormulaProperty(sID, caption, time)));
    }

    protected <P extends PropertyInterface> LCP addTCProp(Time time, String name, boolean isStored, String caption, LCP<P> changeProp, ValueClass... classes) {
        return addTCProp(null, time, name, isStored, caption, changeProp, classes);
    }

    protected <P extends PropertyInterface, T extends PropertyInterface> LCP addTCProp(AbstractGroup group, Time time, String name, boolean isStored, String caption, LCP<P> changeProp, ValueClass... classes) {
        LCP timeProperty = addDProp(group, name, caption, time.getConcreteValueClass(), isStored, overrideClasses(changeProp.getInterfaceClasses(), classes));
        LCP curTime = addTProp(time);
        LAP setCurTime = addSetPropertyAProp(BaseUtils.add(directLI(timeProperty), curTime));
        setCurTime.setEventAction(this, Event.SESSION, changeProp, getIntParams(changeProp));
/*        LAP editAction = changeProp.getEditAction(ServerResponse.CHANGE);
        LAP<?> overrideAction = addListAProp(BaseUtils.add(directLI(editAction), directLI(setCurTime)));
        changeProp.setEditAction(ServerResponse.CHANGE, overrideAction);*/
        return timeProperty;
    }

    protected LCP addSFProp(String name, String formula, int paramCount) {
        return addSFProp(name, formula, null, paramCount);
    }

    protected LCP addSFProp(String formula, int paramCount) {
        return addSFProp(formula, (ConcreteValueClass) null, paramCount);
    }

    protected LCP addSFProp(String formula, ConcreteValueClass value, int paramCount) {
        return addSFProp(genSID(), formula, value, paramCount);
    }

    protected LCP addSFProp(String name, String formula, ConcreteValueClass value, int paramCount) {
        return addProperty(null, new LCP<StringFormulaProperty.Interface>(new StringFormulaProperty(name, value, formula, paramCount)));
    }

    protected LCP addCFProp(Compare compare) {
        return addCFProp(genSID(), compare);
    }

    protected LCP addCFProp(String name, Compare compare) {
        return addProperty(null, new LCP<CompareFormulaProperty.Interface>(new CompareFormulaProperty(name, compare)));
    }

    protected <P extends PropertyInterface> LCP addSumProp(String name) {
        return addProperty(null, new LCP<SumProperty.Interface>(new SumProperty(name, "uSum")));
    }

    @IdentityStrongLazy
    protected <P extends PropertyInterface> LCP addCastProp(DataClass castClass) {
        return addCastProp(genSID(), castClass);
    }

    protected <P extends PropertyInterface> LCP addCastProp(String name, DataClass castClass) {
        return addProperty(null, new LCP<CastProperty.Interface>(new CastProperty(name, "castTo" + castClass.toString(), castClass)));
    }

    protected <P extends PropertyInterface> LCP addSProp(int intNum) {
        return addSProp(genSID(), intNum);
    }

    protected <P extends PropertyInterface> LCP addSProp(String name, int intNum) {
        return addSProp(name, intNum, " ");
    }

    protected <P extends PropertyInterface> LCP addSProp(String name, int intNum, String separator) {
        return addProperty(null, new LCP<StringConcatenateProperty.Interface>(new StringConcatenateProperty(name, ServerResourceBundle.getString("logics.join"), intNum, separator)));
    }

    protected <P extends PropertyInterface> LCP addInsensitiveSProp(int intNum) {
        return addInsensitiveSProp(genSID(), intNum);
    }

    protected <P extends PropertyInterface> LCP addInsensitiveSProp(String name, int intNum) {
        return addInsensitiveSProp(name, intNum, " ");
    }

    protected <P extends PropertyInterface> LCP addInsensitiveSProp(String name, int intNum, String separator) {
        return addProperty(null, new LCP<StringConcatenateProperty.Interface>(new StringConcatenateProperty(name, ServerResourceBundle.getString("logics.join"), intNum, separator, false)));
    }


    protected LCP addMFProp(String name, int paramCount) {
        return addMFProp(name, null, paramCount);
    }
    protected LCP addMFProp(String name, ConcreteValueClass value, int paramCount) {
        return addProperty(null, new LCP<StringFormulaProperty.Interface>(new MultiplyFormulaProperty(name, value, paramCount)));
    }
    protected LCP addMFProp(int paramCount) {
        return addMFProp(genSID(), null, paramCount);
    }
    protected LCP addMFProp(ConcreteValueClass value, int paramCount) {
        return addMFProp(genSID(), value, paramCount);
    }

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

    protected LCP addCCProp(int paramCount) {
        return addProperty(null, new LCP<ConcatenateProperty.Interface>(new ConcatenateProperty(genSID(), paramCount)));
    }

    protected LCP addDCCProp(int paramIndex) {
        return addProperty(null, new LCP<DeconcatenateProperty.Interface>(new DeconcatenateProperty(genSID(), paramIndex, baseLM.baseClass)));
    }

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

    protected LCP addJProp(boolean implementChange, String caption, LCP mainProp, Object... params) {
        return addJProp((AbstractGroup) null, implementChange, caption, mainProp, params);
    }

    protected LCP addJProp(boolean implementChange, String name, String caption, LCP mainProp, Object... params) {
        return addJProp((AbstractGroup) null, implementChange, name, caption, mainProp, params);
    }

    protected LCP addJProp(AbstractGroup group, boolean implementChange, String caption, LCP mainProp, Object... params) {
        return addJProp(group, implementChange, genSID(), caption, mainProp, params);
    }

    protected LCP addJProp(AbstractGroup group, String name, String caption, LCP mainProp, Object... params) {
        return addJProp(group, false, name, caption, mainProp, params);
    }

    protected LCP addJProp(AbstractGroup group, String name, boolean persistent, String caption, LCP mainProp, Object... params) {
        return addJProp(group, false, name, persistent, caption, mainProp, params);
    }

    protected LCP addJProp(boolean implementChange, LCP mainProp, Object... params) {
        return addJProp(baseLM.privateGroup, implementChange, genSID(), "sys", mainProp, params);
    }

    protected LCP addJProp(AbstractGroup group, boolean implementChange, String name, String caption, LCP mainProp, Object... params) {
        return addJProp(group, implementChange, name, false, caption, mainProp, params);
    }

    protected LCP addJProp(AbstractGroup group, boolean implementChange, boolean persistent, String caption, LCP mainProp, Object... params) {
        return addJProp(group, implementChange, genSID(), persistent, caption, mainProp, params);
    }

    protected LCP addJProp(AbstractGroup group, boolean implementChange, String name, boolean persistent, String caption, LCP mainProp, Object... params) {

        ImOrderSet<JoinProperty.Interface> listInterfaces = JoinProperty.getInterfaces(getIntNum(params));
        JoinProperty<?> property = new JoinProperty(name, caption, listInterfaces, implementChange,
                mapCalcImplement(mainProp, readCalcImplements(listInterfaces, params)));
        property.inheritFixedCharWidth(mainProp.property);
        property.inheritImage(mainProp.property);

        return addProperty(group, persistent, new LCP<JoinProperty.Interface>(property, listInterfaces));
    }

    protected LCP[] addJProp(AbstractGroup group, boolean implementChange, String paramID, LCP[] props, String caption, Object... params) {
        LCP[] result = new LCP[props.length];
        for (int i = 0; i < props.length; i++)
            result[i] = addJProp(group, implementChange, props[i].property.getSID() + paramID, props[i].property.caption + (caption.length() == 0 ? "" : (" " + caption)), props[i], params);
        return result;
    }

    protected LCP[] addJProp(AbstractGroup group, boolean implementChange, String paramID, LCP[] props, Object... params) {
        return addJProp(group, implementChange, paramID, props, "", params);
    }

    /**
     * Создаёт свойство для группового изменения, при этом группировка идёт по всем интерфейсам, и мэппинг интерфейсов происходит по порядку
     */
    protected LAP addGCAProp(AbstractGroup group, String name, String caption, LAP mainProperty) {
        return addGCAProp(group, name, caption, null, mainProperty);
    }

    /**
     * Создаёт свойство для группового изменения, при этом группировка идёт по всем интерфейсам, мэппинг интерфейсов происходит по порядку
     * при этом, если groupObject != null, то результирующее свойство будет принимать на входы в том числе и группирующие интерфейсы.
     * Это нужно для того, чтобы можно было создать фильтр по ключам этого groupObject'а
     * <p>
     */
    protected LAP addGCAProp(GroupObjectEntity groupObject, LAP mainProperty) {
        return addGCAProp(null, genSID(), "sys", groupObject, mainProperty);

    }
    protected LAP addGCAProp(AbstractGroup group, String name, String caption, GroupObjectEntity groupObject, LAP mainProperty) {
        int mainInts[] = consecutiveInts(mainProperty.listInterfaces.size());

        return addGCAProp(group, name, caption, groupObject, mainProperty, mainInts, mainInts);
    }

    /**
     * Создаёт свойство для группового изменения
     * Пример:
     * <pre>
     *   LCP ценаПоставкиТовара = Свойство(Товар)
     *   LCP ценаПродажиТовара = Свойство(Магазин, Товар)
     *
     *   Тогда, чтобы установить цену для всех товаров в магазине, равной цене поставки товара, создаём свойство
     *
     *   addGCAProp(...,
     *      ценаПродажиТовара,  // изменяемое свойство
     *      1, 2,               // номера интерфейсов результирующего свойства на входах изменяемого свойтства
     *      ценаПоставкиТовара, // изменяющее свойство
     *      2,                  // номера интерфейсов результирующего свойства на входах изменяющего свойтства
     *      2                   // номера интерфейсов для группировки
     *      )
     * </pre>
     *
     * Если groupObject != null, то результирующее свойство будет принимать на входы в том числе и группирующие интерфейсы.
     * Это нужно для того, чтобы можно было создать фильтр по ключам этого groupObject'а.
     * Если groupObject == null, то группирующие интерфейсы включены не будут.
     *
     * @param groupObject используется для получения фильтров на набор, для которого будут происходить изменения.
     *
     * @param params      сначала идут номера интерфейсов для входов главного свойства,
     *                      затем getterProperty,
     *                      затем мэппинг интерфейсов getterProperty,
     *                      затем номера групирующих интерфейсов
     */
    protected LAP addGCAProp(AbstractGroup group, String name, String caption, GroupObjectEntity groupObject, LAP mainProperty, Object... params) {
        assert params.length > 0;

        int mainIntCnt = mainProperty.listInterfaces.size();

        int groupIntCnt = params.length - mainIntCnt;

        int mainInts[] = new int[mainIntCnt];
        int groupInts[] = new int[groupIntCnt];

        for (int i = 0; i < mainIntCnt; ++i) {
            mainInts[i] = (Integer)params[i] - 1;
        }

        for (int i = 0; i < groupIntCnt; ++i) {
            groupInts[i] = (Integer)params[mainIntCnt] - 1;
        }

        return addGCAProp(group, name, caption, groupObject, mainProperty, mainInts, groupInts);
    }

    private LAP addGCAProp(AbstractGroup group, String name, String caption, GroupObjectEntity groupObject, LAP mainProperty, int[] mainInts, int[] groupInts) {
        return addProperty(group, new LAP(
                new PrevGroupChangeActionProperty(name, caption, groupObject,
                        mainProperty, mainInts, groupInts)));
    }

    public void showIf(FormEntity<?> form, LP[] properties, LCP ifProperty, ObjectEntity... objects) {
        for (LP property : properties)
            showIf(form, property, ifProperty, objects);
    }

    public void showIf(FormEntity<?> form, LP property, LCP ifProperty, ObjectEntity... objects) {
        PropertyObjectEntity hideCaptionProp = form.addPropertyObject(addHideCaptionProp(ifProperty), objects);
        for (PropertyDrawEntity propertyDraw : form.getProperties(property.property)) {
            propertyDraw.propertyCaption = (CalcPropertyObjectEntity) hideCaptionProp;
        }
    }

    public void showIf(FormEntity<?> form, PropertyDrawEntity property, LCP ifProperty, PropertyObjectInterfaceEntity... objects) {
        property.propertyCaption = form.addPropertyObject(addHideCaptionProp(ifProperty), objects);
    }

    private <P extends PropertyInterface, L extends PropertyInterface> LCP mapLProp(AbstractGroup group, boolean persistent, CalcPropertyMapImplement<L, P> implement, ImOrderSet<P> listInterfaces) {
        return addProperty(group, persistent, new LCP<L>(implement.property, listInterfaces.mapOrder(implement.mapping.reverse())));
    }

    private <P extends PropertyInterface, L extends PropertyInterface> LAP mapLAProp(AbstractGroup group, ActionPropertyMapImplement<L, P> implement, ImOrderSet<P> listInterfaces) {
        return addProperty(group, new LAP<L>(implement.property, listInterfaces.mapOrder(implement.mapping.reverse())));
    }

    private <P extends PropertyInterface, L extends PropertyInterface> LCP mapLProp(AbstractGroup group, boolean persistent, CalcPropertyMapImplement<L, P> implement, LCP<P> property) {
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

    protected <P extends PropertyInterface> LCP addOProp(String caption, PartitionType partitionType, LCP<P> sum, boolean ascending, boolean includeLast, int partNum, Object... params) {
        return addOProp(genSID(), caption, partitionType, sum, ascending, includeLast, partNum, params);
    }

    protected <P extends PropertyInterface> LCP addOProp(String name, String caption, PartitionType partitionType, LCP<P> sum, boolean ascending, boolean includeLast, int partNum, Object... params) {
        return addOProp((AbstractGroup) null, name, caption, partitionType, sum, ascending, includeLast, partNum, params);
    }

    protected <P extends PropertyInterface> LCP addOProp(AbstractGroup group, String caption, PartitionType partitionType, LCP<P> sum, boolean ascending, boolean includeLast, int partNum, Object... params) {
        return addOProp(group, genSID(), caption, partitionType, sum, ascending, includeLast, partNum, params);
    }

    protected <P extends PropertyInterface> LCP addOProp(AbstractGroup group, String name, String caption, PartitionType partitionType, LCP<P> sum, boolean ascending, boolean includeLast, int partNum, Object... params) {
        return addOProp(group, name, false, caption, sum, partitionType, ascending, includeLast, partNum, params);
    }

    protected <P extends PropertyInterface> LCP addOProp(AbstractGroup group, String name, boolean persistent, String caption, LCP<P> sum, PartitionType partitionType, boolean ascending, boolean includeLast, int partNum, Object... params) {
        ImList<CalcPropertyInterfaceImplement<P>> li = readCalcImplements(sum.listInterfaces, params);

        ImSet<CalcPropertyInterfaceImplement<P>> partitions = li.subList(0, partNum).toOrderSet().getSet();
        ImOrderMap<CalcPropertyInterfaceImplement<P>, Boolean> orders = li.subList(partNum, li.size()).toOrderSet().toOrderMap(!ascending);

        CalcPropertyMapImplement<?, P> orderProperty;
        orderProperty = DerivedProperty.createOProp(name, caption, partitionType, (CalcProperty<P>) sum.property, partitions, orders, includeLast);

        return mapLProp(group, persistent, orderProperty, sum);
    }

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

    protected <R extends PropertyInterface, L extends PropertyInterface> LCP addUGProp(AbstractGroup group, String caption, boolean ascending, LCP<R> restriction, LCP<L> ungroup, Object... params) {
        return addUGProp(group, genSID(), caption, ascending, restriction, ungroup, params);
    }

    protected <R extends PropertyInterface, L extends PropertyInterface> LCP addUGProp(AbstractGroup group, String name, String caption, boolean ascending, LCP<R> restriction, LCP<L> ungroup, Object... params) {
        return addUGProp(group, name, false, caption, ascending, restriction, ungroup, params);
    }

    protected <R extends PropertyInterface, L extends PropertyInterface> LCP addUGProp(AbstractGroup group, String name, boolean persistent, String caption, boolean ascending, LCP<R> restriction, LCP<L> ungroup, Object... params) {
        return addUGProp(group, name, persistent, caption, ascending, restriction, ungroup, params);
    }

    protected <R extends PropertyInterface, L extends PropertyInterface> LCP addUGProp(AbstractGroup group, String name, boolean persistent, boolean over, String caption, boolean ascending, LCP<R> restriction, LCP<L> ungroup, Object... params) {
        return addUGProp(group, name, persistent, over, caption, restriction.listInterfaces.size(), ascending, Settings.get().isDefaultOrdersNotNull(), ungroup, add(directLI(restriction), params));
    }

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

    protected <R extends PropertyInterface, L extends PropertyInterface> LCP addPGProp(AbstractGroup group, String name, boolean persistent, int roundlen, boolean roundfirst, String caption, LCP<R> proportion, LCP<L> ungroup, Object... params) {
        return addPGProp(group, name, persistent, roundlen, roundfirst, caption, proportion.listInterfaces.size(), true, Settings.get().isDefaultOrdersNotNull(), ungroup, add(add(directLI(proportion), params), getParams(proportion)));
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

    protected LCP addSGProp(LCP groupProp, Object... params) {
        return addSGProp(baseLM.privateGroup, "sys", groupProp, params);
    }

    protected LCP addSGProp(String caption, LCP groupProp, Object... params) {
        return addSGProp((AbstractGroup) null, caption, groupProp, params);
    }

    protected LCP addSGProp(AbstractGroup group, String caption, LCP groupProp, Object... params) {
        return addSGProp(group, genSID(), caption, groupProp, params);
    }

    protected LCP addSGProp(String name, String caption, LCP groupProp, Object... params) {
        return addSGProp(name, false, caption, groupProp, params);
    }

    protected LCP addSGProp(String name, boolean persistent, String caption, LCP groupProp, Object... params) {
        return addSGProp(null, name, persistent, caption, groupProp, params);
    }

    protected LCP addSGProp(AbstractGroup group, String name, String caption, LCP groupProp, Object... params) {
        return addSGProp(group, name, false, caption, groupProp, params);
    }

    protected LCP addSGProp(AbstractGroup group, boolean persistent, String caption, LCP groupProp, Object... params) {
        return addSGProp(group, genSID(), persistent, caption, groupProp, params);
    }

    protected LCP addSGProp(AbstractGroup group, String name, boolean persistent, String caption, LCP groupProp, Object... params) {
        return addSGProp(group, name, persistent, false, caption, groupProp, params);
    }

    protected <T extends PropertyInterface> LCP addSGProp(AbstractGroup group, String name, boolean persistent, boolean notZero, String caption, LCP<T> groupProp, Object... params) {
        return addSGProp(group, name, persistent, notZero, caption, groupProp, readCalcImplements(groupProp.listInterfaces, params));
    }

    private <T extends PropertyInterface> LCP addSGProp(AbstractGroup group, String name, boolean persistent, boolean notZero, String caption, LCP<T> groupProp, ImList<CalcPropertyInterfaceImplement<T>> listImplements) {
        return addSGProp(group, name, persistent, notZero, caption, groupProp.listInterfaces, ListFact.add(((CalcProperty<T>) groupProp.property).getImplement(), listImplements));
    }

    protected ImOrderSet<PropertyInterface> genInterfaces(int interfaces) {
        return SetFact.toOrderExclSet(interfaces, Property.genInterface);
    }

    protected LCP addSGProp(AbstractGroup group, String name, boolean persistent, boolean notZero, String caption, int interfaces, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(interfaces);
        return addSGProp(group, name, persistent, notZero, caption, innerInterfaces, readCalcImplements(innerInterfaces, params));
    }

    private <T extends PropertyInterface> LCP addSGProp(AbstractGroup group, String name, boolean persistent, boolean notZero, String caption, ImOrderSet<T> innerInterfaces, ImList<CalcPropertyInterfaceImplement<T>> implement) {
        ImList<CalcPropertyInterfaceImplement<T>> listImplements = implement.subList(1, implement.size());
        SumGroupProperty<T> property = new SumGroupProperty<T>(name, caption, innerInterfaces.getSet(), listImplements.getCol(), implement.get(0));

        LCP lp = mapLGProp(group, persistent, property, listImplements);
        lp.listGroupInterfaces = innerInterfaces;
        return lp;
    }

    public <T extends PropertyInterface> LCP addOGProp(String caption, GroupType type, int numOrders, boolean descending, LCP<T> groupProp, Object... params) {
        return addOGProp(genSID(), false, caption, type, numOrders, descending, groupProp, params);
    }
    public <T extends PropertyInterface> LCP addOGProp(String name, boolean persist, String caption, GroupType type, int numOrders, boolean descending, LCP<T> groupProp, Object... params) {
        return addOGProp(null, name, persist, caption, type, numOrders, descending, groupProp, params);
    }
    public <T extends PropertyInterface> LCP addOGProp(AbstractGroup group, String name, boolean persist, String caption, GroupType type, int numOrders, boolean descending, LCP<T> groupProp, Object... params) {
        return addOGProp(group, name, persist, caption, type, numOrders, Settings.get().isDefaultOrdersNotNull(), descending, groupProp.listInterfaces, ListFact.add(((CalcProperty<T>) groupProp.property).getImplement(), readCalcImplements(groupProp.listInterfaces, params)));
    }
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


    protected LCP addMGProp(LCP groupProp, Object... params) {
        return addMGProp("sys", groupProp, params);
    }

    protected LCP addMGProp(String caption, LCP groupProp, Object... params) {
        return addMGProp(baseLM.privateGroup, genSID(), caption, groupProp, params);
    }

    protected LCP addMGProp(String name, String caption, LCP groupProp, Object... params) {
        return addMGProp(null, name, caption, groupProp, params);
    }

    protected LCP addMGProp(AbstractGroup group, String name, String caption, LCP groupProp, Object... params) {
        return addMGProp(group, name, false, caption, groupProp, params);
    }

    protected LCP addMGProp(AbstractGroup group, boolean persist, String caption, LCP groupProp, Object... params) {
        return addMGProp(groupProp, genSID(), persist, caption, groupProp, params);
    }

    protected LCP addMGProp(AbstractGroup group, String name, boolean persist, String caption, LCP groupProp, Object... params) {
        return addMGProp(group, name, persist, caption, false, groupProp, params);
    }

    protected LCP addMGProp(String caption, boolean min, LCP groupProp, Object... params) {
        return addMGProp(genSID(), false, caption, min, groupProp, params);
    }

    protected LCP addMGProp(String name, boolean persist, String caption, boolean min, LCP groupProp, Object... params) {
        return addMGProp(null, name, persist, caption, min, groupProp, params);
    }

    protected LCP addMGProp(AbstractGroup group, String name, boolean persist, String caption, boolean min, LCP groupProp, Object... params) {
        return addMGProp(group, name, persist, caption, min, groupProp.listInterfaces.size(), add(directLI(groupProp), params));
    }

    protected LCP addMGProp(AbstractGroup group, String name, boolean persist, String caption, boolean min, int interfaces, Object... params) {
        return addMGProp(group, persist, new String[]{name}, new String[]{caption}, 1, min, interfaces, params)[0];
    }

    protected <T extends PropertyInterface> LCP[] addMGProp(AbstractGroup group, String[] names, String[] captions, int extra, LCP<T> groupProp, Object... params) {
        return addMGProp(group, false, names, captions, extra, groupProp, params);
    }

    protected <T extends PropertyInterface> LCP[] addMGProp(AbstractGroup group, boolean persist, String[] names, String[] captions, int extra, LCP<T> groupProp, Object... params) {
        return addMGProp(group, persist, names, captions, extra + 1, false, groupProp.listInterfaces.size(), add(directLI(groupProp), params));
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

    protected <T extends PropertyInterface, P extends PropertyInterface> LCP addCGProp(AbstractGroup group, String name, String caption, LCP<T> groupProp, LCP<P> dataProp, Object... params) {
        return addCGProp(group, true, name, caption, groupProp, dataProp, params);
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LCP addCGProp(AbstractGroup group, String name, boolean persistent, String caption, LCP<T> groupProp, LCP<P> dataProp, Object... params) {
        return addCGProp(group, true, name, persistent, caption, groupProp, dataProp, params);
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LCP addCGProp(AbstractGroup group, boolean checkChange, String name, String caption, LCP<T> groupProp, LCP<P> dataProp, Object... params) {
        return addCGProp(group, checkChange, name, false, caption, groupProp, dataProp, params);
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LCP addCGProp(AbstractGroup group, boolean checkChange, String name, boolean persistent, String caption, LCP<T> groupProp, LCP<P> dataProp, Object... params) {
        return addCGProp(group, checkChange, name, persistent, caption, dataProp, groupProp.listInterfaces, ListFact.add(((CalcProperty<T>) groupProp.property).getImplement(), readCalcImplements(groupProp.listInterfaces, params)));
    }

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

    protected LCP addAGProp(String name, String caption, LCP... props) {
        return addAGProp(null, name, caption, props);
    }

    protected LCP addAGProp(String name, String caption, boolean noConstraint, LCP... props) {
        return addAGProp(null, name, caption, noConstraint, props);
    }

    protected LCP addAGProp(AbstractGroup group, String name, String caption, LCP... props) {
        return addAGProp(group, name, caption, false, props);
    }

    protected LCP addAGProp(AbstractGroup group, String name, String caption, boolean noConstraint, LCP... props) {
        ClassWhere<Integer> classWhere = ClassWhere.TRUE();
        for (LCP<?> prop : props)
            classWhere = classWhere.and(prop.getClassWhere());
        return addAGProp(group, name, caption, noConstraint, (CustomClass) classWhere.getCommonParent(SetFact.singleton(1)).singleValue(), props);
    }

    protected LCP addAGProp(String name, String caption, CustomClass customClass, LCP... props) {
        return addAGProp(null, name, caption, customClass, props);
    }

    protected LCP addAGProp(AbstractGroup group, String name, String caption, CustomClass customClass, LCP... props) {
        return addAGProp(group, name, caption, false, customClass, props);
    }

    protected LCP addAGProp(AbstractGroup group, String name, String caption, boolean noConstraint, CustomClass customClass, LCP... props) {
        return addAGProp(group, false, name, false, caption, noConstraint, customClass, props);
    }

    protected LCP addAGProp(AbstractGroup group, boolean checkChange, String name, boolean persistent, String caption, CustomClass customClass, LCP... props) {
        return addAGProp(group, checkChange, name, persistent, caption, false, customClass, props);
    }

    protected LCP addAGProp(AbstractGroup group, boolean checkChange, String name, boolean persistent, String caption, boolean noConstraint, CustomClass customClass, LCP... props) {
        if(props.length==1)
            ((CalcProperty)props[0].property).aggProp = true;
        return addAGProp(group, checkChange, name, persistent, caption, noConstraint, is(customClass), add(1, getUParams(props)));
    }

    protected <T extends PropertyInterface<T>> LCP addAGProp(String name, String caption, LCP<T> lp, int aggrInterface, Object... props) {
        return addAGProp(name, false, caption, lp, aggrInterface, props);
    }

    protected <T extends PropertyInterface> LCP addAGProp(AbstractGroup group, String name, String caption, LCP<T> lp, int aggrInterface, Object... props) {
        return addAGProp(group, false, name, false, caption, lp, aggrInterface, props);
    }

    protected <T extends PropertyInterface<T>> LCP addAGProp(String name, boolean persistent, String caption, LCP<T> lp, int aggrInterface, Object... props) {
        return addAGProp(null, false, name, persistent, caption, lp, aggrInterface, props);
    }

    protected <T extends PropertyInterface<T>> LCP addAGProp(AbstractGroup group, String name, boolean persistent, String caption, LCP<T> lp, int aggrInterface, Object... props) {
        return addAGProp(group, false, name, persistent, caption, lp, aggrInterface, props);
    }

    protected <T extends PropertyInterface<T>> LCP addAGProp(AbstractGroup group, boolean checkChange, String name, boolean persistent, String caption, LCP<T> lp, int aggrInterface, Object... props) {
        return addAGProp(group, checkChange, name, persistent, caption, false, lp, add(aggrInterface, props));
    }

    protected <T extends PropertyInterface<T>> LCP addAGProp(AbstractGroup group, boolean checkChange, String name, boolean persistent, String caption, boolean noConstraint, LCP<T> lp, Object... props) {
        ImList<CalcPropertyInterfaceImplement<T>> readImplements = readCalcImplements(lp.listInterfaces, props);
        T aggrInterface = (T) readImplements.get(0);
        ImList<CalcPropertyInterfaceImplement<T>> groupImplements = readImplements.subList(1, readImplements.size());
        ImList<CalcPropertyInterfaceImplement<T>> fullInterfaces = groupImplements.addList(lp.listInterfaces.removeOrderIncl(aggrInterface));
        return addAGProp(group, checkChange, name, persistent, caption, noConstraint, lp.listInterfaces, SetFact.toOrderExclSet(aggrInterface, lp.property.getImplement()).addList(fullInterfaces));
    }

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

    protected <T extends PropertyInterface, P extends PropertyInterface> LCP addDGProp(int orders, boolean ascending, LCP<T> groupProp, Object... params) {
        return addDGProp(baseLM.privateGroup, "sys", orders, ascending, groupProp, params);
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LCP addDGProp(AbstractGroup group, String caption, int orders, boolean ascending, LCP<T> groupProp, Object... params) {
        return addDGProp(group, genSID(), caption, orders, ascending, groupProp, params);
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LCP addDGProp(AbstractGroup group, String name, String caption, int orders, boolean ascending, LCP<T> groupProp, Object... params) {
        return addDGProp(group, name, false, caption, orders, ascending, groupProp, params);
    }

    protected <T extends PropertyInterface> LCP addDGProp(AbstractGroup group, String name, boolean persistent, String caption, int orders, boolean ascending, LCP<T> groupProp, Object... params) {
        return addDGProp(group, name, persistent, caption, orders, ascending, false, groupProp, params);
    }

    protected <T extends PropertyInterface> LCP addDGProp(AbstractGroup group, String name, boolean persistent, String caption, int orders, boolean ascending, boolean over, LCP<T> groupProp, Object... params) {
        ImList<CalcPropertyInterfaceImplement<T>> listImplements = readCalcImplements(groupProp.listInterfaces, params);
        int intNum = listImplements.size();
        LCP result = addSGProp(group, name, persistent, false, caption, groupProp, listImplements.subList(0, intNum - orders - 1));
        result.setDG(ascending, over, listImplements.subList(intNum - orders - 1, intNum));
        return result;
    }

    protected LCP addUProp(AbstractGroup group, String caption, Union unionType, String delimiter, int[] coeffs, Object... params) {
        return addUProp(group, genSID(), false, caption, unionType, null, coeffs, params);
    }

    protected LCP addUProp(AbstractGroup group, String name, boolean persistent, String caption, Union unionType, String delimiter, int[] coeffs, Object... params) {

        assert (unionType==Union.SUM)==(coeffs!=null);
        assert (unionType==Union.STRING_AGG)==(delimiter!=null);

        int intNum = getIntNum(params);
        ImOrderSet<UnionProperty.Interface> listInterfaces = UnionProperty.getInterfaces(intNum);
        ImList<CalcPropertyInterfaceImplement<UnionProperty.Interface>> listOperands = readCalcImplements(listInterfaces, params);

        UnionProperty property = null;
        switch (unionType) {
            case MAX:
                property = new MaxUnionProperty(name, caption, listInterfaces, listOperands.getCol());
                break;
            case SUM:
                MMap<CalcPropertyInterfaceImplement<UnionProperty.Interface>, Integer> mMapOperands = MapFact.mMap(MapFact.<CalcPropertyInterfaceImplement<UnionProperty.Interface>>addLinear());
                for(int i=0;i<listOperands.size();i++)
                    mMapOperands.add(listOperands.get(i), coeffs[i]);
                property = new SumUnionProperty(name, caption, listInterfaces, mMapOperands.immutable());
                break;
            case OVERRIDE:
                property = new CaseUnionProperty(name, caption, listInterfaces, listOperands, false);
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
            case STRING_AGG:
                property = new StringAggUnionProperty(name, caption, listInterfaces, listOperands, delimiter);
                break;
        }

        return addProperty(group, persistent, new LCP<UnionProperty.Interface>(property, listInterfaces));
    }

    protected LCP addAUProp(AbstractGroup group, String name, boolean isExclusive, boolean persistent, String caption, ValueClass valueClass, ValueClass... interfaces) {
        ImOrderSet<UnionProperty.Interface> listInterfaces = UnionProperty.getInterfaces(interfaces.length);
        return addProperty(group, persistent, new LCP<UnionProperty.Interface>(
                new CaseUnionProperty(name, true, !isExclusive, caption, listInterfaces, valueClass, listInterfaces.mapList(ListFact.toList(interfaces))), listInterfaces));
    }

    protected LCP addCaseUProp(AbstractGroup group, String name, boolean persistent, String caption, Object... params) {

        ImOrderSet<UnionProperty.Interface> listInterfaces = UnionProperty.getInterfaces(getIntNum(params));
        MList<CaseUnionProperty.Case> mListCases = ListFact.mList();
        ImList<CalcPropertyMapImplement<?,UnionProperty.Interface>> mapImplements = (ImList<CalcPropertyMapImplement<?, UnionProperty.Interface>>) (ImList<?>) readCalcImplements(listInterfaces, params);
        for (int i = 0; i < mapImplements.size() / 2; i++)
            mListCases.add(new CaseUnionProperty.Case(mapImplements.get(2 * i), mapImplements.get(2 * i + 1)));
        if (mapImplements.size() % 2 != 0)
            mListCases.add(new CaseUnionProperty.Case(new CalcPropertyMapImplement<PropertyInterface, UnionProperty.Interface>((CalcProperty<PropertyInterface>) baseLM.vtrue.property), mapImplements.get(mapImplements.size() - 1)));

        return addProperty(group, persistent, new LCP<UnionProperty.Interface>(new CaseUnionProperty(name, caption, listInterfaces, false, mListCases.immutableList()), listInterfaces));
    }

    // объединение классовое (непересекающихся) свойств

    protected LCP addCUProp(LCP... props) {
        return addCUProp(baseLM.privateGroup, "sys", props);
    }

    protected LCP addCUProp(String caption, LCP... props) {
        return addCUProp((AbstractGroup) null, caption, props);
    }

    protected LCP addCUProp(AbstractGroup group, String caption, LCP... props) {
        return addCUProp(group, genSID(), caption, props);
    }

    protected LCP addCUProp(String name, String caption, LCP... props) {
        return addCUProp(name, false, caption, props);
    }

    protected LCP addCUProp(String name, boolean persistent, String caption, LCP... props) {
        return addCUProp(null, name, persistent, caption, props);
    }

    protected LCP addCUProp(AbstractGroup group, String name, String caption, LCP... props) {
        return addCUProp(group, name, false, caption, props);
    }

    protected LCP addCUProp(AbstractGroup group, String name, boolean persistent, String caption, LCP... props) {
        assert baseLM.checkCUProps.add(props);
        return addXSUProp(group, name, persistent, caption, props);
    }

    // разница

    protected LCP addDUProp(LCP prop1, LCP prop2) {
        return addDUProp(baseLM.privateGroup, "sys", prop1, prop2);
    }

    protected LCP addDUProp(String caption, LCP prop1, LCP prop2) {
        return addDUProp((AbstractGroup) null, caption, prop1, prop2);
    }

    protected LCP addDUProp(AbstractGroup group, String caption, LCP prop1, LCP prop2) {
        return addDUProp(group, genSID(), caption, prop1, prop2);
    }

    protected LCP addDUProp(String name, String caption, LCP prop1, LCP prop2) {
        return addDUProp(null, name, caption, prop1, prop2);
    }

    protected LCP addDUProp(String name, boolean persistent, String caption, LCP prop1, LCP prop2) {
        return addDUProp(null, name, persistent, caption, prop1, prop2);
    }

    protected LCP addDUProp(AbstractGroup group, String name, String caption, LCP prop1, LCP prop2) {
        return addDUProp(group, name, false, caption, prop1, prop2);
    }

    protected LCP addDUProp(AbstractGroup group, String name, boolean persistent, String caption, LCP prop1, LCP prop2) {
        return addUProp(group, name, persistent, caption, Union.SUM, null, new int[]{1, -1}, getUParams(new LCP[]{prop1, prop2}));
    }

    protected LCP addNUProp(LCP prop) {
        return addNUProp(baseLM.privateGroup, genSID(), "sys", prop);
    }

    protected LCP addNUProp(AbstractGroup group, String name, String caption, LCP prop) {
        return addNUProp(group, name, false, caption, prop);
    }

    protected LCP addNUProp(AbstractGroup group, String name, boolean persistent, String caption, LCP prop) {
        int intNum = prop.listInterfaces.size();
        Object[] params = new Object[2 + intNum];
        params[0] = -1;
        params[1] = prop;
        for (int i = 0; i < intNum; i++)
            params[2 + i] = i + 1;
        return addUProp(group, name, persistent, caption, Union.SUM, null, new int[]{-1}, getUParams(new LP[]{prop}));
    }

    public LCP addLProp(SystemEventsLogicsModule systemEventsLM, LCP lp, ValueClass... classes) {
        return addDCProp("LG_" + lp.property.getSID(), ServerResourceBundle.getString("logics.log") + " " + lp.property, 1, lp,
                add(new Object[]{true}, add(getParams(lp), add(new Object[]{addJProp(baseLM.equals2, 1, systemEventsLM.currentSession), lp.listInterfaces.size() + 1}, add(directLI(lp), classes)))));
    }

    // XOR

    protected LCP addXorUProp(LCP prop1, LCP prop2) {
        return addXorUProp(baseLM.privateGroup, genSID(), "sys", prop1, prop2);
    }

    protected LCP addXorUProp(AbstractGroup group, String name, String caption, LCP prop1, LCP prop2) {
        return addXorUProp(group, name, false, caption, prop1, prop2);
    }

    protected LCP addXorUProp(AbstractGroup group, String name, boolean persistent, String caption, LCP... props) {
        return addUProp(group, name, persistent, caption, Union.XOR, null, null, getUParams(props));
//        int intNum = prop1.listInterfaces.size();
//        Object[] params = new Object[2 * (1 + intNum)];
//        params[0] = prop1;
//        for (int i = 0; i < intNum; i++)
//            params[1 + i] = i + 1;
//        params[1 + intNum] = prop2;
//        for (int i = 0; i < intNum; i++)
//            params[2 + intNum + i] = i + 1;
//        return addXSUProp(group, name, persistent, caption, addJProp(andNot1, getUParams(new LCP[]{prop1, prop2}, 0)), addJProp(andNot1, getUParams(new LCP[]{prop2, prop1}, 0)));
    }

    // IF и IF ELSE

    protected LCP addIfProp(LCP prop, boolean not, LCP ifProp, Object... params) {
        return addIfProp(baseLM.privateGroup, genSID(), "sys", prop, not, ifProp, params);
    }

    protected LCP addIfProp(AbstractGroup group, String name, String caption, LCP prop, boolean not, LCP ifProp, Object... params) {
        return addIfProp(group, name, false, caption, prop, not, ifProp, params);
    }

    protected LCP addIfProp(AbstractGroup group, String name, boolean persistent, String caption, LCP prop, boolean not, LCP ifProp, Object... params) {
        return addJProp(group, name, persistent, caption, and(not), add(getUParams(new LCP[]{prop}), add(new LCP[]{ifProp}, params)));
    }

    protected LCP addIfElseUProp(LCP prop1, LCP prop2, LCP ifProp, Object... params) {
        return addIfElseUProp(baseLM.privateGroup, "sys", prop1, prop2, ifProp, params);
    }

    protected LCP addIfElseUProp(AbstractGroup group, String caption, LCP prop1, LCP prop2, LCP ifProp, Object... params) {
        return addIfElseUProp(group, genSID(), caption, prop1, prop2, ifProp, params);
    }

    protected LCP addIfElseUProp(AbstractGroup group, String name, String caption, LCP prop1, LCP prop2, LCP ifProp, Object... params) {
        return addIfElseUProp(group, name, false, caption, prop1, prop2, ifProp, params);
    }

    protected LCP addIfElseUProp(AbstractGroup group, String name, boolean persistent, String caption, LCP prop1, LCP prop2, LCP ifProp, Object... params) {
        return addXSUProp(group, name, persistent, caption, addIfProp(prop1, false, ifProp, params), addIfProp(prop2, true, ifProp, params));
    }

    // объединение пересекающихся свойств

    public LCP addSUProp(Union unionType, LCP... props) {
        return addSUProp(baseLM.privateGroup, "sys", unionType, props);
    }

    protected LCP addSUProp(String caption, Union unionType, LCP... props) {
        return addSUProp((AbstractGroup) null, caption, unionType, props);
    }

    protected LCP addSUProp(AbstractGroup group, String caption, Union unionType, LCP... props) {
        return addSUProp(group, genSID(), caption, unionType, props);
    }

    protected LCP addSUProp(String name, String caption, Union unionType, LCP... props) {
        return addSUProp(name, false, caption, unionType, props);
    }

    protected LCP addSUProp(String name, boolean persistent, String caption, Union unionType, LCP... props) {
        return addSUProp(null, name, persistent, caption, unionType, props);
    }

    // объединяет разные по классам св-ва

    protected LCP addSUProp(AbstractGroup group, String name, String caption, Union unionType, LCP... props) {
        return addSUProp(group, name, false, caption, unionType, props);
    }

    protected LCP addSUProp(AbstractGroup group, String name, boolean persistent, String caption, Union unionType, LCP... props) {
        assert baseLM.checkSUProps.add(props);
        return addUProp(group, name, persistent, caption, unionType, null, (unionType == Union.SUM ? BaseUtils.genArray(1, props.length) : null), getUParams(props));
    }

    protected LCP addSFUProp(AbstractGroup group, String name, String caption, String delimiter, LCP... props) {
        return addSFUProp(group, name, false, caption, delimiter, props);
    }
    protected LCP addSFUProp(AbstractGroup group, String name, boolean persistent, String caption, String delimiter, LCP... props) {
        return addUProp(group, name, persistent, caption, Union.STRING_AGG, delimiter, null, getUParams(props));
    }

    protected LCP addSFUProp(String name, String delimiter, int intNum) {
        return addUProp(null, name, false, ServerResourceBundle.getString("logics.join"), Union.STRING_AGG, delimiter, null, getUParams(intNum));
    }

    protected LCP addXSUProp(AbstractGroup group, String caption, LCP... props) {
        return addXSUProp(group, genSID(), caption, props);
    }

    // объединяет заведомо непересекающиеся но не классовые свойства

    protected LCP addXSUProp(AbstractGroup group, String name, String caption, LCP... props) {
        return addXSUProp(group, name, false, caption, props);
    }

    protected LCP addXSUProp(AbstractGroup group, String name, boolean persistent, String caption, LCP... props) {
        return addUProp(group, name, persistent, caption, Union.EXCLUSIVE, null, null, getUParams(props));
    }

    protected LCP[] addMUProp(AbstractGroup group, String[] names, String[] captions, int extra, LCP... props) {
        int propNum = props.length / (1 + extra);
        LCP[] maxProps = copyOfRange(props, 0, propNum);

        LCP[] result = new LCP[extra + 1];
        int i = 0;
        do {
            result[i] = addSUProp(group, names[i], captions[i], Union.MAX, maxProps);
            if (i < extra) { // если не последняя
                for (int j = 0; j < propNum; j++)
                    maxProps[j] = addJProp(baseLM.and1, add(directLI(props[(i + 1) * propNum + j]), directLI( // само свойство
                            addJProp(baseLM.equals2, add(directLI(maxProps[j]), directLI(result[i])))))); // только те кто дает предыдущий максимум
            }
        } while (i++ < extra);
        return result;
    }

    public LAP addAProp(ActionProperty property) {
        return addAProp(baseLM.privateGroup, property);
    }

    public LAP addAProp(AbstractGroup group, ActionProperty property) {
        return addProperty(group, new LAP(property));
    }

    @IdentityStrongLazy // для ID
    protected LAP<?> addSAProp(LCP lp) {
        return addProperty(null, new LAP(new SeekActionProperty(baseLM.baseClass, lp == null ? null : (CalcProperty) lp.property)));
    }

    protected LAP addMAProp(String message, String title) {
        return addMAProp(null, message, title);
    }

    protected LAP addMAProp(AbstractGroup group, String message, String title) {
        return addMAProp(group, "", title, addCProp(StringClass.get(message.length()), message));
    }

    protected LAP addMAProp(String title, Object... params) {
        return addMAProp(null, "", title, params);
    }

    protected LAP addMAProp(AbstractGroup group, String caption, String title, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<PropertyInterfaceImplement<PropertyInterface>> readImplements = readImplements(listInterfaces, params);

        assert readImplements.size() == 1;

        return addProperty(group, new LAP(
                new MessageActionProperty(genSID(), caption, title, listInterfaces, (CalcPropertyMapImplement<?, PropertyInterface>) readImplements.get(0))));
    }

    protected LAP addConfirmAProp(String message, String title) {
        return addConfirmAProp(null, message, title);
    }

    protected LAP addConfirmAProp(AbstractGroup group, String message, String title) {
        return addConfirmAProp(group, "", title, addCProp(StringClass.get(message.length()), message));
    }

    protected LAP addConfirmAProp(String title, Object... params) {
        return addConfirmAProp(null, "", title, params);
    }

    protected LAP addConfirmAProp(AbstractGroup group, String caption, String title, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<PropertyInterfaceImplement<PropertyInterface>> readImplements = readImplements(listInterfaces, params);

        assert readImplements.size() == 1;

        return addProperty(group, new LAP(
                new ConfirmActionProperty(genSID(), caption, title, listInterfaces, (CalcPropertyMapImplement<?, PropertyInterface>) readImplements.get(0), getConfirmedProperty())));
    }

    protected LAP addAsyncUpdateAProp(Object... params) {
        return addAsyncUpdateAProp("", params);
    }

    protected LAP addAsyncUpdateAProp(String caption, Object... params) {
        return addAsyncUpdateAProp(null, caption, params);
    }

    protected LAP addAsyncUpdateAProp(AbstractGroup group, String caption, Object... params) {
        ImOrderSet<PropertyInterface> listInterfaces = genInterfaces(getIntNum(params));
        ImList<PropertyInterfaceImplement<PropertyInterface>> readImplements = readImplements(listInterfaces, params);

        assert readImplements.size() == 1;

        return addProperty(group, new LAP(new AsyncUpdateEditValueAction(genSID(), caption, listInterfaces, (CalcPropertyMapImplement<?, PropertyInterface>) readImplements.get(0))));
    }

    protected LAP addLFAProp(LCP lp) {
        return addLFAProp(null, "lfa", lp);
    }

    protected LAP addLFAProp(AbstractGroup group, String caption, LCP lp) {
        return addProperty(group, new LAP(new LoadActionProperty(genSID(), caption, lp)));
    }

    protected LAP addOFAProp(LCP lp) {
        return addOFAProp(null, "ofa", lp);
    }

    protected LAP addOFAProp(AbstractGroup group, String caption, LCP lp) { // обернем сразу в and
        return addProperty(group, new LAP(new OpenActionProperty(genSID(), caption, lp)));
    }

    protected LAP addAAProp(ConcreteCustomClass customClass, LCP... properties) {
        return addAAProp(null, customClass, null, properties);
    }

    protected LAP addAAProp(String caption, ConcreteCustomClass customClass, LCP... properties) {
        return addAAProp(caption, null, customClass, null, properties);
    }

    protected LAP addAAProp(LCP barcode, ConcreteCustomClass customClass, LCP barcodePrefix, LCP... properties) {
        return addAAProp(ServerResourceBundle.getString("logics.add"), barcode, customClass, barcodePrefix, properties);
    }

    protected LAP addAAProp(String caption, LCP<?> barcode, ConcreteCustomClass customClass, LCP<?> barcodePrefix, LCP... properties) {
        PropertyInterface addedInterface = new PropertyInterface();

        MList<ActionPropertyMapImplement<?, PropertyInterface>> mList = ListFact.mList();

        // генерация штрихкода
        if(barcode!=null)
            mList.add(new ChangeBarcodeActionProperty(baseClass, barcode.property, barcodePrefix != null ? barcodePrefix.property : null).getImplement(SetFact.singletonOrder(addedInterface)));

        MCol<CalcPropertyMapImplement<?, PropertyInterface>> mCheckClasses = ListFact.mCol();
        MOrderExclSet<PropertyInterface> mInnerInterfaces = SetFact.mOrderExclSet(properties.length);
        for(LCP<?> lp : properties) {
            PropertyInterface genInterface = new PropertyInterface();
            ImSet<PropertyInterface> setInterfaces = SetFact.toExclSet(genInterface, addedInterface);
            mList.add(createSetAction(setInterfaces, lp.getImplement(addedInterface), genInterface));
            mInnerInterfaces.exclAdd(genInterface);

            ValueClass valueClass = lp.property.getValueClass();
            if(valueClass instanceof CustomClass) {
                mCheckClasses.add(((CustomClass)valueClass).getProperty().getImplement(SetFact.singletonOrder(genInterface)));
            }
        }
        ImOrderSet<PropertyInterface> innerInterfaces = mInnerInterfaces.immutableOrder();
        ImCol<CalcPropertyMapImplement<?, PropertyInterface>> checkClasses = mCheckClasses.immutableCol();

        mList.add(addSAProp(null).getImplement(addedInterface));

        ImSet<PropertyInterface> addedInnerInterfaces = innerInterfaces.getSet().addExcl(addedInterface);
        ActionPropertyMapImplement<?, PropertyInterface> result = createForAction(addedInnerInterfaces, innerInterfaces.getSet(), createListAction(addedInnerInterfaces, mList.immutableList()), addedInterface, customClass, false);
        if(checkClasses.size() > 0)
            result = createIfAction(innerInterfaces.getSet(), createAnd(checkClasses), result, null);
        return mapLAProp(null, result, innerInterfaces);
    }

    public LAP addEvalAProp(LCP<?> scriptSource) {
        return addAProp(null, new EvalActionProperty(genSID(), "", scriptSource));
    }

    public LAP getAddObjectAction(CustomClass cls, boolean forceDialog, CalcProperty storeNewObjectProperty) {
        return addAProp(null, new AddObjectActionProperty(genSID(), cls, forceDialog, storeNewObjectProperty));
    }

    public <T extends PropertyInterface, I extends PropertyInterface> LAP getAddObjectAction(CustomClass cls, boolean forceDialog, int resInterfaces, boolean conditional, boolean resultExists, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(getIntNum(params));
        ImList<CalcPropertyInterfaceImplement<PropertyInterface>> readImplements = readCalcImplements(innerInterfaces, params);
        CalcPropertyMapImplement<T, PropertyInterface> resultPart = (CalcPropertyMapImplement<T, PropertyInterface>)
                (resultExists ? readImplements.get(resInterfaces) : null);
        CalcPropertyMapImplement<T, PropertyInterface> conditionalPart = (CalcPropertyMapImplement<T, PropertyInterface>)
                (conditional ? readImplements.get(resInterfaces + (resultExists ? 1 : 0)) : DerivedProperty.createTrue());

        return addAProp(null, new AddObjectActionProperty(genSID(), cls, forceDialog, innerInterfaces.getSet(), (ImOrderSet) readImplements.subList(0, resInterfaces).toOrderExclSet(), conditionalPart, resultPart));
    }

    @IdentityStrongLazy // data
    public SessionDataProperty getAddedObjectProperty() {
        SessionDataProperty addedObject = new SessionDataProperty("addedObject", "Added Object", baseLM.baseClass);
        addProperty(null, new LCP<ClassPropertyInterface>(addedObject));
        return addedObject;
    }

    @IdentityStrongLazy // data
    public LCP getConfirmedProperty() {
        return addProperty(null, new LCP<ClassPropertyInterface>(new SessionDataProperty("confirmed", "Confirmed", LogicalClass.instance)));
    }

    @IdentityStrongLazy // data
    public AnyValuePropertyHolder getChosenValueProperty() {
        return addAnyValuePropertyHolder("chosen", "Chosen", StringClass.get(100));
    }

    @IdentityStrongLazy // data
    public AnyValuePropertyHolder getRequestedValueProperty() {
        return addAnyValuePropertyHolder("requested", "Requested");
    }

    public AnyValuePropertyHolder addAnyValuePropertyHolder(String sidPrefix, String captionPrefix, ValueClass... classes) {
        return new AnyValuePropertyHolder(
                addProperty(null, new LCP<ClassPropertyInterface>(new SessionDataProperty(sidPrefix + "Object", captionPrefix + " Object", classes, baseLM.baseClass))),
                addProperty(null, new LCP<ClassPropertyInterface>(new SessionDataProperty(sidPrefix + "Text", captionPrefix + " Text", classes, TextClass.instance))),
                addProperty(null, new LCP<ClassPropertyInterface>(new SessionDataProperty(sidPrefix + "String", captionPrefix + " String", classes, StringClass.get(2000)))),
                addProperty(null, new LCP<ClassPropertyInterface>(new SessionDataProperty(sidPrefix + "Integer", captionPrefix + " Integer", classes, IntegerClass.instance))),
                addProperty(null, new LCP<ClassPropertyInterface>(new SessionDataProperty(sidPrefix + "Long", captionPrefix + " Long", classes, LongClass.instance))),
                addProperty(null, new LCP<ClassPropertyInterface>(new SessionDataProperty(sidPrefix + "Double", captionPrefix + " Double", classes, DoubleClass.instance))),
                addProperty(null, new LCP<ClassPropertyInterface>(new SessionDataProperty(sidPrefix + "Numeric", captionPrefix + " Numeric", classes, NumericClass.get(20, 7)))),
                addProperty(null, new LCP<ClassPropertyInterface>(new SessionDataProperty(sidPrefix + "Year", captionPrefix + " Year", classes, YearClass.instance))),
                addProperty(null, new LCP<ClassPropertyInterface>(new SessionDataProperty(sidPrefix + "DateTime", captionPrefix + " DateTime", classes, DateTimeClass.instance))),
                addProperty(null, new LCP<ClassPropertyInterface>(new SessionDataProperty(sidPrefix + "Logical", captionPrefix + " Logical", classes, LogicalClass.instance))),
                addProperty(null, new LCP<ClassPropertyInterface>(new SessionDataProperty(sidPrefix + "Date", captionPrefix + " Date", classes, DateClass.instance))),
                addProperty(null, new LCP<ClassPropertyInterface>(new SessionDataProperty(sidPrefix + "Time", captionPrefix + " Time", classes, TimeClass.instance))),
                addProperty(null, new LCP<ClassPropertyInterface>(new SessionDataProperty(sidPrefix + "Color", captionPrefix + " Color", classes, ColorClass.instance))),
                addProperty(null, new LCP<ClassPropertyInterface>(new SessionDataProperty(sidPrefix + "WordFile", captionPrefix + " Word file", classes, WordClass.get(false, false)))),
                addProperty(null, new LCP<ClassPropertyInterface>(new SessionDataProperty(sidPrefix + "ImageFile", captionPrefix + " Image file", classes, ImageClass.get(false, false)))),
                addProperty(null, new LCP<ClassPropertyInterface>(new SessionDataProperty(sidPrefix + "PdfFile", captionPrefix + " Pdf file", classes, PDFClass.get(false, false)))),
                addProperty(null, new LCP<ClassPropertyInterface>(new SessionDataProperty(sidPrefix + "CustomFile", captionPrefix + " Custom file", classes, DynamicFormatFileClass.get(false, false)))),
                addProperty(null, new LCP<ClassPropertyInterface>(new SessionDataProperty(sidPrefix + "ExcelFile", captionPrefix + " Excel file", classes, ExcelClass.get(false, false))))
        );
    }

    @IdentityStrongLazy // data
    public LCP getRequestCanceledProperty() {
        return addProperty(null, new LCP<ClassPropertyInterface>(new SessionDataProperty("requestCanceled", "Request Input Canceled", LogicalClass.instance)));
    }

    @IdentityStrongLazy // data
    public LCP getFormResultProperty() {
        return addProperty(null, new LCP<ClassPropertyInterface>(new SessionDataProperty("formResult", "Form Result", baseLM.formResult)));
    }

    public LAP getFormAddObjectAction(FormEntity formEntity, ObjectEntity obj) {
        return getFormAddObjectAction(formEntity, obj, false);
    }

    public LAP getFormAddObjectAction(FormEntity formEntity, ObjectEntity obj, boolean forceDialog) {
        return getFormAddObjectAction(formEntity, obj, null, forceDialog);
    }

    @IdentityStrongLazy
    public LAP getFormAddObjectAction(FormEntity formEntity, ObjectEntity obj, CustomClass forceClass, boolean forceDialog) { // добавляем и делаем resolveAdd для добавленного объекта
        String sid = "add" + (formEntity==null ? "" : BaseUtils.capitalize(formEntity.getSID())) + BaseUtils.capitalize(obj.getSID())
                + (forceClass==null ? "" : BaseUtils.capitalize(forceClass.getSID())) + (forceDialog ? "Force" : "");
        return addAProp(new FormAddObjectActionProperty(sid, forceClass!=null ? forceClass : (CustomClass)obj.baseClass, forceDialog, obj));
//        PropertyInterface added = new PropertyInterface();
//        return addAProp(null, createForAction(Collections.singleton(added), new ArrayList<PropertyInterface>(),
//                new ResolveAddObjectActionProperty(genSID(), obj).getImplement(added), added, forceClass, forceDialog, false).property);
    }

    protected LAP getFormAddObjectActionWithClassCheck(FormEntity formEntity, ObjectEntity objectEntity, ValueClass checkClass) {
        LAP addObjectAction = getFormAddObjectAction(formEntity, objectEntity);
        return addIfAProp(addObjectAction.property.caption, is(checkClass), 1, addObjectAction);
    }

    public LAP getScriptAddFormAction(CustomClass cls, boolean session) {
        ClassFormEntity form = cls.getEditForm(baseLM);

        LAP property = addDMFAProp(null, genSID(), ServerResourceBundle.getString("logics.add"),
                form.form, new ObjectEntity[] {},
                form.form.addPropertyObject(getFormAddObjectAction(form.form, form.object, cls, false)), !session);
        setAddFormActionProperties(property, form, session);
        return property;
    }

    @IdentityStrongLazy // для ID
    public LAP getAddFormAction(CustomClass cls, boolean oldSession) {
        ClassFormEntity form = cls.getEditForm(baseLM);

        String sid = "add" + BaseUtils.capitalize(form.form.getSID()) + BaseUtils.capitalize(form.object.getSID())
                + (oldSession ? "Session" : "") + "Form" + BaseUtils.capitalize(cls.getSID());

        LAP property = addDMFAProp(publicGroup, sid, ServerResourceBundle.getString("logics.add"), //+ "(" + cls + ")",
                form.form, new ObjectEntity[]{},
                form.form.addPropertyObject(getFormAddObjectAction(form.form, form.object, cls, false)), !oldSession);
        setAddFormActionProperties(property, form, oldSession);
        return property;
    }

    private void setAddFormActionProperties(LAP property, ClassFormEntity form, boolean oldSession) {
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
            formAction.closeAction = form.form.addPropertyObject(baseLM.delete, form.object);
        }
    }

    public LAP getScriptEditFormAction(CustomClass cls, boolean oldSession) {
        ClassFormEntity form = cls.getEditForm(baseLM);
        LAP property = addDMFAProp(null, genSID(), ServerResourceBundle.getString("logics.edit"), form.form, new ObjectEntity[]{form.object}, !oldSession);
        setEditFormActionProperties(property);
        return property;
    }

    @IdentityStrongLazy // для ID
    public LAP getEditFormAction(CustomClass cls, boolean oldSession) {
        ClassFormEntity form = cls.getEditForm(baseLM);
        LAP property = addDMFAProp(publicGroup, "edit" + (oldSession ? "Session" : "") + "Form" + BaseUtils.capitalize(cls.getSID()), ServerResourceBundle.getString("logics.edit"), // + "(" + cls + ")",
                form.form, new ObjectEntity[]{form.object}, !oldSession);
        setEditFormActionProperties(property);
        return property;
    }

    private void setEditFormActionProperties(LAP property) {
        property.setImage("edit.png");
        property.setShouldBeLast(true);
        property.setEditKey(KeyStrokes.getEditActionPropertyKeyStroke());
        property.setShowEditKey(false);
        property.setDrawToToolbar(true);
        property.setForceViewType(ClassViewType.PANEL);
    }

    @IdentityStrongLazy
    public LAP getSimpleDeleteAction(ConcreteCustomClass cls, boolean oldSession) {
        LAP res = addChangeClassAProp(baseClass.unknown, 1, 0, false, true, 1, is(cls), 1);
        if (!oldSession) {
            res = (LAP) addNewSessionAProp(null, genSID(), res.property.caption, res, true, false, SetFact.<SessionDataProperty>EMPTY(), SetFact.<SessionDataProperty>EMPTY());
            res.setAskConfirm(true);
        }
        setDeleteActionOptions(res);
        return res;
    }

    protected void setDeleteActionOptions(LAP property) {
        property.setImage("delete.png");
        property.setShouldBeLast(true);
        property.setEditKey(KeyStrokes.getDeleteActionPropertyKeyStroke());
        property.setShowEditKey(false);
    }

    protected LCP addHideCaptionProp(LCP hideProperty) {
        return addHideCaptionProp(privateGroup, "hideCaption", hideProperty);
    }

    /**
     * Нужно для скрытия свойств при соблюдении какого-то критерия
     * <p/>
     * <pre>
     * Пример использования:
     *       Скроем свойство descriptionPolicy, если у текущего user'а логин - "Admin"
     *
     *       Вводим свойство критерия:
     *
     *         LP hideUserPolicyDescription = addJProp(diff2, userLogin, 1, addCProp(StringClass.get(30), "Admin"));
     *
     *       Вводим свойство которое будет использовано в качестве propertyCaption для descriptionPolicy:
     *
     *         policyDescriptorCaption = addHideCaptionProp(null, "Policy caption", descriptionPolicy, hideUserPolicyDescription);
     *
     *       Далее в форме указываем соответсвующий propertyCaption:
     *
     *         PropertyDrawEntity descriptionDraw = getPropertyDraw(descriptionPolicy, objPolicy.groupTo);
     *         PropertyDrawEntity descriptorCaptionDraw = addPropertyDraw(policyDescriptorCaption, objUser);
     *         descriptionDraw.setPropertyCaption(descriptorCaptionDraw.propertyObject);
     * </pre>
     *
     *
     * @param group        ...
     * @param caption      ...
     * @param hideProperty критерий
     * @return свойство, которое должно использоваться в качестве propertyCaption для скрываемого свойства
     */
    protected LCP addHideCaptionProp(AbstractGroup group, String caption, LCP hideProperty) {
        String captionOriginal = GlobalConstants.CAPTION_ORIGINAL;
        LCP originalCaption = addCProp(StringClass.get(captionOriginal.length()), captionOriginal);
        return addJProp(group, caption, baseLM.and1, add(new Object[]{originalCaption}, directLI(hideProperty)));
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

    private <T extends LP<?, ?>> T addProperty(AbstractGroup group, boolean persistent, T lp) {
        setPropertySID(lp, lp.property.getSID(), true);
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

    protected <T extends LP<?, ?>> void setPropertySID(T lp, String name, boolean generated) {
        String oldSID = lp.property.getSID();
        lp.property.setName(name);
        String newSID = transformNameToSID(name);
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

    @IdentityStrongLazy // для ID
    public <T extends PropertyInterface> LCP<T> addOldProp(LCP<T> lp) {
        return addProperty(null, new LCP<T>(lp.property.getOld(), lp.listInterfaces));
    }

    @IdentityStrongLazy // для ID
    public <T extends PropertyInterface> LCP<T> addCHProp(LCP<T> lp, IncrementType type) {
        addProperty(null, new LCP(lp.property.getOld(), lp.listInterfaces));
        return addProperty(null, new LCP<T>(lp.property.getChanged(type), lp.listInterfaces));
    }

    @IdentityStrongLazy // для ID
    public <T extends PropertyInterface> LCP addClassProp(LCP<T> lp) {
        return mapLProp(null, false, lp.property.getClassProperty().cloneProp(), lp);
    }

    public void addConstraint(CalcProperty property, boolean checkChange) {
        addConstraint(addProp(property), checkChange);
    }

    public void addConstraint(LCP<?> lp, boolean checkChange) {
        addConstraint(lp, (checkChange ? CalcProperty.CheckType.CHECK_ALL : CalcProperty.CheckType.CHECK_NO), null, Event.APPLY, this);
    }

    protected void addConstraint(LCP<?> lp, CalcProperty.CheckType type, ImSet<CalcProperty<?>> checkProps, Event event, LogicsModule lm) {
        if(!((CalcProperty)lp.property).noDB())
            lp = addCHProp(lp, IncrementType.SET);
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
        constraintAction.mapEventAction(this, DerivedProperty.createAnyGProp(property).getImplement(), event, false);
        addProp(constraintAction.property);
    }

    public <T extends PropertyInterface> void addEventAction(Event event, boolean descending, boolean ordersNotNull, ImSet<CalcProperty> prevStart, int noInline, boolean forceInline, Object... params) {
        ImOrderSet<PropertyInterface> innerInterfaces = genInterfaces(getIntNum(params));

        ImList<PropertyInterfaceImplement<PropertyInterface>> listImplements = readImplements(innerInterfaces, params);
        int implCnt = listImplements.size();

        ImOrderMap<CalcPropertyInterfaceImplement<PropertyInterface>, Boolean> orders = BaseUtils.immutableCast(listImplements.subList(2, implCnt - noInline).toOrderSet().toOrderMap(descending));

        ImSet<PropertyInterface> noInlineInterfaces = BaseUtils.<ImList<PropertyInterface>>immutableCast(listImplements.subList(implCnt - noInline, implCnt)).toOrderExclSet().getSet();

        addEventAction(innerInterfaces.getSet(), (ActionPropertyMapImplement<?, PropertyInterface>) listImplements.get(0), (CalcPropertyMapImplement<?, PropertyInterface>) listImplements.get(1), orders, ordersNotNull, event, prevStart, noInlineInterfaces, forceInline, false);
    }

    public <P extends PropertyInterface, D extends PropertyInterface> void addEventAction(ActionProperty<P> actionProperty, CalcPropertyMapImplement<?, P> whereImplement, ImOrderMap<CalcPropertyInterfaceImplement<P>, Boolean> orders, boolean ordersNotNull, Event event, boolean resolve) {
        addEventAction(actionProperty.interfaces, actionProperty.getImplement(), whereImplement, orders, ordersNotNull, event, SetFact.<CalcProperty>EMPTY(), SetFact.<P>EMPTY(), false, resolve);
    }

    public <P extends PropertyInterface, D extends PropertyInterface> void addEventAction(ImSet<P> innerInterfaces, ActionPropertyMapImplement<?, P> actionProperty, CalcPropertyMapImplement<?, P> whereImplement, ImOrderMap<CalcPropertyInterfaceImplement<P>, Boolean> orders, boolean ordersNotNull, Event event, ImSet<CalcProperty> prevStart, ImSet<P> noInline, boolean forceInline, boolean resolve) {
        if(!((CalcProperty)whereImplement.property).noDB())
            whereImplement = whereImplement.mapChanged(IncrementType.SET);

        if(prevStart==null)
            prevStart = actionProperty.mapParseOldDepends().mapSetValues(new GetValue<CalcProperty, OldProperty>() {
                public CalcProperty getMapValue(OldProperty value) {
                    return value.property;
                }});

        ActionProperty<? extends PropertyInterface> action =
                innerInterfaces.isEmpty() ?
                    DerivedProperty.createIfAction(innerInterfaces, whereImplement, actionProperty, null).property :
                    DerivedProperty.createForAction(innerInterfaces, SetFact.<P>EMPTY(), whereImplement, orders, ordersNotNull, actionProperty, null, false, noInline, forceInline).property;

        action.setStrongUsed(whereImplement.property); // добавить сильную связь
//        action.caption = "WHEN " + whereImplement.property + " " + actionProperty;
        addProp(action);

        addBaseEvent(action, event, resolve, false, prevStart);
    }

    public <P extends PropertyInterface> void addBaseEvent(ActionProperty<P> action, Event event, boolean resolve, boolean single, ImSet<CalcProperty> prevStart) {
        action.addEvent(event.base, event.session);
        action.singleApply = single;
        action.prevStart = prevStart;
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
        CalcPropertyMapImplement<L, T> mapClasses = (CalcPropertyMapImplement<L, T>) IsClassProperty.getMapProperty(property.getInterfaceClasses());
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
                setAction.mapEventAction(this, DerivedProperty.createAndNot(property.getChanged(IncrementType.SET), implement), event, true);
            }
        }
        if((options & PropertyFollows.RESOLVE_FALSE)!=0) {
            ActionPropertyMapImplement<?, T> setAction = property.getSetNotNullAction(false);
            if(setAction!=null) {
//                setAction.property.caption = "RESOLVE FALSE : " + property + " => " + implement.property;
                setAction.mapEventAction(this, DerivedProperty.createAnd(property, implement.mapChanged(IncrementType.DROP)), event, true);
            }
        }

        CalcProperty constraint = DerivedProperty.createAndNot(property, implement).property;
        constraint.caption = caption;
        addConstraint(constraint, false);
    }


    protected void followed(LCP first, LCP... lps) {
        for (LCP lp : lps) {
            Integer[] mapping = new Integer[lp.listInterfaces.size()];
            for (int i = 0; i < mapping.length; i++) {
                mapping[i] = i + 1;
            }
            follows(lp, first, mapping);
        }
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

    @IdentityStrongLazy // для ID
    protected LCP dumb(int interfaces) {
        ValueClass params[] = new ValueClass[interfaces];
        for (int i = 0; i < interfaces; ++i) {
            params[i] = baseLM.baseClass;
        }
        return addCProp(privateGroup, "dumb" + interfaces, false, "dumbProperty" + interfaces, StringClass.get(1), "", params);
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
        addObjectActions(form, object, false);
    }

    protected void addObjectActions(FormEntity form, ObjectEntity object, ObjectEntity checkObject) {
        addObjectActions(form, object, checkObject, null);
    }

    protected void addObjectActions(FormEntity form, ObjectEntity object, ObjectEntity checkObject, ValueClass checkObjectClass) {
        addObjectActions(form, object, false, true, checkObject, checkObjectClass);
    }

    public void addObjectActions(FormEntity form, ObjectEntity object, boolean actionImport) {
        addObjectActions(form, object, actionImport, true);
    }

    public void addObjectActions(FormEntity form, ObjectEntity object, boolean actionImport, boolean shouldBeLast) {
        addObjectActions(form, object, actionImport, shouldBeLast, null, null);
    }

    protected void addObjectActions(FormEntity form, ObjectEntity object, boolean actionImport, boolean shouldBeLast, ObjectEntity checkObject, ValueClass checkObjectClass) {

        PropertyDrawEntity actionAddPropertyDraw;
        if (checkObject == null) {
            actionAddPropertyDraw = form.addPropertyDraw(getFormAddObjectAction(form, object));
        } else {
            actionAddPropertyDraw = form.addPropertyDraw(
                    getFormAddObjectActionWithClassCheck(form, object, checkObjectClass != null ? checkObjectClass : checkObject.baseClass),
                    checkObject);

            actionAddPropertyDraw.forceViewType = ClassViewType.PANEL;
        }

        actionAddPropertyDraw.shouldBeLast = shouldBeLast;
        actionAddPropertyDraw.toDraw = object.groupTo;

        form.addPropertyDraw(baseLM.delete, object).shouldBeLast = shouldBeLast;
    }

    public void addFormActions(FormEntity form, ObjectEntity object) {
        addFormActions(form, object, false);
    }

    public void addFormActions(FormEntity form, ObjectEntity object, boolean session) {
        addAddFormAction(form, object, session);
        addEditFormAction(form, object, session);
        form.addPropertyDraw(baseLM.delete, object);
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
        return form.addPropertyDraw(getSimpleDeleteAction((ConcreteCustomClass) object.baseClass, oldSession), object);
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public List<String> getRequiredModules() {
        return requiredModules;
    }

    public void setRequiredModules(List<String> requiredModules) {
        this.requiredModules = requiredModules;
    }

    public interface ModuleFinder<T> {
        public T resolveInModule(LogicsModule module, String simpleName);
    }

    public static class LPNameModuleFinder implements ModuleFinder<LP<?, ?>> {
        @Override
        public LP<?, ?> resolveInModule(LogicsModule module, String simpleName) {
            return module.getLPByName(simpleName);
        }
    }

    public static class GroupNameModuleFinder implements ModuleFinder<AbstractGroup> {
        @Override
        public AbstractGroup resolveInModule(LogicsModule module, String simpleName) {
            return module.getGroupByName(simpleName);
        }
    }

    public static class NavigatorElementNameModuleFinder implements ModuleFinder<NavigatorElement> {
        @Override
        public NavigatorElement resolveInModule(LogicsModule module, String simpleName) {
            return module.getNavigatorElementByName(simpleName);
        }
    }

    public static class WindowNameModuleFinder implements ModuleFinder<AbstractWindow> {
        @Override
        public AbstractWindow resolveInModule(LogicsModule module, String simpleName) {
            return module.getWindowByName(simpleName);
        }
    }

    public static class MetaCodeNameModuleFinder implements ModuleFinder<MetaCodeFragment> {
        private int paramCnt;

        public MetaCodeNameModuleFinder(int paramCnt) {
            this.paramCnt = paramCnt;
        }

        @Override
        public MetaCodeFragment resolveInModule(LogicsModule module, String simpleName) {
            return module.getMetaCodeFragmentByName(simpleName, paramCnt);
        }
    }

    public static class TableNameModuleFinder implements ModuleFinder<ImplementTable> {
        @Override
        public ImplementTable resolveInModule(LogicsModule module, String simpleName) {
            return module.getTableByName(simpleName);
        }
    }

    public static class ClassNameModuleFinder implements ModuleFinder<ValueClass> {
        @Override
        public ValueClass resolveInModule(LogicsModule module, String simpleName) {
            return module.getClassByName(simpleName);
        }
    }
}
