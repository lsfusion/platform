package lsfusion.gwt.client.form.property.cell.classes.controller;

import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

public class StringCellEditor extends SimpleTextBasedCellEditor {
    private boolean isVarString;
    private int stringLength; 

    public StringCellEditor(EditManager editManager, GPropertyDraw property, boolean isVarString, int stringLength) {
        this(editManager, property, isVarString, stringLength, null);
    }

    public StringCellEditor(EditManager editManager, GPropertyDraw property, boolean isVarString, int stringLength, GInputList inputList) {
        super(editManager, property, inputList);
        this.isVarString = isVarString;
        this.stringLength = stringLength;
    }

    @Override
    protected String tryFormatInputText(PValue value) {
        if (value == null) {
            return "";
        }

        String string = PValue.getStringValue(value);
        if(isVarString)
            string = GwtSharedUtils.rtrim(string);

        return string;
    }

    @Override
    protected boolean isStringValid(String string) {
        return string.length() <= stringLength;
    }
}
