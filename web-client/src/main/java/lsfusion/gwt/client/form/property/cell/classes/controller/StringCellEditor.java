package lsfusion.gwt.client.form.property.cell.classes.controller;

import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

public class StringCellEditor extends TextBasedCellEditor {
    private boolean isVarString;
    private int stringLength; 

    public StringCellEditor(EditManager editManager, GPropertyDraw property, boolean isVarString, int stringLength) {
        this(editManager, property, isVarString, stringLength, null, false);
    }

    public StringCellEditor(EditManager editManager, GPropertyDraw property, boolean isVarString, int stringLength, GInputList inputList, boolean escapeComma) {
        super(editManager, property, inputList, escapeComma);
        this.isVarString = isVarString;
        this.stringLength = stringLength;
    }

    @Override
    protected String tryFormatInputText(Object value) {
        if (value == null) {
            return "";
        }

        String stringValue = value.toString();
        return isVarString ? stringValue : GwtSharedUtils.rtrim(stringValue);
    }

    @Override
    protected boolean isStringValid(String string) {
        return string.length() <= stringLength;
    }
}
