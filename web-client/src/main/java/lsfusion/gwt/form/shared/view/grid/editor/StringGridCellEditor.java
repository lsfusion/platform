package lsfusion.gwt.form.shared.view.grid.editor;

import lsfusion.gwt.base.shared.GwtSharedUtils;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.grid.EditManager;

public class StringGridCellEditor extends TextBasedGridCellEditor {
    private boolean isVarString;

    public StringGridCellEditor(EditManager editManager, GPropertyDraw property, boolean isVarString) {
        super(editManager, property);
        this.isVarString = isVarString;
    }

    @Override
    protected Object tryParseInputText(String inputText) {
        return inputText.isEmpty() ? null : inputText;
    }

    @Override
    protected String renderToString(Object value) {
        if (value == null) {
            return "";
        }

        return isVarString ? (String)value : GwtSharedUtils.rtrim(value.toString());
    }
}
