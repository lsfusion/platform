package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

public class HTMLLinkCellRenderer extends CellRenderer<Object> {
    public HTMLLinkCellRenderer(GPropertyDraw property) {
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
    public boolean updateContent(Element element, Object value, boolean loading, UpdateContext updateContext) {
        element.setInnerHTML("<iframe src=\"" + value + "\" style=\"width:100%; height:100%;\" >Unfortunately this content could not be displayed</iframe>");

        return true;
    }

    public String format(Object value) {
        return value == null ? "" : value.toString();
    }
}