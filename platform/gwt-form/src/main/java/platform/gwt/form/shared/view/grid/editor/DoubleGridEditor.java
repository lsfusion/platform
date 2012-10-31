package platform.gwt.form.shared.view.grid.editor;

import platform.gwt.form.shared.view.grid.EditManager;

public class DoubleGridEditor extends TextFieldGridEditor {
    public DoubleGridEditor(EditManager editManager) {
        super(editManager);
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