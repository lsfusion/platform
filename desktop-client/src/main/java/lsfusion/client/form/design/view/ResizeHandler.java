package lsfusion.client.form.design.view;

import lsfusion.client.form.filter.user.view.FilterView;
import lsfusion.client.form.object.table.view.ToolbarView;

import java.awt.*;
import java.awt.event.MouseEvent;

public class ResizeHandler {
    public static final int ANCHOR_WIDTH = 10;
    public static ResizeHandler resizeHandler = null;

    private final ResizeHelper helper;

    //private HandlerRegistration previewHandlerReg;

    private static int initalMouse;
    private static int index;

    public ResizeHandler(ResizeHelper helper, int index) {
        this.helper = helper;
        this.index = index;
        /*
        previewHandlerReg = Event.addNativePreviewHandler(this);
        this.initalMouse = getAbsoluteRight();*/
    }

    // it's important that checkResize event should be called for cursorElement element (otherwise, cursor will be incorrect)
    public static <C> void checkResizeEvent(ResizeHelper helper, FlexPanel cursorElement, /*Supplier<Integer> childIndexSupplier, */MouseEvent event) {
        if(!(cursorElement instanceof FilterView) && !(cursorElement instanceof ToolbarView)) {
            int eventType = event.getID();
            if (eventType == MouseEvent.MOUSE_MOVED || eventType == MouseEvent.MOUSE_PRESSED && resizeHandler == null) {
                ResizedChild resizedChild = getResizedChild(helper, event, cursorElement/*, childIndexSupplier*/);
                //Style cursorStyle = cursorElement.getStyle();
                if (resizedChild != null && resizedChild.mainBorder && helper.isChildResizable(cursorElement, resizedChild.index)) {
                    cursorElement.setCursor(Cursor.getPredefinedCursor(helper.isVertical() ? Cursor.N_RESIZE_CURSOR : Cursor.E_RESIZE_CURSOR));

                    if (eventType == MouseEvent.MOUSE_PRESSED) {
                        resizeHandler = new ResizeHandler(helper, resizedChild.index);

                        // if we're not propagating native event, then BLUR event is also canceled and for example editing is not finished
                        // in all other cases it doesn't matter, since there is no stop propagation for MOUSEDOWN event, and it is the only known problem with preventing default (except expand tree but it seems that in grid everything works fine anyway)
                        event.consume();
                        //stopPropagation(event, true, false);
                    }
                } else {
                    cursorElement.setCursor(Cursor.getDefaultCursor());

                    // this container can be not resizable, but the inner one can be resizable, however it will not get a mouse move event if the cursor is to the right
                    // so we push this event down to that container
                    // in theory we should check that event is trigger to the right of this child widget, but since this child widget can have paddings / margins, we'll just do some extra work
//                if(resizedChild != null && resizedChild.outsideBorder) {
//                    Widget childWidget = helper.getChildWidget(resizedChild.index);
//                    if (childWidget instanceof FlexPanel)
//                        ((FlexPanel)childWidget).checkResizeEvent(event, cursorElement);
//                }
                }
            }

            //todo: from onPreviewNativeEvent
            if (resizeHandler != null) {
                //NativeEvent nativeEvent = event.getNativeEvent();
                //stopPropagation(nativeEvent);
                if (eventType == MouseEvent.MOUSE_DRAGGED) {
                    int client = getEventPosition(helper, event);

                    //resizeHeaders(client);
                    int dragX = event.getX() - initalMouse;
                    if (Math.abs(dragX) > 2) {
                        helper.resizeChild(cursorElement, index, dragX);
                        initalMouse = Math.max(event.getX(), getAbsoluteRight(helper, cursorElement)); // делается max, чтобы при resize'е влево растягивание шло с момента когда курсор вернется на правый край колонки (вправо там другие проблемы)
                    }


                } else if (eventType == MouseEvent.MOUSE_RELEASED) {
                    //previewHandlerReg.removeHandler();
                    resizeHandler = null;
                }
            }
        }
    }

    public static int getAbsolutePosition(boolean vertical, Component element, boolean left) {
        return vertical ? (left ? element.getX() : element.getX() + element.getHeight()) : (left ? element.getY() : element.getY() + element.getWidth());
        //return vertical ? (left ? element.getAbsoluteTop() : element.getAbsoluteBottom()) : (left ? element.getAbsoluteLeft() : element.getAbsoluteRight());
    }
    public static int getAbsolutePosition(ResizeHelper helper, Component element, boolean left) {
        return getAbsolutePosition(helper.isVertical(), element, left);
    }
    public static int getEventPosition(boolean vertical, MouseEvent event) {
        return vertical ? event.getY() : event.getX();
    }
    public static int getEventPosition(ResizeHelper helper, MouseEvent event) {
        return getEventPosition(helper.isVertical(), event);
    }

    private static int getAbsolutePosition(ResizeHelper helper, int index, boolean left, FlexPanel panel) {
        return getAbsolutePosition(helper, helper.getChildElement(panel, index), left);
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

    private static ResizedChild getResizedChild(ResizeHelper helper, MouseEvent event, FlexPanel panel/*, Supplier<Integer> childIndexSupplier*/) {
        int mouse = getEventPosition(helper, event);

        for(int i=0,size=helper.getChildCount(panel);i<size;i++) {
            if(helper.isChildVisible(panel, i)) {
                int right = getAbsolutePosition(helper, i, false, panel);
                boolean mainBorder = Math.abs(mouse - right) < ANCHOR_WIDTH;
                if (mainBorder || right > mouse) {
                    int oppositeRight = getAbsolutePosition(!helper.isVertical(), helper.getChildElement(panel, i), false);
                    int oppositeMouse = getEventPosition(!helper.isVertical(), event);
                    return new ResizedChild(i, mainBorder, (oppositeRight >= oppositeMouse && oppositeMouse - oppositeRight < ANCHOR_WIDTH) || right >= mouse);
                }
            }
        }
        return null;
    }

    private static int getAbsoluteRight(ResizeHelper helper, FlexPanel panel) {
        return getAbsolutePosition(helper, index, false, panel);
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

    private void resizeHeaders(FlexPanel panel, int clientX) {
        int dragX = clientX - initalMouse;
        if (Math.abs(dragX) > 2) {
            helper.resizeChild(panel, index, dragX);
            initalMouse = Math.max(clientX, getAbsoluteRight(helper, panel)); // делается max, чтобы при resize'е влево растягивание шло с момента когда курсор вернется на правый край колонки (вправо там другие проблемы)
        }
    }
}
