package lsfusion.base;

public interface E2Callable<R,E1 extends Exception, E2 extends Exception> {
    R call() throws E1, E2;
}
