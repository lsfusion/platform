package platform.server.data.translator;

import platform.base.QuickMap;
import platform.base.QuickSet;
import platform.server.caches.ValuesContext;
import platform.server.caches.hash.HashValues;
import platform.server.data.Value;

import java.util.Map;
import java.util.Set;

public interface MapValuesTranslate extends MapObject {

    <V extends Value> V translate(V expr);

    int hash(HashValues hashValues);
    QuickSet<Value> getValues();

    // extend'ит интерфейс до MapTranslate
    MapTranslate mapKeys();

    MapValuesTranslate filter(QuickSet<? extends Value> values);

    MapValuesTranslate map(MapValuesTranslate map);

    <K,U extends ValuesContext<U>> Map<K,U> translateValues(Map<K,U> map);
    <V extends Value> Set<V> translateValues(Set<V> values);
    <V extends Value> QuickSet<V> translateValues(QuickSet<V> values);
    <K,U extends Value> Map<K,U> translateMapValues(Map<K,U> map);
    <K extends Value,U> QuickMap<K,U> translateValuesMapKeys(QuickMap<K, U> map);

    boolean assertValuesEquals(Set<? extends Value> values);

    public MapValuesTranslate reverse();

    boolean identityValues(QuickSet<? extends Value> values);

    public HashValues getHashValues();
}
