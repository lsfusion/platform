package platform.client.form.editor;

import platform.interop.ComponentDesign;

import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;

public class DoublePropertyEditor extends TextFieldPropertyEditor {

    public DoublePropertyEditor(Object value, NumberFormat format, ComponentDesign design, Class<?> valueClass) {
        super(design);
        final DecimalFormat df = (DecimalFormat) format;

        NumberFormatter formatter = new NumberFormatter(format) {
            private final char separator = df.getDecimalFormatSymbols().getDecimalSeparator();

            public boolean lastTextEndsWithSeparator;

            @Override
            public String valueToString(Object value) throws ParseException {
                String result = super.valueToString(value);
                if (lastTextEndsWithSeparator && result != null && result.indexOf(separator) == -1) {
                    result += separator;
                    lastTextEndsWithSeparator = false;
                }
                return result;
            }

            @Override
            public Object stringToValue(String text) throws ParseException {
                if (text != null && text.length() > 0) {
                    text = text.replace(',', separator).replace('.', separator);
                    lastTextEndsWithSeparator = text.indexOf(separator) == text.length() - 1;
                } else {
                    lastTextEndsWithSeparator = false;
                }
                return super.stringToValue(text);
            }
        };

        formatter.setValueClass(valueClass);
        formatter.setAllowsInvalid(false);

        this.setHorizontalAlignment(JTextField.RIGHT);
        setFormatterFactory(new DefaultFormatterFactory(formatter));

        if (value != null) {
            setValue(value);
        }

        // выглядит странно, но где-то внутри это позволяет
        // обойти баг со сбрасыванием выделения в ячейках таблицы из-за форматтера
        setText(getText());
    }


    public Object getCellEditorValue() {

        try {
            commitEdit();
        } catch (ParseException e) {
            return null;
        }

        return this.getValue();
    }
}
