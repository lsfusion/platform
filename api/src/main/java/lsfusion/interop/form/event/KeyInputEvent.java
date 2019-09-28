package lsfusion.interop.form.event;

import javax.swing.*;
import java.util.Objects;

public class KeyInputEvent extends InputEvent {
    public final KeyStroke keyStroke;

    public KeyInputEvent(KeyStroke keyStroke) {
        this.keyStroke = keyStroke;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof KeyInputEvent && keyStroke.equals(((KeyInputEvent) o).keyStroke);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyStroke);
    }
}
