package lsfusion.gwt.client.base.jsni;

import com.google.gwt.core.client.JavaScriptObject;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.object.GGroupObjectValue;

// what the page gives the client and a GWT test, which runs without the page, has to give it itself - and what the
// client's GWT tests read and call its JS objects with
public final class JsniTestSupport {
    private JsniTestSupport() {
    }

    // the native maps remove a key through the window's jsMapDelete, which the page's static/js/utils.js defines
    public static native void installMapDelete() /*-{
        $wnd.jsMapDelete = function(map, key) { return map["delete"](key); };
    }-*/;

    // the page's setup, which the server writes into the page (main.jsp): none of its arguments here, so the client
    // takes its defaults - the images of the platform's buttons and the links to files are addressed from the page's
    // own URL
    public static native void installPageSetup() /*-{
        if (!$wnd.pageSetup)
            $wnd.pageSetup = {};
    }-*/;

    public static native JavaScriptObject field(JavaScriptObject object, String name) /*-{ return object[name]; }-*/;
    public static native boolean own(JavaScriptObject object, String name) /*-{ return Object.prototype.hasOwnProperty.call(object, name); }-*/;
    // compiled JSNI runs in the module's window; $wnd is the page's window
    public static native JavaScriptObject moduleWindow() /*-{ return window; }-*/;
    public static native JavaScriptObject pageWindow() /*-{ return $wnd; }-*/;
    // a batch begun through a controller, run being what its function does - the Promise it returns
    public static native JavaScriptObject batch(JavaScriptObject controller, Runnable run) /*-{
        return controller.batch(function () { run.@java.lang.Runnable::run()(); });
    }-*/;
    // what a Promise is resolved with, or the message it is rejected with - ' (cancelled)' appended where its Error says so
    public static native <T> void then(JavaScriptObject promise, java.util.function.Consumer<T> resolved, java.util.function.Consumer<String> rejected) /*-{
        promise.then(function (value) { resolved.@java.util.function.Consumer::accept(*)(value); },
                     function (error) { rejected.@java.util.function.Consumer::accept(*)(error.message + (error.cancelled ? ' (cancelled)' : '')); });
    }-*/;
    public static native JavaScriptObject parse(String json) /*-{ return JSON.parse(json); }-*/;
    // a call of a controller's method - a member's, the form's -, given what the test gives
    public static native Object call(JavaScriptObject object, String method, Object... args) /*-{
        return object[method].apply(object, args);
    }-*/;
    // the objects handle a view has of a row it does not draw - a lookup's (getValues)
    public static JavaScriptObject objects(GGroupObjectValue key) {
        JavaScriptObject handle = GwtClientUtils.newObject();
        GGroupObjectValue.registerRow(handle, key);
        return field(handle, "objects");
    }
}
