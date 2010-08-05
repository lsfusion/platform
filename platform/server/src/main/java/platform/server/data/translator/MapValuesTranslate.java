package platform.server.data.translator;

import platform.server.caches.MapValues;
import platform.server.caches.hash.HashValues;
import platform.server.data.expr.ValueExpr;

import java.util.Map;
import java.util.Set;

public interface MapValuesTranslate {

    ValueExpr translate(ValueExpr expr);

    int hash(HashValues hashValues);
    Set<ValueExpr> getValues();

    // extend'ит интерфейс до MapTranslate
    MapTranslate mapKeys();

    MapValuesTranslate filter(Set<ValueExpr> values);

    boolean identity();
    boolean identityValues(Set<ValueExpr> values);

    MapValuesTranslate map(MapValuesTranslate map);

    <K,U extends MapValues<U>> Map<K,U> translateValues(Map<K,U> map);
    Set<ValueExpr> translateValues(Set<ValueExpr> values);

    boolean assertValuesEquals(Set<ValueExpr> values);

    public MapValuesTranslate reverse();
}
