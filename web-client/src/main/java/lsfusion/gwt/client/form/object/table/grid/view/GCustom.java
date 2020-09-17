package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.property.cell.classes.GDateTimeDTO;

import java.io.Serializable;

public class GCustom extends GSimpleStateTableView {
    private final String renderFunction;

    public GCustom(GFormController form, GGridController grid, String renderFunction) {
        super(form, grid);
        this.renderFunction = renderFunction;
    }

    @Override
    protected void render(Element element, Element recordElement, JsArray<JavaScriptObject> list) {
        runFunction(element, list, renderFunction);
    }

    protected native void runFunction(Element element, JavaScriptObject list, String renderFunction)/*-{
        var thisObj = this;
        var controller = thisObj.@GCustom::getController()();

        var fn = $wnd[renderFunction];
        fn(element, list, controller);
    }-*/;
}
