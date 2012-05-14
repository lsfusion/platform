package platform.server.logics.property;

import platform.base.OrderedMap;
import platform.server.data.expr.Expr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class AddGroupProperty<I extends PropertyInterface> extends GroupProperty<I> {

    protected final CalcPropertyInterfaceImplement<I> groupProperty;

    protected AddGroupProperty(String sID, String caption, Collection<I> innerInterfaces, Collection<? extends CalcPropertyInterfaceImplement<I>> groupInterfaces, CalcPropertyInterfaceImplement<I> groupProperty) {
        super(sID, caption, innerInterfaces, groupInterfaces);
        this.groupProperty = groupProperty;
    }

    protected AddGroupProperty(String sID, String caption, Collection<? extends CalcPropertyInterfaceImplement<I>> interfaces, CalcProperty<I> property) {
        this(sID, caption, property.interfaces, interfaces, property.getImplement());
    }

    protected List<CalcPropertyInterfaceImplement<I>> getProps() {
        return Collections.singletonList(groupProperty);
    }

    protected OrderedMap<CalcPropertyInterfaceImplement<I>, Boolean> getOrders() {
        return new OrderedMap<CalcPropertyInterfaceImplement<I>, Boolean>();
    }

    protected Expr calculateIncrementExpr(Map<Interface<I>, ? extends Expr> joinImplement, PropertyChanges propChanges, Expr prevExpr, WhereBuilder changedWhere) {
        // если нужна инкрементность
        Map<I, Expr> mapKeys = getGroupKeys(joinImplement); // изначально чтобы новые и старые группировочные записи в одном контексте были

        // новые группировочные записи
        WhereBuilder changedGroupWhere = new WhereBuilder();
        Expr changedExpr = GroupExpr.create(getGroupImplements(mapKeys, propChanges, changedGroupWhere), groupProperty.mapExpr(mapKeys, propChanges, changedGroupWhere), changedGroupWhere.toWhere(), getGroupType(), joinImplement);

        // старые группировочные записи
        Expr changedPrevExpr = GroupExpr.create(getGroupImplements(mapKeys, PropertyChanges.EMPTY), groupProperty.mapExpr(mapKeys), changedGroupWhere.toWhere(), getGroupType(), joinImplement);

        return getChangedExpr(changedExpr, changedPrevExpr, prevExpr, joinImplement, propChanges, changedWhere);
    }

    protected boolean noIncrement() {
        return !isStored();
    }

    protected Expr calculateNewExpr(Map<Interface<I>, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges) {
        Map<I, Expr> mapKeys = getGroupKeys(joinImplement);

        return GroupExpr.create(getGroupImplements(mapKeys, propClasses, propChanges), getExprImplements(mapKeys, propClasses, propChanges),
                                getOrderImplements(mapKeys, propClasses, propChanges), getGroupType(), joinImplement);
    }

    protected Expr calculateExpr(Map<Interface<I>, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        assert assertPropClasses(propClasses, propChanges, changedWhere);
        if(!hasChanges(propChanges) || (changedWhere==null && noIncrement()))
            return calculateNewExpr(joinImplement, propClasses, propChanges);

        assert !propClasses;
        return calculateIncrementExpr(joinImplement, propChanges, getExpr(joinImplement), changedWhere);
    }

    protected abstract Expr getChangedExpr(Expr changedExpr, Expr changedPrevExpr, Expr prevExpr, Map<Interface<I>, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere);
}
