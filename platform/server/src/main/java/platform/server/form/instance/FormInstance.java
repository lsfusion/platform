package platform.server.form.instance;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.interop.ClassViewType;
import platform.interop.Scroll;
import platform.interop.action.ClientAction;
import platform.interop.action.ResultClientAction;
import platform.interop.exceptions.ComplexQueryException;
import platform.server.auth.SecurityPolicy;
import platform.server.caches.ManualLazy;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.CustomClass;
import platform.server.data.query.Query;
import platform.server.data.type.TypeSerializer;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.FilterEntity;
import platform.server.form.entity.filter.NotFilterEntity;
import platform.server.form.entity.filter.NotNullFilterEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.form.instance.filter.FilterInstance;
import platform.server.form.instance.filter.RegularFilterGroupInstance;
import platform.server.form.instance.filter.RegularFilterInstance;
import platform.server.form.instance.listener.CustomClassListener;
import platform.server.form.instance.listener.FocusListener;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.logics.property.derived.MaxChangeProperty;
import platform.server.session.*;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

import static platform.interop.ClassViewType.*;
import static platform.server.form.instance.GroupObjectInstance.*;

// класс в котором лежит какие изменения произошли

// нужен какой-то объект который
//  разделит клиента и серверную часть кинув каждому свои данные
// так клиента волнуют панели на форме, список гридов в привязке, дизайн и порядок представлений
// сервера колышет дерево и св-ва предст. с привязкой к объектам

public class FormInstance<T extends BusinessLogics<T>> extends NoUpdateModifier {

    public final T BL;

    public DataSession session;

    public ExprChanges getSession() {
        return session;
    }

    SecurityPolicy securityPolicy;

    public CustomClass getCustomClass(int classID) {
        return BL.baseClass.findClassID(classID);
    }

    public Modifier<? extends Changes> update(final ExprChanges sessionChanges) {
        return new NoUpdateModifier(hintsNoUpdate) {
            public ExprChanges getSession() {
                return sessionChanges;
            }
        };
    }

    public Set<Property> getUpdateProperties(ExprChanges sessionChanges) {
        return getUpdateProperties(update(sessionChanges));
    }

    public Set<Property> getUpdateProperties(Modifier<? extends Changes> modifier) {
        Set<Property> properties = new HashSet<Property>();
        for (Property<?> updateProperty : getUpdateProperties())
            if (updateProperty.hasChanges(modifier))
                properties.add(updateProperty);
        return properties;
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

    public FormInstance(FormEntity<T> entity, T BL, DataSession session, SecurityPolicy securityPolicy, FocusListener<T> focusListener, CustomClassListener classListener, PropertyObjectInterfaceInstance computer) throws SQLException {
        this(entity, BL, session, securityPolicy, focusListener, classListener, computer, new HashMap<ObjectEntity, DataObject>());
    }

    public FormInstance(FormEntity<T> entity, T BL, DataSession session, SecurityPolicy securityPolicy, FocusListener<T> focusListener, CustomClassListener classListener, PropertyObjectInterfaceInstance computer, Map<ObjectEntity, ? extends ObjectValue> mapObjects) throws SQLException {
        this.entity = entity;
        this.BL = BL;
        this.session = session;
        this.securityPolicy = securityPolicy;

        instanceFactory = new InstanceFactory(computer);

        this.weakFocusListener = new WeakReference<FocusListener<T>>(focusListener);
        this.weakClassListener = new WeakReference<CustomClassListener>(classListener);

        hintsNoUpdate = entity.hintsNoUpdate;

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


        for (FilterEntity filterEntity : entity.fixedFilters) {
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

        addObjectOnTransaction();

        for (Entry<ObjectEntity, ? extends ObjectValue> mapObject : mapObjects.entrySet()) {
            ObjectInstance instance = instanceFactory.getInstance(mapObject.getKey());
            instance.groupTo.addSeek(instance, mapObject.getValue(), false);
        }
    }

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

    public PropertyDrawInstance getPropertyDraw(Property<?> property, GroupObjectInstance group) {
        for (PropertyDrawInstance propertyDraw : properties)
            if (property.equals(propertyDraw.propertyObject.property) && (group==null || group.equals(propertyDraw.toDraw)))
                return propertyDraw;
        return null;
    }

    public PropertyDrawInstance getPropertyDraw(Property<?> property) {
        return getPropertyDraw(property, null);
    }

    public PropertyDrawInstance getPropertyDraw(LP<?> property) {
        return getPropertyDraw(property.property);
    }

    public PropertyDrawInstance getPropertyDraw(LP<?> property, GroupObjectInstance group) {
        return getPropertyDraw(property.property, group);
    }

    public PropertyObjectInstance<?> getChangePropertyObjectInstance(PropertyDrawInstance<?> propertyDraw) throws SQLException {
        PropertyObjectInstance<?> change = propertyDraw.propertyObject.getChangeInstance();

        boolean isReadOnly = entity.getRichDesign().getProperty(propertyDraw.entity).readOnly;

        // если readOnly свойство лежит в groupObject в виде панели с одним входом, то показываем диалог выбора объекта
        if ((isReadOnly || !change.getValueImplement().canBeChanged(this))
            && !(propertyDraw.propertyObject.property instanceof ObjectValueProperty)
            && propertyDraw.toDraw != null
            && propertyDraw.toDraw.curClassView == ClassViewType.PANEL
            && propertyDraw.toDraw.objects.size() == 1
            && propertyDraw.propertyObject.mapping.values().size() == 1
            && propertyDraw.propertyObject.mapping.values().iterator().next() == propertyDraw.toDraw.objects.iterator().next()) {

            ObjectInstance singleObject = BaseUtils.single(propertyDraw.toDraw.objects);
            ObjectValueProperty objectValueProperty = BL.getObjectValueProperty(singleObject.getBaseClass());

            return objectValueProperty.getImplement().mapObjects(
                    Collections.singletonMap(
                            BaseUtils.single(objectValueProperty.interfaces),
                            singleObject));
        }

        //контролируем возможность изменения свойства здесь
        return !isReadOnly ? change : null;
    }

    public void serializePropertyEditorType(DataOutputStream outStream, PropertyDrawInstance<?> propertyDraw) throws SQLException, IOException {
        PropertyObjectInstance<?> change = getChangePropertyObjectInstance(propertyDraw);
        if (change != null && securityPolicy.property.change.checkPermission(change.property) && change.getValueImplement().canBeChanged(this)) {
            outStream.writeBoolean(false);
            TypeSerializer.serializeType(outStream, change.getEditorType());
        } else {
            outStream.writeBoolean(true);
        }
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
        group.expandTable.insertRecord(session.sql, value, true);
        group.updated |= UPDATED_EXPANDS;
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
    private boolean dataChanged = false;

    private DataObject createObject(ConcreteCustomClass cls) throws SQLException {

        if (!securityPolicy.cls.edit.add.checkPermission(cls)) return null;

        return session.addObject(cls, this);
    }

    private void resolveAddObject(CustomObjectInstance object, ConcreteCustomClass cls, DataObject addObject) throws SQLException {

        // резолвим все фильтры
        for (FilterInstance filter : object.groupTo.getSetFilters())
            if (!FilterInstance.ignoreInInterface || filter.isInInterface(object.groupTo)) // если ignoreInInterface проверить что в интерфейсе
                filter.resolveAdd(session, this, object, addObject);

        object.groupTo.addSeek(object, addObject, false);

        // меняем вид, если при добавлении может получиться, что фильтр не выполнится, нужно как-то проверить в общем случае
//      changeClassView(object.groupTo, ClassViewType.PANEL);

        dataChanged = true;
    }

    // добавляет во все
    public DataObject addObject(ConcreteCustomClass cls) throws SQLException {

        DataObject addObject = createObject(cls);
        if (addObject == null) return addObject;

        for (ObjectInstance object : getObjects())
            if (object instanceof CustomObjectInstance && cls.isChild(((CustomObjectInstance) object).baseClass))
                resolveAddObject((CustomObjectInstance) object, cls, addObject);

        return addObject;
    }

    public DataObject addObject(CustomObjectInstance object, ConcreteCustomClass cls) throws SQLException {
        // пока тупо в базу

        DataObject addObject = createObject(cls);
        if (addObject == null) return addObject;

        resolveAddObject(object, cls, addObject);

        return addObject;
    }

    public void changeClass(CustomObjectInstance object, DataObject change, int classID) throws SQLException {
        if (securityPolicy.cls.edit.change.checkPermission(object.currentClass)) {
            object.changeClass(session, change, classID);
            dataChanged = true;
        }
    }

    public boolean canChangeClass(CustomObjectInstance object) {
        return securityPolicy.cls.edit.change.checkPermission(object.currentClass);
    }

    public List<ClientAction> changeProperty(PropertyDrawInstance<?> property, Object value) throws SQLException {
        return changeProperty(property.propertyObject, value, null);
    }

    public List<ClientAction> changeProperty(PropertyDrawInstance<?> property, Object value, RemoteForm executeForm, boolean all) throws SQLException {
        return changeProperty(property.propertyObject, value, executeForm, all ? property.toDraw : null);
    }

    public List<ClientAction> changeProperty(PropertyDrawInstance<?> property, Object value, RemoteForm executeForm, boolean all, Map<ObjectInstance, DataObject> mapDataValues) throws SQLException {
        PropertyObjectInstance<?> propertyObject = getChangePropertyObjectInstance(property);

        return changeProperty(propertyObject.getRemappedPropertyObject(mapDataValues), value, executeForm, all ? property.toDraw : null);
    }

    public List<ClientAction> changeProperty(PropertyObjectInstance<?> property, Object value, RemoteForm executeForm) throws SQLException {
        return changeProperty(property, value, executeForm, null);
    }

    public List<ClientAction> changeProperty(PropertyObjectInstance<?> property, Object value, RemoteForm executeForm, GroupObjectInstance groupObject) throws SQLException {
        if (securityPolicy.property.change.checkPermission(property.property)) {
            dataChanged = true;
            // изменяем св-во
            return property.getChangeInstance().execute(session, value, this, executeForm, groupObject);
        } else {
            return null;
        }
    }

    // Обновление данных
    public void refreshData() throws SQLException {

        for (ObjectInstance object : getObjects())
            if (object instanceof CustomObjectInstance)
                ((CustomObjectInstance) object).refreshValueClass(session);
        refresh = true;
    }

    void addObjectOnTransaction() throws SQLException {
        for (ObjectInstance object : getObjects()) {
            if (object instanceof CustomObjectInstance) {
                CustomObjectInstance customObject = (CustomObjectInstance) object;
                if (customObject.isAddOnTransaction()) {
                    addObject(customObject, (ConcreteCustomClass) customObject.gridClass);
                }
            }
            if (object.isResetOnApply())
                object.groupTo.dropSeek(object);
        }
    }

    public void applyActionChanges(List<ClientAction> actions) throws SQLException {

        String check = checkApply();
        if (check != null)
            actions.add(new ResultClientAction(check, true));
        else {
            commitApply();

            actions.add(new ResultClientAction("Изменения были удачно записаны...", false));
        }
    }

    public String checkApply() throws SQLException {
        return session.check(BL);
    }

    public void commitApply() throws SQLException {
        session.write(BL);

        refreshData();
        addObjectOnTransaction();

        dataChanged = true; // временно пока applyChanges синхронен, для того чтобы пересылался факт изменения данных
    }

    public void cancelChanges() throws SQLException {
        session.restart(true);

        // пробежим по всем объектам
        for (ObjectInstance object : getObjects())
            if (object instanceof CustomObjectInstance)
                ((CustomObjectInstance) object).updateCurrentClass(session);
        addObjectOnTransaction();

        dataChanged = true;
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

    public Collection<Property> getUpdateProperties() {

        Set<Property> result = new HashSet<Property>();
        for (PropertyDrawInstance<?> propView : properties) {
            result.add(propView.propertyObject.property);
            if (propView.propertyCaption != null)
                result.add(propView.propertyCaption.property);
        }
        for (GroupObjectInstance group : groups) {
            if (group.propertyHighlight != null)
                result.add(group.propertyHighlight.property);
            group.fillUpdateProperties(result);
        }
        return result;
    }

    public FormInstance<T> createForm(FormEntity<T> form, Map<ObjectEntity, DataObject> mapObjects) throws SQLException {
        return new FormInstance<T>(form, BL, session, securityPolicy, getFocusListener(), getClassListener(), instanceFactory.computer, mapObjects);
    }

    public List<ClientAction> changeObject(ObjectInstance object, ObjectValue value, RemoteForm form) throws SQLException {

        if(entity.autoActions.size() > 0) { // дебилизм конечно но пока так
            if(object instanceof DataObjectInstance && !(value instanceof DataObject))
                object.changeValue(session, ((DataObjectInstance)object).getBaseClass().getDefaultObjectValue());
            else
                object.changeValue(session, value);
        } else
            object.groupTo.addSeek(object, value, false);

        // запускаем все Action'ы, которые следят за этим объектом
        return executeAutoActions(object, form);
    }

    // транзакция для отката при exception'ах
    private class ApplyTransaction {

        private class Group {

            private abstract class Object<O extends ObjectInstance> {
                O object;
                int updated;

                private Object(O object) {
                    this.object = object;
                    updated = object.updated;
                }

                void rollback() {
                    object.updated = updated;
                }
            }

            private class Custom extends Object<CustomObjectInstance> {
                ObjectValue value;
                ConcreteCustomClass currentClass;

                private Custom(CustomObjectInstance object) {
                    super(object);
                    value = object.value;
                    currentClass = object.currentClass;
                }

                void rollback() {
                    super.rollback();
                    object.value = value;
                    object.currentClass = currentClass;
                }
            }

            private class Data extends Object<DataObjectInstance> {
                java.lang.Object value;

                private Data(DataObjectInstance object) {
                    super(object);
                    value = object.value;
                }

                void rollback() {
                    super.rollback();
                    object.value = value;
                }
            }

            GroupObjectInstance group;
            boolean upKeys, downKeys;
            Set<FilterInstance> filters;
            OrderedMap<OrderInstance, Boolean> orders;
            OrderedMap<Map<ObjectInstance, platform.server.logics.DataObject>, Map<OrderInstance, ObjectValue>> keys;
            int updated;

            Collection<Object> objects = new ArrayList<Object>();

            NoPropertyTableUsage<ObjectInstance> groupObjectTable;
            NoPropertyTableUsage<ObjectInstance> expandObjectTable;

            private Group(GroupObjectInstance iGroup) {
                group = iGroup;

                filters = new HashSet<FilterInstance>(group.filters);
                orders = new OrderedMap<OrderInstance, Boolean>(group.orders);
                upKeys = group.upKeys;
                downKeys = group.downKeys;
                keys = new OrderedMap<Map<ObjectInstance, DataObject>, Map<OrderInstance, ObjectValue>>(group.keys);
                updated = group.updated;

                for (ObjectInstance object : group.objects)
                    objects.add(object instanceof CustomObjectInstance ? new Custom((CustomObjectInstance) object) : new Data((DataObjectInstance) object));

                groupObjectTable = group.keyTable;
                expandObjectTable = group.expandTable;
            }

            void rollback() throws SQLException {
                group.filters = filters;
                group.orders = orders;
                group.upKeys = upKeys;
                group.downKeys = downKeys;
                group.keys = keys;
                group.updated = updated;

                for (Object object : objects)
                    object.rollback();

                // восстанавливаем ключи в сессии
                if (groupObjectTable == null) {
                    if (group.keyTable != null) {
                        group.keyTable.drop(session.sql);
                        group.keyTable = null;
                    }
                } else {
                    groupObjectTable.writeKeys(session.sql, group.keys.keySet());
                    group.keyTable = groupObjectTable;
                }
                if (expandObjectTable == null) {
                    if (group.expandTable != null) {
                        group.expandTable.drop(session.sql);
                        group.expandTable = null;
                    }
                } else {
                    expandObjectTable.writeKeys(session.sql, group.keys.keySet());
                    group.expandTable = expandObjectTable;
                }
            }
        }

        Collection<Group> groups = new ArrayList<Group>();
        Map<PropertyDrawInstance, Boolean> isDrawed;
        Map<RegularFilterGroupInstance, RegularFilterInstance> regularFilterValues;

        IdentityHashMap<FormInstance, DataSession.UpdateChanges> incrementChanges;
        IdentityHashMap<FormInstance, DataSession.UpdateChanges> appliedChanges;
        IdentityHashMap<FormInstance, DataSession.UpdateChanges> updateChanges;

        Map<CustomClass, SingleKeyNoPropertyUsage> add;
        Map<CustomClass, SingleKeyNoPropertyUsage> remove;
        Map<DataProperty, SinglePropertyTableUsage<ClassPropertyInterface>> data;

        SingleKeyPropertyUsage news = null;

        ApplyTransaction() {
            for (GroupObjectInstance group : FormInstance.this.groups)
                groups.add(new Group(group));
            isDrawed = new HashMap<PropertyDrawInstance, Boolean>(FormInstance.this.isDrawed);
            regularFilterValues = new HashMap<RegularFilterGroupInstance, RegularFilterInstance>(FormInstance.this.regularFilterValues);

            if (dataChanged) {
                incrementChanges = new IdentityHashMap<FormInstance, DataSession.UpdateChanges>(session.incrementChanges);
                appliedChanges = new IdentityHashMap<FormInstance, DataSession.UpdateChanges>(session.appliedChanges);
                updateChanges = new IdentityHashMap<FormInstance, DataSession.UpdateChanges>(session.updateChanges);

                add = new HashMap<CustomClass, SingleKeyNoPropertyUsage>(session.add);
                remove = new HashMap<CustomClass, SingleKeyNoPropertyUsage>(session.remove);
                data = new HashMap<DataProperty, SinglePropertyTableUsage<ClassPropertyInterface>>(session.data);

                news = session.news;
            }
        }

        void rollback() throws SQLException {
            for (Group group : groups)
                group.rollback();
            FormInstance.this.isDrawed = isDrawed;
            FormInstance.this.regularFilterValues = regularFilterValues;

            if (dataChanged) {
                session.incrementChanges = incrementChanges;
                session.appliedChanges = appliedChanges;
                session.updateChanges = updateChanges;

                session.add = add;
                session.remove = remove;
                session.data = data;

                session.news = news;
            }
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

    public FormChanges endApply() throws SQLException {

        ApplyTransaction transaction = new ApplyTransaction();

        final FormChanges result = new FormChanges();

        try {
            // если изменились данные, применяем изменения
            Collection<Property> changedProps;
            Collection<CustomClass> changedClasses = new HashSet<CustomClass>();
            if (dataChanged) {
                changedProps = session.update(this, changedClasses);
            } else {
                changedProps = new ArrayList<Property>();
            }

            GroupObjectValue updateGroupObject = null; // так как текущий groupObject идет относительно treeGroup, а не group
            for (GroupObjectInstance group : groups) {
                Map<ObjectInstance, DataObject> selectObjects = group.updateKeys(session.sql, session.env, this, BL.baseClass, refresh, result, changedProps, changedClasses);
                if(selectObjects!=null) // то есть нужно изменять объект
                    updateGroupObject = new GroupObjectValue(group, selectObjects);

                if(group.getDownTreeGroups().size()==0 && updateGroupObject !=null) { // так как в tree группе currentObject друг на друга никак не влияют, то можно и нужно делать updateGroupObject в конце
                    updateGroupObject.group.update(session, result, updateGroupObject.value);
                    updateGroupObject = null;
                }
            }

            final Map<PropertyReadInstance, Set<GroupObjectInstance>> readProperties = new HashMap<PropertyReadInstance, Set<GroupObjectInstance>>();

            for (PropertyDrawInstance<?> drawProperty : properties) {
                if (drawProperty.toDraw != null && drawProperty.toDraw.curClassView == HIDE) continue;

                ClassViewType forceViewType = drawProperty.getForceViewType();
                if (forceViewType != null && forceViewType == HIDE) continue;

                Set<GroupObjectInstance> columnGroupGrids = new HashSet<GroupObjectInstance>();
                for (GroupObjectInstance columnGroup : drawProperty.columnGroupObjects)
                    if (columnGroup.curClassView == GRID)
                        columnGroupGrids.add(columnGroup);

                Boolean inInterface = null; Set<GroupObjectInstance> drawGridObjects = null;
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

                    if (drawProperty.propertyCaption != null && (read || propertyUpdated(drawProperty.propertyCaption, columnGroupGrids, changedProps)))
                        readProperties.put(drawProperty.caption, columnGroupGrids);
                } else if (previous!=null) // говорим клиенту что свойство надо удалить
                    result.dropProperties.add(drawProperty);
            }

            for (GroupObjectInstance group : groups) // читаем highlight'ы
                if (group.propertyHighlight != null) {
                    Set<GroupObjectInstance> gridGroups = (group.curClassView == GRID ? Collections.singleton(group) : new HashSet<GroupObjectInstance>());
                    if (refresh || (group.updated & UPDATED_CLASSVIEW) != 0 || propertyUpdated(group.propertyHighlight, gridGroups, changedProps))
                        readProperties.put(group, gridGroups);
                }

            for (Entry<Set<GroupObjectInstance>, Set<PropertyReadInstance>> entry : BaseUtils.groupSet(readProperties).entrySet()) {
                Set<GroupObjectInstance> keyGroupObjects = entry.getKey();
                Set<PropertyReadInstance> propertyList = entry.getValue();

                Query<ObjectInstance, PropertyReadInstance> selectProps = new Query<ObjectInstance, PropertyReadInstance>(GroupObjectInstance.getObjects(getUpTreeGroups(keyGroupObjects)));
                for (GroupObjectInstance keyGroup : keyGroupObjects) {
                    NoPropertyTableUsage<ObjectInstance> groupTable = keyGroup.keyTable;
                    selectProps.and(groupTable.getWhere(selectProps.mapKeys));
                }

                for (PropertyReadInstance propertyDraw : propertyList) {
                    selectProps.properties.put(propertyDraw, propertyDraw.getPropertyObject().getExpr(selectProps.mapKeys, this));
                }

                OrderedMap<Map<ObjectInstance, Object>, Map<PropertyReadInstance, Object>> queryResult = selectProps.execute(session.sql, session.env);
                for (PropertyReadInstance propertyDraw : propertyList) {
                    Map<Map<ObjectInstance, DataObject>, Object> propertyValues = new HashMap<Map<ObjectInstance, DataObject>, Object>();
                    for (Entry<Map<ObjectInstance, Object>, Map<PropertyReadInstance, Object>> resultRow : queryResult.entrySet()) {
                        Map<ObjectInstance, Object> keyRow = resultRow.getKey();

                        Map<ObjectInstance, DataObject> row = new HashMap<ObjectInstance, DataObject>();
                        for (GroupObjectInstance keyGroup : keyGroupObjects) {
                            row.putAll(keyGroup.findGroupObjectValue(keyRow));
                        }

                        propertyValues.put(row, resultRow.getValue().get(propertyDraw));
                    }

                    result.properties.put(propertyDraw, propertyValues);
                }
            }

        } catch (ComplexQueryException e) {
            transaction.rollback();
            if (dataChanged) { // если изменились данные cancel'им изменения
                cancelChanges();
                FormChanges cancelResult = endApply();
                cancelResult.message = e.getMessage() + ". Изменения будут отменены";
                return cancelResult;
            } else
                throw e;
        } catch (RuntimeException e) {
            transaction.rollback();
            throw e;
        } catch (SQLException e) {
            transaction.rollback();
            throw e;
        }

        if (dataChanged)
            result.dataChanged = session.hasChanges();

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

    public <P extends PropertyInterface> AbstractClassFormEntity<T> getDataChangeFormEntity(PropertyObjectInstance<P> changeProperty) {
        PropertyValueImplement<P> implement = changeProperty.getValueImplement();
        AbstractClassFormEntity<T> formEntity = BL.getClassEditForm(changeProperty.getDialogClass()).createCopy();
        formEntity.caption = implement.toString();
        for (MaxChangeProperty<?, P> constrainedProperty : BL.getChangeConstrainedProperties(implement.property)) {
            formEntity.addFixedFilter(
                    new NotFilterEntity(
                            new NotNullFilterEntity<MaxChangeProperty.Interface<P>>(
                                    constrainedProperty.getPropertyObjectEntity(implement.mapping, formEntity.getObject())
                            )
                    )
            );
        }
        return formEntity;
    }

    public DialogInstance<T> createClassPropertyDialog(int viewID, int value) throws RemoteException, SQLException {
        AbstractClassFormEntity<T> classForm = BL.getClassEditForm(getPropertyDraw(viewID).propertyObject.getDialogClass());
        return new DialogInstance<T>(classForm, BL, session, securityPolicy, getFocusListener(), getClassListener(), classForm.getObject(), value, instanceFactory.computer);
    }

    public DialogInstance<T> createObjectEditorDialog(int viewID) throws RemoteException, SQLException {
        PropertyDrawInstance propertyDraw = getPropertyDraw(viewID);
        PropertyObjectInstance<?> changeProperty = getChangePropertyObjectInstance(propertyDraw);
        assert changeProperty != null;

        CustomClass objectClass = changeProperty.getDialogClass();
        AbstractClassFormEntity<T> classForm = BL.getObjectForm(changeProperty.getDialogClass());

        Object currentObject = changeProperty.read(session.sql, this, session.env);
        if (currentObject == null && objectClass instanceof ConcreteCustomClass) {
            currentObject = addObject((ConcreteCustomClass)objectClass).object;
        }

        return currentObject == null
               ? null
               : new DialogInstance<T>(classForm, BL, session, securityPolicy, getFocusListener(), getClassListener(), classForm.getObject(), currentObject, instanceFactory.computer);
    }

    public DialogInstance<T> createEditorPropertyDialog(int viewID) throws SQLException {
        PropertyDrawInstance propertyDraw = getPropertyDraw(viewID);
        PropertyObjectInstance property = propertyDraw.propertyObject;
        PropertyObjectInstance<?> changeProperty = getChangePropertyObjectInstance(propertyDraw);
        assert changeProperty != null;

        AbstractClassFormEntity<T> formEntity = getDataChangeFormEntity(changeProperty);

        ObjectEntity dialogObject = formEntity.getObject();
        DialogInstance<T> dialog = new DialogInstance<T>(formEntity, BL, session, securityPolicy, getFocusListener(), getClassListener(), dialogObject, changeProperty.read(session.sql, this, session.env), instanceFactory.computer);

        //если для readOnly свойства возвращалось ObjectValueProperty для изменения объекта,
        //то используем само свойство в качестве фильтра
        Property filterProperty = changeProperty.property != property.getChangeInstance().property
                         ? property.property
                         : property.property.getFilterProperty();

        if (filterProperty != null) {
            PropertyDrawEntity filterPropertyDraw = formEntity.getPropertyDraw(filterProperty, dialogObject);
            if (filterPropertyDraw == null) {
                filterPropertyDraw = formEntity.addPropertyDraw(new LP(filterProperty), dialogObject);
            }
            dialog.initFilterPropertyDraw = filterPropertyDraw;
        }

        return dialog;
    }

    private List<ClientAction> executeAutoActions(ObjectInstance object, RemoteForm form) throws SQLException {

        List<ClientAction> actions = new ArrayList<ClientAction>();
        for (Entry<ObjectEntity, List<PropertyObjectEntity>> autoActions : entity.autoActions.entrySet())
            if (object.equals(instanceFactory.getInstance(autoActions.getKey())))
                for (PropertyObjectEntity autoAction : autoActions.getValue()) {
                    PropertyObjectInstance action = instanceFactory.getInstance(autoAction);
                    if (action.isInInterface(null)) {
                        List<ClientAction> change = changeProperty(action, action.getChangeInstance().read(session.sql, this, session.env) == null ? true : null, form);
                        if (change != null) {
                            actions.addAll(change);
                        }
                    }
                }
        return actions;
    }
}