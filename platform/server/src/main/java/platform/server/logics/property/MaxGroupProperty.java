package platform.server.logics.property;

import platform.interop.Compare;
import platform.server.data.expr.Expr;

import java.util.Collection;

public class MaxGroupProperty<T extends PropertyInterface> extends GroupProperty<T> {

    public MaxGroupProperty(String sID, String caption, Collection<GroupPropertyInterface<T>> interfaces, Property<T> property) {
        super(sID, caption, interfaces, property, 0);
    }

    Expr getChangedExpr(Expr changedExpr, Expr changedPrevExpr, Expr prevExpr, Expr newExpr) {
//        return newExpr;
        return changedExpr.ifElse(changedExpr.compare(prevExpr,Compare.GREATER).or(prevExpr.getWhere().not()),
                newExpr.ifElse(changedPrevExpr.compare(prevExpr,Compare.EQUALS), prevExpr));
    }
}
