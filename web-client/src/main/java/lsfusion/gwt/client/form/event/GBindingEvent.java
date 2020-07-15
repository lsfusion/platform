package lsfusion.gwt.client.form.event;

import lsfusion.gwt.client.form.controller.GFormController;

import java.util.function.Predicate;

public class GBindingEvent {

//    public Predicate<GInputEvent> eventFilter;

    public final GFormController.BindingCheck event;
    public final GBindingEnv env;

    public GBindingEvent(GFormController.BindingCheck event, GBindingEnv env) {
        this.event = event;
        this.env = env;
    }
}
