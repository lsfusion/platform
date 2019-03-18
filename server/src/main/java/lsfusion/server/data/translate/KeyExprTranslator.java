package lsfusion.server.data.translate;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.SourceJoin;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.PullExpr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.key.ParamExpr;

// заменяет ключы на выражения
public class KeyExprTranslator extends ExprTranslator {

    protected final ImMap<ParamExpr,? extends Expr> keys;

    private final boolean allKeys;

    public KeyExprTranslator translateRemoveValues(MapValuesTranslate translate) {
        return new KeyExprTranslator(translate.mapKeys().translate(keys), allKeys);
    }

    protected KeyExprTranslator(ImMap<ParamExpr, ? extends Expr> keys, boolean allKeys) {
        this.keys = keys;

        this.allKeys = allKeys;
    }

    public KeyExprTranslator(ImMap<KeyExpr, ? extends Expr> joinImplement) {
        this(BaseUtils.<ImMap<ParamExpr, ? extends KeyExpr>>immutableCast(joinImplement), true);
    }

    public Expr translate(ParamExpr key) {
        Expr transExpr = keys.get(key);
        if(transExpr==null) {
            if(allKeys)
                assert key instanceof PullExpr; // не должно быть
            return key;
        } else
            return transExpr;
    }

    @Override
    public <T extends SourceJoin<T>> T translate(T expr) {
        SourceJoin sourceJoin = expr;
        if(sourceJoin instanceof ParamExpr)
            return BaseUtils.immutableCast(translate((ParamExpr) sourceJoin));
        return super.translate(expr);
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return keys.equals(((KeyExprTranslator)o).keys);
    }

    public int immutableHashCode() {
        return keys.hashCode();
    }
}
