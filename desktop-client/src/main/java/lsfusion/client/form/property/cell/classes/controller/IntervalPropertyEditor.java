package lsfusion.client.form.property.cell.classes.controller;

import lsfusion.client.form.property.ClientPropertyDraw;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;

public abstract class IntervalPropertyEditor extends TextFieldPropertyEditor implements PropertyEditor {

    protected SimpleDateFormat editingFormat;
    protected Object defaultValue;

    public IntervalPropertyEditor(ClientPropertyDraw property) {
        super(property);
    }

    @Override
    public Object getCellEditorValue() {
        try {
            String[] dates = getText().split(" - ");
            return new BigDecimal(editingFormat.parse(dates[0]).getTime() / 1000
                    + "." + editingFormat.parse(dates[1]).getTime() / 1000);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
