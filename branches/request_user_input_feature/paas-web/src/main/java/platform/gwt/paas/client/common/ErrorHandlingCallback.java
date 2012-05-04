package platform.gwt.paas.client.common;

import platform.gwt.paas.client.Paas;
import platform.gwt.paas.client.login.LogoutAuthenticatedEvent;
import platform.gwt.paas.shared.exceptions.MessageException;
import platform.gwt.sgwtbase.client.ErrorAsyncCallback;

public abstract class ErrorHandlingCallback<T> extends ErrorAsyncCallback<T> {
    @Override
    protected void relogin() {
        LogoutAuthenticatedEvent.fire(Paas.ginjector.getEventBus());
    }

    @Override
    protected String getServerMessage(Throwable caught) {
        if (caught instanceof MessageException) {
            return caught.getMessage();
        }
        return null;
    }
}
