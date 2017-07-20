package lsfusion.base;

public interface CallableWithParam<K, V> {
    V call(K arg);
}
