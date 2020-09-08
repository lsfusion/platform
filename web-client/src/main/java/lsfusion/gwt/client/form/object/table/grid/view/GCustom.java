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
        var controller = {
            changeObjectProperty: function (property, object, newValue) {
                return thisObj.@GCustom::changeObjectProperty(*)(property, object, newValue);
            },
            changeProperty: function (property, object, year, month, day, hour, minute, second) {
                return thisObj.@GCustom::changeDateTimeProperty(*)(property, object, year, month, day, hour, minute, second);
            }
        };

        var fn = $wnd[renderFunction];
        fn(element, list, controller);
    }-*/;

    private void changeObjectProperty(String property, JavaScriptObject object, Serializable newValue) {
        changeProperty(property, newValue, fromObject(getKey(object)));
    }

    private void changeDateTimeProperty(String property, JavaScriptObject object, int year, int month, int day, int hour, int minute, int second) {
        changeObjectProperty(property, object, new GDateTimeDTO(year, month, day, hour, minute, second));
    }
}
