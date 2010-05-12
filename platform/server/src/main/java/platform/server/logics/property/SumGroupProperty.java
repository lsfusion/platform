package platform.server.logics.property;

import platform.server.data.expr.Expr;

import java.util.*;

public class SumGroupProperty<T extends PropertyInterface> extends GroupProperty<T> {

    public SumGroupProperty(String sID, String caption, Collection<? extends PropertyInterfaceImplement<T>> interfaces, Property<T> property) {
        super(sID, caption, interfaces, property, 1);
    }

    Expr getChangedExpr(Expr changedExpr, Expr changedPrevExpr, Expr prevExpr, Expr newExpr) {
        return changedExpr.sum(changedPrevExpr.scale(-1)).sum(prevExpr);
    }
}
