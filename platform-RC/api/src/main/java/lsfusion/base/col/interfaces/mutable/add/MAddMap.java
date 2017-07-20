package lsfusion.base.col.interfaces.mutable.add;

public interface MAddMap<K, V> {
    
    V get(K key);
    boolean containsKey(K key); // вот тут есть нюансы с null'ом
    boolean add(K key, V value);

    int size();
    K getKey(int i);
    V getValue(int i);
}
