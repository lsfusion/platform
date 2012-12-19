package platform.gwt.form.shared.view.grid.editor;

import com.google.gwt.dom.client.*;
import platform.gwt.cellview.client.cell.Cell;
import platform.gwt.form.shared.view.grid.EditManager;

public class TextGridCellEditor extends TextBasedGridCellEditor {
    public TextGridCellEditor(EditManager editManager) {
        super(editManager);
        inputElementTagName = "textarea";
    }

    @Override
    public void renderDom(Cell.Context context, DivElement cellParent, Object value) {
        DivElement div = cellParent.appendChild(Document.get().createDivElement());
        div.getStyle().setPaddingRight(8, Style.Unit.PX);
        div.getStyle().setPaddingLeft(0, Style.Unit.PX);

        TextAreaElement textArea = div.appendChild(Document.get().createTextAreaElement());
        textArea.setTabIndex(-1);

        Style textareaStyle = textArea.getStyle();
        textareaStyle.setBorderWidth(0, Style.Unit.PX);
        textareaStyle.setPaddingTop(0, Style.Unit.PX);
        textareaStyle.setPaddingRight(4, Style.Unit.PX);
        textareaStyle.setPaddingBottom(0, Style.Unit.PX);
        textareaStyle.setPaddingLeft(4, Style.Unit.PX);
        textareaStyle.setWidth(100, Style.Unit.PCT);
        textareaStyle.setHeight(100, Style.Unit.PCT);
        textareaStyle.setProperty("resize", "none");

        textArea.setValue(currentText);
    }

    @Override
    protected Object tryParseInputText(String inputText) throws ParseException {
        return inputText.isEmpty() ? null : inputText;
    }

    @Override
    protected void enterPressed(NativeEvent event, Element parent) {
        if (event.getShiftKey()) {
            super.enterPressed(event, parent);
        }
    }

    @Override
    protected InputElement getInputElement(Element parent) {
        return parent.getFirstChild().getFirstChild().cast();
    }
}
