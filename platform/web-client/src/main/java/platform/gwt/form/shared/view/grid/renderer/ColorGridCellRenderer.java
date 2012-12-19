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
        div.getStyle().setColor(color);
        div.getStyle().setBackgroundColor(color);
    }

    public static void renderColorBox(DivElement cellParent, Object value) {
        String color = getColorValue(value);

        DivElement div = Document.get().createDivElement();
        div.setInnerText(EscapeUtils.UNICODE_NBSP);

        Style divStyle = div.getStyle();
        divStyle.setBorderWidth(0, Style.Unit.PX);
        divStyle.setColor(color);
        divStyle.setBackgroundColor(color);

        cellParent.appendChild(div);
    }

    private static String getColorValue(Object value) {
        return value == null ? "" : value.toString();
    }
}
