package lsfusion.gwt.client.base.resize;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Result;
import lsfusion.gwt.client.base.view.FlexPanel;

import java.util.List;
import java.util.function.Supplier;

import static com.google.gwt.dom.client.BrowserEvents.*;
import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;

public class ResizeHandler implements Event.NativePreviewHandler {
    public static final int ANCHOR_WIDTH = 10;
    public static ResizeHandler resizeHandler = null;

    private final ResizeHelper helper;

    private HandlerRegistration previewHandlerReg;

    private int initialMouse;
    private final int index;

    public ResizeHandler(ResizeHelper helper, int index) {
        this.helper = helper;
        this.index = index;

        previewHandlerReg = Event.addNativePreviewHandler(this);
        this.initialMouse = getAbsoluteRight();
    }

    public static <C> void dropCursor(Element cursorElement, NativeEvent event) {
        String eventType = event.getType();
        if ((MOUSEMOVE.equals(eventType) || MOUSEDOWN.equals(eventType)) && resizeHandler == null) {
            cursorElement.getStyle().clearCursor();
        }
    }
    public static <C> void setCursor(Element cursorElement, boolean isResizeOnScroll, Style.Cursor cursor) {
        // there is a bug / feature in Chrome, that cursor over scroll is not default, but is inherited from the parent
        // which is not we want in case of the components resize, because scroll events are handled first (and thus resize cursor doesn't make sense)
        // to solve this we manually check if the mouse is over some scroll (however maybe such complicated mechanism is not need and we could've used something more simple)
        if(!isResizeOnScroll)
            cursorElement.getStyle().setCursor(cursor);
    }

    // it's important that checkResize event should be called for cursorElement element (otherwise, cursor will be incorrect)
    // returns whether event was handled or not. used in window resizing, where both vertical and horizontal are handled at the same time 
    public static <C> boolean checkResizeEvent(ResizeHelper helper, Element cursorElement, Supplier<Integer> childIndexSupplier, NativeEvent event) {
        String eventType = event.getType();
        if ((MOUSEMOVE.equals(eventType) || MOUSEDOWN.equals(eventType)) && resizeHandler == null) {
            Result<Boolean> isOnScroll = new Result<>();
            ResizedChild resizedChild = getResizedChild(helper, event, childIndexSupplier, isOnScroll);
            if (resizedChild != null && resizedChild.mainBorder && helper.isChildResizable(resizedChild.index)) {
                setCursor(cursorElement, !resizedChild.outsideBorder && (isOnScroll.result || helper.isResizeOnScroll(resizedChild.index, event)),
                        helper.isVertical() ? Style.Cursor.ROW_RESIZE : Style.Cursor.COL_RESIZE);
                if (eventType.equals(MOUSEDOWN)) {
                    resizeHandler = new ResizeHandler(helper, resizedChild.index);
                    disableSelection();

                    // if we're not propagating native event, then BLUR event is also canceled and for example editing is not finished
                    // in all other cases it doesn't matter, since there is no stop propagation for MOUSEDOWN event, and it is the only known problem with preventing default (except expand tree but it seems that in grid everything works fine anyway)
                    stopPropagation(event, true, false);
                }
                return true;
            } else {
                // this container can be not resizable, but the inner one can be resizable, however it will not get a mouse move event if the cursor is to the right
                // so we push this event down to that container
                // in theory we should check that event is trigger to the right of this child widget, but since this child widget can have paddings / margins, we'll just do some extra work
                if(resizedChild != null)
                    helper.propagateChildResizeEvent(resizedChild.index + (resizedChild.outsideBorder ? 1 : 0), event, cursorElement);
            }
        }
        return false;
    }

    public static int getEventPosition(boolean vertical, boolean main, NativeEvent event) {
        return vertical == main ? event.getClientY() : event.getClientX();
    }
    public static int getEventPosition(ResizeHelper helper, NativeEvent event) {
        return getEventPosition(helper.isVertical(), true, event);
    }

    private static int getAbsolutePosition(ResizeHelper helper, int index, boolean left) {
        return helper.getChildAbsolutePosition(index, left);
    }

    private static int getScrollSize(ResizeHelper helper, int index) {
        return helper.getScrollSize(index);
    }

    public static int getAbsolutePosition(Element element, boolean vertical, boolean left) {
        return vertical ? (left ? element.getAbsoluteTop() : element.getAbsoluteBottom()) : (left ? element.getAbsoluteLeft() : element.getAbsoluteRight());
    }

    public static int getScrollSize(Element element, boolean vertical) {
        return vertical ? GwtClientUtils.getScrollHeight(element) :  GwtClientUtils.getScrollWidth(element);
    }

    private static class ResizedChild {
        public final int index;
        public final boolean mainBorder;
        public final boolean outsideBorder; // if the mouse is close to the border but outside it

        public ResizedChild(int index, boolean mainBorder, boolean outsideBorder) {
            this.index = index;
            this.mainBorder = mainBorder;
            this.outsideBorder = outsideBorder;
        }
    }

    private static ResizedChild getResizedChild(ResizeHelper helper, NativeEvent event, Supplier<Integer> childIndexSupplier, Result<Boolean> isOnScroll) {
        int mouse = getEventPosition(helper, event);

//        String attribute = helper.getElement().getAttribute("lsfusion-container");
//        if(attribute != null && attribute.contains("specPick"))
//            attribute = attribute;

        if(childIndexSupplier != null) { // this branch is needed when we get child from event
            int childIndex = childIndexSupplier.get();
            int anchorRight = getAbsolutePosition(helper, childIndex, false) - ANCHOR_WIDTH;
            int anchorLeft = getAbsolutePosition(helper, childIndex, true) + ANCHOR_WIDTH;
            if ((mouse > anchorRight && childIndex != helper.getChildCount() - 1) || (mouse < anchorLeft && childIndex > 0)) {
                if (mouse < anchorLeft)
                    childIndex--;
                if(isOnScroll != null)
                    isOnScroll.set(false);
                return new ResizedChild(childIndex, true, false); // outside border doesn't matter for that branch
            }
        } else {
            for(int i=0,size=helper.getChildCount();i<size;i++) {
                int right = getAbsolutePosition(helper, i, false);
                boolean mainBorder = Math.abs(mouse - right) < ANCHOR_WIDTH;
                if (mainBorder || right >= mouse) {
                    if(isOnScroll != null)
                        isOnScroll.set(right - mouse < getScrollSize(helper, i));
                    return new ResizedChild(i, mainBorder, right < mouse && i < size - 1);
                }
            }
        }
        return null;
    }

    public static int getResizedChild(boolean vertical, List<Widget> widgets, NativeEvent event, int offset, Result<Boolean> isOnScroll) {
        int mouse = getEventPosition(vertical, true, event);
        for(int i=0,size=widgets.size();i<size;i++) {
            Element element = widgets.get(i).getElement();
            int left = getAbsolutePosition(element, vertical, true);
            if(left - mouse > offset)
                return -1;
            int right = getAbsolutePosition(element, vertical, false);
            if (mouse - right < offset) {
                if(isOnScroll != null)
                    isOnScroll.set(right - mouse < getScrollSize(element, vertical));
                return i;
            }
        }
        return -1;
    }
    public static int getResizedChild(boolean vertical, NativeEvent event, List<FlexPanel.FlexLine> lines, int offset, Result<Boolean> isOnScroll) {
        int mouse = getEventPosition(vertical, true, event);
        for(int i=0,size=lines.size();i<size;i++) {
            FlexPanel.FlexLine flexLine = lines.get(i);
            int left = flexLine.getAbsolutePosition(true);
            if(left - mouse > offset)
                return -1;
            int right = flexLine.getAbsolutePosition(false);
            if (mouse - right < offset) {
                if(isOnScroll != null)
                    isOnScroll.set(right - mouse < flexLine.getScrollSize());
                return i;
            }
        }
        return -1;
    }

    private int getAbsoluteRight() {
        return getAbsolutePosition(helper, index, false);
    }

    @Override
    public void onPreviewNativeEvent(Event.NativePreviewEvent event) {
        NativeEvent nativeEvent = event.getNativeEvent();
        stopPropagation(nativeEvent);
        if (nativeEvent.getType().equals(MOUSEMOVE)) {
            int client = getEventPosition(helper, nativeEvent);
//            Element element = helper.getElement();
//            if (client >= getAbsolutePosition(helper, element, true) // was needed to disable resizing over the container size, which is not desirable sometimes
//                    && client <= getAbsolutePosition(helper, element, false)) {
                resizeHeaders(client);
//            }
        } else if (nativeEvent.getType().equals(MOUSEUP)) {
            previewHandlerReg.removeHandler();
            resizeHandler = null;
            enableSelection();
        }
    }

    private void resizeHeaders(int clientX) {
        int dragX = clientX - initialMouse;
        if (Math.abs(dragX) > 2) {
            double restDelta = helper.resizeChild(index, dragX);
            initialMouse += dragX - Math.round(restDelta);
        }
    }

    private static void disableSelection() {
        GwtClientUtils.addClassName(Document.get().getBody(), "no-select");
    }

    private static void enableSelection() {
        GwtClientUtils.removeClassName(Document.get().getBody(), "no-select");
    }
}
