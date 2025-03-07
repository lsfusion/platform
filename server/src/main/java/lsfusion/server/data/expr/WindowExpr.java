package lsfusion.server.data.expr;

import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.server.data.expr.key.KeyExpr;

public class WindowExpr extends KeyExpr {

    public final static WindowExpr limit = new WindowExpr(0);
    public final static WindowExpr offset = new WindowExpr(1);

    public WindowExpr(int id) {
        super(id);
    }

    public static boolean is(Expr expr) {
        return expr.equals(WindowExpr.limit) || expr.equals(WindowExpr.offset);
    }

    public static boolean has(ImCol<? extends Expr> group) {
        return ((ImCol<Expr>)group).contains(WindowExpr.limit) || ((ImCol<Expr>)group).contains(WindowExpr.offset); // we are assuming that offset only when there is limit
    }

    public static <T> boolean has(T limit, T offset) {
        return limit != null || offset != null; // we are assuming that offset only when there is limit
    }
}
