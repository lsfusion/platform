package lsfusion.base.col.interfaces.mutable.mapvalue;

@FunctionalInterface
public interface ThrowingSupplier<V, E1 extends Exception, E2 extends Exception> {
    V get() throws E1, E2;
}
