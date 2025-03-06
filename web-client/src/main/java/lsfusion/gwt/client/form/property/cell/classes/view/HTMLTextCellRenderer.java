package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.CustomCellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

public class HTMLTextCellRenderer extends HTMLBasedCellRenderer {

    public HTMLTextCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    public boolean renderContent(Element element, RenderContext renderContext) {
        return true;
    }

    @Override
    public boolean updateContent(Element element, PValue value, Object extraValue, UpdateContext updateContext) {
        GwtClientUtils.setField(element, "controller", CustomCellRenderer.getController(property, updateContext, element));
        return super.updateContent(element, value, extraValue, updateContext);
    }

    @Override
    public boolean clearRenderContent(Element element, RenderContext renderContext) {
        GwtClientUtils.removeField(element, "controller");
        return super.clearRenderContent(element, renderContext);
    }
}
