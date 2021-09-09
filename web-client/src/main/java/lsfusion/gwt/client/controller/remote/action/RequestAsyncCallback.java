package lsfusion.gwt.client.controller.remote.action;

public interface RequestAsyncCallback<T> {

    void onSuccess(T result, Runnable onDispatchFinished);

    void onFailure(Throwable caught);
}
