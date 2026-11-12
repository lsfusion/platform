package lsfusion.gwt.client.base.jsni;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;

@Deprecated //deprecated since 6.2, removed in 7.0 (merged with GwtClientUtils)
public class JSNIHelper {

    private JSNIHelper() {
    }

    public static native JavaScriptObject createObject() /*-{
        return new Object;
    }-*/;

    public static native void setAttribute(JavaScriptObject elem, String attr, String value) /*-{
        elem[attr] = value;
    }-*/;

    public static native void setAttribute(JavaScriptObject elem, String attr, boolean value) /*-{
        elem[attr] = value;
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
}
