package lsfusion.client.form.editor;

import lsfusion.interop.ComponentDesign;

import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;

public class DoublePropertyEditor extends TextFieldPropertyEditor {

    public DoublePropertyEditor(Object value, Long maxValue, NumberFormat format, ComponentDesign design, Class formatterValueClass) {
        super(design);
        final DecimalFormat df = (DecimalFormat) format;
        final boolean isGroupSeparatorDot = df.getDecimalFormatSymbols().getGroupingSeparator() == '.';
        final char separator = df.getDecimalFormatSymbols().getDecimalSeparator();

        NumberFormatter formatter = new NullNumberFormatter(format, isGroupSeparatorDot ? 0 : 0.0, String.valueOf(separator)) {
            public boolean lastTextEndsWithSeparator;
            public int lastZero;

            @Override
            public String valueToString(Object value) throws ParseException {
                String result = super.valueToString(value);
                if (lastTextEndsWithSeparator && result != null && result.indexOf(separator) == -1) {
                    result += separator;
                    lastTextEndsWithSeparator = false;
                }
                for (int i = 0; i < lastZero; i++)
                    result += '0';
                return result;
            }

            @Override
            public Object stringToValue(String text) throws ParseException {
                lastZero = 0;
                if (text != null && text.length() > 0) {
                    if (text.contains(",") && separator == '.')
                        text = text.replace(",", ".");
                    else if (text.contains(".") && separator == ',')
                        text = text.replace(".", ",");
                    if (text.indexOf(separator) != -1) {
                        while (lastZero < text.length() - 1 && text.charAt(text.length() - 1 - lastZero) == '0') {
                            lastZero++;
                        }
                    }
                    lastTextEndsWithSeparator = text.indexOf(separator) == text.length() - 1 - lastZero;
                } else {
                    lastTextEndsWithSeparator = false;
                }
                return super.stringToValue(text);
            }
        };

        //через reflection добавляем к разрешённым символам второй decimal separator (. или ,)
        try {
            Field field = NumberFormatter.class.getDeclaredField("specialChars");
            field.setAccessible(true);
            String specialChars = (String) field.get(formatter);
            if(!specialChars.contains("."))
                specialChars +=".";
            if(!specialChars.contains(","))
                specialChars +=",";
            field.set(formatter, specialChars);
        } catch (NoSuchFieldException ignored) {
        } catch (IllegalAccessException ignored) {
        }

        formatter.setValueClass(formatterValueClass);
        formatter.setAllowsInvalid(false);
        if (maxValue != null)
            formatter.setMaximum(new BigDecimal(maxValue));

        this.setHorizontalAlignment(JTextField.RIGHT);
        setFormatterFactory(new DefaultFormatterFactory(formatter));

        if (value != null) {
            try {
                setText(formatter.valueToString(value));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }
}
