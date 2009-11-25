package platform.server.data.translator;

import platform.base.BaseUtils;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;

import java.util.Map;

public abstract class Translator<T extends Expr> {

    public final Map<KeyExpr,T> keys;
    public final Map<ValueExpr,ValueExpr> values; // какой есть - какой нужен

    public Translator(Map<KeyExpr,T> iKeys,Map<ValueExpr,ValueExpr> iValues) {
        keys = iKeys;
        values = iValues;
    }

    public T translate(KeyExpr key) {
        return BaseUtils.nvl(keys.get(key),(T)key);
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
