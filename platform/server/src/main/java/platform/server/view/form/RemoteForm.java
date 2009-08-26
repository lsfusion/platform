/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platform.server.view.form;

import platform.base.BaseUtils;
import platform.interop.Compare;
import platform.interop.Scroll;
import platform.interop.form.RemoteFormInterface;
import platform.server.auth.SecurityPolicy;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.types.Type;
import platform.server.data.classes.ConcreteCustomClass;
import platform.server.data.classes.CustomClass;
import platform.server.data.classes.DataClass;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.cases.CaseExpr;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.data.IDTable;
import platform.server.logics.properties.*;
import platform.server.session.*;
import platform.server.view.navigator.*;
import platform.server.view.navigator.filter.FilterNavigator;
import platform.server.view.form.filter.Filter;
import platform.server.view.form.filter.CompareFilter;
import platform.server.where.Where;
import platform.server.where.WhereBuilder;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

// класс в котором лежит какие изменения произошли

// нужен какой-то объект который разделит клиента и серверную часть кинув каждому свои данные
// так клиента волнуют панели на форме, список гридов в привязке, дизайн и порядок представлений
// сервера колышет дерево и св-ва предст. с привязкой к объектам

public class RemoteForm<T extends BusinessLogics<T>> implements Property.TableDepends<RemoteForm.UsedChanges> {

    // используется для записи в сессии изменений в базу - требуется глобально уникальный идентификатор
    public final int GID;

    public final int sessionID;

    public final int ID;

    T BL;

    public DataSession session;

    SecurityPolicy securityPolicy;

    public CustomClass getCustomClass(int classID) {
        return BL.baseClass.findClassID(classID);
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

            Collection<ObjectImplement> objects = new ArrayList<ObjectImplement>();
            for (ObjectNavigator object : groupKey)
                objects.add(objectMapper.doMapping(object,classView));

            GroupObjectImplement groupValue = new GroupObjectImplement(groupKey.ID,objects,order,
                    groupKey.pageSize,groupKey.gridClassView,groupKey.singleViewType);

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

    final FocusView<T> focusView;

    public RemoteForm(int iID, T iBL, DataSession iSession, SecurityPolicy isecurityPolicy, NavigatorForm<?> navigatorForm, CustomClassView classView, FocusView<T> iFocusView) throws SQLException {

        ID = iID;
        BL = iBL;
        session = iSession;
        securityPolicy = isecurityPolicy;

        focusView = iFocusView;

        GID = IDTable.instance.generateID(session, IDTable.FORM);
        sessionID = session.generateSessionID(ID);

        final ObjectImplementMapper objectMapper = new ObjectImplementMapper();
        GroupObjectImplementMapper groupObjectMapper = new GroupObjectImplementMapper(objectMapper);
        final PropertyObjectImplementMapper propertyMapper = new PropertyObjectImplementMapper(objectMapper);
        PropertyViewMapper propertyViewMapper = new PropertyViewMapper(propertyMapper, groupObjectMapper);

        for (int i=0;i<navigatorForm.groups.size();i++)
            groups.add(groupObjectMapper.doMapping(navigatorForm.groups.get(i),i,classView));

        for (PropertyViewNavigator navigatorProperty : navigatorForm.propertyViews)
            if (securityPolicy.property.view.checkPermission(navigatorProperty.view.property))
                properties.add(propertyViewMapper.doMapping(navigatorProperty));

        FilterNavigator.Mapper filterMapper = new FilterNavigator.Mapper() {
            public <P extends PropertyInterface> PropertyObjectImplement<P> mapProperty(PropertyObjectNavigator<P> navigator) {
                return propertyMapper.doMapping(navigator);
            }
            public ObjectImplement mapObject(ObjectNavigator navigator) {
                return objectMapper.mapper.get(navigator);
            }
            public ObjectValue mapValue(Object value, Type type) throws SQLException {
                return session.getObjectValue(value,type);
            }
        };

        for (FilterNavigator navigatorFilter : navigatorForm.fixedFilters) {
            Filter filter = navigatorFilter.doMapping(filterMapper);
            filter.getApplyObject().fixedFilters.add(filter);
        }

        for (RegularFilterGroupNavigator navigatorGroup : navigatorForm.regularFilterGroups) {

            RegularFilterGroup group = new RegularFilterGroup(navigatorGroup.ID);
            for (RegularFilterNavigator filter : navigatorGroup.filters)
                group.addFilter(new RegularFilter(filter.ID, filter.filter.doMapping(filterMapper), filter.name, filter.key));

            regularFilterGroups.add(group);
        }

        hintsNoUpdate = navigatorForm.hintsNoUpdate;
        hintsSave = navigatorForm.hintsSave;
    }

    public List<GroupObjectImplement> groups = new ArrayList<GroupObjectImplement>();
    public Map<GroupObjectImplement,ViewTable> groupTables = new HashMap<GroupObjectImplement, ViewTable>(); 
    // собсно этот объект порядок колышет столько же сколько и дизайн представлений
    public List<PropertyView> properties = new ArrayList<PropertyView>();

    // ----------------------------------- Поиск объектов по ID ------------------------------ //

    public GroupObjectImplement getGroupObjectImplement(int groupID) {
        for (GroupObjectImplement groupObject : groups)
            if (groupObject.ID == groupID)
                return groupObject;
        return null;
    }

    public ObjectImplement getObjectImplement(int objectID) {
        for (GroupObjectImplement groupObject : groups)
            for (ObjectImplement object : groupObject.objects)
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
            change.serializeChange(outStream, session, propertyView.view.getInterfaceValues(), this);
    }

    // ----------------------------------- Навигация ----------------------------------------- //

    // поиски по свойствам\объектам
    // отличается когда содержит null (то есть end делается) и не содержит элемента
    public Map<GroupObjectImplement,Map<OrderView,Object>> userGroupSeeks = new HashMap<GroupObjectImplement, Map<OrderView, Object>>();

    public void changeGroupObject(GroupObjectImplement group, Scroll changeType) throws SQLException {
        switch(changeType) {
            case HOME:
                userGroupSeeks.put(group,new HashMap<OrderView, Object>());
                break;
            case END:
                userGroupSeeks.put(group,null);
                break;
        }
    }

    public void changeGroupObject(GroupObjectImplement group,Map<ObjectImplement,? extends ObjectValue> value) throws SQLException {
        // проставим все объектам метки изменений
        for(ObjectImplement object : group.objects)
            object.changeValue(session, value.get(object));
    }

    public void switchClassView(GroupObjectImplement group) {
        changeClassView(group, !group.gridClassView);
    }

    private void changeClassView(GroupObjectImplement group,boolean show) {

        if(group.gridClassView == show || group.singleViewType) return;
        group.gridClassView = show;

        group.updated = group.updated | GroupObjectImplement.UPDATED_CLASSVIEW;

    }

    // сстандартные фильтры
    public List<RegularFilterGroup> regularFilterGroups = new ArrayList<RegularFilterGroup>();
    private Map<RegularFilterGroup, RegularFilter> regularFilterValues = new HashMap<RegularFilterGroup, RegularFilter>();
    public void setRegularFilter(RegularFilterGroup filterGroup, RegularFilter filter) {

        RegularFilter prevFilter = regularFilterValues.get(filterGroup);
        prevFilter.filter.getApplyObject().removeRegularFilter(prevFilter.filter);

        if (filter == null || filter.filter == null)
            regularFilterValues.remove(filterGroup);
        else {
            regularFilterValues.put(filterGroup, filter);
            filter.filter.getApplyObject().addRegularFilter(filter.filter);
        }

    }

    // Порядки

    private Map<GroupObjectImplement,LinkedHashMap<OrderView,Boolean>> groupOrders = new HashMap<GroupObjectImplement,LinkedHashMap<OrderView, Boolean>>();

    // -------------------------------------- Изменение данных ----------------------------------- //

    // пометка что изменились данные
    private boolean dataChanged = false;

    public void addObject(CustomObjectImplement object, ConcreteCustomClass cls) throws SQLException {
        // пока тупо в базу

        if (!securityPolicy.cls.edit.add.checkPermission(cls)) return;

        DataObject addObject = session.addObject(cls);

        boolean foundConflict = false;

        // берем все текущие CompareFilter на оператор 0(=) делаем ChangeProperty на Value сразу в сессию
        // тогда добавляет для всех других объектов из того же GroupObjectImplement'а, значение Value, GetValueExpr
        for(Filter filter : object.groupTo.filters) {
            if(filter instanceof CompareFilter && (!Filter.ignoreInInterface || filter.isInInterface(object.groupTo))) { // если ignoreInInterface проверить что в интерфейсе 
                CompareFilter<?> compareFilter = (CompareFilter)filter;
                if(compareFilter.compare==Compare.EQUALS && compareFilter.property.property instanceof DataProperty) {
                    JoinQuery<ObjectImplement,String> subQuery = new JoinQuery<ObjectImplement,String>(ObjectImplement.getMapKeys(compareFilter.property.mapping.values()));
                    Map<ObjectImplement,DataObject> fixedObjects = new HashMap<ObjectImplement, DataObject>();
                    for(ObjectImplement mapObject : compareFilter.property.mapping.values()) {
                        ObjectImplement sibObject = (CustomObjectImplement) mapObject;
                        if(sibObject.groupTo !=object.groupTo)
                            fixedObjects.put(sibObject,sibObject.getDataObject());
                        else
                            if(sibObject!=object)
                                subQuery.and(subQuery.mapKeys.get(sibObject).getIsClassWhere(sibObject.getGridClass().getUpSet()));
                            else
                                fixedObjects.put(sibObject,addObject);
                    }

                    subQuery.putKeyWhere(fixedObjects);

                    subQuery.properties.put("newvalue", compareFilter.value.getSourceExpr(object.groupTo.getClassGroup(),subQuery.mapKeys, session.changes, this));

                    LinkedHashMap<Map<ObjectImplement, DataObject>, Map<String, ObjectValue>> result = subQuery.executeSelectClasses(session,BL.baseClass);
                    // изменяем св-ва
                    for(Entry<Map<ObjectImplement, DataObject>, Map<String, ObjectValue>> row : result.entrySet()) {
                        DataProperty changeProperty = (DataProperty) compareFilter.property.property;
                        Map<DataPropertyInterface,DataObject> keys = new HashMap<DataPropertyInterface, DataObject>();
                        for(DataPropertyInterface propertyInterface : changeProperty.interfaces) {
                            CustomObjectImplement changeObject = (CustomObjectImplement) compareFilter.property.mapping.get(propertyInterface);
                            keys.put(propertyInterface,row.getKey().get(changeObject));
                        }
                        session.changeProperty(changeProperty,keys,row.getValue().get("newvalue"),false);
                    }
                } else {
                    if (object.groupTo.equals(compareFilter.getApplyObject()))
                        foundConflict = true;
                }
            }
        }

//        по идее не над так как в order'ах будут null'ы
//        foundConflict = foundConflict || !object.groupTo.orders.isEmpty();

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
        for(GroupObjectImplement group : groups)
            group.fillUpdateProperties(result);
        return result;
    }

    public Collection<Property> hintsNoUpdate = new HashSet<Property>();
    public Collection<Property> getNoUpdateProperties() {
        return hintsNoUpdate;
    }

    static class UsedChanges extends Property.TableUsedChanges<UsedChanges> {
        final Collection<Property> noUpdateProps = new ArrayList<Property>();

        @Override
        public void add(UsedChanges add, DataProperty exclude) {
            super.add(add, exclude);
            noUpdateProps.addAll(add.noUpdateProps);
        }

        @Override
        public boolean equals(Object o) {
            return this==o || o instanceof UsedChanges && noUpdateProps.equals(((UsedChanges)o).noUpdateProps) && super.equals(o);
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + noUpdateProps.hashCode();
        }
    }

    public <P extends PropertyInterface> SourceExpr changed(Property<P> property, Map<P, ? extends SourceExpr> joinImplement, WhereBuilder changedWhere) {
        if(hintsNoUpdate.contains(property)) // если так то ничего не менять 
            return CaseExpr.NULL;
        else
            return null;
    }

    public UsedChanges used(Property property, UsedChanges changes) {
        if(hintsNoUpdate.contains(property)) {
            changes = new UsedChanges();
            changes.noUpdateProps.add(property);
        }
        return changes;
    }

    public UsedChanges newChanges() {
        return new UsedChanges();
    }

    public static class UpdateChanges extends Property.UsedChanges<ViewDataChanges,UpdateChanges> {
        public ViewDataChanges newChanges() {
            return new ViewDataChanges();
        }
    }
    public Property.Depends<ViewDataChanges,UpdateChanges> updateDepends = new Property.Depends<ViewDataChanges,UpdateChanges>() {
            public UpdateChanges used(Property property, UpdateChanges usedChanges) {
                if(hintsNoUpdate.contains(property))
                    return new UpdateChanges();
                return usedChanges;
            }

            public UpdateChanges newChanges() {
                return new UpdateChanges();
            }
        };

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
            LinkedHashMap<OrderView,Boolean> orders;
            boolean upKeys,downKeys;
            List<Map<ObjectImplement,DataObject>> keys;
            // какие ключи активны
            Map<Map<ObjectImplement,DataObject>,Map<OrderView,ObjectValue>> keyOrders;
            int updated;

            Collection<Object> objects = new ArrayList<Object>();

            ViewTable viewTable;

            private Group(GroupObjectImplement iGroup) {
                group = iGroup;

                filters = new HashSet<Filter>(group.filters);
                orders = new LinkedHashMap<OrderView, Boolean>(group.orders);
                upKeys = group.upKeys;
                downKeys = group.downKeys;
                keys = group.keys;
                keyOrders = group.keyOrders;
                updated = group.updated;

                for(ObjectImplement object : group.objects)
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
        Map<PropertyView,Boolean> cacheInGridInterface;
        Map<PropertyView,Boolean> cacheInInterface;
        Set<PropertyView> isDrawed; 

        Map<RemoteForm, ViewDataChanges> incrementChanges;

        TableChanges changes;

        ApplyTransaction() {
            for(GroupObjectImplement group : RemoteForm.this.groups)
                groups.add(new Group(group));
            cacheInGridInterface = new HashMap<PropertyView, Boolean>(RemoteForm.this.cacheInGridInterface);
            cacheInInterface = new HashMap<PropertyView, Boolean>(RemoteForm.this.cacheInInterface);
            isDrawed = new HashSet<PropertyView>(RemoteForm.this.isDrawed);

            if(dataChanged) {
                incrementChanges = new HashMap<RemoteForm, ViewDataChanges>(session.incrementChanges);
                changes = new TableChanges(session.changes);
            }
        }

        void rollback() throws SQLException {
            for(Group group : groups)
                group.rollback();
            RemoteForm.this.cacheInGridInterface = cacheInGridInterface;
            RemoteForm.this.cacheInInterface = cacheInInterface;
            RemoteForm.this.isDrawed = isDrawed;

            if(dataChanged) {
                session.incrementChanges = incrementChanges;
                session.changes = changes;
            }
        }
    }

    private final static int DIRECTION_DOWN = 1;
    private final static int DIRECTION_UP = 2;
    private final static int DIRECTION_CENTER = 3;

    // оболочка изменения group, чтобы отослать клиенту
    private void updateGroupObject(GroupObjectImplement group,FormChanges changes,Map<ObjectImplement,? extends ObjectValue> value) throws SQLException {
        changes.objects.put(group,value);
        changeGroupObject(group, value);
    }

    private LinkedHashMap<Map<ObjectImplement, DataObject>, Map<OrderView, ObjectValue>> executeSelectKeys(GroupObjectImplement group,Map<OrderView,ObjectValue> orderSeeks,int readSize,boolean down) throws SQLException {
        // assertion что group.orders начинается с orderSeeks
        LinkedHashMap<OrderView,Boolean> orders;
        if(orderSeeks!=null && readSize==1)
            orders = BaseUtils.moveStart(group.orders,orderSeeks.keySet());
        else
            orders = group.orders;

        assert !(orderSeeks!=null && !BaseUtils.starts(orders,orderSeeks.keySet()));

        JoinQuery<ObjectImplement,OrderView> selectKeys = new JoinQuery<ObjectImplement,OrderView>(group); // object потому как нужно еще по ключам упорядочивать, а их тогда надо в св-ва кидать
        group.fillSourceSelect(selectKeys, group.getClassGroup(), session.changes, this);

        Where orderWhere = orderSeeks==null?Where.FALSE:Where.TRUE;
        for(Entry<OrderView, Boolean> toOrder : BaseUtils.reverse(orders).entrySet()) {
            SourceExpr orderExpr = toOrder.getKey().getSourceExpr(group.getClassGroup(), selectKeys.mapKeys, session.changes, this);
            selectKeys.properties.put(toOrder.getKey(), orderExpr); // надо в запрос закинуть чтобы скроллить и упорядочивать

            if(orderSeeks!=null) {
                ObjectValue toSeek = orderSeeks.get(toOrder.getKey());
                if(toSeek!=null)
                    orderWhere = toSeek.order(orderExpr,toOrder.getValue(),orderWhere);
            }
        }
        selectKeys.and(down?orderWhere:orderWhere.not());
        return selectKeys.executeSelectClasses(session, down?orders:JoinQuery.reverseOrder(orders), readSize, BL.baseClass);
    }

    // считывает одну запись
    private Map.Entry<Map<ObjectImplement, DataObject>, Map<OrderView, ObjectValue>> readObjects(GroupObjectImplement group,Map<OrderView,ObjectValue> orderSeeks) throws SQLException {
        LinkedHashMap<Map<ObjectImplement, DataObject>, Map<OrderView, ObjectValue>> result = executeSelectKeys(group, orderSeeks, 1, true);
        if(result.size()==0)
            result = executeSelectKeys(group, orderSeeks, 1, false);
        if(result.size()>0)
            return BaseUtils.singleEntry(result);
        else
            return null;
    }
    private Map<ObjectImplement,? extends ObjectValue> readKeys(GroupObjectImplement group,Map<OrderView,ObjectValue> orderSeeks) throws SQLException {
        Map.Entry<Map<ObjectImplement, DataObject>, Map<OrderView, ObjectValue>> objects = readObjects(group, orderSeeks);
        if(objects!=null)
            return objects.getKey();
        else
            return group.getNulls();
    }
    private Map<OrderView,ObjectValue> readValues(GroupObjectImplement group,Map<OrderView,ObjectValue> orderSeeks) throws SQLException {
        Map.Entry<Map<ObjectImplement, DataObject>, Map<OrderView, ObjectValue>> objects = readObjects(group, orderSeeks);
        if(objects!=null)
            return objects.getValue();
        else
            return new HashMap<OrderView, ObjectValue>();
    }

    // "закэшированная" проверка присутствия в интерфейсе, отличается от кэша тем что по сути функция от mutable объекта
    protected Map<PropertyView,Boolean> cacheInGridInterface = new HashMap<PropertyView,Boolean>();
    protected Map<PropertyView,Boolean> cacheInInterface = new HashMap<PropertyView, Boolean>();
    protected Set<PropertyView> isDrawed = new HashSet<PropertyView>(); 

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

            for(GroupObjectImplement group : groups) {

                // если изменились класс грида или представление 
                boolean updateKeys = (group.updated & (GroupObjectImplement.UPDATED_GRIDCLASS | GroupObjectImplement.UPDATED_CLASSVIEW))!=0;

                if ((group.updated & GroupObjectImplement.UPDATED_CLASSVIEW) != 0)
                    result.classViews.put(group, group.gridClassView);

                if(Filter.ignoreInInterface) {
                    updateKeys |= (group.updated & GroupObjectImplement.UPDATED_FILTER)!=0;
                    group.filters = group.getSetFilters();
                } else
                    if((group.updated & GroupObjectImplement.UPDATED_FILTER)!=0) {
                        Set<Filter> newFilters = new HashSet<Filter>();
                        for(Filter filt : group.getSetFilters())
                            if(filt.isInInterface(group))
                                newFilters.add(filt);

                        updateKeys |= !newFilters.equals(group.filters);
                        group.filters = newFilters;
                    } else // остались те же setFilters
                        for(Filter filt : group.getSetFilters())
                            if(filt.classUpdated(group))
                                updateKeys |= (filt.isInInterface(group)? group.filters.add(filt): group.filters.remove(filt));

                // порядки
                LinkedHashMap<OrderView,Boolean> newOrders = new LinkedHashMap<OrderView, Boolean>();
                if((group.updated & GroupObjectImplement.UPDATED_ORDER)!=0) {
                    for(Entry<OrderView, Boolean> setOrder : group.getSetOrders().entrySet())
                        if(setOrder.getKey().isInInterface(group))
                            newOrders.put(setOrder.getKey(),setOrder.getValue());
                    updateKeys |= !BaseUtils.equalsLinked(group.orders,newOrders);
                } else { // значит setOrders не изменился
                    for(Entry<OrderView, Boolean> setOrder : group.getSetOrders().entrySet()) {
                        boolean isInInterface = group.orders.containsKey(setOrder.getKey());
                        if(setOrder.getKey().classUpdated(group) && !(setOrder.getKey().isInInterface(group)==isInInterface)) {
                            isInInterface = !isInInterface;
                            updateKeys = true;
                        }
                        if(isInInterface)
                            newOrders.put(setOrder.getKey(),setOrder.getValue());
                    }
                }
                group.orders = newOrders;

                if(!updateKeys) // изменились "верхние" объекты для фильтров
                    for(Filter filt : group.filters)
                        if(filt.objectUpdated(group)) {updateKeys = true; break;}
                if(!updateKeys) // изменились "верхние" объекты для порядков
                    for(OrderView order : group.orders.keySet())
                        if(order.objectUpdated(group)) { updateKeys = true; break;}
                if(!updateKeys) // изменились данные по фильтрам
                    for(Filter filt : group.filters)
                        if(filt.dataUpdated(changedProps)) {updateKeys = true; break;}
                if(!updateKeys) // изменились данные по порядкам
                    for(OrderView order : group.orders.keySet())
                        if(order.dataUpdated(changedProps)) {updateKeys = true; break;}
                if(!updateKeys) // классы удалились\добавились
                    for(ObjectImplement object : group.objects)
                        if(object.classChanged(changedClasses)) {updateKeys = true; break;}

                Map<ObjectImplement, ObjectValue> currentObject = group.getGroupObjectValue();
                Map<OrderView,ObjectValue> orderSeeks = null;

                int direction = DIRECTION_CENTER;
                boolean keepObject = true;

                if(updateKeys) // изменились фильтры, порядки, вид, ищем текущий объект
                    orderSeeks = new HashMap<OrderView, ObjectValue>(currentObject);

                if(!updateKeys && userGroupSeeks.containsKey(group)) { // пользовательский поиск
                    Map<OrderView, Object> userSeeks = userGroupSeeks.get(group);
                    if(userSeeks!=null) {
                        orderSeeks = new HashMap<OrderView, ObjectValue>();
                        for(Entry<OrderView, Object> userSeek : userSeeks.entrySet())
                            orderSeeks.put(userSeek.getKey(), session.getObjectValue(userSeek.getValue(),userSeek.getKey().getType()));
                    } else
                        orderSeeks = null;
                    updateKeys = true;
                    keepObject = false;
                }

                if(!updateKeys && group.gridClassView && (group.updated & GroupObjectImplement.UPDATED_OBJECT)!=0) { // скроллирование
                    int keyNum = group.keys.indexOf(currentObject);
                    if(keyNum< group.pageSize && group.upKeys) { // если меньше PageSize осталось и сверху есть ключи
                        updateKeys = true;

                        int lowestInd = group.pageSize *2-1;
                        if (lowestInd >= group.keys.size()) // по сути END
                            orderSeeks = null;
                        else {
                            direction = DIRECTION_UP;
                            orderSeeks = group.keyOrders.get(group.keys.get(lowestInd));
                        }
                    } else // наоборот вниз
                        if(keyNum>= group.keys.size()- group.pageSize && group.downKeys) {
                            updateKeys = true;

                            int highestInd = group.keys.size()- group.pageSize *2;
                            if (highestInd < 0) // по сути HOME
                                orderSeeks = new HashMap<OrderView, ObjectValue>();
                            else {
                                direction = DIRECTION_DOWN;
                                orderSeeks = group.keyOrders.get(group.keys.get(highestInd));
                            }
                        }
                }

                if(updateKeys) {
                    LinkedHashMap<Map<ObjectImplement, DataObject>, Map<OrderView, ObjectValue>> keyResult;
                    if(!group.gridClassView) { // панель
                        Map<ObjectImplement,? extends ObjectValue> objects;
                        boolean read = true; // по умолчанию читаем
                        if(group.singleViewType) { // assertion что нету ни фильтров ни порядков и !gridClassView, то есть чистое изменение класса объекта (или инициализация)
                            // assert'им что должен быть или целиком CustomObjectImplement или DataObjectImplement
                            Iterator<ObjectImplement> i = group.objects.iterator();
                            read = i.next() instanceof CustomObjectImplement;
                            while(i.hasNext())
                                assert read == i.next() instanceof CustomObjectImplement;
                        }
                        if(read)
                            objects = readKeys(group,orderSeeks); // перечитываем, а то мог удалится и т.п.
                        else
                            objects = BaseUtils.filterKeys(orderSeeks,group.objects);
                        updateGroupObject(group,result,objects);
                    } else {
                        if(orderSeeks!=null && !BaseUtils.starts(group.orders,orderSeeks.keySet())) // если не "хватает" спереди ключей, дочитываем
                            orderSeeks = readValues(group,orderSeeks);

                        int activeRow = -1; // какой ряд выбранным будем считать
                        keyResult = new LinkedHashMap<Map<ObjectImplement, DataObject>, Map<OrderView, ObjectValue>>();

                        if(direction==DIRECTION_CENTER) { // оптимизируем если HOME\END то читаем одним запросом
                            if(orderSeeks==null) { // END
                                direction = DIRECTION_UP;
                                group.downKeys = false;
                            } else
                                if(orderSeeks.isEmpty()) { // HOME
                                    direction = DIRECTION_DOWN;
                                    group.upKeys = false;
                                }
                        } else {
                            group.downKeys = true; assert !(orderSeeks==null);
                            group.upKeys = true; assert !(orderSeeks!=null && orderSeeks.isEmpty());
                        }

                        int readSize = group.pageSize *3/(direction ==DIRECTION_CENTER?2:1);
                        if(direction==DIRECTION_UP || direction ==DIRECTION_CENTER) { // сначала Up
                            keyResult.putAll(BaseUtils.reverse(executeSelectKeys(group, orderSeeks, readSize, false)));
                            group.upKeys = (keyResult.size()== readSize);
                            activeRow = keyResult.size()-1; 
                        }
                        if(direction ==DIRECTION_DOWN || direction ==DIRECTION_CENTER) { // затем Down
                            LinkedHashMap<Map<ObjectImplement, DataObject>, Map<OrderView, ObjectValue>> executeList = executeSelectKeys(group, orderSeeks, readSize, true);
                            if(executeList.size()>0) activeRow = keyResult.size();
                            keyResult.putAll(executeList);
                            group.downKeys = (executeList.size()== readSize);
                        }

                        group.keys = new ArrayList<Map<ObjectImplement, DataObject>>();
                        group.keyOrders = new HashMap<Map<ObjectImplement, DataObject>, Map<OrderView, ObjectValue>>();

                        // параллельно будем обновлять ключи чтобы JoinSelect'ить
                        ViewTable insertTable = groupTables.get(group);
                        if(insertTable==null) {
                            insertTable = new ViewTable(group, sessionID * RemoteFormInterface.GID_SHIFT + group.ID);
                            session.createTemporaryTable(insertTable);
                        }

                        List<Map<KeyField,DataObject>> viewKeys = new ArrayList<Map<KeyField, DataObject>>();
                        for(Entry<Map<ObjectImplement, DataObject>, Map<OrderView, ObjectValue>> resultRow : keyResult.entrySet()) {
                            viewKeys.add(BaseUtils.join(insertTable.mapKeys,resultRow.getKey()));

                            Map<ObjectImplement,DataObject> keyRow = new HashMap<ObjectImplement,DataObject>(resultRow.getKey());
                            group.keys.add(keyRow);
                            group.keyOrders.put(keyRow, BaseUtils.filterKeys(resultRow.getValue(),group.orders.keySet()));
                        }

                        groupTables.put(group, insertTable.writeKeys(session, viewKeys));

                        result.gridObjects.put(group, group.keys);

                        // если есть в новых ключах старый ключ, то делаем его активным
                        if(keepObject && group.keys.contains(currentObject))
                            activeRow = group.keys.indexOf(currentObject);

                        updateGroupObject(group,result,group.keys.isEmpty()?group.getNulls():group.keys.get(activeRow));
                    }
                    group.updated = (group.updated | GroupObjectImplement.UPDATED_KEYS);
                }
            }

            Collection<PropertyView> panelProps = new ArrayList<PropertyView>();
            Map<GroupObjectImplement,Collection<PropertyView>> groupProps = new HashMap<GroupObjectImplement, Collection<PropertyView>>();

            for(PropertyView<?> drawProp : properties) {

                boolean read = drawProp.view.dataUpdated(changedProps) ||
                        drawProp.toDraw!=null && (drawProp.toDraw.updated & GroupObjectImplement.UPDATED_KEYS)!=0;

                boolean inGridInterface; // прогоняем через кэши чтобы каждый раз не запускать isInInterface
                if(drawProp.view.classUpdated(drawProp.toDraw)) {
                    inGridInterface = drawProp.view.isInInterface(drawProp.toDraw);
                    cacheInGridInterface.put(drawProp, inGridInterface);
                } else { // пусть будут assert
                    inGridInterface = cacheInGridInterface.get(drawProp);
                    assert inGridInterface==drawProp.view.isInInterface(drawProp.toDraw);
                }

                boolean inInterface; // прогоняем через кэши чтобы каждый раз не запускать isInInterface
                if(drawProp.toDraw==null)
                    inInterface = inGridInterface;
                else
                    if(drawProp.view.classUpdated(null)) { // здесь еще можно вставить : что если inGridInterface и не null'ы
                        inInterface = drawProp.view.isInInterface(null);
                        cacheInInterface.put(drawProp, inInterface);
                    } else {
                        inInterface = cacheInInterface.get(drawProp);
                        assert inInterface==drawProp.view.isInInterface(null);
                    }

                if(inGridInterface && drawProp.toDraw.gridClassView) { // в grid'е
                    if(read || drawProp.view.objectUpdated(drawProp.toDraw)) {
                        Collection<PropertyView> propList = groupProps.get(drawProp.toDraw);
                        if(propList==null) {
                            propList = new ArrayList<PropertyView>();
                            groupProps.put(drawProp.toDraw,propList);
                        }
                        propList.add(drawProp);
                        isDrawed.add(drawProp);
                    }
                } else
                if(inInterface) { // в панели
                    if(read || drawProp.view.objectUpdated(null)) {
                        panelProps.add(drawProp);
                        isDrawed.add(drawProp);
                    }
                } else
                    if(isDrawed.remove(drawProp))
                        result.dropProperties.add(drawProp); // вкидываем удаление из интерфейса
            }

            if(panelProps.size()>0) { // читаем "панельные" свойства
                JoinQuery<Object, PropertyView> selectProps = new JoinQuery<Object, PropertyView>(new HashMap<Object, KeyExpr>());
                for(PropertyView<?> drawProp : panelProps)
                    selectProps.properties.put(drawProp, drawProp.view.getSourceExpr(null,null, session.changes, this));

                Map<PropertyView,Object> resultProps = BaseUtils.singleValue(selectProps.executeSelect(session));
                for(PropertyView drawProp : panelProps)
                    result.panelProperties.put(drawProp,resultProps.get(drawProp));
            }

            for(Entry<GroupObjectImplement, Collection<PropertyView>> mapGroup : groupProps.entrySet()) { // читаем "табличные" свойства
                GroupObjectImplement group = mapGroup.getKey();
                Collection<PropertyView> groupList = mapGroup.getValue();

                JoinQuery<ObjectImplement, PropertyView> selectProps = new JoinQuery<ObjectImplement, PropertyView>(group);

                ViewTable keyTable = groupTables.get(mapGroup.getKey()); // ставим фильтр на то что только из viewTable'а
                selectProps.and(keyTable.joinAnd(BaseUtils.join(keyTable.mapKeys,selectProps.mapKeys)).getWhere());

                for(PropertyView<?> drawProp : groupList)
                    selectProps.properties.put(drawProp, drawProp.view.getSourceExpr(group.getClassGroup(), selectProps.mapKeys, session.changes, this));

                LinkedHashMap<Map<ObjectImplement, Object>, Map<PropertyView, Object>> resultProps = selectProps.executeSelect(session);

                for(PropertyView drawProp : groupList) {
                    Map<Map<ObjectImplement,DataObject>,Object> propResult = new HashMap<Map<ObjectImplement,DataObject>, Object>();
                    for(Entry<Map<ObjectImplement, Object>, Map<PropertyView, Object>> resultRow : resultProps.entrySet())
                        propResult.put(group.findGroupObjectValue(resultRow.getKey()),resultRow.getValue().get(drawProp));
                    result.gridProperties.put(drawProp,propResult);
                }
            }

            userGroupSeeks.clear();

            // сбрасываем все пометки
            for(GroupObjectImplement group : groups) {
                for(ObjectImplement object : group.objects)
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
        for (GroupObjectImplement group : groups)
            if (group.gridClassView)
                reportObjects.add(group);

        return reportObjects;
    }

    // считывает все данные (для отчета)
    public FormData getFormData() throws SQLException {

        // вызовем endApply, чтобы быть полностью уверенным в том, что мы работаем с последними данными
        endApply();

        Set<GroupObjectImplement> classGroups = getClassGroups();

        Collection<ObjectImplement> readObjects = new ArrayList<ObjectImplement>();
        for(GroupObjectImplement group : classGroups)
            readObjects.addAll(group.objects);

        // пока сделаем тупо получаем один большой запрос

        JoinQuery<ObjectImplement,Object> query = new JoinQuery<ObjectImplement,Object>(ObjectImplement.getMapKeys(readObjects));
        LinkedHashMap<Object,Boolean> queryOrders = new LinkedHashMap<Object, Boolean>();

        for (GroupObjectImplement group : groups) {

            if (classGroups.contains(group)) {

                // не фиксированные ключи
                group.fillSourceSelect(query,classGroups, session.changes, this);

                // закинем Order'ы
                for(Entry<OrderView, Boolean> order : group.orders.entrySet()) {
                    query.properties.put(order.getKey(),order.getKey().getSourceExpr(classGroups, query.mapKeys, session.changes, this));
                    queryOrders.put(order.getKey(),order.getValue());
                }

                for(ObjectImplement object : group.objects) {
                    query.properties.put(object,object.getSourceExpr(classGroups,query.mapKeys));
                    queryOrders.put(object,false);
                }
            }
        }

        FormData result = new FormData();

        for(PropertyView<?> property : properties)
            query.properties.put(property, property.view.getSourceExpr(classGroups, query.mapKeys, session.changes, this));

        LinkedHashMap<Map<ObjectImplement, Object>, Map<Object, Object>> resultSelect = query.executeSelect(session,queryOrders,0);
        for(Entry<Map<ObjectImplement, Object>, Map<Object, Object>> row : resultSelect.entrySet()) {
            Map<ObjectImplement,Object> groupValue = new HashMap<ObjectImplement, Object>();
            for(GroupObjectImplement group : groups)
                for(ObjectImplement object : group.objects)
                    if (readObjects.contains(object))
                        groupValue.put(object,row.getKey().get(object));
                    else
                        groupValue.put(object,object.getObjectValue().getValue());

            Map<PropertyView,Object> propertyValues = new HashMap<PropertyView, Object>();
            for(PropertyView property : properties)
                propertyValues.put(property,row.getValue().get(property));

            result.add(groupValue,propertyValues);
        }

        return result;
    }

}

