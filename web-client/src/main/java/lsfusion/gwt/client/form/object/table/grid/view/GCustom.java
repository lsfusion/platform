package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;

public class GCustom extends GTippySimpleStateTableView {
    private final String renderFunction;

    public GCustom(GFormController form, GGridController grid, String renderFunction) {
        super(form, grid);
        this.renderFunction = renderFunction;
    }

    @Override
    protected void render(Element element, JsArray<JavaScriptObject> list) {
        runFunction(element, list, renderFunction, controller);
    }

    @Override
    protected Element getCellParent(Element target) {
        return null;
    }

    protected native void runFunction(Element element, JavaScriptObject list, String renderFunction, JavaScriptObject controller)/*-{
        $wnd[renderFunction](element, list, controller);
    }-*/;
}
