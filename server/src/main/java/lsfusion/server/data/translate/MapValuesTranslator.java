package lsfusion.server.data.translate;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.caches.ValuesContext;
import lsfusion.server.data.expr.key.ParamExpr;
import lsfusion.server.data.value.Value;

import java.util.Map;
import java.util.function.Function;

// отличается тем что не только маппит ValueExpr к ValueExpr, а с assertion'ом, что только одинаковых классов
public abstract class MapValuesTranslator extends AbstractMapTranslator implements MapValuesTranslate {

    private Function<TranslateValues, TranslateValues> trans;
    private <V extends TranslateValues> Function<V, V> TRANS() {
        if(trans==null) {
            trans = value -> value.translateValues(MapValuesTranslator.this);
        }
        return (Function<V, V>)trans;
    }

//    private Function<ImMap<Object, ValuesContext>, ImMap<Object, ValuesContext>> transMap;
    public <K, V extends ValuesContext> Function<ImMap<K, V>, ImMap<K, V>> TRANSMAP() {
//        if(transMap==null) {
            Function<ImMap<Object, ValuesContext>, ImMap<Object, ValuesContext>> transMap = this::translateValues;
//      }
        return BaseUtils.immutableCast(transMap);
    }

    private Function<Value, Value> transValue;
    private <V extends Value> Function<V, V> TRANSVALUE() {
//        if(transValue==null) {
            Function<Value, Value> transValue = this::translate;
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
        return map.mapValues(this.TRANS());
    }

    public <K1, U1 extends ValuesContext<U1>, K2, U2 extends ValuesContext<U2>> ImMap<ImMap<K1, U1>, ImMap<K2, U2>> translateMapKeyValues(ImMap<ImMap<K1, U1>, ImMap<K2, U2>> map) {
        return map.mapKeyValues(this.TRANSMAP(), this.TRANSMAP());
    }

    public <K1, U1 extends ValuesContext<U1>, U2 extends ValuesContext<U2>> ImMap<ImMap<K1, U1>, U2> translateMapKeyValue(ImMap<ImMap<K1, U1>, U2> map) {
        return map.mapKeyValues(this.TRANSMAP(), this.TRANS());
    }

    public <V extends Value> ImSet<V> translateValues(ImSet<V> values) {
        return values.mapSetValues(this.TRANSVALUE());
    }

    public <K, U extends Value> ImMap<K, U> translateMapValues(ImMap<K, U> map) {
        return map.mapValues(this.TRANSVALUE());
    }

    public <K, U extends Value> ImRevMap<K, U> translateMapValues(ImRevMap<K, U> map) {
        return map.mapRevValues(this.TRANSVALUE());
    }

    @Override
    public <K extends Value, U> ImMap<K, U> translateValuesMapKeys(ImMap<K, U> map) {
        return map.mapKeys(this.TRANSVALUE());
    }

    @Override
    public <K extends Value, U> ImRevMap<K, U> translateValuesMapKeys(ImRevMap<K, U> map) {
        return map.mapRevKeys(this.TRANSVALUE());
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
        return mapTrans(translate).mapKeys();
    }

    public MapTranslate filterValues(ImSet<? extends Value> values) {
        return filter(values).mapKeys();
    }
}
