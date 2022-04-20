package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

public class ColorCellRenderer extends CellRenderer<Object> {

    public ColorCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    public void renderStaticContent(Element element, RenderContext renderContext) {
        element.setInnerText(EscapeUtils.UNICODE_NBSP);
//        element.getStyle().setBorderWidth(0, Style.Unit.PX);
    }

    @Override
    public void clearRenderContent(Element element, RenderContext renderContext) {
//        element.getStyle().clearBackgroundColor();
        element.setTitle(null);
    }

    @Override
    public boolean renderDynamicContent(Element element, Object value, boolean loading, UpdateContext updateContext) {
        String baseColor = getColorValue(value);
        element.setTitle(baseColor);

//        color is set with getBaseBackground
//        element.getStyle().setBackgroundColor(baseColor);

        return false;
    }

    @Override
    protected String getBaseBackground(Object value) {
        return getColorValue(value);
    }

    private String getColorValue(Object value) {
        return value == null ? null : value.toString();
    }

    @Override
    public String format(Object value) {
        return getColorValue(value);
    }

    @Override
    public boolean isAutoDynamicHeight() {
        return false;
    }
}
