package lsfusion.gwt.client.base;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public class FocusUtils {

    public enum Reason {
        SHOW, // when container is shown
        RESTOREFOCUS, // when container is hidden, editing stopped, i.e.

        ACTIVATE, // ACTIVATE PROPERTY
        APPLYFILTER, // apply filter (returning focus to grid), new condition
        NEWFILTER, // new filter condition

        KEYMOVENAVIGATE, // UP, DOWN, LEFT, RIGHT
        KEYNEXTNAVIGATE, // ENTER
        MOUSENAVIGATE, // MOUSE CLICK
        SCROLLNAVIGATE, // SCROLL

        MOUSECHANGE,
        BINDING,

        OTHER
    }

    private final static String focusReason = "focusReason";

    public static Reason getFocusReason(Element element) {
        return (Reason) element.getPropertyObject(focusReason);
    }

    public static void focus(Element element, Reason reason) {
        element.setPropertyObject(focusReason, reason);
        try {
            element.focus();
        } finally {
            element.setPropertyObject(focusReason, null);
        }
    }
}
