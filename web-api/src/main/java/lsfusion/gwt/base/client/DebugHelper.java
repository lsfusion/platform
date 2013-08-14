package lsfusion.gwt.base.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.RootPanel;

public class DebugHelper {
    private static AbsolutePanel focusOutline;
    public static void install() {
        //debug events...
        Event.addNativePreviewHandler(new Event.NativePreviewHandler() {
            @Override
            public void onPreviewNativeEvent(Event.NativePreviewEvent event) {
                NativeEvent nativeEvent = event.getNativeEvent();
                if (nativeEvent != null) {
                    String eventType = nativeEvent.getType();
                    EventTarget eventTarget = nativeEvent.getEventTarget();

//                    logEventTarget(eventTarget);
                }
            }

            private native void logEventTarget(EventTarget eventTarget) /*-{
                console.log(eventTarget);
            }-*/;
        });

        //debug focus...
        focusOutline = new AbsolutePanel();
        focusOutline.getElement().getStyle().setPosition(Style.Position.ABSOLUTE);
        focusOutline.getElement().getStyle().setZIndex(100);
        focusOutline.getElement().getStyle().setBorderStyle(Style.BorderStyle.SOLID);
        focusOutline.getElement().getStyle().setBorderColor("red");
        focusOutline.getElement().getStyle().setBorderWidth(2, Style.Unit.PX);
        focusOutline.getElement().getStyle().setProperty("pointerEvents", "none");

        RootPanel.get().getElement().getStyle().setPosition(Style.Position.RELATIVE);
        RootPanel.get().add(focusOutline);

        new Timer() {
            private Element lastActiveElement = null;
            @Override
            public void run() {
//                logCurrentFocusable();
                Element e = getActiveElement();
                int left = 0;
                int top = 0;
                int width = 0;
                int height = 0;
                if (e != null && e != lastActiveElement) {
                    left = e.getAbsoluteLeft() - RootPanel.get().getElement().getAbsoluteLeft() - 1;
                    top = e.getAbsoluteTop() - RootPanel.get().getElement().getAbsoluteTop() - 1;
                    width = e.getOffsetWidth();
                    height = e.getOffsetHeight();
                    focusOutline.getElement().getStyle().setLeft(left, Style.Unit.PX);
                    focusOutline.getElement().getStyle().setTop(top, Style.Unit.PX);
                    focusOutline.getElement().getStyle().setWidth(width, Style.Unit.PX);
                    focusOutline.getElement().getStyle().setHeight(height, Style.Unit.PX);

                    lastActiveElement = e;
                }
            }
        }.scheduleRepeating(50);
    }

    public static Element getActiveElement() {
        return getActiveElement(RootPanel.get().getElement());
    }

    private static native Element getActiveElement(Element rootElement) /*-{
        return rootElement.ownerDocument.activeElement;
    }-*/;

    public static void logCurrentFocusable() {
        logCurrentFocusableImpl(RootPanel.get().getElement());
    }

    private static native void logCurrentFocusableImpl(Element rootElement) /*-{
        console.log(rootElement.ownerDocument.activeElement);
    }-*/;
}
