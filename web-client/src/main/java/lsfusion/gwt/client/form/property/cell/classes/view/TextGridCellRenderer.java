package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public class TextGridCellRenderer extends TextBasedGridCellRenderer {
    private final boolean rich;

    public TextGridCellRenderer(GPropertyDraw property, boolean rich) {
        super(property);
        this.rich = rich;
    }

    @Override
    public void renderDom(DataGrid table, DivElement cellElement, Object value) {
        renderDom(cellElement, value);
        if (property.font == null && table instanceof GGridPropertyTable) {
            property.font = ((GGridPropertyTable) table).font;
        }
    }

    @Override
    public void renderDom(Element cellElement, Object value) {
        Style divStyle = cellElement.getStyle();
        divStyle.setPaddingRight(4, Style.Unit.PX);
        divStyle.setPaddingLeft(4, Style.Unit.PX);

        divStyle.setProperty("lineHeight", "normal");
        if (!rich) {
            divStyle.setProperty("wordWrap", "break-word");
            divStyle.setWhiteSpace(Style.WhiteSpace.PRE_WRAP);
        }
        if (property.font != null) {
            property.font.apply(divStyle);
        }
        updateElement(cellElement, value);
    }

    @Override
    protected void updateElement(Element div, Object value) {
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