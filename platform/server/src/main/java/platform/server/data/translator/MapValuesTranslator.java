package platform.server.data.translator;

import platform.base.BaseUtils;
import platform.server.caches.MapValues;
import platform.server.caches.hash.HashValues;
import platform.server.data.Value;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// отличается тем что не только маппит ValueExpr к ValueExpr, а с assertion'ом, что только одинаковых классов
public class MapValuesTranslator extends AbstractMapTranslator implements MapValuesTranslate {

    private final Map<Value, Value> mapValues;

    public int hash(HashValues hashValues) {
        int hash = 0;
        for(Map.Entry<Value,Value> entry : mapValues.entrySet())
            hash += entry.getKey().hashCode() ^ hashValues.hash(entry.getValue());
        return hash;
    }

    public Set<Value> getValues() {
        return new HashSet<Value>(mapValues.values());
    }

    private MapValuesTranslator() {
        this.mapValues = new HashMap<Value, Value>();
    }
    public final static MapValuesTranslate noTranslate = new MapValuesTranslator();

    public MapValuesTranslator(Map<Value, Value> mapValues) {
        this.mapValues = mapValues;
        // assert что ValueClass'ы совпадают

        assert !ValueExpr.removeStatic(mapValues).containsValue(null);
    }

    public <V extends Value> V translate(V expr) {
        return BaseUtils.nvl((V)mapValues.get(expr),expr);
    }

    public boolean identity() {
        return BaseUtils.identity(mapValues);
    }
    public boolean identityValues(Set<? extends Value> values) {
        return BaseUtils.identity(BaseUtils.filterKeys(mapValues, values));
    }

    public MapValuesTranslate map(MapValuesTranslate map) {
        if(this==noTranslate) return noTranslate;

        Map<Value, Value> mapResult = new HashMap<Value, Value>();
        for(Map.Entry<Value,Value> mapValue : mapValues.entrySet())
            mapResult.put(mapValue.getKey(), map.translate(mapValue.getValue()));
        return new MapValuesTranslator(mapResult);
    }

    public MapValuesTranslate filter(Set<? extends Value> values) {
        if(this==noTranslate) return noTranslate;

        return new MapValuesTranslator(BaseUtils.filterKeys(mapValues,values));
    }

    public <K,U extends MapValues<U>> Map<K,U> translateValues(Map<K,U> map) {
        Map<K,U> result = new HashMap<K,U>();
        for(Map.Entry<K,U> entry : map.entrySet())
            result.put(entry.getKey(),entry.getValue().translate(this));
        return result;
    }

    public <V extends Value> Set<V> translateValues(Set<V> values) {
        Set<V> result = new HashSet<V>();
        for(V value : values)
            result.add(translate(value));
        return result;
    }

    public boolean assertValuesEquals(Set<? extends Value> values) {
        return this==noTranslate || ValueExpr.noStaticEquals(mapValues.keySet(), values); 
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof MapValuesTranslator && mapValues.equals(((MapValuesTranslator) o).mapValues);
    }

    @Override
    public int hashCode() {
        return mapValues.hashCode();
    }

    public KeyExpr translate(KeyExpr expr) {
        return expr;
    }

    public MapValuesTranslate mapValues() {
        return this;
    }

    public <K> Map<K, KeyExpr> translateKey(Map<K, KeyExpr> map) {
        return map;
    }

    public MapTranslate mapKeys() {
        return this;
    }

    public MapValuesTranslate reverse() {
        if(this==noTranslate) return noTranslate;
        
        return new MapValuesTranslator(BaseUtils.reverse(mapValues));
    }

    public MapTranslate reverseMap() {
        if(this==noTranslate) return (MapTranslate) noTranslate;

        return new MapValuesTranslator(BaseUtils.reverse(mapValues));
    }
}
