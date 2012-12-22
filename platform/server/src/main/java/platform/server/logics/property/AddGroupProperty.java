package platform.server.logics.property;

import platform.base.col.ListFact;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.*;
import platform.server.data.expr.Expr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

public abstract class AddGroupProperty<I extends PropertyInterface> extends GroupProperty<I> {

    protected final CalcPropertyInterfaceImplement<I> groupProperty;

    protected AddGroupProperty(String sID, String caption, ImSet<I> innerInterfaces, ImCol<? extends CalcPropertyInterfaceImplement<I>> groupInterfaces, CalcPropertyInterfaceImplement<I> groupProperty) {
        super(sID, caption, innerInterfaces, groupInterfaces);
        this.groupProperty = groupProperty;
    }

    protected AddGroupProperty(String sID, String caption, ImCol<? extends CalcPropertyInterfaceImplement<I>> interfaces, CalcProperty<I> property) {
        this(sID, caption, property.interfaces, interfaces, property.getImplement());
    }

    protected ImList<CalcPropertyInterfaceImplement<I>> getProps() {
        return ListFact.singleton(groupProperty);
    }

    protected ImOrderMap<CalcPropertyInterfaceImplement<I>, Boolean> getOrders() {
        return MapFact.<CalcPropertyInterfaceImplement<I>, Boolean>EMPTYORDER();
    }

    protected Expr calculateIncrementExpr(ImMap<Interface<I>, ? extends Expr> joinImplement, PropertyChanges propChanges, Expr prevExpr, WhereBuilder changedWhere) {
        // если нужна инкрементность
        ImMap<I, Expr> mapKeys = getGroupKeys(joinImplement); // изначально чтобы новые и старые группировочные записи в одном контексте были

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

    protected Expr calculateNewExpr(ImMap<Interface<I>, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges) {
        ImMap<I, Expr> mapKeys = getGroupKeys(joinImplement);
        return GroupExpr.create(getGroupImplements(mapKeys, propClasses, propChanges), groupProperty.mapExpr(mapKeys, propClasses, propChanges, null), getGroupType(), joinImplement);
    }

    protected Expr calculateExpr(ImMap<Interface<I>, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        assert assertPropClasses(propClasses, propChanges, changedWhere);
        if(!hasChanges(propChanges) || (changedWhere==null && noIncrement()))
            return calculateNewExpr(joinImplement, propClasses, propChanges);

        assert !propClasses;
        return calculateIncrementExpr(joinImplement, propChanges, getExpr(joinImplement), changedWhere);
    }

    protected abstract Expr getChangedExpr(Expr changedExpr, Expr changedPrevExpr, Expr prevExpr, ImMap<Interface<I>, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere);
}
