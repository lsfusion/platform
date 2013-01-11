package platform.gwt.form.shared.view.grid.renderer;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style;
import platform.gwt.base.client.EscapeUtils;
import platform.gwt.cellview.client.cell.Cell;
import platform.gwt.form.shared.view.GPropertyDraw;

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
    public void renderDom(Cell.Context context, DivElement cellElement, Object value) {
        DivElement div = cellElement.appendChild(Document.get().createDivElement());

        Style divStyle = div.getStyle();
        if (textAlign != null) {
            divStyle.setTextAlign(textAlign);
        }
        divStyle.setPaddingTop(0, Style.Unit.PX);
        divStyle.setPaddingRight(4, Style.Unit.PX);
        divStyle.setPaddingBottom(0, Style.Unit.PX);
        divStyle.setPaddingLeft(4, Style.Unit.PX);

        if (property.fontSize != null) {
            divStyle.setFontSize(property.fontSize, Style.Unit.PX);
        }
        if (property.fontFamily != null) {
            divStyle.setProperty("fontFamily", property.fontFamily);
        }

        updateElement(div, value);
    }

    @Override
    public void updateDom(DivElement cellElement, Cell.Context context, Object value) {
        DivElement div = cellElement.getFirstChild().cast();
        updateElement(div, value);
    }

    private void updateElement(DivElement div, Object value) {
        String text = value == null ? null : renderToString((T) value);

        if (text == null || text.trim().isEmpty()) {
            div.setInnerText(EscapeUtils.UNICODE_NBSP);
            div.setTitle("");
        } else {
            String stringValue = EscapeUtils.unicodeEscape(text.trim());
            div.setInnerText(stringValue);
            div.setTitle(stringValue);
        }
    }

    protected abstract String renderToString(T value);
}
