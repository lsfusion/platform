package platform.server.logics.property;

import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.Changes;
import platform.server.session.Modifier;

import java.util.Map;

public abstract class IncrementUnionProperty extends UnionProperty {

    protected IncrementUnionProperty(String sID, String caption, int intNum) {
        super(sID, caption, intNum);
    }

    protected abstract Expr calculateNewExpr(Map<Interface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere);
    protected abstract Expr calculateIncrementExpr(Map<Interface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, Expr prevExpr, WhereBuilder changedWhere);

    @Override
    protected Expr calculateExpr(Map<Interface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        if(!hasChanges(modifier) || !isStored())
            return calculateNewExpr(joinImplement, modifier, changedWhere);
        return calculateIncrementExpr(joinImplement, modifier, getExpr(joinImplement), changedWhere);
    }
}
