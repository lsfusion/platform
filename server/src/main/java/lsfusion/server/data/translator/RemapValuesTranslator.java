package lsfusion.server.data.translator;

import lsfusion.base.BaseUtils;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.caches.hash.HashTranslateValues;
import lsfusion.server.caches.hash.HashValues;
import lsfusion.server.data.Value;
import lsfusion.server.data.expr.ValueExpr;

public class RemapValuesTranslator extends MapValuesTranslator {

    private final ImRevMap<Value, Value> mapValues;

    public RemapValuesTranslator(ImRevMap<Value, Value> mapValues) {
        this.mapValues = mapValues;
        // assert что ValueClass'ы совпадают
    }

    public int hash(HashValues hashValues) {
        int hash = 0;
        for(int i=0,size=mapValues.size();i<size;i++)
            hash += mapValues.getKey(i).hashCode() ^ hashValues.hash(mapValues.getValue(i));
        return hash;
    }

    public ImSet<Value> getValues() {
        return mapValues.valuesSet();
    }

    public MapValuesTranslator onlyKeys() {
        return MapValuesTranslator.noTranslate(mapValues.keys());
    }

    public <V extends Value> V translate(V expr) {
        return BaseUtils.nvl((V) mapValues.get(expr), expr);
    }

    public boolean identityValues(ImSet<? extends Value> values) {
        return mapValues.filterInclRev(values).identity();
    }

    public MapValuesTranslate map(MapValuesTranslate map) {
        return new RemapValuesTranslator(map.translateMapValues(mapValues));
    }

    public MapValuesTranslate filter(ImSet<? extends Value> values) {
        if(BaseUtils.hashEquals(mapValues.keys(), values)) return this;

        return new RemapValuesTranslator(mapValues.filterInclRev(values));
    }

    public boolean assertValuesContains(ImSet<? extends Value> values) {
        return ValueExpr.noStaticContains(mapValues.keys(), values);
    }

    public boolean twins(TwinImmutableObject o) {
        return mapValues.equals(((RemapValuesTranslator) o).mapValues);
    }

    public int immutableHashCode() {
        return mapValues.hashCode();
    }

    public MapValuesTranslate reverse() {
        return new RemapValuesTranslator(mapValues.reverse());
    }

    public MapTranslate reverseMap() {
        return new RemapValuesTranslator(mapValues.reverse());
    }

    public HashValues getHashValues() {
        return new HashTranslateValues(this);
    }

}
