package platform.gwt.form.shared.view.grid.editor;

import com.google.gwt.dom.client.Style;
import platform.gwt.form.shared.view.grid.EditManager;

public class IntegerGridCellEditor extends TextGridCellEditor {
    public IntegerGridCellEditor(EditManager editManager) {
        super(editManager, Style.TextAlign.RIGHT);
    }

    @Override
    protected Object tryParseInputText(String inputText) throws ParseException {
        try {
            return inputText.isEmpty() ? null : Integer.parseInt(inputText);
        } catch (NumberFormatException e) {
            throw new ParseException();
        }
    }
}
