package platform.server.data.translator;

import platform.base.BaseUtils;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.PullExpr;

import java.util.Map;

public abstract class Translator<T extends Expr> {

    public final Map<KeyExpr,T> keys;
    public final Map<ValueExpr,ValueExpr> values; // какой есть - какой нужен

    private final boolean allKeys;

    public Translator(Map<KeyExpr,T> keys,Map<ValueExpr,ValueExpr> values, boolean allKeys) {
        this.keys = keys;
        this.values = values;

        this.allKeys = allKeys;
    }

    public T translate(KeyExpr key) {
        T transExpr = keys.get(key);
        if(transExpr==null) {
            if(allKeys)
                assert key instanceof PullExpr; // не должно быть
            return (T)key;
        } else
            return transExpr;
    }

    public ValueExpr translate(ValueExpr expr) {
        return BaseUtils.nvl(values.get(expr),expr);
    }

    public int hashCode() {
        return keys.hashCode()*31+values.hashCode();
    }

    public boolean equals(Object obj) {
        return obj==this || (obj instanceof Translator && keys.equals(((Translator)obj).keys) && values.equals(((Translator)obj).values));
    }
}
