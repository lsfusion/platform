package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;

public class GCustom extends GTippySimpleStateTableView {
    private final JavaScriptObject renderFunction;
    private final boolean renderFunctionWithoutArguments; //backward compatibility

    public GCustom(GFormController form, GGridController grid, String renderFunction) {
        super(form, grid);
        this.renderFunction = GwtClientUtils.getGlobalField(renderFunction);
        this.renderFunctionWithoutArguments = !GwtClientUtils.isFunctionContainsArguments(this.renderFunction);
    }

    @Override
    public void onRender() {
        if (renderFunctionWithoutArguments)
            render(renderFunction, getDrawElement(), controller);
    }

    @Override
    public void onClear() {
        if (renderFunctionWithoutArguments)
            clear(renderFunction, getDrawElement());
    }

    private boolean filterSet = false;
    @Override
    protected void onUpdate(Element element, JsArray<JavaScriptObject> list) {
        if (filterSet) {
            if (renderFunctionWithoutArguments)
                update(renderFunction, element, controller, list);
            else
                runFunction(element, list, renderFunction, controller);
        } else {
            setNotNullViewFilter("selected", 1000);
            filterSet = true;
        }
    }

    @Override
    protected Element getCellParent(Element target) {
        return null;
    }

    protected native void runFunction(Element element, JavaScriptObject list, JavaScriptObject renderFunction, JavaScriptObject controller)/*-{
        renderFunction(element, list, controller);
    }-*/;

    protected native void render(JavaScriptObject renderFunction, Element element, JavaScriptObject controller)/*-{
        renderFunction().render(element, controller);
    }-*/;

    protected native void update(JavaScriptObject renderFunction, Element element, JavaScriptObject controller, JsArray<JavaScriptObject> list)/*-{
        renderFunction().update(element, controller, list);
    }-*/;

    protected native void clear(JavaScriptObject renderFunction, Element element)/*-{
        if (renderFunction().clear !== undefined)
            renderFunction().clear(element);
    }-*/;

}
