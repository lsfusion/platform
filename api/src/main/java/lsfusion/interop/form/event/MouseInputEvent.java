package lsfusion.interop.form.event;

import java.util.Objects;

public class MouseInputEvent extends InputEvent {
    
    public static final MouseInputEvent DBLCLK = new MouseInputEvent("DBLCLK");
    
    public final String mouseEvent;

    public MouseInputEvent(String mouseEvent) {
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
