package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;

public class GCustom extends GSimpleStateTableView {
    private final String js;

    public GCustom(GFormController form, GGridController grid, String js) {
        super(form, grid);
        this.js = js;
    }

    @Override
    protected void render(Element element, Element recordElement, JsArray<JavaScriptObject> list) {
        if (list.length() > 0 && element != null) {
            runFunction(element, list, js);
        }
    }

    protected native void runFunction(Element element, JavaScriptObject list, String jsFunction)/*-{
        var fn = $wnd[jsFunction];
        if (typeof fn === 'function'){
            fn(element, list);
        }
    }-*/;
}
