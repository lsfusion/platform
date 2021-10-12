package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.property.cell.classes.controller.CustomReplaceCellEditor;

public class GCustom extends GTippySimpleStateTableView {
    private final String renderFunction;
    private JavaScriptObject customRenderer = null;

    public GCustom(GFormController form, GGridController grid, String renderFunction) {
        super(form, grid);
        this.renderFunction = renderFunction;
        if (!GwtClientUtils.isFunctionContainsArguments(GwtClientUtils.getGlobalField(renderFunction)))
            customRenderer = CustomReplaceCellEditor.getCustomFunction(renderFunction);
    }

    @Override
    public void onRender() {
        if (customRenderer != null)
            render(customRenderer, getDrawElement(), controller);
    }

    @Override
    public void onClear() {
        if (customRenderer != null)
            clear(customRenderer, getDrawElement());
    }

    @Override
    protected void onUpdate(Element element, JsArray<JavaScriptObject> list) {
        if (customRenderer != null)
            update(customRenderer, element, controller, list);
        else
            runFunction(element, list, renderFunction, controller);
    }

    @Override
    protected Element getCellParent(Element target) {
        return null;
    }

    protected native void runFunction(Element element, JavaScriptObject list, String renderFunction, JavaScriptObject controller)/*-{
        $wnd[renderFunction](element, list, controller);
    }-*/;

    protected native void render(JavaScriptObject customRenderer, Element element, JavaScriptObject controller)/*-{
        customRenderer.render(element, controller);
    }-*/;

    protected native void update(JavaScriptObject customRenderer, Element element, JavaScriptObject controller, JsArray<JavaScriptObject> list)/*-{
        customRenderer.update(element, controller, list);
    }-*/;

    protected native void clear(JavaScriptObject customRenderer, Element element)/*-{
        if (customRenderer.clear !== undefined)
            customRenderer.clear(element);
    }-*/;

}
