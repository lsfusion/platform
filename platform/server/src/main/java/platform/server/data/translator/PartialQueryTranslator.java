package platform.server.data.translator;

import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.Expr;

import java.util.Map;

public class PartialQueryTranslator extends QueryTranslator {

    public PartialQueryTranslator(Map<KeyExpr, ? extends Expr> keys) {
        super(keys, false);
    }
}
