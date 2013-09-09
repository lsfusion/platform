package lsfusion.gwt.form.shared.view.grid.renderer;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.cellview.client.DataGrid;
import lsfusion.gwt.cellview.client.cell.Cell;
import lsfusion.gwt.form.client.form.ui.GGridPropertyTable;
import lsfusion.gwt.form.shared.view.GFont;
import lsfusion.gwt.form.shared.view.GPropertyDraw;

public class TextGridCellRenderer extends TextBasedGridCellRenderer {
    public TextGridCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    public void renderDom(Cell.Context context, DataGrid table, DivElement cellElement, Object value) {
        Style divStyle = cellElement.getStyle();
        divStyle.setPaddingRight(4, Style.Unit.PX);
        divStyle.setPaddingLeft(4, Style.Unit.PX);

        divStyle.setProperty("lineHeight", "normal");

        divStyle.setProperty("wordWrap", "break-word");
        divStyle.setWhiteSpace(Style.WhiteSpace.PRE_WRAP);

        GFont font = property.font;
        if (font == null && table instanceof GGridPropertyTable) {
            font = ((GGridPropertyTable) table).font;
        }
        if (font != null) {
            font.apply(divStyle);
        }

        updateElement(cellElement, value);
    }

    @Override
    protected String renderToString(Object value) {
        return (String) value;
    }
}