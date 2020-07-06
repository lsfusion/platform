package lsfusion.gwt.client.form.event;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.NativeEvent;
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

    public GMouseInputEvent(NativeEvent e, boolean dblClick) {
        this(dblClick, e.getAltKey(), e.getCtrlKey(), e.getShiftKey());
    }

    private GMouseInputEvent(boolean dblClick, boolean alt, boolean ctrl, boolean shift) {
        String event = "";
        if (alt) {
            event += "alt ";
        }
        if (ctrl) {
            event += "ctrl ";
        }
        if (shift) {
            event += "shift ";
        }
        this.mouseEvent = event + (dblClick ? DBLCLK : CLK);
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
