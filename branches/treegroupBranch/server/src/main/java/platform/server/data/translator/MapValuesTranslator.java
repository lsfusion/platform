package platform.server.data.translator;

import platform.base.BaseUtils;
import platform.server.caches.MapValues;
import platform.server.caches.hash.HashValues;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// отличается тем что не только маппит ValueExpr к ValueExpr, а с assertion'ом, что только одинаковых классов
public class MapValuesTranslator extends AbstractMapTranslator implements MapValuesTranslate {

    private final Map<ValueExpr, ValueExpr> mapValues;

    public int hash(HashValues hashValues) {
        int hash = 0;
        for(Map.Entry<ValueExpr,ValueExpr> entry : mapValues.entrySet())
            hash += entry.getKey().hashCode() ^ hashValues.hash(entry.getValue());
        return hash;
    }

    public Set<ValueExpr> getValues() {
        return new HashSet<ValueExpr>(mapValues.values());
    }

    private MapValuesTranslator() {
        this.mapValues = new HashMap<ValueExpr, ValueExpr>();
    }
    public final static MapValuesTranslate noTranslate = new MapValuesTranslator();

    public MapValuesTranslator(Map<ValueExpr, ValueExpr> mapValues) {
        this.mapValues = mapValues;

        assert !ValueExpr.removeStatic(mapValues).containsValue(null);
    }

    public ValueExpr translate(ValueExpr expr) {
        return BaseUtils.nvl(mapValues.get(expr),expr);
    }

    public boolean identity() {
        return BaseUtils.identity(mapValues);
    }
    public boolean identityValues(Set<ValueExpr> values) {
        return BaseUtils.identity(BaseUtils.filterKeys(mapValues, values));
    }

    public MapValuesTranslate map(MapValuesTranslate map) {
        Map<ValueExpr, ValueExpr> mapResult = new HashMap<ValueExpr, ValueExpr>();
        for(Map.Entry<ValueExpr,ValueExpr> mapValue : mapValues.entrySet())
            mapResult.put(mapValue.getKey(), map.translate(mapValue.getValue()));
        return new MapValuesTranslator(mapResult);
    }

    public MapValuesTranslate filter(Set<ValueExpr> values) {
        return new MapValuesTranslator(BaseUtils.filterKeys(mapValues,values));
    }

    public <K,U extends MapValues<U>> Map<K,U> translateValues(Map<K,U> map) {
        Map<K,U> result = new HashMap<K,U>();
        for(Map.Entry<K,U> entry : map.entrySet())
            result.put(entry.getKey(),entry.getValue().translate(this));
        return result;
    }

    public Set<ValueExpr> translateValues(Set<ValueExpr> values) {
        Set<ValueExpr> result = new HashSet<ValueExpr>();
        for(ValueExpr value : values)
            result.add(translate(value));
        return result;
    }

    public boolean assertValuesEquals(Set<ValueExpr> values) {
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
        return new MapValuesTranslator(BaseUtils.reverse(mapValues));
    }

    public MapTranslate reverseMap() {
        return new MapValuesTranslator(BaseUtils.reverse(mapValues));
    }
}
