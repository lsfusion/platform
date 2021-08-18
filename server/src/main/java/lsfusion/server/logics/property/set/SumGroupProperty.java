package lsfusion.server.logics.property.set;

import lsfusion.base.Result;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class SumGroupProperty<I extends PropertyInterface> extends AddGroupProperty<I> {

    public SumGroupProperty(LocalizedString caption, ImSet<I> innerInterfaces, ImCol<? extends PropertyInterfaceImplement<I>> groupInterfaces, PropertyInterfaceImplement<I> property) {
        super(caption, innerInterfaces, groupInterfaces, property);

        finalizeInit();
    }

    public SumGroupProperty(LocalizedString caption, ImSet<I> innerInterfaces, ImList<? extends PropertyInterfaceImplement<I>> groupInterfaces, PropertyInterfaceImplement<I> property) {
        super(caption, innerInterfaces, groupInterfaces, property);

        finalizeInit();
    }
    
    public Expr getChangedExpr(Expr changedExpr, Expr changedPrevExpr, Expr prevExpr, ImMap<Interface<I>, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if(changedWhere!=null) changedWhere.add(changedExpr.getWhere().or(changedPrevExpr.getWhere())); // если хоть один не null
        return changedExpr.diff(changedPrevExpr).sum(getPrevExpr(joinImplement, CalcType.EXPR, propChanges));
    }

    // такая же помошь компилятору как и при getExpr в GroupProperty
    private Where getGroupKeys(PropertyChange<Interface<I>> propertyChange, Result<ImRevMap<I, KeyExpr>> mapKeys, Result<ImMap<I, Expr>> mapValueKeys) {
        ImMap<PropertyInterfaceImplement<I>, Expr> changeValues = propertyChange.getMapExprs().mapKeys(value -> value.implement);

        ImRevMap<I, KeyExpr> innerKeys = KeyExpr.getMapKeys(innerInterfaces);

        Where valueWhere = Where.TRUE();
        ImValueMap<I,Expr> mvMapValueKeys = innerKeys.mapItValues();// есть совместная обработка
        for(int i=0,size=innerKeys.size();i<size;i++) {
            Expr expr = changeValues.get(innerKeys.getKey(i));
            if(expr!=null) {
                mvMapValueKeys.mapValue(i, expr);
                valueWhere = valueWhere.and(innerKeys.getValue(i).compare(expr, Compare.EQUALS));
            } else
                mvMapValueKeys.mapValue(i, innerKeys.getValue(i));
        }

        mapKeys.set(innerKeys);
        mapValueKeys.set(mvMapValueKeys.immutableValue());
        return valueWhere;
    }

    @Override
    protected boolean noIncrement() {
        return false;
    }

    @Override
    public GroupType getGroupType() {
        return GroupType.SUM;
    }
}
