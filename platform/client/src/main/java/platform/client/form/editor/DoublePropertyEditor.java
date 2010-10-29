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
            private final String separatorStr = String.valueOf(separator);

            public boolean lastTextEndsWithSeparator;

            @Override
            public String valueToString(Object value) throws ParseException {
                String result = super.valueToString(value);
                if (lastTextEndsWithSeparator && result != null && !result.endsWith(separatorStr)) {
                    result += separatorStr;
                }
                return result;
            }

            @Override
            public Object stringToValue(String text) throws ParseException {
                text = text.replace(',', separator).replace('.', separator);
                lastTextEndsWithSeparator = text != null && text.endsWith(separatorStr);
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
