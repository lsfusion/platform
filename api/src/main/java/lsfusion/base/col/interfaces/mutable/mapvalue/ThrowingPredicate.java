package lsfusion.base.col.interfaces.mutable.mapvalue;

public interface ThrowingPredicate<K, E1 extends Exception, E2 extends Exception> {

    boolean test(K key) throws E1, E2;
}
