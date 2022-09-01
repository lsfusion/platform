package lsfusion.client.form.design.view;

import lsfusion.client.form.design.view.widget.Widget;

import java.awt.*;
import java.awt.event.MouseEvent;

import static java.awt.event.MouseEvent.MOUSE_RELEASED;

public class ResizeHandler {
    public static final int ANCHOR_WIDTH = 10;
    public static ResizeHandler resizeHandler = null;


    //private HandlerRegistration previewHandlerReg;

    private static ResizeHelper helper;
    private static int initialMouse;
    private static int index;

    public ResizeHandler(ResizeHelper helper, int index) {
        this.helper = helper;
        this.index = index;
        /*
        previewHandlerReg = Event.addNativePreviewHandler(this);
        this.initalMouse = getAbsoluteRight();*/

        this.initialMouse = getAbsoluteRight();
    }

    // it's important that checkResize event should be called for cursorElement element (otherwise, cursor will be incorrect)
    public static <C> void checkResizeEvent(ResizeHelper helper, Component cursorElement, /*Supplier<Integer> childIndexSupplier, */MouseEvent event) {
        int eventType = event.getID();
        if ((eventType == MouseEvent.MOUSE_MOVED || eventType == MouseEvent.MOUSE_PRESSED || eventType == MouseEvent.MOUSE_ENTERED) && resizeHandler == null) {
            ResizedChild resizedChild = getResizedChild(helper, event/*, childIndexSupplier*/);
            //Style cursorStyle = cursorElement.getStyle();
            if (resizedChild != null && resizedChild.mainBorder && helper.isChildResizable(resizedChild.index)) {
                cursorElement.setCursor(Cursor.getPredefinedCursor(helper.isVertical() ? Cursor.N_RESIZE_CURSOR : Cursor.E_RESIZE_CURSOR));

                if (eventType == MouseEvent.MOUSE_PRESSED) {
                    resizeHandler = new ResizeHandler(helper, resizedChild.index);

                    // if we're not propagating native event, then BLUR event is also canceled and for example editing is not finished
                    // in all other cases it doesn't matter, since there is no stop propagation for MOUSEDOWN event, and it is the only known problem with preventing default (except expand tree but it seems that in grid everything works fine anyway)
                    event.consume();
                    //stopPropagation(event, true, false);
                }
            } else {
                cursorElement.setCursor(null);

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

        // since not all components are widgets and have mouseListeners (and unlike in web-client )
        if((eventType == MouseEvent.MOUSE_EXITED || eventType == MOUSE_RELEASED) && resizeHandler == null)
            cursorElement.setCursor(null);

        //todo: from onPreviewNativeEvent
        if (resizeHandler != null) {
            //NativeEvent nativeEvent = event.getNativeEvent();
            //stopPropagation(nativeEvent);
            if (eventType == MouseEvent.MOUSE_DRAGGED) {
                int client = getEventPosition(ResizeHandler.helper, event);

                resizeHeaders(client);
            } else if (eventType == MOUSE_RELEASED) {
                //previewHandlerReg.removeHandler();
                resizeHandler = null;
                cursorElement.setCursor(null); // need this for the same reason that we need MOUSE_EXITED
            }
        }
    }

    public static int getAbsolutePosition(boolean vertical, Component element, boolean left) {
        Point locationOnScreen = element.getLocationOnScreen();
        return vertical ? (left ? locationOnScreen.y : locationOnScreen.y + element.getHeight()) : (left ? locationOnScreen.x : locationOnScreen.x + element.getWidth());
        //return vertical ? (left ? element.getAbsoluteTop() : element.getAbsoluteBottom()) : (left ? element.getAbsoluteLeft() : element.getAbsoluteRight());
    }
    public static int getAbsolutePosition(ResizeHelper helper, Component element, boolean left) {
        return getAbsolutePosition(helper.isVertical(), element, left);
    }
    public static int getEventPosition(boolean vertical, MouseEvent event) {
        return vertical ? event.getYOnScreen() : event.getXOnScreen();
    }
    public static int getEventPosition(ResizeHelper helper, MouseEvent event) {
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

    private static ResizedChild getResizedChild(ResizeHelper helper, MouseEvent event /*, Supplier<Integer> childIndexSupplier*/) {
        int mouse = getEventPosition(helper, event);

        for(int i = 0, size = helper.getChildCount(); i<size; i++) {
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
        return null;
    }

    private static int getAbsoluteRight() {
        return getAbsolutePosition(helper, index, false);
    }

//    @Override
//    public void onPreviewNativeEvent(Event.NativePreviewEvent event) {
//        NativeEvent nativeEvent = event.getNativeEvent();
//        stopPropagation(nativeEvent);
//        if (nativeEvent.getType().equals(MOUSEMOVE)) {
//            int client = getEventPosition(helper, nativeEvent);
////            Element element = helper.getElement();
////            if (client >= getAbsolutePosition(helper, element, true) // was needed to disable resizing over the container size, which is not desirable sometimes
////                    && client <= getAbsolutePosition(helper, element, false)) {
//                resizeHeaders(client);
////            }
//        } else if (nativeEvent.getType().equals(MOUSEUP)) {
//            previewHandlerReg.removeHandler();
//            resizeHandler = null;
//        }
//    }

    private static void resizeHeaders(int clientX) {
        int dragX = clientX - initialMouse;
        if (Math.abs(dragX) > 2) {
            double restDelta = helper.resizeChild(index, dragX);
            initialMouse += dragX - Math.round(restDelta);
        }
    }
}
