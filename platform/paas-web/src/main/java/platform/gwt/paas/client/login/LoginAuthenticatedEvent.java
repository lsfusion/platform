package platform.gwt.paas.client.login;

import com.google.gwt.event.shared.GwtEvent;
import com.google.web.bindery.event.shared.EventBus;

public class LoginAuthenticatedEvent extends GwtEvent<LoginAuthenticatedEventHandler> {

    public static final Type<LoginAuthenticatedEventHandler> TYPE = new Type<LoginAuthenticatedEventHandler>();

    public static void fire(EventBus eventBus, String userName) {
        fire(eventBus, new CurrentUser(userName));
    }

    public static void fire(EventBus eventBus, CurrentUser currentUser) {
        eventBus.fireEvent(new LoginAuthenticatedEvent(currentUser));
    }


    private final CurrentUser currentUser;

    public LoginAuthenticatedEvent(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    public CurrentUser getCurrentUser() {
        return currentUser;
    }

    @Override
    protected void dispatch(LoginAuthenticatedEventHandler handler) {
        handler.onLogin(this);
    }

    @Override
    public Type<LoginAuthenticatedEventHandler> getAssociatedType() {
        return TYPE;
    }
}
