package lsfusion.gwt.client.controller.remote.action;

public interface RequestAsyncCallback<T> {

    void onSuccess(T result, Runnable onFinished);

    void onFailure(Throwable caught);
}
