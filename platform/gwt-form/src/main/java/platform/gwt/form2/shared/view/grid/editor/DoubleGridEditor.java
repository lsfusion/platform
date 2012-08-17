package platform.gwt.form2.shared.view.grid.editor;

import com.google.gwt.i18n.client.NumberFormat;
import platform.gwt.form2.shared.view.grid.EditManager;

public class DoubleGridEditor extends TextFieldGridEditor {
    public DoubleGridEditor(EditManager editManager, Object oldValue) {
        super(editManager, oldValue);
        NumberFormat f = NumberFormat.getDecimalFormat();
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