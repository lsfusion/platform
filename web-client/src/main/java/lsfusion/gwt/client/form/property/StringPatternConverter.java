package lsfusion.gwt.client.form.property;

import com.google.gwt.core.client.JavaScriptObject;
import lsfusion.gwt.client.base.GwtClientUtils;

public class StringPatternConverter {
    public static JavaScriptObject convert(String pattern) {
        if(pattern == null)
            return null;

        JavaScriptObject options = GwtClientUtils.newObject();
        GwtClientUtils.setAttribute(options, "mask", pattern);
        return options;
    }
}
