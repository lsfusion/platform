package lsfusion.base;

public interface DProcessor<K, V> {
    
    void proceed(K key, V value); 
}
