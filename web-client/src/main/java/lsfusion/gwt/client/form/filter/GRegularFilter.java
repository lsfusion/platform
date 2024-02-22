package lsfusion.gwt.client.form.filter;

import lsfusion.gwt.client.form.event.GInputBindingEvent;
import lsfusion.gwt.client.form.event.GKeyInputEvent;
import lsfusion.gwt.client.form.event.GMouseInputEvent;

import java.io.Serializable;

public class GRegularFilter implements Serializable {
    public int ID;

    public String caption;
    public GInputBindingEvent bindingEvent;
    public boolean showKey;

    public GRegularFilter() {
        ID = -1;
    }

    public String getFullCaption() {
        String fullCaption = caption;
        if (showKey && bindingEvent != null) {
            fullCaption += " (" + (bindingEvent.inputEvent instanceof GMouseInputEvent ? ((GMouseInputEvent) bindingEvent.inputEvent).mouseEvent : ((GKeyInputEvent) bindingEvent.inputEvent).keyStroke) + ")";
        }
        return fullCaption;
    }
}
