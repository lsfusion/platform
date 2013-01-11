package platform.gwt.form.shared.view.grid.editor;

import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.EditManager;

public class StringGridCellEditor extends TextBasedGridCellEditor {
    public StringGridCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property);
    }

    @Override
    protected Object tryParseInputText(String inputText) {
        return inputText.isEmpty() ? null : inputText;
    }
}
