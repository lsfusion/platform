package lsfusion.base.col.interfaces.mutable.add;

import lsfusion.base.col.interfaces.immutable.ImMap;

public interface MAddExclMap<K, V> {
    V get(K key);
    boolean containsKey(K key); // вот тут есть нюансы с null'ом
    void exclAdd(K key, V value);
    void exclAddAll(ImMap<? extends K, ? extends V> values);

    int size();
    K getKey(int i);
    V getValue(int i);
    Iterable<K> keyIt();
}
