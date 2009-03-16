package platform.client.form.editor;

import platform.client.form.PropertyEditorComponent;

import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;

public class IntegerPropertyEditor extends TextFieldPropertyEditor
                            implements PropertyEditorComponent {

    public IntegerPropertyEditor(Object value, NumberFormat format, Class<?> valueClass) {

//        NumberFormat format = iformat;
//        if (format == null)
//            format = NumberFormat.getInstance();

        if (Double.class.equals(valueClass) && format instanceof DecimalFormat && format.getMaximumFractionDigits() > 0) {
            ((DecimalFormat) format).setDecimalSeparatorAlwaysShown(true);
        }

        NumberFormatter formatter = new NumberFormatter(format) {

            public Object stringToValue(String text) throws ParseException {
                if (text.isEmpty() || text.equals("-") || text.equals(",") || text.equals(".") || text.equals("-,") || text.equals("-.")) return null;
                return super.stringToValue(text);
            }
        };

        formatter.setValueClass(valueClass);
        formatter.setAllowsInvalid(false);

        this.setHorizontalAlignment(JTextField.RIGHT);

        setFormatterFactory(new DefaultFormatterFactory(formatter));

        if (value != null)
            setValue(value);
        selectAll();

    }

    public Component getComponent() {
        return this;
    }

    public Object getCellEditorValue() {

        try {
            commitEdit();
        } catch (ParseException e) {
            return null;
        }

        return this.getValue();
    }

    public boolean valueChanged() {
        return true;
    }

}
