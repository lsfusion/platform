package platform.gwt.form2.shared.view.grid.editor;

import platform.gwt.form2.shared.view.grid.EditManager;

public class StringGridEditor extends TextFieldGridEditor {
    public StringGridEditor(EditManager editManager) {
        super(editManager);
    }

    @Override
    protected Object tryParseInputText(String inputText) {
        return inputText;
    }
}
