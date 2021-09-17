package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.WindowValueCellEditor;

public class CustomWindowCellEditor extends WindowValueCellEditor implements CustomCellEditor {

    private final GPropertyDraw property;

    private final String renderFunction;
    private final JavaScriptObject customEditor;

    @Override
    public String getRenderFunction() {
        return renderFunction;
    }

    @Override
    public JavaScriptObject getCustomEditor() {
        return customEditor;
    }

    public CustomWindowCellEditor(EditManager editManager, GPropertyDraw property, String renderFunction, JavaScriptObject customEditor) {
        super(editManager);

        this.property = property;

        this.renderFunction = renderFunction;
        this.customEditor = customEditor;
    }

    @Override
    public void start(Event editEvent, Element parent, Object oldValue) {
        render(parent, null, null, oldValue);
    }

    @Override
    public void stop(Element parent) {
        clearRender(parent, null);
    }

    @Override
    public void onBrowserEvent(Element parent, EventHandler handler) {
        super.onBrowserEvent(parent, handler);

        CustomCellEditor.super.onBrowserEvent(parent, handler);
    }
}
