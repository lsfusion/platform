package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.CustomCellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

public class HTMLTextCellRenderer extends CellRenderer {

    public HTMLTextCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    public boolean renderContent(Element element, RenderContext renderContext) {
        return false;
    }

    @Override
    public boolean updateContent(Element element, PValue value, Object extraValue, UpdateContext updateContext) {
        String renderValue = getHTMLValue(value);
        GwtClientUtils.setField(element, "controller", CustomCellRenderer.getController(property, updateContext, element));
        element.setInnerHTML(renderValue);
        return true;
    }

    @Override
    public boolean clearRenderContent(Element element, RenderContext renderContext) {
        GwtClientUtils.removeField(element, "controller");

        return false;
    }

    @Override
    public String format(PValue value) {
        return getHTMLValue(value);
    }

    private String getHTMLValue(PValue value) {
        return PValue.getStringValue(value);
    }
}
