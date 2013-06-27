package lsfusion.server.data.translator;

import lsfusion.base.BaseUtils;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.caches.ParamExpr;
import lsfusion.server.caches.TranslateValues;
import lsfusion.server.caches.ValuesContext;
import lsfusion.server.caches.hash.HashCodeValues;
import lsfusion.server.caches.hash.HashTranslateValues;
import lsfusion.server.caches.hash.HashValues;
import lsfusion.server.data.Value;
import lsfusion.server.data.expr.ValueExpr;

import java.util.Map;

// отличается тем что не только маппит ValueExpr к ValueExpr, а с assertion'ом, что только одинаковых классов
public abstract class MapValuesTranslator extends AbstractMapTranslator implements MapValuesTranslate {

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

    public static MapValuesTranslator noTranslate(ImSet<Value> values) {
        return new IdentityValuesTranslator(values);
    }

    public boolean identityKeysValues(ImSet<ParamExpr> keys, ImSet<? extends Value> values) {
        return identityValues(values);
    }

    public <K,U extends ValuesContext<U>> ImMap<K, U> translateValues(ImMap<K, U> map) {
        return map.mapValues(this.<U>TRANS());
    }

    public <K1, U1 extends ValuesContext<U1>, K2, U2 extends ValuesContext<U2>> ImMap<ImMap<K1, U1>, ImMap<K2, U2>> translateMapKeyValues(ImMap<ImMap<K1, U1>, ImMap<K2, U2>> map) {
        return map.mapKeyValues(this.<K1, U1>TRANSMAP(), this.<K2, U2>TRANSMAP());
    }

    public <K1, U1 extends ValuesContext<U1>, U2 extends ValuesContext<U2>> ImMap<ImMap<K1, U1>, U2> translateMapKeyValue(ImMap<ImMap<K1, U1>, U2> map) {
        return map.mapKeyValues(this.<K1, U1>TRANSMAP(), this.<U2>TRANS());
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
    public <K extends Value, U> ImMap<K, U> translateValuesMapKeys(ImMap<K, U> map) {
        return map.mapKeys(this.<K>TRANSVALUE());
    }

    @Override
    public <K extends Value, U> ImRevMap<K, U> translateValuesMapKeys(ImRevMap<K, U> map) {
        return map.mapRevKeys(this.<K>TRANSVALUE());
    }

    public ParamExpr translate(ParamExpr expr) {
        return expr;
    }

    public MapValuesTranslate mapValues() {
        return this;
    }

    public boolean identityKeys(ImSet<ParamExpr> keys) {
        return true;
    }

    public <K> Map<K, ParamExpr> translateKey(Map<K, ParamExpr> map) {
        return map;
    }

    public MapTranslate mapKeys() {
        return this;
    }

    public MapTranslate mapValues(MapValuesTranslate translate) {
        return map(translate).mapKeys();
    }

    public MapTranslate filterValues(ImSet<? extends Value> values) {
        return filter(values).mapKeys();
    }
}
