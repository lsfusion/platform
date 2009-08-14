package platform.server.data.query.translators;

import platform.base.BaseUtils;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.ValueExpr;

import java.util.HashMap;
import java.util.Map;

import net.jcip.annotations.Immutable;

@Immutable
public abstract class Translator<T extends SourceExpr> {

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

    public <K> Map<K, SourceExpr> translate(Map<K, ? extends SourceExpr> map) {
        Map<K,SourceExpr> transMap = new HashMap<K,SourceExpr>();
        for(Map.Entry<K,? extends SourceExpr> entry : map.entrySet())
            transMap.put(entry.getKey(),entry.getValue().translate(this));
        return transMap;
    }
}
