package platform.server.logics.property;

import platform.server.data.expr.Expr;
import platform.server.session.Changes;
import platform.server.session.Modifier;

import java.util.Collection;
import java.util.Map;

public class SumGroupProperty<T extends PropertyInterface> extends GroupProperty<T> {

    public SumGroupProperty(String sID, String caption, Collection<? extends PropertyInterfaceImplement<T>> interfaces, Property<T> property) {
        super(sID, caption, interfaces, property, 1);
    }

    Expr getChangedExpr(Expr changedExpr, Expr changedPrevExpr, Map<Interface<T>, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier) {
        return changedExpr.sum(changedPrevExpr.scale(-1)).sum(getExpr(joinImplement));
    }
}
