package platform.server.form.instance;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.Result;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.FormEventType;
import platform.interop.Scroll;
import platform.interop.action.*;
import platform.interop.form.*;
import platform.server.Context;
import platform.server.Message;
import platform.server.ParamMessage;
import platform.server.auth.SecurityPolicy;
import platform.server.caches.ManualLazy;
import platform.server.classes.*;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.query.Query;
import platform.server.data.type.ObjectType;
import platform.server.data.type.ParseException;
import platform.server.data.type.Type;
import platform.server.data.type.TypeSerializer;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.FilterEntity;
import platform.server.form.entity.filter.NotFilterEntity;
import platform.server.form.entity.filter.NotNullFilterEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.form.instance.filter.CompareValue;
import platform.server.form.instance.filter.FilterInstance;
import platform.server.form.instance.filter.RegularFilterGroupInstance;
import platform.server.form.instance.filter.RegularFilterInstance;
import platform.server.form.instance.listener.CustomClassListener;
import platform.server.form.instance.listener.FocusListener;
import platform.server.form.instance.remote.RemoteDialog;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.logics.property.derived.MaxChangeProperty;
import platform.server.logics.property.derived.OnChangeProperty;
import platform.server.session.*;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

import static platform.base.BaseUtils.mergeSet;
import static platform.interop.ClassViewType.*;
import static platform.interop.Order.*;
import static platform.server.form.instance.GroupObjectInstance.*;
import static platform.server.logics.ServerResourceBundle.getString;

// класс в котором лежит какие изменения произошли

// нужен какой-то объект который
//  разделит клиента и серверную часть кинув каждому свои данные
// так клиента волнуют панели на форме, список гридов в привязке, дизайн и порядок представлений
// сервера колышет дерево и св-ва предст. с привязкой к объектам

public class FormInstance<T extends BusinessLogics<T>> extends OverrideModifier implements ExecutionEnvironmentInterface {

    public final T BL;

    SecurityPolicy securityPolicy;

    public CustomClass getCustomClass(int classID) {
        return BL.LM.baseClass.findClassID(classID);
    }

    private Set<Property> hintsIncrementTable;
    private List<Property> hintsIncrementList = null;
    private List<Property> getHintsIncrementList() {
        if(hintsIncrementList==null) {
            hintsIncrementList = new ArrayList<Property>();
            for(Property property : BL.getPropertyList())
                if(hintsIncrementTable.contains(property)) // чтобы в лексикографике был список
                    hintsIncrementList.add(property);
        }
        return hintsIncrementList;
    }
    
    Set<Property> hintsNoUpdate = new HashSet<Property>();

    public final DataSession session;
    public final NoUpdate noUpdate = new NoUpdate();
    public final IncrementProps increment = new IncrementProps();
    
    public void addHintNoUpdate(Property property) {
        hintsNoUpdate.add(property);
        noUpdate.add(property);
    }

    public boolean isHintIncrement(Property property) {
        return hintsIncrementTable.contains(property);
    }
    public boolean allowHintIncrement(Property property) {
        return true;
    }
    public void addHintIncrement(Property property) {
        hintsIncrementList = null;
        usedProperties = null;
        boolean noHint = hintsIncrementTable.add(property);
        assert noHint;
        try {
            readIncrement(property);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private <P extends PropertyInterface> void rereadIncrement(Property<P> property) throws SQLException {
        increment.remove(property, session.sql);
        readIncrement(property);
    }

    private <P extends PropertyInterface> void readIncrement(Property<P> property) throws SQLException {
        if(property.hasChanges(this))
            increment.add(property, property.readChangeTable(session.sql, this, BL.LM.baseClass, session.env));
    }

    public <P extends PropertyInterface> void dropIncrement(PropertyChanges changes) throws SQLException {
        for(Property property : hintsIncrementTable)
            if(property.hasChanges(changes, true)) // если зависит от changes - drop'аем
                increment.remove(property, session.sql);
    }

    public Set<Property> getUpdateProperties(PropertyChanges propChanges) {
        return Property.hasChanges(getUsedProperties(), noUpdate.getPropertyChanges().add(propChanges), true);
    }

    public Set<Property> getUpdateProperties() {
        return Property.hasChanges(getUsedProperties(), getPropertyChanges(), false);
    }

    private final WeakReference<FocusListener<T>> weakFocusListener;
    public FocusListener<T> getFocusListener() {
        return weakFocusListener.get();
    }

    private final WeakReference<CustomClassListener> weakClassListener;
    public CustomClassListener getClassListener() {
        return weakClassListener.get();
    }

    public final FormEntity<T> entity;

    public final InstanceFactory instanceFactory;

    // для импорта конструктор, объекты пустые
    public FormInstance(FormEntity<T> entity, T BL, DataSession session, SecurityPolicy securityPolicy, FocusListener<T> focusListener, CustomClassListener classListener, PropertyObjectInterfaceInstance computer) throws SQLException {
        this(entity, BL, session, securityPolicy, focusListener, classListener, computer, new HashMap<ObjectEntity, DataObject>(), false);
    }

    public FormInstance(FormEntity<T> entity, T BL, DataSession session, SecurityPolicy securityPolicy, FocusListener<T> focusListener, CustomClassListener classListener, PropertyObjectInterfaceInstance computer, boolean interactive) throws SQLException {
        this(entity, BL, session, securityPolicy, focusListener, classListener, computer, new HashMap<ObjectEntity, DataObject>(), interactive);
    }

    public FormInstance(FormEntity<T> entity, T BL, DataSession session, SecurityPolicy securityPolicy, FocusListener<T> focusListener, CustomClassListener classListener, PropertyObjectInterfaceInstance computer, Map<ObjectEntity, ? extends ObjectValue> mapObjects, boolean interactive) throws SQLException {
        this(entity, BL, session, securityPolicy, focusListener, classListener, computer, mapObjects, interactive, null);
    }

    public FormInstance(FormEntity<T> entity, T BL, DataSession session, SecurityPolicy securityPolicy, FocusListener<T> focusListener, CustomClassListener classListener, PropertyObjectInterfaceInstance computer, Map<ObjectEntity, ? extends ObjectValue> mapObjects, boolean interactive, Set<FilterEntity> additionalFixedFilters) throws SQLException {
        lateInit(noUpdate, increment, session);
        this.session = session;
        this.entity = entity;
        this.BL = BL;
        this.securityPolicy = securityPolicy;

        instanceFactory = new InstanceFactory(computer);

        this.weakFocusListener = new WeakReference<FocusListener<T>>(focusListener);
        this.weakClassListener = new WeakReference<CustomClassListener>(classListener);

        fillHints(false);

        for (int i = 0; i < entity.groups.size(); i++) {
            GroupObjectInstance groupObject = instanceFactory.getInstance(entity.groups.get(i));
            groupObject.order = i;
            groupObject.setClassListener(classListener);
            groups.add(groupObject);
        }

        for (TreeGroupEntity treeGroup : entity.treeGroups) {
            treeGroups.add(instanceFactory.getInstance(treeGroup));
        }

        for (PropertyDrawEntity<?> propertyDrawEntity : entity.propertyDraws)
            if (this.securityPolicy.property.view.checkPermission(propertyDrawEntity.propertyObject.property)) {
                PropertyDrawInstance propertyDrawInstance = instanceFactory.getInstance(propertyDrawEntity);
                if (propertyDrawInstance.toDraw == null) // для Instance'ов проставляем не null, так как в runtime'е порядок меняться не будет
                    propertyDrawInstance.toDraw = instanceFactory.getInstance(propertyDrawEntity.getToDraw(entity));
                properties.add(propertyDrawInstance);
            }

        Set<FilterEntity> allFixedFilters = additionalFixedFilters == null
                                            ? entity.fixedFilters
                                            : mergeSet(entity.fixedFilters, additionalFixedFilters);
        for (FilterEntity filterEntity : allFixedFilters) {
            FilterInstance filter = filterEntity.getInstance(instanceFactory);
            filter.getApplyObject().fixedFilters.add(filter);
        }

        for (RegularFilterGroupEntity filterGroupEntity : entity.regularFilterGroups) {
            regularFilterGroups.add(instanceFactory.getInstance(filterGroupEntity));
        }

        for (Entry<OrderEntity<?>, Boolean> orderEntity : entity.fixedOrders.entrySet()) {
            OrderInstance orderInstance = orderEntity.getKey().getInstance(instanceFactory);
            orderInstance.getApplyObject().fixedOrders.put(orderInstance, orderEntity.getValue());
        }

        // в первую очередь ставим на объекты из cache'а
        if (classListener != null) {
            for (GroupObjectInstance groupObject : groups) {
                for (ObjectInstance object : groupObject.objects)
                    if (object.getBaseClass() instanceof CustomClass) {
                        Integer objectID = classListener.getObject((CustomClass) object.getBaseClass());
                        if (objectID != null)
                            groupObject.addSeek(object, session.getDataObject(objectID, ObjectType.instance), false);
                    }
            }
        }

        for (Entry<ObjectEntity, ? extends ObjectValue> mapObject : mapObjects.entrySet()) {
            ObjectInstance instance = instanceFactory.getInstance(mapObject.getKey());
            instance.groupTo.addSeek(instance, mapObject.getValue(), false);
        }

        //устанавливаем фильтры и порядки по умолчанию...
        for (RegularFilterGroupInstance filterGroup : regularFilterGroups) {
            int defaultInd = filterGroup.entity.defaultFilter;
            if (defaultInd >= 0 && defaultInd < filterGroup.filters.size()) {
                setRegularFilter(filterGroup, filterGroup.filters.get(defaultInd));
            }
        }

        Set<GroupObjectInstance> wasOrder = new HashSet<GroupObjectInstance>();
        for (Entry<PropertyDrawEntity<?>, Boolean> entry : entity.defaultOrders.entrySet()) {
            PropertyDrawInstance property = instanceFactory.getInstance(entry.getKey());
            GroupObjectInstance toDraw = property.toDraw;
            Boolean ascending = entry.getValue();

            toDraw.changeOrder(property.propertyObject, wasOrder.contains(toDraw) ? ADD : REPLACE);
            if (!ascending) {
                toDraw.changeOrder(property.propertyObject, DIR);
            }
            wasOrder.add(toDraw);
        }

        applyFilters();
        addObjectOnTransaction(FormEventType.INIT);

        if(!interactive) {
            endApply();
            this.mapObjects = mapObjects;
        }
        this.interactive = interactive;
    }

    public FormUserPreferences loadUserPreferences() {
        Map<String, FormColumnUserPreferences> preferences = new HashMap<String, FormColumnUserPreferences>();
        try {

            ObjectValue formValue = BL.LM.SIDToNavigatorElement.readClasses(session, new DataObject(entity.getSID(), StringClass.get(50)));
            if (formValue.isNull())
                return null;
            
            DataObject formObject = (DataObject) formValue;

            KeyExpr propertyDrawExpr = new KeyExpr("propertyDraw");

            Integer userId = (Integer) BL.LM.currentUser.read(session);
            DataObject currentUser = session.getDataObject(userId, ObjectType.instance);

            Expr customUserExpr = currentUser.getExpr();

            Map<Object, KeyExpr> newKeys = new HashMap<Object, KeyExpr>();
            newKeys.put("propertyDraw", propertyDrawExpr);

            Query<Object, Object> query = new Query<Object, Object>(newKeys);
            query.properties.put("propertyDrawSID", BL.LM.propertyDrawSID.getExpr(propertyDrawExpr));
            query.properties.put("nameShowOverridePropertyDrawCustomUser", BL.LM.nameShowOverridePropertyDrawCustomUser.getExpr(propertyDrawExpr, customUserExpr));
            query.properties.put("columnWidthOverridePropertyDrawCustomUser", BL.LM.columnWidthOverridePropertyDrawCustomUser.getExpr(propertyDrawExpr, customUserExpr));
            query.and(BL.LM.formPropertyDraw.getExpr(propertyDrawExpr).compare(formObject.getExpr(), Compare.EQUALS));

            OrderedMap<Map<Object, Object>, Map<Object, Object>> result = query.execute(session.sql);

            for (Map<Object, Object> values : result.values()) {
                String propertyDrawSID = values.get("propertyDrawSID").toString().trim();
                Boolean needToHide = null;
                Object hide = values.get("nameShowOverridePropertyDrawCustomUser");
                if (hide != null) {
                    if (getString("logics.property.draw.hide").equals(hide.toString().trim()))
                        needToHide = true;
                    else if (getString("logics.property.draw.show").equals(hide.toString().trim()))
                        needToHide = false;
                }
                Integer width = (Integer) values.get("columnWidthOverridePropertyDrawCustomUser");
                preferences.put(propertyDrawSID, new FormColumnUserPreferences(needToHide, width));
            }
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return new FormUserPreferences(preferences);
    }

    public void saveUserPreferences(FormUserPreferences preferences, Boolean forAllUsers) {
        try {
            DataSession dataSession = session.createSession();
            for (Map.Entry<String, FormColumnUserPreferences> entry : preferences.getFormColumnUserPreferences().entrySet()) {
                DataObject userObject = dataSession.getDataObject(BL.LM.currentUser.read(dataSession), ObjectType.instance);
                Integer id = (Integer) BL.LM.SIDNavigatorElementSIDPropertyDrawToPropertyDraw.read(dataSession, new DataObject(entity.getSID(), StringClass.get(50)), new DataObject(entry.getKey(), StringClass.get(50)));
                DataObject propertyDrawObject = dataSession.getDataObject(id, ObjectType.instance);
                if (entry.getValue().isNeedToHide() != null) {
                    int idShow = !entry.getValue().isNeedToHide() ? BL.LM.propertyDrawShowStatus.getID("Hide") : BL.LM.propertyDrawShowStatus.getID("Show");
                    BL.LM.showPropertyDrawCustomUser.execute(idShow, dataSession, propertyDrawObject, userObject);
                    if (forAllUsers)
                        BL.LM.showPropertyDraw.execute(idShow, dataSession, propertyDrawObject, userObject);
                }
                BL.LM.columnWidthPropertyDrawCustomUser.execute(entry.getValue().getWidthUser(), dataSession, propertyDrawObject, userObject);
                if (forAllUsers)
                    BL.LM.columnWidthPropertyDraw.execute(entry.getValue().getWidthUser(), dataSession, propertyDrawObject);
            }
            dataSession.apply(BL);
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private Map<ObjectEntity, ? extends ObjectValue> mapObjects = null;

    public boolean areObjectsFound() {
        assert !interactive;
        for(Entry<ObjectEntity, ? extends ObjectValue> mapObjectInstance : mapObjects.entrySet())
            if(!instanceFactory.getInstance(mapObjectInstance.getKey()).getObjectValue().equals(mapObjectInstance.getValue()))
                return false;
        return true;
    }

    private boolean interactive = true;

    public List<GroupObjectInstance> groups = new ArrayList<GroupObjectInstance>();
    public List<TreeGroupInstance> treeGroups = new ArrayList<TreeGroupInstance>();

    // собсно этот объект порядок колышет столько же сколько и дизайн представлений
    public List<PropertyDrawInstance> properties = new ArrayList<PropertyDrawInstance>();

    private Collection<ObjectInstance> objects;

    @ManualLazy
    public Collection<ObjectInstance> getObjects() {
        if (objects == null) {
            objects = new ArrayList<ObjectInstance>();
            for (GroupObjectInstance group : groups)
                for (ObjectInstance object : group.objects)
                    objects.add(object);
        }
        return objects;
    }

    public void addFixedFilter(FilterEntity newFilter) {
        FilterInstance newFilterInstance = newFilter.getInstance(instanceFactory);
        newFilterInstance.getApplyObject().fixedFilters.add(newFilterInstance);
    }

    // ----------------------------------- Поиск объектов по ID ------------------------------ //
    public GroupObjectInstance getGroupObjectInstance(int groupID) {
        for (GroupObjectInstance groupObject : groups)
            if (groupObject.getID() == groupID)
                return groupObject;
        return null;
    }

    public ObjectInstance getObjectInstance(int objectID) {
        for (ObjectInstance object : getObjects())
            if (object.getID() == objectID)
                return object;
        return null;
    }

    public PropertyDrawInstance getPropertyDraw(int propertyID) {
        for (PropertyDrawInstance property : properties)
            if (property.getID() == propertyID)
                return property;
        return null;
    }

    public RegularFilterGroupInstance getRegularFilterGroup(int groupID) {
        for (RegularFilterGroupInstance filterGroup : regularFilterGroups)
            if (filterGroup.getID() == groupID)
                return filterGroup;
        return null;
    }

    public PropertyDrawInstance getPropertyDraw(Property<?> property, ObjectInstance object) {
        for (PropertyDrawInstance propertyDraw : properties)
            if (property.equals(propertyDraw.propertyObject.property) && propertyDraw.propertyObject.mapping.containsValue(object))
                return propertyDraw;
        return null;
    }

    public PropertyDrawInstance getPropertyDraw(Property<?> property, GroupObjectInstance group) {
        for (PropertyDrawInstance propertyDraw : properties)
            if (property.equals(propertyDraw.propertyObject.property) && (group==null || group.equals(propertyDraw.toDraw)))
                return propertyDraw;
        return null;
    }

    public PropertyDrawInstance getPropertyDraw(Property<?> property) {
        return getPropertyDraw(property, (GroupObjectInstance)null);
    }

    public PropertyDrawInstance getPropertyDraw(LP<?> property) {
        return getPropertyDraw(property.property);
    }

    public PropertyDrawInstance getPropertyDraw(LP<?> property, ObjectInstance object) {
        return getPropertyDraw(property.property, object);
    }

    public PropertyDrawInstance getPropertyDraw(LP<?> property, GroupObjectInstance group) {
        return getPropertyDraw(property.property, group);
    }

    // ----------------------------------- Навигация ----------------------------------------- //

    public void changeGroupObject(GroupObjectInstance group, Scroll changeType) throws SQLException {
        switch (changeType) {
            case HOME:
                group.seek(false);
                break;
            case END:
                group.seek(true);
                break;
        }
    }

    public void expandGroupObject(GroupObjectInstance group, Map<ObjectInstance, DataObject> value) throws SQLException {
        if(group.expandTable==null)
            group.expandTable = group.createKeyTable();
        group.expandTable.insertRecord(session.sql, value, true, true);
        group.updated |= UPDATED_EXPANDS;
    }

    public void collapseGroupObject(GroupObjectInstance group, Map<ObjectInstance, DataObject> value) throws SQLException {
        if(group.expandTable!=null) {
            group.expandTable.deleteRecords(session.sql, value);
            group.updated |= UPDATED_EXPANDS;
        }
    }

    public void expandCurrentGroupObject(ValueClass cls) throws SQLException {
        for (ObjectInstance object : getObjects()) {
            if (object.getBaseClass().isCompatibleParent(cls))
                expandCurrentGroupObject(object);
        }
    }
    
    public void expandCurrentGroupObject(ObjectInstance object) throws SQLException {
        GroupObjectInstance groupObject = object.groupTo;
        if (groupObject != null && groupObject.isInTree()) {
            if (groupObject.parent != null) {
                // если рекурсивное свойство, то просто раскрываем текущий объект
                Map<ObjectInstance, DataObject> value = new HashMap<ObjectInstance, DataObject>();
                for (ObjectInstance obj : groupObject.objects) {
                    ObjectValue objectValue = obj.getObjectValue();
                    if (objectValue instanceof DataObject)
                        value.put(obj, (DataObject)objectValue);
                }
                if (!value.isEmpty())
                    expandGroupObject(groupObject, value);
            } else {
                // раскрываем все верхние groupObject
                for (GroupObjectInstance group : groups) {
                    List<GroupObjectInstance> upGroups = group.getUpTreeGroups();
                    Map<ObjectInstance, DataObject> value = new HashMap<ObjectInstance, DataObject>();
                    int upObjects = 0;
                    for (GroupObjectInstance goi : upGroups) {
                        if (goi != null && !goi.equals(group)) {
                            upObjects += goi.objects.size();
                            value.putAll(goi.getGroupObjectValue());
                        }
                    }
                    if (!value.isEmpty() && value.size() == upObjects) // проверка на то, что в каждом из верхних groupObject выбран какой-то объект
                        expandGroupObject(group.getUpTreeGroup(), value);
                    if (group.equals(groupObject))
                        break;
                }
            }
        }
    }

    public void switchClassView(GroupObjectInstance group) {
        ClassViewType newClassView = switchView(group.curClassView);
        if (group.entity.isAllowedClassView(newClassView)) {
            changeClassView(group, newClassView);
        }
    }

    public void changeClassView(GroupObjectInstance group, ClassViewType show) {
        group.curClassView = show;
        group.updated = group.updated | UPDATED_CLASSVIEW;
    }

    // сстандартные фильтры
    public List<RegularFilterGroupInstance> regularFilterGroups = new ArrayList<RegularFilterGroupInstance>();
    private Map<RegularFilterGroupInstance, RegularFilterInstance> regularFilterValues = new HashMap<RegularFilterGroupInstance, RegularFilterInstance>();

    public void setRegularFilter(RegularFilterGroupInstance filterGroup, int filterId) {
        setRegularFilter(filterGroup, filterGroup.getFilter(filterId));
    }

    public void setRegularFilter(RegularFilterGroupInstance filterGroup, RegularFilterInstance filter) {

        RegularFilterInstance prevFilter = regularFilterValues.get(filterGroup);
        if (prevFilter != null)
            prevFilter.filter.getApplyObject().removeRegularFilter(prevFilter.filter);

        if (filter == null) {
            regularFilterValues.remove(filterGroup);
        } else {
            regularFilterValues.put(filterGroup, filter);
            filter.filter.getApplyObject().addRegularFilter(filter.filter);
        }
    }

    // -------------------------------------- Изменение данных ----------------------------------- //

    // пометка что изменились данные
    public boolean dataChanged = true;

    private DataObject createObject(ConcreteCustomClass cls) throws SQLException {

        if (!securityPolicy.cls.edit.add.checkPermission(cls)) return null;

        return session.addObject(cls);
    }

    // временно
    private boolean checkFilters(GroupObjectInstance groupTo) {
        Set<FilterInstance> filters = new HashSet<FilterInstance>();
        for(FilterInstance filter : groupTo.getSetFilters())
            if (!FilterInstance.ignoreInInterface || filter.isInInterface(groupTo))
                filters.add(filter);
        return filters.equals(groupTo.filters);
    }
    private void resolveAdd(CustomObjectInstance object, ConcreteCustomClass cls, DataObject addObject) throws SQLException {

        // резолвим все фильтры
        assert checkFilters(object.groupTo);
        for (FilterInstance filter : object.groupTo.filters)
            filter.resolveAdd(new ExecutionEnvironment(this), object, addObject);

        for (LP lp : BL.LM.lproperties) {
            Property property = lp.property;
            if (property.autoset) {
                Property.CommonClasses<?> propClasses = property.getCommonClasses();
                ValueClass interfaceClass = BaseUtils.singleValue(propClasses.interfaces);
                if (propClasses.value instanceof CustomClass && interfaceClass instanceof CustomClass&&
                        cls.isChild((CustomClass)interfaceClass)) { // в общем то для оптимизации
                    Integer obj = getClassListener().getObject((CustomClass) propClasses.value);
                    if(obj!=null)
                        lp.execute(obj, new ExecutionEnvironment(this), addObject);
                }
            }
        }

        expandCurrentGroupObject(object);

        // todo : теоретически надо переделывать
        // нужно менять текущий объект, иначе не будет работать ImportFromExcelActionProperty
        object.changeValue(session, addObject);

        object.groupTo.addSeek(object, addObject, false);

        // меняем вид, если при добавлении может получиться, что фильтр не выполнится, нужно как-то проверить в общем случае
//      changeClassView(object.groupTo, ClassViewType.PANEL);

        dataChanged = true;
    }

    // добавляет во все
    public DataObject addObject(ConcreteCustomClass cls) throws SQLException {

        DataObject addObject = createObject(cls);
        if (addObject == null) return addObject;

        for (ObjectInstance object : getObjects()) {
            if (object instanceof CustomObjectInstance && cls.isChild(((CustomObjectInstance) object).baseClass)) {
                resolveAdd((CustomObjectInstance) object, cls, addObject);
            }
        }

        return addObject;
    }

    public DataObject addObject(CustomObjectInstance object, ConcreteCustomClass cls) throws SQLException {
        // пока тупо в базу

        DataObject addObject = createObject(cls);
        if (addObject == null) return addObject;

        resolveAdd(object, cls, addObject);

        return addObject;
    }

    public void changeClass(PropertyObjectInterfaceInstance objectInstance, DataObject dataObject, ConcreteObjectClass cls, boolean groupLast) throws SQLException {
        if(objectInstance instanceof CustomObjectInstance) {
            CustomObjectInstance object = (CustomObjectInstance) objectInstance;

            if (securityPolicy.cls.edit.change.checkPermission(object.currentClass)) {
                object.changeClass(session, dataObject, cls);
                dataChanged = true;
            }
        } else
            session.changeClass(objectInstance, dataObject, cls, groupLast);
    }

    public boolean canChangeClass(CustomObjectInstance object) {
        return securityPolicy.cls.edit.change.checkPermission(object.currentClass);
    }

    public List<ClientAction> changeProperty(PropertyDrawInstance<?> property, Object value, boolean aggValue) throws SQLException {
        return changeProperty(property, value, false, aggValue);
    }

    public List<ClientAction> changeProperty(PropertyDrawInstance<?> property, Object value, boolean all, boolean aggValue) throws SQLException {
        return changeProperty(property, new HashMap<ObjectInstance, DataObject>(), value, all, aggValue);
    }

    public List<ClientAction> changeProperty(PropertyDrawInstance<?> property, Map<ObjectInstance, DataObject> mapDataValues,
                                             PropertyDrawInstance<?> value, Map<ObjectInstance, DataObject> valueColumnKeys, boolean all, boolean aggValue) throws SQLException {
        return changeProperty(property, mapDataValues, value.getChangeInstance(aggValue, BL, valueColumnKeys, this), all, aggValue);
    }

    public List<ClientAction> changeProperty(PropertyDrawInstance<?> property, Map<ObjectInstance, DataObject> mapDataValues, Object value, boolean all, boolean aggValue) throws SQLException {
        assert !property.isReadOnly();
        return changeProperty(property.getChangeInstance(aggValue, BL, mapDataValues, this), value, all ? property.toDraw : null);
    }

    @Message("message.form.change.property")
    public List<ClientAction> changeProperty(@ParamMessage PropertyObjectInstance<?> property, Object value, GroupObjectInstance groupObject) throws SQLException {
        if (securityPolicy.property.change.checkPermission(property.property)) {
            dataChanged = true;
            return property.execute(new ExecutionEnvironment(this), value instanceof CompareValue ? (CompareValue) value : session.getObjectValue(value, property.getType()), groupObject);
        } else {
            return null;
        }
    }

    public byte[] serializePropertyChangeType(PropertyDrawInstance<?> propertyDraw, Map<ObjectInstance, DataObject> keys, boolean aggValue) throws SQLException, IOException {
        assert false:"not used anymore";

        ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
        DataOutputStream outStream = new DataOutputStream(dataStream);

        Type type = getPropertyChangeType(propertyDraw, keys, aggValue);
        if (type != null) {
            outStream.writeBoolean(false);
            TypeSerializer.serializeType(outStream, type);
        } else {
            outStream.writeBoolean(true);
        }

        return dataStream.toByteArray();
    }

    public Type getPropertyChangeType(PropertyDrawInstance<?> property, Map<ObjectInstance, DataObject> keys, boolean aggValue) throws SQLException {
        boolean isReadOnly = property.isReadOnly() ||
                //не даём редактировать ObjectValueProperty в таблице - т.к. это бессмысленно
                (property.toDraw!=null && property.toDraw.curClassView == ClassViewType.GRID && property.propertyObject.property instanceof ObjectValueProperty) ||
                (property.propertyReadOnly != null && property.propertyReadOnly.read(session, this) != null);

        if (!isReadOnly) {
            PropertyObjectInstance<?> change = property.getChangeInstance(aggValue, BL, keys, this);
            if (securityPolicy.property.change.checkPermission(change.property) &&
                    (entity.isActionOnChange(change.property) || change.getValueImplement().canBeChanged(this))) {
                return change.getEditorType();
            }
        }
        return null;
    }

    public List<ClientAction> executeEditAction(PropertyDrawInstance property, String editActionSID, Map<ObjectInstance, DataObject> keys) throws SQLException {
        PropertyObjectInstance editAction = property.getEditAction(editActionSID);
        if (editAction == null && (ServerResponse.CHANGE.equals(editActionSID) || ServerResponse.EDIT_OBJECT.equals(editActionSID) || ServerResponse.GROUP_CHANGE.equals(editActionSID))) {
            //изменение через старый механизм
            return executeChangeEditActionOldWayAdapter(property, editActionSID, keys);
        }
        editAction = editAction.getRemappedPropertyObject(keys);
        return editAction.execute(new ExecutionEnvironment(this), ActionClass.TRUE, null);
    }

    private List<ClientAction> executeChangeEditActionOldWayAdapter(PropertyDrawInstance<?> property, String editActionSID, Map<ObjectInstance, DataObject> keys) throws SQLException {
        //todo: this need thoughtfull refactoring

        //todo: reimplement Ctrl+V
        boolean aggValue = true;
        boolean groupChange = ServerResponse.GROUP_CHANGE.equals(editActionSID);

        Context currentContext = Context.context.get();

        Type changeType = getPropertyChangeType(property, keys, aggValue);
        if (changeType != null) {
            //ask confirm logics...
            PropertyDrawView propertyView = entity.getRichDesign().get(property.entity);
            boolean askConfirm = propertyView.askConfirm;
            Type baseType = propertyView.getType();

            if (askConfirm) {
                String msg;
                if (baseType instanceof ActionClass) {
                    msg = getString("form.instance.do.you.really.want.to.take.action");
                } else {
                    msg = getString("form.instance.do.you.really.want.to.edit.property");
                }

                String caption = propertyView.getCaption();
                if (caption != null) {
                    msg += " \"" + caption + "\"?";
                }

                int result = (Integer)currentContext.requestUserInteraction(new ConfirmClientAction("LS Fusion", msg));
                if (result != JOptionPane.YES_OPTION) {
                    return null;
                }
            }
        }

        if (changeType instanceof ActionClass) {
            //сразу изменяем для action'ов
            return changeProperty(property, keys, true, groupChange, aggValue);
        } else if (changeType instanceof DataClass) {
            Object oldValue = null;
            //не шлём значения для файлов, т.к. на клиенте они не нужны, но весят много
            if (!(changeType instanceof FileClass)) {
                PropertyObjectInstance<?> change = property.getChangeInstance(aggValue, BL, keys, this);
                oldValue = change.read(session, this);
            }
            UserInputResult inputResult = currentContext.requestUserInput(changeType, oldValue);
            if (!inputResult.isCanceled()) {
                return changeProperty(property, keys, inputResult.getValue(), groupChange, aggValue);
            }
        } else if (changeType instanceof ObjectType) {
            try {
                if (ServerResponse.EDIT_OBJECT.equals(editActionSID)) {
                    DialogInstance<T> dialogInstance = createObjectEditorDialog(property);
                    RemoteDialog dialog = currentContext.createRemoteDialog(dialogInstance);
                    currentContext.requestUserInteraction(new DialogClientAction(dialog));
                    return new ArrayList<ClientAction>();
                } else {
                    DialogInstance<T> dialogInstance = createChangeEditorDialog(property);
                    RemoteDialog dialog = currentContext.createRemoteDialog(dialogInstance);
                    currentContext.requestUserInteraction(new DialogClientAction(dialog));
                    if (dialogInstance.getFormResult() == FormCloseType.OK) {
                        Object newValue = dialogInstance.getDialogValue();
                        return changeProperty(property, keys, newValue, groupChange, aggValue);
                    }
                    if (dialogInstance.getFormResult() == FormCloseType.NULL) {
                        return changeProperty(property, keys, null, groupChange, aggValue);
                    }
                }
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }

        return null;
    }

    public void pasteExternalTable(List<Integer> propertyIDs, List<List<Object>> table) throws SQLException {
        List<PropertyDrawInstance> properties = new ArrayList<PropertyDrawInstance>();
        for (Integer id : propertyIDs) {
            properties.add(getPropertyDraw(id));
        }
        GroupObjectInstance groupObject = properties.get(0).toDraw;
        OrderedMap<Map<ObjectInstance, DataObject>, Map<OrderInstance, ObjectValue>> executeList = groupObject.seekObjects(session.sql, session.env, this, BL.LM.baseClass, table.size());

        //создание объектов
        int availableQuantity = executeList.size();
        if (availableQuantity < table.size()) {
            executeList.putAll(groupObject.createObjects(session, this, table.size() - availableQuantity));
        }

        for (Map<ObjectInstance, DataObject> key : executeList.keySet()) {
            List<Object> row = table.get(executeList.indexOf(key));
            for (PropertyDrawInstance property : properties) {
                PropertyObjectInstance propertyObjectInstance = property.getPropertyObjectInstance();

                for (ObjectInstance groupKey : (Collection<ObjectInstance>) propertyObjectInstance.mapping.values()) {
                    if (!key.containsKey(groupKey)) {
                        key.put(groupKey, groupKey.getDataObject());
                    }
                }

                int propertyIndex = properties.indexOf(property);
                if (propertyIndex < row.size() //если вдруг копировали не таблицу - может быть разное кол-во значений в строках
                        && !(propertyObjectInstance.getType() instanceof ActionClass)
                        && !property.isReadOnly() && securityPolicy.property.change.checkPermission(property.getPropertyObjectInstance().property)) {
                    dataChanged = true;
                    Object value = row.get(propertyIndex);
                    propertyObjectInstance.property.execute(BaseUtils.join(propertyObjectInstance.mapping, key), new ExecutionEnvironment(this), value);
                }
            }
        }
    }

    public void pasteMulticellValue(Map<Integer, List<Map<Integer, Object>>> cells, Object value) throws SQLException {
        for (Integer propertyId : cells.keySet()) {
            PropertyDrawInstance property = getPropertyDraw(propertyId);
            PropertyObjectInstance propertyObjectInstance = property.getPropertyObjectInstance();
            for (Map<Integer, Object> keyIds : cells.get(propertyId)) {
                Map<ObjectInstance, DataObject> key = new HashMap<ObjectInstance, DataObject>();
                for (Integer objectId : keyIds.keySet()) {
                    ObjectInstance objectInstance = getObjectInstance(objectId);
                    key.put(objectInstance, session.getDataObject(keyIds.get(objectId), objectInstance.getType()));
                }
                for (ObjectInstance groupKey : (Collection<ObjectInstance>) propertyObjectInstance.mapping.values()) {
                    if (!key.containsKey(groupKey)) {
                        key.put(groupKey, groupKey.getDataObject());
                    }
                }
                if (!(propertyObjectInstance.getType() instanceof ActionClass)
                        && !property.isReadOnly() && securityPolicy.property.change.checkPermission(property.getPropertyObjectInstance().property)) {
                    Object parsedValue;
                    try {
                        parsedValue = propertyObjectInstance.getType().parseString((String) value);
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                    propertyObjectInstance.property.execute(BaseUtils.join(propertyObjectInstance.mapping, key), new ExecutionEnvironment(this), parsedValue);
                    dataChanged = true;
                }
            }
        }
    }

    public int countRecords(int groupObjectID) throws SQLException {
        GroupObjectInstance group = getGroupObjectInstance(groupObjectID);
        Expr expr = GroupExpr.create(new HashMap(), new ValueExpr(1, IntegerClass.instance), group.getWhere(group.getMapKeys(), this), GroupType.SUM, new HashMap());
        Query<Object, Object> query = new Query<Object, Object>(new HashMap<Object, KeyExpr>());
        query.properties.put("quant", expr);
        OrderedMap<Map<Object, Object>, Map<Object, Object>> result = query.execute(session.sql, session.env);
        Integer quantity = (Integer) result.getValue(0).get("quant");
        if (quantity != null) {
            return quantity;
        } else {
            return 0;
        }
    }

    public Object calculateSum(PropertyDrawInstance propertyDraw, Map<ObjectInstance, DataObject> columnKeys) throws SQLException {
        GroupObjectInstance groupObject = propertyDraw.toDraw;

        Map<ObjectInstance, KeyExpr> mapKeys = groupObject.getMapKeys();
        Map<ObjectInstance, Expr> keys = new HashMap<ObjectInstance, Expr>(mapKeys);

        for (ObjectInstance object : columnKeys.keySet()) {
            keys.put(object, columnKeys.get(object).getExpr());
        }
        Expr expr = GroupExpr.create(new HashMap(), propertyDraw.propertyObject.getExpr(keys, this), groupObject.getWhere(mapKeys, this), GroupType.SUM, new HashMap());

        Query<Object, Object> query = new Query<Object, Object>(new HashMap<Object, KeyExpr>());
        query.properties.put("sum", expr);
        OrderedMap<Map<Object, Object>, Map<Object, Object>> result = query.execute(session.sql);
        return result.getValue(0).get("sum");
    }

    public Map<List<Object>, List<Object>> groupData(Map<PropertyDrawInstance, List<Map<ObjectInstance, DataObject>>> toGroup,
                                                     Map<PropertyDrawInstance, List<Map<ObjectInstance, DataObject>>> toSum,
                                                     Map<PropertyDrawInstance, List<Map<ObjectInstance, DataObject>>> toMax, boolean onlyNotNull) throws SQLException {
        GroupObjectInstance groupObject = ((PropertyDrawInstance) toGroup.keySet().toArray()[0]).toDraw;
        Map<ObjectInstance, KeyExpr> mapKeys = groupObject.getMapKeys();

        Map<Object, KeyExpr> keyExprMap = new HashMap<Object, KeyExpr>();
        Map<Object, Expr> exprMap = new HashMap<Object, Expr>();
        for (PropertyDrawInstance property : toGroup.keySet()) {
            int i = 0;
            for (Map<ObjectInstance, DataObject> columnKeys : toGroup.get(property)) {
                i++;
                Map<ObjectInstance, Expr> keys = new HashMap<ObjectInstance, Expr>(mapKeys);
                for (ObjectInstance object : columnKeys.keySet()) {
                    keys.put(object, columnKeys.get(object).getExpr());
                }
                keyExprMap.put(property.getsID() + i, new KeyExpr("expr"));
                exprMap.put(property.getsID() + i, property.propertyObject.getExpr(keys, this));
            }
        }

        Query<Object, Object> query = new Query<Object, Object>(keyExprMap);
        Expr exprQuant = GroupExpr.create(exprMap, new ValueExpr(1, IntegerClass.instance), groupObject.getWhere(mapKeys, this), GroupType.SUM, keyExprMap);
        query.and(exprQuant.getWhere());

        int separator = toSum.size();
        int idIndex = 0;
        for (int i = 0; i < toSum.size() + toMax.size(); i++) {
            Map<PropertyDrawInstance, List<Map<ObjectInstance, DataObject>>> currentMap;
            int index;
            GroupType groupType;
            if (i < separator) {
                currentMap = toSum;
                groupType = GroupType.SUM;
                index = i;
            } else {
                currentMap = toMax;
                groupType = GroupType.MAX;
                index = i - separator;
            }
            PropertyDrawInstance property = (PropertyDrawInstance) currentMap.keySet().toArray()[index];
            if (property == null) {
                query.properties.put("quant", exprQuant);
                continue;
            }
            for (Map<ObjectInstance, DataObject> columnKeys : currentMap.get(property)) {
                idIndex++;
                Map<ObjectInstance, Expr> keys = new HashMap<ObjectInstance, Expr>(mapKeys);
                for (ObjectInstance object : columnKeys.keySet()) {
                    keys.put(object, columnKeys.get(object).getExpr());
                }
                Expr expr = GroupExpr.create(exprMap, property.propertyObject.getExpr(keys, this), groupObject.getWhere(mapKeys, this), groupType, keyExprMap);
                query.properties.put(property.getsID() + idIndex, expr);
                if (onlyNotNull) {
                    query.and(expr.getWhere());
                }
            }
        }

        Map<List<Object>, List<Object>> resultMap = new OrderedMap<List<Object>, List<Object>>();
        OrderedMap<Map<Object, Object>, Map<Object, Object>> result = query.execute(session.sql, session.env);
        for (Map<Object, Object> one : result.keyList()) {
            List<Object> groupList = new ArrayList<Object>();
            List<Object> sumList = new ArrayList<Object>();

            for (PropertyDrawInstance propertyDraw : toGroup.keySet()) {
                for (int i = 1; i <= toGroup.get(propertyDraw).size(); i++) {
                    groupList.add(one.get(propertyDraw.getsID() + i));
                }
            }
            int index = 1;
            for (PropertyDrawInstance propertyDraw : toSum.keySet()) {
                if (propertyDraw == null) {
                    sumList.add(result.get(one).get("quant"));
                    continue;
                }
                for (int i = 1; i <= toSum.get(propertyDraw).size(); i++) {
                    sumList.add(result.get(one).get(propertyDraw.getsID() + index));
                    index++;
                }
            }
            for (PropertyDrawInstance propertyDraw : toMax.keySet()) {
                for (int i = 1; i <= toMax.get(propertyDraw).size(); i++) {
                    sumList.add(result.get(one).get(propertyDraw.getsID() + index));
                    index++;
                }
            }
            resultMap.put(groupList, sumList);
        }
        return resultMap;
    }

    // Обновление данных
    public void refreshData() throws SQLException {

        for (ObjectInstance object : getObjects())
            if (object instanceof CustomObjectInstance)
                ((CustomObjectInstance) object).refreshValueClass(session);
        refresh = true;
        dataChanged = session.hasChanges();
    }

    void addObjectOnTransaction(FormEventType event) throws SQLException {
        for (ObjectInstance object : getObjects()) {
            if (object instanceof CustomObjectInstance) {
                CustomObjectInstance customObject = (CustomObjectInstance) object;
                if (customObject.isAddOnEvent(event)) {
                    addObject(customObject, (ConcreteCustomClass) customObject.gridClass);
                }
            }
            if (object.isResetOnApply())
                object.groupTo.dropSeek(object);
        }
    }

    public boolean checkApply(List<ClientAction> actions) throws SQLException {
        return session.check(BL, actions);
    }

    public boolean apply(BusinessLogics<?> BL, List<ClientAction> actions) throws SQLException {
        actions.addAll(fireOnApply());

        if (entity.isSynchronizedApply)
            synchronized (entity) {
                return syncApply(actions);
            }
        else
            return syncApply(actions);
    }

    private void fillHints(boolean restart) throws SQLException {
        if(restart) {
            increment.cleanIncrementTables(session.sql);
            noUpdate.clear();
            
            hintsIncrementList = null;
            usedProperties = null;
        }

        hintsIncrementTable = new HashSet<Property>(entity.hintsIncrementTable);
        hintsNoUpdate = new HashSet<Property>(entity.hintsNoUpdate);
        noUpdate.addAll(hintsNoUpdate);
    }
    
    private boolean syncApply(List<ClientAction> actions) throws SQLException {
        boolean succeeded = session.apply(BL, actions);

        if (!succeeded)
            return false;

        fillHints(true);

        refreshData();
        addObjectOnTransaction(FormEventType.APPLY);

        dataChanged = true; // временно пока applyChanges синхронен, для того чтобы пересылался факт изменения данных

        actions.add(new LogMessageClientAction(getString("form.instance.changes.saved"), false));
        return true;
    }

    public ExecutionEnvironmentInterface cancel() throws SQLException {
        session.restart(true);

        fillHints(true);

        // пробежим по всем объектам
        for (ObjectInstance object : getObjects())
            if (object instanceof CustomObjectInstance)
                ((CustomObjectInstance) object).updateCurrentClass(session);
        addObjectOnTransaction(FormEventType.CANCEL);

        dataChanged = true;

        return this;
    }

    // ------------------ Через эти методы сообщает верхним объектам об изменениях ------------------- //

    // В дальнейшем наверное надо будет переделать на Listener'ы...
    protected void objectChanged(ConcreteCustomClass cls, Integer objectID) {
    }

    public void changePageSize(GroupObjectInstance groupObject, Integer pageSize) {
        groupObject.setPageSize(pageSize);
    }

    public void gainedFocus() {
        dataChanged = true;
        FocusListener<T> focusListener = getFocusListener();
        if(focusListener!=null)
            focusListener.gainedFocus(this);
    }

    void close() throws SQLException {

        session.incrementChanges.remove(this);
        for (GroupObjectInstance group : groups) {
            if(group.keyTable!=null)
                group.keyTable.drop(session.sql);
            if(group.expandTable!=null)
                group.expandTable.drop(session.sql);
        }
    }

    // --------------------------------------------------------------------------------------- //
    // --------------------- Общение в обратную сторону с ClientForm ------------------------- //
    // --------------------------------------------------------------------------------------- //

    public ConcreteCustomClass getObjectClass(ObjectInstance object) {

        if (!(object instanceof CustomObjectInstance))
            return null;

        return ((CustomObjectInstance) object).currentClass;
    }

    private Collection<Property> usedProperties;
    public Collection<Property> getUsedProperties() {
        if(usedProperties == null) {
            usedProperties = new HashSet<Property>();
            for (PropertyDrawInstance<?> propertyDraw : properties) {
                usedProperties.add(propertyDraw.propertyObject.property);
                if (propertyDraw.propertyCaption != null) {
                    usedProperties.add(propertyDraw.propertyCaption.property);
                }
                if (propertyDraw.propertyReadOnly != null) {
                    usedProperties.add(propertyDraw.propertyReadOnly.property);
                }
                if (propertyDraw.propertyFooter != null) {
                    usedProperties.add(propertyDraw.propertyFooter.property);
                }
                if (propertyDraw.propertyBackground != null) {
                    usedProperties.add(propertyDraw.propertyBackground.property);
                }
                if (propertyDraw.propertyForeground != null) {
                    usedProperties.add(propertyDraw.propertyForeground.property);
                }
            }
            for (GroupObjectInstance group : groups) {
                if (group.propertyBackground != null) {
                    usedProperties.add(group.propertyBackground.property);
                }
                if (group.propertyForeground != null) {
                    usedProperties.add(group.propertyForeground.property);
                }
                group.fillUpdateProperties((Set<Property>) usedProperties);
            }
            usedProperties.addAll(hintsIncrementTable); // собственно пока только hintsIncrementTable не позволяет сделать usedProperties просто IdentityLazy
        }
        return usedProperties;
    }

    public FormInstance<T> createForm(FormEntity<T> form, Map<ObjectEntity, DataObject> mapObjects, boolean newSession, boolean interactive) throws SQLException {
        return createForm(form, mapObjects, session, newSession, interactive);
    }

    public FormInstance<T> createForm(FormEntity<T> form, Map<ObjectEntity, DataObject> mapObjects, DataSession session, boolean newSession, boolean interactive) throws SQLException {
        return new FormInstance<T>(form, BL, newSession ? session.createSession() : session, securityPolicy, getFocusListener(), getClassListener(), instanceFactory.computer, mapObjects, interactive);
    }

    public void forceChangeObject(ObjectInstance object, ObjectValue value) throws SQLException {

        if(object instanceof DataObjectInstance && !(value instanceof DataObject))
            object.changeValue(session, ((DataObjectInstance)object).getBaseClass().getDefaultObjectValue());
        else
            object.changeValue(session, value);

        object.groupTo.addSeek(object, value, false);
    }


    public void forceChangeObject(ValueClass cls, ObjectValue value) throws SQLException {

        for (ObjectInstance object : getObjects()) {
            if (object.getBaseClass().isCompatibleParent(cls))
                forceChangeObject(object, value);
        }
    }

    // todo : временная затычка
    public void seekObject(ObjectInstance object, ObjectValue value) throws SQLException {

        if(entity.eventActions.size() > 0) { // дебилизм конечно но пока так
            forceChangeObject(object, value);
        } else {
            object.groupTo.addSeek(object, value, false);
        }
    }

    public void changeObject(PropertyObjectInterfaceInstance objectInstance, ObjectValue objectValue, List<ClientAction> actions) throws SQLException {
        if(objectInstance instanceof ObjectInstance) {
            ObjectInstance object = (ObjectInstance) objectInstance;

            seekObject(object, objectValue);
            actions.addAll(fireObjectChanged(object)); // запускаем все Action'ы, которые следят за этим объектом
        }
    }

    // "закэшированная" проверка присутствия в интерфейсе, отличается от кэша тем что по сути функция от mutable объекта
    protected Map<PropertyDrawInstance, Boolean> isDrawed = new HashMap<PropertyDrawInstance, Boolean>();

    boolean refresh = true;

    private boolean classUpdated(Updated updated, GroupObjectInstance groupObject) {
        return updated.classUpdated(Collections.singleton(groupObject));
    }

    private boolean objectUpdated(Updated updated, GroupObjectInstance groupObject) {
        return updated.objectUpdated(Collections.singleton(groupObject));
    }

    private boolean objectUpdated(Updated updated, Set<GroupObjectInstance> groupObjects) {
        return updated.objectUpdated(groupObjects);
    }

    private boolean propertyUpdated(PropertyObjectInstance updated, Set<GroupObjectInstance> groupObjects, Collection<Property> changedProps) {
        return dataUpdated(updated, changedProps)
                || groupUpdated(groupObjects, UPDATED_KEYS)
                || objectUpdated(updated, groupObjects);
    }

    private boolean groupUpdated(Collection<GroupObjectInstance> groupObjects, int flags) {
        for (GroupObjectInstance groupObject : groupObjects)
            if ((groupObject.updated & flags) != 0)
                return true;
        return false;
    }

    private boolean dataUpdated(Updated updated, Collection<Property> changedProps) {
        return updated.dataUpdated(changedProps);
    }

    void applyFilters() {
        for (GroupObjectInstance group : groups)
            group.filters = group.getSetFilters();
    }

    void applyOrders() {
        for (GroupObjectInstance group : groups)
            group.orders = group.getSetOrders();
    }

    private static class GroupObjectValue {
        private GroupObjectInstance group;
        private Map<ObjectInstance, DataObject> value;

        private GroupObjectValue(GroupObjectInstance group, Map<ObjectInstance, DataObject> value) {
            this.group = group;
            this.value = value;
        }
    }

    @Message("message.form.increment.read.properties")
    private <P extends PropertyInterface> void updateIncrementTableProps(Collection<Property> changedProps) throws SQLException {
        for(Property<P> property : getHintsIncrementList()) // в changedProps могут быть и cancel'ы и новые изменения
            if(refresh || changedProps.contains(property))
                rereadIncrement(property);
    }

    @Message("message.form.update.props")
    private void updateDrawProps(FormChanges result, Set<GroupObjectInstance> keyGroupObjects, @ParamMessage Set<PropertyReaderInstance> propertyList) throws SQLException {
        Query<ObjectInstance, PropertyReaderInstance> selectProps = new Query<ObjectInstance, PropertyReaderInstance>(GroupObjectInstance.getObjects(getUpTreeGroups(keyGroupObjects)));
        for (GroupObjectInstance keyGroup : keyGroupObjects) {
            NoPropertyTableUsage<ObjectInstance> groupTable = keyGroup.keyTable;
            selectProps.and(groupTable.getWhere(selectProps.mapKeys));
        }

        for (PropertyReaderInstance propertyReader : propertyList) {
            selectProps.properties.put(propertyReader, propertyReader.getPropertyObjectInstance().getExpr(selectProps.mapKeys, this));
        }

        OrderedMap<Map<ObjectInstance, DataObject>, Map<PropertyReaderInstance, ObjectValue>> queryResult = selectProps.executeClasses(session.sql, session.env, BL.LM.baseClass);
        for (PropertyReaderInstance propertyReader : propertyList) {
            Map<Map<ObjectInstance, DataObject>, ObjectValue> propertyValues = new HashMap<Map<ObjectInstance, DataObject>, ObjectValue>();
            for (Entry<Map<ObjectInstance, DataObject>, Map<PropertyReaderInstance, ObjectValue>> resultRow : queryResult.entrySet())
                propertyValues.put(resultRow.getKey(), resultRow.getValue().get(propertyReader));
            result.properties.put(propertyReader, propertyValues);
        }
    }

    @Message("message.form.end.apply")
    public FormChanges endApply() throws SQLException {

        assert interactive;

        final FormChanges result = new FormChanges();

        // если изменились данные, применяем изменения
        Collection<Property> changedProps;
        if (dataChanged) {
            changedProps = session.update(this);

            updateIncrementTableProps(changedProps);
        } else
            changedProps = new ArrayList<Property>();

        GroupObjectValue updateGroupObject = null; // так как текущий groupObject идет относительно treeGroup, а не group
        for (GroupObjectInstance group : groups) {
            if (refresh) {
                //обновляем classViews при refresh
                result.classViews.put(group, group.curClassView);
            }

            Map<ObjectInstance, DataObject> selectObjects = group.updateKeys(session.sql, session.env, this, BL.LM.baseClass, refresh, result, changedProps);
            if(selectObjects!=null) // то есть нужно изменять объект
                updateGroupObject = new GroupObjectValue(group, selectObjects);

            if (group.getDownTreeGroups().size() == 0 && updateGroupObject != null) { // так как в tree группе currentObject друг на друга никак не влияют, то можно и нужно делать updateGroupObject в конце
                updateGroupObject.group.update(session, result, updateGroupObject.value);
                updateGroupObject = null;
            }
        }

        for (Entry<Set<GroupObjectInstance>, Set<PropertyReaderInstance>> entry : BaseUtils.groupSet(getChangedDrawProps(result, changedProps)).entrySet())
            updateDrawProps(result, entry.getKey(), entry.getValue());

        if (dataChanged)
            result.dataChanged = session.hasStoredChanges();

        // сбрасываем все пометки
        for (GroupObjectInstance group : groups) {
            group.userSeeks = null;

            for (ObjectInstance object : group.objects)
                object.updated = 0;
            group.updated = 0;
        }
        refresh = false;
        dataChanged = false;

//        result.out(this);

        return result;
    }

    private Map<PropertyReaderInstance, Set<GroupObjectInstance>> getChangedDrawProps(FormChanges result, Collection<Property> changedProps) {
        final Map<PropertyReaderInstance, Set<GroupObjectInstance>> readProperties = new HashMap<PropertyReaderInstance, Set<GroupObjectInstance>>();

        for (PropertyDrawInstance<?> drawProperty : properties) {
            if (drawProperty.toDraw != null && drawProperty.toDraw.curClassView == HIDE) continue;

            ClassViewType forceViewType = drawProperty.getForceViewType();
            if (forceViewType != null && forceViewType == HIDE) continue;

            Set<GroupObjectInstance> columnGroupGrids = new HashSet<GroupObjectInstance>();
            for (GroupObjectInstance columnGroup : drawProperty.columnGroupObjects)
                if (columnGroup.curClassView == GRID)
                    columnGroupGrids.add(columnGroup);

            Boolean inInterface = null;
            Set<GroupObjectInstance> drawGridObjects = null;
            if (drawProperty.toDraw != null && drawProperty.toDraw.curClassView == GRID && (forceViewType == null || forceViewType == GRID) &&
                    drawProperty.propertyObject.isInInterface(drawGridObjects = BaseUtils.addSet(columnGroupGrids, drawProperty.toDraw), forceViewType != null)) // в grid'е
                inInterface = true;
            else if (drawProperty.propertyObject.isInInterface(drawGridObjects = columnGroupGrids, false)) // в панели
                inInterface = false;

            Boolean previous = isDrawed.put(drawProperty, inInterface);
            if(inInterface!=null) {
                boolean read = refresh || !inInterface.equals(previous) // если изменилось представление
                        || groupUpdated(drawProperty.columnGroupObjects, UPDATED_CLASSVIEW); // изменились группы в колонки (так как отбираются только GRID)
                if(read || propertyUpdated(drawProperty.propertyObject, drawGridObjects, changedProps)) {
                    readProperties.put(drawProperty, drawGridObjects);
                    if(!inInterface) // говорим клиенту что свойство в панели
                        result.panelProperties.add(drawProperty);
                }

                if (drawProperty.propertyCaption != null && (read || propertyUpdated(drawProperty.propertyCaption, columnGroupGrids, changedProps))) {
                    readProperties.put(drawProperty.captionReader, columnGroupGrids);
                }
                if (drawProperty.propertyFooter != null && (read || propertyUpdated(drawProperty.propertyFooter, columnGroupGrids, changedProps))) {
                    readProperties.put(drawProperty.footerReader, columnGroupGrids);
                }
                if (drawProperty.propertyBackground != null && (read || propertyUpdated(drawProperty.propertyBackground, drawGridObjects, changedProps))) {
                    readProperties.put(drawProperty.backgroundReader, drawGridObjects);
                }
                if (drawProperty.propertyForeground != null && (read || propertyUpdated(drawProperty.propertyForeground, drawGridObjects, changedProps))) {
                    readProperties.put(drawProperty.foregroundReader, drawGridObjects);
                }
            } else if (previous!=null) // говорим клиенту что свойство надо удалить
                result.dropProperties.add(drawProperty);
        }

        for (GroupObjectInstance group : groups) { // читаем highlight'ы
            if (group.propertyBackground != null) {
                Set<GroupObjectInstance> gridGroups = (group.curClassView == GRID ? Collections.singleton(group) : new HashSet<GroupObjectInstance>());
                if (refresh || (group.updated & UPDATED_CLASSVIEW) != 0 || propertyUpdated(group.propertyBackground, gridGroups, changedProps))
                    readProperties.put(group.rowBackgroundReader, gridGroups);
            }
            if (group.propertyForeground != null) {
                Set<GroupObjectInstance> gridGroups = (group.curClassView == GRID ? Collections.singleton(group) : new HashSet<GroupObjectInstance>());
                if (refresh || (group.updated & UPDATED_CLASSVIEW) != 0 || propertyUpdated(group.propertyForeground, gridGroups, changedProps))
                    readProperties.put(group.rowForegroundReader, gridGroups);
            }
        }

        return readProperties;
    }

    // возвращает какие объекты на форме показываются
    private Set<GroupObjectInstance> getPropertyGroups() {

        Set<GroupObjectInstance> reportObjects = new HashSet<GroupObjectInstance>();
        for (GroupObjectInstance group : groups)
            if (group.curClassView != HIDE)
                reportObjects.add(group);

        return reportObjects;
    }

    // возвращает какие объекты на форме не фиксируются
    private Set<GroupObjectInstance> getClassGroups() {

        Set<GroupObjectInstance> reportObjects = new HashSet<GroupObjectInstance>();
        for (GroupObjectInstance group : groups)
            if (group.curClassView == GRID)
                reportObjects.add(group);

        return reportObjects;
    }

    // считывает все данные с формы
    public FormData getFormData(Collection<PropertyDrawInstance> propertyDraws, Set<GroupObjectInstance> classGroups) throws SQLException {

        applyFilters();
        applyOrders();

        // пока сделаем тупо получаем один большой запрос

        Query<ObjectInstance, Object> query = new Query<ObjectInstance, Object>(GroupObjectInstance.getObjects(classGroups));
        OrderedMap<Object, Boolean> queryOrders = new OrderedMap<Object, Boolean>();

        for (GroupObjectInstance group : groups) {

            if (classGroups.contains(group)) {

                // не фиксированные ключи
                query.and(group.getWhere(query.mapKeys, this));

                // закинем Order'ы
                for (Entry<OrderInstance, Boolean> order : group.orders.entrySet()) {
                    query.properties.put(order.getKey(), order.getKey().getExpr(query.mapKeys, this));
                    queryOrders.put(order.getKey(), order.getValue());
                }

                for (ObjectInstance object : group.objects) {
                    query.properties.put(object, object.getExpr(query.mapKeys, this));
                    queryOrders.put(object, false);
                }
            }
        }

        FormData result = new FormData();

        for (PropertyDrawInstance<?> property : propertyDraws)
            query.properties.put(property, property.propertyObject.getExpr(query.mapKeys, this));

        OrderedMap<Map<ObjectInstance, Object>, Map<Object, Object>> resultSelect = query.execute(session.sql, queryOrders, 0, session.env);
        for (Entry<Map<ObjectInstance, Object>, Map<Object, Object>> row : resultSelect.entrySet()) {
            Map<ObjectInstance, Object> groupValue = new HashMap<ObjectInstance, Object>();
            for (GroupObjectInstance group : groups)
                for (ObjectInstance object : group.objects)
                    if (classGroups.contains(group))
                        groupValue.put(object, row.getKey().get(object));
                    else
                        groupValue.put(object, object.getObjectValue().getValue());

            Map<PropertyDrawInstance, Object> propertyValues = new HashMap<PropertyDrawInstance, Object>();
            for (PropertyDrawInstance property : propertyDraws)
                propertyValues.put(property, row.getValue().get(property));

            result.add(groupValue, propertyValues);
        }

        return result;
    }

    // pullProps чтобы запретить hint'ить
    public <P extends PropertyInterface, F extends PropertyInterface> Set<FilterEntity> getEditFixedFilters(ClassFormEntity<T> editForm, PropertyObjectInstance<P> changeProperty, GroupObjectInstance selectionGroupObject, Collection<PullChangeProperty> pullProps) {
        Set<FilterEntity> fixedFilters = new HashSet<FilterEntity>();

        PropertyValueImplement<P> implement = changeProperty.getValueImplement();

        for (MaxChangeProperty<?, P> constrainedProperty : implement.property.getMaxChangeProperties(BL.getCheckConstrainedProperties(changeProperty))) {
            pullProps.add(constrainedProperty);
            fixedFilters.add(new NotFilterEntity(new NotNullFilterEntity<MaxChangeProperty.Interface<P>>(
                            constrainedProperty.getPropertyObjectEntity(implement.mapping, editForm.object))));
        }

        ObjectEntity object = editForm.object;
        for (FilterEntity filterEntity : entity.fixedFilters) {
            FilterInstance filter = filterEntity.getInstance(instanceFactory);
            if (filter.getApplyObject() == selectionGroupObject) {
                for (ObjectEntity filterObject : filterEntity.getObjects()) {
                    //добавляем фильтр только, если есть хотя бы один объект который не будет заменён на константу
                    if (filterObject.baseClass == object.baseClass) {
                        fixedFilters.add(filterEntity.getRemappedFilter(filterObject, object, instanceFactory));
                        break;
                    }
                }
                for(PropertyValueImplement<?> filterImplement : filter.getResolveChangeProperties(implement.property)) {
                    OnChangeProperty<F, P> onChangeProperty = (OnChangeProperty<F, P>) filterImplement.property.getOnChangeProperty(implement.property);
                    pullProps.add(onChangeProperty);
                    fixedFilters.add(new NotNullFilterEntity<OnChangeProperty.Interface<F, P>>(
                                    onChangeProperty.getPropertyObjectEntity((Map<F,DataObject>) filterImplement.mapping, implement.mapping, editForm.object)));
                }
            }
        }
        return fixedFilters;
    }

    public Object read(PropertyObjectInstance<?> property) throws SQLException {
        return property.read(session, this);
    }

    public DialogInstance<T> createObjectEditorDialog(PropertyDrawInstance propertyDraw) throws RemoteException, SQLException {
        PropertyObjectInstance<?> changeProperty = propertyDraw.getChangeInstance(BL, this);

        CustomClass objectClass = changeProperty.getDialogClass();
        ClassFormEntity<T> classForm = objectClass.getEditForm(BL.LM);

        Object currentObject = read(changeProperty);
        if (currentObject == null && objectClass instanceof ConcreteCustomClass) {
            currentObject = addObject((ConcreteCustomClass)objectClass).object;
        }

        return currentObject == null
               ? null
               : new DialogInstance<T>(classForm.form, BL, session, securityPolicy, getFocusListener(), getClassListener(), classForm.object, currentObject, instanceFactory.computer);
    }

    public DialogInstance<T> createChangeEditorDialog(PropertyDrawInstance propertyDraw) throws SQLException {
        Result<Property> aggProp = new Result<Property>();
        PropertyObjectInstance<?> changeProperty = propertyDraw.getChangeInstance(aggProp, BL, this);

        ClassFormEntity<T> formEntity = changeProperty.getDialogClass().getDialogForm(BL.LM);
        Set<PullChangeProperty> pullProps = new HashSet<PullChangeProperty>();
        Set<FilterEntity> additionalFilters = getEditFixedFilters(formEntity, changeProperty, propertyDraw.toDraw, pullProps);

        ObjectEntity dialogObject = formEntity.object;
        DialogInstance<T> dialog = new DialogInstance<T>(formEntity.form, BL, session, securityPolicy, getFocusListener(), getClassListener(), dialogObject, read(changeProperty), instanceFactory.computer, additionalFilters, pullProps);

        Property<PropertyInterface> filterProperty = aggProp.result;
        if (filterProperty != null) {
            dialog.initFilterPropertyDraw = formEntity.form.getPropertyDraw(filterProperty, dialogObject);
        }

        dialog.undecorated = BL.isDialogUndecorated();

        return dialog;
    }

    // ---------------------------------------- Events ----------------------------------------

    private List<ClientAction> fireObjectChanged(ObjectInstance object) throws SQLException {
        return fireEvent(object.entity);
    }

    public List<ClientAction> fireOnApply() throws SQLException {
        return fireEvent(FormEventType.APPLY);
    }

    public List<ClientAction> fireOnOk() throws SQLException {
        formResult = FormCloseType.OK;
        return fireEvent(FormEventType.OK);
    }

    public List<ClientAction> fireOnClose() throws SQLException {
        formResult = FormCloseType.CLOSE;
        return fireEvent(FormEventType.CLOSE);
    }

    public List<ClientAction> fireOnNull() throws SQLException {
        formResult = FormCloseType.NULL;
        return fireEvent(FormEventType.NULL);
    }

    private List<ClientAction> fireEvent(Object eventObject) throws SQLException {
        List<PropertyObjectEntity> actionsOnEvent = entity.getActionsOnEvent(eventObject);
        List<ClientAction> clientActions = new ArrayList<ClientAction>();
        if (actionsOnEvent != null) {
            AUTOACTIONS:
            for (PropertyObjectEntity autoAction : actionsOnEvent) {
                PropertyObjectInstance autoActionInstance = instanceFactory.getInstance(autoAction);
                if (autoActionInstance.isInInterface(null)) {
                    List<ClientAction> actions = changeProperty(autoActionInstance, read(autoActionInstance) == null ? true : null, null);
                    for (ClientAction clientAction : actions) {
                        clientActions.add(clientAction);
                        if (clientAction instanceof DenyCloseFormClientAction) {
                            break AUTOACTIONS;
                        }
                    }
                }
            }
        }

        return clientActions;
    }

    public <P extends PropertyInterface> void fireChange(Property<P> property, PropertyChange<P> change) throws SQLException {
        entity.onChange(property, change, new ExecutionEnvironment(session));
    }

    private FormCloseType formResult = FormCloseType.NULL;

    public FormCloseType getFormResult() {
        return formResult;
    }

    public DataSession getSession() {
        return session;
    }

    public Modifier getModifier() {
        return this;
    }

    public FormInstance getFormInstance() {
        return this;
    }

    public boolean isInTransaction() {
        return false;
    }
}
