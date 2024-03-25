package lsfusion.gwt.client.base;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.classes.GInputType;
import lsfusion.gwt.client.form.property.cell.classes.view.InputBasedCellRenderer;
import lsfusion.gwt.client.view.MainFrame;

import java.util.ArrayList;
import java.util.function.Consumer;

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
            select(element, reason, event); // need to be inside to have focusReason, because select can eventually call focus
            focus(element, reason.preventScroll());
        } finally {
            element.setPropertyObject(focusReason, prevReason);
        }
    }

    private static void select(Element element, Reason reason, Event event) {
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

    public static boolean isFakeBlur(NativeEvent event, Element blur) {
        EventTarget focus = event.getRelatedEventTarget();
        if(focus == null) {
            if(focusTransaction) {
                pendingBlurElements.add(blur);
                return true;
            }

            return false;
        }

        return isFakeBlur(blur, Element.as(focus));
    }

    private static boolean isFakeBlur(Element element, Element focusElement) {
        if(element.isOrHasChild(focusElement))
            return true;

        Element autoHidePartner = GwtClientUtils.getParentWithProperty(focusElement, "focusPartner");
        if(autoHidePartner != null)
            return isFakeBlur(element, (Element) autoHidePartner.getPropertyObject("focusPartner"));

        return false;
    }

    private static boolean focusTransaction = false;
    private static ArrayList<Element> pendingBlurElements = new ArrayList<>();

    public static void startFocusTransaction() {
        focusTransaction = true;
    }
    public static void endFocusTransaction() {
        focusTransaction = false;

        Element focusedElement = getFocusedElement();
        for(Element pendingBlurElement : pendingBlurElements) {
            if(focusedElement == null || !isFakeBlur(pendingBlurElement, focusedElement))
                fireOnBlur(pendingBlurElement);
        }
        pendingBlurElements.clear();
    }

    public static void addFocusPartner(Element element, Element partner) {
        partner.setPropertyObject("focusPartner", element);
        partner.setTabIndex(-1); // we need this to have related target in isFakeBlur, otherwise it won't work
        setOnFocusOut(partner, event -> {
            GWT.log("FOUT");
            if(!isFakeBlur(event, element)) { // if the focus is not returned to the element
                GWT.log("FBLUR");
                fireOnBlur(element); // ??? maybe relatedEventTarget should be preserved
            }
        });
    }

    public static boolean propagateFocusEvent(Element element, Event event) {
        // there is a problem with non-bubbling events (there are not many such events, see CellBasedWidgetImplStandard.nonBubblingEvents, so basically focus events):
        // handleNonBubblingEvent just looks for the first event listener
        // but if there are 2 widgets listening to the focus events (for example when in ActionOrPropertyValue there is an embedded form and there is a TableContainer inside)
        // then the lower widget gets both blur events (there 2 of them with different current targets, i.e supposed to be handled by different elements) and the upper widget gets none of them (which leads to the very undesirable behaviour with the "double" finishEditing, etc.)
        if(DataGrid.checkSinkFocusEvents(event)) { // there is a bubble field, but it does
            EventTarget currentEventTarget = event.getCurrentEventTarget();
            if(Element.is(currentEventTarget)) {
                Element currentTarget = currentEventTarget.cast();
                // maybe sinked focus events should be checked for the currentTarget
                if(!currentTarget.equals(element) && DOM.dispatchEvent(event, currentTarget))
                    return true;
            }
        }
        return false;
    }

    public static native void fireOnBlur(Element element)/*-{
        element.dispatchEvent(new FocusEvent("blur"));
    }-*/;

    public static native void setOnFocusOut(Element element, Consumer<NativeEvent> run)/*-{
        element.addEventListener("focusout", function(event) { // have no idea why onfocusout doesn't work
            run.@Consumer::accept(*)(event);
        });
    }-*/;

    public static Element getFocusedChild(Element containerElement) {
        Element focusedElement = getFocusedElement();
        if(containerElement.isOrHasChild(focusedElement))
            return focusedElement;
        return null;
    }

    public static native Element getFocusedElement() /*-{
        return $doc.activeElement;
    }-*/;
}
