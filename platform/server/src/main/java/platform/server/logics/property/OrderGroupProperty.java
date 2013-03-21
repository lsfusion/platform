package platform.server.logics.property;

import platform.base.col.interfaces.immutable.*;
import platform.server.data.expr.Expr;
import platform.server.data.expr.query.AggrExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

public class OrderGroupProperty<I extends PropertyInterface> extends GroupProperty<I> {

    private final ImList<CalcPropertyInterfaceImplement<I>> props;
    public ImList<CalcPropertyInterfaceImplement<I>> getProps() {
        return props;
    }

    private final GroupType groupType;
    public GroupType getGroupType() {
        return groupType;
    }

    private final ImOrderMap<CalcPropertyInterfaceImplement<I>, Boolean> orders;
    private final boolean ordersNotNull;
    public ImOrderMap<CalcPropertyInterfaceImplement<I>, Boolean> getOrders() {
        return orders;
    }

    public OrderGroupProperty(String sID, String caption, ImSet<I> innerInterfaces, ImCol<? extends CalcPropertyInterfaceImplement<I>> groupInterfaces, ImList<CalcPropertyInterfaceImplement<I>> props, GroupType groupType, ImOrderMap<CalcPropertyInterfaceImplement<I>, Boolean> orders, boolean ordersNotNull) {
        super(sID, caption, innerInterfaces, groupInterfaces);
        this.props = props;
        this.groupType = groupType;
        this.orders = orders;
        this.ordersNotNull = ordersNotNull;

        finalizeInit();
    }

    protected Expr calculateExpr(ImMap<Interface<I>, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        // если нужна инкрементность
        ImMap<I, Expr> mapKeys = getGroupKeys(joinImplement); // изначально чтобы новые и старые группировочные записи в одном контексте были

        WhereBuilder changedGroupWhere = cascadeWhere(changedWhere);

        ImMap<Interface<I>, Expr> groups = getGroupImplements(mapKeys, propClasses, propChanges, changedGroupWhere);
        ImList<Expr> exprs = getExprImplements(mapKeys, propClasses, propChanges, changedGroupWhere);
        ImOrderMap<Expr, Boolean> orders = getOrderImplements(mapKeys, propClasses, propChanges, changedGroupWhere);

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

    protected Where getPartitionWhere(Where where, ImMap<Interface<I>, Expr> groups, ImList<Expr> exprs, ImOrderMap<Expr, Boolean> orders, ImMap<Interface<I>, ? extends Expr> joinImplement) {
        return GroupExpr.create(groups, where.and(Expr.getWhere(exprs).and(AggrExpr.getOrderWhere(orders, ordersNotNull))), joinImplement).getWhere();
    }
}
