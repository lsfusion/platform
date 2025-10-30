package lsfusion.gwt.client.base.jsni;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;

public class JSNIHelper {

    private JSNIHelper() {
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

    public static native boolean hasAttribute(Element elem, String name) /*-{
        if (elem == null) return false;

        var ret = elem.getAttribute(name);
        if (!(ret === undefined || ret == null))
            return true;

        var children = elem.children;
        for (var i = 0; i < children.length; i++) {
            if (@JSNIHelper::hasAttribute(Lcom/google/gwt/dom/client/Element;Ljava/lang/String;)(children[i], name)) {
                return true;
            }
        }

        return false;
    }-*/;

    public static void consoleLog(String message) {
        consoleLog("JSNIHelper", message);
    }

    public static native void consoleLog(String category, String message) /*-{
        console.log(category + ":" + message);
    }-*/;

    public static native void consoleLogJavaScriptObject(JavaScriptObject object) /*-{
        console.log(object);
    }-*/;
}
