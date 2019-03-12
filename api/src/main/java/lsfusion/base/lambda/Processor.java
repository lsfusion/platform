package lsfusion.base.lambda;

public interface Processor<V> {
    
    void proceed(V value);
}
