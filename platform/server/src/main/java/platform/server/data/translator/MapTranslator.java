package platform.server.data.translator;

import platform.base.BaseUtils;
import platform.server.data.Value;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.PullExpr;

import java.util.Map;
import java.util.Set;

public class MapTranslator extends AbstractMapTranslator {

    // какой есть - какой нужен
    private final Map<KeyExpr,KeyExpr> keys;
    private final MapValuesTranslate values;

    public MapTranslator(Map<KeyExpr, KeyExpr> keys, MapValuesTranslate values) {
        this.keys = keys;
        this.values = values;

        assert !keys.containsValue(null);
    }

    public KeyExpr translate(KeyExpr key) {
        KeyExpr transExpr = keys.get(key);
        if(transExpr==null) {
            assert key instanceof PullExpr; // не должно быть
            return key;
        } else
            return transExpr;
    }

    public <V extends Value> V translate(V expr) {
        return values.translate(expr);
    }

    public int hashCode() {
        return keys.hashCode()*31+values.hashCode();
    }

    public boolean equals(Object obj) {
        return obj==this || (obj instanceof MapTranslator && keys.equals(((MapTranslator)obj).keys) && values.equals(((MapTranslator)obj).values));
    }

    public <K> Map<K, KeyExpr> translateKey(Map<K, KeyExpr> map) {
        return BaseUtils.join(map, keys);
    }

    public MapValuesTranslate mapValues() {
        return values;
    }

    public MapTranslate reverseMap() {
        return new MapTranslator(BaseUtils.reverse(keys), values.reverse());
    }

    public boolean identityValues(Set<? extends Value> values) {
        return BaseUtils.identity(keys) && this.values.identityValues(values);
    }
}
