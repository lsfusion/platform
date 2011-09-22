package platform.gwt.paas.client.login;

import com.google.gwt.event.shared.EventHandler;

public abstract class LoginAuthenticatedEventHandler implements EventHandler {
    public abstract void onLogin(LoginAuthenticatedEvent event);
}
