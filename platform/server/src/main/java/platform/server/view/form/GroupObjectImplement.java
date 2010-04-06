package platform.server.view.form;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.interop.Order;
import platform.interop.form.RemoteFormInterface;
import platform.server.data.query.Query;
import platform.server.data.query.MapKeysInterface;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.Expr;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;
import platform.server.logics.NullValue;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.Property;
import platform.server.session.DataSession;
import platform.server.session.Changes;
import platform.server.session.Modifier;
import platform.server.view.form.filter.Filter;

import java.sql.SQLException;
import java.util.*;

public class GroupObjectImplement implements MapKeysInterface<ObjectImplement> {

    public Collection<ObjectImplement> objects;

    // глобальный идентификатор чтобы писать во ViewTable
    public final int ID;

    public GroupObjectImplement(int ID,Collection<ObjectImplement> objects,int order,int pageSize,boolean gridClassView) {

        assert (ID < RemoteFormInterface.GID_SHIFT);

        this.ID = ID;
        this.order = order;
        this.pageSize = pageSize;
        this.gridClassView = gridClassView;
        this.objects = objects;

        for(ObjectImplement object : objects)
            object.groupTo = this;
    }

    public Map<ObjectImplement, KeyExpr> getMapKeys() {
        return KeyExpr.getMapKeys(objects);
    }

    public Integer order = 0;

    // классовый вид включен или нет
    public boolean gridClassView = true;

    // закэшированные

    public Set<Filter> setFilters = null;
    public Set<Filter> getSetFilters() {
        if(setFilters==null)
            setFilters = BaseUtils.mergeSet(BaseUtils.mergeSet(fixedFilters,userFilters),regularFilters);
        return setFilters;
    }

    // вообще все фильтры
    public Set<Filter> fixedFilters = new HashSet<Filter>();

    private Set<Filter> userFilters = new HashSet<Filter>();
    public void clearUserFilters() {
        userFilters.clear();

        setFilters = null;
        updated |= UPDATED_FILTER;
    }
    public void addUserFilter(Filter addFilter) {
        userFilters.add(addFilter);

        setFilters = null;
        updated |= UPDATED_FILTER;
    }

    private Set<Filter> regularFilters = new HashSet<Filter>();
    public void addRegularFilter(Filter filter) {
        regularFilters.add(filter);

        setFilters = null;
        updated |= UPDATED_FILTER;
    }

    public void removeRegularFilter(Filter filter) {
        regularFilters.remove(filter);

        setFilters = null;
        updated |= UPDATED_FILTER;
    }

    // с активным интерфейсом
    public Set<Filter> filters = new HashSet<Filter>();

    // обертку потому как сложный assertion
    public OrderedMap<OrderView,Boolean> setOrders = null;
    public OrderedMap<OrderView,Boolean> getSetOrders() {
        if(setOrders==null) {
            setOrders = new OrderedMap<OrderView,Boolean>(userOrders);
            for(ObjectImplement object : objects)
                if(!(setOrders.containsKey(object)))
                    setOrders.put(object,false);
        }
        return setOrders;
    }
    private OrderedMap<OrderView,Boolean> userOrders = new OrderedMap<OrderView, Boolean>();
    public void changeOrder(OrderView property, Order modiType) {
        if (modiType == Order.REPLACE)
            userOrders = new OrderedMap<OrderView, Boolean>();

        if (modiType == Order.REMOVE)
            userOrders.remove(property);
        else
        if (modiType == Order.DIR)
            userOrders.put(property,!userOrders.get(property));
        else
            userOrders.put(property,false);

        setOrders = null;
        updated |= UPDATED_ORDER;
    }

    // с активным интерфейсом
    OrderedMap<OrderView,Boolean> orders = new OrderedMap<OrderView, Boolean>();

    public void fillUpdateProperties(Set<Property> properties) {
        for(Filter filter : filters)
            filter.fillProperties(properties);
        for(OrderView order : orders.keySet())
            order.fillProperties(properties);
    }

    boolean upKeys, downKeys;
    OrderedMap<Map<ObjectImplement,DataObject>,Map<OrderView,ObjectValue>> keys = new OrderedMap<Map<ObjectImplement, DataObject>, Map<OrderView, ObjectValue>>();

    // 0 !!! - изменился объект, 1 - класс объекта, 2 !!! - отбор, 3 !!! - хоть один класс, 4 !!! - классовый вид

    public final static int UPDATED_OBJECT = (1);
    public final static int UPDATED_KEYS = (1 << 2);
    public final static int UPDATED_GRIDCLASS = (1 << 3);
    public final static int UPDATED_CLASSVIEW = (1 << 4);
    public final static int UPDATED_ORDER = (1 << 5);
    public final static int UPDATED_FILTER = (1 << 6);

    public int updated = UPDATED_GRIDCLASS | UPDATED_CLASSVIEW | UPDATED_ORDER | UPDATED_FILTER;

    public int pageSize = 50;

    Map<ObjectImplement,ObjectValue> getGroupObjectValue() {
        Map<ObjectImplement,ObjectValue> result = new HashMap<ObjectImplement, ObjectValue>();
        for(ObjectImplement object : objects)
            result.put(object,object.getObjectValue());
        return result;
    }

    public Map<ObjectImplement,DataObject> findGroupObjectValue(Map<ObjectImplement,Object> map) {
        for(Map<ObjectImplement,DataObject> keyRow : keys.keySet()) {
            boolean equal = true;
            for(Map.Entry<ObjectImplement,DataObject> keyEntry : keyRow.entrySet())
                if(!keyEntry.getValue().object.equals(map.get(keyEntry.getKey()))) {
                    equal = false;
                    break;
                }
            if(equal)
                return keyRow;
        }

        throw new RuntimeException("key not found");
    }

    // получает Set группы
    public Set<GroupObjectImplement> getClassGroup() {

        Set<GroupObjectImplement> result = new HashSet<GroupObjectImplement>();
        result.add(this);
        return result;
    }

    Where getFilterWhere(Map<ObjectImplement, ? extends Expr> mapKeys, Set<GroupObjectImplement> classGroup, Modifier<? extends Changes> modifier) throws SQLException {
        Where where = Where.TRUE;
        for(Filter filt : filters)
            where = where.and(filt.getWhere(mapKeys, classGroup, modifier));
        return where;
    }

    Where getClassWhere(Map<ObjectImplement, ? extends Expr> mapKeys, Set<GroupObjectImplement> classGroup, Modifier<? extends Changes> modifier) {
        Where where = Where.TRUE;
        for(ObjectImplement object : objects)
            where = where.and(DataSession.getIsClassWhere(modifier.getSession(),mapKeys.get(object),object.getGridClass(),null));
        return where;
    }

    Where getWhere(Map<ObjectImplement, ? extends Expr> mapKeys, Set<GroupObjectImplement> classGroup, Modifier<? extends Changes> modifier) throws SQLException {
        return getFilterWhere(mapKeys, classGroup, modifier).and(getClassWhere(mapKeys, classGroup, modifier));
    }

    Map<ObjectImplement,ObjectValue> getNulls() {
        Map<ObjectImplement,ObjectValue> result = new HashMap<ObjectImplement, ObjectValue>();
        for(ObjectImplement object : objects)
            result.put(object, NullValue.instance);
        return result;
    }

    boolean isSolid() {
        Iterator<ObjectImplement> i = objects.iterator();
        boolean read = i.next() instanceof CustomObjectImplement;
        while(i.hasNext())
            if(read != i.next() instanceof CustomObjectImplement)
                return false;
        return true;
    }

    public ObjectImplement getObjectImplement(int objectID) {
        for (ObjectImplement object : objects)
            if (object.ID == objectID)
                return object;
        return null;
    }
}
