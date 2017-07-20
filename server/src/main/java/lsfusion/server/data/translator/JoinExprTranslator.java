package lsfusion.server.data.translator;

import lsfusion.base.BaseUtils;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.caches.ParamExpr;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.PullExpr;
import lsfusion.server.data.query.SourceJoin;

// заменяет expr / join на ключи
public class JoinExprTranslator extends ExprTranslator {

    private final ImMap<BaseExpr, KeyExpr> exprs; // те которые заменяем
    private final ImSet<BaseExpr> fullExprs; // те которые заведомо не трогаем

    public JoinExprTranslator(ImMap<BaseExpr, KeyExpr> exprs, ImSet<BaseExpr> fullExprs) {
        this.exprs = exprs;
        this.fullExprs = fullExprs;
    }

    public Expr translate(BaseExpr key) {
        Expr transExpr = exprs.get(key);
        if(transExpr==null) {
            if(fullExprs.contains(key))
                return key;
            return null;
        } else
            return transExpr;
    }

    @Override
    public <T extends SourceJoin<T>> T translate(T expr) {
        SourceJoin sourceJoin = expr;
        if(sourceJoin instanceof BaseExpr)
            return BaseUtils.<T>immutableCast(translate((BaseExpr)sourceJoin));
        return super.translate(expr);
    }

    @Override
    protected boolean calcTwins(TwinImmutableObject o) {
        return exprs.equals(((JoinExprTranslator)o).exprs) && fullExprs.equals(((JoinExprTranslator)o).fullExprs);
    }

    @Override
    public int immutableHashCode() {
        return 31 * exprs.hashCode() + fullExprs.hashCode();
    }
}
