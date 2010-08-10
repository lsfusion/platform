/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platform.server.view.form;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.interop.ClassViewType;
import platform.interop.Scroll;
import platform.interop.action.ClientAction;
import platform.interop.exceptions.ComplexQueryException;
import platform.interop.form.RemoteFormInterface;
import platform.server.auth.SecurityPolicy;
import platform.server.caches.ManualLazy;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.ConcreteValueClass;
import platform.server.classes.CustomClass;
import platform.server.classes.DataClass;
import platform.server.data.KeyField;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.where.EqualsWhere;
import platform.server.data.query.Query;
import platform.server.data.type.TypeSerializer;
import platform.server.data.where.Where;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LP;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyValueImplement;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;
import platform.server.session.SessionChanges;
import platform.server.view.form.client.RemoteFormView;
import platform.server.view.form.filter.Filter;
import platform.server.view.navigator.*;
import platform.server.view.navigator.filter.FilterNavigator;
import platform.server.view.navigator.filter.OrderViewNavigator;

import java.io.DataOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

// класс в котором лежит какие изменения произошли

// нужен какой-то объект который разделит клиента и серверную часть кинув каждому свои данные
// так клиента волнуют панели на форме, список гридов в привязке, дизайн и порядок представлений
// сервера колышет дерево и св-ва предст. с привязкой к объектам

public class RemoteForm<T extends BusinessLogics<T>> extends NoUpdateModifier {

    public final int viewID;

    final T BL;

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
        return getUpdateProperties(update(sessionChanges));
    }

    public Set<Property> getUpdateProperties(Modifier<? extends Changes> modifier) {
        Set<Property> properties = new HashSet<Property>();
        for(Property<?> updateProperty : getUpdateProperties())
            if(updateProperty.hasChanges(modifier))
                properties.add(updateProperty);
        return properties;
    }

    final FocusView<T> focusView;
    final CustomClassView classView;

    public final NavigatorForm<T> navigatorForm;

    public final Mapper mapper;

    public RemoteForm(NavigatorForm<T> navigatorForm, T BL, DataSession session, SecurityPolicy securityPolicy, FocusView<T> focusView, CustomClassView classView, PropertyObjectInterface computer, Map<ObjectNavigator, Object> mapObjects) throws SQLException {
        this.navigatorForm = navigatorForm;
        this.BL = BL;
        this.session = session;
        this.securityPolicy = securityPolicy;

        mapper = new Mapper(computer);

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
                group.addFilter(new RegularFilter(filter.ID, filter.filter.doMapping(mapper), filter.name, filter.key, filter.showKey));
            regularFilterGroups.add(group);
        }

        for (Entry<OrderViewNavigator, Boolean> navigatorOrder : navigatorForm.fixedOrders.entrySet()) {
            OrderView order = navigatorOrder.getKey().doMapping(mapper);
            order.getApplyObject().fixedOrders.put(order, navigatorOrder.getValue());
        }

        navigatorForm.onCreateForm(this);

        addObjectOnTransaction();

        for(Entry<ObjectNavigator, Object> mapObject : mapObjects.entrySet()) {
            ObjectImplement implement = mapper.mapObject(mapObject.getKey());
            Map<OrderView, Object> seeks = userGroupSeeks.get(implement.groupTo);
            if(seeks==null) {
                seeks = new HashMap<OrderView, Object>();
                userGroupSeeks.put(implement.groupTo, seeks);
            }
            seeks.put(implement, mapObject.getValue());
        }
    }

    public RemoteForm(NavigatorForm<T> navigatorForm, T BL, DataSession session, SecurityPolicy securityPolicy, FocusView<T> focusView, CustomClassView classView, PropertyObjectInterface computer) throws SQLException {
        this(navigatorForm, BL, session, securityPolicy, focusView, classView, computer, new HashMap<ObjectNavigator, Object>());
    }

    public List<GroupObjectImplement> groups = new ArrayList<GroupObjectImplement>();
    public Map<GroupObjectImplement,ViewTable> groupTables = new HashMap<GroupObjectImplement, ViewTable>();
    // собсно этот объект порядок колышет столько же сколько и дизайн представлений
    public List<PropertyView> properties = new ArrayList<PropertyView>();

    private Collection<ObjectImplement> objects;
    @ManualLazy
    public Collection<ObjectImplement> getObjects() {
        if(objects==null) {
            objects = new ArrayList<ObjectImplement>();
            for(GroupObjectImplement group : groups)
                for(ObjectImplement object : group.objects)
                    objects.add(object);
        }
        return objects;
    }

    // ----------------------------------- Поиск объектов по ID ------------------------------ //

    public GroupObjectImplement getGroupObjectImplement(int groupID) {
        for (GroupObjectImplement groupObject : groups)
            if (groupObject.ID == groupID)
                return groupObject;
        return null;
    }

    public ObjectImplement getObjectImplement(int objectID) {
        for (ObjectImplement object : getObjects())
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

    public PropertyView getPropertyView(Property<?> property) {
        for (PropertyView propView : properties)
            if (property.equals(propView.view.property))
                return propView;
        return null;
    }

    public PropertyView getPropertyView(LP<?> property) {
        return getPropertyView(property.property); 
    }

    public void serializePropertyEditorType(DataOutputStream outStream, PropertyView<?> propertyView) throws SQLException, IOException {

        PropertyValueImplement<?> change = propertyView.view.getChangeProperty();
        if(securityPolicy.property.change.checkPermission(change.property) && change.canBeChanged(this)) {
            outStream.writeBoolean(false);
            TypeSerializer.serialize(outStream,change.property.getType());
        } else
            outStream.writeBoolean(true);
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
        changeClassView(group, ClassViewType.switchView(group.curClassView));
    }

    public void changeClassView(GroupObjectImplement group,byte show) {

        group.curClassView = show;
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

    private DataObject createObject(ConcreteCustomClass cls) throws SQLException {

        if (!securityPolicy.cls.edit.add.checkPermission(cls)) return null;

        return session.addObject(cls,this);
    }

    private void resolveAddObjectImplement(CustomObjectImplement object, ConcreteCustomClass cls, DataObject addObject) throws SQLException {

        // резолвим все фильтры
        for(Filter filter : object.groupTo.getSetFilters())
            if(!Filter.ignoreInInterface || filter.isInInterface(object.groupTo)) // если ignoreInInterface проверить что в интерфейсе
                filter.resolveAdd(session, this, object, addObject);

        object.changeValue(session, addObject);

        // меняем вид, если при добавлении может получиться, что фильтр не выполнится, нужно как-то проверить в общем случае
//      changeClassView(object.groupTo, ClassViewType.PANEL);

        dataChanged = true;
    }

    // добавляет во все
    public DataObject addObject(ConcreteCustomClass cls) throws SQLException {

        DataObject addObject = createObject(cls);
        if (addObject == null) return addObject;

        for (ObjectImplement object : getObjects())
            if (object instanceof CustomObjectImplement && cls.isChild(((CustomObjectImplement)object).baseClass))
                resolveAddObjectImplement((CustomObjectImplement)object, cls, addObject);

        return addObject;
    }

    public DataObject addObject(CustomObjectImplement object, ConcreteCustomClass cls) throws SQLException {
        // пока тупо в базу

        DataObject addObject = createObject(cls);
        if (addObject == null) return addObject;

        resolveAddObjectImplement(object, cls, addObject);

        return addObject;
    }

    public void changeClass(CustomObjectImplement object, int classID) throws SQLException {
        changeClass(object, object.getDataObject(), classID);
    }

    public void changeClass(CustomObjectImplement object, DataObject change, int classID) throws SQLException {
        if (securityPolicy.cls.edit.change.checkPermission(object.currentClass)) {
            object.changeClass(session, change, classID);
            dataChanged = true;
        }
    }

    public boolean canChangeClass(CustomObjectImplement object) {
        return securityPolicy.cls.edit.change.checkPermission(object.currentClass);
    }

    public List<ClientAction> changeProperty(PropertyView<?> property, Object value) throws SQLException {
        return changeProperty(property.view, value, null);
    }

    public List<ClientAction> changeProperty(PropertyView<?> property, Object value, RemoteFormView executeForm, boolean all) throws SQLException {
        return changeProperty(property.view, value, executeForm, all ? property.toDraw : null);
    }

    public List<ClientAction> changeProperty(PropertyObjectImplement<?> property, Object value, RemoteFormView executeForm) throws SQLException {
        return changeProperty(property, value, executeForm, null);
    }

    public List<ClientAction> changeProperty(PropertyObjectImplement<?> property, Object value, RemoteFormView executeForm, GroupObjectImplement groupObject) throws SQLException {
        if (securityPolicy.property.change.checkPermission(property.property)) {
            dataChanged = true;
            // изменяем св-во
            return property.execute(session, value, this, executeForm, groupObject);
        } else {
            return null;
        }
    }

    // Обновление данных
    public void refreshData() throws SQLException {

        for(ObjectImplement object : getObjects())
            if(object instanceof CustomObjectImplement)
                ((CustomObjectImplement)object).refreshValueClass(session);
        refresh = true;
    }

    void addObjectOnTransaction() throws SQLException {
        for (ObjectImplement object : getObjects()) {
            if (object instanceof CustomObjectImplement) {
                CustomObjectImplement customObject = (CustomObjectImplement) object;
                if (customObject.addOnTransaction) {
                    addObject(customObject, (ConcreteCustomClass) customObject.gridClass);
                }
            }
            if (object.resetOnApply) {
                object.setDefaultValue(session);
            }
        }
    }

    public String checkChanges() throws SQLException {
        return session.check(BL);
    }

    // Применение изменений
    public String applyChanges() throws SQLException {
        String applyString = checkChanges();
        if(applyString==null)
            writeChanges();    
        return applyString;
    }

    public void rollbackChanges() throws SQLException {
        session.rollbackTransaction();
    }

    public void writeChanges() throws SQLException {
        session.write(BL);

        refreshData();
        addObjectOnTransaction();

        dataChanged = true; // временно пока applyChanges синхронен, для того чтобы пересылался факт изменения данных
    }

    public void cancelChanges() throws SQLException {
        session.restart(true);

        // пробежим по всем объектам
        for(ObjectImplement object : getObjects())
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

    public ConcreteCustomClass getObjectClass(ObjectImplement object) {

        if (!(object instanceof CustomObjectImplement))
            return null;

        return ((CustomObjectImplement)object).currentClass;
    }

    public Collection<Property> getUpdateProperties() {

        Set<Property> result = new HashSet<Property>();
        for(PropertyView<?> propView : properties)
            result.add(propView.view.property);
        for(GroupObjectImplement group : groups)
            group.fillUpdateProperties(result);
        return result;
    }

    public Collection<Property> getNoUpdateProperties() {
        return hintsNoUpdate;
    }

    public Collection<Property> hintsSave = new HashSet<Property>();

    public RemoteForm<T> createForm(NavigatorForm<T> form, Map<ObjectNavigator, DataObject> mapObjects) throws SQLException {
        return new RemoteForm<T>(form, BL, session, securityPolicy, focusView, classView, mapper.computer, DataObject.getMapValues(mapObjects));
    }

    public List<ClientAction> changeObject(ObjectImplement object, Object value, RemoteFormView form) throws SQLException {

        object.changeValue(session, value);

        // запускаем все Action'ы, которые следят за этим объектом
        return executeAutoActions(object, form);
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
            orderExprs.put(toOrder.getKey(),toOrder.getKey().getExpr(Collections.singleton(group), mapKeys, this));

        Set<KeyExpr> usedContext = null;
        if(readSize==1 && orderSeeks!=null && down) { // в частном случае если есть "висячие" ключи не в фильтре и нужна одна запись ставим равно вместо >
            usedContext = new HashSet<KeyExpr>();
            group.getFilterWhere(mapKeys, Collections.singleton(group), this).enumKeys(usedContext); // именно после ff'са
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

        return new Query<ObjectImplement,OrderView>(mapKeys,orderExprs,group.getWhere(mapKeys, Collections.singleton(group), this).and(down?orderWhere:orderWhere.not())).executeClasses(session, down?orders:Query.reverseOrder(orders), readSize, BL.baseClass);
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

    private void applyFilters() {
        for (GroupObjectImplement group : groups)
            group.filters = group.getSetFilters();
    }

    private void applyOrders() {
        for (GroupObjectImplement group : groups)
            group.orders = group.getSetOrders();
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

            Collection<PropertyView> panelProperties = new ArrayList<PropertyView>();
            Map<GroupObjectImplement,Collection<PropertyView>> groupProperties = new HashMap<GroupObjectImplement, Collection<PropertyView>>();

            for(PropertyView<?> drawProperty : properties) {

                // прогоняем через кэши чтобы каждый раз не запускать isInInterface
                boolean inGridInterface, inInterface;

                if (drawProperty.forcePanel) {
                    inGridInterface = false;
                } else {
                    if(refresh || classUpdated(drawProperty.view,drawProperty.toDraw)) {
                        inGridInterface = drawProperty.view.isInInterface(drawProperty.toDraw);
                        cacheInGridInterface.put(drawProperty, inGridInterface);
                    } else { // пусть будут assert
                        inGridInterface = cacheInGridInterface.get(drawProperty);
                        assert inGridInterface==drawProperty.view.isInInterface(drawProperty.toDraw);
                    }
                }

                if(drawProperty.toDraw==null)
                    inInterface = inGridInterface;
                else
                    if(refresh || classUpdated(drawProperty.view,null)) { // здесь еще можно вставить : что если inGridInterface и не null'ы
                        inInterface = drawProperty.view.isInInterface(null);
                        cacheInInterface.put(drawProperty, inInterface);
                    } else {
                        inInterface = cacheInInterface.get(drawProperty);
                        assert inInterface==drawProperty.view.isInInterface(null);
                    }

                if (drawProperty.toDraw != null && drawProperty.toDraw.curClassView == ClassViewType.HIDE) continue;
                
                boolean read = refresh || dataUpdated(drawProperty.view,changedProps) ||
                        drawProperty.toDraw!=null && (drawProperty.toDraw.updated & GroupObjectImplement.UPDATED_KEYS)!=0;
                if(inGridInterface && drawProperty.toDraw != null && drawProperty.toDraw.curClassView == ClassViewType.GRID) { // в grid'е
                    if(read || objectUpdated(drawProperty.view,drawProperty.toDraw)) {
                        Collection<PropertyView> propertyList = groupProperties.get(drawProperty.toDraw);
                        if(propertyList==null) {
                            propertyList = new ArrayList<PropertyView>();
                            groupProperties.put(drawProperty.toDraw,propertyList);
                        }
                        propertyList.add(drawProperty);
                        isDrawed.add(drawProperty);
                    }
                } else
                if(inInterface) { // в панели
                    if(read || objectUpdated(drawProperty.view,null)) {
                        panelProperties.add(drawProperty);
                        isDrawed.add(drawProperty);
                    }
                } else
                    if(isDrawed.remove(drawProperty))
                        result.dropProperties.add(drawProperty); // вкидываем удаление из интерфейса
            }

            if(panelProperties.size()>0) { // читаем "панельные" свойства
                Query<Object, PropertyView> selectProps = new Query<Object, PropertyView>(new HashMap<Object, KeyExpr>());
                for(PropertyView<?> drawProperty : panelProperties)
                    selectProps.properties.put(drawProperty, drawProperty.view.getExpr(null,null, this));

                Map<PropertyView,Object> resultProps = selectProps.execute(session).singleValue();
                for(PropertyView drawProp : panelProperties)
                    result.panelProperties.put(drawProp,resultProps.get(drawProp));
            }

            for(Entry<GroupObjectImplement, Collection<PropertyView>> mapGroup : groupProperties.entrySet()) { // читаем "табличные" свойства
                GroupObjectImplement group = mapGroup.getKey();
                Collection<PropertyView> groupList = mapGroup.getValue();

                Query<ObjectImplement, PropertyView> selectProps = new Query<ObjectImplement, PropertyView>(group);

                ViewTable keyTable = groupTables.get(mapGroup.getKey()); // ставим фильтр на то что только из viewTable'а
                selectProps.and(keyTable.joinAnd(BaseUtils.join(keyTable.mapKeys,selectProps.mapKeys)).getWhere());

                for(PropertyView<?> drawProperty : groupList)
                    selectProps.properties.put(drawProperty, drawProperty.view.getExpr(Collections.singleton(group), selectProps.mapKeys, this));

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

        if(dataChanged)
            result.dataChanged = session.changes.hasChanges();

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

    // возвращает какие объекты на форме показываются
    private Set<GroupObjectImplement> getPropertyGroups() {

        Set<GroupObjectImplement> reportObjects = new HashSet<GroupObjectImplement>();
        for (GroupObjectImplement group : groups)
            if (group.curClassView != ClassViewType.HIDE)
                reportObjects.add(group);

        return reportObjects;
    }

    // возвращает какие объекты на форме не фиксируются
    private Set<GroupObjectImplement> getClassGroups() {

        Set<GroupObjectImplement> reportObjects = new HashSet<GroupObjectImplement>();
        for (GroupObjectImplement group : groups)
            if (group.curClassView == ClassViewType.GRID)
                reportObjects.add(group);

        return reportObjects;
    }

    public FormData getFormData(boolean allProperties) throws SQLException {

        // вызовем endApply, чтобы быть полностью уверенным в том, что мы работаем с последними данными
        endApply();

        return getFormData((allProperties) ? new HashSet<GroupObjectImplement>(groups) : getPropertyGroups(), getClassGroups());
    }

    // считывает все данные с формы
    public FormData getFormData(Set<GroupObjectImplement> propertyGroups, Set<GroupObjectImplement> classGroups) throws SQLException {

        applyFilters();
        applyOrders();

        Collection<ObjectImplement> classObjects = new ArrayList<ObjectImplement>();
        for(GroupObjectImplement group : classGroups)
            classObjects.addAll(group.objects);

        // пока сделаем тупо получаем один большой запрос

        Query<ObjectImplement,Object> query = new Query<ObjectImplement,Object>(classObjects);
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
            if (propertyGroups.contains(property.view.getApplyObject()))
                query.properties.put(property, property.view.getExpr(classGroups, query.mapKeys, this));

        OrderedMap<Map<ObjectImplement, Object>, Map<Object, Object>> resultSelect = query.execute(session,queryOrders,0);
        for(Entry<Map<ObjectImplement, Object>, Map<Object, Object>> row : resultSelect.entrySet()) {
            Map<ObjectImplement,Object> groupValue = new HashMap<ObjectImplement, Object>();
            for(GroupObjectImplement group : groups)
                if (propertyGroups.contains(group))
                    for(ObjectImplement object : group.objects)
                        if (classObjects.contains(object))
                            groupValue.put(object,row.getKey().get(object));
                        else
                            groupValue.put(object,object.getObjectValue().getValue());

            Map<PropertyView,Object> propertyValues = new HashMap<PropertyView, Object>();
            for(PropertyView property : properties)
                if (propertyGroups.contains(property.toDraw))
                    propertyValues.put(property,row.getValue().get(property));

            result.add(groupValue,propertyValues);
        }

        return result;
    }

    public RemoteDialog<T> createClassPropertyDialog(int viewID, int value) throws RemoteException, SQLException {
        ClassNavigatorForm<T> classForm = new ClassNavigatorForm<T>(BL, getPropertyView(viewID).view.getDialogClass());
        return new RemoteDialog<T>(classForm, BL, session, securityPolicy, focusView, classView, classForm.object, mapper.computer, value);
    }

    public RemoteDialog<T> createEditorPropertyDialog(int viewID) throws SQLException {
        PropertyValueImplement<?> change = getPropertyView(viewID).view.getChangeProperty();
        DataChangeNavigatorForm<T> navigatorForm = new DataChangeNavigatorForm<T>(BL, change.getDialogClass(session), change);
        return new RemoteDialog<T>(navigatorForm, BL, session, securityPolicy, focusView, classView, navigatorForm.object, mapper.computer, change.read(session, this));
    }

    public RemoteDialog<T> createObjectDialog(int objectID) throws SQLException {
        CustomObjectImplement objectImplement = (CustomObjectImplement) getObjectImplement(objectID);
        ClassNavigatorForm<T> classForm = new ClassNavigatorForm<T>(BL, objectImplement.baseClass);
        if(objectImplement.currentClass!=null)
            return new RemoteDialog<T>(classForm, BL, session, securityPolicy, focusView, classView, classForm.object, mapper.computer, objectImplement.getObjectValue().getValue());
        else
            return new RemoteDialog<T>(classForm, BL, session, securityPolicy, focusView, classView, classForm.object, mapper.computer);
    }

    public RemoteDialog<T> createObjectDialog(int objectID, int value) throws RemoteException {
        try {
            CustomObjectImplement objectImplement = (CustomObjectImplement) getObjectImplement(objectID);
            ClassNavigatorForm<T> classForm = new ClassNavigatorForm<T>(BL, objectImplement.baseClass);
            return new RemoteDialog<T>(classForm, BL, session, securityPolicy, focusView, classView, classForm.object, mapper.computer, value);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<ClientAction> executeAutoActions(ObjectImplement object, RemoteFormView form) throws SQLException {

        List<ClientAction> actions = new ArrayList<ClientAction>();
        for (Entry<ObjectNavigator, List<PropertyObjectNavigator>> autoActions : navigatorForm.autoActions.entrySet())
            if (object.equals(mapper.mapObject(autoActions.getKey())))
                for(PropertyObjectNavigator autoAction : autoActions.getValue()) {
                    PropertyObjectImplement action = mapper.mapProperty(autoAction);
                    if(action.isInInterface(null)) {
                        List<ClientAction> change = changeProperty(action, action.getChangeProperty().read(session, this)==null?true:null, form);
                        if (change != null) {
                            actions.addAll(change);
                        }
                    }
                }
        return actions;
    }
}

