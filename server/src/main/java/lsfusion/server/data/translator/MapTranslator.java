package lsfusion.server.data.translator;

import lsfusion.base.SFunctionSet;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.caches.ParamExpr;
import lsfusion.server.data.Value;
import lsfusion.server.data.expr.PullExpr;

public class MapTranslator extends AbstractMapTranslator {

    // какой есть - какой нужен
    private final ImRevMap<ParamExpr,ParamExpr> keys;
    private final MapValuesTranslate values;

    public MapTranslator(ImRevMap<ParamExpr, ParamExpr> keys, MapValuesTranslate values) {
        this.keys = keys;
        this.values = values;
    }

    public ParamExpr translate(ParamExpr key) {
        ParamExpr transExpr = keys.get(key);
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
        return new MapTranslator(keys, values.onlyKeys());
    }

    public MapTranslate reverseMap() {
        return new MapTranslator(keys.reverse(), values.reverse());
    }

    private final static SFunctionSet<ParamExpr> removePullExpr = new SFunctionSet<ParamExpr>() {
        public boolean contains(ParamExpr element) {
            return !(element instanceof PullExpr);
        }};
    public boolean identityKeys(ImSet<ParamExpr> keys) {
        assert this.keys.keys().filterFn(removePullExpr).containsAll(keys.filterFn(removePullExpr));
        return this.keys.filterRev(keys).identity();
    }
    public boolean identityValues(ImSet<? extends Value> values) {
        return this.values.identityValues(values);
    }
    public boolean identityKeysValues(ImSet<ParamExpr> keys, ImSet<? extends Value> values) {
        return identityKeys(keys) && identityValues(values);
    }

    public MapTranslate mapValues(MapValuesTranslate translate) {
        MapValuesTranslate mapValues = values.mapTrans(translate);
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
