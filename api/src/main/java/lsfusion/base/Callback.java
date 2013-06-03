package lsfusion.base;

public interface Callback<T> {
    void done(T result) throws Exception;
}