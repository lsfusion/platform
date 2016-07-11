package lsfusion.client.form.editor;

import lsfusion.interop.ComponentDesign;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;

public class DoublePropertyEditor extends TextFieldPropertyEditor {
    DecimalFormat df = null;
    public DoublePropertyEditor(Object value, Long maxValue, NumberFormat format, ComponentDesign design, Class formatterValueClass, final boolean hasMask) {
        super(design);
        df = (DecimalFormat) format;
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
                if (minusZeroText == null) {
                    int minFractionDigits = df.getMinimumFractionDigits();
                    int currentFractionDigits = countFractionDigits(result);
                    if(hasMask && result != null) {
                        while (minFractionDigits < currentFractionDigits) {
                            result = result.substring(0, result.length() - 1);
                            currentFractionDigits--;
                        }
                    }
                    while(lastZero > currentFractionDigits) {
                        result += '0';
                        currentFractionDigits++;
                    }
                }
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

            private int countFractionDigits(String text) {
                int count = 0;
                if(text.indexOf(separator) > -1) {
                    while (!text.endsWith(String.valueOf(separator))) {
                        text = text.substring(0, text.length() - 1);
                        count++;
                    }
                }
                return count;
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
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
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


    @Override
    public void replaceSelection(String content) {
        if (getSelectedText() == null) {
            String text = getText();
            if (content.equals("-")) {
                if (text.startsWith("-")) {
                    setSingleSelection(0);
                    content = "";
                } else {
                    setText("-" + text);
                    return;
                }
            } else {
                if (StringUtils.isNumeric(content)) {
                    //проверяем, не превышен ли лимит символов до/после запятой
                    int currentPosition = getCaret().getDot();
                    char separator = df.getDecimalFormatSymbols().getDecimalSeparator();
                    int separatorPosition = text.indexOf(separator);
                    String[] split = text.replace(String.valueOf(df.getDecimalFormatSymbols().getGroupingSeparator()), "").split(String.valueOf(separator));
                    if (currentPosition <= separatorPosition || separatorPosition == -1) {
                        if (df.getMaximumIntegerDigits() <= split[0].length())
                            setSingleSelection(currentPosition - (currentPosition == separatorPosition || currentPosition == text.length() ? 1 : 0));
                    } else {
                        if (split.length > 1 && df.getMaximumFractionDigits() <= split[1].length())
                            setSingleSelection(currentPosition - (currentPosition == text.length() ? 1 : 0));
                    }
                }
            }
        }
        super.replaceSelection(content);
    }

    private void setSingleSelection(int start) {
        setSelectionStart(start);
        setSelectionEnd(start + 1);
    }
}
