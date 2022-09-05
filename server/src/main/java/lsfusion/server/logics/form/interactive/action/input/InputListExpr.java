package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class InputListExpr<P extends PropertyInterface> {

    public final ImRevMap<P, KeyExpr> mapKeys;
    public final Expr expr;
    public final ImOrderMap<Expr, Boolean> orders;

    public InputListExpr(ImRevMap<P, KeyExpr> mapKeys, Expr expr, ImOrderMap<Expr, Boolean> orders) {
        this.mapKeys = mapKeys;
        this.expr = expr;
        this.orders = orders;
    }
}
