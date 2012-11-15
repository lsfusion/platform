package platform.gwt.form.shared.view.grid.editor;

import platform.gwt.form.shared.view.grid.EditManager;

public class StringGridEditor extends TextFieldGridEditor {
    public StringGridEditor(EditManager editManager) {
        super(editManager);
    }

    @Override
    protected Object tryParseInputText(String inputText) {
        return inputText.isEmpty() ? null : inputText;
    }
}
