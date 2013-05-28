package platform.gwt.paas.client.login;

import com.google.gwt.event.shared.GwtEvent;
import com.google.web.bindery.event.shared.EventBus;

public class LogoutAuthenticatedEvent extends GwtEvent<LogoutAuthenticatedEventHandler> {

    public static final Type<LogoutAuthenticatedEventHandler> TYPE = new Type<LogoutAuthenticatedEventHandler>();

    public static void fire(EventBus eventBus) {
        eventBus.fireEvent(new LogoutAuthenticatedEvent());
    }

    public LogoutAuthenticatedEvent() {
    }

    @Override
    protected void dispatch(LogoutAuthenticatedEventHandler handler) {
        handler.onLogout(this);
    }

    @Override
    public Type<LogoutAuthenticatedEventHandler> getAssociatedType() {
        return TYPE;
    }
}
