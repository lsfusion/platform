package platform.server.data.translator;

import platform.base.*;
import platform.server.data.Value;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.PullExpr;

import java.util.Map;

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

    public boolean twins(TwinImmutableInterface o) {
        return keys.equals(((MapTranslator)o).keys) && values.equals(((MapTranslator)o).values);
    }

    public int immutableHashCode() {
        return keys.hashCode()*31+values.hashCode();
    }

    public <K> Map<K, KeyExpr> translateKey(Map<K, KeyExpr> map) {
        return BaseUtils.join(map, keys);
    }

    public MapValuesTranslate mapValues() {
        return values;
    }

    public MapTranslate onlyKeys() {
        return new MapTranslator(keys, MapValuesTranslator.noTranslate);
    }

    public MapTranslate reverseMap() {
        return new MapTranslator(BaseUtils.reverse(keys), values.reverse());
    }

    public boolean identityKeys(QuickSet<KeyExpr> keys) {
        return BaseUtils.identity(BaseUtils.filterKeys(this.keys, keys));
    }
    public boolean identityValues(QuickSet<? extends Value> values) {
        return this.values.identityValues(values);
    }
    public boolean identityKeysValues(QuickSet<KeyExpr> keys, QuickSet<? extends Value> values) {
        return identityKeys(keys) && identityValues(values);
    }

    public MapTranslate mapValues(MapValuesTranslate translate) {
        return new MapTranslator(keys, values.map(translate));
    }
}
