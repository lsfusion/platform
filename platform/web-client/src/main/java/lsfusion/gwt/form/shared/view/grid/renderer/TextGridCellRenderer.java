package lsfusion.gwt.form.shared.view.grid.renderer;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.form.shared.view.GPropertyDraw;

public class TextGridCellRenderer extends TextBasedGridCellRenderer {
    public TextGridCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    public void renderDom(lsfusion.gwt.cellview.client.cell.Cell.Context context, DivElement cellElement, Object value) {
        Style divStyle = cellElement.getStyle();
        divStyle.setPaddingRight(4, Style.Unit.PX);
        divStyle.setPaddingLeft(4, Style.Unit.PX);

        divStyle.setProperty("lineHeight", "normal");

        divStyle.setProperty("wordWrap", "break-word");
        divStyle.setWhiteSpace(Style.WhiteSpace.PRE_WRAP);

        if (property.font != null) {
            divStyle.setProperty("font", property.font.getFullFont());
        }

        updateElement(cellElement, value);
    }

    @Override
    protected String renderToString(Object value) {
        return (String) value;
    }
}