package lsfusion.server.data.expr;

import lsfusion.server.data.expr.key.KeyExpr;

// необходим для Change, позволяет протаскивать ключи вверх пока не попадет в group by
public class PullExpr extends KeyExpr {

    public PullExpr(int id) {
        super(id);
    }
}
