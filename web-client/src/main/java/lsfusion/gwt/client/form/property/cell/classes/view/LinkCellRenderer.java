package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

public class LinkCellRenderer extends StringBasedCellRenderer<String> {

    public LinkCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    public boolean renderDynamicContent(Element element, Object value, boolean loading, UpdateContext updateContext) {
        return super.renderDynamicContent(element, value, loading, updateContext);
    }

    @Override
    public void clearRenderContent(Element element, RenderContext renderContext) {
        super.clearRenderContent(element, renderContext);
    }

    @Override
    protected void setInnerContent(Element element, String innerText) {
        element.setInnerHTML("<a href=" + innerText + ">" + innerText + "</a>");
    }

    @Override
    public String format(String value) {
        return value;
    }
}
