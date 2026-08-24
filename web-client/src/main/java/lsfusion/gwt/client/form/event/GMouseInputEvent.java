package lsfusion.gwt.client.form.event;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Event;

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
        // the declared ctrl modifier is matched against the command key (see GKeyStroke.isCommandKeyDown), otherwise on macOS a 'ctrl CLK'
        // binding would be dead - there ctrl + click is a right click
        this(dblClick, e.getAltKey(), GKeyStroke.isCommandKeyDown(e), e.getShiftKey());
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

    // mouseEvent itself is matched against the declared binding, so only its presentation is adapted
    public String getText() {
        return mouseEvent.replace("ctrl ", GKeyStroke.getCommandKeyText());
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof GMouseInputEvent && mouseEvent.equals(((GMouseInputEvent) o).mouseEvent);
    }

    @Override
    public boolean isEvent(Event event) {
        boolean doubleChangeEvent = GMouseStroke.isDoubleChangeEvent(event);
        return (GMouseStroke.isChangeEvent(event) || doubleChangeEvent) && equals(new GMouseInputEvent(event, doubleChangeEvent));
    }

    @Override
    public int hashCode() {
        return mouseEvent.hashCode();
    }

}
