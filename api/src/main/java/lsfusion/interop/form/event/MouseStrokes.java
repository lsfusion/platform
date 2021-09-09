package lsfusion.interop.form.event;

import java.awt.event.MouseEvent;
import java.util.EventObject;

public class MouseStrokes {

    public static boolean isChangeEvent(EventObject event) {
        return isDownEvent(event);
    }
    public static boolean isDoubleChangeEvent(EventObject event) {
        return isDblClickEvent(event);
    }

    // we will use KEYDOWN everywhere since we usually use mousePressed
    public static boolean isDownEvent(EventObject event) {
        return event instanceof MouseEvent && ((MouseEvent) event).getID() == MouseEvent.MOUSE_PRESSED && ((MouseEvent) event).getButton() == MouseEvent.BUTTON1;
    }

    // in web-client we use dblclk (i.e click) for double click event
    // however in desktop mousePressed is used everywhere (particularly in *TableUI handler) with no mouse clicked (and we need all the cell determining infrastructure as in mousePressed, and it should be provided even for TableUI that is used in trees)
    // so in desktop we'll use mousedown, it shouldn't cause problems since it use only for editobject (where there is !hasChangeAction check) and for tree expand
    public static boolean isDblClickEvent(EventObject event) {
        return event instanceof MouseEvent && ((MouseEvent) event).getID() == MouseEvent.MOUSE_PRESSED && ((MouseEvent) event).getClickCount() % 2 == 0 && ((MouseEvent) event).getButton() == MouseEvent.BUTTON1;
    }
}
