package platform.server.data.translator;

import platform.base.BaseUtils;
import platform.base.TwinImmutableObject;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.caches.TranslateValues;
import platform.server.caches.ValuesContext;
import platform.server.caches.hash.HashCodeValues;
import platform.server.caches.hash.HashTranslateValues;
import platform.server.caches.hash.HashValues;
import platform.server.data.Value;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;

import java.util.Map;

// отличается тем что не только маппит ValueExpr к ValueExpr, а с assertion'ом, что только одинаковых классов
public class MapValuesTranslator extends AbstractMapTranslator implements MapValuesTranslate {

    private GetValue<TranslateValues, TranslateValues> trans;
    private <V extends TranslateValues> GetValue<V, V> TRANS() {
        if(trans==null) {
            trans = new GetValue<TranslateValues, TranslateValues>() {
                public TranslateValues getMapValue(TranslateValues value) {
                    return value.translateValues(MapValuesTranslator.this);
                }};
        }
        return (GetValue<V, V>)trans;
    }

//    private GetValue<ImMap<Object, ValuesContext>, ImMap<Object, ValuesContext>> transMap;
    public <K, V extends ValuesContext> GetValue<ImMap<K, V>, ImMap<K, V>> TRANSMAP() {
//        if(transMap==null) {
            GetValue<ImMap<Object, ValuesContext>, ImMap<Object, ValuesContext>> transMap = new GetValue<ImMap<Object, ValuesContext>, ImMap<Object, ValuesContext>>() {
                public ImMap<Object, ValuesContext> getMapValue(ImMap<Object, ValuesContext> value) {
                    return translateValues(value);
                }};
//      }
        return BaseUtils.immutableCast(transMap);
    }

    private GetValue<Value, Value> transValue;
    private <V extends Value> GetValue<V, V> TRANSVALUE() {
//        if(transValue==null) {
            GetValue<Value, Value> transValue = new GetValue<Value, Value>() {
                public Value getMapValue(Value value) {
                    return translate(value);
                }};
//        }
        return BaseUtils.immutableCast(transValue);
    }

    private final ImRevMap<Value, Value> mapValues;

    public int hash(HashValues hashValues) {
        int hash = 0;
        for(int i=0,size=mapValues.size();i<size;i++)
            hash += mapValues.getKey(i).hashCode() ^ hashValues.hash(mapValues.getValue(i));
        return hash;
    }

    public ImSet<Value> getValues() {
        return mapValues.valuesSet();
    }

    private MapValuesTranslator() {
        this.mapValues = MapFact.EMPTYREV();
    }
    public final static MapValuesTranslator noTranslate = new MapValuesTranslator();

    public MapValuesTranslator(ImRevMap<Value, Value> mapValues) {
        this.mapValues = mapValues;
        // assert что ValueClass'ы совпадают

//        assert !ValueExpr.removeStatic(mapValues).containsValue(null);
    }

    public <V extends Value> V translate(V expr) {
        return BaseUtils.nvl((V) mapValues.get(expr), expr);
    }

    public boolean identityValues(ImSet<? extends Value> values) {
        return mapValues.filterInclRev(values).identity();
    }

    public boolean identityKeysValues(ImSet<KeyExpr> keys, ImSet<? extends Value> values) {
        return identityValues(values);
    }

    public MapValuesTranslate map(MapValuesTranslate map) {
        if(this==noTranslate) return map;
        if(map==noTranslate) return this;

        return new MapValuesTranslator(map.translateMapValues(mapValues));
    }

    public MapValuesTranslate filter(ImSet<? extends Value> values) {
        if(this==noTranslate) return noTranslate;
        if(BaseUtils.hashEquals(mapValues.keys(), values)) return this;

        return new MapValuesTranslator(mapValues.filterInclRev(values));
    }

    public <K,U extends ValuesContext<U>> ImMap<K, U> translateValues(ImMap<K, U> map) {
        return map.mapValues(this.<U>TRANS());
    }

    public <K1, U1 extends ValuesContext<U1>, K2, U2 extends ValuesContext<U2>> ImMap<ImMap<K1, U1>, ImMap<K2, U2>> translateMapKeyValues(ImMap<ImMap<K1, U1>, ImMap<K2, U2>> map) {
        return map.mapKeyValues(this.<K1, U1>TRANSMAP(), this.<K2, U2>TRANSMAP());
    }

    public <V extends Value> ImSet<V> translateValues(ImSet<V> values) {
        return values.mapSetValues(this.<V>TRANSVALUE());
    }

    public <K, U extends Value> ImMap<K, U> translateMapValues(ImMap<K, U> map) {
        return map.mapValues(this.<U>TRANSVALUE());
    }

    public <K, U extends Value> ImRevMap<K, U> translateMapValues(ImRevMap<K, U> map) {
        return map.mapRevValues(this.<U>TRANSVALUE());
    }

    @Override
    public <K extends Value, U> ImRevMap<K, U> translateValuesMapKeys(ImRevMap<K, U> map) {
        return map.mapRevKeys(this.<K>TRANSVALUE());
    }

    public boolean assertValuesEquals(ImSet<? extends Value> values) {
        return this==noTranslate || ValueExpr.noStaticEquals(mapValues.keys(), values);
    }

    public boolean twins(TwinImmutableObject o) {
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

    public boolean identityKeys(ImSet<KeyExpr> keys) {
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
        
        return new MapValuesTranslator(mapValues.reverse());
    }

    public MapTranslate reverseMap() {
        if(this==noTranslate) return (MapTranslate) noTranslate;

        return new MapValuesTranslator(mapValues.reverse());
    }

    public HashValues getHashValues() {
        if(this==noTranslate || mapValues.isEmpty()) return HashCodeValues.instance;

        return new HashTranslateValues(this);
    }

    public MapTranslate mapValues(MapValuesTranslate translate) {
        return map(translate).mapKeys();
    }

    public MapTranslate filterValues(ImSet<? extends Value> values) {
        return filter(values).mapKeys();
    }
}
