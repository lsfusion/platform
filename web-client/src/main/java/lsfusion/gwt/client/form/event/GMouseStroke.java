package lsfusion.gwt.client.form.event;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Event;

import static com.google.gwt.dom.client.BrowserEvents.*;

public class GMouseStroke {

    // there are two options what to consider CHANGE event
    // CLICK vs MOUSEDOWN
    // with CLICK there is a problem that focus is set on MOUSEDOWN (browser default behaviour), which causes unnecessary blinking (especially with not focusable elements in grid)
    // for CLICK - we just consume mousedown and focus on click manually

    public static boolean isChangeEvent(Event event) {
        return isDownEvent(event);
    }

    public static boolean isChangeEvent(NativeEvent event) {
        return isDownEvent(event);
    }
    public static boolean isDoubleChangeEvent(Event event) {
        return isDblClickEvent(event);
    }

    public static boolean isDownEvent(Event event) {
        return MOUSEDOWN.equals(event.getType()) && event.getButton() == NativeEvent.BUTTON_LEFT;
    }
    public static boolean isDownEvent(NativeEvent event) {
        return MOUSEDOWN.equals(event.getType()) && event.getButton() == NativeEvent.BUTTON_LEFT;
    }
    public static boolean isUpEvent(Event event) {
        return MOUSEUP.equals(event.getType()) && event.getButton() == NativeEvent.BUTTON_LEFT;
    }
    public static boolean isClickEvent(Event event) {
        return BrowserEvents.CLICK.equals(event.getType()) && event.getButton() == NativeEvent.BUTTON_LEFT;
    }
    public static boolean isDblClickEvent(Event event) {
        return DBLCLICK.equals(event.getType()) && event.getButton() == NativeEvent.BUTTON_LEFT;
    }
    public static boolean isDblDownEvent(Event event) {
        return isDownEvent(event) && isDblEvent(event);
    }
    public static native boolean isDblEvent (Event event) /*-{
        return event.detail > 1;
    }-*/;

    public static boolean isEvent(Event event) {
        return isDownEvent(event) || isClickEvent(event) || isDblClickEvent(event);
    }

    // technically CONTEXTMENU is not mouse event, but PASTE is not key event either, so we'll leave it here
    public static boolean isContextMenuEvent(Event event) {
        return BrowserEvents.CONTEXTMENU.equals(event.getType());
    }
}
