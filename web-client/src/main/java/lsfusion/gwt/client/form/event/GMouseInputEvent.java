package lsfusion.gwt.client.form.event;

import com.google.gwt.user.client.Event;

import java.util.Map;

public class GMouseInputEvent extends GInputEvent {

    public static final String CLK = "CLK";
    public static final String DBLCLK = "DBLCLK";

    public String mouseEvent;

    public GMouseInputEvent() {
    }

    public GMouseInputEvent(String mouseEvent) {
        this.mouseEvent = mouseEvent;
    }

    public GMouseInputEvent(Event e) {
        String event = "";
        if (e.getAltKey()) {
            event += "alt ";
        }
        if (e.getCtrlKey()) {
            event += "ctrl ";
        }
        if (e.getShiftKey()) {
            event += "shift ";
        }
        this.mouseEvent = event + (e.getTypeInt() == Event.ONCLICK ? CLK : DBLCLK);
    }

    public GMouseInputEvent(String mouseEvent, Map<String, GBindingMode> bindingModes) {
        super(bindingModes);
        this.mouseEvent = mouseEvent;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof GMouseInputEvent && mouseEvent.equals(((GMouseInputEvent) o).mouseEvent);
    }

    @Override
    public int hashCode() {
        return mouseEvent.hashCode();
    }

}
