/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platform.server.view.form;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.interop.Compare;
import platform.interop.Scroll;
import platform.interop.exceptions.ComplexQueryException;
import platform.interop.form.RemoteFormInterface;
import platform.server.auth.SecurityPolicy;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.classes.ConcreteCustomClass;
import platform.server.data.classes.CustomClass;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.cases.CaseExpr;
import platform.server.data.types.Type;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.properties.*;
import platform.server.session.*;
import platform.server.view.form.filter.CompareFilter;
import platform.server.view.form.filter.Filter;
import platform.server.view.form.client.RemoteFormView;
import platform.server.view.navigator.*;
import platform.server.view.navigator.filter.FilterNavigator;
import platform.server.where.Where;
import platform.server.where.WhereBuilder;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.rmi.RemoteException;

import net.sf.jasperreports.engine.JRException;

// класс в котором лежит какие изменения произошли

// нужен какой-то объект который разделит клиента и серверную часть кинув каждому свои данные
// так клиента волнуют панели на форме, список гридов в привязке, дизайн и порядок представлений
// сервера колышет дерево и св-ва предст. с привязкой к объектам

public class RemoteForm<T extends BusinessLogics<T>> extends TableModifier<RemoteForm.UsedChanges> {

    public final int viewID;

    T BL;

    public DataSession session;

    public SessionChanges getSession() {
        return session.changes;
    }

    SecurityPolicy securityPolicy;

    public CustomClass getCustomClass(int classID) {
        return BL.baseClass.findClassID(classID);
    }

    private class Update extends ViewModifier {

        public Update(ViewDataChanges view) {
            super(view);
        }

        public ViewDataChanges used(Property property, ViewDataChanges usedChanges) {
            if(hintsNoUpdate.contains(property))
                return new ViewDataChanges();
            return usedChanges;
        }
    }
    public ViewModifier update(ViewDataChanges view) {
        return new Update(view);
    }

    final FocusView<T> focusView;
    final CustomClassView classView;

    public final NavigatorForm navigatorForm;

    public RemoteFormInterface createClassForm(CustomObjectImplement object,int exportPort) throws RemoteException, SQLException, JRException {

        NavigatorForm navigatorForm = object.baseClass.getClassForm(securityPolicy);

        RemoteForm<?> remoteForm = new RemoteForm<T>(navigatorForm,BL,session,securityPolicy,focusView,classView);

        return new RemoteFormView(remoteForm,navigatorForm.getRichDesign(),navigatorForm.getReportDesign(),exportPort);
    }

    public <P extends PropertyInterface> RemoteFormInterface createChangeForm(PropertyObjectImplement<P> implement,int exportPort) throws SQLException, JRException, RemoteException {

        MapChangeDataProperty<P> change = implement.getChangeProperty(securityPolicy.property.change, false);
        DataChangeNavigatorForm navigatorForm = BL.getDataChangeForm(change.property);

        Mapper mapper = new Mapper(session);
        RemoteForm<T> remoteForm = new RemoteForm<T>(navigatorForm,BL,session,securityPolicy,focusView,classView,mapper);

        Map<DataPropertyInterface, DataObject> interfaceValues = BaseUtils.join(change.mapping,implement.getInterfaceValues());
        navigatorForm.seekObjects(remoteForm, mapper, change.property.read(session,interfaceValues,remoteForm), DataObject.getMapValues(interfaceValues));

        return new RemoteFormView(remoteForm,navigatorForm.getRichDesign(),navigatorForm.getReportDesign(),exportPort);
    }

    public static class Mapper implements FilterNavigator.Mapper {

        private ChangesSession readValue; // чисто для mapValue
        public Mapper(ChangesSession readValue) {
            this.readValue = readValue;
        }

        public final Map<ObjectNavigator, ObjectImplement> objectMapper = new HashMap<ObjectNavigator, ObjectImplement>();

        private ObjectImplement mapObject(ObjectNavigator objKey,CustomClassView classView) {

            ObjectImplement objValue = objKey.baseClass.newObject(objKey.ID,objKey.getSID(),objKey.caption,classView);
            objectMapper.put(objKey, objValue);
            return objValue;
        }

        public ObjectImplement mapObject(ObjectNavigator navigator) {
            return objectMapper.get(navigator);
        }

        public final Map<GroupObjectNavigator, GroupObjectImplement> groupMapper = new HashMap<GroupObjectNavigator, GroupObjectImplement>();

        private GroupObjectImplement mapGroup(GroupObjectNavigator groupKey,int order,CustomClassView classView) {

            Collection<ObjectImplement> objects = new ArrayList<ObjectImplement>();
            for (ObjectNavigator object : groupKey)
                objects.add(mapObject(object,classView));

            GroupObjectImplement groupValue = new GroupObjectImplement(groupKey.ID,objects,order,
                    groupKey.pageSize,groupKey.gridClassView,groupKey.singleViewType);

            groupMapper.put(groupKey, groupValue);
            return groupValue;
        }

        private final Map<PropertyObjectNavigator, PropertyObjectImplement> propertyMapper = new HashMap<PropertyObjectNavigator, PropertyObjectImplement>();

        public <P extends PropertyInterface> PropertyObjectImplement<P> mapProperty(PropertyObjectNavigator<P> propKey) {

            if (propertyMapper.containsKey(propKey)) return propertyMapper.get(propKey);

            PropertyObjectImplement<P> propValue = new PropertyObjectImplement<P>(propKey.property,BaseUtils.join(propKey.mapping,objectMapper));

            propertyMapper.put(propKey, propValue);
            return propValue;
        }

        <P extends PropertyInterface> PropertyView mapPropertyView(PropertyViewNavigator<P> propKey) {
            return new PropertyView<P>(propKey.ID, propKey.getSID(), mapProperty(propKey.view), groupMapper.get(propKey.toDraw));
        }

        public ObjectValue mapValue(java.lang.Object value, Type type) throws SQLException {
            return readValue.getObjectValue(value,type);
        }
    }

    private RemoteForm(NavigatorForm<?> navigatorForm, T BL, DataSession session, SecurityPolicy securityPolicy, FocusView<T> focusView, CustomClassView classView, Mapper mapper) throws SQLException {
        this.navigatorForm = navigatorForm;
        this.BL = BL;
        this.session = session;
        this.securityPolicy = securityPolicy;

        this.focusView = focusView;
        this.classView = classView;

        viewID = this.session.generateViewID(navigatorForm.ID);

        hintsNoUpdate = navigatorForm.hintsNoUpdate;
        hintsSave = navigatorForm.hintsSave;

        for (int i=0;i< navigatorForm.groups.size();i++)
            groups.add(mapper.mapGroup(navigatorForm.groups.get(i),i,classView));

        for (PropertyViewNavigator navigatorProperty : navigatorForm.propertyViews)
            if (this.securityPolicy.property.view.checkPermission(navigatorProperty.view.property))
                properties.add(mapper.mapPropertyView(navigatorProperty));

        for (FilterNavigator navigatorFilter : navigatorForm.fixedFilters) {
            Filter filter = navigatorFilter.doMapping(mapper);
            filter.getApplyObject().fixedFilters.add(filter);
        }

        for (RegularFilterGroupNavigator navigatorGroup : navigatorForm.regularFilterGroups) {

            RegularFilterGroup group = new RegularFilterGroup(navigatorGroup.ID);
            for (RegularFilterNavigator filter : navigatorGroup.filters)
                group.addFilter(new RegularFilter(filter.ID, filter.filter.doMapping(mapper), filter.name, filter.key));

            regularFilterGroups.add(group);
        }
    }

    public RemoteForm(NavigatorForm<?> navigatorForm, T BL, DataSession session, SecurityPolicy securityPolicy, FocusView<T> focusView, CustomClassView classView) throws SQLException {
        this(navigatorForm, BL, session, securityPolicy, focusView, classView, new Mapper(session));
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
        for (GroupObjectImplement groupObject : groups) {
            ObjectImplement result = groupObject.getObjectImplement(objectID);
            if(result!=null) return result;
        }
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
    public Map<GroupObjectImplement,Map<? extends OrderView,Object>> userGroupSeeks = new HashMap<GroupObjectImplement, Map<? extends OrderView, Object>>();

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
        if(prevFilter!=null)
            prevFilter.filter.getApplyObject().removeRegularFilter(prevFilter.filter);

        if (filter == null)
            regularFilterValues.remove(filterGroup);
        else {
            regularFilterValues.put(filterGroup, filter);
            filter.filter.getApplyObject().addRegularFilter(filter.filter);
        }

    }

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
                    JoinQuery<PropertyObjectInterface,String> subQuery = new JoinQuery<PropertyObjectInterface,String>(KeyExpr.getMapKeys(compareFilter.property.mapping.values()));
                    Map<PropertyObjectInterface,DataObject> fixedObjects = new HashMap<PropertyObjectInterface, DataObject>();
                    for(PropertyObjectInterface mapObject : compareFilter.property.mapping.values())
                        if(mapObject.getApplyObject() !=object.groupTo)
                            fixedObjects.put(mapObject, mapObject.getDataObject());
                        else // assert что тогда sibObject instanceof ObjectImplement потому как ApplyObject = null а object.groupTo !=null
                            if(mapObject !=object)
                                subQuery.and(subQuery.mapKeys.get(mapObject).getIsClassWhere(((ObjectImplement)mapObject).getGridClass().getUpSet()));
                            else
                                fixedObjects.put(mapObject,addObject);
                    subQuery.putKeyWhere(fixedObjects);

                    subQuery.properties.put("newvalue", compareFilter.value.getSourceExpr(object.groupTo.getClassGroup(),BaseUtils.filterKeys(subQuery.mapKeys,compareFilter.property.getObjectImplements()), this));

                    OrderedMap<Map<PropertyObjectInterface, DataObject>, Map<String, ObjectValue>> result = subQuery.executeSelectClasses(session,BL.baseClass);
                    // изменяем св-ва
                    for(Map.Entry<Map<PropertyObjectInterface,DataObject>,Map<String,ObjectValue>> row : result.entrySet()) {
                        DataProperty changeProperty = (DataProperty) compareFilter.property.property;
                        Map<DataPropertyInterface,DataObject> keys = new HashMap<DataPropertyInterface, DataObject>();
                        for(DataPropertyInterface propertyInterface : changeProperty.interfaces)
                            keys.put(propertyInterface,row.getKey().get(compareFilter.property.mapping.get(propertyInterface)));
                        session.changeProperty(changeProperty,keys,row.getValue().get("newvalue"),false);
                    }
                } else
                    if (object.groupTo.equals(compareFilter.getApplyObject()))
                        foundConflict = true;
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
            for(ObjectImplement object : group.objects)
                if(object instanceof CustomObjectImplement)
                    object.updated |= ObjectImplement.UPDATED_GRIDCLASS; 
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

    public static class UsedChanges extends TableChanges<UsedChanges> {
        final Collection<Property> noUpdateProps = new ArrayList<Property>();

        @Override
        public void add(UsedChanges add) {
            super.add(add);
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
        if(changes.hasChanges() && hintsNoUpdate.contains(property)) {
            changes = new UsedChanges();
            changes.noUpdateProps.add(property);
        }
        return changes;
    }

    public UsedChanges newChanges() {
        return new UsedChanges();
    }

    public Collection<Property> hintsSave = new HashSet<Property>();

    public boolean hasSessionChanges() {
        return session.changes.hasChanges();
    }

    // транзакция для отката при exception'ах
    private class ApplyTransaction {

        private class Group {

            private abstract class Object<O extends ObjectImplement> {
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
            private class Custom extends Object<CustomObjectImplement> {
                ObjectValue value;
                ConcreteCustomClass currentClass;

                private Custom(CustomObjectImplement object) {
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
            private class Data extends Object<DataObjectImplement> {
                java.lang.Object value;

                private Data(DataObjectImplement object) {
                    super(object);
                    value = object.value;
                }

                void rollback() {
                    super.rollback();
                    object.value = value;
                }
            }

            GroupObjectImplement group;
            boolean upKeys,downKeys;
            Set<Filter> filters;
            OrderedMap<OrderView,Boolean> orders;
            OrderedMap<Map<ObjectImplement,platform.server.logics.DataObject>,Map<OrderView,ObjectValue>> keys;
            int updated;

            Collection<Object> objects = new ArrayList<Object>();

            ViewTable viewTable;

            private Group(GroupObjectImplement iGroup) {
                group = iGroup;

                filters = new HashSet<Filter>(group.filters);
                orders = new OrderedMap<OrderView, Boolean>(group.orders);
                upKeys = group.upKeys; downKeys = group.downKeys;
                keys = new OrderedMap<Map<ObjectImplement,DataObject>,Map<OrderView, ObjectValue>>(group.keys);
                updated = group.updated;

                for(ObjectImplement object : group.objects)
                    objects.add(object instanceof CustomObjectImplement?new Custom((CustomObjectImplement) object):new Data((DataObjectImplement) object));

                viewTable = groupTables.get(group);
            }

            void rollback() throws SQLException {
                group.filters = filters;
                group.orders = orders;
                group.upKeys = upKeys; group.downKeys = downKeys;
                group.keys = keys;
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
                    for(Map<ObjectImplement, DataObject> keyRow : group.keys.keySet())
                        session.insertRecord(viewTable,BaseUtils.join(viewTable.mapKeys,keyRow),new HashMap<PropertyField,ObjectValue>());
                    groupTables.put(group,viewTable);                    
                }
            }
        }

        Collection<Group> groups = new ArrayList<Group>();
        Map<PropertyView,Boolean> cacheInGridInterface;
        Map<PropertyView,Boolean> cacheInInterface;
        Set<PropertyView> isDrawed;
        Map<RegularFilterGroup,RegularFilter> regularFilterValues;

        Map<RemoteForm, ViewDataChanges> incrementChanges;

        SessionChanges changes;

        ApplyTransaction() {
            for(GroupObjectImplement group : RemoteForm.this.groups)
                groups.add(new Group(group));
            cacheInGridInterface = new HashMap<PropertyView, Boolean>(RemoteForm.this.cacheInGridInterface);
            cacheInInterface = new HashMap<PropertyView, Boolean>(RemoteForm.this.cacheInInterface);
            isDrawed = new HashSet<PropertyView>(RemoteForm.this.isDrawed);
            regularFilterValues = new HashMap<RegularFilterGroup, RegularFilter>(RemoteForm.this.regularFilterValues);

            if(dataChanged) {
                incrementChanges = new HashMap<RemoteForm, ViewDataChanges>(session.incrementChanges);
                changes = new SessionChanges(session.changes);
            }
        }

        void rollback() throws SQLException {
            for(Group group : groups)
                group.rollback();
            RemoteForm.this.cacheInGridInterface = cacheInGridInterface;
            RemoteForm.this.cacheInInterface = cacheInInterface;
            RemoteForm.this.isDrawed = isDrawed;
            RemoteForm.this.regularFilterValues = regularFilterValues;

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

    private OrderedMap<Map<ObjectImplement, DataObject>, Map<OrderView, ObjectValue>> executeSelectKeys(GroupObjectImplement group,Map<OrderView,ObjectValue> orderSeeks,int readSize,boolean down) throws SQLException {
        // assertion что group.orders начинается с orderSeeks
        OrderedMap<OrderView,Boolean> orders;
        if(orderSeeks!=null && readSize==1)
            orders = group.orders.moveStart(orderSeeks.keySet());
        else
            orders = group.orders;

        assert !(orderSeeks!=null && !orders.starts(orderSeeks.keySet()));

        JoinQuery<ObjectImplement,OrderView> selectKeys = new JoinQuery<ObjectImplement,OrderView>(group); // object потому как нужно еще по ключам упорядочивать, а их тогда надо в св-ва кидать
        group.fillSourceSelect(selectKeys, group.getClassGroup(), this);

        Where orderWhere = orderSeeks==null?Where.FALSE:Where.TRUE;
        for(Map.Entry<OrderView,Boolean> toOrder : orders.reverse().entrySet()) {
            SourceExpr orderExpr = toOrder.getKey().getSourceExpr(group.getClassGroup(), selectKeys.mapKeys, this);
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
        OrderedMap<Map<ObjectImplement, DataObject>, Map<OrderView, ObjectValue>> result = executeSelectKeys(group, orderSeeks, 1, true);
        if(result.size()==0)
            result = executeSelectKeys(group, orderSeeks, 1, false);
        if(result.size()>0)
            return result.singleEntry();
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

    public static Map<ObjectImplement,DataObject> dataKeys(Map<ObjectImplement,ObjectValue> map) {
        return (Map<ObjectImplement,DataObject>)(Map<ObjectImplement,? extends ObjectValue>)map; 
    }

    // "закэшированная" проверка присутствия в интерфейсе, отличается от кэша тем что по сути функция от mutable объекта
    protected Map<PropertyView,Boolean> cacheInGridInterface = new HashMap<PropertyView,Boolean>();
    protected Map<PropertyView,Boolean> cacheInInterface = new HashMap<PropertyView, Boolean>();
    protected Set<PropertyView> isDrawed = new HashSet<PropertyView>(); 

    public FormChanges endApply() throws SQLException {

        ApplyTransaction transaction = new ApplyTransaction();

        FormChanges result = new FormChanges();

        try {
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
                OrderedMap<OrderView,Boolean> newOrders = new OrderedMap<OrderView, Boolean>();
                if((group.updated & GroupObjectImplement.UPDATED_ORDER)!=0) {
                    for(Entry<OrderView, Boolean> setOrder : group.getSetOrders().entrySet())
                        if(setOrder.getKey().isInInterface(group))
                            newOrders.put(setOrder.getKey(),setOrder.getValue());
                    updateKeys |= !group.orders.equals(newOrders);
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

                if(userGroupSeeks.containsKey(group)) { // пользовательский поиск
                    Map<? extends OrderView, Object> userSeeks = userGroupSeeks.get(group);
                    if(userSeeks!=null) {
                        orderSeeks = new HashMap<OrderView, ObjectValue>();
                        for(Entry<? extends OrderView, Object> userSeek : userSeeks.entrySet())
                            orderSeeks.put(userSeek.getKey(), session.getObjectValue(userSeek.getValue(),userSeek.getKey().getType()));
                    } else
                        orderSeeks = null;
                    updateKeys = true;
                    keepObject = false;
                } else
                    if(updateKeys) // изменились фильтры, порядки, вид, ищем текущий объект
                        orderSeeks = new HashMap<OrderView, ObjectValue>(currentObject);

                if(!updateKeys && group.gridClassView && (group.updated & GroupObjectImplement.UPDATED_OBJECT)!=0) { // скроллирование
                    int keyNum = group.keys.indexOf(dataKeys(currentObject));
                    if(keyNum< group.pageSize && group.upKeys) { // если меньше PageSize осталось и сверху есть ключи
                        updateKeys = true;

                        int lowestInd = group.pageSize *2-1;
                        if (lowestInd >= group.keys.size()) // по сути END
                            orderSeeks = null;
                        else {
                            direction = DIRECTION_UP;
                            orderSeeks = group.keys.getValue(lowestInd);
                        }
                    } else // наоборот вниз
                        if(keyNum>= group.keys.size()- group.pageSize && group.downKeys) {
                            updateKeys = true;

                            int highestInd = group.keys.size()- group.pageSize *2;
                            if (highestInd < 0) // по сути HOME
                                orderSeeks = new HashMap<OrderView, ObjectValue>();
                            else {
                                direction = DIRECTION_DOWN;
                                orderSeeks = group.keys.getValue(highestInd);
                            }
                        }
                }

                if(updateKeys) {
                    OrderedMap<Map<ObjectImplement, DataObject>, Map<OrderView, ObjectValue>> keyResult;
                    if(!group.gridClassView) { // панель
                        Map<ObjectImplement,? extends ObjectValue> objects;
                        boolean read = true; // по умолчанию читаем
                        if(group.singleViewType) { // assertion что нету ни фильтров ни порядков и !gridClassView, то есть чистое изменение класса объекта (или инициализация)
                            // assert'им что должен быть или целиком CustomObjectImplement или DataObjectImplement
                            read = group.objects.iterator().next() instanceof CustomObjectImplement;
                            assert group.isSolid();
                        }
                        if(read)
                            objects = readKeys(group,orderSeeks); // перечитываем, а то мог удалится и т.п.
                        else
                            objects = BaseUtils.filterKeys(orderSeeks,group.objects);
                        updateGroupObject(group,result,objects);
                    } else {
                        if(orderSeeks!=null && !group.orders.starts(orderSeeks.keySet())) // если не "хватает" спереди ключей, дочитываем
                            orderSeeks = readValues(group,orderSeeks);

                        int activeRow = -1; // какой ряд выбранным будем считать
                        keyResult = new OrderedMap<Map<ObjectImplement, DataObject>, Map<OrderView, ObjectValue>>();

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
                            keyResult.putAll(executeSelectKeys(group, orderSeeks, readSize, false).reverse());
                            group.upKeys = (keyResult.size()== readSize);
                            activeRow = keyResult.size()-1; 
                        }
                        if(direction ==DIRECTION_DOWN || direction ==DIRECTION_CENTER) { // затем Down
                            OrderedMap<Map<ObjectImplement, DataObject>, Map<OrderView, ObjectValue>> executeList = executeSelectKeys(group, orderSeeks, readSize, true);
                            if(executeList.size()>0) activeRow = keyResult.size();
                            keyResult.putAll(executeList);
                            group.downKeys = (executeList.size()== readSize);
                        }

                        group.keys = new OrderedMap<Map<ObjectImplement, DataObject>,Map<OrderView, ObjectValue>>();

                        // параллельно будем обновлять ключи чтобы JoinSelect'ить
                        ViewTable insertTable = groupTables.get(group);
                        if(insertTable==null) {
                            insertTable = new ViewTable(group, viewID * RemoteFormInterface.GID_SHIFT + group.ID);
                            session.createTemporaryTable(insertTable);
                        }

                        List<Map<KeyField,DataObject>> viewKeys = new ArrayList<Map<KeyField, DataObject>>();
                        for(Entry<Map<ObjectImplement, DataObject>, Map<OrderView, ObjectValue>> resultRow : keyResult.entrySet()) {
                            viewKeys.add(BaseUtils.join(insertTable.mapKeys,resultRow.getKey()));

                            group.keys.put(new HashMap<ObjectImplement,DataObject>(resultRow.getKey()),BaseUtils.filterKeys(resultRow.getValue(),group.orders.keySet()));
                        }

                        groupTables.put(group, insertTable.writeKeys(session, viewKeys));

                        result.gridObjects.put(group, new ArrayList<Map<ObjectImplement, DataObject>>(group.keys.keySet()));

                        // если есть в новых ключах старый ключ, то делаем его активным
                        if(keepObject && group.keys.containsKey(dataKeys(currentObject)))
                            activeRow = group.keys.indexOf(dataKeys(currentObject));

                        updateGroupObject(group,result,group.keys.isEmpty()?group.getNulls():group.keys.getKey(activeRow));
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
                    selectProps.properties.put(drawProp, drawProp.view.getSourceExpr(null,null, this));

                Map<PropertyView,Object> resultProps = selectProps.executeSelect(session).singleValue();
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
                    selectProps.properties.put(drawProp, drawProp.view.getSourceExpr(group.getClassGroup(), selectProps.mapKeys, this));

                OrderedMap<Map<ObjectImplement, Object>, Map<PropertyView, Object>> resultProps = selectProps.executeSelect(session);

                for(PropertyView drawProp : groupList) {
                    Map<Map<ObjectImplement,DataObject>,Object> propResult = new HashMap<Map<ObjectImplement,DataObject>, Object>();
                    for(Entry<Map<ObjectImplement, Object>, Map<PropertyView, Object>> resultRow : resultProps.entrySet())
                        propResult.put(group.findGroupObjectValue(resultRow.getKey()),resultRow.getValue().get(drawProp));
                    result.gridProperties.put(drawProp,propResult);
                }
            }
        } catch (ComplexQueryException e) {
            transaction.rollback();
            if(dataChanged) { // если изменились данные cancel'им изменения
                cancelChanges();
                result = endApply();
                result.message = e.getMessage()+". Изменения будут отменены";
                return result;
            } else
                throw e;
        } catch (RuntimeException e) {
            transaction.rollback();
            throw e;
        } catch (SQLException e) {
            transaction.rollback();
            throw e;
        }

        userGroupSeeks.clear();

        // сбрасываем все пометки
        for(GroupObjectImplement group : groups) {
            for(ObjectImplement object : group.objects)
                object.updated = 0;
            group.updated = 0;
        }
        dataChanged = false;

        return result;
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

        JoinQuery<ObjectImplement,Object> query = new JoinQuery<ObjectImplement,Object>(KeyExpr.getMapKeys(readObjects));
        OrderedMap<Object,Boolean> queryOrders = new OrderedMap<Object, Boolean>();

        for (GroupObjectImplement group : groups) {

            if (classGroups.contains(group)) {

                // не фиксированные ключи
                group.fillSourceSelect(query, classGroups, this);

                // закинем Order'ы
                for(Entry<OrderView, Boolean> order : group.orders.entrySet()) {
                    query.properties.put(order.getKey(),order.getKey().getSourceExpr(classGroups, query.mapKeys, this));
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
            query.properties.put(property, property.view.getSourceExpr(classGroups, query.mapKeys, this));

        OrderedMap<Map<ObjectImplement, Object>, Map<Object, Object>> resultSelect = query.executeSelect(session,queryOrders,0);
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

