package lsfusion.server.data.translator;

import lsfusion.base.BaseUtils;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.caches.ParamExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.PullExpr;
import lsfusion.server.data.query.SourceJoin;

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
