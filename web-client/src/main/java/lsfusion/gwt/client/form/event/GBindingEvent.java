package lsfusion.gwt.client.form.event;

import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.function.Predicate;

public class GBindingEvent {

//    public Predicate<GInputEvent> eventFilter;

    public final GFormController.BindingCheck event;
    public final GBindingEnv env;
    public final GPropertyDraw property;
    public final boolean mouse;

    public GBindingEvent(GFormController.BindingCheck event, GBindingEnv env) {
        this(event, env, null, false);
    }

    public GBindingEvent(GFormController.BindingCheck event, GBindingEnv env, GPropertyDraw property, boolean mouse) {
        this.event = event;
        this.env = env;
        this.property = property;
        this.mouse = mouse;
    }
}
