package platform.client.form.editor;

import javax.swing.text.NumberFormatter;
import java.text.ParseException;
import java.text.NumberFormat;

public class NullNumberFormatter extends NumberFormatter {

    public NullNumberFormatter(NumberFormat format) {
        super(format);
    }

    @Override
    public Object stringToValue(String text) throws ParseException {
        if(text!=null && text.length() == 0)
             return null;
         else
             return super.stringToValue(text);
    }
}
