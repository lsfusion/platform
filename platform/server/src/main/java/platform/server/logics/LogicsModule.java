package platform.server.logics;

import org.apache.commons.lang.StringUtils;
import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.Result;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.KeyStrokes;
import platform.server.caches.IdentityLazy;
import platform.server.classes.*;
import platform.server.data.Time;
import platform.server.data.Union;
import platform.server.data.expr.StringAggUnionProperty;
import platform.server.data.expr.query.GroupType;
import platform.server.data.expr.query.PartitionType;
import platform.server.data.where.classes.ClassWhere;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.FilterEntity;
import platform.server.form.navigator.NavigatorAction;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.window.AbstractWindow;
import platform.server.logics.linear.LP;
import platform.server.logics.panellocation.ToolbarPanelLocation;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.*;
import platform.server.logics.property.actions.flow.*;
import platform.server.logics.property.derived.*;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.table.ImplementTable;
import platform.server.mail.AttachmentFormat;
import platform.server.mail.EmailActionProperty;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import java.io.FileNotFoundException;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Arrays.copyOfRange;
import static platform.base.BaseUtils.*;
import static platform.server.logics.PropertyUtils.*;
import static platform.server.logics.property.actions.ExecutePropertiesActionProperty.EPA_INTERFACE;
import static platform.server.logics.property.derived.DerivedProperty.createAnd;
import static platform.server.logics.property.derived.DerivedProperty.createStatic;

/**
 * User: DAle
 * Date: 16.05.11
 * Time: 17:37
 */

public abstract class LogicsModule {
    public abstract void initClasses();

    public abstract void initTables();

    public abstract void initGroups();

    public abstract void initProperties() throws FileNotFoundException;

    public abstract void initIndexes();

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

    private final Map<String, LP<?>> moduleProperties = new HashMap<String, LP<?>>();
    private final Map<String, AbstractGroup> moduleGroups = new HashMap<String, AbstractGroup>();
    private final Map<String, ValueClass> moduleClasses = new HashMap<String, ValueClass>();
    private final Map<String, AbstractWindow> windows = new HashMap<String, AbstractWindow>();
    private final Map<String, NavigatorElement<?>> moduleNavigators = new HashMap<String, NavigatorElement<?>>();
    private final Map<String, ImplementTable> moduleTables = new HashMap<String, ImplementTable>();

    private final Map<String, List<String>> propNamedParams = new HashMap<String, List<String>>();
    private final Map<String, MetaCodeFragment> metaCodeFragments = new HashMap<String, MetaCodeFragment>();

    protected LogicsModule() {}

    public LogicsModule(String sID) {
        this.sID = sID;
    }

    private String sID;

    protected String getSID() {
        return sID;
    }

    protected void setSID(String sID) {
        this.sID = sID;
    }

    public LP<?> getLPBySID(String sID) {
        return moduleProperties.get(sID);
    }

    public LP<?> getLPByName(String name) {
        return getLPBySID(transformNameToSID(name));
    }

    protected void addModuleLP(LP<?> lp) {
        assert !moduleProperties.containsKey(lp.property.getSID());
        moduleProperties.put(lp.property.getSID(), lp);
    }

    protected void removeModuleLP(LP<?> lp) {
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

    public MetaCodeFragment getMetaCodeFragmentByName(String name) {
        return getMetaCodeFragmentBySID(transformNameToSID(name));
    }

    protected MetaCodeFragment getMetaCodeFragmentBySID(String sid) {
        return metaCodeFragments.get(sid);
    }

    protected void addMetaCodeFragment(String name, MetaCodeFragment fragment) {
        assert !metaCodeFragments.containsKey(transformNameToSID(name));
        metaCodeFragments.put(transformNameToSID(name), fragment);
    }

    // aliases для использования внутри иерархии логических модулей
    protected BaseClass baseClass;

    protected AbstractGroup rootGroup;
    protected AbstractGroup publicGroup;
    protected AbstractGroup privateGroup;
    protected AbstractGroup baseGroup;
    protected AbstractGroup idGroup;
    protected AbstractGroup actionGroup;
    protected AbstractGroup sessionGroup;
    protected AbstractGroup recognizeGroup;

    protected void setBaseLogicsModule(BaseLogicsModule<?> baseLM) {
        this.baseLM = baseLM;
    }

    protected void initBaseGroupAliases() {
        this.rootGroup = baseLM.rootGroup;
        this.publicGroup = baseLM.publicGroup;
        this.privateGroup = baseLM.privateGroup;
        this.baseGroup = baseLM.baseGroup;
        this.idGroup = baseLM.idGroup;
        this.actionGroup = baseLM.actionGroup;
        this.sessionGroup = baseLM.sessionGroup;
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
        storeCustomClass(baseClass.named);
        storeCustomClass(baseClass.sidClass);
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
        ConcreteCustomClass customClass = new CustomObjectClass(transformNameToSID(name), caption, parents);
        storeCustomClass(customClass);
        return customClass;
    }

    protected AbstractCustomClass addAbstractClass(String name, String caption, CustomClass... parents) {
        AbstractCustomClass customClass = new AbstractCustomClass(transformNameToSID(name), caption, parents);
        storeCustomClass(customClass);
        return customClass;
    }

    protected StaticCustomClass addStaticClass(String name, String caption, String[] sids, String[] names, CustomClass... parents) {
        StaticCustomClass customClass = new StaticCustomClass(transformNameToSID(name), caption, baseLM.baseClass.sidClass, sids, names, parents);
        storeCustomClass(customClass);
        customClass.dialogReadOnly = true;
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

    protected LP addDProp(String name, String caption, ValueClass value, ValueClass... params) {
        return addDProp(null, name, false, caption, value, params);
    }

    protected LP addDProp(AbstractGroup group, String name, String caption, ValueClass value, ValueClass... params) {
        return addDProp(group, name, false, caption, value, params);
    }

    protected LP[] addDProp(AbstractGroup group, String paramID, String[] names, String[] captions, ValueClass[] values, ValueClass... params) {
        LP[] result = new LP[names.length];
        for (int i = 0; i < names.length; i++)
            result[i] = addDProp(group, names[i] + paramID, captions[i], values[i], params);
        return result;
    }

    protected LP addDProp(AbstractGroup group, String name, boolean persistent, String caption, ValueClass value, ValueClass... params) {
        StoredDataProperty dataProperty = new StoredDataProperty(name, caption, params, value);
        LP lp = addProperty(group, persistent, new LP<ClassPropertyInterface>(dataProperty));
        dataProperty.markStored(baseLM.tableFactory);
        return lp;
    }

    protected LP addGDProp(AbstractGroup group, String paramID, String name, String caption, ValueClass[] values, CustomClass[]... params) {
        CustomClass[][] listParams = new CustomClass[params[0].length][]; //
        for (int i = 0; i < listParams.length; i++) {
            listParams[i] = new CustomClass[params.length];
            for (int j = 0; j < params.length; j++)
                listParams[i][j] = params[j][i];
        }
        params = listParams;

        LP[] genProps = new LP[params.length];
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

    protected LP[] addGDProp(AbstractGroup group, String paramID, String[] names, String[] captions, ValueClass[][] values, CustomClass[]... params) {
        LP[] result = new LP[values.length];
        for (int i = 0; i < values.length; i++)
            result[i] = addGDProp(group, paramID, names[i], captions[i], values[i], params);
        return result;
    }

    protected <D extends PropertyInterface> LP addDCProp(String name, String caption, LP<D> derivedProp, Object... params) {
        return addDCProp(null, name, caption, derivedProp, params);
    }

    protected <D extends PropertyInterface> LP addDCProp(String name, String caption, int whereNum, LP<D> derivedProp, Object... params) {
        return addDCProp(null, name, caption, whereNum, derivedProp, params);
    }

    protected <D extends PropertyInterface> LP addDCProp(String name, boolean persistent, String caption, LP<D> derivedProp, Object... params) {
        return addDCProp(null, name, persistent, caption, false, derivedProp, params);
    }

    protected <D extends PropertyInterface> LP addDCProp(AbstractGroup group, String name, String caption, LP<D> derivedProp, Object... params) {
        return addDCProp(group, name, caption, 0, derivedProp, params);
    }

    protected <D extends PropertyInterface> LP addDCProp(AbstractGroup group, String name, String caption, int whereNum,  LP<D> derivedProp, Object... params) {
        return addDCProp(group, name, false, caption, false, whereNum, derivedProp, params);
    }

    protected <D extends PropertyInterface> LP addDCProp(String name, String caption, boolean forced, LP<D> derivedProp, Object... params) {
        return addDCProp(null, name, caption, forced, derivedProp, params);
    }

    protected <D extends PropertyInterface> LP addDCProp(AbstractGroup group, String name, String caption, boolean forced, LP<D> derivedProp, Object... params) {
        return addDCProp(group, name, false, caption, forced, derivedProp, params);
    }

    protected <D extends PropertyInterface> LP addDCProp(AbstractGroup group, String name, boolean persistent, String caption, boolean forced, LP<D> derivedProp, Object... params) {
        return addDCProp(group, name, persistent, caption, forced, 0, derivedProp, params);
    }

    protected <D extends PropertyInterface> LP addDCProp(AbstractGroup group, String name, boolean persistent, String caption, boolean forced, int whereNum, LP<D> derivedProp, Object... params) {

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
        List<PropertyUtils.LI> list = readLI(params);

        int propsize = derivedProp.listInterfaces.size();
        int dersize = getIntNum(params);
        List<JoinProperty.Interface> listInterfaces = JoinProperty.getInterfaces(dersize);

        AndFormulaProperty andProperty = new AndFormulaProperty(genSID(), new boolean[list.size() - propsize]);
        Map<AndFormulaProperty.Interface, PropertyInterfaceImplement<JoinProperty.Interface>> mapImplement = new HashMap<AndFormulaProperty.Interface, PropertyInterfaceImplement<JoinProperty.Interface>>();
        mapImplement.put(andProperty.objectInterface, DerivedProperty.createJoin(mapImplement(derivedProp, mapLI(list.subList(0, propsize), listInterfaces))));
        Iterator<AndFormulaProperty.AndInterface> itAnd = andProperty.andInterfaces.iterator();
        for (PropertyInterfaceImplement<JoinProperty.Interface> partProperty : mapLI(list.subList(propsize, list.size()), listInterfaces))
            mapImplement.put(itAnd.next(), partProperty);

        JoinProperty<AndFormulaProperty.Interface> joinProperty = new JoinProperty<AndFormulaProperty.Interface>(name, caption, listInterfaces, false,
                new PropertyImplement<AndFormulaProperty.Interface, PropertyInterfaceImplement<JoinProperty.Interface>>(andProperty, mapImplement));
        LP<JoinProperty.Interface> listProperty = new LP<JoinProperty.Interface>(joinProperty, listInterfaces);

        // получаем классы
        Result<ValueClass> value = new Result<ValueClass>();
        ValueClass[] commonClasses = listProperty.getCommonClasses(value);

        // override'им классы
        ValueClass valueClass;
        if (overrideClasses.length > dersize) {
            valueClass = overrideClasses[dersize];
            assert !overrideClasses[dersize].isCompatibleParent(value.result);
            overrideClasses = copyOfRange(params, 0, dersize, ValueClass[].class);
        } else
            valueClass = value.result;

        // выполняем само создание свойства
        LP derDataProp = addDProp(group, name, persistent, caption, valueClass, overrideClasses(commonClasses, overrideClasses));
        if (forced)
            derDataProp.setEventSet(defaultChanged, whereNum, derivedProp, params);
        else
            derDataProp.setEvent(defaultChanged, whereNum, derivedProp, params);
        return derDataProp;
    }

    protected LP addSDProp(String caption, ValueClass value, ValueClass... params) {
        return addSDProp((AbstractGroup) null, caption, value, params);
    }

    protected LP addSDProp(AbstractGroup group, String caption, ValueClass value, ValueClass... params) {
        return addSDProp(group, genSID(), caption, value, params);
    }

    protected LP addSDProp(String name, String caption, ValueClass value, ValueClass... params) {
        return addSDProp(null, name, caption, value, params);
    }

    protected LP addSDProp(String name, boolean persistent, String caption, ValueClass value, ValueClass... params) {
        return addSDProp(null, name, persistent, caption, value, params);
    }

    protected LP addSDProp(AbstractGroup group, String name, String caption, ValueClass value, ValueClass... params) {
        return addSDProp(group, name, false, caption, value, params);
    }

    protected LP addSDProp(AbstractGroup group, String name, boolean persistent, String caption, ValueClass value, ValueClass... params) {
        return addProperty(group, persistent, new LP<ClassPropertyInterface>(new SessionDataProperty(name, caption, params, value)));
    }

    protected LP addFAProp(String caption, FormEntity form, ObjectEntity... params) {
        return addFAProp(null, caption, form, params, new PropertyObjectEntity[0], new PropertyObjectEntity[0], false, false);
    }

    protected LP addFAProp(AbstractGroup group, FormEntity form, ObjectEntity... params) {
        return addFAProp(group, form.caption, form, params);
    }

    protected LP addFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity... params) {
        return addFAProp(group, caption, form, params, new PropertyObjectEntity[0], new PropertyObjectEntity[0], false, false);
    }

    public LP addMFAProp(String caption, FormEntity form, ObjectEntity... params) {
        return addMFAProp(null, caption, form, params, new PropertyObjectEntity[0], new PropertyObjectEntity[0]);
    }

    protected LP addMFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, PropertyObjectEntity... setProperties) {
        return addMFAProp(group, caption, form, objectsToSet, false, setProperties);
    }

    protected LP addMFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, boolean newSession, PropertyObjectEntity... setProperties) {
        return addMFAProp(group, genSID(), caption, form, objectsToSet, newSession, setProperties);
    }

    protected LP addMFAProp(AbstractGroup group, String sID, String caption, FormEntity form, ObjectEntity[] objectsToSet, boolean newSession, PropertyObjectEntity... setProperties) {
        return addMFAProp(group, sID, caption, form, objectsToSet, setProperties, new PropertyObjectEntity[setProperties.length], newSession);
    }

    protected LP addMFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, PropertyObjectEntity[] setProperties, OrderEntity[] getProperties) {
        return addMFAProp(group, caption, form, objectsToSet, setProperties, getProperties, true);
    }

    protected LP addMFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, PropertyObjectEntity[] setProperties, OrderEntity[] getProperties, boolean newSession) {
        return addMFAProp(group, caption, form, objectsToSet, setProperties, getProperties, null, newSession);
    }

    protected LP addMFAProp(AbstractGroup group, String sID, String caption, FormEntity form, ObjectEntity[] objectsToSet, PropertyObjectEntity[] setProperties, OrderEntity[] getProperties, boolean newSession) {
        return addMFAProp(group, sID, caption, form, objectsToSet, setProperties, getProperties, null, newSession);
    }

    protected LP addMFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, PropertyObjectEntity[] setProperties, OrderEntity[] getProperties, DataClass valueClass, boolean newSession) {
        return addMFAProp(group, genSID(), caption, form, objectsToSet, setProperties, getProperties, valueClass, newSession);
    }

    protected LP addMFAProp(AbstractGroup group, String sID, String caption, FormEntity form, ObjectEntity[] objectsToSet, PropertyObjectEntity[] setProperties, OrderEntity[] getProperties, DataClass valueClass, boolean newSession) {
        return addFAProp(group, sID, caption, form, objectsToSet, setProperties, getProperties, valueClass, newSession, true, false);
    }

    protected LP addFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, PropertyObjectEntity[] setProperties, OrderEntity[] getProperties, boolean newSession, boolean isModal) {
        return addFAProp(group, caption, form, objectsToSet, setProperties, getProperties, null, newSession, isModal);
    }

    protected LP addFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, PropertyObjectEntity[] setProperties, OrderEntity[] getProperties, DataClass valueClass, boolean newSession, boolean isModal) {
        return addFAProp(group, genSID(), caption, form, objectsToSet, setProperties, getProperties, valueClass, newSession, isModal, false);
    }

    protected LP addFAProp(AbstractGroup group, String sID, String caption, FormEntity form, ObjectEntity[] objectsToSet, PropertyObjectEntity[] setProperties, OrderEntity[] getProperties, DataClass valueClass, boolean newSession, boolean isModal, boolean checkOnOk) {
        return addProperty(group, new LP<ClassPropertyInterface>(new FormActionProperty(sID, caption, form, objectsToSet, setProperties, getProperties, valueClass, newSession, isModal, checkOnOk, baseLM.formResult, baseLM.getFormResultProperty(), baseLM.getChosenObjectProperty())));
    }

    protected LP addSelectFromListAction(AbstractGroup group, String caption, LP selectionProperty, ValueClass selectionClass, ValueClass... baseClasses) {
        return addSelectFromListAction(group, caption, null, new FilterEntity[0], selectionProperty, selectionClass, baseClasses);
    }

    protected LP addSelectFromListAction(AbstractGroup group, String caption, ObjectEntity remapObject, FilterEntity[] remapFilters, LP selectionProperty, ValueClass selectionClass, ValueClass... baseClasses) {
        return addSelectFromListAction(group, caption, remapObject, remapFilters, selectionProperty, false, selectionClass, baseClasses);
    }

    protected LP addSelectFromListAction(AbstractGroup group, String caption, ObjectEntity remapObject, FilterEntity[] remapFilters, LP selectionProperty, boolean isSelectionClassFirstParam, ValueClass selectionClass, ValueClass... baseClasses) {
        BaseLogicsModule.SelectFromListFormEntity selectFromListForm = baseLM.new SelectFromListFormEntity(remapObject, remapFilters, selectionProperty, isSelectionClassFirstParam, selectionClass, baseClasses);
        return addMFAProp(group, caption, selectFromListForm, selectFromListForm.mainObjects, false);
    }

    protected LP addChangeClassAProp(String caption, ConcreteCustomClass cls) {
        return addAProp(new ChangeClassActionProperty(genSID(), caption, cls, baseClass));
    }

    protected LP addStopActionProp(String caption, String header) {
        return addAProp(new StopActionProperty(genSID(), caption, header));
    }

    protected <C extends PropertyInterface, W extends PropertyInterface> LP addSetPropertyAProp(AbstractGroup group, String name, String caption, int interfaces, int resInterfaces,
                                                                                                boolean conditional, Object... params) {
        List<PropertyInterface> innerInterfaces = genInterfaces(interfaces);
        List<PropertyInterfaceImplement<PropertyInterface>> readImplements = readImplements(innerInterfaces, params);
        PropertyMapImplement<W, PropertyInterface> conditionalPart = (PropertyMapImplement<W, PropertyInterface>)
                (conditional ? readImplements.get(resInterfaces + 2) : DerivedProperty.createStatic(true, LogicalClass.instance));
        return addProperty(group, new LP<ClassPropertyInterface>(new ChangeActionProperty<C, W, PropertyInterface>(name, caption,
                                                                                                             innerInterfaces, (List) readImplements.subList(0, resInterfaces), conditionalPart,
                                                                                                             (PropertyMapImplement<C, PropertyInterface>) readImplements.get(resInterfaces), readImplements.get(resInterfaces + 1))));
    }

    protected LP addListAProp(AbstractGroup group, String name, String caption, int interfaces, Object... params) {
        return addListAProp(group, name, caption, interfaces, true, true, params);
    }

    protected LP addListAProp(AbstractGroup group, String name, String caption, int interfaces, boolean newSession, boolean doApply, Object... params) {
        List<PropertyInterface> listInterfaces = genInterfaces(interfaces);
        return addProperty(group, new LP<ClassPropertyInterface>(new ListActionProperty(name, caption, listInterfaces,
                (List) readImplements(listInterfaces, params), newSession, doApply, baseLM.BL)));
    }

    protected LP addIfAProp(AbstractGroup group, String name, String caption, int interfaces, Object... params) {
        List<PropertyInterface> listInterfaces = genInterfaces(interfaces);
        List<PropertyInterfaceImplement<PropertyInterface>> readImplements = readImplements(listInterfaces, params);
        assert readImplements.size() >= 2 && readImplements.size() <= 3;

        if (readImplements.size() == 3) {
            return addProperty(group, new LP<ClassPropertyInterface>(new IfActionProperty(name, caption, listInterfaces, readImplements.get(0),
                        (PropertyMapImplement<ClassPropertyInterface, PropertyInterface>)readImplements.get(1), (PropertyMapImplement<ClassPropertyInterface, PropertyInterface>)readImplements.get(2))));
        } else {
            return addProperty(group, new LP<ClassPropertyInterface>(new IfActionProperty(name, caption, listInterfaces, readImplements.get(0),
                        (PropertyMapImplement<ClassPropertyInterface, PropertyInterface>)readImplements.get(1))));
        }
    }

    protected LP addForAProp(AbstractGroup group, String name, String caption, boolean ascending, boolean recursive, boolean hasElse, int interfaces, int resInterfaces, Object... params) {
        List<PropertyInterface> innerInterfaces = genInterfaces(interfaces);
        List<PropertyInterfaceImplement<PropertyInterface>> readImplements = readImplements(innerInterfaces, params);

        int implCnt = readImplements.size();

        PropertyMapImplement<?, PropertyInterface> ifProp = (PropertyMapImplement<?, PropertyInterface>) readImplements.get(resInterfaces);

        PropertyMapImplement<ClassPropertyInterface, PropertyInterface> action =
                (PropertyMapImplement<ClassPropertyInterface, PropertyInterface>) readImplements.get(implCnt - 1);
        PropertyMapImplement<ClassPropertyInterface, PropertyInterface> elseAction =
                !hasElse ? null : (PropertyMapImplement<ClassPropertyInterface, PropertyInterface>) readImplements.get(implCnt - 2);

        OrderedMap<PropertyInterfaceImplement<PropertyInterface>, Boolean> orders =
                toOrderedMap(readImplements.subList(resInterfaces + 1, implCnt - (hasElse ? 2 : 1)), !ascending);

        List mapInterfaces = readImplements.subList(0, resInterfaces);

        return addProperty(group, new LP<ClassPropertyInterface>(
                new ForActionProperty<PropertyInterface>(name, caption, innerInterfaces, mapInterfaces, ifProp, orders, action, elseAction, recursive))
        );
    }

    protected LP addJoinAProp(AbstractGroup group, String name, String caption, int interfaces, LP action, Object... params) {
        return addJoinAProp(group, name, caption, interfaces, null, action, params);
    }

    protected LP addJoinAProp(AbstractGroup group, String name, String caption, int interfaces, ValueClass[] classes, LP action, Object... params) {
        List<PropertyInterface> listInterfaces = genInterfaces(interfaces);
        List<PropertyInterfaceImplement<PropertyInterface>> readImplements = readImplements(listInterfaces, params);
        return addProperty(group, new LP<ClassPropertyInterface>(new JoinActionProperty(name, caption, listInterfaces, mapImplement(action, readImplements), classes)));
    }

    protected LP addEAProp(ValueClass... params) {
        return addEAProp((String) null, params);
    }

    protected LP addEAProp(LP fromAddress, ValueClass... params) {
        return addEAProp(null, fromAddress, baseLM.emailBlindCarbonCopy, params);
    }

    protected LP addEAProp(String subject, ValueClass... params) {
        return addEAProp(subject, baseLM.fromAddress, baseLM.emailBlindCarbonCopy, params);
    }

    protected LP addEAProp(LP fromAddress, LP emailBlindCarbonCopy, ValueClass... params) {
        return addEAProp(null, fromAddress, emailBlindCarbonCopy, params);
    }

    protected LP addEAProp(String subject, LP fromAddress, LP emailBlindCarbonCopy, ValueClass... params) {
        return addEAProp(null, genSID(), "email", subject, fromAddress, emailBlindCarbonCopy, params);
    }

    protected LP addEAProp(AbstractGroup group, String name, String caption, String subject, LP fromAddress, LP emailBlindCarbonCopy, ValueClass... params) {
        Object[] fromImplement = new Object[] {fromAddress};
        Object[] subjImplement;
        if (subject != null) {
            subjImplement = new Object[] {addCProp(StringClass.get(subject.length()), subject)};
        } else {
            ValueClass[] nParams = new ValueClass[params.length + 1];
            System.arraycopy(params, 0, nParams, 0, params.length);
            nParams[params.length] = StringClass.get(100);

            params = nParams;

            subjImplement = new Object[] {params.length};
        }

        LP<ClassPropertyInterface> eaPropLP = addEAProp(group, name, caption, params, fromImplement, subjImplement);
        addEARecipients(eaPropLP, Message.RecipientType.BCC, emailBlindCarbonCopy);

        return eaPropLP;
    }

    protected LP<ClassPropertyInterface> addEAProp(AbstractGroup group, String name, String caption, ValueClass[] params, Object[] fromAddress, Object[] subject) {
        EmailActionProperty eaProp = new EmailActionProperty(name, caption, baseLM.BL, params);
        LP<ClassPropertyInterface> eaPropLP = addProperty(group, new LP<ClassPropertyInterface>(eaProp));

        if (fromAddress != null) {
            eaProp.setFromAddress(single(readImplements(eaPropLP.listInterfaces, fromAddress)));
        }

        if (subject != null) {
            eaProp.setSubject(single(readImplements(eaPropLP.listInterfaces, subject)));
        }

        return eaPropLP;
    }

    protected void addEARecipients(LP<ClassPropertyInterface> eaProp, Object... params) {
        addEARecipients(eaProp, MimeMessage.RecipientType.TO, params);
    }

    protected void addEARecipients(LP<ClassPropertyInterface> eaProp, Message.RecipientType type, Object... params) {
        List<PropertyInterfaceImplement<ClassPropertyInterface>> recipImpls = readImplements(eaProp.listInterfaces, params);

        for (PropertyInterfaceImplement<ClassPropertyInterface> recipImpl : recipImpls) {
            ((EmailActionProperty) eaProp.property).addRecipient(recipImpl, type);
        }
    }

    private Map<ObjectEntity, PropertyInterfaceImplement<ClassPropertyInterface>> readObjectImplements(LP<ClassPropertyInterface> eaProp, Object[] params) {
        Map<ObjectEntity, PropertyInterfaceImplement<ClassPropertyInterface>> mapObjects = new HashMap<ObjectEntity, PropertyInterfaceImplement<ClassPropertyInterface>>();

        int i = 0;
        while (i < params.length) {
            ObjectEntity object = (ObjectEntity)params[i];

            ArrayList<Object> objectImplement = new ArrayList<Object>();
            while (++i < params.length && !(params[i] instanceof ObjectEntity)) {
                objectImplement.add(params[i]);
            }

            // знаем, что только один будет
            mapObjects.put(object, single(readImplements(eaProp.listInterfaces, objectImplement.toArray())));
        }
        return mapObjects;
    }

    protected void addInlineEAForm(LP<ClassPropertyInterface> eaProp, FormEntity form, Object... params) {
        ((EmailActionProperty) eaProp.property).addInlineForm(form, readObjectImplements(eaProp, params));
    }

    /**
     * @param params : сначала может идти свойство, из которго будет читаться имя attachment'a,
     * при этом все его входы мэпятся на входы eaProp по порядку, <br/>
     * затем список объектов ObjectEntity + мэппинг, из которого будет читаться значение этого объекта.
     * <br/>
     * Мэппинг - это мэппинг на интерфейсы результирующего свойства (prop, 1,3,4 или просто N)
     * @deprecated теперь лучше использовать {@link platform.server.logics.LogicsModule#addAttachEAForm(platform.server.logics.linear.LP<platform.server.logics.property.ClassPropertyInterface>, platform.server.form.entity.FormEntity, platform.server.mail.AttachmentFormat, java.lang.Object...)}
     * с явным мэппингом свойства для имени
     */
    protected void addAttachEAFormNameFullyMapped(LP<ClassPropertyInterface> eaProp, FormEntity form, AttachmentFormat format, Object... params) {
        if (params.length > 0 && params[0] instanceof LP) {
            LP attachmentNameProp = (LP) params[0];

            ArrayList nParams = new ArrayList();
            nParams.add(attachmentNameProp);
            nParams.addAll(consecutiveList(attachmentNameProp.listInterfaces.size()));
            nParams.addAll(asList(copyOfRange(params, 1, params.length)));

            params = nParams.toArray();
        }

        addAttachEAForm(eaProp, form, format, params);
    }

    /**
     * @param params : сначала может идти мэппинг, из которго будет читаться имя attachment'a,
     * затем список объектов ObjectEntity + мэппинг, из которого будет читаться значение этого объекта.
     * <br/>
     * Мэппинг - это мэппинг на интерфейсы результирующего свойства (prop, 1,3,4 или просто N)
     */
    protected void addAttachEAForm(LP<ClassPropertyInterface> eaProp, FormEntity form, AttachmentFormat format, Object... params) {
        PropertyInterfaceImplement<ClassPropertyInterface> attachNameImpl = null;
        if (params.length > 0 && !(params[0] instanceof ObjectEntity)) {
            int attachNameParamsCnt = 1;
            while (attachNameParamsCnt < params.length && !(params[attachNameParamsCnt] instanceof ObjectEntity)) {
                ++attachNameParamsCnt;
            }
            attachNameImpl = single(readImplements(eaProp.listInterfaces, copyOfRange(params, 0, attachNameParamsCnt)));
            params = copyOfRange(params, attachNameParamsCnt, params.length);
        }
        ((EmailActionProperty) eaProp.property).addAttachmentForm(form, format, readObjectImplements(eaProp, params), attachNameImpl);
    }

    protected LP addTAProp(LP sourceProperty, LP targetProperty) {
        return addProperty(null, new LP<ClassPropertyInterface>(new TranslateActionProperty(genSID(), "translate", baseLM.translationDictionaryTerm, sourceProperty, targetProperty, baseLM.dictionary)));
    }

    protected <P extends PropertyInterface> LP addSCProp(LP<P> lp) {
        return addSCProp(baseLM.privateGroup, "sys", lp);
    }

    protected <P extends PropertyInterface> LP addSCProp(AbstractGroup group, String caption, LP<P> lp) {
        return addProperty(group, new LP<ShiftChangeProperty.Interface<P>>(new ShiftChangeProperty<P, PropertyInterface>(genSID(), caption, lp.property, new PropertyMapImplement<PropertyInterface, P>(baseLM.reverseBarcode.property))));
    }

    protected LP addCProp(StaticClass valueClass, Object value, ValueClass... params) {
        return addCProp("sys", valueClass, value, params);
    }

    protected LP addCProp(String caption, StaticClass valueClass, Object value, ValueClass... params) {
        return addCProp(null, false, caption, valueClass, value, params);
    }

    protected LP addCProp(AbstractGroup group, boolean persistent, String caption, StaticClass valueClass, Object value, ValueClass... params) {
        return addCProp(group, persistent, caption, valueClass, value, Arrays.asList(params));
    }

    // только для того, чтобы обернуть все в IdentityLazy, так как только для List нормально сделан equals
    @IdentityLazy
    protected LP addCProp(AbstractGroup group, boolean persistent, String caption, StaticClass valueClass, Object value, List<ValueClass> params) {
        return addCProp(group, genSID(), persistent, caption, valueClass, value, params.toArray(new ValueClass[]{}));
    }

    protected <T extends PropertyInterface> LP addCProp(AbstractGroup group, String name, boolean persistent, String caption, StaticClass valueClass, Object value, ValueClass... params) {
        PropertyImplement<T, Integer> implement = (PropertyImplement<T, Integer>) DerivedProperty.createCProp(name, caption, valueClass, value, BaseUtils.toMap(params));
        return addProperty(group, persistent, new LP<T>(implement.property, BaseUtils.toList(BaseUtils.reverse(implement.mapping))));
    }

    // добавляет свойство с бесконечным значением
    protected <T extends PropertyInterface> LP addICProp(DataClass valueClass, ValueClass... params) {
        PropertyImplement<T, Integer> implement = (PropertyImplement<T, Integer>) DerivedProperty.createCProp(genSID(), ServerResourceBundle.getString("logics.infinity"), valueClass, BaseUtils.toMap(params));
        return addProperty(baseLM.privateGroup, false, new LP<T>(implement.property, BaseUtils.toList(BaseUtils.reverse(implement.mapping))));
    }


    protected LP addTProp(Time time) {
        return addTProp(genSID(), time);
    }
    
    protected LP addTProp(String sID, Time time) {
        return addProperty(null, new LP<PropertyInterface>(new TimeFormulaProperty(sID, time)));
    }

    protected <P extends PropertyInterface> LP addTCProp(Time time, String name, String caption, LP<P> changeProp, ValueClass... classes) {
        return addTCProp(null, time, name, caption, changeProp, classes);
    }

    protected <P extends PropertyInterface> LP addTCProp(Time time, String name, boolean isStored, String caption, LP<P> changeProp, ValueClass... classes) {
        return addTCProp(null, time, name, isStored, caption, changeProp, classes);
    }

    protected <P extends PropertyInterface> LP addTCProp(AbstractGroup group, Time time, String name, String caption, LP<P> changeProp, ValueClass... classes) {
        return addTCProp(group, time, name, false, caption, changeProp, classes);
    }

    protected <P extends PropertyInterface> LP addTCProp(AbstractGroup group, Time time, String name, boolean isStored, String caption, LP<P> changeProp, ValueClass... classes) {
        TimePropertyChange<P> timeProperty = new TimePropertyChange<P>(isStored, time, name, caption, overrideClasses(changeProp.getMapClasses(), classes), changeProp.listInterfaces);

        changeProp.property.timeChanges.put(time, timeProperty);

        if (isStored) {
            timeProperty.property.markStored(baseLM.tableFactory);
        }

        return addProperty(group, false, new LP<ClassPropertyInterface>(timeProperty.property));
    }

    protected LP addSFProp(String name, String formula, int paramCount) {
        return addSFProp(name, formula, null, paramCount);
    }

    protected LP addSFProp(String formula, int paramCount) {
        return addSFProp(formula, (ConcreteValueClass)null, paramCount);
    }

    protected LP addSFProp(String formula, ConcreteValueClass value, int paramCount) {
        return addSFProp(genSID(), formula, value, paramCount);
    }

    protected LP addSFProp(String name, String formula, ConcreteValueClass value, int paramCount) {
        return addProperty(null, new LP<StringFormulaProperty.Interface>(new StringFormulaProperty(name, value, formula, paramCount)));
    }

    protected LP addCFProp(Compare compare) {
        return addCFProp(genSID(), compare);
    }

    protected LP addCFProp(String name, Compare compare) {
        return addProperty(null, new LP<CompareFormulaProperty.Interface>(new CompareFormulaProperty(name, compare)));
    }

    protected <P extends PropertyInterface> LP addSProp(String name, int intNum) {
        return addProperty(null, new LP<StringConcatenateProperty.Interface>(new StringConcatenateProperty(name, ServerResourceBundle.getString("logics.join"), intNum, " ")));
    }

    protected <P extends PropertyInterface> LP addSProp(int intNum) {
        return addProperty(null, new LP<StringConcatenateProperty.Interface>(new StringConcatenateProperty(genSID(), ServerResourceBundle.getString("logics.join"), intNum, " ")));
    }

    protected <P extends PropertyInterface> LP addSProp(int intNum, String separator) {
        return addProperty(null, new LP<StringConcatenateProperty.Interface>(new StringConcatenateProperty(genSID(), ServerResourceBundle.getString("logics.join"), intNum, separator)));
    }

    protected <P extends PropertyInterface> LP addInsensitiveSProp(int intNum) {
        return addProperty(null, new LP<StringConcatenateProperty.Interface>(new StringConcatenateProperty(genSID(), ServerResourceBundle.getString("logics.join"), intNum, " ", false)));
    }

    protected <P extends PropertyInterface> LP addInsensitiveSProp(int intNum, String separator) {
        return addProperty(null, new LP<StringConcatenateProperty.Interface>(new StringConcatenateProperty(genSID(), ServerResourceBundle.getString("logics.join"), intNum, separator, false)));
    }


    protected LP addMFProp(String name, int paramCount) {
        return addMFProp(name, null, paramCount);
    }
    protected LP addMFProp(String name, ConcreteValueClass value, int paramCount) {
        return addProperty(null, new LP<StringFormulaProperty.Interface>(new MultiplyFormulaProperty(name, value, paramCount)));
    }
    protected LP addMFProp(int paramCount) {
        return addMFProp(genSID(), null, paramCount);
    }
    protected LP addMFProp(ConcreteValueClass value, int paramCount) {
        return addMFProp(genSID(), value, paramCount);
    }

    protected LP addAFProp(boolean... nots) {
        return addAFProp((AbstractGroup) null, nots);
    }

    protected LP addAFProp(String name, boolean... nots) {
        return addAFProp(null, name, nots);
    }

    protected LP addAFProp(AbstractGroup group, boolean... nots) {
        return addAFProp(group, genSID(), nots);
    }

    protected LP addAFProp(AbstractGroup group, String name, boolean... nots) {
        return addProperty(group, new LP<AndFormulaProperty.Interface>(new AndFormulaProperty(name, nots)));
    }

    protected LP addCCProp(int paramCount) {
        return addProperty(null, new LP<ConcatenateProperty.Interface>(new ConcatenateProperty(genSID(), paramCount)));
    }

    protected LP addDCCProp(int paramIndex) {
        return addProperty(null, new LP<DeconcatenateProperty.Interface>(new DeconcatenateProperty(genSID(), paramIndex, baseLM.baseClass)));
    }

    protected boolean isDefaultJoinImplementChange(Property property) {
        return property instanceof ActionProperty;
    }

    public LP addJProp(LP mainProp, Object... params) {
        return addJProp(baseLM.privateGroup, "sys", mainProp, params);
    }

    protected LP addJProp(String caption, LP mainProp, Object... params) {
        return addJProp((AbstractGroup) null, caption, mainProp, params);
    }

    protected LP addJProp(String name, String caption, LP mainProp, Object... params) {
        return addJProp(name, false, caption, mainProp, params);
    }

    protected LP addJProp(String name, boolean persistent, String caption, LP mainProp, Object... params) {
        return addJProp(null, name, persistent, caption, mainProp, params);
    }

    protected LP addJProp(AbstractGroup group, String caption, LP mainProp, Object... params) {
        return addJProp(group, genSID(), caption, mainProp, params);
    }

    protected LP addJProp(boolean implementChange, String caption, LP mainProp, Object... params) {
        return addJProp((AbstractGroup) null, implementChange, caption, mainProp, params);
    }

    protected LP addJProp(boolean implementChange, String name, String caption, LP mainProp, Object... params) {
        return addJProp((AbstractGroup) null, implementChange, name, caption, mainProp, params);
    }

    protected LP addJProp(AbstractGroup group, boolean implementChange, String caption, LP mainProp, Object... params) {
        return addJProp(group, implementChange, genSID(), caption, mainProp, params);
    }

    protected LP addJProp(AbstractGroup group, String name, String caption, LP mainProp, Object... params) {
        return addJProp(group, isDefaultJoinImplementChange(mainProp.property), name, caption, mainProp, params);
    }

    protected LP addJProp(AbstractGroup group, String name, boolean persistent, String caption, LP mainProp, Object... params) {
        return addJProp(group, isDefaultJoinImplementChange(mainProp.property), name, persistent, caption, mainProp, params);
    }

    protected LP addJProp(boolean implementChange, LP mainProp, Object... params) {
        return addJProp(baseLM.privateGroup, implementChange, genSID(), "sys", mainProp, params);
    }

    protected LP addJProp(AbstractGroup group, boolean implementChange, String name, String caption, LP mainProp, Object... params) {
        return addJProp(group, implementChange, name, false, caption, mainProp, params);
    }

    protected LP addJProp(AbstractGroup group, boolean implementChange, boolean persistent, String caption, LP mainProp, Object... params) {
        return addJProp(group, implementChange, genSID(), persistent, caption, mainProp, params);
    }

    protected LP addJProp(AbstractGroup group, boolean implementChange, String name, boolean persistent, String caption, LP mainProp, Object... params) {

        List<JoinProperty.Interface> listInterfaces = JoinProperty.getInterfaces(getIntNum(params));
        JoinProperty<?> property = new JoinProperty(name, caption, listInterfaces, implementChange,
                mapImplement(mainProp, readImplements(listInterfaces, params)));
        property.inheritFixedCharWidth(mainProp.property);
        property.inheritImage(mainProp.property);

        return addProperty(group, persistent, new LP<JoinProperty.Interface>(property, listInterfaces));
    }

    protected LP[] addJProp(AbstractGroup group, boolean implementChange, String paramID, LP[] props, String caption, Object... params) {
        LP[] result = new LP[props.length];
        for (int i = 0; i < props.length; i++)
            result[i] = addJProp(group, implementChange, props[i].property.getSID() + paramID, props[i].property.caption + (caption.length() == 0 ? "" : (" " + caption)), props[i], params);
        return result;
    }

    protected LP[] addJProp(AbstractGroup group, boolean implementChange, String paramID, LP[] props, Object... params) {
        return addJProp(group, implementChange, paramID, props, "", params);
    }

    /**
     * Создаёт свойство для группового изменения, при этом группировка идёт по всем интерфейсам, и мэппинг интерфейсов происходит по порядку
     */
    protected LP addGCAProp(AbstractGroup group, String name, String caption, LP mainProperty, LP getterProperty) {
        return addGCAProp(group, name, caption, null, mainProperty, getterProperty);
    }

    /**
     * Создаёт свойство для группового изменения, при этом группировка идёт по всем интерфейсам, мэппинг интерфейсов происходит по порядку
     * при этом, если groupObject != null, то результирующее свойство будет принимать на входы в том числе и группирующие интерфейсы.
     * Это нужно для того, чтобы можно было создать фильтр по ключам этого groupObject'а
     * <p>
     * Пример исользования есть в {@link #addGCAProp(platform.server.logics.property.group.AbstractGroup, String, String, platform.server.form.entity.GroupObjectEntity, platform.server.logics.linear.LP, Object...)}
     */
    protected LP addGCAProp(AbstractGroup group, String name, String caption, GroupObjectEntity groupObject, LP mainProperty, LP getterProperty) {
        int mainInts[] = consecutiveInts(mainProperty.listInterfaces.size());
        int getterInts[] = consecutiveInts(getterProperty.listInterfaces.size());

        return addGCAProp(group, name, caption, groupObject, mainProperty, mainInts, getterProperty, getterInts, mainInts);
    }

    /**
     * Создаёт свойство для группового изменения
     * Пример:
     * <pre>
     *   LP ценаПоставкиТовара = Свойство(Товар)
     *   LP ценаПродажиТовара = Свойство(Магазин, Товар)
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
    protected LP addGCAProp(AbstractGroup group, String name, String caption, GroupObjectEntity groupObject, LP mainProperty, Object... params) {
        assert params.length > 0;

        int mainIntCnt = mainProperty.listInterfaces.size();

        LP getterProperty = (LP) params[mainIntCnt];

        int getterIntCnt = getterProperty.listInterfaces.size();

        int groupIntCnt = params.length - mainIntCnt - 1 - getterIntCnt;

        int mainInts[] = new int[mainIntCnt];
        int getterInts[] = new int[getterIntCnt];
        int groupInts[] = new int[groupIntCnt];

        for (int i = 0; i < mainIntCnt; ++i) {
            mainInts[i] = (Integer)params[i] - 1;
        }

        for (int i = 0; i < getterIntCnt; ++i) {
            getterInts[i] = (Integer)params[mainIntCnt + 1 + i] - 1;
        }

        for (int i = 0; i < groupIntCnt; ++i) {
            groupInts[i] = (Integer)params[mainIntCnt + 1 + getterIntCnt] - 1;
        }

        return addGCAProp(group, name, caption, groupObject, mainProperty, mainInts, getterProperty, getterInts, groupInts);
    }

    private LP addGCAProp(AbstractGroup group, String name, String caption, GroupObjectEntity groupObject, LP mainProperty, int[] mainInts, LP getterProperty, int[] getterInts, int[] groupInts) {
        return addProperty(group, new LP<ClassPropertyInterface>(
                new GroupChangeActionProperty(name, caption, groupObject,
                                              mainProperty, mainInts, getterProperty, getterInts, groupInts)));
    }

    public void showIf(FormEntity<?> form, LP[] properties, LP ifProperty, ObjectEntity... objects) {
        for (LP property : properties)
            showIf(form, property, ifProperty, objects);
    }

    public void showIf(FormEntity<?> form, LP property, LP ifProperty, ObjectEntity... objects) {
        PropertyObjectEntity hideCaptionProp = form.addPropertyObject(addHideCaptionProp(property, ifProperty), objects);
        for (PropertyDrawEntity propertyDraw : form.getProperties(property.property)) {
            propertyDraw.propertyCaption = hideCaptionProp;
        }
    }

    public void showIf(FormEntity<?> form, PropertyDrawEntity property, LP ifProperty, PropertyObjectInterfaceEntity... objects) {
        property.propertyCaption = form.addPropertyObject(addHideCaptionProp(property.propertyObject.property, ifProperty), objects);
    }

    private <P extends PropertyInterface, L extends PropertyInterface> LP mapLProp(AbstractGroup group, boolean persistent, PropertyMapImplement<L, P> implement, List<P> listInterfaces) {
        return addProperty(group, persistent, new LP<L>(implement.property, BaseUtils.mapList(listInterfaces, BaseUtils.reverse(implement.mapping))));
    }

    private <P extends PropertyInterface, L extends PropertyInterface> LP mapLProp(AbstractGroup group, boolean persistent, PropertyMapImplement<L, P> implement, LP<P> property) {
        return mapLProp(group, persistent, implement, property.listInterfaces);
    }

    private <P extends PropertyInterface, L extends PropertyInterface> LP mapLGProp(AbstractGroup group, PropertyImplement<L, PropertyInterfaceImplement<P>> implement, List<PropertyInterfaceImplement<P>> listImplements) {
        return mapLGProp(group, false, implement, listImplements);
    }

    private <P extends PropertyInterface, L extends PropertyInterface> LP mapLGProp(AbstractGroup group, boolean persistent, PropertyImplement<L, PropertyInterfaceImplement<P>> implement, List<PropertyInterfaceImplement<P>> listImplements) {
        return addProperty(group, persistent, new LP<L>(implement.property, BaseUtils.mapList(listImplements, BaseUtils.reverse(implement.mapping))));
    }

    private <P extends PropertyInterface> LP mapLGProp(AbstractGroup group, boolean persistent, GroupProperty property, List<PropertyInterfaceImplement<P>> listImplements) {
        return mapLGProp(group, persistent, new PropertyImplement<GroupProperty.Interface<P>, PropertyInterfaceImplement<P>>(property, property.getMapInterfaces()), listImplements);
    }

    protected <P extends PropertyInterface> LP addOProp(String caption, PartitionType partitionType, LP<P> sum, boolean ascending, boolean includeLast, int partNum, Object... params) {
        return addOProp(genSID(), caption, partitionType, sum, ascending, includeLast, partNum, params);
    }

    protected <P extends PropertyInterface> LP addOProp(String name, String caption, PartitionType partitionType, LP<P> sum, boolean ascending, boolean includeLast, int partNum, Object... params) {
        return addOProp((AbstractGroup) null, name, caption, partitionType, sum, ascending, includeLast, partNum, params);
    }

    protected <P extends PropertyInterface> LP addOProp(AbstractGroup group, String caption, PartitionType partitionType, LP<P> sum, boolean ascending, boolean includeLast, int partNum, Object... params) {
        return addOProp(group, genSID(), caption, partitionType, sum, ascending, includeLast, partNum, params);
    }

    protected <P extends PropertyInterface> LP addOProp(AbstractGroup group, String name, String caption, PartitionType partitionType, LP<P> sum, boolean ascending, boolean includeLast, int partNum, Object... params) {
        return addOProp(group, name, false, caption, sum, partitionType, ascending, includeLast, partNum, params);
    }

    protected <P extends PropertyInterface> LP addOProp(AbstractGroup group, String name, boolean persistent, String caption, LP<P> sum, PartitionType partitionType, boolean ascending, boolean includeLast, int partNum, Object... params) {
        List<LI> li = readLI(params);

        Collection<PropertyInterfaceImplement<P>> partitions = mapLI(li.subList(0, partNum), sum.listInterfaces);
        OrderedMap<PropertyInterfaceImplement<P>, Boolean> orders = new OrderedMap<PropertyInterfaceImplement<P>, Boolean>(mapLI(li.subList(partNum, li.size()), sum.listInterfaces), !ascending);

        PropertyMapImplement<?, P> orderProperty;
        orderProperty = DerivedProperty.createOProp(name, caption, partitionType, sum.property, partitions, orders, includeLast);

        return mapLProp(group, persistent, orderProperty, sum);
    }

    protected <P extends PropertyInterface> LP addOProp(AbstractGroup group, String name, boolean persistent, String caption, PartitionType partitionType, boolean ascending, boolean includeLast, int partNum, Object... params) {
        List<PropertyInterface> interfaces = genInterfaces(getIntNum(params));
        List<PropertyInterfaceImplement<PropertyInterface>> listImplements = mapLI(readLI(params), interfaces);

        List<PropertyInterfaceImplement<PropertyInterface>> mainProp = listImplements.subList(0, 1);
        Collection<PropertyInterfaceImplement<PropertyInterface>> partitions = listImplements.subList(1, partNum + 1);
        OrderedMap<PropertyInterfaceImplement<PropertyInterface>, Boolean> orders =
                new OrderedMap<PropertyInterfaceImplement<PropertyInterface>, Boolean>(listImplements.subList(partNum + 1, listImplements.size()), !ascending);

        return mapLProp(group, persistent, DerivedProperty.createOProp(name, caption, partitionType, interfaces, mainProp, partitions, orders, includeLast), interfaces);
    }

    protected <P extends PropertyInterface> LP addRProp(AbstractGroup group, String name, boolean persistent, String caption, Cycle cycle, List<Integer> resInterfaces, Map<Integer, Integer> mapPrev, Object... params) {
        int innerCount = getIntNum(params);
        List<PropertyInterface> innerInterfaces = genInterfaces(innerCount);
        List<PropertyInterfaceImplement<PropertyInterface>> listImplement = mapLI(readLI(params), innerInterfaces);

        List<RecursiveProperty.Interface> interfaces = RecursiveProperty.getInterfaces(resInterfaces.size());
        Map<RecursiveProperty.Interface, PropertyInterface> mapInterfaces = new HashMap<RecursiveProperty.Interface, PropertyInterface>();
        int index = 0;
        for (int resInterface : resInterfaces) {
            mapInterfaces.put(interfaces.get(index++), innerInterfaces.get(resInterface));
        }
        Map<PropertyInterface, PropertyInterface> mapIterate = new HashMap<PropertyInterface, PropertyInterface>();
        for (Map.Entry<Integer, Integer> entry : mapPrev.entrySet()) { // старые на новые
            mapIterate.put(innerInterfaces.get(entry.getKey()), innerInterfaces.get(entry.getValue()));
        }

        PropertyMapImplement<?, PropertyInterface> initial = (PropertyMapImplement<?, PropertyInterface>) listImplement.get(0);
        PropertyMapImplement<?, PropertyInterface> step = (PropertyMapImplement<?, PropertyInterface>) listImplement.get(1);

        boolean convertToLogical = false;
        assert initial.property.getType() instanceof IntegralClass == (step.property.getType() instanceof IntegralClass);
        if(!(initial.property.getType() instanceof IntegralClass) && (cycle == Cycle.NO || (cycle==Cycle.IMPOSSIBLE && persistent))) {
            PropertyMapImplement<?, PropertyInterface> one = createStatic(1, LongClass.instance);
            initial = createAnd(innerInterfaces, one, initial);
            step = createAnd(innerInterfaces, one, step);
            convertToLogical = true;
        }

        RecursiveProperty<PropertyInterface> property = new RecursiveProperty<PropertyInterface>(name, caption, interfaces, cycle,
                mapInterfaces, mapIterate, initial, step);
        if(cycle==Cycle.NO)
            addConstraint(property.getConstrainedProperty(), false);

        LP result = new LP<RecursiveProperty.Interface>(property, interfaces);
//        if (convertToLogical)
//            return addJProp(group, name, false, caption, baseLM.notZero, directLI(addProperty(null, persistent, result)));
//        else
            return addProperty(group, persistent, result);
    }

    protected <R extends PropertyInterface, L extends PropertyInterface> LP addUGProp(AbstractGroup group, String caption, boolean ascending, LP<R> restriction, LP<L> ungroup, Object... params) {
        return addUGProp(group, genSID(), caption, ascending, restriction, ungroup, params);
    }

    protected <R extends PropertyInterface, L extends PropertyInterface> LP addUGProp(AbstractGroup group, String name, String caption, boolean ascending, LP<R> restriction, LP<L> ungroup, Object... params) {
        return addUGProp(group, name, false, caption, ascending, restriction, ungroup, params);
    }

    protected <R extends PropertyInterface, L extends PropertyInterface> LP addUGProp(AbstractGroup group, String name, boolean persistent, String caption, boolean ascending, LP<R> restriction, LP<L> ungroup, Object... params) {
        return addUGProp(group, name, persistent, caption, ascending, restriction, ungroup, params);
    }

    protected <R extends PropertyInterface, L extends PropertyInterface> LP addUGProp(AbstractGroup group, String name, boolean persistent, boolean over, String caption, boolean ascending, LP<R> restriction, LP<L> ungroup, Object... params) {
        return addUGProp(group, name, persistent, over, caption, restriction.listInterfaces.size(), ascending, ungroup, add(directLI(restriction), params));
    }

    protected <L extends PropertyInterface> LP addUGProp(AbstractGroup group, String name, boolean persistent, boolean over, String caption, int intCount, boolean ascending, LP<L> ungroup, Object... params) {
        int partNum = ungroup.listInterfaces.size();
        List<PropertyInterface> innerInterfaces = genInterfaces(intCount);
        List<PropertyInterfaceImplement<PropertyInterface>> listImplements = readImplements(innerInterfaces, params);
        PropertyInterfaceImplement<PropertyInterface> restriction = listImplements.get(0);
        Map<L, PropertyInterfaceImplement<PropertyInterface>> groupImplement = new HashMap<L, PropertyInterfaceImplement<PropertyInterface>>();
        for (int i = 0; i < partNum; i++)
            groupImplement.put(ungroup.listInterfaces.get(i), listImplements.get(i+1));
        OrderedMap<PropertyInterfaceImplement<PropertyInterface>, Boolean> orders =
                new OrderedMap<PropertyInterfaceImplement<PropertyInterface>, Boolean>(listImplements.subList(partNum + 1, listImplements.size()), !ascending);

        return mapLProp(group, persistent, DerivedProperty.createUGProp(name, caption, innerInterfaces, new PropertyImplement<L, PropertyInterfaceImplement<PropertyInterface>>(ungroup.property, groupImplement), orders, restriction, over), innerInterfaces);
    }

    protected <R extends PropertyInterface, L extends PropertyInterface> LP addPGProp(AbstractGroup group, String name, boolean persistent, int roundlen, boolean roundfirst, String caption, LP<R> proportion, LP<L> ungroup, Object... params) {
        return addPGProp(group, name, persistent, roundlen, roundfirst, caption, proportion.listInterfaces.size(), true, ungroup, add(add(directLI(proportion), params), getParams(proportion)));
    }

    protected <L extends PropertyInterface> LP addPGProp(AbstractGroup group, String name, boolean persistent, int roundlen, boolean roundfirst, String caption, int intCount, boolean ascending, LP<L> ungroup, Object... params) {
        int partNum = ungroup.listInterfaces.size();
        List<PropertyInterface> innerInterfaces = genInterfaces(intCount);
        List<PropertyInterfaceImplement<PropertyInterface>> listImplements = readImplements(innerInterfaces, params);
        PropertyInterfaceImplement<PropertyInterface> proportion = listImplements.get(0);
        Map<L, PropertyInterfaceImplement<PropertyInterface>> groupImplement = new HashMap<L, PropertyInterfaceImplement<PropertyInterface>>();
        for (int i = 0; i < partNum; i++)
            groupImplement.put(ungroup.listInterfaces.get(i), listImplements.get(i+1));
        OrderedMap<PropertyInterfaceImplement<PropertyInterface>, Boolean> orders =
                new OrderedMap<PropertyInterfaceImplement<PropertyInterface>, Boolean>(listImplements.subList(partNum + 1, listImplements.size()), !ascending);

        return mapLProp(group, persistent, DerivedProperty.createPGProp(name, caption, roundlen, roundfirst, baseLM.baseClass, innerInterfaces, new PropertyImplement<L, PropertyInterfaceImplement<PropertyInterface>>(ungroup.property, groupImplement), proportion, orders), innerInterfaces);
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

    protected LP addSGProp(LP groupProp, Object... params) {
        return addSGProp(baseLM.privateGroup, "sys", groupProp, params);
    }

    protected LP addSGProp(String caption, LP groupProp, Object... params) {
        return addSGProp((AbstractGroup) null, caption, groupProp, params);
    }

    protected LP addSGProp(AbstractGroup group, String caption, LP groupProp, Object... params) {
        return addSGProp(group, genSID(), caption, groupProp, params);
    }

    protected LP addSGProp(String name, String caption, LP groupProp, Object... params) {
        return addSGProp(name, false, caption, groupProp, params);
    }

    protected LP addSGProp(String name, boolean persistent, String caption, LP groupProp, Object... params) {
        return addSGProp(null, name, persistent, caption, groupProp, params);
    }

    protected LP addSGProp(AbstractGroup group, String name, String caption, LP groupProp, Object... params) {
        return addSGProp(group, name, false, caption, groupProp, params);
    }

    protected LP addSGProp(AbstractGroup group, boolean persistent, String caption, LP groupProp, Object... params) {
        return addSGProp(group, genSID(), persistent, caption, groupProp, params);
    }

    protected LP addSGProp(AbstractGroup group, String name, boolean persistent, String caption, LP groupProp, Object... params) {
        return addSGProp(group, name, persistent, false, caption, groupProp, params);
    }

    protected <T extends PropertyInterface> LP addSGProp(AbstractGroup group, String name, boolean persistent, boolean notZero, String caption, LP<T> groupProp, Object... params) {
        return addSGProp(group, name, persistent, notZero, caption, groupProp, readImplements(groupProp.listInterfaces, params));
    }

    private <T extends PropertyInterface> LP addSGProp(AbstractGroup group, String name, boolean persistent, boolean notZero, String caption, LP<T> groupProp, List<PropertyInterfaceImplement<T>> listImplements) {
        return addSGProp(group, name, persistent, notZero, caption, groupProp.listInterfaces, add(groupProp.property.getImplement(), listImplements));
    }

    protected List<PropertyInterface> genInterfaces(int interfaces) {
        List<PropertyInterface> innerInterfaces = new ArrayList<PropertyInterface>();
        for(int i=0;i<interfaces;i++)
            innerInterfaces.add(new PropertyInterface(i));
        return innerInterfaces;
    }

    protected LP addSGProp(AbstractGroup group, String name, boolean persistent, boolean notZero, String caption, int interfaces, Object... params) {
        List<PropertyInterface> innerInterfaces = genInterfaces(interfaces);
        return addSGProp(group, name, persistent, notZero, caption, innerInterfaces, readImplements(innerInterfaces, params));
    }

    private <T extends PropertyInterface> LP addSGProp(AbstractGroup group, String name, boolean persistent, boolean notZero, String caption, List<T> innerInterfaces, List<PropertyInterfaceImplement<T>> implement) {
        List<PropertyInterfaceImplement<T>> listImplements = implement.subList(1, implement.size());
                SumGroupProperty<T> property = new SumGroupProperty<T>(name, caption, innerInterfaces, listImplements, implement.get(0));


        LP lp = mapLGProp(group, persistent, property, listImplements);
        lp.listGroupInterfaces = innerInterfaces;
        return lp;
    }

    public <T extends PropertyInterface> LP addOGProp(String caption, GroupType type, int numOrders, boolean descending, LP<T> groupProp, Object... params) {
        return addOGProp(genSID(), false, caption, type, numOrders, descending, groupProp, params);
    }
    public <T extends PropertyInterface> LP addOGProp(String name, boolean persist, String caption, GroupType type, int numOrders, boolean descending, LP<T> groupProp, Object... params) {
        return addOGProp(null, name, persist, caption, type, numOrders, descending, groupProp, params);
    }
    public <T extends PropertyInterface> LP addOGProp(AbstractGroup group, String name, boolean persist, String caption, GroupType type, int numOrders, boolean descending, LP<T> groupProp, Object... params) {
        return addOGProp(group, name, persist, caption, type, numOrders, descending, groupProp.listInterfaces, add(groupProp.property.getImplement(), readImplements(groupProp.listInterfaces, params)));
    }
    public <T extends PropertyInterface> LP addOGProp(AbstractGroup group, String name, boolean persist, String caption, GroupType type, int numOrders, boolean descending, int interfaces, Object... params) {
        List<PropertyInterface> innerInterfaces = genInterfaces(interfaces);
        return addOGProp(group, name, persist, caption, type, numOrders, descending, innerInterfaces, readImplements(innerInterfaces, params));
    }
    public <T extends PropertyInterface> LP addOGProp(AbstractGroup group, String name, boolean persist, String caption, GroupType type, int numOrders, boolean descending, Collection<T> innerInterfaces, List<PropertyInterfaceImplement<T>> listImplements) {
        int numExprs = type.numExprs();
        List<PropertyInterfaceImplement<T>> props = listImplements.subList(0, numExprs);
        OrderedMap<PropertyInterfaceImplement<T>, Boolean> orders = new OrderedMap<PropertyInterfaceImplement<T>, Boolean>(listImplements.subList(numExprs, numExprs + numOrders), descending);
        List<PropertyInterfaceImplement<T>> groups = listImplements.subList(numExprs + numOrders, listImplements.size());
        OrderGroupProperty<T> property = new OrderGroupProperty<T>(name, caption, innerInterfaces, groups, props, type, orders);

        return mapLGProp(group, persist, property, groups);
    }


    protected LP addMGProp(LP groupProp, Object... params) {
        return addMGProp("sys", groupProp, params);
    }

    protected LP addMGProp(String caption, LP groupProp, Object... params) {
        return addMGProp(baseLM.privateGroup, genSID(), caption, groupProp, params);
    }

    protected LP addMGProp(String name, String caption, LP groupProp, Object... params) {
        return addMGProp(null, name, caption, groupProp, params);
    }

    protected LP addMGProp(AbstractGroup group, String name, String caption, LP groupProp, Object... params) {
        return addMGProp(group, name, false, caption, groupProp, params);
    }

    protected LP addMGProp(AbstractGroup group, boolean persist, String caption, LP groupProp, Object... params) {
        return addMGProp(groupProp, genSID(), persist, caption, groupProp, params);
    }

    protected LP addMGProp(AbstractGroup group, String name, boolean persist, String caption, LP groupProp, Object... params) {
        return addMGProp(group, name, persist, caption, false, groupProp, params);
    }

    protected LP addMGProp(String caption, boolean min, LP groupProp, Object... params) {
        return addMGProp(genSID(), false, caption, min, groupProp, params);
    }

    protected LP addMGProp(String name, boolean persist, String caption, boolean min, LP groupProp, Object... params) {
        return addMGProp(null, name, persist, caption, min, groupProp, params);
    }

    protected LP addMGProp(AbstractGroup group, String name, boolean persist, String caption, boolean min, LP groupProp, Object... params) {
        return addMGProp(group, name, persist, caption, min, groupProp.listInterfaces.size(), add(directLI(groupProp), params));
    }

    protected LP addMGProp(AbstractGroup group, String name, boolean persist, String caption, boolean min, int interfaces, Object... params) {
        return addMGProp(group, persist, new String[]{name}, new String[]{caption}, 1, min, interfaces, params)[0];
    }

    protected <T extends PropertyInterface> LP[] addMGProp(AbstractGroup group, String[] names, String[] captions, int extra, LP<T> groupProp, Object... params) {
        return addMGProp(group, false, names, captions, extra, groupProp, params);
    }

    protected <T extends PropertyInterface> LP[] addMGProp(AbstractGroup group, boolean persist, String[] names, String[] captions, int extra, LP<T> groupProp, Object... params) {
        return addMGProp(group, persist, names, captions, extra + 1, false, groupProp.listInterfaces.size(), add(directLI(groupProp), params));
    }

    protected LP[] addMGProp(AbstractGroup group, boolean persist, String[] names, String[] captions, int exprs, boolean min, int interfaces, Object... params) {
        List<PropertyInterface> innerInterfaces = genInterfaces(interfaces);
        return addMGProp(group, persist, names, captions, exprs, min, innerInterfaces, readImplements(innerInterfaces, params));
    }

    protected <T extends PropertyInterface> LP[] addMGProp(AbstractGroup group, boolean persist, String[] names, String[] captions, int exprs, boolean min, List<T> listInterfaces, List<PropertyInterfaceImplement<T>> listImplements) {
        LP[] result = new LP[exprs];

        Collection<Property> overridePersist = new ArrayList<Property>();

        List<PropertyInterfaceImplement<T>> groupImplements = listImplements.subList(exprs, listImplements.size());
        List<PropertyImplement<?, PropertyInterfaceImplement<T>>> mgProps = DerivedProperty.createMGProp(names, captions, listInterfaces, baseLM.baseClass,
                listImplements.subList(0, exprs), new HashSet<PropertyInterfaceImplement<T>>(groupImplements), overridePersist, min);

        for (int i = 0; i < mgProps.size(); i++)
            result[i] = mapLGProp(group, mgProps.get(i), groupImplements);

        if (persist) {
            if (overridePersist.size() > 0) {
                for (Property property : overridePersist)
                    addProperty(null, true, new LP(property));
            } else
                for (int i = 0; i < result.length; i++)
                    addPersistent(result[i]);
        }

        return result;
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LP addCGProp(AbstractGroup group, String name, String caption, LP<T> groupProp, LP<P> dataProp, Object... params) {
        return addCGProp(group, true, name, caption, groupProp, dataProp, params);
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LP addCGProp(AbstractGroup group, String name, boolean persistent, String caption, LP<T> groupProp, LP<P> dataProp, Object... params) {
        return addCGProp(group, true, name, persistent, caption, groupProp, dataProp, params);
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LP addCGProp(AbstractGroup group, boolean checkChange, String name, String caption, LP<T> groupProp, LP<P> dataProp, Object... params) {
        return addCGProp(group, checkChange, name, false, caption, groupProp, dataProp, params);
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LP addCGProp(AbstractGroup group, boolean checkChange, String name, boolean persistent, String caption, LP<T> groupProp, LP<P> dataProp, Object... params) {
        return addCGProp(group, checkChange, name, persistent, caption, dataProp, groupProp.listInterfaces, add(groupProp.property.getImplement(), readImplements(groupProp.listInterfaces, params)));
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LP addCGProp(AbstractGroup group, boolean checkChange, String name, boolean persistent, String caption, LP<P> dataProp, int interfaces, Object... params) {
        List<PropertyInterface> innerInterfaces = genInterfaces(interfaces);
        return addCGProp(group, checkChange, name, persistent, caption, dataProp, innerInterfaces, readImplements(innerInterfaces, params));
    }
    
    protected <T extends PropertyInterface, P extends PropertyInterface> LP addCGProp(AbstractGroup group, boolean checkChange, String name, boolean persistent, String caption, LP<P> dataProp, List<T> innerInterfaces, List<PropertyInterfaceImplement<T>> listImplements) {
        CycleGroupProperty<T, P> property = new CycleGroupProperty<T, P>(name, caption, innerInterfaces, listImplements.subList(1, listImplements.size()), listImplements.get(0), dataProp == null ? null : dataProp.property);

        // нужно добавить ограничение на уникальность
        addConstraint(property.getConstrainedProperty(), checkChange);

        return mapLGProp(group, persistent, property, listImplements.subList(1, listImplements.size()));
    }
    
//    protected static <T extends PropertyInterface<T>> AggregateGroupProperty create(String sID, String caption, Property<T> property, T aggrInterface, Collection<PropertyMapImplement<?, T>> groupProps) {

    protected LP addAGProp(String name, String caption, LP... props) {
        return addAGProp(null, name, caption, props);
    }

    protected LP addAGProp(String name, String caption, boolean noConstraint, LP... props) {
        return addAGProp(null, name, caption, noConstraint, props);
    }

    protected LP addAGProp(AbstractGroup group, String name, String caption, LP... props) {
        return addAGProp(group, name, caption, false, props);
    }

    protected LP addAGProp(AbstractGroup group, String name, String caption, boolean noConstraint, LP... props) {
        ClassWhere<Integer> classWhere = ClassWhere.<Integer>STATIC(true);
        for (LP<?> prop : props)
            classWhere = classWhere.and(prop.getClassWhere());
        return addAGProp(group, name, caption, noConstraint, (CustomClass) BaseUtils.singleValue(classWhere.getCommonParent(Collections.singleton(1))), props);
    }

    protected LP addAGProp(String name, String caption, CustomClass customClass, LP... props) {
        return addAGProp(null, name, caption, customClass, props);
    }

    protected LP addAGProp(AbstractGroup group, String name, String caption, CustomClass customClass, LP... props) {
        return addAGProp(group, name, caption, false, customClass, props);
    }

    protected LP addAGProp(AbstractGroup group, String name, String caption, boolean noConstraint, CustomClass customClass, LP... props) {
        return addAGProp(group, false, name, false, caption, noConstraint, customClass, props);
    }

    protected LP addAGProp(AbstractGroup group, boolean checkChange, String name, boolean persistent, String caption, CustomClass customClass, LP... props) {
        return addAGProp(group, checkChange, name, persistent, caption, false, customClass, props);
    }

    protected LP addAGProp(AbstractGroup group, boolean checkChange, String name, boolean persistent, String caption, boolean noConstraint, CustomClass customClass, LP... props) {
        if(props.length==1)
            props[0].property.aggProp = true;
        return addAGProp(group, checkChange, name, persistent, caption, noConstraint, is(customClass), add(1, getUParams(props, 0)));
    }

    protected <T extends PropertyInterface<T>> LP addAGProp(String name, String caption, LP<T> lp, int aggrInterface, Object... props) {
        return addAGProp(name, false, caption, lp, aggrInterface, props);
    }

    protected <T extends PropertyInterface> LP addAGProp(AbstractGroup group, String name, String caption, LP<T> lp, int aggrInterface, Object... props) {
        return addAGProp(group, false, name, false, caption, lp, aggrInterface, props);
    }

    protected <T extends PropertyInterface<T>> LP addAGProp(String name, boolean persistent, String caption, LP<T> lp, int aggrInterface, Object... props) {
        return addAGProp(null, false, name, persistent, caption, lp, aggrInterface, props);
    }

    protected <T extends PropertyInterface<T>> LP addAGProp(AbstractGroup group, String name, boolean persistent, String caption, LP<T> lp, int aggrInterface, Object... props) {
        return addAGProp(group, false, name, persistent, caption, lp, aggrInterface, props);
    }

    protected <T extends PropertyInterface<T>> LP addAGProp(AbstractGroup group, boolean checkChange, String name, boolean persistent, String caption, LP<T> lp, int aggrInterface, Object... props) {
        return addAGProp(group, checkChange, name, persistent, caption, false, lp, add(aggrInterface, props));
    }

    protected <T extends PropertyInterface<T>> LP addAGProp(AbstractGroup group, boolean checkChange, String name, boolean persistent, String caption, boolean noConstraint, LP<T> lp, Object... props) {
        List<PropertyInterfaceImplement<T>> readImplements = readImplements(lp.listInterfaces, props);
        T aggrInterface = (T) readImplements.get(0);
        List<PropertyInterfaceImplement<T>> groupImplements = readImplements.subList(1, readImplements.size());
        List<PropertyInterfaceImplement<T>> fullInterfaces = BaseUtils.mergeList(groupImplements, BaseUtils.removeList(lp.listInterfaces, aggrInterface));
        return addAGProp(group, checkChange, name, persistent, caption, noConstraint, lp.listInterfaces, mergeList(toList(aggrInterface, lp.property.getImplement()), fullInterfaces));
    }

    protected LP addAGProp(AbstractGroup group, boolean checkChange, String name, boolean persistent, String caption, boolean noConstraint, int interfaces, Object... props) {
        List<PropertyInterface> innerInterfaces = genInterfaces(interfaces);
        return addAGProp(group, checkChange, name, persistent, caption, noConstraint, innerInterfaces, readImplements(innerInterfaces, props));
    }

    protected <T extends PropertyInterface<T>, I extends PropertyInterface> LP addAGProp(AbstractGroup group, boolean checkChange, String name, boolean persistent, String caption, boolean noConstraint, List<T> innerInterfaces, List<PropertyInterfaceImplement<T>> listImplements) {
        T aggrInterface = (T) listImplements.get(0);
        PropertyInterfaceImplement<T> whereProp = listImplements.get(1);
        List<PropertyInterfaceImplement<T>> groupImplements = listImplements.subList(2, listImplements.size());

        AggregateGroupProperty<T> aggProp = AggregateGroupProperty.create(name, caption, innerInterfaces, whereProp, aggrInterface, groupImplements);
        return addAGProp(group, checkChange, persistent, noConstraint, aggProp, groupImplements);
    }

    // чисто для generics
    private <T extends PropertyInterface<T>> LP addAGProp(AbstractGroup group, boolean checkChange, boolean persistent, boolean noConstraint, AggregateGroupProperty<T> property, List<PropertyInterfaceImplement<T>> listImplements) {
        // нужно добавить ограничение на уникальность
        if(!noConstraint)
            addConstraint(property.getConstrainedProperty(), checkChange);

        return mapLGProp(group, persistent, property, listImplements);
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LP addDGProp(int orders, boolean ascending, LP<T> groupProp, Object... params) {
        return addDGProp(baseLM.privateGroup, "sys", orders, ascending, groupProp, params);
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LP addDGProp(AbstractGroup group, String caption, int orders, boolean ascending, LP<T> groupProp, Object... params) {
        return addDGProp(group, genSID(), caption, orders, ascending, groupProp, params);
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LP addDGProp(AbstractGroup group, String name, String caption, int orders, boolean ascending, LP<T> groupProp, Object... params) {
        return addDGProp(group, name, false, caption, orders, ascending, groupProp, params);
    }

    protected <T extends PropertyInterface> LP addDGProp(AbstractGroup group, String name, boolean persistent, String caption, int orders, boolean ascending, LP<T> groupProp, Object... params) {
        return addDGProp(group, name, persistent, caption, orders, ascending, false, groupProp, params);
    }
    
    protected <T extends PropertyInterface> LP addDGProp(AbstractGroup group, String name, boolean persistent, String caption, int orders, boolean ascending, boolean over, LP<T> groupProp, Object... params) {
        List<PropertyInterfaceImplement<T>> listImplements = readImplements(groupProp.listInterfaces, params);
        int intNum = listImplements.size();
        LP result = addSGProp(group, name, persistent, false, caption, groupProp, listImplements.subList(0, intNum - orders - 1));
        result.setDG(ascending, over, listImplements.subList(intNum - orders - 1, intNum));
        return result;
    }

    protected LP addUProp(AbstractGroup group, String name, String caption, Union unionType, Object... params) {
        return addUProp(group, name, false, caption, unionType, params);
    }

    protected LP addUProp(AbstractGroup group, String caption, Union unionType, Object... params) {
        return addUProp(group, genSID(), false, caption, unionType, params);
    }

    protected LP addUProp(AbstractGroup group, String name, boolean persistent, String caption, Union unionType, Object... params) {

        int intNum = ((LP) params[unionType == Union.SUM ? 1 : 0]).listInterfaces.size();

        List<UnionProperty.Interface> listInterfaces = UnionProperty.getInterfaces(intNum);
        List<PropertyMapImplement<?, UnionProperty.Interface>> listOperands = new ArrayList<PropertyMapImplement<?, UnionProperty.Interface>>();
        Map<PropertyMapImplement<?, UnionProperty.Interface>, Integer> mapOperands = new HashMap<PropertyMapImplement<?, UnionProperty.Interface>, Integer>();
        String delimeter = null;
        int extra = 0;

        switch (unionType) {
            case SUM:
                extra = 1;
                break;
            case STRING_AGG:
                delimeter = (String) params[params.length-1];
                params = copyOfRange(params, 0, params.length - 1);
                break;
        }

        for (int i = 0; i < params.length / (intNum + 1 + extra); i++) {
            Integer offs = i * (intNum + 1 + extra);
            LP<?> opImplement = (LP) params[offs + extra];
            PropertyMapImplement operand = new PropertyMapImplement(opImplement.property);
            for (int j = 0; j < intNum; j++)
                operand.mapping.put(opImplement.listInterfaces.get(((Integer) params[offs + 1 + extra + j]) - 1), listInterfaces.get(j));

            switch (unionType) {
                case SUM:
                    if (mapOperands.containsKey(operand)) {
                        mapOperands.put(operand, mapOperands.get(operand) + (Integer) params[offs]);
                    } else {
                        mapOperands.put(operand, (Integer) params[offs]);
                    }
                    break;
                default:
                    listOperands.add(operand);
            }
        }

        UnionProperty property = null;
        switch (unionType) {
            case MAX:
                property = new MaxUnionProperty(name, caption, listInterfaces, listOperands);
                break;
            case SUM:
                property = new SumUnionProperty(name, caption, listInterfaces, mapOperands);
                break;
            case OVERRIDE:
                property = new OverrideUnionProperty(name, caption, listInterfaces, listOperands);
                break;
            case XOR:
                property = new XorUnionProperty(name, caption, listInterfaces, listOperands);
                break;
            case EXCLUSIVE:
                property = new ExclusiveUnionProperty(name, caption, listInterfaces, listOperands);
                break;
            case STRING_AGG:
                property = new StringAggUnionProperty(name, caption, listInterfaces, listOperands, delimeter);
                break;
        }

        return addProperty(group, persistent, new LP<UnionProperty.Interface>(property, listInterfaces));
    }

    protected LP addAUProp(AbstractGroup group, String name, boolean persistent, String caption, ValueClass valueClass, ValueClass... interfaces) {
        List<UnionProperty.Interface> listInterfaces = UnionProperty.getInterfaces(interfaces.length);
        return addProperty(group, persistent, new LP<UnionProperty.Interface>(
                new ExclusiveUnionProperty(name, caption, listInterfaces, valueClass, BaseUtils.buildMap(listInterfaces, toList(interfaces))), listInterfaces));
    }

    protected LP addCaseUProp(AbstractGroup group, String name, boolean persistent, String caption, Object... params) {
        List<LI> list = readLI(params);
        int intNum = ((LMI) list.get(1)).lp.listInterfaces.size(); // берем количество интерфейсов у первого case'а

        List<UnionProperty.Interface> listInterfaces = UnionProperty.getInterfaces(intNum);
        List<AbstractCaseUnionProperty.Case> listCases = new ArrayList<AbstractCaseUnionProperty.Case>();
        List<PropertyMapImplement<?, UnionProperty.Interface>> mapImplements = (List<PropertyMapImplement<?, UnionProperty.Interface>>) (List<?>) mapLI(list, listInterfaces);
        for (int i = 0; i < mapImplements.size() / 2; i++)
            listCases.add(new AbstractCaseUnionProperty.Case(mapImplements.get(2 * i), mapImplements.get(2 * i + 1)));
        if (mapImplements.size() % 2 != 0)
            listCases.add(new AbstractCaseUnionProperty.Case(new PropertyMapImplement<PropertyInterface, UnionProperty.Interface>(baseLM.vtrue.property), mapImplements.get(mapImplements.size() - 1)));

        return addProperty(group, persistent, new LP<UnionProperty.Interface>(new CaseUnionProperty(name, caption, listInterfaces, listCases), listInterfaces));
    }

    // объединение классовое (непересекающихся) свойств

    protected LP addCUProp(LP... props) {
        return addCUProp(baseLM.privateGroup, "sys", props);
    }

    protected LP addCUProp(String caption, LP... props) {
        return addCUProp((AbstractGroup) null, caption, props);
    }

    protected LP addCUProp(AbstractGroup group, String caption, LP... props) {
        return addCUProp(group, genSID(), caption, props);
    }

    protected LP addCUProp(String name, String caption, LP... props) {
        return addCUProp(name, false, caption, props);
    }

    protected LP addCUProp(String name, boolean persistent, String caption, LP... props) {
        return addCUProp(null, name, persistent, caption, props);
    }

    protected LP addCUProp(AbstractGroup group, String name, String caption, LP... props) {
        return addCUProp(group, name, false, caption, props);
    }

    protected LP addCUProp(AbstractGroup group, String name, boolean persistent, String caption, LP... props) {
        assert baseLM.checkCUProps.add(props);
        return addXSUProp(group, name, persistent, caption, props);
    }

    // разница

    protected LP addDUProp(LP prop1, LP prop2) {
        return addDUProp(baseLM.privateGroup, "sys", prop1, prop2);
    }

    protected LP addDUProp(String caption, LP prop1, LP prop2) {
        return addDUProp((AbstractGroup) null, caption, prop1, prop2);
    }

    protected LP addDUProp(AbstractGroup group, String caption, LP prop1, LP prop2) {
        return addDUProp(group, genSID(), caption, prop1, prop2);
    }

    protected LP addDUProp(String name, String caption, LP prop1, LP prop2) {
        return addDUProp(null, name, caption, prop1, prop2);
    }

    protected LP addDUProp(String name, boolean persistent, String caption, LP prop1, LP prop2) {
        return addDUProp(null, name, persistent, caption, prop1, prop2);
    }

    protected LP addDUProp(AbstractGroup group, String name, String caption, LP prop1, LP prop2) {
        return addDUProp(group, name, false, caption, prop1, prop2);
    }

    protected LP addDUProp(AbstractGroup group, String name, boolean persistent, String caption, LP prop1, LP prop2) {
        int intNum = prop1.listInterfaces.size();
        Object[] params = new Object[2 * (2 + intNum)];
        params[0] = 1;
        params[1] = prop1;
        for (int i = 0; i < intNum; i++)
            params[2 + i] = i + 1;
        params[2 + intNum] = -1;
        params[3 + intNum] = prop2;
        for (int i = 0; i < intNum; i++)
            params[4 + intNum + i] = i + 1;
        return addUProp(group, name, persistent, caption, Union.SUM, params);
    }

    protected LP addNUProp(LP prop) {
        return addNUProp(baseLM.privateGroup, genSID(), "sys", prop);
    }

    protected LP addNUProp(AbstractGroup group, String name, String caption, LP prop) {
        return addNUProp(group, name, false, caption, prop);
    }

    protected LP addNUProp(AbstractGroup group, String name, boolean persistent, String caption, LP prop) {
        int intNum = prop.listInterfaces.size();
        Object[] params = new Object[2 + intNum];
        params[0] = -1;
        params[1] = prop;
        for (int i = 0; i < intNum; i++)
            params[2 + i] = i + 1;
        return addUProp(group, name, persistent, caption, Union.SUM, params);
    }

    public LP addLProp(LP lp, ValueClass... classes) {
        return addDCProp("LG_" + lp.property.getSID(), ServerResourceBundle.getString("logics.log") + " " + lp.property, 1, lp,
                add(new Object[]{true}, add(getParams(lp), add(new Object[]{addJProp(baseLM.equals2, 1, baseLM.currentSession), lp.listInterfaces.size() + 1}, add(directLI(lp), classes)))));
    }

    // XOR

    protected LP addXorUProp(LP prop1, LP prop2) {
        return addXorUProp(baseLM.privateGroup, genSID(), "sys", prop1, prop2);
    }

    protected LP addXorUProp(AbstractGroup group, String name, String caption, LP prop1, LP prop2) {
        return addXorUProp(group, name, false, caption, prop1, prop2);
    }

    protected LP addXorUProp(AbstractGroup group, String name, boolean persistent, String caption, LP... props) {
        return addUProp(group, name, persistent, caption, Union.XOR, getUParams(props, 0));
//        int intNum = prop1.listInterfaces.size();
//        Object[] params = new Object[2 * (1 + intNum)];
//        params[0] = prop1;
//        for (int i = 0; i < intNum; i++)
//            params[1 + i] = i + 1;
//        params[1 + intNum] = prop2;
//        for (int i = 0; i < intNum; i++)
//            params[2 + intNum + i] = i + 1;
//        return addXSUProp(group, name, persistent, caption, addJProp(andNot1, getUParams(new LP[]{prop1, prop2}, 0)), addJProp(andNot1, getUParams(new LP[]{prop2, prop1}, 0)));
    }

    // IF и IF ELSE

    protected LP addIfProp(LP prop, boolean not, LP ifProp, Object... params) {
        return addIfProp(baseLM.privateGroup, genSID(), "sys", prop, not, ifProp, params);
    }

    protected LP addIfProp(AbstractGroup group, String name, String caption, LP prop, boolean not, LP ifProp, Object... params) {
        return addIfProp(group, name, false, caption, prop, not, ifProp, params);
    }

    protected LP addIfProp(AbstractGroup group, String name, boolean persistent, String caption, LP prop, boolean not, LP ifProp, Object... params) {
        return addJProp(group, name, persistent, caption, and(not), add(getUParams(new LP[]{prop}, 0), add(new LP[]{ifProp}, params)));
    }

    protected LP addIfElseUProp(LP prop1, LP prop2, LP ifProp, Object... params) {
        return addIfElseUProp(baseLM.privateGroup, "sys", prop1, prop2, ifProp, params);
    }

    protected LP addIfElseUProp(AbstractGroup group, String caption, LP prop1, LP prop2, LP ifProp, Object... params) {
        return addIfElseUProp(group, genSID(), caption, prop1, prop2, ifProp, params);
    }

    protected LP addIfElseUProp(AbstractGroup group, String name, String caption, LP prop1, LP prop2, LP ifProp, Object... params) {
        return addIfElseUProp(group, name, false, caption, prop1, prop2, ifProp, params);
    }

    protected LP addIfElseUProp(AbstractGroup group, String name, boolean persistent, String caption, LP prop1, LP prop2, LP ifProp, Object... params) {
        return addXSUProp(group, name, persistent, caption, addIfProp(prop1, false, ifProp, params), addIfProp(prop2, true, ifProp, params));
    }

    // объединение пересекающихся свойств

    protected LP addSUProp(Union unionType, LP... props) {
        return addSUProp(baseLM.privateGroup, "sys", unionType, props);
    }

    protected LP addSUProp(String caption, Union unionType, LP... props) {
        return addSUProp((AbstractGroup) null, caption, unionType, props);
    }

    protected LP addSUProp(AbstractGroup group, String caption, Union unionType, LP... props) {
        return addSUProp(group, genSID(), caption, unionType, props);
    }

    protected LP addSUProp(String name, String caption, Union unionType, LP... props) {
        return addSUProp(name, false, caption, unionType, props);
    }

    protected LP addSUProp(String name, boolean persistent, String caption, Union unionType, LP... props) {
        return addSUProp(null, name, persistent, caption, unionType, props);
    }

    // объединяет разные по классам св-ва

    protected LP addSUProp(AbstractGroup group, String name, String caption, Union unionType, LP... props) {
        return addSUProp(group, name, false, caption, unionType, props);
    }

    protected LP addSUProp(AbstractGroup group, String name, boolean persistent, String caption, Union unionType, LP... props) {
        assert baseLM.checkSUProps.add(props);
        return addUProp(group, name, persistent, caption, unionType, getUParams(props, (unionType == Union.SUM ? 1 : 0)));
    }

    protected LP addSFUProp(AbstractGroup group, String name, String caption, String delimiter, LP... props) {
        return addSFUProp(group, name, false, caption, delimiter, props);
    }
    protected LP addSFUProp(AbstractGroup group, String name, boolean persistent, String caption, String delimiter, LP... props) {
        return addUProp(group, name, persistent, caption, Union.STRING_AGG, add(getUParams(props, 0), delimiter));
    }
    protected LP addXSUProp(AbstractGroup group, String caption, LP... props) {
        return addXSUProp(group, genSID(), caption, props);
    }

    // объединяет заведомо непересекающиеся но не классовые свойства

    protected LP addXSUProp(AbstractGroup group, String name, String caption, LP... props) {
        return addXSUProp(group, name, false, caption, props);
    }

    protected LP addXSUProp(AbstractGroup group, String name, boolean persistent, String caption, LP... props) {
        return addUProp(group, name, persistent, caption, Union.EXCLUSIVE, getUParams(props, 0));
    }

    protected LP[] addMUProp(AbstractGroup group, String[] names, String[] captions, int extra, LP... props) {
        int propNum = props.length / (1 + extra);
        LP[] maxProps = copyOfRange(props, 0, propNum);

        LP[] result = new LP[extra + 1];
        int i = 0;
        do {
            result[i] = addUProp(group, names[i], captions[i], Union.MAX, getUParams(maxProps, 0));
            if (i < extra) { // если не последняя
                for (int j = 0; j < propNum; j++)
                    maxProps[j] = addJProp(baseLM.and1, add(directLI(props[(i + 1) * propNum + j]), directLI( // само свойство
                            addJProp(baseLM.equals2, add(directLI(maxProps[j]), directLI(result[i])))))); // только те кто дает предыдущий максимум
            }
        } while (i++ < extra);
        return result;
    }

    public LP addAProp(ActionProperty property) {
        return addAProp(baseLM.actionGroup, property);
    }

    public LP addAProp(AbstractGroup group, ActionProperty property) {
        property.finalizeInit();
        return addProperty(group, new LP<ClassPropertyInterface>(property));
    }

    protected LP addBAProp(ConcreteCustomClass customClass, LP add) {
        return addAProp(baseLM.new AddBarcodeActionProperty(customClass, add.property, genSID()));
    }

    protected LP addSAProp(LP lp) {
        return addSAProp(baseLM.privateGroup, "sys", lp);
    }

    protected LP addSAProp(AbstractGroup group, String caption, LP lp) {
        return addProperty(group, new LP<ClassPropertyInterface>(new BaseLogicsModule.SeekActionProperty(genSID(), caption, new ValueClass[]{baseLM.baseClass}, lp == null ? null : lp.property)));
    }

    protected LP addMAProp(String message, String caption) {
        return addMAProp(null, message, caption);
    }

    protected LP addMAProp(AbstractGroup group, String message, String caption) {
        int length = message.length();
        return addJProp(addMAProp(caption, length),
                addCProp(StringClass.get(length), message));
    }

    protected LP addMAProp(String caption, int length) {
        return addMAProp(null, caption, length);
    }

    protected LP addMAProp(AbstractGroup group, String caption, int length) {
        return addProperty(group, new LP<ClassPropertyInterface>(new MessageActionProperty(genSID(), caption, length)));
    }

    /**
     * Добавляет action для запуска свойств с мэппингом по порядку, т.е. на входы и выход каждого свойства мэппятся интерфейсы результирующего по порядку
     */
    protected LP addEPAProp(LP... lps) {
        return addEPAProp(EPA_INTERFACE, lps);
    }

    /**
     * Добавляет action для запуска свойств с мэппингом по порядку, т.е. на входы и выход каждого свойства мэппятся интерфейсы результирующего по порядку
     *
     * @param writeType Если != INTERFACE, то мэппятся только входы, без выхода
     */
    protected LP addEPAProp(int writeType, LP... lps) {
        return addEPAProp(genSID(), "sysEPA", writeType, lps);
    }

    /**
     * @see platform.server.logics.LogicsModule#addEPAProp(int, platform.server.logics.linear.LP...)
     */
    protected LP addEPAProp(String sID, String caption, int writeType, LP... lps) {
        int[][] mapInterfaces = new int[lps.length][];
        for (int i = 0; i < lps.length; ++i) {
            LP lp = lps[i];
            mapInterfaces[i] = consecutiveInts(lp.listInterfaces.size() + (writeType == EPA_INTERFACE ? 1 : 0));
        }

        return addEPAProp(sID, caption, writeType, lps, mapInterfaces);
    }

    /**
     * Добавляет action для запуска других свойств.
     * <p/>
     * Мэппиг задаётся перечислением свойств с указанием после каждого номеров интерфейсов результирующего свойства, <p/>
     * которые пойдут на входы и выход данных свойств<p/>
     * Пример 1: addEPAProp(EPA_DEFAULT, userLogin, 1, inUserRole, 1, 2)<p/>
     * Пример 2: addEPAProp(EPA_INTERFACE, userLogin, 1, 3, inUserRole, 1, 2, 4)<p/>
     *
     * @param writeType как мэпить возвращаемые значения.<p/>
     *                  Если значение этого параметра равно EPA_INTERFACE, то мэпиться должны не только выходы, <p/>
     *                  но и вход, при этом номер интерфейса, который пойдёт на вход, должен быть указан последним. <p/>
     *                  Если значение равно EPA_DEFAULT или EPA_NULL, то буду записаны значения по умолчанию или NULL соотв-но
     */
    protected LP addEPAProp(int writeType, Object... params) {
        return addEPAProp(genSID(), "sysEPA", writeType, params);
    }

    /**
     * @see platform.server.logics.LogicsModule#addEPAProp(int, java.lang.Object...)
     */
    protected LP addEPAProp(String sID, String caption, int writeType, Object... params) {
        List<LP> lps = new ArrayList<LP>();
        List<int[]> mapInterfaces = new ArrayList<int[]>();

        int pi = 0;
        while (pi < params.length) {
            assert params[pi] instanceof LP;

            LP lp = (LP) params[pi++];

            int[] propMapInterfaces = new int[lp.listInterfaces.size() + (writeType == EPA_INTERFACE ? 1 : 0)];
            for (int j = 0; j < propMapInterfaces.length; ++j) {
                propMapInterfaces[j] = (Integer) params[pi++] - 1;
            }

            lps.add(lp);
            mapInterfaces.add(propMapInterfaces);
        }
        return addEPAProp(sID, caption, writeType, lps.toArray(new LP[lps.size()]), mapInterfaces.toArray(new int[mapInterfaces.size()][]));
    }

    private LP addEPAProp(String sID, String caption, int writeType, LP[] lps, int[][] mapInterfaces) {
        return addAProp(new ExecutePropertiesActionProperty(sID, caption, writeType, lps, mapInterfaces));
    }

    protected LP addLFAProp(LP lp) {
        return addLFAProp(null, "lfa", lp);
    }

    protected LP addLFAProp(AbstractGroup group, String caption, LP lp) {
        return addProperty(group, new LP<ClassPropertyInterface>(new BaseLogicsModule.LoadActionProperty(genSID(), caption, lp)));
    }

    protected LP addOFAProp(LP lp) {
        return addOFAProp(null, "ofa", lp);
    }

    protected LP addOFAProp(AbstractGroup group, String caption, LP lp) { // обернем сразу в and
        return addProperty(group, new LP<ClassPropertyInterface>(new BaseLogicsModule.OpenActionProperty(genSID(), caption, lp)));
    }


    // params - по каким входам группировать
    protected LP addIAProp(LP dataProperty, Integer... params) {
        return addAProp(new BaseLogicsModule.IncrementActionProperty(genSID(), "sys", dataProperty,
                addMGProp(dataProperty, params),
                params));
    }

    protected LP addAAProp(CustomClass customClass, LP... properties) {
        return addAAProp(customClass, null, null, false, properties);
    }

    /**
     * Пример использования:
     * fileActPricat = addAAProp(pricat, filePricat.property, FileActionClass.getCustomInstance(true));
     * pricat - добавляемый класс
     * filePricat.property - свойство, которое изменяется
     * FileActionClass.getCustomInstance(true) - класс
     * неявный assertion, что класс свойства должен быть совместим с классом Action
     */
    protected LP addAAProp(ValueClass cls, Property propertyValue, DataClass dataClass) {
        return addAProp(new AddObjectActionProperty(genSID(), (CustomClass) cls, propertyValue, dataClass));
    }

    protected LP addAAProp(CustomClass customClass, LP barcode, LP barcodePrefix, boolean quantity, LP... properties) {
        return addAProp(new AddObjectActionProperty(genSID(),
                (barcode != null) ? barcode.property : null, (barcodePrefix != null) ? barcodePrefix.property : null,
                quantity, customClass, LP.toPropertyArray(properties), null, null));
    }

    @IdentityLazy
    public LP getAddObjectAction(CustomClass cls) {
        //"add" + BaseUtils.capitalize(cls.getSID())
        return addAProp(new AddObjectActionProperty(genSID(), cls));
    }

    @IdentityLazy
    public LP getAddedObjectProperty() {
        return addProperty(null, new LP<ClassPropertyInterface>(new SessionDataProperty("addedObject", "Added Object", new ValueClass[0], baseLM.baseClass)));
    }

    @IdentityLazy
    public AnyValuePropertyHolder getChosenObjectProperty() {
        return new AnyValuePropertyHolder(
                addProperty(null, new LP<ClassPropertyInterface>(new SessionDataProperty("chosenObject", "Chosen Object", new ValueClass[]{StringClass.get(100)}, baseLM.baseClass))),
                addProperty(null, new LP<ClassPropertyInterface>(new SessionDataProperty("chosenText", "Chosen Text", new ValueClass[]{StringClass.get(100)}, TextClass.instance))),
                addProperty(null, new LP<ClassPropertyInterface>(new SessionDataProperty("chosenString", "Chosen String", new ValueClass[]{StringClass.get(100)}, StringClass.get(2000)))),
                addProperty(null, new LP<ClassPropertyInterface>(new SessionDataProperty("chosenInt", "Chosen Int", new ValueClass[]{StringClass.get(100)}, IntegerClass.instance))),
                addProperty(null, new LP<ClassPropertyInterface>(new SessionDataProperty("chosenLong", "Chosen Long", new ValueClass[]{StringClass.get(100)}, LongClass.instance))),
                addProperty(null, new LP<ClassPropertyInterface>(new SessionDataProperty("chosenDouble", "Chosen Double", new ValueClass[]{StringClass.get(100)}, DoubleClass.instance))),
                addProperty(null, new LP<ClassPropertyInterface>(new SessionDataProperty("chosenYear", "Chosen Year", new ValueClass[]{StringClass.get(100)}, YearClass.instance))),
                addProperty(null, new LP<ClassPropertyInterface>(new SessionDataProperty("chosenDateTime", "Chosen DateTime", new ValueClass[]{StringClass.get(100)}, DateTimeClass.instance))),
                addProperty(null, new LP<ClassPropertyInterface>(new SessionDataProperty("chosenLogical", "Chosen Logical", new ValueClass[]{StringClass.get(100)}, LogicalClass.instance))),
                addProperty(null, new LP<ClassPropertyInterface>(new SessionDataProperty("chosenDate", "Chosen Date", new ValueClass[]{StringClass.get(100)}, DateClass.instance))),
                addProperty(null, new LP<ClassPropertyInterface>(new SessionDataProperty("chosenTime", "Chosen Time", new ValueClass[]{StringClass.get(100)}, TimeClass.instance)))
        );
    }

    @IdentityLazy
    public LP getFormResultProperty() {
        return addProperty(null, new LP<ClassPropertyInterface>(new SessionDataProperty("formResult", "Form Result", new ValueClass[0], baseLM.formResult)));
    }

    @IdentityLazy
    public LP getSimpleAddObjectAction(CustomClass cls) {
        return addAProp(new SimpleAddObjectActionProperty(genSID(), cls, baseLM.getAddedObjectProperty()));
    }

    @IdentityLazy
    protected LP getAddObjectActionWithClassCheck(CustomClass baseClass, ValueClass checkClass) {
        LP addObjectAction = getAddObjectAction(baseClass);
        return addJProp(addObjectAction.property.caption, baseLM.and1, addObjectAction, is(checkClass), 1);
    }

    @IdentityLazy
    public LP getAddFormAction(CustomClass cls, boolean session) {
        ClassFormEntity form = cls.getEditForm(baseLM);

        LP addObjectAction = getAddObjectAction(cls);
        LP property = addMFAProp(actionGroup, "add" + (session ? "Session" : "") + "Form" + BaseUtils.capitalize(cls.getSID()), ServerResourceBundle.getString("logics.add"), //+ "(" + cls + ")",
                                form.form, new ObjectEntity[] {},
                                new PropertyObjectEntity[] {form.form.addPropertyObject(addObjectAction)},
                                new OrderEntity[] {null}, ((ActionProperty)addObjectAction.property).getValueClass(), !session);
        property.setImage("add.png");
        property.setShouldBeLast(true);
        property.setEditKey(KeyStrokes.getAddActionPropertyKeyStroke());
        property.setShowEditKey(false);
        property.setPanelLocation(new ToolbarPanelLocation());
        property.setForceViewType(ClassViewType.PANEL);

        // todo : так не очень правильно делать - получается, что мы добавляем к Immutable объекту FormActionProperty ссылки на ObjectEntity
        FormActionProperty formAction = (FormActionProperty)property.property;
        formAction.seekOnOk.add(form.object);
        if (session)
            formAction.closeProperties.add(form.form.addPropertyObject(baseLM.delete, form.object));

        return property;
    }

    @IdentityLazy
    public LP getEditFormAction(CustomClass cls, boolean session) {
        ClassFormEntity form = cls.getEditForm(baseLM);
        LP property = addMFAProp(actionGroup, "edit" + (session ? "Session" : "") + "Form" + BaseUtils.capitalize(cls.getSID()), ServerResourceBundle.getString("logics.edit"), // + "(" + cls + ")",
                form.form, new ObjectEntity[]{form.object}, !session);
        property.setImage("edit.png");
        property.setShouldBeLast(true);
        property.setEditKey(KeyStrokes.getEditActionPropertyKeyStroke());
        property.setShowEditKey(false);
        property.setPanelLocation(new ToolbarPanelLocation());
        property.setForceViewType(ClassViewType.PANEL);
        return property;
    }

    @IdentityLazy
    protected LP getImportObjectAction(CustomClass cls) {
        return addAProp(new ImportFromExcelActionProperty(genSID(), cls));
    }


    protected LP addHideCaptionProp(LP original, LP hideProperty) {
        return addHideCaptionProp(original.property, hideProperty);
    }

    protected LP addHideCaptionProp(Property original, LP hideProperty) {
        return addHideCaptionProp(privateGroup, "hideCaption", original, hideProperty);
    }

    /**
     * Нужно для скрытия свойств при соблюдении какого-то критерия
     * <p/>
     * <pre>
     * Пример использования:
     *       Скроем свойство policyDescription, если у текущего user'а логин - "Admin"
     *
     *       Вводим свойство критерия:
     *
     *         LP hideUserPolicyDescription = addJProp(diff2, userLogin, 1, addCProp(StringClass.get(30), "Admin"));
     *
     *       Вводим свойство которое будет использовано в качестве propertyCaption для policyDescription:
     *
     *         policyDescriptorCaption = addHideCaptionProp(null, "Policy caption", policyDescription, hideUserPolicyDescription);
     *
     *       Далее в форме указываем соответсвующий propertyCaption:
     *
     *         PropertyDrawEntity descriptionDraw = getPropertyDraw(policyDescription, objPolicy.groupTo);
     *         PropertyDrawEntity descriptorCaptionDraw = addPropertyDraw(policyDescriptorCaption, objUser);
     *         descriptionDraw.setPropertyCaption(descriptorCaptionDraw.propertyObject);
     * </pre>
     *
     * @param group        ...
     * @param caption      ...
     * @param original     свойство, к которому будет применятся критерий сокрытия
     * @param hideProperty критерий
     * @return свойство, которое должно использоваться в качестве propertyCaption для скрываемого свойства
     */
    protected LP addHideCaptionProp(AbstractGroup group, String caption, LP original, LP hideProperty) {
        return addHideCaptionProp(group, caption, original.property, hideProperty);
    }

    protected LP addHideCaptionProp(AbstractGroup group, String caption, Property original, LP hideProperty) {
        LP originalCaption = addCProp(StringClass.get(100), original.caption);
        LP result = addJProp(group, caption, baseLM.and1, add(new Object[]{originalCaption}, directLI(hideProperty)));
        return result;
    }

    public LP addProp(Property<? extends PropertyInterface> prop) {
        return addProp(null, prop);
    }

    public LP addProp(AbstractGroup group, Property<? extends PropertyInterface> prop) {
        return addProperty(group, new LP(prop));
    }

    protected <T extends LP<?>> T addProperty(AbstractGroup group, T lp) {
        return addProperty(group, false, lp);
    }

    protected void addPropertyToGroup(Property<?> property, AbstractGroup group) {
        if (group != null) {
            group.add(property);
        } else {
            baseLM.privateGroup.add(property);
        }
    }

    private <T extends LP<?>> T addProperty(AbstractGroup group, boolean persistent, T lp) {
        setPropertySID(lp, lp.property.getSID(), true);
        if (group != null && group != baseLM.privateGroup || persistent) {
            lp.property.freezeSID();
        }
        addModuleLP(lp);
        baseLM.registerProperty(lp);
        addPropertyToGroup(lp.property, group);

        if (persistent) {
            addPersistent(lp);
        }
        return lp;
    }

    protected <T extends LP<?>> void setPropertySID(T lp, String name, boolean generated) {
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

    public void addIndex(LP<?>... lps) {
        baseLM.addIndex(lps);
    }

    protected void addPersistent(LP lp) {
        addPersistent((AggregateProperty) lp.property, null);
    }

    protected void addPersistent(LP lp, ImplementTable table) {
        addPersistent((AggregateProperty) lp.property, table);
    }

    private void addPersistent(AggregateProperty property, ImplementTable table) {
        assert !baseLM.isGeneratedSID(property.getSID());

        baseLM.logger.debug("Initializing stored property " + property + "...");
        property.markStored(baseLM.tableFactory, table);
    }

    public <T extends PropertyInterface> LP<T> addOldProp(LP<T> lp) {
        return addProperty(null, new LP<T>(lp.property.getOld(), lp.listInterfaces));
    }

    public <T extends PropertyInterface> LP<T> addCHProp(LP<T> lp, IncrementType type) {
        return addProperty(null, new LP<T>(lp.property.getChanged(type), lp.listInterfaces));
    }

    public void addConstraint(Property property, boolean checkChange) {
        addConstraint(addProp(property), checkChange);
    }

    public void addConstraint(LP<?> lp, boolean checkChange) {
        addConstraint(lp, (checkChange ? Property.CheckType.CHECK_ALL : Property.CheckType.CHECK_NO), null);
    }

    protected void addConstraint(LP<?> lp, Property.CheckType type, List<Property<?>> checkProps) {
        if(!lp.property.noDB())
            lp = addCHProp(lp, IncrementType.SET);
        // assert что lp уже в списке properties
        lp.property.setConstraint(type, checkProps);
    }

    protected <L extends PropertyInterface, T extends PropertyInterface> void follows(LP<T> first, LP<L> second, int... mapping) {
        follows(first, PropertyFollows.RESOLVE_ALL, second, mapping);
    }

    protected <L extends PropertyInterface, T extends PropertyInterface> void follows(LP<T> first, int options, LP<L> second, int... mapping) {
        Map<L, T> mapInterfaces = new HashMap<L, T>();
        for (int i = 0; i < second.listInterfaces.size(); i++) {
            mapInterfaces.put(second.listInterfaces.get(i), first.listInterfaces.get(mapping[i] - 1));
        }
        first.property.addFollows(new PropertyMapImplement<L, T>(second.property, mapInterfaces), options, this);
    }

    protected void followed(LP first, LP... lps) {
        for (LP lp : lps) {
            int[] mapping = new int[lp.listInterfaces.size()];
            for (int i = 0; i < mapping.length; i++) {
                mapping[i] = i + 1;
            }
            follows(lp, first, mapping);
        }
    }

    protected void setNotNull(LP property, ValueClass... classes) {
        setNotNull(property, PropertyFollows.RESOLVE_TRUE, classes);
    }

    protected <P extends PropertyInterface, C extends PropertyInterface> void setNotNull(LP<P> property, int resolve, ValueClass... classes) {

        ValueClass[] values = new ValueClass[property.listInterfaces.size()];
        System.arraycopy(classes, 0, values, 0, classes.length);
        ValueClass[] propertyClasses = property.getMapClasses();
        System.arraycopy(propertyClasses, classes.length, values, classes.length, propertyClasses.length - classes.length);

        LP<C> checkProp = addCProp(LogicalClass.instance, true, values);

        Map<P,C> mapInterfaces = new HashMap<P,C>();
        for (int i = 0; i < property.listInterfaces.size(); i++) {
            mapInterfaces.put(property.listInterfaces.get(i), checkProp.listInterfaces.get(i));
        }
        checkProp.property.addFollows(
                new PropertyMapImplement<P, C>(property.property, mapInterfaces),
                ServerResourceBundle.getString("logics.property") + " " + property.property.caption + " [" + property.property.getSID() + "] " + ServerResourceBundle.getString("logics.property.not.defined"),
                resolve, this);
    }

    protected void makeUserLoggable(LP... lps) {
        for (LP lp : lps)
            lp.makeUserLoggable(baseLM);
    }

    protected void makeUserLoggable(AbstractGroup group) {
        makeUserLoggable(group, false);
    }

    protected void makeUserLoggable(AbstractGroup group, boolean dataPropertiesOnly) {
        for (Property property : group.getProperties()) {
            if (!dataPropertiesOnly || property instanceof DataProperty) {
                baseLM.getLP(property.getSID()).makeUserLoggable(baseLM);
            }
        }
    }


    protected void makeLoggable(LP... lps) {
        for (LP lp : lps)
            lp.makeLoggable(baseLM);
    }

    protected void makeLoggable(AbstractGroup group) {
        makeLoggable(group, false);
    }

    protected void makeLoggable(AbstractGroup group, boolean dataPropertiesOnly) {
        for (Property property : group.getProperties()) {
            if (!dataPropertiesOnly || property instanceof DataProperty) {
                baseLM.getLP(property.getSID()).makeLoggable(baseLM);
            }
        }
    }

    // получает свойство is
    public LP is(ValueClass valueClass) {
        return baseLM.is(valueClass);
    }

    public LP object(ValueClass valueClass) {
        return baseLM.object(valueClass);
    }

    public LP vdefault(ConcreteValueClass valueClass) {
        return baseLM.vdefault(valueClass);
    }

    protected LP and(boolean... nots) {
        return addAFProp(nots);
    }

    @IdentityLazy
    protected LP dumb(int interfaces) {
        ValueClass params[] = new ValueClass[interfaces];
        for (int i = 0; i < interfaces; ++i) {
            params[i] = baseLM.baseClass;
        }
        return addCProp(privateGroup, "dumb" + interfaces, false, "dumbProperty" + interfaces, StringClass.get(0), "", params);
    }

    protected NavigatorElement addNavigatorElement(String name, String caption) {
        return addNavigatorElement(null, name, caption);
    }

    protected NavigatorElement addNavigatorElement(NavigatorElement parent, String name, String caption) {
        NavigatorElement elem = new NavigatorElement(parent, transformNameToSID(name), caption);
        addModuleNavigator(elem);
        return elem;
    }

    protected NavigatorAction addNavigatorAction(String name, String caption, LP property) {
        return addNavigatorAction(null, name, caption, property);
    }

    protected NavigatorAction addNavigatorAction(NavigatorElement parent, String name, String caption, LP property) {
        return addNavigatorAction(parent, name, caption, property.property);
    }

    protected NavigatorAction addNavigatorAction(NavigatorElement parent, String name, String caption, Property property) {
        assert property instanceof ActionProperty;

        NavigatorAction navigatorAction = new NavigatorAction(parent, transformNameToSID(name), caption);
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
        CustomClass objectClass = (CustomClass) object.baseClass;
        if (actionImport)
            form.addPropertyDraw(getImportObjectAction(objectClass)).toDraw = object.groupTo;

        PropertyDrawEntity actionAddPropertyDraw;
        if (checkObject == null) {
            actionAddPropertyDraw = form.addPropertyDraw(getAddObjectAction(objectClass));
        } else {
            actionAddPropertyDraw = form.addPropertyDraw(
                    getAddObjectActionWithClassCheck(objectClass, checkObjectClass != null ? checkObjectClass : checkObject.baseClass),
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
        LP addForm = getAddFormAction((CustomClass)object.baseClass, session);
        PropertyDrawEntity actionAddPropertyDraw = form.addPropertyDraw(addForm);
        actionAddPropertyDraw.toDraw = object.groupTo;

        return actionAddPropertyDraw;
    }
    
    public PropertyDrawEntity addEditFormAction(FormEntity form, ObjectEntity object, boolean session) {
        return form.addPropertyDraw(getEditFormAction((CustomClass)object.baseClass, session), object);
    }

    protected class MetaCodeFragment {
        public List<String> parameters;
        public List<String> tokens;

        private char QUOTE = '\'';

        public MetaCodeFragment(List<String> params, List<String> code) {
            this.parameters = params;
            this.tokens = code;
        }

        public String getCode(List<String> params) {
            assert params.size() == parameters.size();
            ArrayList<String> newTokens = new ArrayList<String>();
            for (int i = 0; i < tokens.size(); i++) {
                String tokenStr = transformedToken(params, tokens.get(i));
                if (tokenStr.equals("##") || tokenStr.equals("###")) {
                    if (!newTokens.isEmpty() && i+1 < tokens.size()) {
                        String lastToken = newTokens.get(newTokens.size()-1);
                        String nextToken = transformedToken(params, tokens.get(i+1));
                        newTokens.set(newTokens.size()-1, concatTokens(lastToken, nextToken, tokenStr.equals("###")));
                        ++i;
                    }
                } else {
                    newTokens.add(tokenStr);
                }
            }

            StringBuilder resultCode = new StringBuilder();
            for (String token : newTokens) {
                resultCode.append(token);
                resultCode.append(" ");
            }
            return resultCode.toString();
        }

        private String transformedToken(List<String> actualParams, String token) {
            int index = parameters.indexOf(token);
            return index >= 0 ? actualParams.get(index) : token;
        }

        private String concatTokens(String t1, String t2, boolean toCapitalize) {
            if (t1.isEmpty() || t2.isEmpty()) {
                return t1 + capitalize(t2, toCapitalize && !t1.isEmpty());
            } else if (t1.charAt(0) == QUOTE || t2.charAt(0) == QUOTE) {
                return QUOTE + unquote(t1) + capitalize(unquote(t2), toCapitalize) + QUOTE;
            } else {
                return t1 + capitalize(t2, toCapitalize);
            }
        }

        private String unquote(String s) {
            if (s.length() >= 2 && s.charAt(0) == QUOTE && s.charAt(s.length()-1) == QUOTE) {
                s = s.substring(1, s.length()-1);
            }
            return s;
        }

        private String capitalize(String s, boolean toCapitalize) {
            if (toCapitalize && s.length() > 0) {
                s = StringUtils.capitalize(s);
            }
            return s;
        }
    }
}
