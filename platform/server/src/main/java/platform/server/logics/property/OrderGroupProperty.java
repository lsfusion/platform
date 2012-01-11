package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.data.expr.Expr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class OrderGroupProperty<I extends PropertyInterface> extends GroupProperty<I> {

    private final List<PropertyInterfaceImplement<I>> props;
    protected List<PropertyInterfaceImplement<I>> getProps() {
        return props;
    }

    private final GroupType groupType;
    protected GroupType getGroupType() {
        return groupType;
    }

    private final OrderedMap<PropertyInterfaceImplement<I>, Boolean> orders;
    protected OrderedMap<PropertyInterfaceImplement<I>, Boolean> getOrders() {
        return orders;
    }

    public OrderGroupProperty(String sID, String caption, Collection<I> innerInterfaces, Collection<? extends PropertyInterfaceImplement<I>> groupInterfaces, List<PropertyInterfaceImplement<I>> props, GroupType groupType, OrderedMap<PropertyInterfaceImplement<I>, Boolean> orders) {
        super(sID, caption, innerInterfaces, groupInterfaces);
        this.props = props;
        this.groupType = groupType;
        this.orders = orders;
    }

    public OrderGroupProperty(String sID, String caption, Collection<? extends PropertyInterfaceImplement<I>> interfaces, Property<I> property, List<PropertyInterfaceImplement<I>> extras, GroupType groupType, OrderedMap<PropertyInterfaceImplement<I>, Boolean> orders) {
        this(sID, caption, property.interfaces, interfaces, BaseUtils.addList(property.getImplement(), extras), groupType, orders);
    }


    protected Expr calculateExpr(Map<Interface<I>, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        // если нужна инкрементность
        Map<I, Expr> mapKeys = getGroupKeys(joinImplement); // изначально чтобы новые и старые группировочные записи в одном контексте были

        WhereBuilder changedGroupWhere = cascadeWhere(changedWhere);

        Map<Interface<I>, Expr> groups = getGroupImplements(mapKeys, propChanges, changedGroupWhere);
        List<Expr> exprs = getExprImplements(mapKeys, propChanges, changedGroupWhere);
        OrderedMap<Expr, Boolean> orders = getOrderImplements(mapKeys, propChanges, changedGroupWhere);

        if(changedWhere!=null) {
            changedWhere.add(getPartitionWhere(changedGroupWhere.toWhere(), groups, exprs, orders, joinImplement));
            changedWhere.add(getPartitionWhere(changedGroupWhere.toWhere(), getGroupImplements(mapKeys, PropertyChanges.EMPTY),
                    getExprImplements(mapKeys, PropertyChanges.EMPTY), getOrderImplements(mapKeys, PropertyChanges.EMPTY), joinImplement));
        }
        return GroupExpr.create(groups, exprs, orders, getGroupType(), joinImplement);
    }

    protected boolean useSimpleIncrement() {
        return true;
    }

    protected Where getPartitionWhere(Where where, Map<Interface<I>, Expr> groups, List<Expr> exprs, OrderedMap<Expr, Boolean> orders, Map<Interface<I>, ? extends Expr> joinImplement) {
        return GroupExpr.create(groups, where.and(Expr.getWhere(exprs).and(Expr.getWhere(orders.keySet()))), joinImplement).getWhere();
    }
}
