package lsfusion.gwt.client.form.property;

import com.google.gwt.core.client.JavaScriptObject;
import lsfusion.gwt.client.base.jsni.JSNIHelper;

import static lsfusion.gwt.client.form.property.SimpleDatePatternConverter.repeat;

public class DatePatternConverter {
    
    public static JavaScriptObject convert(String pattern) {
        JavaScriptObject options = JSNIHelper.createObject();
        JSNIHelper.setAttribute(options, "prefillYear", false);
        String mask = SimpleDatePatternConverter.patternToMask(pattern, DatePatternConverter::convertSymbol);
        JSNIHelper.setAttribute(options, "inputFormat", mask);
        return options;
    }
    
    private static String convertSymbol(char symbol, int count) {
        switch (symbol) {
            case 'd':
            case 'h':
            case 's':
                return repeat(symbol, Math.min(count, 2));
            case 'H':
                return "HH"; // there is no "H" in library
            case 'M':
                return repeat('m', Math.min(count, 4));
            case 'y':
                return count == 2 ? "yy" : "yyyy";
            case 'm':
                return count == 1 ? "M" : "MM";
            case 'E':
                return count < 4 ? "ddd" : "dddd";
            case 'a':
                return "TT";
        }
        return repeat(symbol, count);
    }
}
