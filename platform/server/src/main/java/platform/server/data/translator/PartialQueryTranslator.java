package platform.server.data.translator;

import platform.base.BaseUtils;
import platform.base.col.interfaces.immutable.ImMap;
import platform.server.caches.ParamExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;

// транслятор в тот же контекст как правило
public class PartialQueryTranslator extends QueryTranslator {

    public PartialQueryTranslator(ImMap<ParamExpr, ? extends Expr> keys) {
        super(keys, false);
    }

    public PartialQueryTranslator(ImMap<KeyExpr, ? extends Expr> keys, boolean keyExprs) {
        this(BaseUtils.<ImMap<ParamExpr, ? extends Expr>>immutableCast(keys));
        assert keyExprs;
    }
}
