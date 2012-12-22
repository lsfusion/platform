package platform.gwt.codemirror.client;

import com.google.gwt.core.client.JavaScriptObject;
import platform.gwt.base.client.jsni.JSNIHelper;

public class CodeMirrorOptions {
    protected JavaScriptObject jsnObj = JSNIHelper.createObject();

    public CodeMirrorOptions(String value) {
        setValue(value);
        //defaults
        setAttribute("tabMode", "shift");
        setAttribute("lineNumbers", true);
        setAttribute("matchBrackets", true);
    }

    public JavaScriptObject getJSNObj() {
        return jsnObj;
    }

    public void setValue(String value) {
        setAttribute("value", value);
    }

    public String getValue() {
        return getAttributeAsString("value");
    }

    public void setAttribute(String property, String value) {
        JSNIHelper.setAttribute(jsnObj, property, value);
    }

    public String getAttributeAsString(String property) {
        return JSNIHelper.getAttributeAsString(jsnObj, property);
    }

    public boolean getAttributeAsBoolean(String property) {
        return JSNIHelper.getAttributeAsBoolean(jsnObj, property);
    }

    public void setAttribute(String property, boolean value) {
        JSNIHelper.setAttribute(jsnObj, property, value);
    }

    public static CodeMirrorOptions getDefaultOptions() {
        return new CodeMirrorOptions("");
    }
}
