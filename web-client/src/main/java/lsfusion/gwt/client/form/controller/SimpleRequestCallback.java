package lsfusion.gwt.client.form.controller;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.controller.remote.action.RequestCountingErrorHandlingCallback;
import lsfusion.gwt.client.form.controller.dispatch.ExceptionResult;

import static lsfusion.gwt.client.controller.remote.action.PriorityErrorHandlingCallback.showErrorMessage;

public abstract class SimpleRequestCallback<T> extends RequestCountingErrorHandlingCallback<T> {

    protected abstract void onSuccess(T result);

    @Override
    public void onSuccess(T result, Runnable onDispatchFinished) {
        onSuccess(result);

        if (onDispatchFinished != null)
            onDispatchFinished.run();
    }

    @Override
    public void onFailure(ExceptionResult exceptionResult) {
        showErrorMessage(exceptionResult.throwable, getPopupOwnerWidget());
    }

    public abstract Widget getPopupOwnerWidget();
}
