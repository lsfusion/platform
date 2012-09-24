package platform.server.form.instance;

import platform.base.BaseUtils;
import platform.base.FunctionSet;
import platform.base.OrderedMap;
import platform.base.QuickSet;
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

    public static List<ObjectInstance> getObjects(Collection<GroupObjectInstance> groups) {
        List<ObjectInstance> result = new ArrayList<ObjectInstance>();
        for(GroupObjectInstance group : groups)
            result.addAll(group.objects);
        return result;
    }

    public Collection<ObjectInstance> objects;

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

    public GroupObjectInstance(GroupObjectEntity entity, Collection<ObjectInstance> objects, CalcPropertyObjectInstance propertyBackground, CalcPropertyObjectInstance propertyForeground, Map<ObjectInstance, CalcPropertyObjectInstance> parent) {

        this.entity = entity;

        this.objects = objects;

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

    public Map<ObjectInstance, KeyExpr> getMapKeys() {
        return KeyExpr.getMapKeys(objects);
    }

    public Integer order = 0;

    // классовый вид включен или нет
    public ClassViewType curClassView = ClassViewType.GRID;

    // закэшированные

    public Set<FilterInstance> setFilters = null;
    public Set<FilterInstance> getSetFilters() {
        if(setFilters==null) {
            FilterInstance userComboFilter = combineUserFilters(userFilters);
            Set<FilterInstance> userComboSet = userComboFilter != null ? new HashSet<FilterInstance>(Collections.singleton(userComboFilter)) : userFilters;
            setFilters = BaseUtils.mergeSet(BaseUtils.mergeSet(BaseUtils.mergeSet(fixedFilters, userComboSet),regularFilters),tempFilters);
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
    public Set<FilterInstance> fixedFilters = new HashSet<FilterInstance>();

    private Set<FilterInstance> userFilters = new LinkedHashSet<FilterInstance>();
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
    public Set<FilterInstance> filters = new HashSet<FilterInstance>();

    public OrderedMap<OrderInstance, Boolean> fixedOrders = new OrderedMap<OrderInstance, Boolean>();

    // обертку потому как сложный assertion
    private OrderedMap<OrderInstance,Boolean> setOrders = null;
    public OrderedMap<OrderInstance,Boolean> getSetOrders() {
        if(setOrders==null) {
            setOrders = new OrderedMap<OrderInstance,Boolean>(userOrders);
            setOrders.putAll(fixedOrders);
            for(ObjectInstance object : objects)
                if(!(setOrders.containsKey(object)))
                    setOrders.put(object,false);
        }
        return setOrders;
    }
    private OrderedMap<OrderInstance,Boolean> userOrders = new OrderedMap<OrderInstance, Boolean>();

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
    OrderedMap<OrderInstance,Boolean> orders = new OrderedMap<OrderInstance, Boolean>();

    boolean upKeys, downKeys;
    public OrderedMap<Map<ObjectInstance,DataObject>,Map<OrderInstance,ObjectValue>> keys = new OrderedMap<Map<ObjectInstance, DataObject>, Map<OrderInstance, ObjectValue>>();

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

    public Map<ObjectInstance,DataObject> getGroupObjectValue() {
        if(isNull())
            return new HashMap<ObjectInstance, DataObject>();
        
        Map<ObjectInstance,DataObject> result = new HashMap<ObjectInstance, DataObject>();
        for(ObjectInstance object : GroupObjectInstance.getObjects(getUpTreeGroups()))
            result.put(object,object.getDataObject());
        return result;
    }

    public Map<ObjectInstance,DataObject> findGroupObjectValue(Map<ObjectInstance,Object> map) {
        for(Map<ObjectInstance,DataObject> keyRow : keys.keySet()) {
            boolean equal = true;
            for(Map.Entry<ObjectInstance,DataObject> keyEntry : keyRow.entrySet()) {
                if(!keyEntry.getValue().object.equals(map.get(keyEntry.getKey()))) {
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

    public Where getFilterWhere(Map<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier) {
        Where where = Where.TRUE;
        for(FilterInstance filt : filters)
            where = where.and(filt.getWhere(mapKeys, modifier));
        return where;
    }

    public static Map<ObjectInstance, ValueClass> getGridClasses(Collection<ObjectInstance> objects) {
        Map<ObjectInstance, ValueClass> result = new HashMap<ObjectInstance, ValueClass>();
        for(ObjectInstance object : objects)
            result.put(object,object.getGridClass());
        return result;
    }
    private Where getClassWhere(Map<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier) {
        return IsClassProperty.getWhere(getGridClasses(objects), mapKeys, modifier);
    }

    public Where getWhere(Map<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier) {
        return getFilterWhere(mapKeys, modifier).and(getClassWhere(mapKeys, modifier));
    }

    public Map<ObjectInstance,ObjectValue> getNulls() {
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
    public List<GroupObjectInstance> getDownTreeGroups() {
        return treeGroup!=null?treeGroup.getDownTreeGroups(this):new ArrayList<GroupObjectInstance>();
    }
    public GroupObjectInstance getUpTreeGroup() {
        return treeGroup!=null?treeGroup.getUpTreeGroup(this):null;
    }
    public List<GroupObjectInstance> getUpTreeGroups() {
        return treeGroup!=null?treeGroup.getUpTreeGroups(this):Collections.singletonList(this);
    }

    public static Set<GroupObjectInstance> getUpTreeGroups(Set<GroupObjectInstance> groups) {
        Set<GroupObjectInstance> result = new HashSet<GroupObjectInstance>();
        for(GroupObjectInstance group : groups)
            result.addAll(group.getUpTreeGroups());
        return result;
    }

    public final Map<ObjectInstance, CalcPropertyObjectInstance> parent;

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
        userSeeks = new SeekObjects(new HashMap<OrderInstance, ObjectValue>(), end);
    }

    public void seek(Map<OrderInstance, ObjectValue> seek, boolean end) {
        userSeeks = new SeekObjects(seek, end);
    }

    @IdentityLazy
    public Collection<DataObjectInstance> getFreeDataObjects() throws SQLException {
        Collection<DataObjectInstance> freeObjects = new ArrayList<DataObjectInstance>();

        Map<ObjectInstance, KeyExpr> mapKeys = getMapKeys();

        QuickSet<KeyExpr> usedContext = getFilterWhere(mapKeys, Property.defaultModifier).getOuterKeys();
        for(ObjectInstance object : objects)
            if(object instanceof DataObjectInstance && !usedContext.contains(mapKeys.get(object))) // если DataObject и нету ключей
                freeObjects.add((DataObjectInstance) object);

        return freeObjects;
    }

    public NoPropertyTableUsage<ObjectInstance> keyTable = null;
    public NoPropertyTableUsage<ObjectInstance> expandTable = null;
    
    private Where getExpandWhere(Map<ObjectInstance, ? extends Expr> mapKeys) {
        if(expandTable==null)
            return Where.FALSE;
        else
            return expandTable.getWhere(mapKeys);
    }

    private OrderedMap<Map<ObjectInstance, DataObject>, Map<ObjectInstance, ObjectValue>> executeTree(SQLSession session, QueryEnvironment env, Modifier modifier, BaseClass baseClass) throws SQLException {
        assert isInTree();

        Map<ObjectInstance, KeyExpr> mapKeys = KeyExpr.getMapKeys(GroupObjectInstance.getObjects(getUpTreeGroups()));

        Map<ObjectInstance,Expr> expandExprs = new HashMap<ObjectInstance, Expr>();

        Where expandWhere;
        if(getUpTreeGroup()!=null)
            expandWhere = getUpTreeGroup().getExpandWhere(mapKeys); // для верхней группы брать только из expandTable'а
        else
            expandWhere = Where.TRUE;

        if(parent!=null) {
            for(Map.Entry<ObjectInstance, CalcPropertyObjectInstance> parentEntry : parent.entrySet())
                expandExprs.put(parentEntry.getKey(), parentEntry.getValue().getExpr(mapKeys, modifier));

            Where nullWhere = Where.FALSE;
            for(Expr expr : expandExprs.values())
                nullWhere = nullWhere.or(expr.getWhere().not());
            expandWhere = expandWhere.and(nullWhere).or(getExpandWhere(BaseUtils.override(mapKeys,expandExprs))); // если есть parent, то те чей parent равен null
        }

        OrderedMap<Expr, Boolean> orderExprs = new OrderedMap<Expr, Boolean>();
        for (Map.Entry<OrderInstance, Boolean> toOrder : orders.entrySet())
            orderExprs.put(toOrder.getKey().getExpr(mapKeys, modifier), toOrder.getValue());

        return new Query<ObjectInstance, ObjectInstance>(mapKeys, expandExprs, getWhere(mapKeys, modifier).and(expandWhere)).
                    executeClasses(session, env, baseClass, orderExprs);
    }

    public void change(SessionChanges session, Map<ObjectInstance, DataObject> value) throws SQLException {
        // проставим все объектам метки изменений
        assert value.isEmpty() || value.keySet().equals(new HashSet<ObjectInstance>(GroupObjectInstance.getObjects(getUpTreeGroups())));
        for (ObjectInstance object : GroupObjectInstance.getObjects(getUpTreeGroups()))
            object.changeValue(session, value.isEmpty()?NullValue.instance:value.get(object));
        for(ObjectInstance object : GroupObjectInstance.getObjects(getDownTreeGroups()))
            object.changeValue(session, NullValue.instance);
    }

    public void update(SessionChanges session, FormChanges changes, Map<ObjectInstance, DataObject> value) throws SQLException {
        changes.objects.put(this, value.isEmpty() ? NullValue.getMap(getObjects(getUpTreeGroups())) : value);
        change(session, value);
    }

    private boolean pendingHidden;
    
    @Message("message.form.update.group.keys")
    @ThisMessage
    public Map<ObjectInstance, DataObject> updateKeys(SQLSession sql, QueryEnvironment env, Modifier modifier, BaseClass baseClass, boolean hidden, boolean refresh, FormChanges result, FunctionSet<CalcProperty> changedProps) throws SQLException {
        if (refresh || (updated & UPDATED_CLASSVIEW) != 0) {
            result.classViews.put(this, curClassView);
        }

        if (keyTable == null) // в общем то только для hidden'а но может и потом понадобиться
            keyTable = createKeyTable();

        if (curClassView == HIDE) return null;

        // если изменились класс грида или представление
        boolean updateKeys = refresh || (updated & (UPDATED_GRIDCLASS | UPDATED_CLASSVIEW)) != 0;

        if (FilterInstance.ignoreInInterface) {
            updateKeys |= (updated & UPDATED_FILTER) != 0;
            filters = getSetFilters();
        } else if ((updated & UPDATED_FILTER) != 0) {
            Set<FilterInstance> newFilters = new HashSet<FilterInstance>();
            for (FilterInstance filt : getSetFilters())
                if (filt.isInInterface(this))
                    newFilters.add(filt);

            updateKeys |= !newFilters.equals(filters);
            filters = newFilters;
        } else // остались те же setFilters
            for (FilterInstance filt : getSetFilters())
                if (refresh || filt.classUpdated(Collections.singleton(this)))
                    updateKeys |= (filt.isInInterface(this) ? filters.add(filt) : filters.remove(filt));

        // порядки
        if(OrderInstance.ignoreInInterface) {
            updateKeys |= (updated & UPDATED_ORDER) != 0;
            orders = getSetOrders();
        } else {
            OrderedMap<OrderInstance, Boolean> newOrders = new OrderedMap<OrderInstance, Boolean>();
            if ((updated & UPDATED_ORDER) != 0) {
                for (Map.Entry<OrderInstance, Boolean> setOrder : getSetOrders().entrySet())
                    if (setOrder.getKey().isInInterface(this))
                        newOrders.put(setOrder.getKey(), setOrder.getValue());
                updateKeys |= !orders.equals(newOrders);
            } else { // значит setOrders не изменился
                for (Map.Entry<OrderInstance, Boolean> setOrder : getSetOrders().entrySet()) {
                    OrderInstance orderInstance = setOrder.getKey();

                    boolean isInInterface = orders.containsKey(orderInstance);
                    if ((refresh || orderInstance.classUpdated(Collections.singleton(this))) && !(orderInstance.isInInterface(this) == isInInterface)) {
                        isInInterface = !isInInterface;
                        updateKeys = true;
                    }
                    if (isInInterface)
                        newOrders.put(orderInstance, setOrder.getValue());
                }
            }
            orders = newOrders;
        }

        if (!updateKeys) // изменились "верхние" объекты для фильтров
            for (FilterInstance filt : filters)
                if (filt.objectUpdated(Collections.singleton(this))) {
                    updateKeys = true;
                    break;
                }
        if (!updateKeys) // изменились "верхние" объекты для порядков
            for (OrderInstance order : orders.keySet())
                if (order.objectUpdated(Collections.singleton(this))) {
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
            for (OrderInstance order : orders.keySet())
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

        Map<ObjectInstance, DataObject> currentObject = getGroupObjectValue();
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
                currentObject = new HashMap<ObjectInstance, DataObject>();
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
                keys = new OrderedMap<Map<ObjectInstance, DataObject>, Map<OrderInstance, ObjectValue>>();

                if (isInTree()) { // если дерево, то без поиска, но возможно с parent'ами
                    assert orderSeeks.values.isEmpty() && !orderSeeks.end;

                    List<Map<ObjectInstance, DataObject>> expandParents = new ArrayList<Map<ObjectInstance, DataObject>>();
                    for(Map.Entry<Map<ObjectInstance, DataObject>, Map<ObjectInstance, ObjectValue>> resultRow : executeTree(sql, env, modifier, baseClass).entrySet()) {
                        keys.put(resultRow.getKey(), new HashMap<OrderInstance, ObjectValue>());
                        expandParents.add(BaseUtils.filterClass(resultRow.getValue(), DataObject.class));
                    }
                    result.parentObjects.put(this, expandParents);
                    activeRow = keys.size() == 0 ? -1 : 0;
                } else {
                    if (!orders.starts(orderSeeks.values.keySet())) // если не "хватает" спереди ключей, дочитываем
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
                        keys.putAll(orderSeeks.executeOrders(sql, env, modifier, baseClass, readSize, false).reverse());
                        upKeys = (keys.size() == readSize);
                        activeRow = keys.size() - 1;
                    }
                    if (direction == DIRECTION_DOWN || direction == DIRECTION_CENTER) { // затем Down
                        OrderedMap<Map<ObjectInstance, DataObject>, Map<OrderInstance, ObjectValue>> executeList = orderSeeks.executeOrders(sql, env, modifier, baseClass, readSize, true);
                        if (executeList.size() > 0 && !(orderSeeks.end && activeRow>0)) activeRow = keys.size(); // не выбираем если идет seekDown и уже выбран ряд - это то что надо
                        keys.putAll(executeList);
                        downKeys = (executeList.size() == readSize);
                    }
                }

                // параллельно будем обновлять ключи чтобы JoinSelect'ить
                keyTable.writeKeys(sql, keys.keyList());
                result.gridObjects.put(this, keys.keyList());

                if (!keys.containsKey(currentObject)) { // если нету currentObject'а, его нужно изменить
                    if(getUpTreeGroup()==null) // если верхняя группа
                        return activeRow>=0?keys.getKey(activeRow):new HashMap<ObjectInstance,DataObject>();
                    else // иначе assertion что activeRow < 0, выбираем верхнюю
                        return keys.size()>0 && !currentObject.isEmpty()?keys.keySet().iterator().next():null;
                } else // так как сейчас клиент требует прислать ему groupObject даже если он не изменился, если приходят ключи
                    return currentObject;
            }
        }

        return null; // ничего не изменилось
    }

    public OrderedMap<Map<ObjectInstance, DataObject>, Map<OrderInstance, ObjectValue>> seekObjects(SQLSession sql, QueryEnvironment env, Modifier modifier, BaseClass baseClass, int readSize) throws SQLException {
        SeekObjects orderSeeks = new SeekObjects(keys.getValue(keys.indexOf(getGroupObjectValue())), false);
        return orderSeeks.executeOrders(sql, env, modifier, baseClass, readSize, true);
    }

    public List<Map<ObjectInstance, DataObject>> createObjects(DataSession session, FormInstance form, int quantity) throws SQLException {
        List<Map<ObjectInstance, DataObject>> resultMap = new ArrayList<Map<ObjectInstance, DataObject>>();
        if (objects.size() > 1) {
            return resultMap;
        }
        for (int i = 0; i < quantity; i++) {
            Map<ObjectInstance, DataObject> objectKeys = new OrderedMap<ObjectInstance, DataObject>();
            for (ObjectInstance objectInstance : objects)
                if (objectInstance.getBaseClass() instanceof ConcreteCustomClass)
                    objectKeys.put(objectInstance, form.addFormObject((CustomObjectInstance)objectInstance, (ConcreteCustomClass) objectInstance.getBaseClass(), null));
            resultMap.add(objectKeys);
        }
        return resultMap;
    }

    public class SeekObjects {
        public Map<OrderInstance, ObjectValue> values;
        public boolean end;

        public SeekObjects(Map<OrderInstance, ObjectValue> values, boolean end) {
            this.values = values;
            this.end = end;
        }

        public SeekObjects(boolean end, Map<ObjectInstance, DataObject> values) {
            this((Map<OrderInstance,ObjectValue>)(Map<? extends OrderInstance,? extends ObjectValue>)values, end);
        }

        public SeekObjects(boolean end) {
            this(new HashMap<OrderInstance, ObjectValue>(), end);
        }

        public SeekObjects add(OrderInstance order, ObjectValue value, boolean down) {
            return new SeekObjects(BaseUtils.override(values, Collections.singletonMap(order, value)), this.end || down);
        }

        public SeekObjects remove(ObjectInstance object) {
            return new SeekObjects(BaseUtils.filterNotKeys(values, Collections.singleton(object)), end);
        }

        public SeekObjects reverse() {
            return new SeekObjects(values, !end);
        }

        // возвращает OrderInstance из orderSeeks со значениями, а также если есть parent, то parent'ы
        public OrderedMap<Map<ObjectInstance, DataObject>, Map<OrderInstance, ObjectValue>>
        executeOrders(SQLSession session, QueryEnvironment env, Modifier modifier, BaseClass baseClass, int readSize, boolean down) throws SQLException {
            assert !isInTree();

            Map<ObjectInstance, KeyExpr> mapKeys = getMapKeys();

            // assertion что group.orders начинается с orderSeeks
            OrderedMap<OrderInstance, Boolean> orders;
            if (readSize == 1)
                orders = GroupObjectInstance.this.orders.moveStart(values.keySet());
            else
                orders = GroupObjectInstance.this.orders;

            assert orders.starts(values.keySet());

            Map<OrderInstance, Expr> orderExprs = new HashMap<OrderInstance, Expr>();
            for (Map.Entry<OrderInstance, Boolean> toOrder : orders.entrySet())
                orderExprs.put(toOrder.getKey(), toOrder.getKey().getExpr(mapKeys, modifier));

            Where orderWhere = end?Where.FALSE:Where.TRUE; // строим условия на упорядочивание
            for (Map.Entry<OrderInstance, Boolean> toOrder : orders.reverse().entrySet()) {
                ObjectValue toSeek = values.get(toOrder.getKey());
                if (toSeek != null)
                    orderWhere = toSeek.order(orderExprs.get(toOrder.getKey()), toOrder.getValue(), orderWhere);
            }

            if(!down)
                orderWhere = orderWhere.not();

            if (readSize == 1) { // в частном случае если есть "висячие" ключи не в фильтре и нужна одна запись ставим равно вместо >
                Collection<DataObjectInstance> freeObjects = getFreeDataObjects();
                for(DataObjectInstance freeObject : freeObjects) {
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
        private Map.Entry<Map<ObjectInstance, DataObject>, Map<OrderInstance, ObjectValue>> readObjects(SQLSession session, QueryEnvironment env, Modifier modifier, BaseClass baseClass) throws SQLException {
            OrderedMap<Map<ObjectInstance, DataObject>, Map<OrderInstance, ObjectValue>> result = executeOrders(session, env, modifier, baseClass, 1, !end);
            if (result.size() == 0)
                result = new SeekObjects(values, !end).executeOrders(session, env, modifier, baseClass, 1, end);
            if (result.size() > 0)
                return result.singleEntry();

            else
                return null;
        }

        public Map<ObjectInstance, DataObject> readKeys(SQLSession session, QueryEnvironment env, Modifier modifier, BaseClass baseClass) throws SQLException {
            Map.Entry<Map<ObjectInstance, DataObject>, Map<OrderInstance, ObjectValue>> objects = readObjects(session, env, modifier, baseClass);
            if (objects != null)
                return objects.getKey();
            else
                return new HashMap<ObjectInstance, DataObject>();
        }

        public SeekObjects readValues(SQLSession session, QueryEnvironment env, Modifier modifier, BaseClass baseClass) throws SQLException {
            Map.Entry<Map<ObjectInstance, DataObject>, Map<OrderInstance, ObjectValue>> objects = readObjects(session, env, modifier, baseClass);
            if (objects != null)
                return new SeekObjects(objects.getValue(), end);
            else
                return new SeekObjects(false);
        }
    }

    public NoPropertyTableUsage<ObjectInstance> createKeyTable() {
        return new NoPropertyTableUsage<ObjectInstance>(GroupObjectInstance.getObjects(getUpTreeGroups()), new Type.Getter<ObjectInstance>() {
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
