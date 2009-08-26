package platform.server.view.form;

import platform.interop.form.RemoteFormInterface;
import platform.interop.Order;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.MapKeysInterface;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.NullValue;
import platform.server.logics.properties.Property;
import platform.server.session.DataSession;
import platform.server.session.TableChanges;
import platform.server.view.form.filter.Filter;
import platform.base.BaseUtils;

import java.sql.SQLException;
import java.util.*;

public class GroupObjectImplement implements MapKeysInterface<ObjectImplement> {

    public Collection<ObjectImplement> objects;

    // глобальный идентификатор чтобы писать во ViewTable
    public final int ID;

    public GroupObjectImplement(int iID,Collection<ObjectImplement> objects,int iOrder,int iPageSize,boolean iGridClassView,boolean iSingleViewType) {

        assert (iID < RemoteFormInterface.GID_SHIFT);

        ID = iID;
        order = iOrder;
        pageSize = iPageSize;
        gridClassView = iGridClassView;
        singleViewType = iSingleViewType;
        this.objects = objects;

        for(ObjectImplement object : objects)
            object.groupTo = this;
    }

    public Map<ObjectImplement, KeyExpr> getMapKeys() {
        return ObjectImplement.getMapKeys(objects);
    }

    public Integer order = 0;

    // классовый вид включен или нет
    public boolean gridClassView = true;
    public boolean singleViewType = false;

    // закэшированные

    private Set<Filter> setFilters = null;
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
    private LinkedHashMap<OrderView,Boolean> setOrders = null;
    public LinkedHashMap<OrderView,Boolean> getSetOrders() {
        if(setOrders==null) {
            setOrders = new LinkedHashMap<OrderView,Boolean>(userOrders);
            for(ObjectImplement object : objects)
                if(!(setOrders.containsKey(object)))
                    setOrders.put(object,false);
        }
        return setOrders;
    }
    private LinkedHashMap<OrderView,Boolean> userOrders = new LinkedHashMap<OrderView, Boolean>();
    public void changeOrder(OrderView property, Order modiType) {
        if (modiType == Order.REPLACE)
            userOrders = new LinkedHashMap<OrderView, Boolean>();

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
    LinkedHashMap<OrderView,Boolean> orders = new LinkedHashMap<OrderView, Boolean>();

    public void fillUpdateProperties(Set<Property> properties) {
        for(Filter filter : filters)
            filter.fillProperties(properties);
        for(OrderView order : orders.keySet())
            order.fillProperties(properties);
    }

    boolean upKeys, downKeys;
    List<Map<ObjectImplement,DataObject>> keys = null;
    // какие ключи активны
    Map<Map<ObjectImplement,DataObject>,Map<OrderView,ObjectValue>> keyOrders = null;

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
        for(Map<ObjectImplement,DataObject> keyRow : keys) {
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

    void fillSourceSelect(JoinQuery<ObjectImplement, ?> query, Set<GroupObjectImplement> classGroup, TableChanges session, Property.TableDepends<? extends Property.TableUsedChanges> depends) throws SQLException {

        for(Filter filt : filters)
            query.and(filt.getWhere(query.mapKeys, classGroup, session, depends));

        // докинем JoinSelect ко всем классам, те которых не было FULL JOIN'ом остальные JoinSelect'ом
        for(ObjectImplement object : objects)
            query.and(DataSession.getIsClassWhere(session,query.mapKeys.get(object),object.getGridClass(),null));
    }

    Map<ObjectImplement,ObjectValue> getNulls() {
        Map<ObjectImplement,ObjectValue> result = new HashMap<ObjectImplement, ObjectValue>();
        for(ObjectImplement object : objects)
            result.put(object, NullValue.instance);
        return result;
    }
}
