package lsfusion.base.col.interfaces.mutable.add;

public interface MAddExclMap<K, V> {
    V get(K key);
    boolean containsKey(K key); // вот тут есть нюансы с null'ом
    void exclAdd(K key, V value);

    int size();
    K getKey(int i);
    V getValue(int i);
    Iterable<K> keyIt();
}
