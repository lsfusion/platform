package platform.server.logics.property.derived;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.data.Field;
import platform.server.data.PropertyField;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.OrderExpr;
import platform.server.data.expr.query.OrderType;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.property.*;
import platform.server.session.Changes;
import platform.server.session.Modifier;

import java.util.*;

public class OrderProperty<T extends PropertyInterface> extends SimpleIncrementProperty<OrderProperty.Interface<T>> {

    protected final OrderType orderType;

    protected final Collection<T> innerInterfaces;
    protected final List<PropertyInterfaceImplement<T>> props;
    protected final OrderedMap<PropertyInterfaceImplement<T>,Boolean> orders;
    protected final Collection<PropertyInterfaceImplement<T>> partitions;
    protected boolean includeLast;

    public OrderProperty(String sID, String caption, OrderType orderType, Property<T> property, Collection<PropertyInterfaceImplement<T>> partitions, OrderedMap<PropertyInterfaceImplement<T>, Boolean> orders, List<PropertyInterfaceImplement<T>> extras, boolean includeLast) {
        this(sID, caption, orderType, property.interfaces, BaseUtils.mergeList(Collections.singletonList(property.getImplement()), extras), partitions, orders, includeLast);
    }

    public OrderProperty(String sID, String caption, OrderType orderType, Collection<T> innerInterfaces, List<PropertyInterfaceImplement<T>> props, Collection<PropertyInterfaceImplement<T>> partitions, OrderedMap<PropertyInterfaceImplement<T>, Boolean> orders, boolean includeLast) {
        super(sID, caption, getInterfaces(innerInterfaces));
        this.innerInterfaces = innerInterfaces;
        this.props = props;
        this.orders = orders;
        this.orderType = orderType;
        this.partitions = partitions;
        this.includeLast = includeLast;
    }

    @Override
    protected void fillDepends(Set<Property> depends, boolean derived) {
        fillDepends(depends, orders.keySet());
        fillDepends(depends, partitions);
        fillDepends(depends, props);
    }

    public static class Interface<T extends PropertyInterface> extends PropertyInterface<Interface<T>> {
        public T propertyInterface;

        public Interface(int ID,T propertyInterface) {
            super(ID);
            this.propertyInterface = propertyInterface;
        }
    }

    private static <T extends PropertyInterface> List<Interface<T>> getInterfaces(Collection<T> innerInterfaces) {
        List<Interface<T>> interfaces = new ArrayList<Interface<T>>();
        for(T propertyInterface : innerInterfaces)
            interfaces.add(new Interface<T>(interfaces.size(), propertyInterface));
        return interfaces;
    }

    public Map<Interface<T>,T> getMapInterfaces() {
        Map<Interface<T>,T> mapInterfaces = new HashMap<Interface<T>, T>();
        for(Interface<T> propertyInterface : interfaces)
            mapInterfaces.put(propertyInterface,propertyInterface.propertyInterface);
        return mapInterfaces;
    }

    // кривовать как и в GroupProperty, перетягивание на себя функций компилятора (то есть с третьего ограничивается второй), но достаточно хороший case оптимизации
    protected Map<T, ? extends Expr> getGroupKeys(Map<Interface<T>, ? extends Expr> joinImplement, Map<KeyExpr, Expr> mapExprs) {
        Map<T, KeyExpr> mapKeys = KeyExpr.getMapKeys(innerInterfaces);

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

    protected Map<PropertyInterfaceImplement<T>,Expr> getPartitionImplements(Map<T, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        Map<PropertyInterfaceImplement<T>,Expr> result = new HashMap<PropertyInterfaceImplement<T>,Expr>();
        for(PropertyInterfaceImplement<T> partition : partitions)
            result.put(partition,partition.mapExpr(joinImplement, modifier, changedWhere));
        return result;
    }

    protected OrderedMap<Expr, Boolean> getOrderImplements(Map<T, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        OrderedMap<Expr, Boolean> result = new OrderedMap<Expr, Boolean>();
        for(Map.Entry<PropertyInterfaceImplement<T>, Boolean> order : orders.entrySet())
            result.put(order.getKey().mapExpr(joinImplement, modifier, changedWhere), order.getValue());
        return result;
    }

    protected List<Expr> getExprImplements(Map<T, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        List<Expr> exprs = new ArrayList<Expr>();
        for(PropertyInterfaceImplement<T> extra : props)
            exprs.add(extra.mapExpr(joinImplement, modifier, changedWhere));
        return exprs;
    }

    protected Expr calculateExpr(Map<Interface<T>, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {

        Map<KeyExpr, Expr> mapExprs = new HashMap<KeyExpr, Expr>();
        Map<T, ? extends Expr> mapKeys = getGroupKeys(joinImplement, mapExprs);

        WhereBuilder orderWhere = cascadeWhere(changedWhere);
        Map<PropertyInterfaceImplement<T>,Expr> partitionImplements = getPartitionImplements(mapKeys, modifier, orderWhere);
        OrderedMap<Expr, Boolean> orderExprs = getOrderImplements(mapKeys, modifier, orderWhere);
        List<Expr> exprs = getExprImplements(mapKeys, modifier, orderWhere);

        if(changedWhere!=null) { // изменившиеся ряды (orderWhere) -> ряды с изменившимися partition'ами -> изменившиеся записи
            changedWhere.add(getPartitionWhere(orderWhere.toWhere(), partitionImplements, exprs, orderExprs, mapExprs));
            changedWhere.add(getPartitionWhere(orderWhere.toWhere(), getPartitionImplements(mapKeys, defaultModifier, null),
                    getExprImplements(mapKeys, defaultModifier, null), getOrderImplements(mapKeys, defaultModifier, null), mapExprs));
        }

        return OrderExpr.create(orderType, exprs, orderExprs, new HashSet<Expr>(partitionImplements.values()), mapExprs);
    }

    private Where getPartitionWhere(Where where, Map<PropertyInterfaceImplement<T>,Expr> partitionImplements, List<Expr> exprs, OrderedMap<Expr, Boolean> orders, Map<KeyExpr, Expr> mapExprs) {
        return GroupExpr.create(partitionImplements, where.and(Expr.getWhere(exprs)).and(Expr.getWhere(orders.keySet())), partitionImplements).getWhere().map(mapExprs);
    }
}
