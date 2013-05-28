package platform.gwt.cellview.client;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

public class KeyboardRowChangedEvent extends GwtEvent<KeyboardRowChangedEvent.Handler> {

    public static interface Handler extends EventHandler {
        void onKeyboardRowChanged(KeyboardRowChangedEvent event);
    }

    public interface HasKeyboardRowChangedHandlers extends HasHandlers {
        HandlerRegistration addKeyboardRowChangedHandler(Handler handler);
    }

    private static Type<KeyboardRowChangedEvent.Handler> TYPE;

    public static void fire(HasKeyboardRowChangedHandlers source) {
        if (TYPE != null) {
            KeyboardRowChangedEvent event = new KeyboardRowChangedEvent();
            source.fireEvent(event);
        }
    }

    public static Type<KeyboardRowChangedEvent.Handler> getType() {
        if (TYPE == null) {
            TYPE = new Type<KeyboardRowChangedEvent.Handler>();
        }
        return TYPE;
    }

    KeyboardRowChangedEvent() {
    }

    @Override
    public final Type<KeyboardRowChangedEvent.Handler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(KeyboardRowChangedEvent.Handler handler) {
        handler.onKeyboardRowChanged(this);
    }
}
