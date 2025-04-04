package lsfusion.gwt.client.form.event;

import java.io.Serializable;

public class GInputBindingEvent implements Serializable {
    public GInputEvent inputEvent;
    public GBindingEnv env;

    public static GInputBindingEvent dumb = new GInputBindingEvent(null, null);

    public GInputBindingEvent() {
    }

    public GInputBindingEvent(GInputEvent event, GBindingEnv env) {
        inputEvent = event;
        this.env = env;
    }
}
