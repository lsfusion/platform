package lsfusion.base.col.interfaces.mutable.add;

import lsfusion.base.col.interfaces.immutable.ImMap;

public interface MAddMap<K, V> {
    
    V get(K key);
    boolean containsKey(K key); // вот тут есть нюансы с null'ом
    boolean add(K key, V value);

    boolean addAll(ImMap<? extends K, ? extends V> map);

    int size();
    K getKey(int i);
    V getValue(int i);
}
