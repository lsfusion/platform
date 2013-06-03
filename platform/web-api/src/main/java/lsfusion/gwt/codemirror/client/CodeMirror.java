package lsfusion.gwt.codemirror.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Widget;

public class CodeMirror extends Widget {
    private static int nextId = 0;

    private final CodeMirrorOptions options;

    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
    private JavaScriptObject editor;
    private final String id;

    public CodeMirror() {
        this(CodeMirrorOptions.getDefaultOptions());
    }

    public CodeMirror(CodeMirrorOptions options) {
        this.options = options;

        id = "codemirror-editor-" + (++nextId);

        Element holdingTextArea = DOM.createTextArea();
        holdingTextArea.setPropertyString("id", id);

        setElement(holdingTextArea);
    }

    public void onLoad() {
        editor = initialize(id, options);
    }

    private native JavaScriptObject initialize(String id, CodeMirrorOptions options) /*-{
        var opts = options.@lsfusion.gwt.codemirror.client.CodeMirrorOptions::getJSNObj()();

        $doc.getElementById(id).value = opts.value;

        return $wnd.CodeMirror.fromTextArea($doc.getElementById(id), opts);
    }-*/;

    public String getValue() {
        if (editor == null) {
            return options.getValue();
        } else {
            return getValueJS();
        }
    }

    public native String getValueJS() /*-{
        var editor = this.@lsfusion.gwt.codemirror.client.CodeMirror::editor;
        return editor.getValue();
    }-*/;

    public void setValue(String value) {
        if (editor == null) {
            options.setValue(value);
        } else {
            setValueJS(value);
        }
    }

    private native void setValueJS(String value) /*-{
        var editor = this.@lsfusion.gwt.codemirror.client.CodeMirror::editor;
        editor.setValue(value);
        editor.refresh();
    }-*/;
}
