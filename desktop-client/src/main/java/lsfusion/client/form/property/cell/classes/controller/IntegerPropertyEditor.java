package lsfusion.client.form.property.cell.classes.controller;

import lsfusion.interop.form.design.ComponentDesign;

import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.text.NumberFormat;
import java.text.ParseException;

public class IntegerPropertyEditor extends TextFieldPropertyEditor {

    public IntegerPropertyEditor(Object value, NumberFormat format, ComponentDesign design, Class<?> valueClass) {
        this(value, null, format, design, valueClass);
    }

    public IntegerPropertyEditor(Object value, Comparable maxValue, NumberFormat format, ComponentDesign design, Class<?> valueClass) {
        super(design);

        NumberFormatter formatter = new NullNumberFormatter(format, 0);
        formatter.setValueClass(valueClass);
        formatter.setMaximum(maxValue);
        formatter.setAllowsInvalid(false);

        setHorizontalAlignment(JTextField.RIGHT);

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
