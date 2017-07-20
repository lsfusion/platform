package lsfusion.base;

public interface ExceptionRunnable<E extends Exception> {
    void run() throws E;
}
