package platform.server.data.translator;

import platform.base.TwinImmutableObject;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.data.Value;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.PullExpr;

public class MapTranslator extends AbstractMapTranslator {

    // какой есть - какой нужен
    private final ImRevMap<KeyExpr,KeyExpr> keys;
    private final MapValuesTranslate values;

    public MapTranslator(ImRevMap<KeyExpr, KeyExpr> keys, MapValuesTranslate values) {
        this.keys = keys;
        this.values = values;
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

    public boolean twins(TwinImmutableObject o) {
        return keys.equals(((MapTranslator)o).keys) && values.equals(((MapTranslator)o).values);
    }

    public int immutableHashCode() {
        return keys.hashCode()*31+values.hashCode();
    }

    public MapValuesTranslate mapValues() {
        return values;
    }

    public MapTranslate onlyKeys() {
        return new MapTranslator(keys, MapValuesTranslator.noTranslate);
    }

    public MapTranslate reverseMap() {
        return new MapTranslator(keys.reverse(), values.reverse());
    }

    public boolean identityKeys(ImSet<KeyExpr> keys) {
        return this.keys.filterInclRev(keys).identity();
    }
    public boolean identityValues(ImSet<? extends Value> values) {
        return this.values.identityValues(values);
    }
    public boolean identityKeysValues(ImSet<KeyExpr> keys, ImSet<? extends Value> values) {
        return identityKeys(keys) && identityValues(values);
    }

    public MapTranslate mapValues(MapValuesTranslate translate) {
        MapValuesTranslate mapValues = values.map(translate);
        if(mapValues==values)
            return this;

        return new MapTranslator(keys, mapValues);
    }

    public MapTranslate filterValues(ImSet<? extends Value> values) {
        MapValuesTranslate filterValues = this.values.filter(values);
        if(filterValues==this.values)
            return this;

        return new MapTranslator(keys, filterValues);
    }
}
