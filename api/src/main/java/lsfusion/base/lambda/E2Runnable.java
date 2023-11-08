package lsfusion.base.lambda;

@FunctionalInterface
public interface E2Runnable<E1 extends Exception, E2 extends Exception> {
    void run() throws E1, E2;
}