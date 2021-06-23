package lsfusion.interop.form.event;

import java.awt.event.MouseEvent;
import java.util.EventObject;

public class MouseStrokes {

    public static boolean isDblClickEvent(EventObject event) {
        return event instanceof MouseEvent && ((MouseEvent) event).getClickCount() % 2 == 0 && ((MouseEvent) event).getButton() == MouseEvent.BUTTON1;
    }
}
