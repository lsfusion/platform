package platform.server.data.translator;

import platform.server.caches.MapValues;
import platform.server.caches.hash.HashValues;
import platform.server.data.Value;

import java.util.Map;
import java.util.Set;

public interface MapValuesTranslate {

    <V extends Value> V translate(V expr);

    int hash(HashValues hashValues);
    Set<Value> getValues();

    // extend'ит интерфейс до MapTranslate
    MapTranslate mapKeys();

    MapValuesTranslate filter(Set<? extends Value> values);

    boolean identity();
    boolean identityValues(Set<? extends Value> values);

    MapValuesTranslate map(MapValuesTranslate map);

    <K,U extends MapValues<U>> Map<K,U> translateValues(Map<K,U> map);
    <V extends Value> Set<V> translateValues(Set<V> values);

    boolean assertValuesEquals(Set<? extends Value> values);

    public MapValuesTranslate reverse();
}
