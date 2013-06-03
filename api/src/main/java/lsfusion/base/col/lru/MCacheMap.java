package lsfusion.base.col.lru;

public interface MCacheMap<K, V> {
    V get(K key);
    boolean containsKey(K key); // вот тут есть нюансы с null'ом
    void exclAdd(K key, V value);
}
