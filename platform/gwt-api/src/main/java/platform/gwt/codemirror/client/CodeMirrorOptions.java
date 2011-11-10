package platform.gwt.codemirror.client;

import com.smartgwt.client.core.DataClass;

public class CodeMirrorOptions extends DataClass {
    public CodeMirrorOptions(String value) {
        setValue(value);
        //defaults
        setAttribute("tabMode", "shift");
        setAttribute("lineNumbers", true);
        setAttribute("matchBrackets", true);
    }

    public void setValue(String value) {
        setAttribute("value", value);
    }

    public String getValue() {
        return getAttributeAsString("value");
    }

    public static CodeMirrorOptions getDefaultOptions() {
        return new CodeMirrorOptions("");
    }
}
