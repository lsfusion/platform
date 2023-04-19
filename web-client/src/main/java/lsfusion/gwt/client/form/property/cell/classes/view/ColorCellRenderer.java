package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

public class ColorCellRenderer extends CellRenderer {

    public ColorCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    public boolean renderContent(Element element, RenderContext renderContext) {
        element.setInnerText(EscapeUtils.UNICODE_NBSP);
//        element.getStyle().setBorderWidth(0, Style.Unit.PX);

        return false;
    }

    @Override
    public boolean clearRenderContent(Element element, RenderContext renderContext) {
//        element.getStyle().clearBackgroundColor();
        element.setTitle(null);

        return false;
    }

    @Override
    public boolean updateContent(Element element, PValue value, Object extraValue, UpdateContext updateContext) {
        String baseColor = getColorValue(value);
        element.setTitle(baseColor);

//        color is set with getBaseBackground
//        element.getStyle().setBackgroundColor(baseColor);

        return false;
    }

    @Override
    protected String getBackground(UpdateContext updateContext) {
        String colorValue = getColorValue(updateContext.getValue());
        if (colorValue != null)
            return colorValue;

        return super.getBackground(updateContext);
    }

    private String getColorValue(PValue value) {
        return PValue.getColorStringValue(value);
    }

    @Override
    public String format(PValue value) {
        return getColorValue(value);
    }
}
