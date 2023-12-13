package lsfusion.gwt.client.controller.remote.action;

import lsfusion.gwt.client.form.controller.dispatch.ExceptionResult;

public interface RequestAsyncCallback<T> {

    void onSuccess(T result, Runnable onDispatchFinished);

    void onFailure(ExceptionResult exceptionResult);
}
