package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.exception.GExceptionManager;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.table.TableContainer;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.property.cell.view.CustomCellRenderer;

public class GCustom extends GTippySimpleStateTableView {
    private final JavaScriptObject renderFunction;
    private final boolean renderFunctionWithoutArguments; //backward compatibility

    public GCustom(GFormController form, GGridController grid, TableContainer tableContainer, String renderFunction) {
        super(form, grid, tableContainer);
        this.renderFunction = GwtClientUtils.getGlobalField(renderFunction);
        this.renderFunctionWithoutArguments = !GwtClientUtils.isFunctionContainsArguments(this.renderFunction);
    }

    @Override
    public void onRender(Event editEvent) {
        Element drawElement = getDrawElement();
        CustomCellRenderer.setCustomElement(drawElement);

        if (renderFunctionWithoutArguments)
            render(renderFunction, drawElement, controller, editEvent);
    }

    @Override
    public void onClear() {
        if (renderFunctionWithoutArguments) {
            // the application's own clear, and a throw from it is reported and goes no further: this runs while the
            // form is being closed as well as when the view is switched, and there application code must not be able
            // to stop the form being torn down and the server told it closed
            try {
                clear(renderFunction, getDrawElement(), controller);
            } catch (Throwable t) {
                GExceptionManager.logClientError(t, null);
            }
        }
    }

    @Override
    protected void onUpdate(Element element, JsArray<JavaScriptObject> list) {
        // the transaction has to be closed even when the application's own render throws: it is what holds back the
        // blur events raised while the view is being redrawn, and one left open pends every later blur for good
        FocusUtils.startFocusTransaction(element);
        try {
            if (renderFunctionWithoutArguments)
                update(renderFunction, element, controller, list, getCustomOptions());
            else
                runFunction(element, list, renderFunction, controller);
        } finally {
            FocusUtils.endFocusTransaction();
        }
    }

    @Override
    protected Element getCellParent(Element target) {
        return null;
    }

    protected native void runFunction(Element element, JavaScriptObject list, JavaScriptObject renderFunction, JavaScriptObject controller)/*-{
        renderFunction(element, list, controller);
    }-*/;

    protected native void render(JavaScriptObject renderFunction, Element element, JavaScriptObject controller, Event event)/*-{
        renderFunction().render(element, controller, event);
    }-*/;

    protected native void update(JavaScriptObject renderFunction, Element element, JavaScriptObject controller, JsArray<JavaScriptObject> list, JavaScriptObject customOptions)/*-{
        renderFunction().update(element, controller, list, customOptions);
    }-*/;

    protected native void clear(JavaScriptObject renderFunction, Element element, JavaScriptObject controller)/*-{
        if (renderFunction().clear !== undefined)
            renderFunction().clear(element, controller);
    }-*/;

}
