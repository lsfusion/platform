package lsfusion.base.col.interfaces.mutable.mapvalue;

@FunctionalInterface
public interface ThrowingBiFunction<K, V, M, E1 extends Exception, E2 extends Exception> {
    M apply(K key, V value) throws E1, E2;
}
