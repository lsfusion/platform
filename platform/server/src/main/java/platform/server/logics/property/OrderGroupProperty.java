package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.data.expr.Expr;
import platform.server.data.expr.query.AggrExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class OrderGroupProperty<I extends PropertyInterface> extends GroupProperty<I> {

    private final List<CalcPropertyInterfaceImplement<I>> props;
    protected List<CalcPropertyInterfaceImplement<I>> getProps() {
        return props;
    }

    private final GroupType groupType;
    protected GroupType getGroupType() {
        return groupType;
    }

    private final OrderedMap<CalcPropertyInterfaceImplement<I>, Boolean> orders;
    private final boolean ordersNotNull;
    protected OrderedMap<CalcPropertyInterfaceImplement<I>, Boolean> getOrders() {
        return orders;
    }

    public OrderGroupProperty(String sID, String caption, Collection<I> innerInterfaces, Collection<? extends CalcPropertyInterfaceImplement<I>> groupInterfaces, List<CalcPropertyInterfaceImplement<I>> props, GroupType groupType, OrderedMap<CalcPropertyInterfaceImplement<I>, Boolean> orders, boolean ordersNotNull) {
        super(sID, caption, innerInterfaces, groupInterfaces);
        this.props = props;
        this.groupType = groupType;
        this.orders = orders;
        this.ordersNotNull = ordersNotNull;

        finalizeInit();
        
        assert !props.contains(null);
    }

    protected Expr calculateExpr(Map<Interface<I>, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        // если нужна инкрементность
        Map<I, Expr> mapKeys = getGroupKeys(joinImplement); // изначально чтобы новые и старые группировочные записи в одном контексте были

        WhereBuilder changedGroupWhere = cascadeWhere(changedWhere);

        Map<Interface<I>, Expr> groups = getGroupImplements(mapKeys, propClasses, propChanges, changedGroupWhere);
        List<Expr> exprs = getExprImplements(mapKeys, propClasses, propChanges, changedGroupWhere);
        OrderedMap<Expr, Boolean> orders = getOrderImplements(mapKeys, propClasses, propChanges, changedGroupWhere);

        if(changedWhere!=null) {
            assert !propClasses;
            changedWhere.add(getPartitionWhere(changedGroupWhere.toWhere(), groups, exprs, orders, joinImplement));
            changedWhere.add(getPartitionWhere(changedGroupWhere.toWhere(), getGroupImplements(mapKeys, PropertyChanges.EMPTY),
                    getExprImplements(mapKeys, PropertyChanges.EMPTY), getOrderImplements(mapKeys, PropertyChanges.EMPTY), joinImplement));
        }
        return GroupExpr.create(groups, exprs, orders, ordersNotNull, getGroupType(), joinImplement);
    }

    protected boolean useSimpleIncrement() {
        return true;
    }

    protected Where getPartitionWhere(Where where, Map<Interface<I>, Expr> groups, List<Expr> exprs, OrderedMap<Expr, Boolean> orders, Map<Interface<I>, ? extends Expr> joinImplement) {
        return GroupExpr.create(groups, where.and(Expr.getWhere(exprs).and(AggrExpr.getOrderWhere(orders, ordersNotNull))), joinImplement).getWhere();
    }
}
