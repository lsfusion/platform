package platform.gwt.paas.client.login;

import com.google.gwt.event.shared.EventHandler;

public abstract class LogoffAuthenticatedEventHandler implements EventHandler {
    public abstract void onLogoff(LogoffAuthenticatedEvent event);
}
