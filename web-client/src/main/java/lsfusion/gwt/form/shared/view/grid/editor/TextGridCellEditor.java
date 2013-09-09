package lsfusion.gwt.form.shared.view.grid.editor;

import com.google.gwt.dom.client.*;
import lsfusion.gwt.cellview.client.DataGrid;
import lsfusion.gwt.cellview.client.cell.Cell;
import lsfusion.gwt.form.client.form.ui.GGridPropertyTable;
import lsfusion.gwt.form.shared.view.GFont;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.grid.EditManager;

public class TextGridCellEditor extends TextBasedGridCellEditor {
    public TextGridCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property);
        inputElementTagName = "textarea";
    }

    @Override
    public void renderDom(Cell.Context context, DataGrid table, DivElement cellParent, Object value) {
        DivElement div = cellParent.appendChild(Document.get().createDivElement());
        div.getStyle().setPaddingRight(8, Style.Unit.PX);
        div.getStyle().setPaddingLeft(0, Style.Unit.PX);
        div.getStyle().setHeight(100, Style.Unit.PCT);

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
        textareaStyle.setWhiteSpace(Style.WhiteSpace.PRE_WRAP);
        textareaStyle.setProperty("wordWrap", "break-word");

        GFont font = property.font;
        if (font == null && table instanceof GGridPropertyTable) {
            font = ((GGridPropertyTable) table).font;
        }
        if (font != null) {
            font.apply(textareaStyle);
        }
        textareaStyle.setFontSize(8, Style.Unit.PT);

        cellParent.getStyle().setProperty("height", cellParent.getParentElement().getStyle().getHeight());
        cellParent.getStyle().setPadding(0, Style.Unit.PX);

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
    protected void arrowPressed(NativeEvent event, Element parent, boolean down) {
        //NOP
    }

    @Override
    protected InputElement getInputElement(Element parent) {
        return parent.getFirstChild().getFirstChild().cast();
    }
}
