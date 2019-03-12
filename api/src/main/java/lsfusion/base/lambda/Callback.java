package lsfusion.base.lambda;

public interface Callback<T> {
    void done(T result);
}