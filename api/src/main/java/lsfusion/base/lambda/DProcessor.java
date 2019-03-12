package lsfusion.base.lambda;

public interface DProcessor<K, V> {
    
    void proceed(K key, V value); 
}
