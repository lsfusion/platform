package lsfusion.gwt.base.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.RootPanel;

public class DebugHelper {
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
        new Timer() {
            @Override
            public void run() {
                logCurrentFocusable(RootPanel.get().getElement());
            }

            private native void logCurrentFocusable(Element element) /*-{
                console.log(element.ownerDocument.activeElement);
            }-*/;
        }.scheduleRepeating(2000);
    }
}
