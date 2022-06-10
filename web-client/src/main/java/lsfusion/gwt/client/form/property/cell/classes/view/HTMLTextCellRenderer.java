package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.CustomCellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

public class HTMLTextCellRenderer extends StringBasedCellRenderer {

    public HTMLTextCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    public boolean renderDynamicContent(Element element, Object value, boolean loading, UpdateContext updateContext) {
        setControllerField(element, CustomCellRenderer.getController(property, updateContext, element));
        return super.renderDynamicContent(element, value, loading, updateContext);
    }

    @Override
    public void clearRenderContent(Element element, RenderContext renderContext) {
        removeControllerField(element);
        super.clearRenderContent(element, renderContext);
    }

    private static native void setControllerField(Element element, JavaScriptObject controller)/*-{
        element.controller = controller;
    }-*/;

    private static native void removeControllerField(Element element)/*-{
        if (element.controller)
            delete element.controller;
    }-*/;

    @Override
    protected void setInnerContent(Element element, String innerText) {
        element.setInnerHTML(innerText);
    }

    @Override
    public String format(Object value) {
        return (String) value;
    }
}
