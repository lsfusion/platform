package lsfusion.gwt.client.base;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.classes.GInputType;
import lsfusion.gwt.client.form.property.cell.classes.view.InputBasedCellRenderer;
import lsfusion.gwt.client.view.MainFrame;

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
        NOTFOCUSABLE, // BINDING OR MOUSECHANGE ON NOTFOCUSABLE PROPERTY

        SUGGEST,
        REPLACE,
        OTHER;

        public boolean selectInputAll() {
            return this != REPLACE; // in replace we manage input selection manually
        }

        public boolean preventScroll() {
            return this == RESTOREFOCUS || this == MOUSECHANGE || this == SHOW;
        }
    }

    private final static String focusReason = "focusReason";

    public static Reason getFocusReason(Element element) {
        return (Reason) element.getPropertyObject(focusReason);
    }

    public static void focus(Element element, Reason reason) {
        focus(element, reason, null);
    }
    public static void focus(Element element, Reason reason, Event event) {
        Object prevReason = element.getPropertyObject(focusReason); // just in case when there are nested focuses
        assert prevReason == null || prevReason == reason;
        element.setPropertyObject(focusReason, reason);
        try {
            GInputType inputType;
            if(reason.selectInputAll() && (inputType = InputBasedCellRenderer.getInputElementType(element)) != null &&
                    inputType.isSelectAll()) {
                InputElement inputElement = (InputElement) element;
                if(event != null) { // we don't want mouse up because it will drop the selection
                    assert reason == Reason.MOUSECHANGE;
                    MainFrame.preventClickAfterDown(inputElement, event);
                }
                inputElement.select();
            }

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

    public static void blur(Element element) {
        element.blur();
        focus(element, FocusUtils.Reason.RESTOREFOCUS);
    }
}
