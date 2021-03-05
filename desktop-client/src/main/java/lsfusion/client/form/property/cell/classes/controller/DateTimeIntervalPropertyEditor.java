package lsfusion.client.form.property.cell.classes.controller;

import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.view.MainFrame;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;

public class DateTimeIntervalPropertyEditor extends TextFieldPropertyEditor implements PropertyEditor {

    private final SimpleDateFormat editingFormat;
    private final Object defaultValue;

    public DateTimeIntervalPropertyEditor(Object value, ClientPropertyDraw property) {
        super(property);
        this.defaultValue = value;

        editingFormat = new SimpleDateFormat("dd.MM.yy HH:mm:ss");
        setText(editingFormat.format(MainFrame.getDateFromInterval(value, true))
                + " - " + editingFormat.format(MainFrame.getDateFromInterval(value, false)));
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
