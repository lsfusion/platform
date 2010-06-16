package platform.server.data.translator;

import platform.server.caches.MapValues;
import platform.server.data.expr.ValueExpr;

import java.util.Map;
import java.util.Set;

import net.jcip.annotations.Immutable;

public interface MapValuesTranslate {

    ValueExpr translate(ValueExpr expr);

    // extend'ит интерфейс до MapTranslate
    MapTranslate mapKeys();

    MapValuesTranslate filter(Set<ValueExpr> values);

    boolean identity();
    boolean identityValues(Set<ValueExpr> values);

    MapValuesTranslate map(MapValuesTranslate map);

    MapValuesTranslate crossMap(MapValuesTranslate map);

    MapValuesTranslate mergeEqualValues(MapValuesTranslate map);

    // множественный полиморфизм - protected по идее
    MapValuesTranslator mergeEqualValuesTranslator(MapValuesTranslator map);

    <K,U extends MapValues<U>> Map<K,U> translateValues(Map<K,U> map);
    Set<ValueExpr> translateValues(Set<ValueExpr> values);

    boolean assertValuesEquals(Set<ValueExpr> values);
}
