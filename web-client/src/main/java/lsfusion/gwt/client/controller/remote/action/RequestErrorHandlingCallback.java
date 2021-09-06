package lsfusion.gwt.client.controller.remote.action;

import static lsfusion.gwt.client.controller.remote.action.PriorityErrorHandlingCallback.showErrorMessage;

public abstract class RequestErrorHandlingCallback<T> implements RequestAsyncCallback<T> {

    @Override
    public void onFailure(Throwable caught) {
        showErrorMessage(caught);
    }
}
