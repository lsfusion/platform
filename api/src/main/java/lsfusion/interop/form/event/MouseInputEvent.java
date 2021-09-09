package lsfusion.interop.form.event;

import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.Objects;

public class MouseInputEvent extends InputEvent {
    
    public static final String CLK = "CLK";
    public static final String DBLCLK = "DBLCLK";

    public final String mouseEvent;

    public MouseInputEvent(String mouseEvent) {
        this.mouseEvent = mouseEvent;
    }

    public MouseInputEvent(MouseEvent e, boolean doubleClick) {
        String event = "";
        if (e.isAltDown()) {
            event += "alt ";
        }
        if (e.isControlDown()) {
            event += "ctrl ";
        }
        if (e.isShiftDown()) {
            event += "shift ";
        }
        this.mouseEvent = event + (doubleClick ? DBLCLK : CLK);
    }

    public MouseInputEvent(String mouseEvent, Map<String, BindingMode> bindingModes) {
        super(bindingModes);
        this.mouseEvent = mouseEvent;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof MouseInputEvent && mouseEvent.equals(((MouseInputEvent) o).mouseEvent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mouseEvent);
    }
}
