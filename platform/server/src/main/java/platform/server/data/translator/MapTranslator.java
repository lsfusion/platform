package platform.server.data.translator;

import net.jcip.annotations.Immutable;
import platform.base.BaseUtils;
import platform.server.data.expr.*;

import java.util.*;

public class MapTranslator extends AbstractMapTranslator implements MapTranslate {

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

    public ValueExpr translate(ValueExpr expr) {
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

    public MapTranslate mergeEqual(MapValuesTranslate map) {
        MapValuesTranslate mergeValues = values.mergeEqualValues(map);
        if(mergeValues==null)
            return null;
        else
            return new MapTranslator(keys, mergeValues);
    }

    public MapValuesTranslate mapValues() {
        return values;
    }
}
