package platform.server.data.translator;

import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;

import java.util.Map;

public class PartialQueryTranslator extends QueryTranslator {

    public PartialQueryTranslator(Map<KeyExpr, ? extends Expr> keys) {
        super(keys, false);
    }
}
