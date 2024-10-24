package lsfusion.base.lambda;

@FunctionalInterface
public interface E2Consumer<T, E1 extends Exception, E2 extends Exception> {
    void accept(T param) throws E1, E2;
}