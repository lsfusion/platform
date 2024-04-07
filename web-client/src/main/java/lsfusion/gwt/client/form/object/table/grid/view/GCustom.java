package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.classes.data.GJSONType;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.TableContainer;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.CustomCellRenderer;

public class GCustom extends GTippySimpleStateTableView {
    private final JavaScriptObject renderFunction;
    private final boolean renderFunctionWithoutArguments; //backward compatibility

    public GCustom(GFormController form, GGridController grid, TableContainer tableContainer, String renderFunction) {
        super(form, grid, tableContainer);
        this.renderFunction = GwtClientUtils.getGlobalField(renderFunction);
        this.renderFunctionWithoutArguments = !GwtClientUtils.isFunctionContainsArguments(this.renderFunction);
    }

    private JavaScriptObject customOptions;
    @Override
    public void updateCustomOptionsValues(NativeHashMap<GGroupObjectValue, PValue> values) {
        customOptions = GSimpleStateTableView.convertToJSValue(GJSONType.instance, null, true, values.firstValue()); // for now we're assuming that custom options is json
        dataUpdated = true;
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
        if (renderFunctionWithoutArguments)
            clear(renderFunction, getDrawElement());
    }

    @Override
    protected void onUpdate(Element element, JsArray<JavaScriptObject> list) {
        FocusUtils.startFocusTransaction();

        if (renderFunctionWithoutArguments)
            update(renderFunction, element, controller, list, customOptions);
        else
            runFunction(element, list, renderFunction, controller);

        FocusUtils.endFocusTransaction();
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

    protected native void clear(JavaScriptObject renderFunction, Element element)/*-{
        if (renderFunction().clear !== undefined)
            renderFunction().clear(element);
    }-*/;

}
