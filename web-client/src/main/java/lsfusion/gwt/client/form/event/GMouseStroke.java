package lsfusion.gwt.client.form.event;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.*;

import static com.google.gwt.dom.client.BrowserEvents.DBLCLICK;
import static com.google.gwt.dom.client.BrowserEvents.MOUSEDOWN;

public class GMouseStroke {

    // there are two options what to consider CHANGE event
    // CLICK vs MOUSEDOWN
    // with CLICK there is a problem that focus is set on MOUSEDOWN (browser default behaviour), which causes unnecessary blinking (especially with not focusable elements in grid)
    // with MOUSEDOWN the problem is that double event should suppress single event if it was consumed (which happens with DBLCLICK / CLICK)

    public static boolean isChangeEvent(NativeEvent event) {
        return MOUSEDOWN.equals(event.getType());
    }
    public static boolean isChangeEventConsumesFocus() { // since MOUSEDOWN is used for focus, we need to propagate it to native
        return true;
    }
    public static boolean isDoubleChangeEvent(NativeEvent event) {
        return DBLCLICK.equals(event.getType());
    }
}
