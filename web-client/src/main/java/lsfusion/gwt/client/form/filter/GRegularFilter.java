package lsfusion.gwt.client.form.filter;

import lsfusion.gwt.client.form.event.GInputBindingEvent;
import lsfusion.gwt.client.form.event.GKeyInputEvent;
import lsfusion.gwt.client.form.event.GMouseInputEvent;

import java.io.Serializable;

public class GRegularFilter implements Serializable {
    public int ID;

    public String caption;
    public GInputBindingEvent keyBindingEvent;
    public boolean showKey;
    public GInputBindingEvent mouseBindingEvent;
    public boolean showMouse;

    public GRegularFilter() {
        ID = -1;
    }

    public String getFullCaption() {
        String keyEventCaption = showKey && keyBindingEvent != null ? ((GKeyInputEvent) keyBindingEvent.inputEvent).keyStroke.toString() : null;
        String mouseEventCaption = showMouse && mouseBindingEvent != null ? ((GMouseInputEvent) mouseBindingEvent.inputEvent).mouseEvent : null;
        String eventCaption = keyEventCaption != null ? (mouseEventCaption != null ? (keyEventCaption + " / " + mouseEventCaption) : keyEventCaption) : mouseEventCaption;
        return caption + (eventCaption != null ? " (" + eventCaption + ")" : "");
    }
}
