package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.property.cell.view.AbstractGridCellRenderer;

public class ColorGridCellRenderer extends AbstractGridCellRenderer {
    @Override
    public void render(Element element, GFont font, Object value, boolean isSingle) {
        renderStatic(element, font, isSingle);
        renderDynamic(element, font, value, isSingle);
    }

    @Override
    public void renderStatic(Element element, GFont font, boolean isSingle) {
        element.setInnerText(EscapeUtils.UNICODE_NBSP);
        element.getStyle().setBorderWidth(0, Style.Unit.PX);
    }

    @Override
    public void renderDynamic(Element element, GFont font, Object value, boolean isSingle) {
        String color = getColorValue(value);
        element.getStyle().setColor(color);
        element.getStyle().setBackgroundColor(color);
        element.setTitle(color);
    }

    private String getColorValue(Object value) {
        return value == null ? "" : value.toString();
    }
}
