package platform.gwt.form.shared.view.grid.renderer;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style;
import platform.gwt.base.client.EscapeUtils;
import platform.gwt.cellview.client.cell.Cell;

public class ColorGridCellRenderer extends AbstractGridCellRenderer {

    @Override
    public void renderDom(Cell.Context context, DivElement cellElement, Object value) {
        renderColorBox(cellElement, value);
    }

    @Override
    public void updateDom(DivElement cellElement, Cell.Context context, Object value) {
        String color = getColorValue(value);

        DivElement div = cellElement.getFirstChild().cast();
        updateElement(div, color);
    }

    public static void renderColorBox(DivElement cellParent, Object value) {
        String color = getColorValue(value);

        DivElement div = Document.get().createDivElement();
        div.setInnerText(EscapeUtils.UNICODE_NBSP);

        div.getStyle().setBorderWidth(0, Style.Unit.PX);

        updateElement(div, color);

        cellParent.appendChild(div);
    }

    private static void updateElement(DivElement div, String colorValue) {
        div.getStyle().setColor(colorValue);
        div.getStyle().setBackgroundColor(colorValue);
        div.setTitle(colorValue);
    }

    private static String getColorValue(Object value) {
        return value == null ? "" : value.toString();
    }
}
