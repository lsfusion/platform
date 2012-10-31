package platform.gwt.base.client;

import com.google.gwt.core.client.JavaScriptObject;

public class JSNHelper {

    private JSNHelper() {
    }

    public static native JavaScriptObject createObject() /*-{
        return new Object;
    }-*/;

    public static native void setAttribute(JavaScriptObject elem, String attr, String value) /*-{
        elem[attr] = value;
    }-*/;

    public static native String getAttributeAsString(JavaScriptObject elem, String attr) /*-{
        var ret = elem[attr];
        return (ret === undefined || ret == null) ? null : String(ret);
    }-*/;

    public static native void setAttribute(JavaScriptObject elem, String attr, boolean value) /*-{
        elem[attr] = value;
    }-*/;

    public static native boolean getAttributeAsBoolean(JavaScriptObject elem, String attr) /*-{
        var ret = elem[attr];
        return (ret == null || ret === undefined) ? false : ret;
    }-*/;
}
