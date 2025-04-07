package lsfusion.gwt.client.form.event;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public class GBindingEvent {
    public final GFormController.BindingCheck event;
    public final GBindingEnv env;
    public final GPropertyDraw property;
    public final Widget widget;
    public final boolean mouse;

    public GBindingEvent(GFormController.BindingCheck event, GBindingEnv env) {
        this(event, env, null, null, false);
    }

    public GBindingEvent(GFormController.BindingCheck event, GBindingEnv env, GPropertyDraw property, Widget widget, boolean mouse) {
        this.event = event;
        this.env = env;
        this.property = property;
        this.widget = widget;
        this.mouse = mouse;
    }
}
