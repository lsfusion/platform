package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.property.cell.view.AbstractGridCellRenderer;

public class ColorGridCellRenderer extends AbstractGridCellRenderer {

    @Override
    public void renderDom(DataGrid table, DivElement cellElement, Object value) {
        renderDom(cellElement, value);
    }

    @Override
    public void renderDom(Element cellElement, Object value) {
        String color = getColorValue(value);
        cellElement.setInnerText(EscapeUtils.UNICODE_NBSP);
        cellElement.getStyle().setBorderWidth(0, Style.Unit.PX);
        updateElement(cellElement, color);
    }

    @Override
    public void updateDom(DivElement cellElement, DataGrid table, Object value) {
        updateDom(cellElement, value);
    }

    @Override
    public void updateDom(Element cellElement, Object value) {
        String color = getColorValue(value);
        updateElement(cellElement, color);
    }

    private void updateElement(Element div, String colorValue) {
        div.getStyle().setColor(colorValue);
        div.getStyle().setBackgroundColor(colorValue);
        div.setTitle(colorValue);
    }

    private String getColorValue(Object value) {
        return value == null ? "" : value.toString();
    }
}
