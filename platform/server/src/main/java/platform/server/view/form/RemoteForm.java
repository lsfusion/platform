/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platform.server.view.form;

import platform.interop.Compare;
import platform.interop.form.RemoteFormInterface;
import platform.server.view.form.FormData;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.query.Join;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.wheres.CompareWhere;
import platform.server.logics.BusinessLogics;
import platform.server.logics.ObjectValue;
import platform.server.logics.auth.SecurityPolicy;
import platform.server.logics.classes.IntegralClass;
import platform.server.logics.classes.ObjectClass;
import platform.server.logics.classes.RemoteClass;
import platform.server.logics.data.IDTable;
import platform.server.logics.properties.DataProperty;
import platform.server.logics.properties.Property;
import platform.server.logics.properties.PropertyInterface;
import platform.server.logics.session.*;
import platform.server.where.Where;

import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

// класс в котором лежит какие изменения произошли

// нужен какой-то объект который разделит клиента и серверную часть кинув каждому свои данные
// так клиента волнуют панели на форме, список гридов в привязке, дизайн и порядок представлений
// сервера колышет дерево и св-ва предст. с привязкой к объектам

public class RemoteForm<T extends BusinessLogics<T>> implements PropertyUpdateView {

    // используется для записи в сессии изменений в базу - требуется глобально уникальный идентификатор
    private final int GID;
    public int getGID() { return GID; }

    private int getGroupObjectGID(GroupObjectImplement group) { return GID * RemoteFormInterface.GID_SHIFT + group.ID; }

    private final int ID;
    public int getID() { return ID; }

    T BL;

    public DataSession session;

    SecurityPolicy securityPolicy;

    protected RemoteForm(int iID, T iBL, DataSession iSession, SecurityPolicy isecurityPolicy) throws SQLException {

        ID = iID;
        BL = iBL;
        session = iSession;
        securityPolicy = isecurityPolicy;

        structUpdated = true;

        GID = BL.tableFactory.idTable.generateID(session, IDTable.FORM);
    }

    public List<GroupObjectImplement> groups = new ArrayList<GroupObjectImplement>();
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

    public ChangeValue getPropertyEditorObjectValue(PropertyView propertyView, boolean externalID) throws SQLException {

        ChangeValue changeValue = propertyView.view.getChangeProperty(session, securityPolicy.property.change);
        if (!externalID) return changeValue;

        if (changeValue == null) return null;
        DataProperty propertyID = changeValue.changeClass.getExternalID();
        if (propertyID == null) return null;

        return new ChangeObjectValue(propertyID.value, null);
    }

    // ----------------------------------- Навигация ----------------------------------------- //

    // поиски по свойствам\объектам
    public Map<PropertyObjectImplement,Object> userPropertySeeks = new HashMap<PropertyObjectImplement, Object>();
    public Map<ObjectImplement,Integer> userObjectSeeks = new HashMap<ObjectImplement, Integer>();

    private Map<GroupObjectImplement, Integer> pendingGroupChanges = new HashMap<GroupObjectImplement, Integer>();

    public void changeGroupObject(GroupObjectImplement group, int changeType) throws SQLException {
        pendingGroupChanges.put(group, changeType);
    }

    public void changeGroupObject(GroupObjectImplement group, GroupObjectValue value) throws SQLException {
        // проставим все объектам метки изменений
        for(ObjectImplement object : group)
            changeObject(object, value.get(object));
    }

    public void changeObject(ObjectImplement object, Integer value) throws SQLException {

        if ((object.idObject==null && value==null) || (object.idObject!=null && object.idObject.equals(value))) return;

        object.idObject = value;

        // запишем класс объекта
        RemoteClass objectClass = null;
        if (value != null) {
            if(object.baseClass instanceof ObjectClass)
                objectClass = session.getObjectClass(value);
            else
                objectClass = object.baseClass;
        }

        if(object.objectClass != objectClass) {

            object.objectClass = objectClass;

            object.updated = object.updated | ObjectImplement.UPDATED_CLASS;
        }

        object.updated = object.updated | ObjectImplement.UPDATED_OBJECT;
        object.groupTo.updated = object.groupTo.updated | GroupObjectImplement.UPDATED_OBJECT;

        // сообщаем всем, кто следит
        // если object.Class == null, то значит объект удалили
        if (object.objectClass != null)
            objectChanged(object.objectClass, value);
    }

    public void changeGridClass(ObjectImplement Object,Integer idClass) throws SQLException {

        RemoteClass GridClass = BL.objectClass.findClassID(idClass);
        if(Object.gridClass == GridClass) return;

        if(GridClass==null) throw new RuntimeException();
        Object.gridClass = GridClass;

        // расставляем пометки
        Object.updated = Object.updated | ObjectImplement.UPDATED_GRIDCLASS;
        Object.groupTo.updated = Object.groupTo.updated | GroupObjectImplement.UPDATED_GRIDCLASS;

    }

    public void switchClassView(GroupObjectImplement group) {
        changeClassView(group, !group.gridClassView);
    }

    private void changeClassView(GroupObjectImplement group,boolean show) {

        if(group.gridClassView == show || group.singleViewType) return;
        group.gridClassView = show;

        // расставляем пометки
        group.updated = group.updated | GroupObjectImplement.UPDATED_CLASSVIEW;

        // на данный момент ClassView влияет на фильтры
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

    public void addObject(ObjectImplement object, RemoteClass cls) throws SQLException {
        // пока тупо в базу

        if (!securityPolicy.cls.edit.add.checkPermission(cls)) return;

        Integer addID = BL.addObject(session, cls);

        boolean foundConflict = false;

        // берем все текущие CompareFilter на оператор 0(=) делаем ChangeProperty на ValueLink сразу в сессию
        // тогда добавляет для всех других объектов из того же GroupObjectImplement'а, значение ValueLink, GetValueExpr
        for(Filter<?> filter : object.groupTo.filters) {
            if(filter.compare ==0) {
                JoinQuery<ObjectImplement,String> subQuery = new JoinQuery<ObjectImplement,String>(filter.property.mapping.values());
                Map<ObjectImplement,Integer> fixedObjects = new HashMap<ObjectImplement, Integer>();
                for(ObjectImplement SibObject : filter.property.mapping.values()) {
                    if(SibObject.groupTo !=object.groupTo) {
                        fixedObjects.put(SibObject,SibObject.idObject);
                    } else {
                        if(SibObject!=object) {
                            Join<KeyField,PropertyField> ObjectJoin = new Join<KeyField,PropertyField>(BL.tableFactory.objectTable.getClassJoin(SibObject.gridClass));
                            ObjectJoin.joins.put(BL.tableFactory.objectTable.key,subQuery.mapKeys.get(SibObject));
                            subQuery.and(ObjectJoin.inJoin);
                        } else
                            fixedObjects.put(SibObject,addID);
                    }
                }

                subQuery.putKeyWhere(fixedObjects);

                subQuery.properties.put("newvalue", filter.value.getValueExpr(object.groupTo.getClassGroup(),subQuery.mapKeys, session, filter.property.property.getType()));

                LinkedHashMap<Map<ObjectImplement,Integer>,Map<String,Object>> Result = subQuery.executeSelect(session);
                // изменяем св-ва
                for(Entry<Map<ObjectImplement,Integer>,Map<String,Object>> row : Result.entrySet()) {
                    Property changeProperty = filter.property.property;
                    Map<PropertyInterface,ObjectValue> keys = new HashMap<PropertyInterface, ObjectValue>();
                    for(PropertyInterface propertyInterface : (Collection<PropertyInterface>)changeProperty.interfaces) {
                        ObjectImplement changeObject = filter.property.mapping.get(propertyInterface);
                        keys.put(propertyInterface,new ObjectValue(row.getKey().get(changeObject),changeObject.gridClass));
                    }
                    changeProperty.changeProperty(keys,row.getValue().get("newvalue"), false, session, null);
                }
            } else {
                if (object.groupTo.equals(filter.getApplyObject())) foundConflict = true;
            }
        }

        for (PropertyView prop : orders.keySet()) {
            if (object.groupTo.equals(prop.toDraw)) foundConflict = true;
        }

        changeObject(object, addID);

        // меняем вид, если при добавлении может получиться, что фильтр не выполнится
        if (foundConflict) {
            changeClassView(object.groupTo, false);
        }

        dataChanged = true;
    }

    public void changeClass(ObjectImplement object, RemoteClass cls) throws SQLException {

        // проверка, что разрешено удалять объекты
        if (cls == null) {
            if (!securityPolicy.cls.edit.remove.checkPermission(object.objectClass)) return;
        } else {
            if (!(securityPolicy.cls.edit.remove.checkPermission(object.objectClass) || securityPolicy.cls.edit.change.checkPermission(object.objectClass))) return;
            if (!(securityPolicy.cls.edit.add.checkPermission(cls) || securityPolicy.cls.edit.change.checkPermission(cls))) return;
        }

        BL.changeClass(session, object.idObject,cls);

        // Если объект удалили, то сбрасываем текущий объект в null
        if (cls == null) {
            changeObject(object, null);
        }

        object.updated = object.updated | ObjectImplement.UPDATED_CLASS;

        dataChanged = true;
    }

    public void changePropertyView(PropertyView property, Object value, boolean externalID) throws SQLException {
        changeProperty(property.view, value, externalID);
    }

    private void changeProperty(PropertyObjectImplement property, Object value, boolean externalID) throws SQLException {

        // изменяем св-во
        property.property.changeProperty(fillPropertyInterface(property), value, externalID, session, securityPolicy.property.change);

        dataChanged = true;
    }

    // Обновление данных
    public void refreshData() {

        for(GroupObjectImplement Group : groups) {
            Group.updated |= GroupObjectImplement.UPDATED_GRIDCLASS;
        }
    }

    // Применение изменений
    public String saveChanges() throws SQLException {
        return BL.apply(session);
    }

    public void cancelChanges() throws SQLException {
        session.restart(true);

        dataChanged = true;
    }

    // ------------------ Через эти методы сообщает верхним объектам об изменениях ------------------- //

    // В дальнейшем наверное надо будет переделать на Listener'ы...
    protected void objectChanged(RemoteClass cls, Integer objectID) {}

    public void changePageSize(GroupObjectImplement groupObject, int pageSize) {

        groupObject.pageSize = pageSize;

        structUpdated = true;
    }

    public void gainedFocus() {
        dataChanged = true;
    }

    void close() throws SQLException {

        session.incrementChanges.remove(this);
        for(GroupObjectImplement Group : groups) {
            ViewTable DropTable = BL.tableFactory.viewTables.get(Group.size()-1);
            DropTable.dropViewID(session, getGroupObjectGID(Group));
        }
    }

    // --------------------------------------------------------------------------------------- //
    // --------------------- Общение в обратную сторону с ClientForm ------------------------- //
    // --------------------------------------------------------------------------------------- //

    private Map<PropertyInterface,ObjectValue> fillPropertyInterface(PropertyObjectImplement<?> property) {

        Property changeProperty = property.property;
        Map<PropertyInterface,ObjectValue> keys = new HashMap<PropertyInterface, ObjectValue>();
        for(PropertyInterface propertyInterface : (Collection<PropertyInterface>)changeProperty.interfaces) {
            ObjectImplement object = property.mapping.get(propertyInterface);
            keys.put(propertyInterface,new ObjectValue(object.idObject,object.objectClass));
        }

        return keys;
    }

    // рекурсия для генерации порядка
    private Where generateOrderWheres(List<SourceExpr> orderSources,List<Object> orderWheres,List<Boolean> orderDirs,boolean down,int index) {

        SourceExpr orderExpr = orderSources.get(index);
        Object orderValue = orderWheres.get(index);
        if(orderValue==null) orderValue = orderExpr.getType().getEmptyValue();
        boolean last = !(index +1< orderSources.size());

        int compareIndex;
        if (orderDirs.get(index)) {
            if (down) {
                if (last)
                    compareIndex = Compare.LESS_EQUALS;
                else
                    compareIndex = Compare.LESS;
            } else
                compareIndex = Compare.GREATER;
        } else {
            if (down) {
                if (last)
                    compareIndex = Compare.GREATER_EQUALS;
                else
                    compareIndex = Compare.GREATER;
            } else
                compareIndex = Compare.LESS;
        }
        Where orderWhere = new CompareWhere(orderExpr,orderExpr.getType().getExpr(orderValue),compareIndex);

        if(!last) // >A OR (=A AND >B)
            return new CompareWhere(orderExpr,orderExpr.getType().getExpr(orderValue), Compare.EQUALS).
                    and(generateOrderWheres(orderSources, orderWheres, orderDirs, down, index +1)).or(orderWhere);
        else
            return orderWhere;
    }

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
    public boolean toSave(Property Property) {
        return hintsSave.contains(Property);
    }

    public boolean hasSessionChanges() {
        return session.hasChanges();
    }

    // транзакция для отката при exception'ах
    private class ApplyTransaction {

        private class Group {

            private class Object {
                ObjectImplement object;
                Integer idObject;
                int updated;
                RemoteClass objectClass;

                private Object(ObjectImplement iObject) {
                    object = iObject;
                    idObject = object.idObject;
                    updated = object.updated;
                    objectClass = object.objectClass;
                }

                void rollback() {
                    object.idObject = idObject;
                    object.updated = updated;
                    object.objectClass = objectClass;
                }
            }

            GroupObjectImplement group;
            Set<Filter> filters;
            LinkedHashMap<PropertyObjectImplement,Boolean> orders;
            boolean upKeys,downKeys;
            List<GroupObjectValue> keys;
            // какие ключи активны
            Map<GroupObjectValue,Map<PropertyObjectImplement, java.lang.Object>> keyOrders;
            int updated;

            Collection<Object> objects = new ArrayList<Object>();

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
                int groupGID = getGroupObjectGID(group);
                ViewTable insertTable = BL.tableFactory.viewTables.get(group.size()-1);
                insertTable.dropViewID(session, groupGID);
                for(GroupObjectValue keyRow : group.keys) {
                    // закинем сразу ключи для св-в чтобы Join'ить
                    Map<KeyField,Integer> viewKeyInsert = new HashMap<KeyField, Integer>();
                    viewKeyInsert.put(insertTable.view,groupGID);
                    // важен правильный порядок в KeyRow
                    ListIterator<KeyField> ivk = insertTable.objects.listIterator();
                    for(ObjectImplement objectKey : group)
                        viewKeyInsert.put(ivk.next(), keyRow.get(objectKey));
                    session.insertRecord(insertTable,viewKeyInsert,new HashMap<PropertyField, java.lang.Object>());
                }                
            }
        }

        Collection<Group> groups = new ArrayList<Group>();
        Map<PropertyView,Boolean> interfacePool;

        Map<Property, Property.Change> propertyChanges;
        Map<PropertyUpdateView, DataChanges> incrementChanges;

        ApplyTransaction() {
            for(GroupObjectImplement group : RemoteForm.this.groups)
                groups.add(new Group(group));
            interfacePool = new HashMap<PropertyView, Boolean>(RemoteForm.this.interfacePool);

            if(dataChanged) {
                incrementChanges = new HashMap<PropertyUpdateView, DataChanges>(session.incrementChanges);
                propertyChanges = new HashMap<Property, Property.Change>(session.propertyChanges);
            }
        }

        void rollback() throws SQLException {
            for(Group group : groups)
                group.rollback();
            RemoteForm.this.interfacePool = interfacePool;

            if(dataChanged) {
                session.incrementChanges = incrementChanges;
                session.propertyChanges = propertyChanges;
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
            Collection<RemoteClass> changedClasses = new HashSet<RemoteClass>();
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
                        setOrderChanged = (order.view.isInInterface(group)?setOrders.add(order.view):group.orders.remove(order.view));
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
                        if(changedClasses.contains(object.gridClass)) {updateKeys = true; break;}
                }

                // по возврастанию (0), убыванию (1), центру (2) и откуда начинать
                Map<PropertyObjectImplement,Object> propertySeeks = new HashMap<PropertyObjectImplement, Object>();

                // объект на который будет делаться активным после нахождения ключей
                GroupObjectValue currentObject = group.getObjectValue();

                // объект относительно которого будет устанавливаться фильтр
                GroupObjectValue objectSeeks = group.getObjectValue();
                int direction;
                boolean hasMoreKeys = true;

                if (objectSeeks.containsValue(null)) {
                    objectSeeks = new GroupObjectValue();
                    direction = DIRECTION_DOWN;
                } else
                    direction = DIRECTION_CENTER;

                // Различные переходы - в самое начало или конец
                Integer pendingChanges = pendingGroupChanges.get(group);
                if (pendingChanges == null) pendingChanges = -1;

                if (pendingChanges == RemoteFormInterface.CHANGEGROUPOBJECT_FIRSTROW) {
                    objectSeeks = new GroupObjectValue();
                    currentObject = null;
                    updateKeys = true;
                    hasMoreKeys = false;
                    direction = DIRECTION_DOWN;
                }

                if (pendingChanges == RemoteFormInterface.CHANGEGROUPOBJECT_LASTROW) {
                    objectSeeks = new GroupObjectValue();
                    currentObject = null;
                    updateKeys = true;
                    hasMoreKeys = false;
                    direction = DIRECTION_UP;
                }

                // один раз читаем не так часто делается, поэтому не будем как с фильтрами
                for(PropertyObjectImplement Property : userPropertySeeks.keySet()) {
                    if(Property.getApplyObject()== group) {
                        propertySeeks.put(Property, userPropertySeeks.get(Property));
                        currentObject = null;
                        updateKeys = true;
                        direction = DIRECTION_CENTER;
                    }
                }
                for(ObjectImplement object : userObjectSeeks.keySet()) {
                    if(object.groupTo == group) {
                        objectSeeks.put(object, userObjectSeeks.get(object));
                        currentObject.put(object, userObjectSeeks.get(object));
                        updateKeys = true;
                        direction = DIRECTION_CENTER;
                    }
                }

                if(!updateKeys && (group.updated & GroupObjectImplement.UPDATED_CLASSVIEW) !=0) {
                   // изменился "классовый" вид перечитываем св-ва
                    objectSeeks = group.getObjectValue();
                    updateKeys = true;
                    direction = DIRECTION_CENTER;
                }

                if(!updateKeys && group.gridClassView && (group.updated & GroupObjectImplement.UPDATED_OBJECT)!=0) {
                    // листание - объекты стали близки к краю (object не далеко от края - надо хранить список не базу же дергать) - изменился объект
                    int keyNum = group.keys.indexOf(group.getObjectValue());
                    // если меньше PageSize осталось и сверху есть ключи
                    if(keyNum< group.pageSize && group.upKeys) {
                        direction = DIRECTION_UP;
                        updateKeys = true;

                        int lowestInd = group.pageSize *2-1;
                        if (lowestInd >= group.keys.size()) {
                            objectSeeks = new GroupObjectValue();
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
                                objectSeeks = new GroupObjectValue();
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
                    for(ObjectImplement object : group)
                        if(objectSeeks.get(object)==null && object.baseClass instanceof IntegralClass && !group.gridClassView)
                            objectSeeks.put(object,1);

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
                        group.fillSourceSelect(selectKeys, group.getClassGroup(),BL.tableFactory, session);
                        for(Entry<PropertyObjectImplement,Object> property : propertySeeks.entrySet())
                            selectKeys.and(new CompareWhere(property.getKey().getSourceExpr(group.getClassGroup(),selectKeys.mapKeys, session),
                                    property.getKey().property.getType().getExpr(property.getValue()), Compare.EQUALS));

                        // докидываем найденные ключи
                        LinkedHashMap<Map<ObjectImplement,Integer>,Map<Object,Object>> resultKeys = selectKeys.executeSelect(session);
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
                            group.fillSourceSelect(selectKeys, group.getClassGroup(),BL.tableFactory, session);
                            LinkedHashMap<Map<ObjectImplement,Integer>,Map<Object,Object>> resultKeys = selectKeys.executeSelect(session,new LinkedHashMap<Object,Boolean>(),1);
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
                            JoinQuery<ObjectImplement, PropertyObjectImplement> orderQuery = new JoinQuery<ObjectImplement, PropertyObjectImplement>(objectSeeks.keySet());
                            orderQuery.putKeyWhere(objectSeeks);

                            for(PropertyObjectImplement order : group.orders.keySet())
                                orderQuery.properties.put(order, order.getSourceExpr(group.getClassGroup(),orderQuery.mapKeys, session));

                            LinkedHashMap<Map<ObjectImplement,Integer>,Map<PropertyObjectImplement,Object>> resultOrders = orderQuery.executeSelect(session);
                            for(PropertyObjectImplement order : group.orders.keySet())
                                propertySeeks.put(order,resultOrders.values().iterator().next().get(order));
                        }

                        LinkedHashMap<Object,Boolean> selectOrders = new LinkedHashMap<Object, Boolean>();
                        JoinQuery<ObjectImplement,Object> selectKeys = new JoinQuery<ObjectImplement,Object>(group); // object потому как нужно еще по ключам упорядочивать, а их тогда надо в св-ва кидать
                        group.fillSourceSelect(selectKeys, group.getClassGroup(),BL.tableFactory, session);

                        // складываются источники и значения
                        List<SourceExpr> orderSources = new ArrayList<SourceExpr>();
                        List<Object> orderWheres = new ArrayList<Object>();
                        List<Boolean> orderDirs = new ArrayList<Boolean>();

                        // закинем порядки (с LEFT JOIN'ом)
                        for(Entry<PropertyObjectImplement,Boolean> toOrder : group.orders.entrySet()) {
                            SourceExpr orderExpr = toOrder.getKey().getSourceExpr(group.getClassGroup(), selectKeys.mapKeys, session);
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
                        for(Entry<ObjectImplement, Integer> objectSeek : objectSeeks.entrySet()) {
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
                        LinkedHashMap<Map<ObjectImplement,Integer>,Map<Object,Object>> keyResult = new LinkedHashMap<Map<ObjectImplement, Integer>, Map<Object, Object>>();

                        int readSize = group.pageSize *3/(direction ==DIRECTION_CENTER?2:1);

                        JoinQuery<ObjectImplement,Object> copySelect = null;
                        if(direction ==DIRECTION_CENTER)
                            copySelect = new JoinQuery<ObjectImplement, Object>(selectKeys);
                        // откопируем в сторону запрос чтобы еще раз потом использовать
                        // сначала Descending загоним
                        group.downKeys = false;
                        group.upKeys = false;
                        if(direction ==DIRECTION_UP || direction ==DIRECTION_CENTER) {
                            if(orderSources.size()>0) {
                                selectKeys.and(generateOrderWheres(orderSources,orderWheres,orderDirs,false,0));
                                group.downKeys = hasMoreKeys;
                            }

    //                        System.out.println(group + " KEYS UP ");
    //                        selectKeys.outSelect(session,JoinQuery.reverseOrder(selectOrders),readSize);
                            LinkedHashMap<Map<ObjectImplement,Integer>,Map<Object,Object>> execResult = selectKeys.executeSelect(session,JoinQuery.reverseOrder(selectOrders), readSize);
                            ListIterator<Map<ObjectImplement,Integer>> ik = (new ArrayList<Map<ObjectImplement,Integer>>(execResult.keySet())).listIterator();
                            while(ik.hasNext()) ik.next();
                            while(ik.hasPrevious()) {
                                Map<ObjectImplement,Integer> row = ik.previous();
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
                                selectKeys.and(generateOrderWheres(orderSources,orderWheres,orderDirs,true,0));
                                if(direction !=DIRECTION_CENTER) group.upKeys = hasMoreKeys;
                            }

    //                        System.out.println(group + " KEYS DOWN ");
    //                        selectKeys.outSelect(session,selectOrders,readSize);
                            LinkedHashMap<Map<ObjectImplement,Integer>,Map<Object,Object>> executeList = selectKeys.executeSelect(session, selectOrders, readSize);
    //                        if((OrderSources.size()==0 || Direction==2) && ExecuteList.size()>0) ActiveRow = KeyResult.size();
                            keyResult.putAll(executeList);
                            group.downKeys = (executeList.size()== readSize);

                            if ((direction == DIRECTION_DOWN || activeRow == -1) && keyResult.size() > 0)
                                activeRow = 0;
                        }

                        group.keys = new ArrayList<GroupObjectValue>();
                        group.keyOrders = new HashMap<GroupObjectValue, Map<PropertyObjectImplement, Object>>();

                        // параллельно будем обновлять ключи чтобы Join'ить

                        int groupGID = getGroupObjectGID(group);
                        ViewTable insertTable = BL.tableFactory.viewTables.get(group.size()-1);
                        insertTable.dropViewID(session, groupGID);
                        session.useTemporaryTable(insertTable);

                        for(Entry<Map<ObjectImplement,Integer>,Map<Object,Object>> resultRow : keyResult.entrySet()) {
                            GroupObjectValue keyRow = new GroupObjectValue();
                            Map<PropertyObjectImplement,Object> orderRow = new HashMap<PropertyObjectImplement, Object>();

                            // закинем сразу ключи для св-в чтобы Join'ить
                            Map<KeyField,Integer> viewKeyInsert = new HashMap<KeyField, Integer>();
                            viewKeyInsert.put(insertTable.view,groupGID);
                            ListIterator<KeyField> ivk = insertTable.objects.listIterator();

                            // важен правильный порядок в KeyRow
                            for(ObjectImplement objectKey : group) {
                                Integer keyValue = resultRow.getKey().get(objectKey);
                                keyRow.put(objectKey,keyValue);
                                viewKeyInsert.put(ivk.next(), keyValue);
                            }
                            session.insertRecord(insertTable,viewKeyInsert,new HashMap<PropertyField, Object>());

                            for(PropertyObjectImplement toOrder : group.orders.keySet())
                                orderRow.put(toOrder,resultRow.getValue().get(toOrder));

                            group.keys.add(keyRow);
                            group.keyOrders.put(keyRow, orderRow);
                        }

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
                            GroupObjectValue newValue = group.keys.get(activeRow);
    //                        if (!newValue.equals(Group.GetObjectValue())) {
                                result.objects.put(group,newValue);
                                changeGroupObject(group,newValue);
    //                        }
                        } else
                            changeGroupObject(group,new GroupObjectValue());
                    }
                }
            }

            Collection<PropertyView> panelProps = new ArrayList<PropertyView>();
            Map<GroupObjectImplement,Collection<PropertyView>> groupProps = new HashMap<GroupObjectImplement, Collection<PropertyView>>();

//        PanelProps.

            for(PropertyView<?> drawProp : properties) {

                // 3 признака : перечитать, (возможно класс изменился, возможно объектный интерфейс изменился - чисто InterfacePool)
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
                        if((object.updated & ObjectImplement.UPDATED_OBJECT)!=0) {
                            // изменился верхний объект, перечитываем
                            read = true;
                            if((object.updated & ObjectImplement.UPDATED_CLASS)!=0) {
                                // изменился класс объекта перепроверяем все
                                if(drawProp.toDraw !=null) checkClass = true;
                                checkObject = true;
                            }
                        }
                    } else {
                        // изменился объект и св-во не было классовым
                        if((object.updated & ObjectImplement.UPDATED_OBJECT)!=0 && (inInterface !=2 || !drawProp.toDraw.gridClassView)) {
                            read = true;
                            // изменися класс объекта
                            if((object.updated & ObjectImplement.UPDATED_CLASS)!=0) checkObject = true;
                        }
                        // изменение общего класса
                        if((object.updated & ObjectImplement.UPDATED_GRIDCLASS)!=0) checkClass = true;

                    }
                }

                // обновим InterfacePool, было в InInterface
                if(checkClass || checkObject) {
                    int newInInterface=0;
                    if(checkClass)
                        newInInterface = (drawProp.view.isInInterface(drawProp.toDraw)?2:0);
                    if((checkObject && !(checkClass && newInInterface==2)) || (checkClass && newInInterface==0 )) // && InInterface==2))
                        newInInterface = (drawProp.view.isInInterface(null)?1:0);

                    if(inInterface !=newInInterface) {
                        inInterface = newInInterface;

                        if(inInterface ==0) {
                            interfacePool.remove(drawProp);
                            // !!! СЮДА НАДО ВКИНУТЬ УДАЛЕНИЕ ИЗ ИНТЕРФЕЙСА
                            result.dropProperties.add(drawProp);
                        }
                        else
                            interfacePool.put(drawProp, inInterface ==2);
                    }
                }

                if(!read && (dataChanged && changedProps.contains(drawProp.view.property)))
                    read = true;

                if (!read && dataChanged) {
                    for (ObjectImplement object : drawProp.view.mapping.values()) {
                        if (changedClasses.contains(object.baseClass)) {
                            read = true;
                            break;
                        }
                    }
                }

                if(inInterface >0 && read) {
                    if(inInterface ==2 && drawProp.toDraw.gridClassView) {
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
                JoinQuery<Object, PropertyView> selectProps = new JoinQuery<Object, PropertyView>(new ArrayList<Object>());
                for(PropertyView drawProp : panelProps)
                    selectProps.properties.put(drawProp, drawProp.view.getSourceExpr(null,null, session));

                Map<PropertyView,Object> resultProps = selectProps.executeSelect(session).values().iterator().next();
                for(PropertyView drawProp : panelProps)
                    result.panelProperties.put(drawProp,resultProps.get(drawProp));
            }

            for(Entry<GroupObjectImplement, Collection<PropertyView>> mapGroup : groupProps.entrySet()) {
                GroupObjectImplement group = mapGroup.getKey();
                Collection<PropertyView> groupList = mapGroup.getValue();

                JoinQuery<ObjectImplement, PropertyView> selectProps = new JoinQuery<ObjectImplement, PropertyView>(group);

                ViewTable keyTable = BL.tableFactory.viewTables.get(group.size()-1);
                Join<KeyField, PropertyField> keyJoin = new Join<KeyField,PropertyField>(keyTable);

                ListIterator<KeyField> ikt = keyTable.objects.listIterator();
                for(ObjectImplement object : group)
                    keyJoin.joins.put(ikt.next(), selectProps.mapKeys.get(object));
                keyJoin.joins.put(keyTable.view,keyTable.view.type.getExpr(getGroupObjectGID(group)));
                selectProps.and(keyJoin.inJoin);

                for(PropertyView drawProp : groupList)
                    selectProps.properties.put(drawProp, drawProp.view.getSourceExpr(group.getClassGroup(), selectProps.mapKeys, session));

                LinkedHashMap<Map<ObjectImplement,Integer>,Map<PropertyView,Object>> resultProps = selectProps.executeSelect(session);

                for(PropertyView drawProp : groupList) {
                    Map<GroupObjectValue,Object> propResult = new HashMap<GroupObjectValue, Object>();
                    result.gridProperties.put(drawProp,propResult);

                    for(Entry<Map<ObjectImplement,Integer>,Map<PropertyView,Object>> resultRow : resultProps.entrySet())
                        propResult.put(new GroupObjectValue(resultRow.getKey()),resultRow.getValue().get(drawProp));
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

//        Result.Out(this);

            return result;
        } catch (RuntimeException e) {
            transaction.rollback();
            throw e;
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

        JoinQuery<ObjectImplement,Object> query = new JoinQuery<ObjectImplement,Object>(readObjects);
        LinkedHashMap<Object,Boolean> queryOrders = new LinkedHashMap<Object, Boolean>();

        for (GroupObjectImplement group : groups) {

            if (classGroups.contains(group)) {

                // не фиксированные ключи
                group.fillSourceSelect(query,classGroups,BL.tableFactory, session);

                // закинем Order'ы
                for(Map.Entry<PropertyObjectImplement,Boolean> order : group.orders.entrySet()) {
                    query.properties.put(order.getKey(),order.getKey().getSourceExpr(classGroups, query.mapKeys, session));
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
            query.properties.put(property, property.view.getSourceExpr(classGroups, query.mapKeys, session));

        LinkedHashMap<Map<ObjectImplement,Integer>,Map<Object,Object>> resultSelect = query.executeSelect(session,queryOrders,0);
        for(Entry<Map<ObjectImplement,Integer>,Map<Object,Object>> row : resultSelect.entrySet()) {
            Map<ObjectImplement,Integer> groupValue = new HashMap<ObjectImplement, Integer>();
            for(GroupObjectImplement group : groups)
                for(ObjectImplement object : group) {
                    if (readObjects.contains(object))
                        groupValue.put(object,row.getKey().get(object));
                    else
                        groupValue.put(object,object.idObject);
                }

            Map<PropertyView,Object> propertyValues = new HashMap<PropertyView, Object>();
            for(PropertyView property : properties)
                propertyValues.put(property,row.getValue().get(property));

            result.add(groupValue,propertyValues);
        }

        return result;
    }

}

