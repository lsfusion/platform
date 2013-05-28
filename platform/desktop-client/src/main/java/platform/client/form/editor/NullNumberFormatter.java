package platform.client.form.editor;

import javax.swing.text.NumberFormatter;
import java.text.ParseException;
import java.text.NumberFormat;

public class NullNumberFormatter extends NumberFormatter {
    public boolean isMinus;
    public Object zeroValue;

    public NullNumberFormatter(NumberFormat format, Object zeroValue) {
        super(format);
        this.zeroValue = zeroValue;
    }

    @Override
    public String valueToString(Object value) throws ParseException {
        if (isMinus) {
            return "-";
        }
        return super.valueToString(value);
    }

    @Override
    public Object stringToValue(String text) throws ParseException {
        isMinus = text.equals("-");
        if (isMinus) {
            return zeroValue;
        }

        if(text!=null && text.length() == 0)
             return null;
         else
             return super.stringToValue(text);
    }
}
