package lsfusion.base.col.interfaces.mutable.mapvalue;

@FunctionalInterface
public interface ThrowingFunction<V, M, E1 extends Exception, E2 extends Exception> {
    M apply(V value) throws E1, E2;
}
