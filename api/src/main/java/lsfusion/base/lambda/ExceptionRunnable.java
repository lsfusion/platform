package lsfusion.base.lambda;

public interface ExceptionRunnable<E extends Exception> {
    void run() throws E;
}
