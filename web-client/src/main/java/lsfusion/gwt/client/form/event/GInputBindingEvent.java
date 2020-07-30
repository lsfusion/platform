package lsfusion.gwt.client.form.event;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

public class GInputBindingEvent implements Serializable {
    public GInputEvent inputEvent;
    public GBindingEnv env;

    public GInputBindingEvent() {
    }

    public GInputBindingEvent(GInputEvent event, GBindingEnv env) {
        inputEvent = event;
        this.env = env;
    }
}
