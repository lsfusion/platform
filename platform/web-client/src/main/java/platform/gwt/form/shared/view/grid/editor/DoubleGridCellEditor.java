package platform.gwt.form.shared.view.grid.editor;

import com.google.gwt.dom.client.Style;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.EditManager;

public class DoubleGridCellEditor extends TextBasedGridCellEditor {
    public DoubleGridCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property, Style.TextAlign.RIGHT);
    }

    @Override
    protected Object tryParseInputText(String inputText) throws ParseException {
        try {
            return inputText.isEmpty() ? null : Double.parseDouble(inputText);
        } catch (NumberFormatException e) {
            throw new ParseException();
        }
    }
}