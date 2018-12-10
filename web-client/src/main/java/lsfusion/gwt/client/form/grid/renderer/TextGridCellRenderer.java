package lsfusion.gwt.client.form.grid.renderer;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.cellview.DataGrid;
import lsfusion.gwt.client.cellview.cell.Cell;
import lsfusion.gwt.client.form.form.ui.GGridPropertyTable;
import lsfusion.gwt.shared.view.GFont;
import lsfusion.gwt.shared.view.GPropertyDraw;

public class TextGridCellRenderer extends TextBasedGridCellRenderer {
    private final boolean rich;

    public TextGridCellRenderer(GPropertyDraw property, boolean rich) {
        super(property);
        this.rich = rich;
    }

    @Override
    public void renderDom(Cell.Context context, DataGrid table, DivElement cellElement, Object value) {
        Style divStyle = cellElement.getStyle();
        divStyle.setPaddingRight(4, Style.Unit.PX);
        divStyle.setPaddingLeft(4, Style.Unit.PX);

        divStyle.setProperty("lineHeight", "normal");
        if (!rich) {
            divStyle.setProperty("wordWrap", "break-word");
            divStyle.setWhiteSpace(Style.WhiteSpace.PRE_WRAP);
        }

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
    protected void updateElement(DivElement div, Object value) {
        if (!rich || value == null) {
            super.updateElement(div, value);
        } else {
            div.removeClassName("nullValueString");
            div.getStyle().setWhiteSpace(Style.WhiteSpace.PRE_WRAP);
            div.setInnerHTML(EscapeUtils.sanitizeHtml((String) value));
        }
    }

    @Override
    protected String renderToString(Object value) {
        return (String) value;
    }
}