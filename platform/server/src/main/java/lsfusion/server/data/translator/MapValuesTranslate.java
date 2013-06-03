package lsfusion.server.data.translator;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.caches.ValuesContext;
import lsfusion.server.caches.hash.HashValues;
import lsfusion.server.data.Value;

public interface MapValuesTranslate extends MapObject {

    <V extends Value> V translate(V expr);

    int hash(HashValues hashValues);
    ImSet<Value> getValues();

    // extend'ит интерфейс до MapTranslate
    MapTranslate mapKeys();

    MapValuesTranslate filter(ImSet<? extends Value> values);

    MapValuesTranslate map(MapValuesTranslate map);

    <K,U extends ValuesContext<U>> ImMap<K, U> translateValues(ImMap<K, U> map);
    <K1,U1 extends ValuesContext<U1>,K2,U2 extends ValuesContext<U2>> ImMap<ImMap<K1,U1>,ImMap<K2,U2>> translateMapKeyValues(ImMap<ImMap<K1,U1>,ImMap<K2,U2>> map);
    <K1,U1 extends ValuesContext<U1>,U2 extends ValuesContext<U2>> ImMap<ImMap<K1,U1>,U2> translateMapKeyValue(ImMap<ImMap<K1,U1>,U2> map);
    <V extends Value> ImSet<V> translateValues(ImSet<V> values);
    <K,U extends Value> ImRevMap<K, U> translateMapValues(ImRevMap<K, U> map);
    <K,U extends Value> ImMap<K, U> translateMapValues(ImMap<K, U> map);
    <K extends Value,U> ImMap<K, U> translateValuesMapKeys(ImMap<K, U> map);
    <K extends Value,U> ImRevMap<K, U> translateValuesMapKeys(ImRevMap<K, U> map);

    boolean assertValuesEquals(ImSet<? extends Value> values);

    public MapValuesTranslate reverse();

    boolean identityValues(ImSet<? extends Value> values);

    public HashValues getHashValues();
}
