package lsfusion.client.form.editor;

import javax.swing.text.NumberFormatter;
import java.text.NumberFormat;
import java.text.ParseException;

public class NullNumberFormatter extends NumberFormatter {
    public String minusZeroText;
    public Object zeroValue;
    private String decimalSeparator;

    public NullNumberFormatter(NumberFormat format, Object zeroValue) {
        this(format, zeroValue, "");
    }
    
    public NullNumberFormatter(NumberFormat format, Object zeroValue, String decimalSeparator) {
        super(format);
        this.zeroValue = zeroValue;
        this.decimalSeparator = decimalSeparator;
    }

    @Override
    public String valueToString(Object value) throws ParseException {
        if (minusZeroText != null) {
            return minusZeroText;
        }
        return super.valueToString(value);
    }

    @Override
    public Object stringToValue(String text) throws ParseException {
        if (text != null) {
            text = text.replace("--", "-");

            minusZeroText = text.equals("-") || 
                    text.equals("-0") || 
                    text.equals("-0" + decimalSeparator) || 
                    text.equals("-0" + decimalSeparator + "0") ||
                    text.equals("-0" + decimalSeparator + "00") ||
                    text.equals("-0" + decimalSeparator + "000") ? text : null;
            if (minusZeroText != null) {
                return zeroValue;
            }

            if (text.length() == 0) {
                return null;
            }
        }
        
        return super.stringToValue(text);
    }
}
