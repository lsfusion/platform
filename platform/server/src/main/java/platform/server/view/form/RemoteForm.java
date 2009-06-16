/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platform.server.view.form;

import platform.base.BaseUtils;
import platform.interop.Compare;
import platform.interop.form.RemoteFormInterface;
import platform.server.auth.SecurityPolicy;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.classes.ConcreteCustomClass;
import platform.server.data.classes.CustomClass;
import platform.server.data.classes.DataClass;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.data.IDTable;
import platform.server.logics.properties.*;
import platform.server.session.*;
import platform.server.view.navigator.*;
import platform.server.where.Where;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

// класс в котором лежит какие изменения произошли

// нужен какой-то объект который разделит клиента и серверную часть кинув каждому свои данные
// так клиента волнуют панели на форме, список гридов в привязке, дизайн и порядок представлений
// сервера колышет дерево и св-ва предст. с привязкой к объектам

public class RemoteForm<T extends BusinessLogics<T>> implements PropertyUpdateView {

    // используется для записи в сессии изменений в базу - требуется глобально уникальный идентификатор
    public final int GID;

    public final int sessionID;

    public final int ID;

    T BL;

    public DataSession session;

    SecurityPolicy securityPolicy;

    public Map<DataProperty, DefaultData> getDefaultProperties() {
        return BL.defaultProps;
    }

    private class ObjectImplementMapper {

        private Map<ObjectNavigator, ObjectImplement> mapper = new HashMap<ObjectNavigator, ObjectImplement>();

        ObjectImplement doMapping(ObjectNavigator objKey,CustomClassView classView) {

            ObjectImplement objValue;
            if(objKey.baseClass instanceof DataClass)
                objValue = new DataObjectImplement(objKey.ID,objKey.getSID(), (DataClass) objKey.baseClass,objKey.caption);
            else
                objValue = new CustomObjectImplement(objKey.ID,objKey.getSID(), (CustomClass) objKey.baseClass,objKey.caption, classView);

            mapper.put(objKey, objValue);
            return objValue;
        }
    }

    private class GroupObjectImplementMapper {

        private Map<GroupObjectNavigator, GroupObjectImplement> mapper = new HashMap<GroupObjectNavigator, GroupObjectImplement>();
        ObjectImplementMapper objectMapper;

        public GroupObjectImplementMapper(ObjectImplementMapper iobjectMapper) {
            objectMapper = iobjectMapper;
        }

        GroupObjectImplement doMapping(GroupObjectNavigator groupKey,int order,CustomClassView classView) {

            GroupObjectImplement groupValue = new GroupObjectImplement(groupKey.ID,order,
                    groupKey.pageSize,groupKey.gridClassView,groupKey.singleViewType);
            for (ObjectNavigator object : groupKey)
                groupValue.addObject(objectMapper.doMapping(object,classView));

            mapper.put(groupKey, groupValue);
            return groupValue;
        }
    }

    private class PropertyObjectImplementMapper {

        private Map<PropertyObjectNavigator, PropertyObjectImplement> mapper = new HashMap<PropertyObjectNavigator, PropertyObjectImplement>();
        ObjectImplementMapper objectMapper;

        PropertyObjectImplementMapper(ObjectImplementMapper iobjectMapper) {
            objectMapper = iobjectMapper;
        }

        PropertyObjectImplement doMapping(PropertyObjectNavigator<?> propKey) {

            if (mapper.containsKey(propKey)) return mapper.get(propKey);

            PropertyObjectImplement propValue = new PropertyObjectImplement(propKey.property,BaseUtils.join(propKey.mapping,objectMapper.mapper));

            mapper.put(propKey, propValue);
            return propValue;
        }
    }

    private class PropertyViewMapper {

        PropertyObjectImplementMapper propertyMapper;
        GroupObjectImplementMapper groupMapper;

        PropertyViewMapper(PropertyObjectImplementMapper ipropertyMapper, GroupObjectImplementMapper igroupMapper) {
            propertyMapper = ipropertyMapper;
            groupMapper = igroupMapper;
        }

        PropertyView doMapping(PropertyViewNavigator<?> propKey) {

            return new PropertyView(propKey.ID, propKey.getSID(), propertyMapper.doMapping(propKey.view), groupMapper.mapper.get(propKey.toDraw));
        }
    }

    private class FilterMapper {

        private ObjectImplementMapper objectMapper;
        private PropertyObjectImplementMapper propertyMapper;

        FilterMapper(ObjectImplementMapper iobjectMapper, PropertyObjectImplementMapper ipropertyMapper) {
            objectMapper = iobjectMapper;
            propertyMapper = ipropertyMapper;
        }

        CompareFilter doMapping(PropertyObjectImplement mapProperty,CompareFilterNavigator filterKey) throws SQLException {

            ValueLinkNavigator navigatorValue = filterKey.value;
            ValueLink value = null;
            if (navigatorValue instanceof UserLinkNavigator)
                value = new UserValueLink(session.getObjectValue(((UserLinkNavigator)navigatorValue).value,mapProperty.property.getType()));
            if (navigatorValue instanceof ObjectLinkNavigator)
                value = new ObjectValueLink(objectMapper.mapper.get(((ObjectLinkNavigator)navigatorValue).object));
            if (navigatorValue instanceof PropertyLinkNavigator)
                value = new PropertyValueLink(propertyMapper.doMapping(((PropertyLinkNavigator)navigatorValue).property));

            return new CompareFilter(mapProperty, filterKey.compare, value);
        }

        NotNullFilter doMapping(PropertyObjectImplement mapProperty, NotNullFilterNavigator filterKey) {
            return new NotNullFilter(mapProperty);
        }

        Filter doMapping(FilterNavigator filterKey) throws SQLException {
            PropertyObjectImplement mapProperty = propertyMapper.doMapping(filterKey.property);
            if(filterKey instanceof CompareFilterNavigator)
                return doMapping(mapProperty, (CompareFilterNavigator) filterKey);
            if(filterKey instanceof NotNullFilterNavigator)
                return doMapping(mapProperty, (NotNullFilterNavigator) filterKey); 

            throw new RuntimeException("not supported");
        }
    }

    final FocusView<T> focusView;

    public RemoteForm(int iID, T iBL, DataSession iSession, SecurityPolicy isecurityPolicy, NavigatorForm<?> navigatorForm, CustomClassView classView, FocusView<T> iFocusView) throws SQLException {

        ID = iID;
        BL = iBL;
        session = iSession;
        securityPolicy = isecurityPolicy;

        focusView = iFocusView;

        structUpdated = true;

        GID = IDTable.instance.generateID(session, IDTable.FORM);
        sessionID = session.generateSessionID(ID);

        ObjectImplementMapper objectMapper = new ObjectImplementMapper();
        GroupObjectImplementMapper groupObjectMapper = new GroupObjectImplementMapper(objectMapper);
        PropertyObjectImplementMapper propertyMapper = new PropertyObjectImplementMapper(objectMapper);
        PropertyViewMapper propertyViewMapper = new PropertyViewMapper(propertyMapper, groupObjectMapper);
        FilterMapper filterMapper = new FilterMapper(objectMapper, propertyMapper);

        for (int i=0;i<navigatorForm.groups.size();i++)
            groups.add(groupObjectMapper.doMapping(navigatorForm.groups.get(i),i,classView));

        for (PropertyViewNavigator navigatorProperty : navigatorForm.propertyViews)
            if (securityPolicy.property.view.checkPermission(navigatorProperty.view.property))
                properties.add(propertyViewMapper.doMapping(navigatorProperty));

        for (FilterNavigator navigatorFilter : navigatorForm.fixedFilters)
            fixedFilters.add(filterMapper.doMapping(navigatorFilter));

        for (RegularFilterGroupNavigator navigatorGroup : navigatorForm.regularFilterGroups) {

            RegularFilterGroup group = new RegularFilterGroup(navigatorGroup.ID);
            for (RegularFilterNavigator filter : navigatorGroup.filters)
                group.addFilter(new RegularFilter(filter.ID, filterMapper.doMapping(filter.filter), filter.name, filter.key));

            regularFilterGroups.add(group);
        }

        hintsNoUpdate = navigatorForm.hintsNoUpdate;
        hintsSave = navigatorForm.hintsSave;
    }

    public List<GroupObjectImplement> groups = new ArrayList<GroupObjectImplement>();
    public Map<GroupObjectImplement,ViewTable> groupTables = new HashMap<GroupObjectImplement, ViewTable>(); 
    // собсно этот объект порядок колышет столько же сколько и дизайн представлений
    public List<PropertyView> properties = new ArrayList<PropertyView>();

    // карта что сейчас в интерфейсе + карта в классовый\объектный вид
    Map<PropertyView,Boolean> interfacePool = new HashMap<PropertyView, Boolean>();

    // ----------------------------------- Поиск объектов по ID ------------------------------ //

    public GroupObjectImplement getGroupObjectImplement(int groupID) {
        for (GroupObjectImplement groupObject : groups)
            if (groupObject.ID == groupID)
                return groupObject;
        return null;
    }

    public ObjectImplement getObjectImplement(int objectID) {
        for (GroupObjectImplement groupObject : groups)
            for (ObjectImplement object : groupObject)
                if (object.ID == objectID)
                    return object;
        return null;
    }

    public PropertyView getPropertyView(int propertyID) {
        for (PropertyView property : properties)
            if (property.ID == propertyID)
                return property;
        return null;
    }

    public RegularFilterGroup getRegularFilterGroup(int groupID) {
        for (RegularFilterGroup filterGroup : regularFilterGroups)
            if (filterGroup.ID == groupID)
                return filterGroup;
        return null;
    }

    public void serializePropertyEditorObjectValue(DataOutputStream outStream, PropertyView propertyView, boolean externalID) throws SQLException, IOException {

        MapChangeDataProperty<?> change = propertyView.view.getChangeProperty(securityPolicy.property.change, externalID);
        outStream.writeBoolean(change==null);
        if(change!=null)
            change.serializeChange(outStream, session, propertyView.view.getInterfaceValues() , getDefaultProperties(), getNoUpdateProperties());
    }

    // ----------------------------------- Навигация ----------------------------------------- //

    // поиски по свойствам\объектам
    public Map<PropertyObjectImplement,Object> userPropertySeeks = new HashMap<PropertyObjectImplement, Object>();
    public Map<ObjectImplement,Integer> userObjectSeeks = new HashMap<ObjectImplement, Integer>();

    private Map<GroupObjectImplement, Integer> pendingGroupChanges = new HashMap<GroupObjectImplement, Integer>();

    public void changeGroupObject(GroupObjectImplement group, int changeType) throws SQLException {
        pendingGroupChanges.put(group, changeType);
    }

    public void changeGroupObject(GroupObjectImplement group,Map<ObjectImplement,DataObject> value) throws SQLException {
        // проставим все объектам метки изменений
        for(ObjectImplement object : group)
            object.changeValue(session, value.get(object));
    }

    public void switchClassView(GroupObjectImplement group) {
        changeClassView(group, !group.gridClassView);
    }

    private void changeClassView(GroupObjectImplement group,boolean show) {

        if(group.gridClassView == show || group.singleViewType) return;
        group.gridClassView = show;

        group.updated = group.updated | GroupObjectImplement.UPDATED_CLASSVIEW;

        structUpdated = true;
    }

    // Фильтры

    // флаги изменения фильтров\порядков чисто для ускорения
    private boolean structUpdated = true;
    // фильтры !null (св-во), св-во - св-во, св-во - объект, класс св-ва (для < > в том числе)?,

    public Set<Filter> fixedFilters = new HashSet<Filter>();
    public List<RegularFilterGroup> regularFilterGroups = new ArrayList<RegularFilterGroup>();
    private Set<Filter> userFilters = new HashSet<Filter>();

    public void clearUserFilters() {

        userFilters.clear();
        structUpdated = true;
    }

    public void addUserFilter(Filter addFilter) {

        userFilters.add(addFilter);
        structUpdated = true;
    }

    private Map<RegularFilterGroup, RegularFilter> regularFilterValues = new HashMap<RegularFilterGroup, RegularFilter>();
    public void setRegularFilter(RegularFilterGroup filterGroup, RegularFilter filter) {

        if (filter == null || filter.filter == null)
            regularFilterValues.remove(filterGroup);
        else
            regularFilterValues.put(filterGroup, filter);

        structUpdated = true;
    }

    // Порядки

    private LinkedHashMap<PropertyView,Boolean> orders = new LinkedHashMap<PropertyView, Boolean>();

    public void changeOrder(PropertyView propertyView, int modiType) {

        if (modiType == RemoteFormInterface.ORDER_REMOVE)
            orders.remove(propertyView);
        else
        if (modiType == RemoteFormInterface.ORDER_DIR)
            orders.put(propertyView,!orders.get(propertyView));
        else {
            if (modiType == RemoteFormInterface.ORDER_REPLACE) {
                for (PropertyView propView : orders.keySet())
                    if (propView.toDraw == propertyView.toDraw)
                        orders.remove(propView);
            }
            orders.put(propertyView,false);
        }

        structUpdated = true;
    }

    // -------------------------------------- Изменение данных ----------------------------------- //

    // пометка что изменились данные
    private boolean dataChanged = false;

    public void addObject(CustomObjectImplement object, ConcreteCustomClass cls) throws SQLException {
        // пока тупо в базу

        if (!securityPolicy.cls.edit.add.checkPermission(cls)) return;

        DataObject addObject = session.addObject(cls);

        boolean foundConflict = false;

        // берем все текущие CompareFilter на оператор 0(=) делаем ChangeProperty на ValueLink сразу в сессию
        // тогда добавляет для всех других объектов из того же GroupObjectImplement'а, значение ValueLink, GetValueExpr
        for(Filter<?> filter : object.groupTo.filters) {
            if(filter instanceof CompareFilter) {
                CompareFilter<?> compareFilter = (CompareFilter)filter;
                if(compareFilter.compare ==0 && filter.property.property instanceof DataProperty) {
                    JoinQuery<ObjectImplement,String> subQuery = new JoinQuery<ObjectImplement,String>(ObjectImplement.getMapKeys(filter.property.mapping.values()));
                    Map<ObjectImplement,DataObject> fixedObjects = new HashMap<ObjectImplement, DataObject>();
                    for(ObjectImplement mapObject : filter.property.mapping.values()) {
                        ObjectImplement sibObject = (CustomObjectImplement) mapObject;
                        if(sibObject.groupTo !=object.groupTo) {
                            fixedObjects.put(sibObject,sibObject.getValue());
                        } else {
                            if(sibObject!=object)
                                subQuery.and(subQuery.mapKeys.get(sibObject).getIsClassWhere(sibObject.getGridClass().getUpSet()));
                            else
                                fixedObjects.put(sibObject,addObject);
                        }
                    }

                    subQuery.putKeyWhere(fixedObjects);

                    subQuery.properties.put("newvalue", compareFilter.value.getValueExpr(object.groupTo.getClassGroup(),subQuery.mapKeys, session.changes, filter.property.property.getType(), getDefaultProperties(), getNoUpdateProperties()));

                    LinkedHashMap<Map<ObjectImplement, DataObject>, Map<String, ObjectValue>> result = subQuery.executeSelectClasses(session,BL.baseClass);
                    // изменяем св-ва
                    for(Entry<Map<ObjectImplement, DataObject>, Map<String, ObjectValue>> row : result.entrySet()) {
                        DataProperty changeProperty = (DataProperty) filter.property.property;
                        Map<DataPropertyInterface,DataObject> keys = new HashMap<DataPropertyInterface, DataObject>();
                        for(DataPropertyInterface propertyInterface : changeProperty.interfaces) {
                            CustomObjectImplement changeObject = (CustomObjectImplement) filter.property.mapping.get(propertyInterface);
                            keys.put(propertyInterface,row.getKey().get(changeObject));
                        }
                        session.changeProperty(changeProperty,keys,row.getValue().get("newvalue"),false);
                    }
                } else {
                    if (object.groupTo.equals(compareFilter.getApplyObject())) foundConflict = true;
                }
            }
        }

        for (PropertyView prop : orders.keySet())
            if (object.groupTo.equals(prop.toDraw)) foundConflict = true;

        object.changeValue(session, addObject);

        // меняем вид, если при добавлении может получиться, что фильтр не выполнится
        if (foundConflict)
            changeClassView(object.groupTo, false);

        dataChanged = true;
    }

    public void changeClass(CustomObjectImplement object, int classID) throws SQLException {
        object.changeClass(session, classID);
        dataChanged = true;
    }

    public void changePropertyView(PropertyView property, Object value, boolean externalID) throws SQLException {
        changeProperty(property.view, value, externalID);
    }

    private void changeProperty(PropertyObjectImplement property, Object value, boolean externalID) throws SQLException {

        // изменяем св-во
        property.getChangeProperty(securityPolicy.property.change, externalID).change(session,property.getInterfaceValues(),value,externalID);

        dataChanged = true;
    }

    // Обновление данных
    public void refreshData() {

        for(GroupObjectImplement group : groups) {
            group.updated |= GroupObjectImplement.UPDATED_GRIDCLASS;
        }
    }

    // Применение изменений
    public String saveChanges() throws SQLException {
        String applyString = session.apply(BL);
        if(applyString==null)
            refreshData();
        return applyString;
    }

    public void cancelChanges() throws SQLException {
        session.restart(true);

        dataChanged = true;
    }

    // ------------------ Через эти методы сообщает верхним объектам об изменениях ------------------- //

    // В дальнейшем наверное надо будет переделать на Listener'ы...
    protected void objectChanged(ConcreteCustomClass cls, Integer objectID) {}

    public void changePageSize(GroupObjectImplement groupObject, int pageSize) {

        groupObject.pageSize = pageSize;

        structUpdated = true;
    }

    public void gainedFocus() {
        dataChanged = true;
        focusView.gainedFocus(this);
    }

    void close() throws SQLException {

        session.incrementChanges.remove(this);
        for(ViewTable viewTable : groupTables.values())
            session.dropTemporaryTable(viewTable);
    }

    // --------------------------------------------------------------------------------------- //
    // --------------------- Общение в обратную сторону с ClientForm ------------------------- //
    // --------------------------------------------------------------------------------------- //

    public Collection<Property> getUpdateProperties() {

        Set<Property> result = new HashSet<Property>();
        for(PropertyView propView : properties)
            result.add(propView.view.property);
        for(Filter<?> filter : fixedFilters)
            result.addAll(filter.getProperties());
        return result;
    }

    public Collection<Property> hintsNoUpdate = new HashSet<Property>();
    public Collection<Property> getNoUpdateProperties() {
        return hintsNoUpdate;
    }

    public Collection<Property> hintsSave = new HashSet<Property>();

    public boolean hasSessionChanges() {
        return session.changes.hasChanges();
    }

    // транзакция для отката при exception'ах
    private class ApplyTransaction {

        private class Group {

            private class Object {
                ObjectImplement object;
                DataObject value;
                int updated;
//                RemoteClass objectClass;

                private Object(ObjectImplement iObject) {
                    object = iObject;
/*                    value = object.value;
                    updated = object.updated;
                    objectClass = object.currentClass;*/
                }

                void rollback() {
/*                    object.value = value;
                    object.updated = updated;
                    object.currentClass = objectClass;*/
                }
            }

            GroupObjectImplement group;
            Set<Filter> filters;
            LinkedHashMap<PropertyObjectImplement,Boolean> orders;
            boolean upKeys,downKeys;
            List<Map<ObjectImplement,DataObject>> keys;
            // какие ключи активны
            Map<Map<ObjectImplement,DataObject>,Map<PropertyObjectImplement,ObjectValue>> keyOrders;
            int updated;

            Collection<Object> objects = new ArrayList<Object>();

            ViewTable viewTable;

            private Group(GroupObjectImplement iGroup) {
                group = iGroup;

                filters = new HashSet<Filter>(group.filters);
                orders = new LinkedHashMap<PropertyObjectImplement, Boolean>(group.orders);
                upKeys = group.upKeys;
                downKeys = group.downKeys;
                keys = group.keys;
                keyOrders = group.keyOrders;
                updated = group.updated;

                for(ObjectImplement object : group)
                    objects.add(new Object(object));

                viewTable = groupTables.get(group);
            }

            void rollback() throws SQLException {
                group.filters = filters;
                group.orders = orders;
                group.upKeys = upKeys;
                group.downKeys = downKeys;
                group.keys = keys;
                group.keyOrders = keyOrders;
                group.updated = updated;

                for(Object object : objects)
                    object.rollback();

                // восстанавливаем ключи в сессии
                if(viewTable==null) {
                    ViewTable newTable = groupTables.get(group);
                    if(newTable!=null) {
                        session.dropTemporaryTable(newTable);
                        groupTables.remove(group);
                    }
                } else {
                    session.deleteKeyRecords(viewTable, new HashMap<KeyField, Integer>());
                    for(Map<ObjectImplement, DataObject> keyRow : group.keys)
                        session.insertRecord(viewTable,BaseUtils.join(viewTable.mapKeys,keyRow),new HashMap<PropertyField,ObjectValue>());
                    groupTables.put(group,viewTable);                    
                }
            }
        }

        Collection<Group> groups = new ArrayList<Group>();
        Map<PropertyView,Boolean> interfacePool;

        Map<PropertyUpdateView, ViewDataChanges> incrementChanges;

        TableChanges changes;

        ApplyTransaction() {
            for(GroupObjectImplement group : RemoteForm.this.groups)
                groups.add(new Group(group));
            interfacePool = new HashMap<PropertyView, Boolean>(RemoteForm.this.interfacePool);

            if(dataChanged) {
                incrementChanges = new HashMap<PropertyUpdateView, ViewDataChanges>(session.incrementChanges);
                changes = new TableChanges(session.changes);
            }
        }

        void rollback() throws SQLException {
            for(Group group : groups)
                group.rollback();
            RemoteForm.this.interfacePool = interfacePool;

            if(dataChanged) {
                session.incrementChanges = incrementChanges;
                session.changes = changes;
            }
        }
    }

    private final static int DIRECTION_DOWN = 0;
    private final static int DIRECTION_UP = 1;
    private final static int DIRECTION_CENTER = 2;

    public FormChanges endApply() throws SQLException {

        ApplyTransaction transaction = new ApplyTransaction();

        try {
            FormChanges result = new FormChanges();
            
            // если изменились данные, применяем изменения
            Collection<Property> changedProps;
            Collection<CustomClass> changedClasses = new HashSet<CustomClass>();
            if(dataChanged)
                changedProps = session.update(this,changedClasses);
            else
                changedProps = new ArrayList<Property>();

            // бежим по списку вниз
            if(structUpdated) {
                // построим Map'ы
                // очистим старые

                for(GroupObjectImplement group : groups) {
                    group.mapFilters = new HashSet<Filter>();
                    group.mapOrders = new ArrayList<PropertyView>();
                }

                // фильтры
                Set<Filter> filters = new HashSet<Filter>();
                filters.addAll(fixedFilters);
                for (RegularFilter regFilter : regularFilterValues.values()) filters.add(regFilter.filter);
                for (Filter filter : userFilters) {
                    // если вид панельный, то фильтры не нужны
                    if (!filter.property.getApplyObject().gridClassView) continue;
                    filters.add(filter);
                }

                for(Filter filt : filters)
                    filt.getApplyObject().mapFilters.add(filt);

                // порядки
                for(PropertyView order : orders.keySet())
                    order.view.getApplyObject().mapOrders.add(order);

            }

            for(GroupObjectImplement group : groups) {

                if ((group.updated & GroupObjectImplement.UPDATED_CLASSVIEW) != 0) {
                    result.classViews.put(group, group.gridClassView);
                }
                // если изменились :
                // хоть один класс из этого GroupObjectImplement'a - (флаг Updated - 3)
                boolean updateKeys = (group.updated & GroupObjectImplement.UPDATED_GRIDCLASS)!=0;

                // фильтр\порядок (надо сначала определить что в интерфейсе (верхних объектов Group и класса этого Group) в нем затем сравнить с теми что были до) - (Filters, Orders объектов)
                // фильтры
                // если изменилась структура или кто-то изменил класс, перепроверяем
                if(structUpdated) {
                    Set<Filter> newFilter = new HashSet<Filter>();
                    for(Filter filt : group.mapFilters)
                        if(filt.isInInterface(group)) newFilter.add(filt);

                    updateKeys |= !newFilter.equals(group.filters);
                    group.filters = newFilter;
                } else
                    for(Filter filt : group.mapFilters)
                        if(filt.classUpdated(group))
                            updateKeys |= (filt.isInInterface(group)? group.filters.add(filt): group.filters.remove(filt));

                // порядки
                boolean setOrderChanged = false;
                Set<PropertyObjectImplement> setOrders = new HashSet<PropertyObjectImplement>(group.orders.keySet());
                for(PropertyView order : group.mapOrders) {
                    // если изменилась структура или кто-то изменил класс, перепроверяем
                    if(structUpdated || order.view.classUpdated(group))
                        setOrderChanged = (order.view.isInInterface(group)?setOrders.add(order.view):group.orders.remove(order.view)!=null);
                }
                if(structUpdated || setOrderChanged) {
                    // переформирываваем порядок, если структура или принадлежность Order'у изменилась
                    LinkedHashMap<PropertyObjectImplement,Boolean> newOrder = new LinkedHashMap<PropertyObjectImplement, Boolean>();
                    for(PropertyView order : group.mapOrders)
                        if(setOrders.contains(order.view)) newOrder.put(order.view, orders.get(order));

                    updateKeys |= setOrderChanged || !(new ArrayList<Entry<PropertyObjectImplement,Boolean>>(group.orders.entrySet())).equals(
                            new ArrayList<Entry<PropertyObjectImplement,Boolean>>(newOrder.entrySet())); //Group.Orders.equals(NewOrder)
                    group.orders = newOrder;
                }

                // объекты задействованные в фильтре\порядке (по Filters\Orders верхних элементов GroupImplement'ов на флаг Updated - 0)
                if(!updateKeys)
                    for(Filter filt : group.filters)
                        if(filt.objectUpdated(group)) {updateKeys = true; break;}
                if(!updateKeys)
                    for(PropertyObjectImplement order : group.orders.keySet())
                        if(order.objectUpdated(group)) {updateKeys = true; break;}
                // проверим на изменение данных
                if(!updateKeys)
                    for(Filter filt : group.filters)
                        if(dataChanged && filt.dataUpdated(changedProps)) {updateKeys = true; break;}
                if(!updateKeys)
                    for(PropertyObjectImplement order : group.orders.keySet())
                        if(dataChanged && changedProps.contains(order.property)) {updateKeys = true; break;}
                // классы удалились\добавились
                if(!updateKeys && dataChanged) {
                    for(ObjectImplement object : group)
                        if(object.classChanged(changedClasses)) {updateKeys = true; break;}
                }

                // по возврастанию (0), убыванию (1), центру (2) и откуда начинать
                Map<PropertyObjectImplement,ObjectValue> propertySeeks = new HashMap<PropertyObjectImplement, ObjectValue>();

                // объект на который будет делаться активным после нахождения ключей
                Map<ObjectImplement,DataObject> currentObject = group.getGroupObjectValue();

                // объект относительно которого будет устанавливаться фильтр
                Map<ObjectImplement,DataObject> objectSeeks = group.getGroupObjectValue();
                int direction;
                boolean hasMoreKeys = true;

                if (objectSeeks.containsValue(null)) {
                    objectSeeks = new HashMap<ObjectImplement, DataObject>();
                    direction = DIRECTION_DOWN;
                } else
                    direction = DIRECTION_CENTER;

                // Различные переходы - в самое начало или конец
                Integer pendingChanges = pendingGroupChanges.get(group);
                if (pendingChanges == null) pendingChanges = -1;

                if (pendingChanges == RemoteFormInterface.CHANGEGROUPOBJECT_FIRSTROW) {
                    objectSeeks = new HashMap<ObjectImplement, DataObject>();
                    currentObject = null;
                    updateKeys = true;
                    hasMoreKeys = false;
                    direction = DIRECTION_DOWN;
                }

                if (pendingChanges == RemoteFormInterface.CHANGEGROUPOBJECT_LASTROW) {
                    objectSeeks = new HashMap<ObjectImplement, DataObject>();
                    currentObject = null;
                    updateKeys = true;
                    hasMoreKeys = false;
                    direction = DIRECTION_UP;
                }

                // один раз читаем не так часто делается, поэтому не будем как с фильтрами
                for(PropertyObjectImplement property : userPropertySeeks.keySet()) {
                    if(property.getApplyObject()== group) {
                        propertySeeks.put(property, session.getObjectValue(userPropertySeeks.get(property),property.property.getType()));
                        currentObject = null;
                        updateKeys = true;
                        direction = DIRECTION_CENTER;
                    }
                }
                for(ObjectImplement object : userObjectSeeks.keySet()) {
                    if(object.groupTo == group) {
                        DataObject objectValue = session.getDataObject(userObjectSeeks.get(object),object.getBaseClass().getType());
                        objectSeeks.put(object, objectValue);
                        currentObject.put(object, objectValue);
                        updateKeys = true;
                        direction = DIRECTION_CENTER;
                    }
                }

                if(!updateKeys && (group.updated & GroupObjectImplement.UPDATED_CLASSVIEW) !=0) {
                   // изменился "классовый" вид перечитываем св-ва
                    objectSeeks = group.getGroupObjectValue();
                    updateKeys = true;
                    direction = DIRECTION_CENTER;
                }

                if(!updateKeys && group.gridClassView && (group.updated & GroupObjectImplement.UPDATED_OBJECT)!=0) {
                    // листание - объекты стали близки к краю (object не далеко от края - надо хранить список не базу же дергать) - изменился объект
                    int keyNum = group.keys.indexOf(group.getGroupObjectValue());
                    // если меньше PageSize осталось и сверху есть ключи
                    if(keyNum< group.pageSize && group.upKeys) {
                        direction = DIRECTION_UP;
                        updateKeys = true;

                        int lowestInd = group.pageSize *2-1;
                        if (lowestInd >= group.keys.size()) {
                            objectSeeks = new HashMap<ObjectImplement, DataObject>();
                            hasMoreKeys = false;
                        } else {
                            objectSeeks = group.keys.get(lowestInd);
                            propertySeeks = group.keyOrders.get(objectSeeks);
                        }

                    } else {
                        // наоборот вниз
                        if(keyNum>= group.keys.size()- group.pageSize && group.downKeys) {
                            direction = DIRECTION_DOWN;
                            updateKeys = true;

                            int highestInd = group.keys.size()- group.pageSize *2;
                            if (highestInd < 0) {
                                objectSeeks = new HashMap<ObjectImplement, DataObject>();
                                hasMoreKeys = false;
                            } else {
                                objectSeeks = group.keys.get(highestInd);
                                propertySeeks = group.keyOrders.get(objectSeeks);
                            }
                        }
                    }
                }

                if(updateKeys) {
                    // --- перечитываем источник (если "классовый" вид - 50, + помечаем изменения GridObjects, иначе TOP 1

                    // проверим на интегральные классы в Group'e
/*                    for(ObjectImplement object : group)
                        if(objectSeeks.get(object)==null && object.baseClass instanceof DataClass && !group.gridClassView)
                            objectSeeks.put(object,new DataObject(((DataClass)object.baseClass).getDefaultValue(),object.baseClass));*/

                    // докидываем Join'ами (INNER) фильтры, порядки

                    // уберем все некорректности в Seekах :
                    // корректно если : PropertySeeks = Orders или (Orders.sublist(PropertySeeks.size) = PropertySeeks и ObjectSeeks - пустое)
                    // если Orders.sublist(PropertySeeks.size) != PropertySeeks, тогда дочитываем ObjectSeeks полностью
                    // выкидываем лишние PropertySeeks, дочитываем недостающие Orders в PropertySeeks
                    // также если панель то тупо прочитаем объект
                    boolean notEnoughOrders = !(propertySeeks.keySet().equals(group.orders.keySet()) || ((propertySeeks.size()< group.orders.size() && (
                            new HashSet<PropertyObjectImplement>((new ArrayList<PropertyObjectImplement>(group.orders.keySet())).subList(0, propertySeeks.size())))
                            .equals(propertySeeks.keySet())) && objectSeeks.size()==0));
                    boolean objectFound = true;
                    if((notEnoughOrders && objectSeeks.size()< group.size()) || !group.gridClassView) {
                        // дочитываем ObjectSeeks то есть на = PropertySeeks, ObjectSeeks
                        JoinQuery<ObjectImplement,Object> selectKeys = new JoinQuery<ObjectImplement,Object>(group);
                        selectKeys.putKeyWhere(objectSeeks);
                        group.fillSourceSelect(selectKeys, group.getClassGroup(), session.changes, getDefaultProperties(), getNoUpdateProperties());
                        for(Entry<PropertyObjectImplement, ObjectValue> property : propertySeeks.entrySet())
                            selectKeys.and(property.getKey().getSourceExpr(group.getClassGroup(),selectKeys.mapKeys, session.changes, getDefaultProperties(), getNoUpdateProperties())
                                    .compare(property.getValue().getExpr(), Compare.EQUALS));

                        // докидываем найденные ключи
                        LinkedHashMap<Map<ObjectImplement,DataObject>,Map<Object, ObjectValue>> resultKeys = selectKeys.executeSelectClasses(session, BL.baseClass);
                        if(resultKeys.size()>0)
                            for(ObjectImplement objectKey : group)
                                objectSeeks.put(objectKey,resultKeys.keySet().iterator().next().get(objectKey));
                        else
                            objectFound = false;
                    }

                    if(!group.gridClassView) {

                        // если не нашли объект, то придется искать
                        if (!objectFound) {
                            JoinQuery<ObjectImplement,Object> selectKeys = new JoinQuery<ObjectImplement,Object>(group);
                            group.fillSourceSelect(selectKeys, group.getClassGroup(), session.changes, getDefaultProperties(), getNoUpdateProperties());
                            LinkedHashMap<Map<ObjectImplement,DataObject>,Map<Object, ObjectValue>> resultKeys = selectKeys.executeSelectClasses(session, new LinkedHashMap<Object, Boolean>(), 1, BL.baseClass);
                            if(resultKeys.size()>0)
                                for(ObjectImplement objectKey : group)
                                    objectSeeks.put(objectKey,resultKeys.keySet().iterator().next().get(objectKey));
                        }

                        // если панель и ObjectSeeks "полный", то просто меняем объект и ничего не читаем
                        result.objects.put(group, objectSeeks);
                        changeGroupObject(group, objectSeeks);

                    } else {
                        // выкидываем Property которых нет, дочитываем недостающие Orders, по ObjectSeeks то есть не в привязке к отбору
                        if(notEnoughOrders && objectSeeks.size()== group.size() && group.orders.size() > 0) {
                            JoinQuery<ObjectImplement, PropertyObjectImplement> orderQuery = new JoinQuery<ObjectImplement, PropertyObjectImplement>(ObjectImplement.getMapKeys(objectSeeks.keySet()));
                            orderQuery.putKeyWhere(objectSeeks);

                            for(PropertyObjectImplement order : group.orders.keySet())
                                orderQuery.properties.put(order, order.getSourceExpr(group.getClassGroup(),orderQuery.mapKeys, session.changes, getDefaultProperties(), getNoUpdateProperties()));

                            LinkedHashMap<Map<ObjectImplement,DataObject>,Map<PropertyObjectImplement, ObjectValue>> resultOrders = orderQuery.executeSelectClasses(session, BL.baseClass);
                            for(PropertyObjectImplement order : group.orders.keySet())
                                propertySeeks.put(order,resultOrders.values().iterator().next().get(order));
                        }

                        LinkedHashMap<Object,Boolean> selectOrders = new LinkedHashMap<Object, Boolean>();
                        JoinQuery<ObjectImplement,Object> selectKeys = new JoinQuery<ObjectImplement,Object>(group); // object потому как нужно еще по ключам упорядочивать, а их тогда надо в св-ва кидать
                        group.fillSourceSelect(selectKeys, group.getClassGroup(), session.changes, getDefaultProperties(), getNoUpdateProperties());

                        // складываются источники и значения
                        List<SourceExpr> orderSources = new ArrayList<SourceExpr>();
                        List<ObjectValue> orderWheres = new ArrayList<ObjectValue>();
                        List<Boolean> orderDirs = new ArrayList<Boolean>();

                        // закинем порядки (с LEFT JOIN'ом)
                        for(Entry<PropertyObjectImplement,Boolean> toOrder : group.orders.entrySet()) {
                            SourceExpr orderExpr = toOrder.getKey().getSourceExpr(group.getClassGroup(), selectKeys.mapKeys, session.changes, getDefaultProperties(), getNoUpdateProperties());
                            // надо закинуть их в запрос, а также установить фильтры на порядки чтобы
                            if(propertySeeks.containsKey(toOrder.getKey())) {
                                orderSources.add(orderExpr);
                                orderWheres.add(propertySeeks.get(toOrder.getKey()));
                                orderDirs.add(toOrder.getValue());
                            } else //здесь надо что-то волшебное написать, чтобы null не было
                                selectKeys.and(orderExpr.getWhere());
                            // также надо кинуть в запрос ключи порядков, чтобы потом скроллить
                            selectKeys.properties.put(toOrder.getKey(), orderExpr);
                            selectOrders.put(toOrder.getKey(),toOrder.getValue());
                        }

                        // докинем в ObjectSeeks недостающие группы
                        for(ObjectImplement objectKey : group)
                            if(!objectSeeks.containsKey(objectKey))
                                objectSeeks.put(objectKey,null);

                        // закинем объекты в порядок
                        for(Entry<ObjectImplement, DataObject> objectSeek : objectSeeks.entrySet()) {
                            // также закинем их в порядок и в запрос6
                            SourceExpr keyExpr = selectKeys.mapKeys.get(objectSeek.getKey());
                            selectKeys.properties.put(objectSeek.getKey(),keyExpr); // чтобы упорядочивать
                            selectOrders.put(objectSeek.getKey(),false);
                            if(objectSeek.getValue()!=null) {
                                orderSources.add(keyExpr);
                                orderWheres.add(objectSeek.getValue());
                                orderDirs.add(false);
                            }
                        }

                        // выполняем запрос
                        // какой ряд выбранным будем считать
                        int activeRow = -1;
                        // результат
                        LinkedHashMap<Map<ObjectImplement,DataObject>,Map<Object,ObjectValue>> keyResult = new LinkedHashMap<Map<ObjectImplement, DataObject>, Map<Object, ObjectValue>>();

                        int readSize = group.pageSize *3/(direction ==DIRECTION_CENTER?2:1);

                        // сгенирируем orderWhere
                        Where orderWhere = Where.TRUE;
                        for(int i=orderSources.size()-1;i>=0;i--) {
                            ObjectValue orderValue = orderWheres.get(i);
                            if(orderValue instanceof DataObject)
                                orderWhere = orderSources.get(i).compare(orderValue.getExpr(), orderDirs.get(i)? Compare.LESS:Compare.GREATER)
                                    .or(orderSources.get(i).compare(orderValue.getExpr(),Compare.EQUALS).and(orderWhere));
                        }

                        // откопируем в сторону запрос чтобы еще раз потом использовать
                        JoinQuery<ObjectImplement,Object> copySelect = null;
                        if(direction ==DIRECTION_CENTER)
                            copySelect = new JoinQuery<ObjectImplement, Object>(selectKeys, false);
                        // сначала Descending загоним
                        group.downKeys = false;
                        group.upKeys = false;
                        if(direction ==DIRECTION_UP || direction ==DIRECTION_CENTER) {
                            if(orderSources.size()>0) {
                                selectKeys.and(orderWhere.not());
                                group.downKeys = hasMoreKeys;
                            }

//                            System.out.println(group + " KEYS UP ");
//                            selectKeys.outSelect(session,JoinQuery.reverseOrder(selectOrders),readSize);
                            LinkedHashMap<Map<ObjectImplement, DataObject>, Map<Object, ObjectValue>> execResult = selectKeys.executeSelectClasses(session, JoinQuery.reverseOrder(selectOrders), readSize, BL.baseClass);
                            ListIterator<Map<ObjectImplement,DataObject>> ik = (new ArrayList<Map<ObjectImplement,DataObject>>(execResult.keySet())).listIterator();
                            while(ik.hasNext()) ik.next();
                            while(ik.hasPrevious()) {
                                Map<ObjectImplement,DataObject> row = ik.previous();
                                keyResult.put(row,execResult.get(row));
                            }
                            group.upKeys = (keyResult.size()== readSize);

                            // проверка чтобы не сбить объект при листании и неправильная (потому как после 2 поиска может получится что надо с 0 без Seek'а перечитывать)
    //                        if(OrderSources.size()==0)
                            // сделано так, чтобы при ненайденном объекте текущий объект смещался вверх, а не вниз
                            activeRow = keyResult.size()-1;
                        }
                        if(direction ==DIRECTION_CENTER) selectKeys = copySelect;
                        // потом Ascending
                        if(direction ==DIRECTION_DOWN || direction ==DIRECTION_CENTER) {
                            if(orderSources.size()>0) {
                                selectKeys.and(orderWhere);
                                if(direction !=DIRECTION_CENTER) group.upKeys = hasMoreKeys;
                            }

//                            System.out.println(group + " KEYS DOWN ");
//                            selectKeys.outSelect(session,selectOrders,readSize);
                            LinkedHashMap<Map<ObjectImplement,DataObject>,Map<Object,ObjectValue>> executeList = selectKeys.executeSelectClasses(session, selectOrders, readSize, BL.baseClass);
    //                        if((OrderSources.size()==0 || Direction==2) && ExecuteList.size()>0) ActiveRow = KeyResult.size();
                            keyResult.putAll(executeList);
                            group.downKeys = (executeList.size()== readSize);

                            if ((direction == DIRECTION_DOWN || activeRow == -1) && keyResult.size() > 0)
                                activeRow = 0;
                        }

                        group.keys = new ArrayList<Map<ObjectImplement, DataObject>>();
                        group.keyOrders = new HashMap<Map<ObjectImplement, DataObject>, Map<PropertyObjectImplement, ObjectValue>>();

                        // параллельно будем обновлять ключи чтобы Join'ить
                        ViewTable insertTable = groupTables.get(group);
                        if(insertTable==null) {
                            insertTable = new ViewTable(group, sessionID * RemoteFormInterface.GID_SHIFT + group.ID);
                            session.createTemporaryTable(insertTable);
                        }

                        List<Map<KeyField,DataObject>> viewKeys = new ArrayList<Map<KeyField, DataObject>>();
                        for(Entry<Map<ObjectImplement, DataObject>, Map<Object, ObjectValue>> resultRow : keyResult.entrySet()) {
                            viewKeys.add(BaseUtils.join(insertTable.mapKeys,resultRow.getKey()));

                            Map<ObjectImplement,DataObject> keyRow = new HashMap<ObjectImplement,DataObject>(resultRow.getKey());
                            group.keys.add(keyRow);
                            group.keyOrders.put(keyRow, BaseUtils.filterKeys(resultRow.getValue(),group.orders.keySet()));
                        }

                        groupTables.put(group, insertTable.writeKeys(session, viewKeys));

                        result.gridObjects.put(group, group.keys);

                        group.updated = (group.updated | GroupObjectImplement.UPDATED_KEYS);

                        // если ряд никто не подставил и ключи есть пробуем старый найти
    //                    if(ActiveRow<0 && Group.Keys.size()>0)
    //                        ActiveRow = Group.Keys.indexOf(Group.GetObjectValue());

                        // если есть в новых ключах старый ключ, то делаем его активным
                        if (group.keys.contains(currentObject))
                            activeRow = group.keys.indexOf(currentObject);

                        if(activeRow >=0 && activeRow < group.keys.size()) {
                            // нашли ряд его выбираем
                            Map<ObjectImplement,DataObject> newValue = group.keys.get(activeRow);
    //                        if (!newValue.equals(Group.GetObjectValue())) {
                                result.objects.put(group,newValue);
                                changeGroupObject(group,newValue);
    //                        }
                        } else
                            changeGroupObject(group,new HashMap<ObjectImplement,DataObject>());
                    }
                }
            }

            Collection<PropertyView> panelProps = new ArrayList<PropertyView>();
            Map<GroupObjectImplement,Collection<PropertyView>> groupProps = new HashMap<GroupObjectImplement, Collection<PropertyView>>();

//        PanelProps.

            for(PropertyView<?> drawProp : properties) {

                // три состояния св-ва : 0 - не в интерфейсе, 1 - в интерфейсе объектов, 2 - в интерфейсе классов (toDraw)
                boolean read = false;
                boolean checkClass = false;
                boolean checkObject = false;
                int inInterface = 0;

                if(drawProp.toDraw !=null) {
                    // если рисуемся в какой-то вид и обновился источник насильно перечитываем все св-ва
                    read = ((drawProp.toDraw.updated & (GroupObjectImplement.UPDATED_KEYS | GroupObjectImplement.UPDATED_CLASSVIEW))!=0);

                    Boolean prevPool = interfacePool.get(drawProp);
                    inInterface = (prevPool==null?0:(prevPool?2:1));
                }

                for(ObjectImplement object : drawProp.view.mapping.values())  {
                    if(object.groupTo != drawProp.toDraw) {
                        // "верхние" объекты интересует только изменение объектов\классов
                        if(object.objectUpdated()) {
                            // изменился верхний объект, перечитываем
                            read = true;
                            if(object.classUpdated()) {
                                // изменился класс объекта перепроверяем все
                                if(drawProp.toDraw !=null) checkClass = true;
                                checkObject = true;
                            }
                        }
                    } else {
                        // изменился объект и св-во не было классовым
                        if(object.objectUpdated() && !(inInterface ==2 && drawProp.toDraw.gridClassView)) {
                            read = true;
                            // изменися класс объекта
                            if((object.classUpdated())) checkObject = true;
                        }
                        // изменение общего класса
                        if((object.groupTo.updated & ObjectImplement.UPDATED_GRIDCLASS)!=0) checkClass = true;
                    }
                }

                // обновим InterfacePool, было в InInterface
                if(checkClass || checkObject) {
                    int newInInterface;
                    if(checkClass) { // если классы изменились перечитываем
                        assert drawProp.toDraw!=null;
                        newInInterface = (drawProp.view.isInInterface(drawProp.toDraw)?2:0);
                    } else // иначе берем какой был
                        newInInterface = inInterface;

                    if(newInInterface!=2 && (checkObject || checkClass)) // если не классовый и объект изменился
                        newInInterface = (drawProp.view.isInInterface(null)?1:0);

                    if(inInterface!=newInInterface) {
                        inInterface = newInInterface;

                        if(inInterface==0) {
                            interfacePool.remove(drawProp);
                            // !!! СЮДА НАДО ВКИНУТЬ УДАЛЕНИЕ ИЗ ИНТЕРФЕЙСА
                            result.dropProperties.add(drawProp);
                        }
                        else
                            interfacePool.put(drawProp,inInterface==2);
                    }
                }

                if(!read && (dataChanged && changedProps.contains(drawProp.view.property)))
                    read = true;

/*                if (!read && dataChanged) {
                    for (ObjectImplement object : drawProp.view.mapping.values()) {
                        if (object.classChanged(changedClasses)) {
                            read = true;
                            break;
                        }
                    }
                }*/

                assert (inInterface==2)==(drawProp.toDraw!=null && drawProp.view.isInInterface(drawProp.toDraw));
                assert !(drawProp.view.isInInterface(null) && inInterface==0);

                if(inInterface>0 && read) {
                    if(inInterface==2 && drawProp.toDraw.gridClassView) {
                        Collection<PropertyView> propList = groupProps.get(drawProp.toDraw);
                        if(propList==null) {
                            propList = new ArrayList<PropertyView>();
                            groupProps.put(drawProp.toDraw,propList);
                        }
                        propList.add(drawProp);
                    } else
                        panelProps.add(drawProp);
                }
            }

            // погнали выполнять все собранные запросы и FormChanges

            // сначала PanelProps
            if(panelProps.size()>0) {
                JoinQuery<Object, PropertyView> selectProps = new JoinQuery<Object, PropertyView>(new HashMap<Object, KeyExpr>());
                for(PropertyView drawProp : panelProps)
                    selectProps.properties.put(drawProp, drawProp.view.getSourceExpr(null,null, session.changes, getDefaultProperties(), getNoUpdateProperties()));

                Map<PropertyView,Object> resultProps = selectProps.executeSelect(session).values().iterator().next();
                for(PropertyView drawProp : panelProps)
                    result.panelProperties.put(drawProp,resultProps.get(drawProp));
            }

            for(Entry<GroupObjectImplement, Collection<PropertyView>> mapGroup : groupProps.entrySet()) {
                GroupObjectImplement group = mapGroup.getKey();
                Collection<PropertyView> groupList = mapGroup.getValue();

                JoinQuery<ObjectImplement, PropertyView> selectProps = new JoinQuery<ObjectImplement, PropertyView>(group);

                ViewTable keyTable = groupTables.get(mapGroup.getKey()); // ставим фильтр на то что только из viewTable'а
                selectProps.and(keyTable.joinAnd(BaseUtils.join(keyTable.mapKeys,selectProps.mapKeys)).getWhere());

                for(PropertyView drawProp : groupList)
                    selectProps.properties.put(drawProp, drawProp.view.getSourceExpr(group.getClassGroup(), selectProps.mapKeys, session.changes, getDefaultProperties(), getNoUpdateProperties()));

                LinkedHashMap<Map<ObjectImplement, Object>, Map<PropertyView, Object>> resultProps = selectProps.executeSelect(session);

                for(PropertyView drawProp : groupList) {
                    Map<Map<ObjectImplement,DataObject>,Object> propResult = new HashMap<Map<ObjectImplement,DataObject>, Object>();
                    for(Entry<Map<ObjectImplement, Object>, Map<PropertyView, Object>> resultRow : resultProps.entrySet())
                        propResult.put(group.findGroupObjectValue(resultRow.getKey()),resultRow.getValue().get(drawProp));
                    result.gridProperties.put(drawProp,propResult);
                }
            }

            userPropertySeeks.clear();
            userObjectSeeks.clear();

            pendingGroupChanges.clear();

            // сбрасываем все пометки
            structUpdated = false;
            for(GroupObjectImplement group : groups) {
                for(ObjectImplement object : group)
                    object.updated = 0;
                group.updated = 0;
            }
            dataChanged = false;

//        result.out(this);

            return result;
//        } catch (RuntimeException e) {
//            transaction.rollback();
//            throw e;
        } catch (SQLException e) {
            transaction.rollback();
            throw e;
        }
    }

    // возвращает какие объекты отчета фиксируются
    private Set<GroupObjectImplement> getClassGroups() {

        Set<GroupObjectImplement> reportObjects = new HashSet<GroupObjectImplement>();
        for (GroupObjectImplement group : groups) {
            if (group.gridClassView)
                reportObjects.add(group);
        }

        return reportObjects;
    }

    // считывает все данные (для отчета)
    public FormData getFormData() throws SQLException {

        // вызовем endApply, чтобы быть полностью уверенным в том, что мы работаем с последними данными
        endApply();

        Set<GroupObjectImplement> classGroups = getClassGroups();

        Collection<ObjectImplement> readObjects = new ArrayList<ObjectImplement>();
        for(GroupObjectImplement group : classGroups)
            readObjects.addAll(group);

        // пока сделаем тупо получаем один большой запрос

        JoinQuery<ObjectImplement,Object> query = new JoinQuery<ObjectImplement,Object>(ObjectImplement.getMapKeys(readObjects));
        LinkedHashMap<Object,Boolean> queryOrders = new LinkedHashMap<Object, Boolean>();

        for (GroupObjectImplement group : groups) {

            if (classGroups.contains(group)) {

                // не фиксированные ключи
                group.fillSourceSelect(query,classGroups, session.changes, getDefaultProperties(), getNoUpdateProperties());

                // закинем Order'ы
                for(Map.Entry<PropertyObjectImplement,Boolean> order : group.orders.entrySet()) {
                    query.properties.put(order.getKey(),order.getKey().getSourceExpr(classGroups, query.mapKeys, session.changes, getDefaultProperties(), getNoUpdateProperties()));
                    queryOrders.put(order.getKey(),order.getValue());
                }

                for(ObjectImplement object : group) {
                    query.properties.put(object,object.getSourceExpr(classGroups,query.mapKeys));
                    queryOrders.put(object,false);
                }
            }
        }

        FormData result = new FormData();

        for(PropertyView property : properties)
            query.properties.put(property, property.view.getSourceExpr(classGroups, query.mapKeys, session.changes, getDefaultProperties(), getNoUpdateProperties()));

        LinkedHashMap<Map<ObjectImplement, Object>, Map<Object, Object>> resultSelect = query.executeSelect(session,queryOrders,0);
        for(Entry<Map<ObjectImplement, Object>, Map<Object, Object>> row : resultSelect.entrySet()) {
            Map<ObjectImplement,Object> groupValue = new HashMap<ObjectImplement, Object>();
            for(GroupObjectImplement group : groups)
                for(ObjectImplement object : group)
                    if (readObjects.contains(object))
                        groupValue.put(object,row.getKey().get(object));
                    else
                        groupValue.put(object,object.getValue().object);

            Map<PropertyView,Object> propertyValues = new HashMap<PropertyView, Object>();
            for(PropertyView property : properties)
                propertyValues.put(property,row.getValue().get(property));

            result.add(groupValue,propertyValues);
        }

        return result;
    }

}

