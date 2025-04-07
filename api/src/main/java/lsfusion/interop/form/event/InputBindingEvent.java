package lsfusion.interop.form.event;

import java.io.Serializable;

public class InputBindingEvent implements Serializable {
    public InputEvent inputEvent;
    public Integer priority;

    public static InputBindingEvent dumb = new InputBindingEvent(null, null);

    public InputBindingEvent(InputEvent inputEvent, Integer priority) {
        this.inputEvent = inputEvent;
        this.priority = priority;
    }
}