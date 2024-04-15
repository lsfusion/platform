package lsfusion.gwt.client.form.controller;

import lsfusion.gwt.client.base.view.PopupOwner;
import lsfusion.gwt.client.controller.remote.action.PriorityErrorHandlingCallback;
import lsfusion.gwt.client.controller.remote.action.RequestCountingErrorHandlingCallback;
import lsfusion.gwt.client.form.controller.dispatch.ExceptionResult;

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
        PriorityErrorHandlingCallback.showErrorMessage(exceptionResult.throwable, getPopupOwner());
    }

    public abstract PopupOwner getPopupOwner();
}
