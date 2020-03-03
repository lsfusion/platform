package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.AbstractGridCellRenderer;

import static lsfusion.gwt.client.base.EscapeUtils.unicodeEscape;

public abstract class TextBasedGridCellRenderer<T> extends AbstractGridCellRenderer {
    protected GPropertyDraw property;

    public TextBasedGridCellRenderer(GPropertyDraw property) {
        this.property = property;
    }

    @Override
    public void renderDom(DataGrid table, DivElement cellElement, Object value) {
        renderDom(cellElement, value);
        if (property.font == null && table instanceof GGridPropertyTable) {
            property.font = ((GGridPropertyTable) table).font;
        }
    }

    public void renderDom(Element cellElement, Object value) {
        Style divStyle = cellElement.getStyle();
        Style.TextAlign textAlignStyle = property.getTextAlignStyle();
        if (textAlignStyle != null) {
            divStyle.setTextAlign(textAlignStyle);
        }
        divStyle.setPaddingTop(0, Style.Unit.PX);
        divStyle.setPaddingRight(4, Style.Unit.PX);
        divStyle.setPaddingBottom(0, Style.Unit.PX);
        divStyle.setPaddingLeft(4, Style.Unit.PX);

        // важно оставить множественные пробелы
        divStyle.setWhiteSpace(Style.WhiteSpace.PRE);
        divStyle.setPosition(Style.Position.RELATIVE);

        //нужно для эллипсиса, но подтормаживает рендеринг,
        //оставлено закомменченым просто для справки
//        divStyle.setOverflow(Style.Overflow.HIDDEN);
//        divStyle.setTextOverflow(Style.TextOverflow.ELLIPSIS);

        if (property.font != null) {
            property.font.apply(divStyle);
        }
        divStyle.clearProperty("lineHeight");

        updateElement(cellElement, value);
    }

    @Override
    public void updateDom(DivElement cellElement, DataGrid table, Object value) {
        updateDom(cellElement, value);

        if (property.font == null && table instanceof GGridPropertyTable) {
            property.font = ((GGridPropertyTable) table).font;
        }
    }

    @Override
    public void updateDom(Element cellElement, Object value) {
        if (property.font != null) {
            property.font.apply(cellElement.getStyle());
        }
        updateElement(cellElement, value);
    }

    protected void updateElement(Element div, Object value) {
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

    protected void setInnerText(Element div, String innerText) {
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
