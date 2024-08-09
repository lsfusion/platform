package lsfusion.gwt.client.base;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.RootPanel;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.classes.GInputType;
import lsfusion.gwt.client.form.property.cell.classes.view.InputBasedCellRenderer;
import lsfusion.gwt.client.view.MainFrame;

import java.util.ArrayList;
import java.util.function.Consumer;

public class FocusUtils {

    public static Element lastBlurredElement;

    // Event.addNativePreviewHandler(this::previewNativeEvent); doesn't work since only mouse events are propagated see DOM.previewEvent(evt) usages (only mouse and keyboard events are previewed);
    // this solution is not pretty clean since not all events are previewed, but for now, works pretty good
    public static void setLastBlurredElement(Element lastBlurredElement) {
        FocusUtils.lastBlurredElement = lastBlurredElement;
    }

    public static Element getLastBlurredElement() {
        return lastBlurredElement;
    }

    private static boolean lastBlurred = false;
    public static boolean focusLastBlurredElement(EventHandler focusEventHandler, Element focusEventElement) {
        // in theory we also have to check if focused element still visible, isShowing in GwtClientUtils but now it's assumed that it is always visible
        if(lastBlurredElement != null && focusReason == null && lastBlurredElement != focusEventElement) { // return focus back where it was
            focusEventHandler.consume();
            try {
                lastBlurred = true;
                focus(lastBlurredElement, Reason.RESTOREFOCUS);
            } finally {
                lastBlurred = false;
            }
            return true;
        }
        return false;
    }

    public static boolean isSuppressOnFocusChange(Element element) {
        Reason focusReason = getFocusReason(element);

        if(focusReason != null) { // system (probably navigate), so we will not suppress it
            switch (focusReason) {
                // for input setting focus will lead to starting change event handling "in between" (inside focus) with unpredictable consequences, so we'll not do that
                // we could not set focus at all (it will work because in SimpleTextBasedEditor we consume the event propagating to native (so focus will be set anyway)), but for now we'll do this way
                case MOUSECHANGE:
                // we don't focus to be set and rely on mouse event handling
                case MOUSENAVIGATE:
                // it's really odd to start editing while scrolling, and other navigating
                case SCROLLNAVIGATE:
                case KEYMOVENAVIGATE:
                // CHANGE will be started anyway
                case BINDING:
                // really odd behaviour to start editing (dropdown list) when focus is returned
                case RESTOREFOCUS:
                // not sure about SHOW, but it seems that this way is better
                case SHOW:
                // after applying filter, start editing does not make much sense
                case APPLYFILTER:
                // because there is a manual startediting
//                case NEWFILTER:
                case SUGGEST:
                case REPLACE:
                // unknown reason, it's better to suppress
                case OTHER:
                    return true;
            }
        }

        return !MainFrame.suppressOnFocusChange;
    }

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

    // buttons for example can be implicitly focusable
    public static native boolean isTabFocusable(Element element) /*-{
        return element.tabIndex >= "0";
    }-*/;

    public static native Element getNextFocusElement(Element formController, boolean forward) /*-{
        var elements = Array.prototype.filter.call(
            formController.querySelectorAll('.nav-item,.tableContainer,button,input,.panelRendererValue'), function (item) {
                //if element or one of its ancestors has display:none, offsetParent is null, so it's a sort of visibility check
                return @FocusUtils::isTabFocusable(*)(item) && item.offsetParent !== null
            });
        if(elements.length === 0)
            return null;
        var index = elements.indexOf($doc.activeElement);
        return forward ? (elements[index + 1] || elements[0]) : (elements[index - 1] || elements[elements.length - 1]);
    }-*/;

    public static Element getParentFocusElement(Element element) {
        Element parentElement = element.getParentElement();
        while (parentElement != null) {
            if (hasTabIndex(parentElement)) {
                return parentElement;
            }
            parentElement = parentElement.getParentElement();
        }
        return null;
    }

    private static boolean hasTabIndex(Element element) {
        return element.hasAttribute(TABINDEX) || isTabFocusable(element);
    }

    public static Element getInnerFocusElement(Element element) {
        if(hasTabIndex(element))
            return element;

        NodeList<Node> childNodes = element.getChildNodes();
        for(int i = 0, size = childNodes.getLength(); i < size; i++) {
            Node childNode = childNodes.getItem(i);
            if(Element.is(childNode)) {
                Element childFocusElement = getInnerFocusElement(Element.as(childNode));
                if(childFocusElement != null)
                    return childFocusElement;
            }
        }
        return null;
    }

    private static final String TABINDEX = "tabIndex";

    public static boolean focusIn(Element element, Reason reason) {
        Element childFocusElement = getInnerFocusElement(element);
        if(childFocusElement != null) {
            focus(childFocusElement, reason);
            return true;
        }
        return false;
    }

    public static void focusInOut(Element element, Reason reason) {
        if(!focusIn(element, reason))
            focusOut(element, reason);
    }
    public static void focusOut(Element element, Reason reason) {
        Element parentFocusElement = getParentFocusElement(element);
        if(parentFocusElement != null) // usually not null because at least body has tabIndex, but when element is removed from dom - not (in addFocusPartner / pending focus events)
            focus(parentFocusElement, reason);
    }

    // there are some tricky parts:
    // it seems that blur moves focus to the     body / html
    // focus doesn't move focus at all, if it doesn't have tabIndex
    public static void focus(Element element, Reason reason) {
        focus(element, reason, (Event)null);
    }
    public static void focus(Element element, Reason reason, Event event) {
        assert hasTabIndex(element);
        focus(element, reason, () -> {
            select(element, reason, event); // need to be inside to have focusReason, because select can eventually call focus
            focus(element, reason.preventScroll());
        });
    }

//    private final static String focusReason = "focusReason";
    private static Reason focusReason = null;

    public static void focus(Element element, Reason reason, Runnable setFocus) {
//        Object prevReason = element.getPropertyObject(focusReason); // just in case when there are nested focuses
//        element.setPropertyObject(focusReason, reason);
//        assert prevReason == null || prevReason == reason;
        Reason prevReason = focusReason;
        focusReason = reason;
        try {
            setFocus.run();
        } finally {
//            element.setPropertyObject(focusReason, prevReason);
            focusReason = prevReason;
        }
    }

    public static Reason getFocusReason(Element element) {
//        return (Reason) element.getPropertyObject(focusReason);
        return focusReason;
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

    public static boolean isFakeBlur(NativeEvent event, Element blur) {
        if(lastBlurred)
            return true;

        EventTarget focus = event.getRelatedEventTarget();
        if(focus == null) {
            // if we're in focus transaction then there are some manipulations in the focusTransaction elements, so we pend all blur events from there
            if(focusTransaction != null && isFakeBlurInner(focusTransaction, blur)) { // element might be removed
                pendingBlurElements.add(blur);
                return true;
            }

            return false;
        }

        return isFakeBlurInner(blur, Element.as(focus));
    }

    private static boolean isFakeBlurInner(Element element, Element focusElement) {
        if(element.isOrHasChild(focusElement))
            return true;

        Element autoHidePartner = GwtClientUtils.getParentWithProperty(focusElement, "focusPartner");
        if(autoHidePartner != null)
            return isFakeBlurInner(element, (Element) autoHidePartner.getPropertyObject("focusPartner"));

        return false;
    }

    private static Element focusTransaction;
    private static ArrayList<Element> pendingBlurElements = new ArrayList<>();

    public static void startFocusTransaction(Element element) {
        focusTransaction = element;
    }
    public static void endFocusTransaction() {
        focusTransaction = null;

        Element focusedElement = getFocusedElement();
        for(Element pendingBlurElement : pendingBlurElements) {
            if(!isFakeBlurInner(pendingBlurElement, focusedElement)) {
                // we need to return focus "inside" the element (to the element) and then to the focusedElement
                // pendingBlurElement is mostly focusable (dispatch branch, but can be not focusable)
                triggerFocus(reason -> focusInOut(pendingBlurElement, reason), focusedElement);
            }
        }
        pendingBlurElements.clear();
    }

    public static void addFocusPartner(Element element, Element partner) {
        partner.setPropertyObject("focusPartner", element);
        partner.setTabIndex(-1); // we need focus partner to get focus, otherwise it will go to the body element
        setOnFocusOut(partner, event -> {
            // we need to return focus to the element and then to the event.relatedEventTarget (getFocusedElement ?)
            EventTarget newFocus = event.getRelatedEventTarget();
            Element newFocusElement = newFocus != null ? Element.as(newFocus) : RootPanel.getBodyElement();
//            assert newFocusElement == FocusUtils.getFocusedElement();
            triggerFocus(reason -> focusInOut(element, reason), newFocusElement);
        });
    }

    public static void triggerFocus(Consumer<Reason> focus, Element returnElement) {
        Reason focusReason = Reason.RESTOREFOCUS;
        focus.accept(focusReason);
        focus(returnElement, focusReason);
    }

    public static void setOnFocusOutWithDropDownPartner(Element element, Element dropdown, JavaScriptObject fnc) {
        setOnFocusOutFnc(element, fnc);
        GwtClientUtils.addDropDownPartner(element, dropdown);
    }
    public static void setOnFocusOutFnc(Element element, JavaScriptObject fnc) {
        setOnFocusOut(element, nativeEvent -> GwtClientUtils.call(fnc, nativeEvent));
    }
    public static native void setOnFocusOut(Element element, Consumer<NativeEvent> run)/*-{
        element.focusOutHandler = function (event) { // have no idea why onfocusout doesn't work
            if(!@lsfusion.gwt.client.base.FocusUtils::isFakeBlur(*)(event, element)) // if the focus is not returned to the element
                run.@Consumer::accept(*)(event);
        }
        element.addEventListener("focusout", element.focusOutHandler);
    }-*/;
    public static native void removeOnFocusOut(Element element)/*-{
        element.removeEventListener("focusout", element.focusOutHandler);
        element.focusOutHandler = null;
    }-*/;

    public static Element getFocusedChild(Element containerElement) {
        Element focusedElement = getFocusedElement();
        if(isFakeBlurInner(containerElement, focusedElement))
            return focusedElement;
        return null;
    }

    public static native Element getFocusedElement() /*-{
        return $doc.activeElement;
    }-*/;
}
