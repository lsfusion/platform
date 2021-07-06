package lsfusion.gwt.client.base.resize;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Result;
import lsfusion.gwt.client.base.view.FlexPanel;

import java.util.function.Supplier;

import static com.google.gwt.dom.client.BrowserEvents.*;
import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;

public class ResizeHandler implements Event.NativePreviewHandler {
    public static final int ANCHOR_WIDTH = 10;
    public static ResizeHandler resizeHandler = null;

    private final ResizeHelper helper;

    private HandlerRegistration previewHandlerReg;

    private int initalMouse;
    private final int index;

    public ResizeHandler(ResizeHelper helper, int index) {
        this.helper = helper;
        this.index = index;

        previewHandlerReg = Event.addNativePreviewHandler(this);
        this.initalMouse = getAbsoluteRight();
    }

    // it's important that checkResize event should be called for cursorElement element (otherwise, cursor will be incorrect)
    public static <C> void checkResizeEvent(ResizeHelper helper, Element cursorElement, Supplier<Integer> childIndexSupplier, NativeEvent event) {
        String eventType = event.getType();
        if ((MOUSEMOVE.equals(eventType) || MOUSEDOWN.equals(eventType)) && resizeHandler == null) {
            ResizedChild resizedChild = getResizedChild(helper, event, childIndexSupplier);
            Style cursorStyle = cursorElement.getStyle();
            if (resizedChild != null && resizedChild.mainBorder && helper.isChildResizable(resizedChild.index)) {
                cursorStyle.setProperty("cursor", (helper.isVertical() ? Style.Cursor.ROW_RESIZE : Style.Cursor.COL_RESIZE).getCssName());
//                    GwtClientUtils.setProperty(cursorStyle, "cursor", (helper.isVertical() ? Style.Cursor.ROW_RESIZE : Style.Cursor.COL_RESIZE).getCssName(), "important");
//                    cursorStyle.setProperty("cursor", (helper.isVertical() ? Style.Cursor.ROW_RESIZE : Style.Cursor.COL_RESIZE).getCssName() + " !important");
                if (eventType.equals(MOUSEDOWN)) {
                    resizeHandler = new ResizeHandler(helper, resizedChild.index);
                    stopPropagation(event);
                }
            } else {
                cursorStyle.clearProperty("cursor");
                // this container can be not resizable, but the inner one can be resizable, however it will not get a mouse move event if the cursor is to the right
                // so we push this event down to that container
                // in theory we should check that event is trigger to the right of this child widget, but since this child widget can have paddings / margins, we'll just do some extra work
                if(resizedChild != null && resizedChild.outsideBorder) {
                    Widget childWidget = helper.getChildWidget(resizedChild.index);
                    if (childWidget instanceof FlexPanel)
                        ((FlexPanel)childWidget).checkResizeEvent(event, cursorElement);
                }
            }
        }
    }

    public static int getAbsolutePosition(boolean vertical, Element element, boolean left) {
        return vertical ? (left ? element.getAbsoluteTop() : element.getAbsoluteBottom()) : (left ? element.getAbsoluteLeft() : element.getAbsoluteRight());
    }
    public static int getAbsolutePosition(ResizeHelper helper, Element element, boolean left) {
        return getAbsolutePosition(helper.isVertical(), element, left);
    }
    public static int getEventPosition(boolean vertical, NativeEvent event) {
        return vertical ? event.getClientY() : event.getClientX();
    }
    public static int getEventPosition(ResizeHelper helper, NativeEvent event) {
        return getEventPosition(helper.isVertical(), event);
    }

    private static int getAbsolutePosition(ResizeHelper helper, int index, boolean left) {
        return getAbsolutePosition(helper, helper.getChildElement(index), left);
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

    private static ResizedChild getResizedChild(ResizeHelper helper, NativeEvent event, Supplier<Integer> childIndexSupplier) {
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
                return new ResizedChild(childIndex, true, false); // outside border doesn't matter for that branch
            }
        } else {
            for(int i=0,size=helper.getChildCount();i<size;i++) {
                if(helper.isChildVisible(i)) {
                    int right = getAbsolutePosition(helper, i, false);
                    boolean mainBorder = Math.abs(mouse - right) < ANCHOR_WIDTH;
                    if (mainBorder || right > mouse) {
                        int oppositeRight = getAbsolutePosition(!helper.isVertical(), helper.getChildElement(i), false);
                        int oppositeMouse = getEventPosition(!helper.isVertical(), event);
                        return new ResizedChild(i, mainBorder, (oppositeRight >= oppositeMouse && oppositeMouse - oppositeRight < ANCHOR_WIDTH) || right >= mouse);
                    }
                }
            }
        }
        return null;
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
        }
    }

    private void resizeHeaders(int clientX) {
        int dragX = clientX - initalMouse;
        if (Math.abs(dragX) > 2) {
            helper.resizeChild(index, dragX);
            initalMouse = Math.max(clientX, getAbsoluteRight()); // делается max, чтобы при resize'е влево растягивание шло с момента когда курсор вернется на правый край колонки (вправо там другие проблемы)
        }
    }
}
