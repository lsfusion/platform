package platform.server.data.translator;

import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;

import java.util.Map;

// транслятор в тот же контекст как правило
public class PartialQueryTranslator extends QueryTranslator {

    public PartialQueryTranslator(Map<KeyExpr, ? extends Expr> keys) {
        super(keys, false);
    }
}
