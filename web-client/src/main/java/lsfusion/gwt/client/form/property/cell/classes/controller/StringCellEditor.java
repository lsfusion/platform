package lsfusion.gwt.client.form.property.cell.classes.controller;

import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

public class StringCellEditor extends TextBasedCellEditor {
    private boolean isVarString;
    private int stringLength; 

    public StringCellEditor(EditManager editManager, GPropertyDraw property, boolean isVarString, int stringLength) {
        super(editManager, property);
        this.isVarString = isVarString;
        this.stringLength = stringLength;
    }

    @Override
    protected String tryParseInputText(String inputText, boolean onCommit) {
        return inputText.isEmpty() ? null : inputText;
    }

    @Override
    protected String tryFormatInputText(Object value) {
        if (value == null) {
            return "";
        }

        return isVarString ? (String)value : GwtSharedUtils.rtrim(value.toString());
    }

    @Override
    protected boolean isStringValid(String string) {
        return string.length() <= stringLength;
    }
}
