package lsfusion.base.col.interfaces.mutable.mapvalue;

@FunctionalInterface
public interface ThrowingIntObjectFunction<V, M, E1 extends Exception, E2 extends Exception> {
    M apply(int i, V value) throws E1, E2;
}
