package lsfusion.gwt.client.base.jsni;

// what the page gives the client and a GWT test, which runs without the page, has to give it itself
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
}
