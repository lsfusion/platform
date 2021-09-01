package lsfusion.base.lambda;

public interface AsyncCallback<T> {

    void done(T result);

    void failure(Throwable t);
}
