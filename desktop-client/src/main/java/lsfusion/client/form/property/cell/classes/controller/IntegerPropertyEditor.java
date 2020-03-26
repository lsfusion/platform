package lsfusion.client.form.property.cell.classes.controller;

import lsfusion.client.form.property.ClientPropertyDraw;

import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.text.NumberFormat;
import java.text.ParseException;

public class IntegerPropertyEditor extends TextFieldPropertyEditor {

    public IntegerPropertyEditor(Object value, NumberFormat format, ClientPropertyDraw property, Class<?> valueClass) {
        this(value, null, format, property, valueClass);
    }

    public IntegerPropertyEditor(Object value, Comparable maxValue, NumberFormat format, ClientPropertyDraw property, Class<?> valueClass) {
        super(property);

        NumberFormatter formatter = new NullNumberFormatter(format, 0);
        formatter.setValueClass(valueClass);
        formatter.setMaximum(maxValue);
        formatter.setAllowsInvalid(false);

        setFormatterFactory(new DefaultFormatterFactory(formatter));

        String stringValue = "";
        if (value != null) {
            try {
                stringValue = formatter.valueToString(value); 
            } catch (ParseException ignored) {}
        }
        setText(stringValue);
    }
}
