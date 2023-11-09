package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.RendererType;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

public abstract class HTMLBasedCellRenderer extends CellRenderer {

    public HTMLBasedCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    public boolean renderContent(Element element, RenderContext renderContext) {
        return false;
    }

    @Override
    public boolean clearRenderContent(Element element, RenderContext renderContext) {
        return false;
    }

    @Override
    public boolean updateContent(Element element, PValue value, Object extraValue, UpdateContext updateContext) {
        GwtClientUtils.setDataHtmlOrText(element, getContentHTMLValue(value), true);
        return true;
    }

    public String format(PValue value, RendererType rendererType) {
        return getHTMLValue(value);
    }

    protected String getContentHTMLValue(PValue value) {
        return getHTMLValue(value);
    }

    protected String getHTMLValue(PValue value) {
        return PValue.getStringValue(value);
    }
}
