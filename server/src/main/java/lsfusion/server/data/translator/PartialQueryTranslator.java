package lsfusion.server.data.translator;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.caches.ParamExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;

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
