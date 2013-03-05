package platform.server.form.instance;

import platform.base.BaseUtils;
import platform.base.FunctionSet;
import platform.base.OrderedMap;
import platform.base.Pair;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.SFunctionSet;
import platform.base.col.interfaces.immutable.*;
import platform.base.col.interfaces.mutable.*;
import platform.base.col.interfaces.mutable.mapvalue.GetStaticValue;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.Order;
import platform.interop.form.PropertyReadType;
import platform.server.Message;
import platform.server.ThisMessage;
import platform.server.caches.IdentityLazy;
import platform.server.classes.BaseClass;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.ValueClass;
import platform.server.data.QueryEnvironment;
import platform.server.data.SQLSession;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.MapKeysInterface;
import platform.server.data.query.Query;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.instance.filter.AndFilterInstance;
import platform.server.form.instance.filter.FilterInstance;
import platform.server.form.instance.filter.OrFilterInstance;
import platform.server.form.instance.listener.CustomClassListener;
import platform.server.logics.DataObject;
import platform.server.logics.NullValue;
import platform.server.logics.ObjectValue;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.IsClassProperty;
import platform.server.logics.property.Property;
import platform.server.session.DataSession;
import platform.server.session.Modifier;
import platform.server.session.NoPropertyTableUsage;
import platform.server.session.SessionChanges;

import java.sql.SQLException;
import java.util.*;

import static platform.interop.ClassViewType.GRID;
import static platform.interop.ClassViewType.HIDE;

public class GroupObjectInstance implements MapKeysInterface<ObjectInstance> {

    public final CalcPropertyObjectInstance propertyBackground;
    public final CalcPropertyObjectInstance propertyForeground;
    final static int DIRECTION_DOWN = 1;
    final static int DIRECTION_UP = 2;
    final static int DIRECTION_CENTER = 3;

    RowBackgroundReaderInstance rowBackgroundReader = new RowBackgroundReaderInstance();
    RowForegroundReaderInstance rowForegroundReader = new RowForegroundReaderInstance();

    GroupObjectEntity entity;

    public static ImSet<ObjectInstance> getObjects(ImSet<GroupObjectInstance> groups) {
        MExclSet<ObjectInstance> mResult = SetFact.mExclSet();
        for(GroupObjectInstance group : groups)
            mResult.exclAddAll(group.objects);
        return mResult.immutable();
    }

    public static ImOrderSet<ObjectInstance> getOrderObjects(ImOrderSet<GroupObjectInstance> groups) {
        MOrderExclSet<ObjectInstance> mResult = SetFact.mOrderExclSet();
        for(GroupObjectInstance group : groups)
            mResult.exclAddAll(group.getOrderObjects());
        return mResult.immutableOrder();
    }

    public ImSet<ObjectInstance> objects; //
    private final ImOrderSet<ObjectInstance> orderObjects;
    public ImOrderSet<ObjectInstance> getOrderObjects() {
        return orderObjects;
    }

    public String toString() {
        return objects.toString();
    }

    // глобальный идентификатор чтобы писать во GroupObjectTable
    public int getID() {
        return entity.getID();
    }

    public String getSID() {
        return entity.getSID();
    }

    private Integer pageSize;
    public int getPageSize() {
        assert !isInTree();
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        if(entity.pageSize == null){
            this.pageSize = pageSize;
        }
    }

    public GroupObjectInstance(GroupObjectEntity entity, ImOrderSet<ObjectInstance> objects, CalcPropertyObjectInstance propertyBackground, CalcPropertyObjectInstance propertyForeground, ImMap<ObjectInstance, CalcPropertyObjectInstance> parent) {

        this.entity = entity;

        this.objects = objects.getSet();
        this.orderObjects = objects;

        this.propertyBackground = propertyBackground;
        this.propertyForeground = propertyForeground;

        for(ObjectInstance object : objects)
            object.groupTo = this;

        // текущее состояние
        if (this.curClassView != entity.initClassView) {
            this.curClassView = entity.initClassView;
            this.updated |= UPDATED_CLASSVIEW;
        }
        if(entity.pageSize != null) {
            this.pageSize = entity.pageSize;
        } else {
            this.pageSize = GroupObjectEntity.PAGE_SIZE_DEFAULT_VALUE;
        }

        this.parent = parent;
    }

    public ImRevMap<ObjectInstance, KeyExpr> getMapKeys() {
        return KeyExpr.getMapKeys(objects);
    }

    public Integer order = 0;

    // классовый вид включен или нет
    public ClassViewType curClassView = ClassViewType.GRID;

    // закэшированные

    public ImSet<FilterInstance> setFilters = null;
    public ImSet<FilterInstance> getSetFilters() {
        if(setFilters==null) {
            FilterInstance userComboFilter = combineUserFilters(userFilters);
            ImSet<FilterInstance> userComboSet = userComboFilter != null ? SetFact.singleton(userComboFilter) : SetFact.fromJavaSet(userFilters);
            setFilters = fixedFilters.merge(userComboSet).merge(SetFact.fromJavaSet(regularFilters)).merge(SetFact.fromJavaSet(tempFilters));
        }
        return setFilters;
    }

    private FilterInstance combineUserFilters(Set<FilterInstance> filterSet) {
        FilterInstance comboFilter = null;
        List<List<FilterInstance>> organizedFilters = new ArrayList<List<FilterInstance>>();
        List<FilterInstance> orFilters = new ArrayList<FilterInstance>();
        for (FilterInstance filter : filterSet) {
            orFilters.add(filter);
            if (filter.junction) {
                organizedFilters.add(orFilters);
                orFilters = new ArrayList<FilterInstance>();
            }
        }
        if (!orFilters.isEmpty())
            organizedFilters.add(orFilters);

        List<FilterInstance> ands = new ArrayList<FilterInstance>();
        for (List<FilterInstance> ors : organizedFilters) {
            FilterInstance filt = null;
            for (FilterInstance filter : ors) {
                if (filt == null) {
                    filt = filter;
                    continue;
                }
                filt = new OrFilterInstance(filt, filter);
            }
            ands.add(filt);
        }

        for (FilterInstance filter : ands) {
            if (comboFilter == null) {
                comboFilter = filter;
                continue;
            }
            comboFilter = new AndFilterInstance(comboFilter, filter);
        }
        return comboFilter;
    }

    // вообще все фильтры
    public ImSet<FilterInstance> fixedFilters = SetFact.EMPTY();

    private Set<FilterInstance> userFilters = SetFact.mAddRemoveSet();
    public void clearUserFilters() {
        userFilters.clear();

        setFilters = null;
        updated |= UPDATED_FILTER;
    }
    public void addUserFilter(FilterInstance addFilter) {
        userFilters.add(addFilter);

        setFilters = null;
        updated |= UPDATED_FILTER;
    }

    private Set<FilterInstance> regularFilters = new HashSet<FilterInstance>();
    public void addRegularFilter(FilterInstance filter) {
        regularFilters.add(filter);

        setFilters = null;
        updated |= UPDATED_FILTER;
    }

    public void removeRegularFilter(FilterInstance filter) {
        regularFilters.remove(filter);

        setFilters = null;
        updated |= UPDATED_FILTER;
    }

    private Set<FilterInstance> tempFilters = new HashSet<FilterInstance>();
    public void clearTempFilters() {
        tempFilters.clear();

        setFilters = null;
        updated |= UPDATED_FILTER;
    }
    public void addTempFilter(FilterInstance addFilter) {
        tempFilters.add(addFilter);

        setFilters = null;
        updated |= UPDATED_FILTER;
    }

    // с активным интерфейсом
    public ImSet<FilterInstance> filters;

    public ImOrderMap<OrderInstance, Boolean> fixedOrders = MapFact.EMPTYORDER();

    // обертку потому как сложный assertion
    private ImOrderMap<OrderInstance,Boolean> setOrders = null;
    public ImOrderMap<OrderInstance,Boolean> getSetOrders() {
        if(setOrders==null)
            setOrders = MapFact.fromJavaOrderMap(userOrders).mergeOrder(fixedOrders).mergeOrder(getOrderObjects().toOrderMap(false));
        return setOrders;
    }
    private OrderedMap<OrderInstance,Boolean> userOrders = MapFact.mAddRemoveOrderMap();

    public void changeOrder(OrderInstance property, Order modiType) {
        if (modiType == Order.REPLACE) {
            userOrders = new OrderedMap<OrderInstance, Boolean>();
        }

        if (modiType == Order.REMOVE) {
            userOrders.remove(property);
        } else if (modiType == Order.DIR) {
            userOrders.put(property, !(userOrders.containsKey(property)? userOrders.get(property) : false));
        } else {
            userOrders.put(property, false);
        }

        setOrders = null;
        updated |= UPDATED_ORDER;
    }

    public void clearOrders() {
        userOrders.clear();
        setOrders = null;
        updated |= UPDATED_ORDER;
    }

    // с активным интерфейсом, assertion что содержит все ObjectInstance
    ImOrderMap<OrderInstance,Boolean> orders = MapFact.EMPTYORDER();

    boolean upKeys, downKeys;
    public ImOrderMap<ImMap<ObjectInstance,DataObject>,ImMap<OrderInstance,ObjectValue>> keys = MapFact.EMPTYORDER();

    // 0 !!! - изменился объект, 1 - класс объекта, 2 !!! - отбор, 3 !!! - хоть один класс, 4 !!! - классовый вид

    public final static int UPDATED_OBJECT = (1);
    public final static int UPDATED_KEYS = (1 << 2);
    public final static int UPDATED_GRIDCLASS = (1 << 3);
    public final static int UPDATED_CLASSVIEW = (1 << 4);
    public final static int UPDATED_ORDER = (1 << 5);
    public final static int UPDATED_FILTER = (1 << 6);
    public final static int UPDATED_EXPANDS = (1 << 7);

    public int updated = UPDATED_GRIDCLASS | UPDATED_ORDER | UPDATED_FILTER;

    private boolean assertNull() {
        Iterator<ObjectInstance> it = objects.iterator();
        boolean isNull = it.next().getObjectValue() instanceof NullValue; 
        for(ObjectInstance object : objects)
            if((object.getObjectValue() instanceof NullValue)!=isNull)
                return false;
        return true;
    }

    public boolean isNull() {
//        assert assertNull();
//        return objects.iterator().next().getObjectValue() instanceof NullValue;
        for (ObjectInstance object : GroupObjectInstance.getObjects(getUpTreeGroups()))
            if (object.getObjectValue() instanceof NullValue) return true;
        return false;
    }

    public ImMap<ObjectInstance,DataObject> getGroupObjectValue() {
        if(isNull())
            return MapFact.EMPTY();

        return GroupObjectInstance.getObjects(getUpTreeGroups()).mapValues(new GetValue<DataObject, ObjectInstance>() {
            public DataObject getMapValue(ObjectInstance value) {
                return value.getDataObject();
            }});
    }

    public ImMap<ObjectInstance,DataObject> findGroupObjectValue(ImMap<ObjectInstance,Object> map) {
        for(ImMap<ObjectInstance, DataObject> keyRow : keys.keyIt()) {
            boolean equal = true;
            for(int i=0,size=keyRow.size();i<size;i++) {
                if(!keyRow.getValue(i).object.equals(map.get(keyRow.getKey(i)))) {
                    equal = false;
                    break;
                }
            }
            if(equal) {
                return keyRow;
            }
        }

        // из-за работы с вебом, может прийти несинхронный запрос на изменение объекта
        // и соответственно ключ будет не найден, поэтому возвращаем null, вместо исключения,
        // чтобы игнорировать этот запрос
        return null;
//        throw new RuntimeException("key not found");
    }

    public Where getFilterWhere(ImMap<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier) {
        Where where = Where.TRUE;
        for(FilterInstance filt : filters)
            where = where.and(filt.getWhere(mapKeys, modifier));
        return where;
    }

    public static ImMap<ObjectInstance, ValueClass> getGridClasses(ImSet<ObjectInstance> objects) {
        return objects.mapValues(new GetValue<ValueClass, ObjectInstance>() {
            public ValueClass getMapValue(ObjectInstance value) {
                return value.getGridClass();
            }});
    }
    private Where getClassWhere(ImMap<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier) {
        return IsClassProperty.getWhere(getGridClasses(objects), mapKeys, modifier);
    }

    public Where getWhere(ImMap<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier) {
        return getFilterWhere(mapKeys, modifier).and(getClassWhere(mapKeys, modifier));
    }

    public ImMap<ObjectInstance,ObjectValue> getNulls() {
        return NullValue.getMap(getObjects(getUpTreeGroups()));
    }

    boolean isSolid() {
        Iterator<ObjectInstance> i = objects.iterator();
        boolean read = i.next() instanceof CustomObjectInstance;
        while(i.hasNext())
            if(read != i.next() instanceof CustomObjectInstance)
                return false;
        return true;
    }

    public void setClassListener(CustomClassListener classListener) {
        for (ObjectInstance object : objects)
            if (object instanceof CustomObjectInstance)
                ((CustomObjectInstance)object).setClassListener(classListener);
    }

    public boolean isInTree() {
        return treeGroup!=null || parent!=null;
    }

    public TreeGroupInstance treeGroup = null;
    public ImOrderSet<GroupObjectInstance> getOrderDownTreeGroups() {
        return treeGroup!=null?treeGroup.getDownTreeGroups(this):SetFact.<GroupObjectInstance>EMPTYORDER();
    }
    public ImSet<GroupObjectInstance> getDownTreeGroups() {
        return getOrderDownTreeGroups().getSet();
    }
    public GroupObjectInstance getUpTreeGroup() {
        return treeGroup!=null?treeGroup.getUpTreeGroup(this):null;
    }
    public ImOrderSet<GroupObjectInstance> getOrderUpTreeGroups() {
        return treeGroup!=null?treeGroup.getUpTreeGroups(this):SetFact.singletonOrder(this);
    }
    public ImSet<GroupObjectInstance> getUpTreeGroups() {
        return getOrderUpTreeGroups().getSet();
    }

    public static ImSet<GroupObjectInstance> getUpTreeGroups(ImSet<GroupObjectInstance> groups) {
        MSet<GroupObjectInstance> mResult = SetFact.mSet();
        for(GroupObjectInstance group : groups)
            mResult.addAll(group.getUpTreeGroups());
        return mResult.immutable();
    }

    public final ImMap<ObjectInstance, CalcPropertyObjectInstance> parent;

    // поиски по свойствам\объектам
    public SeekObjects userSeeks = null;

    public void addSeek(OrderInstance order, ObjectValue value, boolean addSeek) {
        if(userSeeks==null)
            userSeeks = new SeekObjects(false, getGroupObjectValue());
        userSeeks = userSeeks.add(order, value, addSeek);
    }

    public void dropSeek(ObjectInstance object) {
        if(userSeeks==null)
            userSeeks = new SeekObjects(false, getGroupObjectValue());
        userSeeks = userSeeks.remove(object);
    }

    public void seek(boolean end) {
        userSeeks = new SeekObjects(MapFact.<OrderInstance, ObjectValue>EMPTY(), end);
    }

    public void seek(ImMap<OrderInstance, ObjectValue> seek, boolean end) {
        userSeeks = new SeekObjects(seek, end);
    }

    @IdentityLazy
    public ImSet<DataObjectInstance> getFreeDataObjects() throws SQLException {

        final ImRevMap<ObjectInstance, KeyExpr> mapKeys = getMapKeys();

        final ImSet<KeyExpr> usedContext = getFilterWhere(mapKeys, Property.defaultModifier).getOuterKeys();

        return BaseUtils.immutableCast(objects.filterFn(new SFunctionSet<ObjectInstance>() {
            public boolean contains(ObjectInstance object) { // если DataObject и нету ключей
                return object instanceof DataObjectInstance && !usedContext.contains(mapKeys.get(object));
            }
        }));
    }

    public NoPropertyTableUsage<ObjectInstance> keyTable = null;
    public NoPropertyTableUsage<ObjectInstance> expandTable = null;
    
    private Where getExpandWhere(ImMap<ObjectInstance, ? extends Expr> mapKeys) {
        if(expandTable==null)
            return Where.FALSE;
        else
            return expandTable.getWhere(mapKeys);
    }

    private ImOrderMap<ImMap<ObjectInstance, DataObject>, ImMap<ObjectInstance, ObjectValue>> executeTree(SQLSession session, QueryEnvironment env, final Modifier modifier, BaseClass baseClass) throws SQLException {
        assert isInTree();

        final ImRevMap<ObjectInstance, KeyExpr> mapKeys = KeyExpr.getMapKeys(GroupObjectInstance.getObjects(getUpTreeGroups()));

        ImMap<ObjectInstance,Expr> expandExprs = MapFact.EMPTY();

        Where expandWhere;
        if(getUpTreeGroup()!=null)
            expandWhere = getUpTreeGroup().getExpandWhere(mapKeys); // для верхней группы брать только из expandTable'а
        else
            expandWhere = Where.TRUE;

        if(parent!=null) {
            expandExprs = parent.mapValues(new GetValue<Expr, CalcPropertyObjectInstance>() {
                public Expr getMapValue(CalcPropertyObjectInstance value) {
                    return value.getExpr(mapKeys, modifier);
                }});

            Where nullWhere = Where.FALSE;
            for(Expr expr : expandExprs.valueIt())
                nullWhere = nullWhere.or(expr.getWhere().not());
            expandWhere = expandWhere.and(nullWhere).or(getExpandWhere(MapFact.override(mapKeys,expandExprs))); // если есть parent, то те чей parent равен null
        }

        ImOrderMap<Expr, Boolean> orderExprs = orders.mapMergeOrderKeys(new GetValue<Expr, OrderInstance>() {
            public Expr getMapValue(OrderInstance value) {
                return value.getExpr(mapKeys, modifier);
            }});

        return new Query<ObjectInstance, ObjectInstance>(mapKeys, expandExprs, getWhere(mapKeys, modifier).and(expandWhere)).
                    executeClasses(session, env, baseClass, orderExprs);
    }

    public void change(SessionChanges session, ImMap<ObjectInstance, DataObject> value) throws SQLException {
        // проставим все объектам метки изменений
        assert value.isEmpty() || value.keys().equals(GroupObjectInstance.getObjects(getUpTreeGroups()));
        for (ObjectInstance object : GroupObjectInstance.getObjects(getUpTreeGroups()))
            object.changeValue(session, value.isEmpty()?NullValue.instance:value.get(object));
        for(ObjectInstance object : GroupObjectInstance.getObjects(getDownTreeGroups()))
            object.changeValue(session, NullValue.instance);
    }

    public void update(SessionChanges session, MFormChanges changes, ImMap<ObjectInstance, DataObject> value) throws SQLException {
        changes.objects.exclAdd(this, value.isEmpty() ? NullValue.getMap(getObjects(getUpTreeGroups())) : value);
        change(session, value);
    }

    private boolean pendingHidden;
    
    @Message("message.form.update.group.keys")
    @ThisMessage
    public ImMap<ObjectInstance, DataObject> updateKeys(SQLSession sql, QueryEnvironment env, Modifier modifier, BaseClass baseClass, boolean hidden, final boolean refresh, MFormChanges result, FunctionSet<CalcProperty> changedProps) throws SQLException {
        if (refresh || (updated & UPDATED_CLASSVIEW) != 0) {
            result.classViews.exclAdd(this, curClassView);
        }

        if (keyTable == null) // в общем то только для hidden'а но может и потом понадобиться
            keyTable = createKeyTable();

        if (curClassView == HIDE) return null;

        // если изменились класс грида или представление
        boolean updateKeys = refresh || (updated & (UPDATED_GRIDCLASS | UPDATED_CLASSVIEW)) != 0;

        ImSet<FilterInstance> setFilters = getSetFilters();
        if (FilterInstance.ignoreInInterface) {
            updateKeys |= (updated & UPDATED_FILTER) != 0;
            filters = setFilters;
        } else {
            if ((updated & UPDATED_FILTER) != 0) {
                ImSet<FilterInstance> newFilters = setFilters.filterFn(new SFunctionSet<FilterInstance>() {
                    public boolean contains(FilterInstance filt) {
                        return filt.isInInterface(GroupObjectInstance.this);
                    }
                });

                updateKeys |= !BaseUtils.hashEquals(newFilters, filters);
                filters = newFilters;
            } else { // остались те же setFilters
                for (FilterInstance filt : setFilters)
                    if (refresh || filt.classUpdated(SetFact.singleton(this))) {
                        boolean inInterface = filt.isInInterface(this);
                        if(inInterface != filters.contains(filt)) {
                            if(inInterface)
                                filters = filters.addExcl(filt);
                            else
                                filters = filters.removeIncl(filt);
                            updateKeys = true;
                        }
                    }
            }
        }

        // порядки
        if(OrderInstance.ignoreInInterface) {
            updateKeys |= (updated & UPDATED_ORDER) != 0;
            orders = getSetOrders();
        } else {
            ImOrderMap<OrderInstance, Boolean> setOrders = getSetOrders();
            ImOrderMap<OrderInstance, Boolean> newOrders;
            if ((updated & UPDATED_ORDER) != 0) {
                newOrders = setOrders.filterOrder(new SFunctionSet<OrderInstance>() {
                    public boolean contains(OrderInstance orderInstance) {
                        return orderInstance.isInInterface(GroupObjectInstance.this);
                    }
                });
            } else { // значит setOrders не изменился
                newOrders = setOrders.filterOrder(new SFunctionSet<OrderInstance>() {
                    public boolean contains(OrderInstance orderInstance) {
                        boolean isInInterface = orders.containsKey(orderInstance);
                        if ((refresh || orderInstance.classUpdated(SetFact.singleton(GroupObjectInstance.this))) && !(orderInstance.isInInterface(GroupObjectInstance.this) == isInInterface)) {
                            isInInterface = !isInInterface;
                        }
                        return isInInterface;
                    }});
            }
            updateKeys |= !orders.equals(newOrders);
            orders = newOrders;
        }

        if (!updateKeys) // изменились "верхние" объекты для фильтров
            for (FilterInstance filt : filters)
                if (filt.objectUpdated(SetFact.singleton(this))) {
                    updateKeys = true;
                    break;
                }
        if (!updateKeys) // изменились "верхние" объекты для порядков
            for (OrderInstance order : orders.keyIt())
                if (order.objectUpdated(SetFact.singleton(this))) {
                    updateKeys = true;
                    break;
                }
        if (!updateKeys) // изменились данные по фильтрам
            for (FilterInstance filt : filters)
                if (filt.dataUpdated(changedProps)) {
                    updateKeys = true;
                    break;
                }
        if (!updateKeys) // изменились данные по порядкам
            for (OrderInstance order : orders.keyIt())
                if (order.dataUpdated(changedProps)) {
                    updateKeys = true;
                    break;
                }
        if (!updateKeys) // классы удалились\добавились
            for (ObjectInstance object : objects)
                if (object.classChanged(changedProps)) {  // || object.classUpdated() сомнительный or
                    updateKeys = true;
                    break;
                }

        if(hidden) {
            pendingHidden |= updateKeys;
            return null;
        } else {
            updateKeys |= pendingHidden;
            pendingHidden = false;
        }

        ImMap<ObjectInstance, DataObject> currentObject = getGroupObjectValue();
        SeekObjects orderSeeks = null;
        int direction = DIRECTION_CENTER;

        if(isInTree()) {
            if (!updateKeys && (getUpTreeGroup() != null && ((getUpTreeGroup().updated & UPDATED_EXPANDS) != 0)) ||
                    (parent != null && (updated & UPDATED_EXPANDS) != 0)) {
                updateKeys = true;
            }
            orderSeeks = new SeekObjects(false);
        } else {
            if (userSeeks!=null) { // пользовательский поиск
                orderSeeks = userSeeks;
                updateKeys = true;
                currentObject = MapFact.EMPTY();
            } else if (updateKeys) {
                // изменились фильтры, порядки, вид, ищем текущий объект
                orderSeeks = new SeekObjects(false, currentObject);
            }

            if (!updateKeys && curClassView == GRID && !currentObject.isEmpty() && (updated & UPDATED_OBJECT) != 0) { // скроллирование
                int keyNum = keys.indexOf(currentObject);
                if (upKeys && keyNum < getPageSize()) { // если меньше PageSize осталось и сверху есть ключи
                    updateKeys = true;

                    int lowestInd = getPageSize() * 2 - 1;
                    if (lowestInd >= keys.size()) // по сути END
                        orderSeeks = new SeekObjects(true);
                    else {
                        direction = DIRECTION_UP;
                        orderSeeks = new SeekObjects(keys.getValue(lowestInd), false);
                    }
                } else // наоборот вниз
                    if (downKeys && keyNum >= keys.size() - getPageSize()) { // assert что pageSize не null
                        updateKeys = true;

                        int highestInd = keys.size() - getPageSize() * 2;
                        if (highestInd < 0) // по сути HOME
                            orderSeeks = new SeekObjects(false);
                        else {
                            direction = DIRECTION_DOWN;
                            orderSeeks = new SeekObjects(keys.getValue(highestInd), false);
                        }
                    }
            }
        }

        if (updateKeys) {
            assert orderSeeks != null;

            updated = (updated | UPDATED_KEYS);

            if (curClassView != GRID) // панель
                return orderSeeks.readKeys(sql, env, modifier, baseClass);
            else {
                int activeRow = -1; // какой ряд выбранным будем считать

                if (isInTree()) { // если дерево, то без поиска, но возможно с parent'ами
                    assert orderSeeks.values.isEmpty() && !orderSeeks.end;

                    ImOrderMap<ImMap<ObjectInstance, DataObject>, ImMap<ObjectInstance, ObjectValue>> treeElements = executeTree(sql, env, modifier, baseClass);

                    ImList<ImMap<ObjectInstance, DataObject>> expandParents = treeElements.mapListValues(new GetValue<ImMap<ObjectInstance, DataObject>, ImMap<ObjectInstance, ObjectValue>>() {
                        public ImMap<ObjectInstance, DataObject> getMapValue(ImMap<ObjectInstance, ObjectValue> value) {
                            return DataObject.filterDataObjects(value);
                        }
                    });
                    keys = treeElements.mapOrderValues(new GetStaticValue<ImMap<OrderInstance, ObjectValue>>() {
                        public ImMap<OrderInstance, ObjectValue> getMapValue() {
                            return MapFact.EMPTY();
                        }});

                    result.parentObjects.exclAdd(this, expandParents);
                    activeRow = keys.size() == 0 ? -1 : 0;
                } else {
                    keys = MapFact.EMPTYORDER();

                    if (!orders.starts(orderSeeks.values.keys())) // если не "хватает" спереди ключей, дочитываем
                        orderSeeks = orderSeeks.readValues(sql, env, modifier, baseClass);

                    if (direction == DIRECTION_CENTER) { // оптимизируем если HOME\END, то читаем одним запросом
                        if(orderSeeks.values.isEmpty()) {
                            if (orderSeeks.end) { // END
                                direction = DIRECTION_UP;
                                downKeys = false;
                            } else { // HOME
                                direction = DIRECTION_DOWN;
                                upKeys = false;
                            }
                        }
                    } else {
                        downKeys = true;
                        upKeys = true;
                        assert !orderSeeks.values.isEmpty();
                    }

                    int readSize = getPageSize() * 3 / (direction == DIRECTION_CENTER ? 2 : 1);

                    if (direction == DIRECTION_UP || direction == DIRECTION_CENTER) { // сначала Up
                        keys = keys.addOrderExcl(orderSeeks.executeOrders(sql, env, modifier, baseClass, readSize, false).reverseOrder());
                        upKeys = (keys.size() == readSize);
                        activeRow = keys.size() - 1;
                    }
                    if (direction == DIRECTION_DOWN || direction == DIRECTION_CENTER) { // затем Down
                        ImOrderMap<ImMap<ObjectInstance, DataObject>, ImMap<OrderInstance, ObjectValue>> executeList = orderSeeks.executeOrders(sql, env, modifier, baseClass, readSize, true);
                        if (executeList.size() > 0 && !(orderSeeks.end && activeRow>0)) activeRow = keys.size(); // не выбираем если идет seekDown и уже выбран ряд - это то что надо
                        keys = keys.addOrderExcl(executeList);
                        downKeys = (executeList.size() == readSize);
                    }
                }

                // параллельно будем обновлять ключи чтобы JoinSelect'ить
                keyTable.writeKeys(sql, keys.keys());
                result.gridObjects.exclAdd(this, keys.keyOrderSet());

                if (!keys.containsKey(currentObject)) { // если нету currentObject'а, его нужно изменить
                    if(getUpTreeGroup()==null) // если верхняя группа
                        return activeRow>=0?keys.getKey(activeRow):MapFact.<ObjectInstance,DataObject>EMPTY();
                    else // иначе assertion что activeRow < 0, выбираем верхнюю
                        return keys.size()>0 && !currentObject.isEmpty()?keys.getKey(0):null;
                } else // так как сейчас клиент требует прислать ему groupObject даже если он не изменился, если приходят ключи
                    return currentObject;
            }
        }

        return null; // ничего не изменилось
    }

    public ImOrderMap<ImMap<ObjectInstance, DataObject>, ImMap<OrderInstance, ObjectValue>> seekObjects(SQLSession sql, QueryEnvironment env, Modifier modifier, BaseClass baseClass, int readSize) throws SQLException {
        SeekObjects orderSeeks = new SeekObjects(keys.getValue(keys.indexOf(getGroupObjectValue())), false);
        return orderSeeks.executeOrders(sql, env, modifier, baseClass, readSize, true);
    }

    public ImOrderSet<ImMap<ObjectInstance, DataObject>> createObjects(DataSession session, FormInstance form, int quantity) throws SQLException {
        if (objects.size() > 1) {
            return SetFact.EMPTYORDER();
        }
        MOrderExclSet<ImMap<ObjectInstance, DataObject>> mResultSet = SetFact.mOrderExclSet(quantity);
        for (int i = 0; i < quantity; i++) {
            ImFilterValueMap<ObjectInstance, DataObject> mvObjectKeys = objects.mapFilterValues();
            for (int j=0,size=objects.size();j<size;j++) {
                ObjectInstance objectInstance = objects.get(j);
                if (objectInstance.getBaseClass() instanceof ConcreteCustomClass)
                    mvObjectKeys.mapValue(j, form.addFormObject((CustomObjectInstance)objectInstance, (ConcreteCustomClass) objectInstance.getBaseClass(), null));
            }
            mResultSet.exclAdd(mvObjectKeys.immutableValue());
        }
        return mResultSet.immutableOrder();
    }

    public class SeekObjects {
        public ImMap<OrderInstance, ObjectValue> values;
        public boolean end;

        public SeekObjects(ImMap<OrderInstance, ObjectValue> values, boolean end) {
            this.values = values;
            this.end = end;
        }

        public SeekObjects(boolean end, ImMap<ObjectInstance, DataObject> values) {
            this(BaseUtils.<ImMap<OrderInstance, ObjectValue>>immutableCast(values), end);
        }

        public SeekObjects(boolean end) {
            this(MapFact.<OrderInstance, ObjectValue>EMPTY(), end);
        }

        public SeekObjects add(OrderInstance order, ObjectValue value, boolean down) {
            return new SeekObjects(values.override(order, value), this.end || down);
        }

        public SeekObjects remove(ObjectInstance object) {
            return new SeekObjects(values.remove(object), end);
        }

        public SeekObjects reverse() {
            return new SeekObjects(values, !end);
        }

        // возвращает OrderInstance из orderSeeks со значениями, а также если есть parent, то parent'ы
        public ImOrderMap<ImMap<ObjectInstance, DataObject>, ImMap<OrderInstance, ObjectValue>>
        executeOrders(SQLSession session, QueryEnvironment env, final Modifier modifier, BaseClass baseClass, int readSize, boolean down) throws SQLException {
            assert !isInTree();

            final ImRevMap<ObjectInstance, KeyExpr> mapKeys = getMapKeys();

            // assertion что group.orders начинается с orderSeeks
            ImOrderMap<OrderInstance, Boolean> orders;
            if (readSize == 1)
                orders = GroupObjectInstance.this.orders.moveStart(values.keys());
            else
                orders = GroupObjectInstance.this.orders;

            assert orders.starts(values.keys());

            ImMap<OrderInstance, Expr> orderExprs = orders.getMap().mapKeyValues(new GetValue<Expr, OrderInstance>() {
                public Expr getMapValue(OrderInstance value) {
                    return value.getExpr(mapKeys, modifier);
                }
            });

            Where orderWhere = end?Where.FALSE:Where.TRUE; // строим условия на упорядочивание
            ImOrderMap<OrderInstance, Boolean> reverseOrder = orders.reverseOrder();
            for (int i=0,size=reverseOrder.size();i<size;i++) {
                OrderInstance orderInstance = reverseOrder.getKey(i);
                ObjectValue toSeek = values.get(orderInstance);
                if (toSeek != null)
                    orderWhere = toSeek.order(orderExprs.get(orderInstance), reverseOrder.getValue(i), orderWhere);
            }

            if(!down)
                orderWhere = orderWhere.not();

            if (readSize == 1) { // в частном случае если есть "висячие" ключи не в фильтре и нужна одна запись ставим равно вместо >
                for(DataObjectInstance freeObject : getFreeDataObjects()) {
                    ObjectValue freeValue = values.get(freeObject);
                    if(freeValue==null || !(freeValue instanceof DataObject))
                        freeValue = freeObject.getBaseClass().getDefaultObjectValue();
                    orderWhere = orderWhere.and(end==!down?mapKeys.get(freeObject).compare((DataObject)freeValue, Compare.EQUALS):Where.FALSE); // seekDown==!down, чтобы и вверх и вниз не попали одни и те же ключи
                }
            }

            return new Query<ObjectInstance, OrderInstance>(mapKeys, orderExprs, getWhere(mapKeys, modifier).and(orderWhere)).
                        executeClasses(session, down ? orders : Query.reverseOrder(orders), readSize, baseClass, env);
        }

        // считывает одну запись
        private Pair<ImMap<ObjectInstance, DataObject>, ImMap<OrderInstance, ObjectValue>> readObjects(SQLSession session, QueryEnvironment env, Modifier modifier, BaseClass baseClass) throws SQLException {
            ImOrderMap<ImMap<ObjectInstance, DataObject>, ImMap<OrderInstance, ObjectValue>> result = executeOrders(session, env, modifier, baseClass, 1, !end);
            if (result.size() == 0)
                result = new SeekObjects(values, !end).executeOrders(session, env, modifier, baseClass, 1, end);
            if (result.size() > 0)
                return new Pair<ImMap<ObjectInstance, DataObject>, ImMap<OrderInstance, ObjectValue>>(result.singleKey(), result.singleValue());
            else
                return null;
        }

        public ImMap<ObjectInstance, DataObject> readKeys(SQLSession session, QueryEnvironment env, Modifier modifier, BaseClass baseClass) throws SQLException {
            Pair<ImMap<ObjectInstance, DataObject>, ImMap<OrderInstance, ObjectValue>> objects = readObjects(session, env, modifier, baseClass);
            if (objects != null)
                return objects.first;
            else
                return MapFact.EMPTY();
        }

        public SeekObjects readValues(SQLSession session, QueryEnvironment env, Modifier modifier, BaseClass baseClass) throws SQLException {
            Pair<ImMap<ObjectInstance, DataObject>, ImMap<OrderInstance, ObjectValue>> objects = readObjects(session, env, modifier, baseClass);
            if (objects != null)
                return new SeekObjects(objects.second, end);
            else
                return new SeekObjects(false);
        }
    }

    public NoPropertyTableUsage<ObjectInstance> createKeyTable() {
        return new NoPropertyTableUsage<ObjectInstance>(GroupObjectInstance.getOrderObjects(getOrderUpTreeGroups()), new Type.Getter<ObjectInstance>() {
            public Type getType(ObjectInstance key) {
                return key.getType();
            }
        });                
    }

    public class RowBackgroundReaderInstance implements PropertyReaderInstance {
        public CalcPropertyObjectInstance getPropertyObjectInstance() {
            return propertyBackground;
        }

        public byte getTypeID() {
            return PropertyReadType.ROW_BACKGROUND;
        }

        public int getID() {
            return GroupObjectInstance.this.getID();
        }

        @Override
        public String toString() {
            return ServerResourceBundle.getString("logics.background") + " (" + GroupObjectInstance.this.toString() + ")";
        }
    }

    public class RowForegroundReaderInstance implements PropertyReaderInstance {
        public CalcPropertyObjectInstance getPropertyObjectInstance() {
            return propertyForeground;
        }

        public byte getTypeID() {
            return PropertyReadType.ROW_FOREGROUND;
        }

        public int getID() {
            return GroupObjectInstance.this.getID();
        }

        @Override
        public String toString() {
            return ServerResourceBundle.getString("logics.foreground") + " (" + GroupObjectInstance.this.toString() + ")";
        }
    }
}
