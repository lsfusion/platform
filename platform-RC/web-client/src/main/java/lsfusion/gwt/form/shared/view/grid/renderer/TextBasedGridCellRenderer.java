package lsfusion.gwt.form.shared.view.grid.renderer;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.cellview.client.DataGrid;
import lsfusion.gwt.cellview.client.cell.Cell;
import lsfusion.gwt.form.client.form.ui.GGridPropertyTable;
import lsfusion.gwt.form.shared.view.GFont;
import lsfusion.gwt.form.shared.view.GPropertyDraw;

import static lsfusion.gwt.base.client.EscapeUtils.unicodeEscape;

public abstract class TextBasedGridCellRenderer<T> extends AbstractGridCellRenderer {
    protected final Style.TextAlign textAlign;
    protected GPropertyDraw property;

    public TextBasedGridCellRenderer(GPropertyDraw property) {
        this(property, null);
    }

    public TextBasedGridCellRenderer(GPropertyDraw property, Style.TextAlign textAlign) {
        this.property = property;
        this.textAlign = textAlign == Style.TextAlign.LEFT ? null : textAlign;
    }

    @Override
    public void renderDom(Cell.Context context, DataGrid table, DivElement cellElement, Object value) {
        Style divStyle = cellElement.getStyle();
        if (textAlign != null) {
            divStyle.setTextAlign(textAlign);
        }
        divStyle.setPaddingTop(0, Style.Unit.PX);
        divStyle.setPaddingRight(4, Style.Unit.PX);
        divStyle.setPaddingBottom(0, Style.Unit.PX);
        divStyle.setPaddingLeft(4, Style.Unit.PX);

        // важно оставить множественные пробелы
        divStyle.setWhiteSpace(Style.WhiteSpace.PRE);

        //нужно для эллипсиса, но подтормаживает рендеринг,
        //оставлено закомменченым просто для справки
//        divStyle.setOverflow(Style.Overflow.HIDDEN);
//        divStyle.setTextOverflow(Style.TextOverflow.ELLIPSIS);

        GFont font = property.font;
        if (font == null && table instanceof GGridPropertyTable) {
            font = ((GGridPropertyTable) table).font;
        }
        if (font != null) {
            font.apply(divStyle);
        }
        divStyle.clearProperty("lineHeight");

        updateElement(cellElement, value);
    }

    @Override
    public void updateDom(DivElement cellElement, DataGrid table, Cell.Context context, Object value) {
        GFont font = property.font;
        if (font == null && table instanceof GGridPropertyTable) {
            font = ((GGridPropertyTable) table).font;
        }
        if (font != null) {
            font.apply(cellElement.getStyle());
        }
        updateElement(cellElement, value);
    }

    protected void updateElement(DivElement div, Object value) {
        String text = value == null ? null : renderToString((T) value);

        if (text == null) {
            div.setTitle(property.isEditableNotNull() ? REQUIRED_VALUE : "");
            setInnerText(div, null);
        } else {
            String stringValue = unicodeEscape(text);
            setInnerText(div, stringValue);
            div.setTitle(property.echoSymbols ? "" : stringValue);
        }
    }

    protected void setInnerText(DivElement div, String innerText) {
        if (innerText == null) {
            if (property.isEditableNotNull()) {
                div.setInnerText(REQUIRED_VALUE);
                div.addClassName("requiredValueString");
                div.removeClassName("nullValueString");
            } else {
                div.setInnerText(EMPTY_VALUE);
                div.addClassName("nullValueString");
                div.removeClassName("requiredValueString");
            }
        } else {
            div.setInnerText(innerText);
            div.removeClassName("nullValueString");
            div.removeClassName("requiredValueString");
        }
    }

    protected abstract String renderToString(T value);
}
