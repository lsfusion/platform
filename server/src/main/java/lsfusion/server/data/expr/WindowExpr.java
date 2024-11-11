package lsfusion.server.data.expr;

import lsfusion.server.data.expr.key.KeyExpr;

public class WindowExpr extends KeyExpr {

    public final static WindowExpr instance = new WindowExpr(0);

    public WindowExpr(int id) {
        super(id);
    }
}
