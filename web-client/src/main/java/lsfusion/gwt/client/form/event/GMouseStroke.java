package lsfusion.gwt.client.form.event;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.Event;

import static com.google.gwt.dom.client.BrowserEvents.DBLCLICK;
import static com.google.gwt.dom.client.BrowserEvents.MOUSEDOWN;

public class GMouseStroke {

    // there are two options what to consider CHANGE event
    // CLICK vs MOUSEDOWN
    // with CLICK there is a problem that focus is set on MOUSEDOWN (browser default behaviour), which causes unnecessary blinking (especially with not focusable elements in grid)
    // with MOUSEDOWN the problem is that double event should suppress single event if it was consumed (which happens with DBLCLICK / CLICK)
    // because the double click behaviour it's really hard to emulate, we'll just consume mousedown and focus on click manually

    public static boolean isChangeEvent(Event event) {
        return isClickEvent(event);
    }
    public static boolean isDoubleChangeEvent(Event event) {
        return isDblClickEvent(event);
    }

    public static boolean isDownEvent(Event event) {
        return MOUSEDOWN.equals(event.getType());
    }
    public static boolean isClickEvent(Event event) {
        return BrowserEvents.CLICK.equals(event.getType());
    }
    public static boolean isDblClickEvent(Event event) {
        return DBLCLICK.equals(event.getType());
    }
    public static boolean isEvent(Event event) {
        return isDownEvent(event) || isClickEvent(event) || isDblClickEvent(event);
    }
}
