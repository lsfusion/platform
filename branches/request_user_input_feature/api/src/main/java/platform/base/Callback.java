package platform.base;

public interface Callback<T> {
    void done(T result) throws Exception;
}