package platform.gwt.form.shared.view.grid.renderer;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.TextAreaElement;
import platform.gwt.base.client.EscapeUtils;
import platform.gwt.cellview.client.cell.Cell;

public class TextGridCellRenderer extends AbstractGridCellRenderer {
    @Override
    public void renderDom(platform.gwt.cellview.client.cell.Cell.Context context, DivElement cellElement, Object value) {
        DivElement div = cellElement.appendChild(Document.get().createDivElement());
        div.getStyle().setPaddingRight(4, Style.Unit.PX);
        div.getStyle().setPaddingLeft(4, Style.Unit.PX);

        TextAreaElement textArea = div.appendChild(Document.get().createTextAreaElement());
        textArea.setTabIndex(-1);

        Style textareaStyle = textArea.getStyle();
        textareaStyle.setBorderWidth(0, Style.Unit.PX);
        textareaStyle.setBackgroundColor("transparent");
        textareaStyle.setPadding(0, Style.Unit.PX);
        textareaStyle.setWidth(100, Style.Unit.PCT);
        textareaStyle.setHeight(100, Style.Unit.PCT);
        textareaStyle.setOverflowY(Style.Overflow.HIDDEN);
        textareaStyle.setWhiteSpace(Style.WhiteSpace.NORMAL);
        textareaStyle.setProperty("pointerEvents", "none");
        textareaStyle.setProperty("resize", "none");

        updateTextArea(textArea, value);
    }

    @Override
    public void updateDom(DivElement cellElement, Cell.Context context, Object value) {
        updateTextArea(cellElement.getFirstChild().getFirstChild().<TextAreaElement>cast(), value);
    }

    private void updateTextArea(TextAreaElement textArea, Object value) {
        if (value == null) {
            textArea.setValue(EscapeUtils.UNICODE_NBSP);
        } else {
            textArea.setValue((String) value);
        }
    }
}