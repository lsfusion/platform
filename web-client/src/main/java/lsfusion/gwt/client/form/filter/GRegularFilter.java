package lsfusion.gwt.client.form.filter;

import lsfusion.gwt.client.form.event.GInputBindingEvent;
import lsfusion.gwt.client.form.event.GKeyInputEvent;
import lsfusion.gwt.client.form.event.GMouseInputEvent;

import java.io.Serializable;
import java.util.ArrayList;

public class GRegularFilter implements Serializable {
    public int ID;

    public String caption;
    public ArrayList<GInputBindingEvent> bindingEvents = new ArrayList<>();
    public boolean showKey;
    public boolean showMouse;

    public GRegularFilter() {
        ID = -1;
    }

    public String getFullCaption() {
        String keyEventCaption = showKey && hasKeyBinding() ? getKeyBindingText() : null;
        String mouseEventCaption = showMouse && hasMouseBinding() ? getMouseBindingText() : null;
        String eventCaption = keyEventCaption != null ? (mouseEventCaption != null ? (keyEventCaption + " / " + mouseEventCaption) : keyEventCaption) : mouseEventCaption;
        return caption + (eventCaption != null ? " (" + eventCaption + ")" : "");
    }

    private boolean hasKeyBinding() {
        for(GInputBindingEvent bindingEvent : bindingEvents)
            if(bindingEvent.inputEvent instanceof GKeyInputEvent)
                return true;
        return false;
    }
    private String getKeyBindingText() {
        assert hasKeyBinding();
        String result = "";
        for(GInputBindingEvent bindingEvent : bindingEvents)
            if(bindingEvent.inputEvent instanceof GKeyInputEvent) {
                result = (result.isEmpty() ? "" : result + ",") + ((GKeyInputEvent) bindingEvent.inputEvent).keyStroke;
            }
        return result;
    }

    private boolean hasMouseBinding() {
        for(GInputBindingEvent bindingEvent : bindingEvents)
            if(bindingEvent.inputEvent instanceof GMouseInputEvent)
                return true;
        return false;
    }
    private String getMouseBindingText() {
        assert hasMouseBinding();
        String result = "";
        for(GInputBindingEvent bindingEvent : bindingEvents)
            if(bindingEvent.inputEvent instanceof GMouseInputEvent) {
                result = (result.isEmpty() ? "" : result + ",") + ((GMouseInputEvent) bindingEvent.inputEvent).mouseEvent;
            }
        return result;
    }
}
