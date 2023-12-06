package lsfusion.gwt.client.base;

import com.google.gwt.dom.client.Element;

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

        OTHER;

        public boolean preventScroll() {
            return this == RESTOREFOCUS || this == MOUSECHANGE || this == SHOW;
        }
    }

    private final static String focusReason = "focusReason";

    public static Reason getFocusReason(Element element) {
        return (Reason) element.getPropertyObject(focusReason);
    }

    public static void focus(Element element, Reason reason) {
        Object prevReason = element.getPropertyObject(focusReason); // just in case when there are nested focuses
        assert prevReason == null || prevReason == reason;
        element.setPropertyObject(focusReason, reason);
        try {
            focus(element, reason.preventScroll());
        } finally {
            element.setPropertyObject(focusReason, prevReason);
        }
    }

    private static native void focus(Element element, boolean preventScroll) /*-{
        element.focus({
            preventScroll: preventScroll
        });
    }-*/;
}
