/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platform.server.view.form;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.interop.Compare;
import platform.interop.Scroll;
import platform.interop.ClassViewType;
import platform.interop.exceptions.ComplexQueryException;
import platform.interop.form.RemoteFormInterface;
import platform.server.auth.SecurityPolicy;
import platform.server.data.KeyField;
import platform.server.data.type.TypeSerializer;
import platform.server.classes.*;
import platform.server.data.query.Query;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.where.EqualsWhere;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.*;
import platform.server.session.*;
import platform.server.view.form.filter.CompareFilter;
import platform.server.view.form.filter.Filter;
import platform.server.view.navigator.*;
import platform.server.view.navigator.filter.FilterNavigator;
import platform.server.data.where.Where;
import platform.server.caches.ManualLazy;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

// класс в котором лежит какие изменения произошли

// нужен какой-то объект который разделит клиента и серверную часть кинув каждому свои данные
// так клиента волнуют панели на форме, список гридов в привязке, дизайн и порядок представлений
// сервера колышет дерево и св-ва предст. с привязкой к объектам

public class RemoteForm<T extends BusinessLogics<T>> extends NoUpdateModifier {

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

    private UsedChanges fullChanges;
    @Override
    @ManualLazy
    public UsedChanges fullChanges() {
        if(fullChanges==null || !BaseUtils.hashEquals(fullChanges.session,getSession()))
            fullChanges = super.fullChanges();
        return fullChanges;
    }


    public Modifier<? extends Changes> update(final SessionChanges sessionChanges) {
        return new NoUpdateModifier(hintsNoUpdate) {
            public SessionChanges getSession() {
                return sessionChanges;
            }
        };
    }

    public Set<Property> getUpdateProperties(SessionChanges sessionChanges) {
        Set<Property> properties = new HashSet<Property>();
        Modifier<? extends Changes> propertyModifier = update(sessionChanges);
        for(Property<?> updateProperty : getUpdateProperties())
            if(updateProperty.hasChanges(propertyModifier))
                properties.add(updateProperty);
        return properties;
    }

    final FocusView<T> focusView;
    final CustomClassView classView;

    public final NavigatorForm navigatorForm;

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

        addObjectOnTransaction();
    }

    public RemoteForm(NavigatorForm<?> navigatorForm, T BL, DataSession session, SecurityPolicy securityPolicy, FocusView<T> focusView, CustomClassView classView) throws SQLException {
        this(navigatorForm, BL, session, securityPolicy, focusView, classView, new Mapper());
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

    public void serializePropertyEditorType(DataOutputStream outStream, PropertyView propertyView) throws SQLException, IOException {

        PropertyValueImplement change = propertyView.view.getChangeProperty();
        if(change.canBeChanged(this)) {
            outStream.writeBoolean(false);
            TypeSerializer.serialize(outStream,change.property.getType());
        } else
            outStream.writeBoolean(true);
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

    public boolean switchClassView(GroupObjectImplement group) {
        return changeClassView(group, ClassViewType.switchView(group.curClassView));
    }

    public boolean changeClassView(GroupObjectImplement group,byte show) {

        if ((show & group.banClassView) != 0) return false;

        if(group.curClassView == show) return false;
        group.curClassView = show;

        group.updated = group.updated | GroupObjectImplement.UPDATED_CLASSVIEW;
        return true;
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

        DataObject addObject = session.addObject(cls,this);

        boolean foundConflict = false;

        // берем все текущие CompareFilter на оператор 0(=) делаем DataChange на Value сразу в сессию
        // тогда добавляет для всех других объектов из того же GroupObjectImplement'а, значение Value, GetValueExpr
        for(Filter filter : object.groupTo.filters) {
            if(filter instanceof CompareFilter && (!Filter.ignoreInInterface || filter.isInInterface(object.groupTo))) { // если ignoreInInterface проверить что в интерфейсе 
                CompareFilter<?> compareFilter = (CompareFilter)filter;
                if(compareFilter.compare==Compare.EQUALS && compareFilter.property.property instanceof DataProperty) {
                    Query<PropertyObjectInterface,String> subQuery = new Query<PropertyObjectInterface,String>(compareFilter.property.mapping.values());
                    Map<PropertyObjectInterface,DataObject> fixedObjects = new HashMap<PropertyObjectInterface, DataObject>();
                    for(PropertyObjectInterface mapObject : compareFilter.property.mapping.values())
                        if(mapObject.getApplyObject() !=object.groupTo)
                            fixedObjects.put(mapObject, mapObject.getDataObject());
                        else // assert что тогда sibObject instanceof ObjectImplement потому как ApplyObject = null а object.groupTo !=null
                            if(mapObject !=object)
                                subQuery.and(subQuery.mapKeys.get(mapObject).isClass(((ObjectImplement)mapObject).getGridClass().getUpSet()));
                            else
                                fixedObjects.put(mapObject,addObject);
                    subQuery.putKeyWhere(fixedObjects);

                    subQuery.properties.put("newvalue", compareFilter.value.getExpr(object.groupTo.getClassGroup(),BaseUtils.filterKeys(subQuery.mapKeys,compareFilter.property.getObjectImplements()), this));

                    OrderedMap<Map<PropertyObjectInterface, DataObject>, Map<String, ObjectValue>> result = subQuery.executeClasses(session,BL.baseClass);
                    // изменяем св-ва
                    for(Map.Entry<Map<PropertyObjectInterface,DataObject>,Map<String,ObjectValue>> row : result.entrySet()) {
                        DataProperty changeProperty = (DataProperty) compareFilter.property.property;
                        Map<ClassPropertyInterface,DataObject> keys = new HashMap<ClassPropertyInterface, DataObject>();
                        for(ClassPropertyInterface propertyInterface : changeProperty.interfaces)
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
            changeClassView(object.groupTo, ClassViewType.PANEL);

        dataChanged = true;
    }

    public void changeClass(CustomObjectImplement object, int classID) throws SQLException {
        object.changeClass(session, classID);
        dataChanged = true;
    }

    public <P extends PropertyInterface> void changeProperty(PropertyObjectImplement<P> property, Object value) throws SQLException {

        // изменяем св-во
        property.getChangeProperty().change(session, this, value);

        dataChanged = true;
    }

    // Обновление данных
    public void refreshData() throws SQLException {

        for(GroupObjectImplement group : groups)
            for(ObjectImplement object : group.objects)
                if(object instanceof CustomObjectImplement)
                    ((CustomObjectImplement)object).refreshValueClass(session);
        refresh = true;
    }

    void addObjectOnTransaction() throws SQLException {
        for(GroupObjectImplement group : groups)
            for(ObjectImplement object : group.objects)
                if(object instanceof CustomObjectImplement) {
                    CustomObjectImplement customObject = (CustomObjectImplement)object;
                    if(customObject.addOnTransaction)
                        addObject(customObject, (ConcreteCustomClass) customObject.gridClass);
                }
    }

    // Применение изменений
    public String saveChanges() throws SQLException {
        String applyString = session.apply(BL);
        if(applyString==null) {
            refreshData();
            addObjectOnTransaction();
        }
        return applyString;
    }

    public void cancelChanges() throws SQLException {
        session.restart(true);

        // пробежим по всем объектам
        for(GroupObjectImplement group : groups)
            for(ObjectImplement object : group.objects)
                if(object instanceof CustomObjectImplement)
                    ((CustomObjectImplement)object).updateValueClass(session);
        addObjectOnTransaction();

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
                    viewTable.rewrite(session,group.keys.keySet());
                    groupTables.put(group,viewTable);                    
                }
            }
        }

        Collection<Group> groups = new ArrayList<Group>();
        Map<PropertyView,Boolean> cacheInGridInterface;
        Map<PropertyView,Boolean> cacheInInterface;
        Set<PropertyView> isDrawed;
        Map<RegularFilterGroup,RegularFilter> regularFilterValues;

        Map<RemoteForm, DataSession.UpdateChanges> incrementChanges;
        Map<RemoteForm, DataSession.UpdateChanges> appliedChanges;
        Map<RemoteForm, DataSession.UpdateChanges> updateChanges;

        SessionChanges changes;

        ApplyTransaction() {
            for(GroupObjectImplement group : RemoteForm.this.groups)
                groups.add(new Group(group));
            cacheInGridInterface = new HashMap<PropertyView, Boolean>(RemoteForm.this.cacheInGridInterface);
            cacheInInterface = new HashMap<PropertyView, Boolean>(RemoteForm.this.cacheInInterface);
            isDrawed = new HashSet<PropertyView>(RemoteForm.this.isDrawed);
            regularFilterValues = new HashMap<RegularFilterGroup, RegularFilter>(RemoteForm.this.regularFilterValues);

            if(dataChanged) {
                incrementChanges = new HashMap<RemoteForm, DataSession.UpdateChanges>(session.incrementChanges);
                appliedChanges = new HashMap<RemoteForm, DataSession.UpdateChanges>(session.appliedChanges);
                updateChanges = new HashMap<RemoteForm, DataSession.UpdateChanges>(session.updateChanges);
                changes = session.changes;
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
                session.appliedChanges = appliedChanges;
                session.updateChanges = updateChanges;
                session.changes = changes;
            }
        }
    }

    private final static int DIRECTION_DOWN = 1;
    private final static int DIRECTION_UP = 2;
    private final static int DIRECTION_CENTER = 3;

    // оболочка изменения group, чтобы отослать клиенту
    private void updateGroupObject(GroupObjectImplement group,FormChanges changes,Map<ObjectImplement,? extends ObjectValue> value) throws SQLException {

        Map<ObjectImplement,ConcreteValueClass> cls = new HashMap<ObjectImplement,ConcreteValueClass>();
        for (ObjectImplement object : value.keySet()) {
            ObjectValue objectValue = value.get(object);
            if (objectValue instanceof DataObject) {
                cls.put(object, (ConcreteValueClass)session.getCurrentClass((DataObject)objectValue));
            } // если не DataObject, а null, то можно даже не кидать в результат ничего - нету смысла
        }

        changes.objects.put(group, value);
        changes.classes.put(group, cls);

        changeGroupObject(group, value);
    }

    private OrderedMap<Map<ObjectImplement, DataObject>, Map<OrderView, ObjectValue>> executeKeys(GroupObjectImplement group,Map<OrderView,ObjectValue> orderSeeks,int readSize,boolean down) throws SQLException {
        // assertion что group.orders начинается с orderSeeks
        OrderedMap<OrderView,Boolean> orders;
        if(orderSeeks!=null && readSize==1)
            orders = group.orders.moveStart(orderSeeks.keySet());
        else
            orders = group.orders;

        assert !(orderSeeks!=null && !orders.starts(orderSeeks.keySet()));

        Map<ObjectImplement, KeyExpr> mapKeys = group.getMapKeys();
        
        Map<OrderView, Expr> orderExprs = new HashMap<OrderView, Expr>();
        for(Map.Entry<OrderView,Boolean> toOrder : orders.entrySet())
            orderExprs.put(toOrder.getKey(),toOrder.getKey().getExpr(group.getClassGroup(), mapKeys, this));

        Set<KeyExpr> usedContext = null;
        if(readSize==1 && orderSeeks!=null && down) { // в частном случае если есть "висячие" ключи не в фильтре и нужна одна запись ставим равно вместо >
            usedContext = new HashSet<KeyExpr>();
            group.getFilterWhere(mapKeys, group.getClassGroup(), this).enumKeys(usedContext); // именно после ff'са
            for(Expr expr : orderExprs.values())
                if(!(expr instanceof KeyExpr))
                    expr.enumKeys(usedContext);
        }

        Where orderWhere; // строим условия на упорядочивание
        if(orderSeeks!=null) {
            ObjectValue toSeek;
            orderWhere = Where.TRUE;
            for(Map.Entry<OrderView,Boolean> toOrder : orders.reverse().entrySet())
                if((toSeek = orderSeeks.get(toOrder.getKey()))!=null) {
                    Expr expr = orderExprs.get(toOrder.getKey());
                    if(readSize==1 && down && expr instanceof KeyExpr && toSeek instanceof DataObject && ((DataObject)toSeek).getType() instanceof DataClass && !usedContext.contains((KeyExpr)expr))
                        orderWhere = orderWhere.and(new EqualsWhere((KeyExpr)expr,((DataObject)toSeek).getExpr()));
                    else
                        orderWhere = toSeek.order(expr,toOrder.getValue(),orderWhere);
                }
        } else
            orderWhere = Where.FALSE;

        return new Query<ObjectImplement,OrderView>(mapKeys,orderExprs,group.getWhere(mapKeys, group.getClassGroup(), this).and(down?orderWhere:orderWhere.not())).executeClasses(session, down?orders:Query.reverseOrder(orders), readSize, BL.baseClass);
    }

    // считывает одну запись
    private Map.Entry<Map<ObjectImplement, DataObject>, Map<OrderView, ObjectValue>> readObjects(GroupObjectImplement group,Map<OrderView,ObjectValue> orderSeeks) throws SQLException {
        OrderedMap<Map<ObjectImplement, DataObject>, Map<OrderView, ObjectValue>> result = executeKeys(group, orderSeeks, 1, true);
        if(result.size()==0)
            result = executeKeys(group, orderSeeks, 1, false);
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

    boolean refresh = true;
    private boolean classUpdated(Updated updated, GroupObjectImplement groupObject) {
        assert !refresh; // refresh нужен для инициализации (например DataObjectImplement) больше чем для самого refresh
        return updated.classUpdated(groupObject);
    }
    private boolean objectUpdated(Updated updated, GroupObjectImplement groupObject) {
        assert !refresh;
        return updated.objectUpdated(groupObject);
    }
    private boolean dataUpdated(Updated updated, Collection<Property> changedProps) {
        assert !refresh;
        return updated.dataUpdated(changedProps);
    }

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

                if ((group.updated & GroupObjectImplement.UPDATED_CLASSVIEW) != 0)
                    result.classViews.put(group, group.curClassView);

                if (group.curClassView == ClassViewType.HIDE) continue;

                // если изменились класс грида или представление 
                boolean updateKeys = refresh || (group.updated & (GroupObjectImplement.UPDATED_GRIDCLASS | GroupObjectImplement.UPDATED_CLASSVIEW))!=0;

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
                            if(refresh || classUpdated(filt,group))
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
                        if((refresh || classUpdated(setOrder.getKey(),group)) && !(setOrder.getKey().isInInterface(group)==isInInterface)) {
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
                        if(objectUpdated(filt,group)) {updateKeys = true; break;}
                if(!updateKeys) // изменились "верхние" объекты для порядков
                    for(OrderView order : group.orders.keySet())
                        if(objectUpdated(order,group)) { updateKeys = true; break;}
                if(!updateKeys) // изменились данные по фильтрам
                    for(Filter filt : group.filters)
                        if(dataUpdated(filt,changedProps)) {updateKeys = true; break;}
                if(!updateKeys) // изменились данные по порядкам
                    for(OrderView order : group.orders.keySet())
                        if(dataUpdated(order,changedProps)) {updateKeys = true; break;}
                if(!updateKeys) // классы удалились\добавились
                    for(ObjectImplement object : group.objects)
                        if(object.classChanged(changedClasses) || object.classUpdated()) {updateKeys = true; break;}

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

                if(!updateKeys && group.curClassView == ClassViewType.GRID && (group.updated & GroupObjectImplement.UPDATED_OBJECT)!=0) { // скроллирование
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
                    if(group.curClassView != ClassViewType.GRID) // панель
                        updateGroupObject(group,result,readKeys(group,orderSeeks));
                    else {
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
                            keyResult.putAll(executeKeys(group, orderSeeks, readSize, false).reverse());
                            group.upKeys = (keyResult.size()== readSize);
                            activeRow = keyResult.size()-1; 
                        }
                        if(direction ==DIRECTION_DOWN || direction ==DIRECTION_CENTER) { // затем Down
                            OrderedMap<Map<ObjectImplement, DataObject>, Map<OrderView, ObjectValue>> executeList = executeKeys(group, orderSeeks, readSize, true);
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

                        List<Map<ObjectImplement, DataObject>> resultObjects = new ArrayList<Map<ObjectImplement, DataObject>>(group.keys.keySet());
                        List<Map<ObjectImplement, ConcreteValueClass>> resultClasses = new ArrayList<Map<ObjectImplement, ConcreteValueClass>>();

                        // заполняем классы полученных рядов
                        for (Map<ObjectImplement, DataObject> row : resultObjects) {
                            Map<ObjectImplement, ConcreteValueClass> rowClass = new HashMap<ObjectImplement, ConcreteValueClass>();
                            for (ObjectImplement object : row.keySet()) {
                                rowClass.put(object, (ConcreteValueClass)session.getCurrentClass(row.get(object)));
                            }
                            resultClasses.add(rowClass);
                        }

                        result.gridObjects.put(group, resultObjects);
                        result.gridClasses.put(group, resultClasses);

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

                if (drawProp.toDraw != null && drawProp.toDraw.curClassView == ClassViewType.HIDE) continue;

                // прогоняем через кэши чтобы каждый раз не запускать isInInterface
                boolean inGridInterface, inInterface;

                if(refresh || classUpdated(drawProp.view,drawProp.toDraw)) {
                    inGridInterface = drawProp.view.isInInterface(drawProp.toDraw);
                    cacheInGridInterface.put(drawProp, inGridInterface);
                } else { // пусть будут assert
                    inGridInterface = cacheInGridInterface.get(drawProp);
                    assert inGridInterface==drawProp.view.isInInterface(drawProp.toDraw);
                }

                if(drawProp.toDraw==null)
                    inInterface = inGridInterface;
                else
                    if(refresh || classUpdated(drawProp.view,null)) { // здесь еще можно вставить : что если inGridInterface и не null'ы
                        inInterface = drawProp.view.isInInterface(null);
                        cacheInInterface.put(drawProp, inInterface);
                    } else {
                        inInterface = cacheInInterface.get(drawProp);
                        assert inInterface==drawProp.view.isInInterface(null);
                    }

                boolean read = refresh || dataUpdated(drawProp.view,changedProps) ||
                        drawProp.toDraw!=null && (drawProp.toDraw.updated & GroupObjectImplement.UPDATED_KEYS)!=0;
                if(inGridInterface && drawProp.toDraw!=null && drawProp.toDraw.curClassView == ClassViewType.GRID) { // в grid'е
                    if(read || objectUpdated(drawProp.view,drawProp.toDraw)) {
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
                    if(read || objectUpdated(drawProp.view,null)) {
                        panelProps.add(drawProp);
                        isDrawed.add(drawProp);
                    }
                } else
                    if(isDrawed.remove(drawProp))
                        result.dropProperties.add(drawProp); // вкидываем удаление из интерфейса
            }

            if(panelProps.size()>0) { // читаем "панельные" свойства
                Query<Object, PropertyView> selectProps = new Query<Object, PropertyView>(new HashMap<Object, KeyExpr>());
                for(PropertyView<?> drawProp : panelProps)
                    selectProps.properties.put(drawProp, drawProp.view.getExpr(null,null, this));

                Map<PropertyView,Object> resultProps = selectProps.execute(session).singleValue();
                for(PropertyView drawProp : panelProps)
                    result.panelProperties.put(drawProp,resultProps.get(drawProp));
            }

            for(Entry<GroupObjectImplement, Collection<PropertyView>> mapGroup : groupProps.entrySet()) { // читаем "табличные" свойства
                GroupObjectImplement group = mapGroup.getKey();
                Collection<PropertyView> groupList = mapGroup.getValue();

                Query<ObjectImplement, PropertyView> selectProps = new Query<ObjectImplement, PropertyView>(group);

                ViewTable keyTable = groupTables.get(mapGroup.getKey()); // ставим фильтр на то что только из viewTable'а
                selectProps.and(keyTable.joinAnd(BaseUtils.join(keyTable.mapKeys,selectProps.mapKeys)).getWhere());

                for(PropertyView<?> drawProp : groupList)
                    selectProps.properties.put(drawProp, drawProp.view.getExpr(group.getClassGroup(), selectProps.mapKeys, this));

                OrderedMap<Map<ObjectImplement, Object>, Map<PropertyView, Object>> resultProps = selectProps.execute(session);

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
        refresh = false;
        dataChanged = false;

//        result.out(this);

        return result;
    }

    // возвращает какие объекты отчета фиксируются
    private Set<GroupObjectImplement> getClassGroups() {

        Set<GroupObjectImplement> reportObjects = new HashSet<GroupObjectImplement>();
        for (GroupObjectImplement group : groups)
            if (group.curClassView == ClassViewType.GRID)
                reportObjects.add(group);

        return reportObjects;
    }

    // считывает все данные (для отчета)
    public FormData getFormData(boolean allProperties) throws SQLException {

        // вызовем endApply, чтобы быть полностью уверенным в том, что мы работаем с последними данными
        endApply();

        Set<GroupObjectImplement> classGroups = getClassGroups();

        Collection<ObjectImplement> readObjects = new ArrayList<ObjectImplement>();
        for(GroupObjectImplement group : classGroups)
            readObjects.addAll(group.objects);

        // пока сделаем тупо получаем один большой запрос

        Query<ObjectImplement,Object> query = new Query<ObjectImplement,Object>(readObjects);
        OrderedMap<Object,Boolean> queryOrders = new OrderedMap<Object, Boolean>();

        for (GroupObjectImplement group : groups) {

            if (classGroups.contains(group)) {

                // не фиксированные ключи
                query.and(group.getWhere(query.mapKeys, classGroups, this));

                // закинем Order'ы
                for(Entry<OrderView, Boolean> order : group.orders.entrySet()) {
                    query.properties.put(order.getKey(),order.getKey().getExpr(classGroups, query.mapKeys, this));
                    queryOrders.put(order.getKey(),order.getValue());
                }

                for(ObjectImplement object : group.objects) {
                    query.properties.put(object,object.getExpr(classGroups,query.mapKeys));
                    queryOrders.put(object,false);
                }
            }
        }

        FormData result = new FormData();

        for(PropertyView<?> property : properties)
            if (allProperties || property.view.getApplyObject().curClassView != ClassViewType.HIDE) // если свойство находится не в GroupObject, который спрятан
                query.properties.put(property, property.view.getExpr(classGroups, query.mapKeys, this));

        OrderedMap<Map<ObjectImplement, Object>, Map<Object, Object>> resultSelect = query.execute(session,queryOrders,0);
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

