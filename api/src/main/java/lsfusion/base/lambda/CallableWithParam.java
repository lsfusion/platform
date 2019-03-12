package lsfusion.base.lambda;

public interface CallableWithParam<K, V> {
    V call(K arg);
}
