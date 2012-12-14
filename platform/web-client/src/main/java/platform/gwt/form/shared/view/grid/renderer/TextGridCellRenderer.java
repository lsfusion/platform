package platform.gwt.form.shared.view.grid.renderer;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style;
import platform.gwt.base.client.EscapeUtils;
import platform.gwt.cellview.client.cell.Cell;

public abstract class TextGridCellRenderer<T> extends AbstractGridCellRenderer {
    protected final Style.TextAlign textAlign;

    public TextGridCellRenderer() {
        this(null);
    }

    public TextGridCellRenderer(Style.TextAlign textAlign) {
        this.textAlign = textAlign == Style.TextAlign.LEFT ? null : textAlign;
    }

    @Override
    public void renderDom(Cell.Context context, DivElement cellElement, Object value) {
        DivElement div;
        if (textAlign != null) {
            div = cellElement.appendChild(Document.get().createDivElement());
            div.getStyle().setTextAlign(textAlign);
        } else {
            div = cellElement;
        }

        updateElement(div, value);
    }

    @Override
    public void updateDom(DivElement cellElement, Cell.Context context, Object value) {
        DivElement div = textAlign == null ? cellElement : cellElement.getFirstChild().<DivElement>cast();
        updateElement(div, value);
    }

    private void updateElement(DivElement div, Object value) {
        String text = value == null ? null : renderToString((T) value);

        if (text == null || text.trim().isEmpty()) {
            div.setInnerText(EscapeUtils.UNICODE_NBSP);
        } else {
            div.setInnerText(EscapeUtils.unicodeEscape(text.trim()));
        }
    }

    protected abstract String renderToString(T value);
}
