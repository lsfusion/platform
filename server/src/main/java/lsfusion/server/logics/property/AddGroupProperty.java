package lsfusion.server.logics.property;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.session.PropertyChanges;

public abstract class AddGroupProperty<I extends PropertyInterface> extends GroupProperty<I> {

    public final CalcPropertyInterfaceImplement<I> groupProperty;

    protected AddGroupProperty(String caption, ImSet<I> innerInterfaces, ImCol<? extends CalcPropertyInterfaceImplement<I>> groupInterfaces, CalcPropertyInterfaceImplement<I> groupProperty) {
        super(caption, innerInterfaces, groupInterfaces);
        this.groupProperty = groupProperty;
    }

    protected AddGroupProperty(String caption, ImSet<I> innerInterfaces, ImList<? extends CalcPropertyInterfaceImplement<I>> groupInterfaces, CalcPropertyInterfaceImplement<I> groupProperty) {
        super(caption, innerInterfaces, groupInterfaces);
        this.groupProperty = groupProperty;
    }
    
    protected AddGroupProperty(String caption, ImCol<? extends CalcPropertyInterfaceImplement<I>> interfaces, CalcProperty<I> property) {
        this(caption, property.interfaces, interfaces, property.getImplement());
    }

    public ImList<CalcPropertyInterfaceImplement<I>> getProps() {
        return ListFact.singleton(groupProperty);
    }

    public ImOrderMap<CalcPropertyInterfaceImplement<I>, Boolean> getOrders() {
        return MapFact.<CalcPropertyInterfaceImplement<I>, Boolean>EMPTYORDER();
    }

    public boolean getOrdersNotNull() {
        return false;
    }

    protected Expr calculateIncrementExpr(ImMap<Interface<I>, ? extends Expr> joinImplement, PropertyChanges propChanges, Expr prevExpr, WhereBuilder changedWhere) {
        // если нужна инкрементность
        ImMap<I, Expr> mapKeys = getGroupKeys(joinImplement); // изначально чтобы новые и старые группировочные записи в одном контексте были

        if(checkPrereadNull(mapKeys, CalcType.EXPR, propChanges))
            return Expr.NULL;

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

    protected Expr calculateNewExpr(ImMap<Interface<I>, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges) {
        ImMap<I, Expr> mapKeys = getGroupKeys(joinImplement);

        if(checkPrereadNull(mapKeys, calcType, propChanges))
            return Expr.NULL;

        return GroupExpr.create(getGroupImplements(mapKeys, calcType, propChanges), groupProperty.mapExpr(mapKeys, calcType, propChanges, null), getGroupType(), joinImplement);
    }

    protected Expr calculateExpr(ImMap<Interface<I>, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        assert assertPropClasses(calcType, propChanges, changedWhere);
        if(!hasChanges(propChanges) || (changedWhere==null && noIncrement()))
            return calculateNewExpr(joinImplement, calcType, propChanges);

        assert calcType.isExpr();
        return calculateIncrementExpr(joinImplement, propChanges, getExpr(joinImplement), changedWhere);
    }

    protected abstract Expr getChangedExpr(Expr changedExpr, Expr changedPrevExpr, Expr prevExpr, ImMap<Interface<I>, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere);
}
