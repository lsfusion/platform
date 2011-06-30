package platform.server.logics.property.derived;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.OrderExpr;
import platform.server.data.expr.query.OrderType;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.*;
import platform.server.session.Changes;
import platform.server.session.Modifier;

import java.util.*;

public class OrderProperty<T extends PropertyInterface> extends SimpleIncrementProperty<OrderProperty.Interface<T>> {

    public OrderProperty(String sID, String caption, OrderType orderType, Property<T> property, Collection<PropertyInterfaceImplement<T>> partitions, OrderedMap<PropertyInterfaceImplement<T>, Boolean> orders, boolean includeLast) {
        super(sID, caption, getInterfaces(property));
        this.orderType = orderType;
        this.property = property;
        this.partitions = partitions;
        this.orders = orders;
        this.includeLast = includeLast;
    }

    Property<T> property;
    Collection<PropertyInterfaceImplement<T>> partitions;
    OrderedMap<PropertyInterfaceImplement<T>,Boolean> orders;
    boolean includeLast;

    private final OrderType orderType;

    public static class Interface<T extends PropertyInterface> extends PropertyInterface<Interface<T>> {
        public T propertyInterface;

        public Interface(int ID,T propertyInterface) {
            super(ID);
            this.propertyInterface = propertyInterface;
        }
    }

    private static <T extends PropertyInterface> List<Interface<T>> getInterfaces(Property<T> property) {
        List<Interface<T>> interfaces = new ArrayList<Interface<T>>();
        for(T propertyInterface : property.interfaces)
            interfaces.add(new Interface<T>(interfaces.size(),propertyInterface));
        return interfaces;
    }

    @Override
    protected void fillDepends(Set<Property> depends, boolean derived) {
        depends.add(property);
        fillDepends(depends,partitions);
        fillDepends(depends,orders.keySet());
    }

    public Map<Interface<T>,T> getMapInterfaces() {
        Map<Interface<T>,T> mapInterfaces = new HashMap<Interface<T>, T>();
        for(Interface<T> propertyInterface : interfaces)
            mapInterfaces.put(propertyInterface,propertyInterface.propertyInterface);
        return mapInterfaces;
    }

    private Map<PropertyInterfaceImplement<T>,Expr> getPartitionImplements(Map<T, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        Map<PropertyInterfaceImplement<T>,Expr> result = new HashMap<PropertyInterfaceImplement<T>,Expr>();
        for(PropertyInterfaceImplement<T> partition : partitions)
            result.put(partition,partition.mapExpr(joinImplement, modifier, changedWhere));
        return result;
    }

    // кривовать как и в GroupProperty, перетягивание на себя функций компилятора (то есть с третьего ограничивается второй), но достаточно хороший case оптимизации
    private Map<T, ? extends Expr> getGroupKeys(Map<Interface<T>, ? extends Expr> joinImplement, Map<KeyExpr, Expr> mapExprs) {
        Map<T, KeyExpr> mapKeys = property.getMapKeys();
        //mapExprs.putAll(BaseUtils.crossJoin(BaseUtils.join(getMapInterfaces(),mapKeys),joinImplement));

        Map<T, Expr> result = new HashMap<T, Expr>();
        // читаем value из joinImplement, затем фильтруем partitions'ами
        for(Map.Entry<Interface<T>,? extends Expr> mapExpr : joinImplement.entrySet())
            if(mapExpr.getValue().isValue() && partitions.contains(mapExpr.getKey().propertyInterface)) {
                result.put(mapExpr.getKey().propertyInterface, mapExpr.getValue());
            } else {
                KeyExpr keyExpr = mapKeys.get(mapExpr.getKey().propertyInterface);
                result.put(mapExpr.getKey().propertyInterface, keyExpr);
                mapExprs.put(keyExpr, mapExpr.getValue());
            }
        return result;
    }

    protected Expr calculateExpr(Map<Interface<T>, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {

        Map<KeyExpr, Expr> mapExprs = new HashMap<KeyExpr, Expr>();
        Map<T, ? extends Expr> mapKeys = getGroupKeys(joinImplement, mapExprs);

        WhereBuilder orderWhere = cascadeWhere(changedWhere);
        Map<PropertyInterfaceImplement<T>,Expr> partitionImplements = getPartitionImplements(mapKeys, modifier, orderWhere);
        OrderedMap<Expr, Boolean> orderExprs = new OrderedMap<Expr, Boolean>();
        for(Map.Entry<PropertyInterfaceImplement<T>,Boolean> order : orders.entrySet())
            orderExprs.put(order.getKey().mapExpr(mapKeys, modifier, orderWhere), order.getValue());
        Expr propertyExpr = property.getExpr(mapKeys, modifier, orderWhere);

        if(changedWhere!=null) { // изменившиеся ряды (orderWhere) -> ряды с изменившимися partition'ами -> изменившиеся записи
            changedWhere.add(GroupExpr.create(partitionImplements, orderWhere.toWhere(), partitionImplements).getWhere().map(mapExprs));
            changedWhere.add(GroupExpr.create(getPartitionImplements(mapKeys, defaultModifier, null), orderWhere.toWhere(), partitionImplements).getWhere().map(mapExprs));
        }

        return OrderExpr.create(orderType, propertyExpr, orderExprs, new HashSet<Expr>(partitionImplements.values()), mapExprs);
    }
}
