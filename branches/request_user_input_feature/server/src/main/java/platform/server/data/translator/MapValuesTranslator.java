package platform.server.data.translator;

import platform.base.*;
import platform.server.caches.ValuesContext;
import platform.server.caches.hash.HashCodeValues;
import platform.server.caches.hash.HashTranslateValues;
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

    public QuickSet<Value> getValues() {
        return new QuickSet<Value>(mapValues.values());
    }

    private MapValuesTranslator() {
        this.mapValues = new HashMap<Value, Value>();
    }
    public final static MapValuesTranslator noTranslate = new MapValuesTranslator();

    public MapValuesTranslator(Map<Value, Value> mapValues) {
        this.mapValues = mapValues;
        // assert что ValueClass'ы совпадают

        assert !ValueExpr.removeStatic(mapValues).containsValue(null);
    }

    public <V extends Value> V translate(V expr) {
        return BaseUtils.nvl((V) mapValues.get(expr), expr);
    }

    public boolean identityValues(QuickSet<? extends Value> values) {
        return BaseUtils.identity(BaseUtils.filterKeys(mapValues, values));
    }

    public boolean identityKeysValues(QuickSet<KeyExpr> keys, QuickSet<? extends Value> values) {
        return identityValues(values);
    }

    public MapValuesTranslate map(MapValuesTranslate map) {
        if(this==noTranslate) return map;

        Map<Value, Value> mapResult = new HashMap<Value, Value>();
        for(Map.Entry<Value,Value> mapValue : mapValues.entrySet())
            mapResult.put(mapValue.getKey(), map.translate(mapValue.getValue()));
        return new MapValuesTranslator(mapResult);
    }

    public MapValuesTranslate filter(QuickSet<? extends Value> values) {
        if(this==noTranslate) return noTranslate;

        return new MapValuesTranslator(BaseUtils.filterKeys(mapValues,values));
    }

    public <K,U extends ValuesContext<U>> Map<K,U> translateValues(Map<K,U> map) {
        Map<K,U> result = new HashMap<K,U>();
        for(Map.Entry<K,U> entry : map.entrySet())
            result.put(entry.getKey(), entry.getValue().translateValues(this));
        return result;
    }

    public <V extends Value> Set<V> translateValues(Set<V> values) {
        Set<V> result = new HashSet<V>();
        for(V value : values)
            result.add(translate(value));
        return result;
    }

    public <V extends Value> QuickSet<V> translateValues(QuickSet<V> set) {
        QuickSet<V> result = new QuickSet<V>();
        for(V value : set)
            result.add(translate(value));
        return result;
    }

    public <K, U extends Value> Map<K, U> translateMapValues(Map<K, U> map) {
        Map<K,U> result = new HashMap<K,U>();
        for(Map.Entry<K, U> entry : map.entrySet())
            result.put(entry.getKey(), translate(entry.getValue()));
        return result;
    }

    public boolean assertValuesEquals(Set<? extends Value> values) {
        return this==noTranslate || ValueExpr.noStaticEquals(mapValues.keySet(), values); 
    }

    public boolean twins(TwinImmutableInterface o) {
        return mapValues.equals(((MapValuesTranslator) o).mapValues);
    }

    public int immutableHashCode() {
        return mapValues.hashCode();
    }

    public KeyExpr translate(KeyExpr expr) {
        return expr;
    }

    public MapValuesTranslate mapValues() {
        return this;
    }

    public MapTranslate onlyKeys() {
        return MapValuesTranslator.noTranslate;
    }

    public boolean identityKeys(QuickSet<KeyExpr> keys) {
        return true;
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

    public HashValues getHashValues() {
        if(this==noTranslate || mapValues.isEmpty()) return HashCodeValues.instance;

        return new HashTranslateValues(this);
    }

    public MapTranslate mapValues(MapValuesTranslate translate) {
        return map(translate).mapKeys();
    }
}
