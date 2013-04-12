package platform.gwt.paas.client.common;

import platform.gwt.paas.client.Paas;
import platform.gwt.paas.client.login.LogoutAuthenticatedEvent;
import platform.gwt.sgwtbase.client.SGWTErrorHandlingCallback;

public abstract class PaasCallback<T> extends SGWTErrorHandlingCallback<T> {
    @Override
    protected void relogin() {
        LogoutAuthenticatedEvent.fire(Paas.ginjector.getEventBus());
    }
}
