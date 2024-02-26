package lsfusion.gwt.client.form.property;

import com.google.gwt.core.client.JavaScriptObject;
import lsfusion.gwt.client.base.jsni.JSNIHelper;

public class StringPatternConverter {
    public static JavaScriptObject convert(String pattern) {
        if(pattern == null)
            return null;

        JavaScriptObject options = JSNIHelper.createObject();
        JSNIHelper.setAttribute(options, "mask", pattern);
        return options;
    }
}
