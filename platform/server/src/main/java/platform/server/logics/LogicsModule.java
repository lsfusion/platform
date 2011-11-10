package platform.server.logics;

import net.sf.jasperreports.engine.JRException;
import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.Result;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.server.Settings;
import platform.server.caches.IdentityLazy;
import platform.server.classes.*;
import platform.server.data.Time;
import platform.server.data.Union;
import platform.server.data.expr.StringAggUnionProperty;
import platform.server.data.expr.query.GroupType;
import platform.server.data.expr.query.OrderType;
import platform.server.data.where.classes.ClassWhere;
import platform.server.form.entity.*;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.entity.filter.FilterEntity;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.*;
import platform.server.logics.property.derived.AggregateGroupProperty;
import platform.server.logics.property.derived.ConcatenateProperty;
import platform.server.logics.property.derived.CycleGroupProperty;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.mail.EmailActionProperty;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import java.io.FileNotFoundException;
import java.util.*;

import static platform.base.BaseUtils.consecutiveInts;
import static platform.server.logics.PropertyUtils.*;

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

    public abstract void initNavigators() throws JRException, FileNotFoundException;

    public String getErrorsDescription() { return "";}

    /// Добавляется к SID объектов модуля: классам, группам, свойствам...
    public abstract String getNamePrefix();

    public String transformNameToSID(String name) {
        return transformNameToSID(getNamePrefix(), name);
    }

    public static String transformNameToSID(String moduleSID, String name) {
        if (moduleSID == null) {
            return name;
        } else {
            return moduleSID + "_" + name;
        }
    }

    public BaseLogicsModule<?> baseLM;

    private final Map<String, LP<?>> moduleProperties = new HashMap<String, LP<?>>();
    private final Map<String, AbstractGroup> moduleGroups = new HashMap<String, AbstractGroup>();
    private final Map<String, ValueClass> moduleClasses = new HashMap<String, ValueClass>();

    private final Map<String, List<String>> propNamedParams = new HashMap<String, List<String>>();

    public LogicsModule(String sID) {
        this.sID = sID;
    }

    private String sID;

    protected String getSID() {
        return sID;
    }

    public LP<?> getLPBySID(String sID) {
        return moduleProperties.get(sID);
    }

    public LP<?> getLPByName(String name) {
        return getLPBySID(transformNameToSID(name));
    }

    protected void addModuleLP(LP<?> lp) {
//        assert !moduleProperties.containsKey(lp.property.getSID());
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

    public List<String> getNamedParams(String sID) {
        return propNamedParams.get(sID);
    }

    protected void addNamedParams(String sID, List<String> namedParams) {
        assert !propNamedParams.containsKey(sID);
        propNamedParams.put(sID, namedParams);
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

    protected StaticCustomClass addStaticClass(String name, String caption, String[] sids, String[] names) {
        StaticCustomClass customClass = new StaticCustomClass(transformNameToSID(name), caption, baseLM.baseClass.sidClass, sids, names);
        storeCustomClass(customClass);
        customClass.dialogReadOnly = true;
        return customClass;
    }

    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////
    /// Properties
    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////

    public String genSID() {
        String id = "property" + baseLM.idSet.size();
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

    protected <D extends PropertyInterface> LP addDCProp(String name, boolean persistent, String caption, LP<D> derivedProp, Object... params) {
        return addDCProp(null, name, persistent, caption, false, derivedProp, params);
    }

    protected <D extends PropertyInterface> LP addDCProp(AbstractGroup group, String name, String caption, LP<D> derivedProp, Object... params) {
        return addDCProp(group, name, false, caption, false, derivedProp, params);
    }

    protected <D extends PropertyInterface> LP addDCProp(String name, String caption, boolean forced, LP<D> derivedProp, Object... params) {
        return addDCProp(null, name, caption, forced, derivedProp, params);
    }

    protected <D extends PropertyInterface> LP addDCProp(AbstractGroup group, String name, String caption, boolean forced, LP<D> derivedProp, Object... params) {
        return addDCProp(group, name, false, caption, forced, derivedProp, params);
    }

    protected <D extends PropertyInterface> LP addDCProp(AbstractGroup group, String name, boolean persistent, String caption, boolean forced, LP<D> derivedProp, Object... params) {

        // считываем override'ы с конца
        List<ValueClass> backClasses = new ArrayList<ValueClass>();
        int i = params.length - 1;
        while (i > 0 && (params[i] == null || params[i] instanceof ValueClass))
            backClasses.add((ValueClass) params[i--]);
        params = Arrays.copyOfRange(params, 0, i + 1);
        ValueClass[] overrideClasses = BaseUtils.reverse(backClasses).toArray(new ValueClass[1]);

        boolean defaultChanged = false;
        if (params[0] instanceof Boolean) {
            defaultChanged = (Boolean) params[0];
            params = Arrays.copyOfRange(params, 1, params.length);
        }

        // придется создавать Join свойство чтобы считать его класс
        List<PropertyUtils.LI> list = readLI(params);

        int propsize = derivedProp.listInterfaces.size();
        int dersize = getIntNum(params);
        JoinProperty<AndFormulaProperty.Interface> joinProperty = new JoinProperty<AndFormulaProperty.Interface>(name, caption, dersize, false);
        LP<JoinProperty.Interface> listProperty = new LP<JoinProperty.Interface>(joinProperty);

        AndFormulaProperty andProperty = new AndFormulaProperty(genSID(), new boolean[list.size() - propsize]);
        Map<AndFormulaProperty.Interface, PropertyInterfaceImplement<JoinProperty.Interface>> mapImplement = new HashMap<AndFormulaProperty.Interface, PropertyInterfaceImplement<JoinProperty.Interface>>();
        mapImplement.put(andProperty.objectInterface, DerivedProperty.createJoin(mapImplement(derivedProp, mapLI(list.subList(0, propsize), listProperty.listInterfaces))));
        Iterator<AndFormulaProperty.AndInterface> itAnd = andProperty.andInterfaces.iterator();
        for (PropertyInterfaceImplement<JoinProperty.Interface> partProperty : mapLI(list.subList(propsize, list.size()), listProperty.listInterfaces))
            mapImplement.put(itAnd.next(), partProperty);

        joinProperty.implement = new PropertyImplement<AndFormulaProperty.Interface, PropertyInterfaceImplement<JoinProperty.Interface>>(andProperty, mapImplement);

        // получаем классы
        Result<ValueClass> value = new Result<ValueClass>();
        ValueClass[] commonClasses = listProperty.getCommonClasses(value);

        // override'им классы
        ValueClass valueClass;
        if (overrideClasses.length > dersize) {
            valueClass = overrideClasses[dersize];
            assert !overrideClasses[dersize].isCompatibleParent(value.result);
            overrideClasses = Arrays.copyOfRange(params, 0, dersize, ValueClass[].class);
        } else
            valueClass = value.result;

        // выполняем само создание свойства
        LP derDataProp = addDProp(group, name, persistent, caption, valueClass, overrideClasses(commonClasses, overrideClasses));
        if (forced)
            derDataProp.setDerivedForcedChange(defaultChanged, derivedProp, params);
        else
            derDataProp.setDerivedChange(defaultChanged, derivedProp, params);
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

    protected LP addMFAProp(String caption, FormEntity form, ObjectEntity... params) {
        return addMFAProp(null, caption, form, params, new PropertyObjectEntity[0], new PropertyObjectEntity[0]);
    }

    protected LP addMFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, PropertyObjectEntity... setProperties) {
        return addMFAProp(group, caption, form, objectsToSet, false, setProperties);
    }

    protected LP addMFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, boolean newSession, PropertyObjectEntity... setProperties) {
        // во все setProperties просто будут записаны null'ы
        return addMFAProp(group, caption, form, objectsToSet, setProperties, new PropertyObjectEntity[setProperties.length], newSession);
    }

    protected LP addMFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, PropertyObjectEntity[] setProperties, OrderEntity[] getProperties) {
        return addMFAProp(group, caption, form, objectsToSet, setProperties, getProperties, true);
    }

    protected LP addMFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, PropertyObjectEntity[] setProperties, OrderEntity[] getProperties, boolean newSession) {
        return addFAProp(group, caption, form, objectsToSet, setProperties, getProperties, newSession, true);
    }

    protected LP addFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, PropertyObjectEntity[] setProperties, OrderEntity[] getProperties, boolean newSession, boolean isModal) {
        return addProperty(group, new LP<ClassPropertyInterface>(new FormActionProperty(genSID(), caption, form, objectsToSet, setProperties, getProperties, newSession, isModal)));
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

    protected LP addStopActionProp(String caption, String header) {
        return addAProp(new StopActionProperty(genSID(), caption, header));
    }

    protected LP addEAProp(ValueClass... params) {
        return addEAProp((String)null, params);
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
        return addProperty(group, new LP<ClassPropertyInterface>(new EmailActionProperty(name, caption, subject, fromAddress, emailBlindCarbonCopy, baseLM.BL, params)));
    }

    protected <X extends PropertyInterface> void addEARecepient(LP<ClassPropertyInterface> eaProp, LP<X> emailProp, Integer... params) {
        addEARecepient(eaProp, MimeMessage.RecipientType.TO, emailProp, params);
    }

    protected <X extends PropertyInterface> void addEARecepient(LP<ClassPropertyInterface> eaProp, Message.RecipientType type, LP<X> emailProp, Integer... params) {
        Map<X, ClassPropertyInterface> mapInterfaces = new HashMap<X, ClassPropertyInterface>();
        for (int i = 0; i < emailProp.listInterfaces.size(); i++)
            mapInterfaces.put(emailProp.listInterfaces.get(i), eaProp.listInterfaces.get(params[i] - 1));
        ((EmailActionProperty) eaProp.property).addRecipient(new PropertyMapImplement<X, ClassPropertyInterface>(emailProp.property, mapInterfaces), type);
    }

    protected void addInlineEAForm(LP<ClassPropertyInterface> eaProp, FormEntity form, Object... params) {
        Map<ObjectEntity, ClassPropertyInterface> mapObjects = new HashMap<ObjectEntity, ClassPropertyInterface>();
        for (int i = 0; i < params.length / 2; i++)
            mapObjects.put((ObjectEntity) params[2 * i], eaProp.listInterfaces.get((Integer) params[2 * i + 1] - 1));
        ((EmailActionProperty) eaProp.property).addInlineForm(form, mapObjects);
    }

    protected void addAttachEAForm(LP<ClassPropertyInterface> eaProp, FormEntity form, EmailActionProperty.Format format, Object... params) {
        LP attachmentName = null;
        if (params.length > 0 && params[0] instanceof LP) {
            attachmentName = (LP) params[0];
            params = Arrays.copyOfRange(params, 1, params.length);
        }
        Map<ObjectEntity, ClassPropertyInterface> mapObjects = new HashMap<ObjectEntity, ClassPropertyInterface>();
        for (int i = 0; i < params.length / 2; i++)
            mapObjects.put((ObjectEntity) params[2 * i], eaProp.listInterfaces.get((Integer) params[2 * i + 1] - 1));
        ((EmailActionProperty) eaProp.property).addAttachmentForm(form, format, mapObjects, attachmentName);
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

    // добавляет свойство с бесконечным значением
    protected LP addICProp(DataClass valueClass, ValueClass... params) {
        return addProperty(baseLM.privateGroup, false, new LP<ClassPropertyInterface>(new InfiniteClassProperty(genSID(), ServerResourceBundle.getString("logics.infinity"), params, valueClass)));
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

    protected LP addCProp(AbstractGroup group, String name, boolean persistent, String caption, StaticClass valueClass, Object value, ValueClass... params) {
        return addProperty(group, persistent, new LP<ClassPropertyInterface>(new ValueClassProperty(name, caption, params, valueClass, value)));
    }

    protected LP addTProp(Time time) {
        return addProperty(null, new LP<PropertyInterface>(new TimeFormulaProperty(genSID(), time)));
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

    protected LP addMFProp(String name, ConcreteValueClass value, int paramCount) {
        return addProperty(null, new LP<StringFormulaProperty.Interface>(new MultiplyFormulaProperty(name, value, paramCount)));
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
        return addProperty(null, new LP<ConcatenateProperty.Interface>(new ConcatenateProperty(paramCount)));
    }

    protected LP addJProp(LP mainProp, Object... params) {
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
        return addJProp(group, mainProp.property instanceof ActionProperty, name, caption, mainProp, params);
    }

    protected LP addJProp(AbstractGroup group, String name, boolean persistent, String caption, LP mainProp, Object... params) {
        return addJProp(group, mainProp.property instanceof ActionProperty, name, persistent, caption, mainProp, params);
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

        JoinProperty<?> property = new JoinProperty(name, caption, getIntNum(params), implementChange);
        property.inheritFixedCharWidth(mainProp.property);
        property.inheritImage(mainProp.property);

        LP listProperty = new LP<JoinProperty.Interface>(property);
        property.implement = mapImplement(mainProp, readImplements(listProperty.listInterfaces, params));

        return addProperty(group, persistent, listProperty);
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

    protected LP addGCAProp(AbstractGroup group, String name, String caption, GroupObjectEntity groupObject, LP mainProperty, int[] mainInts, LP getterProperty, int[] getterInts, int[] groupInts) {
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

    private <P extends PropertyInterface, L extends PropertyInterface> LP mapLProp(AbstractGroup group, boolean persistent, PropertyMapImplement<L, P> implement, LP<P> property) {
        return addProperty(group, persistent, new LP<L>(implement.property, BaseUtils.mapList(property.listInterfaces, BaseUtils.reverse(implement.mapping))));
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

    protected <P extends PropertyInterface> LP addOProp(String caption, OrderType orderType, LP<P> sum, boolean ascending, boolean includeLast, int partNum, Object... params) {
        return addOProp(genSID(), caption, orderType, sum, ascending, includeLast, partNum, params);
    }

    protected <P extends PropertyInterface> LP addOProp(String name, String caption, OrderType orderType, LP<P> sum, boolean ascending, boolean includeLast, int partNum, Object... params) {
        return addOProp((AbstractGroup) null, name, caption, orderType, sum, ascending, includeLast, partNum, params);
    }

    protected <P extends PropertyInterface> LP addOProp(AbstractGroup group, String caption, OrderType orderType, LP<P> sum, boolean ascending, boolean includeLast, int partNum, Object... params) {
        return addOProp(group, genSID(), caption, orderType, sum, ascending, includeLast, partNum, params);
    }

    protected <P extends PropertyInterface> LP addOProp(AbstractGroup group, String name, String caption, OrderType orderType, LP<P> sum, boolean ascending, boolean includeLast, int partNum, Object... params) {
        return addOProp(group, name, false, caption, sum, false, orderType, ascending, includeLast, partNum, params);
    }

    // проценты
    protected <P extends PropertyInterface> LP addPOProp(AbstractGroup group, String caption, LP<P> sum, boolean ascending, boolean includeLast, int partNum, Object... params) {
        return addPOProp(group, genSID(), false, caption, sum, ascending, includeLast, partNum, params);
    }

    protected <P extends PropertyInterface> LP addPOProp(AbstractGroup group, String name, boolean persistent, String caption, LP<P> sum, boolean ascending, boolean includeLast, int partNum, Object... params) {
        return addOProp(group, name, persistent, caption, sum, true, null, ascending, includeLast, partNum, params);
    }

    private <P extends PropertyInterface> LP addOProp(AbstractGroup group, String name, boolean persistent, String caption, LP<P> sum, boolean percent, OrderType orderType, boolean ascending, boolean includeLast, int partNum, Object... params) {
        List<LI> li = readLI(params);

        Collection<PropertyInterfaceImplement<P>> partitions = mapLI(li.subList(0, partNum), sum.listInterfaces);
        OrderedMap<PropertyInterfaceImplement<P>, Boolean> orders = new OrderedMap<PropertyInterfaceImplement<P>, Boolean>(mapLI(li.subList(partNum, li.size()), sum.listInterfaces), !ascending);

        PropertyMapImplement<?, P> orderProperty;
        if (percent)
            orderProperty = DerivedProperty.createPOProp(name, caption, sum.property, partitions, orders, includeLast);
        else
            orderProperty = DerivedProperty.createOProp(name, caption, orderType, sum.property, partitions, orders, includeLast);

        return mapLProp(group, persistent, orderProperty, sum);
    }

    protected <R extends PropertyInterface, L extends PropertyInterface> LP addUGProp(AbstractGroup group, String caption, boolean ascending, LP<R> restriction, LP<L> ungroup, Object... params) {
        return addUGProp(group, genSID(), caption, ascending, restriction, ungroup, params);
    }

    protected <R extends PropertyInterface, L extends PropertyInterface> LP addUGProp(AbstractGroup group, String name, String caption, boolean ascending, LP<R> restriction, LP<L> ungroup, Object... params) {
        return addUGProp(group, name, false, caption, ascending, restriction, ungroup, params);
    }

    protected <R extends PropertyInterface, L extends PropertyInterface> LP addUGProp(AbstractGroup group, String name, boolean persistent, String caption, boolean ascending, LP<R> restriction, LP<L> ungroup, Object... params) {
        List<LI> li = readLI(params);

        Map<L, PropertyInterfaceImplement<R>> groupImplement = new HashMap<L, PropertyInterfaceImplement<R>>();
        for (int i = 0; i < ungroup.listInterfaces.size(); i++)
            groupImplement.put(ungroup.listInterfaces.get(i), li.get(i).map(restriction.listInterfaces));
        OrderedMap<PropertyInterfaceImplement<R>, Boolean> orders = new OrderedMap<PropertyInterfaceImplement<R>, Boolean>(mapLI(li.subList(ungroup.listInterfaces.size(), li.size()), restriction.listInterfaces), ascending);
        return mapLProp(group, persistent, DerivedProperty.createUGProp(name, caption, new PropertyImplement<L, PropertyInterfaceImplement<R>>(ungroup.property, groupImplement), orders, restriction.property), restriction);
    }

    protected <R extends PropertyInterface, L extends PropertyInterface> LP addPGProp(AbstractGroup group, String name, boolean persistent, int roundlen, boolean roundfirst, String caption, LP<R> proportion, LP<L> ungroup, Object... params) {
        List<LI> li = readLI(params);

        Map<L, PropertyInterfaceImplement<R>> groupImplement = new HashMap<L, PropertyInterfaceImplement<R>>();
        for (int i = 0; i < ungroup.listInterfaces.size(); i++)
            groupImplement.put(ungroup.listInterfaces.get(i), li.get(i).map(proportion.listInterfaces));
        return mapLProp(group, persistent, DerivedProperty.createPGProp(name, caption, roundlen, roundfirst, baseLM.baseClass, new PropertyImplement<L, PropertyInterfaceImplement<R>>(ungroup.property, groupImplement), proportion.property), proportion);
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
        return addSGProp(group, name, persistent, notZero, caption, groupProp.listInterfaces, BaseUtils.add(groupProp.property.getImplement(), listImplements));
    }

    private List<PropertyInterface> genInterfaces(int interfaces) {
        List<PropertyInterface> innerInterfaces = new ArrayList<PropertyInterface>();
        for(int i=0;i<interfaces;i++)
            innerInterfaces.add(new PropertyInterface());
        return innerInterfaces;
    }

    private <T extends PropertyInterface> LP addSGProp(AbstractGroup group, String name, boolean persistent, boolean notZero, String caption, int interfaces, Object... params) {
        List<PropertyInterface> innerInterfaces = genInterfaces(interfaces);
        List<PropertyInterfaceImplement<PropertyInterface>> implement = readImplements(innerInterfaces, params);
        return addSGProp(group, name, persistent, notZero, caption, innerInterfaces, implement);
    }

    private <T extends PropertyInterface> LP addSGProp(AbstractGroup group, String name, boolean persistent, boolean notZero, String caption, List<T> innerInterfaces, List<PropertyInterfaceImplement<T>> implement) {
        boolean wrapNotZero = persistent && (notZero || !Settings.instance.isDisableSumGroupNotZero());
        List<PropertyInterfaceImplement<T>> listImplements = implement.subList(1, implement.size());
        SumGroupProperty<T> property = new SumGroupProperty<T>(name, caption, innerInterfaces, listImplements, implement.get(0));

        LP result;
        if (wrapNotZero)
            result = addJProp(group, name, persistent, caption, baseLM.onlyNotZero, directLI(mapLGProp(null, false, property, listImplements)));
        else
            result = mapLGProp(group, persistent, property, listImplements);

        result.sumGroup = property; // так как может wrap'ся, использование - setDG
        result.listGroupInterfaces = innerInterfaces; // для порядка параметров, использование - setDG

        return result;
    }

    public <T extends PropertyInterface> LP addOGProp(String caption, GroupType type, int numOrders, boolean ascending, LP<T> groupProp, Object... params) {
        return addOGProp(genSID(), false, caption, type, numOrders, ascending, groupProp, params);
    }
    public <T extends PropertyInterface> LP addOGProp(String name, boolean persist, String caption, GroupType type, int numOrders, boolean ascending, LP<T> groupProp, Object... params) {
        return addOGProp(null, name, persist, caption, type, numOrders, ascending, groupProp, params);
    }
    public <T extends PropertyInterface> LP addOGProp(AbstractGroup group, String name, boolean persist, String caption, GroupType type, int numOrders, boolean ascending, LP<T> groupProp, Object... params) {
        List<PropertyInterfaceImplement<T>> listImplements = readImplements(groupProp.listInterfaces, params);
        int numExtras = type.numExprs() - 1;

        List<PropertyInterfaceImplement<T>> extras = listImplements.subList(0, numExtras);
        OrderedMap<PropertyInterfaceImplement<T>, Boolean> orders = new OrderedMap<PropertyInterfaceImplement<T>, Boolean>(listImplements.subList(numExtras, numExtras + numOrders), ascending);
        List<PropertyInterfaceImplement<T>> groups = listImplements.subList(numExtras + numOrders, listImplements.size());
        OrderGroupProperty<T> property = new OrderGroupProperty<T>(name, caption, groups, groupProp.property, extras, type, orders);

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
        return addMGProp(group, persist, new String[]{name}, new String[]{caption}, 0, min, groupProp, params)[0];
    }

    protected <T extends PropertyInterface> LP[] addMGProp(AbstractGroup group, String[] names, String[] captions, int extra, LP<T> groupProp, Object... params) {
        return addMGProp(group, false, names, captions, extra, groupProp, params);
    }

    protected <T extends PropertyInterface> LP[] addMGProp(AbstractGroup group, boolean persist, String[] names, String[] captions, int extra, LP<T> groupProp, Object... params) {
        return addMGProp(group, persist, names, captions, extra, false, groupProp, params);
    }

    protected <T extends PropertyInterface> LP[] addMGProp(AbstractGroup group, boolean persist, String[] names, String[] captions, int extra, boolean min, LP<T> groupProp, Object... params) {
        LP[] result = new LP[extra + 1];

        Collection<Property> overridePersist = new ArrayList<Property>();

        List<PropertyInterfaceImplement<T>> listImplements = readImplements(groupProp.listInterfaces, params);
        List<PropertyInterfaceImplement<T>> groupImplements = listImplements.subList(extra, listImplements.size());
        List<PropertyImplement<?, PropertyInterfaceImplement<T>>> mgProps = DerivedProperty.createMGProp(names, captions, groupProp.property, baseLM.baseClass,
                listImplements.subList(0, extra), new HashSet<PropertyInterfaceImplement<T>>(groupImplements), overridePersist, min);

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
        List<PropertyInterfaceImplement<T>> listImplements = readImplements(groupProp.listInterfaces, params);
        CycleGroupProperty<T, P> property = new CycleGroupProperty<T, P>(name, caption, listImplements, groupProp.property, dataProp.property);

        // нужно добавить ограничение на уникальность
        addProperty(null, new LP(property.getConstrainedProperty(checkChange)));

        return mapLGProp(group, persistent, property, listImplements);
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
        return addAGProp(group, checkChange, name, persistent, caption, noConstraint, is(customClass), 1, getUParams(props, 0));
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
        return addAGProp(group, checkChange, name, persistent, caption, false, lp, aggrInterface, props);
    }

    protected <T extends PropertyInterface<T>> LP addAGProp(AbstractGroup group, boolean checkChange, String name, boolean persistent, String caption, boolean noConstraint, LP<T> lp, int aggrInterface, Object... props) {
        List<PropertyInterfaceImplement<T>> listImplements = readImplements(lp.listInterfaces, props);

        return addAGProp(group, checkChange, persistent, noConstraint, AggregateGroupProperty.create(name, caption, lp.property, lp.listInterfaces.get(aggrInterface - 1), (List<PropertyMapImplement<?, T>>) (List<?>) listImplements), BaseUtils.mergeList(listImplements, BaseUtils.removeList(lp.listInterfaces, aggrInterface - 1)));
    }

    // чисто для generics
    private <T extends PropertyInterface<T>, J extends PropertyInterface> LP addAGProp(AbstractGroup group, boolean checkChange, boolean persistent, boolean noConstraint, AggregateGroupProperty<T, J> property, List<PropertyInterfaceImplement<T>> listImplements) {
        // нужно добавить ограничение на уникальность
        if(!noConstraint)
            addProperty(null, new LP(property.getConstrainedProperty(checkChange)));

        return mapLGProp(group, persistent, property, DerivedProperty.mapImplements(listImplements, property.getMapping()));
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
        List<PropertyInterfaceImplement<T>> listImplements = readImplements(groupProp.listInterfaces, params);
        int intNum = listImplements.size();
        LP result = addSGProp(group, name, persistent, false, caption, groupProp, listImplements.subList(0, intNum - orders - 1));
        result.setDG(ascending, listImplements.subList(intNum - orders - 1, intNum));
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

        UnionProperty property = null;
        int extra = 0;
        switch (unionType) {
            case MAX:
                property = new MaxUnionProperty(name, caption, intNum);
                break;
            case SUM:
                property = new SumUnionProperty(name, caption, intNum);
                extra = 1;
                break;
            case OVERRIDE:
                property = new OverrideUnionProperty(name, caption, intNum);
                break;
            case XOR:
                property = new XorUnionProperty(name, caption, intNum);
                break;
            case EXCLUSIVE:
                property = new ExclusiveUnionProperty(name, caption, intNum);
                break;
            case STRING_AGG:
                property = new StringAggUnionProperty(name, caption, intNum, (String) params[params.length-1]);
                params = Arrays.copyOfRange(params, 0, params.length-1);
                break;
        }

        LP listProperty = new LP<UnionProperty.Interface>(property);

        for (int i = 0; i < params.length / (intNum + 1 + extra); i++) {
            Integer offs = i * (intNum + 1 + extra);
            LP<?> opImplement = (LP) params[offs + extra];
            PropertyMapImplement operand = new PropertyMapImplement(opImplement.property);
            for (int j = 0; j < intNum; j++)
                operand.mapping.put(opImplement.listInterfaces.get(((Integer) params[offs + 1 + extra + j]) - 1), listProperty.listInterfaces.get(j));

            switch (unionType) {
                case MAX:
                    ((MaxUnionProperty) property).operands.add(operand);
                    break;
                case SUM:
                    ((SumUnionProperty) property).operands.put(operand, (Integer) params[offs]);
                    break;
                case OVERRIDE:
                    ((OverrideUnionProperty) property).addOperand(operand);
                    break;
                case XOR:
                    ((XorUnionProperty) property).operands.add(operand);
                    break;
                case EXCLUSIVE:
                    ((ExclusiveUnionProperty) property).addOperand(operand);
                    break;
                case STRING_AGG:
                    ((StringAggUnionProperty) property).operands.add(operand);
                    break;
                default:
                    throw new RuntimeException("could not be");
            }
        }

        return addProperty(group, persistent, listProperty);
    }

    protected LP addCaseUProp(AbstractGroup group, String name, boolean persistent, String caption, Object... params) {
        List<LI> list = readLI(params);
        int intNum = ((LMI) list.get(1)).lp.listInterfaces.size(); // берем количество интерфейсов у первого case'а

        CaseUnionProperty caseProp = new CaseUnionProperty(name, caption, intNum);
        LP<UnionProperty.Interface> listProperty = new LP<UnionProperty.Interface>(caseProp);
        List<PropertyMapImplement<?, UnionProperty.Interface>> mapImplements = (List<PropertyMapImplement<?, UnionProperty.Interface>>) (List<?>) mapLI(list, listProperty.listInterfaces);
        for (int i = 0; i < mapImplements.size() / 2; i++)
            caseProp.addCase(mapImplements.get(2 * i), mapImplements.get(2 * i + 1));
        if (mapImplements.size() % 2 != 0)
            caseProp.addCase(new PropertyMapImplement<PropertyInterface, UnionProperty.Interface>(baseLM.vtrue.property), mapImplements.get(mapImplements.size() - 1));
        return addProperty(group, persistent, listProperty);
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

    protected LP addLProp(LP lp, ValueClass... classes) {
        return addDCProp("LG_" + lp.property.getSID(), ServerResourceBundle.getString("logics.log")+" " + lp.property, baseLM.object1, BaseUtils.add(BaseUtils.add(directLI(lp), new Object[]{addJProp(baseLM.equals2, 1, baseLM.currentSession), lp.listInterfaces.size() + 1}), classes));
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
        return addJProp(group, name, persistent, caption, and(not), BaseUtils.add(getUParams(new LP[]{prop}, 0), BaseUtils.add(new LP[]{ifProp}, params)));
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
        return addSUProp(group, name, persistent, false, caption, unionType, props);
    }

    protected LP addSUProp(AbstractGroup group, String name, boolean persistent, boolean notZero, String caption, Union unionType, LP... props) {
        assert baseLM.checkSUProps.add(props);
        if (notZero) {
            LP uProp = addUProp(null, genSID(), false, caption, unionType, getUParams(props, (unionType == Union.SUM ? 1 : 0)));
            return addJProp(group, name, persistent, caption, baseLM.onlyNotZero, directLI(uProp));
        } else {
            return addUProp(group, name, persistent, caption, unionType, getUParams(props, (unionType == Union.SUM ? 1 : 0)));
        }
    }

    protected LP addSFUProp(AbstractGroup group, String name, String caption, String delimiter, LP... props) {
        return addSFUProp(group, name, false, caption, delimiter, props);
    }
    protected LP addSFUProp(AbstractGroup group, String name, boolean persistent, String caption, String delimiter, LP... props) {
        return addUProp(group, name, persistent, caption, Union.STRING_AGG, BaseUtils.add(getUParams(props, 0), delimiter));
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
        LP[] maxProps = Arrays.copyOfRange(props, 0, propNum);

        LP[] result = new LP[extra + 1];
        int i = 0;
        do {
            result[i] = addUProp(group, names[i], captions[i], Union.MAX, getUParams(maxProps, 0));
            if (i < extra) { // если не последняя
                for (int j = 0; j < propNum; j++)
                    maxProps[j] = addJProp(baseLM.and1, BaseUtils.add(directLI(props[(i + 1) * propNum + j]), directLI( // само свойство
                            addJProp(baseLM.equals2, BaseUtils.add(directLI(maxProps[j]), directLI(result[i])))))); // только те кто дает предыдущий максимум
            }
        } while (i++ < extra);
        return result;
    }

    protected LP addAProp(ActionProperty property) {
        return addAProp(baseLM.actionGroup, property);
    }

    protected LP addAProp(AbstractGroup group, ActionProperty property) {
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
        return addMAProp(message, null, caption);
    }

    protected LP addMAProp(String message, AbstractGroup group, String caption) {
        return addProperty(group, new LP<ClassPropertyInterface>(new BaseLogicsModule.MessageActionProperty(message, genSID(), caption)));
    }

    /**
     * Добавляет action для запуска свойств с мэппингом по порядку, т.е. на входы и выход каждого свойства мэппятся интерфейсы результирующего по порядку
     */
    protected LP addEPAProp(LP... lps) {
        return addEPAProp(EPA_INTERFACE, lps);
    }

    public final static int EPA_INTERFACE = 0; // значение идет доп. интерфейсом
    public final static int EPA_DEFAULT = 1; // писать из getDefaultValue
    public final static int EPA_NULL = 2; // писать null

    /**
     * Добавляет action для запуска свойств с мэппингом по порядку, т.е. на входы и выход каждого свойства мэппятся интерфейсы результирующего по порядку
     *
     * @param writeType Если != INTERFACE, то мэппятся только входы, без выхода
     */

    protected LP addEPAProp(int writeType, LP... lps) {
        return addEPAProp(genSID(), "sysEPA", writeType, lps);
    }

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
     * Мэппиг задаётся перечислением свойств с указанием после каждого номеров интерфейсов результирующего свойства,
     * которые пойдут на входы и выход данных свойств
     * Пример 1: addEPAProp(true, userLogin, 1, inUserRole, 1, 2)
     * Пример 2: addEPAProp(false, userLogin, 1, 3, inUserRole, 1, 2, 4)
     *
     * @param writeType использовать ли значения по умолчанию для записи в свойства.
     *                           Если значение этого параметра false, то мэпиться должны не только выходы, но и вход, номер интерфейса, который пойдёт на вход, должен быть указан последним
     */

    protected LP addEPAProp(int writeType, Object... params) {
        return addEPAProp(genSID(), "sysEPA", writeType, params);
    }

    protected LP addEPAProp(String sID, String caption, int writeType, Object... params) {
        List<LP> lps = new ArrayList<LP>();
        List<int[]> mapInterfaces = new ArrayList<int[]>();

        int pi = 0;
        while (pi < params.length) {
            assert params[pi] instanceof LP;

            LP lp = (LP) params[pi++];

            int[] propMapInterfaces = new int[lp.listInterfaces.size() + (writeType == LogicsModule.EPA_INTERFACE ? 1 : 0)];
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
        LP<ClassPropertyInterface> openAction = new LP<ClassPropertyInterface>(new BaseLogicsModule.OpenActionProperty(genSID(), caption, lp));
        return addJProp(group, caption, baseLM.and1, getUParams(new LP[]{openAction, lp}, 0));
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
    protected LP getAddObjectAction(ValueClass cls) {
        return addAProp(new AddObjectActionProperty(genSID(), (CustomClass) cls));
    }

    @IdentityLazy
    protected LP getAddObjectActionWithClassCheck(ValueClass baseClass, ValueClass checkClass) {
        LP addObjectAction = getAddObjectAction(baseClass);
        return addJProp(addObjectAction.property.caption, baseLM.and1, addObjectAction, is(checkClass), 1);
    }

    @IdentityLazy
    protected LP getAddFormAction(ConcreteCustomClass cls) {
        ClassFormEntity form = cls.getEditForm(baseLM);
        LP property = addMFAProp(actionGroup, ServerResourceBundle.getString("logics.add") + "(" + cls + ")",
                                form, new ObjectEntity[] {},
                                new PropertyObjectEntity[] {form.addPropertyObject(getAddObjectAction(cls))},
                                new OrderEntity[] {(DataObject)cls.getClassObject()}, true);
        property.setImage("add.png");
        return property;
    }

    @IdentityLazy
    protected LP getEditFormAction(ConcreteCustomClass cls) {
        ClassFormEntity form = cls.getEditForm(baseLM);
        LP property = addMFAProp(actionGroup, ServerResourceBundle.getString("logics.edit") + "(" + cls + ")",
                                form, new ObjectEntity[] {form.getObject()}, true);
        property.setImage("edit.png");
        return property;
    }

    @IdentityLazy
    protected LP getImportObjectAction(ValueClass cls) {
        return addAProp(new ImportFromExcelActionProperty(genSID(), (CustomClass) cls));
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
        LP originalCaption = addCProp(StringClass.get(100), original.property.caption);
        LP result = addJProp(group, caption, baseLM.and1, BaseUtils.add(new Object[]{originalCaption}, directLI(hideProperty)));
        return result;
    }

    protected LP addHideCaptionProp(LP original, LP hideProperty) {
        return addHideCaptionProp(privateGroup, "hideCaption", original, hideProperty);
    }

    protected LP addProp(Property<? extends PropertyInterface> prop) {
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
        lp.property.setSID(transformNameToSID(lp.property.getSID()));
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

    public void addIndex(LP<?>... lps) {
        baseLM.addIndex(lps);
    }

    private void addPersistent(AggregateProperty property) {
        assert !baseLM.idSet.contains(property.getSID());
        property.stored = true;

        baseLM.logger.debug("Initializing stored property...");
        property.markStored(baseLM.tableFactory);
    }

    protected void addPersistent(LP lp) {
        addPersistent((AggregateProperty) lp.property);
    }

    protected void addConstraint(LP<?> lp, boolean checkChange) {
        lp.property.setConstraint(checkChange);
    }

    protected <L extends PropertyInterface, T extends PropertyInterface> void follows(LP<T> first, LP<L> second, int... mapping) {
        Map<L, T> mapInterfaces = new HashMap<L, T>();
        for (int i = 0; i < second.listInterfaces.size(); i++) {
            mapInterfaces.put(second.listInterfaces.get(i), first.listInterfaces.get(mapping[i] - 1));
        }
        addProp(first.property.addFollows(new PropertyMapImplement<L, T>(second.property, mapInterfaces)));
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

    protected void setNotNull(LP property) {
        setNotNull(property, PropertyFollows.RESOLVE_TRUE);
    }

    protected void setNotNull(LP property, int resolve) {

        ValueClass[] values = property.getMapClasses();

        LP checkProp = addCProp(LogicalClass.instance, true, values);

        Map mapInterfaces = new HashMap();
        for (int i = 0; i < property.listInterfaces.size(); i++) {
            mapInterfaces.put(property.listInterfaces.get(i), checkProp.listInterfaces.get(i));
        }
        addProp(
                checkProp.property.addFollows(
                        new PropertyMapImplement(property.property, mapInterfaces),
                        ServerResourceBundle.getString("logics.property") + " " + property.property.caption + " [" + property.property.getSID() + "] " + ServerResourceBundle.getString("logics.property.not.defined"),
                        resolve));
    }

    // получает свойство is
    // для множества классов есть CProp
    public LP is(ValueClass valueClass) {
        LP isProp = baseLM.is.get(valueClass);
        if (isProp == null) {
            isProp = addCProp(valueClass.toString() + ServerResourceBundle.getString("logics.pr"), LogicalClass.instance, true, valueClass);
            baseLM.is.put(valueClass, isProp);
        }
        return isProp;
    }

    public LP object(ValueClass valueClass) {
        LP objectProp = baseLM.object.get(valueClass);
        if (objectProp == null) {
            objectProp = addJProp(valueClass.toString(), baseLM.and1, 1, is(valueClass), 1);
            baseLM.object.put(valueClass, objectProp);
        }
        return objectProp;
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
        return addCProp("dumbProperty" + interfaces, StringClass.get(0), "", params);
    }

    protected <T extends FormEntity> T addFormEntity(T form) {
        form.richDesign = form.createDefaultRichDesign();
        return form;
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
        form.addPropertyDraw(baseLM.delete, object).shouldBeLast = shouldBeLast;
        if (actionImport)
            form.forceDefaultDraw.put(form.addPropertyDraw(getImportObjectAction(object.baseClass)), object.groupTo);

        PropertyDrawEntity actionAddPropertyDraw;
        if (checkObject == null) {
            actionAddPropertyDraw = form.addPropertyDraw(getAddObjectAction(object.baseClass));
        } else {
            actionAddPropertyDraw = form.addPropertyDraw(
                    getAddObjectActionWithClassCheck(object.baseClass, checkObjectClass != null ? checkObjectClass : checkObject.baseClass),
                    checkObject);

            actionAddPropertyDraw.shouldBeLast = shouldBeLast;
            actionAddPropertyDraw.forceViewType = ClassViewType.PANEL;
        }
        actionAddPropertyDraw.shouldBeLast = shouldBeLast;
        form.forceDefaultDraw.put(actionAddPropertyDraw, object.groupTo);
    }

    protected void addFormActions(FormEntity form, ObjectEntity object) {
        addFormActions(form, object, true);
    }

    protected void addFormActions(FormEntity form, ObjectEntity object, boolean shouldBeLast) {
        form.addPropertyDraw(baseLM.delete, object).shouldBeLast = shouldBeLast;

        PropertyDrawEntity actionEditPropertyDraw = form.addPropertyDraw(getEditFormAction((ConcreteCustomClass)object.baseClass));
        actionEditPropertyDraw.shouldBeLast = shouldBeLast;
        actionEditPropertyDraw.forceViewType = ClassViewType.PANEL;

        form.forceDefaultDraw.put(actionEditPropertyDraw, object.groupTo);

        LP addForm = getAddFormAction((ConcreteCustomClass)object.baseClass);
        PropertyDrawEntity actionAddPropertyDraw = form.addPropertyDraw(addForm);
        actionAddPropertyDraw.shouldBeLast = shouldBeLast;
        actionAddPropertyDraw.forceViewType = ClassViewType.PANEL;

        // todo : так не очень правильно делать - получается, что мы добавляем к Immutable объекту FormActionProperty ссылки на ObjectEntity
        ((FormActionProperty)addForm.property).seekOnOk.add(object);

        form.forceDefaultDraw.put(actionAddPropertyDraw, object.groupTo);
    }
}
