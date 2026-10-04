package lsfusion.gwt.client.base.jsni;

// what the page gives the client and a GWT test, which runs without the page, has to give it itself
public final class JsniTestSupport {
    private JsniTestSupport() {
    }

    // the native maps remove a key through the window's jsMapDelete, which the page's static/js/utils.js defines
    public static native void installMapDelete() /*-{
        $wnd.jsMapDelete = function(map, key) { return map["delete"](key); };
    }-*/;
}
