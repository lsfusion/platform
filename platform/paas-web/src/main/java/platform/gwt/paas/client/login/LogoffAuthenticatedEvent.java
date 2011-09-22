package platform.gwt.paas.client.login;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.GwtEvent;

public class LogoffAuthenticatedEvent extends GwtEvent<LogoffAuthenticatedEventHandler> {

    public static final Type<LogoffAuthenticatedEventHandler> TYPE = new Type<LogoffAuthenticatedEventHandler>();

    public static void fire(EventBus eventBus) {
        eventBus.fireEvent(new LogoffAuthenticatedEvent());
    }

    public LogoffAuthenticatedEvent() {
    }

    @Override
    protected void dispatch(LogoffAuthenticatedEventHandler handler) {
        handler.onLogoff(this);
    }

    @Override
    public Type<LogoffAuthenticatedEventHandler> getAssociatedType() {
        return TYPE;
    }
}
