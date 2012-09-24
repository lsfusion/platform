package platform.server.logics.property;

import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.session.PropertyChanges;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class IncrementUnionProperty extends UnionProperty {

    protected IncrementUnionProperty(String sID, String caption, List<Interface> interfaces) {
        super(sID, caption, interfaces);
    }

    protected abstract Expr calculateNewExpr(Map<Interface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere);
    protected abstract Expr calculateIncrementExpr(Map<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges, Expr prevExpr, WhereBuilder changedWhere);

    @Override
    protected Expr calculateExpr(Map<Interface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        assert assertPropClasses(propClasses, propChanges, changedWhere);
        if(!hasChanges(propChanges) || !isStored())
            return calculateNewExpr(joinImplement, propClasses, propChanges, changedWhere);

        assert !propClasses;
        return calculateIncrementExpr(joinImplement, propChanges, getExpr(joinImplement), changedWhere);
    }
}
