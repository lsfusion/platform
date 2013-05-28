package platform.gwt.paas.client.login;

import com.google.gwt.event.shared.EventHandler;

public abstract class LogoutAuthenticatedEventHandler implements EventHandler {
    public abstract void onLogout(LogoutAuthenticatedEvent event);
}
