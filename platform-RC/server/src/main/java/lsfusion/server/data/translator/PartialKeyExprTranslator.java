package lsfusion.server.data.translator;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.caches.ParamExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.SourceJoin;

// транслятор в тот же контекст как правило
public class PartialKeyExprTranslator extends KeyExprTranslator {

    public PartialKeyExprTranslator(ImMap<ParamExpr, ? extends Expr> keys) {
        super(keys, false);
    }

    public PartialKeyExprTranslator(ImMap<KeyExpr, ? extends Expr> keys, boolean keyExprs) {
        this(BaseUtils.<ImMap<ParamExpr, ? extends Expr>>immutableCast(keys));
        assert keyExprs;
    }

    @Override
    public <T extends SourceJoin<T>> T translate(T expr) {
        ImSet<ParamExpr> keys = expr.getOuterKeys();
        if (keys.disjoint(this.keys.keys()))
            return expr;
        return super.translate(expr);
    }
}
