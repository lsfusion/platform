package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public abstract class IncrementUnionProperty extends UnionProperty {

    protected IncrementUnionProperty(LocalizedString caption, ImOrderSet<Interface> interfaces) {
        super(caption, interfaces);
    }

    protected abstract Expr calculateNewExpr(ImMap<Interface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere);
    protected abstract Expr calculateIncrementExpr(ImMap<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges, Expr prevExpr, WhereBuilder changedWhere);

    @Override
    protected Expr calculateExpr(ImMap<Interface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        assert assertPropClasses(calcType, propChanges, changedWhere);
        if(!(hasChanges(propChanges) && isStored())) // we need is stored after because initStored uses calcClassValueWhere, which uses calculate logics, and isStored may be not initialize at this point
            return calculateNewExpr(joinImplement, calcType, propChanges, changedWhere);

        assert calcType.isExpr();
        return calculateIncrementExpr(joinImplement, propChanges, getPrevExpr(joinImplement, calcType, propChanges), changedWhere);
    }
}
