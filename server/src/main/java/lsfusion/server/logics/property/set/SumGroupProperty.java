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

    @Override
    protected boolean noIncrement() {
        return false;
    }

    @Override
    public GroupType getGroupType() {
        return GroupType.SUM;
    }
}
