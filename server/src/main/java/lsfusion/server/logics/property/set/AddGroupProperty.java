package lsfusion.server.logics.property.set;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.infer.CalcClassType;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public abstract class AddGroupProperty<I extends PropertyInterface> extends GroupProperty<I> {

    public final PropertyInterfaceImplement<I> groupProperty;

    protected AddGroupProperty(LocalizedString caption, ImSet<I> innerInterfaces, ImCol<? extends PropertyInterfaceImplement<I>> groupInterfaces, PropertyInterfaceImplement<I> groupProperty) {
        super(caption, innerInterfaces, groupInterfaces);
        this.groupProperty = groupProperty;
    }

    protected AddGroupProperty(LocalizedString caption, ImSet<I> innerInterfaces, ImList<? extends PropertyInterfaceImplement<I>> groupInterfaces, PropertyInterfaceImplement<I> groupProperty) {
        super(caption, innerInterfaces, groupInterfaces);
        this.groupProperty = groupProperty;
    }
    
    protected AddGroupProperty(LocalizedString caption, ImCol<? extends PropertyInterfaceImplement<I>> interfaces, Property<I> property) {
        this(caption, property.interfaces, interfaces, property.getImplement());
    }

    public ImList<PropertyInterfaceImplement<I>> getProps() {
        return ListFact.singleton(groupProperty);
    }

    public ImOrderMap<PropertyInterfaceImplement<I>, Boolean> getOrders() {
        return MapFact.EMPTYORDER();
    }

    public boolean getOrdersNotNull() {
        return false;
    }

    protected Expr calculateIncrementExpr(ImMap<Interface<I>, ? extends Expr> joinImplement, PropertyChanges propChanges, Expr prevExpr, WhereBuilder changedWhere) {
        // если нужна инкрементность
        ImMap<I, Expr> mapKeys = getGroupKeys(joinImplement); // изначально чтобы новые и старые группировочные записи в одном контексте были

        if(checkPrereadNull(mapKeys, CalcType.EXPR, propChanges))
            return Expr.NULL();

        // новые группировочные записи
        WhereBuilder changedGroupWhere = new WhereBuilder();
        Expr changedExpr = GroupExpr.create(getGroupImplements(mapKeys, propChanges, changedGroupWhere), groupProperty.mapExpr(mapKeys, propChanges, changedGroupWhere), changedGroupWhere.toWhere(), getGroupType(), joinImplement);

        // старые группировочные записи
        Expr changedPrevExpr = GroupExpr.create(getGroupImplements(mapKeys, getPrevPropChanges(propChanges)), groupProperty.mapExpr(mapKeys, getPrevPropChanges(propChanges)), changedGroupWhere.toWhere(), getGroupType(), joinImplement);

        return getChangedExpr(changedExpr, changedPrevExpr, prevExpr, joinImplement, propChanges, changedWhere);
    }

    protected boolean noIncrement() {
        return !isStored();
    }

    protected Expr calculateNewExpr(ImMap<Interface<I>, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges) {
        ImMap<I, Expr> mapKeys = getGroupKeys(joinImplement);

        if(checkPrereadNull(mapKeys, calcType, propChanges))
            return Expr.NULL();

        return GroupExpr.create(getGroupImplements(mapKeys, calcType, propChanges), groupProperty.mapExpr(mapKeys, calcType, propChanges, null), getGroupType(), joinImplement, calcType instanceof CalcClassType);
    }

    protected Expr calculateExpr(ImMap<Interface<I>, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        assert assertPropClasses(calcType, propChanges, changedWhere);
        if(!hasChanges(propChanges) || (changedWhere==null && noIncrement()))
            return calculateNewExpr(joinImplement, calcType, propChanges);

        assert calcType.isExpr();
        return calculateIncrementExpr(joinImplement, propChanges, getPrevExpr(joinImplement, calcType, propChanges), changedWhere);
    }

    protected abstract Expr getChangedExpr(Expr changedExpr, Expr changedPrevExpr, Expr prevExpr, ImMap<Interface<I>, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere);
}
